package net.runelite.client.plugins.microbot.pluginscheduler.condition.time;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.ConditionType;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.enums.RepeatCycle;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import java.util.logging.Level;

@Data
@EqualsAndHashCode(callSuper = false, exclude = {   })
@Slf4j
public class TimeWindowCondition extends TimeCondition {
    
    // Constants for unlimited date ranges
    public static final LocalDate UNLIMITED_START_DATE = LocalDate.of(1900, 1, 1);
    public static final LocalDate UNLIMITED_END_DATE = LocalDate.of(2100, 12, 31);
    
    public static String getVersion() {
        return "0.0.3";
    }
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
    //@Getter
    //@Setter
    
    @Getter
    @Setter
    private transient LocalDateTime currentEndDateTime;
   
    // Randomization
    private transient boolean useRandomization = false;
    private transient int randomizerValue = 0; // Randomization value, depands on the repeat cycle
    private transient RepeatCycle randomizerValueUnit;
    // Cached timezone for computation - not serialized
    private transient ZoneId zoneId;



    @Getter
    @Setter
    private transient int transientNumberOfResetsWithinDailyInterval = 0; // Number of resets since last calculation

    /**
     * Checks if the start date represents an unlimited (no restriction) start date
     * @return true if start date is unlimited
     */
    public boolean isUnlimitedStartDate() {
        return startDate != null && startDate.equals(UNLIMITED_START_DATE);
    }
    
    /**
     * Checks if the end date represents an unlimited (no restriction) end date
     * @return true if end date is unlimited
     */
    public boolean isUnlimitedEndDate() {
        return endDate != null && endDate.equals(UNLIMITED_END_DATE);
    }
    
    /**
     * Checks if this condition has unlimited date range (both start and end are unlimited)
     * @return true if both dates are unlimited
     */
    public boolean hasUnlimitedDateRange() {
        return isUnlimitedStartDate() && isUnlimitedEndDate();
    }

    
    /**
     * Creates a time window condition with just daily start and end times.
     * Uses unlimited date range (no start/end date restrictions), daily repeat cycle, and unlimited repeats.
     *
     * @param startTime The daily start time of the window
     * @param endTime The daily end time of the window
     */
    public TimeWindowCondition(LocalTime startTime, LocalTime endTime) {
        this(
            startTime, 
            endTime, 
            UNLIMITED_START_DATE, 
            UNLIMITED_END_DATE,
            RepeatCycle.DAYS,
            1,
            0// 0 means infinity 
        );
    }

    /**
     * Creates a time window condition with all parameters specified.
     * This is the full constructor that allows complete configuration of the time window.
     *
     * @param startTime The daily start time of the window
     * @param endTime The daily end time of the window
     * @param startDate The earliest date the window can be active
     * @param endDate The latest date the window can be active
     * @param repeatCycle The cycle type for window repetition (DAYS, WEEKS, etc.)
     * @param repeatIntervalUnit The interval between repetitions (e.g., 2 for every 2 days)
     * @param maximumNumberOfRepeats Maximum number of times this condition can trigger (0 for unlimited)
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
        this.lastValidResetTime = LocalDateTime.now();
        transientNumberOfResetsWithinDailyInterval = 0;
        this.randomizerValue = 0;
        this.useRandomization = true;
        
        // Initialize next window times based on repeat cycle
        calculateNextWindow(getNow().toLocalDateTime());
    }
    /**
     * Factory method to create a simple daily time window that repeats every day.
     * Creates a window that starts and ends at the specified times each day,
     * with unlimited date range (no start/end date restrictions).
     *
     * @param startTime The daily start time of the window
     * @param endTime The daily end time of the window
     * @return A configured TimeWindowCondition for daily repetition
     */
    public static TimeWindowCondition createDaily(LocalTime startTime, LocalTime endTime) {
        return new TimeWindowCondition(
            startTime, 
            endTime, 
            UNLIMITED_START_DATE, 
            UNLIMITED_END_DATE,
            RepeatCycle.DAYS,
            1,
            0// 0 means infinity
        );

    }

    /**
     * {@inheritDoc}
     * Returns the type of this condition, which is TIME.
     *
     * @return The condition type ConditionType.TIME
     */
    @Override
    public ConditionType getType() {
        return ConditionType.TIME;
    }
    /**
     * Gets the timezone used for time calculations in this condition.
     * 
     * @return The ZoneId representing the timezone
     */
    public ZoneId getZoneId() {        
        return zoneId;
    }

