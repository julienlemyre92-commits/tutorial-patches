#!/usr/bin/env python3
"""
Microbot Launcher Auto-Clicker
Waits for the Microbot launcher/game and clicks "Login" or "Play Now".
Uses OCR to find buttons by text (no image templates needed).

Requires: pip install pyautogui pytesseract pillow
  And Tesseract OCR: https://github.com/UB-Mannheim/tesseract/wiki

Usage: python launcher_clicker.py [--timeout 120]
       python launcher_clicker.py --check-once   # single healing pass (Supervisor calls this every 30s)

New in this version: detects the "You were disconnected from the server."
modal and clicks Ok, and recognizes "CLICK HERE TO PLAY". --check-once is a
non-blocking single OCR pass designed to run every Supervisor cycle so a
mid-session disconnect heals itself without a client restart.
"""

import sys
import time
import argparse
import re
from pathlib import Path

try:
    import pyautogui
except ImportError:
    print("ERROR: pip install pyautogui pillow")
    sys.exit(1)

try:
    import pytesseract
    # Try common Tesseract install locations on Windows
    import os
    import shutil
    if not shutil.which("tesseract"):
        for p in [
            r"C:\Program Files\Tesseract-OCR\tesseract.exe",
            r"C:\Program Files (x86)\Tesseract-OCR\tesseract.exe",
        ]:
            if os.path.exists(p):
                pytesseract.pytesseract.tesseract_cmd = p
                print(f"Found Tesseract at: {p}")
                break
    HAS_OCR = True
except ImportError:
    print("WARNING: no pytesseract, using keyboard fallback")
    print("  pip install pytesseract pillow")
    HAS_OCR = False

try:
    from PIL import ImageOps, ImageEnhance
except ImportError:
    ImageOps = None
    ImageEnhance = None

TARGET_WORLD = "random"  # Free world to use
WORLD_LIST_TIMEOUT = 8.0
OCR_MIN_CONF = 45

# World 308 grid position in the world list (as fraction of game window)
# Adjust these if the click misses - run with --calibrate to see the position
WORLD_308_X_PCT = 0.09  # 9% from left (column 1)
WORLD_308_Y_PCT = 0.28  # 26.5% from top (was hitting 307, moved down half a row)


def native_status():
    """Fresh in-client evidence, never a screenshot guess or stored password."""
    try:
        path = Path.home() / '.runelite' / 'cooks-hot' / 'status.properties'
        props = dict(line.split('=', 1) for line in path.read_text().splitlines()
                     if '=' in line and not line.startswith('#'))
        if not 0 <= time.time() * 1000 - int(props['timestamp']) < 5000:
            return None
        return props
    except (OSError, ValueError, KeyError):
        return None


def find_text(data, target, min_conf=50):
    """Find all occurrences of target text in OCR data. Returns list of (x_center, y_center)."""
    results = []
    target = target.lower()
    for i in range(len(data['text'])):
        text = data['text'][i].strip().lower()
        conf = int(data['conf'][i])
        if conf < min_conf:
            continue
        if text == target:
            x = data['left'][i] + data['width'][i] // 2
            y = data['top'][i] + data['height'][i] // 2
            results.append((x, y))
    return results


def ocr_screenshot():
    """Take a full screenshot and return (screenshot, ocr_data)."""
    screenshot = pyautogui.screenshot()
    data = ocr_image_data(screenshot)
    return screenshot, data


def ocr_image_data(image):
    """Run OCR with a same-size contrast variant and return the better result.

    Coordinates stay in the original image space, so OCR-derived clicks remain
    valid on high-DPI displays. The contrast pass helps with RuneLite's small
    pixel font without introducing a scale-dependent coordinate transform.
    """
    candidates = []
    for variant in (image,):
        try:
            candidates.append(pytesseract.image_to_data(
                variant, output_type=pytesseract.Output.DICT, timeout=3))
        except Exception:
            pass
    # A recognized control needs no second Tesseract invocation.
    if candidates:
        data = candidates[0]
        if (find_login_or_playnow(data, 0, 0) or find_click_here_to_play(data, 0, 0)
                or find_disconnect_ok(data, 0, 0) or world_list_visible(data)):
            return data
    if ImageOps is not None and ImageEnhance is not None:
        try:
            gray = ImageOps.grayscale(image)
            enhanced = ImageEnhance.Contrast(gray).enhance(2.2)
            candidates.append(pytesseract.image_to_data(
                enhanced, output_type=pytesseract.Output.DICT, timeout=3))
        except Exception:
            pass
    if not candidates:
        return {'text': [], 'conf': [], 'left': [], 'top': [],
                'width': [], 'height': []}
    def score(data):
        total = 0
        for text, conf in zip(data.get('text', []), data.get('conf', [])):
            try:
                if (text or '').strip() and int(float(conf)) >= OCR_MIN_CONF:
                    total += 1
            except (TypeError, ValueError):
                continue
        return total
    return max(candidates, key=score)


