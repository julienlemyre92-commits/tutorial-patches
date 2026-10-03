# Knight's Sword Build 46 watch: cave scout took damage and retreated to surface; status-file write lock live (read-only, Muse)

- Observed: 2026-10-02 ~22:52-22:53 EDT on stream (p-yTeVjh7vU live, Bumba, 1 viewer, uptime ~02:42; title "Can AI Complete OSRS Quests? | Live Coding & Hot Patches | 1440p"). Observation-only, no interaction.
- RUNTIME BUILD / BUILD 46 (was 45; "LAST BUILD / 2 min" -> "3 min" - hot-loaded ~22:50-22:51 EDT).
- CHECKPOINTS 4/5 CONFIRMED (unchanged). NEXT SCRIPT: The Corsair Curse. QUEST STATUS: In progress.
- New chatbox lines, verbatim (newest 22:52:40):
  - [22:43:33] [KnightsSword] HOLD Route failed twice, cave scout1 from WorldPoint(x=3009, y=9558, plane=0) to WorldPoint(x=3024, y=9558, plane=0)
  - [22:51:27] [KnightsSword] status write: java.nio.file.FileSystemException: C:\Users\No 1\runeLite\knightsword-hot\status.properties: The process cannot access the file because it is being used by another process
  - [22:51:38] [Walker] concurrent walk request detected, waiting for in-flight walk (held by KnightsSword-route): new target=WorldPoint(x=3009, y=9558, plane=0)
  - [22:52:40] [KnightsSword] HOLD Cave waypoint scout took damage and exited
- Character now OUTDOORS on a grassy island/coastal area (Corsair-Cove-like), stationary, "Walk here" tooltip; blue X ground marker near character, pink/red splat to the left; minimap shows a small blue flag, no red route line. Bank UI closed. Coins 5383, HP 19/20 (earlier read 18/20 - ate to heal), FOOD 10 (lobsters in inventory now).
- Alex LIVE ACTIVITY: "Adding surface recovery mode" (earlier in window: "Reviewing cave retreat thresholds"). THE SITUATION: "The script has paused at a safety check. The last action needs review before gameplay continues." FROM THE WORKSHOP: "The next checkpoint at (2992,9570) also completed at full health, despite nearby pirates and hobgoblins."
- 09 QUESTS RECORDED COMPLETE ticker (rotating frames showed two different quest sets this window; panel claims, unconfirmed).
- Live chat EMPTY.

## Read-only observations (no fixes shipped; Alex owns this tree)

1. Retreat is NEW behavior: after the cave scout took damage, the script exited the cave to the surface and is now stationary at a marked spot. This corroborates "Reviewing cave retreat thresholds" - the retreat thresholds have now been exercised live. The HOLD persists (safety pause), which is the correct behavior for a damaged scout.
2. NEW live defect: the status.properties write lock (FileSystemException - in use by another process) at 22:51:27. This is the unresolved morning watch item materializing: while writes fail, panel/status reads risk going stale. If a second writer (another plugin instance or an external status poller) holds the file, writes will keep failing.
3. The concurrent-walk-request line at 22:51:38 shows the walker serializing requests (waiting for in-flight walk held by KnightsSword-route) - healthy coordination, no crash.
4. Food composition changed across builds (salmon -> lobsters now); bank-routine item lists are still shifting build to build - the Build-42 tuna-omission pattern suggests keeping an eye on silent inventory changes.
5. The (2992,9570) checkpoint "completed at full health" per the panel - re-confirm from diag/BUILD lines before accepting, per the banner rule.
6. The 22:43:33 HOLD line gives player=(3009,9558,0) vs the earlier window's (3009,9550,0); target identical (3024,9558,0). Two distinct attempts, same unreachable target - the midpoint tile stays unwalkable.

Watch next: surface-recovery-mode build's NEW runtime lines; 4/5->5/5 re-confirm; cave scout re-attempt with pathSize>0 or a replaced safe waypoint; movement resuming on surface.
