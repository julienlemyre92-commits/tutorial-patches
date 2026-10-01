# Muse review verdict — Ernest Build 22 (patch-624) — read-only

Filed 2026-10-01 00:17 EDT by the review-loop worker. Ernest is Alex-owned; this is review only, nothing shipped.

## Ground truth
- version.txt = 624; commit 61cc3eb1 "Build22: test collision-reachable panelled leaf crossing" (00:16:30 EDT)
- patches/patch-624.hot.json: build=22, patch=624, hostVersion=1, sha256=e03016e83476d94da5aea234701c020da3a0e0c853f1da58a9a27cfe5f8fc6d4
- SHA-256 EXACT-MATCH vs patches/ernestthechicken-22.jar (32748 bytes, +795B vs b21) — download-verified via git blobs API
- Numbering 623 → 624 clean; patch-624.zip net/-rooted (208 entries), only additions META-INF/MANIFEST.MF (RuneLite Main-Class intact) + version.txt=624 — overlay-safe
- 6 Script classes byte-identical zip <-> jar
- javap -constants: BUILD_NUMBER = 22

## What Build 22 does
Refactor of crossPanelledDoor around a new field `panelledDoorClosedTile:WorldPoint`. The five crossPanelledDoor lambdas from Build 21 (11/12/13 + int-returning 14/15) are gone, replaced by a collision-reachable approach: `crossPanelledDoor(Frame)` now consults `Rs2Tile.getReachableTilesFromTile` against the recorded closed tile. String constants b21→b22: zero adds, zero removals. External API class/method/field refs: 82 = 82 identical — zero new external API surface. Inner classes (Frame, Pending, LoginFrame, SkillLevelReview, DoorCandidate): signature-identical. One earlier cmp hiccup during this review was a relative-path artifact in my own check (md5sums confirmed identical on the rerun).

## Verdict: PASS, no blocking defects
- Numbering, SHA chain, zip root, manifest, overlay safety all clean.
- Mechanism matches the commit message exactly ("test collision-reachable panelled leaf crossing") and is the correct next experiment after Build 21's position-proof crossing: instead of trusting the stand tile recorded at crossing time, the door is now crossed through tiles proven collision-reachable from the recorded closed tile.

## Live acceptance (pending, unchanged)
Screenshot feed dark since 2026-09-30 17:44:02 EDT (~393 min, zero ERNEST_* frames ever). Acceptance triggers: fresh RUNNING_BUILD=22 banner, crossPanelledDoor diag lines, or first Ernest screenshot.
