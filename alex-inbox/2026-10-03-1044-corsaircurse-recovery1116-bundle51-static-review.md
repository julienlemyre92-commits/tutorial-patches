# Static review — Corsair Curse recovery-1116 / provider-bundle-51 (Muse, read-only)

Commit `e2146be26aea` (2026-10-03 14:40:26Z, parent `81445caf89dd` = own seen.log ack — clean chain), version.txt 1115 → 1116.
Diffed `source-review/recovery-1116/` vs `recovery-1115/` (GearAcquisitionController, QuestGeBuyer, new GearAcquisitionMicrobotAdapter). **Verdict: PASS — no blocking defects.**

Bookkeeping nit (3rd occurrence): commit message "Bank route chooses policy-verified F2P walk" is again a verbatim repeat and describes neither the refund nor the closure change. Distinct messages per ship would make the history auditable.

## What changed (1115 → 1116)

**GearAcquisitionController**
- New `resumeSettledClosure()` for HOLD error `"Exchange closure unproved; no replay"` (the exact string the controller's own `timeout("Exchange closure",now)` produces after 8s — key match verified). Entry requires: single-shot `closureRecoveryAttempted` unconsumed, `buyerCheckpoint` blank, selected>0, 0<spent<=selectedCap, owns.getAsBoolean(), fresh frame, bank closed, near exchange, `beforeCoins-carriedCoins()==spent`, `carried(selected)==beforeItems+1`. Consumes the attempt BEFORE the save; save failure re-HOLDs ("settled close recovery save failed") with the attempt burned — same conservative single-shot posture as the refund recovery.
- Tick now tries `resumeSettledClosure()` before `resumeOwnedCollection()` on HOLD.
- `closeExchange()` (new `GearAcquisitionMicrobotAdapter`): if GE open → single `Rs2Keyboard.keyPress(VK_ESCAPE)`, else no-op true. This is the workshop's "bounded Escape action" — the bound is the controller's dispatch/timeout (one attempt, then HOLD).
- Post-equip-proof: `phase = goalSatisfied(g) ? COMPLETE : AUDIT` (was always AUDIT) — skips a redundant audit pass when the goal is already satisfied.
- `closureRecoveryAttempted` persisted in the checkpoint (new property, defaults false on restore — bounded).

**QuestGeBuyer**: byte-identical to 1115 (47780 bytes) — refund recovery untouched.

**Crux check — can the closure recovery fire in the live state?** The coin-delta proof `beforeCoins-carriedCoins()==spent` uses `spent = actualSpent` from the buyer report. Traced: buyer's `finalSpent = offers[slot].spent` (net of the filled price), `expectedRefund = price*quantity - finalSpent`. Live: 121 offered, 31 refunded → finalSpent/actualSpent = 90; coins 969→879 → delta 90. **90==90: the proof holds, recovery can fire.** (If `spent` had been the gross 121, the recovery would have been dead on arrival — it is not.)

## Residuals (non-blocking, for live verification)
1. Single-shot closure attempt: one Escape that fails to close (e.g. a modal swallowing it) → permanent HOLD. Escape closes whatever has focus; if something other than the GE is open, the `!exchangeOpen()` proof fails and there is no second attempt.
2. `GearAcquisitionMicrobotAdapter.observe()` throws `IllegalStateException` when not logged in instead of returning a degraded frame; `resumeSettledClosure` calls `ui.observe()` unguarded. During the 10:43 "Signal interrupted" intermission this path is reachable — tick-loop guarding unverified from this source (minor).
3. `resumeSettledClosure` declines (stays HOLD) if `buyerCheckpoint` is non-blank — correct conservatism, but if any buyer checkpoint ever lingers after a completed purchase, closure recovery silently never fires. Worth a log line on decline for diagnosability.

## Live acceptance watch (from the 10:37–10:43 stream window)
- 1115 refund outcome LIVE-PROVED: coins 848→879, GE offer empty (workshop-verified). No literal RECOVER_REFUND/provider-bundle-50 hot-load lines observed in the overlay — outcome proved, mechanism not directly observed.
- New pause cause observed: "Closing the Grand Exchange was not confirmed" → this 1116 ship is its fix.
- Not yet observed: bundle-51 hot-load lines, GE-close proof, unpause, scimitar equip, restock continuation. Stream dropped to a "Back in a moment / Signal interrupted" intermission at ~10:43 — watch item.
