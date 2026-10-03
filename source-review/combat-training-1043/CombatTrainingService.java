package net.runelite.client.plugins.microbot.questcommon.training;

import java.util.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.questcommon.navigation.NavigationGoal;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceHub;

/**
 * Exclusive, bounded training provider. Call tick from the Microbot client-thread lifecycle.
 * Route movement is delegated to the already-registered guarded NAVIGATION provider; this
 * service never treats a walker return value, NPC click, or gate proximity as proof.
 */
public final class CombatTrainingService implements QuestServiceHub.ServicePlugin {
    public interface Driver {
        Frame observe(CombatTrainingGoal goal);
        /** Resolve again by index/id/name and recheck allowlist, region, reachability and LOS. */
        boolean attack(CombatTrainingGoal goal, Npc npc);
        default String attackFailure() { return "unavailable"; }
        /** Eat exactly one allowlisted item; caller must verify HP and inventory next ticks. */
        boolean eat(CombatTrainingGoal goal, int itemId, int observedHp);
        boolean reachable(WorldPoint point);
        default boolean stopped() { return true; }
    }
    public record Npc(int index, int id, String name, int combatLevel, WorldPoint point,
                      boolean dead, boolean lineOfSight, boolean reachable,
                      boolean interactingWithPlayer) { }
    public record Frame(String accountKey, long observedAt, boolean loggedIn, boolean memberWorld,
        boolean dead, boolean inCombat, WorldPoint position, int world,
        int combatLevel, int hpLevel, int hp, int maxHp,
        long attackXp, long strengthXp, long defenceXp, long rangedXp, long magicXp, long hpXp,
        Map<Integer,Integer> inventory, Set<Integer> equipped,
        String interactingNpc, int interactingNpcIndex, List<Npc> npcs) {
        public Frame {
            inventory = inventory == null ? Map.of() : Map.copyOf(inventory);
            equipped = equipped == null ? Set.of() : Set.copyOf(equipped);
            interactingNpc = interactingNpc == null ? "" : interactingNpc;
            npcs = npcs == null ? List.of() : List.copyOf(npcs);
        }
        public int count(int itemId) { return inventory.getOrDefault(itemId, 0); }
        public int foodCount(CombatTrainingGoal goal) {
            return goal.foodItemPriority().stream().mapToInt(this::count).sum();
        }
        public long combatXp() { return attackXp + strengthXp + defenceXp + rangedXp + magicXp; }
    }

    private enum Phase { ENTRY, TRAIN, EXIT }
    private record Pending(String kind, Frame before, int itemId, int npcIndex, long since) { }

    private final Driver driver;
    private QuestServiceHub.Lease lease;
    private CombatTrainingGoal goal;
    private Phase phase;
    private Pending pending;
    private volatile boolean shutdownRequested;
    private volatile QuestServiceHub.Outcome childOutcome;
    private volatile String childProof = "";
    private volatile boolean childReported;
    private volatile long childReportedAt;
    private int npcActions, noProgressScans, repositionCount;
    private int returnDeferrals;
    private WorldPoint approachDestination;
    private long lastProgressAt;
    private long lastCombatXp = -1, lastHpXp = -1;
    private String terminalReason = "";
    private QuestServiceHub.Outcome exitOutcome = QuestServiceHub.Outcome.COMPLETE;
    private String activeRequestId = "";
    private boolean emergencyEat;

    public CombatTrainingService(Driver driver) { this.driver = Objects.requireNonNull(driver); }
    @Override public QuestServiceHub.Kind kind() { return QuestServiceHub.Kind.TRAINING; }
    public boolean register() { return QuestServiceHub.register(this); }
    public boolean unregister() { return QuestServiceHub.unregister(this); }

