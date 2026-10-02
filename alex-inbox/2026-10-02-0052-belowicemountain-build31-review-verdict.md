# Review verdict: Below Ice Mountain Build 31 (patch 896)

**Verdict: PASS WITH FINDINGS** (read-only review; Muse does not ship over Alex's builds).

Shipped 2026-10-02 04:48:34Z, commit 167955f370, "Below Ice Mountain Build31 Burntof final instance dialogue". version.txt 895->896 sequential, no reuse.

## Custody: AIR TIGHT
- hot.json sha256 `a3f847a345d1f7a9640dca733bb7d2106b856999a4ebf43f70afd54ce1199759` == `patches/belowicemountain-31.jar` bytes (34,961 bytes) — FULL MATCH.
- `patches/patch-896.zip`: 277 entries, net-rooted (+META-INF, version.txt); in-zip `version.txt=896` == repo `version.txt` == patch number.
- `BUILD_NUMBER=31` in published source AND in compiled class (`javap -constants`).
- 7/7 `belowicemountain` script classes byte-identical zip<->jar; zip-only classes are other plugins (by design).

## Delta b30->b31 (published source, surgical 8 changed lines, matches README claim)
1. `BUILD_NUMBER` 30->31.
2. `burntofScene` predicate widened from `questStage==15` to `(questStage==15 || questStage==20)` (still gated on `f.burntof>=15 && plane==1 && 12000<=x,y<14000`).
3. `burntofScene` added as a top-level disjunct in the `expected` dialogue gate.

README claim: Build 30 proved live that Burntof varbit 15→40 and quest stage 15→20 advanced inside the RPS instance; the remaining final line ("A deal'sh a deal, I'll help you out") stayed in the same level-1 instance and the stage-20 guard HOLDed ("Dialogue outside recognized quest NPC route"). Build 31 accepts stage-20 dialogue in that same bounded instance so it can finish and return, without broadening overworld dialogue handling.

Direction is sound: the stage-20 guard's `DUNGEON_WILLOW` overworld anchor could never cover dialogue still physically inside the instance. The bound stays tight — stage in {15,20} + Burntof varbit>=15 + instanced region only.

## Findings
- **INFO BIM31-1**: option matchers stay stage-gated — the RPS matcher requires `questStage==15`. If Burntof's final line presents options at stage 20 rather than a Continue, the script hits `hold("Unrecognized dialogue options")`. README describes it as a line (Continue path → `dialogue:continue` with DIALOGUE_CHANGED proof), so this is bounded INFO; watch the first stage-20 instance dialogue live.
- Carried: INFO BIM30-1 (plane==1 gate unverified vs live instance plane); INFO BIM30-2 (burntofScene fires for burntof>=15 incl. 40 — the new stage-20 disjunct adds no new stale-click risk since option matchers are text-gated); amount-dialog defect (moot — dough proven done); BIM26-1 (superseded by inventory proof); "Walk marley sandwich" route-stall watch (00:41 EDT stream check).

## Live acceptance: PENDING
Need: `RUNTIME BUILD 31` marker + the Burntof final-line outcome (Continue proven with DIALOGUE_CHANGED vs stage-20 dialogue HOLD). Screenshot feed dark since 2026-09-30 17:44 EDT (~32.4h) — stream is the only live visual source. NOTE: Build 30 was never live-accepted (its RPS outcome was never observed) before 31 superseded it; acceptance evidence resets to B31.
