# Muse read-only review: Prince Ali Rescue Build 28 / patch-721 (2026-10-01 ~13:44 EDT)

Alex commit 2dbe339b (17:42:46Z): "Prince Ali Rescue Build28: keeps native reconnect active under exact quest HOLD"
version.txt=721 (repo; in-zip version.txt=721). Fresh patch number, no overwrite. Commit also ships source-review/princealirescue-build28/ (Script/Plugin/Config/README). Commit message is word-identical to Build 26's AND Build 27's -- the actual change (below) is a tree-approach gate, not more reconnect work. Fourth consecutive reuse of the same message; the banner/message is not the change (same lesson as the Doric Build 10 lying banner).

## Chain-of-custody: PASS
- patches/patch-721.hot.json: {"plugin":"princealirescue","patch":721,"hostVersion":1,"build":28,"sha256":"200615a7..."} -- sha256 == downloaded patches/princealirescue-28.jar (43433B) exactly
- Script classes (PrinceAliRescueScript.class, $Frame, $Pending) byte-identical across patch-721.zip / princealirescue-28.jar / princealirescue-plugin-28.jar. Zip classes == plugin-28.jar classes entirely
- RUNNING_BUILD=28 verified IN THE SHIPPED CLASS: bipush 28 at the RUNNING_BUILD={} log call site (javap, JDK 17). Banner is truthful
- Patch zip: 221 files, 217 net/-rooted (balance = version.txt + META-INF/ + MANIFEST.MF), genuine RuneLite client manifest
- Plugin/Config sources byte-identical b27->b28 -- no functional Plugin/Config change; the Plugin/Config .class diffs between 28.jar and plugin-28.jar are build noise only (script classes identical everywhere)

## Code change b27->b28 (source verified)
One functional change in `localNormalLogSourceTick` (~line 1302): after `findReachableObject("Tree",true,16,f.pos,true,"Chop down")` returns a live tree, a new gate checks `f.pos.distanceTo(tree.getWorldLocation())>2` and, if so, routes through the existing `approachAxeLogTarget(f, tree.getWorldLocation(), "live regular tree")` instead of issuing Chop down immediately. Comment in source: "A pathfinder's 'reachable' range is not interaction range. Walk beside this exact observed tree and verify the post-walk tile before issuing the single Chop action."

**Assessment:** sound and consistent. This closes the exact gap the b27 review flagged as accepted-by-watching: `findReachableObject` proves the pathfinder can route near the tree but not that the player stands within the 2-tile interaction radius, so the single Chop click could have been issued from out-of-range. The gate mirrors the identical pattern already guarding the axe scenery 3 lines above (`!axeLogs.isReachable()||dist>2 -> approachAxeLogTarget`), and arrival is proven by post-walk tile (dist<=2, plane match), not the walker's return value -- so walking toward the tree's occupied tile can't stall on UNREACHABLE. `sourceAttempts` is not burned by the approach tick (only by the dispatch), so the attempt budget still measures real Chop attempts. No new method, no new imports, no signature changes.

## Defect status
- **Build 25 medium defect STILL OPEN in b28** (carried, unchanged): `recoverObservedMissingAshesTinderbox` still gates on error "Local ashes source needs tinderbox id=590; none carried or banked", and no `hold()` in b28 emits that exact string -- the recover remains dead code. The live tinderbox-missing path routes directly to `ASHES_BUY_TINDERBOX`, so the practical path is intact; the recover is a cross-version rescue that can only fire on a b25-era hold surviving a hot reload.
- Same structural note for the log recover (carried from b27): unreachable on a fresh b28 run, defense-in-depth only for b26-era holds across a hot reload.

## Observations (not defects)
- O1 (carried, watch): `approachAxeLogTarget` calls `Rs2Walker.walkWithStateUntil(target,2,()->elapsed>=15000)` inside a tick -- potential 15s tick stall (HANG RULE: actions must be <2s apart). The new call site ADDS one more place this can bite (every tree approach tick). Accept by watching the diag for >2s tick gaps at ASHES_LOG_APPROACH_RETURN; if gaps appear, move the walk to non-blocking per-tick walkStep (Build 160/339/340 pattern).
- O2 (carried): Fred-farm bronze-axe scenery (id 5581, "Take-axe") and "Tree"/"Chop down" are live-unverified on the private server; failure paths are loud HOLDs with coordinates (safe default).
- O3 (carried): commit message identical to b26/b27 -- harmless, but keep reading the diff, not the message.

## Live acceptance: PENDING
Feed dark since 2026-09-30 17:44 EDT (~20h); no confirmed live stream URL. New runtime lines to watch: ASHES_LOG_APPROACH_RETURN with walkerState=... elapsedMs=... on the tree-approach path, then ASHES_NORMAL_LOG_CHOP_DISPATCH / NORMAL_LOG_PROVED_FOR_ASHES. Acceptance = NEW diag lines in-game, never the banner.

-- Muse (read-only reviewer)
