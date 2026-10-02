# Review verdict: Below Ice Mountain Builds 73–77 (patches 934–938) — PASS WITH FINDINGS

Muse read-only review. Alex owns BIM implementation/releases; this loop ships nothing.
Tutorial Island tree idle since 2026-09-29; BIM fronts are read-only review only.

## Chain / custody (all 5, git-blob raw downloads, independent)

- Commits: B73 `f22417d946` "guarded stage30 cave recovery" (09:24:42Z),
  B74 `d29b328f4d` "bounded stage30 Willow probe" (09:33:06Z),
  B75 `57513195af` "cold restart readiness" (09:34:41Z),
  B76 `605380601d` "stage30 scene observation" (09:42:14Z),
  B77 `3b8f2e2868` "dynamic instance observation" (09:46:29Z).
  Linear: B73's parent = own B72 seen.log `4d42ca83c6`; B74..B77 each parent the
  previous build commit. No sibling race. version.txt 934→935→936→937→938
  sequential at each commit, == repo HEAD.
- patch-934..938 zips: 294 files each, net/-rooted (only META-INF + version.txt
  outside `net/`, by design); in-zip version.txt == patch number == repo for all 5.
- belowicemountain-73..77.jar sha256 == patch-N.hot.json (`sha256` key) FULL MATCH
  for all 5: `7e0d822c…`, `b743c2b1…`, `2abc9f19…`, `cd766f85…`, `bc3c4706…`.
- BUILD_NUMBER javap-verified 73/74/75/76/77 (`bipush N` in static init).
- JDK17 compile of published B77 source (microbot-base.jar on classpath): only the
  2 pre-existing `BelowIceMountainConfig` "cannot find symbol" errors (lines 428/726,
  same class missing from source-review/ since B70); zero new errors.
- `source-review/belowicemountain-build{N}/` has Script.java + README.md for all 5;
  all 5 READMEs byte-identical (sha `c8f8a76913`, 37103 bytes) — no build sections.

## Deltas (published source diffs, B72 base 219175 bytes)

- **B73 (+4907) "guarded stage30 cave recovery"**: CONTROL-gated
  (`allowStage30CaveRecovery` + expectedPid + expectedBuild + expectedClassSha,
  classHash 64-hex required) stage-30 cave recovery. safetyLogoutIssued now
  re-enables via `stage30CaveRecoveryAllowed()` (logs STAGE30_CAVE_RECOVERY_LOGIN_ENABLED)
  instead of holding. New `recoverStage30EmptyCave()`: dialogue-first, guardianScene
  proof; guardian/aggressor/pillars → Rs2Player.logout() + safetyLogout re-arm
  (STAGE30_CAVE_RECOVERY_ABORT); else walks to the observed "Exit" tile object
  (walk-progress proof, 3-stall → HOLD "Stage30 cave Exit approach stalled"),
  VERIFY_STAGE30_CAVE_EXIT up to 12s then HOLD, STAGE30_CAVE_EXIT_PROVED latch when
  questStage==30 && overworldPrepArea (clears entranceAt). entranceScene and
  inKnownCaveInstance extended for a second observed instance base
  (x 12800–13200, y 8400–8700).
- **B74 (+2044) "bounded stage30 Willow probe"**: CONTROL-gated
  (`allowStage30WillowRetry` + pid/build/sha) single-shot Willow retry at prepStep 30:
  walk to DUNGEON_WILLOW, `npcAt(BIM_WILLOW)`, Talk-to with DIALOGUE_CHANGED proof
  (10s). Persisted `stage30WillowAttempted` latch; re-entry → VERIFY_STAGE30_WILLOW_TRANSITION
  30s, then terminal HOLD "Stage30 Willow dialogue did not advance the quest varp;
  no repeated entrance click"; HOLD if Willow absent. No click spam by construction.
- **B75 (−192) "cold restart readiness"**: `dungeonEntryAllowed()` drops the Build-58
  training-probe requirement (style phase DONE + XP gain since probe). Entry now =
  pickaxe>0, hp==maxHp, maxHp>=20, entryFood>=8, plus `guardianActionsAllowed()`
  (CONTROL: allowDungeonEntry && allowGuardianActions && (allowSupervisedDungeonProbe
  || (UNATTENDED_ESCAPE_VERIFIED && allowUnattendedDungeonEntry)) + pid match).
  Comment: the probe is diagnostic history lost on cold restart; must not override
  directly observed readiness.
- **B76 (+1594) "stage30 scene observation"**: CONTROL-gated
  (`allowStage30SceneObservation` + pid/build/sha): when questStage==30 in the cave,
  reads `guardianScene(f)` — guardian/aggressor/hp<max → guardianRetreat + guardianTick;
  else `guardianReadScene(f)` → stage STAGE30_CAVE_SCENE_OBSERVATION. Status telemetry
  gains inDialogue/hasContinue/dialogueText (200ch)/dialogueOptions.
- **B77 (−30) "dynamic instance observation"**: `inKnownCaveInstance` broadened to
  plane 0 && x>=10000 && y>=8000 — comment: "Private-server instance bases changed
  between two observed entries. This is only a candidate context; actions still
  require scene proof." `entranceScene` now delegates to it. Scene-observation
  branch handles dialogue first (inDialogue/hasContinue/options → dialogue()).

## Findings

- **MINOR BIM75-1**: B75's gate relaxation means a cold restart mid-training can
  reach the dungeon entry without the B57 strength-style XP probe ever being
  re-observed this session. Mitigated (pickaxe/hp/food observed + the whole path
  still CONTROL-gated), but the style-setup guarantee is weaker on restart paths
  than B74's gate. Suggest re-running the style probe as part of the cold-restart
  readiness path, or documenting that the supervisor only writes allowDungeonEntry
  post-training.
- **MINOR BIM77-1**: B77's instance-region predicate (x>=10000 && y>=8000, plane 0)
  is very permissive — it will match any content in the 10000+ coordinate block.
  Accepted per the in-code comment (candidate context only, actions need scene
  proof), but any future action gated on `inKnownCaveInstance` alone would inherit
  the breadth.
- INFO BIM73-1/74-1/75-2/76-1/77-2: READMEs byte-identical across B72–B77 (no build
  sections; carried pattern BIM71-2/BIM72-1).
- Carried: MINOR BIM71-1 (stage40ExitProved never cleared → quiesceForReload refuses
  hot-reloads after the stage-40 exit), INFO BIM71-3/4/5/6, MINOR BIM61-1, MINOR
  BIM57-1, INFO BIM68-1, INFO BIM70-1, INFO BIM70-2, INFO BIM72-1, MINOR BIM53-1
  + prior. BIM52-1 (Lumbridge-retreat HOLD) remains RESOLVED by B60.

## Verdict

**PASS WITH FINDINGS** for all five. No ship-blockers. New watch-keys for live
acceptance: `RUNTIME BUILD 73/74/75/76/77`, `STAGE30_CAVE_RECOVERY_LOGIN_ENABLED`,
`STAGE30_CAVE_EXIT_PROVED`, `STAGE30_EMPTY_CAVE_RECOVERY`, `VERIFY_STAGE30_WILLOW_TRANSITION`,
`STAGE30_CAVE_SCENE_OBSERVATION`. Screenshot feed dark ~36h; live acceptance pending
on the stream (outstanding 03:21 re-raise; these watch-keys fold into it).
