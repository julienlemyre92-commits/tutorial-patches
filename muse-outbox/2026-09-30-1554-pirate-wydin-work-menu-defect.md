# Pirate Build 568 HOLD: missing "work out front" dialogue fragment — 2026-09-30 15:54 EDT (Muse, review-only)

Live state: bot entered `HOLD` at ~15:46:55 EDT (first HOLD screenshot 15-46-55; RETRIEVE_SMUGGLED_RUM up to 15:45:25, WAIT_INVENTORY 15:46:10) and has held ~7 min with Wydin's option menu open on every screenshot through 15-52-55:

- "Yes, can I work out front now?"
- "Yes, are you going to pay me yet?"
- "No, it's a complete mess"
- "Can I buy something please?"

## Verified defect (not speculative)

`source-review/build559-piratestreasure/PiratesTreasureScript.java`, `dialogue()`, `allowed[]` (line ~869) has no fragment matching "Yes, can I work out front now?":

- `"yes"`/`"yes please"` require exact `normalize(option).equals("yes")` / `.equals("yes.")` — option normalizes to `yes, can i work out front now?`, no match.
- Closest fragment is `"well, can i get a job here"` (a different menu in a different step) — `contains` fails on all four options.
- `"can i journey on this ship"`, `"will you pay me for another crate full"` etc. — all fail.

So after 12s of this menu being open, `hold("Unexpected Pirate's Treasure choice varp=" + f.varp + " phase=" + phase + " options=" + f.options)` fires (line ~893), matching the observed HOLD.

Also verified in the **shipped** build: `strings` on `PiratesTreasureScript.class` inside `patches/patch-567.zip` (Build 568, class dated 06:16) shows the dialogue fragments still only `"i'm in search of treasure"`, `"can i journey on this ship"`, `"well, can i get a job here"` — no `work out front` fragment. The defect is live in Build 568.

## Suggested fix (yours to make/verify)

Add `"can i work out front now"` to the `allowed` fragments. Note the correct post-Karamja Wydin flow from the wiki: talk to Wydin → "Yes, can I work out front now?" → he sets you working, then search the crate in his back room for the smuggled rum. If the other three options are ever needed ("pay me yet" = post-stocktake pay), add those too — but the observed live blocker is option 1.

No action taken by me — review-only per standing arrangement.
