## 2026-09-30 04:49 EDT (Muse review) -- GOBLIN DIPLOMACY COMPLETE, questState=FINISHED, +5 QP (16 total)
- Build 551 ACCEPTED from observed game state: 04-46-05/04-46-11/04-46-56 diags show build=551, pid=29068, world=301, varbit=6, questState=FINISHED, stage=DONE, error=none, all mail counts 0, health 100%.
- 04-46-56 PNG viewed directly: completion scroll verbatim "You have completed Goblin Diplomacy! ... 5 Quest Points, 200 Crafting XP, A Gold Bar. Total Quest Points: 16"; chatbox: "Congratulations, you've completed a quest: Goblin Diplomacy".
- Your 0446 note (SEEN): one continued run across builds/restarts, no manual gameplay; brown-mail hand-in 04:45:03. Monitoring STOPPED per your note; next quest plugin is yours when ready. Muse review-only, no edits/ships.

## 2026-09-30 04:39 EDT (Muse review) -- Build 549 LADDER FIX VERIFIED LIVE (mail 2->3), dyeing phase started
- diag 04-35-40: build=549, pid=37304 (Supervisor relaunched ~04:34-04:35 to pick up patch-547), world 301, stage=PREPARE_ORANGE, goblinMail=3, error=none, health 100%. diag 04-36-25: stage=WAIT_ROUTE_STOP, goblinMail=2, orangeMail=1, error=none.
- Your ladder-approach stabilization (patch-547, commit fd945c9) ACCEPTED from observed game state, not banner: the 04:30-04:32 oscillation (2953,3497)<->(2955,3497) without clicking is gone; mail=3 proves climb-up + upper crate Search completed. Then verbatim chatbox line (screenshot 04-36-25 viewed): "You dye the goblin mail orange." -- dyeing phase engaged, stage advancing.
- Bot healthy: zero failure lines in chatbox across both frames, error=none. Muse remains review-only per your 04-25 note. Next verification point: blue dye + mail delivery.

## 2026-09-30 04:31 EDT (Muse review) -- Build 548 WEST-CRATE FIX VERIFIED LIVE
- diag 04-29-40: build=548, pid=38512 (relaunched), world 308, stage=WALK_VILLAGE_LADDER_GROUND, error=none, goblinMail=2, orangeMail=0, blueMail=0, health 100%.
- Your alcove-entry fix (patch-546, commit ddad293) ACCEPTED from observed game state, not banner: mail 1->2 at ~04:27-04:29, stage past WEST_CRATE HOLD. The approach-tile-on-crate-tile bug is gone; exact-access approach (2951,3509) pattern worked as predicted.
- Bot now walking toward the village ladder leg; moving, healthy. Screenshot shows only the walker's debug overlay clutter + cosmetic cache-archive-hash client lines, no script failure lines.
- Muse remains review-only per your 04-25 note. Next verification point: orange-mail progress / continued movement.

## 2026-09-30 04:25-04:27 EDT (Muse review-loop) -- WEST_CRATE answer for Alex: approach (2951,3509), no door/gate, smallest change below

- Fresh 04-25-10 pair (viewed directly): diag still build=547, pid=16728, world 308, stage=HOLD, error=present, goblinMail=1 (north-crate mail held), varbit=3, questState=IN_PROGRESS. version.txt=545 via API (build=version+2, no banner lie).
- KEY NEW EVIDENCE: the bot moved since 04-24-25. Player is now at **(2951,3509,0)** -- exactly 1 tile NORTH of the west crate tile (2951,3508,0). Verbatim chatbox [04:24:07]:
  "ROUTE failure: walker segment ended without position progress attempt=2/3 at=WorldPoint(x=2951, y=3509, plane=0) target=WorldPoint(x=2951, y=3508, plane=0), bestDist=1"
  "HOLD Route WEST_CRATE failed after 3 attempts: walker target=WorldPoint(x=2951, y=3508, plane=0), at=WorldPoint(x=2951,y=3509,plane=0)"
- Diagnosis refined: it is no longer a fence/routing problem. The walker is targeting the CRATE'S OWN TILE (2951,3508) -- an object/wall tile -- with bestDist=1 and zero movement across 3 segments. This is the known door-tile failure class: walkTo/walkTarget must only ever target verified-walkable tiles. The route destination must be the approach tile, not the object tile.
- Precise reachable approach: **(2951,3509,0)**. Evidence it is valid: the player is standing on it live in the 04-25-10 screenshot (proof of walkability), it is distance 1 from crate tile (2951,3508), no door or gate between them (open compound ground; the earlier fence run-around is already complete -- the bot IS there). The Search interaction target stays the crate object 16560 at WEST_CRATE=(2951,3508) via object(new int[]{16560}, WEST_CRATE, 6).click("Search") -- same pattern as your north fix ("exact access (2960,3514), Search").
- Smallest code change (read-only review, no edits made by Muse): change WEST_CRATE's route() destination from WEST_CRATE=(2951,3508) to the exact-access tile (2951,3509) -- i.e. the same exact-access treatment north got -- then the existing searchCrate() interact proceeds. No door/gate transition needed; no palisade re-route needed. HOLD error=present will clear once arrival proves at (2951,3509) and the Search click issues.
- Acceptance (Muse watch list): goblinMail 1->2 (or west flag 0->1) in a fresh diag + visible mail pickup; never banner-only. Muse is review-only on this front -- no edits, no compile, no upload.

---
## 2026-09-30 04:24-04:25 EDT (Muse review-loop) -- Build 547 live: NORTH_CRATE FIXED (mail=1), but WEST_CRATE now HOLDs with the SAME fence failure

- Observed: 04-23-40 pair -> build=547, NEW pid=16728 (client restarted ~04:22), world 308, stage=WAIT_ROUTE_STOP, error=none, goblinMail=0. 04-24-25 pair (viewed directly) -> build=547, same pid, stage=HOLD, error=present, goblinMail=1. version.txt=545 via API (2-gap convention holds).
- Build 547's palisade route WORKED for the north crate: player searched it and picked up mail #1 (diag goblinMail=1 at 04-24-25). Congratulations -- the north-crate defect is CLOSED.
- NEW failure, same mechanism, different crate: route WEST_CRATE now HOLDs. Verbatim chatbox lines [04:24:07]:
  "progress attempt=2/3 at=WorldPoint(x=2951, y=3504, plane=0)"
  "failure=walker segment ended without position progress; target=WorldPoint(x=2951, y=3508, plane=0)"
  "HOLD Route WEST_CRATE failed after 3 attempts: walker segment ended without position progress; target=WorldPoint(x=2951, y=3508, plane=0), target=WorldPoint(x=2951, y=3508, plane=0)"
- Diagnosis: player parked at (2951,3504), 4 tiles south of the west-crate approach target (2951,3508), with palisade/fence walls between (visible in the 04-24-25 frame -- player inside the west section of the compound, target across the fence). The approach-tile planner again emits a bestList=1 target the walker's collision model cannot PATH to, the segment makes zero position progress across all 3 attempts, and HOLD latches. Same suggested fix as before: emit only walker-verified pathable approach tiles (route around the fence via its gap, not straight-line north), or fall back to reachability-aware interactObject() for the crate Search.
- Cosmetic note: the HOLD line prints target= twice ("..., target=WorldPoint(x=2951, y=3508, plane=0), target=WorldPoint(x=2951, y=3508, plane=0)") -- same message-format bug seen in the 545/546 HOLD lines.
- No ship from Muse (review-only). Acceptance stays: goblinMail=3 / stage past crate collection.

## 2026-09-30 04:22 EDT (Muse review-loop) -- HOLD stall: exact in-game walker-failure lines, Build 546 live but crate route still failing

- Observed: 04-21-25 HOLD pair (viewed directly). Diag: build=546, NEW pid=35560 (client restarted since 543's pid=7220), world 308, varbit=3, questState=IN_PROGRESS, stage=HOLD, error=present, goblinMail=0, health 100%. version.txt=545 via API (your cumulative counter; 2-gap convention).
- Exact chat-window evidence at [04:16:50]:
  "progress attempt=2/3 at=WorldPoint(x=2959, y=3513, plane=0)"
  "failure=walker segment ended without position progress attempt=3/3 at=WorldPoint(x=2959, y=3513, plane=0)"
  "HOLD Route NORTH_CRATE failed after 3 attempts: walker segment ended without position progress at=WorldPoint(x=2959, y=3514, plane=0), target=WorldPoint(x=2959, y=3514, plane=0)"
- Corroborates the fence-blocked approach-tile diagnosis: 1 tile of movement (3513->3514) then zero position progress across all 3 attempts. The stepToward steps are dying on the fence wall with bestList=1.
- Stall duration: HOLD has persisted since ~03:43 EDT (~39+ min). Diag feed is healthy (~45s cadence), so this is purely the route, not a dead client.
- No ship from Muse (review-only on your line); no source edits by me. Awaiting your NORTH_CRATE approach-tile fix. Acceptance signal unchanged: goblin mail count >0 / stage past HOLD.

## 2026-09-30 02:37 EDT (Muse review-loop) -- MINOR: DONE leaves the congratulations scroll open

- Observed: 7 successive DONE pairs 02-31-23 -> 02-36-48 (~5.5 min), all diags build=538,
  pid=28120, world 308, varp=100, questState=FINISHED, stage=DONE, error=none.
  The "Congratulations! You have completed Romeo & Juliet! / 5 Quest Points /
  Total Quest Points: 11" scroll has sat OPEN across all 7 (viewed directly:
  02-32-18, 02-33-48, 02-36-48) -- the bot never dismisses it.
- Parking-state cosmetic, NOT a hang: quest is FINISHED, nothing gates on the scroll.
- Suggestion: DONE should click away its own congratulations scroll (or move on) so the
  feed shows a clean parked state. Not ship-urgent.
- No ship from Muse (review-only on your line). version.txt=536 vs diag build=538
  (your 2-version patch/build gap persists -- the 02:29 PID/world-change relaunch
  proved the 538 classes are live).

## 2026-09-30 02:26 EDT (Muse review-loop) -- R&J STALL: Juliet "Urk!" continue frame not clicked (Build 537)

- Observed: 6 identical frames 02-21-27 -> 02-24-48 (~3.5 min, ~45s feed cadence, all real frames viewed directly):
  Juliet's balcony, dialogue OPEN with "Urk!" / "Click here to continue", UNHANDLED across all 6.
  Diag all 6: build=537, pid=38388, world 301, stage=HOLD, error=present, varp=50,
  questState=IN_PROGRESS, cadavaPotion=1, cadavaBerries=0 (byte-identical except timestamp).
- Milestone just before: POTION DELIVERED TO JULIET LIVE between 02-21-03 and 02-21-27
  (handover frame "You pass the suspicious potion to Juliet. Please wait..." at 02-21-03,
  stage=DELIVER_POTION, error=none).
- Blocker/cause: script dropped DELIVER_POTION -> HOLD with error=present exactly when the
  handover "Please wait..." frame resolved into Juliet's "Urk!" frame; the continue prompt
  has never been clicked. SAME SIGNATURE as the 02:06-02:08 varp-10 option-menu stall that
  needed your Build 537. Likely: DELIVER_POTION's completion predicate doesn't model the
  handover->"Urk!" transition, so the fresh frame hit the error path and latched HOLD.
- Recovery signal to watch: continue clicked, varp 50->60+, stage leaves HOLD.
- No ship from Muse (review-only on your line). version.txt=535 via API; repo commits since
  Build 537 (06:08:59Z) are screenshots only -- fix not yet shipped as of 02:26 EDT.

## VERIFIED 2026-09-30 01:33-01:34 EDT -- Rune Mysteries COMPLETE
diag build=535/pid=27624/stage=DONE/error=none, questState=FINISHED, varp=6.
PNG frames (292KB, real): in-game "Congratulations! You have completed Rune Mysteries!" scroll,
1 QP + air talisman + rune essence mine access; Total Quest Points: 6.
Zero-hang finish: WALK_TOWER_LADDER 01:31:46 -> turn-in -> DONE 01:33:16 (~90s).
The 01:25 Aubury tea-menu HOLD was the run's only snag -- your Build 535 closed it cleanly.
Bot now sits parked on the congratulation scroll, awaiting next instructions.

## 2026-09-30 01:31 EDT (Muse review-loop) -- VERIFIED LIVE: Build 535 cleared the Aubury tea-dialogue HOLD

> VERIFICATION: the 01:25:45->01:27:15 HOLD (unhandled "I'd love a cup of tea / No, thank you" option menu, stage=HOLD, error=present) is RESOLVED. Build 535 live in-game: fresh 01:30:16 frame viewed directly -- player OUTSIDE Aubury's shop walking south on the Varrock east road, red route line on minimap, NO dialogue open, research notes in inventory; diag tail: build=535, stage=WALK_TOWER_LADDER, error=none, researchNotes=1, varp=5, questState=IN_PROGRESS. Fix accepted from observed game state (new runtime stage lines + live movement), not banner alone. PID changed 12384->27624 and world 308->301 between 01:27:15 and 01:29:30 -- client relaunched on the patch-535 pull; first healthy moving frames at 01:29:30. Bot is on the return leg (tower ladder -> Sedridor); last mile of Rune Mysteries.

- Mechanism corroboration for the 01:27 HOLD writeup: 01:25:00 frame showed Aubury's handover dialogue ("Here, take these notes back to Sedridor...") with researchNotes=0; 01:25:45 frame showed the generic post-handover Aubury shop menu (tea option) with researchNotes=1 -- the re-talk happened between those frames while the inventory gate was still stale. Build 535's post-notes handling now disengages cleanly.
- Note: version.txt still reads 533 while patches 534/535 are committed and the bot runs 535 -- version.txt lags Alex's ship commits; worth syncing in the next ship to keep Supervisor/check-update counters honest.

---
## 2026-09-30 01:27 EDT (Muse review-loop) -- NEW HOLD: Aubury handover complete, then bot re-talked and errored on leftover option menu

> STATUS: quest advancing cleanly through 01:25:00 -- talisman delivered (varp 1->2 at 01:21-15), research package handed to Aubury (varp 2->3 at 01-22-00), walked the Varrock leg, and at 01:25:00 the Aubury notes dialogue is on screen ("Here, take these notes back to Sedridor..." / Click here to continue; diag: varp=5, stage=GO_AUBURY, error=none, researchPackage=0, researchNotes=0). By 01:25:45 the notes are in inventory (researchNotes 0->1) BUT the bot entered stage=HOLD with error=present. Fresh 01-25-45 screenshot viewed directly: player still inside Aubury's Rune Shop; dialogue panel shows an unhandled "Select an option" menu -- options: "I'd love a cup of tea." / "No, thank you." -- the bot re-talked to Aubury (or clicked Talk-to while the dialogue was closing) and the option picker errored to HOLD instead of disengaging.

- Defect hypothesis: after the handover completes (researchNotes observed = 1), the state machine talks to Aubury again / fails to close the option menu before the return leg, and an unhandled dialogue branch errors to HOLD. The quest is one step from done (notes -> Sedridor); the bot is stopped ~45s after receiving the final item.
- Concrete asks: (1) after observing researchNotes=1, never talk to Aubury again -- gate TALK_AUBURY on researchNotes==0; (2) add a disengage step (close dialogue / walk away) before starting the RETURN_SEDRIDOR leg; (3) log the HOLD trigger reason (stage + which dialogue/option set was open) into the diag .txt -- the terse field format (error=present, no text) makes HOLDs undiagnosable from the tail alone, as with the 01:13 Duke HOLD.
- Progression verified from observed game state (varp field + inventory counts + fresh frames), never banner-only. Build 534 still live (diag build=534, pid=12384, world 308; version.txt=532 is the tutorial pipeline counter, unchanged).

---
## 2026-09-30 01:16 EDT (Muse review-loop) -- NEW HOLD after Build 533 recovery: Duke dialogue open, no option picked, stage=HOLD error=present

> STATUS: The 01:10 CASTLE_STAIRS_UPPER route HOLD was cleared by Build 533's recovery (observed RECOVER_TALISMAN + re-engaged Duke dialogue at 01:13:00), but the plugin entered a NEW HOLD at 01:13:09 -- ~9 seconds later -- and is still holding at 01:14:30 (3 pairs: 01-13-09, 01-13-45, 01-14-30, ~45s cadence, feed live; Build 533, pid 12396, world 301, version.txt=531 unchanged).

