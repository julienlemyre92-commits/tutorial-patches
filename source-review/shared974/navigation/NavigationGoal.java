package net.runelite.client.plugins.microbot.questcommon.navigation;

import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceHub;

/** Destination and constraints; the shared provider chooses the route. */
public record NavigationGoal(String accountKey,long pid,String purpose,String requestId,
        WorldPoint destination,int arrivalRadius,boolean f2pOnly,int maximumRisk,
        int maximumSpend,boolean allowTeleports,Set<Integer> forbiddenRegions,
        int maximumReplans,long deadlineMillis) implements QuestServiceHub.Request {
    public NavigationGoal {
        if(accountKey==null || !accountKey.matches("[a-f0-9]{64}") || pid<=0
            || purpose==null || purpose.isBlank() || requestId==null || requestId.isBlank()
            || destination==null || destination.getPlane()<0 || destination.getPlane()>3
            || arrivalRadius<0 || arrivalRadius>16 || maximumRisk<0 || maximumSpend<0
            || forbiddenRegions==null || maximumReplans<0 || maximumReplans>5
            || deadlineMillis<=System.currentTimeMillis())throw new IllegalArgumentException("Invalid navigation goal");
        forbiddenRegions=Set.copyOf(forbiddenRegions);
    }
    @Override public QuestServiceHub.Kind kind(){return QuestServiceHub.Kind.NAVIGATION;}
}
