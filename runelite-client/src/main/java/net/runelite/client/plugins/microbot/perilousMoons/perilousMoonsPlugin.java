package net.runelite.client.plugins.microbot.perilousMoons;

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
        name = PluginDescriptor.Default + "Perilous Moons Beta",
        description = "A plugin to defeat the Perilous Moons bosses",
        tags = {"bossing", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class perilousMoonsPlugin extends Plugin {
    @Inject
    private perilousMoonsConfig config;
    @Provides
    perilousMoonsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(perilousMoonsConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private perilousMoonsOverlay perilousMoonsOverlay;

    @Inject
    perilousMoonsScript perilousMoonsScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(perilousMoonsOverlay);
        }
        perilousMoonsScript.run(config);
    }

    protected void shutDown() {
        perilousMoonsScript.shutdown();
        overlayManager.remove(perilousMoonsOverlay);
    }
    int ticks = 10;
    @Subscribe
    public void onGameTick(GameTick tick)
    {
        //System.out.println(getName().chars().mapToObj(i -> (char)(i + 3)).map(String::valueOf).collect(Collectors.joining()));

        if (ticks > 0) {
            ticks--;
        } else {
            ticks = 10;
        }

    }

}
