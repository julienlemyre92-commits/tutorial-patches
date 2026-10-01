# Review verdict: Ernest the Chicken Build 9 (patch-611) — PASS

Reviewer: Muse review-loop | Time: 2026-09-30 23:02 EDT
Commit reviewed: 71206d52 "Build9: exact-tile east-room approach verification" (2026-10-01T02:59:17Z)
Artifacts: patches/ernestthechicken-9.jar (27546B), patches/patch-611.zip (768389B), patches/patch-611.hot.json

## Byte-level checklist — ALL PASS
1. **hot.json sha256** `e79e115a479128dcf3941daf74318e75efc4ba17b56001b730bb73b33ada20c2`
   exact-matches ernestthechicken-9.jar. ✓
2. **patch-611.zip structure**: 208 entries (same as 610), every entry under `net/`
   (plus META-INF/ + version.txt at root), version.txt reads `611`. Manifest keeps
   `Main-Class: net.runelite.client.RuneLite` — same packaging as the passed Build 8. ✓
3. **zip<->jar class identity**: the 6 ernestthechicken Script classes are
   byte-identical between patch-611.zip and the hot jar. ErnestTheChickenPlugin /
   Config / Plugin$1 remain zip-only = the established hot-reload artifact split. ✓
4. **Build marker**: `public static final int BUILD_NUMBER = 9;` in the jar's Script
   class (and identical in the zip copy). ✓
5. **Feature drift vs patch-610**: REAL change. All 6 Script classes differ
   (main Script 47676->47702B, +26 bytes). Bytecode: the walk helper now computes
   `exact = label.equals("TO_EAST_EXIT_APPROACH") ? 0 : 1` and passes it as the
   tolerance int to `Rs2Walker.walkWithStateUntil(WorldPoint, int, BooleanSupplier)` —
   i.e. the east-exit approach must land on the EXACT tile (tolerance 0), everything
   else keeps tolerance 1. Matches the commit message ("exact-tile east-room
   approach verification"). Inner-class diffs are same-size, no string changes —
   consistent with a tolerance/flag constant change. The Build 8 post-walk
   `[ErnestChicken] WALK ...` diag + `WorldPoint.equals` mismatch->HOLD guard is
   retained. ✓
6. **Packaging**: no new defects. (Minor note: the "TO_EAST_EXIT_APPROACH" string is a
   dead constant-pool entry in the Build 9 Script class — harmless, not a defect.)

## Scope note
No source dir was published for Build 9, so no source-level defect hunt was
possible (same as Build 8). Bytecode review found no concrete API or
state-machine defects to report.

## Verdict: PASS — safe to hot-load.
Live acceptance still pending: screenshot feed dark since 17:44:02 EDT, zero
ERNEST_* frames ever. Acceptance triggers unchanged: fresh RUNNING_BUILD=9
banner, Build 9 diag lines (WALK with label=TO_EAST_EXIT_APPROACH), or the first
Ernest screenshot.
