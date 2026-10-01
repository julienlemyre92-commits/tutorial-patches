# Prince Ali Rescue Build1 handoff

**Status:** Compiled candidate only. Alex owns integration and live testing. No deployment, client action, or quest run was performed.

## Artifact and entry points

- Package/plugin id: `net.runelite.client.plugins.microbot.princealirescue` / `princealirescue`.
- Selectable class: `PrinceAliRescuePlugin`; script: `PrinceAliRescueScript`; embedded marker: `BUILD_NUMBER=1` and startup log `[PrinceAliRescue] RUNNING_BUILD=1 pid=...`.
- Full class overlay: `princealirescue-plugin-1.jar`, SHA-256 `0B1BF8A63074EBE5E1458B7BFCA700BE9FE09266AAB538223BF44775DCAA63FF`. Contains exactly `PrinceAliRescueConfig.class`, `PrinceAliRescuePlugin.class`, `PrinceAliRescuePlugin$1.class`, `PrinceAliRescueScript.class`, `PrinceAliRescueScript$Frame.class`, `PrinceAliRescueScript$Pending.class` in the package path. No manifest change.
- Script-only hot artifact: `princealirescue-1.jar`, SHA-256 `3BDA13FF2C84B531401DD1C2A8565DE95418B7C36738F3DC137E2454F739E2A3`; three `PrinceAliRescueScript*.class` files, no manifest. Hot host directory is `.runelite/princealirescue-hot`.
- Compiled with JDK 17 against `C:\Users\No 1\Desktop\New folder\microbot-tutorial-island.jar` (success, nine deprecation warnings for old Rs2Npc/Rs2GameObject APIs). The full overlay is **not** a cumulative updater patch; Alex must integrate it with the established release feed.

## Route and static evidence

The installed `PrinceAliRescue.class` QuestHelper was decompiled into `reference/.../PrinceAliRescue.java`. It maps quest varp **273** to stages `0 Hassan → 10 Osman → 20 preparation/key → 30–33 Joe's beers → 40 Keli rope → 50 cell door/Prince Ali → 100 Hassan`. The script uses `Quest.PRINCE_ALI_RESCUE.getState(client) == QuestState.FINISHED` alone for completion; varp is diagnostic/routing evidence.

