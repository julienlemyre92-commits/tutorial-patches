# Review verdict: Below Ice Mountain Build 11 (patch-876) — READ-ONLY

Reviewed by: Muse (read-only; Alex owns implementation/releases; no ship)
Commit: 2cc21cc0 ("Below Ice Mountain Build11 Atlas scene region", landed 2026-10-01 23:36:27 EDT)
Verdict: **PASS WITH FINDINGS**

## Custody — AIR TIGHT
- hot.json `patch-876.hot.json` sha256 `f49a93a8cbf161998067fd0537cfdb725ce33551d850dcbd2afbd7c45ad733cf` == `patches/belowicemountain-11.jar` bytes — FULL MATCH.
- `patch-876.zip`: 276 entries, net-rooted (zip-built, no manifest issue), in-zip `version.txt` = `876` == repo `version.txt` (876 at review time) == patch number.
- `BUILD_NUMBER=11` in published source and compiled class constant — no lying banner.
- Single-purpose commit; version 875→876 sequential, no reuse, no overwrite.

## Diff vs Build 10 (published source, 615→616 lines) — surgical
1. `BUILD_NUMBER` 10→11.
2. The single-tile `ATLAS_SCENE` constant + `near(...,30)` gate replaced by an instanced-region gate: `f.checkal>=10 && f.position!=null && 12000<=x<14000 && 12000<=y<14000`. Null-guarded.
Nothing else changed.

## Design rationale (per source-review README §Build 11)
After Build 10's hot load, the observed cutscene world position moved from (12806,12237) to (12867,12275) — outside Build 10's 30-tile radius, so the dialogue gate dropped. Build 11 accepts stage-10 dialogue anywhere in the instanced 12000–13999 x/y band, only with Checkal varbit>=10, retaining the outdoor NPC proximity checks, per-click dialogue-change proof, and two-failure HOLD. It corrects a moving scene location without granting any movement or combat permission there. No guardian logic added.

## Findings
- INFO BIM11-1 (resolves INFO BIM10-1): the region gate fixes the proven coordinate drift. Residual: if a future cutscene instance lands outside the 12000–14000 band, the gate drops dialogue and the script terminal-HOLDs — the HOLD log will show the out-of-band position, so it is diagnosable, and terminal-HOLD is the intended safety posture there. The gate additionally requires stage-10 dialogue + checkal varbit>=10, so the region band alone cannot misfire on an unrelated instance.
- Minor: region check does not constrain plane; harmless given the dialogue+varbit conjunction.
- Carried: INFO BIM9-1 (low-byte decode assumption), LOW BIM7-1 / LOW BIM7-2 (route progress), LOW BIM2-2 (Mining-10 gate), INFO BIM2-4.

## Live acceptance
PENDING — RUNTIME BUILD 11 marker not yet observed on stream (23:35 check showed Build 9/confirmed). Watch the next routine stream window (~23:40/23:41) for the Build 11 hot-load line, whether the script resumes from "paused for review"/preflight, and whether Checkal→Atlas proceeds on the decoded stage.

No shipping action taken — read-only.
