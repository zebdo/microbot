package net.runelite.client.plugins.microbot.runecrafting.runebodycrafter;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.DrDeath + "Body Rune Crafter",
        description = "Body Rune Crafter Plugin",
        tags = {"Body Rune Crafter", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class RuneBodyCrafterPlugin extends Plugin {
    @Inject
    private RuneBodyCrafterConfig config;
    @Provides
    RuneBodyCrafterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RuneBodyCrafterConfig.class);
    }
    @Inject
    private RuneBodyCrafterScript runeBodyCrafterScript;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private RuneBodyCrafterOverlay runeBodyCrafterOverlay;



    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(runeBodyCrafterOverlay);
        }
        runeBodyCrafterScript.run(config);
    }

    protected void shutDown() {
        runeBodyCrafterScript.shutdown();
        overlayManager.remove(runeBodyCrafterOverlay);
    }


}