    /** Tick only from the provider plugin's client-thread loop. */
    public synchronized void tick() {
        QuestServiceHub.Lease current = QuestServiceHub.activeFor(this);
        if (current == null) return;
        if (!(current.request instanceof CombatTrainingGoal requested)) {
            QuestServiceHub.finish(current, this, QuestServiceHub.Outcome.UNAVAILABLE,
                "Unsupported TRAINING request type");
            clear();
            return;
        }
        if (lease != current) initialize(current, requested);
        Frame frame;
        try { frame = driver.observe(goal); }
        catch (Throwable failure) { hold("Fresh Microbot observation failed: " + failure); return; }
        long now = System.currentTimeMillis();
        String invalid = validateFrame(frame, now);
        if (invalid != null) { hold(invalid); return; }
        if (frame.dead()) { hold("Death observed during training; no automatic death recovery attempted"); return; }
        if (pending != null) {
            if (pending.kind().equals("attack") && frame.hp() <= goal.eatAtOrBelowHp()
                && QuestServiceHub.cancellationReason(lease) == null && !shutdownRequested
                && QuestServiceHub.owns(lease, this, frame.accountKey())) {
                // Low HP may interrupt an unproved attack; it is never replayed.
                pending = null; emergencyEat = true;
                handleUrgentEat(frame, now);
                return;
            }
            // Reconcile the last dispatched action before honoring cancel/reload. This
            // observes only; it never resubmits the action.
            verifyPending(frame, now);
            return;
        }
        if (QuestServiceHub.cancellationReason(lease) != null || shutdownRequested) {
            hold("Training lease cancelled/provider stopping; no further game input issued"); return;
        }
        if (!QuestServiceHub.owns(lease, this, frame.accountKey())) return;

        if (handleUrgentEat(frame, now)) return;
        String threat = unexpectedCombat(frame);
        if (threat != null) {
            phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
            terminalReason = "Unexpected combat/aggressor; guarded retreat requested: " + threat;
        }
        if (phase == Phase.ENTRY && frame.combatLevel() >= goal.targetCombatLevel()
            && frame.hpLevel() >= goal.targetHitpointsLevel()) {
            QuestServiceHub.finish(lease, this, QuestServiceHub.Outcome.COMPLETE,
                "Fresh same-account frame already proves combat=" + frame.combatLevel()
                    + " and HP=" + frame.hpLevel() + " targets; no training actions issued");
            clear(); return;
        }
        if (!goal.trainingRegion().contains(frame.position()) && phase != Phase.ENTRY && phase != Phase.EXIT) {
            hold("Player left the allowlisted training region unexpectedly at " + frame.position()); return;
        }
        if (now >= goal.deadlineMillis() && phase == Phase.ENTRY) {
            unavailable("Training deadline expired before entry; no combat actions issued"); return;
        }
        if (phase == Phase.ENTRY) { enter(frame); return; }
        if (phase == Phase.EXIT) { exit(frame); return; }
        train(frame, now);
    }

    private void initialize(QuestServiceHub.Lease current, CombatTrainingGoal requested) {
        lease = current; goal = requested; phase = Phase.ENTRY; pending = null;
        childOutcome = null; childProof = ""; childReported = false; childReportedAt = 0;
        npcActions = noProgressScans = repositionCount = returnDeferrals = 0;
        approachDestination = null;
        lastProgressAt = System.currentTimeMillis(); lastCombatXp = lastHpXp = -1;
        terminalReason = ""; exitOutcome = QuestServiceHub.Outcome.COMPLETE; emergencyEat = false;
        activeRequestId = requested.requestId();
    }

    private String validateFrame(Frame f, long now) {
        if (f == null || !f.loggedIn() || f.position() == null || f.accountKey() == null
            || !goal.accountKey().equals(f.accountKey()) || f.observedAt() <= 0
            || f.observedAt() > now || now - f.observedAt() > 2500)
            return "No fresh same-account logged-in player frame";
        if (goal.f2pOnly() && f.memberWorld()) return "Training goal is F2P-only but current world is members";
        if (f.combatLevel() < 1 || f.hpLevel() < 10 || f.hp() < 1 || f.maxHp() < 1
            || f.hp() > f.maxHp() + 30 || f.hpXp() < 0 || f.combatXp() < 0)
            return "Invalid combat/HP telemetry; refusing to train";
        return null;
    }

