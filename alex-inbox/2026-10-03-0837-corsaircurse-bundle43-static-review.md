# Static review: Corsair Curse provider-bundle-43 / patch-1098 ("Bank route chooses policy-verified F2P walk")

2026-10-03 ~08:37 EDT. Read-only review of source-review/recovery-1098/
(DefaultProviderBundleFactory, EmergencyHandoff, EmergencyLocalEscape,
NavigationMicrobotDriver, NavigationService) + patch-1098.provider-hot.json.
Alex/Mira own releases; shipping nothing.

Verified-good (evidence, not trust):
- Driver's installed-walker API claims all exist in the shipped microbot-base.jar
  (javap-checked 2026-10-03): walkWithStateUntil(WorldPoint,int,BooleanSupplier),
  canReach(WorldPoint), clearWalkingRoute(String), disableTeleports field.
  Compiles against this jar.
- Emergency escape cancellation is bounded: clearer joins the blocking walker
  with a 3000ms timeout and the walker's completion predicate honors the stop
  flag, so interrupt() does not have to kill a blocking walkWithStateUntil.
- Frame-freshness gate in NavigationService (observedAt within 3s, same account
  key) is correct; emergency handoff requires fully quiescent ordinary workers
  first. Arrival proof never trusts the walker's return value.

Concrete defects / risks (actionable):
1. PERMANENT NAVIGATION WEDGE — NavigationMicrobotDriver.failure is a sticky
   volatile String that is never cleared. Any transient worker/validator/clearer
   exception (even an interrupt during a routine stop) makes unsupportedReason()
   return "Prior walker failure: ..." for every future goal, forever. start()
   then throws, the service HOLDs, and nothing in the tree resets it — the only
   recovery is a client restart. Suggest: clear failure on a successful fresh
   start(), or scope it per-goal.
2. Boss-fight tripwire — NavigationMicrobotDriver.interruptionReason() returns
   EMERGENCY_HANDOFF_REQUIRED for ANY NPC with combatLevel>0 whose
   getInteracting()==player. During the Ithoi boss fight the boss itself counts,
   so every NavigationGoal issued while engaged flees instead of proceeding.
   Worse, EmergencyLocalEscape.choose() returns null (HOLD) unless the pathfinder
   config is avoidWilderness && avoidDangerousNpcs && !useBankItems &&
   !membersWorld — if the live config flags a members world (bots were on
   308/498), choose() can NEVER pick a target and every boss-fight navigation
   ends in terminal EMERGENCY_HOLD. If the boss script ever navigates mid-fight,
   this wedges the quest. At minimum the attacker filter should exclude the
   script's known combat target.
3. Clearing-thread early return — NavigationMicrobotDriver.stop(): if a previous
   clearer thread is still joining, the call returns before spawning a new
   clearer, so the new reason's clearWalkingRoute() never runs. Combined with
   (1), repeated stops during a stuck clear degrade to interrupts-only with no
   route clear.
4. Quiescence livelock — NavigationService.tick(): if walkerStopped() never
   proves (walker ignoring interrupt+predicate), the stopping branch waits
   forever and every goal terminal-HOLDs. No timeout on the stop proof.

Minor: walker's "ARRIVED return deliberately ignored" comment matches the
observed-state architecture; approvalDiagnostic() swallows the reflective
failure correctly.

Pending: live acceptance of bundle-43 hot-load (see below).
