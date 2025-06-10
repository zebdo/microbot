package net.runelite.client.plugins.microbot.perilousMoons;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.plugins.microbot.mining.enums.Rocks;

@ConfigGroup("perilousMoons")
public interface perilousMoonsConfig extends Config {

    @ConfigSection(
            name = "Resupply",
            description = "Configure your resupply settings at the end of each run",
            position = 0
    )
    String resupplySection = "Resupply Settings";

    @ConfigSection(
            name = "Bossing",
            description = "Bossing",
            position = 2
    )
    String bossSection = "Bossing";


    @ConfigItem(
            keyName = "moonlightPotionsQuantum",
            name = "Moonlight Potions",
            description = "Choose how many moonlight potions(4) to make",
            position = 0,
            section = resupplySection
    )
    default int moonlightPotionsQuantum()
    {
        return 4;
    }

}
