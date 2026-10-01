# Review verdict — Doric Build 26 / patch-665 (2026-10-01 06:30 EDT run)

**Verdict: PASS** (read-only review; nothing shipped over Alex's build)

## Provenance
- Commit `27531ba5` (2026-10-01T10:29:19Z) — "Doric Build26: recover delayed mining proof without repeating clicks"
- Blobs downloaded via git blobs API + Accept: application/vnd.github.v3.raw (binary-safe, exact)
- patch-665.zip blob `61fc2755...` (818,819 bytes on disk == tree size); patch-664.zip blob `00e148fb...` (818,403 == tree size)
- Working scratch: `~/workspace/goals/tutorial-island-automation/hidden_files/scratch-doric26/`

## Structural checks
- Both zips: 215 entries, net/-rooted (only non-net/ entries: META-INF/, META-INF/MANIFEST.MF, version.txt) — correct patch-root convention
- version.txt = 665 inside patch-665.zip == repo version.txt (665) — no number reuse
- Class lists identical between 664 and 665 (no adds/removals)
- BUILD_NUMBER 25 -> 26 (Script + Plugin bytecode)
- Changed classes (sha256): DoricsQuestScript, DoricsQuestPlugin, $Frame, $LoginFrame, $Pending. Inner classes javap-identical (debug/metadata churn only); Plugin javap-identical (metadata churn only)
- **No stale-class reship** (unlike the patch-658 incident): every changed class carries the new logic

## Delta semantics (Build 26)
Purely additive, mirrors the Build-24 `doricConfirmRecovery` pattern:
- New field `delayedMineRecoveryPending` (boolean) + method `recoverDelayedMineHold(Frame)`
- Arming: `held && phase=="HOLD" && pending.label startsWith "MINE_" && error startsWith "Unproved MINE_"` — the dead-end unproved-mining HOLD
- Recovery: consumes the flag (single-shot per arm), then **re-verifies observed state** before acting: `frame.count(pending.item) > pending.before.count(pending.item)` AND `frame.miningXp > pending.before.miningXp`. If both hold, completes the pending *without re-clicking* (pending=null, held=false, error cleared, phase set), logs `[DoricsQuest] LATE_MINE_ACTION_PROVED label={} item={} gained={} xpBefore={} xpNow={} pos={}`. If not proved: stays explained-HOLD (warn-logged), no blind click, no infinite loop
- Flag persisted into and restored from the proof/status map — survives hot reload (matches Alex's established pattern)
- Bounded: re-arm requires the dead-end hold condition to re-form

## Hot-reload chain
- `doricsquest-26.jar` (blob `46959355...`, 26,377 bytes): script class byte-identical to patch-665.zip's script class (`191e2188...`)
- patch-665.hot.json decodes to `{"plugin":"doricsquest","patch":665,"hostVersion":1,"build":26,"sha256":"0fcea48a...d4bb6b0"}` — points at the right jar, BUT the sha256 value is **one hex digit short (63 chars, `...d4bb6b0` vs the jar's real `...d4bbb6b0`)**. A host that strictly verifies the fingerprint would reject the patch; the intended artifact is correct and byte-consistent with the patch zip. Characterizes and narrows the open Build-25 concern (previously: "hot.json sha matches nothing") — the fingerprint is a near-miss typo, not a wrong artifact

## Notes for Alex
- [m] hot.json sha256 off by one hex digit — re-emit with the full 64-char jar hash or the host may reject a strict check
- Commit message matches behavior exactly ("recover delayed mining proof without repeating clicks")

## Acceptance lines (live verification pending — feed dark ~12h47m)
- `[DoricsQuest] LATE_MINE_ACTION_PROVED label=...`
- Holders must keep judging "Build 26 live" from NEW runtime lines, never the banner
