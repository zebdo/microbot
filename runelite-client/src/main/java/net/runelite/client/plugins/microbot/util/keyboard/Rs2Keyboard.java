package net.runelite.client.plugins.microbot.util.keyboard;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.Global;

import java.awt.*;
import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.CHAR_UNDEFINED;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;

public class Rs2Keyboard
{

	private static Canvas getCanvas()
	{
		return Microbot.getClient().getCanvas();
	}

	private static void withFocusCanvas(Runnable action)
	{
		Canvas canvas = getCanvas();
		boolean originalFocus = canvas.isFocusable();
		if (!originalFocus) canvas.setFocusable(true);

		try
		{
			action.run();
		}
		finally
		{
			if (!originalFocus) canvas.setFocusable(false);
		}
	}

	private static void dispatchKeyEvent(int id, int keyCode, char keyChar, int delay)
	{
		KeyEvent event = new KeyEvent(
			getCanvas(),
			id,
			System.currentTimeMillis() + delay,
			0,
			keyCode,
			keyChar
		);
		getCanvas().dispatchEvent(event);
	}

	public static void typeString(final String word)
	{
		withFocusCanvas(() -> {
			for (char c : word.toCharArray())
			{
				int delay = Rs2Random.between(20, 200);
				dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, c, delay);
				Global.sleep(100, 200);
			}
		});
	}

	public static void keyPress(final char key)
	{
		withFocusCanvas(() -> {
			int delay = Rs2Random.between(20, 200);
			dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, key, delay);
		});
	}

	public static void holdShift()
	{
		withFocusCanvas(() -> {
			int delay = Rs2Random.between(20, 200);
			dispatchKeyEvent(KeyEvent.KEY_PRESSED, KeyEvent.VK_SHIFT, CHAR_UNDEFINED, delay);
		});
	}

	public static void releaseShift()
	{
		withFocusCanvas(() -> {
			int delay = Rs2Random.between(20, 200);
			dispatchKeyEvent(KeyEvent.KEY_RELEASED, KeyEvent.VK_SHIFT, CHAR_UNDEFINED, delay);
		});
	}

	public static void keyHold(int key)
	{
		withFocusCanvas(() ->
			dispatchKeyEvent(KeyEvent.KEY_PRESSED, key, CHAR_UNDEFINED, 0)
		);
	}

	public static void keyRelease(int key)
	{
		withFocusCanvas(() -> {
			int delay = Rs2Random.between(20, 200);
			dispatchKeyEvent(KeyEvent.KEY_RELEASED, key, CHAR_UNDEFINED, delay);
		});
	}

	public static void keyPress(int key)
	{
		keyHold(key);
		keyRelease(key);
	}

	public static void enter()
	{
		// this is to avoid automatically login with jagex account when you are on the login screen
		if (!Microbot.isLoggedIn()) {
			dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '\n', 0);
			return;
		}
		
		keyPress(KeyEvent.VK_ENTER);
	}
}