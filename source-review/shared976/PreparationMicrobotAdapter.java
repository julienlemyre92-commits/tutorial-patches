package net.runelite.client.plugins.microbot.questcommon.preparation;

import java.util.*;
import java.util.function.Supplier;
import java.security.MessageDigest;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
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
                Rs2Bank.isNearBank(8),bankOpen,bankOpen && bank!=null,
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
    @Override public boolean openBank(){return Rs2Bank.openBank();}
    @Override public boolean setWithdrawAsItem(){return Rs2Bank.setWithdrawAsItem();}
    @Override public boolean deposit(int id,int amount){return Rs2Bank.depositX(id,amount);}
    @Override public boolean withdraw(int id,int amount){return amount==1
        ?Rs2Bank.withdrawOne(id):Rs2Bank.withdrawX(id,amount);}
    @Override public boolean closeBank(){return Rs2Bank.closeBank();}
}
