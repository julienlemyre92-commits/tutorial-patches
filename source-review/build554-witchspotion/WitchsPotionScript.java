package net.runelite.client.plugins.microbot.witchspotion;

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
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Installed Quest Helper's varp-67 route. An action is followed by fresh observation. */
public class WitchsPotionScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(WitchsPotionScript.class);
    public static final int BUILD_NUMBER = 554;
    private static final int QUEST_VARP = 67;
    private static final int RAT_TAIL = 300, ONION = 1957, BURNT_MEAT = 2146;
    private static final int EYE_OF_NEWT = 221, RAW_BEEF = 2132, COOKED_MEAT = 2142;
    private static final int COINS = 995, RANGE_ID = 9682, CAULDRON_ID = 2024;
    private static final int[] HETTY_IDS = {4619}, RAT_IDS = {2855};
    private static final int[] WYDIN_IDS = {2890, 1791}, BETTY_IDS = {5905, 1788};
    private static final int[] ONION_IDS = {3366, 5538};
    private static final WorldPoint HETTY = new WorldPoint(2968, 3205, 0);
    private static final WorldPoint HETTY_INTERIOR = new WorldPoint(2968, 3206, 0);
    private static final WorldPoint RAT_HOUSE = new WorldPoint(2956, 3203, 0);
    private static final WorldPoint ONION_FIELD = new WorldPoint(2950, 3253, 0);
    private static final WorldPoint WYDIN = new WorldPoint(3013, 3204, 0);
    private static final WorldPoint BETTY = new WorldPoint(3013, 3261, 0);
    // The adjacent chimney is at 2971,3211. Locate the live range object in its house.
    private static final WorldPoint RANGE_HOUSE = new WorldPoint(2970, 3211, 0);
    private static final Path STATUS_DIR = Paths.get(System.getProperty("user.home"),
        ".runelite", "witchspotion");

    private enum Proof {
        TALK, DIALOGUE, ATTACK_RAT, LOOT_TAIL, ONION, SHOP_OPEN, SHOP_CLOSE,
        BUY_BEEF, BUY_NEWT, COOK_OPEN, COOK_BEEF, BURN_OPEN, BURN_MEAT, CAULDRON, EAT
    }

    private static final class Frame {
        String gameState = "NO_CLIENT", questState = "UNKNOWN", dialogue = "";
        WorldPoint pos;
        int world, varp = -1, freeSlots;
        boolean inventoryLoaded, inDialogue, hasContinue, shopOpen, production, tailGround, inCombat;
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
        Thread worker;
        volatile boolean completed;
        volatile long completedAt;
        long segmentStarted, lastProgress;
        WorldPoint segmentStart, lastPosition;
        int segmentDistance, bestDistance;
        boolean cancelRequested;
        long cancelAt;
        String cancelReason, failureReason;
        Thread clearWorker;
        volatile String clearError;
        Route(String key, WorldPoint target, int radius, WorldPoint pos) {
            this.key = key; this.target = target; this.radius = radius;
            this.started = this.lastProgress = System.currentTimeMillis();
            this.bestDistance = distance(pos, target);
            this.lastPosition = pos;
        }
    }

    private final Map<String, Integer> failures = new HashMap<>();
    private volatile boolean stopped;
    private volatile String stage = "STARTING", error = "";
    private BooleanSupplier ownsInput;
    private Pending pending;
    private Route route;
    private WorldPoint recoveryTarget, recoveryBefore;
    private String recoveryKey = "", missingKey = "", paceReason = "", classSha256 = "UNKNOWN";
    private long recoveryAt, nextActionAt, unknownDialogueSince, missingSince, loggedInAt;
    private long shopStockSince, finishStageSince, ratEngagedAt, ingredientHandInAt;
    private int priorVarp = -1, ratAttempts, ingredientTalkCount;

    public boolean run(WitchsPotionConfig config, BooleanSupplier exclusiveInput) {
        if (isRunning()) return true;
        stopped = false; ownsInput = exclusiveInput;
        error = ""; stage = "STARTING"; pending = null; route = null;
        failures.clear(); priorVarp = -1; ratAttempts = ingredientTalkCount = 0;
        ratEngagedAt = ingredientHandInAt = finishStageSince = shopStockSince = 0;
        loggedInAt = nextActionAt = unknownDialogueSince = missingSince = recoveryAt = 0;
        recoveryTarget = recoveryBefore = null; recoveryKey = missingKey = paceReason = "";
        classSha256 = loadedClassSha256();
        log.info("[WitchsPotion] RUNNING_BUILD={} quest=WITCHS_POTION classSha256={}",
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
        catch (Exception ex) { log.warn("[WitchsPotion] status cleanup: {}", ex.toString()); }
    }

    private void tick() {
        if (stopped || Thread.currentThread().isInterrupted()) return;
        Frame f = null;
        try {
            f = observe();
            if (!f.loggedIn()) {
                cancelRoute(); pending = null; loggedInAt = 0; stage = "WAIT_LOGIN"; return;
            }
            long now = System.currentTimeMillis();
            if (loggedInAt == 0) loggedInAt = now;
            if (!f.inventoryLoaded || now - loggedInAt < 3000) {
                stage = "WAIT_INVENTORY"; return;
            }
            boolean changedStage = priorVarp >= 0 && f.varp != priorVarp;
            if (f.varp >= 0 && f.varp != priorVarp) {
                priorVarp = f.varp; ratEngagedAt = 0; ratAttempts = 0;
                ingredientHandInAt = finishStageSince = 0; ingredientTalkCount = 0;
            }
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
            if (changedStage && route != null) {
                route.cancelReason = "quest stage changed to " + f.varp;
                cancelRoute(); return;
            }
            if (f.health > 0 && f.health < 30 && pending != null
                && pending.proof != Proof.EAT && pending.proof != Proof.ATTACK_RAT) {
                log.warn("[WitchsPotion] SURVIVAL preempt={} health={}", pending.key, f.health);
                pending = null;
            }
            if (pending != null) { verifyPending(f); return; }
            if (f.health > 0 && f.health < 30 && safety(f)) return;
            if (route != null) { route(f, route.key, route.target, route.radius); return; }
            if (now < nextActionAt) { stage = "WAIT_PACE"; return; }
            paceReason = "";
            if (f.varp < 0 || "UNKNOWN".equals(f.questState)) {
                hold("Unknown Witch's Potion state/varp: " + f.questState + "/" + f.varp);
                return;
            }
            if (f.varp > 2) {
                if (finishStageSince == 0) finishStageSince = now;
                if (dialogue(f)) return;
                if (now - finishStageSince > 30000)
                    hold("Varp reached " + f.varp + " without QuestState.FINISHED");
                else stage = "VERIFY_FINISH";
                return;
            }
            if (f.shopOpen && !needsOpenShop(f)) {
                issue("shop:close", Proof.SHOP_CLOSE, f, 5000, 0,
                    () -> { Rs2Shop.closeShop(); return true; });
                return;
            }
            if (dialogue(f)) return;
            if (f.varp == 0) { stage = "START_HETTY"; talkHetty(f, "start"); return; }
            if (f.varp == 1) { stageOne(f); return; }
            if (f.varp == 2) { drinkCauldron(f); return; }
            hold("Unmapped Witch's Potion varp " + f.varp);
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted()) return;
            hold("Tick exception: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            log.error("[WitchsPotion] tick failed", ex);
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
            QuestState qs = Quest.WITCHS_POTION.getState(c);
            s.questState = qs == null ? "UNKNOWN" : qs.name();
            Widget primary = c.getWidget(270, 14);
            Widget alternate = c.getWidget(300, 16);
            s.production = primary != null && !primary.isHidden()
                || alternate != null && !alternate.isHidden();
            ItemContainer inv = c.getItemContainer(InventoryID.INVENTORY);
            s.inventoryLoaded = inv != null;
            if (inv != null) {
                int occupied = 0;
                for (Item item : inv.getItems()) {
                    if (item == null || item.getId() < 0) continue;
                    occupied++;
                    s.items.merge(item.getId(), item.getQuantity(), Integer::sum);
                }
                s.freeSlots = Math.max(0, 28 - occupied);
            }
            return s;
        });
        if (f.loggedIn()) {
            f.health = Rs2Player.getHealthPercentage();
            f.shopOpen = Rs2Shop.isOpen();
            f.inCombat = Rs2Combat.inCombat();
            f.tailGround = distance(f.pos, RAT_HOUSE) < 20
                && Rs2GroundItem.exists(RAT_TAIL, 15);
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
            log.info("[WitchsPotion] PROVED action={} varp={} pos={} tail={} onion={} meat={} newt={}",
                p.key, f.varp, f.pos, f.count(RAT_TAIL), f.count(ONION),
                f.count(BURNT_MEAT), f.count(EYE_OF_NEWT));
            pending = null; failures.remove(p.key); missingKey = ""; missingSince = 0;
            if (p.proof == Proof.TALK && p.key.equals("talk:hetty-ingredients")) {
                ingredientTalkCount++;
                ingredientHandInAt = System.currentTimeMillis();
            }
            long min = p.proof == Proof.TALK || p.proof == Proof.DIALOGUE ? 150 : 300;
            long max = p.proof == Proof.TALK || p.proof == Proof.DIALOGUE ? 450 : 850;
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
        else log.warn("[WitchsPotion] RETRY action={} failure={}/3", p.key, count);
    }

    private boolean proved(Pending p, Frame f) {
        switch (p.proof) {
            case TALK: case DIALOGUE:
                return f.finished() || f.varp != p.before.varp
                    || f.count(RAT_TAIL) != p.before.count(RAT_TAIL)
                    || f.count(ONION) != p.before.count(ONION)
                    || f.count(BURNT_MEAT) != p.before.count(BURNT_MEAT)
                    || f.count(EYE_OF_NEWT) != p.before.count(EYE_OF_NEWT)
                    || f.inDialogue != p.before.inDialogue || f.hasContinue != p.before.hasContinue
                    || !f.dialogue.equals(p.before.dialogue);
            case ATTACK_RAT:
                return f.count(RAT_TAIL) > p.before.count(RAT_TAIL)
                    || f.tailGround || (f.inCombat && !p.before.inCombat);
            case LOOT_TAIL: return f.count(RAT_TAIL) > p.before.count(RAT_TAIL);
            case ONION: return f.count(ONION) > p.before.count(ONION);
            case SHOP_OPEN: return f.shopOpen;
            case SHOP_CLOSE: return !f.shopOpen;
            case BUY_BEEF: return f.count(RAW_BEEF) > p.before.count(RAW_BEEF);
            case BUY_NEWT: return f.count(EYE_OF_NEWT) > p.before.count(EYE_OF_NEWT);
            case COOK_OPEN: return f.production || f.count(RAW_BEEF) < p.before.count(RAW_BEEF)
                    || f.count(COOKED_MEAT) > p.before.count(COOKED_MEAT)
                    || f.count(BURNT_MEAT) > p.before.count(BURNT_MEAT);
            case BURN_OPEN: return f.production
                    || f.count(BURNT_MEAT) > p.before.count(BURNT_MEAT);
            case COOK_BEEF: return f.count(RAW_BEEF) < p.before.count(RAW_BEEF)
                    || f.count(COOKED_MEAT) > p.before.count(COOKED_MEAT)
                    || f.count(BURNT_MEAT) > p.before.count(BURNT_MEAT);
            case BURN_MEAT: return f.count(BURNT_MEAT) > p.before.count(BURNT_MEAT);
            case CAULDRON: return f.finished() || f.varp != p.before.varp
                    || f.inDialogue != p.before.inDialogue
                    || f.hasContinue != p.before.hasContinue
                    || !f.dialogue.equals(p.before.dialogue);
            case EAT: return f.count(p.item) < p.before.count(p.item);
            default: return false;
        }
    }

    private void issue(String key, Proof proof, Frame f, long timeout, int item,
                       BooleanSupplier action) {
        boolean accepted = action.getAsBoolean();
        pending = new Pending(key, proof, f, timeout, item);
        log.info("[WitchsPotion] ACTION key={} accepted={} varp={} pos={} item={} health={}",
            key, accepted, f.varp, f.pos, item, f.health);
    }

    private boolean needsOpenShop(Frame f) {
        return f.varp == 1 && ((f.count(EYE_OF_NEWT) == 0 && distance(f.pos, BETTY) <= 10)
            || (f.count(BURNT_MEAT) == 0 && f.count(RAW_BEEF) == 0
                && f.count(COOKED_MEAT) == 0 && distance(f.pos, WYDIN) <= 10));
    }

    private void stageOne(Frame f) {
        if (ingredientHandInAt != 0) {
            if (System.currentTimeMillis() - ingredientHandInAt <= 25000) {
                stage = "VERIFY_INGREDIENT_HAND_IN"; return;
            }
            if (ingredientTalkCount >= 2 || f.count(RAT_TAIL) == 0
                || f.count(ONION) == 0 || f.count(BURNT_MEAT) == 0
                || f.count(EYE_OF_NEWT) == 0) {
                hold("Hetty conversation did not advance varp from 1; talks="
                    + ingredientTalkCount + ", items=" + f.items); return;
            }
            ingredientHandInAt = 0; // One bounded second conversation if nothing was consumed.
        }
        if (f.count(RAT_TAIL) == 0) { sourceTail(f); return; }
        if (f.count(ONION) == 0) { sourceOnion(f); return; }
        if (f.count(BURNT_MEAT) == 0 && f.count(RAW_BEEF) == 0
            && f.count(COOKED_MEAT) == 0) { buy(f, RAW_BEEF); return; }
        if (f.count(EYE_OF_NEWT) == 0) { buy(f, EYE_OF_NEWT); return; }
        if (f.count(BURNT_MEAT) == 0) { makeBurntMeat(f); return; }
        stage = "RETURN_INGREDIENTS";
        talkHetty(f, "ingredients");
    }

    private void sourceTail(Frame f) {
        stage = "SOURCE_RAT_TAIL";
        if (route(f, "RAT_HOUSE", RAT_HOUSE, 4)) return;
        if (f.tailGround) {
            if (f.freeSlots == 0) { hold("No inventory slot for rat tail"); return; }
            issue("loot:rat-tail", Proof.LOOT_TAIL, f, 9000, RAT_TAIL,
                () -> Rs2GroundItem.loot(RAT_TAIL, 15));
            return;
        }
        long now = System.currentTimeMillis();
        if (ratEngagedAt != 0 && now - ratEngagedAt < 15000) {
            stage = "WAIT_RAT_DROP"; return;
        }
        if (ratAttempts >= 3) { hold("Three rat attacks without a visible rat tail"); return; }
        Rs2NpcModel rat = npc(RAT_IDS, RAT_HOUSE, 12);
        if (rat == null) { missingScene("level-one rat 2855 near Hetty", 12000); return; }
        if (!rat.hasLineOfSight()) {
            if (route(f, "RAT_APPROACH", rat.getWorldLocation(), 1)) return;
            missingScene("rat line of sight", 12000); return;
        }
        ratAttempts++;
        ratEngagedAt = now;
        issue("attack:rat", Proof.ATTACK_RAT, f, 9000, RAT_TAIL,
            () -> rat.click("Attack"));
    }

    private void sourceOnion(Frame f) {
        stage = "SOURCE_ONION";
        if (route(f, "RIMMINGTON_ONIONS", ONION_FIELD, 6)) return;
        Rs2TileObjectModel onion = object(ONION_IDS, ONION_FIELD, 18);
        if (onion == null) { missingScene("Rimmington onion 3366/5538", 12000); return; }
        String action = objectAction(onion, "Pick", "Take");
        if (action == null) { missingScene("onion has no Pick/Take action", 12000); return; }
        issue("pick:onion", Proof.ONION, f, 9000, ONION, () -> onion.click(action));
    }

    private void buy(Frame f, int item) {
        boolean beef = item == RAW_BEEF;
        WorldPoint point = beef ? WYDIN : BETTY;
        String name = beef ? "WYDIN_BEEF" : "BETTY_NEWT";
        stage = "BUY_" + name;
        int neededCoins = beef ? 1 : 3;
        if (f.count(COINS) < neededCoins) {
            hold("Need " + neededCoins + " coins for " + name + "; carried=" + f.count(COINS));
            return;
        }
        if (f.freeSlots == 0) { hold("No inventory slot for " + name); return; }
        if (route(f, name, point, 4)) return;
        if (!f.shopOpen) {
            Rs2NpcModel trader = npc(beef ? WYDIN_IDS : BETTY_IDS, point, 12);
            if (trader == null) { missingScene(name + " trader", 12000); return; }
            if (!trader.hasLineOfSight()) {
                if (route(f, name + "_APPROACH", trader.getWorldLocation(), 1)) return;
                missingScene(name + " line of sight", 12000); return;
            }
            issue("shop:open-" + name, Proof.SHOP_OPEN, f, 8000, 0,
                () -> trader.click("Trade"));
            return;
        }
        if (Rs2Shop.shopItems == null) { missingScene(name + " shop inventory", 8000); return; }
        if (!shopHasStock(item)) {
            if (shopStockSince == 0) shopStockSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - shopStockSince > 120000)
                hold(name + " remained out of stock for two minutes");
            else stage = "WAIT_" + name + "_STOCK";
            return;
        }
        shopStockSince = 0;
        issue("shop:buy-" + name, beef ? Proof.BUY_BEEF : Proof.BUY_NEWT,
            f, 7000, item, () -> Rs2Shop.buyItem(item, "1"));
    }

    private static boolean shopHasStock(int id) {
        List<Rs2ItemModel> items = Rs2Shop.shopItems;
        if (items == null) return false;
        for (Rs2ItemModel item : items)
            if (item != null && item.getId() == id && item.getQuantity() > 0) return true;
        return false;
    }

    private void makeBurntMeat(Frame f) {
        stage = "MAKE_BURNT_MEAT";
        if (route(f, "RIMMINGTON_RANGE", RANGE_HOUSE, 5)) return;
        Rs2TileObjectModel range = object(new int[]{RANGE_ID}, RANGE_HOUSE, 8);
        if (range == null) { missingScene("Rimmington range 9682", 12000); return; }
        if (f.count(COOKED_MEAT) > 0) {
            if (!f.production) {
                issue("burn:open-cooked-meat", Proof.BURN_OPEN, f, 10000, COOKED_MEAT,
                    () -> Rs2Inventory.useItemOnObject(COOKED_MEAT, RANGE_ID));
                return;
            }
            issue("burn:make-one", Proof.BURN_MEAT, f, 12000, BURNT_MEAT,
                () -> Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>) () -> {
                    Widget product = findProductionItem(Microbot.getClient(),
                        BURNT_MEAT, COOKED_MEAT);
                    if (product == null || product.isHidden() || product.getBounds() == null)
                        return false;
                    return Rs2Widget.clickWidget(product);
                }));
            return;
        }
        if (f.count(RAW_BEEF) == 0) { hold("Raw beef vanished before cooking"); return; }
        if (!f.production) {
            issue("cook:open-beef", Proof.COOK_OPEN, f, 10000, RAW_BEEF,
                () -> Rs2Inventory.useItemOnObject(RAW_BEEF, RANGE_ID));
            return;
        }
        issue("cook:one-beef", Proof.COOK_BEEF, f, 12000, RAW_BEEF,
            () -> Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>) () -> {
                Widget product = findCookProduct(Microbot.getClient());
                if (product == null || product.isHidden() || product.getBounds() == null) return false;
                return Rs2Widget.clickWidget(product);
            }));
    }

    private static Widget findCookProduct(Client client) {
        if (client == null) return null;
        return findProductionItem(client, COOKED_MEAT, RAW_BEEF);
    }

    private static Widget findProductionItem(Client client, int... ids) {
        if (client == null) return null;
        Widget product = findProductionItem(client.getWidget(270, 14), 0, ids);
        return product != null ? product
            : findProductionItem(client.getWidget(300, 16), 0, ids);
    }

    private static Widget findProductionItem(Widget widget, int depth, int[] ids) {
        if (widget == null || widget.isHidden() || depth > 8) return null;
        for (int id : ids) if (widget.getItemId() == id) return widget;
        Widget[] children = widget.getChildren();
        if (children != null) for (Widget child : children) {
            Widget found = findProductionItem(child, depth + 1, ids);
            if (found != null) return found;
        }
        Widget[] dynamic = widget.getDynamicChildren();
        if (dynamic != null) for (Widget child : dynamic) {
            Widget found = findProductionItem(child, depth + 1, ids);
            if (found != null) return found;
        }
        Widget[] staticChildren = widget.getStaticChildren();
        if (staticChildren != null) for (Widget child : staticChildren) {
            Widget found = findProductionItem(child, depth + 1, ids);
            if (found != null) return found;
        }
        return null;
    }

    private void talkHetty(Frame f, String suffix) {
        if (route(f, "HETTY_" + suffix.toUpperCase(), HETTY, 4)) return;
        Rs2NpcModel hetty = npc(HETTY_IDS, HETTY, 8);
        if (hetty == null) { missingScene("Hetty 4619", 12000); return; }
        if (!hetty.hasLineOfSight()) {
            // A radius-one NPC route can stop outside the wall while Hetty is
            // one tile away inside.  Reach this walkable interior tile exactly.
            if (route(f, "HETTY_INTERIOR_ACCESS", HETTY_INTERIOR, 0)) return;
            missingScene("Hetty line of sight from interior access", 12000); return;
        }
        issue("talk:hetty-" + suffix, Proof.TALK, f, 9000, 0,
            () -> hetty.click("Talk-to"));
    }

    private void drinkCauldron(Frame f) {
        stage = "DRINK_CAULDRON";
        if (route(f, "HETTY_CAULDRON", HETTY, 4)) return;
        Rs2TileObjectModel cauldron = object(new int[]{CAULDRON_ID}, HETTY, 5);
        if (cauldron == null) { missingScene("Hetty cauldron 2024", 12000); return; }
        issue("drink:cauldron", Proof.CAULDRON, f, 12000, 0,
            () -> cauldron.click("Drink-from"));
    }

    /** A visible dialogue owns the tick. Only known quest choices are selected. */
    private boolean dialogue(Frame f) {
        if (!f.inDialogue && f.options.isEmpty() && !f.hasContinue) {
            unknownDialogueSince = 0; return false;
        }
        if (!f.options.isEmpty()) {
            String[] allowed = f.varp == 0
                ? new String[]{"i am in search of a quest", "yes"} : new String[0];
            for (String fragment : allowed) for (String option : f.options)
                if (knownOption(option, fragment)) {
                    unknownDialogueSince = 0;
                    issue("dialogue:" + fragment, Proof.DIALOGUE, f, 7500, 0,
                        () -> Rs2Dialogue.clickOption(option));
                    return true;
                }
            if (unknownDialogueSince == 0) unknownDialogueSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - unknownDialogueSince > 12000)
                hold("Unexpected Witch's Potion choice at varp " + f.varp + ": " + f.options);
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

    /** The installed walker owns obstacle handling; this code owns route and time bounds. */
    private boolean route(Frame f, String key, WorldPoint target, int radius) {
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
        if (recoveryTarget != null) {
            if (!recoveryKey.equals(key)) { recoveryTarget = recoveryBefore = null; }
            else {
                if (!f.pos.equals(recoveryBefore)) {
                    failures.remove("walk:" + key);
                    recoveryTarget = recoveryBefore = null; recoveryKey = "";
                    return true;
                }
                if (System.currentTimeMillis() - recoveryAt > 8000)
                    hold("Route recovery " + key + " did not move from " + f.pos);
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
            route = r; startRouteSegment(r, distance);
            log.info("[WitchsPotion] ROUTE start={} from={} target={} radius={}",
                key, f.pos, target, radius);
            return true;
        }
        Route r = route;
        long now = System.currentTimeMillis();
        if (!f.pos.equals(r.lastPosition)) {
            r.lastPosition = f.pos; r.lastProgress = now;
        }
        if (distance < r.bestDistance) r.bestDistance = distance;
        if (now - r.started > 240000 || now - r.lastProgress > 20000) {
            routeFailure(f, "route timed out/no progress", r); return true;
        }
        if (r.completed && (r.worker == null || !r.worker.isAlive())
            && now - r.completedAt >= 1500) {
            // A valid detour can move away from the goal before returning.
            if (!f.pos.equals(r.segmentStart)) startRouteSegment(r, distance);
            else routeFailure(f, "walker segment ended without position progress", r);
        }
        return true;
    }

    private void startRouteSegment(Route r, int distance) {
        r.segmentStarted = System.currentTimeMillis();
        r.segmentStart = r.lastPosition;
        r.segmentDistance = distance; r.completed = false; r.completedAt = 0;
        Thread t = new Thread(() -> {
            try {
                Rs2Walker.walkWithStateUntil(r.target, r.radius,
                    () -> stopped || Thread.currentThread().isInterrupted()
                        || System.currentTimeMillis() - r.segmentStarted >= 15000);
            } finally {
                r.completedAt = System.currentTimeMillis(); r.completed = true;
            }
        }, "WitchsPotion-route");
        t.setDaemon(true); r.worker = t; t.start();
    }

    private void routeFailure(Frame f, String reason, Route r) {
        r.failureReason = reason; r.cancelReason = "failure " + reason;
        if (!cancelRoute()) return;
        finishRouteFailure(f, r);
    }

    private void finishRouteFailure(Frame f, Route r) {
        int count = failures.merge("walk:" + r.key, 1, Integer::sum);
        log.warn("[WitchsPotion] ROUTE failure={} attempt={}/3 at={} target={}",
            r.failureReason, count, f.pos, r.target);
        if (count >= 3) hold("Route " + r.key + " failed after " + count
            + " attempts: " + r.failureReason + "; at=" + f.pos + ", target=" + r.target);
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
        if (best == null) return false;
        boolean clicked = Rs2Walker.walkFastCanvas(best);
        if (!clicked) return false;
        recoveryTarget = best; recoveryBefore = f.pos; recoveryKey = r.key;
        recoveryAt = System.currentTimeMillis(); stage = "VERIFY_ROUTE_RECOVERY";
        return true;
    }

    private boolean cancelRoute() {
        Route r = route;
        if (r != null) {
            if (!r.cancelRequested) {
                r.cancelRequested = true; r.cancelAt = System.currentTimeMillis();
                if (r.cancelReason == null) r.cancelReason = "state gate";
                if (r.worker != null && r.worker.isAlive()) r.worker.interrupt();
                Thread clear = new Thread(() -> {
                    try { Rs2Walker.clearWalkingRoute("witchspotion:" + r.cancelReason); }
                    catch (Exception ex) {
                        r.clearError = ex.toString();
                        log.error("[WitchsPotion] route clear failed", ex);
                    }
                }, "WitchsPotion-route-clear");
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

    private void missingScene(String key, long timeout) {
        if (!key.equals(missingKey)) { missingKey = key; missingSince = System.currentTimeMillis(); }
        if (System.currentTimeMillis() - missingSince > timeout)
            hold("Target absent from loaded scene: " + key);
    }

    private void hold(String reason) {
        if (error.isEmpty()) log.error("[WitchsPotion] HOLD {}", reason);
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
        try (InputStream in = WitchsPotionScript.class.getResourceAsStream("WitchsPotionScript.class")) {
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
            log.warn("[WitchsPotion] class SHA-256 unavailable: {}", ex.toString());
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
            p.setProperty("currentWorld", f == null ? "0" : Integer.toString(f.world));
            p.setProperty("gameState", f == null ? "UNKNOWN" : f.gameState);
            p.setProperty("error", error);
            p.setProperty("position", f == null ? "UNKNOWN" : String.valueOf(f.pos));
            p.setProperty("healthPercent", f == null ? "-1" : Double.toString(f.health));
            p.setProperty("sha256", classSha256);
            p.setProperty("ratTail", f == null ? "0" : Integer.toString(f.count(RAT_TAIL)));
            p.setProperty("onion", f == null ? "0" : Integer.toString(f.count(ONION)));
            p.setProperty("burntMeat", f == null ? "0" : Integer.toString(f.count(BURNT_MEAT)));
            p.setProperty("eyeOfNewt", f == null ? "0" : Integer.toString(f.count(EYE_OF_NEWT)));
            p.setProperty("rawBeef", f == null ? "0" : Integer.toString(f.count(RAW_BEEF)));
            p.setProperty("cookedMeat", f == null ? "0" : Integer.toString(f.count(COOKED_MEAT)));
            p.setProperty("coins", f == null ? "0" : Integer.toString(f.count(COINS)));
            p.setProperty("freeSlots", f == null ? "0" : Integer.toString(f.freeSlots));
            p.setProperty("tailGround", f == null ? "false" : Boolean.toString(f.tailGround));
            p.setProperty("production", f == null ? "false" : Boolean.toString(f.production));
            p.setProperty("pending", pending == null ? "" : pending.key);
            p.setProperty("walkTarget", route == null ? "" : String.valueOf(route.target));
            p.setProperty("paceReason", paceReason);
            p.setProperty("paceRemainingMs", Long.toString(Math.max(0,
                nextActionAt - System.currentTimeMillis())));
            Path temp = STATUS_DIR.resolve("status.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) {
                p.store(out, "Witch's Potion live script");
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
        } catch (Exception ex) { log.warn("[WitchsPotion] status write: {}", ex.toString()); }
    }
}
