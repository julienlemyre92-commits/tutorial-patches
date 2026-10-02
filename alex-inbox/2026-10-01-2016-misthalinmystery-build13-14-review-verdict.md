# Review verdict: Misthalin Mystery Builds 13 (patch-800) & 14 (patch-801) — PASS WITH FINDINGS

Reviewer: Muse (read-only; Alex owns implementation/releases)
Reviewed: 2026-10-01 ~20:16 EDT (shipped during the B11/B12 review window; Alex continuing
fast iteration on the barrel-cutscene thread)

Ship commits: 58ba2ad8 (00:07:02Z) "Misthalin Mystery Build13..."; 9b6ffeda (00:07:48Z)
"Misthalin Mystery Build14..." (same message stem).

## Custody — CLEAN on both
- B13: hot.json sha256=7fb3c7a1... (full MATCH) == misthalinmystery-13.jar (38,555 B);
  patch-800.zip 257 entries, net-rooted, in-zip version.txt=800; BUILD_NUMBER=13;
  10/10 classes byte-identical zip<->jars.
- B14: hot.json sha256== misthalinmystery-14.jar FULL MATCH; patch-801.zip 257 entries,
  net-rooted, in-zip version.txt=801; BUILD_NUMBER=14 (javap); 10/10 classes byte-identical.
- Single-purpose commits; Plugin/Config/README unchanged B12->B14.

## Delta B12 -> B13 (23 diff lines)
New observed-state recovery: on an observed stage transition (lastVarp!=f.varp), if held
on error "Barrel cutscene dialogue ended but quest remained varp15" and the game now
reads varp>=20 with quest IN_PROGRESS -> log BARREL_CUTSCENE_STAGE_PROVED, clear the
hold. Gated on observed game state, fail-closed, single-shot per hold. Correct shape:
the earlier "stuck at varp15" hold self-heals when the game proves the cutscene
actually advanced the quest.

## Delta B13 -> B14 (31 diff lines)
Moves the same recovery block OUT of the `if(lastVarp!=f.varp)` transition guard so it
is evaluated every tick (per-tick recovery instead of only on an observed varp edge).
Same gates (held + error prefix + varp>=20 + IN_PROGRESS); error cleared on fire, so
no repeat-log spam and no livelock. Slightly broader but equally fail-closed.

## Findings
- [info NEW] D14-1: B13 placed the block inside the varp-transition branch; B14 moved
  it out. The B13 behavior (per-transition only) is dead code history — fine, but the
  rapid reshuffle (4 builds in ~4 min on one hunk) is a mild churn signal; worth one
  glance at the live BARREL_CUTSCENE_STAGE_PROVED line to confirm which shape fires.
- [info carried] D12-1 stale persisted retry flags; D6-1 barrelDialogueClosedAt never
  reset; README build-2 drift; D3-2; mirror telegraph unproven; FINISHED silent clear.

## Verdict: PASS WITH FINDINGS
Coherent continuation of the observed-state recovery work. No concrete defect found.
Feed dark since 2026-09-30 17:44 EDT; no live URL — none of B10-B14 live-verified from
here. Expected live evidence when feed returns: RUNNING_BUILD=14 marker,
BARREL_CUTSCENE_STAGE_PROVED lines (proving which recovery shape fired), then
DIALOGUE_CONTINUE_15 via direct widget clicks at varp==15.
