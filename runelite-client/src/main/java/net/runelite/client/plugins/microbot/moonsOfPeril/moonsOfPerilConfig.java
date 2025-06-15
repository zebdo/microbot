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
            name = "Eclipse Moon",
            description = "Eclipse Moon",
            position = 1
    )
    String eclipseMoonSection = "Eclipse Moon";

    @ConfigSection(
            name = "Blue Moon",
            description = "Blue Moon",
            position = 2
    )
    String blueMoonSection = "Blue Moon";

    @ConfigSection(
            name = "Blood Moon",
            description = "Blood Moon",
            position = 3
    )
    String bloodMoonSection = "Blood Moon";


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
        return 2;
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
        return 10;
    }

    @ConfigItem(
            keyName = "eclipseWeaponMain",
            name = "Eclipse - Main Weapon",
            description = "The exact name of the weapon to wield during the Eclipse Moon normal attack sequence",
            position = 0,
            section = eclipseMoonSection
    )
    default String eclipseWeaponMain()
    {
        return "";
    };

    @ConfigItem(
            keyName = "eclipseShield",
            name = "Eclipse - Shield",
            description = "Leave blank if main weapon is 2-handed. The exact name of the shield wield during the Eclipse Moon normal attack sequence",
            position = 1,
            section = eclipseMoonSection
    )

    default String eclipseShield()
    {
        return "";
    };

    @ConfigItem(
            keyName = "eclipseWeaponClones",
            name = "Eclipse - Clones Weapon",
            description = "The exact name of the equipment to  wield during the Eclipse Moon Clones attack sequence",
            position = 2,
            section = eclipseMoonSection
    )

    default String eclipseWeaponClones()
    {
        return "";
    }

    @ConfigItem(
            keyName = "blueWeaponMain",
            name = "Blue - Main Weapon",
            description = "The exact name of the weapon to wield during the Blue Moon normal attack sequence",
            position = 0,
            section = blueMoonSection
    )
    default String blueWeaponMain()
    {
        return "";
    };

    @ConfigItem(
            keyName = "blueShield",
            name = "Blue - Shield",
            description = "Leave blank if main weapon is 2-handed. The exact name of the shield wield during the Blue Moon normal attack sequence",
            position = 1,
            section = blueMoonSection
    )

    default String blueShield()
    {
        return "";
    };

    @ConfigItem(
            keyName = "bloodWeaponMain",
            name = "Blood - Main Weapon",
            description = "The exact name of the weapon to wield during the Blood Moon normal attack sequence",
            position = 0,
            section = bloodMoonSection
    )
    default String bloodWeaponMain()
    {
        return "";
    };

    @ConfigItem(
            keyName = "bloodShield",
            name = "Blood - Shield",
            description = "Leave blank if main weapon is 2-handed. The exact name of the shield wield during the Blood Moon normal attack sequence",
            position = 1,
            section = bloodMoonSection
    )

    default String bloodShield()
    {
        return "";
    };

}
