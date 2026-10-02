# Below Ice Mountain — Build 1 handoff

Build 1 is an **observation-only** Microbot plugin. It compiles against the installed `microbot-tutorial-island.jar` and reports the live client and quest preflight. It performs no login, walking, clicks, banking, logout, or combat, even when `allowActions` is enabled. Alex owns integration, release, and live testing.

- Plugin: `belowicemountain-plugin-1.jar`
- Java package: `net.runelite.client.plugins.microbot.belowicemountain`
- Embedded script build: `1`
- Script class SHA-256: `b26a8ec536cc6924f58770203797b40cc9db8ac487a4843612cc206b7aa82284`
- JAR SHA-256: `6029704361854c20d67f35f6ec280d62c982d272b52d3baef23a5856fc842297`
- Runtime status: `%USERPROFILE%\.runelite\belowicemountain\status.properties`. Confirm `pid`, `timestamp`, `build`, and `sha256` against the running client before trusting preflight values.

The preflight reads `Quest.BELOW_ICE_MOUNTAIN`, varplayer `BIM_MAIN` (2951), quest points, Mining, current/max HP, combat level, inventory/equipment pickaxes, food count, login index, and position. It holds if the quest is complete or requirements are missing. The food count is only a rough inventory count; it does **not** prove enough healing for the guardian. A stage of `BUILD1_OBSERVATION_ONLY` never enables gameplay.

The plugin host supports same-PID **script-only** hot reload from `.runelite\belowicemountain-hot`. Later script JARs must contain only `BelowIceMountainScript.class` and its nested classes, match the requested SHA-256 and build number, and expose `quiesceForReload`/`restoreReloadState`. The host rejects other entries and other active quest plugins, including Black Knights Fortress.

## Route research for Build 2

Installed QuestHelper's [Below Ice Mountain helper](https://github.com/chsami/Microbot/blob/main/runelite-client/src/main/java/net/runelite/client/plugins/microbot/questhelper/helpers/quests/belowicemountain/BelowIceMountain.java) gives Willow, Checkal, Atlas, Marley, Burntof, dungeon, and guardian stage sequence. `BIM_MAIN` stages are 0/5/7, 10, 15, 20/25, 30, 35, 40. It does not prove the pillar route. The [OSRS Wiki speedrun guide](https://oldschool.runescape.wiki/w/Below_Ice_Mountain/Speedrun_guide) describes Mining 10 plus four structural pillars as an alternative to fighting the guardian. Installed `ObjectID` exposes structural pillar 41458/41460 and broken pillar 41459, but corner coordinates, object transitions, guardian reachability, and server behavior remain unverified live. Never treat guardian disappearance, an unverified click, or timeout as success; require per-corner object changes and quest-state progress. Low HP and four food on the observed account is unsafe for an automatic boss attempt.

Build 2 should add one action followed by next-tick proof, bounded retries, bank preparation, native login only after explicit arming, collision/door crossing verification, and a separate guardian gate. No route has been built or played in Build 1.
