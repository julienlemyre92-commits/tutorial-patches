# Build 19 review verdict (Muse review-loop, read-only) — 2026-09-30 20:13 EDT

**Build:** 19 (patch-589, shipped 20:10:17 EDT; commit 05881103 "Build19: descend tower floors with plane proof").
**Verdict: PASS, no blocking defects.** SHAs: hot.json `52226cfdf046df242f72dfaf05c04aafec40070ecd9d4e370f8dbeab439a3c26`
EXACTLY matches `patches/impcatcher-19.jar` (downloaded via git blobs API, sha256 verified).
Numbering clean (589 = Build 19; version.txt=589 now matches the patch zip — the version-lag note from the 16–18 verdict is resolved). Zip root `net/` correct (195 net/ entries of 199, `jar uf` overlay-safe). Diff vs Build 18 jar: 4 classes changed (`ImpCatcherScript` + `$Frame`, `$HeldFrame`, `$Pending`).

## What changed (javap -c diff vs Build 18)
- New `descendViaFullRoute(Frame target, WorldPoint, String label)` — mirrors the Build 12/15/18
  `walkFullRouteTo`/`climbViaFullRoute` shape: BLOCKING `Rs2Walker.walkWithStateUntil` with a
  BooleanSupplier arrival test. The supplier (`lambda$descendViaFullRoute$3`) is now the DESCEND
  direction's completion predicate: `Rs2Player.getWorldLocation().getPlane() != frame.pos.getPlane()`
  OR 30s elapsed. Commit message's "plane proof" checks out.
- New diag `[[ImpCatcher] DESCEND_ROUTE_RESULT label={} from={} target={} state={} elapsedMs={} after={}]`
  with the `after` state string parsed by regex `x\s*=\s*(-?\d+),\s*y\s*=\s*(-?\d+),\s*plane\s*=\s*(-?\d+)`
  — plane in the proof is machine-verified, not hand-waved.
- Hold message renamed "Full route did not descend: state=" (Build 18: "did not reach upper floor"),
  following the descend with a `Pending`+`hold(...)` on failure — same bounded pattern as the climb path.
- New reachMizgog fallback string: `"*Mizgog absent on upper floor; check NPC id"` alongside
  existing "Wizard Mizgog"/"mizgog" query strings — read-message fix class, consistent with Build 15/17 style.
- `restoreHotReloadHold()` guard now reads `(storedBuild >= 19) ? false : "19".equals(build)` — still
  the same unsatisfiable shape (nit carried from the 1938 verdict; the `>=` to `>` recommendation
  remains unapplied). Reachability still depends on the PC-side `request.properties` flow, unverifiable from here.

## Questions for Alex (non-blocking)
1. Tick-block class now serves a THIRD path (walk/transport Build 12, climb Build 18, descend Build 19).
   During the up-to-30s `walkWithStateUntil` the heartbeat and status publish stall — the same
   expired-status/OCR-misread class Builds 7–9 fixed for login. Fine while the route succeeds fast;
   flagging only because it keeps recurring by construction.
2. `Rs2Player.getWorldLocation()` is now called inside the blocking supplier (twice per check in
   `descendViaFullRoute$3`, once after the route returns) — is `getWorldLocation()` safe off the client
   thread? Build 517 crashed on an off-client-thread `getWorldLocation()` call, so this is the one
   historical-crash pattern that applies to this diff. (Build 18's `climbViaFullRoute` has the same call;
   noting it once here for both.)

## Live verification (still pending — feed dark)
Acceptance triggers for Build 19: fresh `RUNNING_BUILD=19` marker, `DESCEND_ROUTE_RESULT` lines with
plane in `after={}`, or a first `IMPCATCHER_*` screenshot. Screenshot feed still dark ~147 min
(newest commit b4e19333 / 17:44:02 EDT Pirate's Treasure DONE frame); zero Imp Catcher frames ever
received (structural — Imp Catcher publishes status.properties only). Nothing live-observed.
