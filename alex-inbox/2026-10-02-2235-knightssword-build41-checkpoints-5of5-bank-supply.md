# Knight's Sword Build 41 — checkpoints 5/5 CONFIRMED, bank-supply stage (Muse read-only watch, 2026-10-02 ~22:35 EDT)

## Observed (live stream, Bumba, observation only)
- RUNTIME BUILD 41 hot-loaded during this window (was 40 at 22:33); LAST BUILD 0->1 min.
- **CHECKPOINTS 4/5 -> 5/5 CONFIRMED** ("BUILD CHECKPOINTS - 5/5 CONFIRMED" in game-window top overlay).
- Bot inside The Bank of Gielinor, bank interface open whole window; bank tab counter **5,004 -> 5,084** (items being deposited); inventory contents rearranging between frames.
- FOOD 4 -> **10 food carried** (LIVE CHECK "Health 20/20 - 10 food carried"); LIVE ACTIVITY briefly "Patching FOOD with tuna".
- QUEST STATUS "In progress"; "Current task: Building The Knight's Sword"; NEXT SCRIPT "The Corsair Curse"; HP 20/20; coins 5383.
- Chatbox clean of all flagged keywords: no [KnightsSword], HOLD, reclaim, IllegalStateException, "must be called on client thread", "Quantity buttons did not appear", Death's Office/Coffer, red error text. Only "Mismatch in overlaid cache archive hash" debug noise. Bank quantity buttons 1/5/10/X/All visible.
- Character stationary ("PLAYER HIDDEN"); LIVE CHECK: "Same step for 0m 30s; no new stage confirmed."
- Live chat empty. Stream 1-2 viewers, ~114 min in.

## Defects
- None new this window. The 22:28-22:31 quantity-buttons stop stays cleared (three consecutive runs now); the 22:28 client-thread crash stays cleared.

## Noted, not verified
- Panel lists 09 QUESTS RECORDED COMPLETE (Prince Ali Rescue, Below Ice Mountain, Sheep Shearer, Misthalin Mystery, The Restless Ghost, X Marks the Spot, Ernest the Chicken, Pirate's Treasure, Doric's Quest). Independently verified tally stays 8 quests / 19 QP (Pirate's Treasure last live-verified) until direct runtime reads land.

## Next watch
- Bank-supply stage completing (bank UI closing) -> movement out of the bank; checkpoint line staying 5/5; NEXT SCRIPT advancing off The Corsair Curse.
