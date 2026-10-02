# Muse read-only review — Below Ice Mountain Build 19 (patch-884)

Reviewed 2026-10-02 00:00-00:0X EDT. Alex owns implementation/releases; this review is read-only. No shipping action taken.

## Verdict: PASS WITH FINDINGS

## Custody: AIR TIGHT
- patch-884.zip (commit a60af50be8, shipped 2026-10-02T03:58:52Z): 277 entries, net-rooted (274 net/ + META-INF/ + MANIFEST + version.txt), in-zip version.txt=884 == repo version.txt.
- Versions 883->884 sequential, no reuse. Patch-884 not overwritten.
- BUILD_NUMBER=19 in published source AND javap-compiled class.
- 7/7 script classes byte-identical: patch-884.zip <-> patches/belowicemountain-19.jar (jar sha256 243aa6c6c86f6eb605b89337ef1f102deb74a4d836f326b0ef06ac73ab84aa62).
- Verified via published source (source-review/belowicemountain-build19/); diff 18->19 confirmed surgical.

## Change 18 -> 19: "short-route in-flight grace"
Route progress gate in walk() (verified against published source):
1. `Route` gains `volatile long doneAt`, set in the worker thread's finally alongside `done=true` — tick-side evaluation now waits `route.done && now-doneAt>3000` (3s in-flight grace) before measuring, so the measurement isn't taken while the walker thread just reported done but the player tile hasn't settled.
2. Progress threshold becomes proportional to route length: `needed = before<=15 ? 1 : 5` (before = start-to-target distance). Short routes (the meat-loot hop pattern that killed Build 17/18's evaluation) only need 1 tile net Manhattan progress; long routes keep the 5-tile bar.

This directly addresses Build 17/18's failure class: Build 18's exact-tile loot walked to distance 1, but the old gate would count near-zero net progress on a short route as a stall. The 3s grace avoids mis-measuring a just-finished route as stalled on the very next tick.

## New findings
- LOW BIM19-1: `needed`/`before` derive from the route's ORIGINAL start tile (captured at route creation), so across cancelRoute->restart retries the measurement window stays anchored to the original start — consistent, but a route that made progress then got re-keyed measures from a stale origin. Bounded (HOLD after 2 stalls regardless).
- INFO BIM19-2: doneAt is recorded in finally, so the 3s grace applies even to interrupted/timeout routes — harmless (timeout path already implies done).
- INFO BIM16-4 STILL CARRIES: source-review/belowicemountain-build19/README.md is the stale Build-1 handoff text again (only documents builds 1-2). Reviewers cannot rely on the README for build intent; the commit message carries it.

## Carried
- LOW BIM2-2 (Mining-10 gate), LOW BIM7-1 (Manhattan net-progress false-positives on detours -> terminal HOLD), LOW BIM7-2 (5-tile threshold not proportional — PARTIALLY addressed by the 1/5 split, still absolute per-band), LOW BIM16-1 (dungeon coords unverified), LOW BIM16-2 (guardian/pillar staged), LOW BIM18-1 (raw ground tile, no walkability filter — still bounded by 2-fail HOLD).
- INFO BIM2-4, BIM14-1, BIM16-3, BIM17-1, BIM18-2.

## Live acceptance: PENDING
Screenshot feed dark ~30h (last: 2026-09-30 17:44 EDT). Last live stream observation 2026-10-01 23:56-23:58 EDT showed RUNTIME BUILD 17/confirmed in-game. Build 19 was armed ~00:00 EDT; watch for RUNNING_BUILD=19 / new ROUTE_PROGRESS diag lines. The 3s grace and 1-tile threshold should show up in the diag as ROUTE_PROGRESS with small deltas on short hops.
