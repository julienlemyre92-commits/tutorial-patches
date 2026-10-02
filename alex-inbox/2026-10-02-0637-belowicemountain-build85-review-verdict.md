# Below Ice Mountain Build 85 review verdict (read-only, Muse)

- Date: 2026-10-02 06:37 EDT (10:37Z observe)
- Build: 85 — commit 30cd3b74 "Below Ice Mountain Build85 native entrance warning continue" (10:27:56Z), linear on d8a12705
- Patch: patch-946.zip / belowicemountain-85.jar / patch-946.hot.json; version.txt=946 at ship and at observe (no race)

## Verdict: PASS WITH FINDINGS

## Custody chain (verified)
- patch-946.zip: 294 entries, 291 net/-rooted (same pattern as 936+), plus META-INF/ + MANIFEST.MF + version.txt
- in-zip version.txt = 946
- belowicemountain-85.jar sha256 205f5e4487749e6e7e6fbe070c83933600055619154bda49eee6b68433f3d8d8 == patch-946.hot.json sha256 — FULL MATCH (git-blob raw downloads)
- BUILD_NUMBER = 85 javap-verified in shipped jar
- Source-review script byte-compiles on JDK 17.0.20.1 (microbot-base.jar + lombok.jar on classpath) with ONLY the 2 pre-existing BelowIceMountainConfig symbol errors (lines 428/727 — carried B71→B84, config class lives outside the shipped jar, not a regression)
- README byte-identical to B84 (sha c8f8a769, 37103 bytes) — no B85 section

## Delta B84 → B85 (+5/−0 = 5 lines, "native entrance warning continue")
1. BUILD_NUMBER 84 → 85.
2. In dialogue()'s hasContinue branch, when `stage35EntranceWarning` is true, B85 now issues `dialogue:entrance-warning-continue` via `Rs2Dialogue.clickContinue()` with DIALOGUE_CHANGED proof (9000ms) and returns early — BEFORE the B84 continue-widget scan.
3. BIM84-2 resolved as a replacement: B85 supersedes B84's widget-pair mechanism for the warning case; B84's {{229,2},{229,0},{193,3},{193,0},{11,4}} list and its ternary inside the widget-scan lambda are now unreachable dead code under stage35EntranceWarning (harmless vestige, not a bug). When the warning flag is false, the standard {{217,5},{231,5}} scan is unchanged.

## Findings
- NEW INFO BIM85-1: the issue lambda `() -> { Rs2Dialogue.clickContinue(); return true; }` unconditionally reports "action issued"; acceptance rides on the DIALOGUE_CHANGED proof. If clickContinue() is a no-op on the warning's actual widget, the proof expiry (9000ms) re-issues / parks rather than trapping in HOLD — fail-closed, no new HOLD path.
- NEW INFO BIM85-2: B85 answers BIM84-1 by sidestepping the widget-guess entirely (native continue instead of guessed widget IDs). If the warning dialogue also requires the "yes."/stage35EnterYes option click (B83 mechanism), that path still precedes this one in the options branch — order preserved.
- Carried: BIM84-1 (mooted for the warning case by the native-continue replacement), BIM83-1/83-2, BIM82-1/82-2, BIM81-1/81-2, BIM78-1, BIM79-1, BIM75-1, BIM77-1, BIM71-1, BIM71-3..6, BIM61-1, BIM57-1, BIM68-1, BIM70-1/70-2, BIM72-1, BIM53-1.

## Live acceptance
None — screenshot feed dark since 2026-09-30 17:44 EDT; B57–B85 all pending live confirmation. Watch-keys for B85: RUNTIME BUILD 85 + dialogue:entrance-warning-continue lines on stage-35 entrance-warning dialogues (and absence of WAIT_VISIBLE_CONTINUE_WIDGET parks there).
