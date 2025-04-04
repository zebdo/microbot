package net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.SingleTriggerTimeCondition;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Custom serializer/deserializer for SingleTriggerTimeCondition
 */
@Slf4j
public class SingleTriggerTimeConditionAdapter implements JsonSerializer<SingleTriggerTimeCondition>, JsonDeserializer<SingleTriggerTimeCondition> {
    
    @Override
    public JsonElement serialize(SingleTriggerTimeCondition src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        
        // Store timezone information
        json.addProperty("zoneId", src.getTargetTime().getZone().getId());
        
        // Store target time as epoch millis for cross-platform compatibility
        json.addProperty("targetTimeMillis", src.getTargetTime().toInstant().toEpochMilli());
        
        // Store trigger state
        json.addProperty("hasTriggered", src.isHasTriggered());
        
        return json;
    }
    
    @Override
    public SingleTriggerTimeCondition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            
            // Get timezone (fallback to system default)
            ZoneId zoneId = ZoneId.systemDefault();
            if (jsonObject.has("zoneId")) {
                try {
                    zoneId = ZoneId.of(jsonObject.get("zoneId").getAsString());
                } catch (Exception e) {
                    log.warn("Invalid zoneId in serialized SingleTriggerTimeCondition", e);
                }
            }
            
            // Get target time
            long targetTimeMillis = jsonObject.get("targetTimeMillis").getAsLong();
            ZonedDateTime targetTime = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(targetTimeMillis), zoneId);
            
            // Create condition
            SingleTriggerTimeCondition condition = new SingleTriggerTimeCondition(targetTime);
            
            // Apply trigger state if present
            if (jsonObject.has("hasTriggered") && jsonObject.get("hasTriggered").getAsBoolean()) {
                // Since we can't directly modify hasTriggered, we call reset() which sets hasResetAfterTrigger
                condition.reset(false);
            }
            
            return condition;
        } catch (Exception e) {
            log.error("Error deserializing SingleTriggerTimeCondition", e);
            // Return a default condition that triggers after 24 hours
            return SingleTriggerTimeCondition.afterDelay(24 * 60 * 60);
        }
    }
}