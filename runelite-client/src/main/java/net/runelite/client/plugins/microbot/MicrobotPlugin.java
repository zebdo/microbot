package net.runelite.client.plugins.microbot;

import com.google.inject.Provides;
import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.qualityoflife.scripts.pouch.PouchOverlay;
import net.runelite.client.plugins.microbot.ui.MicrobotPluginConfigurationDescriptor;
import net.runelite.client.plugins.microbot.ui.MicrobotPluginListPanel;
import net.runelite.client.plugins.microbot.ui.MicrobotTopLevelConfigPanel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Gembag;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2RunePouch;
import net.runelite.client.plugins.microbot.util.overlay.GembagOverlay;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.reflection.Rs2Reflection;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = PluginDescriptor.Default + "Microbot",
	description = "Microbot",
	tags = {"main", "microbot", "parent"},
	alwaysOn = true,
	hidden = true,
	priority = true
)
@Slf4j
public class MicrobotPlugin extends Plugin
{

	@Inject
	private Provider<MicrobotPluginListPanel> pluginListPanelProvider;

	@Inject
	private Provider<MicrobotTopLevelConfigPanel> topLevelConfigPanelProvider;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private MicrobotConfig microbotConfig;

	private MicrobotTopLevelConfigPanel topLevelConfigPanel;

	private NavigationButton navButton;

