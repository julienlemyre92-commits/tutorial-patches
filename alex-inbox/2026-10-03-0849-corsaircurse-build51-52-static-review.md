# Muse static review: Corsair Curse Build 51 + Build 52 (2026-10-03 ~08:49 EDT)

Scope: read-only static review (Muse is review-only; Alex owns releases). Diffed the
shipped source-review drops: Build 50 -> Build 51 (62 changed lines), Build 51 -> Build 52
(4 lines). Packaging verified from patch-1099.zip (git blobs API, raw).

## Build 51 (patch-1099, landed 08:43:11 EDT)

1. **New `gameplayInputAllowed()` gate** (`!stopped && armed() && ownsInput && !pauseAllScripts && !InputArbiter.isHuman()`).
   Tick now disarms when input is not owned: cancels pending service requests, unregisters
   death recovery, parks at `WAIT_ARMED_OR_INPUT_OWNER`. Clean hot-swap behavior when a
   newer build takes input ownership. The old inline armed/ownsInput/pause/human check at
   ~line 303 is replaced by the helper (no behavior change there).
2. **`prepAttempted` export quiescence**: state now exports `prepared && prepAttempted` —
   an ended preparation request may be replanned by the replacement, never an in-flight
   transaction. `isQuiescent()` also requires `gameplayInputAllowed()`.
3. **Ithoi-room local action selection**: when `f.instanced && ithoiRoom(f)`, progress p==52
   issues `Plan("Ithoi:fight", COMBAT, CORSCURS_NAVIGATOR_COMBAT, f.pos, 0)`; otherwise the
   local `DS2_CORSAIR_COVE_STAIRS_RAMP` stair is queried within 12 tiles (returns null ->
   mainland-return fallback preserved). `ithoiRoom()` is the same predicate as before,
   extracted (plane==1, in-room raw or instanced-template pos).
4. **Survival meals hardened**: `survivalMeals++` now only on the proved `eat:survival`
   completion (was incremented on dispatch); the `LOW_HP_MEAL_BUDGET` 3-attempt terminal
   HOLD is removed — verified meals continue during ordinary HOLD when the quest owns
   input. Only a proved inventory decrement settles a meal.
5. **Boss pre-exit eat cap removed**: `!bossEatAttempted && bossMealsProved<8 && ...` ->
   `!bossEatAttempted && ...`. With proved-decrement eating this just means the script eats
   to recovery (or food exhaustion) instead of forcing stair exit after 8 proved meals
   while still injured. Safer direction; bounded by actual food count.
6. **CONTROL file disarm**: an explicit control file with `enableActions != "true"` (or an
   unreadable file) overrides an older enabled config approval. Conservative, fine.

## Build 52 (patch-1100, landed 08:45:38 EDT)

Restore-side match for change 2: `prepAttempted = prepared && reloadState.get("prepAttempted")`
— a legacy build's exported flag can't resurrect an in-flight transaction after hot-load.
Consistent with the export side. No other changes.

## Findings

1. **(question)** The new `Ithoi:fight` combat Plan targets `f.pos` (the player's own tile)
   with distance 0 while naming `NpcID.CORSCURS_NAVIGATOR_COMBAT`. Confirm the executor
   resolves the NPC by ID and doesn't navigate-to/click the player's own tile.
2. **(packaging watch)** patch-1099.zip root carries `META-INF/MANIFEST.MF` plus
   `META-INF/quest-navigation-guard.properties`. `net/` prefix verified on all class entries
   (root cause of the 341/342 bad-zip incident is NOT present), version.txt=1099 matches.
   Confirm Check-Update.ps1 never lets the zip-root default manifest overwrite the jar's
   real Main-Class manifest on injection.
3. **(carry-forward, provider bundle UNCHANGED)** patch-1099's
   `quest-services-hot/provider-bundle-1.jar` is byte-identical to patch-1098's
   `patches/provider-bundle-43.jar` (SHA256 f3944ea6c442114421d844a09f305c2007f9a6358eecdd014ad6ad6c13c18bc2;
   rename only). All four 08:37 bundle-43 findings are therefore still live in the shipped
   provider: (a) sticky `NavigationMicrobotDriver.failure` wedges all future navigation
   after one transient exception; (b) `interruptionReason` fires `EMERGENCY_HANDOFF_REQUIRED`
   on ANY attacking NPC including the boss while `EmergencyLocalEscape.choose()` returns
   null unless pathfinder config is exactly avoidWilderness+avoidDangerousNpcs+!useBankItems+!membersWorld
   -> terminal `EMERGENCY_HOLD` for navigation issued mid-fight; (c) `stop()` early-returns
   when the previous clearer is still alive, skipping `clearWalkingRoute` for the new reason;
   (d) no timeout on the stop-quiescence proof in `NavigationService.tick` -> livelock if the
   walker can't stop.
4. **(minor)** `bossMealsProved` is now incremented but never compared (still exported to
   properties). Dead gate, harmless.

## Verdict

Static PASS for the script (51+52). Live acceptance pending, judged by fresh runtime lines
only: Build 51/52 hot-load acceptance -> banking behavior -> food restock -> Ithoi
re-engagement -> proved meals -> checkpoint 14+/50. Last live eyes 08:24-08:26 EDT showed
Build 50 parked at Lumbridge Castle steps in designed post-death preflight (12/50
checkpoints, HP 25/25, FOOD 0). Screenshot feed still dark since 2026-09-30 17:44 EDT.
