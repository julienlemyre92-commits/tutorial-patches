# WATCH: Build 29 in-progress rebuild + RELOAD_HELD return + status.properties write-lock
2026-10-03 06:05 EDT | reviewer: Muse (read-only, direct stream frames 06:02:53-06:04:30 EDT)

## Observed (live stream, not banner)
- RUNTIME BUILD and LAST BUILD overlay fields went BLANK mid-window ("BUILD —" / "—") at ~06:03:31 and stayed blank through 06:04:30 — in-progress rebuild. Right panel: "Adding completion stop-file check"; BACKSTAGE: "Builds upcoming scripts".
- NEW ERROR 1 (06:03:17): "[CorsairCurse] status java.nio.file.FileSystemException: C:\Users\Wo11.runelite\corsaircurse\status.properties: The process cannot access the file because it is being used by another process" — the morning watch item (status-file write lock) is now materializing on the live client. Same class as the Build 46 Knight's Sword lock at 22:51 yesterday.
- NEW ERROR 2 (06:03:01 and 06:03:19): "[CorsairCurseHot] RELOAD_HELD java.lang.reflect.InvocationTargetException caused by java.lang.IllegalStateException: Quest action or shared service still owns input" — RELOAD_HELD/InvocationTargetException recurring during hot-reload; echoes the Knight's Sword Build 48 episode (22:58).
- Training unaffected: GAME ITERATION "Fighting cows", live notes "Training combat on cows before The Corsair Curse — Combat 25/25 · Hitpoints 24/25", XP ticks (Str +36, Atk +8, HP +15), cowhide/raw beef drops. Chatbox otherwise clean (cache-hash noise only).
- No mention of patch-1073 / FitToWound / LevelUpTabCue in visible chatbox; patch-1072/1073 hot-load uptake still unobserved.

## Verdict
FAIL on stability: hot-reload now throws while a quest action or shared service owns input, and the status writer is colliding with another process holding status.properties.

## Ask for Alex
- Serialize the hot-reload path: wait for (or release) quest-action/shared-service input ownership before swapping classes; verify with a clean reload — no RELOAD_HELD lines.
- Fix status.properties contention (exclusive write lease or retry-with-backoff; coordinate with whatever else holds the file); verify with no FileSystemException across a rebuild.
- Acceptance: rebuild lands, build marker re-reads non-blank, chatbox shows neither error across 5+ min of cow training.
