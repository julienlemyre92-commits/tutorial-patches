# Below Ice Mountain Build 94 / patch-956 — read-only review verdict

Reviewed by: Muse (review loop, read-only — Alex owns implementation/releases; no ships)
Commit: e3917279ac21a01d610745a3a814f377ca1b5ba2, landed 2026-10-02T11:35:00Z (07:35:00 EDT)
Review time: 2026-10-02 ~07:37 EDT
Verdict: **PASS WITH FINDINGS**

## Custody (all independent API/blob reads this run)

- version.txt (repo) = 956 == in-zip version.txt = 956. Sequential (955 -> 956), no reuse.
- patch-956.zip: root `net/` (313 files; expected entry count), official-jar MANIFEST.MF present (recurring, benign).
- patch-956.hot.json: build=94, sha256 `a4cc50b119644b3e1887a316f929a120fedd510cb86c0c61c92a2ac20379f31f`
  == sha256(belowicemountain-94.jar, 108134 B). FULL MATCH.
- BUILD_NUMBER=94 in published source (`source-review/belowicemountain-build94/BelowIceMountainScript.java`).
- Commit message again says "Below Ice Mountain Build91 empty equipment bridge" but ships Build 94 / patch-956 (recurring).

## Delta B93 -> B94 (source diff, surgical — GE checkpoint resume)

Two additions, one purpose: make the stage-35 GE buy resumable with byte-exact parameters.

1. **Bank path** (`stage35RecoveryBank`, after the trout>=10 proved-latch): if STAGE35_TROUT_CHECKPOINT exists,
   load it; on PID/account/stage/item mismatch -> `hold("Stage35 trout checkpoint mismatch")`; if the saved
   checkpoint's buyer phase is not COMPLETE (`|COMPLETE|` token = buyer's phase name) -> route to
   `stage35RecoveryGe` to resume the in-flight offer instead of recomputing from inventory.
2. **GE path** (`stage35RecoveryGe`): checkpoint now loaded FIRST; if non-blank, parsed as
   `GE2|<item>|<quantity>|<cap>|...` (schema + item validated, mismatch ->
   `hold("Stage35 GE checkpoint parameters invalid")`); the restored quantity/cap are passed to the
   `QuestGeBuyer` constructor, whose `restore()` requires an EXACT itemId/quantity/cap match
   ("foreign/invalid GE checkpoint" otherwise).

**What this fixes (real B93 defect):** B93 recomputed `quantity=10-troutCount` fresh on every GE entry and passed
the persisted checkpoint through. If any trout had arrived outside the buyer's accounting, the recomputed
quantity mismatched the checkpoint's stored quantity -> `restore()` threw ->
`hold("Stage35 GE buyer invalid: foreign/invalid GE checkpoint")`. B94 eliminates that class: resume always
uses the checkpoint's own quantity/cap, and the buyer's phase/progress restores from the checkpoint itself,
so no double-buy and no re-quote.

## Findings

- **W1 (reframed, carried):** cross-session stale checkpoint + new PID -> `hold("Stage35 trout checkpoint mismatch")`,
  now also reachable from the bank path (earlier surfacing). Fail-closed by design — the checkpoint pins a live GE
  offer whose state is unknown to the new session; holding beats double-buying. Checkpoint files are still never
  deleted. Only bites when the file exists AND trout<10 at a fresh session start; hot-reload keeps PID.
- **I1:** commit message "Build91" reused for a Build-94 ship (recurring).
- **I2:** official-jar MANIFEST.MF in the patch zip (recurring, benign — injection is an overlay).
- **I3:** `stage35RecoveryFoodCount()` still dead code (carried from B93).
- **I4:** stale hold string "without eight raw inventory foods" — target is 10 trout (carried from B93).
- **I5:** 270-coin ceiling has ~10gp margin over the observed 22–26gp trout high (carried from B93).
- **I6:** README under `source-review/belowicemountain-build94/` still the stale early copy, zero mentions of
  builds 90–94 or trout (carried).
- **INFO (new):** script-level GE2 pre-parse requires >=5 fields, but `restore()` requires exactly 20 — a truncated
  checkpoint passes the script gate then throws in the constructor -> caught by the "buyer invalid" hold. Fail-closed.
- **INFO (new):** resume reuses the construction-time cap (coins at order placement), not current coins — correct
  remaining-budget semantics; a mid-session coin drain surfaces as the buyer's NEED_COINS outcome -> hold. Fail-closed.

## Live acceptance (PENDING)

Screenshot feed dark since 2026-09-30 17:44 EDT (~38h); no confirmed live stream URL. Watch for:
`RUNTIME BUILD 94`, `STAGE35_GE_START`, `STAGE35_GE_ORDER_PROVED`, and the
"Stage35 trout acquisition proved" hold line.

Note: B93 (53e77828, PASS WITH FINDINGS at ~07:34 EDT) was never live-accepted (feed dark);
acceptance evidence resets to B94. Alex's ship cadence is currently ~1 build / 3 min (B91->B94 in ~18 min).
