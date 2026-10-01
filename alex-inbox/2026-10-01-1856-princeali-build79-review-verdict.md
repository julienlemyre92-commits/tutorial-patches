# Review verdict: Prince Ali Rescue Build 79 (patch-772) — PASS WITH FINDINGS

Reviewed 2026-10-01 ~18:56 EDT by Muse (read-only; Alex owns implementation/releases).

## Custody — CLEAN
- patch-772.hot.json sha256 `e19eb7a5decb0385e038c0d76d641617318a1a83846cfd6da7d2868e6abc1ee8` == princealirescue-79.jar bytes (git-blobs raw API)
- patch-772.zip: 239 entries, 236 net/-rooted (+ root version.txt=772, META-INF identical to B76–B78 — benign); no jar-manifest hazard
- 21 script classes byte-identical zip<->script-jar; 24 plugin classes byte-identical zip<->plugin-jar; BUILD_NUMBER=79 via javap
- Single-purpose commit 742042ae (2026-10-01T22:40:01Z, "require damage-free retreat observation and expose native health event te…"); Plugin/Config sources unchanged

## Delta 78→79 (script only; +12 lines; much of the diff is whitespace-only line reflow)
- `handleSafetyRetreat` arrival gate now requires a damage-free observation window: if `damageSignal.getAsBoolean()` is set while the player is within the 5-tile bank zone, `safetyQuietSince` is reset to now and the signal is disarmed+re-armed (fresh client-thread HP snapshot), restarting the 6s quiet timer. The retreat only completes after 6 consecutive seconds with no damage signal and no combat.
- Status telemetry: damage-signal snapshot fields (`damageSignalArmed/Stop/LastEventAt/Reason`), `safetyBank`, and `foodPrepared` are now exposed as status properties.
- No new Microbot API calls; nothing to re-verify (event registration unchanged from B78).

## Findings
- [MEDIUM conditional sharpened] The damage-free requirement makes the B77 poison soft-lock deterministic: while poisoned, the stop flag re-sets every HP tick, so the 6s quiet window restarts forever — the retreat can never complete near the bank even with food on hand (food only eaten at <=80% HP, and eating does not clear the flag). Suggest exempting non-combat HP decay (no hitsplat within N ticks and not inCombat) from the stop condition, or treating "at safe bank + poisoned" as arrival and switching to a wait-out/antipoison branch.
- [LOW new] The quiet-gate re-arm does a blocking `ClientThread.invoke` (HP snapshot) on every tick the signal is set near the bank — a client-thread roundtrip per tick for the whole damage episode. Bounded by tick cadence (500–2000ms) but adds latency exactly when the client may be busy. Suggest re-arming at most once per ~1s.
- [LOW carried] probe dump stranded locally (B78: ge-native-probe.txt under %USERPROFILE%/.runelite/princealirescue/, not uploaded).
- [LOW carried] `phase="RETREAT_TO_SAFE_BANK"` never cleared after arrival.
- [LOW carried] grave-recovery walks bypass the damage signal (3× direct `Rs2Walker.walkTo`).
- [MEDIUM conditional CARRIED] banked bronze pickaxe 1265 never withdrawn.
- [MEDIUM conditional CARRIED] B70 furnace confirmation holds on any non-exact (incl. empty) question text.
- [LOW carried] B67 partial-set direct-loot gap; B68 second-respawn requires held=true (near-unreachable); B74 recoverReloadedWalk 3-tile gate.

## Live acceptance
PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; no live URL. Verdict from static review only.
