package net.runelite.client.plugins.microbot.shortestpath.leagues;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PinnedLeagueRegionMapTest
{
	@Test
	public void loadsPinnedRegionGeometry()
	{
		assertEquals(PinnedLeagueRegion.VARLAMORE,
			PinnedLeagueRegionMap.getRegion(worldPointInRegion(4137)));
		assertEquals(PinnedLeagueRegion.KOUREND,
			PinnedLeagueRegionMap.getRegion(worldPointInRegion(4149)));
		assertEquals(PinnedLeagueRegion.MISTHALIN,
			PinnedLeagueRegionMap.getRegion(worldPointInRegion(7515)));
	}

	@Test
	public void parsesTransportRegionOverride()
	{
		assertEquals(PinnedLeagueRegion.ASGARNIA,
			PinnedLeagueRegionMap.parseOverride("Asgarnia"));
	}

	private static WorldPoint worldPointInRegion(int regionId)
	{
		int regionX = regionId >> 8;
		int regionY = regionId & 0xff;
		return new WorldPoint(regionX << 6, regionY << 6, 0);
	}
}
