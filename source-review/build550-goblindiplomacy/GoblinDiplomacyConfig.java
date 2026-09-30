package net.runelite.client.plugins.microbot.goblindiplomacy;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("goblindiplomacy")
public interface GoblinDiplomacyConfig extends Config {
    @ConfigItem(keyName = "tickDelay", name = "Tick delay (ms)",
        description = "Time between quest state observations", position = 0)
    default int tickDelay() { return 650; }
}
