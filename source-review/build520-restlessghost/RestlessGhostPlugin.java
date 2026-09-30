package net.runelite.client.plugins.microbot.restlessghost;

import com.google.inject.Provides;
import java.util.function.BooleanSupplier;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.Microbot;

/** Separate opt-in quest plugin. The old quest bots must be stopped before this script acts. */
@PluginDescriptor(
    name = "The Restless Ghost",
    description = "Observed, bounded Microbot route through The Restless Ghost",
    tags = {"quest", "microbot"},
    enabledByDefault = false
)
public class RestlessGhostPlugin extends Plugin {
    @Inject private RestlessGhostConfig config;
    @Inject private RestlessGhostScript script;
    @Inject private PluginManager pluginManager;

    @Provides
    RestlessGhostConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(RestlessGhostConfig.class);
    }

    @Override
    protected void startUp() {
        // This supplier is rechecked on every automation tick, not only at startup.
        BooleanSupplier ownsInput = () -> Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>) () -> {
            for (Plugin active : pluginManager.getActivePlugins()) {
                if (active == this) continue;
                String name = active.getClass().getName();
                if (name.equals("net.runelite.client.plugins.microbot.cooksassistant.CooksAssistantPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.tutorialisland.TutorialIslandPlugin")) {
                    return false;
                }
            }
            return true;
        });
        script.run(config, ownsInput);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
    }
}
