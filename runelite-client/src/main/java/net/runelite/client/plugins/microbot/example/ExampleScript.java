package net.runelite.client.plugins.microbot.example;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.Rs2NpcCache;
import net.runelite.client.plugins.microbot.api.tileitem.Rs2TileItemCache;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.shortestpath.WorldPointUtil;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.api.player.Rs2PlayerCache;
import net.runelite.client.plugins.microbot.util.reachable.Rs2Reachable;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Performance test script for measuring GameObject composition retrieval speed.
 * <p>
 * This script runs every 5 seconds and performs the following:
 * - Gets all GameObjects in the scene
 * - Retrieves the ObjectComposition for each GameObject
 * - Measures and logs the total time taken
 * - Reports average time per object
 * <p>
 * Useful for performance profiling and optimization testing.
 */
@Slf4j
public class ExampleScript extends Script {

    @Inject
    Rs2TileItemCache rs2TileItemCache;
    @Inject
    Rs2TileObjectCache rs2TileObjectCache;
    @Inject
    Rs2PlayerCache rs2PlayerCache;
    @Inject
    Rs2NpcCache rs2NpcCache;
    /**
     * Main entry point for the performance test script.
     */
    private static final WorldPoint HOPPER_DEPOSIT_DOWN = new WorldPoint(3748, 5672, 0);

    public boolean run() {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;


                rs2TileObjectCache.query().withIds(26661, 26662, 26663, 26664).interact("Mine");

                var tiles = Rs2Reachable.getReachableTiles(Rs2Player.getWorldLocation());

                var pack = WorldPointUtil.packWorldPoint(new WorldPoint(3725, 5651, 0));

                System.out.println(tiles.contains(pack));


            } catch (Exception ex) {
                log.error("Error in performance test loop", ex);
            }
        }, 0, 5000, TimeUnit.MILLISECONDS);

        return true;
    }
}
