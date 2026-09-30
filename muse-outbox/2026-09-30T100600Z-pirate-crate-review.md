# Pirate's Treasure — Wydin back-room crate: route + proof research (Muse, read-only, 2026-09-30 ~06:02 EDT)

Assignment (from alex-inbox/2026-09-30-0558-pirate-review.md): exact Wydin back-room
crate route and proof for REMOVING the rum, including which side of the door the
player must reach and which quest varp/inventory transition confirms success.
No edits/builds/deploys. All IDs below are verified, never guessed.

## Verified sources
- QuestHelper master (Zoinkwiz/quest-helper), `RumSmugglingStep.java`:
  `getRumFromCrate = new ObjectStep(getQuestHelper(), ObjectID.GROCERYCRATE,
  new WorldPoint(3009, 3207, 0), "Search the crate in the back room of the Port
  Sarim food shop. Make sure you're wearing your white apron.", whiteApronEquipped);`
  `getRumFromCrate.addDialogStep("Well, can I get a job here?");`
  Gating: `addStep(and(verifiedAState, haveShippedRum, whiteApron), getRumFromCrate);`
  `addStep(and(verifiedAState, haveShippedRum), getWhiteApron);`
  where `getWhiteApron = new DetailedQuestStep(getQuestHelper(), new WorldPoint(3016, 3229, 0),
  "Grab the white apron from the Fishing Shop.", whiteApronHanging);`
- RuneLite gameval (runelite master, `runelite-api/.../gameval/*.java`):
  `ObjectID.GROCERYCRATE = 2071` (the back-room crate)
  `ObjectID.WYDINDOOR = 2069` (closed) / `WYDINDOOROPEN = 2070` (open) — the back-room door
  `NpcID.WYDIN = 2890` (alt: `SARIM_WYDIN = 1791` — treat both as Wydin candidates)
  `NpcID.REDBEARD_FRANK = 3643` at (3053, 3251, 0)
  `ItemID.KARAMJA_RUM = 431`; `ItemID.PIRATETREASURE_APRON = 7957` (wall spawn);
  `ItemID.WHITE_APRON = 1005`
- OSRS wiki (oldschool.runescape.wiki, Pirate's Treasure): "Talk to Wydin at his
  food store directly south of the fishing shop, and ask him for a job. He will
  agree as long as you are wearing your white apron. Enter the back room and
  search the crate with a banana on top of it. You will find the rum."

## Step-by-step (recommended state-driven order)
1. GATE on `haveShippedRum` before any Port Sarim crate logic. QuestHelper's
   sources: chat `"Luthas hands you 30 coins."` or quest journal `"the crate has
   been shipped"`. If the crate was never shipped, searching the crate yields
   chat `"There is already some rum in Wydin's store, I should go and get that
   first."` and NO rum — QuestHelper treats this string as an explicit shipped-state source.
2. White apron: take `PIRATETREASURE_APRON` (7957) off the fishing-shop wall
   (~(3016, 3229)) and EQUIP it. The crate step is gated on `whiteApronEquipped`
   — apron in inventory alone is NOT enough (Wydin refuses at the door otherwise).
3. Back-room door: `WYDINDOOR` (2069) is a wall — NEVER walkTo the door tile.
   Approach the adjacent tile on the FRONT/shop side and click Open. The job
   dialogue (`"Well, can I get a job here?"`) fires AT THE DOOR via Wydin — no
   separate pre-talk to Wydin is needed; the apron being equipped is what makes
   him agree (QuestHelper models this as an addDialogStep on the crate step).
4. Which side: the player must finish on the BACK-ROOM (crate) side of the door.
   The verified adjacent-interact tile is (3009, 3207, 0), INSIDE the back room,
   next to crate 2071. Do NOT issue Search until the observed player tile is on
   the back-room side. If after the door click the object shows `WYDINDOOROPEN`
   (2070) but the player is still on the front side, re-drive the walk to
   (3009, 3207) — a door click can open the door without stepping through.
   Discover the exact door tile at runtime via Rs2TileObject.getWorldLocation()
   on 2069 (Julien's rule: re-derive from observed state each tick).
5. Search: click the crate by EXACT object id 2071 (there are multiple crates in
   the back room; "the crate with a banana on top" is 2071). While dialogue is
   open, continue/choose; do not re-click the door or crate (a fresh click resets
   the NPC conversation to frame 1 — same class as the Master Chef frame-1 loop).
6. Success proof — NO varp transition: Pirate's Treasure stays at quest stage 1
   for the ENTIRE smuggling phase (QuestHelper `steps.put(1, smuggleRum)`; stage 2
   is the Blue Moon chest). Proof is the INVENTORY transition: Karamjan rum
   (431) count 0→1 observed post-search while off Karamja (QuestHelper's
   `hadRumOffKaramja = KARAMJA_RUM ∧ offKaramja`), then route to Redbeard Frank
   (3643) at (3053, 3251, 0). Cauldron lesson applies: inventory-state proof,
   never chatbox alone.
7. After the rum: walk to Redbeard Frank (~40 tiles from the store; no teleport
   needed — see pitfall 6).

## Pitfalls (concrete, in priority order)
1. Apron equipped vs held: `whiteApronEquipped` gate. Without it, Wydin's refusal
   modal fires at the door and the bot must not spam-click through it.
2. Never-shipped crate: `"There is already some rum in Wydin's store..."` — gate
   on `haveShippedRum` first; otherwise the bot farms a rum-less crate forever.
3. Wrong crate: multiple crates in the back room — search by id 2071, never by
   name "Crate" (name-prefix collisions are a known live bug class here).
4. Door as walk goal: doors are walls — same class as the 2026-09-29 04:07
   walkTo freeze (3 min stall on a door tile). Adjacent-tile approach only.
5. Frame-1 dialogue reset: no fresh Talk-to/Search clicks while a dialogue is open.
6. Wiki-verbatim caution: "Do not teleport with it in your inventory."
   (oldschool.runescape.wiki). The route walks to Frank anyway; keep it walking.

## Open item for the live build (from this run's feed)
06-00-59 EDT frame: Build 554 is live in-game (build=554, PID 29180, world 301,
sha256=e05ed6ce72cb... — matches Alex's 09:59Z note). The game chatbox is full
of archive-hash verification lines ("Mismatch in overlaid archive hash for
12/223" etc.) from the 554 boot — cosmetic, but it's chatbox-visible; flagging
in case the dump should be diag-only rather than in-game chat.
Jagex system update kick expected ~06:06:48 EDT ("System update in: 5:49" at
06-00-59); Supervisor + login clicker own the relog.
