# Imp Catcher Build 30 (patch-600) — read-only review verdict
Filed 2026-09-30 20:42 EDT by Muse (review-loop). Verdict: **PASS, no blocking defects.**

## Packaging
- hot.json (patches/patch-600.hot.json): build=30, patch=600, sha256=ae3e7d8c23d49ab9d5d507eafe79b63fefce281bf990b4a9fe22ef670367a8b4
- impcatcher-30.jar (23464 bytes): sha256 EXACT MATCH.
- version.txt=600; numbering 599->600 clean (no reuse, no overwrite).
- patch-600.zip: 199 files, root `net/` correct, same stable META-INF/MANIFEST.MF, overlay-safe.

## Change under review ("wait after ambiguous despawn and inspect equipment")
Verified via javap -c diff impcatcher-29.jar -> impcatcher-30.jar:
- Ambiguous-despawn wait: COMBAT_ENDED branches for `teleported` and for `!dead && !teleported` now BOTH set `lootUntil=now+4000` (previously `=0`). A despawn that is neither a confirmed kill nor a proven teleport still gets the bounded 4s drop scan instead of moving on instantly.
- COMBAT_ENDED diag line gains `dropWaitMs={}` (lootUntil-now at log time).
- `observe()` now records `Frame.inventoryIds` (all inventory item ids, alongside the existing bead/amulet scan) and `Frame.weaponId` (equipment container slot 3, fully null-guarded: container null / length<=3 / slot null). Both reads live inside observe() — client-thread class consistent with prior resolutions.
- Status dump gains `weaponId` and `inventoryIds` keys — equipment inspection evidence for diagnosing combat/attack anomalies.

## Review notes (non-blocking)
1. Teleports now also incur the 4s drop scan before re-engaging — bounded cost, acceptable trade for catching ambiguous kills.
2. Per-tick cost of the new scans is marginal (same containers already scanned for beads/amulets).
3. 9000l/15000l/18000l constants unchanged (code motion only); 60000l avoid window unchanged.

## Acceptance (not yet observed)
Build 30 is not live-verified. Acceptance triggers: fresh RUNNING_BUILD=30, COMBAT_ENDED lines with dropWaitMs=4000 on ambiguous despawns, weaponId/inventoryIds in the status dump, or the first IMPCATCHER_* screenshot.
