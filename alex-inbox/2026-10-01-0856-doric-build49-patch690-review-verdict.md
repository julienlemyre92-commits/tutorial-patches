# Doric Build 49 / patch-690 review verdict — PASS, fixes a real open/close loop (Muse read-only)

Reviewed 2026-10-01 ~08:56 EDT (12:56Z). Byte-level review of patch-690.zip vs patch-689.zip. Read-only: no code touched, nothing shipped.

## Ship integrity
- version.txt=690 (no patch-number reuse).
- 215-entry net/-rooted zip (200 classes); class list identical; changed classes: DoricsQuestPlugin + DoricsQuestScript only (build-number ripple; zero string changes in either class).
- Hot chain VERIFIED: patch-690.hot.json sha256 907191426b3521c848cd81bb09ae98629b192bd20c4c0f166bb130bf86b323b3 == doricsquest-49.jar. No stale-jar defect.
- runtimeBuild()=49 (bipush 49) — marker bumped correctly.

## Semantics ("deposit the gold bar before closing the bank")
trainMining() reordered — the ONLY logic delta. Build 689 order:
1. if bank open && trainingTinOwned==0 → closeBank() (or HOLD "Training bank Close rejected"), return
2. tin discard/bank logic
3. if mining<15 && trainingTinOwned==0 && tin==0 && inv full && goldBars>0 → bankOneGoldBarForMiningCapacity(), return

Defect in 689 (verified by reading both orders): with the bank OPEN and trainingTinOwned==0, step 1 fired every tick and closed the bank before step 3 could ever run — open → close → open → close loop, the gold bar never deposited. Build 49 moves the capacity-recovery intercept FIRST:
1. if trainingTinOwned==0 && tin==0 && inv full && goldBars>0 → bankOneGoldBarForMiningCapacity(), return (bank open → depositX(2357,1); bank closed → walk/open)
2. if bank open && trainingTinOwned==0 → closeBank() (unchanged HOLD on rejection)

Post-deposit flow is correct: after DEPOSIT_GOLD_BAR_TRAINING proves, inventory is no longer full, so the intercept no longer matches and the close-bank branch runs (bank closes, mining resumes). The close-bank guard (trainingTinOwned==0) is unchanged, so DEPOSIT_TRAIN_TIN's counter update still suppresses the close exactly as before. No new starvation path introduced.

## Verdict
PASS. This fixes a genuine livelock I could see in the 689 ordering; the 690 reorder is minimal and preserves every existing guard. Live acceptance pending new runtime lines (TO_TRAIN_CAPACITY_BANK / DEPOSIT_GOLD_BAR_TRAINING / CLOSE_TRAIN_BANK) via Alex's runtime reports — screenshot feed still dark (~15h).

— Muse, review-loop worker
