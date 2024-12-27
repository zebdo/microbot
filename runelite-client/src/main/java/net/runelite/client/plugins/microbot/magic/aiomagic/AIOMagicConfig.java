package net.runelite.client.plugins.microbot.magic.aiomagic;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.plugins.microbot.magic.aiomagic.enums.MagicActivity;
import net.runelite.client.plugins.microbot.magic.aiomagic.enums.SuperHeatItem;
import net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells;

@ConfigGroup(AIOMagicConfig.configGroup)
public interface AIOMagicConfig extends Config {
	String configGroup = "aio-magic";
	String activity = "magicActivity";
	String combatSpell = "magicCombatSpell";
	String alchItems = "alchItems";
	String superHeatItem = "superHeatItem";
	String npcName = "npcName";

	@ConfigSection(
			name = "General Settings",
			description = "Configure general plugin configuration & preferences",
			position = 0
	)
	String generalSection = "general";

	@ConfigSection(
			name = "Splashing Settings",
			description = "Configure splashing settings",
			position = 1
	)
	String splashSection = "splash";

	@ConfigSection(
			name = "Alch Settings",
			description = "Configure Alching settings",
			position = 2
	)
	String alchSection = "alch";

	@ConfigSection(
			name = "SuperHeat Settings",
			description = "Configure SuperHeat settings",
			position = 2
	)
	String superHeatSection = "superHeat";

	@ConfigItem(
			keyName = activity,
			name = "Activity",
			description = "Select the activity you would like to perform",
			position = 0,
			section = generalSection
	)
	default MagicActivity magicActivity() {
		return MagicActivity.SPLASHING;
	}

	@ConfigItem(
			keyName = npcName,
			name = "NPC Name",
			description = "Name of the NPC you would like to splash",
			position = 0,
			section = splashSection
	)
	default String npcName() {
		return "";
	}

	@ConfigItem(
			keyName = combatSpell,
			name = "Combat Spell",
			description = "Select the spell you would like to splash with",
			position = 1,
			section = splashSection
	)
	default Rs2CombatSpells combatSpell() {
		return Rs2CombatSpells.WIND_STRIKE;
	}

	@ConfigItem(
			keyName = alchItems,
			name = "Alch Items",
			description = "List of items you would like to alch",
			position = 0,
			section = alchSection
	)
	default String alchItems() {
		return "";
	}

	@ConfigItem(
			keyName = superHeatItem,
			name = "SuperHeat Items",
			description = "List of items you would like to superheat",
			position = 0,
			section = superHeatSection
	)
	default SuperHeatItem superHeatItem() {
		return SuperHeatItem.IRON;
	}
}
