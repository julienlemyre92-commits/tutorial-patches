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
