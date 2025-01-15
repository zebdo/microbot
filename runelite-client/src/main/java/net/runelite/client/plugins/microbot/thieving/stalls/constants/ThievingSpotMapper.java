package net.runelite.client.plugins.microbot.thieving.stalls.constants;

import lombok.AllArgsConstructor;
import net.runelite.client.plugins.microbot.thieving.stalls.model.*;

import javax.inject.Inject;
import java.util.Map;

@AllArgsConstructor(onConstructor_ = @Inject)
public class ThievingSpotMapper {

    public IStallThievingSpot getThievingSpot(final ThievingSpot thievingSpot)
    {
        final Map<ThievingSpot, IStallThievingSpot> map = Map.of(
                ThievingSpot.VARROCK_TEA_STALL, varrockTeaStallThievingSpot,
                ThievingSpot.ARDY_BAKER, ardyBakerThievingSpot,
                ThievingSpot.ARDY_SILK, ardySilkThievingSpot,
                ThievingSpot.HOSIDIUS_FRUIT, hosidiusFruitThievingSpot
        );

        return map.get(thievingSpot);
    }

    private VarrockTeaStallThievingSpot varrockTeaStallThievingSpot;
    private ArdyBakerThievingSpot ardyBakerThievingSpot;
    private ArdySilkThievingSpot ardySilkThievingSpot;
    private HosidiusFruitThievingSpot hosidiusFruitThievingSpot;
}
