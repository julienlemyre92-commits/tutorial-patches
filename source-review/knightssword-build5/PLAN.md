# The Knight's Sword — isolated build handoff

## Account preflight (read only before arming)

- Check native `Quest.THE_KNIGHTS_SWORD.getState(client)` and varplayer 122. If finished, do not start.
- Snapshot account hash, PID, Mining level, HP, combat level, equipment, inventory, bank, coins, and free slots. Mining 10 is needed to mine blurite; train first if below 10.
- Obtain a whole redberry pie, two iron bars, a usable pickaxe, food and armour. Confirm the supplies in inventory after banking or purchase. Do not infer them from a requested withdrawal or GE offer.
- Initial route walks. The installed `Rs2Walker.disableTeleports` flag exists, but it is global; set and restore only while this plugin exclusively owns walking input. Confirm actual route in the live client.

## Native quest route

The installed Microbot Quest Helper maps varplayer 122 to seven values, 0–6. Read the live value and inventory after each interaction; do not advance on a click or elapsed time alone.

| State | Action and proof |
| --- | --- |
| 0 | Squire at (2978,3341,0). Quest varplayer advances to 1. |
| 1 | Reldo at (3211,3494,0). Advances to 2. |
| 2 | Thurgo at (3000,3145,0); give whole redberry pie. Pie consumed and varplayer advances to 3. |
| 3 | Speak to Thurgo again. Advances to 4. |
| 4 | Speak to Squire. Advances to 5. |
| 5 | East castle ladder at (2994,3341,0), west stair at (2985,3338,1), Sir Vyvin's cupboard at (2985,3336,2) while he is outside the bedroom; confirm portrait in inventory. Give portrait to Thurgo with both iron bars; confirm quest state 6. Exact object tile and plane matter where same-ID ladders exist. |
| 6 | Enter the trapdoor at (3008,3150,0), prove underground region (x2979–3069,y9538–9602). Mine a blurite rock near (3049,9566,0), prove ore in inventory. Exit via installed transport candidate ladder ID 17385 at (3009,9550,0), proving surface arrival near (3009,3150,0). Give ore plus bars to Thurgo, prove blurite sword in inventory, and give sword to Squire. Confirm native quest `FINISHED` before logging out. |

## Cave and recovery contract

Ice warriors (57) and giants (53) are aggressive. The account observed before this assignment had HP20/combat18, but these are not current proof. Require a live full food and armour preflight; use HP and food checks during every cave walk and interrupt route to eat or exit when unsafe. Installed `transports.tsv` gives cave entry `1738` at (3009,3150,0) to (3009,9550,0) and reverse exit `17385`, recorded in `../knightssword-cave-transport-evidence.json`. Confirm these objects and both transitions in the live scene before relying on escape. A transport click, apparent proximity, or timeout is not proof. Any repeated failure must rescan scene/collision and choose a different reachable approach or retreat; an unresolved state must stop with diagnostics in a safe place.

On death, reset stale route/action state, re-snapshot inventory and equipment, and perform a bounded grave recovery only after its path is verified. Do not resume the cave from an old pre-death step.

## Implementation boundary

This folder is isolated. Alex owns canonical integration, release, and gameplay. No file here is deployed. `KnightsSwordConfig.preflightOnly()` defaults to true: after native login the script only records account/quest/Mining/HP/inventory/equipment, and does not walk, bank, or quest. The full quest route in `KnightsSwordScript` is experimental and must not be armed until missing-item acquisition, Mining training, armour preparation, death recovery, and live cave crossing are integrated. First host install may need the already-proven `PluginManager.loadPlugins` handoff; subsequent script class updates may use the existing content-hash hot host pattern. Runtime build must be proved by its startup marker and same-PID status, not by JAR filename.

## Sources

- Installed `microbot-tutorial-island.jar`: `QuestHelperQuest.THE_KNIGHTS_SWORD`, `QuestVarPlayer.QUEST_THE_KNIGHTS_SWORD` = 122, and `TheKnightsSword` step map. This is the exact client API baseline.
- Microbot project source: https://github.com/chsami/Microbot/blob/main/runelite-client/src/main/java/net/runelite/client/plugins/microbot/questhelper/helpers/quests/theknightssword/TheKnightsSword.java
- OSRS Wiki quick guide: https://oldschool.runescape.wiki/w/The_Knight%27s_Sword/Quick_guide
