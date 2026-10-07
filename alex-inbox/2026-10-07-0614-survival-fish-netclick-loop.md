# SURVIVAL fish step: Net clicks never register (verified loop, read-only)

- FINDING: Fresh tutorial client entered SURVIVAL at 06:10:33 EDT (varp 281=20, server-side). The fishing step has now burned through attempt 3/30 with zero progress. Evidence (06-12-36 and 06-13-36 diag tails):
  - 06:12:30 "Build 323: fish transaction timeout after 10 ticks, no shrimp (attempt 3/30)" -- all 10 pending ticks read playerAnim=-1, inv raw=false.
  - 06:13:27 -> 06:13:35 a second transaction pending 3/10 -> 9/10 ticks, again playerAnim=-1 on every tick, inv raw=false (same timeout trajectory).
  - Interleaved Build 329 "fish action did not issue Net click (walking/retrying, dist>2 or no reachable stand tile) -- transaction NOT armed" on ~6 ticks, while the SAME tick's FISH-DIAG lists 'Fishing spot' id=3317 tile=(3103,3092) dist=1 from player (3104,3092) with actions=[Net].
  - Player never leaves (3104,3092); inv stays empty; no fire; no shrimp after ~3 min of attempts.
- EXPECTED vs OBSERVED: expected a Net click on an adjacent spot -> fishing animation (playerAnim != -1) within ~2 ticks -> raw shrimp in inv. Observed: armed transactions never produce a fishing animation, and the click-guard refuses to click despite an adjacent Net-action spot.
- ROOT CAUSE (candidates): (a) stale click target -- spots drift between ticks (3099,3090 / 3101,3092 / 3103,3092 across scans), so the clicked tile may no longer host the spot when the click lands; and/or (b) the stand-tile derivation behind "no reachable stand tile" rejects the adjacent tile while the transaction is armed by a walk-completion ("Action fish completed") rather than a real Net menu click. The Build 329 refusal predicate contradicts the observed dist=1 [Net] spot on the same tick.
- SUGGESTION: log the target spot tile + the actual clicked menu action when arming the transaction; re-scan the spot object immediately before clicking instead of reusing the scan's tile; log WHICH spot the dist>2/stand-tile guard rejected and why.
- VERIFY BY: a tick where playerAnim shows the fishing animation after arming, then raw shrimp in inv; Build 329 refusal lines gone when a dist<=2 [Net] spot is present.
