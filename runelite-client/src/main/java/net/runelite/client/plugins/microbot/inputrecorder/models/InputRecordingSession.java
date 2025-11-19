package net.runelite.client.plugins.microbot.inputrecorder.models;

import com.google.gson.annotations.Expose;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single recording session containing a sequence of player actions.
 * Sessions can be saved to disk, loaded for replay, or analyzed for ML/profiling.
 *
 * <p>Sessions are self-contained with metadata about when/where they were recorded
 * and what profile/scenario they represent.</p>
 */
@Data
public class InputRecordingSession {

    // ===== Session Identity =====

    /**
     * Unique identifier for this session
     */
    @Expose
    private UUID id;

    /**
     * Human-readable name for this session (e.g., "Zulrah Kill #1", "Vorkath Practice")
     */
    @Expose
    private String name;

    /**
     * Profile/category this session belongs to (e.g., "bossing", "skilling", "questing")
     * Used to organize and filter sessions.
     */
    @Expose
    private String profileName;

    /**
     * Scenario or script name if this was recorded during automated play
     * (e.g., "WoodcuttingScript", "ZulrahKiller")
     */
    @Expose
    private String scenarioName;

    // ===== Timing Information =====

    /**
     * Absolute timestamp when recording started (ms since epoch)
     */
    @Expose
    private long startedAt;

    /**
     * Absolute timestamp when recording stopped (ms since epoch)
     * Null if session is still recording.
     */
    @Expose
    private Long stoppedAt;

    /**
     * Total duration in milliseconds
     * Computed from stoppedAt - startedAt, or current time if still recording.
     */
    @Expose
    private Long durationMs;

    /**
     * Game tick when recording started
     */
    @Expose
    private int startGameTick;

    /**
     * Game tick when recording stopped
     */
    @Expose
    private Integer endGameTick;

    // ===== Recorded Actions =====

    /**
     * Ordered list of all recorded actions in this session
     */
    @Expose
    private List<RecordedMenuAction> actions;

    // ===== Player/Account Information =====

    /**
     * OSRS character/account name (if available and privacy settings allow)
     * Can be anonymized or excluded for privacy.
     */
    @Expose
    private String accountName;

    /**
     * Player's combat level at session start
     */
    @Expose
    private Integer combatLevel;

    /**
     * Player's total skill level at session start
     */
    @Expose
    private Integer totalLevel;

    // ===== Location Information =====

    /**
     * Starting world coordinates
     */
    @Expose
    private Integer startWorldX;

    @Expose
    private Integer startWorldY;

    @Expose
    private Integer startPlane;

    /**
     * Region ID(s) where this session took place
     * Useful for filtering/categorizing sessions by location.
     */
    @Expose
    private List<Integer> regions;

    // ===== Statistics =====

    /**
     * Total number of actions recorded
     */
    @Expose
    private int totalActions;

    /**
     * Breakdown of actions by type (for quick analysis)
     */
    @Expose
    private Map<RecordedActionType, Integer> actionTypeCounts;

    /**
     * Total mouse distance traveled (in pixels)
     */
    @Expose
    private Long totalMouseDistance;

    /**
     * Average time between actions (ms)
     */
    @Expose
    private Double averageActionInterval;

    // ===== Session State =====

    /**
     * Whether this session is currently recording
     */
    private transient boolean isRecording;

    /**
     * Whether this session is paused
     */
    private transient boolean isPaused;

    /**
     * Timestamp when session was paused (for calculating accurate durations)
     */
    private transient Long pausedAt;

    /**
     * Total time spent paused (to exclude from duration calculations)
     */
    @Expose
    private long totalPausedMs;

    // ===== Versioning & Compatibility =====

    /**
     * Version of the recording format (for future compatibility)
     */
    @Expose
    private String formatVersion = "1.0";

    /**
     * Microbot version used to record this session
     */
    @Expose
    private String microbotVersion;

    /**
     * RuneLite version used to record this session
     */
    @Expose
    private String runeliteVersion;

    // ===== Additional Metadata =====

    /**
     * Custom tags for organization/search (e.g., ["pvp", "pk", "practice"])
     */
    @Expose
    private List<String> tags;

    /**
     * Free-form notes about this session
     */
    @Expose
    private String notes;

    /**
     * Custom metadata map for extensibility
     */
    @Expose
    private Map<String, Object> customMetadata;

    /**
     * Constructor initializes a new session with default values
     */
    public InputRecordingSession() {
        this.id = UUID.randomUUID();
        this.actions = new ArrayList<>();
        this.actionTypeCounts = new HashMap<>();
        this.regions = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.customMetadata = new HashMap<>();
        this.startedAt = System.currentTimeMillis();
        this.isRecording = false;
        this.isPaused = false;
        this.totalPausedMs = 0;
        this.totalActions = 0;
    }

    /**
     * Adds an action to this session and updates statistics
     */
    public void addAction(RecordedMenuAction action) {
        this.actions.add(action);
        this.totalActions++;

        // Update type counts
        RecordedActionType type = action.getType();
        actionTypeCounts.put(type, actionTypeCounts.getOrDefault(type, 0) + 1);
    }

    /**
     * Computes and updates session statistics
     * Should be called before saving/serializing.
     */
    public void computeStatistics() {
        if (actions.isEmpty()) {
            return;
        }

        // Compute total mouse distance
        long mouseDistance = 0;
        Integer lastX = null, lastY = null;
        for (RecordedMenuAction action : actions) {
            if (action.getMouseX() != null && action.getMouseY() != null) {
                if (lastX != null && lastY != null) {
                    int dx = action.getMouseX() - lastX;
                    int dy = action.getMouseY() - lastY;
                    mouseDistance += (long) Math.sqrt(dx * dx + dy * dy);
                }
                lastX = action.getMouseX();
                lastY = action.getMouseY();
            }
        }
        this.totalMouseDistance = mouseDistance;

        // Compute average action interval
        if (actions.size() > 1) {
            long firstTime = actions.get(0).getTimestamp();
            long lastTime = actions.get(actions.size() - 1).getTimestamp();
            this.averageActionInterval = (double) (lastTime - firstTime) / (actions.size() - 1);
        }

        // Compute duration (excluding paused time)
        if (stoppedAt != null) {
            this.durationMs = stoppedAt - startedAt - totalPausedMs;
        } else {
            this.durationMs = System.currentTimeMillis() - startedAt - totalPausedMs;
        }
    }

    /**
     * Returns a summary string for display
     */
    public String getSummary() {
        return String.format("Session '%s' [%s]: %d actions over %s",
                name != null ? name : id.toString().substring(0, 8),
                profileName != null ? profileName : "unknown",
                totalActions,
                formatDuration(durationMs != null ? durationMs : 0));
    }

    /**
     * Formats duration in human-readable form
     */
    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
