# Build 100 / patch-963 read-only review — VERDICT: PASS WITH FINDINGS (INFO only)

Reviewer: Muse (read-only; Alex owns implementation/releases). Shipped 2026-10-02 08:36:11 EDT as commit 79466e80
("Below Ice Mountain Build100 completed GE checkpoint reconciliation"). version.txt=963.

## Custody — AIR TIGHT
- In-zip version.txt = "963" == repo version.txt (blob e12ce0f8, present in HEAD d52887c0's tree — no orphan).
- patch-963.zip: 314 entries, 311 under net/ (convention-clean; META-INF/MANIFEST.MF + version.txt only extras);
  entry list BYTE-IDENTICAL to patch-962.zip (no junk paths, no stale overlays).
- patch-963.hot.json: plugin=belowicemountain, patch=963, hostVersion=1, build=100,
  sha256 5cfe9323129188b4c316ad45c5fffbd90d80691d6bf4787b6896c232bacef5b1
  == sha256 of the actual belowicemountain-100.jar bytes — hot-reload host integrity check will pass.
- BUILD_NUMBER=100 confirmed in source (source-review/belowicemountain-build100/BelowIceMountainScript.java:93).
- Source diff B99->B100 (whitespace-normalized): 41 lines total, all inside BelowIceMountainScript.java.
  Changed class files (Script, $2, GuardianPathPlan, QuestGeBuyer+inners, TrainingStyleControl) are
  consistent with a recompile of the single edited source file — no logic changes to path planning
  or training style (the 41-line diff touches neither).

## What changed (mechanism)
1. `loadStage35Checkpoint` (was: hard-reject on PID mismatch): still requires account name + stage 35,
   but a PID mismatch no longer auto-rejects. It now parses the saved offer; if format=="GE2",
   item field matches the requested item id, and status=="COMPLETE", the completed offer is accepted
   across processes. Otherwise it throws IllegalStateException("Unfinished stage35 GE offer belongs
   to another process").
2. New error-recovery branch (mirrors the STAGE35_POST_DEATH_SURFACE_PREP pattern directly above it):
   when error startsWith "Stage35 trout checkpoint mismatch" AND gameState=LOGGED_IN AND questStage==35
   AND near (3222,3217,0) r=12 AND !guardianActionsAllowed() → loadStage35Checkpoint(TROUT); if the
   settled offer is GE2+COMPLETE → log STAGE35_OLD_GE_COMPLETE_RECONCILED (currentPid, accountBound=true,
   caveDisabled=true), clear the error, and set stage="STAGE35_POST_RESTART_BANK_AUDIT". On exception →
   log STAGE35_OLD_GE_RECONCILE_HELD and keep the HOLD.

## Findings
- PASS: This closes the open B92 PID-bound checkpoint watch item (a completed GE trout offer from an
  earlier session no longer HOLDs a fresh session, and no longer risks a redundant rebuy). The
  conservative direction is preserved: UNFINISHED offers still HOLD with a clear message — no double-buy.
- PASS: Reconcile gate is tight — exact error prefix, logged-in, stage 35, surface-near-Lumbridge,
  guardian actions disarmed. Cannot fire mid-cave or mid-encounter.
- PASS: STAGE35_POST_RESTART_BANK_AUDIT needs no dedicated handler: the loop is observation-driven,
  so the next tick routes stage-35-at-surface through the food-count (<16) / healing (<160) gate into
  stage35RecoveryBank → bank open → PREP_BANK_AUDIT snapshot. If the trout were not actually there,
  the bank step rebuys. (Same informational-stage convention as STAGE35_POST_DEATH_SURFACE_PREP.)
- INFO: Reconcile accepts the checkpoint as settled without an immediate bank/inventory re-verification
  at reconcile time; coverage comes from the subsequent bank audit + food gates. Acceptable.
- INFO: STAGE35_OLD_GE_RECONCILE_HELD is warn-logged — the unfinished-offer HOLD path is observable.
- No ship by me; TI tree untouched (build238-src: zero .java newer than 2026-10-02 08:30 EDT).

## Live acceptance (pending — feed dark ~38.9h, newest b4e19333 2026-09-30 17:44:02 EDT)
Watch the stream for: hot-reload acceptance → RUNTIME BUILD "100 / confirmed"; on script resume, if the
old PID checkpoint mismatch fires, the new STAGE35_OLD_GE_COMPLETE_RECONCILED line (not a rebuy, not
a stuck HOLD). Verified quest tally unchanged: 8 quests / 19 QP (Pirate's Treasure last live-verified).
