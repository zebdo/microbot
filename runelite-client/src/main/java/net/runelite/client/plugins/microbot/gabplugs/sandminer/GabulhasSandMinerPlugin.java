package net.runelite.client.plugins.microbot.gabplugs.sandminer;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.Gabulhas + "Sandstone miner",
        description = "",
        tags = {"GabulhasSandMiner", "Gabulhas"},
        enabledByDefault = false
)
@Slf4j
public class GabulhasSandMinerPlugin extends Plugin {
    @Inject
    GabulhasSandMinerScript gabulhasSandMinerScript;
    @Inject
    private GabulhasSandMinerConfig config;
    @Inject
    private OverlayManager overlayManager;
    public Instant scriptStartTime;
    @Inject
    private GabulhasSandMinerOverlay gabulhasSandMinerOverlay;
    public int rocksMined = 0;

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }
    @Provides
    GabulhasSandMinerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GabulhasSandMinerConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        scriptStartTime = Instant.now();
        if (overlayManager != null) {
            overlayManager.add(gabulhasSandMinerOverlay);
        }
        gabulhasSandMinerScript.run(config);
        GabulhasSandMinerInfo.botStatus = config.STARTINGSTATE();
    }

    protected void shutDown() {
        gabulhasSandMinerScript.shutdown();
        overlayManager.remove(gabulhasSandMinerOverlay);
        scriptStartTime = null;
        rocksMined = 0;
    }

    @Subscribe
    public void onStatChanged(StatChanged statChanged)
    {
        final Skill skill = statChanged.getSkill();
        if (skill == Skill.MINING) {
            rocksMined++;
        }
    }
}
