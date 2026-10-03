package net.runelite.client.plugins.microbot.questcommon.preparation.gear;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.runelite.client.plugins.microbot.questcommon.acquisition.QuestGeBuyer;

/** Optional, caller-ticked bank-to-GE armour fallback under the caller's BANKING lease. */
public final class GearAcquisitionController {
    public record Goal(String accountKey,long deadlineMillis,int maximumSpend,int minimumCoins,
                       int minimumDefensiveSlots,int minimumFreeSlots,String userAgent,
                       String defenceMetric,int minimumSlotBonus,
                       int minimumMeleeAttack,int minimumStrength,
                       Set<Integer> acceptableEquippedIds) {
        public Goal {
            if(accountKey==null || accountKey.isBlank() || deadlineMillis<=0 || maximumSpend<0
                || minimumCoins<0 || minimumDefensiveSlots<0 || minimumDefensiveSlots>3
                || minimumFreeSlots<1 || minimumFreeSlots>28 || userAgent==null
                || !Set.of("MELEE_DEFENCE","MAGIC_DEFENCE").contains(defenceMetric)
                || minimumMeleeAttack<0 || minimumStrength<0
                || acceptableEquippedIds==null
                || userAgent.length()<15)throw new IllegalArgumentException("bounded gear goal");
            acceptableEquippedIds=Set.copyOf(acceptableEquippedIds);
            if(acceptableEquippedIds.stream().anyMatch(id->id==null || id<=0))
                throw new IllegalArgumentException("acceptable item id");
        }
    }
    public record Frame(GearUpgradePlanner.Snapshot gear,boolean nearExchange,
                        boolean exchangeOpen,
                        int carriedCoins,int bankCoins,boolean bankItemMode) { }
    public interface Ui {
        /** Fresh same-account client-thread snapshot; includes installed, verified F2P metadata. */
        Frame observe();
        Map<Integer,GearUpgradePlanner.Item> purchasableGear();
        String itemName(int id);
        int quote(int id) throws Exception;
        boolean openBank(); boolean withdrawCoins(int amount); boolean closeBank();
        boolean routeToExchange(); boolean closeExchange(); boolean equipOne(int id);
        Buyer buyer(int id,String name,int cap,String checkpoint);
    }
    public record BuyResult(QuestGeBuyer.Outcome outcome,String phase,String reason,
                            long actualSpent,int coins,int itemCount) { }
    public interface Buyer {
        BuyResult tick(java.util.function.Consumer<String> persist);
        default boolean resumeOwnedCollection(java.util.function.Consumer<String> persist){
            return false;
        }
    }
    /** Production adapter to the installed, journalled generic GE buyer. */
    public static Buyer nativeBuyer(int id,String name,int cap,String userAgent,String checkpoint){
        QuestGeBuyer buyer=new QuestGeBuyer(id,name,1,cap,userAgent,checkpoint);
        return new Buyer(){
            @Override public BuyResult tick(java.util.function.Consumer<String> persist){
                var r=buyer.tick(persist);
                return new BuyResult(r.outcome,r.phase,r.reason,r.actualSpent,r.coins,r.itemCount);
            }
            @Override public boolean resumeOwnedCollection(java.util.function.Consumer<String> persist){
                return buyer.resumeOwnedCollection(persist);
            }
        };
    }
    public record Result(boolean complete,boolean held,String detail) { }
    private enum Phase { AUDIT, OPEN_BANK, COINS, WITHDRAW_COINS, CLOSE_BANK,
        TRAVEL, BUY, CLOSE_EXCHANGE, EQUIP, EQUIP_PROOF, COMPLETE, HOLD }
    private final Goal goal;
    private final Ui ui;
    private final BooleanSupplier owns;
    private final Path checkpoint;
    private long purchaseDeadline;
    private Phase phase=Phase.AUDIT;
    private long intentAt;
    private int spent,selected,selectedCap,beforeCoins,beforeBankCoins,beforeItems;
    private String buyerCheckpoint="",error="";
    private boolean collectionOnlyRecovery;
    private Buyer buyer;

