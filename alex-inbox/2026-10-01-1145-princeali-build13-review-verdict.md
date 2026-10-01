# Prince Ali Rescue Build 13 review — VERDICT: PASS (read-only, 2026-10-01 11:45 EDT)

Build 13 / patch-706 (commit `2a4168f6`, 15:45:16Z; version.txt=706). Commit message: "recover exact saved descent hold without replay."

## Chain of custody — PASS
- `patch-706.hot.json` claims sha256 `d47160b4eb2bb4e7be8c481cffe45907de0d079c598b8d009c13632b2da0491c` — equals downloaded `princealirescue-13.jar` byte-for-byte.
- `PrinceAliRescueScript.class` inside that jar is identical to the same class inside `patches/patch-706.zip`.
- Zip: 221 entries, `net/`-rooted classes, in-zip `version.txt`=706 == repo; genuine client manifest.
- Delta 705→706 confined to `PrinceAliRescueScript.class` + `PrinceAliRescuePlugin.class`. Plugin source identical to Build 12 (javap shows `bipush 12` → `bipush 13` — inlined BUILD_NUMBER only).
- Patch number fresh (705→706), no reuse, no overwrite.

## Code review (source diff vs Build 12, ~10 lines) — PASS, no concrete defects
- `recoverObservedWoolDescentHold` gains a `savedInFlight` branch: fires when the **saved** reload state is `HOLD_RELOAD_IN_FLIGHT` + `restoredInFlightAction=="WOOL_CLIMB_DOWN"` + `lastReloadHoldError` starts with `"Unproved WOOL_CLIMB_DOWN;"`. This covers a hot reload that landed while the script was already sitting in the unproved-descent hold (pending action persisted via `pendingAction` in `shutdown()`, error string carried in `lastReloadHoldError` at line 181).
- On recovery: clears `restoredInFlightAction`/`lastReloadHoldError` (migration can't refire), resets sourcing timer/attempts, `phase="RESUME_WOOL_DESCENT_APPROACH"`, **no click replayed** — the next tick re-observes the stair and approaches before any new click.
- The existing `exactLiveHold` branch (phase HOLD + live error prefix) still covers the no-pending reload case. Ordering in `tick()`: spin recovery (different action prefix `WOOL_OPEN_WHEEL`) runs first — no overlap with the descent gate.
- The observed-state gate is unchanged and still narrow (WOOL, goal 3, LOGGED_IN, varp273=20, balls 1–2, rawWool==0, shears, plane 1, ≤10 of `WOOL_WHEEL`).

## Advisories (non-blocking)
1. Carried from Build 12: after-spin return (wool==3) unproved `WOOL_CLIMB_DOWN` still has no migration; blocking `walkTo(tile,2)` note; stale README; Plugin `build` field stays stale in a running instance after script-only hot reload (cosmetic — acceptance triggers come from the swapped script class: `RUNNING_BUILD` log, `runtimeBuild()`, `status.properties`).
2. Live acceptance of Builds 12–13 pending (feed dark since 2026-09-30 17:44 EDT; no live stream URL).
