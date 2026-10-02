# Read-only review verdict — Below Ice Mountain provider bundle patch-980 (Build 112 script)
Date: 2026-10-02 14:21 EDT | Reviewer: Muse (review-loop, read-only) | No edits shipped.

## Subject
Commit `6f25368a` (2026-10-02 17:55:20Z): "Bootstrap isolated shared providers and Below Ice Mountain reload participant". Files: `patches/patch-980.zip` (added), `source-review/provider-hot980/candidate.json` (added), `version.txt` -> 980.

## Custody: AIR TIGHT (all byte-verified)
- patch-980.zip sha256 == candidate.json `sha256` claim: `1a3ba55a0292b2b9daab6fc1dfe70f8e9a254eed8ede8f8c44e3e811d3e40bf1` (downloaded via git blobs API, 1,955,092 bytes).
- Script UNCHANGED: zip carries no quest script class changes (changed = quest-services-hot/* + version.txt); Build 112 still the live script.
- Zip is `net/`-rooted (449 net entries + 8 non-net: META-INF x3, quest-services-hot x4, version.txt); built with `zip`.
- Provider chain: `manifest.sha256` == sha256(provider-bundle.properties); `artifactSha256` == actual provider-bundle-1.jar sha256 (`fbab73fb...`); `parent-abi.sha256` == candidate `parentAbiSha256` (`637d3d2d...`); 83/83 per-class `class.*` hashes verified against jar bytes (0 mismatches, 0 missing).
- in-zip version.txt = 980 == repo version.txt; baseVersion 979 -> candidateVersion 980; no version reuse; no overwrite of an existing patch zip.
- Bundle diff vs patch-979 (97/97 same class names): exactly 3 classes changed — `FoodAcquisitionService.class`, `ProviderImplBuild.class` (version stamps), `DefaultProviderBundleFactory.class`. All other 94 classes byte-identical.
- candidate.json self-declares `published:false, liveValidated:false`.

## Mechanism review (javap diff of the 3 changed classes, no source)
- `FoodAcquisitionService` (tick remains synchronized; FileChannel+FileLock checkpoint held): the diff adds a "persisted food/funding transaction" gate family with explicit fail-closed refusal strings — "persisted food/funding transaction: foreign or pending food action", "fresh bank audit unavailable", "coin movement lacks bank reserve proof", "GE slots unknown or nonempty", "funding checkpoint remains" — plus a new `REPLAN_READY` phase path (`[FoodAcquisition] REPLAN_READY priorPid={} currentPid={} priorCoins={} carriedCoins={} auditedBankCoins={} foodId={}`) recording replanPid/replanCarriedCoins/replanAuditedBankCoins. This is the persisted-proof-recovery half of the P979-2 risk: after a disconnect/restart the service re-derives state from persisted proofs instead of memory.
- Design consistent with the fail-closed philosophy verified in 979 (proof-before-input, no-replay, bounded reservations): new gates refuse action rather than act on unproved state.
- `ProviderImplBuild` / `DefaultProviderBundleFactory`: version-stamp/providerMarker changes only (generation 2), no logic surface.

## Live corroboration (stream, 14:20-21 EDT, broadcast confirmed LIVE)
- Character LOGGED IN at the Bank of Gielinor (Grand Exchange area), bank interface open, idle: "POSITION UNCHANGED: 21m 37s". The ~13:11 EDT disconnect modal is resolved.
- Overlay: `RUNTIME BUILD: 112 / confirmed` | `SCRIPT STEP: Wait shared qoi` (rendered; likely "Wait shared qol") | `QUEST STATUS: In progress` | `LIVE ACTIVITY: Checking swap guards`.
- Runtime log (bottom-left): `[13:59:07] [BelowIceMountain] HOLD Shared preparation: HOLD Nested FOOD_RESTOCK`.
- Overlay wait notes (14:18-14:20): "The same step has been reported for two minutes. No confirmed stage change has arrived yet."
- Bot-relevant diagnostic text visible on stream: `HOLD: service child failed: UNAVAILABLE: no proved permitted F2P bank surplus sale` and `No approved surplus, gathering requires a separately proved sale path`.

## Findings
- INFO P980-1: the live HOLD since 13:59 (~21 min idle at the bank, FOOD_RESTOCK nested under Shared preparation) is the fail-closed design biting as designed — the service refuses GE/bank money movement until a "proved permitted F2P bank surplus sale" path exists. This matches the P979-2 watch item exactly. It is SAFE (bank, no spend, no replay) but it is also indefinite: there is no evidence yet of a component that *produces* the sale-path proof the gate is waiting on. If no producer exists, this HOLD never clears on its own. Alex's call whether the sale path is wired as a separate provider or the gate needs a producer.
- INFO P980-2: patch-980's persisted-transaction gates (the "REPLAN_READY" path) cannot be live-validated yet — acceptance needs NEW runtime lines mentioning persisted food/funding transaction handling or the REPLAN_READY marker; RUNTIME BUILD 112 alone proves only the script half. Watch the first FOOD_RESTOCK transition after the sale-path proof arrives.
- INFO P980-3: overlay renders "Wait shared qoi" — likely "qol" truncated/misrendered by the overlay; cosmetic only.

## Verdict: PASS WITH INFO FINDINGS
Custody air-tight, fail-closed mechanism sound, no concrete defects, no action taken (read-only role; Alex owns Below Ice Mountain + shared infra). The bot is safe-idle in a proven-safe state; the open question is who produces the proved F2P bank-surplus sale path that FOOD_RESTOCK is HOLD-ing for.
