package net.runelite.client.plugins.microbot.princealirescue;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Locale;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
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
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
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
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.death.Rs2Death;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** QuestHelper stage route, with one dispatched action and a later observation as proof. */
public final class PrinceAliRescueScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(PrinceAliRescueScript.class);
    public static final int BUILD_NUMBER = 71;
    private static final int SHANTAY_BAR_COIN_CAP=50;
    private static final boolean FREE_TO_PLAY_ACCOUNT=true;
    private static final int VARP = 273;
    private static final int SOFT_CLAY=1761, CLAY=434, WOOL=1759, RAW_WOOL=1737, SHEARS=1735,
        DYE=1765, ONION=1957, TINDERBOX=590, LOGS=1511, BRONZE_AXE=1351,
        REDBERRIES=1951,
        ASHES=592, WATER=1929, EMPTY_BUCKET=1925, FLOUR=1933, BRONZE_BAR=2349, SKIRT=1013,
        BEER=1917, ROPE=954, COINS=995, WIG=2421, BLONDE_WIG=2419,
        PASTE=2424, KEY_PRINT=2423, BRONZE_KEY=2418;
    private static final int HASSAN=4285, OSMAN=4286, NED=4280, AGGIE=120,
        KELI=11578, LEELA=4274, JOE=11577, ALI=11579, CELL_DOOR=2881;
    private static final int[] SHEEP_IDS={2786,2699,2787,2693,2694,2695};
    private static final int[] ONION_IDS={3366,5538};
    private static final int[] WOODCUTTING_AXES={1351,1349,1353,1361,1355,1357,1359,6739};
    private static final int BRONZE_AXE_LOGS_OBJECT=5581;
    private static final WorldPoint HASSAN_POS=new WorldPoint(3298,3163,0),
        OSMAN_POS=new WorldPoint(3286,3180,0), NED_POS=new WorldPoint(3097,3257,0),
        AGGIE_POS=new WorldPoint(3086,3257,0), KELI_POS=new WorldPoint(3127,3244,0),
        LEELA_POS=new WorldPoint(3113,3262,0), JOE_POS=new WorldPoint(3124,3245,0),
        CELL_POS=new WorldPoint(3123,3240,0), CELL_DOOR_POS=new WorldPoint(3123,3243,0),
        THESSALIA_POS=new WorldPoint(3206,3415,0),
        BLUE_MOON_POS=new WorldPoint(3228,3393,0),
        SHANTAY_SHOP_POS=new WorldPoint(3304,3124,0),
        WYDIN_POS=new WorldPoint(3013,3204,0),
        FRED_POS=new WorldPoint(3190,3273,0), FRED_ONION_FIELD=new WorldPoint(3190,3263,0),
        SHEEP_FIELD=new WorldPoint(3201,3268,0), ASHES_FIRE_FIELD=new WorldPoint(3201,3268,0),
        ALKHARID_GENERAL_STORE=new WorldPoint(3315,3175,0),
        ALKHARID_PALACE_COURTYARD=new WorldPoint(3293,3171,0),
        LUMBRIDGE_STORE=new WorldPoint(3212,3246,0),
        CASTLE_STAIRS_GROUND=new WorldPoint(3204,3207,0),
        CASTLE_STAIRS_FIRST=new WorldPoint(3204,3207,1),
        WOOL_WHEEL=new WorldPoint(3209,3212,1), WOOL_WHEEL_ROOM=new WorldPoint(3210,3212,1);
    private static final WorldPoint RIMMINGTON_CLAY_MINE=new WorldPoint(2985,3238,0);
    private static final int[] CLAY_ROCK_IDS={ObjectID.CLAYROCK1,ObjectID.CLAYROCK2};
    private static final Path STATUS=Paths.get(System.getProperty("user.home"),
        ".runelite","princealirescue","status.properties");

    private static final class Frame {
        GameState game; QuestState quest; WorldPoint pos; int varp, loginIndex, world, canvasWidth;
        double health; int hp,maxHp,combatLevel; boolean inCombat;
        boolean bank, shop, continuePrompt, inDialogue, production; int animation=-1;
        boolean widgetSelected, shopWidgetPresent, shopWidgetHidden;
        boolean memberPromoVisible;
        Widget memberPromoCloseWidget;
        String memberPromoDiagnostic="";
        int selectedWidgetId=-1, selectedWidgetParentId=-1, selectedWidgetItemId=-1;
        String selectedWidgetName="", selectedWidgetText="";
        String shantayPathDiagnostic="";
        int shantayNpcId=-1, shantayNpcAreaDistance=Integer.MAX_VALUE;
        WorldPoint shantayNpcTile;
        boolean shantayNpcTradeAction, shantayNpcLos;
        String shantayNpcBaseActions="[]";
        String dialogue="", options="", question="";
        final Map<Integer,Integer> items=new HashMap<>();
        final Map<Integer,Integer> bankItems=new HashMap<>();
        boolean bankContentsAvailable;
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
    private BronzeBarSource bronzeSource;
    private String lastBronzeSourceDetail="";
    private long bankEvidenceWaitAt;
    private boolean bankCleanupRequired=true;
    private String graveRecoveryStage="";
    private WorldPoint graveRecoveryTarget;
    private long graveRecoveryDeadline;
    private int graveRecoveryRuns;
    private String graveRecoveryAfterEscapeError="";
    private Map<Integer,Integer> graveRecoveryExpected=new HashMap<>();
    private int loginAttempts, welcomeAttempts, disconnectAttempts, selectedWorld, routeFailures;
    private long loginAt, welcomeAt, disconnectAt, lastMoveAt;
    private String lastRoute="";
    private boolean bankInspected, keySubmitted, reloadStateRestored;
    private boolean continueRetryUsed;
    private boolean shantayOpenRetryUsed;
    private boolean shantayHoldDiagnosticLogged;
    private boolean shantayPathDiagnosticLogged;
    private String shantayPathDiagnostic="";
    private boolean shantayReachableTileRecoveryUsed, shantayReachableTradeUsed;
    private boolean shantayReachableTileTimerRecoveryUsed;
    private boolean shantayPromoRecoveryAvailable, shantayPromoRetryActive, shantayPromoRetryUsed;
    private boolean memberPromoDismissAttempted, memberPromoDiagnosticLogged;
    private boolean memberPromoPriorHeld;
    private String memberPromoPriorPhase="", memberPromoPriorError="";
    private long memberPromoClickedAt;
    private WorldPoint shantayApproachTarget;
    private boolean softClayFallbackRecovered;
    private boolean softClayMineRecoveryDiagnosticLogged;
    private int softClayMineAttempts;
    private boolean keyHandinPending;
    private boolean keyFurnaceConfirmed;
    private String restoredInFlightAction="", lastReloadHoldError="";
    private String loginError="";
    private final Map<String,Integer> talkAttempts=new HashMap<>();
    private String lastMaterialSignature="";
    private int sourceItem, sourceGoal, sourceAttempts, sourceSpent, sourceLastCount, sourceLastCoins;
    private WorldPoint onionApproach, onionLastPosition;
    private long onionLastStepAt, onionLastProgressAt;
    private int onionStepAttempts;
    private boolean onionFailedAttemptRecovered, onionTimeoutRecovered,
        onionSecondPickCapRecovered, ashesFallbackRecovered, ashesTinderboxShopRecovered,
        ashesLogSourceRecovered, ashesTreeRetryRecovered, ashesTreeRerouteRecovered,
        ashesFredTreeRescanRecovered, ashesFredLineOfSightApproachRecovered,
        ashesTreeApproachFailureRecovered, ashesApproachReloadRecovered;
    private final Set<String> ashesNoLosTreeTargets=new HashSet<>();
    private WorldPoint ashesFireTile;
    private long ashesFireStartedAt;
    private int ashesFireAttempts, ashesTinderboxPurchaseAttempts;
    private int waterBucketPurchaseAttempts, waterFillAttempts, waterApproachAttempts;
    private boolean waterQuoteRecovered, waterCandidateHoldRecovered;
    private WorldPoint waterApproachTarget, waterApproachLastPosition;
    private long waterApproachStartedAt, waterApproachProgressAt;
    private WorldPoint ashesLogApproachLastPosition;
    private long ashesLogApproachStartedAt, ashesLogApproachProgressAt;
    private int ashesLogApproachAttempts;
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
    private boolean woolStageRecoveredFromStatus=false;
    private long woolProgressAt=0, woolNoSheepSince=0;
    private long woolStairLastStepAt=0, woolStairLastProgressAt=0;
    private int woolStairStepAttempts=0;
    private WorldPoint woolStairLastPosition;
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
        state.put("bankCleanupRequired",bankCleanupRequired);
        state.put("graveRecoveryStage",graveRecoveryStage);
        state.put("graveRecoveryTarget",graveRecoveryTarget);
        state.put("graveRecoveryDeadline",graveRecoveryDeadline);
        state.put("graveRecoveryRuns",graveRecoveryRuns);
        state.put("graveRecoveryAfterEscapeError",graveRecoveryAfterEscapeError);
        state.put("graveRecoveryExpected",new HashMap<>(graveRecoveryExpected));
        if(bronzeSource!=null) state.put("bronzeSourceState",bronzeSource.exportState());
        state.put("continueRetryUsed",continueRetryUsed);
        state.put("shantayOpenRetryUsed",shantayOpenRetryUsed);
        state.put("shantayHoldDiagnosticLogged",shantayHoldDiagnosticLogged);
        state.put("shantayPathDiagnosticLogged",shantayPathDiagnosticLogged);
        state.put("shantayPathDiagnostic",shantayPathDiagnostic);
        state.put("shantayReachableTileRecoveryUsed",shantayReachableTileRecoveryUsed);
        state.put("shantayReachableTradeUsed",shantayReachableTradeUsed);
        state.put("shantayReachableTileTimerRecoveryUsed",shantayReachableTileTimerRecoveryUsed);
        state.put("shantayPromoRecoveryAvailable",shantayPromoRecoveryAvailable);
        state.put("shantayPromoRetryActive",shantayPromoRetryActive);
        state.put("shantayPromoRetryUsed",shantayPromoRetryUsed);
        state.put("memberPromoDismissAttempted",memberPromoDismissAttempted);
        state.put("memberPromoDiagnosticLogged",memberPromoDiagnosticLogged);
        state.put("memberPromoPriorHeld",memberPromoPriorHeld);
        state.put("memberPromoPriorPhase",memberPromoPriorPhase);
        state.put("memberPromoPriorError",memberPromoPriorError);
        state.put("memberPromoClickedAt",memberPromoClickedAt);
        state.put("shantayApproachTarget",shantayApproachTarget);
        state.put("lastReloadHoldError",lastReloadHoldError);
        state.put("restoredInFlightAction",restoredInFlightAction);
        state.put("bankInspected",bankInspected); state.put("keySubmitted",keySubmitted);
        state.put("softClayFallbackRecovered",softClayFallbackRecovered);
        state.put("softClayMineAttempts",softClayMineAttempts);
        state.put("keyHandinPending",keyHandinPending);
        state.put("keyFurnaceConfirmed",keyFurnaceConfirmed);
        state.put("talkAttempts",new HashMap<>(talkAttempts));
        state.put("lastMaterialSignature",lastMaterialSignature);
        state.put("sourceItem",sourceItem); state.put("sourceGoal",sourceGoal);
        state.put("sourceAttempts",sourceAttempts); state.put("sourceSpent",sourceSpent);
        state.put("onionApproach",onionApproach); state.put("onionLastPosition",onionLastPosition);
        state.put("onionLastStepAt",onionLastStepAt); state.put("onionLastProgressAt",onionLastProgressAt);
        state.put("onionStepAttempts",onionStepAttempts);
        state.put("onionFailedAttemptRecovered",onionFailedAttemptRecovered);
        state.put("onionTimeoutRecovered",onionTimeoutRecovered);
        state.put("onionSecondPickCapRecovered",onionSecondPickCapRecovered);
        state.put("ashesFallbackRecovered",ashesFallbackRecovered);
        state.put("ashesTinderboxShopRecovered",ashesTinderboxShopRecovered);
        state.put("ashesLogSourceRecovered",ashesLogSourceRecovered);
        state.put("ashesTreeRetryRecovered",ashesTreeRetryRecovered);
        state.put("ashesTreeRerouteRecovered",ashesTreeRerouteRecovered);
        state.put("ashesFredTreeRescanRecovered",ashesFredTreeRescanRecovered);
        state.put("ashesFredLineOfSightApproachRecovered",ashesFredLineOfSightApproachRecovered);
        state.put("ashesTreeApproachFailureRecovered",ashesTreeApproachFailureRecovered);
        state.put("ashesApproachReloadRecovered",ashesApproachReloadRecovered);
        state.put("ashesNoLosTreeTargets",new HashSet<>(ashesNoLosTreeTargets));
        state.put("ashesFireTile",ashesFireTile); state.put("ashesFireStartedAt",ashesFireStartedAt);
        state.put("ashesFireAttempts",ashesFireAttempts);
        state.put("ashesTinderboxPurchaseAttempts",ashesTinderboxPurchaseAttempts);
        state.put("waterBucketPurchaseAttempts",waterBucketPurchaseAttempts);
        state.put("waterFillAttempts",waterFillAttempts);
        state.put("waterQuoteRecovered",waterQuoteRecovered);
        state.put("waterCandidateHoldRecovered",waterCandidateHoldRecovered);
        state.put("waterApproachAttempts",waterApproachAttempts);
        state.put("waterApproachTarget",waterApproachTarget);
        state.put("waterApproachLastPosition",waterApproachLastPosition);
        state.put("waterApproachStartedAt",waterApproachStartedAt);
        state.put("waterApproachProgressAt",waterApproachProgressAt);
        state.put("ashesLogApproachLastPosition",ashesLogApproachLastPosition);
        state.put("ashesLogApproachStartedAt",ashesLogApproachStartedAt);
        state.put("ashesLogApproachProgressAt",ashesLogApproachProgressAt);
        state.put("ashesLogApproachAttempts",ashesLogApproachAttempts);
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
        state.put("woolStageRecoveredFromStatus",woolStageRecoveredFromStatus);
        state.put("woolNoSheepSince",woolNoSheepSince);
        state.put("woolStairLastStepAt",woolStairLastStepAt);
        state.put("woolStairLastProgressAt",woolStairLastProgressAt);
        state.put("woolStairStepAttempts",woolStairStepAttempts);
        state.put("woolStairLastPosition",woolStairLastPosition);
        state.put("selectedWorld",selectedWorld); state.put("loginAttempts",loginAttempts);
        state.put("welcomeAttempts",welcomeAttempts); state.put("disconnectAttempts",disconnectAttempts);
        state.put("loginError",loginError);
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
        bankCleanupRequired=(Boolean)state.getOrDefault("bankCleanupRequired",true);
        graveRecoveryStage=(String)state.getOrDefault("graveRecoveryStage","");
        graveRecoveryTarget=(WorldPoint)state.getOrDefault("graveRecoveryTarget",null);
        graveRecoveryDeadline=(Long)state.getOrDefault("graveRecoveryDeadline",0L);
        graveRecoveryRuns=(Integer)state.getOrDefault("graveRecoveryRuns",graveRecoveryStage.isEmpty()?0:1);
        graveRecoveryAfterEscapeError=(String)state.getOrDefault("graveRecoveryAfterEscapeError","");
        if(state.get("graveRecoveryExpected") instanceof Map)
            graveRecoveryExpected=new HashMap<>((Map<Integer,Integer>)state.get("graveRecoveryExpected"));
        if(state.get("bronzeSourceState") instanceof Map) {
            bronzeSource=new BronzeBarSource(5);
            bronzeSource.restoreState((Map<String,Object>)state.get("bronzeSourceState"));
        }
        continueRetryUsed=(Boolean)state.getOrDefault("continueRetryUsed",false);
        shantayOpenRetryUsed=(Boolean)state.getOrDefault("shantayOpenRetryUsed",false);
        shantayHoldDiagnosticLogged=(Boolean)state.getOrDefault("shantayHoldDiagnosticLogged",false);
        shantayPathDiagnosticLogged=(Boolean)state.getOrDefault("shantayPathDiagnosticLogged",false);
        shantayPathDiagnostic=(String)state.getOrDefault("shantayPathDiagnostic","");
        shantayReachableTileRecoveryUsed=(Boolean)state.getOrDefault("shantayReachableTileRecoveryUsed",false);
        shantayReachableTradeUsed=(Boolean)state.getOrDefault("shantayReachableTradeUsed",false);
        shantayReachableTileTimerRecoveryUsed=(Boolean)state.getOrDefault("shantayReachableTileTimerRecoveryUsed",false);
        shantayPromoRecoveryAvailable=(Boolean)state.getOrDefault("shantayPromoRecoveryAvailable",false);
        shantayPromoRetryActive=(Boolean)state.getOrDefault("shantayPromoRetryActive",false);
        shantayPromoRetryUsed=(Boolean)state.getOrDefault("shantayPromoRetryUsed",false);
        memberPromoDismissAttempted=(Boolean)state.getOrDefault("memberPromoDismissAttempted",false);
        memberPromoDiagnosticLogged=(Boolean)state.getOrDefault("memberPromoDiagnosticLogged",false);
        memberPromoPriorHeld=(Boolean)state.getOrDefault("memberPromoPriorHeld",false);
        memberPromoPriorPhase=(String)state.getOrDefault("memberPromoPriorPhase","");
        memberPromoPriorError=(String)state.getOrDefault("memberPromoPriorError","");
        memberPromoClickedAt=(Long)state.getOrDefault("memberPromoClickedAt",0L);
        // Build61's observed getParentId exception was before the click call.
        // Recover only that exact migration, retaining the original quest HOLD.
        if(held&&"HOLD_EXCEPTION".equals(phase)&&memberPromoDismissAttempted
            &&shantayPromoRecoveryAvailable&&"".equals(state.getOrDefault("pendingAction",""))
            &&"java.lang.IllegalStateException: must be called on client thread".equals(error)
            &&"BAR_SHANTAY_REACHABLE_TRADE".equals(state.getOrDefault("restoredInFlightAction",""))) {
            memberPromoDismissAttempted=false;
            phase="HOLD_RELOAD_IN_FLIGHT";
            error="Reload during BAR_SHANTAY_REACHABLE_TRADE; recovered confirmed pre-click popup diagnostic exception";
            LOG.info("[PrinceAliRescue] RECOVER_BUILD61_PROMO_PRE_CLICK_EXCEPTION no click was dispatched");
        }
        shantayApproachTarget=(WorldPoint)state.getOrDefault("shantayApproachTarget",null);
        lastReloadHoldError=(String)state.getOrDefault("lastReloadHoldError",error);
        restoredInFlightAction=(String)state.getOrDefault("restoredInFlightAction","");
        if(held && "HOLD".equals(phase)
            && error.startsWith("Unrecognized Prince Ali dialogue options:")
            && (error.contains("No. I think I know everything I need to.")
                ||error.contains("Do you know why they've taken the Prince?|Where abouts in Draynor is Leela?|I'll get going.|"))) {
            held=false; phase="RESUME_KNOWN_OSMAN_DIALOGUE"; error="";
            LOG.info("[PrinceAliRescue] Reload recovery: resuming only the newly recognized Osman option menu");
        }
        bankInspected=(Boolean)state.getOrDefault("bankInspected",false);
        keySubmitted=(Boolean)state.getOrDefault("keySubmitted",false);
        softClayFallbackRecovered=(Boolean)state.getOrDefault("softClayFallbackRecovered",false);
        softClayMineAttempts=(Integer)state.getOrDefault("softClayMineAttempts",0);
        keyHandinPending=(Boolean)state.getOrDefault("keyHandinPending",false);
        keyFurnaceConfirmed=(Boolean)state.getOrDefault("keyFurnaceConfirmed",false);
        if(held&&error.startsWith("Bartender dialogue lacks verified beer option: A glass of your finest ale please.|")) {
            held=false; error=""; phase="RESUME_VERIFIED_ALE_OPTION";
        }
        Object savedAttempts=state.get("talkAttempts");
        if(savedAttempts instanceof Map<?,?>) for(Map.Entry<?,?> entry:((Map<?,?>)savedAttempts).entrySet())
            if(entry.getKey() instanceof String && entry.getValue() instanceof Integer)
                talkAttempts.put((String)entry.getKey(),(Integer)entry.getValue());
        lastMaterialSignature=(String)state.getOrDefault("lastMaterialSignature","");
        sourceItem=(Integer)state.getOrDefault("sourceItem",0);
        sourceGoal=(Integer)state.getOrDefault("sourceGoal",0);
        sourceAttempts=(Integer)state.getOrDefault("sourceAttempts",0);
        Object savedOnionApproach=state.get("onionApproach");
        if(savedOnionApproach instanceof WorldPoint) onionApproach=(WorldPoint)savedOnionApproach;
        Object savedOnionPosition=state.get("onionLastPosition");
        if(savedOnionPosition instanceof WorldPoint) onionLastPosition=(WorldPoint)savedOnionPosition;
        onionLastStepAt=(Long)state.getOrDefault("onionLastStepAt",0L);
        onionLastProgressAt=(Long)state.getOrDefault("onionLastProgressAt",0L);
        onionStepAttempts=(Integer)state.getOrDefault("onionStepAttempts",0);
        onionFailedAttemptRecovered=(Boolean)state.getOrDefault("onionFailedAttemptRecovered",false);
        onionTimeoutRecovered=(Boolean)state.getOrDefault("onionTimeoutRecovered",false);
        onionSecondPickCapRecovered=(Boolean)state.getOrDefault("onionSecondPickCapRecovered",false);
        ashesFallbackRecovered=(Boolean)state.getOrDefault("ashesFallbackRecovered",false);
        ashesTinderboxShopRecovered=(Boolean)state.getOrDefault("ashesTinderboxShopRecovered",false);
        ashesLogSourceRecovered=(Boolean)state.getOrDefault("ashesLogSourceRecovered",false);
        ashesTreeRetryRecovered=(Boolean)state.getOrDefault("ashesTreeRetryRecovered",false);
        ashesTreeRerouteRecovered=(Boolean)state.getOrDefault("ashesTreeRerouteRecovered",false);
        ashesFredTreeRescanRecovered=(Boolean)state.getOrDefault("ashesFredTreeRescanRecovered",false);
        ashesFredLineOfSightApproachRecovered=(Boolean)state.getOrDefault("ashesFredLineOfSightApproachRecovered",false);
        ashesTreeApproachFailureRecovered=(Boolean)state.getOrDefault("ashesTreeApproachFailureRecovered",false);
        ashesApproachReloadRecovered=(Boolean)state.getOrDefault("ashesApproachReloadRecovered",false);
        Object savedNoLosTrees=state.get("ashesNoLosTreeTargets");
        if(savedNoLosTrees instanceof Set<?>) for(Object tile:(Set<?>)savedNoLosTrees)
            if(tile instanceof String) ashesNoLosTreeTargets.add((String)tile);
        Object savedAshesFireTile=state.get("ashesFireTile");
        if(savedAshesFireTile instanceof WorldPoint) ashesFireTile=(WorldPoint)savedAshesFireTile;
        ashesFireStartedAt=(Long)state.getOrDefault("ashesFireStartedAt",0L);
        ashesFireAttempts=(Integer)state.getOrDefault("ashesFireAttempts",0);
        ashesTinderboxPurchaseAttempts=(Integer)state.getOrDefault("ashesTinderboxPurchaseAttempts",0);
        waterBucketPurchaseAttempts=(Integer)state.getOrDefault("waterBucketPurchaseAttempts",0);
        waterFillAttempts=(Integer)state.getOrDefault("waterFillAttempts",0);
        waterQuoteRecovered=(Boolean)state.getOrDefault("waterQuoteRecovered",false);
        waterCandidateHoldRecovered=(Boolean)state.getOrDefault("waterCandidateHoldRecovered",false);
        waterApproachAttempts=(Integer)state.getOrDefault("waterApproachAttempts",0);
        Object savedWaterTarget=state.get("waterApproachTarget");
        if(savedWaterTarget instanceof WorldPoint) waterApproachTarget=(WorldPoint)savedWaterTarget;
        Object savedWaterPosition=state.get("waterApproachLastPosition");
        if(savedWaterPosition instanceof WorldPoint) waterApproachLastPosition=(WorldPoint)savedWaterPosition;
        waterApproachStartedAt=(Long)state.getOrDefault("waterApproachStartedAt",0L);
        waterApproachProgressAt=(Long)state.getOrDefault("waterApproachProgressAt",0L);
        if(sourceItem==WATER&&"WATER_LOCAL_SOURCE".equals(geStage)&&waterApproachTarget!=null) {
            long resumedAt=System.currentTimeMillis();
            waterApproachStartedAt=resumedAt; waterApproachProgressAt=resumedAt;
            LOG.info("[PrinceAliRescue] REBASED_WATER_APPROACH_BUDGET after reload target={} pos={} attempts={}",
                waterApproachTarget,waterApproachLastPosition,waterApproachAttempts);
        }
        Object savedAshesLogPosition=state.get("ashesLogApproachLastPosition");
        if(savedAshesLogPosition instanceof WorldPoint) ashesLogApproachLastPosition=(WorldPoint)savedAshesLogPosition;
        ashesLogApproachStartedAt=(Long)state.getOrDefault("ashesLogApproachStartedAt",0L);
        ashesLogApproachProgressAt=(Long)state.getOrDefault("ashesLogApproachProgressAt",0L);
        ashesLogApproachAttempts=(Integer)state.getOrDefault("ashesLogApproachAttempts",0);
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
        woolStageRecoveredFromStatus=(Boolean)state.getOrDefault("woolStageRecoveredFromStatus",false);
        woolProgressAt=(Long)state.getOrDefault("woolProgressAt",0L);
        woolNoSheepSince=(Long)state.getOrDefault("woolNoSheepSince",0L);
        woolStairLastStepAt=(Long)state.getOrDefault("woolStairLastStepAt",0L);
        woolStairLastProgressAt=(Long)state.getOrDefault("woolStairLastProgressAt",0L);
        woolStairStepAttempts=(Integer)state.getOrDefault("woolStairStepAttempts",0);
        Object savedStairPosition=state.get("woolStairLastPosition");
        if(savedStairPosition instanceof WorldPoint) woolStairLastPosition=(WorldPoint)savedStairPosition;
        if(held && "HOLD".equals(phase) && sourceItem==WOOL
            && error.startsWith("GE quote unavailable/above 1000gp cumulative cap id="+WOOL)) {
            held=false; phase="RESUME_LOCAL_WOOL_SOURCE"; error=""; geStage="WOOL_GATHER";
            sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0;
            LOG.info("[PrinceAliRescue] Reload recovery: switching wool from unavailable GE quote to local sheep/shears/spinning-wheel source");
        }
        if(held && "HOLD".equals(phase) && sourceItem==SOFT_CLAY
            &&error.startsWith("GE quote unavailable/above 1000gp cumulative cap id="+SOFT_CLAY+" quote=0 deficit=1")) {
            held=false; phase="RESUME_LOCAL_SOFT_CLAY"; error=""; geStage="SOFT_CLAY_LOCAL_MINE";
            sourceStartedAt=System.currentTimeMillis(); softClayMineAttempts=0;
            LOG.info("[PrinceAliRescue] Reload recovery: exact unavailable soft-clay quote; switching to one-item local source");
        }
        selectedWorld=(Integer)state.getOrDefault("selectedWorld",0);
        loginError=(String)state.getOrDefault("loginError","");
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
            if(!error.isEmpty()) lastReloadHoldError=error;
            held=true; phase="HOLD_RELOAD_IN_FLIGHT";
            error="Reload during "+inFlight+"; inspect quest/inventory/scene before resuming";
        }
        if(sourceItem==0&&!held) restoreExactSavedWoolStage();
        reloadStateRestored=true;
    }

    private void tick() {
        if(stopped||Thread.currentThread().isInterrupted()) return;
        try {
            if(ownsInput!=null&&!ownsInput.getAsBoolean()) { phase="YIELD_OTHER_PLUGIN"; status(null); return; }
            Frame f=Microbot.getClientThread().invoke((java.util.function.Supplier<Frame>)this::observe);
            if(f==null) { phase="WAIT_CLIENT"; status(null); return; }
            if(handleMembershipPromo(f)) { status(f); return; }
            if(handleKeyFurnaceConfirmation(f)) { status(f); return; }
            if(handleGraveRecovery(f)) { status(f); return; }
            if(held) {
                if(recoverBronzeMineDamage(f)) { status(f); return; }
                if(!shantayPathDiagnosticLogged&&sourceItem==BRONZE_BAR&&sourceGoal==1
                    &&"BAR_SHANTAY_SHOP".equals(geStage)&&shantayOpenRetryUsed
                    &&(error.startsWith("Unproved BAR_SHANTAY_OPEN_RETRY;")
                        ||error.startsWith("Reload during BAR_SHANTAY_OPEN_RETRY;"))) {
                    shantayPathDiagnostic=Microbot.getClientThread().invoke(
                        (java.util.function.Supplier<String>)() -> shantayReachabilitySnapshot(f.pos));
                    shantayPathDiagnosticLogged=true;
                    LOG.info("[PrinceAliRescue] SHANTAY_REACHABILITY_DIAGNOSTIC {}",shantayPathDiagnostic);
                }
                if(!shantayHoldDiagnosticLogged&&f.game==GameState.LOGGED_IN
                    &&(error.startsWith("Unproved BAR_SHANTAY_OPEN;")
                        ||error.startsWith("Unproved BAR_SHANTAY_OPEN_RETRY;")
                        ||error.startsWith("Reload during BAR_SHANTAY_OPEN_RETRY;"))) {
                    shantayHoldDiagnosticLogged=true;
                    LOG.info("[PrinceAliRescue] SHANTAY_HOLD_WIDGET_DIAGNOSTIC widgetSelected={} selectedWidgetId={} selectedWidgetParentId={} selectedWidgetItemId={} selectedWidgetName={} selectedWidgetText={} shopOpen={} shopWidgetPresent={} shopWidgetHidden={} pos={} varp={} coins={} keyPrint={}",
                        f.widgetSelected,f.selectedWidgetId,f.selectedWidgetParentId,f.selectedWidgetItemId,
                        f.selectedWidgetName,f.selectedWidgetText,f.shop,f.shopWidgetPresent,
                        f.shopWidgetHidden,f.pos,f.varp,f.count(COINS),f.count(KEY_PRINT));
                }
                if(recoverObservedContinueHold(f)) { status(f); return; }
                if(recoverObservedSoftClayMineGain(f)) { status(f); return; }
                if(!softClayMineRecoveryDiagnosticLogged&&error.contains("MINE_SOFT_CLAY")) {
                    softClayMineRecoveryDiagnosticLogged=true;
                    LOG.info("[PrinceAliRescue] SOFT_CLAY_RECOVERY_CHECK phase={} error={} sourceItem={} goal={} game={} varp={} pos={} clay={} softClay={} pickaxe={} stage={} attempts={} restoredAction={}",
                        phase,error,sourceItem,sourceGoal,f.game,f.varp,f.pos,f.count(CLAY),
                        f.count(SOFT_CLAY),f.count(1265),geStage,softClayMineAttempts,restoredInFlightAction);
                }
                // A quest HOLD must not disable the existing native reconnect or
                // WelcomeScreenEvent path. Preserve the original quest diagnosis.
                String savedPhase=phase, savedError=error;
                if(loginTick(f)) {
                    held=true; phase=savedPhase; error=savedError; status(f); return;
                }
                held=true; phase=savedPhase; error=savedError;
                if(recoverObservedUnavailableDyeQuote(f)) { status(f); return; }
                if(recoverObservedUnavailableSoftClayQuote(f)) { status(f); return; }
                if(recoverObservedOnionPickHold(f)) { status(f); return; }
                if(recoverObservedOnionSecondPickCap(f)) { status(f); return; }
                if(recoverObservedOnionTimeoutHold(f)) { status(f); return; }
                if(recoverObservedUnavailableAshesQuote(f)) { status(f); return; }
                if(recoverObservedUnavailableWaterQuote(f)) { status(f); return; }
                if(recoverFreeBronzeRoute(f)) { status(f); return; }
                if(recoverObservedUnavailableBronzeBarQuote(f)) { status(f); return; }
                if(recoverObservedShantayApproachAfterReload(f)) { status(f); return; }
                if(recoverObservedShantayReachableTileHold(f)) { status(f); return; }
                if(recoverObservedShantayOpenHold(f)) { status(f); return; }
                if(recoverObservedWaterFountainWithoutDirectAction(f)) { status(f); return; }
                if(recoverObservedMissingAshesTinderbox(f)) { status(f); return; }
                if(recoverObservedMissingAshesLog(f)) { status(f); return; }
                if(recoverObservedTreeChopAfterReload(f)) { status(f); return; }
                if(recoverObservedInaccessibleTreeTarget(f)) { status(f); return; }
                if(recoverObservedNoTreeAtFred(f)) { status(f); return; }
                if(recoverObservedFredTreeLineOfSightHold(f)) { status(f); return; }
                if(recoverObservedTreeApproachHold(f)) { status(f); return; }
                if(recoverObservedTreeApproachReload(f)) { status(f); return; }
                if(recoverObservedWoolSpinAfterReload(f)) { status(f); return; }
                if(recoverObservedWoolDescentHold(f)) { status(f); return; }
                if(recoverObservedWoolGatherPlaneHold(f)) { status(f); return; }
                if(recoverObservedWoolStairRouteHold(f)) { status(f); return; }
                status(f); return;
            }
            if(loginTick(f)) { status(f); return; }
            if(f.quest==QuestState.FINISHED) { phase="COMPLETE_QUEST_STATE"; status(f); return; }
            if(bankCleanupRequired&&pending==null) { prepareBankInventory(f); return; }
            if(keyHandinPending&&f.count(KEY_PRINT)==0) {
                keySubmitted=true; keyHandinPending=false;
            }
            String signature=f.varp+":"+f.items.hashCode();
            if(!signature.equals(lastMaterialSignature)) {
                talkAttempts.clear(); lastMaterialSignature=signature;
            }
            if(f.health>=0&&f.health<25&&!bankCleanupRequired) { hold(f,"Health below 25%; jail guard risk, no unverified combat recovery"); return; }
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
                if("CONTINUE".equals(pending.action)) {
                    continueRetryUsed=false; restoredInFlightAction=""; lastReloadHoldError="";
                }
                if("WOOL_CLIMB_DOWN".equals(pending.action)) resetWoolStairRoute();
                    if("GIVE_PRINT_OSMAN".equals(pending.action)&&f.count(KEY_PRINT)<pending.before.count(KEY_PRINT)) keySubmitted=true;
                    if(pending.action.startsWith("WALK_")) { routeFailures=0; lastMoveAt=System.currentTimeMillis(); }
                    if("WOOL_SHEAR".equals(pending.action)) {
                        failedWoolSheep.clear(); woolShearTarget=""; woolShearFailures=0;
                    }
                if("WOOL_SPIN".equals(pending.action)) {
                        woolSpinning=true; woolProgressAt=System.currentTimeMillis();
                        lastRawWool=f.count(RAW_WOOL); lastWoolBalls=f.count(WOOL);
                    }
                    if("PICK_DYE_ONION".equals(pending.action)) sourceAttempts=0;
                    if("LIGHT_ASH_FIRE".equals(pending.action)) {
                        ashesFireTile=pending.target; ashesFireStartedAt=System.currentTimeMillis();
                        LOG.info("[PrinceAliRescue] PROVED_ASHES_FIRE tile={} at={}",
                            ashesFireTile,ashesFireStartedAt);
                    }
                    if("SOURCE_SHOP_OPEN".equals(pending.action))
                        sourceShopOpenedAt=System.currentTimeMillis();
                    if("BAR_SHANTAY_OPEN".equals(pending.action))
                        sourceShopOpenedAt=System.currentTimeMillis();
                    if("BAR_SHANTAY_OPEN_RETRY".equals(pending.action))
                        sourceShopOpenedAt=System.currentTimeMillis();
                    if("WATER_SHOP_OPEN".equals(pending.action))
                        sourceShopOpenedAt=System.currentTimeMillis();
                    if("ASHES_SHOP_OPEN".equals(pending.action))
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
    private boolean handleMembershipPromo(Frame f) {
        // A missing interface during logout/loading is not dismissal proof.
        if(f.game!=GameState.LOGGED_IN||f.pos==null) return false;
        if("DISMISS_MEMBER_PROMO".equals(restoredInFlightAction)&&memberPromoClickedAt>0) {
            if(!f.memberPromoVisible) {
                held=memberPromoPriorHeld; phase=memberPromoPriorPhase; error=memberPromoPriorError;
                restoredInFlightAction=""; memberPromoDismissAttempted=false;
                memberPromoDiagnosticLogged=false; memberPromoClickedAt=0;
                LOG.info("[PrinceAliRescue] PROVED DISMISS_MEMBER_PROMO afterReload=popupHidden loggedIn=true");
                return resumeShantayAfterMembershipPromo(f);
            }
            if(System.currentTimeMillis()-memberPromoClickedAt>6000) {
                restoredInFlightAction="";
                hold(f,"Membership promo persisted across reload after one dismissal; no replay");
            }
            return true;
        }
        if(pending!=null&&!"DISMISS_MEMBER_PROMO".equals(pending.action)) {
            if(!held) return false; // Let the existing action finish its proof window.
            if(!error.startsWith("Unproved "+pending.action+";")) return false;
            LOG.info("[PrinceAliRescue] PROMO_INSPECT_AFTER_TERMINAL_ACTION {}",pending.action);
            pending=null;
        }
        if(pending!=null&&"DISMISS_MEMBER_PROMO".equals(pending.action)) {
            if(!f.memberPromoVisible) {
                pending=null; memberPromoDismissAttempted=false; memberPromoDiagnosticLogged=false;
                if(memberPromoClickedAt>0) {
                    held=memberPromoPriorHeld; phase=memberPromoPriorPhase; error=memberPromoPriorError;
                    memberPromoClickedAt=0;
                }
                LOG.info("[PrinceAliRescue] PROVED DISMISS_MEMBER_PROMO nextFrame=popupHidden");
                if(resumeShantayAfterMembershipPromo(f)) return true;
                return false;
            }
            if(System.currentTimeMillis()>pending.deadline) {
                pending=null;
                hold(f,"Membership promo close X was clicked once but the prompt remained visible; no repeat; "+f.memberPromoDiagnostic);
                return true;
            }
            phase="WAIT_MEMBERSHIP_PROMO_DISMISS_PROOF";
            return true;
        }
        if(!f.memberPromoVisible) {
            memberPromoDismissAttempted=false; memberPromoDiagnosticLogged=false;
            if(shantayPromoRecoveryAvailable&&resumeShantayAfterMembershipPromo(f)) return true;
            return false;
        }
        if(!memberPromoDiagnosticLogged) {
            memberPromoDiagnosticLogged=true;
            LOG.info("[PrinceAliRescue] MEMBERSHIP_PROMO_VISIBLE {}",f.memberPromoDiagnostic);
        }
        if(held&&shantayReachableTradeUsed&&!shantayPromoRetryUsed
            &&(error.startsWith("Unproved BAR_SHANTAY_REACHABLE_TRADE;")
                ||error.startsWith("Reload during BAR_SHANTAY_REACHABLE_TRADE;")))
            shantayPromoRecoveryAvailable=true;
        if(memberPromoDismissAttempted) {
            if(pending==null&&!held) hold(f,"Membership promo is still visible after its one dismissal attempt; no repeat; "+f.memberPromoDiagnostic);
            return true;
        }
        Widget close=f.memberPromoCloseWidget;
        if(close==null) {
            if(held&&!error.contains("close X was not uniquely identified")) error=error+"; membership promo blocks UI; close X was not uniquely identified";
            else hold(f,"Membership promo blocks the game UI; close X was not uniquely identified; "+f.memberPromoDiagnostic);
            return true;
        }
        java.awt.Rectangle freshBounds=Microbot.getClientThread().invoke((java.util.function.Supplier<java.awt.Rectangle>)() -> {
            PromoProbe fresh=inspectMemberPromo(Microbot.getClient());
            if(!fresh.visible||fresh.closeWidget==null) return null;
            LOG.info("[PrinceAliRescue] MEMBERSHIP_PROMO_CLOSE_SNAPSHOT {}",fresh.diagnostic);
            return new java.awt.Rectangle(fresh.closeWidget.getBounds());
        });
        if(freshBounds==null) {
            if(held) error=error+"; membership promo changed before X click; no action sent";
            else hold(f,"Membership promo changed before close X click; no action sent");
            return true;
        }
        memberPromoPriorHeld=held; memberPromoPriorPhase=phase; memberPromoPriorError=error;
        memberPromoClickedAt=System.currentTimeMillis();
        memberPromoDismissAttempted=true;
        LOG.info("[PrinceAliRescue] DISMISS_MEMBER_PROMO_DISPATCH bounds={}",freshBounds);
        Microbot.getMouse().click(freshBounds);
        set("DISMISS_MEMBER_PROMO",f,6000,0,null);
        return true;
    }
    private boolean resumeShantayAfterMembershipPromo(Frame f) {
        if(FREE_TO_PLAY_ACCOUNT) return false;
        if(!shantayPromoRecoveryAvailable||shantayPromoRetryUsed||!held
            ||sourceItem!=BRONZE_BAR||sourceGoal!=1||!"BAR_SHANTAY_SHOP".equals(geStage)
            ||!bankInspected||f.game!=GameState.LOGGED_IN||f.quest!=QuestState.IN_PROGRESS||f.varp!=20
            ||f.pos==null||f.pos.getPlane()!=0||f.count(KEY_PRINT)<=0||f.count(BRONZE_BAR)>0
            ||f.count(COINS)<=0||f.count(COINS)>SHANTAY_BAR_COIN_CAP||f.shop
            ||f.inDialogue||f.continuePrompt||!f.options.isEmpty()||f.widgetSelected) return false;
        Set<WorldPoint> excluded=new HashSet<>();
        excluded.add(f.pos);
        excluded.add(new WorldPoint(3304,3123,0));
        excluded.add(new WorldPoint(3302,3124,0));
        excluded.add(new WorldPoint(3303,3123,0));
        if(shantayApproachTarget!=null) excluded.add(shantayApproachTarget);
        WorldPoint playerPos=f.pos;
        WorldPoint target=Microbot.getClientThread().invoke(
            (java.util.function.Supplier<WorldPoint>)() -> findShantayReachableInteractionTile(playerPos,excluded));
        if(target==null) {
            LOG.warn("[PrinceAliRescue] MEMBERSHIP_PROMO_CLOSED_BUT_NO_DISTINCT_SHANTAY_TILE player={} excluded={}",playerPos,excluded);
            return false;
        }
        sourceStartedAt=System.currentTimeMillis(); shantayApproachTarget=target;
        shantayPromoRecoveryAvailable=false; shantayPromoRetryActive=true;
        held=false; error=""; pending=null; restoredInFlightAction="";
        phase="RESUME_SHANTAY_AFTER_MEMBERSHIP_PROMO";
        LOG.info("[PrinceAliRescue] RESUME_SHANTAY_AFTER_MEMBERSHIP_PROMO player={} target={} excluded={} priorTradeUsed=true; exactly one post-dismissal attempt reserved",
            playerPos,target,excluded);
        return true;
    }
    private Frame observe() {
        Client c=Microbot.getClient(); if(c==null) return null;
        Frame f=new Frame(); f.game=c.getGameState(); f.loginIndex=c.getLoginIndex();
        f.world=c.getWorld(); f.canvasWidth=c.getCanvasWidth();
        if(f.game!=GameState.LOGGED_IN||c.getLocalPlayer()==null) return f;
        f.widgetSelected=c.isWidgetSelected();
        Widget selectedWidget=c.getSelectedWidget();
        if(selectedWidget!=null) {
            f.selectedWidgetId=selectedWidget.getId();
            f.selectedWidgetParentId=selectedWidget.getParentId();
            f.selectedWidgetItemId=selectedWidget.getItemId();
            f.selectedWidgetName=selectedWidget.getName()==null?"":selectedWidget.getName();
            f.selectedWidgetText=selectedWidget.getText()==null?"":selectedWidget.getText();
        }
        Widget shopWidget=c.getWidget(19660800);
        f.shopWidgetPresent=shopWidget!=null;
        f.shopWidgetHidden=shopWidget==null||shopWidget.isHidden();
        f.pos=c.getLocalPlayer().getWorldLocation(); f.animation=c.getLocalPlayer().getAnimation();
        f.quest=Quest.PRINCE_ALI_RESCUE.getState(c); f.varp=c.getVarpValue(VARP);
        f.health=Rs2Player.getHealthPercentage(); f.bank=Rs2Bank.isOpen(); f.shop=Rs2Shop.isOpen();
        f.hp=c.getBoostedSkillLevel(Skill.HITPOINTS); f.maxHp=c.getRealSkillLevel(Skill.HITPOINTS);
        f.combatLevel=c.getLocalPlayer().getCombatLevel(); f.inCombat=Rs2Player.isInCombat();
        if(f.bank) {
            net.runelite.api.ItemContainer bank=c.getItemContainer(net.runelite.api.InventoryID.BANK);
            f.bankContentsAvailable=bank!=null&&bank.getItems()!=null;
            if(f.bankContentsAvailable) for(net.runelite.api.Item item:bank.getItems())
                if(item!=null&&item.getId()>0&&item.getQuantity()>0)
                    f.bankItems.merge(item.getId(),item.getQuantity(),Integer::sum);
        }
        f.production=findProduct(c)!=null;
        f.continuePrompt=Rs2Dialogue.hasContinue(); f.inDialogue=Rs2Dialogue.isInDialogue();
        String text=Rs2Dialogue.getDialogueText(); f.dialogue=text==null?"":text;
        String question=Rs2Dialogue.getQuestion(); f.question=question==null?"":question;
        StringBuilder options=new StringBuilder();
        for(Widget w:Rs2Dialogue.getDialogueOptions())
            if(w!=null&&w.getText()!=null) options.append(w.getText()).append('|');
        f.options=options.toString();
        PromoProbe promo=inspectMemberPromo(c);
        f.memberPromoVisible=promo.visible;
        f.memberPromoCloseWidget=promo.closeWidget;
        f.memberPromoDiagnostic=promo.diagnostic;
        Rs2Inventory.items().forEach(item -> f.items.merge(item.getId(),item.getQuantity(),Integer::sum));
        if(shantayApproachTarget!=null&&f.pos.equals(shantayApproachTarget)) {
            net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel shantay=Rs2Shop.getNearestShopNpc("Shantay",true);
            if(shantay!=null) {
                f.shantayNpcId=shantay.getId(); f.shantayNpcTile=shantay.getWorldLocation();
                NPCComposition base=shantay.getComposition();
                String[] actions=base==null?null:base.getActions();
                f.shantayNpcBaseActions=java.util.Arrays.toString(actions);
                f.shantayNpcTradeAction=actions!=null&&java.util.Arrays.stream(actions).anyMatch("Trade"::equalsIgnoreCase);
                WorldArea playerArea=c.getLocalPlayer().getWorldArea(), npcArea=shantay.getWorldArea();
                if(playerArea!=null&&npcArea!=null&&c.getTopLevelWorldView()!=null) {
                    f.shantayNpcAreaDistance=playerArea.distanceTo(npcArea);
                    f.shantayNpcLos=playerArea.hasLineOfSightTo(c.getTopLevelWorldView(),npcArea);
                }
            }
        }
        return f;
    }
    private static final class PromoProbe {
        final boolean visible;
        final Widget closeWidget;
        final String diagnostic;
        PromoProbe(boolean visible,Widget closeWidget,String diagnostic) {
            this.visible=visible; this.closeWidget=closeWidget; this.diagnostic=diagnostic;
        }
    }
    private static PromoProbe inspectMemberPromo(Client client) {
        if(client==null) return new PromoProbe(false,null,"client unavailable");
        // Installed API names this sprite banner and its X explicitly. Its words
        // are artwork and need not occur in any widget's text.
        Widget content=client.getWidget(net.runelite.api.gameval.InterfaceID.MembershipBenefitsPrompt.CONTENT);
        Widget close=client.getWidget(net.runelite.api.gameval.InterfaceID.MembershipBenefitsPrompt.CLOSE);
        Widget art=client.getWidget(net.runelite.api.gameval.InterfaceID.MembershipBenefitsPrompt.ARTCANVAS);
        if(content!=null&&!content.isHidden()&&content.getBounds()!=null
            &&content.getBounds().width>0&&content.getBounds().height>0) {
            java.awt.Rectangle box=content.getBounds();
            java.awt.Rectangle button=close==null?null:close.getBounds();
            boolean valid=close!=null&&!close.isHidden()&&button!=null
                &&button.width>0&&button.height>0&&button.width<=64&&button.height<=64
                &&box.contains(button.getCenterX(),button.getCenterY())
                &&button.intersects(new java.awt.Rectangle(0,0,client.getCanvasWidth(),client.getCanvasHeight()));
            return new PromoProbe(true,valid?close:null,"namedMembershipPrompt content={"+widgetSummary(content)
                +"} art={"+(art==null?"none":widgetSummary(art))+"} close={"+(close==null?"none":widgetSummary(close))+"}");
        }
        String marker="become a member";
        List<Widget> path=findWidgetTextPath(client==null?null:client.getWidgetRoots(),marker,
            new ArrayList<>(),java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));
        if(path==null) return new PromoProbe(false,null,"");
        Widget match=path.get(path.size()-1);
        StringBuilder report=new StringBuilder("markerWidget=").append(widgetSummary(match));
        Widget uniqueClose=null; String selectedAncestor="";
        for(int i=path.size()-2;i>=0;i--) {
            Widget ancestor=path.get(i); java.awt.Rectangle box=ancestor.getBounds();
            if(box==null||box.width<50||box.height<30||box.width>600||box.height>250) continue;
            List<Widget> subtree=new ArrayList<>();
            collectWidgetTree(ancestor,subtree,
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()),0);
            List<Widget> semantic=new ArrayList<>(), geometric=new ArrayList<>();
            for(Widget w:subtree) {
                if(w==match||w==ancestor||w.isHidden()) continue;
                if(isCloseLabel(w)&&isUpperRightCloseCandidate(w,box)) semantic.add(w);
            }
            if(semantic.size()==1) { uniqueClose=semantic.get(0); selectedAncestor=widgetSummary(ancestor); break; }
            if(semantic.size()>1) { report.append("; ambiguousCloseLabels=").append(summaries(semantic,8)); break; }
            if(geometric.size()==1) { uniqueClose=geometric.get(0); selectedAncestor=widgetSummary(ancestor); break; }
            if(geometric.size()>1) { report.append("; ambiguousUpperRightCandidates=").append(summaries(geometric,8)); break; }
            report.append("; ancestor=").append(widgetSummary(ancestor))
                .append(" descendants=").append(summaries(subtree,32));
        }
        report.append("; closeWidget=").append(uniqueClose==null?"none":"{"+widgetSummary(uniqueClose)+"} through {"+selectedAncestor+"}");
        String diagnostic=report.length()>2800?report.substring(0,2800):report.toString();
        return new PromoProbe(true,uniqueClose,diagnostic);
    }
    private static List<Widget> findWidgetTextPath(Widget[] widgets,String marker,
            List<Widget> path,Set<Widget> visited) {
        if(widgets==null) return null;
        for(Widget w:widgets) {
            if(w==null||!visited.add(w)) continue;
            path.add(w);
            String text=w.getText();
            if(!w.isHidden()&&text!=null&&text.toLowerCase(java.util.Locale.ROOT).contains(marker)) return new ArrayList<>(path);
            List<Widget> children=new ArrayList<>();
            if(w.getChildren()!=null) java.util.Collections.addAll(children,w.getChildren());
            if(w.getDynamicChildren()!=null) java.util.Collections.addAll(children,w.getDynamicChildren());
            if(w.getStaticChildren()!=null) java.util.Collections.addAll(children,w.getStaticChildren());
            if(w.getNestedChildren()!=null) java.util.Collections.addAll(children,w.getNestedChildren());
            List<Widget> found=findWidgetTextPath(children.toArray(new Widget[0]),marker,path,visited);
            if(found!=null) return found;
            path.remove(path.size()-1);
        }
        return null;
    }
    private static void collectWidgetTree(Widget root,List<Widget> out,Set<Widget> visited,int depth) {
        if(root==null||depth>12||!visited.add(root)) return;
        out.add(root);
        Widget[] children=root.getChildren();
        if(children!=null) for(Widget child:children) collectWidgetTree(child,out,visited,depth+1);
        children=root.getDynamicChildren();
        if(children!=null) for(Widget child:children) collectWidgetTree(child,out,visited,depth+1);
        children=root.getStaticChildren();
        if(children!=null) for(Widget child:children) collectWidgetTree(child,out,visited,depth+1);
        children=root.getNestedChildren();
        if(children!=null) for(Widget child:children) collectWidgetTree(child,out,visited,depth+1);
    }
    private static boolean isCloseLabel(Widget w) {
        String text=w.getText()==null?"":w.getText().trim();
        String name=w.getName()==null?"":w.getName().trim();
        if(isCloseWord(text)||isCloseWord(name)) return true;
        String[] actions=w.getActions();
        if(actions!=null) for(String action:actions) if(isCloseWord(action==null?"":action.trim())) return true;
        return false;
    }
    private static boolean isCloseWord(String value) {
        return "x".equalsIgnoreCase(value)||"×".equals(value)||"close".equalsIgnoreCase(value)
            ||"dismiss".equalsIgnoreCase(value);
    }
    private static boolean isUpperRightCloseCandidate(Widget w,java.awt.Rectangle container) {
        java.awt.Rectangle b=w.getBounds();
        if(b==null||w.isHidden()||b.width<1||b.height<1||b.width>56||b.height>56
            ||container.width>600||container.height>250) return false;
        double x=(b.getCenterX()-container.x)/container.width;
        double y=(b.getCenterY()-container.y)/container.height;
        boolean clickable=w.hasListener()||w.getSpriteId()>=0
            ||(w.getActions()!=null&&w.getActions().length>0);
        return clickable&&x>=0.72&&x<=1.04&&y>=-0.04&&y<=0.36;
    }
    private static String widgetSummary(Widget w) {
        String text=w.getText()==null?"":w.getText().replace('\n',' ');
        String name=w.getName()==null?"":w.getName();
        return "id="+w.getId()+" parent="+w.getParentId()+" index="+w.getIndex()+
            " text='"+truncate(text,90)+"' name='"+truncate(name,48)+"' sprite="+w.getSpriteId()+
            " bounds="+w.getBounds()+" listener="+w.hasListener()+" actions="+
            java.util.Arrays.toString(w.getActions());
    }
    private static String summaries(List<Widget> widgets,int cap) {
        StringBuilder b=new StringBuilder("[");
        for(int i=0;i<Math.min(cap,widgets.size());i++) {
            if(i>0) b.append("; "); b.append(widgetSummary(widgets.get(i)));
        }
        if(widgets.size()>cap) b.append("; ...count=").append(widgets.size());
        return b.append(']').toString();
    }
    private String shantayReachabilitySnapshot(WorldPoint playerPos) {
        Client c=Microbot.getClient();
        if(c==null||playerPos==null||c.getLocalPlayer()==null) return "client/player unavailable";
        net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel npc=Rs2Shop.getNearestShopNpc("Shantay",true);
        if(npc==null) return "live Trade-capable Shantay not found";
        WorldPoint npcTile=npc.getWorldLocation(); WorldArea npcArea=npc.getWorldArea();
        WorldArea playerArea=c.getLocalPlayer().getWorldArea();
        net.runelite.api.WorldView view=c.getTopLevelWorldView();
        if(npcTile==null||npcArea==null||playerArea==null||view==null) return "NPC/player area or world view unavailable";
        boolean los=playerArea.hasLineOfSightTo(view,npcArea);
        java.util.HashMap<WorldPoint,Integer> reachable=Rs2Tile.getReachableTilesFromTile(playerPos,8);
        java.util.List<String> candidates=new java.util.ArrayList<>();
        int minX=npcArea.getX()-1, maxX=npcArea.getX()+npcArea.getWidth();
        int minY=npcArea.getY()-1, maxY=npcArea.getY()+npcArea.getHeight();
        for(int x=minX;x<=maxX;x++) for(int y=minY;y<=maxY;y++) {
            boolean perimeter=x==minX||x==maxX||y==minY||y==maxY;
            if(!perimeter) continue;
            WorldPoint tile=new WorldPoint(x,y,npcArea.getPlane());
            Integer steps=reachable.get(tile);
            if(steps==null||(!tile.equals(playerPos)&&!Rs2Tile.isWalkable(tile))) continue;
            boolean tileLos=new WorldArea(tile,1,1).hasLineOfSightTo(view,npcArea);
            if(tileLos) candidates.add(tile+":"+steps);
        }
        NPCComposition composition=npc.getTransformedComposition();
        return "npcId="+npc.getId()+" npcTile="+npcTile+" npcArea="+npcArea.getWidth()+"x"+npcArea.getHeight()
            +" player="+playerPos+" distance="+playerPos.distanceTo(npcTile)+" playerLos="+los
            +" reachableLosInteractionTiles="+candidates+" reachableCount="+reachable.size()
            +" convexHull="+(npc.getConvexHull()!=null)+" canvasPoly="+(npc.getCanvasTilePoly()!=null)
            +" actions="+(composition==null?"[]":java.util.Arrays.toString(composition.getActions()));
    }
    private boolean loginTick(Frame f) {
        long now=System.currentTimeMillis();
        if(f.game==GameState.LOGGED_IN) {
            WelcomeScreenEvent welcome=new WelcomeScreenEvent();
            if(!welcome.validate()) {
                loginAttempts=welcomeAttempts=disconnectAttempts=0; loginError=""; return false;
            }
            phase="WAIT_WELCOME";
            if(welcomeAttempts++==0) {
                welcomeAt=now; LOG.info("[PrinceAliRescue] WELCOME_DISMISS_DISPATCH via WelcomeScreenEvent");
                welcome.execute();
            }
            else if(now-welcomeAt>12000) loginHold(f,"Welcome screen persisted after native execute");
            return true;
        }
        if(f.game!=GameState.LOGIN_SCREEN) { phase="WAIT_LOGIN_SCREEN"; return true; }
        if(f.loginIndex==24) {
            if(disconnectAttempts++==0&&f.canvasWidth>0) {
                LOG.info("[PrinceAliRescue] DISCONNECT_MODAL_DISMISS_DISPATCH loginIndex=24 canvasWidth={}",f.canvasWidth);
                Microbot.getClientThread().invoke(() -> {
                    Microbot.getMouse().click(365+(f.canvasWidth-804)/2,308); return true;
                });
                disconnectAt=now; phase="VERIFY_DISCONNECT_DISMISS";
            } else if(now-disconnectAt>8000) loginHold(f,"Disconnected modal remained after native dismiss");
            return true;
        }
        if(disconnectAttempts>0) {
            LOG.info("[PrinceAliRescue] DISCONNECT_MODAL_DISMISS_PROVED loginIndex={}",f.loginIndex);
            disconnectAttempts=0; loginAttempts=0; loginAt=0;
            loginError="";
            phase="DISCONNECT_DISMISSED"; return true;
        }
        disconnectAttempts=0;
        if(f.loginIndex!=10&&f.loginIndex!=34) { phase="WAIT_LOGIN_INDEX_"+f.loginIndex; return true; }
        if(selectedWorld==0) {
            selectedWorld=LoginManager.getRandomWorld(false);
            if(selectedWorld<=0||LoginManager.isMemberWorld(selectedWorld)) {
                loginHold(f,"No verified ordinary free world from native LoginManager"); return true;
            }
        }
        if(loginAttempts++==0) {
            loginAt=now; phase="VERIFY_NATIVE_LOGIN";
            LOG.info("[PrinceAliRescue] NATIVE_LOGIN_DISPATCH world={} index={}",selectedWorld,f.loginIndex);
            if(!LoginManager.login(selectedWorld)) loginHold(f,"Native LoginManager.login rejected");
        } else if(now-loginAt>20000) loginHold(f,"Native login did not reach game; index="+f.loginIndex);
        return true;
    }
    private void loginHold(Frame f,String reason) {
        if(held) {
            if(!reason.equals(loginError)) LOG.warn("[PrinceAliRescue] LOGIN_HOLD {}",reason);
            loginError=reason;
            return;
        }
        loginError=reason;
        hold(f,reason);
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
                makeBronzeKey(f); return;
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
    private void makeBronzeKey(Frame f) {
        if(closeIfOpen(f)) return;
        WorldPoint furnacePoint=new WorldPoint(3273,3184,0);
        if(f.pos==null||f.pos.distanceTo(furnacePoint)>6) {
            walk(f,furnacePoint,"TO_KEY_FURNACE"); return;
        }
        // Current quest-helper source: use the print on a furnace with a bronze bar.
        // Verify a live furnace before using the installed item-on-object API.
        TileObject furnace=Rs2GameObject.getAll(o->o!=null,f.pos,8).stream()
            .filter(o->Rs2GameObject.hasAction(o,"Smelt"))
            .min(Comparator.comparingInt(o->o.getWorldLocation().distanceTo(f.pos))).orElse(null);
        if(furnace==null) { hold(f,"No live Smelt furnace for bronze key at "+f.pos); return; }
        LOG.info("[PrinceAliRescue] MAKE_KEY_FURNACE_DISPATCH object={} tile={} print={} bar={}",
            furnace.getId(),furnace.getWorldLocation(),f.count(KEY_PRINT),f.count(BRONZE_BAR));
        if(Rs2Inventory.useItemOnObject(KEY_PRINT,furnace.getId()))
            set("MAKE_BRONZE_KEY",f,15000,BRONZE_KEY,furnace.getWorldLocation());
        else hold(f,"Key print on live furnace rejected; no automatic replay");
    }
    private boolean handleKeyFurnaceConfirmation(Frame f) {
        boolean awaiting=pending!=null&&"MAKE_BRONZE_KEY".equals(pending.action);
        boolean timedOut=held&&error.startsWith("Unproved MAKE_BRONZE_KEY;");
        if(keyFurnaceConfirmed||(!awaiting&&!timedOut)||f.game!=GameState.LOGGED_IN
            ||f.varp!=20||f.pos==null||f.pos.distanceTo(new WorldPoint(3273,3184,0))>6
            ||f.count(KEY_PRINT)!=1||f.count(BRONZE_BAR)<1||f.count(BRONZE_KEY)>0
            ||!f.options.equals("Yes|No|")) return false;
        LOG.info("[PrinceAliRescue] KEY_FURNACE_CONFIRMATION question={} options={} player={}",f.question,f.options,f.pos);
        if(!f.question.equals("Create a key using the key print?")) {
            hold(f,"Unexpected furnace confirmation question: "+f.question); return true;
        }
        keyFurnaceConfirmed=true;
        held=false; error="";
        if(Rs2Dialogue.clickOption("Yes")) set("MAKE_BRONZE_KEY",f,15000,BRONZE_KEY,new WorldPoint(3273,3184,0));
        else hold(f,"Verified key furnace Yes dispatch rejected");
        return true;
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
        if(!f.bankContentsAvailable) {
            if(bankEvidenceWaitAt==0) bankEvidenceWaitAt=System.currentTimeMillis();
            if(System.currentTimeMillis()-bankEvidenceWaitAt>6000) hold(f,"Open bank has no live item container; absence is unproven for item="+id);
            else { phase="WAIT_LIVE_BANK_CONTENTS"; status(f); }
            return false;
        }
        bankEvidenceWaitAt=0;
        int liveBankCount=f.bankItems.getOrDefault(id,0);
        LOG.info("[PrinceAliRescue] BANK_ITEM_CHECK id={} inventory={} needed={} liveBankCount={} at={} pid={}",
            id,f.count(id),amount,liveBankCount,System.currentTimeMillis(),ProcessHandle.current().pid());
        if(liveBankCount>0&&!Rs2Bank.hasBankItem(id,Math.min(deficit,liveBankCount))) {
            hold(f,"Live bank contains item="+id+" qty="+liveBankCount+" but withdrawal model disagrees; do not buy/gather");
            return false;
        }
        if(id==BRONZE_BAR&&liveBankCount==0) {
            for(int ore:new int[]{436,438}) if(f.count(ore)==0&&f.bankItems.getOrDefault(ore,0)>0) {
                if(!Rs2Bank.hasWithdrawAsItem()) {
                    if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                    else hold(f,"Bronze ingredient withdraw mode rejected");
                } else if(Rs2Bank.withdrawDeficit(ore,1)) set("WITHDRAW",f,7000,ore,null);
                else hold(f,"Banked bronze ingredient withdrawal rejected id="+ore);
                return false;
            }
        }
        if(id==SOFT_CLAY) {
            int ingredient=f.count(CLAY)==0&&Rs2Bank.hasBankItem(CLAY,1)?CLAY:
                f.count(WATER)==0&&Rs2Bank.hasBankItem(WATER,1)?WATER:0;
            if(ingredient!=0) {
                if(!Rs2Bank.hasWithdrawAsItem()) {
                    if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                    else hold(f,"Soft-clay ingredient withdraw mode rejected");
                } else if(Rs2Bank.withdrawDeficit(ingredient,1))
                    set("WITHDRAW",f,7000,ingredient,null);
                else hold(f,"Soft-clay ingredient withdrawal rejected id="+ingredient);
                return false;
            }
            if(f.count(CLAY)>0&&f.count(WATER)==0) {
                LOG.info("[PrinceAliRescue] SOFT_CLAY_LOCAL_PLAN source=water-from-existing-local-water-route clay=1 water=0 bankWater=false");
                beginSource(f,WATER,1);
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
        Boolean membersItem=Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>)() -> {
            net.runelite.api.ItemComposition definition=Microbot.getClient().getItemDefinition(id);
            return definition==null?null:definition.isMembers();
        });
        if(FREE_TO_PLAY_ACCOUNT&&(membersItem==null||membersItem)) {
            hold(f,"F2P acquisition rejected: item="+id+" membership="+membersItem+"; choose a verified free item/source");
            return;
        }
        if(id!=ROPE&&id!=SKIRT&&id!=BEER&&id!=REDBERRIES&&id!=FLOUR
            &&!geEligible(id)) {
            hold(f,"No verified local source for item id="+id+" deficit="+(amount-f.count(id))+
                "; GE runtime stock/server price unverified; no blind offer placed");
            return;
        }
        sourceItem=id; sourceGoal=amount; sourceAttempts=0; sourceSpent=0;
        if(id==BRONZE_BAR) shantayOpenRetryUsed=false;
        sourceLastCount=f.count(id); sourceStartedAt=System.currentTimeMillis();
        sourceLastCoins=f.count(COINS);
        sourceShopOpenedAt=0;
        geStage=id==WOOL?"WOOL_GATHER":id==WATER?"WATER_LOCAL_SOURCE":
            id==BRONZE_BAR?(FREE_TO_PLAY_ACCOUNT?"BAR_F2P_MINE_SMELT":"BAR_SHANTAY_SHOP"):geEligible(id)?"PREPARE":"";
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
            id==BRONZE_BAR?(FREE_TO_PLAY_ACCOUNT?"BRONZE_BAR_F2P_MINE_SMELT":"BRONZE_BAR_SHANTAY_SHOP"):
            id==FLOUR?"FLOUR_WYDIN":"UNKNOWN_"+id;
    }
    private static int unitCap(int id) {
        return id==ROPE?18:id==SKIRT||id==BEER?2:id==REDBERRIES?3:id==FLOUR?10:0;
    }
    private void sourceTick(Frame f) {
        if(woolStageRecoveredFromStatus) {
            if(f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=1
                ||f.pos.distanceTo(WOOL_WHEEL)>10||f.count(WOOL)!=1||f.count(RAW_WOOL)!=0
                ||f.count(SHEARS)!=1) {
                sourceItem=sourceGoal=0; bankInspected=false;
                woolStageRecoveredFromStatus=false; phase="RECONCILE_SAVED_WOOL_STAGE";
                error=""; status(f); return;
            }
            woolStageRecoveredFromStatus=false; resetWoolStairRoute();
            sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0;
            LOG.info("[PrinceAliRescue] RESTORED_SAVED_WOOL_STAGE from same-PID status; fresh frame confirms varp=20 balls=1 raw=0 shears=1 pos={}",f.pos);
        }
        int id=sourceItem, cost=unitCap(id);
        if(id==WOOL) { woolSourceTick(f); return; }
        if(id==WATER) { localWaterSourceTick(f); return; }
        if(id==BRONZE_BAR&&"BAR_F2P_MINE_SMELT".equals(geStage)) { localBronzeSourceTick(f); return; }
        if(id==BRONZE_BAR&&"BAR_SHANTAY_SHOP".equals(geStage)) { bronzeBarShantayTick(f); return; }
        if(id==SOFT_CLAY&&"SOFT_CLAY_LOCAL_MINE".equals(geStage)) { localSoftClayMineTick(f); return; }
        if(id==DYE&&"DYE_LOCAL_ONIONS".equals(geStage)) { localDyeOnionSourceTick(f); return; }
        if(id==ASHES&&("ASHES_LOCAL_BURN".equals(geStage)
            ||"ASHES_BUY_TINDERBOX".equals(geStage)
            ||"ASHES_GET_NORMAL_LOG".equals(geStage))) { localAshesSourceTick(f); return; }
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
            String beerOption=f.options.contains("A glass of your finest ale please.|")
                ?"A glass of your finest ale please.":"Could I buy a beer please?";
            if(f.options.contains(beerOption)) {
                if(Rs2Dialogue.clickOption(beerOption))
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
    private void bronzeBarShantayTick(Frame f) {
        if(FREE_TO_PLAY_ACCOUNT) {
            hold(f,"F2P source unavailable: Shantay Trade recreated the membership prompt; use a free bronze-bar source");
            return;
        }
        if(f.game!=GameState.LOGGED_IN||f.quest!=QuestState.IN_PROGRESS||f.varp!=20
            ||f.pos==null||f.pos.getPlane()!=0||f.count(KEY_PRINT)<=0) {
            hold(f,"Shantay bronze-bar route lost its live quest/key-print/plane precondition game="+
                f.game+" quest="+f.quest+" varp="+f.varp+" keyPrint="+f.count(KEY_PRINT)+" pos="+f.pos);
            return;
        }
        if(f.count(BRONZE_BAR)>=sourceGoal) {
            if(f.shop) { Rs2Shop.closeShop(); set("SOURCE_SHOP_CLOSE",f,6000,0,null); return; }
            if(f.bank) { closeIfOpen(f); return; }
            sourceItem=sourceGoal=sourceAttempts=sourceSpent=0; geStage=""; sourceShopOpenedAt=0;
            phase="SOURCE_COMPLETE_BRONZE_BAR_SHANTAY_SHOP"; status(f); return;
        }
        if(System.currentTimeMillis()-sourceStartedAt>360000) {
            hold(f,"Shantay bronze-bar source exceeded six minutes coins="+f.count(COINS)+
                " pos="+f.pos+" attempts="+sourceAttempts); return;
        }
        if(Rs2Inventory.isFull()) {
            hold(f,"Inventory full before the one-bar Shantay purchase; preserving unclassified items"); return;
        }
        if(f.shop&&f.count(COINS)>SHANTAY_BAR_COIN_CAP) {
            Rs2Shop.closeShop(); set("SOURCE_SHOP_CLOSE",f,6000,0,null); return;
        }
        if(f.count(COINS)>SHANTAY_BAR_COIN_CAP) {
            WorldPoint bankPoint=nearestBank(f.pos);
            if(f.pos.distanceTo(bankPoint)>8) { walk(f,bankPoint,"TO_BAR_COIN_STAGING_BANK"); return; }
            if(!f.bank) {
                if(Rs2Bank.openBank()) set("OPEN_BANK",f,15000,0,null);
                else hold(f,"Could not open nearby bank to cap Shantay purchase cash");
                return;
            }
            int excess=f.count(COINS)-SHANTAY_BAR_COIN_CAP;
            if(Rs2Bank.depositX(COINS,excess)) set("BAR_SHANTAY_STAGE_COINS",f,7000,COINS,null);
            else hold(f,"Exact coin staging deposit rejected amount="+excess+" coins="+f.count(COINS));
            return;
        }
        if(f.count(COINS)<=0) {
            hold(f,"No carried coins for the one-bar Shantay purchase; refusing an unpriced attempt"); return;
        }
        if(f.bank) { closeIfOpen(f); return; }
        if(shantayReachableTileRecoveryUsed&&shantayReachableTradeUsed&&!f.shop&&!shantayPromoRetryActive) {
            hold(f,"Reachable-tile Shantay Trade already dispatched without shop-open proof; no repeat"); return;
        }
        if(shantayApproachTarget!=null) {
            shantayReachableInteractionTick(f); return;
        }
        if(shantayReachableTileRecoveryUsed&&!shantayReachableTradeUsed&&!shantayPromoRetryActive) {
            hold(f,"Reachable Shantay recovery lost its persisted interaction target; no new Trade"); return;
        }
        if(f.pos.distanceTo(SHANTAY_SHOP_POS)>8) {
            walk(f,SHANTAY_SHOP_POS,"TO_BRONZE_BAR_SHANTAY_SHOP"); return;
        }
        net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel npc=Rs2Shop.getNearestShopNpc("Shantay",true);
        if(npc==null) {
            hold(f,"Shantay waypoint reached but no live Trade-capable Shantay NPC; pos="+f.pos+
                " waypoint="+SHANTAY_SHOP_POS); return;
        }
        WorldPoint npcTile=npc.getWorldLocation();
        if(npcTile==null||npcTile.getPlane()!=f.pos.getPlane()) {
            hold(f,"Live Shantay NPC has no usable same-plane tile id="+npc.getId()+" name="+npc.getName()); return;
        }
        int npcTileDistance=f.pos.distanceTo(npcTile);
        if(npcTileDistance>6) {
            walk(f,npcTile,"TO_SHANTAY_TRADE_NPC"); return;
        }
        if(!f.shop) {
            if(shantayOpenRetryUsed) {
                hold(f,"Single adjacent Shantay Trade retry already used without shop-open proof; no repeat"); return;
            }
            LOG.info("[PrinceAliRescue] SHANTAY_SHOP_OPEN_DISPATCH npcId={} tile={} tileDistance={} coins={} cap={}",
                npc.getId(),npcTile,npcTileDistance,f.count(COINS),SHANTAY_BAR_COIN_CAP);
            if(Rs2Shop.openShop("Shantay",true)) set("BAR_SHANTAY_OPEN",f,10000,BRONZE_BAR,null);
            else hold(f,"Microbot rejected Shantay Trade dispatch npcId="+npc.getId()+" tile="+npc.getWorldLocation());
            return;
        }
        if(Rs2Shop.shopItems==null||Rs2Shop.shopItems.isEmpty()) {
            if(sourceShopOpenedAt>0&&System.currentTimeMillis()-sourceShopOpenedAt<5000) {
                phase="WAIT_SHANTAY_SHOP_STOCK"; status(f); return;
            }
            hold(f,"Shantay shop opened but live stock list is unavailable; no purchase attempted"); return;
        }
        if(!Rs2Shop.hasMinimumStock(BRONZE_BAR,1)) {
            hold(f,"Shantay live stock lacks bronze bar 2349; stockList="+Rs2Shop.shopItems+
                " coins="+f.count(COINS)+"; no purchase attempted"); return;
        }
        if(sourceAttempts>0) {
            hold(f,"Shantay Buy-1 was already dispatched without inventory proof; no repeat; bar="+
                f.count(BRONZE_BAR)+" coins="+f.count(COINS)); return;
        }
        sourceAttempts=1;
        LOG.info("[PrinceAliRescue] SHANTAY_BUY_DISPATCH id={} qty=1 liveStock=true coins={} cap={} npcId={} npcTile={} tileDistance={}",
            BRONZE_BAR,f.count(COINS),SHANTAY_BAR_COIN_CAP,npc.getId(),npcTile,npcTileDistance);
        if(Rs2Shop.buyItem(BRONZE_BAR,"1")) set("BAR_SHANTAY_BUY",f,12000,BRONZE_BAR,null);
        else hold(f,"Shantay Buy-1 dispatch rejected; bar="+f.count(BRONZE_BAR)+" coins="+f.count(COINS));
    }

    private void shantayReachableInteractionTick(Frame f) {
        if(FREE_TO_PLAY_ACCOUNT) { hold(f,"F2P account: Shantay source is excluded after membership rejection"); return; }
        WorldPoint target=shantayApproachTarget;
        if(shantayReachableTradeUsed&&!shantayPromoRetryActive) {
            hold(f,"Shantay Trade already dispatched without shop proof; no repeat"); return;
        }
        if(shantayPromoRetryActive&&shantayPromoRetryUsed) {
            hold(f,"Single post-membership-popup Shantay Trade already dispatched; no repeat"); return;
        }
        if(target==null||f.pos==null||target.getPlane()!=f.pos.getPlane()) {
            hold(f,"Reachable Shantay interaction target missing or wrong plane target="+target+" player="+f.pos); return;
        }
        if(f.shop) {
            shantayApproachTarget=null; phase="RECOVERED_SHANTAY_SHOP_OPEN_DURING_APPROACH"; status(f); return;
        }
        if(!f.pos.equals(target)) {
            if(f.inDialogue||f.continuePrompt||!f.options.isEmpty()) {
                hold(f,"Unexpected dialogue before reachable Shantay tile approach: "+f.options); return;
            }
            WorldPoint before=f.pos;
            boolean arrived=Rs2Walker.walkTo(target,0);
            WorldPoint after=Rs2Player.getWorldLocation();
            if(after!=null) f.pos=after;
            LOG.info("[PrinceAliRescue] SHANTAY_REACHABLE_TILE_WALK_DISPATCH target={} before={} walkerArrived={} after={}",
                target,before,arrived,after);
            set("WALK_SHANTAY_INTERACTION_TILE",f,20000,0,target);
            return;
        }
        net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel npc=Rs2Shop.getNearestShopNpc("Shantay",true);
        if(npc==null||npc.getId()!=4642||npc.getWorldLocation()==null
            ||f.shantayNpcId!=4642||!npc.getWorldLocation().equals(f.shantayNpcTile)
            ||f.widgetSelected||f.inDialogue||f.continuePrompt||!f.options.isEmpty()) {
            hold(f,"Fresh reachable-tile Shantay precondition failed player="+f.pos+
                " npc="+(npc==null?"null":npc.getId()+"@"+npc.getWorldLocation())+
                " observedNpc="+f.shantayNpcId+" observedTile="+f.shantayNpcTile+
                " selected="+f.widgetSelected+" dialogue="+f.inDialogue); return;
        }
        NPCComposition composition=npc.getComposition();
        String[] actions=composition==null?null:composition.getActions();
        boolean trade=actions!=null&&java.util.Arrays.stream(actions).anyMatch("Trade"::equalsIgnoreCase);
        int areaDistance=f.shantayNpcAreaDistance;
        boolean los=f.shantayNpcLos;
        if(!trade||areaDistance>1||!los) {
            hold(f,"Fresh Shantay is not interactable from verified reachable tile target="+target+
                " player="+f.pos+" npc="+npc.getWorldLocation()+" areaDistance="+areaDistance+
                " los="+los+" baseActions="+java.util.Arrays.toString(actions)); return;
        }
        boolean postPopupRetry=shantayPromoRetryActive;
        shantayApproachTarget=null; shantayReachableTradeUsed=true;
        if(postPopupRetry) { shantayPromoRetryUsed=true; shantayPromoRetryActive=false; }
        LOG.info("[PrinceAliRescue] SHANTAY_REACHABLE_TRADE_DISPATCH npcId={} tile={} player={} areaDistance={} los={} actions={} widgetSelected={} coins={} keyPrint={} postPopupRetry={}",
            npc.getId(),npc.getWorldLocation(),f.pos,areaDistance,los,java.util.Arrays.toString(actions),
            f.widgetSelected,f.count(COINS),f.count(KEY_PRINT),postPopupRetry);
        if(Rs2Npc.interact(npc,"Trade")) set("BAR_SHANTAY_REACHABLE_TRADE",f,12000,BRONZE_BAR,null);
        else hold(f,"Trade dispatch rejected from verified reachable Shantay tile; no repeat");
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
            resetWoolStairRoute();
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

    private void localDyeOnionSourceTick(Frame f) {
        if(System.currentTimeMillis()-sourceStartedAt>360000) {
            hold(f,"Local yellow-dye source exceeded six minutes; onions="+f.count(ONION)
                +" coins="+f.count(COINS)); return;
        }
        if(f.count(DYE)>=sourceGoal) {
            sourceItem=sourceGoal=sourceAttempts=0; geStage="";
            phase="LOCAL_YELLOW_DYE_COMPLETE"; status(f); return;
        }
        if(f.count(COINS)<5) {
            if(!f.bank) {
                WorldPoint bankPoint=nearestBank(f.pos);
                if(f.pos==null||f.pos.distanceTo(bankPoint)>8) walk(f,bankPoint,"TO_DYE_FEE_BANK");
                else if(Rs2Bank.openBank()) set("OPEN_BANK",f,12000,0,null);
                else hold(f,"Bank open rejected while sourcing Aggie's 5-coin dye fee");
                return;
            }
            int deficit=5-f.count(COINS);
            if(!Rs2Bank.hasBankItem(COINS,deficit)) {
                hold(f,"Aggie's yellow-dye fee unavailable; need 5 coins, carried="+f.count(COINS)); return;
            }
            if(!Rs2Bank.hasWithdrawAsItem()) {
                if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                else hold(f,"Coin withdraw mode rejected for Aggie's dye fee");
            } else if(Rs2Bank.withdrawDeficit(COINS,5)) set("WITHDRAW",f,7000,COINS,null);
            else hold(f,"Five-coin dye-fee withdrawal rejected");
            return;
        }
        if(f.count(ONION)<2) {
            if(f.bank) { closeIfOpen(f); return; }
            if(Rs2Inventory.isFull()) { hold(f,"No inventory slot for quest onions; refusing to discard carried items"); return; }
            if(f.pos==null||f.pos.getPlane()!=0||f.pos.distanceTo(FRED_ONION_FIELD)>5) {
                walk(f,FRED_ONION_FIELD,"TO_YELLOW_DYE_ONIONS"); return;
            }
            Rs2TileObjectModel onion=object(ONION_IDS[0],FRED_ONION_FIELD,17);
            if(onion==null) onion=object(ONION_IDS[1],FRED_ONION_FIELD,17);
            if(onion==null) { hold(f,"No live onion plant 3366/5538 at Fred's field "+FRED_ONION_FIELD); return; }
            net.runelite.api.ObjectComposition composition=onion.getObjectComposition();
            String action=null;
            if(composition!=null&&composition.getActions()!=null) {
                List<String> actions=java.util.Arrays.asList(composition.getActions());
                if(actions.contains("Pick")) action="Pick";
                else if(actions.contains("Take")) action="Take";
            }
            if(action==null) { hold(f,"Live onion plant has no verified Pick/Take action id="+onion.getId()); return; }
            if(sourceAttempts>=2) { hold(f,"Two onion-pick attempts without two verified onions; count="+f.count(ONION)); return; }
            Map<WorldPoint,Integer> reachable=Rs2Tile.getReachableTilesFromTile(f.pos,10);
            WorldPoint approach=null;
            if(reachable!=null) for(WorldPoint tile:reachable.keySet()) {
                if(tile.getPlane()==onion.getWorldLocation().getPlane()
                    &&tile.distanceTo(onion.getWorldLocation())<=1&&Rs2Tile.isWalkable(tile)
                    &&(approach==null||f.pos.distanceTo(tile)<f.pos.distanceTo(approach))) approach=tile;
            }
            if(approach==null) {
                final WorldPoint onionTile=onion.getWorldLocation();
                net.runelite.api.TileObject gate=Rs2GameObject.getAll(o->o!=null,f.pos,8).stream()
                    .filter(o->Rs2GameObject.hasAction(o,"Open"))
                    .filter(o->{String n=Rs2GameObject.getCompositionName(o).orElse("").toLowerCase();
                        return n.contains("gate")||n.contains("door");})
                    .filter(o->o.getWorldLocation()!=null&&o.getWorldLocation().getPlane()==f.pos.getPlane()
                        &&o.getWorldLocation().distanceTo(onionTile)<=5)
                    .min(java.util.Comparator.comparingInt(o->f.pos.distanceTo(o.getWorldLocation())))
                    .orElse(null);
                if(gate!=null) {
                    LOG.info("[PrinceAliRescue] ONION_GATE_OPEN_DISPATCH id={} tile={} name={} player={} onion={}",
                        gate.getId(),gate.getWorldLocation(),Rs2GameObject.getCompositionName(gate).orElse(""),f.pos,
                        onion.getWorldLocation());
                    if(Rs2GameObject.interact(gate,"Open")) set("OPEN_ONION_GATE",f,9000,gate.getId(),gate.getWorldLocation());
                    else hold(f,"Open rejected for live onion-field gate id="+gate.getId()+" tile="+gate.getWorldLocation());
                    return;
                }
                hold(f,"No reachable tile adjacent to onion id="+onion.getId()+" tile="+onion.getWorldLocation()
                    +" and no nearby live closed gate; player="+f.pos+" reachable="+(reachable==null?0:reachable.size()));
                return;
            }
            if(f.pos.distanceTo(approach)>0) {
                long now=System.currentTimeMillis();
                if(onionLastPosition==null||!onionLastPosition.equals(f.pos)) {
                    onionLastPosition=f.pos; onionLastProgressAt=now;
                }
                if(now-onionLastProgressAt>20000||onionStepAttempts>=12) {
                    Rs2Walker.clearWalkingRoute("prince-ali-onion-approach-no-progress");
                    hold(f,"Bounded onion walkStep made no verified progress attempts="+onionStepAttempts
                        +" player="+f.pos+" approach="+approach+" onion="+onion.getWorldLocation()); return;
                }
                if(now-onionLastStepAt<1600||Rs2Player.isMoving()) { phase="WAIT_ONION_APPROACH_STEP"; status(f); return; }
                onionApproach=approach;
                WalkerState state=Rs2Walker.walkStep(approach,0);
                onionLastStepAt=now; onionStepAttempts++;
                LOG.info("[PrinceAliRescue] ONION_APPROACH_STEP state={} attempt={} player={} approach={} plant={} dist={}",
                    state,onionStepAttempts,f.pos,approach,onion.getWorldLocation(),f.pos.distanceTo(approach));
                if(state==WalkerState.UNREACHABLE||state==WalkerState.EXIT) {
                    Rs2Walker.clearWalkingRoute("prince-ali-onion-approach-"+state);
                    hold(f,"Onion approach walkStep "+state+" player="+f.pos+" approach="+approach
                        +" plant="+onion.getWorldLocation()); return;
                }
                phase="WAIT_ONION_APPROACH_STEP"; status(f); return;
            }
            onionApproach=null; onionLastPosition=null; onionStepAttempts=0;
            sourceAttempts++;
            LOG.info("[PrinceAliRescue] DYE_ONION_PICK_DISPATCH id={} tile={} action={} player={} dist={} reachable={} count={}",
                onion.getId(),onion.getWorldLocation(),action,f.pos,f.pos.distanceTo(onion.getWorldLocation()),
                onion.isReachable(),f.count(ONION));
            if(onion.click(action)) set("PICK_DYE_ONION",f,9000,ONION,onion.getWorldLocation());
            else hold(f,"Onion pick rejected id="+onion.getId()+" tile="+onion.getWorldLocation());
            return;
        }
        if(closeIfOpen(f)) return;
        sourceItem=sourceGoal=sourceAttempts=0; geStage="";
        phase="DYE_ONIONS_READY_FOR_AGGIE"; status(f);
    }

    private boolean recoverObservedUnavailableWaterQuote(Frame f) {
        if(waterQuoteRecovered||!"HOLD".equals(phase)
            ||!error.startsWith("GE quote unavailable/above 1000gp cumulative cap id=1929 quote=0 deficit=")
            ||sourceItem!=WATER||sourceGoal!=1||!"PREPARE".equals(geStage)
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.count(WATER)>0||f.count(COINS)<2) return false;
        held=false; error=""; geStage="WATER_LOCAL_SOURCE";
        waterBucketPurchaseAttempts=0; waterFillAttempts=0; resetWaterApproach();
        sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0;
        waterQuoteRecovered=true; phase="RECOVER_WATER_FROM_AL_KHARID_SOURCE";
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_ZERO_WATER_QUOTE; using one bounded local bucket/fountain cycle coins={} pos={}",
            f.count(COINS),f.pos);
        return true;
    }

    private void localBronzeSourceTick(Frame f) {
        if(closeIfOpen(f)) return;
        if(f.memberPromoVisible) return;
        if(bronzeSource==null) bronzeSource=new BronzeBarSource(5);
        BronzeBarSource.Result result=bronzeSource.tick(ownsInput==null||ownsInput.getAsBoolean());
        if(!result.detail.equals(lastBronzeSourceDetail)) {
            LOG.info("[PrinceAliRescue] F2P_BRONZE {}",result);
            lastBronzeSourceDetail=result.detail;
        }
        if(result.outcome==BronzeBarSource.Outcome.HOLD) { hold(f,"F2P bronze source: "+result.detail); return; }
        if(result.outcome==BronzeBarSource.Outcome.COMPLETE) {
            if(f.count(BRONZE_BAR)<1) { phase="WAIT_BRONZE_NEXT_FRAME_INVENTORY"; status(f); return; }
            sourceItem=sourceGoal=0; geStage=""; bronzeSource=null;
            phase="F2P_BRONZE_SOURCE_COMPLETE"; status(f); return;
        }
        phase="F2P_BRONZE_"+result.outcome; status(f);
    }
    private boolean handleGraveRecovery(Frame f) {
        if(f.game!=GameState.LOGGED_IN||f.pos==null) return false;
        if(held&&!graveRecoveryStage.isEmpty()&&f.count(KEY_PRINT)==0&&f.count(PASTE)==0
            &&f.pos.distanceTo(new WorldPoint(3222,3219,0))<=40&&graveRecoveryRuns<2) {
            LOG.warn("[PrinceAliRescue] GRAVE_RECOVERY_SECOND_RESPAWN priorStage={} remaining={} resetting one recovery attempt with immediate loot-to-escape",graveRecoveryStage,Rs2Death.getGraveTimeRemaining());
            graveRecoveryRuns++; held=false; error=""; pending=null;
            graveRecoveryStage="APPROACH"; graveRecoveryAfterEscapeError="";
            WorldPoint latest=Rs2Death.getLastDeathLocation();
            if(latest!=null) graveRecoveryTarget=latest;
        }
        if(graveRecoveryStage.isEmpty()) {
            if(!held||!error.startsWith("F2P bronze source: HP fell during ")
                ||f.varp!=20||f.count(KEY_PRINT)>0||f.count(PASTE)>0
                ||f.pos.distanceTo(new WorldPoint(3222,3219,0))>40) return false;
            graveRecoveryTarget=Rs2Death.getLastDeathLocation();
            if(graveRecoveryTarget==null) graveRecoveryTarget=new WorldPoint(3296,3313,0);
            LOG.warn("[PrinceAliRescue] GRAVE_RECOVERY_START userConfirmedDeath=true target={} hasGrave={} remaining={} inventory={}",
                graveRecoveryTarget,Rs2Death.hasGrave(),Rs2Death.getGraveTimeRemaining(),f.items);
            held=false; error=""; pending=null; bronzeSource=null;
            graveRecoveryRuns++;
            graveRecoveryStage="APPROACH";
        }
        if("WAIT_OPEN".equals(graveRecoveryStage)&&f.count(KEY_PRINT)>0&&f.count(PASTE)>0
            &&f.count(BLONDE_WIG)>0&&f.count(436)>0&&f.count(438)>0) {
            // Some grave Loot interactions return free items directly, without
            // opening the fee interface. Inventory restoration is stronger proof.
            boolean remainingGrave=Rs2Death.hasGrave();
            LOG.info("[PrinceAliRescue] GRAVE_DIRECT_LOOT_PROVED questItemsRestored=true hasRemainingGrave={} inventory={}",remainingGrave,f.items);
            held=false; error=""; graveRecoveryStage="ESCAPE";
            graveRecoveryDeadline=System.currentTimeMillis()+240000;
            phase="RECOVER_GRAVESTONE_RETURN_TO_BANK";
            Rs2Death.closeInterfaces(); Rs2Player.toggleRunEnergy(true);
            Rs2Walker.walkTo(BankLocation.AL_KHARID.getWorldPoint(),3);
            return true;
        }
        if(held) {
            if(f.pos.distanceTo(graveRecoveryTarget)<=40) {
                graveRecoveryAfterEscapeError=error;
                held=false; error=""; graveRecoveryStage="ESCAPE";
                graveRecoveryDeadline=System.currentTimeMillis()+240000;
                phase="GRAVE_FAILURE_RETREAT_TO_BANK";
                Rs2Death.closeInterfaces(); Rs2Player.toggleRunEnergy(true);
                Rs2Walker.walkTo(BankLocation.AL_KHARID.getWorldPoint(),3);
            }
            return true;
        }
        long now=System.currentTimeMillis();
        if("APPROACH".equals(graveRecoveryStage)) {
            graveRecoveryStage="WAIT_APPROACH"; graveRecoveryDeadline=now+240000;
            phase="RECOVER_GRAVESTONE_APPROACH"; status(f);
            Rs2Player.toggleRunEnergy(true);
            boolean arrived=Rs2Walker.walkTo(graveRecoveryTarget,3);
            LOG.info("[PrinceAliRescue] GRAVE_WALK_RETURN arrived={} observed={} target={}",arrived,Rs2Player.getWorldLocation(),graveRecoveryTarget);
            return true;
        }
        if("WAIT_APPROACH".equals(graveRecoveryStage)) {
            var grave=Rs2Death.getGrave();
            if(grave!=null&&grave.getWorldLocation()!=null&&f.pos.distanceTo(grave.getWorldLocation())<=8) {
                graveRecoveryStage="WAIT_OPEN"; graveRecoveryDeadline=now+12000;
                phase="RECOVER_GRAVESTONE_OPEN";
                LOG.info("[PrinceAliRescue] GRAVE_OPEN_DISPATCH npc={} tile={} remaining={}",grave.getId(),grave.getWorldLocation(),Rs2Death.getGraveTimeRemaining());
                // Loot may transfer free items directly. Do not block five seconds
                // waiting for a fee window while standing next to aggressive NPCs.
                grave.click("Loot"); return true;
            }
            if(now>graveRecoveryDeadline) hold(f,"Grave approach unproved; target="+graveRecoveryTarget+" hasGrave="+Rs2Death.hasGrave());
            return true;
        }
        if("WAIT_OPEN".equals(graveRecoveryStage)) {
            if(Rs2Death.isGraveOpen()) {
                var free=Rs2Death.getGraveFreeItems();
                var paid=Rs2Death.getGravePaidItems();
                LOG.info("[PrinceAliRescue] GRAVE_OPEN_PROVED free={} paid={} fee={}",free,paid,Rs2Death.getGraveFee());
                if(free.isEmpty()) { hold(f,"Opened grave has no free items; inspect paid/empty grave before next action"); return true; }
                graveRecoveryExpected=new HashMap<>(f.items);
                for(var item:free) graveRecoveryExpected.merge(item.getId(),item.getQuantity(),Integer::sum);
                graveRecoveryStage="WAIT_LOOT"; graveRecoveryDeadline=now+15000;
                phase="RECOVER_GRAVESTONE_LOOT_FREE";
                Rs2Death.lootGraveFreeItems(); return true;
            }
            if(now>graveRecoveryDeadline) hold(f,"Grave Loot click unproved; no repeated interaction");
            return true;
        }
        if("WAIT_LOOT".equals(graveRecoveryStage)) {
            boolean restored=graveRecoveryExpected.entrySet().stream().allMatch(e->f.count(e.getKey())>=e.getValue());
            if(restored) {
                LOG.info("[PrinceAliRescue] GRAVE_FREE_ITEMS_PROVED inventory={} expected={}",f.items,graveRecoveryExpected);
                if(Rs2Death.isGraveOpen()&&!Rs2Death.getGravePaidItems().isEmpty()) {
                    hold(f,"Free grave items recovered; paid items remain, fee="+Rs2Death.getGraveFee()); return true;
                }
                Rs2Death.closeInterfaces();
                graveRecoveryStage="ESCAPE"; graveRecoveryDeadline=now+240000;
                phase="RECOVER_GRAVESTONE_RETURN_TO_BANK";
                Rs2Player.toggleRunEnergy(true);
                Rs2Walker.walkTo(BankLocation.AL_KHARID.getWorldPoint(),3);
                return true;
            }
            if(now>graveRecoveryDeadline) hold(f,"Grave free-item recovery inventory proof incomplete; expected="+graveRecoveryExpected+" actual="+f.items);
            return true;
        }
        if("ESCAPE".equals(graveRecoveryStage)) {
            if(f.pos.distanceTo(BankLocation.AL_KHARID.getWorldPoint())<=8) {
                graveRecoveryStage=""; bankCleanupRequired=true; bronzeSource=null;
                held=false; error=""; phase="GRAVE_RECOVERY_AT_BANK";
                LOG.info("[PrinceAliRescue] GRAVE_RECOVERY_AT_BANK inventory={} pos={}",f.items,f.pos);
                if(!graveRecoveryAfterEscapeError.isEmpty()) {
                    String reason=graveRecoveryAfterEscapeError; graveRecoveryAfterEscapeError="";
                    hold(f,"Safely retreated from incomplete grave recovery: "+reason);
                }
                return true;
            }
            if(now>graveRecoveryDeadline) hold(f,"Recovered grave items but safe-bank arrival unproved");
            return true;
        }
        return true;
    }
    private boolean recoverBronzeMineDamage(Frame f) {
        if(!held||!error.startsWith("F2P bronze source: HP fell during ")
            ||sourceItem!=BRONZE_BAR||f.game!=GameState.LOGGED_IN||f.pos==null
            ||f.pos.getPlane()!=0||f.pos.distanceTo(new WorldPoint(3300,3307,0))>40) return false;
        LOG.warn("[PrinceAliRescue] RETREAT_MINE_TO_BANK hpPercent={} copper={} tin={} player={}; current mining attempt ended, no repeat",
            f.health,f.count(436),f.count(438),f.pos);
        held=false; error=""; pending=null; bronzeSource=null; bankCleanupRequired=true;
        prepareBankInventory(f);
        return true;
    }
    private boolean keepForCurrentQuest(Frame f,int id) {
        if(id==COINS||id==BEER||id==ROPE||id==SKIRT||id==BLONDE_WIG||id==PASTE
            ||id==KEY_PRINT||id==BRONZE_KEY||id==BRONZE_BAR) return true;
        if(f.count(BRONZE_KEY)==0&&(id==1265||id==436||id==438)) return true;
        if(f.count(BLONDE_WIG)==0&&(id==WIG||id==WOOL||id==RAW_WOOL||id==SHEARS||id==DYE||id==ONION)) return true;
        if(f.count(PASTE)==0&&(id==REDBERRIES||id==ASHES||id==WATER||id==EMPTY_BUCKET||id==FLOUR
            ||id==TINDERBOX||id==LOGS||id==BRONZE_AXE)) return true;
        if(!keySubmitted&&f.count(KEY_PRINT)==0&&f.count(BRONZE_KEY)==0&&(id==SOFT_CLAY||id==CLAY||id==WATER||id==EMPTY_BUCKET)) return true;
        // Preserve edible supplies; equipped gear is outside inventory deposits.
        return Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>)() -> {
            net.runelite.api.ItemComposition item=Microbot.getClient().getItemDefinition(id);
            if(item==null||item.getInventoryActions()==null) return false;
            return java.util.Arrays.stream(item.getInventoryActions()).anyMatch("Eat"::equalsIgnoreCase);
        });
    }
    private void prepareBankInventory(Frame f) {
        if(f.game!=GameState.LOGGED_IN||f.pos==null) return;
        if(!f.bank) {
            WorldPoint bank=nearestBank(f.pos);
            if(f.pos.distanceTo(bank)>8) { walk(f,bank,"TO_INVENTORY_PREP_BANK"); return; }
            if(Rs2Bank.openBank()) set("OPEN_BANK",f,15000,0,null);
            else hold(f,"Inventory preparation bank open rejected");
            return;
        }
        if(!f.bankContentsAvailable) { hold(f,"Inventory preparation requires live bank contents"); return; }
        for(Map.Entry<Integer,Integer> entry:f.items.entrySet()) {
            int id=entry.getKey(),qty=entry.getValue();
            int deposit=id==COINS?Math.max(0,qty-100):keepForCurrentQuest(f,id)?0:qty;
            if(deposit==0) continue;
            LOG.info("[PrinceAliRescue] BANK_PREP_DEPOSIT id={} amount={} beforeInventory={} beforeBank={}",id,deposit,qty,f.bankItems.getOrDefault(id,0));
            if(Rs2Bank.depositX(id,deposit)) set("DEPOSIT_UNNEEDED",f,7000,id,null);
            else hold(f,"Bank preparation deposit rejected id="+id+" amount="+deposit);
            return;
        }
        bankCleanupRequired=false;
        phase="BANK_PREPARED_FOR_CURRENT_QUEST";
        LOG.info("[PrinceAliRescue] BANK_PREPARED_FOR_CURRENT_QUEST retained={} coins={}",f.items,f.count(COINS));
        status(f);
    }
    private boolean recoverFreeBronzeRoute(Frame f) {
        if(!FREE_TO_PLAY_ACCOUNT||!held||sourceItem!=BRONZE_BAR||sourceGoal!=1
            ||!"BAR_SHANTAY_SHOP".equals(geStage)||!bankInspected
            ||f.game!=GameState.LOGGED_IN||f.quest!=QuestState.IN_PROGRESS||f.varp!=20
            ||f.pos==null||f.pos.getPlane()!=0||f.count(KEY_PRINT)<=0
            ||f.memberPromoVisible||f.shop||f.bank||f.inDialogue||f.continuePrompt
            ||!f.options.isEmpty()||f.widgetSelected||pending!=null) return false;
        if(!error.startsWith("Unproved BAR_SHANTAY_")&&!error.startsWith("Reload during BAR_SHANTAY_")
            &&!error.startsWith("F2P source unavailable:")&&!error.startsWith("F2P account: Shantay")) return false;
        LOG.info("[PrinceAliRescue] REPLAN_F2P_BRONZE source=Shantay excluded=membershipRejected player={} bar={} coins={}; mine copper/tin then smelt; no Shantay retry",
            f.pos,f.count(BRONZE_BAR),f.count(COINS));
        held=false; error=""; phase="RECHECK_BANK_BEFORE_FREE_BRONZE_SOURCE"; geStage="";
        sourceItem=sourceGoal=0; bankInspected=false;
        restoredInFlightAction=""; shantayApproachTarget=null;
        sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0;
        return true;
    }
    private boolean recoverObservedUnavailableBronzeBarQuote(Frame f) {
        if(FREE_TO_PLAY_ACCOUNT) return false;
        if(!"HOLD".equals(phase)
            ||!error.equals("GE quote unavailable/above 1000gp cumulative cap id=2349 quote=0 deficit=1 reserved=0")
            ||sourceItem!=BRONZE_BAR||sourceGoal!=1||!"PREPARE".equals(geStage)
            ||!bankInspected||f.game!=GameState.LOGGED_IN||f.quest!=QuestState.IN_PROGRESS
            ||f.varp!=20||f.pos==null||f.pos.getPlane()!=0||f.count(KEY_PRINT)<=0
            ||f.count(BRONZE_BAR)>0) return false;
        held=false; error=""; geStage="BAR_SHANTAY_SHOP"; sourceSpent=0; sourceAttempts=0;
        sourceStartedAt=System.currentTimeMillis(); sourceShopOpenedAt=0;
        phase="RECOVER_ZERO_BRONZE_BAR_QUOTE_TO_SHANTAY_SHOP";
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_ZERO_BRONZE_BAR_QUOTE; one-bar Shantay source with max carried cash={} keyPrint={} coins={} pos={}",
            SHANTAY_BAR_COIN_CAP,f.count(KEY_PRINT),f.count(COINS),f.pos);
        return true;
    }

    private boolean recoverObservedShantayOpenHold(Frame f) {
        if(FREE_TO_PLAY_ACCOUNT) return false;
        boolean exactHold="HOLD".equals(phase)&&error.startsWith("Unproved BAR_SHANTAY_OPEN;")
            &&pending!=null&&"BAR_SHANTAY_OPEN".equals(pending.action);
        boolean exactReload="HOLD_RELOAD_IN_FLIGHT".equals(phase)
            &&error.startsWith("Reload during BAR_SHANTAY_OPEN;")
            &&"BAR_SHANTAY_OPEN".equals(restoredInFlightAction)
            &&lastReloadHoldError.startsWith("Unproved BAR_SHANTAY_OPEN;");
        if((!exactHold&&!exactReload)||shantayOpenRetryUsed||sourceItem!=BRONZE_BAR||sourceGoal!=1
            ||!"BAR_SHANTAY_SHOP".equals(geStage)||!bankInspected
            ||f.game!=GameState.LOGGED_IN||f.quest!=QuestState.IN_PROGRESS||f.varp!=20
            ||f.pos==null||f.pos.getPlane()!=0||f.count(KEY_PRINT)<=0||f.count(BRONZE_BAR)>0
            ||f.count(COINS)<=0||f.count(COINS)>SHANTAY_BAR_COIN_CAP) return false;
        if(f.shop) {
            held=false; error=""; pending=null; restoredInFlightAction="";
            sourceShopOpenedAt=System.currentTimeMillis(); phase="RECOVERED_SHANTAY_SHOP_OPEN_BY_FRESH_STATE";
            LOG.info("[PrinceAliRescue] RECOVERED_SHANTAY_SHOP_OPEN_BY_FRESH_STATE coins={} pos={}",f.count(COINS),f.pos);
            return true;
        }
        if(f.inDialogue||f.continuePrompt||!f.options.isEmpty()) return false;
        net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel npc=Rs2Shop.getNearestShopNpc("Shantay",true);
        if(npc==null||npc.getId()!=4642||npc.getWorldLocation()==null
            ||npc.getWorldLocation().getPlane()!=f.pos.getPlane()||f.pos.distanceTo(npc.getWorldLocation())>2) return false;
        NPCComposition composition=npc.getTransformedComposition();
        String[] actions=composition==null?null:composition.getActions();
        boolean trade=actions!=null&&java.util.Arrays.stream(actions).anyMatch("Trade"::equalsIgnoreCase);
        if(!trade) return false;
        shantayOpenRetryUsed=true; held=false; error=""; pending=null; restoredInFlightAction="";
        LOG.info("[PrinceAliRescue] SHANTAY_ADJACENT_TRADE_RETRY_DISPATCH npcId={} tile={} player={} tileDistance={} actions={} coins={} keyPrint={} retryUsed={}",
            npc.getId(),npc.getWorldLocation(),f.pos,f.pos.distanceTo(npc.getWorldLocation()),
            java.util.Arrays.toString(actions),f.count(COINS),f.count(KEY_PRINT),shantayOpenRetryUsed);
        if(Rs2Npc.interact(npc,"Trade")) {
            set("BAR_SHANTAY_OPEN_RETRY",f,12000,BRONZE_BAR,null);
            return true;
        }
        hold(f,"Adjacent Shantay Trade retry rejected npcId="+npc.getId()+" tile="+npc.getWorldLocation()+
            " player="+f.pos+" actions="+java.util.Arrays.toString(actions));
        return true;
    }

    private boolean recoverObservedShantayReachableTileHold(Frame f) {
        if(FREE_TO_PLAY_ACCOUNT) return false;
        boolean exactHold="HOLD".equals(phase)&&error.startsWith("Unproved BAR_SHANTAY_OPEN_RETRY;");
        boolean exactReload="HOLD_RELOAD_IN_FLIGHT".equals(phase)
            &&error.startsWith("Reload during BAR_SHANTAY_OPEN_RETRY;");
        boolean exactTimerHold="HOLD".equals(phase)
            &&error.startsWith("Shantay bronze-bar source exceeded six minutes")
            &&shantayReachableTileRecoveryUsed&&!shantayReachableTileTimerRecoveryUsed
            &&!shantayReachableTradeUsed&&pending==null&&shantayApproachTarget!=null;
        if((!exactHold&&!exactReload&&!exactTimerHold)||!shantayOpenRetryUsed
            ||(!exactTimerHold&&shantayReachableTileRecoveryUsed)
            ||sourceItem!=BRONZE_BAR||sourceGoal!=1||!"BAR_SHANTAY_SHOP".equals(geStage)
            ||!bankInspected||f.game!=GameState.LOGGED_IN||f.quest!=QuestState.IN_PROGRESS||f.varp!=20
            ||f.pos==null||f.pos.getPlane()!=0||f.count(KEY_PRINT)<=0||f.count(BRONZE_BAR)>0
            ||f.count(COINS)<=0||f.count(COINS)>SHANTAY_BAR_COIN_CAP) return false;
        if(f.shop) {
            held=false; error=""; pending=null; restoredInFlightAction="";
            sourceShopOpenedAt=System.currentTimeMillis(); phase="RECOVERED_SHANTAY_SHOP_OPEN_BY_FRESH_STATE";
            LOG.info("[PrinceAliRescue] RECOVERED_SHANTAY_SHOP_OPEN_BY_FRESH_STATE after Trade retry coins={} pos={}",
                f.count(COINS),f.pos);
            return true;
        }
        if(f.inDialogue||f.continuePrompt||!f.options.isEmpty()||f.widgetSelected) return false;
        WorldPoint playerPos=f.pos;
        java.util.Set<WorldPoint> excluded=new java.util.HashSet<>();
        if(exactTimerHold) {
            // The earlier Trade attempts and the recovered current tile are not fresh alternatives.
            excluded.add(playerPos);
            excluded.add(new WorldPoint(3304,3123,0));
            excluded.add(new WorldPoint(3302,3124,0));
            if(shantayApproachTarget!=null) excluded.add(shantayApproachTarget);
        }
        WorldPoint target=Microbot.getClientThread().invoke(
            (java.util.function.Supplier<WorldPoint>)() -> findShantayReachableInteractionTile(playerPos,excluded));
        if(target==null) {
            shantayPathDiagnostic="no live reachable LOS interaction tile; player="+playerPos+
                " widgetSelected="+f.widgetSelected+" timerRecovery="+exactTimerHold+
                " excluded="+excluded;
            LOG.warn("[PrinceAliRescue] SHANTAY_REACHABLE_TILE_PLAN_FAILED {}",shantayPathDiagnostic);
            return false;
        }
        if(exactTimerHold) {
            sourceStartedAt=System.currentTimeMillis();
            shantayReachableTileTimerRecoveryUsed=true;
        }
        shantayReachableTileRecoveryUsed=true; shantayApproachTarget=target;
        held=false; error=""; pending=null; restoredInFlightAction="";
        phase="RESUME_SHANTAY_REACHABLE_INTERACTION_TILE";
        LOG.info("[PrinceAliRescue] RECOVERED_SHANTAY_HOLD_TO_REACHABLE_TILE player={} target={} distance={} timerRecovery={} timerRebased={} excluded={} priorDiagnostic={}; no Trade dispatched",
            playerPos,target,playerPos.distanceTo(target),exactTimerHold,shantayReachableTileTimerRecoveryUsed,
            excluded,shantayPathDiagnostic);
        return true;
    }

    private WorldPoint findShantayReachableInteractionTile(WorldPoint playerPos) {
        return findShantayReachableInteractionTile(playerPos,java.util.Collections.emptySet());
    }

    private WorldPoint findShantayReachableInteractionTile(WorldPoint playerPos, java.util.Set<WorldPoint> excluded) {
        Client c=Microbot.getClient();
        if(c==null||c.getLocalPlayer()==null||c.getTopLevelWorldView()==null
            ||playerPos==null||!playerPos.equals(c.getLocalPlayer().getWorldLocation())
            ||c.isWidgetSelected()) return null;
        net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel npc=Rs2Shop.getNearestShopNpc("Shantay",true);
        if(npc==null||npc.getId()!=4642||npc.getWorldLocation()==null||npc.getWorldArea()==null) return null;
        NPCComposition base=npc.getComposition();
        String[] actions=base==null?null:base.getActions();
        if(actions==null||java.util.Arrays.stream(actions).noneMatch("Trade"::equalsIgnoreCase)) return null;
        WorldArea area=npc.getWorldArea();
        java.util.HashMap<WorldPoint,Integer> reachable=Rs2Tile.getReachableTilesFromTile(playerPos,8);
        java.util.List<WorldPoint> candidates=new java.util.ArrayList<>();
        int minX=area.getX()-1, maxX=area.getX()+area.getWidth();
        int minY=area.getY()-1, maxY=area.getY()+area.getHeight();
        for(int x=minX;x<=maxX;x++) for(int y=minY;y<=maxY;y++) {
            boolean perimeter=x==minX||x==maxX||y==minY||y==maxY;
            if(!perimeter) continue;
            WorldPoint tile=new WorldPoint(x,y,area.getPlane());
            if(excluded!=null&&excluded.contains(tile)) continue;
            if(!reachable.containsKey(tile)||(!tile.equals(playerPos)&&!Rs2Tile.isWalkable(tile))) continue;
            if(new WorldArea(tile,1,1).hasLineOfSightTo(c.getTopLevelWorldView(),area)) candidates.add(tile);
        }
        return candidates.stream().min(java.util.Comparator
            .comparingInt((WorldPoint p)->reachable.getOrDefault(p,Integer.MAX_VALUE))
            .thenComparingInt(p->{
                int dx=p.getX()<area.getX()?area.getX()-p.getX():p.getX()>=area.getX()+area.getWidth()
                    ?p.getX()-(area.getX()+area.getWidth()-1):0;
                int dy=p.getY()<area.getY()?area.getY()-p.getY():p.getY()>=area.getY()+area.getHeight()
                    ?p.getY()-(area.getY()+area.getHeight()-1):0;
                return dx+dy;
            })
            .thenComparingInt(WorldPoint::getX)
            .thenComparingInt(WorldPoint::getY)).orElse(null);
    }

    private boolean recoverObservedShantayApproachAfterReload(Frame f) {
        if(FREE_TO_PLAY_ACCOUNT) return false;
        if(!"HOLD_RELOAD_IN_FLIGHT".equals(phase)
            ||!error.startsWith("Reload during WALK_TO_SHANTAY_TRADE_NPC;")
            ||!"WALK_TO_SHANTAY_TRADE_NPC".equals(restoredInFlightAction)
            ||sourceItem!=BRONZE_BAR||sourceGoal!=1||sourceAttempts!=0
            ||!"BAR_SHANTAY_SHOP".equals(geStage)||f.game!=GameState.LOGGED_IN
            ||f.quest!=QuestState.IN_PROGRESS||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.pos.distanceTo(SHANTAY_SHOP_POS)>8||f.count(KEY_PRINT)<=0
            ||f.count(BRONZE_BAR)>0||f.count(COINS)<=0||f.count(COINS)>SHANTAY_BAR_COIN_CAP) return false;
        net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel npc=Rs2Shop.getNearestShopNpc("Shantay",true);
        WorldPoint npcTile=npc==null?null:npc.getWorldLocation();
        if(npcTile==null||npcTile.getPlane()!=f.pos.getPlane()||f.pos.distanceTo(npcTile)>6) return false;
        held=false; error=""; pending=null; restoredInFlightAction="";
        phase="RECOVERED_SHANTAY_APPROACH_AFTER_RELOAD_WITHOUT_REPLAY";
        LOG.info("[PrinceAliRescue] RECOVERED_SHANTAY_APPROACH_AFTER_RELOAD_WITHOUT_REPLAY player={} npcId={} npcTile={} tileDistance={} coins={}",
            f.pos,npc.getId(),npcTile,f.pos.distanceTo(npcTile),f.count(COINS));
        return true;
    }

    private boolean recoverObservedWaterFountainWithoutDirectAction(Frame f) {
        if(waterCandidateHoldRecovered||!"HOLD".equals(phase)
            ||!error.startsWith("No live Fill/Use fountain, well, or sink found in Al Kharid courtyard;")
            ||sourceItem!=WATER||sourceGoal!=1||!"WATER_LOCAL_SOURCE".equals(geStage)
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.pos.distanceTo(ALKHARID_PALACE_COURTYARD)>4||f.count(EMPTY_BUCKET)!=1||f.count(WATER)>0) return false;
        held=false; error=""; waterCandidateHoldRecovered=true;
        phase="RESUME_BUCKET_USE_ON_OBSERVED_FOUNTAIN";
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_FOUNTAIN_WITHOUT_DIRECT_ACTION; fresh scene/inventory confirms bucket=1 water=0 player={} fountain-search resumes via item-on-object",f.pos);
        return true;
    }

    private void localWaterSourceTick(Frame f) {
        long now=System.currentTimeMillis();
        if(f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0) {
            hold(f,"Water material source requires live logged-in varp273=20 plane=0; game="+
                f.game+" varp="+f.varp+" pos="+f.pos); return;
        }
        if(now-sourceStartedAt>360000) { hold(f,"Local water source exceeded six minutes"); return; }
        if(f.count(WATER)>=sourceGoal) {
            if(f.shop) { Rs2Shop.closeShop(); set("WATER_SHOP_CLOSE",f,6000,0,null); return; }
            sourceItem=sourceGoal=sourceAttempts=sourceSpent=0; geStage="";
            waterBucketPurchaseAttempts=0; waterFillAttempts=0; waterQuoteRecovered=false;
            resetWaterApproach(); phase="LOCAL_WATER_COMPLETE"; status(f); return;
        }
        if(f.inDialogue||f.continuePrompt||!f.options.isEmpty()) {
            hold(f,"Unexpected dialogue during local water source: "+f.dialogue+" options="+f.options); return;
        }
        if(f.bank) { closeIfOpen(f); return; }

        if(f.count(EMPTY_BUCKET)==0) {
            resetWaterApproach();
            if(f.shop) {
                if(Rs2Shop.shopItems==null||Rs2Shop.shopItems.isEmpty()) {
                    if(sourceShopOpenedAt>0&&now-sourceShopOpenedAt<5000) {
                        phase="WAIT_WATER_SHOP_STOCK"; status(f); return;
                    }
                    hold(f,"Al Kharid shop stock unavailable for empty bucket id=1925"); return;
                }
                if(!Rs2Shop.hasMinimumStock(EMPTY_BUCKET,1)) {
                    hold(f,"Al Kharid shop has no observed stock for empty bucket id=1925"); return;
                }
                if(f.count(COINS)<2) { hold(f,"Cannot buy quest-needed bucket: carried coins="+f.count(COINS)); return; }
                if(waterBucketPurchaseAttempts>=1) {
                    hold(f,"One bounded bucket purchase lacked inventory/coin proof"); return;
                }
                waterBucketPurchaseAttempts++;
                if(Rs2Shop.buyItem(EMPTY_BUCKET,"1")) set("WATER_BUY_BUCKET",f,8000,EMPTY_BUCKET,null);
                else hold(f,"Al Kharid shop rejected Buy-1 empty bucket");
                return;
            }
            if(f.pos.distanceTo(ALKHARID_GENERAL_STORE)>10) {
                walk(f,ALKHARID_GENERAL_STORE,"TO_WATER_BUCKET_SHOP"); return;
            }
            if(Rs2Shop.getNearestShopNpc("Shop keeper",true)==null) {
                hold(f,"Al Kharid General Store waypoint reached but no live Shop keeper with Trade action; pos="+f.pos); return;
            }
            sourceShopOpenedAt=0;
            if(Rs2Shop.openShop("Shop keeper",true)) set("WATER_SHOP_OPEN",f,8000,0,null);
            else hold(f,"Al Kharid General Store open rejected");
            return;
        }
        if(f.shop) { Rs2Shop.closeShop(); set("WATER_SHOP_CLOSE",f,6000,0,null); return; }

        List<net.runelite.api.TileObject> candidates=Rs2GameObject.getAll(o->o!=null,f.pos,16).stream()
            .filter(o->o.getWorldLocation()!=null&&o.getWorldLocation().getPlane()==f.pos.getPlane())
            .filter(o->{
                String name=Rs2GameObject.getCompositionName(o).orElse("").toLowerCase(java.util.Locale.ROOT);
                return name.contains("fountain")||name.contains("well")||name.contains("sink");
            })
            .sorted(java.util.Comparator.comparingInt(o->f.pos.distanceTo(o.getWorldLocation())))
            .toList();
        if(candidates.isEmpty()) {
            if(f.pos.distanceTo(ALKHARID_PALACE_COURTYARD)>10) {
                walk(f,ALKHARID_PALACE_COURTYARD,"TO_AL_KHARID_WATER_SOURCE"); return;
            }
            String nearby=Rs2GameObject.getAll(o->o!=null,f.pos,8).stream()
                .filter(o->o.getWorldLocation()!=null)
                .filter(o->Rs2GameObject.getCompositionName(o).orElse("").toLowerCase(java.util.Locale.ROOT)
                    .matches(".*(fountain|well|sink|water).*"))
                .map(o->o.getId()+"@"+o.getWorldLocation()+" "+
                    Rs2GameObject.getCompositionName(o).orElse("")+" fill="+
                    Rs2GameObject.hasAction(o,"Fill")+" use="+Rs2GameObject.hasAction(o,"Use"))
                .limit(12).collect(java.util.stream.Collectors.joining(";"));
            hold(f,"No live Fill/Use fountain, well, or sink found in Al Kharid courtyard; nearby="+nearby+" player="+f.pos);
            return;
        }
        net.runelite.api.TileObject waterSource=candidates.get(0);
        WorldPoint target=waterSource.getWorldLocation();
        int distance=f.pos.distanceTo(target);
        if(distance>2) {
            if(!target.equals(waterApproachTarget)) {
                waterApproachTarget=target; waterApproachLastPosition=f.pos;
                waterApproachStartedAt=now; waterApproachProgressAt=now; waterApproachAttempts=0;
                Rs2Walker.clearWalkingRoute("prince-ali-water-source-approach-start");
            } else if(!f.pos.equals(waterApproachLastPosition)) {
                waterApproachLastPosition=f.pos; waterApproachProgressAt=now; waterApproachAttempts=0;
            }
            if(now-waterApproachStartedAt>90000||now-waterApproachProgressAt>30000||waterApproachAttempts>=4) {
                Rs2Walker.clearWalkingRoute("prince-ali-water-source-approach-exhausted");
                hold(f,"Bounded approach to live water source made no verified progress attempts="+
                    waterApproachAttempts+" player="+f.pos+" target="+target); return;
            }
            if(Rs2Player.isMoving()) { phase="WAIT_WATER_SOURCE_APPROACH"; status(f); return; }
            long callStarted=now;
            net.runelite.client.plugins.microbot.util.walker.WalkerState walkState=
                Rs2Walker.walkWithStateUntil(target,2,()->System.currentTimeMillis()-callStarted>=8000);
            WorldPoint post=Rs2Player.getWorldLocation();
            if(post!=null) f.pos=post;
            LOG.info("[PrinceAliRescue] WATER_SOURCE_APPROACH_RETURN state={} before={} post={} target={} attempt={} elapsedMs={}",
                walkState,waterApproachLastPosition,post,target,waterApproachAttempts+1,System.currentTimeMillis()-callStarted);
            if(post!=null&&post.distanceTo(target)<=2) {
                waterApproachLastPosition=post; waterApproachProgressAt=System.currentTimeMillis();
                phase="WATER_SOURCE_APPROACH_REACHED"; status(f); return;
            }
            waterApproachAttempts++;
            if(post!=null&&!post.equals(waterApproachLastPosition)) {
                waterApproachLastPosition=post; waterApproachProgressAt=System.currentTimeMillis();
                waterApproachAttempts=0;
            }
            phase="WAIT_WATER_SOURCE_APPROACH"; status(f); return;
        }
        resetWaterApproach();
        if(waterFillAttempts>=1) { hold(f,"One water fill attempt did not prove bucket 1925 to water 1929"); return; }
        waterFillAttempts++;
        boolean sent=Rs2GameObject.hasAction(waterSource,"Fill")
            ?Rs2GameObject.interact(waterSource,"Fill")
            :Rs2Inventory.useItemOnObject(EMPTY_BUCKET,waterSource.getId());
        LOG.info("[PrinceAliRescue] WATER_FILL_DISPATCH objectId={} name={} tile={} action={} bucket={} water={} distance={} attempt={}",
            waterSource.getId(),Rs2GameObject.getCompositionName(waterSource).orElse(""),target,
            Rs2GameObject.hasAction(waterSource,"Fill")?"Fill":"Use bucket on live water-source object",
            f.count(EMPTY_BUCKET),f.count(WATER),distance,waterFillAttempts);
        if(sent) set("WATER_FILL_BUCKET",f,10000,EMPTY_BUCKET,target);
        else hold(f,"Live water-source interaction rejected id="+waterSource.getId()+" tile="+target);
    }

    private void resetWaterApproach() {
        waterApproachTarget=null; waterApproachLastPosition=null;
        waterApproachStartedAt=waterApproachProgressAt=0; waterApproachAttempts=0;
    }

    private void localAshesSourceTick(Frame f) {
        long now=System.currentTimeMillis();
        if(f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0) {
            hold(f,"Ashes material source requires live logged-in varp273=20 plane=0; game="+
                f.game+" varp="+f.varp+" pos="+f.pos); return;
        }
        if(now-sourceStartedAt>360000) { hold(f,"Local ashes source exceeded six minutes"); return; }
        if(f.count(ASHES)>=sourceGoal) {
            sourceItem=sourceGoal=sourceAttempts=0; geStage="";
            ashesFireTile=null; ashesFireStartedAt=0;
            phase="LOCAL_ASHES_COMPLETE"; status(f); return;
        }
        if(Rs2GroundItem.exists(ASHES,10)) {
            if(Rs2GroundItem.take(ASHES)) set("TAKE_ASHES",f,8000,ASHES,null);
            else hold(f,"Nearby ashes id=592 detected but Take was rejected");
            return;
        }
        if(ashesFireStartedAt>0) {
            if(now-ashesFireStartedAt>180000) {
                hold(f,"Fire expired but ashes id=592 did not appear within three minutes at "+ashesFireTile); return;
            }
            phase="WAIT_ASHES_FIRE_BURNOUT"; status(f); return;
        }
        if("ASHES_GET_NORMAL_LOG".equals(geStage)) {
            if(f.shop) { Rs2Shop.closeShop(); set("SOURCE_SHOP_CLOSE",f,6000,0,null); return; }
            if(closeIfOpen(f)) return;
            localNormalLogSourceTick(f); return;
        }
        if("ASHES_BUY_TINDERBOX".equals(geStage)) { localTinderboxShopTick(f); return; }
        if(f.count(TINDERBOX)==0||f.count(LOGS)==0) {
            if(f.shop) { Rs2Shop.closeShop(); set("SOURCE_SHOP_CLOSE",f,6000,0,null); return; }
            if(!f.bank) {
                WorldPoint bankPoint=nearestBank(f.pos);
                if(f.pos==null||f.pos.distanceTo(bankPoint)>8) walk(f,bankPoint,"TO_ASHES_SUPPLY_BANK");
                else if(Rs2Bank.openBank()) set("OPEN_BANK",f,12000,0,null);
                else hold(f,"Bank open rejected while sourcing tinderbox/logs for ashes");
                return;
            }
            if(f.count(TINDERBOX)==0) {
                if(!Rs2Bank.hasBankItem(TINDERBOX,1)) {
                    geStage="ASHES_BUY_TINDERBOX";
                    if(closeIfOpen(f)) return;
                    localTinderboxShopTick(f); return;
                }
                if(!Rs2Bank.hasWithdrawAsItem()) {
                    if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                    else hold(f,"Tinderbox withdraw mode rejected");
                } else if(Rs2Bank.withdrawDeficit(TINDERBOX,1)) set("WITHDRAW",f,7000,TINDERBOX,null);
                else hold(f,"Tinderbox withdrawal rejected");
                return;
            }
            if(f.count(LOGS)==0) {
                if(!Rs2Bank.hasBankItem(LOGS,1)) {
                    int bankedAxe=bankedWoodcuttingAxe();
                    if(!hasWoodcuttingAxe(f)&&bankedAxe!=0) {
                        if(!Rs2Bank.hasWithdrawAsItem()) {
                            if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                            else hold(f,"Axe withdrawal mode rejected while sourcing one normal log");
                        } else if(Rs2Bank.withdrawDeficit(bankedAxe,1))
                            set("WITHDRAW",f,7000,bankedAxe,null);
                        else hold(f,"Banked woodcutting axe withdrawal rejected id="+bankedAxe);
                        return;
                    }
                    geStage="ASHES_GET_NORMAL_LOG";
                    if(closeIfOpen(f)) return;
                    localNormalLogSourceTick(f); return;
                }
                if(!Rs2Bank.hasWithdrawAsItem()) {
                    if(Rs2Bank.setWithdrawAsItem()) set("WITHDRAW_MODE",f,6000,0,null);
                    else hold(f,"Logs withdraw mode rejected");
                } else if(Rs2Bank.withdrawDeficit(LOGS,1)) set("WITHDRAW",f,7000,LOGS,null);
                else hold(f,"One normal log withdrawal rejected");
                return;
            }
        }
        if(f.count(TINDERBOX)<1||f.count(LOGS)<1) {
            hold(f,"Ashes fire prep did not retain one tinderbox and one log"); return;
        }
        if(closeIfOpen(f)) return;
        if(f.pos==null||f.pos.getPlane()!=0||f.pos.distanceTo(ASHES_FIRE_FIELD)>6) {
            walk(f,ASHES_FIRE_FIELD,"TO_ASHES_FIRE_FIELD"); return;
        }
        net.runelite.api.TileObject existingFire=nearbyFire(f.pos,6);
        if(existingFire!=null) {
            ashesFireTile=existingFire.getWorldLocation(); ashesFireStartedAt=now;
            LOG.info("[PrinceAliRescue] ADOPT_NEARBY_LIVE_FIRE tile={} player={}; waiting for ground ashes",
                ashesFireTile,f.pos);
            phase="WAIT_NEARBY_FIRE_FOR_ASHES"; status(f); return;
        }
        if(ashesFireAttempts>=1) { hold(f,"One firemaking attempt did not produce verified ashes"); return; }
        ashesFireAttempts++;
        ashesFireTile=f.pos;
        LOG.info("[PrinceAliRescue] LIGHT_ASH_FIRE_DISPATCH tinderbox={} logs={} tile={} attempt={}",
            f.count(TINDERBOX),f.count(LOGS),ashesFireTile,ashesFireAttempts);
        if(Rs2Inventory.combine(TINDERBOX,LOGS)) set("LIGHT_ASH_FIRE",f,60000,LOGS,ashesFireTile);
        else hold(f,"Tinderbox-on-logs combine rejected at "+ashesFireTile);
    }

    private void localTinderboxShopTick(Frame f) {
        if(f.count(TINDERBOX)>0) {
            if(f.shop) { Rs2Shop.closeShop(); set("SOURCE_SHOP_CLOSE",f,6000,0,null); return; }
            geStage="ASHES_LOCAL_BURN"; phase="TINDERBOX_READY_FROM_LOCAL_SHOP"; status(f); return;
        }
        if(f.bank) { closeIfOpen(f); return; }
        if(f.pos==null||f.pos.distanceTo(LUMBRIDGE_STORE)>8) {
            walk(f,LUMBRIDGE_STORE,"TO_ASHES_TINDERBOX_SHOP"); return;
        }
        if(f.shop) {
            if(Rs2Shop.shopItems==null||Rs2Shop.shopItems.isEmpty()) {
                if(sourceShopOpenedAt>0&&System.currentTimeMillis()-sourceShopOpenedAt<5000) {
                    phase="WAIT_ASHES_TINDERBOX_STOCK"; status(f); return;
                }
                hold(f,"Lumbridge shop stock unavailable while sourcing tinderbox id=590"); return;
            }
            if(!Rs2Shop.hasMinimumStock(TINDERBOX,1)) {
                hold(f,"Lumbridge shop has no observed tinderbox stock id=590"); return;
            }
            if(f.count(COINS)<1) { hold(f,"Cannot buy required tinderbox: no carried coin"); return; }
            if(ashesTinderboxPurchaseAttempts>=1) {
                hold(f,"One tinderbox shop purchase lacked inventory proof"); return;
            }
            ashesTinderboxPurchaseAttempts++;
            if(Rs2Shop.buyItem(TINDERBOX,"1")) set("ASHES_BUY_TINDERBOX",f,7000,TINDERBOX,null);
            else hold(f,"Lumbridge shop rejected tinderbox Buy-1");
            return;
        }
        if(Rs2Shop.getNearestShopNpc("Shop keeper",true)==null) {
            hold(f,"Lumbridge shop keeper unavailable at the verified store waypoint"); return;
        }
        if(Rs2Shop.openShop("Shop keeper",true)) set("ASHES_SHOP_OPEN",f,7000,0,null);
        else hold(f,"Lumbridge shop open rejected while sourcing tinderbox");
    }

    private boolean hasWoodcuttingAxe(Frame f) {
        for(int id:WOODCUTTING_AXES)
            if(f.count(id)>0) return true;
        return hasEquippedWoodcuttingAxe();
    }

    private int bankedWoodcuttingAxe() {
        for(int id:WOODCUTTING_AXES) if(Rs2Bank.hasBankItem(id,1)) return id;
        return 0;
    }

    private void approachAxeLogTarget(Frame f,WorldPoint target,String label) {
        approachAxeLogTarget(f,target,label,2);
    }

    private void approachAxeLogTarget(Frame f,WorldPoint target,String label,int radius) {
        long now=System.currentTimeMillis();
        if(target==null||f.pos==null) { hold(f,"Missing live tile for axe/log approach: "+label); return; }
        if(ashesLogApproachStartedAt==0) {
            Rs2Walker.clearWalkingRoute("prince-ali-ashes-log-approach-start");
            ashesLogApproachStartedAt=now; ashesLogApproachProgressAt=now;
            ashesLogApproachLastPosition=f.pos; ashesLogApproachAttempts=0;
        } else if(!f.pos.equals(ashesLogApproachLastPosition)) {
            ashesLogApproachLastPosition=f.pos; ashesLogApproachProgressAt=now;
            ashesLogApproachAttempts=0;
        }
        if(now-ashesLogApproachStartedAt>120000||now-ashesLogApproachProgressAt>45000
            ||ashesLogApproachAttempts>=4) {
            Rs2Walker.clearWalkingRoute("prince-ali-ashes-log-approach-exhausted");
            hold(f,"Bounded walk toward "+label+" made no verified progress attempts="+
                ashesLogApproachAttempts+" player="+f.pos+" target="+target); return;
        }
        if(Rs2Player.isMoving()) { phase="WAIT_ASHES_LOG_APPROACH"; status(f); return; }
        WorldPoint before=f.pos;
        long callStarted=System.currentTimeMillis();
        WalkerState walkState=Rs2Walker.walkWithStateUntil(target,radius,
            ()->System.currentTimeMillis()-callStarted>=15000);
        WorldPoint post=Rs2Player.getWorldLocation();
        if(post!=null) f.pos=post;
        LOG.info("[PrinceAliRescue] ASHES_LOG_APPROACH_RETURN label={} walkerState={} before={} post={} target={} radius={} attempt={} elapsedMs={}",
            label,walkState,before,post,target,radius,ashesLogApproachAttempts+1,System.currentTimeMillis()-callStarted);
        if(post!=null&&post.getPlane()==target.getPlane()&&post.distanceTo(target)<=radius) {
            resetAshesLogApproach(); set("ASHES_APPROACH_LOG_SOURCE",f,8000,0,target); return;
        }
        ashesLogApproachAttempts++;
        if(post!=null&&!post.equals(before)) {
            ashesLogApproachLastPosition=post; ashesLogApproachProgressAt=now;
            ashesLogApproachAttempts=0; phase="WAIT_ASHES_LOG_APPROACH"; status(f); return;
        }
        if(ashesLogApproachAttempts>=4||now-ashesLogApproachProgressAt>45000) {
            Rs2Walker.clearWalkingRoute("prince-ali-ashes-log-approach-no-progress");
            hold(f,"Walker made no tile progress toward "+label+" at "+post+" target="+target); return;
        }
        phase="WAIT_ASHES_LOG_APPROACH"; status(f);
    }

    private void resetAshesLogApproach() {
        ashesLogApproachLastPosition=null; ashesLogApproachStartedAt=0;
        ashesLogApproachProgressAt=0; ashesLogApproachAttempts=0;
    }

    private void localNormalLogSourceTick(Frame f) {
        if(f.count(LOGS)>0) {
            geStage="ASHES_LOCAL_BURN";
            phase="NORMAL_LOG_PROVED_FOR_ASHES"; status(f);
            return;
        }
        if(f.pos==null||f.pos.getPlane()!=0) {
            hold(f,"Cannot source one normal log from unexpected position/plane pos="+f.pos); return;
        }
        if(!hasWoodcuttingAxe(f)) {
            Rs2TileObjectModel axeLogs=object(BRONZE_AXE_LOGS_OBJECT,FRED_POS,12);
            if(axeLogs==null||!Rs2GameObject.hasAction(axeLogs,"Take-axe")) {
                if(f.pos.distanceTo(FRED_POS)>2) {
                    approachAxeLogTarget(f,FRED_POS,"Fred farm waypoint"); return;
                }
                hold(f,"Fred farm bronze-axe scenery 5581 unavailable; no axe in inventory, equipment, or inspected bank"); return;
            }
            if(!axeLogs.isReachable()||f.pos.distanceTo(axeLogs.getWorldLocation())>2) {
                approachAxeLogTarget(f,axeLogs.getWorldLocation(),"live axe scenery 5581"); return;
            }
            sourceAttempts++;
            LOG.info("[PrinceAliRescue] ASHES_BRONZE_AXE_DISPATCH id={} action=Take-axe tile={} reachable={} attempt={}",
                axeLogs.getId(),axeLogs.getWorldLocation(),axeLogs.isReachable(),sourceAttempts);
            if(axeLogs.click("Take-axe")) set("ASHES_TAKE_BRONZE_AXE",f,30000,BRONZE_AXE,axeLogs.getWorldLocation());
            else hold(f,"Fred farm Take-axe interaction rejected tile="+axeLogs.getWorldLocation());
            return;
        }
        // Always leave the bank area and use the verified Fred's farm tree area first.
        // A merely path-reachable Tree elsewhere may be behind a live gate/collision edge.
        if(f.pos.distanceTo(FRED_POS)>16) {
            approachAxeLogTarget(f,FRED_POS,"Fred farm tree area"); return;
        }
        net.runelite.api.GameObject tree=nearestReachableNormalTree(f,16,ashesNoLosTreeTargets);
        if(tree==null||tree.getWorldLocation()==null) {
            String candidates=Rs2GameObject.getAll(o->o!=null,f.pos,16).stream()
                .filter(o->"Tree".equalsIgnoreCase(Rs2GameObject.getCompositionName(o).orElse("")))
                .map(o->o.getId()+"@"+o.getWorldLocation()+" chop="+Rs2GameObject.hasAction(o,"Chop down")+
                    " reachable="+(o instanceof net.runelite.api.GameObject&&
                        Rs2GameObject.isReachable((net.runelite.api.GameObject)o))+" los="+Rs2GameObject.hasLineOfSight(o))
                .limit(12).toList().toString();
            hold(f,"No untried reachable regular Tree with Chop down within 16 tiles of Fred farm; player="+
                f.pos+" rejectedNoLos="+ashesNoLosTreeTargets+" trees="+candidates); return;
        }
        // Route beside one live, path-reachable tree; fresh LOS must still be true before Chop.
        int treeDistance=f.pos.distanceTo(tree.getWorldLocation());
        if(treeDistance>2) {
            approachAxeLogTarget(f,tree.getWorldLocation(),"live regular tree",2); return;
        }
        if(!Rs2GameObject.hasLineOfSight(tree)) {
            String key=tree.getId()+"@"+tree.getWorldLocation();
            ashesNoLosTreeTargets.add(key); resetAshesLogApproach();
            LOG.info("[PrinceAliRescue] ASHES_TREE_REJECT_NO_LOS id={} tile={} player={} tried={}; selecting another live reachable tree",
                tree.getId(),tree.getWorldLocation(),f.pos,ashesNoLosTreeTargets.size());
            phase="RESCAN_ANOTHER_ASHES_TREE"; status(f); return;
        }
        if(sourceAttempts>=2) {
            hold(f,"Bounded tree Chop down retry limit reached without log gain; player="+f.pos+
                " tree="+tree.getWorldLocation()); return;
        }
        sourceAttempts++;
        LOG.info("[PrinceAliRescue] ASHES_NORMAL_LOG_CHOP_DISPATCH treeId={} tile={} axeInInventory={} axeEquipped={} attempt={}",
            tree.getId(),tree.getWorldLocation(),hasAnyInventoryAxe(f),hasEquippedWoodcuttingAxe(),sourceAttempts);
        if(Rs2GameObject.interact(tree,"Chop down"))
            set("ASHES_CHOP_NORMAL_TREE",f,60000,LOGS,tree.getWorldLocation());
        else hold(f,"Live reachable tree Chop down rejected id="+tree.getId()+" tile="+tree.getWorldLocation());
    }

    private static boolean hasAnyInventoryAxe(Frame f) {
        for(int id:WOODCUTTING_AXES) if(f.count(id)>0) return true;
        return false;
    }

    private static net.runelite.api.GameObject nearestReachableNormalTree(Frame f,int radius,Set<String> rejected) {
        if(f==null||f.pos==null) return null;
        return Rs2GameObject.getAll(o->o!=null,f.pos,radius).stream()
            .filter(net.runelite.api.GameObject.class::isInstance)
            .map(net.runelite.api.GameObject.class::cast)
            .filter(o->"Tree".equalsIgnoreCase(Rs2GameObject.getCompositionName(o).orElse("")))
            .filter(o->Rs2GameObject.hasAction(o,"Chop down"))
            .filter(Rs2GameObject::isReachable)
            .filter(o->o.getWorldLocation()!=null
                &&!(o.getId()==1276&&o.getWorldLocation().equals(new WorldPoint(3265,3215,0)))
                &&!rejected.contains(o.getId()+"@"+o.getWorldLocation()))
            .min(java.util.Comparator.comparingInt(o->f.pos.distanceTo(o.getWorldLocation())))
            .orElse(null);
    }

    private static boolean hasEquippedWoodcuttingAxe() {
        return Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>)()->{
            List<net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel> equipped=
                Rs2Equipment.items();
            return equipped!=null&&equipped.stream().anyMatch(item->{
                for(int id:WOODCUTTING_AXES) if(item.getId()==id) return true;
                return false;
            });
        });
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
                    if(id==DYE&&remaining>0) {
                        geStage="DYE_LOCAL_ONIONS"; sourceAttempts=0;
                        onionFailedAttemptRecovered=false; onionApproach=null; onionLastPosition=null;
                        onionTimeoutRecovered=false;
                        onionLastStepAt=0; onionLastProgressAt=0; onionStepAttempts=0;
                        sourceStartedAt=now; phase="DYE_LOCAL_ONION_FALLBACK"; status(f);
                        LOG.info("[PrinceAliRescue] GE_DYE_UNAVAILABLE_FALLBACK quote={} deficit={} reserved={}; sourcing onions locally",
                            geQuote,remaining,geReservedTotal);
                        return;
                    }
                    if(id==ASHES&&remaining>0) {
                        geStage="ASHES_LOCAL_BURN"; sourceAttempts=0; sourceStartedAt=now;
                        ashesFallbackRecovered=false; ashesFireTile=null; ashesFireStartedAt=0; ashesFireAttempts=0;
                        ashesTinderboxShopRecovered=false; ashesTinderboxPurchaseAttempts=0;
                        phase="ASHES_LOCAL_FIREMAKING_FALLBACK"; status(f);
                        LOG.info("[PrinceAliRescue] GE_ASHES_UNAVAILABLE_FALLBACK quote={} deficit={}; local firemaking path",
                            geQuote,remaining);
                        return;
                    }
                    if(id==SOFT_CLAY&&remaining==1) {
                        geStage="SOFT_CLAY_LOCAL_MINE"; softClayMineAttempts=0;
                        sourceStartedAt=now; phase="SOFT_CLAY_LOCAL_MINE_FALLBACK"; status(f);
                        LOG.info("[PrinceAliRescue] GE_SOFT_CLAY_UNAVAILABLE_FALLBACK quote={} deficit={}; source one clay locally then use the existing water source route",
                            geQuote,remaining);
                        return;
                    }
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
    private boolean recoverObservedUnavailableDyeQuote(Frame f) {
        if(!"HOLD".equals(phase)
            ||!error.startsWith("GE quote unavailable/above 1000gp cumulative cap id=1765 quote=0 deficit=")
            ||sourceItem!=DYE||sourceGoal!=1||!"PREPARE".equals(geStage)
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null
            ||f.count(WIG)==0||f.count(DYE)>0||f.count(ONION)>=2||f.count(COINS)<5) return false;
        held=false; error=""; geStage="DYE_LOCAL_ONIONS";
        onionFailedAttemptRecovered=false; onionApproach=null; onionLastPosition=null;
        onionLastStepAt=0; onionLastProgressAt=0; onionStepAttempts=0;
        sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0;
        phase="RESUME_LOCAL_YELLOW_DYE_SOURCE";
        LOG.info("[PrinceAliRescue] RECOVERED_UNAVAILABLE_DYE_QUOTE; switching to bounded local onion/Aggie source coins={} pos={}",
            f.count(COINS),f.pos);
        return true;
    }
    private boolean recoverObservedUnavailableSoftClayQuote(Frame f) {
        if(softClayFallbackRecovered||!"HOLD".equals(phase)
            ||!error.startsWith("GE quote unavailable/above 1000gp cumulative cap id="+SOFT_CLAY+" quote=0 deficit=1")
            ||sourceItem!=SOFT_CLAY||sourceGoal!=1||!"PREPARE".equals(geStage)
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.count(SOFT_CLAY)>0||f.count(CLAY)>0||f.count(1265)==0) return false;
        softClayFallbackRecovered=true; held=false; error=""; geStage="SOFT_CLAY_LOCAL_MINE";
        sourceStartedAt=System.currentTimeMillis(); softClayMineAttempts=0;
        phase="RESUME_LOCAL_SOFT_CLAY";
        LOG.info("[PrinceAliRescue] RECOVERED_UNAVAILABLE_SOFT_CLAY_QUOTE; one local clay source allowed, pickaxe=1265, pos={}",f.pos);
        return true;
    }
    private boolean recoverObservedSoftClayMineGain(Frame f) {
        boolean exactHold="HOLD".equals(phase)&&error.startsWith("Unproved MINE_SOFT_CLAY;");
        boolean exactReload="HOLD_RELOAD_IN_FLIGHT".equals(phase)
            &&error.startsWith("Reload during MINE_SOFT_CLAY;");
        boolean exactExpiredSource="HOLD".equals(phase)
            &&error.startsWith("Local soft-clay ingredient source exceeded six minutes; clay=1 water=0");
        if((!exactHold&&!exactReload&&!exactExpiredSource)||sourceItem!=SOFT_CLAY||sourceGoal!=1
            ||f.game!=GameState.LOGGED_IN||f.varp!=20
            ||f.pos==null||f.pos.getPlane()!=0
            ||f.pos.distanceTo(RIMMINGTON_CLAY_MINE)>6
            ||f.count(CLAY)!=1||f.count(SOFT_CLAY)>0||f.count(1265)==0) return false;
        if(exactHold&&(!"SOFT_CLAY_LOCAL_MINE".equals(geStage)
            ||!softClayFallbackRecovered||softClayMineAttempts!=1)) return false;
        pending=null; held=false; error=""; restoredInFlightAction="";
        sourceStartedAt=System.currentTimeMillis();
        phase="RECOVERED_MINE_SOFT_CLAY_BY_INVENTORY_GAIN";
        LOG.info("[PrinceAliRescue] RECOVERED_MINE_SOFT_CLAY_BY_INVENTORY_GAIN cause={} clay={} sourceAttempts={} pos={}; timer rebased, no repeat click",
            exactHold?"MISSING_PROOF_PREDICATE":exactExpiredSource?"EXPIRED_TIMER_AFTER_OBSERVED_MINE":"RELOAD_AFTER_OBSERVED_MINE",
            f.count(CLAY),softClayMineAttempts,f.pos);
        return true;
    }
    private void localSoftClayMineTick(Frame f) {
        if(System.currentTimeMillis()-sourceStartedAt>360000) {
            hold(f,"Local soft-clay ingredient source exceeded six minutes; clay="+f.count(CLAY)+" water="+f.count(WATER)); return;
        }
        if(f.count(SOFT_CLAY)>0) {
            if(closeIfOpen(f)) return;
            sourceItem=sourceGoal=sourceAttempts=0; geStage="";
            phase="LOCAL_SOFT_CLAY_COMPLETE"; status(f); return;
        }
        if(f.count(CLAY)>0) {
            if(closeIfOpen(f)) return;
            sourceItem=sourceGoal=sourceAttempts=0; geStage="";
            phase="LOCAL_CLAY_READY_FOR_WATER"; status(f); return;
        }
        if(f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0) {
            hold(f,"Local clay source lost quest/login/plane precondition game="+f.game+" varp="+f.varp+" pos="+f.pos); return;
        }
        if(f.count(1265)==0) {
            hold(f,"Local clay source requires the already inspected bronze pickaxe 1265; inventory no longer has it"); return;
        }
        if(Rs2Inventory.isFull()) {
            hold(f,"Inventory full before local clay source; preserve all unclassified items"); return;
        }
        if(f.bank) { closeIfOpen(f); return; }
        if(f.pos.distanceTo(RIMMINGTON_CLAY_MINE)>22) {
            walk(f,RIMMINGTON_CLAY_MINE,"TO_SOFT_CLAY_MINE"); return;
        }
        net.runelite.api.TileObject rock=Rs2GameObject.getAll(o->o!=null,f.pos,22).stream()
            .filter(o->o.getWorldLocation()!=null&&o.getWorldLocation().getPlane()==0
                &&o.getWorldLocation().distanceTo(RIMMINGTON_CLAY_MINE)<=22
                &&java.util.Arrays.stream(CLAY_ROCK_IDS).anyMatch(id->id==o.getId())
                &&Rs2GameObject.hasAction(o,"Mine"))
            .min(java.util.Comparator.comparingInt(o->o.getWorldLocation().distanceTo(f.pos))).orElse(null);
        if(rock==null) {
            hold(f,"Rimmington waypoint reached but no live Clay rock with Mine action; player="+f.pos+" waypoint="+RIMMINGTON_CLAY_MINE);
            return;
        }
        if(softClayMineAttempts>=1) {
            hold(f,"One local clay Mine attempt already used without inventory proof; rock="+rock.getId()+" tile="+rock.getWorldLocation()); return;
        }
        softClayMineAttempts++;
        LOG.info("[PrinceAliRescue] SOFT_CLAY_MINE_DISPATCH rockId={} name={} tile={} action=Mine attempt={} pickaxe=1265",
            rock.getId(),Rs2GameObject.getCompositionName(rock).orElse(""),rock.getWorldLocation(),softClayMineAttempts);
        if(Rs2GameObject.interact(rock,"Mine")) set("MINE_SOFT_CLAY",f,60000,CLAY,rock.getWorldLocation());
        else hold(f,"Live Clay rock Mine dispatch rejected id="+rock.getId()+" tile="+rock.getWorldLocation());
    }
    private boolean recoverObservedOnionPickHold(Frame f) {
        boolean exactPriorPickHold=error.startsWith("Unproved PICK_DYE_ONION;")
            ||"Reload during PICK_DYE_ONION; inspect quest/inventory/scene before resuming".equals(error);
        if(onionFailedAttemptRecovered||!("HOLD".equals(phase)||"HOLD_RELOAD_IN_FLIGHT".equals(phase))
            ||!exactPriorPickHold||sourceItem!=DYE||sourceGoal!=1
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.count(WIG)==0||f.count(DYE)>0||f.count(ONION)>=2||f.count(COINS)<5
            ||f.pos.distanceTo(FRED_ONION_FIELD)>12) return false;
        held=false; error=""; onionFailedAttemptRecovered=true;
        restoredInFlightAction="";
        sourceStartedAt=System.currentTimeMillis();
        onionApproach=null; onionLastPosition=null; onionLastStepAt=0;
        onionLastProgressAt=System.currentTimeMillis(); onionStepAttempts=0;
        phase="RECOVER_ONION_PICK_BY_ROUTING_TO_REACHABLE_SIDE";
        LOG.info("[PrinceAliRescue] RECOVERED_UNPROVED_ONION_PICK count={} attempts={} player={}; rerouting to verified adjacent tile; no click replay",
            f.count(ONION),sourceAttempts,f.pos);
        return true;
    }
    private boolean recoverObservedOnionTimeoutHold(Frame f) {
        if(onionTimeoutRecovered||!onionFailedAttemptRecovered||!"HOLD".equals(phase)
            ||!error.startsWith("Local yellow-dye source exceeded six minutes; onions=")
            ||sourceItem!=DYE||sourceGoal!=1||!"DYE_LOCAL_ONIONS".equals(geStage)
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.pos.distanceTo(FRED_ONION_FIELD)>12||f.count(WIG)==0||f.count(DYE)>0
            ||f.count(ONION)>=2||f.count(COINS)<5) return false;
        onionTimeoutRecovered=true; sourceStartedAt=System.currentTimeMillis();
        held=false; error=""; phase="RESUME_ONION_SOURCE_WITH_FRESH_BOUNDED_TIMER";
        LOG.info("[PrinceAliRescue] RECOVERED_STALE_ONION_SOURCE_TIMER once; count={} attempts={} player={}; no action replay",
            f.count(ONION),sourceAttempts,f.pos);
        return true;
    }
    private boolean recoverObservedOnionSecondPickCap(Frame f) {
        if(onionSecondPickCapRecovered||!"HOLD".equals(phase)
            ||!error.startsWith("Two onion-pick attempts without two verified onions; count=")
            ||sourceItem!=DYE||sourceGoal!=1||!"DYE_LOCAL_ONIONS".equals(geStage)
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.pos.distanceTo(FRED_ONION_FIELD)>12||f.count(WIG)==0||f.count(DYE)>0
            ||f.count(ONION)!=1||f.count(COINS)<5) return false;
        sourceAttempts=0; sourceStartedAt=System.currentTimeMillis();
        onionSecondPickCapRecovered=true;
        held=false; error=""; phase="RESUME_SECOND_VERIFIED_ONION_PICK";
        LOG.info("[PrinceAliRescue] RECOVERED_SECOND_ONION_PICK_CAP with exactly one verified onion; resetting per-pick attempt budget player={}",f.pos);
        return true;
    }
    private boolean recoverObservedUnavailableAshesQuote(Frame f) {
        if(ashesFallbackRecovered||!"HOLD".equals(phase)
            ||!error.startsWith("GE quote unavailable/above 1000gp cumulative cap id=592 quote=0 deficit=")
            ||sourceItem!=ASHES||sourceGoal!=1||!"PREPARE".equals(geStage)
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.count(ASHES)>0) return false;
        held=false; error=""; geStage="ASHES_LOCAL_BURN"; sourceStartedAt=System.currentTimeMillis();
        ashesFallbackRecovered=true; ashesFireTile=null; ashesFireStartedAt=0; ashesFireAttempts=0;
        phase="RECOVER_ASHES_FROM_EXACT_ZERO_QUOTE";
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_ASHES_ZERO_QUOTE; checking nearby ashes and banked firemaking supplies");
        return true;
    }
    private boolean recoverObservedMissingAshesTinderbox(Frame f) {
        if(ashesTinderboxShopRecovered||!"HOLD".equals(phase)
            ||!error.equals("Local ashes source needs tinderbox id=590; none carried or banked")
            ||sourceItem!=ASHES||sourceGoal!=1||!"ASHES_LOCAL_BURN".equals(geStage)
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.count(ASHES)>0||f.count(TINDERBOX)>0) return false;
        held=false; error=""; geStage="ASHES_BUY_TINDERBOX";
        sourceStartedAt=System.currentTimeMillis(); sourceShopOpenedAt=0;
        ashesTinderboxShopRecovered=true; ashesTinderboxPurchaseAttempts=0;
        phase="RECOVER_TINDERBOX_FROM_LOCAL_SHOP";
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_MISSING_TINDERBOX_HOLD; using stock-checked Lumbridge shop path");
        return true;
    }
    private boolean recoverObservedMissingAshesLog(Frame f) {
        if(ashesLogSourceRecovered||!"HOLD".equals(phase)
            ||!error.equals("Local ashes source needs normal logs id=1511; none carried or banked")
            ||sourceItem!=ASHES||sourceGoal!=1||!"ASHES_LOCAL_BURN".equals(geStage)
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.count(ASHES)>0||f.count(LOGS)>0||f.count(TINDERBOX)==0) return false;
        held=false; error=""; geStage="ASHES_GET_NORMAL_LOG";
        sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0;
        ashesLogSourceRecovered=true; phase="RECOVER_EXACT_MISSING_LOG_HOLD";
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_MISSING_LOG_HOLD; sourcing one normal log from a live tree pos={} coins={}",
            f.pos,f.count(COINS));
        return true;
    }
    private boolean recoverObservedTreeChopAfterReload(Frame f) {
        boolean exactTreeReload="ASHES_CHOP_NORMAL_TREE".equals(restoredInFlightAction)
            &&lastReloadHoldError.startsWith("Unproved ASHES_CHOP_NORMAL_TREE;");
        boolean exactWrappedTreeReload="Reload during ASHES_CHOP_NORMAL_TREE; inspect quest/inventory/scene before resuming"
            .equals(error);
        boolean exactExpiredSourceHold="HOLD".equals(phase)
            &&"Local ashes source exceeded six minutes".equals(error)
            &&ashesTreeRetryRecovered&&sourceAttempts==1;
        if((ashesTreeRetryRecovered&&!exactExpiredSourceHold)
            ||(!"HOLD_RELOAD_IN_FLIGHT".equals(phase)&&!exactExpiredSourceHold)
            ||(!exactTreeReload&&!exactWrappedTreeReload&&!exactExpiredSourceHold)
            ||sourceItem!=ASHES||sourceGoal!=1||!"ASHES_GET_NORMAL_LOG".equals(geStage)
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.count(LOGS)>0||f.count(TINDERBOX)==0||!hasWoodcuttingAxe(f)) return false;
        net.runelite.api.GameObject tree=Rs2GameObject.findReachableObject("Tree",true,16,f.pos,true,"Chop down");
        if(tree==null||tree.getWorldLocation()==null||f.pos.distanceTo(tree.getWorldLocation())>2) return false;
        held=false; error=""; phase="RETRY_TIMED_OUT_TREE_APPROACH";
        sourceAttempts=Math.max(1,sourceAttempts); ashesTreeRetryRecovered=true;
        sourceStartedAt=System.currentTimeMillis();
        restoredInFlightAction=""; lastReloadHoldError="";
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_TREE_CHOP_TIMEOUT after fresh scene/inventory check wrapper={} expiredSourceHold={} player={} tree={} attempts={}; one bounded nearby retry remains",
            exactWrappedTreeReload,exactExpiredSourceHold,
            f.pos,tree.getWorldLocation(),sourceAttempts);
        return true;
    }
    private boolean recoverObservedInaccessibleTreeTarget(Frame f) {
        if(ashesTreeRerouteRecovered||!"HOLD".equals(phase)
            ||!error.startsWith("Live reachable tree Chop down rejected id=1276 tile=WorldPoint(x=3265, y=3215, plane=0)")
            ||sourceItem!=ASHES||sourceGoal!=1||!"ASHES_GET_NORMAL_LOG".equals(geStage)
            ||sourceAttempts!=2||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.pos.distanceTo(new WorldPoint(3265,3215,0))>4||f.count(LOGS)>0
            ||f.count(TINDERBOX)==0||!hasWoodcuttingAxe(f)) return false;
        held=false; error=""; phase="REROUTE_TO_FRED_FARM_AFTER_BLOCKED_TREE";
        ashesTreeRerouteRecovered=true; sourceAttempts=0; sourceStartedAt=System.currentTimeMillis();
        resetAshesLogApproach();
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_BLOCKED_TREE_TARGET id=1276 tile=3265,3215; fresh state confirms no log and one exact failed-reach recovery; rerouting to Fred's farm for a different nearby tree player={}",f.pos);
        return true;
    }
    private boolean recoverObservedNoTreeAtFred(Frame f) {
        if(ashesFredTreeRescanRecovered||!"HOLD".equals(phase)
            ||!error.startsWith("No live reachable regular Tree with Chop down near Fred farm;")
            ||!ashesTreeRerouteRecovered||sourceItem!=ASHES||sourceGoal!=1
            ||!"ASHES_GET_NORMAL_LOG".equals(geStage)||sourceAttempts!=0
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.pos.distanceTo(FRED_POS)>2||f.count(LOGS)>0||f.count(TINDERBOX)==0||!hasWoodcuttingAxe(f)) return false;
        held=false; error=""; phase="RESCAN_FRED_TREES_WITH_LINE_OF_SIGHT";
        ashesFredTreeRescanRecovered=true; sourceStartedAt=System.currentTimeMillis();
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_NO_TREE_WITHIN_8_HOLD; rescanning live regular trees within 16 tiles, requiring Chop down, reachability, and line of sight; excluding the failed id1276 tile");
        return true;
    }
    private boolean recoverObservedFredTreeLineOfSightHold(Frame f) {
        if(ashesFredLineOfSightApproachRecovered||!"HOLD".equals(phase)
            ||!error.startsWith("No live regular Tree with Chop down and clear line of sight within 16 tiles of Fred farm;")
            ||!ashesFredTreeRescanRecovered||sourceItem!=ASHES||sourceGoal!=1
            ||!"ASHES_GET_NORMAL_LOG".equals(geStage)||sourceAttempts!=0
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.pos.distanceTo(FRED_POS)>2||f.count(LOGS)>0||f.count(TINDERBOX)==0
            ||!hasWoodcuttingAxe(f)) return false;
        held=false; error=""; phase="RESCAN_FRED_TREES_AND_APPROACH_FOR_LOS";
        ashesFredLineOfSightApproachRecovered=true; ashesNoLosTreeTargets.clear();
        sourceStartedAt=System.currentTimeMillis(); resetAshesLogApproach();
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_FRED_TREE_NO_LOS_HOLD; will approach one reachable live tree to radius 1, verify fresh line of sight, reject blocked trees and require log 1511 inventory gain");
        return true;
    }
    private boolean recoverObservedTreeApproachHold(Frame f) {
        if(ashesTreeApproachFailureRecovered||!"HOLD".equals(phase)
            ||!error.startsWith("Walker made no tile progress toward live regular tree at ")
            ||!ashesFredLineOfSightApproachRecovered||sourceItem!=ASHES||sourceGoal!=1
            ||!"ASHES_GET_NORMAL_LOG".equals(geStage)||sourceAttempts!=0
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.pos.distanceTo(FRED_POS)>16||f.count(LOGS)>0||f.count(TINDERBOX)==0
            ||!hasWoodcuttingAxe(f)) return false;
        net.runelite.api.GameObject tree=nearestReachableNormalTree(f,16,ashesNoLosTreeTargets);
        if(tree==null||tree.getWorldLocation()==null||f.pos.distanceTo(tree.getWorldLocation())>2) return false;
        String key=tree.getId()+"@"+tree.getWorldLocation();
        ashesNoLosTreeTargets.add(key); ashesTreeApproachFailureRecovered=true;
        held=false; error=""; phase="RESCAN_AFTER_BLOCKED_TREE_APPROACH";
        sourceStartedAt=System.currentTimeMillis(); resetAshesLogApproach();
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_TREE_APPROACH_HOLD; excluded stalled live tree id={} tile={} after fresh state check; rescan another candidate player={}",
            tree.getId(),tree.getWorldLocation(),f.pos);
        return true;
    }
    private boolean recoverObservedTreeApproachReload(Frame f) {
        if(!"HOLD_RELOAD_IN_FLIGHT".equals(phase)
            ||!"Reload during ASHES_APPROACH_LOG_SOURCE; inspect quest/inventory/scene before resuming".equals(error)
            ||sourceItem!=ASHES||sourceGoal!=1
            ||f.game!=GameState.LOGGED_IN||f.varp!=20||f.pos==null||f.pos.getPlane()!=0
            ||f.pos.distanceTo(FRED_POS)>16||f.count(LOGS)>0||f.count(TINDERBOX)==0) return false;
        held=false; error=""; phase="RESCAN_AFTER_TREE_APPROACH_RELOAD";
        geStage="ASHES_GET_NORMAL_LOG";
        sourceStartedAt=System.currentTimeMillis();
        resetAshesLogApproach(); restoredInFlightAction=""; lastReloadHoldError="";
        LOG.info("[PrinceAliRescue] RECOVERED_EXACT_TREE_APPROACH_RELOAD; fresh state confirms logged-in varp20, no log 1511, axe+tinderbox, and inside Fred's 16-tile search; resume from live tree scan without replaying movement or chop");
        return true;
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
        boolean savedStepApproach="HOLD_RELOAD_IN_FLIGHT".equals(phase)
            &&"WOOL_APPROACH_DOWNSTAIRS".equals(restoredInFlightAction)
            &&"Reload during WOOL_APPROACH_DOWNSTAIRS; inspect quest/inventory/scene before resuming".equals(error);
        boolean savedInFlight="HOLD_RELOAD_IN_FLIGHT".equals(phase)
            &&"WOOL_CLIMB_DOWN".equals(restoredInFlightAction)
            &&lastReloadHoldError.startsWith("Unproved WOOL_CLIMB_DOWN;");
        boolean exactLiveHold="HOLD".equals(phase)&&error.startsWith("Unproved WOOL_CLIMB_DOWN;");
        boolean exactStairRouteHold="HOLD".equals(phase)
            &&error.startsWith("Walker exited before reaching live castle stair;");
        boolean exactWrappedReloadHold="HOLD_RELOAD_IN_FLIGHT".equals(phase)
            &&"Reload during WOOL_CLIMB_DOWN; inspect quest/inventory/scene before resuming".equals(error);
        if((!savedStepApproach&&!savedInFlight&&!exactLiveHold&&!exactStairRouteHold&&!exactWrappedReloadHold)
            ||sourceItem!=WOOL||sourceGoal!=3||f.game!=GameState.LOGGED_IN||f.varp!=20
            ||f.count(WOOL)<1||f.count(WOOL)>=sourceGoal||f.count(RAW_WOOL)!=0
            ||f.count(SHEARS)==0||f.pos==null||f.pos.getPlane()!=1
            ||f.pos.distanceTo(WOOL_WHEEL)>10) return false;
        held=false; error=""; phase="RESUME_WOOL_DESCENT_APPROACH";
        sourceStartedAt=System.currentTimeMillis(); sourceAttempts=0; woolSpinning=false;
        restoredInFlightAction=""; lastReloadHoldError="";
        LOG.info("[PrinceAliRescue] RECOVERED_WOOL_DESCENT_HOLD cause={} balls={} goal={} rawWool={} pos={}; will inspect route doors and approach live stairs before another click",
            exactStairRouteHold?"STAIR_ROUTE_HOLD":"DESCENT_HOLD",f.count(WOOL),sourceGoal,f.count(RAW_WOOL),f.pos);
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
    private boolean recoverObservedContinueHold(Frame f) {
        boolean savedUnprovedContinue=lastReloadHoldError.startsWith("Unproved CONTINUE;")
            &&"CONTINUE".equals(restoredInFlightAction);
        boolean explicitReloadedContinue=error.startsWith("Reload during CONTINUE;");
        if(!"HOLD_RELOAD_IN_FLIGHT".equals(phase)
            ||(!savedUnprovedContinue&&!explicitReloadedContinue)||continueRetryUsed
            ||f.game!=GameState.LOGGED_IN||f.quest!=QuestState.IN_PROGRESS||f.varp!=20
            ||f.pos==null||f.pos.getX()!=3126||f.pos.getY()!=3244||f.pos.getPlane()!=0
            ||f.count(KEY_PRINT)<=0||!f.inDialogue||!f.continuePrompt||!f.options.isEmpty()) return false;
        continueRetryUsed=true;
        held=false; error=""; phase="RESUME_ONE_CONTINUE_AFTER_FRESH_PROMPT";
        LOG.warn("[PrinceAliRescue] RECOVERED_ONE_CONTINUE_RETRY fresh prompt=true options=[] varp={} pos={} keyPrint={} dialogue={}",
            f.varp,f.pos,f.count(KEY_PRINT),f.dialogue);
        Rs2Dialogue.clickContinue();
        set("CONTINUE",f,6000,0,null);
        return true;
    }
    private void dialogueOption(Frame f) {
        if(f.varp==20&&f.pos!=null&&f.pos.distanceTo(OSMAN_POS)<=8
            &&f.options.equals("Do you know why they've taken the Prince?|Where abouts in Draynor is Leela?|I'll get going.|")) {
            if(Rs2Dialogue.clickOption("I'll get going.")) set("OPTION",f,7000,0,null);
            else hold(f,"Osman exit option rejected");
            return;
        }
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
        List<Rs2TileObjectModel> routeDoors=Microbot.getRs2TileObjectCache().query()
            .within(f.pos,5)
            .where(o->{
                WorldPoint location=o.getWorldLocation();
                if(location==null||location.getPlane()!=f.pos.getPlane()
                    ||!Rs2GameObject.hasAction(o,"Open")) return false;
                String name=o.getName()==null?"":o.getName().toLowerCase(java.util.Locale.ROOT);
                return (name.contains("door")||name.contains("gate"))
                    &&location.distanceTo(tile)<=f.pos.distanceTo(tile)+1;
            })
            .toListOnClientThread();
        Rs2TileObjectModel routeDoor=routeDoors.stream()
            .min(java.util.Comparator.comparingInt(o->o.getWorldLocation().distanceTo(f.pos)))
            .orElse(null);
        if(routeDoor!=null) {
            WorldPoint doorTile=routeDoor.getWorldLocation();
            String doorName=routeDoor.getName();
            int doorId=routeDoor.getId();
            net.runelite.api.ObjectComposition doorComposition=routeDoor.getObjectComposition();
            String doorActions=doorComposition==null?"[]":java.util.Arrays.toString(doorComposition.getActions());
            LOG.info("[PrinceAliRescue] WOOL_STAIRS_ROUTE_DOOR_DISPATCH id={} tile={} name={} actions={} from={} stair={}",
                doorId,doorTile,doorName,doorActions,f.pos,tile);
            if(routeDoor.click("Open")) set("WOOL_OPEN_STAIR_DOOR",f,9000,doorId,doorTile);
            else hold(f,"Open rejected for live stair-route door id="+doorId+" tile="+doorTile+" name="+doorName);
            return;
        }
        int distance=f.pos.distanceTo(tile);
        if(distance>2) { stepWoolStairApproach(f,stairs,tile,actions); return; }
        LOG.info("[PrinceAliRescue] WOOL_STAIRS_APPROACH_PROVED id={} tile={} player={} distance={} radius=2",
            stairs.getId(),tile,f.pos,distance);
        LOG.info("[PrinceAliRescue] WOOL_CLIMB_DOWN_DISPATCH id={} tile={} name={} actions={} reachable={} player={} distance={}",
            stairs.getId(),tile,stairs.getName(),actions,stairs.isReachable(),f.pos,distance);
        if(stairs.click("Climb-down")) set("WOOL_CLIMB_DOWN",f,12000,0,tile);
        else hold(f,"Climb-down rejected at live castle stair id="+stairs.getId()+" tile="+tile
            +" name="+stairs.getName()+" actions="+actions+" reachable="+stairs.isReachable());
    }
    private void stepWoolStairApproach(Frame f,Rs2TileObjectModel stairs,WorldPoint tile,String actions) {
        long now=System.currentTimeMillis();
        if(woolStairLastPosition==null) {
            // Clear only this script's stale walker target before beginning the bounded approach.
            Rs2Walker.clearWalkingRoute("prince-ali-wool-stair-approach-start");
            woolStairLastPosition=f.pos; woolStairLastProgressAt=now;
            LOG.info("[PrinceAliRescue] WOOL_STAIRS_APPROACH_BEGIN id={} tile={} from={} radius=2",
                stairs.getId(),tile,f.pos);
        } else if(!woolStairLastPosition.equals(f.pos)) {
            LOG.info("[PrinceAliRescue] WOOL_STAIRS_POSITION_CHANGE before={} now={} dist={}",
                woolStairLastPosition,f.pos,f.pos.distanceTo(tile));
            woolStairLastPosition=f.pos; woolStairLastProgressAt=now;
        }
        if(now-woolStairLastProgressAt>20000||woolStairStepAttempts>=12) {
            Rs2Walker.clearWalkingRoute("prince-ali-wool-stair-approach-no-progress");
            hold(f,"Bounded stair walkStep made no verified progress attempts="+woolStairStepAttempts
                +" lastStepAgeMs="+(now-woolStairLastStepAt)+" player="+f.pos+" target="+tile
                +" actions="+actions); return;
        }
        if(now-woolStairLastStepAt<1600||Rs2Player.isMoving()) { phase="WAIT_WOOL_STAIRS_STEP"; status(f); return; }
        WalkerState state=Rs2Walker.walkStep(tile,2);
        woolStairLastStepAt=now; woolStairStepAttempts++;
        LOG.info("[PrinceAliRescue] WOOL_STAIRS_STEP state={} attempt={} player={} target={} dist={} actions={}",
            state,woolStairStepAttempts,f.pos,tile,f.pos.distanceTo(tile),actions);
        if(state==WalkerState.UNREACHABLE||state==WalkerState.EXIT) {
            Rs2Walker.clearWalkingRoute("prince-ali-wool-stair-approach-"+state);
            hold(f,"walkStep "+state+" for live castle stair id="+stairs.getId()+" tile="+tile
                +" player="+f.pos+" actions="+actions); return;
        }
        if(state==WalkerState.ARRIVED) {
            if(f.pos.distanceTo(tile)<=2) set("WOOL_APPROACH_DOWNSTAIRS",f,8000,0,tile);
            else hold(f,"walkStep reported ARRIVED outside stair approach radius; player="+f.pos
                +" target="+tile+" distance="+f.pos.distanceTo(tile));
            return;
        }
        phase="WAIT_WOOL_STAIRS_STEP"; status(f);
    }
    private void resetWoolStairRoute() {
        woolStairLastStepAt=0; woolStairLastProgressAt=0;
        woolStairStepAttempts=0; woolStairLastPosition=null;
    }
    private void restoreExactSavedWoolStage() {
        try {
            Properties saved=new Properties();
            try(java.io.InputStream in=Files.newInputStream(STATUS)) { saved.load(in); }
            long age=System.currentTimeMillis()-Long.parseLong(saved.getProperty("timestamp","0"));
            if(age<0||age>300000||!Long.toString(ProcessHandle.current().pid()).equals(saved.getProperty("pid"))
                ||!"15".equals(saved.getProperty("build"))
                ||!"20".equals(saved.getProperty("varp273"))
                ||!"LOGGED_IN".equals(saved.getProperty("gameState"))
                ||!"1".equals(saved.getProperty("ballOfWool"))
                ||!"0".equals(saved.getProperty("rawWool"))
                ||!"1".equals(saved.getProperty("shears"))
                ||!saved.getProperty("position","").contains("plane=1")
                ||!"1759".equals(saved.getProperty("sourceItem"))
                ||!"3".equals(saved.getProperty("sourceGoal"))) return;
            String oldPhase=saved.getProperty("phase","");
            if(!"RESUME_WOOL_DESCENT_APPROACH".equals(oldPhase)
                &&!"PROVED_WOOL_OPEN_STAIR_DOOR".equals(oldPhase)
                &&!"WAIT_WOOL_STAIRS_STEP".equals(oldPhase)) return;
            sourceItem=WOOL; sourceGoal=3; sourceAttempts=0; sourceSpent=0;
            sourceStartedAt=System.currentTimeMillis(); sourceLastCount=1;
            sourceLastCoins=0;
            bankInspected=true; geStage="WOOL_GATHER"; woolSpinning=false;
            lastRawWool=0; lastWoolBalls=1; woolStageRecoveredFromStatus=true;
            held=false; error=""; phase="VALIDATE_SAVED_WOOL_STAGE";
            LOG.info("[PrinceAliRescue] RECOVERED_SAVED_WOOL_STAGE pid={} ageMs={} priorPhase={}; awaiting fresh frame before action",
                saved.getProperty("pid"),age,oldPhase);
        } catch(Exception ignored) { }
    }
    private boolean proved(Pending p,Frame f) {
        if(f.quest==QuestState.FINISHED) return true;
        if("ASHES_TAKE_BRONZE_AXE".equals(p.action))
            return f.count(BRONZE_AXE)>p.before.count(BRONZE_AXE);
        if("ASHES_CHOP_NORMAL_TREE".equals(p.action))
            return f.count(LOGS)>p.before.count(LOGS);
        if("ASHES_APPROACH_LOG_SOURCE".equals(p.action))
            return f.pos!=null&&p.target!=null&&f.pos.getPlane()==p.target.getPlane()
                &&f.pos.distanceTo(p.target)<=2;
        if("WOOL_GET_SHEARS".equals(p.action)) return f.count(SHEARS)>p.before.count(SHEARS);
        if("WOOL_SHEAR".equals(p.action)) return f.count(RAW_WOOL)>p.before.count(RAW_WOOL);
        if("WOOL_CLIMB_UP".equals(p.action)) return f.pos!=null&&f.pos.getPlane()==1;
        if("WOOL_CLIMB_DOWN".equals(p.action)) return f.pos!=null&&f.pos.getPlane()==0;
        if("WOOL_APPROACH_DOWNSTAIRS".equals(p.action)) return f.pos!=null&&p.target!=null
            &&f.pos.getPlane()==1&&f.pos.distanceTo(p.target)<=2;
        if("WOOL_OPEN_STAIR_DOOR".equals(p.action)) {
            net.runelite.api.TileObject door=Rs2GameObject.findObjectByLocation(p.target);
            return door==null||!Rs2GameObject.hasAction(door,"Open");
        }
        if("WOOL_OPEN_WHEEL".equals(p.action)) return f.production;
        if("WOOL_SPIN".equals(p.action)) return f.count(WOOL)>p.before.count(WOOL)
            &&f.count(RAW_WOOL)<p.before.count(RAW_WOOL);
        if("OPEN_BANK".equals(p.action)) return f.bank;
        if("CLOSE_BANK".equals(p.action)) return !f.bank;
        if("WITHDRAW_MODE".equals(p.action)) return Rs2Bank.hasWithdrawAsItem();
        if("WITHDRAW".equals(p.action)) return f.count(p.item)>p.before.count(p.item);
        if("MINE_SOFT_CLAY".equals(p.action)) return f.count(CLAY)>p.before.count(CLAY);
        if("CRAFT_SOFT_CLAY".equals(p.action)) return f.count(SOFT_CLAY)>p.before.count(SOFT_CLAY)
            &&f.count(CLAY)<p.before.count(CLAY)
            &&f.count(WATER)<p.before.count(WATER);
        if("CRAFT_YELLOW_DYE".equals(p.action)) return f.count(DYE)>p.before.count(DYE)
            &&f.count(ONION)<p.before.count(ONION)
            &&p.before.count(COINS)-f.count(COINS)==5;
        if("PICK_DYE_ONION".equals(p.action)) return f.count(ONION)>p.before.count(ONION);
        if("TAKE_ASHES".equals(p.action)) return f.count(ASHES)>p.before.count(ASHES);
        if("LIGHT_ASH_FIRE".equals(p.action)) return p.target!=null
            &&f.count(LOGS)<p.before.count(LOGS)&&nearbyFire(p.target,2)!=null;
        if("ASHES_SHOP_OPEN".equals(p.action)) return f.shop;
        if("ASHES_BUY_TINDERBOX".equals(p.action)) {
            int debit=p.before.count(COINS)-f.count(COINS);
            return f.count(TINDERBOX)>p.before.count(TINDERBOX)&&debit>0&&debit<=10;
        }
        if("WATER_SHOP_OPEN".equals(p.action)) return f.shop;
        if("WATER_SHOP_CLOSE".equals(p.action)) return !f.shop;
        if("WATER_BUY_BUCKET".equals(p.action)) {
            int debit=p.before.count(COINS)-f.count(COINS);
            return f.count(EMPTY_BUCKET)>p.before.count(EMPTY_BUCKET)&&debit>0&&debit<=5;
        }
        if("WATER_FILL_BUCKET".equals(p.action))
            return f.count(WATER)>p.before.count(WATER)&&f.count(EMPTY_BUCKET)<p.before.count(EMPTY_BUCKET);
        if("SOURCE_SHOP_OPEN".equals(p.action)) return f.shop;
        if("SOURCE_SHOP_CLOSE".equals(p.action)) return !f.shop;
        if("BAR_SHANTAY_OPEN".equals(p.action)) return f.shop;
        if("BAR_SHANTAY_OPEN_RETRY".equals(p.action)) return f.shop;
        if("BAR_SHANTAY_REACHABLE_TRADE".equals(p.action)) return f.shop;
        if("WALK_SHANTAY_INTERACTION_TILE".equals(p.action))
            return f.pos!=null&&p.target!=null&&f.pos.equals(p.target);
        if("BAR_SHANTAY_STAGE_COINS".equals(p.action))
            return f.bank&&p.before.count(COINS)>SHANTAY_BAR_COIN_CAP
                &&f.count(COINS)==SHANTAY_BAR_COIN_CAP;
        if("BAR_SHANTAY_BUY".equals(p.action)) {
            int debit=p.before.count(COINS)-f.count(COINS);
            return f.count(BRONZE_BAR)==p.before.count(BRONZE_BAR)+1
                &&p.before.count(BRONZE_BAR)==0&&p.before.count(COINS)>0
                &&p.before.count(COINS)<=SHANTAY_BAR_COIN_CAP&&debit>0
                &&debit<=p.before.count(COINS);
        }
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
        if("MAKE_BRONZE_KEY".equals(p.action)) return f.count(BRONZE_KEY)>p.before.count(BRONZE_KEY)
            &&f.count(KEY_PRINT)<p.before.count(KEY_PRINT)&&f.count(BRONZE_BAR)<p.before.count(BRONZE_BAR);
        if("DEPOSIT_UNNEEDED".equals(p.action)) return f.count(p.item)<p.before.count(p.item)
            &&f.bank&&f.bankContentsAvailable&&p.before.bankContentsAvailable
            &&f.bankItems.getOrDefault(p.item,0)-p.before.bankItems.getOrDefault(p.item,0)
                ==p.before.count(p.item)-f.count(p.item);
        if("DYE_WIG".equals(p.action)) return f.count(BLONDE_WIG)>p.before.count(BLONDE_WIG);
        if("OPEN_ROUTE_DOOR".equals(p.action)) {
            net.runelite.api.TileObject door=Rs2GameObject.findObjectByLocation(p.target);
            return door==null||!Rs2GameObject.hasAction(door,"Open");
        }
        if("OPEN_ONION_GATE".equals(p.action)) {
            net.runelite.api.TileObject gate=Rs2GameObject.findObjectByLocation(p.target);
            return gate==null||!Rs2GameObject.hasAction(gate,"Open");
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
    private static net.runelite.api.TileObject nearbyFire(WorldPoint tile,int radius) {
        if(tile==null) return null;
        return Rs2GameObject.getAll(o->o!=null,tile,radius).stream()
            .filter(o->o.getWorldLocation()!=null&&o.getWorldLocation().distanceTo(tile)<=radius)
            .filter(o->"fire".equalsIgnoreCase(Rs2GameObject.getCompositionName(o).orElse("")))
            .findFirst().orElse(null);
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
            p.setProperty("loginError",loginError);
            p.setProperty("loginAttempts",Integer.toString(loginAttempts));
            p.setProperty("welcomeAttempts",Integer.toString(welcomeAttempts));
            p.setProperty("disconnectAttempts",Integer.toString(disconnectAttempts));
            p.setProperty("quest",f==null||f.quest==null?"UNKNOWN":f.quest.name());
            p.setProperty("varp273",Integer.toString(f==null?-1:f.varp));
            p.setProperty("widgetSelected",Boolean.toString(f!=null&&f.widgetSelected));
            p.setProperty("selectedWidgetId",Integer.toString(f==null?-1:f.selectedWidgetId));
            p.setProperty("selectedWidgetParentId",Integer.toString(f==null?-1:f.selectedWidgetParentId));
            p.setProperty("selectedWidgetItemId",Integer.toString(f==null?-1:f.selectedWidgetItemId));
            p.setProperty("selectedWidgetName",f==null?"":f.selectedWidgetName);
            p.setProperty("selectedWidgetText",f==null?"":truncate(f.selectedWidgetText,160));
            p.setProperty("shopWidgetPresent",Boolean.toString(f!=null&&f.shopWidgetPresent));
            p.setProperty("shopWidgetHidden",Boolean.toString(f==null||f.shopWidgetHidden));
            p.setProperty("shantayPathDiagnostic",shantayPathDiagnostic);
            p.setProperty("loginIndex",Integer.toString(f==null?-1:f.loginIndex));
            p.setProperty("position",f==null||f.pos==null?"UNKNOWN":f.pos.toString());
            p.setProperty("game",f==null||f.game==null?"UNKNOWN":f.game.name());
            p.setProperty("gameState",f==null||f.game==null?"UNKNOWN":f.game.name());
            p.setProperty("world",Integer.toString(f==null?-1:f.world));
            p.setProperty("items",f==null?"{}":f.items.toString());
            p.setProperty("dialogueContinue",Boolean.toString(f!=null&&f.continuePrompt));
            p.setProperty("inDialogue",Boolean.toString(f!=null&&f.inDialogue));
            p.setProperty("shopOpen",Boolean.toString(f!=null&&f.shop));
            p.setProperty("dialogueText",f==null?"":truncate(f.dialogue,512));
            p.setProperty("dialogueOptions",f==null?"":truncate(f.options,512));
            p.setProperty("continueRetryUsed",Boolean.toString(continueRetryUsed));
            p.setProperty("pendingBeforeContinue",pending==null?"":Boolean.toString(pending.before.continuePrompt));
            p.setProperty("pendingBeforeDialogueText",pending==null?"":truncate(pending.before.dialogue,512));
            p.setProperty("pendingBeforeDialogueOptions",pending==null?"":truncate(pending.before.options,512));
            p.setProperty("bankInspected",Boolean.toString(bankInspected));
            p.setProperty("keySubmitted",Boolean.toString(keySubmitted));
            p.setProperty("hp",f==null?"":Integer.toString(f.hp));
            p.setProperty("maxHp",f==null?"":Integer.toString(f.maxHp));
            p.setProperty("combatLevel",f==null?"":Integer.toString(f.combatLevel));
            p.setProperty("inCombat",Boolean.toString(f!=null&&f.inCombat));
            p.setProperty("sourceItem",Integer.toString(sourceItem));
            p.setProperty("sourceGoal",Integer.toString(sourceGoal));
            p.setProperty("sourceAttempts",Integer.toString(sourceAttempts));
            p.setProperty("sourceSpent",Integer.toString(sourceSpent));
            p.setProperty("shantayOpenRetryUsed",Boolean.toString(shantayOpenRetryUsed));
            p.setProperty("shantayReachableTileRecoveryUsed",Boolean.toString(shantayReachableTileRecoveryUsed));
            p.setProperty("shantayReachableTradeUsed",Boolean.toString(shantayReachableTradeUsed));
            p.setProperty("shantayReachableTileTimerRecoveryUsed",Boolean.toString(shantayReachableTileTimerRecoveryUsed));
            p.setProperty("shantayPromoRecoveryAvailable",Boolean.toString(shantayPromoRecoveryAvailable));
            p.setProperty("shantayPromoRetryActive",Boolean.toString(shantayPromoRetryActive));
            p.setProperty("shantayPromoRetryUsed",Boolean.toString(shantayPromoRetryUsed));
            p.setProperty("shantayApproachTarget",shantayApproachTarget==null?"":shantayApproachTarget.toString());
            p.setProperty("memberPromoDismissAttempted",Boolean.toString(memberPromoDismissAttempted));
            p.setProperty("memberPromoVisible",Boolean.toString(f!=null&&f.memberPromoVisible));
            p.setProperty("memberPromoDiagnostic",f==null?"":truncate(f.memberPromoDiagnostic,2400));
            p.setProperty("sourceStage",geStage);
            p.setProperty("onionApproach",onionApproach==null?"":onionApproach.toString());
            p.setProperty("onionStepAttempts",Integer.toString(onionStepAttempts));
            p.setProperty("onionFailedAttemptRecovered",Boolean.toString(onionFailedAttemptRecovered));
            p.setProperty("onionTimeoutRecovered",Boolean.toString(onionTimeoutRecovered));
            p.setProperty("onionSecondPickCapRecovered",Boolean.toString(onionSecondPickCapRecovered));
            p.setProperty("ashesFallbackRecovered",Boolean.toString(ashesFallbackRecovered));
            p.setProperty("ashesLogSourceRecovered",Boolean.toString(ashesLogSourceRecovered));
            p.setProperty("ashesFireTile",ashesFireTile==null?"":ashesFireTile.toString());
            p.setProperty("ashesFireStartedAt",Long.toString(ashesFireStartedAt));
            p.setProperty("ashesFireAttempts",Integer.toString(ashesFireAttempts));
            p.setProperty("ashesTinderboxPurchaseAttempts",Integer.toString(ashesTinderboxPurchaseAttempts));
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
    private static String truncate(String text,int max) {
        if(text==null||text.length()<=max) return text==null?"":text;
        return text.substring(0,max)+"…";
    }

    // Embedded from questcommon/BronzeBarSource.java for the script hot-swap class loader.
private static final class BronzeBarSource {
    public enum Outcome { WAIT, PENDING, PROVED, COMPLETE, HOLD }

    public static final class Result {
        public final Outcome outcome;
        public final String detail;
        private Result(Outcome outcome, String detail) {
            this.outcome = outcome;
            this.detail = detail;
        }
        public boolean ownsTick() { return true; }
        @Override public String toString() { return outcome + ": " + detail; }
    }

    private enum Action { WALK_MINE, MINE_COPPER, MINE_TIN, WALK_FURNACE, OPEN_FURNACE, CLICK_BRONZE }

    public static final WorldPoint RIMMINGTON_MINE_WAYPOINT = new WorldPoint(2985, 3238, 0);
    public static final WorldPoint AL_KHARID_FURNACE_WAYPOINT = new WorldPoint(3273, 3184, 0);
    private static final int MINE_RADIUS = 22;
    private static final int FURNACE_RADIUS = 12;
    private static final int STATE_SCHEMA = 1;

    private static final class Frame {
        long at;
        GameState game;
        WorldPoint pos;
        int world, hp, maxHp, mining, smithing, miningXp, smithingXp;
        int copper, tin, bronze, coins, emptySlots;
        boolean inventoryKnown, pickaxe, membersWorld, worldTypeKnown;
        boolean selectedWidget, membershipPromo, skillmultiVisible;
        Choice bronzeChoice;
        String bronzeChoiceDiagnostic = "";
    }

    private static final class Choice {
        final int widgetId, index, itemId;
        final Rectangle bounds;
        final String text;
        Choice(Widget widget) {
            widgetId = widget.getId();
            index = widget.getIndex();
            itemId = widget.getItemId();
            bounds = new Rectangle(widget.getBounds());
            text = safe(widget.getText()) + "|" + safe(widget.getName());
        }
        String key() { return widgetId + ":" + index + ":" + itemId + ":" + bounds; }
    }

    private static final class Pending {
        final Action action;
        final WorldPoint target, beforePos;
        final int beforeCopper, beforeTin, beforeBronze, beforeCoins;
        final int beforeMiningXp, beforeSmithingXp, beforeHp;
        final long deadline;
        Pending(Action action, Frame f, WorldPoint target, long timeoutMs) {
            this(action, target, f.pos, f.copper, f.tin, f.bronze, f.coins,
                f.miningXp, f.smithingXp, f.hp, System.currentTimeMillis() + timeoutMs);
        }
        Pending(Action action, WorldPoint target, WorldPoint beforePos,
                int beforeCopper, int beforeTin, int beforeBronze, int beforeCoins,
                int beforeMiningXp, int beforeSmithingXp, int beforeHp, long deadline) {
            this.action = action;
            this.target = target;
            this.beforePos = beforePos;
            this.beforeCopper = beforeCopper;
            this.beforeTin = beforeTin;
            this.beforeBronze = beforeBronze;
            this.beforeCoins = beforeCoins;
            this.beforeMiningXp = beforeMiningXp;
            this.beforeSmithingXp = beforeSmithingXp;
            this.beforeHp = beforeHp;
            this.deadline = deadline;
        }
    }

    private final int minimumHp;
    private Pending pending;
    private boolean held, furnaceOpenProved;
    private String holdReason = "";
    private long missingRockSince, missingFurnaceSince;

    /** The caller supplies a minimum HP; this is not a combat-safety assessment. */
    public BronzeBarSource(int minimumHp) {
        if (minimumHp < 1) throw new IllegalArgumentException("minimumHp");
        this.minimumHp = minimumHp;
    }

    public Result tick(boolean ownsInput) {
        if (!ownsInput) return result(Outcome.WAIT, "quest input belongs to another plugin");
        if (held) return result(Outcome.HOLD, holdReason);
        Frame f = Microbot.getClientThread().invoke((java.util.function.Supplier<Frame>) BronzeBarSource::observe);
        if (f == null || f.game != GameState.LOGGED_IN || f.pos == null || !f.inventoryKnown)
            return result(Outcome.WAIT, "wait for logged-in client and complete inventory frame; pending retained");
        if (!f.worldTypeKnown || f.membersWorld)
            return hold("F2P source requires a confirmed free world; world=" + f.world + " members=" + f.membersWorld);
        if (f.pos.getPlane() != 0) return hold("Al Kharid source requires ground plane; pos=" + f.pos);
        if (f.membershipPromo) return hold("Membership prompt appeared on F2P source route; stop and inspect access");
        if (f.hp < minimumHp) return hold("Insufficient HP for bronze source; hp=" + f.hp + " minimum=" + minimumHp);
        if (pending != null) return resolve(pending, f);
        if (f.bronze > 0) return result(Outcome.COMPLETE, "Bronze bar 2349 observed in inventory; owner verifies quest use separately");
        if (f.selectedWidget || Rs2Bank.isOpen() || Rs2Dialogue.isInDialogue()
            || Rs2Dialogue.hasContinue() || Rs2Dialogue.hasSelectAnOption())
            return hold("Blocking selection/bank/dialogue before bronze source; selected=" + f.selectedWidget);
        if (f.mining < 1 || f.smithing < 1)
            return hold("Bronze route requires Mining 1 and Smithing 1; mining=" + f.mining + " smithing=" + f.smithing);
        int missing = (f.copper > 0 ? 0 : 1) + (f.tin > 0 ? 0 : 1);
        if (missing > 0 && !f.pickaxe) return hold("No bronze pickaxe 1265 in inventory/equipment; no purchase assumed");
        if (f.emptySlots < missing) return hold("Need " + missing + " free inventory slots for ore; available=" + f.emptySlots);

        if (missing > 0) return mineTick(f, f.copper == 0 ? ItemID.COPPER_ORE : ItemID.TIN_ORE);
        return furnaceTick(f);
    }

    private Result mineTick(Frame f, int oreId) {
        if (f.pos.distanceTo(RIMMINGTON_MINE_WAYPOINT) > MINE_RADIUS) {
            if (!Rs2Walker.walkTo(RIMMINGTON_MINE_WAYPOINT))
                return hold("Walk to F2P Rimmington mine rejected; pos=" + f.pos + " target=" + RIMMINGTON_MINE_WAYPOINT);
            return dispatched(Action.WALK_MINE, f, RIMMINGTON_MINE_WAYPOINT, 120_000L);
        }
        TileObject rock = nearestRock(f.pos, oreId);
        if (rock == null) {
            if (missingRockSince == 0) missingRockSince = f.at;
            if (f.at - missingRockSince < 30_000L)
                return result(Outcome.WAIT, "waiting for live F2P " + oreName(oreId) + " Mine rock near " + f.pos);
            return hold("No live " + oreName(oreId) + " rock with Mine action near bronze source after 30s; pos=" + f.pos);
        }
        missingRockSince = 0;
        WorldPoint rockTile = rock.getWorldLocation();
        if (!Rs2GameObject.interact(rock, "Mine"))
            return hold("Mine dispatch rejected for live " + oreName(oreId) + " rock id=" + rock.getId() + " at=" + rockTile);
        return dispatched(oreId == ItemID.COPPER_ORE ? Action.MINE_COPPER : Action.MINE_TIN,
            f, rockTile, 60_000L);
    }

    private Result furnaceTick(Frame f) {
        if (f.pos.distanceTo(AL_KHARID_FURNACE_WAYPOINT) > FURNACE_RADIUS) {
            if (!Rs2Walker.walkTo(AL_KHARID_FURNACE_WAYPOINT))
                return hold("Walk to F2P Al Kharid furnace rejected; pos=" + f.pos + " target=" + AL_KHARID_FURNACE_WAYPOINT);
            return dispatched(Action.WALK_FURNACE, f, AL_KHARID_FURNACE_WAYPOINT, 120_000L);
        }
        if (!furnaceOpenProved) {
            TileObject furnace = nearestFurnace(f.pos);
            if (furnace == null) {
                if (missingFurnaceSince == 0) missingFurnaceSince = f.at;
                if (f.at - missingFurnaceSince < 15_000L)
                    return result(Outcome.WAIT, "waiting for live Furnace with Smelt action near " + f.pos);
                return hold("No live F2P Furnace with Smelt action near Al Kharid waypoint after 15s; pos=" + f.pos);
            }
            missingFurnaceSince = 0;
            if (!Rs2GameObject.interact(furnace, "Smelt"))
                return hold("Smelt dispatch rejected for live Furnace id=" + furnace.getId() + " at=" + furnace.getWorldLocation());
            return dispatched(Action.OPEN_FURNACE, f, furnace.getWorldLocation(), 12_000L);
        }
        if (!f.skillmultiVisible)
            return hold("Proved furnace interface disappeared before Bronze bar selection; no blind re-open");
        if (f.bronzeChoice == null)
            return hold("No unique Bronze bar option in live Skillmulti 270; " + f.bronzeChoiceDiagnostic);
        Choice fresh = Microbot.getClientThread().invoke((java.util.function.Supplier<Choice>) () -> {
            Client client = Microbot.getClient();
            if (client == null || client.getGameState() != GameState.LOGGED_IN) return null;
            Choice c = findBronzeChoice(client).choice;
            return c != null && c.key().equals(f.bronzeChoice.key()) ? c : null;
        });
        if (fresh == null) return hold("Bronze bar option changed before click; no action sent");
        // Mouse.click accepts an immutable client-thread bounds snapshot; no Widget
        // access occurs on this scheduler thread. A click is only a dispatch.
        Microbot.getMouse().click(new Rectangle(fresh.bounds));
        return dispatched(Action.CLICK_BRONZE, f, AL_KHARID_FURNACE_WAYPOINT, 15_000L);
    }

    private Result resolve(Pending p, Frame f) {
        if (f.hp < p.beforeHp)
            return hold("HP fell during " + p.action + " near bronze source; hp=" + p.beforeHp + "->" + f.hp);
        if (f.coins < p.beforeCoins)
            return hold("Unexpected coin debit on free source route during " + p.action + "; coins=" + p.beforeCoins + "->" + f.coins);
        boolean proved;
        switch (p.action) {
            case WALK_MINE:
                proved = f.pos.distanceTo(RIMMINGTON_MINE_WAYPOINT) <= MINE_RADIUS;
                break;
            case WALK_FURNACE:
                proved = f.pos.distanceTo(AL_KHARID_FURNACE_WAYPOINT) <= FURNACE_RADIUS;
                break;
            case MINE_COPPER:
                proved = f.copper > p.beforeCopper && f.miningXp > p.beforeMiningXp;
                break;
            case MINE_TIN:
                proved = f.tin > p.beforeTin && f.miningXp > p.beforeMiningXp;
                break;
            case OPEN_FURNACE:
                proved = f.skillmultiVisible || smeltProof(p, f);
                if (proved && f.skillmultiVisible) furnaceOpenProved = true;
                break;
            case CLICK_BRONZE:
                proved = smeltProof(p, f);
                break;
            default:
                proved = false;
        }
        if (proved) {
            pending = null;
            if (f.bronze > p.beforeBronze && smeltProof(p, f))
                return result(Outcome.COMPLETE, "Proved bronze bar + ore consumption + Smithing XP after " + p.action);
            return result(Outcome.PROVED, p.action + " proved; next tick may choose one new action");
        }
        if (f.at >= p.deadline)
            return hold("Unproved " + p.action + "; before=" + p.beforePos + " copper/tin/bar="
                + p.beforeCopper + "/" + p.beforeTin + "/" + p.beforeBronze
                + " now=" + f.pos + " " + f.copper + "/" + f.tin + "/" + f.bronze
                + " miningXp=" + p.beforeMiningXp + "->" + f.miningXp
                + " smithingXp=" + p.beforeSmithingXp + "->" + f.smithingXp
                + " interface=" + f.skillmultiVisible + "; no automatic replay");
        return result(Outcome.PENDING, "Awaiting " + p.action + " proof; deadline=" + p.deadline);
    }

    private static boolean smeltProof(Pending p, Frame f) {
        return f.bronze > p.beforeBronze && f.copper < p.beforeCopper
            && f.tin < p.beforeTin && f.smithingXp > p.beforeSmithingXp;
    }

    private Result dispatched(Action action, Frame f, WorldPoint target, long timeoutMs) {
        pending = new Pending(action, f, target, timeoutMs);
        return result(Outcome.PENDING, "Dispatched " + action + " once; target=" + target + "; proof required next tick");
    }

    private Result hold(String reason) {
        pending = null;
        held = true;
        holdReason = reason;
        return result(Outcome.HOLD, reason);
    }

    private static Result result(Outcome outcome, String detail) { return new Result(outcome, detail); }
    private static String oreName(int oreId) { return oreId == ItemID.COPPER_ORE ? "copper" : "tin"; }

    private static TileObject nearestRock(WorldPoint player, int oreId) {
        int first = oreId == ItemID.COPPER_ORE ? ObjectID.COPPERROCK1 : ObjectID.TINROCK1;
        int second = oreId == ItemID.COPPER_ORE ? ObjectID.COPPERROCK2 : ObjectID.TINROCK2;
        return Rs2GameObject.getAll(o -> o != null, player, MINE_RADIUS * 2).stream()
            .filter(o -> o != null && o.getWorldLocation() != null
                && o.getWorldLocation().getPlane() == 0
                && o.getWorldLocation().distanceTo(RIMMINGTON_MINE_WAYPOINT) <= MINE_RADIUS
                && (o.getId() == first || o.getId() == second)
                && Rs2GameObject.hasAction(o, "Mine"))
            .min(Comparator.comparingInt(o -> o.getWorldLocation().distanceTo(player)))
            .orElse(null);
    }

    private static TileObject nearestFurnace(WorldPoint player) {
        return Rs2GameObject.getAll(o -> o != null, player, FURNACE_RADIUS * 2).stream()
            .filter(o -> o != null && o.getWorldLocation() != null
                && o.getWorldLocation().getPlane() == 0
                && o.getWorldLocation().distanceTo(AL_KHARID_FURNACE_WAYPOINT) <= FURNACE_RADIUS
                && "furnace".equalsIgnoreCase(Rs2GameObject.getCompositionName(o).orElse(""))
                && Rs2GameObject.hasAction(o, "Smelt"))
            .min(Comparator.comparingInt(o -> o.getWorldLocation().distanceTo(player)))
            .orElse(null);
    }

    private static Frame observe() {
        Client client = Microbot.getClient();
        Frame f = new Frame();
        f.at = System.currentTimeMillis();
        if (client == null) return f;
        f.game = client.getGameState();
        if (f.game != GameState.LOGGED_IN || client.getLocalPlayer() == null) return f;
        f.world = client.getWorld();
        f.worldTypeKnown = client.getWorldType() != null;
        f.membersWorld = f.worldTypeKnown && client.getWorldType().contains(WorldType.MEMBERS);
        f.pos = client.getLocalPlayer().getWorldLocation();
        f.hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
        f.maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
        f.mining = client.getRealSkillLevel(Skill.MINING);
        f.smithing = client.getRealSkillLevel(Skill.SMITHING);
        f.miningXp = client.getSkillExperience(Skill.MINING);
        f.smithingXp = client.getSkillExperience(Skill.SMITHING);
        f.selectedWidget = client.isWidgetSelected();
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        f.inventoryKnown = inventory != null && inventory.getItems() != null;
        if (!f.inventoryKnown) return f;
        Item[] items = inventory.getItems();
        f.emptySlots = Math.max(0, 28 - items.length);
        for (Item item : items) {
            if (item == null || item.getId() < 0) { f.emptySlots++; continue; }
            int quantity = Math.max(1, item.getQuantity());
            if (item.getId() == ItemID.COPPER_ORE) f.copper += quantity;
            else if (item.getId() == ItemID.TIN_ORE) f.tin += quantity;
            else if (item.getId() == ItemID.BRONZE_BAR) f.bronze += quantity;
            else if (item.getId() == ItemID.COINS) f.coins += quantity;
            else if (item.getId() == ItemID.BRONZE_PICKAXE) f.pickaxe = true;
        }
        if (equipment != null && equipment.getItems() != null)
            for (Item item : equipment.getItems())
                if (item != null && item.getId() == ItemID.BRONZE_PICKAXE) f.pickaxe = true;
        Widget promo = client.getWidget(InterfaceID.MembershipBenefitsPrompt.CONTENT);
        f.membershipPromo = promo != null && !promo.isHidden();
        ChoiceScan choiceScan = findBronzeChoice(client);
        f.skillmultiVisible = choiceScan.skillmultiVisible;
        f.bronzeChoice = choiceScan.choice;
        f.bronzeChoiceDiagnostic = choiceScan.diagnostic;
        return f;
    }

    private static final class ChoiceScan {
        final boolean skillmultiVisible;
        final Choice choice;
        final String diagnostic;
        ChoiceScan(boolean visible, Choice choice, String diagnostic) {
            this.skillmultiVisible = visible;
            this.choice = choice;
            this.diagnostic = diagnostic;
        }
    }

    private static ChoiceScan findBronzeChoice(Client client) {
        if (client == null) return new ChoiceScan(false, null, "no client");
        List<Choice> matches = new ArrayList<>();
        Set<Widget> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean visible = false;
        for (int child = 0; child <= 33; child++) {
            Widget widget = client.getWidget(InterfaceID.SKILLMULTI, child);
            if (widget == null || widget.isHidden()) continue;
            Rectangle bounds = widget.getBounds();
            if (bounds == null || bounds.width <= 0 || bounds.height <= 0) continue;
            visible = true;
            collectBronze(widget, seen, matches, client.getCanvasWidth(), client.getCanvasHeight(), 0);
        }
        Choice choice = matches.size() == 1 ? matches.get(0) : null;
        StringBuilder b = new StringBuilder("Skillmulti270 visible=").append(visible)
            .append(" exactBronzeCandidates=").append(matches.size());
        for (int i = 0; i < Math.min(4, matches.size()); i++)
            b.append(" [").append(matches.get(i).key()).append(' ').append(matches.get(i).text).append(']');
        return new ChoiceScan(visible, choice, b.toString());
    }

    private static void collectBronze(Widget w, Set<Widget> seen, List<Choice> matches,
                                      int canvasWidth, int canvasHeight, int depth) {
        if (w == null || depth > 10 || !seen.add(w) || w.isHidden()) return;
        Rectangle b = w.getBounds();
        if (b != null && b.width >= 8 && b.height >= 8 && b.x >= 0 && b.y >= 0
            && b.x + b.width <= canvasWidth && b.y + b.height <= canvasHeight) {
            String text = normalize(w.getText()), name = normalize(w.getName());
            if (w.getItemId() == ItemID.BRONZE_BAR
                || "bronze bar".equals(text) || "bronze bar".equals(name))
                matches.add(new Choice(w));
        }
        for (Widget[] children : new Widget[][] {w.getChildren(), w.getDynamicChildren(), w.getStaticChildren()})
            if (children != null)
                for (Widget child : children)
                    collectBronze(child, seen, matches, canvasWidth, canvasHeight, depth + 1);
    }

    private static String normalize(String value) {
        return safe(value).replaceAll("<[^>]*>", "").trim().toLowerCase(Locale.ROOT);
    }
    private static String safe(String value) { return value == null ? "" : value; }

    /** JDK primitives only, so state can cross the hot-swap class loader. */
    public Map<String, Object> exportState() {
        Map<String, Object> s = new HashMap<>();
        s.put("schema", STATE_SCHEMA);
        s.put("held", held);
        s.put("holdReason", holdReason);
        s.put("furnaceOpenProved", furnaceOpenProved);
        s.put("missingRockSince", missingRockSince);
        s.put("missingFurnaceSince", missingFurnaceSince);
        s.put("pendingAction", pending == null ? "" : pending.action.name());
        if (pending != null) {
            s.put("deadline", pending.deadline);
            putPoint(s, "target", pending.target);
            putPoint(s, "beforePos", pending.beforePos);
            s.put("beforeCopper", pending.beforeCopper);
            s.put("beforeTin", pending.beforeTin);
            s.put("beforeBronze", pending.beforeBronze);
            s.put("beforeCoins", pending.beforeCoins);
            s.put("beforeMiningXp", pending.beforeMiningXp);
            s.put("beforeSmithingXp", pending.beforeSmithingXp);
            s.put("beforeHp", pending.beforeHp);
        }
        return s;
    }

    /** Restore after the owner quiesces its tick. In-flight actions are verified, never replayed. */
    public void restoreState(Map<String, Object> s) {
        if (s == null || number(s, "schema", -1) != STATE_SCHEMA) {
            hold("BronzeBarSource hot-swap state missing/incompatible; inspect current scene before restart");
            return;
        }
        held = Boolean.TRUE.equals(s.get("held"));
        holdReason = safe((String) s.get("holdReason"));
        furnaceOpenProved = Boolean.TRUE.equals(s.get("furnaceOpenProved"));
        missingRockSince = numberLong(s, "missingRockSince", 0L);
        missingFurnaceSince = numberLong(s, "missingFurnaceSince", 0L);
        String action = safe((String) s.get("pendingAction"));
        if (action.isEmpty()) { pending = null; return; }
        try {
            pending = new Pending(Action.valueOf(action), point(s, "target"), point(s, "beforePos"),
                number(s, "beforeCopper", -1), number(s, "beforeTin", -1),
                number(s, "beforeBronze", -1), number(s, "beforeCoins", -1),
                number(s, "beforeMiningXp", -1), number(s, "beforeSmithingXp", -1),
                number(s, "beforeHp", -1), numberLong(s, "deadline", -1L));
            if (pending.beforePos == null || pending.deadline < 0 || pending.beforeCopper < 0
                || pending.beforeTin < 0 || pending.beforeBronze < 0 || pending.beforeCoins < 0)
                hold("Incomplete in-flight bronze source state after hot-swap; no replay");
        } catch (RuntimeException ex) {
            hold("Invalid in-flight bronze source state after hot-swap; no replay: " + ex.getClass().getSimpleName());
        }
    }

    private static void putPoint(Map<String, Object> s, String key, WorldPoint point) {
        if (point == null) return;
        s.put(key + "X", point.getX());
        s.put(key + "Y", point.getY());
        s.put(key + "Plane", point.getPlane());
    }
    private static WorldPoint point(Map<String, Object> s, String key) {
        int x = number(s, key + "X", -1), y = number(s, key + "Y", -1);
        int plane = number(s, key + "Plane", -1);
        return x < 0 || y < 0 || plane < 0 ? null : new WorldPoint(x, y, plane);
    }
    private static int number(Map<String, Object> s, String key, int fallback) {
        Object value = s.get(key);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }
    private static long numberLong(Map<String, Object> s, String key, long fallback) {
        Object value = s.get(key);
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }
}

}

