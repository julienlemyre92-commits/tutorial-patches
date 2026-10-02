# Read-only review: Below Ice Mountain Build 44 (patch-909) — PASS WITH FINDINGS

Reviewed 2026-10-02 ~01:52 EDT by Muse (read-only; Alex / OSRS BOT MAKER (2) own implementation and releases).

## Custody — AIR TIGHT
- Commit `f4b1ad39dc` "Below Ice Mountain Build44 chicken LOS and gate probe" (2026-10-02T05:49:41Z); `version.txt` = 909 (908 → 909 sequential, no reuse).
- `patch-909.zip`: 284 entries, `net/` root (+`META-INF/MANIFEST.MF`, pre-existing B40-era pattern, harmless on hot-reload path); entry name set byte-identical to `patch-908.zip`; in-zip `version.txt` = 909 == repo.
- Only 8 class files differ 908 → 909: `BelowIceMountainScript` + `$1` + 6 `QuestGeBuyer` nested classes (all expected — the buyer classes are nested in the script).
- `runtimeBuild()` in the shipped class = `bipush 44` (javap-verified); `BUILD_NUMBER = 44` in published source; README44 matches the source diff exactly.
- `belowicemountain-44.jar` sha256 `698cbedd77b09345f1167224688e83a2385c5766643b0250a186338869e42948` == `patch-909.hot.json` — FULL MATCH.

## Source diff 43 → 44 (33 lines, matches README44 exactly)
1. `BUILD_NUMBER` 43 → 44.
2. Chicken target selection now prefers live chickens with `hasLineOfSight()`; falls back to nearest chicken only when none have LOS.
3. The existing near-target "Chicken behind gate/wall" HOLD path now dumps scene evidence first: all chicken indices/tiles/LOS values plus nearby pen gate tiles (id 1559 within 20 of CHICKEN_FARM 3238,3298,0), logged as a `CHICKEN_FENCE_SCENE` warn line and appended to the hold string — probe data for the future route correction. No gate interaction yet; no repeated attack (verified in source — no new interact/click calls).
4. `targetChicken` final-capture refactor for the attack lambda (loop-variable hygiene).

## API safety (verified against installed microbot-base.jar)
- `hasLineOfSight()` and `getIndex()` exist on `api.npc.models.Rs2NpcModel`; `getWorldLocation()` inherited from `api.actor.Rs2ActorModel`.
- The `.withId(1559).within(CHICKEN_FARM,20).toListOnClientThread()` query reuses the pattern already proven in this script (e.g. line 535 boat query). No `NoSuchMethodError` risk.

## Findings
- INFO BIM44-1: LOS preference applies to target selection only; the candidate list remains B43's distance-sorted set — a nearer LOS=false chicken is skipped for attack but still appears in the scene dump. As designed (probe).
- INFO BIM44-2: when ALL chickens lack LOS, the fallback still attacks the nearest one, which lands on the existing behind-gate HOLD with the new scene dump. Deliberate probe path, bounded.
- INFO BIM44-3: the fence-scene HOLD remains terminal (no gate interaction yet, README states this); each hot reload re-runs the probe once. Bounded.
- NOTE: README44's stated live motivation ("Build43 gained HP XP1518→1529 then correctly HOLDed adjacent to a Chicken with LOS=false across the fence") is bot-maker-reported and NOT independently verified — screenshot feed dark since 2026-09-30 17:44 EDT (~32.1h); stream is the only live source.

## Carried open items (unchanged)
- BIM43-1 (NPC-index avoid reuse note), BIM43-2 (corpse-avoid terminal-HOLD edge), BIM43-3 (reload-persisted streak); BIM42-1 (sticky retreat latch, deliberate), BIM42-2 (2000-action budget note); BIM41-1; BIM40-1 (status-write FileSystemException, logged-only), BIM40-2/40-3/40-4; bankCoins discrepancy (script debug 3023 vs bot-maker panel "1,023"); guardian controller unimplemented; BIM38-1/38-2/38-3.

## Live acceptance (PENDING)
Next routine stream window 02:00 EDT: watch for `RUNTIME BUILD: 44 / confirmed` + NEW `CHICKEN_FENCE_SCENE` warn / `RESCAN_ALTERNATE_CHICKEN` stage text + per-kill XP proofs. Acceptance only from new runtime lines, never the banner alone.
