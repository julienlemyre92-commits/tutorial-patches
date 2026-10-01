# Prince Ali Rescue Build 42 review verdict — PASS with findings (Muse read-only, 2026-10-01 14:59 EDT)

Patch-735.zip, commit "Prince Ali Rescue Build42: keeps native reconnect active under exact quest HOLD" (2026-10-01T18:53:02Z; commit message reused verbatim 17th time — now hides a water-flow recovery change). Build landed ~10s after the 14:52 review-loop run finished.

## Chain of custody — PASS
- patch-735.zip: 221 files, net/-rooted (217 net/ + 2 META-INF + version.txt), in-zip version.txt=`735` == repo version.txt at ship time. Manifest is the real RuneLite manifest (Main-Class net.runelite.client.RuneLite), not a jar-default.
- patch-735.hot.json: {"plugin":"princealirescue","patch":735,"hostVersion":1,"build":42,"sha256":"d11a70bd…5d5c0d2"}; downloaded patches/princealirescue-42.jar (50,025 bytes), sha256 matches hot.json exactly.
- Hot jar = script classes only (Script + $Frame + $Pending), all three byte-identical to patch-735.zip's script classes (cmp).
- RUNNING_BUILD=42 verified via javap (bipush 42 at the banner site) — banner honest this build.

## Delta Build 41 → Build 42 (source diff 2370→2384 lines; Plugin/Config unchanged)
One targeted change: a one-shot recovery for the exact water-source HOLD, plus a widened candidate scan.
- New `recoverObservedWaterFountainWithoutDirectAction` (~12 lines) hooked into the HOLD recovery chain (after recoverObservedUnavailableWaterQuote). Fires when phase=HOLD, error starts with "No live Fill/Use fountain, well, or sink found in Al Kharid courtyard;", sourceItem==WATER, sourceGoal==1, geStage=="WATER_LOCAL_SOURCE", LOGGED_IN, varp==20, plane 0, player ≤4 tiles from ALKHARID_PALACE_COURTYARD, exactly 1 empty bucket (1925), 0 water (1929). Clears HOLD, sets phase="RESUME_BUCKET_USE_ON_OBSERVED_FOUNTAIN" (diag-only, no consumer), one-shot latch `waterCandidateHoldRecovered` (persisted to state).
- REMOVED the Fill/Use-action filter from the localWaterSourceTick candidate scan — candidates are now name-matched (fountain|well|sink), plane-matched, sorted by distance within 16 tiles. The fill dispatch already falls back to `Rs2Inventory.useItemOnObject(EMPTY_BUCKET, objId)` when no "Fill" action exists; status string updated to "Use bucket on live water-source object". Coherent pair with the recovery: the HOLD fired precisely because the action filter found nothing.
- `waterCandidateHoldRecovered` added to the state dump/restore alongside `waterQuoteRecovered`.

## Findings
- F1 (low, new): the recovery predicate verifies courtyard proximity + inventory, but NOT that any fountain/well/sink TileObject is actually in the streamed scene — the log line claims "fresh scene/inventory confirms" while only inventory is confirmed. If the HOLD fired because the courtyard genuinely has no water source rendered (scene-stream gap), the recovery burns its one-shot latch, resumes, and re-HOLDs the same error — now unrecoverable. Bounded and safe, but the one retry may be spent on a genuinely empty scene. Suggest adding a name-match scene check to the gate, or logging the nearby-object survey (the HOLD message already lists it) in the RECOVERED line for diagnosis.
- O1: commit message reused verbatim 17th time — the message now describes a 14-line water recovery, not anything about native reconnect. One-line real summary per build would keep review (and Alex's own history) honest.
- O2: phase name "RESUME_BUCKET_USE_ON_OBSERVED_FOUNTAIN" is diag-only (no consumer) — consistent with how phase is used elsewhere; fine, just noting the "OBSERVED" in the name refers to inventory/position observation, not a fountain observation.

## Carried open items (unchanged)
- Build 41 F1 (medium): recoverObservedUnavailableWaterQuote STILL UNREACHABLE (geStage=="PREPARE" gate; WATER routes direct to WATER_LOCAL_SOURCE). Dead method + waterQuoteRecovered latch persist in Build 42.
- Build 41 F2 (medium): "Cannot buy quest-needed bucket" HOLDs at line ~1193 when carried coins <2 — still no coin withdraw from bank before the HOLD. Shared with the ASHES tinderbox flow.
- Build 41 F3 (low): shop-open branch walks only when distanceTo(waypoint)>8 vs walk()'s arrival radius 10 — unchanged.
- Build 39 RECOVERED log over-claim ("axe+tinderbox"); Build 25 dead-tinderbox-recover; Build 32 exactExpiredSourceHold — all still open.

## Live acceptance
- PENDING. Screenshot feed dark since 2026-09-30 17:44 EDT (~21h); no live stream URL confirmed. Build 42's fountain recovery + widened scan have zero live evidence. Nothing counts as live-confirmed until the feed returns or the game's own state shows it.

Verdict: PASS — custody clean (jar SHA-verified, banner honest), delta small, targeted, and bounded (one-shot latch, phase display-only, no hot-reload state hazards). New findings are observations, not blockers.
