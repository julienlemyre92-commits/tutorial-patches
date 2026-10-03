package net.runelite.client.plugins.microbot.questcommon.acquisition;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceHub;
import net.runelite.client.plugins.microbot.questcommon.navigation.NavigationGoal;

/** FOOD_RESTOCK child worker; the parent preparation service owns final quest resume. */
public final class FoodAcquisitionService implements QuestServiceHub.ServicePlugin,AutoCloseable {
    public interface AcquisitionUi {
        FoodAcquisitionPlanner.Frame observe();
        FoodAcquisitionPlanner.BankAudit bankAudit(String accountKey);
        FoodAcquisitionPlanner.Quote quote(int itemId) throws Exception;
        QuestGeBuyer buyer(FoodAcquisitionPlanner.Food food,int quantity,int cap,
            String checkpoint);
        boolean openBank();
        boolean withdrawCoins(int quantity);
        boolean closeBank();
        /** Current account GE slots must all be empty before restoring an unexecuted funding request. */
        default boolean noActiveOffers(){return false;}
        default boolean withdrawFood(int itemId,int quantity){return false;}
        default boolean withdrawAsItem(){return false;}
        default boolean setWithdrawAsItem(){return false;}
        default NavigationGoal travelGoal(FoodAcquisitionGoal goal,boolean toGe){return null;}
    }
    /** True only when a registered worker consumes FundingGoal under the same hub. */
    public interface FundingBridge { boolean compatibleMoneyProvider(); }
    public record FundingGoal(String accountKey,long pid,String purpose,String requestId,
        int targetCoins,Set<Integer> protectedItems,int maximumRisk,long deadlineMillis)
        implements QuestServiceHub.Request {
        public FundingGoal {
            if(targetCoins<1 || protectedItems==null)throw new IllegalArgumentException();
            protectedItems=Set.copyOf(protectedItems);
        }
        @Override public QuestServiceHub.Kind kind(){return QuestServiceHub.Kind.MONEY_MAKING;}
    }
    private static final org.slf4j.Logger LOG=
        org.slf4j.LoggerFactory.getLogger(FoodAcquisitionService.class);
    private final AcquisitionUi ui;
    private final FundingBridge funding;
    private final Path checkpoint;
    private final FileChannel channel;
    private final FileLock lock;
    private final Map<Integer,FoodAcquisitionPlanner.Quote> quotes=new HashMap<>();
    private QuestServiceHub.Lease lease;
    private FoodAcquisitionGoal goal;
    private QuestGeBuyer buyer;
    private String buyerCheckpoint="";
    private int spent,buyItem,buyQuantity,buyCap,beforeCoins,beforeItems;
    private FoodAcquisitionPlanner.Decision pendingBank;
    private int beforeBankCoins;
    private int pendingItem,beforeBankItem,beforeCarriedItem;
    private long pendingAt;
    private boolean restoredHold,waitingFunding,navigationSceneCheckPending;
    private String childFailure="";
    private final Set<Boolean> attemptedTravel=new HashSet<>();

