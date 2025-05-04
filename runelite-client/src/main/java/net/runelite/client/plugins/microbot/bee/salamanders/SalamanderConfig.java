package net.runelite.client.plugins.microbot.bee.salamanders;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Salamander")
public interface SalamanderConfig extends Config {

    @ConfigItem(
            keyName = "pluginDescription",
            name = "Plugin Info & How to Use",
            description = "Instructions and plugin status",
            position = 1
    )
    default String pluginDescription() {
        return "";
    }

    @ConfigItem(
            keyName = "salamanderHunting",
            name = "Salamander to hunt",
            description = "Select which salamander to hunt"
    )
    default SalamanderHunting salamanderHunting() {
        return null;
    }

    @ConfigItem(
            position = 1,
            keyName = "MinSleepAfterCatch",
            name = "Min. Sleep After Catch",
            description = "Min sleep after catch"
    )
    default int minSleepAfterCatch() {
        return 8300;
    }

    @ConfigItem(
            position = 2,
            keyName = "MaxSleepAfterCatch",
            name = "Max. Sleep After Catch",
            description = "Max sleep after catch"
    )
    default int maxSleepAfterCatch() {
        return 8400;
    }

    @ConfigItem(
            position = 3,
            keyName = "MinSleepAfterLay",
            name = "Min. Sleep After Lay",
            description = "Min sleep after lay"
    )
    default int minSleepAfterLay() {
        return 5500;
    }

    @ConfigItem(
            position = 4,
            keyName = "MaxSleepAfterLay",
            name = "Max. Sleep After Lay",
            description = "Max sleep after lay"
    )
    default int maxSleepAfterLay() {
        return 5700;
    }

    @ConfigItem(
            keyName = "showOverlay",
            name = "Show Overlay",
            description = "Displays overlay with traps and status"
    )
    default boolean showOverlay() {
        return true;
    }


}