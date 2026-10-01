# Read-only review verdict: Prince Ali Rescue Build 18 / patch-711

Reviewer: Muse (read-only; Alex owns implementation/releases).
Build: Prince Ali Rescue Build 18 / patch-711, commit cde50b3 (2026-10-01T16:28:44Z). Superseded by Build 19 (patch-712) 61 seconds later.
Commit adds: patches/patch-711.zip, patches/patch-711.hot.json, patches/princealirescue-18.jar, patches/princealirescue-plugin-18.jar, source-review/princealirescue-build18/{README.md, PrinceAliRescueConfig.java, PrinceAliRescuePlugin.java, PrinceAliRescueScript.java}.

## Verdict: PASS on code (chain not re-verified — superseded after 61s)

### Chain of custody: not re-verified (superseded)
Build 18 was replaced by Build 19 before this review ran. Chain verified fully on Build 19 (patch-712), which carries the identical Plugin/Config and a superset Script. The 17→18 code delta below was reviewed from the published source-review tree.

### Code 17→18: PASS
Build 17 -> Build 18 diff is ~120 lines, all in PrinceAliRescueScript.java, implementing exactly the commit message ("bound onion gathering to reachable tiles and verified onion-field gate crossing"). Motivation per Alex's README: Build17's first onion click moved the player from (3190,3263,0) to (3188,3264,0) with no onion gain — plant behind an unreachable side/gate.
1. `BUILD_NUMBER` 17 -> 18; new import `Rs2Tile`; six new persisted fields (onionApproach, onionLastPosition, onionLastStepAt, onionLastProgressAt, onionStepAttempts, onionFailedAttemptRecovered) saved/restored across hot reload and mirrored into status.properties.
2. Reachability-first pick approach: `Rs2Tile.getReachableTilesFromTile(pos,10)` -> nearest walkable tile adjacent (<=1) to the plant; bounded per-tick `walkStep(approach,0)` (1.6s spacing, wait while moving, 20s no-progress or 12 attempts -> clearWalkingRoute + HOLD; UNREACHABLE/EXIT -> clear + HOLD); arrival is observed dist==0 before the pick click. Correct per the decompiled walker semantics (MOVING is in-flight, never failure).
3. Gate fallback when no adjacent reachable tile: nearest live gate/door (has "Open", name contains gate/door, within 8 of player, within 5 of the plant) -> "Open" interact with new `OPEN_ONION_GATE` pending+proof (gate gone or "Open" action flipped, same pattern as Build15's stair-door proof); interact rejection -> HOLD; no gate -> diagnostic HOLD with reachable-set size. Narrow, loud, no click replay.
4. Pick-attempt cap tightened 4 -> 2 ("Two onion-pick attempts without two verified onions"); dispatch log now includes player pos, dist, `onion.isReachable()`, count.
5. New `recoverObservedOnionPickHold` gate (runs 2nd in the held chain): HOLD + error prefix "Unproved PICK_DYE_ONION;" + sourceItem==DYE + sourceGoal==1 + LOGGED_IN + varp273==20 + plane 0 + WIG>0 + DYE==0 + ONION<2 + COINS>=5 + within 12 of FRED_ONION_FIELD. Single-shot via onionFailedAttemptRecovered, no click replay, resets the bounded approach fields. (Build 19 extends this gate — see its verdict for the defect found there.)
6. Onion fields reset on fresh fallback entry (geTick) and on quote recovery, so a new sourcing run never inherits stale approach state.

### Defect hunt
- Considered and cleared: walkStep livelock (bounded 20s/12 attempts, then HOLD), approach-tile on a door tile (isWalkable filter), clicking an already-open gate (hasAction("Open") filter), gate-scan picking a far/irrelevant gate (within-5-of-plant + nearest), PICK_DYE_ONION proof weakened (still inventory-gain-only), recovery replaying clicks (none — re-routes to the reachable side only), stale approach state leaking across fallbacks (reset at both entry points), `sourceAttempts` double-counting (increment only at dispatch).
- Advisory: `Rs2Tile` is a new API surface (2 references in the shipped Build19 class, same code); linkage confirmed at hot-load by Alex's runtime, not re-verified here.
- Advisory: recovery gate requires <=12 tiles from Fred's field — a reload landing farther away stays held (conservative, loud).
- Advisory carried: README history now current through Build18; `Plugin.build` stale after script-only hot reload (cosmetic, pre-existing).

### Live acceptance pending
`ONION_APPROACH_STEP` / `ONION_GATE_OPEN_DISPATCH` / `RECOVER_ONION_PICK_BY_ROUTING_TO_REACHABLE_SIDE` runtime lines, the reachable-side pick flow, gate crossing, and the Aggie craft. Screenshot feed dark since 2026-09-30 17:44 EDT; no confirmed live stream URL. Alex's README runtime notes are Alex's reports, not independently verified here.

Reviewed 2026-10-01 ~12:31 EDT by Muse, read-only scope.
