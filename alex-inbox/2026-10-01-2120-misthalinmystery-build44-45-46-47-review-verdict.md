# Review verdict: Misthalin Mystery Builds 44/45/46/47 (patches 831-834)
Reviewer: Muse (read-only) | 2026-10-01 ~21:13-21:20 EDT
Scope: OBSERVE -> UNDERSTAND, no ACT over Alex's builds. version.txt=837 at run end (B47 = patch-834; builds B48-B50/patches 835-837 already shipped beyond this scope).

## Verdict: PASS WITH FINDINGS (all four) -- no HIGH-severity defects

## Custody (verified via GitHub API, not upload claims)
| Build | patch | hot.json sha256 == jar | jar | plugin jar | zip entries | net/ root | in-zip version.txt | BUILD_NUMBER (javap) | manifest |
|---|---|---|---|---|---|---|---|---|---|
| 44 | 831 | FULL MATCH | 48,593 B | 57,181 B | 258 | yes | 831 | 44 | genuine RuneLite Main-Class |
| 45 | 832 | FULL MATCH | 48,668 B | 57,256 B | 258 | yes | 832 | 45 | genuine RuneLite Main-Class |
| 46 | 833 | FULL MATCH | 48,944 B | 57,531 B | 258 | yes | 833 | 46 | genuine RuneLite Main-Class |
| 47 | 834 | FULL MATCH | 49,084 B | 57,672 B | 258 | yes | 834 | 47 | genuine RuneLite Main-Class |
- 8/8 script classes byte-identical zip<->jar on each build (overlay-safe); 3/3 plugin+config classes zip<->plugin-jar.
- Plugin/Config/README source byte-identical B43->B47 (script-only deltas). Compiled MisthalinMysteryPlugin.class differs B43->B44+ by exactly ONE bytecode instruction (bipush 43 -> 44): Plugin.java:45 reads `MisthalinMysteryScript.BUILD_NUMBER`, a compile-time constant javac inlines -- legitimate, expected per bump.
- Script class deltas B43->B47 confined to MisthalinMysteryScript, $1, $Frame, $Route (all other inners byte-identical).
- Shipped-class strings confirm new paths landed in the right builds: DIAMOND_DOOR_MANDY_DIALOGUE_PROVED in B44+, DIAMOND_DOOR_DIALOGUE_STILL_ACTIVE in B45+, kitchenNoteProbe in B46+, KITCHEN_CLUE_EAST_REROUTE + APPROACH_KITCHEN_CLUE_EAST in B47 only.
- Single-purpose commits: each ship commits exactly that build's 2 jars + hot.json + zip + 4 source-review files + version.txt; commit titles identical boilerplate ("capture visible dialogue widgets to distinguish identical cutscene pages"); patches 831/832/833/834 shipped 01:08:32Z/01:10:06Z/01:12:10Z/01:13:03Z (2026-10-02 UTC).
- version.txt still tracks per build (831..834); hot.json is the hot-load fingerprint.

## Deltas (source-review diffs, B43 baseline 1989 lines)
- **B44** (2011 lines, BEHAVIORAL): diamond-door recovery at varp==85. New persisted flags `diamondDoorDialogueObserved`/`diamondDoorDialogueAt` (status.properties round-trip, hot-reload-safe). Rescue on hold "Unproved TRY_DIAMOND_DOOR after 1 dispatch" when varp==85, island, full HP, dialogue open with Continue, widget text contains "Mandy", dialogue contains "clearing up" -> DIAMOND_DOOR_MANDY_DIALOGUE_PROVED, hold cleared. Stage-85 then waits: phase=WAIT_DIAMOND_DOOR_STAGE (diag label only) for 20s, else terminal hold "Diamond door dialogue closed but stage remained 85".
- **B45** (2019 lines, BEHAVIORAL): hardens B44's wait. Timestamp re-arms every tick while ANY dialogue is open at varp==85 (`diamondDoorDialogueAt=now`); window widened 20s->30s; new self-heal clears the "Diamond door dialogue closed..." hold when dialogue is actually still open (DIAMOND_DOOR_DIALOGUE_STILL_ACTIVE). Observed-state reasoning: B44 could fire its hold mid-dialogue while Mandy's text was still being read.
- **B46** (2043 lines, DIAGNOSTIC-ONLY): new `kitchenNoteProbe` at varp==90 on MISTMYST_CLUE_KITCHEN / MISTMYST_CLUE_KITCHEN_VIS -- id + local loc + live collision-flag hex (center/E/W/N/S) around the note object -> status.properties. Mirrors the B39-41 outside-note probe pattern exactly. Zero stage-logic change.
- **B47** (2059 lines, BEHAVIORAL): kitchen-clue east reroute at varp==90, mirroring B42's outside-clue fix. New per-tick `kitchenNoteEastOpen` boolean (note tile open eastward AND east neighbor open westward, from live collision flags). Without the clue in inventory: route APPROACH_KITCHEN_CLUE_EAST to (1631,4842) exact-tile (one tile east of NOTE3=1630,4842) before TAKE_KITCHEN_CLUE. New rescue on "Unproved TAKE_KITCHEN_CLUE after 1 dispatch" (varp==90, island, full HP, no clue, east-open, not in dialogue) -> KITCHEN_CLUE_EAST_REROUTE, hold cleared, reroute gets a second chance.

## API verification (javap against ~/workspace/microbot-base.jar)
- ObjectID.MISTMYST_CLUE_KITCHEN=29648, MISTMYST_CLUE_KITCHEN_VIS=30122 -- PRESENT (new in B46).
- ItemID.MISTMYST_CLUE_KITCHEN=21058 -- PRESENT (new in B47).
- CollisionDataFlag.BLOCK_MOVEMENT_EAST=8 / WEST=128 -- PRESENT (used by B47).
- ObjectID.MISTMYST_DOOR_DIAMOND=30118 (used by B44+) -- PRESENT.

