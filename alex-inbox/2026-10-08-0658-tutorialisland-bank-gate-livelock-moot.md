# Tutorial Island BANK: 'Large door' gate-click loop — RESOLVED BY RESTART (0655 note MOOT)

- FINDING: the gate-click loop in 2026-10-08-0655-tutorialisland-bank-gate-livelock.md (Build 378/379: 'Open' click on the Large door at WorldPoint(3121,3119) returns true since 06:51:52, player tile static, Build 379 re-reports "no reachable tile adjacent to guide" every ~9s) is CLEARED. The client restarted ~06:54:07 EDT (fresh startup banner: Build 104 cached object names, Build 392 MISSION_SELECT tutorial claim, VARP 281 = 530 -> detected start stage BANK). Same runtime build (418/patch-415); no new build shipped, no code change.
- EVIDENCE (all varp281-verified, game state not banner/panel):
  - 06:54:47 BANK: Build 108 Talk-to 'Account Guide' (id 3310, dist 1) + dialogueTick continue clicks (Account-Guide-first order per Build 110 holding)
  - 06:55:50 PRAYER: physical "MOUSE CLICKED PRAYER tab icon" (name-matched); varp 560 -> 570
  - 06:55:54 Talk-to 'Brother Brace' (id 3319, dist 2); 06:56:01 varp 570 -> 610 (talk2/talk3 + Friends tab done)
  - 06:56:06 PRAYER_EXIT: 'Open' clicked on door 9723; crossing VERIFIED by varp 610 -> 620; "Stage: PRAYER -> MAGIC" 06:56:07
  - 06:56:56 Talk-to 'Magic Instructor' (id 3309, dist 1); 06:56:59 varp 640 -> 650 (Instructor talk2 done)
  - 06:57:07 Build 179 found Wind Strike (id 14286859, bounds 40x40 at 919,477), cast invoked, WIDGET_TARGET_ON_NPC on chicken (idx 2078); "Wind strike cast issued (varp=650)" 06:57:10; MAGIC TICK cast=true
  - 06:57:53 mainland question -> Yes (option 1, keypress, attempt 2 after attempt-1 did not register); 06:57:59 Ironman Q -> Build 183 auto-NO (option 3, keypress), varp=680
  - 06:58:13 tail end: MAGIC TICK talked1=true tab=true talked2=true cast=true talked3=false; dialogue continue clicks ongoing
- MECHANISM: no fix shipped — the restart re-derived BANK from varp281=530 and the script re-ran the bank arc cleanly. The Build-378/379 defect (no post-click gate-state verification, no crossing check before re-arm) is therefore NOT proven fixed; the loop was cleared by restart, not by a code change. If the bot revisits that gate, the loop may recur.
- SECONDARY (benign, noted): "Build 108: talking to the Account Guide (round 3/2)" — retry counter exceeds its max; resolved, no action needed. BANK TICK still logs opened=true AND closed=true (contradictory; also flagged in the 0655 note). Build 300 creatorBinding check every tick (noise only).
- VERIFY BY: fresh tail showing MAGIC TICK talked3=true and the tutorial-exit / mainland transition (varp281 past 680). If this client completes Tutorial Island, the ownership question stands: the main account finished TI weeks ago, so this is an unknown account/session.
- NOTE: main-account front (Shield of Arrav staging post-Dragon Slayer I) is unaffected; this remains the mystery Tutorial Island client, diag-only (no PNGs landing on repo).
