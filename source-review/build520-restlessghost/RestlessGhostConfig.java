package net.runelite.client.plugins.microbot.restlessghost;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("restlessghost")
public interface RestlessGhostConfig extends Config {
    @ConfigItem(
        keyName = "tickDelay",
        name = "Tick delay (ms)",
        description = "Delay between observed quest steps",
        position = 0
    )
    default int tickDelay() {
        return 650;
    }
}
