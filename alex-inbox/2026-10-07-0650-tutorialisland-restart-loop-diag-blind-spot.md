# Read-only review note — Tutorial Island: ~2-min client restart loop + diag-tail blind spot (2026-10-07 ~06:50 EDT)

From the review loop, own GitHub API reads (contents API, screenshots path, 2026-10-07_06-45-41 / 06-47-48 / 06-49-46 MINING diag tails).

## Verified facts

1. **Restart loop: new client sessions at 06:41:22, 06:43:37, 06:45:41, 06:47:48, 06:49:46 EDT.**
   Each new diag file opens with the "Build 390: [pin] window pinned" / "REBUILD: WorldModel registered" startup cascade.
   Cadence is ~2 minutes per session. Last observed *gameplay* lines anywhere: 06:44:59
   ("Skipping mining/smelt -- already have bar/dagger", then the 'mine-anvil-click' /
   'mine-esc2' no-progress cycle from the 06:46 note). Nothing after.

2. **The diag evidence is structurally blind during this loop — banner-dump swallows the 250-line tail.**
   Line counts per uploaded 250-line tail:
   - 06-45-41: 217/250 lines are startup-banner lines ("Build N - ..." changelog history)
   - 06-47-48: 234/250 banner
   - 06-49-46: 230/250 banner
   Every session re-prints the full Build-78→418 changelog banner at startup, so each
   post-restart upload carries **zero runtime lines from the current session** — even a
   healthy session's first upload would look empty. The banner cascade ends with
   "Build 82: cleaned up 6 old screenshots" and no RUNNING_BUILD/stage/tick lines follow
   in the uploaded window.

## Correlation lead (not proof)

The restart loop began ~40s after the anvil-click/esc2 no-progress loop was observed
(06:44:06 → 06:44:59; next restart 06:45:41). Candidate mechanisms: a tick-thread
RuntimeException in the smith arc, or the 'mine-esc2 EXHAUSTED' arm/exhaust cycle
tripping a stall detector that restarts the client. The current diag files cannot
show this — the exception text would either be buried above the 250-line window or
lost when the session dies before an upload.

## Suggested fixes (Alex's call)

a. Cap the startup banner: print the full changelog once per *process* (or write it to
   a separate file) and print only a one-line "RUNNING_BUILD=N stage=X" marker per tick/
   per upload. The per-session re-dump is what blinds the tail.
b. Re-diag the one-line build/stage marker at the *end* of each upload cycle so the tail
   always carries current-session state even right after a restart.
c. Add an ESC ownership gate in the smith arc (from the 06:46 note): do not arm
   'mine-esc2' while a 'mine-anvil-click' outcome is still pending verification — the
   hammer arc already got this in Build 354; the smith arc needs the same.
d. Consider a tick-heartbeat line every ~30 ticks in MINING so a tick-thread death is
   distinguishable from a clean Supervisor restart in the next session's file.

## Verification pending

- Next-session diag: whether gameplay lines ever follow the banner in a session that
  survives >2 min.
- Stream check flagged by the review loop to look for crash dialogs / launcher cycling
  (screenshot PNG uploader has been dead 6+ days; no visual corroboration available).

No source touched, no commands issued — read-only note only.
