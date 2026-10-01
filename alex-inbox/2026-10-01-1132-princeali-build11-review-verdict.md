# Prince Ali Rescue Build 11 review — VERDICT: PASS (read-only, 2026-10-01 11:32 EDT)

Build 11 / patch-704 (commit `3638d64c`, 15:31:24Z; version.txt=704). Commit message: "return descent before gathering remaining wool."

## Chain of custody — PASS
- `patch-704.hot.json` claims sha256 `8415acd0ca49a005180faf995003950d2670238665c954b316ea259ef8a5c23a` — equals downloaded `princealirescue-11.jar` (29,791 bytes) byte-for-byte.
- `PrinceAliRescueScript.class` inside that jar (`e34ceea072b1bb099c944351e586fd16a9ca6e92c3ae8c97c4f5db7ca508b12a`) is identical to the same class inside `patches/patch-704.zip`.
- Zip: 221 entries, `net/`-rooted classes, in-zip `version.txt`=704. `META-INF/MANIFEST.MF` is the genuine client manifest (`Main-Class: net.runelite.client.RuneLite`), not a jar-generated default — safe against the manifest-overwrite fault.
- Patch number fresh (703→704), no reuse, no overwrite.

## Code review (source diff vs Build 10, 33 lines) — PASS, no concrete defects
- Wool-gather step now handles plane 1: if the player is upstairs while wool is still owed, it finds castle stairs object 16672 within 8 of `CASTLE_STAIRS_FIRST`, clicks `Climb-down`, and records a bounded `WOOL_CLIMB_DOWN` pending proof (12s). Rejected click / missing stair HOLD with an exact reason — consistent with the established one-action-then-proof pattern. Plane≠0/1 still HOLDs with the fresh position. This fixes the terminal "Unexpected position/plane while gathering wool" HOLD from Build 10's wheel run (player left upstairs at plane 1 beside the wheel).
- New hot-reload migration `recoverObservedWoolGatherPlaneHold` clears only that exact saved HOLD reason, and only when fresh observed state proves the situation: phase HOLD, exact old error string (fresh position stringified), sourceItem=WOOL, LOGGED_IN, varp273=20, balls < goal, raw wool == 0, plane 1, ≤8 tiles of `WOOL_WHEEL`. On resume it resets the sourcing timer/attempts and goes to `RESUME_WOOL_GATHER_BY_DESCENT`; the next tick must freshly observe the stair and click — no click is replayed. Mirrors the Build 9/10 migration discipline (narrow gate, no replay).

## Advisories (non-blocking)
1. `source-review/princealirescue-build11/README.md` is stale — it is still the Build 1 handoff doc and documents nothing through Build 11. Packaging hygiene only, no game effect.
2. Migration error-string equality relies on `WorldPoint.toString()` being stable — same pattern as Builds 9/10; acceptable.
3. Live acceptance is pending Alex's runtime report. New lines to watch: `RECOVERED_WOOL_GATHER_PLANE_HOLD` and `WOOL_CLIMB_DOWN` → plane-0 transition proof, plus the Builds 7–10 lines still outstanding (WALK_RETURN, PROVED_WOOL_SPIN_BY_INVENTORY, RECOVERED_WOOL_SPIN_HOLD).
