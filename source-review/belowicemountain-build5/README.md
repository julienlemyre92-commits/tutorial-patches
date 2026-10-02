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

## Build 2 script-only candidate

`belowicemountain-script-2.jar` contains only `BelowIceMountainScript.class` and its nested classes. It compiled against the installed Microbot JAR with exit 0; the only compiler warnings are that the installed `Rs2GroundItem` API is deprecated. JAR SHA-256: `abbc854fddd6853ad7f5385e2286901fceda1246c061ac7a6dbcf168cdd988ce`. Script class SHA-256: `e771cbaea228dcb7aef18b4201f204efb77645e3d43167219c6aaed98cc0767c`.

Build 2 adds native F2P login, a bounded exit via the Misthalin Mystery island boat for the observed `(1637,4817)` starting position, one Falador east bank visit, deposit of unrelated inventory, withdrawal of an available pickaxe/quest ingredients/food, a Barbarian Village bronze pickaxe spawn fallback, and the Willow then Checkal/Atlas/Flex early route. It verifies each click on a later observation and stops after two unproved dispatches or route failures. **It intentionally stops at `BIM_MAIN >= 15` and never enters the guardian arena.** The pickaxe spawn tile and Flex widget remain live validation candidates; a mismatch holds with status rather than guessing another action. The guardian needs a separately reviewed safety gate and per-pillar proof.

Default actions remain disabled. For a same-PID hot load, Alex may arm Build 2 through `%USERPROFILE%\.runelite\belowicemountain\control.properties` after checking live status:

```properties
enableActions=true
expectedPid=<live PID>
expectedBuild=2
expectedClassSha=e771cbaea228dcb7aef18b4201f204efb77645e3d43167219c6aaed98cc0767c
```

The script reads this each tick; all four fields must match. Alex owns the hot request, arming, and live test. No Build 2 gameplay test or deployment was performed here.

## Build 3 integration correction

Build 2 hot-loaded on client PID 40424, but its runtime `classSha256` incorrectly reported embedded Build 1's class hash because `Class.getResourceAsStream` resolved through the parent loader. Build 3 reads the exact `.class` entry from its own class `CodeSource` JAR. It remains action-disabled until the live Build 3 marker reports the compiled class SHA and the control file matches PID/build/class SHA. Build 3 uses the same early quest route and still performs no guardian actions.

## Build 4 integration correction

The first armed Build 3 attempt remained at (1637,4817) and held after two island-boat routes. Fresh screen capture proved a visible logged-in `CLICK HERE TO PLAY` welcome overlay that blocked movement. Build 4 now uses installed `WelcomeScreenEvent.validate/execute`, dispatches once, verifies it closes on later ticks, and holds after a bounded 12 seconds. It clears the previous route HOLD only through a verified hot reload; no guardian logic changed.

## Build 5 integration correction

Build 4 hot reload was rejected by the host because Build 3's route cancellation reference remained non-null after a terminal route HOLD. Build 5 services and clears a finished cancellation before returning on HOLD, preserving the reload host's no-active-action guard. It includes Build 4's native welcome dismissal. A controlled cold restart is needed to install Build 5 because Build 3 cannot quiesce; rearm only against the new PID/class SHA after startup. The guardian remains action-disabled.
