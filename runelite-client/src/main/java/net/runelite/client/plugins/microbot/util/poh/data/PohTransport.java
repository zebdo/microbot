package net.runelite.client.plugins.microbot.util.poh.data;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

@Slf4j
public class PohTransport extends Transport {

    private final PohTransportable transportable;

    /**
     * Constructs a simple "teleport-like" transport with no fixed origin.
     * The pathfinder can treat this as an available transport when conditions are met.
     */
    public PohTransport(PohTransportable transportable) {
        super(transportable.toString(), transportable.getDestination(), TransportType.POH, 6, true);
        this.transportable = transportable;
    }

    public boolean transport() {
        if (!PohTeleports.isInHouse()) {
            log.debug("Not in house, going inside first...");
            if (PohTeleports.teleportToPoh()) {
                if (!sleepUntil(() -> !Rs2Player.isAnimating() && PohTeleports.isInHouse(), 5000)) {
                    return false;
                }
            }
        }
        return transportable.transport();
    }

    @Override
    public String toString() {
        return getDisplayInfo();
    }
}

