package net.runelite.client.plugins.microbot.util.keyboard;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.Global;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Utility class for simulating keyboard input
 */
public final class Rs2Keyboard {
	private Rs2Keyboard() {}

	/**
	 * Get the game canvas
	 * @return
	 */
	private static Canvas canvas() {
		return Microbot.getClient().getCanvas();
	}

	/**
	 * Get the game frame
	 * @return
	 */
	private static Frame frame() {
		Window w = SwingUtilities.getWindowAncestor(canvas());
		if (w instanceof Frame) {
			return (Frame) w;
		}
		return null;
	}

	/**
	 * Check if the game is iconified (minimized)
	 * @return
	 */
	private static boolean isMinimized() {
		Frame f = frame();
		return f != null && (f.getExtendedState() & Frame.ICONIFIED) != 0;
	}

	/**
	 * Directly dispatch a KeyEvent to the canvas
	 * @param id
	 * @param keyCode
	 * @param keyChar
	 */
	private static void dispatchDirect(int id, int keyCode, char keyChar) {
		long now = System.currentTimeMillis();
		KeyEvent e = new KeyEvent(canvas(), id, now, 0, keyCode, keyChar);
		canvas().dispatchEvent(e);
	}

	/**
	 * Post a KeyEvent to the system event queue
	 * @param id
	 * @param keyCode
	 * @param keyChar
	 */
	private static void postNormal(int id, int keyCode, char keyChar) {
		long now = System.currentTimeMillis();
		KeyEvent e = new KeyEvent(canvas(), id, now, 0, keyCode, keyChar);
		if (SwingUtilities.isEventDispatchThread()) {
			canvas().dispatchEvent(e);
		} else {
			Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
		}
	}

	/**
	 * Send a key event, choosing the method based on whether the game is minimized
	 * @param id
	 * @param keyCode
	 * @param keyChar
	 */
	private static void sendKey(int id, int keyCode, char keyChar) {
		if (isMinimized()) {
			System.out.println("sending minimized keystrokes");
			dispatchDirect(id, keyCode, keyChar); // works while minimized
		} else {
			System.out.println("sending normalized keystrokes");
			postNormal(id, keyCode, keyChar);
		}
	}

	/**
	 * Hold down a key
	 * @param keyCode
	 */
	public static void keyHold(int keyCode) {
		sendKey(KeyEvent.KEY_PRESSED, keyCode, KeyEvent.CHAR_UNDEFINED);
	}

	/**
	 * Release a held key
	 * @param keyCode
	 */
	public static void keyRelease(int keyCode) {
		sendKey(KeyEvent.KEY_RELEASED, keyCode, KeyEvent.CHAR_UNDEFINED);
	}

	/**
	 * Press and release a key
	 * @param keyCode
	 */
	public static void keyPress(int keyCode) {
		sendKey(KeyEvent.KEY_PRESSED, keyCode, KeyEvent.CHAR_UNDEFINED);
		Global.sleep(10, 20);
		sendKey(KeyEvent.KEY_RELEASED, keyCode, KeyEvent.CHAR_UNDEFINED);
	}

	/**
	 * Type a character (for text input)
	 * @param ch
	 */
	public static void typeChar(char ch) {
		sendKey(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, ch);
	}

	/**
	 * Type a string (for text input)
	 * @param s
	 */
	public static void typeString(String s) {
		for (int i = 0; i < s.length(); i++) {
			typeChar(s.charAt(i));
			Global.sleep(30, 80);
		}
	}

	/**
	 * Press the Enter key
	 */
	public static void enter() {
		sendKey(KeyEvent.KEY_PRESSED, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
		Global.sleep(10, 20);
		sendKey(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '\n');
		Global.sleep(5, 10);
		sendKey(KeyEvent.KEY_RELEASED, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
	}
}
