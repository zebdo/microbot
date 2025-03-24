package net.runelite.client.plugins.microbot.pluginscheduler;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("PluginScheduler")
public interface SchedulerConfig extends Config {
    @ConfigItem(
            keyName = "scheduledPlugins",
            name = "Scheduled Plugins",
            description = "JSON representation of scheduled scripts",
            hidden = true
    )
    default String scheduledPlugins() {
        return "";
    }

    @ConfigItem(
            keyName = "scheduledPlugins",
            name = "Scheduled Plugins",
            description = "JSON representation of scheduled scripts",
            hidden = true
    )
    void setScheduledPlugins(String json);

    @ConfigItem(
            keyName = "logOut",
            name = "Log out",
            description = "Log out with no active plugins",
            hidden = true
    )
    default boolean logOut() { return false; }

    @ConfigItem(
            keyName = "randomDelay",
            name = "Random Delay",
            description = "Randomize delay for next run time plugins, between 0-5 minutes",
            hidden = true
    )
    default boolean randomDelay() { return false; }
}