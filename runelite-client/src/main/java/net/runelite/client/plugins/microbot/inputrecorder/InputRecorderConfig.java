package net.runelite.client.plugins.microbot.inputrecorder;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("inputrecorder")
public interface InputRecorderConfig extends Config {

    @ConfigSection(
            name = "Recording Settings",
            description = "Configure what input to record",
            position = 0
    )
    String recordingSection = "recording";

    @ConfigSection(
            name = "Storage Settings",
            description = "Configure where recordings are saved",
            position = 1
    )
    String storageSection = "storage";

    @ConfigSection(
            name = "Replay Settings",
            description = "Configure replay behavior",
            position = 2
    )
    String replaySection = "replay";

    // ===== Recording Settings =====

    @ConfigItem(
            keyName = "enableRecording",
            name = "Enable Recording",
            description = "Master switch to enable/disable input recording",
            section = recordingSection,
            position = 0
    )
    default boolean enableRecording() {
        return false;
    }

    @ConfigItem(
            keyName = "recordMouseMoves",
            name = "Record Mouse Movements",
            description = "Record mouse movement paths (compressed). Useful for ML analysis.",
            section = recordingSection,
            position = 1
    )
    default boolean recordMouseMoves() {
        return true;
    }

    @ConfigItem(
            keyName = "recordKeyboardInput",
            name = "Record Keyboard Input",
            description = "Record keyboard hotkeys and key presses",
            section = recordingSection,
            position = 2
    )
    default boolean recordKeyboardInput() {
        return true;
    }

    @ConfigItem(
            keyName = "recordChatInput",
            name = "Record Chat Input",
            description = "Record typed chat messages (privacy warning: includes text content)",
            section = recordingSection,
            position = 3
    )
    default boolean recordChatInput() {
        return false;
    }

    @ConfigItem(
            keyName = "profileName",
            name = "Profile Name",
            description = "Profile/category for organizing recordings (e.g., 'bossing', 'skilling')",
            section = recordingSection,
            position = 4
    )
    default String profileName() {
        return "default";
    }

    @ConfigItem(
            keyName = "autoStartRecording",
            name = "Auto-Start Recording",
            description = "Automatically start recording when logging in",
            section = recordingSection,
            position = 5
    )
    default boolean autoStartRecording() {
        return false;
    }

    @ConfigItem(
            keyName = "maxSessionDurationMinutes",
            name = "Max Session Duration (min)",
            description = "Automatically stop recording after this many minutes (0 = unlimited)",
            section = recordingSection,
            position = 6
    )
    default int maxSessionDurationMinutes() {
        return 60;
    }

    @ConfigItem(
            keyName = "mouseMoveThrottleMs",
            name = "Mouse Move Throttle (ms)",
            description = "Minimum time between recorded mouse movements (lower = more data)",
            section = recordingSection,
            position = 7
    )
    default int mouseMoveThrottleMs() {
        return 50;
    }

    @ConfigItem(
            keyName = "mouseMoveDistanceThreshold",
            name = "Mouse Move Distance Threshold",
            description = "Minimum pixels moved before recording a mouse movement",
            section = recordingSection,
            position = 8
    )
    default int mouseMoveDistanceThreshold() {
        return 10;
    }

    // ===== Storage Settings =====

    @ConfigItem(
            keyName = "storageDirectory",
            name = "Storage Directory",
            description = "Custom directory for saving recordings (leave empty for default)",
            section = storageSection,
            position = 0
    )
    default String storageDirectory() {
        return "";
    }

    @ConfigItem(
            keyName = "autoSaveOnStop",
            name = "Auto-Save on Stop",
            description = "Automatically save session when stopping recording",
            section = storageSection,
            position = 1
    )
    default boolean autoSaveOnStop() {
        return true;
    }

    @ConfigItem(
            keyName = "maxStoredSessions",
            name = "Max Stored Sessions",
            description = "Maximum number of sessions to keep (0 = unlimited, oldest deleted first)",
            section = storageSection,
            position = 2
    )
    default int maxStoredSessions() {
        return 100;
    }

    @ConfigItem(
            keyName = "includeAccountName",
            name = "Include Account Name",
            description = "Include your account/character name in saved sessions (privacy setting)",
            section = storageSection,
            position = 3
    )
    default boolean includeAccountName() {
        return false;
    }

    // ===== Replay Settings =====

    @ConfigItem(
            keyName = "replaySpeedMultiplier",
            name = "Replay Speed Multiplier",
            description = "Speed multiplier for replay (1.0 = real-time, 2.0 = 2x speed)",
            section = replaySection,
            position = 0
    )
    default double replaySpeedMultiplier() {
        return 1.0;
    }

    @ConfigItem(
            keyName = "replayRespectTiming",
            name = "Respect Original Timing",
            description = "Replay actions with original delays between them",
            section = replaySection,
            position = 1
    )
    default boolean replayRespectTiming() {
        return true;
    }

    @ConfigItem(
            keyName = "replayRandomizeDelays",
            name = "Randomize Delays",
            description = "Add slight random variation to delays (more human-like)",
            section = replaySection,
            position = 2
    )
    default boolean replayRandomizeDelays() {
        return true;
    }

    @ConfigItem(
            keyName = "replaySkipMouseMoves",
            name = "Skip Mouse Movements",
            description = "Skip mouse movement actions during replay (only execute clicks)",
            section = replaySection,
            position = 3
    )
    default boolean replaySkipMouseMoves() {
        return false;
    }

    @ConfigItem(
            keyName = "replayVerboseLogging",
            name = "Verbose Replay Logging",
            description = "Log detailed replay progress to console",
            section = replaySection,
            position = 4
    )
    default boolean replayVerboseLogging() {
        return false;
    }
}
