package net.runelite.client.plugins.microbot.blackknightsfortress;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
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
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.events.WelcomeScreenEvent;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.input.InputArbiter;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.LoginManager;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Build-only candidate. Every game action is followed by a new observation. */
public class BlackKnightsFortressScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(BlackKnightsFortressScript.class);
    public static final int BUILD_NUMBER = 8;
    private static final int VARP = 130;
    private static final int HELM = ItemID.BRONZE_MED_HELM, CHAIN = ItemID.IRON_CHAINBODY;
    private static final int CABBAGE = ItemID.CABBAGE, COINS = ItemID.COINS;
    private static final int[] FOOD = {ItemID.LOBSTER, ItemID.SALMON, ItemID.TROUT};
    private static final WorldPoint BANK = new WorldPoint(2946, 3369, 0);
    private static final WorldPoint AMIK = new WorldPoint(2959, 3339, 2);
    private static final WorldPoint CASTLE_STAIR0 = new WorldPoint(2955, 3339, 0);
    private static final WorldPoint CASTLE_STAIR1 = new WorldPoint(2961, 3339, 1);
    private static final WorldPoint FORT_ENTRY = new WorldPoint(3016, 3514, 0);
    private static final WorldPoint CABBAGE_SPAWN = new WorldPoint(3053, 3508, 0);
    private static final WorldPoint WAYNE_SHOP = new WorldPoint(2972, 3314, 0);
    private static final WorldPoint PEKSA_SHOP = new WorldPoint(3076, 3428, 0);
    private static final Path STATUS = Paths.get(System.getProperty("user.home"),
        ".runelite", "blackknightsfortress", "status.properties");
    private static final Path CONTROL = STATUS.resolveSibling("control.properties");
    private static final Path ACTION = STATUS.resolveSibling("pending-action.properties");

    private enum Proof { QUEST, DIALOGUE, MOVED, MOVED_OR_DIALOGUE, EQUIPPED, ITEM_GAINED, ITEM_LOST, BANK_OPEN, SHOP_OPEN, ATE }
    private static final class Frame {
        String game = "NO_CLIENT", quest = "UNKNOWN", dialogue = "";
        WorldPoint pos;
        int varp = -1, questPoints = -1, world, freeSlots, excessItems, hpCurrent, hpMax, combatLevel;
        int loginIndex=-1;
        long accountHash;
        double hp = -1;
        boolean inventoryLoaded, helmWorn, chainWorn, inDialogue, hasContinue, bankOpen, shopOpen;
        final Map<Integer,Integer> items = new HashMap<>();
        final java.util.ArrayList<String> options = new java.util.ArrayList<>();
        boolean loggedIn() { return "LOGGED_IN".equals(game) && pos != null; }
        int count(int id) { return items.getOrDefault(id, 0); }
        int food() { int n = 0; for (int id : FOOD) n += count(id); return n; }
        int requiredFood() { return combatLevel<15 || hpMax<20 ? 5 : 2; }
    }
    private static final class Pending {
        final String key; final Proof proof; final Frame before;
        final int item, wantedVarp, wantedPlane; final WorldPoint destination;
        final long at = System.currentTimeMillis(), timeout;
        Pending(String key, Proof proof, Frame before, int item, int wantedVarp,
                int wantedPlane, WorldPoint destination, long timeout) {
            this.key=key; this.proof=proof; this.before=before; this.item=item;
            this.wantedVarp=wantedVarp; this.wantedPlane=wantedPlane;
            this.destination=destination; this.timeout=timeout;
        }
    }
    private static final class Route {
        final String key; final WorldPoint target; final int radius;
        final long started=System.currentTimeMillis();
        WorldPoint last; long lastMove=started;
        volatile boolean done; Thread worker;
        Route(String key, WorldPoint target, int radius, WorldPoint start) {
            this.key=key; this.target=target; this.radius=radius; this.last=start;
        }
    }
    private final Map<String,Integer> failures = new HashMap<>();
    private Pending pending; private Route route;
    private Route cancellingRoute;
    private Thread routeClear;
    private long routeClearAt;
    private volatile boolean stopped;
    private String stage="START", error="", classHash="UNKNOWN";
    private boolean prepDone, bankChecked, retreating, logoutIssued, completionProved;
    private boolean emergencyEatAttempted;
    private int emergencyFoodId, emergencyFoodBefore;
    private long emergencyEatAt;
    private BlackKnightsFortressConfig config;
    private BooleanSupplier ownsInput;
    private Map<String,Object> reloadState;
    private WorldPoint lastPosition;
    private long loginAt, missingAt, nextAt;
    private int loginAttempts, selectedWorld, welcomeAttempts;
    private long welcomeAt, deathAt, accountHash, logoutAt;
    private String missingKey="";

    public int runtimeBuild() { return BUILD_NUMBER; }
    public void restoreReloadState(Map<String,Object> state) { reloadState=state; }
    public synchronized Map<String,Object> quiesceForReload() {
        if(pending!=null || route!=null || cancellingRoute!=null || retreating
            || emergencyEatAttempted
            || Files.isRegularFile(ACTION)) {
            throw new IllegalStateException("Action, route, or retreat in progress; retry reload at a safe boundary");
        }
        Map<String,Object> state=new HashMap<>();
        state.put("bankChecked",bankChecked); state.put("prepDone",prepDone);
        state.put("completionProved",completionProved);
        state.put("logoutIssued",logoutIssued);
        state.put("lastPosition",lastPosition);
        state.put("accountHash",accountHash);
        state.put("logoutAt",logoutAt);
        shutdown(); return state;
    }
    public boolean run(BlackKnightsFortressConfig config, BooleanSupplier ownsInput) {
        if (isRunning()) return true;
        this.config=config; this.ownsInput=ownsInput;
        stopped=false;
        prepDone=reloadState!=null && Boolean.TRUE.equals(reloadState.get("prepDone"));
        bankChecked=reloadState!=null && Boolean.TRUE.equals(reloadState.get("bankChecked"));
        completionProved=reloadState!=null && Boolean.TRUE.equals(reloadState.get("completionProved"));
        logoutIssued=reloadState!=null && Boolean.TRUE.equals(reloadState.get("logoutIssued"));
        lastPosition=reloadState!=null && reloadState.get("lastPosition") instanceof WorldPoint
            ? (WorldPoint)reloadState.get("lastPosition") : null;
        accountHash=reloadState!=null && reloadState.get("accountHash") instanceof Number
            ? ((Number)reloadState.get("accountHash")).longValue() : 0;
        logoutAt=reloadState!=null && reloadState.get("logoutAt") instanceof Number
            ? ((Number)reloadState.get("logoutAt")).longValue() : 0;
        retreating=false; emergencyEatAttempted=false; reloadState=null;
        stage="START"; error=""; pending=null; route=null; failures.clear();
        loginAt=missingAt=nextAt=deathAt=0; missingKey=""; classHash=classSha();
        loginAttempts=selectedWorld=welcomeAttempts=0;
        if(Files.isRegularFile(ACTION)) error="Unresolved pre-dispatch action journal; inspect live state before retry";
        log.info("[BlackKnightsFortress] RUNNING_BUILD={} classSha256={}", BUILD_NUMBER, classHash);
        int delay=Math.max(450,Math.min(2000,config.tickDelay()));
        mainScheduledFuture=scheduledExecutorService.scheduleWithFixedDelay(this::tick,0,delay,TimeUnit.MILLISECONDS);
        return true;
    }
    @Override public void shutdown() {
        stopped=true; cancelRoute();
        if (mainScheduledFuture!=null) mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow(); super.shutdown();
        try { Files.deleteIfExists(STATUS); } catch(Exception ex) { log.warn("status cleanup",ex); }
    }
    private synchronized void tick() {
        Frame f=null;
        if (stopped || Thread.currentThread().isInterrupted()) return;
        try {
            f=observe();
            if (completionProved && !f.loggedIn()) {stage="QUEST_FINISHED_LOGGED_OUT";return;}
            if (!f.loggedIn()) {cancelRoute();loginTick(f);return;}
            loginAttempts=0; selectedWorld=0;
            WelcomeScreenEvent welcome=new WelcomeScreenEvent();
            if(welcome.validate()) {
                if(!armed() || ownsInput==null || !ownsInput.getAsBoolean()
                    || Microbot.pauseAllScripts.get() || InputArbiter.isHuman()) {
                    stage="WAIT_WELCOME_ARMED";return;
                }
                stage="VERIFY_WELCOME_DISMISS";
                if(welcomeAttempts++==0){welcomeAt=System.currentTimeMillis();welcome.execute();}
                else if(System.currentTimeMillis()-welcomeAt>12000)hold("Welcome overlay persisted");
                return;
            }
            welcomeAttempts=0;
            if(cancellingRoute!=null) {
                if((cancellingRoute.worker!=null && cancellingRoute.worker.isAlive())
                    || (routeClear!=null && routeClear.isAlive())) {
                    stage="WAIT_ROUTE_STOP";
                    if(System.currentTimeMillis()-routeClearAt>60000) hold("Route did not stop in 60 seconds");
                    return;
                }
                cancellingRoute=null; routeClear=null;
            }
            long now=System.currentTimeMillis();
            if (loginAt==0) loginAt=now;
            if (!f.inventoryLoaded || now-loginAt<3000) { stage="WAIT_INVENTORY"; return; }
            if(f.accountHash==0) {hold("Account identity unavailable; cannot bind quest actions");return;}
            if(accountHash==0) accountHash=f.accountHash;
            else if(accountHash!=f.accountHash) {hold("Different account after login/reload");return;}
            if(completionProved && !"FINISHED".equals(f.quest)) {
                hold("Saved quest completion disagrees with live quest state");return;
            }
            if(f.hpCurrent<=0) {
                if(deathAt==0)deathAt=now;
                if(now-deathAt>30000)hold("Death/respawn not resolved after 30 seconds at "+f.pos);
                else stage="WAIT_RESPAWN";
                return;
            }
            deathAt=0;
            if(lastPosition!=null && lastPosition.getY()>=3480 && f.pos.getY()<3400
                && f.varp<=2) {
                if(pending!=null) {hold("Displacement with unverified action: "+pending.key);return;}
                cancelRoute();prepDone=false;bankChecked=false;retreating=false;
                log.warn("[BlackKnightsFortress] SAFE_REPREP after displacement {} -> {}",lastPosition,f.pos);
            }
            lastPosition=f.pos;
            if (f.quest.equals("FINISHED")) {
                cancelRoute(); pending=null; completionProved=true; stage="QUEST_FINISHED";
                clearActionJournal();
                if (armed() && !logoutIssued && f.pos.getY()<3480 && !f.inDialogue) {
                    logoutIssued=true; logoutAt=now; Rs2Player.logout();
                    log.info("[BlackKnightsFortress] FINISHED logout requested");
                }
                if(logoutIssued && now-logoutAt>10000) hold("Finished logout not proved by login screen");
                return;
            }
            if (!error.isEmpty()) { cancelRoute(); stage="HOLD"; return; }
            if (f.quest.equals("UNKNOWN") || f.varp<0) { hold("Unknown quest state/varp"); return; }
            if (f.varp==0 && f.questPoints>=0 && f.questPoints<12) {
                hold("Quest-point preflight below 12: varplayer101="+f.questPoints); return;
            }
            if (!armed()) { cancelRoute(); pending=null; stage="PREFLIGHT_ACTIONS_DISABLED"; return; }
            if (ownsInput==null || !ownsInput.getAsBoolean()) {
                cancelRoute(); pending=null; stage="WAIT_EXCLUSIVE"; return;
            }
            if (Microbot.pauseAllScripts.get() || InputArbiter.isHuman()) {
                cancelRoute(); stage="WAIT_INPUT"; return;
            }
            if (Microbot.getBlockingEventManager().shouldBlockAndProcess()) {
                cancelRoute(); stage="WAIT_BLOCKING_EVENT"; return;
            }
            if (prepDone && f.varp<=2 && (!f.helmWorn || !f.chainWorn || f.count(CABBAGE)==0)) {
                prepDone=false; bankChecked=false;
            }
            if(emergencyEatAttempted) {
                if(f.count(emergencyFoodId)<emergencyFoodBefore) {
                    emergencyEatAttempted=false;
                    log.info("[BlackKnightsFortress] EMERGENCY_EAT_PROVED item={}",emergencyFoodId);
                } else if(now-emergencyEatAt>6000) {
                    hold("Emergency eat unproved while prior action unresolved");return;
                } else if(pending==null) {stage="VERIFY_EMERGENCY_EAT";return;}
            }
            if(pending!=null) {
                if(f.hpCurrent<=eatThreshold(f)) {
                    if(f.food()==0){hold("Low HP/no food during unresolved "+pending.key);return;}
                    if(!emergencyEatAttempted) emergencyEat(f);
                }
                if(error.isEmpty())verify(f);
                return;
            }
            if (f.hpCurrent>0 && f.hpCurrent<=eatThreshold(f) && !retreating) {
                if (f.food()>0) { cancelRoute(); eat(f); return; }
                retreating=true; log.error("[BlackKnightsFortress] RETREAT health={} no food", f.hp);
            }
            if (retreating) { retreat(f); return; }
            if (f.inDialogue || f.hasContinue || !f.options.isEmpty()) { cancelRoute(); dialogue(f); return; }
            if (route!=null) { walk(f,route.key,route.target,route.radius); return; }
            if (now<nextAt) { stage="WAIT_PACE"; return; }
            if (f.varp>3) { stage="VERIFY_FINISH"; return; }
            if (!prepDone && f.varp<=2) { prepare(f); return; }
            switch(f.varp) {
                case 0: startQuest(f); break;
                case 1: infiltrate(f); break;
                case 2: sabotage(f); break;
                case 3: returnAmik(f); break;
                default: hold("Unmapped quest varp "+f.varp);
            }
        } catch(Exception ex) {
            if (!Thread.currentThread().isInterrupted()) {
                hold("Tick exception: "+ex.getClass().getSimpleName()+": "+ex.getMessage());
                log.error("[BlackKnightsFortress] tick failed",ex);
            }
        } finally { writeStatus(f); }
    }
    private Frame observe() {
        Frame f=Microbot.getClientThread().invoke(() -> {
            Frame s=new Frame(); Client c=Microbot.getClient(); if(c==null) return s;
            GameState gs=c.getGameState(); s.game=gs==null?"UNKNOWN":gs.name(); s.world=c.getWorld();
            s.loginIndex=c.getLoginIndex();
            if(gs!=GameState.LOGGED_IN || c.getLocalPlayer()==null) return s;
            s.accountHash=c.getAccountHash();
            s.pos=c.getLocalPlayer().getWorldLocation(); s.varp=c.getVarpValue(VARP);
            s.hpCurrent=c.getBoostedSkillLevel(Skill.HITPOINTS);
            s.hpMax=c.getRealSkillLevel(Skill.HITPOINTS);
            s.questPoints=c.getVarpValue(VarPlayerID.QP);
            QuestState qs=Quest.BLACK_KNIGHTS_FORTRESS.getState(c);
            s.quest=qs==null?"UNKNOWN":qs.name();
            ItemContainer inv=c.getItemContainer(InventoryID.INVENTORY);
            s.inventoryLoaded=inv!=null;
            if(inv!=null) { int used=0; for(Item item:inv.getItems()) {
                if(item==null || item.getId()<0) continue;
                used++; s.items.merge(item.getId(),item.getQuantity(),Integer::sum);
                int id=item.getId();
                if(id!=HELM && id!=CHAIN && id!=CABBAGE && id!=COINS
                    && id!=FOOD[0] && id!=FOOD[1] && id!=FOOD[2]) s.excessItems++;
            } s.freeSlots=Math.max(0,28-used); }
            ItemContainer eq=c.getItemContainer(InventoryID.EQUIPMENT);
            if(eq!=null) for(Item item:eq.getItems()) if(item!=null) {
                if(item.getId()==HELM) s.helmWorn=true;
                if(item.getId()==CHAIN) s.chainWorn=true;
            }
            return s;
        });
        if(f.loggedIn()) {
            f.hp=Rs2Player.getHealthPercentage(); f.bankOpen=Rs2Bank.isOpen(); f.shopOpen=Rs2Shop.isOpen();
            f.combatLevel=Rs2Player.getCombatLevel();
            f.inDialogue=Rs2Dialogue.isInDialogue(); f.hasContinue=Rs2Dialogue.hasContinue();
            f.dialogue=norm(f.inDialogue?Rs2Dialogue.getDialogueText():"");
            for(Widget option:Rs2Dialogue.getDialogueOptions()) if(option!=null && !norm(option.getText()).isEmpty())
                f.options.add(option.getText());
        }
        return f;
    }
    private void loginTick(Frame f) {
        if (!armed() || ownsInput==null || !ownsInput.getAsBoolean()
            || Microbot.pauseAllScripts.get() || InputArbiter.isHuman()) {
            stage="WAIT_LOGIN_ARMED";return;
        }
        if(!"LOGIN_SCREEN".equals(f.game)) {stage="WAIT_LOGIN_SCREEN";return;}
        // A modal, unknown index, or account picker needs a fresh native scene proof.
        if(f.loginIndex!=10 && f.loginIndex!=34) {stage="WAIT_LOGIN_INDEX_"+f.loginIndex;return;}
        if(selectedWorld==0) {
            selectedWorld=LoginManager.getRandomWorld(false);
            if(selectedWorld<=0 || LoginManager.isMemberWorld(selectedWorld)) {
                hold("No verified free world for native login");return;
            }
        }
        if(loginAttempts++==0) {
            loginAt=System.currentTimeMillis();stage="VERIFY_NATIVE_LOGIN";
            if(!LoginManager.login(selectedWorld)) hold("Native login dispatch rejected");
        } else if(System.currentTimeMillis()-loginAt>20000)
            hold("Native login did not reach LOGGED_IN; no repeated login click");
    }
    private void prepare(Frame f) {
        stage="PREPARE";
        if(bankChecked && !f.bankOpen) {
            if(f.shopOpen) { sourceMissing(f); return; }
            if(f.count(HELM)>0 && !f.helmWorn) { issue("wear:helm",Proof.EQUIPPED,f,HELM,0,-1,null,7000,()->Rs2Inventory.wear(HELM)); return; }
            if(f.count(CHAIN)>0 && !f.chainWorn) { issue("wear:chain",Proof.EQUIPPED,f,CHAIN,0,-1,null,7000,()->Rs2Inventory.wear(CHAIN)); return; }
            sourceMissing(f); return;
        }
        if (!f.bankOpen) {
            if (walk(f,"PREP_BANK",BANK,5)) return;
            issue("bank:open",Proof.BANK_OPEN,f,0,0,-1,null,9000,Rs2Bank::openBank); return;
        }
        // Other quest items may be valuable or protected. Do not bulk-deposit an unknown inventory.
        if(f.freeSlots<3) {hold("Bank preparation needs three free slots; protected-item policy unavailable");return;}
        if(!f.helmWorn && f.count(HELM)==0 && Rs2Bank.count(HELM)>0) {
            issue("bank:helm",Proof.ITEM_GAINED,f,HELM,0,-1,null,9000,()->Rs2Bank.withdrawOne(HELM)); return;
        }
        if(!f.chainWorn && f.count(CHAIN)==0 && Rs2Bank.count(CHAIN)>0) {
            issue("bank:chain",Proof.ITEM_GAINED,f,CHAIN,0,-1,null,9000,()->Rs2Bank.withdrawOne(CHAIN)); return;
        }
        if(f.count(CABBAGE)==0 && Rs2Bank.count(CABBAGE)>0) {
            issue("bank:cabbage",Proof.ITEM_GAINED,f,CABBAGE,0,-1,null,9000,()->Rs2Bank.withdrawOne(CABBAGE)); return;
        }
        if(f.food()<f.requiredFood()) for(int id:FOOD) if(Rs2Bank.count(id)>0) {
            issue("bank:food-"+id,Proof.ITEM_GAINED,f,id,0,-1,null,9000,()->Rs2Bank.withdrawX(id,Math.min(5,Rs2Bank.count(id)))); return;
        }
        if(f.count(COINS)<500 && Rs2Bank.count(COINS)>0) {
            issue("bank:coins",Proof.ITEM_GAINED,f,COINS,0,-1,null,9000,
                ()->Rs2Bank.withdrawX(COINS,Math.min(1000,Rs2Bank.count(COINS)))); return;
        }
        bankChecked=true;
        issue("bank:close",Proof.BANK_OPEN,f,0,0,-1,null,6000,Rs2Bank::closeBank);
        // Once bank closes, source any missing required item. Food shortage is a safety hold.
    }
    private void sourceMissing(Frame f) {
        if(f.shopOpen) {
            if(!f.helmWorn && f.count(HELM)==0 && near(f.pos,PEKSA_SHOP,15)) {
                buyGear(f,HELM,PEKSA_SHOP,"Peksa"); return;
            }
            if(!f.chainWorn && f.count(CHAIN)==0 && near(f.pos,WAYNE_SHOP,15)) {
                buyGear(f,CHAIN,WAYNE_SHOP,"Wayne"); return;
            }
            issue("shop:close",Proof.SHOP_OPEN,f,0,0,-1,null,6000,
                ()->{Rs2Shop.closeShop(); return true;}); return;
        }
        if(f.helmWorn && f.chainWorn && f.count(CABBAGE)>0 && f.food()>=f.requiredFood()) {
            prepDone=true; log.info("[BlackKnightsFortress] PREPARED helm chain cabbage food={} hp={}",f.food(),f.hp); return;
        }
        if(!f.helmWorn && f.count(HELM)==0) { buyGear(f,HELM,PEKSA_SHOP,"Peksa"); return; }
        if(!f.chainWorn && f.count(CHAIN)==0) { buyGear(f,CHAIN,WAYNE_SHOP,"Wayne"); return; }
        if(f.count(CABBAGE)==0) {
            stage="SOURCE_MONASTERY_CABBAGE";
            if(walk(f,"CABBAGE",CABBAGE_SPAWN,5)) return;
            if(Rs2GroundItem.exists(CABBAGE,10)) {
                issue("loot:cabbage",Proof.ITEM_GAINED,f,CABBAGE,0,-1,null,9000,
                    ()->Rs2GroundItem.loot(CABBAGE,10)); return;
            }
            var plant=Rs2GameObject.getGameObject(ObjectID.CABBAGE,10);
            if(plant==null) { missing("Monastery cabbage ground item or plant",12000); return; }
            issue("pick:cabbage",Proof.ITEM_GAINED,f,CABBAGE,0,-1,null,9000,
                ()->Rs2GameObject.interact(plant,"Pick")); return;
        }
        if(f.food()<f.requiredFood()) hold("Only "+f.food()+" food; account needs "+f.requiredFood()
            +" based on combat="+f.combatLevel+" hpMax="+f.hpMax);
    }
    private void buyGear(Frame f,int item,WorldPoint area,String traderName) {
        stage="BUY_"+traderName.toUpperCase();
        if(f.count(COINS)==0) { hold("Need coins to buy "+item+"; carried=0"); return; }
        if(walk(f,"SHOP_"+traderName,area,7)) return;
        if(!f.shopOpen) {
            Rs2NpcModel npc=Microbot.getRs2NpcCache().query().withName(traderName).within(area,15).nearestOnClientThread();
            if(npc==null) { missing(traderName+" shopkeeper",12000); return; }
            issue("shop:open-"+traderName,Proof.SHOP_OPEN,f,0,0,-1,null,9000,()->npc.click("Trade")); return;
        }
        if(Rs2Shop.shopItems==null || Rs2Shop.shopItems.stream().noneMatch(i->i!=null && i.getId()==item && i.getQuantity()>0)) {
            missing(traderName+" stock item "+item,20000); return;
        }
        issue("shop:buy-"+item,Proof.ITEM_GAINED,f,item,0,-1,null,9000,()->Rs2Shop.buyItem(item,"1"));
    }
    private void startQuest(Frame f) {
        if(f.pos.getPlane()==0 && near(f.pos,CASTLE_STAIR0,15)) {
            object(f,"castle:up0",ObjectID.FAI_FALADOR_CASTLE_SPIRALSTAIRS,CASTLE_STAIR0,"Climb-up",1,null); return;
        }
        if(f.pos.getPlane()==1 && near(f.pos,CASTLE_STAIR1,25)) {
            object(f,"castle:up1",ObjectID.FAI_FALADOR_CASTLE_SPIRALSTAIRS,CASTLE_STAIR1,"Climb-up",2,null); return;
        }
        if(walk(f,"AMIK",new WorldPoint(2955,3339,f.pos.getPlane()),5)) return;
        if(f.pos.getPlane()!=2) { hold("Cannot resolve Falador castle staircase from "+f.pos); return; }
        Rs2NpcModel amik=npc(NpcID.SIR_AMIK_VARZE,AMIK,9);
        if(amik==null) { missing("Sir Amik Varze",12000); return; }
        issue("talk:amik-start",Proof.DIALOGUE,f,0,1,-1,null,9000,()->amik.click("Talk-to"));
    }
    private void infiltrate(Frame f) {
        stage="INFILTRATE";
        if(f.pos.getY()<3480) {
            if(f.pos.getPlane()==2 && near(f.pos,AMIK,20)) { object(f,"castle:down2",ObjectID.FAI_FALADOR_CASTLE_SPIRALSTAIRSTOP,new WorldPoint(2960,3339,2),"Climb-down",1,null); return; }
            if(f.pos.getPlane()==1 && near(f.pos,CASTLE_STAIR1,20)) { object(f,"castle:down1",ObjectID.FAI_FALADOR_CASTLE_SPIRALSTAIRSTOP,new WorldPoint(2955,3338,1),"Climb-down",0,null); return; }
            if(f.pos.getPlane()!=0) { hold("Unexpected castle floor "+f.pos); return; }
            if(walk(f,"FORT_ENTRY",FORT_ENTRY,3)) return;
        }
        if(!f.helmWorn || !f.chainWorn || f.count(CABBAGE)==0) { hold("Fortress gear/cabbage missing before entry"); return; }
        if(f.combatLevel<15 || f.hpMax<20) { hold("Fortress entry held for weak account: combat="
            +f.combatLevel+" hpMax="+f.hpMax+"; verify survivability plan"); return; }
        if(f.hp<75 && f.food()>0) { eat(f); return; }
        if(f.pos.getPlane()==0) {
            if(f.pos.getX()<3015 || f.pos.getX()>3030 || f.pos.getY()<3504 || f.pos.getY()>3519) {
                object(f,"fort:enter",ObjectID.BKFORTRESSDOOR1,FORT_ENTRY,"Open",0,new WorldPoint(3018,3515,0)); return;
            }
            if(f.pos.getX()<=3016 && f.pos.getY()>=3517) { object(f,"ladder:up1",ObjectID.DK_LADDER,new WorldPoint(3015,3519,0),"Climb-up",1,null); return; }
            if(f.pos.getX()<=3019 && f.pos.getY()>=3513) { object(f,"wall:secret",ObjectID.BKSECRETDOOR,new WorldPoint(3016,3517,0),"Push",0,new WorldPoint(3015,3518,0)); return; }
            if(f.pos.getX()>=3020 && f.pos.getY()<=3511) { object(f,"grill",ObjectID.WITCHGRILL,new WorldPoint(3026,3507,0),"Listen-at",0,null); return; }
        }
        if(f.pos.getPlane()==1) {
            if(f.pos.getX()<=3018 && f.pos.getY()>=3518) { object(f,"ladder:up2",ObjectID.DK_LADDER,new WorldPoint(3016,3519,1),"Climb-up",2,null); return; }
            if(f.pos.getX()<=3024 && f.pos.getY()>=3512) { object(f,"ladder:up4",ObjectID.DK_LADDER,new WorldPoint(3023,3513,1),"Climb-up",2,null); return; }
            object(f,"ladder:down6",ObjectID.DK_LADDERTOP,new WorldPoint(3021,3510,1),"Climb-down",0,null); return;
        }
        if(f.pos.getPlane()==2) {
            if(f.pos.getX()<=3020) { object(f,"ladder:down3",ObjectID.DK_LADDERTOP,new WorldPoint(3017,3516,2),"Climb-down",1,null); return; }
            object(f,"ladder:down5",ObjectID.DK_LADDERTOP,new WorldPoint(3025,3513,2),"Climb-down",1,null); return;
        }
        hold("Unmapped fortress position at varp1: "+f.pos);
    }
    private void sabotage(Frame f) {
        stage="SABOTAGE";
        if(f.count(CABBAGE)==0) { hold("Non-Draynor cabbage missing before hole"); return; }
        if(f.pos.getY()<3480) { if(walk(f,"FORT_RETURN",FORT_ENTRY,3)) return; }
        if(f.pos.getPlane()==0) {
            if(f.pos.getX()<3015 || f.pos.getX()>3030 || f.pos.getY()<3504 || f.pos.getY()>3519) {
                object(f,"fort:reenter",ObjectID.BKFORTRESSDOOR1,FORT_ENTRY,"Open",0,new WorldPoint(3018,3515,0)); return;
            }
            if(f.pos.getX()>=3020 && f.pos.getY()<=3511) { object(f,"ladder:back-up6",ObjectID.DK_LADDER,new WorldPoint(3021,3510,0),"Climb-up",1,null); return; }
            if(f.pos.getX()<=3016 && f.pos.getY()>=3517) { object(f,"wall:out",ObjectID.BKSECRETDOOR,new WorldPoint(3016,3517,0),"Push",0,new WorldPoint(3018,3515,0)); return; }
            object(f,"ladder:meeting",ObjectID.DK_MEETING_LADDER,new WorldPoint(3022,3518,0),"Climb-up",1,null); return;
        }
        if(f.pos.getPlane()==1) {
            if(f.pos.getX()>=3027 && f.pos.getY()<=3510) {
                Rs2TileObjectModel hole=obj(ObjectID.BLACKKNIGHTHOLE,new WorldPoint(3031,3507,1),6);
                if(hole==null) { missing("cabbage hole",12000); return; }
                issue("use:cabbage-hole",Proof.QUEST,f,CABBAGE,3,-1,null,15000,
                    ()->Rs2Inventory.useItemOnObject(CABBAGE,ObjectID.BLACKKNIGHTHOLE)); return;
            }
            if(f.pos.getX()>=3028 && f.pos.getY()>=3510) { object(f,"wall:storage",ObjectID.BKSECRETDOOR,new WorldPoint(3030,3510,1),"Push",1,new WorldPoint(3030,3508,1)); return; }
            if(f.pos.getX()<=3018 && f.pos.getY()>=3516) { object(f,"ladder:down1",ObjectID.DK_LADDERTOP,new WorldPoint(3015,3519,1),"Climb-down",0,null); return; }
            if(f.pos.getX()>=3022 && f.pos.getY()>=3517) { object(f,"wall:storage",ObjectID.BKSECRETDOOR,new WorldPoint(3030,3510,1),"Push",1,new WorldPoint(3030,3508,1)); return; }
            if(f.pos.getX()>=3020 && f.pos.getY()<=3511) { object(f,"ladder:back-up5",ObjectID.DK_LADDER,new WorldPoint(3025,3513,1),"Climb-up",2,null); return; }
            if(f.pos.getX()<=3024 && f.pos.getY()>=3512) { object(f,"ladder:up3",ObjectID.DK_LADDER,new WorldPoint(3017,3516,1),"Climb-up",2,null); return; }
            if(f.pos.getX()>=3025 && f.pos.getY()>=3510) { object(f,"ladder:back-up5",ObjectID.DK_LADDER,new WorldPoint(3025,3513,1),"Climb-up",2,null); return; }
        }
        if(f.pos.getPlane()==2) {
            if(f.pos.getX()<=3020) { object(f,"ladder:back-down2",ObjectID.DK_LADDERTOP,new WorldPoint(3016,3519,2),"Climb-down",1,null); return; }
            object(f,"ladder:back-down4",ObjectID.DK_LADDERTOP,new WorldPoint(3023,3513,2),"Climb-down",1,null); return;
        }
        hold("Unmapped fortress position at varp2: "+f.pos);
    }
    private void returnAmik(Frame f) {
        stage="RETURN_AMIK";
        if(f.pos.getY()>=3480) {
            if(f.pos.getPlane()==1 && f.pos.getX()>=3027 && f.pos.getY()<=3510) { object(f,"wall:leave-storage",ObjectID.BKSECRETDOOR,new WorldPoint(3030,3510,1),"Push",1,new WorldPoint(3030,3511,1)); return; }
            if(f.pos.getPlane()==1 && f.pos.getX()>=3020) { object(f,"ladder:leave-meeting",ObjectID.DK_MEETING_LADDERTOP,new WorldPoint(3022,3518,1),"Climb-down",0,null); return; }
            if(f.pos.getPlane()==0) { if(walk(f,"FORT_EXIT",new WorldPoint(3016,3513,0),2)) return;
                object(f,"fort:leave",ObjectID.BKFORTRESSDOOR1,FORT_ENTRY,"Open",0,new WorldPoint(3016,3512,0)); return; }
            hold("Cannot leave fortress at "+f.pos); return;
        }
        if(f.pos.getPlane()==0) { if(walk(f,"CASTLE_RETURN",CASTLE_STAIR0,3)) return;
            object(f,"castle:return-up0",ObjectID.FAI_FALADOR_CASTLE_SPIRALSTAIRS,CASTLE_STAIR0,"Climb-up",1,null); return; }
        if(f.pos.getPlane()==1) { object(f,"castle:return-up1",ObjectID.FAI_FALADOR_CASTLE_SPIRALSTAIRS,CASTLE_STAIR1,"Climb-up",2,null); return; }
        if(walk(f,"AMIK_FINISH",AMIK,5)) return;
        Rs2NpcModel amik=npc(NpcID.SIR_AMIK_VARZE,AMIK,9);
        if(amik==null) { missing("Sir Amik Varze for finish",12000); return; }
        issue("talk:amik-finish",Proof.DIALOGUE,f,0,4,-1,null,10000,()->amik.click("Talk-to"));
    }
    private void retreat(Frame f) {
        stage="RETREAT";
        if(f.pos.getY()<3480) { hold("Retreated from fortress; replenish food/health before resume"); return; }
        if(f.food()>0 && f.hp<60) { eat(f); return; }
        if(f.pos.getPlane()!=0) { hold("Low-health retreat requires live floor route at "+f.pos); return; }
        if(walk(f,"RETREAT_EXIT",new WorldPoint(3016,3512,0),2)) return;
        hold("At fortress exit; low supplies/health; safe stop");
    }
    private void eat(Frame f) {
        for(int id:FOOD) if(f.count(id)>0) {
            issue("eat:"+id,Proof.ATE,f,id,0,-1,null,6000,()->Rs2Inventory.interact(id,"Eat")); return;
        }
        retreating=true;
    }
    private void emergencyEat(Frame f) {
        for(int id:FOOD) if(f.count(id)>0) {
            emergencyEatAttempted=true;emergencyFoodId=id;
            emergencyFoodBefore=f.count(id);emergencyEatAt=System.currentTimeMillis();
            boolean accepted=Rs2Inventory.interact(id,"Eat");
            log.warn("[BlackKnightsFortress] EMERGENCY_EAT while prior action={} accepted={}",
                pending==null?"":pending.key,accepted);
            if(!accepted)hold("Emergency food action rejected");
            return;
        }
        hold("No emergency food during pending action");
    }
    private void dialogue(Frame f) {
        stage="DIALOGUE";
        if(!f.options.isEmpty()) {
            String[] allowed=f.varp==0 ? new String[]{"I seek a quest!","I laugh in the face of danger!","Yes."}
                : new String[]{"I don't care. I'm going in anyway."};
            for(String expected:allowed) for(String option:f.options) if(norm(option).equals(norm(expected))) {
                issue("dialogue:"+norm(expected),Proof.DIALOGUE,f,0,0,-1,null,8000,()->Rs2Dialogue.clickOption(option)); return;
            }
            missing("Unrecognized dialogue options "+f.options,12000); return;
        }
        if(f.hasContinue) { issue("dialogue:continue",Proof.DIALOGUE,f,0,0,-1,null,8000,
            ()->{Rs2Dialogue.clickContinue(); return true;}); return; }
        missing("Dialogue without known option or continue: "+f.dialogue,15000);
    }
    private void object(Frame f,String key,int id,WorldPoint at,String action,int plane,WorldPoint beyond) {
        stage=key;
        if(walk(f,key+":approach",at,2)) return;
        // Several fortress ladders share an ID. Pick the ladder at this step's
        // coordinate, rather than the nearest matching ladder to the player.
        Rs2TileObjectModel o=obj(id,at,key.startsWith("ladder:")?1:5);
        if(o==null) { missing("Object "+id+" at "+at,12000); return; }
        // The secret wall occupies a blocked tile. From its adjacent west-side
        // tile the server can accept Push even when cache reachability is false.
        if(!o.isReachable() && !("wall:secret".equals(key) && near(f.pos,at,2))) {
            java.util.ArrayList<WorldPoint> adjacent=new java.util.ArrayList<>();
            for(int dx=-1;dx<=1;dx++) for(int dy=-1;dy<=1;dy++) {
                if(dx==0 && dy==0)continue;
                adjacent.add(new WorldPoint(at.getX()+dx,at.getY()+dy,at.getPlane()));
            }
            WorldPoint approach=Rs2Walker.nearestReachable(at,adjacent);
            if(approach==null || approach.equals(f.pos)) {
                hold("Object "+id+" not reachable from current side at "+f.pos
                    +"; scene/collision rescan needed");return;
            }
            walk(f,key+":reachable-side",approach,0);return;
        }
        Proof proof="grill".equals(key)?Proof.DIALOGUE
            : key.startsWith("fort:enter")?Proof.MOVED_OR_DIALOGUE:Proof.MOVED;
        issue(key,proof,f,0,0,plane,beyond,12000,()->o.click(action));
    }
    private Rs2NpcModel npc(int id,WorldPoint at,int radius) {
        return Microbot.getRs2NpcCache().query().withId(id).within(at,radius).nearestOnClientThread();
    }
    private Rs2TileObjectModel obj(int id,WorldPoint at,int radius) {
        return Microbot.getRs2TileObjectCache().query().withId(id).within(at,radius).nearestOnClientThread();
    }
    private void issue(String key,Proof proof,Frame f,int item,int wantedVarp,int wantedPlane,
                       WorldPoint destination,long timeout,java.util.function.BooleanSupplier action) {
        if(pending!=null || Files.isRegularFile(ACTION)) {
            hold("Action ownership ambiguous before "+key);return;
        }
        try {writeActionJournal(key,proof,f,item);} catch(Exception ex) {
            hold("Could not journal action "+key+": "+ex.getMessage());return;
        }
        boolean accepted;
        try {accepted=action.getAsBoolean();} catch(Exception ex) {
            hold("Action "+key+" threw after journal: "+ex);return;
        }
        pending=new Pending(key,proof,f,item,wantedVarp,wantedPlane,destination,timeout);
        log.info("[BlackKnightsFortress] ACTION key={} accepted={} varp={} pos={} hp={}",key,accepted,f.varp,f.pos,f.hp);
        if(!accepted) hold("Action "+key+" was rejected/uncertain; no blind retry");
    }
    private void verify(Frame f) {
        Pending p=pending;
        if(proved(p,f)) {
            log.info("[BlackKnightsFortress] PROVED key={} varp={} pos={} quest={}",p.key,f.varp,f.pos,f.quest);
            pending=null; clearActionJournal(); failures.remove(p.key); missingKey=""; missingAt=0;
            nextAt=System.currentTimeMillis()+350; return;
        }
        if(System.currentTimeMillis()-p.at<p.timeout) { stage="VERIFY_"+p.key; return; }
        pending=null;
        hold("Unproved action "+p.key+" at "+f.pos+" varp="+f.varp
            +" dialogue="+f.dialogue+"; action may already have reached server");
    }
    private boolean proved(Pending p,Frame f) {
        if(!f.loggedIn()) return false;
        switch(p.proof) {
            case QUEST: return f.quest.equals("FINISHED") || f.varp>=p.wantedVarp;
            case DIALOGUE: return f.varp!=p.before.varp || !f.dialogue.equals(p.before.dialogue)
                || f.inDialogue!=p.before.inDialogue || f.hasContinue!=p.before.hasContinue;
            case MOVED_OR_DIALOGUE:
                if(f.inDialogue!=p.before.inDialogue || f.hasContinue!=p.before.hasContinue
                    || !f.options.equals(p.before.options)) return true;
                // A door may move the player directly without showing dialogue.
            case MOVED:
                if(f.varp!=p.before.varp) return true;
                if(p.wantedPlane>=0 && f.pos.getPlane()==p.wantedPlane
                    && f.pos.getPlane()!=p.before.pos.getPlane()
                    && near(f.pos,new WorldPoint(p.before.pos.getX(),p.before.pos.getY(),p.wantedPlane),5)) return true;
                return p.destination!=null && near(f.pos,p.destination,1) && !near(p.before.pos,p.destination,1);
            case EQUIPPED: return p.item==HELM?f.helmWorn:f.chainWorn;
            case ITEM_GAINED: return f.count(p.item)>p.before.count(p.item);
            case ITEM_LOST: return f.count(p.item)<p.before.count(p.item);
            case BANK_OPEN: return f.bankOpen!=p.before.bankOpen;
            case SHOP_OPEN: return f.shopOpen!=p.before.shopOpen;
            case ATE: return f.count(p.item)<p.before.count(p.item);
            default: return false;
        }
    }
    private boolean walk(Frame f,String key,WorldPoint target,int radius) {
        if(cancellingRoute!=null) { stage="WAIT_ROUTE_STOP"; return true; }
        if(near(f.pos,target,radius)) {
            if(route!=null) {cancelRoute();return true;}
            return false;
        }
        if(route!=null && (!route.key.equals(key) || !route.target.equals(target))) { cancelRoute(); return true; }
        if(route==null) {
            Route r=new Route(key,target,radius,f.pos); route=r;
            r.worker=new Thread(()->{ try { Rs2Walker.walkWithStateUntil(r.target,r.radius,
                ()->stopped || Thread.currentThread().isInterrupted() || System.currentTimeMillis()-r.started>90000);
            } finally { r.done=true; } },"BKF-route");
            r.worker.setDaemon(true); r.worker.start();
            log.info("[BlackKnightsFortress] ROUTE {} {} -> {}",key,f.pos,target);
        }
        stage="WALK_"+key;
        if(!f.pos.equals(route.last)) { route.last=f.pos; route.lastMove=System.currentTimeMillis(); }
        long now=System.currentTimeMillis();
        if(now-route.started>100000 || now-route.lastMove>15000 || route.done) {
            cancelRoute(); int n=failures.merge("walk:"+key,1,Integer::sum);
            if(n>=3) hold("Route "+key+" failed x"+n+" at "+f.pos+" target="+target);
            else Rs2Walker.recalculatePath();
        }
        return true;
    }
    private void cancelRoute() {
        Route r=route; if(r==null) return;
        route=null; cancellingRoute=r; routeClearAt=System.currentTimeMillis();
        if(r.worker!=null && r.worker.isAlive()) r.worker.interrupt();
        routeClear=new Thread(()->Rs2Walker.clearWalkingRoute("blackknightsfortress:cancel"),"BKF-clear-route");
        routeClear.setDaemon(true); routeClear.start();
    }
    private void missing(String key,long timeout) {
        if(!key.equals(missingKey)) { missingKey=key; missingAt=System.currentTimeMillis(); }
        if(System.currentTimeMillis()-missingAt>timeout) hold("Missing/unknown scene: "+key);
    }
    private void hold(String reason) {
        if(error.isEmpty()) log.error("[BlackKnightsFortress] HOLD {}",reason);
        error=reason; stage="HOLD"; cancelRoute();
    }
    private static boolean near(WorldPoint a,WorldPoint b,int r) {
        return a!=null && b!=null && a.getPlane()==b.getPlane()
            && Math.max(Math.abs(a.getX()-b.getX()),Math.abs(a.getY()-b.getY()))<=r;
    }
    private static int eatThreshold(Frame f) {
        return Math.min(f.hpMax-1,Math.max(8,(f.hpMax*3)/4));
    }
    private static String norm(String text) { return text==null?"":text.replaceAll("<[^>]*>","").trim().toLowerCase(); }
    private void writeActionJournal(String key,Proof proof,Frame before,int item) throws Exception {
        Files.createDirectories(ACTION.getParent());
        Properties p=new Properties();
        p.setProperty("key",key);p.setProperty("proof",proof.name());
        p.setProperty("build",Integer.toString(BUILD_NUMBER));
        p.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
        p.setProperty("accountHash",Long.toString(accountHash));
        p.setProperty("at",Long.toString(System.currentTimeMillis()));
        p.setProperty("beforeVarp",Integer.toString(before.varp));
        p.setProperty("beforePosition",String.valueOf(before.pos));
        p.setProperty("item",Integer.toString(item));
        p.setProperty("beforeItemCount",Integer.toString(before.count(item)));
        Path tmp=ACTION.resolveSibling("pending-action.tmp");
        try(OutputStream out=Files.newOutputStream(tmp)){p.store(out,"Pre-dispatch action journal");}
        try {Files.move(tmp,ACTION,StandardCopyOption.ATOMIC_MOVE);}
        catch(java.nio.file.AtomicMoveNotSupportedException ex){Files.move(tmp,ACTION);}
    }
    private void clearActionJournal() {
        try {Files.deleteIfExists(ACTION);}
        catch(Exception ex){hold("Proved action but could not clear journal: "+ex);}
    }
    private static String classSha() {
        String entry=BlackKnightsFortressScript.class.getName().replace('.','/')+".class";
        try {
            Path source=Paths.get(BlackKnightsFortressScript.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
            InputStream in;
            java.util.jar.JarFile jar=null;
            if(Files.isDirectory(source)) in=Files.newInputStream(source.resolve(entry));
            else {
                jar=new java.util.jar.JarFile(source.toFile());
                java.util.jar.JarEntry found=jar.getJarEntry(entry);
                if(found==null){jar.close();return "UNKNOWN";}
                in=jar.getInputStream(found);
            }
            try(InputStream bytes=in;java.util.jar.JarFile archive=jar){
            MessageDigest d=MessageDigest.getInstance("SHA-256"); byte[] b=new byte[8192]; int n;
            while((n=bytes.read(b))>0) d.update(b,0,n);
            StringBuilder s=new StringBuilder(); for(byte v:d.digest()) s.append(String.format("%02x",v&255)); return s.toString();
            }
        } catch(Exception ex) { return "UNKNOWN"; }
    }
    private void writeStatus(Frame f) {
        try {
            Files.createDirectories(STATUS.getParent()); Properties p=new Properties();
            p.setProperty("build",Integer.toString(BUILD_NUMBER));
            p.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
            p.setProperty("timestamp",Long.toString(System.currentTimeMillis()));
            p.setProperty("sha256",classHash); p.setProperty("stage",stage); p.setProperty("error",error);
            p.setProperty("questState",f==null?"UNKNOWN":f.quest);
            p.setProperty("questVarp130",f==null?"-1":Integer.toString(f.varp));
            p.setProperty("questPointsVarp101",f==null?"-1":Integer.toString(f.questPoints));
            p.setProperty("actionsArmed",Boolean.toString(armed()));
            p.setProperty("gameState",f==null?"UNKNOWN":f.game);
            p.setProperty("position",f==null?"UNKNOWN":String.valueOf(f.pos));
            p.setProperty("healthPercent",f==null?"-1":Double.toString(f.hp));
            p.setProperty("hp",f==null?"-1":f.hpCurrent+"/"+f.hpMax);
            p.setProperty("combatLevel",f==null?"-1":Integer.toString(f.combatLevel));
            p.setProperty("foodCount",f==null?"0":Integer.toString(f.food()));
            p.setProperty("pending",pending==null?"":pending.key);
            p.setProperty("pendingJournal",Boolean.toString(Files.isRegularFile(ACTION)));
            p.setProperty("accountHash",Long.toString(accountHash));
            p.setProperty("loginIndex",f==null?"-1":Integer.toString(f.loginIndex));
            p.setProperty("route",route==null?"":route.key+" -> "+route.target);
            Path tmp=STATUS.resolveSibling("status.tmp");
            try(OutputStream out=Files.newOutputStream(tmp)) { p.store(out,"Black Knights' Fortress live plugin"); }
            Files.move(tmp,STATUS,StandardCopyOption.REPLACE_EXISTING);
        } catch(Exception ex) { log.warn("[BlackKnightsFortress] status write {}",ex.toString()); }
    }
    private boolean armed() {
        boolean approvedInClient=config!=null && config.allowActions()
            && config.approvedPid()==ProcessHandle.current().pid()
            && config.approvedBuild()==BUILD_NUMBER
            && classHash.matches("[a-f0-9]{64}")
            && classHash.equalsIgnoreCase(config.approvedSha256());
        if (approvedInClient) return true;
        try (InputStream in=Files.newInputStream(CONTROL)) {
            Properties p=new Properties();p.load(in);
            return "true".equals(p.getProperty("enableActions"))
                && Long.toString(ProcessHandle.current().pid()).equals(p.getProperty("expectedPid"))
                && Integer.toString(BUILD_NUMBER).equals(p.getProperty("expectedBuild"))
                && classHash.matches("[a-f0-9]{64}")
                && classHash.equalsIgnoreCase(p.getProperty("expectedClassSha",""));
        } catch (Exception ignored) { return false; }
    }
}
