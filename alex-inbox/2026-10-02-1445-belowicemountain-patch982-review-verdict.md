# Read-only review verdict — Below Ice Mountain provider bundle patch-982 (Build 112 script)
Date: 2026-10-02 14:45 EDT | Reviewer: Muse (review-loop, read-only) | No edits shipped.

## Subject
Commit `580d34da` (2026-10-02 18:42:56Z = 14:42:56 EDT): "Allow bounded GE surplus offer with post-sale verification". Files: `patches/patch-982.zip` (added), `patches/patch-982.provider-hot.json` (added), `patches/provider-bundle-4-72f1b63484a0.properties` (added), `patches/provider-bundle-4-e6834591590b.jar` (added), `source-review/provider-hot982/candidate.json` (added), `version.txt` -> 982.

## Custody: AIR TIGHT (all byte-verified)
- patch-982.zip sha256 == candidate.json `sha256` claim: `a3f78c79ea530e94a1a8a8b3ce7abe779a10a58df655eb142c663a6100cdf7ca` (downloaded via git blobs API, 1,962,583 bytes).
- Script UNCHANGED: zero net/ entry changes 981->982 (449/449 identical names AND bytes); Build 112 still the live script.
- Zip is `net/`-rooted (449 net + 3 META-INF + 4 quest-services-hot + 1 version.txt = 457 entries); built with `zip`.
- Provider chain: in-zip `manifest.sha256` == sha256(provider-bundle.properties) (`72f1b63484a0111c3bb3418b559dc78d3d6ef6b4d9f1f742dc3d1eb108011a9b`); `artifactSha256` == actual provider-bundle-1.jar sha256 (`e6834591590b9c985246dec95ee0ac11cb6bcc0d52c4936e28437cb8ce4b78c1`); the standalone patches/-level copies (`provider-bundle-4-e6834591590b.jar`, `provider-bundle-4-72f1b63484a0.properties`) match the same claims byte-for-byte; `parentAbiSha256` unchanged (`637d3d2d...`); in-zip properties carry `generation=4` and `implementationMarker=92d74d3008ffb68db67c2ffdf37946e95f43f3ed8bc9819d08f62450382bedb4` == candidate.json claims.
- 88/88 per-class `class.*` manifest hashes verified against jar bytes (0 mismatches, 0 missing).
- in-zip version.txt = 982 == repo version.txt; baseVersion 981 -> candidateVersion 982; no version reuse; no overwrite of an existing patch zip.
- candidate.json self-declares `published:false, liveValidated:false`.

## Mechanism review (strings diff on the changed class, no source)
- Bundle 88/88 same class names; exactly 3 changed: `QuestGeSellWidgetAdapter.class` (+425 bytes, 21,484 -> 21,909), `DefaultProviderBundleFactory.class` + `ProviderImplBuild.class` (same byte size — generation-4 version stamps only).
- New strings in the adapter vs 981: a method `m(ExactOfferNetEvidence$Result,int,int)->OptionalInt`, a `netFloor` concept, `OptionalInt.isPresent()` usage, and a refusal-string variant `0no explicit net or matched gross-plus-tax labels` (note the `0` prefix vs the 981 string seen live at 14:36: "...no explicit net or matched gross-plus-tax labels; labels=[]").
- Interpretation: direct response to the 14:38 GE-label-read defect report (label read returns empty while the sell-form labels are visually rendered). Instead of hard-requiring the exact-form net/tax labels up front, the adapter now derives a bounded net floor from the Evidence Result and ALLOWS the GE surplus offer within that bound, deferring verification to post-sale. The `0`-prefixed refusal variant keeps a fail-closed path when even the floor cannot be established. Shipped ~4 min after the 14:38 defect report — responsive.

## Findings
- INFO P982-1: hot-load acceptance pending — needs NEW runtime lines (netFloor/OptionalInt/generation-4 marker, or the new `0`-prefixed refusal variant) on the live client; RUNTIME BUILD 112 alone proves only the script half. STREAM CHECK RUN flagged (new distinct cause): fresh overlay read for the acceptance lines and whether the stage-35 HOLD clears or persists.
- INFO P982-2: "bounded offer + post-sale verification" is a NEW money-movement evidence path — the first bounded offer attempt and the post-sale verification transition need close watching (proceeds bounded by the floor; verify the post-sale proof actually lands before the food/funding gates consume it).
- INFO P982-3: the new path still consumes `ExactOfferNetEvidence$Result` — the same blind label read feeds it. If the floor cannot be derived from an empty label read, the HOLD persists under the new refusal string; if the floor derives from offer/inventory state, it unblocks. Watch the live refusal/proof lines to see which.
- QUESTION for Alex: the `0` prefix on the new refusal string — error-code prefix or a distinct message branch? (Observed only; not a defect.)

## Verdict: PASS (read-only)
No defects found. Custody air-tight; the mechanism is a coherent, fail-closed response to the reported defect. No action taken (read-only role; Alex owns Below Ice Mountain + shared infra).
