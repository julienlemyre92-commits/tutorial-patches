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

## Build 6 island exit correction

Build 5 proved native login, welcome dismissal, and movement to (1622,4816), then held because object 30109 was not present near the expected island boat anchor after Misthalin Mystery completion. Build 6 checks installed Microbot `Rs2Magic.canCast(LUMBRIDGE_HOME_TELEPORT)` and uses that spell once when the boat is absent. It requires a later mainland world-position transition before considering the exit complete; failed/unavailable casts hold with diagnostics. No guardian actions.

## Build 7 long-route progress correction

Build 6 moved from Lumbridge to (3112,3266), but the 20-second walker segment timer falsely counted two advancing segments as route failures and held. Build 7 stores each segment's starting tile and counts a failure only if Manhattan distance to the target improves by fewer than five tiles. Real consecutive stalls still hold. It does not arm guardian actions or change quest milestones.

## Build 8 Willow dialogue continuity

Build 7 proved the quest varplayer advanced to 10 while Willow's conversation still had Continue pages. Its dialogue location guard only allowed Willow through varp 7, so it held before the conversation could finish. Build 8 allows Willow's existing dialogue through varp 10, still requiring proximity to Willow and per-click dialogue change proof. Checkal/Atlas handling remains separately location-gated.

## Build 9 packed BIM_MAIN stage decoding

Live Checkal conversation changed raw BIM_MAIN from 10 to 40970 (0xA00A) while BIM_CHECKAL changed 0 to 5. The raw varplayer includes high-bit substate flags; the stage is the low byte (10 here). Build 9 branches and guards on the low-byte stage while retaining raw varp in status and proof logs. It allows the current Checkal dialogue to finish, then routes to Atlas. Guardian remains disabled.

## Build 10 Atlas scene dialogue

Build 9 proved Checkal varbit10 and entered Atlas's strongman cutscene at instanced coordinates around (12806,12237). A fresh desktop capture showed Atlas saying “You need to be tough to be a strongman! Show me what you've got! Come on!” while a valid Continue dialogue remained. Build 10 accepts stage10 dialogue inside a 30-tile radius of that observed scene only when Checkal varbit is at least10; it keeps per-click dialogue change proof and holds outside the known scenes. No combat or guardian logic added.

## Build 11 Atlas scene region

After Build10 hot load, observed cutscene world position moved from (12806,12237) to (12867,12275), outside its too-small 30-tile radius; Build10 was not armed. Build11 accepts stage10 dialogue in the instanced 12000–13999 x/y region only with Checkal varbit >=10, while retaining the outdoor NPC checks, one-click proof, and two-failure HOLD. This corrects a moving scene location without granting movement/combat permission there.

## Build 12 Flex action correction

Live Build11 opened the emotes tab and found sprite 2426, but two `Rs2Widget.clickWidget(flex)` mouse clicks returned accepted without a Checkal varbit/dialogue change; it held. Installed `Rs2Widget` bytecode shows that overload only clicks the widget rectangle and returns true regardless of menu execution. Build12 logs widget id/index/parent/bounds/actions, then invokes the installed `clickWidgetFast(widget, index, 1)` CC_OP path once per attempt with later proof. Two unproved actions still HOLD.

## Build 13 Flex client-thread correction

Build12 hit RuneLite's `must be called on client thread` while reading the Flex widget's live fields, before its menu action. Build13 reads widget id/index/parent/bounds/actions and invokes the installed CC_OP click inside `Microbot.getClientThread().invoke`. The same later-tick varbit/dialogue proof and two-attempt HOLD remain.
