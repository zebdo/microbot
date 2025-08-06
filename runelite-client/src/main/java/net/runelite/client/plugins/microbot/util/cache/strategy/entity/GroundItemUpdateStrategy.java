package net.runelite.client.plugins.microbot.util.cache.strategy.entity;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ScheduledExecutorService;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.cache.strategy.CacheOperations;
import net.runelite.client.plugins.microbot.util.cache.strategy.CacheUpdateStrategy;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItemModel;

/**
 * Enhanced cache update strategy for ground item data.
 * Handles automatic cache updates based on ground item spawn/despawn events and provides scene scanning.
 * Follows the same pattern as ObjectUpdateStrategy for consistency.
 */
@Slf4j
public class GroundItemUpdateStrategy implements CacheUpdateStrategy<String, Rs2GroundItemModel> {
    
    // ScheduledExecutorService for non-blocking operations
    private final ScheduledExecutorService executorService;
    private ScheduledFuture<?> sceneScanTask;
    
    // Scene scan tracking
    private final AtomicBoolean scanActive = new AtomicBoolean(false);
    private final AtomicBoolean scanRequest =  new AtomicBoolean(false);        
    private static final long MIN_SCAN_INTERVAL_MS = Constants.GAME_TICK_LENGTH; // Minimum interval between scans
    private volatile long lastSceneScan = 0; // Last time a scene scan was performed
    
    /**
     * Constructor initializes the executor service for background tasks.
     */
    public GroundItemUpdateStrategy() {
        this.executorService = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "GroundItemUpdateStrategy-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
        log.debug("GroundItemUpdateStrategy initialized with ScheduledExecutorService");
    }
    
    @Override
    public void handleEvent(Object event, CacheOperations<String, Rs2GroundItemModel> cache) {
        if (executorService == null || executorService.isShutdown() || !Microbot.loggedIn || Microbot.getClient() == null || Microbot.getClient().getLocalPlayer() == null) {
            return; // Don't process events if shut down
        }
        
        // Submit event handling to executor service for non-blocking processing
        executorService.submit(() -> {
            try {
                processEventInternal(event, cache);
            } catch (Exception e) {
                log.error("Error processing event: {}", event.getClass().getSimpleName(), e);
            }
        });
    }
    
    /**
     * Internal method to process events - runs on background thread.
     */
    private void processEventInternal(Object event, CacheOperations<String, Rs2GroundItemModel> cache) {
        if (event instanceof ItemSpawned) {
            handleItemSpawned((ItemSpawned) event, cache);
        } else if (event instanceof ItemDespawned) {
            handleItemDespawned((ItemDespawned) event, cache);
        }
    }
    
    /**
     * Performs an scene scan to populate the cache.
     * Only scans if certain conditions are met to avoid unnecessary processing.
     * This method runs asynchronously on a background thread to avoid blocking.
     * 
     * @param cache The cache to populate
     */
    public void performSceneScan(CacheOperations<String, Rs2GroundItemModel> cache,long delayMs) {
        if (executorService == null || executorService.isShutdown()) {
            log.debug("Skipping ground item scene scan - strategy is shut down or not logged in");
            return;
        }
        
        // Submit scene scan to executor service for non-blocking execution
        executorService.schedule(() -> {
            try {
                performSceneScanInternal(cache);
            } catch (Exception e) {
                log.error("Error during ground item scene scan", e);
            }
        }, delayMs, TimeUnit.MILLISECONDS); // delay before scan
    }
    
