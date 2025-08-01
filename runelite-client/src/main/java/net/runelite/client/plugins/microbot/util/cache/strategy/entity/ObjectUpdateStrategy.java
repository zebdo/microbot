package net.runelite.client.plugins.microbot.util.cache.strategy.entity;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ScheduledExecutorService;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questhelper.steps.tools.QuestPerspective;
import net.runelite.client.plugins.microbot.util.cache.Rs2Cache;
import net.runelite.client.plugins.microbot.util.cache.strategy.CacheOperations;
import net.runelite.client.plugins.microbot.util.cache.strategy.CacheUpdateStrategy;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2ObjectModel;
/**
 * Enhanced cache update strategy for game object data.
 * Handles all types of object spawn/despawn events and provides scene scanning.
 * Uses String-based cache keys for better tracking and region change handling.
 */
@Slf4j
public class ObjectUpdateStrategy implements CacheUpdateStrategy<String, Rs2ObjectModel> {
    GameState lastGameState = null;
    
    // ScheduledExecutorService for non-blocking operations
    private final ScheduledExecutorService executorService;
    private ScheduledFuture<?> sceneScanTask;
    AtomicBoolean scanActive = new AtomicBoolean(false);
    AtomicBoolean scanRequest = new AtomicBoolean(false);
    private volatile long lastSceneScan = 0;
    private static final long MIN_SCAN_INTERVAL_MS = Constants.GAME_TICK_LENGTH;
    
