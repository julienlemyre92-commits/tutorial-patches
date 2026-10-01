# Muse review verdict — Ernest Build 24 (patch-626)

Reviewed by Muse (read-only review; Alex owns implementation/releases).
Review time: 2026-10-01 00:27–00:33 EDT. Ship commit 3aafedf (04:26:16Z,
"Build24: approach exact closet door before tube pickup") — atomic single
commit: version.txt=626 + patch-626.hot.json + patch-626.zip + ernestthechicken-24.jar.

## Checks — all PASS

1. SHA chain: hot.json sha256
   ab22feddba07dd055bfa004dcdc8ba9076c8e47d990e45da9fbc30a282dfbff9
   exact-matches the downloaded ernestthechicken-24.jar (33525 bytes).
2. Zip hygiene: 208 entries, net/-rooted (only 3 non-net/ entries:
   META-INF + MANIFEST.MF + version.txt). Main-Class
   net.runelite.client.RuneLite intact — safe overlay.
3. Class identity: all 6 script classes byte-identical jar<->zip
   (Script, $DoorCandidate, $Frame, $LoginFrame, $Pending, $SkillLevelReview).
4. Build marker: BUILD_NUMBER=24 via javap -constants.
5. Plugin/Config overlay classes (ErnestTheChickenConfig, ErnestTheChickenPlugin,
   $1) byte-identical to Build 22 — no changes outside the script.
6. External API surface: 280 distinct external class refs in both builds — zero
   new external API calls.
7. Drift confined to the script, matching the commit message ("approach exact
   closet door before tube pickup"):
   - $Frame gains `closetDoors: List<DoorCandidate>` (the only inner-class
     signature change).
   - New string constants: OPEN_CLOSET_DOOR, "Exact closet-door Open dispatch
     rejected: ", "Closet door is open but rubber tube is not visible; door=",
     "Closet door not observed near tube; refusing blind pickup; doors=",
     "Rubber tube pickup dispatch rejected after closet door verified open",
     "tubeVisible=", "closetDoors=", "after one action; closetDoors=".
   - Removed: the looser Build-23-era string "Rubber tube pickup rejected;
     closet door may be closed" and OPEN_CLOSET — superseded by the
     verified-open / exact-door logic.
   - New `gaugeAndTube$2(DoorCandidate)` lambda; remaining lambdas renumbered
     as expected (drift spans b23+b24 since the nearest local baseline was b22).
8. No defects found. The refusal semantics ("refusing blind pickup",
   "rejecting dispatch unless closet door observed open near the tube") are
   tighter guards, not looser ones — correct direction after the manor
   door-position lessons of Builds 20–22.

## Verdict: ACCEPTED

No action required. Live acceptance still pending — no ERNEST_* screenshots
or diag frames have ever landed; acceptance triggers: fresh RUNNING_BUILD=24
banner, OPEN_CLOSET_DOOR / closetDoors diag lines, or the first Ernest
screenshot (feed dark since 2026-09-30 17:44 EDT).
