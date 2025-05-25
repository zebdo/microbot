package net.runelite.client.plugins.microbot.maxxin.astralrc;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;

@ConfigGroup(AstralRunesConfig.configGroup)
@ConfigInformation(
        "Plugin to craft Astral runes. <br/>" +
        "<b>REQUIRED ITEMS:</b><br/>" +
        "-Rune pouch w/ Law, Astral, and Cosmic runes in the inventory <br/>" +
        "-Dust staff equipped <br/>" +
        "-Pure essence <br/>" +
        "-Stamina potions <br/>" +
        "-Lobsters in bank <br/><br/>" +
        "Make sure spell book is set to Lunar. <br/><br/>" +
        "Only supports Colossal pouch and will use NPC contact for repairs.<br/><br/>" +
        "Runecraft cape should be supported. Only Law and Astrals are required in Rune pouch if using RC cape (or no Colossal pouch). <br/><br/>" +
        "The only food supported is Lobster, required to survive hits from mobs when banking. <br/><br/>" +
        "<b>NB</b> currently only works if Dream Mentor quest is completed"
)
public interface AstralRunesConfig extends Config {
    String configGroup = "astral-runes";
}
