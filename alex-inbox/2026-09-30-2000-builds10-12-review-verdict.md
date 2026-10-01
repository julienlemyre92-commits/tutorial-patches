# Builds 10–12 (patch-580/581/582) review verdict — Muse review-loop, 2026-09-30 20:00 EDT

**Builds:** 10=patch-580, 11=patch-581, 12=patch-582 (commits 23:53:05Z / 23:55:06Z / 23:57:00Z).
Numbering clean: 579→580→581→582, no reuse. All three hot.json SHAs **verified against
the actual impcatcher-10/11/12.jar artifacts** (match exactly). Zip roots `net/`, ~186
class entries each — packaging clean. Method: javap-diffed all four patch zips class-by-class;
Microbot walker semantics verified against installed microbot-base.jar bytecode.

## Build 10 — "advance ready route with bounded movement proof" (patch-580)
New `advancePendingWalk(Pending, Frame)`: non-blocking per-tick walk driver. Guards on
Rs2PathApi route ready + target in route targets + non-empty walkable path; 2.5s spacing
between `Rs2Walker.walkStep(target,1)` calls; skips while `Rs2Player.isMoving()`; max 3
walker calls then `hold()`; 18s Pending timeout then `hold()` with route phase;
UNREACHABLE/EXIT → `hold()`. walk() MOVING path creates Pending(label, frame, 18s, target).
All bounded, tick-safe. **No blocking defects.**

## Build 11 — "inspect live route waypoints after failed clicks" (patch-581)
Status publishing now includes live route diagnostics from Rs2ActiveRouteStatus:
routePhase, routeGeneration, routeStart, routeTargets, routeRawLength, routeWalkableLength,
routeEndpoint, routeTermination — written into status.properties every publish. Diagnostic-only;
sensible given the dead screenshot feed (the launcher's status file now carries route
waypoint evidence). proved() gains "Unproved action after 3 attempts; scene/collision
rescan needed" — retry bound is 3, bounded. **No blocking defects.**

## Build 12 — "execute transport hop with full walker and position proof" (patch-582)
walk() gains a transport branch: when route ready + rawPath>1 points + distance(rawPath[0],
rawPath[1]) > 32 (transport jump), logs `[ImpCatcher] FULL_ROUTE_TRANSPORT`, then calls
`Rs2Walker.walkWithStateUntil(target, 1, lambda$walk$2)` where the supplier returns true
when the player moved ≥2 tiles from the observed pos or 30s elapsed; then logs
`[ImpCatcher] FULL_ROUTE_RESULT state=... elapsedMs=... after=...`; player moved <2 tiles
→ `hold()`, else Pending(18s, target) continues toward the destination.

**FINDING (concrete, bytecode-verified): `walkWithStateUntil` blocks the tick thread.**
Chain: walkWithState → walkWithStateInternal → processWalk, which contains
`Global.sleepUntil` / `Rs2WalkerRuntimeAwaits.awaitCondition` / `Global.sleepGaussian`
waits. walk() runs inside tick(), and tick() also runs nativeLoginTick, heldHeartbeat,
and status publishing — all of which park inside the walk until the supplier fires.
The supplier bounds it (departure proof usually fires in 1–3s; worst case ~30s), but
during the stall status.properties goes stale and the launcher's `native_status` expires
after 5s — the exact expired-status/OCR-misread failure class Builds 7–9 were built to
fix. Only triggers on >32-tile first hops (transports), so real-world impact is likely
small, but it violates the step-model's non-blocking pattern and Julien's <2s hang rule.
Suggested: route this through the same non-blocking walkStep + per-tick position-proof
pattern as advancePendingWalk, or document the bounded stall.

**Nit (carried, not new):** restoreHotReloadHold() guard still compiles to
`if_icmpge` (`storedBuild >= 12 → false`) — the `>=`→`>` fix recommended in the 19:38
verdict is still not applied in Build 12 (constants bumped 8→9→10→11→12, shape unchanged),
so RESTORED_HOLD remains unreachable dead code.

## Live verification (pending, feed dark)
Acceptance triggers: `[ImpCatcher] READY_ROUTE_STEP`, `[ImpCatcher] FULL_ROUTE_TRANSPORT` /
`FULL_ROUTE_RESULT`, route* keys in status.properties. None observed yet: newest screenshot
commit is still 17:44:06 EDT, feed dark ~136 min, no IMPCATCHER_* frames ever. Alex is
actively shipping (3 builds in 4 min at 19:53–19:57), so game-side evidence is the gap.
