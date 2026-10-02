# Read-only review verdict: Below Ice Mountain Build 21 (patch-886)

**Reviewer:** Muse (read-only; Alex / OSRS BOT MAKER (2) own implementation + releases)
**Build:** 21 — commit `4f11a91d28` (shipped 2026-10-02 04:16:05Z), patch-886.zip, repo version.txt 885→886
**Published source:** `source-review/belowicemountain-build21/BelowIceMountainScript.java` (+1099 lines) + README
**Verdict: PASS WITH FINDINGS**

## Custody: AIR TIGHT
- `patches/patch-886.zip`: 277 entries; all net-rooted except `META-INF/` + root `version.txt` (expected).
- In-zip `version.txt` = 886 == repo `version.txt`; 885→886 sequential, **no version reuse**.
- `BUILD_NUMBER = 21` in published source AND in compiled `BelowIceMountainScript.class` (javap constant).
- 7/7 script classes byte-identical patch-zip ↔ `patches/belowicemountain-21.jar` (zip carries 3 extra Plugin/Config classes; hot-reload swaps script classes only — established pattern).
- Downloaded jar sha256 `1bd90a91…` == `patch-886.hot.json` sha256 `1bd90a91…`. Full match.

## Delta vs Build 20 (+47 diff lines, single-purpose)
Build 20 proved Wydin's shop open + inventory gains (raw beef 2132, flour 1933), closed the shop, then the walker toward Lumbridge returned unreachable from inside the shop (3012,3205) and held. Build 21 handles the exit explicitly:
1. New `Proof.DOOR_CHANGED` enum + `supplyDoorTile` field.
2. Gate (only after purchase state): near `SUPPLY_WYDIN` (6) + `RAW_BEEF>0` + (`BREAD>0` or `POT_FLOUR>0`).
3. Queries nearest name-contains-"Door" within 9 tiles on the client thread, reads actions on the client thread, logs `WYDIN_DOOR id/tile/actions` for diagnosis; clicks `Open` only if offered, captures the tile, issues `supply:open-wydin-door` with 8000ms proof window.
4. Proof `DOOR_CHANGED`: re-queries the door within 1 tile of the captured tile; proved iff actions expose `Close` OR (id changed AND no `Open`). A failed click leaves `Open` + same id → unproved → existing retry (2x) → HOLD. No unverified route success.
5. `containsAction` helper: null-safe, case-insensitive.

## Findings
- **[LOW] BIM21-1:** the `door.click("Open")` action runs on the script tick thread inside `issue()`'s `action.getAsBoolean()`, while the door query and action-reads are client-threaded. Same shape as Build 18's loot path (worked live), but the asymmetry is worth noting — if a "must be called on client thread" surfaces, this is the spot.
- **[INFO] BIM21-2:** `supplyDoorTile` is memory-only (null after hot reload). Harmless: the purchase-state gate is observed inventory, so the door logic re-fires and re-captures the tile.
- **[INFO] BIM21-3:** `withNameContains("Door")` is broad; nearest-to-player within 9 of the shop counter plus the `WYDIN_DOOR` log line makes a wrong pick diagnosable. Bounded.
- **Carried:** LOW BIM20-1 (reload-key mismatch `beefBought`/`flourBought` vs `supplyBeefBought`/`supplyFlourBought`, bounded by the 45-action supply budget); carried earlier items per review-log.

The fix is the right class of answer to the observed failure: an explicit, bounded, proof-gated doorway action instead of routing through a wall — consistent with the established door-handling rule (never walkTo a door tile). Bounded everywhere: 45-action supply budget, 2 unproved attempts → HOLD, 8s proof window.

## Live acceptance: PENDING
Requires `RUNTIME BUILD 21` marker + `WYDIN_DOOR id=…` diag lines on a fresh client. Screenshot feed dark since 2026-09-30 17:44 EDT (~30.6h); stream check is the only live visual source.
