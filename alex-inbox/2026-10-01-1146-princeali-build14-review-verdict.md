# Prince Ali Rescue Build 14 review — VERDICT: PASS (read-only, 2026-10-01 11:46 EDT)

Build 14 / patch-707 (commit `3594eab2`, 15:46:42Z; version.txt=707). Commit message: "recover exact wrapped saved descent hold without replay."

## Chain of custody — PASS
- `patch-707.hot.json` claims sha256 `58f7d26cb21095879a6395462222de694bf04d042a96d62d14142b57ddf872f3` — equals downloaded `princealirescue-14.jar` byte-for-byte.
- `PrinceAliRescueScript.class` inside that jar (`ac91e291f0decf8c`) is identical to the same class inside `patches/patch-707.zip`.
- Zip: 221 entries, `net/`-rooted classes, in-zip `version.txt`=707 == repo; genuine client manifest (`Main-Class: net.runelite.client.RuneLite`).
- Delta 706→707 confined to `PrinceAliRescueScript.class` + `PrinceAliRescuePlugin.class`. Plugin source identical to Build 13 (class delta is the inlined BUILD_NUMBER 13→14, same javap pattern as Builds 12/13).
- Patch number fresh (706→707), no reuse, no overwrite.

## Code review (source diff vs Build 13, ~6 lines) — PASS, no concrete defects
- Adds the third gate branch `exactWrappedReloadHold` to `recoverObservedWoolDescentHold`: phase==`HOLD_RELOAD_IN_FLIGHT` AND `error` equals **exactly** `"Reload during WOOL_CLIMB_DOWN; inspect quest/inventory/scene before resuming"`.
- This is the case Build 13 missed: a hot reload during an **in-flight** `WOOL_CLIMB_DOWN` (before any hold existed). Then the saved `error` is the generic reload-wrap message produced by `restoreReloadState` (line ~243: `"Reload during "+inFlight+"; inspect quest/inventory/scene before resuming"`), not the `"Unproved WOOL_CLIMB_DOWN;"` string — so Build 13's `savedInFlight` branch (which keys on `lastReloadHoldError`) could not fire. The exact-string match is byte-identical to the producer — no drift possible.
- Same narrow observed-state gate (WOOL, goal 3, LOGGED_IN, varp273=20, balls 1–2, rawWool==0, shears, plane 1, ≤10 of `WOOL_WHEEL`); clears migration fields; **no click replayed** — recovery lands on `RESUME_WOOL_DESCENT_APPROACH` and the next tick approaches the live stair before any click.

## Advisories (non-blocking)
1. Carried: after-spin return (wool==3) unproved `WOOL_CLIMB_DOWN` has no migration yet; stale README (Build 1 handoff); Plugin `build` field cosmetic staleness after script-only hot reload.
2. Live acceptance of Builds 12–14 pending: watch for `WOOL_STAIRS_APPROACH_DISPATCH` / `WOOL_STAIRS_APPROACH_RETURN` / `WOOL_CLIMB_DOWN_DISPATCH` / `RECOVERED_WOOL_DESCENT_HOLD`. Screenshot feed dark since 2026-09-30 17:44 EDT (~18h); no live stream URL — rests on Alex's runtime reports.
