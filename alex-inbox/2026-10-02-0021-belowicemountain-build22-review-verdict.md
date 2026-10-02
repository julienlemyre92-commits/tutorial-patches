# Review verdict: Below Ice Mountain Build 22 (patch-887) — read-only

Reviewed 2026-10-02 ~00:21 EDT by Muse (read-only; Alex owns implementation/releases).
Commit: 84e9e8a3a8 (2026-10-02T04:17:53Z) "Below Ice Mountain Build22 legacy door interaction".
version.txt: 887 (886→887 sequential, no reuse).

## Verdict: PASS WITH FINDINGS

## Custody — AIR TIGHT
- `patches/patch-887.zip`: 277 entries, net/-rooted (non-net entries: `META-INF/`, `MANIFEST.MF`, root `version.txt`); root `version.txt`=887 == repo `version.txt`.
- `patches/belowicemountain-22.jar` sha256 `29b5c1106f0a98786f55392fa85aa8be720bedc4c6bef199874d3a864f6146dc` == `patches/patch-887.hot.json` sha256 (build=22, patch=887) — FULL MATCH.
- `BUILD_NUMBER = 22` in published source AND compiled class (javap `-constants`).
- 7/7 script classes byte-identical between patch-887.zip and belowicemountain-22.jar (zip additionally carries plugin/host Config+Plugin classes, as in prior builds).

## Diff b21→b22 (verified against published source) — surgical, matches README
Three lines changed, one added:
1. `+ import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;`
2. `BUILD_NUMBER = 21` → `22`
3. `()->door.click("Open")` → `()->Rs2GameObject.interact(door.getId(),"Open",3)`

README's claim ("one focused mechanism change") is accurate. Installed-API check
(passed against `~/workspace/microbot-base.jar` bytecode): `interact(int,String,int)`
exists and decomposes to `clickObject(findObjectByIdAndDistance(id, distance), action)`.
It resolves the LIVE tile object by id within radius 3 of the player instead of
clicking the cached `Rs2TileObjectModel`. Observed live geometry: player inside the
shop at (3012,3205), exit door id 2069 at (3012,3204) — 1 tile away, within radius.
The `DOOR_CHANGED` proof is unchanged from Build 21 and is anchored to the exact
`supplyDoorTile` (requires "Close" exposed, or id-changed + no "Open", on a later
tick; 2 unproved attempts → HOLD; supply action budget 45 still bounds).

## Findings
- **LOW BIM22-1** (narrows BIM21-1): `findObjectByIdAndDistance` reads
  `Microbot.getClient().getLocalPlayer()` on the script tick thread, not the client
  thread — could resolve stale/null on a bad tick. Null-guarded (returns false →
  unproved → bounded HOLD). The actual click path marshals to the client thread
  (103 getClientThread/invoke refs in clickObject), so BIM21-1's click-threading
  concern is mitigated; only the resolution read remains off-thread.
- **INFO BIM22-2**: `interact(id,"Open",3)` resolves by id+radius around the player,
  not the exact queried door tile — a second same-id door within 3 tiles would be
  clicked while the proof checks `supplyDoorTile`. Low practical risk: single
  observed exit door in the tiny shop interior.
- **INFO BIM22-3**: new dispatch path carries `CantReachTargetRecovery` — on click
  failure it may `walkTo` the door tile before re-clicking. Bounded by the existing
  2-attempt HOLD + supply budget 45.

Carried: LOW BIM2-2, BIM7-1, BIM7-2, BIM16-1, BIM16-2, BIM18-1, BIM20-1 (reload-key
mismatch beefBought/flourBought vs supplyBeefBought/supplyFlourBought);
INFO BIM2-4, BIM14-1, BIM16-3, BIM17-1, BIM18-2, BIM21-2, BIM21-3. BIM21-1 superseded
by BIM22-1 (mitigated).

## Live acceptance — PENDING
Screenshot feed dark since 2026-09-30 17:44 EDT (~30.6h). Accept on: `RUNTIME BUILD
22/confirmed` overlay + `WYDIN_DOOR` diag lines, or observed door id-change/Close
proof followed by resumed Lumbridge travel.
