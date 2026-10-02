# Review verdict: Misthalin Mystery Builds 28/29/30 (patches 815/816/817) — 2026-10-01 20:44 EDT

Scope: read-only review. Alex owns Misthalin Mystery implementation and releases.

## Verdict: PASS WITH FINDINGS (all three builds)

### Custody (all three airtight)
- hot.json 17c917f3… == misthalinmystery-28.jar (45,794 B) FULL MATCH; c96b76e0… == misthalinmystery-29.jar (45,978 B) FULL MATCH; 0ebc8523… == misthalinmystery-30.jar (46,109 B) FULL MATCH (sha256, downloaded bytes).
- patch-815/816/817.zip: 258 entries each, net-rooted (only META-INF/, META-INF/MANIFEST.MF, net/ dir, version.txt outside net/); in-zip version.txt = 815 / 816 / 817.
- 8/8 shipped classes byte-identical zip↔jar (intersection) for all three.
- BUILD_NUMBER = 28 / 29 / 30 (javap -constants on shipped classes).
- MisthalinMysteryConfig.class byte-identical B28→B29→B30; MisthalinMysteryPlugin.class byte-differ but strings-identical (pure recompile ripple); README.md 5,338 B identical ×3; single-purpose commits (4284cc16, 5e18712f, cb8fdf3e).
- Zip MANIFEST.MF carries genuine RuneLite Main-Class (injection-safe; zips built with zip).

### Delta B27→B28 (12 lines + banner)
- New RUBY_DOOR_DEFINITION_FALLBACK_ONCE recovery gate: when held with error starting "aat:" + varp==45 + exactly 1 ruby key + within 4 of RUBY_DOOR (1640,4828) + full HP + !inDialogue → clears hold (held=false, error="", pending=null), logs "RUBY_DOOR_DEFINITION_FALLBACK_ONCE pos=…".
- getImpostor() call wrapped in try/catch: on exception logs "OBJECT_IMPOSTOR_LOOKUP_FAILED id=…" and continues with impostor=null (fail-soft; downstream null-handles).

### Delta B28→B29 (14 lines, diagnostic-only)
- New `shelfProbe` Frame field: at varp==50, object tile within 3 of SHELVES, appends "id@tile name [actions]" (1200-char cap), persisted to status.properties. Zero stage-logic change. Fail-closed diagnostics.

### Delta B29→B30 (21 lines, behavioral at varp 50)
- New SHELF_LIVE_ACTION_PROVED gate clears "Object has none of expected actions SEARCH_TINDERBOX…" hold when varp==50 + no tinderbox + shelfProbe contains "30146@" and "Take-tinderbox" + full HP + within 7 of SHELVES. Matcher LIVE (emitted at :1119/:1139); the B29 probe paid off — the shelf's real action is "Take-tinderbox", not "Search".
- Both SEARCH_TINDERBOX action sites (:1393, :1501) now issue "Take-tinderbox" as primary with "Search" as fallback. Correct fail-soft ordering.

### Findings
- [LOW NEW] D28-1: RUBY_DOOR_DEFINITION_FALLBACK_ONCE is DEAD CODE in B28–B30 — the string "aat:" has no producer anywhere in the shipped source (no hold() emitter, no pending key starts with "aat:"; the only occurrence is the startsWith check at :655). Half-landed change: recovery gate shipped before its error emitter (or the emitter was dropped). Harmless — it cannot fire — but the "ONCE" name on an unfireable path is misleading.
- [info NEW] D28-2: gate lacks a persisted single-shot latch, but since it clears `error` itself the gate is single-fire by construction (no churn) IF it ever fired.
- [info NEW] D30-1: SHELF_LIVE_ACTION_PROVED is not single-shot latched (unlike PAINTING_*_ONCE gates) — hold/clear can re-occur, but each clear requires fresh observed proof, so fail-safe; churn only if shelf state flaps.
- Carried: D27-1, D27-2, D16-1, D16-2, D14-1, D12-1, D6-1, README drift, D3-2, mirror telegraph, FINISHED silent clear.

### Live acceptance pending
Expect RUNNING_BUILD=28/29/30, OBJECT_IMPOSTOR_LOOKUP_FAILED warn only on impostor throws, shelfProbe lines in status.properties, and on the varp-50 front either SEARCH_TINDERBOX progress or SHELF_LIVE_ACTION_PROVED lines. Screenshot feed dark since 2026-09-30 17:44 EDT (~27.0h); no live URL.
