package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.transport.requirement.ItemRequirement;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TransportCatalogIntegrationTest
{
	private static List<Transport> transports;

	@BeforeClass
	public static void loadCatalog()
	{
		HashMap<WorldPoint, Set<Transport>> byOrigin = Transport.loadAllFromResources();
		transports = byOrigin.values().stream().flatMap(Set::stream).collect(Collectors.toList());
	}

	@Test
	public void pinnedCatalogAndMicrobotOnlyRowsAreBothPresent()
	{
		long upstreamRows = transports.stream()
			.filter(transport -> transport.getSource().startsWith("Skretzo/shortest-path@"))
			.count();
		assertTrue("expanded pinned catalog should contain thousands of edges", upstreamRows > 6000);
		assertTrue("Microbot NPC overlay must be retained", transports.stream()
			.anyMatch(transport -> transport.getType() == TransportType.NPC
				&& "microbot".equals(transport.getSource())));
	}

	@Test
	public void canonicalSpellRequirementsPreserveAndQuantities()
	{
		Transport varrock = find("TELEPORTATION_SPELL", "Varrock Teleport",
			new WorldPoint(3213, 3424, 0));
		assertNotNull(varrock.getCanonicalItemRequirements());
		List<ItemRequirement> requirements = varrock.getCanonicalItemRequirements().getRequirements();
		assertEquals(3, requirements.size());
		assertTrue(requirements.stream().anyMatch(requirement -> requirement.getQuantity() == 3));
		assertEquals("canonical upstream row must win over the stale local duplicate",
			"Skretzo/shortest-path@e3dc7c5", varrock.getSource());
	}

	@Test
	public void quetzalWhistleIncludesInfiniteVariant()
	{
		Transport whistle = find("QUETZAL_WHISTLE", "Quetzal whistle: Aldarin",
			new WorldPoint(1389, 2901, 0));
		assertTrue(whistle.getCanonicalItemRequirements().getRequirements().stream()
			.flatMapToInt(requirement -> Arrays.stream(requirement.getItemIds()))
			.anyMatch(itemId -> itemId == 33120));
		assertTrue("existing item executor handles the whistle map", TransportExecutionRegistry.canExecute(whistle));
	}

	@Test
	public void unsupportedPohCatalogFamiliesRemainFailClosed()
	{
		Transport box = transports.stream()
			.filter(transport -> "TELEPORTATION_BOX".equals(transport.getCatalogType()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("teleportation box catalog is missing"));
		assertFalse(TransportExecutionRegistry.canExecute(box));

		Transport portal = transports.stream()
			.filter(transport -> "TELEPORTATION_PORTAL_POH".equals(transport.getCatalogType()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("POH portal catalog is missing"));
		assertFalse(TransportExecutionRegistry.canExecute(portal));
	}

	@Test
	public void canonicalObjectInfoIsParsedForAutomation()
	{
		Transport tutorialDoor = transports.stream()
			.filter(transport -> "TRANSPORT".equals(transport.getCatalogType()))
			.filter(transport -> new WorldPoint(3097, 3107, 0).equals(transport.getOrigin()))
			.filter(transport -> new WorldPoint(3098, 3107, 0).equals(transport.getDestination()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("Tutorial Island door transport is missing"));

		assertEquals("Open", tutorialDoor.getAction());
		assertEquals("Door", tutorialDoor.getName());
		assertEquals(9398, tutorialDoor.getObjectId());
		assertTrue(TransportExecutionRegistry.canExecute(tutorialDoor));
	}

	@Test
	public void everyNonPohPinnedFamilyHasAnExecutor()
	{
		Set<String> unsupported = transports.stream()
			.filter(transport -> transport.getSource().startsWith("Skretzo/shortest-path@"))
			.filter(transport -> !TransportExecutionRegistry.canExecute(transport))
			.map(Transport::getCatalogType)
			.collect(Collectors.toSet());
		Map<String, List<String>> samples = transports.stream()
			.filter(transport -> transport.getSource().startsWith("Skretzo/shortest-path@"))
			.filter(transport -> !TransportExecutionRegistry.canExecute(transport))
			.collect(Collectors.groupingBy(
				Transport::getCatalogType,
				LinkedHashMap::new,
				Collectors.mapping(transport -> transport.getDisplayInfo() + " "
					+ transport.getOrigin() + " -> " + transport.getDestination(), Collectors.toList())));
		samples.replaceAll((type, values) -> values.stream().limit(3).collect(Collectors.toList()));

		assertEquals("unsupported samples: " + samples,
			Set.of("TELEPORTATION_BOX", "TELEPORTATION_PORTAL_POH"), unsupported);
	}

	private static Transport find(String catalogType, String displayInfo, WorldPoint destination)
	{
		return transports.stream()
			.filter(transport -> catalogType.equals(transport.getCatalogType()))
			.filter(transport -> displayInfo.equals(transport.getDisplayInfo()))
			.filter(transport -> destination.equals(transport.getDestination()))
			.findFirst()
			.orElseThrow(() -> new AssertionError(
				catalogType + " transport is missing: " + displayInfo + " -> " + destination));
	}
}