def _ocr_box(data, index, ox=0, oy=0):
    """Return an OCR token center and size in screen coordinates."""
    return (
        ox + data['left'][index] + data['width'][index] // 2,
        oy + data['top'][index] + data['height'][index] // 2,
        data['width'][index], data['height'][index],
    )


def find_world_target(data, target, ox=0, oy=0, min_conf=OCR_MIN_CONF):
    """Find an exact world-number token in an OCR world-list frame.

    The exact token requirement prevents clicking nearby world numbers. The
    largest high-confidence match is preferred when the number appears in more
    than one UI element.
    """
    target = str(target).strip()
    matches = []
    for i, raw in enumerate(data.get('text', [])):
        text = (raw or '').strip()
        try:
            conf = int(float(data['conf'][i]))
        except (TypeError, ValueError, KeyError, IndexError):
            continue
        if conf < min_conf or text != target:
            continue
        x, y, w, h = _ocr_box(data, i, ox, oy)
        matches.append((h, conf, x, y))
    if not matches:
        return None
    _, _, x, y = max(matches, key=lambda item: (item[0], item[1]))
    return x, y


def world_list_visible(data):
    """Return true when OCR still sees several three-digit world rows."""
    if not data:
        return False
    count = 0
    for raw in data.get('text', []):
        if re.fullmatch(r'\d{3}', (raw or '').strip()):
            count += 1
    return count >= 4


def find_world_switcher(data, ox=0, oy=0, window_bounds=None):
    """Find the current-world control near the lower-left of the game window."""
    if not window_bounds:
        return None
    gx, gy, gw, gh = window_bounds
    candidates = []
    for i, raw in enumerate(data.get('text', [])):
        text = (raw or '').strip()
        if not re.fullmatch(r'\d{3}', text):
            continue
        try:
            conf = int(float(data['conf'][i]))
        except (TypeError, ValueError, KeyError, IndexError):
            continue
        x, y, w, h = _ocr_box(data, i, ox, oy)
        relx, rely = x - gx, y - gy
        if conf >= OCR_MIN_CONF and relx <= gw * 0.35 and rely >= gh * 0.55:
            candidates.append((rely, -relx, conf, x, y))
    if not candidates:
        return None
    # Prefer the lowest, leftmost world-number token.
    _, _, _, x, y = max(candidates)
    return x, y


def find_disconnect_ok(data, ox, oy, min_conf=50):
    """If the 'You were disconnected from the server.' modal is visible,
    return the (x, y) center of its Ok button. Otherwise None.

    Requires BOTH the word 'disconnect' somewhere AND an 'ok' token, so we
    never click a stray 'Ok' during normal gameplay.
    """
    texts = [(t or "").strip().lower() for t in data['text']]
    if not any('disconnect' in t for t in texts):
        return None
    best = None
    for i in range(len(data['text'])):
        t = texts[i]
        try:
            conf = int(data['conf'][i])
        except (ValueError, TypeError):
            continue
        if t == 'ok' and conf >= min_conf:
            h = data['height'][i]
            x = ox + data['left'][i] + data['width'][i] // 2
            y = oy + data['top'][i] + data['height'][i] // 2
            if best is None or h > best[2]:
                best = (x, y, h)
    return (best[0], best[1]) if best else None


def find_exit_modal_dismiss(data, ox, oy, min_conf=50):
    """Return the OCR-derived center of Cancel/No for the exact Exit modal.

    The native Java prompt is infrastructure state, not a Tutorial target. Require
    the full prompt text before accepting either button token so normal gameplay
    buttons are never clicked accidentally.
    """
    texts = [(t or "").strip().lower() for t in data.get('text', [])]
    normalized = " ".join(t for t in texts if t)
    compact = normalized.replace(" ", "")
    if "areyousureyouwanttoexit" not in compact:
        return None
    best = None
    for i, text in enumerate(texts):
        if text not in ("cancel", "no"):
            continue
        try:
            conf = int(data['conf'][i])
        except (ValueError, TypeError, KeyError):
            continue
        if conf < min_conf:
            continue
        h = data['height'][i]
        x = ox + data['left'][i] + data['width'][i] // 2
        y = oy + data['top'][i] + data['height'][i] // 2
        if best is None or h > best[0]:
            best = (h, x, y, text.title())
    return (best[1], best[2], best[3]) if best else None


