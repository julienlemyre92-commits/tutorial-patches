# Below Ice Mountain Build 88 — read-only review verdict

- **Build:** 88 / patch-949 (commit 123b34b1, "Below Ice Mountain Build88 Lumbridge bank floor", 2026-10-02T10:53:52Z)
- **Reviewed:** 2026-10-02 ~06:58 EDT by review-loop (Muse, read-only — Alex owns implementation/releases)
- **Verdict:** PASS WITH FINDINGS (no blocking defects)

## Custody chain — AIR TIGHT
- version.txt=949 at observe (API re-read, sha a0394c20; repo HEAD 44f6f260 = sibling's B87 seen.log update 10:54:40Z; zero commits since — no sibling race).
- patch-949.zip: 294 files, `net/`-rooted (+ META-INF/MANIFEST.MF, root version.txt — same manifest pattern as patches 936+). In-zip version.txt=949.
- Standalone jar belowicemountain-88.jar sha256 `937737eb0f0f4cce6ea43c9ea1ffd5a5dd4fd972d2beddc452dfa6a609e06295` == patch-949.hot.json sha256 — FULL MATCH (git-blob raw downloads).
- In-zip BelowIceMountainScript.class == standalone jar's class (identical sha256 d319ee0e...).
- BUILD_NUMBER javap-verified: 88 (ConstantValue int 88).
- Published source-review/ script byte-compiles on JDK 17.0.20.1 with ONLY the pre-existing error set (filename artifact + 2 BelowIceMountainConfig symbol errors at lines 428/727, carried B71→B87 — error set IDENTICAL to B87). Zero new compile errors.
- README byte-identical to B87 (37103 bytes, no B88 section — carried INFO pattern).

## Delta B87→B88 (+4/−3 lines; script 4215→4216 lines)
1. BUILD_NUMBER 87→88.
2. `overworldPrepArea(p)` widened (lines 1641-1644): was plane==0 && X in [2500,3500] && Y in [3000,3800]; now also `near(p,LUMBRIDGE_BANK,20)`. near() is plane-aware (plane equality required), so this matches only the plane-2 bank floor within 20 tiles of (3208,3220,2) — no plane leakage.

## Why it passes
- **This is the direct fix for B87's latent stage35RecoveryBank bug:** B87 walks the player to LUMBRIDGE_BANK (3208,3220,2) — plane 2 — but the parent stage-35 branch guard (`observedQuestStage==35 && overworldPrepArea(f.position) && !guardianActive`, line ~889) demanded plane==0. On arrival at the bank floor the recovery branch would deactivate; the player then falls through to the `unknownStageScene` computation (line 947: `stage>=35 && !overworldPrepArea && !instancedCave`) → `guardianContext=true` → `dungeonProbe(f)` (line ~993) instead of the bank-restock flow. B88 keeps the recovery branch alive on the bank floor, so the food gate / stage35RecoveryBank / bank-close sequence runs to completion.
- **Trigger stays observed-state:** position from the frame, plane-aware near() — a hot reload re-derives the branch next tick. No new memory-only latch.
- **Blast radius across the other overworldPrepArea usages is benign:** stage-30 exit proof (876); unknownStageScene (947 — bank floor no longer misclassified as a guardian scene); stage30Recovery (954); guardian emergency-exit proof (958/3022 — bank floor correctly counts as a safe surface scene); prep audit 20-30 (1004 — no longer terminal-holds at the bank; prepBankAudit already drives bank visits); random-dismiss gate (1100 — dismisses randoms at the bank, harmless); level-up cue safety (1147 — UI-cue gate only); training scene gate (1942); stage-40 approach (2455 — bank floor treated as safe overworld); DUNGEON_ENTERED proof (3352 — bank floor valid "before").
- The stage-35 branch still requires `!guardianActive` and settles pending proofs first; the food gate (<8) and bank-close ordering are unchanged from B87.

## Findings
- **NEW INFO BIM88-1:** the line-1004 widening loosens one fail-closed guard — stages 20-30 at the bank floor no longer terminal-hold("Prep audit cannot route from unmapped scene") but proceed into the prep audit. Benign in practice (the bank is a legitimate prep stop and prepBankAudit routes bank visits), but it is the one widening side-effect worth watching on live evidence.
- **NEW INFO BIM88-2:** README has no B88 section (byte-identical to B87) — carried doc pattern, not a defect.
- **Carried:** BIM87-1 (no-food hold terminal/unbounded, safe bank, honest msg), BIM87-2 (README no B87 section), BIM86-1/86-2, 85-1/85-2, 84-1 (mooted), 83-1/83-2, 82-1/82-2, 81-1/81-2, 78-1, 79-1, 75-1, 77-1, 71-1, 71-3..6, 61-1, 57-1, 68-1, 70-1/70-2, 72-1, 53-1.

## Live acceptance watch-keys (pending — feed dark since 2026-09-30 17:44 EDT)
- RUNTIME BUILD 88 marker on a fresh client.
- `STAGE35_RECOVERY_BANK` stage lines PERSISTING while on the bank floor (plane 2), `stage35:bank-open` at the bank, `stage35:food:<id>` withdrawal lines, `stage35:bank-close` before re-entry dialogue.
- No `dungeonProbe`/guardian lines at the bank floor on stage 35.
- No live observation yet; B57–B88 live acceptance all pending, folded into the outstanding 03:21 dark-feed stream flag.
