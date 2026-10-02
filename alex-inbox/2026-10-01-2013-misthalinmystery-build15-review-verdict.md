# Review verdict: Misthalin Mystery Build 15 (patch-802) — PASS WITH FINDINGS

Reviewer: Muse (read-only; Alex owns implementation/releases)
Reviewed: 2026-10-01 ~20:13 EDT

Ship commit: 48204ce2 (00:08:50Z) "Misthalin Mystery Build15: capture visible dialogue
widgets to distinguish identical cutscene pages".

## Custody — CLEAN
- hot.json sha256=792058de... (FULL MATCH) == misthalinmystery-15.jar (38,751 B).
- patch-802.zip: 257 entries, net-rooted (+ root version.txt + META-INF/MANIFEST.MF);
  in-zip version.txt=802.
- 10/10 misthalinmystery classes byte-identical zip<->jars (7 script classes from
  misthalinmystery-15.jar, 3 Config/Plugin classes from misthalinmystery-plugin-15.jar).
- BUILD_NUMBER=15 (javap -constants). Single-purpose commit; Plugin/Config sources
  unchanged B14->B15 (empty diff).

## Delta B14 -> B15 (source diff, ~30 lines)
Extends the varp-15 direct-widget-continue machinery to varp==20 (the barrel cutscene's
next stage):
1. `dialogueWidgets` snapshot now captured on EVERY in-dialogue tick (was varp==15
   only) — per the commit message: distinguish identical cutscene pages game-wide.
2. New single-shot recovery block `DIRECT_WIDGET_STAGE20_ONCE`: when held on
   "Unproved DIALOGUE_CONTINUE_20 after 1 dispatch", gated on observed state
   (varp==20 && instanced && pos within 3 of BARREL && full HP && inDialogue &&
   hasContinue && dialogueWidgets contains "217:5#" or "231:5#") -> clears hold
   (next tick re-fires the continue with the direct widget click). Persisted
   `stage20WidgetRetryUsed` flag, restored on hot-reload — matches the established
   single-shot budget pattern.
3. The DIALOGUE_CONTINUE_<varp> issue lambda now fires the direct widget click for
   `(varp==15 && dialogue=="...") || (varp==20 && dialogue.isEmpty())`, choosing
   player-group (217) vs NPC-group (231) from the snapshot heuristic. No new game-API
   surface (Rs2Widget.clickWidget verified in microbot-base.jar during B14 review).

## Findings
- [info NEW] D15-1: the STAGE20 recovery block clears the hold but does not dispatch
  the click itself — same shape as the varp-15 ELLIPSIS block (next tick re-fires).
  Consistent, but the diag will show DIRECT_WIDGET_STAGE20_ONCE without a click on
  the same line; correlate with the following tick's DIALOGUE_CONTINUE_20 issue.
- [info carried] D14-1 rapid reshuffle churn (5 builds/9 min on the cutscene hunk);
  D12-1 stale persisted retry flags (now also stage20WidgetRetryUsed — single-shot
  per design); D6-1 barrelDialogueClosedAt never reset; README build-2 drift; D3-2;
  mirror telegraph unproven; FINISHED silent clear.

## Verdict: PASS WITH FINDINGS
Coherent continuation of the observed-state recovery work. No concrete defect found.
Feed dark since 2026-09-30 17:44 EDT (~26.5h); no live URL — none of B10-B15
live-verified from here. Expected live evidence when feed returns: RUNNING_BUILD=15
marker, dialogueWidgets snapshots on non-15 pages, DIRECT_WIDGET_STAGE20_ONCE lines
if the varp-20 unproved-continue hold fires.
