package net.runelite.client.plugins.microbot.pluginscheduler;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pluginscheduler.model.PluginScheduleEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;

public class SchedulerInfoOverlay extends OverlayPanel {
    private final SchedulerPlugin plugin;

    @Inject
    SchedulerInfoOverlay(SchedulerPlugin plugin, SchedulerConfig config) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_CENTER);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(280, 300));
            
            // Title with icon
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("📅 Plugin Scheduler")
                    .color(Color.CYAN)
                    .build());

            // Current state
            SchedulerState currentState = plugin.getCurrentState();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("State:")
                    .right(getStateWithIcon(currentState))
                    .rightColor(currentState.getColor())
                    .build());

            // Current plugin info
            PluginScheduleEntry currentPlugin = plugin.getCurrentPlugin();
            if (currentPlugin != null) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("🔧 Current:")
                        .right(currentPlugin.getName())
                        .rightColor(Color.WHITE)
                        .build());
            }

            // Next plugin info
            PluginScheduleEntry nextPlugin = plugin.getUpComingPlugin();
            if (nextPlugin != null) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("⏭️ Next:")
                        .right(nextPlugin.getName())
                        .rightColor(Color.LIGHT_GRAY)
                        .build());

                // Time until next plugin
                if (nextPlugin.getCurrentStartTriggerTime().isPresent()) {
                    Duration timeUntilNext = Duration.between(
                        java.time.Instant.now(),
                        nextPlugin.getCurrentStartTriggerTime().get().toInstant()
                    );
                    
                    // Only show if it's in the future
                    if (!timeUntilNext.isNegative()) {
                        panelComponent.getChildren().add(LineComponent.builder()
                                .left("⏰ In:")
                                .right(formatDuration(timeUntilNext))
                                .rightColor(Color.YELLOW)
                                .build());
                    }
                }
            }

            // Break information (conditionally shown)
            if (currentState.isBreaking()) {
                Duration breakDuration = plugin.getCurrentBreakDuration();
                if (!breakDuration.isZero()) {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("🛌 Break:")
                            .right(formatDuration(breakDuration))
                            .rightColor(new Color(100, 149, 237))
                            .build());
                }
            } else {
                Duration timeUntilBreak = plugin.getTimeUntilNextBreak();
                if (!timeUntilBreak.isZero()) {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("🛌 Break in:")
                            .right(formatDuration(timeUntilBreak))
                            .rightColor(new Color(173, 216, 230))
                            .build());
                }
            }

            // Version
            panelComponent.getChildren().add(LineComponent.builder().build()); // spacer
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Version:")
                    .right(SchedulerPlugin.VERSION)
                    .rightColor(Color.GRAY)
                    .build());

        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

    /**
     * Adds appropriate icon to state display based on current state
     */
    private String getStateWithIcon(SchedulerState state) {
        String icon;
        switch (state) {
            case READY:
                icon = "✅";
                break;
            case SCHEDULING:
                icon = "🔄";
                break;
            case STARTING_PLUGIN:
                icon = "🚀";
                break;
            case RUNNING_PLUGIN:
                icon = "⚡";
                break;
            case RUNNING_PLUGIN_PAUSED:
            case SCHEDULER_PAUSED:
                icon = "⏸️";
                break;
            case HARD_STOPPING_PLUGIN:
            case SOFT_STOPPING_PLUGIN:
                icon = "🛑";
                break;
            case HOLD:
                icon = "🔴";
                break;
            case BREAK:
            case PLAYSCHEDULE_BREAK:
                icon = "😴";
                break;
            case WAITING_FOR_SCHEDULE:
                icon = "⏳";
                break;
            case WAITING_FOR_LOGIN:
                icon = "🔑";
                break;
            case LOGIN:
                icon = "🚪";
                break;
            case ERROR:
                icon = "❌";
                break;
            case INITIALIZING:
                icon = "⚙️";
                break;
            case UNINITIALIZED:
                icon = "❓";
                break;
            case WAITING_FOR_STOP_CONDITION:
                icon = "⏳";
                break;
            default:
                icon = "❓";
                break;
        }
        return icon + " " + state.getDisplayName();
    }

    /**
     * Formats duration in a human-readable format
     */
    private String formatDuration(Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            return "Now";
        }
        
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
