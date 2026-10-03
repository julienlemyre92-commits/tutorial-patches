package net.runelite.client.plugins.microbot.questcommon.preparation.gear;

import java.util.*;
import java.util.function.Supplier;
import net.runelite.api.*;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questcommon.preparation.PreparationMicrobotAdapter;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;

/** Fresh client-backed equipment evidence. Controller must own input and journal dispatch. */
public final class GearRecoveryMicrobotAdapter implements GearRecoveryController.Ui {
    private final PreparationMicrobotAdapter bank=new PreparationMicrobotAdapter(List.of());

    public GearUpgradePlanner.Snapshot observe(){
        return Microbot.getClientThread().invoke((Supplier<GearUpgradePlanner.Snapshot>)()->{
            // Reuse the installed account identity, bank-container and same-floor checks.
            var base=bank.observe();
            if(!base.loggedIn())throw new IllegalStateException("gear snapshot requires logged-in player");
            Client c=Microbot.getClient();
            ItemContainer equipment=c.getItemContainer(InventoryID.EQUIPMENT);
            Map<String,Integer> worn=new HashMap<>();
            if(equipment!=null){
                Item[] items=equipment.getItems();
                for(EquipmentInventorySlot slot:EquipmentInventorySlot.values()){
                    int index=slot.getSlotIdx();
                    if(index>=0 && index<items.length && items[index]!=null
                        && items[index].getId()>0 && items[index].getQuantity()>0)
                        worn.put(slot.name(),items[index].getId());
                }
            }
            Map<String,Integer> levels=new HashMap<>();
            for(Skill skill:new Skill[]{Skill.ATTACK,Skill.STRENGTH,Skill.DEFENCE})
                levels.put(skill.name(),c.getRealSkillLevel(skill));
            var verified=GearRecoveryCatalog.verify(Microbot.getItemManager(),
                base.bank().keySet(),base.inventory().keySet(),new HashSet<>(worn.values()));
            return new GearUpgradePlanner.Snapshot(System.currentTimeMillis(),base.accountKey(),
                true,base.membersWorld(),base.inCombat(),base.bankOpen(),base.bankAudited(),
                equipment!=null,levels.values().stream().allMatch(level->level>0),base.freeSlots(),
                base.inventory(),base.bank(),worn,levels,verified);
        });
    }
    public boolean openBank(){return bank.openBank();}
    public boolean closeBank(){return bank.closeBank();}
    public boolean itemMode(){return Rs2Bank.hasWithdrawAsItem();}
    public boolean setItemMode(){return Rs2Bank.setWithdrawAsItem();}
    public boolean withdrawOne(int id){return Rs2Bank.withdrawOne(id);}
    public boolean equipOne(int id){
        String action=Microbot.getClientThread().invoke((Supplier<String>)()->{
            ItemComposition item=Microbot.getItemManager().getItemComposition(id);
            if(item==null || item.isMembers() || item.getInventoryActions()==null)return null;
            for(String value:item.getInventoryActions())
                if("Wield".equals(value) || "Wear".equals(value))return value;
            return null;
        });
        return action!=null && Rs2Inventory.interact(id,action);
    }
}
