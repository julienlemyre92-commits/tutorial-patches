# Prince Ali Rescue Build 72 — read-only review verdict (Muse)
Date: 2026-10-01

## Header
- Build: 72 (in-script `BUILD_NUMBER = 72` confirmed via `javap -constants` on shipped class)
- Patch: 765 → `patches/patch-765.zip` (version.txt = 765, net/-rooted, 231 entries)
- Commit: `bce02e8411` "Prince Ali Rescue Build72: separate travel coin costs from observed shop purchase accounting"
- Commit scope: single-purpose — patch-765.hot.json, patch-765.zip, princealirescue-72.jar, princealirescue-plugin-72.jar, source-review/princealirescue-build72/*, version.txt. Intervening commit between B71 and B72 was only an alex-inbox/seen.log update (no code).

## Custody — PASS
- `patch-765.hot.json`: sha256 `88fa32eb57caccf9812d042d8c0b67a05903958ccc1d9033b9bc1bfc67ec2331`, build=72.
- Script jar `patches/princealirescue-72.jar` fetched via git-blobs API with `Accept: application/vnd.github.v3.raw`: sha256 matches hot.json EXACTLY.
- patch-765.zip: 231 net/-rooted entries; all 13 `.../microbot/princealirescue/PrinceAliRescueScript*.class` classes byte-identical zip↔script-jar.
- Compiled classes contain the new code string `SOURCE_TRAVEL_COIN_DELTA` — no stale classes.

## Delta summary (B71 → B72, source diff, 2 code hunks + BUILD_NUMBER)
The whole delta is the travel-vs-purchase coin accounting fix:
1. Tick loop, proved-pending path: when a `WALK_TO_SOURCE_*` leg resolves and the source item count did NOT change during the leg, the script logs `SOURCE_TRAVEL_COIN_DELTA id=.. coinsBefore=.. coinsAfter=..` and re-baselines `sourceLastCoins = f.count(COINS)` — "establishing purchase baseline". Travel tolls/debits are no longer priced as shop purchase costs.
2. `restoreReloadState`: one-shot rescue for the exact observed hold `error.equals("Source gain/coin delta invalid id=1917 gain=1 spent=12 total=0")` while `sourceItem==BEER`. It re-observes on the client thread via `getClientThread().invoke(Supplier<Frame>)` (correct thread pattern), and only if logged in, varp==20, within 10 tiles of `BLUE_MOON_POS`, holding 1 beer and 28 coins, it reconciles: `sourceLastCoins=28, sourceLastCount=1, sourceSpent=12`, un-holds, phase `RESUME_SOURCE_PRICE_PROOF`.

## Findings
- [MEDIUM, conditional, CARRIED] Empty furnace Yes|No `getQuestion()` → terminal hold, no recovery (unchanged by B72). LEFT OPEN.
- [MEDIUM, conditional, CARRIED] Banked bronze pickaxe 1265 never withdrawn → terminal hold in the soft-clay route (unchanged by B72). LEFT OPEN.
- [LOW, new] B72 restore rescue hardcodes `count(COINS)==28` and `sourceSpent=12`: it only rescues the single observed coin snapshot. If a retry's tolls land differently (different coin total), the exact-match guard fails and the HOLD persists. Narrow but correctly conservative (never fires on a different state).
- [LOW, new/watch] `SOURCE_TRAVEL_COIN_DELTA` re-baseline fires on the proved-pending tick, assuming all travel debits have landed. A toll/boat charge landing a tick AFTER the baseline is set would still be mislabeled as a purchase cost (bounded by the existing `unitCap` gain check, but worth watching in diag).
- [LOW, CARRIED] B68 second-respawn held-requirement near-unreachable; B67 partial-set direct-loot 5-item hardcode; dead-code Shantay resume log; stale source-review README template (byte-identical B70–B72). All LEFT OPEN, untouched.
- Note: B71's new findings (stale "Al Kharid source" hold message, stricter DEPOSIT_UNNEEDED proof) carry forward unchanged into B72.

## API / thread-safety
No new microbot API calls in this delta beyond B71's set. The restore-path re-observe correctly uses `getClientThread().invoke(Supplier<Frame>)` (blocking, client-thread execution; RuneLite's ClientThread.invoke is re-entrant-safe), so no B517-class violation. The `this::observe` method reference on the Supplier is sound.

## Verdict: PASS WITH FINDINGS
No blocking defect. The accounting split is the right fix shape and is correctly gated; the two carried MEDIUM-conditionals remain open (out of this delta's scope); new findings are LOW/watch items.
