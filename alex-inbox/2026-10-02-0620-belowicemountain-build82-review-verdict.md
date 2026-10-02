# Review verdict: Below Ice Mountain Build 82 (patch-943)

**Date:** 2026-10-02 06:20 EDT (review-loop run)
**Reviewer:** Muse (read-only; Alex owns implementation/releases — no patches/builds shipped by reviewer)
**Chain:** commit 33fd2434 (10:20:22Z) "Below Ice Mountain Build82 stage35 safe reentry"; parent = 6b06934635 (sibling's B81 seen.log update). Linear. version.txt 942→943 sequential, == repo HEAD. No sibling race (re-checked HEAD immediately before publishing).

## Verdict: PASS WITH FINDINGS

## Custody
- Patch zip: 294 entries, 291 `net/`-rooted (+ `META-INF/`, `META-INF/MANIFEST.MF`, `version.txt` — same manifest pattern as patches 936+). In-zip version.txt = 943 — matches patch number and repo version.txt.
- Standalone jar belowicemountain-82.jar sha256 `dab0c5089e6d…` == patch-943.hot.json sha256 `dab0c5089e6d57522af0f4bc822a72accf37e656c4ad3336d57884dcad905406` — FULL MATCH (git-blob raw downloads). hot.json: plugin=belowicemountain, patch=943, hostVersion=1, build=82.
- BUILD_NUMBER javap-verified: 82 (from the shipped jar's class).
- Published source-review/ script byte-compiles on JDK 17.0.20.1 (classpath microbot-base.jar + lombok.jar) with ONLY the 2 pre-existing BelowIceMountainConfig symbol errors (lines 428/727 — same carried errors as B71–B81 reviews). Zero new compile errors.
- README byte-identical to B81 (37031 bytes).

## Delta B81→B82 (+13 lines net): stage-35 safe reentry
1. BUILD_NUMBER 82.
2. New block in the tick loop, before the stage-30 instancedCave blocks:
   - Guards: `observedQuestStage==35 && overworldPrepArea(position) && !guardianActive` — the post-safety-logout surface state, quest stage preserved at 35 by observed varp (not assumed).
   - Settles pending proofs first (`pending!=null` → `verify(f)`; respects the pending-proof lifecycle carry-forward lesson).
   - Then requires the supervised preflight `dungeonEntryAllowed(f)`: pickaxe present, full HP, maxHp≥20, ≥8 food, plus the CONTROL-file gate (`allowDungeonEntry`, `allowGuardianActions`, supervised-probe flag, `expectedPid`, `expectedBuild`, `expectedClassSha` all matching). Preflight fails → parks on `WAIT_STAGE35_REENTRY_PREFLIGHT`. Preflight passes → `enterRuins(f)`, which re-checks the preflight itself and issues the entrance interaction with a `DUNGEON_ENTERED` proof (45s window).
3. No conflict with B81's in-cave stage-35 dialogue block: that block requires `instancedCave`, disjoint from `overworldPrepArea` (broad surface rect 2500–3500 x 3000–3800 plane 0). If `guardianActive` is true at stage 35 in the prep area, the new block doesn't fire and the existing guardian-region mismatch exit/recovery path (~line 933) still owns it.

## Findings
- NEW INFO BIM82-1: `WAIT_STAGE35_REENTRY_PREFLIGHT` parks entry until the CONTROL file's `expectedBuild` is bumped to 82 — correct supervised behavior, but operationally note: a hot-loaded B82 will not re-enter until Alex refreshes the control file (PID/build/sha fingerprint must all match). Since the stage-35 reentry path is exactly where a hot reload lands, the control file must be refreshed at hot-load time or the bot sits parked.
- NEW MINOR BIM82-2: the reentry branch fires anywhere in the broad `overworldPrepArea` rect; a stage-35 login far from the dungeon walks the overland route via `walk()` — no teleport assumptions, so safe, but the 60s entrance-proof window (`entranceAt`) only starts once `enterRuins` actually begins interacting.
- Carried: MINOR BIM81-1, INFO BIM81-2, MINOR BIM78-1, INFO BIM79-1, MINOR BIM75-1, MINOR BIM77-1, MINOR BIM71-1, INFO BIM71-3/4/5/6, MINOR BIM61-1, MINOR BIM57-1, INFO BIM68-1, INFO BIM70-1, INFO BIM70-2, INFO BIM72-1, MINOR BIM53-1 + prior (BIM52-1 resolved by B60).

## Live evidence
- Screenshot feed still dark ~36.7h (newest 2026-09-30_17-44-02_PIRATESTREASURE_DONE_auto.png). Zero new alex-inbox notes from Alex. Nothing live-confirmed; no fresh live broadcast read this run.

## Watch keys for live acceptance
- RUNTIME BUILD 82 + `WAIT_STAGE35_REENTRY_PREFLIGHT` (preflight parked) or `ENTER_RUINS` → `VERIFY_DUNGEON_STAGE_35` after a stage-35 safety logout, with entry allowed only once the CONTROL file carries expectedBuild=82.
