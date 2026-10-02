# Review verdict: Below Ice Mountain Build 47 (patch-912) — PASS WITH FINDINGS

**Reviewer:** Muse (read-only review loop worker), 2026-10-02 02:48 EDT
**Commit reviewed:** f6236d43 "Below Ice Mountain Build47 chicken pen route recovery" (2026-10-02T06:44:53Z)
**Scope:** custody verification + source diff vs Build 46 + API safety. No code shipped, no live test (feed dark ~33h).

## Custody — AIR TIGHT
- version.txt 911 → 912, sequential; commit f6236d43 is linear on a082c985 (no sibling race).
- patch-912.zip: 287 entries, net/ root, META-INF/, in-zip version.txt = 912 == repo version.txt.
- belowicemountain-47.jar SHA-256 `d5135e83e61fce802f412deea98afba571bdf338228950d5a4620ecc19e88794` == patch-912.hot.json sha256 FULL MATCH.
- BUILD_NUMBER = 47 via javap on shipped class (matches build tag).
- diff -rq patch-911 vs patch-912: every non-belowicemountain class BYTE-IDENTICAL.
- Published source (source-review/belowicemountain-build47/BelowIceMountainScript.java, 140,603 bytes, 2535 lines) vs Build 46 source: 102 added / 28 removed lines, fully accounted.
- Independent compile (JDK 17.0.20.1+1, -encoding UTF-8, against installed microbot-base.jar, with a compile-only BelowIceMountainConfig stub since the config ships with the host plugin): CLEAN, 18 classes; javap confirms CHICKEN_INTERIOR_PROOF_TILE, trainingFarmApproachComplete, trainingInteriorRepositionAttempted, legacyHpCatchupPending/Baseline, and LevelUpTabCue.noteVerifiedMissedIncrease present in BOTH compiled-from-source and shipped classes — shipped bytecode matches published source.

## What changed (commit title: "chicken pen route recovery")
1. **Farm approach latched:** TRAIN_CHICKEN_FARM walk now runs only before training begins (`!trainingStarted && !trainingFarmApproachComplete`); arrival sets the latch, and restoreReloadState derives it from trainingStarted. Fixes the live Build 44 defect (script bounced back east to the farm waypoint every tick before attacking).
2. **LOS-required target selection:** the `chickens.get(0)` no-LOS fallback is REMOVED. Selection now requires `hasLineOfSight()`; the pre-existing null-worldLocation/isDead/avoid-list filter already covers the old null-guard. On no LOS-visible chicken: full scene diagnostics (index@tile:los=...), one bounded walk to interior tile (3232,3297) with exact-tile arrival, then HOLD with diagnostics. Flag resets per successful XP proof; persists across reload (no re-walk after HOLD on reload).
3. **Disarmed-branch cue tick:** `tickLevelUpCue(f)` now runs while `!armed()` (PREFLIGHT_ACTIONS_DISABLED), gated on input ownership, !pauseAllScripts, !InputArbiter.isHuman(). Documented README behavior ("permits the verified level-up tab acknowledgment while quest actions are disarmed").
4. **One-shot legacy HP catch-up:** `shouldCatchUpBuild46Hp` derives `legacyHpCatchupPending` for Build 46 checkpoints (training started, XP actions > 0, cue baselined IDLE, saved HP >= 14); on the next armed tick, `noteVerifiedMissedIncrease(HITPOINTS, 13, current)` synthesizes the missed 13→14 cue via the normal QUEUED path. Clears after one evaluation. Fail-safe: string-keyed restores simply don't trigger it.

## Findings
- **BIM47-1 (behavior shortfall vs README, not a safety defect):** the one-shot interior reposition gets a single script tick of route flight. Tick N issues TRAIN_CHICKEN_INTERIOR_PROOF_TILE; tick N+1 re-scans, and if still no LOS chicken, `hold()` runs and `cancelRoute()` kills the in-flight walker — the player is unlikely to reach (3232,3297) in one tick (~450–2000 ms). The flag persists across reloads, so the one-shot is consumed without effect; only manual repositioning recovers. If the intent is to actually reach the interior tile, the no-LOS branch should keep returning while the TRAIN_CHICKEN_INTERIOR_PROOF_TILE route is in flight (or until its 20 s worker timeout / 2-failure HOLD) and only HOLD after the route completes without LOS. Safe as-is (HOLD is the safe state), but the recovery feature likely won't achieve its goal live.
- **INFO BIM47-2:** README changelog jumps Build 44 → 47; Builds 45/46 entries still missing (BIM46-3 partially addressed, not closed).
- **INFO BIM47-3:** `walk(...,CHICKEN_INTERIOR_PROOF_TILE,0)` uses exact-tile arrival. If the tile is ever occupied/unwalkable, the 20 s worker timeout + 2-failure HOLD bounds it. Live test item.
- **INFO BIM47-4:** catch-up block synchronizes on `levelEvents` but `tickLevelUpCue` mutates the cue on the script thread without that lock (same discipline as Build 46's baseline seeding). Benign race at worst; the one-shot self-clears either way.
- **CARRIED:** BIM46-1 (telemetry, now extended by the catch-up), BIM46-2 (0->0 cosmetic, superseded by the catch-up), BIM45-2 (Skills tab left open after PROVED), BIM45-3 (asymmetric lock discipline, benign), BIM44-1/44-2/44-3, BIM43-1/43-2/43-3, BIM42-1/42-2, BIM41-1, BIM40-1..4, bankCoins discrepancy note, guardian arena unimplemented, BIM38-1/38-2/38-3.

## Live acceptance — PENDING
Feed dark since 2026-09-30 17:44 EDT (~33h). Accept Build 47 on first sight of `CHICKEN_NO_LOS_REPOSITION` or `LEVEL_UP_CATCHUP_FROM_PRIOR_STATUS` in the diag tail, and/or RUNTIME BUILD 47 marker. Nothing observable until then.

## Verdict
**PASS WITH FINDINGS.** Custody air-tight, source diff minimal and faithful to the stated intent, independent compile clean, shipped classes match published source. No unsafe game actions; all new behavior is bounded and diagnosed. Bot Maker 2's route-recovery build is safe to hot-load when Alex is ready — with the BIM47-1 note that the interior reposition will likely HOLD after one tick rather than reach (3232,3297).
