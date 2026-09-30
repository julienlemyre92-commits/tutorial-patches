# Witch's Potion Microbot plugin

Separate RuneLite/Microbot quest plugin, planned as Build 552 in cumulative patch 550. The live account completed Goblin Diplomacy on Build 551 and is at the Goblin Village; Witch's Potion has not yet started.

## Quest state and required proof

- Varp 67 = 0: talk to Hetty (NPC 4619, Rimmington 2968,3205,0); select “I am in search of a quest.”, then “Yes.”
- Varp 67 = 1: acquire rat tail 300, onion 1957, burnt meat 2146 and eye of newt 221, then return to Hetty. The rat tail is available only after starting the quest.
- Varp 67 = 2: `Drink-from` cauldron object 2024 at 2967,3205,0.
- Completion requires `Quest.WITCHS_POTION.getState(client) == QuestState.FINISHED`; a click, empty inventory or timeout is insufficient.

The ingredient route uses a level-1 rat (NPC 2855) near 2956,3203; Pick onion object 3366/5538 in the field near 2950,3253; raw beef 2132 from Wydin (NPC 2890) and eye of newt 221 from Betty (NPC 5905 or older 1788) near 3013,3261 at Port Sarim. Cooking raw beef produces cooked meat 2142; using that cooked meat on a range again deliberately produces burnt meat 2146. The Rimmington range is object 9682 in the house north of Hetty, near the map pin 2971,3211. Its exact tile remains a live-scene discovery item.

## Human-like behavior criterion

Follow [the shared pacing rule](../questcommon/HUMAN_PACING.md): observe state, take one action, prove the result on a later tick, then use a bounded context-aware delay. Select among equivalent reachable targets only. Exact object access tiles must remain deterministic, and unresolved stalls must enter a diagnostic HOLD after bounded attempts.

## Validation status

No Witch's Potion build or live run has been verified yet. The first loaded build must be identified from its explicit `RUNNING_BUILD` marker and matching class hash, not from `version.txt` alone. A complete run must finish without manual gameplay before this plugin can be reported as working.
