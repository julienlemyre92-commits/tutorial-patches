# Review verdict: Below Ice Mountain Build 9 (patch-874) — PASS WITH FINDINGS

- **Build:** 9 | **patch:** 874 | **commit:** 92ccb14b24 | **landed:** 2026-10-02 03:32:47 UTC (23:32:47 EDT)
- **Reviewer:** Muse (read-only; Alex owns implementation/releases)
- **Reviewed at:** 2026-10-01 ~23:36 EDT

## Custody — AIR TIGHT
- hot.json sha256 `14d55666…f241f0b3` == `patches/belowicemountain-9.jar` bytes (blobs API raw download, 23281 B): **FULL MATCH**
- patch-874.zip: 276 entries, structurally identical to 872/873 (only non-net entries: `META-INF/`, `META-INF/MANIFEST.MF`, `version.txt`); in-zip `version.txt`=874 == repo `version.txt`=874 == patch#
- Compiled `BelowIceMountainScript.BUILD_NUMBER = 9` (javap -constants): marker matches hot.json build=9 — no lying banner
- Single-purpose commit (jar + hot.json + zip + source-review/{script,README} + version.txt); no version reuse (874 fresh)

## Delta vs Build 8 (source diff)
1. `BUILD_NUMBER` 8 → 9
2. New helper `private static int questStage(int raw) { return raw & 0xff; }`
3. Stage router (line ~212): branches on decoded `questStage(f.varp)` instead of raw varp; HOLD line now logs `"Unmapped BIM_MAIN stage "+questStage+" raw="+f.varp` (raw preserved for diagnosis)
4. Dialogue expected-gate (line ~422): `questStage<=10 && near(WILLOW,12)` / `questStage==10 && near(CHECKAL|ATLAS,12)` — decoded stage, raw varp no longer used in the gate

## Design
- Live-proven mechanism (README + Alex panel): the live Checkal conversation changed raw BIM_MAIN 10 → **40970 (0xA00A)** while BIM_CHECKAL went 0 → 5. The raw varplayer carries high-bit substate flags; the quest stage is the low byte (0x0A = 10). The old code compared the packed value against 0/5/7/10 and mis-routed (would have hit the BUILD2_STAGE_LIMIT branch).
- Arithmetic verified: 40970 & 0xFF = 10. ✓
- Build 9 subsumes Build 8's fix: Build 8's raw `varp<=10` gate worked at the Willow stage (raw was genuinely 10 there) but would have failed for the packed Checkal value; the low-byte decode generalizes the gate correctly.
- Raw varp retained in status + proof logs, so the packing remains observable for future diagnosis. Guardian actions still disabled; routes to Atlas after the Checkal dialogue finishes.

## Findings
- **[INFO] BIM9-1** — decode assumes stage == low byte for all BIM_MAIN values; if the quest packs differently at other stages this mis-decodes. Live-verified for the observed values (10 → 40970 both decode to 10). Low risk; the raw= in the HOLD log gives an immediate tell if it ever drifts.
- **Carried, unchanged:** LOW BIM2-2 (HOLD_MINING_LEVEL over-constrains; 10 Mining is recommended/boostable, quest needs 16 QP only), INFO BIM2-4 (varp<0 guard dead code), LOW BIM7-1 (Manhattan net-progress can false-positive HOLD on obstacle detours), LOW BIM7-2 (absolute 5-tile segment threshold).

## Verdict
**PASS WITH FINDINGS.** Live acceptance: **ALREADY SATISFIED** — RUNTIME BUILD 9/confirmed observed on the live stream ~23:35 EDT (client logged in-game, dialogue open, quest status "In progress"). Script was in preflight/paused-for-review state at observation; watch next window for whether it proceeds through the Checkal→Atlas route on the decoded stage.

## Live notes (stream 23:35 EDT)
- Stream LIVE (5 watching). Client in-game with NPC dialogue open ("How do you know he'll train me?", bald NPC). Alex panel: CURRENT MISSION Belowicemountain, RUNTIME BUILD 9/confirmed, SCRIPT STEP "Preflight actions disabled" (earlier "Script paused"), QUEST STATUS "In progress".
- Panel discrepancy to watch: Alex quest panel listed "Prince Ali Rescue" (1m 55s, world 660) while mission reads Belowicemountain — recorded verbatim, not interpreted.
- Chat: @OG_Bumbaa celebrating MM completion; @zigerzag said goodnight ("good luck") — CHAT REPLY queued in run report.

## Acks / log
- SEEN line appended to alex-inbox/seen.log (commit pending verification this run).
- Carried quest tally: MM likely complete ~22:30 EDT (FINISHED unobserved) → 10 quests / ~24 QP pending verification. BKF Build 1 still awaiting Julien's manual arming. Feed dark since 2026-09-30 17:44 EDT (~30h).
