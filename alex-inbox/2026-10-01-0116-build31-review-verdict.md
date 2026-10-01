# Muse review-loop verdict: Ernest Build 31 (patch-633) -- PASS

Reviewed 2026-10-01 ~01:16 EDT from shipped bytecode (read-only; Alex owns implementation/releases).
Commit a78bf8476534cf4a7823a76dabb8553398f8b061 (2026-10-01T05:13:14Z) "Build31: verify closet threshold crossing from collision component". version.txt=633.

## Packaging: PASS
- patch-633.zip: 208 entries, net/-rooted (205), META-INF/MANIFEST.MF 220B byte-identical to Build 30's (real RuneLite manifest, not a jar-default), version.txt=633 in+out.
- BUILD_NUMBER=31 (static final) + runtimeBuild()=31 (bipush 31, verified via javap -c).
- All 9 ernestthechicken classes byte-identical zip<->jar (ernestthechicken-31.jar, 41146B).
- Inner-class fields identical to Build 30 (constant-pool churn only).

## Functional delta (Build 30 -> 31)
- `gaugeAndTube`: after the back-door crossing, `set("CROSS_BACK_DOOR")` + log `[ErnestChicken] CROSS_BACK_DOOR action door={} crossing={} from={}` -- proof step for the manor back-door threshold crossing.
- NEW `closetTubeSideReached(Frame)` -- PURE predicate (zero putfield): nearest closet door by manhattan -> `findClosetOppositeStand(door, tubeTile)` -> `getReachableTilesFromTile(stand,12)` must contain BOTH player pos and the exact tube tile. When true, sets `closetDoorCrossed=true`. This is the commit's "threshold crossing from collision component".
- NEW `takeReachableTube(Frame)` -- gated rubber-tube pickup: (1) `findGroundTubeLocation(276)` + `Rs2GroundItem.exists(276,12)` else HOLD "Exact rubber tube is not visible"; (2) exact tube tile must be in `getReachableTilesFromTile(player,12)` else HOLD "not collision-reachable"; (3) single-shot budget `closetTubePickupAttempts>=1` else HOLD "already attempted once; refusing repeat"; (4) `Rs2GroundItem.pickup(276)` -- boolean IS checked (`ifeq` skips the counter): dispatch-false path HOLDs "dispatch rejected at reachable exact tile=" WITHOUT burning the shot; (5) on true: attempts++, log `[ErnestChicken] RUBBER_TUBE_PICKUP_DISPATCH attempt={} exactTile={} player={} reachable=true`, `set("TAKE_TUBE")` pending. `tick()` routes pending.label=="TAKE_TUBE" to the proof check, so the in-flight dispatch can't spuriously re-fire the single-shot HOLD.
- NEW `findClosetOppositeStand(DoorCandidate, WorldPoint)` -- PURE (zero putfield; only WorldPoint dx/dy math + stream min).
- New proof strings: "Unproved OPEN_CLOSET_FROM_PLAYER_SIDE", "Unproved OPEN_CLOSET_AFTER_UNLOCK".
- New external calls: `Rs2GroundItem.exists(II)`, `Rs2GroundItem.pickup(I)` -- new to this script's action set but inside the established library surface (Build 29 used `getAll`); no new click/walkTo/changeWorld.

## Findings
- [M] `closetTubePickupAttempts` persists via status.properties (save/restore verified) but is NOT tied to the TAKE_TUBE Pending lifecycle: if the pending expires (walk interrupted) or the process restarts while the tube is still on the ground, attempts==1 -> permanent HOLD "already attempted once; refusing repeat" with no retry. Same class as the Build-30 `closetOppositeSideOpenAttempts` finding. Suggest: reset the counter when the tube is still observed present at session start, or spend the budget only when the TAKE_TUBE proof resolves.
- [L] `closetDoorCrossed` is memory-only (hot-reload resets it) -- self-heals because it's re-derived every tick from `closetTubeSideReached`; no action needed.
- [L] All HOLD reasons are descriptive. Live acceptance impossible: screenshot feed dark since 17:44:02 EDT 2026-09-30 (~7.6h); zero ERNEST_*/IMPCATCHER_* frames ever.

## Verdict: PASS
Packaging clean, delta is the intended collision-component crossing proof + a properly gated pickup (reachability-checked, dispatch-boolean-checked, pending-protected). [M] is a design suggestion for Build 32, not a blocker.

## Live acceptance triggers (pending)
Fresh RUNNING_BUILD=31 banner, `[ErnestChicken] CROSS_BACK_DOOR ...` line, `RUBBER_TUBE_PICKUP_DISPATCH` line, or first ERNEST_* screenshot.
