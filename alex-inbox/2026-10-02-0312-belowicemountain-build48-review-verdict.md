# Review verdict: Below Ice Mountain Build 48 (patch-913) — PASS WITH FINDINGS

**Reviewer:** Muse (read-only review loop worker), 2026-10-02 03:12 EDT
**Commit reviewed:** 8cc78f45 "Below Ice Mountain Build48 skill-guide acknowledgment" (2026-10-02T07:11:07Z)
**Scope:** custody verification + source diff vs Build 47 + API safety. No code shipped, no live test (feed dark ~33.5h).

## Custody — AIR TIGHT
- version.txt 912 → 913, sequential; commit 8cc78f45 is linear on 4bf3c2c (no sibling race).
- patch-913.zip: 288 entries, net/ root (285 net/ + META-INF/, MANIFEST.MF, version.txt); in-zip version.txt = 913 == repo version.txt.
- Entry-list delta vs patch-912: exactly ONE added class — `BelowIceMountainScript$LevelUpTabCue$SkillIncrease.class` (the new queued level-up event record). No removals.
- belowicemountain-48.jar SHA-256 `9fd7b53c2fb4ef013a28139a9f4050574d3405209e8cca249d0c3021d7402ad9` == patch-913.hot.json sha256 FULL MATCH.
- BUILD_NUMBER = 48 via javap on shipped class (matches build tag).
- Byte-level class diff patch-912 vs patch-913: every non-belowicemountain class BYTE-IDENTICAL (0 of ~250 differ); only belowicemountain script classes changed.
- Published source (source-review/belowicemountain-build48/BelowIceMountainScript.java, 2783 lines) vs Build 47 source (2536 lines): diff fully accounted (see below).
- Independent compile (JDK 17.0.20.1+1, -encoding UTF-8, against installed microbot-base.jar + sibling BelowIceMountainConfig.class as compile-only stub): CLEAN, 0 errors, 18 classes; shipped `LevelUpTabCue$Result` enum javap-matches source (12 values in order); all new constants (`InterfaceID.SkillGuideV2.FRAME/CLOSE`, `SkillGuide.WINDOW/CLOSE`, `WidgetIndices.SkillsTab.*_CONTAINER`) resolve in the base jar.

## What changed (commit title: "skill-guide acknowledgment")
1. **Skill-guide acknowledgment state machine:** `LevelUpTabCue` grows from a Skills-tab switch into a full sequence: QUEUED → skills tab (safe-gated) → READY_SKILL (click the leveled skill widget via `WidgetIndices.SkillsTab`) → WAITING_GUIDE_PROOF (skill guide V2 or legacy) → READY_CLOSE (click CLOSE) → READY_INVENTORY (switch back) → PROVED. Every wait state has a bounded timeout (3000/3500ms) and degrades to READY_INVENTORY/REJECTED with a `LEVEL_UP_ACK_UNVERIFIED` warn. Back-to-back level-ups queue in an `ArrayDeque<SkillIncrease>`; the whole queue (plus `guideSeen`, `actionAt`) persists across hot reload, and a Build-47 snapshot (PROVED without `guideSeen`) re-acknowledges its last skill exactly once.
2. **Bounded dungeon scene probe:** when `questStage>=35` OR (`==30` + in known cave instance + `dungeonEntryAllowed`), the script runs `dungeonProbe()` — one-shot scene log (guardian NPC 10654, pillar objects 41446/41458/41459/41460 with menu actions, hp, food, aggressor) then `Rs2Player.logout()`. The `safetyLogoutIssued` gate precedes the probe gate, so the probe fires at most once per persisted state; after logout the tick holds ("review scene before re-entry"). On relog the same gate holds ("did not change game state within 12 seconds") — parks, no logout loop.
3. **Entry-flow tightening:** `dungeonEntryAllowed` now uses `entryFoodCount(f)<10` (was `f.food<10`); `DUNGEON_ENTERED` proof now requires `inKnownCaveInstance` (was Y>5000); unapproved entry parks in new `WAIT_DUNGEON_ENTRY_APPROVAL`; a rejected `entrance:` action now holds explicitly instead of falling through.
4. New status props: `levelUpGuideSeen`, `levelUpInventorySelected`, `dungeonProbeLogged`, `dungeonProbeScene`, `safetyLogoutIssued/At`, `entranceAt`.

## Findings
- **BIM48-1 (INFO):** After the safety-logout + relog, the hold message ("Safety logout did not change game state within 12 seconds") is misleading — the logout DID work; the bot is just parked awaiting review. No in-plugin resume path; requires Alex's next build or a manual state clear. Safe direction (parks, never re-enters).
- **BIM48-2 (INFO):** The `questStage>=35` probe branch does not require `inKnownCaveInstance`; a stage-35+ read outside the cave would still probe+logout. Conservative/safe direction; no live impact expected.
- **BIM48-3 (INFO):** README is still the Build-1 handoff doc (changelog stale since ~Build 23).
- Carried: BIM47-1 (interior reposition gets ~1 tick of route flight before HOLD cancels it), BIM46-1/46-2, BIM45-2/45-3, BIM44-1..3, BIM43-1..3, BIM42-1/42-2, BIM41-1, BIM40-1..4, bankCoins discrepancy, guardian unimplemented, BIM38-1..3.

**Live acceptance pending (feed dark):** `LEVEL_UP_TAB ... result=READY_SKILL/WAITING_GUIDE_PROOF` lines, `DUNGEON_PROBE_SCENE` / `DUNGEON_PROBE_LOGOUT_REQUEST`, RUNTIME BUILD 48.
