# X Marks the Spot Microbot plugin (candidate Build 531)

This is a separate RuneLite/Microbot plugin. Build 530 loaded in the live
client and revealed a first-route failure before quest progress. Build 531
contains a focused recovery change; its runtime result remains unverified.

## Installed-client quest evidence

The installed `microbot-tutorial-island.jar` contains
`Quest.X_MARKS_THE_SPOT` and Quest Helper's `QUEST_X_MARKS_THE_SPOT` varbit
**8063**. `questhelper.javap.txt` in this folder is a bytecode listing from
that installed JAR. Its `loadSteps()` maps:

| Varbit | Required observed action | Quest Helper coordinate |
| --- | --- | --- |
| 0–1 | Talk to Veos (NPC 8484) at The Sheared Ram in Lumbridge; known dialogue choices only | (3228,3242,0) |
| 2 | Stand on tile and use spade's **Dig** inventory action north of Bob's house | (3230,3209,0) |
| 3 | Dig behind Lumbridge Castle kitchen | (3203,3212,0) |
| 4 | Dig northwest of Draynor jail by the wheat field | (3109,3264,0) |
| 5 | Dig in Draynor Market pig pen; obtain ancient casket 23071 | (3078,3259,0) |
| 6 | Bring casket to Veos (NPC 8484 or 8630); if casket is absent, recover it at the pig pen first | (3054,3245,0) |
| 7 | Speak to Veos on the Port Sarim dock again if the quest is not FINISHED | (3054,3245,0) |

The helper requires spade 952, which it says can be purchased at Lumbridge
General Store. The [OSRS Wiki walkthrough](https://oldschool.runescape.wiki/w/X_Marks_the_Spot)
corroborates the sequence, warns of aggressive jail guards, and says there
are no quest enemies to defeat. The [Lumbridge shopkeeper stock](https://oldschool.runescape.wiki/w/Shop_keeper_(Lumbridge))
lists NPC 2813, spade stock 5, and price 3 coins. The store's approximate
route tile (3212,3246,0) comes from older map data and needs live verification.

## Action rules

- Every dialogue, item, shop, bank, and dig action creates a pending proof
  checked on later ticks. A client API's `true` return is recorded as an
  attempt. Digging requires **exact player tile equality** first and a later
  varbit increase (or, for stage-6 casket recovery, inventory growth).
- One pending action runs at a time. Three unproved attempts enter `HOLD`
  with a diagnostic; unknown dialogue choices do not get guessed.
- The installed Microbot walker handles collision routes and doors. It runs
  on a supervised worker with a 15-second completion condition per segment,
  a 20-second no-progress watchdog, and a 240-second route budget. A cancel
  interrupts and clears on a separate thread, then waits for both threads to
  stop before another game action. If a route fails, a nearby reachable canvas
  move is attempted once and must produce a later position change. A completed
  walker call alone never proves arrival; the observed player location does.
- After the casket leaves inventory near Veos, a 20-second hand-in latch
  waits for varbit 7 or `QuestState.FINISHED`. It does not immediately re-dig.
- Missing spade can be withdrawn from Draynor bank or bought with coins at
  Lumbridge General Store. If needed, one starter shortbow/bronze axe/bronze
  sword may be sold. If none of these sources is available, the script holds
  explicitly. The exact store interaction must be checked live.
- At low health, available food is eaten with next-tick inventory proof. With
  no food and health at or below 30%, the script holds to prevent a death by
  nearby guards. No combat is required by the quest.
- Small randomized delays occur only **after observed proof**. Observation,
  pending verification, and stall limits still run on every tick.

## Local compile

```powershell
javac -Xlint:deprecation -cp 'C:\Users\No 1\Desktop\New folder\microbot-tutorial-island.jar' `
  -d 'work\xmarks\build531' `
  'work\xmarks\XMarksConfig.java' 'work\xmarks\XMarksPlugin.java' 'work\xmarks\XMarksScript.java'
```

Compiled successfully against the installed JAR on September 30, 2026.
Only three deprecation warnings (`WidgetInfo`, `Client.getWidget(WidgetInfo)`,
`InventoryID`) appeared. Build 530's first walk from `(3189,3274)` to Veos
failed its first scene/minimap click without player movement, and its 3-second
cancel join entered HOLD even though the worker eventually exited. Build 531
keeps route ownership while cancellation finishes and verifies a nearby move
before retrying. Exact Dig/shop/dialogue behavior remains to be tested.
