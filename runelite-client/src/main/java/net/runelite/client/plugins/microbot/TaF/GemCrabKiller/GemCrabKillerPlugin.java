package net.runelite.client.plugins.microbot.TaF.GemCrabKiller;

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

@PluginDescriptor(
        name = PluginDescriptor.TaFCat + "Gem Crab Killer",
        description = "Automatically kills the Gem Crab boss",
        tags = {"TaF", "crab", "combat", "training", "gem"},
        enabledByDefault = false
)
@Slf4j
public class GemCrabKillerPlugin extends Plugin {
    @Inject
    GemCrabKillerScript gemCrabKillerScript;
    @Inject
    private GemCrabKillerConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private GemCrabKillerOverlay gemCrabKillerOverlay;
    private Instant scriptStartTime;
    private long startTotalExp;

    @Provides
    GemCrabKillerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GemCrabKillerConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        scriptStartTime = Instant.now();
        startTotalExp = Microbot.getClient().getOverallExperience();
        if (overlayManager != null) {
            overlayManager.add(gemCrabKillerOverlay);
        }
        gemCrabKillerScript.run(config);
    }

    protected void shutDown() {
        gemCrabKillerScript.shutdown();
        overlayManager.remove(gemCrabKillerOverlay);
        scriptStartTime = null;
        startTotalExp = 0;
        gemCrabKillerScript.totalCrabKills = 0;
    }

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    public long getXpGained() {
        return Microbot.getClient().getOverallExperience() - startTotalExp;
    }

    public long getXpPerHour() {
        if (scriptStartTime == null) {
            return 0;
        }

        long secondsElapsed = java.time.Duration.between(scriptStartTime, Instant.now()).getSeconds();
        if (secondsElapsed <= 0) {
            return 0;
        }

        // Calculate xp per hour
        return (int) ((getXpGained() * 3600L) / secondsElapsed);
    }
}
