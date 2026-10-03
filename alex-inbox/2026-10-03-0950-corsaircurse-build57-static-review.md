# Muse static review: Corsair Curse Build57 (patch-1109)

- Date: 2026-10-03 ~09:50 EDT (review-loop run)
- Ship: patch-1109 / Corsair Curse Build57, version.txt 1108 -> 1109,
  commit 63127082 ("Corsair Curse Build57 script update"), landed ~09:47 EDT
- Source: source-review/corsaircurse-build57/ (diffed against build56: 225 diff lines)
- Scope note: this loop is READ-ONLY on Alex/Mira-owned code. No edits, no re-ship.

## What changed (56 -> 57)

The script-side half of the shared-gear integration (bundle-47 was the provider half):

1. **Weapon scoring** (`weaponScore = max(astab,aslash,acrush) + 2*str`) read from the
   INSTALLED item stats each tick — no hardcoded item-stat table.
2. **`requiredWeaponScore`**: best score across a bounded catalog of F2P one-handed
   swords/scimitars (bronze/iron/steel, verified tier Attack requirements 1/1/5),
   each candidate verified via installed metadata: not members, GE-tradeable,
   equipable, weapon slot, not two-handed, dmagic>=0. Unverifiable catalog entries
   are skipped; empty catalog -> HOLD "Installed F2P weapon metadata unavailable"
   (fail-safe, no blind fight).
3. **`ithoiEquipmentReady()` now gates on `weaponScore >= requiredWeaponScore`** —
   the boss gate requires the best provable weapon, not just "some weapon".
4. **Boss retry budget**: `boss-retry-<account>.properties`, account-bound, validated
   0-3, persisted across hot reloads (follows the Pirate lesson: memory-only flags
   don't survive hot reload). `replanAfterBossExit` increments retreats, records the
   weapon that was in hand, resets combat state; >=2 retreats -> permanent HOLD until
   human review. Never silently fights Ithoi a third time.
5. **"Same failed weapon" gate**: if the equipped weapon is the one that already
   failed AND equipment is otherwise ready -> HOLD. A retry requires a CHANGED,
   better weapon — this is exactly what the bundle-47 gear pass + GE acquisition
   is for.
6. **Cove resupply route** (progress==52 in Cove, equipment not ready or
   food<20/healing<150): sail back with Captain Tock for the free return voyage
   (comment cites wiki transcript check 2026-10-03; the Cove bank is quest-locked —
   correct per game knowledge). Mainland prep raises thresholds to 20 food / 150
   healing (was 10/100) and requests `equippedAnyOf = bossWeapons` — wiring the
   bundle-47 gear pass + GE weapon purchase (budget 2000) into the pre-boss prep.
7. **Stage labels**: `REPLAN_BOSS_WEAPON_AND_SUPPLIES` is set but never matched —
   consistent with every other `REPLAN_*` label in this script; flow is flag-driven
   (`prepared=false` -> `prepare(f)` next tick). Not a defect.
8. **Dialogue fix**: progress==35 with Gnocci's 2-option final menu ("What is the
   mission Francois is doing?" / "Okay, thanks.") now explicitly picks the exit
   option — a stuck-dialogue prophylactic.

## Design observation (not a defect)

`bossRetreats` never resets to 0 — once 2 retreats are recorded, the bot holds
forever even if a much later setup would be better. This is a deliberate bounded
budget (human reviews before a third attempt), consistent with the no-infinite-retry
house rule. Noted so nobody is surprised.

## Concrete defects found

None blocking. The logic is internally consistent: gate math, budget persistence,
exit recovery, and the Tock-return route all reconcile.

## Verdict

PASS static. Live acceptance pending: stream overlay should show RUNTIME BUILD 57
hot-load marker, then the gear pass (GEAR_PASS_STARTED / BANK_AUDITED /
GEAR_PASS_PROVED), weapon purchase + restock verification (food 20+, healing 150+),
checkpoint growth beyond 12/50, and the Ithoi re-engagement gate with the
8-proven-meals rule. Judge from fresh runtime lines only.
