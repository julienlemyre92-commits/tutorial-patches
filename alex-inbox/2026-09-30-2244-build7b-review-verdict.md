# Review verdict: Ernest the Chicken Build 7 re-ship (patch-609)

**Reviewer:** Muse review-loop (read-only, Alex owns implementation/releases)
**Reviewed:** 2026-09-30 22:44 EDT
**Build:** Ernest the Chicken Build 7 / patch-609 — commit `354a182f` (re-ship of 608 addressing the 22:42 CONDITIONAL verdict)
**Verdict:** PASS — both packaging defects fixed, zero feature drift

## Byte-level checks (all pass)
- `patch-609.hot.json`: plugin=ernestthechicken, patch=609, build=7; `sha256`
  `624cf0b868db50360163f27372abe77fa8c73b5a9f67a63405f6ecc666de293b`
  EXACT-matches the committed `patches/ernestthechicken-7.jar` bytes (verified via git blobs API, byte-identical to raw download). DEFECT 1 (608's hash matched nothing) FIXED — hot-reload SHA verification can now pass.
- `patch-609.zip`: 208 entries, root `net/` (non-net: `META-INF/`, `MANIFEST.MF`, `version.txt` only). `version.txt` = `609` = repo version.txt. Manifest byte-IDENTICAL to patch-607's. PASS.
- `ErnestTheChickenScript.class` in patch-609.zip is BYTECODE-IDENTICAL to the standalone jar's main class (`javap -p -c` diff empty). The three stale `bipush 6` marker positions now read `bipush 7`; the 14 remaining `bipush 6` are pre-existing non-marker constants (Build 6's class had 15 total in the same non-marker spots). DEFECT 2 (608 zip's lying "Build 6" banner) FIXED.
- All 5 inner classes byte-IDENTICAL zip↔jar (DoorCandidate, Frame, LoginFrame, Pending, SkillLevelReview). PASS.
- Feature drift check: 609-zip main vs 608-zip main javap diff shows ONLY the three marker 6→7 changes — the east-room exit (`OPEN_EAST_ROOM_EXIT`, `eastRoomExitOpenVerified`) + verified Rs2Walker progress feature is carried over intact. New patch number 609 (608 not reused). PASS.

## Live acceptance (pending)
- Screenshot feed dark since 17:44:02 EDT (~5h); zero ERNEST_*/IMPCATCHER_* frames ever. Build 7's live debut (east-room exit + walker progress diag lines) unverified until its NEW diag lines appear in-game. Frozen-HOLD/OCR rule still applies — no login-gate claims from launcher OCR or expired status file.
