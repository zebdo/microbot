package net.runelite.client.plugins.microbot.pluginscheduler.ui;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerPlugin;
import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerState;
import net.runelite.client.plugins.microbot.pluginscheduler.model.PluginScheduleEntry;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.plugins.microbot.util.events.PluginPauseEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Displays real-time information about the scheduler status and plugins
 */
@Slf4j
public class SchedulerInfoPanel extends JPanel {
    private final SchedulerPlugin plugin;    
    // Scheduler status components
    private final JLabel statusLabel;
    private final JLabel runtimeLabel;
    private ZonedDateTime schedulerStartTime;
    
    // Control buttons
    private final JButton runSchedulerButton;
    private final JButton stopSchedulerButton;
    private final JButton loginButton;
    private final JButton pauseResumePluginButton; // button for only pusing the currently running  plugin (PluginScheduleEntry), by SchedulerPlugin
    private final JButton pauseResumeSchedulerButton; // button for pausing the whole scheduler -> all condition progress is paused for all PluginScheduleEntry currently running managed by SchedulerPlugin by SchedulerPlugin
    // Added hard reset button to reset all user condition states for all scheduled plugins-> initial settings are applied again to all start and stop conditions for  the curre
    private final JButton hardResetButton;
    
    // Combined plugin information panel
    private final JPanel pluginInfoPanel;
    
    // Current plugin components
    private JLabel currentPluginNameLabel;
    private JLabel currentPluginRuntimeLabel;
    private JProgressBar stopConditionProgressBar;
    private JLabel stopConditionStatusLabel;
    private ZonedDateTime currentPluginStartTime;
    
    // Next plugin components
    private JLabel nextPluginNameLabel;
    private JLabel nextPluginTimeLabel;
    private JLabel nextPluginScheduleLabel;
    
    // Previous plugin components
    private JLabel prevPluginNameLabel;
    private JLabel prevPluginDurationLabel;
    private JTextArea prevPluginStatusLabel;
    private JLabel prevPluginStopTimeLabel;
    
    // Player status components
    private final JPanel playerStatusPanel;
    private final JLabel activityLabel;
    private final JLabel activityIntensityLabel;
    private final JLabel idleTimeLabel;
    private final JLabel loginTimeLabel;
    private final JLabel breakStatusLabel;
    private final JLabel nextBreakLabel;
    private final JLabel breakDurationLabel;    

    // State tracking for optimized updates
    private PluginScheduleEntry lastTrackedCurrentPlugin;
    private PluginScheduleEntry lastTrackedPreviousPlugin;
    private PluginScheduleEntry lastTrackedNextPlugin;
  
  
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
        
        // Create control buttons panel
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        // Use GridLayout with 3 rows instead of 2x2 to properly fit all 5 buttons
        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Create run scheduler button
        runSchedulerButton = createCompactButton("Run Scheduler", new Color(76, 175, 80));
        runSchedulerButton.addActionListener(e -> {
            plugin.startScheduler();
            updateButtonStates();
        });
        buttonPanel.add(runSchedulerButton);
        
