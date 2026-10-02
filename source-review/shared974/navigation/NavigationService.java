package net.runelite.client.plugins.microbot.questcommon.navigation;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceHub;

/** Exclusive navigation owner; the adapter retains Microbot's existing walker. */
public final class NavigationService implements QuestServiceHub.ServicePlugin {
    public interface Driver {
        NavigationArrival.Frame observe(NavigationGoal goal);
        /** Must check the complete route's access/risk/spend constraints; no input. */
        String unsupportedReason(NavigationGoal goal);
        /** Includes fresh collision/door/transport evidence, not only player position. */
        String sceneFingerprint();
        /** Start one worker; continuation predicate must guard its input. */
        void start(NavigationGoal goal,BooleanSupplier mayContinue);
        /** Nonblocking interrupt + clear; stopped proof comes from observe(). */
        void stop(String reason);
    }
    private final Driver driver;
    private QuestServiceHub.Lease lease;
    private NavigationGoal goal;
    private boolean started,stopping;
    private volatile boolean cancelled;
    private volatile boolean shutdownRequested;
    private int replans;
    private long lastProgressAt,stopAt;
    private Object lastPosition;
    private String attemptedScene="",terminalReason="";
    private QuestServiceHub.Outcome terminalOutcome;
    public NavigationService(Driver driver){this.driver=Objects.requireNonNull(driver);}
    @Override public QuestServiceHub.Kind kind(){return QuestServiceHub.Kind.NAVIGATION;}
    public boolean register(){return QuestServiceHub.register(this);}
    public boolean unregister(){return QuestServiceHub.unregister(this);}

    public synchronized void tick(){
        var current=QuestServiceHub.activeFor(this);
        if(current==null)return;
        if(!(current.request instanceof NavigationGoal requested)){
            QuestServiceHub.finish(current,this,QuestServiceHub.Outcome.HOLD,"Unsupported navigation request");return;
        }
        if(lease!=current){
            lease=current;goal=requested;started=stopping=cancelled=false;replans=0;
            lastPosition=null;attemptedScene="";terminalReason="";terminalOutcome=null;
            lastProgressAt=System.currentTimeMillis();
        }
        long now=System.currentTimeMillis();
        NavigationArrival.Frame frame;
        try{frame=driver.observe(goal);}
        catch(Exception ex){requestStop("navigation observation failed: "+ex,QuestServiceHub.Outcome.HOLD);return;}
        now=System.currentTimeMillis(); // Observation may complete after this tick began.
        if(QuestServiceHub.cancellationReason(current)!=null)shutdownRequested=true;
        if(shutdownRequested && !stopping){
            if(!started && frame!=null && frame.walkerStopped() && frame.clearingStopped()){
                QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.HOLD,
                    "navigation disabled before dispatch; no movement workers started");return;
            }
            requestStop("navigation plugin stopped",QuestServiceHub.Outcome.HOLD);
            return; // Reobserve worker handles after cancellation.
        }
        // Cancellation proof comes from local worker handles, even after logout.
        // A terminal failure must never require logging the character back in.
        if(stopping && terminalOutcome!=null && frame!=null
            && frame.walkerStopped() && frame.clearingStopped()){
            stopping=false;
            QuestServiceHub.finish(lease,this,terminalOutcome,terminalReason);return;
        }
        if(frame==null || !frame.loggedIn() || !goal.accountKey().equals(frame.accountKey())
            || frame.observedAt()<=0 || frame.observedAt()>now || now-frame.observedAt()>3000){
            requestStop("fresh same-account navigation frame unavailable",QuestServiceHub.Outcome.HOLD);return;
        }
        if(stopping){
            if(!frame.walkerStopped() || !frame.clearingStopped())return;
            stopping=false;
            if(terminalOutcome!=null){
                QuestServiceHub.finish(lease,this,terminalOutcome,terminalReason);return;
            }
            if(NavigationArrival.proved(goal,frame,now)){
                QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.COMPLETE,
                    "fresh reachable arrival and stopped walker/clear workers proved");return;
            }
            started=false; // A retry still needs changed scene evidence below.
        }
        if(now>goal.deadlineMillis()){
            requestStop("navigation deadline exceeded",QuestServiceHub.Outcome.HOLD);return;
        }
        if(NavigationArrival.proved(goal,frame,now)){
            QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.COMPLETE,
                "fresh reachable arrival with no active workers");return;
        }
        if(!Objects.equals(lastPosition,frame.position())){
            lastPosition=frame.position();lastProgressAt=now;
        }
        if(started){
            var positionProof=new NavigationArrival.Frame(frame.accountKey(),frame.observedAt(),
                frame.loggedIn(),frame.position(),frame.instanced(),frame.localDestinationReachable(),true,true);
            if(NavigationArrival.proved(goal,positionProof,now)){
                requestStop("destination reached; verify worker shutdown before handoff",null);return;
            }
            // Never trust the walker's return value as arrival.
            if(frame.walkerStopped() || now-lastProgressAt>15000)
                requestStop("walker ended or movement stalled",null);
            return;
        }
        if(!frame.walkerStopped() || !frame.clearingStopped()){
            requestStop("previous worker still active",QuestServiceHub.Outcome.HOLD);return;
        }
        String unsupported=driver.unsupportedReason(goal);
        if(unsupported!=null && !unsupported.isBlank()){
            QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.UNAVAILABLE,unsupported);return;
        }
        String scene=driver.sceneFingerprint();
        if(scene==null || scene.isBlank() || (!attemptedScene.isEmpty()
            && (scene.equals(attemptedScene) || replans>=goal.maximumReplans()))){
            QuestServiceHub.finish(lease,this,QuestServiceHub.Outcome.HOLD,
                "No changed route/collision evidence or replan limit reached");return;
        }
        if(!attemptedScene.isEmpty())replans++;
        attemptedScene=scene;cancelled=false;started=true;lastProgressAt=now;
        var ownedLease=lease;
        try{driver.start(goal,()->!shutdownRequested && !cancelled && System.currentTimeMillis()<requested.deadlineMillis()
            && QuestServiceHub.owns(ownedLease,this,requested.accountKey()));}
        catch(Exception ex){requestStop("walker start uncertain: "+ex,QuestServiceHub.Outcome.HOLD);}
    }
    private void requestStop(String reason,QuestServiceHub.Outcome outcome){
        cancelled=true;
        if(outcome!=null){terminalOutcome=outcome;terminalReason=reason;}
        if(stopping)return;
        stopping=true;stopAt=System.currentTimeMillis();
        driver.stop(reason);
    }
    public void stop(){
        // Plugin shutdown can run on the client thread while tick() waits for a
        // client-thread observation. Never acquire the tick monitor here.
        shutdownRequested=true;
        cancelled=true;
    }
    public synchronized String status(){
        return "started="+started+" stopping="+stopping+" replans="+replans
            +" stopAt="+stopAt+" reason="+terminalReason;
    }
}
