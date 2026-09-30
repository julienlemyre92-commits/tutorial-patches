package net.runelite.client.plugins.microbot.piratestreasure;

import com.google.inject.Provides;
import java.util.function.BooleanSupplier;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.Microbot;

@PluginDescriptor(name = "Pirate's Treasure",
    description = "Observed, bounded Microbot route through Pirate's Treasure",
    tags = {"quest", "microbot"}, enabledByDefault = false)
public class PiratesTreasurePlugin extends Plugin {
    @Inject private PiratesTreasureConfig config;
    @Inject private PiratesTreasureScript script;
    @Inject private PluginManager pluginManager;

    @Provides PiratesTreasureConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(PiratesTreasureConfig.class);
    }

    @Override protected void startUp() {
        BooleanSupplier ownsInput = () -> Microbot.getClientThread().invoke((java.util.function.Supplier<Boolean>) () -> {
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
                    || name.equals("net.runelite.client.plugins.microbot.doricsquest.DoricsQuestPlugin")) return false;
            }
            return true;
        });
        script.run(config, ownsInput);
    }

    @Override protected void shutDown() { script.shutdown(); }
}
