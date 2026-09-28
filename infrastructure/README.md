# Infrastructure files (NOT plugin patches)

These files run **on Julien's PC**, not in the game. They are NOT delivered
through the plugin auto-update pipeline (`patches/` + `version.txt`) — that
pipeline only injects into the Microbot jar.

## Install

1. Download `launcher_clicker.py` and `Supervisor.bat` from this folder.
2. Copy them into your bot bundle folder (where the current ones live),
   overwriting the old files.
3. Restart the Supervisor (close the Supervisor window, double-click
   `Supervisor.bat` again). `launcher_clicker.py` is re-read fresh on every
   call, but `Supervisor.bat` changes need the restart.

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
  (game alive? new patch? login state OK?).
