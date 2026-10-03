package net.runelite.client.plugins.microbot.questcommon.preparation;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceHub;
import net.runelite.client.plugins.microbot.questcommon.navigation.NavigationGoal;
import net.runelite.api.coords.WorldPoint;

/** One-action-per-tick preparation provider, driven by the host plugin scheduler. */
public final class PreparationBankService implements QuestServiceHub.ServicePlugin,AutoCloseable {
    /** Missing login-time containers are incomplete evidence, never an empty inventory. */
    public static final class SnapshotNotReady extends IllegalStateException {
        public SnapshotNotReady(String message){super(message);}
    }
    private static final org.slf4j.Logger LOG=
        org.slf4j.LoggerFactory.getLogger(PreparationBankService.class);
    public interface BankUi {
        PreparationPlanner.Snapshot observe();
        boolean openBank();
        boolean setWithdrawAsItem();
        boolean deposit(int id,int amount);
        boolean withdraw(int id,int amount);
        boolean closeBank();
        default net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearRecoveryController.Ui gearUi(){return null;}
        default net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearAcquisitionController.Ui
            gearAcquisitionUi(java.util.function.BooleanSupplier guardedTravel){return null;}
    }
    public interface NeedRouter {
        QuestServiceHub.Request request(PreparationGoal goal,PreparationPlanner.Snapshot observed,
            PreparationPlanner.Need need);
    }
    private final NeedRouter children;
    private final BankUi ui;
    private final Path checkpoint;
    private final FileChannel channel;
    private final FileLock lock;
    private QuestServiceHub.Lease lease;
    private PreparationGoal goal;
    private PreparationPlanner.Decision pending;
    private int beforeCarried,beforeBank;
    private long pendingAt;
    private boolean restoredHold;
    private String childProof="";
    private QuestServiceHub.Request deferredChild;
    private boolean verifyNavigationArrival;
    private long snapshotWaitAt;
    private net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearRecoveryController gear;
    private boolean gearDone;
    private net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearAcquisitionController gearAcq;
    private net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearAcquisitionController.Ui gearAcqUi;
    private int childSpent;
    private boolean gearAcqNavigationPending;
    private boolean gearAcqDone;

