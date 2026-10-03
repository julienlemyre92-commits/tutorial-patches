package net.runelite.client.plugins.microbot.questcommon.navigation.escape;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questcommon.navigation.NavigationMicrobotDriver;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.util.walker.*;

/** A bounded, local emergency move. The quest owner retains the input lease. */
public final class EmergencyLocalEscape {
    public interface Owner {
        /** True only after ordinary walker, approval, and clear workers have ended. */
        boolean ordinaryNavigationQuiescent();
        /** True only while this caller, not banking/death/quest clicks, owns input. */
        boolean exclusiveInputGranted();
    }
    public enum State { READY, MOVING, CLEARING, STABILIZING, ESCAPED, HOLD }
    public record Result(State state, String detail, WorldPoint target) { }
    private record Scene(String account, int tick, long at, WorldPoint player,
                         int hp, int maxHp, boolean f2p, boolean instance,
                         List<WorldPoint> attackers) { }
    private record Choice(WorldPoint target, int gain, int pathLength) { }

    private final Owner owner;
    private final NavigationMicrobotDriver.GuardRuntime guard;
    private final String account;
    private volatile Thread worker, clearer, validator;
    private volatile Throwable workerError, clearError, validatorError;
    private volatile boolean stop;
    private volatile boolean guardEnabled;
    private volatile String guardStatus="not started";
    private WorldPoint origin, target;
    private final Set<WorldPoint> attemptedTargets=new HashSet<>();
    private int startTick, attempts, stableTick, stableHp, observedTick, stagnantTicks;
    private WorldPoint observedPosition;
    private long startedAt;
    private State state=State.READY;
    private String detail="waiting for fresh emergency scene";
    private static final int MAX_ATTEMPTS=2;
    private static final long MAX_MOVE_MS=7000;

    public EmergencyLocalEscape(Owner owner, NavigationMicrobotDriver.GuardRuntime guard,
                                String accountKey) {
        this.owner=Objects.requireNonNull(owner);
        this.guard=Objects.requireNonNull(guard);
        this.account=Objects.requireNonNull(accountKey);
    }

