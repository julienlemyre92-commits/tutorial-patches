# Muse static review: Corsair Curse Build53 (patch-1102) + provider-bundle-44 (patch-1101)

Date: 2026-10-03 ~09:00 EDT. Scope: READ-ONLY review (Alex/Mira own implementation and releases).
Basis: repo `source-review/corsaircurse-build53/` + `source-review/recovery-1101/`, plus
byte-level verification of the shipped zips. Feed dark ~63.2h (newest screenshot commit
2026-09-30 17:44:06 EDT); last live eyes 08:24-08:26 EDT showed Build50 parked at Lumbridge
Castle steps in designed post-death preflight (12/50 checkpoints, HP 25/25, FOOD 0).
Neither Build53 nor bundle-44 has been observed live. 08:49 stream flag (hot-load acceptance)
still outstanding — now covers 1101/1102.

## Verdict: static PASS (no ship — read-only scope)

## Commits reviewed
- `4955f01` 08:57:53 EDT "Bank route chooses policy-verified F2P walk": patch-1101.zip,
  patch-1101.provider-hot.json (generation 44), provider-bundle-44.jar (312,536 bytes),
  source-review/recovery-1101/ (4 files), version.txt -> 1101.
- `a080ffad` 08:58:49 EDT "Corsair Curse Build53 script update": patch-1102.zip,
  patch-1102.hot.json (plugin=corsaircurse, patch=1102, build=53), corsaircurse-53.jar,
  source-review/corsaircurse-build53/, version.txt -> 1102 (current).

## Integrity checks (all pass)
- Shipped `CorsairCurseScript.class` sha256 in patch-1102.zip = `483186c5...edb4d5fea1`,
  exactly the identity json's `definingClassSha256`. Source reviewed == class shipped.
- patch-1102.hot.json `sha256` = `9ebc2544...2da02f6` = identity's `scriptSha256`. Consistent.
- Both patch zips: `net/` root prefix correct, full overlay entry lists (identical file lists
  between 1101/1102; only class bytes differ). No zip-root or partial-class faults.
- provider-bundle-44.jar contains all 8 GearRecovery classes (Controller, Catalog,
  MicrobotAdapter + inners). Gear code rides the provider jar, NOT the patch zips
  (0 GearRecovery classes in either zip) — overlay ordering: 1101 provider hot first,
  1102 script hot overlays; provider classes persist. Matches pipeline design.

## Build53 script change (12-line diff vs Build52 — minimal, coherent)
- `BUILD_NUMBER` 52 -> 53.
- Gear-goal protected set: was `Set.of()` (empty); now `f.weapon>0 ? Set.of(f.weapon) :
  Set.of(BRONZE..STEEL_SWORD, BRONZE..STEEL_SCIMITAR, BRONZE..STEEL_PICKAXE)`.
  Rationale: the new bundle-44 gear-recovery pass must not strip the player's weapon —
  protect the wielded weapon, else a default F2P bronze-steel melee set. Pairs correctly
  with the bundle-44 pass. No other script logic touched (boss tagging, death-reset flag,
  8-meals gate, stair proof all byte-identical).

## provider-bundle-44 change (DefaultProviderBundleFactory + gear package)
- Wires `NavigationMicrobotDriver(new VerifiedRoutePolicy(), guard)` — the "policy-verified
  F2P walk" from the commit message. Rest of the bundle (funding/food/banking/training)
  unchanged.
- `GearRecoveryController`: serialized, checkpoint-persisted bank/equip pass. Sound design:
  proof-based single-flight UI intents (OPEN proved by bankOpen+bankAudited, CLOSE, ITEM_MODE;
  no replays), unproved UI action >8s -> terminal HOLD, deadline-bounded, atomic checkpoint
  writes (tmp+move) with account-key + goal-fingerprint validation, snapshot freshness <=3s,
  same-account/noncombat gating, sticky error -> HOLD (never spins).
- `GearRecoveryCatalog`: F2P-only (members excluded), two-handers excluded, unworn gear must
  match a RULE (bronze/iron/steel sword|scimitar|pickaxe, med/full helm, plate/chain body,
  platelegs) incl. ATTACK/DEFENCE level gates (steel=5); worn gear bypasses RULES on proved
  equipability but still needs known WEAPON/HEAD/BODY/LEGS slot. Unverified bank gear never
  selected. `equipOne` re-checks Wield/Wear action on the client thread.
- `GearRecoveryMicrobotAdapter.observe()`: client-thread snapshot reusing installed account
  identity + bank-container + same-floor checks; equipment container + real skill levels.

## Minor notes (non-blocking)
1. Build53 `protectedItems`: if `f.weapon` is a members item it is protected-but-never-equipped
   (catalog excludes it) — harmless retention, not a defect.
2. Controller `hold()` is sticky: after a terminal HOLD the checkpoint persists `uiPending`+`uiAt`;
   on restart a >8s-old pending intent trips the unproved-UI hold on the first tick. Acceptable
   (fails loud, not looping), worth knowing for the Ithoi restock path.
3. `GearUpgradePlanner` (referenced by controller/adapter) ships from an earlier bundle via
   overlay — not re-reviewed here; bundle-43 review (08:37) stands.

## Open / pending (live)
- Live acceptance of provider-bundle-44 + Build53 hot-load: pending fresh runtime lines.
  Judge by new diag lines, never the banner.
- Feed dark ~63.2h; stream remains the only live evidence.
- Prior carry-forward items unchanged: status-file write lock (stale panel-data risk), Build 92
  PID-bound checkpoint note, panel/checkpoint disagreements, Knight's Sword unresolved (not counted).
