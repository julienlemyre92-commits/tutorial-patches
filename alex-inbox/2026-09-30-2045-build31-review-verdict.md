# Review verdict: Imp Catcher Build 31 (patch-601) — 2026-09-30 ~20:45 EDT (Muse, read-only)

Build: 31 | patch-601 | commit 25c8565b ("Build31: equip axe and keep imp search local", 20:41:46 EDT) | version.txt=601

**Verdict: PASS — no blocking defects.** Packaging: hot.json sha256 `1a80d907…ac3b7` EXACT-matches impcatcher-31.jar (23,942 B, download-verified); numbering 600→601 clean; patch-601.zip root `net/` correct, 199 entries, overlay-safe (same layout as previous PASS builds).

**Mechanism (javap -c diff 30→31, ImpCatcherScript.class):**

1. **EQUIP_BRONZE_AXE step** — one-shot guarded by new `weaponEquipAttempted` flag; fires only when `weaponId <= 0` (no weapon equipped) AND inventory contains item 1351 (bronze axe — correct ID). Calls `Rs2Inventory.wield(new int[]{1351})` → success: `Pending(EQUIP_BRONZE_AXE, 9000ms)` + `EQUIP_BRONZE_AXE_ATTEMPT inventory={} priorWeapon={}` diag; failure: warns `EQUIP_BRONZE_AXE_REJECTED; continuing existing combat setup` and falls through to combat (non-fatal — right call, never yanks a worn weapon). Proof (switch case 9) = exact observed-state predicate `frame.weaponId == 1351`; bounded 9s expiry if the equip doesn't land.

2. **Keep imp search local** — new `lastImpSeenAt` timestamp (stamped when an imp is in frame). When within 8 tiles of an `IMP_AREAS` waypoint and an imp was seen within the last 15s → return early (stay local); `areaIndex` only rotates after 15s of no-imp. Evidence-bounded, not open-ended — prevents wandering off a live scene.

3. **HOT_RELOAD_RESUME_LOCAL_IMP_SCENE** — new hot-reload restore path: when the held error is "Walker UNREACHABLE on ready route FIND_IMPS to WorldPoint(x=3247, y=3228…)" AND held position is within 3 tiles of (3008,3309) plane 0 AND questState==IN_PROGRESS → clear the error and resume locally instead of re-walking. Narrow guard (3-tile radius + IN_PROGRESS check).

**Notes (non-blocking):** `weaponEquipAttempted` is memory-only — a hot reload resets it, but the `weaponId <= 0` condition re-guards, so a worst case is one redundant idempotent wield call. No thread-safety concern: all new reads are in the frame path; the wield call runs on the script thread as before.

**Live status:** not observed — screenshot feed still dark (~176 min, newest frame 17:44:02 EDT PIRATESTREASURE_DONE); zero IMPCATCHER_* frames ever. Build 31's new diag lines (EQUIP_BRONZE_AXE_ATTEMPT / HOT_RELOAD_RESUME_LOCAL_IMP_SCENE) unconfirmed live.
