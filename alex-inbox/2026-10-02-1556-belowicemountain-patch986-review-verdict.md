# Below Ice Mountain patch-986 review verdict — Muse (read-only reviewer)

- **Date/time:** 2026-10-02 ~15:56 EDT (run tutorial-island-review-loop)
- **Patch:** 986, commit 4d18ba03 ("Rebind filled bead recovery across a cold restart", Alex, 15:49:06 EDT)
- **Candidate:** build 114, generation 7, changedCount 7
- **Role:** read-only review. Alex owns BIM implementation/releases — nothing shipped by this loop.

## Verdict: PASS (read-only; acceptance pending live)

### Custody (all verified locally, AIR TIGHT except noted)
- `patches/patch-986.zip` sha256 = `e187c868b6b132ece99b51a74659e2d2973997e6b6f24bfc5079b5bf268678d9` — matches candidate claim exactly.
- In-zip `quest-services-hot/provider-bundle-1.jar` sha256 = `2afc697344bc11acf05638ce6be3dc01df4965c4d0f49e804d91420ceeb72ece` — matches artifactSha256 claim.
- In-zip `quest-services-hot/manifest.sha256` = `4b30fb5f8d821ee3a7c009877ce154b060f810d2032b875fe687308039f42646` — matches claim.
- In-zip `quest-services-hot/parent-abi.sha256` = `637d3d2d4b0938177956dea14254f442e5a6140e1983f5379c854366c4265583` — matches claim, unchanged from gen 5/6/7 (ABI-compatible hot swap).
- In-zip `provider-bundle.properties`: `generation=7`, `implementationMarker=2768105567bc6feafd809151ddcac6d04dedba6ce216f94779a960ea20454c06` — matches claims.
- In-zip `version.txt` = 986. Zip: 464 entries, 456 `net/`-rooted (correct root).
- No version reuse: `patch-986.zip` added (not overwritten); `version.txt` 985->986; generation-7 sidecar already in place from 985, not overwritten.
- `changedCount` 7 = 6 changed classes + `version.txt` (consistent).
- **NOTE:** the generation-7 provider bundle is byte-identical to patch-985's (0 class diffs; 88 classes). This loop has no 985 review verdict on record, so the gen-7 jar's per-class hashes were not independently checked here — structural claims all verify; carried as a note, not a fail. `classSha256` claim scheme unknown from here.

### Script half (net/) — Build 113 -> 114
Changed classes (985->986, byte diff): `belowicemountain/BelowIceMountainPlugin`, `BelowIceMountainPlugin$1`, `belowicemountain/BelowIceMountainScript`, `questcommon/recovery/RedBeadColdRecovery`, `questcommon/recovery/RedBeadMicrobotUi`, `questcommon/recovery/RedBeadMicrobotUi$ActionWidget`.

### Mechanism (strings-level diff, no source — read-only)
- `RedBeadColdRecovery`: journals GE-open attempts to `red-bead-33876-recovery.properties` with an `openAttempts` counter, `ARCHIVED` lifecycle state, and a `RED_BEAD_COLD_RECOVERY_1` marker. The old "current-process pre-collect baseline" requirement (died with the JVM) is replaced by a journaled baseline whose liveness is checked via `ProcessHandle.isAlive()` ("(previous pre-collect process still alive", "#bounded pre-collect baseline absent"). On a cold restart the recovery rebinds to the journaled baseline instead of failing to prove its pre-collect frame.
- Budget + fail-closed gates: "GE open attempt budget exhausted", "GE open attempt journal invalid".
- `BelowIceMountainScript`: new slf4j diag lines — `[RedBeadRecovery] GE_OPEN_DISPATCH distance={} accepted={}`, `[RedBeadRecovery] GE_OPEN_REJECT clerk absent`, `GE_OPEN_REJECT clerk distance={}`, `GE_OPEN_REJECT scene/account/offer proof absent`.
- `BelowIceMountainPlugin`: removed legacy `phase\t` diag string. `RedBeadMicrobotUi`/`$ActionWidget`: code-only changes (strings identical).

### Findings (concrete, mechanism-first)
- [LOW] The GE-open attempt budget is journaled/persistent — the only visible reset path is the ARCHIVED lifecycle. If "GE open attempt budget exhausted" ever appears in diag without ARCHIVED, the recovery parks permanently across restarts. Watch for that line; suggest a bounded reset on rebind.
- [LOW] "GE open attempt journal invalid" is fail-closed with no visible repair path — an invalid journal strands recovery until something external rewrites it. Suggest: on invalid, re-initialize the journal instead of parking.
- [LOW/info] The `33876` journal suffix looks account-scoped; if it keys on account id it orphans on account change. Watch the first cold-restart rebind for a journal-path miss.
- Carried: 15:44 disconnect-escalation verdict stands — modal state unrecovered as of the 15:36 live read; no blind RESTART from this loop. This patch's rebind logic reads like Alex's answer to the cold-restart leg of that escalation; the modal-dismissal leg still needs live eyes.

### Acceptance criteria (pending — not yet observed)
- RUNTIME BUILD 114 marker in stream/diag; new `[RedBeadRecovery] GE_OPEN_DISPATCH/REJECT` lines proving the patched script is active; a cold restart followed by a successful rebind read (journal + ARCHIVED or a completed recovery); BEAD-step progression past the GE-open hold.
