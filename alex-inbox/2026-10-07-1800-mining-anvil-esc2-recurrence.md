# MINING smith-arc anvil-click/mine-esc2 ping-pong RECURRING on fresh-character run (read-only review)

Addendum to `2026-10-07-0646-tutorialisland-smith-arc-anvil-esc2-loop.md` (ack'd this morning).

- RECURRENCE: same loop signature on the fresh-character TI run (Build 418, new account 0 QP/343).
  Evidence (`screenshots/2026-10-07_17-56-46_MINING_diag.txt`):
  - 17:56:24 `Build 196: walking to anvil at WorldPoint(x=3083, y=9499, plane=0)` (once, verified next tick)
  - then cycling: `tick-wait 'mine-esc2' armed, bound 3 ticks` -> `EXHAUSTED bound` -> `Skipping mining -- already have bar/dagger` / `Skipping smelt -- already have bar/dagger` -> `tick-wait 'mine-anvil-click' armed, bound 5 ticks` -> `mine-esc2` armed again. Repeats 17:56:24-17:56:46 (22s+, ongoing).
  - Zero smithing-interface lines, zero dagger-progress lines, no stage advance. The anvil click never verifiably opens the smith menu.
- NEW: the loop SURVIVES a client restart. The client restarted 17:54:44 (banner re-dump, `Build 194: logged out 0 min` watchdog line, then re-login); the script re-derived stage from observed state (`VARP: 281 = 320` -> `VARP-281 stage detection -> MINING`, `Stage: null -> MINING`) and walked back to the anvil — the ping-pong is in the script logic, not transient client state.
- PRE-RESTART: instructor dialogue appeared to complete (pre-restart tail showed Talk-to Mining Instructor issued 17:53:44 + continue clicks), so the bot plausibly holds bar+hammer; the `already have bar/dagger` skip claims are unproven either way.
- Suggestions unchanged from the 06:46 note: ESC ownership gate (the `mine-esc2` press should not fire unless the script itself opened something) + anvil-click verification diag (log what the click actually produced: smithing widget open? nothing?).
- VERIFY BY: fresh diag tail showing a smithing-interface-open line (or dagger crafted / varp advance past the smith step) within ~10 ticks of the first `mine-anvil-click`.
