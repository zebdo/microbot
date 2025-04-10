package net.runelite.client.plugins.microbot.pluginscheduler;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import static net.runelite.client.plugins.microbot.pluginscheduler.SchedulerPlugin.configGroup;

@ConfigGroup(configGroup)
public interface SchedulerConfig extends Config {
    final static String CONFIG_GROUP = "PluginScheduler";
    
    @ConfigSection(
            name = "Control",
            description = "Control settings for the plugin scheduler, force stop, etc.",
            position = 11,
            closedByDefault = true
    )
    String controlSection = "Control";
    @ConfigSection(
            name = "Conditions",
            description = "Conditions settings for the plugin scheduler, enforce conditions, etc.",            
            position = 12,
            closedByDefault = true
    )
    String conditionsSection = "Conditions";

    // hidden settings for saving config automatically via runelite config menager
    @ConfigItem(
            keyName = "scheduledPlugins",
            name = "Scheduled Plugins",
            description = "JSON representation of scheduled scripts",
            hidden = true
    )

    default String scheduledPlugins() {
        return "";
    }
   
    void setScheduledPlugins(String json);
    /// Control settings

    @ConfigItem(
        keyName = "softStopRetrySeconds",
        name = "Soft Stop Retry (seconds)",
        description = "Time in seconds between soft stop retry attempts",
        position = 1,
        section = controlSection
    )
    default int softStopRetrySeconds() {
        return 60;
    }
    @ConfigItem(
        keyName = "enableHardStop",
        name = "Enable Hard Stop",
        description = "Enable hard stop after soft stop attempts",
        position = 2,
        section = controlSection
    )
    default boolean enableHardStop() {
        return true;
    }
    void setEnableHardStop(boolean enable);
    @ConfigItem(
        keyName = "hardStopTimeoutSeconds",
        name = "Hard Stop Timeout (seconds)",
        description = "Time in seconds before forcing a hard stop after initial soft stop attempt",
        position = 3,
        section = controlSection
    )
    default int hardStopTimeoutSeconds() {
        return 360;
    }
    void setHardStopTimeoutSeconds(int seconds);

    // Conditions settings
    @ConfigItem(
        keyName = "enforceStopConditions",
        name = "Enforce Stop Conditions",
        description = "Prompt for confirmation before running plugins without stop conditions",
        position = 1,
        section = conditionsSection
    )
    default boolean enforceStopConditions() {
        return true;
    }
    void setEnforceStopConditions(boolean enforce);
}