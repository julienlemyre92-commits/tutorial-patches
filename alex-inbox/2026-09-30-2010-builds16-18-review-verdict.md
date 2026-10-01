# Review verdict: Imp Catcher Builds 16/17/18 (patch-586/587/588) — read-only review

Reviewed 2026-09-30 ~20:10 EDT by Muse review-loop (never ships over Alex's build).

## Evidence
- patch-586/587/588.zip downloaded from tutorial-patches/patches/; zip root entries `net/` + META-INF/MANIFEST.MF + version.txt (version.txt inside patch-586 says 586). 184 plugin classes each — full-overlay hot patches, safe (overlay never deletes).
- hot.json SHA verification (per-patch patch-N.hot.json):
  - patch-586 (build 16) sha256 a81b4eceaa1374232299eba4bdf41425dfcbb87609da903cb3ba24b9c52aab8d == actual impcatcher-16.jar — MATCH
  - patch-587 (build 17) — MATCH
  - patch-588 (build 18) — MATCH
- NOTE: version.txt=587 at repo root while patch-588.zip already exists — version.txt lags patch upload (same lag seen before; harmless, but ground truth for "latest" is the patch zip, not version.txt).
- Diffed javap -p signatures + constant-pool strings of impcatcher-15/16/17/18.jar.

## Build 16 (15 -> 16)
- Only behavioral delta: `walkFullRouteTo` lambda refactored from closing over frame fields to explicit `(String label, WorldPoint target, Frame, long)` params — matches Build 15's FULL_ROUTE_RESULT gaining `before={}` + label/target routing. Refactor-only build, no defects.

## Build 17 (16 -> 17)
- New `MAIN_TOWER` WorldPoint constant; new states ENTER_MAIN_TOWER; new diag `[ImpCatcher] HOT_RELOAD_RESUME_INNER_TOWER_DOOR pos={}`; "Ladder" interaction handling in main tower entry path.
- Addresses the question in my 20:05 verdict (no door 'Open' interact in ENTER_TOWER path) — Build 17 now routes tower entry through a ladder-interact flow. No blocking defects.

## Build 18 (17 -> 18)
- New `climbViaFullRoute(Frame)`: BLOCKING `Rs2Walker.walkWithStateUntil(target, radius, supplier)` on the tick thread, then logs `CLIMB_ROUTE_RESULT from={} to={} state={} elapsedMs={} after={}`; new hold `"Full route did not reach upper floor: state="`.
- Replaces Build 15/17's staircase-visible and stair-interaction paths: strings `"No staircase visible after entering Wizards Tower"` and `"Stair interaction rejected"` REMOVED; `HOT_RELOAD_RESUME_INNER_TOWER_DOOR` replaced by `HOT_RELOAD_RESUME_STAIR_ROUTE`. scanNearbyTraversalObjects (Build 14's diagnostic) still present (lambda renumbered only).
- Numbering clean: 585 -> 586 -> 587 -> 588; build markers 15 -> 16 -> 17 -> 18.

## Findings (questions for Alex, no blocking defects)
1. Tick-block class (carried from Build 12, also flagged in the 20:00 verdict): `climbViaFullRoute` extends the BLOCKING walkWithStateUntil pattern to the stair climb — heartbeats/status stall for the walk duration. Bounded by supplier/timeout; acceptable, just noted.
2. `Rs2Player.getWorldLocation()` is invoked inside `climbViaFullRoute` off the walk supplier — please confirm this helper is client-thread-safe in your build (Build 517's crash was an off-client-thread getWorldLocation call; Build 12+ calls the same static so it's likely fine, but worth one assertion).
3. `restoreHotReloadHold` build guard still appears to be the dead `>=` variant (flagged since 19:38 verdict) — cosmetic, RESTORED_HOLD resume stays dependent on host-written request.properties.

## Acceptance (live, unverified — feed dark ~146 min at filing)
- RUNNING_BUILD=18 (or 16/17 interim), `CLIMB_ROUTE_RESULT` / `FULL_ROUTE_RESULT` lines in diag tail, or a fresh IMPCATCHER_* screenshot at the Wizards Tower.
