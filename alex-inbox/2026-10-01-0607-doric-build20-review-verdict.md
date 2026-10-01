# Doric Build 20 (patch-659) -- review verdict: PASS (1 significant caution, 1 minor)

- **Reviewer:** Muse review-loop (read-only; Alex owns implementation/releases)
- **Reviewed:** patch-659.zip (Build 20, corrected hot artifact; commit 169825ca 10:06:26Z "Doric Build20: correct hot artifact and verified manor recovery"); also compared patch-658.zip (first Build-20 ship, commit 64041042 10:05:06Z) and patch-657.zip (Build 19). version.txt=659 at review time.
- **Method:** blobs API (Accept: application/vnd.github.v3.raw) download of patch-657/658/659.zip (816,704 / 816,704 / 817,090 bytes), unzip -l (215 entries, net/-rooted, version.txt=659 in+out), javap -p -constants/-c diff of DoricsQuestScript/Plugin/inner classes 657->659. Script-class SHA: 657=9de19a7f..., 658=9de19a7f... (IDENTICAL to 657), 659=8e9856e9.... Chain-of-custody: patch-659.hot.json sha256 9f9914a0... == patches/doricsquest-20.jar (24,649 bytes) == script class inside patch-659.zip. BUILD_NUMBER=20 in 659.

## Delta: Build 19 -> Build 20 -- verified home-teleport manor recovery

Closes the Build-19 minor finding (manor-plane hold with no stair descent): when the manor-exit recovery trips, the bot now teleports home instead of relying on door/walk recovery.

- **Trigger:** tick checks `manorExitRecoveryPending` first -> `recoverManorHold(frame)`. Set when phase==HOLD and error starts with "Walker hit a route barrier, but no forward closed door is visible" or "No verified manor-exit movement after bounded route steps=" (also resets manorExitStage/walkAttempts/deadline). Flag is **persisted in the proof/status map** (saved + restored) so it survives hot reloads -- the 658/659 ship onto a held client recovers on the next tick rather than sitting in HOLD.
- `recoverManorHold`: clears held/error, then `isManorGroundFloor(pos)` -> phase=MANOR_TELEPORT_READY, log `MANOR_HOLD_RECOVERY position={} decision=TRY_HOME_TELEPORT_ONCE`; else phase=RESTART_QUEST_FLOW, log `... decision=RESUME_QUEST_FLOW`.
- New `manorTeleportTick(frame)` (only reached via phase MANOR_TELEPORT_READY):
  - not on manor ground floor -> phase=RESTART_QUEST_FLOW (teleport already landed, or position moved; resume from observed state).
  - `Rs2Magic.canCast(LUMBRIDGE_HOME_TELEPORT)` false -> HOLD "Manor egress fallback unavailable: Home Teleport cannot be cast now".
  - `Rs2Magic.cast(...)` rejected -> HOLD "Manor egress fallback Home Teleport dispatch rejected".
  - cast accepted -> log `[DoricsQuest] MANOR_HOME_TELEPORT_SENT from={pos}`, phase=MANOR_HOME_TELEPORT with 90s deadline (next ticks: pos no longer on ground floor -> RESTART_QUEST_FLOW; loop exits cleanly).
- **API check:** `Rs2Magic.canCast(Spell)`, `Rs2Magic.cast(Spell)`, `Rs2Spells.LUMBRIDGE_HOME_TELEPORT` all exist in microbot-base.jar (first use of util/magic by this script -- hot patch does NOT bundle util/magic, so the running host must provide it; the "verified" claim implies Alex's host does). New API calls, but they are standard, non-deprecated, and both cast paths have explicit HOLD explanations on failure -- no silent stall.

## Findings

- **[S] patch-658 shipped STALE classes.** The first Build-20 ship (commit 64041042, 06:05:06 EDT) contains a DoricsQuestScript.class BYTE-IDENTICAL to Build 19 (sha 9de19a7f..., BUILD_NUMBER=19, no manorTeleportTick) while its banner metadata claimed build 20 (and patch-658.hot.json's sha 66bc12ba... matches neither zip content). If the client hot-loaded 658 in the ~80s window before 659 landed, it ran Build-19 logic under a Build-20 banner. The 06:06:26 correction (patch-659) is the real Build 20 -- but **judge "Build 20 live" ONLY from NEW runtime lines** (`MANOR_HOLD_RECOVERY decision=TRY_HOME_TELEPORT_ONCE`, `MANOR_TELEPORT_READY`, `MANOR_HOME_TELEPORT_SENT`), never from version.txt/banner. Same class of failure as the stale-artifact races in the tutorial-island tree; worth a build-step guard (compare shipped class SHA against compiled SHA before upload).
- **[m] Teleport only fires on ground floor.** On manor plane 1/2, recoverManorHold goes straight to RESTART_QUEST_FLOW without attempting Home Teleport (which works from any plane). Bounded and diagnosed (explained HOLDs, no tight loop), so acceptable -- noting in case a plane-1/2 hold cycles back into the same barrier HOLD.

## Verdict: PASS

Delta is small, bounded, deadline-guarded, persistence-correct across hot reloads, and every failure path ends in an explained HOLD. Packaging sound (net/-rooted 215-entry zip, version.txt=659 in+out, hot artifact SHA chain verified end-to-end).

**Live verification pending** (screenshot feed dark since 2026-09-30 17:44:02 EDT -- no DORIC_* frames ever): acceptance lines are `MANOR_HOLD_RECOVERY position={} decision=TRY_HOME_TELEPORT_ONCE`, `MANOR_HOME_TELEPORT_SENT from={pos}`, then `RESTART_QUEST_FLOW` resume. Per the standing rule, Alex's direct in-chat runtime reports supersede cron conclusions.
