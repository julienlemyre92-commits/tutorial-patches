# Prince Ali Rescue Build 12 review — VERDICT: PASS (read-only, 2026-10-01 11:43 EDT)

Build 12 / patch-705 (commit `0108a814`, 15:43:33Z; version.txt=705). Commit message: "approach live stair before descending."

## Chain of custody — PASS
- `patch-705.hot.json` claims sha256 `29e8a9448311d350317fe50bda89058ce901ad9ebb5efa97b38016607420a5fd` — equals downloaded `princealirescue-12.jar` (30,708 bytes) byte-for-byte.
- `PrinceAliRescueScript.class` inside that jar (`6d582f17f95b199e`) is identical to the same class inside `patches/patch-705.zip`.
- Zip: 221 entries, `net/`-rooted classes, in-zip `version.txt`=705 == repo. `META-INF/MANIFEST.MF` is the genuine client manifest (`Main-Class: net.runelite.client.RuneLite`), not a jar-generated default.
- Delta 704→705 confined to `PrinceAliRescueScript.class` + `PrinceAliRescuePlugin.class`. The Plugin change is only the inlined build marker (`bipush 11` → `bipush 12` in javap; `Plugin.build = PrinceAliRescueScript.BUILD_NUMBER` is a compile-time-constant inline) — Plugin.java source is identical between Build 11 and 12.
- Patch number fresh (704→705), no reuse, no overwrite.

## Code review (source diff vs Build 11, ~60 lines) — PASS, no concrete defects
- New `descendForWool(f, reason)` helper replaces the two inline stair-16672 blocks (after-spin return, sheep-field return):
  - Finds stair 16672 within 8 of `CASTLE_STAIRS_FIRST`; HOLD with exact reason if absent.
  - **Live-stair verification**: gets the LIVE stair's tile + action list; HOLDs if the tile is null or the stair lacks the `Climb-down` action (`Rs2GameObject.hasAction`) — guards against clicking a stair variant without the action. Diag includes id/name/tile/actions/player.
  - **Approach before descending**: if player distance > 2, logs `WOOL_STAIRS_APPROACH_DISPATCH` and issues blocking `Rs2Walker.walkTo(tile, 2)` (WorldPoint+radius overload); re-captures player pos fresh from `Rs2Player.getWorldLocation()` post-walk (stale-pos trap avoided) and HOLDs with exact cause if arrival (>2) fails. Then sets bounded `WOOL_APPROACH_DOWNSTAIRS` pending (15s).
  - If already within 2: logs `WOOL_CLIMB_DOWN_DISPATCH` (id/tile/name/actions/reachable/player/distance), clicks `Climb-down` → `WOOL_CLIMB_DOWN` pending (12s); rejection HOLDs with full detail.
- New `WOOL_APPROACH_DOWNSTAIRS` proof predicate (`proved()`): plane==1 && within 2 of target. After proof, `phase="PROVED_..."`, pending cleared, and the next tick re-enters step routing from observed state (plane 1 → `descendForWool` → click). Two-stage flow is sound.
- New `recoverObservedWoolDescentHold` hot-reload migration: clears the exact `"Unproved WOOL_CLIMB_DOWN;"` HOLD only under the narrow observed-state gate (phase HOLD, sourceItem=WOOL, sourceGoal=3, LOGGED_IN, varp273=20, balls 1–2, rawWool==0, shears present, plane 1, ≤10 of `WOOL_WHEEL`). Resets timer/attempts, sets `RESUME_WOOL_DESCENT_APPROACH` (diag marker only — routing stays observed-state driven), **no click replayed**. Error prefix matches the actual timeout format at line 324 (`"Unproved "+pending.action+"; ..."`). Mirrors Build 9/10/11 migration discipline.

## Advisories (non-blocking)
1. The migration gate requires wool balls strictly < goal (1–2, sheep-field return case); an unproved `WOOL_CLIMB_DOWN` after the spin return (wool==3) has no migration yet — narrow residual gap, consistent with the narrow-gate discipline.
2. New blocking `Rs2Walker.walkTo(tile,2)` on the tick thread: goal is a live-observed stair tile (not a door tile), arrival verified with fresh pos — no unreachable-freeze risk in the normal case.
3. New API uses (`isReachable()`, `getObjectComposition()`, `getName()`) compiled clean — signatures exist; off-tick-thread `getWorldLocation()` matches the established `observe()` pattern (no new risk).
4. `source-review/princealirescue-build12/README.md` still the Build 1 handoff — stale, packaging hygiene only.
5. Live acceptance pending. New lines to watch: `WOOL_STAIRS_APPROACH_DISPATCH`, `WOOL_STAIRS_APPROACH_RETURN`, `WOOL_CLIMB_DOWN_DISPATCH`, `RECOVERED_WOOL_DESCENT_HOLD` (screenshot feed dark since 2026-09-30 17:44 EDT; no live stream URL; rests on Alex's runtime reports).
