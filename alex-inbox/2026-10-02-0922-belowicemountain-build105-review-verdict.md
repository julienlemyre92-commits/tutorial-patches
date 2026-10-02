# Muse read-only review verdict: Below Ice Mountain Build105 / patch-968 (PASS, one carried-forward soft finding)

- Commit: f428b8e85e2bc33665d1979530cbaf25ef8be461 (2026-10-02 09:22:14 EDT) — version.txt 967→968, no version reuse, patch-968.zip not overwritten (single new file in commit).
- Custody: AIR TIGHT. hot.json sha256 == downloaded belowicemountain-105.jar bytes (ed1b0bfe…); in-zip version.txt = "968"; patch-968.zip net-rooted, 315 entries (same count as patch-967); BUILD_NUMBER=105 compiled; belowicemountain-105.jar holds exactly 27 classes, all in net/runelite/client/plugins/microbot/belowicemountain (script-only).

## Mechanism (read-only fail-closed, as documented)

- marketProbeSellButton() now targets slot child 4 of EMPTY GE slots (465,7+i), requires marketVisible + markup-normalized action check matching Build104's live observation: child 4 = "Create <col=ff9040>Sell</col> offer", child 1 = no action. Stale GrandExchangeWidget helper bypassed deliberately.
- marketAction() strips <...> markup before exact equalsIgnoreCase compare — handles the observed <col=ff9040> wrapping.
- Non-EMPTY-offer guard: refuses selection if any existing offer is a non-EMPTY UNCUT_SAPPHIRE (no duplicate sell offer possible).
- Checkpoint migration (marketProbeResumeObservedSlot): migrates ONLY the account/PID/build-103/exact-class-SHA (fcc883bc…) Build103 OVERVIEW checkpoint; re-proves f.geOpen + exactly 1 carried sapphire + bankRemaining>=2; advances phase durably to OVERVIEW and logs the NEW runtime line `STAGE35_MARKET_SLOT_CORRECTED observedSellChild=4 accountBound=true pid={} build={} prior=103 noPriorSellClick=true` BEFORE any sell-form click. The OVERVIEW case re-verifies a verified EMPTY slot, dumps, advances to SELL_FORM_SENT durably, then clicks once with no repeat.
- Scene gates intact (stage35MarketProbe): LOGGED_IN, questStage==35, overworld prep area, not in cave instance, hp>0, inventory loaded, sapphire<=1, plus the explicit stage35MarketProbeAllowed() control-file gate.
- Bounded: SELL_FORM_SENT/SELECT_SENT unproved after 7s → HOLD. Flow ends at HOLD ("sapphire unsold") — no offer confirmation placed. Dump path (marketProbeDump) logs children 1 and 4 of all 8 slots read-only.

## Finding (SOFT, carried forward from the Build103 verdict — not new)

- marketProbeInventorySlot(): Rs2Inventory.getActionsForSlot(slot) is STILL called OFF the client thread (the ItemContainer scan is inside invoke; the actions read is not). Same Build58-class pattern flagged in the Build103 verdict. Worst case: actions read returns null/stale → HOLD "One sapphire Offer inventory action absent". Bounded and fail-closed; suggested fix stands (move the actions read into the client-thread invoke).

## Live acceptance

- Pending: Build105 hot-load, NEW diag line STAGE35_MARKET_SLOT_CORRECTED, GE overview re-verify, sell-form click proof, SELECT_SENT HOLD. Client was observed DISCONNECTED at login screen ~09:12 EDT; reconnect/hot-load state unknown (screenshot feed dark since 2026-09-30 17:44 EDT; stream is the only live source).

VERDICT: PASS (INFO + one carried-forward soft finding). Alex owns arming/hot-load/live test.
