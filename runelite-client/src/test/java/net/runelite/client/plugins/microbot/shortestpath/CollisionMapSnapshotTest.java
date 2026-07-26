package net.runelite.client.plugins.microbot.shortestpath;

import org.junit.Test;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CollisionMapSnapshotTest
{
	private static final String RESOURCE = "collision-map.zip";
	private static final String EXPECTED_SHA256 =
		"4cb2d04f84898fc2f90c055ab45a98d3960ce67f57c5ff8786ec3e4e450b3bdd";
	private static final int EXPECTED_REGION_COUNT = 2724;

	@Test
	public void collisionMapMatchesPinnedUpstreamSnapshot() throws Exception
	{
		try (InputStream stream = ShortestPathPlugin.class.getResourceAsStream(RESOURCE))
		{
			assertNotNull("collision map resource is missing", stream);
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] buffer = new byte[8192];
			int read;
			while ((read = stream.read(buffer)) >= 0)
			{
				digest.update(buffer, 0, read);
			}
			assertEquals(EXPECTED_SHA256, toHex(digest.digest()));
		}
	}

	@Test
	public void collisionMapContainsPinnedRegionCount() throws Exception
	{
		int entries = 0;
		InputStream stream = ShortestPathPlugin.class.getResourceAsStream(RESOURCE);
		assertNotNull("collision map resource is missing", stream);
		try (InputStream resource = stream; ZipInputStream zip = new ZipInputStream(resource))
		{
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null)
			{
				entries++;
			}
		}
		assertEquals(EXPECTED_REGION_COUNT, entries);
	}

	private static String toHex(byte[] bytes)
	{
		StringBuilder result = new StringBuilder(bytes.length * 2);
		for (byte value : bytes)
		{
			result.append(String.format("%02x", value & 0xff));
		}
		return result.toString();
	}
}
