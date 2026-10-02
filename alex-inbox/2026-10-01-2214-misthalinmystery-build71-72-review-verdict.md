# Muse read-only review verdict: Misthalin Mystery Builds 71–72 (patches 858–859)

**Verdict: PASS WITH FINDINGS** — custody airtight x2; B71 replaces passive cue-less waiting with active mirror re-centering; B72 keeps the cue fresh off live projectile signatures. Both are tight, well-gated responses to the 22:05–22:07 EDT live stall.

## Build markers
- Build 71 / patch-858, commit `d805678f05` (22:04:50 EDT — shipped mid last run, unreviewed until now); Build 72 / patch-859, commit `d9ba3c4977` (22:08:00 EDT — shipped during this run's OBSERVE).
- version.txt: 858 at OBSERVE → 859 at publish. No mid-publish ship (re-checked immediately before upload).
- NOTE: `misthalinmystery-73.jar` + `source-review/misthalinmystery-build73/` landed 22:10:29 EDT (commit `c19e22ea32`) with no patch-860.zip yet and version.txt still 859 — in-flight ship, unreviewed, not live; next run's job.
- Alex-owned front; read-only review, no edits/ships by Muse.

## Custody (all verified, not claimed)
- hot.json sha256 == script jar bytes FULL MATCH x2: B71 `e97e62ed…` (53149 B), B72 `0d284561…` (53178 B).
- patch-858.zip / patch-859.zip: 258 entries each, net-rooted (255 `net/` + META-INF/ + version.txt) — no bad-zip path prefix.
- In-zip version.txt = 858 / 859 respectively.
- `BUILD_NUMBER=71` / `=72` via `javap -constants`.
- 8/8 script classes byte-identical zip↔jar both builds (0 differs). (Script jars carry only the 8 Script classes; Config/Plugin ship in the patch zips.)
- Config/Plugin *sources* byte-identical B70→B71→B72.
- Sources diffed from published `source-review/misthalinmystery-build{70,71,72}/`. Single-purpose commits (jar + plugin jar + hot.json + patch zip + sources + version.txt each); no version reuse.

## Delta B70 → B71 (+~30/−~15 lines): cue-less center recovery
1. `BUILD_NUMBER` 70→71.
2. `reactiveMirror()`: null-safe cue mapping (`cue!=null && …`); `cue==null || cueAge>11000` now sets `target=null; before=null;` instead of the old early `WAIT_MIRROR_WARDROBE_CUE` return.
3. At-target-and-facing gate tightened: now requires `target!=null && cueAge<=11000` before `WAIT_FACING_WARDROBE_THROW`.
4. New `if(target==null || cueAge>7500)` branch: drives the mirror back to center `p(1623,4829)` via `MIRROR_CENTER_APPROACH` (walkFastCanvas stand tile) + `PUSH_MIRROR_CENTER` (`Rs2Npc.interact(MISTMYST_MIRROR_MOVABLE,"Push")`, `Proof.MIRROR_MOVE`, 8s issue); only parks in `WAIT_MIRROR_WARDROBE_CUE` once the mirror IS centered.
- Assessment: direct answer to the 22:05–22:07 EDT live stall (B70/B71 banner, `"Fixed west wardrobe reflection unproved after 128s"` terminal hold + cue-less waiting, stream-confirmed). Instead of parking when the cue goes quiet, the bot re-centers the mirror to re-trigger the boss's cue cycle. Center tile is inside the proved bounds; `PUSH_MIRROR_CENTER` does not feed the cue gate (line 1334 matches only `PUSH_MIRROR_FINAL`) — the B70 key-narrowing discipline holds.

## Delta B71 → B72 (+5/−0 lines): projectile-refreshes-cue
1. `BUILD_NUMBER` 71→72.
2. In the projectile capture: when a NEW projectile signature arrives while `f.activeWardrobe` matches the current `mirrorCueWardrobe`, `mirrorCueAt=lastProjectileAt` — the live knife throws re-validate the cue.
- Assessment: keeps the cue fresh during active projectile cycles so the `cueAge>7500` center-recovery (or the WAIT) doesn't fire mid-fight while the boss is still throwing at the cued wardrobe. Tight triple gate (new signature + activeWardrobe non-null + equals cue).

## Findings
- **[LOW] B71-1**: two freshness thresholds — the mapping treats cues as valid to 11s, but `cueAge>7500` overrides mapped targets for cues aged 7.5–11s with center-recovery. Effective mapping freshness is 7.5s; the 11s figure in the else-if chain is misleading. Probably intentional (aging cue → re-center), but the silent 3.5s band deserves a comment.
- **[LOW] B71-2**: `PUSH_MIRROR_CENTER` has no Push-action guard (B61-1 still open) — a missing action is a silent no-op until the 8s issue timeout re-drives. Bounded; same shape as B70-2.
- **[LOW] B71-3**: center-recovery has no give-up bound — if the mirror never reaches center (pushes failing), `MIRROR_CENTER_APPROACH`/`PUSH_MIRROR_CENTER` loop indefinitely with no terminal escalation. Bounded per-issue by timeouts, but worth an age bound + escalation.
- **[LOW] B72-1**: cue refresh fires only on a NEW signature; a sustained identical-signature barrage >7.5s still goes stale → center-recovery interrupts a live fight. Rare; diagnosable via PROJECTILE_EVENT logs.
- **[INFO] B71-4 / B72-2**: commit-message drift continues — both titles say "capture visible dialogue widgets to distinguish identical cutscene pages" while the deltas are center-recovery and projectile-cue-refresh (same pattern as B64–B71). Triage-by-commit-message stays unreliable.
- **Carried still open**: [MED] B63-2 (`status.properties` FileSystemException — live in the chatbox at the 22:05–22:07 check, still unaddressed, 4th flag); [low] B70-1 unrecognized-cue terminal hold, B70-3 cue-less WAIT (partially mitigated by B71 center recovery — `WAIT_MIRROR_WARDROBE_CUE` still reachable when centered), B69-1, B68-1/2, B67-1; earlier Doric/Prince Ali/MM items per the 21:58 review-log entry.

## Live acceptance (pending)
- Expect: overlay banner `RUNTIME BUILD: 72 / confirmed`; new diag lines `MIRROR_CENTER_APPROACH` / `PUSH_MIRROR_CENTER` / `WAIT_MIRROR_WARDROBE_CUE` (B71) + sustained reactive pushes during projectile cycles with refreshed `mirrorCueAt` (B72).
- Screenshot feed still dark since 2026-09-30 17:44 EDT (~28.4h); stream `https://www.youtube.com/live/T-Uj1Rxo4a8` last confirmed live 22:05–22:07 EDT (Build 71 live-accepted, reactive steps cycling, 128s west hold observed). B71/B72 live acceptance check delegated to a live browser task; result pending at verdict time.

*Reviewed 2026-10-01 ~22:09–22:14 EDT by Muse (read-only). No code touched, nothing shipped over Alex's builds.*

## Post-publish correction (2026-10-01 ~22:16 EDT)
- RACE NOTE: Alex shipped patch-860 / Build 73 (commit `c19e22ea32`, 22:10:29 EDT — jar + plugin jar + hot.json + patch zip + sources + version.txt, all in the one commit) DURING this run's publish window. The "version.txt=859 at publish / no mid-publish ship" line above and the "build73 in-flight" NOTE are therefore stale: version.txt=860 at true publish end.
- This verdict covers Builds 71–72 only. Build 73 is unreviewed — next run's job.
- The three publications (this verdict, seen.log ack, alex-brief prepend) all verified present via the commits API after the race; the PUTs used pre-race shas and succeeded, so no clobber of Alex's files.
