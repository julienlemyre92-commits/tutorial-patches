# The Restless Ghost Microbot plugin

Separate quest plugin for the existing Microbot 2.6.18 client. Its package is
`net.runelite.client.plugins.microbot.restlessghost`; the Cook's Assistant and
Tutorial Island plugin classes remain separate.

## Installed-client quest sequence

The installed Quest Helper maps progress varplayer 107 as follows:

| Varp 107 | Required observation and action |
| --- | --- |
| 0 | Start with Father Aereck (NPC 2812) in Lumbridge church. |
| 1 | Speak to Father Urhney (NPC 923) and obtain the ghostspeak amulet (item 552). |
| 2 | Equip the amulet; open/search the graveyard coffin (2145/15061), then speak to the restless ghost (NPC 922). |
| 3 | Enter the Wizards' Tower basement by ladder 2147; search altar 2146 for the skull (item 553). A level 13 skeleton spawns; escape by ladder 2148. |
| 4 | Return to Lumbridge, reopen the coffin if it closed, and use the skull on the open coffin. |

Only `Quest.THE_RESTLESS_GHOST.getState(client) == FINISHED`, queried on the
client thread, proves completion. Varp 107 and skull varbit 2130 are progress
signals. Item removal, a click, proximity, or elapsed time are not completion
proof.

Installed Quest Helper reference: `questhelper.javap.txt` in this directory,
decompiled from the exact client JAR. The open coffin may have other scene
variants, so the script must verify the live object and its action.

## Release checkpoint

Build 520 is reserved for the first release of this plugin. Cook's Assistant
Build 519 is the currently loaded prior build; its unshipped Build 520 source
draft is not a release. The new plugin classes will be added to the existing
client JAR through the Supervisor's cumulative patch stream. This first load
requires one client restart; the Cook's Assistant and Tutorial Island classes
remain in the JAR. Runtime quest verification is pending until the new plugin
is loaded and tested.

Patch 517 had a cold-update packaging error: its replacement launch manifest
omitted `Main-Class`, causing `java -jar` to exit before RuneLite started.
Patch 518 restores the validated manifest from the pre-update JAR while keeping
the same Build 520 quest classes. Runtime verification remains pending.
