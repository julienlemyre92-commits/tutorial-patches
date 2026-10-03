package net.runelite.client.plugins.microbot.questcommon.preparation;

import java.util.*;
import net.runelite.api.Skill;

/** Pure bank-first planner; unknown acquisition methods are explicit outcomes. */
public final class PreparationPlanner {
    public record Food(int itemId,int healing,boolean membersOnly) {
        public Food { if(itemId<=0 || healing<=0)throw new IllegalArgumentException(); }
    }
    public record Snapshot(long at,String accountKey,boolean loggedIn,boolean membersWorld,
        boolean inCombat,boolean bankNearby,boolean bankOpen,boolean bankAudited,boolean withdrawAsItem,
        int freeSlots,Map<Integer,Integer> inventory,Map<Integer,Integer> bank,
        Set<Integer> equipment,Map<Skill,Integer> levels,List<Food> food,boolean equipmentObserved) {
        public Snapshot(long at,String accountKey,boolean loggedIn,boolean membersWorld,
            boolean inCombat,boolean bankNearby,boolean bankOpen,boolean bankAudited,boolean withdrawAsItem,
            int freeSlots,Map<Integer,Integer> inventory,Map<Integer,Integer> bank,
            Set<Integer> equipment,Map<Skill,Integer> levels,List<Food> food){
            this(at,accountKey,loggedIn,membersWorld,inCombat,bankNearby,bankOpen,bankAudited,
                withdrawAsItem,freeSlots,inventory,bank,equipment,levels,food,true);
        }
        public Snapshot {
            inventory=Map.copyOf(inventory);bank=Map.copyOf(bank);
            equipment=Set.copyOf(equipment);levels=Map.copyOf(levels);food=List.copyOf(food);
        }
        public int carried(int id){return inventory.getOrDefault(id,0);}
        public int stored(int id){return bank.getOrDefault(id,0);}
    }
    public enum Action { COMPLETE, OPEN_BANK, SET_ITEM_MODE, DEPOSIT, WITHDRAW,
        CLOSE_BANK, NEED_MONEY, UNSUPPORTED, HOLD }
    public enum NeedKind { ACQUISITION, TRAINING, EQUIPMENT, MONEY, ROUTE }
    public record Need(NeedKind kind,int itemId,int quantity,Skill skill,int level) { }
    public record Decision(Action action,int itemId,int quantity,String reason,Need need) { }
    private PreparationPlanner() { }

