package net.runelite.client.plugins.microbot.mining.shootingstar;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.microbot.mining.shootingstar.enums.ShootingStarLocation;
import net.runelite.client.plugins.microbot.mining.shootingstar.model.Star;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

@PluginDescriptor(
	name = PluginDescriptor.GZ + "ShootingStar",
	description = "Finds & Travels to shooting stars",
	tags = {"mining", "microbot", "skilling", "star", "shooting"},
	enabledByDefault = false
)
@Slf4j
public class ShootingStarPlugin extends Plugin
{

	public static String version = "1.4.0";

	@Getter
	public List<Star> starList = new ArrayList<>();

	@Inject
	private ShootingStarScript shootingStarScript;

	@Inject
	private ShootingStarApiClient shootingStarApiClient;

	@Inject
	private ShootingStarConfig config;

	@Provides
	ShootingStarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ShootingStarConfig.class);
	}

	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ShootingStarOverlay shootingStarOverlay;

	@Inject
	private ClientToolbar clientToolbar;

	private NavigationButton navButton;
	private ShootingStarPanel panel;

	@Getter
	@Setter
	public int totalStarsMined = 0;
	private final AtomicInteger apiTickCounter = new AtomicInteger(0);
	private int lastWorld = -1;
	private final int UPDATE_INTERVAL = 3;
	private final ZoneId utcZoneId = ZoneId.of("UTC");
	@Getter
	private boolean displayAsMinutes;
	@Getter
	private boolean hideMembersWorlds;
	@Getter
	private boolean hideF2PWorlds;

	private Set<String> blacklistedLocations = new HashSet<>();

	@Override
	protected void startUp() throws AWTException
	{
		displayAsMinutes = config.isDisplayAsMinutes();
		hideMembersWorlds = !Rs2Player.isInMemberWorld();
		hideF2PWorlds = Rs2Player.isInMemberWorld();

		loadBlacklistedLocations();
		fetchStars();
		createPanel();
		updatePanelList(true);

		toggleOverlay(config.isHideOverlay());
		shootingStarScript.run();
	}

	protected void shutDown()
	{
		shootingStarScript.shutdown();
		removePanel();
		starList.clear();
		lastWorld = -1;
		apiTickCounter.set(0);
		overlayManager.remove(shootingStarOverlay);
	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged event)
	{
		if (!event.getGroup().equals(ShootingStarConfig.configGroup))
		{
			return;
		}

		if (event.getKey().equals(ShootingStarConfig.blacklistedLocations))
		{
			loadBlacklistedLocations();
			updateHiddenStars();
			updatePanelList(true);
		}

		if (event.getKey().equals(ShootingStarConfig.displayAsMinutes))
		{
			displayAsMinutes = config.isDisplayAsMinutes();
			updatePanelList(false);
		}

		if (event.getKey().equals(ShootingStarConfig.hideWildernessLocations))
		{
			updateHiddenStars();
			updatePanelList(true);
		}

		if (event.getKey().equals(ShootingStarConfig.hideOverlay))
		{
			toggleOverlay(config.isHideOverlay());
		}
	}

	@Schedule(
		period = 1,
		unit = ChronoUnit.MINUTES,
		asynchronous = true
	)
	public void tick() {
		checkDepletedStars();

		if (apiTickCounter.get() >= UPDATE_INTERVAL)
		{
			fetchStars();
			apiTickCounter.set(0);
		}

		apiTickCounter.incrementAndGet();

		if (config.useBreakAtBank() && !isBreakHandlerEnabled())
		{
			log.warn("Break Handler is not enabled to utilize the useBreakAtBank feature, enabling now..");
			enableBreakHandler();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			if (lastWorld == -1)
			{
				lastWorld = Microbot.getClient().getWorld();
				fetchStars();
			}
			else
			{
				int currentWorld = Microbot.getClient().getWorld();

				if (currentWorld != lastWorld)
				{
					lastWorld = currentWorld;
					updatePanelList(true);
				}
			}
		}
	}

	public void fetchStars()
	{
		List<Star> latestStars = shootingStarApiClient.getStarData();

		for (Star star : latestStars) {
			// Find oldStar inside starList
			Star oldStar = starList.stream()
				.filter(star::equals)
				.findFirst()
				.orElse(null);

			// If there is an oldStar in the same world & location
			if (oldStar != null)
			{
				oldStar.setEndsAt(star.getEndsAt());
				oldStar.setTier(star.getTier());
				oldStar.setObjectID(oldStar.getObjectIDBasedOnTier());
				oldStar.setMiningLevel(oldStar.getRequiredMiningLevel());
				continue;
			}

			// If oldStar not found, add new star into the list
			starList.add(star);
		}

		updateHiddenStars();
		updatePanelList(true);
	}

	private void checkDepletedStars()
	{
		List<Star> stars = new ArrayList<>(starList);
		ZonedDateTime now = ZonedDateTime.now(utcZoneId);
		boolean fullUpdate = false;

		for (Star star : stars)
		{
			if (star.getEndsAt() < now.minusMinutes(UPDATE_INTERVAL).toInstant().toEpochMilli())
			{
				removeStar(star);
				fullUpdate = true;
			}
		}

		updatePanelList(fullUpdate);
	}

	public void removeStar(Star star)
	{
		if (star.equals(getSelectedStar()))
		{
			star.setSelected(false);
		}
		starList.remove(star);
	}

	public void updateSelectedStar(Star star)
	{
		Star oldStar = getSelectedStar();
		if (oldStar == null)
		{
			star.setSelected(!star.isSelected());
			return;
		}
		else if (!oldStar.equals(star))
		{
			oldStar.setSelected(false);
			star.setSelected(!star.isSelected());
			return;
		}

		oldStar.setTier(star.getTierBasedOnObjectID());
		oldStar.setMiningLevel(star.getRequiredMiningLevel());
	}

	public void updateHiddenStars()
	{
		starList.forEach(star -> {
			boolean hide = hideMembersWorlds && star.isMemberWorld()
				|| (hideF2PWorlds && star.isF2PWorld())
				|| (config.isHideWildernessLocations() && star.isInWilderness())
				|| blacklistedLocations.contains(star.getShootingStarLocation().getLocationName());
			star.setHidden(hide);
		});
	}

	public Star getSelectedStar()
	{
		return starList.stream().filter(Star::isSelected).findFirst().orElse(null);
	}

	private void createPanel()
	{
		if (panel == null)
		{
			panel = new ShootingStarPanel(this);
			final BufferedImage icon = ImageUtil.loadImageResource(ShootingStarPlugin.class, "icon.png");

			navButton = NavigationButton.builder()
				.tooltip("Shooting Stars")
				.icon(icon)
				.priority(7)
				.panel(panel)
				.build();
			clientToolbar.addNavigation(navButton);
		}
	}

	private void removePanel()
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
	}

	public void updatePanelList(boolean fullUpdate)
	{
		List<Star> stars = new ArrayList<>(starList);

		if (fullUpdate)
		{
			SwingUtilities.invokeLater(() -> panel.updateList(stars));
		}
		else
		{
			SwingUtilities.invokeLater(() -> panel.refreshList(stars));
		}
	}

	private void toggleOverlay(boolean hideOverlay)
	{
		if (overlayManager != null)
		{
			boolean hasOverlay = overlayManager.anyMatch(ov -> ov.getName().equalsIgnoreCase(ShootingStarOverlay.class.getSimpleName()));

			if (hideOverlay)
			{
				if (!hasOverlay)
				{
					return;
				}

				overlayManager.remove(shootingStarOverlay);
			}
			else
			{
				if (hasOverlay)
				{
					return;
				}

				overlayManager.add(shootingStarOverlay);
			}
		}
	}

	public Star getClosestHighestTierStar()
	{
		// Get the highest tier available
		int highestTier = starList.stream()
			.filter(s -> !s.isHidden() && s.hasRequirements())
			.mapToInt(Star::getTier)
			.max()
			.orElse(-1);  // Return -1 if no star meets the requirements

		// If no star meets the requirements, return null
		if (highestTier == -1)
		{
			return null;
		}

		int minTier = Math.max(1, highestTier - 2); // The lowest tier to consider (at least 1)
		int maxTier = Math.min(9, highestTier + 1); // The highest tier to consider (up to 9)

		List<Star> accessibleStars = starList.stream()
			.filter(s -> !s.isHidden() && s.hasRequirements())
			.filter(s -> s.getTier() >= minTier && s.getTier() <= maxTier)
			.sorted(Comparator.comparingInt(Star::getTier).reversed())
			.distinct()
			.collect(Collectors.toList());

		Set<WorldPoint> accessibleStarPoints = accessibleStars.stream()
			.map(s -> s.getShootingStarLocation().getWorldPoint())
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		if (accessibleStarPoints.isEmpty())
		{
			return null; // No accessible stars found
		}

		Pathfinder pathfinder = new Pathfinder(ShortestPathPlugin.getPathfinderConfig(), Microbot.getClient().getLocalPlayer().getWorldLocation(), accessibleStarPoints);
		pathfinder.run();
		List<WorldPoint> path = pathfinder.getPath();

		if (path.isEmpty())
		{
			return null; // No path found to any accessible star
		}

		WorldPoint finalTile = path.get(path.size() - 1);
		WorldArea finalTileArea = new WorldArea(finalTile, 3, 3);

		Map<Star, WorldArea> starAreas = accessibleStars.stream()
			.collect(Collectors.toMap(
				star -> star,
				star -> {
					WorldPoint wp = star.getShootingStarLocation().getWorldPoint();
					return new WorldArea(wp, 3, 3);
				}
			));

		Optional<Star> intersectingStar = starAreas.entrySet().stream()
			.filter(entry -> entry.getValue().intersectsWith2D(finalTileArea))
			.map(Map.Entry::getKey)
			.findFirst();

		if (intersectingStar.isEmpty())
		{
			return null;
		}
		else
		{
			Star closestStar = intersectingStar.get();
			closestStar.setSelected(true);
			return closestStar;
		}
	}

	private void enableBreakHandler()
	{
		Plugin breakHandlerPlugin = Microbot.getPluginManager().getPlugins().stream()
			.filter(x -> x.getClass().getName().equals(BreakHandlerPlugin.class.getName()))
			.findFirst()
			.orElse(null);

		Microbot.startPlugin(breakHandlerPlugin);
	}

	public boolean isBreakHandlerEnabled()
	{
		return Microbot.isPluginEnabled(BreakHandlerPlugin.class);
	}

	public Duration getScriptRuntime()
	{
		return shootingStarScript.getRunTime();
	}

	private void loadBlacklistedLocations()
	{
		String raw = Microbot.getConfigManager().getConfiguration(ShootingStarConfig.configGroup, ShootingStarConfig.blacklistedLocations);
		if (raw == null || raw.isEmpty())
		{
			blacklistedLocations.clear();
			return;
		}
		blacklistedLocations = Arrays.stream(raw.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toSet());
	}

	public void addLocationToBlacklist(String locationName)
	{
		blacklistedLocations.add(locationName);
		String joined = String.join(",", blacklistedLocations);
		Microbot.getConfigManager().setConfiguration(ShootingStarConfig.configGroup, ShootingStarConfig.blacklistedLocations, joined);
		updateHiddenStars();
		updatePanelList(true);
	}

	public void clearBlacklistedLocations()
	{
		blacklistedLocations.clear();
		Microbot.getConfigManager().setConfiguration(ShootingStarConfig.configGroup, ShootingStarConfig.blacklistedLocations, "");
		updateHiddenStars();
		updatePanelList(true);
	}
}
