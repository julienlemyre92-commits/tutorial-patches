# 2026-10-07 12:45 EDT — stream shows Cook's Assistant BUILD 519; conflicts with the TI feed's tutorial-mission claim (read-only review note)

## Observed (frame-decoded stream check, 12:37 EDT, https://www.youtube.com/watch?v=5oVGB4psHuY — LIVE, Bumba, 1 watching)
- Real OSRS gameplay decoded: Lumbridge-area wheat field / cow pen / chicken / goblins; the character walks a green waypoint path; minimap shows Lumbridge.
- In-game tooltip (rendered): "Walk here / DIAG: WALL/DOOR / player=(3162,3284,0) r=10 / id=153% @ (3163,3284) actions=[not read]".
- Overlay panel (frame-decoded, verified as rendered on screen — still a panel lead, not runtime lines):
  - CURRENT MISSION: "Cook's Assistant"
  - QUEST PROGRESS (WAITING): "Cook's Assistant" — "Awaiting game checkpoints"
  - RUNTIME BUILD: "BUILD 519" (panel text: "VERIFIED IN CLIENT")
  - GAME ITERATION: "Unknown" (LIVE SCRIPT STEP)
  - LAST BUILD: "2 min" (panel text: "FIRST OBSERVED IN CLIENT") → ~12:35 EDT
  - LATEST VERIFIED CHECKPOINT: "Awaiting checkpoint" (WAITING FOR GAME EVIDENCE)
  - Alex worklog card: "The quest hit a real gate blocker: it reached the whea…" (truncated)
- "13 QUESTS RECORDED COMPLETE" panel unchanged — remains an UNVERIFIED panel lead; verified tally stays 11 quests / 33 QP.

## Conflict with the TI diag feed
The `screenshots/` diag feed (mystery TI client) in the same window shows, verified from runtime lines:
- Client/script restarted 12:25:01 (startup block, builds 390→418 jar banner); single Build 194 "logged out 0 min" at 12:25:02, then no further logged-out lines (launcher clicker re-logged).
- 12:25:53–12:26:01: Build 392 MISSION_SELECT ran as the first post-login phase, claimed tutorial ownership ("otherFound=true, otherWasEnabled=false"), SWITCH COMPLETE, entering quest-state detection.
- 12:26:04 through 12:32:13: Build 393 "parked DONE — quest logic held, housekeeping alive"; Build 300 creatorBinding NOT-creator checks every ~2-3s; intermittent npcReachable(Gielinor Guide) timeouts. No Cook's Assistant lines in this feed.

## Inference (mechanism, open)
These cannot be the same JVM: the TI feed's JVM claims tutorial ownership with the Cook plugin stopped, while the stream shows Cook's Assistant actively pathfinding. Therefore the streamed client and the mystery TI client are DIFFERENT clients. This withdraws the 12:24 run's correlation ("streamed client IS the mystery TI client") — that was overclaimed; the 12:24 Lumbridge gameplay scene was consistent with either client and the identity was never proven.

## Candidate leads for Alex
1. Cook's Assistant Build 519 appears blocked at the Lumbridge wheat-field area: player (3162,3284,0), DIAG: WALL/DOOR tooltip at (3163,3284), worklog "hit a real gate blocker: it reached the whea…". The grain leg (pick grain → windmill mill) is the natural suspect; the tooltip suggests the walker is routing into a wall/door tile near the field.
2. The mystery TI client's 12:25:01 restart trigger is unproven from evidence: bot-command/command.txt still holds only the stale 2026-09-29 STATUS command (no RESTART issued), and the Build 194 logged-out watchdog cannot explain it (the client was logged in at the 12:24 stream check). Diag commits missed at 16:24/16:25 UTC, consistent with the client being briefly down.
3. "LAST BUILD 2 min (FIRST OBSERVED IN CLIENT)" at 12:37 vs version.txt=1116 (unchanged, sha 42ba9642) — repository version domain and runtime build domain disagree as usual; do not mix them.
4. Quest-state watch: "Awaiting checkpoint" + "13 QUESTS RECORDED COMPLETE" panel — unchanged, unverified; verified tally stays 11/33 until game-verified evidence lands.
