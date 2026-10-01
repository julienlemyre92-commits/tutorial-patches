# Prince Ali Rescue Build 94 — read-only review (Muse, Alex owns releases)

- Date: 2026-10-01 ~19:40 EDT (commit b797ea7c, 23:28:50Z; version.txt=787)
- Patch: patches/patch-787.zip + patches/patch-787.hot.json + patches/princealirescue-94.jar + patches/princealirescue-plugin-94.jar + source-review/princealirescue-build94/ (4 files) + version.txt
- Scope: READ-ONLY review. Nothing shipped from this side.

## Custody: CLEAN (byte-level, independently verified via git blobs API)

- hot.json `{"plugin":"princealirescue","patch":787,"hostVersion":1,"build":94,"sha256":"6db7014e6fecaf652d40e5ec1d12d8e4a9f5faa0709f5c7b375c2e6e37cfe8b3"}` — sha256 == princealirescue-94.jar bytes (147,912 B). MATCH.
- patch-787.zip: 247 entries, root `net/` (+ benign META-INF/, version.txt); 232 classes; in-zip version.txt = `787`.
- All 29 script-jar classes byte-identical zip<->jar; all 32 plugin-jar classes byte-identical zip<->plugin-jar. Zero mismatches.
- BUILD_NUMBER = 94 confirmed in the compiled class (javap -constants).
- Plugin.java / Config.java / README.md byte-identical B93->B94. Single-purpose commit (9 files).
- New compiled artifact: `PrinceAliRescueScript$QuestPluginHandoff` (10,151 B) — matches the source diff.

## Delta B93 -> B94 (script only, +108 lines / +7,205 chars)

1. `BUILD_NUMBER` 93 -> 94.
2. Two call sites: on `quest==QuestState.FINISHED` (both the post-safety-retreat branch and the main-loop branch) -> `phase="COMPLETE_QUEST_STATE"; status(f); QuestPluginHandoff.check(); return;`
3. New inner class `QuestPluginHandoff`: guarded same-process next-quest plugin installation, fired only after live completion proof (FINISHED).

