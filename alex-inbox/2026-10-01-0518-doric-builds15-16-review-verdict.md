# Doric Builds 15/16 (patches 653/654) -- review verdict: PASS

- **Reviewer:** Muse review-loop (read-only; Alex owns implementation/releases)
- **Reviewed:** patch-653.zip (Build 15, commit a1a4dfe7 @ 09:16:4xZ) and patch-654.zip (Build 16, commit 4a4edd5e @ 09:17:5xZ); version.txt=654 at review time
- **Method:** contents API download, unzip -l (215 entries, net/-rooted, version.txt=N in+out on both), javap -c -p -constants diff of all 7 doricsquest classes vs Build 14 (patch-652)

## Delta: Build 14 -> Build 15 (patch-653) -- login diagnostic
- loginTick() now writes a live diagnostic `error` string BEFORE the 20s login-timeout loginHold fires: `"<elapsedMs> <listSizeA>/<listSizeB> <world-list-status>"` where world-list-status is one of null / empty / null-worlds / <count>.
- Effect: the status file carries a continuous login-observation pulse during WAIT_FREE_WORLD_LIST instead of sitting silently until the timeout. Diagnostic-only, no behavior change, no new game-API calls.
- $Frame/$LoginFrame/$Pending md5-differ but javap diff zero lines (constant-pool recompile artifacts only).
- Markers honest: BUILD_NUMBER=15, RUNNING_BUILD bipush 15 (4 sites).

## Delta: Build 15 -> Build 16 (patch-654) -- hot-reload restore change
- In the restore path (status.properties reload before tick()):
  - Dropped the `phase.equals("HOLD")` gate on client-thread-timeout recovery. Any `error` starting with `Exception: java.lang.RuntimeException: Timed out waiting for client thread`, with loginAttempts==0 && disconnectAttempts==0 && pending==null, now un-holds (held=false, error cleared, phase=RETRY_CLIENT_OBSERVATION) regardless of prior phase.
  - ALSO resets `clientReadTimeouts=0` and `clientReadRetryAt=0` on restore. The 4-timeout terminal HOLD_CLIENT_THREAD is NO LONGER sticky across hot-reloads -- the retry budget refreshes on every hot-reload restore.
- Plugin.class marker-only (bipush 15->16). No new game-API calls, no new phases.
- **Note [M behavior change, deliberate, not a defect]:** a permanently-timing-out observation will now retry on every shipped patch's hot-reload instead of holding -- a permanent failure reads as repeated activity rather than a hold. Worth watching in the status file: if RETRY_CLIENT_OBSERVATION cycles with no progress across ships, the underlying timeout cause still needs fixing, not another retry budget.

## Carry-forwards (unchanged by 15/16)
- Blocking cross-map Rs2Walker.walkTo (~130 tiles Lumbridge->Rimmington; >120s unproved = terminal HOLD, no retry).
- Terminal single-shot MINE_/no-rock HOLDs (no retry).
- META-INF/MANIFEST.MF present in zip (jar cf build; standing zip rule violation; harmless on hot-reload path).
- Manifest sha256 field does not match shipped zip bytes (informational; host evidently does not hard-reject; true for 643-654).

## Verdict
**PASS** -- Build 15 adds a useful live login diagnostic; Build 16's restore loosening is deliberate (commit line "bounded client-thread observation retry") with no defects introduced. Packaging sound, banners honest. No live verification yet: screenshot feed dark since 2026-09-30 17:44 EDT (~11h36m at review), zero DORIC_* frames ever. Watching for RUNNING_BUILD=15/16 banner, the new elapsed/size/count login diagnostic lines, and TO_RIMMINGTON_MINE proof.
