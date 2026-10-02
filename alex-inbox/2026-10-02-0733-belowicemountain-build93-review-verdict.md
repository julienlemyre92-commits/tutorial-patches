# Below Ice Mountain Build 93 / patch-955 — read-only review verdict

Reviewed by: Muse (review loop, read-only — Alex owns implementation/releases; no ships)
Commit: 53e77828c58adedf7a88ae7046205c5e3f2efa26, landed 2026-10-02T11:32:12Z (07:32:12 EDT)
Review time: 2026-10-02 ~07:34 EDT
Verdict: **PASS WITH FINDINGS**

## Custody (all independent API/blob reads this run)

- version.txt (repo) = 955 == in-zip version.txt = 955. Sequential (954 -> 955), no reuse.
- patch-955.zip: root `net/` (313 files; same expected entry count as 953/954), official-jar MANIFEST.MF present (recurring, benign — injection is an overlay).
- patch-955.hot.json: build=93, sha256 `525221a82bbeac999f05d515689dab80350f525ad17713ea80a06372377d2bbf`
  == sha256(belowicemountain-93.jar, 107849 B). FULL MATCH.
- BUILD_NUMBER=93 in published source (`source-review/belowicemountain-build93/BelowIceMountainScript.java`).
- Commit message says "Below Ice Mountain Build91 empty equipment bridge" but ships Build 93 / patch-955 (recurring commit-message reuse).

## Delta B92 -> B93 (source diff, surgical)

Single purpose: stage-35 food acquisition simplified from mixed salmon/trout to **trout-only**.

- Proved latch: `f.count(ItemID.TROUT)>=10` -> `hold("Stage35 trout acquisition proved; cave food/healing preflight still requires review: ...")`.
  (B92 latched at mixed food count >= 8; the hold is the deliberate pre-cave review gate, by design.)
- Coin ceiling 281 -> 270 ("F2P trout offer ceiling"); deficit/withdraw logic unchanged in shape.
- GE path: trout-only QuestGeBuyer, `quantity=10-troutCount`, `cap=min(1000, coins)`;
  checkpoint moved to STAGE35_TROUT_CHECKPOINT (PID + account + stage "35" + item id validated).
- Bank path unchanged: ENTRY_FOOD={LOBSTER, TUNA, SALMON} withdrawal up to 8, then coin deficit -> GE.
- Trout is NOT in ENTRY_FOOD -> no double-counting; the trout latch is independent of the bank-food path.

## Findings

- **W1 (carried from B92 F1, now item-specific):** a stale STAGE35_TROUT_CHECKPOINT file from a prior session +
  a new session (new PID) entering stage35RecoveryGe -> `loadStage35Checkpoint` throws on PID mismatch ->
  `hold("Stage35 GE checkpoint mismatch")` instead of rebuying. Checkpoint files are never deleted anywhere in
  the source (only the STATUS file is deleted). Mitigations: only bites when the file exists AND trout<10 at a
  fresh session start; hot-reload keeps PID so reloads don't trigger it. Suggestion: on PID mismatch, delete
  the stale checkpoint and start a fresh buy instead of HOLDing.
- **I1:** commit message reused ("Build91") for a Build-93 ship (recurring).
- **I2:** official-jar MANIFEST.MF in the patch zip (recurring, benign).
- **I3:** `stage35RecoveryFoodCount()` is now dead code (zero callers after the trout-only switch).
- **I4:** stale hold string "Stage35 GE target exhausted without eight raw inventory foods" — target is now 10 trout.
- **I5:** 270-coin ceiling has ~10gp margin over the observed trout high (22–26gp, wiki-verified at the B92 review).
  A price spike to 27gp+ forces the `<270` HOLD gate — by design, but thin.
- **I6:** README under `source-review/belowicemountain-build93/` contains zero mentions of builds 90–93 or trout —
  still the stale early copy (carried from B90 F1).

## Live acceptance (PENDING)

Screenshot feed dark since 2026-09-30 17:44 EDT (~37.9h); no confirmed live stream URL. Watch for:
`RUNTIME BUILD 93`, `STAGE35_GE_START`, `STAGE35_GE_ORDER_PROVED`, and the
"Stage35 trout acquisition proved" hold line.

Note: B92 (583dcfba, PASS WITH FINDINGS at 07:31 EDT) was never live-accepted (feed dark);
acceptance evidence resets to B93.