    /**
     * Schedules a periodic scene scan task.
     * This is useful for ensuring the cache stays up-to-date even if events are missed.
     * 
     * @param cache The cache to scan
     * @param intervalSeconds How often to scan in seconds
     */
    public void schedulePeriodicSceneScan(CacheOperations<String, Rs2GroundItemModel> cache, long intervalSeconds) {
        if (executorService == null || executorService.isShutdown()) {
            log.warn("Cannot schedule periodic scan - strategy is shut down");
            return;
        }
        
        // Cancel existing task if any
        stopPeriodicSceneScan();
        
        sceneScanTask = executorService.scheduleWithFixedDelay(() -> {
            try {
                if (scanRequest.get() && Microbot.loggedIn) {
                    log.debug("Periodic ground item scene scan triggered");
                    performSceneScanInternal(cache);
                }
            } catch (Exception e) {
                log.error("Error in periodic ground item scene scan", e);
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        
        log.debug("Scheduled periodic ground item scene scan every {} seconds", intervalSeconds);
    }
    
    /**
     * Stops periodic scene scanning.
     */
    public void stopPeriodicSceneScan() {
        if (sceneScanTask != null && !sceneScanTask.isDone()) {
            sceneScanTask.cancel(false);
            sceneScanTask = null;
            log.debug("Stopped periodic ground item scene scanning");
        }
    }
    
    /**
     * Checks if a scene scan is needed based on cache state.
     * 
     * @param cache The cache to check
     * @return true if a scan would be beneficial
     */
    public boolean requestSceneScan(CacheOperations<String, Rs2GroundItemModel> cache) {
        if (scanRequest.compareAndSet(false, true)) {
            log.debug("Ground item scene scan requested");
            performSceneScan(cache, 0);
        }
        if (!Microbot.getClient().isClientThread()){
            sleepUntil(()->!scanRequest.get()); 
        }        
        return !scanRequest.get(); // Return true if scan was requested,reseted               
    }
    
    /**
     * Performs the actual scene scan to populate ground items from tiles.
     * Scans all tiles in the scene to find ground items similar to ObjectUpdateStrategy.
     */
    private void performSceneScanInternal(CacheOperations<String, Rs2GroundItemModel> cache) {
        if (scanActive.compareAndSet(false, true)) {
            try {
                long currentTime = System.currentTimeMillis();
                
               // Check minimum interval to prevent excessive scanning
                if (currentTime - lastSceneScan < MIN_SCAN_INTERVAL_MS) {
                    log.debug("Skipping Ground scene scan - too soon since last scan");
                    scanActive.set(false);
                    return;
                }
                Player player = Microbot.getClient().getLocalPlayer();
                if (player == null) {
                    log.warn("Cannot perform ground item scene scan - no player");
                    scanActive.set(false);
                    return;
                }
                
                Scene scene = player.getWorldView().getScene();
                if (scene == null) {
                    log.warn("Cannot perform ground item scene scan - no scene");
                    scanActive.set(false);
                    return;
                }
                
                Tile[][][] tiles = scene.getTiles();
                if (tiles == null) {
                    log.warn("Cannot perform ground item scene scan - no tiles");
                    scanActive.set(false);
                    return;
                }
                
                int addedItems = 0;
                int z = player.getWorldView().getPlane();
                
                log.debug("Starting ground item scene scan (cache size: {})", cache.size());
                
                for (int x = 0; x < Constants.SCENE_SIZE; x++) {
                    for (int y = 0; y < Constants.SCENE_SIZE; y++) {
                        Tile tile = tiles[z][x][y];
                        if (tile == null) continue;
                        if(tile.getGroundItems() == null) continue; // Ensure ground items are loaded
                        // Get ground items on this tile
                        for (TileItem tileItem : tile.getGroundItems()) {
                            if (tileItem != null) {
                                String key = generateKey(tileItem, tile.getWorldLocation());
                                
                                // Only add if not already in cache to avoid recursive calls
                                if (!cache.containsKey(key)) {
                                    Rs2GroundItemModel groundItemModel = new Rs2GroundItemModel(tileItem, tile);
                                    cache.put(key, groundItemModel);
                                    addedItems++;
                                }
                            }
                        }
                    }
                }
                                                
                if (addedItems > 0) {
                    log.debug("Ground item scene scan completed - added {} items (total cache size: {})", 
                            addedItems, cache.size());
                } else {
                    log.debug("Ground item scene scan completed - no new items added");
                }
                scanRequest.set(false); //NOT in finally block to allow for rescan if there are an error 
            }catch (Exception e) {
                log.error("Error during ground item scene scan", e);                
            }finally {
                scanActive.set(false);
                lastSceneScan = System.currentTimeMillis(); // Update last scan time                
            }
        } else {
            log.debug("Ground item scene scan already in progress, skipping");
        }
    }
    
    private void handleItemSpawned(ItemSpawned event, CacheOperations<String, Rs2GroundItemModel> cache) {
        TileItem item = event.getItem();
        if (item != null) {
            String key = generateKey(item, event.getTile().getWorldLocation());
            Rs2GroundItemModel groundItem = new Rs2GroundItemModel(item, event.getTile());
            cache.put(key, groundItem);
            log.trace("Added ground item {} at {} to cache via spawn event", item.getId(), event.getTile().getWorldLocation());
        }
    }
    
    private void handleItemDespawned(ItemDespawned event, CacheOperations<String, Rs2GroundItemModel> cache) {
        TileItem item = event.getItem();
        if (item != null) {
            String key = generateKey(item, event.getTile().getWorldLocation());
            cache.remove(key);
            log.trace("Removed ground item {} at {} from cache via despawn event", item.getId(), event.getTile().getWorldLocation());
        }
    }
    
    /**
     * Generates a unique key for ground items based on item ID, quantity, and location.
     * 
     * @param item The tile item
     * @param location The world location
     * @return Unique key string
     */
    private String generateKey(TileItem item, net.runelite.api.coords.WorldPoint location) {
        return String.format("%d_%d_%d_%d_%d", 
                item.getId(), 
                item.getQuantity(), 
                location.getX(), 
                location.getY(), 
                location.getPlane());
    }
    
    @Override
    public Class<?>[] getHandledEventTypes() {
        return new Class<?>[]{ItemSpawned.class, ItemDespawned.class};
    }
    
    @Override
    public void onAttach(CacheOperations<String, Rs2GroundItemModel> cache) {
        log.debug("GroundItemUpdateStrategy attached to cache");
    }
    
    @Override
    public void onDetach(CacheOperations<String, Rs2GroundItemModel> cache) {
        log.debug("GroundItemUpdateStrategy detached from cache");
    }
    @Override
    public void close() {
        stopPeriodicSceneScan();
        shutdownExecutorService();
    }
    /**
     * Shuts down the executor service gracefully, waiting for currently executing tasks to complete.
     * If the executor does not terminate within the initial timeout, it attempts a forced shutdown.
     * Logs warnings or errors if the shutdown process does not complete as expected.
     * If interrupted during shutdown, the method forces shutdown and re-interrupts the current thread.
     */
    private void shutdownExecutorService() {
        if (executorService != null && !executorService.isShutdown()) {
            // Shutdown executor service
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Executor service did not terminate gracefully, forcing shutdown");
                    executorService.shutdownNow();
                    
                    // Wait a bit more for tasks to respond to being cancelled
                    if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                        log.error("Executor service did not terminate after forced shutdown");
                    }
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted during executor shutdown", e);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }        
        }                     
    }
}
