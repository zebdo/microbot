package net.runelite.client.plugins.microbot.moonsOfPeril;

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
        name = PluginDescriptor.Default + "Moons of Peril Beta",
        description = "A plugin to defeat the Perilous Moons bosses",
        tags = {"bossing", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class moonsOfPerilPlugin extends Plugin {
    @Inject
    private moonsOfPerilConfig config;
    @Provides
    moonsOfPerilConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(moonsOfPerilConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private moonsOfPerilOverlay moonsOfPerilOverlay;

    @Inject
    moonsOfPerilScript moonsOfPerilScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(moonsOfPerilOverlay);
        }
        moonsOfPerilScript.run(config);
    }

    protected void shutDown() {
        moonsOfPerilScript.shutdown();
        overlayManager.remove(moonsOfPerilOverlay);
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
