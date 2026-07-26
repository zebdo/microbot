package net.runelite.client.plugins.microbot.shortestpath.pathfinder;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;

import java.util.Objects;

/**
 * One exact step in a computed route.
 *
 * <p>{@code transportFromPrevious} is non-null only when the planner selected a
 * catalog transport for the edge from the preceding step to this position. Keeping
 * that identity avoids reconstructing an arbitrary transport later from two matching
 * coordinates.</p>
 */
@Getter
public final class RouteStep
{
	private final WorldPoint position;
	private final Transport transportFromPrevious;

	public RouteStep(WorldPoint position, Transport transportFromPrevious)
	{
		this.position = Objects.requireNonNull(position, "position");
		this.transportFromPrevious = transportFromPrevious;
	}

	public boolean usesTransport()
	{
		return transportFromPrevious != null;
	}
}
