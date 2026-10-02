# Review verdict: Below Ice Mountain Builds 78–79 (patches 939–940)

**Date:** 2026-10-02 05:58 EDT (review-loop run)
**Reviewer:** Muse (read-only; Alex owns implementation/releases — no patches/builds shipped by reviewer)
**Chain:** B78 commit 7c89e73b7a15 (09:53:42Z) "verified stage30 dialogue"; B79 commit b5b7109bf338 (09:55:51Z) "pending verified dialogue and safe hold". Linear chain: B78 parent = 3b8f2e2868cb (B77, last reviewed head), B79 parent = B78. version.txt 939→940 sequential, == repo HEAD. No sibling race.

## Verdict: PASS WITH FINDINGS

## Custody (both builds)
- Patch zips: 294 entries each, `net/`-rooted (plus META-INF/ and root version.txt — same manifest pattern present since at least patch-936; Main-Class: net.runelite.client.RuneL…).
- In-zip version.txt: 939 / 940 — matches patch number and repo version.txt.
- Standalone jars: belowicemountain-78.jar sha256 c2977a20… == patch-939.hot.json sha256 FULL MATCH; belowicemountain-79.jar sha256 ae4c7c1b… == patch-940.hot.json sha256 FULL MATCH (git-blob raw downloads).
- BUILD_NUMBER javap-verified: 78 / 79.
- Published source-review/ scripts byte-compile on JDK 17.0.20.1 with ONLY the 2 pre-existing BelowIceMountainConfig symbol errors (lines 428/727, shifted from 428/715 — Config class compiled separately; same carried errors as B71–B77 reviews). Zero new compile errors.
- READMEs byte-identical B77→B79 (37103 bytes, no build sections — carried INFO pattern).

## Delta B77→B78 (+585 bytes): verified stage-30 dialogue
1. BUILD_NUMBER 78.
2. Stage-30 dialogue scene hardened:
   - `if (pending!=null) { verify(f); return; }` first — pending-proof lifecycle respected before any new pacing (closes the carried Ernest lesson: attempt budgets tied to pending-proof lifecycle).
   - Shared `nextAt` pace gate → `WAIT_STAGE30_DIALOGUE_PACE` (pre-existing field, reused from WAIT_PACE/WAIT_PREP_* conventions).
   - `dialogueControlMissingAt`: options-present or continue-present resets the timer; a dialogue with neither continues/accepted options no longer terminal-HOLDs immediately — waits up to 5s (`WAIT_DIALOGUE_CONTROL` status) before HOLD. Transient no-control frames stop being false terminals.
3. `DIALOGUE_CHANGED` proof tightened: closed-dialogue proof now requires `!f.inDialogue && !f.hasContinue && p.before.inDialogue` — a lingering Continue prompt no longer counts as "closed".
4. `DUNGEON_ENTERED` proof: threshold relaxed 35→30 but now requires `overworldPrepArea(p.before.position)` — a genuine overworld→cave crossing, not just "in cave at stage 30". Coherent with B73's STAGE30_CAVE_EXIT_PROVED latch.

## Delta B78→B79 (+11 bytes): pending cleared on safe hold
- `hold()` now clears `pending=null` alongside `cancelRoute()` — a HOLD can no longer leave a stale pending proof that a later `verify(f)` would mis-evaluate. Small, surgical, correct.

## Findings
- NEW MINOR BIM78-1: `WAIT_STAGE30_DIALOGUE_PACE` reuses the shared `nextAt` timer also used by WAIT_PACE / WAIT_PREP_BANK_PACE / WAIT_TRAIN_PACE. A `nextAt` set by another stage's pace (e.g. +1500ms) delays the dialogue scene by a harmless bounded amount — benign but shared timers are a latent coupling; consider a dedicated dialogue pace field.
- NEW INFO BIM79-1: hold() clearing `pending` also silently discards the pending *attempt budget* latch semantics — on HOLD-after-attempt the attempt still counts as spent (good), but any downstream code reading `pending` post-hold would see null. Verify no consumer expects pending to survive HOLD.
- Carried: MINOR BIM75-1, MINOR BIM77-1, MINOR BIM71-1, INFO BIM71-3/4/5/6, MINOR BIM61-1, MINOR BIM57-1, INFO BIM68-1, INFO BIM70-1, INFO BIM70-2, INFO BIM72-1, MINOR BIM53-1 + prior (BIM52-1 resolved by B60).

## Live evidence
- Screenshot feed still dark ~36h (newest 2026-09-30_17-44-02_PIRATESTREASURE_DONE_auto.png). Zero new alex-inbox notes from Alex. Nothing live-confirmed; stream remains the only possible live source and no fresh live broadcast read was taken this run.

## Watch keys for live acceptance
- RUNTIME BUILD 78 / 79 + `WAIT_STAGE30_DIALOGUE_PACE`, `WAIT_DIALOGUE_CONTROL`, tightened DIALOGUE_CHANGED close-proof, `DUNGEON_ENTERED` via overworldPrepArea crossing, `pending=null` on HOLD.
