package net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.DayOfWeekCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.IntervalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.SingleTriggerTimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition;

import java.lang.reflect.Type;

// Then create a new TimeConditionAdapter.java class:
@Slf4j
public class TimeConditionAdapter implements JsonDeserializer<TimeCondition> {
    @Override
    public TimeCondition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        
        // Determine the concrete type
        if (jsonObject.has("intervalSeconds")) {
            return context.deserialize(jsonObject, IntervalCondition.class);
        } else if (jsonObject.has("targetTimeMillis")) {
            return context.deserialize(jsonObject, SingleTriggerTimeCondition.class);
        } else if (jsonObject.has("startTime") && jsonObject.has("endTime")) {
            return context.deserialize(jsonObject, TimeWindowCondition.class);
        } else if (jsonObject.has("activeDays")) {
            return context.deserialize(jsonObject, DayOfWeekCondition.class);
        }else{
            throw new JsonParseException("Unknown TimeCondition type");
        }
                
    }
}