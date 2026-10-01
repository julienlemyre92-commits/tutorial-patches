# Review verdict: Prince Ali Rescue Build 76 (patch-769) — PASS WITH FINDINGS

Reviewed 2026-10-01 ~18:46 EDT by Muse (read-only; Alex owns implementation/releases).

## Custody — CLEAN
- hot.json sha256 `520e29012c7f3668...` == princealirescue-76.jar bytes (git-blobs raw API)
- patch-769.zip: 237 entries, net/-rooted (+ benign root version.txt, META-INF); in-zip version.txt=769
- 19 script classes byte-identical zip<->script-jar; 3 plugin classes byte-identical zip<->plugin jar; BUILD_NUMBER=76 via javap
- Single-purpose commit (cbf24a00a1, 22:25:46Z); version.txt=769 on repo at review end

## Delta 75→76 (+14 decompiled lines, script only)
- GE quote source: `Rs2GrandExchange.getPrice(id)` (legacy, unavailable) → `Rs2GrandExchange.getRealTimePrices(id)` returning installed WikiPrice model; quote = ceil(buyPrice × 1.10), 0 when null/non-positive; diag line GE_QUOTE logs observedBuy/fetchedAt/ceiling/cap-remaining
- Reload-migration retry gate: on restore, a "GE quote unavailable/above 1000gp cumulative cap id=333 quote=0" hold with sourceItem==333 and geStage==PREPARE is cleared once → RETRY_FOOD_QUOTE_WITH_WIKI_API with sourceStartedAt reset, giving the new wiki-price path a chance
- API verified against installed microbot-base.jar: getRealTimePrices(int) exists, returns models.WikiPrice with public int buyPrice and long timestamp — no Build-517-class issue (call sits in the established tick-thread/client-thread-dispatch GE flow)

## Findings
- [LOW new] The wiki-quote retry gate is reload-time only (fires in the restore path, not per-tick) and trout-333-specific. In practice it works because Alex ships constantly — each hot-load re-attempts the quote — but during a quiet shipping period a fresh quote failure parks in a terminal hold. The parallel quote-unavailable holds for WOOL / SOFT_CLAY / 1929 / BRONZE_BAR have no retry gate at all and would need their own builds.
- [MEDIUM conditional CARRIED] banked bronze pickaxe 1265 still never withdrawn (withdrawFinishedIfBanked covers only {wig, paste, key, print}; nothing routes a banked 1265 back to inventory).
- [MEDIUM conditional CARRIED] B70 furnace confirmation: handleKeyFurnaceConfirmation holds "Unexpected furnace confirmation question: <text>" on any non-exact question text — including the empty-getQuestion() case — permanent HOLD on first Yes|No sighting when the question read is empty.
- [LOW carried] source-review README is a stale Build-1 template (doc-only).

## Live acceptance
PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; no live URL. Verdict from static review only.
