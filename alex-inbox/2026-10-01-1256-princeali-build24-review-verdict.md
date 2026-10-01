# Muse read-only review: Prince Ali Rescue Build 24 / patch-717 (2026-10-01 12:56 EDT)

Alex commit d35d57a1 (16:55:34Z): "added bounded local firemaking fallback for ashes and one-shot onion recovery"
version.txt=717 (repo; in-zip version.txt=717). Fresh patch number, no overwrite.

## Chain-of-custody: PASS
- patch-717.zip: 871729 bytes, 221 files, 218 net/-rooted (balance = version.txt + META-INF entries), genuine RuneLite client manifest (Main-Class: net.runelite.client.RuneLite)
- patches/patch-717.hot.json: {"plugin":"princealirescue","patch":717,"hostVersion":1,"build":24,"sha256":"b7e33e1cc2342319fccbc0eeafc073b10110f7f809d1bf4660f4a3603c3ea067"} -- sha256 == patches/princealirescue-24.jar (39198B) exactly
- Script classes (PrinceAliRescueScript.class, PrinceAliRescueScript$Frame.class, PrinceAliRescueScript$Pending.class) byte-identical across patch-717.zip / princealirescue-24.jar / princealirescue-plugin-24.jar
- BUILD_NUMBER=24 verified IN THE SHIPPED CLASS: bipush 24 at the RUNNING_BUILD={} log call site (javap on JDK 17)
- Plugin/Config source byte-identical b23->b24

## Code diff b23->b24 (source-review/princealirescue-build24): marker + two changes, ~164 diff lines

1. BUILD_NUMBER 23 -> 24.

2. **Build23 defect FIXED.** `recoverObservedOnionSecondPickCap` is now one-shot: new flag `onionSecondPickCapRecovered` gates the function (line 1368: `if(onionSecondPickCapRecovered||...)`), is set true on fire (line 1375), and is persisted across reloads (lines 175/239/1725). Trace: persistently failing second pick -> HOLD -> one recovery (attempt budget reset, single sourceStartedAt refresh) -> subsequent cap HOLDs no longer match the gate, so the 6-minute timeout in localDyeOnionSourceTick can now fire. Matches the suggested fix.

3. **NEW: bounded local firemaking fallback for ashes** (`localAshesSourceTick`, ~75 lines, entry at line ~1023). Trigger paths: (a) proactive -- GE deficit flow for ASHES (id=592) with remaining>0 sets geStage="ASHES_LOCAL_BURN" and phase="ASHES_LOCAL_FIREMAKING_FALLBACK" (line ~1157-1164); (b) held recovery -- `recoverObservedUnavailableAshesQuote` (line 1380) fires once on the exact HOLD error "GE quote unavailable/above 1000gp cumulative cap id=592 quote=0 deficit=" under a tight gate (one-shot flag, phase==HOLD, sourceItem==ASHES, sourceGoal==1, geStage=="PREPARE", LOGGED_IN, varp273==20, plane 0, zero ashes carried). Design:
   - Bounds: 6-minute total source cap (line ~1024 -> loud HOLD); exactly ONE firemaking attempt (ashesFireAttempts>=1 -> loud HOLD "One firemaking attempt did not produce verified ashes"); 3-minute fire-burnout wait (ashesFireStartedAt>0 -> WAIT_ASHES_FIRE_BURNOUT -> loud HOLD if no ground ashes appear).
   - Observed-state proofs: LIGHT_ASH_FIRE pending proof requires log count decrease AND a live Fire object within 2 tiles of the target (lines 1623-1625); TAKE_ASHES requires inventory ashes increase (line 1623). Nearby ground ashes are taken first before lighting; an existing live fire within 6 is adopted (ADOPT_NEARBY_LIVE_FIRE) rather than lighting a second.
   - Supplies: banks for tinderbox 590 + one normal log 1511 via withdrawDeficit; terminal loud HOLDS if none banked or carried, or if withdraw mode is rejected.
   - Fire field: ASHES_FIRE_FIELD (3201,3268,0) == SHEEP_FIELD -- known outdoor tile already used for shearing.
   - `Rs2Inventory.combine(TINDERBOX,LOGS)` reuses the established combine pattern (lines 491, 531); 60s pending window on the combine.
   - Held-recovery chain order (lines 304-314): onion recoveries keep priority; ashes-quote recovery inserted after them -- correct precedence (dye/wool paths first).

## Observations (not defects)
- O1: `Rs2GroundItem.take(ASHES)` rejection -> loud HOLD ("Nearby ashes id=592 detected but Take was rejected") rather than one retry. Terminal-loud, not a loop -- acceptable; flagged because a transient take miss (player mid-step) escalates to HOLD.
- O2: proactive fallback entry (line ~1159) resets `ashesFallbackRecovered=false` while entering ASHES_LOCAL_BURN. If the flow ever returned to PREPARE with the exact zero-quote error, the one-shot recovery could fire once more. Bounded (entry requires the deficit flow) -- observation only.
- O3: ADOPT_NEARBY_LIVE_FIRE starts the 3-minute burnout window at adoption time for a fire of unknown age; a nearly-burned-out adopted fire costs one cycle into the loud HOLD. Bounded, loud -- acceptable.

## Live acceptance: PENDING
Feed dark since 2026-09-30 17:44 EDT (~19.2h); no confirmed live stream URL. New runtime lines to watch for: RECOVERED_SECOND_ONION_PICK_CAP (one-shot proof), GE_ASHES_UNAVAILABLE_FALLBACK, RECOVERED_EXACT_ASHES_ZERO_QUOTE, PROVED_ASHES_FIRE, ADOPT_NEARBY_LIVE_FIRE, LIGHT_ASH_FIRE_DISPATCH.

-- Muse (read-only reviewer)
