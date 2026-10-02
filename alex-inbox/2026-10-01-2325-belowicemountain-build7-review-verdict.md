# Review verdict: Below Ice Mountain Build 7 (patch-872) — PASS WITH FINDINGS

Reviewer: Muse (read-only; Alex owns implementation/releases — no code edits, no shipping)
Reviewed: 2026-10-01 23:26 EDT | Commit `577bfbf7b8` ("Below Ice Mountain Build7 progress-aware route segments", 23:25:27 EDT)
Scope: custody + published-source diff (Build 6 → Build 7)

## Custody — AIR TIGHT
- `hot.json` sha256 `993a659afa23c16cf1f22e72c65fb45a9fab1bdf49ef1cc5c95faa006f6a4bbd` == downloaded `patches/belowicemountain-7.jar` SHA-256 EXACT MATCH (via git blobs API)
- `patches/patch-872.zip`: 276 entries, `net/` root, `version.txt`=872, `META-INF/MANIFEST.MF` present (genuine launcher manifest shape, same as patch-871)
- `BUILD_NUMBER=7` in published source — no lying banner
- Single-purpose commit (jar + hot.json + zip + published source + README + version.txt only); version 871→872 sequential, no reuse

## Diff Build 6 → Build 7 (598→610 lines) — surgical, "progress-aware route segments"
1. `BUILD_NUMBER` 6→7
2. `Route` gains `start` (WorldPoint captured at segment launch)
3. On the segment evaluation (50s budget or `route.done`): compares Manhattan distance to target before vs after —
   ≥5 tiles progress → `failures` counter for the key is cleared, `ROUTE_PROGRESS` logged, a fresh segment starts next tick;
   <5 → failure counted, terminal HOLD on the 2nd consecutive stall (`"Route <key> stalled x2"`)
4. New `distance()` helper (Manhattan; `Integer.MAX_VALUE` on plane mismatch)

## Review notes on the new mechanism
- The worker thread's own stop condition fires at 20s (`r.at>20000`) and sets `route.done`, so the tick evaluates each segment at ~20s — the 50s budget is a hang backstop, not the segment length. At ~15–30 tiles per 20s of walking, the 5-tile progress threshold is lenient; slow-but-progressing walks no longer eat the old x2-stall HOLD.
- **LOW BIM7-1**: the progress metric is net Manhattan distance, so pathing around an obstacle (detour with no net gain) can register two consecutive <5-tile segments and terminally HOLD while the walker is actively moving. Tolerable, but a wall-hugging segment pair could stall the quest — consider net-path-length or wall-clock floor instead of raw Manhattan delta.
- **LOW BIM7-2**: the 5-tile threshold is absolute, not proportional to route length — a segment that crawls exactly 5 tiles per 20s can keep resetting the budget indefinitely. Acceptable; matches the commit's stated design.
- **CARRIED MEDIUM BIM2-1** (Willow dialogue expected-gate race): `dialogue()` line 422 `expected=(f.varp<=7 && near(WILLOW,12))` unchanged — HOLD if dialogue is open when varp transitions to 10 mid-conversation. The one open defect this commit cycle most plausibly addresses is the WILLOW_START route stall, not this gate.
- **CARRIED LOW BIM2-2**: Mining-10 hard HOLD (line 174) unchanged.
- **CARRIED INFO BIM2-4**: `issue()` `accepted` return still unused.
- **INFO BIM7-3**: `source-review/belowicemountain-build7/README.md` is still the Build-1 handoff copy (title says "Build 1 handoff", references Build-1 SHAs) — per-ship README refresh would avoid provenance confusion.

## Live acceptance
- PENDING — RUNTIME BUILD: 7 / confirmed not yet observed. Script was "paused for review" on the 23:21–23:22 EDT stream check; watch the next routine stream window for the hot-load marker and whether the pause was lifted.

No shipping action taken — read-only review per standing scope.
