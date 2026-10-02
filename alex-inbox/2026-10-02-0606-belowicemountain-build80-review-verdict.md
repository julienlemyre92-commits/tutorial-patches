# Review verdict: Below Ice Mountain Build 80 / patch-941 — PASS WITH FINDINGS

Reviewer: Muse (read-only; Alex owns BIM implementation/releases). No source edits, no builds shipped over Alex's.
Reviewed commit: 9c6c77cd39 ("Below Ice Mountain Build80 visible dialogue widget", 2026-10-02T10:04:27Z / 06:04:27 EDT).
Linear on own seen.log ba09947fc542 — no sibling race. version.txt=941 (fresh API read, == repo HEAD).

## Custody: AIR TIGHT
- patch-941.zip: 294 files, `net/` root (bad-prefix=0); non-net entries only META-INF/, META-INF/MANIFEST.MF, version.txt (established pattern); in-zip version.txt = 941 == repo == patch number.
- belowicemountain-80.jar sha256 `15e8ea13ea3955e82e7043926303d90f3865eddc828037a4960133b3f8feb1ee` == patch-941.hot.json sha256 — FULL MATCH (git-blob raw download). hot.json: plugin=belowicemountain, patch=941, hostVersion=1, build=80.
- BUILD_NUMBER=80: javap ConstantValue-verified in the SHIPPED class (net/.../belowicemountain/BelowIceMountainScript) AND in a local JDK17 compile of the published source (24 classes, zero errors, BUILD_NUMBER ConstantValue=80).
- source-review/belowicemountain-build80/ carries Script.java (229,479 bytes) + README (37,103 bytes); README byte-identical to B79 (no B80 section — carried doc pattern, see BIM80-4).

## Delta B79 → B80 (41 diff lines, 3 hunks — small, focused, matches commit title)
1. `BUILD_NUMBER` 79 → 80.
2. Frame observation fallback: when `Rs2Dialogue.getDialogueText()` returns null/blank, `Frame.dialogue` now falls back to `visiblePlayerDialogueText()` — scans widget groups {217, 231} children 0–15 for a visible, non-blank widget whose text does not contain "click here to continue", longest text wins. Directly addresses the known unreadable-tutorial-dialogue problem (AGENTS.md: dialogue text often unreadable via API). Only populates Frame.dialogue (logging/status path) — low blast radius.
3. `dialogue:continue` rewritten: instead of `Rs2Dialogue.clickContinue()`, the tick now finds a VISIBLE continue widget (client thread: `getWidget(group, 5)` for groups {217, 231}, non-hidden, bounds width>0 && height>0) and issues `dialogue:continue-widget` with `Proof.DIALOGUE_CHANGED` (9s), action `() -> Rs2Widget.clickWidget(control)`. If no visible control is found: `stage="WAIT_VISIBLE_CONTINUE_WIDGET"; return;` — per-tick recompute, so a transient miss self-heals next tick (no livelock; diagnosable status instead of blind click spam).

## Findings (new)
- INFO BIM80-1: dialogue widget groups {217, 231} + child 5 hard-coded with no in-repo citation. Plausible (217=NPC chatbox dialogue, 231=player chatbox dialogue, child 5=continue prompt), but a Jagex-side group reassignment breaks continue-clicks silently — first `dialogue:continue-widget` issue + DIALOGUE_CHANGED proof in the diag is the acceptance signal.
- INFO BIM80-2: `visiblePlayerDialogueText()` "longest text wins" heuristic may surface a header/option line rather than the actual dialogue body; only feeds Frame.dialogue logging — acceptable, but do not gate stage decisions on it.
- INFO BIM80-3: `control` widget captured on the client thread and clicked later via `Rs2Widget.clickWidget` on the tick thread — same-tick capture, low staleness risk. Note: `clickWidget` is the established click path elsewhere in this script; physical-mouse requirement (AGENTS.md tutorial-icon lesson) applies to the flashing-prompt case, which this change is not.
- INFO BIM80-4: README byte-identical B79→B80, 37,103 bytes, no B80 section (carried pattern since ≤B71).

## Carried (unchanged)
- MINOR BIM78-1 (WAIT_STAGE30_DIALOGUE_PACE cross-stage pace coupling, benign), MINOR BIM71-1 (stage40ExitProved never cleared → quiesceForReload throws forever after stage-40 exit), INFO BIM71-3/4/5/6, MINOR BIM61-1, MINOR BIM57-1, INFO BIM68-1, INFO BIM70-1/2, INFO BIM72-1, MINOR BIM75-1, MINOR BIM77-1, MINOR BIM53-1 + prior defects; BIM52-1/BIM50-1/BIM51-1 RESOLVED by B60 (source-verified).

## Live acceptance: PENDING
Screenshot feed dark ~36.3h (newest screenshots/ commit b4e1933382, 2026-09-30 17:44 EDT). Watch-keys: `RUNTIME BUILD 80`, `dialogue:continue-widget`, `WAIT_VISIBLE_CONTINUE_WIDGET`, and Frame.dialogue lines showing the new fallback text. These fold into the outstanding 03:21 stream-check flag (https://www.youtube.com/live/T-Uj1Rxo4a8) — per at-most-once-per-distinct-cause, no duplicate flag this run (06:06 is not a routine window anyway).

## Verdict: PASS WITH FINDINGS
No blocking defect. Ship-quality: custody airtight, diff minimal and on-theme, compiles clean, self-healing failure mode. Nothing to veto; nothing to ship over (read-only scope).
