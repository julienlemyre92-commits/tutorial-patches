# Ernest Build 4 (patch-605) review verdict — 2026-09-30 22:07 EDT (Muse, read-only review)

**VERDICT: PASS** — no blocking defects.

Integrity:
- patch-605.hot.json sha256 f523d86d...48485 EXACT-matches ernestthechicken-4.jar (blob download, blobs API).
- ErnestTheChickenScript.class byte-IDENTICAL between jar and patch-605.zip (the hot-loaded unit). Jar holds the script only; zip's Plugin/Config bytes differ because the zip bundles the hot-reload host adapter (ErnestTheChickenHot: HOST_READY/RELOAD_BEGIN/RELOAD_APPLIED strings) — expected, not a divergence of the build-4 logic.
- patch-605.zip: 207 entries, net/ root rule holds (non-net/ entries = META-INF/MANIFEST.MF + version.txt only). Manifest byte-IDENTICAL to patch-604's (Build 3 reviewed PASS) — no new manifest risk.
- repo version.txt = 605 = zip's internal version.txt.

Code (strings review, commit "Build4: interact with exact manor door object"):
- New states: AT_MANOR_ENTRANCE -> CROSS_MANOR_DOOR -> INSIDE_MANOR; OPEN_MANOR_DOOR logs exactObject id/tile/actions; recovery paths RESCAN_MANOR_DOOR / REPOSITION_MANOR_DOOR; rejection diagnostics ("Manor door exact-object Open rejected id=", "Manor door ID found at unexpected tile id=").
- Coherent with commit message; no hardcoded coordinates visible in strings (tile logged at runtime), no blocking defects found.

Still pending live acceptance (feed dark since 17:44:02 EDT): fresh RUNNING_BUILD for ernestthechicken, new Ernest diag lines, or first ERNEST_* screenshot.
