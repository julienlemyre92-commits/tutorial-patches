# Muse review-loop verdict: Ernest Build 33 (patch-635) -- PASS
Date: 2026-10-01 ~01:26 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)

- Ship: patch-635, commit 24e5034a 2026-10-01T05:23:12Z "Build33: diagnose live manor staircase objects". version.txt=635.
- Packaging clean: 208-entry net/-rooted zip (204 net/runelite entries + net/ + META-INF/ + MANIFEST.MF + version.txt), version.txt=635 in+out, BUILD_NUMBER=33 (static final int) + runtimeBuild()=33 (bipush 33), real RuneLite MANIFEST.MF. Zip built with `zip`, not `jar`.
- Delta vs Build 32 (only changed methods: `nearbyNamedStaircases` 65->85 lines, `runtimeBuild` bipush): diagnostic upgrade of the manor-staircase scanner.
  - Build 32: deprecated `Rs2GameObject.getTileObjects()`, radius 6 of STAIRS0, NO plane filter, name must contain "stair", diag string = `id tile name`.
  - Build 33: `Microbot.getClient()` + `Rs2GameObject.getAll()`, SAME-PLANE filter vs STAIRS0 plane, radius 7, per-candidate `Client.getObjectDefinition(id).getActions()` captured via `Arrays.toString`, diag string = `id tile name actions=[...]` for EVERY object within 7 (not just stair-named). RuntimeException-safe (try/catch -> class-name line in the list).
- Purity: pure read-only diagnostic. Zero `interact`/`click`/`walkTo`/`walkStep`/`changeWorld`/`putstatic`; writes only to the local ArrayList + return. Consumed by the existing "No named manor staircase near ..." HOLD text.
- Findings: none new. [L] Build 33 still illuminates rather than fixes -- the scanner is only read when the climb HOLD fires. [M carry-forward from Build 32] single-shot `oddensteinStairs0to1/1to2Attempts` budgets persist via status.properties untied to the pending lifecycle.
- Verdict: PASS -- read-only diag improvement, zero behavior change, packaging clean.
