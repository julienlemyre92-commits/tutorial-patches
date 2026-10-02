package net.runelite.client.plugins.microbot.knightssword;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.events.WelcomeScreenEvent;
import net.runelite.client.plugins.microbot.util.input.InputArbiter;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.LoginManager;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.questcommon.acquisition.QuestGeBuyer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Isolated candidate. Quest progress, inventory, and crossing proof drive every step. */
public class KnightsSwordScript extends Script {
    private static final Logger LOG=LoggerFactory.getLogger(KnightsSwordScript.class);
    public static final int BUILD_NUMBER=5;
    private static final int VARP=122, PIE=ItemID.REDBERRY_PIE, BAR=ItemID.IRON_BAR;
    private static final int COINS=ItemID.COINS;
    private static final int PORTRAIT=ItemID.KNIGHTS_PORTRAIT, ORE=ItemID.BLURITE_ORE;
    private static final int SWORD=ItemID.FALADIAN_SWORD;
    private static final int[] PICKS={ItemID.RUNE_PICKAXE,ItemID.ADAMANT_PICKAXE,ItemID.MITHRIL_PICKAXE,
        ItemID.STEEL_PICKAXE,ItemID.IRON_PICKAXE,ItemID.BRONZE_PICKAXE};
    private static final int[] FOOD={ItemID.LOBSTER,ItemID.SALMON,ItemID.TROUT};
    private static final WorldPoint SQUIRE=new WorldPoint(2978,3341,0);
    private static final WorldPoint RELDO=new WorldPoint(3211,3494,0);
    private static final WorldPoint THURGO=new WorldPoint(3000,3145,0);
    private static final WorldPoint LADDER0=new WorldPoint(2994,3341,0);
    private static final WorldPoint STAIR1=new WorldPoint(2985,3338,1);
    private static final WorldPoint CUPBOARD=new WorldPoint(2985,3336,2);
    private static final WorldPoint TRAPDOOR=new WorldPoint(3009,3150,0);
    private static final WorldPoint CAVE_LADDER=new WorldPoint(3009,9550,0);
    private static final WorldPoint BLURITE=new WorldPoint(3049,9566,0);
    private static final WorldPoint FALADOR_BANK=new WorldPoint(2946,3369,0);
    private static final Path STATUS=Paths.get(System.getProperty("user.home"),".runelite","knightssword","status.properties");
    private static final Path CONTROL=STATUS.resolveSibling("control.properties");
    private static final Path ACTION=STATUS.resolveSibling("pending-action.properties");

    private static final Path BUY_STATE=STATUS.resolveSibling("supplies-ge.properties");
    private static final WorldPoint GE=new WorldPoint(3164,3487,0);
    private QuestGeBuyer buyer;
    private int buyItem,buyQuantity,buyCap,buyBefore;
    private boolean acquiring,prepReady;
    private long buyCompleteAt;
    private enum Proof { VARP, DIALOGUE, ITEM_GAIN, ITEM_LOSS, CROSS, BANK_OPEN, BANK_CLOSED, EAT }
    private static final class Frame {
        String game="NO_CLIENT", quest="UNKNOWN", dialogue="";
        WorldPoint pos; int varp=-1, mining, hp, hpMax, loginIndex=-1;
        long accountHash; boolean inventoryReady, bankOpen, inDialogue, hasContinue, vyvinInRoom;
        int weaponId=-1, armourCount, bankPie=-1, bankBars=-1, bankCoins=-1;
        String equipment="";
        final Map<Integer,Integer> items=new HashMap<>();
        final java.util.List<String> options=new java.util.ArrayList<>();
        int count(int id){return items.getOrDefault(id,0);}
        int food(){int n=0;for(int id:FOOD)n+=count(id);return n;}
        int pick(){for(int id:PICKS)if(count(id)>0 || weaponId==id)return id;return -1;}
        boolean cave(){return pos!=null && pos.getPlane()==0 && pos.getY()>=9538 && pos.getY()<=9602;}
        boolean loggedIn(){return "LOGGED_IN".equals(game) && pos!=null;}
    }
    private static final class Pending {
        final String key; final Proof proof; final Frame before; final int item, wanted;
        final long at=System.currentTimeMillis();
        Pending(String key,Proof proof,Frame before,int item,int wanted){
            this.key=key;this.proof=proof;this.before=before;this.item=item;this.wanted=wanted;
        }
    }
    private static final class Route {
        final String key; final WorldPoint target; final int radius;
        final long at=System.currentTimeMillis();
        volatile boolean done; Thread worker; WorldPoint last; long lastMoved=at;
        Route(String key,WorldPoint target,int radius,WorldPoint start){
            this.key=key;this.target=target;this.radius=radius;this.last=start;
        }
    }
    private KnightsSwordConfig config; private BooleanSupplier ownsInput;
    private volatile boolean stopped; private boolean previousTeleportSetting;
    private String stage="START",error="",classHash="UNKNOWN";
    private Pending pending; private Route route; private Thread drainingRoute;
    private long drainingAt; private final Map<String,Integer> failures=new HashMap<>();
    private long accountHash,loginAt,logoutAt,dialogueUnknownAt,emergencyAt;
    private boolean logoutIssued,finished,retreating,emergencyEating;
    private int emergencyFoodId,emergencyFoodBefore,emergencyFailures;
    private int loginAttempts,selectedWorld,welcomeAttempts; private long welcomeAt;
    private Map<String,Object> reloadState;

