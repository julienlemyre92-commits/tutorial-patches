# Pirate Build 570: NEW hold "Target absent from loaded scene: NPC line of sight after reachable approach wydin-job"

## Observed
- version.txt = 569 (Alex Build 570 / patch-569, shipped 16:20:13 EDT). Unchanged.
- Build 570's door-cross fix WORKED as a hot-reload: the 16:10:30 typed HOLD ("Wydin door opened three times without crossing; pos=(3011,3204,0)") cleared, and the 16-20-42 frame (~29s after ship) shows stage filename flipped to PIRATESTREASURE_RETRIEVE_SMUGGLED_RUM, player in the Wydin doorway area.
- 10 seconds later (16:20:52) a NEW typed hold line appeared in the chatbox:
  `[Pirate'sTreasure] HOLD Target absent from loaded scene: NPC line of sight after reachable approach wydin-job`
- Four consecutive frames at ~45s cadence (16-21-27, 16-22-13, 16-22-58, 16-23-43) all carry the PIRATESTREASURE_HOLD filename, the player is parked motionless in the Wydin doorway tile region, and the game session timer keeps ticking (01:03:59 -> 01:05:29, live). Zero bot actions for 3+ minutes after the new HOLD.

## Defect
The bot's routing reached the `wydin-job` leg, computed a reachable approach tile for the Wydin NPC, walked there, and then the NPC target scan came back empty -- and instead of retrying, it latched a permanent HOLD on the FIRST absent-target tick. Sequence in the 16-22-13/16-23-43 frames shows the OLD 16:10:30 door HOLD text still in chat history above the new line, so don't mistake history for a second latch.

Likely cause (from evidence, not assumption): the approach tile parks the player in the Wydin doorway while the store door is closed, and "NPC line of sight" fails because Wydin (inside the closed shop) is not in the loaded scene from that tile. The door-cross logic from Build 570 only runs inside its own leg -- the wydin-job leg appears to skip re-proving the door is open before scanning for the NPC. If the door re-closed after the bot crossed out (or the bot is on the wrong side of it), the scan can never succeed from there.

## Requested check
1. Gate the wydin-job NPC scan on being inside the store / door-open proof (reuse Build 570's proved-open tick), and re-prove crossing before the scan -- don't approach-then-scan from the doorway.
2. Consider making "target absent" a bounded retry with a re-approach (or an area/tile-object fallback) rather than an immediate permanent HOLD -- scene population lags a tick or two after door transitions.
3. Log the approach tile, the door open/closed observation, and the NPC-scan result on the tick the HOLD latches so a review can tell "wrong side of door" from "NPC genuinely missing".

## Side observation (host-side, not script)
The 16-23-43 frame's hot line shows:
`java.nio.file.FileSystemException: C:\Users\...\ .runelite\pirates-hot\status.properties: The process cannot access the file because it is being used by another process`
The hot-reload host couldn't write its status file -- two processes may be holding it (a stale host instance?). Hot-reloads have still been succeeding (Builds 569/570 both applied), but worth a look on the PC.

## Evidence
- 16-22-13 frame (new HOLD line in chatbox): https://raw.githubusercontent.com/julienlemyre92-commits/tutorial-patches/main/screenshots/2026-09-30_16-22-13_PIRATESTREASURE_HOLD_auto.png
- 16-23-43 frame (HOLD + FileSystemException): https://raw.githubusercontent.com/julienlemyre92-commits/tutorial-patches/main/screenshots/2026-09-30_16-23-43_PIRATESTREASURE_HOLD_auto.png
- 16-20-42 frame (pre-HOLD, HOLD cleared, stage resumed): https://raw.githubusercontent.com/julienlemyre92-commits/tutorial-patches/main/screenshots/2026-09-30_16-20-42_PIRATESTREASURE_RETRIEVE_SMUGGLED_RUM_auto.png
- Muse review-only; Alex owns the fix.
