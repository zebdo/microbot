package net.runelite.client.plugins.microbot.pluginscheduler.condition.time;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A time condition that triggers exactly once when the target time is reached.
 * After triggering, this condition remains in a "triggered" state until explicitly reset.
 * Perfect for one-time scheduled events or deadlines.
 */
@Slf4j
public class SingleTriggerTimeCondition extends TimeCondition {
    
    @Getter
    private final ZonedDateTime targetTime;
    
    @Getter
    private boolean hasTriggered = false;
    private boolean hasResetedAfterTrigger = false;
    
    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Creates a condition that triggers once at the specified time
     * 
     * @param targetTime The time at which this condition should trigger
     */
    public SingleTriggerTimeCondition(ZonedDateTime targetTime) {
        this.targetTime = targetTime;
    }
    
    /**
     * Creates a condition that triggers once after the specified delay
     * 
     * @param delaySeconds Number of seconds in the future to trigger
     * @return A new SingleTriggerTimeCondition
     */
    public static SingleTriggerTimeCondition afterDelay(long delaySeconds) {
        ZonedDateTime triggerTime = ZonedDateTime.now(ZoneId.systemDefault())
                .plusSeconds(delaySeconds);
        return new SingleTriggerTimeCondition(triggerTime);
    }
    
    @Override
    public boolean isSatisfied() {
        // If already triggered, return true
        if (hasTriggered) {
            if (!hasResetedAfterTrigger) {
                return true; // Only return true once after triggering                
            }
            return false; // Return false after reset  and we have triggered before
        }
        
        // Check if current time has passed the target time
        ZonedDateTime now = getNow();
        if (now.isAfter(targetTime) || now.isEqual(targetTime)) {
            hasTriggered = true;
            log.debug("SingleTriggerTimeCondition triggered at: {}", now.format(FORMATTER));
            return true;
        }
        
        return false;
    }
    
    @Override
    public String getDescription() {
        String triggerStatus = hasTriggered ? "triggered" : "not yet triggered";
        return String.format("One-time trigger at %s (%s)", 
                targetTime.format(FORMATTER), triggerStatus);
    }
    
    @Override
    public void reset(boolean randomize) {
        if (!hasTriggered) {
            return;
        }
        hasResetedAfterTrigger = true;
        log.debug("SingleTriggerTimeCondition reset, will trigger again at: {}", 
                targetTime.format(FORMATTER));
    }
    
    @Override
    public double getProgressPercentage() {
        if (hasTriggered) {
            return 100.0;
        }
        
        ZonedDateTime now = getNow();
        if (now.isAfter(targetTime)) {
            return 100.0;
        }
        
        // Calculate time progress as percentage
        long totalSeconds = java.time.Duration.between(
                ZonedDateTime.now().withSecond(0).withNano(0), targetTime).getSeconds();
        long secondsRemaining = java.time.Duration.between(now, targetTime).getSeconds();
        
        if (totalSeconds <= 0) {
            return 0.0;
        }
        
        double progress = 100.0 * (1.0 - (secondsRemaining / (double) totalSeconds));
        return Math.min(99.9, Math.max(0.0, progress)); // Cap between 0-99.9%
    }
    
    /**
     * Checks if this condition can trigger again (hasn't triggered yet)
     * 
     * @return true if the condition hasn't triggered yet
     */
    public boolean canTriggerAgain() {
        return !hasTriggered;
    }
    
    @Subscribe
    public void onGameTick(GameTick event) {
        // Used to stay registered with the event bus
    }
}