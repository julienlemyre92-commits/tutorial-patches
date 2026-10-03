package net.runelite.client.plugins.microbot.questcommon.hot;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.client.plugins.microbot.questcommon.services.QuestReloadParticipant;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceHub;

/** Parent-loaded, fail-closed integration candidate. No default positive evidence. */
public final class ProviderHotCoordinator implements ProviderHotHost.Gate {
    private static final Set<QuestServiceHub.Kind> FOUR=Set.of(
        QuestServiceHub.Kind.NAVIGATION,QuestServiceHub.Kind.MONEY_MAKING,
        QuestServiceHub.Kind.FOOD_RESTOCK,QuestServiceHub.Kind.BANKING);
    // RuneLite may start enabled facades before the first quest initializes the
    // coordinator. Record their presence without registering a service yet.
    private static final EnumSet<QuestServiceHub.Kind> EARLY_FACADES=
        EnumSet.noneOf(QuestServiceHub.Kind.class);
    private static volatile ProviderHotCoordinator INSTANCE;

    /** Supplied by the installed client/quest adapter. Unknown must return false. */
    public interface Evidence {
        /** Read-only logged-in account hash, or null when unavailable. Never return credentials. */
        String observeSession();
        boolean walkerIdle();
        boolean guardIdle();
        boolean checkpointsClear();
        boolean geOfferClear();
        boolean oldReferencesDrained(ClassLoader oldLoader);
        /** Stop quest input immediately; never release an unresolved lease to simulate success. */
        void failClosed(String reason);
    }
    private final ProviderHotHost host;
    private final Evidence evidence;
    private final Path home;
    private final long pid=ProcessHandle.current().pid();
    private final EnumSet<QuestServiceHub.Kind> facades=EnumSet.noneOf(QuestServiceHub.Kind.class);
    private final EnumMap<QuestServiceHub.Kind,ProviderRuntime> runtimes=
        new EnumMap<>(QuestServiceHub.Kind.class);
    private final EnumMap<QuestServiceHub.Kind,ScheduledExecutorService> workers=
        new EnumMap<>(QuestServiceHub.Kind.class);
    private final List<ScheduledExecutorService> lastStopped=new ArrayList<>();
    private final AtomicInteger ticksInFlight=new AtomicInteger();
    private ScheduledExecutorService watcher;
    private volatile QuestReloadParticipant quest;
    private volatile boolean startupWindow=true,questPaused,failedClosed;
    private volatile String localFailure="";
    private volatile String pinnedAccount;