    private void enter(Frame f) {
        // Consume the child result even if its last in-flight step has since reached
        // the exact tile. Otherwise a stale callback can poison the next route.
        if (childReported) {
            route(goal.entryProofTile(), 0, "training entry arrival reconciliation"); return;
        }
        if (goal.trainingRegion().contains(f.position())) {
            if (!goal.entryProofTile().equals(f.position()) && !driver.reachable(goal.entryProofTile())) {
                hold("Training entry tile is not currently reachable; gate/door crossing is unproved"); return;
            }
            if (!goal.trainingRegion().contains(f.position()) || !goal.entryProofTile().equals(f.position())) {
                route(goal.entryProofTile(), 0, "training interior proof tile"); return;
            }
            String prep = preflight(f);
            if (prep != null) {
                phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
                terminalReason = "Training-entry preflight failed; guarded return requested: " + prep;
                return;
            }
            phase = Phase.TRAIN; lastProgressAt = System.currentTimeMillis();
            lastCombatXp = f.combatXp(); lastHpXp = f.hpXp(); return;
        }
        if (!driver.stopped()) { hold("Cannot route to training while another walker/clear worker is active"); return; }
        String prep = preflight(f);
        if (prep != null) {
            if (f.inCombat()) {
                phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
                terminalReason = "Cannot start training while already in combat; guarded return requested";
            } else unavailable(prep);
            return;
        }
        route(goal.entryProofTile(), 0, "training entry; exact interior tile proves gate crossing");
    }

    private String preflight(Frame f) {
        if (f.inCombat()) return "Cannot start or reroute training while already in combat";
        if (f.foodCount(goal) < goal.minimumFoodAtEntry()) return "Insufficient allowlisted food before training";
        if (!f.equipped().containsAll(goal.requiredEquippedItemIds()))
            return "Required equipment is not all visibly equipped";
        return null;
    }

    private boolean handleUrgentEat(Frame f, long now) {
        if (f.hp() > goal.eatAtOrBelowHp()) return false;
        boolean critical = f.hp() <= goal.stopAtOrBelowHp();
        // The reserve is for normal training. During combat or an exit, consume it
        // before leaving an injured character unable to route or defend itself.
        boolean hasPermittedFood = f.foodCount(goal) > 0
            && (critical || f.inCombat() || phase == Phase.EXIT
                || f.foodCount(goal) > goal.foodReserve());
        if (!hasPermittedFood) {
            phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
            terminalReason = "Low HP with no permitted meal beyond reserve; guarded retreat requested";
            return true;
        }
        int food = goal.foodItemPriority().stream().filter(id -> f.count(id) > 0)
            .findFirst().orElse(-1);
        if (food < 0) {
            phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
            terminalReason = "Low HP; food telemetry conflicted, guarded retreat requested";
            return true;
        }
        boolean accepted;
        try { accepted = driver.eat(goal, food, f.hp()); }
        catch (Throwable ignored) { accepted = false; }
        if (!accepted) {
            phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
            terminalReason = "Urgent Eat dispatch was rejected/uncertain; guarded retreat requested";
            return true;
        }
        emergencyEat = emergencyEat || critical || phase != Phase.TRAIN;
        if (critical) exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
        pending = new Pending("eat", f, food, -1, now);
        return true;
    }

    private String unexpectedCombat(Frame f) {
        Npc currentTarget = f.interactingNpcIndex() < 0 ? null : f.npcs().stream()
            .filter(n -> n.index() == f.interactingNpcIndex()).findFirst().orElse(null);
        // An NPC can leave the local cache or be marked dead while the player's
        // interaction pointer still names it. Treat that as unknown, not hostile.
        // A truly foreign target is still caught by its name or a live NPC proof.
        boolean expectedName = goal.allowedNpcNames().isEmpty()
            || goal.allowedNpcNames().stream()
                .anyMatch(name -> name.equalsIgnoreCase(f.interactingNpc()));
        boolean foreignTarget = !f.interactingNpc().isBlank()
            && (!expectedName || currentTarget != null
                && (!allowed(goal, currentTarget.name(), currentTarget.id())
                    || currentTarget.point() == null
                    || !goal.trainingRegion().contains(currentTarget.point())
                    || currentTarget.combatLevel() > goal.maximumTargetCombatLevel()));
        Npc externalAggressor = f.npcs().stream().filter(n -> n.interactingWithPlayer()
            && !allowed(goal, n.name(), n.id())).findFirst().orElse(null);
        if (!foreignTarget && externalAggressor == null) return null;
        return "target=" + f.interactingNpc()
            + (externalAggressor == null ? "" : " aggressor=" + externalAggressor.name()
                + "#" + externalAggressor.id());
    }

