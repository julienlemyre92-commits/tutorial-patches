package net.runelite.client.plugins.microbot.xmarks;

import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.input.InputArbiter;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Quest Helper's installed varbit-8063 route. No click proves its own result. */
public class XMarksScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(XMarksScript.class);
    public static final int BUILD_NUMBER = 530;
    private static final int QUEST_VARBIT = 8063;
    private static final int SPADE = 952, COINS = 995, CASKET = 23071;
    private static final WorldPoint VEOS_LUMBRIDGE = new WorldPoint(3228, 3242, 0);
    private static final WorldPoint BOBS_HOUSE = new WorldPoint(3230, 3209, 0);
    private static final WorldPoint CASTLE_KITCHEN = new WorldPoint(3203, 3212, 0);
    private static final WorldPoint DRAYNOR_JAIL = new WorldPoint(3109, 3264, 0);
    private static final WorldPoint DRAYNOR_PIG_PEN = new WorldPoint(3078, 3259, 0);
    private static final WorldPoint VEOS_SARIM = new WorldPoint(3054, 3245, 0);
    private static final WorldPoint LUMBRIDGE_STORE = new WorldPoint(3212, 3246, 0);
    private static final WorldPoint DRAYNOR_BANK = new WorldPoint(3092, 3245, 0);
    private static final Path STATUS_DIR = Paths.get(System.getProperty("user.home"), ".runelite", "xmarks");
    private static final int[] SALE_ITEMS = {841, 1351, 1277}; // Tutorial shortbow, bronze axe, bronze sword.

    private enum Proof { TALK, DIALOGUE, DIG, REDIG, BUY, SELL, WITHDRAW_SPADE,
        WITHDRAW_COINS, BANK_OPEN, BANK_CLOSED, SHOP_OPEN, SHOP_CLOSED, EAT, OVERLAY }

    private static final class Frame {
        String gameState = "NO_CLIENT", questState = "UNKNOWN", dialogue = "";
        WorldPoint pos;
        int world, varbit = -1;
        boolean overlay, bankOpen, shopOpen, inDialogue, hasContinue;
        double health = -1;
        final Map<Integer, Integer> items = new HashMap<>();
        final List<String> options = new ArrayList<>();
        int count(int id) { return items.getOrDefault(id, 0); }
        boolean loggedIn() { return "LOGGED_IN".equals(gameState) && pos != null; }
        boolean finished() { return "FINISHED".equals(questState); }
    }

    private static final class Pending {
        final String key;
        final Proof proof;
        final Frame before;
        final long at, timeout;
        final int itemId;
        Pending(String key, Proof proof, Frame before, long timeout, int itemId) {
            this.key = key; this.proof = proof; this.before = before;
            this.at = System.currentTimeMillis(); this.timeout = timeout; this.itemId = itemId;
        }
    }

    private static final class Route {
        final String key;
        final WorldPoint target;
        final int radius;
        final long started;
        Thread worker;
        volatile boolean completed;
        volatile long completedAt;
        long segmentStarted;
        int segmentDistance;
        long lastProgress;
        int bestDistance;
        Route(String key, WorldPoint target, int radius, WorldPoint pos) {
            this.key = key; this.target = target; this.radius = radius;
            this.started = this.lastProgress = System.currentTimeMillis();
            this.bestDistance = distance(pos, target);
        }
    }

    private final Map<String, Integer> failures = new HashMap<>();
    private volatile boolean stopped;
    private volatile String stage = "STARTING", error = "";
    private BooleanSupplier ownsInput;
    private Pending pending;
    private Route route;
    private long nextActionAt, unknownDialogueSince, missingSince;
    private String missingKey = "", paceReason = "";
    private int priorCasket = -1, priorVarbit = -1, startTalkAttempts, finishTalkAttempts;
    private boolean turnInPending;
    private long turnInSince, finishInteractionAt;

    public boolean run(XMarksConfig config, BooleanSupplier exclusiveInput) {
        if (isRunning()) return true;
        stopped = false; ownsInput = exclusiveInput;
        priorCasket = -1; priorVarbit = -1; startTalkAttempts = finishTalkAttempts = 0;
        turnInPending = false; turnInSince = 0; finishInteractionAt = 0;
        log.info("[XMarks] RUNNING_BUILD={} quest=X_MARKS_THE_SPOT", BUILD_NUMBER);
        long delay = Math.max(450, Math.min(2000, config.tickDelay()));
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::tick, 0, delay, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override public void shutdown() {
        stopped = true;
        cancelRoute();
        if (mainScheduledFuture != null) mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow();
        super.shutdown();
        try { Files.deleteIfExists(STATUS_DIR.resolve("status.properties")); }
        catch (Exception ex) { log.warn("[XMarks] status cleanup: {}", ex.toString()); }
    }

    private void tick() {
        if (stopped || Thread.currentThread().isInterrupted()) return;
        Frame f = null;
        try {
            f = observe();
            if (!f.loggedIn()) { cancelRoute(); pending = null; stage = "WAIT_LOGIN"; return; }
            if (f.varbit == 6 && priorCasket > 0 && f.count(CASKET) == 0
                && distance(f.pos, VEOS_SARIM) <= 10 && System.currentTimeMillis() - finishInteractionAt < 30000) {
                turnInPending = true; turnInSince = System.currentTimeMillis();
                log.info("[XMarks] CASKET_HAND_IN_OBSERVED awaiting quest state/varbit proof");
            }
            priorCasket = f.count(CASKET);
            if (f.varbit != priorVarbit && f.varbit >= 0) {
                startTalkAttempts = finishTalkAttempts = 0; priorVarbit = f.varbit;
            }
            if (f.varbit == 7) turnInPending = false;
            if (!ownsInput.getAsBoolean()) { cancelRoute(); pending = null; stage = "WAIT_EXCLUSIVE"; return; }
            if (Microbot.pauseAllScripts.get() || InputArbiter.isHuman()) {
                cancelRoute(); stage = "WAIT_INPUT"; return;
            }
            if (Microbot.getBlockingEventManager().shouldBlockAndProcess()) {
                cancelRoute(); stage = "WAIT_BLOCKING_EVENT"; return;
            }
            if (f.finished()) { cancelRoute(); pending = null; stage = "DONE"; return; }
            if (!error.isEmpty()) { cancelRoute(); stage = "HOLD"; return; }
            if (pending != null) { verifyPending(f); return; }
            if (f.health > 0 && f.health < 35 && safety(f)) return;
            if (route != null) { route(f, route.key, route.target, route.radius); return; }
            if (System.currentTimeMillis() < nextActionAt) { stage = "WAIT_PACE"; return; }
            paceReason = "";
            if (f.overlay) {
                stage = "DISMISS_QUEST_SCROLL";
                issue("dismiss:scroll", Proof.OVERLAY, f, 3000, 0,
                    () -> { Rs2Keyboard.keyPress(27); return true; });
                return;
            }
            if (f.varbit < 0 || f.varbit > 7 || "UNKNOWN".equals(f.questState)) {
                hold("Unknown X Marks quest state/varbit: " + f.questState + "/" + f.varbit); return;
            }
            boolean needSpade = f.varbit >= 2 && f.varbit <= 5
                || f.varbit == 6 && f.count(CASKET) == 0 && !turnInPending;
            if (f.bankOpen && (!needSpade || f.count(SPADE) > 0)) { closeBank(f); return; }
            if (f.shopOpen && (!needSpade || f.count(SPADE) > 0)) { closeShop(f); return; }
            if (f.varbit == 6 && distance(f.pos, VEOS_SARIM) <= 10
                && System.currentTimeMillis() - finishInteractionAt < 30000
                && f.count(CASKET) == 0 && !turnInPending) {
                turnInPending = true; turnInSince = System.currentTimeMillis();
            }
            if (dialogue(f)) return;
            if (turnInPending && f.varbit == 6) {
                stage = "VERIFY_CASKET_HAND_IN";
                if (System.currentTimeMillis() - turnInSince > 20000)
                    hold("Veos consumed the casket but varbit 8063 stayed 6 for 20s");
                return;
            }
            switch (f.varbit) {
                case 0: case 1: start(f); return;
                case 2: case 3: case 4: case 5:
                    if (f.count(SPADE) == 0) { obtainSpade(f); return; }
                    digStage(f); return;
                case 6:
                    if (f.count(CASKET) == 0) {
                        if (f.count(SPADE) == 0) { obtainSpade(f); return; }
                        dig(f, "recover-casket", DRAYNOR_PIG_PEN, Proof.REDIG); return;
                    }
                    finish(f); return;
                case 7: finish(f); return;
                default: hold("Unexpected varbit " + f.varbit);
            }
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted()) return;
            hold("Tick exception: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            log.error("[XMarks] tick failed", ex);
        } finally { writeStatus(f); }
    }

    private Frame observe() {
        Frame f = Microbot.getClientThread().invoke(() -> {
            Frame s = new Frame();
            Client c = Microbot.getClient();
            if (c == null) return s;
            GameState gs = c.getGameState();
            s.gameState = gs == null ? "UNKNOWN" : gs.name();
            s.world = c.getWorld();
            if (gs != GameState.LOGGED_IN || c.getLocalPlayer() == null) return s;
            s.pos = c.getLocalPlayer().getWorldLocation();
            s.varbit = c.getVarbitValue(QUEST_VARBIT);
            QuestState qs = Quest.X_MARKS_THE_SPOT.getState(c);
            s.questState = qs == null ? "UNKNOWN" : qs.name();
            Widget scroll = c.getWidget(WidgetInfo.QUEST_COMPLETED);
            s.overlay = scroll != null && !scroll.isHidden();
            ItemContainer inv = c.getItemContainer(InventoryID.INVENTORY);
            if (inv != null) for (Item item : inv.getItems())
                if (item != null && item.getId() >= 0)
                    s.items.merge(item.getId(), item.getQuantity(), Integer::sum);
            return s;
        });
        if (f.loggedIn()) {
            f.health = Rs2Player.getHealthPercentage();
            f.bankOpen = Rs2Bank.isOpen();
            f.shopOpen = Rs2Shop.isOpen();
            f.inDialogue = Rs2Dialogue.isInDialogue();
            f.hasContinue = Rs2Dialogue.hasContinue();
            String text = f.inDialogue ? Rs2Dialogue.getDialogueText() : "";
            StringBuilder d = new StringBuilder(normalize(text));
            for (Widget option : Rs2Dialogue.getDialogueOptions()) {
                if (option == null || normalize(option.getText()).isEmpty()) continue;
                f.options.add(option.getText()); d.append('|').append(normalize(option.getText()));
            }
            f.dialogue = d.toString();
        }
        return f;
    }

    private void verifyPending(Frame f) {
        Pending p = pending;
        if (proved(p, f)) {
            log.info("[XMarks] PROVED action={} varbit={} pos={} spade={} casket={}",
                p.key, f.varbit, f.pos, f.count(SPADE), f.count(CASKET));
            pending = null; failures.remove(p.key); missingKey = ""; missingSince = 0;
            long min = p.proof == Proof.DIALOGUE || p.proof == Proof.TALK ? 100 : 250;
            long max = p.proof == Proof.DIALOGUE || p.proof == Proof.TALK ? 400 : 750;
            if (f.varbit != p.before.varbit) { min = 500; max = 1200; }
            long delay = ThreadLocalRandom.current().nextLong(min, max + 1);
            nextActionAt = System.currentTimeMillis() + delay;
            paceReason = "proved " + p.key;
            return; // A proof tick never issues another action.
        }
        if (System.currentTimeMillis() - p.at < p.timeout) return;
        int count = failures.merge(p.key, 1, Integer::sum);
        pending = null;
        if (p.proof == Proof.DIG && p.before.varbit == 5
            && f.count(CASKET) > p.before.count(CASKET)) {
            hold("Pig-pen dig produced ancient casket but varbit 8063 stayed 5 after 9s");
            return;
        }
        if (count >= 3) hold("Unproved " + p.key + " after " + count + " attempts; varbit="
            + f.varbit + ", pos=" + f.pos + ", spade=" + f.count(SPADE)
            + ", casket=" + f.count(CASKET) + ", dialogue=" + f.dialogue);
        else log.warn("[XMarks] RETRY action={} failure={}/3", p.key, count);
    }

    private boolean proved(Pending p, Frame f) {
        switch (p.proof) {
            case TALK: case DIALOGUE:
                return f.finished() || f.varbit != p.before.varbit
                    || f.inDialogue != p.before.inDialogue || f.hasContinue != p.before.hasContinue
                    || !f.dialogue.equals(p.before.dialogue);
            case DIG: return f.varbit > p.before.varbit;
            case REDIG: return f.count(CASKET) > p.before.count(CASKET) || f.varbit > p.before.varbit;
            case BUY: case WITHDRAW_SPADE: return f.count(SPADE) > p.before.count(SPADE);
            case SELL: case WITHDRAW_COINS: return f.count(COINS) > p.before.count(COINS);
            case BANK_OPEN: return f.bankOpen;
            case BANK_CLOSED: return !f.bankOpen;
            case SHOP_OPEN: return f.shopOpen;
            case SHOP_CLOSED: return !f.shopOpen;
            case EAT: return f.count(p.itemId) < p.before.count(p.itemId);
            case OVERLAY: return !f.overlay;
            default: return false;
        }
    }

    private void issue(String key, Proof proof, Frame f, long timeout, int itemId, BooleanSupplier action) {
        boolean accepted = action.getAsBoolean();
        pending = new Pending(key, proof, f, timeout, itemId);
        log.info("[XMarks] ACTION key={} accepted={} varbit={} pos={} health={}",
            key, accepted, f.varbit, f.pos, f.health);
    }

    private void start(Frame f) {
        stage = "START_VEOS";
        if (route(f, "VEOS_LUMBRIDGE", VEOS_LUMBRIDGE, 1)) return;
        Rs2NpcModel veos = npc(new int[]{8484}, VEOS_LUMBRIDGE, 8);
        if (veos == null) { missingScene("Veos 8484 in The Sheared Ram", 12000); return; }
        if (++startTalkAttempts > 3) {
            hold("Three Veos start conversations without varbit progression; varbit=" + f.varbit);
            return;
        }
        issue("talk:veos-start", Proof.TALK, f, 9000, 0, () -> veos.click("Talk-to"));
    }

    private void digStage(Frame f) {
        WorldPoint target;
        String name;
        switch (f.varbit) {
            case 2: target = BOBS_HOUSE; name = "BOB_HOUSE"; break;
            case 3: target = CASTLE_KITCHEN; name = "CASTLE_KITCHEN"; break;
            case 4: target = DRAYNOR_JAIL; name = "DRAYNOR_JAIL"; break;
            case 5: target = DRAYNOR_PIG_PEN; name = "PIG_PEN"; break;
            default: hold("No dig step for varbit " + f.varbit); return;
        }
        dig(f, name, target, Proof.DIG);
    }

    private void dig(Frame f, String name, WorldPoint target, Proof proof) {
        stage = "DIG_" + name;
        if (route(f, "DIG_" + name, target, 0)) return;
        if (!target.equals(f.pos)) { hold("Dig tile not reached: " + target + " at " + f.pos); return; }
        issue("dig:" + name, proof, f, 9000, 0, () -> Rs2Inventory.interact(SPADE, "Dig"));
    }

    private void finish(Frame f) {
        stage = "FINISH_VEOS";
        if (route(f, "VEOS_SARIM", VEOS_SARIM, 3)) return;
        Rs2NpcModel veos = npc(new int[]{8484, 8630}, VEOS_SARIM, 9);
        if (veos == null) { missingScene("Veos 8484/8630 on Port Sarim dock", 12000); return; }
        if (++finishTalkAttempts > 3) {
            hold("Three Veos finish conversations without varbit/quest completion; casket="
                + f.count(CASKET) + ", varbit=" + f.varbit); return;
        }
        finishInteractionAt = System.currentTimeMillis();
        issue("talk:veos-finish", Proof.TALK, f, 9000, 0, () -> veos.click("Talk-to"));
    }

    /** The installed walker owns path and door interaction; this watchdog owns the time budget. */
    private boolean route(Frame f, String key, WorldPoint target, int radius) {
        if (route != null && (!route.key.equals(key) || !route.target.equals(target))
            && !cancelRoute()) return true;
        int distance = distance(f.pos, target);
        if (distance <= radius) {
            if (route != null) {
                cancelRoute(); failures.remove("walk:" + key);
                stage = "ARRIVED_" + key; return true;
            }
            return false;
        }
        stage = "WALK_" + key;
        if (route == null) {
            Route r = new Route(key, target, radius, f.pos);
            route = r;
            startRouteSegment(r, distance);
            log.info("[XMarks] ROUTE start={} from={} target={} radius={}", key, f.pos, target, radius);
            return true;
        }
        Route r = route;
        long now = System.currentTimeMillis();
        if (distance < r.bestDistance) { r.bestDistance = distance; r.lastProgress = now; }
        if (now - r.started > 240000 || now - r.lastProgress > 20000) {
            routeFailure(f, "route timed out/no progress", r);
            return true;
        }
        if (r.completed && now - r.completedAt >= 1500) {
            // walkWithStateUntil may return for its 15-second budget. Only an
            // observed position change authorizes another route segment.
            if (distance < r.segmentDistance) {
                startRouteSegment(r, distance);
            } else {
                routeFailure(f, "walker segment ended without position progress", r);
            }
        }
        return true;
    }

    private void startRouteSegment(Route r, int distance) {
        r.segmentStarted = System.currentTimeMillis();
        r.segmentDistance = distance;
        r.completed = false;
        r.completedAt = 0;
        Thread t = new Thread(() -> {
            try {
                Rs2Walker.walkWithStateUntil(r.target, r.radius,
                    () -> stopped || Thread.currentThread().isInterrupted()
                        || System.currentTimeMillis() - r.segmentStarted >= 15000);
            } finally {
                r.completedAt = System.currentTimeMillis();
                r.completed = true;
            }
        }, "XMarks-route");
        t.setDaemon(true); r.worker = t; t.start();
    }

    private void routeFailure(Frame f, String reason, Route r) {
        if (!cancelRoute()) return;
        int count = failures.merge("walk:" + r.key, 1, Integer::sum);
        log.warn("[XMarks] ROUTE failure={} attempt={}/3 at={} target={} bestDist={}",
            reason, count, f.pos, r.target, r.bestDistance);
        if (count >= 3) hold("Route " + r.key + " failed after " + count + " attempts: "
            + reason + "; at=" + f.pos + ", target=" + r.target);
        else Rs2Walker.recalculatePath();
    }

    private boolean cancelRoute() {
        Route r = route;
        route = null;
        if (r != null) {
            if (r.worker != null && r.worker.isAlive()) r.worker.interrupt();
            Rs2Walker.setTarget(null);
            if (r.worker != null && r.worker.isAlive()) {
                try { r.worker.join(3000); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                if (r.worker.isAlive()) {
                    error = "Walker thread did not stop within 3s after cancel at " + r.target;
                    stage = "HOLD";
                    log.error("[XMarks] HOLD {}", error);
                    return false;
                }
            }
        }
        return true;
    }

    private void obtainSpade(Frame f) {
        stage = "GET_SPADE";
        if (f.bankOpen) {
            if (Rs2Bank.hasItem(SPADE)) {
                issue("withdraw:spade", Proof.WITHDRAW_SPADE, f, 7000, 0,
                    () -> Rs2Bank.withdrawOne(SPADE)); return;
            }
            if (f.count(COINS) < 3 && Rs2Bank.hasItem(COINS)) {
                issue("withdraw:coins", Proof.WITHDRAW_COINS, f, 7000, 0,
                    () -> Rs2Bank.withdrawAll(COINS)); return;
            }
            if (f.count(COINS) < 3 && saleItem(f) == 0) {
                hold("No spade or enough coins in inventory/bank and no saleable starter item");
                return;
            }
            closeBank(f); return;
        }
        if (f.shopOpen) {
            if (f.count(COINS) < 3) {
                int sale = saleItem(f);
                if (sale != 0) {
                    issue("sell:starter-item-" + sale, Proof.SELL, f, 7000, 0,
                        () -> Rs2Inventory.sellItem(sale, "1")); return;
                }
                closeShop(f); return;
            }
            if (!Rs2Shop.hasStock(SPADE)) { missingScene("Lumbridge shop spade stock 952", 90000); return; }
            issue("buy:spade", Proof.BUY, f, 7000, 0, () -> Rs2Shop.buyItem(SPADE, "1"));
            return;
        }
        if (f.count(COINS) >= 3 || saleItem(f) != 0) {
            if (route(f, "LUMBRIDGE_STORE", LUMBRIDGE_STORE, 1)) return;
            Rs2NpcModel keeper = npc(new int[]{2813}, LUMBRIDGE_STORE, 9);
            if (keeper == null) { missingScene("Lumbridge Shop keeper 2813", 12000); return; }
            issue("open:lumbridge-shop", Proof.SHOP_OPEN, f, 8000, 0,
                () -> keeper.click("Trade"));
            return;
        }
        if (route(f, "DRAYNOR_BANK", DRAYNOR_BANK, 4)) return;
        issue("open:draynor-bank", Proof.BANK_OPEN, f, 9000, 0, Rs2Bank::openBank);
    }

    private static int saleItem(Frame f) {
        for (int id : SALE_ITEMS) if (f.count(id) > 0) return id;
        return 0;
    }

    private void closeShop(Frame f) {
        stage = "CLOSE_SHOP";
        issue("close:shop", Proof.SHOP_CLOSED, f, 5000, 0,
            () -> { Rs2Shop.closeShop(); return true; });
    }

    private void closeBank(Frame f) {
        stage = "CLOSE_BANK";
        issue("close:bank", Proof.BANK_CLOSED, f, 5000, 0,
            () -> { Rs2Keyboard.keyPress(27); return true; });
    }

    private boolean safety(Frame f) {
        stage = "LOW_HEALTH";
        List<Rs2ItemModel> food = Rs2Inventory.getInventoryFood();
        if (food == null || food.isEmpty()) {
            if (f.health <= 30) {
                hold("Health " + f.health + "% near Draynor guards; no food in inventory");
                return true;
            }
            return false;
        }
        if (!cancelRoute()) return true;
        Rs2ItemModel choice = food.get(0);
        int id = choice.getId();
        issue("eat:" + id, Proof.EAT, f, 5000, id, () -> Rs2Inventory.interact(id, "Eat"));
        return true;
    }

    /** An unknown dialogue is observed, never answered by a generic Yes/first option. */
    private boolean dialogue(Frame f) {
        if (!f.inDialogue && f.options.isEmpty() && !f.hasContinue) {
            unknownDialogueSince = 0; return false;
        }
        if (f.varbit == 6 && distance(f.pos, VEOS_SARIM) <= 10)
            finishInteractionAt = System.currentTimeMillis();
        if (!f.options.isEmpty()) {
            String[] accepted = f.varbit <= 1
                ? new String[]{"i'm looking for a quest", "sounds good, what should i do",
                    "can i help", "yes."}
                : f.varbit == 2
                    ? new String[]{"okay, thanks veos"}
                    : new String[0];
            for (String option : f.options) for (String fragment : accepted)
                if (normalize(option).contains(fragment)) {
                    unknownDialogueSince = 0;
                    issue("dialogue:" + fragment, Proof.DIALOGUE, f, 7500, 0,
                        () -> Rs2Dialogue.clickOption(option));
                    return true;
                }
            if (unknownDialogueSince == 0) unknownDialogueSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - unknownDialogueSince > 10000)
                hold("Unexpected dialogue choices at varbit " + f.varbit + ": " + f.options);
            return true;
        }
        if (f.hasContinue) {
            unknownDialogueSince = 0;
            issue("dialogue:continue", Proof.DIALOGUE, f, 7500, 0,
                () -> { Rs2Dialogue.clickContinue(); return true; });
            return true;
        }
        if (unknownDialogueSince == 0) unknownDialogueSince = System.currentTimeMillis();
        if (System.currentTimeMillis() - unknownDialogueSince > 20000)
            hold("Dialogue has no known option or Continue: " + f.dialogue);
        return true;
    }

    private static Rs2NpcModel npc(int[] ids, WorldPoint point, int radius) {
        return Microbot.getRs2NpcCache().query().withIds(ids).within(point, radius).nearestOnClientThread();
    }

    private void missingScene(String key, long timeout) {
        if (!key.equals(missingKey)) { missingKey = key; missingSince = System.currentTimeMillis(); }
        if (System.currentTimeMillis() - missingSince > timeout) hold("Target absent from loaded scene: " + key);
    }

    private void hold(String reason) {
        if (error.isEmpty()) log.error("[XMarks] HOLD {}", reason);
        error = reason; stage = "HOLD"; paceReason = ""; cancelRoute();
    }

    private static int distance(WorldPoint a, WorldPoint b) {
        if (a == null || b == null || a.getPlane() != b.getPlane()) return Integer.MAX_VALUE;
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("<[^>]*>", "").trim().toLowerCase();
    }

    private void writeStatus(Frame f) {
        try {
            Files.createDirectories(STATUS_DIR);
            Properties p = new Properties();
            p.setProperty("build", Integer.toString(BUILD_NUMBER));
            p.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
            p.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
            p.setProperty("gameState", f == null ? "UNKNOWN" : f.gameState);
            p.setProperty("currentWorld", f == null ? "0" : Integer.toString(f.world));
            p.setProperty("stage", stage);
            p.setProperty("error", error);
            p.setProperty("questVarbit", f == null ? "-1" : Integer.toString(f.varbit));
            p.setProperty("questState", f == null ? "UNKNOWN" : f.questState);
            p.setProperty("position", f == null ? "UNKNOWN" : String.valueOf(f.pos));
            p.setProperty("healthPercent", f == null ? "-1" : Double.toString(f.health));
            p.setProperty("spade", f == null ? "0" : Integer.toString(f.count(SPADE)));
            p.setProperty("ancientCasket", f == null ? "0" : Integer.toString(f.count(CASKET)));
            p.setProperty("coins", f == null ? "0" : Integer.toString(f.count(COINS)));
            p.setProperty("pending", pending == null ? "" : pending.key);
            p.setProperty("walkTarget", route == null ? "" : String.valueOf(route.target));
            p.setProperty("paceRemainingMs", Long.toString(Math.max(0, nextActionAt - System.currentTimeMillis())));
            p.setProperty("paceReason", paceReason);
            Path temp = STATUS_DIR.resolve("status.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "X Marks live script"); }
            Path status = STATUS_DIR.resolve("status.properties");
            for (int attempt = 0; attempt < 4; attempt++) {
                try {
                    try { Files.move(temp, status,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                    catch (AtomicMoveNotSupportedException ex) { Files.move(temp, status,
                        StandardCopyOption.REPLACE_EXISTING); }
                    break;
                } catch (FileSystemException ex) {
                    if (attempt == 3) throw ex;
                    try { Thread.sleep(25L * (attempt + 1)); }
                    catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt(); return;
                    }
                }
            }
        } catch (Exception ex) { log.warn("[XMarks] status write: {}", ex.toString()); }
    }
}
