package net.runelite.client.plugins.microbot.questcommon.preparation.gear;

import java.util.*;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.ItemComposition;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

/** Small F2P bank catalog. An observed owned item and installed stats are both required. */
public final class GearRecoveryCatalog {
    private record Rule(String slot,Skill skill,int level) { }
    private static final Map<Integer,Rule> RULES=Map.ofEntries(
        Map.entry(ItemID.BRONZE_PICKAXE,new Rule("WEAPON",Skill.ATTACK,1)),
        Map.entry(ItemID.IRON_PICKAXE,new Rule("WEAPON",Skill.ATTACK,1)),
        Map.entry(ItemID.STEEL_PICKAXE,new Rule("WEAPON",Skill.ATTACK,5)),
        Map.entry(ItemID.BRONZE_SWORD,new Rule("WEAPON",Skill.ATTACK,1)),
        Map.entry(ItemID.IRON_SWORD,new Rule("WEAPON",Skill.ATTACK,1)),
        Map.entry(ItemID.STEEL_SWORD,new Rule("WEAPON",Skill.ATTACK,5)),
        Map.entry(ItemID.BRONZE_SCIMITAR,new Rule("WEAPON",Skill.ATTACK,1)),
        Map.entry(ItemID.IRON_SCIMITAR,new Rule("WEAPON",Skill.ATTACK,1)),
        Map.entry(ItemID.STEEL_SCIMITAR,new Rule("WEAPON",Skill.ATTACK,5)),
        Map.entry(ItemID.BRONZE_MED_HELM,new Rule("HEAD",Skill.DEFENCE,1)),
        Map.entry(ItemID.IRON_MED_HELM,new Rule("HEAD",Skill.DEFENCE,1)),
        Map.entry(ItemID.STEEL_MED_HELM,new Rule("HEAD",Skill.DEFENCE,5)),
        Map.entry(ItemID.BRONZE_FULL_HELM,new Rule("HEAD",Skill.DEFENCE,1)),
        Map.entry(ItemID.IRON_FULL_HELM,new Rule("HEAD",Skill.DEFENCE,1)),
        Map.entry(ItemID.STEEL_FULL_HELM,new Rule("HEAD",Skill.DEFENCE,5)),
        Map.entry(ItemID.BRONZE_PLATEBODY,new Rule("BODY",Skill.DEFENCE,1)),
        Map.entry(ItemID.IRON_PLATEBODY,new Rule("BODY",Skill.DEFENCE,1)),
        Map.entry(ItemID.STEEL_PLATEBODY,new Rule("BODY",Skill.DEFENCE,5)),
        Map.entry(ItemID.BRONZE_CHAINBODY,new Rule("BODY",Skill.DEFENCE,1)),
        Map.entry(ItemID.IRON_CHAINBODY,new Rule("BODY",Skill.DEFENCE,1)),
        Map.entry(ItemID.STEEL_CHAINBODY,new Rule("BODY",Skill.DEFENCE,5)),
        Map.entry(ItemID.BRONZE_PLATELEGS,new Rule("LEGS",Skill.DEFENCE,1)),
        Map.entry(ItemID.IRON_PLATELEGS,new Rule("LEGS",Skill.DEFENCE,1)),
        Map.entry(ItemID.STEEL_PLATELEGS,new Rule("LEGS",Skill.DEFENCE,5)),
        Map.entry(ItemID.LEATHER_COWL,new Rule("HEAD",Skill.DEFENCE,1)),
        Map.entry(ItemID.LEATHER_ARMOUR,new Rule("BODY",Skill.DEFENCE,1)),
        Map.entry(ItemID.LEATHER_CHAPS,new Rule("LEGS",Skill.DEFENCE,1)));
    private GearRecoveryCatalog() { }