    private void train(Frame f, long now) {
        // Auto-retaliation can award XP after the original attack journal has
        // cleared. That is real progress and earns a fresh, still bounded set
        // of approach attempts for the next moving cow.
        if (f.combatXp() > lastCombatXp || f.hpXp() > lastHpXp) {
            lastCombatXp = f.combatXp(); lastHpXp = f.hpXp();
            lastProgressAt = now; repositionCount = noProgressScans = 0;
        }
        if (f.combatLevel() >= goal.targetCombatLevel()
            && f.hpLevel() >= goal.targetHitpointsLevel()) {
            exitOutcome = QuestServiceHub.Outcome.COMPLETE;
            terminalReason = "Combat and HP targets proved in fresh game telemetry";
            phase = Phase.EXIT; return;
        }
        if (now >= goal.deadlineMillis()) {
            phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
            terminalReason = "Training deadline reached; guarded return route requested"; return;
        }
        if (f.combatLevel() >= goal.targetCombatLevel() || f.hpLevel() >= goal.targetHitpointsLevel()) {
            // One requested threshold is met; keep only training toward the other.
        }
        if (f.foodCount(goal) <= goal.foodReserve()) {
            phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
            terminalReason = "Food reserve reached; returning for quest continuation"; return;
        }
        if (npcActions >= goal.maximumNpcActions()) {
            phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
            terminalReason = "NPC action budget exhausted; guarded return requested"; return;
        }
        if (now - lastProgressAt > goal.noProgressMillis()) {
            phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
            terminalReason = "Combat/HP XP stalled; guarded return requested"; return;
        }
        if (f.inCombat()) return; // Wait for current verified target; never issue another attack.

        if (approachDestination != null) {
            route(approachDestination, 0, "bounded approach to an observed training NPC"); return;
        }

        List<Npc> targets = f.npcs().stream().filter(n -> allowed(goal, n.name(), n.id())
            && !n.dead() && n.point() != null && goal.trainingRegion().contains(n.point())
            && f.position().distanceTo(n.point()) <= 6
            && n.combatLevel() <= goal.maximumTargetCombatLevel()
            && !n.interactingWithPlayer())
            .sorted(Comparator.comparingInt(n -> f.position().distanceTo(n.point())))
            .toList();
        List<Npc> candidates = targets.stream().filter(n -> n.lineOfSight()
            && n.reachable() && f.position().distanceTo(n.point()) <= 2).toList();
        if (candidates.isEmpty()) {
            noProgressScans++;
            if (repositionCount < 3 && now - lastProgressAt < goal.noProgressMillis()) {
                for (Npc target : targets) {
                    WorldPoint beside = approachTile(f.position(), target.point());
                    if (beside == null) continue;
                    approachDestination = beside;
                    repositionCount++;
                    route(beside, 0, "bounded approach to " + target.name() + "#" + target.id());
                    return;
                }
            }
            if (noProgressScans >= 3) {
                phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
                terminalReason = "No allowlisted reachable NPC after bounded approach; "
                    + npcSnapshot(f) + "; guarded return requested";
            }
            return;
        }
        noProgressScans = 0;
        Npc target = candidates.get(0);
        boolean accepted;
        try { accepted = driver.attack(goal, target); }
        catch (Throwable failure) { hold("Attack dispatch failed uncertainly: " + failure); return; }
        if (!accepted) {
            phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
            terminalReason = "Attack dispatch rejected without a click ("
                + driver.attackFailure() + "); guarded return requested";
            return;
        }
        npcActions++;
        pending = new Pending("attack", f, -1, target.index(), now);
    }

