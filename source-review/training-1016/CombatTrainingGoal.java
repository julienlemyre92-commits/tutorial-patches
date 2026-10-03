package net.runelite.client.plugins.microbot.questcommon.training;

import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceHub;

/** Explicit, bounded combat-training request. It does not acquire supplies or choose a zone. */
public record CombatTrainingGoal(
    String accountKey,
    long pid,
    String purpose,
    String requestId,
    int targetCombatLevel,
    int targetHitpointsLevel,
    Set<Integer> allowedNpcIds,
    Set<String> allowedNpcNames,
    int maximumTargetCombatLevel,
    Region trainingRegion,
    WorldPoint entryProofTile,
    WorldPoint returnTile,
    List<Integer> foodItemPriority,
    int minimumFoodAtEntry,
    int foodReserve,
    int eatAtOrBelowHp,
    int stopAtOrBelowHp,
    Set<Integer> requiredEquippedItemIds,
    boolean f2pOnly,
    Set<Integer> forbiddenRegions,
    int maximumRouteReplans,
    int maximumNpcActions,
    long deadlineMillis,
    long noProgressMillis,
    long returnRouteGraceMillis) implements QuestServiceHub.Request {

    /** A flat same-plane allowlist; no training or attack may leave these bounds. */
    public record Region(int minX, int minY, int maxX, int maxY, int plane) {
        public Region {
            if (minX < 0 || minY < 0 || maxX < minX || maxY < minY
                || maxX - minX > 100 || maxY - minY > 100 || plane < 0 || plane > 3)
                throw new IllegalArgumentException("Invalid bounded training region");
        }
        public boolean contains(WorldPoint point) {
            return point != null && point.getPlane() == plane
                && point.getX() >= minX && point.getX() <= maxX
                && point.getY() >= minY && point.getY() <= maxY;
        }
    }

    public CombatTrainingGoal {
        if (accountKey == null || !accountKey.matches("[a-f0-9]{64}") || pid <= 0
            || purpose == null || purpose.isBlank() || requestId == null || requestId.isBlank()
            || targetCombatLevel < 3 || targetCombatLevel > 126
            || targetHitpointsLevel < 10 || targetHitpointsLevel > 99
            || allowedNpcIds == null || allowedNpcNames == null
            || (allowedNpcIds.isEmpty() && allowedNpcNames.isEmpty())
            || maximumTargetCombatLevel < 1 || maximumTargetCombatLevel > 126
            || trainingRegion == null || entryProofTile == null || returnTile == null
            || !trainingRegion.contains(entryProofTile)
            || foodItemPriority == null || foodItemPriority.isEmpty()
            || foodItemPriority.stream().anyMatch(id -> id == null || id <= 0)
            || minimumFoodAtEntry < 0 || minimumFoodAtEntry > 28
            || foodReserve < 0 || foodReserve > 27
            || eatAtOrBelowHp < 1 || eatAtOrBelowHp <= stopAtOrBelowHp
            || stopAtOrBelowHp > 99
            || requiredEquippedItemIds == null || requiredEquippedItemIds.stream().anyMatch(id -> id == null || id <= 0)
            || forbiddenRegions == null || maximumRouteReplans < 0 || maximumRouteReplans > 3
            || maximumNpcActions < 1 || maximumNpcActions > 1000
            || deadlineMillis <= System.currentTimeMillis()
            || noProgressMillis < 10_000 || noProgressMillis > 600_000
            || returnRouteGraceMillis < 15_000 || returnRouteGraceMillis > 180_000)
            throw new IllegalArgumentException("Invalid bounded combat-training goal");
        allowedNpcIds = Set.copyOf(allowedNpcIds);
        allowedNpcNames = allowedNpcNames.stream().map(String::trim)
            .filter(name -> !name.isEmpty()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (allowedNpcIds.isEmpty() && allowedNpcNames.isEmpty())
            throw new IllegalArgumentException("Training goal needs an NPC allowlist");
        foodItemPriority = List.copyOf(new java.util.LinkedHashSet<>(foodItemPriority));
        requiredEquippedItemIds = Set.copyOf(requiredEquippedItemIds);
        forbiddenRegions = Set.copyOf(forbiddenRegions);
    }

    @Override public QuestServiceHub.Kind kind() { return QuestServiceHub.Kind.TRAINING; }
}
