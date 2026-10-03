package net.runelite.client.plugins.microbot.questcommon.acquisition;

import java.util.*;

/** Chooses one supported F2P GE purchase from fresh state and quoted total cost. */
public final class FoodAcquisitionPlanner {
    public record Food(int itemId,String name,int healing,boolean membersOnly) {
        public Food {if(itemId<=0 || name==null || name.isBlank() || healing<=0)
            throw new IllegalArgumentException();}
    }
    public record Quote(int itemId,int unitPrice,long observedAt) { }
    public record BankAudit(String accountKey,long observedAt,Map<Integer,Integer> stock) {
        public BankAudit { stock=Map.copyOf(stock); }
    }
    public record Frame(String accountKey,long observedAt,boolean loggedIn,boolean membersWorld,
        boolean inCombat,boolean nearExchange,boolean bankNearby,boolean bankOpen,
        int hpLevel,int freeSlots,
        int coins,Map<Integer,Integer> carried,List<Food> supportedFood) {
        public Frame {carried=Map.copyOf(carried);supportedFood=List.copyOf(supportedFood);}
        public int count(int id){return carried.getOrDefault(id,0);}
    }
    public enum Action { COMPLETE, BUY, OPEN_BANK_FOR_COINS, OPEN_BANK_FOR_AUDIT,
        WITHDRAW_BANK_COINS, WITHDRAW_BANK_FOOD, SET_ITEM_MODE,
        CLOSE_BANK, NEED_MONEY, NEED_BANK_ROUTE, NEED_GE_ROUTE, HOLD }
    public record Decision(Action action,Food food,int quantity,int totalCap,
        int targetCoins,String reason) { }
    private FoodAcquisitionPlanner() { }