    /** Call from the quest's serialized controller, never from a walker callback. */
    public synchronized Result tick() {
        Scene s;
        try{s=capture();}catch(Exception ex){return hold("emergency scene read failed: "+ex);}
        if(s==null || !account.equals(s.account) || System.currentTimeMillis()-s.at>1500
            || s.player==null || s.hp<=0 || s.maxHp<=0 || !s.f2p || s.instance)
            return hold("fresh same-account F2P non-instance scene unavailable");
        if(!owner.exclusiveInputGranted())return hold("exclusive input lease lost");
        if(state==State.HOLD || state==State.ESCAPED)return result();
        if(state==State.STABILIZING){
            if(!s.attackers.isEmpty() || s.hp<stableHp){
                if(attempts>=MAX_ATTEMPTS)return hold("attacker or HP loss returned after local move");
                state=State.READY;
            }else if(s.tick>stableTick){
                state=State.ESCAPED;
                detail="movement plus later attack-free, stable-HP tick proved; reassess supplies before quest";
                return result();
            }else return result();
        }
        if(state==State.CLEARING){
            if(alive(clearer))return result();
            if(clearError!=null)return hold("route clear failed: "+clearError);
            worker=null;clearer=null;validator=null;
            if(s.tick>startTick && s.player.distanceTo2D(origin)>0
                && s.hp>0 && s.attackers.isEmpty()){
                state=State.STABILIZING;stableTick=s.tick;stableHp=s.hp;
                detail="movement proved; awaiting another attack-free, stable-HP tick";
                return result();
            }
            if(attempts>=MAX_ATTEMPTS)return hold("bounded local escape attempts exhausted; player="+s.player+" hp="+s.hp);
            state=State.READY;
        }
        if(state==State.MOVING){
            if(s.tick>observedTick){
                boolean advanced=!s.player.equals(observedPosition);
                int elapsedTicks=s.tick-observedTick;
                observedTick=s.tick;observedPosition=s.player;
                if(!advanced){
                    stagnantTicks+=elapsedTicks;
                    if(stagnantTicks>=2){
                        detail="walk made no verified movement within two game ticks; clearing for alternate route";
                        beginClear();return result();
                    }
                    detail="awaiting bounded dispatch grace: one game tick without movement";
                }else{
                    stagnantTicks=0;
                    if(s.player.equals(target) || s.attackers.isEmpty()){
                        detail="local movement proved; clearing route for combat reassessment";
                        beginClear();return result();
                    }
                    detail="movement proved at tick "+s.tick+"; continuing bounded escape";
                }
            }
            if(System.currentTimeMillis()-startedAt>MAX_MOVE_MS
                || workerError!=null || validatorError!=null || !alive(worker)){
                detail="walker/validator ended or timed out; walk="+workerError
                    +" validator="+validatorError+" guard="+guardStatus;
                beginClear();return result();
            }
            return result();
        }
        if(!owner.ordinaryNavigationQuiescent())return hold("ordinary walker or clear worker still active");
        if(guard.installationFailure()!=null)return hold("walker input guard unavailable: "+guard.installationFailure());
        if(!quiescent())return hold("prior emergency worker or guard still active");
        if(s.attackers.isEmpty())return hold("no observed attacker location; cannot choose an escape direction");
        Choice choice;
        try{choice=choose(s);}catch(Exception ex){return hold("local path proof failed: "+ex);}
        if(choice==null)return hold("no collision-reachable local route away from observed attackers");
        Scene latest=capture();
        if(latest==null || !account.equals(latest.account)
            || System.currentTimeMillis()-latest.at>1500 || latest.tick!=s.tick
            || !s.player.equals(latest.player) || !s.attackers.equals(latest.attackers)){
            detail="scene changed during local path planning; rescan next tick";
            return result();
        }
        attempts++;origin=s.player;target=choice.target;attemptedTargets.add(target);
        startTick=s.tick;observedTick=s.tick;observedPosition=s.player;stagnantTicks=0;
        startedAt=System.currentTimeMillis();workerError=null;validatorError=null;stop=false;
        try{guard.enable();guardEnabled=true;}
        catch(Exception ex){return hold("emergency input guard could not start: "+ex);}
        state=State.MOVING;detail="attempt "+attempts+" target="+target+" distance gain="+choice.gain;
        worker=new Thread(()->{
            boolean priorTeleports=Rs2Walker.disableTeleports;
            try {
                Rs2Walker.disableTeleports=true;
                Rs2Walker.walkWithStateUntil(target,0,this::shouldStopWorker);
            }
            catch(Throwable ex){workerError=ex;}
            finally{
                try{guard.revoke();}catch(Throwable ex){workerError=ex;}
                Rs2Walker.disableTeleports=priorTeleports;
            }
        },"QuestEmergencyLocalEscape-walk");
        worker.setDaemon(true);worker.start();
        validator=new Thread(()->{
            try{
                while(!stop && owner.exclusiveInputGranted() && alive(worker)){
                    guardStatus=approveActiveLocalRoute();
                    Thread.sleep(100);
                }
            }catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
            catch(Throwable ex){validatorError=ex;stop=true;}
            finally{guard.revoke();}
        },"QuestEmergencyLocalEscape-approval");
        validator.setDaemon(true);validator.start();
        return result();
    }

    /** Owner calls this on stop/reload and waits until quiescent() is true. */
    public synchronized void cancel(){
        if(state==State.MOVING)beginClear();
        else if(state==State.READY || state==State.STABILIZING)state=State.HOLD;
    }
    public synchronized boolean quiescent(){
        return !alive(worker) && !alive(clearer) && !alive(validator) && !guardEnabled;
    }
    private void beginClear(){
        stop=true;guard.revoke();
        if(alive(worker))worker.interrupt();
        if(alive(validator))validator.interrupt();
        state=State.CLEARING;
        clearer=new Thread(()->{
            try {
                Thread w=worker;
                if(w!=null)w.join(3000);
                if(alive(w))throw new IllegalStateException("escape walker did not stop");
                Thread v=validator;
                if(v!=null)v.join(3000);
                if(alive(v))throw new IllegalStateException("escape approval worker did not stop");
                Rs2Walker.clearWalkingRoute("quest emergency local escape stop");
                if(guardEnabled){guard.disable();guardEnabled=false;}
            }catch(Throwable ex){clearError=ex;}
        },"QuestEmergencyLocalEscape-clear");
        clearer.setDaemon(true);clearer.start();
    }
    private Result hold(String why){
        if(state==State.MOVING){detail=why;beginClear();return result();}
        state=State.HOLD;detail=why;return result();
    }
    private Result result(){return new Result(state,detail,target);}
    private static boolean alive(Thread t){return t!=null && t.isAlive();}
    private boolean shouldStopWorker(){
        try{
            boolean end=stop || Thread.currentThread().isInterrupted() || !owner.exclusiveInputGranted();
            if(end)guard.revoke();
            return end;
        }
        catch(Throwable ex){
            workerError=ex;stop=true;
            try{guard.revoke();}catch(Throwable revokeError){workerError=revokeError;}
            return true;
        }
    }

