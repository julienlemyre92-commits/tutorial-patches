package net.runelite.client.plugins.microbot.sheepshearer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("sheepshearer")
public interface SheepShearerConfig extends Config {
    @ConfigItem(keyName = "tickDelay", name = "Tick delay (ms)",
        description = "Delay between observed quest steps", position = 0)
    default int tickDelay() { return 650; }
}
