# Sheep Shearer Microbot plugin

Separate novice Lumbridge quest plugin for the existing Microbot client.
The Restless Ghost, Cook's Assistant, and Tutorial Island classes remain in
the cumulative client JAR. Build 521 / patch 519 is the proven Ghost baseline.

## Quest sequence checked against the installed Quest Helper

1. Inspect `Quest.SHEEP_SHEARER.getState(client)` and varplayer 179 on the
   client thread. Only `FINISHED` is completion proof.
2. At varp 0, speak to Fred the Farmer (NPC 732, 3190,3273,0). Start via
   “I'm looking for a quest.”, “Yes, okay. I can do that.”, and “Yes.”
   Confirm varp progression before leaving.
3. Obtain shears (item 1735) from Fred or a ground item in his house if absent.
   Enter the sheep pen through the live walker's gate route. Select a live
   sheep with a `Shear` action, excluding rams and the disguised penguins;
   require wool (1737) inventory growth after each shear.
4. Carry up to the free inventory slots worth of wool. The quest needs 20
   unnoted balls of wool (1759), but Fred accepts partial deliveries.
   Recalculate remaining as 20 for varp 1, otherwise `21 - varp` for
   varp 2..20; verify each hand-in by varp increase.
5. Walk to Lumbridge Castle, use upstairs staircase object 56230 at
   3204,3207,0 and verify plane 1. Use the spinning wheel object 14889 at
   3209,3212,1; inspect the live production interface for item 1759 (the
   installed helper highlights widget group 270 child 14). Click the live
   product choice once, then prove wool decrease and ball increase. When
   spinning finishes, descend via staircase 16672 at 3204,3207,1 and verify
   plane 0.
6. Return to Fred with unnoted balls. Choose “I need to talk to you about
   shearing these sheep!”, await observed varp/quest-state change, and repeat
   bounded gather/spin/turn-in batches until `QuestState.FINISHED`.

The [OSRS Wiki walkthrough](https://oldschool.runescape.wiki/w/Sheep_Shearer)
confirms 20 balls, Fred-provided shears, the sheep pen, the castle wheel, and
partial deliveries. Exact IDs and coordinates above came from the installed
client's Quest Helper bytecode and must be checked against live scene objects.

## Release and validation

Build 522 packages into cumulative patch 520 and a separate plugin artifact.
The first plugin load requires a cold RuneLite restart. The Supervisor restores
saved RuneLite bounds `(1376,124,820,702)` before login to preserve the OBS
scene. The loaded `RUNNING_BUILD` marker and fresh PID-matched status, rather
than the disk patch number, prove the running code. This quest has not yet been
run or verified.