    private boolean verifyPending(Frame f, long now) {
        Pending action = pending;
        if (action.kind().equals("eat")) {
            if (f.count(action.itemId()) < action.before().count(action.itemId())) {
                boolean hpGainProved = f.hp() > action.before().hp();
                pending = null; lastProgressAt = now;
                boolean stillLow = f.hp() <= goal.eatAtOrBelowHp();
                boolean reserveDepleted = f.foodCount(goal) <= goal.foodReserve();
                if (emergencyEat || stillLow || reserveDepleted) {
                    phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
                    terminalReason = "One low-HP meal consumed (inventory loss proved; HP gain="
                        + hpGainProved + "); retreat requested instead of continuing exposed";
                    emergencyEat = false;
                } else emergencyEat = false;
                return false;
            }
            if (now - action.since() > 8_000)
                hold("Eat outcome not proved by both inventory decrease and HP increase");
            return true;
        }
        if (f.combatXp() > action.before().combatXp() || f.hpXp() > action.before().hpXp()) {
            pending = null; lastProgressAt = now; lastCombatXp = f.combatXp(); lastHpXp = f.hpXp();
            repositionCount = noProgressScans = 0;
            return false;
        }
        boolean selectedTarget = action.npcIndex() == f.interactingNpcIndex()
            && !f.interactingNpc().isBlank();
        if (f.dead() || !goal.trainingRegion().contains(f.position())) {
            hold("Unexpected death/zone exit during pending attack"); return true;
        }
        if (!selectedTarget && now - action.since() > 2_000) {
            // The click did not start a fight. Do not repeat it against an unchanged scene.
            pending = null; noProgressScans++;
            if (noProgressScans >= 2) hold("Two attack attempts failed to start/prove combat");
            return false;
        }
        if (now - action.since() > 20_000) hold("Attack had no verified XP/HP progress within 20 seconds");
        return true;
    }

