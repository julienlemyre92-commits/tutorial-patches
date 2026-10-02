# Muse read-only review verdict: shared quest services patches 974–977

Date: 2026-10-02 ~12:58 EDT (review-loop run 12:56)
Scope: Alex's shared-infra commits after BIM Build110 verdict (all unreviewed until now)
- `1c85ae8c` patch-974: shared quest services + guarded walker (Navigation service, InstalledNavigationGuard, VerifiedRoutePolicy, NavigationMicrobotDriver, QuestServiceClient/Hub)
- `77e046a4` patch-975 / `3d7589f7` patch-976: shared preparation (PreparationBankService + adapter; 976 = 975 with 4-line diff)
- `6ddc8cbd` patch-977: shared QOL (FoodAcquisitionService + adapter; nested HOLD evidence, bounded food funding reconciliation)

Reviewed files (read-only, from source-review/ on the repo): InstalledNavigationGuard.java,
VerifiedRoutePolicy.java, NavigationMicrobotDriver.java, PreparationBankService.java,
FoodAcquisitionService.java.

## Verdict: PASS (conservative design, no blocking defects)

Mechanism notes (not defects, by design):
- InstalledNavigationGuard SHA-256-verifies every listed class's installed bytecode against
  META-INF/quest-navigation-guard.properties; any mismatch/absent install leaves the driver
  unavailable rather than degraded. Exact match to the "never degrade silently" rule.
- VerifiedRoutePolicy rejects: instanced region, combat, members world, any risk budget > 0,
  teleports, non-zero-cost transports, priced/gated transports, and any route not proved
  complete to the arrival radius from the observed origin. Strictest route gate shipped so far.
- PreparationBankService and FoodAcquisitionService: every bank/GE input is one-action-per-tick
  with before/after delta proof, fresh-frame requirements (<=3000ms), account-key binding,
  8s unproved-input timeout -> HOLD (never replay), atomic checkpoint writes, and process
  file locks on the checkpoint (restoredHold reconciles after restart instead of replaying).
- `ClientThread.invoke(Supplier<T>)` used where results are needed (correct async/sync choice).
- Currency check `amount!=0 || (name!=null && !name.isBlank())` reads with intended precedence.

One minor observation (not live): NavigationMicrobotDriver.stop() returns early when a previous
clearer thread is still alive, silently dropping that route-clear request. Low risk given
single-ship usage; flagging so it is not inherited by a multi-quest host.

## Watch items carried forward (from morning runs, still unrechecked)
1. Plugin status-file write lock (stale panel-data risk).
2. Build 92 PID-bound checkpoint that could HOLD a future session instead of rebuying.
3. Screenshot feed still dark since 2026-09-30 17:44 EDT; live evidence comes from the stream.

- muse-review-loop
