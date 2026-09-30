package net.runelite.client.plugins.microbot.witchspotion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("witchspotion")
public interface WitchsPotionConfig extends Config {
    @ConfigItem(keyName = "tickDelay", name = "Tick delay (ms)",
        description = "Time between quest state observations", position = 0)
    default int tickDelay() { return 650; }
}
