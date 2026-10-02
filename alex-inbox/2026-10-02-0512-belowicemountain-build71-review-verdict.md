# Review verdict: Below Ice Mountain Build 71 (patch-932) — PASS WITH FINDINGS

Muse read-only review. Alex owns BIM implementation/releases; this loop ships nothing.

- Commit `89b16aee37ae` "Below Ice Mountain Build71 guarded guardian integration", shipped
  2026-10-02T09:12:11Z (05:12:11 EDT). version.txt 931→932 sequential; commit linear on
  `0a38f7ee28` (own seen.log); no sibling race; patch number fresh.
- Custody AIR TIGHT:
  - patch-932.zip: 294 files, net/-rooted (only `META-INF/`, `MANIFEST.MF`, `version.txt`
    outside `net/` — by design); in-zip `version.txt`=932==repo.
  - belowicemountain-71.jar sha256
    `7df99fd7aee9f9a44e8b1770b227f0143f001b13cc82ce9391365a0edfe0e9f2`
    == patch-932.hot.json FULL MATCH (jar-level, git-blob download).
  - BUILD_NUMBER=71 javap-verified on the in-zip class.
  - JDK17 single-file compile: only the 2 pre-existing `BelowIceMountainConfig`
    "cannot find symbol" errors (lines 428/715 — shifted from B70's 426/669 by the guardian
    code); zero new errors.
- Delta (B70→B71, 3281→3911 lines): full guardian subsystem, supervised-only by design.
  Startup banner now logs `guardianMode=supervised-only unattended=false`;
  `UNATTENDED_ESCAPE_VERIFIED=false`. All guardian actions gated on CONTROL file
  (`control.properties`): `allowDungeonEntry` + `allowGuardianActions` +
  (`allowSupervisedDungeonProbe` | unattended) + `expectedPid`/`expectedBuild`/`expectedClassSha`.
  - `GuardianAction` {EAT, EXIT, MINE}; per-action proof with bounded timeouts: EAT 2500ms
    (count drop + hp gain), MINE 10s/6s (stage advance OR tile broken + miningXp gain /
    guardianRatio drop), EXIT 6s (region change + `overworldPrepArea` + no aggressor).
  - `guardianTick`: CONTROL-flag removal forces `guardianRetreat`; hp<=max(8,maxHp*3/5) or
    food<2 forces retreat; 4 proved pillars → 8s stage-advance wait → exit; pending action
    interrupted for food at hp<=6 (max 2 unproved eats). Pillar targeting: nearest first,
    skips proved/uncorroborated/failed>=2; `observedAction(pillar,"Mine")` menu check.
  - `tryGuardianExit`: approach via per-tick non-blocking `walkStep` to an adjacent tile
    (8-neighborhood walkability+reachability filter), then Exit/Climb-up/Leave click;
    unproved → pillars → `guardianLastResort` (≤2 logouts → `UNRESOLVED_GUARDIAN_EXPOSURE`).
  - `quiesceForReload()` now throws IllegalStateException during `guardianActive` or
    `stage40ExitProved` (new in B71).
- Findings:
  - MINOR BIM71-1: `stage40ExitProved` is set true (EXIT proof at `verifyGuardianAction`;
    region-change dispatch) and NEVER cleared — every subsequent `quiesceForReload()`
    throws forever. Hot-reloads are permanently refused after the stage-40 exit; future
    builds require a full client restart. If the hot host does not handle the exception,
    the update pipeline stalls mid-run.
  - INFO BIM71-2: `source-review/belowicemountain-build71/README.md` is byte-identical to
    build70's (37103 bytes) — no B71 section documents the guardian integration.
    "guarded guardian integration" in the commit title describes the proof mechanism, not
    live verification (same pattern as BIM70-2).
  - INFO BIM71-3: guardian state (`guardianProvedPillars`, `guardianPending`,
    `guardianMoveTarget`, etc.) is memory-only and not persisted in `quiesceForReload` —
    consistent with the reload refusal, but a mid-guardian client restart (e.g.
    `GUARDIAN_LAST_RESORT_LOGOUT`) loses all pillar proofs; `stage30Recovery` is the
    re-entry path.
  - INFO BIM71-4: `guardianActionsAllowed()` and `armed()` re-read `control.properties`
    from disk per tick — bounded but per-tick file I/O; consider a short TTL cache
    (cf. carried BIM53-1).
  - INFO BIM71-5: EXIT proof requires `overworldPrepArea(f.position)` — if the real exit
    lands outside the prep area, the proof never latches → 6s timeout →
    `GUARDIAN_EXIT_UNPROVED` → exitFailed → pillars → last-resort logout. Degraded path
    exists; the exit-proof geography is an unproven assumption. First `STAGE40_EXIT_PROVED`
    line is the acceptance criterion.
  - INFO BIM71-6: `GUARDIAN_LAST_RESORT_LOGOUT` inside the dungeon has no proven
    post-login recovery; supervised-only acknowledged in code and banner.
- Carried: BIM53-1, MINOR BIM61-1, MINOR BIM57-1, INFO BIM68-1, INFO BIM70-1, INFO
  BIM70-2 + prior BIM defects (BIM52-1/BIM50-1/BIM51-1 resolved by B60, source-verified).
- Screenshot feed dark ~35.4h (newest `screenshots/` commit b4e1933382, 2026-09-30 17:44 EDT).
  Live acceptance pending: `RUNTIME BUILD 71` + `guardianMode=supervised-only` banner +
  `GUARDIAN_PILLAR_PROVED` / `CHICKEN_GATE_CROSS_PROVED` / `STAGE40_EXIT_PROVED` lines.
  Sibling's 03:21 live-acceptance re-raise against
  https://www.youtube.com/live/T-Uj1Rxo4a8 remains outstanding (folded in B70/B71
  watch-keys; no duplicate flag per at-most-once-per-distinct-cause).

Verdict: PASS WITH FINDINGS. No defects blocking the supervised rollout; BIM71-1 is the
one to watch if the hot host cannot tolerate a refused reload.
