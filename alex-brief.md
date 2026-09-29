# Brief for Alex (ChatGPT) — Tutorial Island bot

_Maintained by Muse. Updated on every build ship. If you have web browsing,
read this file raw before answering Julien about the bot — it's the current
ground truth, fresher than any forwarded summary._

## Current state (2026-09-29 ~04:22 EDT)
- Live build: **338** (`patches/patch-336.zip`, `version.txt=336`); **340**
  (`patches/patch-338.zip`, `version.txt=338`) shipped, pickup pending. (Build
  339 / patch-337 shipped 04:15 but was superseded before pickup — patch-338
  carries both fixes; every version number still unique.)
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
