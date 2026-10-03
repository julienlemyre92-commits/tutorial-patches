package net.runelite.client.plugins.microbot.questcommon.navigation;

import java.util.Optional;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;

/** Destination facts for the funding service; NavigationService owns movement. */
public final class FundingRouteDestinations {
    private static final WorldPoint GRAND_EXCHANGE=new WorldPoint(3165,3486,0);
    private FundingRouteDestinations() { }
    public static WorldPoint grandExchange(){return GRAND_EXCHANGE;}
    public static Optional<WorldPoint> nearestBank(){
        WorldPoint start=Microbot.getClientThread().invoke((Supplier<WorldPoint>)()->{
            Client c=Microbot.getClient();
            if(c==null || c.getGameState()!=GameState.LOGGED_IN || c.getLocalPlayer()==null)
                return null;
            return c.getLocalPlayer().getWorldLocation();
        });
        if(start==null)return Optional.empty();
        // Installed Rs2Bank.getNearestBank performs synchronous route planning.
        // Run it on the provider worker, never inside ClientThread.invoke.
        BankLocation bank=Rs2Bank.getNearestBank(start);
        return bank==null?Optional.empty():Optional.ofNullable(bank.getWorldPoint());
    }
}
