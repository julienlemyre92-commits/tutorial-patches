# Muse review verdict: Doric Build 34 / patch-673 — PASS (read-only)

Reviewed 2026-10-01 ~07:16 EDT by the tutorial-island-review-loop (read-only scope; no edits, no ships).
Commit: `4d82c6e9` "Doric Build34: account for single tin drop result" (2026-10-01T11:08:54Z). Patch chain: 672 → 673.

## Packaging (verified via blobs API + Accept: application/vnd.github.v3.raw)
- `patches/patch-673.zip`: 212 net/-rooted entries, ROOT is `net/`, version.txt-in-zip=`673` == repo `version.txt` — no version reuse, no upload-over-existing (both shipped by Alex).
- 200/200 class files identical lists vs patch-672. `javap -p` signatures: zero changes. `runtimeBuild()` returns 34.

## Semantic delta (normalized `javap -p -c` diff)
Real changes confined to **one method**: `recoverTinDropHold`.
- Build 33's gate (`frame.tin==before.tin==extra && Rs2Inventory.isFull()`) is replaced by: `before.tin==extra(intValue) && tinBaseline==0 && (frame.tin==before.tin || frame.tin==before.tin-1)`.
- `removed = before.tin - frame.tin` (0 or 1); `pending=null`; `held=false`; `trainingTinOwned=frame.tin` (restores the Build-32 assignment; tinBaseline already 0 by the gate so the `bankTrainingTin` guard `tin-trainingTinOwned >= tinBaseline` passes next tick).
- phase = `PROVED_PARTIAL_TRAIN_TIN_DROP` when `removed==1` (the single drop provably landed), else `BANK_TRAIN_TIN_AFTER_UNPROVED_DROP`; logs `DROP_RECOVERY exactInventoryDelta=true removed={} tinRemaining={} decision=NEVER_REPEAT_DROP`.
- Anything else → HOLD `Tin drop recovery evidence changed …` with full observed/expected detail.
- Single-shot consumption of `tinDropRecoveryPending` at entry retained (hot reload safe).

This closes the repeat-drop hazard: a drop that partially succeeded (1 tin left inventory) is now accounted for as proven and never repeated; a silently-failed drop banks the observed stack. Matches the commit message exactly.

## Invocation/API check
- `Rs2Inventory.isFull` uses drop back to 7 (the Build-33 gate is gone — it was the only use inside this method). Zero new `net/runelite/api` references (10, unchanged). Only delta in call counts: one `Logger.info` overload variant. No new game-API surface.

## Hot-reload chain: VERIFIED
`patch-673.hot.json` sha256 (`2c78ed60…`) == `patches/doricsquest-34.jar` file bytes (27,768 B); the jar's `DoricsQuestScript.class` is byte-identical to patch-673.zip's.

## Defects found
None. The two new phase strings (`PROVED_PARTIAL_TRAIN_TIN_DROP`, `BANK_TRAIN_TIN_AFTER_UNPROVED_DROP`) are set-only diagnostic labels, consistent with how this codebase routes through `trainingTinOwned`/pending fields rather than phase-string dispatch — same pattern as Build 33's bank route.

**Verdict: PASS.** Live acceptance pending (feed dark since 2026-09-30 17:44:02 EDT) — watch for `DROP_RECOVERY exactInventoryDelta=true … decision=NEVER_REPEAT_DROP` runtime lines.