    /**
     * Constructor initializes the executor service for background tasks.
     */
    public ObjectUpdateStrategy() {
        this.executorService = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "ObjectUpdateStrategy-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
    }
    /**
     * Generates a unique cache key for any TileObject (GameObject, WallObject, GroundObject, DecorativeObject).
     * Uses canonical location logic for GameObjects, and world location for others.
     *
     * @param object The TileObject (GameObject, WallObject, etc.)
     * @param tile The tile containing the object
     * @return The cache key string
     */
    public static String generateCacheIdForObject(TileObject object, Tile tile) {
        if (object instanceof GameObject) {
            // Use canonical location logic for GameObjects            
            return ObjectUpdateStrategy.generateCacheIdForGameObject((GameObject) object, tile);
        } else if (object instanceof WallObject) {
            WallObject wallObject = (WallObject) object;            
            return ObjectUpdateStrategy.generateCacheId("WallObject", wallObject.getId(), wallObject.getWorldLocation());
        } else if (object instanceof GroundObject) {
            GroundObject groundObject = (GroundObject) object;
            return ObjectUpdateStrategy.generateCacheId("GroundObject", groundObject.getId(), groundObject.getWorldLocation());
        } else if (object instanceof DecorativeObject) {
            DecorativeObject decorativeObject = (DecorativeObject) object;
            return ObjectUpdateStrategy.generateCacheId("DecorativeObject", decorativeObject.getId(), decorativeObject.getWorldLocation());
        }
        // Fallback: use type name and world location if available
        String type = object != null ? object.getClass().getSimpleName() : "Unknown";
        WorldPoint location = object != null ? object.getWorldLocation() : null;
        int objectId = object != null ? object.getId() : -1;
        return location != null ? String.format("%s_%d_%d_%d_%d", type, objectId, location.getX(), location.getY(), location.getPlane()) : type + "_null";
    }
        
   
    @Override
    public void handleEvent(final Object event, final CacheOperations<String, Rs2ObjectModel> cache) {
        if (executorService == null || executorService.isShutdown()) {
            log.warn("ObjectUpdateStrategy is shut down, ignoring event: {}", event.getClass().getSimpleName());
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
     * Internal method to process events - runs on client thread after coalescing.
     * This is the renamed version of the original processEvent method.
     */
    private void processEventInternal(final Object event, final CacheOperations<String, Rs2ObjectModel> cache) {
        try {       
            if (event instanceof GameObjectSpawned) {
                if(lastGameState == GameState.LOGGED_IN) handleGameObjectSpawned((GameObjectSpawned) event, cache);
            } else if (event instanceof GameObjectDespawned) {
                if(lastGameState == GameState.LOGGED_IN) handleGameObjectDespawned((GameObjectDespawned) event, cache);
            } else if (event instanceof GroundObjectSpawned) {
                if(lastGameState == GameState.LOGGED_IN) handleGroundObjectSpawned((GroundObjectSpawned) event, cache);
            } else if (event instanceof GroundObjectDespawned) {
                if(lastGameState == GameState.LOGGED_IN) handleGroundObjectDespawned((GroundObjectDespawned) event, cache);
            } else if (event instanceof WallObjectSpawned) {
                if(lastGameState == GameState.LOGGED_IN) handleWallObjectSpawned((WallObjectSpawned) event, cache);
            } else if (event instanceof WallObjectDespawned) {
                if(lastGameState == GameState.LOGGED_IN) handleWallObjectDespawned((WallObjectDespawned) event, cache);
            } else if (event instanceof DecorativeObjectSpawned) {
                if(lastGameState == GameState.LOGGED_IN) handleDecorativeObjectSpawned((DecorativeObjectSpawned) event, cache);
            } else if (event instanceof DecorativeObjectDespawned) {
                if(lastGameState == GameState.LOGGED_IN) handleDecorativeObjectDespawned((DecorativeObjectDespawned) event, cache);
            } else if (event instanceof GameStateChanged) {
                handleGameStateChanged((GameStateChanged) event, cache);
            } else if (event instanceof GameTick) {
                //handleGameTick((GameTick) event, cache);
            }
        } catch (Exception e) {
            log.error("Error handling event: {}", event.getClass().getSimpleName(), e);
        }        
    }
    
    /**
     * Performs an scene scan to populate the cache.
     * Only scans if certain conditions are met to avoid unnecessary processing.
     * This method now runs asynchronously on a background thread to avoid blocking.
     * 
     * @param cache The cache to populate
     * @param force Whether to force a scan regardless of conditions
     */
    public void performSceneScan(CacheOperations<String, Rs2ObjectModel> cache, long delayMs) {
        if (executorService == null || executorService.isShutdown()) {
            log.debug("Skipping scene scan - is executor service shutdown: {} or null {}, scan active: {}", 
                      executorService.isShutdown() ,executorService == null  , scanActive.get());
            return;
        }
        
        // Submit scene scan to executor service for non-blocking execution
        executorService.schedule(() -> {
            try {
                performSceneScanInternal(cache);
            } catch (Exception e) {
                log.error("Error during scene scan", e);
            }finally {
                scanActive.set(false);
                scanRequest.set(false); // Reset request after scan
            }
        }, delayMs, TimeUnit.MILLISECONDS); // 100ms delay before scan
    }
    
    /**
     * Schedules a periodic scene scan task.
     * This is useful for ensuring the cache stays up-to-date even if events are missed.
     * 
     * @param cache The cache to scan
     * @param intervalSeconds The interval between scans in seconds
     */
    public void schedulePeriodicSceneScan(CacheOperations<String, Rs2ObjectModel> cache, long intervalSeconds) {
        if (executorService == null || executorService.isShutdown()) {
            log.debug("Cannot schedule periodic scan - strategy is shut down");
            return;
        }
        stopPeriodicSceneScan();
               
        sceneScanTask = executorService.scheduleWithFixedDelay(() -> {
            try {
                if (scanRequest.get() && Microbot.loggedIn) { // Only perform scan if request is set and not already active
                    log.debug("Performing scheduled scene scan");
                    performSceneScanInternal(cache);
                }
            } catch (Exception e) {
                log.error("Error during scheduled scene scan", e);
            }finally {                
            }

        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        
        log.debug("Scheduled periodic scene scan every {} seconds", intervalSeconds);
    }
    
    /**
     * Internal implementation of scene scanning that runs on background thread.
     */

    private void performSceneScanInternal(CacheOperations<String, Rs2ObjectModel> cache) {
        if (scanActive.compareAndSet(false,true)){ 
            try {
                long currentTime = System.currentTimeMillis();               
                log.debug("Performing scene scan (last scan: {}, current time: {}) , loggedin: {}",  lastSceneScan, currentTime, Microbot.loggedIn);
                if (!Microbot.loggedIn || Microbot.getClient() == null || Microbot.getClient().getLocalPlayer() == null) {
                    log.warn("Cannot perform scene scan - not logged in");
                    scanActive.set(false);
                    return;
                }
                // Respect minimum scan interval unless forced
                if ((currentTime - lastSceneScan) < MIN_SCAN_INTERVAL_MS) {
                    log.warn("Skipping scene scan due to minimum interval not reached");
                    scanActive.set(false);
                    return;
                }                                   
                Player player = Microbot.getClient().getLocalPlayer();
                if (player == null) {
                    log.warn("Cannot perform scene scan - no player");
                    scanActive.set(false);
                    return;
                }
                WorldPoint playerPoint = QuestPerspective.getRealWorldPointFromLocal( Microbot.getClient(), player.getWorldLocation());
                if (playerPoint == null) {
                    log.warn("Cannot perform scene scan - player location is null");
                    scanActive.set(false);
                    return ;
                }
        

                Scene scene = player.getWorldView().getScene();
                if (scene == null) {
                    log.warn("Cannot perform scene scan - no scene");
                    scanActive.set(false);
                    return;
                }
                
                Tile[][][] tiles = scene.getTiles();
                if (tiles == null) {
                    log.warn("Cannot perform scene scan - no tiles");
                    scanActive.set(false);
                    return;
                }
                
                // Collect all objects first to avoid recursive cache calls
                java.util.Map<String, Rs2ObjectModel> objectsToAdd = new java.util.HashMap<>();
                int z = player.getWorldView().getPlane();
                
                log.debug("Starting scene scan (cache size: {})", cache.size());
                
                for (int x = 0; x < Constants.SCENE_SIZE; x++) {
                    for (int y = 0; y < Constants.SCENE_SIZE; y++) {
                        Tile tile = tiles[z][x][y];
                        if (tile == null) continue;
                        
                        // Check GameObjects
                        GameObject[] gameObjects = tile.getGameObjects();
                        if (gameObjects != null) {
                            for (GameObject gameObject : gameObjects) {
                                if (gameObject == null) continue;
                                
                                // Only add if it's the primary location for multi-tile objects
                                if (gameObject.getSceneMinLocation().equals(tile.getSceneLocation())) {
                                    String cacheId = generateCacheIdForGameObject(gameObject, tile);
                                    if (!objectsToAdd.containsKey(cacheId)) {
                                        Rs2ObjectModel objectModel = new Rs2ObjectModel(gameObject, tile);
                                        objectsToAdd.put(cacheId, objectModel);
                                    }
                                }
                            }
                        }
                        
                        // Check GroundObject
                        GroundObject groundObject = tile.getGroundObject();
                        if (groundObject != null) {
                            String cacheId = generateCacheId("GroundObject", groundObject.getId(), groundObject.getWorldLocation());
                            if (!objectsToAdd.containsKey(cacheId)) {
                                Rs2ObjectModel objectModel = new Rs2ObjectModel(groundObject, tile);
                                objectsToAdd.put(cacheId, objectModel);
                            }
                        }
                        
                        // Check WallObject
                        WallObject wallObject = tile.getWallObject();
                        if (wallObject != null) {
                            String cacheId = generateCacheId("WallObject", wallObject.getId(), wallObject.getWorldLocation());
                            if (!objectsToAdd.containsKey(cacheId)) {
                                Rs2ObjectModel objectModel = new Rs2ObjectModel(wallObject, tile);
                                objectsToAdd.put(cacheId, objectModel);
                            }
                        }
                        
                        // Check DecorativeObject
                        DecorativeObject decorativeObject = tile.getDecorativeObject();
                        if (decorativeObject != null) {
                            String cacheId = generateCacheId("DecorativeObject", decorativeObject.getId(), decorativeObject.getWorldLocation());
                            if (!objectsToAdd.containsKey(cacheId)) {
                                Rs2ObjectModel objectModel = new Rs2ObjectModel(decorativeObject, tile);
                                objectsToAdd.put(cacheId, objectModel);
                            }
                        }
                    }
                }
                
                // Now update the cache in batch to avoid recursive calls
                int addedObjects = 0;
                for (java.util.Map.Entry<String, Rs2ObjectModel> entry : objectsToAdd.entrySet()) {
                    String cacheId = entry.getKey();
                    Rs2ObjectModel objectModel = entry.getValue();
                    
                    // Only add if not already in cache (avoid recursive get calls by checking internally)
                    if (!cache.containsKey(cacheId)) {
                        cache.put(cacheId, objectModel);
                        addedObjects++;
                    }
                }
                
                
                
                if (addedObjects > 0) {
                    log.debug("scene scan completed - added {} objects (total cache size: {}), time taken: {} ms", 
                            addedObjects, cache.size(), System.currentTimeMillis() - currentTime);
                } else {
                    log.debug("Scene scan completed - no new objects added");
                }
                scanRequest.set(false); //NOT in finally block to allow for rescan if there are an error 
            } catch (Exception e) {
                log.error("Error during scene scan", e);
            } finally {
                // Reset scan state
                lastSceneScan =  System.currentTimeMillis();   // Update last scan time;                
                scanActive.set(false);
            }
            
        }
    }
    
  
    
    /**
     * Stops periodic scene scanning if currently active.
     */
    public void stopPeriodicSceneScan() {        
        if (isPeriodicSceneScanActive()) {
            sceneScanTask.cancel(false);
            sceneScanTask = null;
            log.debug("Stopped periodic scene scanning");
        }
    }
    
    /**
     * Checks if periodic scene scanning is currently active.
     * 
     * @return true if periodic scanning is running
     */
    private boolean isPeriodicSceneScanActive() {
        return sceneScanTask != null && !sceneScanTask.isDone();
    }
  
    /**
     * Checks if a scene scan is needed based on cache state.
     * 
     * @param cache The cache to check
     * @return true if a scan would be beneficial
     */    
    public boolean requestSceneScan(CacheOperations<String, Rs2ObjectModel> cache) {
        if (scanRequest.compareAndSet(false, true)) {
            log.debug("Object scene scan requested");
            performSceneScan(cache, 0); // Perform immediately
        }
        if (!Microbot.getClient().isClientThread()){
            sleepUntil(()->!scanRequest.get(), 1000); // Wait until scan is requested and reset
        }        
        return !scanRequest.get(); // Return true if scan was requested,reseted               
    }
    private void handleGameObjectSpawned(GameObjectSpawned event, CacheOperations<String, Rs2ObjectModel> cache) {
        GameObject gameObject = event.getGameObject();
        Tile tile = event.getTile();
        if (gameObject != null && tile != null) {
            // Only add multi-tile objects from their primary (southwest) tile to prevent duplicates
            String cacheId = generateCacheIdForGameObject(gameObject, tile);
            if (cache.containsKey(cacheId)) {
                log.debug("GameObject {} already in cache, skipping spawn event", gameObject.getId());
                return; // Already cached, skip
            }
            if (isPrimaryTile(gameObject, tile)) {                
                Rs2ObjectModel objectModel = new Rs2ObjectModel(gameObject, tile);                
                cache.put(cacheId, objectModel);                
                log.debug("Added GameObject {} (id: {}) to cache via spawn event from primary tile", 
                         gameObject.getId(), cacheId);
            } else {
                log.trace("Skipped GameObject {} spawn event from non-primary tile", gameObject.getId());
            }
        }
    }
    
    private void handleGameObjectDespawned(GameObjectDespawned event, CacheOperations<String, Rs2ObjectModel> cache) {
        GameObject gameObject = event.getGameObject();
        Tile tile = event.getTile();
        if (gameObject != null && tile != null) {
            // Only process despawn events from the primary tile to prevent multiple removal attempts
            if (isPrimaryTile(gameObject, tile)) {
                String cacheId = generateCacheIdForGameObject(gameObject, tile);
                cache.remove(cacheId);
                log.debug("Removed GameObject {} (id: {}) from cache via despawn event from primary tile", 
                         gameObject.getId(), cacheId);
            } else {
                log.trace("Skipped GameObject {} despawn event from non-primary tile", gameObject.getId());
            }
        }
    }
    
    private void handleGroundObjectSpawned(GroundObjectSpawned event, CacheOperations<String, Rs2ObjectModel> cache) {
        GroundObject groundObject = event.getGroundObject();
        Tile tile = event.getTile();
        String cacheId = generateCacheId("GroundObject", groundObject.getId(), groundObject.getWorldLocation());
        if (cache.containsKey(cacheId)) {
            log.trace("GroundObject {} already in cache, skipping spawn event", groundObject.getId());
            return; // Already cached, skip
        }
        if (groundObject != null && tile != null) {            
            Rs2ObjectModel objectModel = new Rs2ObjectModel(groundObject, tile);            
            cache.put(cacheId, objectModel);
            log.debug("Added GroundObject {} (id: {}) to cache via spawn event", groundObject.getId(), cacheId);
        }
    }
    
    private void handleGroundObjectDespawned(GroundObjectDespawned event, CacheOperations<String, Rs2ObjectModel> cache) {
        GroundObject groundObject = event.getGroundObject();
        if (groundObject != null) {            
            String cacheId = generateCacheId("GroundObject", groundObject.getId(), groundObject.getWorldLocation());
            cache.remove(cacheId);
            log.debug("Removed GroundObject {} (id: {}) from cache via despawn event", groundObject.getId(), cacheId);
        }
    }
    
    private void handleWallObjectSpawned(WallObjectSpawned event, CacheOperations<String, Rs2ObjectModel> cache) {
        WallObject wallObject = event.getWallObject();
        Tile tile = event.getTile();
      
        if (wallObject != null && tile != null) {
            String cacheId = generateCacheId("WallObject", wallObject.getId(), wallObject.getWorldLocation());
            if (cache.containsKey(cacheId)) {
                log.trace("WallObject {} already in cache, skipping spawn event", wallObject.getId());
                return; // Already cached, skip
            }
            Rs2ObjectModel objectModel = new Rs2ObjectModel(wallObject, tile);
            
            cache.put(cacheId, objectModel);
            log.debug("Added WallObject {} (id: {}) to cache via spawn event", wallObject.getId(), cacheId);
        }
    }
    
    private void handleWallObjectDespawned(WallObjectDespawned event, CacheOperations<String, Rs2ObjectModel> cache) {
        WallObject wallObject = event.getWallObject();
        if (wallObject != null) {
            String cacheId = generateCacheId("WallObject", wallObject.getId(), wallObject.getWorldLocation());
            cache.remove(cacheId);
            log.debug("Removed WallObject {} (id: {}) from cache via despawn event", wallObject.getId(), cacheId);
        }
    }
    
    private void handleDecorativeObjectSpawned(DecorativeObjectSpawned event, CacheOperations<String, Rs2ObjectModel> cache) {
        DecorativeObject decorativeObject = event.getDecorativeObject();
        Tile tile = event.getTile();
        if (decorativeObject != null && tile != null) {
            String cacheId = generateCacheId("DecorativeObject", decorativeObject.getId(), decorativeObject.getWorldLocation());
            if (cache.containsKey(cacheId)) {
                log.trace("DecorativeObject {} already in cache, skipping spawn event", decorativeObject.getId());
                return; // Already cached, skip
            }
            Rs2ObjectModel objectModel = new Rs2ObjectModel(decorativeObject, tile);            
            cache.put(cacheId, objectModel);
            log.debug("Added DecorativeObject {} (id: {}) to cache via spawn event", decorativeObject.getId(), cacheId);
        }
    }
    
    private void handleDecorativeObjectDespawned(DecorativeObjectDespawned event, CacheOperations<String, Rs2ObjectModel> cache) {
        DecorativeObject decorativeObject = event.getDecorativeObject();
        if (decorativeObject != null) {
            String cacheId = generateCacheId("DecorativeObject", decorativeObject.getId(), decorativeObject.getWorldLocation());
            cache.remove(cacheId);
            log.debug("Removed DecorativeObject {} (id: {}) from cache via despawn event", decorativeObject.getId(), cacheId);
        }
    }
    
    private void handleGameStateChanged(GameStateChanged event, CacheOperations<String, Rs2ObjectModel> cache) {
        switch (event.getGameState()) {
            case LOGGED_IN:
                // Check for region changes and clear cache if changed
                if (Rs2Cache.checkAndHandleRegionChange(cache)) {
                    log.debug("Region change detected on login - performing scene scan");
                    cache.invalidateAll(); // Clear cache on region change
                    performSceneScan(cache, Constants.GAME_TICK_LENGTH * 2);
                } else if (lastGameState != null && lastGameState != GameState.LOGGED_IN) {
                    cache.invalidateAll(); // Clear cache on region change
                    // Request scene scan when logging in - might have missed spawn events
                    performSceneScan(cache, Constants.GAME_TICK_LENGTH * 3); // Perform scan after 10 game ticks to allow scene to stabilize
                }
                
                lastGameState = GameState.LOGGED_IN;
                log.debug("Player logged in - checking regions and requesting scene scan");
                break;
            case LOADING:
                // Check for region changes during loading
                if (Rs2Cache.checkAndHandleRegionChange(cache)) {
                    log.debug("Region change detected during loading - performing scene scan");
                    cache.invalidateAll(); // Clear cache on region change
                    performSceneScan(cache, Constants.GAME_TICK_LENGTH * 2);
                } else {
                    cache.invalidateAll(); // Clear cache on region change
                    performSceneScan(cache, Constants.GAME_TICK_LENGTH * 3);
                }
                lastGameState = GameState.LOADING;
                log.debug("Game loading - checking regions and requesting scene scan");
                break;
            case LOGIN_SCREEN:
            case LOGGING_IN:
            case CONNECTION_LOST:
                // Clear scan request when logging out and stop periodic scanning
                cache.invalidateAll();
                lastGameState = event.getGameState();
                
                // Cancel periodic scanning
                if (sceneScanTask != null && !sceneScanTask.isDone()) {
                    sceneScanTask.cancel(false);
                    sceneScanTask = null;
                }
                log.debug("Player logged out - clearing scan request and stopping periodic scanning");
                break;
            default:
                lastGameState = event.getGameState();
                break;
        }
    }
    
    /**
     * Gets the canonical world location for a GameObject.
     * For multi-tile objects, this returns the southwest tile location.
     * 
     * @param gameObject The GameObject to get the canonical location for
     * @param tile The tile from the event
     * @return The canonical world location
     */
    private static WorldPoint getCanonicalLocation(GameObject gameObject, Tile tile) {
        // For multi-tile objects, we need to ensure we use the southwest tile consistently
        Point sceneMinLocation = gameObject.getSceneMinLocation();
        Point currentSceneLocation = tile.getSceneLocation();
        
        // If this is the southwest tile, use this tile's location
        if (sceneMinLocation != null && currentSceneLocation != null && 
            sceneMinLocation.getX() == currentSceneLocation.getX() && 
            sceneMinLocation.getY() == currentSceneLocation.getY()) {
            return tile.getWorldLocation();
        }
        
        // Otherwise, we need to calculate the southwest tile's world location
        // This is tricky without scene-to-world conversion, so we'll use a different approach
        WorldPoint currentLocation = tile.getWorldLocation();
        if (sceneMinLocation != null && currentSceneLocation != null) {
            int deltaX = currentSceneLocation.getX() - sceneMinLocation.getX();
            int deltaY = currentSceneLocation.getY() - sceneMinLocation.getY();
            return new WorldPoint(currentLocation.getX() - deltaX, currentLocation.getY() - deltaY, currentLocation.getPlane());
        }
        
        return currentLocation;
    }
    
    /**
     * Checks if the given tile is the primary (southwest) tile for a GameObject.
     * 
     * @param gameObject The GameObject to check
     * @param tile The tile to verify
     * @return true if this is the primary tile, false otherwise
     */
    private boolean isPrimaryTile(GameObject gameObject, Tile tile) {
        Point sceneMinLocation = gameObject.getSceneMinLocation();
        Point currentSceneLocation = tile.getSceneLocation();
        
        return sceneMinLocation != null && currentSceneLocation != null && 
               sceneMinLocation.getX() == currentSceneLocation.getX() && 
               sceneMinLocation.getY() == currentSceneLocation.getY();
    }

    /**
     * Generates a unique object ID for tracking.
     * For GameObjects, uses the canonical (southwest) location to ensure consistent caching.
     */
    private static String generateCacheId(String type, int objectID, net.runelite.api.coords.WorldPoint location) {
        return String.format("%s_%d_%d_%d_%d", type, objectID, location.getX(), location.getY(), location.getPlane());
    }
    
    /**
     * Generates a unique object ID for tracking GameObjects using their canonical location.
     * This ensures that multi-tile GameObjects have consistent cache keys.
     */
    private static String generateCacheIdForGameObject(GameObject gameObject, Tile tile) {
        WorldPoint canonicalLocation = getCanonicalLocation(gameObject, tile);
        return generateCacheId("GameObject", gameObject.getId(), canonicalLocation);
    }
    
    @Override
    public Class<?>[] getHandledEventTypes() {
        return new Class<?>[]{
            GameObjectSpawned.class, GameObjectDespawned.class,
            GroundObjectSpawned.class, GroundObjectDespawned.class,
            WallObjectSpawned.class, WallObjectDespawned.class,
            DecorativeObjectSpawned.class, DecorativeObjectDespawned.class,
            GameStateChanged.class, GameTick.class
        };
    }
    
    @Override
    public void onAttach(CacheOperations<String, Rs2ObjectModel> cache) {
        log.debug("ObjectUpdateStrategy attached to cache");
        // Start periodic scene scanning if logged in
        if (Microbot.loggedIn && lastGameState == GameState.LOGGED_IN) {
            //schedulePeriodicSceneScan(cache, 30); // Every 30 seconds
        }
    }
    
    @Override
    public void onDetach(CacheOperations<String, Rs2ObjectModel> cache) {
        log.debug("ObjectUpdateStrategy detached from cache");
        // Cancel periodic scanning when detaching
        if (sceneScanTask != null && !sceneScanTask.isDone()) {
            sceneScanTask.cancel(false);
            sceneScanTask = null;
        }
    }
    
    @Override
    public void close() {        
        log.info("Shutting down ObjectUpdateStrategy");                                    
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
