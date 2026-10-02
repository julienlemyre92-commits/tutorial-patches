# Review verdict: death-recovery candidate (patch-951 / source-review/death-recovery-candidate/)

- verdict: PASS WITH FINDINGS
- scope: read-only review of the new candidate commit `dafaa63d` (version.txt 950 -> 951; patch-951.zip + 4 source-review files)
- published by: Muse review loop, 2026-10-02 ~07:06 EDT
- custody (independently verified this run, not taken on trust):
  - in-zip `version.txt` = `951`, matches repo version.txt (sha 56f639a0) and patch name
  - `META-INF/MANIFEST.MF` in the zip is byte-identical to the official microbot jar manifest (Main-Class RuneLite) — procedurally wrong per the zip-with-`zip` rule, benign in this instance only
  - zip root is `net/` (312 entries, no depth bug like patches 341/342), plus the two root extras above
  - compiled `questcommon`/`deathplugin` classes javap-match the reviewed sources (Owner interface, QuestDeathRecovery.Stage enum, Bridge signatures) — classes ARE the sources

## Findings (Alex owns implementation/releases; none of these ship anything)

F1 — Office path cannot accept until the fee quote exists. `Owner.officeFeeQuote()` has no Rs2Death backing (INTEGRATION.md already admits "no public Death's Office fee quote"). As written, a no-grave death walks to the F2P office, waits 15s at OFFICE_QUOTE for a callback no quest implements yet, then `fail("OFFICE_FEE_UNPROVED")` -> forced LOGOUT when far from respawn/safeExit. Safe-by-design, but the office reclaim path is unproven end to end and currently terminates in logout. Also INTEGRATION.md says office entry/fee behavior "still require live validation on this private server" — flagging the wording; if this means a private test server, say so, otherwise it reads odd for OSRS.

F2 — Grave helper cannot make room. The plugin constructs `QuestDeathRecovery.Plan(..., minEmptySlots=1, ...)` hardcoded; `DeathRecoveryBridge.Policy` has no capacity field. Any manifest larger than current free slots hits `GRAVE_CAPACITY_LOW` -> `failToSafety` -> EXIT -> HOLD, and a HOLD outcome releases nothing (bridge YIELD stays set, quest paused indefinitely, STOPPED requires plugin restart). Honest diag, but a multi-item grave recovery with a full inventory is a guaranteed dead end with no make-room (drop/bank) step.

F3 — Integration wiring gap. BIM Build 89 (the current live script) does not implement `DeathRecoveryBridge.Owner` or register — shipping patch-951 is inert-by-default (plugin logs "no registered quest owner; no input taken" on death, takes no input). That matches the "inert by default" contract, but 951 alone changes nothing live; the quest-half wire-up is the actual deployment.

F4 — Grave-branch pre-death manifest is static, not proved against `lastHealthy`. The plugin logs `lastHealthy.items` vs post-death items but never uses it; the manifest comes from policy, which the quest must hand-review. Fine if the manifest is genuinely complete, but there is no runtime proof the manifest matches what was actually carried. INTEGRATION.md's owner contract covers this in prose ("caller-reviewed manifest"); nothing more to do statically.

F5 — `DeathRecoveryPlugin.shutDown()` deliberately retains the active lease; re-enable resumes the scheduler mid-stage (YIELD/GRAVE/OFFICE...). Intentional per the comment, but it means a disable/re-enable cycle does NOT abort a recovery — worth one line in the live-validate checklist, not a defect.

## Non-issues checked
- `begin()` misuse throws IllegalStateException; call path (CHOOSE, from safeTick) routes through `catch (Throwable)` -> `fail("ENGINE_EXCEPTION")` -> quest stays yielded (release=false). Safe.
- `finish()` lease accounting: release=true clears YIELD only when `REGISTERED.get() == expected`; STOPPED_* keeps the quest paused. Correct.
- `cancelAndQuiesce` re-enters the grave monitor from the scheduler thread that holds it (reentrant) — no deadlock path.
- Route workers are daemon, cancellable via the Rs2Walker BooleanSupplier honoring interrupt; `stopRoute()` join(3000) before every fail/finish.
- Respawn guard in WATCH correctly avoids the death-animation window (hp>0 + moved-off-death-point required); Build85-style gear-retained death returns NOTHING_TO_RECLAIM without any input.

## Recommendation
Ship nothing new. Accept the candidate into source-review only (already the case). Before any deployment: implement + live-validate the UI-proved `officeFeeQuote()` (F1), add a capacity/make-room answer to the grave path (F2), and land the quest-half Owner wire-up (F3) with a no-owner death drill proving the "no input taken" inert path.
