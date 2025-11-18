package net.runelite.client.plugins.microbot.inputrecorder.models;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single recorded player action that can be analyzed or replayed.
 * This class captures all relevant information about mouse clicks, keyboard input,
 * and menu interactions in a normalized, OSRS-compatible format.
 *
 * <p>The design philosophy is to store everything as menu actions (or close abstractions)
 * rather than raw coordinates, enabling meaningful replay and ML analysis.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordedMenuAction {

    // ===== Timing Information =====

    /**
     * Absolute timestamp in milliseconds (System.currentTimeMillis())
     * Used for precise timing analysis and replay synchronization.
     */
    @Expose
    private long timestamp;

    /**
     * Game tick number when this action occurred.
     * Essential for tick-perfect replay and understanding game state context.
     */
    @Expose
    private int gameTick;

    /**
     * Milliseconds elapsed since the session started.
     * Useful for relative timing analysis without exposing absolute timestamps.
     */
    @Expose
    private long relativeTimeMs;

    // ===== Action Classification =====

    /**
     * The type/category of this action (menu action, widget click, camera move, etc.)
     */
    @Expose
    private RecordedActionType type;

    /**
     * Human-readable description of the action (e.g., "Walk here", "Attack", "Cast Ice Barrage")
     * Derived from menu option or synthesized for non-menu actions.
     */
    @Expose
    private String action;

    /**
     * Target of the action (e.g., NPC name, object name, item name)
     * Corresponds to MenuEntry.getTarget() when applicable.
     */
    @Expose
    private String target;

    // ===== Menu Action Data (OSRS Protocol) =====

    /**
     * OSRS menu opcode (e.g., WALK, NPC_FIRST_OPTION, CC_OP, etc.)
     * Maps to MenuAction enum value.
     */
    @Expose
    private Integer opcode;

    /**
     * First parameter (context-dependent: widget child ID, NPC index, etc.)
     */
    @Expose
    private Integer param0;

    /**
     * Second parameter (context-dependent: widget group ID, item slot, etc.)
     */
    @Expose
    private Integer param1;

    /**
     * Entity or widget identifier (NPC ID, object ID, widget packed ID, etc.)
     */
    @Expose
    private Integer identifier;

    /**
     * Item ID if the action involves an item (inventory use, equip, drop, etc.)
     */
    @Expose
    private Integer itemId;

    // ===== World Coordinates =====

    /**
     * World X coordinate (for tile-based actions like walking, object interaction)
     */
    @Expose
    private Integer worldX;

    /**
     * World Y coordinate
     */
    @Expose
    private Integer worldY;

    /**
     * Plane/height level (0-3 typically)
     */
    @Expose
    private Integer plane;

    // ===== Screen-Space Information =====

    /**
     * Mouse X coordinate on the game canvas at the time of action
     */
    @Expose
    private Integer mouseX;

    /**
     * Mouse Y coordinate on the game canvas at the time of action
     */
    @Expose
    private Integer mouseY;

    /**
     * Mouse button used (1=left, 2=middle, 3=right)
     */
    @Expose
    private Integer mouseButton;

    // ===== Keyboard Information =====

    /**
     * Key code (from java.awt.event.KeyEvent) if action involves keyboard
     */
    @Expose
    private Integer keyCode;

    /**
     * Character typed (if applicable and not privacy-filtered)
     */
    @Expose
    private Character keyChar;

    /**
     * Modifier key states at time of action
     */
    @Expose
    private boolean shiftDown;

    @Expose
    private boolean ctrlDown;

    @Expose
    private boolean altDown;

    // ===== Widget Information =====

    /**
     * Widget path or identifier (e.g., "inventory:5:3" for inventory slot)
     * Stored as string for flexibility and human readability.
     */
    @Expose
    private String widgetInfo;

    /**
     * Widget group ID (for widget-based actions)
     */
    @Expose
    private Integer widgetGroupId;

    /**
     * Widget child ID (for widget-based actions)
     */
    @Expose
    private Integer widgetChildId;

    // ===== Camera/Viewport Information =====

    /**
     * Camera yaw (horizontal rotation) at time of action
     * Useful for understanding player perspective.
     */
    @Expose
    private Integer cameraYaw;

    /**
     * Camera pitch (vertical angle) at time of action
     */
    @Expose
    private Integer cameraPitch;

    // ===== Additional Metadata =====

    /**
     * Detailed description for debugging and human analysis
     * Can include context like "Clicked NPC 'Goblin' (id=3203) at coords (3244, 3220)"
     */
    @Expose
    private String description;

    /**
     * Category tag for ML/profiling (combat, skilling, banking, questing, etc.)
     * Can be inferred from action type or manually tagged.
     */
    @Expose
    private String category;

    /**
     * Whether this action was successful (for post-action validation)
     * Can be populated by checking if expected game state change occurred.
     */
    @Expose
    private Boolean successful;

    /**
     * Custom metadata as JSON string for extensibility
     */
    @Expose
    private String metadata;

    /**
     * Creates a compact one-line summary of this action for logging/debugging.
     * Example: "[T:1234ms/Tick:567] MENU_ACTION: Attack Goblin @ (3244,3220) [Mouse:512,384]"
     */
    public String toCompactString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[T:%dms/Tick:%d] ", relativeTimeMs, gameTick));
        sb.append(type).append(": ");
        if (action != null) {
            sb.append(action);
        }
        if (target != null && !target.isEmpty()) {
            sb.append(" ").append(target);
        }
        if (worldX != null && worldY != null) {
            sb.append(String.format(" @ (%d,%d)", worldX, worldY));
        }
        if (mouseX != null && mouseY != null) {
            sb.append(String.format(" [Mouse:%d,%d]", mouseX, mouseY));
        }
        return sb.toString();
    }
}
