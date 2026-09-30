package net.runelite.client.plugins.microbot.impcatcher;

import com.google.inject.Provides;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;

@PluginDescriptor(name = "Imp Catcher",
    description = "Observed, bounded Microbot route through Imp Catcher",
    tags = {"quest", "microbot"}, enabledByDefault = false)
public class ImpCatcherPlugin extends Plugin {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImpCatcherPlugin.class);
    private static final String SCRIPT = "net.runelite.client.plugins.microbot.impcatcher.ImpCatcherScript";
    private final Path hotHome = Paths.get(System.getProperty("user.home"), ".runelite", "impcatcher-hot");
    @Inject private ImpCatcherConfig config;
    @Inject private PluginManager pluginManager;
    private Script script;
    private URLClassLoader loader;
    private ScheduledExecutorService watcher;
    private BooleanSupplier ownsInput;
    private volatile boolean enabled;
    private String applied = "embedded", rejected = "", hotError = "";
    private int build = ImpCatcherScript.BUILD_NUMBER;

    @Provides ImpCatcherConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(ImpCatcherConfig.class);
    }

    @Override protected synchronized void startUp() throws Exception {
        Files.createDirectories(hotHome);
        ownsInput = () -> Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>) () -> {
            for (Plugin active : pluginManager.getActivePlugins()) {
                if (active == this) continue;
                String name = active.getClass().getName();
                if (name.equals("net.runelite.client.plugins.microbot.cooksassistant.CooksAssistantPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.tutorialisland.TutorialIslandPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.tutorialisland2.TutorialIsland2Plugin")
                    || name.equals("net.runelite.client.plugins.microbot.restlessghost.RestlessGhostPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.sheepshearer.SheepShearerPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.xmarks.XMarksPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.runemysteries.RuneMysteriesPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.romeojuliet.RomeoJulietPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.goblindiplomacy.GoblinDiplomacyPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.witchspotion.WitchsPotionPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.doricsquest.DoricsQuestPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.piratestreasure.PiratesTreasurePlugin")) return false;
            }
            return true;
        });
        enabled = true;
        startScript(new ImpCatcherScript());
        watcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ImpCatcherReloadHost"); t.setDaemon(true); return t;
        });
        watcher.scheduleWithFixedDelay(this::hotTick, 0, 1, TimeUnit.SECONDS);
        log.info("[ImpCatcherHot] HOST_READY version=1 pid={}", ProcessHandle.current().pid());
    }

    private void startScript(Script next) throws Exception {
        Object started = next.getClass().getMethod("run", ImpCatcherConfig.class, BooleanSupplier.class)
            .invoke(next, config, ownsInput);
        if (!Boolean.TRUE.equals(started)) throw new IllegalStateException("script run rejected");
        script = next;
        build = (Integer) next.getClass().getMethod("runtimeBuild").invoke(next);
    }

    @Override protected synchronized void shutDown() {
        enabled = false;
        if (watcher != null) watcher.shutdownNow();
        if (script != null) script.shutdown();
        closeLoader(loader); loader = null;
        try { Files.deleteIfExists(hotHome.resolve("status.properties")); }
        catch (Exception ignored) { }
    }

    private void hotTick() {
        if (!enabled) return;
        try { reloadIfRequested(); }
        catch (Exception | LinkageError ex) {
            hotError = ex.toString();
            log.error("[ImpCatcherHot] RELOAD_HELD {}", hotError, ex);
        }
        try { writeHotStatus(); }
        catch (Exception ex) { log.warn("[ImpCatcherHot] status write: {}", ex.toString()); }
    }

    private synchronized void reloadIfRequested() throws Exception {
        Path request = hotHome.resolve("request.properties");
        if (!enabled || !Files.isRegularFile(request)) return;
        Properties req = new Properties();
        try (InputStream in = Files.newInputStream(request)) { req.load(in); }
        String sha = req.getProperty("sha256", "");
        if (sha.equals(applied) || sha.equals(rejected)) return;
        rejected = sha;
        if (!sha.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("invalid SHA-256");
        int wanted = Integer.parseInt(req.getProperty("build", "0"));
        if (wanted <= build) return;
        Path jar = hotHome.resolve(sha + ".jar");
        byte[] bytes = Files.readAllBytes(jar);
        String actual = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!sha.equals(actual)) throw new IllegalArgumentException("artifact hash mismatch");
        String prefix = SCRIPT.replace('.', '/');
        try (JarFile archive = new JarFile(jar.toFile())) {
            for (JarEntry entry : Collections.list(archive.entries())) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (!(name.equals(prefix + ".class") || name.startsWith(prefix + "$") && name.endsWith(".class")))
                    throw new IllegalArgumentException("non-script entry: " + name);
            }
        }
        URLClassLoader candidate = new URLClassLoader(new URL[]{jar.toUri().toURL()}, getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(SCRIPT) && !name.startsWith(SCRIPT + "$")) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) type = findClass(name);
                    if (resolve) resolveClass(type);
                    return type;
                }
            }
        };
        Script next = null;
        boolean stopped = false;
        Script previous = script;
        URLClassLoader oldLoader = loader;
        try {
            Class<?> type = candidate.loadClass(SCRIPT);
            type.getMethod("run", ImpCatcherConfig.class, BooleanSupplier.class);
            type.getMethod("awaitStopped");
            type.getMethod("runtimeBuild");
            next = (Script) type.getConstructor().newInstance();
            if ((Integer) type.getMethod("runtimeBuild").invoke(next) != wanted)
                throw new IllegalArgumentException("build mismatch");
            log.info("[ImpCatcherHot] RELOAD_BEGIN from={} to={} pid={}", build, wanted, ProcessHandle.current().pid());
            previous.shutdown();
            stopped = (Boolean) previous.getClass().getMethod("awaitStopped").invoke(previous);
            if (!stopped) throw new IllegalStateException("old scheduler did not terminate");
            if (!enabled) throw new IllegalStateException("plugin disabled during reload");
            startScript(next);
            loader = candidate; applied = sha; hotError = "";
            closeLoader(oldLoader);
            log.info("[ImpCatcherHot] RELOAD_APPLIED build={} pid={} sha={}", build, ProcessHandle.current().pid(), sha);
        } catch (Exception | LinkageError ex) {
            if (next != null) next.shutdown();
            closeLoader(candidate);
            if (stopped && enabled) {
                try { startScript((Script) previous.getClass().getConstructor().newInstance()); loader = oldLoader; }
                catch (Exception rollback) { ex.addSuppressed(rollback); script = null; }
            }
            throw ex;
        }
    }

    private void writeHotStatus() throws Exception {
        Properties p = new Properties();
        p.setProperty("hostVersion", "1");
        p.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        p.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
        p.setProperty("build", Integer.toString(build));
        p.setProperty("sha256", applied);
        p.setProperty("rejected", rejected);
        p.setProperty("error", hotError);
        Path temp = hotHome.resolve("status.tmp");
        try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "Imp Catcher script hot host"); }
        Files.move(temp, hotHome.resolve("status.properties"), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void closeLoader(URLClassLoader value) {
        if (value != null) try { value.close(); } catch (Exception ignored) { }
    }
}

