# Review verdict: Below Ice Mountain Build 81 (patch-942)

**Date:** 2026-10-02 06:12 EDT (review-loop run)
**Reviewer:** Muse (read-only; Alex owns implementation/releases — no patches/builds shipped by reviewer)
**Chain:** commit 4da5d7331ed (10:12:27Z) "Below Ice Mountain Build81 stage35 cutscene continuation"; parent = 4a39759baa (B80 seen.log). Linear. version.txt 941→942 sequential, == repo HEAD. No sibling race.

## Verdict: PASS WITH FINDINGS

## Custody
- Patch zip: 294 entries, all `net/`-rooted (+ META-INF/ and root version.txt — same manifest pattern as patches 936+). In-zip version.txt = 942 — matches patch number and repo version.txt.
- Standalone jar belowicemountain-81.jar sha256 0bb4e5d299c9… == patch-942.hot.json sha256 0bb4e5d299c9b329ea0cbec73983ecb2 — FULL MATCH (git-blob raw downloads).
- BUILD_NUMBER javap-verified: 81 (from the shipped zip's class).
- Published source-review/ script byte-compiles on JDK 17.0.20.1 with ONLY the 2 pre-existing BelowIceMountainConfig symbol errors (lines 428/727 — Config class compiled separately; same carried errors as B71–B80 reviews). Zero new compile errors.
- README byte-identical to B77–B80 (37103 bytes, no build sections — carried INFO pattern).

## Delta B80→B81 (+15 lines): stage-35 cutscene continuation
1. BUILD_NUMBER 81.
2. New block in the `observedQuestStage>=35 || stage30Recovery` branch, after the guardian-emergency-exit check and pending-proof settlement, before the guardian-action gate:
   - Comment: "The cutscene can set stage 35 before its final dialogue closes. Continue only while the room has no live encounter target."
   - Guards: `observedQuestStage==35 && instancedCave && f.hp==f.maxHp && f.unsafeAggressor.isEmpty() && (f.inDialogue || f.hasContinue || !f.options.isEmpty())`.
   - Snapshots `guardianScene(f)`; if `!guardianPresent && pillars.isEmpty()` → pace (`WAIT_STAGE35_DIALOGUE_PACE` on shared `nextAt`) or `dialogue(f)` continue, then return. Dialogue-only; no movement/combat.
   - If a guardian or pillars are present, falls through to the guardian-action gate — encounter correctly takes priority over cutscene continuation.
3. Coherent with the B80 stage-30 dialogue acceptance: cutscene advanced past the dialogue guard again, one stage further. Full-HP requirement (11/11 on the observed account) means no dialogue pressure while damaged.

## Findings
- NEW MINOR BIM81-1: `WAIT_STAGE35_DIALOGUE_PACE` reuses the shared `nextAt` timer (same latent coupling as BIM78-1 for the stage-30 pace). A `nextAt` set by another stage's pace delays the stage-35 scene by a bounded amount — benign, but shared timers remain a coupling; consider a dedicated field.
- NEW INFO BIM81-2: the block's entry predicate requires dialogue controls present (`inDialogue || hasContinue || options non-empty`); a stage-35 dialogue frame with no controls at all falls through to the B78 `dialogueControlMissingAt` 5s timer and then HOLD — the two mechanisms don't conflict, but the 5s wait now applies at stage 35 too.
- Carried: MINOR BIM78-1, INFO BIM79-1, MINOR BIM75-1, MINOR BIM77-1, MINOR BIM71-1, INFO BIM71-3/4/5/6, MINOR BIM61-1, MINOR BIM57-1, INFO BIM68-1, INFO BIM70-1, INFO BIM70-2, INFO BIM72-1, MINOR BIM53-1 + prior (BIM52-1 resolved by B60).

## Live evidence
- Screenshot feed still dark ~36.5h (newest 2026-09-30_17-44-02_PIRATESTREASURE_DONE_auto.png). Zero new alex-inbox notes from Alex. Nothing live-confirmed; no fresh live broadcast read this run.

## Watch keys for live acceptance
- RUNTIME BUILD 81 + `WAIT_STAGE35_DIALOGUE_PACE` and dialogue continuation at stage 35 in the instanced cave with no guardian/pillars present.
