# The Corsair Curse — isolated Build 1 candidate

Owner: Rowan (OSRS BOT MAKER (3)). Alex owns integration, release and live validation. Nothing in this folder was deployed or used to control the client.

## Deliverable

- `CorsairCurse-plugin-1.jar`: full RuneLite/Microbot plugin, config, script and the existing bounded `QuestRandomEventDismiss` helper.
- `CorsairCurse-script-1.jar`: script-only hot-host update for later Build 2+ development. The initial Build 1 requires installing the full plugin once.
- `CorsairCursePlugin.java`, `CorsairCurseConfig.java`, `CorsairCurseScript.java`: source.
- `build1-identity.json`: plugin JAR, script JAR, defining class and compilation-reference JAR hashes.
- `UPSTREAM_TheCorsairCurse.reference.txt`: read-only reference source from Microbot main; not a replacement for this script.

The source was compiled with `javac` against `C:\Users\No 1\Desktop\New folder\microbot-tutorial-island.jar` (SHA-256 `44f1b275839c4eb643b682a8447f0324c6b8853f6da9a355415e38f822fc5051`). Compilation succeeded with two deprecation warnings from the existing random-event helper. This compile result does not prove the plugin loads or the quest completes.

## Quest model and provenance

- Current Microbot [TheCorsairCurse.java](https://github.com/chsami/microbot/blob/main/runelite-client/src/main/java/net/runelite/client/plugins/microbot/questhelper/helpers/quests/thecorsaircurse/TheCorsairCurse.java), read 2026-10-02: progress varbit `6071` with stages `0/5,10,15,20,25,30,35/40,45,49,50,52,55`; investigation varbits `6072` through `6075`; NPC, object IDs, coordinates and room zones. The downloaded reference has SHA-256 `84f0eb7fb71ab94ee4a072ad40584598a010dbb503dd7357aa88c8962de72afa`.
- [OSRS Wiki quick guide mirror](https://osrsindex.com/wiki/the-corsair-curse-quick-guide?site=osrs_wiki) and [OSRS Wiki quest mirror](https://osrsindex.com/wiki/the-corsair-curse?site=osrs_wiki), accessed 2026-10-02: F2P, no quest/skill prerequisites, spade and tinderbox available during the quest, final Ithoi fight level 35 in an instance. He uses magic; death moves unprotected items outside the instance. These sources recommend combat gear, food, and about 20+ combat. Our preflight uses a conservative combat 25 and HP 25 because this account was last recorded at combat 18, HP 20.
- Exact private-server dialogue, object actions, instance mapping and target IDs still require fresh live observation. The code never treats dispatch or a timeout as proof.

## What Build 1 implements

- Off by default. An exact PID, build and defining-class SHA must arm it, and `preflightOnly` must be disabled before `mode=QUEST` can act.
- Native login and welcome handling, account binding, fresh quest/varbit/inventory/position/health snapshots, action journal, one action followed by state proof, bounded stalls, status properties and CodeSource-based class SHA.
- Investigation and finishing path selected from quest progress plus four branch varbits. Movement requests go to the existing shared `NavigationGoal` provider, with fresh arrival checked after provider completion. Pre-quest food preparation goes to `PreparationGoal`; the script does not embed another banker, GE buyer or walker.
- Existing bounded random-event dismissal source is included in the full plugin. Shared death recovery is registered with safe-exit intent and an unknown office-fee refusal. Optional level-up tab behavior is not needed for this quest's actions and is not duplicated here.
- Before climbing to the final level-35 instanced fight: combat 25, HP 25, equipped weapon, at least two armour slots, ten food and at least 100 nominal healing. The spade and tinderbox are collected at their free quest-local sources if absent. Combat checks health each tick, eats with item-count proof and attempts to retreat down the hut stairs when supplies fail. Boss victory is accepted only from quest progress/state.

## Integration blockers and validation gaps

1. **Current account feasibility:** last recorded combat 18 and HP 20 fail the conservative fight preflight. There is no proven shared combat-training provider to satisfy this goal automatically. Alex should either route training through a verified provider or change the threshold only after a live safety assessment; Build 1 will HOLD before sailing.
2. **Gear preparation:** the shared preparation request asks for food and tools, but cannot yet guarantee weapon/armour acquisition and equipping. If absent, Build 1 stops safely after one service attempt. Extend the provider rather than adding a private GE/bank workflow to this script.
3. **Navigation:** service registration and routes through the ship, hut stairs and cavern are unproved in this client. The plugin checks the landing tile/plane after the service returns. Instanced cave and boss-hut mapping may need a minimal shared navigation extension.
4. **Combat/death:** the boss instance, immediate aggro, exit stair, emergency escape and death recovery are not live validated. The current recovery policy never authorizes an unknown office fee. An unproved escape can leave a character exposed; do not arm the quest run until this branch is reviewed against a fresh scene.
5. **Quest steps:** no live test has confirmed this private server's dialogue order or interactive object menu actions. The script uses the upstream helper as a candidate map and holds on an unrecognized option or unproved action.
6. **Build identity:** confirm the actual loaded Microbot JAR, PID, `RUNNING_BUILD=1` and `classSha256` matching `build1-identity.json` before enabling actions. A JAR filename or copy alone is insufficient.

## Alex's first integration pass

1. Keep Knight's Sword and other gameplay plugins disabled. Install the full plugin through the established release path. Do not copy this candidate over a live quest or shared service.
2. Inspect the fresh Corsair status at `~/.runelite/corsaircurse/status.properties` and the hot-host status at `~/.runelite/corsaircurse-hot/status.properties`; verify same PID, build and class hash. Default config `preflightOnly=true` and `allowActions=false` leaves game actions off.
3. Check account quest state, combat/HP, gear, food, service registry and room/boat/cave object actions. Resolve the blockers above before authorizing `mode=QUEST` for a live run.
4. Review each observed failure against the source, make a focused Build 2 script-only JAR and hot-load through the host when the previous action and shared service are quiescent. Keep the quest-state proof and action journal intact.

Build command (no live test was run):

```powershell
javac -cp 'C:\Users\No 1\Desktop\New folder\microbot-tutorial-island.jar' -d work/corsaircurse-next/build1-classes work/questcommon/QuestRandomEventDismiss.java work/corsaircurse-next/CorsairCurseConfig.java work/corsaircurse-next/CorsairCurseScript.java work/corsaircurse-next/CorsairCursePlugin.java
```
