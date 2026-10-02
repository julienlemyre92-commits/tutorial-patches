# Read-only review verdict — Below Ice Mountain Build 99 / patch-961

- Ship commit: `2e1d1a55` (2026-10-02 12:26:00Z; commit message "Below Ice Mountain Build99 guarded local path and supplies" — CORRECT this time, stale-message streak ends)
- Files: `patches/belowicemountain-99.jar`, `patches/patch-961.zip`, `patches/patch-961.hot.json`, `source-review/belowicemountain-build99/`, `version.txt` 960 -> 961
- Reviewed by: Muse review loop (read-only; Alex owns implementation/releases — no edits, no ship)
- Matches the 08:18/08:20 stream previews: Bot Maker's food-interruption fix + helper local-path candidate. B99 is the shipped form of both.
- Verdict: **PASS WITH FINDINGS** (INFO-level only, no blocking defects)

## What Build 99 changes (diff b98 -> b98.30s, normalized-whitespace; ~35 real changes)

1. `BUILD_NUMBER` 98 -> 99.
2. **Guardian movement rewritten around a client-thread local path plan** (`guardianLocalPlan`, new ~3486-3543): single client-thread snapshot BFS (<=2500 nodes, <=24 tiles from position), `WorldArea.canTravelInDirection` collision reads, LocalPoint visibility gate, cardinal-adjacency arrival (Manhattan == 1), waypoint = furthest onscreen-visible path tile within 4 path edges (canvas-projected with viewport margin). Replaces the old `near(position,tile,2)` + `isWalkableInCollisionMap`/`canReach` approach check. Per-tick `advanceGuardianWalk` controller with move proof (`GUARDIAN_STEP_MOVED`), `NO_POSITION_DELTA` after 2500ms, bounded budgets (`guardianMoveFailures>=3`, 20s deadline, `guardianMoveDispatches>=32`) -> `GUARDIAN_LOCAL_ROUTE_EXHAUSTED` + retreat (and `guardianExitFailed` when the move was an EXIT). Layout fingerprint (`guardianLayout`: pillars + broken + exit) — layout change clears the blocked set and rescans. `startGuardianWalk` refuses non-EXIT starts while `guardianRetreat` is set. Old `walkStep` fallback line is gone from this path (B99's `advanceGuardianWalk` uses only `walkFastCanvas` on the visible waypoint).
3. **Food-interruption fix** (`discardGuardianInterruptedAction`, ~3413-3424): replaces the blind `guardianPending=guardianInterruptedForFood` restore after EAT_REJECTED/EAT_PROVED/EAT_UNPROVED. Now the interrupted action is discarded with a logged `GUARDIAN_INTERRUPTED_ACTION_RESCAN` (action/tile/reason/pillarFailures); an interrupted MINE increments that tile's pillar failure count; an interrupted EXIT sets `guardianRetreat=true`. This is the exact "stop treating an old Mine click as pending after eating" fix Bot Maker previewed at 08:18.
4. **New respawn-recovery one-shot** (~865-887): matches the real same-file hold string `error.startsWith("Dungeon logout failed after bounded attempt(s)")` (set at ~965) + LOGGED_IN + quest stage 35 + near Lumbridge (3222,3217 r12) + full HP + bronze pickaxe + knife + pot of flour + `!guardianActionsAllowed()` -> clears the error, resets ALL guardian state (target/object/waypoint/pending/interrupt, blocked sets, pillar maps), sets `STAGE35_POST_DEATH_SURFACE_PREP`. Comment names the exact incident ("failed-combat-logout HOLD survived a proved Lumbridge respawn"). Tight conditions; same one-shot pattern as B98's safety-logout reset.
5. **Stage-35 re-entry now armour-gated + higher food bar**: `guardianArmourReady(f)` requires `f.equipmentLoaded` AND iron chainbody + full helm + platelegs + kiteshield — checked against the new `f.equipped` set which is populated ONLY from the EQUIPMENT container (verified lines 1430-1440; `countItem` does not touch `equipped`). Food gates raised from count>=8/budget>=70 to count>=16/budget>=160. Eat thresholds raised (retreat reserve: hp<=max(10,maxHp/2) or food<=5; eat-to max(13,maxHp-7)/max(14,maxHp-6)).
6. **Stage-35 bank food flow generalized**: replaces the trout-specific `TROUT>=10` proved-hold and `COINS>=270` withdraw blocks with a deficit loop over ENTRY_FOOD (lobster 12 / tuna 10 / salmon 9 healing): `needed=max(countDeficit, ceil(healDeficit/healing))`, withdraws `min(needed,stock)` via `Rs2Bank.withdrawX`, else HOLD "Stage35 bank lacks required high-heal food ... evaluate F2P acquisition before entry". New status properties: `stage35FoodCount`, `stage35HealingBudget`, `equipmentLoaded`, `equippedIds`, `guardianArmourReady`.

## Correctness notes

- `guardianLocalPlan` uses `Microbot.getClientThread().invoke(Supplier)` — the blocking variant, not the async trap. BFS is bounded (2500 nodes, 24-tile radius) so the tick stall is bounded by design.
- The new `STAGE35_POST_DEATH_SURFACE_PREP` stage label, like B98's label, is write-only diagnostic — control flow keys off the cleared error + reset flags.
- `guardianMoveForExit` is declared (line 559); all new fields are declared; no dangling references found.

## Custody (independently verified this run)

- hot.json sha256 == belowicemountain-99.jar sha256 (`38bf9040...c2b`) — MATCH
- patch-961.zip net/-rooted (311 net entries); only extras are `META-INF/MANIFEST.MF` + `version.txt` (the carried convention items)
- in-zip version.txt = 961 == repo version.txt (independent API read)
- `BUILD_NUMBER=99` in shipped source AND javap `ConstantValue: int 99`
- zip `BelowIceMountainScript.class` sha256 == jar class sha256 (`af92b4a9...`) — MATCH

## Findings (all INFO, Alex owns)

- BIM99-1: `discardGuardianInterruptedAction` penalizes the interrupted MINE tile (`guardianPillarFailures.merge(tile,1,...)`) even on EAT_PROVED — the pillar failure budget is charged for a food interrupt the pillar never caused. Bounded and logged, but it conflates two failure kinds.
- BIM99-2: arrival predicate tightened from Chebyshev<=2 to cardinal Manhattan==1. Conservative and self-consistent, but any interaction that was fine at diagonal adjacency now costs extra walk steps (bounded by the route budgets).
- BIM99-3: the bank withdraw loop only considers ENTRY_FOOD (lobster/tuna/salmon). Banked TROUT counts toward `stage35RecoveryFoodCount` but is never withdrawn by this loop — bank-full-of-trout still lands on the "lacks high-heal food" HOLD. Consistent with the 16/160 bar, but worth knowing if the account banks trout.
- BIM99-4: `guardianRetreat` now set by both path-exhaustion and interrupted-EXIT — two routes into the same flag with different downstream handling; the EXIT-origin case also sets `guardianExitFailed`. Fine, just noting the fan-in for future diffs.
- POSITIVE (carried findings cleared): commit message finally names the build; source-review README.md for build99 was rewritten for the actual change (274 lines, replaces the stale Build-1 handoff); official MANIFEST.MF still present (benign).

## Live acceptance (pending)

Watch for: `RUNNING_BUILD=99` / `RUNNING_BUILD 99`, `GUARDIAN_LOCAL_ROUTE`, `GUARDIAN_STEP_MOVED`, `GUARDIAN_INTERRUPTED_ACTION_RESCAN`, `STAGE35_POST_DEATH_SURFACE_PREP`, `GUARDIAN_LOCAL_ROUTE_EXHAUSTED`, the status-ping `SocketTimeoutException` open item, and the new status props. Screenshot feed dark ~38.8h; stream (youtube.com/live/T-Uj1Rxo4a8) is the live source. Nothing counts as live until fresh diag/stream evidence shows it. 08:26 run already flagged STREAM CHECK RUN for this ship — no duplicate flag.
