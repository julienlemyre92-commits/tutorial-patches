# Review verdict: Below Ice Mountain Build 49 (patch-914) — PASS WITH FINDINGS

**Reviewer:** Muse (read-only review loop worker), 2026-10-02 03:13 EDT
**Commit reviewed:** e3b0b96c "Below Ice Mountain Build49 safe-bank skill acknowledgment" (2026-10-02T07:12:03Z)
**Scope:** custody verification + source diff vs Build 48 + API safety. No code shipped, no live test (feed dark ~33.5h).

## Custody — AIR TIGHT
- version.txt 913 → 914, sequential; commit e3b0b96c is linear on 8cc78f45 (56s later; no sibling race).
- patch-914.zip: 288 entries; entry list IDENTICAL to patch-913 (no adds/removes).
- belowicemountain-49.jar SHA-256 `be0ceb23e3b41a03d30257eba718130401a16e0f6250938d66fd4df7f6052184` == patch-914.hot.json sha256 FULL MATCH.
- BUILD_NUMBER = 49 via javap on shipped class (matches build tag).
- Byte-level class diff patch-913 vs patch-914: ONLY the outer `BelowIceMountainScript.class` changed; every other class (all non-BIM + all BIM inner classes) byte-identical.
- Published source diff vs Build 48: exactly 2 lines — `BUILD_NUMBER` 48→49, and one condition change (below). Independent JDK17 compile CLEAN (same harness as Build 48).

## What changed (commit title: "safe-bank skill acknowledgment")
The level-up cue's `safe` gate in `tickLevelUpCue` drops the `!trainingRetreat` requirement. Level-ups are now acknowledged through the full Build-48 skill-guide sequence even while the bot is in training retreat. The remaining gate is still strict: LOGGED_IN + overworld prep area + scene stable ≥500ms + no pending/route/cancellingRoute + no prepNativeTick + no dialogue/continue/options + bank/shop/GE/production closed + no interacting NPC + no aggressor + hp > max(6, maxHp/2) + !safetyLogoutIssued + welcomeAttempts==0.

## Findings
- **BIM49-1 (INFO):** Widening the cue to training retreat means a level-up mid-retreat can briefly interleave skills-tab clicks into the retreat sequence. Bounded (one action per tick, REJECTED on any timeout) and re-entrant; the retreat's own guards (route!=null, dialogue, bankOpen) still suppress engagement while it is actually moving/banking. No concrete defect.
- Carried: BIM48-1/48-2/48-3, BIM47-1, BIM46-1/46-2, BIM45-2/45-3, BIM44-1..3, BIM43-1..3, BIM42-1/42-2, BIM41-1, BIM40-1..4, bankCoins discrepancy, guardian unimplemented, BIM38-1..3.

**Live acceptance pending (feed dark):** RUNTIME BUILD 49.
