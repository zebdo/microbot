package net.runelite.client.plugins.microbot.pluginscheduler.condition.time.ui;
import java.time.ZoneId;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.components.DateRangePanel;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.components.IntervalPickerPanel;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.components.SingleDateTimePickerPanel;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.components.TimeRangePanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.time.ZonedDateTime;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.DayOfWeekCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.IntervalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.enums.RepeatCycle;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.SingleTriggerTimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.components.InitialDelayPanel;
@Slf4j
public class TimeConditionPanelUtil {
    public static void createIntervalConfigPanel(JPanel panel, GridBagConstraints gbc) {
        // Title and initial setup
        JLabel titleLabel = new JLabel("Time Interval Configuration:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        // Create and add interval picker component
        gbc.gridy++;
        IntervalPickerPanel intervalPicker = new IntervalPickerPanel(true);
        panel.add(intervalPicker, gbc);
        
        // Add initial delay configuration
        gbc.gridy++;
        InitialDelayPanel initialDelayPanel = new InitialDelayPanel();
        
        panel.add(initialDelayPanel, gbc);
        
        // Add a helpful description
        gbc.gridy++;
        JLabel descriptionLabel = new JLabel("Plugin will stop after specified time interval");
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        // Add additional info about randomization
        gbc.gridy++;
        JLabel randomInfoLabel = new JLabel("Random intervals make your bot behavior less predictable");
        randomInfoLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        randomInfoLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(randomInfoLabel, gbc);
        
        // Add information about initial delay
        gbc.gridy++;
        JLabel initialDelayInfoLabel = new JLabel("Initial delay adds waiting time before the first interval trigger");
        initialDelayInfoLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        initialDelayInfoLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(initialDelayInfoLabel, gbc);
        
        // Store components for later access
        panel.putClientProperty("intervalPicker", intervalPicker);
        panel.putClientProperty("initialDelayPanel", initialDelayPanel);
        // Removed as delayMinutesSpinner is now encapsulated in InitialDelayPanel
        // Removed as delaySecondsSpinner is now encapsulated in InitialDelayPanel
    }
    
    /**
     * Helper method to validate min and max intervals ensure min <= max
     */
    private static void validateMinMaxIntervals(
            JSpinner minHoursSpinner, JSpinner minMinutesSpinner, 
            JSpinner maxHoursSpinner, JSpinner maxMinutesSpinner, 
            boolean isMinUpdated) {
        
        int minHours = (Integer) minHoursSpinner.getValue();
        int minMinutes = (Integer) minMinutesSpinner.getValue();
        int maxHours = (Integer) maxHoursSpinner.getValue();
        int maxMinutes = (Integer) maxMinutesSpinner.getValue();
        
        int minTotalMinutes = minHours * 60 + minMinutes;
        int maxTotalMinutes = maxHours * 60 + maxMinutes;
        
        if (isMinUpdated) {
            // If min was updated and exceeds max, adjust max
            if (minTotalMinutes > maxTotalMinutes) {
                maxHoursSpinner.setValue(minHours);
                maxMinutesSpinner.setValue(minMinutes);
            }
        } else {
            // If max was updated and is less than min, adjust min
            if (maxTotalMinutes < minTotalMinutes) {
                minHoursSpinner.setValue(maxHours);
                minMinutesSpinner.setValue(maxMinutes);
            }
        }
    }
    
    /**
     * Creates an IntervalCondition from the config panel.
     * This replaces the createTimeCondition method.
     */
    public static IntervalCondition createIntervalCondition(JPanel configPanel) {
        IntervalPickerPanel intervalPicker = (IntervalPickerPanel) configPanel.getClientProperty("intervalPicker");
        InitialDelayPanel initialDelayPanel = (InitialDelayPanel) configPanel.getClientProperty("initialDelayPanel");
        if (intervalPicker == null) {
            throw new IllegalStateException("Interval picker component not found");
        }

        // Get the interval condition from the picker component
        IntervalCondition baseCondition = intervalPicker.createIntervalCondition();

        // Check if initial delay should be added
        if (initialDelayPanel != null && initialDelayPanel.isInitialDelayEnabled()) {
            int delayHours = initialDelayPanel.getHours();
            int delayMinutes = initialDelayPanel.getMinutes();
            int delaySeconds = initialDelayPanel.getSeconds();
            int totalDelaySeconds = delayHours * 3600 + delayMinutes * 60 + delaySeconds;

            if (totalDelaySeconds > 0) {
                // Create a new condition with the same parameters as the base condition plus the delay
                if (baseCondition.isRandomize()) {
                    // For randomized intervals
                    return new IntervalCondition(
                        baseCondition.getInterval(), 
                        baseCondition.getMinInterval(), 
                        baseCondition.getMaxInterval(),
                        baseCondition.isRandomize(), 
                        baseCondition.getRandomFactor(), 
                        baseCondition.getMaximumNumberOfRepeats(),
                        (long)totalDelaySeconds
                    );
                } else {
                    // For fixed intervals
                    return new IntervalCondition(
                        baseCondition.getInterval(), 
                        baseCondition.isRandomize(), 
                        baseCondition.getRandomFactor(), 
                        baseCondition.getMaximumNumberOfRepeats(),
                        (long)totalDelaySeconds
                    );
                }
            }
        }

        return baseCondition;
    }
    
    public static void createTimeWindowConfigPanel(JPanel panel, GridBagConstraints gbc) {
        // Section Title
        JLabel titleLabel = new JLabel("Time Window Configuration:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // Date Range Configuration with Preset ComboBox
        gbc.gridy++;
        gbc.gridwidth = 1;
        JPanel dateRangeConfigPanel = createDateRangeConfigPanel();
        gbc.gridwidth = 2;
        panel.add(dateRangeConfigPanel, gbc);
        
        // Time Range Configuration with Preset ComboBox
        gbc.gridy++;
        JPanel timeRangeConfigPanel = createTimeRangeConfigPanel();
        panel.add(timeRangeConfigPanel, gbc);
        
        // Repeat and Randomization Panel (combined for compactness)
        gbc.gridy++;
        JPanel optionsPanel = createOptionsPanel();
        panel.add(optionsPanel, gbc);
        
        // Help text
        gbc.gridy++;
        JPanel helpPanel = createHelpPanel();
        panel.add(helpPanel, gbc);
        
        // Store components for later access
        DateRangePanel dateRangePanel = (DateRangePanel) dateRangeConfigPanel.getClientProperty("dateRangePanel");
        TimeRangePanel timeRangePanel = (TimeRangePanel) timeRangeConfigPanel.getClientProperty("timeRangePanel");
        @SuppressWarnings("unchecked")
        JComboBox<String> repeatComboBox = (JComboBox<String>) optionsPanel.getClientProperty("repeatComboBox");
        JSpinner intervalSpinner = (JSpinner) optionsPanel.getClientProperty("intervalSpinner");
        JCheckBox randomizeCheckBox = (JCheckBox) optionsPanel.getClientProperty("randomizeCheckBox");
        JSpinner randomizeSpinner = (JSpinner) optionsPanel.getClientProperty("randomizeSpinner");
        
        panel.putClientProperty("dateRangePanel", dateRangePanel); 
        panel.putClientProperty("timeRangePanel", timeRangePanel);
        panel.putClientProperty("repeatComboBox", repeatComboBox);
        panel.putClientProperty("intervalSpinner", intervalSpinner);
        panel.putClientProperty("randomizeCheckBox", randomizeCheckBox);
        panel.putClientProperty("randomizeSpinner", randomizeSpinner);
    }
    
    /**
     * Creates a compact date range configuration panel with preset ComboBox
     */
    private static JPanel createDateRangeConfigPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        mainPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), 
            "Date Range", 
            TitledBorder.LEFT, 
            TitledBorder.TOP, 
            FontManager.getRunescapeSmallFont(), 
            Color.WHITE));
        
        // Preset selection panel
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        presetPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel presetLabel = new JLabel("Preset:");
        presetLabel.setForeground(Color.WHITE);
        presetLabel.setFont(FontManager.getRunescapeSmallFont());
        presetPanel.add(presetLabel);
        
        String[] datePresets = {
            "Unlimited", "Today", "This Week", "This Month", 
            "Next 7 Days", "Next 30 Days", "Next 90 Days", "Custom"
        };
        JComboBox<String> datePresetCombo = new JComboBox<>(datePresets);
        datePresetCombo.setSelectedItem("Unlimited");
        datePresetCombo.setFont(FontManager.getRunescapeSmallFont());
        presetPanel.add(datePresetCombo);
        
        mainPanel.add(presetPanel, BorderLayout.NORTH);
        
        // Date range panel (initially hidden for preset selections)
        DateRangePanel dateRangePanel = new DateRangePanel(
            net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition.UNLIMITED_START_DATE,
            net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition.UNLIMITED_END_DATE
        );
        dateRangePanel.setVisible(false); // Hidden by default for "Unlimited"
        mainPanel.add(dateRangePanel, BorderLayout.CENTER);
        
        // Handle preset selection
        datePresetCombo.addActionListener(e -> {
            String selected = (String) datePresetCombo.getSelectedItem();
            LocalDate today = LocalDate.now();
            
            switch (selected) {
                case "Unlimited":
                    dateRangePanel.setStartDate(net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition.UNLIMITED_START_DATE);
                    dateRangePanel.setEndDate(net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition.UNLIMITED_END_DATE);
                    dateRangePanel.setVisible(false);
                    break;
                case "Today":
                    dateRangePanel.setStartDate(today);
                    dateRangePanel.setEndDate(today);
                    dateRangePanel.setVisible(true);
                    break;
                case "This Week":
                    dateRangePanel.setStartDate(today);
                    dateRangePanel.setEndDate(today.plusDays(7 - today.getDayOfWeek().getValue()));
                    dateRangePanel.setVisible(true);
                    break;
                case "This Month":
                    dateRangePanel.setStartDate(today);
                    dateRangePanel.setEndDate(today.withDayOfMonth(today.lengthOfMonth()));
                    dateRangePanel.setVisible(true);
                    break;
                case "Next 7 Days":
                    dateRangePanel.setStartDate(today);
                    dateRangePanel.setEndDate(today.plusDays(7));
                    dateRangePanel.setVisible(true);
                    break;
                case "Next 30 Days":
                    dateRangePanel.setStartDate(today);
                    dateRangePanel.setEndDate(today.plusDays(30));
                    dateRangePanel.setVisible(true);
                    break;
                case "Next 90 Days":
                    dateRangePanel.setStartDate(today);
                    dateRangePanel.setEndDate(today.plusDays(90));
                    dateRangePanel.setVisible(true);
                    break;
                case "Custom":
                    dateRangePanel.setVisible(true);
                    break;
            }
            mainPanel.revalidate();
            mainPanel.repaint();
        });
        
        mainPanel.putClientProperty("dateRangePanel", dateRangePanel);
        mainPanel.putClientProperty("datePresetCombo", datePresetCombo);
        return mainPanel;
    }
    
    /**
     * Creates a compact time range configuration panel with preset ComboBox
     */
    private static JPanel createTimeRangeConfigPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        mainPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), 
            "Time Range", 
            TitledBorder.LEFT, 
            TitledBorder.TOP, 
            FontManager.getRunescapeSmallFont(), 
            Color.WHITE));
        
        // Preset selection panel
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        presetPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel presetLabel = new JLabel("Preset:");
        presetLabel.setForeground(Color.WHITE);
        presetLabel.setFont(FontManager.getRunescapeSmallFont());
        presetPanel.add(presetLabel);
        
        String[] timePresets = {
            "All Day", "Business Hours", "Morning", "Afternoon", 
            "Evening", "Night", "Custom"
        };
        JComboBox<String> timePresetCombo = new JComboBox<>(timePresets);
        timePresetCombo.setSelectedItem("Business Hours");
        timePresetCombo.setFont(FontManager.getRunescapeSmallFont());
        presetPanel.add(timePresetCombo);
        
        mainPanel.add(presetPanel, BorderLayout.NORTH);
        
        // Time range panel (initially hidden for preset selections)
        TimeRangePanel timeRangePanel = new TimeRangePanel(LocalTime.of(9, 0), LocalTime.of(17, 0));
        timeRangePanel.setVisible(false); // Hidden by default for "Business Hours"
        mainPanel.add(timeRangePanel, BorderLayout.CENTER);
        
        // Handle preset selection
        timePresetCombo.addActionListener(e -> {
            String selected = (String) timePresetCombo.getSelectedItem();
            
            switch (selected) {
                case "All Day":
                    timeRangePanel.setStartTime(LocalTime.of(0, 0));
                    timeRangePanel.setEndTime(LocalTime.of(23, 59));
                    timeRangePanel.setVisible(false);
                    break;
                case "Business Hours":
                    timeRangePanel.setStartTime(LocalTime.of(9, 0));
                    timeRangePanel.setEndTime(LocalTime.of(17, 0));
                    timeRangePanel.setVisible(false);
                    break;
                case "Morning":
                    timeRangePanel.setStartTime(LocalTime.of(6, 0));
                    timeRangePanel.setEndTime(LocalTime.of(12, 0));
                    timeRangePanel.setVisible(false);
                    break;
                case "Afternoon":
                    timeRangePanel.setStartTime(LocalTime.of(12, 0));
                    timeRangePanel.setEndTime(LocalTime.of(18, 0));
                    timeRangePanel.setVisible(false);
                    break;
                case "Evening":
                    timeRangePanel.setStartTime(LocalTime.of(18, 0));
                    timeRangePanel.setEndTime(LocalTime.of(22, 0));
                    timeRangePanel.setVisible(false);
                    break;
                case "Night":
                    timeRangePanel.setStartTime(LocalTime.of(22, 0));
                    timeRangePanel.setEndTime(LocalTime.of(6, 0));
                    timeRangePanel.setVisible(false);
                    break;
                case "Custom":
                    timeRangePanel.setVisible(true);
                    break;
            }
            mainPanel.revalidate();
            mainPanel.repaint();
        });
        
        mainPanel.putClientProperty("timeRangePanel", timeRangePanel);
        mainPanel.putClientProperty("timePresetCombo", timePresetCombo);
        return mainPanel;
    }
    
    /**
     * Creates a compact options panel with repeat cycle and randomization controls
     */
    private static JPanel createOptionsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        mainPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), 
            "Options", 
            TitledBorder.LEFT, 
            TitledBorder.TOP, 
            FontManager.getRunescapeSmallFont(), 
            Color.WHITE));
        
        // Repeat options panel
        JPanel repeatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        repeatPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel repeatLabel = new JLabel("Repeat:");
        repeatLabel.setForeground(Color.WHITE);
        repeatLabel.setFont(FontManager.getRunescapeSmallFont());
        repeatPanel.add(repeatLabel);
        
        String[] repeatOptions = {"Every Day", "Every X Days", "Every X Hours", "Every X Minutes", "Every X Weeks", "One Time Only"};
        JComboBox<String> repeatComboBox = new JComboBox<>(repeatOptions);
        repeatComboBox.setFont(FontManager.getRunescapeSmallFont());
        repeatPanel.add(repeatComboBox);
        
        JLabel intervalLabel = new JLabel("Interval:");
        intervalLabel.setForeground(Color.WHITE);
        intervalLabel.setFont(FontManager.getRunescapeSmallFont());
        repeatPanel.add(intervalLabel);
        
        SpinnerNumberModel intervalModel = new SpinnerNumberModel(1, 1, 100, 1);
        JSpinner intervalSpinner = new JSpinner(intervalModel);
        intervalSpinner.setPreferredSize(new Dimension(60, intervalSpinner.getPreferredSize().height));
        intervalSpinner.setEnabled(false);
        repeatPanel.add(intervalSpinner);
        
        // Randomization options panel
        JPanel randomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        randomPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JCheckBox randomizeCheckBox = new JCheckBox("Randomize");
        randomizeCheckBox.setForeground(Color.WHITE);
        randomizeCheckBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        randomizeCheckBox.setFont(FontManager.getRunescapeSmallFont());
        randomPanel.add(randomizeCheckBox);
        
        JLabel randomizeAmountLabel = new JLabel("±");
        randomizeAmountLabel.setForeground(Color.WHITE);
        randomizeAmountLabel.setFont(FontManager.getRunescapeSmallFont());
        randomPanel.add(randomizeAmountLabel);
        
        SpinnerNumberModel randomizeModel = new SpinnerNumberModel(3, 1, 15, 1); // Default to 3 minutes, max 15 for default "Every Day"
        JSpinner randomizeSpinner = new JSpinner(randomizeModel);
        randomizeSpinner.setPreferredSize(new Dimension(50, randomizeSpinner.getPreferredSize().height));
        randomizeSpinner.setEnabled(false);
        randomizeSpinner.setToolTipText("<html>Randomization range: ±1 to ±15 minutes<br>Maximum is 40% of 1 days interval</html>");
        randomPanel.add(randomizeSpinner);
        
        JLabel minutesLabel = new JLabel("min");
        minutesLabel.setForeground(Color.WHITE);
        minutesLabel.setFont(FontManager.getRunescapeSmallFont());
        randomPanel.add(minutesLabel);
        
        // Control interactions
        repeatComboBox.addActionListener(e -> {
            String selected = (String) repeatComboBox.getSelectedItem();
            boolean enableInterval = !selected.equals("Every Day") && !selected.equals("One Time Only");
            intervalSpinner.setEnabled(enableInterval);
            
            // Update randomizer limits based on selected repeat cycle and interval
            updateRandomizerLimits(repeatComboBox, intervalSpinner, randomizeSpinner);
        });
        
        // Update randomizer limits when interval changes
        intervalSpinner.addChangeListener(e -> {
            updateRandomizerLimits(repeatComboBox, intervalSpinner, randomizeSpinner);
        });
        
        randomizeCheckBox.addActionListener(e -> 
            randomizeSpinner.setEnabled(randomizeCheckBox.isSelected())
        );
        
        // Set initial randomizer limits based on default selection
        SwingUtilities.invokeLater(() -> updateRandomizerLimits(repeatComboBox, intervalSpinner, randomizeSpinner));
        
        // Layout both panels
        JPanel combinedPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        combinedPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        combinedPanel.add(repeatPanel);
        combinedPanel.add(randomPanel);
        
        mainPanel.add(combinedPanel, BorderLayout.CENTER);
        
        // Store components for later access
        mainPanel.putClientProperty("repeatComboBox", repeatComboBox);
        mainPanel.putClientProperty("intervalSpinner", intervalSpinner);
        mainPanel.putClientProperty("randomizeCheckBox", randomizeCheckBox);
        mainPanel.putClientProperty("randomizeSpinner", randomizeSpinner);
        
        return mainPanel;
    }
    
    /**
     * Updates the randomizer spinner limits based on the current repeat cycle and interval
     */
    private static void updateRandomizerLimits(JComboBox<String> repeatComboBox, JSpinner intervalSpinner, JSpinner randomizeSpinner) {
        String selectedOption = (String) repeatComboBox.getSelectedItem();
        int interval = (Integer) intervalSpinner.getValue();
        
        // Map the selected option to RepeatCycle
        RepeatCycle repeatCycle;
        switch (selectedOption) {
            case "Every Day":
                repeatCycle = RepeatCycle.DAYS;
                interval = 1;
                break;
            case "Every X Days":
                repeatCycle = RepeatCycle.DAYS;
                break;
            case "Every X Hours":
                repeatCycle = RepeatCycle.HOURS;
                break;
            case "Every X Minutes":
                repeatCycle = RepeatCycle.MINUTES;
                break;
            case "Every X Weeks":
                repeatCycle = RepeatCycle.WEEKS;
                break;
            case "One Time Only":
                repeatCycle = RepeatCycle.ONE_TIME;
                break;
            default:
                repeatCycle = RepeatCycle.DAYS;
                interval = 1;
        }
        
        // Calculate the maximum allowed randomizer value
        int maxRandomizer = calculateMaxRandomizerValue(repeatCycle, interval);
        
        // Ensure minimum valid bounds for SpinnerNumberModel
        if (maxRandomizer < 1) {
            maxRandomizer = 1;
        }
        
        // Update the spinner model with new limits
        SpinnerNumberModel currentModel = (SpinnerNumberModel) randomizeSpinner.getModel();
        int currentValue = currentModel.getNumber().intValue();
        
        // Ensure current value is within valid bounds
        int validatedCurrentValue = Math.max(1, Math.min(currentValue, maxRandomizer));
        
        // Create new model with validated values
        SpinnerNumberModel newModel = new SpinnerNumberModel(
            validatedCurrentValue, // current value, validated to be within bounds
            1, // minimum
            maxRandomizer, // maximum (at least 1)
            1 // step
        );
        
        randomizeSpinner.setModel(newModel);
        
        // Update tooltip to show the reasoning
        randomizeSpinner.setToolTipText(String.format(
            "<html>Randomization range: ±1 to ±%d minutes<br>" +
            "Maximum is %d%% of %d %s interval</html>",
            maxRandomizer, 
            (int)(40), // 40% as we use in calculation
            interval,
            repeatCycle.toString().toLowerCase()
        ));
    }

    /**
     * Creates a help panel with useful information
     */
    private static JPanel createHelpPanel() {
        JPanel helpPanel = new JPanel();
        helpPanel.setLayout(new BoxLayout(helpPanel, BoxLayout.Y_AXIS));
        helpPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel descriptionLabel = new JLabel("Plugin will only run during the specified time window");
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel crossDayLabel = new JLabel("Note: If start time > end time, window crosses midnight");
        crossDayLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        crossDayLabel.setFont(FontManager.getRunescapeSmallFont());
        crossDayLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel timezoneLabel = new JLabel("Timezone: " + ZoneId.systemDefault().getId());
        timezoneLabel.setForeground(Color.YELLOW);
        timezoneLabel.setFont(FontManager.getRunescapeSmallFont());
        timezoneLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        helpPanel.add(descriptionLabel);
        helpPanel.add(Box.createVerticalStrut(2));
        helpPanel.add(crossDayLabel);
        helpPanel.add(Box.createVerticalStrut(2));
        helpPanel.add(timezoneLabel);
        
        return helpPanel;
    }

    public static TimeWindowCondition createTimeWindowCondition(JPanel configPanel) {
        DateRangePanel dateRangePanel = (DateRangePanel) configPanel.getClientProperty("dateRangePanel");
        TimeRangePanel timeRangePanel = (TimeRangePanel) configPanel.getClientProperty("timeRangePanel");
        @SuppressWarnings("unchecked")
        JComboBox<String> repeatComboBox = (JComboBox<String>) configPanel.getClientProperty("repeatComboBox");
        JSpinner intervalSpinner = (JSpinner) configPanel.getClientProperty("intervalSpinner");
        JCheckBox randomizeCheckBox = (JCheckBox) configPanel.getClientProperty("randomizeCheckBox");
        JSpinner randomizeSpinner = (JSpinner) configPanel.getClientProperty("randomizeSpinner");
        
        if (dateRangePanel == null || timeRangePanel == null) {
            throw new IllegalStateException("Time window configuration components not found");
        }
        
        // Get date values
        LocalDate startDate = dateRangePanel.getStartDate();
        LocalDate endDate = dateRangePanel.getEndDate();
        
        // Get time values 
        LocalTime startTime = timeRangePanel.getStartTime();
        LocalTime endTime = timeRangePanel.getEndTime();
        
        // Get repeat cycle configuration
        String repeatOption = (String) repeatComboBox.getSelectedItem();
        RepeatCycle repeatCycle;
        int interval = (Integer) intervalSpinner.getValue();
        long maximumNumberOfRepeats = 0; // Default to infinite repeats
        switch (repeatOption) {
            case "Every Day":
                repeatCycle = RepeatCycle.DAYS;
                interval = 1;
                break;
            case "Every X Days":
                repeatCycle = RepeatCycle.DAYS;
                break;
            case "Every X Hours":
                repeatCycle = RepeatCycle.HOURS;
                break;
            case "Every X Minutes":
                repeatCycle = RepeatCycle.MINUTES;
                break;
            case "Every X Weeks":
                repeatCycle = RepeatCycle.WEEKS;
                break;
            case "One Time Only":
                repeatCycle = RepeatCycle.ONE_TIME;
                interval = 1;
                maximumNumberOfRepeats = 1;
                break;
            default:
                repeatCycle = RepeatCycle.DAYS;
                interval = 1;
        }
        
        // Create the condition
        TimeWindowCondition condition = new TimeWindowCondition(
            startTime,
            endTime,
            startDate,
            endDate,
            repeatCycle,
            interval,
            maximumNumberOfRepeats
            
        );
        
        // Apply randomization if enabled
        if (randomizeCheckBox.isSelected()) {
            int randomizerValue = (Integer) randomizeSpinner.getValue();
            
            // Validate randomizer value against the interval to ensure it stays within reasonable bounds
            int maxAllowedRandomizer = calculateMaxRandomizerValue(repeatCycle, interval);
            if (randomizerValue > maxAllowedRandomizer) {
                log.warn("Randomizer value {} is too large for interval {} {}. Capping at {}", 
                    randomizerValue, interval, repeatCycle, maxAllowedRandomizer);
                randomizerValue = maxAllowedRandomizer;
                randomizeSpinner.setValue(maxAllowedRandomizer); // Update UI to reflect capped value
            }
            
            condition.setRandomization(true);
            condition.setRandomizerValue(randomizerValue);
            // Set the randomization unit based on the repeat cycle - for TimeWindow, randomization is always in minutes
            condition.setRandomizerValueUnit(RepeatCycle.MINUTES);
        }
        
        return condition;
    }


        /**
     * Creates a panel for configuring SingleTriggerTimeCondition
     * Uses the enhanced SingleDateTimePickerPanel component
     */
    public static void createSingleTriggerConfigPanel(JPanel panel, GridBagConstraints gbc) {
        // Section title
        JLabel titleLabel = new JLabel("One-Time Trigger Configuration:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(titleLabel, gbc);
        
        // Create the date/time picker panel
        gbc.gridy++;
        SingleDateTimePickerPanel dateTimePicker = new SingleDateTimePickerPanel();
        panel.add(dateTimePicker, gbc);                     
        // Description
        gbc.gridy++;
        JLabel descriptionLabel = new JLabel("Plugin will be triggered once at the specified date and time");
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        // Current timezone info
        gbc.gridy++;
        JLabel timezoneLabel = new JLabel("Current timezone: " + ZoneId.systemDefault().getId());
        timezoneLabel.setForeground(Color.YELLOW);
        timezoneLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(timezoneLabel, gbc);
        
        // Store components for later access
        panel.putClientProperty("dateTimePicker", dateTimePicker);        
        
    }
    /**
     * Creates a SingleTriggerTimeCondition from the config panel
     * Uses the enhanced SingleDateTimePickerPanel component
     */
    public static SingleTriggerTimeCondition createSingleTriggerCondition(JPanel configPanel) {
        SingleDateTimePickerPanel dateTimePicker = (SingleDateTimePickerPanel) configPanel.getClientProperty("dateTimePicker");        
        
        if (dateTimePicker == null) {
            log.error("Date time picker component not found in panel");
            return null;
        }
        
        // Get the selected date and time as LocalDateTime
        LocalDateTime selectedDateTime = dateTimePicker.getDateTime();
        
        // Convert to ZonedDateTime using the system default timezone
        ZonedDateTime triggerTime = selectedDateTime.atZone(ZoneId.systemDefault());
        
        // Create and return the condition
        return new SingleTriggerTimeCondition(triggerTime,Duration.ofSeconds(0),1);
    }
    public static void createDayOfWeekConfigPanel(JPanel panel, GridBagConstraints gbc) {
        // Title and initial setup
        JLabel titleLabel = new JLabel("Day of Week Configuration:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        panel.add(titleLabel, gbc);

        // Preset options
        gbc.gridy++;
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        presetPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JButton weekdaysButton = new JButton("Weekdays");
        weekdaysButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        weekdaysButton.setForeground(Color.WHITE);
        
        JButton weekendsButton = new JButton("Weekends");
        weekendsButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        weekendsButton.setForeground(Color.WHITE);
        
        JButton allDaysButton = new JButton("All Days");
        allDaysButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        allDaysButton.setForeground(Color.WHITE);
        
        presetPanel.add(weekdaysButton);
        presetPanel.add(weekendsButton);
        presetPanel.add(allDaysButton);
        
        panel.add(presetPanel, gbc);
        
        // Day checkboxes
        gbc.gridy++;
        JPanel daysPanel = new JPanel(new GridLayout(0, 3));
        daysPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        JCheckBox[] dayCheckboxes = new JCheckBox[7];
        
        for (int i = 0; i < dayNames.length; i++) {
            dayCheckboxes[i] = new JCheckBox(dayNames[i]);
            dayCheckboxes[i].setBackground(ColorScheme.DARKER_GRAY_COLOR);
            dayCheckboxes[i].setForeground(Color.WHITE);
            daysPanel.add(dayCheckboxes[i]);
        }
        
        // Set up weekdays button
        weekdaysButton.addActionListener(e -> {
            for (int i = 0; i < 5; i++) {
                dayCheckboxes[i].setSelected(true);
            }
            dayCheckboxes[5].setSelected(false);
            dayCheckboxes[6].setSelected(false);
        });
        
        // Set up weekends button
        weekendsButton.addActionListener(e -> {
            for (int i = 0; i < 5; i++) {
                dayCheckboxes[i].setSelected(false);
            }
            dayCheckboxes[5].setSelected(true);
            dayCheckboxes[6].setSelected(true);
        });
        
        // Set up all days button
        allDaysButton.addActionListener(e -> {
            for (JCheckBox checkbox : dayCheckboxes) {
                checkbox.setSelected(true);
            }
        });
        
        panel.add(daysPanel, gbc);
        
        // Add usage limits panel
        gbc.gridy++;
        JPanel usageLimitsPanel = new JPanel();
        usageLimitsPanel.setLayout(new BoxLayout(usageLimitsPanel, BoxLayout.Y_AXIS));
        usageLimitsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Daily limit panel
        JPanel dailyLimitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dailyLimitPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel dailyLimitLabel = new JLabel("Max repeats per day:");
        dailyLimitLabel.setForeground(Color.WHITE);
        dailyLimitPanel.add(dailyLimitLabel);
        
        SpinnerNumberModel dailyLimitModel = new SpinnerNumberModel(0, 0, 100, 1);
        JSpinner dailyLimitSpinner = new JSpinner(dailyLimitModel);
        dailyLimitSpinner.setPreferredSize(new Dimension(70, dailyLimitSpinner.getPreferredSize().height));
        dailyLimitPanel.add(dailyLimitSpinner);
        
        JLabel dailyUnlimitedLabel = new JLabel("(0 = unlimited)");
        dailyUnlimitedLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        dailyUnlimitedLabel.setFont(FontManager.getRunescapeSmallFont());
        dailyLimitPanel.add(dailyUnlimitedLabel);
        
        usageLimitsPanel.add(dailyLimitPanel);
        
        // Weekly limit panel
        JPanel weeklyLimitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        weeklyLimitPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel weeklyLimitLabel = new JLabel("Max repeats per week:");
        weeklyLimitLabel.setForeground(Color.WHITE);
        weeklyLimitPanel.add(weeklyLimitLabel);
        
        SpinnerNumberModel weeklyLimitModel = new SpinnerNumberModel(0, 0, 100, 1);
        JSpinner weeklyLimitSpinner = new JSpinner(weeklyLimitModel);
        weeklyLimitSpinner.setPreferredSize(new Dimension(70, weeklyLimitSpinner.getPreferredSize().height));
        weeklyLimitPanel.add(weeklyLimitSpinner);
        
        JLabel weeklyUnlimitedLabel = new JLabel("(0 = unlimited)");
        weeklyUnlimitedLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        weeklyUnlimitedLabel.setFont(FontManager.getRunescapeSmallFont());
        weeklyLimitPanel.add(weeklyUnlimitedLabel);
        
        usageLimitsPanel.add(weeklyLimitPanel);
        
        panel.add(usageLimitsPanel, gbc);
        
        // Add interval configuration using the reusable IntervalPickerPanel
        gbc.gridy++;
        JPanel intervalOptionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        intervalOptionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JCheckBox useIntervalCheckBox = new JCheckBox("Use interval between triggers");
        useIntervalCheckBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        useIntervalCheckBox.setForeground(Color.WHITE);
        intervalOptionPanel.add(useIntervalCheckBox);
        
        panel.add(intervalOptionPanel, gbc);
        
        // Add the interval picker panel (initially disabled)
        gbc.gridy++;
        IntervalPickerPanel intervalPicker = new IntervalPickerPanel(false); // No presets needed
        intervalPicker.setEnabled(false);
        panel.add(intervalPicker, gbc);
        
        // Toggle interval picker based on checkbox
        useIntervalCheckBox.addActionListener(e -> {
            boolean useInterval = useIntervalCheckBox.isSelected();
            intervalPicker.setEnabled(useInterval);
        });
        
        // Description
        gbc.gridy++;
        JLabel descriptionLabel = new JLabel("Plugin will only run on selected days of the week");
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        // Add limits description
        gbc.gridy++;
        JLabel limitsLabel = new JLabel("Daily/weekly limits prevent excessive usage");
        limitsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        limitsLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(limitsLabel, gbc);
        
        // Add interval description
        gbc.gridy++;
        JLabel intervalDescLabel = new JLabel("Intervals control time between triggers on the same day");
        intervalDescLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        intervalDescLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(intervalDescLabel, gbc);
        
        // Store components for later access
        panel.putClientProperty("dayCheckboxes", dayCheckboxes);
        panel.putClientProperty("dailyLimitSpinner", dailyLimitSpinner);
        panel.putClientProperty("weeklyLimitSpinner", weeklyLimitSpinner);
        panel.putClientProperty("useIntervalCheckBox", useIntervalCheckBox);
        panel.putClientProperty("intervalPicker", intervalPicker);
}
public static DayOfWeekCondition createDayOfWeekCondition(JPanel configPanel) {
    JCheckBox[] dayCheckboxes = (JCheckBox[]) configPanel.getClientProperty("dayCheckboxes");
    JSpinner dailyLimitSpinner = (JSpinner) configPanel.getClientProperty("dailyLimitSpinner");
    JSpinner weeklyLimitSpinner = (JSpinner) configPanel.getClientProperty("weeklyLimitSpinner");
    JCheckBox useIntervalCheckBox = (JCheckBox) configPanel.getClientProperty("useIntervalCheckBox");
    IntervalPickerPanel intervalPicker = (IntervalPickerPanel) configPanel.getClientProperty("intervalPicker");

    if (dayCheckboxes == null) {
        throw new IllegalStateException("Day of week configuration components not found");
    }
    
    // Collect the selected days
    Set<DayOfWeek> activeDays = EnumSet.noneOf(DayOfWeek.class);
    if (dayCheckboxes[0].isSelected()) activeDays.add(DayOfWeek.MONDAY);
    if (dayCheckboxes[1].isSelected()) activeDays.add(DayOfWeek.TUESDAY);
    if (dayCheckboxes[2].isSelected()) activeDays.add(DayOfWeek.WEDNESDAY);
    if (dayCheckboxes[3].isSelected()) activeDays.add(DayOfWeek.THURSDAY);
    if (dayCheckboxes[4].isSelected()) activeDays.add(DayOfWeek.FRIDAY);
    if (dayCheckboxes[5].isSelected()) activeDays.add(DayOfWeek.SATURDAY);
    if (dayCheckboxes[6].isSelected()) activeDays.add(DayOfWeek.SUNDAY);
    
    // If no days selected, default to all days
    if (activeDays.isEmpty()) {
        activeDays.add(DayOfWeek.MONDAY);
        activeDays.add(DayOfWeek.TUESDAY);
        activeDays.add(DayOfWeek.WEDNESDAY);
        activeDays.add(DayOfWeek.THURSDAY);
        activeDays.add(DayOfWeek.FRIDAY);
        activeDays.add(DayOfWeek.SATURDAY);
        activeDays.add(DayOfWeek.SUNDAY);
    }
    
    // Get daily and weekly limits
    long maxRepeatsPerDay = dailyLimitSpinner != null ? (Integer) dailyLimitSpinner.getValue() : 0;
    long maxRepeatsPerWeek = weeklyLimitSpinner != null ? (Integer) weeklyLimitSpinner.getValue() : 0;
    
    // Create the base condition with appropriate limits
    DayOfWeekCondition condition = new DayOfWeekCondition(0, maxRepeatsPerDay, maxRepeatsPerWeek, activeDays);
    
    // If using interval, add interval condition from the interval picker
    if (useIntervalCheckBox != null && useIntervalCheckBox.isSelected() && intervalPicker != null) {
        IntervalCondition intervalCondition = intervalPicker.createIntervalCondition();
        condition.setIntervalCondition(intervalCondition);
    }
    
    return condition;
}

    
    /**
     * Sets up the panel with values from an existing time condition
     * 
     * @param panel The panel containing the UI components
     * @param condition The time condition to read values from
     */
    public static void setupTimeCondition(JPanel panel, Condition condition) {
        if (condition == null) {
            return;
        }
        
        if (condition instanceof IntervalCondition) {
            setupIntervalCondition(panel, (IntervalCondition) condition);
        } else if (condition instanceof TimeWindowCondition) {
            setupTimeWindowCondition(panel, (TimeWindowCondition) condition);
        } else if (condition instanceof DayOfWeekCondition) {
            setupDayOfWeekCondition(panel, (DayOfWeekCondition) condition);
        } else if (condition instanceof SingleTriggerTimeCondition) {
            setupSingleTriggerCondition(panel, (SingleTriggerTimeCondition) condition);
        }
    }


    /**
     * Sets up the interval condition panel with values from an existing condition
     */
    private static void setupIntervalCondition(JPanel panel, IntervalCondition condition) {
        // Get the IntervalPickerPanel component which encapsulates all the interval UI controls
        IntervalPickerPanel intervalPicker = (IntervalPickerPanel) panel.getClientProperty("intervalPicker");
        InitialDelayPanel initialDelayPanel = (InitialDelayPanel) panel.getClientProperty("initialDelayPanel");
        if (intervalPicker == null) {
            log.error("IntervalPickerPanel component not found for interval condition setup");
            return; // Missing UI components
        }

        // Use the IntervalPickerPanel's built-in method to configure itself from the condition
        intervalPicker.setIntervalCondition(condition);

        // Set initial delay if it exists
        if (condition.getInitialDelayCondition() != null && initialDelayPanel != null) {
            Duration definedDelay = condition.getInitialDelayCondition().getDefinedDelay();
            if (definedDelay != null && definedDelay.getSeconds() > 0) {
                long definedSeconds = definedDelay.getSeconds();

                // Set the delay time in the UI
                int hours = (int) (definedSeconds / 3600);
                int minutes = (int) ((definedSeconds % 3600) / 60);
                int seconds = (int) (definedSeconds % 60);

                initialDelayPanel.setEnabled(true);
                initialDelayPanel.getHoursSpinner().setValue(hours);
                initialDelayPanel.getMinutesSpinner().setValue(minutes);
                initialDelayPanel.getSecondsSpinner().setValue(seconds);
            }
        }
    }

    /**
     * Sets up the time window condition panel with values from an existing condition
     */
    private static void setupTimeWindowCondition(JPanel panel, TimeWindowCondition condition) {
        // Get custom components from client properties
        DateRangePanel dateRangePanel = (DateRangePanel) panel.getClientProperty("dateRangePanel");
        TimeRangePanel timeRangePanel = (TimeRangePanel) panel.getClientProperty("timeRangePanel");
        @SuppressWarnings("unchecked")
        JComboBox<String> repeatComboBox = (JComboBox<String>) panel.getClientProperty("repeatComboBox");
        JSpinner intervalSpinner = (JSpinner) panel.getClientProperty("intervalSpinner");
        JCheckBox randomizeCheckBox = (JCheckBox) panel.getClientProperty("randomizeCheckBox");
        JSpinner randomizeSpinner = (JSpinner) panel.getClientProperty("randomizeSpinner");
        
        // Find and update date preset ComboBox
        updateDatePresetFromCondition(panel, condition);
        
        // Find and update time preset ComboBox
        updateTimePresetFromCondition(panel, condition);
        
        // Set date range
        if (dateRangePanel != null) {
            if (condition.getStartDate() != null) {
                dateRangePanel.setStartDate(condition.getStartDate());
            }
            if (condition.getEndDate() != null) {
                dateRangePanel.setEndDate(condition.getEndDate());
            }
        }
        
        // Set time range
        if (timeRangePanel != null) {
            timeRangePanel.setStartTime(condition.getStartTime());
            timeRangePanel.setEndTime(condition.getEndTime());
        }
        
        // Set repeat cycle
        if (repeatComboBox != null) {
            RepeatCycle cycle = condition.getRepeatCycle();
            int interval = condition.getRepeatIntervalUnit();
            
            // Map RepeatCycle enum to combo box options
            switch (cycle) {
                case DAYS:
                    repeatComboBox.setSelectedItem(interval == 1 ? "Every Day" : "Every X Days");
                    break;
                case HOURS:
                    repeatComboBox.setSelectedItem("Every X Hours");
                    break;
                case MINUTES:
                    repeatComboBox.setSelectedItem("Every X Minutes");
                    break;
                case WEEKS:
                    repeatComboBox.setSelectedItem("Every X Weeks");
                    break;
                case ONE_TIME:
                    repeatComboBox.setSelectedItem("One Time Only");
                    break;
                case SECONDS:
                case MILLIS:
                default:
                    // Fallback for unsupported cycles
                    repeatComboBox.setSelectedItem("Every Day");
                    break;
            }
            
            // Set interval value
            if (intervalSpinner != null) {
                intervalSpinner.setValue(interval);
                intervalSpinner.setEnabled(!cycle.equals(RepeatCycle.DAYS) || interval != 1);
            }
        }
        
        // Set randomization
        if (randomizeCheckBox != null && randomizeSpinner != null) {
            randomizeCheckBox.setSelected(condition.isUseRandomization());
            randomizeSpinner.setEnabled(condition.isUseRandomization());
            
            // Validate and set randomizer value
            int savedRandomizerValue = condition.getRandomizerValue();
            if (savedRandomizerValue > 0) {
                // Calculate max allowed for this condition's settings
                int maxAllowedRandomizer = calculateMaxRandomizerValue(condition.getRepeatCycle(), condition.getRepeatIntervalUnit());
                int validatedValue = Math.max(1, Math.min(savedRandomizerValue, maxAllowedRandomizer));
                
                // Ensure maximum is at least 1
                maxAllowedRandomizer = Math.max(1, maxAllowedRandomizer);
                
                // Update spinner model with proper limits
                SpinnerNumberModel newModel = new SpinnerNumberModel(
                    validatedValue, // current value, validated
                    1, // minimum
                    maxAllowedRandomizer, // maximum
                    1 // step
                );
                randomizeSpinner.setModel(newModel);
                
                if (validatedValue != savedRandomizerValue) {
                    log.warn("Randomizer value {} was too large for {}x{} interval. Capped at {}", 
                        savedRandomizerValue, condition.getRepeatIntervalUnit(), condition.getRepeatCycle(), validatedValue);
                }
            } else {
                // Set default value if no randomization value is set
                int maxAllowedRandomizer = calculateMaxRandomizerValue(condition.getRepeatCycle(), condition.getRepeatIntervalUnit());
                int defaultValue = Math.max(1, Math.min(3, maxAllowedRandomizer));
                
                // Ensure maximum is at least 1
                maxAllowedRandomizer = Math.max(1, maxAllowedRandomizer);
                
                // Update spinner model with proper limits  
                SpinnerNumberModel newModel = new SpinnerNumberModel(
                    defaultValue, // default value
                    1, // minimum
                    maxAllowedRandomizer, // maximum
                    1 // step
                );
                randomizeSpinner.setModel(newModel);
            }
        }
    }
    
    /**
     * Updates the date preset ComboBox based on the condition's date range
     */
    private static void updateDatePresetFromCondition(JPanel panel, TimeWindowCondition condition) {
        // Try to find the date preset ComboBox in the panel hierarchy
        JComboBox<String> datePresetCombo = findDatePresetComboBox(panel);
        if (datePresetCombo == null) return;
        
        LocalDate startDate = condition.getStartDate();
        LocalDate endDate = condition.getEndDate();
        LocalDate today = LocalDate.now();
        
        // Check if it matches any preset
        if (condition.hasUnlimitedDateRange()) {
            datePresetCombo.setSelectedItem("Unlimited");
        } else if (startDate.equals(today) && endDate.equals(today)) {
            datePresetCombo.setSelectedItem("Today");
        } else if (startDate.equals(today) && endDate.equals(today.plusDays(7 - today.getDayOfWeek().getValue()))) {
            datePresetCombo.setSelectedItem("This Week");
        } else if (startDate.equals(today) && endDate.equals(today.withDayOfMonth(today.lengthOfMonth()))) {
            datePresetCombo.setSelectedItem("This Month");
        } else if (startDate.equals(today) && endDate.equals(today.plusDays(7))) {
            datePresetCombo.setSelectedItem("Next 7 Days");
        } else if (startDate.equals(today) && endDate.equals(today.plusDays(30))) {
            datePresetCombo.setSelectedItem("Next 30 Days");
        } else if (startDate.equals(today) && endDate.equals(today.plusDays(90))) {
            datePresetCombo.setSelectedItem("Next 90 Days");
        } else {
            datePresetCombo.setSelectedItem("Custom");
        }
    }
    
    /**
     * Updates the time preset ComboBox based on the condition's time range
     */
    private static void updateTimePresetFromCondition(JPanel panel, TimeWindowCondition condition) {
        // Try to find the time preset ComboBox in the panel hierarchy
        JComboBox<String> timePresetCombo = findTimePresetComboBox(panel);
        if (timePresetCombo == null) return;
        
        LocalTime startTime = condition.getStartTime();
        LocalTime endTime = condition.getEndTime();
        
        // Check if it matches any preset
        if (startTime.equals(LocalTime.of(0, 0)) && endTime.equals(LocalTime.of(23, 59))) {
            timePresetCombo.setSelectedItem("All Day");
        } else if (startTime.equals(LocalTime.of(9, 0)) && endTime.equals(LocalTime.of(17, 0))) {
            timePresetCombo.setSelectedItem("Business Hours");
        } else if (startTime.equals(LocalTime.of(6, 0)) && endTime.equals(LocalTime.of(12, 0))) {
            timePresetCombo.setSelectedItem("Morning");
        } else if (startTime.equals(LocalTime.of(12, 0)) && endTime.equals(LocalTime.of(18, 0))) {
            timePresetCombo.setSelectedItem("Afternoon");
        } else if (startTime.equals(LocalTime.of(18, 0)) && endTime.equals(LocalTime.of(22, 0))) {
            timePresetCombo.setSelectedItem("Evening");
        } else if (startTime.equals(LocalTime.of(22, 0)) && endTime.equals(LocalTime.of(6, 0))) {
            timePresetCombo.setSelectedItem("Night");
        } else {
            timePresetCombo.setSelectedItem("Custom");
        }
    }
    
    /**
     * Recursively searches for the date preset ComboBox in the panel hierarchy
     */
    private static JComboBox<String> findDatePresetComboBox(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                Object datePresetCombo = panel.getClientProperty("datePresetCombo");
                if (datePresetCombo instanceof JComboBox) {
                    @SuppressWarnings("unchecked")
                    JComboBox<String> comboBox = (JComboBox<String>) datePresetCombo;
                    return comboBox;
                }
                // Recursively search in child panels
                JComboBox<String> found = findDatePresetComboBox(panel);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    /**
     * Recursively searches for the time preset ComboBox in the panel hierarchy
     */
    private static JComboBox<String> findTimePresetComboBox(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                Object timePresetCombo = panel.getClientProperty("timePresetCombo");
                if (timePresetCombo instanceof JComboBox) {
                    @SuppressWarnings("unchecked")
                    JComboBox<String> comboBox = (JComboBox<String>) timePresetCombo;
                    return comboBox;
                }
                // Recursively search in child panels
                JComboBox<String> found = findTimePresetComboBox(panel);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Sets up the day of week condition panel with values from an existing condition
     */
    private static void setupDayOfWeekCondition(JPanel panel, DayOfWeekCondition condition) {
        JCheckBox[] dayCheckboxes = (JCheckBox[]) panel.getClientProperty("dayCheckboxes");
        JSpinner dailyLimitSpinner = (JSpinner) panel.getClientProperty("dailyLimitSpinner");
        JSpinner weeklyLimitSpinner = (JSpinner) panel.getClientProperty("weeklyLimitSpinner");
        JCheckBox useIntervalCheckBox = (JCheckBox) panel.getClientProperty("useIntervalCheckBox");
        IntervalPickerPanel intervalPicker = (IntervalPickerPanel) panel.getClientProperty("intervalPicker");
        
        if (dayCheckboxes != null) {
            Set<DayOfWeek> activeDays = condition.getActiveDays();
            
            // Map DayOfWeek enum values to checkbox indices (0 = Monday)
            if (activeDays.contains(DayOfWeek.MONDAY)) dayCheckboxes[0].setSelected(true);
            if (activeDays.contains(DayOfWeek.TUESDAY)) dayCheckboxes[1].setSelected(true);
            if (activeDays.contains(DayOfWeek.WEDNESDAY)) dayCheckboxes[2].setSelected(true);
            if (activeDays.contains(DayOfWeek.THURSDAY)) dayCheckboxes[3].setSelected(true);
            if (activeDays.contains(DayOfWeek.FRIDAY)) dayCheckboxes[4].setSelected(true);
            if (activeDays.contains(DayOfWeek.SATURDAY)) dayCheckboxes[5].setSelected(true);
            if (activeDays.contains(DayOfWeek.SUNDAY)) dayCheckboxes[6].setSelected(true);
        }
        
        // Set daily and weekly limits
        if (dailyLimitSpinner != null) {
            dailyLimitSpinner.setValue((int)condition.getMaxRepeatsPerDay());
        }
        
        if (weeklyLimitSpinner != null) {
            weeklyLimitSpinner.setValue((int)condition.getMaxRepeatsPerWeek());
        }
        
        // Handle interval condition if present
        Optional<IntervalCondition> intervalConditionOpt = condition.getIntervalCondition();
        if (intervalConditionOpt.isPresent() && useIntervalCheckBox != null && intervalPicker != null) {
            // Enable the interval checkbox
            useIntervalCheckBox.setSelected(true);
            intervalPicker.setEnabled(true);
            
            // Configure the interval picker with the condition
            intervalPicker.setIntervalCondition(intervalConditionOpt.get());
        }
        
        // Refresh panel layout
        panel.revalidate();
        panel.repaint();
    }

    /**
     * Sets up the single trigger condition panel with values from an existing condition
     */
    private static void setupSingleTriggerCondition(JPanel panel, SingleTriggerTimeCondition condition) {
        SingleDateTimePickerPanel dateTimePicker = (SingleDateTimePickerPanel) panel.getClientProperty("dateTimePicker");        
        if (dateTimePicker != null) {
            // Convert ZonedDateTime to LocalDateTime
            dateTimePicker.setDateTime(condition.getNextTriggerTimeWithPause().get().toLocalDateTime());
        }
        
    }
    
    /**
     * Calculates the maximum allowed randomizer value for a given repeat cycle and interval.
     * Ensures randomization doesn't exceed 40% of the total interval to prevent overlapping.
     * 
     * @param repeatCycle The repeat cycle (MINUTES, HOURS, DAYS, etc.)
     * @param interval The interval value
     * @return Maximum allowed randomizer value in minutes
     */
    private static int calculateMaxRandomizerValue(RepeatCycle repeatCycle, int interval) {
        int totalIntervalMinutes;
        
        switch (repeatCycle) {
            case MINUTES:
                totalIntervalMinutes = interval;
                break;
            case HOURS:
                totalIntervalMinutes = interval * 60;
                break;
            case DAYS:
                totalIntervalMinutes = interval * 24 * 60;
                break;
            case WEEKS:
                totalIntervalMinutes = interval * 7 * 24 * 60;
                break;
            case ONE_TIME:
                return 60; // For one-time, allow up to 1 hour randomization
            default:
                return 15; // Default fallback
        }
        
        // Allow randomization up to 40% of the total interval, but cap at reasonable limits
        int maxRandomizer = (int) (totalIntervalMinutes * 0.4);
        
        // Apply caps based on interval type
        int result;
        switch (repeatCycle) {
            case MINUTES:
                // For minute intervals, cap at half the interval or 30 minutes, whichever is smaller
                result = Math.min(maxRandomizer, Math.min(interval / 2, 30));
                break;
            case HOURS:
                // For hour intervals, cap at 2 hours
                result = Math.min(maxRandomizer, 120);
                break;
            case DAYS:
                // For day intervals, cap at 12 hours
                result = Math.min(maxRandomizer, 720);
                break;
            case WEEKS:
                // For week intervals, cap at 2 days
                result = Math.min(maxRandomizer, 2880);
                break;
            default:
                result = Math.min(maxRandomizer, 60);
                break;
        }
        
        // Ensure result is at least 1 to avoid invalid SpinnerNumberModel creation
        return Math.max(1, result);
    }
}
