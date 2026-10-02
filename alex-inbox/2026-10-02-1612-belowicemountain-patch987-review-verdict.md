# Below Ice Mountain patch-987 review verdict — Muse (read-only reviewer)

- **Date/time:** 2026-10-02 ~16:12 EDT (run tutorial-island-review-loop)
- **Patch:** 987, commit a43d4056 ("Install a verified same-PID recovery UI host", Alex, 16:03:45 EDT)
- **Candidate:** build 115, generation 7, changedCount 20 (source-review/hot-recovery987/candidate.json; `published:false`/`liveValidated:false` at review time — pre-publish snapshot, patch IS published now)
- **Role:** read-only review. Alex owns BIM implementation/releases — nothing shipped by this loop.

## Verdict: PASS (read-only; acceptance pending live)

### Custody (all verified locally, AIR TIGHT except noted)
- `patches/patch-987.zip` blob via git blobs API: 1,992,218 bytes, 466 entries, `net/`-rooted (457 net entries). In-zip `version.txt`=987, matches repo `version.txt`=987.
- `BelowIceMountainScript.BUILD_NUMBER` = 115 (javap ConstantValue, confirmed) — matches candidate build claim.
- `quest-recovery-hot/red-bead-ui.jar` sha256 = `4ed9e9172a8e3a7d886333118107ab6ca321d086f96995823904e993417cff24` — matches recoveryUiSha256 claim exactly.
- `quest-services-hot/provider-bundle-1.jar` sha256 = `2afc697344bc11acf05638ce6be3dc01df4965c4d0f49e804d91420ceeb72ece` — matches artifactSha256 claim; byte-identical to 985/986 (gen 7 jar unchanged).
- `quest-services-hot/manifest.sha256` content = `4b30fb5f...` matches claim; `parent-abi.sha256` content = `637d3d...` matches claim (ABI-compatible hot swap).
- No version reuse: `patch-987.zip` added (not overwritten); `version.txt` 986->987 in the same single-purpose commit.
- `changedCount` 20 reconciles: 15 changed classes + 2 added + 1 removed + `red-bead-ui.jar` + `version.txt`.
- NOTE: `classSha256`/`sha256`/`implementationMarker` claim scheme still not independently reproducible from here — same gap as the 986 verdict. Structural + content claims all verify.

### Changed files (986 -> 987, byte diff)
- Changed (15): `belowicemountain/BelowIceMountainPlugin{,$1}`, `BelowIceMountainScript{,$3,$GuardianPathPlan,$MarketSaleView,$QuestGeBuyer,$QuestGeBuyer$Frame,$QuestGeBuyer$Offer,$QuestGeBuyer$Outcome,$QuestGeBuyer$Phase,$QuestGeBuyer$Result,$TrainingStyleControl}`, `questcommon/recovery/RedBeadColdRecovery`, `RedBeadMicrobotUi`.
- Added (2): `RedBeadMicrobotUi$1`, `RedBeadMicrobotUi$Probe`.
- Removed (1): `RedBeadMicrobotUi$ActionWidget`.
- Added non-net (1): `quest-recovery-hot/red-bead-ui.jar` (15.5KB, single class `recovery/hot/RedBeadUiV1`).

### Mechanism (strings-level, no source — read-only)
- `RedBeadMicrobotUi` now dynamically loads `quest-recovery-hot/red-bead-ui.jar` at runtime (`loadClass`, path under `user.home`, i.e. the bundle dir) — the "same-PID recovery UI host": the recovery UI lives in a separate jar hot-loaded into the running JVM, no jar reload.
- New nested `Probe` + methods `preDispatchOpen` / `preDispatchRejected` / `preDispatchRejects` / `resumePreDispatchAfterUiReload`: a pre-dispatch gate around the GE-open dispatch. New diag string: `'recovery input gate lost before GE open` — fail-closed REJECT when the input gate is lost before dispatch (the disconnect-modal class of death the 15:37 escalation hit). This is Alex's modal-dismissal leg answer.
- `RedBeadColdRecovery`: the new strings are only the preDispatch hooks; Script-side `QuestGeBuyer` offer/outcome/result/phase + `MarketSaleView` + `TrainingStyleControl` + `GuardianPathPlan` + `Script$3` changes are code-only (no new/removed string literals in Script) — likely wiring the buyer flow through the preDispatch gate.
- UI internals refactored: `ActionWidget` gone, `$1` + `Probe` in its place.

### Findings (concrete, mechanism-first)
- [LOW] New classloading surface: if `red-bead-ui.jar` is missing from the bundle dir on the PC (stale Check-Update injection, manual install gap), `loadClass` throws and the recovery UI host dies at first use. Watch diag for ClassNotFound/NoClassDefFound after hot-load.
- [LOW] `resumePreDispatchAfterUiReload` implies a re-probe on UI reload — if the probe itself rejects persistently (e.g. modal never dismissible), the gate parks the GE step again; the 15:56 verdict's budget-orphan notes carry to this gate too. Watch for repeated `preDispatchRejected` lines.
- [INFO] No new observable script-stage diag strings in this patch (code-only refactors); live acceptance keys are the build marker + preDispatch lines, not new step names.
- Carried: 15:44 disconnect-escalation verdict stands; 16:01 live read cleared the modal. This patch is the code answer to that escalation — acceptance is live-only.

### Acceptance criteria (pending — not yet observed)
- RUNTIME BUILD 115 marker on stream; `preDispatchOpen`/`preDispatchRejected`/`resumePreDispatchAfterUiReload` diag lines proving the patched classes are active; a GE-open dispatch passing the preDispatch gate; BEAD-step progression past the GE-open hold.
- A live stream check was spawned this run (~16:10 EDT) to look for Build 115; result pending.

### Action
- No ship, no RESTART from this loop (read-only). Decision returned to Alex/Julien as usual.
