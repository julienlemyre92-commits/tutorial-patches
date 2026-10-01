# Muse review-loop verdict: Ernest Build 32 (patch-634) -- PASS

Reviewed 2026-10-01 ~01:23 EDT from shipped bytecode (read-only; Alex owns implementation/releases).
Commit 92d0d1b7108d55f4f5f8b2e8b4e5d0b2b3a4b3f0 (2026-10-01T05:21:18Z) "Build32: use verified manor staircase route to Oddenstein". version.txt=634.
hot.json: plugin=ernestthechicken, patch=634, build=32, sha256=9ccfd6debde610434c9d51e0472d7eabba91778f1b49372e94ff44071536129b.

## Packaging: PASS
- patch-634.zip: 208 entries, net/-rooted, version.txt=634 in+out. MANIFEST.MF = real RuneLite manifest (Main-Class net.runelite.client.RuneLite).
- BUILD_NUMBER=32 (static final) + runtimeBuild()=32 (bipush 32, verified via javap -c).
- All 9 ernestthechicken classes byte-identical zip<->jar (ernestthechicken-32.jar, 42196B).
- Inner classes ($DoorCandidate, $Frame, $LoginFrame, $Pending, $SkillLevelReview): no field/method signature diffs vs Build 31 -- constant-pool churn only.

## Functional delta (Build 31 -> 32)
- NEW `STAIRS0 = (3109, 3364, 0)` (manor staircase base), `ODDENSTEIN = (3116, 3364, 2)`.
- Route logic: while plane < 2, expected stair = same XY as STAIRS0 on the CURRENT plane; walk to within 2 tiles ("TO_ODDENSTEIN_STAIRS_P"+plane tag), then climb.
- Climb: single-shot budget per leg (`oddensteinStairs0to1Attempts` / `oddensteinStairs1to2Attempts`, attempts>=1 -> HOLD "Manor staircase climb already attempted once on plane=... expectedStair=...").
- Target verified as named scene object: `Rs2GameObject.getTileObject("Staircase", expectedStair, 3)`; null/missing/too-far (>3) -> HOLD "No named manor staircase near <target>; sceneObjects=<nearbyNamedStaircases>" (NEW pure-ish helper listing nearby composition names as diag).
- `Rs2GameObject.interact(tileObj, "Climb-up")` -- boolean CHECKED: false -> HOLD "Manor staircase Climb-up dispatch rejected at <tile> from=<player>" WITHOUT burning the shot.
- On true: log `[ErnestChicken] MANOR_STAIRCASE_DISPATCH step={} id={} tile={} name={} player={} action=Climb-up` (composition name via getCompositionName, orElse "?"), attempts++, `set(CLIMB_ODDENSTEIN_STAIRS_0_TO_1 | _1_TO_2)` pending with 9000ms timeout.
- Proof predicate (pending-proof): `frame.pos.getPlane() == before.pos.getPlane() + 1` AND `frame.pos.distanceTo(expectedStair) <= 3` -- observed-state plane+proximity proof, in the established pending pattern. Expiry -> "Unproved manor staircase climb after one action; step=... from=... now=...".
- After plane 2: distanceTo(ODDENSTEIN) > 8 -> walk "TO_ODDENSTEIN"; else `npc(3562, ODDENSTEIN, "TALK_ODDENSTEIN")` -- 3562 = Professor Oddenstein (wiki-verified NPC ID).
- No new external API calls beyond the established library surface (getTileObject/getTileObjects/interact/getCompositionName -- Build 29 used getAll).

## Findings
- [M] The single-shot attempt budgets persist via status.properties (save/restore verified) but are NOT tied to the pending lifecycle: if `interact()` returns true (counter spent) but the climb never lands (proof fails -- walk interrupted, plane doesn't change as expected), attempts==1 -> permanent "already attempted once" HOLD with no retry. Same class as the Build-31 `closetTubePickupAttempts` finding. Suggest: spend the budget only when the climb proof resolves, or reset the counter when the pending expires unproved with the staircase still observed nearby.
- [L] Proof requires plane+1 exactly; a staircase that skipped 2 planes in one click would fail proof -> expiry -> then the [M] trap. The manor staircase is a single-plane stair in practice, so acceptable, but the expiry HOLD is only diagnostic, not fatal, until the [M] path locks it.
- [L] Live acceptance impossible: screenshot feed dark since 17:44:02 EDT 2026-09-30 (~7.6h); zero ERNEST_*/IMPCATCHER_* frames ever.

## Verdict: PASS
Packaging clean, delta is the intended manor-staircase route with named-object verification, adjacency gates, dispatch-boolean-checked single shots, and an observed-state plane+proximity proof -- the commit's "verified manor staircase route to Oddenstein". [M] is a design suggestion for Build 33, not a blocker.

## Live acceptance triggers (pending)
Fresh RUNNING_BUILD=32 banner, `[ErnestChicken] MANOR_STAIRCASE_DISPATCH ...` line, or first ERNEST_* screenshot.
