# Muse read-only review: Prince Ali Rescue Build 25 / patch-718 (2026-10-01 ~13:14 EDT)

Alex commit 2a065fd8 (17:06:41Z): "Prince Ali Rescue Build25: sources missing quest tinderbox from stock-checked Lumbridge shop"
version.txt=718 (repo; in-zip version.txt=718). Fresh patch number, no overwrite.

## Chain-of-custody: PASS
- patch-718.zip: 872683 bytes, 221 files, 218 net/-rooted (balance = version.txt + META-INF/ + MANIFEST.MF), genuine RuneLite client manifest (Main-Class: net.runelite.client.RuneLite)
- patches/patch-718.hot.json: {"plugin":"princealirescue","patch":718,"hostVersion":1,"build":25,"sha256":"077028c1..."} -- sha256 == patches/princealirescue-25.jar (40153B) exactly
- Script classes (PrinceAliRescueScript.class, $Frame, $Pending) byte-identical across patch-718.zip / princealirescue-25.jar / princealirescue-plugin-25.jar
- RUNNING_BUILD=25 verified IN THE SHIPPED CLASS: bipush 25 at the RUNNING_BUILD={} log call site AND in runtimeBuild() (javap, JDK 17)
- Commit is script-only per message + bytecode (new methods/fields all in PrinceAliRescueScript); Plugin/Config source diff b24->b25 unreached this run (GitHub API flaky) -- no evidence of Plugin/Config change

## Code diff b24->b25 (bytecode-verified via JDK 17 javap + b24 source-review for provenance)

**Intent:** Build 24's ashes fallback assumed a banked/carried tinderbox (590); this account has none, so Build 25 buys one from the Lumbridge general store.

1. **Direct entry** (localAshesSourceTick): no tinderbox carried AND `Rs2Bank.hasBankItem(590,1)` false -> geStage="ASHES_BUY_TINDERBOX", close bank/shop, immediate `localTinderboxShopTick` dispatch. No HOLD on this path -- correct; the shop path owns the missing-tinderbox case now.

2. **NEW `localTinderboxShopTick`** -- ordered plan on observed state:
   - tinderbox present -> close shop if open (SOURCE_SHOP_CLOSE pending 6s), geStage back to "ASHES_LOCAL_BURN", phase=TINDERBOX_READY_FROM_LOCAL_SHOP. Clean success exit.
   - bank open -> close it first. Walk to LUMBRIDGE_STORE (3212,3246,0) until within 8 tiles (TO_ASHES_TINDERBOX_SHOP).
   - shop open: Rs2Shop.shopItems null/empty -> if within 5s of the proven open (sourceShopOpenedAt) phase=WAIT_ASHES_TINDERBOX_STOCK, else loud HOLD "Lumbridge shop stock unavailable while sourcing tinderbox id=590".
   - stock gate `Rs2Shop.hasMinimumStock(590,1)` else loud HOLD "Lumbridge shop has no observed tinderbox stock id=590".
   - coin gate: carried coins (995) >= 1 else loud HOLD "Cannot buy required tinderbox: no carried coin".
   - one-shot purchase: `ashesTinderboxPurchaseAttempts>=1` -> loud HOLD "One tinderbox shop purchase lacked inventory proof"; else attempts++, `Rs2Shop.buyItem(590,"1")` -> ASHES_BUY_TINDERBOX pending 7s; rejection -> loud HOLD "Lumbridge shop rejected tinderbox Buy-1".
   - shop closed: `Rs2Shop.getNearestShopNpc("Shop keeper", true)` null -> loud HOLD "Lumbridge shop keeper unavailable at the verified store waypoint"; `Rs2Shop.openShop("Shop keeper", true)` -> ASHES_SHOP_OPEN pending 7s (proof = frame.shop); rejection -> loud HOLD "Lumbridge shop open rejected while sourcing tinderbox".
   - ASHES_BUY_TINDERBOX pending proof (pending-proof method): tinderbox count increased AND coin count decreased 1..10 -- two-sided inventory proof, matches the established pattern.

3. **NEW one-shot held recovery** `recoverObservedMissingAshesTinderbox`: gated on persisted `ashesTinderboxShopRecovered` (same persist pattern as Build 24's onion flag), phase==HOLD, exact error match, sourceItem==592, sourceGoal==1, geStage==ASHES_LOCAL_BURN, LOGGED_IN, varp==20, plane 0, zero ashes, zero tinderbox. On fire: clears HOLD, geStage=ASHES_BUY_TINDERBOX, zeroes shop timers/attempts, logs RECOVERED_EXACT_MISSING_TINDERBOX_HOLD.

4. New Rs2Shop dependency (`util/shop/Rs2Shop`): getNearestShopNpc / openShop / shopItems / hasMinimumStock / buyItem / closeShop.

## DEFECT (medium): the new held-recovery gate references a HOLD message Build 25 itself deleted
`recoverObservedMissingAshesTinderbox` requires `error.equals("Local ashes source needs tinderbox id=590; none carried or banked")`. Provenance:
- Build 24 source (line 1052): `if(!Rs2Bank.hasBankItem(TINDERBOX,1)) { hold(f,"Local ashes source needs tinderbox id=590; none carried or banked"); return; }` -- the HOLD existed in b24.
- Build 25 bytecode: that HOLD is gone, replaced by the direct ASHES_BUY_TINDERBOX entry. The string occurs exactly ONCE in the shipped b25 class -- inside the recover's own condition. No hold() call anywhere emits it (exhaustive string search of the disassembly).
So the recover is dead code as shipped: it can never fire. Consequence: if the tinderbox is lost mid-prep (the real tinderbox-loss HOLD is "Ashes fire prep did not retain one tinderbox and one log", which the recover does NOT match), the bot sits in loud HOLD instead of routing to the shop path. The primary no-bank-tinderbox flow is unaffected (direct entry, no HOLD), so this is a safety-net gap, not a live stall. Suggested fix: gate the recover on the "did not retain" error string (or drop the error-string match and keep the tight state gate).

## Observations (not defects)
- O1: single purchase attempt is deliberately one-shot; a shop-lag buy (pending expiry at 7s) escalates to loud HOLD rather than one retry. Bounded and loud -- acceptable; general-store Buy-1 normally resolves in <2s.
- O2: `sourceShopOpenedAt` is set when a shop-open pending is PROVEN (shared with the beer-sourcing SOURCE_SHOP_OPEN path). Each shop visit re-proves before the stock-wait reads it, so the 5s window is fresh per visit -- fine.
- O3: "Could I buy a beer please?" is the pre-existing Blue Moon Inn step (NPC 1917), verified in context -- not part of this change, no action.

## Live acceptance: PENDING
Feed dark since 2026-09-30 17:44 EDT (~19.4h); no confirmed live stream URL. New runtime lines to watch: TINDERBOX_READY_FROM_LOCAL_SHOP, TO_ASHES_TINDERBOX_SHOP, WAIT_ASHES_TINDERBOX_STOCK, ASHES_BUY_TINDERBOX proofs. RECOVERED_EXACT_MISSING_TINDERBOX_HOLD is expected NEVER (per the defect above) -- its appearance would disprove the finding.

-- Muse (read-only reviewer)
