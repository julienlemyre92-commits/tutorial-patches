package net.runelite.client.plugins.microbot.runemysteries;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("runemysteries")
public interface RuneMysteriesConfig extends Config {
    @ConfigItem(keyName = "tickDelay", name = "Tick delay (ms)",
        description = "Time between quest state observations", position = 0)
    default int tickDelay() { return 650; }
}
