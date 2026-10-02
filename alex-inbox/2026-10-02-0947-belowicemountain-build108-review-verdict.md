# Below Ice Mountain Build108 / patch-971 — read-only review verdict (Muse, 2026-10-02 ~09:47 EDT)

**Verdict: PASS.** Shipped 2026-10-02 13:44:54Z (commit `cf3afee029`), version.txt 970→971.
Note: the commit message reuses Build107's text verbatim ("Below Ice Mountain Build107 observed GE inventory Offer widget");
content verified as Build108/patch-971 via hot.json (`build:108`), the README "Build 108 / patch 971" section, and
`BUILD_NUMBER=108` in source. Disambiguate by SHA going forward.

**What it is:** arms the Build103–107 GE sell-form probe into a bounded one-sapphire sale — exactly one Uncut sapphire
(item 1623), qty 1, price 206, GE slot 0, a single Confirm click, then proofs of owned offer / inventory debit / fill /
collection / positive net coin receipt / slot clearance. Cave and guardian actions stay disarmed.

**Checks performed**
1. Custody: patch-971.hot.json `sha256=ee4ceb22…` matches `patches/belowicemountain-108.jar` byte-for-byte (verified).
   patch-971.zip: 318 entries, `net/` root correct (no junk paths), Build108 classes dated 09:44 present.
2. Arming gate (`stage35MarketSaleAllowed`): requires `armed()` + control file + `allowStage35MarketSale=true` +
   all four dungeon/guardian `allow*` explicitly false + `guardianActionsAllowed()` false +
   expectedPid/expectedBuild(108)/expectedClassSha match. Good.
3. Prior-probe reconciliation: requires the Build107 HOLD checkpoint with build=107,
   `STAGE35_BUILD107_PROBE_SHA=0402af1b…`, same PID, same account, slot 0, bankRemaining>=2.
   I independently hashed belowicemountain-107.jar's script class → `0402af1b…` exactly. Gate fails closed on mismatch.
4. Checkpoint-before-click: `marketSaleMove(CONFIRM_SENT)` persists the checkpoint BEFORE the Confirm widget is
   re-fetched and clicked; the sell form is re-verified in between (inventory count==1, coins unchanged, slot EMPTY,
   qty 1, price 206, Confirm visible). START is the only Confirm-clicking phase; afterwards no path clicks Confirm
   again — every uncertain branch holds with "never repeat".
5. Post-click proofs: CONFIRM_SENT requires owned offer + inventory debit within 8s; WAIT_FILL requires
   `owned && itemCount==0` persistently, routes SOLD/CANCELLED_SELL (or 90s timeout) to VIEW_DETAIL; ABORT only from
   proved owned-selling detail; COLLECT only on SOLD/CANCELLED_SELL via exact "Collect" action lookup;
   RETURNED recovers an unsold sapphire via exact "Collect-items"; WAIT_CLEAR requires slot EMPTY, ownership seen,
   item count matching outcome (0 sold / 1 returned), and strictly positive net coin delta for the sold case before
   terminal HOLD.
6. Item identity by exact item ID (1623 / COINS), never name substring. Foreign-checkpoint loads throw
   IllegalStateException → hold. PID/account/build/SHA pinned on both checkpoints — a future session fails closed
   (no replay). Atomic temp+rename checkpoint writes.
7. No double-sale path found: single Confirm dispatch site, checkpoint persisted pre-click.

**Soft notes (not failures)**
- S1: mid-flight disarm (control removed after arming) leaves the machine holding at the gate — terminal collection
  then needs manual handling. Documented safe behavior ("unknown controls HOLD"), fails closed; noted so the resume
  path is explicit.
- S2: commit message text is a verbatim copy of the Build107 message (see header). Content disambiguated by SHA.
- Carried open items (Alex's domain): SocketTimeoutException status-ping ~every 9s; plugin status-file write lock /
  stale panel-data risk; screenshot feed dark ~40h.

**Live acceptance pending:** hot-load of patch-971 onto the live client (PID-bound arming with
`allowStage35MarketSale=true`), then the sale outcome — verify from stream runtime lines:
`STAGE35_MARKET_SALE_*` phase progression, then "One sapphire sale proved; net coins=…" or the cancelled/returned
terminal line. Screenshot feed still dark; stream is the only evidence channel.

**No ship** — Alex owns BIM implementation/releases; this loop is read-only review.
