# Build 20 review verdict (Muse review-loop, read-only) — 2026-09-30 20:16 EDT

Verdict: **PASS — no blocking defects.** Imp Catcher is Alex-owned; this review is read-only, nothing shipped.

## Integrity
- `version.txt` = 590; commit 94a77578 "Build20: exit tower through full route with arrival proof" (20:13:14 EDT). Numbering 589 → 590 clean.
- `patches/patch-590.hot.json` sha256 `18ef6b36eba6c7dbedc3ce915c4269363a532f7299c9f6def6516538456320cd` — **EXACT match** to `patches/impcatcher-20.jar` (20,952 bytes, git-blobs verified).
- `patch-590.zip`: 199 entries, all class files under `net/` (184 classes; only non-net roots are META-INF/, META-INF/MANIFEST.MF, version.txt — same overlay-safe shape as patch-589).

## Diff vs Build 19 (javap)
New field: `private static final WorldPoint TOWER_EXIT = new WorldPoint(3113, 3174, 0)` (ground plane, south of the tower door).
New method `exitTowerViaFullRoute(Frame)` + supplier lambda:
- Blocking `Rs2Walker.walkWithStateUntil(TOWER_EXIT, 1, supplier)`; supplier fires when `getWorldLocation()` is non-null, plane==0, distanceTo(TOWER_EXIT)<=2, **or** elapsed>=30s (bounded).
- Logs `[ImpCatcher] EXIT_TOWER_RESULT state={} elapsedMs={} before={} after={}`.
- Arrival proof: after!=null && after.getPlane()==0 && after.distanceTo(TOWER_EXIT)<=3 → `pending = new Pending("EXIT_TOWER", frame, 9000L, TOWER_EXIT)` (hands off to Build 10's non-blocking advancePendingWalk driver). Else `hold(frame, "Full route did not exit: state=...")`.
- Called from the post-descend branch when frame is plane==0, within 15 of TOWER, y<3173.
- New resume checkpoint string `HOT_RELOAD_RESUME_EXIT_TOWER` (same pattern as Build 17's INNER_TOWER_DOOR / Build 18's STAIR_ROUTE).

## Correction: the 19:38 restoreHotReloadHold nit is WITHDRAWN
My 19:38 verdict (carried through 19) claimed the `storedBuild >= N` + `build.equals("N")` guard was unsatisfiable/dead code. That was wrong. Re-verified against Build 20 bytecode (`restoreHotReloadHold`, `ImpCatcherScript`):
- `storedBuild` is parsed from the plugin's **own STATUS properties** — written by the *old* build (e.g. 19) before the reload.
- `requestBuild` comes from the host-written `~/.runelite/impcatcher-hot/request.properties` (build=20).
- On a 19→20 hot reload: `19 < 20` ✓ and `"20".equals(requestBuild)` ✓ → `RESTORED_HOLD` **is reachable**. The `>=` guard is correct as written: it prevents restoring a HOLD written by the same build or a newer one (self-restore loops). Apologies for the misread; the guard stays as-is.

## Carried notes (not blockers)
1. Tick-block class: blocking `walkWithStateUntil` on the tick thread now serves 4 paths (Builds 12/18/19/20). Each is bounded (30s supplier), but a genuinely hung route stalls heldHeartbeat + status publishing for up to 30s — the same expired-status/OCR-misread class Builds 7–9 fixed. No evidence of a hang yet; watch live.
2. `Rs2Player.getWorldLocation()` inside the blocking supplier — same client-thread-safety question as Builds 18/19 (Build 517 crash pattern). Unverifiable from the repo.

## Live verification (pending — feed dark)
Acceptance triggers for Build 20: fresh `RUNNING_BUILD=20` marker, `EXIT_TOWER_RESULT` lines with plane-0 `after={}`, or a first `IMPCATCHER_*` screenshot. Screenshot feed dark ~152 min (newest commit b4e19333 / 17:44:02 EDT Pirate's Treasure DONE frame); zero Imp Catcher frames ever received (structural — Imp Catcher publishes status.properties only). Nothing live-observed.
