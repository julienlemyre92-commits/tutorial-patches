package net.runelite.client.plugins.microbot.belowicemountain;

import java.io.InputStream;
import java.io.OutputStream;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.util.Locale;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.VarClientInt;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.Experience;
import net.runelite.api.EnumComposition;
import net.runelite.api.StructComposition;
import net.runelite.api.EnumID;
import net.runelite.api.ParamID;
import net.runelite.api.events.StatChanged;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.WidgetIndices;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.events.WelcomeScreenEvent;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spells;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.input.InputArbiter;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.models.RS2Item;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.security.LoginManager;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Isolated Build70 guardian candidate; supervised actions require explicit controls. */
public class BelowIceMountainScript extends Script {
    private static final Logger log = LoggerFactory.getLogger(BelowIceMountainScript.class);
    public static final int BUILD_NUMBER = 80;
    // No unattended entry until a live exit and recovery path is proved.
    private static final boolean UNATTENDED_ESCAPE_VERIFIED = false;
    private static final WorldPoint ISLAND_BOAT = new WorldPoint(1619,4816,0);
    private static final WorldPoint FALADOR_BANK = new WorldPoint(3012,3356,0);
    private static final WorldPoint LUMBRIDGE_BANK = new WorldPoint(3208,3220,2);
    private static final WorldPoint GRAND_EXCHANGE = new WorldPoint(3165,3486,0);
    // Proven Cook's Assistant farm approach; chicken and gate reachability still need live proof.
    private static final WorldPoint CHICKEN_FARM = new WorldPoint(3238,3298,0);
    // Observed player tile after a completed WebWalk through the Lumbridge chicken pen.
    private static final WorldPoint CHICKEN_INTERIOR_PROOF_TILE = new WorldPoint(3232,3297,0);
    private static final WorldPoint WILLOW = new WorldPoint(3003,3435,0);
    private static final WorldPoint CHECKAL = new WorldPoint(3087,3415,0);
    private static final WorldPoint ATLAS = new WorldPoint(3076,3440,0);
    private static final WorldPoint PICKAXE_SPAWN = new WorldPoint(3083,3419,0);
    private static final WorldPoint MARLEY = new WorldPoint(3088,3470,0);
    private static final WorldPoint VARROCK_COOK = new WorldPoint(3230,3401,0);
    // Source areas: Lumbridge Castle kitchen; Wydin's Food Store in Port Sarim;
    // Rimmington range already used by the Witch's Potion project.
    // The Cook's Assistant route uses this proved kitchen interior tile.
    private static final WorldPoint SUPPLY_KITCHEN = new WorldPoint(3208,3213,0);
    private static final WorldPoint SUPPLY_WYDIN = new WorldPoint(3013,3204,0);
    private static final WorldPoint SUPPLY_RANGE = new WorldPoint(2970,3211,0);
    private static final int SUPPLY_RANGE_ID = 9682;
    private static final WorldPoint BURNTOF = new WorldPoint(2956,3367,0);
    private static final WorldPoint BARMAID = new WorldPoint(2954,3368,0);
    private static final WorldPoint DUNGEON_WILLOW = new WorldPoint(2996,3494,0);
    private static final WorldPoint DUNGEON_ENTRANCE = new WorldPoint(3000,3494,0);
    private static final Path STATUS = Paths.get(System.getProperty("user.home"),
        ".runelite", "belowicemountain", "status.properties");
    private static final Path CONTROL = STATUS.resolveSibling("control.properties");
    private static final Path PREP_GE_CHECKPOINT = STATUS.resolveSibling("prep-ge-checkpoint.txt");
    private static final int[] PICKAXES = {1265, 1267, 1269, 1273, 1271, 1275, 12297, 11920};
    private static final int[] FOOD = {315, 329, 333, 339, 361, 373, 379, 385, 391};
    private static final int[] ENTRY_FOOD = {ItemID.LOBSTER, ItemID.TUNA, ItemID.SALMON};
    private static final int PREP_GE_SPEND_CAP = 800;
    private static final int[] AUDIT_GEAR = {ItemID.IRON_SCIMITAR, ItemID.STEEL_SCIMITAR,
        ItemID.IRON_SWORD, ItemID.BRONZE_SWORD, ItemID.IRON_CHAINBODY,
        ItemID.BRONZE_MED_HELM};

    private static final class Frame {
        String gameState = "NO_CLIENT", questState = "UNKNOWN";
        WorldPoint position;
        int loginIndex = -1, world = -1, varp = -1, questPoints = -1;
        int mining = -1, miningXp = -1, attack = -1, hp = -1, maxHp = -1, hpXp = -1, combatLevel = -1;
        int attackXp = -1, strengthXp = -1, combatMode = -1, weaponCategory = -1;
        int weaponId = -1;
        int pickaxes = 0, food = 0, inventorySlots = -1;
        int checkal = -1, marley = -1, burntof = -1, canvasWidth = -1, emoteScroll = -1;
        boolean bankOpen, shopOpen, geOpen, production, inDialogue, hasContinue;
        String interactingNpc = "", unsafeAggressor = "", randomEventName = "";
        int randomEventId = -1, randomEventIndex = -1;
        String dialogue = "";
        final List<String> options = new ArrayList<>();
        final Map<Integer,Integer> items = new HashMap<>();
        boolean inventoryLoaded, equipmentLoaded;
        int count(int id) { return items.getOrDefault(id,0); }
    }

    private enum Proof { QUEST_ADVANCED, VARBIT_CHANGED, DIALOGUE_CHANGED, ITEM_GAINED,
        ITEM_GAINED_OR_DIALOGUE, ITEM_USED_OR_DIALOGUE, INVENTORY_SHED, BANK_TOGGLED,
        SHOP_TOGGLED, PRODUCTION_OPEN, ITEM_CONSUMED, HP_XP_GAINED, FOOD_HEAL,
        GE_CLOSED, WEAPON_EQUIPPED, WEAPON_UNEQUIPPED, STRENGTH_HP_XP_GAINED,
        ISLAND_EXIT, FLEX_OPEN, EMOTE_SCROLLED, DUNGEON_ENTERED }
    private static final class Pending {
        final String key; final Proof proof; final Frame before; final int item;
        final long at = System.currentTimeMillis(), timeout;
        Pending(String key, Proof proof, Frame before, int item, long timeout) {
            this.key=key; this.proof=proof; this.before=before; this.item=item; this.timeout=timeout;
        }
    }
    private static final class Route {
        final String key; final WorldPoint target, start; final int radius;
        final long at=System.currentTimeMillis();
        volatile boolean done;
        volatile long doneAt;
        Thread worker;
        Route(String key, WorldPoint target, WorldPoint start, int radius) {
            this.key=key; this.target=target; this.start=start; this.radius=radius;
        }
    }
    private static final class FlexView {
        final Widget widget;
        final java.awt.Rectangle panel, icon;
        final int scroll;
        FlexView(Widget widget, java.awt.Rectangle panel, java.awt.Rectangle icon, int scroll) {
            this.widget=widget; this.panel=panel; this.icon=icon; this.scroll=scroll;
        }
    }

    /** Script-local copy of the shared cue; the hot host loads script classes only. */
    private static final class LevelUpTabCue {
        enum Result { IDLE, QUEUED, WAITING_SAFE_POINT, WAITING_PROOF,
            READY_SKILL, WAITING_GUIDE_PROOF, READY_CLOSE, WAITING_CLOSE_PROOF,
            READY_INVENTORY, WAITING_INVENTORY_PROOF, PROVED, REJECTED }
        private final Map<Skill,Integer> levels = new EnumMap<>(Skill.class);
        private final ArrayDeque<SkillIncrease> increases = new ArrayDeque<>();
        private boolean baselineReady;
        private Result result = Result.IDLE;
        private Skill skill;
        private int previousLevel, newLevel;
        private long queuedAt, actionAt;
        private boolean guideSeen;
        private static final class SkillIncrease {
            final Skill skill;
            final int previous, current;
            final long at;
            SkillIncrease(Skill skill,int previous,int current,long at) {
                this.skill=skill; this.previous=previous; this.current=current; this.at=at;
            }
        }

        private void start(SkillIncrease increase) {
            skill=increase.skill; previousLevel=increase.previous;
            newLevel=increase.current; queuedAt=increase.at;
            guideSeen=false; actionAt=0; result=Result.QUEUED;
        }

        void onStatChanged(Skill changedSkill, int level, long nowMillis) {
            if (!baselineReady) return;
            Integer previous = levels.put(changedSkill,level);
            // A fresh client snapshot establishes the baseline before events are accepted.
            if (previous != null && level > previous) {
                SkillIncrease increase=new SkillIncrease(changedSkill,previous,level,nowMillis);
                if (result==Result.IDLE || result==Result.PROVED || result==Result.REJECTED)
                    start(increase);
                else increases.addLast(increase);
            }
        }

