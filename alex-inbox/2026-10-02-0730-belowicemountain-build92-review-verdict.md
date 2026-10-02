# Review verdict: Below Ice Mountain Build 92 — stage-35 GE food restock (patch-954)

- verdict: PASS WITH FINDINGS
- scope: read-only review of commit `583dcfb` (version.txt 953 -> 954; patches/patch-954.zip, patches/belowicemountain-92.jar, patches/patch-954.hot.json, source-review/belowicemountain-build92/{BelowIceMountainScript.java,README.md})
- published by: Muse review loop, 2026-10-02 ~07:30 EDT
- custody (independently verified this run, not taken on trust):
  - in-zip `version.txt` = `954`, matches repo version.txt (sha `8a8022ae`) and patch name (no reuse of 953)
  - zip root is `net/` (310 net/ entries; file list byte-identical to patch-953's — no depth bug)
  - all 28 belowicemountain classes differ vs 953 (full recompile; source diff is surgical, see below)
  - `patches/patch-954.hot.json` sha256 `4b190b8c…af30` == `belowicemountain-92.jar` sha256 (jar-sha pattern, same as B37/B90)
  - `BUILD_NUMBER=92` in source (line 93) AND `javap -constants` on the shipped class — bytecode matches source
  - NOTE: commit message says "Build91 empty equipment bridge" but ships Build 92 (copy-paste message; numbers themselves are sequential and un-reused)

## What changed (vs BIM B91)
B91 ended stage-35 recovery with a terminal HOLD ("bank has no F2P food; need a bounded GE/shop restock plan"). B92 implements that plan: a bounded Grand Exchange restock for cooked salmon (333) then cooked trout (339) using the existing `QuestGeBuyer` machinery.

- `stage35RecoveryBank` keeps the bank-food withdraw loop, then: needs >=281 carried+bank coins (else HOLD), withdraws the deficit to 281 carried (Proof.ITEM_GAINED), then `stage35RecoveryGe`.
- `stage35RecoveryGe`: closes bank if open, walks to GE, buys salmon until 5 then trout until `entryFoodCount+trout >= 8`. Total-spend caps: salmon `min(200, carried)`, trout `min(1000, carried)`; QuestGeBuyer refuses any wiki quote where `price*quantity > cap`.
- Checkpoints: per-item files (`stage35-salmon-checkpoint.txt`, `stage35-trout-checkpoint.txt`) with atomic tmp+move writes, bound to PID + account name + stage "35" + item id — same binding discipline as the prep buyer.
- Proved-food gate: `stage35RecoveryFoodCount>=8` HOLDs deliberately ("Guardian eating and cave preflight still require review") — the restock is scoped, not a cave-entry authorization.

## Price verification (live, this run)
OSRS Wiki price API just fetched: cooked salmon (333) 20-21gp, cooked trout (339) 22-26gp. Worst-case spend: 5x21=105 (salmon, cap 200) then <=3x26=78 (trout, carried >=81 after salmon) = 183 total <= 281 top-up. The 281 figure and the caps are adequate at current prices. (An earlier draft of this verdict assumed ~200gp salmon from memory — checked live instead of trusting it.)

## Findings
F1 (new, WARNING): the stage-35 checkpoint files are never deleted. A completed purchase leaves a PID-bound checkpoint behind; on the next client session (new PID — the normal case for death recovery, which is what this flow serves) the `Files.isRegularFile(...)` gate routes straight to `stage35RecoveryGe`, `loadStage35Checkpoint` throws on PID mismatch, and the bot HOLDs on "Stage35 GE checkpoint mismatch" instead of rebuying. The HOLD is fail-closed and clearly worded, but it strands the recovery path until a human deletes the file. Suggest deleting the checkpoint file on COMPLETE (or, on mismatch, verifying no live offer exists and starting fresh). The prep buyer shares the pattern (carried), but it bites harder here because recovery recurs across sessions by design.

F2 (INFO): commit message / build mismatch — "Build91 empty equipment bridge" ships Build 92. Same class as past banner findings; numbers are correct, message is stale.

F3 (INFO, carried): the source-review README is still the Build-1 handoff doc ending at the Build-68 changelog. Builds 69-92 are undocumented in it (same gap flagged in B87/B88/B89 verdicts).

Non-issues checked: `quantity<=0` HOLD branch is unreachable (proved-gate guarantees >=1) — dead but harmless; the `coins>=281 || salmon>=5` GE routing skips the bank-food withdraw (buys fresh instead of using banked food — slightly wasteful, not a defect); pending/route/pace/bank-open gates retained and ordered correctly; withdraw only runs with bank open; trout quantity bounded by the proved-gate (<=3 once salmon>=5).

## Live acceptance (pending)
Feed dark since 2026-09-30 17:44 EDT (~37.7h); no confirmed live stream URL. Acceptance needs: RUNTIME BUILD 92 marker + `STAGE35_GE_START`/`STAGE35_GE_ORDER_PROVED` lines with real coin/food deltas. Nothing here is claimed working until observed in-game.
