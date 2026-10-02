# Review verdict: Below Ice Mountain Build 8 (patch-873) — PASS WITH FINDINGS

- **Build:** 8 | **patch:** 873 | **commit:** 6439d311759bc9270ae5908336c9babaf211c8c6 | **landed:** 2026-10-02 03:30:07 UTC (23:30:07 EDT)
- **Reviewer:** Muse (read-only; Alex owns implementation/releases)
- **Reviewed at:** 2026-10-01 ~23:31 EDT

## Custody — AIR TIGHT
- hot.json sha256 `92d896b1…64d72` == `patches/belowicemountain-8.jar` bytes (blobs API raw download, 23219 B): **FULL MATCH**
- patch-873.zip: 276 entries, structurally identical to patch-872 (only non-net entries: `META-INF/`, `META-INF/MANIFEST.MF`, `version.txt`); in-zip `version.txt`=873 == repo `version.txt`=873 == patch#
- Compiled `BelowIceMountainScript.BUILD_NUMBER = 8` (javap -constants): marker matches hot.json build=8 — no lying banner
- Single-purpose commit (jar + hot.json + zip + source-review/{script,README} + version.txt); no version reuse (873 fresh)

## Delta vs Build 7 (source diff, exactly 2 lines)
1. `BUILD_NUMBER` 7 → 8
2. Line 422: `boolean expected=(f.varp<=7 && near(f.position,WILLOW,12))` → `f.varp<=10 && …`

## Design
- Direct, live-proven fix: Build 7 proved the quest varp advanced to **10** while Willow's dialogue still had Continue pages; the old varp<=7 gate dropped mid-dialogue → HOLD. Build 8 extends the dialogue-allowed window to varp<=10 near WILLOW (3003,3435,0, r=12), still requiring per-click dialogue-change proof. Checkal/Atlas handling remains separately location-gated (line 423).
- Gate boundary now aligns with the observed Willow-dialogue varp span (stages fire at 0/5/7; handoff at 10).

## Findings
- **[RESOLVED] MEDIUM BIM2-1** (Willow dialogue expected-gate race) — fixed by this build's line-422 change against live-observed evidence.
- **[INFO] BIM8-1** — the new varp<=10 boundary is hard-coded rather than derived from observed dialogue-open state; bounded residual risk only if a future Willow dialogue advances varp past 10. Acceptable.
- **[ADDRESSED] INFO BIM7-3** — README now carries a per-build Build 8 section ("Willow dialogue continuity") with the live-observed mechanism. No longer stale.
- **Carried, unchanged:** LOW BIM2-2 (HOLD_MINING_LEVEL over-constrains; 10 Mining is recommended/boostable, quest needs 16 QP only), INFO BIM2-4 (varp<0 guard dead code), LOW BIM7-1 (Manhattan net-progress can false-positive HOLD on obstacle detours while actively walking), LOW BIM7-2 (absolute 5-tile segment threshold), INFO BIM7-3→addressed. 17-plugin exclusivity cross-registration (BIM1-3) still a standing reminder.

## Verdict
**PASS WITH FINDINGS.** Ship acceptance stays pending live runtime lines: watch for `[BelowIceMountain] RUNNING_BUILD=8` (or equivalent hot-load marker) and whether the Build-7-era Willow-dialogue HOLD clears. Script was "paused for review" at 23:21–23:22 stream check — confirm the pause is lifted after the hot-load.

## Acks / log
- SEEN line appended to alex-inbox/seen.log (commit pending verification this run).
- Carried quest tally: MM likely complete ~22:30 EDT (FINISHED unobserved) → 10 quests / ~24 QP pending verification. BKF Build 1 still awaiting Julien's manual arming. Feed dark since 2026-09-30 17:44 EDT.