        // Create stop scheduler button
        stopSchedulerButton = createCompactButton("Stop Scheduler", new Color(244, 67, 54));
        stopSchedulerButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                plugin.stopScheduler();
                updateButtonStates();
            });
        });
        buttonPanel.add(stopSchedulerButton);
        
        // Create login button
        loginButton = createCompactButton("Login", new Color(33, 150, 243)); // Blue
        loginButton.addActionListener(e -> {            
            // Attempt login
            SwingUtilities.invokeLater(() -> {
                plugin.startLoginMonitoringThread();                                
            });
        });
        buttonPanel.add(loginButton);
        
        // Create pause/resume button
        pauseResumePluginButton = createCompactButton("Pause Plugin", new Color(0, 188, 212)); // Cyan color
        pauseResumePluginButton.setVisible(false); // Initially hidden
        pauseResumePluginButton.addActionListener(e -> {
            // Toggle the pause state
            
            
            // Update button text and color based on state
            if (!plugin.isCurrentPluginPaused()) {
                boolean pauseSuccess = plugin.pauseRunningPlugin();
                if (pauseSuccess){
                    pauseResumePluginButton.setText("Resume Plugin");
                    pauseResumePluginButton.setBackground(new Color(76, 175, 80)); // Green color
                }
            } else {
                if(plugin.isCurrentPluginPaused()){
                    plugin.resumeRunningPlugin();
                    pauseResumePluginButton.setText("Pause Plugin");
                    pauseResumePluginButton.setBackground(new Color(0, 188, 212)); // Cyan color
                }
            }
            updateCurrentPluginInfo();
            updateButtonStates();            
        });
        buttonPanel.add(pauseResumePluginButton);
        
        // Create pause/resume scheduler button
        pauseResumeSchedulerButton = createCompactButton("Pause Scheduler", new Color(255, 152, 0)); // Orange color
        pauseResumeSchedulerButton.addActionListener(e -> {
            // Toggle the pause state using our new methods
            if (plugin.isPaused()) {
                // Currently paused, so resume
                plugin.resumeScheduler();
                pauseResumeSchedulerButton.setText("Pause Scheduler");
                pauseResumeSchedulerButton.setBackground(new Color(255, 152, 0)); // Orange color
            } else {
                // Currently running, so pause
                plugin.pauseScheduler();
                pauseResumeSchedulerButton.setText("Resume Scheduler");
                pauseResumeSchedulerButton.setBackground(new Color(76, 175, 80)); // Green color
            }
            
            // Update UI
            updateCurrentPluginInfo();
            updateButtonStates();
        });
        buttonPanel.add(pauseResumeSchedulerButton);
        
        // Create hard reset button
        hardResetButton = createCompactButton("Hard Reset", new Color(156, 39, 176)); // Purple color
        hardResetButton.setToolTipText("Reset all user condition states for all scheduled plugins");
        hardResetButton.addActionListener(e -> showHardResetConfirmation());
        buttonPanel.add(hardResetButton);
        
        statusPanel.add(buttonPanel, gbc);
        
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

        // Create compact plugin information panel
        pluginInfoPanel = createCompactPluginInfoPanel();
        add(pluginInfoPanel);
        add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing
       
        // Initial refresh
        refresh();
    }
    
    /**
     * Creates a compact panel that combines previous, current, and next plugin information
     */
    private JPanel createCompactPluginInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ),
            "Plugin Information",
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            FontManager.getRunescapeBoldFont(),
            Color.WHITE
        ));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Create three sections: Previous, Current, Next
        JPanel topRow = new JPanel(new GridLayout(1, 3, 10, 0));
        topRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Previous Plugin Section
        JPanel prevSection = createCompactSection("Previous");
        GridBagConstraints gbc = createGbc(0, 0);
        
        prevSection.add(new JLabel("Name:"), gbc);
        gbc.gridx++;
        prevPluginNameLabel = createCompactValueLabel("None");
        prevSection.add(prevPluginNameLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        prevSection.add(new JLabel("Duration:"), gbc);
        gbc.gridx++;
        prevPluginDurationLabel = createCompactValueLabel("00:00:00");
        prevSection.add(prevPluginDurationLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        prevSection.add(new JLabel("Stop Time:"), gbc);
        gbc.gridx++;
        prevPluginStopTimeLabel = createCompactValueLabel("--:--:--");
        prevSection.add(prevPluginStopTimeLabel, gbc);

        // Current Plugin Section
        JPanel currentSection = createCompactSection("Current");
        gbc = createGbc(0, 0);
        
        currentSection.add(new JLabel("Name:"), gbc);
        gbc.gridx++;
        currentPluginNameLabel = createCompactValueLabel("None");
        currentSection.add(currentPluginNameLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        currentSection.add(new JLabel("Runtime:"), gbc);
        gbc.gridx++;
        currentPluginRuntimeLabel = createCompactValueLabel("00:00:00");
        currentSection.add(currentPluginRuntimeLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        currentSection.add(new JLabel("Conditions:"), gbc);
        gbc.gridx++;
        stopConditionStatusLabel = createCompactValueLabel("None");
        stopConditionStatusLabel.setToolTipText("Detailed stop condition information");
        currentSection.add(stopConditionStatusLabel, gbc);

        // Next Plugin Section
        JPanel nextSection = createCompactSection("Next");
        gbc = createGbc(0, 0);
        
        nextSection.add(new JLabel("Name:"), gbc);
        gbc.gridx++;
        nextPluginNameLabel = createCompactValueLabel("None");
        nextSection.add(nextPluginNameLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        nextSection.add(new JLabel("Time:"), gbc);
        gbc.gridx++;
        nextPluginTimeLabel = createCompactValueLabel("--:--");
        nextSection.add(nextPluginTimeLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        nextSection.add(new JLabel("Type:"), gbc);
        gbc.gridx++;
        nextPluginScheduleLabel = createCompactValueLabel("None");
        nextSection.add(nextPluginScheduleLabel, gbc);

        topRow.add(prevSection);
        topRow.add(currentSection);
        topRow.add(nextSection);

        // Add progress bar for current plugin at bottom
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        bottomPanel.setBorder(new EmptyBorder(2, 0, 2, 0)); // Reduced padding around progress bar
        
        stopConditionProgressBar = new JProgressBar(0, 100);
        stopConditionProgressBar.setStringPainted(true);
        stopConditionProgressBar.setString("No conditions");
        stopConditionProgressBar.setForeground(new Color(76, 175, 80));
        stopConditionProgressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        stopConditionProgressBar.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
        stopConditionProgressBar.setPreferredSize(new Dimension(0, 12)); // Much thinner progress bar
        
        JLabel stopProgressLabel = new JLabel("Stop Progress:");
        stopProgressLabel.setForeground(Color.WHITE);
        stopProgressLabel.setFont(FontManager.getRunescapeSmallFont());
        
        bottomPanel.add(stopProgressLabel, BorderLayout.WEST); // Shorter label
        bottomPanel.add(stopConditionProgressBar, BorderLayout.CENTER);

        // Add stop reason display for previous plugin
        JPanel stopReasonPanel = new JPanel(new BorderLayout());
        stopReasonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        stopReasonPanel.setBorder(new EmptyBorder(2, 0, 0, 0)); // Reduced padding around stop reason
        
        JLabel stopReasonLabel = new JLabel("Stop Reason:");
        stopReasonLabel.setForeground(Color.WHITE);
        stopReasonLabel.setFont(FontManager.getRunescapeSmallFont()); // Smaller font to save space
        
        prevPluginStatusLabel = createMultiLineTextArea("None");
        prevPluginStatusLabel.setPreferredSize(new Dimension(0, 45)); // Increased height for stop reason
        
        stopReasonPanel.add(stopReasonLabel, BorderLayout.WEST);
        stopReasonPanel.add(prevPluginStatusLabel, BorderLayout.CENTER);

        panel.add(topRow, BorderLayout.NORTH);
        panel.add(bottomPanel, BorderLayout.CENTER);
        panel.add(stopReasonPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    /**
     * Creates a compact section panel for plugin information
     */
    private JPanel createCompactSection(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(8, 8, 8, 8) // Increased padding for more space
            ),
            title,
            TitledBorder.CENTER,
            TitledBorder.TOP,
            FontManager.getRunescapeSmallFont(),
            Color.LIGHT_GRAY
        ));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        return panel;
    }
    
    /**
     * Creates a compact value label with smaller font
     */
    private JLabel createCompactValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setFont(FontManager.getRunescapeSmallFont());
        return label;
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
        gbc.insets = new Insets(6, 4, 6, 4); // Increased vertical spacing
        return gbc;
    }
    
    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }
    
    /**
     * Helper method to create and style a compact button
     * @param text Button text
     * @param bgColor Background color
     * @return Styled JButton
     */
    private JButton createCompactButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        // Make buttons more compact
        button.setFont(button.getFont().deriveFont(11f)); // Smaller font
        button.setMargin(new Insets(2, 4, 2, 4)); // Smaller margins
        return button;
    }
    
    /**
     * Helper method to create a text area for multi-line text display
     * @param text Initial text for the text area
     * @return A configured JTextArea
     */
    private JTextArea createMultiLineTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        textArea.setForeground(Color.WHITE);
        textArea.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        textArea.setFont(FontManager.getRunescapeFont());
        return textArea;
    }

    /**
     * Refreshes all displayed information with selective updates based on plugin state changes
     */
    public void refresh() {
        // Always update scheduler status and buttons for real-time feedback
        updateSchedulerStatus();
        updateButtonStates();
        
        // Always update player status as it changes frequently
        updatePlayerStatusInfo();
        
        // Get current plugin states
        PluginScheduleEntry currentPlugin = plugin.getCurrentPlugin();
        PluginScheduleEntry previousPlugin = plugin.getLastPlugin();
        PluginScheduleEntry nextPlugin = plugin.getNextScheduledPlugin();
        
        // Update current plugin info if it changed or is running (for runtime updates)
        if (currentPlugin != lastTrackedCurrentPlugin || 
            (currentPlugin != null && currentPlugin.isRunning())) {
            updateCurrentPluginInfo();
            lastTrackedCurrentPlugin = currentPlugin;
        }
        
        // Update previous plugin info only if it changed
        if (previousPlugin != lastTrackedPreviousPlugin) {
            updatePreviousPluginInfo();
            lastTrackedPreviousPlugin = previousPlugin;
        }
        updatePreviousPluginInfo();
        
        // Update next plugin info only if it changed
        if (nextPlugin != lastTrackedNextPlugin) {
            updateNextPluginInfo();
            lastTrackedNextPlugin = nextPlugin;
        }
        updateNextPluginInfo();
    }
    
    /**
     * Forces an immediate update of all plugin information.
     * Useful when plugin states change and immediate UI refresh is needed.
     */
    public void forcePluginInfoUpdate() {
        lastTrackedCurrentPlugin = null;
        lastTrackedPreviousPlugin = null;
        lastTrackedNextPlugin = null;
        updateCurrentPluginInfo();
        updatePreviousPluginInfo(); 
        updateNextPluginInfo();
    }
    
    /**
     * Updates the button states based on scheduler state
     */
    private void updateButtonStates() {
        SchedulerState state = plugin.getCurrentState();
        boolean isActive = plugin.getCurrentState().isSchedulerActive();
        
        // Only enable run button if we're in READY or HOLD state
        runSchedulerButton.setEnabled((!isActive && (state == SchedulerState.READY || state == SchedulerState.HOLD)) && !state.isPaused());
        
        runSchedulerButton.setToolTipText(
            !runSchedulerButton.isEnabled() ? 
            "Scheduler cannot be started in " + state.getDisplayName() + " state" :
            "Start running the scheduler");
        
        // Only enable stop button if scheduler is active
        stopSchedulerButton.setEnabled(isActive);
        stopSchedulerButton.setToolTipText(
            isActive ? "Stop the scheduler" : "Scheduler is not running");
            
        // Login button is only enreportFinishedabled when not actively running and not waiting for login
        loginButton.setEnabled((!isActive || 
            (state != SchedulerState.WAITING_FOR_LOGIN && 
             state != SchedulerState.LOGIN)) && !Microbot.isLoggedIn());
        loginButton.setToolTipText("Log in to the game");
        
       
        
        // Update pause/resume scheduler button state
        pauseResumeSchedulerButton.setEnabled(isActive || plugin.getCurrentState()==  SchedulerState.SCHEDULER_PAUSED);
        
        boolean isSchedulerPaused = plugin.isPaused();
        if (isSchedulerPaused) {
            pauseResumeSchedulerButton.setText("Resume Scheduler");
            pauseResumeSchedulerButton.setBackground(new Color(76, 175, 80)); // Green color
            pauseResumeSchedulerButton.setToolTipText("Resume the paused scheduler");
        } else {
            pauseResumeSchedulerButton.setText("Pause Scheduler");
            pauseResumeSchedulerButton.setBackground(new Color(255, 152, 0)); // Orange color
            pauseResumeSchedulerButton.setToolTipText("Pause the scheduler without stopping it");
        }
        boolean currentRunningPluginPaused = plugin.isCurrentPluginPaused();        
        pauseResumeSchedulerButton.setEnabled(isActive || plugin.getCurrentState()==  SchedulerState.RUNNING_PLUGIN_PAUSED);
         // Only show the pause button when a plugin is actively running
        pauseResumePluginButton.setVisible(state == SchedulerState.RUNNING_PLUGIN || 
                                    state == SchedulerState.RUNNING_PLUGIN_PAUSED);
        
        // If Scheulder PLugin is not Running any Plugin at the moment -> detect changed state and we're no longer running, ensure pause for scripts and plugin is reset to false is reset
        if (!(  state == SchedulerState.RUNNING_PLUGIN || 
                state == SchedulerState.RUNNING_PLUGIN_PAUSED ||  
                state == SchedulerState.SCHEDULER_PAUSED ) && PluginPauseEvent.isPaused()) {            
            PluginPauseEvent.setPaused(false); 
            pauseResumePluginButton.setText("Pause Plugin");
            pauseResumePluginButton.setBackground(new Color(255, 152, 0));
        }
        if(state == SchedulerState.RUNNING_PLUGIN ||
           state == SchedulerState.RUNNING_PLUGIN_PAUSED){
            pauseResumePluginButton.setEnabled(true);
            // Update pause/resume plugin button state
            if (currentRunningPluginPaused) {
                pauseResumePluginButton.setText("Resume Plugin");
                pauseResumePluginButton.setBackground(new Color(76, 175, 80)); // Green color
                pauseResumePluginButton.setToolTipText("Resume the currently paused plugin");
            } else {
                pauseResumePluginButton.setText("Pause Plugin");
                pauseResumePluginButton.setBackground(new Color(0, 188, 212)); // Cyan color
                pauseResumePluginButton.setToolTipText("Pause the currently running plugin");
            }
        }else {
            // If the scheduler is paused, disable the pause/resume plugin button
            pauseResumePluginButton.setEnabled(false);
            pauseResumePluginButton.setToolTipText("Cannot pause/resume plugin when scheduler is not running");
        }
        
        // Hard reset button is always enabled if there are plugins scheduled
        boolean hasScheduledPlugins = !plugin.getScheduledPlugins().isEmpty();
        hardResetButton.setEnabled(hasScheduledPlugins);
        hardResetButton.setToolTipText(
            hasScheduledPlugins ? 
            "Hard reset all user condition states for scheduled plugins" : 
            "No plugins scheduled to reset");
    }
    
    /**
     * Updates the scheduler status information
     */
    private void updateSchedulerStatus() {
        SchedulerState state = plugin.getCurrentState();
         // Update state information if available
        String stateInfo = state.getStateInformation();
        if (stateInfo != null && !stateInfo.isEmpty()) {
            statusLabel.setToolTipText(stateInfo);
        } else {
            statusLabel.setToolTipText(null);
        }
        // Update state display
        statusLabel.setText(state.getDisplayName());
        statusLabel.setForeground(state.getColor());
        
        // Update runtime if active
        if (plugin.getCurrentState().isSchedulerActive()) {
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
            // Update name with pause indicator if needed
            if (PluginPauseEvent.isPaused()) {
                currentPluginNameLabel.setText(currentPlugin.getCleanName() + " [PAUSED]");
                currentPluginNameLabel.setForeground(new Color(255, 152, 0)); // Orange
            } else {
                currentPluginNameLabel.setText(currentPlugin.getCleanName());
                currentPluginNameLabel.setForeground(Color.WHITE);
            }
            
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
            stopConditionProgressBar.setString("No conditions");
            currentPluginStartTime = null;
        }
    }
    
    /**
     * Updates information about the next scheduled plugin
     */
    private void updateNextPluginInfo() {

        PluginScheduleEntry nextPlugin = plugin.getUpComingPlugin();
        
        if (nextPlugin != null) {
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
        if (idleTime >= 0) {
            idleTimeLabel.setText(idleTime + " ticks");
            // Change color based on idle time
            if (idleTime > 100) {
                idleTimeLabel.setForeground(new Color(255, 106, 0)); // Orange for long idle
            } else if (idleTime > 50) {
                idleTimeLabel.setForeground(new Color(255, 193, 7)); // Yellow for medium idle
            } else {
                idleTimeLabel.setForeground(Color.WHITE); // Normal color
            }
        } else {
            idleTimeLabel.setText("0 ticks");
            idleTimeLabel.setForeground(Color.WHITE);
        }
        
        // Update login duration
        Duration loginDuration = Microbot.getLoginTime();
        if (loginDuration.getSeconds() > 0 && Microbot.isLoggedIn()) {
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

    /**
     * Updates information about the previously run plugin
     */
    private void updatePreviousPluginInfo() {
        PluginScheduleEntry lastPlugin = plugin.getLastPlugin();
        
        if (lastPlugin != null) {
            // Update name
            prevPluginNameLabel.setText(lastPlugin.getCleanName());
            
            // Update duration if available
            if (lastPlugin.getLastRunDuration() != null && !lastPlugin.getLastRunDuration().isZero()) {
                Duration duration = lastPlugin.getLastRunDuration();
                long hours = duration.toHours();
                long minutes = (duration.toMinutes() % 60);
                long seconds = (duration.getSeconds() % 60);
                prevPluginDurationLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            } else {
                prevPluginDurationLabel.setText("Unknown");
            }
            
            // Update stop reason
            String stopReason = lastPlugin.getLastStopReason();
            PluginScheduleEntry.StopReason stopReasonType = lastPlugin.getLastStopReasonType();
            
            if (stopReason != null && !stopReason.isEmpty()) {
                // Define colors for different states
                Color successColor = new Color(76, 175, 80); // Green for success
                Color unsuccessfulColor = new Color(255, 152, 0); // Orange for unsuccessful
                Color errorColor = new Color(244, 67, 54); // Red for error
                Color defaultColor = Color.WHITE; // Default color
                
                // Determine message color based on stop reason type and success state
                Color messageColor = defaultColor;
                
                if (stopReasonType != null) {
                    switch (stopReasonType) {
                        case PLUGIN_FINISHED:
                            messageColor = lastPlugin.isLastRunSuccessful() ? successColor : unsuccessfulColor;
                            break;
                        case ERROR:
                            messageColor = errorColor;
                            break;
                        case INTERRUPTED:
                            messageColor = unsuccessfulColor;
                            break;
                        default:
                            messageColor = defaultColor;
                            break;
                    }
                }
                
                // Set the text and color for the status text area
                prevPluginStatusLabel.setText(stopReason);
                prevPluginStatusLabel.setForeground(messageColor);
            } else {
                prevPluginStatusLabel.setText("Unknown");
                prevPluginStatusLabel.setForeground(Color.WHITE);
            }
            
            // Update stop time
            if (lastPlugin.getLastRunEndTime() != null) {
                ZonedDateTime stopTime = lastPlugin.getLastRunEndTime();
                prevPluginStopTimeLabel.setText(stopTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            } else {
                prevPluginStopTimeLabel.setText("Unknown");
            }
        } else {
            // Reset all fields
            prevPluginNameLabel.setText("None");
            prevPluginDurationLabel.setText("00:00:00");
            prevPluginStatusLabel.setText("N/A");
            prevPluginStatusLabel.setForeground(Color.WHITE);
            prevPluginStopTimeLabel.setText("--:--:--");
        }
    }

    /**
     * Shows a confirmation dialog for hard resetting all user conditions
     */
    private void showHardResetConfirmation() {
        String message = 
            "<html><body width='400'>" +
            "<h2>Hard Reset All User Conditions</h2>" +
            "<p>This will perform a complete reset of all user conditions for all scheduled plugins.</p>" +
            "<p><b>This will reset:</b></p>" +
            "<ul>" +
            "<li>All accumulated state tracking variables</li>" +
            "<li>Maximum trigger counters</li>" +
            "<li>Daily/periodic usage limits</li>" +
            "<li>Historical tracking data</li>" +
            "<li>Time-based condition states</li>" +
            "</ul>" +
            "<p><b>Are you sure you want to continue?</b></p>" +
            "</body></html>";
        
        int result = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            message,
            "Hard Reset Confirmation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            hardResetAllUserConditions();
        }
    }
    
    /**
     * Performs a hard reset on all user conditions for all scheduled plugins
     */
    private void hardResetAllUserConditions() {
        try {
            // Delegate the hard reset operation to the SchedulerPlugin
            List<String> resetPlugins = plugin.hardResetAllUserConditions();
            
            // Show success message with details
            String resultMessage = String.format(
                "<html><body width='400'>" +
                "<h2>Hard Reset Complete</h2>" +
                "<p>Successfully reset %d user condition states.</p>",
                resetPlugins.size());
            
            if (!resetPlugins.isEmpty()) {
                resultMessage += "<p><b>Reset conditions for:</b></p><ul>";
                for (String pluginName : resetPlugins) {
                    resultMessage += "<li>" + pluginName + "</li>";
                }
                resultMessage += "</ul>";
            }
            
            resultMessage += "</body></html>";
            
            JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(this),
                resultMessage,
                "Hard Reset Complete",
                JOptionPane.INFORMATION_MESSAGE
            );
            
            log.info("Hard reset completed for {} user condition states", resetPlugins.size());
            
        } catch (Exception e) {
            // Show error message
            JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(this),
                "An error occurred while resetting user conditions: " + e.getMessage(),
                "Hard Reset Error",
                JOptionPane.ERROR_MESSAGE
            );
            
            log.error("Error during hard reset of user conditions", e);
        }
    }
}