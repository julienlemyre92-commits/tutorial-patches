# Muse review verdict: Build 7 / patch-577 (read-only review, 2026-09-30 19:15 EDT)

Reviewed the shipped classes (javap bytecode diff vs patch-576). No source in the commit; binaries only.

## What Build 7 adds (verified from classes)

- `held` flag + `heldHeartbeat()`: `tick()` short-circuits to a status heartbeat while HELD —
  rewrites `~/.runelite/impcatcher/status.properties` every tick with build=7, fresh timestamp,
  pid, state=HOLD, gameState, currentWorld, position, pending, error. Warns
  `HOLD_HEARTBEAT_FAILED` if the write fails.
- `restoreHotReloadHold()`: `run()` restores `held` after hot reload from persisted proofs,
  gated on ALL of: prior status state=HOLD, pid == current process, prior build < 7 AND
  `~/.runelite/impcatcher-hot/request.properties` build == 7, prior gameState == LOGGED_IN,
  live observed position within 2 tiles on the same plane. Any mismatch logs
  `HOT_RELOAD_HOLD_NOT_RESTORED` and does NOT restore. On restore it keeps the prior
  `error` message and logs `RESTORED_HOLD build=7 priorBuild=6 pid=... error=...` at WARN.
- New inner class `HeldFrame` (gameState, world, position snapshot for the held frame).

## Verdict: no blocking defects found

The two designs directly answer the known blockers:
1. Frozen status file at HOLD -> launcher `native_status` 5s expiry -> OCR misread of in-game
   text as a login screen. The per-tick heartbeat keeps the status fresh, so that chain should stop.
2. Hot reload resetting memory-only HOLD/checkpoint flags. Restore is guarded against
   stale-process, stale-position, and logged-out states (answers the TOCTOU concern from the proposal).

## Nits for Alex (non-blocking)

1. `RESTORED_HOLD` log hardcodes `priorBuild=6` (bipush literal) instead of the parsed prior
   build — cosmetic, will mislabel a future 7->8 restore.
2. A *second* consecutive hot reload (status.properties build already 7) fails the
   `prior build < 7` gate and silently drops HOLD — it returns false before the gameState
   check, so no `HOT_RELOAD_HOLD_NOT_RESTORED` log fires on that path. Consider persisting
   `held` itself across reloads or logging the skip.

## Acceptance triggers (still pending)

- Hot-reload VERIFIED ack (runtime SHA == shipped `3521e4e46837277df6834bd0fe9174afbdb02f4fe35f226c7c8d9b78d103b1e2`, adapter VERIFIED), or
- Fresh `RUNNING_BUILD=7` marker plus `HOLD_HEARTBEAT` / `RESTORED_HOLD` lines.
- Feed still dark ~91 min (Build 7 still publishes status.properties only, no frame capture —
  the structural-dark-feed finding stands). Review loop stays blind until capture is
  restored on Alex's side or a STATUS-command forced screenshot lands.
