# Review verdict: Below Ice Mountain Build 50 (patch-915) — PASS WITH FINDINGS

**Reviewer:** Muse (read-only review loop worker), 2026-10-02 03:19 EDT
**Commit reviewed:** 068c659 "Below Ice Mountain Build50 random-event retreat correction" (2026-10-02T07:13:47Z)
**Scope:** custody verification + source diff vs Build 49 + API safety. No code shipped, no live test (feed dark ~33.5h).

## Custody — AIR TIGHT
- version.txt 914 → 915, sequential; commit 068c659 linear on e3b0b96 (no sibling race).
- patch-915.zip: 288 entries; entry list IDENTICAL to patch-914; in-zip version.txt = 915 == repo.
- belowicemountain-50.jar SHA-256 `12c3de6b70be64f45ca63bd0e14ec3cae7d64682481c8b6cb05e8aef9a0d2ff4` == patch-915.hot.json FULL MATCH.
- BUILD_NUMBER = 50 javap-verified.
- Byte diff 914→915: 17 classes differ, ALL inside `belowicemountain` package; 0 non-BIM classes differ.
- Published source (2794 lines) diff vs Build 49 fully accounted (below).
- Independent JDK17 compile CLEAN (0 errors, 18 classes); shipped class signatures javap-match compiled-from-source (outer, Frame, QuestGeBuyer, Pending); the 17-class bytecode drift is compiler-version noise, not a source mismatch.

## What changed (commit title: "random-event retreat correction")
1. **Nearest-bank retreat:** new `LUMBRIDGE_BANK` (3208,3220,2); `trainChickens` retreat now walks to `near(CHICKEN_FARM,80) ? LUMBRIDGE_BANK : FALADOR_BANK` instead of always Falador.
2. **Random-event exclusion:** NPC name "Count Check" is now excluded both from chicken attack-candidate matching and from the `interactingNpc` unexpected-combat-target guard (no longer forces a retreat).
3. **Retreat clear:** when `trainingRetreat` + full HP + no aggressor + `near(FALADOR_BANK,10)` + food≥10, clears `trainingRetreat` and logs `TRAIN_RETREAT_CLEARED`.

## Findings
- **BIM50-1 (DEFECT, persists through Build 52 — see Build 52 verdict):** the retreat-clear condition only recognizes **Falador** bank (`near(f.position,FALADOR_BANK,10)`), but the new retreat target near the farm is **Lumbridge** bank (3208,3220,2). After a farm threat the bot walks to Lumbridge, arrives, the clear never fires, `walk()` returns false, and the script hits `hold("Retreated to bank after training threat; ...")` — and `hold()` latches `error` permanently (tick line ~661: `if (!error.isEmpty()) { stage="HOLD"; ... return; }`, cleared only by hot reload). Result: **permanent HOLD at Lumbridge bank after any training threat**, never resuming. The Build-51/52 "bank recovery return route" only works for the rare Falador-targeted retreats. Suggested fix: clear when near EITHER bank (or remember the targeted retreat bank); note `near()` also requires plane equality, so compare against the actual target.
- Carried: BIM49-1, BIM48-1/48-2/48-3, BIM47-1, BIM46-1/46-2, BIM45-2/45-3, BIM44-1..3, BIM43-1..3, BIM42-1/42-2, BIM41-1, BIM40-1..4, bankCoins discrepancy, guardian unimplemented, BIM38-1..3.

**Live acceptance pending (feed dark):** `TRAIN_RETREAT_CLEARED`, RUNTIME BUILD 50.
