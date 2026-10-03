# Corsair Curse Build 46 — static review (Muse, read-only)

**Marker:** version.txt = 1092 (commit b3b194b2cf, 07:23:25 EDT) — patch-1092.zip + corsaircurse-46.jar + source-review/corsaircurse-build46/ (identity json present). Alex owns implementation/releases; no ship from this loop.

**Diff vs Build 45** (CorsairCurseScript.java, 102001 -> 102367 bytes, 2 hunks):
1. `BUILD_NUMBER` 45 -> 46 (line 68).
2. `dialogueOptions(Frame f)` override (lines 844-850): when `f.progress==35` and the observed option menu is exactly the 2-option Gnocci set {"What is the mission Francois is doing?", "Okay, thanks."} (norm-compared, same idiom as the existing progress==0 farm gate), force-select "Okay, thanks." — i.e. exit Gnocci's optional-topic loop instead of re-asking the mission question.

**Verdict: PASS (static).** Narrowly scoped and fail-closed: the predicate requires the exact progress value AND the exact 2-option set; the default switch fall-through is untouched. Worst case it never fires and the dialogue loop persists (pre-existing behavior) — no new failure mode introduced. Targets the same "conversation did not advance" incident family Build 45 addressed.

**Defects found:** none.

**Watch items carried (live verification impossible this run):**
- Stream removed by uploader ~07:08 EDT; screenshot feed dark since 2026-09-30 17:44 EDT — no live window exists to confirm RUNTIME BUILD 46 marker or the progress-35 exit.
- Build 43's hot-load FAILED (instrumentation-module gap); Build 44 needed a full restart. Build 45/46 hot-load-vs-restart untested — watch for the same failure.
- Checkpoint schema /17 with unexplained 4/17 -> 1/17 regression (07:05-07:10 EDT); "Waiting on script toggle" persisted ~07:04-07:10 — needs operator/Alex clearance.
