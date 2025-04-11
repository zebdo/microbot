package net.runelite.client.plugins.microbot.VoxPlugins.schedulable.example;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.plugins.microbot.woodcutting.enums.WoodcuttingResetOptions;
import net.runelite.client.plugins.microbot.woodcutting.enums.WoodcuttingTree;

@ConfigGroup("SchedulableExample")
public interface SchedulableExampleConfig extends Config {
        @ConfigSection(
            name = "Start Conditions",
            description = "Conditions for when the plugin is allowed to be start",
            position = 0
        )
        String StartCondition = "startconditions";
        
        
        @ConfigSection(
            name = "Stop Conditions",
            description = "Conditions for when the plugin should stop",
            position = 1
        )
        String stopSection = "stopConditions";

     
        @ConfigItem(
                keyName = "bankItems",
                name = "Bank Items on Stop",
                description = "Bank Items",
                position = 0,
                section = stopSection
        )
        default boolean bankItems() {
                return false;
        }
        @ConfigItem(
                keyName = "lootItems",
                name = "Stop On Items Looted",
                description = "comma seperated list,Item name supports regex patterns (.*bones.*)",
                position = 1,
                section = stopSection
        )
        default String lootItems() {
                return "Logs";
        }


        @ConfigItem(
                keyName = "itemsToLootLogical",
                name = "Or(False)/And(True)",
                description = "Logical operator for items to loot",
                position = 2,
                section = stopSection
        )
        default boolean itemsToLootLogical() {
                return false;
        }
        @ConfigItem(
                keyName = "minItems",
                name = "Minimum Items",
                description = "Minimum number of Items to loot before stopping, ",
                position = 3,
                section = stopSection
        )
        default int minItems() {
                return 5;
        }

        @ConfigItem(
                keyName = "maxItems",
                name = "Maximum Items",
                description = "Maximum number of Items to collect before stopping",
                position = 4,
                section = stopSection
        )
        default int maxItems() {
                return 10;
        }
        @ConfigItem(
                keyName = "includeNoted",
                name = "Include Noted",
                description = "Include noted items in loot tracking",
                position = 0,
                section = stopSection
        )
        default boolean includeNoted() {
                return false;
        }
        @ConfigItem(
                keyName = "allowNoneOwner",
                name = "Allow None Owner",
                description = "Allow None Owner for loot tracking, items not owned by the player e.g. items which are spawned",
                position = 0,
                section = StartCondition
        )
        default boolean allowNoneOwner() {
                return false;
        }
        @ConfigItem(
                keyName = "minRuntime",
                name = "Minimum Runtime (minutes)",
                description = "Minimum time to run before stopping",
                position = 5,
                section = stopSection
        )
        default int minRuntime() {
                return 1;
        }

        @ConfigItem(
                keyName = "maxRuntime",
                name = "Maximum Runtime (minutes)",
                description = "Maximum time to run before stopping",
                position = 6,
                section = stopSection
        )
        default int maxRuntime() {
                return 2;
        }

    
    
    
        @ConfigItem(
            keyName = "lastLocation",
            name = "Last Location",
            description = "Last tracked location",
            hidden = true
        )
        default WorldPoint lastLocation() {
                return null;
        }       
        void setLastLocation(WorldPoint location);
}