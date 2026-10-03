package net.runelite.client.plugins.microbot.questcommon.navigation.guard;

import java.util.Objects;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathEdge;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/** Candidate primitive for an exact-source or bytecode guard installation.
 * No effect until EVERY walker input and EVERY active-route mutation is wrapped.
 */
public final class RouteInputGuard {
    private record Approval(long generation,Pathfinder route,WorldPoint target,long expiresAt,
                            List<WorldPoint> rawPath,List<WorldPoint> walkablePath,
                            List<PathEdge> edges) { }
    private static final ReentrantReadWriteLock GATE=new ReentrantReadWriteLock();
    private static final AtomicLong GENERATION=new AtomicLong();
    private static final AtomicLong DENIALS=new AtomicLong();
    private static final AtomicReference<Approval> APPROVED=new AtomicReference<>();
    private static volatile boolean enabled;
    private static volatile String lastDecision="not checked";
    private RouteInputGuard() { }

    /** Capture before reading the route for external validation. */
    public static long generation(){return GENERATION.get();}
    /** Bounded diagnostic; no account, path, or screen data is retained. */
    public static String diagnostic(){return lastDecision+" denials="+DENIALS.get();}
    private static Permit denied(String reason){
        lastDecision=reason;DENIALS.incrementAndGet();return null;
    }

    /** Navigation owner enables the gate before starting its walker. */
    public static void enable(){
        Lock lock=GATE.writeLock();lock.lock();
        try{APPROVED.set(null);GENERATION.incrementAndGet();DENIALS.set(0);
            lastDecision="enabled, awaiting approval";enabled=true;}
        finally{lock.unlock();}
    }
    public static void disable(){
        APPROVED.set(null);
        Lock lock=GATE.writeLock();lock.lock();
        try{enabled=false;GENERATION.incrementAndGet();}
        finally{lock.unlock();}
    }

    /** Called only after an external validator has inspected the exact completed route. */
    public static boolean approve(Pathfinder exactRoute,WorldPoint target,long validMillis) {
        return approveValidated(exactRoute,target,validMillis,generation());
    }

    /** Publish only if the generation captured before validation remains current. */
    public static boolean approveValidated(Pathfinder exactRoute,WorldPoint target,long validMillis,
                                           long expectedGeneration) {
        if(exactRoute==null || target==null || validMillis<1 || validMillis>10_000){
            lastDecision="PUBLISH_INVALID_ARGUMENT";return false;
        }
        Lock lock=GATE.writeLock();lock.lock();
        try {
            Future<?> future=ShortestPathPlugin.pathfinderFuture;
            if(!enabled){lastDecision="PUBLISH_DISABLED";return false;}
            if(GENERATION.get()!=expectedGeneration){lastDecision="PUBLISH_GENERATION_CHANGED";return false;}
            if(ShortestPathPlugin.pathfinder!=exactRoute){lastDecision="PUBLISH_ROUTE_CHANGED";return false;}
            if(!exactRoute.isDone() || future!=null && !future.isDone()){
                lastDecision="PUBLISH_ROUTE_PENDING";return false;
            }
            if(!target.equals(Rs2Walker.getCurrentTarget())){
                lastDecision="PUBLISH_TARGET_CHANGED";return false;
            }
            List<WorldPoint> raw,walkable;
            List<PathEdge> edges;
            try {
                raw=List.copyOf(exactRoute.getPath());
                walkable=List.copyOf(exactRoute.getWalkablePath());
                edges=List.copyOf(exactRoute.getPathEdges());
                if(!raw.equals(exactRoute.getPath()) || !walkable.equals(exactRoute.getWalkablePath())
                    || !sameEdges(edges,exactRoute.getPathEdges())){
                    lastDecision="PUBLISH_ROUTE_CONTENTS_CHANGED";return false;
                }
            } catch(RuntimeException inconsistent){
                lastDecision="PUBLISH_ROUTE_READ_FAILED";return false;
            }
            if(GENERATION.get()!=expectedGeneration || ShortestPathPlugin.pathfinder!=exactRoute
                || !exactRoute.isDone()){
                lastDecision="PUBLISH_ROUTE_CHANGED_AFTER_READ";return false;
            }
            APPROVED.set(new Approval(expectedGeneration,exactRoute,target,
                System.currentTimeMillis()+validMillis,raw,walkable,edges));
            lastDecision="PUBLISHED";
            return true;
        } finally {lock.unlock();}
    }

