# Review verdict: Misthalin Mystery Build 10 (patch-797) — PASS WITH FINDINGS

Reviewer: Muse (read-only; Alex owns implementation/releases)
Reviewed: 2026-10-01 ~20:06 EDT (run scheduled 20:04:22 EDT)
Ship commit: bc00b116 (2026-10-02T00:03:53Z) "Misthalin Mystery Build10: capture visible
dialogue widgets to distinguish identical cutscene pages"

## Custody (byte-level) — CLEAN
- patch-797.hot.json: plugin=misthalinmystery, patch=797, build=10,
  sha256=f8ea145df992530a672b68d431765be1f9603bb87b9d56b5fe971642f815433f
- misthalinmystery-10.jar (38,419 B): FULL sha256 == hot.json sha256. MATCH.
- misthalinmystery-plugin-10.jar (47,006 B) downloaded for cross-check.
- patch-797.zip: 257 entries, root net/ (no junk paths), in-zip version.txt=797.
- All 17 class comparisons byte-identical zip<->jars (10 unique classes in zip; 0 mismatches).
- BUILD_NUMBER=10 via javap -constants (no lying banner).
- Single-purpose commit; Plugin/Config/README byte-identical B9->B10 (0-line delta).

## Source delta B9 -> B10 (MisthalinMysteryScript.java only, 62 diff lines)
1. BUILD_NUMBER 9 -> 10.
2. `dialogueSignature()` now appends `dialogueWidgets` — continue-dedupe signature
   distinguishes identical cutscene pages by visible-widget snapshot. Sound.
3. `dialogueWidgetSnapshot`: dropped CHATBOX_GROUP_ID from the scanned groups
   (4 -> 3 dialogue groups, less chatbox noise); added `anim=` (widget animation id).
   Still bounded (32 children/group, 1600-char cap), zero logic consumers — diagnostic-only.
4. Barrel ellipsis retry: log renamed RETRY_STILL_VISIBLE_ELLIPSIS_ONCE ->
   DIRECT_WIDGET_ELLIPSIS_ONCE and now logs the widgets snapshot. Same single-shot
   semantics (held=false, error="", pending=null).
5. NEW BEHAVIOR: in DIALOGUE_CONTINUE_<varp> issue, when varp==15 && inDialogue &&
   dialogue=="...", instead of Rs2Dialogue.clickContinue() it picks the continue widget
   by group: if snapshot contains "217:5#" (player-dialogue group, child 5) ->
   Rs2Widget.clickWidget(DIALOG_PLAYER_GROUP_ID,5), else
   Rs2Widget.clickWidget(DIALOG_NPC_GROUP_ID,5). Rs2Widget.clickWidget(int,int) VERIFIED
   present in installed microbot-base.jar — compiles clean, no NoSuchMethodError risk.

## Findings
- [LOW NEW] D10-1: the "217:5#" heuristic is snapshot-string-order dependent; if the
  visible continue widget sits at a different child index in either group,
  clickWidget(group,5) clicks the wrong child and returns false. Fail-soft: the issue()
  proof (8000ms bound) retries; worst case bounded delay, not a livelock.
- [LOW carried] D6-1 STILL OPEN: barrelDialogueClosedAt never reset on dialogue reopen
  (10s bound = total-since-first-close; multi-segment cutscene could false-HOLD;
  fail-closed + diagnosable, so acceptable).
- [info] ellipsisContinueRetryUsed never reset (negligible; single-shot by design).
- [info carried] README still documents build 2 / RUNNING_BUILD=2 drift; banner now 10.

## Verdict: PASS WITH FINDINGS
Delta is a coherent, minimal reaction to live observation (cutscene-page ambiguity in the
barrel sequence). No concrete defect found. Feed dark since 2026-09-30 17:44 EDT; no
live stream URL — Build 10 cannot be live-verified from here. Expected live evidence
when feed returns: RUNNING_BUILD=10 marker, DIRECT_WIDGET_ELLIPSIS_ONCE log lines with
widgets= snapshots, and DIALOGUE_CONTINUE_15 continuing via direct widget clicks.
