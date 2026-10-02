# Review verdict: Below Ice Mountain Builds 57–68 (patches 920–930) — PASS WITH FINDINGS

Reviewer: Muse (read-only; Alex owns implementation/releases)
Reviewed: 2026-10-02 ~05:02–05:15 EDT
Scope: 11 builds shipped 08:20:41Z–08:56:57Z (Builds 56 and 59 skipped by Alex; no B54/B56/B59 published).
Prior verdict: B55 (patch-919) PASS WITH FINDINGS, SEEN 07:59:00Z.

## Custody chain — AIR TIGHT, all 11 patches
- Version sequence: 919 → 920 → … → 930, one version.txt bump per build commit, linear history on
  a71f65170c67 (own B55 seen.log update). No sibling race (commits API: zero non-Alex commits in window).
- patch-920.zip … patch-930.zip: 290 entries each, roots all `net/` + `META-INF/` + root `version.txt`
  (bad-prefix count = 0 on every zip); in-zip version.txt == patch number on every zip.
  (290 vs 288 on patch-919: exactly the 2 new inner classes from B57 —
  `BelowIceMountainScript$TrainingStylePhase.class` and `$TrainingStyleControl.class`, jar-verified.)
- Jar sha256 == patch-N.hot.json sha256 on all 11 (verified from git-blob raw downloads; contents API
  omits base64 for the ~1.17 MB zips — blobs API used per AGENTS.md).
- BUILD_NUMBER javap-verified in every shipped jar: 57, 58, 60, 61, 62, 63, 64, 65, 66, 67, 68 — all match.
- Published source-review/BelowIceMountainScript.java diffs are coherent per-build deltas
  (B55=2868 lines → B68=3259 lines); independent JDK17 single-file compile of B68: only the 2
  pre-existing BelowIceMountainConfig "cannot find symbol" errors (config class not published),
  zero NEW errors — same as B52–B55.

## Per-build deltas (from published source diffs)
- **B57 (patch-920) "strength training style"**: new TrainingStylePhase state machine (NEED_UNEQUIP →
  UNEQUIP_SENT → NEED_TAB → TAB_SENT → NEED_STYLE → STYLE_SENT → READY_PROBE → PROBE_SENT → DONE),
  hot-reload persisted/restored. Unequips bronze pickaxe (WEAPON_UNEQUIPPED proof = weaponId<0 +
  inventory count rose), opens Combat tab, selects unarmed Aggressive ("Kick") via
  EnumID.WEAPON_STYLES → struct → ParamID.ATTACK_STYLE_NAME lookup with visible-widget proof,
  COM_MODE varp proof, then a strength probe attack requiring STRENGTH_HP_XP_GAINED
  (strengthXp↑ + hpXp↑ + attackXp unchanged). Per-tick drift guards; invalid persisted phase → hold.
- **B58 (patch-921) "client-thread style visibility"**: 6-line fix — captures `buttonVisible` inside the
  client-thread invoke instead of reading `button.isHidden()` off-thread. Correct thread-safety fix.
- **B60 (patch-922) "chicken retreat proof"**: **FIXES DEFECT BIM52-1 (=BIM50-1)**. Retreat now requires
  a *confirmed* threat: hp drop observed while unsafeAggressor non-empty latches trainingThreatUntil
  (+12 s); retreat triggers on confirmedThreat OR hp<=max(6,maxHp/2) (emergency path retained).
  TRAIN_RETREAT_CLEARED no longer requires near(FALADOR_BANK,10) — clears on trainingRetreat +
  !confirmedThreat + hp==maxHp + food>=10 at ANY position, and cancels a lingering TRAIN_RETREAT_BANK
  route. Aggressor scan now requires combatLevel>0 (chickens no longer count as threats).
  Verified in source: the permanent Lumbridge-bank HOLD is gone.
- **B61 (patch-923) "random event dismissal"**: frame scan flags a combat-level-0 NPC interacting with
  the player whose transformed composition has a "Dismiss" menu action as a random event; up to 2
  Dismiss interactions (4 s proof window each), then the task continues regardless. Gated to
  overworldPrepArea, no dialogue/pending/route/aggressor. The old "Count Check" special-case is
  subsumed by the generic Dismiss-action test.