    public int runtimeBuild(){return BUILD_NUMBER;}
    public void restoreReloadState(Map<String,Object> state){reloadState=state;}
    public synchronized Map<String,Object> quiesceForReload(){
        if(pending!=null || route!=null || drainingRoute!=null || emergencyEating
            || retreating || Files.isRegularFile(ACTION))
            throw new IllegalStateException("Action/route unsettled; retry this hash after boundary");
        Map<String,Object> state=new HashMap<>();state.put("accountHash",accountHash);
        state.put("finished",finished);state.put("logoutIssued",logoutIssued);
        shutdown();return state;
    }
    public boolean run(KnightsSwordConfig cfg,BooleanSupplier owner){
        if(isRunning())return true;
        config=cfg;ownsInput=owner;stopped=false;stage="START";error="";
        accountHash=reloadState!=null && reloadState.get("accountHash") instanceof Number
            ?((Number)reloadState.get("accountHash")).longValue():0;
        finished=reloadState!=null && Boolean.TRUE.equals(reloadState.get("finished"));
        logoutIssued=reloadState!=null && Boolean.TRUE.equals(reloadState.get("logoutIssued"));
        retreating=false;emergencyEating=false;emergencyFoodId=0;emergencyFoodBefore=0;
        emergencyFailures=0;
        reloadState=null;previousTeleportSetting=Rs2Walker.disableTeleports;
        classHash=classSha();
        if(Files.isRegularFile(ACTION))error="Unresolved prior action journal; reconcile before arming";
        LOG.info("[KnightsSword] RUNNING_BUILD={} classSha256={}",BUILD_NUMBER,classHash);
        mainScheduledFuture=scheduledExecutorService.scheduleWithFixedDelay(this::tick,0,
            Math.max(450,Math.min(2000,cfg.tickDelay())),TimeUnit.MILLISECONDS);
        return true;
    }
    @Override public void shutdown(){
        stopped=true;cancelRoute();Rs2Walker.disableTeleports=previousTeleportSetting;
        if(mainScheduledFuture!=null)mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow();super.shutdown();
    }
    private synchronized void tick(){
        Frame f=null;
        if(stopped || Thread.currentThread().isInterrupted())return;
        try{
            f=observe();
            if(finished && !f.loggedIn()){stage="QUEST_FINISHED_LOGGED_OUT";return;}
            if(!f.loggedIn()){cancelRoute();login(f);return;}
            if(f.accountHash==0){hold("Account hash unavailable");return;}
            if(accountHash==0)accountHash=f.accountHash;
            else if(accountHash!=f.accountHash){hold("Different account in client");return;}
            if(!f.inventoryReady){stage="WAIT_INVENTORY";return;}
            String mode=mode();
            // The default is login-only. A non-default mode must be explicitly
            // bound to this PID, build and loaded class hash in CONTROL.
            if("LOGIN_ONLY".equals(mode)){
                cancelRoute();stage="PREFLIGHT_ONLY";return;
            }
            if("FINISHED".equals(f.quest)){
                if(pending!=null || Files.isRegularFile(ACTION)){
                    pending=null;Files.deleteIfExists(ACTION);
                }
                cancelRoute();finished=true;stage="QUEST_FINISHED";
                if(!logoutIssued && armed()){
                    logoutIssued=true;logoutAt=System.currentTimeMillis();Rs2Player.logout();
                }else if(logoutIssued && System.currentTimeMillis()-logoutAt>20000)
                    hold("Logout not observed after native completion");
                return;
            }
            if(!error.isEmpty()){stage="HOLD";return;}
            if(!armed() || ownsInput==null || !ownsInput.getAsBoolean()
                || Microbot.pauseAllScripts.get() || InputArbiter.isHuman()){
                cancelRoute();Rs2Walker.disableTeleports=previousTeleportSetting;
                stage="WAIT_ARMED_OR_INPUT_OWNER";return;
            }
            Rs2Walker.disableTeleports=true;
            if(drainingRoute!=null){
                if(drainingRoute.isAlive()){
                    stage="WAIT_ROUTE_STOP";
                    if(System.currentTimeMillis()-drainingAt>30000)
                        hold("Route worker did not settle");
                    return;
                }
                drainingRoute=null;
            }
            WelcomeScreenEvent welcome=new WelcomeScreenEvent();
            if(welcome.validate()){
                stage="WELCOME";
                if(welcomeAttempts++==0){welcomeAt=System.currentTimeMillis();welcome.execute();}
                else if(System.currentTimeMillis()-welcomeAt>12000)hold("Welcome overlay persisted");
                return;
            }
            welcomeAttempts=0;
            if(f.hp<=0){hold("Death/respawn: stale transaction must be reconciled");return;}
            if(emergencyEating){
                if(f.count(emergencyFoodId)<emergencyFoodBefore){
                    emergencyEating=false;retreating=true;
                    emergencyFailures=0;
                    supersedePending("emergency-food-proved");
                }else if(System.currentTimeMillis()-emergencyAt>5000){
                    emergencyEating=false;retreating=true;
                    emergencyFailures++;
                    supersedePending("emergency-food-unproved");
                }else{stage="VERIFY_EMERGENCY_FOOD";return;}
            }
            if(f.cave() && f.hp<=Math.min(12,f.hpMax/2)){
                cancelRoute();retreating=true;
                if(pending!=null)supersedePending("low-hp-retreat");
                if(f.food()>0 && emergencyFailures<2){emergencyEat(f);return;}
            }
            if(retreating){
                if(f.cave()){escape(f);return;}
                retreating=false;stage="SURFACE_REPREP";return;
            }
            if(pending!=null){verify(f);return;}
            if(acquiring || Files.isRegularFile(BUY_STATE)){acquireSupplies(f);return;}
            if("BANK_INSPECT".equals(mode) || "BANK_PREPARE".equals(mode)){
                bankInspect(f,"BANK_PREPARE".equals(mode));return;
            }
            if(!"QUEST".equals(mode)){
                cancelRoute();stage="WAIT_VALID_MODE";return;
            }
            if(route!=null && !route.done){walk(f,route.key,route.target,route.radius);return;}
            if(f.mining<10){stage="NEED_MINING_10";return;}
            if(!supplies(f)){prepare(f);return;}
            if(f.bankOpen){
                issue("bank:close",Proof.BANK_CLOSED,f,0,0,()->Rs2Bank.closeBank());return;
            }
            if(f.inDialogue || f.hasContinue || !f.options.isEmpty()){dialogue(f);return;}
            switch(f.varp){
                case 0:talk(f,"squire:start",NpcID.SQUIRE,SQUIRE);break;
                case 1:talk(f,"reIdo",NpcID.RELDO_NORMAL,RELDO);break;
                case 2:talk(f,"thurgo:pie",NpcID.THURGO,THURGO);break;
                case 3:talk(f,"thurgo:again",NpcID.THURGO,THURGO);break;
                case 4:talk(f,"squire:portrait",NpcID.SQUIRE,SQUIRE);break;
                case 5:portrait(f);break;
                case 6:caveAndFinish(f);break;
                default:hold("Unknown native quest varplayer "+f.varp);
            }
        }catch(Exception ex){hold("Tick: "+ex);LOG.error("[KnightsSword] tick",ex);}
        finally{status(f);}
    }
    private Frame observe(){
        Frame f=Microbot.getClientThread().invoke(()->{
            Frame s=new Frame();Client c=Microbot.getClient();if(c==null)return s;
            GameState g=c.getGameState();s.game=g==null?"UNKNOWN":g.name();s.loginIndex=c.getLoginIndex();
            if(g!=GameState.LOGGED_IN || c.getLocalPlayer()==null)return s;
            s.pos=c.getLocalPlayer().getWorldLocation();s.accountHash=c.getAccountHash();
            s.varp=c.getVarpValue(VARP);s.mining=c.getRealSkillLevel(Skill.MINING);
            s.hp=c.getBoostedSkillLevel(Skill.HITPOINTS);s.hpMax=c.getRealSkillLevel(Skill.HITPOINTS);
            var qs=Quest.THE_KNIGHTS_SWORD.getState(c);s.quest=qs==null?"UNKNOWN":qs.name();
            ItemContainer inv=c.getItemContainer(InventoryID.INVENTORY);s.inventoryReady=inv!=null;
            if(inv!=null)for(Item i:inv.getItems())if(i!=null && i.getId()>0)
                s.items.merge(i.getId(),i.getQuantity(),Integer::sum);
            ItemContainer eq=c.getItemContainer(InventoryID.EQUIPMENT);
            if(eq!=null){
                Item[] equipped=eq.getItems();StringBuilder ids=new StringBuilder();
                for(int slot=0;slot<equipped.length;slot++){
                    Item item=equipped[slot];if(item==null || item.getId()<=0)continue;
                    if(ids.length()>0)ids.append(',');ids.append(slot).append(':').append(item.getId());
                    if(slot==EquipmentInventorySlot.WEAPON.getSlotIdx())s.weaponId=item.getId();
                    if(slot==EquipmentInventorySlot.HEAD.getSlotIdx()
                        || slot==EquipmentInventorySlot.BODY.getSlotIdx()
                        || slot==EquipmentInventorySlot.LEGS.getSlotIdx())s.armourCount++;
                }
                s.equipment=ids.toString();
            }
            return s;
        });
        if(f.loggedIn()){
            f.bankOpen=Rs2Bank.isOpen();f.inDialogue=Rs2Dialogue.isInDialogue();
            if(f.bankOpen){f.bankPie=Rs2Bank.count(PIE);f.bankBars=Rs2Bank.count(BAR);
                f.bankCoins=Rs2Bank.count(COINS);}
            f.hasContinue=Rs2Dialogue.hasContinue();
            f.dialogue=norm(f.inDialogue?Rs2Dialogue.getDialogueText():"");
            for(Widget w:Rs2Dialogue.getDialogueOptions())if(w!=null && !norm(w.getText()).isEmpty())
                f.options.add(w.getText());
            Rs2NpcModel vyvin=Microbot.getRs2NpcCache().query().withId(NpcID.SIR_VYVIN)
                .within(CUPBOARD,6).nearestOnClientThread();
            f.vyvinInRoom=vyvin!=null && vyvin.getWorldLocation()!=null
                && vyvin.getWorldLocation().getPlane()==2
                && vyvin.getWorldLocation().getX()>=2981 && vyvin.getWorldLocation().getX()<=2986
                && vyvin.getWorldLocation().getY()>=3331 && vyvin.getWorldLocation().getY()<=3336;
        }
        return f;
    }
    private boolean supplies(Frame f){
        if(f.varp<=1 || f.varp==3 || f.varp==4)return true;
        if(f.varp==2)return f.count(PIE)>0;
        if(f.varp==5)return f.count(PORTRAIT)==0 || f.count(BAR)>=2;
        if(f.varp==6){
            if(f.count(SWORD)>0)return true;
            if(f.count(ORE)>0)return f.count(BAR)>=2;
            return f.count(BAR)>=2 && f.pick()>0 && f.food()>=8
                && f.armourCount>=1;
        }
        return false;
    }
    private void prepare(Frame f){
        stage="PREPARE";
        if(f.cave()){escape(f);return;}
        if(!f.bankOpen){
            if(walk(f,"bank",FALADOR_BANK,3))return;
            issue("bank:open",Proof.BANK_OPEN,f,0,0,()->Rs2Bank.openBank());return;
        }
        int[] need={BAR,PIE};
        for(int id:need)if((id==BAR?f.varp>=5 && f.count(BAR)<2:
            f.varp==2 && f.count(PIE)<1)){
            int count=Rs2Bank.count(id);
            if(count<=0){hold("Missing required item in bank: "+id+"; use shared acquisition before arming");return;}
            issue("bank:withdraw:"+id,Proof.ITEM_GAIN,f,id,0,
                ()->id==BAR?Rs2Bank.withdrawX(id,2-f.count(BAR)):Rs2Bank.withdrawOne(id));return;
        }
        if(f.varp==6 && f.pick()<0){
            for(int id:PICKS)if(Rs2Bank.count(id)>0){
                issue("bank:pick",Proof.ITEM_GAIN,f,id,0,()->Rs2Bank.withdrawOne(id));return;
            }
            hold("No usable pickaxe in inventory/bank");return;
        }
        if(f.varp==6 && f.food()<8){
            for(int id:FOOD)if(Rs2Bank.count(id)>0){
                issue("bank:food",Proof.ITEM_GAIN,f,id,0,
                    ()->Rs2Bank.withdrawX(id,Math.min(12-f.food(),Rs2Bank.count(id))));return;
            }
            hold("Insufficient food for HP-sensitive ice cave");return;
        }
        if(f.varp==6 && f.armourCount<1)
            hold("No equipped armour for low-HP ice cave; use equipment preparation");
    }
    private void bankInspect(Frame f,boolean prepare){
        stage=prepare?"BANK_PREPARE":"BANK_INSPECT";
        if(prepare && prepReady && f.count(PIE)>=1 && f.count(BAR)>=2){stage="SUPPLIES_READY";return;}
        if(f.cave()){escape(f);return;}
        if(!f.bankOpen){
            if(walk(f,"bank:inspect",FALADOR_BANK,3))return;
            issue("bank:open",Proof.BANK_OPEN,f,0,0,()->Rs2Bank.openBank());return;
        }
        if(!prepare){stage="BANK_SNAPSHOT_READY";return;}
        for(int id:f.items.keySet())if(!retain(id)){
            issue("bank:deposit:"+id,Proof.ITEM_LOSS,f,id,0,()->Rs2Bank.depositAll(id));return;
        }
        if(f.count(PIE)<1 && f.bankPie>0){
            issue("bank:pie",Proof.ITEM_GAIN,f,PIE,0,()->Rs2Bank.withdrawOne(PIE));return;
        }
        if(f.count(BAR)<2 && f.bankBars>0){
            issue("bank:bars",Proof.ITEM_GAIN,f,BAR,0,
                ()->Rs2Bank.withdrawX(BAR,Math.min(2-f.count(BAR),f.bankBars)));return;
        }
        if(f.count(PIE)<1 || f.count(BAR)<2){
            if(f.count(COINS)<2000 && f.bankCoins>0){
                issue("bank:coins",Proof.ITEM_GAIN,f,COINS,0,
                    ()->Rs2Bank.withdrawX(COINS,Math.min(2000-f.count(COINS),f.bankCoins)));return;
            }
            acquiring=true;acquireSupplies(f);return;
        }
        if(f.pick()<0){
            for(int id:PICKS)if(Rs2Bank.count(id)>0){
                issue("bank:pick",Proof.ITEM_GAIN,f,id,0,()->Rs2Bank.withdrawOne(id));return;
            }
            stage="NEED_PICKAXE_ACQUISITION";return;
        }
        if(f.food()<8){
            for(int id:FOOD)if(Rs2Bank.count(id)>0){
                issue("bank:food",Proof.ITEM_GAIN,f,id,0,
                    ()->Rs2Bank.withdrawX(id,Math.min(12-f.food(),Rs2Bank.count(id))));return;
            }
            stage="NEED_FOOD_ACQUISITION";return;
        }
        stage="BANK_PREPARE_READY";
    }
    private void acquireSupplies(Frame f){
        stage="ACQUIRE_SUPPLIES";
        if(f.cave()){escape(f);return;}
        if(f.bankOpen){issue("bank:close-ge",Proof.BANK_CLOSED,f,0,0,()->Rs2Bank.closeBank());return;}
        if(f.inDialogue || f.hasContinue || !f.options.isEmpty()){hold("Unexpected dialogue during acquisition");return;}
        try{
            if(buyer==null && Files.isRegularFile(BUY_STATE)){
                Properties p=new Properties();try(InputStream in=Files.newInputStream(BUY_STATE)){p.load(in);}
                if(!Long.toString(f.accountHash).equals(p.getProperty("account")))throw new IllegalStateException("Buyer account mismatch");
                buyItem=Integer.parseInt(p.getProperty("item"));buyQuantity=Integer.parseInt(p.getProperty("quantity"));
                buyCap=Integer.parseInt(p.getProperty("cap"));buyBefore=Integer.parseInt(p.getProperty("before"));
                if((buyItem!=PIE && buyItem!=BAR)||buyQuantity<1||buyQuantity>2||buyCap<1||buyCap>2000)
                    throw new IllegalStateException("Invalid supply request");
                buyer=new QuestGeBuyer(buyItem,buyItem==PIE?"Redberry pie":"Iron bar",buyQuantity,buyCap,
                    "Alex-KnightSword-private-test",p.getProperty("buyer"));
                acquiring=true;
            }
            if(buyer==null){
                if(f.count(PIE)>=1 && f.count(BAR)>=2){acquiring=false;prepReady=true;stage="SUPPLIES_READY";return;}
                if(walk(f,"supplies:ge",GE,6))return;
                buyItem=f.count(PIE)<1?PIE:BAR;buyQuantity=(buyItem==PIE?1:2)-f.count(buyItem);
                buyBefore=f.count(buyItem);buyCap=Math.min(2000,f.count(COINS)-500);
                int quote=QuestGeBuyer.previewPrice(buyItem,"Alex-KnightSword-private-test");
                if(quote<=0 || (long)quote*buyQuantity>buyCap){hold("Supply quote exceeds available budget/reserve");return;}
                boolean empty=Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>)()->{
                    var offers=Microbot.getClient().getGrandExchangeOffers();if(offers==null)return false;
                    for(var offer:offers)if(offer!=null && offer.getState()!=net.runelite.api.GrandExchangeOfferState.EMPTY)return false;
                    return true;
                });
                if(!empty){hold("Preserving pre-existing GE offers");return;}
                buyer=new QuestGeBuyer(buyItem,buyItem==PIE?"Redberry pie":"Iron bar",buyQuantity,buyCap,
                    "Alex-KnightSword-private-test",null);
                persistBuyer(buyer.checkpoint());
            }
            if(walk(f,"supplies:ge",GE,6))return;
            QuestGeBuyer.Result result=buyer.tick(this::persistBuyer);stage="GE_SUPPLY_"+result.phase;
            switch(result.outcome){
                case WORKING:return;
                case COMPLETE:
                    if(f.count(buyItem)>=buyBefore+buyQuantity){
                        LOG.info("[KnightsSword] GE_SUPPLY_PROVED item={} count={} spent={}",buyItem,f.count(buyItem),result.actualSpent);
                        Files.deleteIfExists(BUY_STATE);buyer=null;buyCompleteAt=0;return;
                    }
                    if(buyCompleteAt==0)buyCompleteAt=System.currentTimeMillis();
                    if(System.currentTimeMillis()-buyCompleteAt>5000)hold("GE completion lacks inventory delta");
                    return;
                default:hold("GE supply "+result.outcome+" "+result.reason);
            }
        }catch(Exception ex){hold("Supply acquisition: "+ex);}
    }
    private void persistBuyer(String checkpoint){
        try{
            if(accountHash==0 || checkpoint==null || checkpoint.isBlank())throw new IllegalStateException("Missing buyer ownership");
            Properties p=new Properties();p.setProperty("account",Long.toString(accountHash));
            p.setProperty("item",Integer.toString(buyItem));p.setProperty("quantity",Integer.toString(buyQuantity));
            p.setProperty("cap",Integer.toString(buyCap));p.setProperty("before",Integer.toString(buyBefore));p.setProperty("buyer",checkpoint);
            Path tmp=BUY_STATE.resolveSibling("supplies-ge.tmp");
            try(OutputStream out=Files.newOutputStream(tmp)){p.store(out,"Persisted GE supply intent");}
            Files.move(tmp,BUY_STATE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        }catch(Exception ex){throw new IllegalStateException("Cannot persist buyer intent",ex);}
    }
    private static boolean retain(int id){
        if(id==COINS || id==PIE || id==BAR || id==PORTRAIT || id==ORE || id==SWORD)return true;
        for(int x:FOOD)if(x==id)return true;
        for(int x:PICKS)if(x==id)return true;
        return false;
    }
    private void portrait(Frame f){
        if(f.count(PORTRAIT)>0){talk(f,"thurgo:portrait",NpcID.THURGO,THURGO);return;}
        if(f.pos.getPlane()==0){object(f,"castle:ladder",ObjectID.FAI_FALADOR_CASTLE_LADDER_UP,LADDER0,"Climb-up",1);return;}
        if(f.pos.getPlane()==1){object(f,"castle:stair",ObjectID.FAI_FALADOR_CASTLE_STAIRS,STAIR1,"Climb-up",2);return;}
        if(f.pos.getPlane()!=2){hold("Unexpected castle floor "+f.pos);return;}
        if(f.vyvinInRoom){stage="WAIT_VYVIN_LEAVE";return;}
        Rs2TileObjectModel o=obj(ObjectID.VYVINCUPBOARDSHUT,CUPBOARD,1);
        if(o==null)o=obj(ObjectID.VYVINCUPBOARDOPEN,CUPBOARD,1);
        if(o==null){hold("Cupboard not found at exact tile");return;}
        if(walk(f,"cupboard:approach",CUPBOARD,2))return;
        Rs2TileObjectModel target=o;
        issue("cupboard:portrait",Proof.ITEM_GAIN,f,PORTRAIT,0,()->target.click("Search"));
    }
    private void caveAndFinish(Frame f){
        if(f.count(SWORD)>0){talk(f,"squire:finish",NpcID.SQUIRE,SQUIRE);return;}
        if(f.count(ORE)>0){
            if(f.cave()){escape(f);return;}
            talk(f,"thurgo:ore",NpcID.THURGO,THURGO);return;
        }
        if(!f.cave()){
            if(f.food()<8 || f.hp<Math.min(16,f.hpMax)){
                stage="CAVE_SUPPLIES_INSUFFICIENT";return;
            }
            object(f,"cave:enter",1738,TRAPDOOR,"Climb-down",0);return;
        }
        if(f.food()<3){escape(f);return;}
        if(walk(f,"cave:blurite",BLURITE,3))return;
        Rs2TileObjectModel rock=obj(ObjectID.BLURITE_ROCK_1,BLURITE,3);
        if(rock==null)rock=obj(ObjectID.BLURITE_ROCK_2,BLURITE,3);
        if(rock==null){escape(f);return;}
        Rs2TileObjectModel target=rock;
        issue("cave:mine",Proof.ITEM_GAIN,f,ORE,0,()->target.click("Mine"));
    }
    private void escape(Frame f){
        stage="CAVE_ESCAPE";
        if(!f.cave())return;
        if(f.hp<=Math.min(12,f.hpMax/2) && f.food()>0){eat(f);return;}
        if(walk(f,"cave:exit-approach",CAVE_LADDER,2))return;
        Rs2TileObjectModel ladder=obj(17385,CAVE_LADDER,1);
        if(ladder==null){hold("Cave exit ladder 17385 absent at exact tile; no blind path");return;}
        issue("cave:exit",Proof.CROSS,f,0,0,()->ladder.click("Climb-up"));
    }
    private void eat(Frame f){
        for(int id:FOOD)if(f.count(id)>0){
            issue("eat:"+id,Proof.EAT,f,id,0,()->Rs2Inventory.interact(id,"Eat"));return;
        }
        escape(f);
    }
    private void emergencyEat(Frame f){
        for(int id:FOOD)if(f.count(id)>0){
            emergencyEating=true;emergencyFoodId=id;emergencyFoodBefore=f.count(id);
            emergencyAt=System.currentTimeMillis();
            boolean accepted=Rs2Inventory.interact(id,"Eat");
            LOG.warn("[KnightsSword] EMERGENCY_EAT accepted={} pending={} hp={}",
                accepted,pending==null?"none":pending.key,f.hp);
            if(!accepted){emergencyEating=false;emergencyFailures++;
                stage="EMERGENCY_EAT_REJECTED";}
            return;
        }
    }
    private void supersedePending(String reason){
        if(pending==null)return;
        try{
            if(Files.isRegularFile(ACTION))
                Files.move(ACTION,ACTION.resolveSibling("superseded-"+System.currentTimeMillis()+".properties"));
        }catch(Exception ex){hold("Could not preserve pending action on "+reason+": "+ex);return;}
        LOG.warn("[KnightsSword] pending {} superseded by {} (not proved)",pending.key,reason);
        pending=null;
    }
    private void dialogue(Frame f){
        stage="DIALOGUE";
        String[] allowed={"And how is life as a squire?","I can make a new sword if you like...",
            "So would these dwarves make another one?","Ok, I'll give it a go.","Yes.",
            "What do you know about the Imcando dwarves?","Would you like a redberry pie?",
            "Can you make a special sword for me?","About that sword...",
            "Can you make that replacement sword now?"};
        for(String expected:allowed)for(String option:f.options)if(norm(option).equals(norm(expected))){
            issue("dialogue:"+expected,Proof.DIALOGUE,f,0,0,()->Rs2Dialogue.clickOption(option));return;
        }
        if(f.hasContinue){issue("dialogue:continue",Proof.DIALOGUE,f,0,0,
            ()->{Rs2Dialogue.clickContinue();return true;});return;}
        if(dialogueUnknownAt==0)dialogueUnknownAt=System.currentTimeMillis();
        if(System.currentTimeMillis()-dialogueUnknownAt>10000)
            hold("Unrecognized quest dialogue "+f.options+" / "+f.dialogue);
        else stage="WAIT_DIALOGUE_OPTIONS";
    }
    private void talk(Frame f,String key,int id,WorldPoint target){
        if(walk(f,key+":approach",target,4))return;
        Rs2NpcModel npc=Microbot.getRs2NpcCache().query().withId(id).within(target,8).nearestOnClientThread();
        if(npc==null){hold("NPC "+id+" absent near "+target);return;}
        issue(key,Proof.DIALOGUE,f,0,0,()->npc.click("Talk-to"));
    }
    private void object(Frame f,String key,int id,WorldPoint target,String action,int otherPlane){
        if(walk(f,key+":approach",target,2))return;
        Rs2TileObjectModel obj=obj(id,target,1);
        if(obj==null){hold("Object "+id+" absent at "+target);return;}
        issue(key,Proof.CROSS,f,0,otherPlane,()->obj.click(action));
    }
    private Rs2TileObjectModel obj(int id,WorldPoint at,int radius){
        return Microbot.getRs2TileObjectCache().query().withId(id).within(at,radius).nearestOnClientThread();
    }
    private void issue(String key,Proof proof,Frame f,int item,int wanted,BooleanSupplier action){
        if(pending!=null || Files.isRegularFile(ACTION)){hold("Action ownership ambiguous");return;}
        try{
            Files.createDirectories(ACTION.getParent());Properties p=new Properties();
            p.setProperty("key",key);p.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
            p.setProperty("accountHash",Long.toString(accountHash));
            p.setProperty("beforeVarp",Integer.toString(f.varp));p.setProperty("beforePos",String.valueOf(f.pos));
            p.setProperty("beforeItem",Integer.toString(f.count(item)));
            try(OutputStream out=Files.newOutputStream(ACTION)){p.store(out,"Action before dispatch");}
            boolean accepted=action.getAsBoolean();pending=new Pending(key,proof,f,item,wanted);
            LOG.info("[KnightsSword] ACTION {} accepted={} varp={} pos={}",key,accepted,f.varp,f.pos);
            if(!accepted)hold("Action dispatch rejected: "+key);
        }catch(Exception ex){hold("Action dispatch uncertain: "+key+" "+ex);}
    }
    private void verify(Frame f){
        Pending p=pending;boolean proved=false;
        if(f.loggedIn())switch(p.proof){
            case VARP:proved=f.varp>=p.wanted;break;
            case DIALOGUE:proved=f.varp!=p.before.varp || f.inDialogue!=p.before.inDialogue
                || f.hasContinue!=p.before.hasContinue || !f.dialogue.equals(p.before.dialogue)
                || !f.options.equals(p.before.options);break;
            case ITEM_GAIN:proved=f.count(p.item)>p.before.count(p.item);break;
            case ITEM_LOSS:case EAT:proved=f.count(p.item)<p.before.count(p.item);break;
            case BANK_OPEN:proved=f.bankOpen && !p.before.bankOpen;break;
            case BANK_CLOSED:proved=!f.bankOpen && p.before.bankOpen;break;
            case CROSS:proved=p.key.equals("cave:enter")?f.cave() && !p.before.cave():
                p.key.equals("cave:exit")?!f.cave() && p.before.cave():
                f.pos.getPlane()==p.wanted && f.pos.getPlane()!=p.before.pos.getPlane();break;
        }
        if(proved){pending=null;try{Files.deleteIfExists(ACTION);}catch(Exception ex){hold("Cannot clear proved journal: "+ex);}
            LOG.info("[KnightsSword] PROVED {} varp={} pos={}",p.key,f.varp,f.pos);return;}
        if(System.currentTimeMillis()-p.at>12000){
            pending=null;hold("Unproved action "+p.key+" at "+f.pos+" varp="+f.varp);
        }else stage="VERIFY_"+p.key;
    }
    private boolean walk(Frame f,String key,WorldPoint target,int radius){
        if(near(f.pos,target,radius)){
            if(route!=null){cancelRoute();return true;}
            return false;
        }
        if(route!=null && (!route.key.equals(key)||!route.target.equals(target))){cancelRoute();return true;}
        if(route==null){
            Route r=new Route(key,target,radius,f.pos);route=r;
            r.worker=new Thread(()->{try{Rs2Walker.walkWithStateUntil(r.target,r.radius,
                ()->stopped || Thread.currentThread().isInterrupted() || System.currentTimeMillis()-r.at>90000);
            }finally{r.done=true;}},"KnightSword-route");
            r.worker.setDaemon(true);r.worker.start();
        }
        stage="WALK_"+key;
        if(!f.pos.equals(route.last)){route.last=f.pos;route.lastMoved=System.currentTimeMillis();}
        if(route.done || System.currentTimeMillis()-route.at>100000
            || System.currentTimeMillis()-route.lastMoved>15000){
            cancelRoute();int n=failures.merge("walk:"+key,1,Integer::sum);
            if(n>=2)hold("Route failed twice: "+key+" from "+f.pos+" to "+target);
            else Rs2Walker.recalculatePath();
        }
        return true;
    }
    private void cancelRoute(){
        Route r=route;route=null;if(r==null)return;
        if(r.worker!=null && r.worker.isAlive()){
            drainingRoute=r.worker;drainingAt=System.currentTimeMillis();r.worker.interrupt();
        }
        Rs2Walker.clearWalkingRoute("knightssword:cancel");
    }
    private void login(Frame f){
        if(!armed() || ownsInput==null || !ownsInput.getAsBoolean()
            || Microbot.pauseAllScripts.get() || InputArbiter.isHuman()){
            stage="WAIT_LOGIN_ARMED";return;
        }
        if(!"LOGIN_SCREEN".equals(f.game) || (f.loginIndex!=10 && f.loginIndex!=34)){
            stage="WAIT_LOGIN_SCENE_"+f.loginIndex;return;
        }
        if(selectedWorld==0){selectedWorld=LoginManager.getRandomWorld(false);
            if(selectedWorld<=0 || LoginManager.isMemberWorld(selectedWorld)){
                hold("No verified free world");return;
            }
        }
        if(loginAttempts++==0){loginAt=System.currentTimeMillis();stage="VERIFY_NATIVE_LOGIN";
            if(!LoginManager.login(selectedWorld))hold("Native login rejected");
        }else if(System.currentTimeMillis()-loginAt>20000)
            hold("Native login did not advance; no repeated click");
    }
    private boolean armed(){
        if(config!=null && config.allowActions()
            && config.approvedPid()==ProcessHandle.current().pid()
            && config.approvedBuild()==BUILD_NUMBER
            && classHash.matches("[a-f0-9]{64}")
            && classHash.equalsIgnoreCase(config.approvedSha256()))return true;
        try(InputStream in=Files.newInputStream(CONTROL)){
            Properties p=new Properties();p.load(in);
            return "true".equals(p.getProperty("enableActions"))
                && Long.toString(ProcessHandle.current().pid()).equals(p.getProperty("expectedPid"))
                && Integer.toString(BUILD_NUMBER).equals(p.getProperty("expectedBuild"))
                && classHash.matches("[a-f0-9]{64}")
                && classHash.equalsIgnoreCase(p.getProperty("expectedClassSha",""));
        }catch(Exception ex){return false;}
    }
    private String mode(){
        try(InputStream in=Files.newInputStream(CONTROL)){
            Properties p=new Properties();p.load(in);
            if(!"true".equals(p.getProperty("enableActions"))
                || !Long.toString(ProcessHandle.current().pid()).equals(p.getProperty("expectedPid"))
                || !Integer.toString(BUILD_NUMBER).equals(p.getProperty("expectedBuild"))
                || !classHash.matches("[a-f0-9]{64}")
                || !classHash.equalsIgnoreCase(p.getProperty("expectedClassSha","")))
                return "LOGIN_ONLY";
            String value=p.getProperty("mode","LOGIN_ONLY").trim().toUpperCase();
            return value.equals("BANK_INSPECT") || value.equals("BANK_PREPARE")
                || value.equals("QUEST")?value:"LOGIN_ONLY";
        }catch(Exception ex){return "LOGIN_ONLY";}
    }
    private void status(Frame f){
        try{
            Files.createDirectories(STATUS.getParent());Properties p=new Properties();
            p.setProperty("timestamp",Long.toString(System.currentTimeMillis()));
            p.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
            p.setProperty("build",Integer.toString(BUILD_NUMBER));p.setProperty("classSha256",classHash);
            p.setProperty("sha256",classHash);
            p.setProperty("stage",stage);p.setProperty("error",error);
            p.setProperty("armed",Boolean.toString(armed()));
            p.setProperty("actionsArmed",Boolean.toString(armed()));
            p.setProperty("preflightOnly",Boolean.toString("LOGIN_ONLY".equals(mode())));
            p.setProperty("mode",mode());
            if(f!=null){p.setProperty("game",f.game);p.setProperty("quest",f.quest);
                p.setProperty("gameState",f.game);p.setProperty("questState",f.quest);
                p.setProperty("questVarp122",Integer.toString(f.varp));
                p.setProperty("varp",Integer.toString(f.varp));p.setProperty("pos",String.valueOf(f.pos));
                p.setProperty("mining",Integer.toString(f.mining));p.setProperty("hp",f.hp+"/"+f.hpMax);
                p.setProperty("miningLevel",Integer.toString(f.mining));
                p.setProperty("foodCount",Integer.toString(f.food()));
                p.setProperty("coins",Integer.toString(f.count(COINS)));
                p.setProperty("bankPie",Integer.toString(f.bankPie));
                p.setProperty("bankBars",Integer.toString(f.bankBars));
                p.setProperty("bankCoins",Integer.toString(f.bankCoins));
                p.setProperty("pickaxeId",Integer.toString(f.pick()));
                p.setProperty("armourCount",Integer.toString(f.armourCount));
                p.setProperty("equipment",f.equipment);
                p.setProperty("accountHash",Long.toString(f.accountHash));
                p.setProperty("food",Integer.toString(f.food()));p.setProperty("bars",Integer.toString(f.count(BAR)));
                p.setProperty("pie",Integer.toString(f.count(PIE)));p.setProperty("ore",Integer.toString(f.count(ORE)));
                p.setProperty("sword",Integer.toString(f.count(SWORD)));}
            Path tmp=STATUS.resolveSibling("status.tmp");
            try(OutputStream out=Files.newOutputStream(tmp)){p.store(out,"Knight Sword live candidate");}
            Files.move(tmp,STATUS,StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception ex){LOG.warn("[KnightsSword] status {}",ex.toString());}
    }
    private void hold(String reason){if(error.isEmpty())LOG.error("[KnightsSword] HOLD {}",reason);
        error=reason;stage="HOLD";cancelRoute();}
    private static boolean near(WorldPoint a,WorldPoint b,int r){
        return a!=null && b!=null && a.getPlane()==b.getPlane()
            && Math.max(Math.abs(a.getX()-b.getX()),Math.abs(a.getY()-b.getY()))<=r;
    }
    private static String norm(String s){return s==null?"":s.replaceAll("<[^>]*>","").trim().toLowerCase();}
    private static String classSha(){
        try {
            java.net.URL origin=KnightsSwordScript.class.getProtectionDomain().getCodeSource().getLocation();
            java.nio.file.Path source=java.nio.file.Paths.get(origin.toURI());
            String entry=KnightsSwordScript.class.getName().replace('.', '/')+".class";
            byte[] bytes;
            if(java.nio.file.Files.isDirectory(source)) bytes=java.nio.file.Files.readAllBytes(source.resolve(entry));
            else try(java.util.jar.JarFile jar=new java.util.jar.JarFile(source.toFile());
                     InputStream in=jar.getInputStream(jar.getJarEntry(entry))){bytes=in.readAllBytes();}
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        }catch(Exception ex){return "UNKNOWN";}
    }
}
