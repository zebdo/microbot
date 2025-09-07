package net.runelite.client.plugins.microbot.util.poh;

import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.util.poh.data.*;

public class PohTsvGenerator {
    private static void generatePohTsvContent() {
        //Teleports
        StringBuilder teleportBuilder = new StringBuilder("Origin\tDestination\tisMembers\tEnumValue\tEnumClass\tDisplay info\tWilderness level\tDuration\n");
        for (NexusPortal value : NexusPortal.values()) {
            teleportBuilder.append(value.getTsvValue());
        }
        for (MountedDigsite value : MountedDigsite.values()) {
            teleportBuilder.append(value.getTsvValue());
        }
        for (MountedGlory value : MountedGlory.values()) {
            teleportBuilder.append(value.getTsvValue());
        }
        for (JewelleryBox value : JewelleryBox.values()) {
            teleportBuilder.append(value.getTsvValue());
        }
        for (PohPortal value : PohPortal.values()) {
            teleportBuilder.append(value.getTsvValue());
        }
        writePohToTsv(teleportBuilder.toString(), "teleportation_poh.tsv");

        //Outside portals
        writePohToTsv(HouseLocation.buildPortalMappingTSV(), "teleportation_portal_poh.tsv");
    }

    /**
     * Writes the Poh Transport TSV data to the ShortestPathPlugin resources directory.
     * This method should be called during development to generate the file.
     */
    public static void writePohToTsv(String content, String fileName) {
        try {
            // Use the same resource loading mechanism to find our resource directory
            java.net.URL resourceUrl = ShortestPathPlugin.class.getResource(fileName);

            if (resourceUrl != null) {
                // Convert URL to file path
                String resourcePath = resourceUrl.getPath();
                String directoryPath = resourcePath.substring(0, resourcePath.lastIndexOf('/'));

                // Handle Windows paths
                if (directoryPath.startsWith("/") && System.getProperty("os.name").toLowerCase().contains("windows")) {
                    directoryPath = directoryPath.substring(1);
                }

                String filePath = directoryPath + "/" + fileName;
                java.nio.file.Path path = java.nio.file.Paths.get(filePath);
                java.nio.file.Files.write(path, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                System.out.println("Successfully wrote to file=" + filePath);
            } else {
                System.err.println("Could not find ShortestPathPlugin resource directory");
            }
        } catch (Exception e) {
            System.err.println("Failed to write to file=" + fileName + " " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        generatePohTsvContent();
    }
}
