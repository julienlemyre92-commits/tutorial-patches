# Read-only review verdict: Prince Ali Rescue Build 19 / patch-712

Reviewer: Muse (read-only; Alex owns implementation/releases).
Build: Prince Ali Rescue Build 19 / patch-712, commit ac9e9ba (2026-10-01T16:29:45Z). Repo version.txt=712.
Commit adds: patches/patch-712.zip, patches/patch-712.hot.json, patches/princealirescue-19.jar, patches/princealirescue-plugin-19.jar, source-review/princealirescue-build19/{README.md, PrinceAliRescueConfig.java, PrinceAliRescuePlugin.java, PrinceAliRescueScript.java}.

## Verdict: ONE CONCRETE DEFECT (medium-low) — otherwise PASS

### Chain of custody: PASS
- patch-712.hot.json sha256 `ae7cd6e52eab90a4c73337592c66704af04e7e1be4e53b7053293f0af5493ef9` == downloaded princealirescue-19.jar, verified locally.
- All three in-zip script classes (PrinceAliRescueScript / $Frame / $Pending) byte-identical between patch-712.zip, the hot jar, and princealirescue-plugin-19.jar (sha256-checked per file).
- patch-712.zip: 221 files, 218 `net/`-rooted entries; in-zip version.txt=712 matches repo version.txt; META-INF/MANIFEST.MF is the genuine client manifest (Main-Class: net.runelite.client.RuneLite); built with `zip`, no `jar cf` contamination.
- Fresh patch number 712; not uploaded over an existing patch; no version reuse.
- `javap -constants` on the shipped class: `BUILD_NUMBER = 19`; new Build18/19 log strings (`ONION_APPROACH_STEP`, `ONION_GATE_OPEN_DISPATCH`, `RECOVER_ONION_PICK_BY_ROUTING_TO_REACHABLE_SIDE`, `OPEN_ONION_GATE`) present in the shipped class.
- PrinceAliRescuePlugin.java and PrinceAliRescueConfig.java byte-identical to Build 18's (script-only change).

### Code 18→19: CONCRETE DEFECT
The delta is ~8 lines in `recoverObservedOnionPickHold` (source lines ~1226-1242): it adds an `exactPriorPickHold` second disjunct covering a hot reload that landed during an in-flight `PICK_DYE_ONION` (restoredInFlightAction=="PICK_DYE_ONION" + exact error "Reload during PICK_DYE_ONION; inspect quest/inventory/scene before resuming"), and clears `restoredInFlightAction` on recovery. Intent mirrors the Build13/14 wool pattern. But the new branch can never fire:
- `restoreReloadState` (lines ~273-279) sets `phase="HOLD_RELOAD_IN_FLIGHT"` whenever it produces exactly the (`restoredInFlightAction=="PICK_DYE_ONION"`, `error=="Reload during PICK_DYE_ONION; ..."`) state.
- `recoverObservedOnionPickHold` gates the ENTIRE method on `"HOLD".equals(phase)` (line 1232).
- Therefore the second disjunct's precondition is unsatisfiable: that state exists only with phase `HOLD_RELOAD_IN_FLIGHT`, never `HOLD`. Dead code.
- The wool analogs got this right: `recoverObservedWoolSpinAfterReload` gates on `"HOLD_RELOAD_IN_FLIGHT".equals(phase)` (line 1246), and `recoverObservedWoolDescentHold`'s `exactWrappedReloadHold` branch gates on `"HOLD_RELOAD_IN_FLIGHT".equals(phase)` (lines 1273-1274).
- Impact: a hot reload landing inside the 9s `PICK_DYE_ONION` pending window leaves a terminal `HOLD_RELOAD_IN_FLIGHT` with no matching recovery gate (every other gate in the held chain is WOOL- or dye-quote-scoped). Loud, bounded, tiny window — but it is exactly the case Build 19 was shipped to fix. Suggested fix: gate the second disjunct on `HOLD_RELOAD_IN_FLIGHT` like the wool gates, or hoist the phase check into the individual disjuncts.
- Verified reachable: the FIRST disjunct (live "Unproved PICK_DYE_ONION;" hold) is correct — `hold()` sets `phase="HOLD"` (line 1538) — and the single-shot `onionFailedAttemptRecovered` latch, observed-state gate, and no-replay behavior are sound.

### Defect hunt (rest of 18→19)
- Considered and cleared: `restoredInFlightAction=""` clearing on recovery (prevents stale in-flight matching later — correct), Plugin/Config drift (none), version reuse (none).

### Live acceptance pending
`ONION_APPROACH_STEP` / `ONION_GATE_OPEN_DISPATCH` / `RECOVER_ONION_PICK_BY_ROUTING_TO_REACHABLE_SIDE` runtime lines, the reachable-side pick flow, gate crossing, the Aggie craft with ==5 coin delta, and the Build18/19 hot-load path. Screenshot feed dark since 2026-09-30 17:44 EDT; no confirmed live stream URL. Same pending acceptance as Builds 12-18.

Reviewed 2026-10-01 ~12:31 EDT by Muse, read-only scope.
