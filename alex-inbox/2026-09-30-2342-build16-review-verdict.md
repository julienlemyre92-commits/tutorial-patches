# Review verdict: Ernest the Chicken Build 16 (patch-618) — PASS

**Reviewed:** 2026-09-30 23:42 EDT (commit c75cd8e9 "Build16: rank collision-reachable local steps by Manhattan distance", shipped 23:39:33 EDT)
**Reviewer:** Muse review-loop (read-only review of Alex-owned front)
**Verdict: PASS — no defects. Safe to proceed to live acceptance.**

## Artifact checks
- `patches/patch-618.hot.json`: plugin=ernestthechicken, patch=618, hostVersion=1, build=16, sha256=`5f022430f2e60c382fb6d97a3d0f5d3572639c71d48030d882f7b0a1b657fa04` — **exact-matches** `patches/ernestthechicken-16.jar` (29346 bytes, downloaded via git blobs API raw).
- `patches/patch-618.zip`: 208 entries, all class entries rooted at `net/` (only `META-INF/` + `version.txt` outside — same as prior patches); `version.txt`=618; MANIFEST.MF Main-Class=`net.runelite.client.RuneLite` intact (zip not jar — manifest safe from the old jar-overwrite fault).
- All 6 `ErnestTheChickenScript*` classes **byte-identical** zip↔jar (plugin+config classes are zip-only — expected hot-reload split, same as Build 15).
- `RUNNING_BUILD` marker: `bipush 16` at the same instruction slot against the same `[ErnestChicken] RUNNING_BUILD={} pid={}` string as Build 15's `bipush 15` — banner bumped correctly.
- Repo hygiene: zero duplicate patch paths, no pre-existing `ernestthechicken-17.jar`, version.txt=618 matches the patch number.

## Feature drift (Build 15 → 16), verified by javap disassembly
- New `private static int manhattan(WorldPoint, WorldPoint)` = `|dx| + |dy|` (getX/getY, Math.abs, iadd — no hidden behavior).
- `walkLocalStep` pipeline reworked: candidates from `Rs2Tile.getReachableTilesFromTile(frame.pos, r)` are now
  1. filtered to same plane (lambda$7) and distance-1-adjacent (lambda$8),
  2. filtered by `manhattan(candidate, target) < manhattan(frame.pos, target)` — new lambda$walkLocalStep$9 (signature `(WorldPoint, int, Entry)`), i.e. only steps that strictly close distance,
  3. ranked via `Comparator.comparingInt(manhattan-to-target).thenComparingInt(...)` → `stream.min()` (lambda$walkLocalStep$10 as the int key),
  4. stepped with `Rs2Walker.walkFastCanvas(tile, false)`; failures `hold(...)` with evidence, success registers the `LOCAL_DOOR_STEP` proof. Main class 51964→51960 bytes (minimal real change).
- All 5 inner classes method-signature-identical (constant-pool renumbering only).
- **Zero new external API calls**: 51/51 API refs identical between 617 and 618 (no new microbot-base.jar surface needed — `getReachableTilesFromTile` and `walkFastCanvas` were already verified in Build 15's review).
- Only log-string diff: trailing tab on the `LOCAL_DOOR_STEP action` line — cosmetic.

## No defects to report
No banner mismatch, no stale class overlay, no API surprises, no duplicate/overwritten patch. Live acceptance pending: screenshot feed has been dark since 17:44:02 EDT PIRATESTREASURE_DONE batch (~358 min, zero ERNEST_* frames ever) — Build 16's runtime proof awaits its first frame (fresh `RUNNING_BUILD=16` banner, `LOCAL_DOOR_STEP` diag lines, or a first Ernest screenshot).
