# Below Ice Mountain Build 87 — read-only review verdict

- **Build:** 87 / patch-948 (commit 7b801951, "Below Ice Mountain Build87 stage35 safe bank restock", 2026-10-02T10:51:00Z)
- **Reviewed:** 2026-10-02 ~06:54 EDT by review-loop (Muse, read-only — Alex owns implementation/releases)
- **Verdict:** PASS WITH FINDINGS (no blocking defects)

## Custody chain — AIR TIGHT
- version.txt=948 at observe (API re-read; matched patch number; later superseded by B88/patch-949 — see race note).
- patch-948.zip: 294 entries, `net/`-rooted (+ META-INF/, MANIFEST.MF, root version.txt — same manifest pattern as patches 936+). In-zip version.txt=948. Zipped 2026-10-02 06:50.
- Standalone jar belowicemountain-87.jar sha256 `8a5e3d03de5f04dfe7d8c2a2ef6d24540456bcacbd2844e1212790f6b0b9624b` == patch-948.hot.json sha256 — FULL MATCH (git-blob raw downloads).
- BUILD_NUMBER javap-verified: 87 (from the shipped jar's BelowIceMountainScript.class).
- Published source-review/ script byte-compiles on JDK 17.0.20.1 with ONLY the 2 pre-existing BelowIceMountainConfig symbol errors (lines 428/727 — Config class compiled separately; identical error set to B86, carried B71→B86). Zero new compile errors.
- README byte-identical to B86 (37031 bytes, no B87 section — carried INFO pattern).

## Delta B86→B87 (+38/−13 lines; script 4177→4215 lines)
1. BUILD_NUMBER 86→87.
2. New food gate in the stage-35 surface-prep branch (`observedQuestStage==35 && overworldPrepArea(f.position) && !guardianActive`), after pending-proof settlement, before dialogue/preflight:
   - `if (entryFoodCount(f)<8) { stage35RecoveryBank(f); return; }`
   - `if (f.bankOpen) { issue("stage35:bank-close",Proof.BANK_TOGGLED,f,0,7000,Rs2Bank::closeBank); return; }`
   - then existing: dialogue → `dungeonEntryAllowed` preflight → `enterRuins`.
3. New `stage35RecoveryBank(Frame f)`: stage="STAGE35_RECOVERY_BANK"; re-drives active route; pace gate (WAIT_STAGE35_RECOVERY_BANK_PACE on shared nextAt); if bank not open, walks to LUMBRIDGE_BANK (3208,3220,2) r=5 and opens bank (issue "stage35:bank-open", Proof.BANK_TOGGLED, 10000ms); once open, withdraws min(needed,stock) per ENTRY_FOOD id ({LOBSTER, TUNA, SALMON} — all F2P) across ticks until 8; terminal diagnostic hold() if the bank holds no F2P food at all.
4. Two whitespace-only changes in prepBankAudit (trailing-space cleanup, no behavior change).

## Why it passes
- **Gate placement is safe by construction:** the food gate can only fire on the surface in the overworld prep area with the guardian inactive (per the branch comment: "A safety logout returns this private-server instance to the surface while preserving quest stage 35. Re-enter only after a fresh supply and supervised-entry check."). It can never divert a mid-cave player to Lumbridge.
- **Trigger is observed-state:** entryFoodCount from live inventory, bankOpen from the frame — a hot reload re-derives the branch next tick. No new memory-only latch (contrast B86's guardianRetreat). Fail-closed.
- **walk() semantics respected:** stale routes from other stages are cancelled by key/target mismatch and re-issued; arrival (near + route cancelled) falls through to openBank; the 50s-stall/2-failure route hold is the standard bounded pattern.
- **Withdrawal accumulates correctly across ticks:** needed is recomputed per tick, each tick withdraws one stocked id's min(needed,stock) and returns; exhaustion of all ids falls through to the honest no-food hold.
- **Lambda captures are effectively final** (loop-local `quantity`, per-iteration `id`) — no capture bug.
- **Threshold <8 is consistent** with the established entry-gate threshold (lines 1031, 2588); the >=10 elsewhere is the prep overstock target, not a contradiction.

## Findings
- **NEW INFO BIM87-1:** the no-F2P-food hold() is terminal and unbounded — no GE/shop fallback exists. Mitigating: it fires at a safe bank with an honest diagnostic ("Need a bounded GE/shop restock plan before cave entry"), and it is the declared gap, not a silent stall. Acceptable; worth a future bounded-restock plan.
- **NEW INFO BIM87-2:** README has no B87 section (byte-identical to B86) — carried doc pattern, not a defect.
- **Carried:** BIM86-1 (guardianRetreat memory-only, fail-closed), BIM86-2 (escape conditional on guardianActionsAllowed control), BIM85-1/85-2, 84-1 (mooted), 83-1/83-2, 82-1/82-2, 81-1/81-2, 78-1, 79-1, 75-1, 77-1, 71-1, 71-3..6, 61-1, 57-1, 68-1, 70-1/70-2, 72-1, 53-1.

## Live acceptance watch-keys (pending — feed dark since 2026-09-30 17:44 EDT)
- RUNTIME BUILD 87 marker on a fresh client.
- `STAGE35_RECOVERY_BANK` / `WALK_STAGE35_RECOVERY_BANK` stage lines, `stage35:bank-open` / `stage35:food:<id>` issue lines with the coins-count log, `stage35:bank-close` before re-entry dialogue.
- No live observation yet; B57–B87 live acceptance all pending, folded into the outstanding 03:21 dark-feed stream flag.

## Race note
Alex's B88 (123b34b1, "Below Ice Mountain Build88 Lumbridge bank floor", patch-949, version.txt=949) landed 10:53:52Z DURING this review — NOT reviewed here, pending next run.
