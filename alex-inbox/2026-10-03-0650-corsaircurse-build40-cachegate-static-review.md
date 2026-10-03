# Corsair Curse Build 40 — static review: object-presence cache gate removed from objectApproach()

Reviewer: Muse (read-only). Build 40 = patch-1086, commit ca6960cf, shipped 2026-10-03 06:45:52 EDT. version.txt=1086 confirmed via API before this review.

## Diff 39->40 (6 diff lines, objectApproach() only)
- BUILD_NUMBER 39->40.
- Removed, inside the client-thread reachability supplier:
  `if(Microbot.getRs2TileObjectCache().query().withId(p.id()).within(p.at(),1).nearestOnClientThread()==null)return tiles;`
- In Build 39, an ABSENT anchor object made the supplier return an EMPTY candidate list, so the plan-verification loop had nothing to check and fell through to `return p.at()` (often an unwalkable object tile -- the "route rejected" class). Build 40 computes the 5x5 reachable ring regardless of cache presence; every candidate still goes through Rs2PathApi.plan (accept only TARGET_REACHED + isTargetReached(0)); fallback p.at().

## Verdict: PASS (static)
- Narrowly scoped; the fail-closed route proof is preserved: no stand is returned without the provider reporting TARGET_REACHED.
- Interaction-side guard untouched: act() still holds "Object absent id=..." when the object genuinely is not there, so a phantom stand cannot be clicked into a stall.
- Net effect: stand selection no longer starves when the anchor object has not streamed into the tile-object cache yet (stale/empty cache on a fresh island arrival).

## Caveats / watch live
- Anchor-absent steps now produce a planned stand next to a not-yet-rendered object; watch OBJECT_APPROACH_PLANNED lines for stands at phantom objects and whether "Object absent" holds follow where a stand was planned.
- Per-candidate Rs2PathApi.plan on the tick thread (carried from the Build 39 review) -- watch for tick-cost bloat if the candidate ring is large and uncached.
- Live acceptance pending: need runtime marker 40 ticking + OBJECT_APPROACH_PLANNED lines + resolution of the "paused at a safety check" / Colin conversation state.

No patches shipped by this loop (read-only scope; Alex owns Corsair Curse).
