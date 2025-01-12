package net.runelite.client.plugins.microbot.runecrafting.runebodycrafter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("RuneBodyCrafter")
public interface RuneBodyCrafterConfig extends Config {

    @ConfigSection(
            name = "General",
            description = "General Information & Settings",
            position = 0
    )
    String generalSection = "General";

    @ConfigItem(
            keyName = "about",
            name = "About This Script",
            position = 0,
            description = "",
            section = generalSection
    )
    default String about() {
        return "This plugin crafts runes at the body altar.\n\nBe near the Edgeville bank before starting the script and have a Body Tiara in the Bank";
    }
}
