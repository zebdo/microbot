package net.runelite.client.plugins.microbot.inputrecorder.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.inputrecorder.InputRecorderConfig;
import net.runelite.client.plugins.microbot.inputrecorder.models.InputRecordingSession;
import net.runelite.client.plugins.microbot.inputrecorder.replay.ActionReplayer;
import net.runelite.client.plugins.microbot.inputrecorder.service.InputRecordingService;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;

/**
 * Overlay panel that displays the current recording/replay status.
 * Shows:
 * - Recording state (idle/recording/paused)
 * - Session name and duration
 * - Number of actions recorded
 * - Replay progress
 */
@Slf4j
public class InputRecorderOverlay extends OverlayPanel {

    private final InputRecorderConfig config;
    private final InputRecordingService recordingService;
    private final ActionReplayer actionReplayer;

    @Inject
    public InputRecorderOverlay(
            InputRecorderConfig config,
            InputRecordingService recordingService,
            ActionReplayer actionReplayer
    ) {
        this.config = config;
        this.recordingService = recordingService;
        this.actionReplayer = actionReplayer;

        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        InputRecordingSession session = recordingService.getCurrentSession();
        boolean isRecording = recordingService.isRecording();
        boolean isReplaying = actionReplayer.isReplaying();

        // Only show overlay if recording or replaying
        if (!isRecording && !isReplaying && session == null) {
            return null;
        }

        panelComponent.getChildren().clear();

        // Title
        if (isRecording) {
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Input Recorder")
                    .color(session.isPaused() ? Color.YELLOW : Color.GREEN)
                    .build());
        } else if (isReplaying) {
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Replay Mode")
                    .color(actionReplayer.isPaused() ? Color.YELLOW : Color.CYAN)
                    .build());
        }

        // Recording status
        if (isRecording && session != null) {
            String status = session.isPaused() ? "PAUSED" : "RECORDING";
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(status)
                    .rightColor(session.isPaused() ? Color.YELLOW : Color.GREEN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Profile:")
                    .right(session.getProfileName())
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Session:")
                    .right(session.getName())
                    .build());

            // Duration
            long durationMs;
            if (session.isPaused() && session.getPausedAt() != null) {
                durationMs = session.getPausedAt() - session.getStartedAt() - session.getTotalPausedMs();
            } else {
                durationMs = System.currentTimeMillis() - session.getStartedAt() - session.getTotalPausedMs();
            }
            String durationStr = formatDuration(durationMs);

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Duration:")
                    .right(durationStr)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Actions:")
                    .right(String.valueOf(session.getTotalActions()))
                    .build());

            // Show actions/minute rate
            if (durationMs > 0) {
                double actionsPerMinute = (session.getTotalActions() * 60000.0) / durationMs;
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Rate:")
                        .right(String.format("%.1f APM", actionsPerMinute))
                        .build());
            }
        }

        // Replay status
        if (isReplaying) {
            String status = actionReplayer.isPaused() ? "PAUSED" : "REPLAYING";
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(status)
                    .rightColor(actionReplayer.isPaused() ? Color.YELLOW : Color.CYAN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Progress:")
                    .right(String.format("%d/%d",
                            actionReplayer.getCurrentActionIndex() + 1,
                            actionReplayer.getTotalActions()))
                    .build());

            // Progress bar would be nice here
            double progress = actionReplayer.getProgress() * 100;
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("")
                    .right(String.format("%.1f%%", progress))
                    .rightColor(Color.CYAN)
                    .build());
        }

        return super.render(graphics);
    }

    /**
     * Formats duration in HH:MM:SS format
     */
    private String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(millis);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
