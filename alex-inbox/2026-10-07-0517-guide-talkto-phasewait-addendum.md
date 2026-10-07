# GUIDE stale-continue stall — follow-up: Talk-to dispatched, now PHASE-WAITing (2026-10-07 05:17 EDT)

Supersedes/adds to `2026-10-07-0516-guide-stale-continue-stall.md`. Evidence: `screenshots/2026-10-07_05-15-41_GUIDE_diag.txt` (commit af1b4ab6, 250 lines, verified).

## What changed since 05:16

1. The STALE-231:5-SUPPRESSED loop ran 05:13:05 -> ~05:14:40 (broad scan kept promoting 'Click here to continue' 162:44 at (20,602); gate suppressed).
2. **05:14:52 Build 290 PRE-205-GATE cleared** (snapDialogOpen=false, standard=false, 231speaker=''): issued **ONE Talk-to** to Gielinor Guide. Build 197: "talkTo 'Gielinor Guide' stepping adjacent (dist 5) -- verifying position next tick". Build205 skipped by design.
3. **05:15:17 -> 05:15:41 Build 292 PHASE-WAIT**: "Talk-to pending, waiting for standard-dialogue proof -- Build205 SKIPPED (no re-dispatch)" — **24s+ with no dialogue proof**. The talkTo-verification line ("verifying position next tick") never produced a follow-up arrival/confirmed click in the visible lines.

## Mechanism reading (evidence-only)

- The suppression escaped, so the stall is no longer the promote/suppress deadlock. Now it's: Talk-to issued once, no standard-dialogue proof within ~25s, and Build205's re-dispatch is deliberately skipped — so if the single Talk-to click never landed (NPC at dist 4-5, step-to-adjacent may not have completed, click never verified), the bot holds in PHASE-WAIT indefinitely.
- Consistent with the phantom-continue hypothesis from the 05:16 note: after a relaunch watchdog exit, 162:44 was likely a stale completed-transaction widget; clicking it would have been harmless, but the code suppressed it instead and then spent ~95s + now ~50s+ idle.

## Two concrete questions for Alex

1. PHASE-WAIT has no timeout/no retry path when Build205 is skipped: if the single Talk-to click fails silently, is there any re-dispatch timer? ~25s at 05:15:41 with none seen.
2. A live stream frame of the (20,602) region would still settle whether 162:44 was phantom or real — companion PNGs are still 404 (diag-only coverage).

Loop is read-only; no changes made. Verified tally stays 10 quests / 31 QP (this GUIDE session is on an unknown fresh-account client, not the main account).
