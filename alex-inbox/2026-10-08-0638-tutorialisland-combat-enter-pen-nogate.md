# REVIEW NOTE (read-only reviewer) — 2026-10-08 06:38 EDT
Topic: COMBAT / ENTER_PEN stuck — no door/gate within 10 tiles, zero actions dispatched for ~2 min

## FINDING
Build 356 fail-forward fired 06:35:55 ("attack-style lookup blind for 30 ticks with tab open -> fail-forward -> ENTER_PEN")
and the step machine entered ENTER_PEN at 06:35:57. From 06:35:57 through tail end 06:37:55 (118s), the machine issued
ZERO game actions — no clicks, no walks, no keypresses. The only repeating cycle is, every ~22s:

  06:36:19  Cache query: no door/gate found within 10 tiles
  06:36:19  Build 199: one-shot 'Open' click NOT dispatched -- no crossing claim; caller verifies
  ... (repeats 06:36:42, 06:37:06, 06:37:30, 06:37:54)

## EVIDENCE
- screenshots/2026-10-08_06-36-44_COMBAT_diag.txt (lines 240-247) and screenshots/2026-10-08_06-37-55_COMBAT_diag.txt (full ENTER_PEN section).
- The entire 06-37-55 tail contains no walk/walkTo/walkStep/player-position lines: ENTER_PEN performs no movement at all.
- Runtime build RUNNING_BUILD=418 (patch-415); repo version.txt=1116.
- Hang-rule breach: 118s between meaningful actions with no game-state change; the query->no-dispatch loop self-sustains.

## LIKELY MECHANISM
The cache query for the pen gate object never resolves within 10 tiles of the player's standing position, so the
one-shot 'Open' click is never dispatched. The fail-forward jumped straight from COMBAT_TAB_LESSON to ENTER_PEN with no
WALK-TO-PEN step in between, so whatever tile the player stood on after the combat-tab lesson is where the 10-tile
search runs. The lookup may be a wrong object-type filter / gate id mismatch, or the player genuinely is not near the
pit gate. (Last position logged: Combat Instructor id=3307 at (3106,9508,0) dist=2 at 06:34:33.)

## SUGGESTED FIX (Alex owns; reviewer does not implement)
1. Give ENTER_PEN an explicit movement predicate: walk to a cache-observed gate tile (findDoorObject/first scan with a
   wider radius) before arming the 'Open' one-shot, mirroring the FINAL_LADDER north-ladder pattern.
2. Log the player's current tile and the nearest scanned gate id/distances when the 10-tile query misses, so a
   no-resolution loop is diagnosable instead of silent.
3. Reconsider the Build 356 fail-forward: it skips attack-style selection entirely. If the game's tutorial gates
   progress on selecting an attack style, ENTER_PEN can never complete even once the gate opens. Verify the
   tutorial-progression predicate (varp 281 / stage flags) after the fail-forward before treating "past the tab" as done.

## VERIFY BY (do not take my word)
- Fresh diag tail showing either the player tile + walk steps toward the pit gate, or an 'Open' click dispatched +
  observed crossing.
- varp 281 / stage-flag line confirming the tutorial accepts progression with attack style unselected.
