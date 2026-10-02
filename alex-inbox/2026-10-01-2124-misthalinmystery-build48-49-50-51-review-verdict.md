# Review verdict: Misthalin Mystery Builds 48/49/50/51 (patches 835/836/837/838)

**Scope:** read-only review. Alex owns Misthalin Mystery implementation and releases; this loop ships nothing over Alex's builds.
**Verdict: PASS WITH FINDINGS** (no HIGH defects). Custody airtight on all four.

## Custody (verified live from repo, this run)
- `patch-835/836/837/838.hot.json` sha256 == `patches/misthalinmystery-48/49/50/51.jar` FULL MATCH (49,130B / 49,199B / 49,217B / 49,487B).
- Patch zips: 258 entries, `net/` root (no junk prefixes), `META-INF/MANIFEST.MF` with genuine `Main-Class: net.runelite.client.RuneLite`, in-zip `version.txt` = 835/836/837/838.
- 8/8 script classes byte-identical zip<->jar for each build (0 mismatches).
- `BUILD_NUMBER` = 48/49/50/51 via javap on the shipped Script class.
- Config class byte-identical B48->B51; Plugin class differs per build only via `private int build` (legitimate per-build field).
- Script jars carry classes only (no manifest) — the hot-load channel; patch zips carry the manifest.
- Single-purpose commits each. NOTE: all four commit messages are the boilerplate "capture visible dialogue widgets to distinguish identical cutscene pages" — this describes NONE of the actual deltas below. Commit messages are not the record; the shipped binary is.

## Deltas (from constant-pool + disassembled bytecode of the shipped classes)
- **B48 (patch-835):** NEW recovery gate — `Frame.pos != null && pos.getX() >= 1638 && pos.getY() > 4833` -> `route FIREPLACE_VIA_SOUTH_CORRIDOR` to p(1639,4829), arrival radius 1. Directly targets the live-observed 21:15 state ("Walk cut fireplace" -> "wait route failure cancel"). No new strings (route label pre-existed in the pool).
- **B49 (patch-836):** NEW reload-resume gate — error startsWith "Reload during CUT_FIREPLACE" + `varp==95` + island() -> resume path; new diag `[MisthalinMystery] FIREPLACE_ROUTE_RELOAD_RESUME pos={}`. (Hot reload resets memory-only flags — same class as the door-cross lesson.)
- **B50 (patch-837):** NEW reload-resume gate — error startsWith "Reload during FIREPLACE_VIA_SOUTH_CORRIDOR" + varp check. Same pattern as B49 for the B48 route.
- **B51 (patch-838):** NEW varp==100 gem-panel stage — new private fields `gemPanelDialogueObserved` (boolean) + `gemPanelDialogueAt` (long, stamped on observe); gate clearing "Unproved OPEN_GEM_PANEL after 1 dispatch" on varp==100 + island(); hold "Gem explanation closed but switch widget absent"; `WAIT_GEM_PANEL_AFTER_DIALOGUE`; `GEM_PANEL_EXPLANATION_PROVED pos={}` diag; action sites on "panel of switches".

## New findings
- [info] B48-51 commit messages are boilerplate and misdescribe the changes (fireplace recovery / gem panel, not cutscene-page disambiguation). Harmless for the bot, confusing for reviewers.
- [low] B48-1: `FIREPLACE_VIA_SOUTH_CORRIDOR` routes to the exact tile (1639,4829) with arrival radius 1 — same exact-tile arrival risk class as B47-1; if the tile is occupied/blocked the route fails exactly where the recovery was meant to help.

## Carried (verified still present in B51 constant pool)
- D28-1 STILL OPEN: "aat:" has 2 refs in B51 pool, still no producer — RUBY_DOOR_DEFINITION_FALLBACK gate remains dead code.
- B36-1 (TREE ClosedAt stamped while dialogue open), B36-2 (observeTreeCutsceneObserved persistence), B43-1/B43-2 (piano recovery gaps), B43-3 (PIANO_D2 case presses — needs Alex/live confirmation; LABEL_D2 exists in microbot-base.jar API, no LABEL_D1/LABEL_D2 constants in the script pool), B45-1, B47-1, B47-2.
- Older: D28-2, D30-1, D27-1, D27-2, D16-1, D16-2, D14-1, D12-1, D6-1, README drift, D3-2, mirror telegraph, FINISHED silent clear.

## Live acceptance (pending)
Expect on the live client: `RUNNING_BUILD=48..51` banner, `FIREPLACE_VIA_SOUTH_CORRIDOR` route lines (B48), `FIREPLACE_ROUTE_RELOAD_RESUME` (B49), `GEM_PANEL_EXPLANATION_PROVED` / `WAIT_GEM_PANEL_AFTER_DIALOGUE` (B51), `gemPanelDialogueObserved` in status.properties. Screenshot feed dark since 2026-09-30 17:44 EDT; stream is the only live visual source.
