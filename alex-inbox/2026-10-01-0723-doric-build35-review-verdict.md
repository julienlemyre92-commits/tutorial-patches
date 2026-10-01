# Muse review-loop verdict: Doric Build 35 (patch-674) -- PASS
Date: 2026-10-01 ~07:23 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)

- Ship: patch-674, commit e1805a6d 2026-10-01T11:15:36Z "Doric Build35: rescan alternate tin rock after no-change hold". version.txt=674.
- Packaging clean: 215-entry net/-rooted zip, version.txt=674 in+out (no reuse), 200 classes, class lists identical 673->674.
- Byte-level delta 673->674 confined to the same 5-class changed set as Builds 26-34 (Plugin, Script, Frame, LoginFrame, Pending); Frame/LoginFrame/Pending and Plugin show zero signature changes; Plugin -c diff is BUILD_NUMBER bipush 34->35 only. Script: +2 fields (avoidedTinRock:WorldPoint, avoidedTinRockUntil:long), zero method signature changes (125 methods); lambda$trainMining$5 static->instance (captures this for the new fields).
- Semantics: rock-scan predicate (lambda$trainMining$5) now excludes the avoided rock while System.currentTimeMillis() < avoidedTinRockUntil (30s cooldown from diag string "cooldownMs=30000"); tin/copper id gates unchanged. No-change hold now sets avoidedTinRock/Until and enters RESCAN_ALTERNATE_TIN_ROCK -> WAIT_TIN_ROCK_RESCAN instead of re-hammering a (depleted?) rock. New diag lines: TRAINING_MINE_NO_CHANGE exact=true avoidedRock={} cooldownMs=30000 decision=RESCAN_ALTERNATE; LATE_TRAINING_ACTION_PROVED tinBefore/Now + xpBefore/Now (proven action resets counters). Bounded (time-based 30s, exact no-change proof). Matches commit message exactly.
- Zero new net/runelite/api type refs (identical sets 673 vs 674).
- Hot chain VERIFIED: patch-674.hot.json sha256 (349b96a5...) == doricsquest-35.jar file bytes (28,092) exact; jar DoricsQuestScript.class byte-identical to patch-674.zip's.
- Nothing to fix. Live acceptance pending: RESCAN_ALTERNATE / LATE_TRAINING_ACTION_PROVED runtime lines (feed dark; rests on Alex's in-chat reports).
