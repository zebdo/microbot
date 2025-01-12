package net.runelite.client.plugins.microbot.runecrafting.chillRunecraft;

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
        name = PluginDescriptor.ChillX + "Auto Runecrafter",
        description = "F2P Auto Runecraft Plugin",
        tags = {"Chill", "Runecraft", "rc", "f2p"},
        enabledByDefault = false
)
@Slf4j
public class RunecraftPlugin extends Plugin {
    @Inject
    private RunecraftConfig config;
    @Provides
    RunecraftConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RunecraftConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private RunecraftOverlay runecraftOverlay;

    @Inject
    RunecraftScript runecraftScript;


    @Override
    protected void startUp() throws AWTException
    {
        if (overlayManager != null)
        {
            overlayManager.add(runecraftOverlay);
        }
        runecraftScript.run(config);
    }

    protected void shutDown()
    {
        runecraftScript.shutdown();
        overlayManager.remove(runecraftOverlay);
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
