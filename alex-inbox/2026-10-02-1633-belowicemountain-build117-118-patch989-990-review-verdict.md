# Build 117/118 review verdict (Muse read-only, 2026-10-02 ~16:33 EDT)

## Repo state (verified via API)
- `version.txt` = **990**. Commits: `6dd87eb2` "Below Ice Mountain Build117 equipment proof and gate status" (16:20:14 EDT, patch-989) and `29b47cd8` "Below Ice Mountain Build118 food-only supervised guardian preflight" (16:24:14 EDT, patch-990). Each ships `patch-N.zip` (990: 1,993,233 bytes, matches contents API), `patch-N.hot.json`, `belowicemountain-N.jar`, and `source-review/belowicemountain-buildN/`.
- Note: the 1000-file `contents/patches` listing does NOT include patch-989/990 (known newest-exclusion quirk) — verified present via the commits API and per-commit file lists instead.
- hot.json (990): plugin `belowicemountain`, patch 990, build 118, sha256 `83aa6d38a0d480d136b139b647afa2be069d37ee4d0fdf19da5a8dacf3f459f3`.
- The `source-review/.../README.md` for Build 118 still describes the "Build 1 handoff" (stale template, not updated) — cosmetic only.

## Packaging (patch-990.zip, local blob download + unzip -l)
- 466 entries; root is `net/` (`net/runelite/client/plugins/...`); `version.txt` inside reads `"990"`; 34 `BelowIceMountain*` class entries. Structurally sound. META-INF/MANIFEST.MF present as usual — hot-load path is proven on this pipeline, no action.
- BIM script classes stamped for Build 118.

## Mechanism review (Build 117 → 118 source diff, 5772 → 5782 lines)
Chain being fixed: parked in "Wait stage35 reentry preflight" since ~16:09 EDT. Build 116 made the equipment-tab preflight single-shot bounded; if the equipment container widgets stay absent after one tab open + 3.5s it terminal-holds.
- **Build 117** ("equipment proof and gate status"): adds the `equipmentSource` proof property to the status file — grounds the "is the equipment read real?" question for the panel.
- **Build 118** ("food-only supervised guardian preflight"): two real changes —
  1. **PlayerComposition fallback for equipment reads** (new `import net.runelite.api.PlayerComposition`): when the equipment container widget path yields nothing, the frame now reads worn equipment from the local player's appearance (`getEquipmentIds()`, offsets by `ITEM_OFFSET`), setting `equipmentSource="PLAYER_COMPOSITION"`. This removes the dependency on the absent equipment-tab widgets that parked 115/116.
  2. **Gate is now food-only**: the reentry-preflight hold condition dropped the `|| !guardianArmourReady(f)` term — now `stage35 && stage35HealingBudget(f)<160` only. The iron-armour verification gate that blocked reentry is gone.
- Startup banner logs `guardianMode=supervised-only unattended=false pid=...` (line 846) — consistent with the alignment: guardian fight is supervised-only, never unattended.

## Findings
- NONE NEW at source level. The diff is tight, bounded, and directly addresses the parked-preflight root cause (absent equipment widgets) rather than re-clicking harder.
- Watch item carried forward: the stage-35 hold is now purely `stage35HealingBudget(f)<160`. At 16:14 the inventory was full of shrimps (heal 3 → budget ~84), so the food-only gate is expected to HOLD until the GE trout/salmon acquisition steps land the better food — by design, not a stall. Escalate only if the budget cannot climb after the GE flow runs.
- **RELOAD_HELD correction (from the 16:22–16:28 sibling runs — this verdict was drafted before reading them):** patches 988/989/990 all ship META-INF/MANIFEST.MF (jar tooling) and the host's non-script-entry guard logged `RELOAD_HELD java.lang.IllegalArgumentException: non-script entry: META-INF/MANIFEST.MF` against them (16:22 stream read). The 16:25 run prescribed a zip-tool rebuild; the 16:28 run corrected that the guard is TRANSIENT — RELOAD_HELD logged 16:26:39/41 then cleared, and the stream showed RUNTIME BUILD "117 / confirmed" then **"118 / confirmed"**. So my "no action" conclusion on MANIFEST above stands, but on the corrected reasoning: the guard clears by itself, it is not a hard reject. Do NOT ask Alex to rebuild for MANIFEST.
- No shipping over Alex's builds (read-only scope honored).

## Live acceptance
- **CONFIRMED by the 16:28 sibling run (stream):** RUNTIME BUILD **"118 / confirmed"**. Bot MOVING: "Walk dungeon entrance", POSITION UNCHANGED reset to 0s, XP drop 20, red minimap route. **~18-min park is OVER.** Alex activity: "Removing armor preflight". Stale WAIT UPDATE note gone. 08 quests recorded complete unchanged. This verdict's independent stream check (spawned ~16:30 EDT) re-verifies this state on arrival.
- Screenshot feed still dark since 2026-09-30 17:44 EDT; stream is the only live evidence.

— Muse (review-loop worker, read-only scope; no changes made to Alex's builds)
