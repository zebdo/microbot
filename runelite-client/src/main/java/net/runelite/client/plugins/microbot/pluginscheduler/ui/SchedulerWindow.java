package net.runelite.client.plugins.microbot.pluginscheduler.ui;

import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerPlugin;
import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerState;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.time.TimeCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.type.PluginScheduleEntry;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.condition.ConditionConfigPanel;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.PluginScheduleEntry.ScheduleFormPanel;
import net.runelite.client.plugins.microbot.pluginscheduler.ui.PluginScheduleEntry.ScheduleTablePanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.plugins.microbot.Microbot;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@Slf4j
public class SchedulerWindow extends JFrame {
    private final SchedulerPlugin plugin;
    private JTabbedPane tabbedPane;
    private final ScheduleTablePanel tablePanel;
    private final ScheduleFormPanel formPanel;
    private final ConditionConfigPanel stopConditionPanel;
    private final SchedulerInfoPanel infoPanel;
    private JButton runSchedulerButton;
    private JButton stopSchedulerButton;    
    // Timer for refreshing the info panel
    private Timer refreshTimer;

    public SchedulerWindow(SchedulerPlugin plugin) {
        super("Plugin Scheduler");
        this.plugin = plugin;

        // Increase width to accommodate the info panel
        setSize(1050, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Create main components
        tablePanel = new ScheduleTablePanel(plugin);
        formPanel = new ScheduleFormPanel(plugin);
        stopConditionPanel = new ConditionConfigPanel(plugin, true);
        infoPanel = new SchedulerInfoPanel(plugin);

        // Set up form panel actions
        formPanel.setAddButtonAction(e -> onAddPlugin());
        formPanel.setUpdateButtonAction(e -> onUpdatePlugin());
        formPanel.setRemoveButtonAction(e -> onRemovePlugin());
        formPanel.setEditMode(false);

        // Set up form panel to clear table selection when ComboBox changes
        formPanel.setSelectionChangeListener(() -> {
            tablePanel.clearSelection();
        });

       

        // Set up condition panel callback
        stopConditionPanel.setUserConditionUpdateCallback(userCondition -> {
            PluginScheduleEntry selected = tablePanel.getSelectedPlugin();
            if (selected != null) {             
                plugin.saveScheduledPlugins();
                tablePanel.refreshTable();
                infoPanel.refresh();
                
                // Check if we're waiting to start a plugin
                if (plugin.getCurrentState() == SchedulerState.STARTING_PLUGIN && 
                    plugin.getCurrentPlugin() == selected &&
                    !selected.getStopConditionManager().getConditions().isEmpty()) {
                    
                    // Conditions added for the plugin we're waiting to start - continue starting
                    int result = JOptionPane.showConfirmDialog(
                        this,
                        "Stop conditions have been added. Would you like to start the plugin now?",
                        "Start Plugin",
                        JOptionPane.YES_NO_OPTION
                    );
                    
                    if (result == JOptionPane.YES_OPTION) {
                        plugin.continueStartingPluginScheduleEntry(selected);
                    } else {
                        // User decided not to start - reset state
                        plugin.resetPendingStart();
                    }
                }
            }
        });

        // Create the status panel for top of window
        JPanel statusPanel = createStatusPanel();

        // Create main content area using a better layout
        JPanel mainContent = createMainContentPanel();
        
        // Add status panel to the top of the window
        add(statusPanel, BorderLayout.NORTH);
        
        // Add main content to the center of the window
        add(mainContent, BorderLayout.CENTER);
        
        // Add tab change listener to sync selection
        tabbedPane.addChangeListener(e -> {
            // When switching to Conditions tab, ensure the condition panel shows the currently selected plugin
            if (tabbedPane.getSelectedIndex() == 1) { // Stop Conditions tab
                PluginScheduleEntry selected = tablePanel.getSelectedPlugin();     
                if (selected == null) {
                 
                    log.info("No plugin selected for editing. {} but plugin");
                    return;
                }                           
                stopConditionPanel.setSelectScheduledPlugin(selected);                                                
            }
        });
        
        // Add table selection listener
        //tablePanel.addSelectionListener(this::onPluginSelected);
        // Modify the existing table selection listener to update ComboBox
        tablePanel.addSelectionListener(pluginEntry -> {
            onPluginSelected(pluginEntry);
            // Synchronize form panel ComboBox with table selection
            if (plugin != null) {
                formPanel.syncWithTableSelection(pluginEntry);
            }
        });
        // Create refresh timer to update info panel
        refreshTimer = new Timer(1000, e -> infoPanel.refresh());
        
        // Start timer when window is opened, stop when closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                refreshTimer.start();
            }
            
            @Override
            public void windowClosing(WindowEvent e) {
                refreshTimer.stop();
            }
        });
        
        // Style all comboboxes in the UI
        styleAllComboBoxes(this);
        
        // Initialize with data
        refresh();
        updateButtonState();
    }

    /**
     * Recursively styles all JComboBoxes found in the container and its children
     */
    private void styleAllComboBoxes(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComboBox) {
                SchedulerUIUtils.styleComboBox((JComboBox<?>) component);
            }
            if (component instanceof Container) {
                styleAllComboBoxes((Container) component);
            }
        }
    }

    /**
     * Creates the status panel with control buttons and current scheduler status
     */
    private JPanel createStatusPanel() {
        // Create status panel with scheduler controls and current status
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        
        // Add scheduler state indicator
        JPanel statePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        JLabel stateLabel = new JLabel("Scheduler Status:");
        stateLabel.setForeground(Color.WHITE);
        stateLabel.setFont(FontManager.getRunescapeBoldFont());
        statePanel.add(stateLabel);
        
        JLabel stateValueLabel = new JLabel(plugin.getCurrentState().getDisplayName());
        stateValueLabel.setForeground(plugin.getCurrentState().getColor());
        stateValueLabel.setFont(FontManager.getRunescapeBoldFont());
        statePanel.add(stateValueLabel);
        
        // Add control buttons on the right
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Create run scheduler button
        runSchedulerButton = new JButton("Run Scheduler");
        runSchedulerButton.setBackground(new Color(76, 175, 80));
        runSchedulerButton.setForeground(Color.WHITE);
        runSchedulerButton.setFocusPainted(false);
        runSchedulerButton.addActionListener(e -> {
            plugin.startScheduler();
            updateButtonState();
            stateValueLabel.setText(plugin.getCurrentState().getDisplayName());
            stateValueLabel.setForeground(plugin.getCurrentState().getColor());
        });
        
        // Create stop scheduler button
        stopSchedulerButton = new JButton("Stop Scheduler");
        stopSchedulerButton.setBackground(new Color(244, 67, 54));
        stopSchedulerButton.setForeground(Color.WHITE);
        stopSchedulerButton.setFocusPainted(false);
        stopSchedulerButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                plugin.stopScheduler();                
            });
            //Mcrobot.getClientThread().runOnClientThreadOptional(() -> {
                // Update the state label after stopping
                
               // plugin.stopScheduler();      
                //return true;
            //}); 
            updateButtonState();
            stateValueLabel.setText(plugin.getCurrentState().getDisplayName());
            stateValueLabel.setForeground(plugin.getCurrentState().getColor());
        });
        
        controlPanel.add(runSchedulerButton);
        controlPanel.add(stopSchedulerButton);
        
        statusPanel.add(statePanel, BorderLayout.WEST);
        statusPanel.add(controlPanel, BorderLayout.EAST);
        
        return statusPanel;
    }

    /**
     * Creates the main content panel with improved layout
     */
    private JPanel createMainContentPanel() {
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Create a more flexible tabbed pane structure
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        tabbedPane.setForeground(Color.WHITE);
        
        // Schedule tab - Split into table (top) and form (bottom) with adjustable divider
        JPanel scheduleTab = new JPanel(new BorderLayout());
        scheduleTab.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Create a split pane for the table and form - use VERTICAL layout for better table visibility
        JSplitPane scheduleSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        scheduleSplitPane.setTopComponent(tablePanel);
        scheduleSplitPane.setBottomComponent(formPanel);
        scheduleSplitPane.setResizeWeight(0.7); // Give 50% space to the table on top
        scheduleSplitPane.setDividerLocation(350); // Set initial divider position
        scheduleSplitPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scheduleTab.add(scheduleSplitPane, BorderLayout.CENTER);
        
        // Stop Conditions tab
        JPanel stopConditionsTab = new JPanel(new BorderLayout());
        stopConditionsTab.setBackground(ColorScheme.DARKER_GRAY_COLOR);
       
        stopConditionsTab.add(stopConditionPanel, BorderLayout.CENTER);
        
        // Add tabs to tabbed pane
        tabbedPane.addTab("Schedule", scheduleTab);
        tabbedPane.addTab("Stop Conditions", stopConditionsTab);
        
        // Create a split pane for the tabs and info panel
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(tabbedPane);
        mainSplitPane.setRightComponent(infoPanel);
        mainSplitPane.setResizeWeight(0.75); // Favor the main content
        mainSplitPane.setDividerLocation(800);
        mainSplitPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        mainContent.add(mainSplitPane, BorderLayout.CENTER);
        return mainContent;
    }

    private void updateButtonState() {
        SchedulerState state = plugin.getCurrentState();
        boolean isActive = plugin.isSchedulerActive();
        
        // Only enable run button if we're in READY or HOLD state
        runSchedulerButton.setEnabled(!isActive && (state == SchedulerState.READY || state == SchedulerState.HOLD));
        
        runSchedulerButton.setToolTipText(
            !runSchedulerButton.isEnabled() ? 
            "Scheduler cannot be started in " + state.getDisplayName() + " state" :
            "Start running the scheduler");
        
        // Only enable stop button if scheduler is active
        stopSchedulerButton.setEnabled(isActive);
        stopSchedulerButton.setToolTipText(
            isActive ? "Stop the scheduler" : "Scheduler is not running");
        
        // Add a status indication
        this.setTitle("Plugin Scheduler - " + state.getDisplayName());
    }

    public void refresh() {
        tablePanel.refreshTable();
        if (formPanel != null) {
            formPanel.updateControlButton();
        }
        updateButtonState();
        if (stopConditionPanel != null) {
            stopConditionPanel.refreshConditions();            
        }
        
        // Refresh info panel
        if (infoPanel != null) {
            infoPanel.refresh();
        }
    }

    private void onPluginSelected(PluginScheduleEntry plugin) {        
        if (tablePanel.getRowCount() == 0) {            
            log.info("No plugins in table.");
            return;
        }
        PluginScheduleEntry selected = tablePanel.getSelectedPlugin();   
        if (selected == null) {            
            formPanel.setEditMode(false);
            if (tabbedPane.getSelectedIndex() != 1) { // Stop Conditions tab
                stopConditionPanel.setSelectScheduledPlugin(null);
            }
            if (plugin == null) {
                log.info("No plugin selected for editing. and plugin null");
                return;
            }
            log.info("No plugin selected for editing. {} but plugin", plugin.getCleanName());
            return;
        }
        if (selected != plugin && selected != null) {            
            formPanel.loadPlugin(plugin);
            formPanel.setEditMode(true);    
            log.info("selection found");
            stopConditionPanel.setSelectScheduledPlugin(selected);                                    
        } else {                                        
         
        }
    }

    private void onAddPlugin() {
        PluginScheduleEntry scheduledPlugin = formPanel.getPluginFromForm();
        if (scheduledPlugin == null) return;
        
        LogicalCondition pluginCond= scheduledPlugin.getStopConditionManager().getPluginCondition();
        if (pluginCond != null) {
            log.info("Plugin condition: " + pluginCond.getDescription());
        }else {
            log.info("No plugin condition set.");
        }

        scheduledPlugin.logConditionInfo(scheduledPlugin.getStopConditions(),
                            "onAddPlugin Stop Conditions: " + scheduledPlugin.getName(), 
                            true);

        // Check if the plugin has stop conditions
        if (scheduledPlugin.getStopConditionManager().getConditions().isEmpty()) {
            // No stop conditions set, show warning and switch to conditions tab
            int result = JOptionPane.showConfirmDialog(this,
                    "No stop conditions are set. The plugin will run until manually stopped.\n" +
                    "Would you like to configure stop conditions now?",
                    "No Stop Conditions",
                    JOptionPane.YES_NO_CANCEL_OPTION);
                    
            if (result == JOptionPane.YES_OPTION) {
                // Add the plugin first so we can set conditions on it
                scheduledPlugin.setEnabled(false); // Set to disabled by default
                plugin.addScheduledPlugin(scheduledPlugin);
                plugin.saveScheduledPlugins();
                refresh();
                log.info("Plugin added without conditions: row count" + tablePanel.getRowCount());
                // Select the newly added plugin
                tablePanel.selectPlugin(scheduledPlugin);
                
                // Switch to conditions tab
                tabbedPane.setSelectedIndex(1);                
                return;
            } else if (result == JOptionPane.CANCEL_OPTION) {
                scheduledPlugin.setEnabled(false); // Set to disabled by default
                return; // Cancel the operation
            }
            // If NO, continue with adding plugin without conditions
        }
        
        scheduledPlugin.setEnabled(false); // Set to disabled by default
        plugin.addScheduledPlugin(scheduledPlugin);
        plugin.saveScheduledPlugins();
        
        refresh();
        formPanel.clearForm();
    }

    private void onUpdatePlugin() {
        PluginScheduleEntry selectedPlugin = tablePanel.getSelectedPlugin();
        if (selectedPlugin == null) {
            return;
        }
        
        try {
            // Get the updated plugin configuration from the form
            PluginScheduleEntry updatedConfig = formPanel.getPluginFromForm();
            
            // Update the selected plugin's properties but keep its identity
            selectedPlugin.setName(updatedConfig.getName());
            selectedPlugin.setEnabled(updatedConfig.isEnabled());
            selectedPlugin.setAllowRandomScheduling(updatedConfig.isAllowRandomScheduling());
            selectedPlugin.setPriority(updatedConfig.getPriority());
            selectedPlugin.setDefault(updatedConfig.isDefault());
              // Get the main time condition from the updated config
            TimeCondition newTimeCondition = updatedConfig.getMainTimeStartCondition();
            
            selectedPlugin.updatePrimaryTimeCondition((TimeCondition) newTimeCondition);            
            // Update the UI
            plugin.saveScheduledPlugins();
            tablePanel.refreshTable();
            formPanel.setEditMode(false);
            formPanel.clearForm();
            tablePanel.clearSelection();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this,
                "Error updating plugin: " + e.getMessage(),
                "Update Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void onRemovePlugin() {
        PluginScheduleEntry _plugin = tablePanel.getSelectedPlugin();
        if (_plugin != null) {
            plugin.removeScheduledPlugin(_plugin);
            tablePanel.refreshTable();
            formPanel.clearForm();
            
            log.info("onRemovePlugin: " + _plugin.getName());
            stopConditionPanel.setSelectScheduledPlugin(null);
        }

        plugin.saveScheduledPlugins();
    }
    
    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        super.dispose();
    }

    /**
     * Selects a plugin in the table
     */
    public void selectPlugin(PluginScheduleEntry plugin) {
        if (tablePanel != null) {
            tablePanel.selectPlugin(plugin);
        }
    }

    /**
     * Switches to the stop conditions tab
     */
    public void switchToStopConditionsTab() {
        if (tabbedPane != null) {
            tabbedPane.setSelectedIndex(1); // Switch to stop conditions tab
        }
    }
}
