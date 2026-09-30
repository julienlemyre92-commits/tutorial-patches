# Verdict: Imp Catcher Build 4 walkStep review (Muse, read-only)

Task from Alex (`2026-09-30-1804-build574-walker.md`): read-only check of the
installed `Rs2Walker.walkStep(WorldPoint, int)` behavior against Build 4's
`walk()` branch. No edits made. All claims below are from javap of the
installed `microbot-base.jar` (`Rs2Walker`, `WalkerState`, `WorldPoint`).

## Q1: Does the first normal path step return MOVING? — CONFIRMED YES

`walkStep(WorldPoint, int)` bytecode:
- Fresh target (`currentTarget == null || !currentTarget.equals(target)`):
  `setTarget(target)` then `return MOVING` — **no click is issued on this call**.
- Same target, route not ready (`!Rs2PathApi.getActiveRouteStatus().isReady()`)
  → `MOVING`. Player already moving (`Rs2Player.isMoving()`) → `MOVING`.
- Same target, route ready, player idle: `walkStepPathReachesTarget(...)`
  fails → `setTarget(null, "rs2walker:walkStep:no-walkable-path")` → `UNREACHABLE`;
  else `Rs2WalkerMovement.clickMiniMapOrFallback(...)` → `MOVING`.

So the first tick's `WALK_STEP state=MOVING` means "target registered, route
computing" — not "motion started". The physical minimap click lands on a later
walkStep call (route-ready tick, +1 tick at 0.5–2s tick delay). Your per-tick
re-issue + position-proof model (`distanceTo(target) <= 4 ||
distanceTo(before.pos) >= 2`) is compatible with this; no defect. Just don't
read the first MOVING log line as motion proof — your proof already doesn't.

## Q2: Is radius 1 suitable? — YES, keep it

Radius feeds three checks: arrival (`distance <= 1` Chebyshev +
`getReachableTilesFromTile(playerLoc, 1)` contains target), and
`walkStepPathReachesTarget(walkablePath, target, 1)`. Arrival-adjacent is the
honest semantic for talk/pickup/attack-followup objectives; your proof
threshold (<=4) is looser than the walker's (<=1), so no conflict.

## Evidence-backed concerns (report only, no edits)

1. **EXIT is a single point of failure → HOLD.** walkStep returns EXIT when
   ShortestPath `config == null`, when called on the client thread, or when
   `InputArbiter.isHuman()`. Your walk() maps EXIT → diagnostic HOLD, which is
   the right handling — but if the ShortestPath plugin/config is missing from
   the portable bundle, EVERY walk HOLDs on first use ("Walker EXIT for
   WALK_TOWER ..."). The tutorial bot used Rs2Walker.walkTo for months so it's
   almost certainly present, but Build 4 has never moved, so this is unverified.
   One-line pre-flight: confirm ShortestPath is installed/enabled.
   (Your tick runs on the script executor, not the client thread, so the
   client-thread guard won't trip from walk(); the isHuman EXIT → HOLD on
   Julien grabbing the mouse is correct behavior, not a bug.)
2. **ARRIVED-on-unwalkable-target edge.** If the target tile itself isn't
   walkable (door tile, wall-adjacent object tile) but is within Chebyshev 1,
   walkStep returns ARRIVED immediately → your walk() HOLDs "already at target
   but action is still unproved". For Imp Catcher targets (tower door tile,
   imp/NPC tiles) prefer verified-walkable target tiles — the tutorial bot's
   `adjacentWalkable` lesson. If a HOLD ever names this message, step the
   target to an adjacent walkable tile rather than raising the radius.
3. **UNREACHABLE → HOLD is correct**, but it keys off the *walkable* path:
   radius 1 demands the path end within 1 of the target. If a HOLD names
   UNREACHABLE, the fix is an adjacent walkable target, not a larger radius.
4. **Plane guard verified fine.** `WorldPoint.distanceTo` returns MAX_VALUE
   across planes (confirmed: delegates to `distanceTo2D` = `max(|dx|,|dy|)`,
   Chebyshev). You already pass `f.pos.getPlane()` for TOWER; NPC tiles carry
   their own plane. No defect.
5. **First-click latency.** Target-set tick → route-ready tick → click tick =
   2+ ticks before the first physical click at 0.5–2s tick delay. The 18s
   pending timeout absorbs it. No defect.

## Verification status

Cannot verify live — consistent with your LIMIT. My observable sources: newest
screenshot commit 21:44:06Z (17:44:02 EDT, stale Pirate DONE frame), no new
frames since (~23 min dark); version.txt=574 confirms Build 4 shipped
22:01:05Z. Your `client.log` RELOAD_APPLIED build=4 / PID 37672 claim is noted
but not independently observable from my side — flagging the gap, not
disputing it. Expectation stands: after manual login, first
`WALK_STEP ... state=MOVING` = target registered (no click yet); click and
tile progress follow on subsequent ticks. Noted: off-client NPC-call theory
withdrawn, no action.
