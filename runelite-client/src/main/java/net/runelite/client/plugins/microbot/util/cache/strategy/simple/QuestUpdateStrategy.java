package net.runelite.client.plugins.microbot.util.cache.strategy.simple;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.ChatMessageType;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questhelper.QuestHelperPlugin;
import net.runelite.client.plugins.microbot.questhelper.questhelpers.QuestHelper;
import net.runelite.client.plugins.microbot.questhelper.questinfo.QuestHelperQuest;
import net.runelite.client.plugins.microbot.util.cache.strategy.CacheOperations;
import net.runelite.client.plugins.microbot.util.cache.strategy.CacheUpdateStrategy;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache update strategy for quest data.
 * Handles VarbitChanged and ChatMessage events to update quest state information.
 * Uses QuestHelperQuest enum to efficiently detect quest-related changes.
 */
@Slf4j
public class QuestUpdateStrategy implements CacheUpdateStrategy<Quest, QuestState> {
    
    // Quest tracking variables moved from Rs2QuestCache
    private static Quest trackedQuest = null;
    
    // Map from varbit/varplayer IDs to corresponding RuneLite Quest objects
    private static final Map<Integer, Quest> varbitToQuestMap = new ConcurrentHashMap<>();
    private static final Map<Integer, Quest> varPlayerToQuestMap = new ConcurrentHashMap<>();
    
    // Flag to track if quest maps have been initialized
    private static volatile boolean mapsInitialized = false;
    
    /**
     * Gets the currently tracked quest.
     * 
     * @return The currently tracked quest, or null if none
     */
    public static Quest getTrackedQuest() {
        return trackedQuest;
    }
    
    /**
     * Sets the quest to track for changes.
     * 
     * @param quest The quest to track, or null to stop tracking
     */
    public static void setTrackedQuest(Quest quest) {
        if (trackedQuest != quest) {
            Quest oldQuest = trackedQuest;
            trackedQuest = quest;
            log.debug("Tracked quest changed from {} to {}", 
                    oldQuest != null ? oldQuest.getName() : "none",
                    quest != null ? quest.getName() : "none");
        }
    }
    
