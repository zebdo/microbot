package net.runelite.client.plugins.microbot.example;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.tileobject.Rs2TileObjectApi;
import net.runelite.client.plugins.microbot.util.tileobject.Rs2TileObjectModel;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
@Slf4j
public class ExampleScript extends Script {

    /**
     * Main entry point for the performance test script.
     */
    public boolean run() {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;

                // Performance test: Loop over game objects and get compositions
                long startTime = System.currentTimeMillis();
                AtomicLong endTime = new AtomicLong();

                var tileObjects = Microbot.getClientThread().invoke(() -> {
                  //  List<Rs2TileObjectModel> _tileObjects = Rs2TileObjectApi.getObjectsStream().filter(x -> x.getName() != null && !x.getName().isEmpty() && x.getName() != "null").collect(Collectors.toList());
                    Rs2TileObjectModel test = Rs2TileObjectApi.getNearest(tile -> tile.getName() != null && tile.getName().toLowerCase().contains("tree"));
                    endTime.set(System.currentTimeMillis());
                    System.out.println("Retrieved " + test.getName() + " game objects in " + (endTime.get() - startTime) + " ms");

                    /*for (Rs2TileObjectModel rs2TileObjectModel: _tileObjects) {
                        var name = rs2TileObjectModel.getName(); // Access name to simulate some processing
                        System.out.println("Object Name: " + name);
                    }
*/
                    return Rs2TileObjectApi.getObjectsStream().collect(Collectors.toList());
                });


                int compositionCount = 0;

                /*for (Rs2TileObjectModel tileObject : tileObjects) {
                    var name = tileObject.getName(); // Access name to simulate some processing
                    if (name != null) {
                        compositionCount++;
                        System.out.println("composition " + compositionCount + ": " + name);
                    }
                }*/

                endTime.set(System.currentTimeMillis());
                long durationMs = (endTime.get() - startTime);

/*
                log.info("Performance Test Results:");
                log.info("  Total GameObjects: {}", tileObjects.size());
                log.info("  Compositions retrieved: {}", compositionCount);
                log.info("  Time taken: {} ms", durationMs);
                log.info("  Average time per object: {} μs",
                        tileObjects.size() > 0 ? (endTime.get() - startTime) / 1000 / tileObjects.size() : 0);
*/

            } catch (Exception ex) {
                log.error("Error in performance test loop", ex);
            }
        }, 0, 5000, TimeUnit.MILLISECONDS);

        return true;
    }
}
