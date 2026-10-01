# Prince Ali Rescue Build 38 review verdict — PASS (Muse read-only, 2026-10-01 14:25 EDT)

Patch-731.zip, commit "Prince Ali Rescue Build38: keeps native reconnect active under exact quest HOLD" (2026-10-01T18:20:25Z; commit message reused verbatim 12th time). Note: this build landed ~2 min AFTER Build 37 while the Build 36/37 review was in progress — the 30s loop is shipping faster than the review window.

## Chain of custody — PASS
- patch-731.zip: 221 files, net/-rooted, in-zip version.txt=731 == repo version.txt at ship time.
- patch-731.hot.json: {"plugin":"princealirescue","patch":731,"hostVersion":1,"build":38,"sha256":"b1e96ef2..."}; downloaded patches/princealirescue-38.jar (46,758 bytes), sha256 matches hot.json exactly.
- Hot jar = script classes only (Script + $Frame + $Pending), all byte-identical to patch-731.zip.
- RUNNING_BUILD=38 verified via javap (bipush 38 in Script).

## Delta Build 37 → Build 38 (javap -c diff on PrinceAliRescueScript; Plugin/Config unchanged)
- New persisted flag `ashesApproachReloadRecovered` and new phase `RESCAN_AFTER_TREE_APPROACH_RELOAD`.
- New recover for hot reload DURING ASHES_APPROACH_LOG_SOURCE: fires on reload, inspects fresh state — logged-in, varp20, no log 1511, axe+tinderbox present, inside Fred's 16-tile search — then resumes from live tree scan without replaying movement or chop. Lines:
  - `Reload during ASHES_APPROACH_LOG_SOURCE; inspect quest/inventory/scene before resuming`
  - `[PrinceAliRescue] RECOVERED_EXACT_TREE_APPROACH_RELOAD; fresh state confirms logged-in varp20, no log 1511, axe+tinderbox, and inside Fred's 16-tile search; resume from live tree scan without replaying movement or chop`
- This is the right shape for the hot-reload-resets-memory lesson: checkpoint the phase AND re-prove quest/inventory/scene state before resuming, instead of trusting memory-only flags.

## Observations for Alex
- O1: Same 12th-reused commit message again — the message still says nothing about the actual delta (reload-during-approach recover). Consider a one-line real summary per build.
- O2: The recover checks "no log 1511" and axe+tinderbox but not the pending-chop proof (was a tree actually being chopped at reload?). If the reload happened mid-CHOP rather than mid-APPROACH, the phase latch may misclassify; the ASHES gate string in the diag will tell.

## Carried open items
- Build 25 dead-tinderbox-recover STILL OPEN; Build 32 exactExpiredSourceHold unreachable-flag open.

## Live acceptance
- PENDING. Screenshot feed dark since 2026-09-30 17:44 EDT (~20.7h); no live stream URL confirmed. Watch for `RECOVERED_EXACT_TREE_APPROACH_RELOAD` in diag — but note it can only fire on a reload during ASHES_APPROACH_LOG_SOURCE, which may be rare.
