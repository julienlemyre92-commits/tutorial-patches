package net.runelite.client.plugins.microbot.corsaircurse;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("corsaircurse")
public interface CorsairCurseConfig extends Config {
    @ConfigItem(keyName = "preflightOnly", name = "Login and inspect only",
        description = "Do not walk, bank, or perform quest actions after login")
    default boolean preflightOnly() { return true; }
    @ConfigItem(keyName = "tickDelay", name = "Tick delay (ms)", description = "Observation interval")
    default int tickDelay() { return 650; }
    @ConfigItem(keyName = "allowActions", name = "Arm game actions", description = "Enable only after reviewing the current preflight status")
    default boolean allowActions() { return false; }
    @ConfigItem(keyName = "approvedPid", name = "Approved client PID", description = "Must match the live status PID")
    default int approvedPid() { return 0; }
    @ConfigItem(keyName = "approvedBuild", name = "Approved build", description = "Must match the live plugin build")
    default int approvedBuild() { return 0; }
    @ConfigItem(keyName = "approvedSha256", name = "Approved class SHA-256", description = "Must match the loaded script class hash")
    default String approvedSha256() { return ""; }
}

