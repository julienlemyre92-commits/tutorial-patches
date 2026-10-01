# Review verdict: Ernest the Chicken Build 1 (patch-602) — 2026-09-30 ~21:22 EDT (Muse, read-only)

Build: 1 (NEW plugin: Ernest the Chicken, quest 10) | patch-602 | commit 98f1b9ae ("Build1: add Ernest the Chicken quest plugin", 21:22:22 EDT) | version.txt=602

**Verdict: PASS — no blocking defects.** Packaging:

- `patches/patch-602.hot.json` sha256 `cde692cc…3963a1` EXACT-matches `patches/ernestthechicken-1.jar` (15,883 B, download-verified via git blobs API).
- Numbering 601→602 clean; no overwrite; repo `version.txt`=602 matches the internal `version.txt` inside the zip. (Note: my first version.txt read this run returned 601 — it raced commit 98f1b9ae by seconds; re-read confirmed 602. Lesson: re-read version.txt whenever a fresh commit appears mid-run.)
- `patch-602.zip`: 206 entries, 191 classes; class paths all under `net/`; the 3 non-net entries are `META-INF/`, `META-INF/MANIFEST.MF`, `version.txt` — identical layout to the PASS baseline patch-601 (199 entries). MANIFEST.MF carries the real `Main-Class: net.runelite.client.RuneLite` + microbot `Add-Opens`/`Add-Exports` lines (same as the official jar manifest), so the Check-Update.ps1 manifest-overwrite fault does not apply.
- New package `net.runelite.client.plugins.microbot.ernestthechicken` complete in the overlay zip: `ErnestTheChickenConfig`, `ErnestTheChickenPlugin` (+`$1` inner), `ErnestTheChickenScript` (+`Frame`, `LoginFrame`, `Pending` inners) = 7 classes. Imp Catcher classes (8) still present — overlay is additive, no classes dropped. No source published, so no line-level mechanism review possible.

**Live acceptance triggers (nothing observed yet):** fresh `RUNNING_BUILD=1` for ernestthechicken, new Ernest diag lines, or first `ERNESTTHECHICKEN_*`/`ERNEST_*` screenshot. Screenshot feed has been dark since 17:44 EDT (~218 min); standing game state per Alex ~18:34 EDT: PID 19476, gameState=LOGGED_IN, currentWorld=308, status frozen at HOLD class (tick-halt + 5s native_status expiry — do NOT read as stuck-at-login).

**Non-blocking notes:** the per-patch hot.json path is now `patches/patch-N.hot.json` (no root `hot.json`); commit-message/plugin-version convention holds. No defects to raise.