        Result tick(boolean safeToSwitch, long nowMillis) {
            if ((result == Result.IDLE || result == Result.PROVED || result == Result.REJECTED)
                && !increases.isEmpty()) start(increases.removeFirst());
            if (result == Result.IDLE || result == Result.PROVED
                || result == Result.REJECTED) return result;
            if (result == Result.WAITING_PROOF) {
                if (Rs2Tab.isCurrentTab(InterfaceTab.SKILLS)) result=Result.READY_SKILL;
                else if (nowMillis-actionAt>3000) result=Result.REJECTED;
                return result;
            }
            if (result == Result.WAITING_GUIDE_PROOF) {
                if (guideOpen()) { guideSeen=true; result=Result.READY_CLOSE; }
                else if (nowMillis-actionAt>3500) result=Result.READY_INVENTORY;
                return result;
            }
            if (result == Result.WAITING_CLOSE_PROOF) {
                if (!guideOpen()) result=Result.READY_INVENTORY;
                else if (nowMillis-actionAt>3500) result=Result.REJECTED;
                return result;
            }
            if (result == Result.WAITING_INVENTORY_PROOF) {
                if (Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY))
                    result=guideSeen?Result.PROVED:Result.REJECTED;
                else if (nowMillis-actionAt>3500) result=Result.REJECTED;
                return result;
            }
            if (!safeToSwitch) {
                if (result==Result.QUEUED) result=Result.WAITING_SAFE_POINT;
                return result;
            }
            if (result == Result.QUEUED || result == Result.WAITING_SAFE_POINT) {
                if (Rs2Tab.isCurrentTab(InterfaceTab.SKILLS)) result=Result.READY_SKILL;
                else if (Rs2Tab.switchTo(InterfaceTab.SKILLS)) {
                    actionAt=nowMillis; result=Result.WAITING_PROOF;
                } else result=Result.REJECTED;
                return result;
            }
            if (result == Result.READY_SKILL) {
                int index=skillIndex(skill);
                if (index<0 || !Rs2Widget.isWidgetVisible(
                    WidgetIndices.SkillsTab.GROUP_INDEX,index)) {
                    result=Result.READY_INVENTORY;
                    return result;
                }
                log.info("[BelowIceMountain] LEVEL_UP_SKILL_WIDGET skill={} group={} child={}",
                    skill,WidgetIndices.SkillsTab.GROUP_INDEX,index);
                if (Rs2Widget.clickWidget(WidgetIndices.SkillsTab.GROUP_INDEX,index)) {
                    actionAt=nowMillis; result=Result.WAITING_GUIDE_PROOF;
                } else result=Result.READY_INVENTORY;
                return result;
            }
            if (result == Result.READY_CLOSE) {
                int close=Rs2Widget.isWidgetVisible(InterfaceID.SkillGuideV2.FRAME)
                    ? InterfaceID.SkillGuideV2.CLOSE : InterfaceID.SkillGuide.CLOSE;
                if (!guideOpen()) result=Result.READY_INVENTORY;
                else if (Rs2Widget.clickWidget(close)) {
                    actionAt=nowMillis; result=Result.WAITING_CLOSE_PROOF;
                } else result=Result.REJECTED;
                return result;
            }
            if (result == Result.READY_INVENTORY) {
                if (Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY))
                    result=guideSeen?Result.PROVED:Result.REJECTED;
                else if (Rs2Tab.switchTo(InterfaceTab.INVENTORY)) {
                    actionAt=nowMillis; result=Result.WAITING_INVENTORY_PROOF;
                } else result=Result.REJECTED;
            }
            return result;
        }

        private static boolean guideOpen() {
            return Rs2Widget.isWidgetVisible(InterfaceID.SkillGuideV2.FRAME)
                || Rs2Widget.isWidgetVisible(InterfaceID.SkillGuide.WINDOW);
        }
        private static int skillIndex(Skill skill) {
            if (skill==null) return -1;
            switch (skill) {
                case ATTACK: return WidgetIndices.SkillsTab.ATTACK_CONTAINER;
                case STRENGTH: return WidgetIndices.SkillsTab.STRENGTH_CONTAINER;
                case DEFENCE: return WidgetIndices.SkillsTab.DEFENCE_CONTAINER;
                case RANGED: return WidgetIndices.SkillsTab.RANGED_CONTAINER;
                case PRAYER: return WidgetIndices.SkillsTab.PRAYER_CONTAINER;
                case MAGIC: return WidgetIndices.SkillsTab.MAGIC_CONTAINER;
                case RUNECRAFT: return WidgetIndices.SkillsTab.RUNECRAFT_CONTAINER;
                case CONSTRUCTION: return WidgetIndices.SkillsTab.CONSTRUCTION_CONTAINER;
                case HITPOINTS: return WidgetIndices.SkillsTab.HITPOINTS_CONTAINER;
                case AGILITY: return WidgetIndices.SkillsTab.AGILITY_CONTAINER;
                case HERBLORE: return WidgetIndices.SkillsTab.HERBLORE_CONTAINER;
                case THIEVING: return WidgetIndices.SkillsTab.THIEVING_CONTAINER;
                case CRAFTING: return WidgetIndices.SkillsTab.CRAFTING_CONTAINER;
                case FLETCHING: return WidgetIndices.SkillsTab.FLETCHING_CONTAINER;
                case SLAYER: return WidgetIndices.SkillsTab.SLAYER_CONTAINER;
                case HUNTER: return WidgetIndices.SkillsTab.HUNTER_CONTAINER;
                case MINING: return WidgetIndices.SkillsTab.MINING_CONTAINER;
                case SMITHING: return WidgetIndices.SkillsTab.SMITHING_CONTAINER;
                case FISHING: return WidgetIndices.SkillsTab.FISHING_CONTAINER;
                case COOKING: return WidgetIndices.SkillsTab.COOKING_CONTAINER;
                case FIREMAKING: return WidgetIndices.SkillsTab.FIREMAKING_CONTAINER;
                case WOODCUTTING: return WidgetIndices.SkillsTab.WOODCUTTING_CONTAINER;
                case FARMING: return WidgetIndices.SkillsTab.FARMING_CONTAINER;
                default: return -1;
            }
        }

        Result result() { return result; }
        Skill skill() { return skill; }
        int previousLevel() { return previousLevel; }
        int newLevel() { return newLevel; }
        boolean guideSeen() { return guideSeen; }
        boolean baselineReady() { return baselineReady; }
        void seedFromClient(Client client) {
            if (baselineReady) return;
            levels.clear();
            for (Skill observed:Skill.values())
                levels.put(observed,client.getRealSkillLevel(observed));
            baselineReady=true;
        }
        void noteVerifiedMissedIncrease(Skill changedSkill,int previous,int current,long nowMillis) {
            if (current<=previous) return;
            levels.put(changedSkill,previous);
            onStatChanged(changedSkill,current,nowMillis);
        }
        Map<String,Object> snapshot() {
            Map<String,Object> state=new HashMap<>();
            state.put("levels",new EnumMap<>(levels));
            state.put("baselineReady",baselineReady);
            state.put("result",result.name());
            state.put("skill",skill==null?"":skill.name());
            state.put("previousLevel",previousLevel);
            state.put("newLevel",newLevel);
            state.put("queuedAt",queuedAt);
            state.put("actionAt",actionAt);
            state.put("guideSeen",guideSeen);
            List<Map<String,Object>> waiting=new ArrayList<>();
            for (SkillIncrease increase:increases) {
                Map<String,Object> saved=new HashMap<>();
                saved.put("skill",increase.skill.name());
                saved.put("previous",increase.previous);
                saved.put("current",increase.current);
                saved.put("at",increase.at);
                waiting.add(saved);
            }
            state.put("increases",waiting);
            return state;
        }
        void restore(Object saved) {
            if (!(saved instanceof Map)) return;
            Map<?,?> state=(Map<?,?>)saved;
            levels.clear();
            Object priorLevels=state.get("levels");
            if (priorLevels instanceof Map) {
                for (Map.Entry<?,?> entry:((Map<?,?>)priorLevels).entrySet())
                    if (entry.getKey() instanceof Skill && entry.getValue() instanceof Number)
                        levels.put((Skill)entry.getKey(),((Number)entry.getValue()).intValue());
            }
            baselineReady=Boolean.TRUE.equals(state.get("baselineReady"));
            Object priorResult=state.get("result");
            try { result=Result.valueOf(String.valueOf(priorResult)); }
            catch (Exception ex) { result=Result.IDLE; }
            Object priorSkill=state.get("skill");
            try { skill=Skill.valueOf(String.valueOf(priorSkill)); }
            catch (Exception ex) { skill=null; }
            Object priorLevel=state.get("previousLevel");
            previousLevel=priorLevel instanceof Number ? ((Number)priorLevel).intValue() : 0;
            Object currentLevel=state.get("newLevel");
            newLevel=currentLevel instanceof Number ? ((Number)currentLevel).intValue() : 0;
            Object priorQueuedAt=state.get("queuedAt");
            queuedAt=priorQueuedAt instanceof Number
                ? ((Number)priorQueuedAt).longValue() : 0;
            Object priorActionAt=state.get("actionAt");
            actionAt=priorActionAt instanceof Number ? ((Number)priorActionAt).longValue() : 0;
            guideSeen=Boolean.TRUE.equals(state.get("guideSeen"));
            increases.clear();
            Object waiting=state.get("increases");
            if (waiting instanceof List) for (Object item:(List<?>)waiting) {
                if (!(item instanceof Map)) continue;
                Map<?,?> entryState=(Map<?,?>)item;
                try {
                    Skill queuedSkill=Skill.valueOf(String.valueOf(entryState.get("skill")));
                    if (entryState.get("previous") instanceof Number
                        && entryState.get("current") instanceof Number
                        && entryState.get("at") instanceof Number)
                        increases.addLast(new SkillIncrease(queuedSkill,
                            ((Number)entryState.get("previous")).intValue(),
                            ((Number)entryState.get("current")).intValue(),
                            ((Number)entryState.get("at")).longValue()));
                } catch (Exception ignored) { }
            }
            // Build47 proved only that Skills opened; re-acknowledge its last skill once.
            if (!state.containsKey("guideSeen") && result==Result.PROVED && skill!=null)
                start(new SkillIncrease(skill,previousLevel,newLevel,queuedAt));
        }
        void clearSession() {
            levels.clear(); increases.clear(); baselineReady=false;
            result=Result.IDLE; skill=null; previousLevel=0; newLevel=0;
            queuedAt=0; actionAt=0; guideSeen=false;
        }
    }
    private static final class LevelEvent {
        final Skill skill;
        final int level;
        final long at;
        LevelEvent(Skill skill, int level, long at) {
            this.skill=skill; this.level=level; this.at=at;
        }
    }

    private BelowIceMountainConfig config;
    private BooleanSupplier ownsInput;
    private volatile boolean stopped;
    private String classHash = "UNKNOWN", stage = "START", error = "";
    private Pending pending;
    private Route route, cancellingRoute;
    private Thread routeClear;
    private long routeClearAt, nextAt, disconnectAt, loginAt, welcomeAt;
    private int disconnectAttempts, loginAttempts, welcomeAttempts, selectedWorld;
    private final Map<String,Integer> failures = new HashMap<>();
    private boolean bankChecked;
    private boolean supplyBankChecked;
    private int supplyActions, beefBought, flourBought;
    private String missingSupplyScene="";
    private boolean doughWidgetDumped;
    private boolean safetyLogoutIssued, dungeonProbeLogged;
    private WorldPoint stage30RecoveryMoveFrom;
    private long stage30RecoveryMoveAt, stage30RecoveryExitAt;
    private int stage30RecoveryMoveFailures;
    private boolean stage30WillowAttempted;
    private long stage30WillowAt;
    private long dialogueControlMissingAt;
    private String dungeonProbeScene="";
    private String prepBankAudit = "NOT_RUN";
    private String prepFoodStage = "QUOTE";
    private int prepFoodId, prepFoodQuote, prepFoodQuantity, prepFoodInitialCount, prepFoodInitialCoins;
    private boolean prepGePlaceAttempted;
    private long prepFoodOfferAt;
    private QuestGeBuyer prepNativeBuyer;
    private String prepNativeCheckpoint = "";
    private boolean prepNativeStarted;
    private volatile boolean prepNativeTickActive;
    private boolean trainingStarted, trainingRetreat;
    private boolean trainingFarmApproachComplete, trainingInteriorRepositionAttempted;
    private int trainingXpActions, trainingTargetIndex=-1, trainingAvoidIndex=-1, trainingUnproved;
    private long trainingEncounterAt, trainingAvoidUntil, trainingThreatUntil;
    private int trainingLastHp=-1;
    private boolean trainingSawChickenCombat;
    private long trainingLastCombatEndAt, trainingLastTargetGapMs=-1;
    private int randomDismissId=-1, randomDismissIndex=-1, randomDismissAttempts;
    private long randomDismissAt;
    private boolean randomDismissAccepted;
    private enum TrainingStylePhase { NEED_UNEQUIP, UNEQUIP_SENT, NEED_TAB,
        TAB_SENT, NEED_STYLE, STYLE_SENT, READY_PROBE, PROBE_SENT, DONE }
    private TrainingStylePhase trainingStylePhase=TrainingStylePhase.NEED_UNEQUIP;
    private int trainingUnequipBefore=-1, trainingStyleIndex=-1, trainingStyleCategory=-1;
    private int trainingProbeHpXp=-1, trainingProbeStrengthXp=-1, trainingProbeAttackXp=-1;
    private long trainingStyleAt;
    private boolean trainingStyleRestoreInvalid;
    private final LevelUpTabCue levelUpTabCue = new LevelUpTabCue();
    private final ConcurrentLinkedQueue<LevelEvent> levelEvents = new ConcurrentLinkedQueue<>();
    private volatile boolean acceptLevelEvents;
    private EventBus.Subscriber levelUpSubscriber;
    private LevelUpTabCue.Result lastLevelUpResult = LevelUpTabCue.Result.IDLE;
    private boolean legacyHpCatchupPending;
    private int legacyHpCatchupBaseline;
    private WorldPoint cueLastPosition;
    private int cueLastVarp = -1;
    private long cueSceneStableSince;
    private long safetyLogoutAt;
    private long dungeonProbeAt;
    private enum GuardianAction { EAT, EXIT, MINE }
    private static final class GuardianPending {
        final GuardianAction action;
        final WorldPoint tile;
        final int item, itemCount, hp, miningXp, guardianRatio, questStage;
        final long at=System.currentTimeMillis();
        GuardianPending(GuardianAction action,WorldPoint tile,int item,Frame f,int ratio) {
            this.action=action; this.tile=tile; this.item=item;
            itemCount=item==0?0:f.count(item); hp=f.hp; miningXp=f.miningXp;
            guardianRatio=ratio; questStage=questStage(f.varp);
        }
    }
    private static final class GuardianScene {
        final Map<WorldPoint,Rs2TileObjectModel> pillars=new HashMap<>();
        final Set<WorldPoint> broken=new java.util.HashSet<>();
        Rs2TileObjectModel exit;
        int guardianRatio=-1;
        boolean guardianPresent;
    }
    private GuardianPending guardianPending;
    private final Set<WorldPoint> guardianProvedPillars=new java.util.HashSet<>();
    private final Set<WorldPoint> guardianUncorroboratedPillars=new java.util.HashSet<>();
    private final Map<WorldPoint,Integer> guardianPillarFailures=new HashMap<>();
    private final Set<WorldPoint> guardianBlockedApproaches=new java.util.HashSet<>();
    private boolean guardianActive, guardianRetreat, guardianExitFailed, guardianPostBossExitReset;
    private int guardianExitFailures, guardianLogoutAttempts, guardianEatUnproved;
    private WorldPoint guardianMoveTarget, guardianMoveOrigin;
    private boolean guardianMoveForExit;
    private boolean guardianWalkPending;
    private long guardianMoveAt, guardianActiveAt, guardianAllPillarsAt;
    private int guardianEntryRegion=-1;
    private enum FinalCutscenePhase { APPROACH, VERIFY_ENTRANCE, CHOOSE_YES,
        VERIFY_CONTINUE, VERIFY_YES, WAIT_FINISHED }
    private FinalCutscenePhase finalCutscenePhase=FinalCutscenePhase.APPROACH;
    private boolean stage40ExitProved;
    private Frame finalDialogueBefore;
    private long finalCutsceneAt;
    private long finalYesAt;
    private int finalContinueClicks, finalYesClicks;
    private boolean guardianExposureReported;
    private final Set<WorldPoint> guardianObservedPillars=new java.util.HashSet<>();
    private long missingSupplySince;
    private int flexScrolls;
    private String laterMilestone="";
    private long laterMilestoneAt, entranceAt;
    private final Map<String,Integer> laterActionCounts=new HashMap<>();

    public int runtimeBuild() { return BUILD_NUMBER; }
    public void restoreReloadState(Map<String,Object> state) {
        levelUpTabCue.restore(state==null?null:state.get("levelUpTabCue"));
        lastLevelUpResult=levelUpTabCue.result();
        bankChecked = state != null && Boolean.TRUE.equals(state.get("bankChecked"));
        supplyBankChecked = state != null && Boolean.TRUE.equals(state.get("supplyBankChecked"));
        supplyActions = state != null && state.get("supplyActions") instanceof Number
            ? ((Number)state.get("supplyActions")).intValue() : 0;
        beefBought = state != null && state.get("beefBought") instanceof Number
            ? ((Number)state.get("beefBought")).intValue() : 0;
        flourBought = state != null && state.get("flourBought") instanceof Number
            ? ((Number)state.get("flourBought")).intValue() : 0;
        prepBankAudit = state != null && state.get("prepBankAudit") instanceof String
            ? (String)state.get("prepBankAudit") : "NOT_RUN";
        prepFoodStage = state != null && state.get("prepFoodStage") instanceof String
            ? (String)state.get("prepFoodStage") : "QUOTE";
        prepFoodId = state != null && state.get("prepFoodId") instanceof Number
            ? ((Number)state.get("prepFoodId")).intValue() : 0;
        prepFoodQuote = state != null && state.get("prepFoodQuote") instanceof Number
            ? ((Number)state.get("prepFoodQuote")).intValue() : 0;
        prepFoodQuantity = state != null && state.get("prepFoodQuantity") instanceof Number
            ? ((Number)state.get("prepFoodQuantity")).intValue() : 0;
        prepFoodInitialCount = state != null && state.get("prepFoodInitialCount") instanceof Number
            ? ((Number)state.get("prepFoodInitialCount")).intValue() : 0;
        prepFoodInitialCoins = state != null && state.get("prepFoodInitialCoins") instanceof Number
            ? ((Number)state.get("prepFoodInitialCoins")).intValue() : 0;
        prepFoodOfferAt = state != null && state.get("prepFoodOfferAt") instanceof Number
            ? ((Number)state.get("prepFoodOfferAt")).longValue() : 0;
        prepGePlaceAttempted = state != null && Boolean.TRUE.equals(state.get("prepGePlaceAttempted"));
        prepNativeCheckpoint = state != null && state.get("prepNativeCheckpoint") instanceof String
            ? (String)state.get("prepNativeCheckpoint") : "";
        prepNativeStarted = (state != null && Boolean.TRUE.equals(state.get("prepNativeStarted")))
            || !prepNativeCheckpoint.isBlank();
        trainingStarted = state != null && Boolean.TRUE.equals(state.get("trainingStarted"));
        trainingFarmApproachComplete = state != null
            ? Boolean.TRUE.equals(state.get("trainingFarmApproachComplete")) || trainingStarted
            : false;
        trainingInteriorRepositionAttempted = state != null
            && Boolean.TRUE.equals(state.get("trainingInteriorRepositionAttempted"));
        dungeonProbeLogged = state != null && Boolean.TRUE.equals(state.get("dungeonProbeLogged"));
        dungeonProbeScene = state != null && state.get("dungeonProbeScene") instanceof String
            ? (String)state.get("dungeonProbeScene") : "";
        safetyLogoutIssued = state != null && Boolean.TRUE.equals(state.get("safetyLogoutIssued"));
        safetyLogoutAt = state != null && state.get("safetyLogoutAt") instanceof Number
            ? ((Number)state.get("safetyLogoutAt")).longValue() : 0;
        entranceAt = state != null && state.get("entranceAt") instanceof Number
            ? ((Number)state.get("entranceAt")).longValue() : 0;
        stage30WillowAttempted = state != null
            && Boolean.TRUE.equals(state.get("stage30WillowAttempted"));
        stage30WillowAt = state != null && state.get("stage30WillowAt") instanceof Number
            ? ((Number)state.get("stage30WillowAt")).longValue() : 0;
        legacyHpCatchupPending = state != null
            && (Boolean.TRUE.equals(state.get("legacyHpCatchupPending"))
                || shouldCatchUpBuild46Hp(state,trainingStarted));
        legacyHpCatchupBaseline = state != null && state.get("legacyHpCatchupBaseline") instanceof Number
            ? ((Number)state.get("legacyHpCatchupBaseline")).intValue()
            : legacyHpCatchupPending ? 13 : 0;
        trainingRetreat = state != null && Boolean.TRUE.equals(state.get("trainingRetreat"));
        trainingLastHp = state != null && state.get("trainingLastHp") instanceof Number
            ? ((Number)state.get("trainingLastHp")).intValue() : -1;
        trainingThreatUntil = state != null && state.get("trainingThreatUntil") instanceof Number
            ? ((Number)state.get("trainingThreatUntil")).longValue() : 0;
        trainingSawChickenCombat = state != null && Boolean.TRUE.equals(state.get("trainingSawChickenCombat"));
        trainingLastCombatEndAt = state != null && state.get("trainingLastCombatEndAt") instanceof Number
            ? ((Number)state.get("trainingLastCombatEndAt")).longValue() : 0;
        trainingLastTargetGapMs = state != null && state.get("trainingLastTargetGapMs") instanceof Number
            ? ((Number)state.get("trainingLastTargetGapMs")).longValue() : -1;
        randomDismissId = state != null && state.get("randomDismissId") instanceof Number
            ? ((Number)state.get("randomDismissId")).intValue() : -1;
        randomDismissIndex = state != null && state.get("randomDismissIndex") instanceof Number
            ? ((Number)state.get("randomDismissIndex")).intValue() : -1;
        randomDismissAttempts = state != null && state.get("randomDismissAttempts") instanceof Number
            ? ((Number)state.get("randomDismissAttempts")).intValue() : 0;
        randomDismissAt = state != null && state.get("randomDismissAt") instanceof Number
            ? ((Number)state.get("randomDismissAt")).longValue() : 0;
        randomDismissAccepted = state != null && Boolean.TRUE.equals(state.get("randomDismissAccepted"));
        trainingXpActions = state != null && state.get("trainingXpActions") instanceof Number
            ? ((Number)state.get("trainingXpActions")).intValue() : 0;
        trainingEncounterAt = state != null && state.get("trainingEncounterAt") instanceof Number
            ? ((Number)state.get("trainingEncounterAt")).longValue() : 0;
        trainingTargetIndex = state != null && state.get("trainingTargetIndex") instanceof Number
            ? ((Number)state.get("trainingTargetIndex")).intValue() : -1;
        trainingAvoidIndex = state != null && state.get("trainingAvoidIndex") instanceof Number
            ? ((Number)state.get("trainingAvoidIndex")).intValue() : -1;
        trainingAvoidUntil = state != null && state.get("trainingAvoidUntil") instanceof Number
            ? ((Number)state.get("trainingAvoidUntil")).longValue() : 0;
        trainingUnproved = state != null && state.get("trainingUnproved") instanceof Number
            ? ((Number)state.get("trainingUnproved")).intValue() : 0;
        if (state!=null && state.containsKey("trainingStylePhase")) {
            try { trainingStylePhase=TrainingStylePhase.valueOf(
                String.valueOf(state.get("trainingStylePhase"))); }
            catch (IllegalArgumentException ex) { trainingStyleRestoreInvalid=true; }
        }
        trainingUnequipBefore = state!=null && state.get("trainingUnequipBefore") instanceof Number
            ? ((Number)state.get("trainingUnequipBefore")).intValue() : -1;
        trainingStyleIndex = state!=null && state.get("trainingStyleIndex") instanceof Number
            ? ((Number)state.get("trainingStyleIndex")).intValue() : -1;
        trainingStyleCategory = state!=null && state.get("trainingStyleCategory") instanceof Number
            ? ((Number)state.get("trainingStyleCategory")).intValue() : -1;
        trainingProbeHpXp = state!=null && state.get("trainingProbeHpXp") instanceof Number
            ? ((Number)state.get("trainingProbeHpXp")).intValue() : -1;
        trainingProbeStrengthXp = state!=null && state.get("trainingProbeStrengthXp") instanceof Number
            ? ((Number)state.get("trainingProbeStrengthXp")).intValue() : -1;
        trainingProbeAttackXp = state!=null && state.get("trainingProbeAttackXp") instanceof Number
            ? ((Number)state.get("trainingProbeAttackXp")).intValue() : -1;
        trainingStyleAt = state!=null && state.get("trainingStyleAt") instanceof Number
            ? ((Number)state.get("trainingStyleAt")).longValue() : 0;
    }
    /** One-time catch-up: Build46 baselined HP14, but the prior saved checkpoint was HP13. */
    private static boolean shouldCatchUpBuild46Hp(Map<String,Object> state,boolean trainingStarted) {
        if (state==null || !trainingStarted || state.containsKey("legacyHpCatchupPending")
            || !(state.get("trainingXpActions") instanceof Number)
            || ((Number)state.get("trainingXpActions")).intValue()<=0
            || !(state.get("levelUpTabCue") instanceof Map)) return false;
        Map<?,?> cue=(Map<?,?>)state.get("levelUpTabCue");
        if (!Boolean.TRUE.equals(cue.get("baselineReady"))
            || !"IDLE".equals(cue.get("result")) || !(cue.get("levels") instanceof Map)) return false;
        Object hp=((Map<?,?>)cue.get("levels")).get(Skill.HITPOINTS);
        return hp instanceof Number && ((Number)hp).intValue()>=14;
    }
    public synchronized Map<String,Object> quiesceForReload() {
        if (guardianActive || stage40ExitProved)
            throw new IllegalStateException("guardian/final cutscene active; no hot reload");
        if (pending != null || route != null || cancellingRoute != null || prepNativeTickActive)
            throw new IllegalStateException("action or route in progress");
        acceptLevelEvents=false;
        if (levelUpSubscriber != null) {
            Microbot.getEventBus().unregister(levelUpSubscriber);
            levelUpSubscriber=null;
        }
        synchronized (levelEvents) {
            while (!levelEvents.isEmpty()) drainLevelEvents();
        }
        Map<String,Object> state = new HashMap<>();
        state.put("levelUpTabCue",levelUpTabCue.snapshot());
        state.put("bankChecked",bankChecked);
        state.put("supplyBankChecked",supplyBankChecked);
        state.put("supplyActions",supplyActions);
        state.put("beefBought",beefBought);
        state.put("flourBought",flourBought);
        state.put("prepBankAudit",prepBankAudit);
        state.put("prepFoodStage",prepFoodStage);
        state.put("prepFoodId",prepFoodId);
        state.put("prepFoodQuote",prepFoodQuote);
        state.put("prepFoodQuantity",prepFoodQuantity);
        state.put("prepFoodInitialCount",prepFoodInitialCount);
        state.put("prepFoodInitialCoins",prepFoodInitialCoins);
        state.put("prepFoodOfferAt",prepFoodOfferAt);
        state.put("prepGePlaceAttempted",prepGePlaceAttempted);
        state.put("prepNativeCheckpoint",prepNativeCheckpoint);
        state.put("prepNativeStarted",prepNativeStarted);
        state.put("trainingStarted",trainingStarted);
        state.put("trainingFarmApproachComplete",trainingFarmApproachComplete);
        state.put("trainingInteriorRepositionAttempted",trainingInteriorRepositionAttempted);
        state.put("legacyHpCatchupPending",legacyHpCatchupPending);
        state.put("legacyHpCatchupBaseline",legacyHpCatchupBaseline);
        state.put("trainingRetreat",trainingRetreat);
        state.put("trainingLastHp",trainingLastHp);
        state.put("trainingThreatUntil",trainingThreatUntil);
        state.put("trainingSawChickenCombat",trainingSawChickenCombat);
        state.put("trainingLastCombatEndAt",trainingLastCombatEndAt);
        state.put("trainingLastTargetGapMs",trainingLastTargetGapMs);
        state.put("randomDismissId",randomDismissId);
        state.put("randomDismissIndex",randomDismissIndex);
        state.put("randomDismissAttempts",randomDismissAttempts);
        state.put("randomDismissAt",randomDismissAt);
        state.put("randomDismissAccepted",randomDismissAccepted);
        state.put("trainingXpActions",trainingXpActions);
        state.put("trainingEncounterAt",trainingEncounterAt);
        state.put("trainingTargetIndex",trainingTargetIndex);
        state.put("trainingAvoidIndex",trainingAvoidIndex);
        state.put("trainingAvoidUntil",trainingAvoidUntil);
        state.put("trainingUnproved",trainingUnproved);
        state.put("trainingStylePhase",trainingStylePhase.name());
        state.put("trainingUnequipBefore",trainingUnequipBefore);
        state.put("trainingStyleIndex",trainingStyleIndex);
        state.put("trainingStyleCategory",trainingStyleCategory);
        state.put("trainingProbeHpXp",trainingProbeHpXp);
        state.put("trainingProbeStrengthXp",trainingProbeStrengthXp);
        state.put("trainingProbeAttackXp",trainingProbeAttackXp);
        state.put("trainingStyleAt",trainingStyleAt);
        state.put("dungeonProbeLogged",dungeonProbeLogged);
        state.put("dungeonProbeScene",dungeonProbeScene);
        state.put("safetyLogoutIssued",safetyLogoutIssued);
        state.put("safetyLogoutAt",safetyLogoutAt);
        state.put("entranceAt",entranceAt);
        state.put("stage30WillowAttempted",stage30WillowAttempted);
        state.put("stage30WillowAt",stage30WillowAt);
        shutdown();
        return state;
    }
    public boolean run(BelowIceMountainConfig config, BooleanSupplier ownsInput) {
        if (isRunning()) return true;
        this.config = config;
        this.ownsInput = ownsInput;
        this.stopped = false;
        this.stage = "START";
        this.error = "";
        this.classHash = classSha();
        log.info("[BelowIceMountain] RUNNING_BUILD={} classSha256={} guardianMode=supervised-only unattended=false pid={}",
            BUILD_NUMBER, classHash, ProcessHandle.current().pid());
        // Register on this script instance so hot reload can remove the exact subscriber.
        acceptLevelEvents=levelUpTabCue.baselineReady();
        levelUpSubscriber = Microbot.getEventBus().register(StatChanged.class, event -> {
            if (!stopped && event != null && event.getSkill() != null
                && Microbot.getClient() != null
                && Microbot.getClient().getGameState() == GameState.LOGGED_IN) {
                synchronized (levelEvents) {
                    if (acceptLevelEvents)
                        levelEvents.add(new LevelEvent(event.getSkill(),event.getLevel(),
                            System.currentTimeMillis()));
                }
            }
        }, 0f);
        // The same safe action/proof guards apply; a short observation interval
        // removes idle time between a chicken's death and the next target click.
        int requested=config.tickDelay();
        int delay=requested==650?250:Math.max(250,Math.min(2000,requested));
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::tick,
            0, delay, TimeUnit.MILLISECONDS);
        return true;
    }
    @Override public void shutdown() {
        stopped = true;
        acceptLevelEvents=false;
        if (levelUpSubscriber != null) {
            Microbot.getEventBus().unregister(levelUpSubscriber);
            levelUpSubscriber=null;
        }
        synchronized (levelEvents) { levelEvents.clear(); }
        cancelRoute();
        if (mainScheduledFuture != null) mainScheduledFuture.cancel(true);
        scheduledExecutorService.shutdownNow();
        super.shutdown();
        try { Files.deleteIfExists(STATUS); }
        catch (Exception ex) { log.warn("status cleanup: {}", ex.toString()); }
    }
    private synchronized void tick() {
        if (stopped || Thread.currentThread().isInterrupted()) return;
        Frame f = null;
        try {
            f = observe();
            if (!"LOGGED_IN".equals(f.gameState)) {
                if (levelUpTabCue.result()==LevelUpTabCue.Result.QUEUED
                    || levelUpTabCue.result()==LevelUpTabCue.Result.WAITING_SAFE_POINT
                    || levelUpTabCue.result()==LevelUpTabCue.Result.WAITING_PROOF)
                    log.info("[BelowIceMountain] LEVEL_UP_TAB_CANCELLED_SESSION skill={} state={}",
                        levelUpTabCue.skill(),f.gameState);
                levelUpTabCue.clearSession();
                lastLevelUpResult=LevelUpTabCue.Result.IDLE;
                synchronized (levelEvents) {
                    acceptLevelEvents=false;
                    levelEvents.clear();
                }
            }
            if (cancellingRoute != null) {
                if ((cancellingRoute.worker != null && cancellingRoute.worker.isAlive())
                    || (routeClear != null && routeClear.isAlive())) {
                    stage="WAIT_ROUTE_STOP";
                    if (System.currentTimeMillis()-routeClearAt>60000) hold("Route did not stop");
                    return;
                }
                cancellingRoute=null; routeClear=null;
            }
            if (!error.isEmpty()) { stage="HOLD"; cancelRoute(); return; }
            if ("FINISHED".equals(f.questState)) { cancelRoute(); pending=null; stage="QUEST_FINISHED"; return; }
            if ("LOGGED_IN".equals(f.gameState)) {
                if (!f.inventoryLoaded) { stage="WAIT_INVENTORY"; return; }
                if ("UNKNOWN".equals(f.questState) || f.varp<0) { hold("Unknown quest state/varp"); return; }
                if (f.questPoints<16 && "NOT_STARTED".equals(f.questState)) {
                    hold("Below Ice Mountain requires 16 quest points; observed "+f.questPoints); return;
                }
                if (f.mining<10) { hold("Mining 10 required for safe pillar strategy; observed "+f.mining); return; }
                if (f.hp<=0 || f.maxHp<=0) { hold("Death or invalid HP "+f.hp+"/"+f.maxHp); return; }
                if (LoginManager.isMemberWorld(f.world)) { hold("F2P quest on members world "+f.world); return; }
            }
            if (!armed()) {
                if ((ownsInput == null || ownsInput.getAsBoolean())
                    && !Microbot.pauseAllScripts.get() && !InputArbiter.isHuman()
                    && tickLevelUpCue(f)) return;
                cancelRoute(); pending=null; stage="PREFLIGHT_ACTIONS_DISABLED"; return;
            }
            if (ownsInput!=null && !ownsInput.getAsBoolean()) { cancelRoute(); stage="WAIT_EXCLUSIVE"; return; }
            if (safetyLogoutIssued) {
                if (stage30CaveRecoveryAllowed()) {
                    safetyLogoutIssued=false;
                    log.warn("[BelowIceMountain] STAGE30_CAVE_RECOVERY_LOGIN_ENABLED pid={}",
                        ProcessHandle.current().pid());
                } else {
                if (!"LOGGED_IN".equals(f.gameState)) {
                    hold("Safety logout proved from dungeon probe; review scene before re-entry");
                    return;
                }
                if (System.currentTimeMillis()-safetyLogoutAt>5000) {
                    safetyLogoutIssued=false;
                    guardianLogoutAttempts++;
                    guardianRetreat=true;
                    if (!guardianActive || guardianLogoutAttempts>=2) {
                        hold("Dungeon logout failed after bounded attempt(s); hp="
                            +f.hp+"/"+f.maxHp+" pos="+f.position);
                        return;
                    }
                    stage="GUARDIAN_LOGOUT_FAILED_RESCAN";
                    log.error("[BelowIceMountain] GUARDIAN_LOGOUT_FAILED attempts={} hp={}/{} pos={}",
                        guardianLogoutAttempts,f.hp,f.maxHp,f.position);
                } else stage="VERIFY_SAFETY_LOGOUT";
                return;
                }
            }
            if (!"LOGGED_IN".equals(f.gameState)) { loginTick(f); return; }
            WelcomeScreenEvent welcome = new WelcomeScreenEvent();
            if (welcome.validate()) {
                cancelRoute(); stage="VERIFY_WELCOME_DISMISS";
                if (welcomeAttempts++ == 0) {
                    welcomeAt=System.currentTimeMillis();
                    welcome.execute();
                    log.info("[BelowIceMountain] WELCOME_DISMISS_DISPATCH");
                } else if (System.currentTimeMillis()-welcomeAt>12000) {
                    hold("Welcome screen persisted after native dismiss");
                }
                return;
            }
            welcomeAttempts=0;
            if (Microbot.pauseAllScripts.get() || InputArbiter.isHuman()) {
                cancelRoute(); stage="WAIT_INPUT"; return;
            }
            if (tickRandomEventDismiss(f)) return;
            int observedQuestStage=questStage(f.varp);
            if (stage30RecoveryExitAt>0 && observedQuestStage==30
                && overworldPrepArea(f.position)) {
                log.info("[BelowIceMountain] STAGE30_CAVE_EXIT_PROVED pos={} elapsedMs={}",
                    f.position,System.currentTimeMillis()-stage30RecoveryExitAt);
                stage30RecoveryExitAt=0;
                entranceAt=0;
                stage="STAGE30_CAVE_EXIT_PROVED";
                return;
            }
            if (observedQuestStage>=40 && stage40ExitProved) {
                stage40Cutscene(f);
                return;
            }
            boolean instancedCave=inKnownCaveInstance(f.position);
            if (observedQuestStage==30 && instancedCave && stage30ObservationAllowed()) {
                if (pending!=null) { verify(f); return; }
                if (System.currentTimeMillis()<nextAt) {
                    stage="WAIT_STAGE30_DIALOGUE_PACE";
                    return;
                }
                if (f.inDialogue || f.hasContinue || !f.options.isEmpty()) {
                    dialogue(f);
                    return;
                }
                GuardianScene observed=guardianScene(f);
                if (observed.guardianPresent || !f.unsafeAggressor.isEmpty()
                    || f.hp<f.maxHp) {
                    guardianRetreat=true;
                    guardianTick(f);
                } else {
                    guardianReadScene(f);
                    stage="STAGE30_CAVE_SCENE_OBSERVATION";
                }
                return;
            }
            if (observedQuestStage==30 && instancedCave && stage30CaveRecoveryAllowed()) {
                recoverStage30EmptyCave(f);
                return;
            }
            GuardianScene liveScene=null;
            if (observedQuestStage>=30 && !instancedCave && f.position!=null)
                liveScene=guardianScene(f);
            boolean sceneEvidence=liveScene!=null && (liveScene.guardianPresent
                || !liveScene.pillars.isEmpty() || !liveScene.broken.isEmpty()
                || liveScene.exit!=null);
            boolean unknownStageScene=observedQuestStage>=35
                && !overworldPrepArea(f.position) && !instancedCave;
            boolean guardianContext=instancedCave || sceneEvidence || unknownStageScene
                || guardianActive;
            // Stage 30 in the ordinary overworld is preparation, even if an
            // unrelated scene object resembles a dungeon clue. An already
            // active encounter still gets its exit/recovery path.
            boolean stage30Recovery=observedQuestStage==30 && guardianContext
                && (guardianActive || !overworldPrepArea(f.position));
            if (observedQuestStage>=35 || stage30Recovery) {
                if (guardianActive && f.position!=null && guardianEntryRegion>=0
                    && f.position.getRegionID()!=guardianEntryRegion
                    && overworldPrepArea(f.position) && f.unsafeAggressor.isEmpty()) {
                    guardianPending=null;
                    guardianActive=false;
                    if (observedQuestStage>=40) {
                        stage40ExitProved=true;
                        stage="STAGE40_EXIT_PROVED_APPROACH_ENTRANCE";
                        log.info("[BelowIceMountain] STAGE40_EXIT_PROVED pos={} entryRegion={}",
                            f.position,guardianEntryRegion);
                    } else hold("Guardian emergency exit proved; no automatic re-entry at "
                        +f.position);
                    return;
                }
                if (pending!=null) {
                    if (proved(pending,f)) verify(f);
                    else {
                        log.warn("[BelowIceMountain] DUNGEON_PROBE_UNPROVED_PENDING key={} stage={} pos={}",
                            pending.key,observedQuestStage,f.position);
                        pending=null;
                    }
                    return; // settle the entry action before a guardian action
                }
                if ((guardianActionsAllowed() || guardianActive) && guardianContext) {
                    if (unknownStageScene && !sceneEvidence)
                        log.warn("[BelowIceMountain] GUARDIAN_UNKNOWN_STAGE_SCENE stage={} pos={} hp={}",
                            observedQuestStage,f.position,f.hp);
                    guardianTick(f);
                } else dungeonProbe(f);
                return;
            }
            if (tickLevelUpCue(f)) return;
            // Phase 1 only: audit supplies before any Willow/entrance dialogue.
            int prepStep = questStage(f.varp);
            if (prepStep>=20 && prepStep<=30) {
                if (!overworldPrepArea(f.position)) {
                    hold("Prep audit cannot route from unmapped scene "+f.position
                        +" stage="+prepStep);
                    return;
                }
                if (f.maxHp<20 && prepNativeStarted
                    && "COMPLETE".equals(prepFoodStage)
                    && prepNativeCheckpoint.contains("|COMPLETE|")) {
                    trainChickens(f);
                } else if (pending!=null) {
                    verify(f);
                } else if (f.hp<f.maxHp && entryFoodCount(f)>0) {
                    // Travel damage is treated locally. Do not send a supplied
                    // character back across the map to the completed GE buyer.
                    if (route!=null) { cancelRoute(); stage="WAIT_PREENTRY_ROUTE_STOP"; return; }
                    for (int id:ENTRY_FOOD) if (f.count(id)>0) {
                        issue("preentry:heal",Proof.FOOD_HEAL,f,id,7000,
                            ()->Rs2Inventory.interact(id,"Eat"));
                        return;
                    }
                } else if (f.inDialogue || f.hasContinue || !f.options.isEmpty()) {
                    if (requiresDungeonPreflight(f) && !dungeonEntryAllowed(f))
                        hold("Dungeon dialogue blocked by preflight at "+f.position);
                    else dialogue(f);
                } else if (route!=null) {
                    walk(f,route.key,route.target,route.radius);
                } else if (f.pickaxes==0 || f.hp<f.maxHp || f.maxHp<20
                    || entryFoodCount(f)<8) {
                    if ("NOT_RUN".equals(prepBankAudit)) prepBankAudit(f);
                    else prepFood(f);
                } else if (!dungeonEntryAllowed(f)) {
                    stage="WAIT_DUNGEON_ENTRY_APPROVAL";
                } else if (prepStep==30) {
                    if (stage30WillowRetryAllowed()) retryStage30Willow(f);
                    else enterRuins(f);
                } else {
                    followWillow(f);
                }
                return;
            }
            // Willow's dialogue can enter the dungeon cutscene before enterRuins runs.
            if (requiresDungeonPreflight(f) && !dungeonEntryAllowed(f)) {
                hold("Unsafe Willow/entrance transition: hp="+f.hp+"/"+f.maxHp
                    +" food="+f.food+" pickaxe="+f.pickaxes
                    +" stage="+questStage(f.varp)+" pos="+f.position);
                return;
            }
            if (pending!=null) { verify(f); return; }
            if (f.inDialogue || f.hasContinue || !f.options.isEmpty()) { dialogue(f); return; }
            if (route!=null) { walk(f,route.key,route.target,route.radius); return; }
            if (System.currentTimeMillis()<nextAt) { stage="WAIT_PACE"; return; }
            if (f.position.getX()<1700 && f.position.getY()>4700 && f.position.getY()<4900) {
                leaveMisthalinIsland(f); return;
            }
            int questStage=questStage(f.varp);
            boolean questDungeon=questStage>=30 && f.position.getX()>=2900
                && f.position.getX()<=3050 && f.position.getY()>5000
                && f.position.getY()<6000 && f.position.getPlane()==0;
            if (!questDungeon && (f.position.getX()<2500 || f.position.getX()>3500
                || f.position.getY()<3000 || f.position.getY()>3800
                || f.position.getPlane()!=0)) {
                hold("Unmapped location for F2P route: "+f.position); return;
            }
            if (!bankChecked) { prepare(f); return; }
            if (f.bankOpen && !(questStage==15 && f.marley>=10
                && f.marley<=30 && needsSupplies(f) && !supplyBankChecked)) {
                issue("bank:close",Proof.BANK_TOGGLED,f,0,7000,Rs2Bank::closeBank); return;
            }
            if (questStage>=15 && questStage<=30 && !laterMilestone(f)) return;
            if (questStage==0 || questStage==5 || questStage==7) { startWillow(f); return; }
            if (questStage==10) { checkal(f); return; }
            if (questStage==15) { recruitCrew(f); return; }
            if (questStage==20 || questStage==25) { followWillow(f); return; }
            if (questStage==30) { enterRuins(f); return; }
            if (questStage>=35) { stage="HOLD_GUARDIAN_UNIMPLEMENTED_"+questStage; return; }
            hold("Unmapped BIM_MAIN stage "+questStage+" raw="+f.varp);
        } catch (Exception ex) {
            hold("Tick exception: "+ex.getClass().getSimpleName()+": "+ex.getMessage());
            log.error("[BelowIceMountain] observation failed", ex);
        } finally {
            writeStatus(f);
        }
    }
    /** Dismiss only a zero-combat NPC targeting this player with an actual Dismiss menu action. */
    private boolean tickRandomEventDismiss(Frame f) {
        if (randomDismissIndex>=0 && (f.randomEventIndex!=randomDismissIndex
            || f.randomEventId!=randomDismissId)) {
            log.info("[BelowIceMountain] {} id={} index={} attempts={}",
                randomDismissAccepted?"RANDOM_DISMISS_PROVED":"RANDOM_EVENT_LEFT",
                randomDismissId,randomDismissIndex,randomDismissAttempts);
            randomDismissId=-1; randomDismissIndex=-1; randomDismissAttempts=0;
            randomDismissAt=0; randomDismissAccepted=false;
        }
        if (f.randomEventIndex<0 || f.unsafeAggressor.length()>0
            || f.hp<=Math.max(6,f.maxHp/2) || f.inDialogue || f.hasContinue
            || !f.options.isEmpty() || pending!=null || route!=null
            || cancellingRoute!=null || !overworldPrepArea(f.position)) return false;
        if (randomDismissIndex!=f.randomEventIndex || randomDismissId!=f.randomEventId) {
            randomDismissIndex=f.randomEventIndex;
            randomDismissId=f.randomEventId;
            randomDismissAttempts=0;
            randomDismissAt=0;
            randomDismissAccepted=false;
        }
        long now=System.currentTimeMillis();
        if (randomDismissAt>0 && now-randomDismissAt<4000) {
            stage="VERIFY_RANDOM_DISMISS"; return true;
        }
        if (randomDismissAttempts>=2) {
            if (randomDismissAt>0 && now-randomDismissAt>=4000) {
                log.warn("[BelowIceMountain] RANDOM_DISMISS_UNPROVED id={} index={} attempts=2; continuing task",
                    randomDismissId,randomDismissIndex);
                randomDismissAt=0;
            }
            return false;
        }
        net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel event =
            Rs2Npc.getNpcByIndex(f.randomEventIndex);
        if (event==null || event.getId()!=f.randomEventId) return false;
        boolean accepted=Rs2Npc.interact(event,"Dismiss");
        randomDismissAccepted |= accepted;
        randomDismissAttempts++;
        randomDismissAt=now;
        stage="VERIFY_RANDOM_DISMISS";
        log.info("[BelowIceMountain] RANDOM_DISMISS_ATTEMPT name={} id={} index={} accepted={} attempt={}",
            f.randomEventName,f.randomEventId,f.randomEventIndex,accepted,randomDismissAttempts);
        if (randomDismissAttempts==2 && !accepted)
            log.warn("[BelowIceMountain] RANDOM_DISMISS_UNPROVED id={} index={} reason=interaction_rejected",
                randomDismissId,randomDismissIndex);
        return true;
    }
    private boolean tickLevelUpCue(Frame f) {
        long now = System.currentTimeMillis();
        // Drain immutable event values on the script thread. Never hold a lock
        // while Rs2Tab may invoke work on the client thread.
        drainLevelEvents();
        if (f.position == null || !f.position.equals(cueLastPosition)
            || f.varp != cueLastVarp) {
            cueLastPosition = f.position;
            cueLastVarp = f.varp;
            cueSceneStableSince = now;
        }
        boolean safe = "LOGGED_IN".equals(f.gameState)
            && f.position != null && overworldPrepArea(f.position)
            && now - cueSceneStableSince >= 500
            && pending == null && route == null && cancellingRoute == null
            && !prepNativeTickActive && !f.inDialogue && !f.hasContinue
            && f.options.isEmpty() && !f.bankOpen && !f.shopOpen
            && !f.geOpen && !f.production && f.interactingNpc.isEmpty()
            && f.unsafeAggressor.isEmpty() && f.hp > Math.max(6,f.maxHp/2)
            && !safetyLogoutIssued
            && welcomeAttempts == 0;
        LevelUpTabCue.Result before = levelUpTabCue.result();
        LevelUpTabCue.Result result = levelUpTabCue.tick(safe,now);
        if (result != lastLevelUpResult) {
            lastLevelUpResult=result;
            log.info("[BelowIceMountain] LEVEL_UP_TAB skill={} level={}->{} result={} safe={}",
                levelUpTabCue.skill(),levelUpTabCue.previousLevel(),levelUpTabCue.newLevel(),result,safe);
        }
        boolean engaged = result!=LevelUpTabCue.Result.IDLE
            && result!=LevelUpTabCue.Result.QUEUED
            && result!=LevelUpTabCue.Result.WAITING_SAFE_POINT
            && result!=LevelUpTabCue.Result.PROVED
            && result!=LevelUpTabCue.Result.REJECTED;
        if (result==LevelUpTabCue.Result.REJECTED
            && before!=LevelUpTabCue.Result.REJECTED)
            log.warn("[BelowIceMountain] LEVEL_UP_ACK_UNVERIFIED skill={} level={}->{} currentTab={}",
                levelUpTabCue.skill(),levelUpTabCue.previousLevel(),
                levelUpTabCue.newLevel(),Rs2Tab.getCurrentTab());
        // Reserve one action per tick until the skill guide and Inventory are verified.
        if (engaged || (before!=result
            && (result==LevelUpTabCue.Result.PROVED
                || result==LevelUpTabCue.Result.REJECTED))) {
            nextAt = Math.max(nextAt,System.currentTimeMillis()+350);
            stage="VERIFY_LEVEL_UP_ACK_"+result;
            return true;
        }
        return false;
    }
    private void drainLevelEvents() {
        for (int count=0; count<64; count++) {
            LevelEvent event=levelEvents.poll();
            if (event==null) break;
            levelUpTabCue.onStatChanged(event.skill,event.level,event.at);
        }
    }
    private Frame observe() {
        Frame result = Microbot.getClientThread().invoke(() -> {
            Frame f = new Frame();
            Client c = Microbot.getClient();
            if (c == null) return f;
            GameState state = c.getGameState();
            f.gameState = state == null ? "UNKNOWN" : state.name();
            f.loginIndex = c.getLoginIndex();
            f.canvasWidth = c.getCanvasWidth();
            f.world = c.getWorld();
            if (state != GameState.LOGGED_IN || c.getLocalPlayer() == null) return f;
            if (!levelUpTabCue.baselineReady()) {
                synchronized (levelEvents) {
                    if (!levelUpTabCue.baselineReady()) {
                        levelUpTabCue.seedFromClient(c);
                        acceptLevelEvents=true;
                        log.info("[BelowIceMountain] LEVEL_UP_BASELINE_READY hp={} mining={}",
                            c.getRealSkillLevel(Skill.HITPOINTS),
                            c.getRealSkillLevel(Skill.MINING));
                    }
                }
            }
            if (legacyHpCatchupPending) {
                synchronized (levelEvents) {
                    if (legacyHpCatchupPending) {
                        int current=c.getRealSkillLevel(Skill.HITPOINTS);
                        if (current>legacyHpCatchupBaseline) {
                            levelUpTabCue.noteVerifiedMissedIncrease(
                                Skill.HITPOINTS,legacyHpCatchupBaseline,current,
                                System.currentTimeMillis());
                            log.info("[BelowIceMountain] LEVEL_UP_CATCHUP_FROM_PRIOR_STATUS skill=HITPOINTS level={}->{}",
                                legacyHpCatchupBaseline,current);
                        }
                        legacyHpCatchupPending=false;
                    }
                }
            }
            f.position = c.getLocalPlayer().getWorldLocation();
            f.varp = c.getVarpValue(VarPlayerID.BIM_MAIN);
            f.questPoints = c.getVarpValue(VarPlayerID.QP);
            f.checkal = c.getVarbitValue(VarbitID.BIM_CHECKAL);
            f.marley = c.getVarbitValue(VarbitID.BIM_MARLEY);
            f.burntof = c.getVarbitValue(VarbitID.BIM_BURNTOF);
            f.mining = c.getRealSkillLevel(Skill.MINING);
            f.miningXp = c.getSkillExperience(Skill.MINING);
            f.attack = c.getRealSkillLevel(Skill.ATTACK);
            f.attackXp = c.getSkillExperience(Skill.ATTACK);
            f.strengthXp = c.getSkillExperience(Skill.STRENGTH);
            f.combatMode = c.getVarpValue(VarPlayerID.COM_MODE);
            f.weaponCategory = c.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
            f.hp = c.getBoostedSkillLevel(Skill.HITPOINTS);
            f.maxHp = c.getRealSkillLevel(Skill.HITPOINTS);
            f.hpXp = c.getSkillExperience(Skill.HITPOINTS);
            f.geOpen = Rs2Widget.isWidgetVisible(465,1);
            f.combatLevel = c.getLocalPlayer().getCombatLevel();
            Actor interacting=c.getLocalPlayer().getInteracting();
            if (interacting instanceof NPC) {
                String name=((NPC)interacting).getName();
                f.interactingNpc=name==null?"":name;
            }
            for (NPC npc:c.getNpcs()) {
                if (npc==null) continue;
                if (npc.getInteracting()!=c.getLocalPlayer()) continue;
                String name=npc.getName();
                net.runelite.api.NPCComposition composition=npc.getTransformedComposition();
                if (composition==null) composition=npc.getComposition();
                String[] actions=composition==null?null:composition.getActions();
                if (npc.getCombatLevel()==0 && actions!=null
                    && java.util.Arrays.stream(actions).anyMatch("Dismiss"::equalsIgnoreCase)) {
                    f.randomEventId=npc.getId();
                    f.randomEventIndex=npc.getIndex();
                    f.randomEventName=name==null?"":name;
                } else if (f.unsafeAggressor.isEmpty() && npc.getCombatLevel()>0
                    && name!=null && !"Chicken".equalsIgnoreCase(name)) {
                    f.unsafeAggressor=name;
                }
            }
            Widget productionPrimary=c.getWidget(270,14);
            Widget productionAlternate=c.getWidget(300,16);
            f.production=(productionPrimary!=null && !productionPrimary.isHidden())
                || (productionAlternate!=null && !productionAlternate.isHidden());
            Widget emotes=c.getWidget(216,1);
            if (emotes!=null) f.emoteScroll=emotes.getScrollY();
            QuestState quest = Quest.BELOW_ICE_MOUNTAIN.getState(c);
            f.questState = quest == null ? "UNKNOWN" : quest.name();
            ItemContainer inventory = c.getItemContainer(InventoryID.INVENTORY);
            ItemContainer equipment = c.getItemContainer(InventoryID.EQUIPMENT);
            f.inventoryLoaded = inventory != null;
            f.equipmentLoaded = equipment != null;
            if (inventory != null) {
                int used = 0;
                for (Item item : inventory.getItems()) {
                    if (item == null || item.getId() < 0) continue;
                    used++;
                    f.items.merge(item.getId(),item.getQuantity(),Integer::sum);
                    countItem(f, item);
                }
                f.inventorySlots = used;
            }
            if (equipment != null) {
                Item[] worn = equipment.getItems();
                int weaponSlot = EquipmentInventorySlot.WEAPON.getSlotIdx();
                if (weaponSlot >= 0 && weaponSlot < worn.length && worn[weaponSlot] != null)
                    f.weaponId = worn[weaponSlot].getId();
                for (Item item : worn)
                    if (item != null && item.getId() >= 0) countItem(f, item);
            }
            return f;
        });
        if ("LOGGED_IN".equals(result.gameState)) {
            result.bankOpen=Rs2Bank.isOpen();
            result.shopOpen=Rs2Shop.isOpen();
            result.inDialogue=Rs2Dialogue.isInDialogue();
            result.hasContinue=Rs2Dialogue.hasContinue();
            result.dialogue=Rs2Dialogue.getDialogueText();
            if (result.dialogue==null || result.dialogue.isBlank())
                result.dialogue=visiblePlayerDialogueText();
            for (Widget option:Rs2Dialogue.getDialogueOptions())
                if (option!=null && option.getText()!=null && !option.getText().trim().isEmpty())
                    result.options.add(option.getText());
        }
        return result;
    }
    private static void countItem(Frame f, Item item) {
        for (int id : PICKAXES) if (item.getId() == id) { f.pickaxes += item.getQuantity(); break; }
        for (int id : FOOD) if (item.getId() == id) { f.food += item.getQuantity(); break; }
    }
    private void loginTick(Frame f) {
        long now=System.currentTimeMillis();
        if (!"LOGIN_SCREEN".equals(f.gameState)) { stage="WAIT_LOGIN_SCREEN"; return; }
        if (f.loginIndex==24) {
            if (disconnectAttempts++==0 && f.canvasWidth>0) {
                Microbot.getClientThread().invoke(() -> {
                    Microbot.getMouse().click(365+(f.canvasWidth-804)/2,308); return true;
                });
                disconnectAt=now; stage="VERIFY_DISCONNECT_DISMISS";
            } else if (now-disconnectAt>8000) hold("Disconnected modal persisted");
            return;
        }
        if (disconnectAttempts>0) {
            disconnectAttempts=0; loginAttempts=0; stage="DISCONNECT_DISMISSED"; return;
        }
        if (f.loginIndex!=10 && f.loginIndex!=34) { stage="WAIT_LOGIN_INDEX_"+f.loginIndex; return; }
        if (selectedWorld==0) {
            selectedWorld=LoginManager.getRandomWorld(false);
            if (selectedWorld<=0 || LoginManager.isMemberWorld(selectedWorld)) {
                hold("No verified F2P world for login"); return;
            }
        }
        if (loginAttempts++==0) {
            loginAt=now; stage="VERIFY_NATIVE_LOGIN";
            log.info("[BelowIceMountain] NATIVE_LOGIN_DISPATCH world={} index={}",selectedWorld,f.loginIndex);
            if (!LoginManager.login(selectedWorld)) hold("Native LoginManager.login rejected");
        } else if (now-loginAt>20000) hold("Native login did not reach game");
    }
    private void leaveMisthalinIsland(Frame f) {
        stage="LEAVE_PREVIOUS_QUEST_ISLAND";
        if (walk(f,"ISLAND_BOAT",ISLAND_BOAT,6)) return;
        Rs2TileObjectModel boat=Microbot.getRs2TileObjectCache().query()
            .withId(ObjectID.MISTMYST_BOAT_ISLAND).within(ISLAND_BOAT,12).nearestOnClientThread();
        if (boat==null) {
            if (!Rs2Magic.canCast(Rs2Spells.LUMBRIDGE_HOME_TELEPORT)) {
                hold("Island boat absent and Lumbridge Home Teleport unavailable at "+f.position); return;
            }
            issue("home:exit-island",Proof.ISLAND_EXIT,f,0,90000,
                ()->Rs2Magic.cast(Rs2Spells.LUMBRIDGE_HOME_TELEPORT));
            return;
        }
        issue("board:island-boat",Proof.ISLAND_EXIT,f,0,18000,()->boat.click("Board"));
    }
    private void prepare(Frame f) {
        stage="PREPARE_BANK";
        if (walk(f,"FALADOR_BANK",FALADOR_BANK,5)) return;
        if (!f.bankOpen) {
            issue("bank:open",Proof.BANK_TOGGLED,f,0,10000,Rs2Bank::openBank); return;
        }
        int[] keep={1265,1267,1269,1273,1271,1275,12297,11920,
            ItemID.COOKED_MEAT,ItemID.BREAD,ItemID.KNIFE,ItemID.COINS,
            ItemID.RAW_BEEF,ItemID.POT_FLOUR,ItemID.BOWL_EMPTY,
            ItemID.BOWL_WATER,ItemID.BREAD_DOUGH,
            ItemID.BIM_STEAK_SANDWICH,ItemID.ASGARNIAN_ALE,
            315,329,333,339,361,373,379,385,391};
        int excess=0;
        for (int id:f.items.keySet()) if (!contains(keep,id)) excess++;
        if (excess>0) {
            Integer[] keepBoxed=java.util.Arrays.stream(keep).boxed().toArray(Integer[]::new);
            issue("bank:deposit-excess",Proof.INVENTORY_SHED,f,0,10000,
                ()->Rs2Bank.depositAllExcept(keepBoxed)); return;
        }
        if (f.pickaxes==0) for (int id:new int[]{1267,1265}) if (Rs2Bank.count(id)>0) {
            issue("bank:pickaxe:"+id,Proof.ITEM_GAINED,f,id,10000,
                ()->Rs2Bank.withdrawOne(id)); return;
        }
        for (int id:new int[]{ItemID.KNIFE,ItemID.BREAD,ItemID.COOKED_MEAT})
            if (f.count(id)==0 && Rs2Bank.count(id)>0) {
                issue("bank:item:"+id,Proof.ITEM_GAINED,f,id,10000,
                    ()->Rs2Bank.withdrawOne(id)); return;
            }
        if (f.count(ItemID.COINS)<3 && Rs2Bank.count(ItemID.COINS)>=3) {
            issue("bank:coins",Proof.ITEM_GAINED,f,ItemID.COINS,10000,
                ()->Rs2Bank.withdrawX(ItemID.COINS,Math.min(100,Rs2Bank.count(ItemID.COINS)))); return;
        }
        if (f.food<10) for (int id:new int[]{385,379,373,361,329,333})
            if (Rs2Bank.count(id)>0) {
                issue("bank:food:"+id,Proof.ITEM_GAINED,f,id,10000,
                    ()->Rs2Bank.withdrawX(id,Math.min(10-f.food,Rs2Bank.count(id)))); return;
            }
        bankChecked=true;
        issue("bank:close",Proof.BANK_TOGGLED,f,0,7000,Rs2Bank::closeBank);
    }
    private void startWillow(Frame f) {
        stage="START_WILLOW";
        if (walk(f,"WILLOW_START",WILLOW,4)) return;
        Rs2NpcModel willow=Microbot.getRs2NpcCache().query()
            .withId(NpcID.BIM_WILLOW).within(WILLOW,9).nearestOnClientThread();
        if (willow==null) { hold("Willow not visible at "+WILLOW); return; }
        issue("talk:willow-start",Proof.DIALOGUE_CHANGED,f,0,12000,()->willow.click("Talk-to"));
    }
    private void checkal(Frame f) {
        stage="RECRUIT_CHECKAL_"+f.checkal;
        if (f.pickaxes==0) {
            if (walk(f,"VILLAGE_PICKAXE",PICKAXE_SPAWN,10)) return;
            if (!Rs2GroundItem.exists(1265,15)) { hold("Bronze pickaxe spawn not visible in Barbarian Village spinning hut; bank empty"); return; }
            issue("loot:village-pickaxe",Proof.ITEM_GAINED,f,1265,10000,
                ()->Rs2GroundItem.loot(1265,15)); return;
        }
        if (f.checkal==0) {
            talkCheckal(f,"talk:checkal-first"); return;
        }
        if (f.checkal==5 || f.checkal==10) {
            if (walk(f,"ATLAS",ATLAS,4)) return;
            Rs2NpcModel atlas=Microbot.getRs2NpcCache().query()
                .withId(NpcID.BIM_ATLAS).within(ATLAS,9).nearestOnClientThread();
            if (atlas==null) { hold("Atlas not visible at "+ATLAS); return; }
            issue("talk:atlas",Proof.DIALOGUE_CHANGED,f,0,15000,()->atlas.click("Talk-to"));
            return;
        }
        if (f.checkal==15 || f.checkal==20) {
            if (walk(f,"CHECKAL_FLEX",CHECKAL,4)) return;
            if (!Rs2Tab.isCurrentTab(InterfaceTab.EMOTES)) {
                issue("tab:emotes",Proof.FLEX_OPEN,f,0,8000,Rs2Tab::switchToEmotesTab);
                return;
            }
            FlexView view=Microbot.getClientThread().invoke((Supplier<FlexView>)()->{
                Widget root=Microbot.getClient().getWidget(216,1);
                Widget flex=findSprite(root,2426);
                return root==null || flex==null ? null
                    : new FlexView(flex,root.getBounds(),flex.getBounds(),root.getScrollY());
            });
            if (view==null || view.panel==null || view.icon==null) {
                hold("Flex emote widget or panel absent"); return;
            }
            if (view.icon.y+view.icon.height>view.panel.y+view.panel.height
                || view.icon.y<view.panel.y) {
                if (flexScrolls++>=15) { hold("Flex emote not visible after 15 verified scrolls"); return; }
                log.info("[BelowIceMountain] FLEX_SCROLL panel={} icon={} scroll={}",
                    view.panel,view.icon,view.scroll);
                net.runelite.api.Point point=new net.runelite.api.Point(
                    view.panel.x+view.panel.width/2,view.panel.y+view.panel.height/2);
                issue("emote:scroll",Proof.EMOTE_SCROLLED,f,0,5000,
                    ()->{Microbot.getMouse().scrollDown(point); return true;});
                return;
            }
            flexScrolls=0;
            Widget flex=view.widget;
            issue("emote:flex",Proof.DIALOGUE_CHANGED,f,0,12000,
                ()->Microbot.getClientThread().invoke((Supplier<Boolean>)()->{
                    log.info("[BelowIceMountain] FLEX_WIDGET id={} index={} parent={} bounds={} actions={} sprite={}",
                        flex.getId(),flex.getIndex(),flex.getParentId(),flex.getBounds(),
                        java.util.Arrays.toString(flex.getActions()),flex.getSpriteId());
                    return Rs2Widget.clickWidget(flex);
                }));
            return;
        }
        if (f.checkal==40) { talkCheckal(f,"talk:checkal-finish"); return; }
        hold("Unmapped Checkal varbit "+f.checkal);
    }
    private void talkCheckal(Frame f,String key) {
        if (walk(f,key,CHECKAL,4)) return;
        Rs2NpcModel npc=Microbot.getRs2NpcCache().query()
            .withId(NpcID.BIM_CHECKAL).within(CHECKAL,9).nearestOnClientThread();
        if (npc==null) { hold("Checkal not visible at "+CHECKAL); return; }
        issue(key,Proof.DIALOGUE_CHANGED,f,0,12000,()->npc.click("Talk-to"));
    }
    private static Widget findSprite(Widget widget,int sprite) {
        if (widget==null) return null;
        if (widget.getSpriteId()==sprite && !widget.isHidden()) return widget;
        for (Widget[] children:new Widget[][]{widget.getStaticChildren(),widget.getDynamicChildren(),widget.getNestedChildren()})
            if (children!=null) for (Widget child:children) {
                Widget found=findSprite(child,sprite); if (found!=null) return found;
            }
        return null;
    }
    private void dialogue(Frame f) {
        stage="DIALOGUE";
        int questStage=questStage(f.varp);
        boolean burntofScene=(questStage==15 || questStage==20)
            && f.burntof>=15 && f.position!=null
            && f.position.getPlane()==1 && f.position.getX()>=12000
            && f.position.getX()<14000 && f.position.getY()>=12000
            && f.position.getY()<14000;
        boolean entranceScene=(questStage==25 || questStage==30) && f.checkal>=40
            && f.marley>=40 && f.burntof>=40 && f.position!=null
            && inKnownCaveInstance(f.position);
        boolean expected=(questStage<=10 && near(f.position,WILLOW,12))
            || (questStage==10 && (near(f.position,CHECKAL,12) || near(f.position,ATLAS,12)
                || (f.checkal>=10 && f.position!=null && f.position.getX()>=12000
                    && f.position.getX()<14000 && f.position.getY()>=12000
                    && f.position.getY()<14000)))
            || (questStage==15 && (near(f.position,CHECKAL,12)
                || near(f.position,MARLEY,12) || near(f.position,VARROCK_COOK,12)
                || near(f.position,BURNTOF,12) || burntofScene))
            || burntofScene || entranceScene
            || (questStage==20 && f.burntof==40 && f.marley==40
                && f.checkal==40 && near(f.position,BURNTOF,12))
            || ((questStage==20 || questStage==25 || questStage==30)
                && near(f.position,DUNGEON_WILLOW,12));
        if (!expected) { hold("Dialogue outside recognized quest NPC route at "+f.position+": "+f.dialogue); return; }
        if (!f.options.isEmpty()) {
            dialogueControlMissingAt=0;
            for (String option:f.options) {
                String text=plain(option);
                boolean willowYes=(text.equals("yes.") || text.equals("yes"))
                    && (questStage<=10 || questStage==20 || questStage==25 || questStage==30)
                    && (near(f.position,WILLOW,12) || near(f.position,DUNGEON_WILLOW,12));
                boolean cookSandwich=questStage==15 && near(f.position,VARROCK_COOK,12)
                    && text.equals("i was wondering if you'd be able to make me a steak sandwich?");
                boolean ale=questStage==15 && near(f.position,BARMAID,12)
                    && (text.equals("what ales are you serving?")
                        || text.equals("one asgarnian ale, please."));
                boolean rps=questStage==15 && (near(f.position,BURNTOF,12) || burntofScene)
                    && (text.equals("rock.") || text.equals("rock"));
                if (willowYes || cookSandwich || ale || rps) {
                    issue("dialogue:"+text,Proof.DIALOGUE_CHANGED,f,0,9000,
                        ()->Rs2Dialogue.clickOption(option)); return;
                }
            }
            hold("Unrecognized dialogue options: "+f.options); return;
        }
        if (f.hasContinue) {
            dialogueControlMissingAt=0;
            Widget control=Microbot.getClientThread().invoke((Supplier<Widget>)()->{
                Client c=Microbot.getClient();
                for (int group:new int[]{217,231}) {
                    Widget w=c.getWidget(group,5);
                    if (w!=null && !w.isHidden() && w.getBounds()!=null
                        && w.getBounds().width>0 && w.getBounds().height>0) return w;
                }
                return null;
            });
            if (control==null) { stage="WAIT_VISIBLE_CONTINUE_WIDGET"; return; }
            issue("dialogue:continue-widget",Proof.DIALOGUE_CHANGED,f,0,9000,
                ()->Rs2Widget.clickWidget(control)); return;
        }
        if (dialogueControlMissingAt==0) dialogueControlMissingAt=System.currentTimeMillis();
        if (System.currentTimeMillis()-dialogueControlMissingAt>5000)
            hold("Dialogue without Continue or accepted option for 5s: "+f.dialogue);
        else stage="WAIT_DIALOGUE_CONTROL";
    }
    private static String plain(String s) { return s==null?"":s.replaceAll("<[^>]*>","").trim().toLowerCase(); }
    private static String visiblePlayerDialogueText() {
        return Microbot.getClientThread().invoke((Supplier<String>)()->{
            Client c=Microbot.getClient();
            String best="";
            if (c==null) return best;
            for (int group:new int[]{217,231}) for (int child=0;child<16;child++) {
                Widget w=c.getWidget(group,child);
                if (w==null || w.isHidden() || w.getText()==null) continue;
                String candidate=plain(w.getText());
                if (candidate.isBlank() || candidate.contains("click here to continue")) continue;
                if (candidate.length()>best.length()) best=candidate;
            }
            return best;
        });
    }
    private static boolean contains(int[] ids,int id) { for (int value:ids) if (value==id) return true; return false; }
    private boolean laterMilestone(Frame f) {
        String signature=f.varp+":"+f.marley+":"+f.burntof+":"
            +f.count(ItemID.BIM_STEAK_SANDWICH)+":"+f.count(ItemID.ASGARNIAN_ALE)
            +":"+f.count(ItemID.BREAD)+":"+f.count(ItemID.COOKED_MEAT)
            +":"+f.count(ItemID.KNIFE);
        if (!signature.equals(laterMilestone)) {
            laterMilestone=signature;
            laterMilestoneAt=System.currentTimeMillis();
            laterActionCounts.clear();
        }
        // Stage 15's ingredient routes span multiple towns even after supplies are acquired.
        // The separate route watchdog still bounds stationary or unreachable walking.
        long limit=questStage(f.varp)==15 ? 600000 : 180000;
        if (System.currentTimeMillis()-laterMilestoneAt>limit) {
            hold("Stage 15-30 unchanged for "+(limit/60000)+" minutes: "+signature); return false;
        }
        return true;
    }
    private void recruitCrew(Frame f) {
        stage="RECRUIT_CREW_M"+f.marley+"_B"+f.burntof;
        if (f.marley!=40) { recruitMarley(f); return; }
        if (f.burntof!=40) { recruitBurntof(f); return; }
        stage="VERIFY_BOTH_CREW_RECRUITED"; // BIM_MAIN must advance to 20.
    }
    private void recruitMarley(Frame f) {
        if (f.marley==0) { talkNpc(f,"marley:intro",NpcID.BIM_MARLEY,MARLEY); return; }
        if (f.marley==5) { talkNpc(f,"cook:recipe",NpcID.FAI_VARROCK_BLUEMOON_CHEF,VARROCK_COOK); return; }
        if (f.marley==35) { talkNpc(f,"marley:after-feed",NpcID.BIM_MARLEY,MARLEY); return; }
        if (f.marley!=10 && f.marley!=15 && f.marley!=20 && f.marley!=30) {
            hold("Unmapped Marley varbit "+f.marley); return;
        }
        if (f.count(ItemID.BIM_STEAK_SANDWICH)>0) {
            if (walk(f,"MARLEY_FEED",MARLEY,4)) return;
            Rs2NpcModel npc=npcAt(NpcID.BIM_MARLEY,MARLEY);
            if (npc==null) { hold("Marley absent at "+MARLEY); return; }
            issue("marley:feed-sandwich",Proof.ITEM_USED_OR_DIALOGUE,f,
                ItemID.BIM_STEAK_SANDWICH,14000,
                ()->Rs2Inventory.useItemOnNpc(ItemID.BIM_STEAK_SANDWICH,NpcID.BIM_MARLEY));
            return;
        }
        if (needsSupplies(f)) { acquireSupplies(f); return; }
        if (walk(f,"MARLEY_SANDWICH",MARLEY,4)) return;
        // Installed QuestHelper says knife on bread; Wiki speedrun says knife on cooked meat.
        // Try the two directions only after each unproved attempt, with a bounded hold.
        boolean useMeat=laterActionCounts.getOrDefault("marley:sandwich-bread",0)>0;
        String key=useMeat?"marley:sandwich-meat":"marley:sandwich-bread";
        int target=useMeat?ItemID.COOKED_MEAT:ItemID.BREAD;
        issue(key,Proof.ITEM_GAINED_OR_DIALOGUE,f,ItemID.BIM_STEAK_SANDWICH,12000,
            ()->Rs2Inventory.combine(ItemID.KNIFE,target));
    }
    private static int entryFoodCount(Frame f) {
        int total=0;
        for (int id:ENTRY_FOOD) total+=f.count(id);
        return total;
    }
    private static boolean overworldPrepArea(WorldPoint p) {
        return p!=null && p.getPlane()==0 && p.getX()>=2500 && p.getX()<=3500
            && p.getY()>=3000 && p.getY()<=3800;
    }
    private static boolean inKnownCaveInstance(WorldPoint p) {
        // Private-server instance bases changed between two observed entries.
        // This is only a candidate context; actions still require scene proof.
        return p!=null && p.getPlane()==0 && p.getX()>=10000 && p.getY()>=8000;
    }
    private boolean stage30CaveRecoveryAllowed() {
        if (!Files.isRegularFile(CONTROL) || !classHash.matches("[a-f0-9]{64}"))
            return false;
        Properties p=new Properties();
        try (InputStream in=Files.newInputStream(CONTROL)) { p.load(in); }
        catch (Exception ex) { return false; }
        return "true".equalsIgnoreCase(p.getProperty("allowStage30CaveRecovery","false"))
            && Long.toString(ProcessHandle.current().pid()).equals(p.getProperty("expectedPid"))
            && Integer.toString(BUILD_NUMBER).equals(p.getProperty("expectedBuild"))
            && classHash.equalsIgnoreCase(p.getProperty("expectedClassSha",""));
    }
    private boolean stage30WillowRetryAllowed() {
        if (!Files.isRegularFile(CONTROL) || !classHash.matches("[a-f0-9]{64}"))
            return false;
        Properties p=new Properties();
        try (InputStream in=Files.newInputStream(CONTROL)) { p.load(in); }
        catch (Exception ex) { return false; }
        return "true".equalsIgnoreCase(p.getProperty("allowStage30WillowRetry","false"))
            && Long.toString(ProcessHandle.current().pid()).equals(p.getProperty("expectedPid"))
            && Integer.toString(BUILD_NUMBER).equals(p.getProperty("expectedBuild"))
            && classHash.equalsIgnoreCase(p.getProperty("expectedClassSha",""));
    }
    private boolean stage30ObservationAllowed() {
        if (!Files.isRegularFile(CONTROL) || !classHash.matches("[a-f0-9]{64}"))
            return false;
        Properties p=new Properties();
        try (InputStream in=Files.newInputStream(CONTROL)) { p.load(in); }
        catch (Exception ex) { return false; }
        return "true".equalsIgnoreCase(p.getProperty("allowStage30SceneObservation","false"))
            && Long.toString(ProcessHandle.current().pid()).equals(p.getProperty("expectedPid"))
            && Integer.toString(BUILD_NUMBER).equals(p.getProperty("expectedBuild"))
            && classHash.equalsIgnoreCase(p.getProperty("expectedClassSha",""));
    }
    private void retryStage30Willow(Frame f) {
        if (stage30WillowAttempted) {
            if (System.currentTimeMillis()-stage30WillowAt>30000)
                hold("Stage30 Willow dialogue did not advance the quest varp; no repeated entrance click");
            else stage="VERIFY_STAGE30_WILLOW_TRANSITION";
            return;
        }
        if (walk(f,"STAGE30_WILLOW_RETRY",DUNGEON_WILLOW,3)) return;
        Rs2NpcModel willow=npcAt(NpcID.BIM_WILLOW,DUNGEON_WILLOW);
        if (willow==null) { hold("Stage30 Willow absent outside entrance at "+f.position); return; }
        stage30WillowAttempted=true;
        stage30WillowAt=System.currentTimeMillis();
        issue("stage30:willow-retry",Proof.DIALOGUE_CHANGED,f,0,10000,
            ()->Rs2Npc.interact(willow.getNpc(),"Talk-to"));
    }
    private void recoverStage30EmptyCave(Frame f) {
        stage="STAGE30_EMPTY_CAVE_RECOVERY";
        if (f.inDialogue || f.hasContinue || !f.options.isEmpty()) {
            dialogue(f);
            return;
        }
        GuardianScene scene=guardianScene(f);
        if (scene.guardianPresent || !f.unsafeAggressor.isEmpty() || !scene.pillars.isEmpty()) {
            safetyLogoutIssued=true; safetyLogoutAt=System.currentTimeMillis();
            Rs2Player.logout();
            log.error("[BelowIceMountain] STAGE30_CAVE_RECOVERY_ABORT guardian={} aggressor={} pillars={} pos={}",
                scene.guardianPresent,f.unsafeAggressor,scene.pillars.size(),f.position);
            return;
        }
        Rs2TileObjectModel exit=scene.exit;
        if (exit==null || !"Exit".equalsIgnoreCase(observedAction(exit,"Exit"))) {
            hold("Stage30 empty cave has no observed Exit action at "+f.position);
            return;
        }
        if (stage30RecoveryExitAt>0) {
            if (System.currentTimeMillis()-stage30RecoveryExitAt>12000)
                hold("Stage30 cave Exit click did not prove overworld transition at "+f.position);
            else stage="VERIFY_STAGE30_CAVE_EXIT";
            return;
        }
        WorldPoint exitTile=exit.getWorldLocation();
        if (!near(f.position,exitTile,2)) {
            if (stage30RecoveryMoveFrom!=null) {
                if (!stage30RecoveryMoveFrom.equals(f.position)) {
                    stage30RecoveryMoveFrom=null;
                    stage30RecoveryMoveFailures=0;
                    stage="STAGE30_CAVE_WALK_PROGRESS_PROVED";
                    return;
                }
                if (System.currentTimeMillis()-stage30RecoveryMoveAt<4000) {
                    stage="VERIFY_STAGE30_CAVE_WALK"; return;
                }
                stage30RecoveryMoveFrom=null;
                if (++stage30RecoveryMoveFailures>=3) {
                    hold("Stage30 cave Exit approach stalled at "+f.position
                        +" exit="+exitTile); return;
                }
            }
            WorldPoint approach=new WorldPoint(exitTile.getX()
                +(stage30RecoveryMoveFailures==1?1:stage30RecoveryMoveFailures==2?-1:0),
                exitTile.getY()+1,exitTile.getPlane());
            stage30RecoveryMoveFrom=f.position;
            stage30RecoveryMoveAt=System.currentTimeMillis();
            Rs2Walker.walkStep(approach,0);
            log.info("[BelowIceMountain] STAGE30_CAVE_EXIT_APPROACH from={} target={}",
                f.position,approach);
            stage="VERIFY_STAGE30_CAVE_WALK";
            return;
        }
        boolean accepted=exit.click("Exit");
        if (!accepted) { hold("Stage30 cave Exit rejected at "+f.position); return; }
        stage30RecoveryExitAt=System.currentTimeMillis();
        stage="VERIFY_STAGE30_CAVE_EXIT";
        log.info("[BelowIceMountain] STAGE30_CAVE_EXIT_CLICK_ACCEPTED pos={} exit={}",
            f.position,exitTile);
    }
    private void prepBankAudit(Frame f) {
        stage="PREP_BANK_AUDIT";
        if (!"NOT_RUN".equals(prepBankAudit)) { prepFood(f); return; }
        if (pending!=null) { verify(f); return; }
        if (route!=null) { walk(f,route.key,route.target,route.radius); return; }
        if (System.currentTimeMillis()<nextAt) { stage="WAIT_PREP_BANK_PACE"; return; }
        if (!f.bankOpen) {
            if (walk(f,"PREP_AUDIT_BANK",FALADOR_BANK,5)) return;
            issue("prep:bank-open",Proof.BANK_TOGGLED,f,0,10000,Rs2Bank::openBank);
            return;
        }
        StringBuilder snapshot=new StringBuilder();
        snapshot.append("stage=").append(questStage(f.varp))
            .append(" pos=").append(f.position)
            .append(" hp=").append(f.hp).append('/').append(f.maxHp)
            .append(" carriedFood=").append(entryFoodCount(f))
            .append(" carriedCoins=").append(f.count(ItemID.COINS))
            .append(" bankCoins=").append(Rs2Bank.count(ItemID.COINS));
        for (int id:ENTRY_FOOD)
            snapshot.append(" bankFood[").append(id).append("]=")
                .append(Rs2Bank.count(id));
        for (int id:AUDIT_GEAR)
            snapshot.append(" bankGear[").append(id).append("]=")
                .append(Rs2Bank.count(id));
        for (int id:PICKAXES)
            snapshot.append(" bankPickaxe[").append(id).append("]=")
                .append(Rs2Bank.count(id));
        prepBankAudit=snapshot.toString();
        log.warn("[BelowIceMountain] PREP_BANK_AUDIT {}",prepBankAudit);
        stage="PREP_BANK_AUDITED";
    }
    private void prepFood(Frame f) {
        stage="PREP_NATIVE_GE_FOOD";
        if (!prepNativeStarted && prepNativeCheckpoint.isBlank()) {
            prepNativeCheckpoint=loadPrepNativeCheckpoint();
            prepNativeStarted=!prepNativeCheckpoint.isBlank();
        }
        if (pending!=null) { verify(f); return; }
        if (route!=null) { walk(f,route.key,route.target,route.radius); return; }
        if (System.currentTimeMillis()<nextAt) { stage="WAIT_PREP_FOOD_PACE"; return; }
        if (entryFoodCount(f)>=10 && !prepNativeStarted) {
            hold("High-heal food obtained; HP training and Willow disabled. food="
                +entryFoodCount(f)+" hp="+f.hp+"/"+f.maxHp); return;
        }
        if (!prepNativeStarted && (f.inventorySlots<0 || 28-f.inventorySlots<10)) {
            hold("Ten food slots required; inventory used="+f.inventorySlots); return;
        }
        // The native buyer owns its offer and persists a checkpoint before each input.
        // Fund the full 800-coin cap before starting it, then never rerun this branch.
        if (!prepNativeStarted) {
            if (!f.bankOpen && f.count(ItemID.COINS)<PREP_GE_SPEND_CAP) {
                if (walk(f,"PREP_NATIVE_BANK",FALADOR_BANK,5)) return;
                issue("prep:native-bank-open",Proof.BANK_TOGGLED,f,0,10000,
                    Rs2Bank::openBank); return;
            }
            if (f.bankOpen) {
                int deficit=PREP_GE_SPEND_CAP-f.count(ItemID.COINS);
                if (deficit>0) {
                    if (Rs2Bank.count(ItemID.COINS)<deficit) {
                        hold("Insufficient bank coins for capped native food buy: deficit="
                            +deficit+" bank="+Rs2Bank.count(ItemID.COINS)); return;
                    }
                    issue("prep:native-coins",Proof.ITEM_GAINED,f,ItemID.COINS,10000,
                        ()->Rs2Bank.withdrawX(ItemID.COINS,deficit)); return;
                }
                issue("prep:native-bank-close",Proof.BANK_TOGGLED,f,0,7000,
                    Rs2Bank::closeBank); return;
            }
            if (f.count(ItemID.COINS)<PREP_GE_SPEND_CAP) {
                hold("Capped GE funds vanished before buyer start"); return;
            }
        }
        if (f.bankOpen) {
            hold("Bank unexpectedly open after native GE buyer started"); return;
        }
        if (walk(f,"PREP_NATIVE_GE",GRAND_EXCHANGE,8)) return;
        if (prepNativeBuyer==null) {
            try {
                prepNativeBuyer=new QuestGeBuyer(ItemID.TUNA,"Tuna",10,
                    PREP_GE_SPEND_CAP,
                    "Alex-BelowIceMountain/1.0 (https://github.com/julienlemyre92-commits/tutorial-patches)",
                    prepNativeCheckpoint);
            } catch (Exception ex) {
                hold("Native GE checkpoint invalid: "+ex.getMessage()); return;
            }
        }
        prepNativeStarted=true;
        QuestGeBuyer.Result result;
        prepNativeTickActive=true;
        try { result=prepNativeBuyer.tick(this::savePrepNativeCheckpoint); }
        finally { prepNativeTickActive=false; }
        prepFoodStage=result.phase;
        prepFoodId=ItemID.TUNA;
        prepFoodQuote=result.quotedPrice;
        prepFoodQuantity=10;
        if (result.outcome==QuestGeBuyer.Outcome.WORKING) {
            stage="PREP_NATIVE_GE_"+result.phase; return;
        }
        if (result.outcome==QuestGeBuyer.Outcome.COMPLETE
            && result.itemCount>=10) {
            hold("Native GE food purchase proved: tuna="+result.itemCount
                +" coins="+result.coins+"; HP training and Willow disabled");
            return;
        }
        hold("Native GE food "+result.outcome+" phase="+result.phase
            +" reason="+result.reason+" tuna="+result.itemCount
            +" coins="+result.coins);
    }
    /** Chicken training candidate; requires the native buyer's persisted COMPLETE checkpoint. */
    private void trainChickens(Frame f) {
        stage="PREP_TRAIN_CHICKENS";
        long now=System.currentTimeMillis();
        boolean hpDropped=trainingLastHp>=0 && f.hp<trainingLastHp;
        trainingLastHp=f.hp;
        if (hpDropped && !f.unsafeAggressor.isEmpty()) {
            trainingThreatUntil=now+12000;
            log.warn("[BelowIceMountain] TRAIN_DAMAGE_PROVED attacker={} hp={} pos={}",
                f.unsafeAggressor,f.hp,f.position);
        }
        boolean confirmedThreat=!f.unsafeAggressor.isEmpty() && now<trainingThreatUntil;
        if (trainingRetreat && !confirmedThreat && f.hp==f.maxHp
            && entryFoodCount(f)>=10) {
            trainingRetreat=false;
            trainingStarted=false;
            trainingFarmApproachComplete=false;
            log.info("[BelowIceMountain] TRAIN_RETREAT_CLEARED no_damage pos={} hp={}/{} food={}",
                f.position,f.hp,f.maxHp,entryFoodCount(f));
            if (route!=null && "TRAIN_RETREAT_BANK".equals(route.key)) {
                cancelRoute(); stage="WAIT_TRAIN_RETREAT_ROUTE_STOP"; return;
            }
        }
        if (confirmedThreat || f.hp<=Math.max(6,f.maxHp/2)) {
            trainingRetreat=true;
            if (pending!=null && !"train:emergency-eat".equals(pending.key)) {
                log.warn("[BelowIceMountain] TRAIN_ABORT_UNPROVED key={} threat={} hp={}",
                    pending.key,f.unsafeAggressor,f.hp);
                pending=null;
            }
            if (route!=null && !"TRAIN_RETREAT_BANK".equals(route.key)) {
                cancelRoute(); stage="WAIT_TRAIN_ROUTE_STOP"; return;
            }
        }
        if (pending!=null) { verify(f); return; }
        if (route!=null) { walk(f,route.key,route.target,route.radius); return; }
        if (System.currentTimeMillis()<nextAt) { stage="WAIT_TRAIN_PACE"; return; }
        if (f.maxHp>=20) {
            hold("HP training target observed: maxHp="+f.maxHp
                +" hpXp="+f.hpXp+"; Willow/guardian remain disabled"); return;
        }
        if (f.pickaxes==0 || (!trainingRetreat && entryFoodCount(f)<10)) {
            hold("Training reserve absent: pickaxe="+f.pickaxes
                +" high-heal food="+entryFoodCount(f)); return;
        }
        if (f.hpXp<0 || f.maxHp<11 || !overworldPrepArea(f.position)) {
            hold("Invalid training scene or HP XP: "+f.position+" xp="+f.hpXp); return;
        }
        if (f.geOpen) {
            Widget close=Microbot.getClientThread().invoke((Supplier<Widget>)()->{
                Widget root=Microbot.getClient().getWidget(465,2);
                Widget w=root==null?null:root.getChild(11);
                return w!=null && !w.isHidden() && w.getActions()!=null
                    && java.util.Arrays.asList(w.getActions()).contains("Close")?w:null;
            });
            if (close==null) { hold("GE Close control absent after completed buyer"); return; }
            issue("train:close-ge",Proof.GE_CLOSED,f,0,6000,
                ()->Rs2Widget.clickWidget(close));
            return;
        }
        if (trainingStarted && !trainingRetreat && !near(f.position,CHICKEN_FARM,30)) {
            hold("Moved away from farm after training started; possible death/teleport at "
                +f.position+" xp="+f.hpXp); return;
        }
        if (trainingRetreat) {
            if (f.hp<=Math.max(6,f.maxHp/2)) {
                for (int id:ENTRY_FOOD) if (f.count(id)>0) {
                    issue("train:emergency-eat",Proof.FOOD_HEAL,f,id,7000,
                        ()->Rs2Inventory.interact(id,"Eat"));
                    return;
                }
                hold("Training retreat has no food at HP "+f.hp); return;
            }
            WorldPoint retreatBank=near(f.position,CHICKEN_FARM,80)
                ? LUMBRIDGE_BANK : FALADOR_BANK;
            if (walk(f,"TRAIN_RETREAT_BANK",retreatBank,5)) return;
            hold("Retreated to bank after training threat; aggressor="
                +f.unsafeAggressor+" hp="+f.hp+"/"+f.maxHp); return;
        }
        if (trainingXpActions>=2000) {
            hold("Chicken XP action budget exhausted before 20 HP"); return;
        }
        if (!"".equals(f.interactingNpc)
            && !"Count Check".equalsIgnoreCase(f.interactingNpc)) {
            if (!"Chicken".equalsIgnoreCase(f.interactingNpc)) {
                trainingRetreat=true;
                hold("Unexpected combat target during chicken training: "+f.interactingNpc);
                return;
            }
            trainingSawChickenCombat=true;
            if (trainingEncounterAt>0 && System.currentTimeMillis()-trainingEncounterAt>45000)
                hold("Chicken combat did not finish within 45 seconds; xp="+f.hpXp);
            else stage="WAIT_CHICKEN_COMBAT";
            return;
        }
        if (trainingSawChickenCombat) {
            trainingLastCombatEndAt=System.currentTimeMillis();
            trainingSawChickenCombat=false;
            log.info("[BelowIceMountain] CHICKEN_COMBAT_END_OBSERVED pos={} xp={}",
                f.position,f.hpXp);
        }
        trainingEncounterAt=0;
        long selectionStart=System.currentTimeMillis();
        if (tickTrainingStrengthStyle(f)) return;
        long styleDoneAt=System.currentTimeMillis();
        // The east side of the fence is outside this pen, even if a prior run
        // persisted the approach flag while standing there.
        if (f.position.getX()>=3237 && near(f.position,CHICKEN_FARM,15))
            trainingFarmApproachComplete=false;
        // The farm marker is outside the fence. Proximity to a chicken (or LOS
        // through the gate) does not prove that the player crossed the gate.
        if (!trainingFarmApproachComplete) {
            if (walk(f,"TRAIN_CHICKEN_GATE_CROSS",CHICKEN_INTERIOR_PROOF_TILE,1)) return;
            if (!net.runelite.client.plugins.microbot.util.reachable.Rs2Reachable
                    .isReachable(CHICKEN_INTERIOR_PROOF_TILE)) {
                hold("Chicken gate crossing unproved at "+f.position
                    +" interior="+CHICKEN_INTERIOR_PROOF_TILE); return;
            }
            trainingFarmApproachComplete=true;
            log.info("[BelowIceMountain] CHICKEN_GATE_CROSS_PROVED player={} interior={}",
                f.position,CHICKEN_INTERIOR_PROOF_TILE);
        }
        List<Rs2NpcModel> chickens=new ArrayList<>(Microbot.getRs2NpcCache().query()
            .withName("Chicken").within(CHICKEN_FARM,15).toListOnClientThread());
        chickens.removeIf(n -> n==null || n.isDead() || n.getWorldLocation()==null
            || (n.getIndex()==trainingAvoidIndex
                && System.currentTimeMillis()<trainingAvoidUntil));
        chickens.sort((a,b)->Integer.compare(
            a.getWorldLocation().distanceTo(f.position),
            b.getWorldLocation().distanceTo(f.position)));
        long listDoneAt=System.currentTimeMillis();
        Rs2NpcModel chicken=null;
        for (Rs2NpcModel candidate:chickens) if (candidate.hasLineOfSight()) {
            chicken=candidate; break;
        }
        long losDoneAt=System.currentTimeMillis();
        if (chicken==null) {
            StringBuilder scene=new StringBuilder();
            for (Rs2NpcModel candidate:chickens) scene.append(candidate.getIndex())
                .append('@').append(candidate.getWorldLocation())
                .append(":los=").append(candidate.hasLineOfSight()).append(' ');
            if (!trainingInteriorRepositionAttempted) {
                trainingInteriorRepositionAttempted=true;
                log.warn("[BelowIceMountain] CHICKEN_NO_LOS_REPOSITION player={} target={} candidates={}",
                    f.position,CHICKEN_INTERIOR_PROOF_TILE,scene);
                if (walk(f,"TRAIN_CHICKEN_INTERIOR_PROOF_TILE",CHICKEN_INTERIOR_PROOF_TILE,0)) return;
            }
            hold("No chicken with line of sight after one bounded interior reposition; player="
                +f.position+" target="+CHICKEN_INTERIOR_PROOF_TILE+" candidates="+scene
                +" avoidIndex="+trainingAvoidIndex+" until="+trainingAvoidUntil); return;
        }
        WorldPoint target=chicken.getWorldLocation();
        if (!net.runelite.client.plugins.microbot.util.reachable.Rs2Reachable
                .isReachable(target)) {
            trainingFarmApproachComplete=false;
            log.warn("[BelowIceMountain] CHICKEN_TARGET_UNREACHABLE player={} target={} gate1559or1560=check; crossing required before attack",
                f.position,target);
            if (walk(f,"TRAIN_CHICKEN_GATE_CROSS",CHICKEN_INTERIOR_PROOF_TILE,1)) return;
            hold("Chicken target unreachable after gate route at "+f.position
                +" target="+target+" interior="+CHICKEN_INTERIOR_PROOF_TILE); return;
        }
        if (walk(f,"TRAIN_CHICKEN_PEN",target,2)) return;
        long walkDoneAt=System.currentTimeMillis();
        if (!chicken.hasLineOfSight() || !near(f.position,target,2)) {
            StringBuilder scene=new StringBuilder();
            for (Rs2NpcModel candidate:chickens) scene.append(candidate.getIndex())
                .append('@').append(candidate.getWorldLocation())
                .append(":los=").append(candidate.hasLineOfSight()).append(' ');
            List<Rs2TileObjectModel> gates=Microbot.getRs2TileObjectCache().query()
                .withId(1559).within(CHICKEN_FARM,20).toListOnClientThread();
            StringBuilder gateTiles=new StringBuilder();
            for (Rs2TileObjectModel gate:gates) gateTiles.append(gate.getWorldLocation()).append(' ');
            log.warn("[BelowIceMountain] CHICKEN_FENCE_SCENE player={} candidates={} gates1559={}",
                f.position,scene,gateTiles);
            hold("Chicken behind gate/wall: player="+f.position+" npc="+target
                +" candidates="+scene+" gates1559="+gateTiles);
            return;
        }
        trainingStarted=true;
        trainingFarmApproachComplete=true;
        trainingEncounterAt=System.currentTimeMillis();
        trainingTargetIndex=chicken.getIndex();
        final long observedEndAt=trainingLastCombatEndAt;
        final Rs2NpcModel targetChicken=chicken;
        boolean probe=trainingStylePhase==TrainingStylePhase.READY_PROBE;
        if (probe) {
            trainingProbeHpXp=f.hpXp;
            trainingProbeStrengthXp=f.strengthXp;
            trainingProbeAttackXp=f.attackXp;
            trainingStyleAt=System.currentTimeMillis();
            trainingStylePhase=TrainingStylePhase.PROBE_SENT;
        }
        final long dispatchStartedAt=System.currentTimeMillis();
        if (observedEndAt>0) log.info(
            "[BelowIceMountain] CHICKEN_SELECTION_TIMING_MS style={} list={} los={} walk={} guard={} totalBeforeApi={}",
            styleDoneAt-selectionStart,listDoneAt-styleDoneAt,losDoneAt-listDoneAt,
            walkDoneAt-losDoneAt,dispatchStartedAt-walkDoneAt,dispatchStartedAt-observedEndAt);
        issue(probe?"train:strength-probe":"train:chicken",
            probe?Proof.STRENGTH_HP_XP_GAINED:Proof.HP_XP_GAINED,f,0,30000,
            ()->Rs2Npc.attack(targetChicken.getNpc()));
        if (observedEndAt>0) {
            trainingLastTargetGapMs=System.currentTimeMillis()-observedEndAt;
            log.info("[BelowIceMountain] CHICKEN_CLICK_TIMING_MS acquire={} api={} total={} target={} pos={}",
                dispatchStartedAt-observedEndAt,System.currentTimeMillis()-dispatchStartedAt,
                trainingLastTargetGapMs,trainingTargetIndex,f.position);
            trainingLastCombatEndAt=0;
        }
    }
    private void savePrepNativeCheckpoint(String value) {
        try {
            Files.createDirectories(PREP_GE_CHECKPOINT.getParent());
            Path temp=PREP_GE_CHECKPOINT.resolveSibling("prep-ge-checkpoint.tmp");
            Files.writeString(temp,ProcessHandle.current().pid()+"\n"
                +prepAccountName()+"\n"+value);
            Files.move(temp,PREP_GE_CHECKPOINT,StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
            prepNativeCheckpoint=value;
        } catch (Exception ex) {
            throw new IllegalStateException("GE checkpoint persist failed before input",ex);
        }
    }
    private String loadPrepNativeCheckpoint() {
        try {
            if (!Files.isRegularFile(PREP_GE_CHECKPOINT)) return "";
            String[] lines=Files.readString(PREP_GE_CHECKPOINT).split("\\R",3);
            if (lines.length!=3 || !Long.toString(ProcessHandle.current().pid()).equals(lines[0])
                || !prepAccountName().equals(lines[1]))
                return "";
            return lines[2].trim();
        } catch (Exception ex) { return ""; }
    }
    private String prepAccountName() {
        return Microbot.getClientThread().invoke((Supplier<String>)()->
            Microbot.getClient()!=null && Microbot.getClient().getLocalPlayer()!=null
                ? String.valueOf(Microbot.getClient().getLocalPlayer().getName()) : "");
    }
    private static boolean needsSupplies(Frame f) {
        return f.count(ItemID.KNIFE)==0 || f.count(ItemID.BREAD)==0
            || f.count(ItemID.COOKED_MEAT)==0;
    }
    private void acquireSupplies(Frame f) {
        stage="MARLEY_SUPPLIES";
        if (!supplyBankChecked) { supplyBank(f); return; }
        if (f.shopOpen) { supplyShop(f); return; }
        if (f.count(ItemID.KNIFE)==0) { supplyKitchenItem(f,ItemID.KNIFE); return; }
        if (f.count(ItemID.COOKED_MEAT)==0 && f.count(ItemID.RAW_BEEF)==0
            || f.count(ItemID.BREAD)==0 && f.count(ItemID.BREAD_DOUGH)==0
                && f.count(ItemID.POT_FLOUR)==0) {
            if (walk(f,"SUPPLY_WYDIN",SUPPLY_WYDIN,3)) return;
            Rs2NpcModel trader=Microbot.getRs2NpcCache().query()
                .withName("Wydin").within(SUPPLY_WYDIN,12).nearestOnClientThread();
            if (trader==null) { missingSupply("Wydin at Port Sarim"); return; }
            clearMissingSupply();
            if (!trader.hasLineOfSight()) {
                if (walk(f,"SUPPLY_WYDIN_APPROACH",trader.getWorldLocation(),1)) return;
                missingSupply("Wydin line of sight"); return;
            }
            issue("supply:shop-open",Proof.SHOP_TOGGLED,f,0,9000,
                ()->trader.click("Trade"));
            return;
        }
        if (f.count(ItemID.BREAD)==0) {
            if (f.count(ItemID.BREAD_DOUGH)>0) {
                cookSupply(f,ItemID.BREAD_DOUGH,ItemID.BREAD); return;
            }
            if (f.count(ItemID.BOWL_WATER)==0) {
                if (f.count(ItemID.BOWL_EMPTY)==0) {
                    supplyKitchenItem(f,ItemID.BOWL_EMPTY); return;
                }
                if (walk(f,"SUPPLY_KITCHEN_SINK",SUPPLY_KITCHEN,0)) return;
                Rs2TileObjectModel sink=Microbot.getRs2TileObjectCache().query()
                    .withNameContains("Sink").within(SUPPLY_KITCHEN,9).nearestOnClientThread();
                if (sink==null) { missingSupply("Lumbridge kitchen sink"); return; }
                if (!sink.isReachable()) { missingSupply("Lumbridge kitchen sink reachability"); return; }
                clearMissingSupply();
                issue("supply:fill-bowl",Proof.ITEM_GAINED,f,ItemID.BOWL_WATER,9000,
                    ()->Rs2Inventory.useItemOnObject(ItemID.BOWL_EMPTY,sink.getId()));
                return;
            }
            Widget dough=Microbot.getClientThread().invoke((Supplier<Widget>)()->
                findDoughChoice(Microbot.getClient()));
            if (dough!=null) {
                issue("supply:select-bread-dough",Proof.ITEM_GAINED,f,ItemID.BREAD_DOUGH,9000,
                    ()->Microbot.getClientThread().invoke((Supplier<Boolean>)()->{
                        Widget current=findDoughChoice(Microbot.getClient());
                        return current!=null && current.getBounds()!=null
                            && Rs2Widget.clickWidget(current);
                    }));
                return;
            }
            if (f.production) {
                if (!doughWidgetDumped) {
                    doughWidgetDumped=true;
                    Microbot.getClientThread().invoke((Supplier<Boolean>)()->{
                        dumpDoughWidgets(Microbot.getClient()); return true;
                    });
                }
                missingSupply("Bread dough choice absent in production widget"); return;
            }
            issue("supply:make-dough:"+f.count(ItemID.POT_FLOUR),Proof.PRODUCTION_OPEN,
                f,ItemID.POT_FLOUR,9000,
                ()->Rs2Inventory.combine(ItemID.BOWL_WATER,ItemID.POT_FLOUR));
            return;
        }
        if (f.count(ItemID.COOKED_MEAT)==0) {
            cookSupply(f,ItemID.RAW_BEEF,ItemID.COOKED_MEAT); return;
        }
    }
    private void supplyBank(Frame f) {
        stage="SUPPLY_RECHECK_BANK";
        if (walk(f,"SUPPLY_FALADOR_BANK",FALADOR_BANK,5)) return;
        if (!f.bankOpen) {
            issue("supply:bank-open",Proof.BANK_TOGGLED,f,0,10000,Rs2Bank::openBank);
            return;
        }
        int[] ids={ItemID.KNIFE,ItemID.BREAD,ItemID.COOKED_MEAT,
            ItemID.RAW_BEEF,ItemID.POT_FLOUR,ItemID.BREAD_DOUGH,
            ItemID.BOWL_WATER,ItemID.BOWL_EMPTY};
        for (int id:ids) if (f.count(id)==0 && Rs2Bank.count(id)>0) {
            int amount=id==ItemID.RAW_BEEF ? Math.min(3,Rs2Bank.count(id))
                : id==ItemID.POT_FLOUR ? Math.min(3,Rs2Bank.count(id)) : 1;
            issue("supply:bank-withdraw:"+id,Proof.ITEM_GAINED,f,id,10000,
                ()->amount==1 ? Rs2Bank.withdrawOne(id) : Rs2Bank.withdrawX(id,amount));
            return;
        }
        if (f.count(ItemID.COINS)<20 && Rs2Bank.count(ItemID.COINS)>0) {
            int amount=Math.min(100,Rs2Bank.count(ItemID.COINS));
            issue("supply:bank-coins",Proof.ITEM_GAINED,f,ItemID.COINS,10000,
                ()->Rs2Bank.withdrawX(ItemID.COINS,amount));
            return;
        }
        supplyBankChecked=true;
        issue("supply:bank-close",Proof.BANK_TOGGLED,f,0,7000,Rs2Bank::closeBank);
    }
    private void supplyKitchenItem(Frame f,int id) {
        stage="SUPPLY_LUMBRIDGE_KITCHEN_"+id;
        if (walk(f,"SUPPLY_KITCHEN",SUPPLY_KITCHEN,0)) return;
        WorldPoint tile=Microbot.getClientThread().invoke((Supplier<WorldPoint>)()->{
            RS2Item[] items=Rs2GroundItem.getAll(id);
            if (items==null) return null;
            WorldPoint best=null; int distance=Integer.MAX_VALUE;
            for (RS2Item item:items) {
                if (item==null || item.getTile()==null) continue;
                WorldPoint where=item.getTile().getWorldLocation();
                if (!near(where,SUPPLY_KITCHEN,9)) continue;
                int d=distance(f.position,where);
                if (d<distance) { distance=d; best=where; }
            }
            return best;
        });
        if (tile==null) { missingSupply("Lumbridge kitchen ground item "+id); return; }
        clearMissingSupply();
        if (walk(f,"SUPPLY_GROUND_"+id,tile,1)) return;
        issue("supply:loot:"+id,Proof.ITEM_GAINED,f,id,10000,
            ()->Rs2GroundItem.loot(tile,id));
    }
    private void supplyShop(Frame f) {
        stage="SUPPLY_WYDIN_STOCK";
        if (!near(f.position,SUPPLY_WYDIN,12)) {
            hold("Shop opened away from verified Wydin area at "+f.position); return;
        }
        if (Rs2Shop.shopItems==null) { missingSupply("Wydin shop inventory"); return; }
        clearMissingSupply();
        int item=0;
        if (f.count(ItemID.BREAD)==0 && shopStock(ItemID.BREAD)>0) item=ItemID.BREAD;
        else if (f.count(ItemID.COOKED_MEAT)==0 && f.count(ItemID.RAW_BEEF)<3
            && beefBought<5 && shopStock(ItemID.RAW_BEEF)>0) item=ItemID.RAW_BEEF;
        else if (f.count(ItemID.BREAD)==0 && f.count(ItemID.BREAD_DOUGH)==0
            && f.count(ItemID.POT_FLOUR)<3 && flourBought<4
            && shopStock(ItemID.POT_FLOUR)>0) item=ItemID.POT_FLOUR;
        if (item!=0) {
            if (f.count(ItemID.COINS)==0) { hold("No coins to buy Marley ingredients at Wydin"); return; }
            final int buy=item;
            issue("supply:buy:"+buy+":"+f.count(buy),Proof.ITEM_GAINED,f,buy,8000,
                ()->Rs2Shop.buyItem(buy,"1"));
            return;
        }
        if (f.count(ItemID.COOKED_MEAT)==0 && f.count(ItemID.RAW_BEEF)==0) {
            hold("Wydin has no usable raw beef; stock="+shopStock(ItemID.RAW_BEEF)); return;
        }
        if (f.count(ItemID.BREAD)==0 && f.count(ItemID.BREAD_DOUGH)==0
            && f.count(ItemID.POT_FLOUR)==0) {
            hold("Wydin has no bread or flour; stock bread="+shopStock(ItemID.BREAD)
                +" flour="+shopStock(ItemID.POT_FLOUR)); return;
        }
        issue("supply:shop-close",Proof.SHOP_TOGGLED,f,0,7000,
            ()->{Rs2Shop.closeShop(); return true;});
    }
    private static int shopStock(int id) {
        List<Rs2ItemModel> items=Rs2Shop.shopItems;
        if (items==null) return 0;
        for (Rs2ItemModel item:items)
            if (item!=null && item.getId()==id) return Math.max(0,item.getQuantity());
        return 0;
    }
    private void cookSupply(Frame f,int raw,int cooked) {
        stage="SUPPLY_COOK_"+raw;
        Rs2TileObjectModel range=null;
        if (near(f.position,SUPPLY_KITCHEN,12)) {
            range=Microbot.getRs2TileObjectCache().query()
                .withNameContains("Cooking range").within(SUPPLY_KITCHEN,9).nearestOnClientThread();
            if (range!=null && !range.isReachable()) range=null;
        }
        if (range==null) {
            if (walk(f,"SUPPLY_RIMMINGTON_RANGE",SUPPLY_RANGE,2)) return;
            range=Microbot.getRs2TileObjectCache().query()
                .withId(SUPPLY_RANGE_ID).within(SUPPLY_RANGE,8).nearestOnClientThread();
        }
        if (range==null) { missingSupply("Rimmington range "+SUPPLY_RANGE_ID); return; }
        if (!range.isReachable()) { missingSupply("Rimmington range reachability"); return; }
        clearMissingSupply();
        log.info("[BelowIceMountain] SUPPLY_RANGE id={} tile={} kitchenCandidate={} raw={}",
            range.getId(),range.getWorldLocation(),near(range.getWorldLocation(),SUPPLY_KITCHEN,9),raw);
        final int rangeId=range.getId();
        Widget product=Microbot.getClientThread().invoke((Supplier<Widget>)()->
            findSupplyProduct(Microbot.getClient(),raw,cooked));
        if (!f.production) {
            issue("supply:cook-open:"+raw,Proof.PRODUCTION_OPEN,f,raw,10000,
                ()->Rs2Inventory.useItemOnObject(raw,rangeId));
            return;
        }
        if (product==null || product.getBounds()==null) {
            missingSupply("Cooking product widget for "+raw); return;
        }
        issue("supply:cook-one:"+raw+":"+f.count(raw),Proof.ITEM_CONSUMED,
            f,raw,13000,()->Microbot.getClientThread().invoke((Supplier<Boolean>)()->{
                Widget current=findSupplyProduct(Microbot.getClient(),raw,cooked);
                return current!=null && !current.isHidden() && current.getBounds()!=null
                    && Rs2Widget.clickWidget(current);
            }));
    }
    private static Widget findSupplyProduct(Client client,int raw,int cooked) {
        if (client==null) return null;
        Widget product=findSupplyProduct(client.getWidget(270,14),0,raw,cooked);
        return product!=null ? product
            : findSupplyProduct(client.getWidget(300,16),0,raw,cooked);
    }
    private static Widget findDoughChoice(Client client) {
        if (client==null) return null;
        Widget title=client.getWidget(270,5), first=client.getWidget(270,15);
        if (title!=null && !title.isHidden() && title.getText()!=null
            && title.getText().contains("What sort of dough") && first!=null
            && !first.isHidden() && first.getActions()!=null
            && java.util.Arrays.asList(first.getActions()).contains("Make")) return first;
        for (int group:new int[]{270,300,309,446})
            for (int child=0;child<100;child++) {
                Widget found=findSupplyProduct(client.getWidget(group,child),0,-1,ItemID.BREAD_DOUGH);
                if (found!=null) return found;
            }
        return null;
    }
    private static void dumpDoughWidgets(Client client) {
        if (client==null) return;
        int shown=0;
        for (int group:new int[]{270,300,309,446})
            for (int child=0;child<100;child++) {
                Widget w=client.getWidget(group,child);
                if (w==null || w.isHidden()) continue;
                String text=w.getText();
                if (w.getItemId()<0 && (text==null || text.isBlank())
                    && (w.getActions()==null || w.getActions().length==0)) continue;
                log.info("[BelowIceMountain] DOUGH_WIDGET group={} child={} id={} item={} text={} actions={}",
                    group,child,w.getId(),w.getItemId(),text,java.util.Arrays.toString(w.getActions()));
                if (++shown>=45) return;
            }
    }
    private static Widget findSupplyProduct(Widget widget,int depth,int raw,int cooked) {
        if (widget==null || widget.isHidden() || depth>8) return null;
        if (widget.getItemId()==raw || widget.getItemId()==cooked) return widget;
        for (Widget[] children:new Widget[][]{widget.getChildren(),
            widget.getDynamicChildren(),widget.getStaticChildren()})
            if (children!=null) for (Widget child:children) {
                Widget found=findSupplyProduct(child,depth+1,raw,cooked);
                if (found!=null) return found;
            }
        return null;
    }
    private void missingSupply(String key) {
        long now=System.currentTimeMillis();
        if (!key.equals(missingSupplyScene)) { missingSupplyScene=key; missingSupplySince=now; }
        if (now-missingSupplySince>15000) hold("Supply scene unavailable: "+key);
        else stage="WAIT_SUPPLY_SCENE_"+key;
    }
    private void clearMissingSupply() { missingSupplyScene=""; missingSupplySince=0; }
    private void recruitBurntof(Frame f) {
        if (f.burntof==0) { talkNpc(f,"burntof:intro",NpcID.BIM_BURNTOF,BURNTOF); return; }
        if (f.burntof==5) {
            if (f.count(ItemID.ASGARNIAN_ALE)>0) {
                if (walk(f,"BURNTOF_ALE",BURNTOF,4)) return;
                Rs2NpcModel npc=npcAt(NpcID.BIM_BURNTOF,BURNTOF);
                if (npc==null) { hold("Burntof absent at "+BURNTOF); return; }
                issue("burntof:give-ale",Proof.ITEM_USED_OR_DIALOGUE,f,ItemID.ASGARNIAN_ALE,12000,
                    ()->Rs2Inventory.useItemOnNpc(ItemID.ASGARNIAN_ALE,NpcID.BIM_BURNTOF));
                return;
            }
            if (f.count(ItemID.COINS)<3) { hold("Need 3 coins for Asgarnian ale"); return; }
            talkNpc(f,"barmaid:ale",NpcID.RISINGSUN_BARMAID2,BARMAID); return;
        }
        if (f.burntof==10 || f.burntof==15) {
            talkNpc(f,"burntof:rps",NpcID.BIM_BURNTOF,BURNTOF); return;
        }
        hold("Unmapped Burntof varbit "+f.burntof);
    }
    private void followWillow(Frame f) {
        stage="FOLLOW_WILLOW_"+f.varp;
        if (!dungeonEntryAllowed(f)) {
            hold("Willow dialogue blocked by dungeon preflight at "+f.position);
            return;
        }
        talkNpc(f,"willow:dungeon",NpcID.BIM_WILLOW,DUNGEON_WILLOW);
    }
    private boolean requiresDungeonPreflight(Frame f) {
        if (f.position==null) return false;
        int step=questStage(f.varp);
        if ((step==20 || step==25) && near(f.position,DUNGEON_WILLOW,12)) return true;
        return step==30 && (near(f.position,DUNGEON_WILLOW,12)
            || (f.position.getX()>10000 && f.position.getY()>10000));
    }
    private void enterRuins(Frame f) {
        stage="ENTER_RUINS";
        if (f.position.getY()>5000) { stage="VERIFY_DUNGEON_STAGE_35"; return; }
        if (!dungeonEntryAllowed(f)) {
            stage="HOLD_STAGE30_GUARDIAN_ENTRY_NOT_ARMED";
            return;
        }
        if (entranceAt>0) {
            if (System.currentTimeMillis()-entranceAt>60000)
                hold("Entrance interaction did not advance BIM_MAIN from 30 to 35");
            else stage="VERIFY_ENTRANCE_STAGE_35";
            return;
        }
        if (walk(f,"DUNGEON_ENTRANCE",DUNGEON_ENTRANCE,3)) return;
        Rs2TileObjectModel entrance=Microbot.getRs2TileObjectCache().query()
            .withId(ObjectID.BIM_ENTRANCE).within(DUNGEON_ENTRANCE,6).nearestOnClientThread();
        if (entrance==null) { hold("BIM_ENTRANCE 41357 absent at "+DUNGEON_ENTRANCE); return; }
        String[] actions=Microbot.getClientThread().invoke((Supplier<String[]>)()->
            entrance.getObjectComposition()==null?null:entrance.getObjectComposition().getActions());
        String chosen=null;
        if (actions!=null) for (String action:actions)
            if (action!=null && (action.equalsIgnoreCase("Enter")
                || action.equalsIgnoreCase("Climb-down"))) { chosen=action; break; }
        if (chosen==null) { hold("BIM_ENTRANCE lacks verified Enter/Climb-down action: "
            +java.util.Arrays.toString(actions)); return; }
        final String menu=chosen;
        issue("entrance:"+menu,Proof.DUNGEON_ENTERED,f,0,45000,()->entrance.click(menu));
    }
    /** Stage 40 is only entered after the guardian exit is proved on a later tick. */
    private void stage40Cutscene(Frame f) {
        long now=System.currentTimeMillis();
        if (!stage40ExitProved || guardianEntryRegion<0 || f.position==null
            || questStage(f.varp)!=40) {
            hold("Stage-40 final cutscene lacks proved exit, position, or exact stage: "
                +f.position+" varp="+f.varp+" entryRegion="+guardianEntryRegion);
            return;
        }
        if (finalCutscenePhase==FinalCutscenePhase.APPROACH) {
            stage="STAGE40_APPROACH_FINAL_ENTRANCE";
            if (f.position.getRegionID()==guardianEntryRegion
                || !overworldPrepArea(f.position) || !f.unsafeAggressor.isEmpty()) {
                hold("Stage-40 entrance approach not proved safe in overworld at "+f.position);
                return;
            }
            if (f.inDialogue || f.hasContinue || !f.options.isEmpty()) {
                hold("Unexpected dialogue before stage-40 entrance click: "+f.options);
                return;
            }
            if (walk(f,"STAGE40_FINAL_ENTRANCE",DUNGEON_ENTRANCE,3)) return;
            Rs2TileObjectModel entrance=Microbot.getRs2TileObjectCache().query()
                .withId(ObjectID.BIM_ENTRANCE).within(DUNGEON_ENTRANCE,6).nearestOnClientThread();
            if (entrance==null) {
                hold("Stage-40 BIM_ENTRANCE 41357 absent at "+DUNGEON_ENTRANCE);
                return;
            }
            String[] actions=Microbot.getClientThread().invoke((Supplier<String[]>)()->
                entrance.getObjectComposition()==null?null:entrance.getObjectComposition().getActions());
            String chosen=null;
            if (actions!=null) for (String action:actions)
                if (action!=null && (action.equalsIgnoreCase("Enter")
                    || action.equalsIgnoreCase("Climb-down"))) { chosen=action; break; }
            if (chosen==null) {
                hold("Stage-40 entrance lacks observed Enter/Climb-down menu: "
                    +java.util.Arrays.toString(actions));
                return;
            }
            if (!entrance.click(chosen)) {
                hold("Stage-40 entrance click rejected: "+chosen);
                return;
            }
            finalDialogueBefore=f;
            finalCutsceneAt=now;
            finalCutscenePhase=FinalCutscenePhase.VERIFY_ENTRANCE;
            stage="STAGE40_VERIFY_ENTRANCE_DIALOGUE";
            log.info("[BelowIceMountain] STAGE40_ENTRANCE_CLICK menu={} pos={}",chosen,f.position);
            return;
        }
        if (finalCutscenePhase==FinalCutscenePhase.VERIFY_ENTRANCE) {
            stage="STAGE40_VERIFY_ENTRANCE_DIALOGUE";
            if (finalDialogueChanged(finalDialogueBefore,f)
                && (f.inDialogue || f.hasContinue || !f.options.isEmpty())) {
                finalCutscenePhase=FinalCutscenePhase.CHOOSE_YES;
                finalCutsceneAt=now;
                stage="STAGE40_ENTRANCE_DIALOGUE_PROVED";
            } else if (now-finalCutsceneAt>15000)
                hold("Stage-40 entrance click produced no observed dialogue in 15s");
            return;
        }
        if (finalCutscenePhase==FinalCutscenePhase.VERIFY_CONTINUE
            || finalCutscenePhase==FinalCutscenePhase.VERIFY_YES) {
            boolean yes=finalCutscenePhase==FinalCutscenePhase.VERIFY_YES;
            stage=yes?"STAGE40_VERIFY_YES":"STAGE40_VERIFY_CONTINUE";
            if (finalDialogueChanged(finalDialogueBefore,f)) {
                finalCutscenePhase=yes?FinalCutscenePhase.WAIT_FINISHED
                    :finalYesClicks==0?FinalCutscenePhase.CHOOSE_YES:FinalCutscenePhase.WAIT_FINISHED;
                finalCutsceneAt=now;
                stage=yes?"STAGE40_YES_DIALOGUE_PROVED":"STAGE40_CONTINUE_PROVED";
            } else if (now-finalCutsceneAt>15000)
                hold("Stage-40 "+(yes?"Yes":"Continue")+" click produced no dialogue change");
            return;
        }
        if (finalCutscenePhase==FinalCutscenePhase.CHOOSE_YES) {
            stage="STAGE40_FIND_YES";
            if (!f.options.isEmpty()) {
                for (String option:f.options) {
                    String label=plain(option);
                    if (label.equals("yes.") || label.equals("yes")) {
                        if (finalYesClicks!=0) {
                            hold("Stage-40 Yes reappeared after it was already clicked");
                            return;
                        }
                        if (!Rs2Dialogue.clickOption(option)) {
                            hold("Stage-40 visible Yes option click rejected");
                            return;
                        }
                        finalYesClicks=1;
                        finalYesAt=now;
                        finalDialogueBefore=f;
                        finalCutsceneAt=now;
                        finalCutscenePhase=FinalCutscenePhase.VERIFY_YES;
                        stage="STAGE40_VERIFY_YES";
                        log.info("[BelowIceMountain] STAGE40_YES_CLICK option={}",option);
                        return;
                    }
                }
                hold("Stage-40 dialogue has no observed Yes option: "+f.options);
                return;
            }
            if (f.hasContinue) {
                stage40Continue(f,now);
                return;
            }
            if (now-finalCutsceneAt>15000)
                hold("Stage-40 entrance dialogue has no Yes or Continue in 15s");
            return;
        }
        // After the one Yes click, completion is proved only by the native quest state
        // checked at the start of tick(). Further cutscene pages must each be observed.
        stage="STAGE40_WAIT_NATIVE_QUEST_FINISHED";
        if (finalYesClicks!=1 || finalYesAt==0) {
            hold("Stage-40 completion wait reached without one proved Yes click");
            return;
        }
        if (!f.options.isEmpty()) {
            hold("Unexpected stage-40 option after Yes; refusing repeat: "+f.options);
            return;
        }
        if (f.hasContinue) {
            stage40Continue(f,now);
            return;
        }
        if (now-finalYesAt>60000)
            hold("Stage-40 cutscene did not produce native Quest.BELOW_ICE_MOUNTAIN FINISHED in 60s");
    }
    private void stage40Continue(Frame f,long now) {
        if (finalContinueClicks>=8) {
            hold("Stage-40 cutscene exceeded eight proved Continue clicks");
            return;
        }
        Rs2Dialogue.clickContinue();
        finalContinueClicks++;
        finalDialogueBefore=f;
        finalCutsceneAt=now;
        finalCutscenePhase=FinalCutscenePhase.VERIFY_CONTINUE;
        stage="STAGE40_VERIFY_CONTINUE";
        log.info("[BelowIceMountain] STAGE40_CONTINUE_CLICK count={}",finalContinueClicks);
    }
    private static boolean finalDialogueChanged(Frame before,Frame after) {
        return before!=null && (after.inDialogue!=before.inDialogue
            || after.hasContinue!=before.hasContinue
            || !plain(after.dialogue).equals(plain(before.dialogue))
            || !after.options.equals(before.options));
    }
    private boolean dungeonEntryAllowed(Frame f) {
        if (f.pickaxes==0 || f.hp<f.maxHp || f.maxHp<20 || entryFoodCount(f)<8)
            return false;
        // Current HP, food and tool are the entry requirements. The training
        // style probe is diagnostic history and can be lost on a cold restart;
        // it must not override directly observed readiness on this account.
        return guardianActionsAllowed();
    }
    private boolean guardianActionsAllowed() {
        if (!Files.isRegularFile(CONTROL) || !classHash.matches("[a-f0-9]{64}"))
            return false;
        Properties props=new Properties();
        try (InputStream in=Files.newInputStream(CONTROL)) { props.load(in); }
        catch (Exception ex) { return false; }
        return "true".equalsIgnoreCase(props.getProperty("allowDungeonEntry","false"))
            && "true".equalsIgnoreCase(props.getProperty("allowGuardianActions","false"))
            && ("true".equalsIgnoreCase(props.getProperty("allowSupervisedDungeonProbe","false"))
                || (UNATTENDED_ESCAPE_VERIFIED
                    && "true".equalsIgnoreCase(props.getProperty("allowUnattendedDungeonEntry","false"))))
            && Long.toString(ProcessHandle.current().pid()).equals(props.getProperty("expectedPid"))
            && Integer.toString(BUILD_NUMBER).equals(props.getProperty("expectedBuild"))
            && classHash.equalsIgnoreCase(props.getProperty("expectedClassSha",""));
    }
    private static int trainingPickaxeToWield(Frame f) {
        // F2P pickaxes only; an occupied weapon slot is never replaced here.
        int[] ids={1275,1271,1273,1269,1267,1265};
        int[] attackRequired={40,30,20,5,1,1};
        for (int i=0;i<ids.length;i++)
            if (f.attack>=attackRequired[i] && f.count(ids[i])>0) return ids[i];
        return -1;
    }
    private static final class TrainingStyleControl {
        int index=-1, category=-1;
        String title="", label="";
        boolean buttonVisible;
        Widget button;
    }
    private TrainingStyleControl unarmedAggressiveControl() {
        return Microbot.getClientThread().invoke((Supplier<TrainingStyleControl>)()->{
            Client c=Microbot.getClient();
            TrainingStyleControl result=new TrainingStyleControl();
            if (c==null) return result;
            result.category=c.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
            EnumComposition categories=c.getEnum(EnumID.WEAPON_STYLES);
            if (categories==null) return result;
            int styleEnumId=categories.getIntValue(result.category);
            if (styleEnumId<0) return result;
            EnumComposition styles=c.getEnum(styleEnumId);
            if (styles==null || styles.getIntVals()==null) return result;
            int[] buttons={InterfaceID.CombatInterface._0,InterfaceID.CombatInterface._1,
                InterfaceID.CombatInterface._2,InterfaceID.CombatInterface._3};
            int[] labels={InterfaceID.CombatInterface._0_TEXT,InterfaceID.CombatInterface._1_TEXT,
                InterfaceID.CombatInterface._2_TEXT,InterfaceID.CombatInterface._3_TEXT};
            int[] structs=styles.getIntVals();
            for (int i=0;i<Math.min(structs.length,buttons.length);i++) {
                StructComposition entry=c.getStructComposition(structs[i]);
                String kind=entry==null?null:entry.getStringValue(ParamID.ATTACK_STYLE_NAME);
                if (!"Aggressive".equalsIgnoreCase(kind)) continue;
                result.index=i;
                result.button=c.getWidget(buttons[i]);
                result.buttonVisible=result.button!=null && !result.button.isHidden();
                Widget label=c.getWidget(labels[i]);
                result.label=plain(label==null?"":label.getText());
                break;
            }
            Widget title=c.getWidget(InterfaceID.CombatInterface.TITLE);
            result.title=plain(title==null?"":title.getText());
            return result;
        });
    }
    /** One action per tick; each tab, equipment, style and XP change needs later proof. */
    private boolean tickTrainingStrengthStyle(Frame f) {
        if (trainingStyleRestoreInvalid) {
            hold("Invalid persisted training style phase; refusing an action repeat"); return true;
        }
        if (f.inDialogue || f.hasContinue || !f.options.isEmpty()) {
            stage="WAIT_TRAIN_STYLE_DIALOGUE"; return true;
        }
        if (trainingStylePhase==TrainingStylePhase.DONE) {
            if (f.weaponId>=0 || f.count(ItemID.BRONZE_PICKAXE)==0
                || trainingStyleIndex<0 || f.weaponCategory!=trainingStyleCategory
                || f.combatMode!=trainingStyleIndex) {
                hold("Confirmed Strength setup changed during training"); return true;
            }
            return false;
        }
        if (Rs2Player.isInCombat()) { stage="WAIT_TRAIN_STYLE_COMBAT_END"; return true; }
        long now=System.currentTimeMillis();
        if (trainingStylePhase==TrainingStylePhase.NEED_UNEQUIP) {
            if (f.weaponId<0) {
                if (f.count(ItemID.BRONZE_PICKAXE)==0) {
                    hold("Unarmed but bronze pickaxe not retained in inventory"); return true;
                }
                trainingStylePhase=TrainingStylePhase.NEED_TAB;
                stage="TRAIN_UNARMED_PICKAXE_RETAINED"; return true;
            }
            if (f.weaponId!=ItemID.BRONZE_PICKAXE || f.inventorySlots<0
                || f.inventorySlots>=28) {
                hold("Cannot safely unequip bronze pickaxe: weapon="+f.weaponId
                    +" slots="+f.inventorySlots); return true;
            }
            trainingUnequipBefore=f.count(ItemID.BRONZE_PICKAXE);
            trainingStylePhase=TrainingStylePhase.UNEQUIP_SENT;
            issue("train:unequip-bronze-pickaxe",Proof.WEAPON_UNEQUIPPED,f,
                ItemID.BRONZE_PICKAXE,8000,
                ()->Rs2Equipment.unEquip(EquipmentInventorySlot.WEAPON));
            return true;
        }
        if (trainingStylePhase==TrainingStylePhase.UNEQUIP_SENT) {
            if (f.weaponId<0 && trainingUnequipBefore>=0
                && f.count(ItemID.BRONZE_PICKAXE)>trainingUnequipBefore) {
                trainingStylePhase=TrainingStylePhase.NEED_TAB;
                stage="TRAIN_UNEQUIP_PROVED"; return true;
            }
            hold("Unequip dispatch persisted without its pending proof; no retry"); return true;
        }
        if (f.weaponId>=0 || f.count(ItemID.BRONZE_PICKAXE)==0) {
            hold("Unarmed Strength setup lost retained quest pickaxe or weapon changed");
            return true;
        }
        if (trainingStylePhase==TrainingStylePhase.NEED_TAB) {
            if (Rs2Tab.isCurrentTab(InterfaceTab.COMBAT)) {
                trainingStylePhase=TrainingStylePhase.NEED_STYLE;
                stage="TRAIN_COMBAT_TAB_PRESENT"; return true;
            }
            if (!Rs2Tab.switchToCombatOptionsTab()) {
                hold("Combat tab switch rejected"); return true;
            }
            trainingStyleAt=now;
            trainingStylePhase=TrainingStylePhase.TAB_SENT;
            stage="VERIFY_TRAIN_COMBAT_TAB"; return true;
        }
        if (trainingStylePhase==TrainingStylePhase.TAB_SENT) {
            if (Rs2Tab.isCurrentTab(InterfaceTab.COMBAT)) {
                trainingStylePhase=TrainingStylePhase.NEED_STYLE;
                stage="TRAIN_COMBAT_TAB_PROVED"; return true;
            }
            if (trainingStyleAt==0 || now-trainingStyleAt>5000)
                hold("Combat tab did not open after one action");
            else stage="VERIFY_TRAIN_COMBAT_TAB";
            return true;
        }
        if (trainingStylePhase==TrainingStylePhase.NEED_STYLE) {
            if (!Rs2Tab.isCurrentTab(InterfaceTab.COMBAT)) {
                hold("Combat tab disappeared before style selection"); return true;
            }
            TrainingStyleControl control=unarmedAggressiveControl();
            if (control==null || control.index<0 || control.index>3
                || !"unarmed".equals(control.title) || !"kick".equals(control.label)
                || !control.buttonVisible) {
                hold("Visible unarmed Aggressive/Kick control unproved: "
                    +(control==null?"null":control.title+"/"+control.label
                        +" index="+control.index+" category="+control.category));
                return true;
            }
            trainingStyleIndex=control.index;
            trainingStyleCategory=control.category;
            if (f.combatMode==trainingStyleIndex) {
                trainingStylePhase=TrainingStylePhase.READY_PROBE;
                stage="TRAIN_AGGRESSIVE_ALREADY_SELECTED"; return true;
            }
            if (!Rs2Widget.clickWidget(control.button)) {
                hold("Unarmed Kick widget click rejected"); return true;
            }
            trainingStyleAt=now;
            trainingStylePhase=TrainingStylePhase.STYLE_SENT;
            stage="VERIFY_TRAIN_STRENGTH_STYLE"; return true;
        }
        if (trainingStylePhase==TrainingStylePhase.STYLE_SENT) {
            if (f.weaponCategory!=trainingStyleCategory || trainingStyleIndex<0) {
                hold("Weapon style category changed before style proof"); return true;
            }
            if (f.combatMode==trainingStyleIndex) {
                trainingStylePhase=TrainingStylePhase.READY_PROBE;
                stage="TRAIN_STRENGTH_STYLE_VARP_PROVED"; return true;
            }
            if (trainingStyleAt==0 || now-trainingStyleAt>5000)
                hold("Unarmed Kick click did not change COM_MODE on a later tick");
            else stage="VERIFY_TRAIN_STRENGTH_STYLE";
            return true;
        }
        if (trainingStyleIndex<0 || f.weaponCategory!=trainingStyleCategory
            || f.combatMode!=trainingStyleIndex) {
            hold("Unarmed Aggressive style drifted before/after probe"); return true;
        }
        if (trainingStylePhase==TrainingStylePhase.PROBE_SENT) {
            if (trainingProbeHpXp>=0 && trainingProbeStrengthXp>=0
                && trainingProbeAttackXp>=0
                && f.hpXp>trainingProbeHpXp && f.strengthXp>trainingProbeStrengthXp
                && f.attackXp==trainingProbeAttackXp) {
                trainingStylePhase=TrainingStylePhase.DONE;
                stage="TRAIN_STRENGTH_HP_XP_PROVED"; return true;
            }
            hold("Strength probe persisted without pending proof; no repeat"); return true;
        }
        stage="TRAIN_ONE_STRENGTH_PROBE_READY";
        return false;
    }
    /** Bounded live scene probe. This build does not interact with the guardian or pillars. */
    /** Read-only scene inventory. Entry and guardian actions remain disabled. */
    private void guardianReadScene(Frame f) {
        stage="DUNGEON_OBSERVATION_ONLY_NO_PROVEN_ESCAPE";
        long now=System.currentTimeMillis();
        if (!dungeonProbeLogged || now-dungeonProbeAt>=2000) {
            dungeonProbeAt=now;
            StringBuilder scene=new StringBuilder();
            scene.append("stage=").append(questStage(f.varp))
                .append(" raw=").append(f.varp).append(" pos=").append(f.position)
                .append(" knownCave=").append(inKnownCaveInstance(f.position))
                .append(" hp=").append(f.hp).append('/').append(f.maxHp)
                .append(" entryFood=").append(entryFoodCount(f))
                .append(" aggressor=").append(f.unsafeAggressor);
            try {
                String guardian=Microbot.getClientThread().invoke((Supplier<String>)()->{
                    StringBuilder found=new StringBuilder();
                    for (NPC npc:Microbot.getClient().getNpcs()) {
                        if (npc!=null && npc.getId()==10654) found.append(npc.getWorldLocation())
                            .append(":health=").append(npc.getHealthRatio()).append(' ');
                    }
                    return found.toString();
                });
                scene.append(" guardian10654=").append(guardian);
                java.util.Set<WorldPoint> intactPillars=new java.util.HashSet<>();
                for (int id:new int[]{ObjectID.BIM_EXIT,ObjectID.BIM_EXIT_CUTSCENE,
                    ObjectID.BIM_BOSS_ROCK,ObjectID.BIM_BOSS_ROCK_MINED,
                    ObjectID.BIM_BOSS_ROCK_CUTSCENE}) {
                    List<Rs2TileObjectModel> objects=Microbot.getRs2TileObjectCache().query()
                        .withId(id).within(f.position,30).toListOnClientThread();
                    scene.append(" objects").append(id).append('=');
                    for (Rs2TileObjectModel object:objects) {
                        if (id==ObjectID.BIM_BOSS_ROCK)
                            intactPillars.add(object.getWorldLocation());
                        String[] menus=Microbot.getClientThread().invoke((Supplier<String[]>)()->
                            object.getObjectComposition()==null?null
                                :object.getObjectComposition().getActions());
                        scene.append(object.getWorldLocation()).append(':')
                            .append(java.util.Arrays.toString(menus)).append(' ');
                    }
                }
                scene.append(" distinctIntactPillars=").append(intactPillars.size());
            } catch (Exception ex) {
                scene.append(" sceneReadError=").append(ex.getClass().getSimpleName())
                    .append(':').append(ex.getMessage());
            }
            String observed=scene.toString();
            boolean changed=!observed.equals(dungeonProbeScene);
            dungeonProbeScene=observed;
            dungeonProbeLogged=true;
            if (changed) log.warn("[BelowIceMountain] DUNGEON_PROBE_SCENE {}",dungeonProbeScene);
        }
    }
    private void dungeonProbe(Frame f) {
        stage="DUNGEON_PROBE_LOGOUT";
        if (!dungeonProbeLogged) {
            StringBuilder scene=new StringBuilder();
            scene.append("stage=").append(questStage(f.varp))
                .append(" raw=").append(f.varp).append(" pos=").append(f.position)
                .append(" knownCave=").append(inKnownCaveInstance(f.position))
                .append(" hp=").append(f.hp).append('/').append(f.maxHp)
                .append(" entryFood=").append(entryFoodCount(f))
                .append(" aggressor=").append(f.unsafeAggressor);
            try {
                String guardian=Microbot.getClientThread().invoke((Supplier<String>)()->{
                    StringBuilder found=new StringBuilder();
                    for (NPC npc:Microbot.getClient().getNpcs()) {
                        if (npc!=null && npc.getId()==10654) found.append(npc.getWorldLocation())
                            .append(":health=").append(npc.getHealthRatio()).append(' ');
                    }
                    return found.toString();
                });
                scene.append(" guardian10654=").append(guardian);
                for (int id:new int[]{41446,41458,41459,41460}) {
                    List<Rs2TileObjectModel> objects=Microbot.getRs2TileObjectCache().query()
                        .withId(id).within(f.position,30).toListOnClientThread();
                    scene.append(" objects").append(id).append('=');
                    for (Rs2TileObjectModel object:objects) {
                        String[] menus=Microbot.getClientThread().invoke((Supplier<String[]>)()->
                            object.getObjectComposition()==null?null
                                :object.getObjectComposition().getActions());
                        scene.append(object.getWorldLocation()).append(':')
                            .append(java.util.Arrays.toString(menus)).append(' ');
                    }
                }
            } catch (Exception ex) {
                scene.append(" sceneReadError=").append(ex.getClass().getSimpleName())
                    .append(':').append(ex.getMessage());
            }
            dungeonProbeScene=scene.toString();
            dungeonProbeLogged=true;
            log.warn("[BelowIceMountain] DUNGEON_PROBE_SCENE {}",dungeonProbeScene);
        }
        safetyLogoutIssued=true;
        safetyLogoutAt=System.currentTimeMillis();
        log.warn("[BelowIceMountain] DUNGEON_PROBE_LOGOUT_REQUEST stage={} hp={}/{} pos={}",
            questStage(f.varp),f.hp,f.maxHp,f.position);
        Rs2Player.logout();
    }
    private GuardianScene guardianScene(Frame f) {
        GuardianScene scene=new GuardianScene();
        try {
            int[] guardian=Microbot.getClientThread().invoke((Supplier<int[]>)()->{
                for (NPC npc:Microbot.getClient().getNpcs())
                    if (npc!=null && npc.getId()==NpcID.BIM_GOLEM_BOSS)
                        return new int[]{1,npc.getHealthRatio()};
                return new int[]{0,-1};
            });
            scene.guardianPresent=guardian[0]==1;
            scene.guardianRatio=guardian[1];
            scene.exit=Microbot.getRs2TileObjectCache().query()
                .withId(ObjectID.BIM_EXIT).within(f.position,30).nearestOnClientThread();
            for (Rs2TileObjectModel object:Microbot.getRs2TileObjectCache().query()
                .withId(ObjectID.BIM_BOSS_ROCK).within(f.position,30).toListOnClientThread())
                scene.pillars.put(object.getWorldLocation(),object);
            for (Rs2TileObjectModel object:Microbot.getRs2TileObjectCache().query()
                .withId(ObjectID.BIM_BOSS_ROCK_MINED).within(f.position,30).toListOnClientThread())
                scene.broken.add(object.getWorldLocation());
        } catch (Exception ex) {
            log.error("[BelowIceMountain] GUARDIAN_SCENE_READ_FAILED pos={} error={}",
                f.position,ex.toString());
        }
        return scene;
    }
    private static String observedAction(Rs2TileObjectModel object,String... wanted) {
        if (object==null) return null;
        String[] actions=Microbot.getClientThread().invoke((Supplier<String[]>)()->
            object.getObjectComposition()==null?null:object.getObjectComposition().getActions());
        if (actions==null) return null;
        for (String action:actions) if (action!=null)
            for (String choice:wanted) if (action.equalsIgnoreCase(choice)) return action;
        return null;
    }
    private int guardianFood(Frame f) {
        for (int id:ENTRY_FOOD) if (f.count(id)>0) return id;
        return 0;
    }
    private void guardianTick(Frame f) {
        if (!guardianActive) {
            guardianActive=true; guardianActiveAt=System.currentTimeMillis();
            guardianEntryRegion=f.position==null?-1:f.position.getRegionID();
        }
        if (route!=null) { cancelRoute(); stage="WAIT_PREENTRY_ROUTE_STOP"; return; }
        guardianReadScene(f); // read-only scene history; no click or logout
        GuardianScene scene=guardianScene(f);
        guardianObservedPillars.addAll(scene.pillars.keySet());
        int step=questStage(f.varp);
        // A removed entry flag cannot strand an already committed encounter.
        if (!guardianActionsAllowed()) guardianRetreat=true;
        if (step>=40 && !guardianPostBossExitReset) {
            guardianPostBossExitReset=true;
            guardianRetreat=true;
            guardianExitFailed=false;
            guardianExitFailures=0;
        }
        if (f.hp<=Math.max(8,f.maxHp*3/5) || entryFoodCount(f)<2)
            guardianRetreat=true;
        if (guardianRetreat && !guardianExitFailed && guardianMoveTarget!=null
            && !guardianMoveForExit) {
            guardianMoveTarget=null;
            stage="GUARDIAN_ABORT_PILLAR_WALK_FOR_EXIT";
            return;
        }
        if (step==30 && !scene.guardianPresent && scene.pillars.isEmpty()
            && System.currentTimeMillis()-guardianActiveAt<10000) {
            stage="WAIT_GUARDIAN_CUTSCENE";
            return;
        }
        if (step==30 && System.currentTimeMillis()-guardianActiveAt>=10000)
            guardianRetreat=true;
        if (guardianPending!=null) {
            if (f.hp<=6 && guardianPending.action!=GuardianAction.EAT
                && guardianFood(f)!=0 && guardianEatUnproved<2) {
                log.warn("[BelowIceMountain] GUARDIAN_ACTION_INTERRUPTED_FOR_FOOD action={} tile={}",
                    guardianPending.action,guardianPending.tile);
                guardianPending=null;
                guardianMoveTarget=null;
                guardianEat(f,scene);
                return;
            }
            verifyGuardianAction(f,scene);
            return;
        }
        if (f.hp<=Math.max(8,f.maxHp*3/5) && guardianFood(f)!=0
            && guardianEatUnproved<2) { guardianEat(f,scene); return; }
        if (guardianMoveTarget!=null) { advanceGuardianWalk(f); return; }
        if (guardianRetreat && !guardianExitFailed) {
            tryGuardianExit(f,scene);
            return;
        }
        if (step>=40) {
            guardianLastResort(f,scene,"stage40 exit unavailable");
            return;
        }
        if (guardianProvedPillars.size()>=4 || scene.pillars.isEmpty()) {
            if (guardianProvedPillars.size()>=4 && guardianAllPillarsAt==0)
                guardianAllPillarsAt=System.currentTimeMillis();
            if (guardianAllPillarsAt>0 && System.currentTimeMillis()-guardianAllPillarsAt<8000) {
                stage="VERIFY_GUARDIAN_STAGE_ADVANCE";
                return;
            }
            if (!guardianRetreat && System.currentTimeMillis()-guardianActiveAt<10000) {
                stage="WAIT_FOUR_GUARDIAN_PILLARS";
                return;
            }
            if (!guardianExitFailed) {
                guardianRetreat=true;
                stage="GUARDIAN_TRY_EXIT_AFTER_PILLARS";
                return;
            }
            guardianLastResort(f,scene,"no intact reachable pillars or stage advance");
            return;
        }
        tryGuardianPillar(f,scene);
    }
    private void guardianEat(Frame f,GuardianScene scene) {
        int id=guardianFood(f);
        if (id==0) { guardianRetreat=true; stage="GUARDIAN_NO_FOOD"; return; }
        boolean accepted=Rs2Inventory.interact(id,"Eat");
        if (!accepted) {
            guardianEatUnproved++;
            guardianRetreat=true;
            stage="GUARDIAN_EAT_REJECTED";
            return;
        }
        guardianPending=new GuardianPending(GuardianAction.EAT,null,id,f,scene.guardianRatio);
        stage="VERIFY_GUARDIAN_EAT";
    }
    private void verifyGuardianAction(Frame f,GuardianScene scene) {
        GuardianPending p=guardianPending;
        if (p.action==GuardianAction.EAT && f.count(p.item)<p.itemCount && f.hp>p.hp) {
            guardianPending=null; guardianEatUnproved=0;
            stage="GUARDIAN_FOOD_PROVED"; return;
        }
        if (p.action==GuardianAction.EXIT && guardianEntryRegion>=0
            && f.position.getRegionID()!=guardianEntryRegion
            && overworldPrepArea(f.position) && f.unsafeAggressor.isEmpty()) {
            guardianPending=null; guardianActive=false;
            if (questStage(f.varp)>=40) {
                stage40ExitProved=true;
                stage="STAGE40_EXIT_PROVED_APPROACH_ENTRANCE";
                log.info("[BelowIceMountain] STAGE40_EXIT_PROVED pos={} entryRegion={}",
                    f.position,guardianEntryRegion);
            } else hold("Guardian emergency exit proved; no automatic re-entry at "
                +f.position);
            return;
        }
        if (p.action==GuardianAction.MINE) {
            if (questStage(f.varp)>p.questStage) {
                guardianPending=null; guardianRetreat=true;
                stage="GUARDIAN_QUEST_STAGE_ADVANCED"; return;
            }
            if (scene.broken.contains(p.tile)
                && (f.miningXp>p.miningXp
                    || (p.guardianRatio>=0 && scene.guardianRatio>=0
                        && scene.guardianRatio<p.guardianRatio))) {
                guardianProvedPillars.add(p.tile);
                guardianPending=null;
                log.info("[BelowIceMountain] GUARDIAN_PILLAR_PROVED tile={} count={} xp={} guardianRatio={}",
                    p.tile,guardianProvedPillars.size(),f.miningXp,scene.guardianRatio);
                stage="GUARDIAN_PILLAR_PROVED"; return;
            }
        }
        // The object update can arrive before XP/guardian HP. Wait longer and
        // never click this already-broken tile a second time.
        long timeout=p.action==GuardianAction.EAT?2500
            : p.action==GuardianAction.MINE && scene.broken.contains(p.tile)?10000:6000;
        if (System.currentTimeMillis()-p.at<timeout) {
            stage="VERIFY_GUARDIAN_"+p.action;
            return;
        }
        guardianPending=null;
        if (p.action==GuardianAction.EXIT) {
            guardianExitFailures++;
            guardianExitFailed=true;
            guardianRetreat=true;
            log.error("[BelowIceMountain] GUARDIAN_EXIT_UNPROVED tile={} hp={} next=pillars",
                p.tile,f.hp);
        } else if (p.action==GuardianAction.MINE) {
            if (scene.broken.contains(p.tile)) guardianUncorroboratedPillars.add(p.tile);
            guardianPillarFailures.merge(p.tile,1,Integer::sum);
            log.warn("[BelowIceMountain] GUARDIAN_PILLAR_UNPROVED tile={} broken={} xp={} ratio={}",
                p.tile,scene.broken.contains(p.tile),f.miningXp,scene.guardianRatio);
        } else {
            guardianEatUnproved++;
            guardianRetreat=true;
            log.error("[BelowIceMountain] GUARDIAN_EAT_UNPROVED item={} before={}/{} now={}/{}",
                p.item,p.hp,p.itemCount,f.hp,f.count(p.item));
        }
        stage="GUARDIAN_RESCAN_AFTER_UNPROVED_"+p.action;
    }
    private void tryGuardianExit(Frame f,GuardianScene scene) {
        Rs2TileObjectModel exit=scene.exit;
        String menu=observedAction(exit,"Exit","Climb-up","Leave");
        if (exit==null || menu==null) {
            guardianExitFailed=true;
            log.warn("[BelowIceMountain] GUARDIAN_EXIT_UNAVAILABLE exit={} menu={} next=pillars",
                exit==null?"absent":exit.getWorldLocation(),menu);
            stage="GUARDIAN_EXIT_UNAVAILABLE_TRY_PILLARS";
            return;
        }
        WorldPoint tile=exit.getWorldLocation();
        if (!near(f.position,tile,2)) {
            if (!startGuardianWalk(f,tile,"EXIT")) {
                guardianExitFailed=true;
                stage="GUARDIAN_EXIT_UNREACHABLE_TRY_PILLARS";
            }
            return;
        }
        boolean accepted=exit.click(menu);
        if (!accepted) {
            guardianExitFailures++;
            guardianExitFailed=true;
            stage="GUARDIAN_EXIT_CLICK_REJECTED_TRY_PILLARS";
            return;
        }
        guardianPending=new GuardianPending(GuardianAction.EXIT,tile,0,f,scene.guardianRatio);
        stage="VERIFY_GUARDIAN_EXIT";
    }
    private void tryGuardianPillar(Frame f,GuardianScene scene) {
        List<WorldPoint> tiles=new ArrayList<>(scene.pillars.keySet());
        tiles.sort((a,b)->Integer.compare(distance(f.position,a),distance(f.position,b)));
        for (WorldPoint tile:tiles) {
            if (guardianProvedPillars.contains(tile)
                || guardianUncorroboratedPillars.contains(tile)
                || guardianPillarFailures.getOrDefault(tile,0)>=2) continue;
            Rs2TileObjectModel pillar=scene.pillars.get(tile);
            String menu=observedAction(pillar,"Mine");
            if (menu==null) continue;
            if (!near(f.position,tile,2)) {
                if (startGuardianWalk(f,tile,"PILLAR")) return;
                guardianPillarFailures.merge(tile,1,Integer::sum);
                continue;
            }
            boolean accepted=pillar.click(menu);
            if (!accepted) {
                guardianPillarFailures.merge(tile,1,Integer::sum);
                stage="GUARDIAN_PILLAR_CLICK_REJECTED_RESCAN";
                return;
            }
            guardianPending=new GuardianPending(GuardianAction.MINE,tile,0,f,scene.guardianRatio);
            stage="VERIFY_GUARDIAN_PILLAR";
            return;
        }
        guardianLastResort(f,scene,"all visible pillars lack Mine/reachability/proof");
    }
    private boolean startGuardianWalk(Frame f,WorldPoint objectTile,String reason) {
        List<WorldPoint> approaches=new ArrayList<>();
        for (int dx=-1;dx<=1;dx++) for (int dy=-1;dy<=1;dy++) {
            if (dx==0 && dy==0) continue;
            WorldPoint tile=new WorldPoint(objectTile.getX()+dx,objectTile.getY()+dy,
                objectTile.getPlane());
            if (guardianBlockedApproaches.contains(tile)) continue;
            try {
                if (Rs2Walker.isWalkableInCollisionMap(tile) && Rs2Walker.canReach(tile))
                    approaches.add(tile);
            } catch (Exception ex) {
                log.warn("[BelowIceMountain] GUARDIAN_APPROACH_CHECK_FAILED tile={} {}",tile,ex.toString());
            }
        }
        approaches.sort((a,b)->Integer.compare(distance(f.position,a),distance(f.position,b)));
        if (approaches.isEmpty()) return false;
        guardianMoveTarget=approaches.get(0);
        guardianMoveForExit="EXIT".equals(reason);
        guardianMoveOrigin=f.position;
        guardianWalkPending=false;
        guardianMoveAt=System.currentTimeMillis();
        log.info("[BelowIceMountain] GUARDIAN_APPROACH reason={} target={} from={}",
            reason,guardianMoveTarget,f.position);
        stage="GUARDIAN_APPROACH_"+reason;
        return true; // dispatch one walkStep on the next tick
    }
    private void advanceGuardianWalk(Frame f) {
        if (near(f.position,guardianMoveTarget,0)) {
            guardianMoveTarget=null;
            guardianWalkPending=false;
            stage="GUARDIAN_APPROACH_PROVED";
            return;
        }
        if (guardianWalkPending) {
            if (!f.position.equals(guardianMoveOrigin)) {
                // This later frame proves the previous step moved us. A further step,
                // if needed, is dispatched on a subsequent tick.
                guardianMoveOrigin=f.position;
                guardianWalkPending=false;
                stage="GUARDIAN_WALK_PROGRESS_PROVED";
                return;
            }
            if (System.currentTimeMillis()-guardianMoveAt>4000) {
                guardianBlockedApproaches.add(guardianMoveTarget);
                log.warn("[BelowIceMountain] GUARDIAN_APPROACH_STALLED target={} pos={} hp={}",
                    guardianMoveTarget,f.position,f.hp);
                guardianMoveTarget=null;
                guardianWalkPending=false;
                stage="GUARDIAN_APPROACH_RESCAN";
                return;
            }
            stage="VERIFY_GUARDIAN_WALK_PROGRESS";
            return;
        }
        guardianMoveOrigin=f.position;
        guardianMoveAt=System.currentTimeMillis();
        Rs2Walker.walkStep(guardianMoveTarget,0);
        guardianWalkPending=true;
        stage="VERIFY_GUARDIAN_WALK_PROGRESS";
    }
    private void guardianLastResort(Frame f,GuardianScene scene,String reason) {
        guardianRetreat=true;
        if (guardianLogoutAttempts<2 && !safetyLogoutIssued) {
            safetyLogoutIssued=true;
            safetyLogoutAt=System.currentTimeMillis();
            Rs2Player.logout();
            stage="VERIFY_GUARDIAN_LAST_RESORT_LOGOUT";
            log.error("[BelowIceMountain] GUARDIAN_LAST_RESORT_LOGOUT reason={} hp={}/{} pos={}",
                reason,f.hp,f.maxHp,f.position);
            return;
        }
        stage="UNRESOLVED_GUARDIAN_EXPOSURE";
        if (!guardianExposureReported) {
            guardianExposureReported=true;
            log.error("[BelowIceMountain] UNRESOLVED_GUARDIAN_EXPOSURE reason={} hp={}/{} food={} "
                    +"exitFailures={} logoutFailures={} pillarsProved={} intact={} guardianPresent={} pos={}",
                reason,f.hp,f.maxHp,entryFoodCount(f),guardianExitFailures,guardianLogoutAttempts,
                guardianProvedPillars.size(),scene.pillars.size(),scene.guardianPresent,f.position);
        }
    }
    private void talkNpc(Frame f,String key,int id,WorldPoint at) {
        if (walk(f,key,at,4)) return;
        Rs2NpcModel npc=npcAt(id,at);
        if (npc==null) { hold("NPC "+id+" absent at "+at+" for "+key); return; }
        issue(key,Proof.DIALOGUE_CHANGED,f,0,15000,()->npc.click("Talk-to"));
    }
    private Rs2NpcModel npcAt(int id,WorldPoint at) {
        return Microbot.getRs2NpcCache().query().withId(id).within(at,9).nearestOnClientThread();
    }
    private void issue(String key,Proof proof,Frame before,int item,long timeout,BooleanSupplier action) {
        int questStage=questStage(before.varp);
        if (key.startsWith("supply:")) {
            if (++supplyActions>45) { hold("Marley supply action budget exhausted"); return; }
        } else if (questStage>=15 && questStage<=30 && !key.startsWith("dialogue:")
            && !key.startsWith("train:")) {
            int count=laterActionCounts.merge(key,1,Integer::sum);
            if (count>2) { hold("Repeated stage action without milestone: "+key+" x"+count); return; }
        }
        boolean accepted=action.getAsBoolean();
        if (!accepted && key.startsWith("entrance:")) {
            hold("Entrance action rejected without state change: "+key
                +" at "+before.position);
            return;
        }
        if (!accepted && key.startsWith("train:")) {
            if ("train:chicken".equals(key) && trainingTargetIndex>=0) {
                trainingAvoidIndex=trainingTargetIndex;
                trainingAvoidUntil=System.currentTimeMillis()+60000;
                trainingEncounterAt=0;
                if (++trainingUnproved<3) {
                    stage="RESCAN_ALTERNATE_CHICKEN";
                    log.warn("[BelowIceMountain] CHICKEN_DISPATCH_REJECTED avoidIndex={} attempts={} pos={}",
                        trainingAvoidIndex,trainingUnproved,before.position);
                    return;
                }
            }
            hold("Training action rejected without state change: "+key+" at "+before.position);
            return;
        }
        if (accepted && key.startsWith("entrance:")) entranceAt=System.currentTimeMillis();
        pending=new Pending(key,proof,before,item,timeout);
        log.info("[BelowIceMountain] ACTION key={} accepted={} varp={} checkal={} pos={}",
            key,accepted,before.varp,before.checkal,before.position);
    }
    private void verify(Frame f) {
        Pending p=pending;
        if (proved(p,f)) {
            log.info("[BelowIceMountain] PROVED key={} varp={} checkal={} pos={}",
                p.key,f.varp,f.checkal,f.position);
            if (p.key.startsWith("supply:buy:"+ItemID.RAW_BEEF+":")) beefBought++;
            if (p.key.startsWith("supply:buy:"+ItemID.POT_FLOUR+":")) flourBought++;
            if ("train:unequip-bronze-pickaxe".equals(p.key))
                trainingStylePhase=TrainingStylePhase.NEED_TAB;
            if ("train:strength-probe".equals(p.key))
                trainingStylePhase=TrainingStylePhase.DONE;
            if ("train:chicken".equals(p.key) || "train:strength-probe".equals(p.key)) {
                trainingXpActions++;
                trainingUnproved=0;
                trainingInteriorRepositionAttempted=false;
                trainingAvoidIndex=trainingTargetIndex;
                trainingAvoidUntil=System.currentTimeMillis()+2500;
            }
            pending=null; failures.remove(p.key);
            nextAt=System.currentTimeMillis()
                + ("train:chicken".equals(p.key)?0:350);
            return;
        }
        if ("train:strength-probe".equals(p.key)
            && (f.hpXp>p.before.hpXp || f.attackXp>p.before.attackXp)) {
            pending=null;
            hold("Strength probe produced HP/Attack XP without isolated Strength+HP proof: "
                +"hp="+p.before.hpXp+"->"+f.hpXp
                +" strength="+p.before.strengthXp+"->"+f.strengthXp
                +" attack="+p.before.attackXp+"->"+f.attackXp);
            return;
        }
        if (System.currentTimeMillis()-p.at<p.timeout) { stage="VERIFY_"+p.key; return; }
        if ("train:chicken".equals(p.key) && trainingTargetIndex>=0) {
            pending=null;
            trainingAvoidIndex=trainingTargetIndex;
            trainingAvoidUntil=System.currentTimeMillis()+60000;
            trainingEncounterAt=0;
            if (++trainingUnproved>=3) {
                hold("Three unproved chicken targets; xp="+f.hpXp
                    +" hp="+f.hp+"/"+f.maxHp+" pos="+f.position);
                return;
            }
            nextAt=System.currentTimeMillis()+1500;
            stage="RESCAN_ALTERNATE_CHICKEN";
            log.warn("[BelowIceMountain] CHICKEN_UNPROVED avoidIndex={} attempts={} xp={} pos={}",
                trainingAvoidIndex,trainingUnproved,f.hpXp,f.position);
            return;
        }
        if (p.key.startsWith("train:")) {
            pending=null;
            hold("Training action unproved; no repeat: "+p.key
                +" xp="+f.hpXp+" hp="+f.hp+"/"+f.maxHp);
            return;
        }
        int attempts=failures.merge(p.key,1,Integer::sum);
        pending=null;
        if (attempts>=2) hold("Unproved "+p.key+" x"+attempts+" at "+f.position
            +" varp="+f.varp+" checkal="+f.checkal);
        else log.warn("[BelowIceMountain] RETRY {} {}/2",p.key,attempts);
    }
    private boolean proved(Pending p,Frame f) {
        if (!"LOGGED_IN".equals(f.gameState) || f.position==null) return false;
        switch (p.proof) {
            case QUEST_ADVANCED: return f.varp!=p.before.varp || !f.questState.equals(p.before.questState);
            case VARBIT_CHANGED: return f.checkal!=p.before.checkal || f.varp!=p.before.varp;
            case DIALOGUE_CHANGED: return f.varp!=p.before.varp || f.checkal!=p.before.checkal
                || f.marley!=p.before.marley || f.burntof!=p.before.burntof
                || (!f.inDialogue && !f.hasContinue && p.before.inDialogue)
                || !plain(f.dialogue).equals(plain(p.before.dialogue))
                || !f.options.equals(p.before.options);
            case ITEM_GAINED: return f.count(p.item)>p.before.count(p.item);
            case ITEM_CONSUMED: return f.count(p.item)<p.before.count(p.item);
            case WEAPON_EQUIPPED: return f.weaponId==p.item
                && f.count(p.item)<p.before.count(p.item);
            case WEAPON_UNEQUIPPED: return f.weaponId<0
                && f.count(p.item)>p.before.count(p.item)
                && f.pickaxes>=p.before.pickaxes;
            case STRENGTH_HP_XP_GAINED: return f.strengthXp>p.before.strengthXp
                && f.hpXp>p.before.hpXp && f.attackXp==p.before.attackXp;
            case HP_XP_GAINED: return f.hpXp>p.before.hpXp;
            case FOOD_HEAL: return f.hp>p.before.hp
                && f.count(p.item)<p.before.count(p.item);
            case GE_CLOSED: return p.before.geOpen && !f.geOpen;
            case PRODUCTION_OPEN: return f.production || f.count(p.item)<p.before.count(p.item);
            case ITEM_GAINED_OR_DIALOGUE: return f.count(p.item)>p.before.count(p.item)
                || dialogueChanged(p.before,f);
            case ITEM_USED_OR_DIALOGUE: return f.count(p.item)<p.before.count(p.item)
                || dialogueChanged(p.before,f);
            case INVENTORY_SHED: return f.inventorySlots<p.before.inventorySlots;
            case BANK_TOGGLED: return f.bankOpen!=p.before.bankOpen;
            case SHOP_TOGGLED: return f.shopOpen!=p.before.shopOpen;
            case ISLAND_EXIT: return p.before.position.getX()<1700 && f.position.getX()>2500;
            case FLEX_OPEN: return Rs2Tab.isCurrentTab(InterfaceTab.EMOTES);
            case EMOTE_SCROLLED: return f.emoteScroll>=0 && f.emoteScroll!=p.before.emoteScroll;
            case DUNGEON_ENTERED: return questStage(f.varp)>=30
                && overworldPrepArea(p.before.position)
                && inKnownCaveInstance(f.position);
            default: return false;
        }
    }
    private static boolean dialogueChanged(Frame before,Frame after) {
        return after.varp!=before.varp || after.checkal!=before.checkal
            || after.marley!=before.marley || after.burntof!=before.burntof
            || after.inDialogue!=before.inDialogue || after.hasContinue!=before.hasContinue
            || !plain(after.dialogue).equals(plain(before.dialogue))
            || !after.options.equals(before.options);
    }
    private boolean walk(Frame f,String key,WorldPoint target,int radius) {
        if (near(f.position,target,radius)) {
            if (route!=null) { cancelRoute(); return true; }
            return false;
        }
        if (cancellingRoute!=null) { stage="WAIT_ROUTE_STOP"; return true; }
        if (route!=null && (!route.key.equals(key) || !route.target.equals(target))) {
            cancelRoute(); return true;
        }
        if (route==null) {
            Route r=new Route(key,target,f.position,radius); route=r;
            r.worker=new Thread(()->{
                try { Rs2Walker.walkWithStateUntil(r.target,r.radius,
                    ()->stopped || Thread.currentThread().isInterrupted()
                        || System.currentTimeMillis()-r.at>20000); }
                finally { r.doneAt=System.currentTimeMillis(); r.done=true; }
            },"BIM-route");
            r.worker.setDaemon(true); r.worker.start();
            log.info("[BelowIceMountain] ROUTE {} {} -> {}",key,f.position,target);
        }
        stage="WALK_"+key;
        if (System.currentTimeMillis()-route.at>50000
            || (route.done && System.currentTimeMillis()-route.doneAt>3000)) {
            Route finished=route;
            cancelRoute();
            int before=distance(finished.start,target), after=distance(f.position,target);
            int needed=before<=15 ? 1 : 5;
            if (before-after>=needed) {
                failures.remove("walk:"+key);
                log.info("[BelowIceMountain] ROUTE_PROGRESS {} distance {} -> {}",key,before,after);
            } else {
                int n=failures.merge("walk:"+key,1,Integer::sum);
                if (n>=2) hold("Route "+key+" stalled x"+n+" at "+f.position+" target="+target);
            }
        }
        return true;
    }
    private void cancelRoute() {
        Route current=route;
        if (current==null) return;
        route=null; cancellingRoute=current; routeClearAt=System.currentTimeMillis();
        if (current.worker!=null && current.worker.isAlive()) current.worker.interrupt();
        routeClear=new Thread(()->Rs2Walker.clearWalkingRoute("belowicemountain:cancel"),"BIM-clear-route");
        routeClear.setDaemon(true); routeClear.start();
    }
    private void hold(String reason) {
        if (error.isEmpty()) log.error("[BelowIceMountain] HOLD {}",reason);
        error=reason; stage="HOLD"; pending=null; cancelRoute();
    }
    private static boolean near(WorldPoint a,WorldPoint b,int radius) {
        return a!=null && b!=null && a.getPlane()==b.getPlane()
            && Math.max(Math.abs(a.getX()-b.getX()),Math.abs(a.getY()-b.getY()))<=radius;
    }
    private static int distance(WorldPoint a,WorldPoint b) {
        return a==null || b==null || a.getPlane()!=b.getPlane() ? Integer.MAX_VALUE
            : Math.abs(a.getX()-b.getX())+Math.abs(a.getY()-b.getY());
    }
    private static int questStage(int raw) { return raw & 0x3f; }
    private boolean armed() {
        if (config != null && config.allowActions()
            && config.approvedPid() == ProcessHandle.current().pid()
            && config.approvedBuild() == BUILD_NUMBER
            && classHash.matches("[a-f0-9]{64}")
            && classHash.equalsIgnoreCase(config.approvedSha256())) return true;
        if (!Files.isRegularFile(CONTROL) || !classHash.matches("[a-f0-9]{64}")) return false;
        Properties p=new Properties();
        try (InputStream in=Files.newInputStream(CONTROL)) { p.load(in); }
        catch (Exception ex) { log.warn("[BelowIceMountain] control read: {}",ex.toString()); return false; }
        return "true".equalsIgnoreCase(p.getProperty("enableActions","false"))
            && Long.toString(ProcessHandle.current().pid()).equals(p.getProperty("expectedPid"))
            && Integer.toString(BUILD_NUMBER).equals(p.getProperty("expectedBuild"))
            && classHash.equalsIgnoreCase(p.getProperty("expectedClassSha",""));
    }
    private static String classSha() {
        String entryName = BelowIceMountainScript.class.getName().replace('.', '/') + ".class";
        try {
            java.net.URL location = BelowIceMountainScript.class.getProtectionDomain()
                .getCodeSource().getLocation();
            try (JarFile archive = new JarFile(Paths.get(location.toURI()).toFile())) {
                JarEntry entry = archive.getJarEntry(entryName);
                if (entry == null) return "UNKNOWN";
                try (InputStream in = archive.getInputStream(entry)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[8192]; int size;
            while ((size = in.read(bytes)) > 0) digest.update(bytes, 0, size);
            StringBuilder result = new StringBuilder();
            for (byte b : digest.digest()) result.append(String.format("%02x", b & 255));
            return result.toString();
                }
            }
        } catch (Exception ex) { return "UNKNOWN"; }
    }
    private void putSkillTelemetry(Properties p,Frame f) {
        boolean prep=f!=null && "LOGGED_IN".equals(f.gameState)
            && "IN_PROGRESS".equals(f.questState)
            && questStage(f.varp)==30 && f.maxHp>0 && f.maxHp<20;
        p.setProperty("prepActive",Boolean.toString(prep));
        p.setProperty("prepActivity",prep?"Training Hitpoints on chickens":"");
        p.setProperty("prepSkill",prep?Skill.HITPOINTS.name():"");
        p.setProperty("prepCurrent",prep?Integer.toString(f.maxHp):"-1");
        p.setProperty("prepTarget",prep?"20":"-1");
        p.setProperty("prepReason",prep?"Preparing for dungeon combat":"");
        p.setProperty("prepCurrentXp",prep?Integer.toString(f.hpXp):"-1");
        p.setProperty("prepTargetXp",prep?Integer.toString(Experience.getXpForLevel(20)):"-1");
        if (prep) p.setProperty("skill.HITPOINTS.target","20");
        if (f==null || !"LOGGED_IN".equals(f.gameState)) return;
        try {
            Map<String,String> skills=Microbot.getClientThread().invoke(
                (Supplier<Map<String,String>>)(()->{
                    Map<String,String> values=new HashMap<>();
                    Client client=Microbot.getClient();
                    if (client==null) return values;
                    for (Skill skill:Skill.values()) {
                        if (skill==Skill.OVERALL) continue;
                        try {
                            int level=client.getRealSkillLevel(skill);
                            int xp=client.getSkillExperience(skill);
                            if (level<=0 || xp<0) continue;
                            String key="skill."+skill.name();
                            values.put(key+".level",Integer.toString(level));
                            values.put(key+".xp",Integer.toString(xp));
                        } catch (RuntimeException ignored) {
                            // A private server may not expose every enum skill.
                        }
                    }
                    return values;
                }));
            if (skills!=null) for (Map.Entry<String,String> value:skills.entrySet())
                p.setProperty(value.getKey(),value.getValue());
        } catch (RuntimeException ex) {
            p.setProperty("skillTelemetryError",ex.getClass().getSimpleName());
        }
    }
    private void writeStatus(Frame f) {
        try {
            Files.createDirectories(STATUS.getParent());
            Properties p = new Properties();
            p.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
            p.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
            p.setProperty("build", Integer.toString(BUILD_NUMBER));
            p.setProperty("sha256", classHash);
            p.setProperty("observationOnly", "false");
            p.setProperty("guardianActions", Boolean.toString(guardianActionsAllowed()));
            p.setProperty("guardianActive", Boolean.toString(guardianActive));
            p.setProperty("unattendedDungeonEntry", Boolean.toString(UNATTENDED_ESCAPE_VERIFIED));
            p.setProperty("actionsArmed", Boolean.toString(armed()));
            p.setProperty("stage", stage);
            p.setProperty("error", error);
            p.setProperty("levelUpTabCue",levelUpTabCue.result().name());
            p.setProperty("levelUpTabSkill",String.valueOf(levelUpTabCue.skill()));
            p.setProperty("levelUpTabLevel",levelUpTabCue.previousLevel()+"->"+levelUpTabCue.newLevel());
            p.setProperty("levelUpGuideSeen",Boolean.toString(levelUpTabCue.guideSeen()));
            p.setProperty("levelUpInventorySelected",Boolean.toString(f!=null
                && "LOGGED_IN".equals(f.gameState)
                && Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)));
            p.setProperty("levelUpBaselineReady",Boolean.toString(levelUpTabCue.baselineReady()));
            p.setProperty("gameState", f == null ? "UNKNOWN" : f.gameState);
            p.setProperty("loginIndex", f == null ? "-1" : Integer.toString(f.loginIndex));
            p.setProperty("world", f == null ? "-1" : Integer.toString(f.world));
            p.setProperty("position", f == null ? "UNKNOWN" : String.valueOf(f.position));
            p.setProperty("questState", f == null ? "UNKNOWN" : f.questState);
            p.setProperty("questVarp2951", f == null ? "-1" : Integer.toString(f.varp));
            p.setProperty("inDialogue",Boolean.toString(f!=null && f.inDialogue));
            p.setProperty("hasContinue",Boolean.toString(f!=null && f.hasContinue));
            p.setProperty("dialogueText",f==null?"":plain(f.dialogue).substring(0,
                Math.min(200,plain(f.dialogue).length())));
            p.setProperty("dialogueOptions",f==null?"":f.options.toString());
            p.setProperty("checkalVarbit12065",f==null?"-1":Integer.toString(f.checkal));
            p.setProperty("marleyVarbit12064",f==null?"-1":Integer.toString(f.marley));
            p.setProperty("burntofVarbit12066",f==null?"-1":Integer.toString(f.burntof));
            p.setProperty("questPointsVarp101", f == null ? "-1" : Integer.toString(f.questPoints));
            p.setProperty("miningLevel", f == null ? "-1" : Integer.toString(f.mining));
            p.setProperty("attackLevel", f == null ? "-1" : Integer.toString(f.attack));
            p.setProperty("weaponId", f == null ? "-1" : Integer.toString(f.weaponId));
            p.setProperty("hp", f == null ? "-1" : f.hp + "/" + f.maxHp);
            p.setProperty("hitpointsXp",f==null?"-1":Integer.toString(f.hpXp));
            putSkillTelemetry(p,f);
            p.setProperty("trainingXpActions",Integer.toString(trainingXpActions));
            p.setProperty("trainingStylePhase",trainingStylePhase.name());
            p.setProperty("trainingProbeStrengthXp",Integer.toString(trainingProbeStrengthXp));
            p.setProperty("trainingProbeHpXp",Integer.toString(trainingProbeHpXp));
            p.setProperty("trainingStyleRestoreInvalid",Boolean.toString(trainingStyleRestoreInvalid));
            p.setProperty("trainingStyleIndex",Integer.toString(trainingStyleIndex));
            p.setProperty("combatMode",f==null?"-1":Integer.toString(f.combatMode));
            p.setProperty("weaponCategory",f==null?"-1":Integer.toString(f.weaponCategory));
            p.setProperty("trainingUnproved",Integer.toString(trainingUnproved));
            p.setProperty("trainingTargetIndex",Integer.toString(trainingTargetIndex));
            p.setProperty("trainingAvoidIndex",Integer.toString(trainingAvoidIndex));
            p.setProperty("trainingFarmApproachComplete",Boolean.toString(trainingFarmApproachComplete));
            p.setProperty("trainingInteriorRepositionAttempted",Boolean.toString(trainingInteriorRepositionAttempted));
            p.setProperty("trainingRetreat",Boolean.toString(trainingRetreat));
            p.setProperty("trainingLastHp",Integer.toString(trainingLastHp));
            p.setProperty("trainingThreatUntil",Long.toString(trainingThreatUntil));
            p.setProperty("trainingLastTargetGapMs",Long.toString(trainingLastTargetGapMs));
            p.setProperty("trainingSawChickenCombat",Boolean.toString(trainingSawChickenCombat));
            p.setProperty("randomEventName",f==null?"":f.randomEventName);
            p.setProperty("randomEventId",f==null?"-1":Integer.toString(f.randomEventId));
            p.setProperty("randomEventIndex",f==null?"-1":Integer.toString(f.randomEventIndex));
            p.setProperty("randomDismissAttempts",Integer.toString(randomDismissAttempts));
            p.setProperty("randomDismissAccepted",Boolean.toString(randomDismissAccepted));
            p.setProperty("dungeonProbeLogged",Boolean.toString(dungeonProbeLogged));
            p.setProperty("dungeonProbeScene",dungeonProbeScene);
            p.setProperty("combatLevel", f == null ? "-1" : Integer.toString(f.combatLevel));
            p.setProperty("pickaxeCount", f == null ? "0" : Integer.toString(f.pickaxes));
            p.setProperty("foodCount", f == null ? "0" : Integer.toString(f.food));
            p.setProperty("inventorySlots", f == null ? "-1" : Integer.toString(f.inventorySlots));
            p.setProperty("bankChecked",Boolean.toString(bankChecked));
            p.setProperty("supplyBankChecked",Boolean.toString(supplyBankChecked));
            p.setProperty("supplyActions",Integer.toString(supplyActions));
            p.setProperty("supplyBeefBought",Integer.toString(beefBought));
            p.setProperty("supplyFlourBought",Integer.toString(flourBought));
            p.setProperty("supplyKnife",f==null?"0":Integer.toString(f.count(ItemID.KNIFE)));
            p.setProperty("prepBankAudit",prepBankAudit);
            p.setProperty("prepFoodStage",prepFoodStage);
            p.setProperty("prepFoodId",Integer.toString(prepFoodId));
            p.setProperty("prepFoodQuote",Integer.toString(prepFoodQuote));
            p.setProperty("prepFoodQuantity",Integer.toString(prepFoodQuantity));
            p.setProperty("prepNativeStarted",Boolean.toString(prepNativeStarted));
            p.setProperty("prepNativeCheckpointPresent",Boolean.toString(!prepNativeCheckpoint.isBlank()));
            p.setProperty("prepGePlaceAttempted",Boolean.toString(prepGePlaceAttempted));
            p.setProperty("entryFoodCount",f==null?"0":Integer.toString(entryFoodCount(f)));
            p.setProperty("supplyBread",f==null?"0":Integer.toString(f.count(ItemID.BREAD)));
            p.setProperty("supplyCookedMeat",f==null?"0":Integer.toString(f.count(ItemID.COOKED_MEAT)));
            p.setProperty("supplyRawBeef",f==null?"0":Integer.toString(f.count(ItemID.RAW_BEEF)));
            p.setProperty("supplyFlour",f==null?"0":Integer.toString(f.count(ItemID.POT_FLOUR)));
            p.setProperty("supplyDough",f==null?"0":Integer.toString(f.count(ItemID.BREAD_DOUGH)));
            p.setProperty("bankOpen",f==null?"false":Boolean.toString(f.bankOpen));
            p.setProperty("pending",pending==null?"":pending.key);
            p.setProperty("route",route==null?"":route.key+" -> "+route.target);
            Path temp = STATUS.resolveSibling("status.tmp");
            try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "Below Ice Mountain early route Build 2"); }
            Files.move(temp, STATUS, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) { log.warn("[BelowIceMountain] status write: {}", ex.toString()); }
    }
    // Native widget GE buyer copied from the existing Prince Ali Rescue project.
    private static final class QuestGeBuyer {
    public enum Outcome { WORKING, NEED_COINS, NEEDS_OVERVIEW, COMPLETE, CANCELLED, HOLD }
    private enum Phase {
        QUOTE, OPEN, WAIT_OPEN, SLOT, WAIT_SEARCH, TYPE_SEARCH,
        WAIT_RESULT, WAIT_ITEM, QUANTITY, WAIT_Q_INPUT, WAIT_Q_VALUE,
        PRICE, WAIT_P_INPUT, WAIT_P_VALUE, CONFIRM, WAIT_OFFER,
        WAIT_FILL, OPEN_OWNED, WAIT_OWNED, COLLECT_ITEM,
        WAIT_ITEM_COLLECT, COLLECT_COINS, WAIT_COIN_COLLECT, WAIT_SLOT_CLEAR,
        ABORT, WAIT_ABORT, COMPLETE, CANCELLED, HOLD
    }
    public static final class Result {
        public final Outcome outcome;
        public final String phase, reason;
        public final int slot, quotedPrice, itemCount, coins;
        private Result(Outcome outcome, String phase, String reason,
                       int slot, int quotedPrice, int itemCount, int coins) {
            this.outcome = outcome;
            this.phase = phase;
            this.reason = reason;
            this.slot = slot;
            this.quotedPrice = quotedPrice;
            this.itemCount = itemCount;
            this.coins = coins;
        }
    }
    private static final class Offer {
        final GrandExchangeOfferState state;
        final int item, total, filled;
        final long price, spent;
        Offer(GrandExchangeOffer raw) {
            state = raw == null ? GrandExchangeOfferState.EMPTY : raw.getState();
            item = raw == null ? 0 : raw.getItemId();
            total = raw == null ? 0 : raw.getTotalQuantity();
            filled = raw == null ? 0 : raw.getQuantitySold();
            price = raw == null ? 0 : raw.getPrice();
            spent = raw == null ? 0 : raw.getSpent();
        }
    }
    private static final class Frame {
        boolean loggedIn, overview, offerScreen, searchPrompt, inputVisible;
        int inventoryItem, coins, quantityVarbit, priceVarbit;
        Offer[] offers;
        Widget[] slots;
        Widget offerRoot, collectRoot, searchRoot, searchPromptWidget,
            inputWidget, confirmWidget;
    }

    private final int itemId, quantity, cap;
    private final String itemName, userAgent;
    private Phase phase = Phase.QUOTE;
    private int price, slot = -1, initialItem = -1, coinsAfterPlace = -1;
    private long quoteEpoch, phaseAt, fillAt, finalSpent, expectedRefund;
    private boolean chooseClicked, itemCollectClicked, coinCollectClicked,
        cancelled, refundProved, ownedOfferSeen;
    private String reason = "";

    public static QuestGeBuyer troutFour(String userAgent, String checkpoint) {
        return new QuestGeBuyer(333, "Trout", 4, 1000, userAgent, checkpoint);
    }

    /** cap is the remaining cumulative budget supplied by the quest caller. */
    public QuestGeBuyer(int itemId, String itemName, int quantity, int cap,
                        String userAgent, String checkpoint) {
        if (itemId <= 0 || itemName == null || itemName.isBlank() || quantity < 1
            || quantity > 28 || cap < 1 || cap > 1000)
            throw new IllegalArgumentException("Explicit item/quantity/remaining cap required");
        if (userAgent == null || userAgent.isBlank() || userAgent.length() < 15)
            throw new IllegalArgumentException("Descriptive Wiki API User-Agent required");
        this.itemId = itemId;
        this.itemName = itemName.trim();
        this.quantity = quantity;
        this.cap = cap;
        this.userAgent = userAgent;
        if (checkpoint != null && !checkpoint.isBlank()) restore(checkpoint);
    }

    /**
     * persist must durably store the checkpoint BEFORE each dispatched input.
     * A failed persist prevents that input. Call only from the script executor.
     */
    public synchronized Result tick(Consumer<String> persist) {
        if (persist == null) throw new IllegalArgumentException("checkpoint persistor required");
        Frame f;
        try { f = Microbot.getClientThread().invoke((Supplier<Frame>) this::frame); }
        catch (Exception ex) { return hold(persist, "client snapshot failed: " + ex); }
        if (f == null || !f.loggedIn || f.offers == null)
            return result(Outcome.WORKING, f, "waiting for logged-in client/offer snapshot");
        if (phase == Phase.HOLD) return result(Outcome.HOLD, f, reason);
        if (phase == Phase.COMPLETE) return result(Outcome.COMPLETE, f, reason);
        if (phase == Phase.CANCELLED) return result(Outcome.CANCELLED, f, reason);

        if (phase == Phase.QUOTE) {
            try {
                int candidate = fetchPrice();
                if ((long) candidate * quantity > cap)
                    return hold(persist, "fresh quote exceeds remaining cap");
                price = candidate;
                quoteEpoch = System.currentTimeMillis();
                move(Phase.OPEN, persist);
            } catch (Exception ex) { return hold(persist, "Wiki quote unavailable: " + ex); }
            return result(Outcome.WORKING, f, "fresh quote accepted");
        }
        if (initialItem < 0) { initialItem = f.inventoryItem; persist.accept(checkpoint()); }
        if (slot >= f.offers.length)
            return hold(persist, "owned slot absent from current client offer array");
        if (slot >= 0 && isForeign(f.offers[slot]))
            return hold(persist, "owned slot changed to a different item/quantity/price");
        if (slot >= 0 && owned(f.offers[slot])) {
            boolean changed = !ownedOfferSeen || finalSpent != f.offers[slot].spent
                || coinsAfterPlace < 0 || f.coins < coinsAfterPlace;
            ownedOfferSeen = true;
            finalSpent = f.offers[slot].spent;
            if (coinsAfterPlace < 0 || f.coins < coinsAfterPlace)
                coinsAfterPlace = f.coins;
            if (finalSpent < 0 || finalSpent > cap)
                return hold(persist, "owned offer spent amount exceeded cap");
            if (changed) persist.accept(checkpoint());
        }
        long now = System.currentTimeMillis();
        switch (phase) {
            case OPEN:
                if (f.overview) { move(Phase.SLOT, persist); break; }
                if (f.offerScreen || f.searchPrompt)
                    return result(Outcome.NEEDS_OVERVIEW, f,
                        "caller must close the pre-open GE form before a fresh buy");
                net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel clerk = Rs2Npc.getNpc("Grand Exchange Clerk");
                if (clerk == null) return result(Outcome.WORKING, f, "caller must reach GE clerk");
                move(Phase.WAIT_OPEN, persist);
                if (!Rs2Npc.interact(clerk, "Exchange"))
                    return hold(persist, "clerk Exchange interaction rejected");
                break;
            case WAIT_OPEN:
                if (f.overview) move(Phase.SLOT, persist);
                else if (expired(now, 8000)) return hold(persist, "GE overview did not open");
                break;
            case SLOT:
                if (!f.overview && slot < 0) { move(Phase.OPEN, persist); break; }
                if (!f.overview) return hold(persist, "GE overview closed after slot selection");
                if (f.coins < (long) price * quantity)
                    return result(Outcome.NEED_COINS, f,
                        "withdraw at least " + (price * quantity) + " coins first");
                for (Offer o : f.offers)
                    if (o.item == itemId && o.state != GrandExchangeOfferState.EMPTY)
                        return hold(persist, "matching existing offer: ownership unknown");
                slot = firstAvailable(f);
                if (slot < 0) return hold(persist, "no verified interactive EMPTY slot");
                persist.accept(checkpoint());
                Widget buy = child(f.slots[slot],3);
                move(Phase.WAIT_SEARCH, persist);
                if (!Rs2Widget.clickWidget(buy)) return hold(persist, "buy button click rejected");
                break;
            case WAIT_SEARCH:
                if (f.searchPrompt) { move(Phase.TYPE_SEARCH, persist); break; }
                if (f.offerScreen && !chooseClicked) {
                    Widget choose = child(f.offerRoot, 0);
                    if (visible(choose)) {
                        chooseClicked = true;
                        persist.accept(checkpoint());
                        if (!Rs2Widget.clickWidget(choose))
                            return hold(persist, "choose-item click rejected");
                        phaseAt = now;
                        persist.accept(checkpoint());
                    }
                }
                if (expired(now, 7000)) return hold(persist, "GE item search did not open");
                break;
            case TYPE_SEARCH:
                if (!f.searchPrompt) return hold(persist, "item search vanished before typing");
                move(Phase.WAIT_RESULT, persist);
                Rs2Keyboard.typeString(itemName);
                break;
            case WAIT_RESULT:
                Widget match = exactResult(f.searchRoot, itemName);
                if (match != null) {
                    move(Phase.WAIT_ITEM, persist);
                    if (!Rs2Widget.clickWidget(match))
                        return hold(persist, "exact item result click rejected");
                } else if (expired(now, 6000))
                    return hold(persist, "exact item search result absent");
                break;
            case WAIT_ITEM:
                if (f.offerScreen && !f.searchPrompt && offerShowsItem(f))
                    move(Phase.QUANTITY, persist);
                else if (expired(now, 6000))
                    return hold(persist, "selected item not proved on offer screen");
                break;
            case QUANTITY:
                if (!offerShowsItem(f)) return hold(persist, "item proof lost before quantity");
                if (f.quantityVarbit == quantity) { move(Phase.PRICE, persist); break; }
                Widget qx = child(f.offerRoot, 7);
                if (!visible(qx)) return hold(persist, "quantity X widget unavailable");
                move(Phase.WAIT_Q_INPUT, persist);
                if (!Rs2Widget.clickWidget(qx)) return hold(persist, "quantity X click rejected");
                break;
            case WAIT_Q_INPUT:
                if (f.inputVisible) {
                    move(Phase.WAIT_Q_VALUE, persist);
                    Rs2Keyboard.typeString(Integer.toString(quantity));
                    Rs2Keyboard.enter();
                } else if (expired(now, 3500))
                    return hold(persist, "quantity chatbox input not observed");
                break;
            case WAIT_Q_VALUE:
                if (f.quantityVarbit == quantity) move(Phase.PRICE, persist);
                else if (expired(now, 3500)) return hold(persist, "quantity value not verified");
                break;
            case PRICE:
                if (!offerShowsItem(f) || f.quantityVarbit != quantity)
                    return hold(persist, "item/quantity proof lost before price");
                if (f.priceVarbit == price) { move(Phase.CONFIRM, persist); break; }
                Widget px = child(f.offerRoot, 12);
                if (!visible(px)) return hold(persist, "price X widget unavailable");
                move(Phase.WAIT_P_INPUT, persist);
                if (!Rs2Widget.clickWidget(px)) return hold(persist, "price X click rejected");
                break;
            case WAIT_P_INPUT:
                if (f.inputVisible) {
                    move(Phase.WAIT_P_VALUE, persist);
                    Rs2Keyboard.typeString(Integer.toString(price));
                    Rs2Keyboard.enter();
                } else if (expired(now, 3500))
                    return hold(persist, "price chatbox input not observed");
                break;
            case WAIT_P_VALUE:
                if (f.priceVarbit == price) move(Phase.CONFIRM, persist);
                else if (expired(now, 3500)) return hold(persist, "price value not verified");
                break;
            case CONFIRM:
                if (!offerShowsItem(f) || f.quantityVarbit != quantity
                    || f.priceVarbit != price || f.coins < (long) price * quantity
                    || f.offers[slot].state != GrandExchangeOfferState.EMPTY
                    || now - quoteEpoch > 1800000)
                    return hold(persist, "offer/cap/coin proof changed before confirm");
                Widget confirm = f.confirmWidget;
                if (!hasAction(confirm, "Confirm")) confirm = null;
                if (!visible(confirm)) return hold(persist, "confirm widget absent");
                move(Phase.WAIT_OFFER, persist);
                if (!Rs2Widget.clickWidget(confirm)) return hold(persist, "confirm click rejected");
                break;
            case WAIT_OFFER:
                if (owned(f.offers[slot])) {
                    ownedOfferSeen = true;
                    coinsAfterPlace = f.coins;
                    fillAt = now;
                    move(Phase.WAIT_FILL, persist);
                } else if (expired(now, 8000))
                    return hold(persist, "owned offer not observed after confirm; no repeat");
                break;
            case WAIT_FILL:
                if (f.offers[slot].state == GrandExchangeOfferState.BOUGHT
                    || f.offers[slot].filled >= quantity) {
                    cancelled = false;
                    move(Phase.OPEN_OWNED, persist);
                } else if (f.offers[slot].state == GrandExchangeOfferState.CANCELLED_BUY) {
                    cancelled = true;
                    move(Phase.OPEN_OWNED, persist);
                } else if (now - fillAt > 90000) {
                    move(Phase.ABORT, persist);
                }
                break;
            case ABORT:
                if (!owned(f.offers[slot])) return hold(persist, "offer ownership lost before abort");
                if (f.offers[slot].state == GrandExchangeOfferState.BOUGHT) {
                    cancelled = false;
                    move(Phase.OPEN_OWNED, persist); break;
                }
                Widget abort = findPrimaryAction(f.offerRoot, "Abort offer");
                if (!visible(abort)) {
                    if (!f.offerScreen) { move(Phase.OPEN_OWNED, persist); break; }
                    return hold(persist, "owned abort control unavailable; offer left untouched");
                }
                move(Phase.WAIT_ABORT, persist);
                if (!Rs2Widget.clickWidget(abort)) return hold(persist, "abort click rejected");
                break;
            case WAIT_ABORT:
                if (f.offers[slot].state == GrandExchangeOfferState.CANCELLED_BUY) {
                    cancelled = true;
                    move(Phase.OPEN_OWNED, persist);
                }
                else if (expired(now, 6000))
                    return hold(persist, "abort not proved; no repeat");
                break;
            case OPEN_OWNED:
                if (!owned(f.offers[slot])) return hold(persist, "owned offer changed before view");
                if (f.offerScreen && offerShowsItem(f)) {
                    move(f.offers[slot].state == GrandExchangeOfferState.BUYING
                        ? Phase.ABORT : Phase.COLLECT_ITEM, persist);
                    break;
                }
                if (!f.overview || !visible(f.slots[slot]))
                    return hold(persist, "owned offer slot not visible");
                move(Phase.WAIT_OWNED, persist);
                if (!Rs2Widget.clickWidget(f.slots[slot]))
                    return hold(persist, "owned offer view click rejected");
                break;
            case WAIT_OWNED:
                if (f.offerScreen && offerShowsItem(f))
                    move(f.offers[slot].state == GrandExchangeOfferState.BUYING
                        ? Phase.ABORT : Phase.COLLECT_ITEM, persist);
                else if (expired(now, 6000))
                    return hold(persist, "owned offer detail not proved");
                break;
            case COLLECT_ITEM:
                if (!offerShowsItem(f) || !owned(f.offers[slot]))
                    return hold(persist, "ownership/detail proof lost before item collect");
                if (f.offers[slot].filled <= 0 || itemCollectClicked) {
                    move(Phase.COLLECT_COINS, persist); break;
                }
                Widget itemCollect = findCollect(f.collectRoot, itemId);
                if (!visible(itemCollect)) return hold(persist, "owned item collect control absent");
                itemCollectClicked = true;
                move(Phase.WAIT_ITEM_COLLECT, persist);
                if (!clickVerifiedAction(itemCollect,"Collect-items"))
                    return hold(persist, "item collect click rejected");
                break;
            case WAIT_ITEM_COLLECT:
                if (f.inventoryItem > initialItem) move(Phase.COLLECT_COINS, persist);
                else if (expired(now, 6000))
                    return hold(persist, "item inventory delta not proved; no repeat");
                break;
            case COLLECT_COINS:
                if (!ownedOfferSeen)
                    return hold(persist, "owned offer was never observed");
                if (f.offers[slot].state != GrandExchangeOfferState.EMPTY
                    && f.offers[slot].state != GrandExchangeOfferState.BOUGHT
                    && f.offers[slot].state != GrandExchangeOfferState.CANCELLED_BUY)
                    return hold(persist, "offer not complete/cancelled before coin collection");
                if (!cancelled && f.inventoryItem < initialItem + quantity)
                    return hold(persist, "buy complete but item inventory short");
                expectedRefund = (long) price * quantity - finalSpent;
                if (expectedRefund < 0 || expectedRefund > cap)
                    return hold(persist, "refund exceeds bounded reservation");
                if (expectedRefund == 0) {
                    refundProved = true;
                    move(Phase.WAIT_SLOT_CLEAR, persist);
                    break;
                }
                if (coinsAfterPlace >= 0
                    && f.coins >= (long) coinsAfterPlace + expectedRefund) {
                    refundProved = true;
                    move(Phase.WAIT_SLOT_CLEAR, persist);
                    break;
                }
                if (f.offers[slot].state == GrandExchangeOfferState.EMPTY)
                    return hold(persist, "slot cleared but required refund not in inventory");
                if (coinCollectClicked) { move(Phase.WAIT_COIN_COLLECT, persist); break; }
                Widget refund = findCollect(f.collectRoot, 995);
                if (!visible(refund)) return hold(persist, "owned coin refund control absent");
                coinCollectClicked = true;
                move(Phase.WAIT_COIN_COLLECT, persist);
                if (!clickVerifiedAction(refund,"Collect"))
                    return hold(persist, "coin refund click rejected");
                break;
            case WAIT_COIN_COLLECT:
                if (coinsAfterPlace >= 0
                    && f.coins >= (long) coinsAfterPlace + expectedRefund) {
                    refundProved = true;
                    move(Phase.WAIT_SLOT_CLEAR, persist);
                } else if (expired(now, 6000))
                    return hold(persist, "coin refund not proved; no repeat");
                break;
            case WAIT_SLOT_CLEAR:
                if (!ownedOfferSeen || !refundProved)
                    return hold(persist, "offer/refund proof missing at final boundary");
                if (f.offers[slot].state != GrandExchangeOfferState.EMPTY) {
                    if (expired(now, 6000))
                        return hold(persist, "owned offer slot did not clear");
                    break;
                }
                if (cancelled) {
                    move(Phase.CANCELLED, persist);
                    reason = "owned offer cancelled, refund and slot clearance verified; items="
                        + (f.inventoryItem - initialItem);
                } else {
                    if (f.inventoryItem < initialItem + quantity)
                        return hold(persist, "slot clear but requested inventory delta absent");
                    move(Phase.COMPLETE, persist);
                    reason = "owned buy, spend, inventory, refund and slot clearance verified";
                }
                persist.accept(checkpoint());
                break;
            default: return hold(persist, "unknown buyer phase");
        }
        return result(phase == Phase.COMPLETE ? Outcome.COMPLETE : Outcome.WORKING, f, reason);
    }

    private Frame frame() {
        Frame f = new Frame();
        Client c = Microbot.getClient();
        if (c == null || c.getGameState() != GameState.LOGGED_IN
            || c.getLocalPlayer() == null) return f;
        f.loggedIn = true;
        GrandExchangeOffer[] raw = c.getGrandExchangeOffers();
        if (raw == null || raw.length < 3) return f;
        f.offers = new Offer[raw.length];
        f.slots = new Widget[raw.length];
        for (int i = 0; i < raw.length; i++) {
            f.offers[i] = new Offer(raw[i]);
            f.slots[i] = i < 8 ? c.getWidget(465, 7 + i) : null;
        }
        f.inventoryItem = inventoryQuantity(c,itemId);
        f.coins = inventoryQuantity(c,995);
        f.offerRoot = visible(c.getWidget(465,26))?c.getWidget(465,26):c.getWidget(465,15);
        f.offerScreen = visible(f.offerRoot)
            && containsText(f.offerRoot, "Buy offer");
        f.overview = visible(c.getWidget(465, 1))
            && visible(c.getWidget(465, 5)) && !f.offerScreen;
        f.collectRoot = c.getWidget(465, 24);
        f.confirmWidget = c.getWidget(465, 30);
        f.searchRoot = c.getWidget(162, 53);
        f.searchPromptWidget = c.getWidget(162, 53);
        f.searchPrompt = visible(f.searchPromptWidget)
            && containsText(f.searchPromptWidget,
                "Start typing the name of an item");
        f.inputWidget = c.getWidget(162, 44);
        f.inputVisible = visible(f.inputWidget)
            && c.getVarcIntValue(VarClientInt.INPUT_TYPE) != 0;
        f.quantityVarbit = numericWidget(child(f.offerRoot,34));
        f.priceVarbit = numericWidget(child(f.offerRoot,41));
        return f;
    }

    private static int inventoryQuantity(Client c,int id) {
        var container=c.getItemContainer(net.runelite.api.InventoryID.INVENTORY);
        if(container==null) return 0;
        int total=0; for(var item:container.getItems()) if(item!=null&&item.getId()==id) total+=item.getQuantity();
        return total;
    }
    private static int numericWidget(Widget widget) {
        if(!visible(widget)) return -1;
        String value=clean(widget.getText()).replace(",", "").replace(" coins", "").trim();
        if(!value.matches("[0-9]+")) return -1;
        try { return Integer.parseInt(value); } catch(NumberFormatException e) { return -1; }
    }
    private int fetchPrice() throws Exception {
        java.net.HttpURLConnection connection=(java.net.HttpURLConnection)
            URI.create("https://prices.runescape.wiki/api/v1/osrs/latest?id="+itemId).toURL().openConnection();
        connection.setConnectTimeout(4000); connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent",userAgent);
        connection.setRequestProperty("Accept","application/json");
        String responseBody;
        try {
            int status=connection.getResponseCode();
            if(status!=200) throw new IllegalStateException("HTTP "+status);
            try(var stream=connection.getInputStream()) {
                responseBody=new String(stream.readNBytes(65536),java.nio.charset.StandardCharsets.UTF_8);
            }
        } finally { connection.disconnect(); }
        JsonObject root = new JsonParser().parse(responseBody).getAsJsonObject();
        JsonObject item = root.getAsJsonObject("data").getAsJsonObject(Integer.toString(itemId));
        if (item == null || !item.has("high") || item.get("high").isJsonNull()
            || !item.has("highTime") || item.get("highTime").isJsonNull())
            throw new IllegalStateException("item quote or high timestamp absent");
        int high = item.get("high").getAsInt();
        long highTime = item.get("highTime").getAsLong();
        long now = System.currentTimeMillis() / 1000;
        if (high <= 0 || highTime < now - 1800 || highTime > now + 60)
            throw new IllegalStateException("invalid/stale high quote");
        long candidate = Math.max((long) high + 2, ((long) high * 5 + 3) / 4);
        if (candidate > Integer.MAX_VALUE) throw new IllegalStateException("quote overflow");
        return (int) candidate;
    }

    private int firstAvailable(Frame f) {
        for (int i = 0; i < Math.min(8, f.offers.length); i++) {
            Widget w = f.slots[i], buy = child(w, 3);
            if (f.offers[i].state == GrandExchangeOfferState.EMPTY && visible(buy)
                && hasAction(buy, "Create Buy offer")) return i;
        }
        return -1;
    }
    private boolean owned(Offer o) {
        return o != null && o.item == itemId && o.total == quantity
            && o.price == price && o.state != GrandExchangeOfferState.EMPTY;
    }
    private boolean isForeign(Offer o) {
        return o != null && o.state != GrandExchangeOfferState.EMPTY && !owned(o);
    }
    private boolean offerShowsItem(Frame f) {
        return f.offerScreen && visible(f.offerRoot)
            && (containsText(f.offerRoot, itemName)
                || containsItem(f.offerRoot, itemId));
    }
    private boolean expired(long now, long limit) {
        return phaseAt > 0 && now - phaseAt > limit;
    }
    private void move(Phase next, Consumer<String> persist) {
        phase = next;
        phaseAt = System.currentTimeMillis();
        persist.accept(checkpoint());
    }
    private Result hold(Consumer<String> persist, String why) {
        reason = why;
        move(Phase.HOLD, persist);
        return result(Outcome.HOLD, null, why);
    }
    private Result result(Outcome outcome, Frame f, String why) {
        return new Result(outcome, phase.name(), why, slot, price,
            f == null ? -1 : f.inventoryItem, f == null ? -1 : f.coins);
    }

    /** Stable primitive checkpoint; never serialize Widget or client objects. */
    public synchronized String checkpoint() {
        return "GE2|" + itemId + '|' + quantity + '|' + cap + '|' + phase.name()
            + '|' + price + '|' + slot + '|' + initialItem + '|' + coinsAfterPlace
            + '|' + quoteEpoch + '|' + phaseAt + '|' + fillAt + '|'
            + (chooseClicked ? 1 : 0) + '|' + (itemCollectClicked ? 1 : 0)
            + '|' + (coinCollectClicked ? 1 : 0) + '|' + (cancelled ? 1 : 0)
            + '|' + (refundProved ? 1 : 0) + '|' + (ownedOfferSeen ? 1 : 0)
            + '|' + finalSpent + '|' + expectedRefund;
    }
    private void restore(String value) {
        String[] a = value.split("\\|", -1);
        if (a.length != 20 || !"GE2".equals(a[0])
            || Integer.parseInt(a[1]) != itemId
            || Integer.parseInt(a[2]) != quantity
            || Integer.parseInt(a[3]) != cap)
            throw new IllegalArgumentException("foreign/invalid GE checkpoint");
        phase = Phase.valueOf(a[4]);
        price = Integer.parseInt(a[5]);
        slot = Integer.parseInt(a[6]);
        initialItem = Integer.parseInt(a[7]);
        coinsAfterPlace = Integer.parseInt(a[8]);
        quoteEpoch = Long.parseLong(a[9]);
        phaseAt = Long.parseLong(a[10]);
        fillAt = Long.parseLong(a[11]);
        chooseClicked = "1".equals(a[12]);
        itemCollectClicked = "1".equals(a[13]);
        coinCollectClicked = "1".equals(a[14]);
        cancelled = "1".equals(a[15]);
        refundProved = "1".equals(a[16]);
        ownedOfferSeen = "1".equals(a[17]);
        finalSpent = Long.parseLong(a[18]);
        expectedRefund = Long.parseLong(a[19]);
        if (slot < -1 || slot > 7 || price < 0 || initialItem < -1)
            throw new IllegalArgumentException("invalid GE checkpoint values");
        reason = "restored; verify pending action before any new input";
    }

    private static Widget child(Widget parent, int index) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Widget>)()->child(parent,index));
        return parent == null ? null : parent.getChild(index);
    }
    private static boolean visible(Widget w) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Boolean>)()->visible(w));
        return w != null && !w.isHidden() && w.getBounds() != null
            && w.getBounds().width > 1 && w.getBounds().height > 1;
    }
    private static String clean(String s) {
        return s == null ? "" : s.replaceAll("<[^>]*>", "").trim()
            .toLowerCase(Locale.ROOT);
    }
    private static boolean hasAction(Widget w, String action) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Boolean>)()->hasAction(w,action));
        if (w == null || w.getActions() == null) return false;
        for (String a : w.getActions())
            if (clean(a).equals(clean(action))) return true;
        return false;
    }
    private static boolean primaryAction(Widget w, String action) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Boolean>)()->primaryAction(w,action));
        String[] actions = w == null ? null : w.getActions();
        return actions != null && actions.length > 0
            && clean(actions[0]).equals(clean(action));
    }
    private static Widget exactResult(Widget root, String name) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Widget>)()->exactResult(root,name));
        if (!visible(root) || root.getChildren() == null) return null;
        Widget[] children = root.getChildren();
        for (int i = 1; i < children.length; i++)
            if (visible(children[i]) && clean(children[i].getText()).equals(clean(name))
                && visible(children[i - 1])) return children[i - 1];
        return null;
    }
    private static boolean containsText(Widget root, String text) {
        return find(root, w -> clean(w.getText()).contains(clean(text))) != null;
    }
    private static boolean containsItem(Widget root, int item) {
        return find(root, w -> w.getItemId() == item) != null;
    }
    private static Widget findActionOrText(Widget root, String text) {
        return find(root, w -> hasAction(w, text)
            || clean(w.getText()).equals(clean(text)));
    }
    private static Widget findPrimaryAction(Widget root, String action) {
        return find(root, w -> primaryAction(w, action));
    }
    private static Widget findCollect(Widget root, int item) {
        return find(root, w -> w.getItemId() == item && hasAction(w,item==995?"Collect":"Collect-items"));
    }
    private static boolean clickVerifiedAction(Widget w,String action) {
        int[] args=Microbot.getClientThread().invoke((Supplier<int[]>)()->{
            if(!visible(w)||w.getActions()==null) return null;
            String[] actions=w.getActions();
            for(int i=0;i<actions.length;i++) if(clean(actions[i]).equals(clean(action))) return new int[]{w.getIndex(),i+1};
            return null;
        });
        if(args==null) return false;
        Rs2Widget.clickWidgetFast(w,args[0],args[1]); return true;
    }
    private interface Test { boolean yes(Widget w); }
    private static Widget find(Widget root, Test test) {
        if(!Microbot.getClient().isClientThread()) return Microbot.getClientThread().invoke((Supplier<Widget>)()->find(root,test));
        Set<Widget> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return find(root, test, seen, 0);
    }
    private static Widget find(Widget w, Test test, Set<Widget> seen, int depth) {
        if (!visible(w) || depth > 8 || !seen.add(w)) return null;
        if (test.yes(w)) return w;
        for (Widget[] a : new Widget[][] {w.getChildren(), w.getDynamicChildren(), w.getStaticChildren()})
            if (a != null) for (Widget child : a) {
                Widget found = find(child, test, seen, depth + 1);
                if (found != null) return found;
            }
        return null;
    }
}
}
