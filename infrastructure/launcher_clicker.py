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

2026-09-28 PM: the clicker used to SKIP the small (360x520) launcher window
("too small, not loaded yet") and wait for it to grow -- but it never grows
until someone clicks Play INSIDE it. Deadlock. Now the small window is OCR'd
and its Play/Login/messages-prompt buttons are clicked. Also handles the
second Play screen ("you have messages" prompt). Every branch logs explicitly
as CLICKER[tag]: branch=<name>.
"""

import sys
import time
import argparse

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

TARGET_WORLD = "308"  # Free world to use

# World 308 grid position in the world list (as fraction of game window)
# Adjust these if the click misses - run with --calibrate to see the position
WORLD_308_X_PCT = 0.09  # 9% from left (column 1)
WORLD_308_Y_PCT = 0.28  # 26.5% from top (was hitting 307, moved down half a row)


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
    data = pytesseract.image_to_data(screenshot, output_type=pytesseract.Output.DICT)
    return screenshot, data


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


def find_messages_play(data, ox, oy, min_conf=40):
    """Second Play screen: a 'you have messages' / 'messages' prompt with a
    Play button (seen 2026-09-28: the session sat at this screen with nobody
    clicking it). Requires a 'message'-containing token AND a standalone
    'play' token; returns the Play button center. Otherwise None."""
    texts = [(t or "").strip().lower() for t in data['text']]
    if not any('message' in t for t in texts):
        return None
    best = None
    for i in range(len(data['text'])):
        t = texts[i]
        try:
            conf = int(data['conf'][i])
        except (ValueError, TypeError):
            continue
        if conf < min_conf or t != 'play':
            continue
        h = data['height'][i]
        x = ox + data['left'][i] + data['width'][i] // 2
        y = oy + data['top'][i] + data['height'][i] // 2
        if best is None or h > best[2]:
            best = (x, y, h)
    return (best[0], best[1]) if best else None


def find_standalone_play(data, ox, oy, min_conf=40):
    """A lone PLAY/Play button (Jagex Launcher small window). Largest wins."""
    best = None
    for i in range(len(data['text'])):
        t = (data['text'][i] or "").strip().lower()
        try:
            conf = int(data['conf'][i])
        except (ValueError, TypeError):
            continue
        if conf < min_conf or t != 'play':
            continue
        h = data['height'][i]
        x = ox + data['left'][i] + data['width'][i] // 2
        y = oy + data['top'][i] + data['height'][i] // 2
        if best is None or h > best[2]:
            best = (x, y, h)
    return (best[0], best[1]) if best else None


def click_at(x, y, tag):
    print(f"CLICKER[{tag}]: clicking at {x},{y}", flush=True)
    try:
        pyautogui.moveTo(x, y, duration=0.3)
        time.sleep(0.3)
        pyautogui.click(x, y)
        return True
    except Exception as e:
        print(f"CLICKER[{tag}]: click failed: {e}", flush=True)
        return False


def heal_screen(data, ox, oy, tag):
    """Run every known login/launcher screen branch in priority order and
    click the first match. Returns the branch label, or None."""
    dlg = find_disconnect_ok(data, ox, oy)
    if dlg:
        print(f"CLICKER[{tag}]: branch=disconnect-modal -> Ok at {dlg[0]},{dlg[1]}", flush=True)
        click_at(dlg[0], dlg[1], tag)
        return "disconnect-modal"
    mp = find_messages_play(data, ox, oy)
    if mp:
        print(f"CLICKER[{tag}]: branch=messages-play-screen -> Play at {mp[0]},{mp[1]}", flush=True)
        click_at(mp[0], mp[1], tag)
        return "messages-play-screen"
    chtp = find_click_here_to_play(data, ox, oy)
    if chtp:
        print(f"CLICKER[{tag}]: branch=click-here-to-play at {chtp[0]},{chtp[1]}", flush=True)
        click_at(chtp[0], chtp[1], tag)
        return "click-here-to-play"
    lp = find_login_or_playnow(data, ox, oy)
    if lp:
        print(f"CLICKER[{tag}]: branch=login-or-play-now({lp[2]}) at {lp[0]},{lp[1]}", flush=True)
        click_at(lp[0], lp[1], tag)
        return "login-or-play-now"
    sp = find_standalone_play(data, ox, oy)
    if sp:
        print(f"CLICKER[{tag}]: branch=standalone-play at {sp[0]},{sp[1]}", flush=True)
        click_at(sp[0], sp[1], tag)
        return "standalone-play"
    print(f"CLICKER[{tag}]: no actionable screen detected", flush=True)
    return None


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
            data = pytesseract.image_to_data(crop, output_type=pytesseract.Output.DICT)
            return data, gx, gy
        data = pytesseract.image_to_data(screenshot, output_type=pytesseract.Output.DICT)
        return data, 0, 0
    except Exception as e:
        print(f"check: OCR failed: {e}", flush=True)
        return None, 0, 0


def check_once():
    """Single non-blocking pass: run every known login/launcher branch
    (disconnect modal, messages Play screen, CLICK HERE TO PLAY, Login /
    Play Now, standalone Play) and click the first match. Does nothing
    otherwise. Always exits 0 (a quiet pass is not an error)."""
    # Live title is e.g. "RuneLite - akjdghaweiog" (launcher appends the
    # profile name), so match case-insensitive containment, not exact title.
    windows = [w for w in pyautogui.getWindowsWithTitle("RuneLite")
               if "runelite" in w.title.strip().lower()]
    if not windows:
        # Fall back to the Jagex Launcher window itself.
        windows = [w for w in pyautogui.getWindowsWithTitle("Jagex Launcher")
                   if "jagex launcher" in w.title.strip().lower()]
    if not windows:
        print("CLICKER[check-once]: no RuneLite/Jagex Launcher window", flush=True)
        return 0
    if not HAS_OCR:
        print("CLICKER[check-once]: no OCR available", flush=True)
        return 0
    data, ox, oy = ocr_game_window()
    if data is None:
        return 0
    heal_screen(data, ox, oy, "check-once")
    return 0


def get_game_window_bounds():
    """Get the RuneLite game window bounds via Win32 API (more reliable than pygetwindow)."""
    try:
        import ctypes
        from ctypes import wintypes
        user32 = ctypes.windll.user32

        # Find window by title containment: live title is e.g.
        # "RuneLite - akjdghaweiog" (launcher appends the profile name), so
        # FindWindowW's exact match never hits. Enumerate visible windows.
        hwnds = []

        @ctypes.WINFUNCTYPE(ctypes.c_bool, wintypes.HWND, wintypes.LPARAM)
        def _enum(hwnd, _lparam):
            if user32.IsWindowVisible(hwnd):
                length = user32.GetWindowTextLengthW(hwnd)
                if length > 0:
                    buf = ctypes.create_unicode_buffer(length + 1)
                    user32.GetWindowTextW(hwnd, buf, length + 1)
                    title = buf.value.strip()
                    if "runelite" in title.lower() and "\\" not in title and "/" not in title:
                        hwnds.append(hwnd)
            return True

        user32.EnumWindows(_enum, 0)
        if not hwnds:
            return None
        hwnd = hwnds[0]

        rect = wintypes.RECT()
        if not user32.GetWindowRect(hwnd, ctypes.byref(rect)):
            return None

        return (rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top)
    except Exception as e:
        print(f"Window bounds failed: {e}", flush=True)
        return None


def switch_world():
    """Switch to world 308. The world list layout is fixed:
    308 is in column 1, row 8 (no scrolling needed)."""
    print(f"Switching to world {TARGET_WORLD}...", flush=True)
    bounds = get_game_window_bounds()
    if not bounds:
        print("Could not get game window bounds, skipping", flush=True)
        return True

    gx, gy, gw, gh = bounds

    # Step 1: Click the world switcher (bottom-left of game window)
    # Position: 7% from left, 72% from top (was 82%, still too low)
    wx = gx + int(gw * 0.07)
    wy = gy + int(gh * 0.72)
    print(f"Opening world list at {wx},{wy}", flush=True)
    try:
        pyautogui.click(wx, wy)
    except Exception as e:
        print(f"Click failed: {e}", flush=True)
        return True
    time.sleep(2.5)  # Wait for list to open

    # Step 2: Click world 308 at its fixed grid position
    # Column 1, row 8 in the world list (no scroll needed)
    # Refresh bounds in case window moved
    bounds = get_game_window_bounds()
    if bounds:
        gx, gy, gw, gh = bounds
    # 308 button center - adjustable via WORLD_308_X_PCT and WORLD_308_Y_PCT
    # Current: 9% from left, 33% from top (was 38%, moved up per feedback)
    bx = gx + int(gw * WORLD_308_X_PCT)
    by = gy + int(gh * WORLD_308_Y_PCT)
    print(f"Clicking world 308 at {bx},{by} ({WORLD_308_X_PCT*100:.0f}%, {WORLD_308_Y_PCT*100:.0f}%)", flush=True)
    try:
        pyautogui.moveTo(bx, by, duration=0.3)
        time.sleep(0.3)
        pyautogui.click(bx, by)
    except Exception as e:
        print(f"Click failed: {e}", flush=True)
    time.sleep(2)
    print("World switch attempted", flush=True)
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
    parser.add_argument('--dump-ocr', action='store_true',
                        help='Print every OCR token (conf>=30) with coordinates and exit. '
                             'Diagnostics for tuning the matchers on a stuck screen.')
    args = parser.parse_args()
    TARGET_WORLD = args.world

    if args.dump_ocr:
        data, ox, oy = ocr_game_window()
        if data is None:
            print("dump-ocr: OCR failed", flush=True)
            sys.exit(1)
        print("OCR DUMP (conf>=30):", flush=True)
        for i in range(len(data['text'])):
            t = (data['text'][i] or "").strip()
            try:
                conf = int(data['conf'][i])
            except (ValueError, TypeError):
                continue
            if not t or conf < 30:
                continue
            x = ox + data['left'][i]
            y = oy + data['top'][i]
            print(f"  [{conf:3d}] ({x},{y}) '{t}'", flush=True)
        sys.exit(0)

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
    pyautogui.PAUSE = 0.5
    has_ocr = HAS_OCR

    deadline = time.time() + args.timeout
    print("Waiting for Microbot launcher...")
    no_window_attempts = 0

    while time.time() < deadline:
        # Wait for the actual RuneLite game window. Live title is e.g.
        # "RuneLite - akjdghaweiog" (launcher appends the profile name), so
        # match case-insensitive containment. Still exclude file-path windows
        # (titles containing \ or /) and our own tooling.
        windows = []
        for w in pyautogui.getWindowsWithTitle("RuneLite"):
            t = w.title.strip()
            tl = t.lower()
            if "runelite" not in tl:
                continue
            if "\\" in t or "/" in t:
                continue  # Explorer/path window, not the game
            if any(s in tl for s in ("microbot-t", ".py", "muse", "chatgpt", "codex")):
                continue  # our own terminal/editor/chat window
            windows.append(w)
            break

        if not windows:
            print("RuneLite game window not open yet, waiting...", flush=True)
            time.sleep(3)
            continue

        game_win = windows[0]
        print(f"Found game window: {game_win.title}", flush=True)

        # The launcher can sit at a tiny 360x520 placeholder window that NEVER
        # grows on its own -- it needs Play/Login clicked INSIDE it (seen
        # 2026-09-28: the clicker waited on "too small" forever = deadlock).
        # So OCR the small window and heal it instead of skipping it.
        bounds = get_game_window_bounds()
        if bounds:
            gx, gy, gw, gh = bounds
            if gw < 600 or gh < 400:
                print(f"CLICKER[main]: small launcher window ({gw}x{gh}) - scanning inside it for Play/Login...", flush=True)
                try:
                    game_win.activate()
                    time.sleep(0.5)
                except Exception as e:
                    print(f"CLICKER[main]: activate note: {e}", flush=True)
                data, ox, oy = ocr_game_window()
                if data is not None:
                    heal_screen(data, ox, oy, "main/small-window")
                time.sleep(3)
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
                        data = pytesseract.image_to_data(game_crop, output_type=pytesseract.Output.DICT)
                        # Offset coordinates by game window position
                        coord_offset_x, coord_offset_y = gx, gy
                    else:
                        data = pytesseract.image_to_data(screenshot, output_type=pytesseract.Output.DICT)
                        coord_offset_x, coord_offset_y = 0, 0
                    print(f"OCR done, found {len(data['text'])} text elements", flush=True)

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
                        # OCR can't read the RuneScape font. Fallback: Play Now button
                        # is always centered in the game window, slightly above middle.
                        bounds = get_game_window_bounds()
                        if bounds:
                            gx, gy, gw, gh = bounds
                            fx = gx + gw // 2
                            fy = gy + int(gh * 0.42)
                            print(f"OCR found no text, using geometric fallback at {fx},{fy}", flush=True)
                            # Switch world first, then click
                            switch_world()
                            print(f"Clicking Play Now (fallback) at {fx},{fy}", flush=True)
                            pyautogui.moveTo(fx, fy, duration=0.3)
                            time.sleep(0.3)
                            pyautogui.click(fx, fy)
                            time.sleep(3)
                            print("Done! (fallback click)", flush=True)
                            sys.exit(0)
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
                            switch_world()
                            # Re-take screenshot after world switch (coordinates may be stale)
                            # Crop to game window again
                            screenshot = pyautogui.screenshot()
                            bounds2 = get_game_window_bounds()
                            if bounds2:
                                gx2, gy2, gw2, gh2 = bounds2
                                game_crop2 = screenshot.crop((gx2, gy2, gx2 + gw2, gy2 + gh2))
                                data = pytesseract.image_to_data(game_crop2, output_type=pytesseract.Output.DICT)
                                coord_offset_x, coord_offset_y = gx2, gy2
                            else:
                                data = pytesseract.image_to_data(screenshot, output_type=pytesseract.Output.DICT)
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
                        # Forceful click: move mouse there first, then click
                        # Try multiple times with small offsets in case of DPI issues
                        clicked = False
                        for attempt, (dx, dy) in enumerate([(0, 0), (0, 10), (0, -10), (10, 0), (-10, 0)]):
                            cx, cy = x + dx, y + dy
                            print(f"  Click attempt {attempt+1} at {cx},{cy}", flush=True)
                            pyautogui.moveTo(cx, cy, duration=0.3)
                            time.sleep(0.3)
                            pyautogui.click(cx, cy)
                            time.sleep(2)
                            # Check if Play Now is gone
                            verify_shot = pyautogui.screenshot()
                            verify_data = pytesseract.image_to_data(verify_shot, output_type=pytesseract.Output.DICT)
                            still_there = False
                            for k in range(len(verify_data['text'])):
                                if verify_data['text'][k].strip().lower() == 'play' and k + 1 < len(verify_data['text']):
                                    if verify_data['text'][k+1].strip().lower() == 'now':
                                        still_there = True
                                        break
                            if not still_there:
                                print(f"  Click worked on attempt {attempt+1}!", flush=True)
                                clicked = True
                                break
                            print(f"  Play Now still visible after attempt {attempt+1}", flush=True)
                        if not clicked:
                            print("WARNING: Play Now clicks did not register", flush=True)
                        print("Done!", flush=True)
                        sys.exit(0)
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
