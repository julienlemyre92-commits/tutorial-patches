# Muse read-only review verdict — Prince Ali Rescue Build 29 / patch-722

- **Build:** 29 (Alex-owned front; Muse review-only, no code shipped)
- **Patch:** patches/patch-722.zip · hot: patches/patch-722.hot.json (`build:"29"`, `patch:"722"`, `hostVersion:"1"`)
- **Commit:** fdffbd3cf38b0d7d49fe3af26c71b4818f769ab7 (17:46:28Z / 13:46:28 EDT)
- **version.txt:** 722 (repo + in-zip root, both read `722`)
- **Reviewed:** 2026-10-01 ~13:47-13:52 EDT · source-review/princealirescue-build29 (full source) + shipped bytecode (javap)

## Verdict: PASS (read-only) — ships clean, one real behavior note + 3 minor observations

### Chain-of-custody — PASS
- `hot.json` sha256 `40eacb23…9bd5` == downloaded `princealirescue-29.jar` sha256. Exact match.
- Script classes byte-identical across all three artifacts: `patch-722.zip` == `princealirescue-29.jar` == `princealirescue-plugin-29.jar`
  (`PrinceAliRescueScript`, `$Frame`, `$Pending` — all three byte-equal in each).
- Zip: 221 entries; every class path under `net/` (only extras: `META-INF/` + root `version.txt`); genuine RuneLite client
  manifest (`Main-Class: net.runelite.client.RuneLite`); in-zip root `version.txt` = `722`.
- Banner-accurate: javap on the shipped class shows `bipush 29` at the `[PrinceAliRescue] RUNNING_BUILD={}` log site
  AND `runtimeBuild()` returns 29. No lying-banner risk.
- `PrinceAliRescuePlugin.java` source is byte-identical b28→b29 (0 diff lines); Config unchanged. Script-only change.

### Code delta b28→b29 (36 diff lines, script only)
1. `BUILD_NUMBER` 28→29.
2. New persisted one-shot flag `ashesTreeRetryRecovered` (shutdown `state.put` L188, restore L259 — survives reloads).
3. New bounded cap in `localNormalLogSourceTick` (L1312-1315): `sourceAttempts>=2` → terminal
   `HOLD "Bounded tree Chop down retry limit reached without log gain; player=… tree=…"`.
4. New `recoverObservedTreeChopAfterReload` (L1663-1678), called from the held branch (L346), which fires only when ALL hold:
   one-shot unused · `phase=="HOLD_RELOAD_IN_FLIGHT"` · `restoredInFlightAction=="ASHES_CHOP_NORMAL_TREE"` ·
   `lastReloadHoldError` starts with `"Unproved ASHES_CHOP_NORMAL_TREE;"` · `sourceItem==ASHES`, `sourceGoal==1`,
   `geStage=="ASHES_GET_NORMAL_LOG"` · `LOGGED_IN`, `varp==20`, plane 0 · no logs, tinderbox present, woodcutting axe
   present (inventory or equipped) · live reachable `Tree`/`Chop down` within 16 AND player within 2 tiles of it.
   On fire: `held=false`, `phase="RETRY_TIMED_OUT_TREE_APPROACH"`, `sourceAttempts=max(1,attempts)`, flag set,
   diag `RECOVERED_EXACT_TREE_CHOP_TIMEOUT`.

### Why the recover mechanics check out
- The `"Unproved ASHES_CHOP_NORMAL_TREE;"` prefix exactly matches the pending-timeout HOLD format
  (`hold(f,"Unproved "+pending.action+"; …")`, L428).
- `lastReloadHoldError` snapshots the pre-reload error (L228) *before* restore overwrites `error` with
  `"Reload during …; inspect quest/inventory/scene before resuming"` (L320) — the prefix gate reads the right string.
- `restoredInFlightAction` comes from the persisted `pendingAction` (L318-323); shutdown persists it but explicitly
  does NOT replay in-flight clicks (L220) — no double-chop.
- Termination is airtight: recover grants at most one more dispatch (attempts 1→2); a second proof failure →
  `Unproved` HOLD → reload → recover blocked by the one-shot flag → terminal `HOLD_RELOAD_IN_FLIGHT`. No loop.
- Ordering is safe: `recoverObservedMissingAshesLog` (L1651) requires `phase=="HOLD"` so it cannot preempt this
  recover under `HOLD_RELOAD_IN_FLIGHT`.

### Observations
- **O1 (minor):** `phase="RETRY_TIMED_OUT_TREE_APPROACH"` (L1672) is write-only — zero readers in the source.
  Harmless as a diag marker (it will show in status.properties), but if behavior is ever keyed off it, it needs a consumer.
- **O2 (minor, behavior note):** the `RECOVERED_EXACT_TREE_CHOP_TIMEOUT` log claims "one bounded nearby retry remains",
  but when `sourceAttempts>=2` at recover time the next tick hits the L1312 cap immediately with zero retries —
  e.g. the bronze-axe `Take-axe` dispatch (L1293) already incremented the *shared* `sourceAttempts` counter before the
  chop dispatch. Outcome is still bounded + loud (terminal HOLD with player/tree tiles), so this is log-accuracy and
  counter-sharing, not a stall — but the "one retry" only materializes when attempts==1 at recover.
- **O3 (carried, medium):** the Build 25 dead-tinderbox-recover defect is STILL OPEN in b29: the string
  `"Local ashes source needs tinderbox id=590; none carried or banked"` occurs exactly once (L1639, inside the
  recover's own gate) — the HOLD that emitted it was deleted in b25, so `recoverObservedMissingAshesTinderbox`
  remains dead code; a real mid-prep tinderbox loss still has no shop-path recovery.
- **O4 (cosmetic):** commit message reused verbatim for the 4th straight build
  ("keeps native reconnect active under exact quest HOLD" — describes b26, not b29's tree-retry). The README's
  Build29 section documents the actual change, so this is harmless.

### Live verification — PENDING (feed dark)
Screenshot feed dark since 2026-09-30 17:44 EDT (~20h); no confirmed live stream URL. Per the source-review README,
Alex has live evidence of the b27 ten-tile chop timeout and patch-721 hot-loading on PID 1708 — acceptance of b29's
new runtime lines rests on Alex's runtime report. Watch for: `RECOVERED_EXACT_TREE_CHOP_TIMEOUT`,
`phase=RETRY_TIMED_OUT_TREE_APPROACH` in status, `ASHES_NORMAL_LOG_CHOP_DISPATCH … attempt=2`, and the bounded-limit
`HOLD "Bounded tree Chop down retry limit reached without log gain"`.

*No code shipped by this loop — Alex owns Prince Ali Rescue implementation/releases.*
