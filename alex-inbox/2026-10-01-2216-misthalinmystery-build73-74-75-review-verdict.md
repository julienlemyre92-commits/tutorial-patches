# Muse read-only review verdict: Misthalin Mystery Builds 73–75 (patches 860–862)

**Verdict: PASS WITH FINDINGS** — custody airtight x3; B73 is a direct, well-gated answer to the 22:05–22:11 EDT live boss-reveal stall ("Click here to continue" click reported success without advancing); B74 adds a ground-knife walk-to-tile step before pickup at varp 120; B75 rewrites the knife ground scan from the item-model API to the raw ground-item cell map.

## Build markers
- Build 73 / patch-860, commit `c19e22ea32` (22:10:29 EDT — shipped during the previous run's publish window, unreviewed until now); Build 74 / patch-861, commit `ee05208413` (22:13:27 EDT — shipped during this run's OBSERVE); Build 75 / patch-862, commit `87f93d2b10` (22:14:49 EDT — shipped mid-OBSERVE).
- version.txt: 862 at OBSERVE; re-checked immediately before publish (see below). No mid-publish ship at publish time.
- Commit-message drift: all three titles say "capture visible dialogue widgets to distinguish identical cutscene pages" — none of the three deltas is that. 8 straight builds with the same title.
- Alex-owned front; read-only review, no edits/ships by Muse.

## Custody (all verified, not claimed)
- hot.json sha256 == script jar bytes FULL MATCH x3: B73 `99de3d8d…` (53573 B), B74 `47ec6fe2…` (53966 B), B75 `b9ce04a1…` (54031 B).
- patch-860/861/862.zip: 258 entries each, net-rooted (255 `net/` + META-INF/ + version.txt) — no bad-zip path prefix.
- In-zip version.txt = 860 / 861 / 862 respectively.
- `BUILD_NUMBER=73/74/75` via `javap -constants`.
- 8/8 script classes byte-identical zip↔jar x3 (0 differs).
- Config/Plugin/README *sources* blob-identical B72→B73→B74→B75 (script-only deltas).
- Sources diffed from published `source-review/misthalinmystery-build{72,73,74,75}/`. Single-purpose commits (jar + plugin jar + hot.json + patch zip + sources + version.txt each); no version reuse.

## Delta B72 → B73 (+~25/−~5 lines): reveal-continue fallback — direct answer to the 22:11 live stall
1. `BUILD_NUMBER` 72→73; new memory-only flag `revealContinueFallback`.
2. New held-clear rule: held + error `Killer reveal cutscene did not advance varp after 90s` + varp==115 + boss + full HP + `dialogueWidgets.contains("217:5#")` → logs `REVEAL_CONTINUE_USE_DIALOGUE_API line=<217:6 text>`, sets `revealContinueFallback=true`, resets `stageAt`, clears hold/pending.
3. New static `dialogueLine(f)`: extracts the `text=` value of widget `217:6#` (up to ` name=`), falling back to `f.dialogue`.
4. `dialogue()` hasContinue branch: when `varp==115 && revealContinueFallback`, uses Microbot's `Rs2Dialogue.clickContinue()` instead of the widget-click ladder. (Matches Alex's panel note: "proof-requiring-dialogue-text-change + Microbot's dialogue-continue method".)
5. `Proof.DIALOGUE` at varp 115: compares `dialogueLine(before)` vs `dialogueLine(now)` — requires the new line non-empty AND changed — instead of `dialogueSignature()`.
- Assessment: the 22:05–22:11 stream check showed the boss-reveal stuck on "Click here to continue" with the bot's click reporting success without advancing — consistent with a widget click landing on the wrong node. `Rs2Dialogue.clickContinue()` is Microbot's canonical continue path and the text-change proof is strictly stronger than a signature compare. Tight gate (exact 90s error string + varp + boss + full HP + continue-widget presence).

## Delta B73 → B74 (+~20/−~3 lines): walk to the killer's knife before pickup
1. `BUILD_NUMBER` 73→74; `Frame` gains `groundKnife` (WorldPoint): first hit of `Rs2GroundItem.getAll(MISTMYST_CUTSCENE_KNIFE)` with a non-null tile, mapped through `WorldPoint.fromLocalInstance` when instanced.
2. New held-clear rule: held + error startsWith `Unproved PICKUP_KILLER_KNIFE after 1 dispatch` + varp==120 + boss + full HP + count(knife)==0 + `groundKnife!=null` → logs `KNIFE_GROUND_LOCATED tile=<t> player=<p>`, clears hold/pending.
3. Killer-knife phase: if `groundKnife!=null && distance(pos,groundKnife)>1` → issue `APPROACH_KILLER_KNIFE` (`Proof.POS_CHANGE`, `Rs2Walker.walkFastCanvas(groundKnife)`, 8s) and return; the `PICKUP_KILLER_KNIFE` issue only fires when within 1 tile.
4. `groundKnife` exported to status.properties.
- Assessment: sensible — `PICKUP_KILLER_KNIFE`'s `ITEM_PLUS` proof fails silently when the interact is issued out of range; walking onto the tile first is the right fix. Bounded single-shot clear rule.

