package net.runelite.client.plugins.microbot.questcommon.navigation;

import java.util.*;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;

/** Destination facts for the funding service; NavigationService owns movement. */
public final class FundingRouteDestinations {
    private static final WorldPoint GRAND_EXCHANGE=new WorldPoint(3165,3486,0);
    private static final org.slf4j.Logger LOG=
        org.slf4j.LoggerFactory.getLogger(FundingRouteDestinations.class);
    private record Scene(String account,WorldPoint start,boolean members) { }
    private FundingRouteDestinations() { }
    public static WorldPoint grandExchange(){return GRAND_EXCHANGE;}
    public static Optional<WorldPoint> nearestBank(){
        Scene scene=Microbot.getClientThread().invoke((Supplier<Scene>)()->{
            Client c=Microbot.getClient();
            if(c==null || c.getGameState()!=GameState.LOGGED_IN || c.getLocalPlayer()==null)
                return null;
            return new Scene(NavigationMicrobotDriver.identity(c),
                c.getLocalPlayer().getWorldLocation(),
                c.getWorldType()!=null && c.getWorldType().contains(WorldType.MEMBERS));
        });
        if(scene==null || scene.start()==null || scene.members())return Optional.empty();
        // Microbot's nearest-bank helper can prefer a teleport and its path search
        // must run off the client thread. Select a nearby F2P bank only when the
        // same route policy used by NAVIGATION proves a zero-cost walk.
        List<BankLocation> candidates=Arrays.stream(BankLocation.values())
            .filter(bank->!bank.isMembers() && bank.hasRequirements()
                && bank.getWorldPoint()!=null
                && bank.getWorldPoint().getPlane()==scene.start().getPlane())
            .sorted(Comparator.comparingInt(bank->distance(scene.start(),bank.getWorldPoint())))
            .filter(bank->distance(scene.start(),bank.getWorldPoint())<=160)
            .limit(6).toList();
        VerifiedRoutePolicy policy=new VerifiedRoutePolicy();
        for(BankLocation bank:candidates){
            WorldPoint destination=bank.getWorldPoint();
            NavigationGoal probe=new NavigationGoal(scene.account(),ProcessHandle.current().pid(),
                "F2P walking bank candidate","bank-probe-"+bank.name(),destination,2,
                true,0,0,false,Set.of(),2,System.currentTimeMillis()+60000);
            String reason=policy.unsupportedReason(probe);
            if(reason==null || reason.isBlank()){
                LOG.info("[QuestBankRoute] selected={} destination={} from={}",
                    bank,destination,scene.start());
                return Optional.of(destination);
            }
            LOG.info("[QuestBankRoute] rejected={} reason={}",bank,reason);
        }
        return Optional.empty();
    }
    private static int distance(WorldPoint a,WorldPoint b){
        return Math.max(Math.abs(a.getX()-b.getX()),Math.abs(a.getY()-b.getY()));
    }
}