    public static Decision choose(PreparationGoal g,Snapshot s) {
        if(!s.loggedIn() || !g.accountKey().equals(s.accountKey())
            || s.at()<=0 || System.currentTimeMillis()-s.at()>3000)
            return d(Action.HOLD,0,0,"fresh same-account frame unavailable");
        if(System.currentTimeMillis()>g.deadlineMillis())
            return d(Action.HOLD,0,0,"preparation deadline exceeded");
        if(g.f2pOnly() && s.membersWorld())
            return d(Action.HOLD,0,0,"F2P-only request on members world");
        if(s.inCombat())return d(Action.HOLD,0,0,"banking while in combat not authorized");
        for(var e:g.skillMinimum().entrySet())
            if(s.levels().getOrDefault(e.getKey(),0)<e.getValue())
                return new Decision(Action.UNSUPPORTED,0,0,"training provider required",
                    new Need(NeedKind.TRAINING,0,0,e.getKey(),e.getValue()));
        if(!g.equippedAnyOf().isEmpty() && !s.equipmentObserved())
            return d(Action.HOLD,0,0,"required equipment evidence unavailable");
        if(!g.equippedAnyOf().isEmpty() && Collections.disjoint(s.equipment(),g.equippedAnyOf()))
            return new Decision(Action.UNSUPPORTED,0,0,"equipment provider required",
                new Need(NeedKind.EQUIPMENT,0,0,null,0));
        int healing=0,foodCount=0;
        for(Food f:s.food()) if(!g.f2pOnly() || !f.membersOnly()){
            healing+=s.carried(f.itemId())*f.healing();
            foodCount+=s.carried(f.itemId());
        }
        boolean missing=false;
        for(var e:g.inventoryMinimum().entrySet())
            if(s.carried(e.getKey())<e.getValue()){missing=true;break;}
        Decision surplus=surplus(g,s);
        if(!missing && healing>=g.minimumHealing() && foodCount>=g.minimumFoodCount() && s.carried(995)>=g.minimumCoins()
            && s.freeSlots()>=g.minimumFreeSlots() && surplus==null)
            return d(s.bankOpen()?Action.CLOSE_BANK:Action.COMPLETE,0,0,"outcomes proved");
        if(!s.bankOpen())return s.bankNearby()
            ?d(Action.OPEN_BANK,0,0,"nearby bank proved; fresh audit required")
            :new Decision(Action.UNSUPPORTED,0,0,"safe reachable bank route provider not wired",
                new Need(NeedKind.ROUTE,0,0,null,0));
        if(!s.bankAudited())return d(Action.HOLD,0,0,"open bank contents not proved");
        if(surplus!=null)return surplus;
        if(!s.withdrawAsItem())return d(Action.SET_ITEM_MODE,0,0,"unnoted withdrawal required");
        for(var e:new TreeMap<>(g.inventoryMinimum()).entrySet()) {
            int need=e.getValue()-s.carried(e.getKey());
            if(need<=0)continue;
            if(s.stored(e.getKey())<need)
                return new Decision(Action.UNSUPPORTED,e.getKey(),need,
                    "bank shortfall; shop/GE/local provider not wired",
                    new Need(NeedKind.ACQUISITION,e.getKey(),need-s.stored(e.getKey()),null,0));
            if(s.freeSlots()==0)return deposit(g,s);
            return d(Action.WITHDRAW,e.getKey(),Math.min(need,s.freeSlots()),"bank stock proved");
        }
        if(healing<g.minimumHealing() || foodCount<g.minimumFoodCount()) {
            final int heal=healing;
            Food best=s.food().stream().filter(f->(!g.f2pOnly() || !f.membersOnly())
                && s.stored(f.itemId())>0)
                .min(Comparator.<Food>comparingInt(f->
                    (g.minimumHealing()-heal+f.healing()-1)/f.healing())
                    .thenComparingInt(Food::itemId)).orElse(null);
            if(best==null)return new Decision(Action.UNSUPPORTED,0,0,
                "bank food absent; acquisition provider not wired",
                    new Need(NeedKind.ACQUISITION,0,Math.max(0,g.minimumHealing()-healing),null,0));
            if(s.freeSlots()==0)return deposit(g,s);
            int amount=Math.min(Math.min(s.freeSlots(),s.stored(best.itemId())),
                Math.max(g.minimumFoodCount()-foodCount,
                    (g.minimumHealing()-healing+best.healing()-1)/best.healing()));
            return d(Action.WITHDRAW,best.itemId(),amount,"choose banked food for healing target");
        }
        int coins=g.minimumCoins()-s.carried(995);
        if(coins>0) {
            if(s.stored(995)<coins)return new Decision(Action.NEED_MONEY,995,coins,
                "bank coins insufficient",
                new Need(NeedKind.MONEY,995,coins-s.stored(995),null,0));
            if(s.freeSlots()==0 && s.carried(995)==0)return deposit(g,s);
            return d(Action.WITHDRAW,995,coins,"withdraw proved bank coins");
        }
        if(s.freeSlots()<g.minimumFreeSlots())return deposit(g,s);
        return d(Action.HOLD,0,0,"unclassified preparation state");
    }
    private static Decision deposit(PreparationGoal g,Snapshot s) {
        Decision decision=surplus(g,s);
        return decision!=null?decision:d(Action.HOLD,0,0,"no depositable slot; required supplies retained");
    }
    private static Decision surplus(PreparationGoal g,Snapshot s) {
        Map<Integer,Integer> retain=new HashMap<>(g.inventoryMinimum());
        retain.merge(995,g.minimumCoins(),Math::max);
        int carriedHealing=0,carriedFood=0;
        for(Food f:s.food())if(!g.f2pOnly()||!f.membersOnly()){
            carriedHealing+=s.carried(f.itemId())*f.healing();
            carriedFood+=s.carried(f.itemId());
        }
        if(carriedHealing<g.minimumHealing() || carriedFood<g.minimumFoodCount())
            retain.merge(995,Math.min(s.carried(995),Math.min(g.maximumSpend(),200)),Math::max);
        for(int id:g.protectedItems())retain.merge(id,s.carried(id),Math::max);
        for(int id:g.equippedAnyOf())
            if(s.carried(id)>0){retain.merge(id,1,Math::max);break;}
        List<Food> foods=s.food().stream().filter(f->!g.f2pOnly()||!f.membersOnly())
            .sorted(Comparator.comparingInt(Food::healing).reversed()
                .thenComparingInt(Food::itemId)).toList();
        long healing=0;
        int foodCount=0;
        for(Food f:foods)foodCount+=Math.min(s.carried(f.itemId()),retain.getOrDefault(f.itemId(),0));
        for(Food f:foods)healing+=(long)Math.min(s.carried(f.itemId()),
            retain.getOrDefault(f.itemId(),0))*f.healing();
        for(Food f:foods){
            if(healing>=g.minimumHealing() && foodCount>=g.minimumFoodCount())break;
            int reserved=Math.min(s.carried(f.itemId()),retain.getOrDefault(f.itemId(),0));
            int extra=(int)Math.min(s.carried(f.itemId())-reserved,
                Math.max(g.minimumFoodCount()-foodCount,
                    (g.minimumHealing()-healing+f.healing()-1)/f.healing()));
            retain.merge(f.itemId(),reserved+extra,Math::max);
            healing+=(long)extra*f.healing();
            foodCount+=extra;
        }
        for(var e:new TreeMap<>(s.inventory()).entrySet()) {
            int id=e.getKey();
            int surplus=e.getValue()-retain.getOrDefault(id,0);
            if(surplus>0)return d(Action.DEPOSIT,id,surplus,"deposit one unprotected surplus stack");
        }
        return null;
    }
    private static Decision d(Action a,int id,int qty,String why){return new Decision(a,id,qty,why,null);}
}
