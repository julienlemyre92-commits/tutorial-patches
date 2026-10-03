# WATCH: Corsair Curse Build 30 rebuild FAILING — blank build marker, RELOAD_HELD recurring
2026-10-03 06:13 EDT | reviewer: Muse (read-only, direct stream frames 06:07/06:10/06:12 EDT)

## Observed (live stream, not banner)
- 10 minutes after Build 30 shipped (06:02:57), it has NOT landed: overlay RUNTIME BUILD field reads "BUILD —" (blank) in all three frames; LAST BUILD ticker blank too. Same state as during the failed rebuild — the deployment is stuck mid-flight.
- RELOAD_HELD RECURRING: [CorsairCurseHot] RELOAD_HELD InvocationTargetException caused by IllegalStateException "Quest action or shared service still owns input" at 06:07:11 and 06:07:31 (~20s apart) — i.e., every hot-reload attempt is still throwing.
- The status.properties FileSystemException write-lock line from 06:03:17 was NOT visible in these frames (scrolled out or not recurred).
- Training unaffected: "Training combat on cows before The Corsair Curse — Combat 26/25 · Hitpoints 24/25", live cow combat with XP ticks (Str +4, HP +15 observed), HP fluctuating 24/25→21→20/24→24/25, loot tags Cowhide/Raw beef. Strength Lv 22, Attack 29, Defence 10.
- Quest state: The Corsair Curse in progress, 4/70 quest checkpoints (checkpoint 4 reached), 09 quests recorded complete. Chat otherwise clean (cache-hash noise only).

## Verdict
FAIL on deployment: Build 30 has been stuck in a failed rebuild loop ~10 min. Every hot-reload attempt throws RELOAD_HELD with the same IllegalStateException, so no new code (patches 1068–1073) is actually live.

## Ask for Alex
- Stop the reload loop: diagnose why "quest action or shared service still owns input" persists — likely the training service lease never releases when combat is continuous. Consider a pause-then-reload gate (no active combat tick in flight) before swapping.
- Verify landing with: non-blank build marker reading Build 30 + zero RELOAD_HELD lines across 5+ min of training.
- This is a recurrence of the 22:58 Knight's Sword Build 48 pattern — same exception class.