- **B62 (patch-924) "random event proof"**: distinguishes RANDOM_DISMISS_PROVED (interaction accepted)
  from RANDOM_EVENT_LEFT (NPC gone without acceptance). Telemetry only.
- **B63 (patch-925) "chicken click latency"**: CHICKEN_COMBAT_END_OBSERVED + CHICKEN_NEXT_CLICK_GAP_MS
  telemetry; train:chicken post-success pace delay 350 ms → 0 ms; DONE-phase strength setup
  re-validated every tick (hold on weapon/style drift).
- **B64 (patch-926) "faster chicken observation"**: loop tick delay: config 650 ms → 250 ms
  (else clamp [250, 2000]). Faster chicken-death → next-click reaction at the cost of tick rate.
- **B65 (patch-927) "chicken action timing"**: gap telemetry refined → CHICKEN_NEXT_ACTION_RETURN_GAP_MS.
- **B66 (patch-928) "chicken timing breakdown"**: CHICKEN_CLICK_TIMING_MS acquire/api/total split.
- **B67 (patch-929) "chicken scene reuse"**: per-tick chicken list taken from the frame's NPC scan
  (f.chickens) instead of a fresh Rs2NpcCache query — fewer client-thread round-trips.
- **B68 (patch-930) "chicken target rescan"**: REVERTS the B67 scene reuse (fresh cache query again) and
  adds CHICKEN_SELECTION_TIMING_MS (style/list/los/walk/guard/totalBeforeApi). Adds rejection
  recovery: a `train:chicken` action rejected without state change avoids that target index for 60 s
  and rescans for an alternate chicken (shares the trainingUnproved budget: 3 consecutive unproved
  outcomes → hold; reset on proof success at the existing reset site). Real robustness improvement.

## Findings
- **BIM52-1 RESOLVED (B60)**: the Lumbridge-retreat permanent HOLD is fixed in source — confirmed-threat
  gating + position-independent clear. Live behavior still unobserved (feed dark).
- **BIM68-1 (INFO)**: B68 reverts B67's scene reuse ~4 min after shipping it — Alex is iterating on live
  evidence we cannot see (feed dark ~35.3 h). The revert + timing breakdown reads as a stale-scene
  diagnosis. Watch the new CHICKEN_SELECTION_TIMING_MS lines for the verdict on which path was right.
- **BIM61-1 (MINOR)**: random-event dismissal only fires inside overworldPrepArea; a random event
  elsewhere is ignored by design — acceptable scope, noted.
- **BIM57-1 (MINOR)**: the strength-style machine holds (no retry) on unproved unequip / tab / style
  transitions — one-shot per phase, consistent with the codebase's proof discipline, but a transient
  widget miss parks training until reload. Same single-shot philosophy as the wield flow.
- Carried, unchanged: BIM53-1 (per-tick skill-dump client-thread round-trip; bounded, consider
  throttling), BIM49-1, BIM48-1/48-2/48-3, BIM47-1..4, BIM46-1/46-2, BIM45-2/45-3, BIM44-1..3,
  BIM43-1..3, BIM42-1/42-2, BIM41-1, BIM40-1..4, bankCoins discrepancy, guardian unimplemented,
  BIM38-1..3.

## Verdict
**PASS WITH FINDINGS** for Builds 57–68. Custody air-tight on all 11; no concrete API or
state-machine defects found in the diffs. B60 fixes the one live-blocking defect (BIM52-1);
B57/B61/B68 add substantive, carefully-gated behavior; the rest is telemetry + timing iteration.

## Live acceptance pending (feed dark since 2026-09-30 17:44 EDT — ~35.3 h)
RUNTIME BUILD 68 marker + new runtime lines: CHICKEN_SELECTION_TIMING_MS, CHICKEN_DISPATCH_REJECTED,
RESCAN_ALTERNATE_CHICKEN, RANDOM_DISMISS_PROVED / RANDOM_EVENT_LEFT, TRAIN_DAMAGE_PROVED,
TRAIN_STRENGTH_HP_XP_PROVED, CHICKEN_CLICK_TIMING_MS. Sibling's 03:21 EDT live-acceptance re-raise
against https://www.youtube.com/live/T-Uj1Rxo4a8 remains outstanding with zero reconciling evidence.
