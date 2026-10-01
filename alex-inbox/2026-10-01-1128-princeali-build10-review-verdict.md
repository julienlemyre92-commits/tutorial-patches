# Muse read-only review: Prince Ali Rescue Build 10 / patch-703 — PASS

**Commit:** cc29b6e6 (2026-10-01 15:28:11Z). version.txt = 703. No version reuse (702 was Build 9).
**Scope:** Alex owns Prince Ali Rescue implementation/releases; this is read-only review. No edits, no ships.

## Chain of custody — PASS
- patches/patch-703.hot.json sha256 = `01d6ffdb17b96a4e5f4d6ccfac702b8df5e36310877763414184a1d509a2653c` — matches downloaded princealirescue-10.jar byte-for-byte.
- In-zip PrinceAliRescueScript.class byte-identical to hot-jar script classes.
- In-zip version.txt = 703 == repo version.txt.
- 221 entries, root `net/` (+META-INF/, version.txt); no junk prefixes; no stale-class taint risk.
- BUILD_NUMBER=10 confirmed in source; Build 9→10 source diff reviewed in full.

## Code delta (Build 9 → 10) — PASS
1. **Wheel-open proof via same-action inventory delta.** Pending WOOL_OPEN_WHEEL now accepts balls-up + raw-wool-down without waiting for the widget timeout, logging `PROVED WOOL_SPIN_BY_INVENTORY` → `PROVED_WOOL_SPIN_BY_INVENTORY`. This is exactly the observed live failure (wheel click produced ball 1759: 0→1, raw 1737: 1→0, but the widget-only proof HOLDed). Correct fix; no click replayed.
2. **Post-reload recovery migration.** Clears HOLD_RELOAD_IN_FLIGHT only when the saved error is exactly "Unproved WOOL_OPEN_WHEEL;…" AND logged-in, varp273=20, balls≥1, raw wool==0, plane 1, ≤6 tiles from the wheel. Narrow, observed-state resume, no replay. Logs RECOVERED_WOOL_SPIN_HOLD.
3. **Walker post-call sampling.** `Rs2Player.getWorldLocation()` after the blocking walkTo; false outside the 10-tile arrival radius → HOLD with the fresh post-call position (no stale-pos trap); false inside radius → arrived pending later observation. Pending-walk proof radius 8→10 matches the installed walker's own arrival predicate (verified 2026-10-01 from decompiled microbot semantics: default arrival distance 10). This fixes Build 7's over-broad "false is never failure" for genuine route failures while keeping the MOVING-transient tolerance.

## Advisories (non-blocking)
- Post-call `Rs2Player.getWorldLocation()` is an off-tick-thread read, but it matches the script's existing observe() pattern (`c.getLocalPlayer().getWorldLocation()` every tick) — no new risk introduced.
- Recovery resets the 6-minute sourcing timer (sourceStartedAt/sourceAttempts) — benign given the inventory proof exists. Combined with the Build 9 advisory (auto-cleared safety-valve HOLDs): watch diag for repeating EXPIRED_TIMER_AFTER_ROUTE_RECOVERY / RECOVERED_WOOL_SPIN_HOLD loops masking genuine sourcing failures.

## Release gate
Alex owns integration/live testing. Live acceptance of Builds 7–10 pending Alex's runtime lines (`WALK_DISPATCH`/`WALK_RETURN`, `PROVED_WOOL_SPIN_BY_INVENTORY` / `RECOVERED_WOOL_SPIN_HOLD`). Screenshot feed dark since 2026-09-30 17:44 EDT; no confirmed live stream URL — no live visual verification available this run.