    /** Market candidates are catalogued F2P combat gear, never arbitrary quest items. */
    public static Map<Integer,GearUpgradePlanner.Item> purchasableGear(ItemManager manager) {
        Objects.requireNonNull(manager);
        Map<Integer,GearUpgradePlanner.Item> found=new HashMap<>();
        Set<Integer> meleeWeapons=Set.of(ItemID.BRONZE_SWORD,ItemID.IRON_SWORD,
            ItemID.STEEL_SWORD,ItemID.BRONZE_SCIMITAR,ItemID.IRON_SCIMITAR,
            ItemID.STEEL_SCIMITAR);
        for(var entry:RULES.entrySet()) {
            int id=entry.getKey(); Rule rule=entry.getValue();
            if(!Set.of("HEAD","BODY","LEGS").contains(rule.slot())
                && !meleeWeapons.contains(id))continue;
            ItemComposition composition=manager.getItemComposition(id);
            ItemStats stats=manager.getItemStats(id);
            ItemEquipmentStats gear=stats==null?null:stats.getEquipment();
            if(composition==null || composition.isMembers() || !composition.isGeTradeable()
                || stats==null
                || !stats.isEquipable() || gear==null || gear.isTwoHanded()
                || !rule.slot().equals(slot(gear.getSlot()))
                || !hasEquipAction(composition.getInventoryActions()))continue;
            int defence=gear.getDstab()+gear.getDslash()+gear.getDcrush();
            int attack=Math.max(gear.getAstab(),Math.max(gear.getAslash(),gear.getAcrush()));
            found.put(id,new GearUpgradePlanner.Item(id,rule.slot(),false,false,
                Map.of(rule.skill().name(),rule.level()),
                Map.of("MELEE_ATTACK",attack,"STRENGTH",gear.getStr(),
                    "MELEE_DEFENCE",defence,"MAGIC_DEFENCE",gear.getDmagic())));
        }
        return Map.copyOf(found);
    }

    /** Call on the client thread after an audited bank/container snapshot. */
    public static Map<Integer,GearUpgradePlanner.Item> verify(ItemManager manager,
        Set<Integer> bankIds,Set<Integer> inventoryIds,Set<Integer> wornIds) {
        Objects.requireNonNull(manager);Objects.requireNonNull(bankIds);
        Objects.requireNonNull(inventoryIds);Objects.requireNonNull(wornIds);
        Set<Integer> observed=new HashSet<>(bankIds);
        observed.addAll(inventoryIds);observed.addAll(wornIds);
        Map<Integer,GearUpgradePlanner.Item> result=new HashMap<>();
        for(int id:observed){
            Rule rule=RULES.get(id);
            if(rule==null && !wornIds.contains(id))continue; // Never select unverified bank gear.
            ItemComposition composition=manager.getItemComposition(id);
            ItemStats stats=manager.getItemStats(id);
            ItemEquipmentStats gear=stats==null?null:stats.getEquipment();
            if(composition==null || composition.isMembers() || stats==null
                || !stats.isEquipable() || gear==null || gear.isTwoHanded())continue;
            String slot=slot(gear.getSlot());
            if(slot==null || rule!=null && !slot.equals(rule.slot()))continue;
            if(rule!=null && !hasEquipAction(composition.getInventoryActions()))continue;
            int attack=Math.max(gear.getAstab(),Math.max(gear.getAslash(),gear.getAcrush()));
            int defence=gear.getDstab()+gear.getDslash()+gear.getDcrush();
            Map<String,Integer> bonuses=Map.of("MELEE_ATTACK",attack,
                "STRENGTH",gear.getStr(),"MELEE_DEFENCE",defence,
                "MAGIC_DEFENCE",gear.getDmagic());
            Map<String,Integer> levels=rule==null?Map.of()
                :Map.of(rule.skill().name(),rule.level()); // Already-worn gear proved equipability.
            result.put(id,new GearUpgradePlanner.Item(id,slot,false,false,levels,bonuses));
        }
        return Map.copyOf(result);
    }
    private static String slot(int index){
        for(EquipmentInventorySlot slot:EquipmentInventorySlot.values())
            if(slot.getSlotIdx()==index){
                return switch(slot){
                    case WEAPON -> "WEAPON";
                    case HEAD -> "HEAD";
                    case BODY -> "BODY";
                    case LEGS -> "LEGS";
                    default -> null;
                };
            }
        return null;
    }
    private static boolean hasEquipAction(String[] actions){
        if(actions==null)return false;
        for(String action:actions)if(action!=null
            && (action.equalsIgnoreCase("Wield") || action.equalsIgnoreCase("Wear")))return true;
        return false;
    }
}
