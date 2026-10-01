# Doric Build 51 (patch-692) -- review verdict: PASS (1 minor)

- **Reviewer:** Muse review-loop (read-only; Alex owns implementation/releases)
- **Reviewed:** patch-692.zip (Build 51, commit 6f73f4f1 13:17:43Z "Doric Build51: deposit the gold bar before closing the bank" -- same message as Builds 49/50, judged by bytes, not message), vs patch-691.zip (Build 50). version.txt=692 at review time.
- **Method:** git-blobs API (Accept: application/vnd.github.v3.raw) download of patch-692.zip (823,957 bytes) + patch-691.zip (823,711 bytes) + patch-692.hot.json + patch-691.hot.json + patches/doricsquest-51.jar (31,516 bytes) + patches/doricsquest-50.jar (31,269 bytes). unzip entry lists IDENTICAL (215 entries, 212 net/-rooted; version.txt=692 in and out of the zip). javap -p -c diff of DoricsQuestScript 691->692. Chain-of-custody: patch-692.hot.json sha256 ce841afe7d8852a8b119095f5f7f65fc2d19b96c840938fa5cec94bfb42052b2 == patches/doricsquest-51.jar == the 4 script classes inside patch-692.zip (jar==zip byte-identical). runtimeBuild()=51, BUILD_NUMBER init and banner bipush 50->51: honest bump, no lying banner. NO stale-class reship; no patch reuse; no overwrite.

## Delta: Build 50 -> Build 51 -- low-value capacity-pot drop to free a slot for iron

One functional change, in `getMaterials()` (the missing-material flow), inserted as a NEW intercept BEFORE the unchanged bankChecked/close-bank region:

- Guard: missing index==2 (iron ore) AND Frame.mining>=15 AND counts[2]<NEEDED[2] AND Rs2Inventory.isFull() AND itemQuantity(1931)>0.
- Action: `Rs2Inventory.interact(1931, "Drop")` (item 1931 = empty Pot, low-value capacity item), then `set("DROP_LOW_VALUE_CAPACITY_POT", frame, 9000L, 1931, countBefore)` + log `[DoricsQuest] DROP_LOW_VALUE_CAPACITY_ITEM item={} countBefore={} decision=FREE_ONE_SLOT_FOR_IRON`.
- Dispatch-reject path: HOLD with "Low-value capacity pot Drop dispatch rejected; preserve other items" -- preserves other items, never deposits valuables.

Why it matters: Build 50's tail of this method HOLDS with "Inventory full; preserve existing valuables, no auto-deposit" whenever the inventory fills with training-tin pots, so iron gathering dead-ended on a pot-full inventory. This intercept runs first and sacrifices one ~1-gp pot to free a slot for the quest iron.

Correctness checks:
- **No livelock:** one drop clears isFull (28->27); drop requires a pot present and full inventory; each episode drops at most ~1 pot before isFull is false. The 9000ms proof suppresses re-entry while the drop settles.
- **Ordering preserved:** Build 49's intercept-first ordering (deposit gold bar before close-bank) and Build 48's livelock fix are byte-intact -- the new block precedes, never reorders, them. The pre-existing trainMining intercept (index==2 && (mining<15 || trainingTinOwned<=0)) is also intact.
- **Frame/LoginFrame/Pending:** normalized javap diff = 0 lines each -- line-number-table churn only (same as the 690->691 review). Plugin class byte-change is the same churn (banner version lives in the Script class).
- **All new API calls verified present:** Rs2Inventory.isFull/itemQuantity/interact(int,String) are the same signatures the class already called in Build 50 -- no new NoSuchMethodError surface.

## Findings

- **[m] `LOW_VALUE_CAPACITY_POT_ID` declared but never read.** The field exists (private static final int) but the drop logic uses inline `sipush 1931` twice; javap shows exactly one reference (the declaration). Harmless dead field, but if Alex intended single-sourcing of the id, the two inlines should use the field. Not a defect -- flagging so it doesn't drift from the literal later.

## Verdict: PASS

Small, targeted, exception-safe ordering: the drop is guarded on all four conditions (right item index, mining level, still-needed iron, full inventory with a pot), dispatch failure holds without touching other items, and the 9000ms proof bounds re-entry. Markers honest (RUNNING_BUILD=51). Live verification pending (screenshot feed dark since 2026-09-30 17:44:02 EDT -- no DORIC_* frames ever): acceptance lines are `[DoricsQuest] DROP_LOW_VALUE_CAPACITY_ITEM item=1931 ... decision=FREE_ONE_SLOT_FOR_IRON` and `DROP_LOW_VALUE_CAPACITY_POT` in diag/status. Per the standing rule, Alex's direct in-chat runtime reports supersede cron conclusions.

Nothing shipped (review-only; Alex's releases).
