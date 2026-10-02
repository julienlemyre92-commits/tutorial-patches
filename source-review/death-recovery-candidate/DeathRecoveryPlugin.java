package net.runelite.client.plugins.microbot.questcommon.deathplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questcommon.QuestDeathRecovery;
import net.runelite.client.plugins.microbot.util.death.DeathsOfficeLocation;
import net.runelite.client.plugins.microbot.util.death.Rs2Death;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;

/** Candidate host. Requires a cooperating quest owner; never clicks without its lease. */
@PluginDescriptor(name = "Quest Death Recovery (candidate)",
    description = "Cooperative, proof-based recovery after a confirmed death",
    tags = {"microbot", "quest"}, enabledByDefault = true)
public final class DeathRecoveryPlugin extends Plugin {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DeathRecoveryPlugin.class);
    private static final WorldPoint LUMBRIDGE_RESPAWN = new WorldPoint(3222, 3217, 0);
    private enum Stage { WATCH, YIELD, CHOOSE, GRAVE, OFFICE_ROUTE, OFFICE_ENTER,
        OFFICE_OPEN, OFFICE_QUOTE, OFFICE_RECLAIM, OFFICE_VERIFY, EXIT, LOGOUT, TERMINAL }

    private static final class Frame {
        final long at;
        final boolean loggedIn;
        final WorldPoint pos;
        final int hp, freeSlots;
        final Map<Integer, Integer> items;
        Frame(long at, boolean loggedIn, WorldPoint pos, int hp, int freeSlots,
              Map<Integer, Integer> items) {
            this.at = at; this.loggedIn = loggedIn; this.pos = pos; this.hp = hp;
            this.freeSlots = freeSlots; this.items = items;
        }
        int count(int id) { return items.getOrDefault(id, 0); }
    }

    private static final class Route {
        final WorldPoint target;
        final long started = System.currentTimeMillis();
        volatile Thread worker;
        volatile WalkerState result;
        volatile Throwable error;
        Route(WorldPoint target) { this.target = target; }
    }

    private ScheduledExecutorService scheduler;
    private volatile boolean enabled, localDeathEvent;
    private boolean unownedReported;
    private volatile WorldPoint eventDeathPoint;
    private volatile Route route;
    private Stage stage = Stage.WATCH;
    private DeathRecoveryBridge.Registration registration;
    private QuestDeathRecovery grave;
    private Frame lastHealthy;
    private long stageAt, deathAt;
    private int attempts, routes, logoutAttempts, reclaimAttempts;
    private String failure = "";

    @Override protected void startUp() {
        enabled = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "QuestDeathRecovery-candidate");
            t.setDaemon(true); return t;
        });
        scheduler.scheduleWithFixedDelay(this::safeTick, 1, 1, TimeUnit.SECONDS);
        LOG.info("[QuestDeathRecovery] CANDIDATE_READY pid={}", ProcessHandle.current().pid());
    }

    @Override protected void shutDown() {
        enabled = false;
        if (scheduler != null) scheduler.shutdownNow();
        Route r = route;
        if (r != null && r.worker != null) r.worker.interrupt();
        if (grave != null && !grave.cancelAndQuiesce(3000))
            LOG.error("[QuestDeathRecovery] Grave worker not quiescent; quest must remain yielded");
        if (r != null && r.worker != null && r.worker.isAlive())
            LOG.error("[QuestDeathRecovery] Route worker not quiescent; quest must remain yielded");
        // An active lease is deliberately retained on shutdown; disabling recovery
        // must never silently resume a quest in the middle of a recovery action.
    }

    @Subscribe public void onActorDeath(ActorDeath event) {
        Client client = Microbot.getClient();
        if (client == null || client.getLocalPlayer() == null
            || event.getActor() != client.getLocalPlayer()) return;
        eventDeathPoint = client.getLocalPlayer().getWorldLocation();
        deathAt = System.currentTimeMillis();
        localDeathEvent = true;
        unownedReported = false;
        LOG.warn("[QuestDeathRecovery] LOCAL_DEATH at={}", eventDeathPoint);
    }

    private void safeTick() {
        if (!enabled) return;
        try { tick(); }
        catch (Throwable ex) {
            LOG.error("[QuestDeathRecovery] tick failed", ex);
            if (registration != null) fail("ENGINE_EXCEPTION=" + ex);
        }
    }

    private void tick() {
        Frame f = capture();
        if (stage == Stage.WATCH) {
            if (f.loggedIn && f.hp > 0 && !localDeathEvent) lastHealthy = f;
            if (!localDeathEvent || !f.loggedIn || f.hp <= 0) return;
            if (f.at - deathAt < 2000) return;
            WorldPoint death = eventDeathPoint;
            if (death == null || f.pos == null) return;
            // Avoid reacting to the death animation before a proved respawn.
            if (near(f.pos, death, 5) && !near(f.pos, LUMBRIDGE_RESPAWN, 16)) return;
            registration = DeathRecoveryBridge.registration();
            if (registration == null) {
                if (!unownedReported) {
                    LOG.error("[QuestDeathRecovery] Death confirmed, no registered quest owner; no input taken");
                    unownedReported = true;
                }
                return;
            }
            LOG.warn("[QuestDeathRecovery] PRE_POST_ITEMS before={} after={}",
                lastHealthy == null ? "unavailable" : lastHealthy.items, f.items);
            try {
                if (!DeathRecoveryBridge.requestYield(registration)) return;
            } catch (RuntimeException ex) {
                fail("OWNER_YIELD_EXCEPTION=" + ex); return;
            }
            transition(Stage.YIELD);
            LOG.warn("[QuestDeathRecovery] YIELD_REQUESTED death={} respawn={}", death, f.pos);
            return;
        }
        if (stage == Stage.TERMINAL) return;
        if (registration == null || DeathRecoveryBridge.registration() != registration
            || !DeathRecoveryBridge.mustYield()) {
            fail("INPUT_LEASE_LOST"); return;
        }
        if (stage == Stage.YIELD) {
            if (!registration.owner.isQuiescent()) {
                if (f.at - stageAt > 15000) fail("QUEST_DID_NOT_QUIESCE");
                return;
            }
            transition(Stage.CHOOSE); return;
        }
        if (!registration.owner.isQuiescent()) { fail("QUEST_RESUMED_DURING_RECOVERY"); return; }
        if (!f.loggedIn) {
            if (stage == Stage.LOGOUT) finish(DeathRecoveryBridge.Outcome.STOPPED_SAFE,
                failure + "; LOGOUT_PROVED", false);
            return;
        }
        if (f.hp <= 0) { fail("DIED_DURING_RECOVERY"); return; }
        switch (stage) {
            case CHOOSE:
                reclaimAttempts = 0;
                if (possessed(f)) {
                    finish(DeathRecoveryBridge.Outcome.NOTHING_TO_RECLAIM,
                        "Critical items retained; quest must assess consumables", true);
                    return;
                }
                WorldPoint death = Rs2Death.getLastDeathLocation();
                if (death == null || eventDeathPoint == null
                    || !near(death, eventDeathPoint, 5)) death = null;
                if (death != null && Rs2Death.hasGrave() && !Rs2Death.hasGraveExpired()
                    && registration.policy.safeGrave.test(death)) {
                    grave = new QuestDeathRecovery(() -> enabled && DeathRecoveryBridge.mustYield()
                        && registration.owner.isQuiescent());
                    grave.begin(new QuestDeathRecovery.Plan(
                        registration.policy.criticalPossession,
                        registration.policy.safeExit, registration.policy.feeCap,
                        1, 1, 3, 4, 30000));
                    transition(Stage.GRAVE);
                } else transition(Stage.OFFICE_ROUTE);
                return;
            case GRAVE:
                QuestDeathRecovery.Result result = grave.tick();
                if (result.outcome == QuestDeathRecovery.Outcome.COMPLETE) {
                    if (possessed(capture())) finish(DeathRecoveryBridge.Outcome.RECOVERED,
                        "Grave recovery and exit proved", true);
                    else fail("GRAVE_RETURNED_COMPLETE_WITH_MISSING_ITEMS");
                } else if (result.outcome == QuestDeathRecovery.Outcome.HOLD
                    || result.outcome == QuestDeathRecovery.Outcome.CANCELLED)
                    fail("GRAVE_RECOVERY=" + result);
                return;
            case OFFICE_ROUTE:
                if (Rs2Death.isInDeathsOffice()) { stopRoute(); transition(Stage.OFFICE_OPEN); return; }
                WorldPoint entrance = DeathsOfficeLocation.LUMBRIDGE.getEntrance();
                if (entrance == null) { fail("NO_F2P_OFFICE_ENTRANCE"); return; }
                if (routeTo(f, entrance, 6)) transition(Stage.OFFICE_ENTER);
                return;
            case OFFICE_ENTER:
                if (Rs2Death.isInDeathsOffice()) { transition(Stage.OFFICE_OPEN); return; }
                if (attempts >= 2) { fail("OFFICE_ENTRY_UNPROVED"); return; }
                if (f.at - stageAt < 3000 && attempts > 0) return;
                attempts++;
                Rs2Death.enterDeathsOffice(); // return value is not proof
                return;
            case OFFICE_OPEN:
                if (Rs2Death.isDeathsOfficeOpen()) { transition(Stage.OFFICE_QUOTE); return; }
                if (attempts >= 2) { fail("OFFICE_INTERFACE_UNPROVED"); return; }
                if (f.at - stageAt < 3000 && attempts > 0) return;
                attempts++;
                Rs2Death.openDeathsOffice();
                return;
            case OFFICE_QUOTE:
                if (!Rs2Death.isDeathsOfficeOpen()) { fail("OFFICE_INTERFACE_CLOSED"); return; }
                if (f.freeSlots < registration.policy.criticalPossession.size()) {
                    fail("INSUFFICIENT_INVENTORY_CAPACITY"); return;
                }
                OptionalInt quote = registration.owner.officeFeeQuote();
                if (quote.isEmpty()) {
                    if (f.at - stageAt > 15000) fail("OFFICE_FEE_UNPROVED");
                    return;
                }
                if (quote.getAsInt() < 0 || quote.getAsInt() > registration.policy.feeCap) {
                    fail("OFFICE_FEE_OUTSIDE_CAP=" + quote.getAsInt()); return;
                }
                transition(Stage.OFFICE_RECLAIM); return;
            case OFFICE_RECLAIM:
                if (possessed(f)) { transition(Stage.EXIT); return; }
                if (!Rs2Death.isDeathsOfficeOpen()) { fail("OFFICE_CLOSED_BEFORE_RECLAIM"); return; }
                if (reclaimAttempts >= 2) { fail("RECLAIM_UNPROVED_AFTER_TWO_ATTEMPTS"); return; }
                OptionalInt currentQuote = registration.owner.officeFeeQuote();
                if (currentQuote.isEmpty() || currentQuote.getAsInt() < 0
                    || currentQuote.getAsInt() > registration.policy.feeCap) {
                    fail("OFFICE_FEE_CHANGED_OR_UNKNOWN"); return;
                }
                reclaimAttempts++;
                Rs2Death.reclaimAll(); // only inventory/equipment delta proves success
                transition(Stage.OFFICE_VERIFY);
                return;
            case OFFICE_VERIFY:
                if (possessed(f)) { Rs2Death.closeInterfaces(); transition(Stage.EXIT); return; }
                if (f.at - stageAt > 5000) transition(Stage.OFFICE_RECLAIM);
                return;
            case EXIT:
                if (near(f.pos, registration.policy.safeExit, 4)) {
                    finish(DeathRecoveryBridge.Outcome.RECOVERED,
                        "Critical inventory/equipment and safe exit proved", true);
                    return;
                }
                if (routeTo(f, registration.policy.safeExit, 4))
                    finish(DeathRecoveryBridge.Outcome.RECOVERED,
                        "Critical inventory/equipment and safe exit proved", true);
                return;
            case LOGOUT:
                if (logoutAttempts >= 2) {
                    finish(DeathRecoveryBridge.Outcome.STOPPED_UNSAFE,
                        failure + "; LOGOUT_UNPROVED", false);
                    return;
                }
                if (f.at - stageAt < 3000 && logoutAttempts > 0) return;
                logoutAttempts++;
                Rs2Player.logout();
                return;
            default: return;
        }
    }

    private boolean routeTo(Frame f, WorldPoint target, int radius) {
        if (near(f.pos, target, radius)) {
            if (!stopRoute()) fail("ROUTE_WORKER_NOT_QUIESCENT_AT_TARGET");
            else return true;
            return false;
        }
        Route current = route;
        if (current != null) {
            if (current.worker != null && current.worker.isAlive()) {
                if (f.at - current.started > 30000) {
                    current.worker.interrupt();
                    fail("ROUTE_TIMEOUT=" + target);
                }
                return false;
            }
            route = null;
            Rs2Walker.clearWalkingRoute("death recovery route ended");
            if (current.error != null) LOG.warn("[QuestDeathRecovery] route error {}", current.error.toString());
        }
        if (++routes > 2) { fail("ROUTE_UNPROVED=" + target); return false; }
        Route next = new Route(target);
        route = next;
        Thread t = new Thread(() -> {
            try {
                next.result = Rs2Walker.walkWithStateUntil(target, radius,
                    () -> !enabled || Thread.currentThread().isInterrupted()
                        || !DeathRecoveryBridge.mustYield());
            } catch (Throwable ex) { next.error = ex; }
        }, "QuestDeathRecovery-route");
        next.worker = t; t.setDaemon(true); t.start();
        return false;
    }

    private void fail(String reason) {
        if (stage == Stage.TERMINAL || stage == Stage.LOGOUT) return;
        failure = reason;
        LOG.error("[QuestDeathRecovery] {}", reason);
        if (!stopRoute()) {
            finish(DeathRecoveryBridge.Outcome.STOPPED_UNSAFE,
                reason + "; ROUTE_WORKER_NOT_QUIESCENT", false);
            return;
        }
        if (grave != null && !grave.cancelAndQuiesce(3000)) {
            finish(DeathRecoveryBridge.Outcome.STOPPED_UNSAFE,
                reason + "; GRAVE_WORKER_NOT_QUIESCENT", false);
            return;
        }
        Frame f = capture();
        if (!f.loggedIn || Rs2Death.isInDeathsOffice()
            || near(f.pos, LUMBRIDGE_RESPAWN, 20)
            || registration != null && near(f.pos, registration.policy.safeExit, 4)) {
            finish(DeathRecoveryBridge.Outcome.STOPPED_SAFE, reason, false);
        } else transition(Stage.LOGOUT);
    }

    private boolean stopRoute() {
        Route r = route;
        if (r == null) return true;
        Thread worker = r.worker;
        if (worker != null && worker.isAlive()) {
            worker.interrupt();
            if (worker != Thread.currentThread()) {
                try { worker.join(3000); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); return false; }
            }
        }
        if (worker != null && worker.isAlive()) return false;
        route = null;
        Rs2Walker.clearWalkingRoute("death recovery route stopped");
        return true;
    }
    private void finish(DeathRecoveryBridge.Outcome outcome, String detail, boolean release) {
        if (stage == Stage.TERMINAL) return;
        if (!stopRoute()) {
            outcome = DeathRecoveryBridge.Outcome.STOPPED_UNSAFE;
            detail += "; ROUTE_WORKER_NOT_QUIESCENT";
            release = false;
        }
        transition(release ? Stage.WATCH : Stage.TERMINAL);
        LOG.warn("[QuestDeathRecovery] RESULT={} {}", outcome, detail);
        if (registration != null) DeathRecoveryBridge.finish(registration, outcome, detail, release);
        localDeathEvent = false;
        if (release) { registration = null; grave = null; lastHealthy = null; }
    }
    private void transition(Stage next) {
        stage = next; stageAt = System.currentTimeMillis(); attempts = 0; routes = 0;
    }
    private boolean possessed(Frame f) {
        for (Map.Entry<Integer, Integer> e : registration.policy.criticalPossession.entrySet())
            if (f.count(e.getKey()) < e.getValue()) return false;
        return true;
    }
    private static boolean near(WorldPoint a, WorldPoint b, int radius) {
        return a != null && b != null && a.getPlane() == b.getPlane()
            && Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())) <= radius;
    }
    private static Frame capture() {
        return Microbot.getClientThread().invoke((Supplier<Frame>) () -> {
            Client c = Microbot.getClient();
            long at = System.currentTimeMillis();
            if (c == null || c.getGameState() != GameState.LOGGED_IN || c.getLocalPlayer() == null)
                return new Frame(at, false, null, 0, 0, Map.of());
            ItemContainer inv = c.getItemContainer(InventoryID.INVENTORY);
            ItemContainer eq = c.getItemContainer(InventoryID.EQUIPMENT);
            if (inv == null || eq == null)
                return new Frame(at, false, null, 0, 0, Map.of());
            Map<Integer, Integer> items = new HashMap<>();
            int free = 0;
            for (Item item : inv.getItems()) {
                if (item == null || item.getId() <= 0) free++;
                else if (item.getQuantity() > 0) items.merge(item.getId(), item.getQuantity(), Integer::sum);
            }
            for (Item item : eq.getItems())
                if (item != null && item.getId() > 0 && item.getQuantity() > 0)
                    items.merge(item.getId(), item.getQuantity(), Integer::sum);
            return new Frame(at, true, c.getLocalPlayer().getWorldLocation(),
                c.getBoostedSkillLevel(Skill.HITPOINTS), free, items);
        });
    }
}
