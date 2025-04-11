package net.runelite.client.plugins.microbot.pluginscheduler.ui.condition;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.resource.BankItemCountCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.resource.InventoryItemCountCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.resource.LootItemCondition;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;

import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.AndCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;




@Slf4j
public class ResourceConditionPanelUtil {
    /**
     * Creates a panel for configuring Inventory Item Count conditions
     */
    public static void createInventoryItemCountPanel(JPanel panel, GridBagConstraints gbc, JPanel configPanel) {
        // Title
        JLabel titleLabel = new JLabel("Inventory Item Count:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // Item name input
        gbc.gridy++;
        JPanel itemNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        itemNamePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel itemNameLabel = new JLabel("Item Name (leave empty for any):");
        itemNameLabel.setForeground(Color.WHITE);
        itemNamePanel.add(itemNameLabel);
        
        JTextField itemNameField = new JTextField(15);
        itemNameField.setForeground(Color.WHITE);
        itemNameField.setBackground(ColorScheme.DARKER_GRAY_COLOR.brighter());
        itemNamePanel.add(itemNameField);
        
        panel.add(itemNamePanel, gbc);
        
        // Count range
        gbc.gridy++;
        JPanel countPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        countPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel minCountLabel = new JLabel("Min Count:");
        minCountLabel.setForeground(Color.WHITE);
        countPanel.add(minCountLabel);
        
        SpinnerNumberModel minCountModel = new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1);
        JSpinner minCountSpinner = new JSpinner(minCountModel);
        countPanel.add(minCountSpinner);
        
        JLabel maxCountLabel = new JLabel("Max Count:");
        maxCountLabel.setForeground(Color.WHITE);
        countPanel.add(maxCountLabel);
        
        SpinnerNumberModel maxCountModel = new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1);
        JSpinner maxCountSpinner = new JSpinner(maxCountModel);
        countPanel.add(maxCountSpinner);
        
        // Link the min and max spinners
        minCountSpinner.addChangeListener(e -> {
            int minValue = (Integer) minCountSpinner.getValue();
            int maxValue = (Integer) maxCountSpinner.getValue();
            if (minValue > maxValue) {
                maxCountSpinner.setValue(minValue);
            }
        });
        
        maxCountSpinner.addChangeListener(e -> {
            int minValue = (Integer) minCountSpinner.getValue();
            int maxValue = (Integer) maxCountSpinner.getValue();
            if (maxValue < minValue) {
                minCountSpinner.setValue(maxValue);
            }
        });
        
        panel.add(countPanel, gbc);
        
        // Options panel
        gbc.gridy++;
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JCheckBox includeNotedCheckbox = new JCheckBox("Include noted items");
        includeNotedCheckbox.setForeground(Color.WHITE);
        includeNotedCheckbox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        includeNotedCheckbox.setSelected(true);
        optionsPanel.add(includeNotedCheckbox);
        
        JCheckBox randomizeCheckbox = new JCheckBox("Randomize target count");
        randomizeCheckbox.setForeground(Color.WHITE);
        randomizeCheckbox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        randomizeCheckbox.setSelected(true);
        optionsPanel.add(randomizeCheckbox);
        
        panel.add(optionsPanel, gbc);
        
        // Add a helpful description
        gbc.gridy++;
        JLabel descriptionLabel = new JLabel("Plugin will stop when you have the target number of items");
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        gbc.gridy++;
        JLabel regexLabel = new JLabel("Item name supports regex patterns (.*bones.*)");
        regexLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        regexLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(regexLabel, gbc);
        
