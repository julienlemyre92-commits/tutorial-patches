# Review verdict: Ernest the Chicken Build 8 (patch-610) — PASS

Reviewed 2026-09-30 22:58 EDT by Muse review-loop (commit 4396baeb "Build8: safe east-room approach and verified walker progress").

## Packaging — all PASS
- hot.json sha256 `394f54f7099e250ce2ee8a3aa624beb38967b5907aa0d671ddbc0ee0973b8eb8` EXACT-matches downloaded `patches/ernestthechicken-8.jar` (27518 bytes). No repeat of the Build 7 original-ship defect (hot.json matching nothing committed).
- `patches/patch-610.zip` (768361 bytes, 208 entries): root is `net/`, no junk paths (no repeat of the 341/342 too-deep zip fault); `version.txt` at root; manifest has `Main-Class: net.runelite.client.RuneLite` (built with `zip`, not `jar`).
- All 6 `ernestthechicken` Script classes byte-identical zip<->jar (the 3 Plugin/Config classes exist only in the zip overlay — expected, the jar is the hot-reload artifact with Script classes only).
- `BUILD_NUMBER = 8` via javap in BOTH zip and jar main class (no stale-marker repeat of patch-608).
- Fresh patch number 610; version.txt flipped 609->610 in the same commit; nothing overwritten.
- Feature is real code, not a re-ship: all 6 Script classes differ vs patch-609's (Plugin/Config byte-identical). New diag strings match the commit message: `[ErnestChicken] WALK label={} state={} from={} after={} target={}`, `[ErnestChicken] OPEN_EAST_ROOM_EXIT exactObject id={} tile={} actions={}`, `[ErnestChicken] EAST_ROOM_EXIT_ALREADY_OPEN id={} tile={} actions={}`.

## Feature review — no source published
- No `source-review/build8-ernest*` dir (newest ernest entry is `build6-impcatcher`), so no source-level defect hunt possible. Bytecode-level spot check shows the advertised feature (verified east-room exit + walker progress logging) present. No packaging defects to report; nothing on file from prior builds is contradicted.

## Live acceptance — PENDING
- Screenshot feed still dark since 2026-09-30 17:44:02 EDT (PIRATESTREASURE_DONE frames); zero ERNEST_* frames ever. Build 8 is not observable in-game yet.
- Watch for on next live frames: fresh `[ErnestChicken] RUNNING_BUILD=8` banner, `WALK`/`OPEN_EAST_ROOM_EXIT` diag lines, and (per the hot-reload rule) hot.json-epoch match if hot-loaded.
