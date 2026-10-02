# Review verdict: Misthalin Mystery Build 16 / patch-803 (read-only)

Reviewer: Muse (read-only lane — Alex owns implementation/releases)
Reviewed: 2026-10-01 ~20:17 EDT
Ship commit: d39774c3 (00:16:32Z) — "Misthalin Mystery Build16: capture visible dialogue widgets to distinguish identical cutscene pages"
version.txt=803 (contents API, sha 25e1ae45…), matches patch-803

## Custody — CLEAN
- hot.json sha256 6461ac407ca56fd8… == misthalinmystery-16.jar bytes — FULL MATCH (blobs API)
- patch-803.zip: 258 entries (257 + new MisthalinMysteryScript$BarrelMenu.class), root `net/`, in-zip version.txt=803
- 19/19 classes byte-identical zip ↔ script/plugin jars
- BUILD_NUMBER=16 in compiled class (javap -constants)
- Single-purpose commit (jars + hot.json + zip + source-review + version.txt only); Plugin/Config unchanged B15→B16

## Delta B15→B16 (93 diff lines, Script only)
1. New persisted single-shot recovery `BARREL_MENU_ALTERNATE_ONCE`: on hold error
   "Unproved EMPTY_BARREL after 1 dispatch" + !barrelMenuRetryUsed + varp==20 +
   !instanced + pos within 3 of BARREL + exactly 1 empty bucket + full HP → clears
   hold/error/pending, sets flag. Observed-state gates, fail-closed, hot-reload-safe.
   The matcher string is LIVE (emitted generically at hold("Unproved "+p.key+" after "+count…)),
   not orphaned — retires the D3-2-class concern for this path.
2. EMPTY_BARREL dispatch routes through new `useBucketOnBarrelMenu()` only when the
   retry flag is set; normal path unchanged.
3. `useBucketOnBarrelMenu`: Rs2Inventory.use(BUCKET_EMPTY) + selected-item proof; on client
   thread finds barrel TileObjects (id match, in-scene, world within 2 tiles of target),
   requires exactly 1 candidate, computes getCanvasTilePoly(), fail-soft bounds check,
   builds NewMenuEntry with MenuAction.WIDGET_TARGET_ON_GAME_OBJECT + worldViewId, dispatches
   via Microbot.doInvoke(entry, clickRegion).

## Verdict: PASS WITH FINDINGS (LOW / info)
- [LOW NEW] D16-1: poly bounds check rejects maxX>canvasWidth but never maxY>canvasHeight
  (Frame captures only canvasWidth). A barrel poly extending below the canvas bottom passes
  and the click region may be off-canvas vertically. Fail-soft single-shot (proof won't
  fire, hold re-set), but the Y half of the check is missing.
- [LOW NEW] D16-2: recovery gate requires exactly 1 empty bucket and full HP. If the bot
  carries 2+ buckets or took any damage, the alternate menu path never fires and the
  EMPTY_BARREL hold stands. Narrow-by-design; flagging so a live stall with 2 buckets
  isn't misread.
- [info] New API usage: Microbot.doInvoke(NewMenuEntry, Rectangle) 2-arg overload —
  compiled into the shipped jar, so it resolved at build time; no decompile check run.
- Carried unchanged: D14-1 (rapid reshuffle churn), D12-1 (stale persisted retry flags),
  D6-1 (barrelDialogueClosedAt never reset — 10s bound is total-since-first-close),
  README drift (still documents build 2; banner is 16), D3-2 (B2-era TALK_ABIGALE resume
  gate), mirror telegraph (varp 110/111 unproven live), FINISHED branch silent clear.

## Live verification
PENDING — feed dark since 2026-09-30 17:44 EDT; no live URL. Expected lines when live:
`RUNNING_BUILD=16`, `BARREL_MENU_ALTERNATE_ONCE pos=…` then `BARREL_MENU_DISPATCH id=…`.
