# Muse review-loop verdict: Doric Build 36 (patch-675) -- PASS
Date: 2026-10-01 ~07:24 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)

- Ship: patch-675, commit ddd63dc8 2026-10-01T11:19:38Z "Doric Build36: recover timed-out mine only after exact no-change". version.txt=675.
- Packaging clean: 215-entry net/-rooted zip, version.txt=675 in+out (no reuse), 200 classes, class lists identical 674->675.
- Byte-level delta 674->675 confined to the same 5-class changed set; Frame/LoginFrame/Pending zero signature changes; Plugin -c diff is BUILD_NUMBER 35->36 only. Script: +2 fields (failedTinRocks:List<WorldPoint>, trainingMineNoChangeAttempts:int), zero method signature changes.
- Semantics: no-change hold now dedupes avoidedTinRock into failedTinRocks, increments trainingMineNoChangeAttempts, sets delayedMineRecoveryPending, re-avoids for 30s. On any PROVED mining action: counter=0 + failedTinRocks.clear() (reset is action-proofed, not time-based). Terminal gate: attempts>=3 -> HOLD with "Three tin-rock interactions produced no ore or Mining XP; rescanned failed rocks=..." (full evidence), else phase WAIT_TIN_ROCK_RESCAN. New diag: TRAINING_MINE_DISPATCH rockId/rock/player/noChangeAttempts; WAIT_MINE_SETTLE phase; "Unsupported login index" login-frame robustness string. Bounded (3 attempts then terminal hold), matches commit message exactly.
- Zero new net/runelite/api type refs (identical sets 674 vs 675).
- Hot chain VERIFIED: patch-675.hot.json sha256 (45f5beda...) == doricsquest-36.jar file bytes (28,600) exact; jar DoricsQuestScript.class byte-identical to patch-675.zip's.
- Nothing to fix. Live acceptance pending: WAIT_TIN_ROCK_RESCAN / TRAINING_MINE_DISPATCH runtime lines (feed dark; rests on Alex's in-chat reports).
