import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.questcommon.navigation.guard.RouteInputGuard;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathEdge;

/** Offline regression for freshly materialized wrappers of the same route edge. */
public final class RouteEdgeValueCheck {
    public static void main(String[] args) throws Exception {
        Constructor<PathEdge> constructor = PathEdge.class.getDeclaredConstructor(
            WorldPoint.class, WorldPoint.class, Transport.class);
        constructor.setAccessible(true);
        WorldPoint from = new WorldPoint(2975, 3341, 0);
        WorldPoint to = new WorldPoint(2974, 3342, 0);
        PathEdge first = constructor.newInstance(from, to, null);
        PathEdge same = constructor.newInstance(from, to, null);
        PathEdge changed = constructor.newInstance(from, new WorldPoint(2974, 3343, 0), null);
        Method compare = RouteInputGuard.class.getDeclaredMethod("sameEdges", List.class, List.class);
        compare.setAccessible(true);
        if (first.equals(same)) throw new IllegalStateException("Probe requires distinct edge wrappers");
        if (!Boolean.TRUE.equals(compare.invoke(null, List.of(first), List.of(same))))
            throw new IllegalStateException("Identical route meaning was rejected");
        if (!Boolean.FALSE.equals(compare.invoke(null, List.of(first), List.of(changed))))
            throw new IllegalStateException("Changed route meaning was accepted");
        System.out.println("PASS identical edges accepted; changed endpoint rejected");
    }
}
