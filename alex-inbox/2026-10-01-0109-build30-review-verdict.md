# Review verdict: Ernest Build 30 (patch-632) — PASS

- **Commit:** f4acc193, 2026-10-01T05:06:01Z — "Build30: open closet from collision-proved player side"
- **Packaging:** CLEAN. 208-entry `net/`-rooted zip (built with `zip`), `version.txt`=632 inside and out (repo root now 632), `BUILD_NUMBER`=30 (`javap -constants`), `runtimeBuild()`=30 (bipush). hot.json sha256 (`596fe62c...`) exact-matches `ernestthechicken-30.jar`; all 6 `ErnestTheChickenScript*` classes byte-identical zip<->jar. Inner classes differ byte-wise vs Build 29 — pure constant-pool churn (no source change; signatures/fields unchanged).
- **New flow** (new branch in `gaugeAndTube`, ahead of the legacy OPEN path):
  1. Gate: `closetDoorUnlocked && closetKeyUseAttempts>=1 && closetOppositeSideOpenAttempts<1` — fires only after the key-on-door path has run at least once.
  2. Door id must be 131, else HOLD ("Opposite-side probe refused unexpected door id=").
  3. `findClosetApproachStand(Frame, DoorCandidate)` picks the stand tile; null -> HOLD with the approach diag.
  4. Stand re-verified every tick inside `getReachableTilesFromTile(playerPos, 3)`; fails -> HOLD ("Selected closet stand lost reachability before movement").
  5. Player != stand -> `walkLocalStep` per tick; player == stand -> `armClosetMenuTrace` + `Rs2GameObject.interact(doorObject, "Open")`.
  6. Success: `CLOSET_OPPOSITE_SIDE_OPEN_DISPATCH attempt={} door={} stand={} tube={} playerReachable={} tubeReachable={}` + pending `OPEN_CLOSET_FROM_PLAYER_SIDE` (8s deadline). interact-false -> clear trace + HOLD.
- **Stand selection** (`findClosetApproachStand`): door-adjacent (dx/dy ±1) candidates, filtered by player-reachable map, tube-side reachability, edge-passability, line-of-sight, collision flags; `stream.min` by (path cost, manhattan distance to player). Rich per-candidate diag strings (walk/edgeOut/edgeBack/losDoor/losTube/flags).
- **External-API audit: PASS.** 114 refs vs 113 in Build 29; the only delta is the script's own new `findGroundTubeLocation` — zero new external API calls. The new path composes already-reviewed APIs only (interact, walkLocalStep, armClosetMenuTrace, getReachableTilesFromTile).
- **Findings:**
  - [M] `closetOppositeSideOpenAttempts` is restored from and persisted to `status.properties` (same as the Builds 26/27 budgets) — one dispatched-but-unproved player-side Open means the new path never retries, even across restarts; it degrades to the legacy OPEN path rather than HOLDing. Suggest a future build replenish the budget on fresh unlock proof or on restart when the door is still closed+locked-out.
  - [L] `tubeReachable=` in the dispatch log prints the keySet of the tube radius-12 map while `playerReachable=` prints the player radius-3 Map — slightly inconsistent semantics; diag-only.
  - [L] The radius-3 reachability re-check every tick assumes deterministic stand selection (no jitter observed possible — feed dark).

**Verdict: PASS.** Ship-quality. Live acceptance pending (screenshot feed dark since 17:44:02 EDT 2026-09-30, ~7.6h; zero ERNEST_*/IMPCATCHER_* frames ever). Acceptance triggers: a `CLOSET_OPPOSITE_SIDE_OPEN_DISPATCH` diag line, or a fresh `RUNNING_BUILD=30` banner.
