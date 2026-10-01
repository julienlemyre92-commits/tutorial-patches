# Muse read-only review verdict: Prince Ali Rescue Build 43 (patch-736)

Reviewed: 2026-10-01 ~15:01 EDT (19:01Z) from published artifacts only. Read-only; no edits over Alex's build.

## Verdict: PASS WITH FINDINGS (low)

## Custody: PASS (clean)
- version.txt=736 (API-decoded); commit cded6034 ships patches/patch-736.zip, patches/patch-736.hot.json, patches/princealirescue-43.jar, patches/princealirescue-plugin-43.jar, source-review/princealirescue-build43/ (Script 2390 lines, Plugin 209, Config 13, README 244).
- patch-736.zip: 221 entries, only non-net/ entries are META-INF/, META-INF/MANIFEST.MF, version.txt (zip root); root version.txt=736.
- hot.json {"plugin":"princealirescue","patch":736,"hostVersion":1,"build":43,"sha256":"7305b2b1...adeca967"} -- EXACT match to princealirescue-43.jar (50,132B) downloaded via git blobs API.
- Hot jar is script-only (3 classes: PrinceAliRescueScript + $Frame + $Pending), all three byte-identical (sha256 cmp) to the zip's classes.
- Banner honest: RUNNING_BUILD via bipush 43 (startup log) and runtimeBuild() bipush 43 -> ireturn.
- PrinceAliRescuePlugin.java byte-identical blob to Build 42 (a4379386); Config/README not diffed (no behavior surface).

## Delta 42 -> 43 (19 diff lines, Script only)
1. BUILD_NUMBER 42 -> 43.
2. New hot-reload rebase (~line 310): if WATER && WATER_LOCAL_SOURCE && waterApproachTarget != null at load, waterApproachStartedAt/waterApproachProgressAt are rebased to now, diag "REBASED_WATER_APPROACH_BUDGET after reload target=... pos=... attempts=...". Prevents an immediate stale-budget HOLD right after a hot reload lands mid-approach.
3. Proximity gates relaxed 8 -> 10 tiles for ALKHARID_GENERAL_STORE and ALKHARID_PALACE_COURTYARD walk checks. This directly CLOSES carried finding b41-F3 (>8 vs walker arrival radius 10) -- the gate now matches Rs2Walker's arrival predicate.

## Findings (read-only, reported not fixed)
- F1 (low/obs): the rebase resets waterApproachProgressAt on EVERY reload. Hot reloads landing repeatedly mid-approach repeatedly defuse the stagnation watchdog; the 90s/30s time bounds get reset while waterApproachAttempts is preserved, so the 4-attempt bound still applies -- bounded, but worth watching if approach time balloons across rapid re-ships.
- O1: commit first line "Prince Ali Rescue Build43: keeps native reconnect active und..." describes the native-reconnect theme while the actual delta is water-approach rebase + 8->10 gates -- message/delta mismatch; commit message still reused per-ship.
- Carried forward (still open, read-only): b41-F1 dead recoverObservedUnavailableWaterQuote code; b41-F2 no coin withdraw before "Cannot buy quest-needed bucket" HOLD (shared w/ ASHES tinderbox flow); b42-F1 one-shot fountain recovery gates on position+inventory, not an observed fountain object in scene; b39 RECOVERED over-claim; b25 dead-tinderbox; b32 expired-source flag.

## Live acceptance: PENDING -- screenshot feed dark since 2026-09-30 17:44 EDT (~21.3h); all stream URLs dead; no live evidence for Build 43's water flow. Verdict is code-review only.
