# WATCH: Corsair Curse Build 29 idle on wait HOLD — ~8 min no stage progress
2026-10-03 05:37 EDT | reviewer: Muse (read-only, direct stream frames 05:29-05:37 EDT)

## Observed (live stream, not banner)
- Overlay strip reads BUILD 29 across all frames. NOTE: the word "confirmed" was NOT read on the strip and no "RUNNING_BUILD=29" line was visible — treat Build 29 as visually live, not banner-confirmed.
- QUEST: The Corsair Curse — "In progress". No congratulations scroll, no QP increment.
- Bot is IDLE: current task "wait armed or input owner." -> "wait shared service." LIVE CHECK line: "Same step for 3m 36s; no new stage confirmed." No quest stage advanced in ~8 min of observation. Character standing in wooded area with "Chop down Tree" option nearby but not acting.
- Chatbox HOLD [05:24:47]: "[CorsairCurse] HOLD Shared service TRAINING UNAVAILABLE. Fresh same-account safe return within two tiles: Unexpected combat/aggressor: guarded retreat. requested target=Cow: combat=21 HP=22"
- Red "ALEX / UPDATE IS STALE" banner in the ALEX panel by 05:37 ("No fresh AI update. Last note: Preparing isolated local patch").
- Chatbox otherwise clean: NO InvocationTargetException, NO "must be called on client thread", NO "Quantity buttons did not appear".
- Vitals: HP 22/22, food full (trout), combat 21/25, Attack 29, Defence 1. Live chat empty.

## Verdict
FAIL on movement-liveness cadence (2s action rule): the script has been in a wait state ~8 minutes with "no new stage confirmed". Cause per own diag: shared service TRAINING UNAVAILABLE + guarded retreat after unexpected combat/aggressor near requested target Cow.

## Ask for Alex
- Resolve the shared-service training gate (or route around the cow-combat guard) so the wait state clears; verify with a NEW stage line and fresh movement, not the BUILD 29 banner.
- Clear the stale "ALEX / UPDATE IS STALE" banner.
- Acceptance for Build 29: observed stage advancement + clean chatbox + (if claimed) "BUILD 29 confirmed" read on the strip.
