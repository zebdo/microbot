package net.runelite.client.plugins.microbot.pluginscheduler.ui.condition;

import java.time.ZoneId;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.runelite.client.plugins.microbot.pluginscheduler.ui.components.DateRangePanel;
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Calendar;
import java.util.Date;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.resource.BankItemCountCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.resource.InventoryItemCountCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.resource.LootItemCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.skill.SkillLevelCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.skill.SkillXpCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.DayOfWeekCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.IntervalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeWindowCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.enums.RepeatCycle;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.SingleTriggerTimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.NotCondition;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
public class TimeConditionPanelUtil {
    public static void createTimeConfigPanel(JPanel panel, GridBagConstraints gbc, JPanel configPanel) {
        // Title label
        JLabel titleLabel = new JLabel("Time Duration:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // Create a panel for duration inputs
        gbc.gridy++;
        JPanel durationPanel = new JPanel();
        durationPanel.setLayout(new BoxLayout(durationPanel, BoxLayout.Y_AXIS));
        durationPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Hours and minutes panel
        JPanel timeInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timeInputPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Hours spinner
        JLabel hoursLabel = new JLabel("Hours:");
        hoursLabel.setForeground(Color.WHITE);
        timeInputPanel.add(hoursLabel);
        
        SpinnerNumberModel hoursModel = new SpinnerNumberModel(1, 0, 99, 1);
        JSpinner hoursSpinner = new JSpinner(hoursModel);
        hoursSpinner.setPreferredSize(new Dimension(70, hoursSpinner.getPreferredSize().height));
        timeInputPanel.add(hoursSpinner);
        
        // Minutes spinner
        JLabel minutesLabel = new JLabel("Minutes:");
        minutesLabel.setForeground(Color.WHITE);
        timeInputPanel.add(minutesLabel);
        
        SpinnerNumberModel minutesModel = new SpinnerNumberModel(0, 0, 59, 5);
        JSpinner minutesSpinner = new JSpinner(minutesModel);
        minutesSpinner.setPreferredSize(new Dimension(70, minutesSpinner.getPreferredSize().height));
        timeInputPanel.add(minutesSpinner);
        
        durationPanel.add(timeInputPanel);
        
        // Separator
        durationPanel.add(Box.createVerticalStrut(10));
        
        // Min/max time panel for randomization
        JPanel minMaxPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        minMaxPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        minMaxPanel.add(new JLabel("Min Hours:"));
        SpinnerNumberModel minHoursModel = new SpinnerNumberModel(1, 0, 99, 1);
        JSpinner minHoursSpinner = new JSpinner(minHoursModel);
        minMaxPanel.add(minHoursSpinner);
        
        minMaxPanel.add(new JLabel("Min Minutes:"));
        SpinnerNumberModel minMinutesModel = new SpinnerNumberModel(0, 0, 59, 5);
        JSpinner minMinutesSpinner = new JSpinner(minMinutesModel);
        minMaxPanel.add(minMinutesSpinner);
        
        minMaxPanel.add(new JLabel("Max Hours:"));
        SpinnerNumberModel maxHoursModel = new SpinnerNumberModel(2, 0, 99, 1);
        JSpinner maxHoursSpinner = new JSpinner(maxHoursModel);
        minMaxPanel.add(maxHoursSpinner);
        
        minMaxPanel.add(new JLabel("Max Minutes:"));
        SpinnerNumberModel maxMinutesModel = new SpinnerNumberModel(30, 0, 59, 5);
        JSpinner maxMinutesSpinner = new JSpinner(maxMinutesModel);
        minMaxPanel.add(maxMinutesSpinner);
        
        // Initially hide min/max panel
        minMaxPanel.setVisible(false);
        durationPanel.add(minMaxPanel);
        
        // Preset durations
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        presetPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel presetsLabel = new JLabel("Presets:");
        presetsLabel.setForeground(Color.WHITE);
        presetPanel.add(presetsLabel);
        
        String[][] presets = {
            {"30m", "0", "30"},
            {"1h", "1", "0"},
            {"1h30m", "1", "30"},
            {"2h", "2", "0"},
            {"3h", "3", "0"},
            {"4h", "4", "0"}
        };
        
        for (String[] preset : presets) {
            JButton presetButton = new JButton(preset[0]);
            presetButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
            presetButton.setForeground(Color.WHITE);
            presetButton.setFocusPainted(false);
            presetButton.addActionListener(e -> {
                hoursSpinner.setValue(Integer.parseInt(preset[1]));
                minutesSpinner.setValue(Integer.parseInt(preset[2]));
            });
            presetPanel.add(presetButton);
        }
        
        durationPanel.add(Box.createVerticalStrut(10));
        durationPanel.add(presetPanel);
        
        // Randomization options
        durationPanel.add(Box.createVerticalStrut(10));
        
        JPanel randomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        randomPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JCheckBox randomizeCheckBox = new JCheckBox("Randomize within range");
        randomizeCheckBox.setForeground(Color.WHITE);
        randomizeCheckBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        randomizeCheckBox.setSelected(true);
        randomPanel.add(randomizeCheckBox);
        
        JLabel randomFactorLabel = new JLabel("Random Factor:");
        randomFactorLabel.setForeground(Color.WHITE);
        randomPanel.add(randomFactorLabel);
        
        SpinnerNumberModel randomFactorModel = new SpinnerNumberModel(0.3, 0.0, 1.0, 0.05);
        JSpinner randomFactorSpinner = new JSpinner(randomFactorModel);
        randomFactorSpinner.setPreferredSize(new Dimension(70, randomFactorSpinner.getPreferredSize().height));
        randomPanel.add(randomFactorSpinner);
        
        durationPanel.add(randomPanel);
        
        // Add the main panel to the container
        panel.add(durationPanel, gbc);
        
        // Toggle between fixed duration and min/max range
        randomizeCheckBox.addChangeListener(e -> {
            boolean useRange = randomizeCheckBox.isSelected();
            timeInputPanel.setVisible(!useRange);
            minMaxPanel.setVisible(useRange);
            
            if (useRange) {
                // Set min/max values based on current duration
                int hours = (Integer) hoursSpinner.getValue();
                int minutes = (Integer) minutesSpinner.getValue();
                
                minHoursSpinner.setValue(Math.max(0, hours - 1));
                minMinutesSpinner.setValue(minutes);
                
                maxHoursSpinner.setValue(hours + 1);
                maxMinutesSpinner.setValue(minutes);
            }
            
            // Update parent container layout
            panel.revalidate();
            panel.repaint();
        });
        
        // Add a helpful description
        gbc.gridy++;
        JLabel descriptionLabel = new JLabel("Plugin will stop after the specified duration");
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        // Store components for later access when creating conditions
        configPanel.putClientProperty("hoursSpinner", hoursSpinner);
        configPanel.putClientProperty("minutesSpinner", minutesSpinner);
        configPanel.putClientProperty("minHoursSpinner", minHoursSpinner);
        configPanel.putClientProperty("minMinutesSpinner", minMinutesSpinner);
        configPanel.putClientProperty("maxHoursSpinner", maxHoursSpinner);
        configPanel.putClientProperty("maxMinutesSpinner", maxMinutesSpinner);
        configPanel.putClientProperty("randomizeCheckBox", randomizeCheckBox);
        configPanel.putClientProperty("randomFactorSpinner", randomFactorSpinner);
    }
    public static IntervalCondition createTimeCondition(JPanel configPanel) {
        JCheckBox randomizeCheckBox = (JCheckBox) configPanel.getClientProperty("randomizeCheckBox");
        
        if (randomizeCheckBox == null) {
            throw new IllegalStateException("Time configuration components not found");
        }
        
        if (randomizeCheckBox.isSelected()) {
            // Use min/max range
            JSpinner minHoursSpinner = (JSpinner) configPanel.getClientProperty("minHoursSpinner");
            JSpinner minMinutesSpinner = (JSpinner) configPanel.getClientProperty("minMinutesSpinner");
            JSpinner maxHoursSpinner = (JSpinner) configPanel.getClientProperty("maxHoursSpinner");
            JSpinner maxMinutesSpinner = (JSpinner) configPanel.getClientProperty("maxMinutesSpinner");
            
            int minHours = (Integer) minHoursSpinner.getValue();
            int minMinutes = (Integer) minMinutesSpinner.getValue();
            int maxHours = (Integer) maxHoursSpinner.getValue();
            int maxMinutes = (Integer) maxMinutesSpinner.getValue();
            
            Duration minDuration = Duration.ofHours(minHours).plusMinutes(minMinutes);
            Duration maxDuration = Duration.ofHours(maxHours).plusMinutes(maxMinutes);
            
            return IntervalCondition.createRandomized(minDuration, maxDuration);
        } else {
            // Use fixed duration
            JSpinner hoursSpinner = (JSpinner) configPanel.getClientProperty("hoursSpinner");
            JSpinner minutesSpinner = (JSpinner) configPanel.getClientProperty("minutesSpinner");
            JSpinner randomFactorSpinner = (JSpinner) configPanel.getClientProperty("randomFactorSpinner");
            
            int hours = (Integer) hoursSpinner.getValue();
            int minutes = (Integer) minutesSpinner.getValue();
            double randomFactor = (Double) randomFactorSpinner.getValue();
            
            Duration duration = Duration.ofHours(hours).plusMinutes(minutes);
            
            return new IntervalCondition(duration, true, randomFactor,0);
        }
    }
    
    public static void createEnhancedTimeWindowConfigPanel(JPanel panel, GridBagConstraints gbc, JPanel configPanel) {
        // Section Title
        JLabel titleLabel = new JLabel("Time Window Configuration:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // Use our new custom components
        gbc.gridy++;
        
        // Create date range panel
        DateRangePanel dateRangePanel = new DateRangePanel();
        panel.add(dateRangePanel, gbc);
        
        // Add small vertical space
        gbc.gridy++;
        panel.add(Box.createVerticalStrut(10), gbc);
        
        // Create time range panel
        gbc.gridy++;
        TimeRangePanel timeRangePanel = new TimeRangePanel();
        panel.add(timeRangePanel, gbc);
        
        // Repeat Cycle Panel
        gbc.gridy++;
        JPanel repeatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        repeatPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel repeatLabel = new JLabel("Repeat Cycle:");
        repeatLabel.setForeground(Color.WHITE);
        repeatPanel.add(repeatLabel);
        
        // Create combo box with repeat cycle options
        String[] repeatOptions = {"Every Day", "Every X Days", "Every X Hours", "Every X Minutes", "Every X Weeks", "One Time Only"};
        JComboBox<String> repeatComboBox = new JComboBox<>(repeatOptions);
        repeatPanel.add(repeatComboBox);
        
        JLabel intervalLabel = new JLabel("Interval:");
        intervalLabel.setForeground(Color.WHITE);
        repeatPanel.add(intervalLabel);
        
        // Spinner for interval value (1-100)
        SpinnerNumberModel intervalModel = new SpinnerNumberModel(1, 1, 100, 1);
        JSpinner intervalSpinner = new JSpinner(intervalModel);
        intervalSpinner.setPreferredSize(new Dimension(60, intervalSpinner.getPreferredSize().height));
        repeatPanel.add(intervalSpinner);
        
        // Initially disable interval spinner for "Every Day" option
        intervalSpinner.setEnabled(false);
        
        // Enable/disable interval spinner based on selection
        repeatComboBox.addActionListener(e -> {
            String selected = (String) repeatComboBox.getSelectedItem();
            intervalSpinner.setEnabled(!selected.equals("Every Day") && !selected.equals("One Time Only"));
        });
        
        panel.add(repeatPanel, gbc);
        
        // Randomization Panel
        gbc.gridy++;
        JPanel randomizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        randomizePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JCheckBox randomizeCheckBox = new JCheckBox("Randomize window times");
        randomizeCheckBox.setForeground(Color.WHITE);
        randomizeCheckBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        randomizePanel.add(randomizeCheckBox);
        
        JLabel randomizeAmountLabel = new JLabel("± Minutes:");
        randomizeAmountLabel.setForeground(Color.WHITE);
        randomizePanel.add(randomizeAmountLabel);
        
        // Spinner for randomization amount (0-60 minutes)
        SpinnerNumberModel randomizeModel = new SpinnerNumberModel(15, 1, 60, 1);
        JSpinner randomizeSpinner = new JSpinner(randomizeModel);
        randomizeSpinner.setPreferredSize(new Dimension(60, randomizeSpinner.getPreferredSize().height));
        randomizeSpinner.setEnabled(false);
        randomizePanel.add(randomizeSpinner);
        
        // Enable/disable randomize spinner based on checkbox
        randomizeCheckBox.addActionListener(e -> 
            randomizeSpinner.setEnabled(randomizeCheckBox.isSelected())
        );
        
        panel.add(randomizePanel, gbc);
        
        // Add a helpful description
        gbc.gridy++;
        JLabel descriptionLabel = new JLabel("Plugin will only run during the specified time window");
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        gbc.gridy++;
        JLabel crossDayLabel = new JLabel("Note: If start time > end time, window crosses midnight");
        crossDayLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        crossDayLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(crossDayLabel, gbc);
        
        gbc.gridy++;
        JPanel timezonePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timezonePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel timezoneLabel = new JLabel("Current timezone: " + ZoneId.systemDefault().getId());
        timezoneLabel.setForeground(Color.YELLOW);
        timezoneLabel.setFont(FontManager.getRunescapeSmallFont());
        timezonePanel.add(timezoneLabel);
        
        panel.add(timezonePanel, gbc);
        
        // Store components for later access
        configPanel.putClientProperty("dateRangePanel", dateRangePanel); 
        configPanel.putClientProperty("timeRangePanel", timeRangePanel);
        configPanel.putClientProperty("repeatComboBox", repeatComboBox);
        configPanel.putClientProperty("intervalSpinner", intervalSpinner);
        configPanel.putClientProperty("randomizeCheckBox", randomizeCheckBox);
        configPanel.putClientProperty("randomizeSpinner", randomizeSpinner);
    }

    public static TimeWindowCondition createEnhancedTimeWindowCondition(JPanel configPanel) {
        DateRangePanel dateRangePanel = (DateRangePanel) configPanel.getClientProperty("dateRangePanel");
        TimeRangePanel timeRangePanel = (TimeRangePanel) configPanel.getClientProperty("timeRangePanel");
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
            int randomizeMinutes = (Integer) randomizeSpinner.getValue();
            condition.setRandomization(true, randomizeMinutes);
        }
        
        return condition;
    }


        /**
     * Creates a panel for configuring SingleTriggerTimeCondition
     * Uses the enhanced SingleDateTimePickerPanel component
     */
    public static void createSingleTriggerConfigPanel(JPanel panel, GridBagConstraints gbc, JPanel configPanel) {
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
        
        // Action selection panel (trigger behavior)
        gbc.gridy++;
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel actionLabel = new JLabel("Action:");
        actionLabel.setForeground(Color.WHITE);
        actionPanel.add(actionLabel);
        
        ButtonGroup actionGroup = new ButtonGroup();
        
        JRadioButton startRadio = new JRadioButton("Start plugin");
        startRadio.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        startRadio.setForeground(Color.WHITE);
        startRadio.setSelected(true);
        actionGroup.add(startRadio);
        actionPanel.add(startRadio);
        
        JRadioButton stopRadio = new JRadioButton("Stop plugin");
        stopRadio.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        stopRadio.setForeground(Color.WHITE);
        actionGroup.add(stopRadio);
        actionPanel.add(stopRadio);
        
        panel.add(actionPanel, gbc);
        
        // Post-trigger behavior panel
        gbc.gridy++;
        JPanel postTriggerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        postTriggerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel postTriggerLabel = new JLabel("After triggering:");
        postTriggerLabel.setForeground(Color.WHITE);
        postTriggerPanel.add(postTriggerLabel);
        
        ButtonGroup postTriggerGroup = new ButtonGroup();
        
        JRadioButton removeRadio = new JRadioButton("Remove condition");
        removeRadio.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        removeRadio.setForeground(Color.WHITE);
        removeRadio.setSelected(true);
        postTriggerGroup.add(removeRadio);
        postTriggerPanel.add(removeRadio);
        
        JRadioButton keepRadio = new JRadioButton("Keep condition");
        keepRadio.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        keepRadio.setForeground(Color.WHITE);
        postTriggerGroup.add(keepRadio);
        postTriggerPanel.add(keepRadio);
        
        panel.add(postTriggerPanel, gbc);
        
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
        configPanel.putClientProperty("dateTimePicker", dateTimePicker);
        configPanel.putClientProperty("singleTriggerStartRadio", startRadio);
        configPanel.putClientProperty("singleTriggerRemoveAfterRadio", removeRadio);
    }
    /**
     * Creates a SingleTriggerTimeCondition from the config panel
     * Uses the enhanced SingleDateTimePickerPanel component
     */
    public static SingleTriggerTimeCondition createSingleTriggerCondition(JPanel configPanel) {
        SingleDateTimePickerPanel dateTimePicker = (SingleDateTimePickerPanel) configPanel.getClientProperty("dateTimePicker");
        JRadioButton startRadio = (JRadioButton) configPanel.getClientProperty("singleTriggerStartRadio");
        JRadioButton removeAfterRadio = (JRadioButton) configPanel.getClientProperty("singleTriggerRemoveAfterRadio");
        
        if (dateTimePicker == null) {
            throw new IllegalStateException("Date/time picker not found. Please check the panel configuration.");
        }
        
        // Get the selected date and time as LocalDateTime
        LocalDateTime selectedDateTime = dateTimePicker.getDateTime();
        
        // Convert to ZonedDateTime using the system default timezone
        ZonedDateTime triggerTime = selectedDateTime.atZone(ZoneId.systemDefault());
        
        // Get action setting (whether this is a start or stop trigger)
        boolean isStartTrigger = startRadio == null || startRadio.isSelected();
        
        // Get post-trigger behavior (whether to remove the condition after triggering)
        boolean removeAfterTrigger = removeAfterRadio == null || removeAfterRadio.isSelected();
        
        // Create the condition with the appropriate settings
        return new SingleTriggerTimeCondition(triggerTime);
    }
}