    public static Decision choose(FoodAcquisitionGoal g,Frame f,BankAudit bank,
            Map<Integer,Quote> quotes,int alreadySpent) {
        long now=System.currentTimeMillis();
        if(f==null || !f.loggedIn())
            return d(Action.HOLD,null,0,0,0,"logged-in food frame unavailable");
        if(!g.accountKey().equals(f.accountKey()))
            return d(Action.HOLD,null,0,0,0,"food frame account changed");
        if(now-f.observedAt()>3000 || f.observedAt()>now)
            return d(Action.HOLD,null,0,0,0,"food frame stale or future: age="+(now-f.observedAt()));
        if(now>g.deadlineMillis())
            return d(Action.HOLD,null,0,0,0,"food request deadline expired");
        if(f.inCombat())
            return d(Action.HOLD,null,0,0,0,"food frame reports combat");
        if(g.f2pOnly() && f.membersWorld())
            return d(Action.HOLD,null,0,0,0,"food frame reports members world");
        if(bank==null || !g.accountKey().equals(bank.accountKey())
            || bank.observedAt()>now || now-bank.observedAt()>120_000)
            return d(f.bankOpen()?Action.HOLD:f.bankNearby()?Action.OPEN_BANK_FOR_AUDIT:Action.NEED_BANK_ROUTE,
                null,0,0,0,"refresh exact same-account bank audit");
        if(alreadySpent<0 || alreadySpent>g.maximumSpend())
            return d(Action.HOLD,null,0,0,0,"spend ledger invalid");
        int foodCount=0,healing=0;
        for(Food food:f.supportedFood())if(!g.f2pOnly() || !food.membersOnly()) {
            int count=f.count(food.itemId());foodCount+=count;
            healing+=count*Math.min(food.healing(),f.hpLevel());
        }
        if(foodCount>=g.minimumFoodCount() && healing>=g.minimumHealing())
            return d(f.bankOpen()?Action.CLOSE_BANK:Action.COMPLETE,null,0,0,0,"carried food outcomes proved");
        if(f.hpLevel()<=0 || f.freeSlots()<1)
            return d(Action.HOLD,null,0,0,0,"HP level or food capacity unavailable");
        for(Food food:f.supportedFood())
            if((!g.f2pOnly() || !food.membersOnly())
                && bank.stock().getOrDefault(food.itemId(),0)>0){
                int needed=Math.max(g.minimumFoodCount()-foodCount,
                    (g.minimumHealing()-healing+Math.min(food.healing(),f.hpLevel())-1)
                        /Math.min(food.healing(),f.hpLevel()));
                int amount=Math.min(f.freeSlots(),Math.min(Math.max(1,needed),bank.stock().get(food.itemId())));
                return d(f.bankOpen()?Action.WITHDRAW_BANK_FOOD:
                    f.bankNearby()?Action.OPEN_BANK_FOR_AUDIT:Action.NEED_BANK_ROUTE,
                    food,amount,0,0,"use verified bank food before buying");
            }
        Decision best=null;
        for(Food food:f.supportedFood()) {
            if(g.f2pOnly() && food.membersOnly())continue;
            Quote q=quotes.get(food.itemId());
            if(q==null || q.itemId()!=food.itemId() || q.unitPrice()<1
                || q.observedAt()>now || now-q.observedAt()>30_000)continue;
            int effectiveHeal=Math.min(food.healing(),f.hpLevel());
            if(effectiveHeal<1)continue;
            int countNeed=g.minimumFoodCount()-foodCount;
            int healNeed=(g.minimumHealing()-healing+effectiveHeal-1)/effectiveHeal;
            int amount=Math.max(1,Math.max(countNeed,healNeed));
            if(amount>f.freeSlots() || amount>28)continue;
            long total=(long)amount*q.unitPrice();
            if(total>g.maximumSpend()-alreadySpent || total>1000)continue;
            Decision candidate=d(Action.BUY,food,amount,(int)total,(int)total,
                "fresh quoted cost for full food outcome");
            if(best==null || candidate.totalCap()<best.totalCap()
                || candidate.totalCap()==best.totalCap()
                    && candidate.food().itemId()<best.food().itemId())best=candidate;
        }
        if(best==null)return d(Action.HOLD,null,0,0,0,
            "no fresh affordable supported quote fits capacity/spend limit");
        if(f.coins()<best.totalCap()) {
            int deficit=best.totalCap()-f.coins();
            int bankCoins=bank.stock().getOrDefault(995,0);
            if(bankCoins>0) {
                // A small rounded reserve avoids exact-price bank withdrawals
                // while keeping the carried surplus bounded.
                int increment=deficit<100?10:50;
                int rounded=((deficit+increment-1)/increment)*increment;
                int withdrawal=Math.min(bankCoins,Math.min(rounded,deficit+50));
                if(f.bankOpen())return d(Action.WITHDRAW_BANK_COINS,best.food(),withdrawal,
                    best.totalCap(),best.totalCap(),"withdraw proved bank funding");
                return f.bankNearby()
                    ?d(Action.OPEN_BANK_FOR_COINS,best.food(),withdrawal,best.totalCap(),
                        best.totalCap(),"nearby bank can fund selected food")
                    :d(Action.NEED_BANK_ROUTE,best.food(),withdrawal,best.totalCap(),
                        best.totalCap(),"travel to bank for existing funds");
            }
            if(f.bankOpen())return d(Action.CLOSE_BANK,best.food(),0,best.totalCap(),
                best.totalCap(),"close bank before money child");
            return d(Action.NEED_MONEY,best.food(),best.quantity(),best.totalCap(),
                best.totalCap(),"carried and bank coins insufficient");
        }
        if(f.bankOpen())return d(Action.CLOSE_BANK,best.food(),0,best.totalCap(),
            best.totalCap(),"funded; close bank before GE");
        if(!f.nearExchange())return d(Action.NEED_GE_ROUTE,best.food(),0,best.totalCap(),
            best.totalCap(),"travel to GE before buyer input");
        return best;
    }
    private static Decision d(Action a,Food f,int q,int cap,int coins,String why) {
        return new Decision(a,f,q,cap,coins,why);
    }
}
