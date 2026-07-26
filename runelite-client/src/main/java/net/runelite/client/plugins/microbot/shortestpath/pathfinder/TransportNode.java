package net.runelite.client.plugins.microbot.shortestpath.pathfinder;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;

public class TransportNode extends Node implements Comparable<TransportNode> {
    private final Transport transport;
    private final boolean reverse;

    public TransportNode(WorldPoint point, Node previous, int travelTime, Transport transport) {
        this(point, previous, travelTime, transport, false);
    }

    public TransportNode(WorldPoint point, Node previous, int travelTime, Transport transport, boolean reverse) {
        // Use Node(int, Node, int cost) which assigns cost directly. The WorldPoint
        // Node constructor re-adds previous.cost via its cost(previous, wait) method,
        // which caused (a) double-counting when we passed prev.cost + travelTime as
        // wait and (b) integer overflow for plane-crossing transports with travelTime=0
        // because its distance fallback returns Integer.MAX_VALUE across planes.
        super(net.runelite.client.plugins.microbot.shortestpath.WorldPointUtil.packWorldPoint(point),
                previous,
                (previous != null ? previous.cost : 0) + travelTime);
        this.transport = transport;
        this.reverse = reverse;
    }

    public Transport getTransport() {
        return transport;
    }

    public boolean isReverse() {
        return reverse;
    }

    @Override
    public int compareTo(TransportNode other) {
        return Integer.compare(cost, other.cost);
    }
}
