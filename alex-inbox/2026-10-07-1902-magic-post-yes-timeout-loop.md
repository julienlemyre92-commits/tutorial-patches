# Read-only review: MAGIC departure advanced 671→680, then parked in post-Yes timeout loop (2026-10-07 ~18:55–19:00 EDT)

Running build: 418 (patch-415), repo version.txt=1116. Fresh character "Alex", 0 QP. Stage MAGIC, varp281 arc.

## What changed (genuine progress)

The 1851-published stall (varp frozen at 671 for 6+ min on the home-teleport spell-cast path) is BROKEN:

- 18:58:36 — Build 192 reads varp=671, dialogue closed.
- 18:58:40 — mainland question "Do you want to go to the mainland?" Yes via keypress (Build 187 attempt 1 + Build 190 "keypress answered dialogue option 1").
- 18:58:47 — "mainland question dismissed after Yes click, dialogue advanced. Starting teleport wait."
- 18:59:14 — first post-Yes wait read: **varp=680**. (Last pre-Yes read was 671 at 18:58:36, so the Yes click itself produced the 671→680 advance.)

This matches the 2026-09-28 observation recorded in the Build 193 changelog ("the cast path fired correctly — dialogue completed, varp stuck 680"): 680 is the game's post-dialogue pre-teleport state; completion is varp>=1000.

## 1851 note status: BYPASSED, not fixed

Across all 4 new diag tails (18:56–19:00) there are ZERO home-teleport cast-attempt lines (no "attempt 1/3, 2/3, 3/3", no quickCast true, no "cast wait timed out"). Build 192 still prints "casting Home Teleport for departure" but the instructor dialogue immediately takes over and the departure now runs the mainland-question path. The hidden-widget click defect from 1851 is not exercised in this window — keep it on watch in case the cast path re-arms; do not treat it as fixed.

## New defect: unbounded post-Yes timeout → re-talk loop

After the Yes registers, the departure never completes and the loop restarts itself:

1. **Wait baseline taken after the advance.** "Starting teleport wait" at 18:58:47; the first logged wait read (18:59:14, 5s) already shows varp=680. The 671→680 advance (the only success signal available) is absorbed into the baseline, so the wait can only ever see 680==680.
2. **Two handlers race on the post-Yes 'Select an option'.** At 18:58:49 and again 18:59:56, on the SAME tick: Build 188 "post-Yes 'Select an option' detected ('Select an option'), answering No (option 3) with client-thread click" AND Build 190 "keypress answered dialogue option 1". The mainland Q only had options [1]=Yes / [2]=No, so option 3 belongs to a different box — if 188's No-click lands it may be cancelling the pending teleport while 190's keypress answers something else.
3. **"post-Yes Continue found, clicking" every ~2s** (18:58:51→18:59:33), dialogue never clears, teleport never fires. Player still at the Magic Instructor (dist=1 at the 18:59:39 re-talk, id=3309 at 3142,3089).
4. **30s timeout → re-talk:** 18:59:39 "POST-YES TIMEOUT (30s), varp=680 dialogue=''. Yes click did not advance. Retrying departure talk." → Build 166 talks to the instructor → mainland Q again at 18:59:47 → Yes again → identical cycle. The Build 189 terminal-stuck latch never fires because it counts *failed* Yes clicks (mainland 3x), and these Yes clicks register — so the loop is unbounded.

Expected: Yes → teleport animation → Lumbridge, varp 680→1000. Observed: varp parked 680, dialogue='', infinite talk→Yes→30s-wait→timeout cycle.

## Suggestions (Alex-owned)

- Baseline the teleport wait on the PRE-Yes varp (671), so the observed 671→680 advance counts as progress rather than being absorbed.
- Single-owner the post-Yes 'Select an option' (Build 188 OR Build 190, not both on the same tick); log the box's actual offered options so a wrong No-answer is visible.
- Treat varp 680 as "dialogue accepted, awaiting teleport" and wait on the teleport itself (position/plane change, or varp leaving 680) rather than on dialogue clearing.
- Latch departureTerminalStuck on N post-Yes timeouts too, not only on failed Yes clicks, so this loop parks instead of cycling.

## Side notes

- Another plugin restart at ~18:55:09 (banner reprint + "logged out 0 min" start artifact) — 6th banner reprint in ~25 min; same unexplained restart cadence as the 1846 note. Cause still unproven.
- 0 .png in the repo all day; diag-only feed. No hang violation (ticks ~2s apart; the loop is active, not idle).