    public PreparationBankService(BankUi ui,Path checkpoint) throws IOException {
        this(ui,checkpoint,(goal,observed,need)->null);
    }
    public PreparationBankService(BankUi ui,Path checkpoint,NeedRouter children) throws IOException {
        this.children=Objects.requireNonNull(children);
        this.ui=Objects.requireNonNull(ui);this.checkpoint=Objects.requireNonNull(checkpoint);
        Files.createDirectories(checkpoint.toAbsolutePath().getParent());
        channel=FileChannel.open(checkpoint.resolveSibling(checkpoint.getFileName()+".lock"),
            StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        FileLock acquired=channel.tryLock();
        if(acquired==null){channel.close();throw new IOException("preparation worker already owns checkpoint");}
        lock=acquired;
        restoredHold=Files.exists(checkpoint);
    }
    @Override public QuestServiceHub.Kind kind(){return QuestServiceHub.Kind.BANKING;}
    public boolean register(){
        boolean accepted=QuestServiceHub.register(this);
        LOG.info("[QuestPreparation] providerRegistered={} pid={} hub={}",accepted,
            ProcessHandle.current().pid(),QuestServiceHub.snapshot());
        return accepted;
    }
    public boolean unregister(){return QuestServiceHub.unregister(this);}

    public synchronized void tick() {
        QuestServiceHub.Lease current=QuestServiceHub.activeFor(this);
        if(current==null)return;
        String cancellation=QuestServiceHub.cancellationReason(current);
        if(cancellation!=null){
            QuestServiceHub.finish(current,this,QuestServiceHub.Outcome.HOLD,cancellation);return;
        }
        if(!(current.request instanceof PreparationGoal g)) {
            QuestServiceHub.finish(current,this,QuestServiceHub.Outcome.HOLD,
                "BANKING request is not a PreparationGoal");return;
        }
        if(lease!=current) {
            lease=current;goal=g;
            gear=null;gearDone=false;
            gearAcq=null;gearAcqUi=null;childSpent=0;gearAcqNavigationPending=false;
            gearAcqDone=false;
            verifyNavigationArrival=false;
            snapshotWaitAt=0;
        }
        PreparationPlanner.Snapshot s;
        try {
            s=ui.observe();
            if(s!=null && s.loggedIn() && !goal.equippedAnyOf().isEmpty() && !s.equipmentObserved())
                throw new SnapshotNotReady("required equipment container unavailable");
        }
        catch(SnapshotNotReady ex){
            long now=System.currentTimeMillis();
            if(snapshotWaitAt==0){snapshotWaitAt=now;LOG.info("[QuestPreparation] WAIT_SNAPSHOT {}",ex.getMessage());}
            if(now-snapshotWaitAt>=10000){
                String reason="preparation data still unavailable after 10s: "+ex.getMessage();
                if(pending==null && !restoredHold && gear==null && deferredChild==null
                    && !Files.exists(checkpoint)){
                    // No bank action was dispatched: return control rather than retain
                    // an input lease for an observation-only failure.
                    QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.UNAVAILABLE,reason);
                    lease=null;goal=null;
                }else hold(reason);
            }
            return; // No bank input while containers are unavailable; pending action proof remains intact.
        }
        catch(Exception ex){hold("preparation snapshot failed: "+ex);return;}
        if(s==null)return;
        snapshotWaitAt=0;
        if(s.loggedIn() && !goal.accountKey().equals(s.accountKey())){
            hold("account changed during preparation");return;
        }
        if(!QuestServiceHub.owns(lease,this,s.accountKey()))return;
        if(verifyNavigationArrival){
            if(!s.loggedIn() || s.at()<=0 || s.at()>System.currentTimeMillis()
                || System.currentTimeMillis()-s.at()>3000 || (!s.bankNearby() && !s.bankOpen())){
                hold("navigation completion did not prove usable bank scene; no route replay");return;
            }
            verifyNavigationArrival=false;
        }
        if(restoredHold){
            try{restore(s);}catch(Exception ex){hold("checkpoint reconciliation failed: "+ex);}
            return;
        }
        if(pending!=null) {
            if(proved(pending,s)){
                pending=null;
                try {save("PROVED");}catch(IOException ex){hold("checkpoint proof write failed");}
                return; // proof consumes this tick
            }
            if(System.currentTimeMillis()-pendingAt>8000)
                hold("pending "+pending.action()+" unproved; no replay");
            return;
        }
        if(deferredChild!=null){
            if(!s.loggedIn() || s.inCombat() || s.bankOpen() || s.at()<=0
                || System.currentTimeMillis()-s.at()>3000){
                hold("fresh safe closed-bank frame required before child handoff");return;
            }
            if(QuestServiceHub.delegate(lease,this,deferredChild)==null){
                hold("prepared child provider unavailable");return;
            }
            LOG.info("[QuestPreparation] CHILD_DELEGATED kind={} bankClose=PROVED",deferredChild.kind());
            deferredChild=null;return;
        }
        if(gear!=null && !gearDone){
            var result=gear.tick();
            if(result.held()){hold("gear recovery: "+result.detail());return;}
            if(result.complete()){
                gearDone=true;
                LOG.info("[QuestPreparation] GEAR_PASS_PROVED; reobserve requested equipment");
            }
            return;
        }
        if(gearAcq!=null){
            if(gearAcqNavigationPending){
                var arrived=gearAcqUi.observe();
                if(!arrived.nearExchange() || arrived.gear().bankOpen()){
                    hold("GE navigation finished without fresh usable Exchange scene");return;
                }
                gearAcqNavigationPending=false;
                LOG.info("[QuestPreparation] GEAR_GE_ARRIVAL_PROVED");
            }
            var result=gearAcq.tick();
            if(result.held()){hold("gear acquisition: "+result.detail());return;}
            if(result.complete()){
                try{childSpent=Math.addExact(childSpent,gearAcq.actualSpent());
                    save("PROVED");}
                catch(Exception ex){hold("gear spend checkpoint uncertain: "+ex);return;}
                gearAcqDone=true;gearAcq=null;gearAcqUi=null;
                LOG.info("[QuestPreparation] GEAR_ACQUISITION_PROVED; reobserve original goal");
            }
            return;
        }
        // Supply preparation precedes equipment recovery, using the same lease.
        // The original goal still decides final completion after the gear pass.
        PreparationGoal planningGoal=goal;
        if(!gearDone && !goal.equippedAnyOf().isEmpty())
            planningGoal=new PreparationGoal(goal.accountKey(),goal.pid(),goal.purpose(),goal.requestId(),
                goal.inventoryMinimum(),goal.minimumHealing(),goal.minimumCoins(),goal.minimumFreeSlots(),
                Set.of(),goal.skillMinimum(),goal.protectedItems(),goal.f2pOnly(),goal.maximumRisk(),
                goal.maximumSpend(),goal.deadlineMillis(),goal.minimumFoodCount());
        PreparationPlanner.Decision d=PreparationPlanner.choose(planningGoal,s);
        if(!gearDone && !goal.equippedAnyOf().isEmpty()
            && (d.action()==PreparationPlanner.Action.COMPLETE
                || d.action()==PreparationPlanner.Action.CLOSE_BANK && "outcomes proved".equals(d.reason()))){
            var gearUi=ui.gearUi();
            if(gearUi==null){hold("requested equipment recovery adapter unavailable");return;}
            try{
                Map<Integer,Integer> retain=new HashMap<>(goal.inventoryMinimum());
                if(goal.minimumCoins()>0)retain.merge(995,goal.minimumCoins(),Math::max);
                for(var food:s.food())if(s.carried(food.itemId())>0)
                    retain.merge(food.itemId(),s.carried(food.itemId()),Math::max);
                for(int id:goal.protectedItems())if(s.carried(id)>0)
                    retain.merge(id,s.carried(id),Math::max);
                var gearGoal=new net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearUpgradePlanner.Goal(
                    goal.accountKey(),goal.deadlineMillis(),retain,
                    Map.of("MELEE_ATTACK",1,"STRENGTH",2,"MELEE_DEFENCE",1),0,goal.f2pOnly());
                var owned=lease;
                gear=new net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearRecoveryController(
                    restrictGearUi(gearUi,goal.equippedAnyOf()),
                    checkpoint.resolveSibling("gear-"+goalFingerprint()+".properties"),
                    ()->QuestServiceHub.owns(owned,this,owned.request.accountKey())
                        && QuestServiceHub.cancellationReason(owned)==null,gearGoal);
                LOG.info("[QuestPreparation] GEAR_PASS_STARTED bankOpen={}",s.bankOpen());
            }catch(Exception ex){hold("gear setup failed: "+ex);}
            return;
        }
        switch(d.action()) {
            case COMPLETE:
                try {Files.deleteIfExists(checkpoint);}catch(IOException ex){hold("checkpoint cleanup failed");return;}
                QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.COMPLETE,
                    "fresh account inventory/healing/coin outcomes and closed bank proved");
                lease=null;goal=null;break;
            case UNSUPPORTED:
            case NEED_MONEY:
                if(gearDone && !gearAcqDone && d.need()!=null
                    && d.need().kind()==PreparationPlanner.NeedKind.EQUIPMENT
                    && !goal.equippedAnyOf().isEmpty()){
                    try{
                        int remaining=goal.maximumSpend()-childSpent;
                        if(remaining<=0){hold("equipment purchase budget exhausted");return;}
                        gearAcqUi=ui.gearAcquisitionUi(this::delegateGearGeTravel);
                        if(gearAcqUi==null){hold("gear acquisition adapter unavailable");return;}
                        var acqGoal=new net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearAcquisitionController.Goal(
                            goal.accountKey(),goal.deadlineMillis(),remaining,goal.minimumCoins(),
                            0,1,"QuestCommon preparation gear buyer",
                            "MAGIC_DEFENCE",0,0,0,goal.equippedAnyOf());
                        var owned=lease;
                        gearAcq=new net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearAcquisitionController(
                            acqGoal,gearAcqUi,
                            ()->QuestServiceHub.owns(owned,this,owned.request.accountKey())
                                && QuestServiceHub.cancellationReason(owned)==null,
                            checkpoint.resolveSibling("gear-buy-"+goalFingerprint()+".properties"));
                        LOG.info("[QuestPreparation] GEAR_ACQUISITION_STARTED accepted={} remainingSpend={}",
                            goal.equippedAnyOf(),remaining);
                    }catch(Exception ex){hold("gear acquisition setup failed: "+ex);}
                    return;
                }
                if(d.need()!=null){
                    QuestServiceHub.Request child;
                    try{
                        PreparationGoal remainingGoal=new PreparationGoal(goal.accountKey(),goal.pid(),goal.purpose(),goal.requestId(),
                            goal.inventoryMinimum(),goal.minimumHealing(),goal.minimumCoins(),goal.minimumFreeSlots(),
                            goal.equippedAnyOf(),goal.skillMinimum(),goal.protectedItems(),goal.f2pOnly(),goal.maximumRisk(),
                            Math.max(0,goal.maximumSpend()-childSpent),goal.deadlineMillis(),goal.minimumFoodCount());
                        child=children.request(remainingGoal,s,d.need());
                    }
                    catch(Exception ex){hold("child planning failed: "+ex);return;}
                    if(child!=null){
                        if(s.bankOpen()){
                            if(child.kind()==QuestServiceHub.Kind.FOOD_RESTOCK && s.bankAudited()
                                && !s.inCombat()){
                                try{save("PROVED");}
                                catch(IOException ex){hold("checkpoint before food handoff failed");return;}
                                if(QuestServiceHub.delegate(lease,this,child)!=null){
                                    LOG.info("[QuestPreparation] CHILD_DELEGATED kind=FOOD_RESTOCK bankOpen=AUDITED");
                                    return;
                                }
                                hold("food child lease unavailable");return;
                            }
                            deferredChild=child;
                            beginAction(new PreparationPlanner.Decision(PreparationPlanner.Action.CLOSE_BANK,
                                0,0,"close bank before child handoff",null),s);
                            return;
                        }
                        try{save("PROVED");}
                        catch(IOException ex){hold("checkpoint before delegation failed");return;}
                        if(QuestServiceHub.delegate(lease,this,child)!=null){
                            LOG.info("[QuestPreparation] CHILD_DELEGATED kind={} need={}",child.kind(),d.need());
                            return;
                        }
                    }
                }
                try {Files.deleteIfExists(checkpoint);}catch(IOException ex){hold("checkpoint cleanup failed");return;}
                QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.UNAVAILABLE,
                    d.reason()+" need="+d.need());
                lease=null;goal=null;break;
            case HOLD: hold(d.reason());break;
            default:
                beginAction(d,s);
        }
    }
    private void beginAction(PreparationPlanner.Decision d,PreparationPlanner.Snapshot s){
                beforeCarried=s.carried(d.itemId());beforeBank=s.stored(d.itemId());
                pending=d;pendingAt=System.currentTimeMillis();
                try {save("PENDING");}
                catch(IOException ex){hold("checkpoint before input failed");return;}
                if(!QuestServiceHub.owns(lease,this,s.accountKey())){
                    hold("input lease lost before dispatch");return;
                }
                boolean accepted;
                try { accepted=dispatch(d); }
                catch(Exception ex){hold("input uncertain: "+ex);return;}
                if(!accepted)hold("input rejected/uncertain: "+d.action());
    }
    private boolean dispatch(PreparationPlanner.Decision d) {
        return switch(d.action()) {
            case OPEN_BANK -> ui.openBank();
            case SET_ITEM_MODE -> ui.setWithdrawAsItem();
            case DEPOSIT -> ui.deposit(d.itemId(),d.quantity());
            case WITHDRAW -> ui.withdraw(d.itemId(),d.quantity());
            case CLOSE_BANK -> ui.closeBank();
            default -> false;
        };
    }
    private boolean proved(PreparationPlanner.Decision d,PreparationPlanner.Snapshot s) {
        if(!s.loggedIn() || !goal.accountKey().equals(s.accountKey())
            || System.currentTimeMillis()-s.at()>3000)return false;
        return switch(d.action()) {
            case OPEN_BANK -> s.bankOpen() && s.bankAudited();
            case SET_ITEM_MODE -> s.bankOpen() && s.withdrawAsItem();
            case DEPOSIT -> s.bankOpen() && s.bankAudited()
                && s.carried(d.itemId())==beforeCarried-d.quantity()
                && s.stored(d.itemId())==beforeBank+d.quantity();
            case WITHDRAW -> s.bankOpen() && s.bankAudited()
                && s.carried(d.itemId())==beforeCarried+d.quantity()
                && s.stored(d.itemId())==beforeBank-d.quantity();
            case CLOSE_BANK -> !s.bankOpen();
            default -> false;
        };
    }
    private void hold(String reason) {
        if(lease!=null)QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.HOLD,reason);
    }
    private String goalFingerprint() throws Exception {
        String text=goal.accountKey()+"|"+goal.requestId()+"|"+goal.purpose()+"|"
            +new TreeMap<>(goal.inventoryMinimum())+"|"+goal.minimumHealing()+"|"
            +goal.minimumCoins()+"|"+goal.minimumFreeSlots()+"|"
            +new TreeSet<>(goal.equippedAnyOf())+"|"+new TreeMap<>(goal.skillMinimum())+"|"
            +new TreeSet<>(goal.protectedItems())+"|"+goal.f2pOnly()+"|"
            +goal.maximumRisk()+"|"+goal.maximumSpend()+"|"+goal.minimumFoodCount();
        return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
            .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    /** Reobserve before planning; never replay an input from a previous process. */
    private void restore(PreparationPlanner.Snapshot s) throws Exception {
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(checkpoint)){p.load(in);}
        if(!"PREP_BANK_2".equals(p.getProperty("schema"))
            ||!goal.accountKey().equals(p.getProperty("account")))
            throw new IOException("foreign/legacy request checkpoint");
        if(!s.loggedIn()||s.at()<=0||System.currentTimeMillis()-s.at()>3000)
            throw new IOException("fresh observation required");
        childSpent=Integer.parseInt(p.getProperty("childSpent","0"));
        if(childSpent<0 || childSpent>goal.maximumSpend())
            throw new IOException("child spend outside request budget");
        String state=p.getProperty("state","");
        if("PROVED".equals(state)){
            if(!p.getProperty("action","").isEmpty())throw new IOException("inconsistent proof record");
            restoredHold=false;
            LOG.info("[QuestPreparation] CHECKPOINT_RECONCILED priorPid={} currentPid={} priorRequest={} currentRequest={} action=PROVED",
                p.getProperty("pid"),ProcessHandle.current().pid(),
                p.getProperty("requestId"),goal.requestId());
            return;
        }
        if(!"PENDING".equals(state))throw new IOException("unknown checkpoint state");
        if(!goal.requestId().equals(p.getProperty("requestId"))
            ||!goalFingerprint().equals(p.getProperty("goalHash")))
            throw new IOException("foreign/legacy pending request checkpoint");
        var action=PreparationPlanner.Action.valueOf(p.getProperty("action"));
        int item=Integer.parseInt(p.getProperty("item")),quantity=Integer.parseInt(p.getProperty("quantity"));
        beforeCarried=Integer.parseInt(p.getProperty("beforeCarried"));
        beforeBank=Integer.parseInt(p.getProperty("beforeBank"));
        pendingAt=Long.parseLong(p.getProperty("at"));
        if(beforeCarried<0||beforeBank<0||quantity<0
            ||((action==PreparationPlanner.Action.DEPOSIT||action==PreparationPlanner.Action.WITHDRAW)
                &&(item<=0||quantity==0)))throw new IOException("invalid pending quantities");
        pending=new PreparationPlanner.Decision(action,item,quantity,"restored pending proof",null);
        if(!proved(pending,s))throw new IOException("pending action not proved by current state; no replay");
        pending=null;save("PROVED");restoredHold=false;
        LOG.info("[QuestPreparation] CHECKPOINT_RECONCILED priorPid={} currentPid={} action={}",
            p.getProperty("pid"),ProcessHandle.current().pid(),action);
    }
    /** Child result is accepted by hub while the quest remains paused. */
    @Override public synchronized void childFinished(QuestServiceHub.Kind kind,
            QuestServiceHub.Outcome outcome,String proof) {
        if(kind==QuestServiceHub.Kind.NAVIGATION && gearAcq!=null){
            if(outcome==QuestServiceHub.Outcome.COMPLETE
                || outcome==QuestServiceHub.Outcome.UNAVAILABLE)
                gearAcqNavigationPending=true;
            return;
        }
        childProof=kind+":"+outcome+":"+proof;
        if(outcome==QuestServiceHub.Outcome.COMPLETE){
            if(kind==QuestServiceHub.Kind.FOOD_RESTOCK){
                java.util.regex.Matcher m=java.util.regex.Pattern
                    .compile("(?:^|;)\\s*spent=(\\d+)(?:$|;)")
                    .matcher(proof==null?"":proof);
                if(!m.find()){hold("food child spend proof missing");return;}
                try{childSpent=Math.addExact(childSpent,Integer.parseInt(m.group(1)));
                    if(childSpent>goal.maximumSpend())throw new IOException("Cumulative child spend exceeds goal");
                    save("PROVED");}
                catch(Exception ex){hold("food spend checkpoint uncertain: "+ex);return;}
            }
            if(kind==QuestServiceHub.Kind.NAVIGATION)verifyNavigationArrival=true;
            LOG.info("[QuestPreparation] CHILD_COMPLETE {}; reobserve before planning",childProof);
        } else if(outcome==QuestServiceHub.Outcome.UNAVAILABLE){
            QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.UNAVAILABLE,
                "Preparation child unavailable: "+childProof);
        } // Child HOLD retains the leaf lease; neither parent nor quest may act.
    }
    private boolean delegateGearGeTravel(){
        if(lease==null || goal==null || gearAcqNavigationPending)return false;
        NavigationGoal child=new NavigationGoal(goal.accountKey(),goal.pid(),
            goal.purpose()+":gear-ge",goal.requestId()+":gear-ge",
            new WorldPoint(3165,3486,0),2,goal.f2pOnly(),goal.maximumRisk(),
            0,false,Set.of(),2,goal.deadlineMillis());
        try{save("GEAR_GE_TRAVEL_PENDING");}
        catch(Exception ex){return false;}
        return QuestServiceHub.delegate(lease,this,child)!=null;
    }
    /** A requested equipment outcome may only equip that item, even from bank stock. */
    private static net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearRecoveryController.Ui
        restrictGearUi(net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearRecoveryController.Ui base,
                       Set<Integer> acceptable){
        return new net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearRecoveryController.Ui(){
            public net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearUpgradePlanner.Snapshot observe(){
                var s=base.observe();
                Map<Integer,net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearUpgradePlanner.Item> keep=
                    new HashMap<>();
                boolean satisfied=s.equipped().values().stream().anyMatch(acceptable::contains);
                for(var entry:s.verifiedItems().entrySet())
                    if((!satisfied && acceptable.contains(entry.getKey()))
                        || s.equipped().containsValue(entry.getKey()))
                        keep.put(entry.getKey(),entry.getValue());
                return new net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearUpgradePlanner.Snapshot(
                    s.at(),s.accountKey(),s.loggedIn(),s.membersWorld(),s.inCombat(),
                    s.bankOpen(),s.bankAudited(),s.equipmentObserved(),s.skillsObserved(),
                    s.freeSlots(),s.inventory(),s.bank(),s.equipped(),s.levels(),keep);
            }
            public boolean openBank(){return base.openBank();}
            public boolean closeBank(){return base.closeBank();}
            public boolean itemMode(){return base.itemMode();}
            public boolean setItemMode(){return base.setItemMode();}
            public boolean withdrawOne(int id){return acceptable.contains(id) && base.withdrawOne(id);}
            public boolean equipOne(int id){return acceptable.contains(id) && base.equipOne(id);}
        };
    }
    private void save(String state) throws IOException {
        Properties p=new Properties();
        p.setProperty("schema","PREP_BANK_2");
        try{p.setProperty("goalHash",goalFingerprint());}
        catch(Exception ex){throw new IOException("goal fingerprint failed",ex);}
        p.setProperty("state",state);
        p.setProperty("account",goal.accountKey());
        p.setProperty("pid",Long.toString(goal.pid()));
        p.setProperty("requestId",goal.requestId());
        p.setProperty("purpose",goal.purpose());
        p.setProperty("childSpent",Integer.toString(childSpent));
        p.setProperty("action",pending==null?"":pending.action().name());
        p.setProperty("item",pending==null?"0":Integer.toString(pending.itemId()));
        p.setProperty("quantity",pending==null?"0":Integer.toString(pending.quantity()));
        p.setProperty("beforeCarried",Integer.toString(beforeCarried));
        p.setProperty("beforeBank",Integer.toString(beforeBank));
        p.setProperty("at",Long.toString(pendingAt));
        Path tmp=Files.createTempFile(checkpoint.toAbsolutePath().getParent(),"prep-",".tmp");
        try {
            try(FileOutputStream out=new FileOutputStream(tmp.toFile())) {
                p.store(out,"Preparation pending input proof");out.getFD().sync();
            }
            Files.move(tmp,checkpoint,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(tmp);}
    }
    @Override public void close() throws IOException {
        try {lock.release();}finally{channel.close();}
    }
}
