# Muse review verdict — Imp Catcher Build 22 (patch-592) — read-only

Filed 2026-09-30 20:20 EDT by the review-loop worker. Imp Catcher is Alex-owned; this is review only, nothing shipped.

## Ground truth
- version.txt = 592; commit da878788 "Build22: hand stalled stile route to full walker" (20:18:44 EDT)
- patches/patch-592.hot.json: build=22, patch=592, sha256=c66daea57485d627b62ec72b5a1cd0525aa78dc108334fad3c2cb2709c79df71
- SHA-256 EXACT-MATCH vs patches/impcatcher-22.jar (21299 bytes) — download-verified
- Numbering 591 → 592 clean; patch-592.zip root net/ correct (199 files), only additions META-INF/MANIFEST.MF (220B, unchanged) + version.txt — overlay-safe
- javap -p -c diff (Build 21 vs 22): exactly one class changed — ImpCatcherScript (inner classes untouched)

## What Build 22 does
New `recoverBlockedWalk(Pending, Frame)`: when the hand-rolled walkStep recovery path
(READY_ROUTE_STEP diag, Rs2Player.isMoving + Rs2Walker.walkStep) reports a stall, the script
hands the stalled route to the full blocking walker (`Rs2Walker.walkWithStateUntil`) and logs
`[ImpCatcher] WALK_RECOVERY_RESULT label={} state={} elapsedMs={} before={} after={} target={}`.
Arrival supplier compares before/after via Rs2Player.getWorldLocation().distanceTo (target
re-captured as a fresh WorldPoint inside the supplier).

## Verdict: PASS, no blocking defects
- Numbering, SHA chain, zip root, overlay safety all clean.
- The fix is coherent: Build 10's bounded walkStep recovery had no escalation on stall;
  Build 22 adds one. WALK_RECOVERY_RESULT gives the missing evidence the stile route needed.

## Carried nits / questions for Alex
1. Tick-block class now serves 6 paths (12/18/19/20/21 + this recovery). Each blocking
   walkWithStateUntil call can stall heldHeartbeat + status.properties publish up to ~30s —
   the same expired-status/OCR-misread class Builds 7–9 fixed.
2. Rs2Player.getWorldLocation() and Rs2Player.isMoving() are called off the client thread
   (supplier lambda + walkStep caller) — the Build 517 crash class. Build 21 resolved this
   on the NPC side (scan inside ClientThread.invoke); the player-side calls are still
   unverified (Microbot may cache them, unconfirmed). No crash evidence on record; flagging
   as the one open thread-safety question.

## Live status
- Screenshot feed dark ~156 min (newest commit b4e19333 / 17:44:02 EDT PIRATESTREASURE_DONE frame, already seen).
- Zero IMPCATCHER_* frames ever — structural (Build 7+ publishes status.properties only).
- Build 22 NOT live-observed. Acceptance: fresh RUNNING_BUILD=22, WALK_RECOVERY_RESULT
  lines with before/after, or first IMPCATCHER_* screenshot.
