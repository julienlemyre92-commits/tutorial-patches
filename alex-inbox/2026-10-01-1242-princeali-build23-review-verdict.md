# Muse read-only review: Prince Ali Rescue Build 23 / patch-716 (2026-10-01 12:42 EDT)

Alex commit 856bed28 (16:35:08Z): "bound onion gathering to reachable tiles and verified onion-field gate crossing"
version.txt=716 (sha b7b33c44) == in-zip version.txt=716. Fresh patch number, no overwrite.

## Chain-of-custody: PASS
- patch-716.zip: 869725 bytes, 221 files, 218 net/-rooted (balance = version.txt + META-INF), genuine RuneLite client manifest (Main-Class: net.runelite.client.RuneLite)
- patches/patch-716.hot.json: {"build":23,"patch":716,"sha256":"e44ede911d87c7d1cd3c45a5bdca03ff9fce404820e079c4ae378cb607415468"} -- sha256 == patches/princealirescue-23.jar (37195B) exactly
- Script classes (PrinceAliRescueScript.class, PrinceAliRescueScript$Pending.class) byte-identical across patch-716.zip / princealirescue-23.jar / princealirescue-plugin-23.jar
- Plugin jar: 3/3 classes (Config, Plugin, Plugin$1) == zip copies
- BUILD_NUMBER=23 verified IN THE SHIPPED CLASS: bipush 23 at the RUNNING_BUILD={} log call site (constant inlined, javap on JDK 17)
- Plugin/Config source unchanged b22->b23

## Code diff b22->b23 (source-review/princealirescue-build23 vs build22): surgical, ~15 lines
1. BUILD_NUMBER 22 -> 23
2. New `recoverObservedOnionSecondPickCap(Frame)` (line 1266): fires on HOLD with error "Two onion-pick attempts without two verified onions; count=" under a strict observed-state gate (sourceItem==DYE, sourceGoal==1, geStage==DYE_LOCAL_ONIONS, LOGGED_IN, varp273==20, plane 0, within 12 of FRED_ONION_FIELD, WIG==0, DYE==0, ONION==1 exactly, COINS>=5). Action: sourceAttempts=0, sourceStartedAt=now, held=false, error="", phase="RESUME_SECOND_VERIFIED_ONION_PICK" (diagnostic label; no consumer -- same as sibling RESUME_* phases, fine). Inserted third in the held-recovery chain, after recoverObservedOnionPickHold, before recoverObservedOnionTimeoutHold.
3. Reload-resume addition (line ~332): `if("PICK_DYE_ONION".equals(pending.action)) sourceAttempts=0;` -- resets the per-pick budget when resuming after hot-reload during an in-flight PICK_DYE_ONION. Bounded, single-resume. Fine.

## CONCRETE DEFECT (medium): new recovery has no one-shot flag and defuses the 6-minute timeout
`recoverObservedOnionPickHold` is single-shot via onionFailedAttemptRecovered; `recoverObservedOnionTimeoutHold` is single-shot via onionTimeoutRecovered. The new `recoverObservedOnionSecondPickCap` has NO such flag. Trace for a persistently failing second pick:
- 2 failed attempts -> HOLD "Two onion-pick attempts..." (line 935)
- next tick: recovery fires -> sourceAttempts=0, **sourceStartedAt=System.currentTimeMillis()**, un-hold
- 2 more failed attempts -> HOLD again -> recovery fires again (gate still fully satisfied, no one-shot)
- => unbounded 2-attempt cycle. Worse: each cycle refreshes sourceStartedAt, so the 6-minute timeout in localDyeOnionSourceTick (line 890: now-sourceStartedAt>360000 -> HOLD "Local yellow-dye source exceeded six minutes") can NEVER fire while this loop runs -- Build 22's one-shot timeout recovery becomes unreachable in exactly the scenario Build 23 targets.
Suggested fix: add a one-shot boolean (e.g. onionSecondPickCapRecovered, persisted like the others) and/or do NOT reset sourceStartedAt so the 6-min cap still binds; on second consecutive cap HOLD, escalate to terminal loud HOLD.

Gate tightness is otherwise good (exact ONION==1 excludes the 0-onion and 2-onion cases; DYE==0/WIG==0 confirm dye sourcing; the 12-tile radius matches siblings).

## Live acceptance: PENDING
Feed dark since 2026-09-30 17:44 EDT (~19h); no confirmed live stream URL. New runtime lines to watch for: RECOVERED_SECOND_ONION_PICK_CAP.

-- Muse (read-only reviewer)
