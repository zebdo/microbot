package net.runelite.client.plugins.microbot.pluginscheduler.ui.PluginScheduleEntry;

import net.runelite.client.plugins.microbot.pluginscheduler.type.PluginScheduleEntry;
import net.runelite.client.plugins.microbot.pluginscheduler.SchedulerPlugin;

import net.runelite.client.plugins.microbot.pluginscheduler.condition.Condition;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
@Slf4j
public class ScheduleTablePanel extends JPanel {
    private final SchedulerPlugin schedulerPlugin;
    private final JTable scheduleTable;
    private final DefaultTableModel tableModel;
    private Consumer<PluginScheduleEntry> selectionListener;
    private boolean updatingTable = false;
    // Colors for different row states
    private static final Color CURRENT_PLUGIN_COLOR = new Color(76, 175, 80, 100); // Green with transparency
    private static final Color NEXT_PLUGIN_COLOR = new Color(255, 193, 7, 70); // Amber with transparency
    private static final Color SELECTION_COLOR = new Color(0, 120, 215, 180); // Blue with transparency
    private static final Color CONDITION_MET_COLOR = new Color(76, 175, 80, 70); // Green with transparency
    private static final Color CONDITION_NOT_MET_COLOR = new Color(244, 67, 54, 70); // Red with transparency
    
    private List<PluginScheduleEntry> rowToPluginMap = new ArrayList<>();
    public int getRowCount() {
        if (tableModel == null) {
            return 0;
        }
        return tableModel.getRowCount();
    }