    public FoodAcquisitionService(AcquisitionUi ui,FundingBridge funding,Path checkpoint)
        throws IOException {
        this.ui=Objects.requireNonNull(ui);this.funding=Objects.requireNonNull(funding);
        this.checkpoint=Objects.requireNonNull(checkpoint);
        Files.createDirectories(checkpoint.toAbsolutePath().getParent());
        channel=FileChannel.open(checkpoint.resolveSibling(checkpoint.getFileName()+".lock"),
            StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        FileLock acquired=channel.tryLock();
        if(acquired==null){channel.close();throw new IOException("food worker checkpoint locked");}
        lock=acquired;
        restoredHold=Files.exists(checkpoint);
    }
    @Override public QuestServiceHub.Kind kind(){return QuestServiceHub.Kind.FOOD_RESTOCK;}
    public boolean register(){
        boolean accepted=QuestServiceHub.register(this);
        LOG.info("[FoodAcquisition] registered={} hub={}",accepted,QuestServiceHub.snapshot());
        return accepted;
    }
    public boolean unregister(){return QuestServiceHub.unregister(this);}

    public synchronized void tick() {
        QuestServiceHub.Lease current=QuestServiceHub.activeFor(this);
        if(current==null)return; // parent or funding child has the input lease
        String cancellation=QuestServiceHub.cancellationReason(current);
        if(cancellation!=null){
            QuestServiceHub.finish(current,this,QuestServiceHub.Outcome.HOLD,cancellation);return;
        }
        if(!(current.request instanceof FoodAcquisitionGoal g)) {
            QuestServiceHub.finish(current,this,QuestServiceHub.Outcome.HOLD,
                "FOOD_RESTOCK request is not FoodAcquisitionGoal");return;
        }
        if(lease!=current) {
            lease=current;goal=g;
            // A provider survives many quests. Never inherit a previous request's
            // budget, quote, input checkpoint or child result.
            buyer=null;buyerCheckpoint="";spent=0;
            buyItem=buyQuantity=buyCap=beforeCoins=beforeItems=beforeBankCoins=0;
            pendingBank=null;pendingAt=0;waitingFunding=false;childFailure="";quotes.clear();
            attemptedTravel.clear();navigationSceneCheckPending=false;
        }
        FoodAcquisitionPlanner.Frame f;
        try {f=ui.observe();}catch(Exception ex){hold("food frame unavailable: "+ex);return;}
        if(f==null || !f.loggedIn())return;
        if(!goal.accountKey().equals(f.accountKey())){hold("account changed");return;}
        long now=System.currentTimeMillis();
        if(f.observedAt()<=0 || f.observedAt()>now || now-f.observedAt()>3000
            || now>goal.deadlineMillis() || f.inCombat()
            || goal.f2pOnly() && f.membersWorld()){
            hold("fresh safe frame and unexpired request required");return;
        }
        if(!QuestServiceHub.owns(lease,this,f.accountKey()))return;
        if(restoredHold){restoreUnexecutedFunding(f);return;}
        if(navigationSceneCheckPending){
            navigationSceneCheckPending=false;
            if(attemptedTravel.contains(true) && f.nearExchange() && !f.bankOpen()
                && !f.inCombat() && ui.noActiveOffers()){
                LOG.info("[FoodAcquisition] GE_SCENE_RECOVERED navigation unavailable but fresh safe GE scene proved");
            } else childFailure="navigation unavailable without a proved usable GE scene";
        }
        if(!childFailure.isBlank()){hold("service child failed: "+childFailure);return;}
        if(pendingBank!=null){verifyBankAction(f);return;}
        if(waitingFunding){
            waitingFunding=false;
            try {save("FUNDING_RETURNED");}catch(IOException ex){hold("funding checkpoint failed");}
            return; // re-observe actual coins next tick
        }
        if(buyer!=null){tickBuyer(f);return;}
        FoodAcquisitionPlanner.BankAudit bank;
        try {bank=ui.bankAudit(goal.accountKey());}
        catch(Exception ex){hold("bank audit unavailable: "+ex);return;}
        for(FoodAcquisitionPlanner.Food food:f.supportedFood()) {
            FoodAcquisitionPlanner.Quote q=quotes.get(food.itemId());
            if(q!=null && System.currentTimeMillis()-q.observedAt()<=30_000)continue;
            try {quotes.put(food.itemId(),ui.quote(food.itemId()));}
            catch(Exception ex){quotes.remove(food.itemId());}
        }
        // Market lookups may take longer than the planner's three-second frame
        // limit. Re-observe after them so the decision uses current game state.
        try {f=ui.observe();}
        catch(Exception ex){hold("food frame refresh unavailable: "+ex);return;}
        FoodAcquisitionPlanner.Decision d=FoodAcquisitionPlanner.choose(goal,f,bank,quotes,spent);
        switch(d.action()) {
            case COMPLETE:
                try {Files.deleteIfExists(checkpoint);}catch(IOException ex){hold("checkpoint cleanup failed");return;}
                QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.COMPLETE,
                    "fresh carried healing/food count proved; all owned GE offers settled; spent="+spent);
                lease=null;goal=null;break;
            case NEED_BANK_ROUTE:
            case NEED_GE_ROUTE:
                travel(f,d.action()==FoodAcquisitionPlanner.Action.NEED_GE_ROUTE);break;
            case OPEN_BANK_FOR_COINS:
            case OPEN_BANK_FOR_AUDIT:
            case WITHDRAW_BANK_COINS:
            case WITHDRAW_BANK_FOOD:
            case SET_ITEM_MODE:
            case CLOSE_BANK:
                if(d.action()==FoodAcquisitionPlanner.Action.WITHDRAW_BANK_FOOD && !ui.withdrawAsItem())
                    d=new FoodAcquisitionPlanner.Decision(FoodAcquisitionPlanner.Action.SET_ITEM_MODE,null,0,0,0,
                        "withdraw edible items, not notes");
                pendingBank=d;pendingAt=System.currentTimeMillis();
                beforeCoins=f.coins();
                beforeBankCoins=bank==null?0:bank.stock().getOrDefault(995,0);
                pendingItem=d.food()==null?0:d.food().itemId();
                beforeCarriedItem=f.count(pendingItem);
                beforeBankItem=bank==null?0:bank.stock().getOrDefault(pendingItem,0);
                try {save("BANK_INPUT_PENDING");}
                catch(IOException ex){hold("bank input checkpoint failed");return;}
                if(!QuestServiceHub.owns(lease,this,f.accountKey())){
                    hold("bank input lease lost");return;
                }
                boolean accepted;
                try {accepted=switch(d.action()) {
                    case OPEN_BANK_FOR_COINS, OPEN_BANK_FOR_AUDIT -> ui.openBank();
                    case WITHDRAW_BANK_COINS -> ui.withdrawCoins(d.quantity());
                    case WITHDRAW_BANK_FOOD -> ui.withdrawFood(pendingItem,d.quantity());
                    case SET_ITEM_MODE -> ui.setWithdrawAsItem();
                    case CLOSE_BANK -> ui.closeBank();
                    default -> false;
                };}catch(Exception ex){hold("bank input uncertain: "+ex);return;}
                if(!accepted)hold("bank input rejected/uncertain: "+d.action());
                break;
            case NEED_MONEY:
                if(!funding.compatibleMoneyProvider()){
                    unavailable("typed MONEY_MAKING need targetCoins="+d.targetCoins()
                        +"; compatible child worker not installed");break;
                }
                FundingGoal child=new FundingGoal(goal.accountKey(),goal.pid(),
                    goal.purpose()+":food-funding",goal.requestId()+":funding",
                    d.targetCoins(),goal.protectedItems(),goal.maximumRisk(),goal.deadlineMillis());
                try {save("FUNDING_REQUESTED");}
                catch(IOException ex){hold("funding checkpoint failed");return;}
                if(QuestServiceHub.delegate(lease,this,child)==null){
                    hold("funding child lease unavailable");return;
                }
                waitingFunding=true;break;
            case BUY:
                buyItem=d.food().itemId();buyQuantity=d.quantity();buyCap=d.totalCap();
                beforeCoins=f.coins();beforeItems=f.count(buyItem);
                try {save("BUY_SELECTED");
                    buyer=ui.buyer(d.food(),buyQuantity,buyCap,buyerCheckpoint);
                } catch(Exception ex){hold("buyer start uncertain: "+ex);}
                break;
            case HOLD: hold(d.reason());break;
        }
    }
    private void tickBuyer(FoodAcquisitionPlanner.Frame f) {
        if(f.bankOpen() || !f.nearExchange()){
            hold("bank/GE scene changed during owned buyer");return;
        }
        QuestGeBuyer.Result r;
        try {r=buyer.tick(this::persistBuyer);}
        catch(Exception ex){hold("buyer input/checkpoint uncertain: "+ex);return;}
        switch(r.outcome) {
            case WORKING: break;
            case COMPLETE:
                if(r.actualSpent<0 || r.actualSpent>buyCap
                    || r.coins<0 || beforeCoins-r.coins!=r.actualSpent
                    || r.itemCount<beforeItems+buyQuantity){
                    hold("buyer result and carried coin/item deltas disagree");return;
                }
                spent+=Math.toIntExact(r.actualSpent);
                buyer=null;buyerCheckpoint="";
                try {save("BUY_PROVED");}
                catch(IOException ex){hold("proved buy checkpoint failed");}
                break; // parent food outcome re-audited on next tick
            case NEED_COINS: hold("buyer quote needs more coins than planned; no offer repeat");break;
            case NEEDS_OVERVIEW: hold("GE overview requires scene recovery");break;
            case CANCELLED: unavailable("owned GE buy cancelled with refund/slot proof");break;
            case HOLD: hold("owned GE buyer: "+r.reason);break;
        }
    }
    private void verifyBankAction(FoodAcquisitionPlanner.Frame f) {
        FoodAcquisitionPlanner.BankAudit audit;
        try {audit=ui.bankAudit(goal.accountKey());}
        catch(Exception ex){hold("bank proof snapshot unavailable: "+ex);return;}
        boolean freshAudit=audit!=null && goal.accountKey().equals(audit.accountKey())
            && audit.observedAt()>0 && audit.observedAt()<=System.currentTimeMillis()
            && System.currentTimeMillis()-audit.observedAt()<=3000;
        boolean proved=switch(pendingBank.action()) {
            case OPEN_BANK_FOR_COINS, OPEN_BANK_FOR_AUDIT -> f.bankOpen() && freshAudit;
            case SET_ITEM_MODE -> f.bankOpen() && ui.withdrawAsItem();
            case WITHDRAW_BANK_FOOD -> f.bankOpen() && freshAudit
                && f.count(pendingItem)==beforeCarriedItem+pendingBank.quantity()
                && audit.stock().getOrDefault(pendingItem,0)==beforeBankItem-pendingBank.quantity();
            case WITHDRAW_BANK_COINS -> f.bankOpen() && freshAudit
                && f.coins()==beforeCoins+pendingBank.quantity()
                && audit.stock().getOrDefault(995,0)==beforeBankCoins-pendingBank.quantity();
            case CLOSE_BANK -> !f.bankOpen();
            default -> false;
        };
        if(proved){
            pendingBank=null;
            try {save("BANK_INPUT_PROVED");}catch(IOException ex){hold("bank proof checkpoint failed");}
        } else if(System.currentTimeMillis()-pendingAt>8000)
            hold("bank input unproved; no replay: "+pendingBank.action());
    }
    private void persistBuyer(String checkpointValue) {
        buyerCheckpoint=checkpointValue;
        try {save("BUY_PENDING");}
        catch(IOException ex){throw new UncheckedIOException(ex);}
    }
    @Override public synchronized void childFinished(QuestServiceHub.Kind kind,
            QuestServiceHub.Outcome outcome,String proof) {
        if(kind!=QuestServiceHub.Kind.MONEY_MAKING && kind!=QuestServiceHub.Kind.NAVIGATION)return;
        if(kind==QuestServiceHub.Kind.NAVIGATION
            && outcome==QuestServiceHub.Outcome.UNAVAILABLE){
            navigationSceneCheckPending=true;return;
        }
        if(outcome!=QuestServiceHub.Outcome.COMPLETE)childFailure=outcome+": "+proof;
    }
    private void travel(FoodAcquisitionPlanner.Frame frame,boolean toGe){
        if(buyer!=null || pendingBank!=null || frame.bankOpen()){
            hold("food travel requires settled transaction and closed bank");return;
        }
        if(attemptedTravel.contains(toGe)){
            hold("navigation did not produce usable "+(toGe?"GE":"bank")+" access; no replay");return;
        }
        NavigationGoal request=ui.travelGoal(goal,toGe);
        if(request==null){unavailable("supported food travel destination unavailable");return;}
        try{save(toGe?"GE_TRAVEL_PENDING":"BANK_TRAVEL_PENDING");}
        catch(IOException ex){hold("travel checkpoint failed");return;}
        if(QuestServiceHub.delegate(lease,this,request)==null){
            unavailable("shared navigation provider unavailable");return;
        }
        attemptedTravel.add(toGe);
    }
    private void unavailable(String reason) {
        try {Files.deleteIfExists(checkpoint);}catch(IOException ex){hold("checkpoint cleanup failed");return;}
        QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.UNAVAILABLE,reason);
        lease=null;goal=null;
    }
    private void hold(String reason) {
        if(lease!=null)QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.HOLD,reason);
    }
    private void restoreUnexecutedFunding(FoodAcquisitionPlanner.Frame f){
        try {
            Properties prior=new Properties();
            try(InputStream in=Files.newInputStream(checkpoint)){prior.load(in);}
            int priorFood=Integer.parseInt(prior.getProperty("pendingItem","0"));
            int priorCoins=Integer.parseInt(prior.getProperty("beforeCoins","-1"));
            int priorItems=Integer.parseInt(prior.getProperty("beforeItems","-1"));
            String phase=prior.getProperty("phase","");
            if("FOOD_ACQ_1".equals(prior.getProperty("schema"))
                && "GE_TRAVEL_PENDING".equals(phase)
                && goal.accountKey().equals(prior.getProperty("account"))
                && prior.getProperty("pendingBankAction","").isEmpty()
                && Integer.parseInt(prior.getProperty("pendingBankQuantity","-1"))==0
                && prior.getProperty("buyerCheckpoint","").isEmpty()
                && Integer.parseInt(prior.getProperty("buyItem","-1"))==0
                && Integer.parseInt(prior.getProperty("buyQuantity","-1"))==0
                && Integer.parseInt(prior.getProperty("buyCap","-1"))==0
                && Integer.parseInt(prior.getProperty("spent","-1"))==0
                && f.nearExchange() && !f.bankOpen() && !f.inCombat()
                && ui.noActiveOffers()){
                Files.deleteIfExists(checkpoint);
                restoredHold=false;attemptedTravel.add(true);
                LOG.info("[FoodAcquisition] GE_TRAVEL_RECONCILED priorPid={} currentPid={} freshScene=PROVED",
                    prior.getProperty("pid"),ProcessHandle.current().pid());
                return; // Reobserve and replan; no old movement or GE input replay.
            }
            if(!"FOOD_ACQ_1".equals(prior.getProperty("schema"))
                || !("FUNDING_REQUESTED".equals(phase) || "REPLAN_READY".equals(phase))
                || !goal.accountKey().equals(prior.getProperty("account"))
                || !goal.requestId().equals(prior.getProperty("requestId"))
                || !prior.getProperty("pendingBankAction","").isEmpty()
                || Integer.parseInt(prior.getProperty("pendingBankQuantity","-1"))!=0
                || !prior.getProperty("buyerCheckpoint","").isEmpty()
                || Integer.parseInt(prior.getProperty("buyItem","-1"))!=0
                || Integer.parseInt(prior.getProperty("buyQuantity","-1"))!=0
                || Integer.parseInt(prior.getProperty("buyCap","-1"))!=0
                || Integer.parseInt(prior.getProperty("spent","-1"))!=0
                || priorFood<=0 || priorCoins<0 || priorItems<0){
                hold("persisted food/funding transaction: foreign or pending food action");return;
            }
            if(Files.exists(checkpoint.resolveSibling("funding.properties"))
                || Files.exists(checkpoint.resolveSibling("funding.properties.seller"))){
                hold("persisted food/funding transaction: funding checkpoint remains");return;
            }
            // Preparation may have banked surplus coins because the parent goal
            // required zero carried coins. Do not call that funding success, and
            // do not replay the old child request. Require a current bank audit.
            FoodAcquisitionPlanner.BankAudit bank=ui.bankAudit(goal.accountKey());
            long now=System.currentTimeMillis();
            if(bank==null || !goal.accountKey().equals(bank.accountKey())
                || bank.observedAt()<=0 || bank.observedAt()>now
                || now-bank.observedAt()>10_000){
                hold("persisted food/funding transaction: fresh bank audit unavailable");return;
            }
            int carriedShortfall=Math.max(0,priorCoins-f.coins());
            if(bank.stock().getOrDefault(995,0)<carriedShortfall){
                hold("persisted food/funding transaction: coin movement lacks bank reserve proof");return;
            }
            if(!ui.noActiveOffers()){
                hold("persisted food/funding transaction: GE slots unknown or nonempty");return;
            }
            if("FUNDING_REQUESTED".equals(phase)){
                prior.setProperty("phase","REPLAN_READY");
                prior.setProperty("replanObservedAt",Long.toString(now));
                prior.setProperty("replanPid",Long.toString(ProcessHandle.current().pid()));
                prior.setProperty("replanCarriedCoins",Integer.toString(f.coins()));
                prior.setProperty("replanAuditedBankCoins",
                    Integer.toString(bank.stock().getOrDefault(995,0)));
                persistReplan(prior);
            }
            restoredHold=false;
            LOG.info("[FoodAcquisition] REPLAN_READY priorPid={} currentPid={} priorCoins={} carriedCoins={} auditedBankCoins={} foodId={}",
                prior.getProperty("pid"),ProcessHandle.current().pid(),priorCoins,f.coins(),
                bank.stock().getOrDefault(995,0),priorFood);
            // Reobserve and replan on the next tick. No input follows reconciliation.
        } catch(Exception failure){hold("food funding checkpoint reconciliation failed: "+failure);}
    }
    private void persistReplan(Properties p) throws IOException {
        Path tmp=Files.createTempFile(checkpoint.toAbsolutePath().getParent(),"food-replan-",".tmp");
        try{
            try(FileOutputStream out=new FileOutputStream(tmp.toFile())){
                p.store(out,"Food funding request reconciled to current-state replan; no action replayed");
                out.getFD().sync();
            }
            Files.move(tmp,checkpoint,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(tmp);}
    }
    private void save(String phase) throws IOException {
        Properties p=new Properties();
        p.setProperty("schema","FOOD_ACQ_1");p.setProperty("phase",phase);
        p.setProperty("account",goal.accountKey());p.setProperty("pid",Long.toString(goal.pid()));
        p.setProperty("requestId",goal.requestId());p.setProperty("spent",Integer.toString(spent));
        p.setProperty("buyerCheckpoint",buyerCheckpoint);
        p.setProperty("buyItem",Integer.toString(buyItem));
        p.setProperty("buyQuantity",Integer.toString(buyQuantity));
        p.setProperty("buyCap",Integer.toString(buyCap));
        p.setProperty("beforeCoins",Integer.toString(beforeCoins));
        p.setProperty("beforeItems",Integer.toString(beforeItems));
        p.setProperty("pendingBankAction",pendingBank==null?"":pendingBank.action().name());
        p.setProperty("pendingBankQuantity",pendingBank==null?"0":Integer.toString(pendingBank.quantity()));
        p.setProperty("beforeBankCoins",Integer.toString(beforeBankCoins));
        p.setProperty("pendingItem",Integer.toString(pendingItem));
        p.setProperty("beforeBankItem",Integer.toString(beforeBankItem));
        p.setProperty("beforeCarriedItem",Integer.toString(beforeCarriedItem));
        p.setProperty("pendingAt",Long.toString(pendingAt));
        Path tmp=Files.createTempFile(checkpoint.toAbsolutePath().getParent(),"food-",".tmp");
        try {
            try(FileOutputStream out=new FileOutputStream(tmp.toFile())) {
                p.store(out,"Owned food acquisition transaction");out.getFD().sync();
            }
            Files.move(tmp,checkpoint,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(tmp);}
    }
    @Override public void close() throws IOException {
        try {lock.release();}finally{channel.close();}
    }
}

