package net.runelite.client.plugins.microbot.util.cache;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.cache.serialization.CacheSerializable;
import net.runelite.client.plugins.microbot.util.cache.strategy.simple.QuestUpdateStrategy;
import net.runelite.client.plugins.microbot.util.cache.util.LogOutputMode;
import net.runelite.client.plugins.microbot.util.cache.util.Rs2CacheLoggingUtils;

/**
 * Thread-safe cache for quest states using the unified cache architecture.
 * Automatically updates when quest-related events are received and supports persistence.
 * 
 * This class extends Rs2UnifiedCache and provides specific quest caching functionality
 * with proper EventBus integration for @Subscribe methods.
 * 
 * Threading Strategy:
 * - All quest state loading uses async approach with invokeLater() for deferred execution
 * - Event handlers: Use invokeLater() to defer execution and avoid blocking event processing
 * - Async methods: Use invokeLater() for deferred execution during initialization scenarios
 * 
 * The cache uses only asynchronous operations to avoid nested thread issues
 * that can occur when events (already on client thread) need to load quest states.
 */
@Slf4j
public class Rs2QuestCache extends Rs2Cache<Quest, QuestState> implements CacheSerializable {
    private static Rs2QuestCache instance;    
    // Async update tracking
    private static final AtomicInteger pendingAsyncUpdates = new java.util.concurrent.atomic.AtomicInteger(0);
    private static final Set<Consumer<Boolean>> updateCompletionCallbacks = java.util.concurrent.ConcurrentHashMap.newKeySet();
    
    // Quest-specific update coordination to prevent deadlocks and race conditions
    private static final java.util.concurrent.ConcurrentHashMap<Quest, Long> questUpdatesInProgress = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long UPDATE_TIMEOUT_MS = 10000; // 10 seconds timeout for update tracking
    
    /**
     * Private constructor for singleton pattern.
     */
    private Rs2QuestCache() {        
        super("QuestCache", CacheMode.EVENT_DRIVEN_ONLY);
        this.withUpdateStrategy(new QuestUpdateStrategy())
                .withPersistence("quests");
    }
    
    /**
     * Gets the singleton instance of Rs2QuestCache.
     * 
     * @return The singleton quest cache instance
     */
    public static synchronized Rs2QuestCache getInstance() {
        if (instance == null) {
            instance = new Rs2QuestCache();
        }
        return instance;
    }
    
    /**
     * Gets the cache instance for backward compatibility.
     * 
    /**
     * Gets the cache instance for backward compatibility.
     * 
     * @return The singleton unified cache instance
     */
    public static Rs2Cache<Quest, QuestState> getCache() {
        return getInstance();
    }
    
    /**
     * Checks if a quest update is currently in progress.
     * Cleans up stale update entries older than UPDATE_TIMEOUT_MS.
     * 
     * @param quest The quest to check
     * @return true if update is in progress, false otherwise
     */
    private static boolean isUpdateInProgress(Quest quest) {
        Long updateStartTime = questUpdatesInProgress.get(quest);
        if (updateStartTime == null) {
            return false;
        }
        
        // Check if update has timed out
        long currentTime = System.currentTimeMillis();
        if (currentTime - updateStartTime > UPDATE_TIMEOUT_MS) {
            // Remove stale update entry
            questUpdatesInProgress.remove(quest);
            log.warn("Removed stale quest update entry for {} (timeout after {}ms)", 
                    quest.getName(), UPDATE_TIMEOUT_MS);
            return false;
        }
        
        return true;
    }
    
    /**
     * Marks a quest as having an update in progress.
     * 
     * @param quest The quest to mark
     * @return true if successfully marked (no existing update), false if update already in progress
     */
    private static boolean markUpdateInProgress(Quest quest) {
        long currentTime = System.currentTimeMillis();
        Long previousTime = questUpdatesInProgress.putIfAbsent(quest, currentTime);
        
        if (previousTime != null) {
            // Check if existing update has timed out
            if (currentTime - previousTime > UPDATE_TIMEOUT_MS) {
                // Replace stale entry
                questUpdatesInProgress.put(quest, currentTime);
                log.debug("Replaced stale quest update entry for {}", quest.getName());
                return true;
            }
            return false; // Update already in progress
        }
        
        return true; // Successfully marked
    }
    
