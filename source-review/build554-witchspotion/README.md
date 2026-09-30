# Witch's Potion Microbot plugin

Separate RuneLite/Microbot quest plugin. Build 552 in cumulative patch 550 reached Hetty's house from Goblin Village but held outside its wall at (2971,3209,0), with quest varp 67 still 0. Build 553 in patch 551 targets a verified walkable interior tile, then requires live line of sight before talking.

## Quest state and required proof

- Varp 67 = 0: talk to Hetty (NPC 4619, Rimmington 2968,3205,0); select “I am in search of a quest.”, then “Yes.”
- Varp 67 = 1: acquire rat tail 300, onion 1957, burnt meat 2146 and eye of newt 221, then return to Hetty. The rat tail is available only after starting the quest.
- Varp 67 = 2: `Drink-from` cauldron object 2024 at 2967,3205,0.
- Completion requires `Quest.WITCHS_POTION.getState(client) == QuestState.FINISHED`; a click, empty inventory or timeout is insufficient.

The ingredient route uses a level-1 rat (NPC 2855) near 2956,3203; Pick onion object 3366/5538 in the field near 2950,3253; raw beef 2132 from Wydin (NPC 2890) and eye of newt 221 from Betty (NPC 5905 or older 1788) near 3013,3261 at Port Sarim. Cooking raw beef produces cooked meat 2142; using that cooked meat on a range again deliberately produces burnt meat 2146. Build 553 handles the production prompt on both the cooking and deliberate burning steps; those widget identities still require live proof. The Rimmington range is object 9682 in the house north of Hetty, near the map pin 2971,3211. Its exact tile remains a live-scene discovery item.

## Human-like behavior criterion

Follow [the shared pacing rule](../questcommon/HUMAN_PACING.md): observe state, take one action, prove the result on a later tick, then use a bounded context-aware delay. Select among equivalent reachable targets only. Exact object access tiles must remain deterministic, and unresolved stalls must enter a diagnostic HOLD after bounded attempts.

## Validation status

Build 552 loaded with an explicit `RUNNING_BUILD=552` marker and matching class hash, then proved movement to Hetty's house before the wall HOLD. Build 553 has not yet completed a live run. Identify each loaded build from its runtime marker and matching class hash, never `version.txt` alone. A complete run must finish without manual gameplay before this plugin can be reported as working.

## Build 554 focused proof correction
The first unattended Witch's Potion run finished on Build 553, but `drink:cauldron` opened a completion dialogue and then timed out after 12 seconds because the CAULDRON proof accepted only varp/QuestState change. Build 554 treats a newly opened or changed dialogue as proof that the cauldron click landed; the following tick may continue the dialogue. It still requires `QuestState.FINISHED` for final completion. This build has not been validated from a fresh Witch start because the quest is already complete on the available character.
