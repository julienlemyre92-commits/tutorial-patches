# Muse read-only review — Below Ice Mountain Build 20 (patch-885)

Reviewed 2026-10-02 00:10-00:2X EDT. Alex owns implementation/releases; this review is read-only. No shipping action taken.

## Verdict: PASS WITH FINDINGS

## Custody: AIR TIGHT
- patch-885.zip (commit 12c22763d7, shipped 2026-10-02T04:09:36Z): 277 entries, net-rooted (274 net/ + META-INF/ + version.txt), in-zip version.txt=885 == repo version.txt.
- Versions 884->885 sequential, no reuse. patch-885.zip not overwritten. hot.json: plugin belowicemountain, patch 885, build 20, hostVersion 1.
- BUILD_NUMBER=20 in published source (source-review/belowicemountain-build20) AND in the javap-compiled class.
- 7/7 script classes byte-identical: patch-885.zip <-> patches/belowicemountain-20.jar (sha256 e9ee5cec...a319).
- Diff 19->20 confirmed surgical (~369 diff lines, all in the new MARLEY_SUPPLIES supply chain).

## Change 19 -> 20: "F2P ingredient fallback"
New stage MARLEY_SUPPLIES: when knife/bread/cooked meat are missing at stage 15-30, the bot acquires them via a four-tier fallback before giving up:
1. `supplyBank`: re-walks to Falador bank and withdraws knife/bread/cooked meat + raw beef/flour up to 3 each + up to 100 coins if below 20.
2. `supplyKitchenItem`: loots ground-spawn knife/bowl in Lumbridge Castle kitchen (3209,3214) and fills bowls at the sink.
3. `supplyShop`: trades Wydin in Port Sarim (3013,3204) and buys bread / raw beef (cap 5, needs <3) / pot of flour (cap 4, needs <3); holds with explicit messages when stock is absent.
4. `cookSupply`: cooks raw beef on the Rimmington range (9682 @ 2970,3211) and makes+bakes dough (bowl of water + pot of flour) via production-widget click.
- New Proof types SHOP_TOGGLED / PRODUCTION_OPEN / ITEM_CONSUMED; supply actions budgeted at 45 (HOLD on exhaustion); stage 15-30 stall limit extended 3->10 min while needsSupplies.
- This directly addresses Build 19's failure class: the old meat flow HOLDed "Need bread and knife for Marley; bank already checked" when bank + ground loot came up empty.

## New findings
- LOW BIM20-1: `restoreReloadState` reads `state.get("beefBought")` / `state.get("flourBought")`, but persist writes `setProperty("supplyBeefBought",...)` / `setProperty("supplyFlourBought",...)` — key mismatch means the per-ingredient buy budgets (beef<5, flour<4) reset to 0 on hot reload. LOW: the 45-action supply budget persists and still bounds total spend; worst case a reload allows 4-5 extra purchases per ingredient.
- Design risks to verify live (feed is dark ~30h; nothing runtime-verified): knife/bowl ground spawns actually exist at Lumbridge kitchen (3209,3214,9); Wydin stocks bread+raw beef+pot of flour on the F2P account's worlds; Rimmington range 9682 reachable at (2970,3211).
- No defects in issue/proof mechanics: SHOP_TOGGLED compares f.shopOpen!=before.shopOpen (Frame captures Rs2Shop.isOpen()); ITEM_CONSUMED counts raw decrease; supplyBank keeps the bank open via the modified bank-close gate while supplies are pending and supplyBankChecked is false.

## Runtime
No live verification possible: screenshot feed still dark (last frame 2026-09-30_17-44-02 EDT, ~30h). Build 20 acceptance pending new diag lines (RUNNING_BUILD=20, MARLEY_SUPPLIES / supply:* issue+PROVED lines).
