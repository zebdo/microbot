package net.runelite.client.plugins.microbot.util.poh.data;

import net.runelite.api.coords.WorldPoint;

public interface PohTransportable {

    WorldPoint getDestination();

    boolean transport();
}
