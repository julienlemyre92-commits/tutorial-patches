# Review verdict: Below Ice Mountain Builds 16/17 (patches 881/882) — READ-ONLY

Reviewed by: Muse (read-only; Alex owns implementation/releases; no ship)
Build 16: commit 7ea7bec46d ("Below Ice Mountain Build16 stage 15 to 30 integration", landed 03:48:44Z)
Build 17: commit 3da47a6f51 ("Below Ice Mountain Build17 packed stage bit mask", landed 03:51:00Z)
Verdict: **PASS WITH FINDINGS**

## Custody — AIR TIGHT
- Version sequence 880→881→882, all sequential, no reuse, no overwrite of any patch-N.zip.
- patch-881 hot.json `sha256: 02d40360b6027d5f5d6e32607d7467c6d52b5995b38c7fda24561d3bd8ef7ef5` == actual `patches/belowicemountain-16.jar` bytes (git blobs API) — FULL MATCH.
- patch-882 hot.json `sha256: 2e9ad64ded0a30ee89705ade30f0b9f62eccc72271f7821627d00305da46be7e` == actual `patches/belowicemountain-17.jar` bytes — FULL MATCH.
- Script-only jars hold exactly 7 class entries (BelowIceMountainScript + nested FlexView, Frame, Pending, Proof, Route, $1) — no stale-class overlay risk.
- `javap -constants`: BUILD_NUMBER=16 / BUILD_NUMBER=17 in the respective compiled classes — no lying banners.
- patch-881.zip / patch-882.zip: 277 entries each, net-rooted, in-zip `version.txt` = 881/882 == repo `version.txt` == patch number. 7 BIM classes in each zip are byte-identical zip<->jar (0 mismatches).
- Single-purpose commits; each touches only jar + hot.json + zip + source-review + version.txt.

## Diff vs Build 15 (published source)
1. **Build 16** (651→836 lines): stage 15→30 integration —
   - `recruitCrew`: Marley varbit (BIM_MARLEY) 0/5/35 talk gates, 10/15/20/30 steak-sandwich feed via `Rs2Inventory.useItemOnNpc` (ITEM_USED_OR_DIALOGUE proof); sandwich crafting alternates knife-on-bread / knife-on-meat after one unproved attempt (code comment cites the QuestHelper-vs-Wiki disagreement); Burntof varbit (BIM_BURNTOF) 0/5/10/15: intro, Asgarnian ale feed (coins<3 → hold), RPS talk.
   - `followWillow` for stages 20/25 (Talk-to Willow at DUNGEON_WILLOW).
   - `enterRuins` for stage 30: control.properties arming gate (allowDungeonEntry + PID + build + class SHA), verifies Enter/Climb-down action from object composition before clicking, 45s DUNGEON_ENTERED proof (stage>=35 + y>5000).
   - Stage ≥35 → HOLD_GUARDIAN_UNIMPLEMENTED (pillar/guardian route still staged, not implemented).
2. **Build 17**: exactly 2 lines — BUILD_NUMBER 16→17, and `questStage(int raw)` `raw & 0xff` → `raw & 0x3f` ("packed stage bit mask"). Behaviorally identical on the only witnessed packed value (40970 = 0xA00A → 10 under both masks) and on all documented stages 0–40; a protective change on the unproven assumption that low-byte bits 6–7 are packed flags.
3. Source-review READMEs for both builds are still the stale Build-1 copy (carried INFO pattern).

## Design rationale — sound
- Talk-to keys ("marley:intro", "burntof:rps", …) fall under the 2-attempt `laterActionCounts` budget → terminal HOLD with a named cause, while "dialogue:" prefixed continuations stay exempt — matches the carried attempt-budget discipline.
- DIALOGUE_CHANGED proof reads varp/checkal/marley/burntof plus inDialogue/hasContinue/dialogue-text/options — robust against the old dialogue-text-unreadable class.
- `recruitCrew` completion predicate is varbit-driven (marley==40 && burntof==40) with a "VERIFY_BOTH_CREW_RECRUITED" stage that waits on BIM_MAIN→20 — observed-state gated, per architectural law.
- The dungeon-entry arming gate (control file + PID + build + SHA) is the right shape for an untested traversal.

## Findings
- **LOW BIM16-1**: dungeon coordinates are unverified. `questDungeon` = stage≥30 && x 2900–3050 && y 5000–6000 plane 0, and DUNGEON_ENTERED requires y>5000. No wiki/live provenance in the source. If the Camdozaal ruins are instanced (the Atlas scene proved instances live at 12000+ with 12806→12867 drift), these gates mis-fire into "Unmapped location" holds. Stage 30+ is not yet reached live, so this is pre-live.
- **LOW BIM16-2**: Stage ≥35 → HOLD_GUARDIAN_UNIMPLEMENTED — the pillar-route/guardian decision is explicitly staged. Fine as staging; the pillar route is still the plan's unproven core.
- **INFO BIM16-3**: the Marley sandwich direction alternation (bread→meat after 1 unproved attempt) is bounded by the 2-attempt budget → terminal HOLD if both fail. Diagnosable; fine.
- **INFO BIM17-1** (supersedes BIM9-1): `& 0x3f` vs `& 0xff` — no observable difference on any documented stage (0–40 cap). If the low byte ever carries genuine stage data in bits 6–7 (stage ≥64), this mis-parses; no evidence of that. Correct-but-unproven.
- **INFO BIM16-4 (carried pattern)**: stale Build-1 README copy in source-review/belowicemountain-build16 and build17.
- Carried: LOW BIM2-2 (mining-10 gate — now a HOLD "Mining 10 required for safe pillar strategy"), LOW BIM7-1/BIM7-2 (route-stall Manhattan gate), INFO BIM2-4 (issue() accepted unused), INFO BIM14-1 (flexScrolls memory-only).

Live acceptance: PENDING. Screenshot feed dark ~30h (newest upload 2026-09-30 17:44 EDT); no live stream URL confirmed; no RUNTIME BUILD 16/17 lines observed this run. No shipping action taken — read-only.
