package net.runelite.client.plugins.microbot.sheepshearer;

import com.google.inject.Provides;
import java.util.function.BooleanSupplier;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.Microbot;

@PluginDescriptor(name = "Sheep Shearer", description = "Observed Microbot route through Sheep Shearer",
    tags = {"quest", "microbot"}, enabledByDefault = false)
public class SheepShearerPlugin extends Plugin {
    @Inject private SheepShearerConfig config;
    @Inject private SheepShearerScript script;
    @Inject private PluginManager pluginManager;

    @Provides SheepShearerConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(SheepShearerConfig.class);
    }

    @Override protected void startUp() {
        BooleanSupplier ownsInput = () -> Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>) () -> {
            for (Plugin active : pluginManager.getActivePlugins()) {
                if (active == this) continue;
                String name = active.getClass().getName();
                if (name.equals("net.runelite.client.plugins.microbot.cooksassistant.CooksAssistantPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.tutorialisland.TutorialIslandPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.restlessghost.RestlessGhostPlugin")) return false;
            }
            return true;
        });
        script.run(config, ownsInput);
    }

    @Override protected void shutDown() { script.shutdown(); }
}
