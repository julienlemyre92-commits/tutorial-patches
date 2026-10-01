# Prince Ali Rescue Build 68 review verdict (Muse read-only)

**Verdict: PASS WITH FINDINGS** — ship-quality, custody clean. One medium-conditional finding on the new mechanism's reachability (safe failure mode, no crash/loop risk).

## Custody (verified via git blobs raw API, byte-exact)
- hot.json `9f2102eb461d0252ca7c1147e13477c575815ca101f4fdd658f6002fb628242f` == princealirescue-68.jar (88680 B) sha256 — MATCH.
- patch-761.zip: 231 files, net-rooted (227) + META-INF/MANIFEST.MF + benign root version.txt; in-zip version.txt=761.
- All 3 script classes byte-identical zip<->jar. BUILD_NUMBER=68. Single-purpose commit 7773af78 (2026-10-01T21:57:46Z). No stale-class reship.

## Delta 67→68 (37 changed lines, script only)
1. **Second-respawn reset**: new persisted `graveRecoveryRuns` (+1 at recovery start; restore default 0-or-1 for reload compat) and `graveRecoveryAfterEscapeError`. If held && stage non-empty && KEY_PRINT==0 && PASTE==0 && within 40 of Lumbridge respawn (3222,3219) && runs<2 → reset to APPROACH with latest death target, capped at 2 total runs. Bounded, no loop.
2. **Nonblocking loot**: `Rs2Death.openGrave()` → `grave.click("Loot")` (API verified: Rs2Death.getGrave() returns Rs2NpcModel, click(String) exists in installed microbot-base.jar; grave null-guarded). Removes the ~5s fee-window block while standing next to aggressive NPCs; the B67 direct-loot fast path and WAIT_OPEN 12s deadline cover both outcomes.
3. **Held→ESCAPE auto-retreat**: `if(held)` now escapes to Al Kharid bank when within 40 of the grave target (phase GRAVE_FAILURE_RETREAT_TO_BANK), and on bank arrival re-holds with the saved reason ("Safely retreated from incomplete grave recovery: ..."). Converts a mid-grave terminal hold into an at-bank terminal hold — safe parking, reason preserved for diagnosis. No NPE path: target is provably non-null whenever stage is non-empty (set with fallback 3296,3313 at recovery start, persisted), and the entry gate sets it before any fall-through.

## Findings
- [MEDIUM conditional new] The second-respawn reset requires `held==true`, but a genuine second death during recovery arrives with held==FALSE: all holds were cleared at recovery start, and the only HP-fall hold source (BronzeBarSource.resolve(), line ~3770) cannot fire while bronzeSource==null during recovery — no other death detector runs on this path (verified: only respawn references are lines 1865/1875). So the reset misses the exact case the commit message names; a true second death → stage stuck (APPROACH/WAIT_*), ~4 min idle at Lumbridge until the 240s deadline → terminal HOLD "Grave approach unproved". Reachable today only when a recovery-internal deadline hold fires while still within 40 of Lumbridge (e.g. walk never left). Safe failure mode (no loop, no crash), but the named mechanism is near-unreachable. Suggest: drop the `held` requirement and key the reset on observed respawn (stage active + within 40 of Lumbridge + no KEY_PRINT/PASTE + runs<2) — the existing conditions already prevent misfire on a healthy recovery. Sub-case: with runs==2 exhausted and held==true at Lumbridge, `if(held) return true` with a far target stalls silently with no deadline — consider a deadline on that return.
- [LOW new] ESCAPE doesn't re-drive the bank walk after a mid-escape death: the blocking walkTo was issued once on ESCAPE entry; after a respawn with quest items in hand, no re-walk is issued → waits for the 240s deadline → HOLD "Recovered grave items but safe-bank arrival unproved". Pre-existing pattern (since Build 66); bounded and messaged.
- [MEDIUM conditional CARRIED] Banked bronze pickaxe 1265 gap still open. [LOW carried] Partial-set direct-loot fast-path gap (from Build 67 verdict). [LOW carried] Dead-code Shantay resume log under F2P; members-world gate by design.

## Live acceptance
PENDING — feed dark since 2026-09-30 17:44 EDT; no live stream URL; builds 3–68 never live-verified from here. Acceptance needs NEW runtime lines (GRAVE_RECOVERY_SECOND_RESPAWN / GRAVE_DIRECT_LOOT_PROVED).
