# Prince Ali Rescue Build 36 review verdict — PASS (Muse read-only, 2026-10-01 14:21 EDT)

Patch-729.zip, commit "Prince Ali Rescue Build36: keeps native reconnect active under exact quest HOLD" (2026-10-01T18:17:22Z; commit message reused verbatim 10th time).

## Chain of custody — PASS
- patch-729.zip: 221 files, net/-rooted (206 class entries), in-zip version.txt=729 == repo version.txt at ship time, genuine RuneLite client manifest.
- patch-729.hot.json: {"plugin":"princealirescue","patch":729,"hostVersion":1,"build":36,"sha256":"e00f1ab4..."}; downloaded patches/princealirescue-36.jar (46,480 bytes) and verified sha256 == hot.json value exactly.
- Hot jar carries script classes only (PrinceAliRescueScript + $Frame + $Pending), consistent with hot-reload scope; all three byte-identical to patch-729.zip.
- RUNNING_BUILD=36 verified via javap (bipush 36 in Script BUILD_NUMBER/banner path and Plugin), replacing 35.

## Delta Build 35 → Build 36 (javap -c diff on PrinceAliRescueScript; Plugin/Config unchanged)
- New persisted flag `ashesTreeApproachFailureRecovered` and new phase `RESCAN_AFTER_BLOCKED_TREE_APPROACH`.
- New one-shot recover: on the "Walker made no tile progress toward live regular tree at ..." hold, fresh state check → exclude the stalled live tree id@tile → rescan another candidate. Ack line: `[PrinceAliRescue] RECOVERED_EXACT_TREE_APPROACH_HOLD; excluded stalled live tree id={} tile={} after fresh state check; rescan another candidate player={}`.
- This answers Build 35's O2 (rejection set quest-lifetime/monotonic): Build 36 excludes only the exact stalled tree after a fresh check rather than relying on the carried rejection set.

## Observations for Alex
- O1: Build 37 (patch-730, shipped ~1.5 min later) is a marker-only re-ship — zero logic delta vs 36. No evidence given for why the bump was needed; if hot reload missed 36 the host log should show it.
- O2: `ashesTreeApproachFailureRecovered` is persisted — a one-shot that never fires this session stays armed next session; check it is tied to the current pending-proof lifecycle.

## Carried open items
- Build 25 dead-tinderbox-recover STILL OPEN; Build 32 exactExpiredSourceHold unreachable-flag open.

## Live acceptance
- PENDING. Screenshot feed dark since 2026-09-30 17:44 EDT (~20.6h); no live stream URL confirmed. Watch for `RECOVERED_EXACT_TREE_APPROACH_HOLD` in diag before claiming the recover works.
