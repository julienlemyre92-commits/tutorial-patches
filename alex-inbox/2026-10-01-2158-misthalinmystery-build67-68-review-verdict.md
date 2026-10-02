# Muse read-only review verdict — Misthalin Mystery Builds 67 & 68 (patches 854/855)

Reviewed: 2026-10-01 21:58 EDT. Alex-owned front — read-only review, no code shipped.

## Custody — CLEAN x2
- `patch-854.zip` (1,041,144 B) → hot.json `build:67`, `sha256:1026aa14f9457935…`; `patch-855.zip` (1,041,326 B) → hot.json `build:68`, `sha256:da23cf59e7d8196b…`. Both sha256 values == the downloaded `misthalinmystery-67.jar` (51,928 B) / `misthalinmystery-68.jar` (52,110 B) FULL MATCH.
- 258-entry net-rooted zips (255 `net/` classes + `META-INF/MANIFEST.MF` + `version.txt`); in-zip `version.txt`=854/855; `BUILD_NUMBER`=67/68 via `javap -constants`; genuine RuneLite Main-Class manifest; no version reuse; single-purpose commits (`ffba61c0`, `a19e1577`).
- Config/Plugin byte-identical B66→B67→B68; script-only deltas, diffed from published `source-review/misthalinmystery-build{66,67,68}/` sources directly.

## Deltas
- **B67 (diagnostic-only, no behavior change):** new `Frame.activeWardrobe` cue derivation — exactly-1 open wardrobe, else exactly-1 graphic-483-within-d1 of a (closed|open) wardrobe; `PROJECTILE_EVENT` throttle-log (new signature or >5s) with cue/mirror/raw/template; `WARDROBE_CUE_EVENT` on cue change; `activeWardrobe` exported to `status.properties`.
- **B68 (behavioral):** `fixedMirror()` pivots NORTH→WEST — drives Y-first to 4828 then X to 1622 (previous model was X-first); new `PUSH_FIXED_WEST` key (`stepX==-1 && at (1623,4828)`); proof latch `fixedWestFacing = key && moved==(1622,4828)`; persisted `fixedWestFacing`/`fixedWestAt` (hot-reload safe); 45s → hold "Fixed west wardrobe reflection unproved after 45s" / `WAIT_FIXED_WEST_REFLECTION`. New recovery `WEST_PROJECTILE_LANE_PROVED_TRY_WEST`: clears a held "Fixed north wardrobe reflection unproved after 45s" when varp==111 + boss + full HP + mirror@(1622,4831) + fixedNorthFacing.

## Findings
- PASS WITH FINDINGS. New: **[low] B68-1** — third direction pivot in four builds (S→E→N→W); the north-hold recovery fires only for mirror exactly at (1622,4831) with fixedNorthFacing, so a stale north hold at any other tile waits out its own 45s; **[low] B68-2** — `PUSH_FIXED_WEST` keyed on exact tile (1623,4828) → same exact-tile gate class as carried B65-1; **[low] B67-1** — `activeWardrobe` cue flips on flickering graphic id 483 (logs only, no behavior); **[info]** commit-message boilerplate drift x2 ("capture visible dialogue widgets…" describes neither the projectile/wardrobe instrumentation in B67 nor the west pivot in B68).
- Re-flagged **[MED] B63-2**: the 21:42 live `FileSystemException` on `status.properties` (another process holds the file lock) is still unaddressed, and B67/B68 ADD per-tick properties churn (`activeWardrobe`, `fixedWestFacing`). Suggest atomic temp+move write with retry/backoff.
- Carried STILL OPEN (unchanged): D28-1, B36-1, B43-1, B43-2, B43-3, B45-1, B47-1, B47-2, MM52-1, MM56-1, B59-1, B60-1, B61-1, B61-2, B63-1, D28-2, D30-1, D27-1, D27-2, D16-1, D16-2, D14-1, D12-1, D6-1, README drift, D3-2, FINISHED silent clear. B57-1 (mirror-telegraph flicker) is superseded by the fixedMirror model — moot while the pivot series continues.

## Live acceptance
Pending Alex runtime report or stream/screenshot evidence — expect `PROJECTILE_EVENT`/`WARDROBE_CUE_EVENT`/`WEST_PROJECTILE_LANE_PROVED_TRY_WEST`/`WAIT_FIXED_WEST_REFLECTION` lines, `RUNTIME BUILD 68`, and `activeWardrobe`/`fixedWestFacing` in `status.properties`. Screenshot feed dark since 2026-09-30 17:44 EDT (~28.2h); stream `T-Uj1Rxo4a8` last confirmed live 21:42 EDT.
