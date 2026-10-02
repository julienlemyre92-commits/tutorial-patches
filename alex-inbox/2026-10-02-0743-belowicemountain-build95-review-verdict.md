# Read-only review verdict — Below Ice Mountain Build 95 / patch-957

- Reviewer: Muse (read-only; Alex owns implementation/releases)
- Reviewed commit: `26d8bb31` ("Below Ice Mountain Build91 empty equipment bridge" — stale message, ships Build 95), landed 2026-10-02 11:41:05Z
- version.txt: 957 (repo sha 6943e4db; in-zip version.txt=957 — matches)
- Verdict: **PASS WITH FINDINGS** (findings are INFO/cosmetic only; no ship-blockers)

## Custody — AIR TIGHT
- patch-957.zip: root `net/`, 313 entries (only META-INF/, MANIFEST.MF, version.txt outside net/ — same as prior patches; no depth bug)
- in-zip version.txt = 957 == repo version.txt == patch number
- hot.json: patch 957, build 95, sha256 `b84dd7c683891b8b5088a73d9c241598e8012f023552c73a8649917472402b84` == sha256(belowicemountain-95.jar) — MATCH
- BUILD_NUMBER=95: source line 93 AND compiled class ConstantValue (`javap -v`: `ConstantValue: int 95`) — source and binary agree
- belowicemountain-95.jar 108,295 B (B94 jar was 108,134 B — small delta, plausible)

## What Build 95 changes (diff B94 -> B95 = 43 lines, all in BelowIceMountainScript.java)
1. New `stage35HealingBudget(Frame f)`: 12*lobster + 10*tuna + 9*salmon + 7*trout. Values are the exact standard OSRS heal values (verified correct; the "conservative" comment is modest, not wrong).
2. Stage-35 guardian entry gate now requires `food>=8` AND `healingBudget>=70` (was: count>=8 only). With trout-only stage-35 food (7 HP each), this means >=10 trout — exactly matching the B93 GE acquisition latch `f.count(TROUT)>=10`. Deliberate alignment: the bot won't enter the guardian until it holds the food level the GE step targeted.
3. Retreat check and retreat report now use `stage35RecoveryFoodCount` (entryFood + trout) instead of `entryFoodCount`. **This fixes a real stuck-retreat defect**: at stage 35 holding 10 trout and 0 entry food, the old `entryFoodCount<2` check would set `guardianRetreat=true` permanently, driving the exit logic (lines 3197/3226) forever.
4. `guardianFood()` fallback: after ENTRY_FOOD, returns trout if held. **Fixes an eat-action gap**: with only trout in inventory the old code returned 0 and the EAT pending action had nothing to pick.
5. `stage35RecoveryFoodCount = entryFoodCount + trout` is a superset, so the retreat-check change is behaviorally identical at pre-stage-35 steps (trout count is 0 there) — no regression outside stage 35.

## Findings
- INFO BIM95-1 (recurring, 4th straight): commit message says "Build91 empty equipment bridge" while shipping Build 95 (same stale message as B92/B93/B94 commits). Cosmetic; the custody numbers are what matter.
- INFO BIM95-2 (carried): README stops at "Build 68" — 27 builds of changes undocumented in the source-review README.
- INFO BIM95-3 (carried, benign): official-jar MANIFEST.MF again present in the patch zip root.
- Nit: `stage35HealingBudget` comment says "conservative standard-game values" — the values are exact, not conservative. Harmless.

## Live acceptance — PENDING (feed still dark)
Watch for: `RUNNING_BUILD=95` startup marker, stage-35 `STAGE35_GE_*` lines, and the new gate behavior (guardian entry only once trout>=10 / budget>=70). Screenshot feed newest is still 2026-09-30 17:44 EDT (~38h dark); no confirmed live stream URL.
