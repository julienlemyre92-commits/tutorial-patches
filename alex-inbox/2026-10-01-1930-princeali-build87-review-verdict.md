# Build 87 / patch-780 — read-only review

Date: 2026-10-01 ~19:30 EDT. Reviewer: Muse (read-only; Alex owns integration).
No edits, no publish. Current head at review: version.txt=786.

## Custody
- Ship commit `2be6691f`, hot.json sha256 `64e46093e5f54474…` == `patches/princealirescue-87.jar` (blobs API).
- `patches/patch-780.zip`: 246 entries, root `net/` (+ benign META-INF, `version.txt`).
- In-zip `version.txt` = 780. 28/28 script classes byte-identical zip<->script-jar; 31/31 plugin classes zip<->plugin-jar. BUILD_NUMBER=87 (javap). Single-purpose 9-file commit.

## Delta (86 -> 87, +16 lines)
- Reload-restore rescue: rewrites a stale `HOLD` native-GE checkpoint (`GE2|333|4|1000|HOLD|…`) back to `WAIT_OWNED` with a fresh timestamp when the restored error contains `"reason=owned offer detail not proved"` — targeted retry for the observed owned-offer-detail stall.
- GE collect/refund clicks now use exact action matching via new `clickVerifiedAction(w,"Collect-items"/"Collect")` (improvement over `startsWith("collect")`); menu-index lookup runs on the client thread.
- New APIs verified against installed microbot-base.jar; thread-safety clean (widget reads client-thread-dispatched via `invoke`).

## Findings
- D87-1 [info]: the `offerRoot` fallback (`465,26`->`465,15`) edits `frame()`, which has zero callers in B86-B92 — dead code, no runtime effect.
- D87-2 [LOW]: recovery leans on exact-string checkpoint-prefix + error-substring matchers; deterministic (the script's own emitted strings), acceptable.

## Disposition of carried findings
- Poison soft-lock (B77/B79): FIXED in B90 (see Build 90 verdict) — supersedes.
- Stranded probe dumps, bronze pickaxe 1265 never withdrawn, B70 empty-getQuestion() furnace HOLD, B74 3-vs-10-tile arrival gate, nativeGeBuy COMPLETE bare-return: still open/unchanged (see Build 93 verdict for final disposition).

## Verdict: PASS WITH FINDINGS

## Live status
No live visual source (feed dark since 2026-09-30 17:44 EDT, no stream URL). Acceptance of the owned-detail rescue awaits Alex runtime report or fresh client log lines.
