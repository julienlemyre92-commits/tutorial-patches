# Corsair Curse Build 43 — static review (Muse, read-only) — 2026-10-03 06:53 EDT

Ship: Alex, Build 43 (commit 2a9aa381, 06:53:06 EDT), patch-1089, version.txt=1089.
Scope: read-only static review of source-review/corsaircurse-build43/CorsairCurseScript.java (42→43 diff, 2 hunks, ~10 lines).

## What changed
1. `BUILD_NUMBER` 42 → 43 (marker only).
2. Pending-proof handler (`dialogue:continue` keys), two gates:
   - NEW: if key starts with `dialogue:continue`, dialogue frame is BLANK, player is outside all quest zones (not inCove, not near FARM/DOCK r=20, not inCavern), and <60s elapsed since issue → stage = `WAIT_DIALOGUE_CUTSCENE_RESULT`, return (wait, do not fail the proof yet).
   - TIGHTENED: the "unchanged continue prompt; one verified widget fallback" path now requires `!f.dialogue.isBlank()` in addition to the previous conditions — the physical continue-widget fallback no longer fires on an empty dialogue frame.

## Mechanism read
- Targets proofs issued across sail/cutscene transitions (e.g. Rimmington→Corsair Cove leg, Colin conversation/cutscene) where the dialogue widget legitimately renders blank for a while. Previously the 12s deadline would fail the proof and drive retry cycles during the blank window.
- 60s is the effective wait: once past 60s the normal fail path re-applies (failures count, one widget fallback if dialogue now non-blank and unchanged). Bounded, no indefinite hold.
- The `isBlank()` guard on the fallback also closes the clicker-firing-on-empty-frame hole — consistent with the flashing-icon rule (clicks must land on real widgets).

## Verdict
- PASS (static). No concrete defects found.
- Minor notes: (a) zone exclusion list must grow if future cutscenes occur inside new zones — a `dialogue:continue` blank-frame inside the cove/farm/dock/cavern still fails fast at 12s; (b) 60s wait vs 12s base deadline is a deliberate asymmetry, fine as bounded.

## Live acceptance pending
- RUNTIME BUILD marker → 43 on stream; new runtime lines: `[CorsairCurse] ... stage=WAIT_DIALOGUE_CUTSCENE_RESULT` and/or the `!dialogue.isBlank()` gate line during the next cutscene transition (Colin conversation leg).
- No ship from Muse (read-only scope; Alex owns Corsair Curse implementation/releases).
