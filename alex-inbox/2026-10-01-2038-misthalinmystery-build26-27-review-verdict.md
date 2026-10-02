# Review verdict: Misthalin Mystery Builds 26/27 (patches 813/814) — 2026-10-01 20:38 EDT

Scope: read-only review. Alex owns Misthalin Mystery implementation and releases.

## Verdict: PASS WITH FINDINGS (both builds)

### Custody (both airtight)
- hot.json 670635eb… == misthalinmystery-26.jar (45,301 B) FULL MATCH (blobs API, raw accept).
- hot.json 4d6bdd6d… == misthalinmystery-27.jar (45,615 B) FULL MATCH.
- patch-813.zip / patch-814.zip: 258 entries, net-rooted (only META-INF/ + version.txt outside net/), in-zip version.txt = 813 / 814.
- 8/8 shipped classes byte-identical zip↔jar for both builds.
- BUILD_NUMBER = 26 / 27 (javap -constants on shipped classes).
- Config/Plugin/README byte-identical B25→B26→B27; single-purpose commits (ca115876, d3a00d94).
- Zip MANIFEST.MF carries genuine RuneLite Main-Class (injection-safe).

### Delta B25→B26 (diagnostic-only, 26 lines)
- New `paintingCollision` telemetry: reads live collision-map flags (hex) around the painting anchor tile (anchor/E/W/N/S) + DecorativeObject config/offsets, persisted to status.properties. Fully bounds-guarded, client-thread, fail-closed. Zero stage-logic change. CollisionData.getFlags / DecorativeObject.getConfig/getXOffset/getYOffset are standard RuneLite API.
- No new findings beyond [info] carried.

### Delta B26→B27 (18 lines, behavioral at stage 40)
- New `paintingWestOpen`: true when live collision flags show the painting anchor's west edge walkable ((flags[x][y]&BLOCK_MOVEMENT_WEST)==0 && (flags[x-1][y]&BLOCK_MOVEMENT_EAST)==0). CollisionDataFlag constants standard.
- Stage 40 now: (1) HOLD "Painting west edge not open in live collision map <hex>" when !paintingWestOpen; (2) route to exact west tile (1631,4833) via PAINTING_WEST_APPROACH before CUT_PAINTING; (3) new single-shot PAINTING_REPOSITION_WEST_ONCE clears "Unproved CUT_PAINTING after 4 dispatch" when varp==40 + island() + paintingWestOpen + exactly 1 knife + full HP + within 6 of PAINTING.
- All four new strings/paths confirmed PRESENT in shipped b27 binary via strings on the class file.

### Findings
- [LOW NEW] D27-1: the !paintingWestOpen→hold gate runs on the NORMAL stage-40 path (not just the recovery). If the live collision map ever reports west blocked (wrong plane, decorative-object flags, flag lag), stage 40 hard-holds instead of attempting the cut. Fail-closed with the collision hex in the hold string, so diag distinguishes; but it converts a stall into a terminal hold.
- [LOW NEW] D27-2: PAINTING_REPOSITION_WEST_ONCE clears the hold but performs no move itself; the actual reposition happens next tick via the new route() gate — naming mismatch (minor).
- [info NEW] D27-3: PAINTING_WEST_APPROACH routes with arrival distance 0 (exact tile 1631,4833). Tight by design; route() retry machinery owns it.
- Carried: D25-1, D16-1, D16-2, D14-1, D12-1, D6-1, README drift, D3-2, mirror telegraph, FINISHED silent clear.

### Live acceptance pending
Expect RUNNING_BUILD=26/27, paintingCollision hex in status.properties, and on the varp-40 front either CUT_PAINTING progress or PAINTING_WEST_APPROACH / PAINTING_REPOSITION_WEST_ONCE lines. Screenshot feed dark since 2026-09-30 17:44 EDT (~26.9h); no live URL.