        // Store the components for later access
        configPanel.putClientProperty("itemNameField", itemNameField);
        configPanel.putClientProperty("minCountSpinner", minCountSpinner);
        configPanel.putClientProperty("maxCountSpinner", maxCountSpinner);
        configPanel.putClientProperty("includeNotedCheckbox", includeNotedCheckbox);
        configPanel.putClientProperty("randomizeCheckbox", randomizeCheckbox);
    }
    public static InventoryItemCountCondition createInventoryItemCountCondition(JPanel configPanel) {
        JTextField itemNameField = (JTextField) configPanel.getClientProperty("itemNameField");
        JSpinner minCountSpinner = (JSpinner) configPanel.getClientProperty("minCountSpinner");
        JSpinner maxCountSpinner = (JSpinner) configPanel.getClientProperty("maxCountSpinner");
        JCheckBox includeNotedCheckbox = (JCheckBox) configPanel.getClientProperty("includeNotedCheckbox");
        JCheckBox randomizeCheckbox = (JCheckBox) configPanel.getClientProperty("randomizeCheckbox");
        
        String itemName = itemNameField.getText().trim();
        int minCount = (Integer) minCountSpinner.getValue();
        int maxCount = (Integer) maxCountSpinner.getValue();
        boolean includeNoted = includeNotedCheckbox.isSelected();
        boolean randomize = randomizeCheckbox.isSelected();
        
        if (randomize && minCount != maxCount) {
            return InventoryItemCountCondition.builder()
                    .itemName(itemName)
                    .targetCountMin(minCount)
                    .targetCountMax(maxCount)
                    .includeNoted(includeNoted)
                    .build();
        } else {
            return InventoryItemCountCondition.builder()
                    .itemName(itemName)
                    .targetCountMin(minCount)
                    .targetCountMax(minCount)                    
                    .includeNoted(includeNoted)
                    .build();
        }
    }
    /**
     * Creates a panel for configuring Bank Item Count conditions
     */
    public static void createBankItemCountPanel(JPanel panel, GridBagConstraints gbc, JPanel configPanel) {
        // Title
        JLabel titleLabel = new JLabel("Bank Item Count:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // Item name input
        gbc.gridy++;
        JPanel itemNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        itemNamePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel itemNameLabel = new JLabel("Item Name (leave empty for total items):");
        itemNameLabel.setForeground(Color.WHITE);
        itemNamePanel.add(itemNameLabel);
        
        JTextField itemNameField = new JTextField(15);
        itemNameField.setForeground(Color.WHITE);
        itemNameField.setBackground(ColorScheme.DARKER_GRAY_COLOR.brighter());
        itemNamePanel.add(itemNameField);
        
        panel.add(itemNamePanel, gbc);
        
        // Count range
        gbc.gridy++;
        JPanel countPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        countPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel minCountLabel = new JLabel("Min Count:");
        minCountLabel.setForeground(Color.WHITE);
        countPanel.add(minCountLabel);
        
        SpinnerNumberModel minCountModel = new SpinnerNumberModel(100, 0, Integer.MAX_VALUE, 10);
        JSpinner minCountSpinner = new JSpinner(minCountModel);
        countPanel.add(minCountSpinner);
        
        JLabel maxCountLabel = new JLabel("Max Count:");
        maxCountLabel.setForeground(Color.WHITE);
        countPanel.add(maxCountLabel);
        
        SpinnerNumberModel maxCountModel = new SpinnerNumberModel(100, 0, Integer.MAX_VALUE, 10);
        JSpinner maxCountSpinner = new JSpinner(maxCountModel);
        countPanel.add(maxCountSpinner);
        
        // Link the min and max spinners
        minCountSpinner.addChangeListener(e -> {
            int minValue = (Integer) minCountSpinner.getValue();
            int maxValue = (Integer) maxCountSpinner.getValue();
            if (minValue > maxValue) {
                maxCountSpinner.setValue(minValue);
            }
        });
        
        maxCountSpinner.addChangeListener(e -> {
            int minValue = (Integer) minCountSpinner.getValue();
            int maxValue = (Integer) maxCountSpinner.getValue();
            if (maxValue < minValue) {
                minCountSpinner.setValue(maxValue);
            }
        });
        
        panel.add(countPanel, gbc);
        
        // Options panel
        gbc.gridy++;
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JCheckBox randomizeCheckbox = new JCheckBox("Randomize target count");
        randomizeCheckbox.setForeground(Color.WHITE);
        randomizeCheckbox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        randomizeCheckbox.setSelected(true);
        optionsPanel.add(randomizeCheckbox);
        
        panel.add(optionsPanel, gbc);
        
        // Add a helpful description
        gbc.gridy++;
        JLabel descriptionLabel = new JLabel("Plugin will stop when you have the target number of items in bank");
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        gbc.gridy++;
        JLabel regexLabel = new JLabel("Item name supports regex patterns (.*rune.*)");
        regexLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        regexLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(regexLabel, gbc);
        
        // Store the components for later access
        configPanel.putClientProperty("bankItemNameField", itemNameField);
        configPanel.putClientProperty("bankMinCountSpinner", minCountSpinner);
        configPanel.putClientProperty("bankMaxCountSpinner", maxCountSpinner);
        configPanel.putClientProperty("bankRandomizeCheckbox", randomizeCheckbox);
    }

   

    public static BankItemCountCondition createBankItemCountCondition(JPanel configPanel) {
        JTextField itemNameField = (JTextField) configPanel.getClientProperty("bankItemNameField");
        JSpinner minCountSpinner = (JSpinner) configPanel.getClientProperty("bankMinCountSpinner");
        JSpinner maxCountSpinner = (JSpinner) configPanel.getClientProperty("bankMaxCountSpinner");
        JCheckBox randomizeCheckbox = (JCheckBox) configPanel.getClientProperty("bankRandomizeCheckbox");
        
        String itemName = itemNameField.getText().trim();
        int minCount = (Integer) minCountSpinner.getValue();
        int maxCount = (Integer) maxCountSpinner.getValue();
        boolean randomize = randomizeCheckbox.isSelected();
        
        if (randomize && minCount != maxCount) {
            return BankItemCountCondition.builder()
                    .itemName(itemName)
                    .targetCountMin(minCount)
                    .targetCountMax(maxCount)
                    .build();
        } else {
            return BankItemCountCondition.builder()
                    .itemName(itemName)
                    .targetCountMin(minCount)
                    .targetCountMax(minCount)                    
                    .build();
        }
    }
    /**
 * Creates a panel for configuring item collection conditions with enhanced options.
        */
    public static void createItemConfigPanel(JPanel panel, GridBagConstraints gbc, JPanel configPanel, boolean stopConditionPanel) {
        // Section title
        JLabel titleLabel = new JLabel(stopConditionPanel ? "Collect Items to Stop:" : "Required Items to Start:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        panel.add(titleLabel, gbc);
        
        gbc.gridy++;
        
        // Item names input
        JLabel itemsLabel = new JLabel("Item Names (comma-separated, supports regex):");
        itemsLabel.setForeground(Color.WHITE);
        panel.add(itemsLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JTextField itemsField = new JTextField();
        itemsField.setToolTipText("<html>Examples:<br>" +
                "- 'Bones' - Exact match for Bones<br>" +
                "- 'Shark|Lobster' - Match either Shark OR Lobster<br>" + 
                "- '.*rune$' - All items ending with 'rune'<br>" +
                "- 'Dragon.*,Rune.*' - Multiple patterns (Dragon items AND Rune items)</html>");
        panel.add(itemsField, gbc);
        
        // Logical operator selection (AND/OR) - only visible with multiple items
        gbc.gridy++;
        JPanel logicalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        logicalPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel logicalLabel = new JLabel("Multiple items logic:");
        logicalLabel.setForeground(Color.WHITE);
        logicalPanel.add(logicalLabel);
        
        JRadioButton andRadioButton = new JRadioButton("Need ALL items (AND)");
        andRadioButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        andRadioButton.setForeground(Color.WHITE);
        andRadioButton.setSelected(true);
        andRadioButton.setToolTipText("All specified items must be collected to satisfy the condition");
        
        JRadioButton orRadioButton = new JRadioButton("Need ANY item (OR)");
        orRadioButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        orRadioButton.setForeground(Color.WHITE);
        orRadioButton.setToolTipText("Any of the specified items will satisfy the condition");
        
        ButtonGroup logicGroup = new ButtonGroup();
        logicGroup.add(andRadioButton);
        logicGroup.add(orRadioButton);
        
        logicalPanel.add(andRadioButton);
        logicalPanel.add(orRadioButton);
        
        // Initially hide the panel - will be shown only when there are commas in the text field
        logicalPanel.setVisible(false);
        panel.add(logicalPanel, gbc);
        
        // Listen for changes in the text field to show/hide the logical panel
        itemsField.getDocument().addDocumentListener(new DocumentListener() {
            private void updateLogicalPanelVisibility() {
                String text = itemsField.getText().trim();
                boolean hasMultipleItems = text.contains(",");
                logicalPanel.setVisible(hasMultipleItems);
                panel.revalidate();
                panel.repaint();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateLogicalPanelVisibility();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateLogicalPanelVisibility();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLogicalPanelVisibility();
            }
        });
        
        // Target amount panel
        gbc.gridy++;
        JPanel amountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        amountPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel amountLabel = new JLabel("Target Amount:");
        amountLabel.setForeground(Color.WHITE);
        amountPanel.add(amountLabel);
        
        SpinnerNumberModel amountModel = new SpinnerNumberModel(100, 1, Integer.MAX_VALUE, 10);
        JSpinner amountSpinner = new JSpinner(amountModel);
        amountSpinner.setPreferredSize(new Dimension(100, amountSpinner.getPreferredSize().height));
        amountPanel.add(amountSpinner);
        
        JCheckBox sameAmountCheckBox = new JCheckBox("Same amount for all items");
        sameAmountCheckBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        sameAmountCheckBox.setForeground(Color.WHITE);
        sameAmountCheckBox.setSelected(true);
        sameAmountCheckBox.setVisible(false); // Only show with multiple items
        sameAmountCheckBox.setToolTipText("Use the same target amount for all items");
        amountPanel.add(sameAmountCheckBox);
        
        JCheckBox randomizeCheckBox = new JCheckBox("Randomize");
        randomizeCheckBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        randomizeCheckBox.setForeground(Color.WHITE);
        randomizeCheckBox.setToolTipText("Randomize the target amount between min and max values");
        amountPanel.add(randomizeCheckBox);
        
        if (!stopConditionPanel) {
            randomizeCheckBox.setVisible(false);
        }
        
        panel.add(amountPanel, gbc);
        
        // Min/Max panel
        gbc.gridy++;
        JPanel minMaxPanel = new JPanel(new GridLayout(1, 4, 5, 0));
        minMaxPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel minLabel = new JLabel("Min:");
        minLabel.setForeground(Color.WHITE);
        minMaxPanel.add(minLabel);
        
        SpinnerNumberModel minModel = new SpinnerNumberModel(50, 1, Integer.MAX_VALUE, 10);
        JSpinner minSpinner = new JSpinner(minModel);
        minMaxPanel.add(minSpinner);
        
        JLabel maxLabel = new JLabel("Max:");
        maxLabel.setForeground(Color.WHITE);
        minMaxPanel.add(maxLabel);
        
        SpinnerNumberModel maxModel = new SpinnerNumberModel(150, 1, Integer.MAX_VALUE, 10);
        JSpinner maxSpinner = new JSpinner(maxModel);
        minMaxPanel.add(maxSpinner);
        
        minMaxPanel.setVisible(false);
        panel.add(minMaxPanel, gbc);
        
        // Additional options panel
        gbc.gridy++;
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JCheckBox includeNotedCheckBox = new JCheckBox("Include noted items");
        includeNotedCheckBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        includeNotedCheckBox.setForeground(Color.WHITE);
        includeNotedCheckBox.setSelected(true);
        includeNotedCheckBox.setToolTipText("<html>If checked, will also count noted versions of the items<br>" +
                "For example, 'Bones' would match both normal and noted bones</html>");
        optionsPanel.add(includeNotedCheckBox);
        
        JCheckBox includeNoneOwnerCheckBox = new JCheckBox("Include unowned items");
        includeNoneOwnerCheckBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        includeNoneOwnerCheckBox.setForeground(Color.WHITE);
        includeNoneOwnerCheckBox.setToolTipText("<html>If checked, items that don't belong to you will also be tracked<br>" +
                "By default, only items that belong to you are counted</html>");
        optionsPanel.add(includeNoneOwnerCheckBox);
        
        panel.add(optionsPanel, gbc);
        
        // Toggle min/max panel based on randomize checkbox
        randomizeCheckBox.addChangeListener(e -> {
            minMaxPanel.setVisible(randomizeCheckBox.isSelected());
            amountSpinner.setEnabled(!randomizeCheckBox.isSelected());
            
            // If enabling randomize, set min/max from current amount
            if (randomizeCheckBox.isSelected()) {
                int amount = (Integer) amountSpinner.getValue();
                minSpinner.setValue(Math.max(1, amount - 50));
                maxSpinner.setValue(amount + 50);
            }
            
            panel.revalidate();
            panel.repaint();
        });
        
        // Link sameAmountCheckBox visibility to comma presence
        itemsField.getDocument().addDocumentListener(new DocumentListener() {
            private void updateCheckBoxVisibility() {
                String text = itemsField.getText().trim();
                boolean hasMultipleItems = text.contains(",");
                sameAmountCheckBox.setVisible(hasMultipleItems);
                panel.revalidate();
                panel.repaint();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCheckBoxVisibility();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCheckBoxVisibility();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCheckBoxVisibility();
            }
        });
        
        // Add value change listeners for min/max validation
        minSpinner.addChangeListener(e -> {
            int min = (Integer) minSpinner.getValue();
            int max = (Integer) maxSpinner.getValue();
            
            if (min > max) {
                maxSpinner.setValue(min);
            }
        });
        
        maxSpinner.addChangeListener(e -> {
            int min = (Integer) minSpinner.getValue();
            int max = (Integer) maxSpinner.getValue();
            
            if (max < min) {
                minSpinner.setValue(max);
            }
        });
        
        // Description
        gbc.gridy++;
        JLabel descriptionLabel;
        if (stopConditionPanel) {
            descriptionLabel = new JLabel("Plugin will stop when target amount of items is collected");
        } else {
            descriptionLabel = new JLabel("Plugin will only start when you have the required items");
        }
        descriptionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(descriptionLabel, gbc);
        
        // Add regex help
        gbc.gridy++;
        JLabel regexHelpLabel = new JLabel("Regex help: Use '|' for OR, '.*' for any characters");
        regexHelpLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        regexHelpLabel.setFont(FontManager.getRunescapeSmallFont());
        panel.add(regexHelpLabel, gbc);
        
        // Store components
        configPanel.putClientProperty("itemsField", itemsField);
        configPanel.putClientProperty("andRadioButton", andRadioButton);
        configPanel.putClientProperty("sameAmountCheckBox", sameAmountCheckBox);
        configPanel.putClientProperty("amountSpinner", amountSpinner);
        configPanel.putClientProperty("minAmountSpinner", minSpinner);
        configPanel.putClientProperty("maxAmountSpinner", maxSpinner);
        configPanel.putClientProperty("randomizeItemAmount", randomizeCheckBox);
        configPanel.putClientProperty("includeNotedCheckBox", includeNotedCheckBox);
        configPanel.putClientProperty("includeNoneOwnerCheckBox", includeNoneOwnerCheckBox);
    }
   /**
 * Creates an appropriate LootItemCondition or logical condition based on user input
 */
    public static Condition createItemCondition(JPanel configPanel) {
        JTextField itemsField = (JTextField) configPanel.getClientProperty("itemsField");
        JRadioButton andRadioButton = (JRadioButton) configPanel.getClientProperty("andRadioButton");
        JCheckBox sameAmountCheckBox = (JCheckBox) configPanel.getClientProperty("sameAmountCheckBox");
        JCheckBox randomizeCheckBox = (JCheckBox) configPanel.getClientProperty("randomizeItemAmount");
        JCheckBox includeNotedCheckBox = (JCheckBox) configPanel.getClientProperty("includeNotedCheckBox");
        JCheckBox includeNoneOwnerCheckBox = (JCheckBox) configPanel.getClientProperty("includeNoneOwnerCheckBox");
        
        // Handle potential component errors
        if (itemsField == null) {
            log.error("Items field component not found");
            return null;
        }
        
        boolean includeNoted = includeNotedCheckBox != null && includeNotedCheckBox.isSelected();
        boolean includeNoneOwner = includeNoneOwnerCheckBox != null && includeNoneOwnerCheckBox.isSelected();
        
        String itemNamesString = itemsField.getText().trim();
        if (itemNamesString.isEmpty()) {
            return null; // Invalid item name
        }
        
        // Split by comma and trim each item name
        String[] itemNamesArray = itemNamesString.split(",");
        List<String> itemNames = new ArrayList<>();
        
        for (String itemName : itemNamesArray) {
            itemName = itemName.trim();
            if (!itemName.isEmpty()) {
                itemNames.add(itemName);
            }
        }
        
        if (itemNames.isEmpty()) {
            return null;
        }
        
        // If only one item, create a simple LootItemCondition
        if (itemNames.size() == 1) {
            if (randomizeCheckBox != null && randomizeCheckBox.isSelected()) {
                JSpinner minAmountSpinner = (JSpinner) configPanel.getClientProperty("minAmountSpinner");
                JSpinner maxAmountSpinner = (JSpinner) configPanel.getClientProperty("maxAmountSpinner");
                
                int minAmount = minAmountSpinner != null ? (Integer) minAmountSpinner.getValue() : 1;
                int maxAmount = maxAmountSpinner != null ? (Integer) maxAmountSpinner.getValue() : minAmount;
                
                return LootItemCondition.createRandomized(itemNames.get(0), minAmount, maxAmount, includeNoted, includeNoneOwner);
            } else {
                JSpinner amountSpinner = (JSpinner) configPanel.getClientProperty("amountSpinner");
                int amount = amountSpinner != null ? (Integer) amountSpinner.getValue() : 1;
                
                return LootItemCondition.builder()
                        .itemName(itemNames.get(0))
                        .targetAmountMin(amount)
                        .targetAmountMax(amount)
                        .includeNoted(includeNoted)
                        .includeNoneOwner(includeNoneOwner)
                        .build();
            }
        }
        
        // For multiple items, create a logical condition based on selection
        boolean useAndLogic = andRadioButton == null || andRadioButton.isSelected();
        boolean useSameAmount = sameAmountCheckBox != null && sameAmountCheckBox.isSelected();
        
        if (useSameAmount) {
            // Same target for all items
            if (randomizeCheckBox != null && randomizeCheckBox.isSelected()) {
                JSpinner minAmountSpinner = (JSpinner) configPanel.getClientProperty("minAmountSpinner");
                JSpinner maxAmountSpinner = (JSpinner) configPanel.getClientProperty("maxAmountSpinner");
                
                int minAmount = minAmountSpinner != null ? (Integer) minAmountSpinner.getValue() : 1;
                int maxAmount = maxAmountSpinner != null ? (Integer) maxAmountSpinner.getValue() : minAmount;
                
                if (useAndLogic) {
                    return LootItemCondition.createAndCondition(itemNames, minAmount, maxAmount, includeNoted, includeNoneOwner);
                } else {
                    return LootItemCondition.createOrCondition(itemNames, minAmount, maxAmount, includeNoted, includeNoneOwner);
                }
            } else {
                JSpinner amountSpinner = (JSpinner) configPanel.getClientProperty("amountSpinner");
                int amount = amountSpinner != null ? (Integer) amountSpinner.getValue() : 1;
                
                if (useAndLogic) {
                    return LootItemCondition.createAndCondition(itemNames, amount, amount, includeNoted, includeNoneOwner);
                } else {
                    return LootItemCondition.createOrCondition(itemNames, amount, amount, includeNoted, includeNoneOwner);
                }
            }
        } else {
            // Individual targets for each item (for simplicity, we'll use the same target for all)
            if (randomizeCheckBox != null && randomizeCheckBox.isSelected()) {
                JSpinner minAmountSpinner = (JSpinner) configPanel.getClientProperty("minAmountSpinner");
                JSpinner maxAmountSpinner = (JSpinner) configPanel.getClientProperty("maxAmountSpinner");
                
                int minAmount = minAmountSpinner != null ? (Integer) minAmountSpinner.getValue() : 1;
                int maxAmount = maxAmountSpinner != null ? (Integer) maxAmountSpinner.getValue() : minAmount;
                
                // Create lists of min/max amounts for each item
                List<Integer> minAmounts = new ArrayList<>();
                List<Integer> maxAmounts = new ArrayList<>();
                
                for (int i = 0; i < itemNames.size(); i++) {
                    minAmounts.add(minAmount);
                    maxAmounts.add(maxAmount);
                }
                
                if (useAndLogic) {
                    LogicalCondition condition = LootItemCondition.createAndCondition(itemNames, minAmounts, maxAmounts, includeNoted, includeNoneOwner);                                                          
                    return condition;
                } else {
                    // For OrCondition with different parameters, we need to use the overloaded version
                    return LootItemCondition.createOrCondition(itemNames, minAmounts, maxAmounts, includeNoted, includeNoneOwner);
                }
            } else {
                JSpinner amountSpinner = (JSpinner) configPanel.getClientProperty("amountSpinner");
                int amount = amountSpinner != null ? (Integer) amountSpinner.getValue() : 1;
                
                // Create lists of amounts for each item
                List<Integer> minAmounts = new ArrayList<>();
                List<Integer> maxAmounts = new ArrayList<>();
                
                for (int i = 0; i < itemNames.size(); i++) {
                    minAmounts.add(amount);
                    maxAmounts.add(amount);
                }
                
                if (useAndLogic) {
                    // Use the version that takes explicit includeNoted/includeNoneOwner params
                    return LootItemCondition.createAndCondition(itemNames, amount, amount, includeNoted, includeNoneOwner);
                } else {
                    return LootItemCondition.createOrCondition(itemNames, amount, amount, includeNoted, includeNoneOwner);
                }
            }
        }
    }

}
