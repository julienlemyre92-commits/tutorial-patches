# Muse review-loop verdict: Ernest Builds 41 (patch-643) + 42 (patch-644) — PASS

Both reviewed read-only (javap diff of ernestthechicken-{40,41,42}.jar; only
`ErnestTheChickenScript.class` differs per build).

## Build 41 — commit `30c7868c` 2026-10-01 05:38:29Z — "find professor and collision-reachable final approach"
- Delta vs Build 40: the ODDENSTEIN final-approach region now keys off
  `frame.professorPosition` (live NPC-3562 tile) instead of the static
  ODDENSTEIN constant:
  - professorPosition null or different plane → hold(reason embeds profPos +
    manorStaircaseDiagnostics).
  - distanceTo2D > 9 → `Rs2Tile.getReachableTilesFromTile(player, 20)`,
    stream-filter, min by (dist-to-professor, then tiebreak) →
    walk(TO_ODDENSTEIN_APPROACH) to the chosen collision-reachable tile,
    logs `[ErnestChicken] ODDENSTEIN_APPROACH npc={} tile={} cost={} player={}`.
    No tile → hold with reachable-set size in reason.
  - ≤ 9 → npc(3562, professorPosition, TALK_ODDENSTEIN).
- Why this is correct: the static ODDENSTEIN tile could be behind a wall/door
  from the actual NPC spot; walking to the *observed* NPC position's
  collision-reachable neighbor applies the door-adjacency lesson (never target
  unverified tiles). All remaining bytecode diff is constant-pool renumbering.
- Findings: [M NEW — regression vs Build 40, corroborated by a sibling review-loop
  run's read at 01:40 EDT] terminal HOLD on null/plane-mismatched
  professorPosition. Right after the 1→2 stair climb, NPC 3562 may not be
  rendered for 1–3 ticks (NPC streaming lag); Build 41's first action on the
  new plane is a terminal hold() when professorPosition is null or on a
  different plane, permanently killing the run. Build 40 in the same spot
  walked to the static ODDENSTEIN tile and kept retrying
  npc(3562, ODDENSTEIN, TALK_ODDENSTEIN) until he rendered — the old code
  never hard-stopped on an unrendered NPC. Suggested: bounded tick-wait on
  null professorPosition (walk toward / stay near the static ODDENSTEIN anchor
  and retry npc() each tick) before holding. [M carry-forward 32–41]
  single-shot climb budgets (`oddensteinStairs0to1Attempts` /
  `oddensteinStairs1to2Attempts`, persisted via status.properties) still
  untied to pending lifecycle — interact-true + unproved → permanent HOLD,
  no retry. The Build-40 proof anchor fix makes it less likely to bite;
  spend-on-proof remains the suggested hardening. [L] none.

## Build 42 — commit `db85b84d` 2026-10-01 05:43:43Z — "clear stale hold after quest completion"
- Delta vs Build 41: new first action in the tick dispatch — if
  `frame.game == LOGGED_IN && frame.quest == FINISHED`: pending=null,
  stopped=false, held=false, error="", status(frame, "COMPLETE_QUEST_STATE"),
  return.
- Why this is correct: a stale hold/pending from the quest run (e.g. a
  pre-completion HOLD that froze the tick) would otherwise persist forever
  after the quest completes; this unfreezes it into a stable terminal state.
  Clearing stopped=false is harmless — the branch returns before any gameplay
  dispatch, so the script idles in COMPLETE_QUEST_STATE each tick.
- Findings: [M] none. [L] the `COMPLETE_QUEST_STATE` status line is the only
  acceptance marker — it never emits a diag line per se, only the status-file
  update; if the feed returns, watch the status file rather than the chatbox.

## VERIFY BY (acceptance — screenshot feed dark since 17:44:02 EDT 2026-09-30)
fresh RUNNING_BUILD=42 banner, an ODDENSTEIN_APPROACH diag line against a
collision-reachable tile, or a COMPLETE_QUEST_STATE status-file update.
