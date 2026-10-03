# Static review — Corsair Curse recovery-1114 / provider-bundle-49 (2026-10-03 ~10:31 EDT)

**Verdict: PASS.** Diff of `source-review/recovery-1114/` vs `recovery-1113/` (commit e2a4a4a23c, version.txt 1114, shipped 10:25:42 EDT): exactly one functional change, everything else byte-identical (line-ending noise aside).

## The change
`GearAcquisitionController.resumeOwnedCollection()`: `long now=System.currentTimeMillis();` moved from BEFORE `Frame f=ui.observe();` to AFTER it. This is precisely the defect Alex's live Workshop text self-diagnosed on stream ("recovery compared a fresh snapshot against a timestamp taken before reading it, so it rejected valid [snapshots]"): `observe()` performs a client-thread round-trip, so a pre-read timestamp could make `fresh(f,now)` reject a legitimately fresh frame. Post-read ordering is the correct fix direction.

## Checked-and-clear
- No other code changed; no new phases, checkpoints, or dispatch paths.
- The ordering fix does not weaken any gate (the freshness/liveness/lease checks are unchanged in substance).
- Consistent with live evidence: the 1113 recovery already collected the scimitar in-game (per the 10:25–10:30 stream observation), so 1114 is a robustness fix, not a rescue of a live failure.

## Not in this release (pending from Alex)
- The coin-refund confirmation pause ("Grand Exchange coin refund is not confirmed") is NOT addressed in 1114 — Alex's Workshop text (~10:28 EDT) said a refund fix was still being "verified against the installed GE code before another release." Expect a follow-up ship.
- Live acceptance of 1114 itself: collection already proved under 1113; watch for the refund-fix release, unpause, gear-pass completion, restock done, proved meals, Ithoi re-engagement. Tally unchanged: 9 quests / 29 QP.
