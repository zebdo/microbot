package net.runelite.client.plugins.microbot.cardewsPlugins.AutoBankPin;

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
        name = PluginDescriptor.Cardew + "AutoBankPin",
        description = "Cardews automatic bank pin entry",
        tags = {"cardew", "microbot", "cd", "bank", "pin"},
        enabledByDefault = true
)
@Slf4j
public class AutoBankPinPlugin extends Plugin {
    @Inject
    private AutoBankPinConfig config;
    @Provides
    AutoBankPinConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoBankPinConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private AutoBankPinOverlay autoBankPinOverlay;

    @Inject
    AutoBankPinScript autoBankPinScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            //overlayManager.add(autoBankPinOverlay);
        }
        autoBankPinScript.run(config);
    }

    protected void shutDown() {
        autoBankPinScript.shutdown();
        //overlayManager.remove(autoBankPinOverlay);
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
