# Doric Build 21 (patch-660) -- review verdict: PASS (1 minor, 1 informational)

- **Reviewer:** Muse review-loop (read-only; Alex owns implementation/releases)
- **Reviewed:** patch-660.zip (Build 21, commit dabcf895 10:13:56Z "Doric Build21: throttle heavy collision-map overlay while quest runs"), vs patch-659.zip (Build 20). version.txt=660 at review time.
- **Method:** blobs API (Accept: application/vnd.github.v3.raw) download of patch-660.zip (817,850 bytes) + patch-659.zip (817,090 bytes), unzip -l (215 entries: 212 net/-rooted, META-INF/MANIFEST.MF, version.txt; version.txt=660 in+out), javap -p -constants/-c diff of DoricsQuestScript 659->660. Chain-of-custody: patch-660.hot.json sha256 32843d1c... == patches/doricsquest-21.jar (25,409 bytes) == DoricsQuestScript.class inside patch-660.zip (26917a3b...). BUILD_NUMBER=21 in 660, 20 in 659. NO stale-class reship (unlike the 658 incident).

## Delta: Build 20 -> Build 21 -- shortest-path collision-map overlay throttled while quest runs

Motivation matches the commit message: the shortest-path plugin's collision-map overlay (`shortestpath.drawCollisionMap`) is a heavy per-frame render; disabling it while the quest bot runs reduces render load.

- **New constants:** SHORTEST_PATH_GROUP="shortestpath", COLLISION_MAP_SETTING="drawCollisionMap" (verified by name against the shortest-path plugin's config; `drawCollisionMap` is its real overlay toggle).
- **`disableHeavyCollisionMapOverlay()` (called once in `run()`, after guardLegacyHostReload):** one-shot (collisionMapSettingCaptured guard); captures current config via `RuneLite.getInjector().getInstance(ConfigManager.class).getConfiguration("shortestpath","drawCollisionMap")`; if it parses true, sets it to "false" and raises collisionMapTemporarilyDisabled; logs `[DoricsQuest] COLLISION_MAP_OVERLAY_TEMP_DISABLED previous={}`. Whole body try/caught with a warn -- a config-manager failure can never break script startup.
- **`restoreHeavyCollisionMapOverlay()` (called in `shutdown()`):** no-ops unless temporarilyDisabled; re-reads current value; restores the captured original (or unsets if it was null) ONLY if it still reads "false" -- it never clobbers a value the user changed mid-run. Logs `COLLISION_MAP_OVERLAY_RESTORED value={}`. try/caught.
- Delta is purely additive: two new methods, three new fields, two new constants; method-list diff 659->660 shows only additions, zero removals. No game-API calls (ConfigManager access via the RuneLite injector, standard). Markers honest: `RUNNING_BUILD` logs build 21.

## Findings

- **[m] Disable is wired to `run()`, not to the hot-reload path.** If patch-660 hot-loads onto a running client (the normal path), `run()` does not re-execute, so the overlay stays on until the script is stopped/started or the client restarts. Not a blocker -- the change is inert without `run()`, and the acceptance line `COLLISION_MAP_OVERLAY_TEMP_DISABLED` is unambiguous about when it fired -- but if Alex intended the throttle to take effect on hot-load, the call needs to ride `guardLegacyHostReload` (or the hot-reload entry) instead of only `run()`.
- **[i] Packaging carry-forward:** patch-660.zip carries META-INF/MANIFEST.MF with `Main-Class: net.runelite.client.RuneLite` (also present in patch-659, so not new). Harmless on the hot-reload path (classes are swapped into the running game, no jar injection); noting only because the old tutorial-island rule was "build zips with zip, never jar".

## Verdict: PASS

Small, additive, exception-guarded, idempotent, and honest markers. Live verification pending (screenshot feed dark since 2026-09-30 17:44:02 EDT -- no DORIC_* frames ever): acceptance lines are `COLLISION_MAP_OVERLAY_TEMP_DISABLED previous={}` at startup and `COLLISION_MAP_OVERLAY_RESTORED value={}` at shutdown. Per the standing rule, Alex's direct in-chat runtime reports supersede cron conclusions.

Nothing shipped (review-only; Alex's releases).
