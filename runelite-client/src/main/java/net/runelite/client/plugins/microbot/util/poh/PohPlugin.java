package net.runelite.client.plugins.microbot.util.poh;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.poh.data.*;
import net.runelite.client.plugins.microbot.util.poh.ui.MasterPanel;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * This plugin is designed to track and manage teleport options available in a player's
 * house including mounted objects, portals, nexus teleports, and jewellery box options.
 *
 * The plugin creates a new panel available directly from the toolbar for managing these teleports.
 * It dynamically retrieves configurations to build an p-to-date list of available transport options.
 *
 * Core responsibilities of this plugin include:
 * - Initializing the plugin and adding a navigation button to the PoH Panel for user access.
 * - Dynamically fetching available Player-Owned House transport options based on the user's configuration.
 */
@PluginDescriptor(
        name = "Player-Owned-House",
        description = "PoH Teleport Tracker",
        tags = {"main", "microbot", "poh", "player-owned-house", "teleports", "webwalker"},
        alwaysOn = true,
        hidden = true
)
@Slf4j
public class PohPlugin extends Plugin {

    private NavigationButton navButton;
    @Inject
    private PohConfig config;

    @Provides
    PohConfig providePohConfig(ConfigManager configManager) {
        return configManager.getConfig(PohConfig.class);
    }

    @Inject
    private ClientToolbar clientToolbar;


    @Override
    protected void startUp() throws AWTException {
        final MasterPanel panel = new MasterPanel(config);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "poh.png");
        navButton = NavigationButton.builder()
                .tooltip("Player-owned-house")
                .icon(icon)
                .priority(1)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);
    }


    /**
     * Retrieves a list of available Player-Owned House (PoH) transports based on the current PohConfig.
     * This includes transports from mounted glory, mounted digsite, portals, nexus teleports, and jewellery box.
     *
     * @return a list of {@link PohTransport} objects representing the available PoH transport options.
     */
    public static List<PohTransport> getAvailableTransports() {
        List<PohTransport> transports = new ArrayList<>();
        boolean mountedGlory = Microbot.getConfigManager().getConfiguration(PohConfig.CONFIG_GROUP, PohConfig.MOUNTED_GLORY, Boolean.class);
        boolean mountedDigsite = Microbot.getConfigManager().getConfiguration(PohConfig.CONFIG_GROUP, PohConfig.MOUNTED_DIGSITE, Boolean.class);
        String portals = Microbot.getConfigManager().getConfiguration(PohConfig.CONFIG_GROUP, PohConfig.PORTALS);
        String nexusTeleports = Microbot.getConfigManager().getConfiguration(PohConfig.CONFIG_GROUP, PohConfig.NEXUS);
        JewelleryBox jewelleryBox = JewelleryBox.valueOf(Microbot.getConfigManager().getConfiguration(PohConfig.CONFIG_GROUP, PohConfig.JEWELLERY_BOX));
        if (mountedGlory) {
            transports.addAll(MountedGlory.getTransports());
        }
        if (mountedDigsite) {
            transports.addAll(MountedDigsite.getTransports());
        }
        if (portals != null && !portals.isEmpty()) {
            for (String portal : portals.split(",")) {
                PohPortal pohPortal = PohPortal.valueOf(portal);
                transports.add(pohPortal.getPohTransport());
            }
        }
        if (nexusTeleports != null && !nexusTeleports.isEmpty()) {
            for (String nt : nexusTeleports.split(",")) {
                NexusTeleport nexusTeleport = NexusTeleport.valueOf(nt);
                transports.add(nexusTeleport.getPohTransport());
            }
        }
        if (jewelleryBox != JewelleryBox.NONE) {
            transports.addAll(jewelleryBox.getTransports());
        }

        return transports;
    }
}
