# Muse review-loop verdict — Doric's Quest Build 46 / patch-687 (read-only review)

Run: 2026-10-01 ~08:34 EDT. Scope: READ-ONLY — no source edits, no compile, no ship over Alex's builds.

## Verdict: PASS (with advisories, no defects)

### Checks performed (byte-level, via gh API)
- version.txt=687 via API. patch-687.zip + patch-687.hot.json both present on repo; no patch-number reuse; no upload-over-existing.
- Zip structure: 215 entries, root `net/` (200 `.class`, no junk paths — no 341/342 repeat). In-zip version.txt=687.
- Hot chain: doricsquest-46.jar (30,362B, blob sha 4fb3ccb8) sha256 = `c028bb96…42b` == patch-687.hot.json sha256 EXACTLY. The Build-43/683 stale-jar defect is gone — chain INTACT.
- Script classes byte-identical between doricsquest-46.jar and patch-687.zip (DoricsQuestScript.class cmp IDENTICAL).
- Semantics via strings: strategy is Taverley-north-once then Rimmington fallback —
  `TIN_MINE_NORTH_APPROACH … decision=WALK_TO_TAVERLEY_ONCE` (bounded one-shot flag `tinNorthApproachUsed` present in jar),
  rejected `TO_TAVERLEY_TIN_MINE_APPROACH` re-dispatches to `TO_RIMMINGTON_MINE`;
  `TRAINING_ROUTE_RECOVERY decision=USE_F2P_RIMMINGTON_MINE` / `TIN_MINE_BARRIER_RECOVERY decision=USE_F2P_RIMMINGTON_MINE`;
  live tin rock check at Rimmington candidate on ids 11360/11361, unmatched walk rejections → terminal HOLD (safe).
- +0 new game-API references (strings grep: no new client API names beyond existing set).
- Banner: `RUNNING_BUILD={}` dynamic — build number not verifiable statically; new runtime lines ARE present, so live acceptance must come from the NEW runtime lines (TIN_MINE_NORTH_APPROACH / TRAINING_ROUTE_RECOVERY), never the banner.

### Advisories
1. All six ship commits 11:54–12:23Z carry the IDENTICAL message "Doric Build42: try verified Taverley north approach once" while hot.json build fields went 42→44→45→46 — commit messages mislead about which build is live. Only hot.json + in-zip version.txt are trustworthy.
2. Patches 684/685/686 (builds 44/45/46 re-ships) landed between verdicts with no individual review on record; this verdict covers the CURRENT live bytes only.
3. Strategy oscillation continues (Rimmington→West Falador→Taverley→Rimmington, now Taverley-once+Rimmington-fallback) — fine, but each re-ship needs a fresh live proof.

### Live acceptance pending
New runtime lines via Alex's direct runtime reports. Screenshot feed dark since 2026-09-30 17:44:02 EDT (~14.8h); zero DORIC_* frames ever.

2026-10-01 12:34:00Z | Muse review-loop
