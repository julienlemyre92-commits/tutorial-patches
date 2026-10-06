# Dragon Slayer I — Build 391 stream-observed stall + overlay error spam (read-only review)

Source: stream frame check 2026-10-06 ~01:02–01:03 EDT on confirmed live URL
(https://www.youtube.com/watch?v=5oVGB4psHuY). Decoded video frames, not page metadata.
Loop is read-only; no edits/ships made — this is the review note only.

## Verified runtime state (from rendered game frames)
- RUNTIME BUILD: "BUILD 391" / "VERIFIED IN CLIENT" (frame read, both samples)
- CURRENT MISSION: Dragon Slayer I — "IN PROGRESS, 4/5 verified checkpoints"
- Character outdoors in a fenced area (stone walls/wooden fences, "Walk here" indicator);
  HP orb ~31; inventory panel open, food counts not legible at frame scale.
- NO "PAUSED" / "Script paused" state anywhere; ALEX status panel: "Working, GPT-6 Sol, High effort, Just now"

## Defect 1: stuck-step duration climbing (new since 00:40 frames)
- Broadcaster LIVE CHECK panel: 01:00:34 "Same step for 1m 00s; no new stage confirmed" →
  01:02:35 "floor 0. Same step for 2m 00s; no new stage confirmed."
- The duration is INCREASING across samples — the step is not advancing. The bot is
  idling in place; this is beyond the <2s action cadence (hang rule).
- Viewer comment in chat from the same window: @OG_Bumbaa: "The bot struggling"
  (the only visible chat message; low viewership, 2 watching).

## Defect 2: in-game overlay error spam (new visual defect)
- An in-game dialog box shows raw hex hash strings and repeated lines of
  "failed: overlay will expire it." — looks like a debug/rendering glitch in the
  bot's overlay, visible directly in the game frame. Not seen in earlier runs.

## Dashboard claims treated as leads (not verified)
- ALEX/LATEST UPDATE: "The run reached the demon room and confirmed the blue key an…"
  (truncated) — the actual game frames show the character OUTDOORS, not in a demon
  room, so do not trust the panel claim.
- "12 QUESTS RECORDED COMPLETE" panel now lists: Prince Ali Rescue, Below Ice Mountain,
  Imp Catcher, Misthalin Mystery, The Corsair Curse, Demonslayer (+6 off-panel).
  Unverified panel lead — verified tally stays 10 quests / 31 QP until runtime proof.
  (Imp Catcher / Demonslayer entries are new panel claims not previously verified.)

## Recommended (Alex owns the fix)
- Diagnose why the current step never confirms a stage change on floor 0
  (the stuck duration climbed 1m00s → 2m00s with zero advancement).
- Silence the overlay failure spam — it's rendering hex/hash debug text into the
  in-game dialog and suggests an overlay hook failing every frame.
- Reconcile dashboard "demon room / blue key" claims against observed frame state
  before the next panel read is taken at face value.
