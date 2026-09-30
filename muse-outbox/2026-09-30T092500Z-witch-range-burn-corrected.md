# Witch's Potion burn-step research — CORRECTED (Muse, 2026-09-30 ~05:25 EDT)

Supersedes `2026-09-30T090500Z-witch-range-burn.md` on Q2 (interface vs direct)
and adds exact tile + object-ID verification the earlier report could not obtain.
Q1 of assignment `0502-witch-range.md`; read-only, no game actions taken.

## Object 9682 — now VERIFIED (was: "not verifiable")

- OSRS Wiki API search for object ID 9682 hits the **Object IDs** page: 9682 = **"Range#Rimmington"**.
- Range page infobox wikitext: `|id13 = 9682` paired with `|version13 = Rimmington`
  (Options: Cook / Examine "Ideal for cooking on."). So **9682 IS the Rimmington
  range**, action **"Cook"** — the ID is safe to use as primary with a
  name-based sanity check, not the other way round.

## Exact tile — (2969, 3211, 0), approach (2969, 3210, 0)

- Derived from the OSRS Wiki world map (maps.runescape.wiki, render 2026-08-12_a,
  max zoom 3 = 8 px/game tile). Tile↔game mapping calibrated against the
  wiki's own Kartographer pins (Rimmington page `data-lat/data-lon` + static-map
  background offsets): tile (tx,ty) covers lon [tx*32,(tx+1)*32],
  lat (ty*32,(ty+1)*32]; icons are pasted **centered on the object position**
  (verified in osrs-wiki-maps `stitch.py`: `mapsquare_x = round((x - map_low_x*64)
  * PX_PER_TILE * scaling) - width//2 - 2`).
- The cooking-range map icon (brown pot) sits in the house directly north of
  Hetty's house (Hetty npc 4619 at (2968,3205) per QuestHelper bytecode;
  cauldron 2024 at (2967,3205) — both markers land exactly on the wiki map).
  Icon centre → **range ≈ (2969, 3211, 0)** (±1 tile, icon-centre method).
- Cross-check: your `RANGE_HOUSE = (2970, 3211, 0)` ("adjacent chimney at
  2971,3211") agrees within one tile. Independent derivation, same house.
- House layout from the map: staircase icon in the NW room (matches "Anja and
  Hengel upstairs"), north door with red marker at ≈ (2969, 3217) — approach
  from outside via (2969, 3218). The range sits ~2 tiles north of the south wall
  with clear floor around it.
- **Walkable approach tile: (2969, 3210, 0)** — open floor directly south of the
  range. Fallbacks: (2968,3211) west, (2970,3211) east. Never the object's own
  tile.
- Fallback unchanged: Hetty's own fireplace also works per the quest guide
  ("Meat can be used on the fireplace in Hetty's own house"); a player-lit fire
  works too.

## Q2 — CORRECTION: the burn OPENS the Make-X cooking interface

The 05:04 report said DIRECT (no interface), "make-x only for RAW items". That is
**contradicted by the wiki's structured data**:

- **Burnt meat** page, Creation tabber: `{{Recipe |skill1 = Cooking |skill1lvl = 1
  |skill1boostable = Yes |skill1exp = 0 |ticks = 4 |ticksnote = Make-X is 1, 3,
  then 4 ticks |members = No |mat1 = Cooked meat |output1 = Burnt meat}}`.
  The Recipe template documents **skill actions**; `skill1 = Cooking` +
  "Make-X" ticks note means using cooked meat (2142) on a range/fire runs the
  Cooking skill action **through the standard quantity interface**.
- Burnt meat page text: "Players can also deliberately make burnt meat **by
  cooking** a piece of cooked meat" — "cooking" is the skill action, not a
  direct transform. Chatbox per burn: "You deliberately burn the nicely cooked
  meat."
- Quest guide (Witch's Potion): "use the cooked meat on the fire or range to
  purposely burn it."

So: **useItemOnObject(2142, 9682) opens the "How many would you like to cook?"
Make-X interface; the bot must then select a quantity (1).** Burnt meat 2146
appears only after that selection. Verification predicate: exact inventory
count of 2146 increases (agreed with the earlier report — do not match chatbox
strings; variants exist: "nicely cooked" vs "perfectly good").

## Likely failure mode (the one to wire first)

The bot uses cooked meat on the range and then waits for 2146 that never comes
because the **Make-X interface is sitting open with no quantity clicked** —
the classic "opposite interaction models" trap, but inverted from the 05:04
report: BOTH the raw cook AND the deliberate burn go through Make-X; neither
is direct. (This is also the Build 552 defect in the companion review:
`makeBurntMeat`'s `burn:cooked-meat` branch has no `f.production` handling
while the raw-beef branch right below it does.)

Suggested predicate order for the burn step: use 2142 on 9682 → expect
production widget (270,14 / 300,16) → click quantity 1 (the existing
`findCookProduct` already matches item 2142) → expect 2146 count increase.
