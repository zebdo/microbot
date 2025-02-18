package net.runelite.client.plugins.microbot.autoGauntletPrayer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Auto Gauntlet Prayer")
@ConfigInformation("LiftedMango <br> 0.1.0 <br><br> Does Gauntlet stuff"
)
public interface AutoGauntletConfig extends Config {
    @ConfigItem(
            keyName = "mysticMight?",
            name = "Use mystic might instead of Augury? This will also force Eagle Eye and Ultimate Strength to be used",
            description = "Use lesser prayers?",
            position = 1
    )
    default boolean MysticMight() {
        return false;
    }
}
