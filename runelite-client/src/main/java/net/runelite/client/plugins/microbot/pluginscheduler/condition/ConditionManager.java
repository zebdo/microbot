package net.runelite.client.plugins.microbot.pluginscheduler.condition;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.AndCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.NotCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.OrCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.resource.ResourceCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.SingleTriggerTimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeCondition;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages multiple conditions for script execution.
 * Supports AND/OR combinations of conditions and listens to relevant events
 * to update condition statuses.
 */
@Slf4j
public class ConditionManager {
    
    //private final List<Condition> userConditions = new ArrayList<>();    
    
    private LogicalCondition pluginCondition = null;
    @Getter
    private LogicalCondition userLogicalCondition;
    private final EventBus eventBus;
    private boolean eventsRegistered = false;
    public ConditionManager() {
        this.eventBus = Microbot.getEventBus();
        userLogicalCondition = new AndCondition();
        registerEvents();
    }
    public void setPluginCondition(LogicalCondition condition) {
        pluginCondition = condition;
    }
    public LogicalCondition getPluginCondition() {
        return pluginCondition;
    }    
    public List<Condition> getConditions() {
        List<Condition> conditions = new ArrayList<>();
        if (pluginCondition != null) {
            conditions.addAll(pluginCondition.getConditions());
        }
        conditions.addAll(userLogicalCondition.getConditions());
        return conditions;
    }
    public boolean hasConditions() {
        return !getConditions().isEmpty();
    }
    public List<TimeCondition> getTimeConditions() {
        List<TimeCondition> timeConditions = new ArrayList<>();
        for (Condition condition : getConditions()) {
            if (condition instanceof TimeCondition) {
                timeConditions.add((TimeCondition) condition);
            }
        }
        return timeConditions;
    }
    public LogicalCondition getUserCondition() {
        return userLogicalCondition;
    }
    public  void clearUserConditions() {
        userLogicalCondition.getConditions().clear();
    }
    public boolean areConditionsMet() {
        boolean userConditionsMet = false;
    
        if (userLogicalCondition == null){
            requiresAll();            
        }
        userConditionsMet = userLogicalCondition.isSatisfied();                            
        if (pluginCondition != null) {          

            return userConditionsMet && pluginCondition.isSatisfied();
        }
        return userConditionsMet;                                                
    }
   /**
     * Returns a list of conditions that were defined by the user (not plugin-defined)
     */
    public List<Condition> getUserConditions() {
        if (userLogicalCondition == null) {
            return new ArrayList<>();
        }
        
        List<Condition> result = new ArrayList<>();
        for (Condition condition : userLogicalCondition.getConditions()) {
            if (!isPluginDefinedCondition(condition)) {
                result.add(condition);
            }
        }
        return result;
    }
    public void registerEvents() {
        if (eventsRegistered) {
            return;
        }
        log.info(getDescription() + " Registering events");
        eventBus.register(this);
        eventsRegistered = true;
    }
    
    public void unregisterEvents() {
        if (!eventsRegistered) {
            return;
        }
        log.info(getDescription() + " Unregistering events");
        eventBus.unregister(this);
        eventsRegistered = false;
    }
    
   
    public void setRequireAll() {
        userLogicalCondition = new AndCondition();
        setUserLogicalCondition(userLogicalCondition);
        
    }
    
    public void setRequireAny() {
        userLogicalCondition = new OrCondition();        
        setUserLogicalCondition(userLogicalCondition);
    }
    
   
    
    public String getDescription() {
        
                  
        StringBuilder sb;
        if (requiresAny()){
            sb = new StringBuilder("ANY of: (");
        }else{
            sb = new StringBuilder("ALL of: (");
        }
        List<Condition> userConditions = userLogicalCondition.getConditions();
        if (userConditions.isEmpty()) {
            sb.append("No conditions");
        }else{
            for (int i = 0; i < userConditions.size(); i++) {
                if (i > 0) sb.append(" OR ");
                sb.append(userConditions.get(i).getDescription());
            }
        }
        sb.append(")");

        if ( this.pluginCondition!= null) {
            sb.append(" AND : ");
            sb.append(this.pluginCondition.getDescription());            
        }
        return sb.toString();      
        
    }
    
