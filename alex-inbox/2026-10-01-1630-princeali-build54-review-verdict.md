# Muse read-only review verdict — Prince Ali Rescue Build 54 / patch-747

**Verdict: PASS — no blockers. No concrete defects found.** (Read-only review; no code shipped.)

Reviewed 2026-10-01 ~16:30 EDT via repo sources: `source-review/princealirescue-build54/` (script source), `patches/patch-747.zip` (blob b336161489a07a60159df83453d828cd032848c2, 221 entries, `net/`-rooted, no junk paths), `patches/princealirescue-54.jar` (sha256 ff578cc9cdb792f3f20ac71a1eac6aa19d17393da88209e05722152f2c4e42a0 == patch-747.hot.json — custody link verified), in-zip `version.txt`=747, compiled class confirms `BUILD_NUMBER = 54` (banner and code agree — no lying-banner issue). In-zip `PrinceAliRescueScript.class` is byte-identical to the in-jar one (401388e425b593abe466582426e817292573491ae87725c35b061de257e79890).

## What Build 54 changes (vs Build 53)

Source diff is 81 lines, all in `bronzeBarShantayTick` + one new recovery:

1. **New recovery `recoverObservedShantayApproachAfterReload`** (wired into the recovery chain right after `recoverObservedUnavailableBronzeBarQuote`). Fires only on the exact `HOLD_RELOAD_IN_FLIGHT` + `Reload during WALK_TO_SHANTAY_TRADE_NPC;` + `restoredInFlightAction=="WALK_TO_SHANTAY_TRADE_NPC"` combination, with a tight observed-state gate: `sourceItem==BRONZE_BAR, sourceGoal==1, sourceAttempts==0, geStage=="BAR_SHANTAY_SHOP"`, logged-in, quest IN_PROGRESS, varp273==20, plane 0, within 8 tiles of the Shantay waypoint, KEY_PRINT>0, bar==0, coins 1..50 (the coin cap still enforced). If a live trade-capable Shantay NPC is same-plane and within 6 tiles of the player, it clears the hold WITHOUT replaying the walk (`held=false; error=""; pending=null; restoredInFlightAction=""`, phase=`RECOVERED_SHANTAY_APPROACH_AFTER_RELOAD_WITHOUT_REPLAY`) and lets the normal stage plan take over. If the NPC isn't live/close, it returns false and the diagnostic HOLD is preserved — no silent stall.
2. **NPC-approach restructure**: the npc-tile null/plane check is hoisted before the distance check, and distance is computed from the captured `npcTile` (`f.pos.distanceTo(npcTile)`) instead of `npc.getDistanceFromPlayer()`. Behaviorally equivalent, but strictly safer — the old code could proceed to `Rs2Shop.openShop` with a null NPC tile when the NPC was within 6; the new code holds with a diagnostic. Log fields now record the tile used for the walk.

Verified correct by source read:

- The new recovery cannot fire on the bronze-bar-quote HOLD (different error string) and cannot overlap the Build 52 CONTINUE recovery (different `restoredInFlightAction`); single-shot semantics hold because the phase gate (`HOLD_RELOAD_IN_FLIGHT`) is cleared on fire, mirroring the Build 51/52 pattern.
- Radius gates (8 tiles to waypoint, 6 tiles to NPC) avoid the exact-tile brittleness carried as a minor note on Builds 51/52.
- `pending=null` on recovery is correct: the walk is deliberately not replayed, so the stale walk pending proof must not linger.
- After recovery, `geStage` is still `BAR_SHANTAY_SHOP`, so the next tick re-enters the normal NPC-approach → shop-open path with the NPC already within 6 — no action gap.

## Non-blocking notes for Alex

- The walk target is a captured NPC tile and NPCs wander; the distance gate re-evaluates every tick, so worst case is a re-walk, not a stall. No change needed.
- Carried from Build 53: if the private server prices the bronze bar above 50 coins, the single buy dispatch fails server-side and the pending proof HOLDs for review — bounded, no overspend, but a price check against the cap is still worthwhile.
- `source-review/princealirescue-build54/README.md` still carries the old handoff doc with no Build 54 change note (same hygiene note as Build 51) — the source diff remains the record of intent.

Live verification still outstanding: feed dark since 2026-09-30 17:44 EDT; Build 54 (like Builds 3–53) has no live runtime evidence yet.

Scope note: read-only review per standing rule — Alex owns implementation and releases.
