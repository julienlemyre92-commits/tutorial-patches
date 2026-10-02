# Review verdict — Misthalin Mystery Build 4 (patch-791) — READ-ONLY

Date: 2026-10-01 ~19:59 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)
Ship commit: cefe01b89f7b64220acca7fd0fed2a687e5ffd4c (2026-10-01T23:51:14Z)
Message: "Misthalin Mystery Build4: diagnose live instance template coordinate mapping at island varp15"
Files: patches/misthalinmystery-4.jar, patches/misthalinmystery-plugin-4.jar,
  patches/patch-791.hot.json, patches/patch-791.zip,
  source-review/misthalinmystery-build4/{MisthalinMysteryScript,Plugin,Config,README}.java,
  version.txt 790 -> 791

## Custody — CLEAN (byte-verified via git blobs API, raw download)
- hot.json: plugin=misthalinmystery, patch=791, hostVersion=1, build=4,
  sha256=b1ac13322acbef95d8982d64a36d282ae949a196f911efda5fb5c69df57d3fef
  == sha256(misthalinmystery-4.jar, 36,149 B): FULL MATCH.
- patch-791.zip: 257 entries, root net/ (+ benign META-INF/MANIFEST.MF, version.txt).
  In-zip version.txt = 791. No junk paths.
- Class parity: 7/7 script classes + 3/3 plugin classes byte-identical zip <-> loose
  jars (plugin jar carries all 10 classes — established packaging, same as B2/B3).
- BUILD_NUMBER = 4 confirmed in compiled class via javap -constants — banner honest.
- Plugin.java / Config.java / README.md byte-identical B3->B4 (sha256 match).
- Commit is single-purpose (2 jars + hot.json + zip + 4 source-review files + version.txt).

## Delta B3 -> B4 (source diff, 4 hunks + banner) — DIAGNOSTIC ONLY
1. BUILD_NUMBER 3 -> 4.
2. Frame gains `WorldPoint templatePos`, `boolean instanced`, `String bucketInstances`.
3. Frame capture adds: `f.instanced=c.isInInstancedRegion()`,
   `f.templatePos=WorldPoint.fromLocalInstance(c, localPlayer.getLocalLocation())`,
   `f.bucketInstances=WorldPoint.toLocalInstance(c, BUCKET).toString()`.
4. Status properties gain `instanced`, `templatePosition`, `bucketInstanceCandidates`.
- The three new fields have ZERO logic consumers — they flow only to the status
  file. This build observes whether the island region is instanced, what template
  tile the player stands on, and where the bucket template tile (1619,4816) maps in
  the live instance. No behavior change.

## API verification (decompiled WorldPoint from installed microbot-base.jar)
- `fromLocalInstance` never returns null (instance -> template-chunk mapping,
  else plain fromLocal). `toLocalInstance(Client, WorldPoint)` returns
  `Collection<WorldPoint>` — never null (empty list when the target is absent from
  the instance; singleton(worldPoint) when not instanced and in scene). So the three
  new reads are null-safe, `.toString()` included.
- (info) the build uses the `@Deprecated` `toLocalInstance(Client, ...)` overload —
  functional, note only.
- No new thread hazard: the reads sit in the frame-capture block already running on
  the client thread via `invoke(Supplier)`.

## Findings
- None blocking. PASS read-only.
- Carried: D3-1 README drift (now documents build 2 while banner=4 — drift widened);
  D3-2 TALK_ABIGALE resume gate matches an error string B3+ can no longer emit
  (harmless); mirror telegraph (varp 110/111) unproven live; FINISHED branch clears
  held/error silently (B2 LOW).

## Verdict: PASS
Custody airtight, delta purely observational, no logic change. Live acceptance
PENDING — feed dark since 2026-09-30 17:44 EDT (~26.2 h), no live URL.
