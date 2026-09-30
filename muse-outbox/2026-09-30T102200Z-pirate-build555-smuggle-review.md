# Build 555 Pirate's Treasure — varp71 stage-1 smuggling review (Muse, 2026-09-30 06:22 EDT)

Bounded task from alex-inbox/2026-09-30-0618-pirate-build555-review.md: read-only review of the
three Build555 sources + README, focused on the varp71 stage-1 smuggling flow —
(a) Luthas crate/payment proof and (b) Wydin's door-to-crate access —
against installed Microbot/QuestHelper bytecode I can verify. No edits, no deploys, no game control.

## Verified against installed bytecode (microbot-base.jar)

All from `questhelper/helpers/quests/piratestreasure/RumSmugglingStep.class` javap and
`gameval/{ObjectID,NpcID}` constants:

- WYDIN = 2890, SARIM_WYDIN = 1791 — script talks to `new int[]{2890, 1791}`. OK.
- GROCERYCRATE = 2071 at WorldPoint(3009,3207,0); script queries exact id 2071 within 6 of (3009,3207). OK.
- WYDINDOOR = 2069 (closed), WYDINDOOROPEN = 2070 — script's WYDIN_DOOR_CLOSED/OPEN. OK.
- LUTHAS = 3647 at (2938,3154,0); REDBEARD_FRANK = 3643 at (3053,3251,0); ZEMBO = 13655 at (2929,3145,0). OK.
- Plantation crate 2072 at (2939,3149,0). OK. Karamjan rum = 431; banana = 1963; white apron item = 1005, hanging = 7957. OK.
- Dialogue strings: "Could you offer me employment on your plantation?", "Well, can I get a job here?",
  "Search away, I have nothing to hide.", "Luthas hands you 30 coins." (QuestHelper `crateSent` =
  ChatMessageRequirement on that chat line). All present in the script's allowed list. OK.

## (b) Wydin's door-to-crate access — VERIFIED SOUND, caution honored

Alex's caution ("treat (3009,3207) as a crate location, not a proven walkable player tile") is
honored in code. The script never walks to WYDIN_INTERIOR directly:

- `PiratesTreasureScript.java`, RETRIEVE_RUM: after the job talk and door open, it computes
  `reachableAccess(f.pos, WYDIN_INTERIOR, 1, 9)` — a tile reachable from the player's current
  position (scan 9) within Chebyshev 1 of the crate tile — then `route(f, "WYDIN_CRATE_ACCESS", access, 0)`.
  The crate click itself is `object(WYDIN_CRATE, WYDIN_INTERIOR, 6)` (object id 2071), same as
  QuestHelper's ObjectStep(2071 @ 3009,3207). The `f.wydinDoorOpen` observer gate
  (`near(f.pos, WYDIN_FRONT, 12)`) is always satisfied in this flow because RETRIEVE_RUM routes to
  WYDIN_FRONT radius 4 before the door branch. No defect found here.

## (a) Luthas crate/payment proof — one concrete robustness defect found

QuestHelper's authoritative payment proof is the chat message "Luthas hands you 30 coins."
The script's equivalent — `f.count(COINS) >= shipCoinBefore + 30` — is a sound observed-state
proof. But the **timeout window is anchored to the wrong event**:

- `onProved`: `if (p.key.equals("talk:luthas-ship")) shipTalkAt = System.currentTimeMillis();`
  The proof for "talk:luthas-ship" is `dialogueChanged(f, after)` — i.e. the stamp lands when the
  Luthas talk DIALOGUE OPENS, not when payment is due.
- `case SHIP_CRATE`: if `now - shipTalkAt >= 20000` without the +30 coins, the bot HARD-HOLDS
  ("Luthas conversation did not prove shipment/payment").

A normal multi-round Luthas shipment dialogue (continues + possible options, tick delay up to
2000ms, 150-850ms post-proof pacing per action) can legitimately exceed 20s from talk-open —
especially with a single retry — while the conversation is progressing normally. That produces a
false hard HOLD requiring manual recovery, not a quest-mechanical failure.

Smallest correction (one line in `onProved`, same file): refresh the deadline as the dialogue
progresses, so the 20s measures payment-after-dialogue-quiescence:

```java
if ((p.key.equals("talk:luthas-ship") || p.key.startsWith("dialogue:"))
    && phase == Phase.SHIP_CRATE) shipTalkAt = System.currentTimeMillis();
```

## Latent (not a verified blocker)

`dialogue()`'s allowed list has "will you pay me for another crate full" BEFORE
"thank you, i'll be on my way", with no phase gate. QuestHelper's `talkToLuthasAgain`
(the shipment talk) has NO required dialog steps — shipment is automatic on talk — so this only
matters if the game ever presents both options during SHIP_CRATE, in which case the bot picks the
repeat-work option. Consider phase-gating that fragment to TALK_LUTHAS (same pattern as the
existing `varp == 0` gate on "yes"). Not verified live; no change demanded.

## HUMAN_PACING.md acceptance

Met: one useful action per tick, proof predicate before the next action, post-proof bounded
pacing (150-450ms talk/dialogue, 300-850ms actions, 500-1200ms on varp change), variation only
among reachable equivalent targets (bananaTree picks among reachable Pick-able trees;
reachableAccess for crate/door tiles), exact quest tiles/doors/NPCs fixed, diagnostic HOLD after
bounded failures instead of retry loops. The 20s SHIP_CRATE hold above is the one place where the
"bounded" part can misfire on a healthy-but-slow dialogue.

## Feed state (no live Pirate result yet — consistent with Alex's note)

06:06:14 screenshot: game client mid-Jagex-system-update ("System update in: 0:34"), bot parked
stage=DONE / questState=FINISHED after Witch's Potion (17 QP); diag build=554, PID 29180, error=none.
No screenshots since 06:06:14 — feed quiet ~16 min, explained by the update kick (~06:06:48 EDT);
Supervisor owns relog. Version.txt = 553.

Verdict: no hard quest blocker in the stage-1 smuggling flow; one concrete payment-timeout
robustness defect with smallest correction above; latent option-order note. Alex owns the next
integration — Muse review-only, no edits/deploys.
