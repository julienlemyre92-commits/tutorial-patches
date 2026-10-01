# Read-only review verdict: Prince Ali Rescue Build 15 / patch-708

Reviewer: Muse (read-only; Alex owns implementation/releases).
Build: Prince Ali Rescue Build 15 / patch-708, commit 129af20a (2026-10-01T15:50:25Z). Repo version.txt=708.
Commit adds: patches/patch-708.zip, patches/patch-708.hot.json, patches/princealirescue-15.jar, patches/princealirescue-plugin-15.jar, source-review/princealirescue-build15/{README.md, PrinceAliRescueConfig.java, PrinceAliRescuePlugin.java, PrinceAliRescueScript.java}.

## Verdict: PASS (no concrete defects)

### Chain of custody: PASS
- patch-708.hot.json sha256 `9eea1150869eee996388df43766e820a12d28cf7fd294149d3d4195dbfe76e9e` == downloaded princealirescue-15.jar (31,590 bytes), verified locally.
- All three in-zip script classes (PrinceAliRescueScript / $Frame / $Pending) byte-identical between patch-708.zip and the hot jar (sha256-checked per file).
- patch-708.zip: 221 files, 218 `net/`-rooted entries; in-zip version.txt=708 matches repo version.txt; META-INF/MANIFEST.MF is the genuine client manifest (Main-Class: net.runelite.client.RuneLite). No `jar cf` contamination.
- Fresh patch number 708; not uploaded over an existing patch; no version reuse.

### Code: PASS
Build 14 -> Build 15 diff is 49 lines, all in PrinceAliRescueScript.java (Plugin.java byte-identical to Build 14's, so the overlay Plugin.class can differ only in the inlined BUILD_NUMBER, the established pattern):
1. `BUILD_NUMBER` 14 -> 15.
2. New narrow recovery branch `exactStairRouteHold`: resumes only when phase=HOLD and error starts with exactly `"Walker exited before reaching live castle stair;"`. That prefix matches Build 14's producer string at source line 1141 byte-for-byte (`hold(f,"Walker exited before reaching live castle stair; id="+...)`). It joins the existing Build 13/14 gates with the same observed-state evidence (sourceItem==WOOL, goal 3, LOGGED_IN, varp273=20, balls 1-2, rawWool==0, shears, plane 1 near wheel); no click is replayed; timer/attempts reset, then "inspect route doors and approach live stairs before another click".
3. New live-door scan before the bounded stair approach: queries the installed tile-object cache within 5 tiles of the player, same plane, live "Open" action, name contains door/gate (case-insensitive), and `door.distanceTo(stairTile) <= player.distanceTo(stairTile)+1` (lies toward the known stair, not away). Opens the nearest such door with one `click("Open")` and sets a 9s `WOOL_OPEN_STAIR_DOOR` pending proof; a rejected click HOLDs with door id/tile/name (no blind replay). If no door qualifies, control falls through to the existing bounded approach — no behavior change there.
4. New proof for `WOOL_OPEN_STAIR_DOOR`: `findObjectByLocation(p.target)` returns null OR the object no longer has the "Open" action. This covers both the door-open action flip (Open->Close) and object replacement — the standard door-open proof, consistent with how the walker identified the blocker (object 1543 at (3207,3214,1)).

### Defect hunt
- No concrete defects found. The recovery prefix cannot drift (exact-string gate on the producer).
- The name filter is generic (any door/gate), but it is conjunctively gated by same-plane, within-5-of-player, live "Open" action, and toward-the-stair geometry — bounded enough for the observed blocker (closed door id 1543 on the stair route).
- Advisory only: `Plugin.build` remains stale in a running instance after a script-only hot reload (pre-existing across Builds 7-15, cosmetic; acceptance already rests on runtime lines, never the banner).
- Note: the published README.md is still the Build 1 handoff for its historical sections; the Build 15 section at the bottom is accurate.

### Live acceptance pending
`WOOL_STAIRS_ROUTE_DOOR_DISPATCH` / `WOOL_OPEN_STAIR_DOOR` proof / `RECOVERED_WOOL_DESCENT_HOLD` (STAIR_ROUTE_HOLD variant) / post-door descent still need Alex's runtime lines. Screenshot feed dark since 2026-09-30 17:44 EDT; no confirmed live stream URL. Same pending acceptance as Builds 12-14.

Reviewed 2026-10-01 ~11:53 EDT by Muse, read-only scope.
