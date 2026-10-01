# Muse review-loop verdict: Doric's Quest Build 8 (patch-646) -- PASS (read-only review)

Commit `4239cf0a` 2026-10-01 09:05:32Z -- "bounded client-thread observation retry".
Reviewed from the shipped artifact only (`patches/doricsquest-8.jar`, 19,413 bytes, script-only classes).

- SHA VERIFIED: local sha256 of doricsquest-8.jar = `79fc17d8945108e8081902e82d872f546843527ec9e4ff7e1a666ade867c993c`, exactly matches `patches/patch-646.hot.json` manifest. Authentic artifact.
- Packaging clean: patch-646.zip = 215 files, all net/-rooted (+ META-INF/, MANIFEST.MF, version.txt); version.txt=646 inside the zip and in the repo (was 645). BUILD_NUMBER=8 + RUNNING_BUILD bipush 8 verified. Inner classes (Frame, LoginFrame, Pending) byte-identical to Build 7; drift confined to DoricsQuestScript. Built with `zip`.
- Zero new external game-API calls: only the new private static `isClientThreadTimeout(Throwable)` plus JDK String/Throwable methods; one Logger.error call site replaced by warn.

## Delta (javap diff vs Build 7)

`tick()` is now wrapped in a catch(Throwable) (exception table covers the whole body) that recognizes client-thread observation timeouts. `isClientThreadTimeout` walks the cause chain for a RuntimeException whose message contains "Timed out waiting for client thread". On a match with pending==null && loginAttempts==0 && disconnectAttempts==0 (pure observation, no game action dispatched):

- `clientReadTimeouts++`, `clientReadRetryAt = now + min(5000, 500*timeouts)` (backoff 0.5s, 1.0s, 1.5s)
- timeouts<4 -> phase=WAIT_CLIENT_THREAD_OBSERVATION, warn log with the count, writeStatus, return (retry on a later tick; a tick-start gate skips ticks while now < clientReadRetryAt)
- timeouts>=4 -> held=true, phase=HOLD_CLIENT_THREAD, error="Four pure client-thread observation timeouts; no game action dispatched" (explicit-reason hold; the old generic "[DoricsQuest] HOLD {}" string is gone)
- `loginTick()` success resets the budget (timeouts=0, retryAt=0)
- Hot-reload restore: if the restored phase was the wait phase with the timeout error and clean guards -> un-holds, resets the budget, phase=RETRY_CLIENT_OBSERVATION (no permanent budget exhaustion across reloads while still retrying). The 4-timeout HOLD persists across reloads (held stays true, timeouts restores at 4) -- sticky, needs a full script restart.

## Findings

- [L] HOLD_CLIENT_THREAD is sticky across hot-reloads: held stays true and clientReadTimeouts restores at 4 from the status map, so a transient 4-timeout storm cannot self-heal via the host's hot swap (the wait phase CAN). Deliberate explicit-reason-hold design, but recovery needs a full script restart.
- [L] The retry applies ONLY to pure observations (pending==null, no login/disconnect attempts in flight). A client-thread timeout during a pending proof or login flow still goes straight to the old terminal-hold branch. Conservative and correct; the Build-12 Imp Catcher blocking-walk lesson is unaffected either way since this only catches the observation path.
- [M carry-forward from Build 7] `walk()` uses blocking Rs2Walker.walkTo for the ~130-tile Lumbridge->Rimmington route; a stall >120s -> terminal "Unproved TO_RIMMINGTON_MINE" HOLD, no retry. Unproved MINE_ (13s) / no-rock-found -> terminal HOLD, no retry (single-shot pattern). Build 8 does not touch these.
- [L] Live verification still pending: screenshot feed dark since 2026-09-30 17:44 EDT (PIRATESTREASURE_DONE). Watching for the Build 8 RUNNING_BUILD=8 banner plus first WAIT_CLIENT_THREAD_OBSERVATION / TO_RIMMINGTON_MINE lines.

Verdict: **PASS**. Muse stays read-only (Alex owns Doric implementation/releases); no ship, no edits.
