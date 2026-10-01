# Read-only review verdict: Prince Ali Rescue Build 55 / patch-748

Reviewed: 2026-10-01 16:35 EDT (muse review-loop, READ-ONLY; Alex owns implementation/releases).
Commit: `9caa5b1eff` "Prince Ali Rescue Build55: performs one adjacent Shantay Trade retry after exact open failure" (2026-10-01T20:32:32Z). version.txt=748.

## Chain of custody — ALL PASS
- hot.json: `{"plugin":"princealirescue","patch":748,"hostVersion":1,"build":55,"sha256":"26e4cd8ef1f25e23b5be514134ef1759a21387a3ed9d31db613d6566cffbfd2b"}`; computed sha256 of patches/princealirescue-55.jar = `26e4cd8ef1f25e23...` — **MATCH**.
- patch-748.zip: 221 entries, `net/` root. Only non-net entries are `META-INF/`, `META-INF/MANIFEST.MF`, `version.txt` (benign). In-zip version.txt = 748.
- Script classes byte-identical zip-vs-hot-jar: `PrinceAliRescueScript.class`, `PrinceAliRescueScript$Frame.class`, `PrinceAliRescueScript$Pending.class` — 3/3 MATCH (sha256).
- javap BUILD_NUMBER = 55 in BOTH the zip copy and the hot jar; matches source (`public static final int BUILD_NUMBER = 55;` line 53).
- New method `recoverObservedShantayOpenHold(Frame)` present in the compiled class.

## Source diff 54 → 55 (source-review, 2736-line script)
Single focused feature: **one adjacent Shantay Trade retry after an exact open failure**.
- New persisted flag `shantayOpenRetryUsed` (saved/restored in status.properties, survives hot reload; reset when BRONZE_BAR is acquired). Single-shot by construction.
- New `recoverObservedShantayOpenHold(Frame)`:
  - Triggers on the exact HOLD `error.startsWith("Unproved BAR_SHANTAY_OPEN;")` + phase=HOLD + pending BAR_SHANTAY_OPEN, **or** the exact reload twin (`HOLD_RELOAD_IN_FLIGHT` + `error.startsWith("Reload during BAR_SHANTAY_OPEN;")` + matching `restoredInFlightAction`/`lastReloadHoldError`).
  - Tight gates unchanged in spirit: LOGGED_IN, quest IN_PROGRESS, varp==20, plane 0, KEY_PRINT>0, BRONZE_BAR==0, 0<COINS<=SHANTAY_BAR_COIN_CAP(50), geStage=="BAR_SHANTAY_SHOP", bankInspected, no dialogue/continue/options.
  - If shop is already open (fresh state): clears hold, phase `RECOVERED_SHANTAY_SHOP_OPEN_BY_FRESH_STATE`. Good.
  - Otherwise requires Shantay adjacent: nearest shop NPC id==4642, same plane, dist<=2, has a Trade action; then `Rs2Npc.interact(npc,"Trade")`, sets pending `BAR_SHANTAY_OPEN_RETRY` (12s, proof=`f.shop`). Sets `sourceShopOpenedAt`, consistent with the existing 5s staleness gates (lines 939/1018/1406/1622).
  - If retry flag already used: explicit terminal HOLD "Single adjacent Shantay Trade retry already used without shop-open proof; no repeat". If interact returns false: explicit terminal HOLD with NPC diagnostics. Both bounded — no infinite retry loop.
- Pending proof wired: `if("BAR_SHANTAY_OPEN_RETRY".equals(p.action)) return f.shop;` (line 2584).

## Defects found
**None concrete.** All paths are gated, single-shot, persisted across hot reload, and fail into explicit bounded HOLDs with diagnostic text. The adjacent-only (dist<=2) requirement plus NPC-id/Trade-action check prevents trading the wrong NPC; the coin-cap gate (<=50) is preserved.

Minor (non-blocking): the retry flag is not reset if the player leaves/re-enters the bronze-sourcing stage without ever acquiring the bar — single-shot per attempt is the deliberate design, so this reads as intended, not a bug.

## Verdict: PASS (read-only)
Live acceptance PENDING — screenshot feed dark since 2026-09-30 17:44 EDT (~22.9h); no confirmed live stream URL. Watch for new runtime lines: `SHANTAY_ADJACENT_TRADE_RETRY_DISPATCH`, `RECOVERED_SHANTAY_SHOP_OPEN_BY_FRESH_STATE`, and the retry terminal HOLDs.
