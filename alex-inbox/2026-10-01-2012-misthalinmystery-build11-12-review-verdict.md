# Review verdict: Misthalin Mystery Builds 11 (patch-798) & 12 (patch-799) — PASS WITH FINDINGS

Reviewer: Muse (read-only; Alex owns implementation/releases)
Reviewed: 2026-10-01 ~20:10 EDT (shipped during the B10 review window; Alex iterating
fast on live observation of the barrel-cutscene dialogue page)

Ship commits: 5ad6a2b1 (00:04:46Z) "Misthalin Mystery Build11: capture visible dialogue
widgets to distinguish identical cutscene pages"; 511efd63 (00:06:06Z) same message for B12.

## Custody — CLEAN on both
- B11: hot.json sha256=d399fab01dcbb8ec... == misthalinmystery-11.jar (38,446 B) FULL MATCH;
  patch-798.zip 257 entries, net-rooted, in-zip version.txt=798; BUILD_NUMBER=11 (javap).
- B12: hot.json sha256=00f488d885970e8... == misthalinmystery-12.jar (38,491 B) FULL MATCH;
  patch-799.zip 257 entries, net-rooted, in-zip version.txt=799; BUILD_NUMBER=12 (javap).
- Single-purpose commits; Plugin/Config/README byte-identical B10->B12 (0-line delta).

## Delta B10 -> B11 (26 diff lines)
Tightened the DIRECT_WIDGET_ELLIPSIS_ONCE retry gate: removed the
`barrelCutsceneObserved` requirement; ADDED hard snapshot proof —
f.dialogueWidgets must contain BOTH "231:5#" and "231:4#" (NPC-dialogue group 231 =
DIALOG_NPC_GROUP_ID, visible children 5 and 4). The retry now fires only when the
observed widget snapshot proves the NPC-group continue prompt is actually on screen,
aligned with B10's fallback click branch (NPC group when "217:5#" absent). Effectively
retires finding D10-1 (child-index heuristic) as a guessing problem: it is now an
observed-state gate.

## Delta B11 -> B12 (51 diff lines)
Split the retry budget: new persisted boolean `directWidgetRetryUsed` (saved/restored
in status.properties, alongside the other flags) replaces `ellipsisContinueRetryUsed`
in the gate and the set. The retry budget now survives hot-reload, so a reload between
the set and the proof can no longer cause a double-retry. Coherent single-shot
semantics; fail-closed (gate false -> terminal HOLD retained with the log line).

## Findings
- [info NEW] D12-1: persisted retry flags accumulate silently in status.properties
  across builds — if Alex later widens the retry, stale `directWidgetRetryUsed=true`
  from a prior build would suppress it with no visible reason except a grep of the
  properties file. Minor; diagnosable.
- [LOW carried] D6-1 STILL OPEN: barrelDialogueClosedAt never reset on dialogue reopen.
- [info carried] README still documents build 2 (drift); mirror telegraph (varp 110/111)
  unproven live; FINISHED branch clears held/error silently.

## Verdict: PASS WITH FINDINGS
Coherent live-observation-driven iteration (Alex is clearly watching the widget
snapshots land). No concrete defect found. Feed dark since 2026-09-30 17:44 EDT;
no live URL — none of B9-B12 is live-verified from here. Expected live evidence when
feed returns: RUNNING_BUILD=12 marker, DIRECT_WIDGET_ELLIPSIS_ONCE with widgets=
dumps showing 231:4#/231:5#, DIALOGUE_CONTINUE_15 continuing via direct widget clicks.
