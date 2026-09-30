# Pirate's Treasure Microbot plugin

Separate RuneLite/Microbot quest plugin. Current source candidate is Build 558. The already-running client is still Build 557 until this candidate is loaded; recent runtime evidence remains at `WAIT_LOGIN`. The plugin writes live status to `~/.runelite/piratestreasure/status.properties`. Identify the running build from `[PiratesTreasure] RUNNING_BUILD=<build>` in the client log and the loaded class SHA-256, not the patch version file alone.

## Login handling

Build 558 adds a bounded native login controller to the existing script tick. On recognized RuneLite login indices 10 or 34, it first checks the client's native world list, then falls back to Jagex's live `slr.ws` list if needed. It selects a random free world, requests one world change, and waits for `currentWorld` to verify before pressing Play Now. Play attempts are limited to two, eight seconds apart. Unknown screen indices are left to the existing OCR clicker. After three failed world lookups, two unverified Play attempts, or 60 seconds on the recognized surface, the controller records the state and yields to OCR. It does not enter account credentials.

## Quest route

The installed Microbot Quest Helper defines varp 71 stages 0–3. Stage 1 contains many rum-smuggling substages without a varp increment, so this plugin also uses inventory, location, dialogue and a local account-bound phase checkpoint.

1. Talk to Redbeard Frank (NPC 3643 at 3053,3251), choose “I'm in search of treasure.” then “Yes.”
2. Sail from Port Sarim (seaman NPC 3645 at 3027,3222) to Karamja with 30 coins. Buy Karamjan rum (431) from Zembo (NPC 13655 at 2929,3145) for 30 coins.
3. Pick ten bananas (1963) from reachable banana trees near 2917,3161. Talk to Luthas (NPC 3647 at 2938,3154) about plantation work. Use rum on crate 2072 at 2939,3149, then `Fill` it with the bananas. Talk to Luthas to ship the crate; verify his 30-coin payment.
4. Return through Customs Officer (NPC 3648 at 2955,3146). Obtain and wear white apron (1005) near 3016,3229. Get through Wydin's door 2069/2070, then `Search` the back-room crate 2071 near 3009,3207. Require rum inventory to increase. Deliver it to Frank; require varp advance to 2.
5. Climb Blue Moon Inn stairs 11796 at 3228,3393,0 and use chest key 432 on chest 2079 at 3219,3396,1. Require a fresh chest response and later varp 3.
6. Obtain spade 952 from Falador spawn near 2982,3369. Walk to the exact cross tile 2999,3383,0 and dig. If gardener NPC 3651 appears, kill it and dig again. Completion requires `Quest.PIRATES_TREASURE.getState(client) == FINISHED`.

The NPC/object IDs, quest-stage map, item IDs, and most named coordinates above were read from the installed Microbot Quest Helper bytecode. The spade spawn and banana patch coordinates were read from its installed `questhelper.logic.PiratesTreasure`. The [OSRS Wiki quest reference](https://oldschool.runescape.wiki/w/Pirate%27s_Treasure) provides independent step context; the live private-server scene remains authoritative for click actions and access.

## Proof and recovery

The script observes, sends one action, and waits for a later observation proving an inventory, dialogue, plane, location, or quest-state change. A click response alone does not advance the stage. Each action has a timeout and at most three attempts; unresolved actions enter `HOLD` with diagnostics. Walking uses the installed Microbot walker, with movement proof, collision-aware local reroute, bounded segments and cancellation. Small random delays occur only after verified actions or arrival. An account-bound checkpoint preserves the stage-1 smuggling phase across a client restart. A Luthas shipment in flight at relog requires payment proof from the saved coin baseline; otherwise it enters `HOLD` instead of repeating the hand-in. During a normal run, the 20-second payment-proof timeout restarts after each verified dialogue step; it holds only after 20 seconds with no proven dialogue progress or payment. An ambiguous checkpoint mismatch enters `HOLD` rather than replaying a voyage.

## Validation gaps

- Compilation against the installed JAR passed; only deprecated ground-item API warnings remain. The bundled Java 17 runtime reached `slr.ws` with HTTP 200 and read a 30 KB response, but Build 558 has not yet exercised world selection or login inside the client. There has been no live Pirate's Treasure run of this build.
- Apron spawn type/action, Wydin's job dialogue, door crossing, and an adjacent accessible crate tile must be confirmed from a fresh live scene. The script inspects the relevant object action and reachable collision tiles; if they are absent it holds.
- Confirm seaman/customs dialogue and boat transition, banana tree morphs, Zembo stock, Luthas payment, chest dialogue, gardener encounter and final dig in game. An inventory or dialogue response is an intermediate proof, not quest completion.
- Stage-1 checkpoint restores only when account, quest varp and region are consistent. If no checkpoint exists and the live state is ambiguous, the script holds for diagnosis.
