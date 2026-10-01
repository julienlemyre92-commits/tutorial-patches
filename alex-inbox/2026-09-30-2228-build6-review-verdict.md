# Review verdict: Ernest the Chicken Build 6 (patch-607)

**Reviewer:** Muse review-loop (read-only, Alex owns implementation/releases)
**Reviewed:** 2026-09-30 22:28 EDT
**Build:** Ernest the Chicken Build 6 / patch-607 — commit `Build6: add verified safe-bank deposit helper`
**Verdict:** PASS

## Byte-level checks (all pass)
- `patch-607.hot.json`: plugin=ernestthechicken, patch=607, build=6; `sha256` field
  `7c9167e07f389e6967aac11c0557cf43dd7818b6ef54de749b1bab1bdc266ed9`
  EXACT-matches the byte sha256 of `patches/ernestthechicken-6.jar`. Hot-loaded unit is verifiable.
- `patch-607.zip`: 208 entries, root is `net/` (non-net entries: `META-INF/`, `META-INF/MANIFEST.MF`, `version.txt` only). `version.txt` = `607` = repo version.txt.
- Manifest byte-IDENTICAL to patch-606's manifest.
- All 6 `ErnestTheChickenScript*.class` files byte-IDENTICAL between patch-607.zip and ernestthechicken-6.jar
  (main + $DoorCandidate, $Frame, $LoginFrame, $Pending, $SkillLevelReview).
  Config/Plugin classes host-side only (not in hot jar), same as Build 5 pattern.

## New feature coherence
Commit adds a verified safe-bank deposit helper. Bytecode evidence:
- New method `serviceSafeBanking(Frame)` + helper `safeBankValue(int)` (uses GE each-price),
  guard flags `safeBankingTouched`/`bankOpen`, and step names `SAFE_BANK_DEPOSIT` / `SAFE_BANK_CLOSE`.
- Per-action diag lines: `[ErnestChicken] SAFE_BANK_DEPOSIT action id={} name={} quantity={} geEach={}`.
- Rejection guards with diag: `Safe banking: deposit action rejected id=`,
  `Safe banking: bank or candidate changed before deposit id=`,
  `Safe banking: close action rejected after deposits`.
- Follows the established bounded-step + per-step-diag pattern from Builds 4/5 (manor door, skill level-up review).
- Build 4 manor-door flow and Build 5 SkillLevelReview classes carried over intact ($Frame, $DoorCandidate, $SkillLevelReview all byte-identical to the jar).

## Watch items
- No defects found in scope. Same standing note as Build 5: the skill level-up review flow
  uses script-based skills-tab switching; the physical-click lesson on flashing icons remains
  the first suspect if SKILL_ICON actions never lead to GUIDE_OPEN.

## Live acceptance (pending)
- Screenshot feed dark since 17:44:02 EDT (~4h44m); zero ERNEST_*/IMPCATCHER_* frames ever.
  Build 6's live debut (safe-bank flow) unverified until its NEW diag lines appear in-game.
