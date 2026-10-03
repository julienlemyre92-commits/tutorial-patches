# 2026-10-03 ~07:10 EDT — Corsair Curse Build 44 ACCEPTED live; stream removed mid-observation

Read-only watch note (Muse reviewer). Stream check on the confirmed live Bumba URL
(https://www.youtube.com/watch?v=p-yTeVjh7vU) — 2 successful looks, then the stream died.

## Build 44 live acceptance: ACCEPTED
- Look 1 (~07:04:30 EDT): RUNTIME BUILD marker = **BUILD 44**, LAST BUILD 2 min.
- Look 2 (~07:05:45 EDT): BUILD 44 again. Build 42 never reappeared — supersession clean.
- Inference: client restarted ~07:02 EDT (LAST BUILD 2 min at 07:04:30; Build 44 shipped 07:00:40).
  The hot-load infra gap (client JRE lacks instrumentation module) was resolved via the
  authorized restart fallback. Build 43's failure was infrastructure, not script logic — confirmed.
- Ticking ambiguity: LAST BUILD read "2 min" in both looks ~75s apart; minute resolution can't
  prove the runtime ticked. Bottom bar read "Script paused" in Look 2, but the character MOVED
  between looks (jetty -> aboard ship deck, floor 0 -> 1), so the script is cycling paused <-> brief
  movement, not frozen.

## Live state (Corsair Curse)
- Quest: "The Corsair Curse" In progress (never marked complete). Checkpoints: **4/17 -> 1/17**
  between looks (checkpoint schema is now /17, not /50). Task line: "wait shared service" -> "leave the area".
- Fresh AI-note block: "The conversation did not advance after Continue. The script paused to
  review the dialogue before trying again." + "Waiting on script toggle".
  This is EXACTLY Build 44's dialogue-reconcile scenario (rejected dialogue dispatch + milestone
  advancement -> verify -> REPLAN_RECONCILED_DIALOGUE) live in production. Build 44's gate is
  correctly scoped to what is happening in-game.
- Player: HP 25/25, 13 food carried, floor 0 -> 1. "09 QUESTS COMPLETED" visible; tally 9 quests / 29 QP.
- Chatbox HOLD/ERROR/exception lines: unreadable at screenshot resolution (no exceptions visible,
  none readable). Chat: staff-only (@OG_Bumbaa housekeeping) — no genuine viewer messages.

## Watch items for Alex
1. Checkpoint regression 4/17 -> 1/17: intended re-plan from an earlier checkpoint, or unintended
   state loss? Flag if 1/17 persists without forward progress.
2. "Script paused" + "Waiting on script toggle" persists across looks — operator/Alex must clear
   the toggle for the dialogue-reconcile path to complete its verify cycle.
3. In-game chatbox diagnostics remain unreadable from the stream — any HOLD/REPLAN_ lines are
   invisible to reviewers; keep the chatbox text mirrored in the overlay panel if possible.

## STREAM REMOVED BY UPLOADER (~07:07-07:08 EDT)
- During the wait for look 3, YouTube autoplay left the tab; on return (~07:08 EDT) the page showed
  "Video unavailable — This video has been removed by the uploader." Reload confirmed removal.
- YouTube search for a restarted Bumba OSRS stream found nothing (only an unrelated Dutch
  children's channel). Web search likewise found no replacement broadcast.
- **Live evidence channel is DEAD until Julien provides a fresh stream URL.** Do not treat
  p-yTeVjh7vU as live in any future run.
