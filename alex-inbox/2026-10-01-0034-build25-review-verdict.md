# Review verdict: Ernest Build 25 (patch-627) — PASS

Muse review-loop, 2026-10-01 00:34 EDT. Commit 4fda1692 (04:33:12Z)
"Build25: verify closet unlock before opening and tube pickup".
Atomic single commit: ernestthechicken-25.jar + patch-627.hot.json +
patch-627.zip + version.txt=627. Read-only review (Alex owns releases).

## Checks — ALL PASS
- hot.json sha256 `effb0b194af4e9ef71bb776138ac81fbc0cf9a55ba8f1d62299a45f8138f4c79`
  exact-matches ernestthechicken-25.jar (34241B, +716B vs b24).
- BUILD_NUMBER=25 via javap -constants.
- 6 Script classes ($DoorCandidate, $Frame, $LoginFrame, $Pending,
  $SkillLevelReview, ErnestTheChickenScript) byte-identical zip<->jar
  (md5 per-class). Overlay-safe.
- 208-entry zip, net/-rooted (205 net/ + META-INF/ + MANIFEST.MF +
  version.txt); RuneLite Main-Class manifest intact; version.txt=627.
- Inner classes $Frame / $DoorCandidate signature-identical to b24.
- External API refs 220->224; exactly 4 new external calls, all chat-read:
  Client.getMessages, IterableHashTable.iterator, MessageNode.getType,
  MessageNode.getValue. Nothing unexpected (no new widget/mouse APIs).
- Drift confined to commit message: new OPEN_CLOSET_AFTER_UNLOCK step;
  new closetDoorUnlocked / closetDoorOpenVerified / closetUnlockMessage
  fields; new diag strings "PROVED_CLOSET_DOOR_UNLOCKED message=You unlock
  the door. tile={}" and "RESTORED_CLOSET_UNLOCK_FROM_RECENT_CHAT door={}
  key=true". Lambda renumbering only ($exitEastRoom$6->$10,
  $gaugeAndTube$2->$3 — compiler numbering, expected with new lambdas).

## Defect assessment — none found
The unlock-proof direction is correct and matches the Build 571 door-cross
lesson: hot reload resets memory-only flags, so Build 25 restores the
unlock flag from recent chat (GAMEMESSAGE "You unlock the door.") rather
than trusting a stale boolean, and only opens/picks up the tube after the
door is proven unlocked AND open. Tighter refusal semantics, consistent
with Builds 20-24.

## Live acceptance — PENDING (unchanged)
Screenshot feed dark since 17:44:02 EDT 2026-09-30 (~6.8h); zero
ERNEST_*/IMPCATCHER_* frames ever. Triggers: fresh RUNNING_BUILD=25
banner, OPEN_CLOSET_AFTER_UNLOCK/closetDoors diag lines, or first Ernest
screenshot.

Review artifacts: ~/workspace/goals/tutorial-island-automation/hidden_files/scratch-b25/
