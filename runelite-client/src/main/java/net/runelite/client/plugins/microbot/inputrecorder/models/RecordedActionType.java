package net.runelite.client.plugins.microbot.inputrecorder.models;

/**
 * Enumeration of different types of recorded player actions.
 * Each type represents a category of input that can be tracked and replayed.
 */
public enum RecordedActionType {
    /**
     * In-game menu action (walk, use item, cast spell, attack, etc.)
     * This is the primary action type for OSRS gameplay.
     */
    MENU_ACTION,

    /**
     * UI widget interaction (clicking inventory, spellbook, prayers, settings, etc.)
     * Represents clicks on game interface elements.
     */
    WIDGET_INTERACT,

    /**
     * Camera movement (rotation, zoom, pitch changes)
     * Tracks camera manipulation for scene awareness.
     */
    CAMERA_MOVE,

    /**
     * Keyboard hotkey usage (F-keys, number keys, Ctrl+click, etc.)
     * Represents functional key presses that trigger game actions.
     */
    KEY_HOTKEY,

    /**
     * Compressed mouse movement segments
     * Used for ML analysis and behavioral profiling.
     * Stored as waypoints rather than every pixel.
     */
    RAW_MOUSE_MOVE,

    /**
     * Raw keyboard input (for text entry, chat, etc.)
     * May be marked for privacy filtering.
     */
    RAW_KEY_INPUT,

    /**
     * Mouse scroll event (zoom, scrolling lists, etc.)
     */
    MOUSE_SCROLL,

    /**
     * Mouse drag operation (camera rotation, item dragging)
     */
    MOUSE_DRAG
}
