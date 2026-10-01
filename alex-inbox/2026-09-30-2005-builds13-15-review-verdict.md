# Builds 13–15 (patch-583/584/585) review verdict — Muse review-loop, 2026-09-30 20:05 EDT

**Builds:** 13=patch-583 (23:59:55Z), 14=patch-584 (00:01:18Z), 15=patch-585 (00:02:36Z).
Numbering clean: 582→583→584→585, no reuse. All three hot.json sha256 values **verified
against the actual impcatcher-13/14/15.jar artifacts** (exact matches). Zip roots `net/`
(same 5-class set: ImpCatcherScript + Frame/HeldFrame/LoginFrame/Pending). Method:
javap-diffed 12→13→14→15.

## Build 13 — "approach tower before scanning stairs" (patch-583)
reachMizgog gains a Chebyshev-distance gate vs TOWER: if max(|dx|,|dy|) > 4 →
`walk(frame, (TOWER.x, TOWER.y, playerPlane), "WALK_TOWER")` and return; the stair scan
(id 12536 → 12537 → name "Staircase", then "Climb-up" interact → Pending CLIMB 9s) only
runs within 4 tiles. New diag `HOT_RELOAD_RESUME_PREMATURE_STAIR_SCAN pos={} target={}`
in the hot-reload resume path (fixes stair-scan-before-arrival on resume). Retired two
Build 10/12 strings: "No position gain after 3 ready-route walk steps: WALK_TOWER" and
"HOT_RELOAD_RESUME_TRANSPORT_ROUTE firstHop={}" — the transport-resume branch is gone;
the WALK_TOWER approach now rides on Pending timeouts (CLIMB 9s) instead of the
3-attempt position-gain bound. The approach path still uses non-blocking walkStep
(`walk` verified walkStep-calling, no walkWithStateUntil inside). **No blocking defects.**

## Build 14 — "inspect live tower traversal objects" (patch-584)
New method `scanNearbyTraversalObjects(WorldPoint)` + client-thread lambda
("client-unavailable" fallback). Pure-diagnostic: enumerates nearby traversal objects
matching door/ladder/stair/climb/open keywords — body verified to contain zero
putfield/hold/Pending (no state mutation). Called from the HOLD status publisher: when
the held position is within 20 tiles of TOWER, the result is published as the
`nearbyTraversalObjects` property in status.properties. Sensible given the dark
screenshot feed — the launcher status file now carries the traversal evidence.
**No blocking defects.**

## Build 15 — "enter tower through live door before stair scan" (patch-585)
New ENTER_TOWER step: when no stair is visible AND player.x < 3110 (outside, west of the
door), calls new `walkFullRouteTo(frame, (3111,3166,0), "ENTER_TOWER")` — interior target
on plane 0 — then rescans stairs on the next tick. New hold reason "No staircase
visible after entering Wizards Tower" (player.x ≥ 3110 but still no stairs). New diag
`HOT_RELOAD_RESUME_TOWER_DOOR pos={}`; FULL_ROUTE_RESULT gains `before={}`. The old
blocking `walk` (lambda$walk$2) was consistently renamed to `walkFullRouteTo$2`.

## Findings / questions for Alex
1. (question, not defect) "through live door": I see no door 'Open' interact in the
   ENTER_TOWER path — entry relies on the full walker crossing the (possibly closed)
   door. If Rs2Walker's door handling does not auto-open this door, ENTER_TOWER ends
   with position ~unchanged and loops: stairs still invisible → x<3110 →
   walkFullRouteTo again (~30s per attempt). Consider a door interact ("Open") before
   the full route, and/or cap ENTER_TOWER attempts before HOLD.
2. (carried from Build 12 verdict) walkFullRouteTo blocks the tick thread
   (walkWithStateUntil, ~30s supplier-bound) — stalls heldHeartbeat + status publish
   for the duration. Now also serves ENTER_TOWER, not just the transport hop.
3. (carried nit) restoreHotReloadHold `>=` guard still dead code in 13–15 (byte-identical
   shape); RESTORED_HOLD reachability still depends on host-written request.properties
   (PC-side, unverifiable from here).
4. Build 13 retired the "No position gain after 3 ready-route walk steps" guard —
   WALK_TOWER stalls are now covered only by Pending timeouts; acceptable but the
   3-attempt early-exit for movement-without-progress is gone.

Live acceptance: RUNNING_BUILD=13/14/15, `nearbyTraversalObjects` in status.properties,
ENTER_TOWER + FULL_ROUTE_RESULT lines. Screenshot feed dark ~140 min — nothing
verified live. This loop remains read-only for Imp Catcher; nothing shipped.
