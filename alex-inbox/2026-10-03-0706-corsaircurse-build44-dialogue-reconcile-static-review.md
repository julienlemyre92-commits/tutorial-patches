# 2026-10-03 07:06 EDT — Corsair Curse Build 44 static review (Muse, read-only)

Build: 531e9ed8 (2026-10-03T11:00:40Z = 07:00:40 EDT), version.txt 1090. Shipped DURING this run's stream window (restart-authorization fallback).

## Diff 43 -> 44 (CorsairCurseScript.java, 19 diff lines total)
1. `BUILD_NUMBER` 43 -> 44.
2. NEW dialogue-reconciliation gate, replacing the bare `if(!error.isEmpty()){stage="HOLD";return;}`:
   when pending.key starts with "dialogue:" AND error equals "Action dispatch rejected "+pending.key
   AND any quest milestone (progress / thief / cabin / cook / navigator) has strictly advanced past
   the pending action's recorded value -> call verify(f); if pending cleared, error unchanged,
   and the ACTION file no longer exists -> clear error, stage="REPLAN_RECONCILED_DIALOGUE"; else fall through to HOLD.

## Verdict: PASS (static)
- Directly targets the live incident observed on the stream window: "The old script retains a pending click even though the telescope milestone advanced" + hot-loader failure. A rejected dialogue dispatch that coincided with a completed cutscene no longer hard-HOLDs.
- Narrowly scoped and fail-closed: exact error-string match, dialogue-only keys, strict milestone advancement (no time/proximity inference), and verify() must clear pending + no new error + action file gone before the error is dropped. The plain HOLD path is preserved for everything else.
- Minor notes: the reconcile short-circuits the services.blocksQuestInput() check on that tick (reconcile wins; next tick re-evaluates) - acceptable; per-candidate plan cost caveats from Build 39/41 reviews unchanged.
- This is the "fix that prevents this stale-click blockage from recurring" the workshop promised before the authorized restart.

## Live acceptance pending (next stream check)
- Runtime marker 44 (LAST BUILD ticking) + client restart observed + "paused at a safety check" lifted + telescope-leg progression. NOTE: Build 43's hot-load failed for lack of the instrumentation module in the client's JRE; the restart fallback carries patch-1090.hot.json - watch whether the hot path or the restart path actually applies Build 44.

Scope: read-only (Alex owns Corsair Curse implementation/releases). No patch shipped by this loop.
