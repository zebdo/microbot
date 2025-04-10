package net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.enums.RepeatCycle;

import java.lang.reflect.Type;
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Custom serializer/deserializer for TimeWindowCondition that handles timezone conversion
 */
@Slf4j
public class TimeWindowConditionAdapter implements JsonSerializer<TimeWindowCondition>, JsonDeserializer<TimeWindowCondition> {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_TIME;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_DATE;
    
    @Override
    public JsonElement serialize(TimeWindowCondition src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        
        // Get source timezone
        ZoneId sourceZone = src.getZoneId() != null ? src.getZoneId() : ZoneId.systemDefault();
        
        // Store the timezone ID for deserialization
        json.addProperty("zoneId", sourceZone.getId());
        
        // Store times in UTC, converting from source timezone
        LocalDate today = LocalDate.now();
        
        // Convert start time to UTC
        ZonedDateTime startZoned = ZonedDateTime.of(today, src.getStartTime(), sourceZone);
        ZonedDateTime startUtc = startZoned.withZoneSameInstant(ZoneId.of("UTC"));
        
        // Convert end time to UTC
        ZonedDateTime endZoned = ZonedDateTime.of(today, src.getEndTime(), sourceZone);
        ZonedDateTime endUtc = endZoned.withZoneSameInstant(ZoneId.of("UTC"));
        
        // Convert dates to UTC (using noon to avoid DST issues)
        ZonedDateTime startDateZoned = ZonedDateTime.of(src.getStartDate(), LocalTime.NOON, sourceZone);
        ZonedDateTime startDateUtc = startDateZoned.withZoneSameInstant(ZoneId.of("UTC"));
        
        ZonedDateTime endDateZoned = ZonedDateTime.of(src.getEndDate(), LocalTime.NOON, sourceZone);
        ZonedDateTime endDateUtc = endDateZoned.withZoneSameInstant(ZoneId.of("UTC"));
        
        // Store UTC times
        json.addProperty("startTime", startUtc.toLocalTime().format(TIME_FORMAT));
        json.addProperty("endTime", endUtc.toLocalTime().format(TIME_FORMAT));
        json.addProperty("startDate", startDateUtc.toLocalDate().format(DATE_FORMAT));
        json.addProperty("endDate", endDateUtc.toLocalDate().format(DATE_FORMAT));
        
        // Mark that these are UTC times for future compatibility
        json.addProperty("timeFormat", "UTC");
        
        // Repeat cycle information
        json.addProperty("repeatCycle", src.getRepeatCycle().name());
        json.addProperty("repeatInterval", src.getRepeatIntervalUnit());
        
        // Randomization settings
        json.addProperty("useRandomization", src.isUseRandomization());
        json.addProperty("randomizeMinutes", src.getRandomizeMinutes());
        
        return json;
    }
    
    @Override
    public TimeWindowCondition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            
            // Get the target timezone for conversion (default to system)
            ZoneId targetZone = ZoneId.systemDefault();
            if (jsonObject.has("zoneId")) {
                try {
                    targetZone = ZoneId.of(jsonObject.get("zoneId").getAsString());
                } catch (Exception e) {
                    log.warn("Invalid zoneId in serialized TimeWindowCondition", e);
                }
            }
            
            // Check if times are stored in UTC format
            boolean isUtcFormat = jsonObject.has("timeFormat") && 
                                  "UTC".equals(jsonObject.get("timeFormat").getAsString());
            
            // Parse time values
            LocalTime serializedStartTime = LocalTime.parse(jsonObject.get("startTime").getAsString(), TIME_FORMAT);
            LocalTime serializedEndTime = LocalTime.parse(jsonObject.get("endTime").getAsString(), TIME_FORMAT);
            
            // Parse date values
            LocalDate serializedStartDate = LocalDate.parse(jsonObject.get("startDate").getAsString(), DATE_FORMAT);
            LocalDate serializedEndDate = LocalDate.parse(jsonObject.get("endDate").getAsString(), DATE_FORMAT);
            
            LocalTime startTime;
            LocalTime endTime;
            LocalDate startDate;
            LocalDate endDate;
            
            if (isUtcFormat) {
                // If stored in UTC format, convert back to target timezone
                LocalDate today = LocalDate.now();
                
                // Convert start time from UTC to target zone
                ZonedDateTime startUtc = ZonedDateTime.of(today, serializedStartTime, ZoneId.of("UTC"));
                ZonedDateTime startTargetZone = startUtc.withZoneSameInstant(targetZone);
                startTime = startTargetZone.toLocalTime();
                
                // Convert end time from UTC to target zone
                ZonedDateTime endUtc = ZonedDateTime.of(today, serializedEndTime, ZoneId.of("UTC"));
                ZonedDateTime endTargetZone = endUtc.withZoneSameInstant(targetZone);
                endTime = endTargetZone.toLocalTime();
                
                // Convert dates from UTC to target zone
                ZonedDateTime startDateUtc = ZonedDateTime.of(serializedStartDate, LocalTime.NOON, ZoneId.of("UTC"));
                ZonedDateTime startDateTarget = startDateUtc.withZoneSameInstant(targetZone);
                startDate = startDateTarget.toLocalDate();
                
                ZonedDateTime endDateUtc = ZonedDateTime.of(serializedEndDate, LocalTime.NOON, ZoneId.of("UTC"));
                ZonedDateTime endDateTarget = endDateUtc.withZoneSameInstant(targetZone);
                endDate = endDateTarget.toLocalDate();
            } else {
                // Legacy format - use times as-is
                startTime = serializedStartTime;
                endTime = serializedEndTime;
                startDate = serializedStartDate;
                endDate = serializedEndDate;
            }
            
            // Parse repeat cycle
            RepeatCycle repeatCycle = RepeatCycle.valueOf(
                    jsonObject.get("repeatCycle").getAsString());
            int repeatInterval = jsonObject.get("repeatInterval").getAsInt();
            
            // Create the condition with the parsed values
            TimeWindowCondition condition = new TimeWindowCondition(
                    startTime, endTime, startDate, endDate, repeatCycle, repeatInterval);
            
            // Set timezone
            condition.setZoneId(targetZone);
            
            // Set randomization if present
            if (jsonObject.has("useRandomization") && jsonObject.has("randomizeMinutes")) {
                boolean useRandomization = jsonObject.get("useRandomization").getAsBoolean();
                int randomizeMinutes = jsonObject.get("randomizeMinutes").getAsInt();
                condition.setRandomization(useRandomization, randomizeMinutes);
            }                        
            return condition;
        } catch (Exception e) {
            log.error("Error deserializing TimeWindowCondition", e);
            // Return a default value on error
            return new TimeWindowCondition();
        }
    }
}