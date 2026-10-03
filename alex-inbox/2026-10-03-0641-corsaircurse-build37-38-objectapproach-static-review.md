# Corsair Curse Build 37 -> Build 38 static review (muse, 2026-10-03 ~06:41 EDT)

## Source
- `source-review/corsaircurse-build37/CorsairCurseScript.java` (99,038 bytes)
- `source-review/corsaircurse-build38/CorsairCurseScript.java` (98,889 bytes)
- Commits: Build 37 `4c17bf9e` (06:37:37 EDT, patch-1083) → Build 38 `9f1aaf93` (06:38:59 EDT, patch-1084); version.txt=1084.

## Diff (14 lines, all inside `objectApproach(Frame f, Plan p)`)
1. `BUILD_NUMBER` 37 → 38.
2. Old (37): `if (Rs2Reachable.isReachable(p.at())) return p.at();` — anchor accepted whenever the raw target tile was reachable.
   New (38): `if (tileObjectCache.query().withId(p.id()).within(p.at(),1).nearestOnClientThread()==null) return p.at();` — falls back to the anchor when the target OBJECT is absent nearby (avoids computing an approach stand for a phantom object).
3. Candidate-tile filter: Build 37 required `Rs2Reachable.isReachable(tile) && WorldArea(tile,1,1).hasLineOfSightTo(worldView, p.at())`. Build 38 drops the LOS gate — reachability alone suffices.

## Verdict: PASS (static)
- Narrowly scoped to `objectApproach`; no navigation/dialogue/checkpoint lifecycle changes.
- Targets the observed stall exactly: Build 37's LOS gate could empty the candidate list (fallback to p.at(), which the provider then rejected → "Route to the first building was rejected"); Build 38's reachability-only filter should leave candidates on the table.
- Safe fallbacks retained: `orElse(p.at())` — can never return null; when p.at() is reachable it now still prefers the nearest candidate stand (behavior change: stand selection runs even when the anchor itself is reachable).
- `nearestOnClientThread()` executes inside `getClientThread().invoke(Supplier)` — correct thread already; naming matches the cache's client-thread-safe query variant.

## Caveats
- Line-of-sight was presumably added for a reason (interaction click-path blockers); dropping it may trade one rejection class for another. Watch whether the provider rejects the new stand.
- Build 37 itself was never live-observed (shipped 06:37:37, superseded by 38 in 82s) — live acceptance must read Build 38's marker.

## Live acceptance criteria (pending stream verification)
- Runtime marker re-read to 38 (not banner alone — need ticking LAST BUILD / new runtime lines).
- `[CorsairCurse] OBJECT_APPROACH target=... stand=...` log line differing from the Build 36 ramp-approach lines.
- The "wait shared service" / IllegalStateException `Quest action or shared service still owns input` stall either clears or persists.
- Quest progress off ~15/50 toward the first building.
