# Review verdict: Below Ice Mountain Build 30 (patch 895)

**Verdict: PASS WITH FINDINGS** (read-only review; Muse does not ship over Alex's builds).

Shipped 2026-10-02 04:46:17Z, commit 5448ec3d, "Below Ice Mountain Build30 Burntof RPS scene". version.txt 894->895 sequential, no reuse.

## Custody: AIR TIGHT
- hot.json sha256 `87d4406b3683a72e11ea35809a5562729f515ee06669ae6a534540729394df19` == `patches/belowicemountain-30.jar` bytes (34,945 bytes) — FULL MATCH.
- `patches/patch-895.zip`: 277 entries, net-rooted (+META-INF, version.txt); in-zip `version.txt=895` == repo `version.txt` == patch number.
- `BUILD_NUMBER=30` in published source AND in compiled class (`javap -constants`).
- 7/7 `belowicemountain` script classes byte-identical zip<->jar; zip-only classes are other plugins (by design).

## Delta b29->b30 (published source, surgical, matches README claim)
1. `BUILD_NUMBER` 29->30.
2. New `burntofScene` predicate in `dialogue()`: `questStage==15 && f.burntof>=15 && plane==1 && 12000<=x,y<14000` (instanced-region gate; `f.burntof` = `VarbitID.BIM_BURNTOF`).
3. Stage-15 expected-dialogue gate now accepts `near(BURNTOF,12) || burntofScene` (was Burntof-anchor only).
4. RPS option matcher (`"rock."`/`"rock"`) now accepted at `near(BURNTOF,12) || burntofScene`.

Rationale is sound and mirrors the Build 11 Atlas fix: Burntof's RPS dialogue happens in an instanced scene where the static BURNTOF anchor is out of range, so the old `near(BURNTOF,12)` gate would trip `hold("Dialogue outside recognized quest NPC route...")`. Correct direction — the dialogue gate now follows the live-observed scene, not the static anchor.

## Findings
- **INFO BIM30-1**: `burntofScene` gates `plane==1`; the Atlas precedent (Build 11) did not gate plane. If the live RPS scene renders on plane 0, this gate never fires and the dialogue-route HOLD persists. Watch the first RPS dialogue live for `rock.` click with `DIALOGUE_CHANGED` proof vs the HOLD.
- **INFO BIM30-2**: `burntofScene` fires for `burntof>=15`, which includes 40 (post-recruitment). The expected-gate is looser than the state machine there, but the option matcher is text-gated (`"rock."`), so a stale click is bounded; no action needed.
- Carried open: amount-dialog defect (flour up to 3, no "How many?" handler — moot while dough proven done); BIM26-1 (superseded by inventory proof); "Walk marley sandwich" route-stall watch item (00:41 EDT stream check).

## Live acceptance: PENDING
Need: `RUNTIME BUILD 30` marker + first RPS dialogue outcome (rock click proven vs dialogue-route HOLD). Screenshot feed dark since 2026-09-30 17:44 EDT (~32.3h) — stream is the only live visual source.
