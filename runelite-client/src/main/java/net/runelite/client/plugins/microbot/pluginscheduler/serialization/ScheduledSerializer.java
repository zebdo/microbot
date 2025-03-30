package net.runelite.client.plugins.microbot.pluginscheduler.serialization;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.ConditionManager;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.type.PluginScheduleEntry;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles serialization and deserialization of ScheduledPlugin objects.
 * This centralizes all JSON conversion logic.
 */
@Slf4j
public class ScheduledSerializer {
    
    /**
     * Creates a properly configured Gson instance with all necessary type adapters
     */
    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
                .registerTypeAdapter(Condition.class, new ConditionTypeAdapter())
                .registerTypeAdapter(LogicalCondition.class, new ConditionTypeAdapter())
                .registerTypeAdapter(ConditionManager.class, new ConditionManagerAdapter())
                .addSerializationExclusionStrategy(new ExcludeTransientAndNonSerializableFieldsStrategy())
                .create();
    }
    
    /**
     * Serialize a list of ScheduledPlugin objects to JSON
     */
    public static String toJson(List<PluginScheduleEntry> plugins) {
        try {
            return createGson().toJson(plugins);
        } catch (Exception e) {
            log.error("Error serializing scheduled plugins", e);
            return "[]";
        }
    }
    
    /**
     * Deserialize a JSON string to a list of ScheduledPlugin objects
     */
    public static List<PluginScheduleEntry> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // Check if the JSON contains the old class name
            if (json.contains("\"ScheduledPlugin\"")) {
                // Replace the class name in the JSON if it's explicitly stored
                json = json.replace("\"ScheduledPlugin\"", "\"PluginScheduleEntry\"");
            }
            
            // Create the Gson instance with your existing configuration
            Gson gson = createGson();
            
            // Create a type token for the new class
            Type listType = new TypeToken<ArrayList<PluginScheduleEntry>>(){}.getType();
            
            // Parse and return
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            log.error("Error deserializing scheduled plugins", e);
            return new ArrayList<>();
        }
    }
}
