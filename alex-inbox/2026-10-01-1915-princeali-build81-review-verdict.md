# Prince Ali Rescue Build 81 Review Verdict — 2026-10-01 19:15 EDT

**Verdict: PASS WITH FINDINGS**

- Build: 81 | Patch: 774 | Commit: 4bb4f24e ("native bounded GE food buyer with durable action checkpoints and verified bank fund")
- Scope: read-only review (Alex owns implementation/releases). No source edited, nothing compiled, nothing uploaded.

## Custody (byte-level, verified this run)

- `patches/patch-774.zip` (blob 15f080aa…): 246 entries, root is `net/` (only META-INF/ + version.txt outside it), in-zip `version.txt` = `774`. Entry count ~240 as expected; no too-deep `runelite/...` entries (the 2026-09-29 patches 341/342 failure mode is absent).
- `patches/princealirescue-81.jar` (blob 7557f360…): sha256 `6c56a9dc2419892b013fa909ebf8e2d44cd9da7a0d5c3447540360d8d833bf61` — EXACT match to `patch-774.hot.json` (`"build":81,"patch":774`).
- Script classes zip↔jar: 28/28 `PrinceAliRescueScript*` classes byte-identical (sha256 per class). Zip additionally carries `PrinceAliRescuePlugin.class`, `PrinceAliRescuePlugin$1.class`, `PrinceAliRescueConfig.class` (script jar is hot-reload artifact, plugin classes ship in `princealirescue-plugin-81.jar`, not in review scope). Plugin.class differs per build in bytes but `javap -constants` output is identical across B81–B86 (compile-order artifacts only); Config.class constant (`8026216c…`) across all six.
- `BUILD_NUMBER`: `javap -constants` on `PrinceAliRescueScript` = 81; RUNNING_BUILD log line, `getBuildNumber()` return, and `status.properties` writer all say 81 — banner is honest.
- Commit 4bb4f24e is single-purpose: 9 files (hot.json, patch zip, script jar, plugin jar, 3 source-review .java, README.md, version.txt).
- Baseline for delta: `princealirescue-80.jar` (blob 963e7c8f…). Repo `version.txt` now reads 780 (post-ship state); `princealirescue-87.jar` already exists in repo — beyond this review's scope.

## Semantic delta (B80 → B81; 1021 changed CFR lines)

New feature: native GE food buyer for cooked Trout (item 333) via new nested `QuestGeBuyer` state machine + outer `nativeGeBuy(frame)` driver.

- Outer: when `sourceItem==333 && geStage=="NATIVE_GE_BUY"` and within 8 tiles of `BankLocation.GRAND_EXCHANGE`, drives funding (bank withdraw of `nativeGeReserve` coins, bounded by `nativeGeCap = 1000 - geReservedTotal`), then delegates to the buyer. Durable checkpoints: `ge-buyer.checkpoint` written atomically (tmp + ATOMIC_MOVE) before every phase transition; state also persisted in `status.properties` (nativeGeCheckpoint, nativeGeUiAction, nativeGeReserve/Cap/Quantity, nativeGeReserved).
- Buyer phases: QUOTE (wiki price fetch) → OPEN/WAIT_OPEN (GE clerk "Exchange") → SLOT (first EMPTY slot with "Create Buy offer" action) → WAIT_SEARCH → TYPE_SEARCH (types "Trout") → WAIT_RESULT (exact-result click) → WAIT_ITEM → QUANTITY/WAIT_Q_INPUT/WAIT_Q_VALUE → PRICE/WAIT_P_INPUT/WAIT_P_VALUE → CONFIRM (re-verifies item/qty/price/coins/EMPTY slot/30-min quote freshness) → WAIT_OFFER → WAIT_FILL (90 s → ABORT) → OPEN_OWNED/WAIT_OWNED → COLLECT_ITEM/WAIT_ITEM_COLLECT → COLLECT_COINS/WAIT_COIN_COLLECT (refund proof vs `coinsAfterPlace`) → WAIT_SLOT_CLEAR → COMPLETE/CANCELLED/HOLD. "No repeat" discipline: every click is followed by a proof wait; failures HOLD with a named reason instead of re-clicking.
- Quote price = max(high+2, ceil(high*5/4)) from prices.runescape.wiki, rejected if stale (>30 min) or non-positive; buy only if `price*qty <= cap`.
- Ownership guards: slot-index bounds, foreign-offer detection (`isForeign` → HOLD), `owned()` requires item+qty+price match, slot-clearance and refund verification before COMPLETE.

