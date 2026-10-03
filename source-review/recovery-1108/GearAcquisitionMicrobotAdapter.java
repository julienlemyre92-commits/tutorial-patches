package net.runelite.client.plugins.microbot.questcommon.preparation.gear;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questcommon.acquisition.QuestGeBuyer;
import net.runelite.client.plugins.microbot.questcommon.preparation.PreparationMicrobotAdapter;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

/** Installed-client adapter; the BANKING owner supplies its guarded GE travel callback. */
public final class GearAcquisitionMicrobotAdapter implements GearAcquisitionController.Ui {
    private static final WorldPoint EXCHANGE=new WorldPoint(3165,3486,0);
    private final GearRecoveryMicrobotAdapter gear=new GearRecoveryMicrobotAdapter();
    private final PreparationMicrobotAdapter bank=new PreparationMicrobotAdapter(List.of());
    private final BooleanSupplier guardedTravel;
    private final String userAgent;
    public GearAcquisitionMicrobotAdapter(BooleanSupplier guardedTravel,String userAgent){
        this.guardedTravel=Objects.requireNonNull(guardedTravel);
        this.userAgent=Objects.requireNonNull(userAgent);
    }
    @Override public GearAcquisitionController.Frame observe(){
        var s=gear.observe();
        return Microbot.getClientThread().invoke((Supplier<GearAcquisitionController.Frame>)()->{
            Client c=Microbot.getClient();
            if(c==null || c.getGameState()!=GameState.LOGGED_IN
                || c.getLocalPlayer()==null)throw new IllegalStateException("client not logged in");
            WorldPoint here=c.getLocalPlayer().getWorldLocation();
            boolean near=here!=null && here.getPlane()==0 && here.distanceTo(EXCHANGE)<=10;
            return new GearAcquisitionController.Frame(s,near,Rs2GrandExchange.isOpen(),
                s.carried(995),s.bankOpen()?s.stored(995):0,Rs2Bank.hasWithdrawAsItem());
        });
    }
    @Override public Map<Integer,GearUpgradePlanner.Item> purchasableGear(){
        return Microbot.getClientThread().invoke((Supplier<Map<Integer,GearUpgradePlanner.Item>>)()->
            GearRecoveryCatalog.purchasableGear(Microbot.getItemManager()));
    }
    @Override public String itemName(int id){
        return Microbot.getClientThread().invoke((Supplier<String>)()->{
            ItemComposition item=Microbot.getItemManager().getItemComposition(id);
            return item==null?null:item.getName();
        });
    }
    @Override public int quote(int id)throws Exception{
        return QuestGeBuyer.previewPrice(id,userAgent);
    }
    @Override public boolean openBank(){return bank.openBank();}
    @Override public boolean withdrawCoins(int amount){return Rs2Bank.withdrawX(995,amount);}
    @Override public boolean closeBank(){return bank.closeBank();}
    @Override public boolean routeToExchange(){return guardedTravel.getAsBoolean();}
    @Override public boolean closeExchange(){Rs2GrandExchange.closeExchange();return true;}
    @Override public boolean equipOne(int id){return gear.equipOne(id);}
    @Override public GearAcquisitionController.Buyer buyer(int id,String name,int cap,String checkpoint){
        return GearAcquisitionController.nativeBuyer(id,name,cap,userAgent,checkpoint);
    }
}
