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
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.time.ZonedDateTime;

public class ScheduleTablePanel extends JPanel {
    private final SchedulerPlugin plugin;
    private final JTable scheduleTable;
    private final DefaultTableModel tableModel;
    private Consumer<PluginScheduleEntry> selectionListener;
    
    // Colors for different row states
    private static final Color CURRENT_PLUGIN_COLOR = new Color(76, 175, 80, 100); // Green with transparency
    private static final Color NEXT_PLUGIN_COLOR = new Color(255, 193, 7, 70); // Amber with transparency
    private static final Color SELECTION_COLOR = new Color(0, 120, 215, 180); // Blue with transparency

    
    public ScheduleTablePanel(SchedulerPlugin plugin) {
        this.plugin = plugin;

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
            new Object[]{"Plugin", "Schedule", "Next Run", "Conditions", "Priority", "Default", "Enabled", "Random", "Runs"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 5 || column == 6 || column == 7) return Boolean.class; // Boolean columns
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                // Default column is 5
                if (column == 5) {
                    // Check if this is a default plugin
                    if (row >= 0 && row < plugin.getScheduledPlugins().size()) {
                        PluginScheduleEntry scheduled = plugin.getScheduledPlugins().get(row);
                        
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
                
                return column == 5 || column == 6 || column == 7; // Only Default, Enabled, and Random columns are editable
            }
        };

        // Update the table model listener to handle Default column changes
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 5 || e.getColumn() == 6 || e.getColumn() == 7) {
                int firstRow = e.getFirstRow();
                int lastRow = e.getLastRow();
                
                // Update all rows in the affected range
                for (int row = firstRow; row <= lastRow; row++) {
                    if (row >= 0 && row < plugin.getScheduledPlugins().size()) {
                        PluginScheduleEntry scheduled = plugin.getScheduledPlugins().get(row);
                        
                        if (e.getColumn() == 5) { // Default column
                            Boolean isDefault = (Boolean) tableModel.getValueAt(row, 5);
                            
                            // Check if this is a default plugin that shouldn't be modified
                            if (scheduled.isDefault()||scheduled.getIntervalDisplay().contains("Every 1 second") || 
                                "No schedule defined".equals(scheduled.getIntervalDisplay())) {
                                // Reset the value and update the isDefault flag for consistency
                                tableModel.setValueAt(true, row, 5);
                                scheduled.setDefault(true);
                            } else {
                                // For non-default plugins, update the value
                                scheduled.setDefault(isDefault);
                                
                                // If being set to default, also set priority to 0
                                if (isDefault) {
                                    scheduled.setPriority(0);
                                    tableModel.setValueAt(0, row, 4); // Update priority column
                                }
                            }
                        }
                        else if (e.getColumn() == 6) { // Enabled column
                            Boolean enabled = (Boolean) tableModel.getValueAt(row, 6);
                            scheduled.setEnabled(enabled);
                        }
                        else if (e.getColumn() == 7) { // Random scheduling column
                            Boolean allowRandom = (Boolean) tableModel.getValueAt(row, 7);
                            scheduled.setAllowRandomScheduling(allowRandom);
                        }
                    }
                }
                
                // Save after all updates are done
                plugin.saveScheduledPlugins();
                // Refresh the table to update visual indicators
                SwingUtilities.invokeLater(this::refreshTable);
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
        scheduleTable.getColumnModel().getColumn(3).setPreferredWidth(90);  // Conditions
        scheduleTable.getColumnModel().getColumn(4).setPreferredWidth(60);  // Priority
        scheduleTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // Default
        scheduleTable.getColumnModel().getColumn(6).setPreferredWidth(60);  // Enabled
        scheduleTable.getColumnModel().getColumn(7).setPreferredWidth(60);  // Random
        scheduleTable.getColumnModel().getColumn(8).setPreferredWidth(50);  // Run Count

        // Custom cell renderer for alternating row colors and special highlights
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (row >= 0 && row < plugin.getScheduledPlugins().size()) {
                    PluginScheduleEntry rowPlugin = plugin.getScheduledPlugins().get(row);
                    
                    if (isSelected) {
                        // Selected row styling takes precedence - use a distinct blue color
                        c.setBackground(SELECTION_COLOR);
                        c.setForeground(Color.WHITE);
                    } 
                    else if (rowPlugin.isRunning() && plugin.getCurrentPlugin() != null && 
                             rowPlugin.getName().equals(plugin.getCurrentPlugin().getName())) {
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
     * Determines if the provided plugin is the next one scheduled to run
     */
    private boolean isNextToRun(PluginScheduleEntry scheduledPlugin) {
        if (!scheduledPlugin.isEnabled()) {
            return false;
        }
        
        PluginScheduleEntry nextPlugin = plugin.getNextScheduledPlugin();
        return nextPlugin != null && nextPlugin.equals(scheduledPlugin);
    }
    
    
    public void refreshTable() {
        // Save current selection
        PluginScheduleEntry selectedPlugin = getSelectedPlugin();
        
        // Get current plugins and sort them by next run time
        List<PluginScheduleEntry> plugins = plugin.getScheduledPlugins();
        
        // Sort plugins by next run time (enabled plugins first, then by time)
        plugins.sort((p1, p2) -> {
            // First sort by enabled status (enabled plugins first)
            if (p1.isEnabled() != p2.isEnabled()) {
                return p1.isEnabled() ? -1 : 1;
            }
            
            // For running plugins, prioritize them at the top
            if (p1.isRunning() != p2.isRunning()) {
                return p1.isRunning() ? -1 : 1;
            }
            
            // If both are enabled or both disabled, sort by next run time
            Optional<ZonedDateTime> nextTime1 = p1.getNextStartTriggerTime();
            Optional<ZonedDateTime> nextTime2 = p2.getNextStartTriggerTime();
            
            if (nextTime1.isPresent() && nextTime2.isPresent()) {
                return nextTime1.get().compareTo(nextTime2.get());
            } else if (nextTime1.isPresent()) {
                return -1;
            } else if (nextTime2.isPresent()) {
                return 1;
            } else {
                // If no specific times, sort by name
                return p1.getName().compareTo(p2.getName());
            }
        });
        
        // Update table model
        tableModel.setRowCount(0);
        
        for (PluginScheduleEntry scheduled : plugins) {
            // Get basic information
            String pluginName = scheduled.getCleanName();
            if (scheduled.isRunning() && plugin.getCurrentPlugin() != null && 
                scheduled.getName().equals(plugin.getCurrentPlugin().getName())) {
                pluginName = "▶ " + pluginName; // Add play icon to current plugin
            }
            
            // Prepare schedule display with enhanced information
            String scheduleDisplay = getEnhancedScheduleDisplay(scheduled);
            
            // Enhanced next run display
            String nextRunDisplay = getEnhancedNextRunDisplay(scheduled);
            
            // Enhanced condition display with start and stop condition info
            String conditionInfo = getEnhancedConditionInfo(scheduled);
            
            // Add row to table
            tableModel.addRow(new Object[]{
                pluginName,
                scheduleDisplay,
                nextRunDisplay,
                conditionInfo,
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
        
        // Restore selection if possible
        if (selectedPlugin != null) {
            for (int i = 0; i < plugins.size(); i++) {
                if (plugins.get(i).equals(selectedPlugin)) {
                    scheduleTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
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
                if (selectedRow >= 0 && selectedRow < plugin.getScheduledPlugins().size()) {
                    listener.accept(plugin.getScheduledPlugins().get(selectedRow));
                } else {
                    listener.accept(null);
                }
            }
        });
    }

    public PluginScheduleEntry getSelectedPlugin() {
        int selectedRow = scheduleTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < plugin.getScheduledPlugins().size()) {
            return plugin.getScheduledPlugins().get(selectedRow);
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

    /**
     * Selects the given plugin in the table
     * @param plugin The plugin to select
     */
    public void selectPlugin(PluginScheduleEntry plugin) {
        if (plugin == null) return;
        
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String rowName = String.valueOf(tableModel.getValueAt(i, 0))
                .replaceAll("▶ ", ""); // Remove play indicator if present
            
            if (rowName.equals(plugin.getName())) {
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