    /** A recalculated route is approved only after rechecking the exact active path. */
    private String approveActiveLocalRoute(){
        try{
            Scene before=capture();
            if(before==null || !account.equals(before.account) || before.player==null
                || before.hp<=0 || before.attackers.isEmpty() || before.instance || !before.f2p
                || !owner.exclusiveInputGranted())return reject("emergency context changed");
            long generation=guard.generation();
            Pathfinder exact=ShortestPathPlugin.pathfinder;
            var future=ShortestPathPlugin.pathfinderFuture;
            if(exact==null || !exact.isDone() || future!=null && !future.isDone())
                return reject("active local pathfinder pending");
            if(!target.equals(Rs2Walker.getCurrentTarget()))return reject("walker target changed");
            var active=Rs2PathApi.getActiveRoute();
            if(active.isEmpty())return reject("active route snapshot missing");
            Rs2RouteResult route=active.get();
            List<WorldPoint> path=List.copyOf(exact.getPath());
            if(!path.equals(route.getPath()) || !exact.getStart().equals(route.getStart())
                || !exact.getTargets().equals(route.getTargets())
                || !route.getTargets().contains(target) || !route.isTargetReached(0)
                || route.getTransportSteps()==null || !route.getTransportSteps().isEmpty()
                || path.size()<2 || path.size()>15 || !path.get(0).equals(origin)
                || !path.get(path.size()-1).equals(target))
                return reject("active route differs from bounded local proof");
            int currentIndex=path.indexOf(before.player);
            if(currentIndex<0)return reject("player left the proved local path");
            int currentDistance=nearest(before.player,before.attackers);
            WorldPoint previous=before.player;
            for(int i=currentIndex;i<path.size();i++){
                WorldPoint tile=path.get(i);
                if(tile==null || tile.getPlane()!=origin.getPlane()
                    || previous.distanceTo2D(tile)>1 || origin.distanceTo2D(tile)>10
                    || nearest(tile,before.attackers)<currentDistance
                    || Rs2PathApi.isInWilderness(tile)
                    || Rs2PathApi.shouldAvoidDangerousTile(tile))
                    return reject("active local path crosses unapproved tile");
                previous=tile;
            }
            if(!before.player.equals(target)
                && nearest(target,before.attackers)<=currentDistance)
                return reject("target no longer increases attacker separation");
            Scene after=capture();
            if(after==null || !account.equals(after.account) || after.tick!=before.tick
                || !before.player.equals(after.player)
                || !before.attackers.equals(after.attackers)
                || System.currentTimeMillis()-after.at>1000)
                return reject("emergency scene changed during active-route validation");
            if(ShortestPathPlugin.pathfinder!=exact || guard.generation()!=generation
                || !path.equals(exact.getPath()) || !owner.exclusiveInputGranted())
                return reject("route or ownership changed before publication");
            return guard.approveValidated(exact,target,500,generation)
                ? "local path approved" : reject("exact route guard rejected publication");
        }catch(Throwable ex){return reject("local route validation error: "+ex);}
    }
    private String reject(String reason){guard.revoke();return reason;}

