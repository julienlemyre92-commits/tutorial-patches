# Review verdict — Prince Ali Rescue Build 49 / patch-742 (read-only)

- Reviewed: 2026-10-01 15:40 EDT (19:40 UTC)
- Commit: 5911aad75890 (2026-10-01T19:26:10Z) — "Prince Ali Rescue Build49: removes stale saved stage guards from the exact mined-clay timer recovery"
- Repo version.txt: 742 (via API) — sequential, no reuse (741→742), no upload-over
- Baseline: Build 48 review (2026-10-01-1535, PASS-with-findings)
- **Verdict: PASS** (no new findings; closes b48-F1 and b32; carried findings unchanged)

## Chain of custody — ALL PASS

1. patch-742.hot.json declares sha256 `9fafe0440b06eca33f5d67a5f00564d4f1c669f7e182f4802599c5ff99b08d39`; downloaded patches/princealirescue-49.jar hashes to the identical value (git blobs API, raw). ✓
2. PrinceAliRescueScript.class byte-identical across patch-742.zip / princealirescue-49.jar / princealirescue-plugin-49.jar: `3593dd27873988d6ad4908bf1a4aea3647240e2b78534adc9fb994babfe1a861`. ✓
3. patch-742.zip: 221 files, net/-rooted (only META-INF/ + MANIFEST.MF + version.txt exceptions), same entry count as patch-741. ✓
4. In-zip version.txt = 742 == repo version.txt = 742. ✓
5. javap on the shipped class: `public static final int BUILD_NUMBER = 49;`. Not banner-alone. ✓
6. PrinceAliRescuePlugin.java / PrinceAliRescueConfig.java sources byte-identical 48→49 (in-repo source-review); Config.class byte-identical 48→49; Plugin.class churn is the inlined BUILD_NUMBER compile-time constant — benign, same pattern as builds 45–48. Script class differs 48→49 (the fix itself). ✓

## Delta (Build 48 → 49, exact source diff — 2 hunks)

1. `BUILD_NUMBER` 48 → 49.
2. `recoverObservedSoftClayMineGain` strict gate: `(exactHold||exactExpiredSource) && (!"SOFT_CLAY_LOCAL_MINE".equals(geStage) || !softClayFallbackRecovered || softClayMineAttempts!=1)` → `exactHold && (...)`. The **exactExpiredSource** branch (6-minute source-timer expiry after an observed clay gain) now fires on observed-evidence guards alone: exact hold text, sourceItem==SOFT_CLAY, goal==1, LOGGED_IN, varp==20, plane 0, ≤6 tiles of RIMMINGTON_CLAY_MINE, CLAY==1, SOFT_CLAY==0, pickaxe 1265 present.

Commit-message claim verified in code: "stale saved stage guards" = exactly the dropped `(geStage / softClayFallbackRecovered / softClayMineAttempts)` requirements for the expired-timer branch.

## b48-F1 (medium) — RESOLVED

The flag is no longer gated anywhere on the expired-source path. Verified in source: `softClayFallbackRecovered` is still set true at exactly one place — :1874 inside `recoverObservedUnavailableSoftClayQuote` — and is now required only at :1892 (the exactHold branch). All three entries into local clay mining — :1874 HOLD-based quote recovery, :1691 live `GE_SOFT_CLAY_UNAVAILABLE_FALLBACK`, :368 fresh-reload quote gate — now recover on a 6-minute timer expiry after an observed gain instead of holding terminally.

Provenance for the relaxed gate is sufficient (matches the suggested fix in the b48 verdict): the exact error text is produced only by `localSoftClayMineTick` (:1903–1906), which dispatches only when `geStage==SOFT_CLAY_LOCAL_MINE` (:822); the startsWith match bakes "clay=1 water=0" into the hold text at hold time, and the broad guards re-verify CLAY==1, at-mine, LOGGED_IN, varp==20, pickaxe present at fire time. No spurious-fire path found — a reload restoring a stale hold is re-verified against live state (e.g. clay banked/spent → count(CLAY)!=1 → reject). Post-recovery flow unchanged and safe: timer rebase → CLAY>0 exit → LOCAL_CLAY_READY_FOR_WATER → water acquisition; `localSoftClayMineTick` byte-untouched 48→49.

**b32 (expired-source flag) — RESOLVED** by the same change.

## Findings

None new.

## Carried findings (unchanged by this delta)

- b47-F1 (low): `softClayMineRecoveryDiagnosticLogged` still a plain field (:112), not persisted across hot reloads — one-shot diagnostic is one-per-session. Untouched by this diff.
- b41-F1 residual exactHold corner: the exactHold ("Unproved MINE_SOFT_CLAY;") branch still requires flag+stage+attempts==1; :368/:1691 entries never set the flag, so a proof-predicate miss with clay==1 on those paths would still terminal-HOLD. Narrow, pre-existing, not worsened.
- b41-F2 (no coin withdraw for shop buys) · b42-F1 (fountain gate lacks object proof) · b39 (RECOVERED over-claim) · b25 (dead-tinderbox-recover) · b44-F2 (remaining==1-only fallback) · b44-F3 (water-leg inherits b41-F2/b42-F1).

## Live acceptance

PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; no live stream URL. Accept when `RECOVERED_MINE_SOFT_CLAY_BY_INVENTORY_GAIN … cause=EXPIRED_TIMER_AFTER_OBSERVED_MINE … timer rebased` appears in fresh in-game diag. Do NOT ship anything over Alex's build — read-only review per scope limit.
