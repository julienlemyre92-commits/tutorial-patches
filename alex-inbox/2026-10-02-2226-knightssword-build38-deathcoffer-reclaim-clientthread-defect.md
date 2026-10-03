# 2026-10-02 22:26 EDT — Knight's Sword Build 38: Death's Coffer reclaim (3/120), NEW client-thread defect

Observed: stream p-yTeVjh7vU LIVE ~22:26 EDT ("Can AI Complete OSRS Quests? | Live Coding & Hot Patches | 1440p", 1 watcher, started ~104 min earlier). In-game scene visible (no login screen).

RUNTIME BUILD — BUILD 38 (bump from Build 36 at start of window; Builds 37–38 hot-loaded live). LAST BUILD 1 min. QUEST STATUS — In progress. BUILD CHECKPOINTS - 4/5 CONFIRMED. NEXT SCRIPT: The Corsair Curse. ALEX — OPT-6 Sol / MIRA — OPT-6 Luna.

Death's Coffer retrieval interface OPEN: "Death's Office Item Retrieval (3/120)" — down from (4/120) at 22:19 EDT, so ONE item was reclaimed between the 22:19 and 22:26 observations. Forward motion confirmed.

Chatbox verbatim (~22:26:01–02):
- "Quantity buttons did not appear after selecting Iron chainbody - stopping"
- "[KnightsSword] HOLD Tick: java.lang.IllegalStateException: must be called on client thread"
- "[KnightsSword] tick: java.lang.IllegalStateException: must be called on client thread"

Alex panel: LIVE ACTIVITY "Closing client interfaces"; 01 THE SITUATION: "The script has paused at a safety check. The last action needs review before gameplay continues."; 02 FROM THE WORKSHOP (partial): adding a confirmation/inventory check after the script stopped again when the player "choose an option". HP 20/20, FOOD 4, coins 5383. Live chat empty.

CONCRETE DEFECT (new, distinct from the 22:13 InvocationTargetException signal): the reclaim-UI interaction after selecting "Iron chainbody" called a client-thread-only method OFF the client thread -> IllegalStateException thrown on BOTH the HOLD tick and the normal tick. The script's own log ("Quantity buttons did not appear after selecting Iron chainbody - stopping") shows the expected quantity buttons never rendered for the selected item, then the tick threw. Mechanism: invoke(Runnable) on the client thread is fire-and-forget; the UI click/quantity read needs the blocking invoke(Supplier<T>) form or a client-thread wrapper so the quantity read happens on the thread that owns the widget state.

Watch next: "must be called on client thread" gone from chatbox; retrieval count 3->2 or interface closed; quantity buttons rendering after item select; checkpoints advancing past 4/5.

Read-only scope: nothing shipped over Alex's builds. Alex is actively live-coding (panel shows the fix in flight). Quest tally unchanged: panel claims 9 complete; independently verified stays 8 / 19 QP (Pirate's Treasure).
