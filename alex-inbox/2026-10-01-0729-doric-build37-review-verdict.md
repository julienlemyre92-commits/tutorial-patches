# Muse review-loop verdict: Doric Build 37 (patch-676) -- PASS
Date: 2026-10-01 ~07:29 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)

- Ship: patch-676, commit 49267e78 2026-10-01T11:23:48Z "Doric Build37: move training to six-tin West Falador mine". version.txt=676.
- Packaging clean: 215-entry net/-rooted zip (100 entries junk-path check N/A -- all prefixed net/; MANIFEST.MF byte-identical to patch-675), version.txt=676 in-zip == repo (no reuse), 200/200 classes identical lists (no stale-class reship). Built with `zip`, not `jar`.
- Changed set: exactly the 5 doricsquest classes (same set as Builds 26-36). Plugin diff = bipush 36->37 only. Script diff (sig-normalized, lambda-number-normalized): +1 field WEST_FALADOR_TIN_MINE_WAYPOINT=(2906,3355,0); BUILD_NUMBER 36->37; +1 static WorldPoint predicate lambda$trainMining$4 (rock within 22 of new waypoint, reused for the failed-rock site check); zero signature changes otherwise.
- Semantics: trainMining now routes TO_TIN_TRAINING_MINE when player >22 tiles from the new waypoint; on arrival, failedTinRocks.stream().noneMatch(nearNewSite) -> clears failedTinRocks, zeroes trainingMineNoChangeAttempts, nulls avoidedTinRock, logs `[DoricsQuest] TIN_TRAINING_SITE_CHANGED waypoint={} decision=RESET_SITE_LOCAL_ROCK_FAILURES` -- i.e. site-local rock-failure state resets on training-site change, exactly what the commit message claims. Quest-material mining (getMaterials) still uses RIMMINGTON_MINE_WAYPOINT -- only the training mine moved. Waypoint constants: old Rimmington (2985,3238,0) retained, new West Falador (2906,3355,0) added.
- No new net/runelite/api refs anywhere in the delta (verified across all 200 classes). No new game-API calls.
- Hot chain VERIFIED: patch-676.hot.json sha256 == doricsquest-37.jar file bytes (28,825B) exact; jar's 4 doricsquest script classes byte-identical to the patch zip's.
- Race check: inbox already has 2026-10-01-0130-build37-review-verdict.md but it is Ernest's Build 37 (patch-639) -- different bot. No sibling on Doric Build 37.
- Live acceptance pending: TIN_TRAINING_SITE_CHANGED / RESET_SITE_LOCAL_ROCK_FAILURES / TO_TIN_TRAINING_MINE runtime lines. Screenshot feed dark since 2026-09-30 17:44:02 EDT (~13.75h); zero DORIC_*/ERNEST_*/IMPCATCHER_* frames ever -- acceptance rests on Alex's direct runtime reports.

Verdict: PASS -- ship matches its commit message; routing change is sound; site-local failure reset is the correct lifecycle; packaging and hot-reload chain byte-clean.
