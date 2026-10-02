# Review verdict: Below Ice Mountain Build 91 — empty-equipment death-recovery bridge (patch-953)

- verdict: PASS (INFO findings only)
- scope: read-only review of commit `851f9482` (version.txt 952 -> 953; patches/patch-953.zip, patches/belowicemountain-91.jar, patches/patch-953.hot.json, source-review/belowicemountain-build91/{BelowIceMountainScript.java,README.md}); Alex owns implementation/releases, no ship from this loop.
- published by: Muse review loop, 2026-10-02 ~07:22 EDT
- custody (independently verified this run, not taken on trust):
  - zip root version.txt = `953`, matches repo version.txt and patch name (no reuse)
  - zip root layout: `net/` classes + META-INF + root version.txt (313 entries; matches 951/952 convention)
  - belowicemountain-91.jar sha256 = hot.json sha256 (`0fd4c6ef…2571f8f7`) — hot-reload fingerprint verified
  - jar carries the new code: warn string DEATH_BRIDGE_EQUIPMENT_CONTAINER_ABSENT present in compiled BelowIceMountainScript.class (25 entries, script-only)
  - BUILD_NUMBER = 91 in source and matches patch name/hot.json build

## What changed (diff vs source-review/belowicemountain-build90)
Three-line change in ensureDeathRecoveryRegistered (~line 2659): the registration gate changed from
  if (!f.inventoryLoaded || !f.equipmentLoaded)
to
  if (!f.inventoryLoaded || (!f.equipmentLoaded && f.weaponId>0))
plus a log.warn when inventory is loaded but the equipment container is absent (worn gear cannot be included until the container loads).

## Review
- Correct intent: on a fresh account the equipment container stays unloaded with weaponId==0; the old gate blocked death-recovery registration entirely, so an empty-equipment character silently had no bridge registered. B91 registers the inventory-only manifest for that case while keeping the wait for loaded equipment when the player is actually wearing gear (weaponId>0). Fail-closed structure preserved.
- INFO BIM91-1: if the equipment container never loads, worn gear is excluded from the manifest beyond the one warn line — the warn is the only record; consider re-registering (with equipment included) once the container loads so the manifest upgrades from inventory-only.
- INFO BIM91-2: README unchanged from Build 1 handoff (4312->4315 line source change not described); death-recovery handoff README gap carries.
- Residual from 0716 verdict (by design, unchanged): office reclaim path unreachable by refusal (fee quote empty) — safe; full inventory at reclaim still dead-ends into HOLD.

Live acceptance pending: screenshot feed dark ~37.5h; no new runtime lines yet.
