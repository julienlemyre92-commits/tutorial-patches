# Review verdict — Doric Build 31 / patch-670 (2026-10-01 07:15 EDT run)

**Verdict: PASS** (read-only review; nothing shipped over Alex's build)

## Provenance
- Commit `469b4226` (2026-10-01T11:05:03Z) — "Doric Build31: recover verified training tin hold"
- Blobs downloaded via git blobs API + Accept: application/vnd.github.v3.raw (binary-safe, exact)
- patch-670.zip blob `6fe7bb60...` (820,062 bytes on disk == tree size); patch-669.zip blob `0fb924ba...` (819,991 == tree size, baseline for diff)
- Working scratch: `~/workspace/goals/tutorial-island-automation/hidden_files/scratch-doric31/`

## Structural checks
- 215 entries, net/-rooted (only non-net/ entries: META-INF/, META-INF/MANIFEST.MF, version.txt) — correct patch-root convention
- version.txt inside patch-670.zip = 670 == repo version.txt (670) — no number reuse
- Class lists identical between 669 and 670 (200 classes each, zero add/remove diff) — no stale-class reship
- javap -p: 125 methods, zero signature changes 669→670; BUILD_NUMBER ConstantValue int 30→31
- javap -c: 5 methods changed body-only (run, runtimeBuild, guardLegacyHostReload, recoverTinDropHold, writeStatus). Of these, run/runtimeBuild/guardLegacyHostReload/writeStatus show ZERO String/Field/Method ref changes — pure constant-pool renumbering noise. The entire semantic delta is inside `recoverTinDropHold`.
- Zero NEW game-API calls (all delta refs are script-internal fields, java.lang, slf4j)

## Delta semantics (Build 31) — tinBaseline gate + ownership recording on tin-drop recovery
Arming condition (reload-restore path) UNCHANGED from Build 30: `held && phase=="HOLD" && pending.label=="DROP_TRAIN_TIN" && error startsWith "Unproved DROP_TRAIN_TIN" && pending.before.tin>0 && pending.extra.intValue()==pending.before.tin` → `tinDropRecoveryPending=true`.

`recoverTinDropHold` (still single-shot: consumes the flag at entry, runs on client thread):
- NEW gate (bytecode 46–50: `getfield tinBaseline; ifne → fail`): recovery now requires `tinBaseline == 0`. With a nonzero baseline, part of the observed tin predates the training cycle (could be quest tin) — Build 30 would have banked it all as training tin; Build 31 conservatively HOLDs instead.
- On recovery (all required): pending non-null, label DROP_TRAIN_TIN, quest IN_PROGRESS, varp==10, tinBaseline==0, frame.tin==before.tin, frame.tin==extra.intValue() → NEW: `trainingTinOwned = frame.tin` (Build 30 left it unset, so the downstream bank flow had no ownership count), clear pending/held/error, phase=`BANK_TRAIN_TIN_AFTER_UNPROVED_DROP`, log `[DoricsQuest] DROP_RECOVERY exactNoChange=true tin={} decision=BANK_AND_PROVE`.
- Else → HOLD with warn `[DoricsQuest] HOLD {}`; the error string is richer than Build 30 (now includes tin, quest, varp, miningXp, tinBaseline, before.tin, extra).

## Hot-reload chain — VERIFIED (jar-level convention)
- patch-670.hot.json: `{"plugin":"doricsquest","patch":670,"hostVersion":1,"build":31,"sha256":"a409e21c...5fe8e46"}` — full 64-char fingerprint
- Fingerprint == sha256 of `patches/doricsquest-31.jar` FILE BYTES (27,621) — EXACT match
- `doricsquest-31.jar`'s DoricsQuestScript.class is BYTE-IDENTICAL to patch-670.zip's (`ae2ee034...`) — hot-reload and patch-injection paths carry the same code

## Findings
- [i] The `tinBaseline==0` gate is the load-bearing safety fix: recovery now only fires when ALL observed tin is provably the script's own training tin. Any baseline tin → detailed HOLD, never a blind bank.
- [i] `trainingTinOwned = frame.tin` closes the proof loop Build 30 left open: the bank flow's expected-count proof now has a recorded count.
- [i] Commit message matches behavior: the verified-training-tin HOLD path is now recovered safely (or held with a diagnosable error).

## Acceptance lines (live verification pending — feed dark ~13.5h)
- `[DoricsQuest] DROP_RECOVERY exactNoChange=true tin={} decision=BANK_AND_PROVE` followed by BANK_TRAIN_TIN_AFTER_UNPROVED_DROP → DEPOSIT_TRAIN_TIN proof
- Or, with tinBaseline>0: HOLD whose error string names tinBaseline/before.tin/extra
- "Build 31 live" judged from NEW runtime lines, never the banner. Verification rests on Alex's direct in-chat runtime reports until screenshots resume.
