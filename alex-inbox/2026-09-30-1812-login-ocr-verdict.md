# Verdict: login-surface recognition failure (Alex 2026-09-30 18:06 note) — read-only review

Filed 2026-09-30 ~18:14 EDT (Muse review-loop). No files edited, no builds, no UI operated, no credentials used.

## Scope caveat (important)

The running build's log vocabulary ("native state unavailable", "OCR scanning",
"no recognized action", "surface=other-text-N-tokens", redacted keyword classifier)
does **not** exist in the last repo copy I can read (`infrastructure/launcher_clicker.py`,
36743 bytes, identical to local `infra-stage/`). That copy is the older architecture
(`heal_screen()` branches, log line `CLICKER[check-once]: no actionable screen detected`).
So the exact functions below map to the reference copy; the running build is a newer
PC-side rewrite (same family as Alex's 2026-09-29 PC-only Back-anchor handler for the
members-world modal, which also never landed in the repo). If the new classifier moved
the matcher logic, treat function names as the conceptual paths, not exact symbols.

## Likely cause: vocabulary gap, NOT capture failure

Evidence in your own log: passes reach "OCR scanning" and produce 3–7 tokens.
- A capture failure would yield **0 tokens** or an exception line ("screenshot failed",
  "OCR failed", "no RuneLite/Jagex Launcher window") — none reported.
- Tokens exist but match none of {username, password, login, play, world} →
  the visible surface's wording is outside the keyword set. Same class as two prior
  incidents: 2026-09-28 messages-play screen (needed a 'message'+'play' branch) and
  the 2026-09-29 members-world rejection modal (needed a 'Back' anchor).
- N jittering 3–7 on a static screen additionally hints at Tesseract instability on
  this surface (gradient/animated launcher UI) — keywords may be *present but mangled*.

Most likely actual surfaces with 3–7 non-keyword tokens: Jagex Launcher SSO/account
interstitial (e.g. a "Continue" page), a world-full/worlds-down notice, a session-expired
page, or a transient error toast — i.e. the clicker is looking at the Launcher window,
not the RuneLite login canvas (reference: `check_once()` falls back to the Jagex
Launcher window when no RuneLite window is found).

## Evidence needed to distinguish the three cases

Add (or check for, in the new build) a per-pass **redacted dump** on every
"no recognized action" pass — no raw text:
- `n_tokens`, and per token: `(conf, w, h, x, y)` only
- `sha1` of the sorted, lowercased, whitespace-normalized token list (lets you match
  "same surface again" across passes without exposing text)
- which window supplied the crop (RuneLite vs Jagex Launcher title match)

Reading it:
- `n_tokens == 0` → capture failure (blank/minimized window, OCR engine missing)
- keywords absent but token count stable → vocabulary mismatch (add anchor)
- keywords present with conf < threshold (50 in reference `find_login_or_playnow`) →
  **confidence mismatch**; the `other-text-N` label is wrong, lower the floor or
  rescale that surface instead of adding keywords
- `n_tokens` jittering on an unchanged `sha1` → OCR instability; anchor on geometry
  (largest token height/width cluster), not on token text

## Minimal proposed patch

1. Add a `surface=unrecognized` redacted-fingerprint line (the tuple above) to the
   no-action log — one line, no raw OCR. This alone makes every future unknown
   surface tunable from logs.
2. Add one scoped anchor branch: a `continue`-like token (SSO continue page) or the
   two most likely anchors for the observed fingerprint, **scoped to the Jagex
   Launcher window only** (reference precedent: `check_once()` already distinguishes
   the windows). Do not add generic tokens to the game-canvas path — the exit-modal
   fix (Build 303) exists precisely because stray clicks are dangerous.
3. Keep the confidence floors where they are; do not lower globally to chase this one
   surface — that re-opens the stray-click class of bugs.

## Acceptance check (redacted data only)

- On the currently-stuck screen: one pass logs `surface=<new-anchor>` (or the new
  fingerprint) and either clicks it or reports the fingerprint — no raw text anywhere.
- No regression: on the known disconnect / messages-play / click-here-to-play /
  login screens, the same branches fire as before (run the existing branch order in
  `heal_screen()` against recorded redacted dumps of those screens).
- Feed: this unblocks WAIT_LOGIN; the dark screenshot feed (nothing since
  2026-09-30 17:44:02 EDT) should resume once the game logs in.

Open question I cannot answer read-only: whether the PC-side rewrite already has the
redacted dump (if so, send one dump line and the anchor is a 10-minute change).
