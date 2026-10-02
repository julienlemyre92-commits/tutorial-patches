# BIM Build 97 read-only review — PASS WITH FINDINGS (2026-10-02 ~07:52 EDT, muse-review-loop)

Build: Below Ice Mountain Build 97 / patch-959 / commit 795e4ba8 (repo version.txt=959).
Scope: read-only review only. No ship — Alex owns implementation/releases.

## Custody: AIR TIGHT
- In-zip `version.txt` = 959 == repo version.txt (independent API read).
- patch-959.zip is `net/`-rooted; 310 net entries; net file list byte-identical to patch-958.
- `BUILD_NUMBER = 97` in shipped source AND compiled class (javap ConstantValue).
- patches/belowicemountain-97.jar sha256 `f67ba6d2…cce49` == patch-959.hot.json sha256.
- zip-class bytes == jar-class bytes (`8bccfd86…f384af`) for BelowIceMountainScript.class.
- Downloaded via git blobs API (contents API returned 0 bytes for the zip — >1MB omission); all shas above are from the blobs fetch.

## Change vs B96: 17-line diff, one real fix
New block in the stage-35 overworld-prep branch (quest stage 35, prep area, guardian not
active), after the food/healing gates: if `f.geOpen`, look up the GE Close control on the
client thread (widget 465,2 → child 11, visible, has "Close" action); fail-closed
`hold("Stage35 GE Close control absent after settled offer")` if absent; else
`issue("stage35:close-ge", Proof.GE_CLOSED, f, 0, 6000, () -> Rs2Widget.clickWidget(close))`.

This is an EXACT clone of the proven training block (`train:close-ge`, script lines
2164–2175): same widget path, same null/hidden/action guards, same proof, same hold style.
`Proof.GE_CLOSED` is an existing enum value with an observable verify predicate
(`p.before.geOpen && !f.geOpen`; `f.geOpen = Rs2Widget.isWidgetVisible(465,1)` per tick).

Why it matters: the re-entry preflight (`dungeonEntryAllowed`, ~line 1248) requires
`!f.geOpen`. In B96 a GE window left open after the stage-35 trout buy had no closer in this
branch — the flow fell through to `WAIT_STAGE35_REENTRY_PREFLIGHT` and stalled there with
the window open. B97 closes it first. Genuine stall fix; minimal, precedent-matched diff.

## Findings (all INFO)
- BIM97-1: new terminal `hold(...)` on the Close-control lookup is fail-closed, matching the
  codebase convention (identical hold in `train:close-ge`). Benign by design; the lookup is
  well-guarded. On proof timeout the branch re-ticks and re-issues (same as training).
- BIM97-2: commit message still "Below Ice Mountain Build91 empty equipment bridge" —
  6th straight ship with this stale/misleading message; the build is 97.
- BIM97-3: source-review README.md still the Build-1 handoff head (carried).
- BIM97-4: official META-INF/MANIFEST.MF present in the zip again (benign; carried).

## Live acceptance (pending)
Watch for: `RUNNING_BUILD=97`, `stage35:close-ge`, `GE_CLOSED`, and the stage-35
re-entry sequence (`STAGE35_GE_ORDER_PROVED` → window closed → re-entry). Screenshot feed
dark ~38h; no confirmed live stream URL. Nothing counts as live until fresh diag/screenshot
evidence shows it.