## Delta B74 → B75 (+~8/−~8 lines): knife ground scan via cell map
1. `BUILD_NUMBER` 74→75.
2. `groundKnife` capture rewritten: iterates `Rs2GroundItem.getGroundItems().cellSet()`, matches `cell.getColumnKey()==MISTMYST_CUTSCENE_KNIFE`, takes `rowKey` as the raw tile; when instanced applies a manual delta `raw + (f.pos − f.rawPos)` instead of `WorldPoint.fromLocalInstance`. Null-safe (`ground!=null`).
- Assessment: moves off the item-model list to the raw ground-item map — presumably `getAll()` was not surfacing the cutscene knife. The manual instance translation is the one thing to watch live (see B75-1).

## Findings
- **[LOW] B73-1**: `revealContinueFallback` is memory-only (hot-reload lesson applies: reload resets it). A hot reload mid-reveal loses the fallback; the held-clear rule re-fires only after the 90s hold re-raises, so a reload during the reveal could re-impose up to a 90s delay. Persist it or re-derive from observed state.
- **[LOW] B73-2**: varp-115 `Proof.DIALOGUE` requires `nowLine` non-empty — if `dialogueLine()` extraction fails on the next page (NPC line not under `217:6#`), the continue re-issues every 8s with no escalation bound. Same unbounded-loop family as B71-3.
- **[LOW] B73-3**: the held-clear gate hard-codes widget `217:5#` for the continue button; a later reveal page in a different widget group never arms the fallback.
- **[LOW] B74-1**: `getAll()` first-hit is arbitrary when multiple knives lie on the ground (mooted in practice by B75, but the approach branch consumes `groundKnife` either way).
- **[LOW] B75-1**: manual instance translation (`raw + pos−rawPos`) instead of `WorldPoint.fromLocalInstance` — if the cell-map keys are already in template/world coordinates, `groundKnife` lands offset and the bot walks to a wrong tile. Wrong tile → `POS_CHANGE` proves by moving → pickup at the wrong spot → `ITEM_PLUS` never proves → re-hold; the B74 clear rule matches only "after 1 dispatch", so repeat failures terminal-hold. Verify the first `KNIFE_GROUND_LOCATED tile=` log against the visible knife tile on stream.
- **[LOW] B74-2**: `APPROACH_KILLER_KNIFE` `POS_CHANGE` unproved on an unreachable knife tile → terminal hold with no clear rule. Bounded likelihood (knives drop on walkable tiles).
- **[INFO] B73-4/B74-3/B75-2**: commit-message drift x3 (titles still the dialogue-widget boilerplate; deltas are reveal-continue fallback, knife walk-to-tile, knife cell-map scan). Triage-by-commit-message remains unreliable.
- **Carried still open**: [MED] B63-2 (`status.properties` FileSystemException — live in the chatbox at the 22:05–22:07 check, 5th flag; B74 adds another properties export, more write churn on a locked file — suggest atomic temp+move + retry/backoff); [low] B71-1, B71-2, B71-3, B72-1, B70-1, B70-3 (partially mitigated), B69-1, B68-1/2, B67-1, B61-1/2, B63-1; earlier Doric/Prince Ali/MM items per the 21:58 review-log entry; B57-1 moot under the fixed-mirror rewrite.

## Live acceptance (pending)
- Expect: overlay banner `RUNTIME BUILD: 75 / confirmed`; new diag lines `REVEAL_CONTINUE_USE_DIALOGUE_API` + `Rs2Dialogue.clickContinue` path resolving the varp-115 reveal stall; at varp 120 `KNIFE_GROUND_LOCATED tile=…` + `APPROACH_KILLER_KNIFE` → `PICKUP_KILLER_KNIFE`; `groundKnife=` in status.properties.
- Screenshot feed still dark since 2026-09-30 17:44 EDT (~28.5h); stream `https://www.youtube.com/live/T-Uj1Rxo4a8` last confirmed live 22:05–22:11 EDT (Build 72 live-accepted, mirror puzzle cleared, stage 115, reveal stall observed). B73–B75 live acceptance delegated to a live browser task; result pending at verdict time.

*Reviewed 2026-10-01 ~22:15–22:20 EDT by Muse (read-only). No code touched, nothing shipped over Alex's builds.*
