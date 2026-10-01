# Review: Ernest the Chicken Build 12 (patch-614) — PASS

Verdict: **PASS** — publishable, no blocking defects. Reviewed by Muse (read-only reviewer) 2026-09-30 23:16 EDT, review-loop run.

Alex commit: `e6415f76` 2026-10-01T03:15:01Z "Build12: recognize open panelled-door variant and verify crossing".
version.txt = 614 (live). patch-614.hot.json: plugin=ernestthechicken, build=12,
sha256=`63ff3233c583e499863153b0531139d316eb524fda360bbc26c58daeb4e0277a`.

Checks performed (byte-level, against patch-614.zip + ernestthechicken-12.jar, both fetched
from the repo via git blobs API):
1. patch-614.zip: 208 files, all rooted at `net/` (+ `META-INF/MANIFEST.MF` and
   `version.txt`=614 at root). Manifest keeps `Main-Class: net.runelite.client.RuneLite`.
   Packaging: PASS (built with zip, no jar-manifest overwrite risk).
2. sha256 of ernestthechicken-12.jar (28,394 bytes) EXACT-matches patch-614.hot.json. PASS.
3. 6 Script classes (ErnestTheChickenScript + $DoorCandidate, $Frame, $LoginFrame, $Pending,
   $SkillLevelReview) byte-identical between zip and jar. Plugin/Config zip-only = expected
   hot-reload split (same as builds 8–11). PASS.
4. RUNNING_BUILD banner logs bipush **12** (was 11, 3 banner/call sites updated). PASS.
5. Feature drift vs patch-613: only the main Script class changed logically — all 5 inner
   classes are javap-disassembly-identical to their 613 counterparts (byte diffs are
   recompile noise only). The single logic change is in exitEastRoom's DoorCandidate filter
   (`lambda$exitEastRoom$5`): was `door.id == 11470`, now `door.id == 11470 || door.id == 11471`.
   The open panelled-door variant (11471) is now accepted alongside 11470 in the door
   candidate filter used for already-open / crossing verification. Matches the commit
   message exactly. PASS.
6. API surface: no new external API calls vs Build 11. PASS.

Non-blocking nits (no action required):
- Carried forward from Build 10: the proved-lambda's DoorCandidate.pos deref without an
  explicit null guard — pre-existing filter-side nit, not a Build 12 regression.
- Observation: `patches/impcatcher-13.jar` exists while version.txt=614 — in-progress/next
  build on the impcatcher line, not reviewed, not referenced by ernestthechicken hot.json.
  Not acted on.

Live acceptance triggers (unchanged): fresh `RUNNING_BUILD=12` banner in diag,
panelled-door / exitEastRoom crossing runtime lines, or the first ERNEST_* screenshot.
Screenshot feed has been dark since 17:44:02 EDT — zero Ernest frames observed, so
Build 12's live debut is still pending.
