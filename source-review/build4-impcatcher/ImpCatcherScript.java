package net.runelite.client.plugins.microbot.impcatcher;

import java.io.OutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tick-observed Imp Catcher. Script alone is hot-swappable by the existing host. */
public class ImpCatcherScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(ImpCatcherScript.class);
    public static final int BUILD_NUMBER = 4;
    private static final int[] BEADS = {1470, 1472, 1474, 1476};
    private static final int MIZGOG = 7746, IMP = 5007, AMULET = 1478;
    private static final WorldPoint TOWER = new WorldPoint(3104, 3164, 0);
    private static final WorldPoint[] IMP_AREAS = {
        new WorldPoint(3008, 3309, 0), new WorldPoint(3247, 3228, 0),
        new WorldPoint(3077, 3248, 0)
    };
    private static final Path STATUS = Paths.get(System.getProperty("user.home"),
        ".runelite", "impcatcher", "status.properties");
    private volatile boolean stopped;
    private volatile String error = "";
    private BooleanSupplier ownsInput;
    private Pending pending;
    private int retries, areaIndex;
    private boolean sawAmulet;
    private QuestState highest = QuestState.NOT_STARTED;
    private long lastCompleteLog;

    public boolean run(ImpCatcherConfig config, BooleanSupplier owner) {
        if (isRunning()) return true;
        stopped = false;
        error = "";
        ownsInput = owner;
        log.info("[ImpCatcher] RUNNING_BUILD={} mode=OBSERVED_QUEST", BUILD_NUMBER);
        long delay = Math.max(500, Math.min(2000, config.tickDelay()));
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::tick, 0, delay, TimeUnit.MILLISECONDS);
        return true;
    }
    public int runtimeBuild() { return BUILD_NUMBER; }
    @Override public void shutdown() {
        stopped = true;
        if (mainScheduledFuture != null) mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow();
        super.shutdown();
    }
    public boolean awaitStopped() {
        try { return scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); return false; }
    }
    private void tick() {
        if (stopped || Thread.currentThread().isInterrupted()) return;
        Frame f = new Frame();
        try {
            f = Microbot.getClientThread().invoke((java.util.function.Supplier<Frame>) this::observe);
            if (f == null) return;
            if (f.quest == QuestState.IN_PROGRESS && highest == QuestState.NOT_STARTED)
                highest = QuestState.IN_PROGRESS;
            if (f.quest == QuestState.FINISHED) highest = QuestState.FINISHED;
            if (f.amulet > 0) sawAmulet = true;
            if (f.quest == QuestState.FINISHED) {
                pending = null;
                writeStatus(f, "COMPLETE_QUEST_STATE");
                if (System.currentTimeMillis() - lastCompleteLog > 10000) {
                    lastCompleteLog = System.currentTimeMillis();
                    log.info("[ImpCatcher] QUEST_FINISHED build={} varp160={} amuletObserved={}",
                        BUILD_NUMBER, f.varp, sawAmulet);
                }
                return;
            }
            if (f.gameState != GameState.LOGGED_IN || f.pos == null) {
                pending = null;
                writeStatus(f, "WAIT_LOGIN");
                return;
            }
            if (f.hp <= 0) { hold(f, "Death/zero HP: fresh scene and inventory review required"); return; }
            if (f.hp < 3) { hold(f, "Critical HP: supply food before fighting"); return; }
            if (pending != null) {
                if (proved(pending, f)) {
                    log.info("[ImpCatcher] PROVED {}", pending.label);
                    pending = null;
                    retries = 0;
                } else if (System.currentTimeMillis() - pending.at < pending.timeout) {
                    writeStatus(f, "WAIT_" + pending.label);
                    return;
                } else {
                    log.warn("[ImpCatcher] UNPROVED {} attempt={}", pending.label, retries + 1);
                    pending = null;
                    if (++retries >= 3) {
                        hold(f, "Unproved action after 3 attempts; scene/collision rescan needed");
                        return;
                    }
                }
            }
            if (ownsInput == null || !ownsInput.getAsBoolean()) {
                writeStatus(f, "WAIT_INPUT_OWNER");
                return;
            }
            if (f.slots >= 28 && !allBeads(f)) {
                hold(f, "Inventory full with missing beads; bank nonquest items");
                return;
            }
            if (f.dialogue) dialogue(f);
            else if (f.quest == QuestState.NOT_STARTED || allBeads(f)) reachMizgog(f);
            else collect(f);
            writeStatus(f, pending == null ? "OBSERVE" : pending.label);
        } catch (Throwable ex) {
            error = ex.toString();
            stopped = true; // Never send another input after an unclassified API error.
            log.error("[ImpCatcher] TICK_ERROR {}", error, ex);
            try {
                writeStatus(f, "ERROR");
            } catch (Throwable statusEx) {
                log.error("[ImpCatcher] ERROR_STATUS_WRITE_FAILED {}", statusEx.toString(), statusEx);
            }
        }
    }
    private void dialogue(Frame f) {
        if (f.continuePrompt) {
            Rs2Dialogue.clickContinue();
            pending = new Pending("CONTINUE", f, 9000, null);
        } else if (!f.options.isEmpty()) {
            String chosen = null;
            for (String option : f.options) {
                String s = option.toLowerCase(Locale.ROOT);
                if (s.contains("yes") || s.contains("help") || s.contains("bead") || s.contains("quest")) {
                    chosen = option;
                    break;
                }
            }
            if (chosen == null) { hold(f, "Unrecognized dialogue options: " + f.options); return; }
            if (Rs2Dialogue.clickOption(chosen))
                pending = new Pending("OPTION", f, 9000, null);
            else hold(f, "Dialogue option click rejected: " + chosen);
        }
    }
    private void reachMizgog(Frame f) {
        if (f.pos.getPlane() < 2) {
            int xyDistance = Math.max(Math.abs(f.pos.getX() - TOWER.getX()),
                Math.abs(f.pos.getY() - TOWER.getY()));
            if (xyDistance > 12) {
                walk(f, new WorldPoint(TOWER.getX(), TOWER.getY(), f.pos.getPlane()), "WALK_TOWER");
                return;
            }
            TileObject stair = Rs2GameObject.getTileObject(12536);
            if (stair == null) stair = Rs2GameObject.getTileObject(12537);
            if (stair == null) stair = Rs2GameObject.getTileObject("Staircase");
            if (stair == null) { hold(f, "No staircase visible in Wizards Tower scene"); return; }
            if (Rs2GameObject.interact(stair, "Climb-up"))
                pending = new Pending("CLIMB", f, 9000, null);
            else hold(f, "Stair interaction rejected");
            return;
        }
        if (f.mizgog == null) { hold(f, "Mizgog absent on upper floor; check NPC id"); return; }
        if (Rs2Npc.interact(f.mizgog, "Talk-to"))
            pending = new Pending("TALK_MIZGOG", f, 9000, null);
        else walk(f, f.mizgog.getWorldLocation(), "APPROACH_MIZGOG");
    }
    private void collect(Frame f) {
        for (int id : BEADS) if (f.count(id) == 0 && Rs2GroundItem.exists(id, 12)) {
            if (Rs2GroundItem.pickup(id)) pending = new Pending("PICKUP_" + id, f, 9000, null);
            else hold(f, "Visible bead pickup rejected: " + id);
            return;
        }
        if (f.hp < Math.max(3, f.maxHp / 3)) {
            hold(f, "Low HP before imp combat; supply food");
            return;
        }
        if (f.inCombat) return;
        if (f.imp != null) {
            if (Rs2Npc.attack(f.imp)) pending = new Pending("ATTACK_IMP", f, 9000, null);
            else walk(f, f.imp.getWorldLocation(), "APPROACH_IMP");
            return;
        }
        WorldPoint area = IMP_AREAS[areaIndex];
        if (f.pos.distanceTo(area) <= 8) {
            areaIndex = (areaIndex + 1) % IMP_AREAS.length;
            area = IMP_AREAS[areaIndex];
        }
        walk(f, area, "FIND_IMPS");
    }
    private void walk(Frame f, WorldPoint target, String label) {
        WalkerState state = Rs2Walker.walkStep(target, 1);
        if (state == WalkerState.MOVING) {
            pending = new Pending(label, f, 18000, target);
            log.info("[ImpCatcher] WALK_STEP label={} state={} from={} target={}",
                label, state, f.pos, target);
        } else if (state == WalkerState.ARRIVED) {
            hold(f, "Walker already at target but action is still unproved: " + label + " " + target);
        } else {
            hold(f, "Walker " + state + " for " + label + " to " + target);
        }
    }
    private boolean proved(Pending p, Frame f) {
        switch (p.label) {
            case "CONTINUE": case "OPTION":
                return f.quest != p.before.quest || !f.dialogue
                    || !f.text.equals(p.before.text) || !f.options.equals(p.before.options);
            case "CLIMB": return f.pos != null && f.pos.getPlane() > p.before.pos.getPlane();
            case "TALK_MIZGOG": return f.dialogue || f.quest != p.before.quest;
            case "ATTACK_IMP": return f.inCombat || f.imp == null || f.impHealth < p.before.impHealth;
            default:
                if (p.label.startsWith("PICKUP_")) {
                    int item = Integer.parseInt(p.label.substring(7));
                    return f.count(item) > p.before.count(item);
                }
                if (p.target != null && f.pos != null)
                    return f.pos.distanceTo(p.target) <= 4 || f.pos.distanceTo(p.before.pos) >= 2;
                return false;
        }
    }
    private Frame observe() {
        Frame f = new Frame();
        Client c = Microbot.getClient();
        if (c == null) return f;
        f.gameState = c.getGameState();
        if (f.gameState != GameState.LOGGED_IN || c.getLocalPlayer() == null) return f;
        f.pos = c.getLocalPlayer().getWorldLocation();
        f.hp = c.getBoostedSkillLevel(Skill.HITPOINTS);
        f.maxHp = c.getRealSkillLevel(Skill.HITPOINTS);
        f.inCombat = c.getLocalPlayer().getInteracting() != null;
        f.varp = c.getVarpValue(160);
        f.quest = Quest.IMP_CATCHER.getState(c);
        ItemContainer inventory = c.getItemContainer(InventoryID.INVENTORY);
        if (inventory != null) for (Item item : inventory.getItems()) {
            if (item == null || item.getId() <= 0) continue;
            f.slots++;
            for (int i = 0; i < BEADS.length; i++)
                if (item.getId() == BEADS[i]) f.beads[i] += item.getQuantity();
            if (item.getId() == AMULET) f.amulet += item.getQuantity();
        }
        List<NPC> imps = new ArrayList<>();
        for (NPC npc : c.getNpcs()) {
            if (npc == null || npc.getWorldLocation() == null) continue;
            if (npc.getId() == MIZGOG || "Wizard Mizgog".equalsIgnoreCase(npc.getName())) f.mizgog = npc;
            if (npc.getId() == IMP || "Imp".equalsIgnoreCase(npc.getName())) imps.add(npc);
        }
        imps.sort(Comparator.comparingInt(n -> n.getWorldLocation().distanceTo(f.pos)));
        if (!imps.isEmpty()) {
            f.imp = imps.get(0);
            f.impHealth = f.imp.getHealthRatio();
        }
        f.dialogue = Rs2Dialogue.isInDialogue();
        if (f.dialogue) {
            f.continuePrompt = Rs2Dialogue.hasContinue();
            String text = Rs2Dialogue.getDialogueText();
            f.text = text == null ? "" : text;
            for (Widget option : Rs2Dialogue.getDialogueOptions())
                if (option != null && option.getText() != null) f.options.add(option.getText());
        }
        return f;
    }
    private boolean allBeads(Frame f) {
        for (int n : f.beads) if (n < 1) return false;
        return true;
    }
    private void hold(Frame f, String why) {
        error = why;
        stopped = true;
        log.warn("[ImpCatcher] HOLD build={} {} pos={} quest={} varp={} beads={}",
            BUILD_NUMBER, why, f.pos, f.quest, f.varp, Arrays.toString(f.beads));
        try { writeStatus(f, "HOLD"); }
        catch (Exception ex) { log.warn("[ImpCatcher] status write {}", ex.toString()); }
    }
    private void writeStatus(Frame f, String state) throws Exception {
        Files.createDirectories(STATUS.getParent());
        Properties p = new Properties();
        p.setProperty("build", Integer.toString(BUILD_NUMBER));
        p.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
        p.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        p.setProperty("state", state);
        p.setProperty("gameState", f.gameState.name());
        p.setProperty("position", String.valueOf(f.pos));
        p.setProperty("questState", f.quest.name());
        p.setProperty("highestQuestState", highest.name());
        p.setProperty("questVarp160", Integer.toString(f.varp));
        for (int i = 0; i < BEADS.length; i++)
            p.setProperty("bead" + BEADS[i], Integer.toString(f.beads[i]));
        p.setProperty("amulet", Integer.toString(f.amulet));
        p.setProperty("amuletEverObserved", Boolean.toString(sawAmulet));
        p.setProperty("hp", f.hp + "/" + f.maxHp);
        p.setProperty("pending", pending == null ? "" : pending.label);
        p.setProperty("retries", Integer.toString(retries));
        p.setProperty("error", error);
        Path tmp = STATUS.resolveSibling("status.tmp");
        try (OutputStream out = Files.newOutputStream(tmp)) { p.store(out, "Imp Catcher runtime"); }
        try { Files.move(tmp, STATUS, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, STATUS, StandardCopyOption.REPLACE_EXISTING); }
    }
    private static final class Pending {
        final String label;
        final Frame before;
        final WorldPoint target;
        final long at = System.currentTimeMillis(), timeout;
        Pending(String label, Frame before, long timeout, WorldPoint target) {
            this.label = label; this.before = before; this.timeout = timeout; this.target = target;
        }
    }
    private static final class Frame {
        GameState gameState = GameState.UNKNOWN;
        QuestState quest = QuestState.NOT_STARTED;
        WorldPoint pos;
        NPC imp, mizgog;
        int varp = -1, hp, maxHp, slots, amulet, impHealth = -1;
        final int[] beads = new int[4];
        boolean inCombat, dialogue, continuePrompt;
        String text = "";
        final List<String> options = new ArrayList<>();
        int count(int id) {
            for (int i = 0; i < BEADS.length; i++) if (BEADS[i] == id) return beads[i];
            return 0;
        }
    }
}
