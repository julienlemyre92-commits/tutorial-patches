package net.runelite.client.plugins.microbot.impcatcher;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("impcatcher")
public interface ImpCatcherConfig extends Config {
    @ConfigItem(keyName = "tickDelay", name = "Tick delay (ms)",
        description = "Time between observed quest states", position = 0)
    default int tickDelay() { return 650; }
}

