# Review verdict — Doric Build 29 / patch-668 (2026-10-01 06:57 EDT run)

**Verdict: PASS** (read-only review; nothing shipped over Alex's build)

## Provenance
- Commit `3d63dd65` (2026-10-01T10:56:13Z) — "Doric Build29: recover tin safely after a client restart"
- Blobs downloaded via git blobs API + Accept: application/vnd.github.v3.raw (binary-safe, exact)
- patch-668.zip blob `7da8ec1f...` (819,791 bytes on disk == tree size); patch-667.zip blob `277581a6...` (819,143 == tree size)
- Working scratch: `~/workspace/goals/tutorial-island-automation/hidden_files/scratch-doric29/`

## Structural checks
- 215 entries, net/-rooted (only non-net/ entries: META-INF/, META-INF/MANIFEST.MF, version.txt) — correct patch-root convention
- version.txt inside patch-668.zip = 668 == repo version.txt (668) — no number reuse
- Class lists identical between 667 and 668 (200 classes each, zero add/remove diff) — no stale-class reship
- Changed classes (5, same set as Builds 26/27/28): DoricsQuestScript, DoricsQuestPlugin, $Frame, $LoginFrame, $Pending
- javap -p signature diff 667->668: only additions — `private boolean bankUnownedTinRecoveryPending`, `private void bankUnownedTrainingTin(Frame)`, `private void recoverUnownedTinHold(Frame)`; zero removals
- BUILD_NUMBER=29 (was 28)

## Delta semantics (Build 29) — restart recovery for untracked training tin
Problem it addresses: `trainingTinOwned` (Build 28's counter) is memory-only, so a client restart resets it to 0 while the bot may still hold script-mined tin — and the surviving TRAIN_MINE_TIN pending from status.properties would latch a dead-end hold.

- Arming: the unproved `TRAIN_MINE_TIN` hold arms `bankUnownedTinRecoveryPending` (once; flag persisted in the proof map — survives hot reload, same idiom as Builds 24/26 recovery flags).
- `recoverUnownedTinHold(Frame)` consumes the flag at entry (iconst_0/putfield — single-shot per arm), logs `[DoricsQuest] RESTART_TIN_RECOVERY quest={} varp31={} tin={} decision=BANK_UNTRACKED_TIN_ONLY` (decision made on observed frame state, not assumption), and sets the `BANK_UNOWNED_TIN_RECOVERY` pending instead of latching terminal HOLD.
- `bankUnownedTrainingTin(Frame)` routes TO_TRAIN_BANK -> OPEN_TRAIN_BANK -> DEPOSIT_UNOWNED_TRAIN_TIN: after a restart the bot can't know which tin is "its" training tin vs quest/baseline material, so banking (not dropping) the untracked tin is the safe choice — nothing is destroyed.
- Failure paths hold with explained messages: "Training bank Open rejected while preserving untracked tin" — the tin is preserved, not dropped, when the bank can't be opened.

## Hot-reload chain — VERIFIED
- patch-668.hot.json: `{"plugin":"doricsquest","patch":668,"hostVersion":1,"build":29,"sha256":"94e7626c..."}` — full 64-char fingerprint
- Fingerprint == sha256 of `patches/doricsquest-29.jar` file bytes (27,350 bytes) — EXACT match
- `doricsquest-29.jar`'s DoricsQuestScript.class is BYTE-IDENTICAL to patch-668.zip's — hot-reload and patch-injection paths carry the same code. Jar-level sha256 convention stable across Builds 27→28→29.

## Findings
- [i] No new game-API calls in the delta; the deposit reuses the existing guarded bank flow + DEPOSIT proof discipline. Additive only.
- [i] Minor: worth Alex confirming the DEPOSIT_UNOWNED_TRAIN_TIN proof clears `trainingTinOwned`-adjacent state so the next TRAIN_MINE_TIN cycle starts from a consistent baseline (can't verify from bytecode alone; no loop risk observed — flag is consumed at entry).

## Acceptance lines (live verification pending — feed dark ~13h)
- `[DoricsQuest] RESTART_TIN_RECOVERY ... decision=BANK_UNTRACKED_TIN_ONLY` after a client restart with held tin
- `BANK_UNOWNED_TIN_RECOVERY` pending + `DEPOSIT_UNOWNED_TRAIN_TIN` proof
- "Build 29 live" judged from NEW runtime lines, never the banner. Verification rests on Alex's direct in-chat runtime reports until screenshots resume.
