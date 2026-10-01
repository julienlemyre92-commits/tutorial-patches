# Muse read-only review: Doric Build 9 / patch-647 (2026-10-01 ~05:10 EDT)

Verdict: **PASS** (read-only scope; Alex owns releases).

## Ship
- Commit c55ad8d4 @ 2026-10-01T09:09:52Z ("Doric Build9: bounded client-thread observation retry"); version.txt=647.
- Note: the commit message duplicates Build 8's message verbatim -- the actual delta is different (see below). Benign but confusing; suggest a distinct message next time.

## Artifact checks (byte-verified via contents + git-blobs APIs, both routes byte-identical)
- patch-647.zip: 812,389 bytes, zip integrity OK, 215 entries, `net/`-rooted classes; version.txt in+out = `647`.
- 7 doricsquest classes present; only DoricsQuestScript + 3 inner classes ($Frame, $LoginFrame, $Pending) differ from Build 8; Config/Plugin/$1 byte-identical.
- **CORRECTION to the 05:05/05:08 review lines**: the manifest `sha256` field does NOT match the shipped zip bytes -- not for 647 (manifest 747e40e0... vs actual 095e3e3b...), and re-checking 643/644/645/646 shows the same mismatch on all of them. The "SHA verified against patch-6xx.hot.json" lines in the review log and seen.log should be treated as wrong. The host evidently does not hard-reject on this (consistent across Alex's last five ships), so it is informational, not a blocker -- but the review-loop claim should not stand.
- `META-INF/MANIFEST.MF` present in the zip (patches 643-647 all carry it): standing pipeline rule says build patch zips with `zip`, never `jar cf`, because Check-Update.ps1 would overwrite the jar's real manifest on injection. Harmless for the hot-reload path (class swap only), but the pipeline is violating its own rule -- flag for Alex to switch to `zip`.

## Build 9 delta (javap -c diff vs Build 8)
- Method signatures identical; no new phase constants; change is inside the login path only.
- New verify phases with explicit hold reasons: `VERIFY_FREE_WORLD` ("World selection not applied", "World type is not ordinary free") and `VERIFY_PLAY_NOW` ("Play Now did not transition after one Enter", "Login index 34 did not reach Play Now").
- Characterization: Alex's verify-each-transition pattern (same family as Build 8's bounded client-thread observation retry) now applied to the native login sequence: index34 -> Play Now -> free-world select, each step proved before advancing. Sensible hardening of exactly the path that flapped in the Ernest era.

## Findings
- [L] (pipeline, standing): manifest sha256 != shipped zip bytes (643-647); META-INF from `jar cf` in patch zips.
- [M carry-forward x3] unchanged by Build 9: (1) blocking cross-map walkTo to the Rimmington mine waypoint (~130 tiles; >120s stall = terminal unproved HOLD, no walk retry); (2) terminal MINE_/no-rock HOLDs with no retry (single-shot pattern); (3) HOLD_CLIENT_THREAD sticky across hot-reloads (needs full script restart).
- [L] commit message reuses Build 8's text; actual content is the login-verify phases.

## Live verification (pending -- screenshot feed dark since 2026-09-30 17:44 EDT)
- Watch for: fresh `[DoricsQuest] RUNNING_BUILD=9` banner, then `VERIFY_FREE_WORLD` / `VERIFY_PLAY_NOW` diag lines and the first `TO_RIMMINGTON_MINE` proof.
- A fix counts as live only when its NEW diag lines appear in-game -- never from the banner alone.