Mechanism (from source):
- `check()` (client tick thread, bounded file I/O only): no-ops until `%USERPROFILE%/.runelite/quest-handoff/request.properties` exists. Reads sha256 (must be 64 hex) + build (>=1). Skips if result.properties already carries the same sha (any state). Verifies `%USERPROFILE%/.runelite/quest-handoff/<sha>.jar` sha256, then scans the jar: EVERY entry must be under `net/runelite/client/plugins/microbot/misthalinmystery/` and end `.class`. Writes QUEUED, then `SwingUtilities.invokeLater(() -> install(...))`.
- `install()` (EDT): refuses if MisthalinMysteryPlugin already registered; requires the Prince Ali plugin active. URLClassLoader (parent = Plugin's loader) -> loads MisthalinMysteryScript, asserts its BUILD_NUMBER == requested build -> `pm.loadPlugins(List.of(MisthalinMysteryPlugin))` (expects exactly 1) -> default config -> sets `misthalinmystery/enableActions=false` -> enable + startPlugin (must return true and be active) -> disables + stops the Prince Ali plugin (asserts inactive) -> atomically writes `%USERPROFILE%/.runelite/bot-mission.txt` = "misthalinmystery" -> result PREFLIGHT_STARTED ("Await fresh same-PID target runtime/status proof"). Full rollback on failure (remove candidate, close loader, re-enable old plugin) -> HELD / HELD_ROLLBACK with the exception text.
- API surface (PluginManager.loadPlugins/setPluginEnabled/startPlugin/stopPlugin/remove, Microbot.getPluginManager/getConfigManager, URLClassLoader, JarFile): standard RuneLite/Microbot calls; the build compiled against the real APIs. EDT choice for plugin lifecycle matches RuneLite UI conventions and keeps the install off the client tick.

## Findings

- [MEDIUM NEW] D94-1: result.properties dedup ignores state. `if (sha.equals(previous.getProperty("sha256"))) return;` skips on ANY prior state for the same sha. (a) Crash between QUEUED-write and install completing -> result stays QUEUED; on restart (quest still FINISHED) check() returns silently -> no install, no bot-mission.txt, account parks in COMPLETE_QUEST_STATE with the next quest never installed; manual deletion of result.properties required. (b) A HELD result (e.g. jar missing on first sight) likewise suppresses all future attempts for that sha across restarts, even after the artifact is fixed. Suggest: only skip on PREFLIGHT_STARTED (or an explicit terminal-success state); retry on QUEUED/HELD, bounded.
- [MEDIUM NEW] D94-2: `attempted=true` is set BEFORE validation. If request.properties lands before the jar (download race), the first check() latches attempted, throws on the missing jar -> HELD, and never retries in-process even after the jar arrives. Suggest: set attempted only after successful validation, or gate the first pass on the jar file existing too.
- [LOW/MEDIUM NEW] D94-3: jar-entry containment rejects META-INF/MANIFEST.MF (and any non-.class resource) with "Unexpected handoff entry". A conventionally built jar (jar/gradle always emits a manifest) will HELD the handoff. If manifest-less classes-only jars are the intent, document it — source-review README.md was NOT updated for B94 (byte-identical to B93); otherwise allow META-INF/*.
- [LOW NEW] D94-4 (question): install() sets `misthalinmystery/enableActions=false` before starting the plugin, and result PREFLIGHT_STARTED says "Await fresh same-PID target runtime/status proof". If the MM plugin's tick loop requires enableActions=true, it will sit installed-but-inert after a nominally successful handoff. Confirm the MM plugin re-arms itself (or that the park is the intended preflight state).
- [LOW NEW] D94-5: README.md not updated — the handoff protocol (request.properties schema, jar constraints, result states, manual-recovery steps for D94-1) is undocumented for the operator.
- [info] Threading: check() does only cheap file checks per tick until the request appears; install() on EDT is the right thread for PluginManager lifecycle and avoids blocking the client tick. The sha256+JarFile scan runs on the client thread but is bounded (~150 KB).
- [info] Security posture: sha256 pinning + strict package containment + BUILD_NUMBER match + same-process guards (target-not-registered, source-active) protect against corrupt/partial/wrong artifacts. Authorship is NOT verified — acceptable here since request.properties + jar are placed by the trusted local operator (Julien/Alex tooling), who could already run arbitrary code.
- Carried (still open through B94, untouched by this delta): D88-1 (Jail-approach 1-HP-deficit terminal HOLD at varp==30 near Draynor bank, no resume); stranded probe dumps under %USERPROFILE%/.runelite/princealirescue/; banked bronze pickaxe 1265 never withdrawn; B70 empty-getQuestion() furnace confirmation HOLD; nativeGeBuy COMPLETE bare-return (silent idle).
- Carried CLOSED: B77/B79 poison soft-lock (B90 dangerRequiresRetreat); B89 Policy IAE (fixed B90); version-divergence (resolved B93).

## Verdict: PASS WITH FINDINGS

Nothing FAIL-worthy. The handoff is well-guarded for the happy path (pinning, containment, build-marker match, rollback); the two MEDIUMs are crash/race consistency gaps that only bite if the handoff actually fires under adverse timing — worth fixing before Alex stages a live request.properties + jar.

## Live acceptance: PENDING

Feed dark since 2026-09-30 17:44 EDT (~26h); no live URL. No request.properties is known to exist, so B94 should show zero behavior change until Alex stages the handoff. If/when it fires, expect: `QUEUED` -> `PREFLIGHT_STARTED` in quest-handoff/result.properties, bot-mission.txt = "misthalinmystery", and the new plugin's own runtime lines — acceptance needs those, never the banner.
