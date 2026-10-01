# Review verdict — Prince Ali Rescue Build 47 / patch-740 (read-only)

- Reviewed: 2026-10-01 15:34 EDT (19:34 UTC)
- Commit: afaff38cc (2026-10-01T19:24:14Z) — "Prince Ali Rescue Build47: matches exact reload-hold text and emits one recovery-gate diagnostic if live proof is insufficient"
- Repo version.txt: 742 (Build 49 landed after; at ship time 740 — sequential, no reuse)
- Baseline: Build 46 review (2026-10-01-1533, PASS)
- **Verdict: PASS-with-findings** (1 new low finding; no blockers)

## Chain of custody — ALL PASS

1. In-zip version.txt = 740 == this build's patch number. ✓
2. patch-740.hot.json declares sha256 `698d363a342aff9245c8fc99972a482cccd476f0ab656d1681db81d1fbdd8949`; patches/princealirescue-47.jar hashes to the identical value (git-blob-hash verified). ✓
3. PrinceAliRescueScript.class byte-identical across patch-740.zip / princealirescue-47.jar / princealirescue-plugin-47.jar: `2d0d97ecfd1b3f71…`. ✓
4. patch-740.zip: 221 files, net/-rooted (only META-INF/ + MANIFEST.MF + version.txt exceptions). ✓
5. javap: `public static final int BUILD_NUMBER = 47;` in the shipped class. ✓
6. Plugin.java / Config.java sources byte-identical 46→47; Config.class byte-identical; Plugin.class churn is the inlined BUILD_NUMBER constant — benign. ✓

## Delta (Build 46 → 47, exact source diff — 4 hunks)

1. `BUILD_NUMBER` 46 → 47.
2. New field `softClayMineRecoveryDiagnosticLogged` (line 112).
3. `tick()` held branch: `recoverObservedSoftClayMineGain(f)` moved to FIRST position (ahead of loginTick and the save/restore), followed by a new one-shot diagnostic: if the recovery did not fire and `error.contains("MINE_SOFT_CLAY")`, logs `SOFT_CLAY_RECOVERY_CHECK phase={} error={} sourceItem={} goal={} game={} varp={} pos={} clay={} softClay={} pickaxe={} stage={} attempts={} restoredAction={}` once, then falls through to the existing recovery chain.
4. exactReload text match: `.equals("Reload during MINE_SOFT_CLAY; inspect quest/inventory/scene before resuming")` → `.startsWith("Reload during MINE_SOFT_CLAY;")`; the `restoredInFlightAction` equality check was dropped — verified redundant in code: the error string is constructed at exactly one place (:387, `error="Reload during "+inFlight+"; …"`) from the same `inFlight` value that sets `restoredInFlightAction`, so the startsWith match subsumes it.

Commit-message claim verified in code. Safety of the moved call: the recovery's own guards require LOGGED_IN, so running it before loginTick changes nothing during the login flow; it is side-effect-free when it returns false.

## New finding

- **F1 (low):** `softClayMineRecoveryDiagnosticLogged` is a plain field (declared :112, checked :401, set :402) with no `state.put`/`getOrDefault` — it is not persisted across hot reloads. The "one recovery-gate diagnostic" is therefore one-per-session: a reload resets it and the SOFT_CLAY_RECOVERY_CHECK line can log once more. Diagnostic-only, zero behavior impact.

## Carried findings (unchanged)

b41-F1 residual exactHold corner (flag still required there; :368/:1691 entries don't set it) · b41-F2 · b42-F1 · b39 · b25 · b32 · b44-F2 · b44-F3.

## Live acceptance

PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; no live stream URL. Accept on either the new `SOFT_CLAY_RECOVERY_CHECK …` line (it names exactly which gate rejected, by design) or `RECOVERED_MINE_SOFT_CLAY_BY_INVENTORY_GAIN … cause=RELOAD_AFTER_OBSERVED_MINE` in fresh in-game diag. Do NOT ship anything over Alex's build — read-only review per scope limit.
