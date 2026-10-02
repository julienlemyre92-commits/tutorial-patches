# Read-only review verdict — Below Ice Mountain provider bundle patch-983 (Build 112 script)
Date: 2026-10-02 14:47 EDT | Reviewer: Muse (review-loop, read-only) | No edits shipped.

## Subject
Commit `6d1b655c` (2026-10-02 18:46:25Z = 14:46:25 EDT): "Read compact GE net proceeds from verified sell form". Files: `patches/patch-983.zip` (added), `patches/patch-983.provider-hot.json` (added), `patches/provider-bundle-5-b67118147bc5.properties` (added), `patches/provider-bundle-5-ea9fbbe8bee4.jar` (added), `source-review/provider-hot983/candidate.json` (added), `version.txt` -> 983.

## Custody: AIR TIGHT (all byte-verified)
- patch-983.zip sha256 == candidate.json `sha256` claim: `f0d3d4e696e36774665ef67101a6aa05ea0d95e9d580be3f5719e00339be0276` (downloaded via git blobs API, 1,962,406 bytes).
- Script UNCHANGED: 449/449 net/ names identical AND byte-identical 982->983; Build 112 still the live script.
- Zip is `net/`-rooted (449 net + 3 META-INF + 4 quest-services-hot + 1 version.txt = 457 entries); built with `zip`.
- Provider chain: in-zip `manifest.sha256` == sha256(provider-bundle.properties) (`b67118147bc5ca8b03390201ac031e14fa6834ebb65a7d8f9dd9bdb9920174ca`); `artifactSha256` == actual provider-bundle-1.jar sha256 (`ea9fbbe8bee4d069fa997ccf45597a233f0a3a5b705b6c53f8ff480e94f7155a`); the standalone patches/-level copies (`provider-bundle-5-ea9fbbe8bee4.jar`, `provider-bundle-5-b67118147bc5.properties`) match the in-zip copies byte-for-byte; `parentAbiSha256` unchanged (`637d3d2d...`); in-zip properties carry `generation=5` and `implementationMarker=a8635e655ec26428ddb284dcbb6eecb4b1ee2e1df2988604c96f5ac0468f505b` == candidate.json claims.
- 88/88 per-class `class.*` manifest hashes verified against jar bytes (0 mismatches, 0 missing).
- in-zip version.txt = 983 == repo version.txt; baseVersion 982 -> candidateVersion 983; no version reuse; no overwrite of an existing patch zip.
- candidate.json self-declares `published:false, liveValidated:false`.

## Mechanism review (strings diff on the changed classes, no source)
- Bundle 88/88 same class names; exactly 4 changed: `ExactOfferNetEvidence.class` (+259 bytes), `QuestGeSellWidgetAdapter.class` (-425 bytes), `DefaultProviderBundleFactory.class` + `ProviderImplBuild.class` (same byte size — generation-5 marker stamps only).
- ExactOfferNetEvidence gains: new regex `^([0-9][0-9,]*)\s+coins?\s*\(([0-9][0-9,]*)\s*-\s*2%\)$` — matches the compact "N coins (M - 2%)" label (the GE sell form's compact net-proceeds rendering, e.g. "1,984 coins (2,024 - 2%)" seen on stream at 14:36); new `COMPACT` flavor alongside `GROSS`/`TAX`; new `TAX\t` label key. This is the direct fix for the 14:38 defect finding: the 981/982 reader looked for separate gross-plus-tax labels and found none (`labels=[]`), while the form renders them combined.
- QuestGeSellWidgetAdapter LOSES the 982 experiment: `netFloor` concept gone, `m(ExactOfferNetEvidence$Result;II)->OptionalInt` gone, `isPresent` usage gone, and the adapter-side `0`-prefixed refusal variant removed from its label table (label-parse table changed: `h:`/`hN,,` out, `V:\t,`/`VN,,` in). Note: the `0`-prefixed string still EXISTS inside ExactOfferNetEvidence.class (see findings), so the QUESTION from the 982 verdict stands.
- Interpretation: 983 reverts 982's bounded-offer/netFloor gate wholesale and instead teaches the evidence reader to match the compact label the sell form actually renders. Cleaner than 982 — one read-path fix instead of a parallel gate — and it answers the 14:38 defect at its root (reader/form mismatch, not missing state).

## Findings
- INFO P983-1: hot-load acceptance pending — needs NEW runtime lines on the live client (COMPACT evidence read, compact net-proceeds lines, or the stage-35 HOLD clearing); RUNTIME BUILD 112 alone proves only the script half. Stream check spawned this run (live overlay read of https://www.youtube.com/live/T-Uj1Rxo4a8); result pending.
- INFO P983-2: money-movement watch — the first sell attempt under the compact read path plus its post-sale verification transition need close observation (the 981/982 fail-closed posture must survive the new read).
- INFO P983-3: refusal-string family in 983's ExactOfferNetEvidence: "invalid exact offer", "unavailable", "ambiguous or invalid fee text", "displayed gross disagrees with exact offer", "0no explicit net or matched gross-plus-tax labels", "tax outside exact gross", "!net/tax disagree with exact gross". If the compact regex fails to match the live form's exact rendering, the HOLD re-nests under one of these — watch the live lines to see which (or whether it clears).
- QUESTION (carried from 982 verdict, still live): the `0` prefix on "0no explicit net or matched gross-plus-tax labels" — error-code prefix or distinct message branch? Observed only; not a defect.

## Verdict: PASS (read-only)
No defects found. Custody air-tight; the mechanism is a coherent root-cause fix for the reported label-read defect (reader now matches the form's compact rendering instead of routing around it). No action taken (read-only role; Alex owns Below Ice Mountain + shared infra).
