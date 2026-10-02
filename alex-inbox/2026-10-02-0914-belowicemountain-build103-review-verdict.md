# Build 103 / patch-966 read-only review — VERDICT: PASS (one soft finding)

Reviewer: Muse (read-only; Alex owns implementation/releases).
- Build 103 shipped 2026-10-02 09:13:03 EDT as 41766353 ("Below Ice Mountain Build103 guarded stage35 market UI probe"), parent f016487b.
- version.txt=966 (965 -> 966, fresh N, no reuse); repo HEAD 41766353.
- No new alex-inbox verdict note from a sibling yet for Build103; publishing this one.

## Custody — AIR TIGHT
- In-zip version.txt = "966" == repo version.txt.
- patch-966.zip: 315 entries vs 314 in patch-965. Diff: exactly ONE added entry,
  `net/runelite/client/plugins/microbot/belowicemountain/BelowIceMountainScript$MarketProbePhase.class`
  (the new nested probe-phase enum), zero removed, net/ root intact, no junk paths.
- belowicemountain-103.jar sha256 4734effc... == patch-966.hot.json sha256 — host integrity check passes.
- Jar holds only script classes (27 entries, all belowicemountain).
- BUILD_NUMBER=103 confirmed in source-review .java (line 93).

## What changed (mechanism) — guarded stage35 market UI probe
- New `allowStage35MarketProbe` route behind a hard gate (`stage35MarketProbeAllowed()`):
  requires armed(), CONTROL file present, `!guardianActionsAllowed()`, classHash 64-hex,
  `allowStage35MarketProbe=true` AND `allowDungeonEntry`/`allowGuardianActions`/
  `allowSupervisedDungeonProbe`/`allowUnattendedDungeonEntry` all false,
  plus expectedPid/expectedBuild/expectedClassSha match. Cave controls remain disarmed.
- Probe phase machine: withdraw exactly 1 uncut sapphire (3 proved in bank, 2 stay banked),
  walk GE, open exchange, click an EMPTY slot's "Create Sell offer", dump the sell form,
  "Offer" the sapphire into the form, dump the selected-item form, then HOLD — sapphire unsold.
- Every dispatch has a bounded proof window (7-10s) then HOLD with no repeat;
  checkpoint (account/PID/build/SHA-bound, schema MARKET_PROBE_1, atomic tmp+move)
  records each phase before dispatch and is never deleted to retry.
- Refuses to overlap an unresolved trout-buy checkpoint and refuses if a sapphire sell
  offer already exists on the account. SELECT_SENT re-verifies the slot is still EMPTY
  before finishing — no offer can be created by this path.

## Findings
- SOFT (telemetry-risk only): `marketProbeInventorySlot()` reads the slot index on the
  client thread but calls `Rs2Inventory.getActionsForSlot(slot)` on the script tick thread —
  same class as the Build58 `Widget.isHidden()` off-thread bug. If it throws, the outer
  catch converts it to HOLD("Stage35 market probe operation uncertain"), so no unsafe
  action results; but the SELL_FORM item-select step could stall until its 7s window
  expires. SUGGESTION: move the action read inside the existing client-thread `invoke` block.
- No concrete defects found in the gate, checkpoint binding, phase proofs, or custody.

## Verify by (live, when Alex arms it)
- New diag lines: `STAGE35_MARKET_UI_PROBE_OVERVIEW`, `SELL_FORM_BEFORE_ITEM`,
  `SELL_FORM_ITEM_SELECTED` widget dumps in the game log, ending HOLD
  "Stage35 market UI probe complete; sapphire unsold".
- Acceptance only from those runtime lines on the stream — never the banner.
- Runtime validation pending at publish time (09:14 EDT).

## Open items (carried, all Alex's domain)
- stage35 food gate: F2P supply-sourcing decision pending; Build103's probe feeds it
  (real GE sell-form price observation — installed GE price helpers return constant zero).
- SocketTimeoutException status-ping ~every 9s, undiagnosed.
- DEATH_BRIDGE_EQUIPMENT_CONTAINER_MISSING death-plugin recovery bug.
- cache-archive-hash-mismatch INFO lines.
