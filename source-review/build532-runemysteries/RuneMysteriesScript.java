package net.runelite.client.plugins.microbot.runemysteries;

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

/** Installed Quest Helper's varp-63 route; actions require later observed proof. */
public class RuneMysteriesScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(RuneMysteriesScript.class);
    public static final int BUILD_NUMBER = 532;
    private static final int QUEST_VARP = 63;
    private static final int PACKAGE_RECOVERY_VARBIT = 13723;
    private static final int NOTES_RECOVERY_VARBIT = 13725;
    private static final int NOTES_HANDED_VARBIT = 13726;
    private static final int AIR_TALISMAN = 1438, PACKAGE = 290, NOTES = 291;
    private static final int[] DUKE_IDS = {815};
    private static final int[] SEDRIDOR_IDS = {11432};
    private static final int[] AUBURY_IDS = {11434};
    private static final WorldPoint CASTLE_STAIRS_GROUND = new WorldPoint(3205, 3208, 0);
    private static final WorldPoint CASTLE_STAIRS_UPPER = new WorldPoint(3204, 3207, 1);
    private static final WorldPoint DUKE = new WorldPoint(3209, 3222, 1);
    private static final WorldPoint TOWER_LADDER = new WorldPoint(3104, 3162, 0);
    private static final WorldPoint BASEMENT_LADDER = new WorldPoint(3103, 9576, 0);
    private static final WorldPoint SEDRIDOR = new WorldPoint(3104, 9571, 0);
    private static final WorldPoint AUBURY = new WorldPoint(3253, 3401, 0);
    private static final Path STATUS_DIR = Paths.get(System.getProperty("user.home"), ".runelite", "runemysteries");

    private enum Proof { TALK, DIALOGUE, CASTLE_UP, CASTLE_DOWN, TOWER_DOWN, TOWER_UP, EAT }

    private static final class Frame {
        String gameState = "NO_CLIENT", questState = "UNKNOWN", dialogue = "";
        WorldPoint pos;
        int world, varp = -1, packageRecovery = -1, notesRecovery = -1, notesHanded = -1;
        boolean inDialogue, hasContinue;
        double health = -1;
        final Map<Integer, Integer> items = new HashMap<>();
        final List<String> options = new ArrayList<>();
        int count(int id) { return items.getOrDefault(id, 0); }
        boolean loggedIn() { return "LOGGED_IN".equals(gameState) && pos != null; }
        boolean finished() { return "FINISHED".equals(questState); }
        boolean basement() { return pos != null && pos.getPlane() == 0
            && pos.getX() >= 3094 && pos.getX() <= 3125
            && pos.getY() >= 9553 && pos.getY() <= 9582; }
        boolean castleUpper() { return pos != null && pos.getPlane() == 1
            && pos.getX() >= 3185 && pos.getX() <= 3225
            && pos.getY() >= 3190 && pos.getY() <= 3240; }
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
    private long recoveryAt, nextActionAt, unknownDialogueSince, missingSince, finishStageSince;
    private long lastSedridorAt, lastAuburyAt, talismanLostAt, packageLostAt, notesLostAt;
    private int priorVarp = -1, priorTalisman = -1, priorPackage = -1, priorNotes = -1, talkAttempts;
    private String talkKey = "", classSha256 = "UNKNOWN";

    public boolean run(RuneMysteriesConfig config, BooleanSupplier exclusiveInput) {
        if (isRunning()) return true;
        stopped = false; ownsInput = exclusiveInput;
        error = ""; stage = "STARTING"; pending = null; route = null;
        failures.clear(); priorVarp = -1; priorTalisman = priorPackage = priorNotes = -1; talkAttempts = 0;
        talkKey = ""; talismanLostAt = packageLostAt = notesLostAt = finishStageSince = 0;
        lastSedridorAt = lastAuburyAt = 0;
        classSha256 = loadedClassSha256();
        log.info("[RuneMysteries] RUNNING_BUILD={} quest=RUNE_MYSTERIES classSha256={}",
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
        catch (Exception ex) { log.warn("[RuneMysteries] status cleanup: {}", ex.toString()); }
    }

    private void tick() {
        if (stopped || Thread.currentThread().isInterrupted()) return;
        Frame f = null;
        try {
            f = observe();
            if (!f.loggedIn()) { cancelRoute(); pending = null; stage = "WAIT_LOGIN"; return; }
            if (f.varp != priorVarp && f.varp >= 0) {
                priorVarp = f.varp; talkAttempts = 0;
                talismanLostAt = packageLostAt = notesLostAt = 0;
            }
            if (f.varp == 1 && priorTalisman > 0 && f.count(AIR_TALISMAN) == 0
                && f.basement() && System.currentTimeMillis() - lastSedridorAt < 30000) {
                talismanLostAt = System.currentTimeMillis();
                log.info("[RuneMysteries] TALISMAN_HAND_IN_OBSERVED awaiting varp proof");
            }
            if (f.varp == 3 && priorPackage > 0 && f.count(PACKAGE) == 0
                && distance(f.pos, AUBURY) <= 10 && System.currentTimeMillis() - lastAuburyAt < 30000) {
                packageLostAt = System.currentTimeMillis();
                log.info("[RuneMysteries] PACKAGE_HAND_IN_OBSERVED awaiting varp proof");
            }
            if (f.varp == 5 && priorNotes > 0 && f.count(NOTES) == 0
                && f.basement() && System.currentTimeMillis() - lastSedridorAt < 30000) {
                notesLostAt = System.currentTimeMillis();
                log.info("[RuneMysteries] NOTES_HAND_IN_OBSERVED awaiting quest/varbit proof");
            }
            priorTalisman = f.count(AIR_TALISMAN);
            priorPackage = f.count(PACKAGE);
            priorNotes = f.count(NOTES);
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
            if (f.varp < 0 || "UNKNOWN".equals(f.questState)) {
                hold("Unknown Rune Mysteries quest state/varp: " + f.questState + "/" + f.varp); return;
            }
            if (f.varp > 5) {
                if (finishStageSince == 0) finishStageSince = System.currentTimeMillis();
                if (dialogue(f)) return;
                if (System.currentTimeMillis() - finishStageSince > 20000)
                    hold("Quest varp reached " + f.varp + " but QuestState is not FINISHED after 20s");
                else stage = "VERIFY_FINISH";
                return;
            }
            if (dialogue(f)) return;
            switch (f.varp) {
                case 0: startAtDuke(f); return;
                case 1: bringTalisman(f); return;
                case 2: getPackage(f); return;
                case 3: bringPackage(f); return;
                case 4: getNotes(f); return;
                case 5: bringNotes(f); return;
                default: hold("Unmapped quest varp " + f.varp);
            }
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted()) return;
            hold("Tick exception: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            log.error("[RuneMysteries] tick failed", ex);
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
            s.packageRecovery = c.getVarbitValue(PACKAGE_RECOVERY_VARBIT);
            s.notesRecovery = c.getVarbitValue(NOTES_RECOVERY_VARBIT);
            s.notesHanded = c.getVarbitValue(NOTES_HANDED_VARBIT);
            QuestState qs = Quest.RUNE_MYSTERIES.getState(c);
            s.questState = qs == null ? "UNKNOWN" : qs.name();
            ItemContainer inv = c.getItemContainer(InventoryID.INVENTORY);
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
            log.info("[RuneMysteries] PROVED action={} varp={} pos={} talisman={} package={} notes={}",
                p.key, f.varp, f.pos, f.count(AIR_TALISMAN), f.count(PACKAGE), f.count(NOTES));
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
            + f.varp + ", pos=" + f.pos + ", talisman=" + f.count(AIR_TALISMAN)
            + ", package=" + f.count(PACKAGE) + ", notes=" + f.count(NOTES)
            + ", dialogue=" + f.dialogue);
        else log.warn("[RuneMysteries] RETRY action={} failure={}/3", p.key, count);
    }

    private boolean proved(Pending p, Frame f) {
        switch (p.proof) {
            case TALK: case DIALOGUE:
                return f.finished() || f.varp != p.before.varp
                    || f.count(AIR_TALISMAN) != p.before.count(AIR_TALISMAN)
                    || f.count(PACKAGE) != p.before.count(PACKAGE)
                    || f.count(NOTES) != p.before.count(NOTES)
                    || f.inDialogue != p.before.inDialogue || f.hasContinue != p.before.hasContinue
                    || !f.dialogue.equals(p.before.dialogue);
            case CASTLE_UP: return f.castleUpper();
            case CASTLE_DOWN: return f.pos != null && f.pos.getPlane() == 0;
            case TOWER_DOWN: return f.basement();
            case TOWER_UP: return f.pos != null && !f.basement() && f.pos.getY() < 9000;
            case EAT: return f.count(p.itemId) < p.before.count(p.itemId);
            default: return false;
        }
    }

    private void issue(String key, Proof proof, Frame f, long timeout, int itemId, BooleanSupplier action) {
        boolean accepted = action.getAsBoolean();
        pending = new Pending(key, proof, f, timeout, itemId);
        log.info("[RuneMysteries] ACTION key={} accepted={} varp={} pos={} health={}",
            key, accepted, f.varp, f.pos, f.health);
    }

    private void startAtDuke(Frame f) {
        stage = "START_DUKE";
        if (!f.castleUpper()) {
            enterCastleUpper(f); return;
        }
        talk(f, "DUKE_START", DUKE_IDS, DUKE, 4);
    }

    private void bringTalisman(Frame f) {
        stage = "BRING_TALISMAN";
        if (f.count(AIR_TALISMAN) == 0) {
            if (talismanLostAt != 0) {
                if (System.currentTimeMillis() - talismanLostAt > 20000)
                    hold("Sedridor took the air talisman but quest varp remained 1 for 20s");
                else stage = "VERIFY_TALISMAN_HAND_IN";
                return;
            }
            stage = "RECOVER_TALISMAN";
            if (!f.castleUpper()) { enterCastleUpper(f); return; }
            talk(f, "DUKE_REISSUE", DUKE_IDS, DUKE, 4);
            return;
        }
        if (goToSedridor(f)) return;
        talk(f, "SEDRIDOR_TALISMAN", SEDRIDOR_IDS, SEDRIDOR, 4);
    }

    private void getPackage(Frame f) {
        stage = "GET_PACKAGE";
        if (goToSedridor(f)) return;
        talk(f, "SEDRIDOR_PACKAGE", SEDRIDOR_IDS, SEDRIDOR, 4);
    }

    private void bringPackage(Frame f) {
        stage = "BRING_PACKAGE";
        if (f.count(PACKAGE) == 0 && packageLostAt != 0) {
            if (System.currentTimeMillis() - packageLostAt > 20000)
                hold("Aubury took the research package but quest varp remained 3 for 20s");
            else stage = "VERIFY_PACKAGE_HAND_IN";
            return;
        }
        // The installed Quest Helper uses varbit 13723=0 to request a
        // replacement package from Sedridor before visiting Aubury.
        if (f.count(PACKAGE) == 0 && f.packageRecovery == 0) {
            stage = "RECOVER_PACKAGE";
            if (goToSedridor(f)) return;
            talk(f, "SEDRIDOR_REISSUE", SEDRIDOR_IDS, SEDRIDOR, 4);
            return;
        }
        if (f.count(PACKAGE) == 0 && f.packageRecovery < 0) {
            hold("Package missing and recovery varbit 13723 unavailable"); return;
        }
        if (goToAubury(f)) return;
        talk(f, "AUBURY_PACKAGE", AUBURY_IDS, AUBURY, 4);
    }

    private void getNotes(Frame f) {
        stage = "GET_NOTES";
        if (goToAubury(f)) return;
        talk(f, "AUBURY_NOTES", AUBURY_IDS, AUBURY, 4);
    }

    private void bringNotes(Frame f) {
        stage = "BRING_NOTES";
        // Varbit 13726 means the notes were handed over and the helper asks
        // for one further Sedridor conversation. Only QuestState proves DONE.
        if (f.notesHanded == 1) {
            notesLostAt = 0;
            if (goToSedridor(f)) return;
            talk(f, "SEDRIDOR_FINAL", SEDRIDOR_IDS, SEDRIDOR, 4);
            return;
        }
        if (f.count(NOTES) == 0 && notesLostAt != 0) {
            if (System.currentTimeMillis() - notesLostAt > 20000)
                hold("Sedridor took the research notes but quest state/varbit stayed incomplete for 20s");
            else stage = "VERIFY_NOTES_HAND_IN";
            return;
        }
        if (f.count(NOTES) == 0) {
            if (f.notesRecovery != 0) {
                hold("Research notes missing, but recovery varbit 13725 is " + f.notesRecovery);
                return;
            }
            stage = "RECOVER_NOTES";
            if (goToAubury(f)) return;
            talk(f, "AUBURY_REISSUE", AUBURY_IDS, AUBURY, 4);
            return;
        }
        if (goToSedridor(f)) return;
        talk(f, "SEDRIDOR_NOTES", SEDRIDOR_IDS, SEDRIDOR, 4);
    }

    /** Route through a live staircase before issuing another quest action. */
    private void enterCastleUpper(Frame f) {
        if (f.basement()) { exitTower(f); return; }
        if (f.pos.getPlane() != 0) { hold("Cannot reach Duke from plane " + f.pos.getPlane()); return; }
        stage = "CLIMB_CASTLE";
        // Sheep Shearer's proven floor tile is 3204,3207; the helper's
        // 3205,3208 is used to locate the live stair object.
        WorldPoint approach = new WorldPoint(3204, 3207, 0);
        if (route(f, "CASTLE_STAIRS_GROUND", approach, 1)) return;
        Rs2TileObjectModel stairs = object(new int[]{56230}, CASTLE_STAIRS_GROUND, 5);
        if (stairs == null) { missingScene("castle up stairs 56230", 12000); return; }
        issue("climb:castle-up", Proof.CASTLE_UP, f, 11000, 0,
            () -> stairs.click("Climb-up"));
    }

    private void leaveCastleUpper(Frame f) {
        if (!f.castleUpper()) return;
        stage = "DESCEND_CASTLE";
        if (route(f, "CASTLE_STAIRS_UPPER", CASTLE_STAIRS_UPPER, 1)) return;
        Rs2TileObjectModel stairs = object(new int[]{16672}, CASTLE_STAIRS_UPPER, 5);
        if (stairs == null) { missingScene("castle down stairs 16672", 12000); return; }
        issue("climb:castle-down", Proof.CASTLE_DOWN, f, 11000, 0,
            () -> stairs.click("Climb-down"));
    }

    private boolean goToSedridor(Frame f) {
        if (f.castleUpper()) { leaveCastleUpper(f); return true; }
        if (f.basement()) return false;
        if (f.pos.getPlane() != 0 || f.pos.getY() >= 9000) {
            hold("Cannot enter Wizards' Tower basement from " + f.pos); return true;
        }
        stage = "ENTER_TOWER_BASEMENT";
        if (route(f, "TOWER_LADDER", TOWER_LADDER, 3)) return true;
        Rs2TileObjectModel ladder = object(new int[]{2147}, TOWER_LADDER, 5);
        if (ladder == null) { missingScene("tower down ladder 2147", 12000); return true; }
        issue("climb:tower-down", Proof.TOWER_DOWN, f, 11000, 0,
            () -> ladder.click("Climb-down"));
        return true;
    }

    private boolean goToAubury(Frame f) {
        if (f.castleUpper()) { leaveCastleUpper(f); return true; }
        if (f.basement()) { exitTower(f); return true; }
        if (f.pos.getPlane() != 0 || f.pos.getY() >= 9000) {
            hold("Cannot travel to Aubury from " + f.pos); return true;
        }
        stage = "GO_AUBURY";
        return route(f, "AUBURY", AUBURY, 4);
    }

    private void exitTower(Frame f) {
        stage = "EXIT_TOWER_BASEMENT";
        if (route(f, "BASEMENT_LADDER", BASEMENT_LADDER, 3)) return;
        Rs2TileObjectModel ladder = object(new int[]{2148}, BASEMENT_LADDER, 5);
        if (ladder == null) { missingScene("tower up ladder 2148", 12000); return; }
        issue("climb:tower-up", Proof.TOWER_UP, f, 11000, 0,
            () -> ladder.click("Climb-up"));
    }

    private void talk(Frame f, String key, int[] ids, WorldPoint point, int radius) {
        if (route(f, key, point, radius)) return;
        Rs2NpcModel target = npc(ids, point, 9);
        if (target == null) { missingScene(key + " NPC " + ids[0], 12000); return; }
        if (!key.equals(talkKey)) { talkKey = key; talkAttempts = 0; }
        if (++talkAttempts > 3) {
            hold("Three " + key + " conversations without quest/item progression; varp=" + f.varp
                + ", talisman=" + f.count(AIR_TALISMAN) + ", package=" + f.count(PACKAGE)
                + ", notes=" + f.count(NOTES));
            return;
        }
        if (key.startsWith("SEDRIDOR")) lastSedridorAt = System.currentTimeMillis();
        if (key.startsWith("AUBURY")) lastAuburyAt = System.currentTimeMillis();
        issue("talk:" + key, Proof.TALK, f, 9000, 0, () -> target.click("Talk-to"));
    }

    /** Dialogue choices come from the installed helper or observed transcript. */
    private boolean dialogue(Frame f) {
        if (!f.inDialogue && f.options.isEmpty() && !f.hasContinue) {
            unknownDialogueSince = 0; return false;
        }
        if (f.varp == 1 && f.basement()) lastSedridorAt = System.currentTimeMillis();
        if (f.varp == 3 && distance(f.pos, AUBURY) <= 10) lastAuburyAt = System.currentTimeMillis();
        if (f.varp == 5 && f.basement()) lastSedridorAt = System.currentTimeMillis();
        String[] accepted;
        switch (f.varp) {
            case 0:
                accepted = new String[]{"have you any quests for me", "yes"}; break;
            case 1:
                accepted = f.count(AIR_TALISMAN) == 0 && f.castleUpper()
                    ? new String[]{"yes", "what did you want me to do again"}
                    : new String[]{"i'm looking for the head wizard", "okay, here you are"};
                break;
            case 2:
                accepted = new String[]{"have you any quests for me", "go ahead", "yes, certainly"};
                break;
            case 3:
                accepted = f.basement() ? new String[]{"go ahead", "yes, certainly"}
                    : new String[]{"been sent here with a package for you"};
                break;
            case 4:
                accepted = new String[]{"anything useful in that package i gave you"}; break;
            case 5:
                accepted = distance(f.pos, AUBURY) <= 10
                    ? new String[]{"anything useful in that package i gave you"}
                    : new String[0];
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
                hold("Unexpected Rune Mysteries dialogue at varp " + f.varp + ": " + f.options);
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
                    log.info("[RuneMysteries] PROVED route recovery={} from={} to={} requested={}",
                        key, recoveryBefore, f.pos, recoveryTarget);
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
            log.info("[RuneMysteries] ROUTE start={} from={} target={} radius={}",
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
        }, "RuneMysteries-route");
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
        log.warn("[RuneMysteries] ROUTE failure={} attempt={}/3 at={} target={} bestDist={}",
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
            log.warn("[RuneMysteries] No nearer reachable tile within 3 of {} toward {}", f.pos, r.target);
            return false;
        }
        boolean clicked = Rs2Walker.walkFastCanvas(best);
        log.info("[RuneMysteries] ROUTE_RECOVERY key={} from={} to={} clicked={}",
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
                    try { Rs2Walker.clearWalkingRoute("runemysteries:" + r.cancelReason); }
                    catch (Exception ex) {
                        r.clearError = ex.toString();
                        log.error("[RuneMysteries] route clear failed", ex);
                    }
                }, "RuneMysteries-route-clear");
                clear.setDaemon(true);
                r.clearWorker = clear;
                clear.start();
                log.info("[RuneMysteries] ROUTE_CANCEL reason={} target={}", r.cancelReason, r.target);
            }
            if ((r.worker != null && r.worker.isAlive())
                || (r.clearWorker != null && r.clearWorker.isAlive())) {
                if (System.currentTimeMillis() - r.cancelAt > 60000 && error.isEmpty()) {
                    error = "Walker still active 60s after cancel; target=" + r.target
                        + ", reason=" + r.cancelReason;
                    stage = "HOLD";
                    log.error("[RuneMysteries] HOLD {}", error);
                } else if (error.isEmpty()) stage = "WAIT_ROUTE_STOP";
                return false;
            }
            route = null;
            if (r.clearError != null && error.isEmpty()) {
                error = "Walker clear failed for " + r.target + ": " + r.clearError;
                stage = "HOLD";
                log.error("[RuneMysteries] HOLD {}", error);
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
        if (error.isEmpty()) log.error("[RuneMysteries] HOLD {}", reason);
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
        try (InputStream in = RuneMysteriesScript.class.getResourceAsStream("RuneMysteriesScript.class")) {
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
            log.warn("[RuneMysteries] class SHA-256 unavailable: {}", ex.toString());
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
            p.setProperty("error", error);
            p.setProperty("airTalisman", f == null ? "0" : Integer.toString(f.count(AIR_TALISMAN)));
            p.setProperty("researchPackage", f == null ? "0" : Integer.toString(f.count(PACKAGE)));
            p.setProperty("researchNotes", f == null ? "0" : Integer.toString(f.count(NOTES)));
            p.setProperty("healthPercent", f == null ? "-1" : Double.toString(f.health));
            p.setProperty("sha256", classSha256);
            p.setProperty("packageRecoveryVarbit", f == null ? "-1" : Integer.toString(f.packageRecovery));
            p.setProperty("notesRecoveryVarbit", f == null ? "-1" : Integer.toString(f.notesRecovery));
            p.setProperty("notesHandedVarbit", f == null ? "-1" : Integer.toString(f.notesHanded));
            p.setProperty("pending", pending == null ? "" : pending.key);
            p.setProperty("walkTarget", route == null ? "" : String.valueOf(route.target));
            p.setProperty("paceRemainingMs", Long.toString(Math.max(0, nextActionAt - System.currentTimeMillis())));
            p.setProperty("paceReason", paceReason);
            Path temp = STATUS_DIR.resolve("status.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) {
                p.store(out, "Rune Mysteries live script");
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
        } catch (Exception ex) { log.warn("[RuneMysteries] status write: {}", ex.toString()); }
    }
}
