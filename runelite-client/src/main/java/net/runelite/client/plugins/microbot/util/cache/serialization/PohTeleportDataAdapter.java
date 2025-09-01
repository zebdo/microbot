package net.runelite.client.plugins.microbot.util.cache.serialization;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.util.cache.model.PohTeleportData;

import java.lang.reflect.Type;

/**
 * Gson adapter for PohTeleportData serialization/deserialization.
 * Handles safe serialization of POH teleport cache data for persistent storage.
 * <p>
 * This adapter serializes only the essential data needed to reconstruct the PohTeleportData:
 * - The enum class name for type safety
 * - The teleportable names (enum names) for reconstruction
 * <p>
 * The PohTransport objects are reconstructed on deserialization to avoid serialization complexity.
 */
@Slf4j
public class PohTeleportDataAdapter implements JsonSerializer<PohTeleportData>, JsonDeserializer<PohTeleportData> {

    @Override
    public JsonElement serialize(PohTeleportData src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();

        try {
            // Store the enum class name for type safety during deserialization
            if (src.getClazz() != null) {
                json.addProperty("enumClassName", src.getClazz().getName());
            }

            // Store teleportable names as a JSON array
            if (src.getTeleportableNames() != null && !src.getTeleportableNames().isEmpty()) {
                JsonArray namesArray = new JsonArray();
                for (String name : src.getTeleportableNames()) {
                    namesArray.add(name);
                }
                json.add("teleportableNames", namesArray);
            }

            // We don't serialize PohTransport objects as they can be reconstructed from teleportableNames
            // This avoids potential serialization issues with complex transport objects

        } catch (Exception e) {
            // Create minimal fallback serialization
            json.addProperty("enumClassName", src.getClazz() != null ? src.getClazz().getName() : null);
            json.add("teleportableNames", new JsonArray());
        }

        return json;
    }

    @Override
    public PohTeleportData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject jsonObject = json.getAsJsonObject();
        log.debug("Deserializing PohTeleportData: {}", jsonObject);

        try {
            PohTeleportData data = new PohTeleportData();

            // Deserialize enum class
            if (jsonObject.has("enumClassName") && !jsonObject.get("enumClassName").isJsonNull()) {
                String className = jsonObject.get("enumClassName").getAsString();
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum> enumClass = (Class<? extends Enum>) Class.forName(className);
                    data.setClazz(enumClass);
                } catch (ClassNotFoundException e) {
                    log.warn("Failed to deserialize PohTeleportData: enum class not found: {}", className);
                    data.setClazz(null);
                }
            } else {
                log.warn("Failed to deserialize PohTeleportData: enumClassName not found");
            }

            // Deserialize teleportable names
            if (jsonObject.has("teleportableNames") && !jsonObject.get("teleportableNames").isJsonNull()) {
                JsonArray namesArray = jsonObject.getAsJsonArray("teleportableNames");
                for (JsonElement nameElement : namesArray) {
                    String name = nameElement.getAsString();
                    data.addTransportable(name);
                    log.debug("Added teleportable name: {}", name);
                }
            }

            return data;

        } catch (Exception e) {
            throw new JsonParseException("Failed to deserialize PohTeleportData: " + e.getMessage(), e);
        }
    }
}
