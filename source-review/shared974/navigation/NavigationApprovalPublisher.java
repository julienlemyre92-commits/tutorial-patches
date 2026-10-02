package net.runelite.client.plugins.microbot.questcommon.navigation;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/** Runs on a separate validator thread, never from a walker completion callback. */
public final class NavigationApprovalPublisher {
    public interface Gate {
        long generation();
        boolean approveValidated(Pathfinder route,WorldPoint target,long validMillis,long generation);
        void revoke();
    }
    private final Gate gate;
    public NavigationApprovalPublisher(Gate gate){this.gate=Objects.requireNonNull(gate);}

    /** Null means approval published; a reason means no input is approved. */
    public String refresh(NavigationGoal goal,BooleanSupplier ownsInput){
        try{
            if(!ownsInput.getAsBoolean() || System.currentTimeMillis()>=goal.deadlineMillis())
                return reject("navigation ownership/deadline ended");
            java.util.function.Supplier<String> inspectContext=()->{
                var client=Microbot.getClient();
                if(client==null || client.getGameState()!=GameState.LOGGED_IN || client.getLocalPlayer()==null)
                    return "logged-in player unavailable";
                if(!goal.accountKey().equals(NavigationMicrobotDriver.identity(client)))return "account changed";
                if(client.isInInstancedRegion() || Rs2Player.isInCombat())return "instance/combat context changed";
                if(client.getWorldType().contains(WorldType.MEMBERS))return "F2P route context changed";
                return null;
            };
            String context=Microbot.getClientThread().invoke(inspectContext);
            if(context!=null)return reject(context);
            long generation=gate.generation();
            Pathfinder exact=ShortestPathPlugin.pathfinder;
            var future=ShortestPathPlugin.pathfinderFuture;
            if(exact==null || !exact.isDone() || future!=null && !future.isDone())
                return reject("route calculation pending");
            if(!goal.destination().equals(Rs2Walker.getCurrentTarget()))
                return reject("walker target differs from requested destination");
            List<WorldPoint> path=List.copyOf(exact.getPath());
            var snapshot=Rs2PathApi.getActiveRoute();
            if(snapshot.isEmpty())return reject("active route snapshot unavailable");
            var route=snapshot.get();
            if(!path.equals(route.getPath()) || !exact.getStart().equals(route.getStart())
                || !exact.getTargets().equals(route.getTargets()))
                return reject("active snapshot differs from actual pathfinder");
            String unsupported=VerifiedRoutePolicy.inspect(goal,route.getStart(),route);
            if(unsupported!=null)return reject(unsupported);
            // Planning/snapshot acquisition can block. Re-read live context after that work.
            long refreshedAt=System.currentTimeMillis();
            context=Microbot.getClientThread().invoke(inspectContext);
            if(context!=null)return reject(context);
            long now=System.currentTimeMillis();
            if(now-refreshedAt>1500 || now>=goal.deadlineMillis())
                return reject("route context refresh exceeded freshness/deadline");
            if(ShortestPathPlugin.pathfinder!=exact || gate.generation()!=generation
                || !path.equals(exact.getPath()) || !ownsInput.getAsBoolean())
                return reject("route/ownership changed during validation");
            return gate.approveValidated(exact,goal.destination(),1500,generation)
                ?null:reject("guard rejected changed route generation");
        }catch(Throwable failure){return reject("route validation failed: "+failure);}
    }
    private String reject(String reason){gate.revoke();return reason;}
}
