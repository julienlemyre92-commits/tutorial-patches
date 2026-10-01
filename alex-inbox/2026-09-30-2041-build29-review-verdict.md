# Imp Catcher Build 29 (patch-599) — read-only review verdict
Filed 2026-09-30 20:41 EDT by Muse (review-loop). Verdict: **PASS, no blocking defects.**

## Packaging
- hot.json (patches/patch-599.hot.json): build=29, patch=599, sha256=8f25dff4379b9adae2a8caf7fec9273cb2f709e86a0fdc86dd252f7700185fbe
- impcatcher-29.jar (23222 bytes): sha256 EXACT MATCH.
- version.txt=600 (repo now ahead at patch-600); numbering 598->599 clean (no reuse, no overwrite).
- patch-599.zip: 199 files, root `net/` correct, same stable META-INF/MANIFEST.MF, overlay-safe.

## Change under review ("distinguish imp death from teleport")
Verified via javap -c diff impcatcher-28.jar -> impcatcher-29.jar (ImpCatcherScript + inner classes only):
- `observe()` (client-thread snapshot) now calls `net.runelite.api.NPC.isDead()` on the interacting and tracked imp, storing `Frame.interactingImpDead` / `Frame.trackedImpDead` (3 call sites, all inside observe() — same client-thread class as the Build 21 resolution).
- In-combat latch: while fighting, if interactingImpDead OR trackedImpDead -> `sawTrackedImpDead=true`. Latch RESET to false at COMBAT_STARTED (new engagement) and again after each COMBAT_ENDED log — no stale-flag leak.
- COMBAT_ENDED: `dead = sawTrackedImpDead || frame.trackedImpDead`. If dead: `confirmedKills++`, `combatEndsWithoutBead++`, `lootUntil=now+4000` (drop wait preserved); the teleport-detection branch is SKIPPED when dead, so a kill can no longer inflate `sameImpTeleports` / trigger AVOID_TELEPORTING_IMP for the wrong imp. If !dead && teleported: Build 28 avoid logic unchanged.
- COMBAT_ENDED diag line gains `dead={} confirmedKills={} noBeadKills={}`; status dump gains confirmedKills / trackedImpDead / interactingImpDead keys.

## Review notes (non-blocking)
1. `sawTrackedImpDead`/`confirmedKills` are memory-only: a hot reload mid-combat resets them — benign (rebuilds next combat).
2. Death is only caught if observe() sees the dead imp before it despawns; a kill that despawns between frames still falls to the teleport/ambiguous path — accepted, bounded by the 4s drop wait.
3. Threshold interactions: a confirmed kill no longer counts toward the >=3 teleport-avoid threshold — strictly reduces false avoids.

## Acceptance (not yet observed)
Build 29 is not live-verified. Acceptance triggers: fresh RUNNING_BUILD=29, COMBAT_ENDED lines with dead=true / confirmedKills incrementing, or the first IMPCATCHER_* screenshot.
