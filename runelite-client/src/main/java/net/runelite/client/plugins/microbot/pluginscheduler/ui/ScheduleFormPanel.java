package net.runelite.client.plugins.microbot.pluginscheduler.ui;

import lombok.Getter;

import net.runelite.client.plugins.microbot.pluginscheduler.type.PluginScheduleEntry;
import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerPlugin;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.IntervalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.SingleTriggerTimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ScheduleFormPanel extends JPanel {
    private final SchedulerPlugin plugin;

    @Getter
    private JComboBox<String> pluginComboBox;
    private JComboBox<String> timeConditionTypeComboBox;
    private JCheckBox randomSchedulingCheckbox;

    // Condition config panels
    private JPanel conditionConfigPanel;
    private JPanel currentConditionPanel;

    private JButton addButton;
    private JButton updateButton;
    private JButton removeButton;
    private JButton controlButton;

    private PluginScheduleEntry selectedPlugin;

    // Constants for time condition types
    private static final String CONDITION_NOW = "Run Now";
    private static final String CONDITION_SPECIFIC_TIME = "Run at Specific Time";
    private static final String CONDITION_INTERVAL = "Run at Interval";
    private static final String CONDITION_TIME_WINDOW = "Run in Time Window";
    private static final String[] TIME_CONDITION_TYPES = {
            CONDITION_NOW,
            CONDITION_SPECIFIC_TIME,
            CONDITION_INTERVAL,
            CONDITION_TIME_WINDOW
    };

    public ScheduleFormPanel(SchedulerPlugin plugin) {
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARK_GRAY_COLOR),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)),
                "Schedule Configuration",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                FontManager.getRunescapeBoldFont(),
                Color.WHITE));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Create the form panel with GridBagLayout for flexibility
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Plugin selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        JLabel pluginLabel = new JLabel("Plugin:");
        pluginLabel.setForeground(Color.WHITE);
        pluginLabel.setFont(FontManager.getRunescapeFont());
        formPanel.add(pluginLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        pluginComboBox = new JComboBox<>();
        formPanel.add(pluginComboBox, gbc);
        updatePluginList(plugin.getAvailablePlugins());

        // Time condition type selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        JLabel conditionTypeLabel = new JLabel("Schedule Type:");
        conditionTypeLabel.setForeground(Color.WHITE);
        conditionTypeLabel.setFont(FontManager.getRunescapeFont());
        formPanel.add(conditionTypeLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        timeConditionTypeComboBox = new JComboBox<>(TIME_CONDITION_TYPES);
        timeConditionTypeComboBox.addActionListener(e -> updateConditionPanel());
        formPanel.add(timeConditionTypeComboBox, gbc);

        // Dynamic condition config panel
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        conditionConfigPanel = new JPanel(new BorderLayout());
        conditionConfigPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        conditionConfigPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        formPanel.add(conditionConfigPanel, gbc);

        // Random scheduling checkbox
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        randomSchedulingCheckbox = new JCheckBox("Allow random scheduling");
        randomSchedulingCheckbox.setSelected(true);
        randomSchedulingCheckbox.setToolTipText(
            "<html>When enabled, this plugin can be randomly selected when multiple plugins are due to run.<br>" +
            "If disabled, this plugin will have higher priority than randomizable plugins.</html>");
        randomSchedulingCheckbox.setForeground(Color.WHITE);
        randomSchedulingCheckbox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        formPanel.add(randomSchedulingCheckbox, gbc);

        // Add the form panel to the center
        add(formPanel, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        buttonPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        addButton = createButton("Add Schedule", ColorScheme.BRAND_ORANGE_TRANSPARENT);
        updateButton = createButton("Update Schedule", ColorScheme.BRAND_ORANGE);
        removeButton = createButton("Remove Schedule", ColorScheme.PROGRESS_ERROR_COLOR);

        // Control button (Run Now/Stop)
        controlButton = createButton("Run Now", ColorScheme.PROGRESS_COMPLETE_COLOR);
        controlButton.addActionListener(this::onControlButtonClicked);

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(controlButton);

        // Add button panel to the bottom
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Initialize the condition panel
        updateConditionPanel();
    }

    /**
     * Updates the condition configuration panel based on the selected time condition type
     */
    private void updateConditionPanel() {
        // Clear existing panel
        if (currentConditionPanel != null) {
            conditionConfigPanel.remove(currentConditionPanel);
        }
        
        // Create a new panel based on selection
        String selectedType = (String) timeConditionTypeComboBox.getSelectedItem();
        currentConditionPanel = new JPanel();
        currentConditionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        
        // Create the appropriate condition panel
        if (CONDITION_NOW.equals(selectedType)) {
            // No configuration needed for immediate execution
            JLabel nowLabel = new JLabel("Plugin will run immediately when scheduled");
            nowLabel.setForeground(Color.WHITE);
            currentConditionPanel.add(nowLabel);
        } 
        else if (CONDITION_SPECIFIC_TIME.equals(selectedType)) {
            // Create a panel for SingleTriggerTimeCondition
            currentConditionPanel.setLayout(new GridBagLayout());
            JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            timePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            
            JLabel timeLabel = new JLabel("Run at time: ");
            timeLabel.setForeground(Color.WHITE);
            timePanel.add(timeLabel);
            
            SpinnerDateModel dateModel = new SpinnerDateModel();
            JSpinner timeSpinner = new JSpinner(dateModel);
            timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "yyyy-MM-dd HH:mm"));
            
            // Set default to current time + 1 hour
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            timeSpinner.setValue(calendar.getTime());
            
            timePanel.add(timeSpinner);
            
            currentConditionPanel.add(timePanel, gbc);
            
            // Store the component for later access
            currentConditionPanel.putClientProperty("timeSpinner", timeSpinner);
            
            // Add description
            gbc.gridy++;
            JLabel descLabel = new JLabel("Plugin will run once at the specified time");
            descLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            descLabel.setFont(FontManager.getRunescapeSmallFont());
            currentConditionPanel.add(descLabel, gbc);
        }
        else if (CONDITION_INTERVAL.equals(selectedType)) {
            // Use ConditionConfigPanelUtil to create an interval condition panel
            currentConditionPanel.setLayout(new GridBagLayout());
            ConditionConfigPanelUtil.createTimeConfigPanel(currentConditionPanel, gbc, currentConditionPanel);
        }
        else if (CONDITION_TIME_WINDOW.equals(selectedType)) {
            // Use ConditionConfigPanelUtil to create a time window condition panel
            currentConditionPanel.setLayout(new GridBagLayout());
            ConditionConfigPanelUtil.createTimeWindowConfigPanel(currentConditionPanel, gbc, currentConditionPanel, true);
        }
        
        // Add the panel
        conditionConfigPanel.add(currentConditionPanel, BorderLayout.CENTER);
        conditionConfigPanel.revalidate();
        conditionConfigPanel.repaint();
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(FontManager.getRunescapeSmallFont());
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }
        });

        return button;
    }

    public void updatePluginList(List<String> plugins) {
        if (plugins == null || plugins.isEmpty()) {
            return;
        }

        pluginComboBox.removeAllItems();
        for (String plugin : plugins) {
            pluginComboBox.addItem(plugin);
        }
    }

    public void loadPlugin(PluginScheduleEntry entry) {
        this.selectedPlugin = entry;
        pluginComboBox.setSelectedItem(entry.getName());
        
        // Set random scheduling checkbox
        randomSchedulingCheckbox.setSelected(entry.isAllowRandomScheduling());
        
        // Determine the time condition type and set appropriate panel
        TimeCondition startCondition = null;
        if (entry.getStartConditionManager() != null) {
            List<TimeCondition> timeConditions = entry.getStartConditionManager().getTimeConditions();
            if (!timeConditions.isEmpty()) {
                startCondition = timeConditions.get(0);
            }
        }
        
        if (startCondition instanceof SingleTriggerTimeCondition) {
            timeConditionTypeComboBox.setSelectedItem(CONDITION_SPECIFIC_TIME);
            updateConditionPanel();
            
            // Set the time spinner value
            SingleTriggerTimeCondition singleTrigger = (SingleTriggerTimeCondition) startCondition;
            JSpinner timeSpinner = (JSpinner) currentConditionPanel.getClientProperty("timeSpinner");
            if (timeSpinner != null) {
                Date date = Date.from(singleTrigger.getTargetTime().toInstant());
                timeSpinner.setValue(date);
            }
        }
        else if (startCondition instanceof IntervalCondition) {
            timeConditionTypeComboBox.setSelectedItem(CONDITION_INTERVAL);
            // Let the panel initialize with defaults
            updateConditionPanel();
            // IntervalCondition settings would need to be set here
            // but it's complex with the current implementation
        }
        else if (startCondition instanceof TimeWindowCondition) {
            timeConditionTypeComboBox.setSelectedItem(CONDITION_TIME_WINDOW);
            // Let the panel initialize with defaults
            updateConditionPanel();
            // TimeWindow settings would need to be set here
        }
        else {
            // Default to "Run Now"
            timeConditionTypeComboBox.setSelectedItem(CONDITION_NOW);
            updateConditionPanel();
        }
        
        // Update the control button to reflect the current plugin
        updateControlButton();
    }

    public void clearForm() {
        selectedPlugin = null;

        if (pluginComboBox.getItemCount() > 0) {
            pluginComboBox.setSelectedIndex(0);
        }

        // Reset condition type to default
        timeConditionTypeComboBox.setSelectedItem(CONDITION_NOW);
        updateConditionPanel();
        
        // Reset random scheduling
        randomSchedulingCheckbox.setSelected(true);
        
        // Update the control button
        updateControlButton();
    }

    public PluginScheduleEntry getPluginFromForm() {
        String pluginName = (String) pluginComboBox.getSelectedItem();
        if (pluginName == null || pluginName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a plugin.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        // Create TimeCondition based on selected type
        String selectedType = (String) timeConditionTypeComboBox.getSelectedItem();
        TimeCondition timeCondition = null;
        
        if (CONDITION_NOW.equals(selectedType)) {
            // For immediate execution, use IntervalCondition with very short interval
            timeCondition = new IntervalCondition(java.time.Duration.ofSeconds(1));
        } 
        else if (CONDITION_SPECIFIC_TIME.equals(selectedType)) {
            // Get the time from spinner
            JSpinner timeSpinner = (JSpinner) currentConditionPanel.getClientProperty("timeSpinner");
            if (timeSpinner != null) {
                Date selectedDate = (Date) timeSpinner.getValue();
                ZonedDateTime triggerTime = ZonedDateTime.ofInstant(
                        selectedDate.toInstant(),
                        ZoneId.systemDefault());
                
                // Create SingleTriggerTimeCondition
                timeCondition = new SingleTriggerTimeCondition(triggerTime);
            }
        }
        else if (CONDITION_INTERVAL.equals(selectedType)) {
            // Create IntervalCondition using ConditionConfigPanelUtil
            timeCondition = ConditionConfigPanelUtil.createTimeCondition(currentConditionPanel);
        }
        else if (CONDITION_TIME_WINDOW.equals(selectedType)) {
            // Create TimeWindowCondition using ConditionConfigPanelUtil
            timeCondition = (TimeCondition) ConditionConfigPanelUtil.createTimeWindowCondition(currentConditionPanel);
        }
        
        if (timeCondition == null) {
            JOptionPane.showMessageDialog(this,
                    "Failed to create time condition.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        // Create the plugin schedule entry with the time condition
        PluginScheduleEntry entry = new PluginScheduleEntry(
                pluginName,
                timeCondition,
                true,  // enabled by default
                randomSchedulingCheckbox.isSelected());
        
        return entry;
    }

    public void updateControlButton() {
        if (plugin.isScheduledPluginRunning()) {
            // If a plugin is running, show "Stop Plugin" button
            controlButton.setText("Stop Running Plugin");
            controlButton.setEnabled(true);
        } else if (selectedPlugin != null) {
            controlButton.setText("Run \"" + selectedPlugin.getCleanName() + "\" Now");
            controlButton.setEnabled(true);
        } else {
            // If no plugin is selected, disable the button
            controlButton.setText("Select a Plugin");
            controlButton.setEnabled(false);
        }
    }

    private void onControlButtonClicked(ActionEvent e) {
        if (plugin.isScheduledPluginRunning()) {
            // Stop the current plugin
            plugin.forceStopCurrentPlugin();
        } else if (selectedPlugin != null) {
            // Run the selected plugin now
            plugin.startPlugin(selectedPlugin);
        }
    }

    public void setEditMode(boolean editMode) {
        updateButton.setEnabled(editMode);
        removeButton.setEnabled(editMode);
        addButton.setEnabled(!editMode);

        // Update control button based on edit mode
        updateControlButton();
    }

    public void setAddButtonAction(ActionListener listener) {
        addButton.addActionListener(listener);
    }

    public void setUpdateButtonAction(ActionListener listener) {
        updateButton.addActionListener(listener);
    }

    public void setRemoveButtonAction(ActionListener listener) {
        removeButton.addActionListener(listener);
    }
}
