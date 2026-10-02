# Review verdict: Misthalin Mystery Builds 31/32/33/34/35 (patches 818/819/820/821/822) — 2026-10-01 20:57 EDT

Scope: read-only review. Alex owns Misthalin Mystery implementation and releases.

## Verdict: PASS WITH FINDINGS (all five)

### Custody (all five airtight)
- hot.json sha256 == misthalinmystery-N.jar (downloaded bytes via blobs API, base64-decoded): `c79e5e24…`(31), `cea01baa…`(32), `9ec4636a…`(33), `5577e272…`(34), `c5686d23…`(35) — all FULL MATCH.
- patch-818/819/820/821/822.zip: 258 entries each, net-rooted (only `META-INF/`, `META-INF/MANIFEST.MF`, `net/`, `version.txt` outside `net/`); in-zip `version.txt` = 818/819/820/821/822.
- `MisthalinMysteryScript.class` byte-identical zip↔jar for all five.
- `BUILD_NUMBER` = 31/32/33/34/35 via `javap -p -constants` on shipped classes.
- Plugin/Config byte-identical B30→B35; single-purpose commits; genuine RuneLite MANIFEST.MF.
- Note: patch-815 is Build 28's hot.json (sibling-verified); Build 30 = patch-817, custody re-verified independently this run (`0ebc8523…` FULL MATCH).

### Delta B30→B31 (diagnostic-only, 9 lines)
- New `candleVarbitLayout` diagnostic: reads `VarbitComposition` (index, LSB, MSB) for MISTMYST_CANDLE1–4 → status.properties `candleVarbitLayout`, plus `rawVarpMisthalin` and `mainVarpId`. Zero stage-logic change. This probe produced the layout knowledge B32 acts on.

### Delta B31→B32 (the packed-varp fix, 16 lines)
- `f.rawVarp = getVarpValue(MISTMYST_MAIN)` (packed); `f.varp = f.rawVarp & 0xff` — stage is bits 0–7, candle varbits 4039–4042 use bits 8–11. All stage comparisons now run on the masked stage byte.
- Audit: every `f.varp` comparison constant is < 256 (0, 10, 15, 20, 30, 35, 40, 45, 50, 60, 65, 70, 120); the only packed-value site (`"Unmapped Misthalin Mystery varp 2098"`) correctly uses `f.rawVarp`. Safe.
- Bonus: the mask also fixes two latent misfires — the `varp>=120` dialogue-"yes" branch no longer fires at packed 2098 (stage 50 + candle bit 11 = 2048), and the stage-progress comparison (`f.varp > p.before.varp`) now compares stages, not packed values.
- New `PACKED_CANDLE_STAGE_PROVED` recovery: clears `"Unmapped Misthalin Mystery varp 2098"` when `rawVarp==2098 && varp==50 && CANDLE4==1 && exactly 1 tinderbox && full HP && island()`.

### Delta B32→B33 (diagnostic-only, 11 lines)
- New `doorProbe`: at `varp==60`, scans tile objects within 2 of RUBY_DOOR, captures `"id@tile name [actions]"` (1200-char cap) → status.properties. Zero stage-logic change; same fail-closed pattern as B29's `shelfProbe`.

### Delta B33→B34 (recovery gate, 6 lines)
- New `EXPLOSION_STAGE_PROVED`: clears `"Object absent LEAVE_EXPLOSION_ROOM"` when `varp>=65` (masked) `&& quest==IN_PROGRESS && full HP`.

### Delta B34→B35 (behavioral at varp 65, 14 lines)
- Stage-65 route: when inside (`!outside()`) with `x∈[1642,1644]` and `y>4833`, issues `DIRECT_SOUTH_CORRIDOR` `walkFastCanvas` to `(1643, max(4833, y-3))` instead of `CLIMB_DAMAGED_WALL`.
- New `BYPASS_LOCKED_NORTH_DOOR` gate: clears `"Route CLIMB_DAMAGED_WALL no position progress"` when `varp==65 && pos==(1643,4839) && full HP && !inCombat`. Coherent pair: `(1643,4839)` satisfies `y>4833`, so the tick after the clear walks south down the corridor. Bounded (y decreases toward 4833, then normal wall logic resumes); no loop.

### Findings
- [info NEW] B35-1: `BYPASS_LOCKED_NORTH_DOOR` is exact-tile + full-HP + `!inCombat` — narrow by design, fail-closed.
- [info NEW] B34-1: `EXPLOSION_STAGE_PROVED` is not single-shot latched (same as the B30/B32 gates) — re-clear possible if the hold re-arms, but each clear requires fresh observed proof; fail-safe.
- [info NEW] B32-1: the `&0xff` mask changes every stage read at once — audited safe (all constants < 256, packed site uses `rawVarp`); this is the correct fix for the packed-varp bug class.
- [info NEW] B21–B35 commit messages are identical boilerplate ("capture visible dialogue widgets to distinguish identical cutscene pages") — unusable for change tracking; the `source-review/` dirs are the real record.
- Carried: **D28-1 STILL OPEN** (verified in B35 source: `"aat:"` has no producer — the only occurrence is the `startsWith` check; the `RUBY_DOOR_DEFINITION_FALLBACK_ONCE` gate is still dead code), D28-2, D30-1, D27-1, D27-2, D16-1, D16-2, D14-1, D12-1, D6-1, README drift, D3-2, mirror telegraph, FINISHED silent clear.

### Live acceptance pending
Expect `RUNNING_BUILD=31..35`, `PACKED_CANDLE_STAGE_PROVED` / `EXPLOSION_STAGE_PROVED` / `BYPASS_LOCKED_NORTH_DOOR` / `DIRECT_SOUTH_CORRIDOR` lines, `doorProbe` + `candleVarbitLayout` in status.properties. Screenshot feed dark since 2026-09-30 17:44 EDT (~27.2h); no live URL.

Nothing shipped (review-only; Alex's releases).
