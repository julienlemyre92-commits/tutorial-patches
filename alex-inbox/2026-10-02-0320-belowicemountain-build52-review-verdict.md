# Review verdict: Below Ice Mountain Build 52 (patch-917) — PASS WITH FINDINGS

**Reviewer:** Muse (read-only review loop worker), 2026-10-02 03:20 EDT
**Commit reviewed:** 9c28a18 "Below Ice Mountain Build52 persisted bank recovery" (2026-10-02T07:15:16Z)
**Scope:** custody verification + source diff vs Build 51 + API safety. No code shipped, no live test (feed dark ~33.5h).

## Custody — AIR TIGHT
- version.txt 916 → 917, sequential; commit 9c28a18 linear on ce97f12 (34s later; no sibling race).
- patch-917.zip: 288 entries; entry list IDENTICAL to patch-916; in-zip version.txt = 917 == repo.
- belowicemountain-52.jar SHA-256 `5ce9c09bd29f837d462dd7b46fce5da4b27a66ee4a78e7c140374b4325fb4734` == patch-917.hot.json FULL MATCH.
- BUILD_NUMBER = 52 javap-verified.
- Byte diff 916→917: exactly 1 class — the outer `BelowIceMountainScript.class`; everything else byte-identical.
- Published source (2796 lines) diff vs Build 51: exactly 2 lines — BUILD_NUMBER 51→52 plus the change below.
- Independent JDK17 compile CLEAN (0 errors, 18 classes).

## What changed (commit title: "persisted bank recovery")
The retreat-clear condition widens from `trainingRetreat &&` to `(trainingRetreat || trainingStarted) &&` — so the bank-recovery clear also fires when the bot is mid-training at a bank without the retreat flag set (e.g. after a hot reload that persisted `trainingStarted` but not `trainingRetreat`).

## Findings
- **BIM52-1 (DEFECT, open since Build 50):** the widened clear still requires `near(f.position,FALADOR_BANK,10)`. The farm-threat retreat targets **Lumbridge** bank (3208,3220,2), so after any training threat the bot walks to Lumbridge, the clear never fires (wrong bank + `near()` requires plane equality: 2 vs 0), `walk()` returns false on arrival, and the script hits `hold("Retreated to bank after training threat; ...")` — `hold()` latches `error` permanently (tick: `if (!error.isEmpty()) { stage="HOLD"; cancelRoute(); return; }`, cleared only by hot reload). **Net effect: any training threat near the farm = permanent HOLD at Lumbridge bank; the bot never resumes.** The Build-51 return-route reset and this build's widening both only execute on the Falador path. Suggested fix: clear when near EITHER bank, or persist and compare against the targeted retreat bank.
- Carried: BIM51-1/BIM50-1 (same defect), BIM49-1, BIM48-1/48-2/48-3, BIM47-1, BIM46-1/46-2, BIM45-2/45-3, BIM44-1..3, BIM43-1..3, BIM42-1/42-2, BIM41-1, BIM40-1..4, bankCoins discrepancy, guardian unimplemented, BIM38-1..3.

**Live acceptance pending (feed dark):** `TRAIN_RETREAT_CLEARED`, RUNTIME BUILD 52.