## API / thread-safety verification (against installed `/home/hatch/workspace/microbot-base.jar`)

- All new calls resolve: `Rs2Widget.isWidgetVisible(int,int)`, `Rs2Widget.clickWidget(Widget)`, `Rs2Npc.getNpc(String)`, `Rs2Npc.interact(Rs2NpcModel,String)`, `Rs2Bank.openBank()/hasWithdrawAsItem()/setWithdrawAsItem()/withdrawX(int,int)`, `Rs2Keyboard.typeString/enter`, `Microbot.getClientThread().invoke(Supplier)` (blocking), `Microbot.getVarbitValue`, `Client.getWidget/getGrandExchangeOffers/getVarcIntValue/getItemContainer(InventoryID)`, gson `JsonParser` (present in jar).
- Buyer `frame()` snapshot runs inside `clientThread.invoke(...)` — no off-client-thread game-state reads (Build-517 lesson honored).
- Actions go through `Rs2*` helpers (established client-thread-marshalling pattern).

## FINDINGS

- **[NEW] MEDIUM — B81 quote path broken at ship (superseded by B82).** B81's `fetchPrice()` used `java.net.http.HttpClient`; the portable JRE lacks the `java.net.http` module (established fact from the bot-command channel), and B82's commit message cites a live "loopback failure". Worse, the QUOTE branch catches only `Exception`, so a `NoClassDefFoundError` (an `Error`) would have escaped `tick()` and killed the script tick thread. Fixed by B82's URLConnection rewrite; no action needed, recorded for the record.
- **[NEW] MEDIUM — silent idle livelock in `nativeGeBuy` COMPLETE branch (persists through B86, marked [carried] there).** `case COMPLETE: if (frame.count(333) < this.sourceGoal) return;` — if trout count is below goal when the buyer completes (e.g. food eaten mid-sequence), the method returns with no HOLD, no log, no state change; the next tick re-enters the same branch forever. Reachability is low (buyer verifies the inventory delta at WAIT_SLOT_CLEAR on its own fresh frame), but the failure mode is exactly the HANG RULE's nightmare: zero diagnostics, infinite idle. One-line fix available (HOLD or re-init buyer).
- **[carried] MEDIUM — poison soft-lock in B77/B79 damage-free retreat gate.** Untouched by B81–B86.
- **[carried] MEDIUM — stranded probe dumps under `%USERPROFILE%/.runelite/princealirescue/`.** Untouched (B85 adds one more dump file, but it overwrites rather than appends).
- **[carried] MEDIUM — banked bronze pickaxe (1265) never withdrawn.** Untouched.
- **[carried] MEDIUM — B70 empty-`getQuestion()` furnace confirmation HOLD.** Untouched.
- **[carried] MEDIUM — B74 3-tile vs 10-tile reloaded-walk arrival gate.** Untouched.
- **LOW [NEW] — terminal HOLD on fresh-quote-over-cap.** `QUOTE` holds permanently when `price*qty > cap`; prices move, so a re-quote-later loop could recover. Design-consistent ("no blind offers"), noted only.
- **LOW [NEW] — `restore()` IAE and `saveNativeGeCheckpoint()` ISE propagate uncaught.** Both are self-written-state paths (atomic writes), so corruption is unlikely; noted only.
