package net.runelite.client.plugins.microbot.romeojuliet;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("romeojuliet")
public interface RomeoJulietConfig extends Config {
    @ConfigItem(keyName = "tickDelay", name = "Tick delay (ms)",
        description = "Time between quest state observations", position = 0)
    default int tickDelay() { return 650; }
}

