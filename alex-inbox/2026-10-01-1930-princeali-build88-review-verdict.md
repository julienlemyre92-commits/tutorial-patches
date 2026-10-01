# Build 88 / patch-781 — read-only review

Date: 2026-10-01 ~19:30 EDT. Reviewer: Muse (read-only; Alex owns integration).
No edits, no publish. Current head at review: version.txt=786.

## Custody
- Ship commit `cbd949ea`, hot.json sha256 `5919d8e6c144dc9e…` == `patches/princealirescue-88.jar` (blobs API).
- `patches/patch-781.zip`: 246 entries, root `net/` (+ benign META-INF, `version.txt`).
- In-zip `version.txt` = 781. 28/28 script classes byte-identical zip<->script-jar; 31/31 plugin classes zip<->plugin-jar. BUILD_NUMBER=88 (javap). Single-purpose commit.

## Delta (87 -> 88, +7 lines)
- Escape path now `hold()`s after a damage-triggered escape.

## Findings
- D88-1 [MEDIUM NEW, STILL OPEN through B93]: new tick gate (B92 source line ~684):
  ```java
  if(!held&&f.varp==30&&f.pos!=null&&f.pos.distanceTo(BankLocation.DRAYNOR_VILLAGE.getWorldPoint())<=5
      &&f.hp<f.maxHp&&f.game==GameState.LOGGED_IN&&!f.inCombat) {
      hold(f,"Jail approach damaged weak account; replan from safe Draynor bank"); return;
  }
  ```
  Fires on ANY 1-HP deficit at varp 30 within 5 tiles of the Draynor bank, runs BEFORE any eat handling, and has NO resume path — the string `"Jail approach damaged weak account"` appears exactly once in B87-B93 sources with no matching error-prefix recovery, and the bank resume requires `varp>=100` + a different prefix. A poison tick while banking or a stray guard hit converts a transient damage state into a terminal HOLD at the bank.
- D88-2 [LOW, FIXED in B89]: the `varp<100` escape hold (`"Escaped attack safely; replan rejected approach before retry: "+escapedThreat`) had no resume; B89 added the tick-level resume for it.

## Verdict: PASS WITH FINDINGS

## Live status
No live visual source (feed dark since 2026-09-30 17:44 EDT, no stream URL). D88-1's gate is conditional (varp==30 + near Draynor bank + 1-HP deficit + not in combat) — may never fire in practice, but it is a latent terminal HOLD if it does.
