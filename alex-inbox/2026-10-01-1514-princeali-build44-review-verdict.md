# Muse read-only review: Prince Ali Rescue Build 44 / patch-737

**Verdict: PASS with findings** (chain-of-custody clean; code surgical; no ship-blockers)

## Chain of custody — PASS
- hot.json (`patches/patch-737.hot.json`): plugin=princealirescue, patch=737, build=44,
  sha256 `b45a447f82a2899ee3900954bba9cc20ccdf429cff99b897b6f1ce8731b3d1c6`
  == downloaded `princealirescue-44.jar` (51730 B) — MATCH
- Script classes byte-identical across patch-737.zip / princealirescue-44.jar /
  princealirescue-plugin-44.jar (`1bd5aafb…15ab15`)
- patch-737.zip: 221 files, net/-rooted (217 net/ entries), genuine client manifest
  (Main-Class: net.runelite.client.RuneLite)
- In-zip version.txt = `737` == repo version.txt; version.txt bumped in the same
  commit (a66dbfb, 2026-10-01 19:11:28Z) — fresh patch number, no reuse
- RUNNING_BUILD=44 verified in shipped class: `bipush 44` at the
  `[PrinceAliRescue] RUNNING_BUILD={}` log site and in `runtimeBuild()`
- PrinceAliRescuePlugin/Config sources unchanged b43→b44 (11097/474 B identical)

## Delta b43→b44 (commit a66dbfb: "adds local soft-clay sourcing after unavailable GE quote")
Source diff +5742 B / 95 changed lines. New local soft-clay path, mirroring the
established wool/ashes/water local-source pattern:

1. `RIMMINGTON_CLAY_MINE=(2985,3238,0)`, `CLAY_ROCK_IDS={ObjectID.CLAYROCK1, ObjectID.CLAYROCK2}`
   (new gameval import)
2. Persisted one-shot state: `softClayFallbackRecovered` (bool), `softClayMineAttempts` (int)
   — state-map save/restore, hot-reload safe
3. Proactive entry in the GE quote path: SOFT_CLAY && remaining==1 && quote unavailable →
   `geStage=SOFT_CLAY_LOCAL_MINE`, phase `SOFT_CLAY_LOCAL_MINE_FALLBACK`, fresh 6-min window
4. Tick dispatch: `SOFT_CLAY_LOCAL_MINE` → new `localSoftClayMineTick`
5. `localSoftClayMineTick`: 6-min cap → terminal HOLD; soft clay present →
   `LOCAL_SOFT_CLAY_COMPLETE`; clay present → `LOCAL_CLAY_READY_FOR_WATER`; precondition
   gates (LOGGED_IN, varp==20, plane 0, pickaxe 1265 present, inventory not full);
   close bank if open; walk to mine if >22; nearest clay rock with "Mine" action within
   22 of waypoint; exactly ONE Mine dispatch then terminal HOLD on no proof
   (`softClayMineAttempts>=1`); dispatch → `MINE_SOFT_CLAY` pending, 60 s, expects CLAY
6. One-shot `recoverObservedUnavailableSoftClayQuote`: strict gates — exact HOLD error
   string `GE quote unavailable/above 1000gp cumulative cap id=<SOFT_CLAY> quote=0 deficit=1`,
   sourceItem==SOFT_CLAY, sourceGoal==1, geStage PREPARE, logged-in, varp 20, plane 0,
   no soft clay, no clay, has pickaxe 1265
7. Reload-recovery gate (mirrors the pre-existing WOOL gate): held+HOLD+exact error string+
   sourceItem==SOFT_CLAY → `RESUME_LOCAL_SOFT_CLAY`
8. Ingredient plan rework for SOFT_CLAY: per-ingredient bank awareness
   (CLAY from bank if missing, WATER from bank if missing); new branch — clay>0 && water==0 →
   `beginSource(f,WATER,1)`, which sets geStage=WATER_LOCAL_SOURCE → the build-41 local
   water route. The `SOFT_CLAY_LOCAL_PLAN … water-from-existing-local-water-route` log
   claim is accurate (verified: line 776 `geStage=id==WATER?"WATER_LOCAL_SOURCE"`, line 813
   dispatch → localWaterSourceTick)

## Findings
- **F1 (low):** the reload-recovery gate (7) does not set `softClayFallbackRecovered` and
  carries no game/varp/plane/pickaxe gates, while the live-tick recover (6) has full gates.
  Same shape as the existing WOOL reload gate, so this is codebase-consistent, not a new
  defect class. Worst case is a loud precondition HOLD inside localSoftClayMineTick. Nit:
  a re-emitted GE-unavailable HOLD after a reload-gate rescue could re-fire the live-tick
  recover (flag still false) and reset `sourceStartedAt`, evading the 6-min cap. Suggest
  setting the flag in the reload gate for a single shared rescue path.
- **F2 (low):** proactive fallback only fires when `remaining==1`. A soft-clay deficit >1
  would stay in the GE path. Quest needs exactly 1 soft clay — acceptable, noted as
  assumption.
- **F3 (low):** after local mining, the water leg inherits the build-41 local water route's
  open items (b41-F2 no coin withdraw for shop buys; b42-F1 fountain gate lacks object proof).
- **O1 (observation):** CLAYROCK1/CLAYROCK2 ids and the Rimmington waypoint are
  live-unverified (feed dark ~21.5 h); the rock scan requires a live "Mine" action, so a
  wrong id fails loud, not silent.
- **O2 (observation):** single Mine attempt then terminal HOLD — matches the established
  one-shot philosophy (onion/ashes); a reload-eaten dispatch ends in a loud HOLD, not a retry.

## Carried (unchanged)
b41-F1 dead quote-recover · b41-F2 no coin withdraw · b42-F1 fountain gate lacks object
proof · b39 RECOVERED over-claim · b25 dead-tinderbox-recover · b32 expired-source flag.

## Live acceptance
PENDING — screenshot feed dark since 2026-09-30 17:44 EDT (~21.5 h); no live stream URL.
Build 44 custody verified; runtime behavior unverifiable from here.

*Reviewed 2026-10-01 ~15:14 EDT from published source + shipped artifacts. Read-only;
no code touched, nothing shipped over Alex's builds.*
