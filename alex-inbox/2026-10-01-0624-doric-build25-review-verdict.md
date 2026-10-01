# Doric Build 25 / patch-664 review verdict — PASS
Review-loop run 2026-10-01 06:24 EDT (sched 06:23:52). Read-only scope; no source edits, no ship.

## Build under review
- Commit c818d544→? "Doric Build25: accept the verified materials prompt and resume" (2026-10-01T10:22:28Z); version.txt 663→664 (confirmed via API at run start).
- patch-664.zip: 215 entries, classes under net/ root + META-INF/MANIFEST.MF + version.txt (in+out version.txt = 664). 200 classes total; 195/200 byte-identical to patch-663 → no stale-class reship, no version reuse.

## Method
Raw binary downloads via git blobs API (contents API on patch-664.hot.json returned repeated transient 404s; blob-by-SHA worked). Per-class sha256 cross-zip, javap -p -constants -c diffs on the 5 changed classes, jar/zip/hot.json chain check.

## Delta (patch-663 → patch-664) — purely additive
1. BUILD_NUMBER 24→25 in DoricsQuestScript and DoricsQuestPlugin.
2. New branch in option(Frame): `frame.varp == 10 && Rs2Dialogue.hasDialogueOption("Certainly, I'll be right back!")` → option text set to "Certainly, I'll be right back!", routed through the existing guarded clickOption + set(DIALOGUE_OPTION, 5500ms) flow. Failed click still falls through to hold() with the option text (existing fallback).
3. Hold literal shortened: "Unknown Doric dialogue options; review live widget text" → "Unknown Doric dialogue options".
4. Inner classes $Frame/$LoginFrame/$Pending: class bytes changed but javap -p -constants -c output identical → constant-pool/debug-metadata churn only, no semantic change.
5. The Build-23 "Yes." branch (frame.varp==0 && hasDialogueOption("Yes.")) is intact; both branches coexist.

## Safety assessment
- The new branch is double-gated on quest-stage state (varp==10, the materials stage) AND a live widget check (hasDialogueOption) before any click — same safe pattern as Build 24's recovery branch. No blind click; bounded by the 5500ms DIALOGUE_OPTION pending budget; failed click → explained HOLD. No infinite-loop path added.
- "Certainly, I'll be right back!" is the canonical player line answering Doric's materials request, consistent with the commit message.

## Verdict: PASS
Ship is structurally clean: fresh version number, correct zip layout, additive guarded delta, SHA link between doricsquest-plugin-25.jar and the zip's script class (5d2b0a3c090c80332fc5980dfc11c38e54a7a3243e8aeaffb4ab4b7bc12daa61 matches both).

## Open question for Alex (not a failure)
patch-664.hot.json carries sha256 b3897358f6be399264494fd7d70a61c92be168031108dd9ab143ff64658a6e6b, which matches NEITHER the jar (abe9cef954776bd099e619355e3b13436146d24cb3d0e20fab0d10071ec9eacc) NOR the script class NOR any concatenation of the 5 Doric classes I tested (sorted, zip-order, inner-only, script+plugin). The 06:21 run reported Build 24's hot.json sha == its jar; patch-663.hot.json is absent from the repo now, so I can't compare conventions. Please confirm what bytes patch-664.hot.json's sha256 covers — the hot-reload host's acceptance check depends on it, and if it compares the downloaded artifact against this value it would currently reject.

## Live verification status
Feed still dark: newest screenshot commit remains 2026-09-30T21:44:06Z (17:44:02 EDT PIRATESTREASURE_DONE); zero ERNEST_*/IMPCATCHER_*/DORIC_* frames ever (~12h45m dark). Acceptance of Build 25's new branch rests on Alex's direct in-chat runtime reports (expected new diag: option click on "Certainly, I'll be right back!" then DIALOGUE_OPTION set, materials stage resumed).
