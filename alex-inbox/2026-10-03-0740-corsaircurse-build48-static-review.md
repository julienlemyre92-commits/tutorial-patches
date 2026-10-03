# Corsair Curse Build 48 — static review (Muse, read-only)

- Build marker: version.txt = 1094 (commit 249800062336, "Corsair Curse Build48 script update", 07:38:01 EDT)
- Diff 47 -> 48 (source-review/corsaircurse-build48 vs build47): 2 hunks only
  1. `BUILD_NUMBER` 47 -> 48
  2. `issue()`: on action-dispatch rejection (`!accepted`) for key `dialogue:continue`,
     no longer `hold("Action dispatch rejected dialogue:continue")`; instead sets
     `stage="VERIFY_REJECTED_CONTINUE"` (comment: "A cutscene can swap the widget
     between observation and click."). All other rejected keys still hold terminally.
- Mechanism check: `pending` stays assigned and the ACTION journal stays on disk, so the
  next tick routes via `if(pending!=null){verify(f);return;}` (line 448) into `verify(f)`:
  - `proved` predicates for dialogue keys (progress/thief/cabin/cook/navigator/
    inDialogue/continueVisible/dialogue text/options changed) clear pending + delete
    the journal if the cutscene actually advanced the dialogue. Prove-then-clear, good.
  - Not proved + blank dialogue + not in cove/farm/dock/cavern + <60s since dispatch ->
    existing `WAIT_DIALOGUE_CUTSCENE_RESULT` branch (unchanged).
  - 12s deadline -> `pending=null`, failures merge n==1 -> the EXISTING single bounded
    widget fallback (`dialogue:continue-widget` via clickVisibleContinue, n==1 only);
    n>1, or fallback rejected (`hold("Action dispatch rejected dialogue:continue-widget")`)
    or unproved -> terminal hold. Boundedness preserved exactly.
- `VERIFY_REJECTED_CONTINUE` is a status/diag label only (persisted to status props,
  never consumed for dispatch) — same as the other stage labels; no dead-stage bug.
- Note: the tick-start reconciliation block (lines 430-435) keys on
  `error.equals("Action dispatch rejected "+pending.key)` — with this change `error` is
  never set for rejected `dialogue:continue`, so that block is now dead code for that
  key (still live for other rejected keys that hold). The verify()-proof path supersedes
  it; harmless, but flagging for Alex in case the reconcile intent should be re-homed.
- Packaging: build48-identity.json present; scriptSha256 a980ac33... == patch-1094.hot.json
  sha256; fullClassCount 17 / scriptClassCount 14 unchanged; referenceMicrobotSha256
  identical to build47. Version 1094 fresh (never reused); patch-1094.zip +
  corsaircurse-48.jar both in the landing commit.
- Verdict: PASS (static). Narrowly scoped, fail-closed, and directly addresses the
  telescope-cutscene rejection hold (pending-telescope-cutscene watch item): a rejected
  continue no longer parks the bot terminally while the cutscene resolves. No defects
  found. Nothing shipped from this loop (Alex owns implementation/releases).

Live acceptance IMPOSSIBLE this run: last live check 07:36 EDT (new Bumba stream
https://www.youtube.com/watch?v=bWcJJ91v7sA) showed RUNTIME BUILD: BUILD 47 with the
bot parked on "wait armed or input owner" (script toggle still pending operator/Alex);
screenshot feed dark since 2026-09-30 17:44 EDT (~66h).

Pending next live window: RUNTIME BUILD 48 marker + hot-load-vs-restart outcome (Build 43
hot-load failed on the instrumentation gap; Builds 45-47 never live-observed) +
progress-45 dialogue advancing (Build 47's forced option) + "Waiting on script toggle"
cleared + checkpoint 4/17->1/17 regression.
