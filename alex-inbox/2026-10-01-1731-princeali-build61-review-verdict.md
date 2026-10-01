# Prince Ali Rescue Build 61 review verdict (Muse, read-only)

- **Verdict: PASS WITH FINDING (carried forward, medium)**
- **Build:** Prince Ali Rescue Build 61 / patch-754 (commit f1d9cc94, 2026-10-01T21:29:49Z). version.txt=754.
- **Custody:** clean. hot.json sha256 `2e5174e7f44fb1e1084f1a687ecf0078b22983abff4d420a3e28518404bf8c5e` matches princealirescue-61.jar (67404 bytes) byte-for-byte (git blobs API). Source-review BUILD_NUMBER=61 matches the shipped build. Single-purpose commit (hot.json, patch-754.zip, princealirescue-61.jar, princealirescue-plugin-61.jar, source-review/princealirescue-build61/, version.txt).

## Delta 60 -> 61 (52 diff lines)
1. **Dismissal-proof gate hardened** (`handleMembershipPromo`, ~line 625): now requires `LOGGED_IN` + `f.pos != null` before a missing prompt counts as dismissal proof ("A missing interface during logout/loading is not dismissal proof") -- fixes false-proof on loading/logout frames. Pending non-promo actions defer while unheld (let their proof window finish); after a terminal `Unproved ...` hold it logs `PROMO_INSPECT_AFTER_TERMINAL_ACTION`, clears pending, and inspects the promo -- the promo can be dismissed even when another action has held.
2. **Named banner route first** (`probeMembershipPromo`, ~line 773): tries `InterfaceID.MembershipBenefitsPrompt.CONTENT`/`CLOSE`/`ARTCANVAS` before the text-marker DFS (comment notes the banner's words are artwork and need not occur in widget text). The close X is validated: not hidden, <=64px bounds, inside the content box, on-canvas.
3. **Close-candidate tightening** (~line 806): semantic candidates now ALSO require the geometric upper-right test (was semantic OR geometric) -- fewer false close clicks.
4. **Tree-traversal coverage fixes** (~lines 823-847): the DFS no longer prunes children of hidden widgets (previously `w.isHidden()` skipped the whole subtree -- children of hidden parents were invisible to the search); text matching now excludes hidden widgets' own text (no hidden-text false positives); static + nested children are collected alongside dynamic children. A prompt nested under hidden/static parents can no longer be missed entirely.

## Findings
- **MEDIUM (carried forward from Builds 58/59/60, NOT fixed):** line 1338 (`Reachable Shantay recovery lost its persisted interaction target; no new Trade`) still lacks the `!f.shop` exemption its line-1331 sibling has. Shop-open-during-approach clears `shantayApproachTarget` into the diagnostic-only `RECOVERED_SHANTAY_SHOP_OPEN_DURING_APPROACH` phase label; the next tick re-enters `bronzeBarShantayTick` via the `geStage` dispatch, and with `shantayReachableTradeUsed=false` the gate terminally HOLDs with the shop OPEN, making the stock-check -> Buy-1 purchase path unreachable. Fix unchanged: add `&&!f.shop` to the line-1338 gate.
- **LOW (partially addressed):** the named route makes the promo-visible case O(1), but `probeMembershipPromo` still runs inside `observe()` every tick on the client thread, and in the common no-promo case the DFS text-marker scan still runs -- now with BROADER traversal (hidden subtrees + static/nested children). Bounded, not a correctness issue; flagging so the per-tick cost growth stays intentional.

## Live verification
None possible this run -- screenshot feed dark since 2026-09-30 17:44 EDT (~23.8h), no live stream URL confirmed. Builds 3-61 remain live-unverified.
