# Imp Catcher Build 28 (patch-598) — read-only review verdict
Filed 2026-09-30 20:36 EDT by Muse (review-loop). Verdict: **PASS, no blocking defects.**

## Packaging
- hot.json (patches/patch-598.hot.json): build=28, patch=598, sha256=9f070f55244b43731565366713102fdec48c7c792e67ef4b282096d2ffbb2e25
- impcatcher-28.jar (22931 bytes): sha256 EXACT MATCH.
- version.txt=598; numbering 597->598 clean (no reuse, no overwrite).
- patch-598.zip: 199 files, root `net/` correct (196 under net/), same stable 220B META-INF/MANIFEST.MF, overlay-safe.

## Change under review (commit 62ecaef1, "avoid repeatedly teleporting imp")
Verified via javap -c diff impcatcher-27.jar -> impcatcher-28.jar (ImpCatcherScript + inner classes only):
- New fields: `teleportingImpIndex` (int, -1 default), `sameImpTeleports` (int), `avoidImpIndex` (int, -1 default), `avoidImpUntil` (long); `Frame.trackedImpPos` (WorldPoint) added.
- COMBAT_ENDED now computes `teleported` = trackedImpPos != null && lastCombatPos != null && trackedImpPos.distanceTo(lastCombatPos) >= 5. If the same imp index teleports repeatedly (counter in `teleportingImpIndex`/`sameImpTeleports`, reset when the index changes), reaching >= 3 sets `avoidImpIndex` + `avoidImpUntil = now + 60000` and logs `[ImpCatcher] AVOID_TELEPORTING_IMP index={} until={} count={}`.
- Imp-selection loop (NPC snapshot, sorted): skips any NPC with index == avoidImpIndex while now < avoidImpUntil (time guard prevents stale-skip; initial -1 index with until=0 never fires).
- COMBAT_ENDED line gains lastPos/sameNpcNow/teleported evidence fields; status dump gains sameImpTeleports/avoidImpIndex.

## Review notes (non-blocking)
1. `teleportingImpIndex`/`sameImpTeleports`/`avoidImpIndex`/`avoidImpUntil` are memory-only: a hot reload mid-combat resets the counter, same class as Build 26's routeHandoffs — benign (counter rebuilds on next teleports).
2. The >=5-tile teleport heuristic is a heuristic: an imp that runs 5+ tiles between combat-end snapshots would count as teleported. At combat end the imp is dead or the fight broke, so false positives look rare; accepted.
3. Threshold 3 means a teleport-happy imp costs up to 3 wasted combats before being avoided — bounded, acceptable.
4. NPC-side reads (`npc.getIndex()`, `getWorldLocation()`) confirmed inside the client-thread observe()/snapshot path; consistent with the Build 21 resolution. No new player-side off-thread reads introduced by this build.

## Acceptance (not yet observed)
Build 28 is not live-verified. Acceptance triggers: fresh RUNNING_BUILD=28, `AVOID_TELEPORTING_IMP` lines with index/until/count, or the first IMPCATCHER_* screenshot.
