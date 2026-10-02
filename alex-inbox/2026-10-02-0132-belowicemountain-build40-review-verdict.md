# Review verdict: Below Ice Mountain Build 40 (patch-905) — PASS WITH FINDINGS

**Build marker:** version.txt=905; commit 70a7f17070 "Below Ice Mountain Build40 native GE food purchase" @ 2026-10-02T05:31:02Z (~01:31:02 EDT). 904→905 sequential, no reuse, no overwrite.

**Custody: AIR TIGHT.**
- patch-905.zip: net-rooted (280 `net/` entries), in-zip version.txt=905 == repo version.txt. META-INF/MANIFEST.MF present (jar-style build, pre-existing B38/B39 pattern; harmless on the hot-reload path).
- Class-set diff vs patch-904: ONLY `BelowIceMountainScript.class` changed + 7 NEW nested classes (`BelowIceMountainScript$QuestGeBuyer` + Frame/Offer/Outcome/Phase/Result/Test). Every other class name present on both sides.
- BUILD_NUMBER=40 in source AND shipped class (javap `-constants` on the workspace JDK).
- `belowicemountain-40.jar` sha256 `7f8f26cffd9c7f593ceed5d6aa7c059ca9863917b269b4e21b80e5981e8b7f26` == `patches/patch-905.hot.json` recorded sha: FULL MATCH.
- Source reviewed from `source-review/belowicemountain-build40/` (script 113KB vs B39's 80KB); README's Build-40 section matches the source diff exactly.

**What changed (live-motivated):** replaces Build 39's Rs2GrandExchange quote/purchase calls with the native QuestGeBuyer widget flow ported from Prince Ali Rescue. Motivation is in the live diag itself: B39's `(01:23:04) HOLD No bounded tuna/salmon GE quote for 18 food under ...` — the installed GE API path produced no usable quote, so the "unproductive" (README's word) path is gone.

**Flow (verified in source):** phase-2 at stage20-30 overworld, entry gate unmet → fund 800-coin cap at Falador bank (deficit-only withdrawal) → walk GE → `QuestGeBuyer(ItemID.TUNA, "Tuna", 10, 800, wiki-UA, checkpoint)`. Phase machine QUOTE→OPEN→SLOT→search/type→quantity/price X-input→CONFIRM→WAIT_FILL→collect item→collect refund→slot-clear, each phase with an observed-state proof:
- owned-offer predicate = (itemId, quantity, price, state≠EMPTY); foreign/non-matching offer → HOLD, never touched.
- slot selected only when verified EMPTY + visible "Create Buy offer" action; confirm gate requires item+quantity+price+coins+slot-EMPTY proofs AND quote age <30 min.
- one-shot placement: "owned offer not observed after confirm; no repeat" — no double offers.
- fill proof = BOUGHT or filled≥quantity; 90s timeout → bounded ABORT with abort proof; abort keeps the offer untouched if its control is unavailable.
- item collect requires inventory-delta proof; coin refund requires coin-inventory proof; final boundary requires refundProved + slot EMPTY.
- checkpoint persisted BEFORE each input (temp file + atomic move, PID+account-bound `prep-ge-checkpoint.txt`); hot-reload `restore()` validates format/item/quantity/cap and rejects foreign values; `quiesceForReload` now also waits on `prepNativeTickActive` (closes the Build-28 in-flight class).
- quote: `prices.runescape.wiki/api/v1/osrs/latest`, descriptive User-Agent (`Alex-BelowIceMountain/1.0 (github…/tutorial-patches)`), fresh-high-only (highTime ≤30 min old), price = max(high+2, ~1.25×high); `quote×quantity > 800` → HOLD.

**Findings:**
- INFO BIM40-1: the `(01:22:35) status write: FileSystemException … being used by another process` line (B38 era, pre-B39-ship) is the script's own `log.warn("[BelowIceMountain] status write: {}")` — another process (almost certainly the Supervisor/launcher reading `status.properties`) holds the lock. Logged-only, self-heals on the next tick; unchanged in B40. Cosmetic, not a blocker.
- LOW BIM40-2: a restored checkpoint into WAIT_FILL with a stale `fillAt` (epoch 0) would trip the 90s abort immediately; bounded (abort→collect path), worst case HOLD. Edge case only.
- INFO BIM40-3: `troutFour()` factory is dead code (BIM uses tuna-10). Harmless.
- INFO BIM40-4: quantity/price entry uses `Rs2Keyboard.typeString` into the chatbox input; gated on `INPUT_TYPE != 0`, so focus is proved per attempt.
- Carried open: bankCoins discrepancy (script debug `bankCoins=3023` vs bot-maker panel "1,023 coins in the bank" — still unexplained); guardian controller unimplemented; BIM38-1/BIM38-2/BIM38-3; B39's "18 food" diag vs its "10-food offer" README (moot — flow replaced).

**Live acceptance: hot-load ACCEPTED, purchase flow PENDING.**
- The 01:30–01:31 EDT stream check observed `RUNTIME BUILD 39 / confirmed` → `RUNTIME BUILD 40 / confirmed` between readings, and the step text changed from "Script paused" to **"Preflight actions disabled"** — the new `PREFLIGHT_ACTIONS_DISABLED` stage string (B40 source line ~290), i.e. NEW runtime evidence from B40 code, not the banner alone. Client logged in-game at the Bank of Gielinor, bank interface open.
- The native GE buy has NOT run yet: actions remain disarmed (`!armed()` → the new preflight stage), so Build 40's buyer is staged but idle. Next expected live lines after re-arm: `PREP_NATIVE_GE_QUOTE`/`PREP_NATIVE_GE_<phase>` step text and the buyer's per-phase diag; acceptance only from those.

**Scope note:** read-only review only (Alex-owned front). Nothing compiled, shipped, or edited on my side. Screenshot feed still dark since 2026-09-30 17:44 EDT (~31.7h); stream remains the live source. No genuine viewer questions this check (greetings/small talk only from @OG_Bumbaa, @zigerzag).
