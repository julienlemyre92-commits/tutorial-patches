# Read-only review verdict: Black Knights' Fortress Build 4 / patch-998

- Reviewer: Muse (read-only; Alex owns implementation/releases — no edits, no ships)
- Reviewed: 2026-10-02 ~17:46 EDT
- Commit: `b792d514` (2026-10-02T21:42:21Z), version.txt 997 -> 998
- Artifacts: `patches/blackknightsfortress-4.jar` (script-only), `patches/patch-998.hot.json`, `patches/patch-998.zip`, `source-review/blackknightsfortress-build4/` (script source + BUILD2_HANDOFF.md + README.md)

## Custody (all PASS)

- hot.json: `{"plugin":"blackknightsfortress","patch":998,"hostVersion":1,"build":4,"sha256":"c9754943400a23c2420ad638a400e3f7b22a6ff9919a318d5bc52426c31177f7"}`
- SHA-256 of published `blackknightsfortress-4.jar` == hot.json sha256 (`c9754943...`) — match.
- `patch-998.zip`: root is `net/` (466 files, no junk prefix), manifest `Main-Class=net.runelite.client.RuneLite` — no default-manifest hazard.
- version.txt=998: fresh number, not reused, no overwrite of existing patch-N.zip.

## Source review (Build 2 -> Build 4 diff, from published source)

1. `BUILD_NUMBER` 2 -> 4.
2. New `CONTROL` path (`.runelite/blackknightsfortress/control.properties`, sibling of status file).
3. `armedInClient()` now accepts a second arming source: `control.properties` with
   `enableActions=true` + `expectedPid` == current PID + `expectedBuild` == BUILD_NUMBER +
   `expectedClassSha` matching the computed script-class hash (64-hex check). Fail-closed on any
   exception (`catch ... { return false; }`). A stale or foreign control file cannot arm the
   wrong build or the wrong process. This matches the stream's ALEX/LIVE ACTIVITY "Adding
   armed-state check" — mechanism looks sound.
4. Route-timeout relaxation: route quiesce 15s -> 90s of travel, route-stall deadline 45s -> 100s
   (no-move stall detection stays 15s). README confirms intent: Build 2 cut the bank walk at 15s
   and held at Falador's north gate; Build 4 walks farther before holding. Matches the observed
   `PREP_BANK failed x3` at WorldPoint(2964,3396) on the stream (Build 3 era) and the subsequent
   progression through the shop steps on Build 4.
5. No new blocking-walk hazards, no reintroduction of blocking `walkTo` on the tick thread;
   journal-before-act + proof-before-clear + quiesceForReload retained from Build 2.

## Live behavior (stream 17:41-17:45 EDT, read-only)

- Marker went `2 / confirmed` -> `3 / confirmed` -> `4 / confirmed` (two hot reloads in the window).
- Bot logged in and is actively executing prep: `Walk shop peksa` -> `Walk shop wayne` -> `verify shop open-peksa`; position-unchanged timer reset from ~17.5 min to 0s. QUEST STATUS `Not started` (fortress route not yet reached).
- No error dialogs, no login flaps, chat timer healthy. No genuine viewer questions (chat skim).

## INFO watches (not blockers)

- BKF4-1: commit message for b792d514 is a copy-paste artifact ("Below Ice Mountain Build118 food-only supervised guardian preflight" on BKF Build 4 files) — cosmetic only; file contents are correct.
- BKF4-2: bank arrival (PREP_BANK) and the fortress route itself remain unverified live; quest FINISHED proof and safe logout unobserved. The relaxed timeouts address the observed stall but the fortress leg is still ahead.
- BKF4-3: control.properties arming is now active client-side; `expectedBuild=4` + class sha `c9754943...` must match the control file Alex writes. Any future hot load changes the hash and must be re-armed.
- BKF4-4: Build 3 was observed live ("3 / confirmed") but was never published to the repo — no patch for it; version.txt 997->998 skips nothing since builds 1/3 were local/unstaged.

## Verdict: PASS (read-only)

Custody air-tight, diff minimal and purposeful, arming gate fail-closed and build+pid+sha bound, and the live behavior matches the intended fix (beyond the Falador gate hold, progressing through shop prep). Next eyes: bank arrival, fortress route, varp-130 transitions, FINISHED-gated logout.
