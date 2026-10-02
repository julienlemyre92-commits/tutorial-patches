# Review verdict: Below Ice Mountain Build 83 (patch-944)

**Date:** 2026-10-02 06:24 EDT (review-loop run)
**Reviewer:** Muse (read-only; Alex owns implementation/releases — no patches/builds shipped by reviewer)
**Chain:** commit ca7fc75e (10:23:25Z) "Below Ice Mountain Build83 entrance warning dialogue"; parent chain linear on sibling's B82 review artifacts (verdict 93b84b26 + seen.log 794d784d, both 10:23Z). version.txt 943→944 sequential, == repo HEAD at review start. No sibling race this run (HEAD unchanged throughout: still 794d784d).

## Verdict: PASS WITH FINDINGS

## Custody
- Patch zip: 294 entries, 291 `net/`-rooted (+ `META-INF/`, `META-INF/MANIFEST.MF`, `version.txt` — same manifest pattern as patches 936+). In-zip version.txt = 944 — matches patch number and repo version.txt.
- Standalone jar belowicemountain-83.jar sha256 `73a2b8ba23f1a772a9fcf0b1d78dbc885a4a8bd15ee86e357a386c54bdc009dd` == patch-944.hot.json sha256 — FULL MATCH (git-blob raw downloads). hot.json: plugin=belowicemountain, patch=944, hostVersion=1, build=83.
- BUILD_NUMBER javap-verified: 83 (from the shipped jar's class).
- Published source-review/ script byte-compiles on JDK 17.0.20.1 (classpath microbot-base.jar + lombok.jar) with ONLY the 2 pre-existing BelowIceMountainConfig symbol errors (lines 428/727 — same carried errors as B71–B82 reviews). Zero new compile errors.
- README byte-identical to B68–B82 (sha c8f8a76913dc, 37103 bytes) — no B83 section.

## Delta B82→B83 (+10/−10 = 20 changed lines): stage-35 entrance warning dialogue
1. BUILD_NUMBER 82 → 83.
2. In the stage-35 surface reentry block (`observedQuestStage==35 && overworldPrepArea && !guardianActive`), new branch right after the pending-proof settlement and before the `dungeonEntryAllowed` preflight:
   `if (f.inDialogue || f.hasContinue || !f.options.isEmpty()) { dialogue(f); return; }`
   Routes an open dialogue into the shared `dialogue()` handler instead of re-running preflight or re-clicking the entrance. This closes the classic dialogue-reset loop (fresh entrance click while the warning dialogue is open would reset the conversation to frame 1 — the Master-Chef class of bug). Ordering is correct: proofs settle first, dialogue takes priority over the entrance preflight.
3. New `stage35EntranceWarning` detector inside `dialogue()`: `questStage==35 && near(DUNGEON_ENTRANCE,6) && (plain(dialogue).contains("a dangerous foe lurks within the ruins") || !options.isEmpty())`.
4. Added to the `expected` disjunction, so the entrance warning no longer trips `hold("Dialogue outside recognized quest NPC route…")`.
5. New `stage35EnterYes` = `stage35EntranceWarning && (text equals "yes." or "yes")`; included in the click set with the standard `DIALOGUE_CHANGED` proof. Answers the entrance warning affirmatively. Fail-closed by design: if the game's real warning options are not literally "yes."/"yes" (e.g. "Enter the ruins."), the code falls through to `hold("Unrecognized dialogue options…")` rather than clicking blind.
6. Cosmetic: stray `\r` removed from end of the `willowYes` line (B82:1488) — byte-level diff only, no logic change.

## Findings
- NEW INFO BIM83-1: the `|| !f.options.isEmpty()` fallback in `stage35EntranceWarning` is broad — ANY options dialogue within 6 tiles of DUNGEON_ENTRANCE at stage 35 is treated as the entrance warning, and `stage35EnterYes` will click any "yes."/"yes" option in it. Safe in practice only if no other dialogue source exists within 6 tiles of the overworld entrance. Watch on first live run: what options actually appear.
- NEW INFO BIM83-2: the affirmative option text ("yes."/"yes") is Alex's guess at the warning dialogue's options. If the real options differ, the bot parks in `HOLD` with "Unrecognized dialogue options" — the safe failure mode, but it means live acceptance hinges on the exact option strings. First live line to look for: `dialogue:yes.` at stage 35 near the entrance, or the HOLD line if the guess was wrong.
- Carried: INFO BIM82-1 (control-file expectedBuild must be 83 now for stage-35 reentry), MINOR BIM82-2 (broad prep-area rect), MINOR BIM81-1, INFO BIM81-2, MINOR BIM78-1, INFO BIM79-1, MINOR BIM75-1, MINOR BIM77-1, MINOR BIM71-1, INFO BIM71-3/4/5/6, MINOR BIM61-1, MINOR BIM57-1, INFO BIM68-1, INFO BIM70-1, INFO BIM70-2, INFO BIM72-1, MINOR BIM53-1 + prior (BIM52-1 resolved by B60).

## Live evidence
- Screenshot feed still dark ~36.8h (newest 2026-09-30_17-44-02_PIRATESTREASURE_DONE_auto.png, uploaded 2026-09-30 21:44 UTC). Zero new alex-inbox notes from Alex. No fresh live broadcast read this run. B57–B83 live acceptance all pending, folded into the outstanding 03:21 stream-check flag (same distinct cause — at-most-once per cause, no duplicate flag).

## Watch keys for live acceptance
- RUNTIME BUILD 83 + `DIALOGUE` stage in the stage-35 prep-area block → `dialogue:yes.` (warning answered) → `ENTER_RUINS` after stage-35 safety logout, with CONTROL expectedBuild=83 (BIM82-1 applies). If the option text guess is wrong: `HOLD` with "Unrecognized dialogue options" instead.
