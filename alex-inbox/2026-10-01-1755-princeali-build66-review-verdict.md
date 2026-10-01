# Prince Ali Rescue Build 66 review verdict (Muse, read-only)

- **Verdict: PASS WITH FINDINGS (lows only)**
- **Build:** Prince Ali Rescue Build 66 / patch-759 (commit b9ee1d5b, 2026-10-01T21:52:38Z). version.txt=759.
- **Custody:** clean. hot.json (patches/patch-759.hot.json) sha256 `0efd44242d175375...` matches princealirescue-66.jar (88216 bytes) byte-for-byte (git blobs API). patch-759.zip: 231 entries, classes net-rooted (only root-level version.txt -- same benign pattern as prior builds), in-zip version.txt=759. BUILD_NUMBER=66 in source-review (line 68). Single-purpose commit.

## Delta 65 -> 66 (+158 diff lines, script only)

Commit message: "recovers gravestone free items with inventory proof then retreats to bank". New `handleGraveRecovery(f)` (wired right after handleMembershipPromo, before the held branch) + 4 new persisted fields (graveRecoveryStage/Target/Deadline/Expected, all exported/restored across hot reload):

- **Entry gate (tight):** only when graveRecoveryStage is empty AND held with the exact "F2P bronze source: HP fell during ..." error AND varp==20 AND no KEY_PRINT/PASTE in inventory AND within 40 tiles of (3222,3219) -- that is the Lumbridge respawn tile (3221,3218): evidence the player just died and respawned. Target = Rs2Death.getLastDeathLocation() (fallback (3296,3313), the Al Kharid mine area); held/error/pending cleared, bronzeSource nulled, stage=APPROACH.
- **APPROACH:** blocking walkTo(target,3) with run energy on, 4-min deadline. **WAIT_APPROACH:** when the grave model is within 8 tiles -> Rs2Death.openGrave(), 12s deadline. **WAIT_OPEN:** on isGraveOpen() -> snapshot free+paid items, HOLD with fee note if free list is empty (no blind empty-grave claim), else build expected inventory map and Rs2Death.lootGraveFreeItems(), 15s deadline. **WAIT_LOOT:** inventory-proof via allMatch(count>=expected) -- true free-item recovery proof, not mere looted-click; if paid items remain -> HOLD naming the fee (human decision point); else closeInterfaces and ESCAPE walking to Al Kharid bank. **ESCAPE:** on arrival within 8 of the bank -> bankCleanupRequired=true (chains into the Build-65 bank-inventory prep), stage cleared.
- **Health-gate change:** `if(f.health>=0&&f.health<25&&!bankCleanupRequired)` -- the <25% HP HOLD is now bypassed while bank cleanup is armed, so the post-HP-fall bank run is not parked. Grave recovery itself runs before the gate (runs at any HP; post-death HP is full anyway).

## API/thread-safety verification (against installed microbot-base.jar)

- Every Rs2Death call used exists with a matching signature (getLastDeathLocation, hasGrave, getGraveTimeRemaining, getGrave, isGraveOpen, openGrave, getGraveFee, getGraveFreeItems, getGravePaidItems, lootGraveFreeItems, closeInterfaces). grave.getId()/getWorldLocation() exist (latter inherited from Rs2ActorModel).
- `Rs2ActorModel.getWorldLocation()` dispatches through `Microbot.getClientThread().invoke(Supplier)` internally (verified in bytecode) -- the tick-thread calls in WAIT_APPROACH/WAIT_OPEN are thread-safe by construction; this is NOT a repeat of the Build-517 off-client-thread crash.
- `Rs2Walker.walkTo(WorldPoint,int)` and `Rs2Player.toggleRunEnergy(boolean)` signatures verified.

## Findings

- **[MEDIUM conditional, CARRIED from B64/B65, STILL OPEN]** banked-pickaxe gap: BronzeBarSource.tick still terminally HOLDs "No bronze pickaxe 1265 in inventory/equipment" without withdrawing a banked 1265; nothing in Build 65/66's bank flows withdraws it.
- **[LOW, new]** low-health gate exemption: the <25% HP HOLD no longer fires while bankCleanupRequired is true. After recoverBronzeMineDamage (alive at low HP near the scorpion mine) the bot walks to a bank unprotected; the gate re-arms when prep completes. Scoped and likely intentional -- recorded as accepted risk, not a defect.
- **[LOW, new]** second-death stall: `if(held) return true;` inside handleGraveRecovery means any deadline HOLD fired mid-recovery consumes the tick forever with held set; a second death during recovery has no path out (consistent with the script's no-retry philosophy, but the path is silent).
- **[LOW, design note]** Build 66's grave recovery is checked before the held branch, so it preempts Build 65's recoverBronzeMineDamage for qualifying deaths (varp 20, respawn-radius, key-item-free); the Build-65 bank-retreat still covers non-qualifying HP-fall HOLDs. Coherent layering, no conflict.

Live acceptance PENDING: feed dark since 2026-09-30 17:44 EDT (~24h), no live URL. After hot-load, the first observable new lines for a resident HP-fall HOLD should be GRAVE_RECOVERY_START -> GRAVE_WALK_RETURN -> GRAVE_OPEN_DISPATCH -> GRAVE_OPEN_PROVED -> GRAVE_FREE_ITEMS_PROVED -> RECOVER_GRAVESTONE_RETURN_TO_BANK -> GRAVE_RECOVERY_AT_BANK. Acceptance needs those NEW runtime lines in-game, never the banner alone.
