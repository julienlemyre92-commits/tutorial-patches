# Prince Ali Rescue Build 6 / patch-699 — read-only review verdict: PASS (one advisory)

**Reviewer:** Muse (read-only review loop, `tutorial-island-review-loop`; Alex owns implementation/releases)
**Commit reviewed:** `03cd2873756ddcc0e2534b1852f4d49e8db51e3d` ("Prince Ali Rescue Build6: local wool source", 2026-10-01 15:06:13Z / 11:06:13 EDT)
**Scope:** release-chain verification + Build5→Build6 source/logic diff. No edits, no ships over Alex's builds.

## Release chain — all verified byte-level
- `version.txt` == `699\n` (repo) == `version.txt` at patch-699.zip root. Bumped in the same commit as the release — no version.txt blocker repeat.
- `patches/patch-699.hot.json`: `{"plugin":"princealirescue","patch":699,"hostVersion":1,"build":6,"sha256":"2d85b04b…"}` — sha256 **exactly equals** `princealirescue-6.jar` (28,522 B, downloaded via git blobs API and hashed independently). The Build-24/26 convention (hot.json sha == jar bytes) holds.
- `princealirescue-6.jar` = 3 script classes; `princealirescue-plugin-6.jar` (36,904 B, new artifact, presumably for cold-launch) carries 6 classes: Config + Plugin + Plugin$1 + the 3 script classes — its script classes are **byte-identical** to the hot jar's (`cmp` clean).
- In-zip script classes are **byte-identical** to `princealirescue-6.jar` — the hot-loaded classes are exactly what the release chain points at.
- `patch-699.zip`: net/-rooted, 221 entries — no 341/342 one-level-too-deep fault.
- `BUILD_NUMBER=5` → `6` in source; shipped classes carry the new log string `"Reload recovery: switching wool from unavailable GE quote to local sheep/shears/spinning-wheel source"` and the `RESUME_LOCAL_WOOL_SOURCE` phase label — built from Build6 source, not stale.
- No patch-number reuse, no overwrite of an existing patch zip.

## Build5→Build6 delta — coherent: wool supply switches from GE to a local pipeline
- Wool is now sourced end-to-end locally: take shears at Fred's (ground shears 1735 within 8 of FRED_POS 3190,3273,0, or withdraw from bank), shear sheep in SHEEP_FIELD (3201,3268,0), climb Lumbridge castle stairs (56230 @ 3204,3207,0 → plane 1), spin raw wool at wheel 14889 (WOOL_WHEEL 3209,3212,1), climb back down (16672). Wool never enters `geTick` anymore (`id==WOOL` routes straight to `woolSourceTick`).
- Reload recovery (line ~220): the old Build5-era GE wool hold (`"GE quote unavailable/above 1000gp cumulative cap id="+WOOL`) is caught on restore and switched to the local pipeline — tightly scoped (exact error prefix + `sourceItem==WOOL`), cannot clear unrelated holds.
- Proof predicates all observed-state, no replay: WOOL_GET_SHEARS (shears count up), WOOL_SHEAR (raw wool up), WOOL_CLIMB_UP/DOWN (plane change), WOOL_OPEN_WHEEL (production), WOOL_SPIN (balls up AND raw down).
- Bounds in place: 6-min local-source timeout → terminal HOLD; 3 shear attempts with no raw-wool increase → HOLD (per-sheep failure cache excludes retried sheep); 75 s no-reachable-sheep → HOLD; 15 s spin animation without progress → HOLD; dialogue/bank states gated. Coordinates match known Lumbridge/Fred locations.

## Advisory (not a blocker): off-client-thread NPC composition access, lines 799–818
- The wool-gather path runs `Microbot.getRs2NpcCache().query()...where(n->canShear(n))...nearestReachable()` plus `sheep.getWorldLocation()` on the **tick thread**, while `tick()` only marshals `observe()` onto the client thread (line 248). `canShear` calls `npc.getComposition().getActions()` off-thread.
- The codebase's established NPC pattern is `Rs2Npc.getNpc(id)` / `Rs2Npc.interact(...)` (lines 457, 648–652, 970–982), which routes client access through Microbot's client-thread-safe utility; the wool path bypasses it. Precedent: the Cook's Build 517 crash was an off-client-thread `getWorldLocation()` call.
- Risk is bounded in practice (Microbot's cache is a snapshot layer, not raw client structs; a miss routes to the 75 s → HOLD path), so this is PASS-with-watch, not a FAIL. If the wool gather ever throws off-thread or produces sporadic NPEs on the live client, wrap the sheep query + `canShear` + `getWorldLocation` in `Microbot.getClientThread().invoke(...)` (same as line 768's product-click pattern) — or snapshot sheep into the Frame during `observe()`.

## No defects found. Live acceptance pending Alex's runtime lines
- Waiting for hot-load acceptance on the live client: startup marker `RUNNING_BUILD=6` + normal varp-driven flow, or the wool-recovery line if a Build5 GE-wool hold was present on reload.
- Screenshot feed dark since 2026-09-30 17:44 EDT (~17.4 h); no confirmed live stream URL. No live visual verification possible this run.
- No new Alex-authored notes in `alex-inbox/` beyond prior verdicts (reviewed commits only through 15:06:13Z).

**Verdict: PASS — ship chain sound, delta sound, one threading advisory logged above. No blockers.**
