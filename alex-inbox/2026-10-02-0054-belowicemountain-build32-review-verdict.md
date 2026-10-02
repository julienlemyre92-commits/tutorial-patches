# Review verdict: Below Ice Mountain Build 32 (patch 897)

**Verdict: PASS** (read-only review; Muse does not ship over Alex's builds).

Shipped 2026-10-02 04:50:20Z, commit dc3b86538f, "Below Ice Mountain Build32 Willow Falador post-scene dialogue". version.txt 896->897 sequential, no reuse.

## Custody: AIR TIGHT
- hot.json sha256 `f35b5de2eb9274defb52fdd9d733efcb009fb145ccc40a1c5701e92f9af3de30` == `patches/belowicemountain-32.jar` bytes (34,995 bytes) — FULL MATCH.
- `patches/patch-897.zip`: 277 files, net-rooted (+META-INF, version.txt); in-zip `version.txt=897` == repo `version.txt` == patch number.
- `BUILD_NUMBER=32` in published source AND in compiled class (`javap -constants`).
- 7/7 `belowicemountain` script classes byte-identical zip<->jar; zip-only classes are other plugins (by design).

## Delta b31->b32 (published source, surgical, matches README claim)
1. `BUILD_NUMBER` 31->32.
2. New expected-dialogue disjunct: `(questStage==20 && f.burntof==40 && f.marley==40 && f.checkal==40 && near(f.position,BURNTOF,12))`.

README claim: Build 31's fix worked live — the stage-20 instance dialogue continued and the player returned to Falador at (2956,3368). The live prompt is Willow saying the group should head to the entrance west of Ice Mountain; stage-20 dialogue was previously accepted only at the dungeon entrance, so this return line HOLDed. Build 32 accepts dialogue near Burntof's Falador location only when all three crew varbits are 40 and stage 20 is active, then proceeds to Willow's entrance route.

Verified against source: `BURNTOF = (2956,3367,0)` — the live return position (2956,3368) is 1 tile away, exactly inside the 12-tile gate. The fix targets the observed live position precisely. The gate is tight: stage==20 + all three crew varbits exactly 40 + player near Burntof. If the line has a Continue, it goes through `dialogue:continue` with DIALOGUE_CHANGED proof; option matchers are text-gated and stage-gated, so a stray option can't fire an unproved action.

## Findings
- **INFO BIM32-1**: the new disjunct gates player position, not the dialogue speaker — any stage-20 dialogue within 12 tiles of (2956,3367) with all crew at 40 is accepted. Continue clicks are DIALOGUE_CHANGED-proven and option matchers are text-gated, so a stray dialogue can't trigger an unproved action; bounded, no action needed.
- Carried: INFO BIM31-1 (option matchers stage-gated, Continue expected); INFO BIM30-1 (plane==1 gate unverified vs live instance plane); INFO BIM30-2 (matchers text-gated, stale clicks bounded); amount-dialog defect (moot — dough proven done); BIM26-1 (superseded by inventory proof).

## Live acceptance: PENDING
Need: `RUNTIME BUILD 32` marker + the Willow post-scene line outcome (Continue proven → Willow entrance route at DUNGEON_WILLOW (2996,3494) resumes vs stage-20 dialogue HOLD). Screenshot feed dark since 2026-09-30 17:44 EDT (~32.4h) — stream is the only live visual source. State progression from Alex's live notes: B31's stage-20 instance fix worked; Burntof recruited (varbit 40); stage 20 active; next = entrance route, then stages 25/30; guardian (35+) still HOLD_GUARDIAN_UNIMPLEMENTED.
