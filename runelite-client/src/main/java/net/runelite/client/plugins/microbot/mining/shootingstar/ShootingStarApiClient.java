package net.runelite.client.plugins.microbot.mining.shootingstar;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.microbot.mining.shootingstar.enums.ShootingStarLocation;
import net.runelite.client.plugins.microbot.mining.shootingstar.enums.ShootingStarProvider;
import net.runelite.client.plugins.microbot.mining.shootingstar.model.OSRSVaultStarModel;
import net.runelite.client.plugins.microbot.mining.shootingstar.model.Star;
import net.runelite.client.plugins.microbot.mining.shootingstar.model.ZeroSevenStarModel;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Singleton
public class ShootingStarApiClient
{
	private final OkHttpClient okHttpClient = new OkHttpClient();

	private final Client client;
	private final WorldService worldService;
	private final String zeroSevenEndpoint;
	private final String osrsVaultEndpoint;

	private final ZoneId utcZoneId = ZoneId.of("UTC");

	@Inject
	public ShootingStarApiClient(Client client, WorldService worldService)
	{
		this.client = client;
		this.worldService = worldService;
		Properties properties = loadProperties();
		this.zeroSevenEndpoint = properties.getProperty("microbot.shootingstar.zeroseven");
		this.osrsVaultEndpoint = properties.getProperty("microbot.shootingstar.osrsvault");
	}

	String fetch(ShootingStarProvider provider)
	{
		String endpoint = getEndpoint(provider);

		if (endpoint == null || endpoint.isEmpty())
		{
			log.warn("Shooting star API endpoint is not configured or is empty");
			return "";
		}

		Request httpRequest = new Request.Builder()
			.url(endpoint)
			.get()
			.build();

		String jsonResponse;

		try (Response response = okHttpClient.newCall(httpRequest).execute())
		{
			if (!response.isSuccessful())
			{
				log.warn("Failed to fetch shooting star data: {}", response.message());
				return "";
			}
			jsonResponse = response.body() != null ? response.body().string() : null;
		}
		catch (Exception e)
		{
			log.trace("Error fetching shooting star data", e);
			return "";
		}

		if (jsonResponse == null || jsonResponse.isEmpty())
		{
			log.warn("Received empty response from shooting star API endpoint");
			return "";
		}

		return jsonResponse;
	}

	List<ZeroSevenStarModel> fromZeroSeven(String jsonResponse)
	{
		Gson gson = new Gson();
		Type listType = new TypeToken<List<ZeroSevenStarModel>>() {}.getType();

		List<ZeroSevenStarModel> deserializedStarData = Collections.emptyList();
		try
		{
			List<ZeroSevenStarModel> result = gson.fromJson(jsonResponse, listType);
			if (result != null)
			{
				deserializedStarData = result;
			}
		}
		catch (JsonSyntaxException e)
		{
			log.trace("Failed to parse response: {}", jsonResponse, e);
		}
		return deserializedStarData;
	}

	List<OSRSVaultStarModel> fromOSRSVault(String jsonResponse)
	{
		Gson gson = new Gson();
		Type listType = new TypeToken<List<OSRSVaultStarModel>>() {}.getType();

		List<OSRSVaultStarModel> deserializedStarData = Collections.emptyList();
		try
		{
			List<OSRSVaultStarModel> result = gson.fromJson(jsonResponse, listType);
			if (result != null)
			{
				deserializedStarData = result;
			}
		}
		catch (JsonSyntaxException e)
		{
			log.trace("Failed to parse response: {}", jsonResponse, e);
		}
		return deserializedStarData;
	}

	public List<Star> getStarData(ShootingStarProvider provider)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			log.warn("GameState is not {}", GameState.LOGGED_IN.name());
			return Collections.emptyList();
		}

		if (worldService.getWorlds() == null)
		{
			log.warn("Worlds are not available");
			return Collections.emptyList();
		}

		ZonedDateTime now = ZonedDateTime.now(utcZoneId);

		String response = fetch(provider);
		if (response.isEmpty() || response.equals("[]"))
		{
			return Collections.emptyList();
		}

		List<Star> starData = (Objects.equals(provider, ShootingStarProvider.ZERO_SEVEN)) ?
			new ArrayList<>(fromZeroSeven(response)) :
			new ArrayList<>(fromOSRSVault(response));

		boolean inSeasonalWorld = client.getWorldType().contains(WorldType.SEASONAL);

		// Remove stars that are older than 3 minutes
		starData.removeIf(s -> s.getEndsAt() < now.minusMinutes(3).toInstant().toEpochMilli());

		// Log & remove stars that have no matching location key or raw location
		starData.removeIf(s -> {
			ShootingStarLocation location = findLocation(s.getLocationKey() != null ? s.getLocationKey().toString() : "", s.getRawLocation());
			boolean toRemove = location == null;
			if (toRemove)
			{
				log.debug("No matching ShootingStarLocation found for key: {} and raw location: {}", s.getLocationKey(), s.getRawLocation());
			}
			else
			{
				s.setShootingStarLocation(location);
			}
			return toRemove;
		});

		// Populate other fields for each star
		starData.forEach(s -> {
			s.setWorldObject(getWorld(s.getWorld()));
		});

		// Remove stars based on seasonal world filtering
		starData.removeIf(s ->
			s.isGameModeWorld() || (inSeasonalWorld && !s.isInSeasonalWorld()) || (!inSeasonalWorld && s.isInSeasonalWorld())
		);

		return starData;
	}

	private ShootingStarLocation findLocation(String locationKey, String rawLocation)
	{
		return Arrays.stream(ShootingStarLocation.values())
			.filter(location ->
				locationKey.equalsIgnoreCase(location.name()) ||
					rawLocation.equalsIgnoreCase(location.getRawLocationName()) ||
					locationKey.equalsIgnoreCase(location.getShortLocationName()))
			.findFirst()
			.orElse(null);
	}

	private World getWorld(int worldId)
	{
		assert worldService.getWorlds() != null : "World Result should not be null";

		WorldResult worldResult = worldService.getWorlds();
		return worldResult.findWorld(worldId);
	}

	private String getEndpoint(ShootingStarProvider provider)
	{
		if (provider == ShootingStarProvider.ZERO_SEVEN)
		{
			return zeroSevenEndpoint;
		}
		else if (provider == ShootingStarProvider.OSRS_VAULT)
		{
			return osrsVaultEndpoint;
		}
		return "";
	}

	private Properties loadProperties()
	{
		Properties properties = new Properties();
		try (InputStream input = ShootingStarApiClient.class.getResourceAsStream("shootingstar.properties"))
		{
			if (input != null)
			{
				properties.load(input);
			}
		}
		catch (Exception e)
		{
			log.trace("Unable to parse shootingstar.properties", e);
		}
		return properties;
	}
}
