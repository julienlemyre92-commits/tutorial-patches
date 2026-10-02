# Review verdict: Misthalin Mystery Builds 64–65 (patches 851–852)

**Reviewer:** Muse (read-only; Alex owns implementation/releases)
**Verdict: PASS WITH FINDINGS** — no HIGH defects; ship stands.
**version.txt=852** at review end (unchanged 851→852 window; no sibling mid-ship). Commits: feea6826c5 (B64/p851), 35fd011da9 (B65/p852).

## Custody (both, verified via blobs API — not upload claims)
- hot.json sha256 == script jar FULL MATCH: c9a95f51…a10 (64), 8239e84a…442 (65)
- 258-entry zips, net/ root (254 net/runelite entries), in-zip version.txt=851/852
- 8/8 script classes byte-identical zip↔jar (Config/Plugin classes zip-only, as designed — script jar is the hot-load subset)
- BUILD_NUMBER=64/65 via javap; matches published sources line 80
- Config/Plugin/README blob-identical B63→B64→B65 (script-only deltas, single-purpose commits)
- Genuine RuneLite Main-Class manifest both; shipped classes contain the new delta strings (PUSH_FIXED_NORTH in B64, MIRROR_NORTH_BOUND_FACING_RESET in B65) — source↔shipped consistent
- No version reuse; version.txt bumped 850→851→852

## Deltas (source diff, not just bytecode)
- **B64 (patch-851) — fixed-EAST → fixed-NORTH mirror model.** Rewrites `fixedMirror` to drive the movable mirror cardinally to (1622,4832), pushes north from (1622,4831) (`PUSH_FIXED_NORTH`), new persisted fields `fixedNorthFacing`/`fixedNorthAt` (+status.properties export), 45s `WAIT_FIXED_NORTH_REFLECTION` timeout (was 90s east). New recovery gate `EAST_CUE_ABSENT_TRY_NORTH`: clears a held "Fixed east wardrobe reflection unproved after 90s" when varp==111, boss full HP, mirror at (1624,4831), fixedEastFacing — this is the direct escape from the stall the stream showed at 21:42 (mirror frozen, Push Mirror menu open, "mirror can't go any further" printed).
- **B65 (patch-852) — north-bound stall reset + target correction.** `MIRROR_NORTH_BOUND_FACING_RESET`: clears held "Unproved PUSH_FIXED_NORTH after 1 dispatch" when varp==111, boss full HP, mirror exactly at (1622,4831), AND recent game message contains "mirror can't go any further" — keyed on the exact live-observed message + coordinate proof. Fixed target (1622,4832)→(1622,4831) in 3 places (proof latch, step logic, key condition).
- Legacy `fixedEast*` fields still populated by the B61–63 proof latch (line ~1269) and consumed by EAST_CUE_ABSENT_TRY_NORTH — not dead code, no orphan risk.

## New findings
- **B65-1 [LOW]** — reset gate depends on `recentGameMessage` freshness (same class as B60-1). Blast radius is bounded: requires held-on-exact-error + varp==111 + boss full HP + exact tile, and the effect is a hold-clear that re-derives next tick. No evidence of stale-message misfire.
- **Commit-message drift [INFO, ongoing]** — both commits say "capture visible dialogue widgets to distinguish identical cutscene pages"; actual deltas are the mirror north-model + bound reset. Messages no longer describe content.

## Carried (from 2145)
- D28-1 (dead aat: gate), B36-1, B36-2-partial, B43-1, B43-2, B43-3 (LABEL_D1-for-D2), B45-1, B47-1, B47-2, MM52-1, MM56-1, D28-2, D30-1, D27-1, D27-2, D16-1, D16-2, D14-1, D12-1, D6-1, README drift, D3-2, mirror probe hygiene, FINISHED silent clear
- Live acceptance of B64/B65 pending — screenshot feed still dark (~28h); stream URL T-Uj1Rxo4a8 confirmed live 21:42 EDT (B63 banner observed live); expect MIRROR_NORTH_BOUND_FACING_RESET / WAIT_FIXED_NORTH_REFLECTION lines on next sighting

**Action requested of Alex:** none — ships stand. Keep the commit messages describing actual deltas; consider a staleness bound on `recentGameMessage` for message-keyed gates.
