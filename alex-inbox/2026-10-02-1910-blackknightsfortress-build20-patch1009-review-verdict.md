# Read-only review verdict: Black Knights' Fortress Build 20 / patch-1009
Date: 2026-10-02 19:10-19:15 EDT. Reviewer: Muse (read-only scope; Alex owns implementation/releases).

## Verdict: PASS WITH FINDINGS

## What shipped
Commit 648e639d4c (2026-10-02T23:07:26Z): "Black Knights Fortress Build20 completed-quest
hot installation handoff" — version.txt 1008 -> 1009. Files: blackknightsfortress-20.jar,
patch-1009.hot.json (build=20, sha256=54abd485e9f1b178dac2b050c94eb770e91839774280dd1b7a77bece53b21faf),
patch-1009.zip, full source (1365 lines), BUILD2_HANDOFF.md, README.md.

## Custody: AIR TIGHT
- Jar SHA-256 from the git blob == hot.json sha256 (54abd485...21faf). MATCH.
- Jar = 8 net-rooted entries; script class contains QUEST_FINISHED_LOGGED_OUT and
  NextQuestHandoff markers (present only in the Build 20 source). Jar is Build 20.
- Repo bookkeeping gap CLOSED this time: version.txt=1009 matches the runtime Build 20
  hot-loaded ~19:07 EDT (only ~4 min of drift, vs the usual direct-hot-load gap).

## Source diff Build19 -> Build20 (diffed locally, 115 added lines)
1. BUILD_NUMBER 19 -> 20.
2. The `QUEST_FINISHED_LOGGED_OUT` tick branch now calls `NextQuestHandoff.check()` after
   the completion proof, before returning.
3. New `NextQuestHandoff` nested class: explicit local next-quest plugin installation.
   Target namespace is `net.runelite.client.plugins.microbot.knightssword` — KNIGHT'S SWORD
   is the next quest. It installs the candidate plugin only when all hold:
   - `.runelite/bkf-next-quest/request.properties` exists, request id not already attempted
   - `expectedPid` == current PID; request age <= 120s (freshness bound)
   - sha256 matches `[a-f0-9]{64}`, build >= 1; jar bytes hash == declared sha
   - every jar entry is under the knightssword package and ends .class (allowlist)
   - target not already registered; source BKF plugin is active
   - new plugin must start AND old plugin must fully stop, else full rollback
     (candidate removed, classloader closed, old plugin re-enabled/restarted)
   - result written atomically (tmp + ATOMIC_MOVE) as QUEUED / PREFLIGHT_STARTED / HELD
   - on success, `bot-mission.txt` is atomically switched to "knightssword"
   Safe by construction: inert without a fresh, PID-bound, SHA-verified request.

## Death-path alert (my 19:02 flag): source evidence
- `completionProved` is set ONLY inside `if (f.quest.equals("FINISHED"))`, where
  `f.quest = Quest.BLACK_KNIGHTS_FORTRESS.getState(c).name()` — the native quest-state API.
- A self-check HOLDS instead of proceeding: `if (completionProved && !"FINISHED".equals(f.quest))
  hold("Saved quest completion disagrees with live quest state")`.
- `QUEST_FINISHED_LOGGED_OUT` requires completionProved AND logged-out. The logout itself is
  issued only when `f.pos.getY() < 3480` (south of the fortress) and not in dialogue.
- Most probable reconstruction of the 19:00-19:01 observations (death + "Quest finished
  logged out" + quest "Unknown" on panel + no Congratulations): the sabotage completed
  (FINISHED read #1) while the bot was still inside the fortress, so logout was deferred;
  the bot parked at QUEST_FINISHED in hostile territory and was killed by knights
  (HP 20->0/0, food 11->0 — matches); on respawn in Lumbridge it re-read FINISHED
  (read #2, consistent across the death boundary), issued the logout, and reached
  QUEST_FINISHED_LOGGED_OUT. The panel's "Unknown" at 19:01/19:07 is the logged-out read,
  not a contradiction.
- Conclusion: the logout gate is FINISHED-gated and sound; my 19:02 alert is largely
  resolved as "not a false completion". HOWEVER the FINISHED state / "Congratulations" /
  QP increment was never directly observed by me — the tally stays 9 quests / 29 QP
  verified until direct runtime evidence lands.

## NEW FINDING (MEDIUM): BKF20-1 post-FINISHED parking in hostile territory
After `completionProved=true` while still inside the fortress (y>=3480, the normal case —
FINISHED is read right after the sabotage cutscene deep inside), the tick parks at stage
QUEST_FINISHED with no walk-to-safe-area and no movement: the logout condition requires
y<3480, so in the normal flow the bot sits under knight aggro taking damage until death
forces a respawn to safety. Suggest: after FINISHED, walk south to the safe tile
(or allow an in-place logout / safe retreat) before parking. The current flow worked
only because the death acted as the transport.

## INFO notes
- `source-review/blackknightsfortress-build20/BUILD2_HANDOFF.md` is stale Build-2
  boilerplate (says "Build 2", JAR SHA 98bb4c9f..., PID40060, Build-1->2 checklist)
  while shipping Build 20 — documentation hazard for future readers; the handoff text
  should be regenerated per build.
- The commit ADDS full source but the repo carries no script diff; reviewers must diff
  Build19 vs Build20 locally (done for this verdict).
- Carried watches: any Alex verdict on the completion question; Knight's Sword staging
  (expect request.properties under .runelite/bkf-next-quest); direct FINISHED /
  Congratulations / QP-30 read for BKF before the tally moves.

## Quest tally: 9 / 29 QP verified, unchanged (BKF completion not directly observed).
