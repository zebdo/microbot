package net.runelite.client.plugins.microbot.util.keyboard;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.Global;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Utility class for simulating keyboard input.
 */
public final class Rs2Keyboard {
	/**
	 * Private constructor to prevent instantiation.
	 */
	private Rs2Keyboard() {}

	/**
	 * Get the game canvas.
	 * @return
	 */
	private static Canvas canvas() { return Microbot.getClient().getCanvas(); }

	/**
	 * Run an action with the canvas focused, restoring previous focus state afterwards.
	 * @param action
	 */
	private static void withFocusedCanvas(Runnable action) {
		Canvas c = canvas();
		boolean wasFocusable = c.isFocusable();
		boolean hadFocus = c.isFocusOwner();
		if (!wasFocusable) c.setFocusable(true);
		if (!hadFocus)     c.requestFocusInWindow();
		try { action.run(); }
		finally {
			if (!wasFocusable) c.setFocusable(false);
		}
	}

	/**
	 * Post an event to the AWT event queue.
	 * @param e
	 */
	private static void post(KeyEvent e) {
		// Safe from any thread; posts into AWT event queue
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
	}

	/**
	 * Fire a keyboard event on the canvas.
	 * @param id
	 * @param keyCode
	 * @param keyChar
	 */
	private static void fire(int id, int keyCode, char keyChar) {
		long now = System.currentTimeMillis();
		KeyEvent e = new KeyEvent(canvas(), id, now, 0, keyCode, keyChar);
		if (SwingUtilities.isEventDispatchThread()) {
			// Dispatch directly if on EDT
			canvas().dispatchEvent(e);
		} else {
			post(e);
		}
	}

	/**
	 * Type a string into the focused canvas, one character at a time.
	 * @param s
	 */
	public static void typeString(String s) {
		withFocusedCanvas(() -> {
			for (char ch : s.toCharArray()) {
				// For printable characters, send TYPED only
				fire(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, ch);
				Global.sleep(30, 80);
			}
		});
	}

	/**
	 * Hold a key down on the focused canvas.
	 * @param keyCode
	 */
	public static void keyHold(int keyCode) {
		withFocusedCanvas(() -> fire(KeyEvent.KEY_PRESSED, keyCode, KeyEvent.CHAR_UNDEFINED));
	}

	/**
	 * Release a key on the focused canvas.
	 * @param keyCode
	 */
	public static void keyRelease(int keyCode) {
		withFocusedCanvas(() -> fire(KeyEvent.KEY_RELEASED, keyCode, KeyEvent.CHAR_UNDEFINED));
	}

	/**
	 * Press (hold and release) a key on the focused canvas.
	 * @param keyCode
	 */
	public static void keyPress(int keyCode) {
		withFocusedCanvas(() -> {
			fire(KeyEvent.KEY_PRESSED, keyCode, KeyEvent.CHAR_UNDEFINED);
			Global.sleep(20, 40); // real delay to preserve ordering
			fire(KeyEvent.KEY_RELEASED, keyCode, KeyEvent.CHAR_UNDEFINED);
		});
	}

	/**
	 * Press (hold and release) a key on the focused canvas, with a character.
	 */
	public static void holdShift()    { keyHold(KeyEvent.VK_SHIFT); }
	public static void releaseShift() { keyRelease(KeyEvent.VK_SHIFT); }

	/**
	 * Press (hold and release) the Enter key on the focused canvas.
	 */
	public static void enter() {
		withFocusedCanvas(() -> {
			fire(KeyEvent.KEY_PRESSED,  KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
			Global.sleep(20, 40);
			fire(KeyEvent.KEY_TYPED,    KeyEvent.VK_UNDEFINED, '\n');
			Global.sleep(5, 10);
			fire(KeyEvent.KEY_RELEASED, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED);
		});
	}
}
