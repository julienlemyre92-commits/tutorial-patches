# Review verdict: Below Ice Mountain Build 42 (patch-907) — PASS WITH FINDINGS

Reviewed 2026-10-02 ~01:45 EDT by Muse (read-only reviewer; Alex owns implementation/releases).
Commit: 9893a00149 "Below Ice Mountain Build42 safe chicken training" (2026-10-02T05:40:36Z).
version.txt = 907.

## Custody (air tight)
- `patches/patch-907.zip`: 284 entries; entry NAME SET byte-identical to
  patch-906 (diff of sorted name lists empty); zip root `net/`;
  version.txt inside zip = 907.
- `patches/belowicemountain-42.jar` sha256 = e22a4340656e8d3d...,
  identical to `patch-907.hot.json` sha256
  (e22a4340656e8d3d1e5485bbeac2901132851608932d7da112cb4ad29182b230).
- Shipped `BelowIceMountainScript.class` disassembled: `runtimeBuild()` =
  `bipush 42; ireturn`. No stale-class risk.
- Source: `source-review/belowicemountain-build42/BelowIceMountainScript.java`
  (121,032 bytes) + cumulative README.md with Build 42 changelog.

## What changed (B41 -> B42, 221 diff lines)
1. `BUILD_NUMBER` 41 -> 42.
2. New `trainChickens(f)` stage (`PREP_TRAIN_CHICKENS`), entered only when
   `maxHp<20 && prepNativeStarted && "COMPLETE".equals(prepFoodStage)
   && prepNativeCheckpoint.contains("|COMPLETE|")` — double proof the native
   buyer finished before any combat.
3. Threat/retreat behavior (the bot-maker's stated safety-review item):
   - Frame now captures `hpXp`, `geOpen` (widget 465,1), `interactingNpc`
     (player's current target name), `unsafeAggressor` (any NPC targeting the
     player whose name is not Chicken, case-insensitive).
   - On threat or `hp <= max(6, maxHp/2)`: `trainingRetreat=true`; in-flight
     pending is ABORTED (logged `TRAIN_ABORT_UNPROVED`, `pending=null`) rather
     than finished; active route cancelled (`WAIT_TRAIN_ROUTE_STOP`).
   - Retreat branch: emergency-eat while hp low (`train:emergency-eat`,
     Proof.FOOD_HEAL), else walk to FALADOR_BANK, then terminal HOLD.
4. Attack path: close GE first (widget 465,2 child 11 "Close", Proof.GE_CLOSED,
   HOLD if the control is absent); walk to CHICKEN_FARM (3238,3298) r=7;
   query nearest Chicken within 15; walk to it r=2; line-of-sight + adjacency
   gate (`hold("Chicken behind gate/wall")` — answers the README's "gate
   reachability still need live proof"); one attack per `train:chicken`
   (Proof.HP_XP_GAINED, 30s timeout); `WAIT_CHICKEN_COMBAT` while engaged,
   45s combat-timeout HOLD; unexpected non-Chicken target -> retreat + HOLD.
5. Bounded train actions: `issue()` excludes `train:` keys from the quest-stage
   re-issue budget; a REJECTED train action -> immediate HOLD; an UNPROVED
   (timeout) train action -> `pending=null` + HOLD, no repeat. `trainingXpActions`
   (proved chicken attacks only) capped at 2000 -> HOLD.
6. Guards: `maxHp>=20` -> HOLD (target reached); `maxHp<11` or hpXp<0 or not in
   overworldPrepArea -> HOLD "Invalid training scene"; death/teleport detector
   (`trainingStarted && !near(CHICKEN_FARM,30)` -> HOLD); training state
   (started/retreat/xpActions/encounterAt) persisted + restored across hot reload.
7. Diag: `hitpointsXp`, `trainingXpActions`, `trainingRetreat` added to status.

## Logic verified
- No train action can fire before the buyer's COMPLETE checkpoint is durably
  proven (two independent signals). The GE must be closed with proof before
  any walk/attack.
- Every unproved or rejected combat action terminates in HOLD — nothing retries
  silently, nothing loops: timeout HOLD, rejection HOLD, budget HOLD,
  combat-stall HOLD, gate HOLD, retreat HOLD. Matches the README claim.
- Retreat eats before walking and never re-engages after an aggressor appears.
- Chicken targeting is name-gated twice (interactingNpc + aggressor scan) and
  pen reachability is proven before the attack issues.

## Findings
- PASS. No defects in the B42 change itself; the in-flight abort + retreat is
  exactly the safety behavior the bot-maker's note asked to be reviewed.
- INFO BIM42-1 (new, deliberate): `trainingRetreat` is a sticky, persisted
  latch whose only exit is the terminal "Retreated to bank after training
  threat" HOLD — one unexpected aggressor ends training permanently with no
  in-script re-arm. Conservative by design; follow-up builds own any re-arm.
- INFO BIM42-2 (new, design note): worst-case XP math — 11->20 HP needs ~4,213
  HP XP; proved attacks count only (misses don't consume budget), but at 1 XP
  per proved attack the 2000-action cap fires before 20 HP. Expected terminal
  state under low XP rate is the deliberate budget HOLD, not a loop.
- Carried open: BIM40-1 (status-write lock contention, cosmetic), BIM40-2
  (stale fillAt restore edge), BIM40-3 (dead troutFour factory), BIM40-4
  (keyboard typing gated on input focus), BIM41-1 (one-tick
  prepNativeStarted/checkpoint window), bankCoins discrepancy (script debug
  3023 vs bot-maker panel "1,023 coins in the bank"), guardian controller
  unimplemented, BIM38-1/38-2/38-3.
- META-INF/MANIFEST.MF in the patch zip remains the pre-existing jar-style
  build pattern (harmless on the hot-reload path).

## Live acceptance (pending)
Watch for overlay "RUNTIME BUILD 42" + NEW `PREP_TRAIN_CHICKENS` /
`WAIT_CHICKEN_COMBAT` step text and per-phase train diag after re-arm.
Acceptance only from new runtime lines, never the banner alone. Script class
marker verified above at the bytecode level; live scene + XP proof pending.
