package net.runelite.client.plugins.microbot.util.cache.strategy.entity;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ScheduledExecutorService;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.cache.strategy.CacheUpdateStrategy;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2ObjectModel;
import net.runelite.client.plugins.microbot.util.cache.strategy.CacheOperations;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;

/**
 * Enhanced cache update strategy for NPC data.
 * Handles NPC spawn/despawn events and provides scene scanning.
 * Follows the same pattern as ObjectUpdateStrategy for consistency.
 */
@Slf4j
public class NpcUpdateStrategy implements CacheUpdateStrategy<Integer, Rs2NpcModel> {
    
    // ScheduledExecutorService for non-blocking operations
    private final ScheduledExecutorService executorService;    
    private ScheduledFuture<?> sceneScanTask;
    
    // Scene scan tracking
    private final AtomicBoolean scanActive = new AtomicBoolean(false);
    private final AtomicBoolean scanRequest = new AtomicBoolean(false);    
    private volatile long lastSceneScan = 0;
    private static final long MIN_SCAN_INTERVAL_MS = Constants.GAME_TICK_LENGTH;
    
    /**
     * Constructor initializes the executor service for background tasks.
     */
    public NpcUpdateStrategy() {
        this.executorService = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "NpcUpdateStrategy-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
        log.debug("NpcUpdateStrategy initialized with ScheduledExecutorService");
    }
    
    @Override
    public void handleEvent(final Object event, CacheOperations<Integer, Rs2NpcModel> cache) {
        if (executorService == null || executorService.isShutdown()) {
            return; // Don't process events if shut down
        }
        
        // Submit event handling to executor service for non-blocking processing
        //executorService.submit(() -> {
            //try {
                processEventInternal(event, cache);
            //} catch (Exception e) {
                //log.error("Error processing event: {}", event.getClass().getSimpleName(), e);
            //}
        //});
    }
    
    /**
     * Internal method to process events - runs on background thread.
     */
    private void processEventInternal(Object event, CacheOperations<Integer, Rs2NpcModel> cache) {
        if (event instanceof NpcSpawned) {
            handleNpcSpawned((NpcSpawned) event, cache);
        } else if (event instanceof NpcDespawned) {
            handleNpcDespawned((NpcDespawned) event, cache);
        }
    }
    
