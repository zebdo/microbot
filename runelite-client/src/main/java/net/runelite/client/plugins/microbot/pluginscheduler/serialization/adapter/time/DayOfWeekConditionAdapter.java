package net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.DayOfWeekCondition;

import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

/**
 * Serializes and deserializes DayOfWeekCondition objects
 */
@Slf4j
public class DayOfWeekConditionAdapter implements JsonSerializer<DayOfWeekCondition>, JsonDeserializer<DayOfWeekCondition> {
    @Override
    public JsonElement serialize(DayOfWeekCondition src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        
        // Serialize active days as an array of day values
        JsonArray daysArray = new JsonArray();
        for (DayOfWeek day : src.getActiveDays()) {
            daysArray.add(day.getValue()); // getValue() returns 1-7 for MON-SUN
        }
        json.add("activeDays", daysArray);
        
        return json;
    }
    
    @Override
    public DayOfWeekCondition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            Set<DayOfWeek> activeDays = EnumSet.noneOf(DayOfWeek.class);
            
            // Parse active days
            if (jsonObject.has("activeDays")) {
                JsonArray daysArray = jsonObject.getAsJsonArray("activeDays");
                for (JsonElement element : daysArray) {
                    int dayValue = element.getAsInt();
                    // DayOfWeek.of expects 1-7 for MON-SUN
                    activeDays.add(DayOfWeek.of(dayValue));
                }
            }
            
            return new DayOfWeekCondition(activeDays);
        } catch (Exception e) {
            log.error("Error deserializing DayOfWeekCondition", e);
            // Return a default condition (all days) on error
            return DayOfWeekCondition.weekdays(); // Default to weekdays
        }
    }
}