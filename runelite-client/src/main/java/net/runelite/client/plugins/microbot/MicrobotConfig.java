package net.runelite.client.plugins.microbot;

import ch.qos.logback.classic.Level;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigButton;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.plugins.microbot.util.prayer.PrayerHotkeyAssignments;
import net.runelite.client.plugins.microbot.util.prayer.PrayerHotkeyConfigAccess;
import net.runelite.client.plugins.microbot.util.prayer.PrayerHotkeyOption;

@ConfigGroup(MicrobotConfig.configGroup)
public interface MicrobotConfig extends Config
{
	String configGroup = "microbot";

	@ConfigSection(
		name = "General",
		description = "The overall global settings for microbot",
		position = 0
	)
	String generalSection = "generalSection";

	String keyDisableLevelUpInterface = "disableLevelUpInterface";
	@ConfigItem(
		keyName = keyDisableLevelUpInterface,
		name = "Disable level-up interface",
		description = "Automatically close the level-up interface when it appears",
		position = 0,
		section = generalSection
	)
	default boolean disableLevelUpInterface()
	{
		return true;
	}

	String keyDisableWorldSwitcherConfirmation = "disableWorldSwitcherConfirmation";
	@ConfigItem(
		keyName = keyDisableWorldSwitcherConfirmation,
		name = "Disable world switcher confirmation",
		description = "Automatically disable the world switcher confirmation prompt",
		position = 1,
		section = generalSection
	)
	default boolean disableWorldSwitcherConfirmation()
	{
		return true;
	}

        @ConfigSection(
                name = "Logging",
                description = "Game chat logging configuration",
                position = 1
        )
        String loggingSection = "loggingSection";
        @ConfigSection(
                        name = "Caching",
                        description = "Caching ingame data",
                        position = 2
        )
        String cacheSection = "cacheSection";
        @ConfigSection(
                name = "Prayer Hotkeys",
                description = "Configure the five Microbot prayer hotkey slots",
                position = 3
        )
        String prayerHotkeysSection = "prayerHotkeysSection";

        String keyPrayerHotkeyConfigure = "configurePrayerHotkeys";

        @ConfigItem(
                keyName = keyPrayerHotkeyConfigure,
                name = "Configure hotkeys",
                description = "Open the in-game selector to assign prayers to hotkey slots.",
                position = 0,
                section = prayerHotkeysSection
        )
        default ConfigButton openPrayerHotkeySelector()
        {
                return new ConfigButton("Open selector", PrayerHotkeyConfigAccess::openSelector);
        }

        String keyPrayerHotkeySlot1 = PrayerHotkeyAssignments.SLOT_KEY_PREFIX + "1";
        String keyPrayerHotkeySlot2 = PrayerHotkeyAssignments.SLOT_KEY_PREFIX + "2";
        String keyPrayerHotkeySlot3 = PrayerHotkeyAssignments.SLOT_KEY_PREFIX + "3";
        String keyPrayerHotkeySlot4 = PrayerHotkeyAssignments.SLOT_KEY_PREFIX + "4";
        String keyPrayerHotkeySlot5 = PrayerHotkeyAssignments.SLOT_KEY_PREFIX + "5";

        @ConfigItem(
                keyName = keyPrayerHotkeySlot1,
                name = "Hotkey 1",
                description = "Prayer triggered when hotkey slot 1 is clicked.",
                position = 1,
                section = prayerHotkeysSection
        )
        default PrayerHotkeyOption prayerHotkeySlot1()
        {
                return PrayerHotkeyOption.NONE;
        }

        @ConfigItem(
                keyName = keyPrayerHotkeySlot2,
                name = "Hotkey 2",
                description = "Prayer triggered when hotkey slot 2 is clicked.",
                position = 2,
                section = prayerHotkeysSection
        )
        default PrayerHotkeyOption prayerHotkeySlot2()
        {
                return PrayerHotkeyOption.NONE;
        }

        @ConfigItem(
                keyName = keyPrayerHotkeySlot3,
                name = "Hotkey 3",
                description = "Prayer triggered when hotkey slot 3 is clicked.",
                position = 3,
                section = prayerHotkeysSection
        )
        default PrayerHotkeyOption prayerHotkeySlot3()
        {
                return PrayerHotkeyOption.NONE;
        }