- Observed state (from fresh screenshots viewed directly, 01-13-45 + 01-14-30 frames): player in Duke's quarters (plane 1); Duke Horacio 'Select an option' dialogue OPEN in both frames with the SAME two options the whole time: 'What did you want me to do again?' / 'Where can I find money?'. The bot opened this dialogue during RECOVER_TALISMAN at 01:13:00 and has NOT picked an option for ~5 minutes.
- Quest state is healthy: varp=1, questState=IN_PROGRESS, airTalisman=1 in inventory; inventory otherwise unchanged (257 coins, spade, shears, egg). Your blue diag-tile overlay + yellow compass-direction letters are on screen (Build 533's route-debug overlay, visible and working).
- This HOLD differs from the 01:10 one: no route-failure text is visible in the chatbox (the dialogue panel covers chat; game tab not readable). The diag files are the terse field format (stage=HOLD, error=present, no error text), so the trigger is NOT observable from the evidence -- I cannot tell whether the failure is the Duke-dialogue-option selection (dialogue open but no option picked for ~5 min is the visible signature) or a second route attempt that failed behind the open dialogue.
- HYPOTHESIS (labeled, not observed): the RECOVER_TALISMAN re-talk opened the Duke's 'Select an option' dialogue, the option picker failed to match/select the quest-resume option, and the plugin errored to HOLD with the dialogue still open. The visible signature (dialogue untouched for 5 min, zero player movement) fits an option-selection failure better than a walker failure.
- Nothing shipped from my side (review-only; no source touched, no version bump). Holding for your fix -- and if the plugin logs the HOLD reason into the diag .txt (not just error=present), that would make the next loop's diagnosis exact instead of inferred.

## 2026-09-30 01:10 EDT (Muse review-loop) -- RUNE MYSTERIES DEFECT: walker cannot reach CASTLE_STAIRS_UPPER, plugin parked in HOLD

> UPDATE 2026-09-30 01:13 EDT (Muse review-loop): Build 533 fix VERIFIED live. Bot relaunched (PID 25452->12396, world 301), stage RECOVER_TALISMAN re-derived from observed inventory (air talisman=1, quest IN_PROGRESS, varp=1), re-engaged Duke Horacio dialogue ('What did you want me to do again?' options) in fresh screenshot 01-13-00. CASTLE_STAIRS_UPPER HOLD is cleared; watching next runs for the Wizard's Tower walk leg as full proof of the route repair.

- Build 532 (patch-530) started the quest cleanly: Duke Horacio dialogue done (varp 0->1, questState=NOT_STARTED->IN_PROGRESS, airTalisman=1 visible in inventory). The quest start is NOT the problem.
- At 01:09:01 the plugin entered stage=HOLD on route CASTLE_STAIRS_UPPER and is still holding at 01:10:45 (4 pairs, ~1m45s, error=present every frame, pid 25452 unchanged). The quest cannot progress from here without a fix on your line.
- The chatbox in the 01-09-59 frame spells out the failure verbatim:
  - `ROUTE failure=walker segment ended without position progress attempt=3/3 at=WorldPoint(x=3211, y=3218, plane=1) target=WorldPoint(x=3204, y=3207, plane=1) bestDist=11`
  - `HOLD Route CASTLE_STAIRS_UPPER failed after 3 attempts: walker segment ended without position progress at=WorldPoint(x=3211, y=3218, plane=1), target=WorldPoint(x=3204, y=3207, plane=1)`
  - One earlier line in the same frame: `pathEndpoint=WorldPoint(x=3205,y=3209,plane=1) pathSize=0 endpointToTarget=2 threshold=1 routeMetrics=null totalUnreachable=3`
- Read from observed state only: the pathfinder found NO path (pathSize=0) from the Duke's quarters (player at 3211,3218,1) to the stairs target (3204,3207,1) — 3 attempts, zero position progress, the player never moved. This is a route/target-side failure, not a stuck pose or a click landing off-target. Suspects on your line: the target tile itself is unwalkable (stair-object tile rather than its adjacent walkable), or the Duke's-quarters exit needs an explicit via tile. (Same class of bug as the tutorial-island door-tile lesson: never route INTO an object/wall tile.)
- HOLD is doing its job (deliberate safe stop, no blind loop, same as the X Marks HOLD pattern) — but it is a genuine blocker, not a cosmetic one.
- Nothing shipped from my side (review-only; no source touched, no version bump). Standing down for your fix.
- Evidence: screenshots/2026-09-30_01-09-{11,14,59}_RUNEMYSTERIES_HOLD_auto.png (+ _diag.txt pairs), 2026-09-30_01-10-45_RUNEMYSTERIES_HOLD pair; diag build=532, world 308, varp=1, IN_PROGRESS.

## 2026-09-30 00:55 EDT (Muse review-loop) -- retroactive HOLD root cause + diag feed restored

- The uploader just backfilled ALL missing _diag.txt (00:37:50 -> 00:53:36) -- the diag gap since 00:37:54Z is closed, and with it the 00:41 XMARKS_HOLD mystery resolves retroactively: your Build 530 HOLD era (00:37:50-00:43:51) was build=530, pid 38052, questState=NOT_STARTED, varbit=0, **error=present** every frame, spade=0, coins=60. So the plugin was holding deliberately on an errored, unstarted quest -- a named, defensible state, not a walker fault. The ~00:44-00:46 client restart (pid 38052 -> 25448) + Build 531 pickup cleared it, and the dig-era diags show a clean run (varbit=6, ancientCasket=1, coins=57, error=none) all the way to FINISHED.
- Side bonus: the blue-checkerboard 3D-scene rendering glitch in the Sheep-era PNGs is GONE in the 00:53:36 frame -- it was PC-client-side and the restart cleared it. Worth knowing next time it shows up.
- 00:53:36 diag: build=531, varbit=8, questState=FINISHED, stage=DONE, error=none, coins=257, ancientCasket=0, spade=1. Quest-complete scroll still open ~2.5 min -- your DONE state's scroll dismissal logic (if any) may be worth a glance when you next touch the plugin, though it's cosmetic and deliberately terminal.
- Nothing shipped from my side (review-only). Next quest front is yours whenever you're ready.

## 2026-09-30 00:51 EDT (Muse review-loop) -- X MARKS THE SPOT COMPLETE

- Build 531 (patch-529) finished the quest ~00:51:10: the quest-complete scroll is open in-game ("Congratulations! You have completed X Marks the Spot!", 1 QP + Antique Lamp + 200 Coins + Beginner Clue Scroll Box, Total Quest Points: 5); chat confirms "Congratulations, you've completed a quest: X Marks the Spot" + 4 music tracks unlocked.
- The Veos turn-in needed 2 retries on dialogue:continue (chatbox diag: [XMarks] RETRY failure=1/3 at 00:49:57, 2/3 at 00:50:06) before succeeding -- your 3-strike retry logic earned its keep here. If the next quest's dialogues also take 2 strikes, a slightly more aggressive continue clicker might save a cycle.
- Verified by the game's own quest-complete scroll -- strongest acceptance signal. Nothing shipped from my side (review-only; your walker/dig/turn-in code did the job). Feed still PNG-only (no _diag.txt since 00:37:54Z); the chatbox-carried [XMarks] lines were enough this time.
- Stage is XMARKS_DONE now. Whenever you're ready with the next quest front, I'm watching.

## 2026-09-30 00:47 EDT (Muse review-loop) -- BUILD 531 PICKED UP, X Marks cold test is MOVING

- Client picked up Build 531 (patch-529) between 00:43:51 and 00:46:06: client relaunched (world 308 -> 301, fresh login 00:46:06 "Connecting to server..."), and the stage names moved off XMARKS_HOLD:
  * 00:46:06 XMARKS_WAIT_LOGIN (lobby, fresh session)
  * 00:46:21 XMARKS_WALK_VEOS_LUMBRIDGE (in-game at Fred's farm, route walking east toward Lumbridge, minimap route arrow visible)
  * 00:46:51 XMARKS_START_VEOS (at Lumbridge, Talk-to Veos menu open, Veos intro dialogue OPEN with player line "Great Kourend? Where's that?")
- The earlier XMARKS_HOLD ambiguity (no diag txt) resolved by itself: the HOLD era ended, the walker actually drove the route and reached Veos. No blind fix was needed -- good thing I didn't ship one.
- Acceptance status: stage-name change + first X Marks actions visible in-game. Full acceptance still = NEW [XMarks] runtime diag lines; diag-txt uploads remain PNG-only (00:37:54Z -> present), so I can't confirm the walker/route/proof diag strings yet. If you can re-enable the diag uploader PC-side, it would close the verification loop.
- Reminder: the 00:46:21 frame shows "Mismatch in overlayed cache archive hash for 12/223" client-console lines -- cosmetic client-side noise, not gameplay.

## 2026-09-30 00:46 EDT (Muse review-loop) -- Build 531 reviewed, NO CONCRETE DEFECTS found; watching for client pickup

- Saw your Build 531 (04:44:26Z, version.txt=529, patch-529.zip + XMarks-plugin-531.jar + source-review/build531-xmarks/). Root-cause match confirmed: the supervised walker design (cancel-before-action, wait-for-stop, thread-interrupt on separate clear thread) directly addresses the overlapping route clicks that caused Build 530's first-route failure/HOLD.
- Reviewed build531 XMarksScript.java (route/watchdog/cancel/recovery + observe()):
  * Route thread-handoff flags are correctly volatile (completed/completedAt/cancelRequested/clearError); bestDistance/lastProgress are tick-thread-only -- my volatile worry from the skim was wrong, threading is handled.
  * observe() runs on the client thread via Microbot.getClientThread().invoke(Supplier) -- Build 517 off-thread crash pattern NOT present in the frame capture.
  * Watchdog layers look right: 15s segment budget, 20s no-progress, 240s route budget, 8s recovery proof, 60s cancel backstop -> HOLD with named reason, 3-strike route failure -> nearby canvas recovery or HOLD. No inverted conditions or missing cancel paths spotted.
  * Cook turn-in-race lesson applied (20s hand-in latch on varbit 7/FINISHED); unknown dialogues -> HOLD after 10-20s, never guessed.
- Acceptance still = NEW runtime diag lines ([XMarks] ROUTE/PROVED/HOLD lines) from the live client, never the banner. Client hasn't picked up 531 yet as of 00:46 (version.txt=529; no new screenshots since 00-43-51). Will watch for the first WALK_/PROVED/ARRIVED lines + stage name change away from XMARKS_HOLD.
- No ship from this side (review-only on your releases); nothing here needed one anyway.

## 2026-09-30 00:41 EDT (Muse review-loop) -- X MARKS COLD TEST LIVE, plugin in HOLD, reason unknown, diag-txt feed interrupted

- PROFILE FLAG FLIPPED ~00:35-00:37: first XMARKS_HOLD frame 00-37-50 (committed 04:37:54Z), then 00-38-35, 00-39-20 (~45s cadence). Stage name proves Build 530's X Marks plugin tick is LIVE in-game (client picked up patch-528.zip + XMarks-plugin-530.jar). No 530 startup banner observed (no diag txt since 00-34-40) -- banner alone never counts anyway.
- HOLD since the very first X Marks frame; game itself is live (chat timer 00:01:07 -> 00:02:37 across frames, entities moving). Player still at Fred's farm / sheep-pen area; inventory unchanged (shears, egg, 60 coins). 3D-scene rendering glitch (blue checkerboard tiles, garbled entity text) persists -- cosmetic, PC-client side.
- DIAG GAP: uploader switched to PNG-only -- NO _diag.txt committed for any XMARKS_HOLD frame (SHEEP era always paired PNG+txt ~3s apart). HOLD reason is therefore unknowable from here. Candidates from Build 530 design: (a) exclusive-input guard holding via plugin-name check, (b) 3-strike pending-action failure, (c) unknown-dialogue 10-20s timer, (d) mission/quest-state gate. Needs your eyes on the live client.log / a diag dump.
- No ship from this side (review-only on your releases; blind fix without diag would violate the targeted-fix rule). If the HOLD is the exclusive-input guard, check what the mission-select gate sees; if it's an unknown dialogue, the PNGs show no dialogue widget open at 00-37-50 or 00-39-20.
- For Julien: the screenshot uploader on his PC stopped pairing diag txt files with the PNGs at 00:37:54Z -- worth a look at screenshot_uploader.py / the diag dump cycle when he's at the machine.

## 2026-09-30 00:23 EDT (Muse review-loop) -- Build 528 LIVE (patch-526.zip), quest still FINISHED, bot holding DONE

- Build 528 live in-game: diag build=528 (patch-526 counter, version.txt=526 -- the banner/counter drift persists, cosmetic). NEW PID 35064 after client restart ~00:20-00:21, world=308. Commit msg: "add bounded state-aware action pacing after proof".
- Quest state unchanged: questState=FINISHED, varp 179 = 21, wool=0, balls=0, shears=1, coins=60, error=none. 00:22:39 PNG: quest-complete scroll dismissed, player parked at Fred, 60 coins + Shears in inventory.
- 00:21:54 post-login screenshot was stage=WAIT_BLOCKING_EVENT (Microbot's own shouldBlockAndProcess hook, transient -- same signature as the 00:14:23 login earlier tonight); stage returned to DONE by 00:22:39. Not a regression.
- Pacing change is unobservable from here (bot is DONE/holding; no actions being issued). No further builds needed from this side -- Sheep Shearer is complete and verified on game state.

## 2026-09-30 00:17 EDT (Muse review-loop) -- SHEEP SHEARER COMPLETE: questState=FINISHED, varp=21, "Congratulations!" scroll observed

- Build 527 / patch-525 live (diag build=527; NEW PID 14280 after client restart ~00:14, world=308). Full quest chain verified END-TO-END: 20 balls spun (Build 526) -> Climb-down on staircase 16672 at (3204,3207,1) with plane 1->0 proof -> Fred return -> turn-in -> QuestState.FINISHED, varp 179 = 21. PNG 00:16:39 shows the quest-complete scroll: "Congratulations! You have completed Sheep Shearer! 1 Quest Point, 150 Crafting XP, 60 Coins. Total Quest Points: 4." Diag: balls=0 (turned in), shears=1, 60 coins, emptySlots=22, error=none, stage=DONE.
- The 00:13 walker-livelock defect is CLOSED: 527's removal of the pre-stair full-walker worked -- the single Climb-down click + 12s plane proof carried it through. One transient "I can't reach that!" game message in the 00:14:23 chat (fresh post-restart login) -- likely one out-of-reach Climb-down attempt before arrival; the retry path recovered it. No HOLD, no error state.
- version.txt=525. Banner-vs-counter drift persists (527 banner vs 525 counter, gap 2) -- cosmetic only; patch names track the counter.
- Chatbox mystery resolved: the hex-looking lines are the game client's own "Mismatch in overload cache archive hash for 12/223:" messages (appear after restart), not bot output. Verified the 527 script never writes to chat.
- Recurring intermittent: "[SheepShearer] status write: java.nio.file.AccessDeniedException: ...\sheepshearer\status.tmp -> ...\sheepshearer\status.properties" reappeared in game chat at 00:15:37 (absent at 00:03). Diag uploads still land, so a retry path covers it -- but the atomic move fails intermittently, possibly the uploader holding the file open. Worth a bounded retry/backoff on the status write if you touch that code again.
- Stage DONE (quest-complete scroll open; the script's DISMISS_PRIOR_QUEST_SCROLL path should ESC it). Sheep Shearer needs no further builds. Next quest call is Julien's.

## 2026-09-30 00:08 EDT (Muse review-loop) -- Build 525 walk fix VERIFIED LIVE; Build 526 spinning, balls=15/20
- Build 525 live in-game (diag build=525 banner+diag agree; PID 37944 after client restart ~00:02-00:03, world=308). Stage moved SHEEP_HOLD -> SPIN_WOOL -- the 12-minute HOLD fully cleared. The 23:59 walk-goal defect is CLOSED: player reached the wheel room, Spin clicked on wheel object 14889, and the "How many would you like to spin?" quantity widget was open at 00:03:53 and 00:04:38 (observed in PNGs).
- Build 526 live by 00:06:53 (diag build=526; NEW PID 14872, restart+relogin to world=301). Spinning confirmed IN PROGRESS: diag wool=5, balls=15, error=none, varp=1, IN_PROGRESS; PNG shows player at the wheel, quantity widget closed, 15 balls of wool in inventory, 5 wool remaining. The guarded product click fired -- the ~45s the widget sat open (00:03:53-00:04:38) was normal batch cadence, not a stall.
- Expected next: balls reach 20/20 -> stage should advance past SPIN_WOOL. Watch items: none open. Banner-vs-version.txt drift persists (526 banner vs 524 counter, gap 2) -- cosmetic only, patch names track the counter.

## 2026-09-29 23:59 EDT (Muse review-loop) -- Build 524 LIVE, walk:SPINNING_WHEEL FAILED 3/3, still HOLD
- Build 524 live in-game: diag build=524 (banner + diag agree), NEW PID 35488 (client restarted ~23:57-23:58; lobby frame 23:57:53 "CLICK HERE TO PLAY" was the restart+relogin, back in game by 23:58:04 via the auto-login clicker).
- Alex's 524 fix ("route to spinning wheel and guard product click") changed open:wheel -> walk:SPINNING_WHEEL. Live result: three attempts at 23:58:04 / 23:58:19 / 23:58:33, each preceded by "WebWalk clear" -- all three "Unproved walk:SPINNING_WHEEL", then HOLD re-entered 23:58:33. Diag: varp=1, wool=20, balls=0, error=present, pos=WorldPoint(x=3207, y=3212, plane=1) -- SAME tile as the 23:54 open:wheel failures. Player never moved.
- Defect analysis (concrete, for the fix): the walk goal is very likely the wheel's own object tile (unwalkable) -- WebWalk "clears" but the arrival proof can never pass, so the player stands 2 tiles away and the retry counter just counts up. Same signature as the chef-door/rat-pit class: walkTo must target an ADJACENT WALKABLE tile of the object (adjacentWalkable), never the object tile itself; verify by observed player tile (dist<=2 of wheel), then issue Spin. The "guard product click" part of 524 can't be judged until the wheel is actually reachable.
- version.txt=522 via API while the banner says 524 (drift continues; patch numbering is off by one in the other direction too -- Build 524 shipped as patch-523). A fix is only "live" when its NEW runtime lines appear, which this run confirms; the counter drift is bookkeeping-only but risks a future version collision.
- 23:57-run's status-write AccessDeniedException did not reappear in this run's chat window (window only goes back to 23:58:04) -- status quo unknown, watch next run.

## 2026-09-29 23:57 EDT (Muse review-loop) -- SHEEP_HOLD parked + status-write failure
- Bot still Build 523 (diag build=523, PID 20184), stage SHEEP_HOLD, wool=20, balls=0, pos=(3207,3212,plane=1), varp=1, questState=IN_PROGRESS. HOLD since 23:54:20; unchanged through 23:55:38 shot.
- NEW diag line [23:55:27]: status write FAILED -- java.nio.file.AccessDeniedException: C:\Users\No 1\runelite\sheepshearer\status.properties -> status.tmp. The status/tmp rename is denied; remote-command/status channel may be degraded. Screenshot+diag uploads still working, so uploader is fine -- this is the in-game status.properties write.
- version.txt bumped 521 -> 522 during this run (new patch uploaded) but running build still reports 523. If 522 was yours, the running build (523) didn't hot-load it or version bookkeeping drifted again.
- The open:wheel reachability defect from the 23:55 note is still the live blocker: no new fix lines observed yet.

## SHEEP SHEARER HOLD 23:54 EDT -- defect report (Muse review-only, no ship)
- **Live state (verified from screenshots + diag tails, Build 523 banner, PID 20184):** SPIN_WOOL attempted `open:wheel` (action=open:wheel) at 23:53:56, 23:54:08, and 23:54:20 -- all three failed with the game's `I can't reach that!` text. Player parked at **WorldPoint(3207, 3212, plane=1)** the whole time. After 3/3 failures the script entered **SHEEP_HOLD "Unproved open:wheel after 3 attempts"** (diag: varp=1, wool=20, balls=0, shears=1, error=present). Gameplay is parked; only diag/screenshots continue. This needs a fix build before the run can proceed.
- **Root cause (concrete, from observed state):** the wheel IS visible and the virtual mouse reaches it (the 23:54:53 screenshot shows the `Spin Spinning wheel / 2 more options` hover tooltip), but the click is dispatched from a tile where the game reports the wheel unreachable. The retry loop re-clicks without moving -- so failures 2 and 3 were guaranteed before attempt 1 even resolved. Same defect class as the Cook's bank-walk, rat-pit gate, and chef-door issues: **action dispatched before arrival; reachability not re-verified at click time.**
- **Fix needed in SheepShearerScript:** before issuing the wheel click, walk to an adjacent walkable tile of the wheel (adjacency probe / findLadderObjectInner-style, like the FINAL_LADDER rule) and verify `Rs2Tile.isTileReachable`-style adjacency at click time; inside the retry loop, re-walk closer BEFORE re-clicking, not just re-click. Also consider that the "Spin" click currently goes through whatever `open:wheel` issues -- if it uses Rs2Walker.walkTo directly at the wheel's tile, doors/stairs-adjacent tiles are walls and freeze the walker (known class: door tiles are walls, walkTo freezes; use adjacentWalkable + walkStep, arrival verified by observed tile).
- **Version drift note:** version.txt = 521 while the live banner says build=523 -- 2-behind drift continues (was 520/522 at review). No action, just noting the tag isn't the release sequence of record.
- **Non-blocking review note stands:** SPIN_PROGRESS 12s timeout < full-batch spin time (~20 wool x ~1.2s/ball), so expect spurious RETRY noise once spinning actually starts; consider ~40s timeout.

---
## Muse review: Restless Ghost Build 521 (2026-09-29 23:28 EDT, review-only -- no ships)
- **Verdict: no blocking defects found.** Reviewed `source-review/build521-restlessghost/RestlessGhostScript.java` against installed microbot-base.jar bytecode (javap).
- **Off-client-thread queryable concern = non-issue.** `AbstractEntityQueryable.nearestOnClientThread()` internally does `Microbot.getClientThread().invoke(Supplier)` (blocking hop) -- your `npc()`/`object()` calls in `observe()`/`stageTwo..Four` are thread-safe off the tick thread. NOT the Build-517 crash class. `Rs2Walker.walkTo(WorldPoint,int)` signature confirmed present.
- **State-machine checks passed:** Talk-to only issued when no dialogue open (dialogue() guard runs first each stage -- matches the no-dialogue-reset rule); turn-in latch anchors the 20s grace to first observed skull loss (turnInObservedAt) + polls `QuestState.FINISHED` on the client thread, bounded 3 uses -> hold; proof-based issue/verify with bounded 3-attempt retries -> hold; `missingScene()` 10s -> hold; varp gating 0-4 with hold on unknown.
- **Minor, non-blocking notes:** (a) `issue()` sets `pending` even when the action supplier returns false -- burns one timeout+retry; cosmetic. (b) `Proof.WALK` accepts "moved closer" -- weak but self-correcting (re-issues next tick). (c) `Proof.FINISH` can be "proved" by any chatbox text change -- self-corrects via stageFour re-run. (d) **Ghost plugin has no bot-command channel handler** -- Muse's STATUS ping (id=muse-status-20260929-2322) sits pending; only Tutorial/Cook scripts ack STATUS. If you want remote PAUSE on Ghost, add the ~45s command.txt poll.
- **Live state 23:24:31 EDT:** GHOST_EXIT_BASEMENT, skull (553) visible in inventory, player walking a tile-marked route (red/blue numbered tiles 162-178 on bridge), "not enough energy to run" x2 = run depleted, walking -- benign. Route progressing toward church/coffin turn-in. No error dialogs.

## Current build
- **Build 463 / patch-459** (shipped 2026-09-29 18:38 EDT). STAGED RETURN ROUTE.
  - Alex 18:33: Milk step RESOLVED (Bucket x0, Bucket of milk x1 verified).
    Next defect: `SHARED_TRAVERSAL FAILED 'to the Cook'` after milk.
  - Diagnosis: direct stepToward(COOK_TILE) from dairy (3172,3317) fails --
    long-distance pathfinder cannot route to (3209,3214) directly.
    Same class as dairy outbound failure (Build 456).
  - Fix: staged return via validated anchors (reverse of outbound):
    DAIRY_PASTURE -> MILL_APPROACH -> WHEAT_FIELD -> COOK_TILE.
    Each leg is a short hop. Typed diagnosis on failure: RETURN_ROUTE_BLOCKED.
  - Dairy interaction preserved, untouched.
  - Acceptance: "return leg 1/3", "return leg 2/3", "return leg 3/3" then Cook dialogue.

## Current build
- **Build 462 / patch-458** (shipped 2026-09-29 18:35 EDT). TWO-STEP MILK WITH LOGGING.
  - Alex 18:32/18:33: Build 461 issued one action but no inventory delta.
    The direct Rs2Inventory.useItemOnObject helper may not be valid for
    special object 8689 (uses Rs2GameObject.interact by ID).
  - Fix: two-step with explicit logging.
    Step 1: Rs2Inventory.use("Bucket") -> log selection boolean.
    Step 2: milkTargetObject.click("Milk") on the validated object -> log click boolean.
    Both must be true to proceed to phase 4 (next-tick proof).
    If either false: HOLD. One-action latch preserved.
  - Acceptance: "Milk action issued (select=true, click=true)" then
    Bucket x0 / Bucket of milk x1 next tick.

## Current build
- **Build 461 / patch-457** (shipped 2026-09-29 18:30 EDT). OBJECT-DIRECT MILK.
  - Alex 18:27: Build 460 found MILK TARGET FOUND: Dairy cow(id=8689)@(3172,3317,0)
    but phase 3 searched NPC names and held after 5 scans.
  - Fix: phase 0 stores the validated milkTargetObject (id=8689). Phase 3 uses
    it DIRECTLY: checks live actions for "Milk". If present: one Bucket-on-object
    via click("Milk"), then phase 4 waits for next-tick Bucket x0 -> Bucket of milk x1.
    If no Milk action: log actions, HOLD with TARGET_NOT_FOUND. No NPC scan loop.
  - Acceptance: "phase 3 using stored OBJECT id=8689" + "OBJECT has live 'Milk'"
    + next-tick inventory proof.

## Current build
- **Build 460 / patch-456** (shipped 2026-09-29 18:25 EDT). PROXIMITY SHORT-CIRCUIT.
  - Alex 18:23: Build 459 at (3176,3320) incorrectly started leg 1/4 toward
    CHICKEN_FARM (3238,3298), walking AWAY from the dairy cow.
  - Fix: before staged route, if within 10 tiles of DAIRY_PASTURE (3172,3317)
    or DAIRY_PASTURE_ALT (3178,3322), SKIP all route legs and enter
    target-resolution immediately. Only use staged route when >10 tiles away.
  - No new waypoint added. Focused correction only.
  - Acceptance: RUNNING_BUILD=460 startup marker, plus when starting within
    10 of dairy, a direct TARGET audit (no "leg 1/4" or "ALTERNATE route" logs).

## Current build
- **Build 459 / patch-455** (shipped 2026-09-29 18:22 EDT). ALTERNATE ARRIVAL HANDOFF.
  - Alex 18:20: Build 458's alternate reached (3176,3320) but repeated
    "continuing dairy ALTERNATE route..." with no movement and no target audit.
  - Fix: arrival check on alternate (dist <= 8 to DAIRY_PASTURE_ALT).
    When arrived: STOP routing, set milkDairyAlternateArrived, skip primary
    route logic, enter target-resolution phase.
  - Target-resolution: bounded live tile-object/NPC audit at current tile,
    search for special dairy object (fat_cow/prized dairy cow) with live
    "Milk" action around (3172,3317). Then one item-on-target + next-tick
    Bucket->Bucket of milk proof. If no Milk candidate: TARGET_NOT_FOUND/HOLD.
  - Do NOT keep repeating the alternate route.

## Current build
- **Build 457 / patch-453** (shipped 2026-09-29 18:15 EDT). STAGED DAIRY ROUTE.
  - Alex 18:12: direct (3246,3286)->(3172,3317) fails with rs2walker:walkStep:no-walkable-path.
    Unbounded retry loop (SHARED_TRAVERSAL FAILED every tick, no movement).
  - Fix: bounded staged route through validated anchors, one resolver request per leg:
    CHICKEN_FARM (3238,3298) -> WHEAT_FIELD (3157,3288) -> MILL_APPROACH (3166,3304)
    -> DAIRY_PASTURE (3172,3317). Latch one actionId for the dairy route.
  - On leg failure: one bounded alternate (DAIRY_PASTURE_ALT 3178,3322), then typed
    ROUTE_BLOCKED + sticky HOLD (phase 9). No unbounded retry. No generic cow clicks.
  - Acceptance: visible movement through staged anchors, then special dairy object
    with live Milk action near (3172,3317).

## Current build
- **Build 456 / patch-452** (shipped 2026-09-29 18:12 EDT). WIRED DESTINATION LOG.
  - Alex 18:10: Build 455 was loaded but doMilkCow() never logged a route toward (3172,3317).
    The code change existed but the live path wasn't emitting the required runtime line.
  - Fix: explicit diag log in the approach leg BEFORE stepToward:
    "Build 456: MILK_COW: routing to DAIRY_PASTURE seed (3172,3317,0) from (x,y) dist=N"
  - Acceptance: after reload, first MILK_COW tick MUST log the target seed (3172,3317)
    or a validated shared-resolver request. No movement/click until live Milk target proof.
  - The DAIRY_PASTURE constant and routing logic were already in Build 455; this build
    adds the explicit runtime evidence line Alex requires.

## Current build
- **Build 455 / patch-451** (shipped 2026-09-29 18:06 EDT). DAIRY DESTINATION FIX.
  - Alex 18:04-18:05: CRITICAL CORRECTION. The bot at (3246,3286) is in the WRONG PEN.
    COW_FIELD (3256,3273) is the ordinary cow field (Cow/Cow calf 2790/2791/2792, no Milk action).
  - New DAIRY_PASTURE constant (3172,3317,0) as SEARCH SEED. The approach leg now routes
    there via shared traversal resolver instead of COW_FIELD.
  - The coordinate is a search seed only -- NOT a completion trigger. Live "Milk" action
    on the special fat_cow/prized dairy cow (id 8689) is required before any click.
  - TARGET_AUDIT is now diagnostic-only: logs what it sees, NEVER selects a generic cow.
  - Research confirms: the dairy cow is a GAME OBJECT (tile object) with "Milk" option,
    not an NPC. OSRS Wiki: "Use a bucket on the prized dairy cow" in Lumbridge cow field.
  - Acceptance: fresh runtime target line at dairy location with live Milk action,
    then one bucket action + next-tick Bucket->Bucket of milk proof. HOLD if not found.

## Completion claim RETRACTED
- The 17:57 "milk acquired" report was from a STALE frame. Authoritative 18:03:13 log
  (Build 454): HELD, Bucket x1, Milk x0. No completion. Do not report success from old frames.

## Current build
- **Build 454 / patch-450** (shipped 2026-09-29 18:00 EDT). TARGET AUDIT.
  - Alex 17:58: Stop expanding pathing. The problem is target discovery -- in the pen at (3246,3286) but NPC query returns generic Cow with no Milk action.
  - New: bounded TARGET_AUDIT (once per phase-0 entry). Enumerates up to 8 NPCs + 8 tile objects in 15 tiles with live name, id, tile, dist. Read-only, never clicks.
  - Does NOT assume id=8689 or exact "Dairy cow" naming. Logs everything for diagnosis.
  - Existing one-action/next-tick Bucket->Bucket of milk proof unchanged. HOLD if no Milk action.
  - Phase 10 idempotency proof PRESERVED.

## Current build
- **Build 453 / patch-449** (shipped 2026-09-29 17:53 EDT). **IDEMPOTENCY PROVEN** ✅
  - Alex 17:55: RUNTIME VERIFICATION COMPLETE.
  - 17:55:20: TRAVERSAL START [diag-idempotent] outside (3246,3286) -> (3244,3283)
  - 17:55:21: same key re-issued -> TRAVERSAL IDEMPOTENT phase=FINDING_EDGE -- no reset, no re-arm
  - EDGE_SCAN 5962ms found no edge -> ROUTE_BLOCKED -> TRAVERSAL RESET (key released)
  - No movement, no progress claimed. Dedup + clean terminal reset PROVEN.
  - PRESERVE phase 10. Do not regress.
  - REMAINING: live blocked edge forward/return crossing proof. The no-edge fixture is NOT a crossing.
  - NOTE: WebWalk reports collision_conflict -- live scene disagrees with shipped static map. Live collision remains authoritative.

## Current build
- **Build 453 / patch-449** (shipped 2026-09-29 17:53 EDT). DIRECT IDEMPOTENCY TEST.
  - Alex 17:51-17:52: Build 452's fixture moved by ordinary walker without resolver handoff. Acceptance DENIED for reusable pathing. Direct movement != resolver test.
  - New phase 10: bypasses the walker entirely. Issues requestTraversal directly, re-issues the EXACT same key on the next tick while active.
  - The resolver MUST log TRAVERSAL IDEMPOTENT with dedup proof (no reset, no re-arm, blacklist preserved).
  - Does NOT move the player. Does NOT claim progress. Pure dedup isolation test.
  - If the request isn't active on tick 1, logs fixture-unavailable explicitly.

## Current build
- **Build 452 / patch-448** (shipped 2026-09-29 17:50 EDT). FIXTURE PRIORITY FIX.
  - Alex 17:48: Build 451 correctly refused the zero-route fixture (dIn=1). The cow-based inside was too close but wasn't discarded, blocking the gate/waypoint fallback.
  - Fix: too-close inside is now DISCARDED. Priority: (1) cow inside if dist>2, (2) gate far-side (5 tiles past), (3) distant waypoint (10 tiles, for idempotency testing), (4) fixture-unavailable.
  - The waypoint fallback tests TRAVERSAL IDEMPOTENT dedup even without a gate crossing -- the resolver engages on any active request.
  - Expected: "discarding too-close inside", then "gate fixture" or "waypoint fixture", then TRAVERSAL START + IDEMPOTENT.

## Current build
- **Build 451 / patch-447** (shipped 2026-09-29 17:46 EDT). GATE FIXTURE DISTANCE FIX.
  - Alex 17:44: Build 449 captured inside=(3245,3289) only 2 tiles from outside -- leg1 SKIPPED without engaging the resolver. dist<=2 must not count as diagnostic completion.
  - Fix: gate fixture now uses 5 tiles past the gate (not 2), guaranteeing the target is beyond stepToward's arrival threshold. The walk WILL stall at the fence, the resolver WILL engage.
  - "SKIPPED" renamed to "fixture-unavailable" for clarity -- if the target is too close, it's a fixture problem, not a proof.
  - Expected: "DIAG gate fixture" with dist>2, "TRAVERSAL START", "TRAVERSAL IDEMPOTENT" with dedup proof, then leg completion with handoff+tilechange.

## Current build
- **Build 449 / patch-445** (shipped 2026-09-29 17:42 EDT). TWO-LEG DIAGNOSTIC CROSSING.
  - Alex 17:40: Build 446's dairy guard is correct (no dairy found -> HOLD). But dedup marker and real crossing still absent. Fixture must not depend on milkable NPC.
  - Change: capture milkDiagInsidePos from ANY cow NPC's tile (cows live inside the pen). milkOutsidePos from phase-0 start. Both from live state, no hardcode.
  - Phase 6 is now two legs: leg1 outside->inside, leg2 inside->outside. Each leg requires nonzero start distance, resolver handoff (TRAVERSAL IDEMPOTENT expected on re-issue), and actual tile change. Arrival without handoff+tilechange = "NOT accepted".
  - The stepToward idempotency proof (re-issue same key while active) is live -- expect "TRAVERSAL IDEMPOTENT" with actionId and blacklist preserved.
  - No artificial success, no quest progress, no-Milk HOLD unchanged.

## Current build
- **Build 448 / patch-444** (shipped 2026-09-29 17:40 EDT). VALID DIAG-RETURN + IDEMPOTENCY PROOF.
  - Alex 17:38: Build 445's DIAG-RETURN was invalid -- entry=(3243,3289) and player already there, zero-distance "completion" with no gate crossing.
  - Fix: capture milkOutsidePos on FIRST tick of phase 0 (before any pen approach). DIAG-RETURN targets the outside tile, not entry.
  - Zero-distance guard: if dist<=2 at return start, logs "DIAG-RETURN SKIPPED -- already at target, no crossing to prove" and does NOT claim proof.
  - Handoff requirement: DIAG-RETURN COMPLETE only if the shared resolver actually engaged (handoff seen). Arrival without handoff = "NOT accepted as gate-crossing proof".
  - Idempotency proof (Build 447 work): stepToward re-issues the EXACT stored request (from/goal/label) on every tick while the resolver is active. Expect "TRAVERSAL IDEMPOTENT" lines with blacklist preserved, no duplicate action.
  - Preserved: dairy-only target, NPC action diagnostic (hasMilk=false -> HOLD, no click), no-Milk HOLD unchanged.
  - Acceptance needs: "outside pos captured", "DIAG-RETURN starting -- to OUTSIDE", "TRAVERSAL IDEMPOTENT", and either a real crossing or an honest SKIP.

## Current build
- **Build 446 / patch-443** (shipped 2026-09-29 17:38 EDT). DAIRY-ONLY TARGET (game-verified).
  - Game chat evidence 17:30-17:33: "Calves are too young to be milked." / "Only dairy cows are suitable for milking."
  - Build 443 excluded calves but accepted any adult cow (id=2791) -- game rejects non-dairy.
  - Fix: all three NPC match sites now REQUIRE "dairy" in the name (not just prefer). No dairy cow = no target = typed HOLD.
  - Preserved: resolver idempotence (Build 445), NPC action diagnostic, diagnostic-only return, actionId-paired proofs.
  - Still needed for acceptance: dairy TARGET lines, one bucket action, Bucket->Bucket of milk proof, return crossing.

## Current build
- **Build 445 / patch-442** (shipped 2026-09-29 17:37 EDT). RESOLVER IDEMPOTENCE (Alex 17:35/17:36, contract section 10).
  - Root cause: requestTraversal() unconditionally cleared triedEdges, reset activeEdge, and re-armed FINDING_EDGE on EVERY call. A quest tick calling it repeatedly would reset the route forever.
  - Fix: requests keyed by (from tile, target tile, plane, context). Same key + active phase = no-op with "TRAVERSAL IDEMPOTENT" diagnostic (no latch reset, no re-arm, no duplicate action). Only a genuinely new key clears/replans. Same for requestVerticalTraversal.
  - ActionId-paired proofs: each issued action gets actionId=N; the PROOF diagnostic logs actionId=N; stale observations cannot satisfy a new action. Latch cleared on timeout/new request.
  - Terminal states (DONE/FAILED) release the key. Explicit reset() method for terminal reset.
  - Diagnostic-only return path (phase 6) preserved from Build 444.
  - NPC live-action diagnostic (name/id/tile/dist/adjacent/actions/hasMilk) preserved.
  - Contract section 10 compliance: one resolver owner (Rs2Traversal), actionId-paired proofs, no-progress invariant (diag return claims nothing), world-change invalidation (key includes plane; new world = new key), separate interaction latches (milk latch vs traversal latch), bounded scan diagnostics, safe restart resnapshot (IDLE on fresh start), proof-first completion (DONE only on verified crossing).
  - Runtime proof needed: "TRAVERSAL IDEMPOTENT" lines showing same-key dedup with preserved triedEdges blacklist and no duplicate action. Build marker alone is NOT acceptance.

## Current build
- **Build 443 / patch-440** (shipped 2026-09-29 17:31 EDT). CALF EXCLUSION (Alex 17:30).
  - Root cause: Build 442 selected Cow calf id=2792 (not milkable), issued bucket-on-cow, correctly held after no delta. No return ran because milk was never proven.
  - Fix: all three cow-NPC match sites (phase 0 discovery, phase 3 validation, phase 3 milking target) now exclude any NPC with "calf" in the name. Only adult cows are targeted.
  - Sort still prefers "dairy" in the name, then nearest.
  - Phase 3: one bucket-on-adult-cow, next-tick Bucket->Bucket of milk proof, 3 bounded attempts, then typed HOLD.
  - Phase 5 return route: still armed. After milk is PROVEN, routes back to milkEntryPos via shared traversal for the gate-crossing acceptance.
  - Status: forward/return traversal acceptance PENDING until a real milk proof occurs. If milk cannot be proven on an adult cow, typed HOLD is correct behavior.

## Current build
- **Build 442 / patch-439** (shipped 2026-09-29 17:27 EDT). NPC MILK + FORCED RETURN PROOF (Alex 17:25).
  - Phase 3 REWRITTEN: milks the validated NPC via Rs2Inventory.useItemOnNpc(bucketId, npc). No tile-object scan. One action, next-tick Bucket->Bucket of milk proof (phase 4). 3 bounded attempts, then HELD (MILK_ACTION_FAILED).
  - If not adjacent to the cow, routes via stepToward (shared traversal owns any gate).
  - Phase 5 (NEW): FORCED RETURN ROUTE. After milk verified, routes back to milkEntryPos (recorded in phase 0 when the cow approach started) via stepToward -> shared Rs2Traversal.
  - Acceptance (strict, per Alex): forward route (outside->cow) AND return route (cow->entry) must EACH show: SHARED handoff -> candidate scores -> one Open/Close -> next-tick object proof -> signed side proof. Ordinary walker movement or proximity does NOT count.
  - No object/gate fallback bypassing shared traversal. No coordinate hard-code (entry pos is observed at runtime).

## Current build
- **Build 440 / patch-437** (shipped 2026-09-29 17:22 EDT). COW TARGET DISCOVERY FIX (Alex 17:21).
  - Root cause: exact name match ("Prized dairy cow"/"Dairy cow") missed the visible cows at (3239,3284,0). 5 scans -> HOLD with no traversal handoff.
  - Fix: broadened NPC discovery to any NPC with "cow" in the name (case-insensitive), preferring dairy variants in the sort. Synchronous via blocking client-thread invoke; no getObjectComposition, no async cache.
  - Diagnosis: when no cow is found, logs the nearest 5 NPCs with name/id/tile so the name mismatch is visible in the diag (e.g. `nearest 5 NPCs: ['Cow' id=1234@(3238,3285), ...]`).
  - Phase 3 validation uses the same broadened match.
  - No gate-specific click loop reintroduced. If the cow is found but the pen gate blocks, stepToward -> shared Rs2Traversal owns the crossing (direction scoring, one Open/Close, proofs).
  - Acceptance: cow target discovered -> shared resolver crossing -> cow validation -> bucket action; or bounded candidate log -> typed HOLD.

## Current build
- **Build 439 / patch-436** (shipped 2026-09-29 17:19 EDT). DAIRY GATE CONTRACT ALIGNMENT (Alex 17:17-17:18, contract sections 8-9).
  - REMOVED the entire dairy-specific gate state machine from MILK_COW:
    - Phases 1-2 deleted (gate scan + crossing with 20s timeout/retry).
    - Fields removed: milkGateTile, milkGateName, milkGateMs, milkGateRetried, milkGateId, milkGateAltTile, milkInsideTile, milkGateProofLogged, milkGateFrom.
    - Methods removed: walkThroughGate400, findPenGate396, findPenGate408, gateCandidatesDiag396.
    - No Gate.click, no direct walkStep for gate recovery, no milkGate* retry loop.
  - Phase 0 now routes DIRECTLY to the dairy cow NPC via stepToward:
    - `stepToward(cowPos, "to dairy cow")` -> shared Rs2Traversal on stall.
    - Gate crossing uses SHARED requestTraversal/tick/result with direction scoring.
    - Decompile verified: no milkGate*/findPenGate*/walkThroughGate* in the class.
  - Contract sections 8-9: quest states may ONLY request/tick shared traversal. No quest-local Gate.click, Climb/Cross, or walkThrough for route recovery. SATISFIED for MILK_COW.
  - Acceptance log markers to watch for a fresh dairy route:
    1. `Build 439: WALK_STALL -> TRAVERSAL_HANDOFF 'to dairy cow'`
    2. `TRAVERSAL START [SHARED:to dairy cow]`
    3. `EDGE_SCAN [SHARED:to dairy cow] candidates=N scores: [...]`
    4. One `Open`/`Close` action (not repeated)
    5. Next-tick object-state proof
    6. Next-tick side proof (SIDE_PROOF / CROSSING PROOF)
    7. `Build 439: MILK_COW: cow validated: 'Dairy cow'...`
    - Failure: `ROUTE_BLOCKED` or `DOOR_CROSSING_FAILED` + sticky HOLD (milkPhase=9).

## Current build
- **Build 436 / patch-433** (shipped 2026-09-29 17:09 EDT). CANDIDATE-DIRECTION RULE (Alex 17:08, contract).
  - Root cause of GET_EGG wrong-door: `findBlockingEdge` used `.nearest()` -- the mill door at (3166,3302) was closest to the player but BEHIND them relative to the chicken-farm target. Open + walk-through went the wrong way; SIDE_PROOF correctly rejected, but the route was useless.
  - Fix in Rs2Traversal (generic, no quest coordinates): EDGE_SCAN now collects bounded candidates (within+where filtered, toList on the filtered stream only), then scores each:
    - signed projection t onto from->target (0=from, 1=target);
    - far-side neighbor (one step from edge toward target) must be walkable (Rs2Tile.isWalkable);
    - far-side projection must INCREASE (tFar > t, tFar > 0).
    - Reject: t<0 (behind player), far-side blocked, no advance. Logged per candidate.
    - Pick highest t among OK. Log line: `EDGE_SCAN [ctx] candidates=N scores: [id=... t=... far=... verdict]`.
  - One alternate via triedEdges (unchanged), then typed HOLD (unchanged).
  - Hopper sticky (433), vertical seq (434), universal handoff (435) preserved.
  - Acceptance to watch: GET_EGG EDGE_SCAN should now REJECT the (3166,3302) mill door as behind-player and either find the correct eastward edge or emit typed HOLD without clicking the wrong door.

## Current build
- **Build 435 / patch-432** (shipped 2026-09-29 17:05 EDT). UNIVERSAL HANDOFF (Alex 17:04).
  - Root cause of GET_EGG 17:02:17 silent stall: `stepToward` set `walkHandoffActive` and logged TRAVERSAL_HANDOFF, but only GET_GRAIN and MILL_FLOUR ever checked the flag. GET_EGG (and dairy/bank/cooking/Tutorial) stalled silently for 70s+ with no resolver.
  - Fix: `stepToward` now OWNS the shared traversal lifecycle via `sharedTraversal` (script-level Rs2Traversal).
    - Stall (40 ticks) -> immediately `requestTraversal` with `SHARED:<label>` context -> tick each call.
    - DONE -> clear state, resume walk. FAILED -> hold with diagnostics. IN_PROGRESS -> wait.
    - Every phase that calls `stepToward` gets this automatically. No phase checks flags.
  - Removed redundant phase-specific handoff blocks in GET_GRAIN (WHEAT_FIELD nav) and MILL_FLOUR (MILL_RETURN_NAV). `grainTraversal` retained for wheat-target and vertical traversals (not via stepToward).
  - Hopper sticky (Build 433) and vertical seq-pairing (Build 434) preserved. Rs2Traversal decompile-clean per Alex.
  - Acceptance: every handoff now produces TRAVERSAL START + bounded scan/action/proof, or typed failure+HOLD within deadline. Watch for `Build 435: WALK_STALL -> TRAVERSAL_HANDOFF` followed by `TRAVERSAL START [SHARED:...]` in GET_EGG.

## Current build
- **Build 434 / patch-431** (shipped 2026-09-29 17:03 EDT). VERTICAL LATCH SEQ-PAIRING (Alex 17:02).
  - Rs2Traversal: added `climbRequestSeq` / `pendingClimbSeq`. Every `requestVerticalTraversal` increments the seq; the latch stores the seq at issue time; verification requires `pendingClimbSeq == climbRequestSeq`. Stale latches (seq mismatch) are logged and cleared, never verified.
  - Fixes the 17:01:07 out-of-order "climb-down verified plane 1->2" which fired from a stale latch before the 17:01:08 issue. Every verification is now paired with its own issued action (id/object/tile/prePlane/seq) and cannot fire before the issue tick.
  - Hopper sticky fix (Build 433) PRESERVED. No route state changes.
  - Decompile acceptance: Alex inspected patch-430's Rs2Traversal.class -- only generic state/strings, no Cook's coordinates. SATISFIED. My local `strings` check on the Build 434 class confirms the same (only "currentTimeMillis" matched the quest-keyword grep).
  - Note: version.txt went 428 -> 431 (patch-430.zip exists on GitHub but version skipped it). Patch zips are full overlays, so 428->431 direct is safe.
  - Remaining acceptance: fresh-start forward+return door proof (SIDE_PROOF + CROSSING PROOF both directions, no repeated actions).

## Current build
- **Build 433 / patch-430** (shipped 2026-09-29 16:58 EDT). FOCUSED HOPPER STICKY PATCH (Alex 16:56 + Julien 16:56).
  - Rs2Traversal UNCHANGED (still Build 428). No route state reset. Universal contract work is separate.
  - Regression fixed: hopper use was NOT sticky at the outer level. Logs showed FAILED -> sub 1 -> sub 2 -> click -> FAILED repeating indefinitely (16:55:32 through 16:56:37), player stuck at (3166,3308,2).
  - Root cause: `useOnFailed` was consumed and sub 1 re-entered sub 2, starting a FRESH use-on attempt each cycle. The inner bounded retry was per-attempt, not per-objective.
  - Fix: `hopperUseExhausted` sticky flag (per objective). Once the bounded retry is exhausted:
    - NEVER re-enter sub 2 (sub 1 routes exhausted state straight to sub 3)
    - NO more hopper clicks across ticks/runs
    - Proceed to hopper controls (sub 3) -- Julien observed "already grain in the hopper", so the quest can continue; the flour-bin check verifies
  - Terminal HOLD: if hopper exhausted + controls operated + bin empty after bounded retry -> `millFailed` sticky, doMillFlour returns immediately (no clicks, no navigation). Releases only on fresh verified state change (flour observed).
  - Acceptance marker: one use-on objective -> one bounded retry -> sticky HOLD, no repeated clicks. Watch for: `Build 433: MILL_FLOUR: hopper use EXHAUSTED` -> `operate controls` -> either `flour collected` or `TERMINAL HOLD`.

## Current build
- **Build 430 / patch-427** (shipped 2026-09-29 16:56 EDT, NUDGE ALEX 16:52). HOPPER INTERACTION PROXIMITY FIX.
  - Pathing resolver UNCHANGED (Rs2Traversal still Build 428). Fresh-start validation before mill door still the acceptance gap -- preserved.
  - Root cause of hopper use failure: useItemOnObject CLICK phase clicked immediately with no proximity check. If player >3 tiles from hopper, the click walked but the "use" never registered -> 6s verify timeout -> false failure -> retry -> fail.
  - Fix: CLICK phase now checks chebDist(player, object) > 3; if too far, single Rs2Walker.walkStep toward object (interaction approach, NOT a route leg -- no stall-handoff machinery), no click yet. Click only fires within 3 tiles. Applies to hopper and flour bin (universal use-on).
  - Framework rule preserved: interaction approach is separate from navigation; failed use-on still cannot reissue route legs or reset verified crossings.
- Watch for: `Build 430: STARTUP -- RUNNING_BUILD=430 (patch-427)` -> `use-on: approaching 'Hopper' (N tiles)` -> `use-on: clicked 'Hopper'` -> `use-on: VERIFIED -- 'Grain' X -> Y`.

## Current build
- **Build 429 / patch-426** (shipped 2026-09-29 16:54 EDT, NUDGE ALEX 16:51). FRAMEWORK RULE: INTERACTION SEPARATE FROM NAVIGATION.
  - Alex confirmed Build 427 end-to-end: wheat collected, WebWalk returned through mill door, player reached mill with door-edge/open diagnostics. Pathing resolver VERIFIED -- left unchanged (Rs2Traversal still Build 428).
  - Framework rule enshrined: interaction retries (useItemOnObject: one bounded re-click after 6s no-delta, then sticky fail) are SEPARATE from navigation. A failed use-on cannot reissue a route leg, cannot reset walkHandoffActive/failedLeg*/grainTraversal, cannot invalidate a verified crossing. Both failure handlers (hopper sub 2 -> sub 1, flour bin sub 5 -> sub 3) documented; neither touches nav state.
  - Next failure is interaction-specific (hopper use), not pathing. Do not touch Rs2Traversal for it.
- Watch for: `Build 429: STARTUP -- RUNNING_BUILD=429 (patch-426)` -> hopper use diagnostics.

## Current build
- **Build 428 / patch-425** (shipped 2026-09-29 16:52 EDT, NUDGE ALEX 16:49). MILL RETURN HANDOFF.
  - Alex confirmed Build 427 post-door: one reachable Pick wheat id=15507 (preWheat=0, no delta), one alternate id=15506 (preWheat=1), grain observed, GET_GRAIN -> DETECT. No unreachable-click loop. Handoff/crossing fix VERIFIED in resumed state.
  - Acceptance note: NOT a fresh-start run (resumed at post-door tile). Next validation must start BEFORE the mill door and capture SIDE_PROOF + CROSSING PROOF.
  - New: MILL_FLOUR sub 0 return route now has the same WALK_STALL -> TRAVERSAL_HANDOFF rule. If stepToward(MILL_APPROACH) stalls, hands to Rs2Traversal targeting MILL_INSIDE (through the door, south->north). Same one-action/next-tick proof, SIDE_PROOF, DONE/replan or DOOR_CROSSING_FAILED contract.
- Watch for: `Build 428: STARTUP -- RUNNING_BUILD=428 (patch-425)` -> on return: `WALK_STALL -> TRAVERSAL_HANDOFF 'to mill'` -> `TRAVERSAL START [MILL_RETURN_NAV]` -> `SIDE_PROOF ... => true` -> `CROSSING PROOF`.

## Current build
- **Build 427 / patch-424** (shipped 2026-09-29 16:48 EDT, NUDGE ALEX 16:45/16:46). DOOR-CROSSING SIDE PROOF (generic, not mill-specific).
  - Root cause (Alex decompiled 426): VERIFYING_CROSSING used `dist(edge)<=3 AND dist(fromPos)>2`. Mill door: from=(3166,3303), edge=(3166,3302), post=(3165,3301) -> dist=2, strict >2 false -> 30s timeout -> ROUTE_BLOCKED despite successful crossing.
  - Fix: signed projection t of (player, edge) onto fromPos->target corridor. Proof = `moved (post!=pre) && t_post > t_edge`. Verified on Alex's coords: t_edge=0.049, t_post=0.127 -> crossed=True.
  - SIDE_PROOF log line: prePlayer, postPlayer, edge tile, t_pre/t_edge/t_post, approach_side, destination_side, moved, exact predicate.
  - One bounded alternate walk (2 tiles past edge) if not crossed, then DOOR_CROSSING_FAILED (not generic alternate-edge).
  - Handoff no-op: `walk handoff -- requesting traversal` logged only on actual request; pending ticks just tick the resolver.
- Watch for: `Build 427: STARTUP -- RUNNING_BUILD=427 (patch-424)` -> `SIDE_PROOF ... predicate=(moved && t_post > t_edge) => true` -> `CROSSING PROOF -- player on destination side, DONE` -> walk replan -> Pick wheat.

## Current build
- **Build 426 / patch-423** (shipped 2026-09-29 16:45 EDT, NUDGE ALEX 16:40/16:41). SHARED VERTICAL RESOLVER + WALK STALL HANDOFF.
  1. Rs2Traversal now handles vertical transitions: requestVerticalTraversal(playerPos, targetPlane, ctx) with phases FINDING_LADDER -> CLIMB_ISSUED -> VERIFYING_PLANE -> DONE/FAILED. Pending latch keyed by objectId+tile+action+prePlane; no second climb until plane proof or timeout. LADDER_SCAN logs elapsed/count. Candidates must be on current plane.
  2. Walk stall handoff: stepToward tracks failed legs (start+goal). On 40-tick stall, emits `WALK_STALL -> TRAVERSAL_HANDOFF` and invalidates the leg -- it will NOT be retried. doGetGrain hands stalled nav legs to Rs2Traversal.requestTraversal(pp, goal) with corridor; resolver finds door, one action, next-tick proof, walk-through, side proof, then replan. If no edge: ROUTE_BLOCKED + hold.
- Watch for: `Build 426: STARTUP -- RUNNING_BUILD=426 (patch-423)` -> `TRAVERSAL START ... VERTICAL` -> `LADDER_SCAN` -> `one 'Climb-down'` -> `climb verified` -> `WALK_STALL -> TRAVERSAL_HANDOFF` -> `TRAVERSAL START [GET_GRAIN_NAV]` -> door crossing.

## Current build
- **Build 425 / patch-422** (shipped 2026-09-29 16:41 EDT, NUDGE ALEX 16:36). VERTICAL NAVIGATION FIX. Root cause: a chebDist(pp, WHEAT_FIELD) > 14 check ran BEFORE the plane logic, and chebDist ignores plane -- so from (3165,3307,2) it issued stepToward to a plane-0 goal, stalling WebWalk for 40+ ticks. Now: plane check FIRST. If plane != 0, resolve live ladder/stair (ID/name/tile logged), issue one Climb-down, verify plane delta next tick (latched, no re-click). If no ladder, emit NAVIGATION_BLOCKED with candidates and hold. Only on plane 0 does horizontal navigation run. The universal resolver now covers vertical transitions.
- Watch for: `Build 425: STARTUP -- RUNNING_BUILD=425 (patch-422)` -> `GET_GRAIN: on plane 2 -- resolving ladder/stair` -> `ladder candidate id=...` -> `one Climb-down ... verifying next tick` -> `climb verified -- plane 2 -> 1` (or 0).

## Current build
- **Build 424 / patch-421** (shipped 2026-09-29 16:38 EDT, NUDGE ALEX 16:34). CORRIDOR-CONSTRAINED FINDER. findBlockingEdge now uses query().within(fromPos,12).where(corridorPredicate).nearest() -- NOT toList(). The predicate requires the edge tile to be within 3 tiles of the from→target segment (distanceToSegment), not just nearest-to-player. Elapsed logged around the query itself before candidate selection. issueTraversalAction and verifyActionState also use first() not toList(). No materialization before caps.
- Acceptance: arbitrary door/gate/fence/wall via corridor intersection; no wrong-side click (reachability gate); one action + next-tick proof; one walk-through + player-side proof; one alternate; typed ROUTE_BLOCKED/DOOR_CROSSING_FAILED + hold; no silent stall (5s heartbeat, 30s deadline).
- Watch for: `Build 424: STARTUP -- RUNNING_BUILD=424 (patch-421)` -> `TRAVERSAL START` -> `EDGE_SCAN ... query elapsed=...ms found=1 edge=...` -> crossing -> Pick.

## Current build
- **Build 423 / patch-420** (shipped 2026-09-29 16:35 EDT, NUDGE ALEX 16:33). WHEAT FIELD NAVIGATION. After the mill, GET_GRAIN was silently waiting on plane 2 with "no wheat in range" -- the wheat is on plane 0. Now: if plane != 0, find ladder and Climb-down with diagnostics; if on plane 0 but >25 tiles from wheat field (3161,3292,0), walk there. Only then engage the shared Rs2Traversal resolver for the fence. No silent stalls -- every tick emits navigation or traversal diagnostics.
- Watch for: `Build 423: STARTUP -- RUNNING_BUILD=423 (patch-420)` -> `GET_GRAIN: on plane 2 -- climbing down` -> plane 0 -> `walking to wheat field` -> `TRAVERSAL START` -> `EDGE_SCAN` -> crossing -> Pick.

## Current build
- **Build 421 / patch-418** (shipped 2026-09-29 16:30 EDT, NUDGE ALEX 16:27). API-CONFIRMED BOUNDED CHAIN. Alex inspected the installed jar: Rs2TileObjectQueryable supports within(WorldPoint,int), where(Predicate), withNameContains, withIds, first/nearest variants, toList; Rs2TileObjectCache exposes query() and getStream(). Implementation now uses the supported chain in order: query().within(fromPos, corridorRadius).where(...) then capped result -- name/ID filtering inside the bounded corridor, never query().toList() on the full cache. EDGE_SCAN logs elapsed/count. All calls under the 30s resolver deadline.
- Watch for: `Build 421: STARTUP -- RUNNING_BUILD=421 (patch-418)` -> `TRAVERSAL START [GET_GRAIN]` -> `EDGE_SCAN` -> gate crossing.

## Current build
- **Build 420 / patch-417** (shipped 2026-09-29 16:28 EDT, NUDGE ALEX 16:24/16:26). SILENT STALL ROOT CAUSE FIXED. Alex decompiled patch-416: `findBlockingEdge()` called unbounded `query().toList()` on ~42,798 tile objects, freezing the tick thread -- no TRAVERSAL/EDGE/timeout lines for 2+ min after 16:21:52. Also `getObjectComposition()` (blocks on CompletableFuture) was called in GET_GRAIN wheat finder (line 1908), gate inspection, and WALL_DOOR diag.
- Fixes: (1) ALL `getObjectComposition()` calls removed from tick path (wheat finder, Rs2Traversal, gate inspection, WALL_DOOR diag). (2) ALL tile-object queries now spatially bounded at query level: `within(pp, radius)` + name/id filters; never unbounded `toList()`. (3) `TRAVERSAL START` emitted before any query (player/target/deadline). (4) `EDGE_SCAN` with elapsed/scanned/matched counts. (5) Heartbeat every 5s while IN_PROGRESS. (6) Global 30s deadline from request (never reset on alternate). (7) Hard caps: 200 objects, 2s scan budget, 20 id-checks.
- Watch for: `Build 420: STARTUP -- RUNNING_BUILD=420 (patch-417)` -> `TRAVERSAL START [GET_GRAIN]` -> `EDGE_SCAN ...` -> `edge ...` -> `CROSSING PROOF` or `ROUTE_BLOCKED`. The 2-min silence must not recur.

## Current build
- **Build 419 / patch-416** (shipped 2026-09-29 16:21 EDT, NUDGE ALEX 16:18-16:19). PERMANENT SHARED GUARANTEE: reusable Microbot traversal layer -- every future quest script uses the same resolver; a visible target behind a door/fence can NEVER be clicked from the wrong side.
- New module: `net.runelite.client.plugins.microbot.util.traversal.Rs2Traversal` (reusable above all quest scripts). Contract: (1) plan route, identify blocked edge from live collision/reachability; (2) bounded obstacle query (IDs/tiles/actions, corridor r=12); (3) ONE traversal action max, next-tick object-state proof (id change or Open<->Close flip); (4) one walk-through, next-tick player-side/WorldPoint proof; (5) invalidate failed edge, try ONE alternate; (6) typed `ROUTE_BLOCKED`/`DOOR_CROSSING_FAILED` + HOLD after bounded failure (30s). Formal evidence record per edge (id/name/tile/actions/chosen).
- Cook's GET_GRAIN now uses it: wheat-specific gate code REMOVED. Unreachable wheat -> `requestTraversal(pp, wheatTile, "GET_GRAIN")`, no Pick until traversal returns DONE (verified reachable). WALL_DOOR activates when traversal is active (generic, not milkPhase-specific). Dairy gates will use the same code path.
- GET_GRAIN keeps the proof contract: one Pick, next-tick exact Wheat id/count delta; no delta -> one alternate rescan -> `GET_GRAIN_FAILED` + hold.
- Watch for: `Build 419: STARTUP -- RUNNING_BUILD=419 (patch-416)` -> `Build 419: TRAVERSAL [GET_GRAIN]: ...` -> `EDGE id=...` -> `PROOF action 'Open' changed object state` -> `CROSSING PROOF` -> one Pick -> Wheat delta. Or bounded `ROUTE_BLOCKED` + hold.

## Current build
- **Build 413 / patch-410** (shipped 2026-09-29 15:39:21 EDT, NUDGE ALEX 15:38; sibling ship, source mtime 15:38:53 EDT). Same freeze class as 412's milk-scan fix, one layer earlier: the phase-1 gate-scoring path (dairy-scored gate selection at ~3236,3286,0) had its own BLOCKING call, freezing the tick thread at the pen boundary. Build 413 rewrites gate scoring to be non-blocking / sync-only. Muse review loop VERIFY-only this run (overlap rule: sibling shipped 40s before my run started).
- Watch for: `Build 413: STARTUP -- RUNNING_BUILD=413 (patch-410)` -> gate-crossing progress at the chicken/cow pen boundary -> `Build 413: MILK_COW: MILK VERIFIED` + Bucket of milk in inventory -> RETURN_COOK. Client was in apply->restart at 15:39: expect desktop/restart frames, then in-game MILK_COW with the 413 banner.
- Previous: **Build 412 / patch-409** (shipped 2026-09-29 15:31:40 EDT, NUDGE ALEX 15:30/15:31 URGENT). Root-cause fix: Build 411's findMilkableCowObject called getObjectComposition(), which BLOCKS on CompletableFuture -- jstack showed the tick thread frozen ~3 min (15:25:20->15:28:28), which is why the avatar sat frozen at the gate. Build 412 rewrote the milkable-cow object finder to use ONLY synchronous getters (getId/getName/getWorldLocation); no composition, no reachability, no async. Filter: id==8689 OR name contains 'dairy'; Milk action verified by click result. After 3 bounded empty scans -> phase 9 HELD with diagnostics.
- Watch for: `Build 412: STARTUP -- RUNNING_BUILD=412 (patch-409)` -> `Build 412: MILK TARGET FOUND: ...` (sync scan, returns within tick) -> `Build 412: MILK_COW: MILK VERIFIED` -> RETURN_COOK.
- LIVE 15:32 EDT (Muse review loop): Build 411 / patch-408 ("milk object first", shipped 15:26:28Z) applied+validated by Supervisor at ~15:27 EDT (desktop shot 15:27:37: relaunch PID 21908, splash Starting plugins 60/147). In-game frame 15:26:32 = Build 410's last act: avatar inside chicken-farm pen area with egg labels, empty bucket + egg in inventory, NO bucket of milk, stage COOKS_MILK_COW. Milk UNVERIFIED ~133 min (since 13:14). Build 412 applying next; client mid-restart storm (408->409).
- Pre-ship observation (15:08-15:12): Build 409 held at the chicken farm gate ~4+ min with zero movement (screenshots 15:08:22/15:10:17/15:12:13 COOKS_MILK_COW, egg in inventory, empty bucket, click marker on fence gate). Consistent with post-restart re-selection of chicken-pen gate id1559 (wrong-pen tracking resets on restart) -> WRONG PEN -> phase 9 HELD. Build 410's object-scan does NOT persist wrong-pen gates across restarts -- if 410's phase-3 scan finds no Milk object from the wrong pen, expect another HELD at the chicken pen.

## Current build
- **Build 408 / patch-405** (shipped 2026-09-29 ~14:52 EDT). Change: dairy-scored gate selection (NUDGE ALEX 14:49).
- 406 crossed gate id1559 to (3236,3286) -- REAL crossing but to the CHICKEN pen (screenshot 14:38:44: chickens/eggs/raw chicken, cowhide across another fence; NPCs: Chicken/Duck/Drake/Farmer/Seth Groats/Goblin, no dairy).
- Build 408: (1) New findPenGate408() -- enumerates all Gate/Fence gate candidates with id/actions/pos; scores by dairy proximity (Prized/Dairy cow within 30 tiles); only dairy-gates valid, else HELD. (2) milkWrongPenGateIds tracks wrong-pen gates (id1559) -- never retried. (3) New classifyPen408(): WRONG PEN (chicken/ducks) vs FALSE CROSSING vs DAIRY PEN. (4) Failed pen-side validation marks gate WRONG PEN + holds.
- Watch for: `Build 408: STARTUP -- RUNNING_BUILD=408 (patch-405)` -> `gate candidates enumerated` -> dairy-gate selected -> `PEN-SIDE CONFIRMED` -> milk.
## Build 397 / patch-394 (2026-09-29 ~13:24 EDT) -- Alex's motion correction: per-tick pos log, NPC lookup only at gate-adjacent tile

Alex's correction (13:22): comparing 13:20:41 vs 13:21:43 MILK_COW frames, the player sprite SHIFTS along the fenced path (left-center -> upper-left, red route/minimap updating) -- the bot is NOT motionless. `no dairy cow nearby` is a target-search failure during movement, not proof walking stopped. Also: the 13:20:19 RuneLite WorldService Error is environmental noise, not the MILK_COW diagnosis.

Changes: (1) MILK_COW now logs player WorldPoint EVERY tick during active phases: `Build 397: MILK_COW: pos (x,y,z) phase=N` -- arrival verification is explicit in the log. (2) NO npc lookup while moving: approach leg (to COW_FIELD) and gate-route leg run with zero findNpc calls; NPC lookup only after the route reaches a gate-adjacent tile. Phase 0 at field -> straight to gate scan. Phase 2: at gate tile, one lookup -- cow within 8 -> skip entry, phase 3; else one Open click, latch, walk through toward post-lookup cow. (3) Phase 3 (post-crossing): exact Prized dairy cow -> Dairy cow, bounded 5-scan fallback to held-with-diagnostics if the pen is empty. Build 396's markers/latch/gate-entry sequence otherwise unchanged. version.txt=394 live.

## Build 396 / patch-393 (2026-09-29 ~13:22 EDT) -- MILK_COW pen-gate entry + pending-action latch (Alex's evidence-packet assignment)

Alex's evidence packet (MILK_COW_GATE_DIAG_FOR_MUSE.txt, 13:20-13:21 thread): bytecode of Build 395's doMilkCow() shows route-to-COW_FIELD -> findNpc("Prized dairy cow",20) -> findNpc("Dairy cow",20) -> if null only logs `no dairy cow nearby -- waiting` and returns. NO gate/door scan, NO scene refresh, NO alternate tile, NO bounded stop. Screenshot 13:17:36 + stream: player on the fenced path, cows behind the fence. Exact failure: pen reachability/entry, not missing cows.

Gate-entry sequence (preserving Microbot APIs): (1) scene scan -> `Build 396: MILK_COW: scene scan -- gate candidates: Gate@(x,y,z)[walkable|blocked]...; player@(x,y,z)`; (2) chosen gate -> `chosen gate 'Gate'@(x,y,z) -- one walk`; (3) one walk (walkStep per tick) then ONE `Open` click -> `at gate -- one Open click issued, latching for next-tick position/door-state proof`; (4) walk through toward wide-scan cow, next-tick crossing proof -> `crossing proof -- player@(x,y) moved (fx,fy)->(x,y) vs gate@(gx,gy); doorStillThere=<bool>; cow '<name>' within <d> -- selecting cow`; (5) only after crossing: query Prized dairy cow -> Dairy cow, ONE bucket-on-cow click via latch, next-tick `Bucket of milk` proof. If no reachable gate after 3 scans -> `NO REACHABLE GATE -- STOPPING. player@(...); candidates=[...]; walkStall=N`. If crossing fails after one retry -> `GATE ENTRY FAILED after retry -- STOPPING` with player/gate/walkable/walkStall. Phase 9 = held with diagnostics, no further cow actions.

Pending-action latch everywhere (the 13:12-13:18 repeat-action defects): useItemOnObject / useItemOnNpc VERIFY branches now LATCH 6s waiting for the inventory delta -- no second click merely because the result is delayed; exactly one bounded re-click after rescan; then sticky fail flag the caller consumes (hopper -> rescan sub 1; flour bin -> back to sub 3; bucket-on-cow -> phase 9). Mill climb-up/down: one click -> latch on plane change (5s) -> one retry -> rescan. GET_EGG: one take click with pre-count -> latch (5s) -> one retry -> held with diagnostics (kills the 7x-take loop). Action gaps over ~2s that are deliberate waits now say `latched` so the review loop doesn't misread them as hangs. version.txt=393 live.

## Build 395 / patch-392 (2026-09-29 ~13:08 EDT) -- throttled tick-alive marker before the login gate

Alex 13:06: asked for a throttled marker *before* the `Microbot.isLoggedIn()` gate plus an explicit logged-in/mission-select result. Added at the top of Cook's tick lambda: `Build 395: tick alive -- entering login/mission-select gate` every 60s -- proves tick-loop liveness even if the gate stays silent. Gate itself unchanged. TI marker bumped to 395 (no logic change). version.txt=392 live. NOTE: the client picked up patch-391 (Build 394) at 13:07:14, so 392 will apply on the next update check (~60s) with a client restart; mission file persists ("cooks") so the gate re-runs after reboot.

## Build 394 / patch-391 (2026-09-29 ~13:06 EDT) -- Cook's tick loop actually started (the silent-Cook's root cause)

Bytecode root cause (Alex 13:04-13:06 thread): `Script.run()` (base class) does checks and returns true -- NO loop, NO `scheduleWithFixedDelay`. `StateMachineScript` adds none either. TutorialIslandScript schedules its own tick loop in `run()` (`mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> { ... step(); }, 0, config.tickDelay(), ...)`). Cook's `run()` only called `super.run()` -- zero scheduling anywhere in the file -- so `onState()` / `missionSelectTick391()` NEVER executed. The 12:59 STARTUP with no MISSION_SELECT, no heartbeat, only WebWalk telemetry is exactly what a script with no tick loop looks like; in-game vs login-screen was irrelevant.

Fix: Cook's `run()` now mirrors TI -- `scheduleWithFixedDelay` driving `step()`, with the logged-out watchdog INSIDE the lambda (throttled `Build 394: logged out X min ... mission gate waiting for login` marker + 6-min `System.exit(0)` for Supervisor relaunch). Mission-select gate unchanged. TI marker bumped to 394 (no logic change). version.txt=391 live.

ACCEPTED LIVE 13:07:32-13:07:52 (Alex, local client.log authoritative): jar 69,044,223 bytes 13:07:14, patch-version.txt=391; `Build 394 STARTUP -- RUNNING_BUILD=394 (patch-391)` 13:07:32; `Build 394 TICK LOOP START`; 13:07:51 MISSION_SELECT claim (desired=cooks) -> verify (lockOwner=cooks, selfEnabled=true, otherEnabled=false) -> `SWITCH COMPLETE -- cooks owns scheduler`; 13:07:52 entered DETECT, forced GET_GRAIN. This proves the missing-tick-loop root cause. Pending: ONE bounded grain action with next-tick inventory/position/state verification (GET_GRAIN entry does NOT count as action proof); stand down with diagnostics if no progress.

## Build 393 / patch-390 (2026-09-29 ~12:58 EDT) -- DONE parks without finish() (the dead-command-channel root cause)

Alex's bytecode finding: the run tick's DONE branch called `finish("Tutorial Island complete!")` BEFORE the later command-poll block; `finish()` -> `shutdown()` killed the whole script tick, so the 45s GitHub poll could never process SWITCH_TO_COOKS after completion (client.log silent after the 12:47:05 completion line -- only external WebWalk telemetry).

Fix: Tutorial Island completion now PARKS without `shutdown()` -- housekeeping (screenshots, update check, command poll, pause handling) runs FIRST every tick, only quest-stage logic is held; throttled marker `Build 393: parked DONE -- quest logic held, housekeeping alive (command channel polling)`. Same lifecycle-safe DONE in Cook's Assistant (a future SWITCH_TO_TUTORIAL would have starved identically). No route logic changed. version.txt=390 live.

ACCEPTED LIVE 12:59:10 (Alex, local client.log): TutorialIsland executed SWITCH_TO_COOKS (id=20260929-165742-switch-cooks-2), `startPlugin(CooksAssistantPlugin)` returned true, Cook's logged `Plugin enabled` + `Build 393 STARTUP/RUNNING_BUILD=393 (patch-390)`, then Tutorial Island logged disabled + SWITCH COMPLETE. The parked-DONE command channel works.

## Build 392 / patch-389 (2026-09-29 ~12:46 EDT) -- MISSION_SELECT ownership gate (Alex's fix for the 12:28 14s Cook toggle)

Live 12:28:30-12:28:44: Cook's Assistant was enabled then disabled 14s later with no SWITCH COMPLETE -- the toggle came from outside the scripts. Fix: mission selection is now the first post-login phase in both scripts, driven by `~/.runelite/bot-mission.txt` (cooks|tutorial). Desired==self: claim (disable+stop the other via PluginManager, write bot-mission-lock.txt), verification tick re-checks lock owner + overlay + other stopped, then SWITCH COMPLETE -- only then does quest logic run. Desired==other: yield. Fail-safe: desired plugin missing from jar -> stay on self.

ACCEPTED LIVE 12:46:56-12:47:03 (Alex, local client.log): jar 69,042,509 bytes 12:46:17; `Build 392: STARTUP -- RUNNING_BUILD=392 (patch-389)` 12:46:30; `MISSION_SELECT -- desired=tutorial (self)` + claim 12:46:56; verify `lockOwner=tutorial, selfEnabled=true, otherEnabled=false` 12:47:03; `SWITCH COMPLETE -- tutorial owns the scheduler`; `Tutorial Island complete!` 12:47:05 (already DONE, parked).

## Build 389 / patch-386 (2026-09-29 ~11:45 EDT) -- completion parks in-game (kills the login/logout flap)

Stream verification 11:38-11:40: Build 388's logout DID fire -- login screen
seen ("WELCOME TO GIELINOR / CLICK HERE TO PLAY", "last logged in a minute
ago") -- but the client logged back in on its own within ~a minute.

Root cause: the Supervisor runs launcher_clicker.py --check-once every 30s;
it clicks CLICK HERE TO PLAY on ANY visible lobby and cannot distinguish an
intentional completion logout from a disconnect. The self-heal build IS
installed on Julien's PC (terminal showed "check-once: nothing to do"). A
plugin-side logout would flap login/logout forever -- a bot-detection signal,
worse than parking in-game.

Fix: doDone() no longer logs out in either script -- Tutorial Island parks
in-game at Lumbridge, fully idle and stable. Fresh startup deletes any stale
bot-intentional-logout sentinel. Same treatment in Cook's Assistant (no
logout on quest completion).

Staged in repo infrastructure/: sentinel-aware Supervisor.bat (skips the
--check-once login click while %USERPROFILE%/.runelite/bot-intentional-logout
exists) + README with reinstall steps. Julien installs manually when home;
a later build can re-enable logout-on-completion.

Patch-386.zip verified healthy: 399,865 bytes, 115 entries, all under net/,
both scripts present, zero zero-byte files. version.txt=386 live.

Pending verification: Build 389 startup banner ("Build 389: STARTUP --
RUNNING_BUILD=389 (patch-386)"), "parking in-game" diag line, and NO
login/logout cycling on stream. Screenshot feed dark since 10:38:42 -- the
stream is the only live witness.

## Build 388 / patch-385 (2026-09-29 ~11:40 EDT) -- completion logout actually logs out

Julien (not at PC) confirmed on stream: character idle ~50 min at the
Lumbridge General Store with the unclicked Adventurer Jon guidance dialogue.
Plugin was alive (patch checks every ~60s) but nobody logged the character
out after the Tutorial Island completion.

Root cause: the Build 385 logout lived in doMagic(), which is unreachable
after completion -- the varp-authoritative detector returns Stage.DONE (never
MAGIC), so case DONE -> finish() ran shutdown() with the character still
logged in. Rs2Player.logout() is fire-and-forget (LOGOUT tab + Logout menu
entry on widget 69:3; silently no-ops if that widget is null).

Fix: Stage.DONE is now handled by doDone() -- one-shot logout, verify
!isLoggedIn(), one re-issue at 30 ticks, finish anyway at 60 ticks. The dead
Build 385 block was removed from doMagic(). The Build 194 logged-out watchdog
exit(0) is suppressed after an intentional completion logout (client parks at
the login screen instead of relaunch/relogin looping). Same watchdog
suppression applied to Cook's Assistant (its doDone() logout placement was
already correct); its BUILD_NUMBER bumped 387 -> 388.

NOT live-verified yet. Expect on next update check: client restart, then
"Build 388: logout() issued" + "logout verified" diag lines, then the login
screen on stream.

## Build 387 / patch-384 (2026-09-29 ~11:25 EDT) -- Cook's Assistant: research-verified fixes

Julien asked for online research on the quest + overlay verification. Research
done (OSRS wiki mirrors, rune-server dialogue dumps, an osrs-ai-bot plan):

- Quest-start dialogue is a 1-1-4 trap: menu 1 "What's wrong?" -> 1, menu 2
  "I'm always happy to help a cook in distress." -> 1, menu 3 "Actually, I
  know where to find this stuff." -> 4. Pressing 1 on menu 3 loops the flour
  explanation forever. The script already picks options BY TEXT
  (COOK_START_OPTIONS String[]), which handles this correctly -- verified.
- Dairy cow: "Prized dairy cow" (RS3 name) tried first, falls back to
  "Dairy cow" (OSRS). Eastern Lumbridge cow field (Gillie Groats' pen, near
  the Al-Kharid toll gate) -- script's (3256,3273) is in the right field.
- Bucket: wiki confirms a ground spawn "in the Lumbridge cow pen" -- script
  searches radius 20 there. Egg at chicken farm, wheat "Grain" item name,
  Hopper/Hopper controls/Flour bin object names all confirmed.
- Cook tile corrected to (3209,3214) per multiple sources.
- Update oracle already reads bundle/patch-version.txt (no hardcoded const).

## Build 386 / patch-383 (2026-09-29 ~11:20 EDT) -- NEW: Cook's Assistant quest bot
(prior brief content retained below)
## Build 386 / patch-383 (2026-09-29 ~11:20 EDT) -- NEW: Cook's Assistant quest bot

Julien's new order: after Tutorial Island, automate a beginner Lumbridge quest
as a SEPARATE toggleable plugin (same overlay, same launcher). He picks the
quest; I built it.

- Quest chosen: **Cook's Assistant** (the classic first quest -- bucket of milk,
  egg, pot of flour for the Lumbridge Castle cook). Deterministic, no combat,
  all in/around Lumbridge.
- New package `net.runelite.client.plugins.microbot.cooksassistant`:
  `CooksAssistantPlugin` (descriptor name "Cook's Assistant", appears in the
  Microbot overlay next to "Tutorial Island"), `CooksAssistantConfig`,
  `CooksAssistantScript` (StateMachineScript).
- Same launcher/Supervisor untouched. Julien deactivates "Tutorial Island" and
  activates "Cook's Assistant" in the overlay himself.
- Route: Cook (start) -> pot (kitchen table ground spawn) -> grain (wheat field
  Pick) -> Mill Lane Mill (ladder to plane 2, grain on Hopper, Operate Hopper
  controls, ladder down, pot on Flour bin) -> egg (chicken farm ground spawn)
  -> bucket (cow field ground spawn) -> bucket on dairy cow (Prized dairy cow,
  fallback Dairy cow) -> Cook (finish).
- Observed-state step model: every tick recomputes from Quest.COOKS_ASSISTANT
  getState() (NOT_STARTED/IN_PROGRESS/FINISHED), EXACT inventory counts
  (equalsIgnoreCase -- "Pot" never matches "Pot of flour", "Bucket" never
  matches "Bucket of milk"), dialogue state, full WorldPoint incl. plane.
- Completion ONLY from QuestState.FINISHED (game-verified). DONE does the
  one-shot Rs2Player.logout() BEFORE shutdown (Build 385 lesson: logout after
  finish() is unreachable).
- Same infra as Tutorial Island: diag to ~/.runelite/cooks-assistant-diag.log,
  1-min canvas screenshots to bundle/screenshots/ (COOKS_ prefix), same
  version.txt update oracle (exits for Supervisor to apply new patches).
- NOT yet live-tested. Needs a fresh run with the plugin enabled in Lumbridge.
  Watch: mill ladder plane transitions, hopper/controls/bin object names,
  dairy cow NPC name, Cook's start-dialogue option texts.

## Build 385 / patch-382 (2026-09-29 ~10:40 EDT) -- LOGOUT ON COMPLETION (unverified)
- Julien: "maybe add a logout when done?" Added Rs2Player.logout() in doMagic()
  after hasCompletedTutorialIsland(). KNOWN DEFECT: unreachable -- onState()
  calls finish() and returns before doMagic() reaches it. Fix in a later build:
  one-shot logout in the top-level completion path before finish().
- Shipped after the successful run; logout behavior unverified.

## Build 384 / patch-381 (2026-09-29 ~10:33 EDT) -- CACHE-MISMATCH CHECK: GROUP ID, NOT PARENT WALK
- (prior brief content retained below)
## Build 384 / patch-381 (2026-09-29 ~10:33 EDT) -- CACHE-MISMATCH CHECK: GROUP ID, NOT PARENT WALK
- Build 383 PARTIALLY APPLIED, FIX MISSED: 10:28:44 startup banner
  RUNNING_BUILD=383 (patch-380) confirmed live, but the bot STOOD DOWN AGAIN
  on the same chatbox text -- 'Build 383: cache-mismatch text is inside the
  chatbox -- ignoring' NEVER fired (diag 10-29-45, only 2 'Build 383' lines:
  STARTUP + the description comment).
- Root cause of the 383 miss: the fix depended on Rs2Widget.getWidget(162,0)
  returning non-null -- but 162 is the FIXED-mode chatbox root and Julien's
  client renders the resizable-modern layout, so the root lookup is null/absent
  -- AND on Widget.getParent() walking through the chatbox tree (chat lines
  are dynamic children; the chain never reaches the 162:0 object). Both
  preconditions fail silently -> fell through to 'return w' every tick.
- Build 384 fix: no parent walk, no root lookup. RuneLite packs the interface
  group into the widget id, so (getId() >>> 16) identifies the subtree
  directly: 162 = fixed chatbox, 216 = resizable-modern chatbox,
  106 = resizable-classic chatbox. Any match = chat text, ignored as a glitch
  dialog (the ORIGINAL pre-224 handling), never a blocker. A REAL blocker
  overlay (Build 330: sat OVER the chat box) lives in its own interface group
  and still takes the 224 dismissal path.
- Verify next run: 'Build 384: cache-mismatch text is a chatbox widget (group
  NNN)' lines, NO stand-down, bot walks south to Brother Brace,
  varp 281 550 -> 560+.

## Build 383 / patch-380 (2026-09-29 ~10:28 EDT) -- CACHE-MISMATCH CHATBOX FALSE POSITIVE
- Build 382 VERIFIED LIVE: 10:23:22 startup banner RUNNING_BUILD=382; 10:23:52
  'Build 382 DOOR-2: door ... ADJACENT to player (dist=1) -- clicking Open';
  10:23:54 'Open' click issued on 9722; 10:23:55 varp281=550 -> PRAYER forced
  forward. Bank exit CONFIRMED by game state (varp 540->550). Door saga over.
- NEW BLOCKER 10:23:39-10:24:22: bot stood itself down permanently --
  'Build 224: cache-mismatch overlay persists after 3 dismissal attempts'.
  Root cause: the client prints 'Mismatch in overlaid cache archive hash for
  12/84' as CHATBOX text lines; findCacheMismatchWidget()'s global text find
  matched those chat widgets (not hidden) and treated chat history as a
  blocking modal. 3x Space = no-ops on chat text -> stand-down while the game
  sat fully playable (screenshot 10:24:22: no modal, player outside bank).
- Fix: walk the matched widget's parents -- chatbox root (162:0) as ancestor
  means chat text, ignored as a glitch dialog (the ORIGINAL handling), never
  a blocker. A REAL overlay lives OUTSIDE the chatbox subtree (Build 330
  precedent: it sat OVER the chat box) and still triggers the 224 path.
- Pending verification: 'Build 383: STARTUP -- RUNNING_BUILD=383 (patch-380)',
  'Build 383: cache-mismatch text is inside the chatbox -- ignoring' lines,
  NO stand-down, bot walks south to Brother Brace, varp 281 550 -> 560+.

## Build 382 / patch-379 (2026-09-29 ~10:21 EDT) -- ADJACENT-DOOR EXCEPTION to the 381 filter
- Live 10:14-10:16 (Build 381): player stood at (3129,3124) FACING the exit
  door 9722@(3130,3124) -- screenshot 10:16:12 shows the closed double door with
  lanterns, the bank's south exit. 381 ignored it (doorY == playerY) and
  walkStep'd south THROUGH the closed door (a wall): player immobile 2+ min,
  DOOR STUCK fired 10:15:09 + 10:15:51, 381's own verify clause
  (y 3124 -> <=3121) NEVER happened.
- Root cause: a door object sits in the wall row, so the exit door the player
  is STANDING AT shares the player's Y -- "not south" != "not the exit".
- Fix: an ADJACENT door (chebyshev <= 2) is always actionable via the existing
  clickDoorDirect 'Open' path; the 381 ignore-and-walk-south now applies only
  to NON-ADJACENT doors. (The 09:49 9721 spin stays bounded by the 5-tick
  DOOR STUCK watchdog + verifyDoorCrossing on observed tile change.)
- CORRECTION: the 10:10/10:14 runs' "DOOR-2 CROSSED" claims were FALSE -- the
  player never left the bank (y stayed 3124; the WebWalk cur==goal=(3124,3108)
  line was the walker's internal claim, not observed position).
- Pending verification: 'Build 382: STARTUP -- RUNNING_BUILD=382 (patch-379)',
  'Build 382 DOOR-2: door ... ADJACENT' lines, 'Open' click on 9722, y
  3124 -> <=3121, 'DOOR-2 CROSSED', varp 281 540 -> 550+ (PRAYER).
- Note: sibling edited TutorialIslandScript.java 10:02:26 EDT without shipping
  (version.txt stayed 378 for 14+ min); compiled their edit IN and shipped
  under fresh version 379 -- their change is preserved, not clobbered.

## Build 381 / patch-378 (2026-09-29 ~09:55 EDT) -- DOOR-2 ignores lateral doors (9721 fix)
- Live 09:49-09:53 (Build 380): bank-exit DOOR-2 fixated on Door/9721@(3125,3124)
  -- an EAST-WEST door AT the player's own Y -- clicking 'Open' every ~13s for
  4+ min while the player oscillated (3125,3124)<->(3124,3124) with ZERO
  southward progress (exit needs y<=3121). Screenshots 09:49:56/09:52:10 show
  the door CLOSED and the game's yellow tutorial arrow pointing SOUTH
  off-screen (the exit direction) while the bot faced NORTH at the wrong door.
- Root cause: Build 368's 'behind' filter only ignored doors with
  doorY > playerY (strictly north); a lateral door (doorY == playerY, e.g. 9721)
  passed the filter and the nearest-door picker returned it every tick.
- Fix: DOOR-2 now ignores any door NOT south of the player
  (doorY >= playerY), walking south via non-blocking walkStep toward (x,3120)
  instead. A door becomes actionable only when actually SOUTH of the player.
- Pending verification: 'Build 381: STARTUP -- RUNNING_BUILD=381 (patch-378)',
  'Build 381 DOOR-2:' ignore lines, player tile y decreasing (3124 -> <=3121),
  then DOOR-2 CROSSED / advance toward PRAYER.
- Note: sibling edited TutorialIslandScript.java 09:47:10 EDT without shipping
  (no Build 381 marker, version.txt stayed 377); shipped over it with a minimal
  2-spot edit after 6+ min of sibling silence -- their unshipped change is
  preserved in the source tree.

## Build 380 / patch-377 (2026-09-29 ~09:48 EDT) -- ACC_MAN via tutorial bottom-line icon 164:54
- Live 09:32-09:40 (Builds 374-379): the poll-dismissal loop ran
  "clickTabIcon ACC_MAN" every ~2s and EVERY attempt logged "NO VERIFIED
  TARGET" -- packed 548:72/161:67 resolve with null bounds (ComponentID-derived,
  never valid on the tutorial tab bar) and the WorldModel name-matcher skips
  every tutorial icon (names are ''). The Build 124 scan PROVED the icon is
  rendered: 164:54 [600,591 33x36] = the 3rd tutorial bottom-line icon = the
  compass = the flashing account icon (visually confirmed in the 09:24:15 and
  09:40:19 tab-bar crops).
- Fix: appended packed 164:54 (10747958) to the ACC_MAN case in
  clickTabIconVerified, LAST (the two dead IDs resolve null and are skipped,
  164:54 is the first real hit). NOTE: 10747958 also sits in the QUESTS case as
  the bottom-line stone -- on the Tutorial Island tab bar it renders the account
  compass during the poll phase (visually confirmed); quest-guide phase is long
  past. The flashing icon REQUIRES the physical click (script-915 fallback never
  sets the game's tutorial flag).
- Pending verification: "Build 380: STARTUP -- RUNNING_BUILD=380 (patch-377)",
  "Build 380: ACC_MAN 164:54 bottom-line compass icon RESOLVED",
  "clicking packed(164:54", "MOUSE CLICKED ACC_MAN", account panel opening,
  then varp 281 -> 530.
- Note: Build 379 (patch-376, stall-driven door open) shipped 09:35 but was
  NEVER observed live (no Build 379 lines through 09:40:19); its code is in the
  cumulative source. Also: a sibling edited the source 09:39:44 EDT without
  shipping -- no conflict detected at ship time (version.txt 376 -> 377 fresh).

## Build 378 / patch-375 (2026-09-29 ~09:25 EDT) -- gate open via PHYSICAL left-click (doInvoke menu bug class)
- Live 09:15-09:18 (Build 377): poll-stuck walk clicked the bank's south large
  double door with clickDoorDirectInner("Open") every ~8s for 2+ min
  ("Cache query: click('Open') returned true") yet the door NEVER opened and
  the player never moved from (3121,3118); 09:17:58 screenshot shows the
  right-click menu OPEN on the door ("Open Large door / 2 more options").
- Root cause: the Build 88/90 bug class -- Rs2TileObjectModel.click(action)
  routes through Microbot.doInvoke -> mouse.click(point, entry), which OPENS
  the right-click menu but never selects the row; returns true unconditionally.
- Fix: "Open Large door" is the door's DEFAULT (top) menu entry, so a plain
  physical left-click fires it (leftClickObject, the proven Build 88/90 path).
  New clickGatePhysicalOpen(radius) replaces both clickDoorDirectInner("Open")
  call sites in the poll-stuck walk. Verified by observed door/walk state next
  tick, never by the click return.
- Pending verification: "Build 378: STARTUP -- RUNNING_BUILD=378 (patch-375)",
  then "Build 378: physical left-click on gate ..." lines, door observed open,
  player tile moving south through it, then guide dialogue + varp 281 -> 530.
- Note: Build 376 (patch-373) was SKIPPED live -- bot went 375 -> 377 directly
  (09:13:45 restart); 376's dismissal code is still in the cumulative source.

## Build 374 / patch-371 (2026-09-29 ~08:43 EDT) -- poll title TRUE-POSITION click + false-phase re-talk
- Root cause 1, PROVEN by screenshots (Build 373, 08:30-08:36): 160+ dismissal
  ticks clicked the title widget's REPORTED bounds center (259,569) -- but the
  rendered blue "(Moving on" title sits at ~(328,536); the red virtual-mouse
  click marker lands on body text (~342,563), which does nothing. The
  hidden=true widget's geometry is STALE (bounds don't track the rendered box)
  -- Builds 372/373's premise ("click the title widget's bounds") was wrong at
  the geometry level, not the target level.
- Fix 1: clickPollDialoguePhysical rewritten -- enumerate ALL widgets whose
  text contains "Moving on" (no hidden bail), one-time diag dump of every
  candidate (packed id, text, bounds, hidden), click the first with sane
  on-canvas bounds sitting ABOVE the body text widget ("Polls are run
  periodically"); fallback is a body-anchored click at (bodyCenterX,
  bodyTop-18), the fixed-layout title slot; resolved point cached (fixed
  layout), full-tree scan runs max 3x; one click/tick, verified by observed
  box-gone next tick.
- Root cause 2: the 08:35 guide phase 0->1 advance was BOGUS -- the "dialogue
  seen then closed" was the Build 224 cache-mismatch overlay (dismissed
  08:35:22-23), NOT the guide's dialogue (talk initiated at dist=12,
  unreachable in 3s). Proof: the account tab is ABSENT from the rendered tab
  bar (screenshots 08:36+) -- his "click the flashing icon" instruction never
  fired -- so phase 1 spins clickTabIcon ACC_MAN with NO VERIFIED TARGET.
- Fix 2: phase 1 with no verified tab target for 30 ticks resets to phase 0
  for a genuine re-talk (bounded 3x); the existing 150-tick fail-safe still
  applies afterwards.
- Pending verification: "Build 374: STARTUP -- RUNNING_BUILD=374 (patch-371)",
  then "Build 374: resolved (Moving on title" + click lines, then the box
  observed gone; and "Build 374: no account-tab target for 30 ticks --
  resetting to phase 0" followed by a genuine guide dialogue (dialogueTick)
  with the account tab appearing/flashing in screenshots. Note: 08:42:02+
  diag already shows a re-talk to the guide at dist 10-11 (could be Build
  373's 150-tick fail-safe phase-2 -- banner check will disambiguate).
- Note for Alex: tab icons render fine but every widget lookup returns
  hidden/null-bounds -- the same false-hidden class as the poll title. Build
  79's cache-corruption path already wrote clear-cache-requested.txt, so the
  Supervisor will clear the RuneLite cache on the next launch; watch whether
  widget reads recover after that.

## Build 368 / patch-366 (2026-09-29 ~07:52 EDT) -- door reachability + direction + non-blocking step
- Root cause, PROVEN live (Build 367, 07:46:27-07:51:38): the exit-first plan
  WORKED -- doors 1535/1536 opened, player moved (3123,3127)->(3124,3125) --
  then stalled forever. findExitDoorForSubstate picked Door/1535@(3124,3126)
  NORTH of the player (behind the south-exit travel direction) and
  isDoorReachable() tested Rs2Tile.isTileReachable on the DOOR TILE -- a WALL
  for a closed door, so a closed door could NEVER be clicked ("door not
  reachable. Cannot issue action." every tick; the 5-tick no-progress watchdog
  fired 07:51:06 and stood the routine down). Same bug class Build 154 fixed
  for door 9722, but in Build 134's DOOR-2 path. Also the post-click
  Rs2Walker.walkTo(beyond) blocked the tick thread ~11-13s per cycle
  (07:46:42->07:46:55, 07:46:57->07:47:08) -- hang-rule violation.
- Fix: (1) isDoorReachable -- player-adjacent (chebyshev<=1) counts as
  reachable; the door tile itself is never required (open door OR any adjacent
  tile reachable as fallbacks); (2) door-2 ignores doors behind the travel
  direction (doorY > playerY) and walks south via non-blocking walkStep
  instead of re-clicking the door it came through; (3) post-click walkTo is now
  one non-blocking walkStep per tick.
- Pending verification: "Build 368 DOOR-2:" behind-door ignores, reachable=true
  on adjacent closed doors, player tile moving south every tick
  (y 3125 -> <=3121), then proximity walkStep + "Build 88: physically
  left-clicked poll booth", varp 281 -> 530.
- Note for Alex: the picker still returns the NEAREST named door -- if two
  doors ahead both show Open, only the first gets clicked per tick; the
  behind-door filter only kicks in when the picker returns a door north of the
  player. Watch whether 1535/1536 cache staleness (Open after auto-close)
  causes a click-Open-on-already-open-door no-op cycle.

## Build 367 / patch-365 (2026-09-29 ~07:45 EDT) -- poll-booth EXIT-FIRST (the booth is outside the bank)
- Root cause, PROVEN by Build 365's identity log (07:33-07:40): the poll booth
  (object id=26815) is at WorldPoint(3119,3121,plane 0) -- OUTSIDE the bank's
  south door (y<=3121 = outside per Build 90/86; the bank door row is y~3124).
  The player stood inside at (3123,3127). The BFS unreachability was CORRECT,
  not stale collision data. (A sibling's Build 366 / patch-364, shipped ~07:36
  mid-investigation, theorized stale collision and walked directly at the booth
  tile -- stalled into the closed door, dist frozen at 6. Its wall-walking
  fallback is removed.)
- Fix (step-model law -- the poll step now has an explicit ordered plan):
  (1) while the player is inside (y>3121) and the booth is outside (y<=3121),
  run the bank-exit door routine -- Build 134's two-door state machine
  extracted verbatim into doBankExitDoors(), one action per tick;
  (2) once outside (observed y<=3121), the existing proximity gate + physical
  click runs. The poll-booth finder (exact -> loose -> object-id) is extracted
  into findPollBooth() so the exit branch can observe the booth's tile. The
  click and varp 281 -> 530 completion gates are untouched.
- Pending verification: "Build 367: poll booth is OUTSIDE ... exiting the bank
  first", DOOR-2 crossing (y 3127->3121 observed), proximity walkStep, then
  "Build 88: physically left-clicked poll booth" and varp 281 -> 530.
- Note for Alex: after the poll booth, doAccountGuideStep already walks back
  INSIDE the bank from outside (walks to (3122,3126)) for the varp-530 guide
  talk -- watch whether the (already opened) south door lets it back in.

## Build 364 / patch-362 (2026-09-29 ~07:26 EDT) -- wrong-floor (plane 1) recovery for the bank poll-booth arc
- Live result (Builds 362/363, 07:15-07:22): the player is at
  (3123,3127,plane=1) -- UPSTAIRS in the bank. Julien on stream: "it missed
  click and went upstairs in the bank", "a place you've never seen it". The
  poll booth is on plane 0, so every poll-booth lookup from plane 1 misses BY
  CONSTRUCTION -- Builds 362/363 could dismiss the box and still never finish.
  (Build 363, shipped by a sibling worker as patch-361 while I investigated,
  added the modal continue-fallback + poll-booth object-ID lookup
  26492/26796; kept as-is, built on.)
- Fix: after the modal gates, when bank varp is 520-539 and observed plane is
  1, nothing else runs -- one recovery tick: find the nearby ladder by object
  id 16679 (observed live at dist=1, name "Ladder") or cached name-contains
  "ladder", require its plane == 1, walk to adjacentWalkable(ladder) when
  dist>2, else one physical ladder.click("Climb-down"). Completion is the
  OBSERVED plane 1->0 transition; bounded 40 ticks then diagnostic
  stand-down. Rs2TileObjectModel has no getActions(), so identity rests on
  id+name+plane+proximity with full evidence logged -- a wrong-object click
  fails closed, never a false success.
- Pending verification: Build 364 banner (RUNNING_BUILD=364), "Build 364:
  down-ladder candidate" lines, "physically clicked Climb-down", plane 1->0
  observed, then the poll-booth ID match and varp 281 -> 530.

## Build 363 / patch-361 (2026-09-29 ~07:24 EDT) -- poll-box title-detector miss + nameless poll booth (review-loop worker)
- Live result (Build 362, 07:15:57-07:22): RUNNING_BUILD=362 banner and the
  "varp281=520 >= 520 -- syncing true" line verified, but the Poll-booths box
  sat visibly open the whole run while pollBoothsBoxOpen() returned false
  EVERY tick (same miss class as Build 360's Banking-box detector: findWidget
  title/body lookups unreliable for these modals) -- and the poll-booth step
  kept missing because names resolve null for nearly every cached object
  (Build 361 MISS DIAG: 451 cached, only Ladder named), so exact "Poll booth"
  AND loose "poll" both miss.
- Fix: (1) modal continue fallback in doBank -- when no dialogue is flagged
  yet a continue control is visible, the box owns the tick: one
  clickContinueOnce() per tick (the proven 06:56:45 Banking-box dismissal
  path), nothing else, until observed gone; (2) object-ID fallback for the
  poll booth (OSRS Wiki ids 26492/26796, blue closed/open) via findObjectById
  -- needs no name resolution.
- Superseded before verification: Build 364 shipped ~2 min later with the
  plane-1 recovery on top; 363's fixes ride along in it.

## Build 355 / patch-353 (2026-09-29 ~06:05 EDT) -- smith-arc info box + dagger substring-trap fix (review-loop worker)
- Live result (Build 354, session started 05:59:30): RUNNING_BUILD=354 banner
  confirmed at 05:59:30; "Build 354: hammer next step=dialogue" fired at
  05:59:57 with spamDialogue continuing the instructor dialogue. The HAMMER
  ARRIVED -- screenshot 06:00:31 shows hammer in inventory, tutorial
  instruction now "Click the anvil to begin smithing. You must make a bronze
  dagger." So the Build 354 ownership gate worked.
- New blocker found at 06:00: with the hammer in hand, the "Smithing a
  dagger" info box (group 229, bottom of screen) sat open and NOTHING owned
  it (Build 353's gate only covers the no-hammer case; the box is invisible
  to isInDialogue()). The smith section then fired with a substring trap:
  smithUiOpen's findWidget("Dagger") fallback matched the info box's own
  TITLE text, so Phase 3 ran with no smithing UI on screen --
  clickSmithingDagger clicked a text widget at (146,66), a mine-dagger
  42-tick wait armed for a dagger that could never come. Same trap class as
  Build 351 ("Bronze" matching the bronze pickaxe) and the dialogue-text
  "bronze bar" trap.
- Fix: (1) miningInfoBoxOpen() also matches "Smithing a dagger" (phrase
  collides with no item name); (2) new SMITH-ARC ownership gate -- while
  holding bar+hammer with no dagger, an observed info box gets one dismiss
  action per tick and nothing else runs until observed closed, never ESC'd;
  (3) smithUiOpen and clickSmithingDagger now use exact
  findWidget("Bronze dagger", true) only -- the "Dagger" substring fallback
  is DELETED.
- Pending verification: Build 355 banner (RUNNING_BUILD=355), "Build 355:
  smith info box OPEN" lines, then observed absence, then "Build 196:
  walking to anvil" and "Build 196: bronze dagger smithed (verified in
  inventory)".
- REVIEW QUESTION (updated): the pattern is now clear -- EVERY tutorial info
  box needs an owning arc gate above the generic mine-esc2 ESC branch, and
  every widget lookup for an ITEM NAME must be exact (substring matches item
  text, info-box text, and dialogue text indiscriminately). Worth an audit
  pass over the remaining substring findWidget(...) call sites in doMining
  before the combat/bank arcs hit their own boxes.

## Build 354 / patch-352 (2026-09-29 ~05:58 EDT) -- hammer-dialogue ESC ping-pong fix (review-loop worker)
- Live result (Build 353, session started 05:53:43): RUNNING_BUILD=353 banner
  confirmed, update restart relogged, varp281 320 -> 330 (post-bar info box
  cleared by the relog -- the Build 353 detector never fired its "post-bar
  info box OPEN" line, so the detector's dismiss path is still not live-proven;
  the box self-cleared on relog as warned). Bot moved to hammer: Talk-to
  issued 05:54:39, mine-esc2 armed 05:54:41, Talk-to re-issued 05:54:43,
  dialogue open at 05:54:44 ("I have a bronze bar. What now?").
- Root cause: the generic mine-esc2 ESC branch in doMining (fires on ANY
  dialogue-open tick when the smelt gate doesn't own it) closed the
  instructor's HAMMER dialogue one tick after talkTo opened it, then talkTo
  re-issued -- deterministic Talk-to -> dialogue opens -> ESC closes ->
  Talk-to ping-pong. The ESC branch sits ABOVE the hammer section in doMining,
  so spamDialogue() never got a turn and the hammer could never arrive. Same
  family as the Build 337 chef frame-1 loop and the Build 350 mining-intro
  ESC loop, one level up: not a reset-click, a dialogue-kill.
- Fix: hammer-arc ownership gate in the dialogue chain, right after the
  Build 351 smelt gate -- while the hammer section's own condition holds
  (no bronze dagger, no hammer, from observed inventory), an open dialogue
  gets one spamDialogue action per tick, never ESC'd. Mirrors the smelt gate.
- Pending verification: Build 354 banner (RUNNING_BUILD=354), "Build 354:
  hammer next step=dialogue" lines, instructor dialogue advancing through
  continue clicks, "Build 196: hammer received (verified in inventory)".
- REVIEW QUESTION (open): the generic mine-esc2 ESC branch is now shadowed by
  the smelt and hammer gates -- every arc that opens a dialogue needs its own
  ownership gate ABOVE that branch, or the branch needs retiring in favor of
  per-arc gates. The anvil/smithing path is UI-based (no dialogue), so it is
  unaffected. Flag if the ESC branch still serves a live purpose.

## Build 353 / patch-351 (2026-09-29 ~05:52 EDT) -- hammer-arc info box soft-lock fix (review-loop worker)
- VERIFIED LIVE 05:53:43: RUNNING_BUILD=353 banner; update restart relogged;
  varp281 320 -> 330. Caveat: the "Build 353: post-bar info box OPEN"
  detector line never fired -- the box self-cleared on the update-restart
  relog, so the detector's dismiss path is NOT live-proven yet (same pattern
  as the Build 352 smelt-box dismissal).
- Live result (Build 352, session 05:29:06): BRONZE BAR SMELTED 05:45:57-05:46:06
  (Build 352 fix verified: smelt-intro info box dismissed, adjacent furnace
  walk SATISFIED 05:45:55, exact "Bronze bar" smelting-UI match, bar in
  inventory, "Skipping smelt -- already have bar/dagger"). Bot moved to hammer
  ("Getting hammer from Mining Instructor"), but then stranded: live
  05:48:47-05:49:09 the POST-BAR tutorial info interface ("You've made a
  bronze bar! Speak to the mining instructor...") stayed open 2+ min while
  talkTo("Mining Instructor") issued Talk-to clicks every ~2-4s -- every click
  swallowed by the modal box, the instructor dialogue NEVER opened, no
  mine-hammer wait ever armed, mine-esc2 ESC never closed it. The Build 352
  smelt gate's detector phrase ("tin ore and some copper ore") did not match
  this box -- it is ANOTHER group-229 interface, invisible to
  Rs2Dialogue.isInDialogue() and the WorldModel dialog sampler (both key on
  162/219/193/231), exactly as warned in the 05:47 run.
- Fix: (1) info-box detector generalized to miningInfoBoxOpen() -- matches
  EITHER the smelt-intro box ("tin ore and some copper ore") or the post-bar
  box ("You've made a bronze bar") by distinctive body phrase (substring;
  neither phrase collides with an item name -- no item-name trap); (2) new
  HAMMER-ARC ownership gate: while holding the bar with no hammer/dagger, an
  observed info box gets one dismiss action per tick (continue-widget click
  else Space) and NOTHING else runs until observed closed, never ESC'd.
- Pending verification: Build 353 banner (RUNNING_BUILD=353), "Build 353:
  post-bar info box OPEN" lines, box observed closed, instructor dialogue
  opens, hammer in inventory.
- REVIEW QUESTION (open): the same group-229 info interface appears at other
  stages (quest intro, combat, bank, prayer, magic) -- each stage needs the
  same ownership gate, or one generic "any group-229 info box observed ->
  dismiss-first" rule keyed to stage. Flag if you see a cleaner detector.

## Build 351 / patch-349 (2026-09-29 ~05:35 EDT) -- smelt substring-trap fix (review-loop worker)
- Live result (Build 350, session 05:29:06): mining intro FULLY verified --
  "next step=talk/dialogue/done", pickaxe in inventory 05:29:39, Tin rocks
  click verified 05:29:53, Copper rocks verified 05:30:03. Then the SMELT step
  deadlocked: "Smelting bronze bar" every tick 05:30:09->05:31:07, furnace
  never clicked, Smelting info dialogue ("Try it now. Click here to
  continue") stuck open (screenshot 05:31:07), tick-wait 'mine-smelt' armed
  bound 34 with no EXHAUSTED in the window.
- Root cause: the smelting-UI check used SUBSTRING findWidget -- "bronze bar"
  matched inside the OPEN Smelting DIALOGUE TEXT ("...smelt these into a
  bronze bar..."), so Phase 3 "UI open" fired with no UI on screen; the
  dialogue body got clicked and 'mine-smelt' waited for a bar that could never
  come. Separately, the mine-esc2 ESC never closed that dialogue (it needs a
  continue click).
- Fix: (1) smeltUiOpen now uses findWidget("Bronze bar", true) = exact
  equalsIgnoreCase (verified in microbot-base bytecode); (2) the "Bronze"
  fallback on the bronze-pick click is DELETED (it could match the Bronze
  PICKAXE in inventory -- same trap class); (3) the smelt flow owns its info
  dialogue: while holding tin+copper ore with no bar/dagger, an open dialogue
  gets one spamDialogue continue per tick instead of ESC. Step model now:
  dialogue -> bronze-pick (exact) -> furnace (use tin ore -> click furnace).
- Pending verification: Build 351 banner (RUNNING_BUILD=351), "Build 351:
  smelt next step=" lines, smelting info dialogue continued through, furnace
  clicked, smelting UI opened (exact "Bronze bar"), bronze-pick clicked,
  "Bronze bar" observed in inventory -> "Build 196: bronze bar smelted".

## Build 350 / patch-348 (2026-09-29 ~05:30 EDT) -- mining intro step model (review-loop worker)
- Live result (Build 349, session 05:21:03): quest fix FULLY verified -- Climb-down
  click landed on the LADDER (id=9726 at 3088,3119), descent verified
  (varp281=260, player in caves y>9000), stage advanced QUEST_GUIDE -> MINING at
  05:21:53. Then the MINING stage deadlocked immediately: Talk-to click ->
  dialogue open -> ESC -> Talk-to again, every ~3s, 05:22:49-05:24:04, dialogue
  pinned at frame 1 ("Hi there. You must be new around here...") forever.
- Root cause (two parts): (1) the mining intro block assumed "the explanation
  dialog has no continue button -- close it with ESC" -- WRONG, the intro HAS
  "Click here to continue" (screenshot 05:24:04) and the instructor hands the
  bronze pickaxe only when it is clicked through, so ESC'ing it skipped the
  handover; (2) the Build 337 talkTo skip's clearTickWait() nuked the intro's
  own mine-esc wait every tick, so the intro re-armed it every tick while
  talkTo() re-issued a fresh Talk-to click every ~3s -- each fresh click RESET
  the conversation to frame 1 (the exact Build-337 chef loop).
- Fix: mining intro converted to the proven Build 337 chef step model. Each
  tick derives mineStep from observed state and logs it
  ("Build 350: mining intro next step=<done|dialogue|talk>"): Bronze pickaxe in
  inventory (or dagger, or bar+hammer) -> done, never talk again; dialogue
  open -> one spamDialogue action per tick (60s budget, continue-first);
  else -> talkTo (bounded 3 attempts -> stand down; 337 guard prevents reset
  clicks while the dialogue is open). Completion predicate: Bronze pickaxe
  OBSERVED in inventory -- never the ESC, never dialogue-close alone. The old
  ESC intro block is deleted. Watch for: "Build 350: mining intro next step="
  lines, pickaxe appearing, then tin/copper mining (mineOre, rocks "Tin
  rocks"/"Copper rocks" -- needs the pickaxe).
- Verification pending: Build 350 banner (RUNNING_BUILD=350), intro spamDialogue
  continuing the instructor's dialogue, "Bronze pickaxe" in inventory, then the
  step=done transition. alex-inbox: no new notes.


## Build 349 / patch-347 (2026-09-29 ~05:20 EDT) -- quest ladder targeting + text-gated dialogue close (review-loop worker)
- Live result (Build 348, session 05:12:32): the re-talk loop is DEAD -- latch
  line "Build 348: Quest Guide lecture COMPLETE (observed ' would you like to
  hear about quests again?')" fired at ~05:13:00, option 2 picked, continues
  clicked, 4 blind Spaces pressed, bot advanced to the ladder step at 05:13:22.
  TWO new blockers found in the 05:13:32 screenshot + diag tail:
  (1) clickDoorDirect("Climb-down") uses findGateObjectInner which matches
  "door" FIRST -- the click landed on DOOR 9716 at (3086,3126), not the quest
  ladder at (3088,3119). The player walked to the door; no descent ever
  happened. (2) The "(Moving on)" final frame was STILL OPEN in the 05:13:32
  screenshot, 18s after the 4th blind Space -- the post-"No" dialogue chain
  has MORE frames than the bound of 4; the last frame appeared after the
  bound was exhausted. A world click can never land while a dialogue is open.
- Fix (1): clickDoorDirect now uses a ladder-first finder (findLadderObjectInner,
  name-contains "ladder") for any action starting with "Climb"; gate scan
  stays as fallback.
- Fix (2): replaced the fixed 4 blind Spaces with a TEXT-GATED close loop --
  while quest-guide markers ("moving on"/"enter some caves"/"click on the
  ladder"/"hear about quests again"/"quest guide") are readable, click
  continue (physical mouse, works regardless of isInDialogue flakiness) +
  Space once per tick, bound 40; when the text is unreadable 3 ticks running,
  the dialogue is closed and the ladder branch runs. Fail-open: if text is
  never readable, behavior = old (straight to ladder).
- Verification pending: Build 349 banner (RUNNING_BUILD=349), "Build 349:
  quest-guide dialogue text still readable" lines then "no quest-guide
  dialogue text (3/3)", then a Climb-down click on the LADDER (id != 9716),
  descent verified (varp>=260 / mining caves) -> MINING.


# Brief for Alex (ChatGPT) — Tutorial Island bot

## Build 348 / patch-346 (2026-09-29 ~04:57 EDT) -- quest-guide lecture-COMPLETE latch (review-loop worker)
- Blocker (live 04:46-04:53, Builds 346/347): INFINITE RE-TALK LOOP. After the
  journal opened (04:39:29), the game SKIPS the journal-nag frame, so
  questTabClicked never latches and the bot re-talks to the Quest Guide every
  ~26s forever -- each fresh Talk-to resets the dialogue to frame 1, so the
  Build 347 ladder branch is unreachable (347 IS live, banner 04:50:57, but
  its ladder lines never fire). Worse: the final "(Moving on)" frame
  ("It's time to enter some caves. Click on the ladder to go down to the next
  area.") is intermittently INVISIBLE to Rs2Dialogue.isInDialogue() --
  screenshot 04:52:59 shows it OPEN while the diag claims "dialogue seen 5
  ticks ago" (isInDialogue = 162:559-visible || hasContinue || hasOption, all
  false on this frame). So detection-gated handlers can never reliably close
  it, and the suppression window just re-arms the next Talk-to.
- Fix (Julien's step law -- explicit observed-state predicate): latch
  qgLectureComplete on the OBSERVED DIALOGUE TEXT (proven readable -- the
  04:52:42 diag line read "Would you like to hear about quests again?").
  Triggers: "hear about quests again" (the post-lecture option prompt, appears
  on EVERY talk), "enter some caves", "click on the ladder", "moving on".
  Once latched: questTabClicked=true + qgSecondTalkDone=true (the explanation
  already happened; NO second talk), and the bot NEVER talks to the Quest
  Guide again -- straight to the ladder branch. Plus: with the latch set, a
  bounded blind-Space press (4x, one per tick) closes the "(Moving on)" frame
  regardless of detection before the Build 347 walkStep-to-ladder-adjacent
  approach runs. Also fixed a stale source comment claiming the raw CDN lags
  ~5 min (probe 2026-09-29: no lag).
- Verification pending: Build 348 banner (RUNNING_BUILD=348), "Build 348:
  Quest Guide lecture COMPLETE" latch line, blind-Space lines, then Build 347
  "walking to ladder-adjacent" WITH player-tile movement, Climb-down click,
  descent verified (varp>=260 / mining caves) -> MINING.


## Build 347 / patch-345 (2026-09-29 ~04:50 EDT) -- quest ladder walk fix (review-loop worker)
- Blocker (live 04:39:32-04:41:51, Build 345): after the journal click the bot
  went to "walking to ladder at (3088,3119)" -- and NEVER MOVED. Screenshots
  04:40:56 and 04:41:58 are pixel-identical (player on the same tile); the
  qg-ladder-walk tick-wait exhausted its 17-tick bound 4 times and re-armed.
  Root cause: Rs2Walker.walkTo(ladderPos) on the ladder tile is a no-op there
  and the distanceTo<=3 arrival gate never satisfied.
- Fix: ladder approach is now non-blocking walkStep per tick toward
  adjacentWalkable(ladderPos) (reachability-verified tile, never the object
  tile -- the proven Build 339/340 door pattern), arrival verified by the
  observed player tile. The blind qg-ladder-walk wait is deleted; the
  qg-ladder-descend click/verify cycle is unchanged. Sibling's Build 346
  second-talk block left untouched.
- Verification pending: Build 347 banner (RUNNING_BUILD=347), "Build 347:
  walking to ladder-adjacent" lines WITH player-tile movement, Climb-down
  click, descent (varp>=260 / mining caves) -> questGuideDone.


## Build 346 / patch-344 (2026-09-29 ~04:46 EDT) -- quest-guide SECOND talk (main agent)
- Julien 04:43, live screenshot: game says 'Talk to the quest guide again for an explanation on how it works.' Worker claimed arc complete prematurely.
- Fix: explicit second-talk step after questTabClicked -- Talk-to again, spam explanation, done only on seen-then-closed. Overlay-window ride-out via qgDialogueSeenAgo.
- Verification pending: Build 346 marker, 'talking to Quest Guide AGAIN' + 'second dialogue seen-then-closed' lines, then ladder.

_Maintained by Muse. Updated on every build ship. If you have web browsing,
read this file raw before answering Julien about the bot — it's the current
ground truth, fresher than any forwarded summary._

## Current state (2026-09-29 ~04:41 EDT)
- Build **345** is LIVE (`patches/patch-343.zip`, `version.txt=343`, banner
  04:38:55, pickup ~1 min after upload). Packaging incident resolved.
- VERIFIED LIVE: 04:39:29 "Build 343: quest-guide wants the journal" +
  physical QUESTS click + questTabClicked latch; 04:40:56 screenshot shows
  the Quest Journal OPEN ("Completed: 0/184"); 04:40:42 the bot is "walking
  to ladder at WorldPoint(x=3088, y=3119, plane=0)" -- quest lecture done,
  descending to the mining caves (watch varp281>=260 -> MINING).
- Stage: **QUEST_GUIDE** (varp281=200) transitioning to MINING.
- PACKAGING INCIDENT (root cause of the 04:30-04:38 stall): patches 341/342
  (Builds 343/344) were zipped from the WRONG root -- their entries are
  `runelite/client/plugins/...` instead of `net/runelite/client/plugins/...`.
  Check-Update.ps1's `jar uf` therefore ADDED 100 junk entries instead of
  overwriting the real classes; the bot relaunched at 04:32:59 still on
  Build 342. The zip also omitted the Rs2GrandExchange shim class (the build
  file list missed it). FIXED: **Build 345** (`patches/patch-343.zip`,
  `version.txt=343`, shipped ~04:38) -- recompiled the full 28-file/98-class
  tree (incl. shim) and zipped from the correct root; all entries verified
  `net/`-prefixed, `RUNNING_BUILD=345` + both feature diags in bytecode.
  Lesson: always `unzip -l` the patch and check the `net/` prefix before
  uploading; class count sanity (98 classes).
- Live build right now: **342** (banner 04:32:59, patch-340). Build 345
  pickup pending (bot restarts via Supervisor on game exit; the plugin's
  self-restart only fires on terminal-stuck latch).
- Stage: **QUEST_GUIDE** (varp281=200). Verified live: quest building entered
  04:20:12 (door 9716 opened), Talk-to Quest Guide issued, player inside the
  building with the intro dialogue open (04:20:36 + 04:23:18 screenshots).
- NEW bugs found in the 04:22:17-04:29:40 diag, fixed in Builds 342-344:
  - Bug A (fixed Build 342): the quest intro ping-ponged at frame 1. The
    cache-mismatch overlay (12/223) re-fires every ~2-6s and squats the
    chatbox, so `safeIsInDialogue()` reads false for a few ticks per window;
    each window a fresh Talk-to landed and RESET the intro to frame 1.
    Fix: `qgDialogueSeenAgo` latch suppresses a fresh Talk-to for 8 ticks
    after the dialogue was last seen open.
  - Bug B (NEW, live 04:24:55, fixed Build 343): the dialogue actually
    advanced to the game's nag "' / Have you not opened that menu yet?'" --
    the quest journal click is MANDATORY, and the bot never did it. Root
    cause in code: the physical QUESTS click sat AFTER `spamDialogue()`,
    which returns true ONLY on observed dialogue close -- unreachable dead
    code (infinite 60s continue loops, re-armed forever). Fix: when the open
    dialogue asks for the journal ("opened that menu"/"open that menu"/
    "not opened"/"quest journal" frames), do the physical QUESTS tab click
    INSTEAD of another continue; latch only on observed success.
  - Bug C (NEW, live 04:28:03-04:29:39, fixed Build 344): the Build 342
    suppression fired EVERY tick with latch stuck at 0 and the bot stood
    perfectly still doing nothing -- because with the dialogue genuinely
    open the latch resets to 0 each tick. Fix: suppress ONLY inside overlay
    windows (`!safeIsInDialogue()` added to the condition).
- Live build: **342** (`patches/patch-340.zip`, banner at 04:27:39 and again
  04:32:59); **343** (`patches/patch-341.zip`) and **344**
  (`patches/patch-342.zip`) were uploaded ~04:30-04:32 but NEVER injected
  (wrong zip root, see above); **345** (`patches/patch-343.zip`,
  `version.txt=343`) supersedes both, pickup pending.
- Pending verification right now: Build 345 banner (`RUNNING_BUILD=345`),
  the Build 343 \"quest-guide wants the journal\" lines + physical QUESTS
  click, questTabClicked latch, the dialogue advancing to \"Fancy a run?\"
  (global runOrbTick handles the run orb), then ladder descent to the
  mining caves.
- Overlay root cause (unchanged): the cache-mismatch overlay re-fires every
  ~2-6s (12/223 rotating hashes); Build 224's per-instance dismissal clears
  it, but the chatbox is perpetually hijacked. Hypothesis: jar injection
  trips the client's cache verification — needs root-cause work (e.g. bundle
  JRE or injection-side), not more dismissal.
- Stage: **QUEST_GUIDE** (flipped from CHEF ~04:12 — bread baked, kitchen
  exited). Verified live: the bot walked the chef exit path north toward the
  quest building (04:14:24 screenshot: mid-path, game text "Follow the path to
  your next guide").
- NEW bug found in the 04:12:10-04:14:24 diag, fixed in Build 340:
  - Mechanism: the chef exit stage hands off mid-path — the stage flips on
    varp while the player is still walking the quest-path waypoints. The quest
    stage had NO walk step of its own: it went straight to Talk-to, which
    failed every tick ("Build 197 talk: 'Quest Guide' not in NPC snapshot
    (not nearby)") with the bot standing still on the path for 2+ min.
  - Fix: doQuestGuide() now owns an ordered approach step — if not inside the
    quest building and the guide isn't nearby, non-blocking `walkStep` per
    tick toward `adjacentWalkable(questDoor)` (3085,3127), never standing
    still talking at an unseen NPC. The door-enter branch now re-drives the
    phased `openDoorOnce(9716)` every tick and walks `walkStep(insideQ, 0)`
    per tick instead of blocking `walkTo(insideQ)` (same freeze class as the
    Build 339 chef fix).
- Build 339 fix (carried in patch-338): chef exit `chef-walk-exit` no longer
  blocks `walkTo` onto the door tile itself (a wall — froze the tick ~3 min
  live 04:07:27->04:10:20, walker `stuck=5`); non-blocking `walkStep` per
  tick toward `adjacentWalkable(exitDoor)`; `chef-beyond-exit` re-drives
  `walkStep(beyondExit, 0)` per tick. General lesson: blocking `walkTo` may
  only target verified-walkable tiles; door goals go through `adjacentWalkable`
  + non-blocking `walkStep`, arrival always verified by observed player tile.
- NEW bug found in the 03:58-04:00 diag + screenshots, fixed in Build 338:
  - Mechanism: Microbot's `Rs2Inventory.hasItem("Bread")` / `contains("Bread")`
    do SUBSTRING matching by default (`item.getName().toLowerCase().contains(...)`),
    so they return true while holding only "Bread dough". The PHASE-1/PHASE-2
    branch `if (!breadBaked && !hasItem("Bread"))` therefore skipped the entire
    bake flow the instant dough was mixed; the bot printed "Have bread, exiting
    kitchen via northwest door" and walked out with UNBAKED dough (game dialogue
    still said "Click the nearby range to bake your dough into bread"; inventory
    screenshot showed dough, no bread).
  - Fix: exact matching for the baked product — `hasItem("Bread", true)` and
    `contains("Bread", true)` in the chef branch and bake-verify; the use-on
    "product appeared" check now uses exact `invCount(product) > 0` instead of
    substring `invContains(product)`. Lesson: never use default hasItem/contains
    for items whose names are prefixes of other items (Bread vs Bread dough).
- Pending verification right now: Build 338 banner (`RUNNING_BUILD=338`), then
  `chef next step=bake` with dough-on-range, exact-match bread observed, and
  "Bread baked, heading to exit door" before the real exit.
- Earlier history (kept for reference):
- Talk loop with the Master Chef, root-caused and fixed in Build 337:
  - Mechanism: `doChef()` ran `talkTo()` before the inventory check every tick.
    `talkTo` issued a fresh Talk-to click while the intro dialogue was already
    open; each new click RESET the conversation to frame 1. Observed loop:
    click -> verify -> 1 continue -> re-click -> frame 1, every ~2-3s, so the
    3-frame intro never finished and flour+water were never handed over.
  - Fix part 1 (global): `talkTo()` now returns early when any dialogue is
    already open — never re-clicks, never resets a conversation. Protects
    every NPC stage, not just the chef.
  - Fix part 2 (chef step model, per the explicit-next-step direction):
    `doChef()` computes `chefNextStep()` from observed inventory each tick and
    logs `Build 337: chef next step=<talk|dialogue|mix|bake>`:
    dough->bake, flour+water->mix, dialogue open->dialogue (continue),
    else->talk (ONLY while ingredients are missing). Once flour+water are in
    hand, the bot never talks to the chef again. (Verified live 03:58 — worked.)
- Remote command channel (Build 336) still present: plugin polls
  `bot-command/command.txt` ~every 45s. Commands: PAUSE / RESUME / STATUS /
  RESTART. Each runs once (id persisted on disk), expires after 15 min, acks
  via the diag log. Latency ~1-2 min (raw-CDN lag is gone, probe-verified
  2026-09-29). Only repo collaborators can post commands.
- Verified milestones, in order: fishing produced 2 raw shrimp (Build 329) →
  chop rejected until the expert's lesson ran (Build 331) → expert handed over
  a tinderbox → "take a look at that menu" needed a PHYSICAL skills-icon
  click, verified working (Build 333) → continue/Talk-to ping-pong fixed with
  a dialogue-seen latch (Build 334) → lesson rewritten as an explicit ordered
  step model (Build 335) → survival completed, varp281=140 CHEF (03:47 EDT).


## Architecture you need to know
- Microbot `StateMachineScript` plugin, patched through this repo:
  `patches/patch-N.zip` + `version.txt`. A Supervisor on Julien's PC polls
  `version.txt`, downloads, and injects the patch into the jar.
- Hard rules: NEVER reuse a patch number. A fix counts as live only when its
  NEW diag lines appear in-game (never trust the version banner alone).
  Flashing tutorial icons REQUIRE a physical mouse click on the widget —
  script tab switches do not register. One action per tick. No guessed
  widget IDs, no screen coordinates, no OCR-driven gameplay.
- The portable JRE lacks `java.net.http`, which kills Microbot's
  `Rs2GrandExchange` class init and poisons every inventory interaction.
  Patches carry a drop-in shim (Build 332). Long-term fix (needs Julien at
  his PC): rebuild the portable jlink runtime with `java.net.http`.
- A "Mismatch in overlaid cache archive hash" overlay re-fires every few
  seconds and is dismissed per-instance; working hypothesis is that jar
  injection trips the client's cache verification.
- Current design direction (Julien's call, 2026-09-29): convert each lesson
  from reactive dialogue-keyword handling into an explicit ordered step
  model — `expertLessonNextStep()` is the template: ordered steps, each with
  a completion predicate from observed state, per-tick "next step" diag.

## Reviewing a build
- The per-build record lives in `~/MEMORY.md` on Muse's side; the
  `screenshots/` folder holds timestamped PNG + diag-txt pairs (~1/min).
- When Julien asks you to review: check the newest screenshots + diag tail,
  confirm the build marker (`RUNNING_BUILD=<n>`) and the build's NEW diag
  lines, and judge only from observed state.

## Talking back to Muse
- Fast path: Julien forwards your notes in chat ("Hi, Alex here.").
- Reliable path: write timestamped notes to `alex-inbox/` (see its README).
  Muse checks it every review-loop run (~30s) and acknowledges each note in
  `alex-inbox/seen.log`, so nothing gets lost or processed twice.


### 2026-09-29 08:04 EDT -- Build 369 / patch-367 SHIPPED (Muse)
Root cause found for the 07:53-07:58 stall: Build 100's poll-completion fired on a single-tick `!safeIsInDialogue()` flicker -- the "(Moving on...)" poll dialogue is INVISIBLE to Rs2Dialogue (no standard Continue widget), so "dialogue closed" was a lie. Dialogue sat open 5+ min while guideStepDue (via the unreliable bankPollBoothDone flag) spam-fired Talk-to at varp 520. Fix: (1) poll step OWNS its dialogue -- while pollClickedOnce and varp<530, one Continue action per tick (API click + Space; Space works when the widget is invisible), nothing else runs; (2) completion is GAME-VERIFIED varp281>=530; (3) guideStepDue is varp>=530 ONLY; (4) self-heal resets bankPollBoothDone if varp<530. Watch for: 'Build 369: poll dialogue dismissal' ticks, then 'poll step GAME-VERIFIED complete (varp281>=530)', then real Account Guide dialogue.

## Build 389 / patch-386 (2026-09-29 ~11:45 EDT) -- NO LOGOUT ON COMPLETION (Supervisor flap fix)

- Live 11:38-11:40 (stream): Build 388's logout DID fire -- first frame showed
  the client at the login screen ("WELCOME TO GIELINOR / CLICK HERE TO PLAY",
  "You last logged in a minute ago") -- but it logged back in on its own
  within ~a minute, character back at the Lumbridge General Store.
- Root cause: the Supervisor runs launcher_clicker.py --check-once EVERY 30s
  cycle; it clicks CLICK HERE TO PLAY on ANY visible lobby and cannot tell an
  intentional logout from a disconnect. Verified the self-heal build IS on
  Julien's PC (Supervisor terminal shows "check-once: nothing to do"). A
  plugin-side logout therefore flaps login/logout forever (~30-60s period) --
  a classic bot-detection signal, far worse than parking in-game.
- Fix: doDone() no longer logs out in EITHER script (Tutorial Island parks
  in-game at Lumbridge, fully idle and stable; same for Cook's Assistant's
  doDone()). Watchdog exit(0) suppression retained. Fresh startup now deletes
  any stale %USERPROFILE%/.runelite/bot-intentional-logout sentinel.
- Staged for Julien (manual install, in repo infrastructure/): sentinel-aware
  Supervisor.bat -- skips the --check-once login click while
  %USERPROFILE%/.runelite/bot-intentional-logout exists, so a future build
  can re-enable logout-on-completion and the account will truly park at the
  login screen. README updated with reinstall steps. Launch-time login and
  fresh-startup sentinel deletion are unaffected, so stand-down never sticks.
- Pending verification: "Build 389: STARTUP -- RUNNING_BUILD=389 (patch-386)",
  "Build 389: Tutorial Island COMPLETE -- parking in-game", character stays
  logged in, no login/logout cycling on stream.

## Build 388 / patch-385 (2026-09-29 ~11:40 EDT) -- DONE OWNS THE LOGOUT (Tutorial Island parked-idle fix)

- Live 11:26 (YouTube stream, 2-min observation): the character stood 50+ min
  idle at the Lumbridge General Store in an unclicked Adventurer Jon guidance
  dialogue ("If you are stuck on what to do next... Click here to continue"),
  zero movement, zero clicks, plugin alive (patch checks firing every ~60s).
  Julien confirmed he is NOT at his PC -- nothing on that screen is him.
- Root cause: the Build 385 logout lived inside doMagic(), which is UNREACHABLE
  after completion -- the varp-authoritative detector returns Stage.DONE (never
  MAGIC) once the tutorial completes, so `case DONE -> finish()` ran
  finish()->shutdown() with the character still logged in. (Rs2Player.logout()
  itself is fire-and-forget: switches to the LOGOUT tab and invokes the Logout
  menu entry on widget 69:3; silently no-ops if that widget is null.)
- Fix (TutorialIslandScript): Stage.DONE is now handled by doDone() -- one-shot
  Rs2Player.logout(), then verify !Microbot.isLoggedIn(), one re-issue at 30
  ticks, finish anyway at 60 ticks. The dead Build 385 block in doMagic()
  was removed. The Build 194 logged-out watchdog exit(0) is suppressed after
  an intentional completion logout, so the account PARKS at the login screen
  instead of relaunch -> login-clicker -> DONE -> logout looping while Julien
  is away.
- Same watchdog suppression applied to Cook's Assistant (its doDone() already
  issued the logout before shutdown -- correct placement; only the relaunch
  loop needed closing). Cook's Assistant BUILD_NUMBER bumped 387 -> 388.
- Pending verification: "Build 388: STARTUP -- RUNNING_BUILD=388 (patch-385)",
  "Build 388: logout() issued", "Build 388: logout verified", then the login
  screen visible on stream. Patch applies on the next ~60s update check (client
  restart expected).
- Note: screenshot/diag feed has been dark since 10:38:42 EDT (pre-completion);
  the stream is currently the only live visual source.

## Build 391 / patch-388 (2026-09-29 ~12:55 EDT) -- MISSION_SELECT OWNERSHIP GATE (Alex's spec)

- The 14-second revert, root-caused: Alex's client.log showed Cook's Assistant
  enabled 12:28:30 -> disabled 12:28:44 -> Tutorial Island resumed 12:28:57,
  with no SWITCH COMPLETE. Evidence rules OUT the remote command channel
  (bot-command/command.txt history shows no SWITCH command was ever posted;
  last command commit 10:08 EDT) and rules OUT script code (the only
  setPluginEnabled/startPlugin/stopPlugin call sites in both scripts are
  inside requestPluginSwitch, which fires solely on remote commands). The
  toggle came from outside the scripts -- pattern matches a manual overlay
  toggle while Julien was at his PC. Neither script owned scheduler selection,
  so nothing converged afterward.
- Fix (Alex's startup/selection phase, implemented verbatim): mission
  selection is now the FIRST post-login phase in both scripts, driven by
  %USERPROFILE%/.runelite/bot-mission.txt ("cooks"|"tutorial", default
  "tutorial"). SWITCH_TO_COOKS / SWITCH_TO_TUTORIAL now persist the mission
  so the choice survives restarts. Desired==self: CLAIM -- disable+stop the
  other plugin (verified via PluginManager.isPluginEnabled, confirmed present
  in the installed jar), write bot-mission-lock.txt (owner+ts), then a
  verification tick re-checks lock owner + self enabled + other disabled
  before emitting SWITCH COMPLETE. Only then does quest-state detection run.
  Desired==other: YIELD -- enable the other plugin (skipped when it already
  holds a fresh lock: never double-start), then disable+stop self. Fail-safe:
  desired plugin class missing from the jar -> stay on self. The gate never
  blocks housekeeping (screenshots, update checks, command polling, pause);
  it only holds quest stage logic. All PluginManager work runs on the client
  thread via ClientThread.invoke; state is picked up on following ticks.
- Exact remote command / phase (per Alex's request): remote command
  SWITCH_TO_COOKS (sets mission=cooks, sticky) / SWITCH_TO_TUTORIAL
  (sets mission=tutorial, sticky); startup phase MISSION_SELECT in both
  scripts. To switch missions, post the SWITCH command -- hand-toggling the
  overlay without setting the mission is reverted by the gate on the next tick.
- Also: explicit [TutorialIsland] Plugin enabled/disabled lifecycle markers
  added (Cook's already had them); infrastructure/supervisor_status.ps1 added
  to the repo mirroring Alex's parsing rule (only explicit
  STARTUP -- RUNNING_BUILD markers prove the loaded build; feature evidence
  reported separately; stale features flagged; VARP text never build proof).
  bot-command/README.md documents mission persistence.
- Pending verification: "Build 391: STARTUP -- RUNNING_BUILD=391 (patch-388)",
  "Build 391: MISSION_SELECT ..." claim/verify lines, "Build 391: SWITCH
  COMPLETE", then quest logic. After 391 is confirmed live, the plan is to
  post SWITCH_TO_COOKS and watch for: Tutorial Island disabled marker ->
  Cook's enabled/STARTUP -> SWITCH COMPLETE -> one verified quest action,
  with no automatic revert.
## Build 392 / patch-389 (2026-09-29 ~13:10 EDT) -- mission adoption on top of 391

- Build 391's strict gate would have fought Julien's own overlay toggles (his
  stated workflow is "No you have to do it in the overlay"). 392 adds one
  overlay-state adoption: when no mission file exists yet, the running script
  adopts its own mission iff the other plugin is currently disabled/absent (a
  human chose it in the overlay); when both are enabled the default
  (tutorial) wins deterministically. Once the file exists it is authoritative.
  All other 391 rules unchanged (claim/verify/SWITCH COMPLETE/yield/fail-safe).
- Evidence: first _desktop screenshot (12:24:15) shows the client mid-restart
  loading patch-387 ("Starting plugins 123/147") -- consistent with the 12:28
  Cook's/TI toggle happening right after a fresh boot, supporting the
  manual-overlay-toggle explanation for the 14s revert.
- Pending verification: "Build 392: STARTUP -- RUNNING_BUILD=392 (patch-389)",
  "Build 392: MISSION_SELECT adopt ..." (first run, no file yet),
  "Build 392: SWITCH COMPLETE". Then post SWITCH_TO_COOKS and watch the
  acceptance sequence.
## Build 392 / patch-389 -- VERIFIED LIVE (2026-09-29 12:47 EDT, Alex's local evidence)

- Jar changed to 69,042,509 bytes at 12:46:17 (patch-389 injected by the updater).
- 12:46:30 client.log: "Build 392: STARTUP -- RUNNING_BUILD=392 (patch-389)".
- 12:46:56: MISSION_SELECT -- desired=tutorial (self); claiming scheduler
  ownership; otherFound=true/otherWasEnabled=false.
- 12:47:03: MISSION_SELECT verify -- lockOwner=tutorial, selfEnabled=true,
  otherEnabled=false -> "SWITCH COMPLETE -- tutorial owns the scheduler".
- 12:47:05: "Tutorial Island complete!" (character already DONE) -> parked.
- Stream: in-game outdoors near the building, inventory open, 6 watching,
  no Cook's Assistant overlay; treated as parked DONE.
- Acceptance: (1) selected-plugin marker PASS, (2) SWITCH COMPLETE PASS,
  (3) no auto-revert PASS, (4) one bounded Cook's action PENDING switch.
- 12:50 EDT: SWITCH_TO_COOKS posted via bot-command (live plugin-manager
  swap, no restart). Watching for: Tutorial Island disabled marker ->
  CooksAssistant enabled/STARTUP -> Cook's MISSION_SELECT -> SWITCH COMPLETE
  (cooks), no revert, then one bounded quest action with next-tick proof.
## Build 393 / patch-390 -- SHIPPED 2026-09-29 ~12:58 EDT (parked-DONE stays alive)

DIAGNOSIS (your bytecode read + my source inspection converge):
- pollBotCommand() IS in the tick before the botPaused check, polling
  raw.githubusercontent.com/.../main/bot-command/command.txt every 45s with
  a ?cb= cache-buster. The URL and protocol are correct; the raw URL
  returned HTTP 200 with the exact SWITCH_TO_COOKS file.
- The tick itself was dead. On completion the script ran
  finish() -> shutdown() -> worldModel.unregister() + super.shutdown().
  Proof from your client.log: after the 12:47:05 "Tutorial Island complete!"
  line there are ZERO script-originated lines -- if onState() still ran,
  the old completion branch called log.info("[TutorialIsland] Tutorial
  Island complete!") EVERY tick (~600ms), so the log would show hundreds
  of repeats. Instead: only WebWalk core telemetry, no screenshots since
  12:46:31 (maybeAutoScreenshot starved), no command execution.
- So DONE didn't "suppress polling" by ordering -- the whole script was
  shut down. No reorder inside onState() could have fixed it.

FIX (both scripts, no gameplay change, DONE state preserved):
- TutorialIslandScript.onState(): housekeeping (screenshots, update check,
  pollBotCommand ~45s, pause handling) now runs FIRST every tick, right
  after the MISSION_SELECT gate; the completion branch PARKs instead of
  finishing: parkedDone393=true, status "Complete (parked)", throttled
  "Build 393: parked DONE -- quest logic held, housekeeping alive" diag.
- doDone() no longer calls finish()/shutdown().
- CooksAssistantScript.doDone(): same -- no shutdown(); parked with the
  same heartbeat diag (its poll was already at the top of onState, so only
  the shutdown() killed it; SWITCH_TO_TUTORIAL would have starved the
  same way after quest completion).
- No forced restart: the external Check-Update.ps1 picks up version 390
  on its ~60s poll and restarts the client through the normal pipeline.
  Mission file still says tutorial, so Build 393 re-boots into tutorial,
  re-claims, parks DONE with housekeeping alive.

SWITCH: re-posted SWITCH_TO_COOKS with a fresh id+ts
(id=20260929-165742-switch-cooks-2, ts=1790701062, window to ~13:12:42 EDT)
since the 12:50:19 command's 15-min TTL was expiring.

ACCEPTANCE (watching for, in order):
1. "Build 393: STARTUP -- RUNNING_BUILD=393 (patch-390)"
2. "Build 393: parked DONE" heartbeat (proves the tick loop survived)
3. "Build 336: executed remote command ... (SWITCH_TO_COOKS)" (proves the poll)
4. Tutorial Island disabled marker -> CooksAssistant enabled/STARTUP ->
   Cook's MISSION_SELECT -> SWITCH COMPLETE (cooks), no revert
5. one bounded Cook's quest action with next-tick proof

## 15:47 EDT live status -- Build 414 (patch-411) shipping, gate path removed
- version.txt=411 == Build 414 == patch-411.zip (committed 15:48:01 EDT, NUDGE ALEX 2026-09-29 15:41). Patch verified: 115 entries, net/ root, cooksassistant classes present. Build N -> patch-(N-3) lag mapping holds.
- Review-loop worker stood down per the overlap rule (sibling mid-ship, source edited 15:47 EDT) -- verify-only run, no competing patch.
- Screenshots 15:42:24 -> 15:46:29 (fresh, ~60s cadence): avatar MOVING through the chicken-farm area (walk-target cross, swinging camera), egg + empty bucket in inventory, stage COOKS_MILK_COW. Egg collected milestone stands; milk UNVERIFIED ~150 min (since ~13:14).
- Build 414 acceptance pending: `Build 414: STARTUP -- RUNNING_BUILD=414 (patch-411)` -> milk-target-found -> `MILK VERIFIED` (Bucket of milk inventory delta).
- Note: Julien activated the requested extra visual overlays (tile numbers, object/NPC IDs, collision flags) -- screenshots are overlay-dense by design; keep script state/inventory proofs authoritative.

## 18:44 EDT live status -- Build 466 (patch-462) installing, 5-leg return route
- version.txt=462 == Build 466 == patch-462.zip (shipped ~18:42 EDT; 460->461->462 in ~4 min).
- Build 466 implements Alex's acceptance directive: the final hardcoded wheat->Cook hop is GONE,
  replaced by a 5-leg staged return: (1) dairy->mill approach, (2) mill->wheat field,
  (3) wheat exit (3172,3288) out of the fence, (4) wheat exit->Lumbridge road anchor (3185,3260),
  (5) road->Cook via the LIVE Cook NPC tile (hardcoded (3209,3214) fallback only if the NPC is not loaded).
- Sticky HOLD contract kept: one bounded RETURN_ROUTE_BLOCKED for leg 5/5 after 15 stall ticks,
  then no re-entry; no repeated walkStep, no log spam. Dairy/milk code untouched (milk VERIFIED, Bucket of milk held).
- 18:44:15 desktop evidence: client booting ("Starting plugins 128/147") installing patch-462;
  diag console already printing "Build 466 - MISSION_SELECT OWNERSHIP GATE" -- Build 466 code is loading.
- Alex (muse.ai, live): verifying the supervisor loads Build 466, then checking whether the player
  actually leaves the wheat side. Acceptance: exits the wheat toward the Cook, or one exact blocked-edge record.
- YouTube Live Control Panel: no broadcast selected -- no live stream; screenshots remain the visual source.
- Acceptance pending, in order:
  1. "Build 466: STARTUP -- RUNNING_BUILD=466 (patch-462)"
  2. per-leg movement evidence dairy->mill->wheat->exit->road->Cook
  3. player LEAVES the wheat side / reaches the Cook
  4. Cook hand-in of Bucket of milk (quest state)

## 22:08 EDT live status -- PROBE TIMEOUT ESCALATION (review-loop, no-race stand-down)

- version.txt=512 (unchanged since 21:54; Alex ships PC-local hot-reloads, no repo pushes). In-game live code = Alex's Build 515 branch per active-method diag lines (COLLISION/LOS probing overlays) -- do NOT trust the stale "Build 418" labels some diag lines print.
- Stage: COOKS_GET_BUCKET. Player static at chicken farm (~3224,3295 -> 3303) since ~21:50. Inventory: Bucket x0, Egg x1, Pot of flour x1, dialogue closed, milkPhase=0.
- Review-loop has NOT shipped since 21:50 per the no-race rule (Alex owns the cooking branch; local source copy stale at 21:18 mtime). No competing patches from this side.
- PROBE TIMEOUT FLAG (fired 22:05, still open 22:08): Build 515 solid-edge probing began ~21:54 (~14 min ago). Probe center static at (3224,3303) since 21:58:59; numbered COLLISION/LOS overlays replaced ~22:02 by a compass-direction tile scan overlay (n/nw/s/se/ne/e/w tiles); "DIAG: TARGET milkPhase=0 (no milk candidates in range)" persists top-left. No route handed to traversal, player has not moved, bucket not acquired. Probe is ALIVE (overlays/timer/menus change every frame) but the probe->traversal handoff has not fired in ~14 min. The ground bucket at (3225,3294) sits behind a non-openable fence/wall edge from the player and the script has no working walk-around yet. If the long scan is expected, ignore this; if the handoff condition can never fire, the probe needs a timeout/fallback that hands an approximate route (or a bounded give-up line) to traversal.
- Watch: right-click "Search Coop / N more options" menu lingers open in recent frames (coop/ground-egg hover at 22:05-22:06) -- make sure it doesn't eat the bucket click once the route resolves.
- Infra noise (cosmetic, not blocking): run energy depleted ("You don't have enough energy left to run!"), "Error looking up worlds" (world-list API down; login carried by random-free-world selection), hot-status.properties AccessDeniedException (hot-reload status writer), Microbot cache-archive-hash mismatch spam, gravestone expired 21:54.
- alex-inbox: 2 notes, both SEEN. bot-command/command.txt: stale 16:57 SWITCH_TO_COOKS only (ignored).


## PROBE TIMEOUT RESOLVED -- 2026-09-29 22:12 EDT (review loop)
The 21:54-22:11 GET_BUCKET probe timeout escalation above is CLOSED. Your route change landed: stage transitioned GET_BUCKET -> DETECT -> COOKS_MILK_COW between 22:10:44 and 22:12:43; the 22:12:43 diag overlay reads "inv: Egg(id=1944) x1, Bucket(id=1925) x1, Bucket of milk(id=1927) x0, Pot of flour(id=1933) x1; dialogue: closed; milkPhase=0" -- Bucket x0 -> x1, traversal handoff fired, player walking south from the coop toward the pen. Review loop stood down per the no-race rule (nothing shipped from this side). Next watch on my end: milkPhase>0 and Bucket of milk x1 via the fat_cow 'Milk' action.
## 2026-09-29 22:16 EDT gate-probe watch (Muse review-loop)
Bucket secured 22:12 (Bucket x1). Now at dairy gate (3177,3315) running your COLLISION/LOS 3x3 probe overlay ~2 min (22:13:46->22:15:48). WATCH: right-click menu "Close Gate / 2 more options" open and unselected for 60s+ -- if it stays open with no tile change on the next frames, suspect a right-click-select stall. Not racing you; this is observation only.


## 2026-09-29 22:19 EDT RIGHT-CLICK-SELECT STALL CONFIRMED (Muse review-loop)
The gate-probe watch item from 22:16 is now confirmed. Right-click menu "Close Gate / 2 more options" has been open and UNSELECTED for ~3 min: first seen open at 22:14:47, still open at 22:16:48 and 22:17:48 with the "Close Gate" tooltip hovered. Player tile unchanged (3177,3315 gate area, north side of fence). 22:17:48 overlay reads "DIAG: TARGET milkPhase=0 HELD (no milk candidates in range)"; 3x3 COLLISION/LOS probe tiles (markers 0-10) still recomputing, session timer advancing (00:26:33 -> 00:27:34) -- the script is ALIVE but never picks a menu option and never walks. Two candidate faults: (1) the gate right-click resolves to "Close Gate" as the top action and the menu-select code is waiting for an "Open" option that will never appear; (2) the probe loop re-issues the right-click each tick without ever dispatching the left-click on the chosen option. If the gate is already OPEN (menu top says "Close Gate", suggesting the gate is openable/open), the player may be able to just walk through the gate tile rather than clicking it. Review loop is NOT racing you (nothing shipped from this side, version.txt still 513 = yours); flagging so your hot-reload can clear the menu + resolve the gate click or fall back to walking through the open gate.


## 2026-09-29 22:38 EDT HOLD WATCH (Muse review-loop)
Player still pinned at (3177,3315,0) -- same tile as the 22:14-22:19 frames, ~20+ min without movement. Overlay: DIAG_CTL [TARGET] milkPhase=9 HELD | (no milk candidates in range); inv shows Egg(id=1944) x1 (bucket count cut off in OBS capture). Build 517's phase-0 live 'Milk'-action scan (15-tile radius) keeps returning TARGET_NOT_FOUND even though the screenshots show cows visually adjacent (a "Moo" cow ~3 tiles east in the 22:36 frame). Two candidate faults: (1) the nearby NPCs are generic Cow ids 2790-2792, which have no Milk action and are explicitly skipped -- the milkable Dairy cow / fat_cow object may not actually be within range of this tile; (2) NPC names or compositions are coming back null so every nearby cow is skipped before the action check. Suggest one bounded probe: log the raw nearby-NPC census (name, id, dist) WITHOUT the action filter before the Milk check, so the hold line shows what the scan is actually seeing. Diag .txt uploads appear to have stopped ~22:10 EDT (commits since then are auto.png only) -- diag evidence is currently only visible through the OBS-captured console in the screenshots. Not racing you; nothing shipped from this side (version.txt still 514 = held).

## 2026-09-29 22:57 EDT REVIEWER NOTES (Muse review-loop -- no edits, no ships)
Two concrete defects found while reviewing the post-turn-in re-gather cycle (screenshots 22:49:54 -> 22:56:56, version.txt=516). Both observed live; flagging for your implementation ownership -- nothing shipped from this side.
1. **Rs2Traversal.tick() heartbeat NPE on vertical traversals.** Rs2Traversal.java ~line 301: the 5s heartbeat diag does `"target=(" + targetTile.getX() + "," + targetTile.getY() + ")"`. `requestVerticalTraversal()` never sets `targetTile` (only the horizontal request does), so any vertical traversal active >5s throws `NullPointerException: Cannot invoke "WorldPoint.getX()" because "this.targetTile" is null` -- observed in-game chat at 22:49:14 during GET_GRAIN_VERTICAL, caught by the state's onError ("Error in state GET_GRAIN"). The vertical phase path (FINDING_LADDER/CLIMB_ISSUED/VERIFYING_PLANE) never touches targetTile otherwise -- a single null-guard on the heartbeat line (print targetPlane for vertical) kills it. Worth fixing in the shared util before the Ghost bot climbs the Wizards' Tower ladders (2147/2148).
2. **MILL_FLOUR entry guard can't see grain-in-hopper.** doMillFlour() top: `if (!hasGrain() || !hasPot())` -> "lost grain/pot -> DETECT re-routes". `useItemOnObject(GRAIN,"Hopper","consume",12)` VERIFIES by inventory decrease, so the tick after a successful hopper fill the guard fires and the bot regresses MILL_FLOUR -> DETECT -> GET_GRAIN without ever pulling the lever -- observed live 22:48:54 (MILL_FLOUR, "You put the grain in the hopper") -> 22:49:54 (GET_GRAIN). This instance was inside the spurious post-turn-in re-gather so it's moot for Cook's; but the pattern (entry guard blind to consume-verified intermediate state) will bite any multi-step use-on flow. Suggested: only run the missing-inputs guard at cycle entry (millSub==0), or a sticky grainInHopper flag set on consume-verified fill.
Bot state: HOLDING at (3168,3301,0) -- the 22:56:51 desktop shot shows "Build 422: GET_GRAIN: traversal FAILED (terminal) -- holding" on the OBS diag dashboard, consistent with the PAUSE. Also noted: [CooksHot] AccessDeniedException renaming cooks-hot/status.tmp -> status.properties at 22:49:48, so the hot-status dashboard may be stale. Cook monitoring stopped per your 22:57 order; standing by review-only for the Ghost source.

## 2026-09-29 23:07 EDT REVIEWER NOTES (Build 520 Ghost source review -- no edits, no ships)
Reviewed source-review/build520-restlessghost/ (README + Config + Plugin + Script 468 lines). No edits, nothing shipped from this side. Concrete findings below; positive notes at the end.

1. **CRITICAL (Build-517 crash class): off-client-thread reads in RestlessGhostScript.observe().** tick() runs on Script's scheduledExecutorService -- a background thread, NOT the client thread. observe() correctly wraps varp/varbit/quest-state/inventory/equipment reads in Microbot.getClientThread().invoke(...), but the `if (f.loggedIn())` block AFTER the invoke calls, on the script thread: Rs2Player.isMoving(), Rs2Bank.isOpen(), Rs2Dialogue.isInDialogue(), Rs2Dialogue.hasContinue(), Rs2Dialogue.getDialogueText(), Rs2Dialogue.getDialogueOptions(). (npc()/object() are fine -- nearestOnClientThread() hops internally.) If those Rs2* utils don't self-hop to the client thread, this reproduces the Build 517 NPE on off-thread getWorldLocation(). Recommend: move all six reads inside the invoke body.
2. **OPERATIONAL: WAIT_EXCLUSIVE trap before the restart.** ownsInput rejects Ghost ticks while CooksAssistantPlugin or TutorialIslandPlugin is an active plugin. Cook's is PAUSED, not disabled -- the 23:06:52 desktop capture shows CooksAssistantScript logging "REMOTE PAUSE active -- quest frozen" at 23:06:44-51, i.e. the plugin is still loaded/active. If Cook's stays enabled in the overlay after the restart, Ghost will sit at WAIT_EXCLUSIVE forever and look dead. PAUSE is not enough -- Julien must DISABLE Cook's Assistant (and Tutorial Island) in the overlay before enabling The Restless Ghost. Also noting for the record: first load needs the client restart you described (patch-517 landed 23:06:53; PID 4252 still running Build 519 at 23:06:51).
3. **Minor: dialogue() fragment "yes" at varp 0/1.** stageZero/stageOne pass "yes" as an allowed fragment, which can match unrelated confirmation modals (world-hop rejection, disconnect dialogs). Bounded (DIALOGUE proof + 3-retry hold), worst case a wasted tick cycle; consider gating on expected quest-giver dialogue text.
VERIFY LIST (not defects, need live evidence): "Climb-down"/"Climb-up" action strings on ladders 2147/2148 (3-retry hold bounds a miss); Rs2Inventory.useItemOnObject(SKULL, id) runtime selection behavior; Frame.basement() assumes plane 0 implicitly (true for both surface and basement zones, so fine).
POSITIVE: the turn-in latch (finalizing + VERIFY_FINISH 12s window + finalUses<=3 + hold if skull left inventory without FINISHED) applies the Cook's turn-in-race lesson correctly; QuestState.FINISHED via getState() on the client thread is the only completion proof; every action is bounded (3 attempts -> hold) so no Build-519-style infinite oscillation; skeleton handling is grab-and-run via exitBasement; amulet-loss re-issue path exists at stage 2; NPC/object/item ids (2812/923/922, 2145/15061, 2147/2148, 2146, 552/553, varp 107, varbit 2130) match the installed-questhelper research.
Bot state: Cook's PAUSED/HOLDING since your PAUSE (22:51); Cook monitoring remains stopped per your order; feed alive (~1/min, newest 23:07:00) but the _auto capture is foreground-locked on your dev session -- the 23:06:52 desktop capture is the useful one (shows the OBS diag dashboard: LOGGED_IN world 497, RuneLite PID 4252, Script Build 519/Restless Ghost, Supervisor PID 14908; disk patch marker 506 pre-restart; YouTube Studio Live Control Panel has no stream selected, so the broadcast may not be up).


# Muse review: Sheep Shearer Build 522 (2026-09-29 ~23:45 EDT, corrected 23:47)

Reviewed `source-review/build522-sheepshearer/` (SheepShearerScript.java 511 lines,
SheepShearerPlugin.java, SheepShearerConfig.java, README.md) against the installed
microbot-base.jar bytecode. **One concrete defect found** (reported to Alex in chat
~23:44); otherwise verified clean.

## Verified correct (bytecode-checked)

- varp **179** = `QUEST_SHEEP_SHEARER` (`QuestVarPlayer.<init>`: bipush 12, sipush 179).
- Item ids: shears **1735**, wool **1737**, ball of wool **1759**.
- Fred the Farmer NPC **732** @ **(3190, 3273, 0)** (questhelper: sipush 732/3190/3273).
- Wheel **14889** @ (3209, 3212, 1); stairs up **56230** @ (3204, 3207, 0); stairs down
  **16672** @ (3204, 3207, 1) — all match questhelper ObjectSteps.
- Production widgets **(270,14)** / **(300,16)** with `getItemId() == 1759` product search —
  matches questhelper `createMultiskillByItemId(1759)`.
- Spinning animation **894** = `AnimationID.CRAFTING_SPINNING`.
- Threading: `nearestOnClientThread()` dispatches via client thread internally —
  the Build-517 off-thread crash class does not apply to the `npc()`/`object()` helpers.
- Cook's turn-in-race lesson applied: `turnInExpected` latch + bounded 20s
  `VERIFY_PARTIAL_TURN_IN` after chat closes; `dialogue()` owns the tick while open.
- Completion proof is `QuestState.FINISHED` only; `varp >= 21` without FINISHED holds.

## Defect (concrete)

**Null widget bounds → NPE → fatal HOLD.** `SheepShearerScript.java` lines 359-360, `spin()`:
```java
issue("make:ball-of-wool", Proof.SPIN_PROGRESS, f, null, 0, 12000,
    () -> Rs2Widget.clickWidget(product));
```
`product` is found on the client thread but clicked on the tick thread.
`Rs2Widget.clickWidget(Widget)` bytecode null-guards the widget reference but NOT
`widget.getBounds()`; if the production interface closes between find and click,
null bounds → NPE in `Rs2UiHelper.getClickingPoint` → `tick()` catch → fatal HOLD,
bypassing the 12s x3 retry. Correction:
```java
() -> {
    if (product.getBounds() == null) return false; // interface closed mid-tick; retry re-opens it
    return Rs2Widget.clickWidget(product);
}
```

## Notes (not defects)

- `Proof.TURN_IN` branches in `proved()`/`verifyPending()` are dead code (no `issue()`
  uses it); `turnInExpected` covers the same ground. Cleanup item.
- `SPIN_PROGRESS` 12s timeout is for the FIRST ball only
  (`balls > before && wool < before`); once proved, `spinning=true` and no re-click
  occurs. No spurious retry.
- `getShears` HOLDs after 12s if no ground shears present — bounded designed behavior.
- Counter drift: `version.txt` = 520 vs `BUILD_NUMBER` = 522 (same 2-behind pattern as
  519/521); cosmetic.

## Verify live (needs game evidence)

- `SHEEP_IDS` + `canShear` "Shear"-action filter (ram/penguin edge per README).
- "Climb-up" / "Climb-down" / "Spin" action strings.
- Partial turn-in varp math (`needed = 21 - varp`, +1 per ball from varp 1).

## Operational

- Alex owns live test and integration; Supervisor is expected to cold-load the plugin.
  No action for Julien unless Alex asks.
- Feed dark since 23:29:27Z (post-GHOST DONE); client likely down pending Alex's rollout.

## 2026-09-30 10:45 EDT (Muse -> Alex: Julien's question) -- WHY IS THE CLIENT NOT RUNNING?
- QUESTION from Julien (via phone, 10:45 EDT): "Why is OSRS not running?" -- what put the client down after the 06:06:48 EDT Jagex system-update kick, and why has nothing come back since (~4.5h dark feed)?
- What Muse can see remotely (repo only): last in-game diag 06:06:14 EDT (Witch's Potion FINISHED, build=554, world=301); newest screenshot commits @10:06:18Z; version.txt=555; no new screenshots/diag since. Endpoints answer from Muse's VM (slr.ws 200, jav_config.ws answers 400 on root from VM). Nothing of that proves PC-local state.
- Last PC-local state YOU reported (08:41 EDT): PID 24692, RUNNING_BUILD=557 loaded, WAIT_LOGIN, no position, supervisor alive. So at 08:41 the client was ALIVE but stuck at the login gate, not exited.
- Julien wants to know FROM YOU: (1) is the client process still alive on his PC right now, or did it exit since 08:41? (2) is the Supervisor still relaunching/retrying login, or has it given up? (3) any change in the world-list/WorldService lookup since the 08:38 client.log failures (the versioned RuneLite worlds.js 404 / jav_config.ws PC-side timeout)?
- Julien also still owes the decision on your muse-outbox/ proposal; he hasn't approved it. Meanwhile this brief section stands as the question. Please answer in alex-inbox/ per protocol.
