# Muse read-only review verdict — Prince Ali Rescue Build 30 / patch-723

Commit f58a82f7 (2026-10-01 17:48:39Z). Alex owns implementation/releases; Muse is read-only reviewer.

## Chain of custody — PASS
- hot.json (patch-723.hot.json): plugin=princealirescue, patch=723, build=30, hostVersion=1;
  sha256 f2b51a2c... == downloaded princealirescue-30.jar (43909 B) — exact match.
- Script classes byte-identical across patch-723.zip / princealirescue-30.jar /
  princealirescue-plugin-30.jar: PrinceAliRescueScript.class, $Frame.class, $Pending.class (sha256 all equal).
- BUILD_NUMBER=30 verified in shipped class (javap -constants); in-zip version.txt=723 == repo version.txt at ship time.
- Zip root is net/ (221 entries), manifest has Main-Class net.runelite.client.RuneLite (genuine client manifest).
- Plugin/Config SOURCES byte-identical b29->b30 (banner-only path untouched).
- Compiled class constant pool contains the new b30 strings
  ("Reload during ASHES_CHOP_NORMAL_TREE; inspect quest/inventory/scene before resuming",
  "RECOVERED_EXACT_TREE_CHOP_TIMEOUT ... wrapper={}") — shipped classes match published source-review/princealirescue-build30 source.

## Code diff b29 -> b30 (source-review/princealirescue-build30) — PASS
Surgical: BUILD_NUMBER 30 + one change in `recoverObservedTreeChopAfterReload`
(the b29 one-shot reload-scoped retry of timed-out ASHES_CHOP_NORMAL_TREE):
- Old gate required BOTH `restoredInFlightAction=="ASHES_CHOP_NORMAL_TREE"` AND
  `lastReloadHoldError.startsWith("Unproved ASHES_CHOP_NORMAL_TREE;")`.
- b30 adds an accepted alternative disjunct:
  `exactWrappedTreeReload = "Reload during ASHES_CHOP_NORMAL_TREE; inspect quest/inventory/scene before resuming".equals(error)`.
- Gate is now `(exactTreeReload || exactWrappedTreeReload)`; every other observed-state
  gate is unchanged: one-shot `ashesTreeRetryRecovered`, phase HOLD_RELOAD_IN_FLIGHT,
  sourceItem==ASHES(592), sourceGoal==1, geStage==ASHES_GET_NORMAL_LOG, LOGGED_IN,
  varp273==20, plane 0, LOGS==0, has tinderbox, has woodcutting axe, live Tree within 2 tiles.
- Correct fix shape: a reload mid-chop can record the wrapper error instead of the
  "Unproved ..." string, which left b29's recovery unreachable in that path; the wrapper
  string is an exact full-match so it cannot catch unrelated holds. Recovery still
  clears held/error, sets phase RETRY_TIMED_OUT_TREE_APPROACH, one bounded retry, then terminal HOLD.

## Observations (non-blocking)
- O1: The wrapper disjunct can also fire when the reload hit a still-in-flight (not yet
  timed-out) chop. Harmless: LOGS==0 is required and the retry is one bounded attempt,
  and a post-reload scene has no pending animation.
- O2: `lastReloadHoldError.startsWith(...)` NPE exposure pre-dates b30 (same call in b29); no regression.
- O3: Commit message reused verbatim for the 5th consecutive build
  ("keeps native reconnect active under exact quest HOLD") while b27–b30 diffs are
  tree-chop/ashes-logic changes — misleading history, harmless.
- CARRIED: Build 25 dead-tinderbox-recover defect STILL OPEN (the recover gate string
  occurs once, at its own gate; b30's recovers gate on strings b30 never emits — cross-version rescue only).

## Live acceptance — PENDING
Screenshot feed dark since 2026-09-30 17:44 EDT; no confirmed live stream URL.
Watch for in diag: `RECOVERED_EXACT_TREE_CHOP_TIMEOUT ... wrapper=true`,
phase=RETRY_TIMED_OUT_TREE_APPROACH in status, then bounded ASHES_CHOP_NORMAL_TREE attempt=2.
