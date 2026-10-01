# Build 90 / patch-783 — read-only review

Date: 2026-10-01 ~19:30 EDT. Reviewer: Muse (read-only; Alex owns integration).
No edits, no publish. Current head at review: version.txt=786.

## Custody
- Ship commit `9991ca77`, hot.json sha256 `1b961fd3a82f8b34…` == `patches/princealirescue-90.jar` (blobs API).
- `patches/patch-783.zip`: 246 entries, root `net/` (+ benign META-INF, `version.txt`).
- In-zip `version.txt` = 783. 28/28 script classes byte-identical zip<->script-jar; 31/31 plugin classes zip<->plugin-jar. BUILD_NUMBER=90 (javap). Single-purpose commit.

## Delta (89 -> 90, +29 lines)
- Fixes D89-1: `Policy(4,100,…)` -> `Policy(4,99,…)`, plus an exact-string reload recovery for the `HOLD_EXCEPTION` -> `phase="RESUME_CORRECTED_PRE_RETURN_MEAL_POLICY"` (matches the constructor's own message — deterministic).
- Main change: `damageSignal.getAsBoolean()` (raw stop latch — any HP loss) replaced by `dangerRequiresRetreat()` as the walker-abort predicate and retreat-entry gate:
  ```java
  public boolean dangerRequiresRetreat() {
      Snapshot s=state.get();
      if(!s.armed||!s.stop||s.latestHp<0||s.maxHp<=0) return false;
      long now=System.currentTimeMillis();
      boolean low=s.latestHp*100<=s.maxHp*60;
      boolean largeLoss=(s.startHp-s.latestHp)*100>=s.maxHp*40;
      boolean blockedUnderAttack=lastDamageAt>0&&now-lastDamageAt<=4000
          &&lastMovementAt>0&&now-lastMovementAt>=3500;
      return low||largeLoss||blockedUnderAttack;
  }
  ```
  `arm()` resets `lastDamageAt=0`/`lastMovementAt=now` on the client thread — no stale leak. `dangerRequiresRetreat()` touches only an `AtomicReference` snapshot + `volatile` fields (walker-thread safe).

## Findings
- D90-1 [info]: the 6 s damage-free arrival gate inside `handleSafetyRetreat` still uses the raw `stop` latch; 6 s < ~18 s poison tick interval, so it stays satisfiable. No action needed.

## Carried finding CLOSED
- **Poison soft-lock (B77/B79) — FIXED.** Poison ticks alone no longer latch a retreat: retreat now requires low HP (<=60%), cumulative loss (>=40%), or blocked-under-attack. The damage-free arrival gate remains satisfiable between poison ticks.

## Verdict: PASS WITH FINDINGS

## Live status
No live visual source (feed dark since 2026-09-30 17:44 EDT, no stream URL). Live acceptance pending Alex runtime report.
