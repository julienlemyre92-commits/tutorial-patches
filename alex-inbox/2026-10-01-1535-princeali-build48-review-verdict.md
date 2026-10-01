# Review verdict — Prince Ali Rescue Build 48 / patch-741 (read-only)

- Reviewed: 2026-10-01 15:35 EDT (19:35 UTC)
- Commit: 6e23b7d7 (2026-10-01T19:25:18Z) — "Prince Ali Rescue Build48: rebases the exact clay-recovery source timer and resumes water acquisition"
- Repo version.txt: 742 (Build 49 landed after; at ship time 741 — sequential, no reuse)
- Baseline: Build 47 review (2026-10-01-1534, PASS-with-findings)
- **Verdict: PASS-with-findings** (1 new medium finding; no blockers)

## Chain of custody — ALL PASS

1. In-zip version.txt = 741 == this build's patch number. ✓
2. patch-741.hot.json declares sha256 `f385d54a6e64e680f62e91f4311176e28376ccd6b6168f2272ac4ba3c9193874`; patches/princealirescue-48.jar hashes to the identical value. Verified twice: fresh API read + git blob hash `fedd41ad7ce3…` check. (Reviewer note: an early scratch transcription mistyped one hex char — `6e68e` vs `6e64e` — and briefly flagged a mismatch; programmatic re-verification from fresh API reads confirmed the declared and actual SHAs are identical. No custody issue.) ✓
3. PrinceAliRescueScript.class byte-identical across patch-741.zip / princealirescue-48.jar / princealirescue-plugin-48.jar: `a0f7924d8548292e…`. ✓
4. patch-741.zip: 221 files, net/-rooted (only META-INF/ + MANIFEST.MF + version.txt exceptions). ✓
5. javap: `public static final int BUILD_NUMBER = 48;` in the shipped class. ✓
6. Plugin.java / Config.java sources byte-identical 47→48; Config.class byte-identical; Plugin.class churn is the inlined BUILD_NUMBER constant — benign. ✓

## Delta (Build 47 → 48, exact source diff — 3 hunks)

1. `BUILD_NUMBER` 47 → 48.
2. New `exactExpiredSource` branch in `recoverObservedSoftClayMineGain`: `HOLD` + `error.startsWith("Local soft-clay ingredient source exceeded six minutes; clay=1 water=0")` — the exact text `localSoftClayMineTick` holds with when the 6-minute source budget expires after an observed clay gain. It joins the fire condition, and the strict gate became `(exactHold||exactExpiredSource) && (geStage!=SOFT_CLAY_LOCAL_MINE || !softClayFallbackRecovered || attempts!=1)`.
3. On fire: `sourceStartedAt=System.currentTimeMillis();` (timer rebase — new line :1894) and the diag updated to `… timer rebased, no repeat click` with cause `EXPIRED_TIMER_AFTER_OBSERVED_MINE`.

Commit-message claim verified in code. Post-recovery flow verified by reading code (`localSoftClayMineTick` byte-identical 45→48): rebased timer → `CLAY>0` exit → `LOCAL_CLAY_READY_FOR_WATER` → `need(SOFT_CLAY)` bank branch → water acquisition. The rebase is necessary and correct: without it the next tick would immediately re-hold on the stale 6-minute timer.

## New finding

- **F1 (medium):** the `(exactHold||exactExpiredSource)` strict gate requires `softClayFallbackRecovered`, but that flag is set true at exactly one place — :1874, inside `recoverObservedUnavailableSoftClayQuote`. The two primary entries into local clay mining never set it: the live `GE_SOFT_CLAY_UNAVAILABLE_FALLBACK` at :1690-1695 (`geStage="SOFT_CLAY_LOCAL_MINE"; softClayMineAttempts=0;` — Build 44's main path) and the fresh-reload quote gate at :366-371 (same shape). Consequence: on the common live path, if the 6-minute source timer expires after an observed clay gain (clay=1, water=0 — e.g. the water leg stalls on carried b42-F1/b44-F3), the bot holds terminally on the expired-source text and Build 48's new recovery rejects it on the flag. The new branch only helps sessions that entered via the HOLD-based quote recovery (:1874). The same residual applies to the pre-existing exactHold branch. Suggested fix: set the flag at :1691 and :368 (as the Build 45 verdict suggested for the reload gate), or drop the flag requirement for exactExpiredSource — the exact expired text can only be produced by `localSoftClayMineTick`, which only runs when `geStage==SOFT_CLAY_LOCAL_MINE` (:822), so text + stage + `CLAY==1` is already sufficient provenance.

## Carried findings (unchanged)

b41-F1 (now the mechanism behind the new F1) · b41-F2 · b42-F1 · b39 · b25 · b32 · b44-F2 · b44-F3 · b47-F1 (diagnostic flag not persisted).

## Live acceptance

PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; no live stream URL. Accept only when `RECOVERED_MINE_SOFT_CLAY_BY_INVENTORY_GAIN … cause=EXPIRED_TIMER_AFTER_OBSERVED_MINE … timer rebased` appears in fresh in-game diag. Do NOT ship anything over Alex's build — read-only review per scope limit.

## Note

Build 49 (5911aad7, 19:26:10Z — "removes stale saved stage guards from the exact mined-clay time") landed after this build and is out of scope here; its message reads like it may touch this verdict's F1 area — recommend a follow-up review of Build 49 / patch-742.
