# Ernest the Chicken Build 10 (patch-612) review verdict — PASS (2026-09-30 23:06 EDT)

Request context: Alex shipped Build 10 / patch-612 at 23:06:13 EDT (commit 4c877f10
"Build10: guard invalid door clickbox and recenter once"), seconds into this review-loop
run. version.txt 611 -> 612. Review-only: no source edits, no compile, no uploads over
Alex's artifacts.

## Artifact integrity — ALL PASS
- hot.json: {"plugin":"ernestthechicken","patch":612,"hostVersion":1,"build":10,
  "sha256":"908a2b36..."} — sha256 EXACT-MATCHES ernestthechicken-10.jar (28181B).
- Numbering 611 -> 612 clean; jar 9 (27546B) -> 10 (28181B), +635B.
- patch-612.zip: 208 files, root net/ (zero classes outside
  net/runelite/client/plugins/microbot/), version.txt="612" at zip root,
  META-INF 220B with RuneLite Main-Class intact. Overlay-safe.
- Zip<->jar: all 6 Ernest Script classes byte-identical (Script, $Frame, $Pending,
  $LoginFrame, $SkillLevelReview, NEW $DoorCandidate); Plugin/Config zip-only as before.
- Build marker: RUNNING_BUILD site now bipush 10 (was 9, two sites), consistent.

## Feature diff 9 -> 10 (javap -c on both jars)
New `exitEastRoom` flow, new inner class ErnestTheChickenScript$DoorCandidate
(TileObject object, int id, WorldPoint pos, String actions, boolean open, boolean close):
1. Selects nearest candidate from Frame.nearbyDoors: open && dist(player)<=6 &&
   dist(MANOR)>3 (nearest by distanceTo(player)).
2. If candidate.pos == player pos: walk to (x-1,y,plane) label TO_EAST_EXIT_APPROACH
   (Build 9 exact-tile approach preserved), return.
3. Clickbox guard (new): Rs2UiHelper.getObjectClickbox(door.object); invalid =
   clickbox null || client null || clickbox.x<=1 || clickbox.y<=1 ||
   width>=canvasWidth-2 || height>=canvasHeight-2.
   - Invalid && !eastRoomExitCameraTurned: warn-log EAST_ROOM_EXIT_NO_CLICKBOX with
     detail (id/class/tile/local/clickbox/onScreen), Rs2Camera.turnTo(door), set flag.
   - Invalid && already recentered: hold("EAST_ROOM_EXIT_NO_CLICKBOX recenter ...").
4. Valid: info-log EAST_ROOM_EXIT_CLICKBOX (id/class/tile/local/clickbox),
   Rs2GameObject.interact(object,"Open"), log OPEN_EAST_ROOM_EXIT exactObject.

## API verification (installed microbot-base.jar)
- Rs2UiHelper.getObjectClickbox(TileObject) exists.
- Rs2Camera.isTileOnScreen(TileObject), Rs2Camera.turnTo(TileObject) exist.
- Rs2GameObject.interact(TileObject,String) — pre-existing usage, unchanged.

## Nits (non-blocking)
- eastRoomExitCameraTurned is memory-only: a hot reload resets it, so a second
  recenter is possible post-reload. Benign (one extra camera turn), consistent with
  the established memory-only-flag pattern.
- x<=1/y<=1 heuristic treats a partially-offscreen clickbox as invalid; conservative,
  bounded by the one-shot recenter then evidence-rich HOLD.
- DoorCandidate.pos comes from TileObject.getWorldLocation(); a null would NPE in
  the pre-existing distance lambdas, not a Build 10 regression.

## Verdict
PASS — no blocking defects, no hard quest blockers. Packaging exact, API surface
real, logic matches the commit message, failure path is bounded and observable.

## Live acceptance — PENDING
Feed still dark since 17:44:02 EDT (zero ERNEST_* frames ever). Acceptance =
fresh RUNNING_BUILD=10 banner, EAST_ROOM_EXIT_CLICKBOX / EAST_ROOM_EXIT_NO_CLICKBOX
diag lines, or first Ernest screenshot. Do NOT treat the frozen HOLD status file or
launcher OCR other-text as a login gate (standing 18:34 EDT rule).
