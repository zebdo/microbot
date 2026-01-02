package net.runelite.client.plugins.microbot.mouserecorder;

import lombok.Getter;

@Getter
public class MouseMacroEvent
{
	private final MouseMacroEventType type;
	private final long offsetMs;
	private final int x;
	private final int y;
	private final MenuEntrySnapshot menuEntry;

	public MouseMacroEvent(MouseMacroEventType type, long offsetMs, int x, int y, MenuEntrySnapshot menuEntry)
	{
		this.type = type;
		this.offsetMs = offsetMs;
		this.x = x;
		this.y = y;
		this.menuEntry = menuEntry;
	}
}
