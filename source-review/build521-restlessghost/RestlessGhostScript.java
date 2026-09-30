package net.runelite.client.plugins.microbot.restlessghost;

import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.input.InputArbiter;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** QuestHelper's installed varp-107 stages, with one issued action per observed step. */
public class RestlessGhostScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(RestlessGhostScript.class);
    public static final int BUILD_NUMBER = 521;
    private static final int QUEST_VARP = 107, SKULL_VARBIT = 2130;
    private static final int AMULET = 552, SKULL = 553;
    private static final WorldPoint AERECK = new WorldPoint(3243, 3206, 0);
    private static final WorldPoint URHNEY = new WorldPoint(3147, 3175, 0);
    private static final WorldPoint COFFIN = new WorldPoint(3250, 3193, 0);
    private static final WorldPoint GHOST = new WorldPoint(3250, 3195, 0);
    private static final WorldPoint TOWER_LADDER = new WorldPoint(3104, 3162, 0);
    private static final WorldPoint ALTAR = new WorldPoint(3120, 9567, 0);
    private static final WorldPoint BASEMENT_LADDER = new WorldPoint(3103, 9576, 0);
    private static final WorldPoint DRAYNOR_BANK = new WorldPoint(3092, 3245, 0);
    private static final Path STATUS_DIR = Paths.get(System.getProperty("user.home"), ".runelite", "restlessghost");

    private enum Proof { DIALOGUE, AMULET, EQUIPPED, COFFIN_OPEN, GHOST, BASEMENT, SKULL, OUTSIDE, BANK_OPEN, BANK_CLOSED, FINISH, WALK }

    private static final class Frame {
        String gameState = "NO_CLIENT", dialogue = "", questState = "UNKNOWN";
        int varp = -1, skullVarbit = -1, amuletCount, skullCount, emptySlots, world;
        boolean equipped, bankOpen, moving, ghostVisible, coffinOpen, coffinClosed, hasContinue, inDialogue;
        WorldPoint position;
        List<String> options = new ArrayList<>();
        boolean loggedIn() { return "LOGGED_IN".equals(gameState) && position != null; }
        boolean basement() { return position != null && position.getY() >= 9553 && position.getY() <= 9582
            && position.getX() >= 3094 && position.getX() <= 3125; }
        boolean finished() { return "FINISHED".equals(questState); }
    }

    private static final class Pending {
        final String key;
        final Proof proof;
        final Frame before;
        final WorldPoint target;
        final int radius;
        final long issuedAt, timeoutMs;
        Pending(String key, Proof proof, Frame before, WorldPoint target, int radius, long timeoutMs) {
            this.key = key; this.proof = proof; this.before = before; this.target = target;
            this.radius = radius; this.timeoutMs = timeoutMs; this.issuedAt = System.currentTimeMillis();
        }
    }

    private volatile boolean stopped;
    private volatile String stage = "STARTING", error = "";
    private BooleanSupplier ownsInput;
    private Pending pending;
    private final Map<String, Integer> failures = new HashMap<>();
    private long unknownDialogueSince, finalSince, turnInObservedAt, sceneWaitSince;
    private String sceneWaitKey = "";
    private int finalUses;
    private boolean finalizing;

    public int runtimeBuild() { return BUILD_NUMBER; }

    public boolean run(RestlessGhostConfig config, BooleanSupplier exclusiveInput) {
        if (isRunning()) return true;
        stopped = false;
        ownsInput = exclusiveInput;
        log.info("[RestlessGhost] RUNNING_BUILD={} quest=THE_RESTLESS_GHOST", BUILD_NUMBER);
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
        catch (Exception ex) { log.warn("[RestlessGhost] status cleanup: {}", ex.toString()); }
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
            if (f.questState.equals("UNKNOWN") || f.varp < 0 || f.varp > 4) {
                hold("Unknown quest state/varp: " + f.questState + "/" + f.varp); return;
            }
            switch (f.varp) {
                case 0: stageZero(f); break;
                case 1: stageOne(f); break;
                case 2: stageTwo(f); break;
                case 3: stageThree(f); break;
                case 4: stageFour(f); break;
                default: hold("Unmapped quest varp " + f.varp);
            }
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted()) return;
            hold("Tick exception: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            log.error("[RestlessGhost] tick failed", ex);
        } finally {
            writeStatus(f);
        }
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
            s.position = c.getLocalPlayer().getWorldLocation();
            s.varp = c.getVarpValue(QUEST_VARP);
            s.skullVarbit = c.getVarbitValue(SKULL_VARBIT);
            QuestState qs = Quest.THE_RESTLESS_GHOST.getState(c);
            s.questState = qs == null ? "UNKNOWN" : qs.name();
            ItemContainer inventory = c.getItemContainer(InventoryID.INVENTORY);
            if (inventory != null) {
                for (Item item : inventory.getItems()) {
                    if (item == null || item.getId() < 0) { s.emptySlots++; continue; }
                    if (item.getId() == AMULET) s.amuletCount += item.getQuantity();
                    if (item.getId() == SKULL) s.skullCount += item.getQuantity();
                }
            }
            ItemContainer equipment = c.getItemContainer(InventoryID.EQUIPMENT);
            if (equipment != null) for (Item item : equipment.getItems())
                if (item != null && item.getId() == AMULET) s.equipped = true;
            return s;
        });
        if (f.loggedIn()) {
            f.moving = Rs2Player.isMoving();
            f.bankOpen = Rs2Bank.isOpen();
            f.inDialogue = Rs2Dialogue.isInDialogue();
            f.hasContinue = Rs2Dialogue.hasContinue();
            String text = f.inDialogue ? Rs2Dialogue.getDialogueText() : "";
            StringBuilder dialogue = new StringBuilder(normalize(text));
            for (Widget option : Rs2Dialogue.getDialogueOptions()) {
                if (option == null) continue;
                String label = option.getText();
                if (!normalize(label).isEmpty()) { f.options.add(label); dialogue.append('|').append(normalize(label)); }
            }
            f.dialogue = dialogue.toString();
            if (f.varp == 2 || pending != null && pending.proof == Proof.GHOST)
                f.ghostVisible = npc(922, GHOST, 10) != null;
            if (f.varp == 2 || f.varp == 4 || pending != null && pending.proof == Proof.COFFIN_OPEN) {
                f.coffinOpen = object(new int[]{15061, 15052, 15053}, COFFIN, 5) != null;
                f.coffinClosed = object(new int[]{2145}, COFFIN, 5) != null;
            }
        }
        return f;
    }

    private void verifyPending(Frame f) {
        Pending p = pending;
        if (proved(p, f)) {
            log.info("[RestlessGhost] PROVED action={} stage={} varp={} skull={} quest={}",
                p.key, stage, f.varp, f.skullCount, f.questState);
            failures.remove(p.key);
            pending = null;
            sceneWaitSince = 0;
            sceneWaitKey = "";
            return; // Never act in the same tick as the proof.
        }
        if (System.currentTimeMillis() - p.issuedAt < p.timeoutMs) return;
        int count = failures.merge(p.key, 1, Integer::sum);
        pending = null;
        if (p.proof == Proof.WALK) Rs2Walker.setTarget(null);
        if (count >= 3) hold("Unproved " + p.key + " after " + count + " attempts, varp=" + f.varp
            + ", pos=" + f.position + ", dialogue=" + f.dialogue);
        else log.warn("[RestlessGhost] RETRY action={} failure={}/3", p.key, count);
    }

    private boolean proved(Pending p, Frame f) {
        switch (p.proof) {
            case DIALOGUE: return f.varp != p.before.varp || f.amuletCount != p.before.amuletCount
                || f.skullCount != p.before.skullCount || f.inDialogue != p.before.inDialogue
                || !f.dialogue.equals(p.before.dialogue);
            case AMULET: return f.amuletCount > p.before.amuletCount || f.equipped;
            case EQUIPPED: return f.equipped;
            case COFFIN_OPEN: return f.coffinOpen || f.ghostVisible;
            case GHOST: return f.ghostVisible;
            case BASEMENT: return f.basement();
            case SKULL: return f.skullCount > p.before.skullCount || f.skullVarbit > p.before.skullVarbit;
            case OUTSIDE: return !f.basement() && f.position != null && f.position.getY() < 9000;
            case BANK_OPEN: return f.bankOpen;
            case BANK_CLOSED: return !f.bankOpen;
            case FINISH: return f.finished() || f.skullCount < p.before.skullCount
                || !f.dialogue.equals(p.before.dialogue);
            case WALK: return f.position != null && p.before.position != null &&
                (distance(f.position, p.target) <= p.radius
                    || distance(f.position, p.target) < distance(p.before.position, p.target));
            default: return false;
        }
    }

    private void issue(String key, Proof proof, Frame f, WorldPoint target, int radius,
                       long timeoutMs, BooleanSupplier action) {
        boolean accepted = action.getAsBoolean();
        pending = new Pending(key, proof, f, target, radius, timeoutMs);
        log.info("[RestlessGhost] ACTION key={} accepted={} varp={} skullVarbit={} pos={} target={}",
            key, accepted, f.varp, f.skullVarbit, f.position, target);
    }

    private boolean travel(Frame f, String name, WorldPoint target, int radius) {
        if (distance(f.position, target) <= radius) return false;
        stage = "WALK_" + name;
        if (!f.moving) issue("walk:" + name, Proof.WALK, f, target, radius, 12000,
            () -> Rs2Walker.walkTo(target, radius));
        return true;
    }

    private void stageZero(Frame f) {
        stage = "TALK_AERECK";
        if (dialogue(f, "i'm looking for a quest", "yes")) return;
        if (travel(f, "AERECK", AERECK, 5)) return;
        Rs2NpcModel father = npc(2812, AERECK, 8);
        if (father == null) { missingScene("Father Aereck NPC 2812"); return; }
        issue("talk:aereck", Proof.DIALOGUE, f, AERECK, 0, 8000, () -> father.click("Talk-to"));
    }

    private void stageOne(Frame f) {
        stage = "TALK_URHNEY";
        if (dialogue(f, "father aereck sent me", "he's got a ghost", "ghostspeak amulet")) return;
        if (travel(f, "URHNEY", URHNEY, 5)) return;
        Rs2NpcModel father = npc(923, URHNEY, 8);
        if (father == null) { missingScene("Father Urhney NPC 923"); return; }
        issue("talk:urhney", Proof.DIALOGUE, f, URHNEY, 0, 8000, () -> father.click("Talk-to"));
    }

    private void stageTwo(Frame f) {
        stage = "SPEAK_GHOST";
        if (dialogue(f, "yep", "yes, ok", "tell me what", "another", "ghostspeak amulet")) return;
        if (!f.equipped) {
            if (f.amuletCount > 0) {
                stage = "EQUIP_AMULET";
                issue("equip:amulet", Proof.EQUIPPED, f, null, 0, 6000, () -> Rs2Inventory.wear(AMULET));
            } else {
                stage = "REPLACE_AMULET";
                if (travel(f, "URHNEY", URHNEY, 5)) return;
                Rs2NpcModel father = npc(923, URHNEY, 8);
                if (father == null) { missingScene("Father Urhney for replacement amulet"); return; }
                issue("talk:replacement-amulet", Proof.DIALOGUE, f, URHNEY, 0, 12000,
                    () -> father.click("Talk-to"));
            }
            return;
        }
        if (travel(f, "COFFIN", COFFIN, 5)) return;
        if (f.ghostVisible) {
            Rs2NpcModel ghost = npc(922, GHOST, 10);
            if (ghost == null) { missingScene("Ghost NPC 922"); return; }
            issue("talk:ghost", Proof.DIALOGUE, f, GHOST, 0, 8000, () -> ghost.click("Talk-to"));
        } else if (f.coffinOpen) {
            Rs2TileObjectModel coffin = object(new int[]{15061, 15052, 15053}, COFFIN, 5);
            if (coffin == null) { missingScene("open coffin"); return; }
            issue("search:coffin-spawn", Proof.GHOST, f, COFFIN, 0, 9000, () -> coffin.click("Search"));
        } else {
            Rs2TileObjectModel coffin = object(new int[]{2145}, COFFIN, 5);
            if (coffin == null) { missingScene("closed coffin 2145"); return; }
            issue("open:coffin", Proof.COFFIN_OPEN, f, COFFIN, 0, 9000, () -> coffin.click("Open"));
        }
    }

    private void stageThree(Frame f) {
        stage = "GET_SKULL";
        if (f.skullCount > 0 || f.skullVarbit == 1) {
            if (f.skullCount == 0) { retrieveSkull(f); return; }
            if (f.basement()) { exitBasement(f); return; }
            stageFour(f); return;
        }
        if (f.emptySlots < 1) { hold("A free inventory slot is required for the ghost skull"); return; }
        if (!f.basement()) {
            if (travel(f, "TOWER_LADDER", TOWER_LADDER, 4)) return;
            Rs2TileObjectModel ladder = object(new int[]{2147}, TOWER_LADDER, 5);
            if (ladder == null) { missingScene("tower ladder 2147"); return; }
            issue("climb:tower-ladder", Proof.BASEMENT, f, TOWER_LADDER, 0, 11000,
                () -> ladder.click("Climb-down"));
            return;
        }
        if (travel(f, "ALTAR", ALTAR, 5)) return;
        Rs2TileObjectModel altar = object(new int[]{2146, 15050, 15051}, ALTAR, 6);
        if (altar == null) { missingScene("altar 2146/15050/15051"); return; }
        issue("search:altar", Proof.SKULL, f, ALTAR, 0, 10000, () -> altar.click("Search"));
    }

    private void stageFour(Frame f) {
        stage = "RETURN_SKULL";
        if (finalizing) {
            stage = "VERIFY_FINISH";
            if (f.skullCount == 0 && turnInObservedAt == 0)
                turnInObservedAt = System.currentTimeMillis();
            if (dialogue(f, "yes", "put the skull")) return;
            if (turnInObservedAt != 0) {
                if (System.currentTimeMillis() - turnInObservedAt < 20000) return;
                hold("Skull left inventory, but quest state is not FINISHED after 20s");
                return;
            }
            if (System.currentTimeMillis() - finalSince < 12000) return;
            if (finalUses >= 3) { hold("Using the skull on the coffin did not finish quest after three attempts"); return; }
            finalizing = false;
            return;
        }
        if (dialogue(f, "yes", "put the skull")) return;
        if (f.skullCount == 0) { retrieveSkull(f); return; }
        if (f.basement()) { exitBasement(f); return; }
        if (f.bankOpen) {
            issue("close:bank", Proof.BANK_CLOSED, f, null, 0, 5000,
                () -> { net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard.keyPress(27); return true; });
            return;
        }
        if (travel(f, "COFFIN", COFFIN, 5)) return;
        if (!f.coffinOpen) {
            Rs2TileObjectModel closed = object(new int[]{2145}, COFFIN, 5);
            if (closed == null) { missingScene("closed coffin for skull return"); return; }
            issue("open:coffin-return", Proof.COFFIN_OPEN, f, COFFIN, 0, 9000,
                () -> closed.click("Open"));
            return;
        }
        Rs2TileObjectModel opened = object(new int[]{15061, 15052, 15053}, COFFIN, 5);
        if (opened == null) { missingScene("open coffin for skull return"); return; }
        finalUses++;
        finalizing = true;
        finalSince = System.currentTimeMillis();
        turnInObservedAt = 0;
        // Installed Microbot's useItemOnObject(int,int) selects the inventory
        // item and interacts with this live object's ID. The following tick
        // still requires observed quest completion before DONE is possible.
        issue("use:skull-on-coffin", Proof.FINISH, f, COFFIN, 0, 10000,
            () -> Rs2Inventory.useItemOnObject(SKULL, opened.getId()));
    }

    private void exitBasement(Frame f) {
        stage = "EXIT_BASEMENT";
        if (travel(f, "BASEMENT_LADDER", BASEMENT_LADDER, 4)) return;
        Rs2TileObjectModel ladder = object(new int[]{2148}, BASEMENT_LADDER, 5);
        if (ladder == null) { missingScene("basement ladder 2148"); return; }
        issue("climb:basement-ladder", Proof.OUTSIDE, f, BASEMENT_LADDER, 0, 11000,
            () -> ladder.click("Climb-up"));
    }

    private void retrieveSkull(Frame f) {
        stage = "RETRIEVE_BANKED_SKULL";
        if (f.basement()) { exitBasement(f); return; }
        if (!f.bankOpen) {
            if (travel(f, "DRAYNOR_BANK", DRAYNOR_BANK, 5)) return;
            issue("open:bank", Proof.BANK_OPEN, f, DRAYNOR_BANK, 0, 9000, Rs2Bank::openBank);
            return;
        }
        if (f.emptySlots < 1) { hold("A free inventory slot is required to withdraw the skull"); return; }
        if (!Rs2Bank.hasItem(SKULL)) { hold("Skull missing from inventory and bank; varbit=" + f.skullVarbit); return; }
        issue("withdraw:skull", Proof.SKULL, f, null, 0, 7000, () -> Rs2Bank.withdrawOne(SKULL));
    }

    /** Returns true when dialogue is visible and therefore owns this tick. */
    private boolean dialogue(Frame f, String... allowedFragments) {
        if (!f.inDialogue && f.options.isEmpty() && !f.hasContinue) {
            unknownDialogueSince = 0; return false;
        }
        if (!f.options.isEmpty()) {
            for (String option : f.options) for (String allowed : allowedFragments) {
                if (normalize(option).contains(allowed)) {
                    unknownDialogueSince = 0;
                    issue("dialogue:" + allowed, Proof.DIALOGUE, f, null, 0, 7000,
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
            issue("dialogue:continue", Proof.DIALOGUE, f, null, 0, 7000,
                () -> { Rs2Dialogue.clickContinue(); return true; });
            return true;
        }
        if (unknownDialogueSince == 0) unknownDialogueSince = System.currentTimeMillis();
        if (System.currentTimeMillis() - unknownDialogueSince > 20000)
            hold("Dialogue did not offer Continue or a known option: " + f.dialogue);
        return true;
    }

    private static Rs2NpcModel npc(int id, WorldPoint location, int radius) {
        return Microbot.getRs2NpcCache().query().withId(id).within(location, radius).nearestOnClientThread();
    }

    private static Rs2TileObjectModel object(int[] ids, WorldPoint location, int radius) {
        return Microbot.getRs2TileObjectCache().query().withIds(ids).within(location, radius).nearestOnClientThread();
    }

    private void missingScene(String object) {
        if (!object.equals(sceneWaitKey)) { sceneWaitKey = object; sceneWaitSince = 0; }
        if (sceneWaitSince == 0) sceneWaitSince = System.currentTimeMillis();
        if (System.currentTimeMillis() - sceneWaitSince > 10000) hold("Target absent from loaded scene: " + object);
    }

    private void hold(String reason) {
        if (error.isEmpty()) log.error("[RestlessGhost] HOLD {}", reason);
        error = reason; stage = "HOLD";
    }

    private static int distance(WorldPoint a, WorldPoint b) {
        if (a == null || b == null || a.getPlane() != b.getPlane()) return Integer.MAX_VALUE;
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replaceAll("<[^>]*>", "").trim().toLowerCase();
    }

    private void writeStatus(Frame f) {
        try {
            Files.createDirectories(STATUS_DIR);
            Properties p = new Properties();
            p.setProperty("build", Integer.toString(BUILD_NUMBER));
            p.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
            p.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
            p.setProperty("currentWorld", f == null ? "0" : Integer.toString(f.world));
            p.setProperty("gameState", f == null ? "UNKNOWN" : f.gameState);
            p.setProperty("stage", stage);
            p.setProperty("questVarp", f == null ? "-1" : Integer.toString(f.varp));
            p.setProperty("questState", f == null ? "UNKNOWN" : f.questState);
            p.setProperty("skullVarbit", f == null ? "-1" : Integer.toString(f.skullVarbit));
            p.setProperty("error", error);
            Path temp = STATUS_DIR.resolve("status.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "Restless Ghost live script"); }
            try { Files.move(temp, STATUS_DIR.resolve("status.properties"),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, STATUS_DIR.resolve("status.properties"),
                StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception ex) { log.warn("[RestlessGhost] status write: {}", ex.toString()); }
    }
}
