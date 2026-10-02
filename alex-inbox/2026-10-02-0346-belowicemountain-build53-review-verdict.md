# Review verdict: Below Ice Mountain Build 53 (patch-918) — PASS WITH FINDINGS

**Reviewer:** Muse (read-only review loop worker), 2026-10-02 03:46 EDT
**Commit reviewed:** 6b60a47c8b "Below Ice Mountain Build53 skill telemetry" (2026-10-02T07:43:15Z)
**Scope:** custody verification + source diff vs Build 52 + API safety. No code shipped, no live test (feed dark ~34h).

## Custody — AIR TIGHT
- version.txt 917 → 918, sequential; commit 6b60a47c8b linear on ddaba973 (no sibling race).
- patch-918.zip: 288 entries; entry list IDENTICAL to patch-917; in-zip version.txt = 918 == repo.
- belowicemountain-53.jar SHA-256 `90677b4ac2695e51cbe2338e26ffa7d5c337852710b5845b7f8c65c3eded102a` == patch-918.hot.json FULL MATCH.
- BUILD_NUMBER = 53 javap-verified.
- Published source (2838 lines) diff vs Build 52: exactly the telemetry method + call site + BUILD_NUMBER 52→53 + `Experience` import. Nothing else touched.
- Independent JDK17 single-file compile: same 2 pre-existing errors as Build 52 (`BelowIceMountainConfig` not part of the single-file source-review publish), zero NEW errors — the telemetry code itself is clean.

## What changed (commit title: "skill telemetry")
Adds `putSkillTelemetry(Properties, Frame)`, called from `writeStatus` (every tick, in the `finally`):
- `prep*` keys: `prepActive` when LOGGED_IN + quest IN_PROGRESS + stage (low-6-bit) == 30 + 0 < maxHp < 20; then `prepActivity`="Training Hitpoints on chickens", `prepSkill`=HITPOINTS, `prepCurrent`=maxHp, `prepTarget`=20, `prepReason`="Preparing for dungeon combat", `prepCurrentXp`=hpXp, `prepTargetXp`=Experience.getXpForLevel(20); plus `skill.HITPOINTS.target`=20 when prep.
- Full skill dump: every `Skill` enum value except OVERALL → `skill.<NAME>.level`/`.xp` via the blocking `Microbot.getClientThread().invoke(Supplier)` (the correct variant), outer + per-skill try/catch, `skillTelemetryError` on failure, early return when not LOGGED_IN.

## Findings
- **BIM53-1 (MINOR, new):** per-tick client-thread round-trip dumping all ~23 skills on every status write (`writeStatus` runs in the tick `finally`, i.e. every ~600ms tick). Bounded and exception-safe, but it fires regardless of need. Consider throttling to prep-state changes or every N ticks. Not a correctness defect.
- **BIM52-1 (DEFECT, open since Build 50): NOT FIXED** — the diff contains zero retreat/bank-recovery logic changes. A farm-threat retreat still targets Lumbridge bank (3208,3220,2) while TRAIN_RETREAT_CLEARED only fires near Falador bank, so the clear never fires and `hold()` latches the error permanently — the bot never resumes. This is the live blocker for Build 53 acceptance: the first training threat ends the run before any telemetry matters.
- Carried: BIM51-1/BIM50-1, BIM49-1, BIM48-1/48-2/48-3, BIM47-1, BIM46-1/46-2, BIM45-2/45-3, BIM44-1..3, BIM43-1..3, BIM42-1/42-2, BIM41-1, BIM40-1..4, bankCoins discrepancy, guardian unimplemented, BIM38-1..3.
- Note: source-review README.md has no Build 53 section (newest section is Builds 50–52) — doc gap only.
- Note: hot.json `sha256` was verified to be jar-level (matches on both 917 and 918); an initial class-level misread was corrected before recording.

**Verdict: PASS WITH FINDINGS.** Feed dark ~34h; live acceptance pending RUNTIME BUILD 53 + prep* keys in status.
