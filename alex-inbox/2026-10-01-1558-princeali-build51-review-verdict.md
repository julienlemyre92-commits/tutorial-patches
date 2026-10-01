# Review verdict — Prince Ali Rescue Build 51 / patch-744 (read-only)

- Reviewed: 2026-10-01 15:58 EDT (19:58 UTC)
- Commit: 4a0bae2d (2026-10-01T19:56:35Z) — "Prince Ali Rescue Build51: adds one guarded retry for a fresh proven Continue prompt"
- Baseline: Build 50 review (2026-10-01-1552, PASS)
- **Verdict: PASS** (1 minor, 1 hygiene; no blocking defect)

## Chain of custody — ALL PASS

1. patch-744.hot.json declares sha256 `d23be63097008831053f701ef7aba7032b8f7b550a303406f48661d3b8a2f0b3`; downloaded patches/princealirescue-51.jar hashes to the identical value (git blobs API, raw). ✓
2. PrinceAliRescueScript.class byte-identical across patch-744.zip / princealirescue-51.jar / princealirescue-plugin-51.jar: `496bc48ae427fc12e275248ec0133558`. ✓
3. patch-744.zip: 221 files (same count as 743), class entries net/-rooted; root entries are META-INF/ + version.txt only. ✓
4. In-zip version.txt = 744 == repo HEAD version.txt (commit bumps it; contents API still serving cached 743 at read time). ✓
5. javap on the shipped class: `public static final int BUILD_NUMBER = 51;` (+ `private boolean continueRetryUsed`). Not banner-alone. ✓
6. PrinceAliRescuePlugin.java / PrinceAliRescueConfig.java byte-identical 50→51 (in-repo source-review). Only PrinceAliRescueScript.java changed. ✓

## Delta (Build 50 → 51, exact source diff — 32 changed lines)

1. `BUILD_NUMBER` 50 → 51.
2. New `recoverObservedContinueHold(Frame)`: when phase is HOLD_RELOAD_IN_FLIGHT and the reload hold error starts with "Unproved CONTINUE;" and the restored in-flight action is CONTINUE, one guarded retry is issued **iff** all hold simultaneously: logged in, quest IN_PROGRESS, varp==20, observed tile exactly (3126,3244,0), KEY_PRINT in inventory, in dialogue, fresh continue prompt, zero options. It sets continueRetryUsed=true, clears held/error, moves to phase RESUME_ONE_CONTINUE_AFTER_FRESH_PROMPT, clicks Continue once, and re-arms a CONTINUE pending with a 6000ms deadline. On CONTINUE proof, the flag and restored error/action reset.
3. Hot-reload persistence fix: `lastReloadHoldError` and `restoredInFlightAction` now survive restore (Build 50 reset them to error/"" on reload, wiping the recovery predicate); `continueRetryUsed` persisted in the status snapshot + status.properties.

Commit-message claim verified in code: "one guarded retry for a fresh proven Continue prompt" = exactly the above. No tick-logic, walk, click, GE/shop, or dialogue-input changes outside this recovery path.

## Findings

- PASS: retry amplification. Single-shot per persisted lifecycle, reset only on CONTINUE proof; the persisted flag survives hot reloads so a reload between dispatch and proof cannot re-arm it. No infinite-retry loop.
- PASS: recovery runs inside the held block before the soft-clay mine-gain recovery; the two are mutually conditioned (one needs "Unproved CONTINUE;", the other mine-gain state) — no conflict.
- [minor] The recovery gate is exact-tile (3126,3244,0) + varp==20. If the observed player tile is one off (e.g. 3127,3244 beside Keli, or a dialogue nudge moved the player), the recovery never fires and the HOLD persists. Same narrow-gate class as Ernest Build 41's terminal-HOLD regression, though here it gates a recovery path, not the main flow — low severity. Suggest a small radius (e.g. dist<=2 of (3126,3244)) or matching the CONTINUE dispatch position instead of a literal tile.
- [hygiene] source-review/princealirescue-build51/README.md is still the Build 1 handoff doc.

Live verification unavailable: screenshot feed dark since 2026-09-30 17:44 EDT (~22h), no live stream URL. Acceptance lines for Build 51: `RECOVERED_ONE_CONTINUE_RETRY fresh prompt=true` warn log + phase RESUME_ONE_CONTINUE_AFTER_FRESH_PROMPT + CONTINUE proved (provisional on hot-load).

Nothing shipped (review-only; Alex's releases).
