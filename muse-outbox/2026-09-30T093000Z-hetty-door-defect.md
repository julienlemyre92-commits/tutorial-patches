# Build 552 review — Hetty door-crossing defect (bounded read-only, per alex-inbox/2026-09-30-0523-witch-door.md)

Assignment: inspect `source-review/build552-witchspotion/WitchsPotionScript.java` lines 501-510 and 561 onward;
identify why `route(... hetty.getWorldLocation(), 1)` treats a tile across a wall/door as arrived;
give the smallest general correction proving a reachable approach or door crossing before clicking Hetty.
No source edits, no deploy, no restart, no game interaction. Findings only.

## Verified live state (this run)

- 05:22:07 EDT, Build 552 / patch 550, PID 38728, player at (2971,3209,0), varp 67 = 0,
  questState = NOT_STARTED. Chatbox verbatim:
  `[05:22:07] [WitchsPotion] HOLD target absent from loaded scene: Hetty line of sight`
  (viewed directly in screenshots/2026-09-30_05-22-50 and _05-24-11_WITCHSPOTION_HOLD_auto.png).
- Diags 05-22-41 through 05-24-11: `stage=HOLD`, `error=present`, ingredients all 0, health 100%.
- Stage sequence: WALK_HETTY_START (05-19-41 → 05-21-11) → START_HETTY (05-21-56) → HOLD (05-22-41 on).
  No quest progress claimed. HOLD is stable, not flapping.
- Player never entered Hetty's house. Anchor HETTY = (2968,3205,0) (matches QuestHelper
  WitchsPotion.class NpcStep(4619, (2968,3205,0)) — verified via javap on microbot-base.jar).
  Player tile (2971,3209) is outside the house; Hetty (id 4619) is inside.

## Root cause

`talkHetty` (lines 501-510):

```java
private void talkHetty(Frame f, String suffix) {
    if (route(f, "HETTY_" + suffix.toUpperCase(), HETTY, 4)) return;   // (A)
    Rs2NpcModel hetty = npc(HETTY_IDS, HETTY, 8);                     // (B)
    if (hetty == null) { missingScene("Hetty 4619", 12000); return; }
    if (!hetty.hasLineOfSight()) {                                    // (C)
        if (route(f, "HETTY_APPROACH", hetty.getWorldLocation(), 1)) return;  // (D)
        missingScene("Hetty line of sight", 12000); return;           // (E)
    }
    issue("talk:hetty-" + suffix, Proof.TALK, f, 9000, 0, () -> hetty.click("Talk-to"));
}
```

1. (A) arrives at Chebyshev distance ≤ 4 of the anchor — up to 4 tiles away, i.e. the player
   can stop OUTSIDE the house and still count as arrived.
2. (B) `npc()` = NPC-cache query `.withIds(4619).within(point, 8)` — pure world-coordinate
   distance. NPCs behind walls are still in the cache, so Hetty is "found" while unreachable.
3. (C) `hasLineOfSight()` false (wall between player outside and Hetty inside) → (D).
4. (D) `route()`'s arrival predicate (line 585) is `distance(f.pos, target) <= radius` where
   `distance()` (line 748) is pure Chebyshev: `Math.max(|dx|,|dy|)` — NO collision,
   reachability, or line-of-sight awareness. Target = Hetty's LIVE tile INSIDE the house.
   A player standing adjacent-across-the-wall (Chebyshev ≤ 1, e.g. a door-adjacent tile)
   satisfies the predicate on the first tick: if `route == null` it returns `false`
   (never spawns a walker segment, no door crossing attempted); if a Route was in flight
   it cancels it as "arrived". Either way the script concludes it is standing next to Hetty.
5. (E) still no line of sight → `missingScene("Hetty line of sight", 12000)` → 12 s later
   `hold()` → the exact HOLD line seen live at 05:22:07.

Confirmed contributing gap: the script contains ZERO door handling —
grep for `door`/`"Open"`/`openDoor` in WitchsPotionScript.java returns no hits.
This is the first building-interior NPC in the quest (rat/shop/cauldron are all outdoors),
so the gap was never exercised before. Even a started walker segment
(`Rs2Walker.walkWithStateUntil(target, 1)`) stops at radius 1 and never clicks doors.

Secondary (bonus) defect, same method: line 505 calls `hetty.getWorldLocation()` on the
tick thread. The Build 517 crash class (off-client-thread `getWorldLocation()`).
`objectAction()` in this same file already shows the safe pattern:
`Microbot.getClientThread().invoke(() -> ...)`.

## Smallest general correction (no hardcoded door coordinates)

Gate the approach on REACHABILITY, not distance, and add a bounded door-crossing
sub-state. All signatures below verified via javap against the installed
microbot-base.jar (same API the script already imports):

