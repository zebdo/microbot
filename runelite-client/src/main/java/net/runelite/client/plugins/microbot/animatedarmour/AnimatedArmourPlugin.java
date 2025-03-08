package net.runelite.client.plugins.microbot.animatedarmour;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.See1Duck + "Animated Armour Killer",
        description = "Builds, Kills, and loots animated armour and warrior guild tokens",
        tags = {"example", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class AnimatedArmourPlugin extends Plugin {
    @Inject
    private AnimatedArmourConfig config;
    @Provides
    AnimatedArmourConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AnimatedArmourConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private AnimatedArmourOverlay animatedArmourOverlay;

    @Inject
    AnimatedArmourScript animatedArmourScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(animatedArmourOverlay);
        }
        animatedArmourScript.run(config);
    }

    protected void shutDown() {
        animatedArmourScript.shutdown();
        overlayManager.remove(animatedArmourOverlay);
    }

}
