package net.runelite.client.plugins.microbot.goblindiplomacy;

import java.io.InputStream;
import java.io.OutputStream;
import java.awt.Rectangle;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.input.InputArbiter;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Noncombat Goblin Diplomacy route for the installed Quest Helper's varbit 2378. */
public class GoblinDiplomacyScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(GoblinDiplomacyScript.class);
    public static final int BUILD_NUMBER = 547;
    private static final int QUEST_VARBIT = 2378;
    private static final int MAIL = 288, ORANGE_MAIL = 286, BLUE_MAIL = 287;
    private static final int RED_DYE = 1763, YELLOW_DYE = 1765;
    private static final int BLUE_DYE = 1767, ORANGE_DYE = 1769;
    private static final int REDBERRIES = 1951, ONION = 1957, WOAD = 1793, COINS = 995;
    private static final int[] GENERALS = {669, 670};
    private static final int[] AGGIE_IDS = {120, 121, 4284};
    private static final int[] WYSON_IDS = {5422};
    private static final int[] WYDIN_IDS = {2890, 1791};
    private static final int[] ONION_IDS = {3366, 5538};
    private static final WorldPoint GENERALS_POINT = new WorldPoint(2958, 3512, 0);
    private static final WorldPoint NORTH_CRATE = new WorldPoint(2959, 3514, 0);
    private static final WorldPoint NORTH_CRATE_ACCESS = new WorldPoint(2960, 3514, 0);
    private static final WorldPoint WEST_CRATE = new WorldPoint(2951, 3508, 0);
    private static final WorldPoint UPPER_CRATE = new WorldPoint(2955, 3498, 2);
    private static final WorldPoint LADDER_GROUND = new WorldPoint(2954, 3497, 0);
    private static final WorldPoint LADDER_UPPER = new WorldPoint(2954, 3497, 2);
    private static final WorldPoint WEST_VARROCK_EXIT = new WorldPoint(3175, 3429, 0);
    private static final WorldPoint WYSON = new WorldPoint(3025, 3378, 0);
    private static final WorldPoint WYDIN = new WorldPoint(3013, 3204, 0);
    private static final WorldPoint FRED_ONIONS = new WorldPoint(3190, 3263, 0);
    private static final WorldPoint PORT_SARIM_NORTHEAST = new WorldPoint(3054, 3245, 0);
    private static final WorldPoint DRAYNOR_JAIL_ROAD = new WorldPoint(3109, 3264, 0);
    private static final WorldPoint AGGIE = new WorldPoint(3086, 3258, 0);
    private static final Path STATUS_DIR = Paths.get(System.getProperty("user.home"),
        ".runelite", "goblindiplomacy");

    private enum Proof {
        TALK, DIALOGUE, LADDER_UP, LADDER_DOWN, CRATE, ONION,
        SHOP_OPEN, SHOP_CLOSE, REDBERRY, WOAD, RED_DYE, YELLOW_DYE,
        BLUE_DYE, ORANGE_DYE, ORANGE_MAIL, BLUE_MAIL, EAT
    }

    private static final class Frame {
        String gameState = "NO_CLIENT", questState = "UNKNOWN", dialogue = "";
        WorldPoint pos;
        int world, varbit = -1, northFlag = -1, westFlag = -1, upperFlag = -1;
        int freeSlots;
        boolean inventoryLoaded, inDialogue, hasContinue, shopOpen;
        double health = -1;
        final Map<Integer, Integer> items = new HashMap<>();
        final List<String> options = new ArrayList<>();
        int count(int id) { return items.getOrDefault(id, 0); }
        boolean loggedIn() { return "LOGGED_IN".equals(gameState) && pos != null; }
        boolean finished() { return "FINISHED".equals(questState); }
        boolean upstairs() { return pos != null && pos.getPlane() == 2
            && pos.getX() >= 2952 && pos.getX() <= 2959
            && pos.getY() >= 3495 && pos.getY() <= 3498; }
        int mailTotal() { return count(MAIL) + count(ORANGE_MAIL) + count(BLUE_MAIL); }
    }

    private static final class Pending {
        final String key;
        final Proof proof;
        final Frame before;
        final long at, timeout;
        final int item;
        Pending(String key, Proof proof, Frame before, long timeout, int item) {
            this.key = key; this.proof = proof; this.before = before;
            this.at = System.currentTimeMillis(); this.timeout = timeout; this.item = item;
        }
    }

    private static final class Route {
        final String key;
        final WorldPoint target;
        final int radius;
        final long started;
        WorldPoint walkTarget, lastObserved, segmentStart;
        Thread worker;
        volatile boolean completed;
        volatile long completedAt;
        long segmentStarted, lastProgress;
        int segmentDistance, bestDistance;
        final Set<WorldPoint> visited = new HashSet<>();
        boolean cancelRequested;
        long cancelAt;
        String cancelReason, failureReason;
        Thread clearWorker;
        volatile String clearError;
        Route(String key, WorldPoint target, int radius, WorldPoint pos) {
            this.key = key; this.target = target; this.radius = radius;
            this.started = this.lastProgress = System.currentTimeMillis();
            this.bestDistance = distance(pos, target);
            this.lastObserved = pos;
            this.visited.add(pos);
        }
    }

    private final Map<String, Integer> failures = new HashMap<>();
    private volatile boolean stopped;
    private volatile String stage = "STARTING", error = "";
    private BooleanSupplier ownsInput;
    private Pending pending;
    private Route route;
    private long loggedInAt, nextActionAt, missingSince, unknownDialogueSince;
    private long transientStageSince, handoffAt, shopStockSince, lastWysonAt;
    private String missingKey = "", paceReason = "", talkKey = "", classSha256 = "UNKNOWN";
    private int priorVarbit = -1, priorOrange = -1, priorBlue = -1, priorPlain = -1;
    private int talkAttempts;
    private boolean memberClosePending;
    private long memberCloseAt;
    private int memberCloseAttempts, escapeAttempts;
    private WorldPoint escapeFrom, escapeTarget;
    private long escapeAt;
    private final Set<WorldPoint> escapeRejected = new HashSet<>();

    public boolean run(GoblinDiplomacyConfig config, BooleanSupplier exclusiveInput) {
        if (isRunning()) return true;
        stopped = false; ownsInput = exclusiveInput;
        stage = "STARTING"; error = ""; pending = null; route = null;
        failures.clear(); loggedInAt = nextActionAt = missingSince = 0;
        unknownDialogueSince = transientStageSince = handoffAt = shopStockSince = lastWysonAt = 0;
        priorVarbit = priorOrange = priorBlue = priorPlain = -1;
        talkAttempts = 0; talkKey = missingKey = paceReason = "";
        memberClosePending = false; memberCloseAt = 0; memberCloseAttempts = 0;
        escapeFrom = escapeTarget = null; escapeAt = 0; escapeAttempts = 0;
        escapeRejected.clear();
        classSha256 = loadedClassSha256();
        log.info("[GoblinDiplomacy] RUNNING_BUILD={} quest=GOBLIN_DIPLOMACY classSha256={}",
            BUILD_NUMBER, classSha256);
        long delay = Math.max(450, Math.min(2000, config.tickDelay()));
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::tick,
            0, delay, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override public void shutdown() {
        stopped = true;
        cancelRoute();
        if (mainScheduledFuture != null) mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow();
        super.shutdown();
        try { Files.deleteIfExists(STATUS_DIR.resolve("status.properties")); }
        catch (Exception ex) { log.warn("[GoblinDiplomacy] status cleanup: {}", ex.toString()); }
    }

    private void tick() {
        if (stopped || Thread.currentThread().isInterrupted()) return;
        Frame f = null;
        try {
            f = observe();
            if (!f.loggedIn()) {
                cancelRoute(); pending = null; loggedInAt = 0;
                stage = "WAIT_LOGIN"; return;
            }
            long now = System.currentTimeMillis();
            if (loggedInAt == 0) loggedInAt = now;
            if (!f.inventoryLoaded || now - loggedInAt < 3000) {
                stage = "WAIT_INVENTORY"; return;
            }
            boolean stageChanged = priorVarbit >= 0 && f.varbit != priorVarbit;
            if (stageChanged) {
                talkKey = ""; talkAttempts = 0; handoffAt = transientStageSince = 0;
            }
            if (f.varbit >= 0) priorVarbit = f.varbit;
            if (priorOrange > 0 && f.count(ORANGE_MAIL) == 0 && f.varbit == 3) handoffAt = now;
            if (priorBlue > 0 && f.count(BLUE_MAIL) == 0 && f.varbit == 4) handoffAt = now;
            if (priorPlain > 0 && f.count(MAIL) == 0 && f.varbit == 5) handoffAt = now;
            priorOrange = f.count(ORANGE_MAIL);
            priorBlue = f.count(BLUE_MAIL);
            priorPlain = f.count(MAIL);
            if (!ownsInput.getAsBoolean()) {
                cancelRoute(); pending = null; stage = "WAIT_EXCLUSIVE"; return;
            }
            if (Microbot.pauseAllScripts.get() || InputArbiter.isHuman()) {
                cancelRoute(); stage = "WAIT_INPUT"; return;
            }
            if (Microbot.getBlockingEventManager().shouldBlockAndProcess()) {
                cancelRoute(); stage = "WAIT_BLOCKING_EVENT"; return;
            }
            if (f.finished()) { cancelRoute(); pending = null; stage = "DONE"; return; }
            if (!error.isEmpty()) { cancelRoute(); stage = "HOLD"; return; }
            if (handleMemberPrompt()) return;
            if (stageChanged && route != null) {
                route.cancelReason = "quest stage changed to " + f.varbit;
                cancelRoute(); return;
            }
            if (f.health > 0 && f.health < 35 && pending != null
                && pending.proof != Proof.EAT) {
                log.warn("[GoblinDiplomacy] SURVIVAL preempt={} at health={}%; rescan next tick",
                    pending.key, f.health);
                pending = null;
                cancelRoute(); stage = "PREEMPT_CRITICAL_HEALTH"; return;
            }
            if (pending != null) { verifyPending(f); return; }
            if (f.health > 0 && f.health < 35 && safety(f)) return;
            if (route != null) { route(f, route.key, route.target, route.radius); return; }
            if (now < nextActionAt) { stage = "WAIT_PACE"; return; }
            paceReason = "";
            if (f.varbit < 0 || "UNKNOWN".equals(f.questState)) {
                hold("Unknown Goblin Diplomacy state/varbit: " + f.questState + "/" + f.varbit);
                return;
            }
            if (f.varbit >= 6) {
                if (transientStageSince == 0) transientStageSince = now;
                if (dialogue(f)) return;
                if (now - transientStageSince > 30000)
                    hold("Quest varbit " + f.varbit + " did not prove QuestState.FINISHED");
                else stage = "VERIFY_FINISH";
                return;
            }
            if (dialogue(f)) return;
            if (f.varbit == 1 || f.varbit == 2) {
                if (transientStageSince == 0) transientStageSince = now;
                if (now - transientStageSince > 30000)
                    hold("Start dialogue paused at unmapped transient varbit " + f.varbit);
                else stage = "WAIT_START_DIALOGUE";
                return;
            }
            transientStageSince = 0;
            if (handoffAt != 0) {
                if (now - handoffAt > 30000)
                    hold("Mail handed in but quest varbit did not advance from " + f.varbit);
                else stage = "VERIFY_MAIL_HAND_IN";
                return;
            }
            if (f.varbit != 0 && !preflight(f)) return;
            switch (f.varbit) {
                case 0: talkGeneral(f, "START"); return;
                case 3: orangeStage(f); return;
                case 4: blueStage(f); return;
                case 5: brownStage(f); return;
                default: hold("Unmapped Goblin Diplomacy varbit " + f.varbit);
            }
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted()) return;
            hold("Tick exception: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            log.error("[GoblinDiplomacy] tick failed", ex);
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
            s.northFlag = c.getVarbitValue(2379);
            s.westFlag = c.getVarbitValue(2380);
            s.upperFlag = c.getVarbitValue(2381);
            QuestState qs = Quest.GOBLIN_DIPLOMACY.getState(c);
            s.questState = qs == null ? "UNKNOWN" : qs.name();
            ItemContainer inv = c.getItemContainer(InventoryID.INVENTORY);
            s.inventoryLoaded = inv != null;
            if (inv != null) {
                int occupied = 0;
                for (Item item : inv.getItems()) {
                    if (item == null || item.getId() < 0) continue;
                    occupied++;
                    s.items.merge(item.getId(), item.getQuantity(), Integer::sum);
                }
                // RuneLite can expose only the occupied prefix of this container.
                // Backpack capacity is 28 even when that array is shorter.
                s.freeSlots = Math.max(0, 28 - occupied);
            }
            return s;
        });
        if (f.loggedIn()) {
            f.health = Rs2Player.getHealthPercentage();
            f.shopOpen = Rs2Shop.isOpen();
            f.inDialogue = Rs2Dialogue.isInDialogue();
            f.hasContinue = Rs2Dialogue.hasContinue();
            String text = f.inDialogue ? Rs2Dialogue.getDialogueText() : "";
            StringBuilder d = new StringBuilder(normalize(text));
            for (Widget option : Rs2Dialogue.getDialogueOptions()) {
                if (option == null || normalize(option.getText()).isEmpty()) continue;
                f.options.add(option.getText());
                d.append('|').append(normalize(option.getText()));
            }
            f.dialogue = d.toString();
        }
        return f;
    }

    private void verifyPending(Frame f) {
        Pending p = pending;
        if (proved(p, f)) {
            log.info("[GoblinDiplomacy] PROVED action={} varbit={} pos={} mail={}/{}/{}",
                p.key, f.varbit, f.pos, f.count(MAIL), f.count(ORANGE_MAIL), f.count(BLUE_MAIL));
            pending = null; failures.remove(p.key); missingKey = ""; missingSince = 0;
            long min = p.proof == Proof.DIALOGUE || p.proof == Proof.TALK ? 100 : 250;
            long max = p.proof == Proof.DIALOGUE || p.proof == Proof.TALK ? 400 : 750;
            if (f.varbit != p.before.varbit) { min = 500; max = 1200; }
            nextActionAt = System.currentTimeMillis()
                + ThreadLocalRandom.current().nextLong(min, max + 1);
            paceReason = "proved " + p.key;
            return;
        }
        if (System.currentTimeMillis() - p.at < p.timeout) return;
        int count = failures.merge(p.key, 1, Integer::sum);
        pending = null;
        if (count >= 3) hold("Unproved " + p.key + " after " + count
            + " attempts; varbit=" + f.varbit + ", pos=" + f.pos
            + ", mail=" + f.count(MAIL) + "/" + f.count(ORANGE_MAIL)
            + "/" + f.count(BLUE_MAIL) + ", dialogue=" + f.dialogue);
        else log.warn("[GoblinDiplomacy] RETRY action={} failure={}/3", p.key, count);
    }

    private boolean proved(Pending p, Frame f) {
        switch (p.proof) {
            case TALK: case DIALOGUE:
                return f.finished() || f.varbit != p.before.varbit
                    || f.count(MAIL) != p.before.count(MAIL)
                    || f.count(ORANGE_MAIL) != p.before.count(ORANGE_MAIL)
                    || f.count(BLUE_MAIL) != p.before.count(BLUE_MAIL)
                    || f.count(WOAD) != p.before.count(WOAD)
                    || f.inDialogue != p.before.inDialogue
                    || f.hasContinue != p.before.hasContinue
                    || !f.dialogue.equals(p.before.dialogue);
            case LADDER_UP: return f.upstairs();
            case LADDER_DOWN: return f.pos != null && f.pos.getPlane() == 0;
            case CRATE: return f.count(MAIL) > p.before.count(MAIL);
            case ONION: return f.count(ONION) > p.before.count(ONION);
            case SHOP_OPEN: return f.shopOpen;
            case SHOP_CLOSE: return !f.shopOpen;
            case REDBERRY: return f.count(REDBERRIES) > p.before.count(REDBERRIES);
            case WOAD: return f.count(WOAD) > p.before.count(WOAD);
            case RED_DYE: return f.count(RED_DYE) > p.before.count(RED_DYE);
            case YELLOW_DYE: return f.count(YELLOW_DYE) > p.before.count(YELLOW_DYE);
            case BLUE_DYE: return f.count(BLUE_DYE) > p.before.count(BLUE_DYE);
            case ORANGE_DYE: return f.count(ORANGE_DYE) > p.before.count(ORANGE_DYE);
            case ORANGE_MAIL: return f.count(ORANGE_MAIL) > p.before.count(ORANGE_MAIL);
            case BLUE_MAIL: return f.count(BLUE_MAIL) > p.before.count(BLUE_MAIL);
            case EAT: return f.count(p.item) < p.before.count(p.item);
            default: return false;
        }
    }

    private void issue(String key, Proof proof, Frame f, long timeout, int item,
                       BooleanSupplier action) {
        boolean accepted = action.getAsBoolean();
        pending = new Pending(key, proof, f, timeout, item);
        log.info("[GoblinDiplomacy] ACTION key={} accepted={} varbit={} pos={} target={} health={}",
            key, accepted, f.varbit, f.pos, item, f.health);
    }

    private boolean preflight(Frame f) {
        int coins = 0, supplies = 0, mail = 0;
        if (f.varbit <= 3) {
            if (f.count(ORANGE_MAIL) == 0 && f.count(ORANGE_DYE) == 0) {
                if (f.count(RED_DYE) == 0) {
                    int berriesToBuy = Math.max(0, 3 - f.count(REDBERRIES));
                    coins += 5 + 3 * berriesToBuy;
                    supplies += berriesToBuy;
                }
                if (f.count(YELLOW_DYE) == 0) {
                    coins += 5; supplies += Math.max(0, 2 - f.count(ONION));
                }
            }
            mail = Math.max(0, 3 - f.mailTotal());
        } else if (f.varbit == 4)
            mail = Math.max(0, 2 - f.count(MAIL) - f.count(BLUE_MAIL));
        else if (f.varbit == 5) mail = Math.max(0, 1 - f.count(MAIL));
        if (f.varbit <= 4 && f.count(BLUE_MAIL) == 0 && f.count(BLUE_DYE) == 0) {
            coins += 5;
            if (f.count(WOAD) < 2) { coins += 20; supplies += 2 - f.count(WOAD); }
        }
        if (f.count(COINS) < coins) {
            hold("Need " + coins + " coins in inventory for missing berries/dyes/woad; have "
                + f.count(COINS)); return false;
        }
        int needed = Math.max(mail, supplies);
        if (f.freeSlots < needed) {
            hold("Need at least " + needed + " free inventory slots to source missing items; have "
                + f.freeSlots); return false;
        }
        return true;
    }

    private void orangeStage(Frame f) {
        stage = "PREPARE_ORANGE";
        if (prepareDyes(f, true)) return;
        if (collectMail(f, 3)) return;
        if (f.count(ORANGE_MAIL) == 0) {
            if (f.count(ORANGE_DYE) == 0 || f.count(MAIL) < 2) {
                hold("Orange stage lacks orange dye or two plain mail after collection"); return;
            }
            issue("combine:orange-mail", Proof.ORANGE_MAIL, f, 7000, ORANGE_MAIL,
                () -> Rs2Inventory.combine(ORANGE_DYE, MAIL));
            return;
        }
        talkGeneral(f, "ORANGE");
    }

    private void blueStage(Frame f) {
        stage = "PREPARE_BLUE";
        if (prepareDyes(f, false)) return;
        if (collectMail(f, 2)) return;
        if (f.count(BLUE_MAIL) == 0) {
            if (f.count(BLUE_DYE) == 0 || f.count(MAIL) < 2) {
                hold("Blue stage lacks blue dye or two plain mail after collection"); return;
            }
            issue("combine:blue-mail", Proof.BLUE_MAIL, f, 7000, BLUE_MAIL,
                () -> Rs2Inventory.combine(BLUE_DYE, MAIL));
            return;
        }
        talkGeneral(f, "BLUE");
    }

    private void brownStage(Frame f) {
        stage = "PREPARE_BROWN";
        if (collectMail(f, 1)) return;
        if (f.count(MAIL) == 0) { hold("Brown mail unavailable after crate search"); return; }
        talkGeneral(f, "BROWN");
    }

    /** Make only dyes still needed by the current stage and observed inventory. */
    private boolean prepareDyes(Frame f, boolean needOrange) {
        boolean orangeMissing = needOrange && f.count(ORANGE_MAIL) == 0;
        if (orangeMissing && f.count(ORANGE_DYE) == 0) {
            if (f.count(RED_DYE) == 0 && f.count(REDBERRIES) < 3) {
                sourceRedberries(f); return true;
            }
            if (f.count(YELLOW_DYE) == 0 && f.count(ONION) < 2) {
                sourceOnions(f); return true;
            }
        }
        boolean blueMissing = f.count(BLUE_MAIL) == 0 && f.count(BLUE_DYE) == 0;
        if (blueMissing && f.count(WOAD) < 2) { sourceWoad(f); return true; }
        if (orangeMissing && f.count(ORANGE_DYE) == 0) {
            if (f.count(RED_DYE) == 0) { makeDye(f, REDBERRIES, RED_DYE, Proof.RED_DYE); return true; }
            if (f.count(YELLOW_DYE) == 0) { makeDye(f, ONION, YELLOW_DYE, Proof.YELLOW_DYE); return true; }
            issue("combine:orange-dye", Proof.ORANGE_DYE, f, 7000, ORANGE_DYE,
                () -> Rs2Inventory.combine(RED_DYE, YELLOW_DYE));
            return true;
        }
        if (blueMissing) { makeDye(f, WOAD, BLUE_DYE, Proof.BLUE_DYE); return true; }
        return false;
    }

    private void sourceRedberries(Frame f) {
        stage = "SOURCE_REDBERRIES_WYDIN";
        if (f.upstairs()) { descend(f); return; }
        if (route(f, "WYDIN", WYDIN, 4)) return;
        if (!f.shopOpen) {
            Rs2NpcModel wydin = npc(WYDIN_IDS, WYDIN, 12);
            if (wydin == null) { missingScene("Wydin near Port Sarim Food Store", 12000); return; }
            if (!wydin.hasLineOfSight()) {
                if (route(f, "WYDIN_APPROACH", wydin.getWorldLocation(), 1)) return;
                missingScene("Wydin line of sight", 12000); return;
            }
            issue("shop:open-wydin", Proof.SHOP_OPEN, f, 8000, 0,
                () -> wydin.click("Trade"));
            return;
        }
        if (Rs2Shop.shopItems == null) {
            missingScene("Wydin shop inventory", 8000); return;
        }
        if (!shopHasStock(REDBERRIES)) {
            if (shopStockSince == 0) shopStockSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - shopStockSince > 120000)
                hold("Wydin had no redberries in stock for two minutes");
            else stage = "WAIT_REDBERRY_STOCK";
            return;
        }
        shopStockSince = 0;
        issue("shop:buy-redberries", Proof.REDBERRY, f, 7000, REDBERRIES,
            // Rs2Shop.buyItem prefixes "Buy " before its quantity argument.
            () -> Rs2Shop.buyItem(REDBERRIES, "1"));
    }

    private static boolean shopHasStock(int itemId) {
        // Same positive-quantity test as Rs2Shop.hasStock, without its warning
        // on every empty-stock tick (which is echoed into the game chat).
        List<Rs2ItemModel> items = Rs2Shop.shopItems;
        if (items == null) return false;
        for (Rs2ItemModel item : items)
            if (item != null && item.getId() == itemId && item.getQuantity() > 0)
                return true;
        return false;
    }

    private void sourceOnions(Frame f) {
        stage = "SOURCE_ONIONS_FRED";
        if (f.shopOpen) {
            issue("shop:close-wydin", Proof.SHOP_CLOSE, f, 5000, 0,
                () -> { Rs2Shop.closeShop(); return true; });
            return;
        }
        if (f.upstairs()) { descend(f); return; }
        if (route(f, "FRED_ONION_FIELD", FRED_ONIONS, 5)) return;
        Rs2TileObjectModel onion = object(ONION_IDS, FRED_ONIONS, 17);
        if (onion == null) { missingScene("onion plant 3366/5538 at Fred's field", 12000); return; }
        String action = objectAction(onion, "Pick", "Take");
        if (action == null) { missingScene("onion plant has no Pick/Take action", 12000); return; }
        issue("pick:onion", Proof.ONION, f, 9000, ONION, () -> onion.click(action));
    }

    private void sourceWoad(Frame f) {
        stage = "SOURCE_WOAD_WYSON";
        if (f.shopOpen) {
            issue("shop:close-wydin", Proof.SHOP_CLOSE, f, 5000, 0,
                () -> { Rs2Shop.closeShop(); return true; });
            return;
        }
        if (f.upstairs()) { descend(f); return; }
        if (route(f, "WYSON", WYSON, 5)) return;
        if (lastWysonAt != 0 && System.currentTimeMillis() - lastWysonAt < 8000) {
            stage = "VERIFY_WOAD_PURCHASE"; return;
        }
        Rs2NpcModel wyson = npc(WYSON_IDS, WYSON, 18);
        if (wyson == null) { missingScene("Wyson the gardener 5422", 12000); return; }
        if (!wyson.hasLineOfSight()) {
            if (route(f, "WYSON_APPROACH", wyson.getWorldLocation(), 1)) return;
            missingScene("Wyson line of sight", 12000); return;
        }
        talk(f, "WYSON", wyson);
    }

    private void makeDye(Frame f, int ingredient, int product, Proof proof) {
        stage = "MAKE_DYE_" + product;
        if (f.shopOpen) {
            issue("shop:close-wydin", Proof.SHOP_CLOSE, f, 5000, 0,
                () -> { Rs2Shop.closeShop(); return true; });
            return;
        }
        if (f.upstairs()) { descend(f); return; }
        if (route(f, "AGGIE", AGGIE, 4)) return;
        Rs2NpcModel aggie = npc(AGGIE_IDS, AGGIE, 12);
        if (aggie == null) { missingScene("Aggie 120/121/4284", 12000); return; }
        if (!aggie.hasLineOfSight()) {
            if (route(f, "AGGIE_APPROACH", aggie.getWorldLocation(), 1)) return;
            missingScene("Aggie line of sight", 12000); return;
        }
        // The installed Microbot API sends one selected ingredient to the NPC;
        // Aggie makes the primary dye when the remaining ingredients/coins exist.
        issue("make:dye-" + product, proof, f, 10000, product,
            () -> Rs2Inventory.useItemOnNpc(ingredient, aggie.getNpc()));
    }

    private boolean collectMail(Frame f, int neededTotal) {
        int usable = f.varbit == 4 ? f.count(MAIL) + f.count(BLUE_MAIL)
            : f.varbit == 5 ? f.count(MAIL) : f.mailTotal();
        if (usable >= neededTotal) return false;
        stage = "COLLECT_GOBLIN_MAIL";
        if (f.shopOpen) {
            issue("shop:close-wydin", Proof.SHOP_CLOSE, f, 5000, 0,
                () -> { Rs2Shop.closeShop(); return true; });
            return true;
        }
        if (f.upstairs() && f.upperFlag == 1) { descend(f); return true; }
        if (f.upstairs()) {
            searchCrate(f, "UPPER", 16561, UPPER_CRATE); return true;
        }
        if (f.pos.getPlane() != 0) {
            hold("Cannot reach Goblin Village crates from plane " + f.pos.getPlane()); return true;
        }
        if (f.northFlag != 1) {
            searchCrate(f, "NORTH", 16559, NORTH_CRATE); return true;
        }
        if (f.westFlag != 1) {
            searchCrate(f, "WEST", 16560, WEST_CRATE); return true;
        }
        if (f.upperFlag != 1) { ascend(f); return true; }
        hold("All three village crate flags are set but only " + f.mailTotal()
            + " usable mail remain; no combat replacement route is enabled");
        return true;
    }

    private void searchCrate(Frame f, String name, int id, WorldPoint point) {
        stage = "SEARCH_" + name + "_CRATE";
        if (route(f, name + "_CRATE", point, 3)) return;
        Rs2TileObjectModel crate = object(new int[]{id}, point, 6);
        if (crate == null) { missingScene(name + " crate " + id, 12000); return; }
        String action = objectAction(crate, "Search");
        if (action == null) { missingScene(name + " crate has no Search action", 12000); return; }
        issue("search:" + name.toLowerCase() + "-crate", Proof.CRATE, f, 9000, MAIL,
            () -> crate.click(action));
    }

    private void ascend(Frame f) {
        stage = "CLIMB_VILLAGE_LADDER_UP";
        if (route(f, "VILLAGE_LADDER_GROUND", LADDER_GROUND, 3)) return;
        Rs2TileObjectModel ladder = object(new int[]{16450}, LADDER_GROUND, 5);
        if (ladder == null) { missingScene("village up ladder 16450", 12000); return; }
        String action = objectAction(ladder, "Climb-up", "Climb");
        if (action == null) { missingScene("village up ladder has no climb action", 12000); return; }
        issue("climb:village-up", Proof.LADDER_UP, f, 12000, 0,
            () -> ladder.click(action));
    }

    private void descend(Frame f) {
        stage = "CLIMB_VILLAGE_LADDER_DOWN";
        if (route(f, "VILLAGE_LADDER_UPPER", LADDER_UPPER, 3)) return;
        Rs2TileObjectModel ladder = object(new int[]{16556}, LADDER_UPPER, 5);
        if (ladder == null) { missingScene("village down ladder 16556", 12000); return; }
        String action = objectAction(ladder, "Climb-down", "Climb");
        if (action == null) { missingScene("village down ladder has no climb action", 12000); return; }
        issue("climb:village-down", Proof.LADDER_DOWN, f, 12000, 0,
            () -> ladder.click(action));
    }

    private void talkGeneral(Frame f, String purpose) {
        stage = "TALK_GENERALS_" + purpose;
        if (f.shopOpen) {
            issue("shop:close-wydin", Proof.SHOP_CLOSE, f, 5000, 0,
                () -> { Rs2Shop.closeShop(); return true; });
            return;
        }
        if (f.upstairs()) { descend(f); return; }
        if (route(f, "GENERALS", GENERALS_POINT, 4)) return;
        Rs2NpcModel general = reachableGeneral(f);
        if (general == null) { missingScene("reachable general 669/670", 12000); return; }
        talk(f, "GENERAL_" + purpose, general);
    }

    private void talk(Frame f, String key, Rs2NpcModel npc) {
        if (!key.equals(talkKey)) { talkKey = key; talkAttempts = 0; }
        if (++talkAttempts > 3) {
            hold("Three " + key + " conversations without quest/item progress; varbit="
                + f.varbit + ", dialogue=" + f.dialogue); return;
        }
        log.info("[GoblinDiplomacy] TARGET talk={} npcId={} at={}",
            key, npc.getId(), npc.getWorldLocation());
        issue("talk:" + key, Proof.TALK, f, 9000, npc.getId(),
            () -> npc.click("Talk-to"));
    }

    private boolean dialogue(Frame f) {
        if (!f.inDialogue && f.options.isEmpty() && !f.hasContinue) {
            unknownDialogueSince = 0; return false;
        }
        String[] accepted;
        if (distance(f.pos, WYSON) <= 18 && f.count(WOAD) < 2) {
            accepted = new String[]{"woad leaves", "buy one", "20 coins"};
        } else if (distance(f.pos, AGGIE) <= 12) {
            accepted = new String[]{"can you make dyes", "make dyes for me"};
            if (f.count(RED_DYE) == 0 && f.count(REDBERRIES) >= 3)
                accepted = new String[]{"make me some red dye", "what do you need to make red dye",
                    "can you make dyes", "make dyes for me"};
            else if (f.count(YELLOW_DYE) == 0 && f.count(ONION) >= 2)
                accepted = new String[]{"make me some yellow dye", "what do you need to make yellow dye",
                    "can you make dyes", "make dyes for me"};
            else if (f.count(BLUE_DYE) == 0 && f.count(WOAD) >= 2)
                accepted = new String[]{"make me some blue dye", "what do you need to make blue dye",
                    "can you make dyes", "make dyes for me"};
        } else {
            switch (f.varbit) {
                case 0:
                    accepted = new String[]{"do you want me to pick an armour colour",
                        "what about a different colour", "yes.",
                        "so how is life for the goblins", "yes, wartface looks fat"};
                    break;
                case 1: case 2:
                    accepted = new String[]{"what about a different colour", "yes."};
                    break;
                case 3:
                    // The opening conversation can remain on the generals' help
                    // menu after the quest has already advanced to stage 3.
                    // Exit that exact menu before sourcing dyes and armour.
                    if (f.count(ORANGE_MAIL) == 0
                        && f.options.stream().anyMatch(o -> knownOption(o, "where do i get goblin armour"))
                        && f.options.stream().anyMatch(o -> knownOption(o, "okay, i'll be back soon")))
                        accepted = new String[]{"okay, i'll be back soon"};
                    else
                        accepted = new String[]{"i have some orange armour here"};
                    break;
                case 4: accepted = new String[]{"i have some blue armour here"}; break;
                case 5: accepted = new String[]{"i have some brown armour here"}; break;
                default: accepted = new String[0];
            }
        }
        if (!f.options.isEmpty()) {
            for (String fragment : accepted) for (String option : f.options)
                if (knownOption(option, fragment)) {
                    unknownDialogueSince = 0;
                    if (fragment.equals("20 coins")) lastWysonAt = System.currentTimeMillis();
                    issue("dialogue:" + fragment, Proof.DIALOGUE, f, 7500, 0,
                        () -> Rs2Dialogue.clickOption(option));
                    return true;
                }
            if (unknownDialogueSince == 0) unknownDialogueSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - unknownDialogueSince > 10000)
                hold("Unexpected Goblin Diplomacy dialogue at varbit " + f.varbit
                    + " near " + f.pos + ": " + f.options);
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

    private static boolean knownOption(String option, String fragment) {
        String actual = normalize(option);
        if ("yes.".equals(fragment)) return "yes".equals(actual) || "yes.".equals(actual);
        return actual.contains(fragment);
    }

    /** The installed walker handles obstacles; this script supervises its route and proof. */
    private boolean route(Frame f, String key, WorldPoint target, int radius) {
        if (key.equals("NORTH_CRATE") && f.pos.getPlane() == 0
            && distance(f.pos, NORTH_CRATE) <= 8
            && !f.pos.equals(NORTH_CRATE_ACCESS)) {
            // The crate is one tile beyond the north palisade. The installed
            // walker treats a nearby unwalkable crate tile as arrived even
            // when the fence blocks the Search action. Route to a known
            // walkable tile on the crate side through the east opening.
            return route(f, "NORTH_CRATE_ACCESS", NORTH_CRATE_ACCESS, 0);
        }
        if (key.equals("FRED_ONION_FIELD") && f.pos.getPlane() == 0) {
            // The direct Port Sarim→Fred route tried the nearby members' manhole
            // at (3018,3231) and stalled. These surface waypoints were walked
            // during the earlier X Marks route and avoid that transport.
            if (f.pos.getX() < 3048)
                return route(f, "PORT_SARIM_NORTHEAST", PORT_SARIM_NORTHEAST, 2);
            if (f.pos.getX() < 3100)
                return route(f, "DRAYNOR_JAIL_ROAD", DRAYNOR_JAIL_ROAD, 2);
        }
        if (!key.equals("SAFE_WEST_VARROCK_EXIT") && target.getX() < 3150
            && f.pos.getPlane() == 0 && f.pos.getX() >= 3179 && f.pos.getX() <= 3295
            && f.pos.getY() >= 3390 && f.pos.getY() <= 3490)
            return route(f, "SAFE_WEST_VARROCK_EXIT", WEST_VARROCK_EXIT, 2);
        if (key.equals("PORT_SARIM_NORTHEAST")
            && (escapeTarget != null || portSarimManholeArea(f.pos)))
            return escapePortSarimManhole(f, target);
        if (route != null && route.cancelRequested) {
            Route prior = route;
            if (!cancelRoute()) return true;
            if (prior.failureReason != null) finishRouteFailure(f, prior);
            return true;
        }
        if (route != null && (!route.key.equals(key) || !route.target.equals(target))) {
            route.cancelReason = "target changed to " + key;
            cancelRoute(); return true;
        }
        int distance = distance(f.pos, target);
        if (distance <= radius && arrivalProved(f, key, target, radius)) {
            if (route != null) {
                route.cancelReason = "arrived " + key;
                if (!cancelRoute()) return true;
                failures.remove("walk:" + key);
                nextActionAt = System.currentTimeMillis()
                    + ThreadLocalRandom.current().nextLong(250, 751);
                paceReason = "proved arrival " + key;
                stage = "ARRIVED_" + key;
                return true;
            }
            return false;
        }
        stage = "WALK_" + key;
        if (route == null) {
            Route r = new Route(key, target, radius, f.pos);
            route = r; startRouteSegment(f, r,
                distance <= radius && !arrivalProved(f, key, target, radius));
            log.info("[GoblinDiplomacy] ROUTE start={} from={} target={} radius={}",
                key, f.pos, target, radius);
            return true;
        }
        Route r = route;
        long now = System.currentTimeMillis();
        if (distance < r.bestDistance) { r.bestDistance = distance; r.lastProgress = now; }
        if (!f.pos.equals(r.lastObserved)) {
            r.lastObserved = f.pos;
            if (!key.equals("NORTH_CRATE_ACCESS") || r.visited.add(f.pos))
                r.lastProgress = now;
        }
        if (now - r.started > 480000 || now - r.lastProgress > 25000) {
            routeFailure(f, "route timed out/no progress", r); return true;
        }
        if (r.completed && (r.worker == null || !r.worker.isAlive())
            && now - r.completedAt >= 1500) {
            if (distance(f.pos, r.walkTarget) < r.segmentDistance
                || (key.equals("NORTH_CRATE_ACCESS") && !f.pos.equals(r.segmentStart)))
                startRouteSegment(f, r,
                    distance <= radius && !arrivalProved(f, key, target, radius));
            else routeFailure(f, "walker segment ended without position progress", r);
        }
        return true;
    }

    private boolean handleMemberPrompt() {
        boolean visible = Rs2Widget.isWidgetVisible(278, 0);
        if (!visible) {
            if (memberClosePending) {
                log.info("[GoblinDiplomacy] PROVED members prompt closed after {} click(s)",
                    memberCloseAttempts);
                memberClosePending = false;
                memberCloseAttempts = 0;
                nextActionAt = System.currentTimeMillis() + 300;
                stage = "MEMBER_PROMPT_CLOSED";
                return true;
            }
            return false;
        }
        if (!cancelRoute()) return true;
        long now = System.currentTimeMillis();
        if (memberClosePending && now - memberCloseAt < 4000) {
            stage = "VERIFY_MEMBER_PROMPT_CLOSE"; return true;
        }
        if (memberCloseAttempts >= 2) {
            hold("Members prompt 278 remained visible after two verified close attempts");
            return true;
        }
        Widget close = Rs2Widget.getWidget(278, 3);
        Widget clickable = close;
        Rectangle bounds = close == null ? null : close.getBounds();
        if (bounds == null || bounds.width > 80 || bounds.height > 80) {
            clickable = null;
            if (close != null && close.getDynamicChildren() != null)
                for (Widget child : close.getDynamicChildren()) {
                    Rectangle childBounds = child == null ? null : child.getBounds();
                    if (child != null && !child.isHidden() && childBounds != null
                        && childBounds.width > 0 && childBounds.width <= 80
                        && childBounds.height > 0 && childBounds.height <= 80
                        && (clickable == null || childBounds.x > clickable.getBounds().x))
                        clickable = child;
                }
            bounds = clickable == null ? null : clickable.getBounds();
        }
        if (clickable == null || clickable.isHidden() || bounds == null
            || bounds.width <= 0 || bounds.height <= 0) {
            hold("Members prompt 278 visible, but close widget 278:3 has no small visible bounds");
            return true;
        }
        boolean clicked = Rs2Widget.clickWidget(clickable);
        memberCloseAttempts++;
        memberClosePending = true;
        memberCloseAt = now;
        stage = "VERIFY_MEMBER_PROMPT_CLOSE";
        log.info("[GoblinDiplomacy] MEMBERS_CLOSE attempt={} bounds={} clicked={}",
            memberCloseAttempts, bounds, clicked);
        return true;
    }

    private static boolean portSarimManholeArea(WorldPoint pos) {
        return pos != null && pos.getPlane() == 0
            && pos.getX() >= 3015 && pos.getX() <= 3023
            && pos.getY() >= 3228 && pos.getY() <= 3235;
    }

    /** Bypass a bad transport scan with a local collision-aware WALK click. */
    private boolean escapePortSarimManhole(Frame f, WorldPoint goal) {
        if (route != null) {
            route.cancelReason = "local Port Sarim escape";
            if (!cancelRoute()) return true;
        }
        long now = System.currentTimeMillis();
        if (escapeTarget != null) {
            if (f.pos.getPlane() == escapeFrom.getPlane()
                && f.pos.getX() > escapeFrom.getX()
                && f.pos.getY() >= escapeFrom.getY()
                && distance(f.pos, goal) < distance(escapeFrom, goal)) {
                log.info("[GoblinDiplomacy] PROVED local escape from={} to={} requested={}",
                    escapeFrom, f.pos, escapeTarget);
                escapeFrom = escapeTarget = null;
                nextActionAt = now + ThreadLocalRandom.current().nextLong(300, 701);
                stage = "PROVED_PORT_SARIM_ESCAPE";
                if (!portSarimManholeArea(f.pos)) {
                    escapeAttempts = 0; escapeRejected.clear();
                }
                return true;
            }
            if (now - escapeAt < 8000) {
                stage = "VERIFY_PORT_SARIM_ESCAPE"; return true;
            }
            log.warn("[GoblinDiplomacy] Local escape unproved from={} requested={} actual={}",
                escapeFrom, escapeTarget, f.pos);
            escapeRejected.add(escapeTarget);
            escapeFrom = escapeTarget = null;
        }
        if (!portSarimManholeArea(f.pos)) {
            escapeAttempts = 0; escapeRejected.clear(); return false;
        }
        if (escapeAttempts >= 3) {
            hold("Port Sarim local escape failed after three distinct reachable clicks at " + f.pos);
            return true;
        }
        Map<WorldPoint, Integer> reachable = Rs2Tile.getReachableTilesFromTile(f.pos, 3);
        WorldPoint best = null;
        if (reachable != null) for (Map.Entry<WorldPoint, Integer> entry : reachable.entrySet()) {
            WorldPoint tile = entry.getKey();
            Integer steps = entry.getValue();
            if (tile == null || steps == null || steps < 1 || steps > 3
                || tile.getPlane() != f.pos.getPlane() || tile.getX() <= f.pos.getX()
                || tile.getY() < f.pos.getY() || escapeRejected.contains(tile)
                || distance(tile, goal) >= distance(f.pos, goal)) continue;
            if (best == null || distance(tile, goal) < distance(best, goal)
                || (distance(tile, goal) == distance(best, goal)
                    && tile.getX() > best.getX())) best = tile;
        }
        if (best == null) {
            hold("No eastward collision-reachable tile near Port Sarim manhole from " + f.pos);
            return true;
        }
        boolean clicked = Rs2Walker.walkFastCanvas(best, false);
        escapeAttempts++;
        log.info("[GoblinDiplomacy] LOCAL_ESCAPE attempt={} from={} target={} clicked={}",
            escapeAttempts, f.pos, best, clicked);
        if (!clicked) {
            escapeRejected.add(best);
            stage = "RESELECT_PORT_SARIM_ESCAPE";
            return true;
        }
        escapeFrom = f.pos;
        escapeTarget = best;
        escapeAt = now;
        stage = "VERIFY_PORT_SARIM_ESCAPE";
        return true;
    }

    /** Do not count a tile on the other side of a wall or closed door as arrival. */
    private static boolean arrivalProved(Frame f, String key, WorldPoint target, int radius) {
        if (key.equals("NORTH_CRATE_ACCESS")) return f.pos.equals(NORTH_CRATE_ACCESS);
        Rs2TileObjectModel object = routeObject(key);
        if (object != null) {
            if (key.endsWith("_CRATE")) {
                // One tile from the north crate was the wrong side of a
                // palisade: three visible Search clicks got "I can't reach
                // that!". Require a collision-aware sight line before any
                // crate click; only later mail inventory gain proves success.
                if (distance(f.pos, object.getWorldLocation()) > 1
                    || object.getClickbox() == null) return false;
                if (key.equals("NORTH_CRATE") && f.pos.equals(NORTH_CRATE_ACCESS))
                    return true;
                return Boolean.TRUE.equals(Microbot.getClientThread().invoke(
                    (java.util.function.Supplier<Boolean>) () -> {
                    return f.pos.toWorldArea().hasLineOfSightTo(
                        Microbot.getClient().getTopLevelWorldView(), object.getWorldLocation());
                }));
            }
            WorldPoint approach = objectApproach(object, f.pos);
            Map<WorldPoint, Integer> reachable = Rs2Tile.getReachableTilesFromTile(
                f.pos, Math.max(6, radius + 4));
            return approach != null && reachable != null && reachable.containsKey(approach)
                && f.pos.equals(approach);
        }
        if (key.equals("NORTH_CRATE") || key.equals("WEST_CRATE")
            || key.equals("UPPER_CRATE") || key.startsWith("VILLAGE_LADDER")
            || key.equals("FRED_ONION_FIELD")) return false;
        if (key.equals("SAFE_WEST_VARROCK_EXIT"))
            return f.pos.getX() <= 3177 && f.pos.getY() >= 3426 && f.pos.getY() <= 3432;
        if (key.equals("PORT_SARIM_NORTHEAST") || key.equals("DRAYNOR_JAIL_ROAD"))
            return adjacentReachable(f.pos, target, 6);
        if (key.equals("GENERALS")) return reachableGeneral(f) != null;
        if (key.equals("WYSON") || key.equals("WYDIN") || key.equals("AGGIE")) {
            int[] ids = key.equals("WYSON") ? WYSON_IDS
                : key.equals("WYDIN") ? WYDIN_IDS : AGGIE_IDS;
            Rs2NpcModel npc = npc(ids, target, 18);
            if (npc == null || !npc.hasLineOfSight()) return false;
            return adjacentReachable(f.pos, npc.getWorldLocation(), Math.max(8, radius + 4));
        }
        return Rs2Tile.isTileReachable(target)
            || adjacentReachable(f.pos, target, Math.max(8, radius + 4));
    }

    private static Rs2TileObjectModel routeObject(String key) {
        switch (key) {
            case "NORTH_CRATE": return object(new int[]{16559}, NORTH_CRATE, 6);
            case "WEST_CRATE": return object(new int[]{16560}, WEST_CRATE, 6);
            case "UPPER_CRATE": return object(new int[]{16561}, UPPER_CRATE, 6);
            case "VILLAGE_LADDER_GROUND":
                return object(new int[]{16450}, LADDER_GROUND, 5);
            case "VILLAGE_LADDER_UPPER":
                return object(new int[]{16556}, LADDER_UPPER, 5);
            case "FRED_ONION_FIELD": return object(ONION_IDS, FRED_ONIONS, 17);
            default: return null;
        }
    }

    private static WorldPoint objectApproach(Rs2TileObjectModel object, WorldPoint player) {
        WorldPoint target = object.getWorldLocation();
        WorldPoint approach = Rs2Walker.getPointWithWallDistance(target, player);
        if (approach == null || distance(approach, target) > 1 || !Rs2Tile.isWalkable(approach))
            approach = Rs2Tile.getNearestWalkableTileWithLineOfSight(target);
        if (approach == null || distance(approach, target) > 1 || !Rs2Tile.isWalkable(approach))
            return null;
        return approach;
    }

    private static boolean adjacentReachable(WorldPoint from, WorldPoint target, int radius) {
        Map<WorldPoint, Integer> tiles = Rs2Tile.getReachableTilesFromTile(from, radius);
        if (tiles == null) return false;
        for (WorldPoint tile : tiles.keySet()) if (distance(tile, target) <= 1) return true;
        return false;
    }

    private void startRouteSegment(Frame f, Route r, boolean collisionBlocked) {
        r.segmentStarted = System.currentTimeMillis();
        r.segmentStart = f.pos;
        r.completed = false; r.completedAt = 0;
        WorldPoint walkTarget = r.target;
        int walkRadius = r.radius;
        if (collisionBlocked) {
            walkRadius = 0;
            Rs2TileObjectModel object = routeObject(r.key);
            if (object != null && r.key.endsWith("_CRATE")) {
                // A radius-three walk stopped just outside the palisade.
                // Radius one forces the installed walker to find an adjacent
                // tile on the reachable side, through the compound entrance.
                walkTarget = object.getWorldLocation();
                walkRadius = 1;
            } else if (object != null) walkTarget = objectApproach(object, f.pos);
            else {
                WorldPoint candidate = Rs2Walker.getPointWithWallDistance(r.target, f.pos);
                if (candidate != null && distance(candidate, r.target) <= 1
                    && Rs2Tile.isWalkable(candidate)) walkTarget = candidate;
            }
            if (walkTarget == null) {
                routeFailure(f, "no walkable approach tile", r); return;
            }
        }
        r.walkTarget = walkTarget;
        r.segmentDistance = distance(f.pos, walkTarget);
        final WorldPoint segmentTarget = walkTarget;
        final int segmentRadius = walkRadius;
        log.info("[GoblinDiplomacy] ROUTE_SEGMENT key={} walkTarget={} radius={} collisionBlocked={}",
            r.key, segmentTarget, segmentRadius, collisionBlocked);
        Thread t = new Thread(() -> {
            try {
                Rs2Walker.walkWithStateUntil(segmentTarget, segmentRadius,
                    () -> stopped || Thread.currentThread().isInterrupted()
                        || System.currentTimeMillis() - r.segmentStarted >= 15000);
            } finally {
                r.completedAt = System.currentTimeMillis(); r.completed = true;
            }
        }, "GoblinDiplomacy-route");
        t.setDaemon(true); r.worker = t; t.start();
    }

    private void routeFailure(Frame f, String reason, Route r) {
        r.failureReason = reason; r.cancelReason = "failure " + reason;
        if (!cancelRoute()) return;
        finishRouteFailure(f, r);
    }

    private void finishRouteFailure(Frame f, Route r) {
        int count = failures.merge("walk:" + r.key, 1, Integer::sum);
        log.warn("[GoblinDiplomacy] ROUTE failure={} attempt={}/3 at={} target={} bestDist={}",
            r.failureReason, count, f.pos, r.target, r.bestDistance);
        if (count >= 3) hold("Route " + r.key + " failed after " + count
            + " attempts: " + r.failureReason + "; at=" + f.pos + ", target=" + r.target);
        else Rs2Walker.recalculatePath();
    }

    private boolean cancelRoute() {
        Route r = route;
        if (r == null) return true;
        if (!r.cancelRequested) {
            r.cancelRequested = true; r.cancelAt = System.currentTimeMillis();
            if (r.cancelReason == null) r.cancelReason = "state gate";
            if (r.worker != null && r.worker.isAlive()) r.worker.interrupt();
            Thread clear = new Thread(() -> {
                try { Rs2Walker.clearWalkingRoute("goblindiplomacy:" + r.cancelReason); }
                catch (Exception ex) { r.clearError = ex.toString();
                    log.error("[GoblinDiplomacy] route clear failed", ex); }
            }, "GoblinDiplomacy-route-clear");
            clear.setDaemon(true); r.clearWorker = clear; clear.start();
            log.info("[GoblinDiplomacy] ROUTE_CANCEL reason={} target={}", r.cancelReason, r.target);
        }
        if ((r.worker != null && r.worker.isAlive())
            || (r.clearWorker != null && r.clearWorker.isAlive())) {
            if (System.currentTimeMillis() - r.cancelAt > 60000 && error.isEmpty()) {
                error = "Walker still active 60s after cancel; target=" + r.target;
                stage = "HOLD"; log.error("[GoblinDiplomacy] HOLD {}", error);
            } else if (error.isEmpty()) stage = "WAIT_ROUTE_STOP";
            return false;
        }
        route = null;
        if (r.clearError != null && error.isEmpty()) {
            error = "Walker clear failed for " + r.target + ": " + r.clearError;
            stage = "HOLD"; log.error("[GoblinDiplomacy] HOLD {}", error);
            return false;
        }
        return true;
    }

    private boolean safety(Frame f) {
        stage = "LOW_HEALTH";
        List<Rs2ItemModel> food = Rs2Inventory.getInventoryFood();
        if (food == null || food.isEmpty()) {
            hold("Health " + f.health + "% with no food in inventory"); return true;
        }
        if (!cancelRoute()) return true;
        int id = food.get(0).getId();
        issue("eat:" + id, Proof.EAT, f, 5000, id,
            () -> Rs2Inventory.interact(id, "Eat"));
        return true;
    }

    private static Rs2NpcModel npc(int[] ids, WorldPoint point, int radius) {
        return Microbot.getRs2NpcCache().query().withIds(ids)
            .within(point, radius).nearestOnClientThread();
    }

    private static Rs2TileObjectModel object(int[] ids, WorldPoint point, int radius) {
        return Microbot.getRs2TileObjectCache().query().withIds(ids)
            .within(point, radius).nearestOnClientThread();
    }

    private static String objectAction(Rs2TileObjectModel object, String... choices) {
        return Microbot.getClientThread().invoke(() -> {
            ObjectComposition composition = object.getObjectComposition();
            if (composition == null || composition.getActions() == null) return null;
            for (String choice : choices) for (String action : composition.getActions())
                if (action != null && action.equalsIgnoreCase(choice)) return action;
            return null;
        });
    }

    /** Vary only between the two equivalent generals with reachable adjacent tiles. */
    private static Rs2NpcModel reachableGeneral(Frame f) {
        List<Rs2NpcModel> candidates = Microbot.getRs2NpcCache().query()
            .withIds(GENERALS).within(GENERALS_POINT, 9).toListOnClientThread();
        Map<WorldPoint, Integer> reachable = Rs2Tile.getReachableTilesFromTile(f.pos, 10);
        List<Rs2NpcModel> eligible = new ArrayList<>();
        if (reachable != null) for (Rs2NpcModel candidate : candidates) {
            if (candidate == null || !candidate.hasLineOfSight()
                || distance(f.pos, candidate.getWorldLocation()) > 7) continue;
            for (WorldPoint tile : reachable.keySet()) {
                if (distance(tile, candidate.getWorldLocation()) <= 1) {
                    eligible.add(candidate); break;
                }
            }
        }
        if (eligible.isEmpty()) return null;
        return eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
    }

    private void missingScene(String key, long timeout) {
        if (!key.equals(missingKey)) { missingKey = key; missingSince = System.currentTimeMillis(); }
        if (System.currentTimeMillis() - missingSince > timeout)
            hold("Target absent from loaded scene: " + key);
        else stage = "WAIT_SCENE_" + key;
    }

    private void hold(String reason) {
        if (error.isEmpty()) log.error("[GoblinDiplomacy] HOLD {}", reason);
        error = reason; stage = "HOLD"; paceReason = ""; cancelRoute();
    }

    private static int distance(WorldPoint a, WorldPoint b) {
        if (a == null || b == null || a.getPlane() != b.getPlane()) return Integer.MAX_VALUE;
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("<[^>]*>", "")
            .replace('\u2019', '\'').trim().toLowerCase();
    }

    private static String loadedClassSha256() {
        try (InputStream in = GoblinDiplomacyScript.class.getResourceAsStream(
            "GoblinDiplomacyScript.class")) {
            if (in == null) return "UNKNOWN";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) >= 0) if (count > 0) digest.update(buffer, 0, count);
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest.digest()) {
                int unsigned = value & 0xff;
                hex.append(Character.forDigit(unsigned >>> 4, 16));
                hex.append(Character.forDigit(unsigned & 15, 16));
            }
            return hex.toString();
        } catch (Exception ex) {
            log.warn("[GoblinDiplomacy] class SHA-256 unavailable: {}", ex.toString());
            return "UNKNOWN";
        }
    }

    private void writeStatus(Frame f) {
        try {
            Files.createDirectories(STATUS_DIR);
            Properties p = new Properties();
            p.setProperty("build", Integer.toString(BUILD_NUMBER));
            p.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
            p.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
            p.setProperty("questVarbit", f == null ? "-1" : Integer.toString(f.varbit));
            p.setProperty("questState", f == null ? "UNKNOWN" : f.questState);
            p.setProperty("stage", stage);
            p.setProperty("position", f == null ? "UNKNOWN" : String.valueOf(f.pos));
            p.setProperty("currentWorld", f == null ? "0" : Integer.toString(f.world));
            p.setProperty("gameState", f == null ? "UNKNOWN" : f.gameState);
            p.setProperty("inventoryLoaded", f == null ? "false" : Boolean.toString(f.inventoryLoaded));
            p.setProperty("freeSlots", f == null ? "0" : Integer.toString(f.freeSlots));
            p.setProperty("error", error);
            p.setProperty("goblinMail", f == null ? "0" : Integer.toString(f.count(MAIL)));
            p.setProperty("orangeMail", f == null ? "0" : Integer.toString(f.count(ORANGE_MAIL)));
            p.setProperty("blueMail", f == null ? "0" : Integer.toString(f.count(BLUE_MAIL)));
            p.setProperty("redberries", f == null ? "0" : Integer.toString(f.count(REDBERRIES)));
            p.setProperty("onions", f == null ? "0" : Integer.toString(f.count(ONION)));
            p.setProperty("woadLeaves", f == null ? "0" : Integer.toString(f.count(WOAD)));
            p.setProperty("coins", f == null ? "0" : Integer.toString(f.count(COINS)));
            p.setProperty("northCrate", f == null ? "-1" : Integer.toString(f.northFlag));
            p.setProperty("westCrate", f == null ? "-1" : Integer.toString(f.westFlag));
            p.setProperty("upperCrate", f == null ? "-1" : Integer.toString(f.upperFlag));
            p.setProperty("healthPercent", f == null ? "-1" : Double.toString(f.health));
            p.setProperty("sha256", classSha256);
            p.setProperty("pending", pending == null ? "" : pending.key);
            p.setProperty("walkTarget", route == null ? "" : String.valueOf(route.target));
            p.setProperty("paceRemainingMs", Long.toString(Math.max(0,
                nextActionAt - System.currentTimeMillis())));
            p.setProperty("paceReason", paceReason);
            Path temp = STATUS_DIR.resolve("status.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) {
                p.store(out, "Goblin Diplomacy live script");
            }
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
        } catch (Exception ex) { log.warn("[GoblinDiplomacy] status write: {}", ex.toString()); }
    }
}
