package net.runelite.client.plugins.microbot.gabplugs.sandminer;

import net.runelite.client.config.*;

@ConfigGroup("GabulhasSandMiner")
@ConfigInformation(
        "Mines sandstone and deposits it in the Quarry. Just start next to the grinder with a pickaxe. The scripts supports handling the heat using waterskins + humidify spell (Lunar spells) or using the circlet of water. If you get below 25% health, it will bank for safety.\n\n Updated by TaF"
)
public interface GabulhasSandMinerConfig extends Config {

    @ConfigSection(
            name = "Starting State",
            description = "Starting State",
            position = 1
    )
    String startingStateSection = "startingStateSection";

    @ConfigItem(
            keyName = "startingState",
            name = "(Debug) Starting State",
            description = "Starting State. Only used for development.",
            position = 1,
            section = startingStateSection
    )
    default GabulhasSandMinerInfo.states STARTINGSTATE() {
        return GabulhasSandMinerInfo.states.Mining;
    }

}


