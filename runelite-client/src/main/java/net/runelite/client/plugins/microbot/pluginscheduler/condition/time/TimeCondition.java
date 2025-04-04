package net.runelite.client.plugins.microbot.pluginscheduler.condition.time;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.ConditionType;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Abstract base class for all time-based conditions.
 * Provides common functionality for time calculations and event handling.
 */
@Slf4j
@EqualsAndHashCode(callSuper = false)
public abstract class TimeCondition implements Condition {
    
    @Getter
    protected boolean registered = false;
    protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * Gets the current date and time in the system default time zone
     * 
     * @return The current ZonedDateTime
     */
    protected ZonedDateTime getNow() {
        return ZonedDateTime.now(ZoneId.systemDefault());
    }
    
    @Override
    public ConditionType getType() {
        return ConditionType.TIME;
    }
    
      
    
    /**
     * Default GameTick handler that subclasses can override
     */    
    public void onGameTick(GameTick gameTick) {
        // Default implementation does nothing
    }
    
    @Override
    public void reset() {
        reset(false);
    }
    /**
     * Gets the next time this time condition will be satisfied.
     * Subclasses should override this to provide specific trigger time calculation.
     * 
     * @return Optional containing the next trigger time, or empty if not applicable
     */
    @Override
    public Optional<ZonedDateTime> getNextTriggerTime() {
        // Base implementation for time conditions
        // Concrete subclasses should override this
        return Optional.empty();
    }

    /**
     * Gets the duration until the next trigger time.
     * 
     * @return Optional containing the duration until next trigger, or empty if not applicable
     */
    public Optional<Duration> getDurationUntilNextTrigger() {
        Optional<ZonedDateTime> nextTrigger = getNextTriggerTime();
        if (nextTrigger.isPresent()) {
            ZonedDateTime now = getNow();
            ZonedDateTime triggerTime = nextTrigger.get();
            
            // If trigger time is in the future, return the duration
            if (triggerTime.isAfter(now)) {
                return Optional.of(Duration.between(now, triggerTime));
            }
        }
        return Optional.empty();
    }

    /**
     * Calculates progress percentage toward next trigger.
     * 
     * @return Progress percentage (0-100) toward next trigger time
     */
    @Override
    public double getProgressPercentage() {
        // If already satisfied, return 100%
        if (isSatisfied()) {
            return 100.0;
        }
        
        // Calculate progress based on time until next trigger
        Optional<ZonedDateTime> nextTrigger = getNextTriggerTime();
        if (nextTrigger.isPresent()) {
            ZonedDateTime now = getNow();
            ZonedDateTime triggerTime = nextTrigger.get();
            
            // If trigger is in the past, it's either 0% or 100%
            if (!triggerTime.isAfter(now)) {
                return isSatisfied() ? 100.0 : 0.0;
            }
            
            // Calculate progress based on reference point and trigger time
            // This requires a reference point which subclasses need to provide
            return calculateProgressTowardTrigger(now, triggerTime);
        }
        
        // Default behavior
        return 0.0;
    }

    /**
     * Calculates progress percentage toward a specific trigger time.
     * Base implementation for subclasses to override with specific calculations.
     * 
     * @param now Current time
     * @param triggerTime Target trigger time
     * @return Progress percentage (0-100)
     */
    protected double calculateProgressTowardTrigger(ZonedDateTime now, ZonedDateTime triggerTime) {
        // Default implementation - subclasses should override
        return 0.0;
    }

    /**
     * Check if this condition uses randomization
     * @return true if randomization is enabled, false otherwise
     */
    public boolean isUseRandomization() {
        return false; // Default implementation, subclasses should override if needed
    }
    
}