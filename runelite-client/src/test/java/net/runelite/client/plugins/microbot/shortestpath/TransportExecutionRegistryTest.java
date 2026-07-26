package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransportExecutionRegistryTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(3201, 3200, 0);

	@Test
	public void genericObjectTransportRequiresExecutableObjectMetadata()
	{
		Transport valid = new Transport(
			ORIGIN, DESTINATION, "Door", TransportType.TRANSPORT, false,
			"Open", "Door", 123);
		Transport missingObject = new Transport(
			ORIGIN, DESTINATION, "Door", TransportType.TRANSPORT, false, 1);

		assertTrue(TransportExecutionRegistry.canExecute(valid));
		assertFalse(TransportExecutionRegistry.canExecute(missingObject));
	}

	@Test
	public void specializedTransportIsRegistered()
	{
		Transport itemTeleport = new Transport(
			null, DESTINATION, "Teleport", TransportType.TELEPORTATION_ITEM, true, 1);
		assertTrue(TransportExecutionRegistry.canExecute(itemTeleport));
	}

	@Test
	public void destinationWidgetNetworksAreRegistered()
	{
		Transport balloon = new Transport(
			ORIGIN, DESTINATION, "Varrock", TransportType.HOT_AIR_BALLOON, true, 1);
		Transport mushtree = new Transport(
			ORIGIN, new WorldPoint(3760, 3758, 0),
			"Verdant Valley", TransportType.MAGIC_MUSHTREE, true, 1);

		assertTrue(TransportExecutionRegistry.canExecute(balloon));
		assertTrue(TransportExecutionRegistry.canExecute(mushtree));
	}
}
