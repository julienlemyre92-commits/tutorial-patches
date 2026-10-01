# Review: Ernest the Chicken Build 11 (patch-613) — PASS

Verdict: **PASS** — publishable, no blocking defects. Reviewed by Muse (read-only reviewer) 2026-09-30 23:15 EDT, review-loop run.

Alex commit: `15462ae1` 2026-10-01T03:13:53Z "Build11: route through observed interior door and verify crossing".
version.txt = 613 (live). patch-613.hot.json: plugin=ernestthechicken, build=11,
sha256=`a6dae971f0a09c744148e1a7ebd202df7bfa2036ef5c54834880a84655e54016`.

Checks performed (byte-level, against patch-613.zip + ernestthechicken-11.jar, both fetched
from the repo via git blobs API):
1. patch-613.zip: 208 files, all rooted at `net/` (+ `META-INF/MANIFEST.MF` and
   `version.txt`=613 at root). Manifest keeps `Main-Class: net.runelite.client.RuneLite`.
   Packaging: PASS (built with zip, no jar-manifest overwrite risk).
2. sha256 of ernestthechicken-11.jar (28,387 bytes) EXACT-matches patch-613.hot.json. PASS.
3. 6 Script classes (ErnestTheChickenScript + $DoorCandidate, $Frame, $LoginFrame, $Pending,
   $SkillLevelReview) byte-identical between zip and jar. Plugin/Config zip-only = expected
   hot-reload split (same as builds 8–10). PASS.
4. RUNNING_BUILD banner logs bipush **11** (was 10). Marker correct. PASS.
5. Feature drift vs patch-612: all 6 Script classes differ (main Script + inner classes) —
   real feature change, not a re-ship. New: MANOR / INSIDE_MANOR / BOOKCASE WorldPoint
   constants, `crossManorEntrance(Frame)`, observed-door routing:
   - OPEN_MANOR_DOOR with exact-object recovery (`lastManorDoorId`/`lastManorDoorPos`,
     `manorDoorRecovery` attempts, `doors=` list in diag),
   - CROSS_MANOR_DOOR / CROSS_BOOKCASE steps, `manorDoorOpenVerified` proof flag,
     `inEastManorRoom()` zone check. Matches the commit message. PASS.
6. API surface: no new external API calls detected vs Build 10 (door-candidate machinery
   reuses verified Rs2UiHelper/Rs2Camera/TileObject APIs). PASS.

Non-blocking nits (no action required):
- Memory-only progress flags (`manorDoorOpenVerified`, `manorDoorOpenAttempts`,
  `manorDoorRecovery`, `lastManorDoorId/Pos`) reset on hot reload — same benign class as
  Build 10's `eastRoomExitCameraTurned`; the per-tick Frame re-observes door state, so a
  reload only repeats one routing pass. Not a defect.
- Carried-forward from Build 10: `lambda$proved$7` dereferences DoorCandidate.pos without
  an explicit null guard in the same bytecode window — pre-existing filter-side nit, not a
  Build 11 regression.
- Observation: `patches/ernestthechicken-12.jar` exists while version.txt=613/hot.json=11 —
  appears to be a pre-uploaded or in-progress next build; not reviewed, not referenced by
  hot.json. Not acted on.

Live acceptance triggers (unchanged): fresh `RUNNING_BUILD=11` banner in diag,
OPEN_MANOR_DOOR/CROSS_MANOR_DOOR/CROSS_BOOKCASE runtime lines, or the first ERNEST_*
screenshot. Screenshot feed has been dark since 17:44:02 EDT — zero Ernest frames observed,
so Build 11's live debut is still pending.
