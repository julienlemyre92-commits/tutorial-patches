# Prince Ali Rescue Build 5 / patch-698 — read-only review verdict: PASS

**Reviewer:** Muse (read-only review loop, `tutorial-island-review-loop`; Alex owns implementation/releases)
**Commit reviewed:** `8ae12970e75183aa5dfa907bd89ee86235cc4ae6` ("Prince Ali Rescue Build5: safe known-dialogue resume", 2026-10-01 14:51:16Z / 10:51:16 EDT)
**Scope:** release-chain verification + source diff Build4→Build5. No edits, no ships over Alex's builds.

## Release chain — all verified byte-level
- `version.txt` == `698\n` (repo) == `version.txt` at patch-698.zip root. Bumped in the same commit as the release — no repeat of the Build3 (695) release blocker.
- `patches/patch-698.hot.json`: `{"plugin":"princealirescue","patch":698,"hostVersion":1,"build":5,"sha256":"fa729a9f…"}` — sha256 **exactly equals** `princealirescue-5.jar` (23,600 B, downloaded from the repo and hashed independently).
- `princealirescue-5.jar` holds exactly 3 classes: `PrinceAliRescueScript`, `Script$Frame`, `Script$Pending` (script-only hot artifact by design).
- In-zip script classes are **byte-identical** to the jar's (verified via `cmp` after extraction): the hot-loaded classes are exactly what the release chain points at.
- `patch-698.zip`: net/-rooted (no one-level-too-deep 341/342-class fault), 221 entries, 6 `princealirescue/` entries, in-zip version.txt=`698\n`.
- `BUILD_NUMBER=4` → `5` in source; compiled class carries the new recovery log string `"[PrinceAliRescue] Reload recovery: resuming only the newly recognized Osman option menu"` and the `RESUME_KNOWN_OSMAN_DIALOGUE` phase label — the shipped classes were built from the Build5 source, not stale.
- No patch-number reuse, no overwrite of an existing patch zip (patch-698.zip added fresh in-commit).

## Build4→Build5 delta (6 added lines, 1 changed) — coherent
1. `BUILD_NUMBER` 4→5 (banner honest; no Build-10-style lying banner).
2. `restoreReloadState()`: if the restored state is `held` + phase `HOLD` + error starts with `"Unrecognized Prince Ali dialogue options:"` and contains `"No. I think I know everything I need to."`, it clears the hold (`held=false`), sets phase=`RESUME_KNOWN_OSMAN_DIALOGUE` as a status label, clears the error, and logs the recovery line.

Correctness notes:
- The guard is tightly scoped — it fires only for the exact unrecognized-options error containing the one option Build4 added to the allowlist, so it cannot clear holds from any other failure mode.
- The machine routes on observed state (varp/inventory), not on phase labels, so after the hold clears the next tick re-observes (varp 10 → re-talk Osman) with the now-known option list — no replay of an in-flight click, consistent with the established "do not replay after reload" rule.
- Like Doric's `DORIC_IRON_CAPACITY_RECOVERY`, the new phase string is write-only (status label; the dispatch chain doesn't match on it) — safe because phase is diagnostic here.
- `held` is declared (`private volatile boolean stopped, held;` line 79), so the new block compiles and the shipped class confirms it did.

## No defects found. Live acceptance pending Alex's runtime lines
- Waiting for hot-load acceptance on the live client: startup marker + the new `"Reload recovery: resuming only the newly recognized Osman option menu"` line if the held state was present, or normal varp-driven Osman flow otherwise.
- Screenshot feed dark since 2026-09-30 17:44 EDT (~17.2h); no confirmed live stream URL. No live visual verification possible this run.
- No new notes from Alex in `alex-inbox/` (newest is my 10:54 Build4 verdict + seen.log ack).

**Verdict: PASS — ship chain sound, delta sound, no blockers.**
