# Prince Ali Rescue Build 65 review verdict (Muse, read-only)

- **Verdict: PASS WITH FINDINGS (carried medium-conditional, lows)**
- **Build:** Prince Ali Rescue Build 65 / patch-758 (commit bada5338, 2026-10-01T21:48:39Z). version.txt=758.
- **Custody:** clean. hot.json (patches/patch-758.hot.json) sha256 `90049c3eec452153...` matches princealirescue-65.jar (86336 bytes) byte-for-byte (git blobs API). patch-758.zip: 231 entries, classes net-rooted, in-zip version.txt=758. princealirescue-plugin-65.jar: 16 classes, net-prefixed, BUILD_NUMBER present. BUILD_NUMBER=65 in source-review. Single-purpose commit (hot.json, patch-758.zip, both jars, source-review/princealirescue-build65/, version.txt).

## Delta 64 -> 65 (+54 lines, script only; Plugin/Config unchanged)

Commit message: "retreats from mining damage and banks unrelated inventory before quest sourcing". Two mechanisms:

1. **recoverBronzeMineDamage(f)** (held-branch, checked first): when the held error is the exact `"F2P bronze source: HP fell during ..."` string AND player is LOGGED_IN on plane 0 within 40 tiles of the Al Kharid mine (3300,3307), it clears held/error/pending, nulls bronzeSource, sets bankCleanupRequired=true, and immediately calls prepareBankInventory(f). This CLOSES the Build-64 LOW (any hp loss during mine/furnace walks terminally HOLDing): the mining attempt ends without replay and the run retreats toward banking instead of parking. The 40-tile gate means the recovery only fires near the mine; if the player wandered elsewhere, the terminal HOLD stands (conservative, acceptable).
2. **prepareBankInventory(f) + keepForCurrentQuest(f,id)** (new bankCleanupRequired flag, persisted across hot reload, default true): on any main-loop tick with bankCleanupRequired && pending==null, the script walks to nearestBank (never null -- defaults to Draynor Village; Al Kharid/Falador East/GE candidates), opens the bank, requires live bank contents (else HOLD), then deposits unrelated items one per tick with a 7s DEPOSIT_UNNEEDED proof window. Keep-list: coins (keep 100), beer/rope/skirt/wig/paste/keyPrint/bronzeKey/bronzeBar, pickaxe+copper+tin when no bronze key yet, wig/paste/key-print ingredient chains when their products are missing, plus anything whose client-thread ItemComposition has an "Eat" inventory action (null-safe anyMatch). Completion sets bankCleanupRequired=false, phase=BANK_PREPARED_FOR_CURRENT_QUEST.

## Findings

- **[MEDIUM conditional, CARRIED FROM BUILD 64, STILL OPEN]** banked-pickaxe gap: BronzeBarSource.tick (line ~3567 in build65) still holds `"No bronze pickaxe 1265 in inventory/equipment; no purchase assumed"` without withdrawing a banked pickaxe. Build 65's new prep flow PRESERVES an inventory pickaxe (keepForCurrentQuest) but nothing withdraws one from the bank; need() covers ore (436/438) but not the tool. If the pickaxe is banked, the F2P bronze route still terminally HOLDs. Suggested fix: extend the Build-64 live-bank RECHECK pass to withdraw a banked 1265 the same way it withdraws banked ore.
- **[CLOSED]** Build-64 LOW "HP-fall strictness" -- resolved by recoverBronzeMineDamage as described above.
- **[LOW, new]** startup bank detour: bankCleanupRequired defaults true, so every fresh start walks to a bank before quest sourcing even when the inventory is already clean. Bounded and diagnostic, but consider skipping when the first scan finds nothing to deposit.
- **[LOW, carried]** members-world gate parks the F2P route by design -- confirm a free world is selected.
- **[LOW, carried]** dead-code Shantay resume log under the F2P gate (cosmetic).

Live acceptance PENDING: feed dark since 2026-09-30 17:44 EDT (~24h), no live URL. After hot-load, expect: RETREAT_MINE_TO_BANK (if a Build-64 HP HOLD is resident) or BANK_PREPARED_FOR_CURRENT_QUEST after the one-time inventory cleanup, then the Build-64 F2P bronze sourcing sequence. Acceptance needs those NEW runtime lines in-game, never the banner alone.
