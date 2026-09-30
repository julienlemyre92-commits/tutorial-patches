# Pirate door-cross HOLD (Build 569 verified live, fix itself failed)

## Observed
- Build 569 / patch-568 ("recover Wydin room and cross door") shipped 16:09:52 EDT; version.txt=568, unchanged since.
- NEW typed runtime line in the in-game chatbox at 16:10:30 EDT (38s after ship) -- Build 569 confirmed running per the standing rule:
  `[Pirate'sTreasure] HOLD Wydin door opened three times without crossing: pos=WorldPoint(x=3011, y=3204, plane=0)`
- Player is OUTSIDE Wydin's grocery at (3011,3204,0); the "Select an option" menu is closed (Build 569 moved past the old menu site). HOLD latched since 16:10:30 with zero further action through 16:12:27 (session timer 00:52:43->00:54:14, game live, feed ~45s cadence).

## Defect
The door-recovery loop clicked "Open" 3 times but the player tile never crossed. The attempt counter appears to count door clicks, not verified player-tile crossings -- so it burned all retries without ever testing whether the player moved through, then latched a permanent HOLD.

## Requested check
1. Count a cross-door attempt only after an observed player-tile change to the other side (open + verified move, not open alone), and log the targeted door object id/tile so reviews can confirm the right door was clicked.
2. Confirm the menu-fragment gap from the 15:54 note ("can i work out front now" not in dialogue() allowed[]) is still in the code -- the employment menu will likely reopen once the door leg is fixed.

## Evidence
- Fresh screenshot (chatbox shows the new HOLD line): https://raw.githubusercontent.com/julienlemyre92-commits/tutorial-patches/main/screenshots/2026-09-30_16-12-27_PIRATESTREASURE_HOLD_auto.png
- Same state in the 16-10-57 and 16-11-42 frames.
- Build 569 commit: https://github.com/julienlemyre92-commits/tutorial-patches/commit/fd225da
