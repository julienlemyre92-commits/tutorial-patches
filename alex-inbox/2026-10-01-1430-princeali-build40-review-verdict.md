# Prince Ali Rescue Build 40 review verdict — PASS (Muse read-only, 2026-10-01 14:30 EDT)

Patch-733.zip, commit "Prince Ali Rescue Build40: keeps native reconnect active under exact quest HOLD" (2026-10-01T18:25:08Z; commit message reused verbatim 14th time). Note: this build landed ~106s AFTER Build 39 while the Build 39 review was still in progress — the 30s loop keeps shipping faster than the review window, and Build 39 was never verified live.

## Chain of custody — PASS
- patch-733.zip: 221 files, net/-rooted (218 net/ + META-INF/ + MANIFEST.MF + version.txt), in-zip version.txt=`733` == repo version.txt at ship time.
- patch-733.hot.json: {"plugin":"princealirescue","patch":733,"hostVersion":1,"build":40,"sha256":"d8162bc5...ff7399"}; downloaded patches/princealirescue-40.jar (46,751 bytes), sha256 matches hot.json exactly.
- Hot jar = script classes only (Script + $Frame + $Pending), all byte-identical to patch-733.zip.
- RUNNING_BUILD=40 verified via javap (bipush 40 in Script).

## Delta Build 39 → Build 40 (normalized javap -c diff on PrinceAliRescueScript; Plugin/Config unchanged)
Cleanup-only, in `recoverObservedTreeApproachReload`:
- Removed the `!ashesApproachReloadRecovered` one-shot latch gate.
- Removed the redundant `restoredInFlightAction.equals("ASHES_APPROACH_LOG_SOURCE")` gate.
- Both were dead code: after a successful recover phase is already `RESCAN_AFTER_TREE_APPROACH_RELOAD` (≠ `HOLD_RELOAD_IN_FLIGHT`), so the method cannot re-fire; the error string `"Reload during ASHES_APPROACH_LOG_SOURCE;..."` already encodes the in-flight action. The `ashesApproachReloadRecovered` field now survives only in the status.properties dump/restore paths (no longer consulted as a gate).
- Zero behavior change; no new game-API calls; remaining gates unchanged (logged-in, varp20, plane 0, within 16 of FRED_POS, no log 1511, tinderbox 590 present).

## Build 39 delta (superseded, reviewed, never live-verified)
Build 39 → same method: removed the `hasWoodcuttingAxe(frame)` gate from the recover's success path; success path now sets `geStage="ASHES_GET_NORMAL_LOG"`. Likely deliberate (avoids false-negative when the axe is equipped rather than in inventory; the main flow still gates on the axe at 7 remaining call sites). But the RECOVERED diag line still claims "axe+tinderbox" while the axe is no longer actually verified — the log message now over-claims. Suggest updating the line to match the actual checks.

## Observations for Alex
- O1: Commit message reused verbatim 14th time — still hides the actual delta (dead-gate cleanup). One-line real summary per build would help the review keep up.
- O2: At this ship cadence (Builds 36→40 in ~13 min) review verdicts land after supersession. Consider batching or pausing the loop when no new live signal arrives.

## Carried open items
- Build 25 dead-tinderbox-recover STILL OPEN; Build 32 exactExpiredSourceHold unreachable-flag open.
- Build 39 RECOVERED log line over-claims "axe+tinderbox" (axe gate removed) — NEW.

## Live acceptance
- PENDING. Screenshot feed dark since 2026-09-30 17:44 EDT (~20.75h); no live stream URL confirmed. Nothing counts as live-confirmed until the feed returns or the game's own state shows it.
