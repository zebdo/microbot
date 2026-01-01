package net.runelite.client.plugins.microbot.mouserecorder;

import lombok.Getter;

import java.util.List;

@Getter
public class MouseMacroRecording
{
	private final long startedAtEpochMs;
	private final List<MouseMacroEvent> events;

	public MouseMacroRecording(long startedAtEpochMs, List<MouseMacroEvent> events)
	{
		this.startedAtEpochMs = startedAtEpochMs;
		this.events = events;
	}
}
