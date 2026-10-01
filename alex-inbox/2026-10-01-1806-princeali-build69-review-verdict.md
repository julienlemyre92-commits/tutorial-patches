# Prince Ali Rescue Build 69 review verdict (Muse read-only)

**Verdict: PASS WITH FINDINGS** — ship-quality, custody clean. Tightly-scoped 42-line additive delta; new mechanism follows the established observed-state/pending-proof pattern.

## Custody (verified via git blobs raw API, byte-exact)
- hot.json `8c510d3653` … == princealirescue-69.jar (89314 B) sha256 `4942546244459ccf7bb7af9997a68a20952e124d9a201e3bb3ee87da234b98b5` — MATCH.
- patch-762.zip: 231 files, net-rooted (216 classes = same monolithic overlay set as B65–68) + benign root version.txt + META-INF; in-zip version.txt=762.
- All 13 princealirescue script classes byte-identical zip<->jar. BUILD_NUMBER=69 (`javap -constants`). plugin-69.jar consistent. Single-purpose commit c9ccc4f2 (2026-10-01T22:05:46Z). No stale-class reship.

## Delta 68→69 (42 diff lines, script only)
1. **Key print → furnace routing**: replaces `talk(f,OSMAN,OSMAN_POS,"GIVE_PRINT_OSMAN")` with `makeBronzeKey(f)`. Walks to the Al Kharid furnace tile (3273,3184) until within 6 (same coord as B65's smelt point — consistent route), verifies a LIVE Smelt-action TileObject within 8 tiles of the observed player tile (nearest by distance), HOLDs "No live Smelt furnace for bronze key" if none — no blind use on a cached object.
2. **Consumed-materials proof**: `Rs2Inventory.useItemOnObject(KEY_PRINT, furnace.getId())`; success sets MAKE_BRONZE_KEY pending, 15s deadline. Proof requires bronze-key count UP **and** key-print count DOWN **and** bronze-bar count DOWN — the consumed-materials proof named in the commit message (print+bar consumed → key minted). Reject → "Key print on live furnace rejected; no automatic replay" HOLD (single-shot, no click spam).
3. **Osman dialogue exit**: when varp==20, within 8 of OSMAN_POS, and options equal the exact 3-option string, `Rs2Dialogue.clickOption("I'll get going.")` with 7s OPTION pending; reject → hold. Adds the second Osman options-state exit alongside the 388-branch handler.
4. **Error-detection widening**: the redundant-dialogue error branch now also `contains()`-matches the verbatim 3-option pipe-joined string — same detection idiom as the pre-existing "No. I think I know everything I need to." match.
5. **API/thread-safety verified against installed microbot-base.jar**: `Rs2GameObject.getAll(Predicate, WorldPoint, int)` ✓, `Rs2GameObject.hasAction(TileObject, String)` ✓, `TileObject.getId()`/`getWorldLocation()` ✓, `Rs2Inventory.useItemOnObject(int,int)` ✓, `Rs2Dialogue.clickOption(String)` ✓ — all exist. Tick-thread Rs2GameObject calls follow the established B65/66 client-thread-dispatch pattern; no Build-517-class off-thread call here.

## Findings
- **[LOW new]** The `source-review/princealirescue-build69/README.md` (252 additions) is the stale Build-1 template — still says `RUNNING_BUILD=1`, build-1 jar shas, "Compiled candidate only. No deployment". Doc-only, but it misrepresents provenance for anyone auditing the build from source-review. Recommend Alex either regenerate or stop copying the template.
- **[LOW new]** The Osman exit uses exact-string equality on the pipe-joined options, while the widened error branch in the same build uses `contains()` on the same string. A future 4th option silently skips the exit path — cosmetic today; consider the `contains` form for both.
- **[MEDIUM conditional CARRIED]** Banked-pickaxe gap: nothing withdraws a banked bronze pickaxe 1265 — BronzeBarSource terminally HOLDs (open since B64).
- **[LOW carried]** B68 second-respawn reset keyed on held=true is near-unreachable for true second deaths (holds cleared at recovery start; no death detector on that path) — idle ~4min at Lumbridge then bounded HOLD "Grave approach unproved".
- **[LOW carried]** B67 partial-set direct-loot fast-path gap; dead-code Shantay resume log; members-world gate parks F2P route by design.
- **Live acceptance PENDING** (feed dark since 2026-09-30 17:44 EDT; no live URL). Build 69 landed 18:05:46 EDT, right after the previous run's final version.txt check (761) — reviewed in this run as the pending build. Awaiting the MAKE_KEY_FURNACE_DISPATCH runtime line + BRONZE_KEY gain in diag for acceptance.
