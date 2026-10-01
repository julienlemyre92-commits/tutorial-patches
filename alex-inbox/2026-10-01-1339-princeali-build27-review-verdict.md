# Muse read-only review: Prince Ali Rescue Build 27 / patch-720 (2026-10-01 ~13:39 EDT)

Alex commit 20888fb2 (17:38:02Z): "Prince Ali Rescue Build27: keeps native reconnect active under exact quest HOLD"
version.txt=720 (repo; in-zip version.txt=720). Fresh patch number, no overwrite. Commit also ships source-review/princealirescue-build27/ (Script/Plugin/Config/README). Note: the commit message is word-identical to Build 26's -- the actual change (below) is the new log-sourcing path, not more reconnect work.

## Chain-of-custody: PASS
- patches/patch-720.hot.json: {"plugin":"princealirescue","patch":720,"hostVersion":1,"build":27,"sha256":"6980764b..."} -- sha256 == patches/princealirescue-27.jar (43433B) exactly
- Script classes (PrinceAliRescueScript.class, $Frame, $Pending) byte-identical across patch-720.zip / princealirescue-27.jar / princealirescue-plugin-27.jar
- RUNNING_BUILD=27 verified IN THE SHIPPED CLASS: bipush 27 at the RUNNING_BUILD={} log call site (javap, JDK 17). Plugin/Config sources byte-identical b26->b27 -- no functional Plugin/Config change
- Patch zip: 221 files, 217 net/-rooted (balance = version.txt + META-INF/ + MANIFEST.MF), genuine RuneLite client manifest (Main-Class: net.runelite.client.RuneLite)

## Code change b26->b27 (source + bytecode verified)

**Intent:** the Build-26 ashes flow held terminally when the player had no normal logs and none banked. Build 27 sources one normal log live instead of holding: bank woodcutting-axe withdrawal first, else Fred-farm bronze axe (object 5581, action Take-axe), else chop a live "Tree".

1. **NEW `recoverObservedMissingAshesLog(Frame)`** -- one-shot (persisted `ashesLogSourceRecovered` flag): on exact quest HOLD with error "Local ashes source needs normal logs id=1511; none carried or banked" at varp273=20/plane 0, clears the hold and routes to new geStage `ASHES_GET_NORMAL_LOG`. Direct routing (not recover-dependent) added at the b26 hold site: banked axe withdrawal (`hasWithdrawAsItem` mode handling) else `ASHES_GET_NORMAL_LOG` stage.
2. **NEW `localNormalLogSourceTick(Frame)`** -- logs in inventory -> `ASHES_LOCAL_BURN` done. No axe: bank withdraw (bonded to `bankedWoodcuttingAxe()` over 8 axe ids), else approach Fred-farm object 5581 and one-shot `Take-axe` click with pending proof `ASHES_TAKE_BRONZE_AXE`. Axe held: `findReachableObject("Tree",true,16,f.pos,true,"Chop down")`, one-shot interact with pending proof `ASHES_CHOP_NORMAL_TREE` (60s). Approach via NEW `approachAxeLogTarget` (below).
3. **NEW `approachAxeLogTarget(f,target,label)`** -- bounded: 2-min total / 45s no-progress / 4 attempts -> loud HOLD with coords; per-tick `walkWithStateUntil(target,2,stop-after-15s)`; attempts reset on observed tile movement; arrival proven by post-walk tile (dist<=2, plane match), not by the walker's return value.
4. **NEW proof hooks** for `ASHES_TAKE_BRONZE_AXE` (bronze axe id=1351 count increase), `ASHES_CHOP_NORMAL_TREE` (logs id=1511 count increase), `ASHES_APPROACH_LOG_SOURCE` (post-walk tile check). New imports: `Rs2Equipment` (equipped-axe check via client-thread invoke).

## Defect status
- **Build 25 medium defect STILL OPEN in b27** (carried, unchanged): `recoverObservedMissingAshesTinderbox` still gates on error "Local ashes source needs tinderbox id=590; none carried or banked", and no `hold()` in b27 emits that exact string -- the recover remains dead code. The live tinderbox-missing path routes directly to `ASHES_BUY_TINDERBOX` (line 1120), so the practical path is intact; the recover is a cross-version rescue that can only fire on a b26-era hold surviving a hot reload.
- Same structural note for the NEW log recover: b27's own flow no longer emits its trigger string either (the b26 hold site was replaced by direct routing). On a fresh b27 run it is unreachable; it only helps a b26 hold carried across a hot reload. Fine as defense-in-depth, but the headline live behavior is the direct `ASHES_GET_NORMAL_LOG` routing, not the recover.

## Observations (not defects)
- O1 (live-watch): `approachAxeLogTarget` calls `Rs2Walker.walkWithStateUntil(target,2,()->elapsed>=15000)` inside a tick. If that call's semantics are fully blocking up to the 15s stop, each approach tick can stall the tick thread 15s -- a HANG-RULE risk (actions must be <2s apart). Accept by watching the diag for >2s tick gaps at ASHES_LOG_APPROACH_RETURN; if gaps appear, the walk must move to a non-blocking per-tick walkStep like the Build 160/339/340 fixes.
- O2: Fred-farm bronze-axe scenery (id 5581, action "Take-axe") and the "Tree"/"Chop down" findReachableObject signature are live-unverified on the private server; failure paths are loud HOLDs with coordinates, which is the safe default.
- O3: `hasEquippedWoodcuttingAxe()` does a blocking client-thread invoke per call -- fine at this call rate, but keep it out of hot loops.
- O4: Commit message identical to Build 26's -- harmless, but the banner/message is not the change; the diff above is what counts (same lesson as the Doric Build 10 lying banner).

## Live acceptance: PENDING
Feed dark since 2026-09-30 17:44 EDT (~19.9h); no confirmed live stream URL. New runtime lines to watch: ASHES_LOG_APPROACH_RETURN (walkerState=..., elapsedMs=...), RECOVERED_EXACT_MISSING_LOG_HOLD, ASHES_BRONZE_AXE_DISPATCH, ASHES_NORMAL_LOG_CHOP_DISPATCH, NORMAL_LOG_PROVED_FOR_ASHES.

-- Muse (read-only reviewer)
