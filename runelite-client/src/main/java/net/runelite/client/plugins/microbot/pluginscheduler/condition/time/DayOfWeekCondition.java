package net.runelite.client.plugins.microbot.pluginscheduler.condition.time;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Condition that is met on specific days of the week.
 * This allows scheduling tasks to run only on certain days.
 */
@Getter
@Slf4j
public class DayOfWeekCondition extends TimeCondition {
    private final Set<DayOfWeek> activeDays;
    
    /**
     * Creates a day of week condition for the specified days
     */
    public DayOfWeekCondition(Set<DayOfWeek> activeDays) {
        this.activeDays = EnumSet.copyOf(activeDays);
    }
    
    /**
     * Creates a day of week condition for the specified days
     */
    public DayOfWeekCondition(DayOfWeek... days) {
        this.activeDays = EnumSet.noneOf(DayOfWeek.class);
        this.activeDays.addAll(Arrays.asList(days));
    }
    
    /**
     * Creates a condition for weekdays (Monday through Friday)
     */
    public static DayOfWeekCondition weekdays() {
        return new DayOfWeekCondition(
                DayOfWeek.MONDAY, 
                DayOfWeek.TUESDAY, 
                DayOfWeek.WEDNESDAY, 
                DayOfWeek.THURSDAY, 
                DayOfWeek.FRIDAY);
    }
    
    /**
     * Creates a condition for weekends (Saturday and Sunday)
     */
    public static DayOfWeekCondition weekends() {
        return new DayOfWeekCondition(
                DayOfWeek.SATURDAY, 
                DayOfWeek.SUNDAY);
    }
    
    /**
     * Creates a day of week condition from a ZonedDateTime, using its day of week.
     * 
     * @param dateTime The date/time to extract day of week from
     * @return A condition for that specific day of the week
     */
    public static DayOfWeekCondition fromZonedDateTime(ZonedDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        return new DayOfWeekCondition(day);
    }

    /**
     * Creates a randomized DayOfWeekCondition with a random number of consecutive days.
     * 
     * @param minDays Minimum number of consecutive days
     * @param maxDays Maximum number of consecutive days
     * @return A condition with randomly selected consecutive days
     */
    public static DayOfWeekCondition createRandomized(int minDays, int maxDays) {
        int numDays = Rs2Random.between(minDays, maxDays);
        numDays = Math.min(numDays, 7); // Cap at 7 days
        
        // Randomly pick a starting day
        int startDayIndex = Rs2Random.between(0, 6);
        Set<DayOfWeek> selectedDays = EnumSet.noneOf(DayOfWeek.class);
        
        for (int i = 0; i < numDays; i++) {
            int dayIndex = (startDayIndex + i) % 7;
            selectedDays.add(DayOfWeek.of(dayIndex == 0 ? 7 : dayIndex)); // DayOfWeek is 1-based
        }
        
        return new DayOfWeekCondition(selectedDays);
    }

    @Override
    public boolean isSatisfied() {                
        DayOfWeek today = getNow().getDayOfWeek();
        return activeDays.contains(today);
    }

    @Override
    public String getDescription() {
        if (activeDays.isEmpty()) {
            return "No active days";
        }
        
        if (activeDays.size() == 7) {
            return "Every day";
        }
        
        if (activeDays.size() == 5 && 
            activeDays.contains(DayOfWeek.MONDAY) &&
            activeDays.contains(DayOfWeek.TUESDAY) &&
            activeDays.contains(DayOfWeek.WEDNESDAY) &&
            activeDays.contains(DayOfWeek.THURSDAY) &&
            activeDays.contains(DayOfWeek.FRIDAY)) {
            return "Weekdays";
        }
        
        if (activeDays.size() == 2 && 
            activeDays.contains(DayOfWeek.SATURDAY) &&
            activeDays.contains(DayOfWeek.SUNDAY)) {
            return "Weekends";
        }
        
        return "On " + activeDays.stream()
            .map(day -> day.toString().charAt(0) + day.toString().substring(1).toLowerCase())
            .collect(Collectors.joining(", "));
    }

    @Override
    public void reset(boolean randomize) {
        // Nothing to reset for day of week condition
    }
    
    @Override
    public double getProgressPercentage() {
        if (isSatisfied()) {
            return 100.0;
        }
        
        // If today isn't active, find the next active day
        ZonedDateTime now = getNow();
        DayOfWeek today = now.getDayOfWeek();
        
        // If no active days, return 0
        if (activeDays.isEmpty()) {
            return 0.0;
        }
        
        // Find number of days until next active day
        int daysUntilNext = 1;
        while (daysUntilNext <= 7) {
            DayOfWeek nextDay = today.plus(daysUntilNext);
            if (activeDays.contains(nextDay)) {
                break;
            }
            daysUntilNext++;
        }
        
        // If we couldn't find a day, return 0
        if (daysUntilNext > 7) {
            return 0.0;
        }
        
        // Calculate percentage based on how close we are to the next active day
        return 100.0 * (1.0 - (daysUntilNext / 7.0));
    }
    
    @Override
    public Optional<ZonedDateTime> getCurrentTriggerTime() {
        ZonedDateTime now = getNow();
        DayOfWeek today = now.getDayOfWeek();
        
        // If today is an active day and it's already satisfied, return slightly in the past
        if (activeDays.contains(today) && isSatisfied()) {
            return Optional.of(now.minusSeconds(1)); // Slightly in the past to indicate "ready now"
        }
        
        // If no active days defined, return empty
        if (activeDays.isEmpty()) {
            return Optional.empty();
        }
        
        // Find the next active day
        int daysToAdd = 1;
        while (daysToAdd <= 7) {
            DayOfWeek checkDay = today.plus(daysToAdd);
            if (activeDays.contains(checkDay)) {
                // Found the next active day, return the start of that day
                ZonedDateTime nextDay = now.plusDays(daysToAdd)
                    .truncatedTo(java.time.temporal.ChronoUnit.DAYS); // Start of the day
                return Optional.of(nextDay);
            }
            daysToAdd++;
        }
        
        // If we looked through all 7 days and found nothing (shouldn't happen with the isEmpty check above)
        return Optional.empty();
    }
    
    /**
     * Finds the next day that is NOT an active day
     * 
     * @return Optional containing the start time of the next non-active day
     */
    public Optional<ZonedDateTime> getNextNonActiveDay() {
        ZonedDateTime now = getNow();
        DayOfWeek today = now.getDayOfWeek();
        
        // If today is already non-active, return today
        if (!activeDays.contains(today)) {
            return Optional.of(now.truncatedTo(java.time.temporal.ChronoUnit.DAYS));
        }
        
        // Otherwise, find the next non-active day
        int daysToAdd = 1;
        while (daysToAdd <= 7) {
            DayOfWeek nextDay = today.plus(daysToAdd);
            if (!activeDays.contains(nextDay)) {
                // Found the next non-active day
                ZonedDateTime nextNonActiveDay = now.plusDays(daysToAdd)
                    .truncatedTo(java.time.temporal.ChronoUnit.DAYS); // Start of the day
                return Optional.of(nextNonActiveDay);
            }
            daysToAdd++;
        }
        
        // If all days are active, return empty
        return Optional.empty();
    }
    
    @Subscribe
    public void onGameTick(GameTick tick) {
        // Just used to ensure we stay registered with the event bus
    }
}