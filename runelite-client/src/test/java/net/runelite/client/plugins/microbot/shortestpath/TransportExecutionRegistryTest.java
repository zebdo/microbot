package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
		assertEquals(
			TransportExecutionRegistry.Executor.OBJECT,
			TransportExecutionRegistry.executionIntentFor(valid).orElseThrow().getExecutor());
	}

	@Test
	public void specializedTransportIsRegistered()
	{
		Transport itemTeleport = new Transport(
			null, DESTINATION, "Teleport", TransportType.TELEPORTATION_ITEM, true, 1);
		assertTrue(TransportExecutionRegistry.canExecute(itemTeleport));
		assertEquals(
			TransportExecutionRegistry.Executor.ITEM_TELEPORT,
			TransportExecutionRegistry.executionIntentFor(itemTeleport).orElseThrow().getExecutor());
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
		assertEquals(
			TransportExecutionRegistry.Executor.HOT_AIR_BALLOON,
			TransportExecutionRegistry.executionIntentFor(balloon).orElseThrow().getExecutor());
		assertEquals(
			TransportExecutionRegistry.Executor.MAGIC_MUSHTREE,
			TransportExecutionRegistry.executionIntentFor(mushtree).orElseThrow().getExecutor());
	}

	@Test
	public void npcIntentRecordsDialogueTargetAndAction()
	{
		Transport npc = new Transport(
			ORIGIN, DESTINATION, "Destination", TransportType.NPC, true,
			"Travel", "Renu", 13350);

		TransportExecutionRegistry.ExecutionIntent intent =
			TransportExecutionRegistry.executionIntentFor(npc).orElseThrow();
		assertEquals(TransportExecutionRegistry.Executor.NPC_DIALOGUE, intent.getExecutor());
		assertEquals("Travel", intent.getAction());
		assertEquals("Renu", intent.getTarget());
		assertEquals("Destination", intent.getDisplayInfo());
	}
}