   /**
     * Checks if the manager requires all conditions to be met (AND logic)
     */
    public boolean userConditionRequiresAll() {

        return userLogicalCondition instanceof AndCondition;
    }
    public boolean userConditionRequiresAny() {
        return userLogicalCondition instanceof OrCondition;
    }
    public boolean requiresAll() {
        return this.getFullLogicalCondition() instanceof AndCondition;
    }
    public boolean requiresAny() {
        return this.getFullLogicalCondition() instanceof OrCondition;
    }
    public void reset() {
        userLogicalCondition.reset();
        if (pluginCondition != null) {
            pluginCondition.reset();
        }
        
    }
    public void reset(boolean randomize) {
        userLogicalCondition.reset(randomize);
        if (pluginCondition != null) {
            pluginCondition.reset(randomize);
        }
    }

    /**
     * Checks if a condition is a plugin-defined condition that shouldn't be edited by users.
     */
    public boolean isPluginDefinedCondition(Condition condition) {
        // If there are no plugin-defined conditions, return false
        if (pluginCondition == null) {
            return false;
        }
        if (condition instanceof LogicalCondition) {
            // If the condition is a logical condition, check if it's part of the plugin condition
            if (pluginCondition == condition) {
                return true;
            }            
        }
        // Checfk if the condition is contained in the plugin condition hierarchy
        return pluginCondition.contains(condition);
    }

    

