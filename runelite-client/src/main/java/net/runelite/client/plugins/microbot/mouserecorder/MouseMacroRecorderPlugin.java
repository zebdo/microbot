package net.runelite.client.plugins.microbot.mouserecorder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

@PluginDescriptor(
	name = "Mouse Macro Recorder",
	description = "Record mouse movements and clicked menu entries",
	tags = {"mouse", "macro", "recorder", "microbot"}
)
@Slf4j
public class MouseMacroRecorderPlugin extends Plugin implements MouseListener
{
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	@Inject
	private Client client;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private MouseMacroRecorderConfig config;

	@Inject
	private ConfigManager configManager;

	private final Object lock = new Object();
	private final List<MouseMacroEvent> events = new ArrayList<>();
	private long recordingStartMs;
	private long lastMoveTimeMs;
	private java.awt.Point lastMovePoint;
	private boolean recording;

	@Provides
	MouseMacroRecorderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MouseMacroRecorderConfig.class);
	}

	@Override
	protected void startUp()
	{
		mouseManager.registerMouseListener(this);
		if (config.recordingEnabled())
		{
			startRecording();
		}
	}

	@Override
	protected void shutDown()
	{
		mouseManager.unregisterMouseListener(this);
		recording = false;
	}

	public void startRecording()
	{
		synchronized (lock)
		{
			events.clear();
			recordingStartMs = System.currentTimeMillis();
			lastMoveTimeMs = 0;
			lastMovePoint = null;
			recording = true;
		}
	}

	public void stopRecording()
	{
		recording = false;
	}

	public void clearRecording()
	{
		synchronized (lock)
		{
			events.clear();
			recordingStartMs = System.currentTimeMillis();
		}
	}

	public String exportJson()
	{
		MouseMacroRecording snapshot;
		synchronized (lock)
		{
			snapshot = new MouseMacroRecording(recordingStartMs, new ArrayList<>(events));
		}
		return GSON.toJson(snapshot);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!MouseMacroRecorderConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		String key = event.getKey();
		if ("recordingEnabled".equals(key))
		{
			if (config.recordingEnabled())
			{
				startRecording();
			}
			else
			{
				stopRecording();
			}
		}
		else if ("clearRecording".equals(key) && config.clearRecording())
		{
			clearRecording();
			configManager.setConfiguration(MouseMacroRecorderConfig.CONFIG_GROUP, "clearRecording", false);
		}
		else if ("exportJson".equals(key) && config.exportJson())
		{
			copyToClipboard(exportJson());
			configManager.setConfiguration(MouseMacroRecorderConfig.CONFIG_GROUP, "exportJson", false);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!recording)
		{
			return;
		}

		MenuEntry menuEntry = event.getMenuEntry();
		Point mousePosition = client.getMouseCanvasPosition();
		if (!recordEvent())
		{
			return;
		}

		MenuEntrySnapshot snapshot = new MenuEntrySnapshot(menuEntry);
		recordEvent(MouseMacroEventType.CLICK, mousePosition.getX(), mousePosition.getY(), snapshot);
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent mouseEvent)
	{
		recordMouseMove(mouseEvent);
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent mouseEvent)
	{
		recordMouseMove(mouseEvent);
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	private void recordMouseMove(MouseEvent mouseEvent)
	{
		if (!recording || !config.recordMouseMovement())
		{
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastMoveTimeMs < config.movementSampleIntervalMs())
		{
			return;
		}

		int minDistance = config.movementMinDistance();
		java.awt.Point point = mouseEvent.getPoint();
		if (lastMovePoint != null && minDistance > 0)
		{
			int dx = point.x - lastMovePoint.x;
			int dy = point.y - lastMovePoint.y;
			if (dx * dx + dy * dy < minDistance * minDistance)
			{
				return;
			}
		}

		if (!recordEvent())
		{
			return;
		}

		recordEvent(MouseMacroEventType.MOVE, point.x, point.y, null);
		lastMoveTimeMs = now;
		lastMovePoint = point;
	}

	private boolean recordEvent()
	{
		if (!recording)
		{
			return false;
		}

		synchronized (lock)
		{
			if (events.size() >= config.maxRecordedEvents())
			{
				recording = false;
				log.info("Mouse macro recorder hit max events ({}), stopping.", config.maxRecordedEvents());
				return false;
			}
		}

		return true;
	}

	private void recordEvent(MouseMacroEventType type, int x, int y, MenuEntrySnapshot menuEntry)
	{
		long now = System.currentTimeMillis();
		MouseMacroEvent event = new MouseMacroEvent(type, now - recordingStartMs, x, y, menuEntry);
		synchronized (lock)
		{
			events.add(event);
		}
	}

	private void copyToClipboard(String export)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(export), null);
	}
}
