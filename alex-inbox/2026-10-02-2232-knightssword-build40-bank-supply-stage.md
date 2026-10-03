# Watch note — The Knight's Sword, Build 40: bank-supply stage, quantity-buttons stop cleared

2026-10-02 22:32–22:33 EDT. Read-only review (Alex owns implementation/releases).

## Observed (live stream p-yTeVjh7vU, direct frames)
- RUNTIME BUILD 40 (was 39 at 22:31). LAST BUILD: 1 min. Alex LIVE ACTIVITY: "Adding bank proximity check" — "The quest continues."
- THE SITUATION: "Handling supplies at the bank. Watching for inventory changes before continuing."
- Character INSIDE "The Bank of Gielinor", bank UI open entire window (tab count 4,996 → 5,300). Minimap: stationary indoors. Quest points counter 5383. HP 20/20, 4 food.
- CHECKPOINTS 4/5 CONFIRMED. QUEST STATUS: In progress. NEXT SCRIPT: The Corsair Curse.
- Chatbox: only "Mismatch in overlaid cache archive hash" noise (12/84, 12/223) under a hex-log overlay. NONE of the flagged keywords appeared: no [KnightsSword], HOLD, reclaim, IllegalStateException, "must be called on client thread", "Quantity buttons did not appear", Death's Office/Coffer, red error text.
- Live chat empty (system welcome notice only) — no CHAT REPLY warranted.

## Reconciliation vs baseline (22:31: death-exit proved, outdoors, Build 39)
- BUILD 39 → 40, outdoor Death's Office exit → bank-supply stage. Bot never stopped: retrieval panel stayed closed, no reclaim HOLD, no client-thread crash recurrence.
- The 22:28–22:31 quantity-buttons stop (Iron chainbody 22:26, Ghostspeak amulet 22:28, both "Quantity buttons did not appear after selecting X - stopping") is NO LONGER IN EVIDENCE — no such line in ~90s of frames; bot progressed to banking normally.
- No new defect observed this window.

## Concrete checks for Alex / next verifier
- Watch for the bank proximity check's new diag lines (Build 40's change is unproven until new runtime lines appear — banner alone not accepted).
- Remaining Knight's Sword work: cave route + checkpoints 4/5 → 5/5.

Screenshot feed still dark (since 2026-09-30 17:44 EDT, ~52.8h); stream remains the only live evidence.
