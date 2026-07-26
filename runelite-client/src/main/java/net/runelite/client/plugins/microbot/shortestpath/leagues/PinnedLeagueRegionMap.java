package net.runelite.client.plugins.microbot.shortestpath.leagues;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.shortestpath.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Map-region classifier vendored with the pinned upstream transport snapshot.
 */
@Slf4j
public final class PinnedLeagueRegionMap
{
	private static final String RESOURCE_PATH = "upstream/leagues/regions.tsv";
	private static volatile Map<Integer, PinnedLeagueRegion> regionsById;

	private PinnedLeagueRegionMap()
	{
	}

	public static PinnedLeagueRegion getRegion(WorldPoint point)
	{
		if (point == null)
		{
			return PinnedLeagueRegion.NEUTRAL;
		}
		return getRegion(point.getRegionID());
	}

	public static PinnedLeagueRegion getRegion(int regionId)
	{
		Map<Integer, PinnedLeagueRegion> snapshot = regionsById;
		if (snapshot == null)
		{
			synchronized (PinnedLeagueRegionMap.class)
			{
				snapshot = regionsById;
				if (snapshot == null)
				{
					snapshot = load();
					regionsById = snapshot;
				}
			}
		}
		return snapshot.getOrDefault(regionId, PinnedLeagueRegion.NEUTRAL);
	}

	public static PinnedLeagueRegion parseOverride(String value)
	{
		if (value == null || value.isBlank())
		{
			return null;
		}
		try
		{
			return PinnedLeagueRegion.valueOf(value.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex)
		{
			log.warn("Unknown pinned shortest-path league region override '{}'", value);
			return null;
		}
	}

	static Map<Integer, PinnedLeagueRegion> parse(String body)
	{
		Map<Integer, PinnedLeagueRegion> result = new HashMap<>();
		if (body == null)
		{
			return result;
		}
		for (String rawLine : body.split("\\R"))
		{
			String line = rawLine.trim();
			if (line.isEmpty() || line.startsWith("#"))
			{
				continue;
			}
			String[] fields = line.split("\\s+", 2);
			if (fields.length != 2)
			{
				continue;
			}
			try
			{
				result.put(Integer.parseInt(fields[0]), PinnedLeagueRegion.valueOf(fields[1]));
			}
			catch (IllegalArgumentException ex)
			{
				log.warn("Skipping malformed pinned shortest-path league region row '{}'", rawLine);
			}
		}
		return result;
	}

	private static Map<Integer, PinnedLeagueRegion> load()
	{
		try (InputStream input = ShortestPathPlugin.class.getResourceAsStream(RESOURCE_PATH))
		{
			if (input == null)
			{
				log.warn("Pinned shortest-path league region resource is missing");
				return Collections.emptyMap();
			}
			return Collections.unmodifiableMap(parse(new String(
				Util.readAllBytes(Objects.requireNonNull(input)),
				StandardCharsets.UTF_8)));
		}
		catch (IOException ex)
		{
			log.warn("Unable to load pinned shortest-path league region map", ex);
			return Collections.emptyMap();
		}
	}
}
