package net.runelite.client.plugins.microbot.driftnet;

import net.runelite.client.config.*;

import java.awt.*;
@ConfigGroup(DriftNetPlugin.CONFIG_GROUP)
@ConfigInformation("Start this script at the driftet fishing area. <br /> Make sure to have driftnet in your inventory or driftnet stored with Anette.")
public interface DriftNetConfig extends Config {

        // ------------------------------
        // Sections
        // ------------------------------

        @ConfigSection(
                name = "General Settings",
                description = "Basic settings and usage instructions",
                position = 0,
                closedByDefault = false
        )
        String generalSettings = "generalSettings";

        @ConfigSection(
                name = "Banking Settings",
                description = "Options related to banking fish",
                position = 1,
                closedByDefault = false
        )
        String bankingSettings = "bankingSettings";

        @ConfigSection(
                name = "Visual Settings",
                description = "Customization options for visual elements",
                position = 2,
                closedByDefault = true
        )
        String visualSettings = "visualSettings";

        // ------------------------------
        // General Settings
        // ------------------------------

        @ConfigItem(
                keyName = "guide",
                name = "How to use",
                description = "How to use this plugin",
                position = 0,
                section = generalSettings
        )
        default String GUIDE() {
                return "Start at the driftnet area\n" +
                        "MUST HAVE DRIFTNET IN INVENTORY OR STORED WITH THE NPC";
        }

        @ConfigItem(
                position = 1,
                keyName = "showNetStatus",
                name = "Show net status",
                description = "Show net status and fish count",
                section = generalSettings
        )
        default boolean showNetStatus() {
                return true;
        }

        // ------------------------------
        // Banking Settings
        // ------------------------------

        @ConfigItem(
                keyName = "bankFish",
                name = "Bank Fish?",
                description = "Will bank fish, but will result in less xp/hr",
                position = 0,
                section = bankingSettings
        )
        default boolean bankFish() {
                return false;
        }

        // ------------------------------
        // Visual Settings
        // ------------------------------

        @ConfigItem(
                position = 0,
                keyName = "countColor",
                name = "Fish count color",
                description = "Color of the fish count text",
                section = visualSettings
        )
        default Color countColor() {
                return Color.WHITE;
        }

        @ConfigItem(
                position = 1,
                keyName = "highlightUntaggedFish",
                name = "Highlight untagged fish",
                description = "Highlight the untagged fish",
                section = visualSettings
        )
        default boolean highlightUntaggedFish() {
                return true;
        }

        @Alpha
        @ConfigItem(
                keyName = "untaggedFishColor",
                name = "Untagged fish color",
                description = "Color of untagged fish",
                position = 2,
                section = visualSettings
        )
        default Color untaggedFishColor() {
                return Color.CYAN;
        }

        @ConfigItem(
                position = 3,
                keyName = "timeoutDelay",
                name = "Tagged timeout",
                description = "Time required for a tag to expire",
                section = visualSettings
        )
        @Range(
                min = 1,
                max = 100
        )
        @Units(Units.TICKS)
        default int timeoutDelay() {
                return 60;
        }

        @ConfigItem(
                keyName = "tagAnnette",
                name = "Tag Annette",
                description = "Tag Annette when no nets in inventory",
                position = 4,
                section = visualSettings
        )
        default boolean tagAnnetteWhenNoNets() {
                return true;
        }

        @Alpha
        @ConfigItem(
                keyName = "annetteTagColor",
                name = "Annette tag color",
                description = "Color of Annette tag",
                position = 5,
                section = visualSettings
        )
        default Color annetteTagColor() {
                return Color.RED;
        }
}