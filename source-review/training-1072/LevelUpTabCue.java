package net.runelite.client.plugins.microbot.questcommon.training;
import java.util.*;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.WidgetIndices;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
public final class LevelUpTabCue {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(LevelUpTabCue.class);
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
            if (result == Result.QUEUED || result == Result.WAITING_SAFE_POINT
                || result == Result.READY_SKILL) {
                int notification = pendingNotification();
                if (notification <= 0) {
                    log.info("[SharedTraining] LEVEL_CUE_SKIPPED skill={} pending={} reason=cleared-or-unavailable", skill, notification);
                    result = result == Result.READY_SKILL ? Result.READY_INVENTORY : Result.IDLE;
                    return result;
                }
            }
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
                    result=pendingNotification()==0?Result.PROVED:Result.REJECTED;
                else if (nowMillis-actionAt>3500) result=Result.REJECTED;
                return result;
            }
            if (!safeToSwitch) {
                if (result==Result.QUEUED) result=Result.WAITING_SAFE_POINT;
                return result;
            }
            if (result == Result.QUEUED || result == Result.WAITING_SAFE_POINT) {
                // A dead target clears attack ownership before the game's combat
                // timer clears. Skill guides can still be refused in that window.
                // Hold only this queued optional action; urgent eating stays above us.
                if (net.runelite.client.plugins.microbot.util.player.Rs2Player.isInCombat()) {
                    result=Result.WAITING_SAFE_POINT;
                    return result;
                }
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
                log.info("[SharedTraining] LEVEL_UP_SKILL_WIDGET skill={} group={} child={}",
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
                    result=pendingNotification()==0?Result.PROVED:Result.REJECTED;
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
        // RuneLite cs2-scripts: stats_init -> script9337 -> script9348.
        // The icon timer is enabled precisely when this per-skill value is >0.
        private int pendingNotification() {
            if (skill == null) return -1;
            try {
                int id = VarbitID.class.getField("LEVELUP_LIST_"+skill.name()).getInt(null);
                return Microbot.getClientThread().invoke((java.util.function.Supplier<Integer>) () -> {
                    Client client = Microbot.getClient();
                    return client == null ? -1 : client.getVarbitValue(id);
                });
            } catch (Exception unavailable) { return -1; }
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
