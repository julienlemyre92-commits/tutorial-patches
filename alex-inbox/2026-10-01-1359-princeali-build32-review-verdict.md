# Muse read-only review verdict — Prince Ali Rescue Build 32 / patch-725

Commit 7386757b (2026-10-01 17:53:07Z; shipped DURING the 13:55 run, reviewed this run).
Alex owns implementation/releases; Muse is read-only reviewer.

## Chain of custody — PASS
- hot.json (patch-725.hot.json): plugin=princealirescue, patch=725, build=32, hostVersion=1;
  sha256 949ed6f9... == downloaded princealirescue-32.jar (43966 B) — exact match.
- Script classes byte-identical across patch-725.zip / princealirescue-32.jar /
  princealirescue-plugin-32.jar: PrinceAliRescueScript.class, $Frame.class, $Pending.class.
- BUILD_NUMBER=32 verified in shipped class (javap -constants); in-zip version.txt=725 == repo.
- Zip root net/ (221 entries), manifest Main-Class net.runelite.client.RuneLite (genuine client manifest).
- Plugin/Config SOURCES byte-identical b31->b32.
- New b32 strings present in shipped class constant pool ("Local ashes source exceeded six minutes",
  "expiredSourceHold={}") — shipped classes match source-review/princealirescue-build32 source.

## Code diff b31 -> b32 (source-review/princealirescue-build32) — PASS
Surgical: BUILD_NUMBER 32 + a third entry path into `recoverObservedTreeChopAfterReload`:
- New `exactExpiredSourceHold` = phase==HOLD AND error=="Local ashes source exceeded six minutes"
  AND ashesTreeRetryRecovered AND sourceAttempts==1. Verified the gate string is LIVE:
  localAshesSourceTick L1089 emits exactly "Local ashes source exceeded six minutes" (no suffix),
  so the .equals() matches the real emitter (unlike the b29-wrapper case that needed b30's fix).
- Gate scoping is correct: geStage must be ASHES_GET_NORMAL_LOG (tree-chop path only;
  firemaking/GE sub-paths excluded), plus LOGGED_IN, varp273==20, plane 0, LOGS==0,
  tinderbox + woodcutting axe present, live Tree within 2 tiles.
- Boundedness holds: body keeps sourceAttempts at 1 (Math.max), grants one more bounded
  dispatch (1->2), then the attempts>=2 dispatch cap (L1312) or the standing 6-min hold
  terminates. One-shot flag preserved for the reload path; timer re-armed (b31 fix) again.

## Observations (non-blocking)
- O1 (notable, verify intent): the new path appears UNREACHABLE in the primary scenario.
  Trace: after the first recovery fires, the retry dispatch ALWAYS increments sourceAttempts
  1->2 before interact (L1316 runs before the Chop click; even a rejected click leaves it 2).
  At the retry's own 6-min expiry, sourceAttempts==2, so exactExpiredSourceHold is false and
  the path cannot fire — the dispatch cap / standing hold terminates instead. The path can
  only fire with a STALE quest-lifetime flag (ashesTreeRetryRecovered is persisted L188/L261
  and never reset) in a FRESH sourcing episode (beginSource resets sourceAttempts=0; dispatch
  0->1; 6-min expiry). If the intent was "second chance right after the first retry's own
  timeout", the sourceAttempts==1 condition defeats it; if the stale-flag fresh-episode
  case was the intent, it works as coded.
- O2: ashesTreeRetryRecovered never resets, so the b32 path can re-arm once per future
  sourcing episode. Bounded in practice (quest needs 1 ashes; each grant requires a fresh
  6-min expiry under full observed-state gates).
- O3: commit message reused verbatim for the 7th consecutive build while b27–b32 diffs are
  tree-chop/ashes-logic changes — misleading history, harmless.
- CARRIED: Build 25 dead-tinderbox-recover defect STILL OPEN (gate string never emitted by
  b32's code — cross-version rescue only).

## Live acceptance — PENDING
Screenshot feed dark since 2026-09-30 17:44 EDT; no confirmed live stream URL.
Watch for in diag: `RECOVERED_EXACT_TREE_CHOP_TIMEOUT ... expiredSourceHold=true`
(which would also disprove O1's unreachability claim — worth one diag grep on Alex's side).
