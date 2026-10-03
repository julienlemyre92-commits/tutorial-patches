# Muse static review: Corsair Curse provider-bundle-47 (patch-1108)

- Date: 2026-10-03 ~09:47 EDT (review-loop run)
- Ship: patch-1108 / provider-bundle-47.jar (generation 47), version.txt 1107 -> 1108,
  commit 30cc6919a233 ("Bank route chooses policy-verified F2P walk"), landed 09:45:21 EDT
- Source: source-review/recovery-1108/ (10 new files; reviewed the 3 load-bearing ones:
  GearUpgradePlanner.java, GearRecoveryController.java, PreparationBankService.java)
- Scope note: this loop is READ-ONLY on Alex/Mira-owned code. No edits, no re-ship.

## What it is

The shared-gear integration Alex described at 09:43 ("continuing the shared gear
integration, then I'll hot-load it and verify the weapon purchase and restock"):
- `GearUpgradePlanner`: pure, bank-stock-only equip decisions — best verified upgrade
  per slot by bonus-weight gain, one item at a time, zero spend. Members items excluded
  on F2P; two-handed excluded; skill requirements checked against observed levels.
- `GearRecoveryController`: tick wrapper — persists every bank-UI intent BEFORE dispatch,
  requires proof on the next fresh frame, NEVER replays an unproved action, account-bound
  checkpoint with schema + goal fingerprint. Follows the house single-shot discipline.
- `PreparationBankService`: host service — supply prep first, then the gear pass
  (restricted UI: may only touch items in `equippedAnyOf` / already equipped), then gear
  acquisition via GE child if the bank lacks the requested equipment (budget-capped by
  `maximumSpend`, child spend tracked and checkpointed).
- Hot-load manifest: patch-1108.provider-hot.json (gen 47, artifactSha256
  f80f0661d1e632..., parent ABI chain from bundle-46 generation intact).

## Concrete defects found

1. MINOR (non-blocking): `PreparationBankService.tick()` — in the
   `gearAcqNavigationPending` branch, `gearAcqUi.observe()` is dereferenced without a
   null check (`arrived.nearExchange()`). Every other snapshot read in this file guards
   null; this branch does not, and there is no try/catch around it. If the GE adapter
   ever returns null, this NPEs out of the synchronized tick. Suggest a null guard
   matching the pattern used for `ui.observe()`.
2. None found in `GearUpgradePlanner` / `GearRecoveryController`: reconcile logic,
   deadline handling, pending-action lifecycle, account binding, and checkpoint
   serialization all read correctly. `restrictGearUi` correctly filters verifiedItems
   to acceptable+equipped only.

## Not reviewed this run

GearAcquisitionController / adapters / DefaultProviderBundleFactory — skimming only.
The F2P-walk policy change named in the commit message is not surfaced in these three
files; it presumably lives in the navigation child consumed by `delegateGearGeTravel`
(which passes `goal.f2pOnly()` through to the NavigationGoal — correct).

## Verdict

PASS static, with the one minor null-guard note above. Live acceptance pending: watch
for the bundle-47 hot-load marker on the stream, then `GEAR_PASS_STARTED` /
`BANK_AUDITED` / `GEAR_PASS_PROVED` lines, weapon purchase + restock verification,
checkpoint growth beyond 12/50, and the Ithoi re-engagement gate. Acceptance is judged
from fresh runtime lines only — never the banner.
