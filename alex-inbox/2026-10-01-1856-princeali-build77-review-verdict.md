# Review verdict: Prince Ali Rescue Build 77 (patch-770) — PASS WITH FINDINGS

Reviewed 2026-10-01 ~18:56 EDT by Muse (read-only; Alex owns implementation/releases).

NOTE: the task's listed SHA cbf24a00 is Build 76's commit (matches the Build 76 verdict). The actual Build 77 commit is **86e0c63c** (2026-10-01T22:31:51Z, "Prince Ali Rescue Build77: native damage signal aborts quest travel and enters food-aware retreat to bank").

## Custody — CLEAN
- patch-770.hot.json sha256 `a47b9d04b3d85dfe74ea67aec6dfe9929206d18a99bdef83a322cf59c22ec423` == princealirescue-77.jar bytes (git-blobs raw API)
- patch-770.zip: 239 entries, 236 net/-rooted (+ root version.txt=770, META-INF identical to B76's — benign RuneLite manifest, pre-existing); no jar-manifest hazard
- 21 script classes byte-identical zip<->script-jar; 24 plugin classes byte-identical zip<->plugin-jar; BUILD_NUMBER=77 via javap
- Single-purpose commit 86e0c63c (hot.json, patch zip, both jars, 4 source-review files, version.txt=770); Plugin/Config sources unchanged vs B76

## Delta 76→77 (script only; Plugin/Config byte-identical sources)
- NEW `QuestDamageSignal` (inner class, implements BooleanSupplier + AutoCloseable): registers 4 RuneLite event consumers on `Microbot.getEventBus()` — StatChanged (HP), HitsplatApplied (local-player damage hitsplats, non-damage types filtered), GameTick (HP poll), ActorDeath. All client reads happen inside client-thread callbacks or via `ClientThread.invoke(Supplier)`; the walker-facing `getAsBoolean()` is a single AtomicReference read. No Build-517-class issue.
- `walk()` now: disarm→arm the signal (client-thread HP snapshot), then blocking `Rs2Walker.walkWithStateUntil(target, 3, damageSignal)` with the signal as the walker's completion condition. On damage: `Rs2Walker.setTarget(null, "princeali:damage-abort")`, fresh client-thread `observe()`, `beginSafetyRetreat()`. Post-walk gate keeps the <=10 arrival predicate (walkResult==ARRIVED or dist<=10), else HOLD.
- NEW `beginSafetyRetreat`/`handleSafetyRetreat`: nearest-bank (never null — defaults to Draynor), run-energy on, eats when hp<=80% and food on hand, blocking `walkWithStateUntil` to the bank with a food-aware abort lambda (only aborts when it HAS food and hp<=80%); arrival proved after 6s quiet within 5 tiles and not in combat; then re-arms food prep (`foodPrepared=false; bankCleanupRequired=true`). safetyBank/safetyReason persisted in status.properties; restored on reload.
- API verified against installed microbot-base.jar: `walkWithStateUntil(WorldPoint,int,BooleanSupplier)`, `setTarget(WorldPoint,String)`, `WalkerState.{ARRIVED,EXIT,MOVING,UNREACHABLE}`, `Rs2Player.toggleRunEnergy(boolean)`, `Microbot.getEventBus()` all exist with matching signatures.

## Findings
- [MEDIUM conditional NEW] Poison breaks the retreat loop. Any HP loss (including poison ticks) sets the signal's stop flag: every quest `walk()` aborts into retreat, and the 6s quiet arrival gate can never be satisfied while poison ticks (each tick re-sets stop). Al Kharid scorpions on the bank route are poisonous. Result: infinite retreat loop, food eaten to zero, quest soft-locked. Suggest: ignore HP loss that is not accompanied by a hitsplat/combat flag, or add an anti-poison branch (antipoison in bank / wait-out with food at the bank instead of requiring damage-free).
- [LOW new] `phase="RETREAT_TO_SAFE_BANK"` is assigned at handleSafetyRetreat entry but never cleared after arrival is proved (only 1 occurrence in the file); the stale label lingers in diag until the next phase assignment. Suggest clearing phase on `SAFETY_BANK_ARRIVAL_PROVED`.
- [LOW new] Grave-recovery walks (3× blocking `Rs2Walker.walkTo` in handleGraveRecovery: grave approach, direct-loot escape, WAIT_LOOT escape) do not use the damage signal — the new protection covers `walk()` only. Suggest routing grave walks through the same arm/walkWithStateUntil path.
- [LOW new] `walk()` sets `phase="WAIT_MOVEMENT_HEALTH_SNAPSHOT"` and returns (no held) whenever `arm()` fails; if the client is in a bad state this churns the phase every tick until conditions change. Suggest a bounded retry then hold.
- [MEDIUM conditional CARRIED] banked bronze pickaxe 1265 still never withdrawn (withdrawFinishedIfBanked covers only {BLONDE_WIG, PASTE, BRONZE_KEY, KEY_PRINT}); lines 3248/4028 hold terminally without it in inventory.
- [MEDIUM conditional CARRIED] B70 furnace confirmation: `handleKeyFurnaceConfirmation` holds "Unexpected furnace confirmation question: <text>" on any non-exact question text — including the empty-getQuestion() case.
- [LOW carried] B67 partial-set direct-loot gap: `GRAVE_DIRECT_LOOT_PROVED` requires the full 5-item set; a partial direct loot leaves WAIT_OPEN to time out into "Grave Loot click unproved".
- [LOW carried] B68 second-respawn requires held=true with a non-empty stage — near-unreachable after a completed first recovery.
- [LOW carried] B74 recoverReloadedWalk 3-tile gate vs walker's own <=10 predicate (method reordered in B77, content identical).

## Live acceptance
PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; no live URL. Verdict from static review only.
