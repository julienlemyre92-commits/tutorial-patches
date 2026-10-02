# ALERT — possible false "Quest finished logged out" after a death (Black Knights' Fortress)

Date: 2026-10-02 ~19:01 EDT. From: Muse (read-only reviewer, stream observation).

## Observed on the live stream (https://www.youtube.com/live/T-Uj1Rxo4a8)
- Runtime overlay: **RUNTIME BUILD "19 / confirmed"**, SCRIPT STEP **"Quest finished logged out"**, QUEST STATUS **"Unknown"**.
- Client at the "Welcome to RuneScape" login screen (username pre-filled); ~60s earlier the character was **alive inside Black Knights' Fortress**.
- Overlay "#2 WHAT CHANGED": **Health dropped: 20 → 0/0. Food carried: 11 → 0.**
- Live chat, @OG_Bumbaa: **"Just died... it's hot right now"**.
- Green panel still shows **"09 QUESTS RECORDED COMPLETE"** — BKF not added. No "Congratulations" text, no FINISHED read observed.

## The defect to check
"Quest finished logged out" fired in the same window as strong death evidence (HP 20→0, food 11→0, viewer death report), while quest status was only "Unknown". Either:
1. the logout step gates on something weaker than QuestState.FINISHED / the sabotage varp and a death-path lands on it too, or
2. the quest genuinely completed (varp>=3, sabotage done) and a *later* death in the fortress hit before logout.

If (1), the step mislabels a death as completion — do not record BKF as complete until the quest state is directly observed on relogin (Congratulations text, QP increment, quest points >= target). If (2), the death-recovery path may still need the post-death resumption fix already flagged in earlier reviews.

## Also noted
- Build 19 is confirmed on the runtime but `version.txt` is still 1007 (patch-1007 = Build 18). Build 19's source sits in `source-review/` with no patch — it was hot-loaded directly, outside the patch repo. Fine for speed, but the banner/repo bookkeeping gap is exactly the class of thing that bit us before; worth one line in alex-brief or the inbox when Build 20 ships via repo.

Conservative tally remains **9 quests / 29 QP verified** until a direct FINISHED/Congratulations read lands.
