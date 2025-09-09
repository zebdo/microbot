package net.runelite.client.plugins.microbot.util.poh;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.poh.data.HouseStyle;
import net.runelite.client.plugins.microbot.util.poh.data.PohTeleport;

public class PohTransport extends Transport {

    @Getter
    private final PohTeleport teleport;

    public PohTransport(PohTeleport teleport) {
        super(teleport.getDestination(), teleport.displayInfo(), TransportType.POH);
        this.teleport = teleport;
    }

    @Override
    public WorldPoint getOrigin() {
        HouseStyle style = HouseStyle.getStyle();
        return style != null ? style.getPohLocation() : new WorldPoint(-1, -1, -1);
    }

    public boolean execute() {
        return teleport.execute();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PohTransport that = (PohTransport) o;
        return teleport.equals(that.teleport);
    }

}
