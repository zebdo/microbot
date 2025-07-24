package net.runelite.client.plugins.microbot.cardewsPlugins.AutoBankPin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("AutoBankPin")
public interface AutoBankPinConfig extends Config {
    @ConfigSection(
            name = "Bank Pin",
            description = "Configure the Bank Pin to be entered",
            position = 0,
            closedByDefault = true
    )
    String bankPinSection = "Configure the Bank Pin to be entered";
    @ConfigItem(
            position = 0,
            keyName = "pinNumber1",
            name = "Pin Number 1",
            description = "First digit to enter",
            section = bankPinSection
    )
    default int digit1() { return 0; }
    @ConfigItem(
            position = 1,
            keyName = "pinNumber2",
            name = "Pin Number 2",
            description = "Second digit to enter",
            section = bankPinSection
    )
    default int digit2() { return 0; }
    @ConfigItem(
            position = 2,
            keyName = "pinNumber3",
            name = "Pin Number 3",
            description = "Third digit to enter",
            section = bankPinSection
    )
    default int digit3() { return 0; }
    @ConfigItem(
            position = 3,
            keyName = "pinNumber4",
            name = "Pin Number 4",
            description = "Fourth and last digit to enter",
            section = bankPinSection
    )
    default int digit4() { return 0; }
}
