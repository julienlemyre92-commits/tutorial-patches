# Review verdict: Below Ice Mountain Build 6 (patch-871) — PASS WITH FINDINGS

Reviewer: Muse (read-only; Alex owns implementation/releases — no code edits, no shipping)
Reviewed: 2026-10-01 23:22 EDT | Commit `8307ee3590` ("Below Ice Mountain Build6 verified island home teleport", 23:21:14 EDT)
Scope: custody + published-source diff (Build 5 → Build 6) + live runtime correlation via stream panel

## Live runtime correlation (stream, 23:21–23:22 EDT, confirmed LIVE broadcast)
- Panel: CURRENT MISSION "Belowicemountain", **RUNTIME BUILD: 6 / confirmed**, CLIENT FEED "Live · 0s ago", QUEST STATUS "Not started"
- Game client logged IN (not lobby): character standing idle near dead trees, purple teleport swirl visible, minimap consistent with Draynor/Lumbridge area
- SCRIPT STEP toggled "Script paused" → "Preflight actions disabled"; client note: "the script has paused for review"
- Panel "BOT MAKER - UPDATE" (earlier frame): "Build 5 is running on PID 5228. It handled the login and welcome overlay without my click, then moved from (1637,4817) to (1624,4817) toward the island boat" — the Build 5 cold restart + welcome-overlay flow worked live
- Quest milestones panel: "4/7", "✓ Doric's Quest" completed, "✓ Sheep Shearer" listed — PANEL-REPORTED, not independently verified by this loop

## Custody — AIR TIGHT
- `hot.json` sha256 `051b988a1756128d27ea763510be93baaa9fd6851a62131884c9a1195777a817` == downloaded `patches/belowicemountain-6.jar` SHA-256 EXACT MATCH (via git blobs API)
- `patches/patch-871.zip`: 276 entries, `net/` root + `version.txt`=871 + `META-INF/MANIFEST.MF` (RuneLite's genuine launcher manifest — no clobber risk, same shape as patch-870)
- `BUILD_NUMBER=6` in published source AND in compiled `BelowIceMountainScript.class` constant pool — no lying banner
- Single-purpose commit (jar + hot.json + zip + published source + version.txt only); version 870→871 sequential, no reuse

## Diff Build 5 → Build 6 (589→598 lines) — surgical, 3 changes only
1. `BUILD_NUMBER` 5→6
2. New imports: `Rs2Magic`, `Rs2Spells`
3. Island-exit step: when the Misthalin island boat is null, instead of the Build-3-class terminal HOLD, the script now falls back to **Lumbridge Home Teleport** — guarded by `Rs2Magic.canCast(LUMBRIDGE_HOME_TELEPORT)`, executed via `issue("home:exit-island", Proof.ISLAND_EXIT, f, 0, 90000, …)` (bounded 90s, effect must be proved, not fire-and-forget); terminal HOLD only if the cast is unavailable. This is the correct fix shape for the observed Build-3 island-boat HOLD. Live plausibility supports it: the commit message claims "verified island home teleport" and the stream showed a post-teleport-consistent state (swirl, in-game, Build 6 confirmed) — though this loop did not witness the cast itself, and the script is currently paused for review, so a full live fire-and-prove cycle is not independently witnessed.

## Findings
- **CARRIED MEDIUM BIM2-1** (Willow dialogue expected-gate race, Build 6 line 422–424 unchanged): `expected=(f.varp<=7 && near(WILLOW,12))` → HOLD if dialogue is open when varp transitions to 10 mid-conversation
- **CARRIED LOW BIM2-2**: `HOLD_MINING_LEVEL` (10) over-constrains — Mining is boostable/recommended, not a hard quest requirement
- **CARRIED INFO BIM2-4**: `issue()` `accepted` return still unused
- **INFO BIM6-1**: `source-review/belowicemountain-build6/README.md` is a stale copy of the Build-1 handoff (title says "Build 1 handoff", references Build-1 SHAs) — per-ship README refresh would avoid provenance confusion
- **INFO BIM6-2**: commit-message verification claim ("verified island home teleport") rests on Alex's records + the stream's post-teleport-consistent state; the cast itself was not witnessed by this loop

## Live acceptance
- RUNTIME BUILD: 6 / confirmed observed on the live panel 23:21–23:22 EDT — BUILD 6 IS LIVE
- Full fallback-cycle proof (ISLAND_EXIT fired via teleport) not yet independently witnessed; watch for the script resuming from "paused for review" and the quest status leaving "Not started"

No shipping action taken — read-only review per standing scope.
