package net.runelite.client.plugins.microbot.SulphurNaguaAIO; // Passe den Paketnamen an

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.TaF.GemCrabKiller.GemCrabKillerScript;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = "Sulphur Nagua Fighter",
        description = "Kämpft automatisch gegen Sulphur Nagua.",
        tags = {"combat", "pvm", "nagua", "valamore"},
        enabledByDefault = false
)
@Slf4j
public class SulphurNaguaPlugin extends Plugin {
    @Inject
    SulphurNaguaScript sulphurNaguaScript;
    @Inject
    private SulphurNaguaConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private SulphurNaguaOverlay sulphurNaguaOverlay;
    private Instant scriptStartTime;
    private long startTotalExp;

    @Provides
    SulphurNaguaConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SulphurNaguaConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        scriptStartTime = Instant.now();
        startTotalExp = Microbot.getClient().getOverallExperience();
        if (overlayManager != null) {
            overlayManager.add(sulphurNaguaOverlay);
        }
        sulphurNaguaScript.run(config);
    }

    @Override
    protected void shutDown() {
        sulphurNaguaScript.shutdown();
        overlayManager.remove(sulphurNaguaOverlay);
        scriptStartTime = null;
        startTotalExp = 0;
        sulphurNaguaScript.totalNaguaKills = 0;
    }

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    public long getXpGained() {
        if (startTotalExp == 0) return 0;
        return Microbot.getClient().getOverallExperience() - startTotalExp;
    }

    public long getXpPerHour() {
        if (scriptStartTime == null) return 0;

        long secondsElapsed = java.time.Duration.between(scriptStartTime, Instant.now()).getSeconds();
        if (secondsElapsed <= 0) return 0;

        return (getXpGained() * 3600L) / secondsElapsed;
    }
}

