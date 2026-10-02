package net.runelite.client.plugins.microbot.questcommon;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.death.Rs2Death;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

/**
 * One quest script owns this helper and calls tick from its existing scheduler.
 * The owner must return from its tick for every result except IDLE, and must
 * not release input ownership until cancelAndQuiesce returns true. There is no
 * background click worker; the only worker is a cancellable walker route.
 */
public final class QuestDeathRecovery {
    public enum Stage {
        IDLE, TO_GRAVE, OPEN, WAIT_OPEN, FREE, WAIT_FREE,
        PAID, WAIT_PAID, CLOSE, WAIT_CLOSE, EXIT, DONE, HOLD,
        CANCELLING, CANCELLED
    }
    public enum Outcome { IDLE, PENDING, COMPLETE, HOLD, CANCELLED }

    public static final class Result {
        public final Outcome outcome;
        public final Stage stage;
        public final String detail;
        private Result(Outcome outcome, Stage stage, String detail) {
            this.outcome = outcome; this.stage = stage; this.detail = detail;
        }
        public boolean ownsTick() { return outcome != Outcome.IDLE; }
        @Override public String toString() { return outcome + ":" + stage + ":" + detail; }
    }

    /**
     * expectedPossession is a complete, caller-reviewed manifest of item IDs
     * and minimum quantities that must be present after recovery, including
     * equipment. It must exclude items intentionally consumed before death.
     */
    public static final class Plan {
        public final Map<Integer, Integer> expectedPossession;
        public final WorldPoint safeExit;
        public final int feeCap, minEmptySlots, minHp, graveRadius, exitRadius;
        public final long routeTimeoutMs;

        public Plan(Map<Integer, Integer> expectedPossession, WorldPoint safeExit,
                    int feeCap, int minEmptySlots, int minHp,
                    int graveRadius, int exitRadius, long routeTimeoutMs) {
            Objects.requireNonNull(expectedPossession, "expectedPossession");
            if (expectedPossession.isEmpty() || expectedPossession.entrySet().stream()
                    .anyMatch(e -> e.getKey() <= 0 || e.getValue() <= 0))
                throw new IllegalArgumentException("Explicit nonempty recovery manifest required");
            if (safeExit == null || feeCap < 0 || minEmptySlots < 1 || minHp < 1
                || graveRadius < 1 || exitRadius < 0 || routeTimeoutMs < 10000)
                throw new IllegalArgumentException("Invalid recovery safety policy");
            this.expectedPossession = Collections.unmodifiableMap(new HashMap<>(expectedPossession));
            this.safeExit = safeExit;
            this.feeCap = feeCap; this.minEmptySlots = minEmptySlots;
            this.minHp = minHp; this.graveRadius = graveRadius;
            this.exitRadius = exitRadius; this.routeTimeoutMs = routeTimeoutMs;
        }
    }

    /** Raw client-thread item containers. Never infer recovery from an API return. */
    public static final class Frame {
        public final long at;
        public final boolean loggedIn;
        public final WorldPoint position;
        public final int hp, emptySlots;
        public final Map<Integer, Integer> inventory, equipment;
        private Frame(long at, boolean loggedIn, WorldPoint position, int hp,
                      int emptySlots, Map<Integer, Integer> inventory,
                      Map<Integer, Integer> equipment) {
            this.at = at; this.loggedIn = loggedIn; this.position = position;
            this.hp = hp; this.emptySlots = emptySlots;
            this.inventory = Collections.unmodifiableMap(inventory);
            this.equipment = Collections.unmodifiableMap(equipment);
        }
        public int possessed(int id) {
            return inventory.getOrDefault(id, 0) + equipment.getOrDefault(id, 0);
        }
        public static Frame capture() {
            return Microbot.getClientThread().invoke((Supplier<Frame>) () -> {
                Client c = Microbot.getClient();
                long now = System.currentTimeMillis();
                if (c == null || c.getGameState() != GameState.LOGGED_IN
                    || c.getLocalPlayer() == null)
                    return new Frame(now, false, null, 0, 0, Map.of(), Map.of());
                ItemContainer inv = c.getItemContainer(InventoryID.INVENTORY);
                ItemContainer equip = c.getItemContainer(InventoryID.EQUIPMENT);
                if (inv == null || equip == null)
                    return new Frame(now, false, null, 0, 0, Map.of(), Map.of());
                int free = 0;
                for (Item item : inv.getItems()) if (item == null || item.getId() <= 0) free++;
                return new Frame(now, true, c.getLocalPlayer().getWorldLocation(),
                    c.getBoostedSkillLevel(Skill.HITPOINTS), free,
                    count(inv), count(equip));
            });
        }
        private static Map<Integer, Integer> count(ItemContainer container) {
            Map<Integer, Integer> found = new HashMap<>();
            for (Item item : container.getItems())
                if (item != null && item.getId() > 0 && item.getQuantity() > 0)
                    found.merge(item.getId(), item.getQuantity(), Integer::sum);
            return found;
        }
    }

