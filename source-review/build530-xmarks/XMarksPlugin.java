package net.runelite.client.plugins.microbot.xmarks;

import com.google.inject.Provides;
import java.util.function.BooleanSupplier;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.Microbot;

@PluginDescriptor(name = "X Marks the Spot", description = "Observed Microbot route through X Marks the Spot",
    tags = {"quest", "microbot"}, enabledByDefault = false)
public class XMarksPlugin extends Plugin {
    @Inject private XMarksConfig config;
    @Inject private XMarksScript script;
    @Inject private PluginManager pluginManager;

    @Provides XMarksConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(XMarksConfig.class);
    }

    @Override protected void startUp() {
        BooleanSupplier ownsInput = () -> Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>) () -> {
            for (Plugin active : pluginManager.getActivePlugins()) {
                if (active == this) continue;
                String name = active.getClass().getName();
                if (name.equals("net.runelite.client.plugins.microbot.cooksassistant.CooksAssistantPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.tutorialisland.TutorialIslandPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.restlessghost.RestlessGhostPlugin")
                    || name.equals("net.runelite.client.plugins.microbot.sheepshearer.SheepShearerPlugin")) return false;
            }
            return true;
        });
        script.run(config, ownsInput);
    }

    @Override protected void shutDown() { script.shutdown(); }
}
