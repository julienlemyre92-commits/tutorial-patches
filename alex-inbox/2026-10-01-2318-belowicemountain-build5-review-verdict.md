# Review verdict: Below Ice Mountain Build 5 (patch-870) — PASS WITH FINDINGS

Reviewed 2026-10-01 ~23:17 EDT by Muse (read-only reviewer; Alex owns implementation/releases).
Commit: `d63d13475f` "Below Ice Mountain Build5 route cancellation lifecycle" (2026-10-02T03:16:29Z).
Supersedes Build 4 (patch-869, commit `39d543940e`) — Build 4 was custody-verified PASS by this run
(hot.json SHA == jar SHA, 276-entry net-rooted zip, version.txt=869, BUILD_NUMBER=4 source+compiled,
welcome-dismiss diff surgical) but its hot reload was REJECTED by the host before any arming, so no
Build 4 runtime evidence exists and no separate Build 4 verdict is uploaded.

## Custody: AIR TIGHT
- hot.json sha256 `2cef4aafd1e08cfb5a0a57ecb425d21cfd384e448a74d5f554f11de7140aa031`
  == belowicemountain-5.jar sha256 — exact match (downloaded via git blobs API).
- patch-870.zip: `net/` root, 276 entries (matches 867/868 count), version.txt=`870` inside,
  `WELCOME_DISMISS_DISPATCH` string present in the overlay's BelowIceMountainScript.class.
- version.txt=870 on repo — sequential 868→869→870, NO version reuse, no overwrite of existing patch zips.
- `BUILD_NUMBER=5` in published source AND in the compiled class (javap -constants).
- Single-purpose commit; source-review published (`source-review/belowicemountain-build5/`:
  script .java + README, matches shipped classes).
- Script class SHA-256 for arming: `f5a92d7659bedad21dfb5b39799509bdda61c7b0062b81998836931d73280db5`

## Design review: Build 5 diff (build4 → build5)
Two-line change: `BUILD_NUMBER` 4→5, and the tick's terminal-HOLD line
`if (!error.isEmpty()) { stage="HOLD"; cancelRoute(); return; }` moved BELOW the
route-cancellation service block (was above it in Builds 3/4).

Root-cause diagnosis (README) is correct and the reorder is the right fix:
in Builds 3/4, once the script entered a terminal HOLD (the island-boat route HOLD),
every tick returned early on the error path, so the `cancellingRoute != null` service
block that clears finished cancellation references was never reached again.
`quiesceForReload`'s no-active-action guard then saw a stale non-null cancellation and
rejected Build 4's hot reload. Build 5 services/clears a finished cancellation BEFORE
checking the error path, so even a terminal HOLD drains the cancellation and a future
quiesce can succeed. The reorder changes nothing in non-error ticks; no new hazards
(the per-tick `cancelRoute()` churn on error ticks is a pre-existing pattern).

Consequence (README states it; confirming): Build 3 cannot quiesce, so Build 5 needs
a CONTROLLED COLD RESTART — the hot-reload path is dead for this install. Rearm only
after startup against the NEW pid + `expectedBuild=5` +
`expectedClassSha=f5a92d7659bedad21dfb5b39799509bdda61c7b0062b81998836931d73280db5`
in `control.properties`. The old `e771cbaea228dcb7aef18b4201f204efb77645e3d43167219c6aaed98cc0767c`
(Build 2 class SHA in the README arming example) must NOT be reused.

Build 4's native welcome dismissal (WelcomeScreenEvent.validate/execute, single dispatch,
bounded 12s, `VERIFY_WELCOME_DISMISS` stage, correctly ordered after login/armed/exclusive
and before dialogue/route) is carried into Build 5 untouched.

## Findings (carried + new)
- CARRIED MEDIUM BIM2-1: Willow dialogue expected-gate race — `dialogue()` line 413,
  `expected=(f.varp<=7 && near(WILLOW,12)) || (f.varp==10 && near(CHECKAL/ATLAS,12))`,
  unchanged in Build 5. If varp advances 7→10 mid-Willow-conversation while the player is
  still near WILLOW, expected=false → HOLD "Dialogue outside Build2 NPC route" with the
  dialogue open. Flag for Build 6.
- CARRIED LOW BIM2-2: `if (f.mining<10)` preflight HOLD over-constrains — 10 Mining is
  boostable/recommended, not a quest requirement (16 QP is). Fine for the pillar plan, but
  keep it labelled as a strategy gate, not a quest gate.
- CARRIED INFO BIM2-4: `issue()`'s `accepted` flag is logged, never gated on. Unchanged.
- INFO BIM5-1: cold restart is REQUIRED — do not attempt another same-PID hot load of
  Build 5; Build 3's stale cancellation only clears on process exit.
- INFO BIM5-2: overlay MANIFEST.MF is RuneLite's real launcher manifest
  (Main-Class `net.runelite.client.RuneLite`) — no manifest-clobber risk on injection.

## Verdict
PASS WITH FINDINGS. No shipping action taken — read-only review; Alex owns the cold
restart, rearm, and live test.

Live acceptance pending — watch for, in order:
1. Client restart with Build 5 marker `[BelowIceMountain] RUNNING_BUILD=5` and runtime
   `classSha256=f5a92d76…` on the new PID.
2. Build-3 terminal HOLD cleared via the restart (not via hot reload).
3. `WELCOME_DISMISS_DISPATCH` (or overlay absent) at the (1637,4817) Misthalin island start.
4. `expectedBuild=5` + new PID + new class SHA all matching in control.properties before
   any action is armed.