    private void route(WorldPoint destination, int radius, String why) {
        if (childReported) {
            boolean returning = phase == Phase.EXIT;
            QuestServiceHub.Outcome outcome = childOutcome;
            String proof = childProof;
            if (outcome != QuestServiceHub.Outcome.COMPLETE) {
                childReported = false; childOutcome = null; childProof = "";
                if (returning && outcome == QuestServiceHub.Outcome.UNAVAILABLE
                    && proof.contains("instance/combat route unsupported")) {
                    try {
                        Frame retry = driver.observe(goal);
                        if (retry != null && goal.accountKey().equals(retry.accountKey())
                            && retry.position() != null
                            && goal.trainingRegion().contains(retry.position())) {
                            if (retry.inCombat()) return; // Urgent Eat keeps running.
                            if (returnDeferrals++ < 2) return; // Replan only after combat clears.
                        }
                    } catch (Throwable failure) {
                        hold("Cannot recheck combat after rejected return route: " + failure);
                        return;
                    }
                }
                if (outcome == QuestServiceHub.Outcome.UNAVAILABLE
                    && !returning)
                    unavailable("Navigation child unavailable: " + proof);
                else hold(returning
                    ? "No guarded safe-return route was proved; character may still be exposed: " + proof
                    : "Navigation child did not complete (" + outcome + "): " + proof);
                return;
            }
            Frame arrived;
            try { arrived = driver.observe(goal); }
            catch (Throwable failure) { hold("Cannot verify navigation result: " + failure); return; }
            if (arrived == null || !goal.accountKey().equals(arrived.accountKey())
                || arrived.observedAt() <= 0 || System.currentTimeMillis() - arrived.observedAt() > 2500
                || arrived.position() == null || arrived.position().getPlane() != destination.getPlane()
                || arrived.position().distanceTo(destination) > radius
                || (!arrived.position().equals(destination) && !driver.reachable(destination))) {
                // A walker can finish while its final game movement is still in flight.
                // Wait briefly for fresh arrival evidence without issuing another click.
                if (System.currentTimeMillis() - childReportedAt <= 5_000) return;
                childReported = false; childOutcome = null; childProof = "";
                String detail = "Navigation completion was not corroborated by fresh destination proof: "
                    + proof + "; actual=" + (arrived == null ? "no frame" : arrived.position());
                if (!returning && arrived != null && !arrived.inCombat())
                    unavailable(detail);
                else hold(detail);
                return;
            }
            childReported = false; childOutcome = null; childProof = "";
            if (returning) {
                if (arrived.position().distanceTo(goal.returnTile()) > 2) {
                    hold("Fresh return point did not match the configured quest return tile"); return;
                }
                if (arrived.inCombat() || unexpectedCombat(arrived) != null) {
                    hold("Return tile reached but combat is not proved clear: "
                        + (unexpectedCombat(arrived) == null ? "active combat" : unexpectedCombat(arrived)));
                    return;
                }
                if (exitOutcome == QuestServiceHub.Outcome.COMPLETE
                    && (arrived.combatLevel() < goal.targetCombatLevel()
                        || arrived.hpLevel() < goal.targetHitpointsLevel())) {
                    hold("Requested training thresholds were not present at final return verification"); return;
                }
                String done = terminalReason.isBlank()
                    ? "Bounded training finished: observed combat=" + arrived.combatLevel()
                        + " HP=" + arrived.hpLevel() + " combatXP=" + arrived.combatXp()
                        + " hpXP=" + arrived.hpXp()
                    : terminalReason + "; arrived at return tile; combat=" + arrived.combatLevel()
                        + " HP=" + arrived.hpLevel();
                QuestServiceHub.finish(lease, this, exitOutcome,
                    "Fresh same-account return-tile/reachability proof; " + done);
                clear(); return;
            }
            if (destination.equals(goal.entryProofTile())) {
                if (!goal.trainingRegion().contains(arrived.position())) {
                    hold("Gate/interior crossing not proved inside configured training bounds"); return;
                }
                if (System.currentTimeMillis() >= goal.deadlineMillis()) {
                    phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
                    terminalReason = "Entry route finished after training deadline; safe return requested without attacking";
                    return;
                }
                String prep = preflight(arrived);
                if (prep != null) {
                    phase = Phase.EXIT; exitOutcome = QuestServiceHub.Outcome.UNAVAILABLE;
                    terminalReason = "Entry route proved but preflight failed; guarded return requested: " + prep;
                    return;
                }
                phase = Phase.TRAIN; lastProgressAt = System.currentTimeMillis();
                lastCombatXp = arrived.combatXp(); lastHpXp = arrived.hpXp(); return;
            }
            if (phase == Phase.TRAIN && destination.equals(approachDestination)) {
                if (!goal.trainingRegion().contains(arrived.position())) {
                    hold("NPC approach left the allowlisted training region"); return;
                }
                approachDestination = null; noProgressScans = 0;
                return;
            }
            hold("Navigation destination did not match the active entry/return goal"); return;
        }
        if (childOutcome != null) return; // callback in flight; never double-submit.
        NavigationGoal child = new NavigationGoal(goal.accountKey(), goal.pid(),
            "combat training: " + why, activeRequestId
                + (phase == Phase.EXIT ? ":return" : phase == Phase.ENTRY ? ":entry" : ":rescan"),
            destination, radius, goal.f2pOnly(), 0, 0, false, goal.forbiddenRegions(),
            goal.maximumRouteReplans(), Math.max(goal.deadlineMillis(),
                System.currentTimeMillis() + goal.returnRouteGraceMillis()));
        try {
            QuestServiceHub.Lease delegated = QuestServiceHub.delegate(lease, this, child);
            if (delegated == null) {
                if (phase == Phase.EXIT)
                    hold("Guarded NAVIGATION provider unavailable during return; no escape proof: " + why);
                else unavailable("Guarded NAVIGATION provider unavailable for " + why);
            }
            else childOutcome = null;
        } catch (Throwable failure) { hold("Unable to request guarded navigation: " + failure); }
    }

