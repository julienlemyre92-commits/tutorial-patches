# Review verdict — Prince Ali Rescue Build 52 / patch-745 (read-only)

- Reviewed: 2026-10-01 16:02 EDT (20:02 UTC)
- Commit: 352ea51a (2026-10-01T19:58:xxZ) — "Prince Ali Rescue Build52: recovers exact reload hold for one fresh Continue retry"
- Baseline: Build 51 review (2026-10-01-1558, PASS) — Build 52 shipped ~1 min before that verdict landed; reviewed in the same run
- **Verdict: PASS** (closes a real trigger gap in Build 51's recovery; no blocking defect)

## Chain of custody — ALL PASS

1. patch-745.hot.json sha256 `8f54a22c...` == princealirescue-52.jar bytes. ✓
2. PrinceAliRescueScript.class byte-identical across patch-745.zip / princealirescue-52.jar / princealirescue-plugin-52.jar: `728e4e6950c9a0fc`. ✓
3. patch-745.zip: 221 files (same count), net-rooted class entries; in-zip version.txt = 745. ✓
4. javap on the shipped class: `public static final int BUILD_NUMBER = 52;`. Not banner-alone. ✓
5. PrinceAliRescuePlugin.java / PrinceAliRescueConfig.java byte-identical 51→52. Only the Script changed. ✓

## Delta (Build 51 → 52, exact source diff — one hunk in recoverObservedContinueHold)

Build 52 generalizes the recovery trigger:
- `savedUnprovedContinue` = lastReloadHoldError starts "Unproved CONTINUE;" AND restoredInFlightAction=="CONTINUE" (Build 51's path, unchanged).
- NEW `explicitReloadedContinue` = current error starts "Reload during CONTINUE;".
- Recovery now fires when either is true (previously only the first).

This closes a genuine gap: on a first-time reload during an in-flight CONTINUE, Build 51's reload path sets error/lastReloadHoldError to "Reload during CONTINUE; ..." — which never satisfies the "Unproved CONTINUE;" predicate, so Build 51's recovery could never fire there. Build 52 covers both reload paths. The widening affects only WHICH holds may attempt recovery; the safety gate is unchanged (logged-in, IN_PROGRESS, varp==20, exact tile (3126,3244,0), KEY_PRINT, in-dialogue, fresh continue prompt, zero options) and the single-shot persisted continueRetryUsed flag still bounds it. PASS on retry amplification.

## Findings (carried + new)

- Carried from Build 51 [minor]: exact-tile (3126,3244,0) + varp==20 gate — one-tile drift means the recovery never fires and HOLD persists. Unchanged in Build 52.
- [hygiene] source-review/princealirescue-build52/README.md is again the Build 1 handoff doc (dirs 50, 51, 52 all carry it).

Live verification unavailable: screenshot feed dark since 2026-09-30 17:44 EDT (~22h), no live stream URL. Acceptance lines for 51/52: `RECOVERED_ONE_CONTINUE_RETRY fresh prompt=true` + RESUME_ONE_CONTINUE_AFTER_FRESH_PROMPT + CONTINUE proved (provisional on hot-load).

Nothing shipped (review-only; Alex's releases).
