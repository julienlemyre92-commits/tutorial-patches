# Review verdict: Below Ice Mountain Build 90 — death recovery handoff (patch-952)

- verdict: PASS WITH FINDINGS
- scope: read-only review of commit `48942846` (version.txt 951 -> 952; patches/patch-952.zip, patches/belowicemountain-90.jar, patches/patch-952.hot.json, source-review/belowicemountain-build90/{BelowIceMountainScript.java,README.md})
- published by: Muse review loop, 2026-10-02 ~07:16 EDT
- custody (independently verified this run, not taken on trust):
  - in-zip `version.txt` = `952`, matches repo version.txt and patch name (no reuse of 951)
  - zip root is `net/` (313 entries, no depth bug like patches 341/342)
  - `belowicemountain-90.jar` sha256 == hot.json sha256 (`edebb53c…ba731a3d38`)
  - `BUILD_NUMBER=90` in source, matches patch name and hot.json build
  - source is the 4312-line BelowIceMountainScript.java with the new `DeathRecoveryBridge.Owner` wiring (lines ~454-482, 835-843, 2657-2690)

## What changed (vs BIM B89 + 07:06 death-recovery candidate verdict)
This ship is the quest-half Owner wire-up that the 07:06 verdict's F3 called "the actual deployment." BIM Build 90 implements `DeathRecoveryBridge.Owner` and registers it per tick once preflight passes and the player is logged in (line 882), before any dungeon entry.

- F4 (static pre-death manifest) — ADDRESSED: the Policy manifest is built LIVE from the frame's inventory + equipment (`ensureDeathRecoveryRegistered`, lines 2657-2690), excluding coins and food. The 07:06 concern that "nothing proves the manifest matches what was actually carried" is gone — it is read, not guessed.
- F3 (no Owner wire-up) — ADDRESSED: `requestYield`/`isQuiescent`/`officeFeeQuote`/`recoveryFinished` all implemented; tick gate at line 835 cancels routes on yield, holds on `WAIT_DEATH_RECOVERY_YIELD`, parks on `HOLD_DEATH_RECOVERY` for STOPPED outcomes, and resumes via `RECHECK_SUPPLIES_AFTER_DEATH` on clean outcomes. `isQuiescent` requires all route workers dead — correct yield hygiene. `shutdown()` unregisters only when `!mustYield()`, consistent with F5's noted lease-retention design.
- F1 (no Rs2Death fee quote) — ADDRESSED BY REFUSAL: `officeFeeQuote()` returns `OptionalInt.empty()` with a comment: "Never authorize an unknown fee from an estimate or stale label." The office reclaim path is now unreachable instead of logout-terminating. Residual (by design): a no-grave death far from the safe exit still ends in a forced logout outcome — safe, but it is the ceiling of what this handoff can recover.
- F2 (grave capacity / no make-room) — PARTIALLY ADDRESSED: Policy signature is still `(critical, safeExitPoint, predicate, feeCap)` with no capacity field and no drop/bank make-room step. Mitigating reality: post-death inventory is empty (everything went to the grave), so `GRAVE_CAPACITY_LOW` at reclaim time is unlikely; a full inventory at reclaim still dead-ends into HOLD. Low severity, not a blocker.
- Manifest exclusions: coins and food are excluded from the critical manifest. Food is consumable (correct), coins exclusion means lost coins are not reclaimed — fine given F2P grave economics, worth one diag line in the live-validate checklist.

## Findings
F1 — README.md is the stale Build-1 handoff doc (mentions belowicemountain-plugin-1.jar, Build-1 SHAs, "Build 2" route research). It was copied verbatim under source-review/belowicemountain-build90/ and now actively misdescribes the shipped build. Doc hygiene only — fix by replacing with a Build-90 handoff note (Owner wiring, empty office quote, manifest exclusions, safeExit (3222,3217,0)).

F2 — `META-INF/MANIFEST.MF` is again byte-identical to the official microbot jar manifest (Main-Class RuneLite), same note as the 07:06 verdict: procedurally wrong per the zip-with-`zip` rule (patch zips must not carry a manifest), benign in this instance only. Recurring pattern — worth one line in the ship checklist.

F3 — `ensureDeathRecoveryRegistered` builds the manifest once at registration, but the quest continues to bank/food-manage through stages 10-35 afterwards; items picked up AFTER registration (stage-35 quest items, extra food) are not in the manifest. If death occurs late in the run, the reclaim may under-claim. Consider re-registering (unregister + register) at stage-35 dungeon entry, or documenting the manifest as pre-dungeon-supplies-only. Not a blocker for the first live drill.

## Non-issues checked
- `safeExit` WorldPoint(3222,3217,0) passed to Policy; bridge treats it as the walk-to-safety anchor — matches the script's existing Lumbridge-bank-floor conventions.
- feeCap=1000 is logged at registration (`DEATH_BRIDGE_REGISTERED critical=... feeCap=1000`); with empty quote the cap is unreachable — harmless.
- `DeathRecoveryBridge.mustYield()` is checked at both line 687 (guardianActive gate) and the tick head — the quest cannot enter the guardian fight while a recovery is pending.
- Register happens once (`deathRecoveryRegistered` latch); a failed `register()` calls `hold()` — visible, not silent.
- No sibling race this run: HEAD `48942846` was independent-API-verified before review; nothing shipped over it during review.

## Recommendation
Ship nothing new. Live-validate the recovery drill on the live client: force or observe a real death (training phase is the natural candidate), and watch for the NEW diag lines `DEATH_BRIDGE_REGISTERED critical=... feeCap=1000`, `WAIT_DEATH_RECOVERY_YIELD`, and either `RECHECK_SUPPLIES_AFTER_DEATH` or `HOLD_DEATH_RECOVERY` — acceptance comes only from those lines plus the post-death state, never the Build-90 banner alone. Feed is still dark (last screenshot 2026-09-30 17:44 EDT), so this is pending visual evidence like everything else.
