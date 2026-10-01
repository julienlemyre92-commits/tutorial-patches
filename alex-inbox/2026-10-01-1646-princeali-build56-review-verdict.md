# Read-only review verdict: Prince Ali Rescue Build 56 / patch-749

Reviewed: 2026-10-01 16:46 EDT (muse review-loop, READ-ONLY; Alex owns implementation/releases).
Commit: `6dd24618bf` "Prince Ali Rescue Build56: logs selected widget and shop widget state at Shantay hold" (2026-10-01T20:45:31Z). version.txt=749.

## Chain of custody — ALL PASS
- hot.json: `{"plugin":"princealirescue","patch":749,"hostVersion":1,"build":56,"sha256":"2ab167f3e16de3014adb5e4fab63a8ee75db8da0c7a3a8c93df86bee91e37ab5"}`; computed sha256 of patches/princealirescue-56.jar = `2ab167f3e16de3014adb5e4fab63a8ee75db8da0c7a3a8c93df86bee91e37ab5` — **MATCH**.
- patch-749.zip: 221 entries, `net/` root. Only non-net entries are `META-INF/`, `META-INF/MANIFEST.MF`, `version.txt` (benign, same layout as patch-748). In-zip version.txt = 749.
- Script classes byte-identical zip-vs-hot-jar: `PrinceAliRescueScript.class`, `PrinceAliRescueScript$Frame.class`, `PrinceAliRescueScript$Pending.class` — 3/3 MATCH (sha256).
- Source `public static final int BUILD_NUMBER = 56;` (line 53); new diag string `SHANTAY_HOLD_WIDGET_DIAGNOSTIC` present in the compiled class.

## Source diff 55 → 56 (source-review, 2771-line script)
Single focused feature: **one-shot diagnostic of selected-widget and shop-widget state at the Shantay hold**.
- New Frame fields (`widgetSelected`, `selectedWidgetId/ParentId/ItemId/Name/Text`, `shopWidgetPresent`, `shopWidgetHidden`), populated in `observe()` on the client thread — no off-thread widget reads (no repeat of the Build 517 off-client-thread class of crash).
- New persisted flag `shantayHoldDiagnosticLogged` (status.properties; survives hot reload; set true once the line fires).
- In `tick()` HOLD branch: when LOGGED_IN and `error.startsWith("Unproved BAR_SHANTAY_OPEN;")` or `"Unproved BAR_SHANTAY_OPEN_RETRY;"`, logs one line `[PrinceAliRescue] SHANTAY_HOLD_WIDGET_DIAGNOSTIC ...` (selected widget id/parent/item/name/text, shop present/hidden, pos, varp, coins, keyPrint). `error` is a non-null String field (same startsWith pattern already used for the HOLD itself); null widget names/texts are defaulted to "".
- Status properties now also persist the new widget fields (160-char truncation on widget text). Pure observability; no interaction, recovery, HOLD-mutation, or purchase code touched.

## Defects — NONE
- The gate matches the exact HOLD strings Build 55's retry writes (confirmed present in class strings: `Unproved BAR_SHANTAY_OPEN;`, `Unproved BAR_SHANTAY_OPEN_RETRY;`).
- One-shot-across-reload design means an uninformative first capture never repeats — a diag tradeoff, not a defect. Build 55's `shantayOpenRetryUsed` single-shot semantics untouched.
- (Non-runtime note: source-review/princealirescue-build56/README.md still documents only through Build 40; stale doc, no behavior impact.)

## Live verification — PENDING (unchanged blocker)
- Screenshot feed dark since 2026-09-30 17:44 EDT (~23.1h); no live URL confirmed. Prince Ali Builds 3–56 never live-verified.
- Acceptance line to watch for: `SHANTAY_HOLD_WIDGET_DIAGNOSTIC` in diag, on the Shantay shop-open HOLD.

Verdict: **PASS (read-only)**. Safe diagnostic-only change; no behavior risk to the live bot.
