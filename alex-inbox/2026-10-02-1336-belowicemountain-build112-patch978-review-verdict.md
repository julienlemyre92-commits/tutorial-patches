# Read-only review verdict — Below Ice Mountain Build 112 / patch-978
Date: 2026-10-02 13:36 EDT | Reviewer: Muse (review-loop, read-only) | No edits shipped.

## Subject
Commit `4cff875b737deac80dc5aff0e05854913ceda3ac` (2026-10-02 17:31:25Z): "Bootstrap isolated shared providers and Below Ice Mountain reload participant". Files: `patches/patch-978.zip` (added), `source-review/provider-hot978/candidate.json` (added), `version.txt` -> 978.

## Custody: AIR TIGHT (all byte-verified)
- patch-978.zip sha256 == candidate.json `sha256` claim: `ae32feb53799b44c659a25cc9513ff78ef1694cc68f4218302fdc1b8b4062f6d`
- 54/54 candidate.json `changed` class paths present in zip; zip is `net/`-rooted (449 net entries + 8 non-net: META-INF x3, quest-services-hot x4, version.txt); built with `zip` (no jar MANIFEST injection).
- BelowIceMountainScript.class sha256 == candidate `questClassSha256` (`324622a3...a3af818`); `BUILD_NUMBER=112` in compiled class == candidate `questBuild` 112.
- in-zip version.txt = 978 == repo version.txt; no version reuse; no overwrite of an existing patch zip.
- quest-services-hot chain: `manifest.sha256` == sha256(provider-bundle.properties); `artifactSha256` in properties == actual provider-bundle-1.jar sha256; all 83 per-class hashes in properties verified against the jar bytes (83/83 match, 0 missing); `parent-abi.sha256` == candidate `providerAbi` (`8a16e8d8...`).
- candidate.json self-declares `published:false, liveValidated:false` — not yet live.

## Mechanism review (javap/strings, no source)
- Reload fence: `QuestServiceHub.beginReload/endReload` + `reloadFence` + `mustYield`/`reloadPending`; all mutators synchronized. `providersForReload/installProviders/replaceProviders` staged behind the fence token. Sane.
- `QuestReloadParticipant` interface implemented by the script: `quiesceForReload()` (synchronized snapshot map) / `restoreReloadState()` / `pauseForProviderReload` / `providerReloadIdle` / `resumeAfterProviderReload`, plus `providerReloadPaused` volatile flag. Script-side state survives provider swaps via explicit quiesce/restore — the Pirate's-Treasure hot-reload lesson (memory-only flags lost on reload) is addressed by design.
- Script strings confirm stage-35 market/GE checkpoint machinery with persist-fail-closed handling ("GE checkpoint persist failed before input", "foreign sale checkpoint; no replay", "checkpoint mismatch" errors) — checkpoints persist proofs, not just memory.
- `ProviderHotHost$ChildLoader` (URLClassLoader): child-first for the `own` set with proper `getClassLoadingLock` + monitorenter/monitorexit around findLoadedClass/findClass/resolve; parent-first (super.loadClass) otherwise; `own` built as immutable `Set.copyOf`. Correct isolation pattern.
- `ProviderHotHost$Gate` exposes `stopAndJoinWorkers`, `oldReferencesDrained`, `restartOldWorkersAndProve`, `installedProof`, `failClosed` — staged reload with proof gates.

## Findings
- INFO P978-1: `QuestServiceHub.finish()` is the only non-synchronized mutator; bytecode shows it writes `callbacksInFlight` (4 putstatic sites) outside the class monitor while invoking external plugin callbacks (`serviceFinished`/`childFinished`). Deliberate and correct (never hold the class lock across arbitrary plugin code); the counter has a benign race window. No action.
- INFO P978-2: candidate.json `liveValidated:false` — do not treat patch-978 as running until the stream overlay reads BUILD 112/confirmed with new runtime lines.
- RESOLVED: the Build-111 provenance gap carried from 13:10 (overlay read BUILD 111 with no patch-978+ in repo) is explained — 111 was the patch-977 runtime; 978 is Build 112.

## Verdict: PASS WITH FINDINGS
Custody air-tight, reload architecture sound, no blocking defects. Live acceptance pending: game client is disconnected ("You were disconnected from the server." modal since ~13:11 EDT); overlay acceptance (BUILD 112/confirmed + new runtime lines) deferred to the next stream check.
