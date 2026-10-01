# Muse read-only review verdict — Prince Ali Rescue Build 53 / patch-746

**Verdict: PASS — no blockers. No concrete defects found.** (Read-only review; no code shipped.)

Reviewed 2026-10-01 ~16:25 EDT via repo sources: `source-review/princealirescue-build53/`, patch-746.zip (blob 7266440470db9c18, 221 entries, all 6 PrinceAliRescue classes present with `net/` prefix, no junk paths), `princealirescue-53.jar` (sha256 matches patch-746.hot.json `ece4abea...f578`), compiled class confirms `BUILD_NUMBER = 53` (banner and code agree — no lying-banner issue).

## What Build 53 changes (vs Build 52)

Replaces the unavailable-GE-quote bronze-bar branch with a bounded local Shantay shop purchase (`bronzeBarShantayTick`), plus a narrow recovery (`recoverObservedUnavailableBronzeBarQuote`) from the exact Build-52 `GE quote unavailable/above 1000gp cumulative cap id=2349 quote=0 deficit=1 reserved=0` HOLD.

Verified correct by source read:

1. **Ordering is deadlock-free.** `prepare()` only routes to `BAR_SHANTAY_SHOP` when `KEY_PRINT>0` (soft clay → Keli first, per the existing stage order). The tick's `KEY_PRINT<=0` HOLD precondition is consistent with that order; on hot-reload with a stale persisted stage it holds with a clear diagnostic rather than acting.
2. **Coin cap is enforced before any purchase.** Carried coins >50 → banked down to exactly 50 (`BAR_SHANTAY_STAGE_COINS` pending proof requires bank open, before>50, after==50). Zero coins → HOLD ("refusing an unpriced attempt"). Inventory full → HOLD.
3. **Live stock gate precedes the buy.** `Rs2Shop.hasMinimumStock(BRONZE_BAR,1)` must be true (5s grace for the stock list to populate) before `buyItem(BRONZE_BAR,"1")` is dispatched — so the unverified "bronze bar from Shantay" assumption on the private server cannot cause a blind purchase; a missing stock item holds with the full stock list in the diagnostic.
4. **Single-attempt, proof-gated purchase.** `sourceAttempts>0` → HOLD (no retry, deliberate). Pending proof requires bar 0→1, debit 1..carried, before-coins 1..50.
5. **Recovery is narrowly scoped.** Only fires on the exact old HOLD error string + `sourceItem==BRONZE_BAR, sourceGoal==1, geStage=="PREPARE", bankInspected`, logged-in, varp273=20, plane 0, KEY_PRINT>0, bar==0. Resets `sourceStartedAt` (fresh 6-min window) and attempt counters.

## Non-blocking notes for Alex

- If the private server prices the bronze bar above 50 coins, the single buy dispatch will fail server-side and the pending proof will HOLD for review — bounded, no overspend, but worth a price check against the cap.
- `nearestBank` from the Shantay area resolves to Al Kharid (~45 tiles); fine, just slow staging if coins need capping.
- Waypoint (3304,3124,0) + "Trade-capable Shantay" NPC: live presence/action unverified — the code holds cleanly on miss (`getNearestShopNpc("Shantay",true)==null` → diagnostic HOLD), so nothing stalls silently.
- Live verification still outstanding: feed dark since 2026-09-30 17:44 EDT; Build 53 (like Builds 3–52) has no live runtime evidence yet.

Scope note: read-only review per standing rule — Alex owns implementation and releases.
