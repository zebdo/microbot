package net.runelite.client.plugins.microbot.inputrecorder.replay;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration options for replaying recorded sessions.
 * Controls timing, randomization, and behavior when replay encounters issues.
 */
@Data
@Builder
public class ReplayOptions {

    /**
     * Speed multiplier for replay (1.0 = real-time, 2.0 = 2x speed, 0.5 = half speed)
     */
    @Builder.Default
    private double speedMultiplier = 1.0;

    /**
     * Whether to enforce original inter-action delays based on timestamps
     * If false, actions will execute as fast as possible.
     */
    @Builder.Default
    private boolean respectTiming = true;

    /**
     * Whether to add slight random variation to delays (more human-like)
     */
    @Builder.Default
    private boolean randomizeDelays = true;

    /**
     * Random delay variation range (in ms) when randomizeDelays is true
     * Actual delay = originalDelay +/- (0 to delayVariation)
     */
    @Builder.Default
    private int delayVariationMs = 50;

    /**
     * Whether to skip mouse movement actions (only execute clicks/menu actions)
     */
    @Builder.Default
    private boolean skipMouseMoves = false;

    /**
     * Whether to skip keyboard input actions
     */
    @Builder.Default
    private boolean skipKeyboardInput = false;

    /**
     * What to do when a target entity/object is not found during replay
     */
    @Builder.Default
    private TargetNotFoundBehavior targetNotFoundBehavior = TargetNotFoundBehavior.SKIP_AND_CONTINUE;

    /**
     * Maximum distance (in tiles) to search for a replacement target if original is missing
     */
    @Builder.Default
    private int maxTargetSearchDistance = 10;

    /**
     * Whether to log detailed replay progress
     */
    @Builder.Default
    private boolean verboseLogging = false;

    /**
     * Whether to pause replay on errors/warnings
     */
    @Builder.Default
    private boolean pauseOnError = false;

    /**
     * Maximum number of consecutive errors before aborting replay
     */
    @Builder.Default
    private int maxConsecutiveErrors = 5;

    /**
     * Whether to replay camera movements
     */
    @Builder.Default
    private boolean replayCameraMoves = true;

    public enum TargetNotFoundBehavior {
        /**
         * Skip the action and continue with next action
         */
        SKIP_AND_CONTINUE,

        /**
         * Try to find a nearby entity of the same type/ID
         */
        FIND_NEAREST_MATCH,

        /**
         * Pause replay and wait for manual intervention
         */
        PAUSE_AND_WAIT,

        /**
         * Abort the entire replay
         */
        ABORT
    }
}