    private void exit(Frame f) {
        if (f.dead()) { hold("Death observed before return route; no automatic recovery attempted"); return; }
        // NAVIGATION correctly refuses a new route while combat is active. Keep
        // this provider alive so urgent Eat continues to run on every tick.
        if (f.inCombat()) return;
        if (!driver.stopped()) { hold("Walker/clear worker not stopped before exit routing"); return; }
        route(goal.returnTile(), 2, "return to quest owner");
    }

    private static boolean allowed(CombatTrainingGoal g, String name, int id) {
        boolean idMatch = g.allowedNpcIds().isEmpty() || g.allowedNpcIds().contains(id);
        boolean nameMatch = g.allowedNpcNames().isEmpty() || g.allowedNpcNames().stream()
            .anyMatch(expected -> expected.equalsIgnoreCase(name == null ? "" : name));
        return idMatch && nameMatch;
    }

    private String npcSnapshot(Frame f) {
        StringBuilder text = new StringBuilder("nearby NPCs=");
        int shown = 0;
        for (Npc npc : f.npcs()) {
            if (npc.point() == null || f.position().distanceTo(npc.point()) > 15) continue;
            if (shown++ == 8) { text.append(" ..."); break; }
            text.append('[').append(npc.name()).append('#').append(npc.id())
                .append('@').append(npc.point()).append(" level=").append(npc.combatLevel())
                .append(" allowed=").append(allowed(goal, npc.name(), npc.id()))
                .append(" region=").append(goal.trainingRegion().contains(npc.point()))
                .append(" los=").append(npc.lineOfSight())
                .append(" reachable=").append(npc.reachable()).append(']');
        }
        return shown == 0 ? "nearby NPCs=none" : text.toString();
    }

    private WorldPoint approachTile(WorldPoint player, WorldPoint npc) {
        if (player == null || npc == null || player.getPlane() != npc.getPlane()) return null;
        List<WorldPoint> neighbors = new ArrayList<>();
        for (int[] delta : new int[][] {{0,1},{1,0},{-1,0},{0,-1},
                                        {1,1},{-1,1},{1,-1},{-1,-1}}) {
            WorldPoint tile = new WorldPoint(npc.getX() + delta[0],
                npc.getY() + delta[1], npc.getPlane());
            if (!tile.equals(player) && goal.trainingRegion().contains(tile)) neighbors.add(tile);
        }
        neighbors.sort(Comparator.comparingInt(player::distanceTo));
        for (WorldPoint tile : neighbors) if (driver.reachable(tile)) return tile;
        return null;
    }

    @Override public void childFinished(QuestServiceHub.Kind child,
        QuestServiceHub.Outcome outcome, String proof) {
        if (child != QuestServiceHub.Kind.NAVIGATION) return;
        childProof = proof == null ? "" : proof;
        childOutcome = outcome;
        childReported = true;
        childReportedAt = System.currentTimeMillis();
    }

    private void unavailable(String reason) {
        terminalReason = reason;
        if (lease != null) QuestServiceHub.finish(lease, this, QuestServiceHub.Outcome.UNAVAILABLE,
            reason == null || reason.isBlank() ? "Training prerequisites unavailable" : reason);
        clear();
    }
    private void hold(String reason) {
        terminalReason = reason;
        if (lease != null) QuestServiceHub.finish(lease, this, QuestServiceHub.Outcome.HOLD,
            reason == null || reason.isBlank() ? "Training state unresolved; input remains held" : reason);
        clear();
    }
    private void clear() {
        lease = null; goal = null; pending = null; phase = null;
        approachDestination = null;
        childOutcome = null; childProof = ""; childReported = false; childReportedAt = 0;
    }
    public void stop() { shutdownRequested = true; }
    public synchronized String status() {
        return "lease=" + (lease == null ? "none" : activeRequestId)
            + " phase=" + phase + " actions=" + npcActions + "/"
            + (goal == null ? 0 : goal.maximumNpcActions()) + " pending="
            + (pending == null ? "none" : pending.kind()) + " reason=" + terminalReason;
    }
}
