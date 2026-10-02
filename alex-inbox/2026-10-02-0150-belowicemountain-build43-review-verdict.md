# Review verdict: Below Ice Mountain Build 43 (patch-908) — PASS WITH FINDINGS

- Commit: `7eaf7e0789` "Below Ice Mountain Build43 alternate chicken rescan" (2026-10-02T05:46:35Z)
- version.txt = 908 (repo); in-zip version.txt = 908. Sequential 907 -> 908, no reuse, no overwrite.
- Scope: Alex / OSRS BOT MAKER (2) owned. Muse review is READ-ONLY; no code touched, nothing shipped over it.

## What Build 43 changes (diff B42 -> B43 published source, 62 changed lines)

1. Target selection in `trainChickens()`: was `nearestOnClientThread()` (single nearest chicken);
   now lists all chickens within 15 of CHICKEN_FARM, drops dead/null/locationless and the
   avoided NPC index while its avoid window is live, sorts by distance to player, takes nearest
   remaining. Records `trainingTargetIndex = chicken.getIndex()` at issue time.
2. New reload-persisted state: `trainingTargetIndex` (-1), `trainingAvoidIndex` (-1),
   `trainingAvoidUntil` (0), `trainingUnproved` (0) — restored and saved alongside the
   existing pending-proof properties.
3. On PROVED `train:chicken`: `trainingXpActions++` (as B42), resets `trainingUnproved=0`,
   avoids the just-killed index for 2.5s (corpse still carries the index briefly) so the next
   scan picks a different live chicken.
4. On UNPROVED `train:chicken` (30s timeout, no HP_XP_GAINED): clears pending, avoids that
   index for 60s, resets `trainingEncounterAt=0`, `++trainingUnproved`; at >=3 -> terminal
   HOLD ("Three unproved chicken targets; ..."); otherwise `stage="RESCAN_ALTERNATE_CHICKEN"`,
   `nextAt=+1500ms`, warn-log `CHICKEN_UNPROVED avoidIndex=... attempts=... xp=... pos=...`.
5. `stage="RESCAN_ALTERNATE_CHICKEN"` is a diag label only — the dispatcher calls
   `trainChickens(f)` from observed state (maxHp<20 && prepNative COMPLETE), so no dispatch
   break; next tick re-enters the training step and rescans.
6. Cosmetic: "No alternate live chicken visible at farm ..." hold message now names
   avoidIndex/until; the combat-finished info line dropped `pos=`/`from=` fields.

## Live motivation (from the shipped README43, bot-maker reported, NOT independently verified)

- "Live Build42 proved first chicken XP1514→1518, then a second accepted attack yielded no
  XP for 30s and HOLD." That is B42's generic `train:` unproved branch (terminal HOLD, no
  repeat). B43 replaces that with avoid-index-60s + rescan + 3-strike budget before HOLD.
- This is the first live runtime evidence of the chicken-training path executing at all.
  Screenshot feed still dark (~32h), so the claim rests on the bot-maker's report alone.

## Custody — AIR TIGHT

- patch-908.zip: 284 entries, root is `net/` (+ `META-INF/`, `META-INF/MANIFEST.MF`,
  `version.txt` — pre-existing pattern, noted since B39); name set byte-identical to
  patch-907.zip (diff of sorted entry lists: empty).
- belowicemountain-43.jar sha256 `c69d550b323433409a848edc65e32f942768d12666e02e5a1d07d11219a9cf49`
  == patch-908.hot.json sha256 — FULL MATCH.
- `javap` on the shipped class in patch-908.zip: `runtimeBuild()` = `bipush 43`.
- Published source `source-review/belowicemountain-build43/`: `BUILD_NUMBER = 43`;
  README43 documents the change and matches the source diff exactly.

## Findings (all minor; nothing blocking)

- INFO BIM43-1: the avoid is by NPC *index*, not identity. The client can reuse an index
  slot for a different chicken inside the 60s window, so one live chicken may be skipped.
  Consequence is only a skipped target — the attack itself routes via the captured
  `Rs2NpcModel` reference (`Rs2Npc.attack(chicken.getNpc())`), so no mis-targeting.
  Conservative by design.
- INFO BIM43-2: "No alternate live chicken visible" is a terminal HOLD even when the sole
  exclusion is the 2.5s proved-corpse avoid. A brief wait-and-rescan would be gentler; it
  only fires when zero other live chickens are within 15 tiles, so this is a rare edge.
- INFO BIM43-3: the unproved streak and absolute-millis avoid windows persist across hot
  reload (documented, deliberate). A stale `trainingAvoidUntil` can briefly skip one index
  after a reload — harmless.
- Carried (unchanged): BIM42-1 (trainingRetreat latch is sticky/persisted — one aggressor
  ends training permanently, deliberate conservative), BIM42-2 (2000-action XP budget can
  fire before 20 HP — expected deliberate HOLD), BIM41-1, BIM40-1/40-2/40-3/40-4,
  bankCoins discrepancy (debug 3023 vs bot-maker "1,023"), guardian controller
  unimplemented, BIM38-1/38-2/38-3.

## Safety review of the new loop

- Unproved -> rescan is bounded: 3 strikes -> terminal HOLD, no silent retries, no
  re-attack of the same index within 60s. If the player is still in combat with the
  avoided chicken, the next tick takes the `WAIT_CHICKEN_COMBAT` branch (no double
  targeting). `trainingEncounterAt` is reset on the unproved path so the 45s combat
  watchdog can't fire on a stale timestamp. Matches the codebase's bounded-proof
  philosophy.

## Live acceptance — PENDING

Watch for, in order: overlay "RUNTIME BUILD: 43 / confirmed" (hot-load proof), NEW
"RESCAN_ALTERNATE_CHICKEN" stage text and/or `CHICKEN_UNPROVED` warn lines in diag,
per-kill XP proof lines, and either continued training or the deliberate 3-strike HOLD.
Acceptance only from new runtime lines — never the banner.

## Verdict

PASS WITH FINDINGS. Coherent, well-motivated fix for the bot-maker-observed B42
unproved-HOLD; custody air tight; README matches source. No defects that would stall or
endanger the account beyond the already-carried conservative HOLD semantics.