    public ProviderHotCoordinator(String parentAbi,Evidence evidence,Path home){
        this.host=new ProviderHotHost(parentAbi);
        this.evidence=Objects.requireNonNull(evidence);
        this.home=Objects.requireNonNull(home);
        synchronized(ProviderHotCoordinator.class){
            if(INSTANCE!=null)throw new IllegalStateException("provider coordinator already installed");
            INSTANCE=this;
            for(QuestServiceHub.Kind kind:EARLY_FACADES)addFacade(kind);
            EARLY_FACADES.clear();
        }
    }
    public static ProviderHotCoordinator current(){
        ProviderHotCoordinator c=INSTANCE;
        if(c==null)throw new IllegalStateException("provider coordinator absent");
        return c;
    }
    public static ProviderHotCoordinator existing(){return INSTANCE;}
    public synchronized boolean healthy(){
        return !failedClosed && "APPLIED".equals(host.status().phase())
            && runtimes.keySet().equals(FOUR);
    }
    static synchronized void facadeStarted(QuestServiceHub.Kind kind){
        ProviderHotCoordinator c=INSTANCE;
        if(c==null){
            if(!FOUR.contains(kind) || !EARLY_FACADES.add(kind))
                throw new IllegalStateException("duplicate/invalid early provider facade: "+kind);
            return;
        }
        c.addFacade(kind);
    }
    static synchronized void facadeStopped(QuestServiceHub.Kind kind){
        ProviderHotCoordinator c=INSTANCE;
        if(c==null)EARLY_FACADES.remove(kind);
        else c.removeFacade(kind);
    }
    private synchronized void addFacade(QuestServiceHub.Kind kind){
        if(failedClosed || !FOUR.contains(kind) || !facades.add(kind))
            throw new IllegalStateException("duplicate/invalid provider facade: "+kind);
    }
    private void removeFacade(QuestServiceHub.Kind kind){
        boolean active;
        synchronized(this){facades.remove(kind);active=!runtimes.isEmpty();}
        if(active)failClosed("provider facade disabled: "+kind);
    }
    /** Called after each quest script start or script hot reload, never keep an old script loader. */
    public synchronized void bindQuest(QuestReloadParticipant participant){
        if(quest!=null && quest!=participant)
            throw new IllegalStateException("another quest reload participant remains bound");
        quest=Objects.requireNonNull(participant);startupWindow=false;
    }
    public synchronized void unbindQuest(QuestReloadParticipant participant){
        if(quest==participant)quest=null;
    }
    public void initialInstall(Path bundle,Path manifest,String manifestSha) throws Exception {
        synchronized(this){if(!facades.containsAll(FOUR) || facades.size()!=FOUR.size())
            throw new IllegalStateException("all four dormant facades required");
        }
        try{
            ProviderHotHost.Loaded staged=host.stage(bundle,manifest,manifestSha);
            if(!host.initialInstall(staged,this))
                throw new IllegalStateException("baseline provider install deferred");
        }catch(Exception | Error ex){
            failClosed("baseline install: "+ex);writeStatus();throw ex;
        }
        writeStatus();
        watcher=Executors.newSingleThreadScheduledExecutor(r->{
            Thread t=new Thread(r,"QuestProviderHotWatcher");t.setDaemon(true);return t;
        });
        watcher.scheduleWithFixedDelay(this::watchTick,1,1,TimeUnit.SECONDS);
    }
    private void watchTick(){
        if(failedClosed){writeStatus();return;}
        try{if(pinSession()){writeStatus();return;}}
        catch(Exception | Error ex){localFailure="session observation: "+ex;writeStatus();return;}
        if(failedClosed){writeStatus();return;}
        Path request=home.resolve("request.properties");
        if(!Files.isRegularFile(request)){writeStatus();return;}
        try {
            Properties p=new Properties();
            try(InputStream in=Files.newInputStream(request)){p.load(in);}
            int generation=Integer.parseInt(p.getProperty("generation","0"));
            String artifact=p.getProperty("artifactSha256","");
            String manifestSha=p.getProperty("manifestSha256","");
            if(!sha(artifact) || !sha(manifestSha))throw new IOException("invalid request hashes");
            if(generation<=host.status().generation()){writeStatus();return;}
            Path bundle=home.resolve(artifact+".jar");
            Path manifest=home.resolve(manifestSha+".properties");
            Properties descriptor=new Properties();
            try(InputStream in=Files.newInputStream(manifest)){descriptor.load(in);}
            if(!Integer.toString(generation).equals(descriptor.getProperty("generation"))
                || !artifact.equals(descriptor.getProperty("artifactSha256")))
                throw new IOException("request and provider manifest disagree");
            ProviderHotHost.Loaded next=host.stage(bundle,manifest,manifestSha);
            if(host.apply(next,this))localFailure=""; // DEFERRED is retried.
        }catch(Exception | Error ex){
            localFailure=ex.toString();
            if("FAILED_CLOSED".equals(host.status().phase()))failClosed(localFailure);
        }
        writeStatus();
    }
    private static boolean sha(String s){return s.matches("[a-f0-9]{64}");}
    /** First login is observed on an ordinary tick; never first pinned during a swap proof. */
    private boolean pinSession(){
        String seen=evidence.observeSession();
        if(seen==null)return false;
        if(!sha(seen)){failClosed("invalid logged-in account hash");return false;}
        String pinned=pinnedAccount;
        if(pinned==null){pinnedAccount=seen;return true;}
        if(!pinned.equals(seen))failClosed("account changed after provider install");
        return false;
    }
    private boolean sameSession(){
        String pinned=pinnedAccount;
        if(pinned==null)return false;
        String current=evidence.observeSession();
        return pinned.equals(current);
    }
    private void writeStatus(){
        try {
            Files.createDirectories(home);
            ProviderHotHost.Status s=host.status();
            Properties p=new Properties();
            p.setProperty("timestamp",Long.toString(System.currentTimeMillis()));
            p.setProperty("pid",Long.toString(pid));
            p.setProperty("phase",failedClosed?"FAILED_CLOSED":s.phase());
            p.setProperty("parentAbi",s.parentAbi());
            p.setProperty("artifactSha",s.artifactSha());
            p.setProperty("marker",s.marker());
            p.setProperty("generation",Integer.toString(s.generation()));
            p.setProperty("failure",localFailure.isEmpty()?s.failure():localFailure);
            Path temp=home.resolve("status.properties.tmp");
            try(OutputStream out=Files.newOutputStream(temp)){p.store(out,"provider hot host");}
            try{Files.move(temp,home.resolve("status.properties"),StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex){
                Files.move(temp,home.resolve("status.properties"),StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(Exception ignored){ /* File I/O cannot authorize input; caller observes missing status. */ }
    }
    @Override public synchronized boolean pauseQuest(){
        if(failedClosed || questPaused)return false;
        QuestReloadParticipant q=quest;
        if(q==null){if(!startupWindow)return false;questPaused=true;return true;}
        if(!q.pauseForProviderReload())return false;
        questPaused=true;return true;
    }
    @Override public synchronized void resumeQuest(){
        if(!questPaused)throw new IllegalStateException("quest not paused");
        QuestReloadParticipant q=quest;
        if(q!=null)q.resumeAfterProviderReload();
        questPaused=false;
    }
    @Override public ProviderHotHost.Proof proof(){
        Map<String,String> hub=QuestServiceHub.snapshot();
        boolean noLease="NONE".equals(hub.get("active"))
            && "0".equals(hub.get("depth")) && "false".equals(hub.get("held"));
        boolean noCallbacks="0".equals(hub.get("callbacksInFlight"));
        boolean paused,idle,cold;
        QuestReloadParticipant participant;
        ProviderRuntime navigation;
        synchronized(this){
            paused=questPaused;
            idle=workers.isEmpty() && ticksInFlight.get()==0;
            cold=startupWindow;
            participant=quest;
            navigation=runtimes.get(QuestServiceHub.Kind.NAVIGATION);
        }
        try{
            boolean localNavigationIdle=navigation==null?cold:navigation.idleForReload();
            boolean questIdle=paused && (participant==null?cold:participant.providerReloadIdle());
            boolean accountSame=sameSession();
            boolean walkerIdle=evidence.walkerIdle();
            boolean guardIdle=evidence.guardIdle();
            boolean checkpointsClear=evidence.checkpointsClear();
            boolean geOfferClear=evidence.geOfferClear();
            boolean stable;
            synchronized(this){
                stable=!failedClosed && questPaused==paused && startupWindow==cold
                    && quest==participant && runtimes.get(QuestServiceHub.Kind.NAVIGATION)==navigation
                    && (workers.isEmpty() && ticksInFlight.get()==0)==idle;
            }
            Map<String,String> after=QuestServiceHub.snapshot();
            stable=stable && "NONE".equals(after.get("active"))
                && "0".equals(after.get("depth"))
                && "0".equals(after.get("callbacksInFlight"))
                && "true".equals(after.get("reloadFenced"));
            return new ProviderHotHost.Proof(questIdle,
                stable && pid==ProcessHandle.current().pid(),accountSame,
                noLease,noCallbacks,idle,
                localNavigationIdle && walkerIdle,
                guardIdle,checkpointsClear,geOfferClear);
        }catch(Exception | Error ex){
            localFailure="proof unavailable: "+ex;
            return new ProviderHotHost.Proof(false,false,false,false,false,false,
                false,false,false,false);
        }
    }
    @Override public boolean stopAndJoinWorkers(Duration limit){
        long deadline=System.nanoTime()+limit.toNanos();
        // Quiescent proof already requires walker/guard idle. Do not poison an old
        // runtime that might need to restart if a precommit drain is deferred.
        List<ScheduledExecutorService> stopping;
        synchronized(this){
            stopping=List.copyOf(workers.values());
            lastStopped.clear();lastStopped.addAll(stopping);
        }
        for(ScheduledExecutorService worker:stopping)worker.shutdownNow();
        for(ScheduledExecutorService worker:stopping){
            long remaining=deadline-System.nanoTime();
            if(remaining<=0)return false;
            try{if(!worker.awaitTermination(remaining,TimeUnit.NANOSECONDS))return false;}
            catch(InterruptedException ex){Thread.currentThread().interrupt();return false;}
        }
        synchronized(this){
            if(!new HashSet<>(workers.values()).equals(new HashSet<>(stopping)))return false;
            workers.clear();
            return ticksInFlight.get()==0;
        }
    }
    @Override public synchronized Map<QuestServiceHub.Kind,ProviderRuntime> currentRuntimes(){
        return Map.copyOf(runtimes);
    }
    @Override public synchronized Map<QuestServiceHub.Kind,QuestServiceHub.ServicePlugin> registryWith(
        String token,Map<QuestServiceHub.Kind,ProviderRuntime> replacement){
        Map<QuestServiceHub.Kind,QuestServiceHub.ServicePlugin> old=QuestServiceHub.providersForReload(token);
        if(old.isEmpty() || !replacement.keySet().equals(FOUR))return null;
        EnumMap<QuestServiceHub.Kind,QuestServiceHub.ServicePlugin> all=
            new EnumMap<>(QuestServiceHub.Kind.class);
        all.putAll(old);
        for(var e:replacement.entrySet())all.put(e.getKey(),e.getValue().service());
        return all;
    }
    @Override public boolean restartOldWorkersAndProve(){
        Map<QuestServiceHub.Kind,ProviderRuntime> old;
        synchronized(this){
            if(failedClosed || !lastStopped.stream().allMatch(ExecutorService::isTerminated)
                || !QuestServiceHub.reloadPending() || !questPaused)return false;
            old=Map.copyOf(runtimes);
        }
        if(!proof().afterDrain())return false;
        synchronized(this){lastStopped.clear();}
        return attachAndStart(old);
    }
    @Override public synchronized boolean attachAndStart(
        Map<QuestServiceHub.Kind,ProviderRuntime> replacement){
        if(failedClosed || !QuestServiceHub.reloadPending() || !facades.containsAll(FOUR)
            || !replacement.keySet().equals(FOUR) || !workers.isEmpty())return false;
        EnumMap<QuestServiceHub.Kind,ScheduledExecutorService> started=
            new EnumMap<>(QuestServiceHub.Kind.class);
        try{
            for(var e:replacement.entrySet()){
                QuestServiceHub.Kind kind=e.getKey();ProviderRuntime runtime=e.getValue();
                ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{
                    Thread t=new Thread(r,"QuestProvider-"+kind);t.setDaemon(true);return t;
                });
                started.put(kind,worker);
                worker.scheduleWithFixedDelay(()->tick(runtime),0,600,TimeUnit.MILLISECONDS);
            }
            runtimes.clear();runtimes.putAll(replacement);workers.putAll(started);
            return true;
        }catch(Exception | Error ex){
            for(ScheduledExecutorService worker:started.values())worker.shutdownNow();
            localFailure="attach/start failed: "+ex;
            return false;
        }
    }
    private void tick(ProviderRuntime runtime){
        if(failedClosed || QuestServiceHub.reloadPending())return;
        ticksInFlight.incrementAndGet();
        try{
            if(!failedClosed && !QuestServiceHub.reloadPending())runtime.tick();
        }catch(Throwable ex){
            failClosed("provider worker failure: "+ex);runtime.requestStop();
        }finally{ticksInFlight.decrementAndGet();}
    }
    @Override public synchronized boolean installedProof(String token,String marker,
        ClassLoader loader,long expectedPid){
        if(failedClosed || expectedPid!=pid || !marker.matches("[a-f0-9]{64}")
            || !runtimes.keySet().equals(FOUR) || !facades.containsAll(FOUR)
            || workers.size()!=FOUR.size() || ticksInFlight.get()!=0)return false;
        Map<QuestServiceHub.Kind,QuestServiceHub.ServicePlugin> registry=
            QuestServiceHub.providersForReload(token);
        for(var e:runtimes.entrySet()){
            ProviderRuntime r=e.getValue();
            if(r.getClass().getClassLoader()!=loader || r.service().getClass().getClassLoader()!=loader
                || registry.get(e.getKey())!=r.service())return false;
        }
        return true;
    }
    @Override public boolean oldReferencesDrained(ClassLoader oldLoader){
        if(oldLoader==null)return true;
        synchronized(this){
            if(ticksInFlight.get()!=0 || !lastStopped.stream().allMatch(ExecutorService::isTerminated))
                return false;
            for(ProviderRuntime r:runtimes.values())
                if(r.getClass().getClassLoader()==oldLoader)return false;
        }
        if(!evidence.oldReferencesDrained(oldLoader))return false;
        synchronized(this){lastStopped.clear();}
        return true;
    }
    @Override public void failClosed(String why){
        List<ProviderRuntime> stopping;
        List<ScheduledExecutorService> executors;
        synchronized(this){
            failedClosed=true;localFailure=why;
            stopping=List.copyOf(runtimes.values());
            executors=List.copyOf(workers.values());
        }
        try{evidence.failClosed(why);}catch(Throwable ignored){}
        for(ProviderRuntime r:stopping)try{r.requestStop();}catch(Throwable ignored){}
        for(ScheduledExecutorService worker:executors)worker.shutdownNow();
        writeStatus();
    }
}
