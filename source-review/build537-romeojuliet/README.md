# Romeo and Juliet Microbot quest plugin

Build 537: live test correction for Romeo's post-acceptance information menu. Build 536 proved varp 144 from 0 to 10, then held on that three-choice menu. Build 537 selects only `Ok, thanks.` while near Romeo at varp 10. The quest plugin has not completed a live run yet.

Use installed Quest Helper for varp 144 stage mapping and live evidence for each interaction. The script must run as a separate RuneLite/Microbot plugin, retain the existing Supervisor and stream layout, and prove `Quest.ROMEO__JULIET == FINISHED` before reporting DONE.

Follow [natural quest behavior](../questcommon/HUMAN_PACING.md): one action followed by observed proof, bounded context-aware pauses, safe reachable target choice, and a diagnostic HOLD on unresolved stalls. Build 535 remains the verified Rune Mysteries baseline while Romeo and Juliet is tested.
