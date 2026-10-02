# Review verdict: Below Ice Mountain Builds 13/14/15 (patches 878/879/880) — READ-ONLY

Reviewed by: Muse (read-only; Alex owns implementation/releases; no ship)
Build 13: commit 03b8bed214 ("Flex client-thread correction", landed 03:41:05Z)
Build 14: commit 9b2114c877 ("Flex panel visibility", landed 03:43:47Z)
Build 15: commit 958cee8aaf ("visible Flex mouse click", landed 03:45:36Z)
Verdict: **PASS WITH FINDINGS**

## Custody — AIR TIGHT
- Version sequence 877→878→879→880, all sequential, no reuse, no overwrite of any patch-N.zip.
- patch-880 hot.json `sha256: de0e77914632716f45bf4536bc48ba92c0e3e83088f5ce90224e8fd2f5f85d01` == actual `patches/belowicemountain-15.jar` bytes (git blobs API, 25,055 bytes) — FULL MATCH.
- Script-only jar holds exactly 7 class entries (`BelowIceMountainScript` + nested `FlexView`, `Frame`, `Pending`, `Proof`, `Route`, `$1`) — no stale-class overlay risk.
- `javap -constants` on the compiled class: `BUILD_NUMBER = 15` — no lying banner.
- `patch-880.zip`: 277 entries, net-rooted, identical entry set to patch-879 (only expected classes), in-zip `version.txt` = `880` == repo `version.txt` == patch number. Built with `zip` (META-INF/ present but default-manifest overwrite issue does not apply to this script-jar pipeline; same as prior builds).
- Single-purpose commits; each touches only jar + hot.json + zip + source-review + version.txt.

## Diff vs Build 12 (published source, verified byte-diff 620→651 lines) — matches READMEs
1. **Build 13**: widget reads (id/index/parent/bounds/actions) + the CC_OP `clickWidgetFast(flex,index,1)` dispatch both moved inside `Microbot.getClientThread().invoke` — corrects the `must be called on client thread` crash Build 12 hit.
2. **Build 14**: new `FlexView` (widget, panel bounds, icon bounds, scrollY captured on client thread) + `EMOTE_SCROLLED` proof + `emoteScroll` status field; vertical visibility gate; scrolls panel one notch (`Microbot.getMouse().scrollDown` at panel center) with later-`scrollY` proof, capped at 15 verified scrolls; Flex invoked only after the icon is inside the visible panel.
3. **Build 15**: the dispatch reverts from CC_OP back to the plain `Rs2Widget.clickWidget(flex)` mouse click — but ONLY after the icon intersects the visible panel, on the client thread, with `FLEX_WIDGET` diagnostics (id/index/parent/bounds/actions/sprite) logged at dispatch. Proof stays `DIALOGUE_CHANGED` with 12s budget; two unproved attempts → HOLD.

## Design rationale — sound
- Build 11's accepted-true clicks hit an icon at bounds y701–749 while the visible panel ended above it — a click that cannot execute. The visibility gate removes exactly that masked-no-op class.
- Reverting CC_OP to a plain mouse click is the right call after the CC_OP action itself went unproved twice in Build 14: with the icon now provably visible, the installed mouse click is the dispatch the game actually responds to. The `FLEX_WIDGET` pre-dispatch log gives a live tell (the `actions` array shows whether the Flex menu item exists at click time).
- Proof-gate discipline unchanged: every scroll verified by `scrollY` change, every click verified by later dialogue change, terminal HOLD after two failures, guardian still disabled.

## Findings
- **INFO BIM15-1**: proof `DIALOGUE_CHANGED` is unverified against the actual Flex success event. The README cites the expected result as a Checkal varbit change ("did not change Checkal varbit15"). If a successful Flex changes the varbit without a dialogue change (or vice versa), the proof can false-negative into HOLD. If Build 15 holds after two visible clicks, check the diag for whether a `DIALOGUE_CHANGED` event or a varbit change actually occurred — that decides whether the proof type is wrong.
- **INFO BIM15-2**: the visibility predicate checks vertical containment only (`icon.y+height` vs `panel.y+height`, `icon.y` vs `panel.y`) — no horizontal check. Fine for the single-column emote panel; noted for completeness.
- **INFO BIM14-1 (carried)**: `flexScrolls` is memory-only; a hot reload mid-scroll resets it to 0 (re-scrolls at most 15 more). Bounded and acceptable.
- **Carried**: LOW BIM2-2 (mining-10 gate), LOW BIM7-1 (Manhattan net-progress false-positive on detours), LOW BIM7-2 (absolute 5-tile threshold), INFO BIM2-4 (`issue()` accepted-unused), INFO BIM9-1 (low-byte decode assumption).
- Resolves the Build-11 accepted-no-op class by construction; BIM2-1 (Willow dialogue gate race) already resolved in Build 8.

## Live acceptance — PENDING
- RUNTIME BUILD 15 marker + `FLEX_SCROLL` / `FLEX_WIDGET` lines not yet observed. Screenshot feed dark since 2026-09-30 17:44 EDT (~30h); a read-only stream liveness check was spawned this run. Accept only when the new runtime lines appear on the live client — never from the banner alone.

No shipping action taken — read-only review.
