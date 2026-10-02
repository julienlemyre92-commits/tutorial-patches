package net.runelite.client.plugins.microbot.misthalinmystery;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("misthalinmystery")
public interface MisthalinMysteryConfig extends Config {
    @ConfigItem(keyName = "enableActions", name = "Enable quest actions",
        description = "Start with status-only preflight. Enable after the live quest state is verified.", position = 0)
    default boolean enableActions() { return false; }

    @ConfigItem(keyName = "tickDelay", name = "Tick delay (ms)",
        description = "Time between observed quest states", position = 1)
    default int tickDelay() { return 650; }

    @ConfigItem(keyName = "logoutOnCompletion", name = "Log out after completion",
        description = "Log out only after live QuestState.FINISHED", position = 2)
    default boolean logoutOnCompletion() { return true; }
}
