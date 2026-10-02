# Review verdict: Below Ice Mountain Build 41 (patch-906) — PASS WITH FINDINGS

Reviewed 2026-10-02 ~01:37 EDT by Muse (read-only reviewer; Alex owns implementation/releases).
Commit: 0f2f2b5a61 "Below Ice Mountain Build41 GE collection completion" (2026-10-02T05:35:26Z).
version.txt = 906.

## Custody (air tight)
- `patches/patch-906.zip`: 284 entries (281 `net/` + META-INF/MANIFEST.MF + version.txt).
  Zip root is `net/`; entry NAME SET byte-identical to patch-905 (diff of sorted
  name lists empty). version.txt inside zip = 906.
- `patches/belowicemountain-41.jar` sha256 = 37c25fe9091f2de0...,
  identical to `patch-906.hot.json` sha256 (37c25fe9091f2de09c988881d7f7c7d3df3d081a3fa8f8c981277cdd8a0c7886).
- Shipped `BelowIceMountainScript.class` disassembled: `runtimeBuild()` =
  `bipush 41; ireturn`. No stale-class risk.
- Source: `source-review/belowicemountain-build41/BelowIceMountainScript.java`
  (113,356 bytes) + README.md with Build 41 changelog.

## What changed (B40 -> B41, 12 diff lines)
1. `BUILD_NUMBER` 40 -> 41.
2. `prepFood` early-HOLD gates now skip once the native buyer has started:
   - `entryFoodCount(f)>=10 && !prepNativeStarted` (was unconditional) — no more
     "High-heal food obtained" HOLD while the buyer is mid-collection.
   - `!prepNativeStarted && (slots<10)` — no more "Ten food slots required" HOLD
     while the buyer is mid-collection.
3. `prepNativeStarted` is restored from reload state (line 214) and persisted
   (lines 236, 1400), so the gate change survives hot reload.

## Logic verified
- Once `prepNativeStarted` is true, the only exits from `prepFood` are the
  buyer's own tick path: `WORKING` -> stage `PREP_NATIVE_GE_<phase>`;
  `COMPLETE && itemCount>=10` -> terminal HOLD "Native GE food purchase proved"
  (coin collection + owned slot clearance owned by QuestGeBuyer); any other
  outcome -> terminal HOLD with outcome/phase/reason. No second offer, no
  Willow/training actions — matches the README claim.
- Checkpoint resume: line 767-769 loads the durable checkpoint file (pid +
  account-name gated) on first entry; after that the gates let the buyer run to
  completion even with 10 tuna already carried and <10 free slots. This is the
  stated fix for B40's early food-count HOLD during collection.
- Buyer owns durable checkpoint persistence before each UI input
  (`savePrepNativeCheckpoint`, atomic move) — hot-reload safe.

## Findings
- PASS. No defects in the B41 change itself; the fix is surgical and matches
  its stated intent.
- INFO BIM41-1 (new, minor): `prepNativeStarted=true` is set at buyer
  construction (line 820) before the first checkpoint save inside the first
  tick. A hot reload landing in that window reconstructs the buyer with a blank
  checkpoint (line 767 only reloads when the flag is false). Pre-existing B40
  pattern; window is one tick; flag is persisted so the buyer restarts cleanly.
- Carried open: BIM40-1 (status-write lock contention, cosmetic), BIM40-2
  (stale fillAt restore edge), BIM40-3 (dead troutFour factory), BIM40-4
  (keyboard typing gated on input focus), bankCoins discrepancy (script debug
  3023 vs bot-maker panel "1,023 coins in the bank"), guardian controller
  unimplemented, BIM38-1/38-2/38-3.
- META-INF/MANIFEST.MF in the patch zip is a pre-existing jar-style build
  pattern (harmless on the hot-reload path).

## Live acceptance (pending)
Watch for overlay "RUNTIME BUILD 41" + NEW `PREP_NATIVE_GE_<phase>` step text
and buyer per-phase diag after re-arm. Acceptance only from new runtime lines,
never the banner alone. Script class marker verified above at the bytecode
level; live runtime lines still pending.
