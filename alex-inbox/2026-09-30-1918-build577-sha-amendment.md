# SHA amendment: Build 7 hot artifact re-packaging (Muse review-loop, 2026-09-30 19:18 EDT)

Alex's 19:16:00 EDT commit "Fix Imp Catcher hot artifact packaging for build 7" (d267d8f4)
re-zipped `patches/impcatcher-7.jar` and regenerated `patches/patch-577.hot.json`.

## Verified from the repo (read-only review)

- `patch-577.hot.json` now pins `sha256=e4ac15395eddae1becc6dfbf4b7c94c9463bd1ecfe735e21d5ea5ff803159092`.
- Downloaded `impcatcher-7.jar` from the repo: its actual SHA256 matches the new hot.json exactly.
- All 4 shipped classes (ImpCatcherScript + Frame/HeldFrame/Pending) are BYTE-IDENTICAL between
  the original `patch-577.zip` (19:13:38 commit) and the re-packaged jar (verified per-class SHA256).
  The SHA change is pure re-zipping — zero code change. HeldFrame (Build 7 heartbeat + HOLD
  restore) is intact in the new packaging.

## Acceptance-trigger correction

My 19:15 verdict cited runtime SHA `3521e4e4...` as the acceptance trigger. That was the
pre-fix packaging SHA and is now STALE. The correct live-verification target is the new
hot.json SHA `e4ac1539...` (adapter validates against hot.json). A hot-reload VERIFIED ack
logged before 19:16 EDT would have been against the old packaging — harmless, same classes,
but the post-fix ack should use `e4ac1539...`.

## Verdict

No blocking defects. Packaging fix is clean: same classes, new zip, hot.json consistent.
Feed still dark ~94 min (no frame capture on the Imp Catcher side) — Build 7 still
publishes status.properties only.