    private static Scene capture(){
        return Microbot.getClientThread().invoke((Supplier<Scene>)()->{
            Client c=Microbot.getClient();
            if(c==null || c.getGameState()!=GameState.LOGGED_IN || c.getLocalPlayer()==null)return null;
            Player p=c.getLocalPlayer();
            String identity;
            try {
                String key=c.getUsername().trim().toLowerCase(Locale.ROOT)+"\n"
                    +p.getName().trim().toLowerCase(Locale.ROOT);
                identity=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8)));
            }catch(Exception ex){return null;}
            List<WorldPoint> attackers=new ArrayList<>();
            for(var model:Microbot.getRs2NpcCache().query()
                .within(p.getWorldLocation(),12).toListOnClientThread()){
                NPC npc=model==null?null:model.getNpc();
                if(npc!=null && !npc.isDead() && npc.getCombatLevel()>0
                    && npc.getInteracting()==p && npc.getWorldLocation()!=null)
                    attackers.add(npc.getWorldLocation());
            }
            for(Player other:c.getPlayers())if(other!=null && other!=p
                && !other.isDead() && other.getInteracting()==p
                && other.getWorldLocation()!=null)attackers.add(other.getWorldLocation());
            return new Scene(identity,c.getTickCount(),System.currentTimeMillis(),
                p.getWorldLocation(),c.getBoostedSkillLevel(Skill.HITPOINTS),
                c.getRealSkillLevel(Skill.HITPOINTS),
                !c.getWorldType().contains(WorldType.MEMBERS),c.isInInstancedRegion(),
                List.copyOf(attackers));
        });
    }

    private Choice choose(Scene s){
        PathfinderConfig config=Rs2PathApi.getPathfinderConfig();
        if(config==null || !config.isAvoidWilderness() || !config.isAvoidDangerousNpcs()
            || config.isUseBankItems()
            || config.isMembersWorld())return null;
        int initial=nearest(s.player,s.attackers);
        Choice best=null;
        List<WorldPoint> targets=new ArrayList<>();
        int[][] directions={{1,0},{2,1},{1,1},{1,2},{0,1},{-1,2},{-1,1},{-2,1},
            {-1,0},{-2,-1},{-1,-1},{-1,-2},{0,-1},{1,-2},{1,-1},{2,-1}};
        for(int distance:new int[]{3,5,7})for(int[] direction:directions){
            int scale=Math.max(Math.abs(direction[0]),Math.abs(direction[1]));
            WorldPoint t=new WorldPoint(s.player.getX()+direction[0]*distance/scale,
                s.player.getY()+direction[1]*distance/scale,s.player.getPlane());
            if(!attemptedTargets.contains(t) && nearest(t,s.attackers)-initial>=3)targets.add(t);
        }
        targets.sort(Comparator.comparingInt((WorldPoint t)->nearest(t,s.attackers)).reversed());
        synchronized(Rs2PathApi.getPathfinderMutex()){
            boolean priorIgnore=config.isIgnoreTeleportAndItems();
            boolean priorDisable=Rs2Walker.disableTeleports;
            try {
                config.setIgnoreTeleportAndItems(true);Rs2Walker.disableTeleports=true;
                for(WorldPoint t:targets){
                    int gain=nearest(t,s.attackers)-initial;
                    if(gain<3 || Rs2PathApi.isInWilderness(t) || Rs2PathApi.shouldAvoidDangerousTile(t))continue;
                    Rs2RouteResult route=Rs2PathApi.plan(Rs2RouteRequest.to(s.player,t).withBankItems(false));
                    if(route==null || !s.player.equals(route.getStart())
                        || !route.getTargets().contains(t) || !route.isTargetReached(0)
                        || route.getPath()==null
                        || route.getPath().isEmpty() || route.getTransportSteps()==null
                        || !route.getTransportSteps().isEmpty() || route.getPath().size()>15
                        || !route.getPath().get(0).equals(s.player))continue;
                    boolean valid=true;WorldPoint previous=s.player;
                    for(WorldPoint tile:route.getPath()){
                        if(tile==null || tile.getPlane()!=s.player.getPlane()
                            || previous.distanceTo2D(tile)>1
                            || s.player.distanceTo2D(tile)>10
                            || Rs2PathApi.isInWilderness(tile)
                            || Rs2PathApi.shouldAvoidDangerousTile(tile)
                            || nearest(tile,s.attackers)<initial) {valid=false;break;}
                        previous=tile;
                    }
                    if(!valid || !previous.equals(t))continue;
                    Choice candidate=new Choice(t,gain,route.getPath().size());
                    if(best==null || candidate.gain>best.gain
                        || candidate.gain==best.gain && candidate.pathLength<best.pathLength)
                        best=candidate;
                }
                return best;
            }finally{config.setIgnoreTeleportAndItems(priorIgnore);
                Rs2Walker.disableTeleports=priorDisable;}
        }
    }
    private static int nearest(WorldPoint tile,List<WorldPoint> attackers){
        int nearest=Integer.MAX_VALUE;
        for(WorldPoint attacker:attackers)if(attacker.getPlane()==tile.getPlane())
            nearest=Math.min(nearest,tile.distanceTo2D(attacker));
        return nearest;
    }
}
