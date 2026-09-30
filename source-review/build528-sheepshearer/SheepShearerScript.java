package net.runelite.client.plugins.microbot.sheepshearer;

import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPCComposition;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.input.InputArbiter;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** QuestHelper's installed varp-179 route. Every action needs a later observation. */
public class SheepShearerScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(SheepShearerScript.class);
    public static final int BUILD_NUMBER = 528;
    private static final int QUEST_VARP = 179, SHEARS = 1735, WOOL = 1737, BALL = 1759;
    private static final WorldPoint FRED = new WorldPoint(3190, 3273, 0);
    private static final WorldPoint SHEEP_FIELD = new WorldPoint(3201, 3268, 0);
    private static final WorldPoint STAIRS_GROUND = new WorldPoint(3204, 3207, 0);
    private static final WorldPoint STAIRS_FIRST = new WorldPoint(3204, 3207, 1);
    private static final WorldPoint WHEEL = new WorldPoint(3209, 3212, 1);
    private static final WorldPoint WHEEL_ROOM_FLOOR = new WorldPoint(3210, 3212, 1);
    private static final WorldPoint DRAYNOR_BANK = new WorldPoint(3092, 3245, 0);
    private static final Path STATUS_DIR = Paths.get(System.getProperty("user.home"), ".runelite", "sheepshearer");
    private static final int[] SHEEP_IDS = {2786, 2699, 2787, 2693, 2694, 2695};

    private enum Proof { OVERLAY, DIALOGUE, TALK, SHEARS, SHEAR, STAIR_UP, STAIR_DOWN,
        SPIN_OPEN, SPIN_PROGRESS, TURN_IN, BANK_OPEN, BANK_DEPOSIT, BANK_CLOSED, WALK }

    private static final class Frame {
        String gameState = "NO_CLIENT", questState = "UNKNOWN", dialogue = "";
        WorldPoint pos;
        int world, varp = -1, shears, wool, balls, emptySlots, animation = -1;
        boolean overlay, production, bankOpen, moving, inDialogue, hasContinue;
        List<String> options = new ArrayList<>();
        boolean loggedIn() { return "LOGGED_IN".equals(gameState) && pos != null; }
        boolean finished() { return "FINISHED".equals(questState); }
        int plane() { return pos == null ? -1 : pos.getPlane(); }
    }

    private static final class Pending {
        final String key;
        final Proof proof;
        final Frame before;
        final WorldPoint target;
        final int radius;
        final long at, timeout;
        Pending(String key, Proof proof, Frame before, WorldPoint target, int radius, long timeout) {
            this.key = key; this.proof = proof; this.before = before; this.target = target;
            this.radius = radius; this.timeout = timeout; this.at = System.currentTimeMillis();
        }
    }

    private volatile boolean stopped;
    private volatile String stage = "STARTING", error = "";
    private BooleanSupplier ownsInput;
    private Pending pending;
    private final Map<String, Integer> failures = new HashMap<>();
    private final Set<String> failedSheep = new HashSet<>();
    private String shearTarget = "";
    private long nextActionAt;
    private String paceReason = "";
    private String missingKey = "";
    private long missingSince, unknownDialogueSince, lastSpinProgressAt;
    private int lastWool = -1, lastBalls = -1;
    private boolean spinning;
    private boolean turnInExpected;
    private int turnInVarp;
    private long turnInChatClosedAt;

    public boolean run(SheepShearerConfig config, BooleanSupplier exclusiveInput) {
        if (isRunning()) return true;
        stopped = false;
        ownsInput = exclusiveInput;
        nextActionAt = 0;
        paceReason = "";
        failedSheep.clear();
        shearTarget = "";
        log.info("[SheepShearer] RUNNING_BUILD={} quest=SHEEP_SHEARER", BUILD_NUMBER);
        long delay = Math.max(450, Math.min(2000, config.tickDelay()));
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::tick, 0, delay, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override public void shutdown() {
        stopped = true;
        if (mainScheduledFuture != null) mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow();
        super.shutdown();
        try { Files.deleteIfExists(STATUS_DIR.resolve("status.properties")); }
        catch (Exception ex) { log.warn("[SheepShearer] status cleanup: {}", ex.toString()); }
    }

    private void tick() {
        if (stopped || Thread.currentThread().isInterrupted()) return;
        Frame f = null;
        try {
            f = observe();
            if (!f.loggedIn()) { stage = "WAIT_LOGIN"; pending = null; return; }
            if (!ownsInput.getAsBoolean()) { stage = "WAIT_EXCLUSIVE"; pending = null; return; }
            if (Microbot.pauseAllScripts.get() || InputArbiter.isHuman()) { stage = "WAIT_INPUT"; return; }
            if (Microbot.getBlockingEventManager().shouldBlockAndProcess()) { stage = "WAIT_BLOCKING_EVENT"; return; }
            if (f.finished()) { stage = "DONE"; pending = null; return; }
            if (!error.isEmpty()) { stage = "HOLD"; return; }
            if (pending != null) { verifyPending(f); return; }
            long paceRemaining = nextActionAt - System.currentTimeMillis();
            if (paceRemaining > 0) { stage = "WAIT_PACE"; return; }
            paceReason = "";
            if (f.overlay) {
                stage = "DISMISS_PRIOR_QUEST_SCROLL";
                issue("dismiss:quest-scroll", Proof.OVERLAY, f, null, 0, 3000,
                    () -> { Rs2Keyboard.keyPress(27); return true; });
                return;
            }
            if (f.questState.equals("UNKNOWN") || f.varp < 0 || f.varp > 21) {
                hold("Unknown quest state/varp: " + f.questState + "/" + f.varp); return;
            }
            if (f.varp >= 21) { hold("Varp 179 reached " + f.varp + " but quest state is not FINISHED"); return; }
            if (f.varp == 0) { startQuest(f); return; }
            progress(f);
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted()) return;
            hold("Tick exception: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            log.error("[SheepShearer] tick failed", ex);
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
            s.animation = c.getLocalPlayer().getAnimation();
            s.varp = c.getVarpValue(QUEST_VARP);
            QuestState qs = Quest.SHEEP_SHEARER.getState(c);
            s.questState = qs == null ? "UNKNOWN" : qs.name();
            Widget questScroll = c.getWidget(WidgetInfo.QUEST_COMPLETED);
            s.overlay = questScroll != null && !questScroll.isHidden();
            Widget production = c.getWidget(270, 14);
            Widget alternateProduction = c.getWidget(300, 16);
            s.production = production != null && !production.isHidden()
                || alternateProduction != null && !alternateProduction.isHidden();
            ItemContainer inv = c.getItemContainer(InventoryID.INVENTORY);
            if (inv != null) for (Item item : inv.getItems()) {
                if (item == null || item.getId() < 0) continue;
                if (item.getId() == SHEARS) s.shears += item.getQuantity();
                if (item.getId() == WOOL) s.wool += item.getQuantity();
                if (item.getId() == BALL) s.balls += item.getQuantity();
            }
            return s;
        });
        if (f.loggedIn()) {
            // ItemContainer#getItems can contain only populated entries. Use
            // Microbot's 28-slot inventory count for the routing decision.
            f.emptySlots = Rs2Inventory.emptySlotCount();
            f.moving = Rs2Player.isMoving();
            f.bankOpen = Rs2Bank.isOpen();
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
            log.info("[SheepShearer] PROVED action={} varp={} wool={} balls={} pos={}",
                p.key, f.varp, f.wool, f.balls, f.pos);
            if (p.proof == Proof.SPIN_PROGRESS) { spinning = true; lastSpinProgressAt = System.currentTimeMillis(); }
            if (p.proof == Proof.SHEAR) { failedSheep.clear(); shearTarget = ""; }
            schedulePace(p, f);
            failures.remove(p.key); pending = null; missingKey = ""; missingSince = 0;
            return; // Never issue a second action on the proof tick.
        }
        if (System.currentTimeMillis() - p.at < p.timeout) return;
        int count = failures.merge(p.key, 1, Integer::sum);
        pending = null;
        if (p.proof == Proof.SHEAR && !shearTarget.isEmpty()) {
            failedSheep.add(shearTarget);
            log.warn("[SheepShearer] SHEEP_EXCLUDED target={} unproved={}/3", shearTarget, count);
            shearTarget = "";
        }
        if (p.proof == Proof.WALK) Rs2Walker.setTarget(null);
        if (p.proof == Proof.TURN_IN && f.balls < p.before.balls)
            hold("Balls left inventory but varp did not advance and quest is not FINISHED");
        else if (count >= 3)
            hold("Unproved " + p.key + " after " + count + " attempts; varp=" + f.varp
                + ", wool=" + f.wool + ", balls=" + f.balls + ", pos=" + f.pos + ", dialogue=" + f.dialogue);
        else log.warn("[SheepShearer] RETRY action={} failure={}/3", p.key, count);
    }

    private void schedulePace(Pending p, Frame f) {
        long min = 200, max = 700;
        if (p.proof == Proof.DIALOGUE || p.proof == Proof.TALK) { min = 100; max = 400; }
        if (p.proof == Proof.WALK || p.proof == Proof.STAIR_UP || p.proof == Proof.STAIR_DOWN
            || f.varp != p.before.varp) { min = 500; max = 1200; }
        long delay = ThreadLocalRandom.current().nextLong(min, max + 1);
        nextActionAt = System.currentTimeMillis() + delay;
        paceReason = "proved " + p.key;
        log.info("[SheepShearer] PACE action={} delayMs={}", p.key, delay);
    }

    private boolean proved(Pending p, Frame f) {
        switch (p.proof) {
            case OVERLAY: return !f.overlay;
            case TALK: case DIALOGUE: return f.varp != p.before.varp || f.inDialogue != p.before.inDialogue
                || f.hasContinue != p.before.hasContinue || !f.dialogue.equals(p.before.dialogue);
            case SHEARS: return f.shears > p.before.shears;
            case SHEAR: return f.wool > p.before.wool;
            case STAIR_UP: return f.plane() == 1;
            case STAIR_DOWN: return f.plane() == 0;
            case SPIN_OPEN: return f.production;
            case SPIN_PROGRESS: return f.balls > p.before.balls && f.wool < p.before.wool;
            case TURN_IN: return f.finished() || f.varp > p.before.varp;
            case BANK_OPEN: return f.bankOpen;
            case BANK_DEPOSIT: return f.emptySlots > p.before.emptySlots;
            case BANK_CLOSED: return !f.bankOpen;
            case WALK: return f.pos != null && p.before.pos != null &&
                (distance(f.pos, p.target) <= p.radius
                    || distance(f.pos, p.target) < distance(p.before.pos, p.target));
            default: return false;
        }
    }

    private void issue(String key, Proof proof, Frame f, WorldPoint target, int radius,
                       long timeout, BooleanSupplier action) {
        boolean accepted = action.getAsBoolean();
        pending = new Pending(key, proof, f, target, radius, timeout);
        log.info("[SheepShearer] ACTION key={} accepted={} varp={} wool={} balls={} pos={} target={}",
            key, accepted, f.varp, f.wool, f.balls, f.pos, target);
    }

    private boolean travel(Frame f, String name, WorldPoint target, int radius) {
        if (distance(f.pos, target) <= radius) return false;
        stage = "WALK_" + name;
        if (!f.moving) issue("walk:" + name, Proof.WALK, f, target, radius, 12000,
            () -> Rs2Walker.walkTo(target, radius));
        return true;
    }

    private void startQuest(Frame f) {
        stage = "START_FRED";
        if (dialogue(f, "i'm looking for a quest", "yes, okay", "yes.")) return;
        if (travel(f, "FRED", FRED, 4)) return;
        Rs2NpcModel fred = npc(732, FRED, 8);
        if (fred == null) { missingScene("Fred NPC 732"); return; }
        issue("talk:fred-start", Proof.TALK, f, FRED, 0, 9000, () -> fred.click("Talk-to"));
    }

    private void progress(Frame f) {
        int needed = 21 - f.varp; // Installed QuestHelper: partial deposits increment varp.
        if (needed < 1 || needed > 20) { hold("Invalid remaining balls from varp " + f.varp); return; }
        // A Fred conversation can span the 0 -> 1 transition. It owns input
        // until fully closed; inventory routing must not click through it.
        if (dialogue(f, "yes, okay", "yes.", "i need to talk to you about shearing these sheep",
            "i have some")) return;
        if (turnInExpected) {
            if (f.varp > turnInVarp) {
                turnInExpected = false; turnInChatClosedAt = 0;
                return; // Re-evaluate required quantity on the next tick.
            }
            stage = "VERIFY_PARTIAL_TURN_IN";
            if (turnInChatClosedAt == 0) turnInChatClosedAt = System.currentTimeMillis();
            if (System.currentTimeMillis() - turnInChatClosedAt > 20000)
                hold("Fred dialogue closed but varp did not advance after 20s; prior varp="
                    + turnInVarp + ", current=" + f.varp + ", balls=" + f.balls);
            return;
        }
        if (f.bankOpen && f.emptySlots == 0 && f.wool == 0 && f.balls == 0) {
            freeSlots(f); return;
        }
        if (f.bankOpen) {
            stage = "CLOSE_BANK";
            issue("close:bank", Proof.BANK_CLOSED, f, null, 0, 5000,
                () -> { Rs2Keyboard.keyPress(27); return true; });
            return;
        }
        if (f.balls >= needed || f.balls > 0 && f.emptySlots == 0 && f.wool == 0) {
            turnIn(f); return;
        }
        if (f.wool > 0 && (f.wool + f.balls >= needed || f.emptySlots == 0 || spinning)) {
            spin(f); return;
        }
        if (f.emptySlots == 0) { freeSlots(f); return; }
        if (f.shears == 0) { getShears(f); return; }
        shear(f);
    }

    private void getShears(Frame f) {
        stage = "GET_SHEARS";
        if (travel(f, "FRED_SHEARS", FRED, 3)) return;
        if (!Rs2GroundItem.exists(SHEARS, 8)) { missingScene("ground shears 1735 in Fred's house"); return; }
        issue("take:shears", Proof.SHEARS, f, FRED, 0, 9000, () -> Rs2GroundItem.take(SHEARS));
    }

    private void shear(Frame f) {
        stage = "SHEAR_SHEEP";
        if (travel(f, "SHEEP_FIELD", SHEEP_FIELD, 2)) return;
        Rs2NpcModel sheep = Microbot.getClientThread().invoke(() -> {
            Rs2NpcModel nearest = Microbot.getRs2NpcCache().query().withIds(SHEEP_IDS)
                .within(SHEEP_FIELD, 14)
                .where(n -> canShear(n) && !failedSheep.contains(sheepKey(n)))
                .nearestReachable();
            if (nearest == null) return null;
            // Occasionally choose another nearby reachable sheep; never repeat
            // a failed target without a verified wool increase.
            if (ThreadLocalRandom.current().nextInt(3) != 0) return nearest;
            String nearestKey = sheepKey(nearest);
            Rs2NpcModel alternate = Microbot.getRs2NpcCache().query().withIds(SHEEP_IDS)
                .within(SHEEP_FIELD, 14)
                .where(n -> canShear(n) && !failedSheep.contains(sheepKey(n))
                    && !nearestKey.equals(sheepKey(n))).nearestReachable();
            return alternate != null && distance(f.pos, alternate.getWorldLocation())
                <= distance(f.pos, nearest.getWorldLocation()) + 3 ? alternate : nearest;
        });
        if (sheep == null) {
            // Shorn sheep need time to regrow. Reobserve the live flock without
            // issuing clicks, then hold if no shearable NPC appears.
            missingScene("reachable untried sheep with Shear action near 3201,3268", 75000);
            return;
        }
        shearTarget = sheepKey(sheep);
        log.info("[SheepShearer] SHEEP_SELECTED target={} pos={} excluded={}",
            shearTarget, sheep.getWorldLocation(), failedSheep.size());
        issue("shear:sheep", Proof.SHEAR, f, sheep.getWorldLocation(), 0, 12000,
            () -> sheep.click("Shear"));
    }

    private static String sheepKey(Rs2NpcModel sheep) {
        return sheep.getId() + ":" + sheep.getIndex();
    }

    private static boolean canShear(Rs2NpcModel sheep) {
        if (sheep == null || sheep.getNpc() == null) return false;
        NPCComposition composition = sheep.getNpc().getComposition();
        if (composition == null || composition.getActions() == null) return false;
        for (String action : composition.getActions()) if ("Shear".equalsIgnoreCase(action)) return true;
        return false;
    }

    private void spin(Frame f) {
        stage = "SPIN_WOOL";
        if (f.wool == 0) { spinning = false; return; }
        if (lastWool != f.wool || lastBalls != f.balls) {
            lastWool = f.wool; lastBalls = f.balls; lastSpinProgressAt = System.currentTimeMillis();
        }
        if (spinning) {
            if (System.currentTimeMillis() - lastSpinProgressAt > 15000 && f.animation == 894) {
                hold("Spinning animation continued 15s without wool/ball inventory progress"); return;
            }
            if (f.animation == 894 || System.currentTimeMillis() - lastSpinProgressAt < 8000) return;
            spinning = false; // A stopped batch may be restarted only after observed silence.
        }
        if (f.plane() == 0) {
            if (travel(f, "CASTLE_STAIRS_UP", STAIRS_GROUND, 4)) return;
            Rs2TileObjectModel stairs = object(56230, STAIRS_GROUND, 6);
            if (stairs == null) { missingScene("castle staircase up 56230"); return; }
            issue("climb:castle-up", Proof.STAIR_UP, f, STAIRS_GROUND, 0, 12000,
                () -> stairs.click("Climb-up"));
            return;
        }
        if (f.plane() != 1) { hold("Unexpected castle plane " + f.plane()); return; }
        // The wheel's object tile is sealed. Route to a floor tile in its room,
        // but keep the production interface open after Spin automatically moves
        // us to another valid tile beside the wheel.
        boolean insideWheelRoom = f.pos.getX() >= 3209 && f.pos.getX() <= 3211
            && f.pos.getY() >= 3212 && f.pos.getY() <= 3214;
        if (!f.production && !insideWheelRoom
            && travel(f, "WHEEL_ROOM_FLOOR", WHEEL_ROOM_FLOOR, 0)) return;
        if (!f.production) {
            Rs2TileObjectModel wheel = object(14889, WHEEL, 6);
            if (wheel == null) { missingScene("spinning wheel 14889"); return; }
            issue("open:wheel", Proof.SPIN_OPEN, f, WHEEL, 0, 10000, () -> wheel.click("Spin"));
            return;
        }
        issue("make:ball-of-wool", Proof.SPIN_PROGRESS, f, null, 0, 12000,
            () -> Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>) () -> {
                Widget product = findProduct(Microbot.getClient());
                if (product == null || product.isHidden() || product.getBounds() == null) return false;
                return Rs2Widget.clickWidget(product);
            }));
    }

    private static Widget findProduct(Client c) {
        if (c == null) return null;
        Widget product = findProduct(c.getWidget(270, 14), 0);
        return product != null ? product : findProduct(c.getWidget(300, 16), 0);
    }

    private static Widget findProduct(Widget widget, int depth) {
        if (widget == null || widget.isHidden() || depth > 8) return null;
        if (widget.getItemId() == BALL) return widget;
        Widget[] children = widget.getChildren();
        if (children != null) for (Widget child : children) {
            Widget found = findProduct(child, depth + 1);
            if (found != null) return found;
        }
        Widget[] dynamic = widget.getDynamicChildren();
        if (dynamic != null) for (Widget child : dynamic) {
            Widget found = findProduct(child, depth + 1);
            if (found != null) return found;
        }
        Widget[] staticChildren = widget.getStaticChildren();
        if (staticChildren != null) for (Widget child : staticChildren) {
            Widget found = findProduct(child, depth + 1);
            if (found != null) return found;
        }
        return null;
    }

    private void turnIn(Frame f) {
        stage = "TURN_IN_BALLS";
        spinning = false;
        if (dialogue(f, "i need to talk to you about shearing these sheep", "i have some", "yes")) return;
        if (f.plane() == 1) {
            // The full walker can livelock on a near staircase path segment.
            // Click the live staircase once and prove the plane change instead.
            Rs2TileObjectModel stairs = object(16672, STAIRS_FIRST, 8);
            if (stairs == null) { missingScene("castle staircase down 16672"); return; }
            issue("climb:castle-down", Proof.STAIR_DOWN, f, STAIRS_FIRST, 0, 12000,
                () -> stairs.click("Climb-down"));
            return;
        }
        if (f.plane() != 0) { hold("Unexpected return plane " + f.plane()); return; }
        if (travel(f, "FRED_TURN_IN", FRED, 4)) return;
        Rs2NpcModel fred = npc(732, FRED, 8);
        if (fred == null) { missingScene("Fred NPC 732 for turn-in"); return; }
        issue("talk:fred-turn-in", Proof.TALK, f, FRED, 0, 9000, () -> fred.click("Talk-to"));
    }

    private void freeSlots(Frame f) {
        stage = "FREE_INVENTORY_SLOTS";
        if (!f.bankOpen) {
            if (travel(f, "DRAYNOR_BANK", DRAYNOR_BANK, 5)) return;
            issue("open:bank", Proof.BANK_OPEN, f, DRAYNOR_BANK, 0, 9000, Rs2Bank::openBank);
            return;
        }
        issue("deposit:nonquest-inventory", Proof.BANK_DEPOSIT, f, null, 0, 8000,
            () -> Rs2Bank.depositAllExcept(SHEARS, WOOL, BALL));
    }

    /** A visible dialogue owns the tick; unexpected choices stop with diagnostics. */
    private boolean dialogue(Frame f, String... allowedFragments) {
        if (!f.inDialogue && f.options.isEmpty() && !f.hasContinue) {
            unknownDialogueSince = 0; return false;
        }
        if (!f.options.isEmpty()) {
            for (String option : f.options) for (String allowed : allowedFragments) {
                if (normalize(option).contains(allowed)) {
                    unknownDialogueSince = 0;
                    if (normalize(option).contains("i need to talk to you about shearing these sheep")
                        || normalize(option).contains("i have some")) {
                        turnInExpected = true;
                        turnInVarp = f.varp;
                        turnInChatClosedAt = 0;
                    }
                    issue("dialogue:" + allowed, Proof.DIALOGUE, f, null, 0, 7500,
                        () -> Rs2Dialogue.clickOption(option));
                    return true;
                }
            }
            if (unknownDialogueSince == 0) unknownDialogueSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - unknownDialogueSince > 10000)
                hold("Unexpected dialogue choices: " + f.options);
            return true;
        }
        if (f.hasContinue) {
            unknownDialogueSince = 0;
            issue("dialogue:continue", Proof.DIALOGUE, f, null, 0, 7500,
                () -> { Rs2Dialogue.clickContinue(); return true; });
            return true;
        }
        if (unknownDialogueSince == 0) unknownDialogueSince = System.currentTimeMillis();
        if (System.currentTimeMillis() - unknownDialogueSince > 20000)
            hold("Dialogue lacks a known option or Continue: " + f.dialogue);
        return true;
    }

    private static Rs2NpcModel npc(int id, WorldPoint point, int radius) {
        return Microbot.getRs2NpcCache().query().withId(id).within(point, radius).nearestOnClientThread();
    }

    private static Rs2TileObjectModel object(int id, WorldPoint point, int radius) {
        return Microbot.getRs2TileObjectCache().query().withId(id).within(point, radius).nearestOnClientThread();
    }

    private void missingScene(String target) { missingScene(target, 12000); }

    private void missingScene(String target, long timeoutMs) {
        if (!target.equals(missingKey)) { missingKey = target; missingSince = 0; }
        if (missingSince == 0) missingSince = System.currentTimeMillis();
        if (System.currentTimeMillis() - missingSince > timeoutMs) hold("Target absent from loaded scene: " + target);
    }

    private void hold(String reason) {
        if (error.isEmpty()) log.error("[SheepShearer] HOLD {}", reason);
        error = reason; stage = "HOLD"; paceReason = "";
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
            p.setProperty("paceRemainingMs", Long.toString(Math.max(0,
                nextActionAt - System.currentTimeMillis())));
            p.setProperty("paceReason", paceReason);
            p.setProperty("sheepTarget", shearTarget);
            p.setProperty("sheepExcluded", Integer.toString(failedSheep.size()));
            p.setProperty("questVarp", f == null ? "-1" : Integer.toString(f.varp));
            p.setProperty("questState", f == null ? "UNKNOWN" : f.questState);
            p.setProperty("wool", f == null ? "0" : Integer.toString(f.wool));
            p.setProperty("balls", f == null ? "0" : Integer.toString(f.balls));
            p.setProperty("shears", f == null ? "0" : Integer.toString(f.shears));
            p.setProperty("emptySlots", f == null ? "0" : Integer.toString(f.emptySlots));
            p.setProperty("error", error);
            Path temp = STATUS_DIR.resolve("status.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "Sheep Shearer live script"); }
            try { Files.move(temp, STATUS_DIR.resolve("status.properties"),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, STATUS_DIR.resolve("status.properties"),
                StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception ex) { log.warn("[SheepShearer] status write: {}", ex.toString()); }
    }
}
