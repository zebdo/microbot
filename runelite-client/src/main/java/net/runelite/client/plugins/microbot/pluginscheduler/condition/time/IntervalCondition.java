package net.runelite.client.plugins.microbot.pluginscheduler.condition.time;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Condition that is met at regular intervals.
 * Can be used for periodic tasks or for creating natural breaks.
 */

@Slf4j
public class IntervalCondition extends TimeCondition {
    @Getter
    private final Duration interval;
    private ZonedDateTime nextTriggerTime;
    @Getter
    private boolean randomize;
    @Getter
    private double randomFactor;
 
    /**
     * Creates an interval condition that triggers at regular intervals
     * 
     * @param interval The time interval between triggers
     */
    public IntervalCondition(Duration interval) {
        this(interval, false,0.0, 0);
    }
    
    /**
     * Creates an interval condition with optional randomization
     * 
     * @param interval The base time interval
     * @param randomize Whether to randomize intervals
     * @param randomFactor Randomization factor (0-1.0) - how much to vary the interval
     */
    public IntervalCondition(Duration interval, boolean randomize, double randomFactor, long maximumNumberOfRepeats) {
        super(maximumNumberOfRepeats);
        this.interval = interval;
        this.randomize = randomize;
        this.randomFactor = Math.max(0, Math.min(1.0, randomFactor));
        this.nextTriggerTime = calculateNextTriggerTime(randomize, randomFactor);
    }
    
    /**
     * Creates an interval condition with minutes
     */
    public static IntervalCondition everyMinutes(int minutes) {
        return new IntervalCondition(Duration.ofMinutes(minutes));
    }
    
    /**
     * Creates an interval condition with hours
     */
    public static IntervalCondition everyHours(int hours) {
        return new IntervalCondition(Duration.ofHours(hours));
    }
    
    /**
     * Creates an interval condition with randomized timing
     */
    public static IntervalCondition randomizedMinutes(int baseMinutes, double randomFactor ,long maximumNumberOfRepeats) {
        return new IntervalCondition(Duration.ofMinutes(baseMinutes), true, randomFactor, maximumNumberOfRepeats);
    }
    
    /**
     * Creates an interval condition that triggers at intervals between the provided min and max durations.
     * 
     * @param minDuration Minimum interval duration
     * @param maxDuration Maximum interval duration
     * @return A randomized interval condition
     */
    public static IntervalCondition createRandomized(Duration minDuration, Duration maxDuration) {
        long minSeconds = minDuration.getSeconds();
        long maxSeconds = maxDuration.getSeconds();
        long randomSeconds = ThreadLocalRandom.current().nextLong(minSeconds, maxSeconds + 1);
        return new IntervalCondition(Duration.ofSeconds(randomSeconds));
    }

    @Override
    public boolean isSatisfied() {
        if(!canTriggerAgain()) {
            return false;
        }
        
        ZonedDateTime now = getNow();
        if (now.isAfter(nextTriggerTime)) {            
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        ZonedDateTime now = getNow();
        String timeLeft = "";
        
        if (nextTriggerTime != null) {
            if (now.isAfter(nextTriggerTime)) {
                timeLeft = " (ready now)";
            } else {
                Duration remaining = Duration.between(now, nextTriggerTime);
                long seconds = remaining.getSeconds();
                timeLeft = String.format(" (next in %02d:%02d:%02d)", 
                    seconds / 3600, (seconds % 3600) / 60, seconds % 60);
            }
        }
        
        if (randomize) {
            return String.format("Every %s±%.0f%%%s", 
                    formatDuration(interval), randomFactor * 100, timeLeft);
        } else {
            return String.format("Every %s%s", formatDuration(interval), timeLeft);
        }
    }

    /**
     * Returns a detailed description of the interval condition with additional status information
     */
    public String getDetailedDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDescription()).append("\n");
        
