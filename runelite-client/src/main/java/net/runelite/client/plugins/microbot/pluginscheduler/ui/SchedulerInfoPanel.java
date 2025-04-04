package net.runelite.client.plugins.microbot.pluginscheduler.ui;

import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerPlugin;
import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerState;
import net.runelite.client.plugins.microbot.pluginscheduler.type.PluginScheduleEntry;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;

import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.Duration;
import java.time.ZonedDateTime;


/**
 * Displays real-time information about the scheduler status and plugins
 */
public class SchedulerInfoPanel extends JPanel {
    private final SchedulerPlugin plugin;
    
    // Scheduler status components
    private final JLabel statusLabel;
    private final JLabel runtimeLabel;
    private ZonedDateTime schedulerStartTime;
    
    // Current plugin components
    private final JPanel currentPluginPanel;
    private final JLabel currentPluginNameLabel;
    private final JLabel currentPluginRuntimeLabel;
    private final JProgressBar stopConditionProgressBar;
    private final JLabel stopConditionStatusLabel;
    private ZonedDateTime currentPluginStartTime;
    
    // Next plugin components
    private final JPanel nextPluginPanel;
    private final JLabel nextPluginNameLabel;
    private final JLabel nextPluginTimeLabel;
    private final JLabel nextPluginScheduleLabel;
    
    // Player status components
    private final JPanel playerStatusPanel;
    private final JLabel activityLabel;
    private final JLabel activityIntensityLabel;
    private final JLabel idleTimeLabel;
    private final JLabel loginTimeLabel;
    private final JLabel breakStatusLabel;
    private final JLabel nextBreakLabel;
    private final JLabel breakDurationLabel;
    

  
  
