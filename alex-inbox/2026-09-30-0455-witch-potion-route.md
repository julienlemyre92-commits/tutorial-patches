# Witch's Potion — ingredient acquisition route research (Muse, read-only)

Time: 2026-09-30 ~04:55 EDT. Assignment: `2026-09-30-0450-witch-potion-research.md`.
No local commands were run this session (per your restriction — no javap, no builds, no gameplay).
Sources: OSRS Wiki "Witch's Potion" quest page + "Hetty" NPC page, retrieved 2026-09-30;
Alex-supplied item IDs from the assignment note.

## Verified vs uncertain

**VERIFIED (OSRS Wiki, retrieved today):**
- Quest #13 in the quest list (list number, NOT the client quest id/varp), F2P, Novice, very short. Requirements: none. Reward: **1 QP + 325 Magic XP**.
- Start: talk to **Hetty** in her house, **south-east Rimmington** (south of Falador, west of Port Sarim).
- **Hetty NPC ID 4619** (OSRS Wiki advanced data, retrieved today).
- Ingredients: rat's tail, burnt meat, eye of newt, onion. **Rat's tail only drops after the quest is started**; giant rats do NOT drop it.
- Rat: **level 1**, found in the archery shop (**Brian's Archery Supplies**) west of Hetty's house.
- Burnt meat: kill a **giant rat just north of the chapel graveyard south-east of Rimmington** (raw rat meat), or a cow in the cow pen south of Falador, or buy raw meat from **Wydin** (food store, Port Sarim). Cook on **the fireplace in Hetty's own house**. If it cooks to cooked meat instead of burning, **use the cooked meat on the fire/range again to force-burn it** (deterministic — no RNG dependence). A player-lit fire also works.
- Eye of newt: **Port Sarim magic shop, 3 coins each**.
- Onion: **field directly north of Rimmington, east of Melzar's Maze** (alt: Fred's farm backyard, Lumbridge).
- Finish: talk to Hetty → she puts everything in her cauldron → **drink from the cauldron** → quest complete.
- Account state tonight: parked at Goblin Village post-Goblin Diplomacy, 16 QP, 200+ coins visible — the 3-coin newt purchase is fine.

**ALEX-SUPPLIED (from your note, not jar-verified this run):** rat tail 300, onion 1957, burnt meat 2146, eye of newt 221.

**UNVERIFIED (needs your implementation/live check):**
- Client quest id / varp / varbit for Witch's Potion (QuestHelper not inspected — no-commands restriction).
- Betty (magic shop) NPC id; level-1 rat / giant rat NPC ids; Hetty fireplace + cauldron object ids and their live menu actions; onion-patch object ids.
- Exact tiles for Hetty's house, fireplace, archery shop, chapel-graveyard giant-rat spawn, onion patch, Betty's shop counter. I did not invent coordinates.
- Whether Hetty's fireplace is lit by default; whether the account currently carries a tinderbox and logs.

## Suggested acquisition order (Rimmington hub + one Port Sarim trip)
1. **START FIRST:** talk to Hetty 4619 (ask about the black arts). Gate everything on quest-started — killing rats before this drops nothing.
2. **Rat's tail:** Brian's Archery Supplies, west of Hetty's. Kill a level-1 rat (fists are fine). Proof: item 300 in inventory, next tick.
3. **Burnt meat:** giant rat north of the chapel graveyard (SE Rimmington) → raw rat meat. Cook at Hetty's fireplace; if cooked-not-burnt, use cooked meat on the fire again → forced burn. Proof: item 2146.
4. **Onion:** patch north of Rimmington, east of Melzar's Maze. Pick one. Proof: item 1957.
5. **Eye of newt:** walk east to the Port Sarim magic shop, buy 1 for 3 coins (bounded stock wait if 0 — your existing shop-path pattern). Proof: item 221.
6. **Finish:** return to Hetty → cauldron cutscene → drink from cauldron (modal interface click; prove with questState/VARP, never inventory-emptiness alone — the Cook turn-in lesson).

## Required start state (pre-flight checks for the plugin)
- Quest unstarted is fine (script starts it); if already started, detect and skip to missing-ingredient fill.
- 3+ coins (have 200+).
- **Tinderbox + a way to make a lit fire** (logs, or an already-lit fireplace/range). If Hetty's fireplace is unlit and inventory lacks tinderbox+logs, the burn step is blocked — see blocker below.
- Weapon optional; food unnecessary (only rats).

## First likely blocker
**The burn step's fire.** Everything hinges on a lit fire at Hetty's fireplace. If it's unlit and the inventory has no tinderbox+logs, the script must source them (chop/carry logs — a new sub-route) or fall back to another lit range/fire. Validate live on the first run: fireplace lit state + tinderbox/logs in inventory.
Secondary, in order: rat's tail is quest-gated (start quest before any rat kill or it farms forever); eye-of-newt shop stock 0 (bounded wait, then HOLD); the cauldron-drink modal's exact option text (live).

## Hazards
None like the Aubury stone circle. The whole route is Rimmington + Port Sarim walking with no aggressive monsters at level 3. The giant rat is the only combat and it's wiki-suggested for this exact purpose.

## Pacing/proof notes (human-like, bounded)
One action per tick; next-tick inventory-delta proof per ingredient; **exact-name** item matching (the Bread/Bread-dough substring lesson — "burnt meat" exact, never `contains`); bounded retries then a typed HOLD with the ingredient name. The forced-burn (cooked→burn) is the human-like deterministic play — no burn-RNG farming.