    private static final class Route {
        final WorldPoint target;
        final int radius;
        final long startedAt;
        volatile Thread worker;
        volatile WalkerState result;
        volatile Throwable error;
        Route(WorldPoint target, int radius) {
            this.target = target; this.radius = radius;
            this.startedAt = System.currentTimeMillis();
        }
    }

    private final BooleanSupplier ownsInput; // must be fast, thread-safe, and free of client calls
    private volatile boolean cancelled, inAction;
    private volatile Route route;
    private Plan plan;
    private Stage stage = Stage.IDLE;
    private String detail = "";
    private long stageAt;
    private int openAttempts, freeAttempts, paidAttempts, closeAttempts,
        graveRoutes, exitRoutes;
    private boolean holdAfterExit, holdAfterRouteStop;

    public QuestDeathRecovery(BooleanSupplier ownsInput) {
        this.ownsInput = Objects.requireNonNull(ownsInput, "ownsInput");
    }

    /** Call after a confirmed death with a manifest from the pre-death frame. */
    public synchronized void begin(Plan next) {
        if (stage != Stage.IDLE && stage != Stage.DONE && stage != Stage.CANCELLED)
            throw new IllegalStateException("Prior recovery still owns input: " + stage);
        if (route != null && route.worker != null && route.worker.isAlive())
            throw new IllegalStateException("Prior recovery route is alive");
        plan = Objects.requireNonNull(next, "plan");
        cancelled = false; detail = ""; holdAfterExit = false;
        holdAfterRouteStop = false;
        openAttempts = freeAttempts = paidAttempts = closeAttempts = 0;
        graveRoutes = exitRoutes = 0;
        setStage(Stage.TO_GRAVE);
    }

    public synchronized Result tick() { return tick(Frame.capture()); }

