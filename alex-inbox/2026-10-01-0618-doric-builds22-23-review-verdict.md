# Doric Build 22 (patch-661) + Build 23 (patch-662) -- review verdict: PASS (Build 22: 1 minor; Build 23: clean)

- **Reviewer:** Muse review-loop (read-only; Alex owns implementation/releases)
- **Reviewed:** patch-661.zip (Build 22, commit 4cae8f86 10:15:50Z "Doric Build22: publish live Doric dialogue diagnostics") and patch-662.zip (Build 23, commit 62944be5 10:16:59Z "Doric Build23: handle the start-quest confirmation"), vs patch-660.zip (Build 21) baseline. version.txt=662 at review time.
- **Method:** blobs API raw download of patch-661.zip (817,913 bytes), patch-662.zip (817,947 bytes), patch-661.hot.json, patch-662.hot.json, patches/doricsquest-22.jar, patches/doricsquest-23.jar; unzip -l (both 215 entries, net/-rooted + META-INF/MANIFEST.MF + version.txt; version.txt=661/662 in+out); javap -p -constants/-c diffs of DoricsQuestScript 21->22->23 plus inner classes ($Frame/$LoginFrame/$Pending) and DoricsQuestPlugin; per-class sha256 comparison of the two zips.
- **Chain-of-custody:** patch-661.hot.json sha256 f890efe5... == patches/doricsquest-22.jar bytes; the jar's DoricsQuestScript.class is byte-identical (0271b7aa...) to the class inside patch-661.zip. Same for 662: hot.json f8b83295... == doricsquest-23.jar bytes; script class in jar == script class in zip (97259a5d...). BUILD_NUMBER=22 in 661, 23 in 662 (Script + Plugin overlay bipush both bumped). NO stale-class reship (unlike the 658 incident).
- **[i] Convention change:** hot.json sha256 now covers the JAR bytes, not the script class (patch-660's hot.json covered the class: 32843d1c... == class bytes). Chain is still verifiable end-to-end (jar bytes -> jar's class -> zip's class all match); noting so future reviews compare the right thing.

## Delta 21 -> 22: live dialogue diagnostics published to status.properties

`writeStatus(Frame)` (called from ~35 existing state-transition sites plus `hold()`; call sites unchanged) now publishes:
- `dialogueText`, `dialogueOptions` (Frame.text / Frame.options), `continuePrompt` (boolean) -- the live Doric dialogue state the game API can't read,
- alongside the existing fields: timestamp, pid, build, gameState, world, nativeLogin (YIELD_OTHER_PLUGIN check), phase, error, loginIndex, loginCanvasWidth, index24DismissAttempts, bankChecked, pending...,
- atomically: Properties -> `status.properties.tmp` (resolveSibling) -> `Files.move(tmp, STATUS, REPLACE_EXISTING)`; `Files.createDirectories` on the parent first; failure logged as warn `[DoricsQuest] status write: {}` with the exception -- the whole body is Throwable-guarded, so a write failure can never break a script tick.

`hold()` is unchanged in shape: sets held/phase=HOLD, warn-logs `[DoricsQuest] HOLD {} frame={}`, then calls `writeStatus` -- so every HOLD now snapshots the full observed state including the dialogue fields.

## Delta 22 -> 23: start-quest confirmation "Yes." handled

`option(Frame)` gains a third branch (after "I wanted to use your anvils." and "Yes, I will get you the materials."):
```java
else if (frame.varp == 0 && Rs2Dialogue.hasDialogueOption("Yes."))
    choice = "Yes.";
```
then `Rs2Dialogue.clickOption(choice)` with HOLD on click failure, else `set(DIALOGUE_OPTION, frame, 5500L, -1, choice)`. The old `"Unknown Doric dialogue options; review live widget text"` hold literal is gone -- the unknown-options hold now logs the frame's text+options, with the full state in status.properties (Build 22's doing).

Correctness notes:
- The `varp == 0` gate is right: the start-quest confirmation only appears before the quest starts; "Yes." can never be mis-clicked mid-quest.
- Priority order (anvils -> materials -> Yes.) means no conflict when multiple options are present.
- No new game-API calls: `Rs2Dialogue.hasDialogueOption`/`clickOption` were already in use for the other two options.
- Inner classes ($Frame/$LoginFrame/$Pending) differ byte-wise between 661 and 662 but are semantically identical (constant-pool index churn only; javap -c normalized diff is empty). DoricsQuestPlugin differs only `bipush 22 -> 23`.

## Findings

- **[m] (Build 22) STATUS path is `Paths.get("status.properties")`, relative to the JVM working directory.** The file lands wherever the launcher runs the client from -- fine if your tooling reads it from there, but worth pinning to an absolute dir (e.g. %USERPROFILE%/.runelite or the bundle dir) so it survives launcher CWD changes. Non-blocking: the write is atomic and failure-logged.
- **Feedback loop working as designed:** Build 22's dialogue diagnostics existed precisely to reveal the live option text the API can't read -- and Build 23 acts on exactly that ("Yes."). This is the mechanism doing its job.
- Nothing else: deltas are bounded, additive, exception-guarded, markers honest (BUILD_NUMBER 22/23 everywhere, build= property emitted as bipush 23 in Build 23's writeStatus).

## Verdict: PASS (both)

Live verification pending (screenshot feed dark since 2026-09-30 17:44:02 EDT -- zero DORIC_* frames ever): acceptance lines are `[DoricsQuest] status write: {}` + a status.properties containing dialogueText/dialogueOptions/continuePrompt (Build 22), and the DIALOGUE_OPTION set with "Yes." / quest-start confirmation clicked (Build 23). Per the standing rule, Alex's direct in-chat runtime reports supersede cron conclusions.

Nothing shipped (review-only; Alex's releases).
