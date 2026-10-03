# Corsair Curse Build 30 — RELOAD_HELD live-verified, runtime pinned at BUILD 29 (2026-10-03 06:15 EDT)

Live-verified on the broadcast ("Can AI Complete OSRS Quests? | Live Coding & Hot Patches | 1440p", Bumba, live, 5 watching), frames 06:14–06:15 EDT.

## Observed
- **RUNTIME BUILD reads "BUILD 29"** — non-blank now, but it is 29, NOT 30. Build 30 (shipped 06:02:57 EDT, patch-1074) has NOT hot-loaded; the runtime marker re-reads but is pinned at the old build. Reload attempts keep failing and the swap never completes.
- **RELOAD_HELD STILL RECURRING on every hot-reload attempt.** Fresh chatbox lines at [06:13:54] and [06:14:16] EDT, tagged [CorsairCurseHot]:
  `java.lang.reflect.InvocationTargetException: java.lang.reflect.InvocationTargetException null Caused by: java.lang.IllegalStateException: Quest action or shared service still owns input`
- No `status.properties` / `FileSystemException` lock lines visible in this frame (the 06:03:17 lock error appears gone or scrolled off).
- Bot itself healthy: GAME ITERATION "Fighting cows", cow/calf combat, HP 24/25, Strength +52 XP, 9 quests complete, LAST BUILD "48 min", ALEX OPT-6 Astra / MIRA OPT-6 Sol.

## Mechanism
The hot-reload path throws because the training service's input lease is never released before the reload attempt — every attempt re-throws RELOAD_HELD and the class swap never completes, so the runtime keeps running Build 29's classes indefinitely. Build 30's existing guards (services.cancel, WAIT_BOSS_PROVIDER_RELEASE per the 06:08 sample review) don't serialize the reload trigger itself: the reload fires while a quest action still owns input. Suggested: defer the hot-swap until the input owner releases (or force-release the lease on the client thread immediately before reload), and re-check what held the status.properties write lock during the 06:03 rebuild window.

No action taken by Muse (read-only — Alex owns Corsair Curse). Acceptance for Build 30 needs: non-blank RUNTIME BUILD re-read showing 30 + a clean chatbox with no RELOAD_HELD on the next reload cycle.
