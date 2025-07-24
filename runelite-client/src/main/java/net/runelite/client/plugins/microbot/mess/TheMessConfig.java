package net.runelite.client.plugins.microbot.mess;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.plugins.microbot.mess.TheMessScript.Dish;

@ConfigGroup("the_mess")
public interface TheMessConfig extends Config {
    @ConfigSection(
            name = "",
            description = "General information about the activity and requirements.",
            position = 0
    )
    String aboutSection = "about";

    @ConfigSection(
            name = "Settings",
            description = "Settings for the script itself.",
            position = 1
    )
    String settingsSection = "settings";

    @ConfigItem(
            keyName = "info_box",
            name = "-- ABOUT THE MESS PLUGIN --",
            description = "Some information about the usage and requirements for the script",
            position = 0,
            section = aboutSection
    )
    default String info_box() {
        return "• Wiki:\n" +
                "https://oldschool.runescape.wiki/w/Mess \n" +
                "• It's basically Overcooked on OSRS\n" +
                "• Perfect for ironman to bump cook level as" +
                "there are no items needed to get good cooking xp/h.\n" +
                "• There are no rewards other than XP in this.\n\n" +
                "FLOW OF THE PLUGIN:\n\n" +
                "• If are not in the zone, it gonna travel to the Mess Hall.\n" +
                "• When reaching there, if inv not empty, it gonna bank it nearby.\n" +
                "• If everything is in order it will loop into making dishes" +
                "and turning them in.\n" +
                "• Whenever it sees the appreciation for the selected dish" +
                "below the set threshold, it will hop worlds till it's over." +
                "(beware that threshold over 33 may cause hop loop.)\n";
    }

    @ConfigItem(
            keyName = "requirements",
            name = "-- REQUIREMENTS --",
            description = "Requirements for the selected dish",
            position = 0,
            section = aboutSection
    )
    default String requirements() {
        return "• Servery Meat Pie (20 Cooking)\n" +
                "• Servery Stew (25 Cooking)\n" +
                "• Servery Pineapple Pizza (65 Cooking)\n";
    }

    @ConfigItem(
            keyName = "dish",
            name = "Dish",
            description = "Select the dish to cook, make sure to\n" +
                    "to have the required cooking level shown above.",
            position = 0,
            section = settingsSection
    )
    default Dish dish() {
        return Dish.STEW;
    }

    @ConfigItem(
            keyName = "appreciation_threshold",
            name = "Minimum Appreciation",
            description = "Percentage of appreciation required to continue serving.\n" +
                    "If appreciation is below this value, the bot will hop worlds.",
            position = 1,
            section = settingsSection
    )
    default int appreciation_threshold() {
        return 29;
    }

    @ConfigItem(
            keyName = "debug_mode",
            name = "Debug Mode",
            description = "Enables debug mode for the script.\n" +
                    "Useful for troubleshooting and development.",
            position = 2,
            section = settingsSection
    )
    default boolean debug_mode() {
        return false;
    }
}
