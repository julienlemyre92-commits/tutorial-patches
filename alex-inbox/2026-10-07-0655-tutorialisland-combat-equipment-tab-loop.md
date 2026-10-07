# Read-only review note: COMBAT stage equipment-tab no-progress loop (2026-10-07 06:55 EDT)

Muse read-only review. No code touched. Runtime Build 418 (patch-415), version.txt=1116.

## Observed state
- Stage COMBAT since ~06:50:02, re-derived after the 06:49:46 client restart (re-derivation predicate unobserved in the banner-only tails — open question below).
- Step `COMBAT TICK: OPEN_EQUIPMENT_TAB` looping for 16+ script-915 fallback attempts (~48+ miss ticks, ~2 min across two sessions). Runtime build 418 RE-VERIFIED via explicit `Build 418: STARTUP -- RUNNING_BUILD=418 (patch-415)` marker in the fresh 06:53:08 session.

## Evidence
`screenshots/2026-10-07_06-50-46_COMBAT_diag.txt` (06:50:06-06:50:46):
- `Build 124: EQUIPMENT check: [548:68 name='' bounds=null] [161:63 name='' bounds=null] [164:56 name='' bounds=null] [161:70 name='' bounds=null] [164:63 name='' bounds=null] | name-matched name='' bounds=null age=9223372036854775807ms`
- `clickTabIcon EQUIPMENT: NO VERIFIED TARGET (no packed ID resolved, name-matched stale/missing)` -> `no verified target this tick — counting miss`
- Every 3rd miss: `button missing 3x in a row — script-switch fallback (attempt 9..11)` -> `switchTabViaScript: ran client script 915 -> EQUIPMENT`
- The SAME session's Build-120 widget dump PROVES the bottom tab strip is visible: `164:60 bounds=[577,645 33x36]`, `164:61 [610,645 33x36]`, `164:62 [643,645 33x36]`, hidden=false — NONE of these are in the probe list.

`screenshots/2026-10-07_06-53-09_COMBAT_diag.txt` (fresh session after the 06:53:08 restart):
- `Build 125 SCAN: 164:53 [577,645 33x36]`, `164:54 [610,645 33x36]`, `164:55 [643,645 33x36]` — SAME coordinates as 164:60/61/62 last session, but DIFFERENT child IDs.
- Probe list unchanged -> still `NO VERIFIED TARGET` (attempts 15, 16); script-915 fallback 2x; step still `OPEN_EQUIPMENT_TAB` — the tutorial prompt is not clearing.

## Mechanism
1. `clickTabIcon(EQUIPMENT)` resolves its click target from a hard-coded packed-ID candidate list that no longer matches the live widget tree. Child indices SHIFT between sessions (60/61/62 -> 53/54/55 at identical coords), so any fixed list is fragile.
2. With no verified target, the FORCED physical click never fires (gating is correct — no blind clicks).
3. The miss-3x fallback runs client script 915 (tab switch). Standing rule (verified 2026-09-27): flashing tutorial icons REQUIRE a physical mouse click — script/hotkey tab switches don't register with the tutorial prompt. So the prompt never clears, nextStep recomputes OPEN_EQUIPMENT_TAB, and the loop is infinite.
4. The Build-125 live scan ALREADY finds the real icons with valid bounds, but its results are not fed into clickTabIcon's target resolution.

## Expected vs observed
- Expected: physical click on the visible equipment-tab icon -> flashing prompt clears -> combat lesson advances.
- Observed: ZERO physical clicks in ~2 min; repeated script-915 switches the tutorial ignores; step never completes.

## History
Same defect class as the 2026-09-27 PRAYER flashing-icon loop (banner: Builds 114/116/117 — "packed 548:69/161:64 null", "BOTTOM-LINE TAB ICONS (group 164)"). The tab-strip scan work (Builds 125/127) exists but is not wired into the click path for EQUIPMENT.

## Suggested fix (for Alex)
- Derive the EQUIPMENT tab click target from the live Build-125 scan (children with non-null bounds in the bottom strip, y~645, 33x36) instead of hard-coded packed IDs; or expand the candidate list and re-probe against live scan results each session.
- Investigate why the name-matched fallback returns bounds=null even while group-164 children are visibly listed.
- New verify diag: print the CHOSEN target's packed ID + bounds at click time (the Build-124 check line is close; add a chosen-target line).
- OPEN VERIFICATION QUESTION (not a defect claim): the MINING->COMBAT transition happened during the banner-only 06:49:46 session; no dagger/smith-completion lines are observable in ANY tail. Please confirm the COMBAT entry predicate (bronze dagger observed in inventory? varp value?) so a skipped smith arc cannot hide behind stage re-derivation (cf. the 2026-09-29 "Have bread" lesson).

## Carried watch items
- Restart loop continues: 6th client restart at 06:53:08 (~2-3 min cadence since 06:41:22). Cause still unknown.
- PNG uploader still dead: no `2026-10-07_*_auto.png` in the repo tree today — zero visual corroboration for 6+ days.
- Banner-dump blind spot: 06:53:08 session's upload is banner + scan lines; per-session gameplay only appears in later uploads.
