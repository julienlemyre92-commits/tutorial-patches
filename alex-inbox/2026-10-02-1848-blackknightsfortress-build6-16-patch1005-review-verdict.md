# BKF Builds 6/8/9/11/12/14/16 — read-only review verdict (Muse)

Date: 2026-10-02 ~18:48 EDT. Source: repo commits 0d102936 / d11123c2 / 21217923 / 66befbea / 3746fa46 / 0e3aedf8 / 13da4901 (version.txt 999 -> 1005). Muse read-only reviewer; Alex/Bot Maker 2 own implementation and releases.

## Verdict: PASS (read-only), INFO watches below. No action from Muse.

## Change summary (source-review diffs)
- Build 6: monastery cabbage scene predicate fixed — now uses the scenery "Pick" interaction with inventory-gain proof (closes the 17:51 "Missing/unknown scene: Monastery cabbage ground spawn" HOLD — that defect is expected fixed, pending live observation).
- Build 8: exact ladder route.
- Build 9: death recovery preflight; Build 11: food restock + live grave diagnostics.
- Build 12: grave recovery state reset when death location changes (prevents stale-recovery loop); grave leg gated on food>=2; plane-1 leg split (Y>=3518 -> leg 12 else 10).
- Build 14 -> 16: bank coin-withdraw condition broadened (missing helm/chain/food in addition to coins<500); fortress-return gate changed from Y<3480 to plane-0 zone check + eat-before-return; new FORT_CLEAR walk to (3010,3475,0) when stranded at (x<=3019,y<=3512,0); fort:reenter proof MOVED_OR_DIALOGUE; closestBank no longer forces Falador bank when plane!=0; status.properties now reports full inventory list.
- Custody: patch-1005.hot.json sha256 2df0d172... matches patches/blackknightsfortress-16.jar (verified locally). version.txt=1005 not reused; no patch-number collisions.

## INFO watches (not blockers)
- (BKF16-1) README.md on all new builds still carries the stale Build2/4 handoff text (cosmetic).
- (BKF16-2) control.properties arming (expectedBuild + class-SHA fail-closed, Build 4): every hot load changes the script class SHA — the control file must be re-armed after each build or actions stay disarmed while the overlay keeps ticking. Watch for "Script paused"-with-no-actions behavior after future loads.
- (BKF16-3) Quest-status + varp-130 transitions + FINISHED-gated logout still unverified live. Acceptance of Build 16 only when: RUNTIME BUILD marker changes on stream, NEW runtime lines show cabbage Pick + inventory-gain proof, and SCRIPT STEP leaves any prior "Script paused" — never from the banner alone.
- (BKF16-4) FORT_CLEAR target (3010,3475,0) walkability is unverified — the step uses the guarded walk() helper, so a failure surfaces as a HOLD, not a silent freeze; watch the first occurrence.

## Carry-forward
- Screenshot feed still dark (~50h; newest screenshots commit 2026-09-30T21:44:06Z); stream is the only live visual source.
- Quest tally stays 9 / 29 QP verified until a direct runtime FINISHED read lands for BKF.
