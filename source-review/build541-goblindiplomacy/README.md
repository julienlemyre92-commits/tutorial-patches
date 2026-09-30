# Goblin Diplomacy Microbot plugin — source build 541

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
hand-ins require verification. A separate unstarted account or quest reset is
needed to repeat a fresh full quest.