- `net.runelite.client.plugins.microbot.util.tile.Rs2Tile.isTileReachable(WorldPoint)`
  → `public static boolean isTileReachable(net.runelite.api.coords.WorldPoint)` ✓
- `Rs2Tile.getWalkableTilesAroundTile(WorldPoint, int)`
  → `public static java.util.List<WorldPoint> getWalkableTilesAroundTile(WorldPoint, int)` ✓
- `Rs2Tile.getReachableTilesFromTile(WorldPoint, int)`
  → `public static HashMap<WorldPoint,Integer> getReachableTilesFromTile(WorldPoint, int)` ✓
  (already used by `nearbyRecovery`, line ~660)
- Tile-object query: `Microbot.getRs2TileObjectCache().query().withName("Door").within(HETTY, 8)
  .nearestOnClientThread()` — `withName`/`within`/`nearestOnClientThread` verified on
  `AbstractEntityQueryable`; "Open"-action filter via the script's existing
  `objectAction(door, "Open")` helper (client-thread composition read) ✓
- `door.click("Open")` — `Rs2TileObjectModel.click(String)` verified ✓

Proposed shape for `talkHetty`:

```java
Rs2NpcModel hetty = npc(HETTY_IDS, HETTY, 8);
if (hetty == null) { missingScene("Hetty 4619", 12000); return; }
WorldPoint ht = Microbot.getClientThread().invoke(hetty::getWorldLocation); // thread-safe
boolean reachable = ht != null && Rs2Tile.isTileReachable(ht);
if (!reachable || !hetty.hasLineOfSight()) {
    // DOOR sub-state: one action (Open nearest door), proof on a LATER tick
    // = isTileReachable(ht) flipped true (or player tile crossed the doorway).
    // Bounded: 3 Open attempts, then hold("door:hetty ...").
    if (doorCross(f, hetty, ht)) return;   // returns true while acting/waiting
    hold("Hetty unreachable after door attempts at " + f.pos); return;
}
// only now: route already-arrived AND reachable AND line of sight → click Talk-to
issue("talk:hetty-" + suffix, Proof.TALK, f, 9000, 0, () -> hetty.click("Talk-to"));
```

`doorCross` sketch (one action, later proof, bounded):

1. Find door: nearest tile object within 8 of HETTY with `objectAction(obj,"Open") != null`
   (name filter "Door" first, fall back to any object with an "Open" action — fully general,
   no IDs). None found → return false (caller HOLDs).
2. Approach: pick a walkable tile adjacent to the door
   (`Rs2Tile.getWalkableTilesAroundTile(doorTile, 1)`, keep reachable-from-player ones);
   `route(f, "HETTY_DOOR", adjTile, 0)` — radius 0, and extend `route()`'s arrival check
   with `Rs2Tile.isTileReachable(target)` OR keep the Chebyshev check but never treat
   arrival as success unless the door is the target; arrival at the door tile is real
   because it is walkable by construction.
3. Click `door.click("Open")` ONCE; record attempt count + timestamp in the Frame/failures map.
4. Proof on subsequent ticks: `Rs2Tile.isTileReachable(ht)` true → exit door state, re-run
   approach-to-Hetty; or player world location changed across the doorway.
   If 20 s pass with no proof → next attempt (re-query door: its actions may now be "Close").
   After 3 attempts → `hold(...)` with at/f.pos, never silent.

Notes for integration:
- The same `isTileReachable` gate generalizes to every future interior NPC; consider adding
  it inside `route()`'s arrival branch as an optional strict mode rather than only in talkHetty.
- Do NOT widen the HETTY_APPROACH radius as a "fix" — a larger radius makes the
  across-the-wall false arrival MORE likely, not less.
- Live context: chatbox showed `System update in: 42:36` at 05-24-11 → Jagex system update
  ≈ 06:06 EDT will disconnect PID 38728 mid-HOLD; Supervisor + login clicker own that path.
  Run energy was depleted earlier ("You don't have enough energy left to run!" ×2) —
  expect slow walking after the update.
- The Make-X burn defect from the 05:25 delivery (muse-outbox/2026-09-30T092500Z-build552-review.md)
  still stands for the later stage; this door defect blocks reaching it.

## Files reviewed

- `source-review/build552-witchspotion/WitchsPotionScript.java` (830 lines; focus 501-510, 561-660, 716-752)
- Live evidence: screenshots/diags `2026-09-30_05-22-41` → `2026-09-30_05-24-11_WITCHSPOTION_HOLD_*`
  (PNGs viewed directly), `version.txt` = 550 (patch-550 = Build 552, Alex's 2-gap).
- API surface verified via `javap` on the installed microbot-base.jar (NOT from memory).

Muse review-only. No source edits, no compile, no upload, no game interaction.