    public ScheduleTablePanel(SchedulerPlugin schedulerPlugin) {
        this.schedulerPlugin = schedulerPlugin;
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARK_GRAY_COLOR),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ),
                "Scheduled Plugins",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                FontManager.getRunescapeBoldFont()
        ));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Update the table model to restrict editing of Default column for default plugins
        tableModel = new DefaultTableModel(
            new Object[]{"Plugin", "Schedule", "Next Run", "Start Conditions", "Stop Conditions", "Priority", "Default", "Enabled", "Random", "Runs"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 6 || column == 7 || column == 8) return Boolean.class; // Boolean columns
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                // Default column is 6
                if (column == 6) {
                    // Check if this is a default plugin
                    if (row >= 0 && row < schedulerPlugin.getScheduledPlugins().size()) {
                        PluginScheduleEntry scheduled = schedulerPlugin.getScheduledPlugins().get(row);
                        
                        // More robust detection - check the isDefault flag directly
                        // Also check interval condition with very short duration (1 second)
                        if (scheduled.isDefault()) {
                            return false;
                        }
                        
                        // Secondary check for legacy plugins created as "Run Default"
                        String scheduleDisplay = scheduled.getIntervalDisplay();
                        if ("No schedule defined".equals(scheduleDisplay) || 
                            scheduled.getIntervalDisplay().contains("Every 1 second")) {
                            // Also update the isDefault flag for consistency
                            scheduled.setDefault(true);
                            return false;
                        }
                    }
                }
                
                return column == 6 || column == 7 || column == 8; // Only Default, Enabled, and Random columns are editable
            }
        };
        
        // Update the table model listener to handle Default column changes
        tableModel.addTableModelListener(e -> {
            if (updatingTable) {                
                return; // Skip processing if we're already updating or it's not our columns
            }            
            if (e.getColumn() == 6 || e.getColumn() == 7 || e.getColumn() == 8) {                
                try {
                    updatingTable = true;
                    int firstRow = e.getFirstRow();
                    int lastRow = e.getLastRow();
                    
                    // Update all rows in the affected range
                    for (int row = firstRow; row <= lastRow; row++) {
                        if (row >= 0 && row < schedulerPlugin.getScheduledPlugins().size()) {
                            PluginScheduleEntry scheduled = schedulerPlugin.getScheduledPlugins().get(row);
                            
                            if (e.getColumn() == 6) { // Default column
                                Boolean isDefault = (Boolean) tableModel.getValueAt(row, 6);
                                
                                // Check if this is a default plugin that shouldn't be modified
                                if (scheduled.isDefault() || scheduled.getIntervalDisplay().contains("Every 1 second") || 
                                    "No schedule defined".equals(scheduled.getIntervalDisplay())) {
                                    // Reset the value and update the isDefault flag for consistency
                                    
                                    tableModel.setValueAt(true, row, 6);
                                    
                                    scheduled.setDefault(true);
                                } else {
                                    // For non-default plugins, update the value
                                    scheduled.setDefault(isDefault);
                                    
                                    // If being set to default, also set priority to 0
                                    if (isDefault) {
                                        scheduled.setPriority(0);
                                        tableModel.setValueAt(0, row, 5); // Update priority column
                                    }
                                }
                            }
                            else if (e.getColumn() == 7) { // Enabled column
                                Boolean enabled = (Boolean) tableModel.getValueAt(row, 7);
                                tableModel.setValueAt(enabled, row, 7);
                                
                                scheduled.setEnabled(enabled);
                            }
                            else if (e.getColumn() == 8) { // Random scheduling column
                                Boolean allowRandom = (Boolean) tableModel.getValueAt(row, 8);
                                tableModel.setValueAt(allowRandom, row, 8);
                                scheduled.setAllowRandomScheduling(allowRandom);
                            }
                        }
                    }
                    
                    // Save after all updates are done
                    //schedulerPlugin.saveScheduledPlugins();
                    // Refresh the table to update visual indicators
                    //SwingUtilities.invokeLater(this::refreshTable);                
                    updatingTable = false;
                    //log.info("table value changed ");
                }finally {
                    updatingTable = false;
                }
            }
        });

        // Create table with custom styling
        scheduleTable = new JTable(tableModel);
        scheduleTable.setFillsViewportHeight(true);
        scheduleTable.setRowHeight(30);
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.setShowGrid(false);
        scheduleTable.setIntercellSpacing(new Dimension(0, 0));
        scheduleTable.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scheduleTable.setForeground(Color.WHITE);

        // Add mouse listener to handle clicks outside the table data
        scheduleTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = scheduleTable.rowAtPoint(e.getPoint());
                int col = scheduleTable.columnAtPoint(e.getPoint());

                // If clicked outside the table data area, clear selection
                if (row == -1 || col == -1) {
                    clearSelection();
                }
            }
        });

        // Style the table header
        JTableHeader header = scheduleTable.getTableHeader();
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setForeground(Color.WHITE);
        header.setFont(FontManager.getRunescapeBoldFont());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.LIGHT_GRAY_COLOR));

        // Add mouse listener to header to clear selection when clicked
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clearSelection();
            }
        });

        // Set column widths
        scheduleTable.getColumnModel().getColumn(0).setPreferredWidth(160); // Plugin
        scheduleTable.getColumnModel().getColumn(1).setPreferredWidth(130); // Schedule
        scheduleTable.getColumnModel().getColumn(2).setPreferredWidth(130); // Next Run
        scheduleTable.getColumnModel().getColumn(3).setPreferredWidth(90);  // Start Conditions
        scheduleTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // Stop Conditions
        scheduleTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // Priority
        scheduleTable.getColumnModel().getColumn(6).setPreferredWidth(60);  // Default
        scheduleTable.getColumnModel().getColumn(7).setPreferredWidth(60);  // Enabled
        scheduleTable.getColumnModel().getColumn(8).setPreferredWidth(60);  // Random
        scheduleTable.getColumnModel().getColumn(9).setPreferredWidth(50);  // Run Count
        DefaultTableCellRenderer tooltipRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (row >= 0 && row < schedulerPlugin.getScheduledPlugins().size()) {
                    PluginScheduleEntry entry = schedulerPlugin.getScheduledPlugins().get(row);
                    
                    // Set tooltips based on column
                    if (column == 1) { // Schedule
                        setToolTipText(getScheduleTooltip(entry));
                    } else if (column == 2) { // Next Run
                        setToolTipText(getNextRunTooltip(entry));
                    } else if (column == 3) { // Start Conditions
                        setToolTipText(getStartConditionsTooltip(entry));
                    } else if (column == 4) { // Stop Conditions
                        setToolTipText(getStopConditionsTooltip(entry));
                    }
                }
                
                return c;
            }
        };
        scheduleTable.getColumnModel().getColumn(1).setCellRenderer(tooltipRenderer);
        scheduleTable.getColumnModel().getColumn(2).setCellRenderer(tooltipRenderer);
        scheduleTable.getColumnModel().getColumn(3).setCellRenderer(tooltipRenderer);
        scheduleTable.getColumnModel().getColumn(4).setCellRenderer(tooltipRenderer);

        // Custom cell renderer for the condition columns
        DefaultTableCellRenderer conditionRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (row >= 0 && row < schedulerPlugin.getScheduledPlugins().size() && !isSelected) {
                    PluginScheduleEntry entry = schedulerPlugin.getScheduledPlugins().get(row);
                    
                    // Apply background color based on condition status
                    if (column == 3) { // Start conditions
                        boolean hasStartConditions = entry.hasAnyStartConditions();
                        boolean startConditionsMet = hasStartConditions && entry.getStartConditionManager().areConditionsMet();
                        
                        if (hasStartConditions) {
                            c.setBackground(startConditionsMet ? CONDITION_MET_COLOR : CONDITION_NOT_MET_COLOR);
                            c.setForeground(Color.BLACK);
                        }
                    } else if (column == 4) { // Stop conditions
                        boolean hasStopConditions = entry.hasAnyStopConditions();
                        boolean stopConditionsMet = hasStopConditions && entry.getStopConditionManager().areConditionsMet();
                        
                        if (hasStopConditions) {
                            c.setBackground(stopConditionsMet ? CONDITION_MET_COLOR : CONDITION_NOT_MET_COLOR);
                            c.setForeground(Color.BLACK);
                        }
                    }
                }
                
                return c;
            }
        };

        // Apply the condition renderer to the condition columns
        scheduleTable.getColumnModel().getColumn(3).setCellRenderer(conditionRenderer);
        scheduleTable.getColumnModel().getColumn(4).setCellRenderer(conditionRenderer);
        // Custom cell renderer for alternating row colors and special highlights
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (row >= 0 && row < schedulerPlugin.getScheduledPlugins().size()) {
                    PluginScheduleEntry rowPlugin = schedulerPlugin.getScheduledPlugins().get(row);
                    
                    if (isSelected) {
                        // Selected row styling takes precedence - use a distinct blue color
                        c.setBackground(SELECTION_COLOR);
                        c.setForeground(Color.WHITE);
                    } 
                    else if (rowPlugin.isRunning() && schedulerPlugin.getCurrentPlugin() != null && 
                             rowPlugin.getName().equals(schedulerPlugin.getCurrentPlugin().getName())) {
                        // Currently running plugin
                        c.setBackground(CURRENT_PLUGIN_COLOR);
                        c.setForeground(Color.BLACK);
                    }
                    else if (isNextToRun(rowPlugin)) {
                        // Next plugin to run
                        c.setBackground(NEXT_PLUGIN_COLOR);
                        c.setForeground(Color.BLACK);
                    }
                    else {
                        // Normal alternating row colors
                        c.setBackground(row % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.DARK_GRAY_COLOR);
                        c.setForeground(Color.WHITE);
                    }
                }

                setBorder(new EmptyBorder(2, 5, 2, 5));
                return c;
            }
        };

        renderer.setHorizontalAlignment(SwingConstants.LEFT);

        // Apply renderer to all columns except the boolean column
        for (int i = 0; i < scheduleTable.getColumnCount() - 1; i++) {
            scheduleTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Add table to scroll pane with custom styling
        JScrollPane scrollPane = new JScrollPane(scheduleTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Style the scrollbar
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Add mouse listener to the scroll pane to clear selection when clicking empty space
        scrollPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clearSelection();
            }
        });

        add(scrollPane, BorderLayout.CENTER);
        
        // Add this to the constructor after adding the scrollPane
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        legendPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        legendPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Current plugin indicator
        JPanel currentPluginIndicator = new JPanel();
        currentPluginIndicator.setBackground(CURRENT_PLUGIN_COLOR);
        currentPluginIndicator.setPreferredSize(new Dimension(15, 15));
        legendPanel.add(currentPluginIndicator);
        JLabel currentPluginLabel = new JLabel("Currently running");
        currentPluginLabel.setForeground(Color.WHITE);
        currentPluginLabel.setFont(FontManager.getRunescapeSmallFont());
        legendPanel.add(currentPluginLabel);

        // Next plugin indicator
        JPanel nextPluginIndicator = new JPanel();
        nextPluginIndicator.setBackground(NEXT_PLUGIN_COLOR);
        nextPluginIndicator.setPreferredSize(new Dimension(15, 15));
        legendPanel.add(nextPluginIndicator);
        JLabel nextPluginLabel = new JLabel("Next to run");
        nextPluginLabel.setForeground(Color.WHITE);
        nextPluginLabel.setFont(FontManager.getRunescapeSmallFont());
        legendPanel.add(nextPluginLabel);

        // Add the legend panel to the bottom of the main panel
        add(legendPanel, BorderLayout.SOUTH);
    }
        /**
     * Creates a detailed tooltip for schedule information
     */
    private String getScheduleTooltip(PluginScheduleEntry entry) {
        StringBuilder tooltip = new StringBuilder("<html><b>Schedule Details:</b><br>");
        
        // Add schedule type and details
        tooltip.append(entry.getIntervalDisplay());
        
        if (entry.hasAnyOneTimeStartConditions()) {
            tooltip.append("<br>One-time schedule");
            if (entry.hasTriggeredOneTimeStartConditions() && !entry.canStartTriggerAgain()) {
                tooltip.append(" (completed)");
            }
        }
        
        // Add priority information
        tooltip.append("<br><br><b>Priority:</b> ").append(entry.getPriority());
        if (entry.isDefault()) {
            tooltip.append(" (Default plugin)");
        }
        
        // Add random scheduling info
        tooltip.append("<br><b>Random Scheduling:</b> ").append(entry.isAllowRandomScheduling() ? "Enabled" : "Disabled");
        
        return tooltip.toString() + "</html>";
    }
    /**
     * Creates a detailed tooltip for start conditions
     */
    private String getStartConditionsTooltip(PluginScheduleEntry entry) {
        StringBuilder tooltip = new StringBuilder("<html><b>Start Conditions:</b><br>");
        
        List<Condition> conditions = entry.getStartConditionManager().getConditions();
        if (conditions.isEmpty()) {
            tooltip.append("No start conditions defined");
        } else {
            tooltip.append("Logic: ").append(entry.getStartConditionManager().requiresAll() ? "ALL conditions must be met" : "ANY condition can be met");
            
            tooltip.append("<br><br>Conditions:<br>");
            for (Condition condition : conditions) {
                tooltip.append("• ")
                    .append(condition.isSatisfied() ? "✓ " : "✗ ")
                    .append(condition.getDescription())
                    .append("<br>");
            }
            
            // Add information about one-time conditions
            if (entry.hasAnyOneTimeStartConditions()) {
                tooltip.append("<br>Contains one-time trigger conditions");
                if (!entry.canStartTriggerAgain()) {
                    tooltip.append(" (cannot trigger again)");
                }
            }
        }
        
        return tooltip.toString() + "</html>";
    }

    /**
 * Creates a detailed tooltip for stop conditions
 */