        @ConfigItem(
                keyName = keyPrayerHotkeySlot4,
                name = "Hotkey 4",
                description = "Prayer triggered when hotkey slot 4 is clicked.",
                position = 4,
                section = prayerHotkeysSection
        )
        default PrayerHotkeyOption prayerHotkeySlot4()
        {
                return PrayerHotkeyOption.NONE;
        }

        @ConfigItem(
                keyName = keyPrayerHotkeySlot5,
                name = "Hotkey 5",
                description = "Prayer triggered when hotkey slot 5 is clicked.",
                position = 5,
                section = prayerHotkeysSection
        )
        default PrayerHotkeyOption prayerHotkeySlot5()
        {
                return PrayerHotkeyOption.NONE;
        }

	String keyEnableGameChatLogging = "enableGameChatLogging";
	@ConfigItem(
		keyName = keyEnableGameChatLogging,
		name = "Enable Game Chat Logging",
		description = "Enable or disable logging to game chat",
		position = 0,
		section = loggingSection
	)
	default boolean enableGameChatLogging() {
		return true;
	}

	String keyGameChatLogLevel = "gameChatLogLevel";
	@ConfigItem(
		keyName = keyGameChatLogLevel,
		name = "Log Level",
		description = "Minimum log level to show in game chat:<br>" +
				"• ERROR: Only error messages<br>" +
				"• WARN: Warning and error messages<br>" +
				"• INFO: Info, warning, and error messages<br>" +
				"• DEBUG: All messages (debug mode only)",
		position = 1,
		section = loggingSection
	)
	default GameChatLogLevel getGameChatLogLevel() {
		return GameChatLogLevel.WARN;
	}

	String keyGameChatLogPattern = "gameChatLogPattern";
	@ConfigItem(
		keyName = keyGameChatLogPattern,
		name = "Log Pattern",
		description = "Format of log messages in game chat",
		position = 2,
		section = loggingSection
	)
	default GameChatLogPattern getGameChatLogPattern() {
		return GameChatLogPattern.SIMPLE;
	}

	String keyOnlyMicrobotLogging = "onlyMicrobotLogging";
	@ConfigItem(
		keyName = keyOnlyMicrobotLogging,
		name = "Only Microbot Logs",
		description = "Show only Microbot plugin logs in game chat (filters out other RuneLite logs)",
		position = 3,
		section = loggingSection
	)
	default boolean onlyMicrobotLogging() {
		return false;
	}

        String keyEnableMenuEntryLogging = "enableMenuEntryLogging";

        @ConfigItem(
                        keyName = keyEnableMenuEntryLogging,
                        name = "Enable Menu Entry Logging",
			description = "Enable or disable logging menu entry clicked",
			position = 4,
			section = loggingSection
	)
        default boolean enableMenuEntryLogging() {
                return false;
        }

        String keyEnableCache = "enableRs2Cache";
        @ConfigItem(
                        keyName = keyEnableCache,
                        name = "Enable Microbot Cache",
			description = "This will cache ingame entities (npcs, objects,...) to improve performance",
			position = 0,
			section = cacheSection
	)
	default boolean isRs2CacheEnabled() {
		return false;
	}

	@AllArgsConstructor
	enum GameChatLogLevel {
		ERROR("Error", Level.ERROR),
		WARN("Warning", Level.WARN),
		INFO("Info", Level.INFO),
		DEBUG("Debug", Level.DEBUG);

		private final String displayName;
		@Getter
		private final Level level;

		@Override
		public String toString() {
			return displayName;
		}
	}

	enum GameChatLogPattern {
		SIMPLE("Simple", "[%d{HH:mm:ss}] %msg%ex{0}%n"),
		DETAILED("Detailed", "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%ex{0}%n");

		private final String displayName;
		@Getter
        private final String pattern;

		GameChatLogPattern(String displayName, String pattern) {
			this.displayName = displayName;
			this.pattern = pattern;
		}

		@Override
		public String toString() {
			return displayName;
		}
    }

	@ConfigItem(
		keyName = "showCacheInfo",
		name = "Show Cache Information",
		description = "Display cache statistics and management button in overlays",
		position = 2,
		section = generalSection
	)
	default boolean showCacheInfo() {
		return false;
	}


}
