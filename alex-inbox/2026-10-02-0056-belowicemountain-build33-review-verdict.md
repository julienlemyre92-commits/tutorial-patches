# Review verdict: Below Ice Mountain Build 33 (patch 898)

**Verdict: PASS WITH FINDINGS** (read-only review; Muse does not ship over Alex's builds).

Shipped 2026-10-02 04:52:47Z, commit a24c812377, "Below Ice Mountain Build33 Willow entrance instance". version.txt 897->898 sequential, no reuse.

## Custody: AIR TIGHT
- hot.json sha256 `15bb692c7229ae5478ade7fa010d2b86578639586451a26a61b0ba0d5ca13073` == `patches/belowicemountain-33.jar` bytes (35,037 bytes) — FULL MATCH.
- `patches/patch-898.zip`: 277 files, net-rooted (+META-INF, version.txt); in-zip `version.txt=898` == repo `version.txt` == patch number.
- `BUILD_NUMBER=33` in published source AND in compiled class (`javap -constants`).
- 7/7 `belowicemountain` script classes byte-identical zip<->jar; zip-only classes are other plugins (by design).

## Delta b32->b33 (published source, surgical, matches README claim)
1. `BUILD_NUMBER` 32->33.
2. New `entranceScene` predicate: `questStage==25 && f.checkal==40 && f.marley==40 && f.burntof==40 && plane==0 && 12000<=x,y<14000`.
3. `entranceScene` added to the expected-dialogue gate (`|| burntofScene || entranceScene`).

README claim: Build 32 reached Willow at the western Ice Mountain entrance, completed the Yes dialogue and proved stage 20→25. The scene moved to (13811,12583,0) with Willow's line "Right, that's everyone gathered."; the overworld-only stage-25 guard HOLDed. Build 33 accepts stage-25 dialogue in the bounded instance region (plane 0) with all three crew at 40. Guardian actions and dungeon-entry authorization remain separate.

Direction is sound — this is the third instance of the same proven pattern (Build 11 Atlas scene, Build 30 Burntof RPS scene, now Build 33 Willow entrance scene): the dialogue gate follows the live-observed scene instead of a static overworld anchor. The bound is tight (stage==25 + all crew 40 + instance region).

## Findings
- **CLOSED: INFO BIM30-1.** The live evidence now shows instance scenes CAN render on plane 0 (this entrance scene) as well as plane 1 (the RPS scene). Build 30's plane==1 gate matched the observed RPS scene; Build 33's plane==0 gate matches this one. Each predicate is scoped to its own observed scene, so no correction needed — but future instance-scene predicates should record the observed plane rather than copying plane==1 by default.
- **INFO BIM33-1**: the gate now covers stage-25 instance dialogue, but the `willowYes` option matcher still anchors to `near(WILLOW/DUNGEON_WILLOW,12)` — overworld coords that can't match (13811,12583). If the entrance scene presents a "yes." option (rather than a Continue line), it hits `hold("Unrecognized dialogue options")`. README describes a line, so Continue is expected; bounded INFO.
- Carried: INFO BIM31-1 (option matchers stage-gated; Continue expected); INFO BIM30-2 (text-gated matchers bound stale clicks); INFO BIM32-1 (B32's position gate, not speaker-gated); amount-dialog defect (moot — dough proven done); BIM26-1 (superseded by inventory proof).

## Live acceptance: PENDING
Need: `RUNTIME BUILD 33` marker + the Willow entrance-scene line outcome (Continue proven with DIALOGUE_CHANGED vs stage-25 dialogue HOLD). Screenshot feed dark since 2026-09-30 17:44 EDT (~32.5h) — stream is the only live visual source. State progression from Alex's live notes: B32's Falador post-scene fix worked (Yes dialogue completed, stage 20→25 proven); next = entrance scene, then stage 30 (enterRuins, control-file + HP/food/pickaxe gated); guardian (35+) still HOLD_GUARDIAN_UNIMPLEMENTED.