	@Provides
	@Singleton
	MicrobotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MicrobotConfig.class);
	}

	@Inject
	private OverlayManager overlayManager;
	@Inject
	private MicrobotOverlay microbotOverlay;
	@Inject
	private GembagOverlay gembagOverlay;
	@Inject
	private PouchOverlay pouchOverlay;

	@Override
	protected void startUp() throws AWTException
	{
		Microbot.pauseAllScripts = false;

		MicrobotPluginListPanel pluginListPanel = pluginListPanelProvider.get();
		pluginListPanel.addFakePlugin(new MicrobotPluginConfigurationDescriptor(
			"Microbot", "Microbot client settings",
			new String[]{"client"},
			microbotConfig, configManager.getConfigDescriptor(microbotConfig)
		));
		pluginListPanel.rebuildPluginList();

		topLevelConfigPanel = topLevelConfigPanelProvider.get();

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "microbot_config_icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Community Plugins")
			.icon(icon)
			.priority(0)
			.panel(topLevelConfigPanel)
			.build();

		clientToolbar.addNavigation(navButton);

		new InputSelector(clientToolbar);

		Microbot.getPouchScript().startUp();

		if (overlayManager != null)
		{
			overlayManager.add(microbotOverlay);
			overlayManager.add(gembagOverlay);
			overlayManager.add(pouchOverlay);
		}
	}

	protected void shutDown()
	{
		overlayManager.remove(microbotOverlay);
		overlayManager.remove(gembagOverlay);
		overlayManager.remove(pouchOverlay);
		clientToolbar.removeNavigation(navButton);
	}


	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		Microbot.setIsGainingExp(true);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		Microbot.getPouchScript().onItemContainerChanged(event);
		if (event.getContainerId() == InventoryID.BANK.getId())
		{
			Rs2Bank.storeBankItemsInMemory(event);
		}
		else if (event.getContainerId() == InventoryID.INVENTORY.getId())
		{
			Rs2Inventory.storeInventoryItemsInMemory(event);
		}
		else if (event.getContainerId() == InventoryID.EQUIPMENT.getId())
		{
			Rs2Equipment.storeEquipmentItemsInMemory(event);
		}
		else
		{
			Rs2Shop.storeShopItemsInMemory(event, event.getContainerId());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			Microbot.setLoginTime(Instant.now());
			Rs2RunePouch.fullUpdate();
		}
		if (gameStateChanged.getGameState() == GameState.HOPPING || gameStateChanged.getGameState() == GameState.LOGIN_SCREEN || gameStateChanged.getGameState() == GameState.CONNECTION_LOST)
		{
			if (Rs2Bank.bankItems != null)
			{
				Rs2Bank.bankItems.clear();
			}
			Microbot.loggedIn = false;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		Rs2Player.handlePotionTimers(event);
		Rs2Player.handleTeleblockTimer(event);
		Rs2RunePouch.onVarbitChanged(event);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Rs2Player.handleAnimationChanged(event);
	}

	@Subscribe(priority = 999)
	private void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (Microbot.targetMenu != null && event.getType() != Microbot.targetMenu.getType().getId())
		{
			Microbot.getClient().getMenu().setMenuEntries(new MenuEntry[]{});
		}

		if (Microbot.targetMenu != null)
		{
			MenuEntry entry =
				Microbot.getClient().getMenu().createMenuEntry(-1)
					.setOption(Microbot.targetMenu.getOption())
					.setTarget(Microbot.targetMenu.getTarget())
					.setIdentifier(Microbot.targetMenu.getIdentifier())
					.setType(Microbot.targetMenu.getType())
					.setParam0(Microbot.targetMenu.getParam0())
					.setParam1(Microbot.targetMenu.getParam1())
					.setForceLeftClick(false);

			if (Microbot.targetMenu.getItemId() > 0)
			{
				try
				{
					Rs2Reflection.setItemId(entry, Microbot.targetMenu.getItemId());
				}
				catch (IllegalAccessException | InvocationTargetException e)
				{
					System.out.println(e.getMessage());
				}
			}
			Microbot.getClient().getMenu().setMenuEntries(new MenuEntry[]{entry});
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		Microbot.getPouchScript().onMenuOptionClicked(event);
		Rs2Gembag.onMenuOptionClicked(event);
		Microbot.targetMenu = null;
		System.out.println(event.getMenuEntry());
	}

	@Subscribe
	private void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.ENGINE && event.getMessage().equalsIgnoreCase("I can't reach that!"))
		{
			Microbot.cantReachTarget = true;
		}
		if (event.getType() == ChatMessageType.GAMEMESSAGE && event.getMessage().toLowerCase().contains("you can't log into a non-members"))
		{
			Microbot.cantHopWorld = true;
		}
		Microbot.getPouchScript().onChatMessage(event);
		Rs2Gembag.onChatMessage(event);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged ev)
	{
		if (ev.getKey().equals("displayPouchCounter"))
		{
			if (ev.getNewValue() == "true")
			{
				Microbot.getPouchScript().startUp();
			}
			else
			{
				Microbot.getPouchScript().shutdown();
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		Rs2RunePouch.onWidgetLoaded(event);
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		// Case 1: Hitsplat applied to the local player (indicates someone or something is attacking you)
		if (event.getActor().equals(Microbot.getClient().getLocalPlayer()))
		{
			if (!event.getHitsplat().isOthers())
			{
				Rs2Player.updateCombatTime();
			}
		}

		// Case 2: Hitsplat is applied to another player (indicates you are attacking another player)
		else if (event.getActor() instanceof Player)
		{
			if (event.getHitsplat().isMine())
			{
				Rs2Player.updateCombatTime();
			}
		}

		// Case 3: Hitsplat is applied to an NPC (indicates you are attacking an NPC)
		else if (event.getActor() instanceof NPC)
		{
			if (event.getHitsplat().isMine())
			{
				Rs2Player.updateCombatTime();
			}
		}
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked overlayMenuClicked)
	{
		OverlayMenuEntry overlayMenuEntry = overlayMenuClicked.getEntry();
		if (overlayMenuEntry.getMenuAction() == MenuAction.RUNELITE_OVERLAY_CONFIG)
		{
			Overlay overlay = overlayMenuClicked.getOverlay();
			Plugin plugin = overlay.getPlugin();
			if (plugin == null)
			{
				return;
			}

			// Expand config panel for plugin
			SwingUtilities.invokeLater(() ->
			{
				clientToolbar.openPanel(navButton);
				topLevelConfigPanel.openConfigurationPanel(plugin.getName());
			});
		}
	}
}
