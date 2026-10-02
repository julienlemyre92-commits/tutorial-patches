# Review verdict: Below Ice Mountain Build 10 (patch-875) — READ-ONLY

Reviewed by: Muse (read-only; Alex owns implementation/releases; no ship)
Commit: 9b4be484 ("Below Ice Mountain Build10 Atlas scene dialogue", landed 2026-10-01 23:35:30 EDT)
Verdict: **PASS WITH FINDINGS**

## Custody — AIR TIGHT
- hot.json `patch-875.hot.json` sha256 `5dbf51de7731b3fc3849a904572794dd8d3594d8504ab834a840b1011856a8e8` == `patches/belowicemountain-10.jar` bytes — FULL MATCH.
- `patch-875.zip`: 276 entries, net-rooted (zip-built, no manifest issue), in-zip `version.txt` = `875` == repo `version.txt` (at review time) == patch number.
- `BUILD_NUMBER=10` in published source and in the compiled class constant — no lying banner.
- Single-purpose commit (patch-875.zip + hot.json + jar + source-review Build 10 + version.txt); version 874→875 sequential, no reuse, no overwrite of an existing patch-N.zip.

## Diff vs Build 9 (published source, 613→615 lines) — surgical
1. `BUILD_NUMBER` 9→10.
2. New constant `ATLAS_SCENE = (12806,12237,0)` — Atlas cutscene tile observed live.
3. Dialogue expected-gate (line ~425) extended: `questStage==10 && (near(CHECKAL,12) || near(ATLAS,12) || (f.checkal>=10 && near(f.position,ATLAS_SCENE,30)))`.
Nothing else changed.

## Design rationale (per source-review README §Build 10)
Build 9 entered Atlas's strongman cutscene ~ (12806,12237); a fresh desktop capture showed Atlas saying "You need to be tough to be a strongman! Show me what you've got! Come on!" with a valid Continue dialogue while the script held. Build 10 accepts stage-10 dialogue within a 30-tile radius of that observed scene, only when Checkal varbit>=10; per-click dialogue-change proof retained; holds outside known scenes. No combat or guardian logic added.

## Findings
- INFO BIM10-1: the single-point 30-tile radius gate is brittle — instanced cutscene coordinates drift between entries. (Build 11, shipped ~1 min later, live-proved this: the scene moved to (12867,12275), outside the radius.) Superseded, not blocking.
- Carried: INFO BIM9-1 (low-byte stage decode assumption; raw varp retained in logs), LOW BIM7-1 / LOW BIM7-2 (route progress thresholds), LOW BIM2-2 (Mining-10 gate), INFO BIM2-4 (issue() unused).

## Live acceptance
PENDING — RUNTIME BUILD 10 lines not yet observed on stream (23:35 check showed Build 9/confirmed; per README Build 10 was "not armed" and Build 11 shipped ~1 min after Build 10's hot load).

No shipping action taken — read-only.
