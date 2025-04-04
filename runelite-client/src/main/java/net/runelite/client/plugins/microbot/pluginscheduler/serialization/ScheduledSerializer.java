package net.runelite.client.plugins.microbot.pluginscheduler.serialization;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.ConditionManager;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.SingleTriggerTimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.ConditionTypeAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.LocalDateAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.LocalTimeAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.TimeWindowConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.ConditionManagerAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.SingleTriggerTimeConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.ZonedDateTimeAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.type.PluginScheduleEntry;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.IntervalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.DayOfWeekCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.AndCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.OrCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.IntervalConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.DayOfWeekConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.LogicalConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.AndConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.OrConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.DurationAdapter;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.Duration;
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
        GsonBuilder builder = new GsonBuilder()
                .setExclusionStrategies(new ExcludeTransientAndNonSerializableFieldsStrategy())
                .setPrettyPrinting();
        
        // Register all the type adapters
        builder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        builder.registerTypeAdapter(LocalTime.class, new LocalTimeAdapter());
        builder.registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter());
        builder.registerTypeAdapter(Duration.class, new DurationAdapter());  // Add the Duration adapter
        builder.registerTypeAdapter(Condition.class, new ConditionTypeAdapter());
        
        // Ensure all time condition classes have adapters registered
        builder.registerTypeAdapter(IntervalCondition.class, new IntervalConditionAdapter());
        builder.registerTypeAdapter(SingleTriggerTimeCondition.class, new SingleTriggerTimeConditionAdapter());
        builder.registerTypeAdapter(TimeWindowCondition.class, new TimeWindowConditionAdapter());
        builder.registerTypeAdapter(DayOfWeekCondition.class, new DayOfWeekConditionAdapter());
        
        // Logical condition adapters
        builder.registerTypeAdapter(LogicalCondition.class, new LogicalConditionAdapter());
        builder.registerTypeAdapter(AndCondition.class, new AndConditionAdapter());
        builder.registerTypeAdapter(OrCondition.class, new OrConditionAdapter());
        
        // Add missing ConditionManager adapter registration
        builder.registerTypeAdapter(ConditionManager.class, new ConditionManagerAdapter());

        // Register additional condition adapters
        
        
        return builder.create();
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
