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

## Build 14 Flex panel visibility

Build13 logged Flex sprite2426 at emote container child index96 with bounds y701–749. The visible emote panel in a fresh desktop capture ends above this rectangle. Direct clicks were accepted by APIs but did not change Checkal varbit15. Build14 compares the icon and panel bounds on the client thread, scrolls the panel one notch at a time with later `scrollY` proof, caps at 15 verified scrolls, then invokes Flex when inside the visible panel. Two unproved Flex actions HOLD.

## Build 15 visible Flex mouse click

Build14 scrolled the emotes panel with `scrollY` proof, placing Flex sprite2426 in the visible rectangle; a fresh screenshot showed the Flex tooltip. The `CC_OP` direct action remained unproved twice. Build15 retains verified bounded scrolling and uses installed `Rs2Widget.clickWidget(flex)` only after the icon intersects the panel, on the client thread. A later Checkal varbit or dialogue change is still required, with two-attempt HOLD.

## Build 16 stage 15–30 integration

Build15 proved Flex: Checkal varbit15→40 and raw BIM_MAIN122890→327695 (decoded stage15). It then held because the old dialogue guard did not include stage15 near Checkal. Build16 integrates the helper's source-only Marley/Burntof/Willow/entrance candidate into Alex's current Build15, preserving packed stage decoding, long-route progress, Atlas scene handling, and visible Flex click. New stage actions have later-tick proof and bounded attempts; dungeon entrance requires an additional exact-PID/build/class SHA control gate plus HP>=20, full HP, ten food and a pickaxe. Current account HP11/food4 cannot enter. No guardian action is implemented. Candidate ingredient/supplier assumptions remain live tests, not verified quest progress.

## Build 17 packed stage bit mask

Marley's introduction proved BIM_MARLEY 0→5 while raw BIM_MAIN327695 (0x5000F)→328335 (0x5028F). The low byte became 0x8F, but the quest stage remains 15; bit 0x80 belongs to packed substate. Build17 decodes stage from low six bits (`raw & 0x3f`), preserving stage values through 40 and leaving raw varp in status/logs. It should finish Marley dialogue and route to the cook without changing action permissions.

## Build 18 exact ground-item route

Build17 saw cooked meat in the Barbarian Village ground-item list and two generic `loot(2142,15)` calls returned accepted, but inventory never gained meat; the bot held at (3074,3441), potentially on the wrong side of a Long Hall wall. Build18 reads the exact nearest cooked-meat tile from the installed `Rs2GroundItem.getAll`/`RS2Item.getTile().getWorldLocation()` APIs on the client thread, walks to within one tile (door-aware walker), then invokes `loot(tile,id)` and requires later inventory proof. If the tile is absent or the route/loot fails twice, it holds. No other stage changes.

## Build 19 short-route in-flight grace

Build18 found cooked meat tiles (3077,3441) and (3077,3439), but its route worker returned while the player was still moving from (3078,3435) toward the latter. The old route watchdog counted two immediate segment completions as stalls because it demanded 5 tiles of improvement even for a 4-tile target. Build19 waits 3 seconds after worker completion for in-flight movement and requires only one Manhattan tile of progress when the segment starts within 15 tiles of its target; long routes still require five. Two genuinely stagnant segments HOLD. No supply or guardian permissions changed.

## Build 20 F2P ingredient fallback

Build19 confirmed a true obstruction to the Barbarian Village cooked-meat ground tile (player3077,3437; item3077,3439); two route segments stalled. Build20 integrates OSRS BOT MAKER (2)'s separately compiled source candidate from the exact Build19 baseline. It rechecks bank for ingredients/coins, picks up live kitchen knife/bowl, inspects Wydin shop stock before bounded purchase of bread/raw beef/flour, fills a bowl, makes dough, and cooks one item at a time on the Rimmington range. Inventory/coin/production/shop changes are later-tick proof; unavailable scene/stock, failed action or action budget HOLD. The three-minute stage watchdog is extended to ten minutes only while required Marley supplies are missing, because the F2P route crosses several towns; individual route/action bounds remain. All private-server source locations and menus require live validation. Guardian entry remains gated and unavailable at HP11/food4.

