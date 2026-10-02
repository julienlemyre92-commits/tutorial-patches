# Review verdict: Misthalin Mystery Builds 57–60 (patches 844–847) — 2026-10-01 21:35 EDT
Reviewer: Muse (read-only). Verdict: **PASS WITH FINDINGS** — no HIGH defects.

## Custody (all four builds)
- hot.json sha256 == patches/misthalinmystery-{57..60}.jar FULL MATCH (verified via git-blobs raw download):
  B57 2093ae92c578… (50,084B), B58 c3ad1ca4cd55… (50,331B), B59 666a6d7b8af3… (50,459B), B60 fd2915053ed4… (50,686B).
- patch-844..847.zip: 258 entries each; all 243 classes net-rooted; version.txt at zip root == patch number (844/845/846/847); genuine RuneLite Main-Class manifest (META-INF/MANIFEST.MF, Main-Class: net.runelite.client.RuneLite).
- 8/8 script classes byte-identical zip <-> jar (0 differ, per build).
- BUILD_NUMBER=57/58/59/60 via javap -constants on the shipped jars.
- MisthalinMysteryPlugin.java and MisthalinMysteryConfig.java byte-identical B56->B60 (script-only deltas).
- Commits: e4d7608d (B57), 98d50704 (B58), 26043367 (B59), 7b411351 (B60); version.txt=847 at review time.
- [INFO] Commit messages for all four again say "capture visible dialogue widgets to distinguish identical cutscene pages" but the actual deltas are mirror-boss-fight gates — boilerplate drift continues (same class as B48–56 note).

## Deltas (source-review diffs B56->B57->B58->B59->B60, verified against shipped sources)
- **B57**: reworked the "No unique stable wardrobe telegraph after 30s" recovery gate (varp 110/111, boss, full HP, mirror NPC present, pos!=null). Instead of requiring the latched mirrorCueWardrobe + 650ms clock, it now re-derives the cue live: exactly-1 open wardrobe (MISTMYST_BOSS_WARDROBE_OPEN) wins; else the set of wardrobes (open+closed) with a live graphic within distance<=1 — exactly 1 unique match wins. On a derived cue: logs MIRROR_TELEGRAPH_REFRESH, re-arms mirrorCueWardrobe/mirrorCueAt/mirrorCueLastSeenAt, mirrorCueSeen=false (flips true 650ms later via the unchanged inference path), clears the hold. Directly answers MM53-1 (stability no longer asserted from the 650ms clock; cue re-derived from observed state each tick). Fail-closed on ambiguity (needs exactly 1) — correct. This is the recovery path for the 30s hold firing on stale state (e.g. mirrorCueLastSeenAt not surviving a hot reload).
- **B58**: diagnostic-only. New `mirrorCollision` probe: during NPC scan, when the movable-mirror NPC has a local location, reads collision-map flags at its tile (center + E/W/N/S as hex) into f.mirrorCollision, persisted to status.properties. No behavior change — observe-first for the mirror-push pathing Alex was debugging live.
- **B59**: (1) new recovery gate on `Unproved PUSH_MIRROR_-1_0 after 1 dispatch` (varp==111, boss, full HP, mirror NPC at exactly (1622,4831), mirrorCueWardrobe==(1619,4828), recentGameMessage contains "mirror can't go any further") → logs MIRROR_WEST_BLOCKED_TRY_SOUTH, clears the hold; next tick's inference then pushes south. (2) step-chooser tie-break `Math.abs(dx)<=Math.abs(dy)` → `<`: exact-diagonal offsets (|dx|==|dy|) now prefer the vertical (Y) step instead of X.
- **B60**: (1) new recovery gate on `Unproved PUSH_MIRROR_-1_0 after 2 dispatch` (varp==111, boss, full HP, mirror NPC at exactly (1622,4828), "mirror can't go any further" in the game message) → logs MIRROR_WEST_BOUND_PROVED, clears the hold. (2) east-step override in the push inference: mirror at (1622,4828) + cue (1619,4828) + !mirrorFacingWardrobe → stepX=1 (move east once, then the normal west push re-aligns). (3) west-bound guard: stepX<0 && movable.x<=1622 → 30s-bounded hold ("Mirror west-bound cue persisted without a reachable alternative") else phase WAIT_REACHABLE_MIRROR_CUE_WEST_BOUND, no push issued. mirrorFacingWardrobe is persisted via the saved map (lines 295/338) and reset on cue change — hot-reload safe.

## New findings
- [LOW] B57-1: while ANY cue is continuously visible, the REFRESH/inference path refreshes mirrorCueLastSeenAt every tick, so the 30s telegraph hold (anchored on max(stageAt, mirrorCueLastSeenAt)) can never fire mid-visibility. The flickering-cue corner (cue changes every <650ms forever) sits in WAIT_UNIQUE_MIRROR_TELEGRAPH with no absolute timeout — MM56-1's concern persists there.
- [LOW] B59-1: the tie-break flip changes the push axis on exact diagonals (|dx|==|dy| now steps Y, was X). Needs live confirmation that the observed misfire was a wrong-X push; if the mechanic needed X on ties, this regresses.
- [LOW] B60-1: the two "can't go any further" gates are exact-tile narrow (1622,4831 / 1622,4828) and keyed on recentGameMessage — a stale message from an earlier blocked push could clear an unrelated unproved hold. Fail-safe direction (keeps the loop trying), but the MIRROR_WEST_BLOCKED_TRY_SOUTH / MIRROR_WEST_BOUND_PROVED lines would mislabel it.
- [INFO] Boilerplate commit-message drift (see Custody).

## Carried (verified still open in B60 source)
- D28-1 STILL OPEN: `"aat:"` exactly 1 occurrence (line 768), still no producer — the gate is dead code.
- B36-1 STILL OPEN: observeTreeDialogueClosedAt stamped while the cutscene is merely observed (lines 1699–1701), not when dialogue closes — the 15s hold can fire mid-dialogue.
- B43-1, B43-2 STILL OPEN (piano recovery silent-loop / edge-case terminal hold).
- B43-3 STILL OPEN: piano case 3 ("PIANO_D2") still presses InterfaceID.MistmystPiano.LABEL_D1 (line 1857) while a distinct LABEL_D2 exists — possible wrong-note root cause.
- B45-1, B47-1, B47-2, MM52-1, MM56-1 (see B57-1), D28-2, D30-1, D27-1, D27-2, D16-1, D16-2, D14-1, D12-1, D6-1, README drift, D3-2, mirror telegraph (B57/B58 actively addressing), FINISHED-silent-clear.
- MM53-1 CLOSED by B57 (cue re-derived from live state; 650ms clock no longer the stability assertion in the recovery path).
- MM54-1 closed by B55 (unchanged).

## Live acceptance
Pending. Last eyes-on: stream 21:24–21:28 EDT (RUNTIME BUILD 53/confirmed). Screenshot feed dark since 2026-09-30 17:44 EDT (~27.9h). Expect: RUNNING_BUILD=57..60, MIRROR_TELEGRAPH_REFRESH / MIRROR_WEST_BLOCKED_TRY_SOUTH / MIRROR_WEST_BOUND_PROVED lines, mirrorCollision in status.properties.
