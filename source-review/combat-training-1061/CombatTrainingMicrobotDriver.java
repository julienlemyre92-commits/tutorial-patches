package net.runelite.client.plugins.microbot.questcommon.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.reachable.Rs2Reachable;

/** Installed-Microbot adapter. All observation and dispatch revalidation is client-thread-bound. */
public final class CombatTrainingMicrobotDriver implements CombatTrainingService.Driver {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CombatTrainingMicrobotDriver.class);
    private int lastAttackedIndex = -1;
    private long lastObservedDeathAt;
    private volatile String lastAttackFailure = "not attempted";
    @Override public String attackFailure() { return lastAttackFailure; }
    @Override public CombatTrainingService.Frame observe(CombatTrainingGoal goal) {
        return Microbot.getClientThread().invoke((Supplier<CombatTrainingService.Frame>) () -> {
            Client client = Microbot.getClient();
            Player player = client == null ? null : client.getLocalPlayer();
            long now = System.currentTimeMillis();
            if (client == null || player == null || client.getGameState() != GameState.LOGGED_IN)
                return new CombatTrainingService.Frame("", now, false, false, false, false,
                    null, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0,
                    Map.of(), Set.of(), "", -1, List.of());
            WorldPoint position = player.getWorldLocation();
            EnumSet<WorldType> worldTypes = client.getWorldType();
            boolean memberWorld = worldTypes != null && worldTypes.contains(WorldType.MEMBERS);
            Actor interacting = player.getInteracting();
            String targetName = interacting == null ? "" : Objects.toString(interacting.getName(), "");
            int targetIndex = interacting instanceof NPC npc ? npc.getIndex() : -1;
            Map<Integer,Integer> inventory = itemCounts(client.getItemContainer(InventoryID.INVENTORY));
            Set<Integer> equipment = itemIds(client.getItemContainer(InventoryID.EQUIPMENT));
            Skill[] combatSkills = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};
            long[] xp = new long[combatSkills.length];
            for (int i = 0; i < combatSkills.length; i++) xp[i] = client.getSkillExperience(combatSkills[i]);
            List<CombatTrainingService.Npc> npcs = new ArrayList<>();
            for (Rs2NpcModel model : Microbot.getRs2NpcCache().query()
                .within(position, 100).toListOnClientThread()) {
                NPC npc = model == null ? null : model.getNpc();
                if (npc == null || npc.getWorldLocation() == null) continue;
                boolean inTrainingRegion = goal.trainingRegion().contains(npc.getWorldLocation());
                boolean nearPlayer = position.distanceTo(npc.getWorldLocation()) <= 15;
                if (!inTrainingRegion && !nearPlayer) continue;
                boolean interactingWithPlayer = npc.getInteracting() == player;
                boolean lineOfSight = model.hasLineOfSight();
                // A live NPC occupies its own collision tile. That tile can be absent from
                // the walkable flood-fill even when the player is already beside it.
                boolean meleeAdjacent = player.getWorldArea() != null && npc.getWorldArea() != null
                    && player.getWorldArea().isInMeleeDistance(npc.getWorldArea());
                npcs.add(new CombatTrainingService.Npc(npc.getIndex(), npc.getId(),
                    Objects.toString(npc.getName(), ""), npc.getCombatLevel(), npc.getWorldLocation(),
                    npc.isDead(), lineOfSight,
                    inTrainingRegion && (Rs2Reachable.isReachable(npc.getWorldLocation())
                        || meleeAdjacent && lineOfSight), interactingWithPlayer));
            }
            if (lastAttackedIndex >= 0 && npcs.stream()
                .anyMatch(n -> n.index() == lastAttackedIndex && n.dead())) {
                lastObservedDeathAt = now;
                LOG.info("[TrainingTiming] TARGET_DEAD index={} at={}", lastAttackedIndex, now);
                lastAttackedIndex = -1;
            }
            boolean liveCombat = interacting instanceof NPC target
                && !target.isDead() && target.getCombatLevel() > 0
                || npcs.stream().anyMatch(n -> n.combatLevel() > 0
                    && !n.dead() && n.interactingWithPlayer());
            return new CombatTrainingService.Frame(identity(client, player), now, true, memberWorld,
                player.isDead(), liveCombat, position, client.getWorld(),
                player.getCombatLevel(), client.getRealSkillLevel(Skill.HITPOINTS),
                client.getBoostedSkillLevel(Skill.HITPOINTS), client.getRealSkillLevel(Skill.HITPOINTS),
                xp[0], xp[1], xp[2], xp[3], xp[4], client.getSkillExperience(Skill.HITPOINTS),
                inventory, equipment, targetName, targetIndex, npcs);
        });
    }

    @Override public boolean attack(CombatTrainingGoal goal, CombatTrainingService.Npc selected) {
        return Microbot.getClientThread().invoke((Supplier<Boolean>) () -> {
            Client client = Microbot.getClient();
            Player player = client == null ? null : client.getLocalPlayer();
            if (player == null || client.getGameState() != GameState.LOGGED_IN
                || !goal.accountKey().equals(identity(client, player)) || player.isDead()
                || liveCombat(player)) {
                lastAttackFailure = "client/account/death/combat preflight changed";
                return false;
            }
            int seen = 0;
            String rejection = "no matching live NPC in selected two-tile query";
            for (Rs2NpcModel model : Microbot.getRs2NpcCache().query()
                .withId(selected.id()).within(selected.point(), 2).toListOnClientThread()) {
                NPC npc = model == null ? null : model.getNpc();
                seen++;
                if (npc == null) { rejection = "cached model has no NPC"; continue; }
                if (npc.getIndex() != selected.index() || npc.getId() != selected.id()) {
                    rejection = "selected NPC index/id changed"; continue;
                }
                if (npc.isDead() || npc.getWorldLocation() == null) {
                    rejection = "selected NPC died or lost location"; continue;
                }
                if (!goal.trainingRegion().contains(npc.getWorldLocation())
                    || npc.getCombatLevel() > goal.maximumTargetCombatLevel()
                    || !isAllowed(goal, npc)) {
                    rejection = "selected NPC left region/level/allowlist"; continue;
                }
                if (!model.hasLineOfSight()) { rejection = "selected NPC LOS changed"; continue; }
                boolean adjacent = player.getWorldArea() != null && npc.getWorldArea() != null
                    && player.getWorldArea().isInMeleeDistance(npc.getWorldArea());
                if (!Rs2Reachable.isReachable(npc.getWorldLocation()) && !adjacent) {
                    rejection = "no collision path or adjacent melee position"; continue;
                }
                if (player.getWorldLocation() == null
                    || player.getWorldLocation().distanceTo(npc.getWorldLocation()) > 6) {
                    rejection = "selected NPC moved beyond six-tile local attack range"; continue;
                }
                if (npc.getInteracting() != null && npc.getInteracting() != player) {
                    rejection = "selected NPC is interacting with another actor"; continue;
                }
                boolean clicked = model.click("Attack");
                if (clicked) {
                    long dispatchedAt = System.currentTimeMillis();
                    lastAttackedIndex = npc.getIndex();
                    LOG.info("[TrainingTiming] ATTACK_DISPATCH index={} at={} sinceObservedDeathMs={} attackXp={} strengthXp={} defenceXp={}",
                        lastAttackedIndex, dispatchedAt,
                        lastObservedDeathAt == 0 ? -1 : dispatchedAt - lastObservedDeathAt,
                        client.getSkillExperience(Skill.ATTACK), client.getSkillExperience(Skill.STRENGTH),
                        client.getSkillExperience(Skill.DEFENCE));
                    lastObservedDeathAt = 0;
                }
                lastAttackFailure = clicked ? "" : "Microbot NPC click returned false";
                return clicked;
            }
            lastAttackFailure = rejection + "; queried=" + seen
                + "; selected=" + selected.name() + "#" + selected.id()
                + "@" + selected.point();
            return false;
        });
    }

    /** Microbot's isInCombat() has a ten-second trailing timer. Use live actors
     * for the next attack after a verified kill, while retaining the service's
     * pending-action journal so a live target is never clicked twice. */
    private static boolean liveCombat(Player player) {
        Actor target = player.getInteracting();
        if (target instanceof NPC npc && !npc.isDead() && npc.getCombatLevel() > 0)
            return true;
        for (Rs2NpcModel model : Microbot.getRs2NpcCache().query()
            .within(player.getWorldLocation(), 15).toListOnClientThread()) {
            NPC npc = model == null ? null : model.getNpc();
            if (npc != null && !npc.isDead() && npc.getCombatLevel() > 0
                && npc.getInteracting() == player) return true;
        }
        return false;
    }

    @Override public boolean eat(CombatTrainingGoal goal, int itemId, int observedHp) {
        return Microbot.getClientThread().invoke((Supplier<Boolean>) () -> {
            Client client = Microbot.getClient();
            Player player = client == null ? null : client.getLocalPlayer();
            if (player == null || client.getGameState() != GameState.LOGGED_IN
                || !goal.accountKey().equals(identity(client, player)) || player.isDead()
                || !goal.foodItemPriority().contains(itemId)
                || client.getBoostedSkillLevel(Skill.HITPOINTS) != observedHp
                || observedHp > goal.eatAtOrBelowHp()
                || Rs2Inventory.count(itemId) <= 0) return false;
            return Rs2Inventory.interact(itemId, "Eat");
        });
    }

    @Override public boolean reachable(WorldPoint point) {
        return point != null && Microbot.getClientThread().invoke((Supplier<Boolean>) () -> {
            Client client = Microbot.getClient();
            Player player = client == null ? null : client.getLocalPlayer();
            return player != null && client.getGameState() == GameState.LOGGED_IN
                && player.getWorldLocation() != null
                && player.getWorldLocation().getPlane() == point.getPlane()
                && Rs2Reachable.isReachable(point);
        });
    }

    private static boolean isAllowed(CombatTrainingGoal goal, NPC npc) {
        boolean id = goal.allowedNpcIds().isEmpty() || goal.allowedNpcIds().contains(npc.getId());
        boolean name = goal.allowedNpcNames().isEmpty() || goal.allowedNpcNames().stream()
            .anyMatch(n -> n.equalsIgnoreCase(Objects.toString(npc.getName(), "")));
        return id && name;
    }
    private static Map<Integer,Integer> itemCounts(ItemContainer container) {
        if (container == null || container.getItems() == null) return Map.of();
        Map<Integer,Integer> result = new HashMap<>();
        for (Item item : container.getItems()) if (item != null && item.getId() > 0 && item.getQuantity() > 0)
            result.merge(item.getId(), item.getQuantity(), Integer::sum);
        return Map.copyOf(result);
    }
    private static Set<Integer> itemIds(ItemContainer container) {
        if (container == null || container.getItems() == null) return Set.of();
        Set<Integer> result = new HashSet<>();
        for (Item item : container.getItems()) if (item != null && item.getId() > 0 && item.getQuantity() > 0)
            result.add(item.getId());
        return Set.copyOf(result);
    }
    private static String identity(Client client, Player player) {
        try {
            String username = client.getUsername(), character = player.getName();
            if (username == null || character == null) return "";
            String value = username.trim().toLowerCase(Locale.ROOT) + "\n"
                + character.trim().toLowerCase(Locale.ROOT);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) { return ""; }
    }
}