## Build 21 Wydin exit door

Build20 proved Wydin shop open and inventory gains of raw beef2132 and flour1933, then closed the shop. A fresh screen showed the player inside the shop at (3012,3205); walker toward Lumbridge returned unreachable and held after two stationary segments. Build21 queries the nearby Door object only after the purchase state, logs its actual id/tile/actions, clicks Open if offered, and requires the same door tile to change id or expose Close on a later tick before resuming travel. This is one bounded doorway action, not an unverified route success. If no door object/action exists or crossing remains blocked, route still HOLDs. No guardian action.

## Build 22 legacy door-object interaction

Build21 identified Wydin's closed exit Door ID2069 at (3012,3204), but two `Rs2TileObjectModel.click("Open")` dispatches returned accepted without door state change and HOLDed. Build22 retains exact scene/action checks and next-tick door-state proof, but dispatches through installed `Rs2GameObject.interact(2069,"Open",3)` to resolve the live tile object and use Microbot's object interaction path. One focused mechanism change; if the door remains closed after two attempts it still HOLDs.

## Build 23 kitchen route waypoint

Full client log corrected the Build20 diagnosis: `Rs2Walker.walkWithStateUntil` rejected Lumbridge kitchen target (3209,3214) as `target-not-walkable` with nearest walkable (3208,3213), pathSize0. That rejection happened before doorway traversal. Build23 targets (3208,3213) and removes the Wydin door special click so the walker can plan its route. Validate movement from Port Sarim on live PID before inferring any door outcome.

## Build 24 bread dough selection

Build23 reached Lumbridge kitchen, picked up bowl1923 and filled it, each with inventory proof. Combining water and flour opened a three-choice dough production interface; two accepted combine calls yielded no bread dough, so it HOLDed. Build24 verifies that production interface opened, then locates the live Bread dough item widget by item ID and clicks that choice, requiring a later inventory gain. Missing choice or two failed clicks still HOLD with diagnostics.

## Build 25 broad dough choice lookup

Build24 found the production interface but no Bread dough item under its two cooking subtrees. Build25 searches all visible children of the installed cooking and production widget groups for the exact Bread dough item ID before clicking, and logs a bounded widget inventory once if the choice remains absent. It preserves inventory gain proof and bounded HOLD behavior.

## Build 26 visible dough Make choice

Live Build25 dump identified group270 title `What sort of dough do you wish to make?` and three `Make` widgets at children15–17; all had itemId -1, explaining the item-ID lookup failure. Build26 selects the first visible `Make` choice only under that exact title and requires actual Bread dough inventory gain. A wrong product or no change still HOLDs after bounded verification.

## Build 27 nearby Lumbridge range

