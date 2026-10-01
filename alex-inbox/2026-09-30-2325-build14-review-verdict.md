# Review verdict: Ernest the Chicken Build 14 (patch-616) — PASS

Reviewed 2026-09-30 ~23:25 EDT by Muse (read-only review loop).
Repo commit: 35aa2650 "Build14: click detected Continue widget and verify dialogue transition"
(version.txt 615 -> 616 at 23:23:46 EDT)

## Packaging (all PASS)
- `patches/patch-616.hot.json`: build=14, sha256 `5247c74d8f1ba386e7d377d0bdc6bfa9505d80fa7fdad868a591203fd7a1a69a`
  — exact-matches `patches/ernestthechicken-14.jar` (28373B) byte-for-byte.
- `patches/patch-616.zip`: 208 entries, all under `net/` root (+ `META-INF/`),
  `version.txt` at root = 616, RuneLite `Main-Class` manifest intact. No bad-zip
  class (the 2026-09-29 341/342 failure mode is absent).
- 6 Script classes (`ErnestTheChickenScript` + DoorCandidate/Frame/LoginFrame/
  Pending/SkillLevelReview inner classes) byte-identical zip<->jar.
  Plugin/Config/$1 zip-only = expected hot-reload artifact split (unchanged).
- `BUILD_NUMBER` = 14 constant; `RUNNING_BUILD` startup banner `bipush 14`
  and `runtimeBuild()` both 14.

## Feature drift vs Build 13 (patch-615) — exact match to commit message
- Only `ErnestTheChickenScript.class` changed (+90B, 49104->49194B). All 5 inner
  classes are bytecode-identical to Build 13 (javap -c diff = 0). No method
  signature changes anywhere; no new constant-pool external references.
- New code is in `dialogue(Frame)`:
  - When `frame.visibleContinuePrompt` is true (a visible "Click here to continue"
    widget was detected this tick), the bot now calls
    `Rs2Widget.clickWidget("Click here to continue")` on the DETECTED widget.
  - If that click returns false (rejected), it goes to `hold(...)` with the new
    evidence string "Visible Continue widget found but click was rejected"
    (the only new string in the class) and returns — hold-with-evidence, no spin.
  - On click success (or when only the plain `continuePrompt` flag is set), it
    takes the existing `Rs2Dialogue.clickContinue()` path, then registers
    `set("CONTINUE", frame, 9000L, 0, null)` — a 9-second pending expectation
    so the next tick VERIFIES the dialogue actually transitioned. This is the
    "verify dialogue transition" half of the commit message.
- `Rs2Widget.clickWidget(String)` / `Rs2Dialogue.clickContinue()` were already
  referenced in Build 13 — no new external API surface, only a new branch
  combining existing verified APIs.

## Defect hunt
- None found. Non-blocking nits carried forward from prior builds (memory-only
  recenter/exit flags, conservative clickbox x<=1/y<=1 heuristic, null-pos
  DoorCandidate filter) — none are Build 14 regressions.
- The rejected-click hold path is the Julien-approved failure posture
  (evidence-logged hold, not a click loop).

## Verdict: PASS — no defects to report. Ready for hot-load.

## Live acceptance (pending, not blocking)
Feed dark since 17:44:02 EDT (~340 min, zero ERNEST_* frames ever), so Build 14
cannot be observed in-game yet. Acceptance triggers: fresh
`[ErnestChicken] RUNNING_BUILD=14` banner, `CONTINUE` set/HOLD lines from the new
`dialogue()` branch ("Visible Continue widget found but click was rejected" =
negative path), or the first Ernest screenshot.
