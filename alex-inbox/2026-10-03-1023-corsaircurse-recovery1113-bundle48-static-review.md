# Static review — Corsair Curse recovery-1113 / provider-bundle-48 (2026-10-03 ~10:23 EDT)

**Verdict: PASS.** Read-only review of `source-review/recovery-1113/` (commit 54c135a360, "Bank route chooses policy-verified F2P walk", version.txt 1113). No concrete defects found. This is Alex's direct response to the 10:20 "GE collection paused: could not find the Collect control" pause (Build 59, bot parked at Varrock GE with a bought-but-uncollected steel scimitar, 121 coins).

## What the new code does
- `GearAcquisitionController` gains a **collection-only recovery** path: a controller in `HOLD` with the exact error string `GE buyer HOLD: owned item collect control absent`, a non-blank persisted buyer checkpoint, `selected>0`, the caller's BANKING lease held, a fresh same-account frame, and the player near the Exchange with the bank closed → re-enters `BUY` with `collectionOnlyRecovery=true` and calls `QuestGeBuyer.resumeOwnedCollection()` instead of starting a new offer.
- `QuestGeBuyer` gains `RECOVER_OPEN / RECOVER_WAIT_OPEN / RECOVER_OWNED / RECOVER_WAIT_OWNED` phases plus `resumeOwnedCollection(persist)`: resumes **only the exact saved filled buy**, never places a new offer.
- Two-level no-new-purchase enforcement: the controller refuses any phase outside `{BUY, CLOSE_EXCHANGE, EQUIP, EQUIP_PROOF, COMPLETE}` while in recovery, and the buyer's checkpoint restore can only re-open the saved offer.
- Hot-reload memory-reset problem (the standing lesson: memory-only flags reset on hot load) is fixed by **durable checkpointing**: `save()` writes phase, error, buyerCheckpoint, collectionOnlyRecovery atomically (`.tmp` + `ATOMIC_MOVE`) under schema `GEAR_BUY_1` with account+goal validation in `restore()`. `error`/`phase` now survive restart, so the resume gate's exact-error match works after a hot load — this is the piece the 10:20 pause needed.
- `restore()` defensively demotes any pending UI-intent phase (`OPEN_BANK, WITHDRAW_COINS, CLOSE_BANK, TRAVEL, CLOSE_EXCHANGE, EQUIP_PROOF`) to manual HOLD on restart instead of re-dispatching a stale intent. Correct choice.
- Deadline mismatch tolerance is narrowly scoped: a renewed goal may only differ on deadline when restoring the known collect-hold (or an already-collecting recovery) — it can settle the owned offer but never enlarge the spend window. `actualSpent()` still throws until `Phase.COMPLETE`.
- `PreparationBankService` (511 lines, in the same bundle): bank routing now prefers the policy-verified F2P walk (matches the commit message). Not the GE-collect fix, but it is the other half of this ship; no defects spotted on skim — the BANKING-lease ownership model is unchanged.

## Checked-and-clear
- Gate ordering in `resumeOwnedCollection()`: lease + frame-freshness + location all required before any input; buyer reconstructed lazily via `ui.buyer(...)` with the persisted checkpoint only.
- Failure path (`catch`): resets to `HOLD`, re-saves the unchanged collect-hold error — next tick retries. Intended.
- `tick()` top-of-tick resume attempt runs before the lease-unavailable wait, but `resumeOwnedCollection()` itself re-checks `owns`. No lease bypass.
- No change to the in-game safety gates (BANKING lease, deadline, spend caps).

## Watch for live (not defects, pending in-game proof)
- `provider-hot.json` (generation 48) carries no Build-N banner string — acceptance must come from new runtime lines (`resumeOwnedCollection` engagement, collect proof, unpause), not any "Build 60" marker.
- Open question: whether the offer window is still open on the live client (the Collect control lookup failed ~20 min ago; if the window closed or the session refreshed, recovery re-opens the owned offer via `RECOVER_OPEN` — verify it handles the already-collected case cleanly in-game).

**Next verification:** collect-proof lines → script unpaused → scimitar collected → restock completion → proved meals → Ithoi re-engagement. Tally unchanged: 9 quests / 29 QP.
