package net.runelite.client.plugins.microbot.moonsOfPeril;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("perilousMoons")
public interface moonsOfPerilConfig extends Config {

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
            name = "Moonlight Potions Resupply",
            description = "Choose how many moonlight potions to have in inventory after resupplying",
            position = 0,
            section = resupplySection
    )
    default int moonlightPotionsQuantum()
    {
        return 4;
    }

    @ConfigItem(
            keyName = "moonlightPotionsMinimum",
            name = "Moonlight Potions Minimum",
            description = "The minimum moonlight potions to hold in inventory before bossing",
            position = 1,
            section = resupplySection
    )
    default int moonlightPotionsMinimum()
    {
        return 1;
    }

    @ConfigItem(
            keyName = "cookedBreamMinimum",
            name = "Cooked Bream Minimum",
            description = "The minimum cooked bream to hold in inventory before bossing",
            position = 2,
            section = resupplySection
    )
    default int cookedBreamMinimum()
    {
        return 1;
    }

}
