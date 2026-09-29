# Infrastructure files (NOT plugin patches)

These files run **on Julien's PC**, not in the game. They are NOT delivered
through the plugin auto-update pipeline (`patches/` + `version.txt`) — that
pipeline only injects into the Microbot jar.

## Install

1. Download `launcher_clicker.py`, `Supervisor.bat`, and `Check-Update.ps1`
   from this folder.
2. Copy them into your bot bundle folder (where the current ones live),
   overwriting the old files.
3. Restart the Supervisor (close the Supervisor window, double-click
   `Supervisor.bat` again). `launcher_clicker.py` is re-read fresh on every
   call, but `Supervisor.bat` changes need the restart.

## What changed (2026-09-29) — quiet Supervisor + visible heartbeat

**The problem (Julien):** the Supervisor window showed a wall of text every
30s (`Checking for patches...`, `Local version: 386`, `Remote version: 386`,
`Already up to date.`, `Found Tesseract at: ...`, `check-once: nothing to
do`) — noise. But the window must stay alive and visibly prove it is
working, so Julien and the stream can see it at a glance.

**The fix:**
- `Supervisor.bat` wait loop no longer echoes the per-cycle check headers.
  It prints ONE heartbeat line per cycle:
  `[date time] Supervisor OK | patch 386 | in-game | next check 30s`
  (or `parked at login (intentional)` while the logout sentinel exists).
  Anything that ACTED — new patch, login click, restart, error — still
  prints its own line above the heartbeat.
- `Check-Update.ps1` gained `-Quiet`: silent when up to date (exit 0),
  still loud on new patch (exit 2) / failure (exit 1).
- `launcher_clicker.py` gained `--quiet`: in `--check-once` mode it prints
  only when it actually clicks/dismisses something; the per-cycle no-op
  lines are silenced. Real actions still print.
- `Check-Update.ps1` is now distributed from this folder (it was previously
  bundle-only), so all three files update in one copy.

## What changed (2026-09-29) — intentional-logout stand-down

**The problem:** Build 388 taught the bot to log out when Tutorial Island
completes, so the account would park at the login screen. The logout worked
(stream showed the login screen at ~11:38) — but the Supervisor's 30s
`--check-once` self-heal clicks CLICK HERE TO PLAY on ANY visible lobby and
can't tell an intentional logout from a disconnect, so the client logged back
in within a minute. A plugin-side logout would flap login/logout forever.

**The fix:** `Supervisor.bat` now checks for the sentinel file
`%USERPROFILE%\.runelite\bot-intentional-logout` before the `--check-once`
call. While the sentinel exists, the Supervisor stands down (no auto-login)
and the account parks at the login screen. The plugin writes the sentinel
right before an intentional completion logout, and deletes it on every fresh
startup — so standing down never sticks, and the normal launch-time login is
unaffected. **Julien: reinstall `Supervisor.bat` (steps above) when home, then
a follow-up plugin build re-enables logout-on-completion.**

## What changed (2026-09-28)

**The problem:** the Supervisor ran the login clicker exactly once at launch,
then only checked "is the game process alive?" every 30s. When the
"You were disconnected from the server." modal appeared mid-session, the game
was still "alive" so nothing dismissed it — the clicker had already exited.
The bot sat behind the dialog for ~3 hours.

**The fix:**
- `launcher_clicker.py` now detects the disconnect dialog (OCR looks for
  "disconnected" + an "Ok" button — both must be present, so it can't
  misfire during gameplay) and clicks Ok. It also recognizes
  "CLICK HERE TO PLAY". New `--check-once` flag: one quick non-blocking
  pass, designed to run every Supervisor cycle.
- `Supervisor.bat` now runs `launcher_clicker.py --check-once` every 30s in
  its wait loop, so a mid-session disconnect heals itself: Ok → login screen
  → Login click → back in game. No client restart needed.

## Files

- `launcher_clicker.py` — OCR login automation (pyautogui/pytesseract).
  One-shot mode at launch (`--timeout 180`), healing mode via `--check-once`.
- `Supervisor.bat` — patch check → launch → login click → 30s watch loop
  (game alive? new patch? login state OK?). Prints one heartbeat line per
  cycle (`Supervisor OK | patch N | in-game | next check 30s`).
- `Check-Update.ps1` — patch download/inject/validate. `-CheckOnly` for the
  30s loop (exit 2 = new patch), `-Quiet` to silence the up-to-date path.
