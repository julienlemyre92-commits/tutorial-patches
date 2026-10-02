# Review verdict: Below Ice Mountain Build 12 (patch-877) — READ-ONLY

Reviewed by: Muse (read-only; Alex owns implementation/releases; no ship)
Commit: 57bee1a905 ("Below Ice Mountain Build12 Flex action correction", landed 2026-10-01 23:39:59 EDT — mid-run)
Verdict: **PASS WITH FINDINGS**

## Custody — AIR TIGHT
- hot.json `patch-877.hot.json` sha256 `29af5a0ca74e55e680a34f24fc44fa5a351a5c9cebd016adf3b289fd0ff9aefe` == `patches/belowicemountain-12.jar` bytes — FULL MATCH.
- `patch-877.zip`: 276 entries, net-rooted (only extras META-INF/ + version.txt, same as prior builds; zip-built, no manifest overwrite issue), in-zip `version.txt` = `877` == repo `version.txt` (877 at review time) == patch number.
- `BUILD_NUMBER=12` in published source (line 58) and confirmed in the compiled class via `javap -constants` — no lying banner.
- Single-purpose commit; version 876→877 sequential, no reuse, no overwrite. Commit touches only jar + hot.json + zip + source-review + version.txt.

## Diff vs Build 11 (published source, 616→620 lines) — surgical
1. `BUILD_NUMBER` 11→12.
2. The `emote:flex` action: before issuing, logs full widget diagnostics (`id, index, parent, bounds, actions, sprite`). The dispatch lambda changes from `Rs2Widget.clickWidget(flex)` (accepted-returning mouse click) to `Rs2Widget.clickWidgetFast(flex, flex.getIndex(), 1)` — the installed CC_OP path with explicit widget index and menu index 1 — once per attempt. Action name, `Proof.DIALOGUE_CHANGED`, and 12000ms proof budget unchanged.
Nothing else changed.

## Design rationale (per source-review README §Build 12)
Live Build 11 opened the emotes tab and located sprite 2426, but two `Rs2Widget.clickWidget(flex)` clicks returned accepted with no Checkal varbit/dialogue change, so it held. Installed `Rs2Widget` bytecode shows that overload only clicks the widget rectangle and returns true regardless of menu execution. Build 12 logs the widget state, then invokes `clickWidgetFast(widget, index, 1)` once per attempt with later proof. Two unproved actions still HOLD. No movement/combat logic added; guardian remains disabled.

## Findings
- INFO BIM12-1: the clickWidgetFast switch is a genuine correction — the generic click overload's accepted-true return was masking a no-op menu execution, which is exactly the class of false-progress the proof-gate design exists to catch. The pre-dispatch diagnostics log gives a live tell: the widget `actions` array will show whether menu index 1 is actually "Flex" when the line appears. Bounded risk: dispatch lambda still returns true unconditionally, so the real guard remains the DIALOGUE_CHANGED proof within 12s + two-failure HOLD (unchanged).
- Carried: INFO BIM9-1 (low-byte decode assumption), INFO BIM11-1 (region gate), LOW BIM7-1 / LOW BIM7-2 (route progress), LOW BIM2-2 (Mining-10 gate), INFO BIM2-4.

## Live acceptance
PENDING — a read-only stream check was spawned this run (routine window) to watch for the RUNTIME BUILD 12 hot-load line, whether the script leaves "paused for review"/preflight-disabled, and the Flex step outcome. Result pending at verdict time.

No shipping action taken — read-only.
