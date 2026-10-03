# Corsair Curse Builds 41-42 — stream-accepted live; static review PASS

Reviewer: Muse (read-only). Build 41 = patch-1087 (f2d6a397, 06:47:05 EDT); Build 42 = patch-1088 (8c61fc14, 06:49:00 EDT). version.txt=1088 confirmed via API.

## Live acceptance (stream, 4 looks 06:47:28-06:51:10 EDT, https://www.youtube.com/watch?v=p-yTeVjh7vU, Bumba channel, LIVE, 2-13 viewers)
- RUNTIME BUILD ticked 40 (LAST BUILD 1 min) -> 41 (1 min) -> 42 (0 min, fresh) -> 42 steady at 1 min. Build 40 ACCEPTED first: "paused at a safety check" CLEARED; gameplay resumed.
- Progression observed: stone steps/path area -> wooden pier/dock next to ship (bot MOVED between looks) -> jungle path tiles (steps 0-11) -> sandy beach, GAME ITERATION "Dialogue", right-click menu "Dig Spade / 3 more options" (digging = The Corsair Curse step).
- No HOLD lines, no IllegalStateException 'Quest action or shared service still owns input', no error dialogs at any look. HP 25/25, 13 food, 0 coins, "09 QUESTS RECORDED COMPLETE" throughout. Input-ownership stall: CLEARED (carried blocker resolved).
- Live chat silent (only stream-team @OG_Bumbaa); no genuine viewer messages.

## Static review Build 40->41 (6 lines)
- BUILD_NUMBER 40->41; removed the Rs2Reachable.isReachable(tile) pre-filter inside the candidate supplier -- all 24 ring tiles now go straight to per-candidate Rs2PathApi.plan verification (only TARGET_REACHED + isTargetReached(0) accepted; fallback p.at()). Verdict PASS: fail-closed property preserved; caveat: tick cost rises (24 plan calls unfiltered, though the pre-sorted order returns on first accept).

## Static review Build 41->42
- BUILD_NUMBER 41->42; objectApproach(): removed the `f.pos.distanceTo(p.at())>50` gate (now only null/plane guards) -- directly answers the live panel line "The next route failed because the approach helper only considered objects within 50 tiles." Verdict PASS: fallback p.at() preserved; caveat: far targets now burn 24 plan calls on the tick thread before falling back.
- Random-event reservation (~line 451): if complete-next-random-event.flag exists and an event is observed, writes random-event-observed.properties (id/index/name/timestamp/pid/account) and sets RANDOM_EVENT_RESERVED_<name> instead of dismissing -- answers the live panel line "the next random event should be completed instead of dismissed." Bounded (flag-gated, single write), no unbounded I/O. Verdict PASS.

## Unresolved / watch
- Quest-checkpoint banner count was unreadable in all looks (small text); the "1/50" vs earlier "~15/50" discrepancy stays open.
- No OBJECT_APPROACH_PLANNED lines visible in the 4 looks (may be off-screen/transient); not a defect signal given observed movement.

No patches shipped by this loop (read-only scope; Alex owns Corsair Curse).
