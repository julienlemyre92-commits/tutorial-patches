# Read-only review verdict: Below Ice Mountain Build 29 (patch-894)

**Verdict: PASS — custody air tight, diff surgical, matches stated intent. No new defects. One watch item carried forward.**

Reviewer: Muse (read-only; Alex owns implementation/releases — no edits, no ships).

## Ship facts (all verified via GitHub API, 2026-10-02 ~00:43–00:45 EDT)
- `version.txt` = 894 (repo), patch-894.zip + belowicemountain-29.jar + patch-894.hot.json shipped in commit `725a1c9d` @ 04:41:10Z ("Below Ice Mountain Build29 stage15 route time budget"). 893→894 sequential, no number reuse, no overwrite.
- Custody: patch-894.zip = 277 entries, all `net/`-rooted (only non-net entries: META-INF/ + version.txt); in-zip `version.txt` = 894 == repo. hot.json sha256 `bb70705d…0d781d0a12f` == belowicemountain-29.jar bytes, FULL MATCH. All 7 script classes byte-identical zip↔jar (the 3 zip-only classes are Plugin/Config — by design the jar is the script-only hot-load artifact). `BUILD_NUMBER=29` in published source line 62.
- Source diff b28→b29 (published source-review dirs): exactly 2 functional lines —
  - `BUILD_NUMBER` 28→29
  - watchdog limit: `questStage(f.varp)==15 && needsSupplies(f) ? 600000 : 180000` → `questStage(f.varp)==15 ? 600000 : 180000` (+2 comment lines: stage-15 routes span multiple towns; the separate route watchdog still bounds stationary/unreachable walking).

## Intent vs. evidence
- README claim: B28 proved Bread + cooked meat, walked Rimmington→Marley; at (3088,3469) the stage-15 unchanged-signature watchdog HOLDed after 3 minutes before the sandwich action ran; B29 gives stage 15 ten minutes regardless of inventory.
- Code matches the claim exactly: old limit was 600000 only when `needsSupplies(f)` true, else 180000 (3 min) — with supplies already held, the 3-min watchdog fired. New limit is 600000 for stage 15 unconditionally.
- Provenance caveat (read-only discipline): the (3088,3469) HOLD is Bot Maker 2's report, not independently observed — screenshot feed dark since 2026-09-30 17:44 EDT (~32h at review time), no fresh diag visible to this reviewer. The mechanism diagnosis is consistent with the code.

## Findings
- No defects in the delta. The trade-off is deliberate and documented: a genuinely stalled stage-15 route now gets 10 min instead of 3 before HOLD. The independent route watchdog (stationary/unreachable segments) is untouched and still bounds real stalls.
- WATCH ITEM: the 00:41 EDT stream check saw "Walk marley sandwich" un-advanced 2+ min with a mostly-idle character and red-X route marks on the minimap. Under B29, if that route is truly blocked rather than slow, the bot will idle up to 10 min before the watchdog HOLDs. If the next live check still shows no stage change on "walk marley sandwich", the route plan (not the watchdog) is the suspect.
- Carried open (unchanged by B29): amount-dialog defect (flour withdraw up to 3, no "How many?" handler — moot while dough flow is complete, but the bank path still allows it); BIM26-1 (dough fast-path selects by widget position 270,15, not product identity — superseded by the proven inventory gains).

## Live acceptance (pending)
- RUNTIME BUILD: 29 marker + "walk marley sandwich" advancing (or a clean stage-15 HOLD with the route watchdog's own diag) on the live client. Screenshot feed dark ~32h; stream is the only visual path until Julien's feed returns.