    /** Pass the owner's existing fresh client-thread frame when available. */
    public synchronized Result tick(Frame f) {
        if (stage == Stage.IDLE) return result(Outcome.IDLE);
        if (cancelled || !owner()) return cancelTick();
        if (stage == Stage.CANCELLED) return result(Outcome.CANCELLED);
        if (stage == Stage.DONE) return result(Outcome.COMPLETE);
        if (stage == Stage.HOLD) return result(Outcome.HOLD);
        if (f == null || !f.loggedIn || f.position == null) {
            if (route != null) return holdRoute("DISCONNECTED_DURING_RECOVERY_ROUTE");
            return pending("WAIT_LOGGED_IN_AND_RAW_ITEMS");
        }
        if (f.hp <= 0) return holdRoute("DIED_AGAIN_DURING_RECOVERY");
        if (f.hp < plan.minHp && stage != Stage.EXIT) {
            failToSafety("HP_BELOW_RECOVERY_FLOOR=" + f.hp);
            return pending(detail);
        }
        if (recovered(f) && stage != Stage.CLOSE && stage != Stage.WAIT_CLOSE
            && stage != Stage.EXIT)
            setStage(Rs2Death.isGraveOpen() ? Stage.CLOSE : Stage.EXIT);
        if (route != null && stage != Stage.TO_GRAVE && stage != Stage.EXIT) {
            if (route.worker != null && route.worker.isAlive()) {
                route.worker.interrupt();
                return pending("WAIT_ROUTE_QUIESCE_BEFORE_GRAVE_ACTION");
            }
            route = null;
            Rs2Walker.clearWalkingRoute("quest death route complete before grave action");
        }
        switch (stage) {
            case TO_GRAVE:
                WorldPoint death = Rs2Death.getLastDeathLocation();
                if (death == null) return hold("NO_DEATH_LOCATION; manifest=" + missing(f));
                if (near(f.position, death, plan.graveRadius)) {
                    setStage(Stage.OPEN); return pending("AT_GRAVE_AREA");
                }
                return walk(f, death, plan.graveRadius, false);
            case OPEN:
                if (!capacity(f)) { failToSafety("GRAVE_CAPACITY_LOW slots=" + f.emptySlots); return pending(detail); }
                if (Rs2Death.isGraveOpen()) { setStage(Stage.FREE); return pending("GRAVE_WIDGET_VISIBLE"); }
                if (openAttempts >= 2) { failToSafety("LOOT_UNPROVED_AFTER_TWO_ATTEMPTS"); return pending(detail); }
                if (Rs2Death.getGrave() == null) {
                    failToSafety("GRAVE_NPC_NOT_VISIBLE; missing=" + missing(f)); return pending(detail);
                }
                openAttempts++;
                if (!action(() -> Rs2Death.openGrave())) return cancelTick();
                if (stage == Stage.EXIT) return pending(detail);
                setStage(Stage.WAIT_OPEN);
                return pending("LOOT_ATTEMPT=" + openAttempts + "; verify raw items next tick");
            case WAIT_OPEN:
                if (Rs2Death.isGraveOpen()) { setStage(Stage.FREE); return pending("GRAVE_WIDGET_VISIBLE"); }
                if (f.at - stageAt >= 6000) {
                    setStage(Stage.OPEN); return pending("LOOT_NOT_PROVED; retry bounded");
                }
                return pending("WAIT_LOOT_PROOF_OR_WIDGET");
            case FREE:
                if (!Rs2Death.isGraveOpen()) { setStage(Stage.OPEN); return pending("GRAVE_WIDGET_CLOSED; reconcile"); }
                if (Rs2Death.getGraveFreeItems().isEmpty()) { setStage(Stage.PAID); return pending("NO_FREE_SLOTS"); }
                if (!capacity(f)) { failToSafety("NO_CAPACITY_FOR_FREE_ITEMS"); return pending(detail); }
                if (freeAttempts >= 2) { failToSafety("FREE_ITEMS_UNPROVED"); return pending(detail); }
                freeAttempts++;
                if (!action(() -> Rs2Death.lootGraveFreeItems())) return cancelTick();
                if (stage == Stage.EXIT) return pending(detail);
                setStage(Stage.WAIT_FREE); return pending("FREE_LOOT_ATTEMPT=" + freeAttempts);
            case WAIT_FREE:
                if (!Rs2Death.isGraveOpen()) { setStage(Stage.OPEN); return pending("WIDGET_CLOSED_AFTER_FREE; reconcile"); }
                if (Rs2Death.getGraveFreeItems().isEmpty()) { setStage(Stage.PAID); return pending("FREE_SLOTS_CLEARED"); }
                if (f.at - stageAt >= 4000) { setStage(Stage.FREE); return pending("FREE_SLOTS_REMAIN"); }
                return pending("WAIT_FREE_ITEM_DELTA");
            case PAID:
                if (!Rs2Death.isGraveOpen()) { setStage(Stage.OPEN); return pending("WIDGET_CLOSED_BEFORE_PAID; reconcile"); }
                if (Rs2Death.getGravePaidItems().isEmpty()) {
                    failToSafety("GRAVE_EMPTY_BUT_MANIFEST_MISSING=" + missing(f)); return pending(detail);
                }
                int fee = Rs2Death.getGraveFee();
                if (fee < 0 || fee > plan.feeCap) {
                    failToSafety("GRAVE_FEE_OUTSIDE_CAP fee=" + fee + " cap=" + plan.feeCap);
                    return pending(detail);
                }
                if (!capacity(f)) { failToSafety("NO_CAPACITY_FOR_PAID_ITEMS"); return pending(detail); }
                if (paidAttempts >= 2) { failToSafety("PAID_ITEMS_UNPROVED"); return pending(detail); }
                paidAttempts++;
                if (!action(() -> Rs2Death.lootGravePaidItems(plan.feeCap))) return cancelTick();
                if (stage == Stage.EXIT) return pending(detail);
                setStage(Stage.WAIT_PAID); return pending("PAID_LOOT_ATTEMPT=" + paidAttempts + " fee=" + fee);
            case WAIT_PAID:
                if (!Rs2Death.isGraveOpen()) { setStage(Stage.OPEN); return pending("WIDGET_CLOSED_AFTER_PAID; reconcile"); }
                if (Rs2Death.getGravePaidItems().isEmpty()) {
                    failToSafety("PAID_SLOTS_CLEARED_BUT_MANIFEST_MISSING=" + missing(f));
                    return pending(detail);
                }
                if (f.at - stageAt >= 4000) { setStage(Stage.PAID); return pending("PAID_SLOTS_REMAIN"); }
                return pending("WAIT_PAID_ITEM_DELTA");
            case CLOSE:
                if (!Rs2Death.isGraveOpen()) { setStage(Stage.EXIT); return pending("GRAVE_WIDGET_CLOSED"); }
                if (closeAttempts >= 2) {
                    failToSafety("GRAVE_WIDGET_CLOSE_UNPROVED"); return pending(detail);
                }
                closeAttempts++;
                if (!action(() -> Rs2Widget.clickWidget(44040195))) return cancelTick();
                if (stage == Stage.EXIT) return pending(detail);
                setStage(Stage.WAIT_CLOSE);
                return pending("CLOSE_WIDGET_ATTEMPT=" + closeAttempts);
            case WAIT_CLOSE:
                if (!Rs2Death.isGraveOpen()) { setStage(Stage.EXIT); return pending("GRAVE_WIDGET_CLOSED"); }
                if (f.at - stageAt >= 5000) { setStage(Stage.CLOSE); return pending("WAIT_CLOSE_TIMED_OUT"); }
                return pending("WAIT_GRAVE_WIDGET_CLOSE_PROOF");
            case EXIT:
                if (route != null && route.worker != null && route.worker.isAlive()
                    && near(f.position, plan.safeExit, plan.exitRadius)) {
                    route.worker.interrupt();
                    return pending("WAIT_ROUTE_QUIESCE_BEFORE_EXIT_PROOF");
                }
                if (near(f.position, plan.safeExit, plan.exitRadius)) {
                    if (route != null) {
                        route = null;
                        Rs2Walker.clearWalkingRoute("quest death safe exit reached");
                    }
                    setStage(holdAfterExit ? Stage.HOLD : Stage.DONE);
                    return result(holdAfterExit ? Outcome.HOLD : Outcome.COMPLETE);
                }
                return walk(f, plan.safeExit, plan.exitRadius, true);
            default:
                return hold("UNEXPECTED_STAGE=" + stage);
        }
    }

