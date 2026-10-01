# Review verdict — Doric Build 30 / patch-669 (2026-10-01 07:10 EDT run)

**Verdict: PASS** (read-only review; nothing shipped over Alex's build)

## Provenance
- Commit `a35edd9` (2026-10-01T11:02:43Z) — "Doric Build30: bank training tin safely"
- Blobs downloaded via git blobs API + Accept: application/vnd.github.v3.raw (binary-safe, exact)
- patch-669.zip blob `0fb924ba...` (819,991 bytes on disk == tree size); patch-668.zip blob `7da8ec1f...` (819,791 == tree size)
- Working scratch: `~/workspace/goals/tutorial-island-automation/hidden_files/scratch-doric30/`

## Structural checks
- 215 entries, net/-rooted (only non-net/ entries: META-INF/, META-INF/MANIFEST.MF, version.txt) — correct patch-root convention
- version.txt inside patch-669.zip = 669 == repo version.txt (669) — no number reuse
- Class lists identical between 668 and 669 (200 classes each, zero add/remove diff) — no stale-class reship
- javap -p signature diff 668->669: only additions — `private boolean tinDropRecoveryPending`, `private void recoverTinDropHold(Frame)`; BUILD_NUMBER 29->30 (in DoricsQuestPlugin); zero removals
- javap -c delta: zero NEW game-API calls (all invoked methods already present in 668) — additive only

## Delta semantics (Build 30) — safe recovery for an unproved DROP_TRAIN_TIN
Problem it addresses: after a hot reload / client restart, a surviving `DROP_TRAIN_TIN` pending whose proof never completed (the drop action never took effect — tin, mining XP and level all unchanged) would previously latch a terminal HOLD.

- Arming (in the reload-restore path, once): `held && phase=="HOLD" && pending.label=="DROP_TRAIN_TIN" && error startsWith "Unproved DROP_TRAIN_TIN" && pending.before.tin>0 && pending.extra==pending.before.tin` → `tinDropRecoveryPending=true`. Arms only when ALL observed tin is the script's own drop target (extra == before.tin), never quest/baseline tin.
- `recoverTinDropHold(Frame)`: consumes the flag at entry (single-shot), runs on the client thread alongside the other recovery handlers (doricConfirm/delayedMine/bankUnowned/manor).
  - Recovery branch (all required): `pending.label==DROP_TRAIN_TIN`, `quest==IN_PROGRESS`, `varp==10`, `frame.tin==pending.before.tin` (zero change), `frame.tin==pending.extra`, `miningXp` and `mining` level unchanged → clears pending + held, phase=`BANK_TRAIN_TIN_AFTER_UNPROVED_DROP`, logs `[DoricsQuest] DROP_RECOVERY exactNoChange=true tin={} decision=BANK_AND_PROVE`.
  - Else → HOLD with a detailed error string (tin/quest/varp/miningXp) + warn `[DoricsQuest] HOLD {}` — conservative: any unexpected state change holds rather than acting.
- Follow-through: with held/pending cleared, tick()'s normal flow routes `bankTrainingTin(frame)` (trainingTinOwned>0, inventory full, mining<15) → TO_TRAIN_BANK → OPEN_TRAIN_BANK → DEPOSIT_TRAIN_TIN. The phase string is diag-only; routing is by the existing state machine. If trainingTinOwned==0 post-restart, Build 29's `bankUnownedTinRecoveryPending` path covers it instead — the two recoveries are complementary, not overlapping.
- Commit message matches behavior exactly: the unproved-drop tin is banked and proved there, nothing destroyed.

## Hot-reload chain — VERIFIED (jar-level convention, stated precisely)
- patch-669.hot.json: `{"plugin":"doricsquest","patch":669,"hostVersion":1,"build":30,"sha256":"fba26f6c...a269c1337"}` — full 64-char fingerprint
- Fingerprint == sha256 of `patches/doricsquest-30.jar` FILE BYTES (27,548) — EXACT match
- `doricsquest-30.jar`'s DoricsQuestScript.class is BYTE-IDENTICAL to patch-669.zip's (`a5ea099b...`) — hot-reload and patch-injection paths carry the same code
- Convention correction (stated once, precisely): hot.json sha256 covers the FULL jar file bytes (plugin+config+script classes), not the script class alone. Prior verdicts' shorthand "== jar bytes == zip script class" conflated the two equalities; the verified facts are (1) hot.json↔jar-file-bytes, (2) jar-script-class↔zip-script-class.

## Findings
- [i] No new game-API calls in the delta; the recovery reuses existing guarded flows + proof discipline. Additive only.
- [i] The arming condition's `extra==before.tin` gate is the load-bearing safety check — it ensures only script-targeted tin triggers the bank recovery. Exact-equality semantics verified in bytecode (if_icmpne chains).

## Acceptance lines (live verification pending — feed dark ~13.5h)
- `[DoricsQuest] DROP_RECOVERY exactNoChange=true tin={} decision=BANK_AND_PROVE` after a hot reload with an unproved DROP_TRAIN_TIN
- `BANK_TRAIN_TIN_AFTER_UNPROVED_DROP` phase + DEPOSIT_TRAIN_TIN proof
- "Build 30 live" judged from NEW runtime lines, never the banner. Verification rests on Alex's direct in-chat runtime reports until screenshots resume.