    /**
     * Marks a quest update as completed.
     * 
     * @param quest The quest to mark as completed
     */
    private static void markUpdateCompleted(Quest quest) {
        questUpdatesInProgress.remove(quest);
    }
    
    /**
     * Cleans up stale quest update entries.
     */
    private static void cleanupStaleUpdates() {
        long currentTime = System.currentTimeMillis();
        questUpdatesInProgress.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > UPDATE_TIMEOUT_MS) {
                log.debug("Cleaned up stale quest update entry for {}", entry.getKey().getName());
                return true;
            }
            return false;
        });
    }
    

    
  
    
    
    
    /**
     * Asynchronously loads quest state using invokeLater for deferred execution.
     * This is the core method for all quest state loading to ensure consistent async behavior.
     * Prevents duplicate updates for the same quest using quest-specific coordination.
     * 
     * @param quest The quest to load state for
     * @param callback Callback to handle the loaded quest state
     */
    private static void loadQuestStateFromClientAsync(Quest quest, Consumer<QuestState> callback) {
        try {
            log.debug("Setting up async quest state loading for {}", quest.getName());
            
            if (Microbot.getClient() == null) {
                log.warn("Client is null when loading quest state for {}", quest);
                executeCallback(callback, QuestState.NOT_STARTED);
                return;
            }
            
            // Check if update is already in progress for this quest
            if (!markUpdateInProgress(quest)) {
                log.debug("Quest update already in progress for {}, rejecting duplicate request", quest.getName());
                return; // Simply reject the duplicate update request
            }
            
            // Increment pending updates counter
            pendingAsyncUpdates.incrementAndGet();
            
            // Always use invokeLater for consistency, even when on client thread
            Microbot.getClientThread().invokeLater(() -> {
                executeQuestStateLoad(quest, callback);
            });
            
        } catch (Exception e) {
            log.error("Error setting up async quest state loading for {}: {}", quest, e.getMessage(), e);
            handleLoadFailure(quest, callback);
        }
    }
    
    /**
     * Executes the actual quest state loading on the client thread.
     * 
     * @param quest The quest to load state for
     * @param callback Callback to handle the loaded quest state
     */
    private static void executeQuestStateLoad(Quest quest, Consumer<QuestState> callback) {
        try {
            QuestState state = quest.getState(Microbot.getClient());
            log.debug("Loaded quest state: {} = {}", quest.getName(), state);
            
            // Update cache with new state
            updateQuestState(quest, state);
            
            // Execute callback
            executeCallback(callback, state);
            
        } catch (Exception e) {
            log.error("Error in quest state loading for {}: {}", quest, e.getMessage(), e);
            executeCallback(callback, QuestState.NOT_STARTED);
        } finally {
            // Always clean up tracking and notify completion
            handleLoadCompletion(quest);
        }
    }
    
    /**
     * Handles cleanup and notification when a quest load completes.
     * 
     * @param quest The quest that completed loading
     */
    private static void handleLoadCompletion(Quest quest) {
        // Mark quest update as completed
        markUpdateCompleted(quest);
        
        // Decrement pending updates and check for completion
        int remaining = pendingAsyncUpdates.decrementAndGet();
        if (remaining == 0) {
            // Clean up stale updates
            cleanupStaleUpdates();
            
            // Notify all completion callbacks
            notifyCompletionCallbacks();
        }
    }
    
    /**
     * Handles load failure scenarios.
     * 
     * @param quest The quest that failed to load
     * @param callback The callback to notify of failure
     */
    private static void handleLoadFailure(Quest quest, Consumer<QuestState> callback) {
        markUpdateCompleted(quest);
        executeCallback(callback, QuestState.NOT_STARTED);
        pendingAsyncUpdates.decrementAndGet();
    }
    
    /**
     * Safely executes a callback with error handling.
     * 
     * @param callback The callback to execute
     * @param state The quest state to pass to the callback
     */
    private static void executeCallback(Consumer<QuestState> callback, QuestState state) {
        if (callback != null) {
            try {
                callback.accept(state);
            } catch (Exception e) {
                log.error("Error in quest state callback: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Notifies all completion callbacks safely.
     */
    private static void notifyCompletionCallbacks() {
        updateCompletionCallbacks.forEach(completionCallback -> {
            try {
                completionCallback.accept(true);
            } catch (Exception e) {
                log.error("Error in update completion callback: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Gets quest state from the cache. If not cached, returns NOT_STARTED and triggers async loading.
     * This prevents blocking behavior and deadlocks by not waiting for completion.
     * 
     * @param quest The quest to retrieve state for
     * @return The cached QuestState or NOT_STARTED if not in cache
     */
    public static QuestState getQuestState(Quest quest) {
        // Use the base cache get method directly
        QuestState cachedState = getInstance().get(quest);           
        if (cachedState != null ) {
            return cachedState;
        }
        if ( isUpdateInProgress(quest)) {
            //log.info("Quest update in progress for {}, returning NOT_STARTED", quest.getName());
            return QuestState.NOT_STARTED; // Return NOT_STARTED if update is in progress
        }
        
        // Trigger async loading if not cached
        updateQuestStateAsync(quest);        
        return getCache().get(quest); // Default state if not cached 
    
                  
        

    }
    
    /**
     * Asynchronously updates quest state in cache. Useful during initialization or event processing
     * where you want to ensure quest state loading doesn't block current execution.
     * 
     * @param quest The quest to update
     */
    public static void updateQuestStateAsync(Quest quest) {
        loadQuestStateFromClientAsync(quest, state -> {
            log.debug("Async quest update completed: {} = {}", quest.getName(), state);
        });
    }
    
    /**
     * Gets quest state asynchronously with a callback. Preferred method for async operations.
     * If cached, callback is executed immediately. Otherwise, loads asynchronously.
     * 
     * @param quest The quest to retrieve state for
     * @param callback Callback to receive the quest state
     */
    public static void getQuestStateAsync(Quest quest, Consumer<QuestState> callback) {
        QuestState cachedState = getInstance().get(quest);
        if (cachedState != null) {
            executeCallback(callback, cachedState);
        } else {
            loadQuestStateFromClientAsync(quest, callback);
        }
    }
    
    /**
     * Registers a callback to be notified when all pending async updates are complete.
     * 
     * @param callback Callback to be called with true when all updates are complete
     */
    public static void onAllAsyncUpdatesComplete(Consumer<Boolean> callback) {
        if (callback != null) {
            updateCompletionCallbacks.add(callback);
            // If no updates are pending, call immediately
            if (pendingAsyncUpdates.get() == 0) {
                try {
                    callback.accept(true);
                } catch (Exception e) {
                    log.error("Error in immediate completion callback: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Gets the number of pending async quest state updates.
     * 
     * @return The number of pending updates
     */
    public static int getPendingAsyncUpdates() {
        return pendingAsyncUpdates.get();
    }
    
    /**
     * Clears all completion callbacks. Useful for cleanup.
     */
    public static void clearCompletionCallbacks() {
        updateCompletionCallbacks.clear();
    }
    
    /**
     * Gets quest state from the cache or loads it with a custom supplier.
     * 
     * @param quest The quest to retrieve state for
     * @param valueLoader Custom supplier for loading the quest state
     * @return The QuestState
     */
    public static QuestState getQuestState(Quest quest, Supplier<QuestState> valueLoader) {
        return getInstance().get(quest, valueLoader);
    }
    
    /**
     * Manually updates a quest state in the cache.
     * 
     * @param quest The quest to update
     * @param state The new quest state
     */
    private static void updateQuestState(Quest quest, QuestState state) {
        getInstance().put(quest, state);
        log.debug("Updated quest cache: {} = {}", quest, state);
    }
    
    /**
     * Checks if a quest is started (not NOT_STARTED).
     * 
     * @param quest The quest to check
     * @return true if the quest is started
     */
    public static boolean isQuestStarted(Quest quest) {
        return getQuestState(quest) != QuestState.NOT_STARTED;
    }
    
    /**
     * Checks if a quest is completed (FINISHED).
     * 
     * @param quest The quest to check
     * @return true if the quest is completed
     */
    public static boolean isQuestCompleted(Quest quest) {
        return getQuestState(quest) == QuestState.FINISHED;
    }
    
    /**
     * Checks if a quest is in progress (IN_PROGRESS).
     * 
     * @param quest The quest to check
     * @return true if the quest is in progress
     */
    public static boolean isQuestInProgress(Quest quest) {
        return getQuestState(quest) == QuestState.IN_PROGRESS;
    }
    
    /**
     * Schedules an asynchronous update of all cached quests using invokeLater.
     * This is useful during initialization or when you want to ensure quest updates
     * don't block current event processing, even when already on the client thread.
     */
    public static void updateAllFromClientAsync() {
        
        try {
            log.debug("Starting asynchronous quest cache update...");
            getInstance().update();
            log.debug("Completed asynchronous quest cache update");
        } catch (Exception e) {
            log.error("Error during asynchronous quest cache update: {}", e.getMessage(), e);
        }
        
    }
    
    /**
     * Updates all cached data by retrieving fresh values from the game client asynchronously.
     * Implements the abstract method from Rs2Cache.
     * 
     * Iterates over all currently cached quest keys and refreshes their states asynchronously.
     */
    @Override
    public void update() {
        log.debug("Updating all cached quests from client asynchronously...");
        
        if (Microbot.getClient() == null) {
            log.warn("Cannot update quests - client is null");
            return;
        }
        
        int beforeSize = size();
        
        // Get all currently cached quest keys and update them asynchronously
        java.util.Set<Quest> cachedQuests = entryStream()
            .map(java.util.Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toSet());
        
        if (cachedQuests.isEmpty()) {
            log.debug("No cached quests to update");
            return;
        }
        
        log.info("Starting async update of {} cached quests", cachedQuests.size());
        
        for (Quest quest : cachedQuests) {
            loadQuestStateFromClientAsync(quest, freshState -> {
                if (freshState != null) {
                    put(quest, freshState);
                    log.debug("Updated quest {} with fresh state: {}", quest.getName(), freshState);
                }
            });
        }
        
        log.info("Initiated async update for {} quests (cache had {} entries total)", 
                cachedQuests.size(), beforeSize);
    }
    
   
    
    /**
     * Event handler registration for the unified cache.
     * The unified cache handles events through its strategy automatically.
     */    
        
    @Subscribe
    public void onVarbitChanged(final VarbitChanged event) {
        try {
            getInstance().handleEvent(event);                       
        } catch (Exception e) {
            log.error("Error handling VarbitChanged event: {}", e.getMessage(), e);
        }
    }
    
    @Subscribe
    public void onChatMessage(final ChatMessage chatMessage) {
        try {
            getInstance().handleEvent(chatMessage);            
        } catch (Exception e) {
            log.error("Error handling ChatMessage event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Resets the singleton instance. Used for testing.
     */
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.invalidateAll();
            instance = null;
        }
    }
    
    // ============================================
    // CacheSerializable Implementation
    // ============================================
    
    @Override
    public String getConfigKey() {
        return "quests";
    }
    
    @Override
    public String getConfigGroup() {
        return "microbot";
    }
    
    @Override
    public boolean shouldPersist() {
        return true; // Quest states should be persisted for progress tracking
    }
    
    /**
     * Logs the current state of all cached quests for debugging.
     * 
     * @param dumpToFile Whether to also dump the information to a file
     */
    public static void logState(LogOutputMode mode) {
        var cache = getInstance();
        var stats = cache.getStatistics();
        
        // Create the log content
        StringBuilder logContent = new StringBuilder();
        
        String header = String.format("=== Quest Cache State (%d entries) ===", cache.size());
        logContent.append(header).append("\n");
        
        String statsInfo = Rs2CacheLoggingUtils.formatCacheStatistics(
            stats.getHitRate(), stats.cacheHits, stats.cacheMisses, stats.cacheMode.toString());        
        logContent.append(statsInfo).append("\n\n");
        
        if (cache.size() == 0) {
            String emptyMsg = "Cache is empty";            
            logContent.append(emptyMsg).append("\n");
        } else {
            // Table format for quests
            String[] headers = {"Quest", "State", "ID", "Cache Timestamp", "Varbit ID", "VarPlayer ID"};
            int[] columnWidths = {40, 15, 8, 22, 10, 12};
            
            String tableHeader = Rs2CacheLoggingUtils.formatTableHeader(headers, columnWidths);            
            logContent.append("\n").append(tableHeader);
            
            // Sort quests by state (completed first, then in progress, then not started)
            cache.entryStream()
                .sorted((a, b) -> {
                    try {
                        // Handle null entries
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        if (a.getKey() == null && b.getKey() == null) return 0;
                        if (a.getKey() == null) return 1;
                        if (b.getKey() == null) return -1;
                        if (a.getValue() == null && b.getValue() == null) return 0;
                        if (a.getValue() == null) return 1;
                        if (b.getValue() == null) return -1;
                        
                        // Sort by state priority: FINISHED > IN_PROGRESS > NOT_STARTED
                        int aOrder = getQuestStateOrder(a.getValue());
                        int bOrder = getQuestStateOrder(b.getValue());
                        if (aOrder != bOrder) {
                            return Integer.compare(aOrder, bOrder);
                        }
                        // Then sort alphabetically by quest name
                        String nameA = a.getKey().getName();
                        String nameB = b.getKey().getName();
                        if (nameA == null && nameB == null) return 0;
                        if (nameA == null) return 1;
                        if (nameB == null) return -1;
                        return nameA.compareTo(nameB);
                    } catch (Exception e) {
                        // Fallback to ID comparison if anything goes wrong
                        try {
                            return Integer.compare(
                                a != null && a.getKey() != null ? a.getKey().getId() : Integer.MAX_VALUE,
                                b != null && b.getKey() != null ? b.getKey().getId() : Integer.MAX_VALUE
                            );
                        } catch (Exception e2) {
                            return 0; // Last resort - consider equal
                        }
                    }
                })                
                .forEach(entry -> {
                    Quest quest = entry.getKey();
                    QuestState questState = entry.getValue();
                    
                    // Get cache timestamp for this quest
                    Long cacheTimestamp = cache.getCacheTimestamp(quest);
                    String cacheTimestampStr = cacheTimestamp != null ? 
                        Rs2Cache.formatUtcTimestamp(cacheTimestamp) : "N/A";
                    
                    // Get varbit/varPlayer IDs for this quest
                    Integer varbitId = QuestUpdateStrategy.getVarbitIdByQuest(quest);
                    Integer varPlayerId = QuestUpdateStrategy.getVarPlayerIdByQuest(quest);
                    
                    String[] values = {
                        Rs2CacheLoggingUtils.truncate(quest.getName(), 39),
                        questState.toString(),
                        String.valueOf(quest.getId()),
                        Rs2CacheLoggingUtils.truncate(cacheTimestampStr, 21),
                        varbitId != null ? String.valueOf(varbitId) : "N/A",
                        varPlayerId != null ? String.valueOf(varPlayerId) : "N/A"
                    };
                    
                    String row = Rs2CacheLoggingUtils.formatTableRow(values, columnWidths);
                    logContent.append(row);
                });
            
            String tableFooter = Rs2CacheLoggingUtils.formatTableFooter(columnWidths);            
            logContent.append(tableFooter);
            
            // Summary statistics
            long completedCount = cache.entryStream().filter(e -> e.getValue() == QuestState.FINISHED).count();
            long inProgressCount = cache.entryStream().filter(e -> e.getValue() == QuestState.IN_PROGRESS).count();
            long notStartedCount = cache.entryStream().filter(e -> e.getValue() == QuestState.NOT_STARTED).count();
            
            String summaryMsg = String.format("Quest Summary: %d Completed, %d In Progress, %d Not Started", 
                completedCount, inProgressCount, notStartedCount);
            logContent.append(summaryMsg).append("\n");
        }
        
        String footer = "=== End Quest Cache State ===";        
        logContent.append(footer).append("\n");                
        Rs2CacheLoggingUtils.outputCacheLog(getInstance().getCacheName(), logContent.toString(), mode);                 
    }
    
    /**
     * Helper method to define quest state sorting order.
     */
    private static int getQuestStateOrder(QuestState state) {
        switch (state) {
            case FINISHED:
                return 0; // Highest priority
            case IN_PROGRESS:
                return 1; // Medium priority
            case NOT_STARTED:
            default:
                return 2; // Lowest priority
        }
    }
    
  
}
