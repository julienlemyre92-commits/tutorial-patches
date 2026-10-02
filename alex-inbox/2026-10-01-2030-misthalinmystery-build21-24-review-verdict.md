# Review verdict: Misthalin Mystery Builds 21-24 (patches 808-811) — PASS WITH FINDINGS

Reviewed: 2026-10-01 ~20:31 EDT. Four builds shipped in ~4 min on the same
theme (CUT_PAINTING unproved-dispatch recovery + telemetry for the varp-40
painting step); reviewed as one chain. Build 21 `c952bb48` 20:26:40 EDT,
Build 22 `9578e3fa` 20:28:30 EDT, Build 23 `098e7624` 20:29:59 EDT,
Build 24 `4bc176d1` 20:30:50 EDT.

## Custody — CLEAN (all four)
- All four commits single-purpose: `misthalinmystery-N.jar`,
  `misthalinmystery-plugin-N.jar`, `patch-M.hot.json`, `patch-M.zip`,
  `source-review/misthalinmystery-buildN/*`, version.txt
  (fresh numbers 808/809/810/811, no reuse, no overwrite).
- Jar sha256 == hot.json sha256 FULL MATCH on all four (via contents API,
  self-verified): B21 `577e6e83de91540b…`, B22 `55012b71aa595882…`,
  B23 `f067c4ca416d49b0…`, B24 `ad04eaa1fa23e9e8…`.
- All four zips: 258 entries, `net/` rooted, in-zip `version.txt` == patch N.
  `META-INF/MANIFEST.MF` is the genuine RuneLite manifest
  (`Main-Class: net.runelite.client.RuneLite` + Add-Opens/Add-Exports), not a
  `jar cf` default — injection-safe.
- `BUILD_NUMBER=21/22/23/24` confirmed via javap on the shipped jars.
- Config/Plugin/README byte-identical B21->B22->B23->B24.

## Delta (attributed)
- Build 21: new persisted single-shot flag `paintingDelayedRetryUsed` clears
  "Unproved CUT_PAINTING after 2 dispatch" when observed state matches
  (varp==40, island(), exactly 1 KNIFE, full HP, within 6 of PAINTING) ->
  logs `CUT_PAINTING_DELAYED_SELECTION_ONCE`, hold cleared, pending reset.
  Plus: item-use selection now sleeps 150ms then does a client-thread read of
  `client.getSelectedWidget()`/`isWidgetSelected()`, logging
  `ITEM_USE_SELECTION`, before the existing `Rs2Inventory.isItemSelected()`
  proof gate — directly answers the MM20-2 "same-tick selection proof"
  finding from the Build 20 verdict.
- Build 22: new `EventBus.Subscriber menuSubscriber` registered in `run()` on
  `MenuOptionClicked`, filtered to `ObjectID.MISTMYST_PAINTING`, logging
  `PAINTING_MENU_EVENT` (action/id/scene x/y/item/widget/consumed);
  unregistered in both `shutdown()` and `quiesceForReload()`. New persisted
  single-shot flag `paintingEventProbeUsed` clears
  "Unproved CUT_PAINTING after 3 dispatch" under the same observed-state gate
  -> logs `CUT_PAINTING_EVENT_PROBE_ONCE`.
- Build 23: diagnostic-only telemetry — `observe()` (client thread) scans
  `Client.getChatLineMap()` for the newest GAMEMESSAGE/ENGINE/SPAM/DIALOG/
  MESBOX message (`recentGameMessage`); painting objects now also capture
  class name + local point + worldView id (`paintingType`). Both persisted to
  status properties.
- Build 24: `recentGameMessage` widened from single-newest to the 6 newest
  GAMEMESSAGE/SPAM/MESBOX messages, `ts:type:value;`-joined (ENGINE and
  DIALOG dropped from the filter).

## Correctness review
- Escalation chain coherent: three ONCE gates for "after 1/2/3 dispatch"
  with identical fail-closed observed-state predicates; each clears the hold
  so the next tick retries with the next strategy (menu-alternate ->
  delayed-selection -> event-probe). No hold/clear churn (each gate has its
  own persisted single-shot flag — D17-1 class avoided).
- API-verified against installed microbot-base.jar: `Microbot.getEventBus()`
  exists; `EventBus.register(Class,Consumer,float)` returns `Subscriber`;
  `unregister(Subscriber)` exists; `MenuOptionClicked` has all seven logged
  getters; `gameval.ObjectID.MISTMYST_PAINTING=29650` (already used at three
  other sites in B21); `stopped` is `volatile`, so the client-thread lambda
  read is safe; `Client.getChatLineMap()` -> `Map<Integer,ChatLineBuffer>`;
  `ChatLineBuffer.getLines()` -> `MessageNode[]`; `MessageNode`
  getType/getValue/getTimestamp all exist; ChatMessageType has
  GAMEMESSAGE/ENGINE/SPAM/DIALOG/MESBOX.
- B21's 150ms sleep is on the script tick thread, bounded; interruption
  re-interrupts and returns false (fail-closed). Final `isItemSelected()`
  proof remains the gate.
- B23/B24 chat scan runs inside `observe()` on the client thread — correct
  thread; bounded (buffers x lines), null-guarded.

## Findings (low/info, routed read-only)
- [LOW NEW] D21-1: all three CUT_PAINTING gates require `f.hp==f.maxHp` —
  any chip damage leaves the hold uncleared. Narrow-by-design (D17-3 shape),
  fail-closed and diagnosable.
- [info NEW] D22-1: MenuOptionClicked probe is diagnostic-only (no logic
  consumer yet — the diagnose-then-fix pattern); `run()` re-registration
  would double-log if `run()` were called twice without shutdown, but
  `quiesceForReload()` unregisters — negligible.
- [info NEW] D22-2: each build adds a fresh single-shot flag keyed to a new
  hold string, so no D12-1-class stale-flag collision.
- [info NEW] D24-1: B24 narrowed the chat filter (dropped ENGINE and DIALOG
  vs B23) — diagnostic-only, no functional impact.
- Carried: D16-1/D16-2, D14-1, D12-1, D6-1, README drift (still documents
  build 2 while banner=24), D3-2, mirror telegraph (varp 110/111) unproven
  live, FINISHED branch silent hold-clear.

## Acceptance criteria (when feed returns)
- Expect `RUNNING_BUILD=21..24` in the startup banner; `ITEM_USE_SELECTION
  selected=...` lines; `CUT_PAINTING_DELAYED_SELECTION_ONCE` /
  `CUT_PAINTING_EVENT_PROBE_ONCE pos=...` on 2nd/3rd unproved dispatch;
  `PAINTING_MENU_EVENT` lines on any painting click; `paintingType` /
  `recentGameMessage` in status telemetry. Feed dark since 2026-09-30
  17:44 EDT — nothing live-verifiable from here.
