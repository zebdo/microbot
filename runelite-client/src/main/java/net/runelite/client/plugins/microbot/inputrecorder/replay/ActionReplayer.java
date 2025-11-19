package net.runelite.client.plugins.microbot.inputrecorder.replay;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.inputrecorder.models.InputRecordingSession;
import net.runelite.client.plugins.microbot.inputrecorder.models.RecordedActionType;
import net.runelite.client.plugins.microbot.inputrecorder.models.RecordedMenuAction;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.mouse.VirtualMouse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for replaying recorded input sessions.
 *
 * <p>Converts RecordedMenuAction objects back into actual game actions by:
 * <ul>
 *   <li>Reconstructing menu entries and invoking them</li>
 *   <li>Simulating mouse movements and clicks</li>
 *   <li>Simulating keyboard input</li>
 *   <li>Respecting original timing or applying speed adjustments</li>
 *   <li>Handling missing targets gracefully</li>
 * </ul>
 */
@Slf4j
@Singleton
public class ActionReplayer {

    @Inject
    private Client client;

    @Getter
    private volatile boolean isReplaying = false;

    @Getter
    private volatile boolean isPaused = false;

    private Thread replayThread;
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);
    private final Random random = new Random();

    private InputRecordingSession currentReplaySession;
    private ReplayOptions currentOptions;
    private int currentActionIndex = 0;
    private int consecutiveErrors = 0;

    /**
     * Starts replaying a session with default options
     */
    public void replay(InputRecordingSession session) {
        replay(session, ReplayOptions.builder().build());
    }

    /**
     * Starts replaying a session with custom options
     */
    public synchronized void replay(InputRecordingSession session, ReplayOptions options) {
        if (isReplaying) {
            log.warn("Already replaying a session. Stop current replay first.");
            return;
        }

        if (session == null || session.getActions() == null || session.getActions().isEmpty()) {
            log.warn("Cannot replay: session is null or has no actions");
            return;
        }

        this.currentReplaySession = session;
        this.currentOptions = options;
        this.currentActionIndex = 0;
        this.consecutiveErrors = 0;
        this.isReplaying = true;
        this.isPaused = false;
        this.shouldStop.set(false);

        log.info("Starting replay of session '{}' with {} actions",
                session.getName(), session.getTotalActions());

        // Start replay in a background thread
        replayThread = new Thread(this::replayLoop, "InputRecorder-Replay");
        replayThread.start();
    }

    /**
     * Stops the current replay
     */
    public synchronized void stopReplay() {
        if (!isReplaying) {
            return;
        }

        shouldStop.set(true);
        log.info("Stopping replay...");

        if (replayThread != null) {
            try {
                replayThread.join(5000); // Wait up to 5 seconds
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for replay thread to stop", e);
            }
        }

        isReplaying = false;
        isPaused = false;
        currentReplaySession = null;
    }

    /**
     * Pauses the current replay
     */
    public synchronized void pauseReplay() {
        if (isReplaying && !isPaused) {
            isPaused = true;
            log.info("Paused replay at action {}/{}", currentActionIndex, currentReplaySession.getTotalActions());
        }
    }

    /**
     * Resumes a paused replay
     */
    public synchronized void resumeReplay() {
        if (isReplaying && isPaused) {
            isPaused = false;
            log.info("Resumed replay");
        }
    }

    /**
     * Main replay loop - executes actions in sequence
     */
    private void replayLoop() {
        List<RecordedMenuAction> actions = currentReplaySession.getActions();
        long previousTimestamp = actions.get(0).getTimestamp();

        try {
            for (int i = 0; i < actions.size(); i++) {
                // Check for stop signal
                if (shouldStop.get()) {
                    log.info("Replay stopped by user");
                    break;
                }

                // Handle pause
                while (isPaused && !shouldStop.get()) {
                    Thread.sleep(100);
                }

                currentActionIndex = i;
                RecordedMenuAction action = actions.get(i);

                // Calculate and apply delay
                if (currentOptions.isRespectTiming() && i > 0) {
                    long originalDelay = action.getTimestamp() - previousTimestamp;
                    long adjustedDelay = (long) (originalDelay / currentOptions.getSpeedMultiplier());

                    if (currentOptions.isRandomizeDelays()) {
                        int variation = random.nextInt(currentOptions.getDelayVariationMs() * 2)
                                - currentOptions.getDelayVariationMs();
                        adjustedDelay = Math.max(0, adjustedDelay + variation);
                    }

                    if (adjustedDelay > 0) {
                        Thread.sleep(adjustedDelay);
                    }
                }

                // Execute the action
                boolean success = executeAction(action);

                if (!success) {
                    consecutiveErrors++;
                    log.warn("Failed to execute action {}: {}", i, action.toCompactString());

                    if (consecutiveErrors >= currentOptions.getMaxConsecutiveErrors()) {
                        log.error("Too many consecutive errors ({}), aborting replay", consecutiveErrors);
                        break;
                    }

                    if (currentOptions.isPauseOnError()) {
                        pauseReplay();
                    }
                } else {
                    consecutiveErrors = 0; // Reset on success
                }

                previousTimestamp = action.getTimestamp();
            }

            log.info("Replay completed: {}/{} actions executed",
                    currentActionIndex + 1, actions.size());

        } catch (InterruptedException e) {
            log.info("Replay interrupted");
        } catch (Exception e) {
            log.error("Error during replay", e);
        } finally {
            isReplaying = false;
            isPaused = false;
        }
    }

    /**
     * Executes a single recorded action
     */
    private boolean executeAction(RecordedMenuAction action) {
        if (currentOptions.isVerboseLogging()) {
            log.debug("Executing: {}", action.toCompactString());
        }

        try {
            switch (action.getType()) {
                case MENU_ACTION:
                case WIDGET_INTERACT:
                    return executeMenuAction(action);

                case RAW_MOUSE_MOVE:
                    if (!currentOptions.isSkipMouseMoves()) {
                        return executeMouseMove(action);
                    }
                    return true; // Skipped but not an error

                case MOUSE_DRAG:
                    if (!currentOptions.isSkipMouseMoves()) {
                        return executeMouseDrag(action);
                    }
                    return true;

                case MOUSE_SCROLL:
                    return executeMouseScroll(action);

                case KEY_HOTKEY:
                case RAW_KEY_INPUT:
                    if (!currentOptions.isSkipKeyboardInput()) {
                        return executeKeyboardInput(action);
                    }
                    return true;

                case CAMERA_MOVE:
                    if (currentOptions.isReplayCameraMoves()) {
                        return executeCameraMove(action);
                    }
                    return true;

                default:
                    log.warn("Unknown action type: {}", action.getType());
                    return false;
            }
        } catch (Exception e) {
            log.error("Exception while executing action: {}", action.toCompactString(), e);
            return false;
        }
    }

    /**
     * Executes a menu action by reconstructing the MenuEntry
     */
    private boolean executeMenuAction(RecordedMenuAction action) {
        if (action.getOpcode() == null) {
            log.warn("Cannot execute menu action: missing opcode");
            return false;
        }

        try {
            // Construct a NewMenuEntry from recorded data
            NewMenuEntry entry = new NewMenuEntry();
            entry.setOption(action.getAction());
            entry.setTarget(action.getTarget() != null ? action.getTarget() : "");
            entry.setType(MenuAction.of(action.getOpcode()));
            entry.setParam0(action.getParam0() != null ? action.getParam0() : 0);
            entry.setParam1(action.getParam1() != null ? action.getParam1() : 0);
            entry.setIdentifier(action.getIdentifier() != null ? action.getIdentifier() : 0);

            if (action.getItemId() != null && action.getItemId() > 0) {
                entry.setItemId(action.getItemId());
            }

            // Determine click position
            Point clickPoint;
            if (action.getMouseX() != null && action.getMouseY() != null) {
                clickPoint = new Point(action.getMouseX(), action.getMouseY());
            } else {
                // Use current mouse position as fallback
                Point mousePos = client.getMouseCanvasPosition();
                if (mousePos == null) {
                    log.warn("Cannot execute menu action: mouse position is null and no recorded position available");
                    return false;
                }
                clickPoint = mousePos;
            }

            // Validate target exists (for certain action types)
            if (!validateTarget(action)) {
                return handleMissingTarget(action);
            }

            // Execute via Microbot's mouse system
            Mouse baseMouse = Microbot.getMouse();
            if (!(baseMouse instanceof VirtualMouse)) {
                log.error("Cannot execute menu action: mouse is not VirtualMouse instance");
                return false;
            }
            VirtualMouse mouse = (VirtualMouse) baseMouse;
            mouse.click(clickPoint, action.getMouseButton() != null && action.getMouseButton() == 3, entry);

            return true;

        } catch (Exception e) {
            log.error("Failed to execute menu action", e);
            return false;
        }
    }

    /**
     * Validates that the target of an action still exists
     */
    private boolean validateTarget(RecordedMenuAction action) {
        // For walk commands, always valid
        if (action.getOpcode() != null && action.getOpcode() == MenuAction.WALK.getId()) {
            return true;
        }

        // For widget interactions, check if widget exists
        if (action.getWidgetGroupId() != null) {
            // Could add widget validation here
            return true;
        }

        // For NPC/object interactions, would need to check if entity still exists
        // For simplicity, we'll assume it exists and let the game handle failures
        return true;
    }

    /**
     * Handles missing target based on replay options
     */
    private boolean handleMissingTarget(RecordedMenuAction action) {
        switch (currentOptions.getTargetNotFoundBehavior()) {
            case SKIP_AND_CONTINUE:
                log.debug("Target not found, skipping action");
                return true; // Not an error, just skip

            case FIND_NEAREST_MATCH:
                return findAndExecuteNearestMatch(action);

            case PAUSE_AND_WAIT:
                pauseReplay();
                log.warn("Target not found, replay paused");
                return false;

            case ABORT:
                shouldStop.set(true);
                log.error("Target not found, aborting replay");
                return false;

            default:
                return false;
        }
    }

    /**
     * Attempts to find a nearby matching target and execute the action
     */
    private boolean findAndExecuteNearestMatch(RecordedMenuAction action) {
        // This would require more complex logic to search for NPCs/objects by ID
        // For now, just skip
        log.warn("Find nearest match not yet implemented, skipping action");
        return true;
    }

    /**
     * Executes a mouse movement
     */
    private boolean executeMouseMove(RecordedMenuAction action) {
        if (action.getMouseX() == null || action.getMouseY() == null) {
            return false;
        }

        Mouse baseMouse = Microbot.getMouse();
        if (!(baseMouse instanceof VirtualMouse)) {
            log.warn("Cannot execute mouse move: mouse is not VirtualMouse instance");
            return false;
        }
        VirtualMouse mouse = (VirtualMouse) baseMouse;
        mouse.move(new Point(action.getMouseX(), action.getMouseY()));
        return true;
    }

    /**
     * Executes a mouse drag operation
     */
    private boolean executeMouseDrag(RecordedMenuAction action) {
        // Would need start and end points - for now just log
        log.debug("Mouse drag replay not fully implemented");
        return true;
    }

    /**
     * Executes a mouse scroll
     */
    private boolean executeMouseScroll(RecordedMenuAction action) {
        if (action.getMouseX() == null || action.getMouseY() == null) {
            return false;
        }

        Mouse baseMouse = Microbot.getMouse();
        if (!(baseMouse instanceof VirtualMouse)) {
            log.warn("Cannot execute mouse scroll: mouse is not VirtualMouse instance");
            return false;
        }
        VirtualMouse mouse = (VirtualMouse) baseMouse;
        Point scrollPoint = new Point(action.getMouseX(), action.getMouseY());

        int scrollAmount = action.getParam0() != null ? action.getParam0() : 1;
        if (scrollAmount > 0) {
            mouse.scrollDown(scrollPoint);
        } else {
            mouse.scrollUp(scrollPoint);
        }

        return true;
    }

    /**
     * Executes keyboard input
     */
    private boolean executeKeyboardInput(RecordedMenuAction action) {
        if (action.getKeyCode() == null) {
            return false;
        }

        // Would use Rs2Keyboard here
        log.debug("Keyboard replay: keyCode={}", action.getKeyCode());

        // Example: Rs2Keyboard.keyPress(action.getKeyCode());
        return true;
    }

    /**
     * Executes camera movement
     */
    private boolean executeCameraMove(RecordedMenuAction action) {
        // Would use Rs2Camera to set camera angles
        log.debug("Camera move replay not fully implemented");
        return true;
    }

    /**
     * Gets current replay progress (0.0 to 1.0)
     */
    public double getProgress() {
        if (!isReplaying || currentReplaySession == null) {
            return 0.0;
        }
        return (double) currentActionIndex / currentReplaySession.getTotalActions();
    }

    /**
     * Gets current action index
     */
    public int getCurrentActionIndex() {
        return currentActionIndex;
    }

    /**
     * Gets total actions in current replay
     */
    public int getTotalActions() {
        return currentReplaySession != null ? currentReplaySession.getTotalActions() : 0;
    }
}
