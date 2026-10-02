# Muse read-only review verdict: Below Ice Mountain Build 109 / patch 972

**Verdict: PASS** — read-only review, no release authority exercised (Alex owns implementation/releases).

## What shipped (commit 6d289ebfa992, 2026-10-02T13:50:54Z = 09:50:54 EDT)
- version.txt=972 (bump only). Note: commit message text "Below Ice Mountain Build107 observed GE inventory Offer widget" is a reused template — disambiguate by SHA, as with Build 108.
- files: patches/belowicemountain-109.jar, patches/patch-972.hot.json, patches/patch-972.zip, source-review/belowicemountain-build109/BelowIceMountainScript.java, source-review/.../README.md, version.txt.
- hot.json: {plugin: belowicemountain, patch: 972, build: 109, sha256: 1011676dc3192f153c7c66abd40c86ab1a7cca64d25363e3aff3b1e0c4125984}.

## Change (per README section "Build 109 / patch 972: read-only owned-offer recovery diagnosis")
Build 108 placed one owned sapphire sale; the offer remained unfilled and a single View attempt did not prove the detail pane, so Build 108 stopped at a durable VIEW_SENT checkpoint. Build 109 reads only that exact Build 108 checkpoint, records native offer state + the GE widget tree once, and HOLDs. No sale, abort, collect, or repost input. User's fast-sale policy is a 20s fill threshold; future repricing requires proven abort, return collection, and a price floor before a single lower-price repost.

## Custody verified
- belowicemountain-109.jar sha256 == hot.json sha256 exactly (1011676d…).
- patch-972.zip: 318 entries, all under net/ (same shape as 970/971) plus root META-INF/ + version.txt.
- The checkpoint SHA gate in Build 109 source (`22f16a83ba99f0527044283a7fe5785d8cde3f5c8ffed206b97c06d78e75967a`) was hashed independently against the Build 108 class from belowicemountain-108.jar — EXACT match. The probe will recognize the genuine Build 108 VIEW_SENT checkpoint and fails closed otherwise.

## Logic review (new path: stage35MarketRecoveryProbe, ~L2176)
- Call site (L1059): runs only in the under-supplied recovery branch (stage 35, overworld prep, no guardian, food<16 or healing<160) when the MARKET_SALE checkpoint file exists — diagnosis is attempted BEFORE stage35MarketSale/stage35MarketProbe.
- Gates: LOGGED_IN, within 10 tiles of GE, not in cave instance, hp>0 — any failure throws → catch-all → hold.
- Checkpoint binding: schema MARKET_SALE_1 + account + ProcessHandle.current().pid() + build "108" + class SHA + phase VIEW_SENT + slot 0 + item 1623 (uncut sapphire). All-or-nothing; mismatch → "Build108 sale checkpoint mismatch" → hold.
- Read-only confirmed: marketSaleView() is pure reads (inventory container, c.getGrandExchangeOffers()[0], widget tree via client thread); marketProbeDump() is a widget-tree text dump — no clicks, no inputs anywhere on this path.
- Single-shot flag marketSaleRecoveryDumped; every uncertain branch ends in hold (terminal, no replay).
- Soft notes: (S1) probe is one-shot per session — a transient native-snapshot absence burns the single attempt, but the catch-all still fails closed to hold; (S2) commit message reuse again — SHA disambiguation remains the rule.

## Live acceptance (pending, stream-only — feed still dark ~40h)
Watch for: STAGE35_MARKET_RECOVERY_STATE log line (offer/item/qty/price/filled/carriedSapphire/coins/detail) and the BUILD108_VIEW_SENT_RECOVERY UI-probe dump, i.e. Alex's claim that the Build 108 Confirm click landed and the offer sits unfilled. Cannot be verified from the repo — needs the live client/stream.

2026-10-02 ~09:54 EDT — Muse, read-only reviewer.
