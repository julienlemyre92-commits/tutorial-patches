package net.runelite.client.plugins.microbot.questcommon.training;

import java.util.Locale;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.StructComposition;
import net.runelite.api.EnumID;
import net.runelite.api.ParamID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

/** Isolated integration candidate. Caller owns input and may start at most one probe attack. */
public final class StrengthStyleSwitch {
    public enum Result { WAIT_SAFE, ACTION_SENT, WAIT_PROOF, READY_FOR_PROBE,
        VERIFIED, HOLD }
    private enum Phase { OPEN_TAB, VERIFY_TAB, SELECT_STYLE, VERIFY_STYLE,
        READY_FOR_PROBE, VERIFY_XP, VERIFIED, HOLD }
    private static final int[] BUTTONS={InterfaceID.CombatInterface._0,
        InterfaceID.CombatInterface._1,InterfaceID.CombatInterface._2,
        InterfaceID.CombatInterface._3};
    private static final int[] LABELS={InterfaceID.CombatInterface._0_TEXT,
        InterfaceID.CombatInterface._1_TEXT,InterfaceID.CombatInterface._2_TEXT,
        InterfaceID.CombatInterface._3_TEXT};
    private final int expectedWeaponId;
    private Phase phase=Phase.OPEN_TAB;
    private long actionAt, probeAt;
    private int targetIndex=-1, targetCategory=-1, baselineStrengthXp,
        baselineAttackXp, baselineHitpointsXp;
    private String reason="";

    /** Use -1 for empty weapon slot, or the currently equipped pickaxe ID. */
    public StrengthStyleSwitch(int expectedWeaponId) {
        this.expectedWeaponId=expectedWeaponId;
    }
    public String reason() { return reason; }
    public boolean canIssueOneProbeAttack() { return phase==Phase.READY_FOR_PROBE; }
    public boolean verified() { return phase==Phase.VERIFIED; }

    /** Call once per script tick only while this script owns input. */
    public Result tick(boolean safeToChange) {
        if (phase==Phase.HOLD) return Result.HOLD;
        Snapshot s=observe();
        if (s==null || !s.loggedIn || s.weaponId!=expectedWeaponId)
            return hold("Client unavailable or expected pickaxe changed");
        if (s.aggressiveIndex<0 || s.aggressiveIndex>3)
            return hold("No mapped Aggressive style for equipped weapon category "
                +s.category);
        if (targetIndex>=0 && (s.aggressiveIndex!=targetIndex
            || s.category!=targetCategory))
            return hold("Weapon style mapping changed during switch");
        if (phase==Phase.VERIFIED) {
            return s.selectedIndex==targetIndex ? Result.VERIFIED : hold("Verified style changed");
        }
        if (phase==Phase.OPEN_TAB) {
            targetIndex=s.aggressiveIndex;
            targetCategory=s.category;
            if (s.selectedIndex==targetIndex) {
                phase=Phase.READY_FOR_PROBE;
                return Result.READY_FOR_PROBE;
            }
            if (!safeToChange) return Result.WAIT_SAFE;
            if (Rs2Tab.isCurrentTab(InterfaceTab.COMBAT)) {
                phase=Phase.SELECT_STYLE;
                return Result.WAIT_PROOF;
            }
            if (!Rs2Tab.switchToCombatOptionsTab())
                return hold("Combat tab switch rejected");
            actionAt=System.currentTimeMillis();
            phase=Phase.VERIFY_TAB;
            return Result.ACTION_SENT;
        }
        if (phase==Phase.VERIFY_TAB) {
            if (Rs2Tab.isCurrentTab(InterfaceTab.COMBAT)) {
                phase=Phase.SELECT_STYLE;
                return Result.WAIT_PROOF;
            }
            if (System.currentTimeMillis()-actionAt>4000)
                return hold("Combat tab switch unproved");
            return Result.WAIT_PROOF;
        }
        if (phase==Phase.SELECT_STYLE) {
            if (s.selectedIndex==targetIndex) {
                phase=Phase.READY_FOR_PROBE;
                return Result.READY_FOR_PROBE;
            }
            if (!safeToChange) return Result.WAIT_SAFE;
            boolean expectedLabel=expectedWeaponId<0
                ? s.title.equalsIgnoreCase("Unarmed") && s.label.equalsIgnoreCase("Kick")
                : s.title.toLowerCase(Locale.ROOT).contains("pickaxe")
                    && (s.label.equalsIgnoreCase("Impale")
                        || s.label.equalsIgnoreCase("Smash"));
            if (!Rs2Tab.isCurrentTab(InterfaceTab.COMBAT)
                || !s.buttonVisible || !expectedLabel)
                return hold("Aggressive style widget or weapon label not visibly corroborated: "
                    +s.title+" / "+s.label);
            if (!Rs2Widget.clickWidget(BUTTONS[targetIndex]))
                return hold("Aggressive style widget click rejected");
            actionAt=System.currentTimeMillis();
            phase=Phase.VERIFY_STYLE;
            return Result.ACTION_SENT;
        }
        if (phase==Phase.VERIFY_STYLE) {
            if (s.selectedIndex==targetIndex) {
                phase=Phase.READY_FOR_PROBE;
                return Result.READY_FOR_PROBE;
            }
            if (System.currentTimeMillis()-actionAt>4000)
                return hold("Aggressive style varplayer COM_MODE=43 did not change");
            return Result.WAIT_PROOF;
        }
        if (phase==Phase.READY_FOR_PROBE) return Result.READY_FOR_PROBE;
        if (s.strengthXp>baselineStrengthXp
            && s.hitpointsXp>baselineHitpointsXp
            && s.attackXp==baselineAttackXp) {
            phase=Phase.VERIFIED;
            return Result.VERIFIED;
        }
        if (s.hitpointsXp>baselineHitpointsXp || s.attackXp>baselineAttackXp)
            return hold("Probe XP did not isolate Strength and Hitpoints");
        if (System.currentTimeMillis()-probeAt>30000)
            return hold("Probe attack produced no Strength/Hitpoints XP in 30s");
        return Result.WAIT_PROOF;
    }

