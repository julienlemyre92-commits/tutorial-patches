package net.runelite.client.plugins.microbot.piratestreasure;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
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
import java.util.function.Predicate;
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
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.input.InputArbiter;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Installed Quest Helper varp-71 stages, with fresh proof after every action. */
public class PiratesTreasureScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(PiratesTreasureScript.class);
    public static final int BUILD_NUMBER = 556;
    private static final long SHIP_PAYMENT_TIMEOUT_MS = 20_000L;
    private static final int QUEST_VARP = 71;
    private static final int COINS = 995, RUM = 431, BANANA = 1963, APRON = 1005;
    private static final int KEY = 432, MESSAGE = 433, SPADE = 952;
    private static final int PLANTATION_CRATE = 2072, WYDIN_CRATE = 2071;
    private static final int WYDIN_DOOR_CLOSED = 2069, WYDIN_DOOR_OPEN = 2070;
    private static final int BLUE_MOON_STAIRS = 11796, BLUE_MOON_CHEST = 2079;
    private static final int[] FRANK_IDS = {3643}, SAILOR_IDS = {3645};
    private static final int[] ZEMBO_IDS = {13655}, LUTHAS_IDS = {3647};
    private static final int[] CUSTOMS_IDS = {3648}, GARDENER_IDS = {3651};
    private static final WorldPoint FRANK = new WorldPoint(3053, 3251, 0);
    private static final WorldPoint PORT_DOCK = new WorldPoint(3027, 3222, 0);
    private static final WorldPoint ZEMBO = new WorldPoint(2929, 3145, 0);
    private static final WorldPoint BANANA_PATCH = new WorldPoint(2917, 3161, 0);
    private static final WorldPoint LUTHAS = new WorldPoint(2938, 3154, 0);
    private static final WorldPoint PLANTATION = new WorldPoint(2939, 3149, 0);
    private static final WorldPoint CUSTOMS = new WorldPoint(2955, 3146, 0);
    private static final WorldPoint APRON_SPAWN = new WorldPoint(3016, 3229, 0);
    private static final WorldPoint WYDIN_INTERIOR = new WorldPoint(3009, 3207, 0);
    private static final WorldPoint WYDIN_FRONT = new WorldPoint(3013, 3204, 0);
    private static final WorldPoint BLUE_MOON = new WorldPoint(3228, 3393, 0);
    private static final WorldPoint BLUE_MOON_UP = new WorldPoint(3219, 3396, 1);
    private static final WorldPoint FALADOR_SPADE = new WorldPoint(2982, 3369, 0);
    private static final WorldPoint FALADOR_CROSS = new WorldPoint(2999, 3383, 0);
    private static final Path STATUS_DIR = Paths.get(System.getProperty("user.home"),
        ".runelite", "piratestreasure");

    private enum Phase {
        SAIL_OUT, BUY_RUM, PICK_BANANAS, TALK_LUTHAS, STASH_RUM,
        FILL_CRATE, SHIP_CRATE, SAIL_HOME, GET_APRON, RETRIEVE_RUM, DELIVER_RUM
    }
    private static final class Frame {
        String gameState = "NO_CLIENT", questState = "UNKNOWN", dialogue = "", accountKey = "";
        WorldPoint pos;
        int world, varp = -1, freeSlots, equippedApron;
        boolean inventoryLoaded, inDialogue, hasContinue, shopOpen, inCombat;
        boolean gardenerVisible, wydinDoorOpen;
        double health = -1;
        final Map<Integer, Integer> items = new HashMap<>();
        final List<String> options = new ArrayList<>();
        int count(int id) { return items.getOrDefault(id, 0); }
        boolean loggedIn() { return "LOGGED_IN".equals(gameState) && pos != null; }
        boolean finished() { return "FINISHED".equals(questState); }
    }
    private static final class Pending {
        final String key;
        final Frame before;
        final Predicate<Frame> proof;
        final long at, timeout;
        Pending(String key, Frame before, long timeout, Predicate<Frame> proof) {
            this.key = key; this.before = before; this.timeout = timeout; this.proof = proof;
            this.at = System.currentTimeMillis();
        }
    }
    private static final class Route {
        final String key;
        final WorldPoint target;
        final int radius;
        final long started;
        Thread worker, clearWorker;
        volatile boolean completed;
        volatile long completedAt;
        WorldPoint lastPosition, segmentStart;
        long lastProgress, segmentStarted, cancelAt;
        boolean cancelRequested;
        String failureReason, cancelReason, clearError;
        Route(String key, WorldPoint target, int radius, WorldPoint pos) {
            this.key = key; this.target = target; this.radius = radius;
            this.started = this.lastProgress = System.currentTimeMillis();
            this.lastPosition = pos;
        }
    }

    private final Map<String, Integer> failures = new HashMap<>();
    private final Object statusLock = new Object();
    private volatile boolean stopped;
    private volatile String stage = "STARTING", error = "";
    private BooleanSupplier ownsInput;
    private Pending pending;
    private Route route;
    private Phase phase = Phase.SAIL_OUT;
    private boolean checkpointLoaded;
    private WorldPoint recoveryTarget, recoveryBefore;
    private String recoveryKey = "", missingKey = "", paceReason = "", classSha256 = "UNKNOWN";
    private String lastCheckpointSnapshot = "";
    private long recoveryAt, nextActionAt, missingSince, unknownDialogueSince, loggedInAt;
    private long voyageStarted, talkStartedAt, shipTalkAt, gardenerSeenAt, wydinTalkAt;
    private int shipCoinBefore;
    private int priorVarp = -1, digAttempts, attackAttempts;
    private boolean gardenerAppeared;

    public boolean run(PiratesTreasureConfig config, BooleanSupplier exclusiveInput) {
        if (isRunning()) return true;
        stopped = false; ownsInput = exclusiveInput;
        error = ""; stage = "STARTING"; pending = null; route = null;
        failures.clear(); priorVarp = -1; phase = Phase.SAIL_OUT; checkpointLoaded = false;
        digAttempts = attackAttempts = 0; gardenerAppeared = false;
        voyageStarted = talkStartedAt = shipTalkAt = gardenerSeenAt = wydinTalkAt = 0;
        shipCoinBefore = 0;
        loggedInAt = nextActionAt = missingSince = unknownDialogueSince = recoveryAt = 0;
        recoveryTarget = recoveryBefore = null; recoveryKey = missingKey = paceReason = "";
        lastCheckpointSnapshot = "";
        classSha256 = loadedClassSha256();
        log.info("[PiratesTreasure] RUNNING_BUILD={} quest=PIRATES_TREASURE classSha256={}",
            BUILD_NUMBER, classSha256);
        long delay = Math.max(450, Math.min(2000, config.tickDelay()));
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::tick,
            0, delay, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override public void shutdown() {
        stopped = true; cancelRoute();
        if (mainScheduledFuture != null) mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow(); super.shutdown();
        synchronized (statusLock) {
            try { Files.deleteIfExists(STATUS_DIR.resolve("status.properties")); }
            catch (Exception ex) { log.warn("[PiratesTreasure] status cleanup: {}", ex.toString()); }
        }
    }

    private void tick() {
        if (stopped || Thread.currentThread().isInterrupted()) return;
        Frame f = null;
        try {
            f = observe();
            if (!f.loggedIn()) {
                cancelRoute(); pending = null; loggedInAt = 0;
                checkpointLoaded = false; lastCheckpointSnapshot = "";
                stage = "WAIT_LOGIN"; return;
            }
            long now = System.currentTimeMillis();
            if (loggedInAt == 0) loggedInAt = now;
            if (!f.inventoryLoaded || now - loggedInAt < 3000) {
                stage = "WAIT_INVENTORY"; return;
            }
            if (!checkpointLoaded) { restoreCheckpoint(f); checkpointLoaded = true; }
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
            if (f.varp >= 0 && f.varp != priorVarp) {
                log.info("[PiratesTreasure] MILESTONE varp {} -> {} quest={}",
                    priorVarp, f.varp, f.questState);
                priorVarp = f.varp; failures.clear();
                if (f.varp == 0) phase = Phase.SAIL_OUT;
                if (route != null) { route.cancelReason = "quest stage changed"; cancelRoute(); return; }
            }
            if (f.health > 0 && f.health < 30 && pending != null
                && !pending.key.startsWith("eat:")) pending = null;
            if (pending != null) { verifyPending(f); return; }
            if (f.health > 0 && f.health < 30 && safety(f)) return;
            if (route != null) { route(f, route.key, route.target, route.radius); return; }
            if (now < nextActionAt) { stage = "WAIT_PACE"; return; }
            paceReason = "";
            if (f.varp < 0 || "UNKNOWN".equals(f.questState)) {
                hold("Unknown Pirate's Treasure state/varp: " + f.questState + "/" + f.varp); return;
            }
            if (f.varp > 3) { stage = "VERIFY_FINISH"; return; }
            if (dialogue(f)) return;
            if (f.varp == 0) { stage = "START_FRANK"; talk(f, "frank-start", FRANK_IDS, FRANK); return; }
            if (f.varp == 1) { smuggle(f); return; }
            if (f.varp == 2) { chest(f); return; }
            if (f.varp == 3) { dig(f); return; }
            hold("Unmapped Pirate's Treasure varp " + f.varp);
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted()) return;
            hold("Tick exception: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            log.error("[PiratesTreasure] tick failed", ex);
        } finally { writeStatus(f); writeCheckpoint(f); }
    }

    private Frame observe() {
        Frame f = Microbot.getClientThread().invoke(() -> {
            Frame s = new Frame(); Client c = Microbot.getClient();
            if (c == null) return s;
            GameState gs = c.getGameState();
            s.gameState = gs == null ? "UNKNOWN" : gs.name(); s.world = c.getWorld();
            if (gs != GameState.LOGGED_IN || c.getLocalPlayer() == null) return s;
            s.pos = c.getLocalPlayer().getWorldLocation();
            s.accountKey = fingerprint(c.getUsername());
            s.varp = c.getVarpValue(QUEST_VARP);
            QuestState qs = Quest.PIRATES_TREASURE.getState(c);
            s.questState = qs == null ? "UNKNOWN" : qs.name();
            ItemContainer inv = c.getItemContainer(InventoryID.INVENTORY);
            ItemContainer equip = c.getItemContainer(InventoryID.EQUIPMENT);
            s.inventoryLoaded = inv != null;
            if (inv != null) {
                int occupied = 0;
                for (Item item : inv.getItems()) {
                    if (item == null || item.getId() < 0) continue;
                    occupied++; s.items.merge(item.getId(), item.getQuantity(), Integer::sum);
                }
                s.freeSlots = Math.max(0, 28 - occupied);
            }
            if (equip != null) for (Item item : equip.getItems())
                if (item != null && item.getId() == APRON) s.equippedApron += item.getQuantity();
            return s;
        });
        if (f.loggedIn()) {
            f.health = Rs2Player.getHealthPercentage();
            f.inCombat = Rs2Combat.inCombat(); f.shopOpen = Rs2Shop.isOpen();
            f.inDialogue = Rs2Dialogue.isInDialogue();
            f.hasContinue = Rs2Dialogue.hasContinue();
            String text = f.inDialogue ? Rs2Dialogue.getDialogueText() : "";
            StringBuilder d = new StringBuilder(normalize(text));
            for (Widget option : Rs2Dialogue.getDialogueOptions()) {
                if (option == null || normalize(option.getText()).isEmpty()) continue;
                f.options.add(option.getText()); d.append('|').append(normalize(option.getText()));
            }
            f.dialogue = d.toString();
            if (f.varp == 3 && near(f.pos, FALADOR_CROSS, 15))
                f.gardenerVisible = npc(GARDENER_IDS, FALADOR_CROSS, 15) != null;
            if (f.varp == 1 && near(f.pos, WYDIN_FRONT, 12))
                f.wydinDoorOpen = object(WYDIN_DOOR_OPEN, WYDIN_INTERIOR, 8) != null;
        }
        return f;
    }

    private void verifyPending(Frame f) {
        Pending p = pending;
        if (p.proof.test(f)) {
            log.info("[PiratesTreasure] PROVED action={} varp={} pos={} phase={} items={}",
                p.key, f.varp, f.pos, phase, f.items);
            pending = null; failures.remove(p.key); missingKey = ""; missingSince = 0;
            onProved(p, f);
            long min = p.key.startsWith("dialogue:") || p.key.startsWith("talk:") ? 150 : 300;
            long max = p.key.startsWith("dialogue:") || p.key.startsWith("talk:") ? 450 : 850;
            if (f.varp != p.before.varp) { min = 500; max = 1200; }
            nextActionAt = System.currentTimeMillis()
                + ThreadLocalRandom.current().nextLong(min, max + 1);
            paceReason = "proved " + p.key;
            return;
        }
        if (System.currentTimeMillis() - p.at < p.timeout) return;
        int count = failures.merge(p.key, 1, Integer::sum);
        pending = null;
        if (count >= 3) hold("Unproved " + p.key + " after " + count
            + " attempts; varp=" + f.varp + ", pos=" + f.pos
            + ", items=" + f.items + ", dialogue=" + f.dialogue);
        else log.warn("[PiratesTreasure] RETRY action={} failure={}/3", p.key, count);
    }

    private void onProved(Pending p, Frame f) {
        if (p.key.equals("buy:rum")) phase = Phase.PICK_BANANAS;
        if (p.key.equals("talk:luthas-start")) talkStartedAt = System.currentTimeMillis();
        if (p.key.equals("stash:rum")) phase = Phase.FILL_CRATE;
        if (p.key.equals("fill:crate") && f.count(BANANA) == 0) phase = Phase.SHIP_CRATE;
        if (p.key.equals("talk:luthas-ship")) {
            shipTalkAt = System.currentTimeMillis();
        }
        if (phase == Phase.SHIP_CRATE && p.key.startsWith("dialogue:")) {
            shipTalkAt = System.currentTimeMillis();
            log.info("[PiratesTreasure] SHIP_DIALOGUE_PROGRESS action={} paymentTimeoutMs={}",
                p.key, SHIP_PAYMENT_TIMEOUT_MS);
        }
        if (p.key.equals("talk:wydin-job")) wydinTalkAt = System.currentTimeMillis();
        if (p.key.equals("loot:apron") || p.key.equals("take:apron")) phase = Phase.GET_APRON;
        if (p.key.equals("equip:apron")) phase = Phase.RETRIEVE_RUM;
        if (p.key.equals("search:wydin-crate")) phase = Phase.DELIVER_RUM;
        if (p.key.equals("dig:cross") && f.gardenerVisible) {
            gardenerAppeared = true; gardenerSeenAt = System.currentTimeMillis();
        }
    }

    private void issue(String key, Frame f, long timeout, Predicate<Frame> proof,
                       BooleanSupplier action) {
        boolean accepted = action.getAsBoolean();
        pending = new Pending(key, f, timeout, proof);
        log.info("[PiratesTreasure] ACTION key={} accepted={} varp={} pos={} phase={}",
            key, accepted, f.varp, f.pos, phase);
    }

    private static boolean dialogueChanged(Frame before, Frame after) {
        return before.varp != after.varp || before.inDialogue != after.inDialogue
            || before.hasContinue != after.hasContinue || !before.dialogue.equals(after.dialogue);
    }

    private void smuggle(Frame f) {
        if (onKaramja(f.pos) && phase == Phase.SAIL_OUT) phase = Phase.BUY_RUM;
        if (!onKaramja(f.pos) && f.count(RUM) > 0 && phase != Phase.SAIL_OUT)
            phase = Phase.DELIVER_RUM;
        switch (phase) {
            case SAIL_OUT:
                stage = "SAIL_TO_KARAMJA";
                if (f.count(COINS) < 60) {
                    hold("Need 60 coins for outward fare and rum; carried=" + f.count(COINS)); return;
                }
                if (voyageStarted != 0) {
                    if (System.currentTimeMillis() - voyageStarted < 18000) {
                        stage = "VERIFY_SAIL_OUT"; return;
                    }
                    voyageStarted = 0;
                }
                talk(f, "seaman-out", SAILOR_IDS, PORT_DOCK);
                return;
            case BUY_RUM:
                stage = "BUY_RUM";
                if (f.count(RUM) > 0) { phase = Phase.PICK_BANANAS; return; }
                if (f.count(COINS) < 30) { hold("Need 30 coins for Karamjan rum"); return; }
                if (f.freeSlots < 11) {
                    hold("Need 11 free inventory slots for rum and bananas; free=" + f.freeSlots); return;
                }
                if (route(f, "ZEMBO", ZEMBO, 4)) return;
                if (!f.shopOpen) {
                    Rs2NpcModel zembo = npc(ZEMBO_IDS, ZEMBO, 10);
                    if (zembo == null) { missingScene("Zembo 13655", 12000); return; }
                    issue("shop:zembo", f, 8000, after -> after.shopOpen,
                        () -> zembo.click("Trade"));
                    return;
                }
                if (!shopHasStock(RUM)) { missingScene("Zembo rum stock 431", 12000); return; }
                issue("buy:rum", f, 8000, after -> after.count(RUM) > f.count(RUM),
                    () -> Rs2Shop.buyItem(RUM, "1"));
                return;
            case PICK_BANANAS:
                stage = "PICK_BANANAS";
                if (f.shopOpen) {
                    issue("shop:close", f, 5000, after -> !after.shopOpen,
                        () -> { Rs2Shop.closeShop(); return true; }); return;
                }
                if (f.count(RUM) == 0) { hold("Rum missing before plantation crate"); return; }
                if (f.count(BANANA) >= 10) { phase = Phase.TALK_LUTHAS; return; }
                if (f.freeSlots == 0) { hold("No slot for banana " + (f.count(BANANA) + 1)); return; }
                if (route(f, "BANANA_PATCH", BANANA_PATCH, 6)) return;
                Rs2TileObjectModel tree = bananaTree(f);
                if (tree == null) { missingScene("reachable banana tree with Pick action", 12000); return; }
                issue("pick:banana", f, 9000,
                    after -> after.count(BANANA) > f.count(BANANA),
                    () -> tree.click("Pick"));
                return;
            case TALK_LUTHAS:
                stage = "TALK_LUTHAS";
                if (talkStartedAt != 0) {
                    if (System.currentTimeMillis() - talkStartedAt < 3000) return;
                    phase = Phase.STASH_RUM; talkStartedAt = 0; return;
                }
                talk(f, "luthas-start", LUTHAS_IDS, LUTHAS);
                return;
            case STASH_RUM:
                stage = "STASH_RUM";
                if (f.count(RUM) == 0) { phase = Phase.FILL_CRATE; return; }
                if (route(f, "PLANTATION_CRATE", PLANTATION, 3)) return;
                Rs2TileObjectModel crate = object(PLANTATION_CRATE, PLANTATION, 6);
                if (crate == null) { missingScene("plantation crate 2072", 12000); return; }
                issue("stash:rum", f, 10000, after -> after.count(RUM) < f.count(RUM),
                    () -> Rs2Inventory.useItemOnObject(RUM, PLANTATION_CRATE));
                return;
            case FILL_CRATE:
                stage = "FILL_CRATE";
                if (f.count(BANANA) == 0) { phase = Phase.SHIP_CRATE; return; }
                if (route(f, "PLANTATION_CRATE", PLANTATION, 3)) return;
                Rs2TileObjectModel fillCrate = object(PLANTATION_CRATE, PLANTATION, 6);
                if (fillCrate == null || objectAction(fillCrate, "Fill") == null) {
                    missingScene("plantation crate Fill action", 12000); return;
                }
                issue("fill:crate", f, 10000,
                    after -> after.count(BANANA) < f.count(BANANA),
                    () -> fillCrate.click("Fill"));
                return;
            case SHIP_CRATE:
                stage = "SHIP_CRATE";
                if (shipTalkAt != 0) {
                    if (f.count(COINS) >= shipCoinBefore + 30) {
                        phase = Phase.SAIL_HOME; shipTalkAt = 0; return;
                    }
                    if (System.currentTimeMillis() - shipTalkAt < SHIP_PAYMENT_TIMEOUT_MS) {
                        stage = "VERIFY_LUTHAS_SHIPMENT"; return;
                    }
                    hold("Luthas conversation made no proved dialogue/payment progress for 20s; coins="
                        + shipCoinBefore + "->" + f.count(COINS)); return;
                }
                shipCoinBefore = f.count(COINS);
                talk(f, "luthas-ship", LUTHAS_IDS, LUTHAS);
                return;
            case SAIL_HOME:
                stage = "SAIL_TO_PORT_SARIM";
                if (!onKaramja(f.pos)) { phase = Phase.GET_APRON; voyageStarted = 0; return; }
                if (f.count(COINS) < 30) { hold("Need 30 coins for return fare"); return; }
                if (voyageStarted != 0) {
                    if (System.currentTimeMillis() - voyageStarted < 18000) {
                        stage = "VERIFY_SAIL_HOME"; return;
                    }
                    voyageStarted = 0;
                }
                talk(f, "customs-home", CUSTOMS_IDS, CUSTOMS);
                return;
            case GET_APRON:
                stage = "GET_WHITE_APRON";
                if (f.equippedApron > 0) { phase = Phase.RETRIEVE_RUM; return; }
                if (f.count(APRON) > 0) {
                    issue("equip:apron", f, 6000, after -> after.equippedApron > 0,
                        () -> Rs2Inventory.wear(APRON)); return;
                }
                if (route(f, "WHITE_APRON", APRON_SPAWN, 4)) return;
                if (Rs2GroundItem.exists(APRON, 12)) {
                    issue("loot:apron", f, 9000,
                        after -> after.count(APRON) > f.count(APRON),
                        () -> Rs2GroundItem.loot(APRON, 12)); return;
                }
                Rs2TileObjectModel hanging = object(7957, APRON_SPAWN, 8);
                if (hanging == null || objectAction(hanging, "Take") == null) {
                    missingScene("white apron spawn 1005/7957", 12000); return;
                }
                issue("take:apron", f, 9000,
                    after -> after.count(APRON) > f.count(APRON),
                    () -> hanging.click("Take"));
                return;
            case RETRIEVE_RUM:
                stage = "RETRIEVE_SMUGGLED_RUM";
                if (f.count(RUM) > 0) { phase = Phase.DELIVER_RUM; return; }
                if (f.equippedApron == 0) { phase = Phase.GET_APRON; return; }
                if (route(f, "WYDIN_FRONT", WYDIN_FRONT, 4)) return;
                if (wydinTalkAt == 0) {
                    talk(f, "wydin-job", new int[]{2890, 1791}, WYDIN_FRONT); return;
                }
                if (!f.wydinDoorOpen) {
                    Rs2TileObjectModel door = object(WYDIN_DOOR_CLOSED, WYDIN_INTERIOR, 8);
                    if (door == null || objectAction(door, "Open") == null) {
                        missingScene("Wydin closed door 2069/Open", 12000); return;
                    }
                    issue("open:wydin-door", f, 9000,
                        after -> after.wydinDoorOpen,
                        () -> door.click("Open"));
                    return;
                }
                WorldPoint access = reachableAccess(f.pos, WYDIN_INTERIOR, 1, 9);
                if (access == null) {
                    missingScene("reachable Wydin back-room tile adjacent crate 2071", 12000); return;
                }
                if (route(f, "WYDIN_CRATE_ACCESS", access, 0)) return;
                Rs2TileObjectModel foodCrate = object(WYDIN_CRATE, WYDIN_INTERIOR, 6);
                if (foodCrate == null) { missingScene("Wydin back-room crate 2071", 12000); return; }
                issue("search:wydin-crate", f, 10000,
                    after -> after.count(RUM) > f.count(RUM),
                    () -> foodCrate.click("Search"));
                return;
            case DELIVER_RUM:
                stage = "DELIVER_RUM_FRANK";
                if (onKaramja(f.pos)) { hold("Cannot deliver rum while still on Karamja"); return; }
                if (f.count(RUM) == 0) { phase = Phase.RETRIEVE_RUM; return; }
                talk(f, "frank-rum", FRANK_IDS, FRANK);
                return;
            default: hold("Unknown smuggling phase " + phase);
        }
    }

    private void chest(Frame f) {
        stage = "BLUE_MOON_CHEST";
        if (f.count(KEY) == 0) {
            hold("Quest stage 2 but no chest key 432 in inventory; items=" + f.items); return;
        }
        if (f.pos.getPlane() == 0) {
            if (route(f, "BLUE_MOON_STAIRS", BLUE_MOON, 3)) return;
            Rs2TileObjectModel stairs = object(BLUE_MOON_STAIRS, BLUE_MOON, 7);
            if (stairs == null) { missingScene("Blue Moon Inn stairs 11796", 12000); return; }
            String action = objectAction(stairs, "Climb-up", "Climb");
            if (action == null) { missingScene("Blue Moon stairs climb action", 12000); return; }
            issue("climb:blue-moon", f, 11000,
                after -> after.pos != null && after.pos.getPlane() == 1,
                () -> stairs.click(action));
            return;
        }
        if (f.pos.getPlane() != 1) { hold("Unexpected plane at Blue Moon stage " + f.pos); return; }
        if (route(f, "BLUE_MOON_CHEST", BLUE_MOON_UP, 3)) return;
        Rs2TileObjectModel box = object(BLUE_MOON_CHEST, BLUE_MOON_UP, 7);
        if (box == null) { missingScene("Blue Moon chest 2079", 12000); return; }
        issue("open:blue-moon-chest", f, 11000,
            after -> after.varp > f.varp || after.count(KEY) < f.count(KEY)
                || dialogueChanged(f, after),
            () -> Rs2Inventory.useItemOnObject(KEY, BLUE_MOON_CHEST));
    }

    private void dig(Frame f) {
        stage = "FALADOR_PARK_TREASURE";
        if (f.count(SPADE) == 0) {
            if (route(f, "FALADOR_SPADE", FALADOR_SPADE, 4)) return;
            if (!Rs2GroundItem.exists(SPADE, 12)) {
                missingScene("Falador spade 952 at 2982,3369", 12000); return;
            }
            issue("loot:spade", f, 9000,
                after -> after.count(SPADE) > f.count(SPADE),
                () -> Rs2GroundItem.loot(SPADE, 12));
            return;
        }
        if (route(f, "FALADOR_CROSS", FALADOR_CROSS, 0)) return;
        if (f.gardenerVisible) {
            gardenerAppeared = true; gardenerSeenAt = System.currentTimeMillis();
            if (f.inCombat) { stage = "WAIT_GARDENER_COMBAT"; return; }
            if (attackAttempts >= 3) { hold("Gardener resisted three attack attempts"); return; }
            Rs2NpcModel gardener = npc(GARDENER_IDS, FALADOR_CROSS, 10);
            if (gardener == null) { missingScene("quest gardener 3651", 12000); return; }
            attackAttempts++;
            issue("attack:gardener", f, 10000,
                after -> after.inCombat || !after.gardenerVisible || after.finished(),
                () -> gardener.click("Attack"));
            return;
        }
        if (gardenerAppeared && System.currentTimeMillis() - gardenerSeenAt < 3000) {
            stage = "VERIFY_GARDENER_DEATH"; return;
        }
        if (digAttempts >= 3) { hold("Three digs at exact Falador cross without quest completion"); return; }
        digAttempts++;
        issue("dig:cross", f, 12000,
            after -> after.finished() || after.varp != f.varp || after.gardenerVisible,
            () -> Rs2Inventory.interact(SPADE, "Dig"));
    }

    private void talk(Frame f, String key, int[] ids, WorldPoint point) {
        if (route(f, key.toUpperCase(), point, 4)) return;
        Rs2NpcModel target = npc(ids, point, 10);
        if (target == null) { missingScene("NPC " + key, 12000); return; }
        if (!target.hasLineOfSight()) {
            WorldPoint access = reachableAccess(f.pos, target.getWorldLocation(), 2, 9);
            if (access == null) { missingScene("reachable NPC access " + key, 12000); return; }
            if (route(f, key.toUpperCase() + "_APPROACH", access, 0)) return;
            missingScene("NPC line of sight after reachable approach " + key, 12000); return;
        }
        issue("talk:" + key, f, 9000, after -> dialogueChanged(f, after),
            () -> target.click("Talk-to"));
    }

    private boolean dialogue(Frame f) {
        if (!f.inDialogue && f.options.isEmpty() && !f.hasContinue) {
            unknownDialogueSince = 0;
            if (talkStartedAt != 0 && phase == Phase.TALK_LUTHAS) {
                if (System.currentTimeMillis() - talkStartedAt > 1500) {
                    phase = Phase.STASH_RUM; talkStartedAt = 0;
                }
            }
            if (shipTalkAt != 0 && phase == Phase.SHIP_CRATE) {
                if (f.count(COINS) >= shipCoinBefore + 30) {
                    log.info("[PiratesTreasure] PAYMENT_PROVED after dialogue closed coins={}->{}",
                        shipCoinBefore, f.count(COINS));
                    phase = Phase.SAIL_HOME; shipTalkAt = 0;
                }
            }
            return false;
        }
        if (!f.options.isEmpty()) {
            String[] allowed = {
                "i'm in search of treasure", "yes", "yes please",
                "could you offer me employment on your plantation",
                "will you pay me for another crate full", "thank you, i'll be on my way",
                "can i journey on this ship", "search away", "ok", "okay",
                "well, can i get a job here", "ok thanks, i'll go and get it"
            };
            for (String fragment : allowed) for (String option : f.options)
                if ((!fragment.equals("yes") || f.varp == 0)
                    && (fragment.equals("yes") ? normalize(option).equals("yes")
                        : normalize(option).contains(fragment))) {
                    unknownDialogueSince = 0;
                    if (phase == Phase.SAIL_OUT || phase == Phase.SAIL_HOME)
                        voyageStarted = System.currentTimeMillis();
                    if (phase == Phase.RETRIEVE_RUM) wydinTalkAt = System.currentTimeMillis();
                    issue("dialogue:" + fragment, f, 7500,
                        after -> dialogueChanged(f, after),
                        () -> Rs2Dialogue.clickOption(option));
                    return true;
                }
            if (unknownDialogueSince == 0) unknownDialogueSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - unknownDialogueSince > 12000)
                hold("Unexpected Pirate's Treasure choice varp=" + f.varp
                    + " phase=" + phase + " options=" + f.options);
            return true;
        }
        if (f.hasContinue) {
            unknownDialogueSince = 0;
            issue("dialogue:continue", f, 7500, after -> dialogueChanged(f, after),
                () -> { Rs2Dialogue.clickContinue(); return true; });
            return true;
        }
        if (unknownDialogueSince == 0) unknownDialogueSince = System.currentTimeMillis();
        if (System.currentTimeMillis() - unknownDialogueSince > 20000)
            hold("Dialogue has no known option or Continue: " + f.dialogue);
        return true;
    }

    /** The installed walker handles obstacles; this plugin bounds its ownership. */
    private boolean route(Frame f, String key, WorldPoint target, int radius) {
        if (route != null && route.cancelRequested) {
            Route prior = route;
            if (!cancelRoute()) return true;
            if (prior.failureReason != null) finishRouteFailure(f, prior);
            return true;
        }
        if (route != null && (!route.key.equals(key) || !route.target.equals(target))) {
            route.cancelReason = "target changed to " + key; cancelRoute(); return true;
        }
        if (recoveryTarget != null) {
            if (!recoveryKey.equals(key)) { recoveryTarget = recoveryBefore = null; }
            else {
                if (!f.pos.equals(recoveryBefore)) {
                    failures.remove("walk:" + key); recoveryTarget = recoveryBefore = null;
                    recoveryKey = ""; return true;
                }
                if (System.currentTimeMillis() - recoveryAt > 8000)
                    hold("Route recovery " + key + " did not move from " + f.pos);
                else stage = "VERIFY_ROUTE_RECOVERY";
                return true;
            }
        }
        int dist = distance(f.pos, target);
        if (dist <= radius) {
            if (route != null) {
                route.cancelReason = "arrived " + key;
                if (!cancelRoute()) return true;
                failures.remove("walk:" + key);
                nextActionAt = System.currentTimeMillis()
                    + ThreadLocalRandom.current().nextLong(300, 851);
                paceReason = "proved arrival " + key;
                stage = "ARRIVED_" + key; return true;
            }
            return false;
        }
        stage = "WALK_" + key;
        if (route == null) {
            Route r = new Route(key, target, radius, f.pos);
            route = r; startRouteSegment(r);
            log.info("[PiratesTreasure] ROUTE start={} from={} target={} radius={}",
                key, f.pos, target, radius);
            return true;
        }
        Route r = route; long now = System.currentTimeMillis();
        if (!f.pos.equals(r.lastPosition)) {
            r.lastPosition = f.pos; r.lastProgress = now;
        }
        if (now - r.started > 240000 || now - r.lastProgress > 20000) {
            routeFailure(f, "route timed out/no progress", r); return true;
        }
        if (r.completed && (r.worker == null || !r.worker.isAlive())
            && now - r.completedAt >= 1500) {
            // A legitimate detour may increase distance first. Position change is proof.
            if (!f.pos.equals(r.segmentStart)) startRouteSegment(r);
            else routeFailure(f, "walker segment ended without position progress", r);
        }
        return true;
    }

    private void startRouteSegment(Route r) {
        r.segmentStarted = System.currentTimeMillis();
        r.segmentStart = r.lastPosition; r.completed = false; r.completedAt = 0;
        Thread t = new Thread(() -> {
            try {
                Rs2Walker.walkWithStateUntil(r.target, r.radius,
                    () -> stopped || Thread.currentThread().isInterrupted()
                        || System.currentTimeMillis() - r.segmentStarted >= 15000);
            } finally {
                r.completedAt = System.currentTimeMillis(); r.completed = true;
            }
        }, "PiratesTreasure-route");
        t.setDaemon(true); r.worker = t; t.start();
    }

    private void routeFailure(Frame f, String reason, Route r) {
        r.failureReason = reason; r.cancelReason = "failure " + reason;
        if (!cancelRoute()) return;
        finishRouteFailure(f, r);
    }

    private void finishRouteFailure(Frame f, Route r) {
        int count = failures.merge("walk:" + r.key, 1, Integer::sum);
        log.warn("[PiratesTreasure] ROUTE failure={} attempt={}/3 at={} target={}",
            r.failureReason, count, f.pos, r.target);
        if (count >= 3) hold("Route " + r.key + " failed after " + count
            + " attempts: " + r.failureReason + "; at=" + f.pos + ", target=" + r.target);
        else if (!nearbyRecovery(f, r)) Rs2Walker.recalculatePath();
    }

    private boolean nearbyRecovery(Frame f, Route r) {
        Map<WorldPoint, Integer> reachable = Rs2Tile.getReachableTilesFromTile(f.pos, 3);
        if (reachable == null || reachable.isEmpty()) return false;
        WorldPoint best = null; int current = distance(f.pos, r.target);
        for (WorldPoint candidate : reachable.keySet()) {
            if (candidate == null || candidate.equals(f.pos)
                || distance(f.pos, candidate) > 3
                || distance(candidate, r.target) >= current) continue;
            if (best == null || distance(candidate, r.target) < distance(best, r.target))
                best = candidate;
        }
        if (best == null || !Rs2Walker.walkFastCanvas(best)) return false;
        recoveryTarget = best; recoveryBefore = f.pos; recoveryKey = r.key;
        recoveryAt = System.currentTimeMillis(); stage = "VERIFY_ROUTE_RECOVERY";
        return true;
    }

    private boolean cancelRoute() {
        Route r = route;
        if (r == null) return true;
        if (!r.cancelRequested) {
            r.cancelRequested = true; r.cancelAt = System.currentTimeMillis();
            if (r.cancelReason == null) r.cancelReason = "state gate";
            if (r.worker != null && r.worker.isAlive()) r.worker.interrupt();
            Thread clear = new Thread(() -> {
                try { Rs2Walker.clearWalkingRoute("piratestreasure:" + r.cancelReason); }
                catch (Exception ex) {
                    r.clearError = ex.toString();
                    log.error("[PiratesTreasure] route clear failed", ex);
                }
            }, "PiratesTreasure-route-clear");
            clear.setDaemon(true); r.clearWorker = clear; clear.start();
        }
        if ((r.worker != null && r.worker.isAlive())
            || (r.clearWorker != null && r.clearWorker.isAlive())) {
            if (System.currentTimeMillis() - r.cancelAt > 60000 && error.isEmpty()) {
                error = "Walker still active 60s after cancel; target=" + r.target;
                stage = "HOLD";
            } else if (error.isEmpty()) stage = "WAIT_ROUTE_STOP";
            return false;
        }
        route = null;
        if (r.clearError != null && error.isEmpty()) {
            error = "Walker clear failed for " + r.target + ": " + r.clearError;
            stage = "HOLD"; return false;
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
        issue("eat:" + id, f, 5000,
            after -> after.count(id) < f.count(id),
            () -> Rs2Inventory.interact(id, "Eat"));
        return true;
    }

    private static Rs2NpcModel npc(int[] ids, WorldPoint point, int radius) {
        return Microbot.getRs2NpcCache().query().withIds(ids)
            .within(point, radius).nearestOnClientThread();
    }
    private static WorldPoint reachableAccess(WorldPoint from, WorldPoint target,
                                              int radius, int scanRadius) {
        if (from == null || target == null || from.getPlane() != target.getPlane()) return null;
        Map<WorldPoint, Integer> reachable = Rs2Tile.getReachableTilesFromTile(from, scanRadius);
        if (reachable == null || reachable.isEmpty()) return null;
        WorldPoint best = null;
        for (WorldPoint candidate : reachable.keySet()) {
            if (candidate == null || distance(candidate, target) > radius) continue;
            if (best == null || distance(from, candidate) < distance(from, best))
                best = candidate;
        }
        return best;
    }
    private static Rs2TileObjectModel object(int id, WorldPoint point, int radius) {
        return Microbot.getRs2TileObjectCache().query().withId(id)
            .within(point, radius).nearestOnClientThread();
    }
    private static Rs2TileObjectModel bananaTree(Frame f) {
        List<Rs2TileObjectModel> choices = Microbot.getRs2TileObjectCache().query()
            .within(BANANA_PATCH, 15).toListOnClientThread();
        List<Rs2TileObjectModel> reachable = new ArrayList<>();
        for (Rs2TileObjectModel candidate : choices)
            if (candidate != null && normalize(candidate.getName()).contains("banana")
                && objectAction(candidate, "Pick") != null && candidate.isReachable())
                reachable.add(candidate);
        if (reachable.isEmpty()) return null;
        reachable.sort((a, b) -> Integer.compare(
            distance(f.pos, a.getWorldLocation()), distance(f.pos, b.getWorldLocation())));
        return reachable.get(Math.min(f.count(BANANA) % 3, reachable.size() - 1));
    }
    private static String objectAction(Rs2TileObjectModel object, String... wanted) {
        return Microbot.getClientThread().invoke(() -> {
            if (object.getObjectComposition() == null) return null;
            String[] actions = object.getObjectComposition().getActions();
            if (actions == null) return null;
            for (String preference : wanted) for (String action : actions)
                if (action != null && action.equalsIgnoreCase(preference)) return action;
            return null;
        });
    }
    private static boolean shopHasStock(int id) {
        List<Rs2ItemModel> items = Rs2Shop.shopItems;
        if (items == null) return false;
        for (Rs2ItemModel item : items)
            if (item != null && item.getId() == id && item.getQuantity() > 0) return true;
        return false;
    }
    private static boolean onKaramja(WorldPoint p) {
        return p != null && p.getX() < 2970 && p.getY() >= 2879 && p.getY() <= 3187;
    }
    private void missingScene(String key, long timeout) {
        if (!key.equals(missingKey)) { missingKey = key; missingSince = System.currentTimeMillis(); }
        if (System.currentTimeMillis() - missingSince > timeout)
            hold("Target absent from loaded scene: " + key);
    }
    private void hold(String reason) {
        if (error.isEmpty()) log.error("[PiratesTreasure] HOLD {}", reason);
        error = reason; stage = "HOLD"; paceReason = ""; cancelRoute();
    }
    private static boolean near(WorldPoint a, WorldPoint b, int radius) {
        return distance(a, b) <= radius;
    }
    private static int distance(WorldPoint a, WorldPoint b) {
        if (a == null || b == null || a.getPlane() != b.getPlane()) return Integer.MAX_VALUE;
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }
    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("<[^>]*>", "")
            .replace('\u2019', '\'').trim().toLowerCase();
    }
    private static String fingerprint(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : bytes) {
                int n = b & 255;
                hex.append(Character.forDigit(n >>> 4, 16));
                hex.append(Character.forDigit(n & 15, 16));
            }
            return hex.toString();
        } catch (Exception ex) { return ""; }
    }
    private void restoreCheckpoint(Frame f) {
        if (f.varp == 0) {
            phase = Phase.SAIL_OUT;
            try { Files.deleteIfExists(STATUS_DIR.resolve("checkpoint.properties")); }
            catch (Exception ex) { log.warn("[PiratesTreasure] checkpoint reset: {}", ex.toString()); }
            return;
        }
        if (f.varp != 1) return;
        Path checkpoint = STATUS_DIR.resolve("checkpoint.properties");
        try {
            if (Files.isRegularFile(checkpoint) && !f.accountKey.isEmpty()) {
                Properties p = new Properties();
                try (InputStream in = Files.newInputStream(checkpoint)) { p.load(in); }
                if (f.accountKey.equals(p.getProperty("accountKey"))
                    && "1".equals(p.getProperty("questVarp"))) {
                    Phase saved = Phase.valueOf(p.getProperty("phase", "SAIL_OUT"));
                    boolean island = onKaramja(f.pos);
                    if (island && saved.ordinal() <= Phase.SAIL_HOME.ordinal()
                        || !island && (saved == Phase.SAIL_OUT
                            || saved.ordinal() >= Phase.SAIL_HOME.ordinal())) {
                        phase = saved;
                        if (saved == Phase.SAIL_HOME && !island) phase = Phase.GET_APRON;
                        if (saved == Phase.SAIL_OUT && island) phase = Phase.BUY_RUM;
                        if (saved == Phase.SHIP_CRATE) {
                            int before = Integer.parseInt(p.getProperty("shipCoinBefore", "-1"));
                            boolean attempted = Boolean.parseBoolean(p.getProperty("shipAttempted", "false"));
                            if (attempted) {
                                if (before >= 0 && f.count(COINS) >= before + 30)
                                    phase = Phase.SAIL_HOME;
                                else {
                                    hold("Luthas shipment was in flight at relog; payment unproved, coins="
                                        + before + "->" + f.count(COINS));
                                    return;
                                }
                            }
                        }
                        log.info("[PiratesTreasure] RESTORED phase={} varp=1 region={}",
                            phase, island ? "KARAMJA" : "MAINLAND");
                        return;
                    }
                    hold("Checkpoint phase/region conflict: saved=" + saved
                        + ", island=" + island + ", items=" + f.items);
                    return;
                }
            }
        } catch (Exception ex) {
            log.warn("[PiratesTreasure] checkpoint read: {}", ex.toString());
        }
        // Only unambiguous inventory/region combinations are safe to reconstruct.
        if (onKaramja(f.pos)) {
            if (f.count(RUM) > 0) phase = f.count(BANANA) >= 10
                ? Phase.TALK_LUTHAS : Phase.PICK_BANANAS;
            else if (f.count(BANANA) > 0) phase = Phase.FILL_CRATE;
            else hold("Cannot reconstruct Karamja rum substage without checkpoint");
        } else if (f.count(RUM) > 0) phase = Phase.DELIVER_RUM;
        else if (f.equippedApron > 0) phase = Phase.RETRIEVE_RUM;
        else hold("Cannot reconstruct mainland rum substage without checkpoint");
    }
    private void writeCheckpoint(Frame f) {
        if (!checkpointLoaded || f == null || !f.loggedIn() || f.varp != 1
            || !error.isEmpty()
            || f.accountKey.isEmpty()) return;
        boolean shipAttempted = phase == Phase.SHIP_CRATE
            && (shipTalkAt != 0 || pending != null && (
                pending.key.equals("talk:luthas-ship")
                || pending.key.startsWith("dialogue:")));
        String snapshot = f.accountKey + ':' + phase + ':' + f.count(RUM) + ':'
            + f.count(BANANA) + ':' + f.count(COINS) + ':' + f.equippedApron
            + ':' + shipCoinBefore + ':' + shipAttempted;
        if (snapshot.equals(lastCheckpointSnapshot)) return;
        try {
            Files.createDirectories(STATUS_DIR);
            Properties p = new Properties();
            p.setProperty("accountKey", f.accountKey);
            p.setProperty("questVarp", "1");
            p.setProperty("phase", phase.name());
            p.setProperty("rum", Integer.toString(f.count(RUM)));
            p.setProperty("bananas", Integer.toString(f.count(BANANA)));
            p.setProperty("coins", Integer.toString(f.count(COINS)));
            p.setProperty("equippedApron", Integer.toString(f.equippedApron));
            p.setProperty("shipCoinBefore", Integer.toString(shipCoinBefore));
            p.setProperty("shipAttempted", Boolean.toString(shipAttempted));
            Path tmp = STATUS_DIR.resolve("checkpoint.tmp");
            try (OutputStream out = Files.newOutputStream(tmp))
                { p.store(out, "Private Pirate's Treasure phase checkpoint"); }
            try { Files.move(tmp, STATUS_DIR.resolve("checkpoint.properties"),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(tmp,
                STATUS_DIR.resolve("checkpoint.properties"), StandardCopyOption.REPLACE_EXISTING); }
            lastCheckpointSnapshot = snapshot;
        } catch (Exception ex) { log.warn("[PiratesTreasure] checkpoint write: {}", ex.toString()); }
    }
    private static String loadedClassSha256() {
        try (InputStream in = PiratesTreasureScript.class.getResourceAsStream("PiratesTreasureScript.class")) {
            if (in == null) return "UNKNOWN";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) >= 0) if (count > 0) digest.update(buffer, 0, count);
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest.digest()) {
                int unsigned = value & 0xff;
                hex.append(Character.forDigit(unsigned >>> 4, 16));
                hex.append(Character.forDigit(unsigned & 15, 16));
            }
            return hex.toString();
        } catch (Exception ex) {
            log.warn("[PiratesTreasure] class SHA-256 unavailable: {}", ex.toString());
            return "UNKNOWN";
        }
    }
    private void writeStatus(Frame f) {
        synchronized (statusLock) {
        if (stopped) return;
        try {
            Files.createDirectories(STATUS_DIR);
            Properties p = new Properties();
            p.setProperty("build", Integer.toString(BUILD_NUMBER));
            p.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
            p.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
            p.setProperty("questVarp", f == null ? "-1" : Integer.toString(f.varp));
            p.setProperty("questState", f == null ? "UNKNOWN" : f.questState);
            p.setProperty("stage", stage);
            p.setProperty("currentWorld", f == null ? "0" : Integer.toString(f.world));
            p.setProperty("gameState", f == null ? "UNKNOWN" : f.gameState);
            p.setProperty("error", error);
            p.setProperty("position", f == null ? "UNKNOWN" : String.valueOf(f.pos));
            p.setProperty("healthPercent", f == null ? "-1" : Double.toString(f.health));
            p.setProperty("sha256", classSha256);
            p.setProperty("phase", phase.name());
            p.setProperty("rum", f == null ? "0" : Integer.toString(f.count(RUM)));
            p.setProperty("bananas", f == null ? "0" : Integer.toString(f.count(BANANA)));
            p.setProperty("key", f == null ? "0" : Integer.toString(f.count(KEY)));
            p.setProperty("message", f == null ? "0" : Integer.toString(f.count(MESSAGE)));
            p.setProperty("spade", f == null ? "0" : Integer.toString(f.count(SPADE)));
            p.setProperty("apron", f == null ? "0" : Integer.toString(f.count(APRON)));
            p.setProperty("equippedApron", f == null ? "0" : Integer.toString(f.equippedApron));
            p.setProperty("coins", f == null ? "0" : Integer.toString(f.count(COINS)));
            p.setProperty("freeSlots", f == null ? "0" : Integer.toString(f.freeSlots));
            p.setProperty("pending", pending == null ? "" : pending.key);
            p.setProperty("walkTarget", route == null ? "" : String.valueOf(route.target));
            p.setProperty("paceReason", paceReason);
            p.setProperty("paceRemainingMs", Long.toString(Math.max(0,
                nextActionAt - System.currentTimeMillis())));
            Path temp = STATUS_DIR.resolve("status.tmp");
            try (OutputStream out = Files.newOutputStream(temp))
                { p.store(out, "Pirate's Treasure live script"); }
            Path status = STATUS_DIR.resolve("status.properties");
            try { Files.move(temp, status,
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, status,
                StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception ex) { log.warn("[PiratesTreasure] status write: {}", ex.toString()); }
        }
    }
}
