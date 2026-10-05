# Dragon Slayer I 32-QP gate — verify before burning more builds (2026-10-04 23:17 EDT)

Review note (Muse, read-only reviewer). No code touched; flag only.

## The gate
Dragon Slayer I cannot start below **32 Quest Points** — the Champions' Guild
door (Guildmaster at the south entrance) refuses entry with fewer than 32 QP.
Wiki-verified 2026-10-04. This is a server-side start gate, not something the
script can work around.

## Why this matters now
- Last **verified** runtime tally: **10 quests / 31 QP** (Corsair Curse
  completion declared 2026-10-03; its own Congratulations scroll never observed).
- The overlay panel currently claims "12 QUESTS RECORDED COMPLETE" / ~32 QP,
  naming Imp Catcher and Demon Slayer. This is an **unverified panel lead**:
  its own math does not reconcile (31 + Demon Slayer's 3 QP = 34, not 32),
  and no QuestState / quest-point counter / completed-quest list from the live
  game state corroborates either quest.
- If the account truly sits at 31 QP, any Dragon Slayer build will stall at the
  Guildmaster on quest start. Please do not ship further Dragon Slayer builds
  against that assumption.

## Recommended action
Have the script read the **game's own quest-point counter** (Quest tab) and log
it before continuing the Dragon Slayer front. One in-game QP read resolves both
the tally dispute and the gate question:
- If QP >= 32: proceed; gate is clear.
- If QP = 31: the mission needs one more QP first — either verify Imp Catcher /
  Demon Slayer completion for real (QuestState, not the panel), or run a quick
  1-2 QP quest before returning to Dragon Slayer.

## Secondary watch item
Even with the gate cleared, combat readiness is the next wall: this account's
profile is low-combat, and Dragon Slayer I requires a level-82 lesser demon
(Melzar's Maze) plus Elvarg at level 83. A training/combat plan will be needed
or the bot stalls at the maze/dragon fight.

## Publication status
- 2026-10-04 23:18–23:20 EDT: three PUT attempts to repo alex-inbox/
  (2026-10-04-2317-dragonslayer-32qp-gate.md) all returned HTTP 403 while reads
  (whoami, contents) work fine. Not transient flapping — persistent write
  denial for this token path. Do NOT hammer; retry via review loop on later
  runs, and rely on Julien relaying to Alex in the meantime.
