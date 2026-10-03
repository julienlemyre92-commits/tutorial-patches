package net.runelite.client.plugins.microbot.corsaircurse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Objects;
import java.util.HexFormat;
import java.util.StringJoiner;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.questcommon.QuestRandomEventDismiss;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.questcommon.navigation.NavigationGoal;
import net.runelite.client.plugins.microbot.questcommon.training.CombatTrainingGoal;
import net.runelite.client.plugins.microbot.questcommon.deathplugin.DeathRecoveryBridge;
import net.runelite.client.plugins.microbot.questcommon.preparation.PreparationGoal;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceClient;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceHub;
import net.runelite.client.plugins.microbot.questcommon.services.QuestReloadParticipant;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.events.WelcomeScreenEvent;
import net.runelite.client.plugins.microbot.util.input.InputArbiter;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.LoginManager;
import net.runelite.client.plugins.microbot.util.walker.Rs2MiniMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Isolated Corsair Curse candidate. The live quest/varbits, never a click, select each next step. */
public final class CorsairCurseScript extends Script implements QuestReloadParticipant {
    private static final Logger LOG=LoggerFactory.getLogger(CorsairCurseScript.class);
    public static final int BUILD_NUMBER=58;
    private static final int SPADE=ItemID.SPADE, TINDER=ItemID.TINDERBOX, RELIC=ItemID.CORSCURS_RELIC;
    private static final int[] FOOD={ItemID.LOBSTER,ItemID.TUNA,ItemID.SALMON,ItemID.TROUT};
    private static final WorldPoint FARM=pt(3030,3273,0), DOCK=pt(2910,3226,0);
    private static final WorldPoint ITHOI=pt(2529,2840,1), ARSEN=pt(2554,2859,1);
    private static final WorldPoint COLIN=pt(2558,2858,1), GNOCCI=pt(2545,2863,1);
    private static final WorldPoint TOCK=pt(2574,2835,1), TESS=pt(2012,9006,1);
    private static final WorldPoint ITHOI_RAMP=pt(2531,2834,0), ARSEN_STAIRS=pt(2555,2856,0);
    private static final WorldPoint GNOCCI_STAIRS=pt(2549,2862,0), SHIP_PLANK=pt(2578,2839,0);
    private static final WorldPoint CAVE=pt(2523,2861,0), SAND=pt(2504,2840,0);
    private static final WorldPoint SPADE_OBJECT=pt(2552,2846,0), DRIFTWOOD=pt(2531,2838,0);
    private static final Path HOME=Paths.get(System.getProperty("user.home"),".runelite","corsaircurse");
    private static final Path STATUS=HOME.resolve("status.properties"), CONTROL=HOME.resolve("control.properties");
    private static final Path ACTION=HOME.resolve("pending-action.properties");
    private enum Kind { NPC, OBJECT, DIG, ITEM_OBJECT, COMBAT, WAIT }
    private record Plan(String key,Kind kind,int id,WorldPoint at,int item) { }
    private static final class Frame {
        String game="NO_CLIENT",quest="UNKNOWN",dialogue="",accountKey="";
        int loginIndex=-1,progress=-1,thief=-1,cabin=-1,cook=-1,navigator=-1;
        int hp,hpMax,combat,prayer,weapon=-1,armour,magicDefence,enemyId=-1;
        boolean equipmentStatsKnown;
        int weaponScore,requiredWeaponScore;
        final Set<Integer> bossWeapons=new HashSet<>();
        long accountHash,combatXp,attackXp,strengthXp,defenceXp,rangedXp,magicXp,hpXp;
        int world;
        final Set<Integer> equipment=new java.util.HashSet<>();
        WorldPoint pos,templatePos; boolean instanced,invReady,inDialogue,continueVisible,bankOpen,benignCowScene;
        final Map<Integer,Integer> items=new HashMap<>();
        final List<String> options=new ArrayList<>();
        int count(int id){return items.getOrDefault(id,0);}
        int food(){int n=0;for(int id:FOOD)n+=count(id);return n;}
        int healing(){return count(ItemID.LOBSTER)*12+count(ItemID.TUNA)*10
            +count(ItemID.SALMON)*9+count(ItemID.TROUT)*7;}
        int foodId(){for(int id:FOOD)if(count(id)>0)return id;return -1;}
        boolean loggedIn(){return "LOGGED_IN".equals(game) && pos!=null;}
        boolean ithoiEquipmentReady(){return weapon>0 && equipmentStatsKnown && magicDefence>=0
            && requiredWeaponScore>0 && weaponScore>=requiredWeaponScore;}
        String signature(){return progress+":"+thief+":"+cabin+":"+cook+":"+navigator+":"+pos+":"+items+":"+inDialogue+":"+continueVisible+":"+dialogue+":"+options+":"+enemyId;}
    }
    private static final class Pending {
        final String key,before; final Kind kind; final WorldPoint pos; final int item,count,progress;
        final int thief,cabin,cook,navigator; final boolean inDialogue,continueVisible;
        final String dialogue; final List<String> options;
        final long at=System.currentTimeMillis();
        Pending(Plan p,Frame f){key=p.key();kind=p.kind();before=f.signature();pos=f.pos;
            item=p.item();count=f.count(item);progress=f.progress;
            thief=f.thief;cabin=f.cabin;cook=f.cook;navigator=f.navigator;
            inDialogue=f.inDialogue;continueVisible=f.continueVisible;
            dialogue=f.dialogue;options=List.copyOf(f.options);}
    }
    private CorsairCurseConfig config; private BooleanSupplier ownsInput;
    private final QuestRandomEventDismiss randomDismiss=new QuestRandomEventDismiss();
    private final QuestServiceClient services=new QuestServiceClient(new QuestServiceClient.InputState(){
        public void requestStop(){serviceIdle=true;}
        public boolean isIdle(){return serviceIdle && pending==null;}
    });
    private volatile boolean serviceIdle=true,stopped;
    private String stage="START",error="",classHash="UNKNOWN",accountKey="";
    private WorldPoint navigationTarget;
    private int navigationStartDistance, navigationDeadlineReplans;
    private int navigationRadius;
    private Pending pending; private Map<String,Object> reloadState;
    private boolean prepared,finished,logoutIssued,combatStarted,combatRetreat;
    private boolean cowhideCollected,cowhideBankPending;
    private long cowhideStarted,cowhidePendingAt;
    private int cowhideBefore,cowhidePickups;
    private volatile boolean providerReloadPaused;
    private boolean bossSafetyActive,bossEatAttempted,bossEscapeAttempted,bossCancelRequested;
    private int bossMealsProved;
    private int bossRetreats,failedBossWeapon;
    private boolean bossRetryLoaded;
    private int survivalMeals;
    private boolean prepAttempted,deathRegistered;
    private int trainingRequests;
    private int trainingNoProgressRequests;
    private long trainingStartXp=-1;
    private long trainingRetryAfter;
    private String trainingRequestId="";
    private String trainingMaintenanceProof="";
    private boolean bankArrivalRetryUsed;
    private String heldTrainingReason="";
    private TrainingMaintenanceProtocol.Receipt heldTrainingReceipt;
    private boolean farmSurveyLogged;
    private int navProbeSamples;
    private long navProbeLastAt;
    private volatile boolean deathYield;
    private volatile DeathRecoveryBridge.Outcome deathOutcome;
    private volatile String deathDetail="";
    private final DeathRecoveryBridge.Owner deathOwner=new DeathRecoveryBridge.Owner(){
        public void requestYield(){synchronized(CorsairCurseScript.this){deathYield=true;pending=null;}}
        public boolean isQuiescent(){return gameplayInputAllowed() && deathYield
            && pending==null && !services.hasPendingRequest();}
        public OptionalInt officeFeeQuote(){return OptionalInt.empty();}
        public void recoveryFinished(DeathRecoveryBridge.Outcome outcome,String detail){
            deathOutcome=outcome;deathDetail=detail==null?"":detail;
        }
    };
    private long accountHash,loginAt,welcomeAt,logoutAt,cutsceneAt,combatAt;
    private int loginAttempts,welcomeAttempts,selectedWorld;
    private final Map<String,Integer> failures=new HashMap<>();

