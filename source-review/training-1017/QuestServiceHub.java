package net.runelite.client.plugins.microbot.questcommon.services;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Parent-classloader API shared by quest and independent Microbot plugins. */
public final class QuestServiceHub {
    public enum Kind { DEATH_RECOVERY, BANKING, TRAINING, MONEY_MAKING, GE_TRADE, FOOD_RESTOCK, NAVIGATION }
    public enum Outcome { COMPLETE, UNAVAILABLE, HOLD }
    public interface Request {
        Kind kind();
        String accountKey();
        long pid();
        String purpose();
    }
    public interface QuestOwner {
        /** Cancel/join route and stop quest clicks before reporting quiescent. */
        void requestYield();
        boolean isQuiescent();
        /** A held job keeps the quest paused. */
        void serviceFinished(Kind kind, Outcome outcome, String proof);
    }
    public interface ServicePlugin {
        Kind kind();
        /** Parent service receives a child result without waking the quest. */
        default void childFinished(Kind child,Outcome outcome,String proof) { }
    }
    public static final class ReloadPendingException extends IllegalStateException {
        public ReloadPendingException(){super("Shared providers are reloading");}
    }
    public static final class Lease {
        public final Request request;
        public final QuestOwner owner;
        public final ServicePlugin provider;
        public final Lease parent;
        private boolean notified;
        private boolean callbackPending;
        private String cancellationReason;
        private String terminalProof="";
        private Lease(Request request, QuestOwner owner, ServicePlugin provider,Lease parent) {
            this.request=request; this.owner=owner; this.provider=provider;this.parent=parent;
        }
    }
    private static final Map<Kind,ServicePlugin> PROVIDERS=new EnumMap<>(Kind.class);
    private static final String REGISTRY_ID=UUID.randomUUID().toString();
    private static Lease active;
    private static String reloadFence;
    private static int callbacksInFlight;
    private static long providerGeneration;
    private QuestServiceHub() { }

    /** Compare this marker from the host, each provider and the quest after load. */
    public static String registryId() { return REGISTRY_ID; }
    public static String runtimeBuildMarker() { return QuestServicesBuild.ID; }
    public static synchronized boolean isRegistered(Kind kind){return PROVIDERS.containsKey(kind);}
    /** Diagnostics only; a marker or registered provider is not completion proof. */
    public static synchronized Map<String,String> snapshot() {
        int depth=0;
        for(Lease lease=active;lease!=null;lease=lease.parent)depth++;
        return Map.ofEntries(
            Map.entry("registryId",REGISTRY_ID),
            Map.entry("build",QuestServicesBuild.ID),
            Map.entry("pid",Long.toString(ProcessHandle.current().pid())),
            Map.entry("classLoader",String.valueOf(QuestServiceHub.class.getClassLoader())),
            Map.entry("providers",PROVIDERS.keySet().toString()),
            Map.entry("providerGeneration",Long.toString(providerGeneration)),
            Map.entry("active",active==null?"NONE":active.request.kind().name()),
            Map.entry("reloadFenced",Boolean.toString(reloadFence!=null)),
            Map.entry("callbacksInFlight",Integer.toString(callbacksInFlight)),
            Map.entry("held",Boolean.toString(active!=null && active.notified)),
            Map.entry("depth",Integer.toString(depth)),
            Map.entry("heldReason",active!=null && active.notified?active.terminalProof:""));
    }

