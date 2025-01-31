package net.runelite.client.plugins.microbot.brimAgility;

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
        name = PluginDescriptor.Gage + "Brimhaven Agility Spike",
        description = "Brimhaven Agility Spike",
        tags = {"Brimhaven Agility Spike", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class BrimPlugin extends Plugin {
    @Inject
    private net.runelite.client.plugins.microbot.brimAgility.BrimConfig config;
    @Provides
    net.runelite.client.plugins.microbot.brimAgility.BrimConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BrimConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private BrimOverlay brimOverlay;

    @Inject
    BrimScript brimScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(brimOverlay);
        }
        brimScript.run(config);
    }

    protected void shutDown() {
        brimScript.shutdown();
        overlayManager.remove(brimOverlay);
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
