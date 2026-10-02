# Review verdict — Misthalin Mystery Build 6 (patch-793) — READ-ONLY

Date: 2026-10-01 ~20:01 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)
Ship commit: 081370e72dd3dc717d6cd5be2f5b6732911cc228 (2026-10-01T23:55:30Z)
Message: "Misthalin Mystery Build6: continue observed barrel cutscene dialogue and bound repeat search"
Files: patches/misthalinmystery-6.jar, patches/misthalinmystery-plugin-6.jar,
  patches/patch-793.hot.json, patches/patch-793.zip,
  source-review/misthalinmystery-build6/{MisthalinMysteryScript,Plugin,Config,README}.java,
  version.txt 792 -> 793

## Custody — CLEAN (byte-verified via git blobs API, raw download)
- hot.json: plugin=misthalinmystery, patch=793, hostVersion=1, build=6,
  sha256=38cce2b4a30d64b4bd8cf5f96eacf56797916ad848a502d81de1946e685a138b
  == sha256(misthalinmystery-6.jar, 37,688 B): FULL MATCH.
- patch-793.zip: 257 entries, root net/ (+ benign META-INF, version.txt).
  In-zip version.txt = 793. No junk paths.
- Class parity: all 10 misthalinmystery classes byte-identical zip <-> loose jars.
- BUILD_NUMBER = 6 confirmed in compiled class via javap -constants — banner honest.
- Plugin.java / Config.java / README.md byte-identical B5->B6.
- Commit is single-purpose (2 jars + hot.json + zip + 4 source-review files + version.txt).

## Delta B5 -> B6 (source diff, script only)
1. BUILD_NUMBER 5 -> 6.
2. New persisted state (saved/restored): `barrelCutsceneObserved`, `barrelDialogueClosedAt`.
3. Pending-proof branch (~L667): `SEARCH_BARREL_FIRST` pending with varp==15 AND
   instanced AND pos near BARREL (template, <=3) AND inDialogue AND hasContinue ->
   barrelCutsceneObserved=true, pending=null, phase label BARREL_CUTSCENE_DIALOGUE,
   LOG line with stage/pos/dialogue text. Stops re-searching while the cutscene
   dialogue is up.
4. New hold-recovery (~L437): clears held when error starts with
   "Reload during SEARCH_BARREL_FIRST" AND varp==15 AND instanced AND pos near
   BARREL AND hasContinue AND inDialogue -> logs BARREL_RELOAD_DIALOGUE_PROVED,
   clears hold, sets barrelCutsceneObserved=true. Evidence-gated, same pattern as B5.
5. varp!=15 resets both flags (stage-change hygiene).
6. Stage-level (~L957): varp==15 AND barrelCutsceneObserved -> stamp
   barrelDialogueClosedAt on first entry; >10 s since stamp with quest still
   varp15 -> terminal HOLD ("Barrel cutscene dialogue ended but quest remained
   varp15; inspect scene before another Search"); else phase label
   WAIT_BARREL_STAGE_AFTER_DIALOGUE. Bounded wait instead of a search loop.
- Ordering verified: generic `dialogue(f)` continuation runs BEFORE `stage(f)` in
  tick(), so an open cutscene dialogue is continued, not preempted by the timer branch.

## Findings
- [LOW] D6-1 (NEW): `barrelDialogueClosedAt` is stamped once and never reset when
  the cutscene dialogue re-opens for another segment — the 10 s bound measures
  total elapsed since the FIRST close, not continuous post-dialogue idle. A
  multi-segment cutscene spanning >10 s would false-HOLD. Suggest resetting the
  stamp in the dialogue-observed branch (~L667). Fail-closed and diagnosable;
  low reachability (the barrel cutscene looks single-dialogue).
- Carried: README drift (documents build 2, banner=6); D3-2; mirror telegraph
  unproven live; FINISHED silent clear.

## Verdict: PASS WITH FINDINGS
Custody airtight, delta coherent, one LOW logic nit. Live acceptance PENDING —
feed dark since 2026-09-30 17:44 EDT (~26.2 h), no live URL.