Build26 proved Bread dough inventory gain, walked to Rimmington and used the range; live cooking overlay showed one burned loaf, and Bread remained zero. Build27 checks for a reachable Cooking range in the Lumbridge kitchen while there and uses it for the next attempt; the established Rimmington range remains fallback. The [OSRS Wiki Lumbridge Castle page](https://oldschool.runescape.wiki/w/Undercook) documents the kitchen cooking range and lower burn rate for certain foods. The script still verifies cooked inventory and does not treat dough consumption as success.

## Build 28 idle reload and range evidence

Build27 upload succeeded but its hot-load was rejected because Build26 had an in-flight route at the instant the host attempted `quiesceForReload`. Actions were then disarmed, leaving Build26 idle. Build28 retains the local-range preference and logs the chosen range ID/tile so the next live attempt can prove which range was used. It is a new artifact hash for the host's one-shot rejection guard.

## Build 29 stage-15 route time budget

Build28 proved Bread and cooked meat, then walked from Rimmington to Marley. At (3088,3469), the stage15 unchanged-signature watchdog HOLDed after three minutes before the sandwich action ran. Build29 gives stage15's multi-town supply/recruitment phase ten minutes regardless of whether supplies are already in inventory. The independent route watchdog, action proof and bounded retries still apply. The bot is already at Marley for the next live check.

## Build 30 Burntof RPS instanced scene

Build29 proved Marley varbit40, Burntof intro varbit5, bought/gave Asgarnian ale, and reached Burntof varbit15. Dialogue moved to a level1 instanced room at (13396,12561); existing overworld-only stage15 guard HOLDed on the rules dialogue. Build30 accepts stage15 dialogue in the bounded 12000–13999 instanced x/y region on plane1 only when Burntof varbit is at least15, and permits the already scripted Rock choice there. The [OSRS Wiki speedrun guide](https://oldschool.runescape.wiki/w/Below_Ice_Mountain%2FSpeedrun_guide) states any rock-paper-scissors option works. Each click remains verified.

## Build 31 Burntof final instance line

Build30 proved the game advanced Burntof varbit15→40 and main quest stage15→20 inside the RPS scene. The remaining line “A deal'sh a deal, I'll help you out” stayed in the same level1 instance and the new stage20 guard HOLDed. Build31 accepts stage20 dialogue in that same bounded instance only with Burntof varbit>=15, so it can finish and return without broadening overworld dialogue handling.

## Build 32 Willow Falador post-scene line

Build31 continued the stage20 instance and returned to Falador at (2956,3368). The live prompt is Willow saying the group should head to the entrance west of Ice Mountain. Stage20 dialogue was previously accepted only at the dungeon entrance and HOLDed on this return line. Build32 accepts dialogue near Burntof's Falador location only when all three crew varbits are40 and stage20 is active, then proceeds to Willow's entrance route.

## Build 33 Willow entrance instance

Build32 reached Willow at the western Ice Mountain entrance, completed the Yes dialogue and proved main stage20→25. The scene moved to (13811,12583,0), with Willow's line “Right, that's everyone gathered.” The overworld-only stage25 guard HOLDed. Build33 accepts stage25 dialogue inside a bounded instanced x/y region on plane0 only when all three crew flags are40. Guardian actions and dungeon-entry authorization remain separate.

## Build 34 entrance scene stage30 dialogue

Build33 advanced the entrance cutscene across moving instanced coordinates and proved main stage25→30 and Checkal varbit40→45. The line “Alright guys! It's safe to come in! I think...” remained at (13864,12647,0), outside the stage25-only dialogue guard. Build34 accepts stage30 dialogue in the same bounded instance with Checkal>=40, Marley40, Burntof40. No movement or combat action is granted by this dialogue change.

## Build 35 advanced crew flags

Live Build34 still HOLDed on that line because the entrance cutscene also advanced Marley and Burntof from40 to45. Build35 requires all three crew flags>=40, while keeping stage25/30, instanced coordinate/plane and dialogue-only restrictions. The exact stage30 values were verified in same-PID status before this change.

## Build 36 pre-Willow dungeon safety gate

Build35 continued the stage30 cutscene from (13864,12647) to a new cave scene at (10313,12745), HP11/11 and food4, then HOLDed. This exposed a safety defect: the old `enterRuins` preflight ran after Willow's dialogue had already placed the player in a dungeon scene. Integrating the helper's separately compiled candidate, Build36 calls the existing exact-PID/build/class-SHA HP/food/pickaxe/dungeon gate before Willow dialogue at stage20/25 and before stage30 cutscene dialogue. It deliberately keeps the current low-HP scene HOLDed. No guardian actions or unverified exit action are added.

## Build 37 stage30 safety logout

Build36 proved the low-HP stage30 cave remains HOLDed at (10313,12745). Build37 uses the installed `Rs2Player.logout()` once only in that bounded stage30 cave scene when dungeon preflight fails, then requires the game state to leave LOGGED_IN within 12 seconds. Either result becomes HOLD, preventing automatic relogin and any guardian action. This is an emergency exit attempt, not proof the account returned outside; position after a later authorized login must be checked separately.

## Build 38 / patch 903
Phase-1 stage20-30 recovery from logged-out dungeon cutscene: after native login, mapped overworld branch walks to Falador bank when entry HP/pickaxe/high-heal food gate is unmet, opens the bank with next-tick proof, records food/coins/gear/pickaxe counts, and HOLDs before Willow/guardian. Does not withdraw, shop, train, or enter cave. Existing stage30 cave safety logout remains first. Compiled against installed Microbot jar; live behavior pending.


## Build 39 / patch 904
After Build38's observed empty-food bank audit, quotes tuna or salmon through installed Microbot GE API and permits one 10-food offer only when positive real-time quote totals <=800 coins. Withdraws only required coins, requires empty GE/free slot, one-shot placement across reloads, verifies item/price/quantity/coin debit and collection inventory gain. HOLDs on missing quote, uncertain offer, unfilled 45s, or after food; no training or Willow/guardian actions. Compiled against installed Microbot jar; live result pending.


## Build 40 / patch 905
Replaces the unproductive installed Microbot GE quote/purchase calls with the existing native QuestGeBuyer widget flow from Prince Ali Rescue, embedded for script-only hot load. At stage20-30 overworld with empty food bank, funds an 800-coin cap, walks GE, uses fresh Wiki quote, durable checkpoint before each UI input, owned offer and inventory/coin/slot proofs, and HOLDs after 10 tuna. Willow/guardian/training remain disabled. Compiled against installed jar; live result pending.


## Build 41 / patch 906
Corrects Build40's early food-count HOLD during native GE item collection. Once native buyer has started, it resumes its durable checkpoint even when ten tuna are already carried and fewer than ten inventory slots remain, then requires the buyer's COMPLETE outcome (coin collection and owned slot clearance) before HOLD. No second offer and no Willow/training actions. Compiled; live completion pending.


## Build 42 / patch 907
After native GE COMPLETE and ten tuna, closes GE with proof, walks Lumbridge chicken farm (3238,3298), attacks only visible/reachable Chicken NPCs one at a time, requires Hitpoints XP gain, waits for combat end, and caps training actions. Unexpected attacker or low HP cancels in-flight action, eats and retreats to Falador bank with HOLD. At max HP>=20 it HOLDs; Willow/guardian still disabled. Compiled; live scene and XP proof pending.


## Build 43 / patch 908
Live Build42 proved first chicken XP1514→1518, then a second accepted attack yielded no XP for30s and HOLD. Build43 records target NPC index; after one unproved attack it excludes that index for60s, rescans other live non-dead chickens and tries a different reachable target. Three unproved targets HOLD. XP proof avoids the just-hit index briefly; target/avoid counters survive hot reload. HP/food/Willow safety gates unchanged. Compiled; live result pending.


## Build 44 / patch 909
Build43 gained HP XP1518→1529 then correctly HOLDed adjacent to a Chicken with LOS=false across the fence. Build44 prefers live chickens with LOS; on near-target LOS failure records all chicken indices/tiles/LOS and nearby pen gate IDs1559/tiles for a route correction. No gate interaction yet; no repeated attack. Compiled; scene probe pending.

## Build 47 / patch 912
Corrects the chicken training route after live Build44 repeatedly returned east to the farm waypoint and then selected a chicken without line of sight through the fence. The farm approach runs only before training begins. Subsequent attacks require fresh line of sight. If no chicken is visible, one bounded walk to the previously observed interior tile (3232,3297) is allowed, followed by a new scan; unresolved visibility holds with diagnostics. The script retains route state across hot reload. It also permits the verified level-up tab acknowledgment while quest actions are disarmed and queues one documented HP level 13→14 catch-up for this session. Compiled against the installed Microbot JAR; live route, XP, and cue proofs pending.

## Build 48 / patch 913
Corrects the level-up cue. The previous implementation proved only that the Skills tab opened and left it selected; a newly leveled skill could still flash. Build48 opens the specific skill guide using the installed `WidgetIndices.SkillsTab` mapping, verifies the guide appeared, closes it, returns to Inventory, and verifies that tab on later ticks. It queues overlapping level increases and migrates the last Build47 tab-only cue once. A failed guide or return is reported as unverified, never as a completed acknowledgment. This release also tightens the dungeon entry food count to exact reserved lobster/tuna/salmon and prepares a one-time dungeon scene probe behind the separate `allowDungeonEntry` control flag. Guardian and pillar actions remain disabled. The cave escape after a failed logout is not proved, so entry must remain disarmed. Compiled against the installed Microbot JAR; live skill-guide and visual flash proof pending.


## Build 49 / patch 914
At a proven safe bank, a previous training retreat no longer blocks the queued level-up acknowledgment. Live PID 5228 opened Attack 19→20 skill guide, closed it, and returned to Inventory; all three runtime states were proved on later ticks. Quest actions and dungeon entry remain disarmed pending Count Check retreat review.

## Builds 50–52 / patches 915–917
Corrects Count Check random event false threat, chooses the nearby Lumbridge Castle bank for a genuine retreat from the chicken area, and resets training approach after safe bank recovery. Build52 loaded on PID5228 and issued a route back to the chicken farm with HP15/15; bank routing and resumed combat need live proof. Dungeon entry remains disarmed.

## Build 53 / patch 918
Read-only all-skill telemetry for the stream overlay. Each status heartbeat reports skill.<SKILL>.xp and .level from a client-thread snapshot for available real skills, plus current HP20 preparation fields. No gameplay decisions changed. Hot-loaded on PID5228 with explicit RUNNING_BUILD=53 class SHA 763b82666244833d808cc74b91b2b6f575d0b024427319e8b33a02427c6f82de, hot JAR SHA 90677b4ac2695e51cbe2338e26ffa7d5c337852710b5845b7f8c65c3eded102a. Attack XP6564→6576 and Hitpoints XP3287→3291 observed while actionsArmed=true, HP17/17 and no error. The separate guardian candidate also bears a local Build53 marker but is undeployed; renumber before integration.

## Build 55 / patch 919
Before the next chicken attack, if the weapon slot is empty, select a carried F2P pickaxe whose wield Attack requirement is met. Issue one Wield action and require the exact weapon ID in the equipment slot plus loss of that ID from inventory on a later tick; an unproved action HOLDs without repetition. The equipped pickaxe still counts as the quest mining tool. Status now reports attackLevel and weaponId. Build55 hot-loaded on unchanged PID5228 at 03:57 EDT, with explicit class SHA 8551a5e5955bde8d10d6c8ee7efb5ac4f4912178b67461cf57cc8e797be5a22f. One Wield item1265 action at 03:57:17 was PROVED at 03:57:18; weaponId=1265 and actionsArmed=true. HP18/18, XP3591. Sustained combat-rate benefit remains to be observed.

## Build 57 / patch 920
Training optimizer probe: Build55 proved the bronze pickaxe was equipped, but did not prove faster HP XP; standard OSRS pickaxes attack in five ticks versus four unarmed. At a safe gap between fights, unequip once with weapon-slot and inventory proof, retain the pickaxe for Mining, open Combat tab once with later proof, and select the dynamically mapped unarmed Aggressive/Kick style once with later COM_MODE proof. One chicken probe must then increase Strength and Hitpoints XP without Attack XP before ordinary training resumes. Phases and baselines survive hot reload, with no replay of a sent action. Compiled against the installed Microbot JAR; private-server style mapping and XP rate remain to be verified live.

## Build 58 / patch 921
Build57 hot-loaded on unchanged PID5228 at 04:21 EDT and proved one bronze-pickaxe unequip at 04:21:19: weapon slot empty and item1265 retained for Mining. It then HOLDed before the style click because `Widget.isHidden()` was called off the client thread. Build58 snapshots the button visibility on the client thread with the dynamic style observation and uses that boolean on the script tick. It preserves the proved unequip phase through hot reload and makes no second unequip request. Build58 hot-loaded at 04:23 EDT with explicit class SHA 785d9e191f80718740fd62c96663da81af5e40c38cbed201f4f1853e464f2c6e. One chicken Strength probe was accepted at 04:23:43 and PROVED at 04:23:44: Strength XP12→24, Hitpoints XP4062→4066, unchanged Attack XP8948, unarmed combatMode1, retained pickaxe. Continued HP XP rate and quest progression still require observation.

## Build 60 focused retreat correction

Build 60 changes chicken-training threat detection after Capt' Arnav triggered a false retreat at full HP in Build 58. It requires a combat NPC interaction plus an observed HP drop to confirm a threat, bounds that evidence to 12 seconds, clears a stale retreat at full HP with ten food, and lets an active retreat route continue without restarting it each tick. Build 57/58's proved unarmed Aggressive/Kick Strength training is preserved. The live client must confirm Build 60's loaded marker and resumed XP before this correction is considered verified. Dungeon actions remain disabled.


## Build 61 random-event dismissal

Build 61 observes a zero-combat NPC targeting the player with an actual Dismiss menu action. At a safe input point it issues one Microbot NPC Dismiss interaction, checks that the same event is gone on a later tick, and permits at most two attempts per event. A failed dismissal logs diagnostics and leaves the main activity running; random events no longer trigger bank retreat merely because they interact with the player. Completion of random-event minigames is deferred. Live proof of the dismiss action awaits the next event; runtime load and ordinary training still need verification.


## Build 62 dismissal proof correction

Build 62 keeps a dismissal attempt in verification for four seconds even on the second attempt. It reports RANDOM_DISMISS_PROVED only when an accepted Dismiss interaction is followed by the targeted event leaving; an event that departs without an accepted interaction is recorded as RANDOM_EVENT_LEFT. This preserves one-action-then-proof semantics. It includes the Build 61 event selection and two-attempt bound.


## Build 63 grinding handoff latency

Build 63 removes the broad isInCombat wait after the Strength combat style is already proved. It still verifies weapon, pickaxe retention, weapon category and combat style every training tick. It removes the extra 350 ms pace after a proved chicken XP action and records elapsed time from the first observed end of chicken combat to the next attack click. The metric is approximate to one observation tick; target movement may add unavoidable time. Use live timings to assess the requested near-1-second handoff. Random-event dismissal from Build62 is retained.


## Build 64 faster observation cadence

With the default 650 ms observation setting, Build64 schedules the script every 250 ms to reduce idle time during training while keeping pending-action proof, input ownership, HP checks and bounded retries. Explicit nondefault tick-delay settings remain honored within 250–2000 ms. Build63 measured 1.1–1.6 seconds from observed combat end to the next target dispatch; the Build64 goal is below one second when another chicken is reachable without walking. Confirm with live CHICKEN_NEXT_CLICK_GAP_MS samples, not a version marker.


## Build 65 retry after busy hot reload

Build64 patch926 was published but the live host rejected its reload because Build63 had an attack action in progress. Build65 keeps the 250 ms default observation interval and records the upper-bound delay from observed combat end through the Microbot attack call return, so the metric includes API dispatch cost. Disarm first, confirm PRELIGHT_ACTIONS_DISABLED and no pending action, then hot-load Build65 on the same PID. Re-arm using the loaded class SHA and compare live timings. Build64 was never the running build.


## Build 66 chicken click timing breakdown

Build66 measures elapsed time from first observed combat end to the start of the Microbot attack call (cquire), from call start to return (pi), and combined 	otal. It retains Build65's 250 ms default observation interval. These are upper-bound timings for the actual menu input; do not claim the input occurred at API return. Disarm and verify the old script quiescent before hot loading to avoid the busy reload guard.


## Build 67 reuse NPC scene snapshot

Build67 collects Chicken NPCs during the existing client-thread scene observation and reuses that same fresh frame for training target selection. It removes the second NPC-cache client-thread query after each kill. The previous Build66 timing breakdown measured acquisition at roughly 1.1–1.5 seconds and the Microbot attack call at roughly 0.4 seconds. This change targets acquisition latency; compare fresh acquire/api/total samples and XP before judging improvement. All LOS, alive, distance, target-avoid and proof guards remain.


## Build 68 target-rescan recovery and timing diagnosis

Build67's frame-created NPC models increased acquisition time and one attack was rejected at HP XP4459. Build68 returns to the prior proven NPC cache query, bounds a rejected chicken dispatch to three distinct target attempts instead of immediately holding on the first, and logs style/list/LOS/walk/guard timing to locate the remaining target selection cost. Do not repeat a rejected target within 60 seconds. Build67 was stopped and disarmed before this release. A no-error Build68 load and fresh XP are required to resume validation.


## Build 103 / patch 966: guarded stage35 GE sell-form probe

Build102/PID24328 held safely at the Lumbridge bank with zero high-heal food and seven bank coins; full bank audit showed three uncut sapphires. The installed Microbot GE price helpers return constant zero, so a live private-server sell-form observation is required. Build103 adds a PID/build/SHA-bound llowStage35MarketProbe route that withdraws exactly one spare sapphire, proves two remain banked, opens the GE, observes an empty sell slot and selected-item form, then HOLDs without placing an offer. The checkpoint is account/PID/build/SHA-bound and records each input phase before dispatch; uncertain bank/GE actions are not repeated. Cave controls remain disarmed. The form, quote, prompts, fees and any trade restriction require live verification before a sale. Candidate source compiled against the installed Microbot JAR; runtime validation pending.

## Build 104 / patch 967: read-only GE slot widget recovery

Build103/PID24328 proved login, one sapphire withdrawn (two retained in bank), route to GE and visible empty offer slots, then HELD before a sell click because the expected Create Sell offer child action was absent. Build104 recognizes only that exact same-account/PID/Build103/SHA/OVERVIEW checkpoint, dumps GE slot and child widget IDs/text/actions read-only, then HOLDs. It does not repeat a bank, GE, offer or sale input. The failed three-minute upload target followed the selector mismatch; this diagnostic release aims to identify the host's real slot action before a corrected click build.

## Build 105 / patch 968: observed GE sell-slot correction

Build104's same-PID read-only dump at the visible GE overview showed child 4 of each EMPTY slot has Create <col=ff9040>Sell</col> offer; child 1 has no action. The installed GrandExchangeWidget helper is stale for this client. Build105 normalizes markup in the exact action check, targets child 4, and migrates only the account/PID/SHA-bound Build103 OVERVIEW checkpoint after proving the current GE and one carried sapphire. It advances the phase durably before any sell-form click and still stops before offer confirmation. The separate QuestGeSellWidgetAdapter uses the same observed child. Live form and default-price evidence pending.

## Build 106 / patch 969: read-only GE inventory widget diagnosis

Build105 corrected the live sell slot to child4 and opened the sell form, but its normal Rs2Inventory action list did not contain Offer. Build106 recognizes only the exact Build105 account/PID/SHA/SELL_FORM_SENT checkpoint with the visible form and one carried sapphire, dumps widgets 467 and 149 including dynamic item children, then HOLDs without an item Offer click or sale. Installed WidgetID identifies 467 as GRAND_EXCHANGE_INVENTORY_GROUP_ID; live item/action proof remains pending.

## Build 107 / patch 970: observed GE inventory Offer widget

Build106's live same-PID read-only dump proved the single carried sapphire is dynamic child 3 of widget 467:0, with item ID1623, quantity1 and primary Offer action. Normal Rs2Inventory actions did not expose it. Build107 uses the exact visible GE inventory item widget, migrates only the account/PID/SHA-bound Build105 SELL_FORM_SENT checkpoint after re-proving form/item/retained-bank count, and observes selected item on a later tick. It still has no sale confirmation path. The separate GE seller adapter uses the same observed widget group.

## Build 108 / patch 971: bounded one-sapphire sale

Build107 held on a selected sapphire sell form with quantity 1, displayed price 206 coins, EMPTY slot 0, and two other sapphires retained in the bank. Build108 requires that exact same-account/PID/Build107/SHA/HOLD checkpoint and a separate `allowStage35MarketSale=true` control bound to Build108's loaded class SHA. It persists a sale checkpoint before a single Confirm, verifies an owned offer and inventory debit, then checks fill, collection, actual positive coin receipt, and slot clearance. Unknown controls or uncertain outcomes HOLD without repeating Confirm. The cave remains disarmed. Compilation and packaging passed; live sale results remain unverified.

## Build 109 / patch 972: read-only owned-offer recovery diagnosis

Build108 placed one owned sapphire sale, then the offer remained unfilled. A single View attempt did not prove the detail pane, so it stopped at a durable `VIEW_SENT` checkpoint. Build109 reads only that exact Build108 account/PID/SHA/slot/item/phase checkpoint, records native offer state and the current GE widget tree once, and HOLDs. It makes no sale, abort, collect, or repost input. The user's fast-sale policy is a 20-second fill threshold; future repricing requires proven abort, return collection, and a price floor before a single lower-price repost.
