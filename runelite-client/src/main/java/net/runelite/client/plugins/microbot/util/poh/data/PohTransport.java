package net.runelite.client.plugins.microbot.util.poh.data;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

/**
 * Represents a "teleport-like" transport that primarily operates within a Player Owned House (POH).
 * This transport leverages a {@link PohTransportable} instance to handle destination-specific
 * transport operations once the player is in their POH.
 * <p>
 * The transport logic accounts for ensuring the player is inside their POH before
 * executing the associated transportable's actions. This class serves as an abstraction
 * for POH-based teleportation mechanisms.
 */
@Slf4j
public class PohTransport extends Transport {

    private final PohTransportable transportable;

    /**
     * Constructs a simple "teleport-like" transport with no fixed origin.
     * The pathfinder can treat this as an available transport when conditions are met.
     */
    public PohTransport(PohTransportable transportable) {
        super(transportable.toString(), transportable.getDestination(), TransportType.POH, transportable.getTime(), true);
        this.transportable = transportable;
    }

    /**
     * Executes the PoH transport action, ensuring the player is inside their POH (Player Owned House)
     * before delegating the transport operation to the associated {@code PohTransportable} instance.
     *
     * @return {@code true} if the transport operation is successful; {@code false} otherwise.
     */
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

