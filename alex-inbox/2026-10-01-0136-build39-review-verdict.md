# Muse review-loop verdict: Ernest Build 39 (patch-641) — PASS

- Shipped: commit `c7b821aa` 2026-10-01 05:35:26Z — "Build39: align stair approach with walker arrival radius".
- Packaging: 208-entry net/-rooted zip (built with `zip`), version.txt=641 inside matches ship-time version.txt, META-INF/MANIFEST.MF byte-identical to patch-640, only `ErnestTheChickenScript.class` differs, `runtimeBuild()` = 39 (bipush 39 verified).
- Delta vs Build 38 (javap-verified): exactly one constant change, `iconst_2` → `iconst_3`, at the STAIRS0 approach gate:
  `pos.distanceTo(p(STAIRS0.x, STAIRS0.y, pos.plane)) <= 3` now admits climb-dispatch; beyond 3 tiles the bot keeps `walk()`-ing toward STAIRS0. Aligns the gate with the walker's arrival radius (commit message intent verified in the bytecode).
- Safe: the widened radius changes only *when* dispatch fires, not *what* fires. Dispatch still requires same-plane, a "Climb-up" action candidate, boolean-checked `Rs2GameObject.interact`, per-leg single-shot budgets, 9000ms pending, and the MANOR_STAIRCASE_DISPATCH diag line. Both endpoints of the distanceTo are same-plane-projected (via `p(x,y,pos.plane)`), so no Build-36-style cross-plane capture is introduced.
- Findings: [L] none. [M carry-forward 32–39] single-shot climb budgets persist via status.properties untied to pending lifecycle (interact-true + unproved → permanent HOLD; suggest spend-on-proof for a later build).
- VERIFY BY (acceptance, feed dark since 17:44:02 EDT 2026-09-30 — still pending): fresh `RUNNING_BUILD=39` banner, a MANOR_STAIRCASE_DISPATCH line firing from within 3 tiles of STAIRS0, or the first ERNEST_* screenshot.
