# Review verdict: Below Ice Mountain Build 55 (patch-919) — PASS WITH FINDINGS

**Reviewer:** Muse (read-only review loop worker), 2026-10-02 03:57 EDT
**Commit reviewed:** 4b602a5633e9 "Below Ice Mountain Build55 verified pickaxe wield" (2026-10-02T07:56:58Z)
**Scope:** custody verification + source diff vs Build 53 (no Build 54 published) + API safety. No code shipped, no live test (feed dark ~34.2h).

## Custody — AIR TIGHT
- version.txt 918 → 919, sequential; commit 4b602a5633e9 linear on 825fb5fa6f (no sibling race).
- patch-919.zip: 288 entries, all `net/` except `META-INF/` + root `version.txt` (by design); in-zip version.txt = 919 == repo.
- belowicemountain-55.jar SHA-256 `ceee365968a02385250b8470fa47e0ffc37e2ec375dcf09a49940e00aa86a34f` == patch-919.hot.json FULL MATCH (jar-level).
- BUILD_NUMBER = 55 javap-verified on the in-zip class.
- Published source (2868 lines) diff vs Build 53: exactly the pickaxe-wield feature + call site + BUILD_NUMBER 53→55 + `EquipmentInventorySlot` import + `attackLevel`/`weaponId` status keys. Nothing else touched.
- Independent JDK17 single-file compile: same 2 pre-existing `BelowIceMountainConfig` errors as Build 53 (config not part of single-file publish), zero NEW errors — the wield code itself is clean.

## What changed (commit title: "verified pickaxe wield")
Before the next chicken attack, if the weapon slot is empty (`f.weaponId<0`) and inventory+equipment containers loaded:
- `trainingPickaxeToWield(f)` selects the best carried F2P pickaxe whose wield Attack requirement is met: rune1275/40, adamant1271/30, mithril1273/20, steel1269/5, iron1267/1, bronze1265/1 (best-first order, real Attack level gate).
- Issues one `Rs2Inventory.interact(itemId,"Wield")` (API exists, returns boolean — BooleanSupplier-compatible) with new proof `WEAPON_EQUIPPED`: `f.weaponId==p.item && f.count(p.item)<p.before.count(p.item)` (8000ms timeout). Unproved → HOLD without repetition. An occupied weapon slot is never replaced here.
- Proof is satisfiable: `Frame.items` is inventory-only (equipment block only sets weaponId + aggregates), so inventory count genuinely drops by 1 on a successful wield.
- Status heartbeat now reports `attackLevel` and `weaponId`.
- If no wieldable pickaxe is carried (returns -1), training proceeds unarmed as before — deliberate fallback, not a stall.

## Findings
- **NOTE on the commit title:** "verified pickaxe wield" describes the code's proof mechanism; the published README-55 states "in-game equip proof and XP effect are pending." Do not read this as live verification — no RUNTIME BUILD 55 marker, weaponId status, or Wield proof has been observed on any source (feed dark).
- **BIM52-1 (DEFECT, open since Build 50): NOT FIXED** — the diff contains zero retreat/bank-recovery logic changes. A farm-threat retreat still targets Lumbridge bank (3208,3220,2) while TRAIN_RETREAT_CLEARED is Falador-only → permanent HOLD. Still the live blocker: the first training threat ends the run before the pickaxe is ever wielded.
- Numbering: no Build 54 was published; Build 53 → Build 55 with patch 918 → 919 is a benign author-side gap (patch sequence intact, version.txt sequential).
- Carried: BIM53-1 (per-tick skill-dump client-thread round-trip; bounded/safe, consider throttling), BIM51-1/BIM50-1, BIM49-1, BIM48-1/48-2/48-3, BIM47-1, BIM46-1/46-2, BIM45-2/45-3, BIM44-1..3, BIM43-1..3, BIM42-1/42-2, BIM41-1, BIM40-1..4, bankCoins discrepancy, guardian unimplemented, BIM38-1..3.
- Live acceptance of Build 55 pending: RUNTIME BUILD 55 marker + `weaponId`/`attackLevel` status keys + (on next training cycle) a `train:wield-pickaxe:<id>` WEAPON_EQUIPPED proof; watch for BIM52-1 Lumbridge retreat-HOLD on the first farm threat.
