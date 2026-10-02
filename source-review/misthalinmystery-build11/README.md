# Misthalin Mystery handoff

Owner: Bot Maker 2 built this isolated plugin. Alex owns integration, deployment,
live controls, and gameplay validation. The source is compiled against the
installed `C:\Users\No 1\Desktop\New folder\microbot-tutorial-island.jar`.
The Quest Helper route was read at commit
`75b623a6bc14237831fddc10b44765c0910a4eb0` and the [OSRS Wiki quest
guide](https://oldschool.runescape.wiki/w/Misthalin_Mystery) was used for the
mirror orientation and final reward-slot rule.

## Deliverables

- `MisthalinMysteryPlugin.java`: separate plugin, exclusive quest input,
  script-only hot reload host.
- `MisthalinMysteryConfig.java`: actions disabled by default.
- `MisthalinMysteryScript.java`: Build 2 step map, telemetry, bounded
  action/proof loop, native login, bank/food prep, mirror candidate sensing,
  completion logout.
- `MisthalinMystery-build1.jar`: initial full plugin for guarded installation,
  SHA-256 `1CA9F244DF0044DD3E1201920172FEB75AAA1F80B551F62D918739A8D14EBD32`.
- `MisthalinMystery-build2-script.jar`: script-only hot swap,
  SHA-256 `3FDC6989F346987E8E340C0A5ADCBC2A56C165A0C5ED1EEE0ED38396DE2856C9`.
- `build2-javac.err.log`: compiler output. The compile had no errors; warnings
  are from deprecated Microbot helpers still present in this installed JAR.

## Live enable sequence

1. Alex installs the full plugin with actions disabled, then hot swaps the
   Build 2 script in the same PID.
2. Confirm a current `RUNNING_BUILD=2` startup marker and fresh
   `%USERPROFILE%\.runelite\misthalinmystery\status.properties`. Confirm
   `pid`, `quest=NOT_STARTED`, `varp=0`, `hp=11`, `maxHp=11`, current
   inventory, and `actionsEnabled=false`.
3. The config checkbox may enable actions. For a file-based guarded enable,
   write `%USERPROFILE%\.runelite\misthalinmystery\control.properties`
   with `expectedPid=<live PID>`, `expectedBuild=2`, and
   `enableActions=true`. A wrong PID or build leaves the script status-only.
   Remove or set false to stop new quest input.
4. The plugin verifies `QuestState.FINISHED` before logging out. It writes
   `completed.flag` in its status directory and suppresses native relogin
   after completion, including a plugin restart. A different account logged
   in with a non-finished state clears that stale marker.

## Stage map

| Live varp | Planned action and proof |
| --- | --- |
| 0, 5 | Abigale dialogue; option `Yes.`; boat after stage 10 |
| 10–25 | Bucket, barrel cutscene, empty/search barrel, manor key and door |
| 30–45 | Knife, pink door, read clue 1, slash/search painting, ruby door |
| 50–60 | Shelves tinderbox, four candle varbits, barrel fuse, exit room |
| 65–80 | Damaged wall, Lacey/tree and clue 2, piano D-E-A-D, emerald key, return |
| 85–105 | Diamond door, clue 3, fireplace, S-D-Z-E-O-R gems, sapphire key/door |
| 110, 111 | Unique wardrobe telegraph, mirror tile and facing verified by a push |
| 115–130 | Reveal, ground knife/equip, Fight Abigale, exit door, Mandy |
| FINISHED | Quest state proof, safe logout, relogin suppression |

Every dispatched interaction waits for a later varp, inventory, varbit,
position, widget, equipment, mirror, or dialogue change. An unproved action
holds with diagnostics instead of entering a click loop. Route movement uses
the installed walker and position progress; stage changes cancel old routes.
The starting bank visit deposits excess items and withdraws four available
food in one visit. The account currently has four trout, so no purchase is
needed for the first run. If food is absent from inventory and bank, the
plugin holds at the bank and reports the deficit.

## Validation gaps

Only compilation and status-only preflight were reported at handoff. **No
quest stage has been live-tested with Build 2.** In particular:

- The mirror wardrobe cue has no confirmed graphic ID in this client. The
  script accepts a unique open-wardrobe object, a killer NPC at a wardrobe,
  or a unique graphic near one wardrobe observed across two samples. It
  records all nearby graphic IDs and wardrobe/mirror coordinates under
  `mirrorSignal`, `graphics`, `mirrorTile` and `mirrorCueWardrobe`.
  If the cue is absent or ambiguous, it holds. Capture a fresh screenshot
  and matching status at varp 110/111; use those to identify the actual
  telegraph before claiming the mirror solver works.
- The wiki requires a push **toward** the active wardrobe even when the
  mirror is already in its row or column. Build 2 implements this orientation
  rule and verifies mirror movement, but it needs live proof.
- The private server's damage and death handling are unknown. Low HP with
  food eats first. Low HP without food on the island attempts the sapphire
  door/boat retreat. Death recovery makes one `Rs2Death.recoverItems()`
  attempt and requires the live death state to clear. A blocked boss exit,
  unproved grave recovery, or absent food remains an explicit HOLD.
- Verify object actions and puzzle widget visibility live. The compiler
  proves API shape, not that each object is present at the predicted tile.
- Completion needs a fresh `QuestState.FINISHED` and safe logout observation.
  It has not been claimed.

These gaps call for the normal Alex loop: one live stage, inspect matching
timestamp/status and client log, focused script-only hot swap, then resume.
