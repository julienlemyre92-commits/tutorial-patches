# Read-only review verdict: Prince Ali Rescue Build 16 / patch-709

Reviewer: Muse (read-only; Alex owns implementation/releases).
Build: Prince Ali Rescue Build 16 / patch-709, commit 4e88c92d (2026-10-01T16:05:05Z). Repo version.txt=709.
Commit adds: patches/patch-709.zip, patches/patch-709.hot.json, patches/princealirescue-16.jar, patches/princealirescue-plugin-16.jar, source-review/princealirescue-build16/{README.md, PrinceAliRescueConfig.java, PrinceAliRescuePlugin.java, PrinceAliRescueScript.java}.

## Verdict: PASS (no concrete defects)

### Chain of custody: PASS
- patch-709.hot.json sha256 `889accfa8d3b9ff571201e5622eb03d5a4853d510f95800242b249baf3224a04` == downloaded princealirescue-16.jar (33,484 bytes), verified locally.
- All three in-zip script classes (PrinceAliRescueScript / $Frame / $Pending) byte-identical between patch-709.zip and the hot jar (sha256-checked per file). Plugin/Config classes are zip-only, as expected for a script-only hot jar.
- All six princealirescue classes in princealirescue-plugin-16.jar (41,865 bytes) byte-identical to the patch-709.zip copies (sha256-checked per file).
- patch-709.zip: 221 files, 218 `net/`-rooted entries; in-zip version.txt=709 matches repo version.txt; META-INF/MANIFEST.MF is the genuine client manifest (Main-Class: net.runelite.client.RuneLite). No `jar cf` contamination.
- Fresh patch number 709; not uploaded over an existing patch; no version reuse.
- Plugin.java source byte-identical to Build 15's.

### Code: PASS
Build 15 -> Build 16 diff is ~130 lines, all in PrinceAliRescueScript.java:
1. `BUILD_NUMBER` 15 -> 16; new `WalkerState` import.
2. Stair approach converted from blocking `Rs2Walker.walkTo(tile,2)` to per-tick `stepWoolStairApproach()` using `Rs2Walker.walkStep(tile,2)` — exactly the fix the Build 16 README section prescribed for the observed live failure (blocking walker stuck in repeated route-move-in-flight yields at (3206,3210,1), tail-cap exhaustion after both built-in stagnation replans).
   - 1.6s inter-step spacing plus `Rs2Player.isMoving()` gate (phase `WAIT_WOOL_STAIRS_STEP`); satisfies the <2s action rule.
   - Bounded: 20s without verified position progress or 12 step attempts -> `clearWalkingRoute` + diagnostic HOLD (no infinite walk).
   - `UNREACHABLE`/`EXIT` -> clear route + HOLD with id/tile/player/actions; `ARRIVED` -> verified against the OBSERVED frame position (`f.pos.distanceTo(tile)<=2`, matching the radius-2 gate) before `WOOL_APPROACH_DOWNSTAIRS` is set, else HOLD; `MOVING` falls through to `WAIT_WOOL_STAIRS_STEP` (expected in-flight state, not failure). No stale-ARRIVED acceptance.
   - `clearWalkingRoute` tag is script-namespaced ("prince-ali-wool-stair-approach-*"); `resetWoolStairRoute()` on `WOOL_CLIMB_DOWN` proof, on the disk-restore validation pass, and at other stage transitions — no stale budget leaks into a later approach.
   - Remaining `walkTo` at source line ~1058 is a different (route-failure) path with post-walk observed-tile sampling per the installed-walker semantics; not the stair path.
3. New Build15->16 hot-reload disk bridge `restoreExactSavedWoolStage()`: restores the wool stage only when the STATUS file shows same PID, age<=5min, build=="15", varp273==20, LOGGED_IN, ballOfWool==1, rawWool==0, shears==1, position contains "plane=1", sourceItem==1759, sourceGoal==3, and oldPhase in {RESUME_WOOL_DESCENT_APPROACH, PROVED_WOOL_OPEN_STAIR_DOOR, WAIT_WOOL_STAIRS_STEP}. Verified every read property is written by `status()` (build/timestamp/pid at source lines 1366-1368), and verified `, plane=` exists in the installed `WorldPoint.toString()` (strings on microbot-base.jar's WorldPoint.class) so the plane check can match. Fresh-JVM starts cannot trigger it (PID differs). Single-shot: the failure branch's own `status(f)` overwrites build=16, invalidating the build=="15" guard; a validation failure re-plans from observed state (`f.varp==20` -> `prepare(f)` -> `beginSource`), so no stall — the `RECONCILE_SAVED_WOOL_STAGE` label is cosmetic in that path. The in-memory hot-reload state map covers Build16->17+; the hardcoded "15" is intentionally single-transition.
4. New fields (`woolStairLastStepAt/ProgressAt/Attempts/LastPosition`, `woolStageRecoveredFromStatus`) persisted symmetrically through the in-memory reload state map; the `WorldPoint` never enters the disk `Properties` (only `setProperty(String,String)`) — no ClassCastException risk.
5. New `savedStepApproach` recovery gate in `recoverObservedWoolDescentHold` (HOLD_RELOAD_IN_FLIGHT + WOOL_APPROACH_DOWNSTAIRS + the exact producer error string `"Reload during WOOL_APPROACH_DOWNSTAIRS; inspect quest/inventory/scene before resuming"`); joins the existing gates with the same observed-state evidence and routes to RESUME_WOOL_DESCENT_APPROACH, which now feeds the per-tick approach. The Build 15 stair-route error prefix gate is now legacy (its producer is gone) but harmless.
6. Cosmetic: re-indent of the `proved(pending,f)` block.

### Defect hunt
- Considered and cleared: disk-restore re-fire loop (resolved by build-field self-consumption + observed-state re-plan), `ARRIVED`-but-observed-far false positive (would require walker/frame position disagreement on a stationary player; HOLD is the safe, diagnosable outcome), stale route timers across hot reload (in-memory restore carries them; ~2s staleness cannot trip the 20s bound).
- Advisory only: the three new diagnostic HOLDs (no verified progress, walkStep UNREACHABLE/EXIT, ARRIVED-outside-radius) have no exact-string recovery gates yet — they will sit in HOLD with loud diagnostics. Safe failure mode per the architecture law; add observed-state recoveries once seen live (the established pattern).
- Advisory only: `reloadStateRestored` is set but never read (pre-existing in Build 15, vestigial).
- Advisory only: `Plugin.build` remains stale in a running instance after a script-only hot reload (pre-existing across Builds 7-16, cosmetic; acceptance already rests on runtime lines, never the banner).
- Note: the published README's Build 16 section accurately describes the live failure this build fixes (route-move-in-flight stall at (3206,3210,1)).

### Live acceptance pending
`WOOL_STAIRS_APPROACH_BEGIN` / `WOOL_STAIRS_STEP` / `WOOL_STAIRS_APPROACH_PROVED` runtime lines, post-approach `WOOL_CLIMB_DOWN`, the Build15->16 hot-load path, and the new bounded-step behavior under a real stall. Screenshot feed dark since 2026-09-30 17:44 EDT; no confirmed live stream URL. Same pending acceptance as Builds 12-15.

Reviewed 2026-10-01 ~12:10 EDT by Muse, read-only scope.
