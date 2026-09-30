# Pirate Build 555/556 review: concrete blocker at Redbeard Frank ("Yes." never matches)

Hi Alex -- Muse here, bounded read-only review per your 0618 task (varp71 stage-1 smuggling flow; Luthas crate/payment proof and Wydin door-to-crate access). I found one concrete, verified quest blocker -- earlier in the flow than the focal points (varp 0), which gates everything behind it.

## The blocker (verified, not speculative)

File: `source-review/build556-piratestreasure/PiratesTreasureScript.java` (identical logic in build555), `dialogue()`, lines 604-607:

```java
for (String fragment : allowed) for (String option : f.options)
    if ((!fragment.equals("yes") || f.varp == 0)
        && (fragment.equals("yes") ? normalize(option).equals("yes")
            : normalize(option).contains(fragment))) {
```

The `"yes"` fragment requires exact `normalize(option).equals("yes")`. But Redbeard Frank's second option is **"Yes." with a trailing period**. Verified two independent ways:

1. Installed Microbot QuestHelper bytecode (`helpers/quests/piratestreasure/PiratesTreasure.class`): the Frank NpcStep registers the exact dialog strings `"I'm in search of treasure."` and `"Yes."` (javap `ldc`). QuestHelper matches dialog options against these exact strings, so the live widget text is `"Yes."` -- the period is real.
2. Your own live-completed Witch's Potion Build 553 script already learned this: its `knownOption` reads `if ("yes".equals(fragment)) return "yes".equals(actual) || "yes.".equals(actual);`. The `"yes."` alternative was dropped in the Pirate rewrite.

Effect: at varp 0, after clicking "I'm in search of treasure.", the option screen shows `["Yes.", "No."]` -> no fragment matches -> 12s -> `HOLD "Unexpected Pirate's Treasure choice varp=0 phase=START_FRANK options=[Yes., No.]"`. The quest can never start; Luthas, Wydin, and every later stage are unreachable until this is fixed. (Also note: the exact-match exists to keep "yes" from matching "Yes please." at the seaman -- the fix below preserves that.)

## Checked and NOT blockers (for the record)

- Luthas crate/payment proof (Build 556): `shipCoinBefore` captured pre-talk; payment timer resets on each proved dialogue action; coin-delta >= +30 advances even mid-dialogue; bounded 20s HOLD otherwise. Sound.
- Wydin door-to-crate: crate 2071 @ (3009,3207) confirmed in QuestHelper's ObjectStep; the script already treats it as a location-not-tile via `reachableAccess` over `getReachableTilesFromTile` -- matches your 0605 caution, no change needed there. Door 2069/2070 is NOT in QuestHelper (your validation-gap note stands; needs the live scene). Wydin NPC id 2890 verified on the OSRS Wiki; apron-equipped-before-job ordering matches the wiki ("He will agree as long as you are wearing your white apron").
- Remaining dialogue fragments verified verbatim against QuestHelper's RumSmugglingStep: "Could you offer me employment on your plantation?", "Will you pay me for another crate full?", "Well, can I get a job here?", "Can I journey on this ship?", "Search away", "Ok thanks, I'll go and get it.", "Thank you, I'll be on my way".

## Smallest correction

In `dialogue()`, accept the trailing period for the yes-branch, mirroring the Witch script:

```java
fragment.equals("yes") ? ("yes".equals(n) || "yes.".equals(n))
```

with `String n = normalize(option);` -- or strip trailing `[.?!]` inside `normalize()`. No other fragment depends on trailing punctuation, and `"Yes please."` still cannot match `"yes"`.

Muse review-only -- no edits, builds, deploys, or game control. Integration is yours.
