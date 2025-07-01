package net.runelite.client.plugins.microbot.moonsOfPeril;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("perilousMoons")
public interface moonsOfPerilConfig extends Config {

    @ConfigSection(
            name     = "General",
            description = "Global settings for the plugin",
            position = 0
    )
    String generalSection = "General";

    @ConfigSection(
            name = "Resupply",
            description = "Configure your resupply settings at the end of each run",
            position = 1
    )
    String resupplySection = "Resupply Settings";

    @ConfigSection(
            name = "Eclipse Moon",
            description = "Eclipse Moon",
            position = 2
    )
    String eclipseMoonSection = "Eclipse Moon";

    @ConfigSection(
            name = "Blue Moon",
            description = "Blue Moon",
            position = 3
    )
    String blueMoonSection = "Blue Moon";

    @ConfigSection(
            name = "Blood Moon",
            description = "Blood Moon",
            position = 4
    )
    String bloodMoonSection = "Blood Moon";

    @ConfigItem(
            keyName  = "debugLogging",
            name     = "Debug logging",
            description = "Logs to the Microbot console",
            position = 0,
            section  = generalSection
    )
    default boolean debugLogging() { return false; }

    @ConfigItem(
            keyName  = "shutdownOnDeath",
            name     = "Shutdown on death",
            description = "Automatically stop the script after a death event",
            position = 1,
            section  = generalSection
    )
    default boolean shutdownOnDeath() { return false; }

    @ConfigItem(
            keyName  = "healthPercentage",
            name     = "Health % topup",
            description = "Strategically eat during boss sequences below this health %",
            position = 2,
            section  = generalSection
    )
    default int healthPercentage() { return 70; }

    @ConfigItem(
            keyName  = "prayerPercentage",
            name     = "Prayer % topup",
            description = "Strategically drink during boss sequences below this prayer %",
            position = 2,
            section  = generalSection
    )
    default int prayerPercentage() { return 70; }

    @ConfigItem(
            keyName = "moonlightPotionsQuantum",
            name = "Moonlight Potions Resupply",
            description = "Choose how many moonlight potions to have in inventory post resupply",
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
            keyName  = "enableEclipse",
            name     = "Fight Eclipse Moon",
            description = "Untick to skip Eclipse Moon runs",
            position = 0,
            section  = eclipseMoonSection
    )
    default boolean enableEclipse() { return true; }

    @ConfigItem(
            keyName = "eclipseWeaponMain",
            name = "Eclipse - Main Weapon",
            description = "The exact name of the weapon to wield during the Eclipse Moon normal attack sequence",
            position = 1,
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
            position = 2,
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
            position = 3,
            section = eclipseMoonSection
    )

    default String eclipseWeaponClones()
    {
        return "";
    }

    @ConfigItem(
            keyName  = "enableBlue",
            name     = "Fight Blue Moon",
            description = "Untick to skip Blue Moon runs",
            position = 0,
            section  = blueMoonSection
    )
    default boolean enableBlue() { return true; }

    @ConfigItem(
            keyName = "blueWeaponMain",
            name = "Blue - Main Weapon",
            description = "The exact name of the weapon to wield during the Blue Moon normal attack sequence",
            position = 1,
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
            position = 2,
            section = blueMoonSection
    )

    default String blueShield()
    {
        return "";
    };

    @ConfigItem(
            keyName  = "enableBlood",
            name     = "Fight Blood Moon",
            description = "Untick to skip Blood Moon runs",
            position = 0,
            section  = bloodMoonSection
    )
    default boolean enableBlood() { return true; }

    @ConfigItem(
            keyName = "bloodWeaponMain",
            name = "Blood - Main Weapon",
            description = "The exact name of the weapon to wield during the Blood Moon normal attack sequence",
            position = 1,
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
            position = 2,
            section = bloodMoonSection
    )

    default String bloodShield()
    {
        return "";
    };

}
