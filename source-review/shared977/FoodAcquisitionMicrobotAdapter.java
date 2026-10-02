package net.runelite.client.plugins.microbot.questcommon.acquisition;

import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.questcommon.navigation.*;

/** Native frame + the same Wiki quote formula as the live-proved GE buyer. */
public final class FoodAcquisitionMicrobotAdapter implements FoodAcquisitionService.AcquisitionUi {
    public interface BankEvidence {
        /** Fresh audited bank snapshot from the preparation provider, if the bank is closed. */
        FoodAcquisitionPlanner.BankAudit latest(String accountKey);
    }
    private static final WorldPoint EXCHANGE=new WorldPoint(3165,3486,0);
    private final BankEvidence bankEvidence;
    private final String userAgent;
    private final List<FoodAcquisitionPlanner.Food> foods;
    private volatile FoodAcquisitionPlanner.BankAudit lastLiveAudit;
    @Override public NavigationGoal travelGoal(FoodAcquisitionGoal goal,boolean toGe){
        WorldPoint destination=toGe?FundingRouteDestinations.grandExchange():
            FundingRouteDestinations.nearestBank().orElse(null);
        if(destination==null)return null;
        return new NavigationGoal(goal.accountKey(),goal.pid(),goal.purpose()+":travel",
            goal.requestId()+(toGe?":ge":":bank"),destination,2,goal.f2pOnly(),
            goal.maximumRisk(),0,false,Set.of(),2,goal.deadlineMillis());
    }
    public FoodAcquisitionMicrobotAdapter(BankEvidence bankEvidence,String userAgent) {
        this.bankEvidence=Objects.requireNonNull(bankEvidence);
        if(userAgent==null || userAgent.isBlank() || userAgent.length()<15)
            throw new IllegalArgumentException("descriptive Wiki API User-Agent required");
        this.userAgent=userAgent;
        foods=List.of(Rs2Food.TROUT,Rs2Food.SALMON,Rs2Food.TUNA,Rs2Food.LOBSTER)
            .stream().map(f->new FoodAcquisitionPlanner.Food(f.getId(),f.getName(),
                f.getHeal(),false)).toList();
    }
    @Override public FoodAcquisitionPlanner.Frame observe() {
        return Microbot.getClientThread().invoke((Supplier<FoodAcquisitionPlanner.Frame>)()->{
            Client c=Microbot.getClient();
            if(c==null || c.getGameState()!=GameState.LOGGED_IN || c.getLocalPlayer()==null)
                return new FoodAcquisitionPlanner.Frame("",System.currentTimeMillis(),
                    false,false,false,false,false,false,0,0,0,Map.of(),foods);
            ItemContainer inv=c.getItemContainer(InventoryID.INVENTORY);
            if(inv==null)throw new IllegalStateException("inventory container unavailable");
            String account;
            try {
                if(c.getUsername()==null || c.getLocalPlayer().getName()==null)
                    throw new IllegalStateException("account names unavailable");
                String id=c.getUsername().trim().toLowerCase(Locale.ROOT)+"\n"
                    +c.getLocalPlayer().getName().trim().toLowerCase(Locale.ROOT);
                account=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(id.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            } catch(Exception e){throw new IllegalStateException("account identity unavailable",e);}
            Map<Integer,Integer> carried=new HashMap<>();int slots=0;
            for(Item item:inv.getItems())if(item!=null && item.getId()>0
                && item.getQuantity()>0){slots++;carried.merge(item.getId(),item.getQuantity(),Integer::sum);}
            var point=c.getLocalPlayer().getWorldLocation();
            var bankWidget=c.getWidget(12,1);
            return new FoodAcquisitionPlanner.Frame(account,System.currentTimeMillis(),
                true,c.getWorldType().contains(WorldType.MEMBERS),Rs2Player.isInCombat(),
                point!=null && point.getPlane()==0 && point.distanceTo(EXCHANGE)<=10,
                Rs2Bank.isNearBank(8),
                bankWidget!=null && !bankWidget.isHidden(),
                c.getRealSkillLevel(Skill.HITPOINTS),Math.max(0,28-slots),
                carried.getOrDefault(995,0),carried,foods);
        });
    }
    @Override public FoodAcquisitionPlanner.BankAudit bankAudit(String accountKey) {
        FoodAcquisitionPlanner.BankAudit current=Microbot.getClientThread()
            .invoke((Supplier<FoodAcquisitionPlanner.BankAudit>)()->{
                Client c=Microbot.getClient();
                if(c==null || c.getGameState()!=GameState.LOGGED_IN || c.getLocalPlayer()==null)
                    throw new IllegalStateException("account unavailable during bank audit");
                try{
                    String user=c.getUsername(),name=c.getLocalPlayer().getName();
                    if(user==null || name==null)throw new IllegalStateException("account names unavailable");
                    String identity=user.trim().toLowerCase(Locale.ROOT)+"\n"+name.trim().toLowerCase(Locale.ROOT);
                    String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    if(!actual.equals(accountKey))throw new IllegalStateException("account changed during bank audit");
                }catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
                if(c.getWidget(12,1)==null || c.getWidget(12,1).isHidden())return null;
                ItemContainer bank=c.getItemContainer(InventoryID.BANK);
                if(bank==null)return null;
                Map<Integer,Integer> stock=new HashMap<>();
                for(Item item:bank.getItems())if(item!=null && item.getId()>0
                    && item.getQuantity()>0)
                    stock.merge(item.getId(),item.getQuantity(),Integer::sum);
                return new FoodAcquisitionPlanner.BankAudit(accountKey,
                    System.currentTimeMillis(),stock);
            });
        if(current!=null){lastLiveAudit=current;return current;}
        // An open bank with no container is unknown, never a cached stock proof.
        if(observe().bankOpen())return null;
        FoodAcquisitionPlanner.BankAudit local=lastLiveAudit;
        return local!=null && local.accountKey().equals(accountKey)
            ?local:bankEvidence.latest(accountKey);
    }
    @Override public FoodAcquisitionPlanner.Quote quote(int itemId) throws Exception {
        int price=QuestGeBuyer.previewPrice(itemId,userAgent);
        return new FoodAcquisitionPlanner.Quote(itemId,price,System.currentTimeMillis());
    }
    @Override public QuestGeBuyer buyer(FoodAcquisitionPlanner.Food food,int quantity,
            int cap,String checkpoint) {
        return new QuestGeBuyer(food.itemId(),food.name(),quantity,cap,userAgent,checkpoint);
    }
    @Override public boolean noActiveOffers(){
        return Microbot.getClientThread().invoke((Supplier<Boolean>)()->{
            Client c=Microbot.getClient();
            if(c==null || c.getGameState()!=GameState.LOGGED_IN || c.getLocalPlayer()==null)return false;
            GrandExchangeOffer[] offers=c.getGrandExchangeOffers();
            if(offers==null)return false;
            for(GrandExchangeOffer offer:offers)
                if(offer!=null && offer.getState()!=GrandExchangeOfferState.EMPTY)return false;
            return true;
        });
    }
    @Override public boolean openBank(){return Rs2Bank.openBank();}
    @Override public boolean withdrawCoins(int quantity){return Rs2Bank.withdrawX(995,quantity);}
    @Override public boolean withdrawFood(int itemId,int quantity){return Rs2Bank.withdrawX(itemId,quantity);}
    @Override public boolean withdrawAsItem(){return Rs2Bank.hasWithdrawAsItem();}
    @Override public boolean setWithdrawAsItem(){return Rs2Bank.setWithdrawAsItem();}
    @Override public boolean closeBank(){return Rs2Bank.closeBank();}
}
