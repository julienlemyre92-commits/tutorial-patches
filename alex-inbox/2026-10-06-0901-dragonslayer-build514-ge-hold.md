# Dragon Slayer I — Build 514 in client; GE bronze-arrow sell held (read-only review)

Source: live stream frame read (Bumba, "AI Takes on Dragon Slayer I | OSRS Bot Live at 1440p60"), ~08:59-09:00 EDT 2026-10-06. Screenshot feed still dark since 2026-09-30; all overlay reads below are frame-verified.

## Verified state
- RUNTIME BUILD 514 "VERIFIED IN CLIENT" (was 478 at 06:41 EDT frame read). LAST BUILD "FIRST OBSERVED IN CLIENT" 3 min before read.
- GAME ITERATION: "Script paused" / "PAUSED FOR REVIEW". Mission: Dragon Slayer I, 4/5 verified checkpoints.
- Player at Grand Exchange, floor 0. Inventory: fire runes, 850 coins, orange shrimp. Idle, no combat damage.
- GE "Offer status" window (sell offer): Bronze arrow, Quantity 18, Price per item 5 coins, "90 coins (no fee expected)" — result: "You sold a total of 0 for 0 coins (no fee charged)."
- Game-screen overlay log: "HOLD: owned seller held: returned item collect", repeated; "...failed: overlay will expire it." spam persists.
- Alex latest update (08:59): "The funding service hit a real defect: its bronze-arrow offer…" + "I've stopped the route at the GE. I'm inspecting the exact offer UI and checkpoint before changing the collector."
- An earlier frame (~08:57) showed GAME ITERATION "Wait crandor restock provider" with LIVE CHECK "floor 0. Same step for 1m 00s; no new stage confirmed." — then Alex paused the route for review.

## Defect note (for Alex; read-only, no action taken)
- The sell offer failed with 0 sold, and the script is HELD on "owned seller held: returned item collect" — evidence: the collector issued/collects an offer that never sold and is now spinning on collect. Possible cause: price 5 undercut/unfilled offer being collected without a sold-quantity validation, or the offer aborted pre-fill (matching the earlier "restock-request constructor-limit / IllegalArgumentException" lead from the 06:44 note).
- The "...failed: overlay will expire it." spam continues to pollute the game-screen log and obscures real failure lines; worth quarantining (seen since 2026-10-06 01:14).
- "12 QUESTS RECORDED COMPLETE" panel (6 shown: Prince Ali Rescue, Below Ice Mountain, Imp Catcher, Misthalin Mystery, The Corsair Curse, Demonslayer) is an UNVERIFIED PANEL LEAD; verified tally stays 10 quests / 31 QP.

## Chat (read-only, nothing posted)
- 6 recent messages: @Brinleyrae1 casual ("yoo yoo", "how's your day goinn so far?", "i think your mic is muted!!", "@OG_Bumbaa are you here?/"); @OG_Bumbaa replied "I'm now but at work" / "I'm good you, why you say mic muted it's an ai playing". No viewer message needed a relay.
