package net.runelite.client.plugins.microbot.example;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Example Plugin",
        description = "Performance test for GameObject composition retrieval",
        tags = {"performance", "microbot", "test", "gameobject"},
        enabledByDefault = false
)
@Slf4j
public class ExamplePlugin extends Plugin {
    @Inject
    ExampleScript exampleScript;


    @Override
    protected void startUp() throws AWTException {

        exampleScript.run();
    }



    protected void shutDown() {
        exampleScript.shutdown();
    }

    // on settings change
    @Subscribe
    public void onConfigChanged(final ConfigChanged event) {
    }
}