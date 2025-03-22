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
}