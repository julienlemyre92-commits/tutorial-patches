# Review verdict — Misthalin Mystery Build 7 (patch-794) — READ-ONLY

Date: 2026-10-01 ~20:02 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)
Ship commit: 472af09dc6c713aab19efd9d02c4092a3a930e26 (2026-10-01T23:56:24Z)
Message: "Misthalin Mystery Build7: resume verified barrel dialogue after prior second unproved dispatch"
Files: patches/misthalinmystery-7.jar, patches/misthalinmystery-plugin-7.jar,
  patches/patch-794.hot.json, patches/patch-794.zip,
  source-review/misthalinmystery-build7/{MisthalinMysteryScript,Plugin,Config,README}.java,
  version.txt 793 -> 794

## Custody — CLEAN (byte-verified via git blobs API, raw download)
- hot.json: plugin=misthalinmystery, patch=794, hostVersion=1, build=7,
  sha256=ed3d37727809da57744c9a91db64f35d8fb30f62c874edc6a05e5cb5b6421987
  == sha256(misthalinmystery-7.jar, 37,519 B): FULL MATCH.
- patch-794.zip: 257 entries, root net/ (+ benign META-INF, version.txt).
  In-zip version.txt = 794. No junk paths.
- Class parity: all 10 misthalinmystery classes byte-identical zip <-> loose jars.
- BUILD_NUMBER = 7 confirmed in compiled class via javap -constants — banner honest.
- Plugin.java / Config.java / README.md byte-identical B6->B7.
- Commit is single-purpose (2 jars + hot.json + zip + 4 source-review files + version.txt).

## Delta B6 -> B7 (source diff, 3 hunks + banner)
Widens the B5 `BARREL_INSTANCE_DIALOGUE_PROVED` false-hold recovery:
1. Error prefix loosened: "Unproved SEARCH_BARREL_FIRST after 1 dispatch" ->
   "Unproved SEARCH_BARREL_FIRST after " (any dispatch count).
2. Dialogue gate widened: `"Woo, party on bro!".equals(f.dialogue)` ->
   `f.inDialogue && ("Woo, party on bro!".equals(f.dialogue) || f.dialogue.startsWith("Woah, that wind"))`.
3. Now sets `barrelCutsceneObserved=true` when clearing the hold, routing subsequent
   ticks into the B6 cutscene machinery.
- This is Alex reacting to live observation: the barrel cutscene has a second
  dialogue text starting "Woah, that wind...", which the B5 exact-match gate missed.

## Findings
- None new blocking. PASS read-only.
- D6-1 (barrelDialogueClosedAt never reset on dialogue reopen) NOT addressed — STILL OPEN.
- Carried: README drift (documents build 2, banner=7); D3-2; mirror telegraph
  unproven live; FINISHED silent clear.

## Verdict: PASS
Custody airtight, targeted widening of an evidence-gated recovery, no new risk.
Live acceptance PENDING — feed dark since 2026-09-30 17:44 EDT (~26.2 h), no live URL.
