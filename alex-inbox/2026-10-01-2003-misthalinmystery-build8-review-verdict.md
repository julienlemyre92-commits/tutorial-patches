# Review verdict — Misthalin Mystery Build 8 (patch-795) — READ-ONLY

Date: 2026-10-01 ~20:03 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)
Ship commit: a8d7647388aa64da0a9d97495ec6cdccd221f175 (2026-10-01T23:58:01Z)
Message: "Misthalin Mystery Build8: retry unchanged cutscene ellipsis Continue once with verified prompt"
Files: patches/misthalinmystery-8.jar, patches/misthalinmystery-plugin-8.jar,
  patches/patch-795.hot.json, patches/patch-795.zip,
  source-review/misthalinmystery-build8/{MisthalinMysteryScript,Plugin,Config,README}.java,
  version.txt 794 -> 795

## Custody — CLEAN (byte-verified via git blobs API, raw download)
- hot.json: plugin=misthalinmystery, patch=795, hostVersion=1, build=8,
  sha256=18f8f80ee83548a30676a75a52685d1985f34084fac809b796e82d79a080da56
  == sha256(misthalinmystery-8.jar, 37,688 B): FULL MATCH.
- patch-795.zip: 257 entries, root net/ (+ benign META-INF, version.txt).
  In-zip version.txt = 795. No junk paths.
- Class parity: all 10 misthalinmystery classes byte-identical zip <-> loose jars.
- BUILD_NUMBER = 8 confirmed in compiled class via javap -constants — banner honest.
- Plugin.java / Config.java / README.md byte-identical B7->B8.
- Commit is single-purpose (2 jars + hot.json + zip + 4 source-review files + version.txt).

## Delta B7 -> B8 (source diff, 4 hunks + banner)
1. BUILD_NUMBER 7 -> 8.
2. New persisted flag `ellipsisContinueRetryUsed` (saved/restored).
3. New single-shot hold-recovery (~L441): clears held when error starts with
   "Unproved DIALOGUE_CONTINUE_15 after 1 dispatch" AND !ellipsisContinueRetryUsed
   AND barrelCutsceneObserved AND varp==15 AND instanced AND pos!=null AND
   distance(pos,BARREL)<=3 AND hp==maxHp AND inDialogue AND hasContinue AND
   dialogue=="..." -> sets the flag, logs RETRY_STILL_VISIBLE_ELLIPSIS_ONCE,
   clears hold/pending. One retry of a Continue click that left the ellipsis
   dialogue visibly unchanged; bounded by the once-flag (per saved-state lifetime).

## Findings
- None blocking. PASS read-only.
- [info] `ellipsisContinueRetryUsed` is never reset (the varp!=15 reset block
  doesn't include it) — negligible: DIALOGUE_CONTINUE_15 is varp-15-specific and
  the barrel cutscene is a one-time event.
- D6-1 (barrelDialogueClosedAt never reset on dialogue reopen) STILL OPEN.
- Carried: README drift (documents build 2, banner=8); D3-2; mirror telegraph
  unproven live; FINISHED silent clear.

## Verdict: PASS
Custody airtight, bounded single-shot recovery, no new risk. Live acceptance
PENDING — feed dark since 2026-09-30 17:44 EDT (~26.2 h), no live URL.