        ZonedDateTime now = getNow();
        if (nextTriggerTime != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            sb.append("Next trigger at: ").append(nextTriggerTime.format(formatter)).append("\n");
            
            if (now.isAfter(nextTriggerTime)) {
                sb.append("Status: Ready to trigger\n");
            } else {
                Duration remaining = Duration.between(now, nextTriggerTime);
                long seconds = remaining.getSeconds();
                sb.append("Time remaining: ")
                  .append(String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60))
                  .append("\n");
            }
        }
        
        sb.append("Progress: ").append(String.format("%.1f%%", getProgressPercentage())).append("\n");
        
        if (randomize) {
            sb.append("Randomization: Enabled (±").append(String.format("%.0f", randomFactor * 100)).append("%)\n");
        }
        
        sb.append(super.getDescription());
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Basic information
        sb.append("IntervalCondition:\n");
        sb.append("  ┌─ Configuration ─────────────────────────────\n");
        sb.append("  │ Interval: ").append(formatDuration(interval)).append("\n");
        
        // Randomization
        sb.append("  ├─ Randomization ────────────────────────────\n");
        sb.append("  │ Randomization: ").append(randomize ? "Enabled" : "Disabled").append("\n");
        if (randomize) {
            sb.append("  │ Random Factor: ±").append(String.format("%.0f%%", randomFactor * 100)).append("\n");
        }
        
        // Status information
        sb.append("  ├─ Status ──────────────────────────────────\n");
        sb.append("  │ Satisfied: ").append(isSatisfied()).append("\n");
        
        ZonedDateTime now = getNow();
        if (nextTriggerTime != null) {
            sb.append("  │ Next Trigger: ").append(nextTriggerTime.format(dateTimeFormatter)).append("\n");
            
            if (!now.isAfter(nextTriggerTime)) {
                Duration remaining = Duration.between(now, nextTriggerTime);
                long seconds = remaining.getSeconds();
                sb.append("  │ Time Remaining: ")
                  .append(String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60))
                  .append("\n");
            } else {
                sb.append("  │ Ready to trigger\n");
            }
        }
        
        sb.append("  │ Progress: ").append(String.format("%.1f%%", getProgressPercentage())).append("\n");
        
        // Tracking info
        sb.append("  └─ Tracking ────────────────────────────────\n");
        sb.append("    Reset Count: ").append(currentResetCount);
        if (this.getMaximumNumberOfRepeats() > 0) {
            sb.append("/").append(getMaximumNumberOfRepeats());
        } else {
            sb.append(" (unlimited)");
        }
        sb.append("\n");
        if (lastResetTime != null) {
            sb.append("    Last Reset: ").append(lastResetTime.format(dateTimeFormatter)).append("\n");
        }
        sb.append("    Can Trigger Again: ").append(canTriggerAgain()).append("\n");
        
        return sb.toString();
    }

    @Override
    public void reset(boolean randomize) {
        this.nextTriggerTime = calculateNextTriggerTime(randomize, randomFactor);
        this.currentResetCount++;
        this.lastResetTime = LocalDateTime.now();
        log.debug("IntervalCondition reset, next trigger at: {}", nextTriggerTime);
    }
   
    @Override
    public double getProgressPercentage() {
        ZonedDateTime now = getNow();
        if (now.isAfter(nextTriggerTime)) {
            return 100.0;
        }
        
        // Calculate how much time has passed since the last trigger
        Duration timeUntilNextTrigger = Duration.between(now, nextTriggerTime);
        double elapsedRatio = 1.0 - (timeUntilNextTrigger.toMillis() / (double) interval.toMillis());
        return Math.max(0, Math.min(100, elapsedRatio * 100));
    }
    @Override
    public Optional<ZonedDateTime> getCurrentTriggerTime() {
        if (!canTriggerAgain()) {
            return Optional.empty(); // No trigger time if already triggered to often
        }
        ZonedDateTime now = getNow();

        
        // If already satisfied (past the trigger time)
        if (now.isAfter(nextTriggerTime)) {
            return Optional.of(nextTriggerTime); // Return the passed time until reset
        }
        
        // Otherwise return the scheduled next trigger time
        return Optional.of(nextTriggerTime);
    }
    
    private ZonedDateTime calculateNextTriggerTime(boolean randomize, double factor) {
        ZonedDateTime now = getNow();
        
        if (!randomize || factor <= 0) {
            return now.plus(interval);
        }
        
        // Apply randomization to the interval
        long intervalMillis = interval.toMillis();
        long variance = (long) (intervalMillis * factor);
        long randomAdditionalMillis = ThreadLocalRandom.current().nextLong(-variance, variance + 1);
        
        Duration randomizedInterval = Duration.ofMillis(intervalMillis + randomAdditionalMillis);
        return now.plus(randomizedInterval);
    }
    
    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        } else {
            return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
        }
    }
}