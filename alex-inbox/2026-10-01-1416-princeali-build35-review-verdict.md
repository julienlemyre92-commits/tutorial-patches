# Muse read-only review verdict: Prince Ali Rescue Build 35 / patch-728

- Build 35 (patch-728, commit 0d98c9b9 2026-10-01T18:14:24Z; version.txt 727->728).
  Commit message reused verbatim 10th time ("keeps native reconnect active under
  exact quest HOLD") -- describes nothing in this diff. Harmless; misleading history.
- Scope: read-only review. Alex owns implementation/releases; nothing shipped by Muse.

## Chain of custody: PASS

- hot.json sha256 5791384a1cae0253356ec32ee57673e074cfd330087601561b0d9bd93b0f3b4e
  == downloaded patches/princealirescue-35.jar byte-for-byte.
- PrinceAliRescueScript.class byte-identical across patch-728.zip and
  princealirescue-35.jar.
- patch-728.zip: 221 entries, net/-rooted; META-INF/MANIFEST.MF has genuine
  Main-Class: net.runelite.client.RuneLite; in-zip version.txt=728 == repo version.txt.
- RUNNING_BUILD=35 verified in shipped class (javap: banner bipush 35 + runtimeBuild()=35);
  BUILD_NUMBER=35 in published source.
- Plugin/Config sources byte-identical b34->b35; Script 126908->129611 bytes.
  New strings (RECOVERED_EXACT_FRED_TREE_NO_LOS_HOLD,
  "No untried reachable regular Tree with Chop down within 16 tiles of Fred farm",
  ashesNoLosTreeTargets, approach radius label) present in shipped class constant
  pool -- shipped class matches published b35 source.

## Change b34->b35 (source-verified, diff = 53 lines)

1. Tree sourcing: `nearestVisibleNormalTree` (scan-time LOS) replaced by
   `nearestReachableNormalTree` -- exact name "Tree", action "Chop down",
   `isReachable` only at scan; LOS is now verified FRESH after walking adjacent
   (approach radius 1). A tree without LOS at chop range is rejected into the new
   `ashesNoLosTreeTargets` set ("id@tile" keys) and the next nearest reachable
   tree is tried. Directly answers the b33 observation (a merely path-reachable
   tree behind a live gate/collision edge).
2. `approachAxeLogTarget` gains a radius overload; Fred-tree approach now walks
   to radius 1 before the LOS check.
3. No-tree hold now reports `rejectedNoLos=<set>` plus candidate diagnostics --
   good observability.
4. New persisted one-shot flag `ashesFredLineOfSightApproachRecovered` and new
   one-shot `recoverObservedFredTreeLineOfSightHold` keyed on the exact b34 hold
   prefix "No live regular Tree with Chop down and clear line of sight within 16
   tiles of Fred farm;"; gates: ashesFredTreeRescanRecovered fired, ASHES/1,
   ASHES_GET_NORMAL_LOG, sourceAttempts==0, LOGGED_IN, varp==20, plane 0,
   within 2 of FRED_POS, no logs, tinderbox + axe. Clears hold, sets diag phase
   RESCAN_FRED_TREES_AND_APPROACH_FOR_LOS, clears the rejection set, fresh window.
5. The rejection set survives hot reload via the in-memory state HashMap (not the
   .properties checkpoint -- verified the Set never touches status.properties);
   lost only on full client restart, which is acceptable.

## Observations for Alex

- O1 (medium, new): the no-tree hold string CHANGED again -- b35 emits "No
  untried reachable regular Tree with Chop down within 16 tiles of Fred farm;..."
  which has NO one-shot recover keyed to it. The b34-era recover's gate string
  ("No live regular Tree with Chop down and clear line of sight...") is now
  referenced ONLY in that gate (verified: single occurrence at script line 1765)
  -- b35's own code never emits it. If the rejection set ever fills completely
  (every candidate tree rejected on fresh LOS), the bot parks in a HOLD no
  recover handles. Same churn pattern flagged at b34 O1, one level deeper.
- O2 (low-medium): the rejection set is cleared only by the one-shot recover,
  so it is quest-lifetime and monotonically grows; stale "id@tile" keys from an
  earlier sourcing episode can shadow trees that would be fine in a later episode
  (e.g. after the shop run). No TTL, no per-episode reset.
- O3 (positive note): verified the write-only phase strings are harmless by
  design -- main dispatch is on geStage (line 752), phase is a diag label
  (same pattern as b29/b34). So RESCAN_FRED_TREES_AND_APPROACH_FOR_LOS being
  label-only is intended, not a defect.
- O4 (cosmetic): commit message reused 10th time; describes nothing in this diff.

## Carried (still open)

- Build 25 dead-tinderbox-recover defect (recovers gate on hold strings never emitted).
- b32 exactExpiredSourceHold unreachable-in-primary-scenario flag.
- b34 O3: permanent id1276@(3265,3215,0) exclusion (inherited).

## Verdict

PASS. Chain of custody clean, delta coherent and directly addresses b33 O1
(fresh post-approach LOS + reject-set is the right mechanism). One-shot scoping
correct (persisted flag, checkpointed, bounded). No blocking defects found.
Live acceptance pending: screenshot feed dark since 2026-09-30 17:44 EDT
(~20.5h), no confirmed live stream URL -- watch for
RECOVERED_EXACT_FRED_TREE_NO_LOS_HOLD / ASHES_TREE_REJECT_NO_LOS in diag when
the feed returns.
