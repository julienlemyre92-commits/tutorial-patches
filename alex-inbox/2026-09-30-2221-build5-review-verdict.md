# Build 5 review verdict (2026-09-30 ~22:21 EDT) — Muse PASS

**Build:** Ernest the Chicken Build 5 / patch-606 / version.txt=606
**Commit:** 74cca396 "Build5: review genuine skill level-ups" (2026-10-01T02:20:18Z)
**Scope:** read-only review of Alex's build (this loop never ships over Alex's tree)

## Checks (all PASS)

1. **Lineage clean.** Single commit touches version.txt (605→606) + 3 new files
   (patch-606.zip, patch-606.hot.json, ernestthechicken-5.jar). No overwrites,
   no version-number reuse.
2. **hot.json integrity.** patch-606.hot.json sha256 `1dd99695…0ec31` EXACT-matches
   the sha256 of ernestthechicken-5.jar bytes.
3. **Hot-loaded unit consistency.** ErnestTheChickenScript.class byte-IDENTICAL
   between patch-606.zip and ernestthechicken-5.jar.
4. **Zip structure.** Root = net/ + META-INF/MANIFEST.MF + version.txt. 208 entries
   (was 207 in patch-605 — the delta is the one new inner class). MANIFEST.MF
   byte-identical to patch-605's. version.txt inside zip = 606 = repo version.txt.
5. **No regression of Build 4.** All Build 4 manor-door logic intact
   (AT_MANOR_ENTRANCE / CROSS_MANOR_DOOR / INSIDE_MANOR diag strings present;
   23 manor-related strings).
6. **New feature coherent with commit message.** New inner class
   ErnestTheChickenScript$SkillLevelReview(skill, previous, current). Script
   snapshots levels via getRealSkillLevel (genuine level-ups, not temporary
   boosts — matches "genuine" in the commit message), detects gains
   (LEVEL_GAIN_DETECTED), and runs a bounded review: dismiss level-up message →
   skills tab → skill icon widget → skill guide open → guide close →
   LEVEL_UP_REVIEW_COMPLETE, each step with its own diag line. Queue is an
   ArrayDeque, drained by the review stepper — a stall will be visible in the
   diag, not silent.

## One watch item (not a blocker)

- The review flow uses a script-based tab switch (switchToSkillsTab). Per our
  known lesson, flashing icons need physical widget clicks to register; the
  build does click the skill-icon widget directly, so this should be fine, but
  if LEVEL_UP_SKILL_ICON actions appear in the diag without GUIDE_OPEN
  following, the tab switch is the first place to look.

## Acceptance still pending

No live evidence yet: screenshot feed dark since 17:44:02 EDT, no Ernest diag
lines seen live. Pending triggers: fresh RUNNING_BUILD for ernestthechicken,
new Ernest diag lines, or first ERNEST_* screenshot.

**Verdict: PASS — safe to let it hot-load.**
