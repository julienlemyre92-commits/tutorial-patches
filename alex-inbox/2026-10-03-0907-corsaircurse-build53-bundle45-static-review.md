# Corsair Curse patch-1103 / provider-bundle-45 — read-only static review (Muse, 2026-10-03 ~09:07 EDT)

Verdict: **PASS static.** Live acceptance pending: generation-45 hot-load -> BANKING
provider tick -> PreparationBankService pass (food/coins/free-slots, then gear pass) ->
checkpoint growth beyond 12/50 -> Ithoi re-engagement. Judge only by fresh runtime lines.

## Identity / integrity (all via GitHub API, jar downloaded direct)
- version.txt=1103 (commit 821ab733, 13:05:53Z, "Bank route chooses policy-verified F2P
  walk"). Fresh patch number — no reuse.
- patch-1103.provider-hot.json: patch 1103, generation 45, parentAbi d8911107...,
  artifactSha256 5be33f17..., manifestSha256 ef9b8ca8..., implementationMarker 00e82cbc...
- provider-bundle-45.jar sha256 = 5be33f170bd67a0b7d1747e3122fa34a73daa1271a266a2b669e599c3aff7477
  == hot.json artifactSha256 == properties artifactSha256.
- Per-class sha spot-checks (jar vs properties manifest): PreparationBankService
  821e4d51... OK; PreparationMicrobotAdapter c5e1d7c2... OK; gear/GearUpgradePlanner
  519dfcfd... OK; gear/GearRecoveryMicrobotAdapter 032a2dee... OK;
  gear/GearRecoveryController edeebb7c... OK.
- patch-1103.zip: 482-class entry list identical to patch-1102.zip; only byte-diffs are
  quest-services-hot/manifest.sha256, provider-bundle-1.jar (= the bundle-45 jar),
  provider-bundle.properties, version.txt (in-zip manifest.sha256 = ef9b8ca8... == hot.json).
  The Corsair script classes are byte-identical — still Build 53; correct, no script change
  was needed for this generation.

## What changed in generation 45
- source-review/recovery-1103/ (read in full): PreparationBankService, PreparationMicrobotAdapter,
  GearUpgradePlanner (+ gear trio carried over).
- DefaultProviderBundleFactory.java: byte-identical to bundle-44 — provider wiring
  (NAVIGATION / FUNDING / FOOD_RESTOCK / BANKING / TRAINING) unchanged.
- GearRecovery{Controller,Catalog}: byte-identical. GearRecoveryMicrobotAdapter: one-line
  improvement — `equipmentObserved` now comes from the observed-unequipped-appearance check
  instead of `equipment!=null`, closing the stripped-post-death container gap (the EQUIPMENT
  container can be null after death until the first equip).
- PreparationBankService tick: single-flight pending intent, 8s proof deadline -> terminal
  HOLD (no replay); atomic PREP_BANK_2 checkpoint; restore() re-proves or HOLDs — never replays
  an input from a previous process; child delegation only on a proved closed-bank frame (or an
  AUDITED open-bank frame for FOOD_RESTOCK); navigation-arrival proof required before planning;
  gear pass runs before final COMPLETE; 10s snapshot-wait degrades to UNAVAILABLE when no input
  was dispatched; snapshot failure while holding = HOLD with pending proof intact.
- PreparationPlanner (source not shipped; decompiled from the jar with CFR): pure decision
  function. Gates: fresh same-account frame (<=3s), deadline, F2P-only-on-members -> HOLD,
  combat -> HOLD, skill shortfall -> TRAINING need, equipment shortfall -> EQUIPMENT need.
  Withdrawals are bank-stock-only; shortfalls raise UNSUPPORTED/ACQUISITION needs (routed to the
  child food/funding providers) rather than spending silently; surplus-deposit frees slots while
  the required-supply/coin reserve is never violated; terminal "unclassified preparation state"
  is a fail-closed HOLD.
- GearUpgradePlanner: bank-stock-only, zero-spend upgrades; two-handers excluded; members items
  excluded under f2pOnly; free-slot reserve protected; 4s per-action proof deadline -> HOLD with
  no repetition; attempted-set persisted; beginDispatch re-verifies the decision is still live;
  afterBankClosed re-proves the carried candidate before EQUIP_ONE.
- PreparationMicrobotAdapter: bank targets only from observed targets within 8 tiles on the
  SAME plane (patch-1097 same-plane fix carried over); withdraw-as-item audited; account identity
  hashed; equipment-observed from live untransformed appearance.

## Open note (not a defect)
- The gear-pass Goal weights use bonus key "STRENGTH" ("MELEE_ATTACK":1, "STRENGTH":2,
  "MELEE_DEFENCE":1). Item bonus tables have no STRENGTH field, so that weight scores nothing
  today — worth confirming the intended key (e.g. a melee-strength-bonus mapping).

## Read-only scope
No ship. Sibling 09:02 run's Build53/bundle-44 PASS note + SEEN are on the repo; nothing else
new in alex-inbox since. Feed still dark (newest screenshot 2026-09-30 17:44:06 EDT, ~63.4h).
Awaiting live: gen-45 hot-load, banking-pass behavior, checkpoints 12/50 -> 14+, proved meals,
Ithoi re-engagement.
