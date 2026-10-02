# Read-only review verdict — Below Ice Mountain shared infra patch-979 (Build 112)
Date: 2026-10-02 13:45 EDT | Reviewer: Muse (review-loop, read-only) | No edits shipped.

## Subject
Commit `d4fa947be9409e48f62c9cf6fee5522a3b87dcee` (2026-10-02 17:36:46Z): "Bootstrap isolated shared providers and Below Ice Mountain reload participant". Files: `patches/patch-979.zip` (added), `source-review/provider-hot979/candidate.json` (added), `version.txt` -> 979.

Note on overlap: this commit landed 13:36:46 EDT mid-way through the 13:36 review run, which correctly reviewed patch-978 but recorded version.txt=978 from its pre-ship read. This review covers patch-979 only; no double-counting of 978.

## Custody: AIR TIGHT (all byte-verified)
- patch-979.zip sha256 == candidate.json `sha256` claim: `358ed1e86fb99270ec772bd21ab2ad68496137c435cf9a580291b888da50fa38` (downloaded via git blobs API, 1,954,543 bytes).
- 5/5 candidate `changed` plugin classes present in zip; quest Build UNCHANGED: BelowIceMountainScript.class sha256 == candidate `questClassSha256` (`324622a3772...a3af818`) == patch-978's value, `questBuild`=112.
- in-zip version.txt = 979 == repo version.txt; no version reuse; no overwrite of an existing patch zip; baseVersion 978 -> candidateVersion 979.
- Provider chain: `manifest.sha256` == sha256(provider-bundle.properties); `artifactSha256` in properties == actual provider-bundle-1.jar sha256 (`a8452329...`); `parent-abi.sha256` == candidate `providerAbi` (`637d3d2d4...`); 83/83 per-class `class.*` hashes verified against jar bytes (0 mismatches, 0 missing, 0 extra).
- Diff vs patch-978's provider bundle: 83/83 same class names; only 2 classes changed — `ProviderImplBuild` (version stamps: providerMarker `fdeff885...` == candidate `providerMarker`, parentAbi == candidate `providerAbi`) and `DefaultProviderBundleFactory`. All 81 logic classes byte-identical.
- candidate.json self-declares `published:false, liveValidated:false` — not yet live.

## Mechanism review (javap/strings on the 5 changed classes + coordinator + bundle)
- The 5 changed classes are thin plugin shells: each PluginDescriptor'd plugin (Quest Food Restock / Quest Funding / Quest Navigation / Quest Preparation) calls `ProviderHotFacade.start(Kind)` on startUp and `.stop(Kind)` on shutDown. Real logic remains in the unchanged 81 bundle classes.
- `ProviderHotCoordinator` (outer zip, host side, 21KB) owns the real gate: verifies `parentAbi`/`marker` before attach; failures recorded fail-closed (`FAILED_CLOSED`, `attach/start failed:`). This is where the ABI check lives, not in the plugin shells.
- FundingService (bundle): fail-closed design confirmed — FileChannel+FileLock checkpoint, synchronized tick(), `!bank input unproved; no replay`, `!coin refund not proved; no repeat`, `funding checkpoint exists; reconcile before input`, `restoredHold` flag. Matches the observed "Wait shared qol" HOLD philosophy: no spend/sale without proof.

## Findings
- INFO P979-1: `QuestFundingPlugin.compatibleProvider()` is a presence check (`QuestServiceHub.isRegistered(Kind.MONEY_MAKING)`), NOT an ABI/marker check, and `startUp()` calls `ProviderHotFacade.start()` unconditionally without consulting it. Harmless — the real ABI gate is the coordinator — but the name over-promises. Suggest renaming to `fundingServiceAvailable()` or wiring the coordinator's ABI verdict in.
- INFO P979-2: this is the first patch where the food-restock + funding plugins are wired into the hot-provider pipeline (the "banking/fighting/money-acquisition update" Alex was bug-testing). Behavioral risk surface: the first live GE/bank money movement will go through newly-instantiated service paths with providerGeneration=1. The fail-closed checkpoints (proof-before-input, bounded reservations, no-replay) are the right shape for this; watch the first live FOOD_RESTOCK/funding transition closely for the HOLD->act path.

## Verdict: PASS WITH INFO FINDINGS
No concrete defects found. No action taken (read-only role; Alex owns Below Ice Mountain + shared infra). Live acceptance still pending: the client is disconnected (modal since ~13:11 EDT), so patch-979 cannot hot-load until Julien clears it; judge acceptance only from NEW runtime lines ("RUNTIME BUILD 112" already confirmed pre-disconnect; look for provider-979/marker lines on next load).
