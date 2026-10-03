package net.runelite.client.plugins.microbot.questcommon.navigation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.*;

/** Conservative full-route preflight for the installed Microbot planner.
 * Allows zero-cost walking and simple zero-cost transport only. In particular,
 * it never approves a route from destination proximity alone.
 */
public final class VerifiedRoutePolicy implements NavigationMicrobotDriver.RoutePolicy {
    /** Called on the client thread. A trailing combat timer is not a live attacker. */
    static boolean hasLiveCombat(Client client) {
        Player player = client.getLocalPlayer();
        if (player == null || player.isDead()) return true;
        Actor target = player.getInteracting();
        if (target != null && !target.isDead()
            && (target instanceof Player || target instanceof NPC npc && npc.getCombatLevel() > 0))
            return true;
        for (var model : Microbot.getRs2NpcCache().query()
            .within(player.getWorldLocation(), 15).toListOnClientThread()) {
            NPC npc = model == null ? null : model.getNpc();
            if (npc != null && !npc.isDead() && npc.getCombatLevel() > 0
                && npc.getInteracting() == player) return true;
        }
        return false;
    }
    private record Scene(String account,WorldPoint start,boolean f2p,boolean instanced,
                         boolean combat,long at) { }

    @Override public String unsupportedReason(NavigationGoal goal) {
        try {
            Scene scene=Microbot.getClientThread().invoke((Supplier<Scene>)()->{
                Client c=Microbot.getClient();
                if(c==null || c.getGameState()!=GameState.LOGGED_IN || c.getLocalPlayer()==null)
                    return null;
                String user=c.getUsername(),name=c.getLocalPlayer().getName();
                if(user==null || name==null)return null;
                String key=user.trim().toLowerCase(Locale.ROOT)+"\n"
                    +name.trim().toLowerCase(Locale.ROOT);
                String account;
                try{account=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8)));}
                catch(Exception ex){throw new IllegalStateException("account identity unavailable",ex);}
                return new Scene(account,c.getLocalPlayer().getWorldLocation(),
                    !c.getWorldType().contains(WorldType.MEMBERS),c.isInInstancedRegion(),
                    hasLiveCombat(c),System.currentTimeMillis());
            });
            if(scene==null || scene.start()==null || !goal.accountKey().equals(scene.account())
                || System.currentTimeMillis()-scene.at()>3000)
                return "fresh same-account route origin unavailable";
            if(scene.instanced() || scene.combat())return "instance/combat route unsupported";
            if(!scene.f2p())return "members-world route requires separate access policy";
            if(goal.maximumRisk()>0)return "nonzero risk budget needs account/gear/food model";
            if(goal.allowTeleports())return "teleport use needs exact rune/item and spend proof";
            PathfinderConfig config=Rs2PathApi.getPathfinderConfig();
            if(config==null)return "Microbot route planner unavailable";
            if(!config.isAvoidWilderness() || !config.isAvoidDangerousNpcs()
                || config.isUseBankItems())
                return "walker route settings do not enforce zero-risk inventory-only travel";
            if(config.isMembersWorld())
                return "planner is configured for members routes";
            // The installed plan() resolves policy from the live config, even when
            // Rs2RouteRequest.withPolicy() is set. Inspect the actual resolved path.
            Rs2RouteRequest request=Rs2RouteRequest.to(scene.start(),goal.destination())
                .withBankItems(false).withPurpose(Rs2RouteRequest.Purpose.GENERAL);
            Rs2RouteResult plan=Rs2PathApi.plan(request);
            return inspect(goal,scene.start(),plan);
        } catch(Exception ex){return "route proof unavailable: "+ex;}
    }

    /** Host may call this when the walker changes its active route. */
    public String activeRouteUnsupportedReason(NavigationGoal goal) {
        try {
            Optional<Rs2RouteResult> active=Rs2PathApi.getActiveRoute();
            if(active.isEmpty())return "active route not yet available for inspection";
            WorldPoint start=active.get().getStart();
            return inspect(goal,start,active.get());
        } catch(Exception ex){return "active route proof unavailable: "+ex;}
    }

    static String inspect(NavigationGoal goal,WorldPoint start,Rs2RouteResult route) {
        if(route==null || !route.getTargets().contains(goal.destination())
            || route.getTerminationReason()!=Rs2RouteTermination.TARGET_REACHED
            || !route.isTargetReached(goal.arrivalRadius()))
            return "complete route to requested arrival radius not proved";
        List<WorldPoint> path=route.getPath();
        if(path==null || path.isEmpty() || start==null || route.getStart()==null
            || !start.equals(route.getStart()))return "complete route origin/path unavailable";
        if(!path.get(0).equals(start))return "route path does not begin at observed origin";
        for(WorldPoint tile:path){
            if(tile==null)return "route contains unknown tile";
            if(goal.forbiddenRegions().contains(tile.getRegionID()))
                return "route crosses forbidden region "+tile.getRegionID();
            if(Rs2PathApi.isInWilderness(tile))return "route crosses wilderness";
            if(Rs2PathApi.shouldAvoidDangerousTile(tile))
                return "route crosses known dangerous NPC tile";
        }
        List<Rs2RouteStep> steps=route.getSteps();
        if(steps==null || steps.isEmpty())return "route steps unavailable";
        WorldPoint previous=start;
        for(Rs2RouteStep step:steps){
            if(step==null || step.getFrom()==null || step.getTo()==null)
                return "route contains unknown step";
            if(!step.getFrom().equals(previous))return "route steps are not continuous";
            previous=step.getTo();
            if(goal.forbiddenRegions().contains(step.getFrom().getRegionID())
                || goal.forbiddenRegions().contains(step.getTo().getRegionID()))
                return "route step crosses forbidden region";
            if(Rs2PathApi.isInWilderness(step.getFrom())
                || Rs2PathApi.isInWilderness(step.getTo())
                || Rs2PathApi.shouldAvoidDangerousTile(step.getFrom())
                || Rs2PathApi.shouldAvoidDangerousTile(step.getTo()))
                return "route step crosses known dangerous tile";
            if(!step.isTransport()){
                if(step.getFrom().getPlane()!=step.getTo().getPlane()
                    || Math.abs(step.getFrom().getX()-step.getTo().getX())>1
                    || Math.abs(step.getFrom().getY()-step.getTo().getY())>1)
                    return "walking step skips uninspected tiles";
                continue;
            }
            Optional<Rs2TransportEdge> opt=step.getTransport();
            if(opt.isEmpty())return "transport metadata missing";
            Rs2TransportEdge edge=opt.get();
            if(edge.isMembers())return "members transport in F2P route";
            if(edge.isTeleport() || edge.isConsumable()
                || edge.getType()!=Rs2TransportType.TRANSPORT)
                return "teleport, consumable, or nonlocal transport requires separate proof";
            if(edge.getCurrencyAmount()!=0 || edge.getCurrencyName()!=null
                && !edge.getCurrencyName().isBlank())
                return "priced transport requires exact spend proof";
            if(!edge.getItemRequirements().isEmpty() || edge.isQuestGated()
                || edge.isSkillGated() || edge.isStateGated())
                return "gated transport requires current requirement proof";
            if(edge.getOrigin()==null || edge.getDestination()==null
                || edge.getOrigin().getPlane()!=edge.getDestination().getPlane()
                || Math.abs(edge.getOrigin().getX()-edge.getDestination().getX())>1
                || Math.abs(edge.getOrigin().getY()-edge.getDestination().getY())>1)
                return "nonadjacent or plane-changing transport requires route-specific proof";
            if(Rs2PathApi.isInWilderness(edge.getOrigin())
                || Rs2PathApi.isInWilderness(edge.getDestination()))
                return "transport crosses wilderness";
        }
        if(previous.getPlane()!=goal.destination().getPlane()
            || previous.distanceTo(goal.destination())>goal.arrivalRadius())
            return "route steps do not end in requested arrival area";
        return null;
    }
}
