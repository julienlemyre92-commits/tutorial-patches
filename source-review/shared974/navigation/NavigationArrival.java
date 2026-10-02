package net.runelite.client.plugins.microbot.questcommon.navigation;

import net.runelite.api.coords.WorldPoint;

/** Arrival proof is independent of the walker's return value. */
public final class NavigationArrival {
    public record Frame(String accountKey,long observedAt,boolean loggedIn,
        WorldPoint position,boolean instanced,boolean localDestinationReachable,
        boolean walkerStopped,boolean clearingStopped) { }
    private NavigationArrival() { }
    public static boolean proved(NavigationGoal goal,Frame frame,long now){
        if(frame==null || !frame.loggedIn() || frame.instanced()
            || !goal.accountKey().equals(frame.accountKey()) || frame.observedAt()<=0
            || frame.observedAt()>now || now-frame.observedAt()>3000
            || !frame.walkerStopped() || !frame.clearingStopped()
            || frame.position()==null || frame.position().getPlane()!=goal.destination().getPlane()
            || goal.forbiddenRegions().contains(frame.position().getRegionID()))return false;
        int distance=Math.max(Math.abs(frame.position().getX()-goal.destination().getX()),
            Math.abs(frame.position().getY()-goal.destination().getY()));
        // Adjacent across a closed gate/wall is not arrival. Instanced routing
        // needs a separately verified local-to-template transform.
        return distance<=goal.arrivalRadius() && (distance==0 || frame.localDestinationReachable());
    }
}
