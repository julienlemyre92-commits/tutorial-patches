# Muse read-only review verdict: Below Ice Mountain Build106 / patch-969 (PASS, INFO only + one carried-forward soft finding)

- Commit: eb27289e9df0081595a8648dd4241882af7c9bd4 (2026-10-02 09:26:25 EDT) — version.txt 968→969, no version reuse, patch-969.zip is a new file in the commit (not overwritten).
- Custody: AIR TIGHT. hot.json sha256 (dec19645…) == downloaded belowicemountain-106.jar bytes; in-zip version.txt = "969"; patch-969.zip net-rooted, 315 entries (same count as patch-968; zero added/removed); entry diff vs patch-968 = ONLY BelowIceMountainScript + 9 nested classes (recompiled) + version.txt — every other class byte-identical; BUILD_NUMBER=106 verified in the compiled class (javap); belowicemountain-106.jar is the script-only hot artifact.

## Mechanism (read-only fail-closed, as documented in README)

- New `marketProbeReadOnlyGeInventory(f, account)`, called from `stage35MarketProbe()` AFTER the scene gate (`hold("Stage35 market probe scene/inventory changed; no input")` on gate fail) and before `marketProbeResumeObservedSlot`/`marketProbeLoad`; any exception → hold. Call-site diff is 36 lines total — surgical.
- Recognition gate: recognizes ONLY the checkpoint file `STAGE35_MARKET_PROBE_CHECKPOINT` with schema=MARKET_PROBE_1, matching account, matching current PID, build="105", phase=SELL_FORM_SENT, and exact class SHA `1b45c6f2…f9` — independently verified as the sha256 of Build105's BelowIceMountainScript.class from patch-968.zip. Anything else → returns false (inert, probe continues normally).
- Re-proves before dumping: `marketProbeSellForm()` visible (client-thread invoke) AND exactly 1 carried UNCUT_SAPPHIRE; otherwise `IllegalStateException` → caught → hold.
- Dump: `marketProbeDump("BUILD105_GE_INVENTORY_RECOVERY", 0)` reads widgets 467 (installed WidgetID: GRAND_EXCHANGE_INVENTORY_GROUP_ID) and 149 group:0 plus up to 28 dynamic children — ALL inside `Microbot.getClientThread().invoke((Supplier<String>)->…)` (line 2306-2307). The new diagnosis path does NOT exercise the off-thread actions read flagged in the Build103/105 verdicts.
- Ends at terminal `hold("Build105 GE inventory Offer action absent; Build106 read-only widget dump complete")` — zero clicks, no Offer click, no sale. Read-only as the commit message says.
- Scene gates intact (LOGGED_IN, questStage==35, overworld prep area, not in cave instance, hp>0, inventory loaded, sapphire<=1, no unresolved trout checkpoint, plus the explicit stage35MarketProbeAllowed() control-file gate).

## Finding (SOFT, carried forward from Build103/105 — not new)

- `marketProbeInventorySlot()`: `Rs2Inventory.getActionsForSlot(slot)` is STILL called OFF the client thread (Build58-class). That is the OLD path that observed the "Offer absent" result this build is diagnosing; the new Build106 dump path itself is client-threaded. Worst case unchanged: null/stale actions → bounded HOLD. Suggested fix stands (move the actions read into the client-thread invoke).

## Live acceptance

- Pending: Build106 hot-load, NEW diag evidence — dump label `BUILD105_GE_INVENTORY_RECOVERY` and hold reason "Build105 GE inventory Offer action absent; Build106 read-only widget dump complete". Client was observed DISCONNECTED at login screen ~09:12 EDT; reconnect/hot-load state unknown (screenshot feed dark since 2026-09-30 17:44 EDT ~39.7h; stream is the only live source).

VERDICT: PASS (INFO only + one carried-forward soft finding). Alex owns arming/hot-load/live test.
