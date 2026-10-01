# Doric Build 40 review verdict — PASS with one concrete defect (Muse read-only review)

Reviewed 2026-10-01 07:44 EDT. Alex owns Doric's Quest implementation/releases; this is
read-only review. Build 40 = patch-679.zip, commit 040da3db 11:41:46Z
"Doric Build40: route to verified south approach waypoint". Repo version.txt=679 == version.txt
inside the zip (no version reuse). Zip: 215 entries, net/-rooted, META-INF/MANIFEST.MF
byte-identical to patch-678 (220B, real RuneLite manifest — same packaging as builds 37-39).

## Byte-level delta vs patch-678 (all 200 classes compared)
- 196 classes byte-identical. Only changed: DoricsQuestScript, DoricsQuestScript$Frame,
  DoricsQuestScript$LoginFrame, DoricsQuestScript$Pending.
- $Frame/$LoginFrame/$Pending: no member-level diff (javap -p) — constant-pool renumbering only.
- Script logic additions: new static waypoint `CRAFTING_GUILD_APPROACH = (2933, 3289, 0)`;
  `routeTinMineViaSouthGate` fallback branch: Makeover Mage NPC null / no worldLocation /
  plane != 0 → if player >6 tiles from the waypoint: log
  `[DoricsQuest] TIN_MINE_MAGE_NOT_LOADED player={} approach={} decision=WALK_TO_CRAFTING_GUILD`,
  walk with pending label `TO_TIN_MINE_CRAFTING_GUILD_APPROACH` (arrival ≤6 tiles resolves to
  phase TO_TIN_MINE_SOUTH_GATE); if within 6 tiles: HOLD
  "Makeover Mage 1306 is not live near verified Crafting Guild waypoint; cannot verify
  south-gate approach". Mage live on plane 0 → Build-39 live-NPC-tile walk unchanged.
- Self-heal on upgrade: if currently HELD with Build-39's exact string
  "Makeover Mage 1306 is not live in the scene; cannot verify south-gate approach" →
  clears hold, sets phase=TO_TIN_MINE_SOUTH_GATE, logs
  `TIN_MINE_MAGE_NOT_LOADED decision=WALK_VERIFIED_CRAFTING_GUILD_WAYPOINT`. The new
  Build-40 hold string does NOT match the recovery prefix → terminal if it fires again
  (intentional single-recovery, no loop).
- +0 new game-API classes; only WorldPoint.<init> (+1) and WorldPoint.distanceTo (+1) —
  expected for the new waypoint.

## DEFECT (concrete): build marker not bumped — ships as "Build 39"
Verified in the shipped classes:
- `DoricsQuestScript.BUILD_NUMBER` ConstantValue = **39**
- `DoricsQuestScript.runtimeBuild()` returns **39** (bipush 39)
- Startup banner prints `[DoricsQuest] RUNNING_BUILD=39` (bipush 39)
- `DoricsQuestPlugin.build` = **39** (constructor bipush 39; Plugin.class byte-identical to 678)
- `guardLegacyHostReload` compares the persisted "build" property against bipush **39**

Consequences: (1) the live-acceptance trigger "RUNNING_BUILD with matching class SHA" can
never show 40 — reviewers must accept Build 40 on the NEW runtime lines only
(`TIN_MINE_MAGE_NOT_LOADED ... WALK_TO_CRAFTING_GUILD` / the new HOLD string), never the
banner; (2) `guardLegacyHostReload` will treat Build-40 code as the same build as 39 when
a persisted "39" action snapshot exists, defeating the cross-build restore guard.
Fix (Alex): bump the marker to 40 in DoricsQuestScript (BUILD_NUMBER, runtimeBuild,
RUNNING_BUILD site, guardLegacyHostReload comparison) and DoricsQuestPlugin.build.

## Non-blocking observations
- Arrival proof is `distanceTo((2933,3289,0)) <= 6` evaluated BEFORE the walk is issued;
  the walk target is the exact waypoint tile. (2933,3289) is the Crafting Guild entrance
  area; if that exact tile is a door/wall tile the dispatch could reject it — acceptance
  watches for the live WALK_TO_CRAFTING_GUILD lines or a "Walk dispatch rejected" hold.
- Replacing the live-NPC anchor with a static verified waypoint is the right robustness
  direction (removes the NPC-streaming-lag dependency flagged in the Build-41 review note).

## Verdict: PASS (code sound; defect is diagnostic-only, does not change behavior)
Live acceptance pending: `TIN_MINE_MAGE_NOT_LOADED` runtime lines or the new HOLD string.
Screenshot feed dark since 2026-09-30 17:44 EDT (~14h) — acceptance rests on Alex's
runtime reports; zero DORIC_* frames ever captured.
