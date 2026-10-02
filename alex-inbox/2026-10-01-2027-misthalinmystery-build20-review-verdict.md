# Review verdict: Misthalin Mystery Build 20 (patch-807) — PASS WITH FINDINGS

Reviewed: 2026-10-01 20:27 EDT. Builds 18 and 19 are folded into this verdict —
all three shipped within ~4 min (20:20/20:22/20:24 EDT) on the same theme and
build 20 supersedes them; the cumulative 17→20 script delta was reviewed whole.

## Custody — CLEAN
- Commit `6ef0a51a` single-purpose: adds `patches/misthalinmystery-20.jar`,
  `patches/misthalinmystery-plugin-20.jar`, `patches/patch-807.hot.json`,
  `patches/patch-807.zip`, `source-review/misthalinmystery-build20/*`,
  version.txt 806→807 (fresh number, no reuse, no overwrite).
- `misthalinmystery-20.jar` sha256 `466aea919f14638acc031891f3c5228433b7ff78abe3164fa56c767fb7457101`
  == `patch-807.hot.json` sha256 — FULL MATCH via git blobs API.
- `patch-807.zip`: 258 entries, `net/` rooted, in-zip `version.txt`=807.
- Plugin/Config byte-identical to build 19 (same blob SHAs `493a30aa`/`8f3d880b`).
- Shipped `MisthalinMysteryScript.class` in the hot zip is byte-identical to the
  jar's class (`38990df6…`) and contains the new code — no stale classes.
- `BUILD_NUMBER=20` in source-review; banner logs `[MisthalinMystery] RUNNING_BUILD={}`.

## Delta 17→20 (attributed)
- Build 18: `libraryClueMenuRetryUsed` single-shot gate (clears HOLD on
  "Unproved TAKE_LIBRARY_CLUE after 1 dispatch"; varp==35, island, 0 library
  clues, full HP, within 6 of NOTE1) + new `safeObjectMenuAction` (first scoped
  to the library-clue retry).
- Build 19: `safeObjectMenuAction` generalized to ALL chosen object actions
  (replaces `Rs2GameObject.interact(obj,chosen)`); `cutPaintingMenuRetryUsed`
  gate (varp==40, exactly 1 KNIFE, full HP, within 6 of PAINTING); item-on-object
  generalized to `useItemOnObjectMenu` (replaces bucket-only barrel path).
- Build 20: diagnostic-only — `selectedWidgetMeta` (`client.getSelectedWidget()`
  id/itemId/name, captured inside `observe()` which runs on the client thread)
  and `paintingObjects` (MISTMYST_PAINTING count) persisted to status properties.

## Correctness review
- `safeObjectMenuAction`: client-thread re-resolution of the live object
  (strictly better than the old dispatch-time `obj`, avoids stale references);
  exactly-one-candidate guard (warns `OBJECT_MENU_CANDIDATES` and aborts —
  fail-closed); impostor resolution for state-swapped objects; action text
  matched case-insensitively against the first 5 composition actions;
  GAME_OBJECT_*_OPTION slot mapping; click region from `getCanvasTilePoly()`
  with null/degenerate/off-canvas guards; `worldViewId` handles instanced
  regions. Sound.
- `useItemOnObjectMenu`: exact item selection via `Rs2Inventory.use(item)` +
  `isItemSelected()` proof before dispatch; WIDGET_TARGET_ON_GAME_OBJECT with
  scene coords + identifier + worldViewId — same proven shape as the build-16
  barrel fix. Sound.
- Single-shot retry gates follow the established BARREL_MENU_ALTERNATE_ONCE
  pattern (persisted flags, tight varp/item/position/HP gates, HOLD cleared once
  with a distinct diag line). Fail-safe direction: over-strict gates just skip
  the alternate path.

## Findings
- [LOW NEW] MM20-1: `safeObjectMenuAction` and `useItemOnObjectMenu` canvas
  guards check `bounds.getMaxX()>canvasWidth` but omit
  `bounds.getMaxY()>canvasHeight` — same shape as carried D16-1. Fail-closed in
  practice (off-canvas rect won't land a click); suggest adding the maxY check.
- [LOW NEW] MM20-2: `useItemOnObjectMenu` proves selection same-tick
  (`isItemSelected()` immediately after `Rs2Inventory.use(item)`), not the
  next-tick proof the build-518 barrel fix used. If selection lands a tick late
  this false-negatives into one extra retry cycle — fail-safe, cosmetic.
- Carried from build-17 verdict: D16-1, D16-2, D14-1, D12-1, D6-1, README drift,
  D3-2, mirror/FINISHED-silent-clear.

## Live acceptance
PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; acceptance requires
`RUNNING_BUILD=20` plus the new diag lines (`OBJECT_MENU_DISPATCH` /
`ITEM_OBJECT_MENU_DISPATCH` / `LIBRARY_CLUE_MENU_ALTERNATE_ONCE` /
`CUT_PAINTING_MENU_ALTERNATE_ONCE`) in-game.

Verdict: PASS WITH FINDINGS. No blocking defects; ship stands.
