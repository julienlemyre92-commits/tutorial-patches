# Review verdict: Misthalin Mystery Builds 52–56 (patches 839–843) — 2026-10-01 21:30 EDT
Reviewer: Muse (read-only). Verdict: **PASS WITH FINDINGS** — no HIGH defects.

## Custody (all five builds)
- hot.json sha256 == patches/misthalinmystery-{52..56}.jar FULL MATCH (verified via git-blobs raw download).
- patch-839..843.zip: 258 entries, net/ root (no jar-manifest overwrite risk), in-zip version.txt == patch number.
- 8/8 script classes byte-identical zip <-> jar (0 differ, per build).
- BUILD_NUMBER=52/53/54/55/56 via javap -constants on the shipped jars.
- Source-review deltas (b51->b52->...->b56) are consistent with jar size changes; Config not re-published for this range (unchanged since B51 — acceptable, no config touched).
- Commits: cb5e2b34 (B52), 5e6f871b (B53), d54afaf6 (B54), 0398ba30 (B55), cca5e940 (B56); version.txt=843.
- Note: commit messages for all five say "capture visible dialogue widgets to distinguish identical cutscene pages" but the actual deltas are mirror-boss-fight gates — boilerplate drift (same class as the B48-51 "boilerplate commit messages misdescribe deltas" info note).

## Deltas
- **B52**: new hold-clear gate "Movable mirror NPC absent" → clears when varp∈{110,111} && boss && full HP && mirror NPC present (logs MIRROR_NPC_LOADED tile).
- **B53**: new hold-clear gate "No unique stable wardrobe telegraph after 30s" → clears when varp∈{110,111} && boss && full HP && mirror NPC present && mirrorCueWardrobe!=null && ≥650ms since mirrorCueAt && (open-wardrobe object list contains cue || live graphics has a point on cue tile) → sets mirrorCueSeen=true, refreshes mirrorCueLastSeenAt (logs MIRROR_TELEGRAPH_TILE_STABLE). Also: mirror-signal snapshot no longer re-arms mirrorCueAt when only graphicId changes (stability clock survives graphic-id flips).
- **B54**: new hold-clear gate "Route MIRROR_PUSH_SIDE consumed ten bounded walker segments" with EXACT-tile conditions (NPC at (1622,4828), cue (1627,4831), player (1623,4830)) → MIRROR_SOUTH_APPROACH_RESUME; new two-branch south/west approach via walkFastCanvas when stand==(1622,4827).
- **B55**: widens the B54 gate to non-null presence (no exact tiles) → MIRROR_CANVAS_APPROACH_RESUME; replaces the two-branch approach with a single MIRROR_CANVAS_TO_SIDE issue via walkFastCanvas(stand). Directly answers MM54-1.
- **B56**: the 30s "No unique stable wardrobe telegraph" hold now anchors on max(stageAt, mirrorCueLastSeenAt) instead of stageAt alone — the clock restarts from the last-seen telegraph.

## New findings
- [LOW] MM52-1: the new "Movable mirror NPC absent" clear gate has no single-shot latch (D17-1 class) — NPC stream in/out churn can hold/clear repeatedly.
- [LOW] MM53-1: telegraph "stability" is inferred from a single tick's object/graphic read; with B53's removal of the graphicId-change re-arm, a flip-flopping graphicId no longer resets the clock — stability is asserted, not observed across ticks.
- [LOW] MM54-1: B54's exact-tile resume gate + south/west approach branches (NPC/cue/player exact tiles) were brittle — **CLOSED by B55** (widened to presence, single canvas walk).
- [LOW] MM56-1: the 30s timer now re-anchors on every re-seen telegraph — a flickering telegraph (re-seen every <30s) can defer the hold indefinitely; no absolute stage cap. Consider an absolute timeout.
- [INFO] Commit messages for B52–B56 all misdescribe the deltas (boilerplate) — cosmetic, but it makes the repo history misleading for future reviewers.

## Carried (unchanged)
D28-1 (dead "aat:" gate — STILL OPEN, line 750 in b56 source, no producer), B36-1, B36-2, B43-1, B43-2, B43-3, B45-1, B47-1, B47-2, D28-2, D30-1, D27-1, D27-2, D16-1, D16-2, D14-1, D12-1, D6-1, README drift, D3-2, mirror telegraph (partially addressed B53/B56), FINISHED-silent-clear.

## Live acceptance
Pending. Last eyes-on: stream 21:24–21:28 EDT showed RUNTIME BUILD 53 /confirmed, "Script paused", Alex debugging the mirror-push pathing live (B54/B55's exact-tile → canvas approach iterations are the in-production answer). Screenshot feed still dark since 2026-09-30 17:44 EDT (~27.7h).
