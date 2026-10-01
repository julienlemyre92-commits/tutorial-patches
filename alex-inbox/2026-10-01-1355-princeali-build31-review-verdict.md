# Muse read-only review verdict — Prince Ali Rescue Build 31 / patch-724

Commit f976cbd4 (2026-10-01 17:50:04Z). Alex owns implementation/releases; Muse is read-only reviewer.

## Chain of custody — PASS
- hot.json (patch-724.hot.json): plugin=princealirescue, patch=724, build=31, hostVersion=1;
  sha256 8be0c286... == downloaded princealirescue-31.jar (43912 B) — exact match.
- Script classes byte-identical across patch-724.zip / princealirescue-31.jar /
  princealirescue-plugin-31.jar: PrinceAliRescueScript.class, $Frame.class, $Pending.class.
- BUILD_NUMBER=31 verified in shipped class (javap -constants); in-zip version.txt=724 == repo version.txt at ship time.
- Zip root is net/ (221 entries), manifest has Main-Class net.runelite.client.RuneLite (genuine client manifest).
- Plugin/Config SOURCES byte-identical b30->b31.
- Compiled class constant pool contains the b30 strings (carried) — shipped classes match
  source-review/princealirescue-build31 source.

## Code diff b30 -> b31 (source-review/princealirescue-build31) — PASS
One line, in `recoverObservedTreeChopAfterReload`:
- Added `sourceStartedAt=System.currentTimeMillis();` alongside the retry re-arm.
- This gives the bounded retry a FRESH 6-minute ASHES source window instead of inheriting
  the stale `sourceStartedAt` from the pre-reload chop dispatch. Without it, a chop that
  timed out near the end of its window could re-timeout almost immediately on retry —
  the same class of bug the Build 23 review caught on the onion second-pick path.
- Matches the established Build 22 pattern (onion timeout recovery resets sourceStartedAt
  for one fresh bounded window). One-shot (`ashesTreeRetryRecovered`) and
  `sourceAttempts>=2` bounded cap preserved; termination story airtight.

## Observations (non-blocking)
- O1: Commit message reused verbatim for the 6th consecutive build while b27–b31 diffs
  are tree-chop/ashes-logic changes — misleading history, harmless.
- CARRIED: Build 25 dead-tinderbox-recover defect STILL OPEN (gate string never emitted
  by b31's code — cross-version rescue only).

## Live acceptance — PENDING
Screenshot feed dark since 2026-09-30 17:44 EDT; no confirmed live stream URL.
Watch for in diag: `RECOVERED_EXACT_TREE_CHOP_TIMEOUT`, phase=RETRY_TIMED_OUT_TREE_APPROACH,
bounded ASHES_CHOP_NORMAL_TREE attempt=2 within a fresh ~6-min window (sourceStartedAt reset),
then terminal HOLD on second failure.
