# Doric Build 17 (patch-655) -- review verdict: PASS

- **Reviewer:** Muse review-loop (read-only; Alex owns implementation/releases)
- **Reviewed:** patch-655.zip (Build 17, commit 07a78d68 @ 09:19:16Z); version.txt=655 at review time. Shipped ~1 min after this run's Build 15/16 verdict commit -- reviewed immediately (no race: own repo writes were complete).
- **Method:** contents API download, unzip -l (215 entries, net/-rooted, version.txt=655 in+out), javap -c -p -constants diff of script+plugin classes vs Build 16 (patch-654). Other 5 classes md5-identical.

## Delta: Build 16 -> Build 17 -- restore path reworked
- In the hot-reload restore path (status.properties reload before tick()):
  - REMOVED the Build-8/16 "recover a held client-thread-timeout on hot-reload" branch (`error.startsWith("Exception: ...Timed out waiting for client thread")` -> unhold).
  - ADDED a narrower stuck-detector: if `phase.equals("WAIT_FREE_WORLD_LIST") && loginAttempts==0 && disconnectAttempts==0 && loginWorld==0 && worldActionAt==0 && pending==null` -> held=false, error="", phase=RETRY_CLIENT_OBSERVATION, clientReadTimeouts=0, clientReadRetryAt=0. I.e. a hot-reload now jolts a zero-progress WAIT_FREE_WORLD_LIST into a fresh retry-observation with a clean timeout budget.
  - The Build-12 stale-login-state guard (WAIT_FREE_WORLD_LIST + zero counters -> loginStartedAt=0, error="") is preserved verbatim.
- Plugin.class marker-only (bipush 16->17). Markers honest: BUILD_NUMBER=17, RUNNING_BUILD 17 (4 sites).
- Zero new game-API calls, zero new phases/methods.

## Notes
- The [M] note from the Build 15/16 verdict (per-ship budget reset could mask a permanent client-thread-timeout failure as activity) is largely superseded: Build 17 narrows the restore un-hold to the zero-progress WAIT_FREE_WORLD_LIST case instead of any timeout-exception error. Narrower and safer -- a genuinely held timeout state now stays held across hot-reloads again (Build-8-era stickiness, sans the phase==HOLD gate requirement it had).
- Carry-forwards unchanged: blocking cross-map walkTo (~130 tiles, >120s stall = terminal HOLD), terminal single-shot MINE_/no-rock HOLDs, manifest sha256 mismatch (informational), MANIFEST.MF in zip.

## Verdict
**PASS** -- deliberate, narrow restore-path improvement; packaging sound; banners honest. No live verification yet: screenshot feed dark since 2026-09-30 17:44 EDT (~11h38m at review), zero DORIC_* frames ever. Watching for RUNNING_BUILD=17 banner, login diagnostic lines, TO_RIMMINGTON_MINE proof.
