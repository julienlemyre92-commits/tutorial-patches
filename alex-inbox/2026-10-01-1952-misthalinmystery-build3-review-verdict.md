# Review verdict — Misthalin Mystery Build 3 (patch-790) — READ-ONLY

Date: 2026-10-01 ~19:52 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)
Ship commit: 7cfa11abe5fd64c4d537014fdc4a4e21d1f0c756 (2026-10-01T23:47:19Z)
Files: patches/misthalinmystery-3.jar, patches/misthalinmystery-plugin-3.jar,
  patches/patch-790.hot.json, patches/patch-790.zip,
  source-review/misthalinmystery-build3/{MisthalinMysteryScript,Plugin,Config,README}.java,
  version.txt 789 -> 790

## Custody — CLEAN (byte-verified)
- hot.json: plugin=misthalinmystery, patch=790, hostVersion=1, build=3,
  sha256=aca7acc64fff76021c967573b88107c365bf28683a8d48b6791e9b86ea9ec51b
  == sha256(misthalinmystery-3.jar, 35864 B): FULL MATCH.
- patch-790.zip: 257 entries, root net/ (+ benign META-INF/MANIFEST.MF 220 B,
  version.txt). In-zip version.txt = 790. No junk paths.
- Class parity: 7/7 script classes + 10/10 plugin classes byte-identical
  zip <-> loose jars (Script, Script$1, Script$Frame, Script$Graphic,
  Script$Pending, Script$Proof, Script$Route + Config, Plugin, Plugin$1).
- BUILD_NUMBER = 3 confirmed in compiled class via javap -constants — banner honest.
- Commit is single-purpose (jars + hot.json + zip + 4 source-review files + version.txt).

## Delta B2 -> B3 (source diff, 3 hunks + banner)
1. BUILD_NUMBER 2 -> 3.
2. NEW false-hold recovery (Script.java ~L412): clears held/error
   "Route TALK_ABIGALE segment ended without progress" ONLY when observed state
   proves the goal was reached: varp==0 AND pos==(3222,3219,0) exact AND
   hp==maxHp AND foodCount>=4 AND pending==null. Logs RESUME_VERIFIED_LUMBRIDGE_TELEPORT.
   Gates are narrow and evidence-based.
3. Route progress predicate changed (routeTick ~L768): OLD `dist<r.bestDistance`
   (approach-only) -> NEW `!f.pos.equals(r.lastPos)` (any observed movement
   counts; bestDistance tracked via Math.min). Commit message matches:
   "count verified teleport and detour movement as route progress".
   Sound: lastPos is observed state; teleport position jumps no longer
   misread as "no progress".
4. Stall terminal hold "Route <key> segment ended without progress" REPLACED by
   unconditional startRouteSegment(r). Bounded stalls RETAINED: 20 s
   no-position-change -> HOLD, 180 s total route age -> HOLD, 10 consumed
   segments -> HOLD ("consumed ten bounded walker segments"). Each segment's
   walkWithStateUntil carries its own 15 s deadline. No new livelock: worst
   case before terminal HOLD is ~180 s or 10 segments.

## Findings
- [LOW] D3-1: README.md is byte-identical to B2's (5338 B): documents
  RUNNING_BUILD=2, expectedBuild=2, build-2 jar names. actionsEnabled()
  requires expectedBuild == Integer.toString(BUILD_NUMBER) == "3" EXACTLY,
  so a literal README-following enable leaves the script status-only.
  Practical impact low (Alex drives the enable, not the README), but the
  drift is concrete — bump the doc with the banner.
- [LOW/info] D3-2: the TALK_ABIGALE resume gate matches an error string B3
  itself can no longer emit (that hold branch was replaced by the
  unconditional segment restart). It only fires for a B2-persisted hold under
  a state-preserving hot-load; hot reload resets memory-only flags in the
  common case, so reachability is narrower than it looks. Harmless.
- [carried] mirror telegraph (varp 110/111) unproven live (B2 self-flagged);
  FINISHED branch clears held/error silently (B2 LOW).

## Verdict: PASS WITH FINDINGS
Custody airtight, delta small and coherent, stall bounds preserved, no
HIGH/MEDIUM defects. Live acceptance PENDING — feed dark since 2026-09-30
17:44 EDT (~26.1 h), no live URL; nothing live-verified from here.
