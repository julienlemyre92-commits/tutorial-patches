# Review verdict: Prince Ali Rescue Build 78 (patch-771) — PASS WITH FINDINGS

Reviewed 2026-10-01 ~18:56 EDT by Muse (read-only; Alex owns implementation/releases).

## Custody — CLEAN
- patch-771.hot.json sha256 `99a28a3b217dde9be341e69c669e51edb04312ae65ff2cf4b6a9bcbb2fc1948e` == princealirescue-78.jar bytes (git-blobs raw API)
- patch-771.zip: 239 entries, 236 net/-rooted (+ root version.txt=771, META-INF identical to B76/B77 — benign); no jar-manifest hazard
- 21 script classes byte-identical zip<->script-jar; 24 plugin classes byte-identical zip<->plugin-jar; BUILD_NUMBER=78 via javap
- Single-purpose commit e066c171 (2026-10-01T22:34:47Z, "probe real GE widgets via clerk and register direct event consumers acros…"); Plugin/Config sources unchanged

## Delta 77→78 (script only; +48 lines)
- QuestDamageSignal registration refined: `bus.register(this)` (@Subscribe whole-object, B77) → 4 explicit `bus.register(Class, this::methodRef, 0f)` calls returning `EventBus.Subscriber` handles, unregistered individually in `close()` (the @Subscribe annotations remain on the methods but are now inert — harmless). Verified against microbot-base.jar: `register(Class<T>, Consumer<T>, float)` → Subscriber and `unregister(Subscriber)` both exist.
- NEW `probeNativeGe(f)`: diagnostic native-GE probe for trout-333 sourcing. Flow: close bank if open → check `Rs2Widget.isWidgetVisible(465,1)` on the client thread → if open, dump all 8 `GrandExchangeOffer`s + full widget tree of group 465 (depth-12, identity-deduped) to `%USERPROFILE%/.runelite/princealirescue/ge-native-probe.txt`, log byte count, geStage="NATIVE_GE_PROBED", then HOLD "Native GE opened; widget/offer probe captured before any buy". If closed: find "Grand Exchange Clerk" via `Rs2Npc.getNpc`, verify the "Exchange" action on its NPCComposition (client thread), `Rs2Npc.interact(id,"Exchange")`, 10s open-wait → hold on timeout.
- Routing: the B76 reload-time wiki-quote retry gate (trout 333 "GE quote unavailable" hold → RETRY_FOOD_QUOTE_WITH_WIKI_API) is REPLACED by `phase="PROBE_NATIVE_GE_WIDGETS"; geStage="NATIVE_GE_PROBE"`, and `sourceTick` routes any `id==333 && geStage.startsWith("NATIVE_GE_")` to `probeNativeGe` — so once a trout session enters the probe path it never returns to the wiki-quote GE flow.
- API verified: `Rs2Npc.getNpc(String)`, `Rs2Npc.interact(int,String)`, `Rs2Widget.isWidgetVisible(int,int)` all exist in microbot-base.jar with matching signatures; `interact`/`getNpc` are the standard tick-thread-safe Microbot paths. All client reads (`getGrandExchangeOffers`, `getWidget`, `getNpcDefinition`) run inside `ClientThread.invoke` — no Build-517-class issue.

## Findings
- [MEDIUM new] The probe dump is stranded locally. `ge-native-probe.txt` is written to `%USERPROFILE%/.runelite/princealirescue/` — the screenshot uploader watches `bundle/screenshots/`, so the widget/offer dump (the entire point of this build) never reaches the repo/reviewers; only the byte-count diag line does. Suggest writing the probe file into `bundle/screenshots/` (or echoing a compact summary into the diag/status) so the capture is actually retrievable.
- [LOW/info new] Trout food sourcing is now deliberately parked: the probe path ends in a terminal HOLD and there is no build yet that consumes the probe to resume buying. The quest cannot progress past trout acquisition as of this build — expected for a diagnostic build, but the run is not "progressing" until the follow-up lands.
- [LOW carried] B77 poison/retreat interaction (any HP loss incl. poison ticks aborts walks into a retreat whose 6s quiet gate can never be satisfied while poisoned).
- [LOW carried] `phase="RETREAT_TO_SAFE_BANK"` never cleared after arrival (diag staleness).
- [MEDIUM conditional CARRIED] banked bronze pickaxe 1265 never withdrawn (withdrawFinishedIfBanked covers only {wig, paste, key, print}).
- [MEDIUM conditional CARRIED] B70 furnace confirmation holds on any non-exact (incl. empty) question text.
- [LOW carried] B67 partial-set direct-loot gap; B68 second-respawn requires held=true (near-unreachable); B74 recoverReloadedWalk 3-tile gate.

## Live acceptance
PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; no live URL. Verdict from static review only.
