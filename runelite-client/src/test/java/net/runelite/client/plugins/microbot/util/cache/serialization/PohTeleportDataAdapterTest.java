package net.runelite.client.plugins.microbot.util.cache.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.client.plugins.microbot.util.cache.model.PohTeleportData;
import net.runelite.client.plugins.microbot.util.poh.data.PohPortal;
import net.runelite.client.plugins.microbot.util.poh.data.PohTransport;

/**
 * Test class for PohTeleportDataAdapter deserialization functionality.
 * This class provides a simple way to test the adapter with sample JSON data.
 */
public class PohTeleportDataAdapterTest {

    /**
     * Creates a Gson instance configured with the PohTeleportDataAdapter.
     */
    private static Gson createTestGson() {
        return new GsonBuilder()
                .registerTypeAdapter(PohTeleportData.class, new PohTeleportDataAdapter())
                .setPrettyPrinting()
                .create();
    }

    /**
     * Tests deserialization of the provided JSON string.
     */
    public static void testDeserialization() {
        String testJson = "{\"enumClassName\":\"net.runelite.client.plugins.microbot.util.poh.data.PohPortal\",\"teleportableNames\":[\"WEISS\",\"TROLL_STRONGHOLD\",\"LUNAR_ISLE\",\"BARROWS\"]}";

        System.out.println("=== PohTeleportDataAdapter Test ===");
        System.out.println("Input JSON:");
        System.out.println(testJson);
        System.out.println();

        try {
            Gson gson = createTestGson();
            PohTeleportData deserializedData = gson.fromJson(testJson, PohTeleportData.class);

            System.out.println("Deserialization Result:");
            System.out.println("- Enum Class: " + (deserializedData.getClazz() != null ? deserializedData.getClazz().getSimpleName() : "null"));
            System.out.println("- Teleportable Names Count: " + deserializedData.getTeleportableNames().size());
            System.out.println("- Teleportable Names: " + deserializedData.getTeleportableNames());
            System.out.println("- Reconstructed Transports Count: " + deserializedData.getTransportCount());
            System.out.println();

            // Test individual portals
            System.out.println("Individual Portal Details:");
            for (String portalName : deserializedData.getTeleportableNames()) {
                try {
                    PohPortal portal = PohPortal.valueOf(portalName);
                    System.out.println("- " + portalName + ": " + portal.getDisplayName() + " -> " + portal.getDestination());
                } catch (Exception e) {
                    System.out.println("- " + portalName + ": ERROR - " + e.getMessage());
                }
            }
            System.out.println();

            // Test reconstructed transports
            System.out.println("Reconstructed Transport Details:");
            for (PohTransport transport : deserializedData.getTransports()) {
                System.out.println("- " + transport.getDisplayInfo());
                System.out.println("  Destination: " + transport.getDestination());
                System.out.println("  Time: " + transport.getDuration() + "s");
            }
            System.out.println();

            // Test round-trip serialization
            System.out.println("Round-trip Test (Serialize -> Deserialize):");
            String reserializedJson = gson.toJson(deserializedData);
            System.out.println("Re-serialized JSON:");
            System.out.println(reserializedJson);

            PohTeleportData roundTripData = gson.fromJson(reserializedJson, PohTeleportData.class);
            boolean roundTripSuccess =
                    deserializedData.getTeleportableNames().equals(roundTripData.getTeleportableNames()) &&
                            deserializedData.getTransportCount() == roundTripData.getTransportCount() &&
                            deserializedData.getClazz().equals(roundTripData.getClazz());

            System.out.println("Round-trip successful: " + roundTripSuccess);

        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== Test Complete ===");
    }

    /**
     * Tests edge cases and error conditions.
     */
    public static void testEdgeCases() {
        System.out.println("\n=== Edge Case Tests ===");
        Gson gson = createTestGson();

        // Test 1: Missing enum class
        String noEnumClassJson = "{\"teleportableNames\":[\"VARROCK\",\"LUMBRIDGE\"]}";
        System.out.println("Test 1: Missing enum class");
        testSingleCase(gson, noEnumClassJson);

        // Test 2: Invalid enum class
        String invalidEnumClassJson = "{\"enumClassName\":\"com.invalid.Class\",\"teleportableNames\":[\"VARROCK\"]}";
        System.out.println("Test 2: Invalid enum class");
        testSingleCase(gson, invalidEnumClassJson);

        // Test 3: Invalid enum values
        String invalidEnumValuesJson = "{\"enumClassName\":\"net.runelite.client.plugins.microbot.util.poh.data.PohPortal\",\"teleportableNames\":[\"INVALID_PORTAL\",\"ANOTHER_INVALID\"]}";
        System.out.println("Test 3: Invalid enum values");
        testSingleCase(gson, invalidEnumValuesJson);

        // Test 4: Empty teleportable names
        String emptyNamesJson = "{\"enumClassName\":\"net.runelite.client.plugins.microbot.util.poh.data.PohPortal\",\"teleportableNames\":[]}";
        System.out.println("Test 4: Empty teleportable names");
        testSingleCase(gson, emptyNamesJson);

        // Test 5: Null values
        String nullValuesJson = "{\"enumClassName\":null,\"teleportableNames\":null}";
        System.out.println("Test 5: Null values");
        testSingleCase(gson, nullValuesJson);

        System.out.println("=== Edge Case Tests Complete ===");
    }

    private static void testSingleCase(Gson gson, String json) {
        try {
            PohTeleportData data = gson.fromJson(json, PohTeleportData.class);
            System.out.println("  SUCCESS: Enum=" + (data.getClazz() != null ? data.getClazz().getSimpleName() : "null") +
                    ", Names=" + data.getTeleportableNames().size() +
                    ", Transports=" + data.getTransportCount());
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }

    /**
     * Main method to run all tests.
     */
    public static void main(String[] args) {
        testDeserialization();
        testEdgeCases();
    }
}
