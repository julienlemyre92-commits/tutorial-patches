# Pirate's Treasure Build 571 — false HOLD after chest unlock (key consumed)

**Observed live (frames 16-30-28 / 16-31-13 / 16-31-58 EDT, chatbox zoom verified):**
- Bot is parked upstairs in the Blue Moon Inn, stage `PIRATESTREASURE_HOLD`, zero movement
  for 2+ min, session timer ticking (01:12:15 → 01:13:45 — game live, not a stall).
- Chat history: `You unlock the chest.` / `All that's in the chest is a message...` /
  `You take the message from the chest.` — the chest (obj 2079) was successfully opened.
- Typed latch at 16:29:56: `[PiratesTreasure] HOLD Quest stage 2 but no chest key 432 in
  inventory; items={23072=1, 433=1, 995=149, ...}` — inventory shows MESSAGE (433)=1,
  KEY (432)=0.

**Defect** — `PiratesTreasureScript.java`, `chest()` (Build 571 source, L810–816):

```java
private void chest(Frame f) {
    stage = "BLUE_MOON_CHEST";
    if (f.count(KEY) == 0) {
        hold("Quest stage 2 but no chest key 432 in inventory; items=" + f.items); return;
    }
    ...
```

The chest-open action's own proof (L~840–844) explicitly accepts
`after.count(KEY) < f.count(KEY)` as proof of a successful unlock — the script KNOWS the
key is consumed when the chest opens. Yet on the very next tick, the guard at L813 treats
the now-absent key as a fatal condition and latches a permanent HOLD. The key's absence
here is the *success signature*, not an error.

**Missing step:** nothing ever reads the pirate's message. `MESSAGE = 433` is defined
(L55) and persisted to the account checkpoint (L1456), but no phase interacts with it —
there is no message-read and no deliver-message-to-Frank leg. Live state is varp==2,
key==0, message==1. Per the quest flow, reading the message (item 433, "Read") / handing
it to Redbeard Frank is what advances varp to 3, after which `dig()` runs at the
Falador cross.

**Suggested fix:**
1. In `chest()`, when `count(KEY)==0`: if `count(MESSAGE)>0`, read the message
   (`Rs2Inventory.interact(MESSAGE, "Read")`) — or route to Frank with the message if
   the dialogue is the varp-3 trigger — instead of HOLDing. The `open:blue-moon-chest`
   proof should also transition to a `READ_MESSAGE` phase on key-consumption.
2. Only HOLD for a genuinely missing key when `count(KEY)==0 && count(MESSAGE)==0`
   (e.g. key lost before ever opening the chest — that path needs a re-acquire route
   from Frank, not a permanent latch).

**Acceptance:** fresh frame showing the bot reading the message (or talking to Frank with
it), varp 2→3, stage `FALADOR_PARK_TREASURE` / `dig()` active — instead of the current
permanent HOLD.
