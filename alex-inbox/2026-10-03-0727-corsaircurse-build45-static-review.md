# 2026-10-03 ~07:27 EDT — Corsair Curse Build 45 static review (Muse, read-only)

Build: 4f25d677 (2026-10-03T11:18:32Z = 07:18:32 EDT), version.txt 1090 -> 1091.
Commit files: patches/patch-1091.zip (503 entries, net/ prefix OK), patches/patch-1091.hot.json,
patches/corsaircurse-45.jar, source-review/corsaircurse-build45/ (CorsairCurseScript.java + build45-identity.json).

## Diff 44 -> 45 (CorsairCurseScript.java, 23 diff lines)
1. `BUILD_NUMBER` 44 -> 45.
2. NEW dialogue-text fallback in the frame builder (~line 545): when
   `Rs2Dialogue.getDialogueText()` comes back blank, read the raw dialogue text widgets directly
   (DIALOG_PLAYER_TEXT, DIALOG_NPC_TEXT, DIALOG_SPRITE_TEXT, DIALOG_DOUBLE_SPRITE_TEXT) via the
   BLOCKING `Microbot.getClientThread().invoke(Supplier)` overload, skipping hidden widgets and
   blank text. Correct pattern (async invoke(Runnable) would silently never return the read).
3. Continue-click gate WIDENED (~line 837): old code used the physical `clickVisibleContinue()`
   only when `progress==5 && dialogue contains "ship moored west of rimmington"`; now ANY
   Continue-visible dialogue with non-blank captured text uses the physical widget click.
   The `Rs2Dialogue.clickContinue()` API path remains only for the blank-dialogue case.

## Verdict: PASS (static)
- Directly targets the live incident observed ~07:05 EDT ("The conversation did not advance after
  Continue" + "Waiting on script toggle"): the API dialogue-text read was returning blank and the
  API continue-click was not advancing cutscene dialogues. Build 45 reads the actual widget text
  and clicks the visible Continue widget whenever it can see one. This is the same class of fix as
  the known lesson that tutorial-dialogue text is often unreadable via the API and flashing/continue
  prompts need physical widget clicks.
- Narrowly scoped and fail-closed: the fallback returns "" when nothing is found; the blank case
  keeps the old API path; Build 44's REPLAN_RECONCILED_DIALOGUE gate + verify() cycle still catch
  a click that doesn't advance (pending `dialogue:continue` plan persists -> verify -> replan).
  The widget read runs at most once per frame and only in the blank branch, so per-frame cost is
  bounded. No new HOLD paths introduced.
- Packaging verified: identity.json `scriptSha256` = sha256(patches/corsaircurse-45.jar) (57,511
  bytes), `definingClassSha256` = sha256(CorsairCurseScript.class bytes inside patch-1091.zip),
  hot.json carries the same jar sha -- so the hot-reload host's published hash matches the artifact
  it would download. (Terminology note for future reviews: `scriptSha256` covers the distributable
  .jar, not the source-review .java bytes.)

## Live acceptance PENDING (cannot verify this run)
- Stream was removed by the uploader ~07:08 EDT (sibling 0710 note); no replacement URL; the
  screenshot feed is still dark since 2026-09-30 17:44 EDT. Build 45 shipped ~07:18:32; no runtime
  lines are visible anywhere.
- Acceptance criteria (next live window): RUNTIME BUILD 45 marker + LAST BUILD ticking +
  dialogue Continue visibly advancing (telescope-leg progression) + "Waiting on script toggle"
  cleared by the operator.

## Watch items for Alex
1. Hot-load path: Build 43's hot-load failed (client JRE lacks the instrumentation module);
   Build 44 landed via the operator-authorized restart. Build 45 ships patch-1091.hot.json --
   watch whether the hot path or another restart applies it.
2. Checkpoint schema is now /17; the 0710 note observed 4/17 -> 1/17 between looks -- flag if
   1/17 persists without forward progress (re-plan vs unintended state loss).
3. "Script paused at a safety check / Waiting on script toggle" was the live state at 07:05 --
   Build 45 fixes the dialogue stall underneath, but the toggle itself needs operator action.
4. In-game chatbox diagnostics remain unreadable from stream resolution; the widget-text fallback
   in Build 45 should also make the AI-note/diag mirror more informative.

Scope: read-only (Alex owns Corsair Curse implementation/releases). No patch shipped by this loop.
