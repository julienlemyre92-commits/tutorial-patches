package net.runelite.client.plugins.microbot.questcommon.navigation;

import java.security.MessageDigest;
import java.util.*;
import java.util.function.*;
import net.runelite.api.*;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questcommon.navigation.escape.EmergencyLocalEscape;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;

/** Uses the installed walker unchanged, including its accumulated door/transport fixes. */
public final class NavigationMicrobotDriver implements NavigationService.Driver, EmergencyHandoff {
    public interface RoutePolicy {
        /** Null only when the requested route's access, danger and costs are verified. */
        String unsupportedReason(NavigationGoal goal);
        default String activeRouteUnsupportedReason(NavigationGoal goal){
            return "Active route validation not implemented";
        }
    }
    public interface GuardRuntime extends NavigationApprovalPublisher.Gate {
        /** Null only after exact installed instrumentation has been verified. */
        String installationFailure();
        void enable();
        void disable();
    }
    private final RoutePolicy policy;
    private final GuardRuntime guard;
    private volatile Thread walker,clearer,validator;
    private volatile boolean guardEnabled;
    private volatile boolean stopped;
    private volatile String failure="";
    private volatile String approvalStatus="not started";
    private volatile EmergencyLocalEscape emergency;
    public NavigationMicrobotDriver(RoutePolicy policy){this(policy,null);}
    public NavigationMicrobotDriver(RoutePolicy policy,GuardRuntime guard){
        this.policy=Objects.requireNonNull(policy);this.guard=guard;
    }
    private static boolean ended(Thread thread){return thread==null || !thread.isAlive();}

    /** Source-backed proof for provider replacement; no route worker or guard may remain. */
    public boolean idleForReload(){
        return ordinaryQuiescent() && emergencyQuiescent();
    }
    @Override public boolean ordinaryQuiescent(){
        return ended(walker) && ended(clearer) && ended(validator) && !guardEnabled;
    }
    @Override public boolean emergencyQuiescent(){
        EmergencyLocalEscape current=emergency;
        return current==null || current.quiescent();
    }
    @Override public synchronized void beginEmergency(NavigationGoal goal,BooleanSupplier ownsInput){
        if(!ordinaryQuiescent() || !emergencyQuiescent() || emergency!=null)
            throw new IllegalStateException("old navigation or emergency worker still owns input");
        if(guard==null || guard.installationFailure()!=null)
            throw new IllegalStateException("installed per-input route guard unavailable");
        emergency=new EmergencyLocalEscape(new EmergencyLocalEscape.Owner(){
            @Override public boolean ordinaryNavigationQuiescent(){return ordinaryQuiescent();}
            @Override public boolean exclusiveInputGranted(){return ownsInput.getAsBoolean();}
        },guard,goal.accountKey());
    }
    @Override public EmergencyLocalEscape.Result tickEmergency(){
        EmergencyLocalEscape current=emergency;
        if(current==null)throw new IllegalStateException("emergency escape not started");
        return current.tick();
    }
    @Override public void cancelEmergency(){
        EmergencyLocalEscape current=emergency;
        if(current!=null)current.cancel();
    }
    @Override public synchronized void releaseEmergency(){
        if(!emergencyQuiescent())throw new IllegalStateException("emergency worker still active");
        emergency=null;
    }

