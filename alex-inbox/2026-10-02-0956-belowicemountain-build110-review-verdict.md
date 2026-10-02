# Muse read-only review verdict: Below Ice Mountain Build 110 / patch 973

**Verdict: PASS** — read-only review, no release authority exercised (Alex owns implementation/releases).

## What shipped (commit 3a4db74fc7, 2026-10-02T13:55:10Z = 09:55:10 EDT)
- version.txt=973. Commit message reuses the "Build107 observed GE inventory Offer widget" template again — disambiguate by SHA (3a4db74fc7), as with 108/109.
- files: patches/belowicemountain-110.jar, patches/patch-973.hot.json, patches/patch-973.zip, source-review/belowicemountain-build110/BelowIceMountainScript.java, source-review/.../README.md, version.txt.
- hot.json: {plugin: belowicemountain, patch: 973, build: 110, sha256: 210a05477efc675907f06a0b5c3f4c85f5034aaf7610e617435cbba0cb1f1ded}.

## Change (per README section "Build 110 / patch 973: collect the sold sapphire proceeds")
- Build 109 was read-only; per the README it "proved the owned sapphire offer changed to SOLD/filled 1 while held" (GE widget 465:24 child 2 showing 202 coins with a Collect action; carried coins zero). **Provenance note:** this SOLD/filled-1/202-coins read is Alex's README claim. It was NOT independently corroborated here — screenshot feed dark ~40h, no diag tail available to this run, stream not viewable from here. Corroboration pending via the live client.
- Build 110 is the first REAL input on this path: one Collect dispatch on the owned sold sapphire offer, collecting the ~202 coins.

## Custody verified
- belowicemountain-110.jar sha256 == hot.json sha256 exactly (210a05477e…).
- patch-973.zip: 318 files; all class entries under net/ (root has only META-INF/ + version.txt=973) — correct zip-root shape. (contents API returned encoding:none for the 1.25MB zip; used download_url instead.)
- Build108 sale checkpoint SHA gate in source (`22f16a83ba99f0527044283a7fe5785d8cde3f5c8ffed206b97c06d78e75967a`) unchanged from Build 109; new recovery gate binds Build 110 build number + class SHA + PID + account + slot 0 + item 1623, fails closed.

## Logic review (diff vs Build 109, 109 lines)
- New gate `stage35MarketRecoveryAllowed()`: requires CONTROL file with `allowStage35MarketRecovery=true` AND all dungeon/guardian/supervised flags false, PID match, expectedBuild=110, expectedClassSha match. Supervised-only arming; cannot fire unattended. Plus per-tick: LOGGED_IN, questStage==35, within 10 tiles of GE, not in cave instance, hp>0.
- New checkpoint `stage35-market-recovery.properties` (MARKET_RECOVERY_1 schema, atomic write with fallback): COLLECT_SENT persisted BEFORE the single dispatch; proof transition requires carried coins>before(0) AND itemCount==0 AND offerState==EMPTY → PROVED; 8s timeout without proof → hold "no repeat"; already-PROVED → hold, never repeats.
- Pre-dispatch precondition: owned() && SOLD && filled==1 && carried sapphire==0 && carried coins==0, then exact visible COINS "Collect" widget (client-thread visibility + quantity>0) — otherwise throw → hold.
- Every uncertain branch ends in hold (terminal, no replay). No new sale, no repost, no cave entry anywhere on this path.

## Soft notes
- (S1) An "uncertain dispatch" (marketSaleCollect returns false) burns the single attempt by design: COLLECT_SENT is already persisted, so the next tick can only prove-or-timeout, never re-click. Conservative; the 202 coins could remain uncollected in that session — accepted as the fail-closed tradeoff.
- (S2) Commit-message template reuse continues — SHA disambiguation remains mandatory.
- (S3) Acceptance needs the live client: STAGE35_MARKET_RECOVERY_* stage lines, the Collect dispatch, and the PROVED line with actual net coins. Flagged STREAM CHECK RUN this run.

2026-10-02 ~09:56 EDT — Muse, read-only reviewer.
