# GUIDE client restart ~05:32:54 EDT — cache-hash snapshot interrupt before exit (2026-10-07 05:33 EDT, read-only loop)

New diag `screenshots/2026-10-07_05-32-55_GUIDE_diag.txt` (commit bad6e1c6) covers 05:29:37 -> 05:32:55.

## Observed (all from the diag tail, verified via GitHub contents API)
- 05:29:37 -> 05:30:36: same terminal PHASE-WAIT stall as the 05:28/05:30 notes
  ("Build 292 PHASE-WAIT: Talk-to pending, waiting for standard-dialogue proof -- Build205 SKIPPED",
  alternating with "Build 300: creatorBinding: Gielinor Guide NPC visible"). No dialogue proof, no re-dispatch.
- 05:30:36: `Build 258: cache-hash modal snapshot failed (invoke:java.lang.RuntimeException: Interrupted waiting for client thread) (1/5) -- waiting`
- 05:30:36 -> 05:32:54: DIAG SILENCE (~2m18s, zero lines).
- 05:32:54: full startup banner replay + `Build 390: [pin] window pinned to fixed rect (64,32,1280,720)` +
  `REBUILD: WorldModel registered on event bus` -> client RESTARTED (Supervisor relaunch or process exit).
- Banner replay max this session: RUNNING_BUILD=418 (patch-415) — SAME jar as the 05:27:51 session.
  (First read of the diag tail only showed replay lines <=352; the full 250-line read confirms 418.
  No build change, no downgrade.)
- New session back at GUIDE via `Build 300: creatorBinding: Gielinor Guide NPC visible -- NOT creator, keeping GUIDE`.
- Companion PNG 2026-10-07_05-32-55_GUIDE_auto.png: saved locally per diag, 404 in repo (feed dark ~7d, standing).

## What is new vs the 05:28/05:30 notes
The 05:30:36 cache-hash-modal snapshot interrupt is a NEW anomaly immediately before the restart gap.
The diag contains NO exit line, so the exit cause is UNPROVEN: it could be the Build 194 logged-out
watchdog (6-min System.exit), a crash cascade from the client-thread interrupt, or a manual/Supervisor
restart. The PHASE-WAIT stall itself does not log out, so the watchdog firing would need the client to
have been logged out — the diag shows no logout line in this window either.

## Ask (Alex owns the fix)
1. Does Build 258's cache-hash-modal path have any exit/crash cascade when the client-thread invoke is
   interrupted? The interrupt -> silence -> restart sequence here is unexplained.
2. Consider a final pre-exit diag line (reason + build marker flushed to the uploader) so the loop can
   distinguish watchdog-exit vs crash vs manual restart. The PHASE-WAIT no-redispatch ask from the
   05:28/05:30 notes stands unchanged.

Read-only reviewer note; no code touched. Verified tally unchanged: 10 quests / 31 QP (DS1 panel-level only).
