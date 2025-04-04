package net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.enums.RepeatCycle;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
        
        // Store the timezone ID used for serialization to assist with deserialization
        json.addProperty("zoneId", ZoneId.systemDefault().getId());
        
        // Store times in the system timezone (will be interpreted as UTC by the server)
        json.addProperty("startTime", src.getStartTime().format(TIME_FORMAT));
        json.addProperty("endTime", src.getEndTime().format(TIME_FORMAT));
        json.addProperty("startDate", src.getStartDate().format(DATE_FORMAT));
        json.addProperty("endDate", src.getEndDate().format(DATE_FORMAT));
        
        // Repeat cycle information
        json.addProperty("repeatCycle", src.getRepeatCycle().name());
        json.addProperty("repeatInterval", src.getRepeatInterval());
        
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
            
            // Get the original serialization timezone (fallback to UTC if not present)
            ZoneId originalZone = ZoneId.systemDefault();
            if (jsonObject.has("zoneId")) {
                try {
                    originalZone = ZoneId.of(jsonObject.get("zoneId").getAsString());
                } catch (Exception e) {
                    log.warn("Invalid zoneId in serialized TimeWindowCondition", e);
                }
            }
            
            // Parse time values
            LocalTime startTime = LocalTime.parse(jsonObject.get("startTime").getAsString(), TIME_FORMAT);
            LocalTime endTime = LocalTime.parse(jsonObject.get("endTime").getAsString(), TIME_FORMAT);
            
            // Parse date values
            LocalDate startDate = LocalDate.parse(jsonObject.get("startDate").getAsString(), DATE_FORMAT);
            LocalDate endDate = LocalDate.parse(jsonObject.get("endDate").getAsString(), DATE_FORMAT);
            
            // Parse repeat cycle
            RepeatCycle repeatCycle = RepeatCycle.valueOf(
                    jsonObject.get("repeatCycle").getAsString());
            int repeatInterval = jsonObject.get("repeatInterval").getAsInt();
            
            // Create the condition with the parsed values
            TimeWindowCondition condition = new TimeWindowCondition(
                    startTime, endTime, startDate, endDate, repeatCycle, repeatInterval);
            
            // Set timezone
            condition.setZoneId(originalZone);
            
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