    /**
     * Sets the timezone to use for time calculations in this condition.
     * Changes to the timezone will affect when the time window activates.
     * 
     * @param zoneId The timezone to use for calculations
     */
    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;        
    }

   /**
     * Calculate the next window start and end times based on current time and reset settings
     */
    private void calculateNextWindow(LocalDateTime lastValidTime) {
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();


        LocalDateTime referenceTime = lastValidTime != null ? lastValidTime : nowLocal;
        
        //LocalDateTime todayStartDateTime = LocalDateTime.of(nowLocal.toLocalDate(), startTime);
        //LocalDateTime todayEndDateTime = LocalDateTime.of(nowLocal.toLocalDate(), endTime);
        if(Microbot.isDebug() ) log.info("Calculating next window - current: \n" +  this,Level.INFO);
        
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
                calculateCycleWindow(referenceTime);
                
                break;
            case HOURS:
                calculateCycleWindow(referenceTime);
               
                break;
                
            default:
                log.warn("Unsupported repeat cycle: {}", repeatCycle);
                break;
        }
        log.info(this.getDetailedDescription());
        if(Microbot.isDebug() ) log.info("After calculate new cycle window : \n" +  this,Level.INFO);
        // Apply randomization if enabled
        
        // Only check end date bounds if not unlimited
        if (!isUnlimitedEndDate()) {
            LocalDateTime lastEnd = LocalDateTime.of(endDate, endTime);
            if (getNextTriggerTimeWithPause().orElse(null) != null) {
                LocalDateTime nextTrigger = getNextTriggerTimeWithPause().get().toLocalDateTime();
                if (nextTrigger.isAfter(lastEnd)){
                    setNextTriggerTime(null);
                    this.currentEndDateTime = null;
                }
            }else{
                   this.currentEndDateTime = null;
            }
        }
        if(Microbot.isDebug() ) log.info("Calculating done -  new time window: \n" +  this,Level.INFO);
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
                setNextTriggerTime(todayStartDateTime.atZone(getZoneId()));
                this.currentEndDateTime = todayEndDateTime;
            } else {
                setNextTriggerTime(todayEndDateTime.plusDays(1).atZone(getZoneId()));
                this.currentEndDateTime = todayEndDateTime.plusDays(1);
                
            }
        }else{
            // If the reset time is after the end of the window, set to null
            if (lastValidResetTime.isAfter(currentEndDateTime)) {
                 setNextTriggerTime(null);
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
                

        
        LocalDateTime nextTriggerTime =  calculateNextStartWindow(referenceTime);
        setNextTriggerTime(nextTriggerTime.atZone(getZoneId()));        
        if (Microbot.isDebug()) log.info("calculation of new cycle window after calculation of next start window:\n {}", this);
        
        
        // If next interval starts after the outer window end, it's not valid today      
        LocalDate startday = nextTriggerTime.toLocalDate();           
        this.currentEndDateTime = LocalDateTime.of(startday, endTime);        
        if (currentEndDateTime.isBefore(nextTriggerTime)) {
            this.currentEndDateTime = calculateNextTime( this.currentEndDateTime);  
            LocalDateTime endDateTimeNextDay = LocalDateTime.of(startday.plusDays(1), endTime);
            if (this.currentEndDateTime.isBefore(nextTriggerTime) || endDateTimeNextDay.isBefore(this.currentEndDateTime)) {
                throw new IllegalStateException("Invalid end time calculation: " + this.currentEndDateTime);
            }
        }      
    
    }
    private void randomizeCurrentStartEnd( ) {
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();                
        LocalDateTime nextStartDateTime = LocalDateTime.of(nowLocal.toLocalDate().plusDays(1), startTime);
        LocalDateTime nextEndDateTime = LocalDateTime.of(nowLocal.toLocalDate().plusDays(1), endTime);
        if(Microbot.isDebug() ) log.info("Calculating next window - current: \n" +  this,Level.INFO);
        int minOffset = -1;
        int maxOffset=-1;
        long intervalLength =0 ;
        randomizerValueUnit = null;
        LocalDateTime nextTriggerTime = getNextTriggerTimeWithPause().orElse(null).toLocalDateTime();
        log.info(" currentStartDateTime: {} - currentEndDateTime: {}", nextTriggerTime, currentEndDateTime);
        if (useRandomization && nextTriggerTime != null && this.currentEndDateTime != null) {

            switch (repeatCycle) {
                case ONE_TIME:
                    randomizerValueUnit = RepeatCycle.ONE_TIME;
                    break;
                case DAYS:                                                      
                    intervalLength = ChronoUnit.MINUTES.between(nextTriggerTime, currentEndDateTime);   
                                    
                    randomizerValueUnit = RepeatCycle.MINUTES;
                    minOffset = Math.max(0,(int)(intervalLength*0.1));
                    maxOffset = Math.max((int)(intervalLength*0.2),1);
                    
                    this.randomizerValue = Rs2Random.between(minOffset, maxOffset);
                    
                    if (nextTriggerTime.plusMinutes(this.randomizerValue).isBefore(nextEndDateTime)){
                        nextTriggerTime =  nextTriggerTime.plusMinutes(this.randomizerValue);
                    }
                    if (!LocalDateTime.of(endDate, endTime).isBefore(this.currentEndDateTime.plusMinutes(randomizerValue))) {                
                        this.currentEndDateTime = this.currentEndDateTime.plusMinutes(randomizerValue);
                    }else {
                        this.currentEndDateTime = this.currentEndDateTime.plusMinutes(randomizerValue);
                    }
                    
                    break;
                case WEEKS:
                                    
                    long intervalLengthHours= ChronoUnit.HOURS.between(nextTriggerTime, currentEndDateTime);   
                    randomizerValueUnit = RepeatCycle.HOURS;
                    minOffset = Math.max( 0,(int)(intervalLengthHours*0.1));
                    maxOffset = Math.max((int)(intervalLengthHours*0.2),1 );
                    this.randomizerValue = Rs2Random.between(minOffset, maxOffset);
                    //when on next day
                    if (nextTriggerTime.isAfter(nextStartDateTime)){
                        if (nextTriggerTime.plusHours(this.randomizerValue).isBefore(nextEndDateTime)){
                            nextTriggerTime = nextTriggerTime.plusHours(this.randomizerValue);
                        }  
                    }                  
                    if (!LocalDateTime.of(endDate, endTime).isBefore(this.currentEndDateTime.plusHours(randomizerValue))) {                
                        this.currentEndDateTime = this.currentEndDateTime.plusHours(randomizerValue);
                    }else {
                        this.currentEndDateTime = this.currentEndDateTime.plusHours(randomizerValue);
                    }                    
                    break;
                case MINUTES:                
                   
                    intervalLength = ChronoUnit.MILLIS.between(nextTriggerTime, currentEndDateTime);   
                    randomizerValueUnit = RepeatCycle.MILLIS;
                    minOffset = Math.max(0,(int)(intervalLength*0.1));
                    maxOffset = Math.max(1,(int)(intervalLength*0.2));
                    this.randomizerValue = Rs2Random.between(minOffset, maxOffset);
                    nextTriggerTime = nextTriggerTime.plusSeconds(this.randomizerValue*1000);

                    if (!LocalDateTime.of(endDate, endTime).isBefore(this.currentEndDateTime.plusSeconds(randomizerValue*1000))) {                
                        this.currentEndDateTime = this.currentEndDateTime.plusSeconds(randomizerValue*1000);
                    }else {
                        this.currentEndDateTime = this.currentEndDateTime.plusSeconds(randomizerValue*1000);
                    }            
                    break;
                case HOURS:                                  
                    long intervalLengthSeconds = ChronoUnit.SECONDS.between(nextTriggerTime, currentEndDateTime);                   
                    randomizerValueUnit = RepeatCycle.SECONDS;
                    minOffset =  Math.max(0, (int)(intervalLengthSeconds*0.1));
                    maxOffset = Math.max(1,(int)(intervalLengthSeconds*0.2));
                    this.randomizerValue = Rs2Random.between(minOffset, maxOffset);
                    nextTriggerTime = nextTriggerTime.plusSeconds(this.randomizerValue);
                    if (!LocalDateTime.of(endDate, endTime).isBefore(this.currentEndDateTime.plusSeconds(randomizerValue))) {                
                        this.currentEndDateTime = this.currentEndDateTime.plusSeconds(randomizerValue);
                    }else {
                        this.currentEndDateTime = this.currentEndDateTime.plusSeconds(randomizerValue);
                    }                    
                    break;
                    
                default:
                    log.warn("Unsupported repeat cycle: {}", repeatCycle);
                    break;
            }
            setNextTriggerTime(nextTriggerTime.atZone(zoneId));
            log.info("Interval length in hours: {} - min: {} - max: {} - randomizerValue: {} {}", intervalLength, minOffset, maxOffset,randomizerValue,repeatCycle.unit());
        }
       

    }
    private LocalDateTime calculateNextTime( LocalDateTime referenceTime) {
            LocalDateTime nextStartTime;
            int minOffset = -1;
            int maxOffset=-1;
            long intervalLength =0 ;
            if (repeatIntervalUnit == 0) {
                return referenceTime;
            }   
            switch (repeatCycle) {
                case ONE_TIME:
                    nextStartTime = referenceTime;
                    break;
                case MINUTES:
                    if (useRandomization){
                        intervalLength = ChronoUnit.SECONDS.between(referenceTime,  referenceTime.plusMinutes(repeatIntervalUnit));                   
                        randomizerValueUnit = RepeatCycle.SECONDS;
                        minOffset =  Math.max(0, (int)(intervalLength*0.1));
                        maxOffset = Math.max(1,(int)(intervalLength*0.2));
                        this.randomizerValue = Rs2Random.between(minOffset, maxOffset);
                        nextStartTime = referenceTime.plusSeconds(this.randomizerValue);
                    }else{
                        nextStartTime = referenceTime.plusMinutes(repeatIntervalUnit);
                    }
                    break;
                case HOURS:
                    if(useRandomization){
                        intervalLength = ChronoUnit.MINUTES.between(referenceTime,  referenceTime.plusHours(repeatIntervalUnit));                   
                        randomizerValueUnit = RepeatCycle.MINUTES;
                        minOffset =  Math.max(0, (int)(intervalLength*0.1));
                        maxOffset = Math.max(1,(int)(intervalLength*0.2));
                        this.randomizerValue = Rs2Random.between(minOffset, maxOffset);
                        nextStartTime = referenceTime.plusMinutes(this.randomizerValue);
                    }else{
                        nextStartTime = referenceTime.plusHours(repeatIntervalUnit);
                    }
                    break;
                case DAYS:                                        
                    if (useRandomization){
                        intervalLength = ChronoUnit.HOURS.between(referenceTime,  referenceTime.plusDays(repeatIntervalUnit));
                        randomizerValueUnit = RepeatCycle.HOURS;
                        minOffset =  Math.max(0, (int)(intervalLength*0.1));
                        maxOffset = Math.max(1,(int)(intervalLength*0.2));
                        this.randomizerValue = Rs2Random.between(minOffset, maxOffset);
                        nextStartTime = referenceTime.plusHours(this.randomizerValue);
                    }else{
                        nextStartTime = referenceTime.plusDays(repeatIntervalUnit);
                    }
                    break;
                case WEEKS:
                    if (useRandomization){
                        intervalLength = ChronoUnit.DAYS.between(referenceTime,  referenceTime.plusWeeks(repeatIntervalUnit));
                        randomizerValueUnit = RepeatCycle.DAYS;
                        minOffset =  Math.max(0, (int)(intervalLength*0.1));
                        maxOffset = Math.max(1,(int)(intervalLength*0.2));
                        this.randomizerValue = Rs2Random.between(minOffset, maxOffset);
                        nextStartTime = referenceTime.plusDays(this.randomizerValue);

                    
                    }else{
                        nextStartTime =  referenceTime.plusWeeks(repeatIntervalUnit);                                
                    }
                default:
                    log.warn("Unsupported repeat cycle: {}", repeatCycle);
                    nextStartTime=  referenceTime;
                log.info("\n\t -Interval length in hours: {} - min: {} - max: {} - randomizerValue: {}", intervalLength, minOffset, maxOffset,randomizerValue);
            }
            return nextStartTime;
        }
    /**
     * Helper method to calculate interval from a reference point
     */
    private LocalDateTime calculateNextStartWindow( LocalDateTime referenceTime) {
        LocalDateTime nextStartTime;
        ZonedDateTime now = ZonedDateTime.now(getZoneId());        
        LocalDateTime nowLocal = now.toLocalDateTime();
        LocalDate today = now.toLocalDate();
        LocalDateTime currentDayWindowStart = LocalDateTime.of(today, startTime);
        LocalDateTime currentDayWindowEnd = LocalDateTime.of(today, endTime);
        if (this.currentValidResetCount > 0) {
            
            nextStartTime = calculateNextTime(referenceTime);
           
        }else {
            if (nowLocal.isBefore(currentDayWindowEnd)) {                
                nextStartTime = currentDayWindowStart;
            } else {
                nextStartTime = currentDayWindowStart.plusDays(1);
            }
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

   
    /**
     * {@inheritDoc}
     * Determines if the current time is within the configured time window.
     * Checks if the current time is after the start time and before the end time
     * of the current window, and if the condition can still trigger.
     *
     * @return true if the current time is within the active window and the condition can trigger,
     *         false otherwise
     */
    @Override
    public boolean isSatisfied() {
        if (getNextTriggerTimeWithPause().isPresent()) {
            return isSatisfied(getNextTriggerTimeWithPause().get().toLocalDateTime());
        }        
        return false;
    }
    @Override
    public boolean isSatisfiedAt(ZonedDateTime triggerTime) {
        return isSatisfied(triggerTime.toLocalDateTime());
        
    }
    
    private boolean isSatisfied(LocalDateTime currentStartDateTime) {
        if (isPaused()) {
            return false;
        }
        if (!canTriggerAgain()) {
            return false;
        }
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        LocalDate today = now.toLocalDate();
        LocalDate dayBefore = today.minusDays(1);
        LocalDate nextDay = today.plusDays(1);
        LocalDateTime currentDayWindowStart = LocalDateTime.of(today, startTime);
        LocalDateTime currentDayWindowEnd = LocalDateTime.of(today, endTime); 
        LocalDateTime beforeDayWindowStart = LocalDateTime.of( dayBefore,  startTime);            
        LocalDateTime beforeDayWindowEnd = LocalDateTime.of(dayBefore, endTime);    

        LocalDateTime nextDayWindowStart = LocalDateTime.of(nextDay, startTime);
        LocalDateTime nextDayWindowEnd = LocalDateTime.of(nextDay, endTime);
        // For non-daily or interval > 1 day cycles, check against calculated next window
        if (currentStartDateTime == null || currentEndDateTime == null) {                
            
            return false; // No more windows in range
            
        }
        if ((currentStartDateTime.isAfter(beforeDayWindowStart) && currentEndDateTime.isBefore(beforeDayWindowEnd))) {
            lastValidResetTime = currentDayWindowStart;
            this.calculateNextWindow(this.lastValidResetTime);
            
        }
        // Check if window has passed - but don't auto-recalculate
        // Let the scheduler decide when to reset the condition
        if (nowLocal.isAfter(currentEndDateTime)) {            
            return false;
        }
        
        // Check if within next window
        return nowLocal.isAfter(currentStartDateTime) && nowLocal.isBefore(currentEndDateTime);
               
    }

    /**
     * {@inheritDoc}
     * Calculates progress through the current time window as a percentage.
     * Returns 0% if outside the window or 0-100% based on how much of the window has elapsed.
     *
     * @return A percentage from 0-100 indicating progress through the current time window
     */
    @Override
    public double getProgressPercentage() {
        if (!isSatisfied()) {
            return 0.0;
        }
        
        // If our window bounds aren't set, we can't calculate progress
        if (getNextTriggerTimeWithPause().orElse(null) == null || currentEndDateTime == null) {
            log.debug("Unable to calculate progress - window bounds are null");
            return 0.0;
        }
        
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        LocalDateTime currentStartDateTime = getNextTriggerTimeWithPause().get().toLocalDateTime();
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

    /**
     * {@inheritDoc}
     * Provides a user-friendly description of this time window condition.
     * Includes the time range, repeat information, and timezone.
     *
     * @return A human-readable string describing the time window parameters
     */
    @Override
    public String getDescription() {
        StringBuilder description = new StringBuilder("Time Window: ");
        
        // Format times
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String timeRangeStr = startTime.format(timeFormatter) + " to " + endTime.format(timeFormatter);
        description.append(timeRangeStr);
        // Add date range information only if not unlimited
        if (!hasUnlimitedDateRange()) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            description.append(" ( Between: ");
            
            if (isUnlimitedStartDate()) {
                description.append("No start limit");
            } else {
                description.append(startDate.format(dateFormatter));
            }
            
            description.append(" to ");
            
            if (isUnlimitedEndDate()) {
                description.append("No end limit");
            } else {
                description.append(endDate.format(dateFormatter));
            }
            
            description.append(")");
        }
        // Add repeat information
        if (repeatCycle != RepeatCycle.ONE_TIME) {
            
            description.append(" (")
                      .append(repeatCycle.getDisplayName().replace("X", Integer.toString(repeatIntervalUnit)))
                      .append(")");
        }
        
        // Add timezone information for clarity
        //description.append(" [").append(getZoneId().getId()).append("]");
        description.append("\n"+super.getDescription());
        return description.toString();
    }

    
    /**
     * Configures time randomization for this window.
     * When enabled, the start and end times will be adjusted by a random
     * amount within the specified range each time the condition is reset.
     *
     * @param useRandomization Whether to enable time randomization
     * @param randomizerValue Maximum number of minutes to randomize by (plus or minus)
     */
    public void setRandomization(boolean useRandomization, int randomizerValue) {
        this.useRandomization = useRandomization;
        this.randomizerValue = Math.max(0, randomizerValue);
    }
   /**
     * Configures time randomization for this window.
     * When enabled, the start and end times will be adjusted by a random
     * amount within the specified range each time the condition is reset.
     *
     * @param useRandomization Whether to enable time randomization     
     */
    public void setRandomization(boolean useRandomization) {
        this.useRandomization = useRandomization;
        this.randomizerValue = 0;
    }

   
    
    /**
     * Custom deserialization method to initialize transient fields.
     * This ensures that the timezone is properly set after deserialization.
     *
     * @return The properly initialized deserialized object
     */
    public Object readResolve() {
        // Initialize timezone if needed
        if (zoneId == null) {
            zoneId = ZoneId.systemDefault();
        }        
        return this;
    }

    /**
     * Resets the time window condition with default settings.
     * Calculates the next time window based on current time and settings.
     * This is a shorthand for reset(false).
     */
    public void reset() {
        reset(false);
    }

  
    public void hardReset() {
        this.currentValidResetCount = 0;
        resume();
        this.lastValidResetTime = LocalDateTime.now();
        this.setNextTriggerTime(null);
        
        this.currentEndDateTime = null;
        this.useRandomization = true;
        this.transientNumberOfResetsWithinDailyInterval = 0;
        // Initialize next window times based on repeat cycle
        calculateNextWindow(this.lastValidResetTime);
  
    }
    /**
     * {@inheritDoc}
     * Resets the time window condition and calculates the next active window.
     * Updates the reset count, applies randomization if enabled, and advances
     * the window if necessary based on current time.
     *
     * @param randomize Whether to apply randomization to window times
     */
    @Override
    public void reset(boolean randomize) {        
        // Store current time as the reset reference
        log.debug("Last reset time: {}", lastValidResetTime);
        this.lastValidResetTime = LocalDateTime.now();                                
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();        
        // If we are have a current window and we are within the window or after it, we need to force an advance
        Optional<ZonedDateTime>    currentZoneStartDateTime =     getNextTriggerTimeWithPause();
        
        if (currentZoneStartDateTime ==null || !currentZoneStartDateTime.isPresent()) {
            return;
        }
        LocalDateTime  currentStartDateTime =currentZoneStartDateTime.get().toLocalDateTime();            
        boolean needsAdvance = currentZoneStartDateTime!=null && currentZoneStartDateTime.isPresent() && nowLocal.isAfter(currentZoneStartDateTime.get().toLocalDateTime());                           
        // If this the next  start window that's passed or any window that needs advancing
        if (needsAdvance && canTriggerAgain() ) {     
            this.currentValidResetCount++;       
            calculateNextWindow(this.lastValidResetTime);            
        }          
        if (nowLocal.isAfter(currentStartDateTime) && nowLocal.isBefore(this.currentEndDateTime)) {
            transientNumberOfResetsWithinDailyInterval++;
        }else {
            transientNumberOfResetsWithinDailyInterval = 0;
        }       
        // Log the new window for debugging
        if (currentStartDateTime != null && this.currentEndDateTime != null) {
            log.debug("Next window after reset: {} to {}", 
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(currentStartDateTime),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(currentEndDateTime));
        } else {
            log.debug("No next window available after reset");
        }
    }
    
    /**
     * {@inheritDoc}
     * Indicates whether this condition uses randomization.
     *
     * @return true if randomization is enabled, false otherwise
     */
    @Override
    public boolean isUseRandomization() {
        return useRandomization;
    }

    /**
     * {@inheritDoc}
     * Calculates the next time this condition will be satisfied (the start of the next window).
     * If already within a window, returns a time slightly in the past to indicate the condition
     * is currently satisfied.
     *
     * @return An Optional containing the time when the next window starts,
     *         or empty if no future windows are scheduled or the condition cannot trigger again
     */
    @Override
    public Optional<ZonedDateTime> getCurrentTriggerTime() {
        if (getNextTriggerTimeWithPause().orElse(null) == null || currentEndDateTime == null || !canTriggerAgain()) {
            return Optional.empty();
        }
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime currentStartDateTime = getNextTriggerTimeWithPause().get().toLocalDateTime();
        // If the condition is already satisfied (we're in the window), return the current time
        if (isSatisfied()) {
            assert(!currentStartDateTime.isAfter(now.toLocalDateTime()) && !currentEndDateTime.isBefore(now.toLocalDateTime()));
            return Optional.of(currentStartDateTime.atZone(getZoneId())); // Slightly in the past to indicate "ready now"
        }
                                
        // If our window calculation failed or hasn't been done, calculate it
        if (currentStartDateTime == null) {
            return Optional.empty();
        }else{
            return Optional.of(currentStartDateTime.atZone(getZoneId()));    
        }                        
    }
    @Override
    public boolean canTriggerAgain(){

        boolean canTrigger = super.canTriggerAgain();
        
        // If end date is unlimited, only check parent class logic
        if (isUnlimitedEndDate()) {
            return canTrigger;
        }
        
        LocalDateTime lastDateTime = LocalDateTime.of( endDate, endTime);
        if (canTrigger ) {
            ZonedDateTime now = ZonedDateTime.now(getZoneId());
            LocalDateTime nowLocal = now.toLocalDateTime();
            return nowLocal.isBefore(lastDateTime);
        }
        return canTrigger;

    }

    /**
     * {@inheritDoc}
     * Generates a detailed string representation of this time window condition.
     * Includes configuration, status, window times, randomization settings,
     * and trigger count information formatted with visual separators.
     *
     * @return A multi-line string representation with detailed state information
     */
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
        sb.append("  │ Date Range: ");
        if (isUnlimitedStartDate()) {
            sb.append("No start limit");
        } else {
            sb.append(startDate.format(dateFormatter));
        }
        sb.append(" to ");
        if (isUnlimitedEndDate()) {
            sb.append("No end limit");
        } else {
            sb.append(endDate.format(dateFormatter));
        }
        sb.append("\n");
        sb.append("  │ Repeat: ").append(repeatCycle)
          .append(", Unit: ").append(repeatIntervalUnit).append("\n");
        sb.append("  │ Timezone: ").append(getZoneId().getId()).append("\n");
        
        // Status information
        sb.append("  ├─ Status ──────────────────────────────────\n");
        sb.append("  │ Satisfied: ").append(isSatisfied()).append("\n");
        sb.append("  │ Paused: ").append(isPaused()).append("\n");
        if (getNextTriggerTimeWithPause().orElse(null) != null && currentEndDateTime != null) {
            sb.append("  │ Current Window: ").append(getNextTriggerTimeWithPause().get().toLocalDateTime().format(dateTimeFormatter))
              .append("\n  │ To: ").append(currentEndDateTime.format(dateTimeFormatter)).append("\n");
        } else if (getNextTriggerTimeWithPause().orElse(null) != null) {
            sb.append("  │ Current Window: ").append(getNextTriggerTimeWithPause().get().toLocalDateTime().format(dateTimeFormatter))
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
            sb.append("  │ Random Range: ±").append(randomizerValue);
            switch (repeatCycle) {
                case MINUTES:
                    sb.append(" millisec\n");
                    break;
                case HOURS:
                    sb.append(" seconds\n");
                    break;
                case DAYS:
                    sb.append(" minutes\n");
                    break;         
                case WEEKS:
                    sb.append(" hours\n");
                    break;   
                default:
                    break;
            }
        }
        
        // Tracking info
        sb.append("  └─ Tracking ────────────────────────────────\n");
        sb.append("    Reset Count: ").append(currentValidResetCount);
        if (this.getMaximumNumberOfRepeats() > 0) {
            sb.append("/").append(getMaximumNumberOfRepeats());
        } else {
            sb.append(" (unlimited)");
        }
        sb.append("\n");
        if (lastValidResetTime != null) {
            sb.append("    Last Reset: ").append(lastValidResetTime.format(dateTimeFormatter)).append("\n");
        }
        sb.append("    Daily Reset Count: ").append(transientNumberOfResetsWithinDailyInterval).append("\n");
        sb.append("    Can Trigger Again: ").append(canTriggerAgain()).append("\n");
        
        // Add pause information
        if (isPaused()) {
            sb.append("    Paused: Yes\n");
            Duration currentPauseDuration = Duration.between(pauseStartTime, getNow());
            sb.append("    Current Pause Duration: ").append(formatDuration(currentPauseDuration)).append("\n");
        }
        if (totalPauseDuration.getSeconds() > 0) {
            sb.append("    Total Pause Duration: ").append(formatDuration(totalPauseDuration)).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Provides a detailed description of the time window condition with status information.
     * Includes the window times, repeat cycle, current status, progress, randomization,
     * and tracking information in a human-readable format.
     *
     * @return A detailed multi-line string with current status and configuration details
     */
    public String getDetailedDescription() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Basic description
        sb.append("Time Window Condition: Active from ").append(startTime.format(timeFormatter))
          .append(" to ").append(endTime.format(timeFormatter)).append("\n");
        
        // Repeat cycle
        if (repeatCycle == RepeatCycle.ONE_TIME) {
            sb.append("Schedule: One time only");
            if (!hasUnlimitedDateRange()) {
                sb.append(" (");
                if (isUnlimitedStartDate()) {
                    sb.append("No start limit");
                } else {
                    sb.append(startDate.format(dateFormatter));
                }
                sb.append(" - ");
                if (isUnlimitedEndDate()) {
                    sb.append("No end limit");
                } else {
                    sb.append(endDate.format(dateFormatter));
                }
                sb.append(")");
            }
            sb.append("\n");
        } else {
            sb.append("Schedule: Repeats every ").append(repeatIntervalUnit).append(" ")
              .append(repeatCycle.toString().toLowerCase()).append("\n");
            if (!hasUnlimitedDateRange()) {
                sb.append("Valid period: ");
                if (isUnlimitedStartDate()) {
                    sb.append("No start limit");
                } else {
                    sb.append(startDate.format(dateFormatter));
                }
                sb.append(" - ");
                if (isUnlimitedEndDate()) {
                    sb.append("No end limit");
                } else {
                    sb.append(endDate.format(dateFormatter));
                }
                sb.append("\n");
            }
        }
        
        // Status information
        boolean satisfied = isSatisfied();
        sb.append("Status: ").append(satisfied ? "Active (in time window)" : "Inactive (outside time window)").append("\n");
        
        // Current window information
        ZonedDateTime now = ZonedDateTime.now(getZoneId());
        LocalDateTime nowLocal = now.toLocalDateTime();
        
        if (getNextTriggerTimeWithPause() != null && currentEndDateTime != null) {
            sb.append("Current window: ").append(getNextTriggerTimeWithPause().get().toLocalDateTime().format(dateTimeFormatter))
              .append(" to ").append(currentEndDateTime.format(dateTimeFormatter)).append("\n");
              
            if (nowLocal.isAfter(getNextTriggerTimeWithPause().get().toLocalDateTime()) && nowLocal.isBefore(currentEndDateTime)) {
                sb.append("Time remaining: ")
                  .append(ChronoUnit.MINUTES.between(nowLocal, currentEndDateTime))
                  .append(" minutes\n");
                sb.append("Progress: ").append(String.format("%.1f%%", getProgressPercentage())).append("\n");
            } else if (nowLocal.isBefore(getNextTriggerTimeWithPause().get().toLocalDateTime())) {
                sb.append("Window starts in: ")
                  .append(ChronoUnit.MINUTES.between(nowLocal, getNextTriggerTimeWithPause().get().toLocalDateTime()))
                  .append(" minutes\n");
            } else {
                sb.append("Window has passed\n");
            }
        } else {
            sb.append("No active window available\n");
        }
        if (useRandomization) {
            sb.append("  │ Random Range: ±").append(randomizerValue);
            switch (repeatCycle) {
                case MINUTES:
                    sb.append(" millisec\n");
                    break;
                case HOURS:
                    sb.append(" seconds\n");
                    break;
                case DAYS:
                    sb.append(" minutes\n");
                    break;         
                case WEEKS:
                    sb.append(" hours\n");
                    break;   
                default:
                    break;
            }
        }else {
            sb.append("Randomization: Disabled\n");
        }
        
        // Reset tracking
        sb.append("Reset count: ").append(currentValidResetCount);
        if (getMaximumNumberOfRepeats() > 0) {
            sb.append("/").append(getMaximumNumberOfRepeats());
        } else {
            sb.append(" (unlimited)");
        }
        sb.append("\n");
        
        if (lastValidResetTime != null) {
            sb.append("Last reset: ").append(lastValidResetTime.format(dateTimeFormatter)).append("\n");
        }
        
        // Timezone information
        sb.append("Timezone: ").append(getZoneId().getId()).append("\n");
        
        return sb.toString();
    }

    /**
     * Called when the condition is resumed.
     * Shifts time windows by the pause duration to maintain the same
     * relative timing after a pause.
     * 
     * @param pauseDuration The duration of the most recent pause
     */
    @Override
    protected void onResume(Duration pauseDuration) {
        if (isPaused()) {
            return;
        }
        ZonedDateTime currentTriggerTimeWithOutPause = getNextTriggerTimeWithPause().orElse(null);
        ZonedDateTime newTriggerTime = currentTriggerTimeWithOutPause.plus(pauseDuration);
        if (currentTriggerTimeWithOutPause != null) {            
            if (isSatisfied(newTriggerTime.toLocalDateTime())){
                setNextTriggerTime(currentTriggerTimeWithOutPause);
            }
            // Recalculate the next window after being paused
            // This will adjust the timing based on the current time
            calculateNextWindow(newTriggerTime.toLocalDateTime());
        }
        
        
        log.debug("TimeWindowCondition resumed after {}, window recalculated, new trigger time: {}", 
                formatDuration(pauseDuration),  getNextTriggerTimeWithPause().orElse(null));
    }
}