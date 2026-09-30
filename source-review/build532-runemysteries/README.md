# Rune Mysteries Microbot plugin — Build 532

This is a separate RuneLite plugin for the installed Microbot client. Its
source is `RuneMysteriesPlugin.java`, `RuneMysteriesConfig.java`, and
`RuneMysteriesScript.java`. The standalone class JAR is
`RuneMysteries-plugin-532.jar`; the cumulative Supervisor update is
`patch-530.zip`, based on the validated X Marks patch 529.

Quest Helper in the installed client maps Rune Mysteries varp 63, stages 0–5.
The script visits Duke Horacio, Archmage Sedridor, Aubury, and Sedridor again.
It handles castle stairs and the Wizards' Tower basement ladder with later
plane/region proof. It checks air talisman 1438, research package 290, and
research notes 291; a consumed quest item near its recipient is held for a
server stage update before any recovery trip. Only
`Quest.RUNE_MYSTERIES.getState() == FINISHED` marks completion. See
[`RUNE_MYSTERIES_STEPS.md`](../release/RUNE_MYSTERIES_STEPS.md) for exact
steps and sources.

Actions use the Microbot click and walker APIs. Every click/dialogue action
gets a later state proof; three unproved attempts stop in HOLD with status and
log evidence. The walker runs on a supervised route thread with bounded
cancellation and later position proof. Modest timing variation happens after
proof; it cannot hide a stalled action. The script pauses when another quest
plugin owns input or when Microbot's human-input arbiter is active. It checks
health and uses available inventory food before continuing at low health.

To run in the established test environment, select Rune Mysteries as the only
quest plugin in the active profile (`runelite.runemysteriesplugin=true`, other
quest-plugin flags false), write `runemysteries` to `.runelite/bot-mission.txt`,
then let the established Supervisor install patch 530 and launch RuneLite.
The stream dashboard reads `.runelite/runemysteries/status.properties` and
the capture helper records game-only PNG/TXT evidence. Verify the fresh
`[RuneMysteries] RUNNING_BUILD=532` line and matching PID before interpreting
the status. A patch marker or source file alone does not prove the loaded build.

The active character's Rune Mysteries start state was unknown before the first
live load. The long route to Aubury may approach aggressive dark wizards at
Varrock's southern entrance; live position/health evidence decides whether
a safer waypoint or teleport branch is required. A second fresh full run needs
another unstarted character or a private-server reset facility.