    /**
     * Initializes the varbit/varPlayer to Quest mapping from QuestHelperQuest enum.
     * This is done lazily on first access to avoid initialization order issues.
     */
    private static void initializeQuestMaps() {
        if (mapsInitialized) {
            return;
        }
        
        synchronized (QuestUpdateStrategy.class) {
            if (mapsInitialized) {
                return;
            }
            
            try {
                log.debug("Initializing quest variable maps from QuestHelperQuest enum...");
                
                for (QuestHelperQuest questHelperQuest : QuestHelperQuest.values()) {
                    // Get RuneLite Quest by ID
                    Quest runeliteQuest = getQuestById(questHelperQuest.getId());
                    if (runeliteQuest == null) {
                        continue; // Skip quests without RuneLite Quest mapping
                    }
                    
                    // Use reflection to access private varbit and varPlayer fields
                    try {
                        Field varbitField = QuestHelperQuest.class.getDeclaredField("varbit");
                        varbitField.setAccessible(true);
                        Object varbitValue = varbitField.get(questHelperQuest);
                        
                        if (varbitValue != null) {
                            // Get the ID from the QuestVarbits enum
                            Field idField = varbitValue.getClass().getDeclaredField("id");
                            idField.setAccessible(true);
                            int varbitId = (Integer) idField.get(varbitValue);
                            varbitToQuestMap.put(varbitId, runeliteQuest);
                            log.trace("Mapped varbit {} to quest {}", varbitId, runeliteQuest.getName());
                        }
                        
                        Field varPlayerField = QuestHelperQuest.class.getDeclaredField("varPlayer");
                        varPlayerField.setAccessible(true);
                        Object varPlayerValue = varPlayerField.get(questHelperQuest);
                        
                        if (varPlayerValue != null) {
                            // Get the ID from the QuestVarPlayer enum
                            Field idField = varPlayerValue.getClass().getDeclaredField("id");
                            idField.setAccessible(true);
                            int varPlayerId = (Integer) idField.get(varPlayerValue);
                            varPlayerToQuestMap.put(varPlayerId, runeliteQuest);
                            log.trace("Mapped varPlayer {} to quest {}", varPlayerId, runeliteQuest.getName());
                        }
                        
                    } catch (Exception reflectionException) {
                        log.trace("Reflection failed for quest {}: {}", questHelperQuest.getName(), reflectionException.getMessage());
                    }
                }
                
                mapsInitialized = true;
                log.info("Initialized quest maps: {} varbits, {} varPlayers", 
                        varbitToQuestMap.size(), varPlayerToQuestMap.size());
                
            } catch (Exception e) {
                log.error("Error initializing quest variable maps: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Gets a RuneLite Quest by its ID.
     * 
     * @param questId The quest ID
     * @return The Quest object, or null if not found
     */
    private static Quest getQuestById(int questId) {
        try {
            for (Quest quest : Quest.values()) {
                if (quest.getId() == questId) {
                    return quest;
                }
            }
        } catch (Exception e) {
            log.trace("Error finding quest by ID {}: {}", questId, e.getMessage());
        }
        return null;
    }
    
    /**
     * Gets the Quest associated with a varbit ID.
     * 
     * @param varbitId The varbit ID
     * @return The associated Quest, or null if none found
     */
    public static Quest getQuestByVarbit(int varbitId) {
        initializeQuestMaps();
        return varbitToQuestMap.get(varbitId);
    }
    
    /**
     * Gets the Quest associated with a varPlayer ID.
     * 
     * @param varPlayerId The varPlayer ID
     * @return The associated Quest, or null if none found
     */
    public static Quest getQuestByVarPlayer(int varPlayerId) {
        initializeQuestMaps();
        return varPlayerToQuestMap.get(varPlayerId);
    }
    
    /**
     * Gets the varbit ID associated with a quest.
     * 
     * @param quest The quest to look up
     * @return The varbit ID, or null if none found
     */
    public static Integer getVarbitIdByQuest(Quest quest) {
        initializeQuestMaps();
        return varbitToQuestMap.entrySet().stream()
            .filter(entry -> entry.getValue().equals(quest))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Gets the varPlayer ID associated with a quest.
     * 
     * @param quest The quest to look up
     * @return The varPlayer ID, or null if none found
     */
    public static Integer getVarPlayerIdByQuest(Quest quest) {
        initializeQuestMaps();
        return varPlayerToQuestMap.entrySet().stream()
            .filter(entry -> entry.getValue().equals(quest))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Gets the QuestHelperPlugin instance.
     * 
     * @return The QuestHelperPlugin instance, or null if not available
     */
    private static QuestHelperPlugin getQuestHelperPlugin() {
        try {
            return (QuestHelperPlugin) Microbot.getPluginManager().getPlugins().stream()
                    .filter(plugin -> plugin instanceof QuestHelperPlugin && Microbot.getPluginManager().isPluginEnabled(plugin))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.trace("Error getting QuestHelper plugin: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the currently selected quest from QuestHelperPlugin if available.
     * 
     * @return The currently selected QuestHelper, or null if none selected or plugin unavailable
     */
    private static QuestHelper getSelectedQuestHelper() {
        QuestHelperPlugin plugin = getQuestHelperPlugin();
        if (plugin != null) {
            return plugin.getSelectedQuest();
        }
        return null;
    }
    
    /**
     * Gets the currently active RuneLite Quest from QuestHelperPlugin.
     * Maps QuestHelper to RuneLite Quest objects using QuestHelperQuest info.
     * 
     * @return The currently active Quest, or null if none selected or not mappable
     */
    public static Quest getCurrentlyActiveQuest() {
        try {
            QuestHelper selectedHelper = getSelectedQuestHelper();
            if (selectedHelper == null) {
                return null;
            }
            
            // Find the corresponding QuestHelperQuest enum entry
            for (QuestHelperQuest questHelperQuest : QuestHelperQuest.values()) {
                if (questHelperQuest.getQuestHelper() != null && 
                    questHelperQuest.getQuestHelper().getClass().equals(selectedHelper.getClass())) {
                    return getQuestById(questHelperQuest.getId());
                }
            }
            
            log.trace("No RuneLite Quest found for selected QuestHelper: {}", selectedHelper.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            log.trace("Error getting currently active quest: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public void handleEvent(Object event, CacheOperations<Quest, QuestState> cache) {
        if (event instanceof VarbitChanged) {
            handleVarbitChanged((VarbitChanged) event, cache);
        } else if (event instanceof ChatMessage) {
            handleChatMessage((ChatMessage) event, cache);
        }
    }
    
    private void handleVarbitChanged(VarbitChanged event, CacheOperations<Quest, QuestState> cache) {
        try {
            initializeQuestMaps(); // Ensure maps are initialized
            
            // Check if the changed varbit/varPlayer corresponds to a known quest
            Quest affectedQuest = null;
            
            // Check varbit mapping
            if (event.getVarbitId() > 0) {
                affectedQuest = varbitToQuestMap.get(event.getVarbitId());
                if (affectedQuest != null) {
                    log.debug("VarbitChanged - Detected quest {} affected by varbit {}", 
                            affectedQuest.getName(), event.getVarbitId());
                }
            }
            
            // Check varPlayer mapping (VarbitChanged can also affect varPlayers)
            if (affectedQuest == null && event.getVarpId() > 0) {
                affectedQuest = varPlayerToQuestMap.get(event.getVarpId());
                if (affectedQuest != null) {
                    log.debug("VarbitChanged - Detected quest {} affected by varPlayer {}", 
                            affectedQuest.getName(), event.getVarpId());
                }
            }
            
            // If a specific quest is affected, trigger its update
            if (affectedQuest != null) {
                updateQuestAsync(affectedQuest, cache);
            } else {
                // Fallback: check tracked quest or currently active quest
                Quest questToCheck = trackedQuest != null ? trackedQuest : getCurrentlyActiveQuest();
                if (questToCheck != null) {
                    log.trace("VarbitChanged - Checking tracked/active quest: {}", questToCheck.getName());
                    updateQuestAsync(questToCheck, cache);
                }
            }
            
        } catch (Exception e) {
            log.error("Error handling VarbitChanged event for quests: {}", e.getMessage(), e);
        }
    }
    
    private void handleChatMessage(ChatMessage chatMessage, CacheOperations<Quest, QuestState> cache) {
        try {
            if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE) {
                return;
            }
            
            String message = chatMessage.getMessage();
            if (message == null) {
                return;
            }
            
            // Check for quest-related messages
            if (message.contains("You have completed") || 
                message.contains("Quest complete") ||
                message.contains("quest") || 
                message.contains("Quest")) {
                //should not be need, the varbitChanged event should handle this already, only dont work for qeust not in the enum QuestHelperQuest
                log.debug("ChatMessage - Quest-related message detected: {}", message);                
                // Update tracked quest if any
                Quest questToUpdate = trackedQuest != null ? trackedQuest : getCurrentlyActiveQuest();
                if (questToUpdate != null) {
                    updateQuestAsync(questToUpdate, cache);                                       
                }
            }
            
        } catch (Exception e) {
            log.error("Error handling ChatMessage event for quests: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Asynchronously updates a quest's state in the cache.
     * 
     * @param quest The quest to update
     * @param cache The cache operations interface
     */
    private void updateQuestAsync(Quest quest, CacheOperations<Quest, QuestState> cache) {
        Microbot.getClientThread().invokeLater(() -> {
            try {
                if (Microbot.getClient() == null) {
                    return;
                }
                
                QuestState oldState = cache.get(quest);
                QuestState newState = quest.getState(Microbot.getClient());
                
                if (oldState != newState) {
                    log.debug("\n\tdetection Quest state changed update cache\n\t\t {}: cached: {} -> client: {} ", 
                            quest.getName(), oldState, newState);
                    cache.put(quest, newState);
                    
                    // If quest is now complete and was being tracked, clear tracking
                    if (newState == QuestState.FINISHED && trackedQuest == quest) {
                        log.debug("Quest completed, clearing tracking: {}", quest.getName());
                        trackedQuest = null;
                    }
                }
            } catch (Exception e) {
                log.error("Error updating quest state for {}: {}", quest.getName(), e.getMessage());
            }
        });
    }
    
    @Override
    public Class<?>[] getHandledEventTypes() {
        return new Class<?>[]{VarbitChanged.class, ChatMessage.class};
    }
    
    @Override
    public void onAttach(CacheOperations<Quest, QuestState> cache) {
        log.debug("QuestUpdateStrategy attached to cache");
    }
    
    @Override
    public void onDetach(CacheOperations<Quest, QuestState> cache) {
        log.debug("QuestUpdateStrategy detached from cache");
    }
}
