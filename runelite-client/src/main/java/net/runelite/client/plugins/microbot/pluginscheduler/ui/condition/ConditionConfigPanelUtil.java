package net.runelite.client.plugins.microbot.pluginscheduler.ui.condition;

import java.awt.Color;
import java.awt.Container;
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
import net.runelite.api.annotations.Component;
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

public class ConditionConfigPanelUtil {

    public static void createTimeConfigPanel(JPanel panel, GridBagConstraints gbc, JPanel configPanel) {
        JLabel durationLabel = new JLabel("Duration Range:");
        durationLabel.setForeground(Color.WHITE);
        durationLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(durationLabel, gbc);
        
        // Time range panel with combined HH:MM format
        gbc.gridy++;
        JPanel timeRangePanel = new JPanel(new GridLayout(2, 2, 5, 5));
        timeRangePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Min time input
        JLabel minTimeLabel = new JLabel("Min Duration (HH:MM):");
        minTimeLabel.setForeground(Color.WHITE);
        timeRangePanel.add(minTimeLabel);
        
        // Create combined min time panel
        JPanel minTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        minTimePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Use a formatted text field with validation for "HH:MM" format
        JTextField minTimeField = new JTextField(5);
        minTimeField.setText("01:00"); // Default 1 hour
        minTimeField.setForeground(Color.WHITE);
        minTimeField.setBackground(ColorScheme.DARKER_GRAY_COLOR.brighter());
        minTimePanel.add(minTimeField);
        
        timeRangePanel.add(minTimePanel);
        
        // Max time input
        JLabel maxTimeLabel = new JLabel("Max Duration (HH:MM):");
        maxTimeLabel.setForeground(Color.WHITE);
        timeRangePanel.add(maxTimeLabel);
        
        // Create combined max time panel
        JPanel maxTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        maxTimePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Use a formatted text field with validation for "HH:MM" format
        JTextField maxTimeField = new JTextField(5);
        maxTimeField.setText("02:30"); // Default 2:30 hours
        maxTimeField.setForeground(Color.WHITE);
        maxTimeField.setBackground(ColorScheme.DARKER_GRAY_COLOR.brighter());
        maxTimePanel.add(maxTimeField);
        
        timeRangePanel.add(maxTimePanel);
        
        panel.add(timeRangePanel, gbc);
        
        // Add input validation for time fields
        minTimeField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateTimeField(minTimeField, maxTimeField, true);
            }
        });
        
        maxTimeField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateTimeField(minTimeField, maxTimeField, false);
            }
        });
        
        // Randomization options
        gbc.gridy++;
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
        
        // Disable random factor when randomize is unchecked
        randomizeCheckBox.addChangeListener(e -> {
            randomFactorSpinner.setEnabled(randomizeCheckBox.isSelected());
        });
        
        panel.add(randomPanel, gbc);
        
        // Add a helpful description
        gbc.gridy++;
        JLabel descriptionLabel = new JLabel("Plugin will stop after a random time between min and max");
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        // Store components for later access when creating conditions
        configPanel.putClientProperty("minTimeField", minTimeField);
        configPanel.putClientProperty("maxTimeField", maxTimeField);
        configPanel.putClientProperty("randomizeCheckBox", randomizeCheckBox);
        configPanel.putClientProperty("randomFactorSpinner", randomFactorSpinner);
    }
    /**
     * Validates a time field to ensure it contains a valid HH:MM format
     * Enforces min ≤ max constraint when comparing fields
     */
    private static void validateTimeField(JTextField minField, JTextField maxField, boolean isMinField) {
        try {
            // Parse the current field
            String text = isMinField ? minField.getText() : maxField.getText();
            String[] parts = text.split(":");
            
            if (parts.length != 2) {
                throw new NumberFormatException("Invalid format");
            }
            
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            
            // Validate ranges
            hours = Math.min(23, Math.max(0, hours));
            minutes = Math.min(59, Math.max(0, minutes));
            
            // Format properly
            String formatted = String.format("%02d:%02d", hours, minutes);
            
            if (isMinField) {
                minField.setText(formatted);
                
                // Check min ≤ max
                String[] maxParts = maxField.getText().split(":");
                if (maxParts.length == 2) {
                    int maxHours = Integer.parseInt(maxParts[0]);
                    int maxMinutes = Integer.parseInt(maxParts[1]);
                    
                    if (hours > maxHours || (hours == maxHours && minutes > maxMinutes)) {
                        maxField.setText(formatted);
                    }
                }
            } else {
                maxField.setText(formatted);
                
                // Check max ≥ min
                String[] minParts = minField.getText().split(":");
                if (minParts.length == 2) {
                    int minHours = Integer.parseInt(minParts[0]);
                    int minMinutes = Integer.parseInt(minParts[1]);
                    
                    if (hours < minHours || (hours == minHours && minutes < minMinutes)) {
                        minField.setText(formatted);
                    }
                }
            }
        } catch (NumberFormatException e) {
            // Reset to a default valid value if parsing fails
            if (isMinField) {
                minField.setText("01:00");
            } else {
                maxField.setText("02:00");
            }
        }
    }

     public static IntervalCondition createTimeCondition(JPanel configPanel) {
        JTextField minTimeField = (JTextField) configPanel.getClientProperty("minTimeField");
        JTextField maxTimeField = (JTextField) configPanel.getClientProperty("maxTimeField");
        JCheckBox randomizeCheckBox = (JCheckBox) configPanel.getClientProperty("randomizeCheckBox");
        JSpinner randomFactorSpinner = (JSpinner) configPanel.getClientProperty("randomFactorSpinner");
        
        // Parse min time
        String[] minParts = minTimeField.getText().split(":");
        int minHours = Integer.parseInt(minParts[0]);
        int minMinutes = Integer.parseInt(minParts[1]);
        
        // Parse max time
        String[] maxParts = maxTimeField.getText().split(":");
        int maxHours = Integer.parseInt(maxParts[0]);
        int maxMinutes = Integer.parseInt(maxParts[1]);
        
        boolean randomize = randomizeCheckBox.isSelected();
        double randomFactor = randomize ? (Double) randomFactorSpinner.getValue() : 0.0;
        
        Duration minDuration = Duration.ofHours(minHours).plusMinutes(minMinutes);
        Duration maxDuration = Duration.ofHours(maxHours).plusMinutes(maxMinutes);
        
        // Check if min and max are the same
        if (minHours == maxHours && minMinutes == maxMinutes) {
            return new IntervalCondition(minDuration, randomize, randomFactor,0);
        }
        
        // Create randomized interval condition
        return IntervalCondition.createRandomized(minDuration, maxDuration);
    }



    public static void createTimeWindowConfigPanel(JPanel panel, GridBagConstraints gbc, JPanel configPanel, boolean withInWindow) {
        JLabel startTimeLabel = new JLabel("Start Time (24h format):");
        startTimeLabel.setForeground(Color.WHITE);
        panel.add(startTimeLabel, gbc);
        
        // Start time panel with hour and minute
        gbc.gridy++;
        JPanel startTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startTimePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        SpinnerNumberModel startHourModel = new SpinnerNumberModel(9, 0, 23, 1);
        JSpinner startHourSpinner = new JSpinner(startHourModel);
        startTimePanel.add(startHourSpinner);
        
        JLabel hourLabel = new JLabel("hour");
        hourLabel.setForeground(Color.WHITE);
        startTimePanel.add(hourLabel);
        
        SpinnerNumberModel startMinuteModel = new SpinnerNumberModel(0, 0, 59, 1);
        JSpinner startMinuteSpinner = new JSpinner(startMinuteModel);
        startTimePanel.add(startMinuteSpinner);
        
        JLabel minuteLabel = new JLabel("min");
        minuteLabel.setForeground(Color.WHITE);
        startTimePanel.add(minuteLabel);
        
        panel.add(startTimePanel, gbc);
        
        // End time
        gbc.gridy++;
        JLabel endTimeLabel = new JLabel("End Time (24h format):");
        endTimeLabel.setForeground(Color.WHITE);
        panel.add(endTimeLabel, gbc);
        
        gbc.gridy++;
        JPanel endTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        endTimePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        SpinnerNumberModel endHourModel = new SpinnerNumberModel(17, 0, 23, 1);
        JSpinner endHourSpinner = new JSpinner(endHourModel);
        endTimePanel.add(endHourSpinner);
        
        JLabel endHourLabel = new JLabel("hour");
        endHourLabel.setForeground(Color.WHITE);
        endTimePanel.add(endHourLabel);
        
        SpinnerNumberModel endMinuteModel = new SpinnerNumberModel(0, 0, 59, 1);
        JSpinner endMinuteSpinner = new JSpinner(endMinuteModel);
        endTimePanel.add(endMinuteSpinner);
        
        JLabel endMinuteLabel = new JLabel("min");
        endMinuteLabel.setForeground(Color.WHITE);
        endTimePanel.add(endMinuteLabel);
        
        panel.add(endTimePanel, gbc);
        
        // Description
        gbc.gridy++;
        JLabel descriptionLabel; 
        if(withInWindow){
            descriptionLabel = new JLabel("Plugin can not be stop\\started in the give only run during window");
        }else{
            descriptionLabel = new JLabel("Plugin will only stoped\\started during the given window");
        }
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        gbc.gridy++;
        JLabel crossDayLabel = new JLabel("Note: If start > end, window crosses midnight");
        crossDayLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        crossDayLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(crossDayLabel, gbc);
        
        // Store the components for later
        configPanel.putClientProperty("startHourSpinner", startHourSpinner);
        configPanel.putClientProperty("startMinuteSpinner", startMinuteSpinner);
        configPanel.putClientProperty("endHourSpinner", endHourSpinner);
        configPanel.putClientProperty("endMinuteSpinner", endMinuteSpinner);
        configPanel.putClientProperty("withInWindow", !withInWindow);
    }


    public static void createEnhancedTimeWindowConfigPanel(JPanel panel, GridBagConstraints gbc, JPanel configPanel) {
        // Section Title
        JLabel titleLabel = new JLabel("Time Window Configuration:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        panel.add(titleLabel, gbc);
        
        // Date Range Panel
        gbc.gridy++;
        JPanel dateRangePanel = new JPanel(new GridLayout(2, 2, 5, 5));
        dateRangePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel startDateLabel = new JLabel("Start Date (yyyy-MM-dd):");
        startDateLabel.setForeground(Color.WHITE);
        dateRangePanel.add(startDateLabel);
        
        // Start date field with default today
        JTextField startDateField = new JTextField(LocalDate.now().toString());
        startDateField.setForeground(Color.WHITE);
        startDateField.setBackground(ColorScheme.DARKER_GRAY_COLOR.brighter());
        dateRangePanel.add(startDateField);
        
        JLabel endDateLabel = new JLabel("End Date (yyyy-MM-dd):");
        endDateLabel.setForeground(Color.WHITE);
        dateRangePanel.add(endDateLabel);
        
        // End date field with default one month from today
        JTextField endDateField = new JTextField(LocalDate.now().plusMonths(1).toString());
        endDateField.setForeground(Color.WHITE);
        endDateField.setBackground(ColorScheme.DARKER_GRAY_COLOR.brighter());
        dateRangePanel.add(endDateField);
        
        panel.add(dateRangePanel, gbc);
        
        // Daily Time Window Panel
        gbc.gridy++;
        JPanel timeWindowPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        timeWindowPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel startTimeLabel = new JLabel("Start Time (HH:mm):");
        startTimeLabel.setForeground(Color.WHITE);
        timeWindowPanel.add(startTimeLabel);
        
        // Start time field with default 9:00
        JTextField startTimeField = new JTextField("09:00");
        startTimeField.setForeground(Color.WHITE);
        startTimeField.setBackground(ColorScheme.DARKER_GRAY_COLOR.brighter());
        timeWindowPanel.add(startTimeField);
        
        JLabel endTimeLabel = new JLabel("End Time (HH:mm):");
        endTimeLabel.setForeground(Color.WHITE);
        timeWindowPanel.add(endTimeLabel);
        
        // End time field with default 17:00
        JTextField endTimeField = new JTextField("17:00");
        endTimeField.setForeground(Color.WHITE);
        endTimeField.setBackground(ColorScheme.DARKER_GRAY_COLOR.brighter());
        timeWindowPanel.add(endTimeField);
        
        panel.add(timeWindowPanel, gbc);
        
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
        
        // Set up validation for date and time fields
        startDateField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateDateField(startDateField);
            }
        });
        
        endDateField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateDateField(endDateField);
            }
        });
        
        startTimeField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateTimeField(startTimeField);
            }
        });
        
        endTimeField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateTimeField(endTimeField);
            }
        });
        
        // Store components for later access
        configPanel.putClientProperty("startDateField", startDateField);
        configPanel.putClientProperty("endDateField", endDateField);
        configPanel.putClientProperty("startTimeField", startTimeField);
        configPanel.putClientProperty("endTimeField", endTimeField);
        configPanel.putClientProperty("repeatComboBox", repeatComboBox);
        configPanel.putClientProperty("intervalSpinner", intervalSpinner);
        configPanel.putClientProperty("randomizeCheckBox", randomizeCheckBox);
        configPanel.putClientProperty("randomizeSpinner", randomizeSpinner);

        gbc.gridy++;
        JPanel timezonePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timezonePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel timezoneLabel = new JLabel("Current timezone: " + ZoneId.systemDefault().getId());
        timezoneLabel.setForeground(Color.YELLOW);
        timezoneLabel.setFont(FontManager.getRunescapeSmallFont());
        timezonePanel.add(timezoneLabel);

        //panel.add(timezonePanel, gbc);
    }

    // Helper methods for validation
    private static void validateDateField(JTextField field) {
        try {
            String text = field.getText().trim();
            LocalDate date = LocalDate.parse(text);
            field.setText(date.toString()); // Format consistently
        } catch (Exception e) {
            // Reset to today's date on parse error
            field.setText(LocalDate.now().toString());
        }
    }

    private static void validateTimeField(JTextField field) {
        try {
            String text = field.getText().trim();
            // Handle different formats (H:mm or HH:mm)
            LocalTime time;
            if (text.contains(":")) {
                time = LocalTime.parse(text);
            } else if (text.length() <= 2) {
                // Handle hour-only input
                time = LocalTime.of(Integer.parseInt(text), 0);
            } else if (text.length() <= 4) {
                // Handle military-style input (e.g., "1430" for 14:30)
                int hour = Integer.parseInt(text.substring(0, text.length() - 2));
                int minute = Integer.parseInt(text.substring(text.length() - 2));
                time = LocalTime.of(hour, minute);
            } else {
                throw new IllegalArgumentException("Invalid time format");
            }
            
            // Format consistently as HH:mm
            field.setText(time.format(DateTimeFormatter.ofPattern("HH:mm")));
        } catch (Exception e) {
            // Reset to default time on parse error
            field.setText("09:00");
        }
    }
    public static TimeWindowCondition createEnhancedTimeWindowCondition(JPanel configPanel) {
        JTextField startDateField = (JTextField) configPanel.getClientProperty("startDateField");
        JTextField endDateField = (JTextField) configPanel.getClientProperty("endDateField");
        JTextField startTimeField = (JTextField) configPanel.getClientProperty("startTimeField");
        JTextField endTimeField = (JTextField) configPanel.getClientProperty("endTimeField");
        JComboBox<String> repeatComboBox = (JComboBox<String>) configPanel.getClientProperty("repeatComboBox");
        JSpinner intervalSpinner = (JSpinner) configPanel.getClientProperty("intervalSpinner");
        JCheckBox randomizeCheckBox = (JCheckBox) configPanel.getClientProperty("randomizeCheckBox");
        JSpinner randomizeSpinner = (JSpinner) configPanel.getClientProperty("randomizeSpinner");
        
        // Parse date values
        LocalDate startDate = LocalDate.parse(startDateField.getText().trim());
        LocalDate endDate = LocalDate.parse(endDateField.getText().trim());
        
        // Swap dates if start is after end
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }
        
        // Parse time values - use the system default timezone
        LocalTime startTime = LocalTime.parse(startTimeField.getText().trim());
        LocalTime endTime = LocalTime.parse(endTimeField.getText().trim());
        
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
    public static Condition createTimeWindowCondition(JPanel configPanel) {
        // Check if this is the enhanced version
        JTextField startDateField = (JTextField) configPanel.getClientProperty("startDateField");
        
        if (startDateField != null) {
            // This is the enhanced panel, use the new method
            return (Condition)createEnhancedTimeWindowCondition(configPanel);
        }
        
        // Legacy implementation for backward compatibility
        JSpinner startHourSpinner = (JSpinner) configPanel.getClientProperty("startHourSpinner");
        JSpinner startMinuteSpinner = (JSpinner) configPanel.getClientProperty("startMinuteSpinner");
        JSpinner endHourSpinner = (JSpinner) configPanel.getClientProperty("endHourSpinner");
        JSpinner endMinuteSpinner = (JSpinner) configPanel.getClientProperty("endMinuteSpinner");
        boolean withInWindow = (boolean) configPanel.getClientProperty("withInWindow");
        
        int startHour = (Integer) startHourSpinner.getValue();
        int startMinute = (Integer) startMinuteSpinner.getValue();
        int endHour = (Integer) endHourSpinner.getValue();
        int endMinute = (Integer) endMinuteSpinner.getValue();
        
        LocalTime startTime = LocalTime.of(startHour, startMinute);
        LocalTime endTime = LocalTime.of(endHour, endMinute);
        
        // Create a simple daily window
        TimeWindowCondition condition = TimeWindowCondition.createDaily(startTime, endTime);
        
        if (withInWindow) {
            return ((Condition) condition);
        } else {
            return (Condition) new NotCondition(condition);
        }
    }
    public static void createDayOfWeekConfigPanel(JPanel panel, GridBagConstraints gbc, JPanel configPanel) {
        JLabel daysLabel = new JLabel("Active Days:");
        daysLabel.setForeground(Color.WHITE);
        panel.add(daysLabel, gbc);
        
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
        
        // Description
        gbc.gridy++;
        JLabel descriptionLabel = new JLabel("Plugin will only run on selected days of the week");
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        // Store components for later access
        configPanel.putClientProperty("dayCheckboxes", dayCheckboxes);
    }

    public static DayOfWeekCondition createDayOfWeekCondition(JPanel configPanel) {
        JCheckBox[] dayCheckboxes = (JCheckBox[]) configPanel.getClientProperty("dayCheckboxes");
        Set<DayOfWeek> activeDays = EnumSet.noneOf(DayOfWeek.class);
        
        // Map checkbox indices to DayOfWeek enum values (0 = Monday)
        if (dayCheckboxes[0].isSelected()) activeDays.add(DayOfWeek.MONDAY);
        if (dayCheckboxes[1].isSelected()) activeDays.add(DayOfWeek.TUESDAY);
        if (dayCheckboxes[2].isSelected()) activeDays.add(DayOfWeek.WEDNESDAY);
        if (dayCheckboxes[3].isSelected()) activeDays.add(DayOfWeek.THURSDAY);
        if (dayCheckboxes[4].isSelected()) activeDays.add(DayOfWeek.FRIDAY);
        if (dayCheckboxes[5].isSelected()) activeDays.add(DayOfWeek.SATURDAY);
        if (dayCheckboxes[6].isSelected()) activeDays.add(DayOfWeek.SUNDAY);
        
        return new DayOfWeekCondition(0,activeDays);
    }
   
    /**
     * Creates a panel for configuring SingleTriggerTimeCondition
     */
    public static void createSingleTriggerConfigPanel(JPanel panel, GridBagConstraints gbc, JPanel configPanel) {
        // Date/time picker
        JLabel dateTimeLabel = new JLabel("Trigger at (date & time):");
        dateTimeLabel.setForeground(Color.WHITE);
        panel.add(dateTimeLabel, gbc);
        
        gbc.gridy++;
        JPanel dateTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateTimePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dateTimeSpinner = new JSpinner(dateModel);
        dateTimeSpinner.setEditor(new JSpinner.DateEditor(dateTimeSpinner, "yyyy-MM-dd HH:mm"));
        
        // Set default to current time + 1 hour
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        dateTimeSpinner.setValue(calendar.getTime());
        
        dateTimePanel.add(dateTimeSpinner);
        panel.add(dateTimePanel, gbc);
        
        // Description
        gbc.gridy++;
        JLabel descriptionLabel = new JLabel("Plugin will trigger once at the specified time");
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        // Store components for later access
        configPanel.putClientProperty("dateTimeSpinner", dateTimeSpinner);
    }

    /**
     * Creates a SingleTriggerTimeCondition from the config panel
     */
    public static SingleTriggerTimeCondition createSingleTriggerCondition(JPanel configPanel) {
        JSpinner dateTimeSpinner = (JSpinner) configPanel.getClientProperty("dateTimeSpinner");
        Date selectedDate = (Date) dateTimeSpinner.getValue();
        
        // Convert to ZonedDateTime
        ZonedDateTime triggerTime = ZonedDateTime.ofInstant(
                selectedDate.toInstant(),
                ZoneId.systemDefault());
        
        return new SingleTriggerTimeCondition(triggerTime);
    }

    /**
     * Helper method to find a component of a specific type in a container
     
    private <T> T findComponentOfType(Container container, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            
            if (component instanceof Container) {
                T found = findComponentOfType((Container) component, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }*/
}
