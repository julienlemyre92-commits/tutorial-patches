# Doric Build 18 (patch-656) -- review verdict: PASS

- **Reviewer:** Muse review-loop (read-only; Alex owns implementation/releases)
- **Reviewed:** patch-656.zip (Build 18, commit 09:34:11Z); version.txt=656 at review time. Shipped ~1 min after this run's version.txt poll -- reviewed immediately (no race: read-only, no repo writes of my own pending).
- **Method:** blobs API download of patch-655.zip + patch-656.zip (812,544 / 813,670 bytes), unzip -l (215 entries, net/-rooted, version.txt=656 in+out), javap -p -constants/-c diff of script+plugin+inner classes vs Build 17. Only 5 classes differ (Plugin, Script, Frame/LoginFrame/Pending are structurally identical members; inner classes byte-identical in members).

## Delta: Build 17 -> Build 18 -- closed-door route recovery

- In the hot-reload restore path: if `phase.equals("HOLD") && error.startsWith("Walk dispatch rejected TO_DORIC target=")` -> held=false, phase=ROUTE_DOOR_RECOVERY, then calls the new `recoverRouteDoor(Frame)`.
- New `recoverRouteDoor(Frame)`:
  - `routeDoorRecoveries >= 2` -> terminal HOLD ("Route door recovery limit reached; stopping after two verified attempts").
  - Finds nearest closed door within 5 tiles of the frozen frame pos, filtered by 4 predicates (forward-of-travel, "Open" action present, etc.) and sorted by distance-to-DORIC_HUT; none found -> HOLD ("Walker hit a route barrier, but no forward closed door is visible within five tiles").
  - One `Rs2GameObject.interact(door, "Open")` -> increments counter, logs `[DoricsQuest] ROUTE_DOOR_OPEN_SENT attempt={} id={} at={} name={}`, sets OPEN_ROUTE_DOOR pending (9s, door id, door WorldPoint).
- `proved()` extended for OPEN_ROUTE_DOOR: object at door's location is null OR no longer has the "Open" action -> true. Genuine observed-state proof (action-list change), not a timer.
- `BUILD_NUMBER` 17->18; RUNNING_BUILD banner is parameterized (driven by the constant). Banners honest.

## Notes
- The recovery is narrowly scoped: it fires only on the explicit "Walk dispatch rejected TO_DORIC" hold, not on general walker stalls; budget is 2 verified attempts then terminal HOLD.
- Carry-forward unchanged: routeDoorRecoveries is memory-only (hot reload resets it), but the terminal HOLD persists and the Build-17 narrowed un-hold gates (zero-progress WAIT_FREE_WORLD_LIST only) do not touch this path, so a reload after the limit can retry at most the same bounded sequence, ending again in HOLD -- no unbounded recovery loop.
- Informational carry-forwards: MANIFEST.MF present in zip; manifest sha256 vs patch mismatch (per hot.json).
- No new game-API calls beyond the already-in-use Rs2GameObject calls; zero new phases except ROUTE_DOOR_RECOVERY.

## Verdict
**PASS** -- deliberate, bounded, proof-gated door recovery for the TO_DORIC walk-dispatch stall; packaging sound; banners honest. No live verification yet: screenshot feed dark since 2026-09-30 17:44 EDT (~11h50m at review), zero DORIC_* frames ever. Watching for RUNNING_BUILD=18 banner, ROUTE_DOOR_OPEN_SENT, TO_RIMMINGTON_MINE proof.
