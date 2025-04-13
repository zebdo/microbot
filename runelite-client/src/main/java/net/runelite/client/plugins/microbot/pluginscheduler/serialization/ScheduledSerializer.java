package net.runelite.client.plugins.microbot.pluginscheduler.serialization;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.ConditionManager;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.SingleTriggerTimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.ConditionTypeAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.ConditionManagerAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.ZonedDateTimeAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.logical.AndConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.logical.LogicalConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.logical.OrConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time.DayOfWeekConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time.DurationAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time.IntervalConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time.LocalDateAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time.LocalTimeAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time.SingleTriggerTimeConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time.TimeConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.time.TimeWindowConditionAdapter;
import net.runelite.client.plugins.microbot.pluginscheduler.type.PluginScheduleEntry;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.IntervalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.DayOfWeekCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.AndCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.OrCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.serialization.adapter.PluginScheduleEntryAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
        builder.registerTypeAdapter(Duration.class, new DurationAdapter());
        builder.registerTypeAdapter(Condition.class, new ConditionTypeAdapter());
        
        // Register our custom PluginScheduleEntry adapter
        builder.registerTypeAdapter(PluginScheduleEntry.class, new PluginScheduleEntryAdapter());
        
        // Other adapters still needed for individual conditions
        builder.registerTypeAdapter(TimeCondition.class, new TimeConditionAdapter());
        builder.registerTypeAdapter(IntervalCondition.class, new IntervalConditionAdapter());
        builder.registerTypeAdapter(SingleTriggerTimeCondition.class, new SingleTriggerTimeConditionAdapter());
        builder.registerTypeAdapter(TimeWindowCondition.class, new TimeWindowConditionAdapter());
        builder.registerTypeAdapter(DayOfWeekCondition.class, new DayOfWeekConditionAdapter());
        
        // Logical condition adapters
        builder.registerTypeAdapter(LogicalCondition.class, new LogicalConditionAdapter());
        builder.registerTypeAdapter(AndCondition.class, new AndConditionAdapter());
        builder.registerTypeAdapter(OrCondition.class, new OrConditionAdapter());
        
        // ConditionManager adapter
        builder.registerTypeAdapter(ConditionManager.class, new ConditionManagerAdapter());
        builder.registerTypeAdapter(Pattern.class, new TypeAdapter<Pattern>() {
            @Override
            public void write(JsonWriter out, Pattern value) throws IOException {
                if (value == null) {
                    out.nullValue();
                } else {
                    out.value(value.pattern());
                }
            }

            @Override
            public Pattern read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                String pattern = in.nextString();
                return Pattern.compile(pattern);
            }
        });
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
                json = json.replace("\"ScheduledPlugin\"", "\"PluginScheduleEntry\"");
            }
            
            Gson gson = createGson();
            Type listType = new TypeToken<ArrayList<PluginScheduleEntry>>(){}.getType();
            
            // Let Gson and our adapter handle everything
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            log.error("Error deserializing scheduled plugins", e);
            return new ArrayList<>();
        }
    }
}
