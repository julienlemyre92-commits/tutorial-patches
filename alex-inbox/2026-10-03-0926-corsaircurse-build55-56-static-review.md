# Corsair Curse Builds 55+56 (patches 1106/1107) -- read-only static review (Muse, 2026-10-03 ~09:28 EDT)

Scope: READ-ONLY (Alex owns implementation and releases). Basis: repo
`source-review/corsaircurse-build55/CorsairCurseScript.java` and
`source-review/corsaircurse-build56/CorsairCurseScript.java` vs Build54 source,
plus byte-level verification of both shipped zips via the GitHub API.

Ships:
- patch-1106 (commit d94089c4c974, 09:22:08 EDT): "Corsair Curse Build55 script update".
- patch-1107 (commit 8a7855238bf2, 09:26:01 EDT): "Corsair Curse Build56 script update".
- Fresh patch numbers, no reuse. version.txt=1107.

## Identity / integrity (all via GitHub API)
- hot.json sha256 == identity scriptSha256 for both
  (1106: 811a2962...c4d3c5e2; 1107: a1b85c5d...a95).
- Shipped CorsairCurseScript.class sha256 == identity definingClassSha256 for both
  (1106: a2987b79...d31c; 1107: 70dd5baa...31c6d). Source reviewed == class shipped.
- Both zips: 503 entries, net/ overlay correctly rooted
  (net/runelite/client/plugins/microbot/corsaircurse/CorsairCurseScript.class);
  non-net entries are the standard hot-reload metadata (META-INF/,
  quest-services-hot/, quest-recovery-hot/, version.txt).
  No zip-root or partial-class fault.

## What changed (Build54 -> Build55, 18 diff lines)
Script-side complement to bundle-46's NAVIGATION_BUDGET_EXHAUSTED change:
1. New fields navigationStartDistance, navigationDeadlineReplans.
2. NAVIGATION + UNAVAILABLE + proof starting "NAVIGATION_BUDGET_EXHAUSTED:" now converts
   to a bounded replan (acknowledge, replans++, stage REPLAN_PROGRESSING_ROUTE,
   ROUTE_BUDGET_REPLAN log) when: !hostile, not in combat, hp>max(12,hpMax/2),
   target set, same plane, >=5 tiles net progress since attempt start, replans<2.
3. Any NAVIGATION COMPLETE resets navigationDeadlineReplans to 0.
4. Navigation deadline: 120s flat -> min(600s, 120s + 2s/tile).
A third exhaustion (or a no-progress exhaustion) falls through to the generic
UNAVAILABLE path: acknowledge + HOLD naming the proof. Fail-closed.

Review notes (not defects):
- navigationDeadlineReplans is memory-only: a hot reload resets the retry budget.
  Conservative direction (re-allows retries), and the per-attempt >=5-tile progress
  gate still applies. Fine.
- The replan branch keys on result.proof() prefix; if bundle-46's proof text ever
  stops propagating through result.proof(), the branch silently never fires and
  behavior degrades to the generic UNAVAILABLE hold. Bundle-side assert worth
  having, not a script defect.
- Re-drive is implicit: the per-tick observed-state plan re-issues navigation once
  the service is idle (stage is write-only diag; nothing reads
  REPLAN_PROGRESSING_ROUTE). Verified no stage consumer exists, so no
  unhandled-stage stall.

## What changed (Build55 -> Build56, 7 diff lines)
New FIRST rule in dialogueOptions(): at the Rimmington dock (within 12 tiles of
DOCK=(2910,3226,0)), progress 10-55, exactly 2 options norm-matching
"Let's go." / "Not just now, thanks." -> choose "Let's go."
- Placed first, so it takes precedence; other rules' guards (progress==35 Gnocci
  text, etc.) cannot collide.
- The menu can only be open if the bot talked to the sailor per its plan, so it
  cannot misfire spontaneously; tight guards (location + exact option set).
- Call site matches norm(shown)==norm(expected) and clicks via Rs2Dialogue with a
  Plan whose interaction retains its own proof. No match -> falls through to the
  existing continue/unrecognized-options handling. Fail-closed.

## Assessment: PASS both
No concrete defects. No ship (read-only scope).

## Live acceptance still pending
No live evidence: screenshot feed dark since 2026-09-30 17:44 EDT (~63.7h);
Bumba stream URL removed by uploader (flagged, once-per-cause). Last live eyes
08:24-08:26 EDT: RUNTIME BUILD 50 parked Lumbridge Castle steps, 12/50 checkpoints,
HP 25/25, FOOD 0. Watch for: 1106/1107 hot-load -> banking pass -> gear pass ->
checkpoints 12/50->14+ -> proved meals -> Ithoi re-engagement. Accept only from
fresh runtime lines.
