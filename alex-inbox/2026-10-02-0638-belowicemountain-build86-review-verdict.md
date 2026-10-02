# Below Ice Mountain Build 86 review verdict (read-only, Muse)

- Date: 2026-10-02 06:38 EDT (10:38Z observe)
- Build: 86 — commit e8ecade0 "Below Ice Mountain Build86 stage35 transition escape" (10:35:19Z), linear on own B85 seen.log 05e36c52
- Patch: patch-947.zip / belowicemountain-86.jar / patch-947.hot.json; version.txt=947 at ship, at observe, and re-checked pre-publish (no race)

## Verdict: PASS WITH FINDINGS

## Custody chain (verified)
- patch-947.zip: 294 entries, 291 net/-rooted (same pattern as 936+), plus META-INF/ + MANIFEST.MF + version.txt
- in-zip version.txt = 947
- belowicemountain-86.jar sha256 f4d919e58bde65b3d25c575eb51590818d49c4eb7db2ffb5d06e91ac9cd20bc2 == patch-947.hot.json sha256 — FULL MATCH (git-blob raw downloads)
- BUILD_NUMBER = 86 javap-verified in shipped jar (public static final int BUILD_NUMBER = 86)
- Source-review script byte-compiles on JDK 17.0.20.1 (microbot-base.jar + lombok.jar on classpath) with ONLY the 2 pre-existing BelowIceMountainConfig symbol errors (lines 428/727 — carried B71→B85, config class lives outside the shipped jar, not a regression)
- README byte-identical to B85 (sha 4a0dd2e74d9b951f, 37103 bytes) — no B86 section

## Delta B85 → B86 (+17/−8, "stage35 transition escape")
1. BUILD_NUMBER 85 → 86.
2. The diagnostic error path changed: previously `if (!error.isEmpty()) { stage="HOLD"; cancelRoute(); return; }` was an unconditional hard HOLD. B86 now, on non-empty error, cancelRoute()s first and then — instead of hard-freezing — checks `LOGGED_IN && inKnownCaveInstance(f.position) && f.hp>0 && guardianActionsAllowed()`; if true it sets guardianRetreat=true and runs guardianTick(f) on the same tick (a bounded "transition escape" so a diagnostic HOLD does not strand the player under attack in the instanced cave). Otherwise stage="HOLD" exactly as before.
3. In dialogue()'s options branch, the issue lambda for the stage35EnterYes / willowYes / cookSandwich / ale / rps options now records `entranceAt=System.currentTimeMillis()` when the option click is ACCEPTED for stage35EnterYes (only on `accepted && stage35EnterYes`, not on rejection). This arms the existing 60s post-entrance timeout at line 2383 for the stage-35 warning-dialogue acceptance path (previously entranceAt was only set via the `entrance:` issue-key path at line 3212).

## Findings
- NEW INFO BIM86-1: the escape's `guardianRetreat=true` is memory-only — guardianRetreat is NOT in the hot-reload state map (only trainingRetreat is persisted). A hot reload landing mid-retreat wipes the flag; however the pre-existing guardian arming paths (e.g. ~2895 !guardianActionsAllowed() → guardianRetreat=true, ~2948 guardianActiveAt window) re-set it on subsequent ticks, so the window is a single-tick loss, not a trap. Fail-closed.
- NEW MINOR BIM86-2: the escape gate requires f.hp>0 AND guardianActionsAllowed() (supervised control file); if the control file is stale/disabled while the player is mid-cave with a live guardian, the escape is skipped and the hard HOLD path applies — consistent with B82-1 (CONTROL file expectedBuild must be current), but worth noting the escape is conditional, not unconditional.
- entranceAt persistence confirmed (state map lines 581/721, line 2383 60s timeout, line 3212 entrance: keys) — no hot-reload loss on the new timestamp.
- Carried: BIM85-1/85-2, BIM84-1 (mooted for the warning case), BIM83-1/83-2, BIM82-1/82-2, BIM81-1/81-2, BIM78-1, BIM79-1, BIM75-1, BIM77-1, BIM71-1, BIM71-3..6, BIM61-1, BIM57-1, BIM68-1, BIM70-1/70-2, BIM72-1, BIM53-1.

## Live acceptance
None — screenshot feed dark since 2026-09-30 17:44 EDT; B57–B86 all pending live confirmation. Watch-keys for B86: RUNTIME BUILD 86 + absence of HOLD parks when a diagnostic error fires mid-cave (guardianRetreat/escape lines instead), and entranceAt-driven timeout behavior after stage-35 entrance "yes." acceptance.
