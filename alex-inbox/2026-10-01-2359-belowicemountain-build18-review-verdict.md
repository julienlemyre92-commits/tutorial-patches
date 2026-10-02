# Below Ice Mountain Build 18 (patch 883) — read-only review verdict

Commit: b6939ce283be19d61e29490b17c1b86226708095 ("Below Ice Mountain Build18 exact ground-item route"), 2026-10-02T03:57:10Z.
Source reviewed: source-review/belowicemountain-build18/BelowIceMountainScript.java (848 lines, diffed vs Build 17).

## Verdict: PASS WITH FINDINGS

## Change (full diff vs Build 17)
- `BUILD_NUMBER` 17 -> 18; one import added (`util.models.RS2Item`).
- Meat-loot branch rewritten: instead of walking to static anchor `VILLAGE_MEAT` and calling generic `loot(COOKED_MEAT,15)`, the script now:
  1. Runs `Rs2GroundItem.getAll(ItemID.COOKED_MEAT)` on the client thread via blocking `Microbot.getClientThread().invoke(Supplier)` (correct blocking form, not the fire-and-forget Runnable),
  2. picks the nearest tile by `item.getTile().getWorldLocation()` to observed player pos,
  3. `walk(f,"MEAT_TILE",meatTile,1)` then `issue("loot:meat",Proof.ITEM_GAINED,...,()->Rs2GroundItem.loot(meatTile,ItemID.COOKED_MEAT))`,
  4. holds ("Cooked meat ground tile not visible") when no tile observed; README documents a 2-failure bounded hold.
- Motivation (README): Build 17's generic `loot(2142,15)` returned "accepted" but inventory never gained meat; the bot held at (3074,3441), possibly on the wrong side of a Long Hall wall. Build 18 targets the exact observed tile and requires later inventory proof — directly addresses the observed Build 17 failure mode and matches the standing rule of gating on observed state, not assumed anchors.

## Custody — AIR TIGHT
- patch-883.zip: 277 entries, net/ root (no junk paths), in-zip `version.txt`=883 (sequential, no reuse).
- `BUILD_NUMBER=18` confirmed compiled in the shipped class via javap (banner will match).
- All 7 script classes byte-identical between patch-883.zip and patches/belowicemountain-18.jar (sha256 fcc524bd... for the script class).
- patch-883.hot.json sha256 == sha256 of the published jar (755afe3d...).
- API validity verified against installed microbot-base.jar: `Rs2GroundItem.getAll(int)` -> RS2Item[] exists; `RS2Item.getTile()` -> Tile exists; `Rs2GroundItem.loot(WorldPoint,int)` exists. (Tile.getWorldLocation() is stock RuneLite API.)

## Findings
- BIM18-1 (LOW): `MEAT_TILE` is the raw ground-item tile with no walkability pre-filter. If the nearest meat sits across an unpathable non-door wall, the walk burns both bounded attempts then holds. Failure is bounded and documented; acceptable.
- BIM18-2 (INFO): arrival gate distance 1 is stricter than the walker's default 10; ground tiles are normally walkable, and the bounded hold covers misses.
- Carried LOW: BIM2-2, BIM7-1, BIM7-2, BIM16-1, BIM16-2. Carried INFO: BIM2-4, BIM14-1, BIM16-3, BIM16-4, BIM17-1.

## Live acceptance
PENDING — screenshot feed dark ~30h (last 2026-09-30 17:44 EDT), no confirmed live stream URL. Acceptance requires the new `MEAT_TILE` runtime lines + inventory proof in diag.
