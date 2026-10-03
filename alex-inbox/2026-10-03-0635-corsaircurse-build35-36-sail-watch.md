# 2026-10-03 06:35 EDT — Corsair Curse Builds 35/36 static review + live watch

## Live verification (stream, Bumba channel, "Can AI Complete OSRS Quests? | Live Coding & Hot Patches | 1440p", confirmed LIVE ~06:30:50–06:35:00 EDT, 5 looks)
- Runtime marker: BUILD 34 (06:30:50) -> BUILD 35 -> BUILD 36 (06:35). Both shipped DURING the window:
  - Build 35 (68364a48, 06:30:57 EDT, patch-1080)
  - Build 36 (fb00b331, 06:34:44 EDT, patch-1082)
  - Provider hot artifacts patch-1081 (06:33:41–06:33:45): "Bank route chooses policy-verified F2P walk"
- QUEST STARTED: the 06:28 "Start The Corsair Curse quest?" modal is GONE. Captain Tock dialogue ("Now, I've a ship moored west of Rimmington...") shown with "Click here to continue" -> bot sailed to Corsair Cove AUTONOMOUSLY. Quest progress ~15/50 checkpoints (workshop note: "The bot sailed to Corsair Cove on its own — quest progress is now 15, with full health"). Build 33/34 modal fix ACCEPTED.
- New state at end of window: "Sailing succeeded, but the route to the first building was rejected." Player on wooden ramp/jetty at Corsair Cove (numbered path overlay 30-56, "Changing the ramp approach"). GAME ITERATION: "Current task: wait shared service." Effectively paused/waiting.
- Chatbox: java.lang.IllegalStateException: 'Quest action or shared service still owns input' (adjacent to "Mismatch in overlaid cache archive hash for 12/223" — cache noise, not fatal).
- Vitals: HP 25/25, 13 food (cooked fish), combat 3, "09 QUESTS RECORDED COMPLETE" (Restless Ghost, X Marks the Spot, Ernest the Chicken, Sheep Shearer, Pirate's Treasure, Prince Ali Rescue, Below Ice Mountain, Misthalin Mystery, Doric's Quest). No combat.
- Live chat: zero genuine viewer messages (only @OG_Bumbaa owner housekeeping). No CHAT REPLY.

## Static review — Build 34 -> 35
- New handler: progress==5 + dialogue.contains("ship moored west of rimmington") -> clickVisibleContinue(). Directly targets the observed Tock dialogue stall frame.
- New clickVisibleContinue(): client-thread widget find of "Click here to continue" + physical Rs2Widget.clickWidget — not a hotkey; satisfies the physical-click rule.
- Stall recovery: dialogue:continue plan repeated exactly once (n==1) with unchanged continue prompt, empty options, same progress AND same dialogue text -> re-issue via widget fallback. Gates are tight; the equality conjunctions prevent blind re-clicks.
- Caveat: dialogue substring read assumed correct; live behavior corroborates (dialogue completed, sail succeeded).
- Verdict: PASS.

## Static review — Build 35 -> 36
- Single change: ITHOI_RAMP pt(2531,2833,0) -> pt(2531,2834,0) — 1-tile anchor nudge matching the observed ramp approach stall.
- Verdict: PASS (trivial, targeted).

## Open questions / new alert for Alex
- IllegalStateException 'Quest action or shared service still owns input' — input-ownership stall after sailing; distinct from the fixed dialogue stall. Root cause unobserved; one-line message only.
- "Route to the first building was rejected" -> patch-1081 swapped the provider route to a policy-verified F2P walk (bank route). Watch next run: does the ramp walk proceed or re-reject?
- "Wait shared service" persists as current task — verify this isn't a silent infinite wait on the next stream check.
