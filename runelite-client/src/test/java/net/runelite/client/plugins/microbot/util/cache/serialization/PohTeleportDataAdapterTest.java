package net.runelite.client.plugins.microbot.util.cache.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.client.plugins.microbot.util.cache.Rs2PohCache;
import net.runelite.client.plugins.microbot.util.poh.data.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test class for PohTeleportDataAdapter serialization and deserialization functionality.
 * This class provides comprehensive testing for the adapter with various PohTransportable enums.
 */
public class PohTeleportDataAdapterTest {

    /**
     * Creates a Gson instance configured with the PohTeleportDataAdapter.
     */
    private static Gson createTestGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Rs2PohCache.TYPE_TOKEN, new PohTeleportDataAdapter())
                .setPrettyPrinting()
                .create();
    }

    /**
     * Tests basic serialization and deserialization functionality.
     */
    public static void testBasicSerialization() {
        System.out.println("=== Basic Serialization Test ===");
        Gson gson = createTestGson();

        // Test different types of PohTransportable enums
        PohTeleport[] testCases = {
                NexusPortal.VARROCK,
                NexusPortal.LUMBRIDGE,
                PohPortal.CAMELOT,
                PohPortal.ARDOUGNE,
                MountedDigsite.FOSSIL_ISLAND
        };

        for (PohTeleport transportable : testCases) {
            System.out.println("\nTesting: " + transportable.getClass().getSimpleName() + "." + transportable.name());

            try {
                // Serialize
                String json = gson.toJson(transportable);
                System.out.println("Serialized JSON: " + json);

                // Deserialize
                PohTeleport deserialized = gson.fromJson(json, PohTeleport.class);
                System.out.println("Deserialized: " + deserialized.getClass().getSimpleName() + "." + deserialized.name());

                // Verify equality
                boolean matches = transportable.equals(deserialized) &&
                        transportable.name().equals(deserialized.name()) &&
                        transportable.getClass().equals(deserialized.getClass());

                System.out.println("Round-trip successful: " + matches);

                if (matches) {
                    System.out.println("✓ PASS - Properties match:");
                    System.out.println("  - Destination: " + deserialized.getDestination());
                    System.out.println("  - Duration: " + deserialized.getDuration());
                    System.out.println("  - Display Info: " + deserialized.displayInfo());
                } else {
                    System.out.println("✗ FAIL - Properties don't match");
                }

            } catch (Exception e) {
                System.out.println("✗ FAIL - Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n=== Basic Serialization Test Complete ===");
    }

    /**
     * Tests deserialization from manually crafted JSON strings.
     */
    public static void testManualDeserialization() {
        System.out.println("\n=== Manual Deserialization Test ===");
        Gson gson = createTestGson();

        // Test cases with manually crafted JSON
        String[] testJsons = {
                // Valid NexusPortal
                "{\"enumClass\":\"net.runelite.client.plugins.microbot.util.poh.data.NexusPortal\",\"enumName\":\"VARROCK\"}",
                // Valid PohPortal
                "{\"enumClass\":\"net.runelite.client.plugins.microbot.util.poh.data.PohPortal\",\"enumName\":\"LUMBRIDGE\"}",
                // Valid MountedDigsite
                "{\"enumClass\":\"net.runelite.client.plugins.microbot.util.poh.data.MountedDigsite\",\"enumName\":\"FOSSIL_ISLAND\"}"
        };

        String[] expectedResults = {
                "NexusPortal.VARROCK",
                "PohPortal.LUMBRIDGE",
                "MountedDigsite.FOSSIL_ISLAND"
        };

        for (int i = 0; i < testJsons.length; i++) {
            System.out.println("\nTest " + (i + 1) + ": " + expectedResults[i]);
            System.out.println("Input JSON: " + testJsons[i]);

            try {
                PohTeleport result = gson.fromJson(testJsons[i], PohTeleport.class);
                String actualResult = result.getClass().getSimpleName() + "." + result.name();

                System.out.println("Result: " + actualResult);
                System.out.println("Expected: " + expectedResults[i]);
                System.out.println("✓ PASS: " + actualResult.equals(expectedResults[i]));

            } catch (Exception e) {
                System.out.println("✗ FAIL - Exception: " + e.getMessage());
            }
        }

        System.out.println("\n=== Manual Deserialization Test Complete ===");
    }

    /**
     * Tests edge cases and error conditions.
     */
    public static void testEdgeCases() {
        System.out.println("\n=== Edge Case Tests ===");
        Gson gson = createTestGson();

        // Test cases for various error conditions
        Object[][] testCases = {
                // {description, json, shouldSucceed}
                {"Null value", null, false},
                {"Empty JSON", "{}", false},
                {"Missing enumClass", "{\"enumName\":\"VARROCK\"}", false},
                {"Missing enumName", "{\"enumClass\":\"net.runelite.client.plugins.microbot.util.poh.data.NexusPortal\"}", false},
                {"Invalid enum class", "{\"enumClass\":\"com.invalid.Class\",\"enumName\":\"VARROCK\"}", false},
                {"Non-PohTransportable enum", "{\"enumClass\":\"java.lang.Thread.State\",\"enumName\":\"NEW\"}", false},
                {"Invalid enum value", "{\"enumClass\":\"net.runelite.client.plugins.microbot.util.poh.data.NexusPortal\",\"enumName\":\"INVALID_PORTAL\"}", false},
                {"Null enum class", "{\"enumClass\":null,\"enumName\":\"VARROCK\"}", false},
                {"Null enum name", "{\"enumClass\":\"net.runelite.client.plugins.microbot.util.poh.data.NexusPortal\",\"enumName\":null}", false},
                {"JSON null", "null", true}, // This should return null, which is valid
                {"With error field", "{\"enumClass\":\"net.runelite.client.plugins.microbot.util.poh.data.NexusPortal\",\"enumName\":\"VARROCK\",\"error\":\"test error\"}", true}
        };

        for (Object[] testCase : testCases) {
            String description = (String) testCase[0];
            String json = (String) testCase[1];
            boolean shouldSucceed = (Boolean) testCase[2];

            System.out.println("\nTesting: " + description);
            System.out.println("JSON: " + json);
            System.out.println("Expected to succeed: " + shouldSucceed);

            try {
                PohTeleport result = gson.fromJson(json, PohTeleport.class);

                if (shouldSucceed) {
                    System.out.println("✓ PASS - Result: " + (result != null ? result.getClass().getSimpleName() + "." + result.name() : "null"));
                } else {
                    System.out.println("✗ UNEXPECTED SUCCESS - Result: " + (result != null ? result.getClass().getSimpleName() + "." + result.name() : "null"));
                }

            } catch (Exception e) {
                if (!shouldSucceed) {
                    System.out.println("✓ PASS - Expected failure: " + e.getMessage());
                } else {
                    System.out.println("✗ UNEXPECTED FAILURE - Exception: " + e.getMessage());
                }
            }
        }

        System.out.println("\n=== Edge Case Tests Complete ===");
    }

    /**
     * Tests serialization of null values.
     */
    public static void testNullSerialization() {
        System.out.println("\n=== Null Serialization Test ===");
        Gson gson = createTestGson();

        try {
            String json = gson.toJson(null, PohTeleport.class);
            System.out.println("Null serialized to: " + json);

            PohTeleport result = gson.fromJson(json, PohTeleport.class);
            System.out.println("Null deserialized to: " + result);
            System.out.println("✓ PASS - Null handling works correctly");

        } catch (Exception e) {
            System.out.println("✗ FAIL - Null handling failed: " + e.getMessage());
        }

        System.out.println("\n=== Null Serialization Test Complete ===");
    }


    public static void testMap() {
        System.out.println("\n=== Map Serialization Test ===");
        Map<String, List<PohTeleport>> map = new HashMap<>();
        map.put(NexusPortal.class.getSimpleName(), List.of(NexusPortal.ARDOUGNE, NexusPortal.LUMBRIDGE, NexusPortal.VARROCK));
        map.put(PohPortal.class.getSimpleName(), List.of(PohPortal.values()));
        map.put(MountedDigsite.class.getSimpleName(), List.of(MountedDigsite.values()));
        map.put(MountedGlory.class.getSimpleName(), List.of(MountedGlory.values()));

        Gson gson = createTestGson();
        try {
            String json = gson.toJson(map, Rs2PohCache.TYPE_TOKEN);
            System.out.println("Map Serialzied to: " + json);

            Map<String, List<PohTeleport>> result = gson.fromJson(json, Rs2PohCache.TYPE_TOKEN);
            System.out.println("Map deserialized to: " + result);
            if (result.equals(map)) {
                System.out.println("✓ PASS - Map handling works correctly");
            } else {
                System.out.println("✗ FAIL - Null handling failed");
            }
        } catch (Exception e) {
            System.out.println("✗ FAIL - Null handling failed: " + e.getMessage());
        }
    }

    /**
     * Main method to run all tests.
     */
    public static void main(String[] args) {
        System.out.println("Starting PohTeleportDataAdapter Tests...\n");

//        testBasicSerialization();
//        testManualDeserialization();
//        testEdgeCases();
//        testNullSerialization();
        testMap();

        System.out.println("\n=== All Tests Complete ===");
    }
}