package net.runelite.client.plugins.microbot.util.poh;

import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.util.poh.data.*;

import java.io.File;
import java.net.URL;

public class PohTsvGenerator {
    private static void generatePohTsvContent() {
        //Teleports
        StringBuilder teleportBuilder = new StringBuilder("Origin\tDestination\tisMembers\tEnumValue\tEnumClass\tDisplay info\tWilderness level\tDuration\n");

        for (MountedDigsite value : MountedDigsite.values()) {
            teleportBuilder.append(value.getTsvValue());
        }
        for (MountedGlory value : MountedGlory.values()) {
            teleportBuilder.append(value.getTsvValue());
        }
        for (PohPortal value : PohPortal.values()) {
            teleportBuilder.append(value.getTsvValue());
        }
        for (MountedXerics value : MountedXerics.values()) {
            teleportBuilder.append(value.getTsvValue());
        }
        for (MountedMythical value : MountedMythical.values()) {
            teleportBuilder.append(value.getTsvValue());
        }
        for (JewelleryBox value : JewelleryBox.values()) {
            teleportBuilder.append(value.getTsvValue());
        }
        for (NexusPortal value : NexusPortal.values()) {
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
            String resourceDir = "src" + File.separator + "main" + File.separator + "resources";
            URL targetClassUrl = ShortestPathPlugin.class.getResource("");
            if (targetClassUrl == null) {
                throw new IllegalStateException("Could not find ShortestPathPlugin target/classes resource directory");
            }
            String resourcePath = targetClassUrl.getPath().replace("target" + File.separator + "classes", resourceDir);
            String filePath = resourcePath + File.separator + fileName;
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            java.nio.file.Files.write(path, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("Successfully wrote to file=" + filePath);

        } catch (Exception e) {
            System.err.println("Failed to write to file=" + fileName + " " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        generatePohTsvContent();
    }
}
