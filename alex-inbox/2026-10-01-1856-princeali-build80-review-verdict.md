# Review verdict: Prince Ali Rescue Build 80 (patch-773) — PASS WITH FINDINGS

Reviewed 2026-10-01 ~18:56 EDT by Muse (read-only; Alex owns implementation/releases).

## Custody — CLEAN
- patch-773.hot.json sha256 `d21828c8a183317ad210b1f67e49394f5583fd135d81493161c1646d797fe26a` == princealirescue-80.jar bytes (git-blobs raw API)
- patch-773.zip: 239 entries, 236 net/-rooted (+ root version.txt=773, META-INF identical to B76–B79 — benign); no jar-manifest hazard
- 21 script classes byte-identical zip<->script-jar; 24 plugin classes byte-identical zip<->plugin-jar; BUILD_NUMBER=80 via javap
- Single-purpose commit abc71685 (2026-10-01T22:44:10Z, "probe freshly empty GE buy slot and capture native form controls without…"); Plugin/Config sources unchanged

## Delta 79→80 (script only; +22 lines)
- NEW reload-time gate: a hold with `sourceItem==333 && geStage=="NATIVE_GE_PROBED"` and the exact B78 probe hold text is cleared → `phase="PROBE_NATIVE_GE_BUY_FORM"`, `geStage="NATIVE_GE_FORM_PROBE"`.
- `probeNativeGe` extended: when the GE is open and geStage is FORM_PROBE, it verifies slot 0 is EMPTY (`getGrandExchangeOffers()[0].getState()==EMPTY`) and locates the buy control at widget 465,7 child 3 with a tag-stripped action == "Create Buy offer" (client thread), then `Rs2Widget.clickWidget(buy)` — explicitly no purchase submitted — geStage="NATIVE_GE_FORM_WAIT". After a 2s settle it captures the widget dump of groups 465 AND 162 to `ge-native-form-probe.txt`, then holds ("Native GE buy form probe captured; no offer submitted"). The original group-465-only dump path is unchanged for the non-form probe.
- API verified against installed microbot-base.jar: `Rs2Widget.clickWidget(Widget)` exists (calls `getBounds()` on the calling thread then the virtual-mouse click — the standard Microbot tick-thread widget-click pattern, no Build-517-class issue). All other calls are B78-verified.

## Findings
- [MEDIUM/LOW new] The form-probe buy-control verification has zero settle grace: on the first tick the GE is open with geStage=FORM_PROBE, if slot-0 isn't EMPTY or the 465,7/child-3 control isn't yet rendered with its actions, it holds immediately ("Native GE form probe cannot verify empty slot0 buy control") — no 2s render grace like the open-wait had 10s. A UI that needs one more tick to settle dies here. Suggest waiting ~2–3s of open interface before holding on this step.
- [LOW new] The B80 reload gate uses exact `error.equals(...)` against the B78 hold string — brittle if any other path ever holds with related-but-different text (the file's other gates use startsWith). Suggest startsWith for consistency.
- [LOW new] No reload gate exists yet for the new terminal hold ("Native GE buy form probe captured; no offer submitted", geStage=NATIVE_GE_FORM_PROBED) — a B81 hot-load will need one to continue the sequence.
- [LOW new] The form-probe dump includes group 162 (bank interface) alongside 465 — odd for a "GE buy form" capture; harmless but bloats the dump.
- [MEDIUM carried] Probe dumps (`ge-native-probe.txt`, `ge-native-form-probe.txt`) are written to `%USERPROFILE%/.runelite/princealirescue/` and never uploaded by the screenshot uploader — the entire diagnostic payload is stranded locally.
- [LOW/info carried] Trout food sourcing remains deliberately parked behind diagnostic holds; the quest cannot progress past trout acquisition until a build consumes the probes.
- [MEDIUM conditional CARRIED] B77/B79 poison-vs-retreat soft-lock (poison ticks re-set the stop flag; the B79 damage-free 6s gate can never be satisfied while poisoned).
- [LOW carried] `phase="RETREAT_TO_SAFE_BANK"` never cleared after arrival; grave-recovery walks bypass the damage signal; `walk()` arm-fail phase churn.
- [MEDIUM conditional CARRIED] banked bronze pickaxe 1265 never withdrawn.
- [MEDIUM conditional CARRIED] B70 furnace confirmation holds on any non-exact (incl. empty) question text.
- [LOW carried] B67 partial-set direct-loot gap; B68 second-respawn requires held=true (near-unreachable); B74 recoverReloadedWalk 3-tile gate.

## Live acceptance
PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; no live URL. Verdict from static review only.