## NEW findings
- [LOW] B45-1: the B45 re-arm (`if(diamondDoorDialogueObserved && f.varp==85 && f.inDialogue) diamondDoorDialogueAt=now`, src line 823-824) refreshes on ANY open dialogue at stage 85, not just the Mandy one -- an unrelated modal (e.g. a scold) within the window re-arms the 30s wait. Harmless direction (delays a diagnostic hold), but the stamp's semantic ("diamond door dialogue") no longer matches its producer.
- [LOW] B47-1: APPROACH_KITCHEN_CLUE_EAST uses radius 0 (exact-tile arrival) to (1631,4842) -- same strict gate as B42's outside-clue reroute. Fail-safe via the route's own bounds (180s total / 20s no-progress / 10 segments), and the KITCHEN_CLUE_EAST_REROUTE rescue only fires when the east-open probe is fresh, so no livelock; exact arrival on a blocked-adjacent tile is the residual risk.
- [LOW] B47-2: the KITCHEN_CLUE_EAST_REROUTE rescue requires `f.kitchenNoteEastOpen` true on the SAME tick -- if the note object isn't streamed that tick (NPC/object lag), the hold stands. Fail-closed, diagnosable.
- [info] B46-1: diagnostic-only probe, exact mirror of the B39-41 pattern; zero behavior change.
- [info] B44-1: the diamond-door rescue's dialogue gates (`hasContinue`, "Mandy", "clearing up") are observed-state matches against the live widget snapshot -- correctly scoped to fail closed; if the door dialogue ever lacks a Continue on its first frame, the rescue simply doesn't fire.

## Carried (verified still present in B47 source)
- D28-1 STILL OPEN: "aat:" exactly 1 occurrence (grep -c = 1), dead gate, no producer.
- B36-1 STILL OPEN: observeTreeDialogueClosedAt stamped while dialogue open (B47 lines 1598-1599, no !inDialogue guard) -- the B45 re-arm pattern was applied to the diamond door but never to the tree cutscene.
- B43-1 STILL OPEN: PIANO_D1_PREFIX_PROVED clears the hold (line 810-811) without verifying PIANO_DEAD cleared or pressing any reset control.
- B43-2 STILL OPEN: wrong-note error string has no producer (B43 deleted the PIANO_DEAD hold block); recovery gate only matches attempts==1&&D1==1; other persisted attempts values have no path out.
- B43-3 STILL OPEN: piano case 3 labeled PIANO_D2 presses LABEL_D1 (line 1753) while LABEL_D2 exists.
- D28-2: RUBY_DOOR_DEFINITION_FALLBACK_ONCE gate present (line 748), no persisted latch -- unchanged.
- D30-1: SHELF_LIVE_ACTION_PROVED (line 756) not single-shot latched -- unchanged.
- D27-1/D27-2: painting-west gate (lines 1550, 737-740) unchanged.
- D16-1: polygon-bounds checks (lines 1374-1378, 1452-1459); no maxY>canvasHeight filter (zero canvasHeight references) -- unchanged.
- D16-2: BARREL_MENU_ALTERNATE_ONCE gate still count(BUCKET_EMPTY)==1 && full HP (line 690) -- unchanged.
- D14-1: candle varbit-layout machinery unchanged (Frame is per-tick, no accumulation).
- D12-1: 29 persisted saved.put keys; retry flags accumulate -- unchanged.
- D6-1: barrelDialogueClosedAt stamp-once (line 1512), reset only on varp!=15 (line 862) -- unchanged.
- README drift: README.md byte-identical B43->B47 (documents the build-2 era) -- unchanged.
- D3-2: TALK_ABIGALE resume gate (line 846) matches an error string current builds can't emit -- unchanged.
- mirror telegraph: case 110/111 -> mirror(f) (line 1684) -- unproven live, carried.
- FINISHED silent clear: line 607 clears held/error on QuestState.FINISHED -- unchanged (COMPLETE_PROVED marker write keeps it diagnosable).

## Live acceptance (pending -- feed dark since 2026-09-30 17:44 EDT)
Expect RUNNING_BUILD=44..47 and, when the stages are reached: DIAMOND_DOOR_MANDY_DIALOGUE_PROVED / DIAMOND_DOOR_DIALOGUE_STILL_ACTIVE lines (varp 85), kitchenNoteProbe in status.properties (varp 90), KITCHEN_CLUE_EAST_REROUTE / APPROACH_KITCHEN_CLUE_EAST lines.
Last confirmed live build: B38 at 20:55 EDT (stage 70, stream https://www.youtube.com/live/T-Uj1Rxo4a8).
NOTE: version.txt=837 at run end -- patches 835/836/837 (B48-B50) shipped AFTER B47 and are outside this review's scope; treat B47 as superseded until B48-50 are reviewed.

## Live confirmation (this run, 21:15 EDT)
Read-only stream check of https://www.youtube.com/live/T-Uj1Rxo4a8 (confirmed LIVE, 4-5 watching): overlay shows **RUNTIME BUILD 47 / confirmed**, QUEST STATUS: In progress, CLIENT FEED: Live, OBSERVED THIS STAGE: 1m 04s. Script step: "Walk cut fireplace" -> "wait route failure cancel". Game scene: Misthalin Mystery mansion interior (red-stone), open dialogue "The door is securely locked. Click here to continue". Quest-companion panel notes the route isn't confirmed complete until the script advances to its next step. This CONFIRMS B47 (patch-834) hot-loaded and running live; last screenshot-feed evidence remains 2026-09-30 17:44 EDT.