def find_click_here_to_play(data, ox, oy, min_conf=40):
    """If 'CLICK HERE TO PLAY' text is visible, return the center of the
    phrase. Otherwise None."""
    n = len(data['text'])
    texts = [(t or "").strip().lower() for t in data['text']]
    # Fast path: single glued token
    for i in range(n):
        if 'clickheretoplay' in texts[i].replace(' ', ''):
            x = ox + data['left'][i] + data['width'][i] // 2
            y = oy + data['top'][i] + data['height'][i] // 2
            return (x, y)
    # Word sequence: click / here / to / play on roughly the same line
    for i in range(n - 3):
        try:
            confs = [int(data['conf'][i + k]) for k in range(4)]
        except (ValueError, TypeError):
            continue
        if all(c >= min_conf for c in confs):
            seq = [texts[i + k] for k in range(4)]
            if seq == ['click', 'here', 'to', 'play']:
                ys = [data['top'][i + k] for k in range(4)]
                if max(ys) - min(ys) > 30:
                    continue  # not on the same line
                x0 = min(data['left'][i + k] for k in range(4))
                x1 = max(data['left'][i + k] + data['width'][i + k] for k in range(4))
                y0 = min(ys)
                y1 = max(data['top'][i + k] + data['height'][i + k] for k in range(4))
                return (ox + (x0 + x1) // 2, oy + (y0 + y1) // 2)
    return None


def find_login_or_playnow(data, ox, oy, min_conf=50):
    """Return (x, y, label) for the Login or Play Now button, or None."""
    targets = []
    n = len(data['text'])
    for i in range(n):
        text = (data['text'][i] or "").strip().lower()
        try:
            conf = int(data['conf'][i])
        except (ValueError, TypeError):
            continue
        if conf < min_conf:
            continue
        if text == 'login':
            targets.append((i, 1, data['height'][i]))
        elif text == 'play' and i + 1 < n and (data['text'][i+1] or "").strip().lower() == 'now':
            targets.append((i, 2, data['height'][i] + data['height'][i+1]))
    if not targets:
        return None
    targets.sort(key=lambda t: (t[1], -t[2]))
    i, kind, _ = targets[0]
    x = ox + data['left'][i] + data['width'][i] // 2
    y = oy + data['top'][i] + data['height'][i] // 2
    if kind == 2 and i + 1 < n:
        x2 = ox + data['left'][i+1] + data['width'][i+1] // 2
        y2 = oy + data['top'][i+1] + data['height'][i+1] // 2
        x, y = (x + x2) // 2, (y + y2) // 2
    return (x, y, "Login" if kind == 1 else "Play Now")


def find_members_world_prompt(data, ox, oy):
    """Return the Switch World button for the members-world rejection screen.

    RuneLite's game font is often misread (for example ``Switch`` becomes
    ``Suiteh``), so require the distinctive rejection text and use the live
    Back button as an anchor.  The Switch World button is immediately above
    Back in this modal; this keeps the recovery tied to the current window
    geometry instead of a desktop coordinate.
    """
    tokens = [(t or '').strip().lower() for t in data.get('text', [])]
    has_members = any('member' in t or 'embers' in t for t in tokens)
    has_account = any('account' in t for t in tokens)
    has_world = any('world' in t for t in tokens)
    has_subscribe_or_login = any(
        'subscrib' in t or 'leese' in t or 'esubser' in t or 'login' in t
        for t in tokens
    )
    # The private client often renders this as "different world" / "Switch
    # World" without the word login.  The members+account+world+Back anchors
    # already make this a bounded, unambiguous rejection modal.
    has_world_choice = any(
        'different' in t or 'switch' in t or 'digger' in t or 'suitch' in t
        for t in tokens
    )
    has_back = any(t == 'back' for t in tokens)
    # Some frames drop the first sentence but retain the modal's
    # ``different world`` / ``Switch World`` and ``Back`` controls.
    if not (has_world and has_back and ((has_members and has_account) or has_world_choice)):
        return None

    backs = find_text(data, 'back', min_conf=65)
    if backs:
        # The largest/highest-confidence Back token is normally the modal
        # button.  Use its center and move one button-height upward.
        bx, by = backs[0]
        bounds = get_game_window_bounds()
        button_gap = max(38, int((bounds[3] if bounds else 700) * 0.07))
        return ox + bx, oy + by - button_gap

    # OCR occasionally misses Back while still recognizing the rejection
    # message.  Keep one bounded geometry fallback inside the live window.
    bounds = get_game_window_bounds()
    if not bounds:
        return None
    gx, gy, gw, gh = bounds
    return gx + int(gw * 0.46), gy + int(gh * 0.43)


def ocr_game_window():
    """Screenshot the RuneLite game window and OCR it.
    Returns (data, ox, oy) or (None, 0, 0) on failure."""
    try:
        screenshot = pyautogui.screenshot()
    except Exception as e:
        print(f"check: screenshot failed: {e}", flush=True)
        return None, 0, 0
    bounds = get_game_window_bounds()
    try:
        if bounds:
            gx, gy, gw, gh = bounds
            crop = screenshot.crop((gx, gy, gx + gw, gy + gh))
            data = ocr_image_data(crop)
            return data, gx, gy
        data = ocr_image_data(screenshot)
        return data, 0, 0
    except Exception as e:
        print(f"check: OCR failed: {e}", flush=True)
        return None, 0, 0


def find_runelite_windows():
    """Return visible RuneLite windows, largest first.

    Titles vary by launcher/profile, so exact ``RuneLite`` matching is too
    brittle.  Keep this as the single window-discovery path used by the
    polling loop and the non-blocking Supervisor check.
    """
    try:
        windows = []
        for window in pyautogui.getAllWindows():
            title = (getattr(window, 'title', '') or '').strip()
            title_lower = title.lower()
            # The bundled Microbot client can expose its native window as
            # ``...\\runtime\\bin\\java.exe`` instead of ``RuneLite``.
            # Accept only that runtime-Java shape; do not treat the Jagex
            # Launcher or arbitrary Java windows as the game client.
            is_runtime_java = ('java.exe' in title_lower and
                               'runtime' in title_lower)
            if 'runelite' not in title_lower and not is_runtime_java:
                continue
            width = int(getattr(window, 'width', 0) or 0)
            height = int(getattr(window, 'height', 0) or 0)
            if width >= 500 and height >= 300:
                # Prefer the actual RuneLite GUI over the Java console that
                # may be larger and carries the same runtime-java title.
                title_priority = 1 if 'runelite' in title_lower else 0
                windows.append((title_priority, width * height, window))
        windows.sort(key=lambda item: (item[0], item[1]), reverse=True)
        return [window for _, _, window in windows]
    except Exception as exc:
        print(f"RuneLite window discovery failed: {exc}", flush=True)
        return []


def check_once():
    """Single non-blocking pass: dismiss the disconnect dialog, click
    CLICK HERE TO PLAY / Login / Play Now if visible. Does nothing otherwise.
    Always exits 0 (a quiet pass is not an error)."""
    native = native_status()
    if native and (native.get('gameState') in ('LOGGED_IN', 'LOGGING_IN', 'LOADING')
                   or native.get('nativeLogin') == 'true'):
        print('check-once: client state=' + native['gameState'] + '; OCR skipped', flush=True)
        return 0
    windows = find_runelite_windows()
    if not windows:
        print("check-once: no RuneLite window", flush=True)
        return 0
    if not HAS_OCR:
        print("check-once: no OCR available", flush=True)
        return 0
    data, ox, oy = ocr_game_window()
    if data is None:
        return 0
    if world_list_visible(data):
        print("check-once: world list visible -> selecting configured world", flush=True)
        try:
            switch_world()
        except Exception as e:
            print(f"check-once: world-list recovery failed: {e}", flush=True)
        return 0
    members_prompt = find_members_world_prompt(data, ox, oy)
    if members_prompt:
        print(
            f"check-once: members-world rejection -> Switch World at "
            f"{members_prompt[0]},{members_prompt[1]}",
            flush=True,
        )
        try:
            pyautogui.moveTo(*members_prompt, duration=0.3)
            time.sleep(0.3)
            pyautogui.click(*members_prompt)
            # Reuse the verified OCR selector.  It will either find the list
            # already open or locate the live world-switcher anchor once.
            time.sleep(0.5)
            switch_world()
        except Exception as e:
            print(f"check-once: world-recovery failed: {e}", flush=True)
        return 0
    exit_dismiss = find_exit_modal_dismiss(data, ox, oy)
    if exit_dismiss:
        x, y, label = exit_dismiss
        print(f"check-once: exit modal -> {label} at {x},{y}", flush=True)
        try:
            pyautogui.moveTo(x, y, duration=0.3)
            time.sleep(0.3)
            pyautogui.click(x, y)
        except Exception as e:
            print(f"check-once: click failed: {e}", flush=True)
        return 0
    dlg = find_disconnect_ok(data, ox, oy)
    if dlg:
        print(f"check-once: disconnect dialog -> clicking Ok at {dlg[0]},{dlg[1]}", flush=True)
        try:
            pyautogui.moveTo(dlg[0], dlg[1], duration=0.3)
            time.sleep(0.3)
            pyautogui.click(dlg[0], dlg[1])
        except Exception as e:
            print(f"check-once: click failed: {e}", flush=True)
        return 0
    chtp = find_click_here_to_play(data, ox, oy)
    if chtp:
        print(f"check-once: CLICK HERE TO PLAY -> clicking at {chtp[0]},{chtp[1]}", flush=True)
        try:
            pyautogui.moveTo(chtp[0], chtp[1], duration=0.3)
            time.sleep(0.3)
            pyautogui.click(chtp[0], chtp[1])
        except Exception as e:
            print(f"check-once: click failed: {e}", flush=True)
        return 0
    # Prefer an OCR-confirmed Play Now button whenever available. Tesseract
    # is intermittent on the RuneScape lobby font; take two bounded retries
    # before declaring the healing pass idle.
    lp = find_login_or_playnow(data, ox, oy)
    if lp is None:
        for _ in range(1):
            time.sleep(0.2)
            retry_data, retry_ox, retry_oy = ocr_game_window()
            if retry_data is None:
                continue
            data, ox, oy = retry_data, retry_ox, retry_oy
            lp = find_login_or_playnow(data, ox, oy)
            if lp is not None:
                break
    # The RuneLite lobby's red "CLICK HERE TO PLAY" text is often stylized
    # enough that Tesseract sees WELCOME/TO/GIELINOR but misses the button.
    # On that unmistakable lobby screen use its fixed central button geometry.
    words = {(t or '').strip().lower() for t in data.get('text', [])}
    if lp is None and 'welcome' in words and ('gielinor' in words or 'runescape' in words):
        bounds = get_game_window_bounds()
        if bounds:
            gx, gy, gw, gh = bounds
            bx, by = gx + int(gw * 0.50), gy + int(gh * 0.52)
            print(f"check-once: lobby welcome/messages -> clicking Play geometry at {bx},{by}", flush=True)
            pyautogui.moveTo(bx, by, duration=0.3)
            time.sleep(0.3)
            pyautogui.click(bx, by)
            return 0
    if lp:
        # The lobby button can also be OCR-visible behind the private
        # client's members-world rejection modal.  Enforce the configured
        # free world before any Play Now click so the 30-second healing pass
        # cannot reconnect to a members world and immediately loop.
        if lp[2] == "Play Now":
            try:
                if not switch_world():
                    return 0
                # World-list selection can move/replace the lobby surface;
                # reacquire the live button before clicking once.
                refreshed, rox, roy = ocr_game_window()
                refreshed_lp = find_login_or_playnow(refreshed, rox, roy) if refreshed else None
                if refreshed_lp:
                    lp = refreshed_lp
            except Exception as e:
                print(f"check-once: world enforcement failed: {e}", flush=True)
                return 0
        print(f"check-once: {lp[2]} visible -> clicking at {lp[0]},{lp[1]}", flush=True)
        try:
            pyautogui.moveTo(lp[0], lp[1], duration=0.3)
            time.sleep(0.3)
            pyautogui.click(lp[0], lp[1])
        except Exception as e:
            print(f"check-once: click failed: {e}", flush=True)
        return 0
    print("check-once: nothing to do", flush=True)
    return 0


def get_game_window_bounds():
    """Get the RuneLite game window bounds via Win32 API (more reliable than pygetwindow)."""
    try:
        import ctypes
        from ctypes import wintypes
        user32 = ctypes.windll.user32

        # Select the largest visible RuneLite window instead of requiring an
        # exact title. RuneLite can append a profile/account name to the title,
        # and the old exact-title lookup caused the clicker to miss the client.
        EnumProc = ctypes.WINFUNCTYPE(ctypes.c_bool, wintypes.HWND, wintypes.LPARAM)
        found = []
        def _enum(h, _):
            n = ctypes.create_unicode_buffer(512)
            user32.GetWindowTextW(h, n, 512)
            title = n.value.strip().lower()
            is_runtime_java = ('java.exe' in title and 'runtime' in title)
            if 'runelite' not in title and not is_runtime_java:
                return True
            if not user32.IsWindowVisible(h):
                return True
            rect = wintypes.RECT()
            if user32.GetWindowRect(h, ctypes.byref(rect)):
                width = rect.right - rect.left
                height = rect.bottom - rect.top
                if width >= 500 and height >= 300:
                    # Prefer a real RuneLite GUI title over the larger
                    # runtime-java console window when both are present.
                    title_priority = 1 if 'runelite' in title else 0
                    found.append((title_priority, width * height, h))
            return True
        user32.EnumWindows(EnumProc(_enum), 0)
        hwnd = max(found, key=lambda item: (item[0], item[1]))[2] if found else 0
        if not hwnd:
            return None

        rect = wintypes.RECT()
        if not user32.GetWindowRect(hwnd, ctypes.byref(rect)):
            return None

        return (rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top)
    except Exception as e:
        print(f"Window bounds failed: {e}", flush=True)
        return None


def switch_world():
    """Switch worlds using OCR anchors and bounded state verification.

    The old implementation assumed fixed world-list percentages. Window moves,
    DPI scaling, and list layout changes made that click unreliable. We now
    locate the current-world control and the exact target-world token from the
    live frame. The old percentages remain only as a logged last resort.
    """
    print(f"Switching to world {TARGET_WORLD}...", flush=True)
    global TARGET_WORLD
    native = native_status()
    if native and int(native.get('selectedWorld', '0')) > 0:
        TARGET_WORLD = native['selectedWorld']
        if native.get('currentWorld') == TARGET_WORLD:
            print('World selection already verified by client: ' + TARGET_WORLD, flush=True)
            return True
    if not str(TARGET_WORLD).isdigit():
        print('Waiting for a verified random free-world selection from the client', flush=True)
        return False
    bounds = get_game_window_bounds()
    if not bounds:
        print("Could not get game window bounds, skipping", flush=True)
        return True

    data, ox, oy = ocr_game_window()
    target = find_world_target(data, TARGET_WORLD, ox, oy) if data else None
    if target:
        print(f"World list already open; OCR found {TARGET_WORLD} at {target[0]},{target[1]}", flush=True)
    else:
        switcher = find_world_switcher(data, ox, oy, bounds) if data else None
        if switcher:
            wx, wy = switcher
            print(f"Opening world list via OCR anchor at {wx},{wy}", flush=True)
        else:
            gx, gy, gw, gh = bounds
            wx = gx + int(gw * 0.07)
            wy = gy + int(gh * 0.72)
            print(f"World switcher OCR anchor unavailable; bounded fallback at {wx},{wy}", flush=True)
        try:
            pyautogui.moveTo(wx, wy, duration=0.2)
            pyautogui.click(wx, wy)
        except Exception as e:
            print(f"World switcher click failed: {e}", flush=True)
            return False

        deadline = time.time() + WORLD_LIST_TIMEOUT
        target = None
        while time.time() < deadline:
            time.sleep(0.35)
            data, ox, oy = ocr_game_window()
            if data:
                target = find_world_target(data, TARGET_WORLD, ox, oy)
                if target:
                    break
        if not target:
            print('Exact selected world unreadable; no fixed-grid click issued', flush=True)
            return False

    bx, by = target
    print(f"Clicking world {TARGET_WORLD} via OCR at {bx},{by}", flush=True)
    try:
        pyautogui.moveTo(bx, by, duration=0.2)
        pyautogui.click(bx, by)
    except Exception as e:
        print(f"World target click failed: {e}", flush=True)
        return False

    # Verify the list is no longer showing the exact target. A persistent exact
    # token means the click did not switch; do not blind-click it repeatedly.
    time.sleep(1.0)
    verify, vox, voy = ocr_game_window()
    if verify and find_world_target(verify, TARGET_WORLD, vox, voy):
        print(f"World {TARGET_WORLD} token remains visible; switch is unverified", flush=True)
        return False
    print(f"World {TARGET_WORLD} selection click verified by list dismissal", flush=True)
    return True


def main():
    global TARGET_WORLD
    parser = argparse.ArgumentParser()
    parser.add_argument('--timeout', type=int, default=120)
    parser.add_argument('--world', type=str, default=TARGET_WORLD,
                        help='Free world to switch to before clicking Play')
    parser.add_argument('--calibrate', action='store_true',
                        help='Open world list and hover over 308 position without clicking')
    parser.add_argument('--calibrate-switcher', action='store_true',
                        help='Test the world switcher click (bottom-left) to see if the list opens')
    parser.add_argument('--check-once', action='store_true',
                        help='Single non-blocking pass: dismiss disconnect dialog / click '
                             'CLICK HERE TO PLAY / Login / Play Now if visible, then exit. '
                             'Designed to be called every Supervisor cycle.')
    args = parser.parse_args()
    TARGET_WORLD = args.world

    if args.check_once:
        sys.exit(check_once())

    if args.calibrate_switcher:
        print("SWITCHER CALIBRATION: Testing world switcher click...", flush=True)
        bounds = get_game_window_bounds()
        if not bounds:
            print("Game window not found!", flush=True)
            sys.exit(1)
        gx, gy, gw, gh = bounds
        wx = gx + int(gw * 0.07)
        wy = gy + int(gh * 0.72)
        print(f"Clicking world switcher at {wx},{wy}", flush=True)
        print(f"Position: 7% from left, 72% from top", flush=True)
        pyautogui.moveTo(wx, wy, duration=0.5)
        print("Hovering over switcher position. Is this on the world number?", flush=True)
        time.sleep(5)
        print("Clicking now...", flush=True)
        pyautogui.click(wx, wy)
        time.sleep(3)
        print("Did the world list open? (yes/no)", flush=True)
        sys.exit(0)

    if args.calibrate:
        print("CALIBRATION MODE:", flush=True)
        print("1. Manually open the world list in the game (click the world number bottom-left)", flush=True)
        print("2. Then press Enter here...", flush=True)
        input()
        bounds = get_game_window_bounds()
        if not bounds:
            print("Game window not found!", flush=True)
            sys.exit(1)
        gx, gy, gw, gh = bounds
        bx = gx + int(gw * WORLD_308_X_PCT)
        by = gy + int(gh * WORLD_308_Y_PCT)
        print(f"Hovering at {bx},{by} - is this on world 308?", flush=True)
        print(f"Position: {WORLD_308_X_PCT*100:.1f}% from left, {WORLD_308_Y_PCT*100:.1f}% from top", flush=True)
        pyautogui.moveTo(bx, by, duration=0.5)
        print("Mouse is now hovering. Tell me: higher/lower/left/right?", flush=True)
        # Keep running so Julien can see
        time.sleep(30)
        sys.exit(0)

    pyautogui.FAILSAFE = True
    pyautogui.PAUSE = 0.1
    has_ocr = HAS_OCR

    deadline = time.time() + args.timeout
    print("Waiting for Microbot launcher...")
    no_window_attempts = 0

    while time.time() < deadline:
        native = native_status()
        if native and native.get('gameState') == 'LOGGED_IN':
            print('Login verified by live client state; OCR skipped', flush=True)
            return 0
        if native and (native.get('nativeLogin') == 'true'
                       or native.get('gameState') in ('LOGGING_IN', 'LOADING')):
            time.sleep(0.25)
            continue
        # Profile/account suffixes are allowed; use the shared flexible
        # discovery helper instead of requiring the literal title "RuneLite".
        windows = find_runelite_windows()

        if not windows:
            print("RuneLite game window not open yet, waiting...", flush=True)
            time.sleep(3)
            continue

        game_win = windows[0]
        print(f"Found game window: {game_win.title}", flush=True)

        # Wait for the game to actually load: the window must be a reasonable
        # size (not the tiny 360x520 launcher placeholder)
        bounds = get_game_window_bounds()
        if bounds:
            gx, gy, gw, gh = bounds
            if gw < 600 or gh < 400:
                print(f"Game window too small ({gw}x{gh}), not loaded yet, waiting...", flush=True)
                time.sleep(5)
                continue
            print(f"Game window loaded: {gw}x{gh}", flush=True)

        # Bring the game window to the front so clicks land
        try:
            game_win.activate()
            time.sleep(1)
        except Exception as e:
            print(f"Activate note: {e}", flush=True)
        region = None  # full screen

        if True:
            try:
                print("Taking screenshot...", flush=True)
                screenshot = pyautogui.screenshot()
                w, h = screenshot.size
                region = (0, 0, w, h)
                print(f"Screenshot taken: {screenshot.size}", flush=True)
            except Exception as e:
                print(f"Screenshot failed: {e}")
                time.sleep(2)
                continue

            if has_ocr:
                try:
                    print("Running OCR...", flush=True)
                    # Crop to game window for accurate coordinates
                    bounds = get_game_window_bounds()
                    if bounds:
                        gx, gy, gw, gh = bounds
                        game_crop = screenshot.crop((gx, gy, gx + gw, gy + gh))
                        data = ocr_image_data(game_crop)
                        # Offset coordinates by game window position
                        coord_offset_x, coord_offset_y = gx, gy
                    else:
                        data = ocr_image_data(screenshot)
                        coord_offset_x, coord_offset_y = 0, 0
                    print(f"OCR done, found {len(data['text'])} text elements", flush=True)

                    exit_dismiss = find_exit_modal_dismiss(data, coord_offset_x, coord_offset_y)
                    if exit_dismiss:
                        x, y, label = exit_dismiss
                        print(f"Exit modal detected, clicking {label} at {x},{y}", flush=True)
                        pyautogui.moveTo(x, y, duration=0.3)
                        time.sleep(0.3)
                        pyautogui.click(x, y)
                        time.sleep(2)
                        continue

                    # NEW: the client can boot straight into the disconnect
                    # dialog (seen 2026-09-28 02:23). Handle it before the
                    # login logic so we don't time out staring at it.
                    dlg = find_disconnect_ok(data, coord_offset_x, coord_offset_y)
                    if dlg:
                        print(f"Disconnect dialog detected, clicking Ok at {dlg[0]},{dlg[1]}", flush=True)
                        pyautogui.moveTo(dlg[0], dlg[1], duration=0.3)
                        time.sleep(0.3)
                        pyautogui.click(dlg[0], dlg[1])
                        time.sleep(3)
                        continue
                    chtp = find_click_here_to_play(data, coord_offset_x, coord_offset_y)
                    if chtp:
                        print(f"CLICK HERE TO PLAY detected, clicking at {chtp[0]},{chtp[1]}", flush=True)
                        pyautogui.moveTo(chtp[0], chtp[1], duration=0.3)
                        time.sleep(0.3)
                        pyautogui.click(chtp[0], chtp[1])
                        time.sleep(3)
                        continue

                    # Look for Login or Play Now directly.
                    # (Skip the "Welcome" check - the RuneScape font is hard for OCR.
                    #  Finding Play Now proves the game is loaded.)
                    # Collect all candidates, then pick the best (largest text = the button)
                    targets = []
                    for i in range(len(data['text'])):
                        text = data['text'][i].strip().lower()
                        conf = int(data['conf'][i])
                        if conf < 50:
                            continue
                        if text == 'login':
                            # Score by text height (larger = more likely the button)
                            h = data['height'][i]
                            targets.append((i, 1, h))
                        elif text == 'play' and i + 1 < len(data['text']) and data['text'][i+1].strip().lower() == 'now':
                            h = data['height'][i] + data['height'][i+1]
                            targets.append((i, 2, h))
                    # Prefer Login over Play Now, then largest text
                    targets.sort(key=lambda t: (t[1], -t[2]))
                    if not targets:
                        print("No Login/Play Now found yet, waiting...", flush=True)
                        time.sleep(3)
                        continue
                    if targets:
                        i = targets[0][0]
                        x = coord_offset_x + data['left'][i] + data['width'][i] // 2
                        y = coord_offset_y + data['top'][i] + data['height'][i] // 2
                        # For "Play Now", click between the two words
                        if targets[0][1] == 2 and i + 1 < len(data['text']):
                            x2 = coord_offset_x + data['left'][i+1] + data['width'][i+1] // 2
                            y2 = coord_offset_y + data['top'][i+1] + data['height'][i+1] // 2
                            x = (x + x2) // 2
                            y = (y + y2) // 2
                        print(f"Found {len(targets)} candidates, picking largest at {x},{y}", flush=True)
                        label = "Login" if targets[0][1] == 1 else "Play Now"
                        if targets[0][1] == 2:
                            # Before clicking Play Now, make sure we're on the target free world
                            if not switch_world():
                                time.sleep(0.5)
                                continue
                            # Re-take screenshot after world switch (coordinates may be stale)
                            # Crop to game window again
                            screenshot = pyautogui.screenshot()
                            bounds2 = get_game_window_bounds()
                            if bounds2:
                                gx2, gy2, gw2, gh2 = bounds2
                                game_crop2 = screenshot.crop((gx2, gy2, gx2 + gw2, gy2 + gh2))
                                data = ocr_image_data(game_crop2)
                                coord_offset_x, coord_offset_y = gx2, gy2
                            else:
                                data = ocr_image_data(screenshot)
                                coord_offset_x, coord_offset_y = 0, 0
                            targets = []
                            for i in range(len(data['text'])):
                                text = data['text'][i].strip().lower()
                                conf = int(data['conf'][i])
                                if conf < 50:
                                    continue
                                if text == 'play' and i + 1 < len(data['text']) and data['text'][i+1].strip().lower() == 'now':
                                    h = data['height'][i] + data['height'][i+1]
                                    targets.append((i, 2, h))
                            targets.sort(key=lambda t: -t[2])
                            if not targets:
                                print("Play Now not found after world switch, retrying...", flush=True)
                                time.sleep(2)
                                continue
                            i = targets[0][0]
                            x = coord_offset_x + data['left'][i] + data['width'][i] // 2
                            y = coord_offset_y + data['top'][i] + data['height'][i] // 2
                            if i + 1 < len(data['text']):
                                x2 = coord_offset_x + data['left'][i+1] + data['width'][i+1] // 2
                                y2 = coord_offset_y + data['top'][i+1] + data['height'][i+1] // 2
                                x = (x + x2) // 2
                                y = (y + y2) // 2
                        print(f"Clicking {label} at {x},{y}", flush=True)
                        # One OCR-derived click, then reacquire and verify.  Do
                        # not spray fixed offsets: if the control remains,
                        # the next polling cycle gets a fresh OCR position.
                        print(f"Clicking {label} once at {x},{y}", flush=True)
                        pyautogui.moveTo(x, y, duration=0.3)
                        time.sleep(0.3)
                        pyautogui.click(x, y)
                        verify_deadline = time.time() + 3.0
                        verified = False
                        while time.time() < verify_deadline:
                            time.sleep(0.35)
                            verify_data, verify_ox, verify_oy = ocr_game_window()
                            if verify_data is None:
                                continue
                            if find_login_or_playnow(verify_data, verify_ox, verify_oy) is None:
                                verified = True
                                break
                        if verified:
                            print("Login control changed; checking next screen", flush=True)
                            if native_status() is None:
                                return 0
                            continue
                        print("Click not verified; returning to OCR polling without a blind retry", flush=True)
                        time.sleep(1)
                        continue
                except Exception as e:
                    print(f"OCR error: {e}")
            else:
                # Keyboard fallback: assume Login is focused, press Enter
                print("Pressing Enter (Login)...")
                pyautogui.press('enter')
                print("Done! (keyboard)")
                sys.exit(0)

        time.sleep(2)

    print("TIMEOUT: Login button not found")
    sys.exit(1)

if __name__ == '__main__':
    main()