    /** Call before dispatching exactly one combat probe; do not call again. */
    public boolean markProbeAttack() {
        if (phase!=Phase.READY_FOR_PROBE) return false;
        Snapshot s=observe();
        if (s==null || s.weaponId!=expectedWeaponId
            || s.selectedIndex!=targetIndex || s.category!=targetCategory) {
            hold("Style or weapon changed before probe attack");
            return false;
        }
        baselineStrengthXp=s.strengthXp;
        baselineAttackXp=s.attackXp;
        baselineHitpointsXp=s.hitpointsXp;
        probeAt=System.currentTimeMillis();
        phase=Phase.VERIFY_XP;
        return true;
    }

    private Result hold(String why) {
        reason=why;
        phase=Phase.HOLD;
        return Result.HOLD;
    }
    private static String text(Widget widget) {
        return widget==null || widget.getText()==null ? ""
            : widget.getText().replaceAll("<[^>]*>","").trim();
    }
    private static final class Snapshot {
        boolean loggedIn,buttonVisible;
        int weaponId=-1,category=-1,selectedIndex=-1,aggressiveIndex=-1;
        int strengthXp,attackXp,hitpointsXp;
        String title="",label="";
    }
    private Snapshot observe() {
        try {
            return Microbot.getClientThread().invoke((Supplier<Snapshot>)()->{
                Client c=Microbot.getClient();
                if (c==null) return null;
                Snapshot s=new Snapshot();
                s.loggedIn=c.getGameState()==GameState.LOGGED_IN;
                if (!s.loggedIn) return s;
                ItemContainer equipment=c.getItemContainer(InventoryID.EQUIPMENT);
                if (equipment!=null) {
                    Item[] worn=equipment.getItems();
                    int slot=EquipmentInventorySlot.WEAPON.getSlotIdx();
                    if (worn!=null && slot>=0 && slot<worn.length && worn[slot]!=null)
                        s.weaponId=worn[slot].getId();
                }
                s.selectedIndex=c.getVarpValue(VarPlayerID.COM_MODE);
                s.category=c.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
                s.strengthXp=c.getSkillExperience(Skill.STRENGTH);
                s.attackXp=c.getSkillExperience(Skill.ATTACK);
                s.hitpointsXp=c.getSkillExperience(Skill.HITPOINTS);
                EnumComposition categories=c.getEnum(EnumID.WEAPON_STYLES);
                if (categories==null) return s;
                int styleEnumId=categories.getIntValue(s.category);
                if (styleEnumId<0) return s;
                EnumComposition styles=c.getEnum(styleEnumId);
                if (styles==null || styles.getIntVals()==null) return s;
                int[] structs=styles.getIntVals();
                for (int i=0;i<Math.min(structs.length,BUTTONS.length);i++) {
                    StructComposition style=c.getStructComposition(structs[i]);
                    String kind=style==null?null:style.getStringValue(ParamID.ATTACK_STYLE_NAME);
                    if ("Aggressive".equalsIgnoreCase(kind)) {
                        s.aggressiveIndex=i;
                        break;
                    }
                }
                if (s.aggressiveIndex>=0) {
                    Widget button=c.getWidget(BUTTONS[s.aggressiveIndex]);
                    s.buttonVisible=button!=null && !button.isHidden();
                    s.label=text(c.getWidget(LABELS[s.aggressiveIndex]));
                }
                s.title=text(c.getWidget(InterfaceID.CombatInterface.TITLE));
                return s;
            });
        } catch (RuntimeException ex) {
            reason="Combat style observation failed: "+ex.getClass().getSimpleName();
            return null;
        }
    }
}
