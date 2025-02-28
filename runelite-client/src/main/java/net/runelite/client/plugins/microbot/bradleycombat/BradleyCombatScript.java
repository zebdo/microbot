package net.runelite.client.plugins.microbot.bradleycombat;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;

import java.util.concurrent.TimeUnit;

public class BradleyCombatScript extends Script {

    public boolean run(BradleyCombatConfig config) {

        Microbot.enableAutoRunOn = true;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() ->
        {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                // TODO: Logic for potions and food coming soon.
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}