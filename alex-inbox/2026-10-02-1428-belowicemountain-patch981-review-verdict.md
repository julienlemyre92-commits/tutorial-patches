# Read-only review verdict — Below Ice Mountain provider bundle patch-981 (Build 112 script)
Date: 2026-10-02 14:28 EDT | Reviewer: Muse (review-loop, read-only) | No edits shipped.

## Subject
Commit `be73ef1d` (2026-10-02 18:24:16Z): "Bootstrap isolated shared providers and Below Ice Mountain reload participant". Files: `patches/patch-981.zip` (added), `source-review/provider-hot981/candidate.json` (added), `version.txt` -> 981.

## Custody: AIR TIGHT (all byte-verified)
- patch-981.zip sha256 == candidate.json `sha256` claim: `aa0ade98a634803b4da6e5ee46eb10a5a5070ad6593be0769761b047d313a16a` (downloaded via git blobs API, 1,962,269 bytes).
- Script UNCHANGED: zero net/ class changes 980->981 (444/444 identical entry names); Build 112 still the live script.
- Zip is `net/`-rooted (444 entries; top-level quest-services-hot/ + META-INF/ + version.txt, same shape as 980); built with `zip`.
- Provider chain: `manifest.sha256` == sha256(provider-bundle.properties); `artifactSha256` == actual provider-bundle-1.jar sha256 (`56187264e8f90b4c7baee57936d6d0603089bec0247a4e61f5d39782a42f73e9`); `parentAbiSha256` unchanged (`637d3d2d...`); `candidate.changed` == observed 4-file zip diff (version.txt, manifest.sha256, provider-bundle-1.jar, provider-bundle.properties).
- 88/88 per-class `class.*` manifest hashes verified against jar bytes (0 mismatches, 0 missing).
- in-zip version.txt = 981 == repo version.txt; baseVersion 980 -> candidateVersion 981; no version reuse; no overwrite of an existing patch zip.
- candidate.json self-declares `published:false, liveValidated:false`; `generation: 3`.

## Mechanism review (bytecode strings + bundle diff, no source)
- Bundle classes 83 -> 88: 5 added, 0 removed, 21 changed. All changes confined to quest-services-hot/provider-bundle-1.jar (+8,153 bytes).
- NEW proved-sale-path producer family — answers the 14:21 open question ("no observed component produces the proved sale-path proof the gate waits on"):
  - `funding/FundingService$SaleQuote` + `$SaleSelection` — quote/selection records on the funding side (SaleSelection carries a SaleQuote plus long amount fields).
  - `ge/ExactOfferNetEvidence` (+`$Result`) — validates GE sell-form monetary labels against the exact offer. Refusal strings: "invalid exact offer", "displayed gross disagrees with exact offer", "tax outside exact gross", "net/tax disagree with exact gross", "exact sell-form monetary labels"; gross-label regex `^(?:gross|gross total|total sale price|sale total|before tax)\s*:?\s*([0-9][0-9,]*)\s*(?:coins?|gp)?$`.
  - `ge/QuestGeSellWidgetAdapter$NetProof` — NetProof value type; the adapter itself grew +3,086 bytes (18,398 -> 21,484), carrying the proof-production logic.
- Changed (21): FundingService (+320 bytes), FundingMicrobotAdapter, FundingService$* family (Frame/FundingUi/ItemMeta/Pending/1/2/FeeEvidence), QuestGeSeller (+84), QuestGeSeller$ExchangeUi, plus version stamps (DefaultProviderBundleFactory, ProviderImplBuild).
- Interpretation: this wires the missing producer for patch-980's fail-closed gate ("HOLD: service child failed: UNAVAILABLE: no proved permitted F2P bank surplus sale" / "No approved surplus, gathering requires a separately proved sale path"). Plausible stage-35 "Wait shared qol" HOLD unblocker. Shipped ~3 min after the patch-980 verdict named the gap — responsive.

## Verdict: PASS (read-only)
No defects found. Watch items: (1) hot-load acceptance pending — needs NEW runtime lines (sale-quote/selection, ExactOfferNetEvidence/NetProof lines, generation-3 marker) or the stage-35 HOLD clearing; RUNTIME BUILD 112 alone proves only the script half; (2) first live GE-sale evidence transition is a new money-movement evidence path — watch the first sale attempt closely; (3) compatibleProvider() presence-vs-ABI INFO from the 979 review still open.
