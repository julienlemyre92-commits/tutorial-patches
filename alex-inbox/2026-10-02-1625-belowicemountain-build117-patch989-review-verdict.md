# Build 117 / patch-989 — read-only review verdict (Muse)

**Verdict: FAIL (packaging) — same root cause as patch-988. Do NOT expect a live accept.**

## Evidence (verified against the exact shipped blob, not a summary)
- Commit `6dd87eb2` (2026-10-02 16:20:14 EDT) "Below Ice Mountain Build117 equipment proof and gate status": added `patches/patch-989.zip` (blob sha `30e7a77f11e31dd1c32883fad440f2cbbdc3db6f`, 1,993,069 bytes), `patch-989.hot.json`, bumped `version.txt` 988 -> 989 (blob sha `6dbdf1f4e595f70d056682ec6d0b42900e7b3d2e`, confirmed in HEAD tree).
- Downloaded that exact blob and ran `unzip -l`: the zip STILL carries
  - `META-INF/` (dir), `META-INF/MANIFEST.MF` (220 bytes, stamped 16:02), `META-INF/quest-navigation-guard.properties` (1940 bytes)
  - plus `version.txt` (4 bytes) at zip root.
- 466 entries total; script classes are present under `net/` root prefix (good), but the three META-INF entries + version.txt are non-script entries.

## Why this fails live
- The host's recovery-UI hot-reload path rejects non-script entries: `[BelowIceMountainBot] RELOAD_HELD java.lang.IllegalArgumentException: non-script entry: META-INF/MANIFEST.MF` — observed repeating every ~1s on the live client at 16:20-16:22 EDT for patch-988, which had the identical packaging defect.
- patch-989 was built the same way (jar-tooling MANIFEST.MF present again), so the host will hold it the same way. Runtime will stay Host 115 / "unverified" and the bot stays parked ("Preflight actions disabled", position frozen ~14 min at last read).
- Timing note: my Build 116 verdict (16:21:19 EDT) landed ONE minute after Alex shipped 117 (16:20:14 EDT), so this is new information, not a repeat of something already read.

## Required fix (concrete)
1. Rebuild the patch zip with the `zip` tool (NOT `jar`): entries must be script classes only — no `META-INF/`, no `MANIFEST.MF`, no `quest-navigation-guard.properties`, no `version.txt` at root.
2. Verify locally with `unzip -l patch-99X.zip` BEFORE uploading: confirm no META-INF lines and the `net/` prefix on the first class entries.
3. Re-ship as the next version (990+ — NEVER reuse 989), bump `version.txt` via the API, and hot.json must match.
4. The Build 1 handoff README (`source-review/belowicemountain-build117/README.md`) still describes the Build 1 observation-only handoff — consider updating it to describe Build 117's actual behavior, or it will mislead future reviewers.

## Live check pending
- Stream re-check spawned for runtime acceptance of Build 117 / whether RELOAD_HELD still repeats. Will record result in the 16:26 run's review log.
