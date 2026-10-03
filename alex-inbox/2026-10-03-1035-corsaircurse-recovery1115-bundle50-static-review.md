# Static review — Corsair Curse recovery-1115 / provider-bundle-50 (Muse, read-only)

Commit `2808a1f4fe37858183ee7a5cd61f6b6e478a688f` (2026-10-03 14:34:13Z, parent `61f70a824213`), version.txt 1114 → 1115.
Diffed `source-review/recovery-1115/` vs `recovery-1114/` (GearAcquisitionController, QuestGeBuyer). **Verdict: PASS — no blocking defects.**

Bookkeeping nit: commit message "Bank route chooses policy-verified F2P walk" is a verbatim repeat of the 1113 commit message and does not describe this change (refund recovery). Consider a distinct message per ship.

## What changed (1114 → 1115)

**GearAcquisitionController**
- Collection-recovery entry is now `buyer.resumeOwnedCollection(persist) || buyer.resumeOwnedRefund(persist)` (short-circuit: refund path only tried when item-collect recovery declines).
- The recognized recovery HOLD-error set gains `"GE buyer HOLD: coin refund not proved; no repeat"` (two sites: entry gate and checkpoint-error check) — the live refund-safety pause is now a recovery entry point.

**QuestGeBuyer**
- Five new phases `RECOVER_REFUND_OPEN → WAIT_OPEN → OWNED → WAIT_OWNED → COLLECT`, mirroring the item-collection recovery flow. Every phase re-proves `recoverableRefund(f)` and HOLDs on any mutation; waits bounded (8000/6000 ms); foreign GE forms rejected with HOLD ("no repeat").
- `recoverableRefund(f)` proof predicate is exact-match: owned slot, state==BOUGHT, filled==quantity, spent==finalSpent, `inventoryItem==initialItem+quantity`, `coins==coinsAfterPlace`, `expectedRefund==price*quantity-finalSpent`, >0, <=cap. No new purchase possible.
- `resumeOwnedRefund(persist)` entry gate: phase==HOLD, itemCollectClicked && coinCollectClicked, !refundProved, !refundRecoveryAttempted, !cancelled, ownedOfferSeen — then a FRESH frame must satisfy recoverableRefund before moving. If the offer screen already shows the item and a 995 collect control exists → RECOVER_REFUND_COLLECT; else → RECOVER_REFUND_OPEN (re-opens via GE clerk).
- Single-shot budget: new persisted `refundRecoveryAttempted` flag; checkpoint format GE2 20→20|21 fields, restore accepts both (old checkpoints default false — one bounded post-restore attempt, fine). RECOVER_REFUND_COLLECT consumes the flag BEFORE `clickVerifiedAction`; a rejected click HOLDs with "refund recovery click rejected; no further retry" and the attempt is burned.
- Control lookup: `CollectControl` gains `index`; `findCollect` fallback for 995 — exactly one control, index==3, itemId<=0 or 995, has "Collect" → treat as the refund button. Absent control → HOLD ("exact refund still uncollected but control absent"), never a blind click.
- HOLD message now differentiates post-attempt: "refund recovery attempt unproved; no further retry".

## Residuals (non-blocking, for live verification)
1. Single-shot burns on transient click rejection: the flag is consumed before the click, so one rejected (not failed) click leaves the refund permanently uncollected behind a manual HOLD. Defensible safety posture — just confirming the rejection path is genuinely rare.
2. The `index==3` lone-control heuristic for the refund button — verify against the installed GE code live (matches the ~10:28 "verifying the refund fix against the installed GE code" work).

## Live acceptance watch (not yet observed)
provider-bundle-50 hot-load lines → RECOVER_REFUND_* phase progression → 995 collect proof → unpause → gear pass → restock done → proved meals → Ithoi re-engagement.
