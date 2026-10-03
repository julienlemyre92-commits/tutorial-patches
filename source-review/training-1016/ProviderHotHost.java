package net.runelite.client.plugins.microbot.questcommon.hot;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.time.Duration;
import java.util.*;
import java.util.jar.*;
import net.runelite.client.plugins.microbot.questcommon.services.QuestServiceHub;

/** Parent-loader candidate. All game state/worker proofs are supplied by the integration gate. */
public final class ProviderHotHost {
    private static final String FACTORY=
        "net.runelite.client.plugins.microbot.questcommon.hot.impl.DefaultProviderBundleFactory";
    private static final Set<QuestServiceHub.Kind> FIVE=Set.of(
        QuestServiceHub.Kind.NAVIGATION,QuestServiceHub.Kind.MONEY_MAKING,
        QuestServiceHub.Kind.FOOD_RESTOCK,QuestServiceHub.Kind.BANKING,
        QuestServiceHub.Kind.TRAINING);
    private static final String ROOT="net/runelite/client/plugins/microbot/questcommon/";
    private final String parentAbi;
    private final long pid=ProcessHandle.current().pid();
    private volatile Loaded current;
    private volatile String phase="BASELINE",failure="";

    /** The gate is implemented by the stable parent facades and quest participant. */
    public interface Gate {
        boolean pauseQuest();
        /** Clear the local pause while the Hub fence still blocks every quest tick/input. */
        void resumeQuest();
        Proof proof();
        /** Join scheduled ticks off EDT/client thread; no forced release of held work. */
        boolean stopAndJoinWorkers(Duration limit);
        Map<QuestServiceHub.Kind,ProviderRuntime> currentRuntimes();
        /** Preserve unrelated registered services such as death recovery. */
        Map<QuestServiceHub.Kind,QuestServiceHub.ServicePlugin> registryWith(
            String reloadToken,Map<QuestServiceHub.Kind,ProviderRuntime> replacements);
        /** Required if a pre-commit drain fails after any old worker was touched. */
        boolean restartOldWorkersAndProve();
        boolean attachAndStart(Map<QuestServiceHub.Kind,ProviderRuntime> replacements);
        boolean installedProof(String reloadToken,String marker,ClassLoader loader,long pid);
        boolean oldReferencesDrained(ClassLoader oldLoader);
        void failClosed(String why);
    }
    public record Proof(boolean scriptPaused,boolean samePid,boolean sameAccount,
        boolean noHeldOrNestedLease,boolean noCallbacks,boolean workersIdle,
        boolean walkerIdle,boolean guardIdle,boolean checkpointsClear,
        boolean geOfferClear) {
        boolean beforeDrain(){return scriptPaused && samePid && sameAccount
            && noHeldOrNestedLease && noCallbacks && walkerIdle && guardIdle
            && checkpointsClear && geOfferClear;}
        boolean afterDrain(){return beforeDrain() && workersIdle;}
        /** Cold registration can restore persisted checkpoints after a new login. */
        boolean beforeInitialInstall(){return scriptPaused && samePid && noHeldOrNestedLease
            && noCallbacks && workersIdle && walkerIdle && guardIdle;}
    }
    public record Status(String phase,String failure,long pid,String parentAbi,
        String marker,String artifactSha,int generation,String loader) { }
    public ProviderHotHost(String parentAbi) {
        if(!hash(parentAbi))throw new IllegalArgumentException("compiled parent ABI SHA required");
        this.parentAbi=parentAbi;
    }
    public Status status(){
        Loaded c=current;
        return new Status(phase,failure,pid,parentAbi,c==null?"":c.marker,
            c==null?"":c.artifactSha,c==null?0:c.generation,
            c==null?"":Integer.toHexString(System.identityHashCode(c.loader)));
    }

