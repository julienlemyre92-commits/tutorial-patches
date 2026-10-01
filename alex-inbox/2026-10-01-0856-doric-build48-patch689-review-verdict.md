# Doric Build 48 / patch-689 review verdict — PASS (Muse read-only)

Reviewed 2026-10-01 ~08:56 EDT (12:56Z). Byte-level review of patch-689.zip vs patch-688.zip. Read-only: no code touched, nothing shipped.

## Ship integrity
- version.txt=689 (no patch-number reuse).
- 215-entry net/-rooted zip (200 classes); class list identical to 688; changed classes confined to Plugin/Script/$Frame/$LoginFrame/$Pending (recompile ripple, no signature changes).
- Hot chain VERIFIED: patch-689.hot.json sha256 13c20e339d8506d637917812a98c8bab430451d2cfece4bc9be3410831d402b5 == doricsquest-48.jar. No stale-jar defect.
- runtimeBuild()=48 (bipush 48) — marker bumped correctly; commit message, hot.json build field, and bytecode agree.

## Semantics ("route the stale tin recovery into exact capacity recovery")
Two coordinated changes, both in the tin-recovery path:
1. restoreReloadState(): the bankUnownedTinRecoveryPending trigger now fires for held/phase==HOLD/pending==null with error starting EITHER "Inventory full without script-acquired tin" OR the new "Tin restart recovery evidence changed; quest=" (the stale-restart hold). New log string is the only string delta.
2. recoverUnownedTinHold(): new routing branch — if quest==IN_PROGRESS && varp==10 && tin==0 && trainingTinOwned==0 && goldBars>0 && mining<15 && inventory full && pending==null → unholds into TRAIN_MINE_TIN_CAPACITY_RECOVERY with `[DoricsQuest] RECOVER_CAPACITY_HOLD exactGoldBars={} decision=BANK_ONE_GOLD_BAR` (routes into Build 47's gold-bar capacity recovery); otherwise the existing BANK_UNOWNED_TIN_RECOVERY / RESTART_TIN_RECOVERY decision=BANK_UNTRACKED_TIN_ONLY path. Guards mirror Build 47's intercept exactly (same six conditions + pending null), so no state can satisfy both branches ambiguously.
- +0 new game-API refs. No changes to route/door/login/deposit code.

## Verdict
PASS. Matches the commit message exactly; guards are consistent with Build 47's capacity-recovery preconditions; ship chain clean. Live acceptance pending new runtime lines via Alex's runtime reports — screenshot feed still dark (~15h).

— Muse, review-loop worker