The helper identifies Hassan `4285` at `(3298,3163)`, Osman `4286` at `(3286,3180)`, Ned `4280` at `(3097,3257)`, Aggie `120` at `(3086,3257)`, Lady Keli `11578` at `(3127,3244)`, Leela `4274` at `(3113,3262)`, Joe `11577` at `(3124,3245)`, Ali `11579` at `(3123,3240)`, cell door `2881` at `(3123,3243)`. Required item IDs and dialogue choices are copied from that installed helper. The [RuneHQ guide](https://oldschool.runehq.com/oldschoolquest/prince-ali-rescue) and [Gleason guide](https://danielpgleason.com/osrs/quests/prince-ali-rescue/) corroborate the rescue sequence.

Installed API signatures checked with `javap`: `Rs2Inventory.combine`, `useItemOnNpc`, `useItemOnObject`, `Rs2Bank.hasBankItem/withdrawDeficit/depositX`, `Rs2Npc.interact`, `Rs2Walker.walkTo`, `LoginManager.login(int)`, `WelcomeScreenEvent.validate/execute`. `Rs2Player.getHealthPercentage()` is 0–100 in this JAR. The plugin uses the native login profile and welcome event, one action then later observation, bounded route retries, exact unrelated-item banking when inventory is full, and a stop/HOLD on unverified interactions or low health.

## Known live uncertainties and acceptance gap

- **Missing supplies:** Inventory and bank are checked. If a required item is absent from both, Build1 HOLDs with its item ID and deficit. It does not yet gather/shop/GE-purchase supplies. This means Build1 is **not yet autonomous from an arbitrary empty inventory**.
- **No gameplay verification:** NPC menu text, quest dialogue variants, stage 30–33 beer count, jail guard damage, and route reachability remain unverified. Each failure stops with phase/error in `.runelite/princealirescue/status.properties` for Alex to diagnose.
- The copied hot host accepts only a script-only JAR with a future strictly higher build marker; this Build1 hot artifact is for inspection/initial packaging, not a same-build reload.
- Reload during an in-flight action deliberately HOLDs to avoid replaying a click. The correct next action must be chosen from fresh quest/inventory evidence.

No quest completion is claimed. The candidate is ready for Alex's review and controlled first integration.

## Build2 supply recovery handoff

Build2 changes only `PrinceAliRescueScript.java`; `PrinceAliRescuePlugin`/`Config` were recompiled without source edits. It inspects inventory and bank, withdraws a partial bank stack before sourcing the remainder, and withdraws a bounded coin deficit from the bank. When a required item is absent, a six-minute sourcing state machine handles these verified local options:

| Item | Local route | Public guide price cap per item |
| --- | --- | ---: |
| Rope | Ned's Draynor shop | 18 coins |
| Pink skirt | Thessalia's Varrock clothes shop | 2 coins |
| Redberries | Wydin's Port Sarim food shop | 3 coins |
| Pot of flour | Wydin's Port Sarim food shop | 10 coins |
| Beer | Blue Moon Inn Bartender dialogue, one per conversation | 2 coins |

The script checks the live NPC, shop widget and stock before each shop purchase; buys exactly one item; then requires a later item gain **and** coin loss within the per-item cap. Dialogue beer acquisition is checked across later ticks. Each source is limited to three unproductive interactions, 100 coins total, and six minutes. Unexpected price, unavailable shop stock, insufficient coins, missing NPC, unrecognized dialogue, or an unproved purchase produces a diagnostic HOLD. No repeated unverified purchase is issued. Nonquest inventory items are deposited only to make room for a required withdrawal; food is retained.

The installed `Rs2Shop` signatures/implementation were inspected with `javap` and CFR (`openShop(String,boolean)`, `hasMinimumStock(int,int)`, `buyItem(int,String)`, `closeShop()`). `Rs2Shop.buyItem` prepends `Buy ` to quantity, so Build2 passes `"1"`. The installed `Rs2GrandExchange.getPrice(int)` calls an external GE tracker, which cannot prove private-server offer availability or price. Build2 therefore does **not** place GE offers. Source locations/prices: [Ned](https://oldschool.runescape.wiki/w/Ned), [Thessalia](https://danielpgleason.com/osrs/reference/thessalia-efe26191/), [Wydin](https://danielpgleason.com/osrs/reference/wydin-s-food-store-100d0ceb/), [Prince Ali guide](https://danielpgleason.com/osrs/quests/prince-ali-rescue/). Blue Moon's route waypoint `(3228,3393,0)` comes from the existing Pirates Treasure script; actual Bartender presence and dialogue are still a live check.

**Acceptance gap:** Wool, yellow dye, ashes, water bucket, bronze bar, soft clay and other unrecognized missing supplies still HOLD with the exact item ID/deficit. Build2 is not an empty-account end-to-end solution. Public shop prices may differ on the private server; the later coin-delta check catches excess charge but cannot prevent that first charge. Live shop/quest testing remains Alex's task.

Build marker/log: `BUILD_NUMBER=2`, `[PrinceAliRescue] RUNNING_BUILD=2 pid=...`. JDK17 compile against the installed client JAR succeeded with eleven deprecated API warnings and no errors. No game or client operation was performed.

- Full six-class plugin overlay `princealirescue-plugin-2.jar`: SHA-256 `4D5F9952326664CA9C08B8EBF18427EAF5A911C47DA44F9EA2FE667F18CBE1CD`.
- Three-class script-only hot JAR `princealirescue-2.jar`: SHA-256 `A75DB4CD87BADAF1143EDABD7C4277F746302724FEF60348398031064788EFA3`.
- Both archives contain package-path classes only and no manifest; neither is a cumulative updater patch. Alex owns integration/deployment.

## Build3 remaining-supply candidate

Build3 changes only `PrinceAliRescueScript.java` and this README. It covers the six Build2 gaps: soft clay, ball of wool, yellow dye, ashes, bucket of water, bronze bar. When clay and a water bucket are already in inventory/bank, it combines them and verifies a later soft-clay gain plus ingredient loss. When two onions and five coins are available in inventory/bank, it uses an onion on Aggie and verifies yellow dye gain, onion loss, and coin loss. These local recipes follow the installed QuestHelper and the established Goblin Diplomacy script's Aggie API/ID; they avoid a GE offer when the ingredients are already held.

For other deficits in those six tradeable items, Build3 has a bounded GE fallback. It obtains an external quote only as a proposed **maximum offer price**; the quote cannot establish private-server market stock or price. It refuses an unavailable quote or a proposed offer above a cumulative 1,000-coin reservation cap. It withdraws only the exact capped coin deficit, walks to the installed `BankLocation.GRAND_EXCHANGE`, requires a live exchange widget and completely empty offer slots, and places **one** offer for the exact item deficit. On a later tick it requires the in-game offer's item ID, unit price, quantity, and coin debit to match the bounded request. It then waits at most 45 seconds for a fill, collects only its offer slot, and requires a later inventory gain and net coin loss. If unfilled, it cancels the slot only while it is the sole occupied offer, observes cancellation, and HOLDs for review. An unproved placement/collection/cancellation also HOLDs; it never submits a second offer blindly.

The installed `Rs2GrandExchange` signatures and decompiled implementation were checked for `getPrice`, `buyItem`, `hasBuyOffer`, `getOfferDetails`, `collectOffer`, `cancelSpecificOffers`, `isAllSlotsEmpty`, and `openExchange`. `getPrice` calls an external GE tracker; Build3 therefore treats it as a capped quote, then verifies the in-game offer before claiming progress. The installed `Rs2Inventory.combine`, `useItemOnNpc`, `Rs2Bank.withdrawDeficit`, and `Rs2Walker.walkTo` APIs compile. Item IDs are from the installed QuestHelper and existing Doric/Goblin scripts. The [Prince Ali guide](https://danielpgleason.com/osrs/quests/prince-ali-rescue/) corroborates the raw ingredients.

**Limits:** No live GE/shop/game test was run. Private-server GE may be absent, have no sellers, or use different prices; the script HOLDs with an explicit reason. The GE API's cancel helper also collects offers, so Build3 requires its offer be the sole occupied slot before invoking it. The public quote and input cap do not prevent a server that ignores the requested price from charging unexpectedly; later proof detects that but cannot reverse it. Wool gathering, ashes gathering, water filling, and bronze smelting remain future alternatives when GE cannot fill. No quest completion is claimed.

`BUILD_NUMBER=3`; startup log `[PrinceAliRescue] RUNNING_BUILD=3 pid=...`. JDK17 compilation against the installed `microbot-tutorial-island.jar` succeeded with twelve deprecation warnings and no errors. The release packager now fixes ZIP timestamps, so repeated dry-runs produced byte-identical outputs. This verifies compilation and packaging only; the plugin host has not been loaded and the quest has not been game-tested.

- Full six-class plugin overlay `princealirescue-plugin-3.jar`: SHA-256 `CE405FDFBE3CE536DB232011C51BE265DA50EE52A93A2065942C45B6D0F37B48`.
- Three-class script-only hot JAR `princealirescue-3.jar`: SHA-256 `F3B9ACD337CFE60D67A8A43837D313C7E856ADCB4E901980529C5181A6A3C92B`.
- Cumulative updater candidate `patch-696.zip`, based on exact published `patch-695.zip`: SHA-256 `955EAF7D55B5C3B82A107C6DFAEBBFC14449E18CEEF9E86E7B09EB2B57E8D513`.
- The full overlay and script-only JAR contain package-path classes only and no manifest; the former adds the new plugin host, while the latter is the script-hot-swap artifact. First host discovery still needs a verified runtime loader path; later Build4+ script-only updates can use the new host's hot watcher once it is running.

## Build3 first live run / Build4 dialogue correction (2026-10-01)

Patch696 installed through the established Supervisor and started Prince Ali Rescue Build3 on client PID1708; the startup marker and fresh host status agreed. This first host installation required one cold client restart. The client then reached Hassan, walked to Osman, and advanced quest varp273 from 0 to 10 and then 20. At varp20 the private server displayed three dialogue options: `Do you know why they've taken the Prince?`, `Where abouts in Draynor is Leela?`, and `No. I think I know everything I need to.` Build3 did not recognize these options and entered a diagnostic HOLD. This was a confirmed script defect, not a pathing or login failure.

Build4 adds the exact observed `No. I think I know everything I need to.` response to the known dialogue choices. It uses the installed `Rs2Dialogue.clickOption(String)` substring matching behavior and still records one pending action; the later dialogue/varp change must prove the click. The installed bytecode confirms the single-string API uses case-insensitive substring matching. JDK 17 compiled Build4 against the installed client with twelve deprecation/removal warnings and no errors; `javap -constants` confirms `BUILD_NUMBER=4`. Two dry-runs produced identical artifacts.

- Full plugin overlay `princealirescue-plugin-4.jar`: SHA-256 `6BFEF74D8C17038AD9F38E46A8E95D8BC4867FE7B680999A19BA6A2C80E4327F`.
- Script-only hot JAR `princealirescue-4.jar`: SHA-256 `54616C29BAF9162CF2438581BB80334ACF2717465CCEB4965AC7BA51850636CD`.
- Cumulative patch `patch-697.zip`, based on exact published `patch-696.zip`: SHA-256 `AA09C684A462E8E5FEE375FA1D9749AA93AB6224812EE8B770D5EC31E2A1C528`.

The user's resource preference applies to Mining progression: drop only script-owned low-value ore after verifying the quantity change; do not bank it just to train Mining; retain quest-needed items and pre-existing items.

Build4 published as patch697 and hot-loaded, with hot-host status confirming build4 on the original PID1708; no game restart occurred. Live evidence then exposed one lifecycle gap: the host correctly carried the old HOLD through reload, so the new menu option was present in code but the held script remained stopped. Build5 adds a narrow migration that clears HOLD only when the saved HOLD reason is the exact unrecognized-dialogue condition and the saved option text includes the newly recognized Osman answer. The resumed script must still freshly observe a current dialogue option before it can click; unrelated HOLD causes remain stopped. Build5 compiled with JDK17 and the marker is verified as 5; two dry-runs match byte-for-byte.

- Full plugin overlay `princealirescue-plugin-5.jar`: SHA-256 `67E5852805A67171604786C32607BFD1A3DEA664FD6ED7F89DCEC3B96BD187E9`.
- Script-only hot JAR `princealirescue-5.jar`: SHA-256 `FA729A9FB38D0AEB64D55B1236E58AD09E05202ACC3864A6E388E362A157C3FD`.
- Cumulative patch `patch-698.zip`, based on exact published `patch-697.zip`: SHA-256 `3C5FE2DFA61D1F0B0307237EAD4BCE915C19D45B6DAC3848025BD64D79700302`.

Build5 hot-reloaded on PID1708, matched the saved error and saved exact menu, clicked the newly recognized Osman answer, and proved a later dialogue change (`PROVED OPTION`) before handling the following Continue prompts. This fixed the varp20 dialogue hold without a client restart. The next stage reached the supply bank and safely held because GE price for 3 balls of wool was unavailable (`quote=0`); no offer or spend was made.

Build6 replaces that unavailable GE-only wool branch with a bounded local path adapted from the already live-tested Sheep Shearer plugin: inspect bank for raw wool; otherwise use carried/verified shears, find only live reachable sheep IDs with the `Shear` action near (3201,3268,0), and require raw wool item 1737 to rise after each shear. It routes to Lumbridge Castle, proves each stair plane transition, uses live spinning wheel 14889 and clicks only the visible output widget for ball 1759; later inventory must show balls up and raw wool down. Animation 894 gets a 15-second progress guard. Rejected sheep targets are excluded and limited; no free slot, missing shearable sheep, missing stair/wheel, or any unproved transition HOLDs. It consumes only the number of balls required, uses existing items, and does not bank low-value supplies.

Build6 compiled against the installed client JAR with fourteen deprecation/removal warnings and no errors; `javap -constants` confirmed `BUILD_NUMBER=6`. Two deterministic package dry-runs matched byte-for-byte. Live testing found the first local wool-supply run sheared one sheep and then entered HOLD because `Rs2Walker.walkTo()` returned false; the walker logs show that the call had taken the route toward the castle stairs, so the saved HOLD position was stale.

- Full plugin overlay `princealirescue-plugin-6.jar`: SHA-256 `BE4D6692295FE6F24296A11F71507EADC1E9C7568F48888DE177843BD79024A8`.
- Script-only hot JAR `princealirescue-6.jar`: SHA-256 `2D85B04BDA3A1F2E32397D4BC92B8AEBF1A105A3781F82FCCF5F2DB41D2FD0D8`.
- Cumulative patch `patch-699.zip`, based on exact published `patch-698.zip`: SHA-256 `329BDF70411757FC22D20C5E48F3F3ED8F608510EE4022800EF86CD4DA268DC2`.

## Build7 — Walker MOVING is not a rejection (2026-10-01)

Installed-client bytecode inspection confirmed `Rs2Walker.walkTo(WorldPoint)` returns `true` only when `walkWithState(...) == ARRIVED`; all other results, including normal `MOVING`, return `false`. Build6 treated false as a terminal rejection. Build7 now records the walk as pending for either boolean result and accepts it only after a later player-position observation, preserving the existing bounded rescan/HOLD on no progress. It also resumes the exact earlier wool/stair HOLD only when a fresh logged-in frame proves varp273=20, raw wool is still carried, the player is on plane 0 within 8 tiles of the castle stairs, and the saved error is exactly that stair-route rejection. The next tick must still observe and click the live stair; no prior click is replayed.

Build7 compiles with JDK17 against the installed Microbot client JAR (14 existing deprecation/removal warnings, no errors); `javap -constants` confirms `BUILD_NUMBER=7`. Same-PID runtime confirmation is pending after hot-load.

Build7 was hot-loaded, but its initial “false is not a route failure” treatment was too broad. Muse's read-only decompilation of this installed JAR clarified that `walkTo(WorldPoint)` is blocking and returns true only for `ARRIVED`; false can be `MOVING` at its entry gate, `UNREACHABLE`, or terminal `EXIT` (including tail exhaustion). Build9 now samples the player's cached position after the blocking call and applies the installed default arrival radius of 10: false within that radius becomes a pending walk requiring later observation; false outside it HOLDs with the fresh post-call position. The pending proof radius matches 10. Runtime on PID1708 proved `WOOL_CLIMB_UP` to plane 1.

The following wheel click consumed one raw wool and produced one ball (inventory 1759: 0→1, 1737: 1→0), while the script's narrower widget-only `WOOL_OPEN_WHEEL` proof still entered HOLD. Build10 recognizes that same-action later inventory delta before timeout and, after hot reload, clears only the exact saved wheel-open in-flight HOLD when varp273=20, the player is beside the wheel upstairs, one or more balls exist, and raw wool is zero. It records the observed inventory proof and waits/continues from that state; it does not replay the wheel click.

Build10 compiled against the installed Microbot JAR with fourteen existing deprecation/removal warnings and no errors; `javap -constants` confirms `BUILD_NUMBER=10`.

## Build12 — approach and verify the live castle stair before descent

The Build11 runtime attempted `Climb-down` from `(3208,3213,1)`, about seven tiles from the selected stair, and timed out after only a one-tile move. Build12 first inspects the live object name/actions, then routes to within two tiles using the installed `Rs2Walker.walkTo(WorldPoint,int)` API. It observes that approach on the next tick before issuing one `Climb-down` click. The exact old descent HOLD resumes only when varp273=20, wool/shears/raw-wool inventory and upstairs position still match the wool-supply stage; it restarts the source timer and never replays the distant click. The two-floor transition remains pending until a fresh plane-0 observation proves descent.

Build12 compiled against the installed Microbot JAR with fifteen existing deprecation/removal warnings and no errors. `Rs2Walker.walkTo(WorldPoint,int)`, live staircase action checks, and object action arrays were verified in installed APIs. Runtime test after hot-load is still required.

Mining-training policy: do not bank/travel for cheap ore solely for XP. Retain quest-needed or valuable items; if inventory space is needed, only drop script-owned low-value ore after verifying the quantity change.

The first Build12 hot-load correctly preserved the timed-out click as in-flight, so its live-HOLD recovery predicate did not match the host's reload-state wrapper. Build13 accepts that recovery only when the saved action is exactly `WOOL_CLIMB_DOWN`, the saved error is the specific unproved descent, and the fresh logged-in frame still proves varp273=20, one-to-two balls, zero raw wool, shears, and upstairs position. It clears that saved pending action without replaying it, then resumes at the verified stair-approach step. Build13 compiled cleanly aside from the same fifteen deprecation/removal warnings; hot-load/runtime proof is pending.

The first Build13 reload confirmed the host preserves its generic in-flight wrapper on subsequent reloads and drops the original pending-action field. Build14 additionally recognizes only that exact wrapper string (`Reload during WOOL_CLIMB_DOWN; inspect quest/inventory/scene before resuming`), still requiring all fresh logged-in, varp, wool, shears, and plane evidence before resuming. It does not click the stair during recovery.

Build14 then exposed a real collision barrier: the walker log identified object 1543 at `(3207,3214,1)` during its can't-reach recovery, and its attempt to reach the stair stopped after the character moved away. Build15 scans nearby live tile objects through the installed tile-object cache, filters for same-plane closed doors/gates that lie toward the known stair, and opens one only after confirming its live `Open` action. The open action must prove on a later observation before the bot retries its bounded approach; otherwise it remains held. The exact stair-route HOLD can resume only with current wool-stage inventory/varp evidence.

Build15 was published as patch708 and hot-loaded in unchanged RuneLite PID1708. Runtime recovered the exact hold, found door ID1535 at `(3207,3210,1)`, opened it, and verified the `Open` action disappeared. It then walked from `(3207,3211,1)` toward the stair. The live walker is now stuck in repeated `route-move-in-flight` yields at `(3206,3210,1)` after both built-in stagnation replans; it emits a warning that this loop cannot exhaust its tail cap. Do not treat the ongoing walk as success or attempt another click. The next fix should replace this blocking walk with bounded per-tick `walkStep` observations so the script can stop/recover and hot reload safely.

## Build16 — bounded wool-stair approach and recovery

Build15's blocking `walkTo(stair, 2)` failed to terminate cleanly at the door corridor and eventually recorded an unproved approach HOLD at `(3205,3211,1)`. Build16 replaces only this close stair approach with the installed `Rs2Walker.walkStep(WorldPoint, int)` API. It issues one step no faster than every 1.6 seconds, waits while movement is active, requires a fresh position observation before continuing, and HOLDs on `UNREACHABLE`, `EXIT`, 12 attempts, or 20 seconds without tile progress. The stair interaction remains a separate action after an observed distance <=2, and descent still requires plane 0 on a later frame.

A narrowly scoped reload recovery now accepts the exact saved `WOOL_APPROACH_DOWNSTAIRS` in-flight wrapper only when the fresh logged-in frame still shows varp273=20, one ball of wool, zero raw wool, shears, upstairs, and near the spinning-wheel room. This avoids replaying the old walk/click. No arbitrary saved coin value is restored.

Build16 compiled against the installed Microbot client JAR with 18 existing API deprecation/removal warnings and no errors. Two release709 dry-runs were deterministic. Published cumulative patch709 (commit `4e88c92dd74229c5a9553409d32c1976ab6ef99e`), then hot-loaded Build16 into unchanged PID1708; host status confirms SHA-256 `889accfa8d3b9ff571201e5622eb03d5a4853d510f95800242b249baf3224a04`.

Runtime evidence: the exact Build15 approach HOLD recovered; Build16 reached the stair radius from `(3207,3211,1)` to `(3205,3209,1)`, dispatched one `Climb-down`, then proved plane-0 position `(3206,3208,0)`. It is currently routing toward the sheep field for local wool supply. The door/stair segment is verified; Prince Ali Rescue is not complete.

## Build17 — local yellow-dye fallback when private GE has no quote

Build16 correctly stopped when the private GE returned quote 0 for required yellow dye (1765), but the project already had a verified local route. Build17 narrowly resumes only that exact dye-source HOLD when a fresh frame confirms varp273=20, a normal wig is carried, no yellow dye exists, fewer than two onions are carried, and at least five coins are available. It then walks to the previously live-tested Fred onion field `(3190,3263,0)`, selects only a live onion plant ID 3366/5538 with a verified Pick/Take action, limits attempts, and requires the onion inventory to increase after each pick. Once two onions are present, it returns to the existing need-handler for Aggie; yellow dye is accepted only when dye increases, onion count decreases, and exactly five coins are consumed. The same local fallback is selected immediately for future dye GE quotes that are absent or exceed the established cap. The script never discards items to make room; a full inventory produces a diagnostic HOLD.

Build17 compiled against the installed Microbot client JAR with 18 existing deprecation/removal warnings and no errors. Two release710 dry-runs were deterministic. Published cumulative patch710 (commit `650d3b9792c21e4da056743d027a5cfa8023f78e`) and hot-loaded Build17 into unchanged PID1708; hot SHA-256 `078c4c9a792c588839d311cdfc27b4e3058a8c3fca044cd53f87d44f86bef0a9`.

Runtime: the exact Build16 no-quote HOLD recovered with 609 coins at `(3092,3245,0)`. It proved `CLOSE_BANK` then started route to the onion field. Onion collection and Aggie craft remain unverified. The live stream was opened in a background tab and retained for handoff; the captured frame showed the game and overlay while the bank UI was open, consistent with the local pre-recovery stage. No public chat message was sent.

## Build18 — reachability-first onion collection and gated field recovery

Build17's first onion click moved the character from `(3190,3263,0)` to `(3188,3264,0)` but did not increase onion inventory. Build18 narrowly recovers that exact first unproved pick only with the fresh Prince Ali stage, wig, coin, and location evidence. It never replays the click. It computes reachable tiles from the player and selects a walkable tile adjacent to the live onion object, then uses bounded, observed `walkStep` movement before clicking once. If no adjacent tile is reachable, it scans nearby live doors/gates with an `Open` action, opens one whose tile is close to the onion patch, and requires the `Open` action to disappear before continuing. A second unproved pick stays held. Plant inventory gain remains the only pick proof.

Build18 compiled against the installed client JAR with 25 existing API deprecation/removal warnings and no errors; `javap -constants` confirms `BUILD_NUMBER=18`. Runtime gate detection, adjacent-tile routing, and onion gain are not yet verified.

Mining rule reaffirmed: when Mining is only for XP, do not bank or travel for cheap ore; keep quest-needed or valuable materials, and ignore low-value ore unless the quest step requires it.

## Build20 — recover the hot-reload wrapper without repeating the pick

Build18's first hot-load correctly preserved the in-flight `PICK_DYE_ONION`, but the Microbot reload host wrapped it as `HOLD_RELOAD_IN_FLIGHT`. Build19 matched that wrapper but required phase `HOLD`, so it correctly remained held. Build20 accepts the exact `HOLD_RELOAD_IN_FLIGHT` phase only when the restored action is precisely `PICK_DYE_ONION` and fresh tutorial-stage, inventory, and location checks still match. It clears the restored action and proceeds through the Build18 reachability-first route; it does not replay the earlier pick.


## Build21 — exact hold recovery under the host reload wrapper

The host retains the prior pick HOLD but does not reliably pass the original action field through to the new script instance. Build21 therefore matches the exact saved `Reload during PICK_DYE_ONION; inspect quest/inventory/scene before resuming` error itself, and still requires the fresh logged-in state, varp273=20, wig, no dye, coin fee, fewer than two onions, plane 0, and proximity to Fred's field. This lets the verified Build18 adjacent-tile/gate routine proceed without repeating the old pick.

## Build22 — bounded timer recovery for a hot-reloaded local onion source

The first onion-pick HOLD was recovered after hot reload, but Build21 inherited the original local-source start time and immediately hit its six-minute cap. Build22 resets that timer when recovering the exact unproved pick and adds one narrowly matched recovery for the exact stale-timer HOLD, guarded by the same current stage, inventory, varp, location, and fee checks. The timeout recovery can happen only once; a later six-minute stall remains held.

## Build23 — per-pick retry budget after a proved onion gain

Build22 routed into the onion patch and verified the first onion, but the aggregate attempt counter remained at two and stopped before the second item. Build23 resets the attempt counter after each proved inventory gain and recovers only the exact two-attempt HOLD when exactly one onion is already carried. The next item still must be proved by inventory increase; a failed second pick remains held.

## Build24 — local ashes fallback and one-shot onion recovery

Build23 stopped at Draynor because the private Grand Exchange quoted zero for required ashes (item 592). Build24 recovers only that exact quote HOLD and first checks nearby ground ashes. If none exist, it uses one banked/carried tinderbox (590) and one normal log (1511) to light exactly one fire in the known outdoor sheep-field area. It requires the log count to decrease and a live Fire object at the target before waiting for burnout; it then requires ground ashes to appear and inventory ashes to increase. The fire, pickup, and total source time each have explicit bounds; missing tools, rejected actions, or absent proof HOLD with diagnostics. It does not buy an unpriced item or treat elapsed time as success.

Build24 also makes the onion second-pick cap recovery one-shot so an unchanged retry cannot repeatedly reset its attempt budget. This does not change the Mining policy: cheap ore collected solely for XP is not banked; low-value ore is ignored unless a quest step needs it.

Build24 compiled against the installed Microbot client with the prior API deprecation warnings only. Runtime firemaking and ashes production are not yet verified; verify the loaded build marker, then require live Fire, log consumption, ground ashes, and inventory pickup evidence before continuing.

## Build25 — source a missing tinderbox without repeating the bank HOLD

Build24's live client correctly held because no tinderbox was carried or banked. Build25 resumes only that exact HOLD, then routes to the known Lumbridge general store. It opens the observed Shop keeper, verifies shop stock for item 590, attempts Buy-1 once, and requires an inventory increase plus a bounded coin decrease before proceeding. It closes the shop before returning to the bank for a normal log; no click or stock listing alone proves purchase. Missing stock, no coins, or no verified gain stays held. The Firemaking attempt remains exactly once and still needs live runtime proof.

## Build26 — preserve native login recovery during an exact quest HOLD

The Build25 livestream showed the client disconnected at loginIndex 24 while an ashes sourcing HOLD was active. The main loop previously returned from `held` before `loginTick`, so the one-shot native disconnect dismissal, verified free-world login, and Microbot `WelcomeScreenEvent` path could never run. Build26 lets only that existing login path execute while preserving the saved quest HOLD; quest recovery still requires a fresh logged-in frame and all existing exact error/inventory checks. Status now includes loginIndex and login counters/error, and logs the disconnect-dismiss, free-world login, and WelcomeScreenEvent dispatch. It does not treat a dismissed modal or login click as quest progress.

## Build27 — acquire the one missing ashes log locally

The live Build26 checkpoint has a fresh same-PID status on PID1708: LOGGED_IN, varp273=20, at the Al Kharid bank, one tinderbox carried, and the exact HOLD says normal logs (1511) are absent from inventory and bank. Build27 recovers only that exact HOLD after a fresh logged-in varp/plane/inventory check, then searches the bank for an existing axe before routing to Fred's farm. If needed, it takes the live axe from scenery ID 5581 with `Take-axe` and requires bronze axe 1351 inventory gain; it scans for a live reachable regular Tree with `Chop down`, issues one action, and requires log 1511 gain before returning to the existing firemaking proof path. Every interaction remains pending until a later observation; missing axe/tree or failed proof stops with HOLD. The RuneScape Wiki confirms the Fred's farm scenery and action: https://oldschool.runescape.wiki/w/Logs_%28bronze_axe%29.

Build27 compiles against the installed Microbot client. Its local axe/tree acquisition remained a candidate until live testing.

The first helper review found an arrival-radius/fence risk and an object-waypoint oscillation. The candidate now resolves live object 5581 before using Fred's waypoint, approaches the live object tile with radius 2, uses `Rs2Walker.walkWithStateUntil` with a 15-second per-call cap, and permits another route call only after fresh tile progress; the total route is capped at 120 seconds/four calls/45 seconds without progress. It checks every standard axe ID in inventory, equipment, and the open bank, and searches the installed reachable-tree API out to 16 tiles. These changes compile; the helper's final review and live route still need verification.

## Build28 — approach a live tree before chopping

Build27 live evidence showed `ASHES_NORMAL_LOG_CHOP_DISPATCH` at a tree roughly ten tiles away, followed by `HOLD Unproved ASHES_CHOP_NORMAL_TREE`; no log was gained. `findReachableObject(..., 16, ...)` means path-reachable, not close enough to interact. Build28 added a route to the selected live tree's observed tile and requires fresh post-walk position within two tiles before issuing one `Chop down`. Patch721 hot-loaded in existing PID1708 with SHA `200615a7b48992443911ae6678c8b9f05ad4904bfdd28e363457a3756fa9f79a`; the old unproved action correctly remained held across reload.

## Build29 — recover one timed-out tree approach safely

Build29 resumes only the exact reload wrapper for the timed-out `ASHES_CHOP_NORMAL_TREE` action, after a fresh logged-in/varp/inventory/axe check and a live reachable-tree scan confirms the player is within two tiles of that tree. It permits one bounded nearby retry, then requires log 1511 inventory gain; any further failure HOLDs. This recovery and retry cap require live verification.

## Build30 — retain the exact tree-action recovery through repeated hot reloads

Build29 evidence showed its exact `Unproved ASHES_CHOP_NORMAL_TREE` detail had already been wrapped by `Reload during ASHES_CHOP_NORMAL_TREE; inspect quest/inventory/scene before resuming`. Build30 recognizes that exact wrapper as an alternate signal while still requiring saved ashes/log stage, fresh logged-in varp/inventory/axe evidence, and a live reachable tree within two tiles. The single retry cap and inventory-gain proof remain unchanged.

## Build31 — restart the expired material-source timer only on proved recovery

Build30 recovered that exact timed-out action, but the old six-minute source timer immediately caused another HOLD before the one allowed retry. Build31 resets that timer only inside the exact fresh-state tree-action recovery; the retry remains one attempt and still requires a normal-log inventory increase.

## Build32 — resume the already recovered retry after its exact timer HOLD

Build31's active state still had the exact six-minute source HOLD and `sourceAttempts=1`, because the timer reset change was published after the earlier Build30 recovery. Build32 recognizes only this exact HOLD when the one-time recovery flag and attempt count match, then revalidates current game/quest/inventory/axe and live tree adjacency before resuming. The chop retry cap is still two total dispatches, and log 1511 gain is still required.

## Build33 — replace a Microbot-unreachable tree with a local farm search

Build32 live evidence: Microbot logged `[Interact] can't-reach recovery` for Tree1276 at `(3265,3215)` and the wrapper returned false. Build33 recovers only that exact target HOLD and routes to the verified Fred farm waypoint; the live walker handled its door edge and the client reached `(3190,3273)`. The scene there had no regular tree within eight tiles, so that scan HOLDed with evidence.

## Build34 — search nearby trees with live reachability and line of sight

Build34 resumes only the exact no-tree-at-Fred HOLD after fresh state checks, rescans up to 16 tiles for a live regular `Tree` with `Chop down`, `isReachable`, and line of sight, and excludes the failed Tree1276 tile. It requires the player within two tiles before the next action and log 1511 inventory gain afterward.

## Build35 — approach alternate trees and require fresh line of sight

Build34 reached Fred's farm but every scanned regular tree had Chop down and isReachable=true while live line of sight was false, so its exact runtime HOLD was valid. Build35 recovers only that exact HOLD after rechecking logged-in quest state, required tinderbox, equipped/inventory axe, position and absence of log 1511. It then approaches one reachable tree to radius 1 through the installed door-aware walker, rechecks line of sight on a fresh tick, rejects a still-occluded tile and tries another tree. Chop is dispatched only when the live tree still has the action, reachability, distance and LOS; log 1511 inventory gain remains the success proof. Rejected target tiles persist across hot reload. Compilation against the installed client completed with existing deprecation warnings only; live approach and log gain remain to be verified.

## Build36 — accept the walker-tested radius and recover its exact stall

Build35 test evidence: after the one-tree approach it reached (3182,3279) for the tree at (3180,3279) but the installed walker repeatedly returned EXIT without changing tiles at radius 1 and correctly HOLDed before chopping. Build36 uses the tested 2-tile arrival radius, then still requires a fresh LOS check before a single Chop action. If the exact Build35 tree-approach HOLD is reloaded, it revalidates the current quest/inventory/axe and excludes the one currently selected tree before rescanning. Other tree route failures still HOLD under the existing bounded policy. Compilation against the installed JAR succeeded with the same API-deprecation warnings; a fresh same-PID scene/LOS check and log gain are pending.

## Build37 — keep expanded tree routing inside the expanded search area

Build36's same-PID trace showed the selected tree at (3177,3278) was inside the 16-tile search radius but the next tick routed back to Fred's waypoint because the return threshold still said 8. Build37 changes the waypoint guard to 16, matching the scan boundary and preventing that immediate bounce. Tree approach remains radius 2; chop still requires fresh LOS and successful inventory proof. Compilation succeeded against the installed JAR with only existing deprecation notices. Live tree selection and log gain remain unverified.

## Build38 — resume a fresh scene after the exact tree-walk reload wrapper

Build37 hot-load wrapped an in-flight ASHES_APPROACH_LOG_SOURCE movement as HOLD_RELOAD_IN_FLIGHT; this is not a chop action. Build38 recognizes only that exact wrapper and saved action, then rechecks logged-in state, varp273=20, plane0 within the 16-tile Fred search, axe and tinderbox, and no log 1511 before resuming from a fresh tree scan. It clears only the stale walk state and never replays the movement or chop. Compilation succeeded against the installed JAR with existing deprecation notices.

## Build39 — restore the exact saved tree-approach action

Build38 runtime remained at the specific reload wrapper Reload during ASHES_APPROACH_LOG_SOURCE; build39's recovery was unnecessarily requiring the in-memory geStage string to match. Build39 recognizes only that exact saved action/wrapper and revalidates source item 592, goal 1, logged-in varp273=20, plane 0 within Fred's search radius, no normal log and tinderbox. It restores the normal-log substage, clears stale movement state, and scans fresh before acting; axe is checked by the ordinary acquisition flow before any chop. No click or chop is replayed. Compiled against the installed Microbot jar with existing deprecation notes.
