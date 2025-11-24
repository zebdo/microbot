package net.runelite.client.plugins.microbot.example;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.sailing.data.BoatPathFollower;
import net.runelite.client.plugins.microbot.util.sailing.data.PortPaths;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Performance test script for measuring GameObject composition retrieval speed.
 *
 * This script runs every 5 seconds and performs the following:
 * - Gets all GameObjects in the scene
 * - Retrieves the ObjectComposition for each GameObject
 * - Measures and logs the total time taken
 * - Reports average time per object
 *
 * Useful for performance profiling and optimization testing.
 */
@Singleton
@Slf4j
public class ExampleScript extends Script {

    /**
     * Main entry point for the performance test script.
     */
    public boolean run() {
        //var boatPathFollower = new BoatPathFollower(PortPaths.PORT_SARIM_PANDEMONIUM.getFullPath(true));
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;

                WorldPoint worldPoint = WorldPoint.fromRegion(Microbot.getClient().getLocalPlayer().getWorldLocation().getRegionID(),
                        35,
                        34,
                        Microbot.getClient().getTopLevelWorldView().getPlane());

                Rs2Bank.openBank();

                Rs2Bank.withdrawAll("coins");

            } catch (Exception ex) {
                log.error("Error test loop", ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);

        return true;
    }
}
