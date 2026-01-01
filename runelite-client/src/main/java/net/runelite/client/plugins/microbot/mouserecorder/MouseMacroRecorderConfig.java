package net.runelite.client.plugins.microbot.mouserecorder;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(MouseMacroRecorderConfig.CONFIG_GROUP)
public interface MouseMacroRecorderConfig extends Config
{
	String CONFIG_GROUP = "mouseMacroRecorder";

	@ConfigSection(
		name = "Recording",
		description = "Configure how movement samples are recorded",
		position = 0
	)
	String recordingSection = "recording";

	@ConfigItem(
		keyName = "recordMouseMovement",
		name = "Record mouse movement",
		description = "Capture mouse move/drag events",
		position = 0,
		section = recordingSection
	)
	default boolean recordMouseMovement()
	{
		return true;
	}

	@ConfigItem(
		keyName = "movementSampleIntervalMs",
		name = "Movement sample interval (ms)",
		description = "Minimum time between recorded mouse movement samples",
		position = 1,
		section = recordingSection
	)
	@Range(min = 5, max = 500)
	default int movementSampleIntervalMs()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "movementMinDistance",
		name = "Movement min distance",
		description = "Minimum pixel distance before recording a move",
		position = 2,
		section = recordingSection
	)
	@Range(min = 0, max = 50)
	default int movementMinDistance()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "maxRecordedEvents",
		name = "Max recorded events",
		description = "Stop recording after reaching this many events",
		position = 3,
		section = recordingSection
	)
	@Range(min = 100, max = 50000)
	default int maxRecordedEvents()
	{
		return 10000;
	}
}
