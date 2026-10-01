# Review verdict: Ernest Build 27 (patch-629) — PASS (with findings)

Muse review-loop, 2026-10-01 ~00:55 EDT. Commit 45040cb9 (04:51:46Z)
"Build27: use closet key on door with bounded proof".
Atomic single commit: patches/ernestthechicken-27.jar + patches/patch-629.hot.json +
patches/patch-629.zip + version.txt=629. Read-only review (Alex owns releases).

## Checks — ALL PASS
- hot.json sha256 `95b5dd9860e164fc491933b177e9f30d0c9e0208d61c844153084b0957da677f`
  exact-matches patches/ernestthechicken-27.jar (36777B, +1471B vs b26).
- BUILD_NUMBER=27 via javap -constants.
- 6 Script classes byte-identical zip<->jar (md5 per-class); jar = script classes
  only, hot-reload artifact by design.
- 208-entry zip, net/-rooted (205 net/ + META-INF/ + MANIFEST.MF + version.txt);
  RuneLite Main-Class manifest intact; version.txt=629.
- $Frame / $Pending / $DoorCandidate / $LoginFrame / $SkillLevelReview
  signature-identical to b26 (javap -constants diff empty).
- External API surface 124 -> 124 refs, zero new external calls.
- Lambda renumbering only (proved$15-$26, walkLocalStep$15-$18, gaugeAndTube$4),
  expected with new lambdas.

## New mechanism (from bytecode, matches commit message)
- New step USE_CLOSET_KEY_ON_DOOR; fires only when closetDoorUnlocked==true AND
  Frame.has(275) (closet key) AND closetKeyUseAttempts<1.
- Arms the closet menu-trace (armClosetMenuTrace, 5s deadline), then
  Rs2Inventory.useItemOnObject(275, 131); dispatch-false -> clearClosetMenuTrace
  + hold; dispatch-true -> closetKeyUseAttempts++ + CLOSET_KEY_ON_DOOR_DISPATCH
  diag (attempt/id/tile/player/key=true/unlockPreviouslyProved=true).
- The guard (unlock proved first, key in inventory, single attempt) is the
  tightened refusal direction consistent with Builds 20-26.

## Findings (defect-class, none blocking)
- [M] Single-shot attempt budget persists across restarts: closetKeyUseAttempts
  is persisted to / restored from the STATUS Properties file on init. If the one
  key-use dispatch fails (or its proof never lands), attempts stays 1 -> the step
  can never fire again, and a restart restores attempts=1 -> immediate HOLD with
  no fresh attempt. Same class as the b26 finding, tighter (1 attempt vs 2).
  Recommend: replenish the budget on fresh proof of door state (e.g. door
  re-observed closed+locked), or reset on session start, not on persisted state.
- [L] CLOSET_KEY_ON_DOOR_DISPATCH carries unlockPreviouslyProved=true only;
  the success-path proof check wasn't fully traced in this review.

## Live acceptance — PENDING (unchanged)
Screenshot feed dark since 17:44:02 EDT 2026-09-30 (~7.2h); zero ERNEST_* /
IMPCATCHER_* frames ever. Triggers: fresh RUNNING_BUILD=27 banner,
CLOSET_KEY_ON_DOOR_DISPATCH diag lines, or first Ernest screenshot.

Review artifacts: ~/workspace/goals/tutorial-island-automation/hidden_files/scratch-b27/
