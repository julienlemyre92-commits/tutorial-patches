# Prince Ali Rescue Build 57 / patch-750 -- read-only review verdict: PASS

Reviewed by: muse-loop (2026-10-01 ~16:56 EDT, scheduled run 16:54:22)
Build: Alex-owned. Read-only review -- nothing shipped, no source edits, no releases over Alex's builds.
Commit: c2b28df82b 2026-10-01T20:54:10Z "Prince Ali Rescue Build57: logs live reachable Shantay interaction tiles at safe hold"
version.txt: 749 -> 750 (sha 9beea1f9dc6e3e79bfb53017a387a2caf78cf203)

## Chain of custody: ALL PASS

- hot.json (patch-750.hot.json): plugin=princealirescue, patch=750, hostVersion=1, build=57,
  sha256=018c7e4aed672fdaff822fcf14bb4d72774c8fde5610a03a081c93086295a3c7
- princealirescue-57.jar sha256 = 018c7e4aed672fda... == hot.json value (full 64-hex match). PASS
- patch-750.zip: 221 entries, net-rooted (only META-INF/ + version.txt non-net -- same benign shape as patches 747-749). PASS
- 3/3 script classes byte-identical zip-vs-hot-jar (PrinceAliRescueScript.class + $Frame + $Pending), 0 mismatches. PASS
- in-zip version.txt = 750. PASS
- BUILD_NUMBER=57 (PrinceAliRescueScript.java line 53; source blob 301673a0ac). PASS
- No version reuse: patch-750 / version 750 fresh, nothing overwritten.

## Delta 56 -> 57 (source diff reviewed, build57 source blob 301673a0ac vs build56 ddda67da0c)

One-shot LOG-ONLY `SHANTAY_REACHABILITY_DIAGNOSTIC`. New method
`shantayReachabilitySnapshot(WorldPoint)` runs inside `Microbot.getClientThread().invoke(Supplier<String>)`
(correct blocking-invoke usage -- pure observation, no off-thread API reads):

- finds live Trade-capable Shantay via `Rs2Shop.getNearestShopNpc("Shantay", true)`
- computes player-area -> NPC-area line of sight (`hasLineOfSightTo(view, npcArea)`)
- BFS `Rs2Tile.getReachableTilesFromTile(playerPos, 8)`, then scans the NPC-area perimeter (+-1)
  for walkable (or player-occupied) tiles with tile->NPC LoS, reporting `tile:steps` candidates
- logs npcId, npcTile, npcArea dims, player pos, distance, playerLoS, reachableLosInteractionTiles,
  reachableCount, convexHull/canvasTilePoly presence, NPC actions

Fires exactly once, only when: `!shantayPathDiagnosticLogged` && sourceItem==BRONZE_BAR && sourceGoal==1
&& geStage=="BAR_SHANTAY_SHOP" && shantayOpenRetryUsed && error startsWith
"Unproved BAR_SHANTAY_OPEN_RETRY;" or "Reload during BAR_SHANTAY_OPEN_RETRY;" (Build 55's HOLD literals).
Persisted via status.properties (`shantayPathDiagnosticLogged`, `shantayPathDiagnostic`) -- survives hot
reload; one-shot-across-reload tradeoff carried (an uninformative first capture never repeats).

Zero interaction / recovery / HOLD-mutation code touched. The line-97 `String shantayPathDiagnostic=""`
is a field on the inner Frame class (diag snapshot), distinct from the outer persisted field at line 122 --
no shadowing bug.

## Defects found: none.

[hygiene] source-review/princealirescue-build57/README.md (245 lines) is STILL the Build 1 handoff doc --
documents BUILD_NUMBER=1 only, no Build 56 or 57 mention. Stale since Build 41.

## Live acceptance (pending -- no live visual source)

Feed dark since 2026-09-30 17:44 EDT (~23.2h); no live URL confirmed. Prince Ali Builds 3-57 never
live-verified. Acceptance line: `SHANTAY_REACHABILITY_DIAGNOSTIC` in diag (fires only on a live
BAR_SHANTAY_OPEN_RETRY HOLD).
