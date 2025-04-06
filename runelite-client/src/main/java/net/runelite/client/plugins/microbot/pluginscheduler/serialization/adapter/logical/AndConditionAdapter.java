package net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.logical;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.AndCondition;

import java.lang.reflect.Type;

/**
 * Serializes and deserializes AndCondition objects
 */
@Slf4j
public class AndConditionAdapter implements JsonSerializer<AndCondition>, JsonDeserializer<AndCondition> {
    @Override
    public JsonElement serialize(AndCondition src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        
        // Serialize the conditions
        JsonArray conditionsArray = new JsonArray();
        for (Condition condition : src.getConditions()) {
            conditionsArray.add(context.serialize(condition));
        }
        json.add("conditions", conditionsArray);
        
        return json;
    }
    
    @Override
    public AndCondition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            AndCondition andCondition = new AndCondition();
            
            // Handle conditions
            if (jsonObject.has("conditions")) {
                JsonArray conditionsArray = jsonObject.getAsJsonArray("conditions");
                for (JsonElement element : conditionsArray) {
                    Condition condition = context.deserialize(element, Condition.class);
                    if (condition != null) {
                        andCondition.addCondition(condition);
                    }
                }
            }
            
            return andCondition;
        } catch (Exception e) {
            log.error("Error deserializing AndCondition", e);
            // Return empty AndCondition on error
            return new AndCondition();
        }
    }
}