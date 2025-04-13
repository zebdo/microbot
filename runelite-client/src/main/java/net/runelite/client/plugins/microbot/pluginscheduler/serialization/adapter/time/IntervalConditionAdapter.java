package net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.IntervalCondition;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Serializes and deserializes IntervalCondition objects
 */
@Slf4j
public class IntervalConditionAdapter implements JsonSerializer<IntervalCondition>, JsonDeserializer<IntervalCondition> {
    @Override
    public JsonElement serialize(IntervalCondition src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        
        // Store interval as seconds for cross-platform compatibility
        json.addProperty("intervalSeconds", src.getInterval().getSeconds());
        
        // Store randomization settings
        json.addProperty("randomize", src.isRandomize());
        json.addProperty("randomFactor", src.getRandomFactor());
        json.addProperty("maximumNumberOfRepeats", src.getMaximumNumberOfRepeats());
        
        // Store next trigger time if available
        Optional<ZonedDateTime> nextTrigger = src.getCurrentTriggerTime();
        if (nextTrigger.isPresent()) {
            json.addProperty("nextTriggerTimeMillis", nextTrigger.get().toInstant().toEpochMilli());
        }
        
        return json;
    }
    
    @Override
    public IntervalCondition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            
            // Parse interval
            long intervalSeconds = jsonObject.get("intervalSeconds").getAsLong();
            Duration interval = Duration.ofSeconds(intervalSeconds);
            
            // Parse randomization settings
            boolean randomize = jsonObject.has("randomize") && jsonObject.get("randomize").getAsBoolean();
            double randomFactor = randomize && jsonObject.has("randomFactor") ? 
                                jsonObject.get("randomFactor").getAsDouble() : 0.0;
            long maximumNumberOfRepeats = jsonObject.has("maximumNumberOfRepeats") ? 
                                jsonObject.get("maximumNumberOfRepeats").getAsLong() : 0;
            // Create the condition and return it
            return new IntervalCondition(interval, randomize, randomFactor,maximumNumberOfRepeats);
        } catch (Exception e) {
            log.error("Error deserializing IntervalCondition", e);
            // Return a default condition on error (5 minute interval)
            return new IntervalCondition(Duration.ofMinutes(5));
        }
    }
}