package net.runelite.client.plugins.microbot.pluginscheduler.condition.time;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.ConditionType;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.enums.RepeatCycle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class TimeWindowCondition extends TimeCondition {
    // Time window bounds (daily start/end times)
    private LocalTime startTime;
    private LocalTime endTime;

    // Date range for validity period 
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Repeat cycle configuration
    private RepeatCycle repeatCycle = RepeatCycle.DAYS;
    private int repeatInterval = 1; // Default: every 1 day

    // Next window tracking (for non-daily cycles)
    private transient LocalDateTime nextWindowStart;
    private transient LocalDateTime nextWindowEnd;
    
    // Randomization
    private boolean useRandomization = false;
    private int randomizeMinutes = 0; // Minutes to randomize start/end by
    
    // Cached timezone for computation - not serialized
    private transient ZoneId zoneId;

    // Add a field to store the zone ID string for serialization
    private String zoneIdString;

    /**
     * Default constructor for serialization
     */
    public TimeWindowCondition() {
        this(LocalTime.of(9, 0), LocalTime.of(17, 0));
    }

    /**
     * Basic constructor with just time window
     */
    public TimeWindowCondition(LocalTime startTime, LocalTime endTime) {
        this(
            startTime, 
            endTime, 
            LocalDate.now(), 
            LocalDate.now().plusMonths(1),
            RepeatCycle.DAYS,
            1
        );
    }

    /**
     * Full constructor
     */
    public TimeWindowCondition(
            LocalTime startTime, 
            LocalTime endTime, 
            LocalDate startDate,
            LocalDate endDate,
            RepeatCycle repeatCycle,
            int repeatInterval) {
        
        this.startTime = startTime;
        this.endTime = endTime;
        this.startDate = startDate;
        this.endDate = endDate;
        this.repeatCycle = repeatCycle;
        this.repeatInterval = Math.max(1, repeatInterval); // Ensure positive interval
        this.zoneId = ZoneId.systemDefault(); // Initialize with system default
        
        // Initialize next window times based on repeat cycle
        calculateNextWindow();
    }

    /**
     * Gets the current system timezone
     */
    public ZoneId getZoneId() {
        if (zoneId == null) {
            // Try to initialize from stored string first
            if (zoneIdString != null && !zoneIdString.isEmpty()) {
                try {
                    zoneId = ZoneId.of(zoneIdString);
                } catch (Exception e) {
                    // If stored string is invalid, fall back to system default
                    zoneId = ZoneId.systemDefault();
                }
            } else {
                // No stored string, use system default
                zoneId = ZoneId.systemDefault();
            }
            
            // Update the string field for future serialization
            zoneIdString = zoneId.getId();
        }
        return zoneId;
    }

    /**
     * Sets the zone ID to use for time calculations
     */
    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
        this.zoneIdString = zoneId.getId();
    }

    /**
     * Calculate the next window start and end times based on current time and repeat settings
     */
    public void calculateNextWindow() {
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        
        // For ONE_TIME, just use the defined start/end date with time
        if (repeatCycle == RepeatCycle.ONE_TIME) {
            nextWindowStart = LocalDateTime.of(startDate, startTime);
            nextWindowEnd = LocalDateTime.of(endDate, endTime);
            
            // If window is in the past, make it inactive
            if (nextWindowEnd.isBefore(nowLocal)) {
                nextWindowStart = null;
                nextWindowEnd = null;
            }
            return;
        }
        
        // For daily repeat cycle - standard time window on each day
        if (repeatCycle == RepeatCycle.DAYS && repeatInterval == 1) {
            // No need to calculate next window specifically - just use today's window
            nextWindowStart = null;
            nextWindowEnd = null;
            return;
        }
        
        // For other repeat cycles, calculate the next window
        LocalDateTime baseStart = LocalDateTime.of(startDate, startTime);
        
        // If first window hasn't started yet, use it
        if (baseStart.isAfter(nowLocal)) {
            nextWindowStart = baseStart;
            nextWindowEnd = baseStart.with(endTime);
            
            // Handle case where end time is before start time (crosses midnight)
            if (endTime.isBefore(startTime)) {
                nextWindowEnd = nextWindowEnd.plusDays(1);
            }
            return;
        }
        
        // Calculate how many intervals have elapsed since the start date
        long unitsBetween;
        switch (repeatCycle) {
            case MINUTES:
                unitsBetween = ChronoUnit.MINUTES.between(baseStart, nowLocal) / repeatInterval;
                nextWindowStart = baseStart.plusMinutes((unitsBetween + 1) * repeatInterval);
                break;
            case HOURS:
                unitsBetween = ChronoUnit.HOURS.between(baseStart, nowLocal) / repeatInterval;
                nextWindowStart = baseStart.plusHours((unitsBetween + 1) * repeatInterval);
                break;
            case DAYS:
                unitsBetween = ChronoUnit.DAYS.between(baseStart, nowLocal) / repeatInterval;
                nextWindowStart = baseStart.plusDays((unitsBetween + 1) * repeatInterval);
                break;
            case WEEKS:
                unitsBetween = ChronoUnit.WEEKS.between(baseStart, nowLocal) / repeatInterval;
                nextWindowStart = baseStart.plusWeeks((unitsBetween + 1) * repeatInterval);
                break;
            default:
                nextWindowStart = baseStart; // Shouldn't happen with validation
        }
        
        // Calculate end time based on start time
        nextWindowEnd = nextWindowStart.with(endTime);
        
        // Handle case where end time is before start time (crosses midnight)
        if (endTime.isBefore(startTime)) {
            nextWindowEnd = nextWindowEnd.plusDays(1);
        }
        
        // Apply randomization if enabled
        if (useRandomization && randomizeMinutes > 0) {
            int randomStart = ThreadLocalRandom.current().nextInt(-randomizeMinutes, randomizeMinutes + 1);
            int randomEnd = ThreadLocalRandom.current().nextInt(-randomizeMinutes, randomizeMinutes + 1);
            
            nextWindowStart = nextWindowStart.plusMinutes(randomStart);
            nextWindowEnd = nextWindowEnd.plusMinutes(randomEnd);
        }
        
        // Check if next window is beyond the end date
        LocalDateTime latestEnd = LocalDateTime.of(endDate, endTime);
        if (endTime.isBefore(startTime)) {
            latestEnd = latestEnd.plusDays(1);
        }
        
        if (nextWindowStart.isAfter(latestEnd)) {
            // No more windows in the valid date range
            nextWindowStart = null;
            nextWindowEnd = null;
        }
    }

    @Override
    public boolean isSatisfied() {
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        LocalDate today = now.toLocalDate();
        
        // If outside overall date range, immediately return false
        if (today.isBefore(startDate) || today.isAfter(endDate)) {
            return false;
        }
        
        // Handle specific repeat cycles
        if (repeatCycle != RepeatCycle.DAYS || repeatInterval > 1) {
            // For non-daily or interval > 1 day cycles, check against calculated next window
            if (nextWindowStart == null || nextWindowEnd == null) {
                // If windows aren't set, we need to initialize them once
                if (nextWindowStart == null && nextWindowEnd == null) {
                    log.info("Window not initialized, calculating initial window");
                    calculateNextWindow();
                }
                
                if (nextWindowStart == null) {
                    return false; // No more windows in range
                }
            }
            
            // Check if window has passed - but don't auto-recalculate
            // Let the scheduler decide when to reset the condition
            if (nowLocal.isAfter(nextWindowEnd)) {
                log.info("Current window has passed, waiting for reset");
                return false;
            }
            
            // Check if within next window
            return nowLocal.isAfter(nextWindowStart) && nowLocal.isBefore(nextWindowEnd);
        }
        
        // Standard daily window check - this doesn't use the next window calculation
        LocalTime currentTime = now.toLocalTime();
        
        // If start time is before end time, simple range check
        if (startTime.isBefore(endTime) || startTime.equals(endTime)) {
            return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
        } 
        // If start time is after end time, the window crosses midnight
        else {
            return !currentTime.isBefore(startTime) || !currentTime.isAfter(endTime);
        }
    }

    @Override
    public double getProgressPercentage() {
        if (!isSatisfied()) {
            return 0.0;
        }
        
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        
        // Calculate progress differently based on repeat cycle
        if (repeatCycle != RepeatCycle.DAYS || repeatInterval > 1) {
            if (nextWindowStart == null || nextWindowEnd == null) {
                return 0.0;
            }
            
            long totalDuration = ChronoUnit.SECONDS.between(nextWindowStart, nextWindowEnd);
            long elapsedDuration = ChronoUnit.SECONDS.between(nextWindowStart, nowLocal);
            
            return Math.min(100.0, (elapsedDuration * 100.0) / totalDuration);
        }
        
        // For standard daily window
        LocalTime currentTime = now.toLocalTime();
        
        // Calculate duration in seconds
        long totalSeconds;
        long elapsedSeconds;
        
        if (startTime.isBefore(endTime) || startTime.equals(endTime)) {
            totalSeconds = ChronoUnit.SECONDS.between(startTime, endTime);
            elapsedSeconds = ChronoUnit.SECONDS.between(startTime, currentTime);
        } else {
            // Window crosses midnight
            LocalTime midnight = LocalTime.MIDNIGHT;
            LocalTime endOfDay = LocalTime.of(23, 59, 59);
            
            totalSeconds = ChronoUnit.SECONDS.between(startTime, endOfDay) + 
                          ChronoUnit.SECONDS.between(midnight, endTime) + 1;
            
            if (currentTime.isBefore(endTime)) {
                elapsedSeconds = ChronoUnit.SECONDS.between(startTime, endOfDay) + 
                               ChronoUnit.SECONDS.between(midnight, currentTime) + 1;
            } else {
                elapsedSeconds = ChronoUnit.SECONDS.between(startTime, currentTime);
            }
        }
        
        return Math.min(100.0, (elapsedSeconds * 100.0) / totalSeconds);
    }

    @Override
    public String getDescription() {
        StringBuilder description = new StringBuilder("Time Window: ");
        
        // Format times
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String timeRangeStr = startTime.format(timeFormatter) + " to " + endTime.format(timeFormatter);
        description.append(timeRangeStr);
        
        // Add repeat information
        if (repeatCycle == RepeatCycle.ONE_TIME) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            description.append(" (One time from ")
                      .append(startDate.format(dateFormatter))
                      .append(" to ")
                      .append(endDate.format(dateFormatter))
                      .append(")");
        } else if (repeatCycle != RepeatCycle.DAYS || repeatInterval > 1) {
            description.append(" (")
                      .append(repeatCycle.getDisplayName().replace("X", Integer.toString(repeatInterval)))
                      .append(")");
        }
        
        // Add timezone information for clarity
        description.append(" [").append(getZoneId().getId()).append("]");
        
        return description.toString();
    }
    
    /**
     * Sets randomization parameters for window times
     * Does not automatically recalculate the window - call reset() to apply changes
     * 
     * @param useRandomization Whether to use randomization
     * @param randomizeMinutes Maximum minutes to randomize by (±)
     */
    public void setRandomization(boolean useRandomization, int randomizeMinutes) {
        this.useRandomization = useRandomization;
        this.randomizeMinutes = Math.max(0, randomizeMinutes);
    }

    /**
     * Factory method to create a simple daily window
     */
    public static TimeWindowCondition createDaily(LocalTime startTime, LocalTime endTime) {
        return new TimeWindowCondition(
            startTime, 
            endTime, 
            LocalDate.now(), 
            LocalDate.now().plusYears(1),
            RepeatCycle.DAYS,
            1
        );
    }

    @Override
    public ConditionType getType() {
        return ConditionType.TIME;
    }
    
    /**
     * Called after deserialization to initialize transient fields
     */
    public Object readResolve() {
        // Initialize timezone if needed
        if (zoneId == null) {
            zoneId = ZoneId.systemDefault();
        }        
        return this;
    }

    /**
     * Resets the time window condition, calculating the next time window
     * based on current settings
     */
    public void reset() {
        calculateNextWindow();
    }

    /**
     * Resets the time window condition with optional randomization
     * 
     * @param randomize Whether to apply randomization to the window times
     * @param randomizeMinutes Maximum minutes to randomize by (±)
     */
    public void reset(boolean randomize, int randomizeMinutes) {
        this.useRandomization = randomize;
        this.randomizeMinutes = randomizeMinutes;
        calculateNextWindow();
    }
    /**
     * Resets the time window condition with optional randomization
     * 
     * @param randomize Whether to apply randomization to the window times
     * @param randomizeMinutes Maximum minutes to randomize by (±)
     */
    public void reset(boolean randomize) {
        this.useRandomization = randomize;        
        calculateNextWindow();
    }
    @Override
    public boolean isUseRandomization() {
        return useRandomization;
    }
}