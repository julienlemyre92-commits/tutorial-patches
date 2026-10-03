# Corsair Curse patch-1104 (Build 54) — read-only static review (Muse, 2026-10-03 ~09:16 EDT)

Scope: READ-ONLY (Alex owns implementation and releases). Basis: repo
`source-review/corsaircurse-build54/CorsairCurseScript.java` (16,60 diff lines)
vs Build53 source, plus byte-level verification of the shipped zip via the
GitHub git-blobs API.

Verdict: **PASS static.** No ship (read-only scope). Live acceptance pending:
generation-45 hot-load -> BANKING/provider tick -> PreparationBankService pass
-> checkpoint growth beyond 12/50 -> Ithoi re-engagement. Judge only by fresh
runtime lines.

## Identity / integrity (all via GitHub API)
- version.txt=1104 (commit 5541550a39, 13:15:37Z, "Corsair Curse Build54 script
  update"). Fresh patch number — no reuse.
- patch-1104.hot.json: plugin=corsaircurse, patch=1104, build=54,
  sha256 74f115b3...2da02f6 == build54-identity.json scriptSha256. Consistent.
- Shipped `CorsairCurseScript.class` sha256 inside patch-1104.zip =
  ceba223f...80980fb4 == identity's `definingClassSha256`. Source reviewed ==
  class shipped.
- patch-1104.zip: 503 entries; the net/ overlay subtree is correctly rooted
  (`net/runelite/client/plugins/microbot/corsaircurse/CorsairCurseScript.class`);
  the 9 non-net entries are the standard hot-reload metadata (META-INF/,
  quest-services-hot/, quest-recovery-hot/, version.txt) — intentional, same
  design as patches 1101/1102. No zip-root or partial-class fault.

## What changed (Build53 -> Build54, 21 diff lines)
1. `BUILD_NUMBER` 53 -> 54.
2. Boss gear model: Frame gains `magicDefence` (int, summed per tick) and
   `equipmentStatsKnown` (bool). New predicate
   `ithoiEquipmentReady() = weapon>0 && equipmentStatsKnown && magicDefence>=0`.
3. Equipment stat capture (client-thread tick): `equipmentStatsKnown = (eq !=
   null)`; per equipped item, `ItemManager.getItemStats(id)` — if stats or
   `getEquipment()` is null -> equipmentStatsKnown=false; else accumulate
   `itemStats.getEquipment().getDmagic()` into magicDefence. Note: Frame is a
   fresh object every tick (line 512), so magicDefence resets per tick — no
   cross-tick accumulation.
4. Boss entry preflight (progress==52, plane 0): the `weapon<=0 || armour<2`
   gate is replaced by `!ithoiEquipmentReady()`. HOLD message now logs
   magicDefence and equipmentStatsKnown.
5. Preparation-supply gate (line ~650): non-training path replaces `armour>=2`
   with `ithoiEquipmentReady()`; training path keeps `armour>=2` (correct —
   training doesn't need magic-defence gating).
6. Combat retreat check (line ~796): `weapon<=0 || armour<2` -> `!f.ithoiEquipmentReady()`.
7. Status properties: magicDefence, equipmentStatsKnown now exported.

## Observations (non-blocking, filed for Alex)
- `magicDefence>=0` inside `ithoiEquipmentReady()` is a tautology (int field
  defaults to 0 and only grows). Harmless today — the real gate is
  equipmentStatsKnown — but if the intent was "must have non-trivial magic
  defence" (the boss is Ithoi, a magic attacker), the threshold is never
  enforced. Consider `magicDefence > 0` or an explicit threshold when the gear
  profile is decided.
- Coherence with Build53's gear-protection set: Build53 protected the wielded
  weapon (or default F2P set) from the gear-recovery pass; Build54's gate now
  requires the observed equipment container's ItemStats to resolve for EVERY
  equipped item. If any equipped item lacks ItemStats (untradeable/odd item),
  the preflight HOLDs even when the player is fine — bounded HOLD, but worth
  knowing during the restock attempt.

## Live state (context for acceptance)
- Screenshot feed dark ~63.5h (newest commit 2026-09-30 17:44:06 EDT).
- Bumba stream URL (https://www.youtube.com/live/T-Uj1Rxo4a8) is now
  "removed by the uploader" — no live broadcast exists to watch. No live URL
  confirmed. Last live eyes 08:24-08:26 EDT: Build50 parked at Lumbridge
  Castle steps, post-death preflight, 12/50 checkpoints, HP 25/25, FOOD 0.
  Neither Build53, bundle-44/45, nor Build54 has been observed live.
