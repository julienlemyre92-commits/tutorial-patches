# Muse review verdict: Doric Build 33 / patch-672 — PASS (read-only)

Reviewed 2026-10-01 ~07:15 EDT by the tutorial-island-review-loop (read-only scope; no edits, no ships).
Commit: `54f38605` "Doric Build33: bank tin after failed drop recovery" (2026-10-01T11:07:46Z). Patch chain: 671 → 672.

## Packaging (verified via blobs API + Accept: application/vnd.github.v3.raw)
- `patches/patch-672.zip`: 212 net/-rooted entries, ROOT is `net/` (no one-level-too-deep fault), version.txt-in-zip=`672` == repo `version.txt` at review start — no version reuse.
- 200/200 class files identical lists vs patch-671 (no stale-class reship, no partial-overlay gap).
- `javap -p` signatures: zero signature changes across all 64 methods. `runtimeBuild()` returns 33 (BUILD_NUMBER 32→33 confirmed in method body).

## Semantic delta (normalized `javap -p -c` diff, constant-pool refs + branch offsets stripped)
Real changes confined to **two methods**:
1. `recoverTinDropHold` — the exactNoChange gate now requires `frame.tin==before.tin==extra` **AND `Rs2Inventory.isFull()`** (the isFull util was already called 7× elsewhere in this class — no new game-API call). On success: `trainingTinOwned=0`, `tinBaseline=0` (Build 32 set `trainingTinOwned=frame.tin`; Build 33 zeroes both, banking the observed stack instead), `pending=null`, `held=false`, phase=`BANK_TRAIN_TIN_AFTER_UNPROVED_DROP`, log `DROP_RECOVERY exactNoChange=true tin={} decision=BANK_OBSERVED_STACK_AND_PROVE`. Anything else → HOLD with `Tin drop recovery evidence changed ...` detail (tin/quest/varp/xp/baseline/extra).
2. `restoreReloadState` — re-arms `tinDropRecoveryPending` when a hot-reload restores a Build-32 session whose `error.startsWith("Tin drop recovery evidence changed")` with `before.tin>0`, so the new bank-observed-stack branch gets its chance after reload. Coherent with the commit message.

## Routing after recovery (traced, not assumed)
Next tick: logged-in path drains `tinDropRecoveryPending` first (Build 32 change), recovery consumes it single-shot at entry. Then `mining<15 || trainingTinOwned<=0` → `trainMining` → `trainingTinOwned>0` and `tin-trainingTinOwned >= tinBaseline` → `bankTrainingTin` → TO_TRAIN_BANK / OPEN_TRAIN_BANK / DEPOSIT_TRAIN_TIN (`Rs2Bank.depositX(438, n)`). Phase strings are diagnostic; routing goes through the owned-count fields. The Build-31 `tinBaseline==0` gate is replaced by the isFull gate here (it returns in Build 34).

## Hot-reload chain: VERIFIED
`patch-672.hot.json` sha256 (`540da9ae…`) == `patches/doricsquest-33.jar` file bytes (27,680 B); the jar's `DoricsQuestScript.class` is byte-identical to patch-672.zip's. Patch path and hot path carry the same code.

## Defects found
None. Conservative, matches the commit message exactly, no new game-API surface, no unhandled phases introduced.

**Verdict: PASS.** Live acceptance still pending (screenshot feed dark since 2026-09-30 17:44:02 EDT) — watch for `DROP_RECOVERY … decision=BANK_OBSERVED_STACK_AND_PROVE` runtime lines in Alex's direct reports.
