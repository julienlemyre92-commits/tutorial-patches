# Review verdict — Prince Ali Rescue Build 45 / patch-738 (read-only)

- Reviewed: 2026-10-01 15:24 EDT (19:24 UTC)
- Commit: affcba5a (2026-10-01T19:19:42Z) — "Prince Ali Rescue Build45: proves the observed local soft-clay mine gain and resumes without a second click"
- Repo version.txt: 738 (sha 3e40c38982ee9ddbd2ede47c8644ede75dbd2143)
- Baseline: Build 44 review (2026-10-01-1514, PASS-with-findings)
- **Verdict: PASS-with-findings** (1 new low finding; no blockers; carried lows unchanged)

## Chain of custody — ALL PASS

1. Repo version.txt (738, via API) == in-zip version.txt (738). ✓
2. patch-738.hot.json declares sha256
   `51e1d42e7a5e63bb12ed7f013501ea7292540d6ced6700c75f8d500d2297f602`;
   downloaded princealirescue-45.jar hashes to the identical value. ✓
3. PrinceAliRescueScript.class byte-identical across all three artifacts
   (patch-738.zip, princealirescue-45.jar, princealirescue-plugin-45.jar):
   `253da81b81359c6e7067ca00ed083a5d999c93a3b94e22a2b98a63530c207283`. ✓
4. patch-738.zip: 221 files, net/-rooted (only META-INF/ + version.txt
   exceptions), same entry count as patch-737 (221). ✓
5. javap: `public static final int BUILD_NUMBER = 45;` in the shipped class;
   the new method `recoverObservedSoftClayMineGain` and the new runtime
   strings ("RECOVERED_MINE_SOFT_CLAY_BY_INVENTORY_GAIN" x2,
   "Unproved MINE_SOFT_CLAY;", "Reload during MINE_SOFT_CLAY; ...") are
   present in the compiled class. Not banner-alone. ✓
6. Plugin/Config: Config.class byte-identical 737→738. Plugin.class changed
   but Plugin.java source is byte-identical between builds — explained:
   `plugin45.java:45` does `private int build = PrinceAliRescueScript.BUILD_NUMBER;`
   and BUILD_NUMBER is a compile-time constant, so javac inlines 44→45 into
   Plugin.class. Benign, expected. ✓

## Delta (Build 44 → 45, from source-review diff)

1. `BUILD_NUMBER` 44 → 45.
2. New `recoverObservedSoftClayMineGain(Frame)`, called FIRST in the
   held-recovery chain in tick() (before the dye/soft-clay-quote/onion
   recoveries). It fires only on the exact terminal states:
   - HOLD with error starting "Unproved MINE_SOFT_CLAY;" (matches the
     hold() call at script45.java:503, "Unproved "+pending.action+";"), or
   - HOLD_RELOAD_IN_FLIGHT with restoredInFlightAction=="MINE_SOFT_CLAY" and
     the exact reload error string (matches restore block at :385-386).
   - Plus strict gates: sourceItem==SOFT_CLAY, sourceGoal==1,
     geStage=="SOFT_CLAY_LOCAL_MINE", softClayFallbackRecovered,
     softClayMineAttempts==1 (persisted at :189/:271, survives hot reload),
     LOGGED_IN, varp==20, plane 0, within 6 tiles of RIMMINGTON_CLAY_MINE,
     count(CLAY)==1, count(SOFT_CLAY)==0, pickaxe 1265 present.
   - On fire: pending=null, held=false, error="", restoredInFlightAction="",
     phase="RECOVERED_MINE_SOFT_CLAY_BY_INVENTORY_GAIN", diag line logged,
     NO repeat click.
3. New proof predicate in proved(): MINE_SOFT_CLAY is proved when
   `f.count(CLAY) > p.before.count(CLAY)`. This is the piece Build 44 was
   missing — a successful mine could never be proved, so the 60 s pending
   deadline expired into the terminal "Unproved MINE_SOFT_CLAY;" HOLD even
   with the clay in inventory.

Post-recovery flow verified by reading code (no assumptions): next tick,
geStage is still SOFT_CLAY_LOCAL_MINE → localSoftClayMineTick sees
count(CLAY)>0 → LOCAL_CLAY_READY_FOR_WATER, clears source/geStage; then
need(SOFT_CLAY) takes the bank branch → "water-from-existing-local-water-route"
beginSource(WATER,1) → fill bucket → combine → CRAFT_SOFT_CLAY (existing
predicate). The attempts>=1 HOLD gate at :1928 sits below the CLAY>0 exit,
so no accidental re-hold and no second Mine click. Commit-message claim holds.

## New finding

- **F1 (low):** the new recovery's exactReload branch requires
  `softClayFallbackRecovered`, but the fresh-reload quote→mine gate
  (script45.java:365-369, carried b44-F1) sets geStage and resets
  softClayMineAttempts without setting softClayFallbackRecovered. If a hot
  reload lands in the ~1-tick window between a successful clay gain and the
  next proof tick AND the session came through that reload-gate path (not the
  live quote-recover path, which does set the flag), the recovery rejects
  and the bot sits in terminal HOLD_RELOAD_IN_FLIGHT with the clay already
  in inventory. Rare (narrow window + specific path), loud if it happens.
  Fix suggestion: set softClayFallbackRecovered=true in the :365-369 reload
  gate (also closes carried b44-F1).

No shadowing: the new recovery's preconditions (geStage SOFT_CLAY_LOCAL_MINE
+ softClayFallbackRecovered) are mutually exclusive with
recoverObservedUnavailableSoftClayQuote's (geStage PREPARE +
!softClayFallbackRecovered); the dye recoveries require sourceItem==DYE.

## Carried findings (unchanged by this delta)

b41-F1 dead quote-recover / reload gate lacks flag (now partially implicated
in new F1) · b41-F2 no coin withdraw for shop buys · b42-F1 fountain gate
lacks object proof · b39 RECOVERED over-claim · b25 dead-tinderbox-recover ·
b32 expired-source flag · b44-F2 remaining==1-only fallback (quest needs
exactly 1 soft clay — acceptable) · b44-F3 water-leg inherits b41-F2/b42-F1.

## Live acceptance

PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; no live stream
URL. Accept only when `PROVED_MINE_SOFT_CLAY` or
`RECOVERED_MINE_SOFT_CLAY_BY_INVENTORY_GAIN` appears in fresh in-game diag.
Do NOT ship anything over Alex's build — read-only review per scope limit.
