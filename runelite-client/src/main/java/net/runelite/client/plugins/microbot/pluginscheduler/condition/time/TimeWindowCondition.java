package net.runelite.client.plugins.microbot.pluginscheduler.condition.time;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;

import net.runelite.client.plugins.microbot.pluginscheduler.condition.ConditionType;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.enums.RepeatCycle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class TimeWindowCondition extends TimeCondition {
    // Time window bounds (daily start/end times)
    private final LocalTime startTime;
    private final LocalTime endTime;

    // Date range for validity period 
    private final LocalDate startDate;
    private final LocalDate endDate;
    
    // Repeat cycle configuration
    private final RepeatCycle repeatCycle;
    private final int repeatIntervalUnit; //based on the repeat cycle, defines the interval unit (e.g., days, weeks, etc.)
    

    // Next window tracking (for non-daily cycles)
    private transient LocalDateTime currentStartDateTime;
    private transient LocalDateTime currentEndDateTime;
   
    // Randomization
    private boolean useRandomization = false;
    private int randomizeMinutes = 0; // Minutes to randomize start/end by
    
    // Cached timezone for computation - not serialized
    private transient ZoneId zoneId;


    private int randomStartMinutes = 0; // Minutes to randomize start/end by
    private int randomEndMinutes = 0; // Minutes to randomize start/end by

    private int transientNumberOfResetsWithinDailyInterval = 0; // Number of resets since last calculation

    
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
            1,
            0// 0 means infinity 
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
            int repeatIntervalUnit,
            long maximumNumberOfRepeats) {
        super(maximumNumberOfRepeats);
                
        this.startTime = startTime;
        this.endTime = endTime;
        this.startDate = startDate;
        this.endDate = endDate;
        this.repeatCycle = repeatCycle;
        this.repeatIntervalUnit = Math.max(1, repeatIntervalUnit); // Ensure positive interval
        this.zoneId = ZoneId.systemDefault(); // Initialize with system default
        this.lastResetTime = LocalDateTime.now();
        transientNumberOfResetsWithinDailyInterval = 0;
        
        // Initialize next window times based on repeat cycle
        calculateNextWindow();
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
            1,
            0// 0 means infinity
        );
    }

    @Override
    public ConditionType getType() {
        return ConditionType.TIME;
    }
    /**
     * Gets the current system timezone
     */
    public ZoneId getZoneId() {        
        return zoneId;
    }

    /**
     * Sets the zone ID to use for time calculations
     */
    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;        
    }

   /**
     * Calculate the next window start and end times based on current time and reset settings
     */
    private void calculateNextWindow() {
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        LocalDateTime referenceTime = lastResetTime != null ? lastResetTime : nowLocal;
        
        log.info("Calculating next window: \n" +  this,Level.INFO);
        
        switch (repeatCycle) {
            case ONE_TIME:
                calculateOneTimeWindow(referenceTime);
                break;
            case DAYS:              
                calculateCycleWindow(referenceTime);                
                break;
            case WEEKS:
                calculateCycleWindow(referenceTime);
                break;
            case MINUTES:
            case HOURS:
                calculateCycleWindow(referenceTime);
                break;
            default:
                log.warn("Unsupported repeat cycle: {}", repeatCycle);
                break;
        }
        
        // Apply randomization if enabled
        if (useRandomization && randomizeMinutes > 0 && currentStartDateTime != null && this.currentEndDateTime != null) {        
            currentStartDateTime = currentStartDateTime.plusMinutes(randomStartMinutes);
            if (!LocalDateTime.of(endDate, endTime).isBefore(this.currentEndDateTime.plusMinutes(randomEndMinutes))) {                
                this.currentEndDateTime = this.currentEndDateTime.plusMinutes(randomEndMinutes);
            }else {
                this.currentEndDateTime = this.currentEndDateTime.minusMinutes(randomEndMinutes);
            }
            
        }
        LocalDateTime lastEnd = LocalDateTime.of(endDate, endTime);
        if (currentStartDateTime != null) {
            if (this.currentStartDateTime.isAfter(lastEnd)){
                this.currentStartDateTime = null;
                this.currentEndDateTime = null;
            }
        }
        log.info("Calculating of new window done: \n" +  this,Level.INFO);
    }

    /**
     * Calculates window for ONE_TIME repeat cycle
     */
    private void calculateOneTimeWindow(LocalDateTime referenceTime) {
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        LocalDateTime todayStartDateTime = LocalDateTime.of(nowLocal.toLocalDate(), startTime);
        LocalDateTime todayEndDateTime = LocalDateTime.of(nowLocal.toLocalDate(), endTime);
        if (transientNumberOfResetsWithinDailyInterval == 0 ){
            if (todayEndDateTime.isBefore(todayStartDateTime)) {
                todayEndDateTime = todayEndDateTime.plusDays(1);
            }
            if (referenceTime.isAfter(todayStartDateTime) && referenceTime.isBefore(todayEndDateTime)) {
                this.currentStartDateTime = todayStartDateTime;
                this.currentEndDateTime = todayEndDateTime;
            } else {
                this.currentStartDateTime = todayEndDateTime.plusDays(1);
                this.currentEndDateTime = todayEndDateTime.plusDays(1);
                
            }
        }else{
            // If the reset time is after the end of the window, set to null
            if (lastResetTime.isAfter(currentEndDateTime)) {
                this.currentStartDateTime = null;
                this.currentEndDateTime = null;
            } else {
                // wait until we are outside current vaild window
            }        
        }
    }

    

    /**
     * Calculates window for sub-day repeat cycles (MINUTES, HOURS)
     */
    private void calculateCycleWindow(LocalDateTime referenceTime) {
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        // First, determine the bounds of today's overall window
        LocalDate today = now.toLocalDate();
        LocalDateTime currentDayWindowStart = LocalDateTime.of(today, startTime);
        LocalDateTime currentDayWindowEnd = LocalDateTime.of(today, endTime);
        
        // Handle cross-midnight windows
        if (currentDayWindowEnd.isBefore(currentDayWindowStart)) {
            currentDayWindowEnd = currentDayWindowEnd.plusDays(1);
        }
                

        
                        
        this.currentStartDateTime = calculateNextStartWindow(referenceTime);
        log.info("Current start time calculation: {}", this);
        
        
        // If next interval starts after the outer window end, it's not valid today      
        LocalDate startday = currentStartDateTime.toLocalDate();           
        this.currentEndDateTime = LocalDateTime.of(startday, endTime);        
        if (currentEndDateTime.isBefore(currentStartDateTime)) {
            currentEndDateTime = currentEndDateTime.plusDays(1);
        }      
    }
    
    /**
     * Helper method to calculate interval from a reference point
     */
    private LocalDateTime calculateNextStartWindow( LocalDateTime referenceTime) {
        LocalDateTime nextStartTime;
        ZonedDateTime now = ZonedDateTime.now(getZoneId());        
        LocalDate today = now.toLocalDate();
        LocalDateTime currentDayWindowStart = LocalDateTime.of(today, startTime);
        LocalDateTime currentDayWindowEnd = LocalDateTime.of(today, endTime);
        if (this.currentResetCount > 0) {
            
        
            switch (repeatCycle) {
                case ONE_TIME:
                    nextStartTime = referenceTime;
                    break;
                case MINUTES:
                    nextStartTime = referenceTime.plusMinutes(repeatIntervalUnit);
                    break;
                case HOURS:
                    nextStartTime = referenceTime.plusHours(repeatIntervalUnit);
                    break;
                case DAYS:                                        
                    nextStartTime = referenceTime.plusDays(repeatIntervalUnit);
                    break;
                case WEEKS:
                    nextStartTime =  referenceTime.plusWeeks(repeatIntervalUnit);                                
                default:
                    log.warn("Unsupported repeat cycle: {}", repeatCycle);
                    nextStartTime=  referenceTime;
            }
        }else {
            nextStartTime = currentDayWindowStart;
        }
        
        if (nextStartTime.isBefore(currentDayWindowStart)) {
            nextStartTime = currentDayWindowStart;
        }else if (nextStartTime.isBefore(currentDayWindowEnd)) {
            
        }else if (nextStartTime.isAfter(currentDayWindowEnd)) {
            LocalDate nextDay = now.toLocalDate().plusDays(1);
            nextStartTime = LocalDateTime.of(nextDay, startTime);
        }
        return nextStartTime;
        
       
    }

   
    @Override
    public boolean isSatisfied() {
        if (!canTriggerAgain()) {
            return false;
        }
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        LocalDate today = now.toLocalDate();
                              
        // For non-daily or interval > 1 day cycles, check against calculated next window
        if (currentStartDateTime == null || currentEndDateTime == null) {                
            
            return false; // No more windows in range
            
        }
        
        // Check if window has passed - but don't auto-recalculate
        // Let the scheduler decide when to reset the condition
        if (nowLocal.isAfter(currentEndDateTime)) {
            log.info("Current window has passed, waiting for reset");
            return false;
        }
        
        // Check if within next window
        return nowLocal.isAfter(currentStartDateTime) && nowLocal.isBefore(currentEndDateTime);
               
    }

    @Override
    public double getProgressPercentage() {
        if (!isSatisfied()) {
            return 0.0;
        }
        
        // If our window bounds aren't set, we can't calculate progress
        if (currentStartDateTime == null || currentEndDateTime == null) {
            log.debug("Unable to calculate progress - window bounds are null");
            return 0.0;
        }
        
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        
        // Calculate total window duration in seconds
        long totalDuration = ChronoUnit.SECONDS.between(currentStartDateTime, currentEndDateTime);
        if (totalDuration <= 0) {
            log.debug("Invalid window duration: {} seconds", totalDuration);
            return 0.0;
        }
        
        // Calculate elapsed duration in seconds
        long elapsedDuration = ChronoUnit.SECONDS.between(currentStartDateTime, nowLocal);
        
        // Calculate percentage - cap at 100%
        double percentage = Math.min(100.0, (elapsedDuration * 100.0) / totalDuration);
        
        log.debug("Progress calculation: {}% ({}/{} seconds)", 
            String.format("%.1f", percentage),
            elapsedDuration, 
            totalDuration);
        
        return percentage;
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
        } else  {
            description.append(" (")
                      .append(repeatCycle.getDisplayName().replace("X", Integer.toString(repeatIntervalUnit)))
                      .append(")");
        }
        
        // Add timezone information for clarity
        description.append(" [").append(getZoneId().getId()).append("]");
        description.append("\n"+super.getDescription());
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
        reset(false);
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
        reset(randomize);
    }
  

    /**
     * Helper method to apply randomization to window times
     */
    private void applyRandomization() {
        if (useRandomization && randomizeMinutes > 0 && currentStartDateTime != null && currentEndDateTime != null) {
            this.randomStartMinutes = ThreadLocalRandom.current().nextInt(-randomizeMinutes, randomizeMinutes + 1);
            this.randomEndMinutes = ThreadLocalRandom.current().nextInt(-randomizeMinutes, randomizeMinutes + 1);                    
        }
    }
    
      /**
     * Resets the time window condition, calculating the next time window
     * based on current settings and reset time
     */
    @Override
    public void reset(boolean randomize) {
        this.useRandomization = randomize;
        applyRandomization();
        // Store current time as the reset reference
        log.debug("Last reset time: {}", lastResetTime);
        this.lastResetTime = LocalDateTime.now();
        log.info("Reset time window condition at: \n\t{}\n\t", lastResetTime);
        
        
        
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        
        // If we are have a current window and we are within the window or after it, we need to force an advance
        boolean needsAdvance = currentStartDateTime != null && nowLocal.isAfter(currentStartDateTime);
        
        log.info("current time: {}", nowLocal);
        log.info("current start time: {}", currentStartDateTime);
        log.info("current end time: {}", currentEndDateTime);
        
       
        // If this the next  start window that's passed or any window that needs advancing
        if (needsAdvance && canTriggerAgain() ) {     
            this.currentResetCount++;       
            calculateNextWindow();            
        }          
        if (nowLocal.isAfter(this.currentStartDateTime) && nowLocal.isBefore(this.currentEndDateTime)) {
            transientNumberOfResetsWithinDailyInterval++;
        }else {
            transientNumberOfResetsWithinDailyInterval = 0;
        }       
        // Log the new window for debugging
        if (this.currentStartDateTime != null && this.currentEndDateTime != null) {
            log.debug("Next window after reset: {} to {}", 
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(this.currentStartDateTime),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(this.currentEndDateTime));
        } else {
            log.debug("No next window available after reset");
        }
    }
    
    @Override
    public boolean isUseRandomization() {
        return useRandomization;
    }

    @Override
    public Optional<ZonedDateTime> getCurrentTriggerTime() {
        if (currentStartDateTime == null || currentEndDateTime == null || !canTriggerAgain()) {
            return Optional.empty();
        }
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        
        // If the condition is already satisfied (we're in the window), return the current time
        if (isSatisfied()) {
            return Optional.of(now.minusSeconds(1)); // Slightly in the past to indicate "ready now"
        }
                                
        // If our window calculation failed or hasn't been done, calculate it
        if (currentStartDateTime == null) {
            return Optional.empty();
        }else{
            return Optional.of(currentStartDateTime.atZone(getZoneId()));    
        }                        
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Basic information
        sb.append("TimeWindowCondition:\n");
        sb.append("  ┌─ Configuration ─────────────────────────────\n");
        sb.append("  │ Time Range: ").append(startTime.format(timeFormatter))
          .append(" to ").append(endTime.format(timeFormatter)).append("\n");
        sb.append("  │ Date Range: ").append(startDate.format(dateFormatter))
          .append(" to ").append(endDate.format(dateFormatter)).append("\n");
        sb.append("  │ Repeat: ").append(repeatCycle)
          .append(", Unit: ").append(repeatIntervalUnit).append("\n");
        sb.append("  │ Timezone: ").append(getZoneId().getId()).append("\n");
        
        // Status information
        sb.append("  ├─ Status ──────────────────────────────────\n");
        sb.append("  │ Satisfied: ").append(isSatisfied()).append("\n");
        if (currentStartDateTime != null && currentEndDateTime != null) {
            sb.append("  │ Current Window: ").append(currentStartDateTime.format(dateTimeFormatter))
              .append("\n  │ To: ").append(currentEndDateTime.format(dateTimeFormatter)).append("\n");
        } else if (currentStartDateTime != null) {
            sb.append("  │ Current Window: ").append(currentStartDateTime.format(dateTimeFormatter))
              .append("\n  │ To: Not available\n");
        } else {
            sb.append("  │ Current Window: Not available\n");
        }
        if (isSatisfied()) {
            sb.append("  │ Progress: ").append(String.format("%.1f%%", getProgressPercentage())).append("\n");
        }
        
        // Randomization
        sb.append("  ├─ Randomization ────────────────────────────\n");
        sb.append("  │ Randomization: ").append(useRandomization ? "Enabled" : "Disabled").append("\n");
        if (useRandomization) {
            sb.append("  │ Random Range: ±").append(randomizeMinutes).append(" minutes\n");
            sb.append("  │ Applied Offset: Start ").append(randomStartMinutes)
              .append(" min, End ").append(randomEndMinutes).append(" min\n");
        }
        
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
        sb.append("    Daily Reset Count: ").append(transientNumberOfResetsWithinDailyInterval).append("\n");
        sb.append("    Can Trigger Again: ").append(canTriggerAgain()).append("\n");
        
        return sb.toString();
    }
}