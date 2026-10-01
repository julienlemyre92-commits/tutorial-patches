package net.runelite.client.plugins.microbot.princealirescue;

import java.io.OutputStream;
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
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPCComposition;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.events.WelcomeScreenEvent;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.grandexchange.models.GrandExchangeOfferDetails;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.LoginManager;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** QuestHelper stage route, with one dispatched action and a later observation as proof. */
public final class PrinceAliRescueScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(PrinceAliRescueScript.class);
    public static final int BUILD_NUMBER = 12;
    private static final int VARP = 273;
    private static final int SOFT_CLAY=1761, CLAY=434, WOOL=1759, RAW_WOOL=1737, SHEARS=1735,
        DYE=1765, ONION=1957,
        REDBERRIES=1951,
        ASHES=592, WATER=1929, FLOUR=1933, BRONZE_BAR=2349, SKIRT=1013,
        BEER=1917, ROPE=954, COINS=995, WIG=2421, BLONDE_WIG=2419,
        PASTE=2424, KEY_PRINT=2423, BRONZE_KEY=2418;
    private static final int HASSAN=4285, OSMAN=4286, NED=4280, AGGIE=120,
        KELI=11578, LEELA=4274, JOE=11577, ALI=11579, CELL_DOOR=2881;
    private static final int[] SHEEP_IDS={2786,2699,2787,2693,2694,2695};
    private static final WorldPoint HASSAN_POS=new WorldPoint(3298,3163,0),
        OSMAN_POS=new WorldPoint(3286,3180,0), NED_POS=new WorldPoint(3097,3257,0),
        AGGIE_POS=new WorldPoint(3086,3257,0), KELI_POS=new WorldPoint(3127,3244,0),
        LEELA_POS=new WorldPoint(3113,3262,0), JOE_POS=new WorldPoint(3124,3245,0),
        CELL_POS=new WorldPoint(3123,3240,0), CELL_DOOR_POS=new WorldPoint(3123,3243,0),
        THESSALIA_POS=new WorldPoint(3206,3415,0),
        BLUE_MOON_POS=new WorldPoint(3228,3393,0),
        WYDIN_POS=new WorldPoint(3013,3204,0),
        FRED_POS=new WorldPoint(3190,3273,0), SHEEP_FIELD=new WorldPoint(3201,3268,0),
        CASTLE_STAIRS_GROUND=new WorldPoint(3204,3207,0),
        CASTLE_STAIRS_FIRST=new WorldPoint(3204,3207,1),
        WOOL_WHEEL=new WorldPoint(3209,3212,1), WOOL_WHEEL_ROOM=new WorldPoint(3210,3212,1);
    private static final Path STATUS=Paths.get(System.getProperty("user.home"),
        ".runelite","princealirescue","status.properties");

    private static final class Frame {
        GameState game; QuestState quest; WorldPoint pos; int varp, loginIndex, world, canvasWidth;
        double health; boolean bank, shop, continuePrompt, inDialogue, production; int animation=-1;
        String dialogue="", options="";
        final Map<Integer,Integer> items=new HashMap<>();
        int count(int id) { return items.getOrDefault(id,0); }
    }
    private static final class Pending {
        final String action; final Frame before; final long deadline; final int item;
        final WorldPoint target;
        Pending(String action,Frame before,long timeout,int item,WorldPoint target) {
            this.action=action; this.before=before; this.deadline=System.currentTimeMillis()+timeout;
            this.item=item; this.target=target;
        }
    }
    private BooleanSupplier ownsInput;
    private volatile boolean stopped, held;
    private String phase="START", error="";
    private Pending pending;
    private int loginAttempts, welcomeAttempts, disconnectAttempts, selectedWorld, routeFailures;
    private long loginAt, welcomeAt, disconnectAt, lastMoveAt;
    private String lastRoute="";
    private boolean bankInspected, keySubmitted, reloadStateRestored;
    private boolean keyHandinPending;
    private String restoredInFlightAction="", lastReloadHoldError="";
    private final Map<String,Integer> talkAttempts=new HashMap<>();
    private String lastMaterialSignature="";
    private int sourceItem, sourceGoal, sourceAttempts, sourceSpent, sourceLastCount, sourceLastCoins;
    private long sourceStartedAt;
    private long sourceShopOpenedAt;
    private int geQuote, geInitialItem, geInitialCoins, geQuantity, geReservedTotal;
    private long geOfferAt;
    private String geStage="";
    private String geOfferSlotName="";
    private final Set<String> failedWoolSheep=new HashSet<>();
    private String woolShearTarget="";
    private int woolShearFailures=0, lastRawWool=-1, lastWoolBalls=-1;
    private boolean woolSpinning=false;
    private long woolProgressAt=0, woolNoSheepSince=0;
    private static final Set<Integer> QUEST_ITEMS=Set.of(SOFT_CLAY,CLAY,WOOL,RAW_WOOL,SHEARS,DYE,ONION,REDBERRIES,
        ASHES,WATER,FLOUR,BRONZE_BAR,SKIRT,BEER,ROPE,COINS,WIG,BLONDE_WIG,
        PASTE,KEY_PRINT,BRONZE_KEY);

    public boolean run(PrinceAliRescueConfig config, BooleanSupplier owner) {
        if (isRunning()) return true;
        ownsInput=owner;
        LOG.info("[PrinceAliRescue] RUNNING_BUILD={} pid={}",BUILD_NUMBER,ProcessHandle.current().pid());
        long delay=Math.max(500,Math.min(2000,config.tickDelay()));
        mainScheduledFuture=scheduledExecutorService.scheduleWithFixedDelay(this::tick,0,delay,TimeUnit.MILLISECONDS);
        return true;
    }
    public int runtimeBuild() { return BUILD_NUMBER; }
    @Override public void shutdown() {
        stopped=true;
        if(mainScheduledFuture!=null) mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow(); super.shutdown();
    }
    public boolean awaitStopped() {
        try { return scheduledExecutorService.awaitTermination(5,TimeUnit.SECONDS); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); return false; }
    }
    public Map<String,Object> quiesceForReload() {
        if(mainScheduledFuture!=null) mainScheduledFuture.cancel(false);
        scheduledExecutorService.shutdown();
        try {
            if(!scheduledExecutorService.awaitTermination(30,TimeUnit.SECONDS))
                throw new IllegalStateException("Prince Ali tick did not quiesce");
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("reload interrupted",e);
        }
        stopped=true;
        Map<String,Object> state=new HashMap<>();
        state.put("held",held); state.put("phase",phase); state.put("error",error);
        state.put("bankInspected",bankInspected); state.put("keySubmitted",keySubmitted);
        state.put("keyHandinPending",keyHandinPending);
        state.put("talkAttempts",new HashMap<>(talkAttempts));
        state.put("lastMaterialSignature",lastMaterialSignature);
        state.put("sourceItem",sourceItem); state.put("sourceGoal",sourceGoal);
        state.put("sourceAttempts",sourceAttempts); state.put("sourceSpent",sourceSpent);
        state.put("sourceLastCount",sourceLastCount); state.put("sourceStartedAt",sourceStartedAt);
        state.put("sourceLastCoins",sourceLastCoins);
        state.put("sourceShopOpenedAt",sourceShopOpenedAt);
        state.put("geQuote",geQuote); state.put("geInitialItem",geInitialItem);
        state.put("geInitialCoins",geInitialCoins); state.put("geQuantity",geQuantity);
        state.put("geReservedTotal",geReservedTotal); state.put("geOfferAt",geOfferAt);
        state.put("geStage",geStage);
        state.put("geOfferSlotName",geOfferSlotName);
        state.put("failedWoolSheep",new HashSet<>(failedWoolSheep));
        state.put("woolShearTarget",woolShearTarget); state.put("woolShearFailures",woolShearFailures);
        state.put("lastRawWool",lastRawWool); state.put("lastWoolBalls",lastWoolBalls);
        state.put("woolSpinning",woolSpinning); state.put("woolProgressAt",woolProgressAt);
        state.put("woolNoSheepSince",woolNoSheepSince);
        state.put("selectedWorld",selectedWorld); state.put("loginAttempts",loginAttempts);
        state.put("welcomeAttempts",welcomeAttempts); state.put("disconnectAttempts",disconnectAttempts);
        state.put("loginAt",loginAt); state.put("welcomeAt",welcomeAt);
        state.put("disconnectAt",disconnectAt); state.put("routeFailures",routeFailures);
        state.put("lastRoute",lastRoute); state.put("lastMoveAt",lastMoveAt);
        // Do not replay an in-flight click after reload. Resume from a fresh scene observation.
        state.put("pendingAction",pending==null?"":pending.action);
        super.shutdown(); return state;
    }
    public void restoreReloadState(Map<String,Object> state) {
        if(isRunning()) throw new IllegalStateException("running script reload");
        held=(Boolean)state.getOrDefault("held",false);
        phase=(String)state.getOrDefault("phase","RESUME");
        error=(String)state.getOrDefault("error","");
        lastReloadHoldError=error;
        restoredInFlightAction="";
        if(held && "HOLD".equals(phase)
            && error.startsWith("Unrecognized Prince Ali dialogue options:")
            && error.contains("No. I think I know everything I need to.")) {
            held=false; phase="RESUME_KNOWN_OSMAN_DIALOGUE"; error="";
            LOG.info("[PrinceAliRescue] Reload recovery: resuming only the newly recognized Osman option menu");
        }
        bankInspected=(Boolean)state.getOrDefault("bankInspected",false);
        keySubmitted=(Boolean)state.getOrDefault("keySubmitted",false);
        keyHandinPending=(Boolean)state.getOrDefault("keyHandinPending",false);
        Object savedAttempts=state.get("talkAttempts");
        if(savedAttempts instanceof Map<?,?>) for(Map.Entry<?,?> entry:((Map<?,?>)savedAttempts).entrySet())
            if(entry.getKey() instanceof String && entry.getValue() instanceof Integer)
                talkAttempts.put((String)entry.getKey(),(Integer)entry.getValue());
        lastMaterialSignature=(String)state.getOrDefault("lastMaterialSignature","");
        sourceItem=(Integer)state.getOrDefault("sourceItem",0);
        sourceGoal=(Integer)state.getOrDefault("sourceGoal",0);
        sourceAttempts=(Integer)state.getOrDefault("sourceAttempts",0);
        sourceSpent=(Integer)state.getOrDefault("sourceSpent",0);
        sourceLastCount=(Integer)state.getOrDefault("sourceLastCount",0);
        sourceLastCoins=(Integer)state.getOrDefault("sourceLastCoins",0);
        sourceStartedAt=(Long)state.getOrDefault("sourceStartedAt",0L);
        sourceShopOpenedAt=(Long)state.getOrDefault("sourceShopOpenedAt",0L);
        geQuote=(Integer)state.getOrDefault("geQuote",0);
        geInitialItem=(Integer)state.getOrDefault("geInitialItem",0);
        geInitialCoins=(Integer)state.getOrDefault("geInitialCoins",0);
        geQuantity=(Integer)state.getOrDefault("geQuantity",0);
        geReservedTotal=(Integer)state.getOrDefault("geReservedTotal",0);
        geOfferAt=(Long)state.getOrDefault("geOfferAt",0L);
        geStage=(String)state.getOrDefault("geStage","");
        geOfferSlotName=(String)state.getOrDefault("geOfferSlotName","");
        Object savedFailedSheep=state.get("failedWoolSheep");
        if(savedFailedSheep instanceof Set<?>) for(Object key:(Set<?>)savedFailedSheep)
            if(key instanceof String) failedWoolSheep.add((String)key);
        woolShearTarget=(String)state.getOrDefault("woolShearTarget","");
        woolShearFailures=(Integer)state.getOrDefault("woolShearFailures",0);
        lastRawWool=(Integer)state.getOrDefault("lastRawWool",-1);
        lastWoolBalls=(Integer)state.getOrDefault("lastWoolBalls",-1);
        woolSpinning=(Boolean)state.getOrDefault("woolSpinning",false);
        woolProgressAt=(Long)state.getOrDefault("woolProgressAt",0L);
        woolNoSheepSince=(Long)state.getOrDefault("woolNoSheepSince",0L);
        if(held && "HOLD".equals(phase) && sourceItem==WOOL
            && error.startsWith("GE quote unavailable/above 1000gp cumulative cap id="+WOOL)) {
            held=false; phase="RESUME_LOCAL_WOOL_SOURCE"; error=""; geStage="WOOL_GATHER";
            sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0;
            LOG.info("[PrinceAliRescue] Reload recovery: switching wool from unavailable GE quote to local sheep/shears/spinning-wheel source");
        }
        selectedWorld=(Integer)state.getOrDefault("selectedWorld",0);
        loginAttempts=(Integer)state.getOrDefault("loginAttempts",0);
        welcomeAttempts=(Integer)state.getOrDefault("welcomeAttempts",0);
        disconnectAttempts=(Integer)state.getOrDefault("disconnectAttempts",0);
        loginAt=(Long)state.getOrDefault("loginAt",0L);
        welcomeAt=(Long)state.getOrDefault("welcomeAt",0L);
        disconnectAt=(Long)state.getOrDefault("disconnectAt",0L);
        routeFailures=(Integer)state.getOrDefault("routeFailures",0);
        lastRoute=(String)state.getOrDefault("lastRoute","");
        lastMoveAt=(Long)state.getOrDefault("lastMoveAt",0L);
        String inFlight=(String)state.getOrDefault("pendingAction","");
        if(!inFlight.isEmpty()) {
            restoredInFlightAction=inFlight;
            held=true; phase="HOLD_RELOAD_IN_FLIGHT";
            error="Reload during "+inFlight+"; inspect quest/inventory/scene before resuming";
        }
        reloadStateRestored=true;
    }

    private void tick() {
        if(stopped||Thread.currentThread().isInterrupted()) return;
        try {
            if(ownsInput!=null&&!ownsInput.getAsBoolean()) { phase="YIELD_OTHER_PLUGIN"; status(null); return; }
            Frame f=Microbot.getClientThread().invoke((java.util.function.Supplier<Frame>)this::observe);
            if(f==null) { phase="WAIT_CLIENT"; status(null); return; }
            if(held) {
                if(recoverObservedWoolSpinAfterReload(f)) { status(f); return; }
                if(recoverObservedWoolDescentHold(f)) { status(f); return; }
                if(recoverObservedWoolGatherPlaneHold(f)) { status(f); return; }
                if(recoverObservedWoolStairRouteHold(f)) { status(f); return; }
                status(f); return;
            }
            if(loginTick(f)) { status(f); return; }
            if(f.quest==QuestState.FINISHED) { phase="COMPLETE_QUEST_STATE"; status(f); return; }
            if(keyHandinPending&&f.count(KEY_PRINT)==0) {
                keySubmitted=true; keyHandinPending=false;
            }
            String signature=f.varp+":"+f.items.hashCode();
            if(!signature.equals(lastMaterialSignature)) {
                talkAttempts.clear(); lastMaterialSignature=signature;
            }
            if(f.health>=0&&f.health<25) { hold(f,"Health below 25%; jail guard risk, no unverified combat recovery"); return; }
            if(pending!=null) {
                if(pending.action.startsWith("SOURCE_")
                    &&f.count(pending.item)>pending.before.count(pending.item)) {
                    int coinDelta=pending.before.count(COINS)-f.count(COINS);
                    if(coinDelta<=0||coinDelta>unitCap(pending.item)) {
                        hold(f,"Source item gain lacked verified bounded coin loss id="+pending.item+
                            " coinDelta="+coinDelta); return;
                    }
                }
                if(proved(pending,f)) {
                    LOG.info("[PrinceAliRescue] PROVED {} varp={} pos={}",pending.action,f.varp,f.pos);
                    if("GIVE_PRINT_OSMAN".equals(pending.action)&&f.count(KEY_PRINT)<pending.before.count(KEY_PRINT)) keySubmitted=true;
                    if(pending.action.startsWith("WALK_")) { routeFailures=0; lastMoveAt=System.currentTimeMillis(); }
                    if("WOOL_SHEAR".equals(pending.action)) {
                        failedWoolSheep.clear(); woolShearTarget=""; woolShearFailures=0;
                    }
                    if("WOOL_SPIN".equals(pending.action)) {
                        woolSpinning=true; woolProgressAt=System.currentTimeMillis();
                        lastRawWool=f.count(RAW_WOOL); lastWoolBalls=f.count(WOOL);
                    }
                    if("SOURCE_SHOP_OPEN".equals(pending.action))
                        sourceShopOpenedAt=System.currentTimeMillis();
                    if("GE_PLACE".equals(pending.action)) {
                        geStage="WAIT_FILL"; geOfferAt=System.currentTimeMillis();
                        geReservedTotal+=geQuote*geQuantity;
                        GrandExchangeOfferDetails placed=Rs2GrandExchange.hasBuyOffer(pending.item);
                        geOfferSlotName=placed==null?"":placed.getSlot().name();
                    }
                    if("GE_COLLECT".equals(pending.action)) geStage="DONE";
                    if("GE_CANCEL".equals(pending.action)) geStage="CANCELLED";
                    phase="PROVED_"+pending.action; pending=null; status(f); return;
                }
                if("WOOL_OPEN_WHEEL".equals(pending.action)
                    &&f.count(WOOL)>pending.before.count(WOOL)
                    &&f.count(RAW_WOOL)<pending.before.count(RAW_WOOL)) {
                    woolSpinning=true; woolProgressAt=System.currentTimeMillis();
                    lastRawWool=f.count(RAW_WOOL); lastWoolBalls=f.count(WOOL);
                    LOG.info("[PrinceAliRescue] PROVED WOOL_SPIN_BY_INVENTORY after wheel-open action rawWool={} balls={} pos={}",
                        f.count(RAW_WOOL),f.count(WOOL),f.pos);
                    phase="PROVED_WOOL_SPIN_BY_INVENTORY"; pending=null; status(f); return;
                }
                if(System.currentTimeMillis()>=pending.deadline) {
                    if("WOOL_SHEAR".equals(pending.action)) {
                        if(!woolShearTarget.isEmpty()) failedWoolSheep.add(woolShearTarget);
                        woolShearTarget=""; pending=null; woolShearFailures++;
                        if(woolShearFailures>=3) {
                            hold(f,"Three live sheep Shear attempts produced no raw-wool increase; targets="+failedWoolSheep);
                        } else { phase="WOOL_RESCAN_"+woolShearFailures; status(f); }
                        return;
                    }
                    if(pending.action.startsWith("WALK_")&&routeFailures++<2) {
                        phase="ROUTE_RESCAN"; pending=null; status(f); return;
                    }
                    hold(f,"Unproved "+pending.action+"; before varp="+pending.before.varp+
                        " now="+f.varp+" beforePos="+pending.before.pos+" nowPos="+f.pos);
                } else status(f);
                return;
            }
            if(sourceItem!=0) { sourceTick(f); return; }
            if(Rs2Dialogue.hasSelectAnOption()) { dialogueOption(f); return; }
            if(Rs2Dialogue.hasContinue()) {
                Rs2Dialogue.clickContinue(); set("CONTINUE",f,6000,0,null); return;
            }
            if(f.inDialogue) { hold(f,"Unrecognized dialogue without Continue/option: "+f.dialogue); return; }
            if(f.varp==0) { talk(f,HASSAN,HASSAN_POS,"START_HASSAN"); return; }
            if(f.varp==10) { talk(f,OSMAN,OSMAN_POS,"START_OSMAN"); return; }
            if(f.varp>=100) { talk(f,HASSAN,HASSAN_POS,"FINISH_HASSAN"); return; }
            if(f.varp>=50) { freeAli(f); return; }
            if(f.varp>=40) { useRopeOnKeli(f); return; }
            if(f.varp>=30) {
                for(int id:new int[]{BEER,BRONZE_KEY,BLONDE_WIG,PASTE,ROPE,SKIRT})
                    if(id!=BEER||f.varp<33) if(!need(f,id,id==BEER?33-f.varp:1)) return;
                talk(f,JOE,JOE_POS,"GIVE_BEER_JOE"); return;
            }
            if(f.varp==20) { prepare(f); return; }
            hold(f,"Unknown Prince Ali quest varp="+f.varp+" state="+f.quest);
        } catch(Throwable ex) {
            held=true; phase="HOLD_EXCEPTION"; error=ex.toString();
            LOG.error("[PrinceAliRescue] HOLD_EXCEPTION",ex); status(null);
        }
    }
    private Frame observe() {
        Client c=Microbot.getClient(); if(c==null) return null;
        Frame f=new Frame(); f.game=c.getGameState(); f.loginIndex=c.getLoginIndex();
        f.world=c.getWorld(); f.canvasWidth=c.getCanvasWidth();
        if(f.game!=GameState.LOGGED_IN||c.getLocalPlayer()==null) return f;
        f.pos=c.getLocalPlayer().getWorldLocation(); f.animation=c.getLocalPlayer().getAnimation();
        f.quest=Quest.PRINCE_ALI_RESCUE.getState(c); f.varp=c.getVarpValue(VARP);
        f.health=Rs2Player.getHealthPercentage(); f.bank=Rs2Bank.isOpen(); f.shop=Rs2Shop.isOpen();
        f.production=findProduct(c)!=null;
        f.continuePrompt=Rs2Dialogue.hasContinue(); f.inDialogue=Rs2Dialogue.isInDialogue();
        String text=Rs2Dialogue.getDialogueText(); f.dialogue=text==null?"":text;
        StringBuilder options=new StringBuilder();
        for(Widget w:Rs2Dialogue.getDialogueOptions())
            if(w!=null&&w.getText()!=null) options.append(w.getText()).append('|');
        f.options=options.toString();
        Rs2Inventory.items().forEach(item -> f.items.merge(item.getId(),item.getQuantity(),Integer::sum));
        return f;
    }
    private boolean loginTick(Frame f) {
        long now=System.currentTimeMillis();
        if(f.game==GameState.LOGGED_IN) {
            WelcomeScreenEvent welcome=new WelcomeScreenEvent();
            if(!welcome.validate()) { loginAttempts=welcomeAttempts=disconnectAttempts=0; return false; }
            phase="WAIT_WELCOME";
            if(welcomeAttempts++==0) { welcomeAt=now; welcome.execute(); }
            else if(now-welcomeAt>12000) hold(f,"Welcome screen persisted after native execute");
            return true;
        }
        if(f.game!=GameState.LOGIN_SCREEN) { phase="WAIT_LOGIN_SCREEN"; return true; }
        if(f.loginIndex==24) {
            if(disconnectAttempts++==0&&f.canvasWidth>0) {
                Microbot.getClientThread().invoke(() -> {
                    Microbot.getMouse().click(365+(f.canvasWidth-804)/2,308); return true;
                });
                disconnectAt=now; phase="VERIFY_DISCONNECT_DISMISS";
            } else if(now-disconnectAt>8000) hold(f,"Disconnected modal remained after native dismiss");
            return true;
        }
        if(disconnectAttempts>0) {
            disconnectAttempts=0; loginAttempts=0; loginAt=0;
            phase="DISCONNECT_DISMISSED"; return true;
        }
        disconnectAttempts=0;
        if(f.loginIndex!=10&&f.loginIndex!=34) { phase="WAIT_LOGIN_INDEX_"+f.loginIndex; return true; }
        if(selectedWorld==0) {
            selectedWorld=LoginManager.getRandomWorld(false);
            if(selectedWorld<=0||LoginManager.isMemberWorld(selectedWorld)) {
                hold(f,"No verified ordinary free world from native LoginManager"); return true;
            }
        }
        if(loginAttempts++==0) {
            loginAt=now; phase="VERIFY_NATIVE_LOGIN";
            if(!LoginManager.login(selectedWorld)) hold(f,"Native LoginManager.login rejected");
        } else if(now-loginAt>20000) hold(f,"Native login did not reach game; index="+f.loginIndex);
        return true;
    }
    private void prepare(Frame f) {
        // Reconcile only the supplies required for the current unfinished subphase.
        if(!bankInspected) {
            if(!f.bank) {
                WorldPoint bankPoint=nearestBank(f.pos);
                if(f.pos==null||f.pos.distanceTo(bankPoint)>8) walk(f,bankPoint,"TO_SUPPLY_BANK");
                else if(Rs2Bank.openBank()) set("OPEN_BANK",f,15000,0,null);
                else hold(f,"Initial supply bank open rejected");
                return;
            }
            bankInspected=true; phase="BANK_INVENTORY_INSPECTED"; status(f); return;
        }
        for(int finished:new int[]{BLONDE_WIG,PASTE,BRONZE_KEY,KEY_PRINT})
            if((finished!=KEY_PRINT||f.count(BRONZE_KEY)==0)
                &&withdrawFinishedIfBanked(f,finished)) return;
        if(f.count(BLONDE_WIG)==0) {
            if(f.count(WIG)>0) {
                if(!need(f,DYE,1)) return;
                if(closeIfOpen(f)) return;
                if(Rs2Inventory.combine(WIG,DYE)) set("DYE_WIG",f,7000,BLONDE_WIG,null);
                else hold(f,"Wig + yellow dye combine rejected");
                return;
            }
            if(!need(f,WOOL,3)) return;
            talk(f,NED,NED_POS,"MAKE_WIG"); return;
        }
        if(f.count(PASTE)==0) {
            for(int id:new int[]{REDBERRIES,ASHES,WATER,FLOUR}) if(!need(f,id,1)) return;
            talk(f,AGGIE,AGGIE_POS,"MAKE_PASTE"); return;
        }
        if(f.count(BRONZE_KEY)==0) {
            if(f.count(KEY_PRINT)>0) {
                if(!need(f,BRONZE_BAR,1)) return;
                talk(f,OSMAN,OSMAN_POS,"GIVE_PRINT_OSMAN"); return;
            }
            if(!keySubmitted) {
                if(!need(f,SOFT_CLAY,1)) return;
                talk(f,KELI,KELI_POS,"GET_KEY_PRINT"); return;
            }
        }
        for(int id:new int[]{BEER,ROPE,SKIRT})
            if(!need(f,id,id==BEER?3:1)) return;
        talk(f,LEELA,LEELA_POS,"GET_KEY_LEELA");
    }
    private boolean withdrawFinishedIfBanked(Frame f,int id) {
        if(!f.bank||f.count(id)>0||!Rs2Bank.hasBankItem(id,1)) return false;
        if(!Rs2Bank.hasWithdrawAsItem()) {
            if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
            else hold(f,"Cannot set bank withdraw-as-item for finished supply "+id);
            return true;
        }
        if(Rs2Bank.withdrawDeficit(id,1)) set("WITHDRAW",f,7000,id,null);
        else hold(f,"Finished supply withdrawal rejected id="+id);
        return true;
    }
    private boolean need(Frame f,int id,int amount) {
        if(f.count(id)>=amount) return true;
        if(id==SOFT_CLAY&&f.count(CLAY)>0&&f.count(WATER)>0) {
            if(closeIfOpen(f)) return false;
            if(Rs2Inventory.combine(CLAY,WATER)) set("CRAFT_SOFT_CLAY",f,7000,SOFT_CLAY,null);
            else hold(f,"Clay + water combine rejected");
            return false;
        }
        if(id==DYE&&f.count(ONION)>=2&&f.count(COINS)>=5) {
            if(closeIfOpen(f)) return false;
            if(f.pos==null||f.pos.distanceTo(AGGIE_POS)>8) {
                walk(f,AGGIE_POS,"TO_DYE_AGGIE"); return false;
            }
            if(Rs2Npc.getNpc(AGGIE)==null) { hold(f,"Aggie not visible for yellow dye"); return false; }
            if(Rs2Inventory.useItemOnNpc(ONION,AGGIE))
                set("CRAFT_YELLOW_DYE",f,10000,DYE,null);
            else hold(f,"Onion on Aggie rejected for yellow dye");
            return false;
        }
        if(!f.bank) {
            WorldPoint bankPoint=nearestBank(f.pos);
            if(f.pos==null||f.pos.distanceTo(bankPoint)>8) {
                walk(f,bankPoint,"TO_SUPPLY_BANK"); return false;
            }
            if(Rs2Bank.openBank()) set("OPEN_BANK",f,15000,0,null);
            else hold(f,"Bank open rejected for item "+id);
            return false;
        }
        bankInspected=true;
        int deficit=amount-f.count(id);
        if(id==SOFT_CLAY&&(f.count(CLAY)>0||Rs2Bank.hasBankItem(CLAY,1))
            &&(f.count(WATER)>0||Rs2Bank.hasBankItem(WATER,1))) {
            int ingredient=f.count(CLAY)==0?CLAY:f.count(WATER)==0?WATER:0;
            if(ingredient!=0) {
                if(!Rs2Bank.hasWithdrawAsItem()) {
                    if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                    else hold(f,"Soft-clay ingredient withdraw mode rejected");
                } else if(Rs2Bank.withdrawDeficit(ingredient,1))
                    set("WITHDRAW",f,7000,ingredient,null);
                else hold(f,"Soft-clay ingredient withdrawal rejected id="+ingredient);
                return false;
            }
        }
        if(id==DYE&&(f.count(ONION)>=2||
            Rs2Bank.hasBankItem(ONION,2-f.count(ONION)))) {
            int ingredient=f.count(ONION)<2?ONION:f.count(COINS)<5?COINS:0;
            int required=ingredient==ONION?2:5;
            if(ingredient!=0&&Rs2Bank.hasBankItem(ingredient,required-f.count(ingredient))) {
                if(!Rs2Bank.hasWithdrawAsItem()) {
                    if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                    else hold(f,"Yellow-dye ingredient withdraw mode rejected");
                } else if(Rs2Bank.withdrawDeficit(ingredient,required))
                    set("WITHDRAW",f,7000,ingredient,null);
                else hold(f,"Yellow-dye ingredient withdrawal rejected id="+ingredient);
                return false;
            }
        }
        if(!Rs2Bank.hasBankItem(id,deficit)) {
            int partial=Rs2Bank.count(id);
            if(partial>0) {
                if(!Rs2Bank.hasWithdrawAsItem()) {
                    if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                    else hold(f,"Cannot set bank withdraw-as-item for partial id="+id);
                } else if(Rs2Bank.withdrawX(id,Math.min(deficit,partial)))
                    set("WITHDRAW",f,7000,id,null);
                else hold(f,"Partial bank withdrawal rejected id="+id+" available="+partial);
                return false;
            }
            beginSource(f,id,amount);
            return false;
        }
        if(!Rs2Bank.hasWithdrawAsItem()) {
            if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
            else hold(f,"Cannot set bank withdraw-as-item");
            return false;
        }
        if(Rs2Inventory.isFull()&&f.count(id)==0) {
            net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel extra=
                Rs2Inventory.items().filter(item->!QUEST_ITEMS.contains(item.getId())&&!item.isFood())
                    .findFirst().orElse(null);
            if(extra==null) { hold(f,"Inventory full; no safe unneeded item to deposit for "+id); return false; }
            if(Rs2Bank.depositX(extra.getId(),extra.getQuantity()))
                set("DEPOSIT_UNNEEDED",f,7000,extra.getId(),null);
            else hold(f,"Exact bank deposit rejected for unrelated id="+extra.getId());
            return false;
        }
        if(Rs2Bank.withdrawDeficit(id,amount)) set("WITHDRAW",f,7000,id,null);
        else hold(f,"Bank withdrawal rejected id="+id+" deficit="+deficit);
        return false;
    }
    private static WorldPoint nearestBank(WorldPoint pos) {
        WorldPoint best=BankLocation.DRAYNOR_VILLAGE.getWorldPoint();
        if(pos==null) return best;
        for(BankLocation location:new BankLocation[]{BankLocation.AL_KHARID,
            BankLocation.DRAYNOR_VILLAGE,BankLocation.FALADOR_EAST,
            BankLocation.GRAND_EXCHANGE}) {
            WorldPoint candidate=location.getWorldPoint();
            if(pos.distanceTo(candidate)<pos.distanceTo(best)) best=candidate;
        }
        return best;
    }
    private void beginSource(Frame f,int id,int amount) {
        if(id!=ROPE&&id!=SKIRT&&id!=BEER&&id!=REDBERRIES&&id!=FLOUR
            &&!geEligible(id)) {
            hold(f,"No verified local source for item id="+id+" deficit="+(amount-f.count(id))+
                "; GE runtime stock/server price unverified; no blind offer placed");
            return;
        }
        sourceItem=id; sourceGoal=amount; sourceAttempts=0; sourceSpent=0;
        sourceLastCount=f.count(id); sourceStartedAt=System.currentTimeMillis();
        sourceLastCoins=f.count(COINS);
        sourceShopOpenedAt=0;
        geStage=id==WOOL?"WOOL_GATHER":geEligible(id)?"PREPARE":"";
        geQuote=geInitialItem=geInitialCoins=geQuantity=0; geOfferAt=0;
        geOfferSlotName="";
        phase="SOURCE_"+sourceName(id); status(f);
    }
    private static boolean geEligible(int id) {
        return id==SOFT_CLAY||id==WOOL||id==DYE||id==ASHES
            ||id==WATER||id==BRONZE_BAR;
    }
    private static String geName(int id) {
        return id==SOFT_CLAY?"Soft clay":id==WOOL?"Ball of wool":
            id==DYE?"Yellow dye":id==ASHES?"Ashes":
            id==WATER?"Bucket of water":id==BRONZE_BAR?"Bronze bar":"";
    }
    private static String sourceName(int id) {
        return id==WOOL?"WOOL_LOCAL_SHEEP_WHEEL":id==ROPE?"ROPE_NED":id==SKIRT?"SKIRT_THESSALIA":
            id==BEER?"BEER_BLUE_MOON":id==REDBERRIES?"REDBERRIES_WYDIN":
            id==FLOUR?"FLOUR_WYDIN":"UNKNOWN_"+id;
    }
    private static int unitCap(int id) {
        return id==ROPE?18:id==SKIRT||id==BEER?2:id==REDBERRIES?3:id==FLOUR?10:0;
    }
    private void sourceTick(Frame f) {
        int id=sourceItem, cost=unitCap(id);
        if(id==WOOL) { woolSourceTick(f); return; }
        if(geEligible(id)) { geTick(f); return; }
        int remaining=sourceGoal-f.count(id);
        if(System.currentTimeMillis()-sourceStartedAt>360000) {
            hold(f,"Source timed out after 6 minutes item="+id+" remaining="+remaining); return;
        }
        if(f.count(COINS)>sourceLastCoins) sourceLastCoins=f.count(COINS);
        if(f.count(id)>sourceLastCount) {
            int gain=f.count(id)-sourceLastCount;
            int spent=sourceLastCoins-f.count(COINS);
            if(spent<=0||spent>unitCap(id)*gain||sourceSpent+spent>100) {
                hold(f,"Source gain/coin delta invalid id="+id+" gain="+gain+
                    " spent="+spent+" total="+sourceSpent); return;
            }
            sourceSpent+=spent; sourceLastCoins=f.count(COINS);
            sourceLastCount=f.count(id); sourceAttempts=0;
        }
        if(remaining<=0) {
            if(f.shop) { Rs2Shop.closeShop(); set("SOURCE_SHOP_CLOSE",f,6000,0,null); return; }
            sourceItem=sourceGoal=sourceAttempts=sourceSpent=0; sourceShopOpenedAt=0;
            phase="SOURCE_COMPLETE_"+sourceName(id); status(f); return;
        }
        if(sourceAttempts>=3) {
            hold(f,"Three source interactions without inventory gain item="+id); return;
        }
        if(cost<=0||sourceSpent+cost>100) {
            hold(f,"Source price budget exceeded item="+id+" spent="+sourceSpent); return;
        }
        int requiredCoins=cost*remaining;
        if(f.bank) {
            if(f.count(COINS)<requiredCoins) {
                int missing=requiredCoins-f.count(COINS);
                if(!Rs2Bank.hasBankItem(COINS,missing)) {
                    hold(f,"Insufficient coins for source id="+id+" need="+requiredCoins+
                        " inventory="+f.count(COINS)+" bankShort="+missing); return;
                }
                if(!Rs2Bank.hasWithdrawAsItem()) {
                    if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                    else hold(f,"Coin withdraw mode rejected");
                } else if(Rs2Bank.withdrawDeficit(COINS,requiredCoins))
                    set("WITHDRAW",f,7000,COINS,null);
                else hold(f,"Coin deficit withdrawal rejected id="+id);
                return;
            }
            closeIfOpen(f); return;
        }
        if(f.count(COINS)<requiredCoins) {
            hold(f,"Source coin reserve fell below cap id="+id+" remaining="+remaining+
                " coins="+f.count(COINS)+" required="+requiredCoins); return;
        }
        WorldPoint target=id==ROPE?NED_POS:id==SKIRT?THESSALIA_POS:
            id==BEER?BLUE_MOON_POS:WYDIN_POS;
        if(f.pos==null||f.pos.distanceTo(target)>8) {
            walk(f,target,"TO_SOURCE_"+sourceName(id)); return;
        }
        if(id==BEER) {
            if(f.options.contains("Could I buy a beer please?")) {
                if(Rs2Dialogue.clickOption("Could I buy a beer please?"))
                    set("SOURCE_BEER_OPTION",f,7000,id,null);
                else hold(f,"Beer option click rejected");
                return;
            }
            if(Rs2Dialogue.hasContinue()) {
                Rs2Dialogue.clickContinue(); set("SOURCE_CONTINUE",f,7000,id,null); return;
            }
            if(f.inDialogue) {
                hold(f,"Bartender dialogue lacks verified beer option: "+f.options); return;
            }
            if(Rs2Npc.getNpc("Bartender")==null) {
                hold(f,"Blue Moon waypoint reached but no live Bartender; pos="+f.pos); return;
            }
            sourceAttempts++;
            if(Rs2Npc.interact("Bartender","Talk-to"))
                set("SOURCE_BEER_TALK",f,10000,id,null);
            else hold(f,"Bartender Talk-to rejected");
            return;
        }
        String shopNpc=id==ROPE?"Ned":id==SKIRT?"Thessalia":"Wydin";
        if(!f.shop) {
            if(Rs2Shop.getNearestShopNpc(shopNpc,true)==null) {
                hold(f,"Source shop NPC "+shopNpc+" not visible at "+target); return;
            }
            if(Rs2Shop.openShop(shopNpc,true)) set("SOURCE_SHOP_OPEN",f,7000,id,null);
            else hold(f,"Shop Open rejected NPC="+shopNpc);
            return;
        }
        if(Rs2Shop.shopItems==null||Rs2Shop.shopItems.isEmpty()) {
            if(sourceShopOpenedAt>0&&System.currentTimeMillis()-sourceShopOpenedAt<5000) {
                phase="WAIT_SOURCE_SHOP_STOCK"; status(f); return;
            }
            hold(f,"Shop stock list unavailable after open shop="+shopNpc); return;
        }
        if(!Rs2Shop.hasMinimumStock(id,1)) {
            hold(f,"Local shop stock unavailable item="+id+" shop="+shopNpc); return;
        }
        sourceAttempts++;
        if(Rs2Shop.buyItem(id,"1")) set("SOURCE_BUY",f,7000,id,null);
        else hold(f,"Local shop Buy-1 rejected item="+id);
    }
    private void woolSourceTick(Frame f) {
        long now=System.currentTimeMillis();
        if(now-sourceStartedAt>360000) {
            hold(f,"Local wool source exceeded six minutes; balls="+f.count(WOOL)+
                " rawWool="+f.count(RAW_WOOL)); return;
        }
        if(f.inDialogue||f.continuePrompt||!f.options.isEmpty()) {
            hold(f,"Unexpected dialogue during local wool source: "+f.dialogue+" options="+f.options); return;
        }
        if(f.bank) {
            if(f.count(SHEARS)==0&&Rs2Bank.hasBankItem(SHEARS,1)) {
                if(!Rs2Bank.hasWithdrawAsItem()) {
                    if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                    else hold(f,"Shears bank withdraw mode rejected");
                } else if(Rs2Bank.withdrawDeficit(SHEARS,1))
                    set("WITHDRAW",f,7000,SHEARS,null);
                else hold(f,"Shears withdrawal rejected");
                return;
            }
            int rawDeficit=sourceGoal-f.count(WOOL)-f.count(RAW_WOOL);
            if(rawDeficit>0&&Rs2Bank.hasBankItem(RAW_WOOL,1)) {
                if(!Rs2Bank.hasWithdrawAsItem()) {
                    if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                    else hold(f,"Raw wool bank withdraw mode rejected");
                } else if(Rs2Bank.withdrawDeficit(RAW_WOOL,rawDeficit))
                    set("WITHDRAW",f,7000,RAW_WOOL,null);
                else hold(f,"Raw wool bank withdrawal rejected; deficit="+rawDeficit);
                return;
            }
            closeIfOpen(f); return;
        }

        if(woolSpinning) {
            if(lastRawWool!=f.count(RAW_WOOL)||lastWoolBalls!=f.count(WOOL)) {
                lastRawWool=f.count(RAW_WOOL); lastWoolBalls=f.count(WOOL); woolProgressAt=now;
            }
            if(f.animation==894) {
                if(now-woolProgressAt>15000) {
                    hold(f,"Spinning animation continued 15s without wool/ball progress"); return;
                }
                phase="WAIT_WOOL_SPIN_ANIMATION"; status(f); return;
            }
            if(now-woolProgressAt<8000) { phase="WAIT_WOOL_SPIN_SETTLE"; status(f); return; }
            woolSpinning=false;
        }

            if(f.count(WOOL)>=sourceGoal) {
                if(f.pos==null) { phase="WAIT_WOOL_RETURN_POSITION"; status(f); return; }
                if(f.pos.getPlane()==1) {
                    descendForWool(f,"after wool spin");
                    return;
                }
            if(f.pos.getPlane()!=0) { hold(f,"Unexpected plane after spinning wool: "+f.pos); return; }
            sourceItem=sourceGoal=sourceAttempts=sourceSpent=0; geStage="";
            woolShearTarget=""; woolShearFailures=0; woolNoSheepSince=0;
            phase="WOOL_SOURCE_COMPLETE"; status(f); return;
        }

        if(f.count(RAW_WOOL)>0) {
            geStage="WOOL_SPIN";
            if(f.pos==null) { phase="WAIT_WOOL_POSITION"; status(f); return; }
            if(f.pos.getPlane()==0) {
                if(f.pos.distanceTo(CASTLE_STAIRS_GROUND)>4) {
                    walk(f,CASTLE_STAIRS_GROUND,"WOOL_TO_CASTLE_STAIRS"); return;
                }
                Rs2TileObjectModel stairs=object(56230,CASTLE_STAIRS_GROUND,6);
                if(stairs==null) { hold(f,"Castle upstairs stair 56230 not visible at "+f.pos); return; }
                if(stairs.click("Climb-up")) set("WOOL_CLIMB_UP",f,12000,0,CASTLE_STAIRS_GROUND);
                else hold(f,"Climb-up rejected at castle stair 56230");
                return;
            }
            if(f.pos.getPlane()!=1) { hold(f,"Unexpected plane during wool spinning: "+f.pos); return; }
            boolean inWheelRoom=f.pos.getX()>=3209&&f.pos.getX()<=3211
                &&f.pos.getY()>=3212&&f.pos.getY()<=3214;
            if(!f.production&&!inWheelRoom) {
                walk(f,WOOL_WHEEL_ROOM,"WOOL_TO_WHEEL_ROOM"); return;
            }
            if(!f.production) {
                Rs2TileObjectModel wheel=object(14889,WOOL_WHEEL,6);
                if(wheel==null) { hold(f,"Spinning wheel 14889 not visible at "+f.pos); return; }
                if(wheel.click("Spin")) set("WOOL_OPEN_WHEEL",f,10000,0,WOOL_WHEEL);
                else hold(f,"Spin action rejected at wheel 14889");
                return;
            }
            boolean clicked=Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>)() -> {
                Widget product=findProduct(Microbot.getClient());
                return product!=null&&!product.isHidden()&&product.getBounds()!=null
                    &&Rs2Widget.clickWidget(product);
            });
            if(clicked) set("WOOL_SPIN",f,15000,WOOL,null);
            else hold(f,"Visible spinning product 1759 not clickable");
            return;
        }

        geStage="WOOL_GATHER";
        if(Rs2Inventory.emptySlotCount()<=0) {
            hold(f,"No free inventory slot for quest wool; preserving all current items"); return;
        }
        if(f.count(SHEARS)==0) {
            if(f.pos==null||f.pos.distanceTo(FRED_POS)>4) {
                walk(f,FRED_POS,"WOOL_TO_FRED_SHEARS"); return;
            }
            if(!Rs2GroundItem.exists(SHEARS,8)) {
                hold(f,"Shears 1735 absent from inventory and Fred house ground scan"); return;
            }
            if(Rs2GroundItem.take(SHEARS)) set("WOOL_GET_SHEARS",f,10000,SHEARS,FRED_POS);
            else hold(f,"Taking ground shears 1735 rejected");
            return;
        }
        if(f.pos==null) { hold(f,"Missing player position while gathering wool"); return; }
        if(f.pos.getPlane()==1) {
            descendForWool(f,"returning to sheep field");
            return;
        }
        if(f.pos.getPlane()!=0) {
            hold(f,"Unexpected plane while gathering wool: "+f.pos); return;
        }
        if(f.pos.distanceTo(SHEEP_FIELD)>3) {
            walk(f,SHEEP_FIELD,"WOOL_TO_SHEEP_FIELD"); return;
        }
        Rs2NpcModel sheep=Microbot.getRs2NpcCache().query().withIds(SHEEP_IDS)
            .within(SHEEP_FIELD,14).where(n->canShear(n)&&!failedWoolSheep.contains(sheepKey(n)))
            .nearestReachable();
        if(sheep==null) {
            if(woolNoSheepSince==0) woolNoSheepSince=now;
            if(now-woolNoSheepSince>75000) hold(f,"No reachable untried sheep with Shear action near "+SHEEP_FIELD);
            else { phase="WAIT_SHEEP_RESPAWN"; status(f); }
            return;
        }
        woolNoSheepSince=0; woolShearTarget=sheepKey(sheep);
        if(sheep.click("Shear")) set("WOOL_SHEAR",f,12000,RAW_WOOL,sheep.getWorldLocation());
        else hold(f,"Shear click rejected for live sheep "+woolShearTarget);
    }

    private static String sheepKey(Rs2NpcModel sheep) {
        return sheep.getId()+":"+sheep.getIndex();
    }
    private static boolean canShear(Rs2NpcModel sheep) {
        if(sheep==null||sheep.getNpc()==null) return false;
        NPCComposition composition=sheep.getNpc().getComposition();
        if(composition==null||composition.getActions()==null) return false;
        for(String action:composition.getActions()) if("Shear".equalsIgnoreCase(action)) return true;
        return false;
    }
    private static Widget findProduct(Client c) {
        if(c==null) return null;
        Widget product=findProduct(c.getWidget(270,14),0);
        return product!=null?product:findProduct(c.getWidget(300,16),0);
    }
    private static Widget findProduct(Widget widget,int depth) {
        if(widget==null||widget.isHidden()||depth>8) return null;
        if(widget.getItemId()==WOOL) return widget;
        Widget[] children=widget.getChildren();
        if(children!=null) for(Widget child:children) { Widget found=findProduct(child,depth+1); if(found!=null) return found; }
        Widget[] dynamic=widget.getDynamicChildren();
        if(dynamic!=null) for(Widget child:dynamic) { Widget found=findProduct(child,depth+1); if(found!=null) return found; }
        Widget[] statics=widget.getStaticChildren();
        if(statics!=null) for(Widget child:statics) { Widget found=findProduct(child,depth+1); if(found!=null) return found; }
        return null;
    }

    private void geTick(Frame f) {
        int id=sourceItem;
        long now=System.currentTimeMillis();
        if(now-sourceStartedAt>360000) {
            hold(f,"GE source exceeded six minutes id="+id+" stage="+geStage); return;
        }
        if("CANCELLED".equals(geStage)) {
            hold(f,"GE offer cancelled after no fill; inspect returned coins/items id="+id+
                " inventory="+f.count(id)+" coins="+f.count(COINS)); return;
        }
        if("DONE".equals(geStage)) {
            if(Rs2GrandExchange.isOpen()) {
                Rs2GrandExchange.closeExchange(); set("GE_CLOSE",f,6000,id,null); return;
            }
            sourceItem=sourceGoal=0; geStage="";
            phase="GE_SOURCE_COMPLETE_"+id; status(f); return;
        }
        if("PREPARE".equals(geStage)) {
            if(geQuote==0) {
                // The quote is a spending ceiling candidate, not proof of private-server stock.
                geQuote=Rs2GrandExchange.getPrice(id);
                int remaining=sourceGoal-f.count(id);
                if(geQuote<=0||remaining<=0||
                    (long)geQuote*remaining>1000-geReservedTotal) {
                    hold(f,"GE quote unavailable/above 1000gp cumulative cap id="+id+
                        " quote="+geQuote+" deficit="+remaining+
                        " reserved="+geReservedTotal); return;
                }
                geQuantity=remaining; geInitialItem=f.count(id);
                phase="GE_QUOTE_BOUNDED_"+id; status(f); return;
            }
            int reserve=geQuote*geQuantity;
            if(f.bank) {
                if(f.count(COINS)<reserve) {
                    int shortfall=reserve-f.count(COINS);
                    if(!Rs2Bank.hasBankItem(COINS,shortfall)) {
                        hold(f,"GE coin cap not funded id="+id+" need="+reserve+
                            " inventory="+f.count(COINS)+" bankShort="+shortfall); return;
                    }
                    if(!Rs2Bank.hasWithdrawAsItem()) {
                        if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                        else hold(f,"GE coin withdraw mode rejected");
                    } else if(Rs2Bank.withdrawDeficit(COINS,reserve))
                        set("WITHDRAW",f,7000,COINS,null);
                    else hold(f,"GE coin withdrawal rejected");
                    return;
                }
                closeIfOpen(f); return;
            }
            if(f.count(COINS)<reserve) {
                hold(f,"GE reserve vanished before offer id="+id+" need="+reserve); return;
            }
            WorldPoint exchange=BankLocation.GRAND_EXCHANGE.getWorldPoint();
            if(f.pos==null||f.pos.distanceTo(exchange)>8) {
                walk(f,exchange,"TO_GE_"+id); return;
            }
            if(!Rs2GrandExchange.isOpen()) {
                if(Rs2GrandExchange.openExchange()) set("GE_OPEN",f,8000,id,null);
                else hold(f,"GE runtime clerk/widget unavailable id="+id);
                return;
            }
            if(!Rs2GrandExchange.isAllSlotsEmpty()||Rs2GrandExchange.getAvailableSlot()==null) {
                hold(f,"GE has existing offers or no free slot; refusing to touch user offers"); return;
            }
            geInitialCoins=f.count(COINS);
            geOfferAt=now;
            if(Rs2GrandExchange.buyItem(geName(id),geQuote,geQuantity))
                set("GE_PLACE",f,15000,id,null);
            else hold(f,"GE bounded offer rejected id="+id+" quote="+geQuote+
                " qty="+geQuantity+"; no second offer attempted");
            return;
        }
        if("WAIT_FILL".equals(geStage)) {
            GrandExchangeOfferDetails offer=Rs2GrandExchange.hasBuyOffer(id);
            if(offer==null||offer.getPrice()>geQuote||offer.getTotalQuantity()!=geQuantity) {
                hold(f,"GE offer state/price mismatch id="+id+" offer="+offer); return;
            }
            if(offer.getState()==net.runelite.api.GrandExchangeOfferState.BOUGHT
                ||offer.getQuantitySold()>=geQuantity) {
                if(!Rs2GrandExchange.isOpen()) {
                    if(Rs2GrandExchange.openExchange()) set("GE_OPEN",f,8000,id,null);
                    else hold(f,"GE completed offer cannot reopen for collection");
                    return;
                }
                if(Rs2GrandExchange.collectOffer(offer.getSlot(),false))
                    set("GE_COLLECT",f,10000,id,null);
                else hold(f,"GE completed offer collect rejected slot="+offer.getSlot());
                return;
            }
            if(now-geOfferAt<45000) {
                phase="GE_WAIT_FILL_"+id; status(f); return;
            }
            if(!Rs2GrandExchange.isOpen()) {
                if(Rs2GrandExchange.openExchange()) set("GE_OPEN",f,8000,id,null);
                else hold(f,"GE timed-out offer cannot reopen for cancellation");
                return;
            }
            // cancelSpecificOffers collects all slots. The strict single-offer guard
            // prevents touching unrelated offers.
            long occupied=java.util.Arrays.stream(
                net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeSlots.values())
                .filter(slot->Rs2GrandExchange.getOfferDetails(slot)!=null).count();
            if(occupied!=1||!offer.getSlot().name().equals(geOfferSlotName)) {
                hold(f,"GE timeout; other active offer appeared, manual slot review required"); return;
            }
            if(!Rs2GrandExchange.cancelSpecificOffers(java.util.List.of(offer.getSlot()),false).isEmpty())
                set("GE_CANCEL",f,10000,id,null);
            else hold(f,"GE timed-out offer cancellation rejected slot="+offer.getSlot());
            return;
        }
        hold(f,"Unknown GE source stage="+geStage+" id="+id);
    }
    private void freeAli(Frame f) {
        for(int id:new int[]{BLONDE_WIG,PASTE,SKIRT,BRONZE_KEY}) if(!need(f,id,1)) return;
        if(closeIfOpen(f)) return;
        if(f.pos!=null&&f.pos.getX()>=3121&&f.pos.getX()<=3125
            &&f.pos.getY()>=3240&&f.pos.getY()<=3243) {
            talk(f,ALI,CELL_POS,"FREE_ALI"); return;
        }
        if(f.pos==null||f.pos.distanceTo(CELL_DOOR_POS)>8) {
            walk(f,CELL_DOOR_POS,"TO_CELL_DOOR"); return;
        }
        if(Rs2Inventory.useItemOnObject(BRONZE_KEY,CELL_DOOR))
            set("UNLOCK_CELL",f,10000,BRONZE_KEY,CELL_POS);
        else hold(f,"Bronze key on verified cell door 2881 rejected");
    }
    private void useRopeOnKeli(Frame f) {
        if(!need(f,ROPE,1)) return;
        if(closeIfOpen(f)) return;
        if(f.pos==null||f.pos.distanceTo(KELI_POS)>8) { walk(f,KELI_POS,"TO_KELI"); return; }
        if(Rs2Npc.getNpc(KELI)==null) { hold(f,"Lady Keli not visible at jail"); return; }
        if(Rs2Inventory.useItemOnNpc(ROPE,KELI)) set("TIE_KELI",f,12000,ROPE,null);
        else hold(f,"Rope on Lady Keli rejected");
    }
    private void talk(Frame f,int npc,WorldPoint target,String action) {
        if(closeIfOpen(f)) return;
        if(f.pos==null||f.pos.distanceTo(target)>8) { walk(f,target,"TO_"+action); return; }
        if(Rs2Npc.getNpc(npc)==null) { hold(f,"NPC "+npc+" not visible near "+target); return; }
        int attempts=talkAttempts.getOrDefault(action,0);
        if(attempts>=3) { hold(f,"Three "+action+" conversations without material/varp progress"); return; }
        talkAttempts.put(action,attempts+1);
        if("GIVE_PRINT_OSMAN".equals(action)) keyHandinPending=true;
        if(Rs2Npc.interact(npc,"Talk-to")) set(action,f,11000,0,null);
        else hold(f,"NPC "+npc+" Talk-to rejected at "+f.pos);
    }
    private boolean closeIfOpen(Frame f) {
        if(!f.bank) return false;
        if(Rs2Bank.closeBank()) set("CLOSE_BANK",f,7000,0,null);
        else hold(f,"Bank close rejected");
        return true;
    }
    private void walk(Frame f,WorldPoint target,String action) {
        if(!action.equals(lastRoute)) { routeFailures=0; lastRoute=action; }
        if(routeFailures>=2) {
            // A nearby closed gate/door is a route barrier, never proof of arrival.
            net.runelite.api.TileObject door=Rs2GameObject.getAll(o->o!=null,f.pos,5).stream()
                .filter(o->Rs2GameObject.hasAction(o,"Open"))
                .filter(o->{ String name=Rs2GameObject.getCompositionName(o).orElse("").toLowerCase();
                    return name.contains("door")||name.contains("gate"); })
                .findFirst().orElse(null);
            if(door==null) { hold(f,"Walker stalled twice; no nearby verified door/gate; target="+target); return; }
            if(Rs2GameObject.interact(door,"Open")) set("OPEN_ROUTE_DOOR",f,8000,door.getId(),door.getWorldLocation());
            else hold(f,"Nearby route door Open rejected");
            routeFailures=0; return;
        }
        // The installed blocking API returns true only for ARRIVED. False can be EXIT
        // after the player already reached its 10-tile arrival radius, so sample the
        // post-call cached tile before classifying it as a genuine route failure.
        boolean arrived=Rs2Walker.walkTo(target);
        WorldPoint postWalk=Rs2Player.getWorldLocation();
        if(!arrived&&postWalk!=null&&postWalk.distanceTo(target)>10) {
            f.pos=postWalk;
            hold(f,"Walker exited outside its arrival radius target="+target+" post="+postWalk);
            return;
        }
        if(postWalk!=null) f.pos=postWalk;
        LOG.info("[PrinceAliRescue] WALK_RETURN action={} target={} arrived={} post={} radius=10",
            action,target,arrived,postWalk);
        set("WALK_"+action,f,20000,0,target);
    }
    private boolean recoverObservedWoolSpinAfterReload(Frame f) {
        if(!"HOLD_RELOAD_IN_FLIGHT".equals(phase)
            ||!"WOOL_OPEN_WHEEL".equals(restoredInFlightAction)
            ||!lastReloadHoldError.startsWith("Unproved WOOL_OPEN_WHEEL;")
            ||sourceItem!=WOOL||f.game!=GameState.LOGGED_IN||f.varp!=20
            ||f.count(WOOL)<1||f.count(RAW_WOOL)!=0||f.pos==null||f.pos.getPlane()!=1
            ||f.pos.distanceTo(WOOL_WHEEL)>6) return false;
        held=false; error=""; phase="RECOVER_WOOL_SPIN_FROM_INVENTORY";
        woolSpinning=true; woolProgressAt=System.currentTimeMillis();
        lastRawWool=f.count(RAW_WOOL); lastWoolBalls=f.count(WOOL);
        sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0;
        LOG.info("[PrinceAliRescue] RECOVERED_WOOL_SPIN_HOLD from exact wheel-open reload; rawWool={} balls={} pos={}",
            f.count(RAW_WOOL),f.count(WOOL),f.pos);
        restoredInFlightAction=""; lastReloadHoldError="";
        return true;
    }
    private boolean recoverObservedWoolDescentHold(Frame f) {
        if(!"HOLD".equals(phase)
            ||!error.startsWith("Unproved WOOL_CLIMB_DOWN;")
            ||sourceItem!=WOOL||sourceGoal!=3||f.game!=GameState.LOGGED_IN||f.varp!=20
            ||f.count(WOOL)<1||f.count(WOOL)>=sourceGoal||f.count(RAW_WOOL)!=0
            ||f.count(SHEARS)==0||f.pos==null||f.pos.getPlane()!=1
            ||f.pos.distanceTo(WOOL_WHEEL)>10) return false;
        held=false; error=""; phase="RESUME_WOOL_DESCENT_APPROACH";
        sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0; woolSpinning=false;
        LOG.info("[PrinceAliRescue] RECOVERED_WOOL_DESCENT_HOLD balls={} goal={} rawWool={} pos={}; will approach live stairs before another click",
            f.count(WOOL),sourceGoal,f.count(RAW_WOOL),f.pos);
        return true;
    }
    private boolean recoverObservedWoolGatherPlaneHold(Frame f) {
        if(!"HOLD".equals(phase)
            ||!error.equals("Unexpected position/plane while gathering wool: "+f.pos)
            ||sourceItem!=WOOL||f.game!=GameState.LOGGED_IN||f.varp!=20
            ||f.count(WOOL)>=sourceGoal||f.count(RAW_WOOL)!=0||f.pos==null
            ||f.pos.getPlane()!=1||f.pos.distanceTo(WOOL_WHEEL)>8) return false;
        held=false; error=""; phase="RESUME_WOOL_GATHER_BY_DESCENT";
        sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0;
        LOG.info("[PrinceAliRescue] RECOVERED_WOOL_GATHER_PLANE_HOLD balls={} goal={} rawWool={} pos={}",
            f.count(WOOL),sourceGoal,f.count(RAW_WOOL),f.pos);
        return true;
    }
    private boolean recoverObservedWoolStairRouteHold(Frame f) {
        boolean exactRouteHold=error.equals("Walker rejected route to "+CASTLE_STAIRS_GROUND);
        boolean expiredTimerHold=error.equals("Local wool source exceeded six minutes; balls=0 rawWool=1");
        if(!"HOLD".equals(phase)
            ||(!exactRouteHold&&!expiredTimerHold)
            ||sourceItem!=WOOL||f.game!=GameState.LOGGED_IN||f.varp!=20
            ||f.count(RAW_WOOL)<=0||f.pos==null||f.pos.getPlane()!=0
            ||f.pos.distanceTo(CASTLE_STAIRS_GROUND)>8) return false;
        held=false; error=""; phase="RESUME_WOOL_AT_OBSERVED_STAIRS";
        routeFailures=0; lastRoute="";
        sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0;
        LOG.info("[PrinceAliRescue] RECOVERED_WOOL_STAIRS_HOLD cause={} freshPos={} rawWool={} target={}",
            exactRouteHold?"FALSE_WALK_RESULT":"EXPIRED_TIMER_AFTER_ROUTE_RECOVERY",
            f.pos,f.count(RAW_WOOL),CASTLE_STAIRS_GROUND);
        return true;
    }
    private void dialogueOption(Frame f) {
        String[] expected={"Is there anything I can help you with?","Yes.",
            "No. I think I know everything I need to.",
            "Could you make other things apart from rope?","How about some sort of wig?",
            "I have them here. Please make me a wig.","Can you make skin paste?",
            "Yes please. Mix me some skin paste.","Heard of you? You're famous in Gielinor!",
            "What's your latest plan then?","How do you know someone won't try to free him?",
            "Could I see the key please?","Could I touch the key for a moment please?",
            "I have some beer here. Fancy one?"};
        for(String choice:expected) if(f.options.contains(choice)) {
            if(Rs2Dialogue.clickOption(choice)) set("OPTION",f,7000,0,null);
            else hold(f,"Known option click rejected: "+choice);
            return;
        }
        hold(f,"Unrecognized Prince Ali dialogue options: "+f.options);
    }
    private void set(String action,Frame f,long timeout,int item,WorldPoint target) {
        pending=new Pending(action,f,timeout,item,target); phase=action; status(f);
    }
    private void descendForWool(Frame f,String reason) {
        Rs2TileObjectModel stairs=object(16672,CASTLE_STAIRS_FIRST,8);
        if(stairs==null) {
            hold(f,"Castle downstairs stair 16672 not visible "+reason+" at "+f.pos); return;
        }
        WorldPoint tile=stairs.getWorldLocation();
        net.runelite.api.ObjectComposition composition=stairs.getObjectComposition();
        String actions=composition==null?"[]":java.util.Arrays.toString(composition.getActions());
        if(tile==null||!Rs2GameObject.hasAction(stairs,"Climb-down")) {
            hold(f,"Live castle stair lacks Climb-down id="+stairs.getId()+" name="+stairs.getName()
                +" tile="+tile+" actions="+actions+" player="+f.pos); return;
        }
        int distance=f.pos.distanceTo(tile);
        if(distance>2) {
            LOG.info("[PrinceAliRescue] WOOL_STAIRS_APPROACH_DISPATCH id={} tile={} name={} actions={} from={} distance={} radius=2",
                stairs.getId(),tile,stairs.getName(),actions,f.pos,distance);
            boolean arrived=Rs2Walker.walkTo(tile,2);
            WorldPoint post=Rs2Player.getWorldLocation();
            if(post!=null) f.pos=post;
            if(post==null||post.distanceTo(tile)>2) {
                hold(f,"Walker exited before reaching live castle stair; id="+stairs.getId()+" tile="+tile
                    +" arrived="+arrived+" post="+post+" actions="+actions); return;
            }
            LOG.info("[PrinceAliRescue] WOOL_STAIRS_APPROACH_RETURN id={} tile={} arrived={} post={} radius=2",
                stairs.getId(),tile,arrived,post);
            set("WOOL_APPROACH_DOWNSTAIRS",f,15000,0,tile);
            return;
        }
        LOG.info("[PrinceAliRescue] WOOL_CLIMB_DOWN_DISPATCH id={} tile={} name={} actions={} reachable={} player={} distance={}",
            stairs.getId(),tile,stairs.getName(),actions,stairs.isReachable(),f.pos,distance);
        if(stairs.click("Climb-down")) set("WOOL_CLIMB_DOWN",f,12000,0,tile);
        else hold(f,"Climb-down rejected at live castle stair id="+stairs.getId()+" tile="+tile
            +" name="+stairs.getName()+" actions="+actions+" reachable="+stairs.isReachable());
    }
    private boolean proved(Pending p,Frame f) {
        if(f.quest==QuestState.FINISHED) return true;
        if("WOOL_GET_SHEARS".equals(p.action)) return f.count(SHEARS)>p.before.count(SHEARS);
        if("WOOL_SHEAR".equals(p.action)) return f.count(RAW_WOOL)>p.before.count(RAW_WOOL);
        if("WOOL_CLIMB_UP".equals(p.action)) return f.pos!=null&&f.pos.getPlane()==1;
        if("WOOL_CLIMB_DOWN".equals(p.action)) return f.pos!=null&&f.pos.getPlane()==0;
        if("WOOL_APPROACH_DOWNSTAIRS".equals(p.action)) return f.pos!=null&&p.target!=null
            &&f.pos.getPlane()==1&&f.pos.distanceTo(p.target)<=2;
        if("WOOL_OPEN_WHEEL".equals(p.action)) return f.production;
        if("WOOL_SPIN".equals(p.action)) return f.count(WOOL)>p.before.count(WOOL)
            &&f.count(RAW_WOOL)<p.before.count(RAW_WOOL);
        if("OPEN_BANK".equals(p.action)) return f.bank;
        if("CLOSE_BANK".equals(p.action)) return !f.bank;
        if("WITHDRAW_MODE".equals(p.action)) return Rs2Bank.hasWithdrawAsItem();
        if("WITHDRAW".equals(p.action)) return f.count(p.item)>p.before.count(p.item);
        if("CRAFT_SOFT_CLAY".equals(p.action)) return f.count(SOFT_CLAY)>p.before.count(SOFT_CLAY)
            &&f.count(CLAY)<p.before.count(CLAY)
            &&f.count(WATER)<p.before.count(WATER);
        if("CRAFT_YELLOW_DYE".equals(p.action)) return f.count(DYE)>p.before.count(DYE)
            &&f.count(ONION)<p.before.count(ONION)
            &&f.count(COINS)<p.before.count(COINS)
            &&p.before.count(COINS)-f.count(COINS)<=5;
        if("SOURCE_SHOP_OPEN".equals(p.action)) return f.shop;
        if("SOURCE_SHOP_CLOSE".equals(p.action)) return !f.shop;
        if("SOURCE_BUY".equals(p.action)) return f.count(p.item)>p.before.count(p.item)
            &&f.count(COINS)<p.before.count(COINS);
        if("GE_OPEN".equals(p.action)) return Rs2GrandExchange.isOpen();
        if("GE_CLOSE".equals(p.action)) return !Rs2GrandExchange.isOpen();
        if("GE_PLACE".equals(p.action)) {
            GrandExchangeOfferDetails offer=Rs2GrandExchange.hasBuyOffer(p.item);
            int debit=p.before.count(COINS)-f.count(COINS);
            return offer!=null&&offer.getPrice()>0&&offer.getPrice()<=geQuote
                &&offer.getTotalQuantity()==geQuantity&&debit>0
                &&debit<=1000-geReservedTotal;
        }
        if("GE_COLLECT".equals(p.action)) return f.count(p.item)>=geInitialItem+geQuantity
            &&geInitialCoins>f.count(COINS)
            &&geInitialCoins-f.count(COINS)<=1000;
        if("GE_CANCEL".equals(p.action)) {
            if(geOfferSlotName.isEmpty()) return false;
            GrandExchangeOfferDetails offer=Rs2GrandExchange.getOfferDetails(
                net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeSlots.valueOf(
                    geOfferSlotName));
            return offer==null||offer.getState()==net.runelite.api.GrandExchangeOfferState.CANCELLED_BUY;
        }
        if(p.action.startsWith("SOURCE_BEER_")) return dialogueChanged(p.before,f)
            ||f.count(BEER)>p.before.count(BEER);
        if("SOURCE_CONTINUE".equals(p.action)) return dialogueChanged(p.before,f)
            ||f.count(BEER)>p.before.count(BEER);
        if("DEPOSIT_UNNEEDED".equals(p.action)) return f.count(p.item)<p.before.count(p.item)
            && f.bank;
        if("DYE_WIG".equals(p.action)) return f.count(BLONDE_WIG)>p.before.count(BLONDE_WIG);
        if("OPEN_ROUTE_DOOR".equals(p.action)) {
            net.runelite.api.TileObject door=Rs2GameObject.findObjectByLocation(p.target);
            return door==null||!Rs2GameObject.hasAction(door,"Open");
        }
        if(p.action.startsWith("WALK_")) return f.pos!=null&&p.before.pos!=null
            &&(!f.pos.equals(p.before.pos)||f.pos.distanceTo(p.target)<=10);
        if("UNLOCK_CELL".equals(p.action)) return f.pos!=null&&f.pos.getX()>=3121
            &&f.pos.getX()<=3125&&f.pos.getY()>=3240&&f.pos.getY()<=3243;
        if("TIE_KELI".equals(p.action)) return f.varp>p.before.varp;
        if("GIVE_PRINT_OSMAN".equals(p.action))
            return f.count(KEY_PRINT)<p.before.count(KEY_PRINT)||dialogueChanged(p.before,f);
        if("MAKE_WIG".equals(p.action)) return f.count(WIG)>p.before.count(WIG)||dialogueChanged(p.before,f);
        if("MAKE_PASTE".equals(p.action)) return f.count(PASTE)>p.before.count(PASTE)||dialogueChanged(p.before,f);
        if("GET_KEY_PRINT".equals(p.action)) return f.count(KEY_PRINT)>p.before.count(KEY_PRINT)||dialogueChanged(p.before,f);
        if("GET_KEY_LEELA".equals(p.action)) return f.count(BRONZE_KEY)>p.before.count(BRONZE_KEY)||f.varp>p.before.varp||dialogueChanged(p.before,f);
        if("GIVE_BEER_JOE".equals(p.action)) return f.varp>p.before.varp||f.count(BEER)<p.before.count(BEER)||dialogueChanged(p.before,f);
        if("FREE_ALI".equals(p.action)) return f.varp>=100||dialogueChanged(p.before,f);
        return f.varp!=p.before.varp||dialogueChanged(p.before,f);
    }
    private static boolean dialogueChanged(Frame a,Frame b) {
        return !a.dialogue.equals(b.dialogue)||!a.options.equals(b.options)
            ||a.continuePrompt!=b.continuePrompt;
    }
    private static Rs2TileObjectModel object(int id,WorldPoint point,int radius) {
        return Microbot.getRs2TileObjectCache().query().withId(id).within(point,radius).nearestOnClientThread();
    }
    private void hold(Frame f,String reason) {
        held=true; phase="HOLD"; error=reason;
        LOG.warn("[PrinceAliRescue] HOLD {} varp={} pos={}",reason,f==null?-1:f.varp,f==null?null:f.pos);
        status(f);
    }
    private void status(Frame f) {
        try {
            Files.createDirectories(STATUS.getParent());
            Properties p=new Properties();
            p.setProperty("timestamp",Long.toString(System.currentTimeMillis()));
            p.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
            p.setProperty("build",Integer.toString(BUILD_NUMBER));
            p.setProperty("phase",phase); p.setProperty("error",error);
            p.setProperty("held",Boolean.toString(held));
            p.setProperty("pending",pending==null?"":pending.action);
            p.setProperty("quest",f==null||f.quest==null?"UNKNOWN":f.quest.name());
            p.setProperty("varp273",Integer.toString(f==null?-1:f.varp));
            p.setProperty("position",f==null||f.pos==null?"UNKNOWN":f.pos.toString());
            p.setProperty("game",f==null||f.game==null?"UNKNOWN":f.game.name());
            p.setProperty("gameState",f==null||f.game==null?"UNKNOWN":f.game.name());
            p.setProperty("world",Integer.toString(f==null?-1:f.world));
            p.setProperty("items",f==null?"{}":f.items.toString());
            p.setProperty("bankInspected",Boolean.toString(bankInspected));
            p.setProperty("keySubmitted",Boolean.toString(keySubmitted));
            p.setProperty("sourceItem",Integer.toString(sourceItem));
            p.setProperty("sourceGoal",Integer.toString(sourceGoal));
            p.setProperty("sourceAttempts",Integer.toString(sourceAttempts));
            p.setProperty("sourceSpent",Integer.toString(sourceSpent));
            p.setProperty("woolStage",sourceItem==WOOL?geStage:"");
            p.setProperty("rawWool",Integer.toString(f==null?0:f.count(RAW_WOOL)));
            p.setProperty("ballOfWool",Integer.toString(f==null?0:f.count(WOOL)));
            p.setProperty("shears",Integer.toString(f==null?0:f.count(SHEARS)));
            p.setProperty("woolTarget",woolShearTarget);
            p.setProperty("woolShearFailures",Integer.toString(woolShearFailures));
            p.setProperty("woolSpinning",Boolean.toString(woolSpinning));
            p.setProperty("production",Boolean.toString(f!=null&&f.production));
            Path temp=STATUS.resolveSibling("status.tmp");
            try(OutputStream out=Files.newOutputStream(temp)) { p.store(out,"Prince Ali Rescue runtime"); }
            Files.move(temp,STATUS,StandardCopyOption.REPLACE_EXISTING);
        } catch(Exception e) { LOG.warn("Prince Ali status write: {}",e.toString()); }
    }
}
