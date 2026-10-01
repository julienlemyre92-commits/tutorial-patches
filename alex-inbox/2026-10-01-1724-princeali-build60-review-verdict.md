# Prince Ali Rescue Build 60 review verdict (Muse, read-only)

- **Verdict: PASS WITH FINDING (carried forward, medium)**
- **Build:** Prince Ali Rescue Build 60 / patch-753 (commit b26dd8718d, 2026-10-01T21:23:10Z). version.txt=753.
- **Custody:** clean. hot.json sha256 `aaff515ebd8a484744858013223ef3b7f4ec27f30d8174333b64afce46dd394b` matches princealirescue-60.jar byte-for-byte (blobs API). Source-review BUILD_NUMBER=60 matches the shipped build. Single-purpose commit (hot.json, patch-753.zip, princealirescue-60.jar, plugin-60.jar, source-review/princealirescue-build60/, version.txt).

## Delta 59 -> 60 (228 diff lines)
1. **Membership-promo blocker handling** (bulk of the delta): `handleMembershipPromo()` runs in `tick()` before any stage dispatch. It probes the widget tree for a "become a member" marker + a unique close X, clicks it once via Rs2Widget, and waits for proof the popup is hidden (6s pending). Dismissal-failure modes are single-shot HOLDs with distinct errors — no infinite click loops.
2. **Post-promo Shantay resume** (`resumeShantayAfterMembershipPromo`): if the bot was HELD on an unproved/reloaded `BAR_SHANTAY_REACHABLE_TRADE` when the promo blocked the UI, after dismissal it replans a reachable interaction tile through a NEW exclusion set (current tile, three hard tiles, prior approach target), rebases `sourceStartedAt`, clears HOLD state, and reserves exactly one post-dismissal Trade attempt (`shantayPromoRetryActive`/`shantayPromoRetryUsed`). Gating is tight: requires held + LOGGED_IN + varp273=20 + key print + exact coin cap + shop closed + no dialogue; returns false cleanly when the tile planner finds nothing.
3. Exclusion-set overload of `findShantayReachableInteractionTile` shared by the new resume path.

## Findings
- **MEDIUM (carried forward from Builds 58/59, NOT fixed):** the "lost its persisted interaction target" HOLD at source-review line ~1307 (`if(shantayReachableTileRecoveryUsed&&!shantayReachableTradeUsed&&!shantayPromoRetryActive) hold("Reachable Shantay recovery lost its persisted interaction target; no new Trade")`) still lacks the `!f.shop` exemption its line-~1301 sibling has. With the shop OPEN and `shantayReachableTradeUsed=false` (shop-open-during-approach clears `shantayApproachTarget` at line ~1369 into the diagnostic-only `RECOVERED_SHANTAY_SHOP_OPEN_DURING_APPROACH` phase label, and the next tick re-enters `bronzeBarShantayTick` via the geStage dispatch at line 1157), the bot terminally HOLDs with the shop open and the purchase logic (stock check -> Buy-1 at lines ~1336-1354) unreachable. Fix unchanged: add `&&!f.shop` to the line-1307 gate.
- **LOW (new):** the "become a member" marker probe runs inside `observe()` on the client thread every tick — a full widget-tree DFS per tick. Bounded cost, not a correctness issue; only flagging so it stays intentional.
- Noted, not defects: `RECOVERED_SHANTAY_SHOP_OPEN_DURING_APPROACH` and `RESUME_SHANTAY_AFTER_MEMBERSHIP_PROMO` are diagnostic-only phase labels (dispatch routes by `geStage`, line 1157), which is fine; the DISMISS-promo happy path resumes naturally because `shantayApproachTarget` is never cleared by the promo path.

## Live verification
None possible this run — screenshot feed dark since 2026-09-30 17:44 EDT (~23.7h), no live stream URL confirmed. Builds 3-60 remain live-unverified.
