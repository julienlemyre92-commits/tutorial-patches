# Doric Build 47 / patch-688 review verdict — PASS (Muse read-only)

Reviewed 2026-10-01 ~08:52 EDT (12:52Z). Byte-level review of patch-688.zip vs patch-687.zip. Read-only: no code touched, nothing shipped.

## Ship integrity
- version.txt=688 (no patch-number reuse); in-zip version.txt=688.
- 215-entry net/-rooted zip (200 classes, +dirs/MANIFEST/version.txt); no junk paths; class list identical to 687.
- Changed classes confined to 5: DoricsQuestPlugin, DoricsQuestScript, $Frame, $LoginFrame, $Pending.
- Hot chain VERIFIED: patch-688.hot.json sha256 74ae19803e62e806b6ebc80c2da7ff4e103dbe17918992700ceeb231d86b7bbc == doricsquest-47.jar (30,821B). No stale-jar defect.
- runtimeBuild() returns 47 (bipush 47) — build marker correctly bumped; commit message, hot.json build field, and bytecode all agree this time. Acceptance still rests on NEW runtime lines, never the banner.

## Semantics (new in Build 47 — "recover mining capacity by banking one verified gold bar")
1. New Frame field `goldBars` = Rs2Inventory.count(2357) — counted by item ID (Gold bar), not by name: no substring-matching hazard.
2. tick() gains a recovery trigger: when held with error exactly "Inventory full without script-acquired tin; preserve pre-existing items" AND quest IN_PROGRESS, varp==10, mining<15, tin==0, trainingTinOwned==0, goldBars>0, inventory full → clears the hold, sets phase TRAIN_MINE_TIN_CAPACITY_RECOVERY, logs `[DoricsQuest] RECOVER_CAPACITY_HOLD exactGoldBars={} decision=BANK_ONE_GOLD_BAR`. Belt-and-braces for a hold already in place (e.g. hot-loaded onto the stuck state).
3. trainMining() intercepts BEFORE that hold: mining<15, trainingTinOwned==0, tin==0, inv full, goldBars>0 → bankOneGoldBarForMiningCapacity(). If the inventory is full WITHOUT gold bars, the old preserve-items HOLD still stands (correct).
4. bankOneGoldBarForMiningCapacity(): re-verifies goldBars>0 && trainingTinOwned==0 && tin==0 (else diagnostic HOLD "Unsafe gold-bar capacity recovery; goldBars=..."); walks to FALADOR_BANK when dist>7 (same walk() helper + distance gate as the existing TO_TRAIN_BANK flow — bank tile target, not a door tile, so the blocking-walkTo lesson does not bite); openBank rejected → diagnostic HOLD; OPEN_TRAIN_BANK pending proved by Rs2Bank.isOpen(); then Rs2Bank.depositX(2357, 1) → DEPOSIT_GOLD_BAR_TRAINING pending (9s).
5. DEPOSIT_GOLD_BAR_TRAINING proof (proved()) is exact: bank open, now.goldBars == before.goldBars - 1, tin unchanged, miningXp unchanged, counts[] equal. Exactly one bar, nothing else moves.
6. Expired pending → diagnostic HOLD (fail-closed, no spin). Rejected bank open / rejected deposit → diagnostic HOLD. All failure modes terminate visibly.

## Observations (non-blocking)
- No label-dispatch branch for DEPOSIT_GOLD_BAR_TRAINING after proof: benign — pending clears, phase stays DEPOSIT_GOLD_BAR_TRAINING, and the next tick re-derives work from quest/varp/dialogue/inventory state via getMaterials→trainMining (phase string is a status label, not the dispatch key). After the deposit, isFull() is false, so normal mining resumes with no loop.
- +0 new game-API refs; no changes to route/door/login code.

## Verdict
PASS. Semantics are sound, guards are exact, failures are fail-closed, ship chain is clean. Live acceptance pending new runtime lines (RECOVER_CAPACITY_HOLD / TO_TRAIN_CAPACITY_BANK / DEPOSIT_GOLD_BAR_TRAINING) via Alex's runtime reports — screenshot feed still dark (~15h).

— Muse, review-loop worker
