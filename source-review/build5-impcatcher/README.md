# Imp Catcher build 3 — implementation handoff to Alex

## Permission and ownership

Alex assigned this task to implement the separately selectable Imp Catcher plugin, compile it against the installed Microbot JAR, and hand him source/build output. I was permitted to edit ImpCatcherScript.java, ImpCatcherConfig.java, README.md, and Plugin.java if needed. I changed only the script and this README. Alex owns integration, release archives, deployment, loading, game runs, and ordinary debugging. I will help with persistent bugs only if Alex explicitly asks.

## Build 3 change

Build 3 preserves Build 2 quest behavior and writes `state=ERROR` plus the captured exception into runtime `status.properties` before stopping after an unclassified tick exception. If the status write itself fails, it logs `ERROR_STATUS_WRITE_FAILED`. This is a diagnostics-only change; it does not address the login screen or validate quest actions.

## Files

- `ImpCatcherScript.java`: build 2 quest state machine.
- `Impcatcher-plugin-2.jar`: all six plugin classes; candidate for Alex's integration.
- `Impcatcher-script-hot-2.jar`: script classes only; usable by the existing host after its first cold installation.
- `build2/`: local javac output.

No release archive, updater, manifest, hot_update.py, Pirate file, client, or game was changed.

## Behavior

The script observes quest state via the installed `Quest.IMP_CATCHER.getState(Client)` API, reads varp 160 as a separate diagnostic, and counts exact inventory IDs 1470/1472/1474/1476. It starts with Mizgog if the quest has not begun, otherwise gathers only missing beads from observed ground items/imp combat, then returns with all four. It walks with the installed Microbot walker, uses observed plane changes to confirm stair crossings, and verifies dialogue, combat, inventory, and movement changes before another action. Three unproved actions produce a HOLD with diagnostics. Critical HP, death, full inventory, unrecognized dialogue, and absent stair/NPC also HOLD. Status is written to `~/.runelite/impcatcher/status.properties`; startup log has `RUNNING_BUILD=2`.

The host's script-only hot reload retains a stable plugin class and can load `Impcatcher-script-hot-2.jar` after Alex installs the host. The script does not change existing release machinery.

## Build result and installed API

`javac` completed against `C:\Users\No 1\Desktop\New folder\microbot-tutorial-island.jar`. It reported only deprecation/removal warnings for older Microbot helper classes. Installed signatures checked with `javap`: `Quest.IMP_CATCHER.getState(Client)`, `Rs2Npc.attack(NPC)`, `Rs2Npc.interact(NPC,String)`, `Rs2GroundItem.exists(int,int)`, `Rs2GroundItem.pickup(int)`, `Rs2Walker.walkTo(WorldPoint)`, `Rs2GameObject.getTileObject(int/String)`, `Rs2GameObject.interact(TileObject,String)`, and `Rs2Dialogue` continue/options methods.

## First live validation for Alex

1. Integrate the plugin classes and confirm client startup log says `RUNNING_BUILD=2` and hot host reports build 2. A version file or archive name is insufficient.
2. Read the initial status and compare `questState`, `questVarp160`, position, HP, and bead IDs with the game scene. The quest-state API invokes client script 4029 with quest id 76; its private-server result is unverified.
3. Observe Mizgog start dialogue, staircase IDs/actions, and ground bead visibility. Confirm each pending action clears only after a real transition. Inspect `HOLD` messages before any code change.
4. Check completion with a visible quest completion screen, quest state, varp transition, bead consumption, and reward amulet/XP/QP. The script's `COMPLETE_QUEST_STATE` means only that the RuneLite quest-state API returned FINISHED; cross-check before claiming a full run.

Unknown until Alex's live run: private-server imp spawns/drop rates, tower stair variant and route, dialogue wording, quest varp values, and script-4029 fidelity. The Falador/Lumbridge/Draynor search coordinates are starting hypotheses, not measured spawns. The script currently HOLDs rather than banking or eating; those recovery paths require observed live inventory and route evidence.

## Build 5 status publishing recovery

Build 5 changes status telemetry to best-effort. A transient Windows sharing/replace denial no longer stops the quest tick; the writer uses a unique temporary file, preserves the last complete status file, logs a throttled warning, and retries on the next tick. Local compilation succeeded with the existing Microbot API snapshot and only the same deprecated-helper warnings. Runtime validation is pending hot load: confirm the status timestamp recovers and the script remains active after a controlled file-sharing collision, then verify the login detector reads the fresh same-PID Imp Catcher status.
