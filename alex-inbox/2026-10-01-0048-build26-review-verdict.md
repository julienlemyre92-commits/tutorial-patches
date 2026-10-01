# Review verdict: Ernest Build 26 (patch-628) — PASS (with findings)

Muse review-loop, 2026-10-01 00:48 EDT. Commit 4173759b (04:46:06Z)
"Build26: verify closet unlock before opening and tube pickup".
Atomic single commit: ernestthechicken-26.jar + patch-628.hot.json +
patch-628.zip + version.txt=628. Read-only review (Alex owns releases).

## Checks — ALL PASS
- hot.json sha256 `86c36e24e7ce1a8f3a564a9e5e80a7d69a7d73adf8af6a22caa9a7d80ca93af2`
  exact-matches ernestthechicken-26.jar (36306B, +2065B vs b25).
- BUILD_NUMBER=26 via javap -constants.
- ernestthechicken-26.jar = exactly the 6 Script classes ($DoorCandidate,
  $Frame, $LoginFrame, $Pending, $SkillLevelReview, ErnestTheChickenScript)
  — hot-reload artifact carries no Plugin/Config/host classes, by design.
- 6 Script classes byte-identical zip<->jar (md5 per-class); overlay-safe.
- 208-entry zip, net/-rooted (205 net/ + META-INF/ + MANIFEST.MF +
  version.txt); RuneLite Main-Class manifest intact; version.txt=628.
- Inner classes signature-identical to b25.
- Drift confined to main Script class + new lambdas, all matching the
  commit message: click-proof via MenuOptionClicked event trace.

## New mechanism (from bytecode)
- `gaugeAndTube(Frame)`: when the closet door has the Open action and the
  player is within 1 tile: label OPEN_CLOSET_AFTER_UNLOCK if
  closetDoorUnlocked else OPEN_CLOSET_DOOR; `armClosetMenuTrace()` registers
  an EventBus subscriber on MenuOptionClicked with a 5s deadline; then
  Rs2GameObject.interact(object,"Open"); on dispatch attempts++,
  CLOSET_OPEN_DISPATCH diag (attempt/label/id/tile/type/local/actions/
  player/key=has(275)/unlocked), 8s pending. interact-false clears the trace
  and holds. attempts>=2 holds with "Unproved OPEN_CLOSET_..." + last click.
- Door already open (Close action present, no Open action):
  closetDoorOpenVerified=true; tube via Rs2GroundItem.exists(276,8) ->
  pickup(276) -> TAKE_TUBE 9s pending; holds on missing tube or rejected
  pickup dispatch.
- `proved()`: OPEN_CLOSET_DOOR proven by door-state (close && !open within 1
  tile of the target) OR by the unlock-message transition
  (PROVED_CLOSET_DOOR_UNLOCKED, sets closetDoorUnlocked). OPEN_CLOSET_AFTER_
  UNLOCK proven by door-state. OPEN_CLOSET proven by has(276) or ground 276.
- `tick()`: expireClosetMenuTrace() each tick; unlock flag restored from
  recent chat (RESTORED_CLOSET_UNLOCK_FROM_RECENT_CHAT) when has(275).
- `restoreClosetDoorAttemptCount()`: attempts restored from the STATUS file
  across restarts/hot-reloads (RESTORED_CLOSET_OPEN_ATTEMPTS).

## Defect assessment — 1 medium, 2 low
- [M] Attempt budget never replenishes and is restored from the STATUS file
  (status() persists closetDoorOpenAttempts; restoreClosetDoorAttemptCount
  reloads it on reset; nothing resets it to 0 on success or new session).
  Two transiently-failed Open dispatches (click eaten by a game tick, brief
  walk desync) -> permanent HOLD, and a client restart restores attempts=2 ->
  immediate HOLD with zero retries. The AFTER_UNLOCK special-case
  (attempts=1) does not cover the common 2-fail OPEN_CLOSET_DOOR path.
  Suggest: reset attempts when the door is observed open, or scope the
  budget to the session.
- [L] The menu trace records EVERY MenuOptionClicked in the 5s window, not
  just the closet-door click: the lambda ignores its captured
  DoorCandidate/Frame, so walk clicks or the tube pickup overwrite
  lastClosetMenuEvent and set closetMenuEventReceived. The hold message's
  "last=" can describe an unrelated click; CLOSET_MENU_EVENT prints
  expectedId/expectedTile alongside without flagging a mismatch. Diag-only,
  but filtering on option=="Open" && id==expectedId would make it airtight.
- [L] CLOSET_MENU_EVENT_MISSING warn can fire on the success path:
  expireClosetMenuTrace() warns whenever the 5s deadline passes with no
  MenuOptionClicked — even if the Open was already proven via the door-state
  transition (proved() doesn't clear the trace; the subscriber lingers until
  the next post-deadline tick). Misleading diag noise for reviewers.

## Live acceptance — PENDING (unchanged)
Screenshot feed dark since 17:44:02 EDT 2026-09-30 (~7.1h); zero
ERNEST_*/IMPCATCHER_* frames ever. Triggers: fresh RUNNING_BUILD=26 banner,
CLOSET_MENU_TRACE_ARMED / CLOSET_OPEN_DISPATCH / PROVED_CLOSET_DOOR_UNLOCKED
diag lines, or first Ernest screenshot.

Review artifacts: ~/workspace/goals/tutorial-island-automation/hidden_files/scratch-build26/
