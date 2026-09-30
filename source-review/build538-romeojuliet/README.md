# Romeo and Juliet Microbot quest plugin

Build 538: Juliet's potion conversation entered an instanced cutscene during Build 537's live run. A dialogue gap fell through to normal pathing and held because the instance is outside the canonical Juliet room. Build 538 recognizes the instanced scene at varp 50/60, advances visible dialogue, waits through animation gaps, and holds with diagnostics if the scene does not progress. It still requires a verified varp and finished quest state.

Build 538 completed one multi-build development run on RuneLite PID 28120 in World 308. The final client state was `QuestState.FINISHED`, varp 144=100, and `DONE`; the 2026-09-30 02:31:23 screenshot shows the congratulations scroll. Earlier quest stages used Builds 536 and 537 before Supervisor resumed on Build 538. A fresh single-build run and a second fresh run have not been validated because no quest reset or new unstarted character is available.

Use installed Quest Helper for varp 144 stage mapping and live evidence for each interaction. The script must run as a separate RuneLite/Microbot plugin, retain the existing Supervisor and stream layout, and prove `Quest.ROMEO__JULIET == FINISHED` before reporting DONE.

Follow [natural quest behavior](../questcommon/HUMAN_PACING.md): one action followed by observed proof, bounded context-aware pauses, safe reachable target choice, and a diagnostic HOLD on unresolved stalls. Build 535 remains the verified Rune Mysteries baseline while Romeo and Juliet is tested.
