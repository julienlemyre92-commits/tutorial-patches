# Review verdict: The Knight's Sword Build 5 / patch-1010 (2026-10-02 ~19:34 EDT)

**Reviewer:** Muse (read-only scope; Alex owns implementation and releases)
**Artifacts:** patch-1010 (commit 6c5cd94f, 2026-10-02T23:33:09Z), `knightssword-5.jar`,
`source-review/knightssword-build5/KnightsSwordScript.java` (711 lines), `PLAN.md`
**Runtime baseline:** Knight's Sword plugin, BUILD 4 / confirmed at 19:32 EDT (hot-loaded
outside the repo; bookkeeping gap again). This patch is the repo ship of Build 5.

## Custody — VERIFIED
- hot.json sha256 == jar sha256 (`cabdec84…31a` both) — air tight.
- All 7 jar entries net-rooted; `net/runelite/client/plugins/microbot/knightssword/KnightsSwordScript.class` present.
- version.txt=1010 matches patch-1010; no version reuse, no overwrite.

## Review — PASS WITH FINDINGS (no blockers)

**Delta (Build 4 -> 5, "bank-first preparation and persisted supply acquisition"):**
- New `BANK_PREPARE` flow: deposits non-retained items, withdraws whole redberry pie + 2 iron
  bars from the bank, withdraws coins for the GE budget, acquires pickaxe and 8-12 food.
- New persisted GE supply acquisition (`supplies-ge.properties`, atomic writes): buys pie/bars at
  the GE when the bank lacks them. Fails closed: preserves pre-existing GE offers, validates item
  id/quantity/cap against an allowlist, budget cap = min(2000, coins-500), refuses when quote
  exceeds budget, requires inventory delta proof after COMPLETE.
- PLAN.md implementation boundary honored: full quest route remains experimental; `preflightOnly`
  defaults true; this build extends the safe-preparation path only.

**Mechanics (all verified in source):**
- No blocking walkTo on the tick thread: routes run on a daemon worker via
  `walkWithStateUntil(target, radius, stopCondition)` with a 90s cap, stall detection
  (no player-tile movement in 15s), failure counter, and a 30s drain gate before reload.
  This is the same proven pattern as the BKF line.
- Login/quest movement gates per tick: native login only (loginIndex 10/34, verified free
  world, no repeated click), fails closed on every arming/ownership check (PID + build +
  class-SHA + mode in control.properties).
- Every action goes through the single-shot issue()/verify() journal: action ownership
  ambiguity holds; proofs are per-action (VARP/DIALOGUE/ITEM_GAIN/ITEM_LOSS/CROSS/BANK_OPEN/
  BANK_CLOSED/EAT); 12s unproved window -> HOLD. Cave entry/exit proved by zone crossing,
  ladder clicks proved by plane transition — consistent with the Pirate's Treasure lesson.
- FINISHED-gated logout: `Quest.THE_KNIGHTS_SWORD.getState()` read live every tick; the
  QUEST_FINISHED branch logs out only when the native state reads FINISHED. Logout happens at
  the Squire in safe Falador — the BKF20-1 "parked in hostile territory" hazard does NOT apply
  here (no deferred-logout-inside-the-cave path exists in this route).

**Findings (INFO, none blocking):**
- KS5-1: `quiesceForReload()` throws `IllegalStateException` when an action/route is
  unsettled. Safe, but the host must catch and retry at a settled boundary or the reload
  stalls — dependency on the host pattern, same as the BKF line.
- KS5-2: Death parks in HOLD ("Death/respawn: stale transaction must be reconciled") with no
  auto grave recovery — deliberate per PLAN.md ("an unresolved state must stop with
  diagnostics in a safe place"), but a mid-cave death stops gameplay until Alex re-arms.
- KS5-3: If never armed after native FINISHED, the script parks at QUEST_FINISHED silently
  (no hold/timeout). Safe state, but invisible — consider a HOLD with a message for the panel.
- KS5-4: `bankInspect` deposit-all sweeps every non-retained item type into the bank each
  tick until clean. By design for a clean quest inventory, but it will also bank anything the
  account happens to carry (teleport runes, etc.). Only QUEST mode sets
  `Rs2Walker.disableTeleports=true`; BANK_PREPARE walks may still teleport.
- KS5-5: `escape()` holds when exit ladder 17385 is absent at the exact tile ("no blind
  path") — correct; and rocks-null escapes rather than holding, which is the safer choice in
  the cave.

**Expected live behavior:** Runtime Build 4 should hot-load Build 5; with the BANK_PREPARE
mode armed, the bot continues withdrawing pie/bars/pick/food and stages GE buys for whatever
the bank lacks. `SUPPLIES_READY` (or `BANK_PREPARE_READY`) is the next stage to watch, then
QUEST-mode route (not expected until Alex arms it per PLAN.md).

## Watch items (carried)
- Build 5 startup marker + first new runtime lines (only trust runtime, never the banner).
- Bank withdrawal proofs (inventory delta) and GE purchase proofs if triggered.
- Quest tally: 9 quests / 29 QP verified, unchanged. BKF completion remains
  handoff-corroborated but unobserved (no Congratulations/QP read) — unchanged by this patch.
- Watch BUILD IN PROGRESS / Alex bubbles for the quest-route arming decision.
