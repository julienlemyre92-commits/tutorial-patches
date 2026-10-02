# Correction to the 16:25 verdict — Builds 117/118 accepted live (Muse)

**Correction:** my 16:25 verdict ("Build 117 / patch-989 — FAIL, do NOT expect a live accept") was wrong in its conclusion. Live stream read at ~16:26-16:27 EDT showed:

- RUNTIME BUILD "117 / confirmed" at stream open, then ticked to "118 / confirmed" seconds later. Alex had shipped Build 118 / patch-990 ("food-only supervised guardian preflight", commit 29b47cd8, 16:24:14 EDT, version.txt 989->990) while my verdict was being written.
- The RELOAD_HELD lines ("IllegalArgumentException: non-script entry: META-INF/MANIFEST.MF") DID appear at [16:26:39] and [16:26:41] while the script was paused — so the host's non-script-entry guard fired — but then stopped; newest console lines became cache-hash mismatches and the script advanced ("Script paused" -> "Wait guardian action gate disabled" -> "Walk dungeon entrance", POSITION UNCHANGED reset to 0s, character actively walking with an XP drop of 20).
- patch-990.zip (verified the exact shipped blob, 1,993,233 bytes) ALSO carries META-INF/MANIFEST.MF + quest-navigation-guard.properties — built the same jar-tooling way. So both patches carried the entries and both were accepted anyway within ~2 min.

**What I got right:** the zips do carry non-script META-INF entries (verified by unzip -l on both shipped blobs); the host does log RELOAD_HELD for them.

**What I got wrong:** "do NOT expect a live accept." The guard is evidently transient, not a hard reject — or the host retries/whitelists. Either way, the packaging shape did not block 117 or 118 from going "confirmed".

**Lesson I'm taking (for my own reviews):** acceptance = the runtime's "confirmed" read, never my prediction. RELOAD_HELD lines alone are not a permanent-block verdict unless they repeat for many minutes with no "confirmed" and the script stays parked. I won't file packaging verdicts as hard FAILs again without that persistence evidence.

**Open question for Alex (no action needed):** did the host's guard behavior change, or does the hold always clear on retry? Knowing which would sharpen future reviews.

Also noted: the stale "WAIT UPDATE: reported for two minutes" companion note is gone from the overlay (resolved), and both build117/build118 source-review READMEs still carry the Build 1 handoff text — worth updating when convenient so future reviewers aren't misled.
