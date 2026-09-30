package net.runelite.client.plugins.microbot.cooksassistant;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.events.WelcomeScreenEvent;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.security.MessageDigest;

@PluginDescriptor(name="Cook's Assistant", description="Cook's Assistant with verified script reload", tags={"quest","microbot"}, enabledByDefault=false)
public class CooksAssistantPlugin extends Plugin {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(CooksAssistantPlugin.class);
    private static final String SCRIPT="net.runelite.client.plugins.microbot.cooksassistant.CooksAssistantScript";
    private final Path home=Paths.get(System.getProperty("user.home"),".runelite","cooks-hot");
    @Inject private CooksAssistantConfig config;
    @Inject private OverlayManager overlayManager;
    @Inject private CooksAssistantDiagOverlay diagOverlay;
    private Script active;
    private URLClassLoader loader;
    private ScheduledExecutorService watcher;
    private volatile boolean enabled;
    private String applied="embedded", rejected="", error="";
    private int build=509, loginIndex=-1, loginAttempts;
    private String loginState="UNKNOWN", loginKey="";
    private long lastLoginAction;
    @Provides CooksAssistantConfig provideConfig(ConfigManager manager) { return manager.getConfig(CooksAssistantConfig.class); }

    @Override protected synchronized void startUp() throws Exception {
        Files.createDirectories(home);
        System.setProperty("alex.cooks.hotHost","true");
        enabled=true;
        overlayManager.add(diagOverlay);
        startScript(new CooksAssistantScript());
        watcher=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"CooksReloadHost");t.setDaemon(true);return t;});
        watcher.scheduleWithFixedDelay(this::tick,0,1,TimeUnit.SECONDS);
        log.info("[CooksHot] HOST_READY version=2 pid={}",ProcessHandle.current().pid());
    }
    private void startScript(Script next) throws Exception {
        next.getClass().getMethod("setDiagOverlay",CooksAssistantDiagOverlay.class).invoke(next,diagOverlay);
        active=next;
        Object started=next.getClass().getMethod("run",CooksAssistantConfig.class).invoke(next,config);
        if (!Boolean.TRUE.equals(started)) throw new IllegalStateException("run rejected");
        build=(Integer)next.getClass().getMethod("runtimeBuild").invoke(next);
    }
    @Override protected synchronized void shutDown() {
        enabled=false;
        if(watcher!=null) watcher.shutdownNow();
        if(active!=null) active.shutdown();
        overlayManager.remove(diagOverlay);
        diagOverlay.clear();
        closeLoader(loader); loader=null;
        try {Files.deleteIfExists(home.resolve("status.properties"));} catch(Exception ignored) {}
    }
    private void tick() {
        if(!enabled) return;
        try { nativeLogin(); } catch(Exception ex) {log.warn("[CooksHot] login observation failed: {}",ex.toString());}
        try { reloadIfRequested(); } catch(Exception ex) {error=ex.toString();log.error("[CooksHot] reload held: {}",error);}
        try { writeStatus(); } catch(Exception ex) {log.warn("[CooksHot] status write failed: {}",ex.toString());}
    }
    private void nativeLogin() throws Exception {
        if(Microbot.getClient()==null) return;
        loginState=Microbot.getClient().getGameState().name();
        loginIndex=Microbot.getClient().getLoginIndex();
        if(active!=null) active.getClass().getMethod("loginTick").invoke(active);
    }
    private synchronized void reloadIfRequested() throws Exception {
        if(!enabled || !Files.exists(home.resolve("request.properties"))) return;
        Properties req=new Properties();
        try(java.io.InputStream in=Files.newInputStream(home.resolve("request.properties"))){req.load(in);}
        String sha=req.getProperty("sha256","");
        if(sha.equals(applied)||sha.equals(rejected)) return;
        rejected=sha; // Never loop a failed build.
        if(!sha.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("invalid hash");
        int wanted=Integer.parseInt(req.getProperty("build"));
        if(wanted<=build) return; // stale manifest after a cold-start update
        Path jar=home.resolve(sha+".jar");
        byte[] bytes=Files.readAllBytes(jar);
        String actual=java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if(!sha.equals(actual)) throw new IllegalArgumentException("artifact hash mismatch");
        // Only this script and its nested classes are replaceable. Shared API,
        // config, overlay, host, and dependencies always stay parent-loaded.
        try(java.util.jar.JarFile archive=new java.util.jar.JarFile(jar.toFile())) {
            for(java.util.jar.JarEntry entry:Collections.list(archive.entries())) {
                if(entry.isDirectory()) continue;
                String n=entry.getName();
                String prefix=SCRIPT.replace('.','/');
                if(!(n.equals(prefix+".class")||(n.startsWith(prefix+"$")&&n.endsWith(".class"))))
                    throw new IllegalArgumentException("non-script entry: "+n);
            }
        }
        URLClassLoader candidate=new URLClassLoader(new URL[]{jar.toUri().toURL()},getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
                if(!name.equals(SCRIPT)&&!name.startsWith(SCRIPT+"$")) return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)) {
                    Class<?> c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;
                }
            }
        };
        Script next=null;
        boolean stopped=false;
        try {
            Class<?> type=candidate.loadClass(SCRIPT);
            type.getMethod("run",CooksAssistantConfig.class);
            type.getMethod("awaitStopped");
            type.getMethod("loginTick");
            type.getMethod("nativeLoginOwned");
            type.getMethod("selectedLoginWorld");
            next=(Script)type.getConstructor().newInstance();
            if((Integer)type.getMethod("runtimeBuild").invoke(next)!=wanted) throw new IllegalArgumentException("build mismatch");
            log.info("[CooksHot] RELOAD_BEGIN from={} to={} pid={}",build,wanted,ProcessHandle.current().pid());
            if(active!=null) {
                active.shutdown();
                stopped=(Boolean)active.getClass().getMethod("awaitStopped").invoke(active);
                if(!stopped) throw new IllegalStateException("old scheduler did not terminate; holding both scripts");
            }
            if(!enabled) throw new IllegalStateException("plugin disabled during reload");
            diagOverlay.clear();
            URLClassLoader old=loader;
            startScript(next);
            loader=candidate;applied=sha;error="";
            closeLoader(old);
            log.info("[CooksHot] RELOAD_APPLIED build={} pid={} sha={} loader={} source={}",build,ProcessHandle.current().pid(),sha,type.getClassLoader(),type.getProtectionDomain().getCodeSource().getLocation());
        } catch(Exception|LinkageError ex) {
            if(next!=null) next.shutdown();
            closeLoader(candidate);
            if(stopped) active=null;
            throw ex;
        }
    }
    private void writeStatus() throws Exception {
        Properties p=new Properties();
        p.setProperty("hostVersion","2");p.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
        p.setProperty("timestamp",Long.toString(System.currentTimeMillis()));p.setProperty("build",Integer.toString(build));
        p.setProperty("sha256",applied);p.setProperty("rejected",rejected);p.setProperty("error",error);
        p.setProperty("gameState",loginState);p.setProperty("loginIndex",Integer.toString(loginIndex));
        p.setProperty("nativeLogin",String.valueOf(active!=null && (Boolean)active.getClass().getMethod("nativeLoginOwned").invoke(active)));
        p.setProperty("selectedWorld",active==null?"0":String.valueOf(active.getClass().getMethod("selectedLoginWorld").invoke(active)));
        p.setProperty("currentWorld",Integer.toString(Microbot.getClient().getWorld()));
        Path temp=home.resolve("status.tmp");
        try(java.io.OutputStream out=Files.newOutputStream(temp)){p.store(out,"Cook script host runtime evidence");}
        try {Files.move(temp,home.resolve("status.properties"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException ex){Files.move(temp,home.resolve("status.properties"),StandardCopyOption.REPLACE_EXISTING);}
    }
    private static void closeLoader(URLClassLoader value) {if(value!=null)try{value.close();}catch(Exception ignored){}}
}
