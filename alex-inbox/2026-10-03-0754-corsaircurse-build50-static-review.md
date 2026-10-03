# Corsair Curse Build 50 — Muse static review (2026-10-03 ~07:54 EDT, read-only)

**Build:** 50 (BUILD_NUMBER 49 → 50) · patch-1096 / corsaircurse-50.jar
· commit 4b00bbd3 (07:51:31 EDT) · version.txt 1095 → 1096
· scriptSha256 `1ad41532…92b35fb` matches patch-1096.hot.json
· source-review/corsaircurse-build50/ (full 1613-line script + identity.json present)
· Parent: Build 49 (a4003404). Diff scope: 117 diff lines, all in CorsairCurseScript.java.

## What changed (Build 49 → 50)

1. **New `bossRoom(Frame f)` predicate** — replaces three inline copies of
   `f.progress==52 && (inRoom(f.pos,ITHOI) || f.enemyId==NAVIGATOR)`:
   `progress==52 && pos!=null && ((instanced && plane==1 && inRoom(templatePos,ITHOI)) || inRoom(pos,ITHOI) || enemyId==NAVIGATOR)`.
   Frame gains `instanced` (`c.isInInstancedRegion()`) and `templatePos`
   (`WorldPoint.fromLocalInstance(c, localLocation)` when instanced, else raw pos).
2. **Death recovery resets boss flags** — RECOVERED/NOTHING_TO_RECLAIM now also clears
   `bossSafetyActive, bossEatAttempted, bossEscapeAttempted, bossMealsProved(=0),
   combatRetreat, combatStarted, error` (was: only prepared/prepAttempted).
3. **Boss preflight fail-soft** — preflight failure at safe ground now attempts ONE bounded
   `prepare(f)` (gated `!inCombat && !blocksQuestInput && !prepAttempted`) before the
   `"Boss entry preflight failed after bounded preparation"` hold.
4. **Proof-based boss meals** — new `bossMealsProved` counter (status file + snapshot);
   eat-proof receipt on pending-eat journal: `bossEatAttempted=false; bossMealsProved++`
   with `BOSS_EAT_PROVED` log; gate is now
   `!bossEatAttempted && bossMealsProved<8 && hp<=max(12,hpMax*3/4) && food>0`.
5. **Boss exit proof hardened** — replaces `leaveRoom(f.pos)` plan with direct query for
   `ObjectID.DS2_CORSAIR_COVE_STAIRS_RAMP` within 12 tiles of raw pos + explicit checks
   (stairPoint non-null, plane==1, near(pos,stair,12), clickbox non-null); new stages
   BOSS_EXIT_ROUTE_UNPROVED / BOSS_EXIT_OBJECT_ABSENT / BOSS_EXIT_OBJECT_UNPROVED;
   `BOSS_EXIT_OBSERVED` warn-log with raw/template/stair/id.
6. **retreat() routes through bossSafety** — boss room → `bossSafetyActive=true;
   bossSafety(f,true)` instead of `execute(f, leaveRoom(f.pos))`.
7. **COMBAT NPC query** — for boss, `.within(bossRoom(f) ? f.pos : p.at(), 7)` (raw player
   pos inside the instance instead of the template plan anchor).
8. **Status snapshot** — adds `bossMealsProved`, `instanceTemplate`, `instanced` properties.

## Reviewer assessment

Coherent, mechanism-first package aimed squarely at the 07:44 EDT live observation
(Build 49: Ithoi fight in the instanced room → death with 0 food → recovery → replan):
- The instanced-coordinate defect is real and the fix is the right shape: inside the
  Ithoi template instance, raw `WorldPoint` does not satisfy `inRoom(raw, ITHOI)`,
  so the old inline guard silently misfired (boss-mode actions refused, combat query
  anchored wrong). `fromLocalInstance` template coords + `isInInstancedRegion` gate is
  the standard correction. Null-safety: `getLocalPlayer()` null-checked at snapshot
  entry; `templatePos` falls back to raw pos when not instanced. OK.
- Death-recovery boss-flag reset kills the stale-flag recurrence class
  (Build 30/31 "reloadheld recurrence" watch items) — recovery now provably returns to
  a neutral boss state instead of leaking `bossSafetyActive`/`bossEscapeAttempted`.
- Proof-based meals (`bossMealsProved<8`, eat on injury ≤75%) directly answers
  "died with 0 food": re-engagement must now prove up to 8 eats rather than attempt
  once. Eat-proof uses the pending/journal pattern consistent with the rest of the script.
- Exit proof is strictly stronger: object-ID query + plane/near/clickbox verification +
  observed-log beats the old `leaveRoom` plan hop. Directly addresses the Build 40
  cachegate watch item.
- Bounded prepare on preflight failure is fail-soft, not a loop: single attempt gated on
  `!prepAttempted`, then an honest named hold. No unbounded retry introduced.
- No new ship-side or pipeline changes (read-only review; Alex owns releases).

**Defects found:** none at static level.

## Verdict

**PASS (static). Live acceptance pending** — requires stream overlay:
RUNTIME BUILD: BUILD 50 marker + new runtime lines (BOSS_EAT_PROVED / BOSS_EXIT_OBSERVED),
post-death Ithoi re-engagement with proved food, checkpoints 14/18+.

Read-only review per standing scope; nothing shipped. Screenshot feed dark since
2026-09-30 17:44:02 EDT; last live eyes 07:44 EDT (Build 49, 13/18 checkpoints).