    /** Hash/ABI verification and class loading only; never starts services. */
    public Loaded stage(Path jarPath,Path manifestPath,String expectedManifestSha) throws Exception {
        if(!hash(expectedManifestSha)
            || !expectedManifestSha.equals(sha(Files.readAllBytes(manifestPath))))
            throw new IOException("provider manifest SHA mismatch");
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(manifestPath)){p.load(in);}
        if(!"1".equals(p.getProperty("format")))throw new IOException("manifest format");
        String abi=p.getProperty("parentAbiSha256","");
        String artifactSha=p.getProperty("artifactSha256","");
        String marker=p.getProperty("implementationMarker","");
        int generation=Integer.parseInt(p.getProperty("generation","0"));
        if(!parentAbi.equals(abi) || !hash(artifactSha) || !hash(marker)
            || generation<=0 || current!=null && generation<=current.generation)
            throw new IOException("parent ABI/hash/generation mismatch");
        if(!artifactSha.equals(sha(Files.readAllBytes(jarPath))))
            throw new IOException("provider artifact SHA mismatch");
        Map<String,String> classes=new TreeMap<>();
        for(String key:p.stringPropertyNames())if(key.startsWith("class.")) {
            String resource=key.substring(6);
            if(!allowed(resource) || !hash(p.getProperty(key)))
                throw new IOException("non-provider class or invalid digest: "+resource);
            classes.put(resource,p.getProperty(key));
        }
        if(classes.isEmpty() || !classes.containsKey(FACTORY.replace('.','/')+".class")
            || !classes.containsKey(ROOT+"hot/impl/ProviderImplBuild.class"))
            throw new IOException("factory/marker missing");
        try(JarFile jar=new JarFile(jarPath.toFile(),true)) {
            Set<String> found=new HashSet<>();
            Enumeration<JarEntry> entries=jar.entries();
            while(entries.hasMoreElements()){
                JarEntry entry=entries.nextElement();
                if(entry.isDirectory())continue;
                String name=entry.getName();
                if(!classes.containsKey(name) || !found.add(name))
                    throw new IOException("extra or duplicate entry: "+name);
                byte[] bytes;
                try(InputStream in=jar.getInputStream(entry)){bytes=in.readAllBytes();}
                if(!classes.get(name).equals(sha(bytes)))
                    throw new IOException("class SHA mismatch: "+name);
            }
            if(!found.equals(classes.keySet()))throw new IOException("manifest class set mismatch");
        }
        ChildLoader loader=new ChildLoader(jarPath.toUri().toURL(),
            ProviderHotHost.class.getClassLoader(),classes.keySet());
        try {
            Class<?> type=Class.forName(FACTORY,true,loader);
            if(type.getClassLoader()!=loader || !ProviderBundleFactory.class.isAssignableFrom(type))
                throw new LinkageError("factory resolved outside child or wrong parent interface");
            ProviderBundleFactory factory=(ProviderBundleFactory)type.getConstructor().newInstance();
            if(!marker.equals(factory.implementationMarker())
                || !parentAbi.equals(factory.parentAbiSha256()))
                throw new LinkageError("loaded implementation marker/ABI mismatch");
            verifyNavigationBoundary(loader);
            String preflight=factory.preflightFailure();
            if(preflight!=null)throw new IllegalStateException(preflight);
            return new Loaded(loader,factory,artifactSha,marker,generation);
        }catch(Exception | Error ex){loader.close();throw ex;}
    }

    /** Cold host rollout: first child set, before any quest/provider gameplay begins. */
    public synchronized boolean initialInstall(Loaded first,Gate gate) throws Exception {
        Objects.requireNonNull(first);Objects.requireNonNull(gate);
        if(current!=null || first.used)throw new IllegalStateException("not first child install");
        first.used=true;
        String token=QuestServiceHub.beginReload();
        if(token==null){phase="DEFERRED";failure="active/held lease or callback";first.close();return false;}
        boolean committed=false,paused=false;
        Map<QuestServiceHub.Kind,ProviderRuntime> fresh=null;
        try {
            paused=gate.pauseQuest();
            if(!paused || !safeInitial(gate.proof())
                || gate.currentRuntimes()==null || !gate.currentRuntimes().isEmpty())
                throw new Deferred("first install requires empty, quiescent four-provider set");
            fresh=first.factory.createAll();
            if(!valid(fresh,first.loader))throw new IllegalStateException("initial child set invalid");
            if(!QuestServiceHub.installProviders(token,services(fresh)))
                throw new IllegalStateException("atomic first registration rejected");
            committed=true;
            if(!gate.attachAndStart(fresh)
                || !gate.installedProof(token,first.marker,first.loader,pid))
                throw new IllegalStateException("initial same-PID four-provider proof failed");
            current=first;phase="APPLIED";failure="";
            gate.resumeQuest();
            if(!QuestServiceHub.endReload(token))
                throw new IllegalStateException("initial fence release failed");
            return true;
        } catch(Exception | Error ex) {
            failure=ex.toString();
            if(!committed){
                if(fresh!=null)for(ProviderRuntime runtime:fresh.values())
                    try{runtime.close();}catch(Exception ignored){}
                first.close();
                phase="DEFERRED";
                try {if(paused)gate.resumeQuest();}
                catch(RuntimeException | Error resumeFailure){
                    phase="FAILED_CLOSED";ex.addSuppressed(resumeFailure);
                    gate.failClosed("initial quest resume failed under fence: "+failure);
                    throw ex;
                }
                if(!QuestServiceHub.endReload(token)){
                    phase="FAILED_CLOSED";gate.failClosed("initial fence release failed: "+failure);
                }
                if(ex instanceof Deferred)return false;
            } else {
                phase="FAILED_CLOSED";
                gate.failClosed(failure); // Keep fence and do not run a partial first install.
            }
            throw ex;
        }
    }

    /** Failure after old locks close remains fenced; never resumes a partial provider set. */
    public synchronized boolean apply(Loaded next,Gate gate) throws Exception {
        Objects.requireNonNull(next);Objects.requireNonNull(gate);
        if(next.used || next==current || next.loader==null || next.generation<=
            (current==null?0:current.generation))throw new IllegalArgumentException("stale candidate");
        next.used=true;
        String token=QuestServiceHub.beginReload();
        if(token==null){phase="DEFERRED";failure="active/held lease or callback";next.close();return false;}
        boolean oldClosed=false,committed=false,drainAttempted=false,questPaused=false;
        try {
            phase="FROZEN";
            questPaused=gate.pauseQuest();
            if(!questPaused || !safe(gate.proof(),false))
                throw new Deferred("quest/input/checkpoint/GE not quiescent");
            drainAttempted=true;
            if(!gate.stopAndJoinWorkers(Duration.ofSeconds(3)) || !safe(gate.proof(),true))
                throw new Deferred("worker/walker/guard did not drain");
            Map<QuestServiceHub.Kind,ProviderRuntime> old=gate.currentRuntimes();
            if(old==null || !old.keySet().equals(FIVE))
                throw new Deferred("old four-provider set unavailable");
            // Constructors acquire checkpoint locks. Release old only after the freeze proof.
            oldClosed=true; // A partial close is not safely restartable.
            for(ProviderRuntime r:old.values())r.close();
            Map<QuestServiceHub.Kind,ProviderRuntime> fresh=next.factory.createAll();
            if(!valid(fresh,next.loader))throw new IllegalStateException("candidate provider set invalid");
            Map<QuestServiceHub.Kind,QuestServiceHub.ServicePlugin> all=gate.registryWith(token,fresh);
            if(all==null || !QuestServiceHub.replaceProviders(token,all))
                throw new IllegalStateException("atomic registry replacement rejected");
            committed=true;
            if(!gate.attachAndStart(fresh)
                || !gate.installedProof(token,next.marker,next.loader,pid)
                || !gate.oldReferencesDrained(current==null?null:current.loader))
                throw new IllegalStateException("same-PID provider/old-loader proof failed");
            Loaded previous=current;
            current=next;phase="APPLIED";failure="";
            if(previous!=null)previous.close();
            gate.resumeQuest();
            if(!QuestServiceHub.endReload(token))throw new IllegalStateException("reload fence release failed");
            return true;
        } catch(Exception | Error ex) {
            failure=ex.toString();
            if(!oldClosed && !committed) {
                next.close();
                if(drainAttempted && !gate.restartOldWorkersAndProve()) {
                    phase="FAILED_CLOSED";
                    gate.failClosed("old workers not restored after failed drain: "+failure);
                    throw ex; // Retain fence and quest pause.
                }
                phase="DEFERRED";
                try {if(questPaused)gate.resumeQuest();}
                catch(RuntimeException | Error resumeFailure){
                    phase="FAILED_CLOSED";ex.addSuppressed(resumeFailure);
                    gate.failClosed("quest resume failed under fence: "+failure);
                    throw ex;
                }
                if(!QuestServiceHub.endReload(token)) {
                    phase="FAILED_CLOSED";
                    gate.failClosed("reload fence release failed: "+failure);
                    throw ex;
                }
                if(ex instanceof Deferred)return false;
            } else {
                phase="FAILED_CLOSED";
                gate.failClosed(failure); // Retain fence and pause; explicit recovery required.
            }
            throw ex;
        }
    }
    private static boolean safe(Proof p,boolean drained){
        return p!=null && (drained?p.afterDrain():p.beforeDrain())
            && !QuestServiceHub.mustYield() && QuestServiceHub.reloadPending();
    }
    private static boolean safeInitial(Proof p){
        return p!=null && p.beforeInitialInstall() && !QuestServiceHub.mustYield()
            && QuestServiceHub.reloadPending();
    }
    private static void verifyNavigationBoundary(ClassLoader child) throws Exception {
        String base="net.runelite.client.plugins.microbot.questcommon.navigation.";
        ClassLoader parent=ProviderHotHost.class.getClassLoader();
        Class<?> gate=Class.forName(base+"NavigationApprovalPublisher$Gate",false,parent);
        Class<?> childGate=Class.forName(base+"NavigationApprovalPublisher$Gate",false,child);
        Class<?> runtime=Class.forName(base+"NavigationMicrobotDriver$GuardRuntime",false,child);
        Class<?> installed=Class.forName(base+"InstalledNavigationGuard",false,child);
        Class<?> publisher=Class.forName(base+"NavigationApprovalPublisher",false,child);
        Class<?> driver=Class.forName(base+"NavigationMicrobotDriver",false,child);
        if(childGate!=gate || runtime.getClassLoader()!=parent
            || installed.getClassLoader()!=parent || !gate.isAssignableFrom(runtime)
            || !runtime.isAssignableFrom(installed)
            || publisher.getClassLoader()!=child || driver.getClassLoader()!=child)
            throw new LinkageError("navigation parent/child gate identity mismatch");
        publisher.getConstructor(gate); // exact JVM constructor linkage, without game input
    }
    private static boolean valid(Map<QuestServiceHub.Kind,ProviderRuntime> set,ClassLoader loader){
        if(set==null || !set.keySet().equals(FIVE))return false;
        Set<ProviderRuntime> unique=Collections.newSetFromMap(new IdentityHashMap<>());
        for(var entry:set.entrySet()){
            ProviderRuntime runtime=entry.getValue();
            if(runtime==null || !unique.add(runtime) || runtime.kind()!=entry.getKey()
                || runtime.service()==null || runtime.service().kind()!=entry.getKey()
                || runtime.getClass().getClassLoader()!=loader
                || runtime.service().getClass().getClassLoader()!=loader)return false;
        }
        return true;
    }
    private static Map<QuestServiceHub.Kind,QuestServiceHub.ServicePlugin> services(
        Map<QuestServiceHub.Kind,ProviderRuntime> runtimes){
        EnumMap<QuestServiceHub.Kind,QuestServiceHub.ServicePlugin> services=
            new EnumMap<>(QuestServiceHub.Kind.class);
        for(var entry:runtimes.entrySet())services.put(entry.getKey(),entry.getValue().service());
        return services;
    }
    private static boolean allowed(String path){
        if(!path.startsWith(ROOT) || !path.endsWith(".class") || path.contains(".."))return false;
        String n=path.substring(ROOT.length());
        if(n.startsWith("hot/impl/"))return true;
        if(n.startsWith("preparation/"))return !n.startsWith("preparation/PreparationGoal")
            && !n.startsWith("preparation/QuestPreparationPlugin");
        if(n.startsWith("acquisition/"))return !n.startsWith("acquisition/FoodAcquisitionGoal")
            && !n.startsWith("acquisition/QuestFoodRestockPlugin");
        if(n.startsWith("navigation/"))return !n.startsWith("navigation/NavigationGoal")
            && !n.startsWith("navigation/QuestNavigationPlugin")
            && !n.startsWith("navigation/guard/")
            && !n.equals("navigation/InstalledNavigationGuard.class")
            && !n.equals("navigation/NavigationApprovalPublisher$Gate.class")
            && !n.equals("navigation/NavigationMicrobotDriver$GuardRuntime.class");
        if(n.startsWith("funding/"))return !n.startsWith("funding/QuestFundingPlugin");
        if(n.startsWith("training/"))return !n.startsWith("training/CombatTrainingGoal");
        return n.startsWith("ge/") || n.startsWith("moneymaker/");
    }
    private static boolean hash(String s){return s!=null && s.matches("[a-f0-9]{64}");}
    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static final class Deferred extends Exception {Deferred(String s){super(s);}}
    public static final class Loaded implements AutoCloseable {
        private final ChildLoader loader;
        private final ProviderBundleFactory factory;
        private final String artifactSha,marker;
        private final int generation;
        private boolean used;
        private Loaded(ChildLoader loader,ProviderBundleFactory factory,String sha,String marker,int gen){
            this.loader=loader;this.factory=factory;this.artifactSha=sha;this.marker=marker;this.generation=gen;
        }
        @Override public void close() throws IOException {loader.close();}
    }
    private static final class ChildLoader extends URLClassLoader {
        private final Set<String> own;
        ChildLoader(URL jar,ClassLoader parent,Set<String> resources){
            super(new URL[]{jar},parent);
            HashSet<String> names=new HashSet<>();
            for(String r:resources)names.add(r.substring(0,r.length()-6).replace('/','.'));
            own=Set.copyOf(names);
        }
        @Override protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
            if(!own.contains(name))return super.loadClass(name,resolve);
            synchronized(getClassLoadingLock(name)){
                Class<?> type=findLoadedClass(name);
                if(type==null)type=findClass(name);
                if(resolve)resolveClass(type);
                return type;
            }
        }
    }
}
