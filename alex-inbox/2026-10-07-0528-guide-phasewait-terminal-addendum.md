# GUIDE Talk-to PHASE-WAIT — terminal: ~10-min wait, no re-dispatch, ended only by restart (2026-10-07 05:28 EDT)

Follow-up to `2026-10-07-0517-guide-talkto-phasewait-addendum.md`.
Evidence: `screenshots/2026-10-07_05-25-24_GUIDE_diag.txt` (commit 277d5116) and
`screenshots/2026-10-07_05-26-24_GUIDE_diag.txt` (commit 3379ae72), 250 lines each, verified.

## New facts

1. **The PHASE-WAIT never self-recovers.** It ran 05:15:17 -> ~05:25:22 (~10 min) with ZERO
   dialogue proof and ZERO Talk-to re-dispatch — `Build205 SKIPPED (no re-dispatch)` every
   tick, alternating only with `Build 300: creatorBinding: Gielinor Guide NPC visible -- NOT
   creator, keeping GUIDE`. This answers the 05:17 addendum's question 1: there is NO
   re-dispatch timer; the wait is unbounded within a session and ended only when the process
   exited (watchdog restart ~05:25:22-05:25:23, Supervisor relaunched).
2. **Runtime build correction.** The fresh session banner (05:25:23-05:25:24) shows the actual
   running build is **RUNNING_BUILD=418 (patch-415)**. The `Build 292/300/318` strings in the
   diag are stale method-level labels, not the runtime build (judge from the active startup
   marker). Note the banner also carries the classic mismatch line
   `Build 395: STARTUP -- RUNNING_BUILD=397 (patch-394)` — the max marker (418) is
   unambiguous.
3. **The new session is back in the same stall.** From 05:25:24 through 05:26:24 the tick is
   the identical PHASE-WAIT/creatorBinding alternation, no dialogue proof. Whether the new
   session re-dispatched Talk-to is unknowable — the ~150-line banner crowded the first ~10s
   of runtime lines out of the 250-line upload window.
4. **Companion PNGs still absent from the repo** (`Screenshot saved: ..._05-25-24_GUIDE_auto.png`
   and `..._05-26-24_GUIDE_auto.png` logged, never uploaded) — PNG uploader still broken;
   coverage remains diag-only.

## Still open for Alex

- PHASE-WAIT needs a bounded budget + re-dispatch or re-sync escape (per the 05:16 note's
  suggestion). The single Talk-to click at 05:14:52 evidently never landed (NPC at dist 4-5,
  step-to-adjacent unverified in the visible lines), and the ~10-min wait proves the failure
  is silent and permanent within a session.
- Verified tally stays 10 quests / 31 QP (this GUIDE session is the fresh-account Tutorial
  Island client, not the main account; main front last seen Build 660, DS1 COMPLETE panel /
  intermission).

Loop is read-only; no changes made.
