# Muse read-only review: Prince Ali Rescue Build7 / patch-700 (VERDICT: PASS)

Reviewed commit 82d27224 (2026-10-01 15:16:06Z / 11:16:06 EDT), version.txt=700. Read-only review; no edits or releases by Muse.

## Chain of custody: PASS
- patches/patch-700.hot.json sha256 == actual patches/princealirescue-7.jar: 88f653269d850493b69e474091aee778cbf43c317ce1cb826360548d581b3a34
- In-zip PrinceAliRescueScript.class contains Build 7 markers (RECOVERED_WOOL_STAIRS_ROUTE_HOLD, WALK_DISPATCH, "false is not route failure")
- patch-700.zip: 221 entries, 6 princealirescue entries all net/-rooted; in-zip version.txt=700 == repo version.txt
- BUILD_NUMBER=7 in source; Build6->7 delta is exactly: banner bump, walk() rework, recoverObservedWoolStairRouteHold()

## Code review: PASS (correct application of verified walker semantics)
Build 7 implements the decompiled Rs2Walker semantics correctly: installed walkTo(WorldPoint) is true ONLY for ARRIVED; false collapses MOVING (transient entry gate), UNREACHABLE, and EXIT. Build6's false->HOLD turned a normal in-progress route into a terminal stop (the live wool-stairs HOLD). Build7:
- walk(): latches WALK_<action> pending (20s deadline) for EITHER boolean result; acceptance only from later observed position. WALK_DISPATCH log line per dispatch.
- Timeout path: routeFailures<2 -> ROUTE_RESCAN (bounded retry); then hold("Unproved WALK_..."). Genuine unreachable routes still terminate in HOLD. No infinite walk loop.
- recoverObservedWoolStairRouteHold(): one-shot migration clearing ONLY the exact Build-6 HOLD (phase==HOLD, exact error string, sourceItem==WOOL, LOGGED_IN, varp273==20, raw wool carried, plane 0, within 8 tiles of CASTLE_STAIRS_GROUND (3204,3207,0)). After clear, routing re-derives from observed state (varp==20 -> prepare -> wool path). Narrowly gated; no stale-click replay ("The next tick must still observe and click the live stair").

## Advisories (non-blocking)
1. WALK_ proof predicate is weak: proved() accepts ANY player movement OR already-within-8-tiles as proof of a WALK_ pending -- movement != arrival. Harmless in practice: the step model re-evaluates each tick and re-issues walk() while still >4 tiles out (blocking walkTo, no overlap), so it costs one extra dispatch, not a lost route. Weak-proof early-accept is by design per the README ("preserving the existing bounded rescan/HOLD on no progress").
2. RESUME_WOOL_AT_OBSERVED_STAIRS phase label is write-only (never routed on) -- cosmetic, same pattern as Build5's intercept label. Routing goes through sourceTick.
3. WALK_DISPATCH logs pre=f.pos captured at tick entry (pre-walk tile) -- cosmetic stale-Frame trap noted in the Build 6 review; affects only the log line.
4. Carried forward from Build 6: canShear reads NPC.getComposition() off the client thread (Build-517 crash precedent). Untouched by Build7; still the top watch item.

## Live acceptance: PENDING
Feed dark since 2026-09-30 17:44:02 EDT; no confirmed live stream URL. Acceptance needs fresh runtime lines: WALK_DISPATCH (returned=false with later PROVED), or RECOVERED_WOOL_STAIRS_ROUTE_HOLD. No live evidence available to this review.
