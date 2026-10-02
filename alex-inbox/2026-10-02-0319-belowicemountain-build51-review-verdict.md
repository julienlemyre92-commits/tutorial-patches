# Review verdict: Below Ice Mountain Build 51 (patch-916) — PASS WITH FINDINGS

**Reviewer:** Muse (read-only review loop worker), 2026-10-02 03:19 EDT
**Commit reviewed:** ce97f12 "Below Ice Mountain Build51 bank recovery return route" (2026-10-02T07:14:42Z)
**Scope:** custody verification + source diff vs Build 50 + API safety. No code shipped, no live test (feed dark ~33.5h).

## Custody — AIR TIGHT
- version.txt 915 → 916, sequential; commit ce97f12 linear on 068c659 (55s later; no sibling race).
- patch-916.zip: 288 entries; entry list IDENTICAL to patch-915; in-zip version.txt = 916 == repo.
- belowicemountain-51.jar SHA-256 `881344e64d89689ed1457a74c8c3ebec80fca69dca902a40653c5f93c7ad21f8` == patch-916.hot.json FULL MATCH.
- BUILD_NUMBER = 51 javap-verified.
- Byte diff 915→916: 8 classes differ, ALL inside `belowicemountain` package; 0 non-BIM classes differ.
- Published source (2796 lines) diff vs Build 50: exactly 3 lines — BUILD_NUMBER 50→51 plus the change below.
- Independent JDK17 compile CLEAN (0 errors, 18 classes).

## What changed (commit title: "bank recovery return route")
On `TRAIN_RETREAT_CLEARED`, the script now also resets `trainingStarted=false` and `trainingFarmApproachComplete=false`, so after recovering at the bank it re-approaches the chicken farm and restarts training instead of resuming mid-training state.

## Findings
- **BIM51-1 (DEFECT, carried from BIM50-1):** the clear still requires `near(f.position,FALADOR_BANK,10)` while the farm-threat retreat targets Lumbridge bank — permanent HOLD at Lumbridge after any training threat (full mechanism in the Build 50 verdict). This build's return-route reset only executes on the Falador path.
- Carried: BIM50-1, BIM49-1, BIM48-1/48-2/48-3, BIM47-1, BIM46-1/46-2, BIM45-2/45-3, BIM44-1..3, BIM43-1..3, BIM42-1/42-2, BIM41-1, BIM40-1..4, bankCoins discrepancy, guardian unimplemented, BIM38-1..3.

**Live acceptance pending (feed dark):** `TRAIN_RETREAT_CLEARED` with return route, RUNTIME BUILD 51.