    /**
     * Recursively searches for and removes a condition from nested logical conditions.
     * 
     * @param parent The logical condition to search within
     * @param target The condition to remove
     * @return true if the condition was found and removed, false otherwise
     */
    private boolean removeFromNestedCondition(LogicalCondition parent, Condition target) {
        // Search each child of the parent logical condition
        for (int i = 0; i < parent.getConditions().size(); i++) {
            Condition child = parent.getConditions().get(i);
            
            // If this child is itself a logical condition, search within it
            if (child instanceof LogicalCondition) {
                LogicalCondition logicalChild = (LogicalCondition) child;
                
                // First check if the target is a direct child of this logical condition
                if (logicalChild.getConditions().remove(target)) {
                    // If removing the condition leaves the logical condition empty, remove it too
                    if (logicalChild.getConditions().isEmpty()) {
                        parent.getConditions().remove(i);
                    }
                    return true;
                }
                
                // If not a direct child, recurse into the logical child
                if (removeFromNestedCondition(logicalChild, target)) {
                    // If removing the condition leaves the logical condition empty, remove it too
                    if (logicalChild.getConditions().isEmpty()) {
                        parent.getConditions().remove(i);
                    }
                    return true;
                }
            }
            // Special case for NotCondition
            else if (child instanceof NotCondition) {
                NotCondition notChild = (NotCondition) child;
                
                // If the NOT condition wraps our target, remove the whole NOT condition
                if (notChild.getCondition() == target) {
                    parent.getConditions().remove(i);
                    return true;
                }
                
                // If the NOT condition wraps a logical condition, search within that
                if (notChild.getCondition() instanceof LogicalCondition) {
                    LogicalCondition wrappedLogical = (LogicalCondition) notChild.getCondition();
                    if (removeFromNestedCondition(wrappedLogical, target)) {
                        // If removing the condition leaves the logical condition empty, remove the NOT condition too
                        if (wrappedLogical.getConditions().isEmpty()) {
                            parent.getConditions().remove(i);
                        }
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Sets the user's logical condition structure
     * 
     * @param logicalCondition The logical condition to set as the user structure
     */
    public void setUserLogicalCondition(LogicalCondition logicalCondition) {
        this.userLogicalCondition = logicalCondition;
    }

    /**
     * Gets the user's logical condition structure
     * 
     * @return The current user logical condition, or null if none exists
     */
    public LogicalCondition getUserLogicalCondition() {
        return this.userLogicalCondition;
    }

    
    public boolean addToLogicalStructure(LogicalCondition parent, Condition toAdd) {
        // Try direct addition first
        if (parent.getConditions().add(toAdd)) {
            return true;
        }
        
        // Try to add to child logical conditions
        for (Condition child : parent.getConditions()) {
            if (child instanceof LogicalCondition) {
                if (addToLogicalStructure((LogicalCondition) child, toAdd)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Recursively removes a condition from a logical structure
     */
    public boolean removeFromLogicalStructure(LogicalCondition parent, Condition toRemove) {
        // Try direct removal first
        if (parent.getConditions().remove(toRemove)) {
            return true;
        }
        
        // Try to remove from child logical conditions
        for (Condition child : parent.getConditions()) {
            if (child instanceof LogicalCondition) {
                if (removeFromLogicalStructure((LogicalCondition) child, toRemove)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Makes sure there's a valid user logical condition to work with
     */
    private void ensureUserLogicalExists() {
        if (userLogicalCondition == null) {
            userLogicalCondition = new AndCondition();
        }
    }

    /**
     * Checks if the condition exists in either user or plugin logical structures
     */
    public boolean containsCondition(Condition condition) {
        ensureUserLogicalExists();
        
        // Check user conditions
        if (userLogicalCondition.contains(condition)) {
            return true;
        }
        
        // Check plugin conditions
        return pluginCondition != null && pluginCondition.contains(condition);
    }

    /**
     * Finds which logical condition contains the given condition
     */
    public LogicalCondition findContainingLogical(Condition condition) {
        ensureUserLogicalExists();
        
        // Check user logical first
        LogicalCondition result = userLogicalCondition.findContainingLogical(condition);
        if (result != null) {
            return result;
        }
        
        // Check plugin logical if exists
        if (pluginCondition != null) {
            return pluginCondition.findContainingLogical(condition);
        }
        
        return null;
    }

    /**
     * Adds a condition to the specified logical condition, or to the user root if none specified
     */
    public void addCondition(Condition condition, LogicalCondition targetLogical) {
        ensureUserLogicalExists();
        
        // If no target specified, add to user root
        if (targetLogical == null) {
            userLogicalCondition.addCondition(condition);
            return;
        }
        
        // Otherwise, add to the specified logical
        targetLogical.addCondition(condition);
    }

    /**
     * Adds a condition to the user logical root
     */
    public void addCondition(Condition condition) {
        addCondition(condition, userLogicalCondition);
    }

    /**
     * Removes a condition from any location in the logical structure
     */
    public boolean removeCondition(Condition condition) {
        ensureUserLogicalExists();
        
        // Don't allow removing plugin conditions
        if (isPluginDefinedCondition(condition)) {
            log.warn("Attempted to remove a plugin-defined condition");
            return false;
        }
        
        // Remove from user logical structure
        if (userLogicalCondition.removeCondition(condition)) {
            return true;
        }
        
        log.warn("Condition not found in any logical structure");
        return false;
    }

    /**
     * Gets the root logical condition that should be used for the current UI operation
     */
    public LogicalCondition getFullLogicalCondition() {
        // First check if there are plugin conditions
        if (pluginCondition != null && !pluginCondition.getConditions().isEmpty()) {
            // Need to combine user and plugin conditions with AND logic
            AndCondition combinedRoot = new AndCondition();
            
            // Add user logical if it has conditions
            if (userLogicalCondition != null && !userLogicalCondition.getConditions().isEmpty()) {
                combinedRoot.addCondition(userLogicalCondition);
                // Add plugin logical
                combinedRoot.addCondition(pluginCondition);
                return combinedRoot;
            }else {
                // If no user conditions, just return plugin condition
                return pluginCondition;
            }                                                
        }
        
        // If no plugin conditions, just return user logical
        return userLogicalCondition;
    }
    public LogicalCondition getFullLogicalUserCondition() {
        return userLogicalCondition;
    }
    public LogicalCondition getFullLogicalPluginCondition() {
        return pluginCondition;
    }

    /**
     * Checks if a condition is a SingleTriggerTimeCondition
     * 
     * @param condition The condition to check
     * @return true if the condition is a SingleTriggerTimeCondition
     */
    private boolean isSingleTriggerCondition(Condition condition) {
        return condition instanceof SingleTriggerTimeCondition;
    }
    public List<SingleTriggerTimeCondition> getTriggerdOnTimeConditions(){
        List<SingleTriggerTimeCondition> result = new ArrayList<>();
        for (Condition condition : userLogicalCondition.getConditions()) {
            if (isSingleTriggerCondition(condition)) {
                SingleTriggerTimeCondition singleTrigger = (SingleTriggerTimeCondition) condition;
                if (singleTrigger.isHasTriggered()) {
                    result.add(singleTrigger);
                }
            }
        }
        if (pluginCondition != null) {
            for (Condition condition : pluginCondition.getConditions()) {
                if (isSingleTriggerCondition(condition)) {
                    SingleTriggerTimeCondition singleTrigger = (SingleTriggerTimeCondition) condition;
                    if (singleTrigger.isHasTriggered()) {
                        result.add(singleTrigger);
                    }
                }
            }
        }
        return result;
    }
    /**
     * Checks if this condition manager contains any SingleTriggerTimeCondition that
     * can no longer trigger (has already triggered)
     * 
     * @return true if at least one single-trigger condition has already triggered
     */
    public boolean hasTriggeredOneTimeConditions() {
        // Check user conditions first
        for (Condition condition : getUserLogicalCondition().getConditions()) {
            if (isSingleTriggerCondition(condition)) {
                SingleTriggerTimeCondition singleTrigger = (SingleTriggerTimeCondition) condition;
                if (singleTrigger.isHasTriggered()) {
                    return true;
                }
            }
        }
        
        // Then check plugin conditions if present
        if (pluginCondition != null) {
            for (Condition condition : pluginCondition.getConditions()) {
                if (isSingleTriggerCondition(condition)) {
                    SingleTriggerTimeCondition singleTrigger = (SingleTriggerTimeCondition) condition;
                    if (singleTrigger.isHasTriggered()) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

     /**
     * Checks if the logical structure cannot trigger again due to triggered one-time conditions.
     * Considers the nested AND/OR condition structure to determine if future triggering is possible.
     * 
     * @return true if the structure cannot trigger again due to one-time conditions
     */
    public boolean cannotTriggerDueToOneTimeConditions() {
        // If there are no one-time conditions, the structure can always trigger again
        if (!hasAnyOneTimeConditions()) {
            return false;
        }

        // Start evaluation at the root of the condition tree
        return !canLogicalStructureTriggerAgain(getFullLogicalCondition());
    }

    /**
     * Recursively evaluates if a logical structure can trigger again based on one-time conditions.
     * 
     * @param logical The logical condition to evaluate
     * @return true if the logical structure can trigger again, false otherwise
     */
    private boolean canLogicalStructureTriggerAgain(LogicalCondition logical) {
        if (logical instanceof AndCondition) {
            // For AND logic, if any direct child one-time condition has triggered,
            // the entire AND branch cannot trigger again
            for (Condition condition : logical.getConditions()) {
                if (condition instanceof SingleTriggerTimeCondition) {
                    SingleTriggerTimeCondition singleTrigger = (SingleTriggerTimeCondition) condition;
                    if (singleTrigger.isHasTriggered()) {
                        // One triggered one-time condition in an AND means the branch can't trigger
                        return false;
                    }
                }else if (condition instanceof ResourceCondition) {
                    ResourceCondition resourceCondition = (ResourceCondition) condition;
                    
                }
                else if (condition instanceof LogicalCondition) {
                    // Recursively check nested logic
                    if (!canLogicalStructureTriggerAgain((LogicalCondition) condition)) {
                        // If a nested branch can't trigger, this AND branch can't trigger
                        return false;
                    }
                }
            }
            // If we get here, all branches can still trigger
            return true;
        } else if (logical instanceof OrCondition) {
            // For OR logic, if any one-time condition hasn't triggered yet,
            // the OR branch can still trigger
            boolean anyCanTrigger = false;
            
            for (Condition condition : logical.getConditions()) {
                if (condition instanceof SingleTriggerTimeCondition) {
                    SingleTriggerTimeCondition singleTrigger = (SingleTriggerTimeCondition) condition;
                    if (!singleTrigger.isHasTriggered()) {
                        // Found an untriggered one-time condition, so this branch can trigger
                        return true;
                    }
                } else if (condition instanceof LogicalCondition) {
                    // Recursively check nested logic
                    if (canLogicalStructureTriggerAgain((LogicalCondition) condition)) {
                        // If a nested branch can trigger, this OR branch can trigger
                        return true;
                    }
                } else {
                    // Regular non-one-time conditions can always trigger
                    anyCanTrigger = true;
                }
            }
            
            // If there are no one-time conditions in this OR, it can trigger if it has any conditions
            return anyCanTrigger;
        } else {
            // For any other logical condition type (e.g., NOT), assume it can trigger
            return true;
        }
    }
    /**
     * Validates if the current condition structure can be triggered again
     * based on the status of one-time conditions in the logical structure.
     * 
     * @return true if the condition structure can be triggered again
     */
    public boolean canTriggerAgain() {
        return !cannotTriggerDueToOneTimeConditions();
    }

    /**
     * Checks if this condition manager contains any SingleTriggerTimeConditions
     * 
     * @return true if at least one single-trigger condition exists
     */
    public boolean hasAnyOneTimeConditions() {
        // Check user conditions
        for (Condition condition : getUserLogicalCondition().getConditions()) {
            if (isSingleTriggerCondition(condition)) {
                return true;
            }
        }
        
        // Check plugin conditions if present
        if (pluginCondition != null) {
            for (Condition condition : pluginCondition.getConditions()) {
                if (isSingleTriggerCondition(condition)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    /**
     * Calculates overall progress percentage across all conditions.
     * This respects the logical structure of conditions.
     * Returns 0 if progress cannot be determined.
     */
    private double getFullRootConditionProgress() {
        // If there are no conditions, no progress to report -> nothing can be satisfied
        if ( getConditions().isEmpty()) {
            return 0.0;
        }
        
        // If using logical root condition, respect its logical structure
        LogicalCondition rootLogical = getFullLogicalCondition();
        if (rootLogical != null) {
            return rootLogical.getProgressPercentage();
        }
        
        // Fallback for direct condition list: calculate based on AND/OR logic
        boolean requireAll = requiresAll();
        List<Condition> conditions = getConditions();
        
        if (requireAll) {
            // For AND logic, use the minimum progress (weakest link)
            return conditions.stream()
                .mapToDouble(Condition::getProgressPercentage)
                .min()
                .orElse(0.0);
        } else {
            // For OR logic, use the maximum progress (strongest link)
            return conditions.stream()
                .mapToDouble(Condition::getProgressPercentage)
                .max()
                .orElse(0.0);
        }
    }
    /**
     * Gets the overall condition progress
     * Integrates both standard progress and single-trigger time conditions
     */
    public double getFullConditionProgress() {
        // First check for regular condition progress
        double stopProgress = getFullRootConditionProgress();
        
        // Then check if we have any single-trigger conditions
        boolean hasOneTime = hasAnyOneTimeConditions();
        if (hasOneTime) {
            // If all one-time conditions have triggered, return 100%
            if (canTriggerAgain()) {
                return 100.0;
            }
            
            // If no standard progress but we have one-time conditions that haven't
            // all triggered, return progress based on closest one-time condition
            if (stopProgress == 0.0) {
                return calculateClosestOneTimeProgress();
            }
        }
        
        // Return the standard progress
        return stopProgress;
    }

    /**
     * Calculates progress based on the closest one-time condition to triggering
     */
    private double calculateClosestOneTimeProgress() {
        // Find the single-trigger condition that's closest to triggering
        double maxProgress = 0.0;
        
        for (Condition condition : getConditions()) {
            if (condition instanceof SingleTriggerTimeCondition) {
                SingleTriggerTimeCondition singleTrigger = (SingleTriggerTimeCondition) condition;
                if (!singleTrigger.isHasTriggered()) {
                    double progress = singleTrigger.getProgressPercentage();
                    maxProgress = Math.max(maxProgress, progress);
                }
            }
        }
        
        return maxProgress;
    }
    /**
     * Gets the next time any condition in the structure will trigger.
     * This recursively examines the logical condition tree and finds the earliest trigger time.
     * 
     * @return Optional containing the earliest next trigger time, or empty if none available
     */
    public Optional<ZonedDateTime> getNextTriggerTime() {
        // Start at the root of the condition tree
        return getNextTriggerTimeForLogical(getFullLogicalCondition());
    }
    /**
     * Gets the next time any condition in the structure will trigger.
     * This recursively examines the logical condition tree and finds the earliest trigger time.
     * 
     * @return Optional containing the earliest next trigger time, or empty if none available
     */
    public Optional<ZonedDateTime> getNextTriggerTimeBasedOnUserConditions() {
        // Start at the root of the condition tree
        return getNextTriggerTimeForLogical(getFullLogicalUserCondition());
    }
    /**
     * Determines the next time a plugin should be triggered based on the plugin's set conditions.
     * This method evaluates the full logical condition tree for the plugin to calculate
     * when the next condition-based execution should occur.
     *
     * @return An Optional containing the ZonedDateTime of the next trigger time if one exists,
     *         or an empty Optional if no future trigger time can be determined
     */
    public Optional<ZonedDateTime> getNextTriggerTimeBasedOnPluginConditions() {
        // Start at the root of the condition tree
        return getNextTriggerTimeForLogical(getFullLogicalPluginCondition());
    }


    /**
     * Recursively finds the earliest next trigger time within a logical condition.
     * 
     * @param logical The logical condition to examine
     * @return Optional containing the earliest next trigger time, or empty if none available
     */
    private Optional<ZonedDateTime> getNextTriggerTimeForLogical(LogicalCondition logical) {
        if (logical == null || logical.getConditions().isEmpty()) {
            return Optional.empty();
        }
        
        ZonedDateTime earliestTrigger = null;
        
        for (Condition condition : logical.getConditions()) {
            Optional<ZonedDateTime> nextTrigger;
            
            if (condition instanceof LogicalCondition) {
                // Recursively check nested logical conditions
                nextTrigger = getNextTriggerTimeForLogical((LogicalCondition) condition);
            } else {
                // Get trigger time from individual condition
                nextTrigger = condition.getNextTriggerTime();
            }
            
            // Update earliest trigger if this one is earlier
            if (nextTrigger.isPresent()) {
                ZonedDateTime triggerTime = nextTrigger.get();
                if (earliestTrigger == null || triggerTime.isBefore(earliestTrigger)) {
                    earliestTrigger = triggerTime;
                }
            }
        }
        
        return earliestTrigger != null ? Optional.of(earliestTrigger) : Optional.empty();
    }

    /**
     * Gets the duration until the next condition trigger.
     * 
     * @return Optional containing the duration until next trigger, or empty if none available
     */
    public Optional<Duration> getDurationUntilNextTrigger() {
        Optional<ZonedDateTime> nextTrigger = getNextTriggerTime();
        if (nextTrigger.isPresent()) {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            ZonedDateTime triggerTime = nextTrigger.get();
            
            // If trigger time is in the future, return the duration
            if (triggerTime.isAfter(now)) {
                return Optional.of(Duration.between(now, triggerTime));
            }
        }
        return Optional.empty();
    }

    /**
     * Formats the next trigger time as a human-readable string.
     * 
     * @return A string representing when the next condition will trigger, or "No upcoming triggers" if none
     */
    public String getNextTriggerTimeString() {
        Optional<ZonedDateTime> nextTrigger = getNextTriggerTime();
        if (nextTrigger.isPresent()) {
            ZonedDateTime triggerTime = nextTrigger.get();
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            
            // Format nicely depending on how far in the future
            Duration timeUntil = Duration.between(now, triggerTime);
            long seconds = timeUntil.getSeconds();
            
            if (seconds < 0) {
                return "Already triggered";
            } else if (seconds < 60) {
                return String.format("Triggers in %d seconds", seconds);
            } else if (seconds < 3600) {
                return String.format("Triggers in %d minutes %d seconds", 
                        seconds / 60, seconds % 60);
            } else if (seconds < 86400) { // Less than a day
                return String.format("Triggers in %d hours %d minutes", 
                        seconds / 3600, (seconds % 3600) / 60);
            } else {
                // More than a day away, use date format
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d 'at' HH:mm");
                return "Triggers on " + triggerTime.format(formatter);
            }
        }
        
        return "No upcoming triggers";
    }

    /**
     * Gets the overall progress percentage toward the next trigger time.
     * For logical conditions, this respects the logical structure.
     * 
     * @return Progress percentage (0-100)
     */
    public double getProgressTowardNextTrigger() {
        LogicalCondition rootLogical = getFullLogicalCondition();
        
        if (rootLogical instanceof AndCondition) {
            // For AND logic, use the minimum progress (we're only as close as our furthest condition)
            return rootLogical.getConditions().stream()
                    .mapToDouble(Condition::getProgressPercentage)
                    .min()
                    .orElse(0.0);
        } else {
            // For OR logic, use the maximum progress (we're as close as our closest condition)
            return rootLogical.getConditions().stream()
                    .mapToDouble(Condition::getProgressPercentage)
                    .max()
                    .orElse(0.0);
        }
    }

    @Subscribe(priority = -1)
    public void onStatChanged(StatChanged event) {
        // Propagate event to all conditions
        for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onStatChanged(event);
            } catch (Exception e) {
                log.error("Error in condition {} during StatChanged event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onStatChanged(event);
            } catch (Exception e) {
                log.error("Error in plugin condition during StatChanged event: {}", 
                    e.getMessage(), e);
            }
        }
    }
    
    @Subscribe(priority = -1)
    public void onItemContainerChanged(ItemContainerChanged event) {
         // Propagate event to all conditions
         log.info("ItemContainerChanged event" + event);
         for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onItemContainerChanged(event);
            } catch (Exception e) {
                log.error("Error in condition {} during ItemContainerChanged event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onItemContainerChanged(event);
            } catch (Exception e) {
                log.error("Error in plugin condition during ItemContainerChanged event: {}", 
                    e.getMessage(), e);
            }
        }
    }
    @Subscribe(priority = -1)
    public void onGameTick(GameTick gameTick) {
        // Propagate event to all conditions
        for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onGameTick(gameTick);
            } catch (Exception e) {
                log.error("Error in condition {} during GameTick event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onGameTick(gameTick);
            } catch (Exception e) {
                log.error("Error in plugin condition during GameTick event: {}", 
                    e.getMessage(), e);
            }
        }
    }

    @Subscribe(priority = -1)
    public void onGroundObjectSpawned(GroundObjectSpawned event) {
        // Propagate event to all conditions
        for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onGroundObjectSpawned(event);
            } catch (Exception e) {
                log.error("Error in condition {} during GroundItemSpawned event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onGroundObjectSpawned(event);
            } catch (Exception e) {
                log.error("Error in plugin condition during GroundItemSpawned event: {}", 
                    e.getMessage(), e);
            }
        }
    }

    @Subscribe(priority = -1)
    public void onGroundObjectDespawned(GroundObjectDespawned event) {
        for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onGroundObjectDespawned(event);
            } catch (Exception e) {
                log.error("Error in condition {} during GroundItemDespawned event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onGroundObjectDespawned(event);
            } catch (Exception e) {
                log.error("Error in plugin condition during GroundItemDespawned event: {}", 
                    e.getMessage(), e);
            }
        }
    }

    @Subscribe(priority = -1)
    public void onMenuOptionClicked(MenuOptionClicked event) {
        for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onMenuOptionClicked(event);
            } catch (Exception e) {
                log.error("Error in condition {} during MenuOptionClicked event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onMenuOptionClicked(event);
            } catch (Exception e) {
                log.error("Error in plugin condition during MenuOptionClicked event: {}", 
                    e.getMessage(), e);
            }
        }
    }

    @Subscribe(priority = -1)
    public void onChatMessage(ChatMessage event) {
        for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onChatMessage(event);
            } catch (Exception e) {
                log.error("Error in condition {} during ChatMessage event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onChatMessage(event);
            } catch (Exception e) {
                log.error("Error in plugin condition during ChatMessage event: {}", 
                    e.getMessage(), e);
            }
        }
    }

    @Subscribe(priority = -1)
    public void onHitsplatApplied(HitsplatApplied event) {
        for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onHitsplatApplied(event);
            } catch (Exception e) {
                log.error("Error in condition {} during HitsplatApplied event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onHitsplatApplied(event);
            } catch (Exception e) {
                log.error("Error in plugin condition during HitsplatApplied event: {}", 
                    e.getMessage(), e);
            }
        }
    }
    @Subscribe(priority = -1)
	public void onVarbitChanged(VarbitChanged event)
	{
		for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onVarbitChanged(event);
            } catch (Exception e) {
                log.error("Error in condition {} during VarbitChanged event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onVarbitChanged(event);
            } catch (Exception e) {
                log.error("Error in plugin condition during VarbitChanged event: {}", 
                    e.getMessage(), e);
            }
        }      
	}
    @Subscribe(priority = -1)
    void onNpcChanged(NpcChanged event){
        for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onNpcChanged(event);
            } catch (Exception e) {
                log.error("Error in condition {} during NpcChanged event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onNpcChanged(event);
            } catch (Exception e) {
                log.error("Error in plugin condition during NpcChanged event: {}", 
                    e.getMessage(), e);
            }
        }
    }
    @Subscribe(priority = -1)
    void onNpcSpawned(NpcSpawned npcSpawned){
        for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onNpcSpawned(npcSpawned);
            } catch (Exception e) {
                log.error("Error in condition {} during NpcSpawned event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onNpcSpawned(npcSpawned);
            } catch (Exception e) {
                log.error("Error in plugin condition during NpcSpawned event: {}", 
                    e.getMessage(), e);
            }
        }
    }
    @Subscribe(priority = -1)
    void onNpcDespawned(NpcDespawned npcDespawned){
        for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onNpcDespawned(npcDespawned);
            } catch (Exception e) {
                log.error("Error in condition {} during NpcDespawned event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onNpcDespawned(npcDespawned);
            } catch (Exception e) {
                log.error("Error in plugin condition during NpcDespawned event: {}", 
                    e.getMessage(), e);
            }
        }
    }
    @Subscribe(priority = -1)
    void onInteractingChanged(InteractingChanged event){
        for (Condition condition : userLogicalCondition.getConditions()) {
            try {
                condition.onInteractingChanged(event);
            } catch (Exception e) {
                log.error("Error in condition {} during InteractingChanged event: {}", 
                    condition.getDescription(), e.getMessage(), e);
            }
        }
        if (pluginCondition != null) {
            try {
                pluginCondition.onInteractingChanged(event);
            } catch (Exception e) {
                log.error("Error in plugin condition during InteractingChanged event: {}", 
                    e.getMessage(), e);
            }
        }
    }

}