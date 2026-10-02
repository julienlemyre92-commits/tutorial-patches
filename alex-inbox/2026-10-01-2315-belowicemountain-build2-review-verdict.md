# Review verdict: Below Ice Mountain Build 2 (patch-867) — PASS WITH FINDINGS

Read-only review of `source-review/belowicemountain-build2/BelowIceMountainScript.java`
(565 lines), patch-867.zip (full overlay), patches/patch-867.hot.json,
patches/belowicemountain-2.jar (script-only hot candidate). No shipping action taken.

## Fingerprint verification (all match)
- hot.json: plugin=belowicemountain, patch=867, hostVersion=1, build=2,
  sha256=`abbc854fddd6853ad7f5385e2286901fceda1246c061ac7a6dbcf168cdd988ce`
- `belowicemountain-2.jar` on repo: SHA-256 `abbc854f…cdd988ce` — matches hot.json and README claim
- Script class inside the jar: `e771cbaea228dcb7aef18b4201f204efb77645e3d43167219c6aaed98cc0767c` — matches README claim
- Script-only jar contains ONLY `BelowIceMountainScript` + 5 nested classes, `net/` root — safe for hot reload
- patch-867.zip: 276 entries, `net/` root (no 341/342-class root mistake); script classes built 23:10 EDT,
  host plugin classes 22:58, old cooksassistant/tutorialisland classes carried byte-identical from
  2026-09-30 18:33 (overlay-inherited, fine)

## Verdict: PASS WITH FINDINGS

Architecture is sound: arming gate (PID+build+SHA via config or control.properties), per-tick
observe→act→verify with proof-gated actions, bounded retries (2 unproved → HOLD), atomic status
writes (temp+move), F2P-world enforcement for native login, guardian never attempted
(`f.varp>=15` parks at `BUILD2_STAGE_LIMIT_<varp>`). Emote-tab switch is proof-gated
(`Proof.FLEX_OPEN` checks the tab actually switched), Flex-widget/pickaxe-spawn mismatches hold
with status rather than guessing.

### MEDIUM BIM2-1: Willow dialogue expected-gate race can HOLD with dialogue open
`dialogue()` accepts only `(varp<=7 && near(WILLOW,12)) || (varp==10 && near(CHECKAL/ATLAS,12))`.
`talk:willow-start` proves on `DIALOGUE_CHANGED`, which fires on ANY dialogue text/frame change —
so pending clears mid-conversation. If BIM_MAIN advances to 10 while Willow's dialogue window is
still open (quest start often lands the varp before the final frame closes), the next tick calls
`dialogue()` with varp=10 near WILLOW → `expected=false` → `hold("Dialogue outside Build2 NPC route…")`
with the dialogue window open. The bot then sits frozen in a conversation it could have finished.
Suggested fix: treat `varp==10 && near(WILLOW,12) && inDialogue` as expected (quest-start dialogue
lingering), or require the varp advance *plus* dialogue-close in the DIALOGUE_CHANGED proof for the
final Willow frame.

### LOW BIM2-2: Mining<10 HOLD over-constrains Build 2's early route (carry-over from BIM1-1)
Build 1 verdict already acknowledged this as OK for the pillar-route plan. Kept in Build 2; fine
for now, but the early route (Willow→Checkal→Atlas→Flex) needs no Mining — consider downgrading
to a warning log until the guardian/pillar stage is actually built.

### INFO BIM2-3: `issue()` logs `accepted` but never branches on it
A rejected action still sets `pending`, so it surfaces as "Unproved x2" HOLD rather than an
immediate reject. Bounded and acceptable; no change needed.

### INFO BIM2-4: login-modal dismiss x differs from the live-verified Build 43 coordinate
`loginTick` computes `365+(canvasWidth-804)/2` → x=365 at 804-wide canvas; Ernest Build 43's
live-verified dismiss was x=346/y=308. The modals may differ; flagging so the live test compares
against the observed button, not the formula.

### INFO BIM2-5: Flex widget (216,1 + sprite 2426) and Barbarian Village pickaxe spawn tile
(3083,3419) remain live-validation candidates — README already says this, and a mismatch holds
with status. Correct conservative behavior; nothing to change.

## Live acceptance pending
Watch for `RUNNING_BUILD=2` diag lines on the live client. No deployment or arming performed here —
Alex owns the hot request, arming (control.properties), and live test. No shipping action taken (read-only).
