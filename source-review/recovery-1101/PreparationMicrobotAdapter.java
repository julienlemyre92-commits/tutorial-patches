package net.runelite.client.plugins.microbot.questcommon.preparation;

import java.util.*;
import java.util.function.Supplier;
import java.security.MessageDigest;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

/** Installed-API bank adapter; food facts are supplied by a separately verified catalog. */
public final class PreparationMicrobotAdapter implements PreparationBankService.BankUi {
    private final List<PreparationPlanner.Food> foodCatalog;
    public PreparationMicrobotAdapter(List<PreparationPlanner.Food> foodCatalog) {
        this.foodCatalog=List.copyOf(Objects.requireNonNull(foodCatalog));
    }
    @Override public PreparationPlanner.Snapshot observe() {
        return Microbot.getClientThread().invoke((Supplier<PreparationPlanner.Snapshot>)()->{
            Client c=Microbot.getClient();
            boolean logged=c!=null && c.getGameState()==GameState.LOGGED_IN
                && c.getLocalPlayer()!=null;
            if(!logged)return new PreparationPlanner.Snapshot(System.currentTimeMillis(),"",
                false,false,false,false,false,false,false,0,Map.of(),Map.of(),Set.of(),
                Map.of(),foodCatalog);
            String account;
            try {
                if(c.getUsername()==null || c.getLocalPlayer().getName()==null)
                    throw new IllegalStateException("account names unavailable");
                String identity=c.getUsername().trim().toLowerCase(Locale.ROOT)+"\n"
                    +c.getLocalPlayer().getName().trim().toLowerCase(Locale.ROOT);
                account=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }
            catch(Exception ex){throw new IllegalStateException("account identity unavailable",ex);}
            ItemContainer inv=c.getItemContainer(InventoryID.INVENTORY);
            ItemContainer equipment=c.getItemContainer(InventoryID.EQUIPMENT);
            Widget bankWidget=c.getWidget(12,1);
            boolean bankOpen=bankWidget!=null && !bankWidget.isHidden();
            ItemContainer bank=bankOpen?c.getItemContainer(InventoryID.BANK):null;
            if(inv==null)
                throw new PreparationBankService.SnapshotNotReady("inventory="+(inv!=null)+", equipment="+(equipment!=null));
            Map<Integer,Integer> carried=count(inv);
            Map<Integer,Integer> stored=bank==null?Map.of():count(bank);
            Set<Integer> worn=new HashSet<>(count(equipment).keySet());
            Map<Skill,Integer> levels=new EnumMap<>(Skill.class);
            for(Skill skill:Skill.values())levels.put(skill,c.getRealSkillLevel(skill));
            int used=0;
            if(inv!=null)for(Item item:inv.getItems())if(item!=null && item.getId()>0
                && item.getQuantity()>0)used++;
            return new PreparationPlanner.Snapshot(System.currentTimeMillis(),account,
                true,c.getWorldType().contains(WorldType.MEMBERS),Rs2Player.isInCombat(),
                bankOpen || nearbyBankTarget(c)!=null,bankOpen,bankOpen && bank!=null,
                bankOpen && Rs2Bank.hasWithdrawAsItem(),
                Math.max(0,28-used),carried,stored,worn,levels,foodCatalog,equipment!=null);
        });
    }
    private static Map<Integer,Integer> count(ItemContainer container) {
        Map<Integer,Integer> result=new HashMap<>();
        if(container!=null)for(Item item:container.getItems())
            if(item!=null && item.getId()>0 && item.getQuantity()>0)
                result.merge(item.getId(),item.getQuantity(),Integer::sum);
        return result;
    }
    /** A 2-D bank location can be upstairs while the player is on the ground floor. */
    private static boolean samePlaneAndNear(WorldPoint here,WorldPoint target){
        return here!=null && target!=null && here.getPlane()==target.getPlane()
            && Math.max(Math.abs(here.getX()-target.getX()),
                Math.abs(here.getY()-target.getY()))<=8;
    }
    /** Return only a currently observed target, never a bank enum coordinate. */
    private static Object nearbyBankTarget(Client c){
        if(c==null || c.getLocalPlayer()==null)return null;
        WorldPoint here=c.getLocalPlayer().getWorldLocation();
        GameObject bank=Rs2GameObject.findBank(8);
        if(bank!=null && samePlaneAndNear(here,bank.getWorldLocation()))return bank;
        WallObject booth=Rs2GameObject.findGrandExchangeBooth(8);
        if(booth!=null && samePlaneAndNear(here,booth.getWorldLocation()))return booth;
        Rs2NpcModel banker=Rs2Npc.getBankerNPC();
        if(banker!=null && samePlaneAndNear(here,banker.getWorldLocation()))return banker;
        return null;
    }
    @Override public boolean openBank(){
        Object target=Microbot.getClientThread().invoke((Supplier<Object>)()->
            nearbyBankTarget(Microbot.getClient()));
        if(target instanceof TileObject object)return Rs2Bank.openBank(object);
        if(target instanceof Rs2NpcModel banker)return Rs2Bank.openBank(banker);
        if(target instanceof NPC banker)return Rs2Bank.openBank(banker);
        return false;
    }
    @Override public boolean setWithdrawAsItem(){return Rs2Bank.setWithdrawAsItem();}
    @Override public boolean deposit(int id,int amount){return Rs2Bank.depositX(id,amount);}
    @Override public boolean withdraw(int id,int amount){return amount==1
        ?Rs2Bank.withdrawOne(id):Rs2Bank.withdrawX(id,amount);}
    @Override public boolean closeBank(){return Rs2Bank.closeBank();}
    @Override public net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearRecoveryController.Ui gearUi(){
        return new net.runelite.client.plugins.microbot.questcommon.preparation.gear.GearRecoveryMicrobotAdapter();
    }
}
