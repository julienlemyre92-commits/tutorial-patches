# Prince Ali Rescue Build 41 review verdict — PASS with findings (Muse read-only, 2026-10-01 14:52 EDT)

Patch-734.zip, commit "Prince Ali Rescue Build41: keeps native reconnect active under exact quest HOLD" (2026-10-01T18:48:03Z; commit message reused verbatim 15th time — now hides a 181-line feature). Build landed ~11s after the 14:47 review-loop run started.

## Chain of custody — PASS
- patch-734.zip: 221 files, net/-rooted (218 net/ + 2 META-INF + version.txt), in-zip version.txt=`734` == repo version.txt at ship time.
- patch-734.hot.json: {"plugin":"princealirescue","patch":734,"hostVersion":1,"build":41,"sha256":"c5a42d4a...c4"}; downloaded patches/princealirescue-41.jar (49,773 bytes), sha256 matches hot.json exactly.
- Hot jar = script classes only (Script + $Frame + $Pending), all byte-identical to patch-734.zip's script classes.
- RUNNING_BUILD=41 verified via javap (bipush 41 at two sites).

## Delta Build 40 → Build 41 (source diff 2189→2370 lines; Plugin/Config unchanged)
New local water-sourcing flow; WATER (1929) no longer goes through GE:
- beginSource now routes id==WATER straight to geStage="WATER_LOCAL_SOURCE" (WATER was GE-eligible PREPARE in Build 40).
- New localWaterSourceTick (~145 lines): buy empty bucket (1925) at Al Kharid General Store waypoint (3315,3175,0) via "Shop keeper" Trade, then find nearest fountain/well/sink (name-contains, plane-matched, Fill/Use action) within 16 tiles; bounded approach (90s total / 30s no-progress / 4 attempts; walkWithStateUntil ≤8s with isMoving gate, post-walk tile re-capture, WATER_SOURCE_APPROACH_RETURN diag); then Fill interact or useItemOnObject(1925, objId). Pending proofs: WATER_SHOP_OPEN/CLOSE, WATER_BUY_BUCKET (empty-bucket +1, coin debit 1..5), WATER_FILL_BUCKET (water +1, empty -1). Six-minute overall bound; one purchase + one fill attempt.
- New recoverObservedUnavailableWaterQuote (~25 lines) + 8 persisted water* state fields (dump/restore).
- Item IDs correct (empty bucket 1925, bucket of water 1929); waypoints correct (Al Kharid general store 3315,3175; palace courtyard fountain 3293,3171).

## Findings
- F1 (medium, dead code): recoverObservedUnavailableWaterQuote is UNREACHABLE. Its gate requires geStage=="PREPARE" plus the GE error string "GE quote unavailable/above 1000gp cumulative cap id=1929 quote=0 deficit=" — but beginSource now routes WATER directly to WATER_LOCAL_SOURCE, so WATER can never enter the GE quote path that produces that error. The waterQuoteRecovered latch, the RECOVER_WATER_FROM_AL_KHARID_SOURCE phase, and the persisted water* fields serve a recovery that cannot fire. Suggest deleting the method + latch, or re-adding a GE-first path if the local flow is meant as fallback only.
- F2 (medium, shared pattern): local shop purchases don't fund coins from the bank. localWaterSourceTick HOLDs "Cannot buy quest-needed bucket: carried coins=N" when inventory coins <2, even if coins sit in the bank — no withdrawDeficit(COINS, 2) attempt (the GE path does withdraw coin deficits). Same pattern in the ASHES tinderbox flow ("Cannot buy required tinderbox: no carried coin"). Suggest a bounded coin withdraw before the HOLD.
- F3 (low, shared pattern): the shop-open branch walks only when distanceTo(waypoint)>8, but walk()'s arrival radius is 10 — a stop at 9–10 tiles skips re-walk while getNearestShopNpc's search radius is unknown; possible "no live Shop keeper" HOLD from just outside interaction range. Suggest aligning the walk check to >10 or confirming the NPC search radius covers 10.

## Observations
- O1: commit message reused verbatim 15th time — now hides a whole new feature (WATER local sourcing). One-line real summary per build would help review keep up.
- O2: "Shop keeper" (spaced) used for both Al Kharid and Lumbridge flows; live verification still needed that the API's name match opens Trade on the real "Shopkeeper" NPC.
- O3: walkWithStateUntil + isMoving gate + post-walk re-capture follows the established (Build 40) pattern; the stale-Frame trap is avoided.

## Carried open items
- Build 39 RECOVERED log line still over-claims "axe+tinderbox" (axe gate removed in Build 39) — Build 41 did not touch it.
- Build 25 dead-tinderbox-recover STILL OPEN; Build 32 exactExpiredSourceHold unreachable-flag open.

## Live acceptance
- PENDING. Screenshot feed dark since 2026-09-30 17:44 EDT (~21h); no live stream URL confirmed. Build 41's water flow (shop buy + fountain fill + proofs) has zero live evidence. Nothing counts as live-confirmed until the feed returns or the game's own state shows it.

Verdict: PASS — custody clean, delta coherent and bounded (F1 dead code is harmless at runtime since unreachable; F2/F3 HOLD with a message rather than failing silently).
