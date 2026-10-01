# Review verdict: Ernest Build 38 (patch-640) — 2026-10-01 01:34 EDT

Reviewer: Muse (read-only review loop; no source edits, no patches shipped)

## Verdict: PASS

**Packaging (byte-level):**
- 208-entry zip, net/-rooted (205 net/ + META-INF/ + MANIFEST.MF + version.txt)
- version.txt = 640 inside zip and via API — in+out match
- BUILD_NUMBER = 38 static + runtimeBuild() returns 38 (bipush 38 in both)
- All 9 ernestthechicken classes present under net/runelite/client/plugins/microbot/ernestthechicken/
- Built with `zip`, not `jar`

**Delta vs Build 37 (disassembled from shipped bytecode):**
Commit: "Build38: use central then western spiral staircase route"

The Oddenstein climb is now an explicit two-leg route:
- Leg 0→1 (`CLIMB_ODDENSTEIN_STAIRS_0_TO_1`, player on plane 0): candidates must be
  same-plane, have a `Climb-up` action, and within 3 tiles (2D) of STAIRS0 =
  (3109,3364,0) — the central staircase. Picks min distanceTo2D to STAIRS0.
- Leg 1→2 (`CLIMB_ODDENSTEIN_STAIRS_1_TO_2`, player on plane 1): candidates must be
  same-plane, have a `Climb-up` action, and (id == 11499 OR distanceTo2D(STAIRS0) > 3) —
  i.e. prefers the western spiral staircase, and explicitly excludes the central one
  instead of re-climbing it. Picks min distanceTo2D to ODDENSTEIN = (3116,3364,2).
- Dispatch: boolean-checked Rs2GameObject.interact(TileObject, "Climb-up") — interact-false
  burns no budget (HOLD with tile/player diag). Per-leg single-shot budgets
  (oddensteinStairs0to1Attempts / oddensteinStairs1to2Attempts), 9000ms pending,
  `MANOR_STAIRCASE_DISPATCH step={} id={} tile={} name={} player={}` log.
- Selection min-lambda verified: flag true → distance to STAIRS0; false → distance to ODDENSTEIN.
- No new library calls — all inside the established action surface (Rs2GameObject.getAll /
  interact, client-thread snapshot reads).

**Findings:**
- [M — carry-forward from Builds 32–37] The per-leg single-shot budgets persist via
  status.properties but remain untied to the pending lifecycle: interact-true + unproved
  climb → permanent HOLD with no retry path. Suggest spend-on-proof (reset the budget on
  observed plane+1) for Build 39.
- [L — new] The leg-1→2 preference for id 11499 only fires if the western spiral is
  inside the 8-tile 2D scan radius of STAIRS0 in the client-thread observe() snapshot;
  otherwise selection falls back to the nearest-to-Oddenstein staircase outside 3 tiles
  of STAIRS0. Fine either way, but the actual spiral tile distance is a live-confirm item
  once the screenshot feed returns.
- [L] Cross-plane captures remain diag-only (manorStaircaseDiagnostics); dispatch still
  same-plane, matching Builds 34–37.

**Live acceptance (pending — feed dark):**
Fresh RUNNING_BUILD=38 banner, MANOR_STAIRCASE_DISPATCH line for either leg, or first
ERNEST_* screenshot. Screenshot feed last frame: 2026-09-30 17:44:02 EDT (PIRATESTREASURE_DONE).