    private boolean recovered(Frame f) {
        for (Map.Entry<Integer, Integer> e : plan.expectedPossession.entrySet())
            if (f.possessed(e.getKey()) < e.getValue()) return false;
        return true;
    }
    private String missing(Frame f) {
        Map<Integer, Integer> absent = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : plan.expectedPossession.entrySet()) {
            int n = e.getValue() - f.possessed(e.getKey());
            if (n > 0) absent.put(e.getKey(), n);
        }
        return absent.toString();
    }
    private boolean capacity(Frame f) {
        return f.emptySlots >= plan.minEmptySlots;
    }
    private Result walk(Frame f, WorldPoint target, int radius, boolean leaving) {
        Route current = route;
        if (current != null) {
            if (current.worker != null && current.worker.isAlive()) {
                if (!current.target.equals(target) || current.radius != radius) {
                    current.worker.interrupt();
                    return pending("STOP_OLD_ROUTE_BEFORE_NEW_TARGET");
                }
                if (f.at - current.startedAt > plan.routeTimeoutMs) {
                    current.worker.interrupt();
                    return holdRoute("ROUTE_TIMEOUT target=" + target);
                }
                return pending("ROUTE_RUNNING " + target);
            }
            route = null;
            Rs2Walker.clearWalkingRoute("quest death route ended");
            if (holdAfterRouteStop) return hold(detail);
            if (near(f.position, target, radius)) {
                if (leaving) {
                    setStage(holdAfterExit ? Stage.HOLD : Stage.DONE);
                    return result(holdAfterExit ? Outcome.HOLD : Outcome.COMPLETE);
                }
                setStage(Stage.OPEN); return pending("GRAVE_AREA_REACHED");
            }
            if (current.error != null) detail = "ROUTE_ERROR=" + current.error;
        }
        if (leaving ? ++exitRoutes > 2 : ++graveRoutes > 2)
            return hold("ROUTE_NOT_PROVED target=" + target + " pos=" + f.position
                + " walker=" + (current == null ? "none" : current.result));
        Route next = new Route(target, radius);
        route = next;
        Thread worker = new Thread(() -> {
            try {
                next.result = Rs2Walker.walkWithStateUntil(target, radius,
                    () -> cancelled || Thread.currentThread().isInterrupted() || !owner());
            } catch (Throwable ex) { next.error = ex; }
        }, "QuestDeathRecovery-route");
        next.worker = worker;
        worker.setDaemon(true);
        worker.start();
        return pending("ROUTE_DISPATCH target=" + target);
    }

    private void failToSafety(String reason) {
        detail = reason;
        holdAfterExit = true;
        setStage(Stage.EXIT);
    }
    private boolean action(BooleanSupplier primitive) {
        if (cancelled || !owner()) return false;
        inAction = true;
        try {
            if (cancelled || !owner()) return false;
            try { primitive.getAsBoolean(); } // accepted is not proof
            catch (RuntimeException ex) {
                failToSafety("GRAVE_PRIMITIVE_ERROR=" + ex);
                return true;
            }
            return !cancelled && owner();
        } finally { inAction = false; }
    }
    private boolean owner() {
        try { return ownsInput.getAsBoolean(); }
        catch (RuntimeException ex) { return false; }
    }
    private static boolean near(WorldPoint a, WorldPoint b, int radius) {
        return a != null && b != null && a.getPlane() == b.getPlane()
            && Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())) <= radius;
    }
    private void setStage(Stage next) { stage = next; stageAt = System.currentTimeMillis(); }
    private Result pending(String what) { detail = what; return result(Outcome.PENDING); }
    private Result hold(String what) { detail = what; setStage(Stage.HOLD); return result(Outcome.HOLD); }
    private Result holdRoute(String reason) {
        detail = reason;
        Route current = route;
        if (current != null && current.worker != null && current.worker.isAlive()) {
            holdAfterRouteStop = true;
            current.worker.interrupt();
            return result(Outcome.PENDING);
        }
        if (current != null) {
            route = null;
            Rs2Walker.clearWalkingRoute("quest death route held");
        }
        return hold(reason);
    }
    private Result result(Outcome outcome) { return new Result(outcome, stage, detail); }

    /** Stop request is safe from the plugin's shutdown thread. */
    public void cancel() {
        cancelled = true;
        Route current = route;
        if (current != null && current.worker != null) current.worker.interrupt();
    }
    private Result cancelTick() {
        cancel();
        Route current = route;
        if (inAction || current != null && current.worker != null && current.worker.isAlive()) {
            setStage(Stage.CANCELLING);
            return pending("WAIT_RECOVERY_INPUT_QUIESCE");
        }
        if (current != null) {
            route = null;
            Rs2Walker.clearWalkingRoute("quest death recovery cancelled");
        }
        setStage(Stage.CANCELLED);
        return result(Outcome.CANCELLED);
    }

    /** Do not start other game input or unload the classloader until true. */
    public boolean cancelAndQuiesce(long timeoutMs) {
        cancel();
        long end = System.currentTimeMillis() + Math.max(0, timeoutMs);
        Route current = route;
        Thread worker = current == null ? null : current.worker;
        if (worker != null && worker != Thread.currentThread()) {
            while (worker.isAlive() && System.currentTimeMillis() < end) {
                try { worker.join(Math.min(200, Math.max(1, end - System.currentTimeMillis()))); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); return false; }
            }
        }
        if (worker != null && worker.isAlive() || inAction) return false;
        synchronized (this) {
            if (inAction) return false;
            if (route != null) {
                route = null;
                Rs2Walker.clearWalkingRoute("quest death recovery shutdown");
            }
            setStage(Stage.CANCELLED);
            return true;
        }
    }

    public synchronized Stage stage() { return stage; }
}
