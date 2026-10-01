# Review verdict — Doric Build 27 / patch-666 (2026-10-01 06:35 EDT run)

**Verdict: PASS** (read-only review; nothing shipped over Alex's build)

## Provenance
- Commit `f5063a55` (2026-10-01T10:34:49Z) — "Doric Build27: recover delayed tin-training proof without repeating clicks"
- Blobs downloaded via git blobs API + Accept: application/vnd.github.v3.raw (binary-safe, exact)
- patch-666.zip blob `80ee134e...` (818,956 bytes on disk == tree size); patch-665.zip blob `61fc2755...` (818,819 == tree size)
- Working scratch: `~/workspace/goals/tutorial-island-automation/hidden_files/scratch-doric27/`

## Structural checks
- Both zips: 215 entries, net/-rooted (only non-net/ entries: META-INF/, META-INF/MANIFEST.MF, version.txt) — correct patch-root convention
- version.txt inside patch-666.zip = 666 == repo version.txt (666) — no number reuse
- Class lists identical between 665 and 666 (no adds/removals); no stale-class reship — every changed class carries the new logic
- Changed classes (5, same set as Build 26): DoricsQuestScript, DoricsQuestPlugin, $Frame, $LoginFrame, $Pending
- javap signatures identical across all 5 classes (no API surface change); inner-class bytecode diffs are constant-pool/metadata churn only; Script class delta confined to new string literals + static init (BUILD_NUMBER 26 -> 27)

## Delta semantics (Build 27)
Mirrors the Build-26 `recoverDelayedMineHold` pattern, this time for the tin-training step:
- New label `TRAIN_MINE_TIN` + proof key `LATE_PROOF_TRAIN_MINE_TIN` + diag line
  `[DoricsQuest] LATE_TRAINING_ACTION_PROVED tinBefore={} tinNow={} xpBefore={} xpNow={} actions={} pos={}`
- Recovers a delayed tin-training proof (re-verifies tin count + mining XP) without repeating the click — same no-blind-click discipline as the MINE_ recovery; stays an explained HOLD if the proof doesn't materialize
- Commit message matches behavior exactly

## Hot-reload chain
- `doricsquest-27.jar` (blob `240261da...`, 26,515 bytes): script class byte-identical to patch-666.zip's script class (`8c75a5f5...`)
- patch-666.hot.json decodes to `{"plugin":"doricsquest","patch":666,"hostVersion":1,"build":27,"sha256":"efe6dd0bcad9d97a34c341f115225327e8d1b4a4575c06a72df567abc80cee63"}` — full 64-char fingerprint, **byte-matches the jar exactly**. The Build-26 one-hex-digit-sha typo (63 chars) is FIXED in this build; strict-fingerprint hosts will now accept it

## Acceptance lines (live verification pending — feed dark ~13h)
- `[DoricsQuest] LATE_TRAINING_ACTION_PROVED tinBefore=... tinNow=...`
- Holders must keep judging "Build 27 live" from NEW runtime lines, never the banner
