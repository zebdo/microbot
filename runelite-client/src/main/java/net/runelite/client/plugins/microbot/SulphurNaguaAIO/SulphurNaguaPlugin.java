package net.runelite.client.plugins.microbot.SulphurNaguaAIO; // Adjust the package name

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

/**
 * The main plugin class for the Sulphur Nagua Fighter script.
 * It handles the plugin lifecycle (startup, shutdown) and provides data to the overlay.
 */
@PluginDescriptor(
        name = "Sulphur Nagua Fighter",
        description = "Automatically fights Sulphur Naguas.",
        tags = {"combat", "pvm", "nagua", "valamore"},
        enabledByDefault = false
)
@Slf4j
public class SulphurNaguaPlugin extends Plugin {
    // --- Injected dependencies ---
    @Inject
    SulphurNaguaScript sulphurNaguaScript;
    @Inject
    private SulphurNaguaConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private SulphurNaguaOverlay sulphurNaguaOverlay;

    // --- State variables ---
    private Instant scriptStartTime;
    private long startTotalExp;

    /**
     * Provides the configuration for the plugin.
     * @param configManager The RuneLite config manager.
     * @return The SulphurNaguaConfig instance.
     */
    @Provides
    SulphurNaguaConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SulphurNaguaConfig.class);
    }

    /**
     * Called when the plugin is started.
     * Initializes timers, XP tracking, adds the overlay, and starts the script.
     */
    @Override
    protected void startUp() throws AWTException {
        scriptStartTime = Instant.now();
        startTotalExp = Microbot.getClient().getOverallExperience();
        if (overlayManager != null) {
            overlayManager.add(sulphurNaguaOverlay);
        }
        sulphurNaguaScript.run(config);
    }

    /**
     * Called when the plugin is stopped.
     * Shuts down the script, removes the overlay, and resets tracking variables.
     */
    @Override
    protected void shutDown() {
        sulphurNaguaScript.shutdown();
        overlayManager.remove(sulphurNaguaOverlay);
        scriptStartTime = null;
        startTotalExp = 0;
        sulphurNaguaScript.totalNaguaKills = 0; // Reset kill count
    }

    /**
     * Calculates the total time the script has been running.
     * @return A formatted string of the runtime (e.g., "01:23:45").
     */
    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    /**
     * Calculates the total experience gained since the script started.
     * @return The total XP gained.
     */
    public long getXpGained() {
        if (startTotalExp == 0) return 0;
        return Microbot.getClient().getOverallExperience() - startTotalExp;
    }

    /**
     * Calculates the experience gained per hour.
     * @return The XP per hour.
     */
    public long getXpPerHour() {
        if (scriptStartTime == null) return 0;

        long secondsElapsed = java.time.Duration.between(scriptStartTime, Instant.now()).getSeconds();
        if (secondsElapsed <= 0) return 0;

        // Formula: (XP Gained * Seconds in an Hour) / Seconds Elapsed
        return (getXpGained() * 3600L) / secondsElapsed;
    }
}