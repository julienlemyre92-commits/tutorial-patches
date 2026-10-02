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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.events.WelcomeScreenEvent;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.input.InputArbiter;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.security.LoginManager;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bounded early route candidate; stops before the unverified guardian. */
public class BelowIceMountainScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(BelowIceMountainScript.class);
    public static final int BUILD_NUMBER = 4;
    private static final WorldPoint ISLAND_BOAT = new WorldPoint(1619,4816,0);
    private static final WorldPoint FALADOR_BANK = new WorldPoint(3012,3356,0);
    private static final WorldPoint WILLOW = new WorldPoint(3003,3435,0);
    private static final WorldPoint CHECKAL = new WorldPoint(3087,3415,0);
    private static final WorldPoint ATLAS = new WorldPoint(3076,3440,0);
    private static final WorldPoint PICKAXE_SPAWN = new WorldPoint(3083,3419,0);
    private static final Path STATUS = Paths.get(System.getProperty("user.home"),
        ".runelite", "belowicemountain", "status.properties");
    private static final Path CONTROL = STATUS.resolveSibling("control.properties");
    private static final int[] PICKAXES = {1265, 1267, 1269, 1273, 1271, 1275, 12297, 11920};
    private static final int[] FOOD = {315, 329, 333, 339, 361, 373, 379, 385, 391};

    private static final class Frame {
        String gameState = "NO_CLIENT", questState = "UNKNOWN";
        WorldPoint position;
        int loginIndex = -1, world = -1, varp = -1, questPoints = -1;
        int mining = -1, hp = -1, maxHp = -1, combatLevel = -1;
        int pickaxes = 0, food = 0, inventorySlots = -1;
        int checkal = -1, marley = -1, burntof = -1, canvasWidth = -1;
        boolean bankOpen, inDialogue, hasContinue;
        String dialogue = "";
        final List<String> options = new ArrayList<>();
        final Map<Integer,Integer> items = new HashMap<>();
        boolean inventoryLoaded, equipmentLoaded;
        int count(int id) { return items.getOrDefault(id,0); }
    }

    private enum Proof { QUEST_ADVANCED, VARBIT_CHANGED, DIALOGUE_CHANGED, ITEM_GAINED, INVENTORY_SHED, BANK_TOGGLED, ISLAND_EXIT, FLEX_OPEN }
    private static final class Pending {
        final String key; final Proof proof; final Frame before; final int item;
        final long at = System.currentTimeMillis(), timeout;
        Pending(String key, Proof proof, Frame before, int item, long timeout) {
            this.key=key; this.proof=proof; this.before=before; this.item=item; this.timeout=timeout;
        }
    }
    private static final class Route {
        final String key; final WorldPoint target; final int radius;
        final long at=System.currentTimeMillis();
        volatile boolean done;
        Thread worker;
        Route(String key, WorldPoint target, int radius) {
            this.key=key; this.target=target; this.radius=radius;
        }
    }

    private BelowIceMountainConfig config;
    private BooleanSupplier ownsInput;
    private volatile boolean stopped;
    private String classHash = "UNKNOWN", stage = "START", error = "";
    private Pending pending;
    private Route route, cancellingRoute;
    private Thread routeClear;
    private long routeClearAt, nextAt, disconnectAt, loginAt, welcomeAt;
    private int disconnectAttempts, loginAttempts, welcomeAttempts, selectedWorld;
    private final Map<String,Integer> failures = new HashMap<>();
    private boolean bankChecked;

    public int runtimeBuild() { return BUILD_NUMBER; }
    public void restoreReloadState(Map<String,Object> state) {
        bankChecked = state != null && Boolean.TRUE.equals(state.get("bankChecked"));
    }
    public synchronized Map<String,Object> quiesceForReload() {
        if (pending != null || route != null || cancellingRoute != null)
            throw new IllegalStateException("action or route in progress");
        Map<String,Object> state = new HashMap<>();
        state.put("bankChecked",bankChecked);
        shutdown();
        return state;
    }
    public boolean run(BelowIceMountainConfig config, BooleanSupplier ownsInput) {
        if (isRunning()) return true;
        this.config = config;
        this.ownsInput = ownsInput;
        this.stopped = false;
        this.stage = "START";
        this.error = "";
        this.classHash = classSha();
        log.info("[BelowIceMountain] RUNNING_BUILD={} classSha256={} guardianActions=false pid={}",
            BUILD_NUMBER, classHash, ProcessHandle.current().pid());
        int delay = Math.max(450, Math.min(2000, config.tickDelay()));
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
        try { Files.deleteIfExists(STATUS); }
        catch (Exception ex) { log.warn("status cleanup: {}", ex.toString()); }
    }
    private void tick() {
        if (stopped || Thread.currentThread().isInterrupted()) return;
        Frame f = null;
        try {
            f = observe();
            if (!error.isEmpty()) { stage="HOLD"; cancelRoute(); return; }
            if (cancellingRoute != null) {
                if ((cancellingRoute.worker != null && cancellingRoute.worker.isAlive())
                    || (routeClear != null && routeClear.isAlive())) {
                    stage="WAIT_ROUTE_STOP";
                    if (System.currentTimeMillis()-routeClearAt>60000) hold("Route did not stop");
                    return;
                }
                cancellingRoute=null; routeClear=null;
            }
            if ("FINISHED".equals(f.questState)) { cancelRoute(); pending=null; stage="QUEST_FINISHED"; return; }
            if ("LOGGED_IN".equals(f.gameState)) {
                if (!f.inventoryLoaded) { stage="WAIT_INVENTORY"; return; }
                if ("UNKNOWN".equals(f.questState) || f.varp<0) { hold("Unknown quest state/varp"); return; }
                if (f.questPoints<16 && "NOT_STARTED".equals(f.questState)) {
                    hold("Below Ice Mountain requires 16 quest points; observed "+f.questPoints); return;
                }
                if (f.mining<10) { hold("Mining 10 required for safe pillar strategy; observed "+f.mining); return; }
                if (f.hp<=0 || f.maxHp<=0) { hold("Death or invalid HP "+f.hp+"/"+f.maxHp); return; }
                if (LoginManager.isMemberWorld(f.world)) { hold("F2P quest on members world "+f.world); return; }
            }
            if (!armed()) { cancelRoute(); pending=null; stage="PREFLIGHT_ACTIONS_DISABLED"; return; }
            if (ownsInput!=null && !ownsInput.getAsBoolean()) { cancelRoute(); stage="WAIT_EXCLUSIVE"; return; }
            if (!"LOGGED_IN".equals(f.gameState)) { loginTick(f); return; }
            WelcomeScreenEvent welcome = new WelcomeScreenEvent();
            if (welcome.validate()) {
                cancelRoute(); stage="VERIFY_WELCOME_DISMISS";
                if (welcomeAttempts++ == 0) {
                    welcomeAt=System.currentTimeMillis();
                    welcome.execute();
                    log.info("[BelowIceMountain] WELCOME_DISMISS_DISPATCH");
                } else if (System.currentTimeMillis()-welcomeAt>12000) {
                    hold("Welcome screen persisted after native dismiss");
                }
                return;
            }
            welcomeAttempts=0;
            if (Microbot.pauseAllScripts.get() || InputArbiter.isHuman()) {
                cancelRoute(); stage="WAIT_INPUT"; return;
            }
            if (pending!=null) { verify(f); return; }
            if (f.inDialogue || f.hasContinue || !f.options.isEmpty()) { dialogue(f); return; }
            if (route!=null) { walk(f,route.key,route.target,route.radius); return; }
            if (System.currentTimeMillis()<nextAt) { stage="WAIT_PACE"; return; }
            if (f.position.getX()<1700 && f.position.getY()>4700 && f.position.getY()<4900) {
                leaveMisthalinIsland(f); return;
            }
            if (f.position.getX()<2500 || f.position.getX()>3500 || f.position.getY()<3000
                || f.position.getY()>3800 || f.position.getPlane()!=0) {
                hold("Unmapped location for F2P route: "+f.position); return;
            }
            if (!bankChecked) { prepare(f); return; }
            if (f.bankOpen) {
                issue("bank:close",Proof.BANK_TOGGLED,f,0,7000,Rs2Bank::closeBank); return;
            }
            if (f.varp==0 || f.varp==5 || f.varp==7) { startWillow(f); return; }
            if (f.varp==10) { checkal(f); return; }
            if (f.varp>=15) { stage="BUILD2_STAGE_LIMIT_"+f.varp; return; }
            hold("Unmapped BIM_MAIN stage "+f.varp);
        } catch (Exception ex) {
            hold("Tick exception: "+ex.getClass().getSimpleName()+": "+ex.getMessage());
            log.error("[BelowIceMountain] observation failed", ex);
        } finally {
            writeStatus(f);
        }
    }
    private Frame observe() {
        Frame result = Microbot.getClientThread().invoke(() -> {
            Frame f = new Frame();
            Client c = Microbot.getClient();
            if (c == null) return f;
            GameState state = c.getGameState();
            f.gameState = state == null ? "UNKNOWN" : state.name();
            f.loginIndex = c.getLoginIndex();
            f.canvasWidth = c.getCanvasWidth();
            f.world = c.getWorld();
            if (state != GameState.LOGGED_IN || c.getLocalPlayer() == null) return f;
            f.position = c.getLocalPlayer().getWorldLocation();
            f.varp = c.getVarpValue(VarPlayerID.BIM_MAIN);
            f.questPoints = c.getVarpValue(VarPlayerID.QP);
            f.checkal = c.getVarbitValue(VarbitID.BIM_CHECKAL);
            f.marley = c.getVarbitValue(VarbitID.BIM_MARLEY);
            f.burntof = c.getVarbitValue(VarbitID.BIM_BURNTOF);
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
                    f.items.merge(item.getId(),item.getQuantity(),Integer::sum);
                    countItem(f, item);
                }
                f.inventorySlots = used;
            }
            if (equipment != null) for (Item item : equipment.getItems())
                if (item != null && item.getId() >= 0) countItem(f, item);
            return f;
        });
        if ("LOGGED_IN".equals(result.gameState)) {
            result.bankOpen=Rs2Bank.isOpen();
            result.inDialogue=Rs2Dialogue.isInDialogue();
            result.hasContinue=Rs2Dialogue.hasContinue();
            result.dialogue=Rs2Dialogue.getDialogueText();
            for (Widget option:Rs2Dialogue.getDialogueOptions())
                if (option!=null && option.getText()!=null && !option.getText().trim().isEmpty())
                    result.options.add(option.getText());
        }
        return result;
    }
    private static void countItem(Frame f, Item item) {
        for (int id : PICKAXES) if (item.getId() == id) { f.pickaxes += item.getQuantity(); break; }
        for (int id : FOOD) if (item.getId() == id) { f.food += item.getQuantity(); break; }
    }
    private void loginTick(Frame f) {
        long now=System.currentTimeMillis();
        if (!"LOGIN_SCREEN".equals(f.gameState)) { stage="WAIT_LOGIN_SCREEN"; return; }
        if (f.loginIndex==24) {
            if (disconnectAttempts++==0 && f.canvasWidth>0) {
                Microbot.getClientThread().invoke(() -> {
                    Microbot.getMouse().click(365+(f.canvasWidth-804)/2,308); return true;
                });
                disconnectAt=now; stage="VERIFY_DISCONNECT_DISMISS";
            } else if (now-disconnectAt>8000) hold("Disconnected modal persisted");
            return;
        }
        if (disconnectAttempts>0) {
            disconnectAttempts=0; loginAttempts=0; stage="DISCONNECT_DISMISSED"; return;
        }
        if (f.loginIndex!=10 && f.loginIndex!=34) { stage="WAIT_LOGIN_INDEX_"+f.loginIndex; return; }
        if (selectedWorld==0) {
            selectedWorld=LoginManager.getRandomWorld(false);
            if (selectedWorld<=0 || LoginManager.isMemberWorld(selectedWorld)) {
                hold("No verified F2P world for login"); return;
            }
        }
        if (loginAttempts++==0) {
            loginAt=now; stage="VERIFY_NATIVE_LOGIN";
            log.info("[BelowIceMountain] NATIVE_LOGIN_DISPATCH world={} index={}",selectedWorld,f.loginIndex);
            if (!LoginManager.login(selectedWorld)) hold("Native LoginManager.login rejected");
        } else if (now-loginAt>20000) hold("Native login did not reach game");
    }
    private void leaveMisthalinIsland(Frame f) {
        stage="LEAVE_PREVIOUS_QUEST_ISLAND";
        if (walk(f,"ISLAND_BOAT",ISLAND_BOAT,6)) return;
        Rs2TileObjectModel boat=Microbot.getRs2TileObjectCache().query()
            .withId(ObjectID.MISTMYST_BOAT_ISLAND).within(ISLAND_BOAT,12).nearestOnClientThread();
        if (boat==null) { hold("Misthalin island boat not visible near "+ISLAND_BOAT); return; }
        issue("board:island-boat",Proof.ISLAND_EXIT,f,0,18000,()->boat.click("Board"));
    }
    private void prepare(Frame f) {
        stage="PREPARE_BANK";
        if (walk(f,"FALADOR_BANK",FALADOR_BANK,5)) return;
        if (!f.bankOpen) {
            issue("bank:open",Proof.BANK_TOGGLED,f,0,10000,Rs2Bank::openBank); return;
        }
        int[] keep={1265,1267,1269,1273,1271,1275,12297,11920,
            ItemID.COOKED_MEAT,ItemID.BREAD,ItemID.KNIFE,ItemID.COINS,
            ItemID.BIM_STEAK_SANDWICH,ItemID.ASGARNIAN_ALE,
            315,329,333,339,361,373,379,385,391};
        int excess=0;
        for (int id:f.items.keySet()) if (!contains(keep,id)) excess++;
        if (excess>0) {
            Integer[] keepBoxed=java.util.Arrays.stream(keep).boxed().toArray(Integer[]::new);
            issue("bank:deposit-excess",Proof.INVENTORY_SHED,f,0,10000,
                ()->Rs2Bank.depositAllExcept(keepBoxed)); return;
        }
        if (f.pickaxes==0) for (int id:new int[]{1267,1265}) if (Rs2Bank.count(id)>0) {
            issue("bank:pickaxe:"+id,Proof.ITEM_GAINED,f,id,10000,
                ()->Rs2Bank.withdrawOne(id)); return;
        }
        for (int id:new int[]{ItemID.KNIFE,ItemID.BREAD,ItemID.COOKED_MEAT})
            if (f.count(id)==0 && Rs2Bank.count(id)>0) {
                issue("bank:item:"+id,Proof.ITEM_GAINED,f,id,10000,
                    ()->Rs2Bank.withdrawOne(id)); return;
            }
        if (f.count(ItemID.COINS)<3 && Rs2Bank.count(ItemID.COINS)>=3) {
            issue("bank:coins",Proof.ITEM_GAINED,f,ItemID.COINS,10000,
                ()->Rs2Bank.withdrawX(ItemID.COINS,Math.min(100,Rs2Bank.count(ItemID.COINS)))); return;
        }
        if (f.food<10) for (int id:new int[]{385,379,373,361,329,333})
            if (Rs2Bank.count(id)>0) {
                issue("bank:food:"+id,Proof.ITEM_GAINED,f,id,10000,
                    ()->Rs2Bank.withdrawX(id,Math.min(10-f.food,Rs2Bank.count(id)))); return;
            }
        bankChecked=true;
        issue("bank:close",Proof.BANK_TOGGLED,f,0,7000,Rs2Bank::closeBank);
    }
    private void startWillow(Frame f) {
        stage="START_WILLOW";
        if (walk(f,"WILLOW_START",WILLOW,4)) return;
        Rs2NpcModel willow=Microbot.getRs2NpcCache().query()
            .withId(NpcID.BIM_WILLOW).within(WILLOW,9).nearestOnClientThread();
        if (willow==null) { hold("Willow not visible at "+WILLOW); return; }
        issue("talk:willow-start",Proof.DIALOGUE_CHANGED,f,0,12000,()->willow.click("Talk-to"));
    }
    private void checkal(Frame f) {
        stage="RECRUIT_CHECKAL_"+f.checkal;
        if (f.pickaxes==0) {
            if (walk(f,"VILLAGE_PICKAXE",PICKAXE_SPAWN,10)) return;
            if (!Rs2GroundItem.exists(1265,15)) { hold("Bronze pickaxe spawn not visible in Barbarian Village spinning hut; bank empty"); return; }
            issue("loot:village-pickaxe",Proof.ITEM_GAINED,f,1265,10000,
                ()->Rs2GroundItem.loot(1265,15)); return;
        }
        if (f.checkal==0) {
            talkCheckal(f,"talk:checkal-first"); return;
        }
        if (f.checkal==5 || f.checkal==10) {
            if (walk(f,"ATLAS",ATLAS,4)) return;
            Rs2NpcModel atlas=Microbot.getRs2NpcCache().query()
                .withId(NpcID.BIM_ATLAS).within(ATLAS,9).nearestOnClientThread();
            if (atlas==null) { hold("Atlas not visible at "+ATLAS); return; }
            issue("talk:atlas",Proof.DIALOGUE_CHANGED,f,0,15000,()->atlas.click("Talk-to"));
            return;
        }
        if (f.checkal==15 || f.checkal==20) {
            if (walk(f,"CHECKAL_FLEX",CHECKAL,4)) return;
            if (!Rs2Tab.isCurrentTab(InterfaceTab.EMOTES)) {
                issue("tab:emotes",Proof.FLEX_OPEN,f,0,8000,Rs2Tab::switchToEmotesTab);
                return;
            }
            Widget flex=Microbot.getClientThread().invoke((Supplier<Widget>)()->{
                Widget root=Microbot.getClient().getWidget(216,1);
                return findSprite(root,2426);
            });
            if (flex==null) { hold("Flex emote widget sprite 2426 absent"); return; }
            issue("emote:flex",Proof.DIALOGUE_CHANGED,f,0,12000,()->Rs2Widget.clickWidget(flex));
            return;
        }
        if (f.checkal==40) { talkCheckal(f,"talk:checkal-finish"); return; }
        hold("Unmapped Checkal varbit "+f.checkal);
    }
    private void talkCheckal(Frame f,String key) {
        if (walk(f,key,CHECKAL,4)) return;
        Rs2NpcModel npc=Microbot.getRs2NpcCache().query()
            .withId(NpcID.BIM_CHECKAL).within(CHECKAL,9).nearestOnClientThread();
        if (npc==null) { hold("Checkal not visible at "+CHECKAL); return; }
        issue(key,Proof.DIALOGUE_CHANGED,f,0,12000,()->npc.click("Talk-to"));
    }
    private static Widget findSprite(Widget widget,int sprite) {
        if (widget==null) return null;
        if (widget.getSpriteId()==sprite && !widget.isHidden()) return widget;
        for (Widget[] children:new Widget[][]{widget.getStaticChildren(),widget.getDynamicChildren(),widget.getNestedChildren()})
            if (children!=null) for (Widget child:children) {
                Widget found=findSprite(child,sprite); if (found!=null) return found;
            }
        return null;
    }
    private void dialogue(Frame f) {
        stage="DIALOGUE";
        boolean expected=(f.varp<=7 && near(f.position,WILLOW,12))
            || (f.varp==10 && (near(f.position,CHECKAL,12) || near(f.position,ATLAS,12)));
        if (!expected) { hold("Dialogue outside Build2 NPC route at "+f.position+": "+f.dialogue); return; }
        if (!f.options.isEmpty()) {
            for (String option:f.options) {
                String text=plain(option);
                if (text.equals("yes.") || text.equals("yes")
                    || text.equals("rock.") || text.equals("rock")) {
                    issue("dialogue:"+text,Proof.DIALOGUE_CHANGED,f,0,9000,
                        ()->Rs2Dialogue.clickOption(option)); return;
                }
            }
            hold("Unrecognized dialogue options: "+f.options); return;
        }
        if (f.hasContinue) {
            issue("dialogue:continue",Proof.DIALOGUE_CHANGED,f,0,9000,
                ()->{Rs2Dialogue.clickContinue(); return true;}); return;
        }
        hold("Dialogue without Continue or accepted option: "+f.dialogue);
    }
    private static String plain(String s) { return s==null?"":s.replaceAll("<[^>]*>","").trim().toLowerCase(); }
    private static boolean contains(int[] ids,int id) { for (int value:ids) if (value==id) return true; return false; }
    private void issue(String key,Proof proof,Frame before,int item,long timeout,BooleanSupplier action) {
        boolean accepted=action.getAsBoolean();
        pending=new Pending(key,proof,before,item,timeout);
        log.info("[BelowIceMountain] ACTION key={} accepted={} varp={} checkal={} pos={}",
            key,accepted,before.varp,before.checkal,before.position);
    }
    private void verify(Frame f) {
        Pending p=pending;
        if (proved(p,f)) {
            log.info("[BelowIceMountain] PROVED key={} varp={} checkal={} pos={}",
                p.key,f.varp,f.checkal,f.position);
            pending=null; failures.remove(p.key); nextAt=System.currentTimeMillis()+350;
            return;
        }
        if (System.currentTimeMillis()-p.at<p.timeout) { stage="VERIFY_"+p.key; return; }
        int attempts=failures.merge(p.key,1,Integer::sum);
        pending=null;
        if (attempts>=2) hold("Unproved "+p.key+" x"+attempts+" at "+f.position
            +" varp="+f.varp+" checkal="+f.checkal);
        else log.warn("[BelowIceMountain] RETRY {} {}/2",p.key,attempts);
    }
    private boolean proved(Pending p,Frame f) {
        if (!"LOGGED_IN".equals(f.gameState) || f.position==null) return false;
        switch (p.proof) {
            case QUEST_ADVANCED: return f.varp!=p.before.varp || !f.questState.equals(p.before.questState);
            case VARBIT_CHANGED: return f.checkal!=p.before.checkal || f.varp!=p.before.varp;
            case DIALOGUE_CHANGED: return f.varp!=p.before.varp || f.checkal!=p.before.checkal
                || f.marley!=p.before.marley || f.burntof!=p.before.burntof
                || f.inDialogue!=p.before.inDialogue || f.hasContinue!=p.before.hasContinue
                || !plain(f.dialogue).equals(plain(p.before.dialogue))
                || !f.options.equals(p.before.options);
            case ITEM_GAINED: return f.count(p.item)>p.before.count(p.item);
            case INVENTORY_SHED: return f.inventorySlots<p.before.inventorySlots;
            case BANK_TOGGLED: return f.bankOpen!=p.before.bankOpen;
            case ISLAND_EXIT: return p.before.position.getX()<1700 && f.position.getX()>2500;
            case FLEX_OPEN: return Rs2Tab.isCurrentTab(InterfaceTab.EMOTES);
            default: return false;
        }
    }
    private boolean walk(Frame f,String key,WorldPoint target,int radius) {
        if (near(f.position,target,radius)) {
            if (route!=null) { cancelRoute(); return true; }
            return false;
        }
        if (cancellingRoute!=null) { stage="WAIT_ROUTE_STOP"; return true; }
        if (route!=null && (!route.key.equals(key) || !route.target.equals(target))) {
            cancelRoute(); return true;
        }
        if (route==null) {
            Route r=new Route(key,target,radius); route=r;
            r.worker=new Thread(()->{
                try { Rs2Walker.walkWithStateUntil(r.target,r.radius,
                    ()->stopped || Thread.currentThread().isInterrupted()
                        || System.currentTimeMillis()-r.at>20000); }
                finally { r.done=true; }
            },"BIM-route");
            r.worker.setDaemon(true); r.worker.start();
            log.info("[BelowIceMountain] ROUTE {} {} -> {}",key,f.position,target);
        }
        stage="WALK_"+key;
        if (System.currentTimeMillis()-route.at>50000 || route.done) {
            cancelRoute(); int n=failures.merge("walk:"+key,1,Integer::sum);
            if (n>=2) hold("Route "+key+" failed x"+n+" at "+f.position+" target="+target);
        }
        return true;
    }
    private void cancelRoute() {
        Route current=route;
        if (current==null) return;
        route=null; cancellingRoute=current; routeClearAt=System.currentTimeMillis();
        if (current.worker!=null && current.worker.isAlive()) current.worker.interrupt();
        routeClear=new Thread(()->Rs2Walker.clearWalkingRoute("belowicemountain:cancel"),"BIM-clear-route");
        routeClear.setDaemon(true); routeClear.start();
    }
    private void hold(String reason) {
        if (error.isEmpty()) log.error("[BelowIceMountain] HOLD {}",reason);
        error=reason; stage="HOLD"; cancelRoute();
    }
    private static boolean near(WorldPoint a,WorldPoint b,int radius) {
        return a!=null && b!=null && a.getPlane()==b.getPlane()
            && Math.max(Math.abs(a.getX()-b.getX()),Math.abs(a.getY()-b.getY()))<=radius;
    }
    private boolean armed() {
        if (config != null && config.allowActions()
            && config.approvedPid() == ProcessHandle.current().pid()
            && config.approvedBuild() == BUILD_NUMBER
            && classHash.matches("[a-f0-9]{64}")
            && classHash.equalsIgnoreCase(config.approvedSha256())) return true;
        if (!Files.isRegularFile(CONTROL) || !classHash.matches("[a-f0-9]{64}")) return false;
        Properties p=new Properties();
        try (InputStream in=Files.newInputStream(CONTROL)) { p.load(in); }
        catch (Exception ex) { log.warn("[BelowIceMountain] control read: {}",ex.toString()); return false; }
        return "true".equalsIgnoreCase(p.getProperty("enableActions","false"))
            && Long.toString(ProcessHandle.current().pid()).equals(p.getProperty("expectedPid"))
            && Integer.toString(BUILD_NUMBER).equals(p.getProperty("expectedBuild"))
            && classHash.equalsIgnoreCase(p.getProperty("expectedClassSha",""));
    }
    private static String classSha() {
        String entryName = BelowIceMountainScript.class.getName().replace('.', '/') + ".class";
        try {
            java.net.URL location = BelowIceMountainScript.class.getProtectionDomain()
                .getCodeSource().getLocation();
            try (JarFile archive = new JarFile(Paths.get(location.toURI()).toFile())) {
                JarEntry entry = archive.getJarEntry(entryName);
                if (entry == null) return "UNKNOWN";
                try (InputStream in = archive.getInputStream(entry)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[8192]; int size;
            while ((size = in.read(bytes)) > 0) digest.update(bytes, 0, size);
            StringBuilder result = new StringBuilder();
            for (byte b : digest.digest()) result.append(String.format("%02x", b & 255));
            return result.toString();
                }
            }
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
            p.setProperty("observationOnly", "false");
            p.setProperty("guardianActions", "false");
            p.setProperty("actionsArmed", Boolean.toString(armed()));
            p.setProperty("stage", stage);
            p.setProperty("error", error);
            p.setProperty("gameState", f == null ? "UNKNOWN" : f.gameState);
            p.setProperty("loginIndex", f == null ? "-1" : Integer.toString(f.loginIndex));
            p.setProperty("world", f == null ? "-1" : Integer.toString(f.world));
            p.setProperty("position", f == null ? "UNKNOWN" : String.valueOf(f.position));
            p.setProperty("questState", f == null ? "UNKNOWN" : f.questState);
            p.setProperty("questVarp2951", f == null ? "-1" : Integer.toString(f.varp));
            p.setProperty("checkalVarbit12065",f==null?"-1":Integer.toString(f.checkal));
            p.setProperty("marleyVarbit12064",f==null?"-1":Integer.toString(f.marley));
            p.setProperty("burntofVarbit12066",f==null?"-1":Integer.toString(f.burntof));
            p.setProperty("questPointsVarp101", f == null ? "-1" : Integer.toString(f.questPoints));
            p.setProperty("miningLevel", f == null ? "-1" : Integer.toString(f.mining));
            p.setProperty("hp", f == null ? "-1" : f.hp + "/" + f.maxHp);
            p.setProperty("combatLevel", f == null ? "-1" : Integer.toString(f.combatLevel));
            p.setProperty("pickaxeCount", f == null ? "0" : Integer.toString(f.pickaxes));
            p.setProperty("foodCount", f == null ? "0" : Integer.toString(f.food));
            p.setProperty("inventorySlots", f == null ? "-1" : Integer.toString(f.inventorySlots));
            p.setProperty("bankChecked",Boolean.toString(bankChecked));
            p.setProperty("bankOpen",f==null?"false":Boolean.toString(f.bankOpen));
            p.setProperty("pending",pending==null?"":pending.key);
            p.setProperty("route",route==null?"":route.key+" -> "+route.target);
            Path temp = STATUS.resolveSibling("status.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "Below Ice Mountain early route Build 2"); }
            Files.move(temp, STATUS, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) { log.warn("[BelowIceMountain] status write: {}", ex.toString()); }
    }
}
