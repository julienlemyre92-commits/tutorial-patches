# Doric Builds 10/11/12 review verdict — PASS (2026-10-01 05:14 EDT, Muse read-only review)

Reviewed patches: 648 (Build10, commit ca779646 @ 09:10:37Z), 649 (Build11, commit 5f9bf87a @ 09:11:10Z), 650 (Build12, commit 27b6a30d @ 09:13:12Z). All three: zip integrity OK, 215 entries, net/-rooted, version.txt=N at zip root matching repo version. META-INF/MANIFEST.MF present (jar cf build; harmless on the hot-reload path).

## Semantic deltas (javap -c diffs)
- **Build 10 (patch-648): code = Build-9 level, marker = 8.** The script class contains Build 9's login verify phases (VERIFY_FREE_WORLD / VERIFY_PLAY_NOW) and Build 8's client-thread observation retry, but the RUNNING_BUILD banner is hardcoded `bipush 8` — the same marker as Build 8. Only delta vs Build 9's class: the marker stayed at 8. When patch-648 hot-loads, the diag will read `RUNNING_BUILD=8` — a LYING BANNER. Judging acceptance from the banner would misattribute. Accept only from NEW runtime lines (VERIFY_* / WAIT_CLIENT_THREAD_OBSERVATION / phase transitions).
- **Build 11 (patch-649): marker-only bump.** Byte-identical logic to 648; only change is RUNNING_BUILD 8→11 (same four hardcoded marker sites). Verdict applies transitively from Build 10's.
- **Build 12 (patch-650): real change + marker 12.** In the hot-reload restore path (after clientReadTimeouts/clientReadRetryAt reset), new guard: if phase=="WAIT_FREE_WORLD_LIST" AND loginAttempts==0 AND disconnectAttempts==0 AND loginWorld==0 AND worldActionAt==0 → loginStartedAt=0, error cleared, then reloadStateRestored=true. Sensible: a restore with no in-flight login state restarts the native login observation cleanly instead of preserving stale phase/counters. No other semantic change (rest of the diff is constant-pool renumbering from the added string).

## Findings
- **[M] Lying banner on Build 10 (patch-648).** Diag RUNNING_BUILD will report 8 on Build-10 code. If the bot ever ran patch-648 and logs show RUNNING_BUILD=8, that does NOT mean Build 8 is running. Verify by behavior/lines, not the banner. Fixed at 649+ (marker 11/12).
- Carry-forwards (unchanged in all three): blocking cross-map walkTo (~130 tiles Lumbridge→Rimmington; >120s stall = terminal unproved HOLD), terminal MINE_/no-rock HOLDs (single-shot, no retry), sticky HOLD_CLIENT_THREAD after 4 client-thread timeouts (needs full script restart; Build 8's retry covers pure observations only).
- Manifest sha256 field does not match shipped zip bytes (consistent with 643-647; informational only — the host evidently does not hard-reject).
- Commit messages reuse Build 8's text verbatim for all three ships (informational).

## Verdict: PASS — no defects that would block hot-load or misbehave in-game.
## Live verification pending: screenshot feed dark since 2026-09-30 17:44 EDT; watching for RUNNING_BUILD=12 banner + first VERIFY_FREE_WORLD / TO_RIMMINGTON_MINE lines. (For Build 10/11, ignore the banner number.)
