package net.runelite.client.plugins.microbot.util.poh;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.poh.data.HouseStyle;
import net.runelite.client.plugins.microbot.util.poh.data.PohTeleport;

/**
 * Represents a transport mechanism using the Player-Owned House (POH) teleportation system.
 * This class extends the base Transport class and provides specific implementations
 * for POH teleportation.
 */
public class PohTransport extends Transport {

    @Getter
    private final PohTeleport teleport;
    private final WorldPoint exitPortalPoint;

    public PohTransport(PohTeleport teleport) {
        super(teleport.getDestination(), teleport.displayInfo(), TransportType.POH);
        this.teleport = teleport;
        HouseStyle style = HouseStyle.getStyle();
        this.exitPortalPoint = style != null ? style.getPohLocation() : new WorldPoint(-1, -1, -1);
    }

    /**
     * The origin of a PoH transport is the WorldPoint inside Player-Owned House (POH) on entering
     *
     * @return WorldPoint N.W. of Exit Portal
     */
    @Override
    public WorldPoint getOrigin() {
        return exitPortalPoint;
    }

    /**
     * Executes the Transport's PoH teleportation action.
     *
     * @return true on successful teleportation
     */
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
