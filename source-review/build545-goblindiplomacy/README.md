# Goblin Diplomacy Microbot plugin — source build 543

This directory contains a separate RuneLite plugin in package
`net.runelite.client.plugins.microbot.goblindiplomacy`. It compiled locally
against the installed Microbot JAR. Build 539 was installed and started the
quest on a fresh account; the supervising launcher and previous quest plugins
remain in the cumulative base patch.

The installed Quest Helper maps quest progress varbit **2378** as 0/3 for
starting and offering orange mail, 4 for blue mail, and 5 for undyed brown
mail. Values 1/2 can appear during the opening conversation and are treated
as transient; the script waits through visible dialogue and enters HOLD if
they stall. A progress varbit never marks completion. Only
`Quest.GOBLIN_DIPLOMACY.getState(client) == QuestState.FINISHED` marks `DONE`.

## Materials and route

The script reads the current inventory before deciding what to source. It
uses carried dyes, dyed mail, raw ingredients, and coins when present.
Missing redberries are bought one at a time from Wydin's Food Store in Port
Sarim. Missing onions are picked from Fred's field. Wyson in Falador Park
sells two woad leaves for 20 coins. Aggie in Draynor makes red, yellow, and
blue dyes from 3 redberries, 2 onions, and 2 woad leaves respectively, plus
5 coins per dye. The script combines red and yellow into orange, then uses
orange and blue on separate plain goblin mail. A third mail stays plain.
It takes mail without combat from the north, west, and upstairs Goblin
Village crates. The three crate varbits identify previously searched crates;
an increased inventory count proves each search.

Preflight reserves three coins for each missing redberry based on Wydin's
standard shop price. The private-server price and stock remain live test
points; inventory gain is the purchase proof.

At startup, the script waits for a loaded inventory. Build 540 counts free
backpack slots against the 28-slot capacity because the live RuneLite item
array was shorter than that capacity. At the first dye stage,
it checks the free slots and coins needed for its observed inventory. Missing
coins or space cause a diagnostic HOLD. It does not withdraw from a bank, use
the Grand Exchange, kill goblins, or claim to recover mail after all three crates have
been searched. Wydin stock is given two minutes to replenish before HOLD.

The west Varrock exit is a fixed waypoint for westbound travel from inside
Varrock. The installed walker handles doors and other path obstacles under a
route time budget. A click or walker return value is never treated as proof;
the next observation must show dialogue, inventory, plane, position, shop, or
quest state progress. Three unproved actions or route failures cause HOLD.
After proof, the next action has a small bounded delay. Only the two
equivalent generals are varied, and only when they have line of sight and a
reachable adjacent tile. Unique ladders, crates, and route chokepoints stay
fixed. Low health prompts food from inventory or HOLD.

The live status path is
`~/.runelite/goblindiplomacy/status.properties`. It includes `build`, `pid`,
`timestamp`, `currentWorld`, `gameState`, `questVarbit`, `questState`,
`stage`, `position`, `error`, `healthPercent`, `goblinMail`, `orangeMail`,
`blueMail`, ingredient counts, crate flags, pending action, route target,
and pacing reason. Confirm the fresh `RUNNING_BUILD=539` log entry, PID,
and class SHA-256 before interpreting any live status. Build 540 also exits
the exact generals' help menu that remains open after the opening dialogue
advances the quest varbit to 3.

## Evidence and live review points

The installed `GoblinDiplomacy` Quest Helper class supplies quest stages,
mail/dye IDs, crate and ladder IDs and locations, crate flags, general ID
669, and the exact orange/blue/brown dialogue choices. RuneLite `ItemID`,
`NpcID`, and `ObjectID` classes in the installed client supply the other IDs.
The [OSRS Wiki quest guide](https://oldschool.runescape.wiki/w/Goblin_Diplomacy)
confirms the three mail colours, noncombat route, Aggie/Wyson requirements,
and item sources. The [Dye guide](https://oldschool.runescape.wiki/w/Dye)
describes using ingredients directly on Aggie. These sources do not prove
that the current account can reach every ingredient source or that Wydin
stock, nearby object variants, and all dialogue text match this installation.
Live observations should resolve those points, with HOLD diagnostics rather
than speculative clicks if the scene differs.

Build 539's first live run advanced varbit 0→3 and then HOLDed at the generals'
unhandled help menu. Its inventory snapshot reported zero free slots even
though the screenshot showed open backpack cells. Build 540 corrects these
two observed defects. Build 540 reached Wydin's shop but could not buy a
redberry: the installed `Rs2Shop.buyItem(int, String)` prepends `Buy ` to its
argument, so its `Buy-1` argument never matches `Buy 1`. Build 541 passes
`1` as the quantity. Its live purchase, later sourcing, and the three mail
hand-ins require verification. Build 541 subsequently bought all three
redberries with inventory and coin proof, then began the Fred onion route.
Build 542 checks the shop item quantities directly while waiting for stock,
avoiding a warning in game chat each tick. It also accepts the installed
Aggie NPC variants 120, 121, and 4284 within her Draynor location. A separate
unstarted account or quest reset is needed to repeat a fresh full quest.

Build 541 bought the redberries, but its direct Port Sarim to Fred route
stalled at (3018,3231) while the walker repeatedly scanned the nearby
manhole transport; the matching screenshot showed a members-only upsell.
Build 542 retained that direct route and repeated the same stuck position.
Build 543 steers the Fred trip through two previously walked surface points,
the Port Sarim Veos location (3054,3245) and Draynor jail road
(3109,3264), then resumes the Fred target. Those points were verified during
X Marks in the reverse direction. Their forward direction still needs live
verification; any no-progress route remains bounded and HOLDs.

Build 543 nevertheless HOLDed at (3018,3231) before reaching the first
waypoint. The matching fresh capture showed the members-only manhole prompt.
Build 544 closes the installed group-278 membership prompt with one widget
click and next-tick disappearance proof, then makes up to three distinct
collision-reachable local east/northeast WALK clicks away from the manhole.
Each move requires observed position progress within eight seconds. It only
then resumes the long surface route. The prompt could be a consequence of
the bad route click rather than its cause; live Build 544 movement is still
required before declaring this correction effective.

Build 544 then picked two onions, bought two woad leaves, made red/yellow,
orange, and blue dyes, and reached the Goblin Village north crate. It HOLDed
at (2959,3513), one tile south of that crate, because the walkable approach
helper excluded the player's current tile and returned null. Build 545
allows one visible adjacent crate Search from the current tile; the later
Goblin mail inventory gain remains the only proof of a successful search.
The live crate click and the other two crate routes still need verification.
