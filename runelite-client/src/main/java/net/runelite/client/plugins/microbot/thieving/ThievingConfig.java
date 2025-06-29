package net.runelite.client.plugins.microbot.thieving;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.plugins.microbot.thieving.enums.ThievingNpc;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;

@ConfigGroup("Thieving")
public interface ThievingConfig extends Config {

    // ===== GENERAL SECTION =====
    @ConfigSection(
            name = "General",
            description = "General settings for the Thieving script.",
            position = 0
    )
    String generalSection = "General";

    @ConfigItem(
            keyName = "Npc",
            name = "NPC to Thieve",
            description = "Choose the NPC to start thieving from.",
            position = 0,
            section = generalSection
    )
    default ThievingNpc THIEVING_NPC() {
        return ThievingNpc.NONE;
    }

    @ConfigItem(
            keyName = "ardougneAreaCheck",
            name = "Ardy Knights Bank Area Check",
            description = "Require Ardougne Knight to be in the Ardougne bank area.",
            position = 1,
            section = generalSection
    )
    default boolean ardougneAreaCheck() {
        return false;
    }

    // ===== BUFFS SECTION =====
    @ConfigSection(
            name = "Buffs",
            description = "Buffs and spell-casting options.",
            position = 1
    )
    String buffsSection = "Buffs";

    @ConfigItem(
            keyName = "shadowVeil",
            name = "Shadow Veil",
            description = "Enable automatic casting of Shadow Veil.",
            position = 0,
            section = buffsSection
    )
    default boolean shadowVeil() {
        return false;
    }

    // ===== FOOD SECTION =====
    @ConfigSection(
            name = "Food",
            description = "Food and eating settings.",
            position = 2
    )
    String foodSection = "Food";

    @ConfigItem(
            keyName = "UseFood",
            name = "Auto Eat Food",
            description = "Automatically eat food if HP is low.",
            position = 0,
            section = foodSection
    )
    default boolean useFood() {
        return true;
    }

    @ConfigItem(
            keyName = "Hitpoints",
            name = "Eat Below HP %",
            description = "Eat food when HP falls below this percent.",
            position = 1,
            section = foodSection
    )
    default int hitpoints() {
        return 20;
    }

    @ConfigItem(
            keyName = "Food",
            name = "Food Type",
            description = "Type of food to use.",
            position = 2,
            section = foodSection
    )
    default Rs2Food food() {
        return Rs2Food.MONKFISH;
    }

    @ConfigItem(
            keyName = "FoodAmount",
            name = "Food Amount",
            description = "Amount of food to withdraw from bank.",
            position = 3,
            section = foodSection
    )
    default int foodAmount() {
        return 5;
    }

    // ===== COIN POUCH & ITEMS SECTION =====
    @ConfigSection(
            name = "Coin Pouch & Items",
            description = "Settings for coin pouch handling and inventory management.",
            position = 3
    )
    String coinPouchSection = "Coin Pouch & Items";

    @ConfigItem(
            keyName = "CoinPouchTreshHold",
            name = "Coin Pouch Threshold",
            description = "How many coin pouches to keep before opening them.",
            position = 0,
            section = coinPouchSection
    )
    @Range(min = 1, max = 28)
    default int coinPouchTreshHold() {
        return 28;
    }

    @ConfigItem(
            keyName = "KeepItemsAboveValue",
            name = "Keep Items Above Value",
            description = "Keep items in inventory worth more than this GP value.",
            position = 1,
            section = coinPouchSection
    )
    default int keepItemsAboveValue() {
        return 10000;
    }

    @ConfigItem(
            keyName = "DodgyNecklaceAmount",
            name = "Dodgy Necklace Amount",
            description = "Amount of Dodgy Necklaces to withdraw from bank.",
            position = 2,
            section = coinPouchSection
    )
    default int dodgyNecklaceAmount() {
        return 5;
    }

    @ConfigItem(
            keyName = "DoNotDropItemList",
            name = "Do Not Drop Item List",
            description = "Comma-separated list of items never to drop from inventory.",
            position = 3,
            section = coinPouchSection
    )
    default String DoNotDropItemList() {
        return "";
    }
}