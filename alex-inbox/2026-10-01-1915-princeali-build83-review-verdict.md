# Prince Ali Rescue Build 83 Review Verdict — 2026-10-01 19:15 EDT

**Verdict: PASS**

- Build: 83 | Patch: 776 | Commit: 3bd4ca00 ("read raw inventory quantities and native offer value widgets in…")
- Scope: read-only review (Alex owns implementation/releases). No source edited, nothing compiled, nothing uploaded.

## Custody (byte-level, verified this run)

- `patches/patch-776.zip` (blob 6e35f5bf…): 246 entries, root `net/`, in-zip `version.txt` = `776`.
- `patches/princealirescue-83.jar` (blob 554eca2a…): sha256 `f6902ba9258ade7d50f786bcb6bf17bade5bec2bec8b6f2a8a3ad174a9d12585` — EXACT match to `patch-776.hot.json`.
- Script classes zip↔jar: 28/28 byte-identical. Plugin/Config as before.
- `BUILD_NUMBER` = 83 via `javap -constants`; RUNNING_BUILD log, `getBuildNumber()`, status.properties all 83 — banner honest.
- Commit 3bd4ca00 single-purpose (9 files).

## Semantic delta (B82 → B83; 45 changed CFR lines)

Inside `QuestGeBuyer.frame()` (which runs on the client thread via `invoke`):

1. `frame.inventoryItem` / `frame.coins`: `Rs2Inventory.count(id)` → new `inventoryQuantity(client, id)` reading `client.getItemContainer(InventoryID.INVENTORY)` directly and summing quantities. Same semantics, but the read now happens on the client thread with the rest of the snapshot instead of through the Rs2 helper.
2. `frame.quantityVarbit` / `frame.priceVarbit`: `Microbot.getVarbitValue(4397/4398)` → new `numericWidget(child(frame.offerRoot, 34/41))` parsing the native offer screen's quantity/price widget text (strips tags/commas/" coins", requires all-digits, returns -1 when unavailable/malformed). Commit message says this replaces "missing GE" varbits — i.e. 4397/4398 were not the right source.

## API / thread-safety verification

- `Client.getItemContainer(InventoryID)` resolves on the installed microbot-base.jar ✓. Both changes execute inside the existing `clientThread.invoke(this::frame)` — client-thread discipline preserved (this is the Build-517-safe pattern).
- `numericWidget` is pure string parsing, no game-state access — safe anywhere.
- Behavior note (benign): if child 34/41 is absent or unparseable, the value reads -1, which simply fails the `== quantity/price` shortcut and routes through the normal X-widget type-and-verify flow; `WAIT_Q_VALUE`/`WAIT_P_VALUE` then HOLD after 3.5 s if the typed value never appears. No new livelock introduced.

## FINDINGS

- **[carried] MEDIUM — silent idle livelock in `nativeGeBuy` COMPLETE branch** (B81, unchanged).
- **[carried] MEDIUMs (all five):** poison soft-lock (B77/B79); stranded probe dumps; bronze pickaxe 1265 never withdrawn; B70 empty-`getQuestion()` furnace HOLD; B74 reloaded-walk arrival gate. None touched by B83.
- No new findings. No API or thread-safety defects in this delta.
