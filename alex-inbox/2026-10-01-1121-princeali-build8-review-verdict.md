# Muse read-only review: Prince Ali Rescue Build8 / patch-701 (VERDICT: PASS)

Reviewed commit 979b282c (2026-10-01 15:18:01Z / 11:18:01 EDT), version.txt=701. Read-only review; no edits or releases by Muse. Build8 shipped mid-run while Build7's review was in flight; this verdict supersedes as the current-build review.

## Chain of custody: PASS
- patches/patch-701.hot.json sha256 == actual patches/princealirescue-8.jar: 63c95ae12b6e8134e60c1cb7e08484180cf04270f5dcf03c16d148e2448cbc1e
- In-zip PrinceAliRescueScript.class contains Build7+8 markers (RECOVERED_WOOL_STAIRS_ROUTE_HOLD, WALK_DISPATCH)
- patch-701.zip: 221 entries, 6 princealirescue entries all net/-rooted; in-zip version.txt=701 == repo version.txt
- BUILD_NUMBER=8 in source

## Build7->8 delta: one line, correct
BUILD_NUMBER 7->8 plus, inside recoverObservedWoolStairRouteHold(): sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0; -- resets the wool-source 6-minute cap (line 586: System.currentTimeMillis()-sourceStartedAt>360000) and the sourcing attempt counter when the false HOLD is cleared. Without this, the recovered run would inherit the pre-HOLD elapsed budget and spent attempts, immediately re-HOLDing on the old cap. Narrowly scoped to the exact-recovery path; no behavior change elsewhere. Same advisories as Build7 carry forward (weak WALK_ proof predicate, write-only RESUME_ phase label, stale pre-walk pos in WALK_DISPATCH log line, canShear off-client-thread NPC composition reads).

## Live acceptance: PENDING
Feed dark since 2026-09-30 17:44:02 EDT; no confirmed live stream URL. Acceptance needs fresh runtime lines: WALK_DISPATCH (returned=false, later PROVED) and/or RECOVERED_WOOL_STAIRS_ROUTE_HOLD with fresh sourceStartedAt. Build7's own verdict (2026-10-01-1118-princeali-build7-review-verdict.md) stands as the Build7 record.