    /** Pre-input check; caller must hold the returned permit until the input call ends. */
    public static Permit tryEnter(WorldPoint target) {
        if(!enabled)return new Permit(null);
        Lock lock=GATE.writeLock();
        if(!lock.tryLock())return denied("INPUT_GATE_BUSY");
        boolean valid=false;
        try {
            Approval a=APPROVED.get();
            Future<?> future=ShortestPathPlugin.pathfinderFuture;
            if(a==null)return denied("INPUT_NO_APPROVAL");
            if(target==null || !target.equals(a.target()))return denied("INPUT_TARGET_MISMATCH");
            if(a.generation()!=GENERATION.get())return denied("INPUT_GENERATION_CHANGED");
            if(a.expiresAt()<System.currentTimeMillis())return denied("INPUT_APPROVAL_EXPIRED");
            if(a.route()!=ShortestPathPlugin.pathfinder)return denied("INPUT_ROUTE_CHANGED");
            if(!a.route().isDone() || future!=null && !future.isDone())
                return denied("INPUT_ROUTE_PENDING");
            if(!target.equals(Rs2Walker.getCurrentTarget()))
                return denied("INPUT_WALKER_TARGET_CHANGED");
            if(!sameRouteContents(a))return denied("INPUT_ROUTE_CONTENTS_CHANGED");
            valid=true;lastDecision="INPUT_APPROVED";
            return new Permit(lock);
        } finally {if(!valid)lock.unlock();}
    }

    /** No pathfinder-mutex acquisition or route inspection on the input thread. */
    public static Permit tryEnterCurrent(){
        if(!enabled)return new Permit(null);
        return tryEnter(Rs2Walker.getCurrentTarget());
    }

    /** An external transport implementation has unknown deferred inputs; fail closed only for guarded navigation. */
    public static Permit denyWhenEnabled(){return enabled?null:new Permit(null);}

    /** The write lock must be acquired before pathfinderMutex or other route locks. */
    public static void routeMutation(Runnable mutation) {
        Objects.requireNonNull(mutation);
        APPROVED.set(null); // fail closed even while waiting for a current input to finish
        Lock lock=GATE.writeLock();lock.lock();
        try {GENERATION.incrementAndGet();mutation.run();}
        finally {lock.unlock();}
    }

    /** Bytecode mutation hook; close in a finally block around the entire mutator. */
    public static Permit beginMutation(){
        APPROVED.set(null);
        Lock lock=GATE.writeLock();lock.lock();
        GENERATION.incrementAndGet();
        return new Permit(lock);
    }

    /** Serialize active Pathfinder.run/cancel with a permitted input. */
    public static Permit beginPathfinderMutation(Pathfinder route){
        if(!enabled || route==null || ShortestPathPlugin.pathfinder!=route)return new Permit(null);
        APPROVED.set(null);
        Lock lock=GATE.writeLock();lock.lock();
        GENERATION.incrementAndGet();
        return new Permit(lock);
    }

    private static boolean sameRouteContents(Approval a){
        try {
            return a.rawPath().equals(a.route().getPath())
                && a.walkablePath().equals(a.route().getWalkablePath())
                && sameEdges(a.edges(),a.route().getPathEdges());
        } catch(RuntimeException inconsistent){return false;}
    }

    /** Expose the approved immutable list to callers; detect a changed backing route. */
    public static List<?> freezeApprovedRouteList(Pathfinder route,List<?> actual,int kind){
        if(!enabled)return actual;
        Approval a=APPROVED.get();
        if(a==null || a.route()!=route){
            if(actual==null)return null;
            try {return List.copyOf(actual);}
            catch(RuntimeException inconsistent){revoke();return List.of();}
        }
        List<?> expected=switch(kind){
            case 0 -> a.rawPath();
            case 1 -> a.walkablePath();
            case 2 -> a.edges();
            default -> throw new IllegalArgumentException("route list kind "+kind);
        };
        try {
            if(actual!=null && (kind==2 ? sameEdges(expected,actual) : expected.equals(actual)))
                return expected;
        } catch(RuntimeException inconsistent){/* deny below */}
        revoke();
        if(actual==null)return null;
        try {return List.copyOf(actual);}
        catch(RuntimeException inconsistent){return List.of();}
    }

    public static void revoke(){APPROVED.set(null);GENERATION.incrementAndGet();}

    /** Pathfinder can recreate edge wrappers on every read. Compare their route meaning. */
    private static boolean sameEdges(List<?> first,List<?> second){
        if(first==second)return true;
        if(first==null || second==null || first.size()!=second.size())return false;
        for(int i=0;i<first.size();i++){
            if(!(first.get(i) instanceof PathEdge a) || !(second.get(i) instanceof PathEdge b)
                || !Objects.equals(a.getFrom(),b.getFrom())
                || !Objects.equals(a.getTo(),b.getTo())
                || !Objects.equals(a.getTransport(),b.getTransport()))return false;
        }
        return true;
    }

    public static final class Permit implements AutoCloseable {
        private final Lock lock;
        private boolean closed;
        private Permit(Lock lock){this.lock=lock;}
        @Override public void close(){if(!closed){closed=true;if(lock!=null)lock.unlock();}}
    }
}
