# Review verdict: Ernest Build 29 (patch-631) — PASS

- **Commit:** 4a375c36, 2026-10-01T05:02:03Z — "Build29: identify exact tube tile and collision component"
- **Packaging:** CLEAN. 208-entry `net/`-rooted zip (built with `zip`), `version.txt`=631 inside and out, `BUILD_NUMBER`=29 confirmed via `javap -constants`. Only the 6 `ErnestTheChickenScript*` classes changed vs Build 28; inner-class fields (Frame, DoorCandidate, LoginFrame, Pending, SkillLevelReview) byte-comparable identical — pure constant-pool churn.
- **Delta (vs Build 28), in `closetApproachDiagnostics`:**
  1. **Exact tube tile:** `Rs2GroundItem.getAll(276)` (radius 276 — effectively a junk/accidental radius in Build 28) → `Rs2GroundItem.getAll(12)` filtered by `getTileItem().getId()==276`. The `tubeLocations=` list now holds exact rubber-tube ground-item tiles within 12 tiles of the player.
  2. **Per-tube reachability:** for each exact tube tile, `Rs2Tile.getReachableTilesFromTile(tubeTile, 12)` → reachable-tile/cost map.
  3. **New diag fields:** `playerReachableTube=` (per tube tile, is the tube tile itself inside the player's reachable map — new `lambda$closetApproachDiagnostics$28` = `Map.containsKey(tile)`), and `tubeCosts=` (per-tube path-cost data). Together with the existing player/tube collision-flag dump, the diag now answers which collision component blocks the approach, per tube tile.
- **Purity audit: PASS — still fully read-only.** Zero `putfield`/`putstatic` in the method bytecode. New call surface is read-only: `Rs2GroundItem.getAll`, `RS2Item.getTileItem`/`getId`, `Rs2Tile.getReachableTilesFromTile`, `Map.containsKey`/`get`, stream/sort/limit collectors. No click/walkTo/walkStep/changeWorld/useItem/interact/menu-action anywhere in the delta. This is diagnosis tooling only — it changes nothing in game state.
- **Fires once at the HOLD threshold** (closetDoorOpenAttempts>=2 && closetKeyUseAttempts>=1) — same as Build 28. Documents the stall; does not attempt recovery.
- **Findings (carried from Build 28 review, still open):**
  - [M] Single-shot `closetKeyUseAttempts` persists via STATUS file across restarts → 1 failed/unproved dispatch = permanent HOLD; restart restores attempts=1 → immediate HOLD, no fresh attempt. Build 29 illuminates the HOLD further instead of fixing the budget. Suggested for a future build: budget reset on fresh unlock-message, or replenish-on-restart.
  - [L] `tubeCosts=` semantics (which map each cost is read from) not fully traced — harmless for a diag-only build.

**Verdict: PASS.** Ship-quality for a diagnostic build. Live acceptance pending (screenshot feed dark since 17:44:02 EDT 2026-09-30, ~7.6h; zero ERNEST_*/IMPCATCHER_* frames ever). Acceptance triggers: `CLOSET_APPROACH_DIAGNOSTICS {}` line containing `tubeLocations=` + `playerReachableTube=` + `tubeCosts=`, or a fresh `RUNNING_BUILD=29` banner.