    /**
     * Performs an scene scan to populate the cache.
     * Only scans if certain conditions are met to avoid unnecessary processing.
     * This method runs asynchronously on a background thread to avoid blocking.
     * 
     * @param cache The cache to populate
     */
    public void performSceneScan(CacheOperations<Integer, Rs2NpcModel> cache, long delayMs) {
        if (executorService == null || executorService.isShutdown() ) {
            log.debug("Skipping NPC scene scan - strategy is shut down or not logged in");
            return;
        }
        
        // Submit scene scan to executor service for non-blocking execution
        executorService.schedule(() -> {
            try {
                performSceneScanInternal(cache);
            } catch (Exception e) {
                log.error("Error during NPC scene scan", e);
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
    public void schedulePeriodicSceneScan(CacheOperations<Integer, Rs2NpcModel> cache, long intervalSeconds) {
        if (executorService == null || executorService.isShutdown()) {
            log.warn("Cannot schedule periodic scan - strategy is shut down");
            return;
        }
        
        // Cancel existing task if any
        stopPeriodicSceneScan();
        
        sceneScanTask = executorService.scheduleWithFixedDelay(() -> {
            try {
                if (scanRequest.get() && Microbot.loggedIn) {
                    log.debug("Periodic NPC scene scan triggered");
                    performSceneScanInternal(cache);
                }
            } catch (Exception e) {
                log.error("Error in periodic NPC scene scan", e);
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        
        log.debug("Scheduled periodic NPC scene scan every {} seconds", intervalSeconds);
    }
    
    /**
     * Stops periodic scene scanning.
     */
    public void stopPeriodicSceneScan() {
        if (sceneScanTask != null && !sceneScanTask.isDone()) {
            sceneScanTask.cancel(false);
            sceneScanTask = null;
            log.debug("Stopped periodic NPC scene scanning");
        }
    }
    
    
    /**
     * Checks if a scene scan is needed based on cache state.
     * 
     * @param cache The cache to check
     * @return true if a scan would be beneficial
     */    
    public boolean requestSceneScan(CacheOperations<Integer, Rs2NpcModel> cache) {
        if (scanRequest.compareAndSet(false, true)) {
            log.debug("Object scene scan requested");
            performSceneScan(cache, 0); // Perform immediately
        }
        if (!Microbot.getClient().isClientThread()){
            sleepUntil(()->!scanRequest.get(), 1000); // Wait until scan is requested and reset
        }        
        return !scanRequest.get(); // Return true if scan was requested,reseted               
    }
    
    /**
     * Performs the actual scene scan to populate NPCs from the client's NPC list.
     * This is similar to how Rs2Npc.getNpcs() works but loads into cache.
     */
    private void performSceneScanInternal(CacheOperations<Integer, Rs2NpcModel> cache) {
        if (scanActive.compareAndSet(false, true)) {
            try {
                long currentTime = System.currentTimeMillis();
                
                // Check minimum interval to prevent excessive scanning
                if (currentTime - lastSceneScan < MIN_SCAN_INTERVAL_MS) {
                    log.debug("Skipping NPC scene scan - too soon since last scan");
                    scanActive.set(false);
                    return;
                }
                
                Player player = Microbot.getClient().getLocalPlayer();
                if (player == null) {
                    log.info("Cannot perform NPC scene scan - no player");
                    scanActive.set(false);
                    return;
                }
                
                // Get all NPCs from the client
                int addedNpcs = 0;
                log.debug("Starting NPC scene scan (cache size: {})", cache.size());
                
                for (NPC npc : Microbot.getClient().getTopLevelWorldView().npcs()) {
                    if (npc != null && npc.getName() != null) {
                        // Only add if not already in cache to avoid recursive calls
                        if (!cache.containsKey(npc.getIndex())) {
                            Rs2NpcModel npcModel = new Rs2NpcModel(npc);
                            cache.put(npc.getIndex(), npcModel);
                            addedNpcs++;
                        }
                    }
                }
                            
                
                if (addedNpcs > 0) {
                    log.debug("NPC scene scan completed - added {} NPCs (total cache size: {})", 
                            addedNpcs, cache.size());
                } else {
                    log.debug("NPC scene scan completed - no new NPCs added");
                }
                scanRequest.set(false); // Reset request after scan
            } finally {
                lastSceneScan =  System.currentTimeMillis();   // Update last scan time;                
                scanActive.set(false);                
            }
        } else {
            log.debug("NPC scene scan already in progress, skipping");
        }
    }
    
    private void handleNpcSpawned(NpcSpawned event, CacheOperations<Integer, Rs2NpcModel> cache) {
        NPC npc = event.getNpc();
        if (npc != null) {
            Rs2NpcModel npcModel = new Rs2NpcModel(npc);
            cache.put(npc.getIndex(), npcModel);
            log.trace("Added NPC {} (index: {}) to cache via spawn event", npc.getName(), npc.getIndex());
        }
    }
    
    private void handleNpcDespawned(NpcDespawned event, CacheOperations<Integer, Rs2NpcModel> cache) {
        NPC npc = event.getNpc();
        if (npc != null) {
            cache.remove(npc.getIndex());
            log.trace("Removed NPC {} (index: {}) from cache via despawn event", npc.getName(), npc.getIndex());
        }
    }
    
    @Override
    public Class<?>[] getHandledEventTypes() {
        return new Class<?>[]{NpcSpawned.class, NpcDespawned.class};
    }
    
    @Override
    public void onAttach(CacheOperations<Integer, Rs2NpcModel> cache) {
        log.debug("NpcUpdateStrategy attached to cache");
    }
    
    @Override
    public void onDetach(CacheOperations<Integer, Rs2NpcModel> cache) {
        log.debug("NpcUpdateStrategy detached from cache");
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