    @Override public NavigationArrival.Frame observe(NavigationGoal goal){
        return Microbot.getClientThread().invoke((Supplier<NavigationArrival.Frame>)()->{
            Client client=Microbot.getClient();
            if(client==null || client.getGameState()!=GameState.LOGGED_IN || client.getLocalPlayer()==null)
            return new NavigationArrival.Frame("",System.currentTimeMillis(),false,null,false,false,
                    ended(walker) && ended(validator) && emergencyQuiescent(),
                    ended(clearer) && !guardEnabled && emergencyQuiescent());
            String identity=identity(client);
            var player=client.getLocalPlayer().getWorldLocation();
            boolean reachable=emergency==null && player!=null && player.getPlane()==goal.destination().getPlane()
                && player.distanceTo(goal.destination())<=16 && Rs2Walker.canReach(goal.destination());
            return new NavigationArrival.Frame(identity,System.currentTimeMillis(),true,player,
                client.isInInstancedRegion(),reachable,
                ended(walker) && ended(validator) && emergencyQuiescent(),
                ended(clearer) && !guardEnabled && emergencyQuiescent());
        });
    }
    static String identity(Client client){
        try{
            String username=client.getUsername(),name=client.getLocalPlayer().getName();
            if(username==null || name==null)throw new IllegalStateException("Missing account identity");
            String value=username.trim().toLowerCase(Locale.ROOT)+"\n"+name.trim().toLowerCase(Locale.ROOT);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }catch(Exception ex){throw new IllegalStateException(ex);}
    }
    @Override public String unsupportedReason(NavigationGoal goal){
        if(!failure.isEmpty())return "Prior walker failure: "+failure;
        if(guard==null)return "Installed walker lacks verified per-input route approval hook";
        String installation=guard.installationFailure();
        return installation!=null?installation:policy.unsupportedReason(goal);
    }
    @Override public String interruptionReason(NavigationGoal goal){
        return Microbot.getClientThread().invoke((Supplier<String>)()->{
            Client client=Microbot.getClient();
            if(client==null || client.getGameState()!=GameState.LOGGED_IN
                || client.getLocalPlayer()==null)return "navigation context unavailable";
            if(!goal.accountKey().equals(identity(client)))return "navigation account changed";
            Player player=client.getLocalPlayer();
            if(player.getWorldLocation()==null)return "navigation player location unavailable";
            boolean incoming=false;
            for(var model:Microbot.getRs2NpcCache().query()
                .within(player.getWorldLocation(),15).toListOnClientThread()){
                NPC npc=model==null?null:model.getNpc();
                if(npc!=null && !npc.isDead() && npc.getCombatLevel()>0
                    && npc.getInteracting()==player){incoming=true;break;}
            }
            if(!incoming)for(Player other:client.getPlayers()){
                if(other!=null && other!=player && !other.isDead()
                    && other.getInteracting()==player){incoming=true;break;}
            }
            if(incoming)
                return "EMERGENCY_HANDOFF_REQUIRED: observed attacker interrupted ordinary navigation";
            if(client.isInInstancedRegion())return "ordinary navigation entered an unsupported instance";
            return null;
        });
    }
    @Override public String sceneFingerprint(){
        return Microbot.getClientThread().invoke((Supplier<String>)()->{
            Client client=Microbot.getClient();
            if(client==null || client.getLocalPlayer()==null)return "";
            var position=client.getLocalPlayer().getWorldLocation();
            CollisionData[] maps=client.getCollisionMaps();
            if(position==null || maps==null || position.getPlane()>=maps.length
                || maps[position.getPlane()]==null)return "";
            return position+":"+client.getBaseX()+":"+client.getBaseY()+":"
                +Arrays.deepHashCode(maps[position.getPlane()].getFlags());
        });
    }
    @Override public synchronized void start(NavigationGoal goal,BooleanSupplier mayContinue){
        String unsupported=unsupportedReason(goal);
        if(unsupported!=null)throw new IllegalStateException(unsupported);
        if(!ordinaryQuiescent() || emergency!=null)
            throw new IllegalStateException("Old navigation workers or guard still active");
        stopped=false;
        guardEnabled=true;
        guard.enable();
        walker=new Thread(()->{
            boolean priorTeleports=Rs2Walker.disableTeleports;
            try{
                if(stopped || !mayContinue.getAsBoolean())return;
                if(!goal.allowTeleports())Rs2Walker.disableTeleports=true;
                // Installed bytecode names this completionCondition: true ends walking.
                // Its ARRIVED return is deliberately ignored; service proves position.
                Rs2Walker.walkWithStateUntil(goal.destination(),goal.arrivalRadius(),
                    ()->shouldStop(goal,mayContinue));
            }catch(Exception ex){failure=ex.toString();}
            finally{guard.revoke();Rs2Walker.disableTeleports=priorTeleports;}
        },"QuestNavigation-walker");
        walker.setDaemon(true);walker.start();
        validator=new Thread(()->{
            var publisher=new NavigationApprovalPublisher(guard);
            try{
                while(!stopped && !ended(walker) && mayContinue.getAsBoolean()){
                    approvalStatus=publisher.refresh(goal,()->!stopped && !ended(walker) && mayContinue.getAsBoolean());
                    Thread.sleep(300);
                }
            }catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
            catch(Throwable ex){failure="approval worker failed: "+ex;stopped=true;}
            finally{guard.revoke();}
        },"QuestNavigation-approval");
        validator.setDaemon(true);validator.start();
    }
    private boolean shouldStop(NavigationGoal goal,BooleanSupplier mayContinue){
        // The installed walker catches a throwing completion predicate and then
        // continues distance-based walking. Convert every failure into true.
        try{
            if(stopped || Thread.currentThread().isInterrupted() || !mayContinue.getAsBoolean()){
                guard.revoke();return true;
            }
            // Do not acquire pathfinderMutex from a callback holding walkerLock.
            // Route approval belongs in a nonblocking per-input walker hook.
            return false;
        }catch(Throwable ex){failure="navigation guard failed: "+ex;stopped=true;return true;}
    }
    @Override public synchronized void stop(String reason){
        stopped=true;
        if(guard!=null)guard.revoke();
        if(emergency!=null){emergency.cancel();return;}
        if(!ended(walker))walker.interrupt();
        if(!ended(validator))validator.interrupt();
        if(!ended(clearer))return;
        clearer=new Thread(()->{
            try{
                Rs2Walker.clearWalkingRoute("quest-navigation:"+reason);
                if(walker!=null)walker.join();
                if(validator!=null)validator.join();
                if(guardEnabled){guard.disable();guardEnabled=false;}
            }
            catch(Exception ex){failure="route clear: "+ex;}
        },"QuestNavigation-clear");
        clearer.setDaemon(true);clearer.start();
    }
    public String approvalStatus(){return approvalStatus;}
    @Override public String approvalDiagnostic(){
        String reason=failure.isEmpty()?approvalStatus:approvalStatus+"; worker="+failure;
        try{return reason+"; input="+guard.getClass().getMethod("diagnostic").invoke(guard);}
        catch(Exception unavailable){return reason+"; input=unavailable";}
    }
}
