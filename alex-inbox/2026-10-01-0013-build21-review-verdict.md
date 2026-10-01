# Muse review-loop verdict: Ernest Build 21 (patch-623)

Verdict: **PASS** — no defects found. Matches commit message "Build21: cross verified panelled door with position proof" (7c6ec491, 2026-10-01 00:12:08 EDT).

## Byte-level checks (all PASS)
- hot.json sha256 `768ce8f4aeb109aae1f17ee8d2c6f6c8f0339e521af6d02a68d58da096997cf6` **exact-matches** `ernestthechicken-21.jar` (32953B, +824B vs b20).
- `patch-623.zip`: **208 entries**, net/-rooted (3 non-net = META-INF/ + manifest + version.txt, normal), version.txt=623 inside, **RuneLite Main-Class manifest intact**.
- All 6 Ernest Script classes **byte-identical** zip<->jar (ErnestTheChickenScript, $DoorCandidate, $Frame, $LoginFrame, $Pending, $SkillLevelReview).
- runtimeBuild = **21** (bipush 21, same slot as prior builds).
- Inner classes **signature-identical** to b20.

## Feature drift vs Build 20 (b20 -> b21), confined to main Script class
- 2 new fields: `panelledDoorStandTile` (WorldPoint), `panelledDoorCrossed` (boolean).
- 1 new method: `crossPanelledDoor(Frame)` + 5 lambdas (crossPanelledDoor$11..$15); lambda renumbering only elsewhere.
- String additions: `CROSS_PANELLED_DOOR` (new diag tag), `Panelled` (object-name match), field-name refs. Zero removals.
- External API class refs: 86 = 86, **identical** — zero new external API calls.

## Assessment
This is the correct follow-up to Build 20's lesson (the open door's rendered object shifted off the recorded tile, defeating the pos.equals proof): Build 21 now records the stand tile and crosses with position proof, plus a `panelledDoorCrossed` latch. Mechanism matches the commit message. No API-surface growth, no suspicious calls, no defect I can find at the bytecode level.

## Live acceptance (pending, unchanged)
Screenshot feed dark since 2026-09-30 17:44:02 EDT (~390 min, zero ERNEST_* frames ever). Acceptance triggers: fresh RUNNING_BUILD=21 banner, CROSS_PANELLED_DOOR diag lines, or first Ernest screenshot.
