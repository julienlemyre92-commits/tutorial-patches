# Review verdict: Misthalin Mystery Builds 39/40/41/42/43 (patches 826-830)
Reviewer: Muse (read-only) | 2026-10-01 ~21:06-21:12 EDT | run scheduled 21:05:52 EDT
Scope: OBSERVE -> UNDERSTAND, no ACT over Alex's builds. version.txt=830 at run end (B43).

## Verdict: PASS WITH FINDINGS (all five)

## Custody (verified via GitHub API, not upload claims)
| Build | patch | hot.json sha256 == jar | jar | plugin jar | zip entries | net/ root | in-zip version.txt | BUILD_NUMBER (javap) | manifest |
|---|---|---|---|---|---|---|---|---|---|
| 39 | 826 | FULL MATCH | 47,826 B | 56,412 B | 258 | yes | 826 | 39 | genuine RuneLite Main-Class |
| 40 | 827 | FULL MATCH | 47,914 B | 56,502 B | 258 | yes | 827 | 40 | genuine RuneLite Main-Class |
| 41 | 828 | FULL MATCH | 47,938 B | 56,526 B | 258 | yes | 828 | 41 | genuine RuneLite Main-Class |
| 42 | 829 | FULL MATCH | 48,139 B | 56,727 B | 258 | yes | 829 | 42 | genuine RuneLite Main-Class |
| 43 | 830 | FULL MATCH | 48,296 B | 56,883 B | 258 | yes | 830 | 43 | genuine RuneLite Main-Class |
- 8/8 script classes byte-identical zip<->jar on each build (overlay-safe).
- Config/Plugin/README byte-identical B38->B43 (script-only deltas).
- Shipped-class strings confirm new paths landed in the right builds:
  outsideNoteProbe in B39+, OUTSIDE_CLUE_BLOCKED_SOUTH_REROUTE in B42+,
  PIANO_D1_PREFIX_PROVED in B43 only.
- Single-purpose commits; commit titles identical boilerplate
  ("capture visible dialogue widgets to distinguish identical cutscene pages").
- NOTE: these builds DO track version.txt (B39->826 ... B43->830); the
  "hot-channel, not version.txt-tracked" note in the 21:05 run's log is
  superseded -- patch-N.hot.json is the hot-load fingerprint, version.txt
  still advances per build.

## Deltas (source-review diffs, B38 baseline 1938 lines)
- **B39** (1960 lines, diagnostic-only): new `outsideNoteProbe` telemetry at
  varp==70 on MISTMYST_CLUE_OUTSIDE_VIS -- live collision-flag hex
  (center/E/W/N/S) around the note object -> status.properties.
- **B40** (1964 lines, diagnostic-only): probe records "seen=" first (proves the
  object was observed even when collision maps are null), falls back to the
  client's collision maps when o.getWorldView()==null.
- **B41** (1965 lines, diagnostic-only): probe trigger widened to also match
  MISTMYST_CLUE_OUTSIDE (non-VIS id) at varp==70.
- **B42** (1980 lines, BEHAVIORAL): new `outsideNoteEastOpen` boolean
  (E tile of note walkable E/W from live collision flags); new recovery gate on
  "Unproved TAKE_OUTSIDE_CLUE after 2 dispatch" -- varp==70, outside, full HP,
  no clue, east open, !inDialogue -> clears hold + reroutes via
  APPROACH_OUTSIDE_CLUE_EAST to (1633,4850) dist 0.
- **B43** (1989 lines, BEHAVIORAL): piano step at varp==75. New
  PIANO_D1_PREFIX_PROVED gate clears the persisted
  "Piano wrong-note flag set; inspect reset control before resuming" hold when
  varp==75, pianoWidget visible, full HP, attempts==1, D1==1 ("resume E").
  The old terminal `PIANO_DEAD>0` hold block is DELETED from piano(); replaced
  with prefix-mismatch holds: attempts>=1&&D1!=1, attempts>=2&&E!=1,
  attempts>=3&&A!=1 -> hold "Piano sequence prefix mismatch attempts=N".

## API verification (javap against installed microbot-base.jar)
MISTMYST_CLUE_OUTSIDE, MISTMYST_CLUE_OUTSIDE_VIS (ObjectID),
MISTMYST_PIANO_ATTEMPTS/D1/E/A/D2/DEAD (VarbitID),
InterfaceID$MistmystPiano.LABEL_D1/E1/A2 (+C1/F1/G1/A3/B2/C2/D2/E2/F2/G2/B3),
CollisionDataFlag.BLOCK_MOVEMENT_EAST/WEST -- ALL present.

## NEW findings
- [LOW NEW] B43-1: PIANO_D1_PREFIX_PROVED clears the hold but never verifies
  the PIANO_DEAD flag actually cleared and presses no reset control. If the
  game's dead-note state still blocks input, attempts stays 1, D1 stays 1, and
  the prefix-mismatch gate never fires -- the bot presses E every tick with NO
  hold string (silent invisible loop). Suggest: bound the recovery (e.g. count
  E presses post-clear; re-hold if attempts doesn't advance within N ticks).
- [LOW NEW] B43-2: the recovery gate requires exactly attempts==1 && D1==1.
  A persisted wrong-note hold with any other attempts value now has NO
  producer (B43 deleted the PIANO_DEAD hold block) and NO recovery gate --
  terminal hold with no path out. Edge case, fail-closed.
- [LOW NEW] B43-3 (surfaced by this read; code predates window, never flagged):
  piano case 3 is labeled "PIANO_D2" but presses
  `InterfaceID.MistmystPiano.LABEL_D1`, while a distinct `LABEL_D2` exists in
  the interface. If the quest melody's final note is the octave-2 D key, this
  IS the wrong-note root cause B43 is recovering from. Needs Alex/live
  confirmation of the actual required melody.

## Info
- [info] B42-1: reroute gate clears the hold before arriving (clear-now /
  route-next-tick, same pattern as D27-2) -- fail-safe, re-gated on fresh
  outsideNoteEastOpen per tick.
- [info] B39-41: diagnostic-only, defensively widened; zero stage-logic change.

## Carried (verified still present in B43 source)
- D28-1 STILL OPEN: "aat:" exactly 1 occurrence, dead gate, no producer.
- B36-1 STILL OPEN: observeTreeDialogueClosedAt stamped while dialogue open.
- D28-2, D30-1, D27-1, D27-2, D16-1, D16-2, D14-1, D12-1, D6-1, README drift,
  D3-2, mirror telegraph, FINISHED silent clear.

## Live acceptance (pending -- feed dark since 2026-09-30 17:44 EDT)
Expect RUNNING_BUILD=39..43, outsideNoteProbe in status.properties,
OUTSIDE_CLUE_BLOCKED_SOUTH_REROUTE / PIANO_D1_PREFIX_PROVED lines.
Last confirmed live build: B38 at 20:55 EDT (stage 70, stream).
