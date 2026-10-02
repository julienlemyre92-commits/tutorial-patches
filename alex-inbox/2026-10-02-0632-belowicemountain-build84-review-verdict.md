# Below Ice Mountain Build 84 review verdict (read-only, Muse)

- Date: 2026-10-02 06:32 EDT (10:32Z observe)
- Build: 84 — commit d8a12705 "Below Ice Mountain Build84 entrance warning widget" (10:25:27Z)
- Patch: patch-945.zip / belowicemountain-84.jar / patch-945.hot.json; version.txt=945 at ship, 946 at observe (B85 patch-946 landed 10:27:56Z — NOT reviewed here)

## Verdict: PASS WITH FINDINGS

## Custody chain (verified)
- patch-945.zip: 294 entries, net/-rooted (same pattern as 936+), plus META-INF/ + MANIFEST.MF + version.txt
- in-zip version.txt = 945
- belowicemountain-84.jar sha256 720c678d466e898d9e5ecaf5a814dfeaf8cba941760b9cb43e4147c61a1c8df7 == patch-945.hot.json sha256 — FULL MATCH
- BUILD_NUMBER = 84 javap-verified in shipped jar
- Source-review script byte-compiles on JDK 17.0.20.1 (microbot-base.jar + lombok.jar + shipped jar on classpath) with ONLY the 2 pre-existing BelowIceMountainConfig symbol errors (lines 428/727 — carried B71→B83, config class lives outside the shipped jar, not a regression)
- README byte-identical to B83 (sha 4a0dd2e74d9b…, 37103 bytes) — no B84 section

## Delta B83 → B84 (+5/−2 = 13 changed lines, "entrance warning widget")
1. BUILD_NUMBER 83 → 84.
2. In dialogue()'s hasContinue branch: when `stage35EntranceWarning` is true, the continue-widget scan now tries {{229,2},{229,0},{193,3},{193,0},{11,4}} INSTEAD OF the standard {{217,5},{231,5}}.
3. Miss path unchanged: control==null → WAIT_VISIBLE_CONTINUE_WIDGET (fail-closed, no HOLD trap).

## Findings
- NEW INFO BIM84-1: the new pair list REPLACES the standard two pairs under stage35EntranceWarning (ternary, not appended). If the warning dialogue actually renders a standard 217,5/231,5 continue, B84 won't find it and parks in WAIT_VISIBLE_CONTINUE_WIDGET until the dialogue times out. Alex is betting the warning renders on a different widget.
- NEW INFO BIM84-2: B85 ("native entrance warning continue", landed 10:27:56Z, unreviewed) suggests Alex is already iterating this exact mechanism again — watch whether B85's approach contradicts B84's pair list.
- Carried: BIM83-1/83-2, BIM82-1/82-2, BIM81-1/81-2, BIM78-1, BIM79-1, BIM75-1, BIM77-1, BIM71-1, BIM71-3..6, BIM61-1, BIM57-1, BIM68-1, BIM70-1/70-2, BIM72-1, BIM53-1.

## Live acceptance
None — screenshot feed dark since 2026-09-30 17:44 EDT; B57–B84 all pending live confirmation. Watch-keys for B84: RUNTIME BUILD 84 + dialogue:continue-widget lines on stage-35 entrance-warning dialogues.
