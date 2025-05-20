package net.runelite.client.plugins.microbot.TaF.VolcanicAshMiner;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.TaFCat + "Volcanic Ash Miner",
        description = "Start either at the ash mine on Fossil Island or with a digsite pendant in your inventory. Have a pickaxe in your inventory or equipped.",
        tags = {"Volcanic", "ash", "mining", "ironman", "taf", "microbot"},
        enabledByDefault = false
)
public class VolcanicAshMinerPlugin extends Plugin {

    private Instant scriptStartTime;
    @Inject
    private VolcanicAshMinerConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private VolcanicAshMinerOverlay volcanicAshMinerOverlay;
    @Inject
    private VolcanicAshMinerScript volcanicAshMinerScript;

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    @Override
    protected void startUp() throws AWTException {
        scriptStartTime = Instant.now();
        if (overlayManager != null) {
            overlayManager.add(volcanicAshMinerOverlay);
        }
        volcanicAshMinerScript.run(config);
    }

    @Override
    protected void shutDown() {
        volcanicAshMinerScript.shutdown();
        overlayManager.remove(volcanicAshMinerOverlay);
    }

    @Provides
    VolcanicAshMinerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(VolcanicAshMinerConfig.class);
    }
}