    public GearAcquisitionController(Goal goal,Ui ui,BooleanSupplier owns,Path checkpoint)
        throws IOException {
        this.goal=Objects.requireNonNull(goal);this.ui=Objects.requireNonNull(ui);
        this.owns=Objects.requireNonNull(owns);this.checkpoint=Objects.requireNonNull(checkpoint);
        this.purchaseDeadline=goal.deadlineMillis();
        if(Files.exists(checkpoint))restore();
    }
    /** Spend can be booked by the parent only after equipped outcome is proved. */
    public synchronized int actualSpent(){
        if(phase!=Phase.COMPLETE)throw new IllegalStateException("gear outcome unproved");
        return spent;
    }
    /** Resume only the exact saved filled buy; never starts a new offer. */
    private synchronized boolean resumeOwnedCollection(){
        if(phase!=Phase.HOLD || !"GE buyer HOLD: owned item collect control absent".equals(error)
            || buyerCheckpoint.isBlank() || selected<=0 || selectedCap<1
            || !owns.getAsBoolean())return false;
        Frame f=ui.observe();
        long now=System.currentTimeMillis();
        if(!fresh(f,now) || f.gear().bankOpen() || !f.nearExchange())return false;
        try{
            if(buyer==null){
                String name=ui.itemName(selected);
                if(name==null || name.isBlank())return false;
                buyer=ui.buyer(selected,name,selectedCap,buyerCheckpoint);
            }
            // This outer BUY intent is persisted before the child advances. A crash
            // at this boundary leaves the child HOLD, which cannot issue a new buy.
            collectionOnlyRecovery=true;phase=Phase.BUY;save();
            if(!buyer.resumeOwnedCollection(this::persistBuyer)){
                collectionOnlyRecovery=false;phase=Phase.HOLD;save();return false;
            }
            error="";save();return true;
        }catch(Exception ex){
            collectionOnlyRecovery=false;phase=Phase.HOLD;
            try{save();}catch(IOException ignored){}
            return false;
        }
    }
    public synchronized Result tick(){
        if(phase==Phase.HOLD && !resumeOwnedCollection())
            return new Result(false,true,error);
        if(!owns.getAsBoolean())return waitFor("BANKING lease unavailable");
        try {
            Frame f=ui.observe();long now=System.currentTimeMillis();
            if(!fresh(f,now))return hold("fresh same-account F2P noncombat bank/equipment frame required");
            GearUpgradePlanner.Snapshot g=f.gear();
            if(now>purchaseDeadline && !collectionOnlyRecovery)
                return hold("acquisition deadline exceeded");
            if(collectionOnlyRecovery && !Set.of(Phase.BUY,Phase.CLOSE_EXCHANGE,
                Phase.EQUIP,Phase.EQUIP_PROOF,Phase.COMPLETE).contains(phase))
                return hold("collection-only recovery cannot begin another purchase");
            if(phase==Phase.COMPLETE)return new Result(true,false,"armour slots proved");
            switch(phase){
                case AUDIT -> {
                    if(!g.bankOpen())return dispatch(Phase.OPEN_BANK,()->ui.openBank());
                    if(!g.bankAudited())return hold("bank stock not audited");
                    if(goalSatisfied(g)){
                        phase=Phase.COMPLETE;save();return new Result(true,false,"equipped armour proved");
                    }
                    // A bank-stock upgrade must be tried by the existing gear pass first.
                    if(hasBankUpgrade(g))return hold("eligible bank armour exists; run bank-stock gear pass first");
                    if(selected==0){
                        selected=choose(g,Math.min(1000,goal.maximumSpend()-spent));
                        if(selected==0)return hold("no quoted, level-valid F2P armour within budget");
                        save();
                    }
                    phase=Phase.COINS;save();return waitFor("candidate and budget saved");
                }
                case OPEN_BANK -> {
                    if(g.bankOpen() && g.bankAudited() && g.at()>intentAt){phase=Phase.AUDIT;save();return waitFor("bank audited");}
                    return timeout("bank open",now);
                }
                case COINS -> {
                    if(!g.bankOpen() || !g.bankAudited())return hold("bank closed before coin audit");
                    long target=(long)selectedCap+goal.minimumCoins();
                    if(target>Integer.MAX_VALUE)return hold("coin target overflows inventory count");
                    if(f.carriedCoins()<target){
                        int amount=(int)(target-f.carriedCoins());
                        if(f.bankCoins()<amount)return hold("bank coins below purchase and carried reserve");
                        beforeCoins=f.carriedCoins();beforeBankCoins=f.bankCoins();
                        return dispatch(Phase.WITHDRAW_COINS,()->ui.withdrawCoins(amount));
                    }
                    return dispatch(Phase.CLOSE_BANK,()->ui.closeBank());
                }
                case WITHDRAW_COINS -> {
                    int expected=Math.max(0,selectedCap+goal.minimumCoins()-beforeCoins);
                    if(g.at()>intentAt && g.bankOpen() && g.bankAudited()
                        && f.carriedCoins()==beforeCoins+expected
                        && f.bankCoins()==beforeBankCoins-expected){phase=Phase.COINS;save();return waitFor("coin delta proved");}
                    return timeout("coin withdrawal",now);
                }
                case CLOSE_BANK -> {
                    if(g.at()>intentAt && !g.bankOpen()){
                        phase=Phase.TRAVEL;intentAt=0;save();return waitFor("bank closure proved");}
                    return timeout("bank closure",now);
                }
                case TRAVEL -> {
                    if(f.nearExchange()){
                        beforeCoins=f.carriedCoins();beforeItems=g.carried(selected);
                        phase=Phase.BUY;save();return waitFor("GE scene proved");
                    }
                    if(intentAt>0)return now-intentAt>120000
                        ?hold("GE route unproved after 120s; no replay")
                        :waitFor("verifying GE route");
                    return dispatch(Phase.TRAVEL,()->ui.routeToExchange());
                }
                case BUY -> {
                    if(g.bankOpen() || !f.nearExchange())return hold("GE scene changed during owned offer");
                    if(buyer==null){
                        String name=ui.itemName(selected);
                        if(name==null || name.isBlank())return hold("item name unavailable");
                        buyer=ui.buyer(selected,name,selectedCap,buyerCheckpoint);
                    }
                    BuyResult r=buyer.tick(this::persistBuyer);
                    switch(r.outcome()){
                        case WORKING -> {return waitFor("GE buyer "+r.phase());}
                        case COMPLETE -> {
                            if(r.actualSpent()<0 || r.actualSpent()>selectedCap
                                || beforeCoins-r.coins()!=r.actualSpent()
                                || r.itemCount()<beforeItems+1)return hold("GE buyer deltas unproved");
                            spent=Math.addExact(spent,Math.toIntExact(r.actualSpent()));
                            if(spent>goal.maximumSpend())return hold("cumulative spend above goal");
                            buyer=null;buyerCheckpoint="";phase=Phase.CLOSE_EXCHANGE;
                            intentAt=0;save();return waitFor("GE item/coins proved; close Exchange");
                        }
                        case NEED_COINS,NEEDS_OVERVIEW,CANCELLED,HOLD -> {
                            return hold("GE buyer "+r.outcome()+": "+r.reason());
                        }
                    }
                    return hold("unsupported GE buyer outcome");
                }
                case CLOSE_EXCHANGE -> {
                    if(!f.exchangeOpen()){
                        phase=Phase.EQUIP;intentAt=0;save();return waitFor("Exchange closed");
                    }
                    if(intentAt>0)return timeout("Exchange closure",now);
                    return dispatch(Phase.CLOSE_EXCHANGE,()->ui.closeExchange());
                }
                case EQUIP -> {
                    if(g.bankOpen() || g.carried(selected)<1 || !ui.purchasableGear().containsKey(selected))
                        return hold("collected item or metadata unavailable before equip");
                    GearUpgradePlanner.Item item=ui.purchasableGear().get(selected);
                    if(g.equipped().getOrDefault(item.slot(),0)==selected){
                        selected=0;selectedCap=0;phase=Phase.AUDIT;save();return waitFor("equipment proved");
                    }
                    beforeItems=g.carried(selected);
                    return dispatch(Phase.EQUIP_PROOF,()->ui.equipOne(selected));
                }
                case EQUIP_PROOF -> {
                    GearUpgradePlanner.Item item=ui.purchasableGear().get(selected);
                    if(item!=null && g.at()>intentAt && g.equipmentObserved()
                        && g.equipped().getOrDefault(item.slot(),0)==selected
                        && g.carried(selected)<beforeItems){
                        selected=0;selectedCap=0;intentAt=0;
                        phase=goalSatisfied(g)?Phase.COMPLETE:Phase.AUDIT;save();
                        return phase==Phase.COMPLETE
                            ?new Result(true,false,"requested equipment proved without bank reopen")
                            :waitFor("equip delta proved; re-audit remaining slots");
                    }
                    return timeout("equipment",now);
                }
                default -> {return hold("invalid acquisition phase");}
            }
        }catch(Exception ex){return hold("acquisition observation/action uncertain: "+ex);}
    }
    private int choose(GearUpgradePlanner.Snapshot g,int cap) throws Exception {
        if(cap<1)return 0;
        int best=0,bestPrice=Integer.MAX_VALUE;
        boolean needWeapon=needsWeapon(g);
        for(var entry:ui.purchasableGear().entrySet()){
            var item=entry.getValue();
            if(!goal.acceptableEquippedIds().isEmpty()
                && !goal.acceptableEquippedIds().contains(item.id()))continue;
            if(g.carried(item.id())>0
                || g.stored(item.id())>0)continue;
            GearUpgradePlanner.Item worn=g.verifiedItems().get(g.equipped().getOrDefault(item.slot(),0));
            if(needWeapon){
                if(!"WEAPON".equals(item.slot()) || !meetsWeaponFloor(item)
                    || !strongerWeapon(item,worn))continue;
            }else{
                if(!Set.of("HEAD","BODY","LEGS").contains(item.slot()))continue;
                int oldBonus=worn==null?0:worn.bonuses().getOrDefault(goal.defenceMetric(),0);
                int newBonus=item.bonuses().getOrDefault(goal.defenceMetric(),Integer.MIN_VALUE);
                if(newBonus<goal.minimumSlotBonus() || newBonus<=oldBonus)continue;
            }
            if(!levelsMet(item,g))continue;
            int price;
            try{price=ui.quote(item.id());}
            catch(Exception unavailable){continue;}
            if(price>0 && price<=cap && (price<bestPrice || price==bestPrice && item.id()<best)){
                best=item.id();bestPrice=price;
            }
        }
        if(best>0)selectedCap=cap; // buyer independently refreshes and enforces quote.
        return best;
    }
    private boolean hasBankUpgrade(GearUpgradePlanner.Snapshot g){
        boolean needWeapon=needsWeapon(g);
        for(var item:g.verifiedItems().values()){
            if(!goal.acceptableEquippedIds().isEmpty()
                && !goal.acceptableEquippedIds().contains(item.id()))continue;
            if(g.stored(item.id())<1 || item.membersOnly() || item.twoHanded()
                || !levelsMet(item,g))continue;
            GearUpgradePlanner.Item worn=g.verifiedItems().get(g.equipped().getOrDefault(item.slot(),0));
            if(needWeapon && "WEAPON".equals(item.slot())
                && meetsWeaponFloor(item) && strongerWeapon(item,worn))return true;
            if(!needWeapon && Set.of("HEAD","BODY","LEGS").contains(item.slot())
                && item.bonuses().getOrDefault(goal.defenceMetric(),Integer.MIN_VALUE)
                    >=goal.minimumSlotBonus()
                && item.bonuses().getOrDefault(goal.defenceMetric(),Integer.MIN_VALUE)
                    >(worn==null?0:worn.bonuses().getOrDefault(goal.defenceMetric(),0)))return true;
        }
        return false;
    }
    private boolean weaponReady(GearUpgradePlanner.Snapshot g){
        if(goal.minimumMeleeAttack()==0 && goal.minimumStrength()==0)return true;
        var worn=g.verifiedItems().get(g.equipped().getOrDefault("WEAPON",0));
        return worn!=null && !worn.membersOnly() && !worn.twoHanded()
            && meetsWeaponFloor(worn);
    }
    private boolean needsWeapon(GearUpgradePlanner.Snapshot g){
        if(!goal.acceptableEquippedIds().isEmpty()){
            if(acceptableReady(g))return false;
            return ui.purchasableGear().entrySet().stream()
                .anyMatch(e->goal.acceptableEquippedIds().contains(e.getKey())
                    && "WEAPON".equals(e.getValue().slot()));
        }
        return !weaponReady(g);
    }
    private boolean acceptableReady(GearUpgradePlanner.Snapshot g){
        return goal.acceptableEquippedIds().isEmpty()
            || g.equipped().values().stream().anyMatch(goal.acceptableEquippedIds()::contains);
    }
    private boolean goalSatisfied(GearUpgradePlanner.Snapshot g){
        return acceptableReady(g) && weaponReady(g)
            && defensiveSlots(g)>=goal.minimumDefensiveSlots();
    }
    private boolean meetsWeaponFloor(GearUpgradePlanner.Item item){
        return item.bonuses().getOrDefault("MELEE_ATTACK",Integer.MIN_VALUE)
                >=goal.minimumMeleeAttack()
            && item.bonuses().getOrDefault("STRENGTH",Integer.MIN_VALUE)
                >=goal.minimumStrength()
            && item.bonuses().getOrDefault("MAGIC_DEFENCE",Integer.MIN_VALUE)>=0;
    }
    private static boolean levelsMet(GearUpgradePlanner.Item item,GearUpgradePlanner.Snapshot g){
        return item.requiredLevels().entrySet().stream()
            .allMatch(e->g.levels().getOrDefault(e.getKey(),-1)>=e.getValue());
    }
    private static boolean strongerWeapon(GearUpgradePlanner.Item item,GearUpgradePlanner.Item worn){
        if(worn==null)return true;
        return item.bonuses().getOrDefault("MELEE_ATTACK",0)
                >worn.bonuses().getOrDefault("MELEE_ATTACK",0)
            || item.bonuses().getOrDefault("STRENGTH",0)
                >worn.bonuses().getOrDefault("STRENGTH",0);
    }
    private int defensiveSlots(GearUpgradePlanner.Snapshot g){
        int count=0;
        for(String slot:List.of("HEAD","BODY","LEGS")){
            var item=g.verifiedItems().get(g.equipped().getOrDefault(slot,0));
            if(item!=null && !item.membersOnly() && !item.twoHanded()
                && item.bonuses().getOrDefault(goal.defenceMetric(),Integer.MIN_VALUE)
                    >=goal.minimumSlotBonus())count++;
        }
        return count;
    }
    private boolean fresh(Frame f,long now){
        if(f==null || f.gear()==null)return false;
        var g=f.gear();
        return g.loggedIn() && goal.accountKey().equals(g.accountKey())
            && g.at()>0 && g.at()<=now && now-g.at()<=3000
            && !g.inCombat() && !g.membersWorld() && g.equipmentObserved()
            && g.skillsObserved() && g.freeSlots()>=goal.minimumFreeSlots()
            && f.carriedCoins()>=0 && f.bankCoins()>=0;
    }
    private Result dispatch(Phase next,java.util.function.BooleanSupplier input)throws IOException{
        phase=next;intentAt=System.currentTimeMillis();save();
        if(!owns.getAsBoolean())return hold("lease lost after saved intent");
        if(!input.getAsBoolean())return hold("input rejected or uncertain: "+next);
        return waitFor("verifying "+next);
    }
    private Result timeout(String action,long now){
        return now-intentAt>8000?hold(action+" unproved; no replay"):waitFor("verifying "+action);
    }
    private Result waitFor(String detail){return new Result(false,false,detail);}
    private Result hold(String detail){
        error=detail;phase=Phase.HOLD;
        try{save();}catch(Exception ex){error+="; checkpoint write failed";}
        return new Result(false,true,error);
    }
    private void persistBuyer(String value){
        if(!owns.getAsBoolean())throw new IllegalStateException("BANKING lease lost before GE input");
        buyerCheckpoint=value;
        try{save();}catch(IOException ex){throw new UncheckedIOException(ex);}
    }
    private void save()throws IOException{
        Properties p=new Properties();p.setProperty("schema","GEAR_BUY_1");
        p.setProperty("account",goal.accountKey());p.setProperty("deadline",Long.toString(purchaseDeadline));
        p.setProperty("maxSpend",Integer.toString(goal.maximumSpend()));
        p.setProperty("minCoins",Integer.toString(goal.minimumCoins()));
        p.setProperty("minSlots",Integer.toString(goal.minimumDefensiveSlots()));
        p.setProperty("freeSlots",Integer.toString(goal.minimumFreeSlots()));
        p.setProperty("userAgent",goal.userAgent());
        p.setProperty("metric",goal.defenceMetric());
        p.setProperty("minBonus",Integer.toString(goal.minimumSlotBonus()));
        p.setProperty("minAttack",Integer.toString(goal.minimumMeleeAttack()));
        p.setProperty("minStrength",Integer.toString(goal.minimumStrength()));
        p.setProperty("accepted",goal.acceptableEquippedIds().stream().sorted()
            .map(String::valueOf).reduce((a,b)->a+","+b).orElse(""));
        p.setProperty("phase",phase.name());p.setProperty("at",Long.toString(intentAt));
        p.setProperty("spent",Integer.toString(spent));p.setProperty("selected",Integer.toString(selected));
        p.setProperty("cap",Integer.toString(selectedCap));p.setProperty("beforeCoins",Integer.toString(beforeCoins));
        p.setProperty("beforeBankCoins",Integer.toString(beforeBankCoins));
        p.setProperty("beforeItems",Integer.toString(beforeItems));
        p.setProperty("buyer",buyerCheckpoint);p.setProperty("error",error);
        p.setProperty("collectionOnlyRecovery",Boolean.toString(collectionOnlyRecovery));
        Files.createDirectories(checkpoint.toAbsolutePath().getParent());
        Path tmp=checkpoint.resolveSibling(checkpoint.getFileName()+".tmp");
        try(var out=Files.newOutputStream(tmp)){p.store(out,"gear acquisition intent/proof");}
        Files.move(tmp,checkpoint,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    private void restore()throws IOException{
        Properties p=new Properties();try(var in=Files.newInputStream(checkpoint)){p.load(in);}
        long savedDeadline=Long.parseLong(p.getProperty("deadline"));
        boolean knownCollectHold="HOLD".equals(p.getProperty("phase"))
            && "GE buyer HOLD: owned item collect control absent".equals(p.getProperty("error"))
            && p.getProperty("buyer","").startsWith("GE2|");
        boolean alreadyCollecting=Boolean.parseBoolean(p.getProperty("collectionOnlyRecovery","false"));
        if(!"GEAR_BUY_1".equals(p.getProperty("schema"))
            || !goal.accountKey().equals(p.getProperty("account"))
            || goal.deadlineMillis()!=savedDeadline && !knownCollectHold && !alreadyCollecting
            || goal.maximumSpend()!=Integer.parseInt(p.getProperty("maxSpend"))
            || goal.minimumCoins()!=Integer.parseInt(p.getProperty("minCoins"))
            || goal.minimumDefensiveSlots()!=Integer.parseInt(p.getProperty("minSlots"))
            || goal.minimumFreeSlots()!=Integer.parseInt(p.getProperty("freeSlots"))
            || !goal.userAgent().equals(p.getProperty("userAgent"))
            || !goal.defenceMetric().equals(p.getProperty("metric"))
            || goal.minimumSlotBonus()!=Integer.parseInt(p.getProperty("minBonus"))
            || goal.minimumMeleeAttack()!=Integer.parseInt(p.getProperty("minAttack"))
            || goal.minimumStrength()!=Integer.parseInt(p.getProperty("minStrength"))
            || !goal.acceptableEquippedIds().stream().sorted().map(String::valueOf)
                .reduce((a,b)->a+","+b).orElse("").equals(p.getProperty("accepted")))
            throw new IOException("gear acquisition checkpoint/goal mismatch");
        // A renewed request cannot enlarge the old spend window. It may only
        // settle the already owned filled offer under the recovery-only gate.
        purchaseDeadline=savedDeadline;
        phase=Phase.valueOf(p.getProperty("phase"));intentAt=Long.parseLong(p.getProperty("at"));
        spent=Integer.parseInt(p.getProperty("spent"));selected=Integer.parseInt(p.getProperty("selected"));
        selectedCap=Integer.parseInt(p.getProperty("cap"));beforeCoins=Integer.parseInt(p.getProperty("beforeCoins"));
        beforeBankCoins=Integer.parseInt(p.getProperty("beforeBankCoins"));
        beforeItems=Integer.parseInt(p.getProperty("beforeItems"));
        buyerCheckpoint=p.getProperty("buyer","");error=p.getProperty("error","");
        collectionOnlyRecovery=Boolean.parseBoolean(p.getProperty("collectionOnlyRecovery","false"));
        // Persisted non-buyer UI intents have uncertain dispatch after restart.
        if(Set.of(Phase.OPEN_BANK,Phase.WITHDRAW_COINS,Phase.CLOSE_BANK,Phase.TRAVEL,
            Phase.CLOSE_EXCHANGE,
            Phase.EQUIP_PROOF).contains(phase)){
            phase=Phase.HOLD;error="restart with pending UI intent; reconcile manually";save();
        }
        if(phase==Phase.BUY && buyerCheckpoint.isBlank()){
            phase=Phase.HOLD;error="buyer checkpoint missing after restart";save();
        }
    }
}
