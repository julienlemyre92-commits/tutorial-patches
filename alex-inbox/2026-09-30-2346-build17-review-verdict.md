# Review: Ernest the Chicken Build 17 (patch-619) — verdict: PASS

Reviewed 2026-09-30 ~23:46 EDT by Muse review-loop (read-only).
Commit: `99b17972` "Build17: step through manor using visited collision-reachable tiles" (2026-10-01T03:45:34Z). version.txt=619 confirmed via API.

## Checks (all PASS)
- **hot.json ↔ jar identity**: `patches/patch-619.hot.json` declares build=17, patch=619, sha256=`dc597ec6d425b997988408ed510f8929b73e1c733046936a663e8af9638a1b61`; jar `ernestthechicken-17.jar` (29825B, +479B vs build 16) hashes to exactly that value. Exact match.
- **RUNNING_BUILD marker**: banner string `[ErnestChicken] RUNNING_BUILD={} pid={}` now loads `bipush 17` (was 16) at the same code slot.
- **patch-619.zip shape**: 208 entries, all class entries under `net/` root (no junk paths), `version.txt`=619 at root, META-INF/MANIFEST.MF retains RuneLite Main-Class (jar injection remains an overlay; consistent with patch-618's layout).
- **zip ↔ jar class identity**: all 6 Script classes byte-identical zip↔jar — `ErnestTheChickenScript.class`, `$Frame`, `$Pending`, `$LoginFrame`, `$SkillLevelReview`, `$DoorCandidate`. (Config/Plugin classes byte-identical to build 16's zip — unchanged carryover, hot-reload split as in builds 15/16.)
- **Inner-class signature identity**: `$Frame`, `$Pending`, `$LoginFrame`, `$SkillLevelReview` are signature-identical to build 16 (`javap -p`); byte differences are constant-pool renumbering only. `$DoorCandidate` (DTO: object/id/pos/actions/open/close) unchanged in shape.
- **Feature drift 618→619** (confined to main Script class, +1039B): implements "visited collision-reachable" manor stepping:
  - new fields `localVisitedTiles` (Set<WorldPoint>), `localStepHistory` (ArrayDeque), `localDoorRouteActive`, `localStepCount`, `localStepGoal`.
  - `walkLocalStep`: on new goal resets history/visited, seeds visited with player pos; candidate pool from `Rs2Tile.getReachableTilesFromTile(pos, 3)` filtered by manhattan improvement (build 16) AND new `lambda$walkLocalStep$9` = `!localVisitedTiles.contains(entry.getKey())`, ranked by comparator via `stream.min`.
  - Failure posture unchanged: no candidate → `hold()` with evidence (now includes visited set); `localStepCount >= 32` → `hold()` "Local closet route exceeded 32 verified tile steps; player=… visited=…" — bounded, no spin risk. Step executes via `Rs2Walker.walkFastCanvas(tile, false)`.
- **External API refs**: zero new microbot-base.jar / RuneLite API calls vs build 16 (added only JDK lambda/synthetic entries). New APIs from build 16 (`getReachableTilesFromTile`, `walkFastCanvas`) are the only ones used.
- **Repo hygiene**: no `ernestthechicken-18.jar`, no duplicate `patch-619.*` paths, version sequence clean (616→617→618→619, never reused).

## Defects
None found. No ship-blocking or advisory issues.

## Note (not a defect)
The visited-set filter excludes already-visited tiles each tick; if the reachable set is fully visited the no-candidate hold-with-evidence fires rather than looping — acceptable under the project's hold posture.

## Live acceptance (pending, unchanged)
Screenshot feed dark since 17:44:02 EDT (~6h); zero ERNEST_* frames ever. Awaiting first Build 17 evidence: fresh RUNNING_BUILD=17 banner, new manor-stepping diag lines, or first Ernest frame.
