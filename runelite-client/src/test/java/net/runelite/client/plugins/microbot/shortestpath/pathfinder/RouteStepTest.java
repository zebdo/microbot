package net.runelite.client.plugins.microbot.shortestpath.pathfinder;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class RouteStepTest
{
	@Test
	public void forwardRouteCarriesExactSelectedTransport()
	{
		WorldPoint start = new WorldPoint(3200, 3200, 0);
		WorldPoint origin = new WorldPoint(3201, 3200, 0);
		WorldPoint destination = new WorldPoint(3300, 3300, 0);
		Transport transport = new Transport(
			origin, destination, "Test", TransportType.TRANSPORT, false,
			"Open", "Door", 123);

		Node startNode = new Node(start, null);
		Node originNode = new Node(origin, startNode);
		Node destinationNode = new TransportNode(destination, originNode, 1, transport);

		List<RouteStep> route = Pathfinder.buildForwardRouteSteps(destinationNode);
		assertEquals(3, route.size());
		assertNull(route.get(1).getTransportFromPrevious());
		assertSame(transport, route.get(2).getTransportFromPrevious());
	}

	@Test
	public void bidirectionalJoinKeepsReverseSearchTransportIdentity()
	{
		WorldPoint start = new WorldPoint(3200, 3200, 0);
		WorldPoint meeting = new WorldPoint(3201, 3200, 0);
		WorldPoint destination = new WorldPoint(3300, 3300, 0);
		Transport transport = new Transport(
			meeting, destination, "Test", TransportType.TRANSPORT, false,
			"Open", "Door", 123);

		Node forwardStart = new Node(start, null);
		Node forwardMeeting = new Node(meeting, forwardStart);
		Node backwardGoal = new Node(destination, null);
		Node backwardMeeting = new TransportNode(meeting, backwardGoal, 1, transport, true);

		List<RouteStep> route = Pathfinder.combineBidirectionalRoute(forwardMeeting, backwardMeeting);
		assertEquals(3, route.size());
		assertEquals(destination, route.get(2).getPosition());
		assertSame(transport, route.get(2).getTransportFromPrevious());
	}
}
