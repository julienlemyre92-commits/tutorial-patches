# Witch's Potion burn-step research (Muse, 2026-09-30 05:04 EDT)

## Q1: approach tile + object ID/action for the Rimmington range
- Object 9682 NOT verifiable from installed sources. QuestHelper's WitchsPotion.class (microbot-base.jar) has no burn-step object — burnt meat is only an ItemRequirement with the note "You can use cooked meat on a fire/range to burn it". osrsbox has no objects endpoint; no cache dump on this VM. 9682 remains your assertion — recommend name-based lookup ("Range", op "Cook") as primary, ID as sanity check.
- Range location corroborated multi-source: Piano & Range house (Anja and Hengel's house), ground floor, eastern wall, directly north of Hetty's house. RSC wiki: "In the northern building there is a range on the eastern wall"; RSC Witch's Potion: "Another [range] is located directly north of Hetty the witch". QuestHelper anchor: Hetty (npc 4619) at WorldPoint(2968, 3205, 0); range house is the joined building immediately north.
- Burn interaction is item-on-object: use Cooked meat (2142) ON the range — NOT the "Cook" left-click op.
- Exact approach tile not obtainable without cache/game. Use the Build 547/548 crate pattern: query Range by name, derive an adjacent tile, verify walkability from observed player position; never walkTo the object's own tile.
- Fallback: Hetty's own fireplace is usable ("Meat can be used on the fireplace in Hetty's own house" — OSRS Wiki Witch's Potion); a player-lit fire also works. Zero travel from quest start.

## Q2: interface or direct burn?
DIRECT — no cooking interface. The Cook 1/5/X/All make-x interface fires only for RAW cookable items; cooked meat (2142) is not cookable, so the deliberate-burn path runs immediately, yielding Burnt meat (2146). Evidence: OSRS Wiki Witch's Potion ("use the cooked meat on the fire or range to purposely burn it"); answers.com ("use it on range again to deliberately burn it"); ironman optimal guide ("cook it twice" at the range); Darkan wiki chatbox "You deliberately burn the perfectly good piece of meat". Verification predicate: inventory contains 2146 (exact match) — do not match the chatbox string (variants exist).

## Likely failure mode
The FIRST cook (raw meat on range) DOES open the cooking interface — if the script uses raw meat expecting a direct cook, it stalls waiting for cooking that never starts. The two steps have opposite interaction models: raw->range needs interface selection, cooked->range is direct. Also: verify at runtime that 9682's actions contain "Cook" before trusting the ID; a stale ID clicking a non-range object fails silently.
