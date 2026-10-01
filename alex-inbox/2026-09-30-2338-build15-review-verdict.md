# Review verdict: Ernest the Chicken Build 15 (patch-617) — PASS

Reviewed 2026-09-30 ~23:38 EDT by Muse (read-only review loop).
Repo commit: 8d257651 "Build15: explicitly open manor panelled door before closet route"
(version.txt 616 -> 617 at 23:37:29 EDT)

## Packaging (all PASS)
- `patches/patch-617.hot.json`: plugin=ernestthechicken, patch=617, hostVersion=1, build=15,
  sha256 `ccb3c167c327d45f18aae6a2e3215c566a2f1865aed3bd4bd116c32e3d331078`
  — exact-matches `patches/ernestthechicken-15.jar` (29274B) byte-for-byte.
- `patches/patch-617.zip`: 208 entries, all under `net/` root (+ `META-INF/`,
  `version.txt`), `version.txt` at root = 617, RuneLite `Main-Class`
  (`net.runelite.client.RuneLite`) manifest intact. No bad-zip class
  (the 2026-09-29 341/342 failure mode is absent).
- 6 Script classes (`ErnestTheChickenScript` + DoorCandidate/Frame/LoginFrame/
  Pending/SkillLevelReview inner classes) byte-identical zip<->jar.
  Other-plugin classes (cooks, goblin, impcatcher, pirates, restlessghost,
  romeojuliet, runemysteries, sheepshearer, tutorialisland, witchspotion, xmarks,
  grandexchange/traversal shims) zip-only = expected hot-reload artifact split.
- `RUNNING_BUILD` startup banner bipush 14 -> 15. No patch >617 on the repo,
  no duplicate ship, no pre-existing patch-618/16.jar.

## Feature drift vs Build 14 (patch-616) — exact match to commit message
- Change is confined to the 6 Ernest Script classes; every other class in the
  zip is byte-identical to patch-616 (no cross-contamination). Main Script
  49194 -> 51964B (+2770B). Inner classes: no method signature changes —
  their byte diffs are constant-pool renumbering only.
- New private method `walkLocalStep(Frame, WorldPoint)` (+ lambdas
  walkLocalStep$7/8/9/10; existing `proved` lambda renumbered $7 -> $11,
  benign), called from `exitEastRoom(Frame)`:
  - Computes `Rs2Tile.getReachableTilesFromTile(frame.pos, radius)` (collision-
    aware reachable set), filters via 3 predicates, picks min by
    `Comparator.comparingInt(distance-to-target).thenComparingInt(...)`, then
    `Rs2Walker.walkFastCanvas(tile, false)` — a local one-tile-step toward the
    manor panelled door instead of a blind long walk before the closet route.
  - Success path logs `[ErnestChicken] LOCAL_DOOR_STEP action from={} to={}
    target={}` and registers proof key `LOCAL_DOOR_STEP` via `set(...)`.
  - Failure paths hold WITH evidence (no spin): "No adjacent
    collision-reachable step toward panelled door; player=" and
    "Panelled-door step did not reach its adjacent tile; from=".
- New external API surface (2 methods), both verified exact in
  `~/workspace/microbot-base.jar`:
  - `Rs2Tile.getReachableTilesFromTile(WorldPoint, int): HashMap` — exists.
  - `Rs2Walker.walkFastCanvas(WorldPoint, boolean): boolean` — exists.

## Nits (non-blocking)
- None on the Build 15 delta itself. Carried forward: the 18:34 standing rule
  still applies (do not infer login state from the frozen HOLD status file or
  launcher OCR text alone).

## Live acceptance (pending)
Screenshot feed dark since 17:44:02 EDT (~354 min, zero ERNEST_* frames ever).
Acceptance triggers: fresh `RUNNING_BUILD=15` banner, `LOCAL_DOOR_STEP` diag
lines, or first Ernest screenshot.
