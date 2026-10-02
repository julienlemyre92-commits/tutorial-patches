package net.runelite.client.plugins.microbot.belowicemountain;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
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
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Build 1 is deliberately observation-only. It cannot dispatch game input. */
public class BelowIceMountainScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(BelowIceMountainScript.class);
    public static final int BUILD_NUMBER = 1;
    private static final Path STATUS = Paths.get(System.getProperty("user.home"),
        ".runelite", "belowicemountain", "status.properties");
    private static final int[] PICKAXES = {1265, 1267, 1269, 1273, 1271, 1275, 12297, 11920};
    private static final int[] FOOD = {315, 329, 333, 339, 361, 373, 379, 385, 391, 2142, 2309};

    private static final class Frame {
        String gameState = "NO_CLIENT", questState = "UNKNOWN";
        WorldPoint position;
        int loginIndex = -1, world = -1, varp = -1, questPoints = -1;
        int mining = -1, hp = -1, maxHp = -1, combatLevel = -1;
        int pickaxes = 0, food = 0, inventorySlots = -1;
        boolean inventoryLoaded, equipmentLoaded;
    }

    private BelowIceMountainConfig config;
    private BooleanSupplier ownsInput;
    private volatile boolean stopped;
    private String classHash = "UNKNOWN", stage = "START", error = "";

    public int runtimeBuild() { return BUILD_NUMBER; }
    public void restoreReloadState(Map<String,Object> ignored) { }
    public synchronized Map<String,Object> quiesceForReload() {
        shutdown();
        return Collections.emptyMap();
    }
    public boolean run(BelowIceMountainConfig config, BooleanSupplier ownsInput) {
        if (isRunning()) return true;
        this.config = config;
        this.ownsInput = ownsInput;
        this.stopped = false;
        this.stage = "START";
        this.error = "";
        this.classHash = classSha();
        log.info("[BelowIceMountain] RUNNING_BUILD={} classSha256={} observationOnly=true pid={}",
            BUILD_NUMBER, classHash, ProcessHandle.current().pid());
        int delay = Math.max(450, Math.min(2000, config.tickDelay()));
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::tick,
            0, delay, TimeUnit.MILLISECONDS);
        return true;
    }
    @Override public void shutdown() {
        stopped = true;
        if (mainScheduledFuture != null) mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow();
        super.shutdown();
        try { Files.deleteIfExists(STATUS); }
        catch (Exception ex) { log.warn("status cleanup: {}", ex.toString()); }
    }
    private void tick() {
        if (stopped || Thread.currentThread().isInterrupted()) return;
        Frame f = null;
        try {
            f = observe();
            if (!"LOGGED_IN".equals(f.gameState) || f.position == null) stage = "WAIT_LOGIN";
            else if (!f.inventoryLoaded) stage = "WAIT_INVENTORY";
            else if ("FINISHED".equals(f.questState)) stage = "QUEST_FINISHED";
            else if ("UNKNOWN".equals(f.questState) || f.varp < 0) stage = "HOLD_UNKNOWN_QUEST";
            else if (f.questPoints < 16 && "NOT_STARTED".equals(f.questState)) stage = "HOLD_QUEST_POINTS";
            else if (f.mining < 10) stage = "HOLD_MINING_LEVEL";
            else if (f.pickaxes == 0) stage = "HOLD_NO_PICKAXE";
            else if (f.hp <= 0 || f.maxHp <= 0) stage = "HOLD_HEALTH";
            else if (f.maxHp <= 11 && f.food < 10) stage = "HOLD_LOW_FOOD_FOR_GUARDIAN";
            else if (!armed()) stage = "PREFLIGHT_ACTIONS_DISABLED";
            else if (ownsInput != null && !ownsInput.getAsBoolean()) stage = "WAIT_EXCLUSIVE";
            else stage = "BUILD1_OBSERVATION_ONLY";
        } catch (Exception ex) {
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            stage = "HOLD_EXCEPTION";
            log.error("[BelowIceMountain] observation failed", ex);
        } finally {
            writeStatus(f);
        }
    }
    private Frame observe() {
        return Microbot.getClientThread().invoke(() -> {
            Frame f = new Frame();
            Client c = Microbot.getClient();
            if (c == null) return f;
            GameState state = c.getGameState();
            f.gameState = state == null ? "UNKNOWN" : state.name();
            f.loginIndex = c.getLoginIndex();
            f.world = c.getWorld();
            if (state != GameState.LOGGED_IN || c.getLocalPlayer() == null) return f;
            f.position = c.getLocalPlayer().getWorldLocation();
            f.varp = c.getVarpValue(VarPlayerID.BIM_MAIN);
            f.questPoints = c.getVarpValue(VarPlayerID.QP);
            f.mining = c.getRealSkillLevel(Skill.MINING);
            f.hp = c.getBoostedSkillLevel(Skill.HITPOINTS);
            f.maxHp = c.getRealSkillLevel(Skill.HITPOINTS);
            f.combatLevel = c.getLocalPlayer().getCombatLevel();
            QuestState quest = Quest.BELOW_ICE_MOUNTAIN.getState(c);
            f.questState = quest == null ? "UNKNOWN" : quest.name();
            ItemContainer inventory = c.getItemContainer(InventoryID.INVENTORY);
            ItemContainer equipment = c.getItemContainer(InventoryID.EQUIPMENT);
            f.inventoryLoaded = inventory != null;
            f.equipmentLoaded = equipment != null;
            if (inventory != null) {
                int used = 0;
                for (Item item : inventory.getItems()) {
                    if (item == null || item.getId() < 0) continue;
                    used++;
                    countItem(f, item);
                }
                f.inventorySlots = used;
            }
            if (equipment != null) for (Item item : equipment.getItems())
                if (item != null && item.getId() >= 0) countItem(f, item);
            return f;
        });
    }
    private static void countItem(Frame f, Item item) {
        for (int id : PICKAXES) if (item.getId() == id) { f.pickaxes += item.getQuantity(); break; }
        for (int id : FOOD) if (item.getId() == id) { f.food += item.getQuantity(); break; }
    }
    private boolean armed() {
        return config != null && config.allowActions()
            && config.approvedPid() == ProcessHandle.current().pid()
            && config.approvedBuild() == BUILD_NUMBER
            && classHash.matches("[a-f0-9]{64}")
            && classHash.equalsIgnoreCase(config.approvedSha256());
    }
    private static String classSha() {
        try (InputStream in = BelowIceMountainScript.class.getResourceAsStream("BelowIceMountainScript.class")) {
            if (in == null) return "UNKNOWN";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[8192]; int size;
            while ((size = in.read(bytes)) > 0) digest.update(bytes, 0, size);
            StringBuilder result = new StringBuilder();
            for (byte b : digest.digest()) result.append(String.format("%02x", b & 255));
            return result.toString();
        } catch (Exception ex) { return "UNKNOWN"; }
    }
    private void writeStatus(Frame f) {
        try {
            Files.createDirectories(STATUS.getParent());
            Properties p = new Properties();
            p.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
            p.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
            p.setProperty("build", Integer.toString(BUILD_NUMBER));
            p.setProperty("sha256", classHash);
            p.setProperty("observationOnly", "true");
            p.setProperty("actionsArmed", Boolean.toString(armed()));
            p.setProperty("stage", stage);
            p.setProperty("error", error);
            p.setProperty("gameState", f == null ? "UNKNOWN" : f.gameState);
            p.setProperty("loginIndex", f == null ? "-1" : Integer.toString(f.loginIndex));
            p.setProperty("world", f == null ? "-1" : Integer.toString(f.world));
            p.setProperty("position", f == null ? "UNKNOWN" : String.valueOf(f.position));
            p.setProperty("questState", f == null ? "UNKNOWN" : f.questState);
            p.setProperty("questVarp2951", f == null ? "-1" : Integer.toString(f.varp));
            p.setProperty("questPointsVarp101", f == null ? "-1" : Integer.toString(f.questPoints));
            p.setProperty("miningLevel", f == null ? "-1" : Integer.toString(f.mining));
            p.setProperty("hp", f == null ? "-1" : f.hp + "/" + f.maxHp);
            p.setProperty("combatLevel", f == null ? "-1" : Integer.toString(f.combatLevel));
            p.setProperty("pickaxeCount", f == null ? "0" : Integer.toString(f.pickaxes));
            p.setProperty("foodCount", f == null ? "0" : Integer.toString(f.food));
            p.setProperty("inventorySlots", f == null ? "-1" : Integer.toString(f.inventorySlots));
            Path temp = STATUS.resolveSibling("status.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "Below Ice Mountain observation-only Build 1"); }
            Files.move(temp, STATUS, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) { log.warn("[BelowIceMountain] status write: {}", ex.toString()); }
    }
}
