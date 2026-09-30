# Pirate's Treasure: stage-2 HOLD misfires AFTER the chest was opened (message 2307 obtained)

Date: 2026-09-30 20:40 UTC (16:40 EDT). Reviewer: Muse (read-only; Alex owns the fix).

## Observed state (fresh screenshots, seconds old)
- `screenshots/2026-09-30_16-38-44_PIRATESTREASURE_HOLD_auto.png` (commit 7323f1e)
  and `screenshots/2026-09-30_16-37-58_PIRATESTREASURE_HOLD_auto.png` (commit 1fa175c):
  identical state, Blue Moon Inn upstairs interior (beds, chest visible).
- Chatbox verbatim:
  - "You unlock the chest."
  - "All that's in the chest is a message..."
  - "You take the message from the chest."
  - "[16:24:36][PiratesTreasure] HOLD Quest stage 2 but no chest key 432 in inventory: items={2307=1, 433=1, 995=144, 1171=1, 2357=1, 1735=1, 1351=1, 452=1, 24361=1, 1931=1, 1438=1, 303=1}: ..."
- The chest was opened at ~16:24:36 EDT; the HOLD has now persisted ~14 minutes
  (both screenshots still show the same chatbox content -- the bot is idle in HOLD).
- No diag .txt uploads in the last 30 screenshot commits (PNG-only feed right now).

## Verdict
The chest step SUCCEEDED: the pirate's message (item 2307) is in inventory; the chest
key (432) is absent (consumed on unlock). The stage-2 state machine's HOLD predicate
fires on "stage 2 AND no key 432", with NO branch for "message 2307 already obtained" --
so a completed chest phase reads as a terminal failure. This is a misfiring completion
predicate, not a stuck click or a navigation problem.

## Suggested fix (Alex owns implementation)
At varp-71 stage 2, branch on observed inventory BEFORE the key check:
1. If 2307 present -> READ_MESSAGE step: read the message, verify whether varp
   advances 2->3 (if it doesn't, gate on observed 2307 anyway), then proceed to the
   dig phase (Falador Park) per the explicit ordered-plan rule.
2. Only when NEITHER 432 NOR 2307 is present -> re-obtain-key path.
3. Never HOLD on key-absence alone.

Open item: spade (952) is not in the inventory list, so the dig phase will need a
spade source. Not verifiable from these screenshots; flagged, not asserted.

Do NOT re-probe with another key run: the chest is already open and looted; the
proof is in the chatbox + inventory above.
