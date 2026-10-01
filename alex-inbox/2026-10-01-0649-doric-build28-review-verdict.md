# Review verdict — Doric Build 28 / patch-667 (2026-10-01 06:52 EDT run)

**Verdict: PASS** (read-only review; nothing shipped over Alex's build)

## Provenance
- Commit `18ed6f71` (2026-10-01T10:47:52Z) — "Doric Build28: drop only owned training tin to avoid bank detours"
- Blobs downloaded via git blobs API + Accept: application/vnd.github.v3.raw (binary-safe, exact)
- patch-667.zip blob `277581a6...` (819,143 bytes on disk == tree size); patch-666.zip blob from tree `03cc56df...` (818,956 == tree size)
- Working scratch: `~/workspace/goals/tutorial-island-automation/hidden_files/scratch-doric28/`

## Structural checks
- Both zips: 215 entries, net/-rooted (only non-net/ entries: META-INF/, META-INF/MANIFEST.MF, version.txt) — correct patch-root convention
- version.txt inside patch-667.zip = 667 == repo version.txt (667) — no number reuse
- Class lists identical between 666 and 667 (200 classes each, zero add/remove diff) — no stale-class reship
- Changed classes (5, same set as Builds 26/27): DoricsQuestScript, DoricsQuestPlugin, $Frame, $LoginFrame, $Pending
- javap -p signatures identical (diff exit 0) — no API surface change; inner-class diffs are constant-pool churn only

## Delta semantics (Build 28)
`trainMining(Frame)` gains a drop-in-the-field branch before the bank-deposit fallback:
- New field `trainingTinOwned` (int) — counts script-acquired training tin, incremented (+1) only inside the `LATE_PROOF_TRAIN_MINE_TIN` handler (proven mining action, not mere observation)
- Entry integrity HOLD: `trainingTinOwned < 0` or `trainingTinOwned > frame.tin - tinBaseline` → explained hold. Owned can never exceed observed gain — good invariant
- Bank open + `trainingTinOwned == 0` → CLOSE_TRAIN_BANK (no owned tin, no reason to be at the bank)
- Drop branch (all four required): `trainingTinOwned > 0` AND inventory full AND mining < 15 AND `tinBaseline == 0` AND `frame.tin == trainingTinOwned` → `Rs2Inventory.drop(438)` (438 = tin ore), then `DROP_TRAIN_TIN` pending (15s) with extra=Integer(count)
- Proof (`isProof` DROP_TRAIN_TIN): `frame.tin == before.tin - extra` AND `counts[]` unchanged → `trainingTinOwned -= extra` (→ 0). Same no-blind-click proof discipline as DEPOSIT
- Commit message matches behavior exactly: only the script's own training tin (never pre-existing/baseline tin, never quest materials) is dropped, replacing a bank detour with a field drop
- Failure paths hold with explained messages (drop rejected, tin count mismatch); DEPOSIT_TRAIN_TIN bank path preserved for all other cases

## Hot-reload chain — VERIFIED (with convention correction)
- patch-667.hot.json: `{"plugin":"doricsquest","patch":667,"hostVersion":1,"build":28,"sha256":"bed0eedb...0ede3"}` — full 64-char fingerprint
- Fingerprint == sha256 of `patches/doricsquest-28.jar` file bytes (26,701 bytes) — EXACT match, chain VERIFIED
- `doricsquest-28.jar`'s DoricsQuestScript.class is BYTE-IDENTICAL to patch-667.zip's — hot-reload and patch-injection paths carry the same code
- CORRECTION to the Build-27 verdict: its fingerprint (`efe6dd0b...`) also matched the full Build-27 jar bytes (26,515), not the script class. Convention is STABLE across 27→28: **hot.json sha256 = full jar file**. The Build-27 verdict's "class-level" reading was wrong (it looked consistent only because the jar's class happened to be byte-identical to the zip's). No action needed — just recording the true convention so future reviews check jar-level.

## Acceptance lines (live verification pending — feed dark ~13h)
- `[DoricsQuest] DROP_TRAIN_TIN` proof / `LATE_TRAINING_ACTION_PROVED` with trainingTinOwned accounting
- "Build 28 live" must be judged from NEW runtime lines, never the banner
