package net.runelite.client.plugins.microbot.romeojuliet;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
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
import net.runelite.api.ObjectComposition;
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
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Installed Quest Helper's varp-144 route; every action needs later proof. */
public class RomeoJulietScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(RomeoJulietScript.class);
    public static final int BUILD_NUMBER = 536;
    private static final int QUEST_VARP = 144;
    private static final int BERRIES = 753, MESSAGE = 755, POTION = 756;
    private static final int[] ROMEO_IDS = {5037}, JULIET_IDS = {5035};
    private static final int[] LAWRENCE_IDS = {5038}, APOTHECARY_IDS = {5036};
    private static final int[] BUSH_IDS = {23625, 23626, 23627};
    private static final WorldPoint ROMEO = new WorldPoint(3211, 3422, 0);
    private static final WorldPoint JULIET = new WorldPoint(3158, 3427, 1);
    private static final WorldPoint JULIET_STAIRS_GROUND = new WorldPoint(3157, 3436, 0);
    private static final WorldPoint JULIET_STAIRS_APPROACH = new WorldPoint(3159, 3436, 0);
    private static final WorldPoint JULIET_STAIRS_UPPER = new WorldPoint(3156, 3435, 1);
    private static final WorldPoint LAWRENCE = new WorldPoint(3254, 3483, 0);
    private static final WorldPoint APOTHECARY = new WorldPoint(3195, 3405, 0);
    private static final WorldPoint BERRY_BUSHES = new WorldPoint(3266, 3374, 0);
    private static final Path STATUS_DIR = Paths.get(System.getProperty("user.home"), ".runelite", "romeojuliet");

    private enum Proof { TALK, DIALOGUE, STAIRS_UP, STAIRS_DOWN, PICK_BERRIES, EAT }

    private static final class Frame {
        String gameState = "NO_CLIENT", questState = "UNKNOWN", dialogue = "";
        WorldPoint pos;
        int world, varp = -1;
        boolean inDialogue, hasContinue, inventoryLoaded, startPrompt;
        double health = -1;
        final Map<Integer, Integer> items = new HashMap<>();
        final List<String> options = new ArrayList<>();
        int count(int id) { return items.getOrDefault(id, 0); }
        boolean loggedIn() { return "LOGGED_IN".equals(gameState) && pos != null; }
        boolean finished() { return "FINISHED".equals(questState); }
        boolean julietUpper() { return pos != null && pos.getPlane() == 1
            && pos.getX() >= 3147 && pos.getX() <= 3166
            && pos.getY() >= 3425 && pos.getY() <= 3443; }
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
        boolean cancelRequested;
        long cancelAt;
        String cancelReason, failureReason;
        Thread clearWorker;
        volatile String clearError;
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
    private WorldPoint recoveryTarget, recoveryBefore;
    private String recoveryKey = "", missingKey = "", paceReason = "";
    private long recoveryAt, nextActionAt, unknownDialogueSince, missingSince, finishStageSince, loggedInAt, itemStageSince;
    private long lastRomeoAt, lastApothecaryAt, lastJulietAt;
    private long messageHandInAt, berryHandInAt, potionHandInAt, finalTalkAt;
    private int priorVarp = -1, priorMessage = -1, priorBerries = -1, priorPotion = -1, talkAttempts;
    private String talkKey = "", classSha256 = "UNKNOWN";

    public boolean run(RomeoJulietConfig config, BooleanSupplier exclusiveInput) {
        if (isRunning()) return true;
        stopped = false; ownsInput = exclusiveInput;
        error = ""; stage = "STARTING"; pending = null; route = null;
        failures.clear(); priorVarp = -1; priorMessage = priorBerries = priorPotion = -1;
        talkAttempts = 0; talkKey = "";
        messageHandInAt = berryHandInAt = potionHandInAt = finalTalkAt = 0;
        finishStageSince = loggedInAt = itemStageSince = lastRomeoAt = lastApothecaryAt = lastJulietAt = 0;
        classSha256 = loadedClassSha256();
        log.info("[RomeoJuliet] RUNNING_BUILD={} quest=ROMEO__JULIET classSha256={}",
            BUILD_NUMBER, classSha256);
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
        catch (Exception ex) { log.warn("[RomeoJuliet] status cleanup: {}", ex.toString()); }
    }

    private void tick() {
        if (stopped || Thread.currentThread().isInterrupted()) return;
        Frame f = null;
        try {
            f = observe();
            if (!f.loggedIn()) {
                cancelRoute(); pending = null; loggedInAt = 0; stage = "WAIT_LOGIN"; return;
            }
            if (loggedInAt == 0) loggedInAt = System.currentTimeMillis();
            if (!f.inventoryLoaded || System.currentTimeMillis() - loggedInAt < 3000) {
                stage = "WAIT_INVENTORY"; return;
            }
            long now = System.currentTimeMillis();
            boolean changedStage = priorVarp >= 0 && f.varp != priorVarp;
            if (f.varp != priorVarp && f.varp >= 0) {
                priorVarp = f.varp; talkAttempts = 0;
                messageHandInAt = berryHandInAt = potionHandInAt = 0;
                finalTalkAt = 0;
                finishStageSince = itemStageSince = 0;
            }
            if (f.varp == 20 && priorMessage > 0 && f.count(MESSAGE) == 0
                && distance(f.pos, ROMEO) <= 18 && now - lastRomeoAt < 30000)
                messageHandInAt = now;
            if (f.varp == 40 && priorBerries > 0 && f.count(BERRIES) == 0
                && distance(f.pos, APOTHECARY) <= 10 && now - lastApothecaryAt < 30000)
                berryHandInAt = now;
            if (f.varp == 50 && priorPotion > 0 && f.count(POTION) == 0
                && f.julietUpper() && now - lastJulietAt < 30000)
                potionHandInAt = now;
            priorMessage = f.count(MESSAGE);
            priorBerries = f.count(BERRIES);
            priorPotion = f.count(POTION);
            if (!ownsInput.getAsBoolean()) { cancelRoute(); pending = null; stage = "WAIT_EXCLUSIVE"; return; }
            if (Microbot.pauseAllScripts.get() || InputArbiter.isHuman()) {
                cancelRoute(); stage = "WAIT_INPUT"; return;
            }
            if (Microbot.getBlockingEventManager().shouldBlockAndProcess()) {
                cancelRoute(); stage = "WAIT_BLOCKING_EVENT"; return;
            }
            if (f.finished()) { cancelRoute(); pending = null; stage = "DONE"; return; }
            if (!error.isEmpty()) { cancelRoute(); stage = "HOLD"; return; }
            if (changedStage && route != null) {
                route.cancelReason = "quest stage changed to " + f.varp;
                cancelRoute(); return;
            }
            if (pending != null) { verifyPending(f); return; }
            if (f.health > 0 && f.health < 35 && safety(f)) return;
            if (route != null) { route(f, route.key, route.target, route.radius); return; }
            if (now < nextActionAt) { stage = "WAIT_PACE"; return; }
            paceReason = "";
            if (f.varp < 0 || "UNKNOWN".equals(f.questState)) {
                hold("Unknown Romeo & Juliet state/varp: " + f.questState + "/" + f.varp); return;
            }
            if (f.varp > 60) {
                if (finishStageSince == 0) finishStageSince = now;
                if (dialogue(f)) return;
                if (now - finishStageSince > 30000)
                    hold("Quest varp reached " + f.varp + " without QuestState.FINISHED");
                else stage = "VERIFY_FINISH";
                return;
            }
            if (dialogue(f)) return;
            switch (f.varp) {
                case 0: stage = "START_ROMEO"; talk(f, "ROMEO_START", ROMEO_IDS, ROMEO, 10); return;
                case 10: visitJuliet(f, false); return;
                case 20: deliverMessage(f); return;
                case 30: visitLawrence(f); return;
                case 40: getPotion(f); return;
                case 50: deliverPotion(f); return;
                case 60: finishQuest(f); return;
                default: hold("Unmapped Romeo & Juliet varp " + f.varp);
            }
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted()) return;
            hold("Tick exception: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            log.error("[RomeoJuliet] tick failed", ex);
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
            s.varp = c.getVarpValue(QUEST_VARP);
            QuestState qs = Quest.ROMEO__JULIET.getState(c);
            s.questState = qs == null ? "UNKNOWN" : qs.name();
            ItemContainer inv = c.getItemContainer(InventoryID.INVENTORY);
            s.inventoryLoaded = inv != null;
            if (inv != null) for (Item item : inv.getItems())
                if (item != null && item.getId() >= 0)
                    s.items.merge(item.getId(), item.getQuantity(), Integer::sum);
            return s;
        });
        if (f.loggedIn()) {
            f.health = Rs2Player.getHealthPercentage();
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
            if ("talk:ROMEO_FINAL".equals(p.key)) finalTalkAt = System.currentTimeMillis();
            log.info("[RomeoJuliet] PROVED action={} varp={} pos={} berries={} message={} potion={}",
                p.key, f.varp, f.pos, f.count(BERRIES), f.count(MESSAGE), f.count(POTION));
            pending = null; failures.remove(p.key); missingKey = ""; missingSince = 0;
            long min = p.proof == Proof.DIALOGUE || p.proof == Proof.TALK ? 100 : 250;
            long max = p.proof == Proof.DIALOGUE || p.proof == Proof.TALK ? 400 : 750;
            if (f.varp != p.before.varp) { min = 500; max = 1200; }
            nextActionAt = System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(min, max + 1);
            paceReason = "proved " + p.key;
            return;
        }
        if (System.currentTimeMillis() - p.at < p.timeout) return;
        int count = failures.merge(p.key, 1, Integer::sum);
        pending = null;
        if (count >= 3) hold("Unproved " + p.key + " after " + count + " attempts; varp="
            + f.varp + ", pos=" + f.pos + ", berries=" + f.count(BERRIES)
            + ", message=" + f.count(MESSAGE) + ", potion=" + f.count(POTION)
            + ", dialogue=" + f.dialogue);
        else log.warn("[RomeoJuliet] RETRY action={} failure={}/3", p.key, count);
    }

    private boolean proved(Pending p, Frame f) {
        switch (p.proof) {
            case TALK: case DIALOGUE:
                return f.finished() || f.varp != p.before.varp
                    || f.count(BERRIES) != p.before.count(BERRIES)
                    || f.count(MESSAGE) != p.before.count(MESSAGE)
                    || f.count(POTION) != p.before.count(POTION)
                    || f.inDialogue != p.before.inDialogue || f.hasContinue != p.before.hasContinue
                    || !f.dialogue.equals(p.before.dialogue);
            case STAIRS_UP: return f.julietUpper();
            case STAIRS_DOWN: return f.pos != null && f.pos.getPlane() == 0;
            case PICK_BERRIES: return f.count(BERRIES) > p.before.count(BERRIES);
            case EAT: return f.count(p.itemId) < p.before.count(p.itemId);
            default: return false;
        }
    }

    private void issue(String key, Proof proof, Frame f, long timeout, int itemId, BooleanSupplier action) {
        boolean accepted = action.getAsBoolean();
        pending = new Pending(key, proof, f, timeout, itemId);
        log.info("[RomeoJuliet] ACTION key={} accepted={} varp={} pos={} health={}",
            key, accepted, f.varp, f.pos, f.health);
    }

    private void visitJuliet(Frame f, boolean recovery) {
        stage = recovery ? "RECOVER_MESSAGE" : "VISIT_JULIET";
        if (!recovery && f.count(MESSAGE) > 0) {
            if (itemStageSince == 0) itemStageSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - itemStageSince > 20000)
                hold("Juliet gave a message but varp remained 10");
            else stage = "VERIFY_MESSAGE";
            return;
        }
        if (!f.julietUpper()) { enterJulietUpper(f); return; }
        talk(f, recovery ? "JULIET_REISSUE" : "JULIET_MESSAGE", JULIET_IDS, JULIET, 4);
    }

    private void deliverMessage(Frame f) {
        stage = "DELIVER_MESSAGE";
        if (f.count(MESSAGE) == 0 && messageHandInAt != 0) {
            if (System.currentTimeMillis() - messageHandInAt > 20000)
                hold("Romeo took Juliet's message but varp remained 20");
            else stage = "VERIFY_MESSAGE_HAND_IN";
            return;
        }
        if (f.count(MESSAGE) == 0) { visitJuliet(f, true); return; }
        if (f.julietUpper()) { leaveJulietUpper(f); return; }
        talk(f, "ROMEO_MESSAGE", ROMEO_IDS, ROMEO, 10);
    }

    private void visitLawrence(Frame f) {
        stage = "VISIT_LAWRENCE";
        if (f.julietUpper()) { leaveJulietUpper(f); return; }
        talk(f, "LAWRENCE", LAWRENCE_IDS, LAWRENCE, 4);
    }

    private void getPotion(Frame f) {
        stage = "GET_POTION";
        if (f.julietUpper()) { leaveJulietUpper(f); return; }
        if (f.count(POTION) > 0) {
            if (itemStageSince == 0) itemStageSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - itemStageSince > 20000)
                hold("Cadava potion appeared but varp remained 40");
            else stage = "VERIFY_POTION";
            return;
        }
        if (f.count(BERRIES) == 0 && berryHandInAt == 0) { pickBerries(f); return; }
        if (f.count(BERRIES) == 0 && System.currentTimeMillis() - berryHandInAt < 9000) {
            stage = "VERIFY_BERRY_HAND_IN"; return;
        }
        talk(f, "APOTHECARY_POTION", APOTHECARY_IDS, APOTHECARY, 4);
    }

    private void deliverPotion(Frame f) {
        stage = "DELIVER_POTION";
        if (f.count(POTION) == 0 && potionHandInAt != 0) {
            if (System.currentTimeMillis() - potionHandInAt > 30000)
                hold("Juliet took the potion but varp remained 50");
            else stage = "VERIFY_POTION_HAND_IN";
            return;
        }
        if (f.count(POTION) == 0 && f.julietUpper()) {
            if (itemStageSince == 0) itemStageSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - itemStageSince < 20000) {
                stage = "VERIFY_POTION_HAND_IN"; return;
            }
            leaveJulietUpper(f); return;
        }
        if (f.count(POTION) == 0) {
            if (f.count(BERRIES) == 0 && berryHandInAt == 0) { pickBerries(f); return; }
            talk(f, "APOTHECARY_REMAKE", APOTHECARY_IDS, APOTHECARY, 4);
            return;
        }
        if (!f.julietUpper()) { enterJulietUpper(f); return; }
        talk(f, "JULIET_POTION", JULIET_IDS, JULIET, 4);
    }

    private void finishQuest(Frame f) {
        stage = "FINISH_ROMEO";
        if (f.julietUpper()) { leaveJulietUpper(f); return; }
        if (finalTalkAt != 0) {
            if (System.currentTimeMillis() - finalTalkAt > 90000)
                hold("Final Romeo dialogue/cutscene did not prove QuestState.FINISHED in 90s");
            else stage = "VERIFY_FINAL_CUTSCENE";
            return;
        }
        talk(f, "ROMEO_FINAL", ROMEO_IDS, ROMEO, 10);
    }

    private void enterJulietUpper(Frame f) {
        if (f.pos.getPlane() != 0) { hold("Cannot reach Juliet from " + f.pos); return; }
        stage = "CLIMB_JULIET_STAIRS";
        if (route(f, "JULIET_STAIRS_GROUND", JULIET_STAIRS_APPROACH, 2)) return;
        Rs2TileObjectModel stairs = object(new int[]{11797}, JULIET_STAIRS_GROUND, 5);
        if (stairs == null) { missingScene("Juliet up staircase 11797", 12000); return; }
        issue("climb:juliet-up", Proof.STAIRS_UP, f, 12000, 0,
            () -> stairs.click("Climb-up"));
    }

    private void leaveJulietUpper(Frame f) {
        stage = "DESCEND_JULIET_STAIRS";
        if (route(f, "JULIET_STAIRS_UPPER", JULIET_STAIRS_UPPER, 3)) return;
        Rs2TileObjectModel stairs = object(new int[]{11799}, JULIET_STAIRS_UPPER, 5);
        if (stairs == null) { missingScene("Juliet down staircase 11799", 12000); return; }
        issue("climb:juliet-down", Proof.STAIRS_DOWN, f, 12000, 0,
            () -> stairs.click("Climb-down"));
    }

    private void pickBerries(Frame f) {
        stage = "PICK_BERRIES";
        if (route(f, "CADAVA_BUSHES", BERRY_BUSHES, 4)) return;
        Rs2TileObjectModel bush = object(BUSH_IDS, BERRY_BUSHES, 12);
        if (bush == null) { missingScene("cadava bush 23625/23626/23627", 12000); return; }
        String action = Microbot.getClientThread().invoke(() -> {
            if (bush.getObjectComposition() == null) return null;
            String[] actions = bush.getObjectComposition().getActions();
            if (actions == null) return null;
            for (String candidate : actions) if (candidate != null &&
                (candidate.equalsIgnoreCase("Pick-from") || candidate.equalsIgnoreCase("Take")))
                return candidate;
            return null;
        });
        if (action == null) { missingScene("cadava bush has no Pick-from/Take action", 12000); return; }
        issue("pick:cadava-berries", Proof.PICK_BERRIES, f, 10000, 0,
            () -> bush.click(action));
    }

    private void talk(Frame f, String key, int[] ids, WorldPoint point, int radius) {
        if (route(f, key, point, radius)) return;
        Rs2NpcModel target = npc(ids, point, key.startsWith("ROMEO") ? 20 : 12);
        if (target == null) { missingScene(key + " NPC " + ids[0], 12000); return; }
        if (!target.hasLineOfSight()) {
            if (route(f, key + "_APPROACH", target.getWorldLocation(), 1)) return;
            missingScene(key + " line of sight", 12000);
            return;
        }
        if (!key.equals(talkKey)) { talkKey = key; talkAttempts = 0; }
        if (++talkAttempts > 3) {
            hold("Three " + key + " conversations without quest/item progression; varp=" + f.varp
                + ", berries=" + f.count(BERRIES) + ", message=" + f.count(MESSAGE)
                + ", potion=" + f.count(POTION));
            return;
        }
        if (key.startsWith("ROMEO")) lastRomeoAt = System.currentTimeMillis();
        if (key.startsWith("JULIET")) lastJulietAt = System.currentTimeMillis();
        if (key.startsWith("APOTHECARY")) lastApothecaryAt = System.currentTimeMillis();
        issue("talk:" + key, Proof.TALK, f, 9000, 0, () -> target.click("Talk-to"));
    }

    /** Dialogue choices come from the installed helper or observed transcript. */
    private boolean dialogue(Frame f) {
        if (!f.inDialogue && f.options.isEmpty() && !f.hasContinue) {
            unknownDialogueSince = 0; return false;
        }
        if (f.varp == 20 && distance(f.pos, ROMEO) <= 20) lastRomeoAt = System.currentTimeMillis();
        if (f.varp == 40 && distance(f.pos, APOTHECARY) <= 12) lastApothecaryAt = System.currentTimeMillis();
        if (f.varp == 50 && f.julietUpper()) lastJulietAt = System.currentTimeMillis();
        String[] accepted;
        switch (f.varp) {
            case 0:
                accepted = new String[]{"perhaps i could help to find her for you",
                    "yes, i have seen her actually", "yes, ok, i'll let her know", "yes"};
                break;
            case 30: accepted = new String[]{"ok, thanks"}; break;
            case 40: accepted = new String[]{"talk about something else", "talk about romeo & juliet"}; break;
            case 50:
                accepted = f.julietUpper() ? new String[0]
                    : new String[]{"talk about something else", "talk about romeo & juliet"};
                break;
            default: accepted = new String[0];
        }
        if (!f.options.isEmpty()) {
            for (String fragment : accepted) for (String option : f.options)
                if (knownOption(option, fragment)) {
                    unknownDialogueSince = 0;
                    issue("dialogue:" + fragment, Proof.DIALOGUE, f, 7500, 0,
                        () -> Rs2Dialogue.clickOption(option));
                    return true;
                }
            if (unknownDialogueSince == 0) unknownDialogueSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - unknownDialogueSince > 10000)
                hold("Unexpected Romeo and Juliet dialogue at varp " + f.varp + ": " + f.options);
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
        if ("yes".equals(fragment)) return "yes".equals(actual) || "yes.".equals(actual);
        return actual.contains(fragment);
    }

    /** The installed walker owns path and door interaction; this code owns the time budget. */
    private boolean route(Frame f, String key, WorldPoint target, int radius) {
        if (route != null && route.cancelRequested) {
            Route prior = route;
            if (!cancelRoute()) return true;
            if (prior.failureReason != null) finishRouteFailure(f, prior);
            return true;
        }
        if (route != null && (!route.key.equals(key) || !route.target.equals(target))) {
            route.cancelReason = "target changed to " + key;
            cancelRoute();
            return true;
        }
        if (recoveryTarget != null) {
            if (!recoveryKey.equals(key)) { recoveryTarget = recoveryBefore = null; }
            else {
                if (!f.pos.equals(recoveryBefore)) {
                    log.info("[RomeoJuliet] PROVED route recovery={} from={} to={} requested={}",
                        key, recoveryBefore, f.pos, recoveryTarget);
                    // Count consecutive failures without any movement, not
                    // short walker segments that a canvas nudge recovered.
                    failures.remove("walk:" + key);
                    recoveryTarget = recoveryBefore = null;
                    recoveryKey = "";
                    return true;
                }
                if (System.currentTimeMillis() - recoveryAt > 8000)
                    hold("Route recovery " + key + " produced no position change after 8s; at="
                        + f.pos + ", requested=" + recoveryTarget);
                else stage = "VERIFY_ROUTE_RECOVERY";
                return true;
            }
        }
        int distance = distance(f.pos, target);
        if (distance <= radius) {
            if (route != null) {
                route.cancelReason = "arrived " + key;
                if (!cancelRoute()) return true;
                failures.remove("walk:" + key);
                stage = "ARRIVED_" + key; return true;
            }
            return false;
        }
        stage = "WALK_" + key;
        if (route == null) {
            Route r = new Route(key, target, radius, f.pos);
            route = r;
            startRouteSegment(r, distance);
            log.info("[RomeoJuliet] ROUTE start={} from={} target={} radius={}",
                key, f.pos, target, radius);
            return true;
        }
        Route r = route;
        long now = System.currentTimeMillis();
        if (distance < r.bestDistance) { r.bestDistance = distance; r.lastProgress = now; }
        if (now - r.started > 240000 || now - r.lastProgress > 20000) {
            routeFailure(f, "route timed out/no progress", r);
            return true;
        }
        if (r.completed && (r.worker == null || !r.worker.isAlive())
            && now - r.completedAt >= 1500) {
            // A completed walker call alone does not prove arrival.
            if (distance < r.segmentDistance) startRouteSegment(r, distance);
            else routeFailure(f, "walker segment ended without position progress", r);
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
        }, "RomeoJuliet-route");
        t.setDaemon(true); r.worker = t; t.start();
    }

    private void routeFailure(Frame f, String reason, Route r) {
        r.failureReason = reason;
        r.cancelReason = "failure " + reason;
        if (!cancelRoute()) return;
        finishRouteFailure(f, r);
    }

    private void finishRouteFailure(Frame f, Route r) {
        int count = failures.merge("walk:" + r.key, 1, Integer::sum);
        log.warn("[RomeoJuliet] ROUTE failure={} attempt={}/3 at={} target={} bestDist={}",
            r.failureReason, count, f.pos, r.target, r.bestDistance);
        if (count >= 3) hold("Route " + r.key + " failed after " + count + " attempts: "
            + r.failureReason + "; at=" + f.pos + ", target=" + r.target);
        else if (!nearbyRecovery(f, r)) Rs2Walker.recalculatePath();
    }

    private boolean nearbyRecovery(Frame f, Route r) {
        Map<WorldPoint, Integer> reachable = Rs2Tile.getReachableTilesFromTile(f.pos, 3);
        if (reachable == null || reachable.isEmpty()) return false;
        WorldPoint best = null;
        int current = distance(f.pos, r.target);
        for (WorldPoint candidate : reachable.keySet()) {
            if (candidate == null || candidate.equals(f.pos)
                || distance(f.pos, candidate) > 3 || distance(candidate, r.target) >= current) continue;
            if (best == null || distance(candidate, r.target) < distance(best, r.target))
                best = candidate;
        }
        if (best == null) {
            log.warn("[RomeoJuliet] No nearer reachable tile within 3 of {} toward {}", f.pos, r.target);
            return false;
        }
        boolean clicked = Rs2Walker.walkFastCanvas(best);
        log.info("[RomeoJuliet] ROUTE_RECOVERY key={} from={} to={} clicked={}",
            r.key, f.pos, best, clicked);
        if (!clicked) return false;
        recoveryTarget = best;
        recoveryBefore = f.pos;
        recoveryKey = r.key;
        recoveryAt = System.currentTimeMillis();
        stage = "VERIFY_ROUTE_RECOVERY";
        return true;
    }

    private boolean cancelRoute() {
        Route r = route;
        if (r != null) {
            if (!r.cancelRequested) {
                r.cancelRequested = true;
                r.cancelAt = System.currentTimeMillis();
                if (r.cancelReason == null) r.cancelReason = "state gate";
                if (r.worker != null && r.worker.isAlive()) r.worker.interrupt();
                Thread clear = new Thread(() -> {
                    try { Rs2Walker.clearWalkingRoute("romeojuliet:" + r.cancelReason); }
                    catch (Exception ex) {
                        r.clearError = ex.toString();
                        log.error("[RomeoJuliet] route clear failed", ex);
                    }
                }, "RomeoJuliet-route-clear");
                clear.setDaemon(true);
                r.clearWorker = clear;
                clear.start();
                log.info("[RomeoJuliet] ROUTE_CANCEL reason={} target={}", r.cancelReason, r.target);
            }
            if ((r.worker != null && r.worker.isAlive())
                || (r.clearWorker != null && r.clearWorker.isAlive())) {
                if (System.currentTimeMillis() - r.cancelAt > 60000 && error.isEmpty()) {
                    error = "Walker still active 60s after cancel; target=" + r.target
                        + ", reason=" + r.cancelReason;
                    stage = "HOLD";
                    log.error("[RomeoJuliet] HOLD {}", error);
                } else if (error.isEmpty()) stage = "WAIT_ROUTE_STOP";
                return false;
            }
            route = null;
            if (r.clearError != null && error.isEmpty()) {
                error = "Walker clear failed for " + r.target + ": " + r.clearError;
                stage = "HOLD";
                log.error("[RomeoJuliet] HOLD {}", error);
                return false;
            }
        }
        return true;
    }

    private boolean safety(Frame f) {
        stage = "LOW_HEALTH";
        List<Rs2ItemModel> food = Rs2Inventory.getInventoryFood();
        if (food == null || food.isEmpty()) {
            if (f.health <= 30) {
                hold("Health " + f.health + "% with no food in inventory");
                return true;
            }
            return false;
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

    private void missingScene(String key, long timeout) {
        if (!key.equals(missingKey)) { missingKey = key; missingSince = System.currentTimeMillis(); }
        if (System.currentTimeMillis() - missingSince > timeout)
            hold("Target absent from loaded scene: " + key);
    }

    private void hold(String reason) {
        if (error.isEmpty()) log.error("[RomeoJuliet] HOLD {}", reason);
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
        try (InputStream in = RomeoJulietScript.class.getResourceAsStream("RomeoJulietScript.class")) {
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
            log.warn("[RomeoJuliet] class SHA-256 unavailable: {}", ex.toString());
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
            p.setProperty("questVarp", f == null ? "-1" : Integer.toString(f.varp));
            p.setProperty("questState", f == null ? "UNKNOWN" : f.questState);
            p.setProperty("stage", stage);
            p.setProperty("position", f == null ? "UNKNOWN" : String.valueOf(f.pos));
            p.setProperty("currentWorld", f == null ? "0" : Integer.toString(f.world));
            p.setProperty("gameState", f == null ? "UNKNOWN" : f.gameState);
            p.setProperty("inventoryLoaded", f == null ? "false" : Boolean.toString(f.inventoryLoaded));
            p.setProperty("error", error);
            p.setProperty("cadavaBerries", f == null ? "0" : Integer.toString(f.count(BERRIES)));
            p.setProperty("message", f == null ? "0" : Integer.toString(f.count(MESSAGE)));
            p.setProperty("cadavaPotion", f == null ? "0" : Integer.toString(f.count(POTION)));
            p.setProperty("healthPercent", f == null ? "-1" : Double.toString(f.health));
            p.setProperty("sha256", classSha256);
            p.setProperty("pending", pending == null ? "" : pending.key);
            p.setProperty("walkTarget", route == null ? "" : String.valueOf(route.target));
            p.setProperty("paceRemainingMs", Long.toString(Math.max(0, nextActionAt - System.currentTimeMillis())));
            p.setProperty("paceReason", paceReason);
            Path temp = STATUS_DIR.resolve("status.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) {
                p.store(out, "Romeo and Juliet live script");
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
        } catch (Exception ex) { log.warn("[RomeoJuliet] status write: {}", ex.toString()); }
    }
}
