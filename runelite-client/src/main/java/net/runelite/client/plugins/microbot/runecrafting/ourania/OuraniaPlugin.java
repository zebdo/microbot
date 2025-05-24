package net.runelite.client.plugins.microbot.runecrafting.ourania;

import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.AWTException;
import java.time.Instant;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ItemID;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = PluginDescriptor.GMason + "Ourania Altar",
	description = "Microbot Ourania Altar plugin",
	tags = {"runecrafting", "microbot", "skilling"},
	enabledByDefault = false
)
public class OuraniaPlugin extends Plugin
{

	public static String version = "1.3.0";
	@Getter
	public Instant startTime;
	@Inject
	private OuraniaConfig config;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private OuraniaOverlay ouraniaOverlay;
	@Inject
	private OuraniaScript ouraniaScript;
	@Getter
	private boolean ranOutOfAutoPay = false;
	@Getter
	private int profit;

	@Provides
	OuraniaConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OuraniaConfig.class);
	}

	@Override
	protected void startUp() throws AWTException
	{
		startTime = Instant.now();

		if (overlayManager != null)
		{
			overlayManager.add(ouraniaOverlay);
		}

		ouraniaScript.run();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(ouraniaOverlay);
		ouraniaScript.shutdown();
		ranOutOfAutoPay = false;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (event.getMessage().toLowerCase().contains("you could not afford to pay with your quick payment"))
		{
			ranOutOfAutoPay = true;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(OuraniaConfig.configGroup))
		{
			return;
		}

		if (event.getKey().equals(OuraniaConfig.toggleOverlay))
		{
			toggleOverlay(config.toggleOverlay());
		}
	}

	public boolean isBreakHandlerEnabled()
	{
		return Microbot.isPluginEnabled(BreakHandlerPlugin.class);
	}

	public void calcuateProfit()
	{
		int teleportCost = Rs2GrandExchange.getPrice(ItemID.LAW_RUNE) + (Rs2GrandExchange.getPrice(ItemID.ASTRAL_RUNE) * 2);
		int runesCrafted = Rs2Inventory.items().stream()
			.filter(rs2Item -> rs2Item.getName().toLowerCase().contains("rune") && !rs2Item.getName().toLowerCase().contains("rune pouch"))
			.mapToInt(rs2Item -> Rs2GrandExchange.getPrice(rs2Item.getId()))
			.sum();
		int profitFromRun = teleportCost - runesCrafted;

		profit += profitFromRun;
	}

	private void toggleOverlay(boolean hideOverlay)
	{
		if (overlayManager != null)
		{
			boolean hasOverlay = overlayManager.anyMatch(ov -> ov.getName().equalsIgnoreCase(OuraniaOverlay.class.getSimpleName()));

			if (hideOverlay)
			{
				if (!hasOverlay)
				{
					return;
				}

				overlayManager.remove(ouraniaOverlay);
			}
			else
			{
				if (hasOverlay)
				{
					return;
				}

				overlayManager.add(ouraniaOverlay);
			}
		}
	}
}
