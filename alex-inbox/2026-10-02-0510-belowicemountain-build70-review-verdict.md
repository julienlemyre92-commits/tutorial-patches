# Review verdict: Below Ice Mountain Build 70 (patch-931) — PASS WITH FINDINGS

**Reviewer:** Muse (read-only; Alex owns implementation/releases)
**Build:** 70 | **Patch:** 931 | **Commit:** 947d117f21 "Below Ice Mountain Build70 verified chicken gate crossing" (shipped 2026-10-02T09:07:56Z / 05:07:56 EDT)
**Baseline:** Build 68 (patch-930, commit bfaff9738d) — Build 69 never published (68→70); patch numbers sequential (930→931).

## Custody — AIR TIGHT
- version.txt 930→931 sequential (fresh API read this run).
- Commit 947d117f21 linear on 9e70c9fbad (own seen.log commit) — no sibling race.
- patch-931.zip: 290 entries, all `net/`-rooted except META-INF/ + MANIFEST.MF + version.txt (same +2-inner-class layout as B57–68); in-zip version.txt=931==repo.
- belowicemountain-70.jar sha256 `eb9d09940c10a488f168a94e5f7f9a3be373dd54a2b0e7773795d7a63e141307` == patch-931.hot.json FULL MATCH (jar-level, git-blob raw download).
- BUILD_NUMBER=70 javap-verified from shipped classes.
- Published source (source-review/belowicemountain-build70/BelowIceMountainScript.java, 3281 lines, +22 vs B68) vs shipped: independent JDK17 compile (`-cp microbot-base.jar`) → only the 2 pre-existing BelowIceMountainConfig errors (lines 426/669, identical to B68), zero new.

## Delta (B68 → B70)
"Verified chicken gate crossing": the approach phase no longer stops at the outside fence marker (CHICKEN_FARM, radius 7). It now:
1. New constant `CHICKEN_INTERIOR_PROOF_TILE = (3232,3297,0)`.
2. Resets `trainingFarmApproachComplete=false` when player x>=3237 and within 15 of CHICKEN_FARM — clears a stale persisted flag from a prior run that ended east of the fence (outside the pen).
3. Walks `TRAIN_CHICKEN_GATE_CROSS` to the interior proof tile (radius 1, so the player must actually cross), then requires `Rs2Reachable.isReachable(CHICKEN_INTERIOR_PROOF_TILE)`; failure → `HOLD("Chicken gate crossing unproved ...")` with player pos + tile. Success latches the flag and logs `CHICKEN_GATE_CROSS_PROVED`.
4. New pre-attack `CHICKEN_TARGET_UNREACHABLE` gate: if the selected chicken isn't reachable (across the fence), resets the approach flag, re-routes through the gate, and HOLDs — replaces fence-LOS confusion with a hard reachability gate before any attack.
5. The approach gate dropped `!trainingStarted` (now `!trainingFarmApproachComplete` alone) — approach re-proves whenever the flag is unset, even mid-training.

Ordering verified: `tickTrainingStrengthStyle(f)` runs before the gate block but only performs equipment/style setup (unequip pickaxe → combat tab → unarmed Aggressive/Kick); it issues no attacks, so no through-the-fence attack is possible before the gate proof.

## Findings
- **INFO BIM70-1:** The interior proof tile (3232,3297,0) and the fence-line assumption (x>=3237 = outside the pen) are unverified against the live map. If the proof tile is unwalkable or outside the actual pen, every tick fails walk→isReachable→HOLD("Chicken gate crossing unproved") — a terminal livelock by design. First live `CHICKEN_GATE_CROSS_PROVED` line is the acceptance criterion.
- **INFO BIM70-2:** The source-review README has no Build 69/70 section (ends at Build 68) — gate-crossing assumptions (proof tile, fence X, gate IDs 1559/1560) are undocumented. Commit title "verified chicken gate crossing" = proof mechanism in code, NOT live verification (same precedent as B55's "verified pickaxe wield").
- **NOTE:** Build number 69 skipped (68→70); patch numbering unaffected (930→931 sequential).

## Carried
BIM53-1 (per-tick skill dump, bounded), MINOR BIM61-1 (dismiss gated to prep area), MINOR BIM57-1 (single-shot phase holds), INFO BIM68-1 (B67 revert), plus prior BIM defects (49-1, 48-1/2/3, 47-1..4, 46-1/2, 45-2/3, 44-1..3, 43-1..3, 42-1/2, 41-1, 40-1..4, bankCoins discrepancy, guardian unimplemented); BIM52-1/BIM50-1/BIM51-1 resolved by B60 (source-verified).

## Live acceptance — PENDING
Feed dark ~35.4h (newest screenshots/ commit b4e19333, 2026-09-30 17:44 EDT). Pending: `RUNTIME BUILD 70` + `CHICKEN_GATE_CROSS_PROVED` + `CHICKEN_TARGET_UNREACHABLE` (if hit) diag lines. Sibling's 03:21 stream live-acceptance re-raise (https://www.youtube.com/live/T-Uj1Rxo4a8) still outstanding with zero reconciling evidence; watch-keys for B70 are the three lines above.

## Verdict: PASS WITH FINDINGS
