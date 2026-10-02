# Misthalin Mystery Builds 37 + 38 review verdict: PASS WITH FINDINGS (read-only)

Reviewed: 2026-10-01 ~21:01 EDT (Muse review-loop, read-only — Alex owns implementation/releases).
Build 37 = patch-824 (commit 62f4e278, 2026-10-02T00:54:54Z). Build 38 = patch-825 (commit ad1bec96, 2026-10-02T00:56:29Z).
Both commit messages are the same boilerplate ("capture visible dialogue widgets to distinguish identical cutscene pages"); source-review/ dirs are the real record.

## Chain of custody — ALL PASS (both builds)
- hot.json sha256 == misthalinmystery-N.jar FULL MATCH: 6e9ea7578f6988c7161a25cddd3df1b0c07a83617ed2b6dfaf9b800c82e23fa6 (B37, 47,521 B) / 5f2d194cf56a39fee5262d36d19bb6210520d3ce0da1abb6776d089ff38f96f6 (B38, 47,624 B).
- patch-824.zip / patch-825.zip: 258 entries, root `net/` (not the too-deep bug); genuine RuneLite MANIFEST.MF (Main-Class: net.runelite.client.RuneLite); in-zip version.txt = 824 / 825.
- 8/8 MisthalinMysteryScript*.class byte-identical zip<->script-jar; MisthalinMysteryPlugin.class, Plugin$1, Config byte-identical zip<->plugin-jar (both builds).
- BUILD_NUMBER = 37 / 38 via javap on the shipped binary (never banner-alone).
- Single-purpose commits: patch-N.zip + patch-N.hot.json + misthalinmystery-N.jar + misthalinmystery-plugin-N.jar + source-review/misthalinmystery-buildN/ (Config/Plugin/Script/README) + version.txt bump. No sibling-race artifacts observed.

## Delta Build 37 (8 changed lines vs B36)
- OBSERVE_TREE recovery gate widened: now fires on `error.startsWith("Unproved OBSERVE_TREE after 1 dispatch")` in addition to `"Reload during OBSERVE_TREE"` (same widening pattern as Prince Ali Build 52's continue-recovery).
- The varp==65 branch now also requires `f.dialogueWidgets.contains("text=Killer")` — recovery only on positive proof the visible dialogue page is the tree-killer page. This is the widget-snapshot disambiguation the commit message describes: identical cutscene pages distinguished by the captured dialogue-widget text.
- Side effect: the gate recomputes `observeTreeCutsceneObserved = (f.varp==65)` on recovery — partially mitigates B36-2 (stale persistence), though only inside this gate.

## Delta Build 38 (13 changed lines vs B37)
- New OUTSIDE_CLUE_PLAYER_DIALOGUE_PROVED recovery gate: clears an `"Unproved TAKE_OUTSIDE_CLUE after 1 dispatch"` hold when varp==70 + outside() + full HP + clue count==0 + inDialogue + hasContinue + dialogueWidgets contains `"217:5#"` AND `"That monster"`. Clears on dialogue-widget evidence alone, before the clue item lands. Fail-safe: requires fresh observed proof each tick, not latched.
- New click path in the DIALOGUE_CONTINUE issue: when `"217:5#"` is present, dispatches a physical `Rs2Widget.clickWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, 5)` first, ahead of the varp-15/20 ellipsis fallback and `Rs2Dialogue.clickContinue()`. The issue() wrapper still proof-gates it (Proof.DIALOGUE, 8 s deadline), so a stale widget cannot loop it forever.

## Findings
- NEW [info] B37-1: widened OBSERVE_TREE gate stays fail-closed — without the Killer-page proof the hold persists with the error string. By design.
- NEW [info] B38-1: OUTSIDE_CLUE gate clears on dialogue evidence with clue count==0; if the "That monster" text ever appears without the clue actually being takeable, the hold clears one tick early and the normal path re-issues. Bounded, fail-safe.
- NEW [info] B38-2: the new clickWidget branch takes priority over the varp-15/20 ellipsis branch but yields the identical outcome when "217:5#" is present (same group/child); for other varps it upgrades clickContinue() to a physical widget click. No regression; aligns with the physical-click rule.
- CARRIED — D28-1 STILL OPEN (verified in B38 source: `"aat:"` occurs exactly once, the dead RUBY_DOOR_DEFINITION_FALLBACK_ONCE gate at line 682; no producer anywhere).
- CARRIED — B36-1 STILL OPEN (observeTreeDialogueClosedAt stamped on first tick observeTreeCutsceneObserved is true, lines 1495–1497, with no !inDialogue check — the 15 s clock starts while the cutscene dialogue may still be open).
- CARRIED — B36-2 PARTIALLY ADDRESSED (B37 re-arms observeTreeCutsceneObserved from live varp inside the widened recovery gate; still no quest-replay invalidation outside that gate).
- CARRIED — D28-2, D30-1, D27-1, D27-2, D16-1, D16-2, D14-1, D12-1, D6-1, README drift (README.md still the 5338 B Build-1-era doc), D3-2, mirror telegraph, FINISHED silent clear.

## Live verification (stream, 2026-10-01 ~20:55 EDT — first live eyes in ~27 h)
- RUNTIME BUILD 38 banner CONFIRMED live; quest stage 70; tree cutscene finished live 65→70 — the B36/B37 OBSERVE_TREE recovery path verified working in production.
- Bot at the outside-clue step, dark dungeon area, fighting skeletal enemies. Alex's panel notes: B37 resumed from visible dialogue without another Observe click; outside-clue attempt while the "That monster" player dialogue was still open → correct HOLD, clear dialogue first, then fresh pickup with inventory proof — exactly the situation B38's new gate and click path target.
- Acceptance lines to watch for: OUTSIDE_CLUE_PLAYER_DIALOGUE_PROVED, TREE_RELOAD_DIALOGUE_PROVED, DIRECT_WIDGET_*.

Nothing shipped (review-only; Alex's releases). Screenshot feed still dark since 2026-09-30 17:44 EDT (~27.3 h); the new confirmed live stream (Julien's 20:55 URL) is the current live visual source.
