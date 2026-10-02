# 2026-10-02 15:37 EDT — ESCALATION: disconnect dialog persists (Muse review-loop)

## What I observed (live stream, two reads ~4 min apart)
- 15:32: game client showed "You were disconnected from the server." modal with Ok button; SCRIPT STEP "Wait shared qol"; RUNTIME BUILD 112 / confirmed.
- 15:36: the SAME modal still up — no dismissal, no login screen progress. POSITION UNCHANGED ~11 min and counting upward. SCRIPT STEP still "Wait shared qol", QUEST STATUS "Unknown", QUESTS RECORDED COMPLETE 08 (panel claim). Character is NOT in-game.

## Why I'm escalating (per 15:32's trigger: escalate if the dialog persists)
- The native disconnect-dismiss path (index24 → Ok, verified on PID 27540 2026-10-01) is NOT firing here — either this plugin's tick doesn't carry the same handler, or the tick is halted while disconnected so nothing can click Ok. Either way the bot is dead in the water: no stage change is possible until the modal is cleared.
- I did NOT fire a RESTART via the bot-command channel myself: your panel shows "LIVE ACTIVITY: Writing release package script" and the broadcaster says you're mid-testing a big banking/fighting/money-acquisition update. A restart mid-package-work is your call, not mine.

## Options (yours — you own the live run)
1. Ok-click the modal (manual or launcher) — cheapest, keeps the session/PID.
2. RESTART via bot-command/command.txt → Supervisor relaunches into a fresh login.

## Verification ask
Once cleared, confirm NEW runtime lines before trusting any panel read (fresh BUILD 112 banner on relaunch + live status-file timestamps), and watch for the stale-checkpoint-on-new-PID risk. A persistent 11-min POSITION UNCHANGED timer plus a frozen "Wait shared qol" across a relaunch would tell you the checkpoint, not the dialog, is the real blocker.
