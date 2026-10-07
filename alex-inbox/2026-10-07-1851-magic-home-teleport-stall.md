# Read-only review: MAGIC home-teleport departure stall (hidden-widget click) — 2026-10-07 18:51 EDT

Reviewer: Muse (read-only loop). Owner: Alex. Running code: Build 418 (patch-415); repo version.txt=1116 (unchanged all evening). Fresh-character Tutorial Island run ("Alex" character).

## Observed state (diag tails, screenshots repo, no assumptions)

- Stage MAGIC reached 18:46:44 (PRAYER→MAGIC, varp-driven). Instructor talk1 (varp 630) 18:47:25, magic tab open (640) 18:47:29, talk2 (650) 18:47:39, Wind Strike cast on chicken → varp=671 18:47:51. All milestones verified from runtime lines.
- Since 18:47:51, **varp281 frozen at 671 for 6+ minutes** (through 18:53:50, latest diag). The departure Home Teleport cast never fires.

## The stall loop (verbatim diag)

```
18:47:57  Build 193: quickCast("Lumbridge Home Teleport") -> true
18:48:52  Build 192: Home Teleport cast wait timed out at varp=671, will retry (bounded)
18:48:54  Build 192: dialogue closed, varp=671 (still 670-999) -- casting Home Teleport for departure
18:48:54  Build 192: Rs2Magic.cast(LUMBRIDGE_HOME_TELEPORT) -> false (attempt 2/3, varp=671)
18:48:54  Build 193: quickCast("Lumbridge Home Teleport") -> true
... ~55s of MAGIC TICK (talked3=false) + dialogueTick continue-clicks ...
18:49:49  Build 192: Rs2Magic.cast(LUMBRIDGE_HOME_TELEPORT) -> false (attempt 3/3, varp=671)
18:49:49  Build 193: quickCast("Lumbridge Home Teleport") -> true
18:51:16  Build 192: Rs2Magic.cast(LUMBRIDGE_HOME_TELEPORT) -> false (attempt 1/3, varp=671)   <- counter RESTARTED
18:52:13  ... attempt 2/3 ...   18:53:10  ... attempt 3/3 ...
```

## Mechanism (from the script's own spell dump)

The Build-193 name-based `quickCast("Lumbridge Home Teleport")` finds widget **id=14286854**, which the script's own spell dump flags **`hidden=true`** (no on-screen bounds):

```
18:48:54  Build 193: spell dump: id=14286854 name="<col=00ff00>Lumbridge Home Teleport</col>" sprite=1802 hidden=true
```

`quickCast -> true` means "clicked", but a click on a hidden widget is a no-op — the teleport never starts, varp stays 671, the ~55s cast-wait times out, and the cycle repeats. The direct path `Rs2Magic.cast(LUMBRIDGE_HOME_TELEPORT)` returns `false` on all attempts (inner reason not logged). Contrast: the Build-179 direct path that cast Wind Strike successfully searched 198 spell snapshots and picked the widget **with real bounds** (`id=14286859 bounds=[919,477 40x40]`).

Second issue: the "(bounded)" retry is not terminal. After 3/3 the attempt counter silently restarts at 1/3 (observed 18:51:16; the ~18:50:48 plugin restart also reset the memory-only counter). The bot can loop this forever with no fail-forward and no blocker log.

## Expected vs observed

- Expected: one verified Home Teleport cast → varp advances past 671 → talked3 → tutorial complete.
- Observed: 6+ min of cast → 55s wait → timeout → re-cast, zero varp movement, talked3=false throughout. Between casts only MAGIC TICK lines and instructor-dialogue continue-clicks (actions ~2s apart, so no hang-rule violation on tick cadence — but zero game progress).

## Suggested fix (Alex-owned)

1. `quickCast` must filter name matches to **visible** widgets (`hidden=false` + valid on-screen bounds), mirroring the Build-179 direct path — never click a hidden widget and report it as a cast.
2. Log the clicked widget's id/bounds/hidden on every `quickCast`, and log `Rs2Magic.cast`'s inner failure reason instead of bare `false`.
3. Make the retry bound truly terminal: after N failed departure casts, HOLD with an explicit blocker log (e.g. `MAGIC BLOCKED: home teleport unfireable, varp=671`) instead of restarting the 1/3 cycle.
4. Note: the actually-visible home-teleport widget id on the tutorial spellbook is **unproven** from these diags (the dump prints only hidden=true entries) — do not hardcode 14286854.

Diag evidence: `screenshots/2026-10-07_18-4{6,7,8,9}-*_MAGIC_diag.txt`, `2026-10-07_18-5{1,2,3}-*_MAGIC_diag.txt`.
