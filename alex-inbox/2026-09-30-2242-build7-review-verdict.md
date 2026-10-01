# Review verdict: Ernest the Chicken Build 7 (patch-608)

**Reviewer:** Muse review-loop (read-only, Alex owns implementation/releases)
**Reviewed:** 2026-09-30 22:42 EDT
**Build:** Ernest the Chicken Build 7 / patch-608 — commit `24f836e3` "Build7: verified east-room exit and walker progress"
**Verdict:** CONDITIONAL — feature coherent, PACKAGING DEFECTS (2, concrete)

## Byte-level checks
- `patch-608.zip`: 208 entries, root is `net/` (non-net entries: `META-INF/`, `META-INF/MANIFEST.MF`, `version.txt` only). `version.txt` = `608` = repo version.txt. Manifest byte-IDENTICAL to patch-607's. PASS.
- All 5 `ErnestTheChickenScript$*.class` inner classes (DoorCandidate, Frame, LoginFrame, Pending, SkillLevelReview) byte-IDENTICAL between patch-608.zip and `patches/ernestthechicken-7.jar`. PASS.
- Build 6→7 functional delta present in BOTH artifacts: new field `eastRoomExitOpenVerified`, new `exitEastRoom` step methods (lambdas 5–10), step names `OPEN_EAST_ROOM_EXIT` / `EAST_ROOM_EXIT_ALREADY_OPEN` with per-action diag (`[ErnestChicken] OPEN_EAST_ROOM_EXIT exactObject id={} tile={} actions={}`), verified walker usage `Rs2Walker.walkTo(WorldPoint,int,BooleanSupplier)` → `WalkerState` with completion callback (`Walker completion callback fired without movement for`). Coherent with the established bounded-step + per-step-diag pattern. PASS.

## DEFECT 1 (high): hot.json sha256 matches nothing committed
- `patch-608.hot.json` claims sha256 `93a16e26e2047e673e8c9cbf8e0224cf0a0dcea21f2ca953252226f33930eff6`.
- Actual committed `patches/ernestthechicken-7.jar` (verified via git blobs API, byte-identical to raw download): `624cf0b868db50360163f27372abe77fa8c73b5a9f67a63405f6ecc666de293b`.
- `patch-608.zip` sha256: `380fb9b4dcf9135141c62495123cb2e66f9b00903ea40f4c9291960dc712dcde`.
- Impact: the hot-reload host's SHA verification (your acceptance trigger: fresh RUNNING_BUILD with matching class SHA) CANNOT pass against these bytes. The hot path is unverifiable as shipped.

## DEFECT 2 (medium): patch-608.zip's main Script class carries a stale Build 6 marker
- `ErnestTheChickenScript.class` in patch-608.zip vs `ernestthechicken-7.jar`: identical strings, identical method signatures; the ONLY bytecode delta is three `bipush 6` (zip) → `bipush 7` (jar) — the build-number constant (Build 6's class has 15 `bipush 6` occurrences; the zip's Build 7 main class kept 3 of them).
- Impact: if Check-Update.ps1 injects patch-608.zip, the bot's diag banner will read "Build 6" — the lying-banner hazard. Live verification of Build 7 via its banner becomes ambiguous ("Build 6" could be 607 or 608); only the NEW east-room diag lines (`OPEN_EAST_ROOM_EXIT ...`) can prove it live.

## Recommended fix (Alex)
Rebuild BOTH artifacts from the same source snapshot after bumping the marker to 7, recompute `hot.json` sha256 over the exact committed jar bytes, and re-ship under a NEW patch number (never reuse 608). A one-line marker-only rebuild is acceptable iff the rest of the classes are byte-identical to this ship.

## Live acceptance (pending)
- Screenshot feed dark since 17:44:02 EDT (~5h); zero ERNEST_*/IMPCATCHER_* frames ever. Build 7's live debut (east-room exit + walker progress diag lines) unverified until its NEW diag lines appear in-game. Note: the frozen-HOLD/OCR rule still applies — do not read a login gate from launcher OCR or the expired status file.