    public static synchronized boolean register(ServicePlugin provider) {
        Objects.requireNonNull(provider);
        return reloadFence==null
            && PROVIDERS.putIfAbsent(Objects.requireNonNull(provider.kind()),provider)==null;
    }
    public static synchronized boolean unregister(ServicePlugin provider) {
        if (provider==null || reloadFence!=null) return false;
        for (Lease lease=active;lease!=null;lease=lease.parent)
            if (lease.provider==provider) return false;
        return PROVIDERS.remove(provider.kind(),provider);
    }
    /** Stop new root requests only after every nested lease has finished and released. */
    public static synchronized String beginReload() {
        if(active!=null || reloadFence!=null || callbacksInFlight!=0)return null;
        reloadFence=UUID.randomUUID().toString();
        return reloadFence;
    }
    public static synchronized boolean endReload(String token) {
        if(token==null || !token.equals(reloadFence))return false;
        reloadFence=null;
        return true;
    }
    /** Identity snapshot for a fenced, all-provider replacement; never expose it unfenced. */
    public static synchronized Map<Kind,ServicePlugin> providersForReload(String token) {
        if(token==null || !token.equals(reloadFence) || active!=null
            || callbacksInFlight!=0)return Map.of();
        return Map.copyOf(PROVIDERS);
    }
    /** First child-bundle install: add the five implementation providers as one fenced set. */
    public static synchronized boolean installProviders(String token,Map<Kind,ServicePlugin> additions) {
        if(token==null || !token.equals(reloadFence) || active!=null
            || callbacksInFlight!=0 || additions==null
            || !additions.keySet().equals(java.util.EnumSet.of(Kind.NAVIGATION,
                Kind.MONEY_MAKING,Kind.FOOD_RESTOCK,Kind.BANKING,Kind.TRAINING)))return false;
        for(var entry:additions.entrySet())
            if(entry.getValue()==null || entry.getValue().kind()!=entry.getKey()
                || PROVIDERS.containsKey(entry.getKey()))return false;
        if(new java.util.HashSet<>(additions.values()).size()!=additions.size())return false;
        PROVIDERS.putAll(additions);providerGeneration++;
        return true;
    }
    /** One all-provider registry commit while the quest is paused and every worker is drained. */
    public static synchronized boolean replaceProviders(String token,Map<Kind,ServicePlugin> replacements) {
        if(token==null || !token.equals(reloadFence) || active!=null
            || callbacksInFlight!=0 || replacements==null
            || !replacements.keySet().equals(PROVIDERS.keySet()))return false;
        for(var entry:replacements.entrySet())
            if(entry.getValue()==null || entry.getValue().kind()!=entry.getKey())return false;
        if(new java.util.HashSet<>(replacements.values()).size()!=replacements.size())return false;
        PROVIDERS.clear();PROVIDERS.putAll(replacements);
        providerGeneration++;
        return true;
    }
    /** Null means unavailable or another service already owns game input. */
    public static synchronized Lease submit(Request request,QuestOwner owner) {
        Objects.requireNonNull(request);Objects.requireNonNull(owner);
        if(reloadFence!=null)throw new ReloadPendingException();
        if (request.kind()==null || request.pid()!=ProcessHandle.current().pid()
            || request.accountKey()==null || !request.accountKey().matches("[a-f0-9]{64}")
            || request.purpose()==null || request.purpose().isBlank())
            throw new IllegalArgumentException("Invalid account-bound service request");
        ServicePlugin provider=PROVIDERS.get(request.kind());
        if (provider==null || active!=null) return null;
        Lease lease=new Lease(request,owner,provider,null);
        active=lease;
        try { owner.requestYield(); return lease; }
        catch (RuntimeException failure) { active=null; throw failure; }
    }
    /** A service may call another service while retaining the quest's single input lease. */
    public static synchronized Lease delegate(Lease parent,ServicePlugin caller,Request child) {
        Objects.requireNonNull(child);
        if (reloadFence!=null || parent==null || active!=parent || parent.provider!=caller || parent.notified || parent.callbackPending
            || parent.cancellationReason!=null
            || child.kind()==null || child.pid()!=parent.request.pid()
            || !parent.request.accountKey().equals(child.accountKey())
            || child.purpose()==null || child.purpose().isBlank()) return null;
        ServicePlugin provider=PROVIDERS.get(child.kind());
        if (provider==null || provider==caller) return null;
        if (!parent.owner.isQuiescent()) return null;
        for (Lease ancestor=parent;ancestor!=null;ancestor=ancestor.parent)
            if (ancestor.provider==provider) return null;
        Lease lease=new Lease(child,parent.owner,provider,parent);
        active=lease;
        return lease;
    }
    public static synchronized boolean mustYield() { return active!=null; }
    public static synchronized boolean reloadPending() { return reloadFence!=null; }
    public static synchronized Lease activeFor(ServicePlugin provider) {
        return active!=null && !active.notified && !active.callbackPending && active.provider==provider
            && (active.cancellationReason!=null || active.owner.isQuiescent())
            ? active : null;
    }
    public static synchronized boolean owns(Lease lease,ServicePlugin provider,
                                             String observedAccount) {
        return lease!=null && active==lease && !lease.notified && !lease.callbackPending
            && lease.cancellationReason==null && lease.provider==provider
            && lease.owner.isQuiescent() && lease.request.pid()==ProcessHandle.current().pid()
            && lease.request.accountKey().equals(observedAccount);
    }
    /** Revokes input throughout a nested request; providers must drain and retain unresolved proof. */
    public static synchronized void cancel(QuestOwner owner,String reason){
        if(reason==null || reason.isBlank())throw new IllegalArgumentException("Cancellation reason required");
        if(active==null || active.owner!=owner)return;
        for(Lease current=active;current!=null;current=current.parent)current.cancellationReason=reason;
    }
    public static synchronized String cancellationReason(Lease lease){
        return lease==null?null:lease.cancellationReason;
    }
    /** Provider must verify service-specific success before reporting COMPLETE. */
    public static boolean finish(Lease lease,ServicePlugin provider,
                                               Outcome outcome,String proof) {
        synchronized(QuestServiceHub.class){
            if (lease==null || active!=lease || lease.provider!=provider || outcome==null
                || proof==null || proof.isBlank() || lease.notified) return false;
            if(lease.cancellationReason!=null && outcome!=Outcome.HOLD)return false;
            lease.notified=true;
            lease.terminalProof=proof;
            if (lease.parent!=null) lease.parent.callbackPending=true;
            if (outcome!=Outcome.HOLD) active=lease.parent;
            callbacksInFlight++;
        }
        // Callbacks may acquire provider/quest locks; never retain the hub monitor.
        try {
            if (lease.parent==null)
                lease.owner.serviceFinished(lease.request.kind(),outcome,proof);
            else {
                try {lease.parent.provider.childFinished(lease.request.kind(),outcome,proof);}
                finally {
                    // A held child retains ownership, so its parent cannot tick to relay the failure.
                    // Notify the quest for diagnostics without unwinding or granting any input.
                    if(outcome==Outcome.HOLD){
                        Lease root=lease;
                        while(root.parent!=null)root=root.parent;
                        lease.owner.serviceFinished(root.request.kind(),Outcome.HOLD,
                            "Nested "+lease.request.kind()+" HOLD: "+proof);
                    }
                }
            }
        } finally {
            synchronized(QuestServiceHub.class) {
                if (lease.parent!=null) lease.parent.callbackPending=false;
                callbacksInFlight--;
            }
        }
        return true;
    }
    /** Owner may release an inspected HOLD; this cannot be automatic. */
    public static synchronized boolean releaseHeld(Lease lease,QuestOwner owner) {
        if (lease==null || active!=lease || lease.owner!=owner || !lease.notified) return false;
        active=lease.parent;return true;
    }
}
