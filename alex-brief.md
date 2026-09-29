## Build 372 / patch-370 (2026-09-29 ~08:32 EDT) -- poll box-center click (hidden-guard bypass)
- Root cause, PROVEN by zoomed screenshots (08:20:30, 08:15:51): the
  "(Moving on...)" box has NO rendered "Click here to continue" link --
  Build 371's premise was wrong. The 229:4 link widget exists in the
  interface but is genuinely hidden (not rendered). The title at 263:1 IS
  rendered (visible blue text) yet reports hidden=true -- a FALSE reading
  (Build 224 precedent: widgets reporting hidden=true while visibly
  rendered), which the Build 276 guard in physicalClickWidget() turned into
  a click refusal. Build 370's scan never fired either (findMovingOnBounds
  bails on w.isHidden() at the root). NET: zero physical clicks were ever
  issued on this box in 20+ min; Space (130+ presses) does nothing.
- Fix: each dismissal tick physical-clicks the BOX CENTER -- title widget's
  parent-container bounds (fallback: title bounds) -- via
  physicalClickWidgetBypassHidden() (hidden guard bypassed ONLY for this
  proven-visible target; bounds sanity kept), one click/tick, verified by
  observed box-gone next tick. Space is the last resort when no target is
  found. The poll step still owns the tick until varp281>=530.
- Verify: "Build 372: physically clicking (Moving on) box center ... at X,Y
  (hidden-guard BYPASSED ...)" lines, then box observed gone and varp 281
  -> 530, then the Account Guide talk.

## Build 371 / patch-369 (2026-09-29 ~08:19 EDT) -- poll continue-link targeting
- Root cause, PROVEN live (Build 370, 08:12-08:18): the Build 370 escalation
  to a PHYSICAL click never fired -- the custom getWidgetRoots() title-text
  scan produced ZERO "physically clicking" lines across 100+ dismissal ticks,
  so the escalation issued no clicks at all. The "(Moving on...)" dialogue
  stayed open ~12 min, varp stuck at 525, tick log looping "poll dialogue
  dismissal tick N (varp=525, physical=true)".
- Fix: target the dialogue's REAL control -- its "Click here to continue"
  link -- using the proven Rs2Widget.findWidget("Click here to continue") +
  physicalClickWidget() pattern (world-error modal Builds 207/213, banking
  info box Build 360). Title-text lookup ("Moving on") is the fallback;
  Space is the last resort. No 30-tick warmup (Build 370 already waited).
  The poll step still owns the tick until varp281>=530 game-verifies
  completion. Scan outcome is logged every 10 ticks ("continue-link widget
  not found -- Space fallback").
- Verify: "physically clicking 'poll (Moving on) continue link' at X,Y"
  lines, then the box observed gone and varp 281 -> 530, then the Account
  Guide talk.
- NOTE: patch uploads can 409-conflict with the screenshot uploader's
  commits (the uploader itself sees 409/422s) -- retry the PUT a few times.

## Build 369 / patch-367 (2026-09-29 ~08:03 EDT) -- poll dialogue ownership + varp-driven completion
- Root cause, PROVEN live (Build 368, 07:53-08:03): the poll-booth click at
  07:53:47 opened the "(Moving on)" closing dialogue, but Build 100's
  single-tick !safeIsInDialogue() check falsely marked the step done at
  07:53:56 -- the "Moving on" box is INVISIBLE to Rs2Dialogue
  (isInDialogue()=false, hasContinue()=false; same modal-info-box class as
  Builds 352/353/355/360/362, new title). The box sat open ~10 min while
  step-5 spam-fired Talk-to on the Account Guide (dist frozen 9-11, player
  static in the bank south room) and varp stayed 520. Screenshots 07:54:33
  vs 07:55:33 are 99.6% pixel-identical -- a hard soft-lock.
- Fix: the poll step OWNS its dialogue transaction. Completion is
  GAME-VERIFIED (varp281 >= 530), never a dialogue flicker. While
  pollClickedOnce and varp < 530: one Continue action per tick
  (clickContinueOnce = API click + Space; Space advances even when the
  Continue widget is API-invisible), NOTHING else runs. Self-heal:
  bankPollBoothDone=true with varp<530 resets and re-enters dismissal.
  Guide step is now varp-gated ONLY (>=530); the bankPollBoothDone shortcut
  is removed (it carried Build 100's false positive).
- Verify: "Build 369: poll dialogue dismissal tick N" lines, "Build 369:
  poll step GAME-VERIFIED complete (varp281>=530)", then the Account Guide
  talk with a real dialogue.

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