    public int runtimeBuild(){return BUILD_NUMBER;}
    public void restoreReloadState(Map<String,Object> state){reloadState=state;}
    public synchronized Map<String,Object> quiesceForReload(){
        if(providerReloadPaused || pending!=null || bossSafetyActive || cowhidePendingAt!=0
            || services.blocksQuestInput() || Files.isRegularFile(ACTION))
            throw new IllegalStateException("Quest action or shared service still owns input");
        Map<String,Object> state=new HashMap<>();
        state.put("accountHash",accountHash);state.put("accountKey",accountKey);
        state.put("finished",finished);state.put("prepared",prepared);
        state.put("cowhideCollected",cowhideCollected);state.put("cowhideBankPending",cowhideBankPending);
        state.put("cowhideStarted",cowhideStarted);state.put("cowhidePickups",cowhidePickups);
        // Exported only after quiescence above: an ended preparation request
        // may be replanned by the replacement, never an in-flight transaction.
        state.put("prepAttempted",prepared && prepAttempted);
        state.put("bankArrivalRetryUsed",bankArrivalRetryUsed);
        state.put("trainingRequests",trainingRequests);
        state.put("trainingNoProgressRequests",trainingNoProgressRequests);
        state.put("trainingStartXp",trainingStartXp);
        state.put("trainingRetryAfter",trainingRetryAfter);
        state.put("trainingRequestId",trainingRequestId);
        state.put("trainingMaintenanceProof",trainingMaintenanceProof);
        state.put("survivalMeals",survivalMeals);
        shutdown();return state;
    }
    @Override public synchronized boolean pauseForProviderReload(){
        if(stopped || providerReloadPaused || pending!=null || bossSafetyActive
            || deathYield || DeathRecoveryBridge.mustYield()
            || services.hasPendingRequest() || QuestServiceHub.mustYield()
            || Files.isRegularFile(ACTION))return false;
        providerReloadPaused=true;return true;
    }
    @Override public synchronized boolean providerReloadIdle(){
        return providerReloadPaused && !stopped && pending==null && !bossSafetyActive
            && !deathYield && !DeathRecoveryBridge.mustYield()
            && !services.hasPendingRequest() && !QuestServiceHub.mustYield()
            && !Files.isRegularFile(ACTION);
    }
    @Override public synchronized void resumeAfterProviderReload(){
        // The coordinator calls this only after the guarded reload boundary.
        // Replan an acknowledged unavailable route; never clear uncertain actions/HOLDs.
        if(providerReloadPaused && pending==null && !Files.exists(ACTION)
            && !services.hasPendingRequest() && !QuestServiceHub.mustYield()
            && error.startsWith("Shared service NAVIGATION UNAVAILABLE:")){
            LOG.info("[CorsairCurse] REPLAN_AFTER_PROVIDER_UPDATE prior={}",error);
            error="";stage="REPLAN_AFTER_PROVIDER_UPDATE";
        }
        providerReloadPaused=false;
    }
    public boolean run(CorsairCurseConfig cfg,BooleanSupplier owner){
        if(isRunning())return true;
        config=cfg;ownsInput=owner;stopped=false;stage="START";error="";
        classHash=classSha();
        if(reloadState!=null){
            Object hash=reloadState.get("accountHash");
            if(hash instanceof Number)accountHash=((Number)hash).longValue();
            Object key=reloadState.get("accountKey");if(key instanceof String)accountKey=(String)key;
            finished=Boolean.TRUE.equals(reloadState.get("finished"));
            cowhideCollected=Boolean.TRUE.equals(reloadState.get("cowhideCollected"));
            cowhideBankPending=Boolean.TRUE.equals(reloadState.get("cowhideBankPending"));
            if(reloadState.get("cowhideStarted") instanceof Number n)cowhideStarted=n.longValue();
            if(reloadState.get("cowhidePickups") instanceof Number n)cowhidePickups=n.intValue();
            prepared=Boolean.TRUE.equals(reloadState.get("prepared"));
            // Legacy builds exported an attempted but already-ended request
            // after death. Quiescent reload proves no transaction is in flight.
            prepAttempted=prepared && Boolean.TRUE.equals(reloadState.get("prepAttempted"));
            bankArrivalRetryUsed=Boolean.TRUE.equals(reloadState.get("bankArrivalRetryUsed"));
            Object requests=reloadState.get("trainingRequests");
            if(requests instanceof Number)trainingRequests=((Number)requests).intValue();
            Object stalled=reloadState.get("trainingNoProgressRequests");
            if(stalled instanceof Number)trainingNoProgressRequests=((Number)stalled).intValue();
            Object startXp=reloadState.get("trainingStartXp");
            if(startXp instanceof Number)trainingStartXp=((Number)startXp).longValue();
            Object retryAfter=reloadState.get("trainingRetryAfter");
            if(retryAfter instanceof Number)trainingRetryAfter=((Number)retryAfter).longValue();
            Object trainingId=reloadState.get("trainingRequestId");
            if(trainingId instanceof String)trainingRequestId=(String)trainingId;
            Object maintenanceProof=reloadState.get("trainingMaintenanceProof");
            if(maintenanceProof instanceof String)trainingMaintenanceProof=(String)maintenanceProof;
            Object meals=reloadState.get("survivalMeals");
            if(meals instanceof Number)survivalMeals=((Number)meals).intValue();
        }
        reloadState=null;
        if(Files.isRegularFile(ACTION))error="Unresolved previous action journal; reconcile before arming";
        LOG.info("[CorsairCurse] RUNNING_BUILD={} classSha256={}",BUILD_NUMBER,classHash);
        mainScheduledFuture=scheduledExecutorService.scheduleWithFixedDelay(this::tick,0,
            Math.max(500,Math.min(2000,cfg.tickDelay())),TimeUnit.MILLISECONDS);
        return true;
    }
    @Override public void shutdown(){
        stopped=true;services.cancel("quest stopped");
        if(deathRegistered && !DeathRecoveryBridge.mustYield()){
            DeathRecoveryBridge.unregister(deathOwner);deathRegistered=false;
        }
        if(mainScheduledFuture!=null)mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow();super.shutdown();
    }
    private synchronized void tick(){
        Frame f=null;
        if(stopped || Thread.currentThread().isInterrupted())return;
        try{
            if(providerReloadPaused){stage="WAIT_PROVIDER_RELOAD";return;}
            f=observe();
            if(!f.loggedIn() && (finished || Files.exists(HOME.resolve("gameplay-paused-after-quest.flag")))){
                stage="QUEST_FINISHED_LOGGED_OUT";return;
            }
            if(!f.loggedIn()){login(f);return;}
            loginAttempts=0;selectedWorld=0;
            if(f.accountHash==0 || f.accountKey.isBlank()){hold("Account identity unavailable");return;}
            if(accountHash==0){accountHash=f.accountHash;accountKey=f.accountKey;}
            else if(accountHash!=f.accountHash || !accountKey.equals(f.accountKey)){
                hold("Different account in client");return;
            }
            // Includes completion/logout and death recovery, not just quest actions.
            if(!gameplayInputAllowed()){
                if(services.hasPendingRequest())services.cancel("gameplay input disarmed");
                if(deathRegistered && !DeathRecoveryBridge.mustYield()
                    && DeathRecoveryBridge.unregister(deathOwner))deathRegistered=false;
                stage="WAIT_ARMED_OR_INPUT_OWNER";return;
            }
            if(!f.invReady){stage="WAIT_INVENTORY";return;}
            if(!bossRetryLoaded){
                Path retry=HOME.resolve("boss-retry-"+accountKey+".properties");
                if(Files.exists(retry)){
                    Properties r=new Properties();try(InputStream in=Files.newInputStream(retry)){r.load(in);}
                    if(!accountKey.equals(r.getProperty("account")))throw new IOException("Boss retry account mismatch");
                    bossRetreats=Integer.parseInt(r.getProperty("retreats"));
                    failedBossWeapon=Integer.parseInt(r.getProperty("weapon"));
                    if(bossRetreats<0 || bossRetreats>3)throw new IOException("Invalid boss retry budget");
                }
                bossRetryLoaded=true;
            }
            if(!farmSurveyLogged && near(f.pos,pt(3025,3307,0),45))farmSurvey(f);
            if("FINISHED".equals(f.quest)){
                if(!Files.exists(HOME.resolve("gameplay-paused-after-quest.flag")))
                    Files.writeString(HOME.resolve("gameplay-paused-after-quest.flag"),
                        "Julien requested no further gameplay until explicit resume.\n");
                pending=null;Files.deleteIfExists(ACTION);finished=true;stage="QUEST_FINISHED";
                if(armed() && !logoutIssued){logoutIssued=true;logoutAt=System.currentTimeMillis();Rs2Player.logout();}
                else if(logoutIssued && System.currentTimeMillis()-logoutAt>20000)
                    hold("Logout unproved after quest completion");
                return;
            }
            if(!deathRegistered){
                DeathRecoveryBridge.Policy policy=new DeathRecoveryBridge.Policy(
                    Map.of(),pt(2550,2845,0),point->false,0);
                if(!DeathRecoveryBridge.register(deathOwner,policy)){
                    hold("Shared death recovery already owned by another quest");return;
                }
                deathRegistered=true;
            }
            if(deathOutcome!=null && !DeathRecoveryBridge.mustYield()){
                DeathRecoveryBridge.Outcome outcome=deathOutcome;deathOutcome=null;deathYield=false;
                if(outcome==DeathRecoveryBridge.Outcome.RECOVERED
                    || outcome==DeathRecoveryBridge.Outcome.NOTHING_TO_RECLAIM){
                    // A recovery outcome releases the lease; it does not prove combat supplies.
                    bossSafetyActive=false;bossEatAttempted=false;bossEscapeAttempted=false;
                    bossMealsProved=0;combatRetreat=false;combatStarted=false;
                    error="";prepared=false;prepAttempted=false;
                    stage="REPLAN_AFTER_DEATH";return;
                }
                hold("Shared death recovery "+outcome+": "+deathDetail);return;
            }
            if(deathYield || DeathRecoveryBridge.mustYield()){
                stage="WAIT_SHARED_DEATH_RECOVERY";return;
            }
            if(!gameplayInputAllowed()){
                stage="WAIT_ARMED_OR_INPUT_OWNER";return;
            }
            boolean hostile=bossRoom(f);
            WelcomeScreenEvent welcome=new WelcomeScreenEvent();
            if(welcome.validate()){
                stage="WELCOME";
                if(welcomeAttempts++==0){welcomeAt=System.currentTimeMillis();welcome.execute();}
                else if(System.currentTimeMillis()-welcomeAt>12000)hold("Welcome overlay persisted");
                return;
            }
            welcomeAttempts=0;
            if(!"QUEST".equals(mode())){
                if(services.hasPendingRequest())services.cancel("quest mode disarmed");
                stage="PREFLIGHT_ONLY";return;
            }
            if(f.hp<=0){hold("Death/respawn requires shared recovery reconciliation");return;}
            QuestServiceClient.Result result=services.poll();
            if(result!=null){
                LOG.info("[CorsairCurse] SERVICE kind={} outcome={} proof={}",result.kind(),result.outcome(),result.proof());
                if(result.kind()==QuestServiceHub.Kind.BANKING && cowhideBankPending
                    && result.outcome()==QuestServiceHub.Outcome.COMPLETE){
                    if(f.count(ItemID.COW_HIDE)>0){hold("Banking completed but cowhides remain carried");return;}
                    cowhideBankPending=false;
                    LOG.info("[CorsairCurse] COWHIDE_BANKED pickups={} serviceProof={}",cowhidePickups,result.proof());
                }
                if(result.kind()==QuestServiceHub.Kind.TRAINING && result.outcome()==QuestServiceHub.Outcome.HOLD)
                    heldTrainingReason=Objects.toString(result.proof(),"");
                if(result.kind()==QuestServiceHub.Kind.BANKING
                    && result.outcome()==QuestServiceHub.Outcome.UNAVAILABLE
                    && result.proof()!=null && result.proof().contains("NAVIGATION:UNAVAILABLE:")
                    && !bankArrivalRetryUsed && !hostile && !Rs2Player.isInCombat()
                    && net.runelite.client.plugins.microbot.util.bank.Rs2Bank.isNearBank(8)){
                    if(!services.acknowledge()){hold("Bank arrival recovery acknowledgement failed");return;}
                    bankArrivalRetryUsed=true;prepared=false;prepAttempted=false;
                    stage="REPLAN_AT_OBSERVED_BANK";
                    LOG.info("[CorsairCurse] BANK_ARRIVAL_RECOVERY usable bank scene observed; one fresh preparation request");
                    return;
                }
                if(result.kind()==QuestServiceHub.Kind.TRAINING
                    && result.outcome()==QuestServiceHub.Outcome.UNAVAILABLE
                    && result.proof()!=null
                    && result.proof().startsWith(TrainingMaintenanceProtocol.PROOF_PREFIX+"|")){
                    TrainingMaintenanceProtocol.Receipt receipt=
                        TrainingMaintenanceProtocol.parseProof(result.proof());
                    long pid=ProcessHandle.current().pid();
                    if(!TrainingMaintenanceProtocol.matches(receipt,pid,accountKey,trainingRequestId)){
                        if(result.outcome()!=QuestServiceHub.Outcome.HOLD)services.acknowledge();
                        hold("Malformed or mismatched training-maintenance proof; automation remains stopped");
                        return;
                    }
                    if(!services.acknowledge()){
                        hold("Training-maintenance result could not be acknowledged safely");return;
                    }
                    trainingMaintenanceProof=result.proof();
                    stage="WAIT_TRAINING_MAINTENANCE_APPLIED";
                    LOG.info("[CorsairCurse] TRAINING_MAINTENANCE_YIELD request={} fromGeneration={} toGeneration={} artifact={} checkpoint={}",
                        trainingRequestId,receipt.update().fromGeneration(),receipt.update().generation(),
                        receipt.update().artifact(),receipt.checkpointSha());
                    return; // Maintenance never consumes the normal no-progress retry budget.
                }
                if(result.kind()==QuestServiceHub.Kind.TRAINING
                    && result.outcome()==QuestServiceHub.Outcome.UNAVAILABLE){
                    if(trainingStartXp>=0 && f.combatXp>trainingStartXp)trainingNoProgressRequests=0;
                    else trainingNoProgressRequests++;
                    trainingRequestId="";
                }
                if(result.kind()==QuestServiceHub.Kind.TRAINING
                    && result.outcome()==QuestServiceHub.Outcome.UNAVAILABLE
                    && f.food()<12 && !hostile){
                    services.acknowledge();prepAttempted=false;
                    stage="REPLAN_FOOD_AFTER_TRAINING";return;
                }
                if(result.kind()==QuestServiceHub.Kind.TRAINING
                    && result.outcome()==QuestServiceHub.Outcome.UNAVAILABLE
                    && (result.proof().contains("instance/combat route unsupported")
                        || result.proof().startsWith("Fresh same-account safe return within two tiles;"))
                    && !hostile && f.hp>8 && f.food()>=12 && trainingNoProgressRequests<4){
                    services.acknowledge();
                    trainingRetryAfter=System.currentTimeMillis()+12_000;
                    stage="WAIT_TRAINING_COMBAT_COOLDOWN";return;
                }
                if(result.kind()==QuestServiceHub.Kind.TRAINING
                    && result.outcome()==QuestServiceHub.Outcome.COMPLETE
                    && (f.combat<25 || f.hpMax<25)){
                    hold("Training claimed completion without required levels: combat="
                        +f.combat+" hpMax="+f.hpMax);return;
                }
                if(result.kind()==QuestServiceHub.Kind.NAVIGATION
                    && result.outcome()==QuestServiceHub.Outcome.UNAVAILABLE
                    && result.proof()!=null && result.proof().startsWith("NAVIGATION_BUDGET_EXHAUSTED:")
                    && !hostile && !Rs2Player.isInCombat() && f.hp>Math.max(12,f.hpMax/2)
                    && navigationTarget!=null && f.pos.getPlane()==navigationTarget.getPlane()
                    && f.pos.distanceTo2D(navigationTarget)+5<navigationStartDistance
                    && navigationDeadlineReplans<2){
                    if(!services.acknowledge()){hold("Stopped navigation result could not be acknowledged");return;}
                    navigationDeadlineReplans++;
                    stage="REPLAN_PROGRESSING_ROUTE";
                    LOG.info("[CorsairCurse] ROUTE_BUDGET_REPLAN actual={} target={} remaining={} attempt={}",
                        f.pos,navigationTarget,f.pos.distanceTo2D(navigationTarget),navigationDeadlineReplans);
                    return;
                }
                if(result.outcome()!=QuestServiceHub.Outcome.COMPLETE){
                    if(result.outcome()==QuestServiceHub.Outcome.UNAVAILABLE)
                        services.acknowledge();
                    hold("Shared service "+result.kind()+" "+result.outcome()+": "+result.proof());
                    if(!hostile)return;
                }
                else if(result.kind()==QuestServiceHub.Kind.NAVIGATION &&
                    !near(f.pos,navigationTarget,navigationRadius)){
                    hold("Navigation reported completion without observed arrival at "+navigationTarget+
                        "; actual="+f.pos);
                    if(!hostile)return;
                }else{
                    if(result.kind()==QuestServiceHub.Kind.NAVIGATION)navigationDeadlineReplans=0;
                    navigationTarget=null;
                    services.acknowledge();stage="REOBSERVE_AFTER_SERVICE";
                    if(!hostile)return;
                }
            }
            if(!hostile && survivalEat(f))return;
            if(hostile || bossSafetyActive){
                if(bossSafety(f,hostile))return;
            }
            if(!trainingMaintenanceProof.isBlank()){
                TrainingMaintenanceProtocol.Receipt receipt=
                    TrainingMaintenanceProtocol.parseProof(trainingMaintenanceProof);
                TrainingMaintenanceProtocol.Wait wait=TrainingMaintenanceProtocol.await(
                    Path.of(System.getProperty("user.home"),".runelite","quest-services-hot"),
                    Path.of(System.getProperty("user.home"),".runelite","quest-services-hot","training-maintenance"),
                    receipt,ProcessHandle.current().pid(),accountKey,trainingRequestId,System.currentTimeMillis());
                if(wait.state()==TrainingMaintenanceProtocol.WaitState.WAITING){
                    stage="WAIT_TRAINING_MAINTENANCE_APPLIED";return;
                }
                if(wait.state()!=TrainingMaintenanceProtocol.WaitState.APPLIED){
                    hold("Training maintenance failed closed: "+wait.reason());return;
                }
                LOG.info("[CorsairCurse] TRAINING_MAINTENANCE_APPLIED generation={} artifact={}; replan from fresh levels/inventory",
                    receipt.update().generation(),receipt.update().artifact());
                trainingMaintenanceProof="";trainingRequestId="";
                trainingRetryAfter=0;prepAttempted=false;prepared=false;
                stage="REPLAN_AFTER_TRAINING_MAINTENANCE";return;
            }
            // A rejected UI dispatch can still coincide with a completed cutscene.
            // Reconcile only explicit quest milestones, never time or proximity.
            if(pending!=null && pending.key.startsWith("dialogue:")
                && error.equals("Action dispatch rejected "+pending.key)
                && (f.progress>pending.progress || f.thief>pending.thief
                    || f.cabin>pending.cabin || f.cook>pending.cook || f.navigator>pending.navigator)){
                String rejectedError=error; verify(f);
                if(pending==null && error.equals(rejectedError) && !Files.exists(ACTION)){
                    error="";stage="REPLAN_RECONCILED_DIALOGUE";return;
                }
            }
            if(!error.isEmpty()){stage="HOLD";return;}
            if(services.blocksQuestInput()){stage="WAIT_SHARED_SERVICE";return;}
            if(f.progress>=50 && f.progress<=52 && f.hp<=Math.max(8,f.hpMax/2)){
                if(pending!=null && pending.key.startsWith("eat:")){verify(f);return;}
                if(pending!=null)supersedePending("urgent health response");
                if(!error.isEmpty())return;
                if(f.food()>0){eat(f);return;}
                combatRetreat=true;
            }
            if(pending!=null){verify(f);return;}
            if(f.progress<=10 && f.combat>=25 && f.hpMax>=25 && collectCowhides(f))return;
            if(!prepared && f.progress<=10){prepare(f);return;}
            if(f.progress==52 && !f.instanced && f.pos.getPlane()==1
                && f.pos.distanceTo2D(DOCK)<=20){
                if(f.inDialogue || f.continueVisible || !f.options.isEmpty()){dialogue(f);return;}
                List<Rs2TileObjectModel> planks=Microbot.getRs2TileObjectCache().query().within(f.pos,15)
                    .toListOnClientThread().stream().filter(o->o!=null && "Gangplank".equalsIgnoreCase(o.getName())
                        && o.getWorldLocation().getPlane()==1).toList();
                if(planks.size()!=1){hold("Return ship requires one observed Gangplank; found="+planks.size());return;}
                var plank=planks.get(0);
                LOG.info("[CorsairCurse] RETURN_GANGPLANK id={} at={} player={}",plank.getId(),plank.getWorldLocation(),f.pos);
                execute(f,object("off:Rimmington",plank.getId(),plank.getWorldLocation()));return;
            }
            if(f.progress==52 && !hostile && inCove(f.pos)
                && (!f.ithoiEquipmentReady() || f.food()<20 || f.healing()<150)){
                if(bossRetreats>=2 || failedBossWeapon>0 && f.weapon==failedBossWeapon && f.ithoiEquipmentReady()){
                    hold("Boss retry exhausted; improved setup required before further travel");return;
                }
                // The Cove bank is quest-locked. Captain Tock provides the free
                // return voyage during the quest (Wiki transcript, checked 2026-10-03).
                if(f.inDialogue || f.continueVisible || !f.options.isEmpty())dialogue(f);
                else execute(f,reach(f,"Tock:resupply-return",NpcID.CORSAIR_CAPTAIN_2OPS,TOCK));
                return;
            }
            if(f.progress==52 && f.pos.getPlane()==0){
                if(bossRetreats>=2 || failedBossWeapon>0 && f.weapon==failedBossWeapon && f.ithoiEquipmentReady()){
                    hold("Boss retry budget exhausted or improved setup failed; no repeated fight");return;
                }
                if(4L*f.hp<3L*f.hpMax || !f.ithoiEquipmentReady() || f.food()<20
                    || f.healing()<150){
                    if(!Rs2Player.isInCombat() && !services.blocksQuestInput() && !prepAttempted){
                        prepared=false;prepare(f);return;
                    }
                    hold("Boss entry preflight failed after bounded preparation: HP="+f.hp+"/"+f.hpMax+
                        " weapon="+f.weapon+" magicDefence="+f.magicDefence+" equipmentStatsKnown="+f.equipmentStatsKnown+" food="+f.food()+
                        " healing="+f.healing());return;
                }
            }
            if(combatRetreat){retreat(f);return;}
            if(f.inDialogue || f.continueVisible || !f.options.isEmpty()){dialogue(f);return;}
            if(f.progress<52){
                QuestRandomEventDismiss.Event observedEvent=QuestRandomEventDismiss.observe();
                if(Files.exists(HOME.resolve("complete-next-random-event.flag")) && observedEvent!=null){
                    Properties event=new Properties();event.setProperty("id",Integer.toString(observedEvent.id));
                    event.setProperty("index",Integer.toString(observedEvent.index));event.setProperty("name",observedEvent.name);
                    event.setProperty("timestamp",Long.toString(System.currentTimeMillis()));
                    event.setProperty("pid",Long.toString(ProcessHandle.current().pid()));event.setProperty("account",accountKey);
                    try(OutputStream out=Files.newOutputStream(HOME.resolve("random-event-observed.properties"))){event.store(out,"Next event reserved for completion; not dismissed");}
                    stage="RANDOM_EVENT_RESERVED_"+observedEvent.name;return;
                }
                QuestRandomEventDismiss.Result random=randomDismiss.tick(
                    observedEvent,true,System.currentTimeMillis());
                if(random==QuestRandomEventDismiss.Result.UNPROVED){
                    hold("Random event Dismiss unproved after bounded attempts");return;
                }
                if(random==QuestRandomEventDismiss.Result.DISPATCHED
                    || random==QuestRandomEventDismiss.Result.VERIFYING
                    || random==QuestRandomEventDismiss.Result.DEFERRED){
                    stage="RANDOM_EVENT_"+random;return;
                }
            }
            Plan p=plan(f);
            if(p==null){hold("No verified plan for progress="+f.progress+" vars="+
                f.thief+","+f.cabin+","+f.cook+","+f.navigator+" pos="+f.pos);return;}
            execute(f,p);
        }catch(Exception ex){hold("Tick: "+ex);LOG.error("[CorsairCurse] tick",ex);}
        finally{if("WAIT_SHARED_SERVICE".equals(stage) || error.contains("NAVIGATION HOLD"))
            navigationGeometryProbe();status(f);}
    }
    private Frame observe(){
        Frame f=Microbot.getClientThread().invoke(()->{
            Frame s=new Frame();Client c=Microbot.getClient();if(c==null)return s;
            GameState g=c.getGameState();s.game=g==null?"UNKNOWN":g.name();s.loginIndex=c.getLoginIndex();
            if(g!=GameState.LOGGED_IN || c.getLocalPlayer()==null)return s;
            s.pos=c.getLocalPlayer().getWorldLocation();s.accountHash=c.getAccountHash();
            s.instanced=c.isInInstancedRegion();
            s.templatePos=s.instanced?WorldPoint.fromLocalInstance(c,
                c.getLocalPlayer().getLocalLocation()):s.pos;
            if(c.getUsername()!=null && c.getLocalPlayer().getName()!=null){
                String identity=c.getUsername().trim().toLowerCase(Locale.ROOT)+"\n"+
                    c.getLocalPlayer().getName().trim().toLowerCase(Locale.ROOT);
                try{s.accountKey=java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}
            }
            s.progress=c.getVarbitValue(VarbitID.CORSCURS_PROGRESS);
            s.thief=c.getVarbitValue(VarbitID.CORSCURS_THIEF);
            s.cabin=c.getVarbitValue(VarbitID.CORSCURS_CABINBOY);
            s.cook=c.getVarbitValue(VarbitID.CORSCURS_COOK);
            s.navigator=c.getVarbitValue(VarbitID.CORSCURS_NAVIGATOR);
            var state=Quest.THE_CORSAIR_CURSE.getState(c);s.quest=state==null?"UNKNOWN":state.name();
            s.hp=c.getBoostedSkillLevel(Skill.HITPOINTS);s.hpMax=c.getRealSkillLevel(Skill.HITPOINTS);
            s.prayer=c.getBoostedSkillLevel(Skill.PRAYER);s.combat=c.getLocalPlayer().getCombatLevel();
            s.attackXp=c.getSkillExperience(Skill.ATTACK);
            s.strengthXp=c.getSkillExperience(Skill.STRENGTH);
            s.defenceXp=c.getSkillExperience(Skill.DEFENCE);
            s.rangedXp=c.getSkillExperience(Skill.RANGED);s.magicXp=c.getSkillExperience(Skill.MAGIC);
            s.hpXp=c.getSkillExperience(Skill.HITPOINTS);s.world=c.getWorld();
            s.combatXp=s.attackXp+s.strengthXp+s.defenceXp+c.getSkillExperience(Skill.HITPOINTS)
                +c.getSkillExperience(Skill.RANGED)+c.getSkillExperience(Skill.MAGIC);
            if(c.getLocalPlayer().getInteracting() instanceof NPC)
                s.enemyId=((NPC)c.getLocalPlayer().getInteracting()).getId();
            s.benignCowScene=s.pos!=null && s.hpMax>=20 && s.combat>=15
                && new CombatTrainingGoal.Region(3022,3300,3035,3313,0).contains(s.pos);
            for(NPC npc:c.getNpcs()) {
                if(npc==null || npc.isDead() || npc.getCombatLevel()<=0)continue;
                if(npc.getInteracting()!=c.getLocalPlayer() && c.getLocalPlayer().getInteracting()!=npc)continue;
                String name=npc.getName();
                if(npc.getCombatLevel()>2 || name==null
                    || !(name.equalsIgnoreCase("Cow") || name.equalsIgnoreCase("Cow calf")))
                    s.benignCowScene=false;
            }
            ItemContainer inv=c.getItemContainer(InventoryID.INVENTORY);s.invReady=inv!=null;
            if(inv!=null)for(Item i:inv.getItems())if(i!=null && i.getId()>0)
                s.items.merge(i.getId(),i.getQuantity(),Integer::sum);
            ItemContainer eq=c.getItemContainer(InventoryID.EQUIPMENT);
            s.equipmentStatsKnown=eq!=null;
            if(eq!=null){Item[] items=eq.getItems();for(int slot=0;slot<items.length;slot++){
                Item i=items[slot];if(i==null || i.getId()<=0)continue;
                s.equipment.add(i.getId());
                var itemStats=Microbot.getItemManager().getItemStats(i.getId());
                if(itemStats==null || itemStats.getEquipment()==null)s.equipmentStatsKnown=false;
                else s.magicDefence+=itemStats.getEquipment().getDmagic();
                if(slot==EquipmentInventorySlot.WEAPON.getSlotIdx()){
                    s.weapon=i.getId();
                    if(itemStats!=null && itemStats.getEquipment()!=null){
                        var stats=itemStats.getEquipment();
                        s.weaponScore=Math.max(stats.getAstab(),Math.max(stats.getAslash(),stats.getAcrush()))+2*stats.getStr();
                    }
                }
                if(slot==EquipmentInventorySlot.HEAD.getSlotIdx() ||
                    slot==EquipmentInventorySlot.BODY.getSlotIdx() ||
                    slot==EquipmentInventorySlot.LEGS.getSlotIdx())s.armour++;
            }}
            // Installed metadata proves the outcome; this bounded catalog contains
            // F2P one-handed swords/scimitars, with verified tier Attack requirements.
            int attack=c.getRealSkillLevel(Skill.ATTACK);
            for(int id:new int[]{ItemID.BRONZE_SWORD,ItemID.IRON_SWORD,ItemID.STEEL_SWORD,
                ItemID.BRONZE_SCIMITAR,ItemID.IRON_SCIMITAR,ItemID.STEEL_SCIMITAR}){
                if(attack<((id==ItemID.STEEL_SWORD || id==ItemID.STEEL_SCIMITAR)?5:1))continue;
                var comp=Microbot.getItemManager().getItemComposition(id);
                var stats=Microbot.getItemManager().getItemStats(id);
                if(comp==null || comp.isMembers() || !comp.isGeTradeable() || stats==null
                    || !stats.isEquipable() || stats.getEquipment()==null)continue;
                var e=stats.getEquipment();
                if(e.isTwoHanded() || e.getDmagic()<0 || e.getSlot()!=EquipmentInventorySlot.WEAPON.getSlotIdx())continue;
                int score=Math.max(e.getAstab(),Math.max(e.getAslash(),e.getAcrush()))+2*e.getStr();
                if(score>s.requiredWeaponScore){s.requiredWeaponScore=score;s.bossWeapons.clear();}
                if(score==s.requiredWeaponScore && score>0)s.bossWeapons.add(id);
            }
            if(s.ithoiEquipmentReady())s.bossWeapons.add(s.weapon);
            return s;
        });
        if(f.loggedIn()){
            f.inDialogue=Rs2Dialogue.isInDialogue();f.continueVisible=Rs2Dialogue.hasContinue();
            f.dialogue=norm(f.inDialogue?Rs2Dialogue.getDialogueText():"");
            if(f.dialogue.isBlank())f.dialogue=Microbot.getClientThread().invoke((java.util.function.Supplier<String>)()->{
                for(var info:new net.runelite.api.widgets.WidgetInfo[]{
                    net.runelite.api.widgets.WidgetInfo.DIALOG_PLAYER_TEXT,
                    net.runelite.api.widgets.WidgetInfo.DIALOG_NPC_TEXT,
                    net.runelite.api.widgets.WidgetInfo.DIALOG_SPRITE_TEXT,
                    net.runelite.api.widgets.WidgetInfo.DIALOG_DOUBLE_SPRITE_TEXT}){
                    Widget text=Microbot.getClient().getWidget(info);
                    if(text!=null && !text.isHidden() && !norm(text.getText()).isBlank())return norm(text.getText());
                }
                return "";
            });
            for(Widget w:Rs2Dialogue.getDialogueOptions())if(w!=null && !norm(w.getText()).isEmpty())
                f.options.add(w.getText());
        }
        return f;
    }
    private boolean collectCowhides(Frame f) throws IOException {
        if(cowhideCollected)return false;
        long now=System.currentTimeMillis();
        Path receipt=HOME.resolve("cowhide-pending.properties");
        if(cowhideStarted==0)cowhideStarted=now;
        if(cowhidePendingAt==0 && Files.exists(receipt)){
            Properties saved=new Properties();
            try(InputStream in=Files.newInputStream(receipt)){saved.load(in);}
            if(!f.accountKey.equals(saved.getProperty("account"))){hold("Cowhide receipt belongs to another account");return true;}
            cowhideBefore=Integer.parseInt(saved.getProperty("before"));
            cowhidePendingAt=Long.parseLong(saved.getProperty("at"));
        }
        if(cowhidePendingAt!=0){
            if(f.count(ItemID.COW_HIDE)>cowhideBefore){
                cowhidePickups+=f.count(ItemID.COW_HIDE)-cowhideBefore;
                Files.deleteIfExists(receipt);cowhidePendingAt=0;
                LOG.info("[CorsairCurse] COWHIDE_PICKUP_PROVED count={}",f.count(ItemID.COW_HIDE));
            }else if(now-cowhidePendingAt<5000){stage="VERIFY_COWHIDE_PICKUP";return true;}
            else {Files.deleteIfExists(receipt);cowhidePendingAt=0;finishCowhides(f,"pickup unproved; no repeat");return false;}
        }
        if(now-cowhideStarted>90000){finishCowhides(f,"collection deadline reached");return false;}
        if(services.blocksQuestInput())return true;
        if(!f.benignCowScene || f.hp<=12){finishCowhides(f,"outside safe cow scene");return false;}
        if(Rs2Player.isInCombat()){stage="WAIT_LOOT_COMBAT_CLEAR";return true;}
        if(cowhidePickups>=12 || Rs2Inventory.getEmptySlots()<=2){
            finishCowhides(f,"collection limit reached");return false;
        }
        var target=net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem.getGroundItems().values().stream()
            .filter(item->item.getId()==ItemID.COW_HIDE && item.getLocation()!=null
                && new CombatTrainingGoal.Region(3022,3300,3035,3313,0).contains(item.getLocation())
                && f.pos.distanceTo(item.getLocation())<=5
                && net.runelite.client.plugins.microbot.util.reachable.Rs2Reachable.isReachable(item.getLocation())
                && net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem.canTakeGroundItem(item))
            .min(java.util.Comparator.comparingInt(item->f.pos.distanceTo(item.getLocation()))).orElse(null);
        if(target==null){finishCowhides(f,"no nearby reachable hides");return false;}
        cowhideBefore=f.count(ItemID.COW_HIDE);
        Properties saved=new Properties();saved.setProperty("account",f.accountKey);
        saved.setProperty("before",Integer.toString(cowhideBefore));saved.setProperty("at",Long.toString(now));
        saved.setProperty("target",target.getLocation().toString());
        Path temp=HOME.resolve("cowhide-pending.tmp");
        try(OutputStream out=Files.newOutputStream(temp)){saved.store(out,"Take must reconcile before another dispatch");}
        Files.move(temp,receipt,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        cowhidePendingAt=now;
        net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem.interact(target);
        stage="VERIFY_COWHIDE_PICKUP";return true;
    }
    private void finishCowhides(Frame f,String reason){
        cowhideCollected=true;cowhideBankPending=f.count(ItemID.COW_HIDE)>0;
        if(cowhideBankPending){prepared=false;prepAttempted=false;}
        LOG.info("[CorsairCurse] COWHIDE_COLLECTION_END count={} reason={}",f.count(ItemID.COW_HIDE),reason);
    }
    private void prepare(Frame f){
        if(System.currentTimeMillis()<trainingRetryAfter){
            stage="WAIT_TRAINING_COMBAT_COOLDOWN";return;
        }
        boolean needsTraining=f.hpMax<25 || f.combat<25;
        boolean atCows=f.pos!=null && new CombatTrainingGoal.Region(3022,3300,3035,3313,0).contains(f.pos);
        // Short local cow bouts retain three meals for return travel. Quest/boss
        // preparation keeps its larger supply target after training completes.
        int minimumFood=needsTraining?(atCows?4:12):20;
        int minimumHealing=needsTraining?(atCows?28:100):150;
        if(!cowhideBankPending && f.weapon>0 && (needsTraining?f.armour>=2:f.ithoiEquipmentReady())
            && f.food()>=minimumFood && f.healing()>=minimumHealing){
            if(!needsTraining){prepared=true;stage="SUPPLIES_READY";return;}
            if(trainingNoProgressRequests>=4){hold("Four bounded training requests produced no verified combat XP gain");return;}
            CombatTrainingGoal goal=new CombatTrainingGoal(accountKey,ProcessHandle.current().pid(),
                "Corsair Curse combat and HP readiness","corsair-train-"+System.currentTimeMillis(),
                25,25,Set.of(),Set.of("Cow","Cow calf"),2,
                new CombatTrainingGoal.Region(3022,3300,3035,3313,0),
                pt(3023,3307,0),pt(3023,3307,0),
                List.of(ItemID.TROUT,ItemID.TUNA,ItemID.SALMON,ItemID.LOBSTER),
                minimumFood,3,16,8,Set.of(f.weapon),true,Set.of(),2,800,
                System.currentTimeMillis()+3600000,60000,90000);
            trainingRequestId=goal.requestId();
            if(!services.begin(goal)){trainingRequestId="";stage="WAIT_SHARED_TRAINING_SUBMIT";return;}
            trainingRequests++;trainingStartXp=f.combatXp;stage="WAIT_SHARED_TRAINING";return;
        }
        if(prepAttempted && !bankArrivalRetryUsed && !Rs2Player.isInCombat()
            && !services.blocksQuestInput() && !Files.isRegularFile(ACTION)
            && net.runelite.client.plugins.microbot.util.bank.Rs2Bank.isNearBank(8)){
            bankArrivalRetryUsed=true;prepAttempted=false;
            LOG.info("[CorsairCurse] BANK_RELOAD_REPLAN observed bank; bounded retry of completed preparation attempt");
        }
        if(!needsTraining && (bossRetreats>=2 || failedBossWeapon>0 && f.weapon==failedBossWeapon && f.ithoiEquipmentReady())){
            hold("Boss retry budget exhausted or same failed weapon still equipped; improved combat plan required");return;
        }
        if(!needsTraining && f.bossWeapons.isEmpty()){hold("Installed F2P weapon metadata unavailable");return;}
        if(prepAttempted){hold("Shared preparation returned without required weapon/armour/food");return;}
        // Banking's installed adapter uses Microbot's trailing combat timer.
        // Let that timer clear before submitting a new banking request.
        if(Rs2Player.isInCombat()){stage="WAIT_PREPARATION_COMBAT_CLEAR";return;}
        // Both tools have free quest-local sources; do not block the voyage on bank stock.
        Map<Integer,Integer> required=Map.of();
        Set<Integer> protectedItems=new HashSet<>(Set.of(RELIC,SPADE,TINDER));
        if(f.weapon>0)protectedItems.add(f.weapon);
        if(!services.begin(new PreparationGoal(accountKey,ProcessHandle.current().pid(),
            "Corsair Curse boss supplies and tools","corsair-prep-"+System.currentTimeMillis(),
            required,needsTraining?100:150,0,4,!needsTraining?Set.copyOf(f.bossWeapons):f.weapon>0?Set.of(f.weapon):Set.of(
                ItemID.BRONZE_SWORD,ItemID.IRON_SWORD,ItemID.STEEL_SWORD,
                ItemID.BRONZE_SCIMITAR,ItemID.IRON_SCIMITAR,ItemID.STEEL_SCIMITAR,
                ItemID.BRONZE_PICKAXE,ItemID.IRON_PICKAXE,ItemID.STEEL_PICKAXE),
            Map.of(),protectedItems,true,0,2000,
            System.currentTimeMillis()+900000,needsTraining?15:20)))return;
        prepAttempted=true;
        stage="WAIT_SHARED_PREPARATION";
    }
    private Plan plan(Frame f){
        int p=f.progress;
        // Select the local instance action before the mainland-return fallback.
        if(f.instanced && ithoiRoom(f)){
            if(p==52)return new Plan("Ithoi:fight",Kind.COMBAT,
                NpcID.CORSCURS_NAVIGATOR_COMBAT,f.pos,0);
            Rs2TileObjectModel exit=Microbot.getRs2TileObjectCache().query()
                .withId(ObjectID.DS2_CORSAIR_COVE_STAIRS_RAMP).within(f.pos,12)
                .nearestOnClientThread();
            if(exit!=null && near(f.pos,exit.getWorldLocation(),12))
                return object("down:Ithoi",exit.getId(),exit.getWorldLocation());
            return null;
        }
        if(p==0 || p==5)return npc("start:Tock",NpcID.CORSAIR_CAPTAIN_1OP,FARM);
        if(p==10)return npc("sail:Tock",NpcID.CORSAIR_CAPTAIN_1OP,DOCK);
        if(!inCove(f.pos) && !inCavern(f.pos) && p>=15 && p<=55)
            return npc("return:Tock",NpcID.CORSAIR_CAPTAIN_2OPS,DOCK);
        if(p==15){
            if(f.navigator<1)return reach(f,"Ithoi:first",NpcID.CORSCURS_NAVIGATOR,ITHOI);
            if(f.thief<2)return reach(f,"Arsen:first",NpcID.CORSCURS_THIEF,ARSEN);
            if(f.cabin<1)return reach(f,"Colin:first",NpcID.CORSCURS_CABINBOY,COLIN);
            if(f.cook<1)return reach(f,"Gnocci:first",NpcID.CORSCURS_COOK,GNOCCI);
            if(f.count(SPADE)==0)return objectOnGround(f,"get:spade",ObjectID.SAND_WITHSPADE,SPADE_OBJECT,SPADE);
            if(f.count(TINDER)==0)return reachObject(f,"get:tinder",ObjectID.RAIDS_ICEDEMON_TINDERBOX,
                pt(2543,2862,1),TINDER);
            if(f.thief<4){
                if(f.count(RELIC)==0)return reach(f,"Tock:relic",NpcID.CORSAIR_CAPTAIN_2OPS,TOCK);
                return reach(f,"Tess:relic",NpcID.OGRESS_TESS,TESS);
            }
            if(f.cook<2)return onGround(f,new Plan("dig:doll",Kind.DIG,0,SAND,SPADE));
            if(f.cabin<2)return reachObject(f,"look:telescope",ObjectID.CORSCURS_TELESCOPE,
                pt(2528,2835,1),0);
            if(f.cook<3)return reach(f,"Gnocci:resolve",NpcID.CORSCURS_COOK,GNOCCI);
            if(f.thief<6)return reach(f,"Arsen:resolve",NpcID.CORSCURS_THIEF,ARSEN);
            if(f.cabin<3)return reach(f,"Colin:resolve",NpcID.CORSCURS_CABINBOY,COLIN);
            return new Plan("wait:progress20",Kind.WAIT,0,f.pos,0);
        }
        if(p==20)return reach(f,"Tock:theories",NpcID.CORSAIR_CAPTAIN_2OPS,TOCK);
        if(p==25)return reach(f,"Gnocci:dinner",NpcID.CORSCURS_COOK,GNOCCI);
        if(p==30)return reach(f,"Arsen:dinner",NpcID.CORSCURS_THIEF,ARSEN);
        if(p==35 || p==40)return reach(f,"Ithoi:accuse",NpcID.CORSCURS_NAVIGATOR,ITHOI);
        if(p==45)return onGround(f,new Plan("burn:wood",Kind.ITEM_OBJECT,
            ObjectID.CORSCURS_DRIFTWOOD_MULTI,DRIFTWOOD,TINDER));
        if(p==49)return new Plan("wait:cutscene",Kind.WAIT,0,f.pos,0);
        if(p==50)return reach(f,"Tock:expose",NpcID.CORSAIR_CAPTAIN_2OPS,TOCK);
        if(p==52)return reach(f,"Ithoi:fight",NpcID.CORSCURS_NAVIGATOR_COMBAT,ITHOI);
        if(p==55)return reach(f,"Tock:finish",NpcID.CORSAIR_CAPTAIN_2OPS,TOCK);
        return null;
    }
    private Plan reach(Frame f,String key,int npc,WorldPoint destination){
        Plan crossing=crossing(f,destination);
        return crossing!=null?crossing:new Plan(key,npc==NpcID.CORSCURS_NAVIGATOR_COMBAT?Kind.COMBAT:Kind.NPC,npc,destination,0);
    }
    private Plan reachObject(Frame f,String key,int id,WorldPoint destination,int item){
        Plan crossing=crossing(f,destination);
        return crossing!=null?crossing:new Plan(key,Kind.OBJECT,id,destination,item);
    }
    private Plan onGround(Frame f,Plan p){Plan crossing=crossing(f,p.at());return crossing!=null?crossing:p;}
    private Plan objectOnGround(Frame f,String key,int id,WorldPoint destination,int item){
        return onGround(f,new Plan(key,Kind.OBJECT,id,destination,item));
    }
    private Plan crossing(Frame f,WorldPoint goal){
        WorldPoint here=f.pos;if(here==null)return null;
        if(inCavern(here) && !inCavern(goal))return object("leave:cavern",ObjectID.DS2_OGRE_CORSAIR_VINE_LADDER,pt(2012,9005,1));
        if(inCavern(goal) && !inCavern(here)){
            if(here.getPlane()!=0)return leaveRoom(here);
            return object("enter:cavern",ObjectID.DS2_OGRE_CORSAIR_VINE_LADDER_ENTRANCE,CAVE);
        }
        if(here.getPlane()==1 && inCove(here) && !sameRoom(here,goal))return leaveRoom(here);
        if(here.getPlane()==0 && goal.getPlane()==1){
            if(inRoom(goal,ITHOI))return object("up:Ithoi",ObjectID.DS2_CORSAIR_COVE_STAIRS_RAMP,ITHOI_RAMP);
            if(inRoom(goal,GNOCCI))return object("up:Gnocci",ObjectID.DS2_CORSAIR_COVE_STAIRS,GNOCCI_STAIRS);
            if(inRoom(goal,TOCK))return object("onto:ship",ObjectID.DS2_CORSAIR_COVE_SHIPPLANK,SHIP_PLANK);
            return object("up:Arsen",ObjectID.DS2_CORSAIR_COVE_STAIRS,ARSEN_STAIRS);
        }
        return null;
    }
    private Plan leaveRoom(WorldPoint here){
        if(inRoom(here,ITHOI))return object("down:Ithoi",ObjectID.DS2_CORSAIR_COVE_STAIRS_RAMP,pt(2529,2834,1));
        if(inRoom(here,GNOCCI))return object("down:Gnocci",ObjectID.DS2_CORSAIR_COVE_STAIRS_DOWN,pt(2548,2862,1));
        if(inRoom(here,TOCK))return object("off:ship",ObjectID.DS2_CORSAIR_COVE_SHIPPLANK,pt(2578,2838,1));
        if(inRoom(here,ARSEN))return object("down:Arsen",ObjectID.DS2_CORSAIR_COVE_STAIRS_DOWN,pt(2555,2855,1));
        return null;
    }
    private void execute(Frame f,Plan p){
        stage=p.key();
        if(p.kind()==Kind.WAIT){
            if(cutsceneAt==0)cutsceneAt=System.currentTimeMillis();
            if(System.currentTimeMillis()-cutsceneAt>20000)hold("Quest progress did not advance after "+p.key());
            return;
        }
        cutsceneAt=0;
        if(!near(f.pos,p.at(),p.kind()==Kind.NPC || p.kind()==Kind.COMBAT?5:2)
            && !(p.kind()==Kind.COMBAT && bossRoom(f))){
            navigate(f,p);return;
        }
        if(p.kind()==Kind.NPC || p.kind()==Kind.COMBAT){
            Rs2NpcModel npc=Microbot.getRs2NpcCache().query().withId(p.id())
                .within(p.kind()==Kind.COMBAT && bossRoom(f)?f.pos:p.at(),7)
                .nearestOnClientThread();
            if(npc==null){
                if(p.kind()==Kind.COMBAT){combatRetreat=true;retreat(f);return;}
                hold("NPC absent id="+p.id()+" at "+p.at());return;
            }
            if(p.kind()==Kind.COMBAT){
                if(!f.ithoiEquipmentReady() || f.food()<6){
                    combatRetreat=true;retreat(f);return;
                }
                if(combatStarted){
                    if(f.enemyId==p.id()){stage="FIGHTING_ITHOI";return;}
                    if(System.currentTimeMillis()-combatAt>15000)hold("Combat stopped without quest progress");
                    return;
                }
                combatStarted=true;combatAt=System.currentTimeMillis();
                issue(p,f,()->npc.click("Attack"));return;
            }
            issue(p,f,()->npc.click("Talk-to"));return;
        }
        if(p.kind()==Kind.DIG){issue(p,f,()->Rs2Inventory.interact(SPADE,"Dig"));return;}
        Rs2TileObjectModel object=Microbot.getRs2TileObjectCache().query().withId(p.id())
            .within(p.at(),1).nearestOnClientThread();
        if(object==null){hold("Object absent id="+p.id()+" at "+p.at());return;}
        if(p.kind()==Kind.ITEM_OBJECT){
            issue(p,f,()->Rs2Inventory.useItemOnObject(p.item(),p.id()));return;
        }
        if(p.key().equals("off:Rimmington")){issue(p,f,()->object.click("Cross"));return;}
        issue(p,f,object::click);
    }
    private void navigate(Frame f,Plan p){
        if(bossRoom(f)){
            combatRetreat=true;stage="BOSS_NAVIGATION_REFUSED";return;
        }
        if(services.blocksQuestInput()){stage="WAIT_NAVIGATION";return;}
        // A provider owns all walking and gate/transport decisions. It must prove arrival.
        WorldPoint destination=objectApproach(f,p);
        int radius=destination.equals(p.at())?(p.kind()==Kind.NPC || p.kind()==Kind.COMBAT?4:2):0;
        NavigationGoal goal=new NavigationGoal(accountKey,ProcessHandle.current().pid(),
            p.key(),"corsair-nav-"+System.currentTimeMillis(),destination,
            radius,true,0,0,false,
            Set.of(),2,System.currentTimeMillis()+Math.min(600000L,
                120000L+2000L*f.pos.distanceTo2D(destination)));
        if(!services.begin(goal)){stage="WAIT_NAVIGATION_SUBMIT";return;}
        navigationTarget=destination;navigationRadius=goal.arrivalRadius();
        navigationStartDistance=f.pos.distanceTo2D(destination);
        stage="WAIT_NAVIGATION_"+p.key();
    }
    /** Resolve a local object approach from current collision data; interaction retains its own proof. */
    private WorldPoint objectApproach(Frame f,Plan p){
        if(p.kind()!=Kind.OBJECT && p.kind()!=Kind.ITEM_OBJECT)return p.at();
        if(f.pos==null || f.pos.getPlane()!=p.at().getPlane())return p.at();
        List<WorldPoint> candidates=Microbot.getClientThread().invoke((java.util.function.Supplier<List<WorldPoint>>)()->{
            List<WorldPoint> tiles=new ArrayList<>();
            for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++){
                if(dx==0 && dy==0)continue;
                WorldPoint tile=pt(p.at().getX()+dx,p.at().getY()+dy,p.at().getPlane());
                tiles.add(tile);
            }
            return tiles;
        });
        candidates.sort(java.util.Comparator.comparingInt((WorldPoint tile)->tile.distanceTo(p.at()))
            .thenComparingInt(f.pos::distanceTo));
        for(WorldPoint tile:candidates){
            var route=net.runelite.client.plugins.microbot.util.walker.Rs2PathApi.plan(
                net.runelite.client.plugins.microbot.util.walker.Rs2RouteRequest.to(f.pos,tile).withBankItems(false));
            if(route.getTerminationReason()==net.runelite.client.plugins.microbot.util.walker.Rs2RouteTermination.TARGET_REACHED
                && route.isTargetReached(0)){
                LOG.info("[CorsairCurse] OBJECT_APPROACH_PLANNED target={} stand={}",p.at(),tile);
                return tile; // Shared navigation still validates every route step and proves actual arrival.
            }
        }
        return p.at();
    }
    private void dialogue(Frame f){
        stage="DIALOGUE";
        if(f.progress==0 && near(f.pos,FARM,12) && f.options.size()==2
            && f.options.stream().anyMatch(x->norm(x).equals(norm("Yes.")))
            && f.options.stream().anyMatch(x->norm(x).equals(norm("No.")))){
            boolean startPrompt=Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>)()->{
                Widget prompt=net.runelite.client.plugins.microbot.util.widget.Rs2Widget.findWidget("Start The Corsair Curse quest?");
                return prompt!=null && !prompt.isHidden() && norm(prompt.getText()).equals(norm("Start The Corsair Curse quest?"));
            });
            if(startPrompt){
                issue(new Plan("dialogue:accept-corsair",Kind.NPC,0,f.pos,0),f,
                    ()->Rs2Dialogue.clickOption("Yes."));return;
            }
        }
        List<String> allowed=dialogueOptions(f);
        for(String expected:allowed)for(String shown:f.options)if(norm(shown).equals(norm(expected))){
            Plan p=new Plan("dialogue:"+expected,Kind.NPC,0,f.pos,0);
            issue(p,f,()->Rs2Dialogue.clickOption(shown));return;
        }
        if(f.continueVisible){Plan p=new Plan("dialogue:continue",Kind.NPC,0,f.pos,0);
            if(!f.dialogue.isBlank()){
                issue(p,f,this::clickVisibleContinue);return;
            }
            issue(p,f,()->{Rs2Dialogue.clickContinue();return true;});return;}
        if(!f.options.isEmpty())hold("Unrecognized dialogue options progress="+f.progress+" "+f.options);
        else stage="WAIT_DIALOGUE";
    }
    private List<String> dialogueOptions(Frame f){
        if(f.progress==52 && inCove(f.pos) && near(f.pos,TOCK,12)
            && (!f.ithoiEquipmentReady() || f.food()<20 || f.healing()<150)
            && f.options.stream().anyMatch(x->norm(x).equals(norm("Please take me back to Rimmington."))))
            return List.of("Please take me back to Rimmington.");
        // Live return-voyage menu, observed at the Rimmington dock at stage52.
        // Travel can occur at multiple quest stages after a bank/death return.
        if(f.progress>=10 && f.progress<=55 && near(f.pos,DOCK,12)
            && f.options.size()==2
            && f.options.stream().anyMatch(x->norm(x).equals(norm("Let's go.")))
            && f.options.stream().anyMatch(x->norm(x).equals(norm("Not just now, thanks."))))
            return List.of("Let's go.");
        // Gnocci advances to 35 before his final optional-topic menu closes.
        if(f.progress==35 && f.options.size()==2
            && f.options.stream().anyMatch(x->norm(x).equals(norm("What is the mission Francois is doing?")))
            && f.options.stream().anyMatch(x->norm(x).equals(norm("Okay, thanks."))))
            return List.of("Okay, thanks.");
        return switch(f.progress){
            case 0,5 -> List.of("What kind of help do you need?","Sure, I'll try to help with your curse.");
            case 10 -> List.of("Okay, I'm ready go to Corsair Cove.","Let's go.");
            case 15 -> List.of("I hear you've been cursed.","Arsen says he gave you a sacred ogre relic.",
                "About that sacred ogre relic...","I've come to return what Arsen stole.",
                "Search for the possessed doll and face the consequences.","Let's go.");
            case 20 -> List.of("I've ruled out all the Corsairs' theories...","So what do I do now?");
            case 25 -> List.of("I hear it happened straight after dinner.");
            case 30 -> List.of("I hear Ithoi cooked the meal you ate that night.",
                "What is the mission Francois is doing?");
            case 35,40 -> List.of("I hear you cooked the meal they ate before getting sick.",
                "Maybe because the Captain's thinking of firing you.","I know you've faked the curse.",
                "I bet I can prove you're well enough to get up.");
            case 50 -> List.of("I've seen Ithoi running around. He's not sick at all.","I'll be back.");
            case 52 -> List.of("I'll be back.");
            case 45 -> List.of("I bet I can prove you're well enough to get up.");
            case 55 -> List.of("I've killed Ithoi for poisoning your crew.");
            default -> List.of("Let's go.");
        };
    }
    private boolean clickVisibleContinue(){
        Widget button=Microbot.getClientThread().invoke((java.util.function.Supplier<Widget>)()->{
            Widget w=net.runelite.client.plugins.microbot.util.widget.Rs2Widget.findWidget("Click here to continue");
            return w!=null && !w.isHidden()?w:null;
        });
        return button!=null && net.runelite.client.plugins.microbot.util.widget.Rs2Widget.clickWidget(button);
    }
    private void issue(Plan p,Frame f,BooleanSupplier action){
        if(pending!=null || Files.isRegularFile(ACTION)){hold("Unsettled action journal");return;}
        try{
            Files.createDirectories(HOME);Properties props=new Properties();
            props.setProperty("key",p.key());props.setProperty("accountKey",accountKey);
            props.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
            props.setProperty("before",f.signature());
            try(OutputStream out=Files.newOutputStream(ACTION)){props.store(out,"Corsair action before dispatch");}
            boolean accepted=action.getAsBoolean();pending=new Pending(p,f);
            LOG.info("[CorsairCurse] ACTION key={} accepted={} progress={} pos={}",p.key(),accepted,f.progress,f.pos);
            if(!accepted){
                if(p.key().equals("dialogue:continue")){
                    // A cutscene can swap the widget between observation and click.
                    // Keep the action pending so the next tick can prove a changed
                    // dialogue or use the existing single bounded widget fallback.
                    stage="VERIFY_REJECTED_CONTINUE";
                }else hold("Action dispatch rejected "+p.key());
            }
        }catch(Exception ex){hold("Action dispatch uncertain "+p.key()+": "+ex);}
    }
    private void verify(Frame f){
        Pending p=pending;boolean proved=false;
        if(f.loggedIn()){
            if(p.kind==Kind.COMBAT)proved=f.enemyId==NpcID.CORSCURS_NAVIGATOR_COMBAT
                || f.progress>p.progress;
            else if(p.key.startsWith("get:"))proved=f.count(p.item)>p.count;
            else if(p.key.startsWith("eat:"))proved=f.count(p.item)<p.count;
            else if(p.key.startsWith("dig:"))proved=f.cook>=2 || f.progress>p.progress
                || f.inDialogue!=p.inDialogue || !f.options.equals(p.options);
            else if(p.key.startsWith("look:"))proved=f.cabin>=2 || f.progress>p.progress
                || f.inDialogue!=p.inDialogue;
            else if(p.key.startsWith("burn:"))proved=f.progress>p.progress;
            else if(p.key.equals("off:Rimmington"))proved=f.pos!=null && f.pos.getPlane()==0
                && f.pos.distanceTo2D(DOCK)<=20 && p.pos!=null && p.pos.getPlane()==1;
            else if(p.key.startsWith("up:") || p.key.startsWith("down:")
                || p.key.startsWith("onto:") || p.key.startsWith("off:")
                || p.key.startsWith("enter:") || p.key.startsWith("leave:"))
                proved=f.pos!=null && p.pos!=null && !f.pos.equals(p.pos)
                    && (f.pos.getPlane()!=p.pos.getPlane() || inCavern(f.pos)!=inCavern(p.pos));
            else if(p.key.startsWith("dialogue:") || p.kind==Kind.NPC)
                proved=f.progress!=p.progress || f.thief!=p.thief || f.cabin!=p.cabin
                    || f.cook!=p.cook || f.navigator!=p.navigator
                    || f.inDialogue!=p.inDialogue || f.continueVisible!=p.continueVisible
                    || !f.dialogue.equals(p.dialogue) || !f.options.equals(p.options);
            else proved=f.progress!=p.progress || f.thief!=p.thief || f.cabin!=p.cabin
                || f.cook!=p.cook || f.navigator!=p.navigator;
        }
        if(proved){pending=null;FilesDeleteJournal();failures.remove(p.key);
            if(p.key.equals("eat:survival"))survivalMeals++;
            LOG.info("[CorsairCurse] PROVED key={} progress={} pos={}",p.key,f.progress,f.pos);return;}
        long deadline=p.key.startsWith("burn:") || p.key.startsWith("look:")?30000:12000;
        if(p.key.startsWith("dialogue:continue") && f.pos!=null && !inCove(f.pos)
            && !near(f.pos,FARM,20) && !near(f.pos,DOCK,20) && !inCavern(f.pos)
            && f.dialogue.isBlank() && System.currentTimeMillis()-p.at<60000){
            stage="WAIT_DIALOGUE_CUTSCENE_RESULT";return;
        }
        if(System.currentTimeMillis()-p.at>deadline){
            pending=null;int n=failures.merge(p.key,1,Integer::sum);
            if(p.key.equals("dialogue:continue") && n==1 && f.continueVisible
                && f.options.isEmpty() && !f.dialogue.isBlank() && f.progress==p.progress && f.dialogue.equals(p.dialogue)){
                supersedeJournal("unchanged continue prompt; one verified widget fallback");
                if(!error.isEmpty())return;
                issue(new Plan("dialogue:continue-widget",Kind.NPC,0,f.pos,0),f,this::clickVisibleContinue);return;
            }
            if(bossRoom(f)){
                supersedeJournal("unproved combat action "+p.key);
                if(!error.isEmpty())return;
                combatRetreat=true;retreat(f);return;
            }
            hold("Unproved action "+p.key+" attempt="+n+" progress="+f.progress+" pos="+f.pos);
        }else stage="VERIFY_"+p.key;
    }
    private void FilesDeleteJournal(){try{Files.deleteIfExists(ACTION);}catch(Exception ex){hold("Cannot clear action journal "+ex);}}
    private void supersedePending(String reason){
        pending=null;supersedeJournal(reason);
    }
    private void supersedeJournal(String reason){
        try{
            if(Files.isRegularFile(ACTION)){
                Path archived=HOME.resolve("superseded-"+System.currentTimeMillis()+".properties");
                Files.move(ACTION,archived,StandardCopyOption.REPLACE_EXISTING);
                LOG.warn("[CorsairCurse] ACTION_SUPERSEDED {} {}",archived,reason);
            }
        }catch(Exception ex){hold("Cannot supersede action for safe retreat: "+ex);}
    }
    private void farmSurvey(Frame f){
        try{
            Properties p=Microbot.getClientThread().invoke(()->{
                Properties survey=new Properties();
                survey.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
                survey.setProperty("observedAt",Long.toString(System.currentTimeMillis()));
                survey.setProperty("player",String.valueOf(f.pos));
                int count=0;
                for(Rs2NpcModel model:Microbot.getRs2NpcCache().query()
                    .within(f.pos,100).toListOnClientThread()){
                    NPC npc=model==null?null:model.getNpc();
                    if(npc==null || npc.getWorldLocation()==null)continue;
                    String name=String.valueOf(npc.getName());
                    if(!name.equalsIgnoreCase("Cow") && !name.equalsIgnoreCase("Cow calf")
                        && !name.equalsIgnoreCase("Dairy cow") && !name.equalsIgnoreCase("Dog")
                        && !name.equalsIgnoreCase("Sheepdog") && !name.equalsIgnoreCase("Chicken"))continue;
                    if(!near(npc.getWorldLocation(),pt(3025,3307,0),50))continue;
                    survey.setProperty("npc."+(count++),name+"#"+npc.getId()+" level="
                        +npc.getCombatLevel()+" at="+npc.getWorldLocation());
                    if(count>=50)break;
                }
                survey.setProperty("npcCount",Integer.toString(count));
                int gates=0;
                for(Rs2TileObjectModel object:Microbot.getRs2TileObjectCache().query()
                    .within(f.pos,100).toListOnClientThread()){
                    if(object==null || object.getWorldLocation()==null)continue;
                    String name=String.valueOf(object.getName());
                    if(!name.toLowerCase(Locale.ROOT).contains("gate")
                        || !near(object.getWorldLocation(),pt(3025,3307,0),50))continue;
                    survey.setProperty("gate."+(gates++),name+"#"+object.getId()
                        +" at="+object.getWorldLocation());
                    if(gates>=35)break;
                }
                survey.setProperty("gateCount",Integer.toString(gates));
                return survey;
            });
            Files.createDirectories(HOME);
            try(OutputStream out=Files.newOutputStream(HOME.resolve("farm-survey.properties"))){
                p.store(out,"Read-only live farm NPC and gate locations");
            }
            farmSurveyLogged=true;
            LOG.info("[CorsairCurse] FARM_SURVEY npcCount={} gateCount={} player={}",
                p.getProperty("npcCount"),p.getProperty("gateCount"),f.pos);
        }catch(Exception ex){
            farmSurveyLogged=true;
            LOG.warn("[CorsairCurse] FARM_SURVEY_UNAVAILABLE {}",ex.toString());
        }
    }
    /** Continue verified meals during ordinary HOLD when the quest owns input. */
    private boolean survivalEat(Frame f){
        if(pending!=null && pending.key.equals("eat:survival")){
            verify(f);return true;
        }
        int threshold=Math.max(12,(f.hpMax*4)/5);
        if(f.hp>threshold){if(f.hp>=f.hpMax)survivalMeals=0;return false;}
        int id=f.foodId();
        if(f.benignCowScene && f.progress<=10 && !bossSafetyActive && f.weapon>0
            && f.hp>Math.max(8,f.hpMax/2)) {
            int fitting=-1,bestHeal=0; boolean known=true;
            for(int candidate:FOOD) {
                if(f.count(candidate)<=0)continue;
                int heal=-1;
                for(var meal:net.runelite.client.plugins.microbot.util.misc.Rs2Food.values())
                    if(meal.getId()==candidate){heal=meal.getHeal();break;}
                if(heal<=0){known=false;break;}
                if(heal<=f.hpMax-f.hp && heal>bestHeal){fitting=candidate;bestHeal=heal;}
            }
            if(known && f.food()>0) {
                if(fitting<0)return false;
                id=fitting;
            }
        }
        if(services.blocksQuestInput()){
            stage="WAIT_HEALTH_SERVICE_LEASE";return true;
        }
        if(f.food()==0){
            LOG.error("[CorsairCurse] LOW_HP_NO_FOOD hp={}/{}",f.hp,f.hpMax);
            stage="LOW_HP_NO_FOOD";return true;
        }
        if(pending!=null){
            supersedePending("urgent health response");
            if(Files.isRegularFile(ACTION)){stage="HEALTH_JOURNAL_BLOCKED";return true;}
        }
        if(Files.isRegularFile(ACTION)){stage="HEALTH_JOURNAL_BLOCKED";return true;}
        if(id<0){stage="LOW_HP_FOOD_UNRECOGNIZED";return true;}
        // Inventory bounds consumption. Only a proved decrement settles a meal;
        // the pending action/journal blocks retries when consumption is uncertain.
        LOG.warn("[CorsairCurse] SURVIVAL_EAT_ATTEMPT hp={}/{} item={} mealsProved={}",
            f.hp,f.hpMax,id,survivalMeals);
        final int mealId=id;
        issue(new Plan("eat:survival",Kind.OBJECT,0,f.pos,mealId),f,
            ()->Rs2Inventory.interact(mealId,"Eat"));
        stage="VERIFY_SURVIVAL_EAT";return true;
    }
    private void replanAfterBossExit(Frame f){
        if(f.instanced || f.pos==null || f.pos.getPlane()!=0 || !inCove(f.pos)){
            hold("Boss exit recovery lacks mainland arrival proof");return;
        }
        if(pending!=null && !pending.key.equals("down:boss-safety")){
            hold("Unrelated pending action after boss exit");return;
        }
        try{
            // The known exit transition reconciles its own action, never another transaction.
            pending=null;Files.deleteIfExists(ACTION);
            Properties r=new Properties();r.setProperty("account",accountKey);
            r.setProperty("retreats",Integer.toString(++bossRetreats));
            r.setProperty("weapon",Integer.toString(f.weapon));
            r.setProperty("at",Long.toString(System.currentTimeMillis()));
            Path target=HOME.resolve("boss-retry-"+accountKey+".properties");
            Path tmp=target.resolveSibling(target.getFileName()+".tmp");
            try(OutputStream out=Files.newOutputStream(tmp)){r.store(out,"Verified boss retreat; bounded retry budget");}
            Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
            failedBossWeapon=f.weapon;prepared=false;prepAttempted=false;bankArrivalRetryUsed=false;
            bossSafetyActive=false;bossEscapeAttempted=false;bossEatAttempted=false;
            combatRetreat=false;combatStarted=false;bossMealsProved=0;
            if(bossRetreats>=2 || f.ithoiEquipmentReady()){
                hold("Boss safe exit proved; upgraded setup failed or retry budget exhausted");return;
            }
            error="";stage="REPLAN_BOSS_WEAPON_AND_SUPPLIES";
            LOG.info("[CorsairCurse] BOSS_EXIT_REPLAN retreat={} weapon={} hp={}/{} food={} acceptableWeapons={}",
                bossRetreats,f.weapon,f.hp,f.hpMax,f.food(),f.bossWeapons);
        }catch(Exception ex){hold("Boss retry checkpoint failed: "+ex);}
    }

    /** Safety may bypass an ordinary HOLD, but never another provider's live input lease. */
    private boolean bossSafety(Frame f,boolean hostile){
        // Start recovery before one more boss hit can make eating too late.
        boolean critical=f.hp<=Math.max(12,(f.hpMax*3)/4);
        if(!bossSafetyActive){
            if(!hostile || (!critical && error.isEmpty() && !combatRetreat))return false;
            bossSafetyActive=true;
            LOG.warn("[CorsairCurse] BOSS_SAFETY_START hp={}/{} error={} pending={}",
                f.hp,f.hpMax,error,pending==null?"none":pending.key);
        }
        if(!hostile){
            if(f.pos!=null && f.pos.getPlane()==0 && inCove(f.pos)){
                bossSafetyActive=false;bossEatAttempted=false;bossEscapeAttempted=false;
                bossMealsProved=0;
                combatRetreat=false;combatStarted=false;
                replanAfterBossExit(f);
            }else stage="BOSS_SAFETY_LOCATION_UNPROVED";
            return true;
        }
        // A cancelled/HOLD provider can retain the hub lease until it yields. Never click over it.
        if(services.blocksQuestInput()){
            if(!bossCancelRequested){services.cancel("boss safety needs input lease");bossCancelRequested=true;}
            stage="WAIT_BOSS_PROVIDER_RELEASE";return true;
        }
        bossCancelRequested=false;
        if(pending!=null){
            Pending p=pending;
            if(p.key.startsWith("eat:")){
                if(f.count(p.item)<p.count){
                    pending=null;FilesDeleteJournal();failures.remove(p.key);
                    bossEatAttempted=false;bossMealsProved++;
                    LOG.info("[CorsairCurse] BOSS_EAT_PROVED item={}",p.item);
                    if(error.isEmpty() && !combatRetreat
                        && f.hp>Math.max(12,(f.hpMax*3)/4) && f.food()>=6){
                        bossSafetyActive=false;bossEatAttempted=false;
                        combatStarted=f.enemyId==NpcID.CORSCURS_NAVIGATOR_COMBAT;
                        combatAt=System.currentTimeMillis();
                        stage="BOSS_EAT_PROVED_RESUME";return true;
                    }
                    stage="BOSS_EAT_PROVED";return true;
                }
                if(System.currentTimeMillis()-p.at<=6000){stage="VERIFY_BOSS_EAT";return true;}
                supersedePending("boss Eat unproved; do not repeat");
                if(Files.isRegularFile(ACTION)){stage="BOSS_JOURNAL_BLOCKED";return true;}
                LOG.warn("[CorsairCurse] BOSS_EAT_UNPROVED; attempting one exit, no second Eat");
            }else if(p.key.equals("down:boss-safety")){
                bossEscapeAttempted=true;
                if(f.pos!=null && f.pos.getPlane()==0 && inCove(f.pos)){
                    pending=null;FilesDeleteJournal();bossSafetyActive=false;
                    bossEatAttempted=false;bossEscapeAttempted=false;
                    combatRetreat=false;combatStarted=false;
                    replanAfterBossExit(f);
                    return true;
                }
                if(System.currentTimeMillis()-p.at<=12000){stage="VERIFY_BOSS_EXIT";return true;}
                pending=null; // Keep ACTION on disk to prevent a restart from repeating this uncertain click.
                hold("Boss exit unproved after one stair click; location="+f.pos);
                stage="BOSS_EXIT_UNPROVED";return true;
            }else{
                supersedePending("ordinary action superseded by boss safety");
                if(Files.isRegularFile(ACTION)){stage="BOSS_JOURNAL_BLOCKED";return true;}
            }
        }
        if(Files.isRegularFile(ACTION)){stage="BOSS_JOURNAL_BLOCKED";return true;}
        // An ordinary HOLD can arrive while injured above the critical threshold.
        // Prove one eat before attempting the exit in that case as well.
        if(!bossEatAttempted
            && f.hp<=Math.max(12,(f.hpMax*3)/4) && f.food()>0){
            bossEatAttempted=true;
            eat(f);stage="BOSS_EAT_DISPATCHED";return true;
        }
        if(bossEscapeAttempted){stage="BOSS_EXIT_ALREADY_ATTEMPTED";return true;}
        if(!bossRoom(f)){
            hold("Boss safety has no proved Ithoi scene from "+f.pos+
                " template="+f.templatePos);
            stage="BOSS_EXIT_ROUTE_UNPROVED";return true;
        }
        Rs2TileObjectModel stairs=Microbot.getRs2TileObjectCache().query()
            .withId(ObjectID.DS2_CORSAIR_COVE_STAIRS_RAMP)
            .within(f.pos,12).nearestOnClientThread();
        if(stairs==null){
            hold("Boss exit stair absent near raw "+f.pos+" template="+f.templatePos);
            stage="BOSS_EXIT_OBJECT_ABSENT";return true;
        }
        WorldPoint stairPoint=stairs.getWorldLocation();
        if(stairPoint==null || stairPoint.getPlane()!=1 || !near(f.pos,stairPoint,12)
            || stairs.getClickbox()==null){
            hold("Boss exit stair unverified at "+stairPoint+" from "+f.pos);
            stage="BOSS_EXIT_OBJECT_UNPROVED";return true;
        }
        LOG.warn("[CorsairCurse] BOSS_EXIT_OBSERVED raw={} template={} stair={} id={}",
            f.pos,f.templatePos,stairPoint,stairs.getId());
        bossEscapeAttempted=true;
        issue(new Plan("down:boss-safety",Kind.OBJECT,stairs.getId(),stairPoint,0),f,stairs::click);
        stage="BOSS_EXIT_DISPATCHED";return true;
    }
    private void eat(Frame f){
        int id=f.foodId();if(id<0){combatRetreat=true;retreat(f);return;}
        Plan p=new Plan("eat:combat",Kind.OBJECT,0,f.pos,id);
        issue(p,f,()->Rs2Inventory.interact(id,"Eat"));
    }
    private void retreat(Frame f){
        stage="EMERGENCY_RETREAT";
        if(bossRoom(f)){
            bossSafetyActive=true;bossSafety(f,true);return;
        }
        if(f.pos.getPlane()==0 && inCove(f.pos)){
            combatRetreat=false;combatStarted=false;
            hold("Escaped boss hut with insufficient food; shared recovery/prep required");return;
        }
        hold("Emergency retreat route not proved from "+f.pos);
    }
    private void login(Frame f){
        if(!armed() || ownsInput==null || !ownsInput.getAsBoolean() || InputArbiter.isHuman()){
            stage="WAIT_LOGIN_ARMED";return;
        }
        if(!"LOGIN_SCREEN".equals(f.game) || (f.loginIndex!=10 && f.loginIndex!=34)){
            stage="WAIT_LOGIN_SCENE_"+f.loginIndex;return;
        }
        if(selectedWorld==0){selectedWorld=LoginManager.getRandomWorld(false);
            if(selectedWorld<=0 || LoginManager.isMemberWorld(selectedWorld)){hold("No verified free world");return;}
        }
        if(loginAttempts++==0){loginAt=System.currentTimeMillis();stage="VERIFY_NATIVE_LOGIN";
            if(!LoginManager.login(selectedWorld))hold("Native login rejected");
        }else if(System.currentTimeMillis()-loginAt>20000)
            hold("Native login did not advance; no repeated click");
    }
    private boolean armed(){
        // An explicit file disarm overrides an older enabled config approval.
        if(Files.isRegularFile(CONTROL)){
            try(InputStream in=Files.newInputStream(CONTROL)){
                Properties p=new Properties();p.load(in);
                if(!"true".equals(p.getProperty("enableActions")))return false;
            }catch(Exception ex){return false;}
        }
        if(config!=null && config.allowActions() && config.approvedPid()==ProcessHandle.current().pid()
            && config.approvedBuild()==BUILD_NUMBER && classHash.matches("[a-f0-9]{64}")
            && classHash.equalsIgnoreCase(config.approvedSha256()))return true;
        try(InputStream in=Files.newInputStream(CONTROL)){
            Properties p=new Properties();p.load(in);
            return "true".equals(p.getProperty("enableActions"))
                && Long.toString(ProcessHandle.current().pid()).equals(p.getProperty("expectedPid"))
                && Integer.toString(BUILD_NUMBER).equals(p.getProperty("expectedBuild"))
                && classHash.equalsIgnoreCase(p.getProperty("expectedClassSha",""));
        }catch(Exception ex){return false;}
    }
    private boolean gameplayInputAllowed(){
        return !stopped && armed() && ownsInput!=null && ownsInput.getAsBoolean()
            && !Microbot.pauseAllScripts.get() && !InputArbiter.isHuman();
    }
    private String mode(){
        if(config==null || config.preflightOnly())return "LOGIN_ONLY";
        try(InputStream in=Files.newInputStream(CONTROL)){
            Properties p=new Properties();p.load(in);
            if(!armed())return "LOGIN_ONLY";
            return "QUEST".equalsIgnoreCase(p.getProperty("mode",""))?"QUEST":"LOGIN_ONLY";
        }catch(Exception ex){return "LOGIN_ONLY";}
    }
    private void status(Frame f){
        try{
            Files.createDirectories(HOME);Properties p=new Properties();
            p.setProperty("timestamp",Long.toString(System.currentTimeMillis()));
            p.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
            p.setProperty("build",Integer.toString(BUILD_NUMBER));p.setProperty("classSha256",classHash);
            p.setProperty("stage",stage);p.setProperty("error",error);p.setProperty("mode",mode());
            p.setProperty("armed",Boolean.toString(armed()));
            p.setProperty("bossSafetyActive",Boolean.toString(bossSafetyActive));
            p.setProperty("bossEatAttempted",Boolean.toString(bossEatAttempted));
            p.setProperty("bossMealsProved",Integer.toString(bossMealsProved));
            p.setProperty("bossEscapeAttempted",Boolean.toString(bossEscapeAttempted));
            p.setProperty("bossRetreats",Integer.toString(bossRetreats));
            p.setProperty("weaponScore",Integer.toString(f.weaponScore));
            p.setProperty("requiredWeaponScore",Integer.toString(f.requiredWeaponScore));
            p.setProperty("bossWeapons",f.bossWeapons.toString());
            p.setProperty("trainingRequests",Integer.toString(trainingRequests));
            p.setProperty("trainingNoProgressRequests",Integer.toString(trainingNoProgressRequests));
            p.setProperty("sharedServices",QuestServiceHub.snapshot().toString());
            if(f!=null){
                p.setProperty("game",f.game);p.setProperty("quest",f.quest);
                p.setProperty("progress",Integer.toString(f.progress));
                p.setProperty("thief",Integer.toString(f.thief));p.setProperty("cabin",Integer.toString(f.cabin));
                p.setProperty("cook",Integer.toString(f.cook));p.setProperty("navigator",Integer.toString(f.navigator));
                p.setProperty("position",String.valueOf(f.pos));
                p.setProperty("instanceTemplate",String.valueOf(f.templatePos));
                p.setProperty("instanced",Boolean.toString(f.instanced));
                p.setProperty("hp",f.hp+"/"+f.hpMax);
                p.setProperty("combat",Integer.toString(f.combat));p.setProperty("weapon",Integer.toString(f.weapon));
                p.setProperty("attackXp",Long.toString(f.attackXp));
                p.setProperty("strengthXp",Long.toString(f.strengthXp));
                p.setProperty("defenceXp",Long.toString(f.defenceXp));
                p.setProperty("hitpointsXp",Long.toString(f.hpXp));
                p.setProperty("rangedXp",Long.toString(f.rangedXp));
                p.setProperty("magicXp",Long.toString(f.magicXp));
                p.setProperty("skill.HITPOINTS.xp",Long.toString(f.hpXp));
                p.setProperty("skill.HITPOINTS.level",Integer.toString(f.hpMax));
                p.setProperty("combatXp",Long.toString(f.combatXp));
                p.setProperty("armour",Integer.toString(f.armour));p.setProperty("food",Integer.toString(f.food()));
                p.setProperty("magicDefence",Integer.toString(f.magicDefence));
                p.setProperty("equipmentStatsKnown",Boolean.toString(f.equipmentStatsKnown));
                p.setProperty("healingBudget",Integer.toString(f.healing()));
                p.setProperty("inventoryItems",f.items.toString());
                p.setProperty("accountHash",Long.toString(f.accountHash));
                p.setProperty("accountKey",f.accountKey);
            }
            Path tmp=HOME.resolve("status.tmp");
            try(OutputStream out=Files.newOutputStream(tmp)){p.store(out,"Corsair Curse candidate");}
            Files.move(tmp,STATUS,StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception ex){LOG.warn("[CorsairCurse] status {}",ex.toString());}
    }
    private void navigationGeometryProbe(){
        long now=System.currentTimeMillis();
        if(navProbeSamples>=12 || now-navProbeLastAt<1000)return;
        navProbeLastAt=now;
        try{
            Properties probe=Microbot.getClientThread().invoke(()->{
                Client c=Microbot.getClient();Properties p=new Properties();
                p.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
                p.setProperty("stage",stage);
                p.setProperty("time",Long.toString(System.currentTimeMillis()));
                p.setProperty("position",String.valueOf(c.getLocalPlayer().getWorldLocation()));
                p.setProperty("resized",Boolean.toString(c.isResized()));
                p.setProperty("minimapZoom",Double.toString(c.getMinimapZoom()));
                p.setProperty("varbit4607",Integer.toString(c.getVarbitValue(4607)));
                Widget widget=Rs2MiniMap.getMinimapDrawWidget();
                p.setProperty("widget",widget==null?"null":widget.getId()+" "+widget.getBounds()+" hidden="+widget.isHidden());
                java.awt.Shape clip=Rs2MiniMap.getMinimapClipArea();
                p.setProperty("clipBounds",clip==null?"null":String.valueOf(clip.getBounds()));
                try{
                    Class<?> guard=Class.forName("net.runelite.client.plugins.microbot.questcommon.navigation.guard.RouteInputGuard");
                    java.lang.reflect.Field enabled=guard.getDeclaredField("enabled");enabled.setAccessible(true);
                    java.lang.reflect.Field approved=guard.getDeclaredField("APPROVED");approved.setAccessible(true);
                    Object record=((java.util.concurrent.atomic.AtomicReference<?>)approved.get(null)).get();
                    p.setProperty("guardEnabled",Boolean.toString(enabled.getBoolean(null)));
                    p.setProperty("approval",record==null?"null":record.toString());
                    p.setProperty("guardDecision",String.valueOf(guard.getMethod("diagnostic").invoke(null)));
                    java.lang.reflect.Field providers=QuestServiceHub.class.getDeclaredField("PROVIDERS");
                    providers.setAccessible(true);
                    Object nav=((Map<?,?>)providers.get(null)).get(QuestServiceHub.Kind.NAVIGATION);
                    if(nav!=null){
                        java.lang.reflect.Field driver=nav.getClass().getDeclaredField("driver");
                        driver.setAccessible(true);Object liveDriver=driver.get(nav);
                        p.setProperty("approvalStatus",String.valueOf(liveDriver.getClass()
                            .getMethod("approvalStatus").invoke(liveDriver)));
                    }
                }catch(Exception probeError){p.setProperty("guardProbeError",probeError.toString());}
                WorldPoint here=c.getLocalPlayer().getWorldLocation();
                WorldPoint[] points={here,pt(2974,3341,0),pt(2972,3342,0),
                    pt(2970,3345,0),pt(2965,3354,0),pt(2976,3340,0)};
                for(int i=0;i<points.length;i++){
                    WorldPoint target=points[i];
                    net.runelite.api.coords.LocalPoint local=net.runelite.api.coords.LocalPoint.fromWorld(
                        c.getTopLevelWorldView(),target);
                    net.runelite.api.Point screen=Rs2MiniMap.worldToMinimap(target);
                    p.setProperty("tile"+i,target+" local="+local+" point="+screen+
                        " clipContains="+(screen!=null && clip!=null &&
                        clip.contains(screen.getX(),screen.getY())));
                }
                return p;
            });
            int sample=++navProbeSamples;
            Path tmp=HOME.resolve("navigation-geometry-"+sample+".tmp");
            try(OutputStream out=Files.newOutputStream(tmp)){probe.store(out,"Read-only minimap geometry at NAVIGATION HOLD");}
            Files.move(tmp,HOME.resolve("navigation-geometry-"+sample+".properties"),StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception ex){LOG.warn("[CorsairCurse] navigation geometry probe {}",ex.toString());}
    }
    private void hold(String reason){if(error.isEmpty())LOG.error("[CorsairCurse] HOLD {}",reason);
        error=reason;stage="HOLD";services.cancel(reason);}
    private static WorldPoint pt(int x,int y,int z){return new WorldPoint(x,y,z);}
    private static Plan npc(String key,int id,WorldPoint at){return new Plan(key,Kind.NPC,id,at,0);}
    private static Plan object(String key,int id,WorldPoint at){return new Plan(key,Kind.OBJECT,id,at,0);}
    private static boolean near(WorldPoint a,WorldPoint b,int radius){
        return a!=null && b!=null && a.getPlane()==b.getPlane()
            && Math.max(Math.abs(a.getX()-b.getX()),Math.abs(a.getY()-b.getY()))<=radius;
    }
    private static boolean inCavern(WorldPoint p){return p!=null && p.getX()>=1876 && p.getX()<=2073
        && p.getY()>=8960 && p.getY()<=9093;}
    private static boolean inCove(WorldPoint p){return p!=null && p.getX()>=2308 && p.getX()<=2705
        && p.getY()>=2806 && p.getY()<=3136;}
    private static boolean inRoom(WorldPoint p,WorldPoint anchor){
        if(p==null || p.getPlane()!=1)return false;
        int x=p.getX(),y=p.getY();
        if(anchor==ITHOI)return x>=2527 && x<=2532 && y>=2835 && y<=2841;
        if(anchor==GNOCCI)return x>=2543 && x<=2547 && y>=2860 && y<=2864;
        if(anchor==ARSEN || anchor==COLIN)return x>=2553 && x<=2559 && y>=2853 && y<=2859;
        if(anchor==TOCK)return x>=2573 && x<=2583 && y>=2835 && y<=2837;
        return false;
    }
    private static boolean bossRoom(Frame f){
        return f!=null && f.progress==52 && f.pos!=null &&
            (ithoiRoom(f)
                || f.enemyId==NpcID.CORSCURS_NAVIGATOR_COMBAT);
    }
    private static boolean ithoiRoom(Frame f){
        return f!=null && f.pos!=null && f.pos.getPlane()==1 &&
            (inRoom(f.pos,ITHOI) || (f.instanced && inRoom(f.templatePos,ITHOI)));
    }
    private static boolean sameRoom(WorldPoint a,WorldPoint b){
        if(a==null || b==null || a.getPlane()!=b.getPlane())return false;
        for(WorldPoint point:Arrays.asList(ITHOI,GNOCCI,ARSEN,TOCK))
            if(inRoom(a,point) && inRoom(b,point))return true;
        return false;
    }
    private static String norm(String text){return text==null?"":text.replaceAll("<[^>]*>","")
        .trim().toLowerCase(Locale.ROOT);}
    private static String classSha(){
        try{
            Path source=Paths.get(CorsairCurseScript.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            String entry=CorsairCurseScript.class.getName().replace('.','/')+".class";
            byte[] bytes;
            if(Files.isDirectory(source))bytes=Files.readAllBytes(source.resolve(entry));
            else try(java.util.jar.JarFile jar=new java.util.jar.JarFile(source.toFile());
                InputStream in=jar.getInputStream(jar.getJarEntry(entry))){bytes=in.readAllBytes();}
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        }catch(Exception ex){return "UNKNOWN";}
    }

        /** Value-only file protocol. This source is compiled separately into provider and quest artifacts. */
    private static final class TrainingMaintenanceProtocol {
        public static final String PROOF_PREFIX = "TRAINING_MAINTENANCE_YIELD_V1";
        private static final long STATUS_MAX_AGE_MS = 10_000;
        private static final long WAIT_MAX_MS = 120_000;
        private TrainingMaintenanceProtocol() { }

        public enum ProbeState { NONE, READY, INVALID }
        public enum WaitState { WAITING, APPLIED, FAILED }
        public record Update(long pid, String accountKey, String requestId,
            int fromGeneration, String fromArtifact, int generation, String artifact,
            String manifest, String marker, String parentAbi, long requestedAt) { }
        public record Probe(ProbeState state, Update update, String reason) { }
        public record Receipt(Update update, String checkpointName, String checkpointSha,
            long createdAt) { }
        public record Wait(WaitState state, String reason) { }

        /**
         * Returns NONE when no newer update is requested. A present but malformed/stale request is
         * INVALID so the training provider can stop issuing new attacks and fail closed at a safe point.
         */
        public static Probe inspect(Path hotHome, long pid, String accountKey, String requestId, long now) {
            if (hotHome == null || pid <= 0 || !hash(accountKey) || !safeId(requestId))
                return invalid("invalid caller identity");
            Path requestFile = hotHome.resolve("request.properties");
            if (!Files.exists(requestFile)) return new Probe(ProbeState.NONE, null, "no request");
            try {
                Properties req = load(requestFile);
                Properties status = load(hotHome.resolve("status.properties"));
                long statusPid = number(status, "pid");
                long statusAt = number(status, "timestamp");
                int from = Math.toIntExact(number(status, "generation"));
                String phase = status.getProperty("phase", "");
                String fromArtifact = status.getProperty("artifactSha", "");
                String oldMarker = status.getProperty("marker", "");
                String parentAbi = status.getProperty("parentAbi", "");
                int generation = Integer.parseInt(req.getProperty("generation", "0"));
                String artifact = req.getProperty("artifactSha256", "");
                String manifest = req.getProperty("manifestSha256", "");
                if (statusPid != pid || !"APPLIED".equals(phase) && !"DEFERRED".equals(phase)
                    || statusAt <= 0 || now < statusAt || now - statusAt > STATUS_MAX_AGE_MS
                    || !hash(fromArtifact) || !hash(oldMarker) || !hash(parentAbi))
                    return invalid("provider status is stale, failed, or belongs to another client");
                if (!hash(artifact) || !hash(manifest) || generation <= 0)
                    return invalid("request hashes/generation malformed");
                if (generation <= from) {
                    if (generation == from && artifact.equals(fromArtifact))
                        return new Probe(ProbeState.NONE, null, "requested generation already loaded");
                    return generation < from ? new Probe(ProbeState.NONE, null, "request older than loaded provider")
                        : invalid("same generation names a different provider artifact");
                }
                Path jar = hotHome.resolve(artifact + ".jar");
                Path manifestFile = hotHome.resolve(manifest + ".properties");
                byte[] manifestBytes = Files.readAllBytes(manifestFile);
                if (!manifest.equals(sha(manifestBytes)) || !artifact.equals(sha(Files.readAllBytes(jar))))
                    return invalid("immutable artifact/manifest digest mismatch");
                Properties descriptor = load(manifestFile);
                String marker = descriptor.getProperty("implementationMarker", "");
                String requestedAbi = descriptor.getProperty("parentAbiSha256", "");
                if (!"1".equals(descriptor.getProperty("format"))
                    || !Integer.toString(generation).equals(descriptor.getProperty("generation"))
                    || !artifact.equals(descriptor.getProperty("artifactSha256"))
                    || !parentAbi.equals(requestedAbi) || !hash(marker))
                    return invalid("request manifest does not match loaded parent ABI or artifact");
                Update update = new Update(pid, accountKey, requestId, from, fromArtifact,
                    generation, artifact, manifest, marker, parentAbi, now);
                return new Probe(ProbeState.READY, update, "newer verified provider request");
            } catch (Exception failure) {
                return invalid("cannot verify provider request: " + failure.getClass().getSimpleName());
            }
        }

        /** Persist a value-only same-process/account/request checkpoint before releasing the lease. */
        public static Receipt checkpoint(Path root, Update update, int x, int y, int plane, int world,
            int combat, int hpLevel, int hp, int maxHp, long attackXp, long strengthXp,
            long defenceXp, long rangedXp, long magicXp, long hpXp,
            Map<Integer,Integer> inventory, Set<Integer> equipment, long now) throws IOException {
            Objects.requireNonNull(root); Objects.requireNonNull(update);
            if (x < 0 || y < 0 || plane < 0 || plane > 3 || world <= 0 || combat < 1
                || hpLevel < 10 || hp < 1 || maxHp < 1 || now < update.requestedAt())
                throw new IOException("invalid fresh training checkpoint frame");
            Files.createDirectories(root);
            String name = "training-" + update.pid() + "-" + update.accountKey() + "-"
                + update.requestId() + "-g" + update.generation() + ".properties";
            Path target = root.resolve(name);
            Properties p = new Properties();
            p.setProperty("schema", "1"); p.setProperty("pid", Long.toString(update.pid()));
            p.setProperty("accountKey", update.accountKey()); p.setProperty("requestId", update.requestId());
            p.setProperty("fromGeneration", Integer.toString(update.fromGeneration()));
            p.setProperty("fromArtifact", update.fromArtifact());
            p.setProperty("generation", Integer.toString(update.generation()));
            p.setProperty("artifact", update.artifact()); p.setProperty("manifest", update.manifest());
            p.setProperty("marker", update.marker()); p.setProperty("parentAbi", update.parentAbi());
            p.setProperty("createdAt", Long.toString(now)); p.setProperty("world", Integer.toString(world));
            p.setProperty("x", Integer.toString(x)); p.setProperty("y", Integer.toString(y));
            p.setProperty("plane", Integer.toString(plane)); p.setProperty("combat", Integer.toString(combat));
            p.setProperty("hpLevel", Integer.toString(hpLevel)); p.setProperty("hp", Integer.toString(hp));
            p.setProperty("maxHp", Integer.toString(maxHp)); p.setProperty("attackXp", Long.toString(attackXp));
            p.setProperty("strengthXp", Long.toString(strengthXp)); p.setProperty("defenceXp", Long.toString(defenceXp));
            p.setProperty("rangedXp", Long.toString(rangedXp)); p.setProperty("magicXp", Long.toString(magicXp));
            p.setProperty("hpXp", Long.toString(hpXp)); p.setProperty("inventory", encodeInventory(inventory));
            p.setProperty("equipment", encodeSet(equipment));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            p.store(bytes, "Training maintenance checkpoint; no live game objects");
            byte[] data = bytes.toByteArray();
            if (Files.exists(target)) {
                byte[] existing = Files.readAllBytes(target);
                if (!sha(existing).equals(sha(data))) throw new IOException("checkpoint path already occupied");
                return new Receipt(update, name, sha(existing), now);
            }
            Path temp = Files.createTempFile(root, ".training-maintenance-", ".tmp");
            try {
                try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(data);
                    while (buffer.hasRemaining()) channel.write(buffer);
                    channel.force(true);
                }
                try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException unsupported) {
                    throw new IOException("atomic checkpoint commit unsupported; do not yield", unsupported);
                }
            } finally { Files.deleteIfExists(temp); }
            return new Receipt(update, name, sha(data), now);
        }

        public static String proof(Receipt r) {
            Update u = r.update();
            return PROOF_PREFIX + "|" + u.pid() + "|" + u.accountKey() + "|" + u.requestId()
                + "|" + u.fromGeneration() + "|" + u.fromArtifact() + "|" + u.generation()
                + "|" + u.artifact() + "|" + u.manifest() + "|" + u.marker()
                + "|" + u.parentAbi() + "|" + r.checkpointName() + "|" + r.checkpointSha()
                + "|" + r.createdAt();
        }

        public static Receipt parseProof(String proof) {
            try {
                String[] a = proof.split("\\|", -1);
                if (a.length != 14 || !PROOF_PREFIX.equals(a[0])) return null;
                long pid = Long.parseLong(a[1]); String account = a[2], requestId = a[3];
                int from = Integer.parseInt(a[4]); String fromArtifact = a[5];
                int gen = Integer.parseInt(a[6]); String artifact = a[7], manifest = a[8], marker = a[9];
                String parentAbi = a[10], checkpoint = a[11], checkpointSha = a[12];
                long createdAt = Long.parseLong(a[13]);
                if (pid <= 0 || !hash(account) || !safeId(requestId) || from < 0 || gen <= from
                    || !hash(fromArtifact) || !hash(artifact) || !hash(manifest) || !hash(marker)
                    || !hash(parentAbi)
                    || !checkpoint.matches("training-[0-9]+-[a-f0-9]{64}-[A-Za-z0-9._-]{1,80}-g[0-9]+\\.properties")
                    || !hash(checkpointSha) || createdAt <= 0) return null;
                Update u = new Update(pid, account, requestId, from, fromArtifact, gen,
                    artifact, manifest, marker, parentAbi, createdAt);
                return new Receipt(u, checkpoint, checkpointSha, createdAt);
            } catch (Exception failure) { return null; }
        }

        /** Quest-side gate: only exact proof + immutable request/checkpoint + exact applied status resumes. */
        public static Wait await(Path hotHome, Path checkpointRoot, Receipt receipt,
            long pid, String accountKey, String requestId, long now) {
            if (receipt == null || receipt.update().pid() != pid
                || !receipt.update().accountKey().equals(accountKey)
                || !receipt.update().requestId().equals(requestId))
                return failed("maintenance proof identity mismatch");
            try {
                Update u = receipt.update();
                Properties req = load(hotHome.resolve("request.properties"));
                if (!Integer.toString(u.generation()).equals(req.getProperty("generation"))
                    || !u.artifact().equals(req.getProperty("artifactSha256"))
                    || !u.manifest().equals(req.getProperty("manifestSha256")))
                    return failed("requested provider changed while maintenance was pending");
                Path manifestPath = hotHome.resolve(u.manifest() + ".properties");
                if (!u.manifest().equals(sha(Files.readAllBytes(manifestPath)))
                    || !u.artifact().equals(sha(Files.readAllBytes(hotHome.resolve(u.artifact() + ".jar"))))
                    || !u.marker().equals(load(manifestPath).getProperty("implementationMarker"))
                    || !u.parentAbi().equals(load(manifestPath).getProperty("parentAbiSha256")))
                    return failed("requested provider artifact changed after yield");
                Path checkpoint = checkpointRoot.resolve(receipt.checkpointName());
                byte[] checkpointBytes = Files.readAllBytes(checkpoint);
                if (!receipt.checkpointSha().equals(sha(checkpointBytes)))
                    return failed("checkpoint digest mismatch");
                Properties cp = load(checkpoint);
                if (!"1".equals(cp.getProperty("schema"))
                    || !Long.toString(pid).equals(cp.getProperty("pid"))
                    || !accountKey.equals(cp.getProperty("accountKey"))
                    || !requestId.equals(cp.getProperty("requestId"))
                    || !Integer.toString(u.generation()).equals(cp.getProperty("generation"))
                    || !u.artifact().equals(cp.getProperty("artifact"))
                    || !u.manifest().equals(cp.getProperty("manifest"))
                    || !u.marker().equals(cp.getProperty("marker")))
                    return failed("checkpoint identity or request mismatch");
                long created = number(cp, "createdAt");
                if (created != receipt.createdAt() || now < created || now - created > WAIT_MAX_MS)
                    return failed("maintenance application timed out or checkpoint time mismatch");
                Properties status = load(hotHome.resolve("status.properties"));
                if (number(status, "pid") != pid) return failed("provider status PID mismatch");
                if (!u.parentAbi().equals(status.getProperty("parentAbi")))
                    return failed("provider parent ABI changed during reload");
                long timestamp = number(status, "timestamp");
                if (timestamp <= 0 || timestamp > now || now - timestamp > STATUS_MAX_AGE_MS)
                    return new Wait(WaitState.WAITING, "waiting for fresh provider host status");
                String phase = status.getProperty("phase", "");
                if ("FAILED_CLOSED".equals(phase)) return failed("provider host entered FAILED_CLOSED");
                int generation = Math.toIntExact(number(status, "generation"));
                String artifact = status.getProperty("artifactSha", "");
                String marker = status.getProperty("marker", "");
                if (generation == u.generation() && artifact.equals(u.artifact())
                    && marker.equals(u.marker()) && "APPLIED".equals(phase))
                    return new Wait(WaitState.APPLIED, "exact requested provider marker loaded");
                if (generation > u.generation()) return failed("provider advanced past requested generation");
                if (generation == u.generation()) return failed("generation loaded with wrong artifact or marker");
                return new Wait(WaitState.WAITING, "provider update not yet applied");
            } catch (Exception failure) {
                return failed("maintenance verification failed: " + failure.getClass().getSimpleName());
            }
        }

        public static boolean matches(Receipt r, long pid, String account, String requestId) {
            return r != null && r.update().pid() == pid && r.update().accountKey().equals(account)
                && r.update().requestId().equals(requestId);
        }
        private static Probe invalid(String why) { return new Probe(ProbeState.INVALID, null, why); }
        private static Wait failed(String why) { return new Wait(WaitState.FAILED, why); }
        private static Properties load(Path path) throws IOException {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(path)) { p.load(in); }
            return p;
        }
        private static long number(Properties p, String key) {
            return Long.parseLong(p.getProperty(key, ""));
        }
        private static boolean hash(String s) { return s != null && s.matches("[a-f0-9]{64}"); }
        private static boolean safeId(String id) { return id != null && id.matches("[A-Za-z0-9._-]{1,80}"); }
        private static String sha(byte[] data) throws IOException {
            try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
            catch (Exception failure) { throw new IOException("SHA-256 unavailable", failure); }
        }
        private static String encodeInventory(Map<Integer,Integer> inventory) throws IOException {
            if (inventory == null || inventory.size() > 28) throw new IOException("invalid inventory checkpoint");
            StringJoiner out = new StringJoiner(",");
            inventory.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
                if (e.getKey() <= 0 || e.getValue() <= 0) throw new IllegalArgumentException("invalid inventory item");
                out.add(e.getKey() + "=" + e.getValue());
            });
            return out.toString();
        }
        private static String encodeSet(Set<Integer> values) throws IOException {
            if (values == null || values.size() > 14 || values.stream().anyMatch(i -> i == null || i <= 0))
                throw new IOException("invalid equipment checkpoint");
            return values.stream().sorted().map(String::valueOf).reduce((a,b) -> a + "," + b).orElse("");
        }
    }

}