    public SchedulerInfoPanel(SchedulerPlugin plugin) {
        this.plugin = plugin;
        
        // Use a box layout instead of BorderLayout
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Add panels with some vertical spacing
        JPanel statusPanel = createInfoPanel("Scheduler Status");
        GridBagConstraints gbc = createGbc(0, 0);
        
        statusPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx++;
        statusLabel = createValueLabel("Not Running");
        statusPanel.add(statusLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        statusPanel.add(new JLabel("Runtime:"), gbc);
        gbc.gridx++;
        runtimeLabel = createValueLabel("00:00:00");
        statusPanel.add(runtimeLabel, gbc);
        
        add(statusPanel);
        add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing
         
        // Create the player status panel
        playerStatusPanel = createInfoPanel("Player Status");
        gbc = createGbc(0, 0);

        playerStatusPanel.add(new JLabel("Activity:"), gbc);
        gbc.gridx++;
        activityLabel = createValueLabel("None");
        playerStatusPanel.add(activityLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        playerStatusPanel.add(new JLabel("Intensity:"), gbc);
        gbc.gridx++;
        activityIntensityLabel = createValueLabel("None");
        playerStatusPanel.add(activityIntensityLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        playerStatusPanel.add(new JLabel("Idle Time:"), gbc);
        gbc.gridx++;
        idleTimeLabel = createValueLabel("0 ticks");
        playerStatusPanel.add(idleTimeLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        playerStatusPanel.add(new JLabel("Login Duration:"), gbc);
        gbc.gridx++;
        loginTimeLabel = createValueLabel("Not logged in");
        playerStatusPanel.add(loginTimeLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        playerStatusPanel.add(new JLabel("Break Status:"), gbc);
        gbc.gridx++;
        breakStatusLabel = createValueLabel("Not on break");
        playerStatusPanel.add(breakStatusLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        playerStatusPanel.add(new JLabel("Next Break:"), gbc);
        gbc.gridx++;
        nextBreakLabel = createValueLabel("--:--:--");
        playerStatusPanel.add(nextBreakLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        playerStatusPanel.add(new JLabel("Current Break:"), gbc);
        gbc.gridx++;
        breakDurationLabel = createValueLabel("00:00:00");
        playerStatusPanel.add(breakDurationLabel, gbc);

        add(playerStatusPanel, BorderLayout.CENTER);


        currentPluginPanel = createInfoPanel("Current Plugin");
        gbc = createGbc(0, 0);
        
        currentPluginPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx++;
        currentPluginNameLabel = createValueLabel("None");
        currentPluginPanel.add(currentPluginNameLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        currentPluginPanel.add(new JLabel("Runtime:"), gbc);
        gbc.gridx++;
        currentPluginRuntimeLabel = createValueLabel("00:00:00");
        currentPluginPanel.add(currentPluginRuntimeLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        currentPluginPanel.add(new JLabel("Stop Conditions:"), gbc);
        gbc.gridx++;
        stopConditionStatusLabel = createValueLabel("None");
        stopConditionStatusLabel.setToolTipText("Detailed stop condition information");
        currentPluginPanel.add(stopConditionStatusLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        stopConditionProgressBar = new JProgressBar(0, 100);
        stopConditionProgressBar.setStringPainted(true);
        stopConditionProgressBar.setString("0%");
        stopConditionProgressBar.setForeground(new Color(76, 175, 80));
        stopConditionProgressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        stopConditionProgressBar.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
        currentPluginPanel.add(stopConditionProgressBar, gbc);
        
        add(currentPluginPanel);
        add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing
        
        nextPluginPanel = createInfoPanel("Next Scheduled Plugin");
        gbc = createGbc(0, 0);
        
        nextPluginPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx++;
        nextPluginNameLabel = createValueLabel("None");
        nextPluginPanel.add(nextPluginNameLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        nextPluginPanel.add(new JLabel("Scheduled Time:"), gbc);
        gbc.gridx++;
        nextPluginTimeLabel = createValueLabel("--:--");
        nextPluginPanel.add(nextPluginTimeLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        nextPluginPanel.add(new JLabel("Schedule Type:"), gbc);
        gbc.gridx++;
        nextPluginScheduleLabel = createValueLabel("None");
        nextPluginPanel.add(nextPluginScheduleLabel, gbc);
        
        add(nextPluginPanel);
       

        // Initial refresh
        refresh();
    }
    
    private JPanel createInfoPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ),
            title,
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            FontManager.getRunescapeBoldFont(),
            Color.WHITE
        ));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        return panel;
    }
    
    private GridBagConstraints createGbc(int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = (x == 0) ? 0.0 : 1.0; // Labels (x=0) don't expand, values (x=1) do
        gbc.weighty = 1.0;
        gbc.anchor = (x == 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        return gbc;
    }
    
    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }
    
    /**
     * Refreshes all displayed information
     */
    public void refresh() {
        updateSchedulerStatus();
        updateCurrentPluginInfo();
        updateNextPluginInfo();
        updatePlayerStatusInfo();
    }
    
    /**
     * Updates the scheduler status information
     */
    private void updateSchedulerStatus() {
        SchedulerState state = plugin.getCurrentState();
        
        // Update state display
        statusLabel.setText(state.getDisplayName());
        statusLabel.setForeground(state.getColor());
        
        // Update runtime if active
        if (plugin.isSchedulerActive()) {
            if (schedulerStartTime == null) {
                schedulerStartTime = ZonedDateTime.now();
            }
            
            Duration runtime = Duration.between(schedulerStartTime, ZonedDateTime.now());
            long totalSeconds = runtime.getSeconds();
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            
            runtimeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        } else {
            schedulerStartTime = null;
            runtimeLabel.setText("00:00:00");
        }
    }
    
    /**
     * Updates information about the currently running plugin
     */
    private void updateCurrentPluginInfo() {
        PluginScheduleEntry currentPlugin = plugin.getCurrentPlugin();
        
        if (currentPlugin != null && currentPlugin.isRunning()) {
            // Set visibility
            currentPluginPanel.setVisible(true);
            
            // Update name
            currentPluginNameLabel.setText(currentPlugin.getCleanName());
            
            // Update runtime
            if (currentPluginStartTime == null) {
                currentPluginStartTime = ZonedDateTime.now();
            }
            
            Duration runtime = Duration.between(currentPluginStartTime, ZonedDateTime.now());
            long totalSeconds = runtime.getSeconds();
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            
            currentPluginRuntimeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            
            // Update stop condition status
            if (currentPlugin.hasAnyStopConditions()) {
                int total = currentPlugin.getTotalStopConditionCount();
                int satisfied = currentPlugin.getSatisfiedStopConditionCount();
                
                // Updates the stop condition status text
                stopConditionStatusLabel.setText(String.format("%d/%d conditions met", satisfied, total));
                stopConditionStatusLabel.setToolTipText(currentPlugin.getDetailedStopConditionsStatus());
                
                // Update progress bar
                double progress = currentPlugin.getStopConditionProgress();
                stopConditionProgressBar.setValue((int) progress);
                stopConditionProgressBar.setString(String.format("%.1f%%", progress));
                
                // Color the progress bar based on progress
                if (progress > 80) {
                    stopConditionProgressBar.setForeground(new Color(76, 175, 80)); // Green
                } else if (progress > 50) {
                    stopConditionProgressBar.setForeground(new Color(255, 193, 7)); // Amber
                } else {
                    stopConditionProgressBar.setForeground(new Color(33, 150, 243)); // Blue
                }
                
                stopConditionProgressBar.setVisible(true);
            } else {
                stopConditionStatusLabel.setText("None");
                stopConditionStatusLabel.setToolTipText("No stop conditions defined");
                stopConditionProgressBar.setVisible(false);
            }
        } else {
            // Reset all fields
            currentPluginNameLabel.setText("None");
            currentPluginRuntimeLabel.setText("00:00:00");
            stopConditionStatusLabel.setText("None");
            stopConditionProgressBar.setValue(0);
            stopConditionProgressBar.setString("0%");
            currentPluginStartTime = null;
        }
    }
    
    /**
     * Updates information about the next scheduled plugin
     */
    private void updateNextPluginInfo() {
        PluginScheduleEntry nextPlugin = plugin.getNextScheduledPlugin();
        
        if (nextPlugin != null) {
            // Set visibility
            nextPluginPanel.setVisible(true);
            
            // Update name
            nextPluginNameLabel.setText(nextPlugin.getCleanName());
            
            // Set the next run time display (already handles various condition types)
            nextPluginTimeLabel.setText(nextPlugin.getNextRunDisplay());
            
            // Create an enhanced schedule description
            StringBuilder scheduleDesc = new StringBuilder(nextPlugin.getIntervalDisplay());
            
            // Add information about one-time schedules
            if (nextPlugin.hasAnyOneTimeStartConditions()) {
                if (nextPlugin.hasTriggeredOneTimeStartConditions() && !nextPlugin.canStartTriggerAgain()) {
                    scheduleDesc.append(" (Completed)");
                } else {
                    scheduleDesc.append(" (One-time)");
                }
            }
            
            nextPluginScheduleLabel.setText(scheduleDesc.toString());
        } else {
            // Reset all fields
            nextPluginNameLabel.setText("None");
            nextPluginTimeLabel.setText("--:--");
            nextPluginScheduleLabel.setText("None");
        }
    }
    
    /**
     * Updates information about the player's status
     */
    private void updatePlayerStatusInfo() {
        // Update activity info
        Activity activity = plugin.getCurrentActivity();
        if (activity != null) {
            activityLabel.setText(activity.toString());
            activityLabel.setForeground(Color.WHITE);
        } else {
            activityLabel.setText("None");
            activityLabel.setForeground(Color.GRAY);
        }
        
        // Update activity intensity
        ActivityIntensity intensity = plugin.getCurrentIntensity();
        if (intensity != null) {
            activityIntensityLabel.setText(intensity.getName());
            activityIntensityLabel.setForeground(Color.WHITE);
        } else {
            activityIntensityLabel.setText("None");
            activityIntensityLabel.setForeground(Color.GRAY);
        }
        
        // Update idle time
        int idleTime = plugin.getIdleTime();
        idleTimeLabel.setText(idleTime + " ticks");
        
        // Update login duration
        Duration loginDuration = plugin.getLoginDuration();
        if (loginDuration.getSeconds() > 0) {
            long hours = loginDuration.toHours();
            long minutes = (loginDuration.toMinutes() % 60);
            long seconds = (loginDuration.getSeconds() % 60);
            loginTimeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            loginTimeLabel.setForeground(Color.WHITE);
        } else {
            loginTimeLabel.setText("Not logged in");
            loginTimeLabel.setForeground(Color.GRAY);
        }
        
        // Update break status
        boolean onBreak = plugin.isOnBreak();
        if (onBreak) {
            breakStatusLabel.setText("On Break");
            breakStatusLabel.setForeground(new Color(255, 193, 7)); // Amber color
        } else {
            breakStatusLabel.setText("Not on break");
            breakStatusLabel.setForeground(Color.WHITE);
        }
        
        // Update next break time
        Duration timeUntilBreak = plugin.getTimeUntilNextBreak();
        if (timeUntilBreak.getSeconds() > 0) {
            long hours = timeUntilBreak.toHours();
            long minutes = (timeUntilBreak.toMinutes() % 60);
            long seconds = (timeUntilBreak.getSeconds() % 60);
            nextBreakLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            nextBreakLabel.setForeground(Color.WHITE);
        } else {
            nextBreakLabel.setText("--:--:--");
            nextBreakLabel.setForeground(Color.GRAY);
        }
        
        // Update current break duration
        Duration breakDuration = plugin.getCurrentBreakDuration();
        if (breakDuration.getSeconds() > 0) {
            long hours = breakDuration.toHours();
            long minutes = (breakDuration.toMinutes() % 60);
            long seconds = (breakDuration.getSeconds() % 60);
            breakDurationLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            breakDurationLabel.setForeground(onBreak ? new Color(255, 193, 7) : Color.WHITE); // Amber if on break
        } else {
            breakDurationLabel.setText("00:00:00");
            breakDurationLabel.setForeground(Color.GRAY);
        }
    }
}