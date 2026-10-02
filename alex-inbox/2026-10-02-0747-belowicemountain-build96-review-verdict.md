# Below Ice Mountain Build 96 — read-only review verdict (Muse)

**Date:** 2026-10-02 07:47 EDT
**Ship:** commit cb0ece60 (11:44:31Z) — patch-958 / belowicemountain-96.jar / version.txt=958
**Verdict: PASS WITH FINDINGS** (no blocking defects; Alex owns release)

## Custody (verified via independent GitHub API reads + binary downloads)
- version.txt = 958 (sha 1c0f77af) == in-zip version.txt ("958")
- patch-958.zip: 313 files, root = `net/` (309 under net/runelite; rest = META-INF + version.txt + dirs)
- belowicemountain-96.jar sha256 `44de31fb…03a` == patch-958.hot.json sha256 — MATCH
- zip's BelowIceMountainScript.class bytes == jar's (sha b9c785f7…)
- BUILD_NUMBER = 96 in source AND compiled class (javap ConstantValue) — banner will be truthful this time
- Commit message says "Build91" but ships 96 — 5th straight stale message (INFO, carried)

## What B96 actually changes (B95 → B96 diff, 129 lines)
Commit message "empty equipment bridge" does not describe the change. The real change set is
guardian eat/interrupt/retreat hardening:

1. **Interrupted-action preservation ("bridge")**: interrupting a pending guardian action for
   food no longer empties `guardianPending` (+ `guardianMoveTarget`, b95 behavior). It parks the
   action in new field `guardianInterruptedForFood` and restores it after the eat resolves —
   on CONSUMED, REJECTED, and UNPROVED paths alike. Correct in all three restore sites.
2. **Eat throttle**: new `guardianNextEatAt`, 1800 ms between eats; all three eat entry paths
   respect it (two pre-check, `guardianEat` self-guards). Kills the double-eat race.
3. **Relaxed eat proof**: consumption proved by item-count decrease alone; HP gain now only
   selects the stage label (`GUARDIAN_FOOD_CONSUMED_HP_GAIN` vs `..._HP_UNCONFIRMED`) and a
   rich info log (netHpDelta, healingVisible). Fixes the false-UNPROVED when eating at/near
   full HP. Count-decrease is a safe consumption proof in the arena (bot never drops food).
4. **Trout-aware eat threshold**: `max(max(8, maxHp*3/5), maxHp-7)` when food is trout —
   eats early enough to never waste the 7-HP heal. Matches B93's trout-only latch.
5. **Emergency eat in the failed-logout window**: the 5 s post-logout verification window
   (previously action-dead while taking damage) now permits one bounded emergency eat
   (hp<=max(8,maxHp-7), food present, eatUnproved<2, input owned, not paused/human).
6. **Retreat fail-closed**: `guardianRetreat && guardianExitFailed` previously fell THROUGH the
   retreat block and could reach `tryGuardianPillar` (mining while retreating). Now routes
   straight to `guardianLastResort` (bounded logout → UNRESOLVED hold). Real fix.
7. **Error-HOLD escape gate**: the `!error.isEmpty()` branch now gates the bounded guardian
   escape controller on input-ownership (`ownsInput`, `!pauseAllScripts`, `!InputArbiter.isHuman`)
   instead of `guardianActionsAllowed()`. Safe by construction: `guardianTick` forces
   `guardianRetreat=true` whenever `!guardianActionsAllowed()`, and with retreat set the tick
   can only eat/exit/logout — `tryGuardianPillar` is unreachable. Fixes the strand-under-attack
   case (error HOLD + unarmed + in cave = stood still). Safety improvement, not a weakening.

## Findings
- **W1 (minor, benign edge)**: the emergency-eat path does not pre-check `guardianNextEatAt`;
  `guardianEat` self-guards but returns silently while throttled, leaving
  `guardianInterruptedForFood` set with `guardianPending=null`. A later exit dispatch that gets
  interrupted for food overwrites `guardianInterruptedForFood`, losing the pre-emergency action.
  Benign in practice (retreat was already true; the lost action is moot), but the field has no
  single-owner discipline. Suggest: clear or re-assert it when a non-eat pending is created.
- **I1**: stale commit message ("Build91", undescriptive "empty equipment bridge") — 5th straight.
- **I2**: README still opens with the Build 1 handoff; documents nothing past ~Build 68.
- **I3**: official `META-INF/MANIFEST.MF` in the patch zip again (benign, carried).
- **I4**: `guardianNextEatAt`/`guardianInterruptedForFood` are not in the hot-reload persisted
  set — after reload they reset to 0/null, which is the fail-safe direction (eat allowed sooner).
  Benign.

## Live acceptance (pending — no fresh evidence)
Watch for `RUNNING_BUILD=96` / `BUILD_NUMBER=96` runtime lines, `GUARDIAN_ACTION_INTERRUPTED_FOR_FOOD`
with restore, `GUARDIAN_FOOD_CONSUMED_HP_*`, and `GUARDIAN_LAST_RESORT_LOGOUT` on exit failure.
Screenshot feed dark since 2026-09-30 17:44 EDT (~38 h); no confirmed live stream URL.
B57–B96 live acceptance all still pending.

## Scope note
Read-only review per standing rule (Alex owns BIM implementation/releases). No code touched,
nothing shipped. Tutorial Island tree idle; build238-src untouched (no sibling mid-ship).
