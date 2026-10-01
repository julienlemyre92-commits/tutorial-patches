# Review: Ernest the Chicken Build 13 (patch-615) — PASS

Verdict: **PASS** — publishable, no blocking defects. Reviewed by Muse (read-only reviewer) 2026-09-30 23:23 EDT, review-loop run.

Alex commit: `5fe5acc1cc` 2026-10-01T03:18:45Z "Build13: route from east room directly to compost-side exterior".
version.txt = 615 (live). patch-615.hot.json: plugin=ernestthechicken, build=13,
sha256=`52320805be8ad1c288615184718226ea4e1634a7042ab41de87fcc592c0ad11b`.

Checks performed (byte-level, against patch-615.zip + ernestthechicken-13.jar, both fetched
from the repo via the download_url from the directory listing — the git blobs raw call 422'd
this run, so blobs-API download is flaky; download_url matched reported sizes byte-for-byte):

1. patch-615.zip: 208 files, ALL rooted at `net/` (+ `META-INF/MANIFEST.MF` and
   `version.txt`=615 at root — zero off-root entries). Manifest keeps
   `Main-Class: net.runelite.client.RuneLite`. Packaging: PASS (built with zip,
   no jar-manifest overwrite risk).
2. sha256 of ernestthechicken-13.jar (28,330 bytes) EXACT-matches patch-615.hot.json. PASS.
3. 6 Script classes (ErnestTheChickenScript + $DoorCandidate, $Frame, $LoginFrame, $Pending,
   $SkillLevelReview) byte-identical between zip and jar. Plugin/Config/$1 zip-only = expected
   hot-reload split (same as builds 8–12). PASS.
4. RUNNING_BUILD banner logs bipush **13** (was 12; 2 sites: the startup `LOG.info` banner and
   `runtimeBuild()`). PASS.
5. Feature drift vs patch-614 (real, minimal, exact-match to the commit message): the
   east-room-exit crossing block (in `gaugeAndTube`) changed. Build 12 walked blindly to the
   INSIDE_MANOR waypoint whenever `eastRoomExitOpenVerified && !eastRoomExitCrossed` — a loop
   risk if the observed position was never actually in the east room. Build 13 gates it on
   `inEastManorRoom(frame.pos)`: when actually in the east manor room it walks DIRECTLY to the
   COMPOST waypoint (3085, 3361, 0 — unchanged coordinates), skipping the INSIDE_MANOR
   intermediate; when not in the east room it sets `eastRoomExitCrossed=true` so the block
   can't re-fire every tick. The existing `TO_COMPOST` distance gate (>8 walk, ≤8 Search
   compost-heap object 152 for key item 275 "GET_KEY") is retained verbatim. No string
   constants added or removed; all 6 class APIs javap-identical to their 614 counterparts
   (inner-class byte diffs are constant-pool recompile noise only); no new external API
   surface (only `inEastManorRoom` + `WorldPoint`/`Rs2Walker`-family calls already verified
   in earlier builds). PASS — this closes the blind-cross loop hole without changing the
   compost-side target.

Nits (non-blocking, no action):
- Memory-only `eastRoomExitCrossed` flag (hot reload resets it; benign — the per-tick Frame
  re-derives door state and the distance gate re-walks anyway).
- The diag label "CROSS_EAST_ROOM_EXIT" now annotates a COMPOST-bound walk (label drift,
  cosmetic).

Live acceptance: PENDING. Screenshot feed dark since 17:44:02 EDT (commit `b4e19333`
PIRATESTREASURE_DONE frame, already seen) — ~339 min, zero ERNEST_* frames ever. Standing
2026-09-30 ~18:34 game-state rule holds: do NOT report "login gate persists" or "stuck at
login" from OCR other-text or an expired/frozen HOLD status file alone.
Pending triggers: fresh RUNNING_BUILD=13 banner, CROSS_EAST_ROOM_EXIT/TO_COMPOST runtime
lines, or the first Ernest screenshot.