private String getStopConditionsTooltip(PluginScheduleEntry entry) {
    StringBuilder tooltip = new StringBuilder("<html><b>Stop Conditions:</b><br>");
    
    List<Condition> conditions = entry.getStopConditionManager().getConditions();
    if (conditions.isEmpty()) {
        tooltip.append("No stop conditions defined");
    } else {
        tooltip.append("Logic: ").append(entry.getStopConditionManager().requiresAll() ? "ALL conditions must be met" : "ANY condition can be met");
        
        // Add progress if running
        if (entry.isRunning()) {
            double progress = entry.getStopConditionProgress();
            tooltip.append("<br>Progress: ").append(String.format("%.1f%%", progress));
        }
        
        tooltip.append("<br><br>Conditions:<br>");
        for (Condition condition : conditions) {
            tooltip.append("• ")
                  .append(condition.isSatisfied() ? "✓ " : "✗ ")
                  .append(condition.getDescription());
            
            // Add progress for individual conditions if available
            double condProgress = condition.getProgressPercentage();
            if (condProgress > 0 && condProgress < 100) {
                tooltip.append(String.format(" (%.1f%%)", condProgress));
            }
            tooltip.append("<br>");
        }
        
        // Add information about one-time conditions
        if (entry.hasAnyOneTimeStopConditions()) {
            tooltip.append("<br>Contains one-time trigger conditions");
            if (!entry.canStopTriggerAgain()) {
                tooltip.append(" (cannot trigger again)");
            }
        }
    }
    
    return tooltip.toString() + "</html>";
}
    /**
     * Creates a detailed tooltip for next run information
     */
    private String getNextRunTooltip(PluginScheduleEntry entry) {
        StringBuilder tooltip = new StringBuilder("<html><b>Next Run Details:</b><br>");
        
        // Add next run time
        Optional<ZonedDateTime> nextTime = entry.getNextStartTriggerTime();
        if (nextTime.isPresent()) {
            tooltip.append("Next scheduled time: ").append(nextTime.get().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else {
            tooltip.append("No specific next run time");
        }
        
        // Add completed runs
        tooltip.append("<br>Times run: ").append(entry.getRunCount());
        
        // Add enabled status
        tooltip.append("<br><b>Status:</b> ").append(entry.isEnabled() ? "Enabled" : "Disabled");
        if (entry.isRunning()) {
            tooltip.append(" (Currently running)");
        }
        
        return tooltip.toString() + "</html>";
    }
   
    /**
     * Determines if the provided plugin is the next one scheduled to run
     */
    private boolean isNextToRun(PluginScheduleEntry scheduledPlugin) {
        if (!scheduledPlugin.isEnabled()) {
            return false;
        }
        
        PluginScheduleEntry nextPlugin = schedulerPlugin.getNextScheduledPlugin();
        return nextPlugin != null && nextPlugin.equals(scheduledPlugin);
    }
    public List<PluginScheduleEntry> sortPluginScheduleEntries(){
        List<PluginScheduleEntry> plugins = schedulerPlugin.getScheduledPlugins();
        plugins.sort((p1, p2) -> {
            // First sort by enabled status (enabled plugins first)
            if (p1.isEnabled() != p2.isEnabled()) {
                return p1.isEnabled() ? -1 : 1;
            }
            
            // For running plugins, prioritize them at the top
            // IMPORTANT: Check if it's THE actual running plugin instance, not just same name
            boolean p1IsCurrentRunning = schedulerPlugin.getCurrentPlugin() == p1; // Reference equality, not name equality
            boolean p2IsCurrentRunning = schedulerPlugin.getCurrentPlugin() == p2;
            
            if (p1IsCurrentRunning != p2IsCurrentRunning) {
                return p1IsCurrentRunning ? -1 : 1;
            }
            
            // If both are enabled or both disabled, sort by next run time
            Optional<ZonedDateTime> nextTime1 = p1.getNextStartTriggerTime();
            Optional<ZonedDateTime> nextTime2 = p2.getNextStartTriggerTime();
            
            if (nextTime1.isPresent() && nextTime2.isPresent()) {
                int timeCompare = nextTime1.get().compareTo(nextTime2.get());
                if (timeCompare != 0) return timeCompare;
            } else if (nextTime1.isPresent()) {
                return -1;
            } else if (nextTime2.isPresent()) {
                return 1;
            }
            
            // When times are equal or both absent, use name as a stable tiebreaker
            return p1.getName().compareTo(p2.getName());
        });
        return plugins;
    }
    public void refreshTable() {       
        
        
        // Save current selection
        PluginScheduleEntry selectedPlugin = getSelectedPlugin();
        int selectedRow = scheduleTable.getSelectedRow();
        
        // Get current plugins and sort them by next run time
        List<PluginScheduleEntry> sortedPlugins = sortPluginScheduleEntries();
        
        // Save the current row map for comparison
        List<PluginScheduleEntry> previousRowMap = new ArrayList<>(rowToPluginMap);
        
        // Clear our tracking list
        rowToPluginMap.clear();
        
        // Create a set to track which rows we've handled
        Set<Integer> handledRows = new HashSet<>();
        rowToPluginMap = new ArrayList<>(sortedPlugins.size()); // Pre-allocate correct size
        for (int i = 0; i < sortedPlugins.size(); i++) {
            rowToPluginMap.add(null); // Initialize with nulls
        }
        // First pass: update existing rows (maintain object identity where possible)
        for (int newIndex = 0; newIndex < sortedPlugins.size(); newIndex++) {
            PluginScheduleEntry plugin = sortedPlugins.get(newIndex);
            
            // Try to find existing row with the same plugin object (reference equality)
            boolean found = false;
            for (int oldIndex = 0; oldIndex < previousRowMap.size(); oldIndex++) {
                if (previousRowMap.get(oldIndex) == plugin) { // Reference equality
                    if (oldIndex != newIndex && oldIndex < tableModel.getRowCount()) {
                        // Plugin exists but in wrong position - move it
                        if (newIndex < tableModel.getRowCount()) {
                            // Move the row data
                            Object[] rowData = new Object[tableModel.getColumnCount()];
                            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                                rowData[col] = tableModel.getValueAt(oldIndex, col);
                            }
                            
                            // Update the row with fresh data
                            updateRowData(rowData, plugin);
                            
                            // Remove from old position and insert at new position
                            tableModel.removeRow(oldIndex);
                            tableModel.insertRow(newIndex, rowData);
                            
                            // Adjust indices for removed row
                            if (oldIndex < newIndex) {
                                newIndex--;
                            }
                        } else {
                            // Update row data in place
                            updateRowWithPlugin(oldIndex, plugin);
                        }
                    } else if (oldIndex < tableModel.getRowCount()) {
                        // Position is correct, just update data
                        updateRowWithPlugin(oldIndex, plugin);
                    }
                    
                    // Mark as handled
                    handledRows.add(oldIndex);
                    found = true;
                    break;
                }
            }
            
            // If not found by reference, add as new
            if (!found) {
                if (newIndex < tableModel.getRowCount()) {
                    // Insert at this position
                    tableModel.insertRow(newIndex, createRowData(plugin));
                } else {
                    // Add to the end
                    tableModel.addRow(createRowData(plugin));
                }
            }
            rowToPluginMap.set(newIndex, plugin); // Set at the exact position that matches table row
            
        }
        
        // Remove any rows that no longer correspond to plugins
        for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
            if (i >= rowToPluginMap.size()) {
                tableModel.removeRow(i);
            }
        }
        
        // Restore selection if possible
        if (selectedPlugin != null) {
            selectPlugin(selectedPlugin);
        }
    }

    /**
     * Updates row data array with current plugin values
     */
    private void updateRowData(Object[] rowData, PluginScheduleEntry plugin) {
        // Get basic information
        String pluginName = plugin.getCleanName();
        
        if (schedulerPlugin.isRunningEntry(plugin)) {
            pluginName = "▶ " + pluginName;
        }
        
        // Update row data
        rowData[0] = pluginName;
        rowData[1] = getEnhancedScheduleDisplay(plugin);
        rowData[2] = getEnhancedNextRunDisplay(plugin);
        rowData[3] = getStartConditionInfo(plugin);
        rowData[4] = getStopConditionInfo(plugin);
        rowData[5] = plugin.getPriority();
        rowData[6] = plugin.isDefault();
        rowData[7] = plugin.isEnabled();
        rowData[8] = plugin.isAllowRandomScheduling();
        rowData[9] = plugin.getRunCount();
    }

    /**
     * Updates existing row in the table with current plugin values
     */
    private void updateRowWithPlugin(int rowIndex, PluginScheduleEntry plugin) {
        // Get basic information
        String pluginName = plugin.getCleanName();
        
        if (schedulerPlugin.isRunningEntry(plugin)) {
            pluginName = "▶ " + pluginName;
        }        
        // Update existing row
        tableModel.setValueAt(pluginName, rowIndex, 0);
        tableModel.setValueAt(getEnhancedScheduleDisplay(plugin), rowIndex, 1);
        tableModel.setValueAt(getEnhancedNextRunDisplay(plugin), rowIndex, 2);
        tableModel.setValueAt(getStartConditionInfo(plugin), rowIndex, 3);
        tableModel.setValueAt(getStopConditionInfo(plugin), rowIndex, 4);
        tableModel.setValueAt(plugin.getPriority(), rowIndex, 5);
        tableModel.setValueAt(plugin.isDefault(), rowIndex, 6);
        tableModel.setValueAt(plugin.isEnabled(), rowIndex, 7);
        tableModel.setValueAt(plugin.isAllowRandomScheduling(), rowIndex, 8);
        tableModel.setValueAt(plugin.getRunCount(), rowIndex, 9);
    }

    /**
     * Creates a new row data array for a plugin
     */
    private Object[] createRowData(PluginScheduleEntry plugin) {
        // Get basic information
        String pluginName = plugin.getCleanName();
        
        if (schedulerPlugin.isRunningEntry(plugin)) {
            pluginName = "▶ " + pluginName;
        }
        
        return new Object[]{
            pluginName,
            getEnhancedScheduleDisplay(plugin),
            getEnhancedNextRunDisplay(plugin),
            getStartConditionInfo(plugin),
            getStopConditionInfo(plugin),
            plugin.getPriority(),
            plugin.isDefault(),
            plugin.isEnabled(),
            plugin.isAllowRandomScheduling(),
            plugin.getRunCount()
        };
    }
    public void refreshTableOld() {
        log.info("Refreshing schedule table");
        
        // Save current selection
        PluginScheduleEntry selectedPlugin = getSelectedPlugin();
        int selectedRow = scheduleTable.getSelectedRow();
        // Get current plugins and sort them by next run time
        
        List<PluginScheduleEntry> plugins = sortPluginScheduleEntries();
        
        rowToPluginMap.clear();
        
        
               
        
         // Update table model
        tableModel.setRowCount(0);
        
        for (PluginScheduleEntry scheduled : plugins) {
            rowToPluginMap.add(scheduled);
            // Get basic information
            String pluginName = scheduled.getCleanName();
            
            
            if (schedulerPlugin.isRunningEntry(scheduled)) {
                pluginName = "▶ " + pluginName;
            }
            
            // Prepare schedule display with enhanced information
            String scheduleDisplay = getEnhancedScheduleDisplay(scheduled);
            
            // Enhanced next run display
            String nextRunDisplay = getEnhancedNextRunDisplay(scheduled);
            
            // Split condition info
            String startConditionInfo = getStartConditionInfo(scheduled);
            String stopConditionInfo = getStopConditionInfo(scheduled);
            
            // Add row to table
            tableModel.addRow(new Object[]{
                pluginName,
                scheduleDisplay,
                nextRunDisplay,
                startConditionInfo,
                stopConditionInfo,
                scheduled.getPriority(),
                scheduled.isDefault(),
                scheduled.isEnabled(),
                scheduled.isAllowRandomScheduling(),
                scheduled.getRunCount()
            });
        }
    
        // Remove excess rows if there are more rows than plugins
        while (tableModel.getRowCount() > plugins.size()) {
            tableModel.removeRow(tableModel.getRowCount() - 1);
        }
       
        // Restore selection if possible - using reference equality, not just equals()
        if (selectedPlugin != null) {
            log.info("Restoring selection for plugin: " + selectedPlugin.getName());
            
            // First try to find the exact same object reference
            for (int i = 0; i < plugins.size(); i++) {
                if (plugins.get(i) == selectedPlugin) { // Use reference equality
                    scheduleTable.setRowSelectionInterval(i, i);
                    log.info("Found selected plugin by reference equality: " + selectedPlugin.getName() + " at row " + i);                
                    return;                   
                }
            }
            
            
            // If reference equality fails, try equals() as fallback
            for (int i = 0; i < plugins.size(); i++) {
                if (plugins.get(i).equals(selectedPlugin)) {
                    log.info("Found selected plugin by equals(): " + selectedPlugin.getName() + " at row " + i);
                    scheduleTable.setRowSelectionInterval(i, i);
                    return;
                }
            }
            
            // If all else fails, try by name as a last resort
            for (int i = 0; i < plugins.size(); i++) {
                if (plugins.get(i).getName().equals(selectedPlugin.getName())) {
                    log.info("Found selected plugin by name: " + selectedPlugin.getName() + " at row " + i);
                    scheduleTable.setRowSelectionInterval(i, i);
                    return;
                }
            }
        }else {
            log.info("No selected previous plugin found.");
        }
        log.info("No selected plugin found in the table.");
    }
    /**
     * Creates a display of start condition information
     */
    private String getStartConditionInfo(PluginScheduleEntry entry) {
        int startTotal = entry.getStartConditionManager().getConditions().size();
        if (startTotal == 0) {
            return "None";
        }
        
        int startMet = (int) entry.getStartConditionManager().getConditions().stream()
            .filter(Condition::isSatisfied).count();
        
        StringBuilder info = new StringBuilder();
        info.append(startMet).append("/").append(startTotal);
        
        // Add type indicator
        boolean startRequiresAll = entry.getStartConditionManager().requiresAll();
        info.append(startRequiresAll ? " (ALL)" : " (ANY)");
        
        return info.toString();
    }

    /**
     * Creates a display of stop condition information
     */
    private String getStopConditionInfo(PluginScheduleEntry entry) {
        int stopTotal = entry.getStopConditionManager().getConditions().size();
        if (stopTotal == 0) {
            return "None";
        }
        
        int stopMet = (int) entry.getStopConditionManager().getConditions().stream()
            .filter(Condition::isSatisfied).count();
        
        StringBuilder info = new StringBuilder();
        info.append(stopMet).append("/").append(stopTotal);
        
        // Add type indicator
        boolean stopRequiresAll = entry.getStopConditionManager().requiresAll();
        info.append(stopRequiresAll ? " (ALL)" : " (ANY)");
        
        // Add progress for running plugins
        if (entry.isRunning()) {
            double progress = entry.getStopConditionProgress();
            if (progress > 0) {
                info.append(String.format(" %.0f%%", progress));
            }
        }
        
        return info.toString();
    }

    /**
     * Creates an enhanced display of schedule information
     */
    private String getEnhancedScheduleDisplay(PluginScheduleEntry entry) {
        // Check for one-time schedule first
        boolean isOneTime = entry.hasAnyOneTimeStartConditions();
        
        // If it's a one-time schedule that's already triggered, show completion status
        if (isOneTime && entry.hasTriggeredOneTimeStartConditions() && !entry.canStartTriggerAgain()) {
            return "One-time (Completed)";
        }
        
        // Get the base interval display
        String baseDisplay = entry.getIntervalDisplay();
        
        // For one-time schedules, add an indicator
        if (isOneTime) {
            return "One-time: " + baseDisplay;
        }
        
        return baseDisplay;
    }

    /**
     * Creates an enhanced display of the next run time
     */
    private String getEnhancedNextRunDisplay(PluginScheduleEntry entry) {
        // Use the plugin's optimized display method
        String baseDisplay = entry.getNextRunDisplay();
        
        // Add extra information for one-time entries that can't run again
        if (entry.hasAnyOneTimeStartConditions() && !entry.canStartTriggerAgain()) {
            if (entry.getRunCount() > 0) {
                return "Completed";
            } else {
                return "Cannot run";
            }
        }
        
        return baseDisplay;
    }

    /**
     * Creates an enhanced display of condition information
     */
    private String getEnhancedConditionInfo(PluginScheduleEntry entry) {
        StringBuilder info = new StringBuilder();
        
        // Add start condition info if available
        int startTotal = entry.getStartConditionManager().getConditions().size();
        if (startTotal > 0) {
            int startMet = (int) entry.getStartConditionManager().getConditions().stream()
                .filter(Condition::isSatisfied).count();
            
            info.append("Start: ").append(startMet).append("/").append(startTotal);
            
            // Add type indicator
            boolean startRequiresAll = entry.getStartConditionManager().requiresAll();
            info.append(startRequiresAll ? " (ALL)" : " (ANY)");
        }
        
        // Add stop condition info if available
        int stopTotal = entry.getStopConditionManager().getConditions().size();
        if (stopTotal > 0) {
            if (info.length() > 0) {
                info.append(" | ");
            }
            
            int stopMet = (int) entry.getStopConditionManager().getConditions().stream()
                .filter(Condition::isSatisfied).count();
            
            info.append("Stop: ").append(stopMet).append("/").append(stopTotal);
            
            // Add type indicator
            boolean stopRequiresAll = entry.getStopConditionManager().requiresAll();
            info.append(stopRequiresAll ? " (ALL)" : " (ANY)");
            
            // Add progress for running plugins
            if (entry.isRunning()) {
                double progress = entry.getStopConditionProgress();
                if (progress > 0) {
                    info.append(String.format(" (%.0f%%)", progress));
                }
            }
        }
        
        // If no conditions, show "None"
        if (info.length() == 0) {
            return "None";
        }
        
        return info.toString();
    }

    public void addSelectionListener(Consumer<PluginScheduleEntry> listener) {
        this.selectionListener = listener;
        scheduleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = scheduleTable.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < schedulerPlugin.getScheduledPlugins().size()) {
                    listener.accept(schedulerPlugin.getScheduledPlugins().get(selectedRow));
                } else {
                    listener.accept(null);
                }
            }
        });
    }

    public PluginScheduleEntry getSelectedPlugin() {
        int selectedRow = scheduleTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < schedulerPlugin.getScheduledPlugins().size()) {
            return rowToPluginMap.get(selectedRow);
            //return schedulerPlugin.getScheduledPlugins().get(selectedRow);
        }
        return null;
    }

    /**
     * Clears the current table selection and notifies the selection listener
     */
    public void clearSelection() {
        scheduleTable.clearSelection();
        if (selectionListener != null) {
            selectionListener.accept(null);
        }
    }
    public void addAndSelect(PluginScheduleEntry pluginEntry) {
        if (pluginEntry == null) return;
        
        for (PluginScheduleEntry entry : schedulerPlugin.getScheduledPlugins()) {
            if ( pluginEntry == entry) {
                // Plugin already exists, no need to add -> no duplicate
                return;
            }
        }
        schedulerPlugin.addScheduledPlugin(pluginEntry);
        rowToPluginMap.add(pluginEntry);
        tableModel.addRow(new Object[]{
            pluginEntry.getName(),
            pluginEntry.getIntervalDisplay(),
            pluginEntry.getNextRunDisplay(),
            getStartConditionInfo(pluginEntry),
            getStopConditionInfo(pluginEntry),
            pluginEntry.getPriority(),
            pluginEntry.isDefault(),
            pluginEntry.isEnabled(),
            pluginEntry.isAllowRandomScheduling(),
            pluginEntry.getRunCount()
        });
        scheduleTable.setRowSelectionInterval(getRowCount(), getRowCount());        
    }
    /**
     * Selects the given plugin in the table
     * @param plugin The plugin to select
     */
    public void selectPlugin(PluginScheduleEntry plugin) {
        if (plugin == null) return;
        List<PluginScheduleEntry> plugins = this.rowToPluginMap;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String rowName = String.valueOf(tableModel.getValueAt(i, 0))
                .replaceAll("▶ ", ""); // Remove play indicator if present
             // First try to find the exact same object reference
             
            if (plugins.get(i) == plugin) { // Use reference equality
                scheduleTable.setRowSelectionInterval(i, i);
                // Make sure the selected row is visible
                Rectangle rect = scheduleTable.getCellRect(i, 0, true);
                scheduleTable.scrollRectToVisible(rect);
                
                // Notify listeners
                if (selectionListener != null) {
                    selectionListener.accept(plugin);
                }
                return;
            }
        }
    }
}
