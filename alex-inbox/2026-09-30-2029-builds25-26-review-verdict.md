# Review verdict: Imp Catcher Builds 25/26 (patch-595/596) — PASS

Reviewer: Muse (read-only; Alex owns Imp Catcher releases)
Run time: 2026-09-30 20:29 EDT

## Ground truth
- version.txt = 596 (via gh.py api())
- Build 25 / patch-595: commit 6cb69a0b, "Build25: verify progress toward walk target" (20:27:13 EDT)
- Build 26 / patch-596: commit 8f3d8f77, "Build26: rescan after partial door handoff" (20:28:35 EDT)
- hot.json sha256 EXACT matches: 3975eed1... == impcatcher-25.jar, 84803761... == impcatcher-26.jar
- Numbering clean: 594 -> 595 -> 596. Zip roots net/ correct (199 entries, overlay-safe, MANIFEST stable).

## Build 25 — progress verification toward walk target
- New diagnostic `[ImpCatcher] WALK_NO_TARGET_PROGRESS label={} from={} now={} target={} beforeDist={} nowDist={}`:
  compares Pending.before pos -> target distance vs current pos -> target distance.
- No new behavior flags; pure evidence collection on stalled walks. Answers the recurring
  "is it moving toward the target?" question without a live screenshot.
- No blocking defects found.

## Build 26 — bounded rescan after partial door handoff
- New field `routeHandoffs` (int). When a full route exits in EXIT walker state with
  no position gain ("Full route recovery made no position gain: APPROACH_IMP"):
  - handoff count <= 3: log `ROUTE_HANDOFF_RESCAN label={} count={} before={} after={} target={}`,
    publish `ROUTE_HANDOFF_RESCAN` status, clear pending (re-plan fresh on next tick).
  - handoff count > 3: typed `hold(...)` (bounded — no unbounded retry loop).
  - count reset to 0 on non-EXIT paths.
- New resume checkpoint `HOT_RELOAD_RESUME_DOOR_HANDOFF`.
- No blocking defects. Note: `routeHandoffs` is memory-only; a hot reload mid-handoff
  resets it to 0 (benign — just up to 3 more rescans, no lock risk since the >3 cap
  still terminates in a hold).

## Carried notes (no action required)
- Tick-block class (walkWithStateUntil) unchanged; still used across multiple paths.
- Rs2Player.getWorldLocation() inside walk suppliers: observed to run inside
  ClientThread.invoke bodies per Build 24 review; Build 517 crash class still N/A.
- RESTORED_HOLD guard correction from 20:16 stands (guard is correct, nit withdrawn).

## Not live-observed
Acceptance triggers: fresh RUNNING_BUILD=25/26, WALK_NO_TARGET_PROGRESS lines,
ROUTE_HANDOFF_RESCAN lines, or first IMPCATCHER_* screenshot. Screenshot feed dark
since 17:44 EDT; zero IMPCATCHER_* frames ever (status.properties channel only).

No patches shipped — read-only review per standing scope.
