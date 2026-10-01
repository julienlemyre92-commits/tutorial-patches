# Verdict: Ernest the Chicken Build 19 (2026-10-01 00:04-00:10 EDT)

Read-only inspection of patch-621 (`patches/patch-621.zip`, `patches/ernestthechicken-19.jar`, `patches/patch-621.hot.json`). No edits, no deployment, no game input.

- IDENT: hot.json = {plugin: ernestthechicken, patch: 621, build: 19, hostVersion: 1, sha256: fb29b697e5d8a74e20fed0902a7a9ded6507af8c08721b485dfbbc839f61dcd5} — EXACT match of ernestthechicken-19.jar (31980B, +834B vs build 18).
- ZIP: patch-621.zip, 208 entries, all class paths under net/ root, META-INF/MANIFEST.MF intact (Main-Class: net.runelite.client.RuneLite), version.txt=621 inside.
- IDENTITY: all 6 Script classes (ErnestTheChickenScript + Frame/Pending/LoginFrame/SkillLevelReview/DoorCandidate) byte-identical zip<->jar. Plugin/Config zip-only = expected hot-reload split.
- MARKER: runtimeBuild() = bipush 19, ireturn (same slot as build 18).
- DRIFT (620->621, javap -p -c + string diff): confined to the main Script class. New fields `panelledDoorOpenVerified:Z` and `panelledDoorTile:LWorldPoint`; new method `openPanelledDoor(Frame)V`; new log strings OPEN_PANELLED_DOOR (x3) and action "Open". Matches commit 04:04:50Z "Build19: verify panelled door before closet route".
- INNER CLASSES: Frame/Pending/LoginFrame/SkillLevelReview/DoorCandidate signatures identical (modulo Compiled-from header).
- EXTERNAL SURFACE: zero new external API calls — the only refs delta is the two own-class fields + one own-class method. No new microbot-base.jar / net.runelite surface.
- HYGIENE: version sequence 619->620->621 clean; no pre-existing ernest19 jar; no duplicate patch-621 paths. (patch-622.zip / ernestthechicken-20.jar already staged in patches/ but version.txt still 621 = Build 20 mid-ship, NOT reviewed here.)
- DEFECTS: none. PASS.

- LIVE ACCEPTANCE PENDING: screenshot feed dark since 2026-09-30 17:44:02 EDT (~386 min, zero ERNEST_* frames ever); Build 19 acceptance triggers = fresh RUNNING_BUILD=19 banner, OPEN_PANELLED_DOOR diag lines, or first Ernest screenshot.
