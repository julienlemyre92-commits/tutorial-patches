# Review verdict — Doric Build 32 / patch-671 (2026-10-01 07:16 EDT run)

**Verdict: PASS** (read-only review; nothing shipped over Alex's build)

## Provenance
- Commit `3d511cf6` (2026-10-01T11:06:15Z) — "Doric Build32: run recovery on logged-in tick"
- Blobs downloaded via git blobs API + Accept: application/vnd.github.v3.raw (binary-safe, exact)
- patch-671.zip blob `fa52de1d...` (820,082 bytes on disk == tree size); patch-670.zip blob `6fe7bb60...` (820,062, baseline for diff)
- Working scratch: `~/workspace/goals/tutorial-island-automation/hidden_files/scratch-doric32/`

## Structural checks
- 215 entries, net/-rooted (only non-net/ entries: META-INF/, META-INF/MANIFEST.MF, version.txt) — correct patch-root convention
- version.txt inside patch-671.zip = 671 == repo version.txt (671) — no number reuse
- Class lists identical between 670 and 671 (200 classes each, zero add/remove diff) — no stale-class reship
- javap -p: 125 methods, zero signature changes 670→671; BUILD_NUMBER ConstantValue int 31→32
- javap -c: 5 methods changed body-only (run, runtimeBuild, guardLegacyHostReload, tick, writeStatus). run/runtimeBuild/guardLegacyHostReload/writeStatus show ZERO String/Field/Method ref changes — pure renumbering noise. The entire semantic delta is inside `tick()`.
- Zero NEW game-API calls (delta refs are script-internal fields/methods, java.lang, slf4j)

## Delta semantics (Build 32) — tin-drop recovery serviced on the logged-in tick
tick() control flow: `loginTick()` true → client-flow branch drains all five recovery flags (doricConfirm → delayedMine → bankUnownedTin → tinDrop → manorExit), writeStatus, return. `loginTick()` false → frame fetch + LOGGED_IN gate → logged-in main tick.

The defect fixed: in Build 31, the logged-in main tick drained ONLY `manorExitRecoveryPending` before the `held` check. A `tinDropRecoveryPending` armed or restored from status.properties while already logged in (loginTick()==false, so the client-flow drain block is skipped) would starve → permanent HOLD.

- Logged-in path now drains `tinDropRecoveryPending` FIRST: `recoverTinDropHold(frame)` → writeStatus → return, ahead of the manorExit check. One flag per tick; tinDrop prioritized over manorExit (matches the client-flow branch's relative order).
- `recoverTinDropHold` still consumes the flag at entry (single-shot) — no re-entry loop; if it HOLDs, the next tick proceeds to manorExit then the normal held path.
- Diag improvement on the four-observation-timeouts path: `phase` is now explicitly set to `HOLD_CLIENT_THREAD` (previously left stale), and writeStatus is called before and after the hold decision so the status file records the transition.

## Hot-reload chain — VERIFIED (jar-level convention)
- patch-671.hot.json: `{"plugin":"doricsquest","patch":671,"hostVersion":1,"build":32,"sha256":"b10f5dbf...82ab6d"}` — full 64-char fingerprint
- Fingerprint == sha256 of `patches/doricsquest-32.jar` FILE BYTES (27,640) — EXACT match
- `doricsquest-32.jar`'s DoricsQuestScript.class is BYTE-IDENTICAL to patch-671.zip's (`e11560c3...`) — hot-reload and patch-injection paths carry the same code

## Findings
- [i] The starvation scenario is real in Build 31's bytecode (logged-in tick had no tinDrop drain) and Build 32 closes it exactly where the commit message says. No behavior change for the already-working client-flow path.
- [i] `HOLD_CLIENT_THREAD` as an explicit phase makes the status file self-diagnosing for the observation-timeout hold; the double writeStatus on that path is redundant but harmless (final write carries the decided state).
- [i] Commit message matches behavior exactly.

## Acceptance lines (live verification pending — feed dark ~13.5h)
- A tin-drop recovery armed while logged in is serviced within one tick (no logout/login needed): `DROP_RECOVERY` log line or the detailed HOLD error, without a client-flow round trip
- `phase=HOLD_CLIENT_THREAD` in the status file on observation-timeout holds
- "Build 32 live" judged from NEW runtime lines, never the banner. Verification rests on Alex's direct in-chat runtime reports until screenshots resume.
