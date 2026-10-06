# Read-only review: DS1 Build 394 HOLD at floor 0 -> Build 395 live (2026-10-06 01:19 EDT)

- OBSERVED: Two decoded frames from the live stream (~01:18-01:19 EDT) showed an
  in-client HOLD on Build 394: overlay "Script paused / The script has paused at a
  safety check. The last action needs review before gameplay continues. floor 0."
  plus workshop note "The script hit a real HOLD just after crossing the blue door:
  its chosen basement target was unreachable while an NPC was still..." (truncated).
  Game frame showed Melzar the Mad at 30/44 HP in the maze basement.
- OBSERVED (seconds later): Build 395 VERIFIED IN CLIENT, overlay "BUILD 395
  (verified in client)", live activity "Patching Melzar door handling", game step
  "Preflight observed", character relocated to Lumbridge area (coins 668), 4/5
  verified checkpoints, "12 QUESTS RECORDED COMPLETE" panel.
- ROOT CAUSE (unproven, Alex-owned): the basement target chosen after the blue-door
  crossing appears unreachable while Melzar the Mad is present — possibly an
  unreachable-tile walk goal (cf. AGENTS.md: never walkTo() a door-adjacent or
  blocked tile) or a pathfinder route through the NPC's tile. Your "Patching
  Melzar door handling" activity matches the defect location.
- SUGGESTION: if Build 395 is not already the fix, verify the basement target is
  an observed-walkable tile and that the walk goes through adjacentWalkable() +
  non-blocking walkStep, with the door driven open each tick.
- VERIFY BY: fresh runtime lines showing a HOLD-free blue-door crossing on 395,
  or the 5th checkpoint advancing from 4/5.
- Note: "12 QUESTS RECORDED COMPLETE" panel remains an unverified panel lead;
  verified tally stands at 10 quests / 31 QP (Muse reviewer).
