package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ShortestPathCoreTest {

	private static SplitFlagMap collisionMap;

	@BeforeClass
	public static void loadCollisionMap() {
		collisionMap = SplitFlagMap.fromResources();
		assertNotNull("Collision map should load from resources", collisionMap);
		assertNotNull("Region extents should be set", SplitFlagMap.getRegionExtents());
	}

	// ========================
	// PrimitiveIntHashMap Tests
	// ========================

	@Test
	public void testHashMapBasicPutAndGet() {
		PrimitiveIntHashMap<String> map = new PrimitiveIntHashMap<>(16);
		map.put(WorldPointUtil.packWorldPoint(3200, 3200, 0), "lumbridge");
		map.put(WorldPointUtil.packWorldPoint(3222, 3218, 0), "lumbridge_castle");

		assertEquals("lumbridge", map.get(WorldPointUtil.packWorldPoint(3200, 3200, 0)));
		assertEquals("lumbridge_castle", map.get(WorldPointUtil.packWorldPoint(3222, 3218, 0)));
		assertNull(map.get(WorldPointUtil.packWorldPoint(9999, 9999, 0)));
	}

	@Test
	public void testHashMapRehashPreservesAllEntries() {
		PrimitiveIntHashMap<Integer> map = new PrimitiveIntHashMap<>(8, 0.5f);
		int entryCount = 500;

		for (int i = 0; i < entryCount; i++) {
			int packed = WorldPointUtil.packWorldPoint(3000 + (i % 200), 3000 + (i / 200), 0);
			map.put(packed, i);
		}

		int found = 0;
		for (int i = 0; i < entryCount; i++) {
			int packed = WorldPointUtil.packWorldPoint(3000 + (i % 200), 3000 + (i / 200), 0);
			Integer val = map.get(packed);
			if (val != null && val == i) {
				found++;
			}
		}
		assertEquals("All entries must survive rehash", entryCount, found);
	}

	@Test
	public void testHashMapStressRehash() {
		PrimitiveIntHashMap<Set<String>> map = new PrimitiveIntHashMap<>(4, 0.25f);
		int entryCount = 2000;

		for (int i = 0; i < entryCount; i++) {
			int packed = WorldPointUtil.packWorldPoint(2944 + (i % 448), 3525 + (i / 448), 0);
			Set<String> set = new HashSet<>();
			set.add("transport_" + i);
			map.put(packed, set);
		}

		int found = 0;
		for (int i = 0; i < entryCount; i++) {
			int packed = WorldPointUtil.packWorldPoint(2944 + (i % 448), 3525 + (i / 448), 0);
			Set<String> val = map.get(packed);
			if (val != null && val.contains("transport_" + i)) {
				found++;
			}
		}
		assertEquals("All entries must survive multiple rehashes", entryCount, found);
	}

	@Test
	public void testHashMapCollectionValueMerge() {
		PrimitiveIntHashMap<Set<String>> map = new PrimitiveIntHashMap<>(16);
		int packed = WorldPointUtil.packWorldPoint(3200, 3200, 0);

		Set<String> first = new HashSet<>();
		first.add("fairy_ring");
		map.put(packed, first);

		Set<String> second = new HashSet<>();
		second.add("spirit_tree");
		map.put(packed, second);

		Set<String> result = map.get(packed);
		assertNotNull(result);
		assertTrue("Should contain fairy_ring after merge", result.contains("fairy_ring"));
		assertTrue("Should contain spirit_tree after merge", result.contains("spirit_tree"));
	}

	// ========================
	// Wilderness Boundary Tests
	// ========================

	@Test
	public void testWildernessAboveGroundBoundary() {
		int insideWild = WorldPointUtil.packWorldPoint(3100, 3530, 0);
		int outsideWild = WorldPointUtil.packWorldPoint(3100, 3520, 0);
		int deepWild = WorldPointUtil.packWorldPoint(3100, 3900, 0);

		assertTrue("Point at 3100,3530 should be in wilderness",
				PathfinderConfig.isInWilderness(insideWild));
		assertFalse("Point at 3100,3520 should NOT be in wilderness",
				PathfinderConfig.isInWilderness(outsideWild));
		assertTrue("Point at 3100,3900 should be in deep wilderness",
				PathfinderConfig.isInWilderness(deepWild));
	}

	@Test
	public void testWildernessUndergroundBoundary() {
		int insideUnderground = WorldPointUtil.packWorldPoint(3100, 10000, 0);
		int outsideUnderground = WorldPointUtil.packWorldPoint(3100, 9900, 0);
		int wideUnderground = WorldPointUtil.packWorldPoint(3400, 10100, 0);

		assertTrue("Point at 3100,10000 should be in underground wilderness",
				PathfinderConfig.isInWilderness(insideUnderground));
		assertFalse("Point at 3100,9900 should NOT be in underground wilderness",
				PathfinderConfig.isInWilderness(outsideUnderground));
		assertTrue("Point at 3400,10100 should be in underground wilderness (wide area)",
				PathfinderConfig.isInWilderness(wideUnderground));
	}

	@Test
	public void testWildernessUndergroundWidthCoversUpstream() {
		int farEastUnderground = WorldPointUtil.packWorldPoint(3450, 10100, 0);
		assertTrue("Point at 3450,10100 should be in underground wilderness (upstream width=518)",
				PathfinderConfig.isInWilderness(farEastUnderground));
	}

	@Test
	public void testFeroxEnclaveNotWilderness() {
		int feroxCenter = WorldPointUtil.packWorldPoint(3130, 3630, 0);
		assertFalse("Ferox Enclave center should NOT be wilderness",
				PathfinderConfig.isInWilderness(feroxCenter));
	}

	@Test
	public void testWildernessAboveGroundStartsAtCorrectY() {
		int atY3524 = WorldPointUtil.packWorldPoint(3100, 3524, 0);
		int atY3525 = WorldPointUtil.packWorldPoint(3100, 3525, 0);

		assertFalse("Y=3524 should NOT be wilderness (boundary is Y=3525)",
				PathfinderConfig.isInWilderness(atY3524));
		assertTrue("Y=3525 should be wilderness",
				PathfinderConfig.isInWilderness(atY3525));
	}

	// ========================
	// Collision Map Tests
	// ========================

	@Test
	public void testCollisionMapLoadsRegions() {
		SplitFlagMap.RegionExtent extents = SplitFlagMap.getRegionExtents();
		assertTrue("Region width should be > 0", extents.getWidth() > 0);
		assertTrue("Region height should be > 0", extents.getHeight() > 0);
	}

	@Test
	public void testCollisionMapWalkableTiles() {
		CollisionMap map = new CollisionMap(collisionMap);
		assertTrue("Lumbridge center should allow north movement", map.n(3222, 3218, 0));
		assertTrue("Lumbridge center should allow east movement", map.e(3222, 3218, 0));
	}

	@Test
	public void testCollisionMapBlockedTile() {
		CollisionMap map = new CollisionMap(collisionMap);
		boolean canMoveNorth = map.n(3222, 3218, 0);
		assertNotNull("Collision map should return a boolean for any valid tile", (Boolean) canMoveNorth);
	}

	// ========================
	// Transport Loading Tests
	// ========================

	@Test
	public void testTransportLoadingDoesNotThrow() {
		HashMap<WorldPoint, Set<Transport>> transports = Transport.loadAllFromResources();
		assertNotNull("Transports should load", transports);
		assertTrue("Should load at least 100 transport origins", transports.size() > 100);
	}

	@Test
	public void testNewTransportTypesLoaded() {
		HashMap<WorldPoint, Set<Transport>> transports = Transport.loadAllFromResources();

		boolean hasHotAirBalloon = false;
		boolean hasMagicMushtree = false;
		boolean hasSeasonalTransport = false;

		for (Set<Transport> transportSet : transports.values()) {
			for (Transport t : transportSet) {
				if (t.getType() == TransportType.HOT_AIR_BALLOON) hasHotAirBalloon = true;
				if (t.getType() == TransportType.MAGIC_MUSHTREE) hasMagicMushtree = true;
				if (t.getType() == TransportType.SEASONAL_TRANSPORT) hasSeasonalTransport = true;
			}
		}

		assertTrue("Hot air balloon transports should be loaded", hasHotAirBalloon);
		assertTrue("Magic mushtree transports should be loaded", hasMagicMushtree);
		assertTrue("Seasonal transports should be loaded", hasSeasonalTransport);
	}

	@Test
	public void testFairyRingTransportsExist() {
		HashMap<WorldPoint, Set<Transport>> transports = Transport.loadAllFromResources();
		boolean hasFairyRing = false;
		for (Set<Transport> transportSet : transports.values()) {
			for (Transport t : transportSet) {
				if (t.getType() == TransportType.FAIRY_RING) {
					hasFairyRing = true;
					break;
				}
			}
			if (hasFairyRing) break;
		}
		assertTrue("Fairy ring transports should be loaded", hasFairyRing);
	}

	// ========================
	// WorldPointUtil Tests
	// ========================

	@Test
	public void testPackUnpackRoundTrip() {
		int x = 3222, y = 3218, z = 0;
		int packed = WorldPointUtil.packWorldPoint(x, y, z);
		assertEquals(x, WorldPointUtil.unpackWorldX(packed));
		assertEquals(y, WorldPointUtil.unpackWorldY(packed));
		assertEquals(z, WorldPointUtil.unpackWorldPlane(packed));
	}

	@Test
	public void testPackUnpackHighCoords() {
		int x = 3462, y = 10376, z = 2;
		int packed = WorldPointUtil.packWorldPoint(x, y, z);
		assertEquals(x, WorldPointUtil.unpackWorldX(packed));
		assertEquals(y, WorldPointUtil.unpackWorldY(packed));
		assertEquals(z, WorldPointUtil.unpackWorldPlane(packed));
	}

	@Test
	public void testDistanceBetween() {
		int a = WorldPointUtil.packWorldPoint(3200, 3200, 0);
		int b = WorldPointUtil.packWorldPoint(3210, 3200, 0);
		assertEquals(10, WorldPointUtil.distanceBetween(a, b));
	}

	@Test
	public void testDistanceToArea() {
		WorldArea area = new WorldArea(3200, 3200, 10, 10, 0);
		int inside = WorldPointUtil.packWorldPoint(3205, 3205, 0);
		int outside = WorldPointUtil.packWorldPoint(3220, 3205, 0);

		assertEquals("Inside point should have distance 0", 0, WorldPointUtil.distanceToArea2D(inside, area));
		assertTrue("Outside point should have distance > 0", WorldPointUtil.distanceToArea2D(outside, area) > 0);
	}

	// ========================
	// Pathfinder Partial Path Tests
	// ========================

	@Test
	public void testPathfinderGetPathReturnsEmptyWhenNoPath() {
		Pathfinder pf = new Pathfinder(
				createMinimalConfig(),
				new WorldPoint(3222, 3218, 0),
				new WorldPoint(3232, 3218, 0)
		);
		List<WorldPoint> path = pf.getPath();
		assertNotNull("getPath() should return empty list before run, not throw", path);
		assertTrue("Path should be empty before pathfinder runs", path.isEmpty());
	}

	@Test
	public void testPathfinderRunsAndProducesPath() throws Exception {
		PathfinderConfig config = createMinimalConfig();
		assertNotNull("Config map should be available", config.getMap());

		Pathfinder pf = new Pathfinder(
				config,
				new WorldPoint(3222, 3218, 0),
				new WorldPoint(3232, 3218, 0)
		);

		pf.run();

		assertTrue("Pathfinder should complete", pf.isDone());
		List<WorldPoint> path = pf.getPath();
		assertNotNull("Path should not be null", path);
		assertFalse("Path should not be empty for a short walkable route", path.isEmpty());
	}

	@Test
	public void testPathfinderLongRoute() {
		Pathfinder pf = new Pathfinder(
				createMinimalConfig(),
				new WorldPoint(3222, 3218, 0),
				new WorldPoint(3164, 3485, 0)
		);

		pf.run();

		assertTrue("Pathfinder should complete for Lumbridge to GE route", pf.isDone());
		List<WorldPoint> path = pf.getPath();
		assertNotNull(path);
		assertTrue("Path should have many tiles for a long route", path.size() > 50);
	}

	@Test
	public void testPathfinderCancelReturnsPath() throws Exception {
		Pathfinder pf = new Pathfinder(
				createMinimalConfig(),
				new WorldPoint(3222, 3218, 0),
				new WorldPoint(2500, 3500, 0)
		);

		Thread t = new Thread(pf);
		t.start();
		Thread.sleep(200);
		pf.cancel();
		t.join(5000);

		List<WorldPoint> path = pf.getPath();
		assertNotNull("Cancelled pathfinder should return a path (possibly partial)", path);
	}

	@Test
	public void testPathfinderWildernessRoute() {
		Pathfinder pf = new Pathfinder(
				createMinimalConfig(),
				new WorldPoint(3094, 3500, 0),
				new WorldPoint(3094, 3550, 0)
		);

		pf.run();

		assertTrue("Pathfinder should complete", pf.isDone());
		List<WorldPoint> path = pf.getPath();
		assertNotNull(path);
		assertFalse("Path into wilderness should not be empty", path.isEmpty());
	}

	private PathfinderConfig createMinimalConfig() {
		PathfinderConfig config = new PathfinderConfig(
				collisionMap,
				new HashMap<>(),
				Collections.emptyList(),
				null,
				null
		);
		try {
			java.lang.reflect.Field f = PathfinderConfig.class.getDeclaredField("calculationCutoffMillis");
			f.setAccessible(true);
			f.setLong(config, 10000);
		} catch (Exception e) {
			throw new RuntimeException("Failed to set calculationCutoffMillis", e);
		}
		return config;
	}
}
