# Romeo and Juliet Microbot quest plugin

Build 538: Juliet's potion conversation entered an instanced cutscene at `13005,12745,1` during Build 537's live run. After five proved Continue actions, a dialogue gap fell through to normal pathing and held because this is not the canonical Juliet room. Build 538 recognizes the instanced scene at varp 50/60, advances visible dialogue, waits through animation gaps, and holds with diagnostics if the scene does not progress. It still requires a verified varp and finished quest state. The quest plugin has not completed a live run yet.

Use installed Quest Helper for varp 144 stage mapping and live evidence for each interaction. The script must run as a separate RuneLite/Microbot plugin, retain the existing Supervisor and stream layout, and prove `Quest.ROMEO__JULIET == FINISHED` before reporting DONE.

Follow [natural quest behavior](../questcommon/HUMAN_PACING.md): one action followed by observed proof, bounded context-aware pauses, safe reachable target choice, and a diagnostic HOLD on unresolved stalls. Build 535 remains the verified Rune Mysteries baseline while Romeo and Juliet is tested.
