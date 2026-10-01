# Muse read-only review verdict: Prince Ali Rescue Build 34 / patch-727

- Build 34 (patch-727, commit f39da959 2026-10-01T18:05:38Z; version.txt 726->727).
  Commit message reused verbatim 9th time ("keeps native reconnect active under
  exact quest HOLD") -- describes nothing in this diff. Harmless; misleading history.
- Scope: read-only review. Alex owns implementation/releases; nothing shipped by Muse.

## Chain of custody: PASS

- hot.json sha256 c98c95cd4a9fb4454c1f1144d5b5dc4c0c6d2f6a06c2c5f60dfe97338a4240af
  == downloaded patches/princealirescue-34.jar byte-for-byte.
- Script classes byte-identical across patch-727.zip / princealirescue-34.jar /
  princealirescue-plugin-34.jar (hot jar carries the 3 script classes only -- consistent).
- patch-727.zip: 221 entries, net/-rooted; META-INF/MANIFEST.MF has genuine
  Main-Class: net.runelite.client.RuneLite; in-zip version.txt=727 == repo version.txt.
- RUNNING_BUILD=34 verified in shipped class (javap: banner bipush 34 + runtimeBuild()=34).
- Plugin/Config sources byte-identical b33->b34; Script 124497->126908 bytes.
  New log strings (RECOVERED_EXACT_NO_TREE_WITHIN_8_HOLD,
  "No live regular Tree with Chop down and clear line of sight within 16 tiles",
  RESCAN_FRED_TREES_WITH_LINE_OF_SIGHT) present in shipped class constant pool --
  shipped class matches published b34 source.

## Change b33->b34 (source-verified, diff = 47 lines)

1. Tree sourcing replaces `findReachableObject("Tree",true,8,...)` with new
   `nearestVisibleNormalTree(f,16)`: exact name "Tree", action "Chop down",
   `isReachable(o)` AND `hasLineOfSight(o)`, picks nearest, and PERMANENTLY excludes
   id=1276 at (3265,3215,0) -- the exact tile from b33's observed
   "Live reachable tree Chop down rejected" HOLD. Directly answers b33 verdict O1.
2. No-tree hold now dumps candidate diagnostics (id@loc chop= los=, up to 12) --
   good observability for the next stuck state.
3. New persisted one-shot flag `ashesFredTreeRescanRecovered` (checkpointed).
4. New one-shot `recoverObservedNoTreeAtFred` matching the exact b33 hold prefix
   "No live reachable regular Tree with Chop down near Fred farm;"; gates:
   ashesTreeRerouteRecovered already fired, ASHES/sourceGoal=1,
   geStage=ASHES_GET_NORMAL_LOG, sourceAttempts==0, LOGGED_IN, varp==20, plane 0,
   within 2 of FRED_POS, no logs, tinderbox + woodcutting axe. Clears hold,
   phase=RESCAN_FRED_TREES_WITH_LINE_OF_SIGHT, fresh 6-min window (b31 pattern),
   logs RECOVERED_EXACT_NO_TREE_WITHIN_8_HOLD.

## Observations for Alex

- O1 (reachability): the new recover matches b33's OLD hold string, which b34's own
  no-tree branch never emits (b34 emits "No live regular Tree with Chop down and
  clear line of sight within 16 tiles of Fred farm;..."). error/phase are NOT in the
  checkpoint (verified: checkpoint persists flags only), so after a hot reload the
  in-memory error is whatever the host preserves -- the recover fires only if the
  b33 string survives reload. Harmless either way: the new 16-tile LoS scan runs
  unconditionally on the next sourcing tick, so behavior converges to the intended
  rescan with or without the recover. The recover is belt-and-suspenders for the
  exact observed stuck state.
- O2: phase="RESCAN_FRED_TREES_WITH_LINE_OF_SIGHT" is write-only (no reader in
  source; same pattern as b29's RETRY_TIMED_OUT_TREE_APPROACH). Diag marker only;
  the rescan happens via the normal sourcing path. Harmless.
- O3: the id1276@(3265,3215,0) exclusion is permanent and quest-lifetime wide. If
  that tree respawns and is the only candidate, the bot holds with the new dump --
  which will at least show it. Acceptable tradeoff; noting it.
- O4 (cosmetic): hold message says "within 16 tiles of Fred farm" but the scan is
  16 tiles from the player (player guaranteed within 8 of Fred by the pre-route).
- O5: commit message reused verbatim 9th time; describes nothing in this diff.

## Carried (still open)

- Build 25 dead-tinderbox-recover defect (both recovers gate on hold strings never
  emitted -- cross-version rescue only).
- b32 exactExpiredSourceHold unreachable-in-primary-scenario flag.

## Verdict

PASS. Chain of custody clean, delta coherent and directly addresses b33 O1,
one-shot scoping correct (persisted flag, checkpointed, bounded then terminal HOLD).
No blocking defects found. Live acceptance pending: screenshot feed dark since
2026-09-30 17:44 EDT (~20.4h), no confirmed live stream URL -- watch for
RECOVERED_EXACT_NO_TREE_WITHIN_8_HOLD / phase=RESCAN_FRED_TREES_WITH_LINE_OF_SIGHT
in diag when the feed returns.
