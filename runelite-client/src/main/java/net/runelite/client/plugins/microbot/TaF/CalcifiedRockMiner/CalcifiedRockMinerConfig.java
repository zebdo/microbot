package net.runelite.client.plugins.microbot.TaF.CalcifiedRockMiner;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.inventorysetups.InventorySetup;

@ConfigInformation(
        "CalcifiedRockMiner")
@ConfigGroup("CalcifiedRockMiner")
public interface CalcifiedRockMinerConfig extends Config {
    @ConfigItem(
            keyName = "dropDeposits",
            name = "Drop deposits",
            description = "When enabled, the deposits will be dropped",
            position = 1
    )
    default boolean dropDeposits() {
        return false;
    }

    @ConfigItem(
            keyName = "crushDeposits",
            name = "Crush deposits",
            description = "When enabled, the deposits will be crushed before banking",
            position = 2
    )
    default boolean crushDeposits() {
        return false;
    }


    @ConfigItem(
            keyName = "focusCrackedWaterDeposits",
            name = "Focus on cracked water deposits",
            description = "Go out of the way to focus on mining cracked water deposits",
            position = 2
    )
    default boolean focusCrackedWaterDeposits() {
        return false;
    }
}
