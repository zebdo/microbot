package net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.AndCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.OrCondition;

import java.lang.reflect.Type;

/**
 * Serializes and deserializes LogicalCondition objects
 */
@Slf4j
public class LogicalConditionAdapter implements JsonSerializer<LogicalCondition>, JsonDeserializer<LogicalCondition> {
    @Override
    public JsonElement serialize(LogicalCondition src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        
        // Add class info to distinguish different logical conditions
        json.addProperty("class", src.getClass().getName());
        
        // Serialize the conditions
        JsonArray conditionsArray = new JsonArray();
        for (Condition condition : src.getConditions()) {
            conditionsArray.add(context.serialize(condition));
        }
        json.add("conditions", conditionsArray);
        
        return json;
    }
    
    @Override
    public LogicalCondition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            LogicalCondition logicalCondition;
            
            // Determine the concrete class
            if (jsonObject.has("class")) {
                String className = jsonObject.get("class").getAsString();
                if (className.contains("OrCondition")) {
                    logicalCondition = new OrCondition();
                } else {
                    // Default to AndCondition
                    logicalCondition = new AndCondition();
                }
            } else {
                // Default if no class info
                logicalCondition = new AndCondition();
            }
            
            // Handle conditions
            if (jsonObject.has("conditions")) {
                JsonArray conditionsArray = jsonObject.getAsJsonArray("conditions");
                for (JsonElement element : conditionsArray) {
                    Condition condition = context.deserialize(element, Condition.class);
                    if (condition != null) {
                        logicalCondition.addCondition(condition);
                    }
                }
            }
            
            return logicalCondition;
        } catch (Exception e) {
            log.error("Error deserializing LogicalCondition", e);
            // Return empty AndCondition on error
            return new AndCondition();
        }
    }
}