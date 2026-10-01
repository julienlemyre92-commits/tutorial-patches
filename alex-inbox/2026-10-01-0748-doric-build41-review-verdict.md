# Doric Build 41 (patch-681) — read-only review verdict: PASS

Reviewed 2026-10-01 ~07:49 EDT by Muse (read-only reviewer; Doric is Alex-owned — no patches shipped).

**Ship:** patch-681.zip, commit b6154e24 2026-10-01T11:46:07Z
"Doric Build41: use verified Makeover Mage map waypoint".

**Verdict: PASS.**

**Diff 680 -> 681 (byte-level):** 5 doricsquest classes changed; zero
added/removed classes, 200 total, zero signature changes in
Frame/LoginFrame/Pending (recompiled only — renumbered constant pool from
the field rename, member-identical). Delta confined to
`routeTinMineViaSouthGate`'s mage-absent fallback:

- Field `CRAFTING_GUILD_APPROACH` -> `MAKEOVER_MAGE_WAYPOINT` (WorldPoint);
  static waypoint changes **(2933,3289,0) -> (2918,3322,0)**, per commit
  message the wiki-verified Makeover Mage map coordinate.
- Log lines: `TO_TIN_MINE_CRAFTING_GUILD_APPROACH` -> `TO_TIN_MINE_MAGE_WAYPOINT`;
  `TIN_MINE_MAGE_NOT_LOADED ... decision=WALK_TO_CRAFTING_GUILD` ->
  `decision=WALK_TO_WIKI_MAGE_COORDINATE` (+ `WALK_WIKI_MAGE_COORDINATE`
  variant); HOLD string now "Makeover Mage 1306 is not live near verified
  map coordinate ...".
- Semantics unchanged: mage live -> walk to observed NPC tile
  (TO_TIN_MINE_SOUTH_GATE); mage absent -> walk to static waypoint, arrival
  <=6 tiles -> phase TO_TIN_MINE_SOUTH_GATE, else terminal HOLD.
- `BUILD_NUMBER`/`runtimeBuild()` correctly **41** (bipush 41); Plugin build 41.
- **Zero new net/runelite/api references** anywhere in the delta (javap -c
  verified); all API usage identical to 680.

**Hygiene:** 215-entry net/-rooted zip, version.txt-in-zip=681==repo at
ship time, no patch number reuse, MANIFEST convention holds.

**Hot chain VERIFIED:** patch-681.hot.json sha256 ==
doricsquest-41.jar (29,705B) exact; all 4 script classes byte-identical to
the zip's.

**Acceptance:** RUNNING_BUILD=41 with matching class SHA, or the new
runtime lines `TO_TIN_MINE_MAGE_WAYPOINT` / `TIN_MINE_MAGE_NOT_LOADED ...
WALK_TO_WIKI_MAGE_COORDINATE`.

**Caveats:** the (2918,3322) coordinate's wiki-verification is per the
commit message — not independently verifiable from here; the in-game
`waypoint={}` log line is the ground truth. No repo-side live evidence:
screenshot feed dark since 2026-09-30 17:44:02 EDT (~14h); zero DORIC_*
frames ever — live acceptance rests on Alex's direct runtime reports.
