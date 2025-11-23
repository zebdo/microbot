package net.runelite.client.plugins.microbot.shortestpath;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.sailing.Rs2Sailing;
import net.runelite.client.plugins.microbot.util.sailing.data.BoatPathFollower;
import net.runelite.client.plugins.microbot.util.sailing.data.PortLocation;
import net.runelite.client.plugins.microbot.util.sailing.data.PortPaths;
import net.runelite.client.plugins.microbot.util.sailing.data.PortTaskData;
import net.runelite.client.plugins.microbot.util.sailing.data.PortTaskVarbits;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

/**
 * SailingPanel provides a navigation interface for sailing to port task destinations.
 * It displays all currently available port tasks and allows the user to navigate
 * their boat to the destination of the selected task.
 */
public class SailingPanel extends PluginPanel
{
    private static final int REFRESH_INTERVAL_MS = 2000;
    private static final int START_LOCATION_TOLERANCE = 50; // tiles

    private JPanel tasksPanel;
    private JLabel statusLabel;
    private javax.swing.Timer refreshTimer;
    private JComboBox<PortLocation> startLocationDropdown;
    private JComboBox<PortLocation> endLocationDropdown;
    private JLabel routeStatusLabel;

    // Active path following
    private BoatPathFollower activePathFollower;
    private ScheduledExecutorService sailingExecutor;
    private ScheduledFuture<?> sailingFuture;
    private PortTaskData currentTask;
    private PortPaths currentManualPath;
    private boolean currentManualReverse;

    // Debug overlay
    private boolean debugOverlayEnabled = false;
    private List<WorldPoint> currentPath = null;

    public SailingPanel()
    {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createInfoPanel());
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(createStatusPanel());
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(createTabbedPane());

        // Start refresh timer
        refreshTimer = new javax.swing.Timer(REFRESH_INTERVAL_MS, e -> refreshTaskList());
        refreshTimer.start();
    }

    private JPanel createInfoPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(createCenteredTitledBorder("Sailing Navigation", "sailing_icon.png"));

        JLabel infoLabel = new JLabel("<html><center>"
            + "<b>Boat Path Navigator</b><br><br>"
            + "Navigate your boat using predefined routes. "
            + "Use <b>Manual</b> tab to select custom routes, or "
            + "<b>Tasks</b> tab for your active port deliveries.<br><br>"
            + "<b>Note:</b> You must be on your boat and close to the starting location."
            + "</center></html>");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        panel.add(infoLabel);
        return panel;
    }

    private JPanel createStatusPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Status"));

        statusLabel = new JLabel("Checking boat status...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton stopButton = new JButton("Stop Navigation");
        stopButton.addActionListener(e -> stopNavigation());
        buttonPanel.add(stopButton);

        // Debug overlay checkbox
        JCheckBox debugCheckbox = new JCheckBox("Show Path Overlay");
        debugCheckbox.setSelected(debugOverlayEnabled);
        debugCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        debugCheckbox.addActionListener(e -> debugOverlayEnabled = debugCheckbox.isSelected());

        panel.add(statusLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(buttonPanel);
        panel.add(debugCheckbox);

        return panel;
    }

    private JTabbedPane createTabbedPane()
    {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Manual Navigation Tab
        tabbedPane.addTab("Manual", createManualNavigationPanel());

        // Port Tasks Tab
        tabbedPane.addTab("Tasks", createTasksPanel());

        return tabbedPane;
    }

    private JPanel createManualNavigationPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        // Get all unique locations from PortPaths (excluding EMPTY/DEFAULT)
        Set<PortLocation> locationSet = new HashSet<>();
        for (PortPaths path : PortPaths.values())
        {
            if (path != PortPaths.DEFAULT)
            {
                locationSet.add(path.getStart());
                locationSet.add(path.getEnd());
            }
        }

        PortLocation[] locations = locationSet.stream()
            .filter(loc -> loc != PortLocation.EMPTY)
            .sorted(Comparator.comparing(PortLocation::getName))
            .toArray(PortLocation[]::new);

        // Custom renderer for location dropdowns
        DefaultListCellRenderer locationRenderer = new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PortLocation)
                {
                    PortLocation loc = (PortLocation) value;
                    setText(loc.getName());
                }
                return this;
            }
        };

        // Starting location
        JLabel startLabel = new JLabel("Starting Location:");
        startLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        startLocationDropdown = new JComboBox<>(locations);
        startLocationDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        startLocationDropdown.setAlignmentX(Component.CENTER_ALIGNMENT);
        startLocationDropdown.setRenderer(locationRenderer);
        startLocationDropdown.addActionListener(e -> updateRouteStatus());

        // Ending location
        JLabel endLabel = new JLabel("Ending Location:");
        endLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        endLocationDropdown = new JComboBox<>(locations);
        endLocationDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        endLocationDropdown.setAlignmentX(Component.CENTER_ALIGNMENT);
        endLocationDropdown.setRenderer(locationRenderer);
        endLocationDropdown.addActionListener(e -> updateRouteStatus());

        if (locations.length > 1)
        {
            endLocationDropdown.setSelectedIndex(1);
        }

        // Route status label
        routeStatusLabel = new JLabel(" ");
        routeStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        updateRouteStatus();

        // Navigate button
        JButton navigateButton = new JButton("Navigate");
        navigateButton.addActionListener(e -> startManualNavigation());
        navigateButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components
        panel.add(startLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));
        panel.add(startLocationDropdown);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(endLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));
        panel.add(endLocationDropdown);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(routeStatusLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(navigateButton);

        // Add filler to push content to top
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createTasksPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        tasksPanel = new JPanel();
        tasksPanel.setLayout(new BoxLayout(tasksPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(tasksPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);

        // Refresh button at the bottom
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshButton = new JButton("Refresh Tasks");
        refreshButton.addActionListener(e -> refreshTaskList());
        bottomPanel.add(refreshButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateRouteStatus()
    {
        // Guard against null during initialization
        if (startLocationDropdown == null || endLocationDropdown == null || routeStatusLabel == null)
        {
            return;
        }

        PortLocation start = (PortLocation) startLocationDropdown.getSelectedItem();
        PortLocation end = (PortLocation) endLocationDropdown.getSelectedItem();

        if (start == null || end == null)
        {
            routeStatusLabel.setText(" ");
            routeStatusLabel.setForeground(Color.GRAY);
            return;
        }

        if (start == end)
        {
            routeStatusLabel.setText("Start and end must be different");
            routeStatusLabel.setForeground(Color.RED);
            return;
        }

        // Find a matching path
        PortPaths matchingPath = findPath(start, end);
        if (matchingPath != null)
        {
            routeStatusLabel.setText("Route available");
            routeStatusLabel.setForeground(new Color(0, 150, 0));
        }
        else
        {
            routeStatusLabel.setText("No direct route available");
            routeStatusLabel.setForeground(Color.RED);
        }
    }

    /**
     * Finds a PortPath that connects the given start and end locations.
     * Returns null if no path exists.
     */
    private PortPaths findPath(PortLocation start, PortLocation end)
    {
        for (PortPaths path : PortPaths.values())
        {
            if (path == PortPaths.DEFAULT)
            {
                continue;
            }

            // Check forward direction
            if (path.getStart() == start && path.getEnd() == end)
            {
                return path;
            }

            // Check reverse direction
            if (path.getStart() == end && path.getEnd() == start)
            {
                return path;
            }
        }
        return null;
    }

    /**
     * Determines if the path needs to be reversed based on the selected start location.
     */
    private boolean isPathReversed(PortPaths path, PortLocation selectedStart)
    {
        return path.getEnd() == selectedStart;
    }

    private void refreshTaskList()
    {
        new SwingWorker<RefreshResult, Void>()
        {
            @Override
            protected RefreshResult doInBackground()
            {
                boolean onBoat = Rs2Sailing.isOnBoat();
                boolean navigating = Rs2Sailing.isNavigating();

                Map<PortTaskVarbits, Integer> activeTasks = Rs2Sailing.getPortTasksVarbits();

                java.util.List<PortTaskData> taskDataList = new ArrayList<>();
                if (activeTasks != null && !activeTasks.isEmpty())
                {
                    for (Map.Entry<PortTaskVarbits, Integer> entry : activeTasks.entrySet())
                    {
                        PortTaskData taskData = Rs2Sailing.getPortTaskData(entry.getValue());
                        if (taskData != null)
                        {
                            taskDataList.add(taskData);
                        }
                    }
                }

                return new RefreshResult(onBoat, navigating, taskDataList);
            }

            @Override
            protected void done()
            {
                RefreshResult result;
                try
                {
                    result = get();
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    return;
                }

                boolean onBoat = result.onBoat;
                boolean navigating = result.navigating;
                java.util.List<PortTaskData> taskDataList = result.tasks;

                // Status label logic (unchanged)
                if (currentTask != null && sailingFuture != null && !sailingFuture.isDone())
                {
                    statusLabel.setText("<html><center>Sailing to: <b>" + currentTask.taskName + "</b></center></html>");
                    statusLabel.setForeground(new Color(0, 150, 0));
                }
                else if (currentManualPath != null && sailingFuture != null && !sailingFuture.isDone())
                {
                    String dest = currentManualReverse
                            ? currentManualPath.getStart().getName()
                            : currentManualPath.getEnd().getName();
                    statusLabel.setText("<html><center>Sailing to: <b>" + dest + "</b></center></html>");
                    statusLabel.setForeground(new Color(0, 150, 0));
                }
                else if (onBoat && navigating)
                {
                    statusLabel.setText("On boat - At helm (Ready)");
                    statusLabel.setForeground(new Color(0, 150, 0));
                }
                else if (onBoat)
                {
                    statusLabel.setText("On boat - Not at helm");
                    statusLabel.setForeground(new Color(200, 150, 0));
                }
                else
                {
                    statusLabel.setText("Not on a boat");
                    statusLabel.setForeground(Color.RED);
                }

                tasksPanel.removeAll();

                if (taskDataList.isEmpty())
                {
                    JLabel noTasksLabel = new JLabel(
                            "<html><center>No active port tasks found.<br><br>" +
                                    "Visit a port notice board<br>to get delivery tasks.</center></html>"
                    );
                    noTasksLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    noTasksLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    tasksPanel.add(Box.createVerticalGlue());
                    tasksPanel.add(noTasksLabel);
                    tasksPanel.add(Box.createVerticalGlue());
                }
                else
                {
                    for (PortTaskData taskData : taskDataList)
                    {
                        tasksPanel.add(createTaskPanel(taskData));
                        tasksPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                    }
                }

                tasksPanel.revalidate();
                tasksPanel.repaint();
            }
        }.execute();
    }

    /**
     * Simple DTO for background result
     */
    private static final class RefreshResult
    {
        final boolean onBoat;
        final boolean navigating;
        final java.util.List<PortTaskData> tasks;

        RefreshResult(boolean onBoat, boolean navigating, java.util.List<PortTaskData> tasks)
        {
            this.onBoat = onBoat;
            this.navigating = navigating;
            this.tasks = tasks;
        }
    }

    private JPanel createTaskPanel(PortTaskData task)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JLabel nameLabel = new JLabel("<html><b>" + formatTaskName(task.taskName) + "</b></html>");
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String startName = task.getDockMarkers().getStart().getName();
        String endName = task.getDockMarkers().getEnd().getName();
        String routeText = task.isReversePath()
            ? String.format("%s -> %s", endName, startName)
            : String.format("%s -> %s", startName, endName);

        JLabel routeLabel = new JLabel(routeText);
        routeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        routeLabel.setFont(routeLabel.getFont().deriveFont(11f));
        routeLabel.setForeground(Color.GRAY);

        JButton navigateButton = new JButton("Navigate");
        navigateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        navigateButton.addActionListener(e -> startNavigation(task));

        if (currentTask != null && currentTask.equals(task) && sailingFuture != null && !sailingFuture.isDone())
        {
            navigateButton.setText("Active");
            navigateButton.setEnabled(false);
            panel.setBackground(new Color(200, 255, 200));
            panel.setOpaque(true);
        }

        panel.add(nameLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));
        panel.add(routeLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(navigateButton);

        return panel;
    }

    private String formatTaskName(String taskName)
    {
        if (taskName == null || taskName.isEmpty())
        {
            return taskName;
        }
        String[] words = taskName.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words)
        {
            if (!word.isEmpty())
            {
                result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
            }
        }
        return result.toString().trim();
    }

    private void startManualNavigation()
    {
        PortLocation start = (PortLocation) startLocationDropdown.getSelectedItem();
        PortLocation end = (PortLocation) endLocationDropdown.getSelectedItem();

        if (start == null || end == null)
        {
            Microbot.showMessage("Please select both starting and ending locations.");
            return;
        }

        if (start == end)
        {
            Microbot.showMessage("Starting and ending locations must be different.");
            return;
        }

        if (!Rs2Sailing.isOnBoat())
        {
            Microbot.showMessage("You must be on a boat to navigate!");
            return;
        }

        // Find the path
        PortPaths path = findPath(start, end);
        if (path == null)
        {
            Microbot.showMessage("No route available between " + start.getName() + " and " + end.getName() + ".");
            return;
        }

        boolean reverse = isPathReversed(path, start);

        // Get start location based on reverse flag
        WorldPoint startLocation = start.getNavigationLocation();

        // Check if player is close enough to start location
        WorldPoint boatLocation = Rs2Sailing.getPlayerBoatLocation();
        if (boatLocation == null)
        {
            Microbot.showMessage("Could not determine boat location.");
            return;
        }

        int distance = boatLocation.distanceTo(startLocation);
        if (distance > START_LOCATION_TOLERANCE)
        {
            Microbot.showMessage(String.format(
                "You are too far from %s. Distance: %d tiles.",
                start.getName(), distance));
            return;
        }

        // Stop any existing navigation
        stopNavigation();

        currentManualPath = path;
        currentManualReverse = reverse;
        currentTask = null;

        // Get the full path
        List<WorldPoint> fullPath = path.getFullPath(reverse);

        if (fullPath == null || fullPath.isEmpty())
        {
            Microbot.showMessage("No valid path found for this route.");
            currentManualPath = null;
            return;
        }

        Microbot.log("Starting sailing navigation from " + start.getName() + " to " + end.getName());
        Microbot.log("Path has " + fullPath.size() + " waypoints, reverse=" + reverse);

        // Store path for debug overlay
        currentPath = fullPath;

        // Create the path follower
        activePathFollower = new BoatPathFollower(fullPath);

        // Start the sailing loop
        sailingExecutor = Executors.newSingleThreadScheduledExecutor();
        sailingFuture = sailingExecutor.scheduleWithFixedDelay(() -> {
            try
            {
                if (!Rs2Sailing.isOnBoat())
                {
                    Microbot.log("No longer on boat, stopping navigation.");
                    stopNavigation();
                    return;
                }

                boolean result = activePathFollower.loop();
                if (result)
                {
                    Microbot.log("Reached destination: " + end.getName());
                    sailingFuture.cancel(true);
                }
            }
            catch (Exception e)
            {
                Microbot.log("Error during sailing navigation: " + e.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);

        // Refresh UI to show active state
        refreshTaskList();
    }

    private void startNavigation(PortTaskData task)
    {
        if (!Rs2Sailing.isOnBoat())
        {
            Microbot.showMessage("You must be on a boat to navigate!");
            return;
        }

        PortPaths path = task.getDockMarkers();
        boolean reverse = task.isReversePath();

        // Get start location based on reverse flag
        WorldPoint startLocation = reverse
            ? path.getEnd().getNavigationLocation()
            : path.getStart().getNavigationLocation();

        // Check if player is close enough to start location
        WorldPoint boatLocation = Rs2Sailing.getPlayerBoatLocation();
        if (boatLocation == null)
        {
            Microbot.showMessage("Could not determine boat location.");
            return;
        }

        int distance = boatLocation.distanceTo(startLocation);
        if (distance > START_LOCATION_TOLERANCE)
        {
            String startName = reverse ? path.getEnd().getName() : path.getStart().getName();
            Microbot.showMessage(String.format(
                "You are too far from %s. Distance: %d tiles.",
                startName, distance));
            return;
        }

        // Stop any existing navigation
        stopNavigation();

        currentTask = task;
        currentManualPath = null;

        // Get the full path based on the reverse flag
        List<WorldPoint> fullPath = path.getFullPath(reverse);

        if (fullPath == null || fullPath.isEmpty())
        {
            Microbot.showMessage("No valid path found for this task.");
            currentTask = null;
            return;
        }

        Microbot.log("Starting sailing navigation to: " + task.taskName);
        Microbot.log("Path has " + fullPath.size() + " waypoints, reverse=" + reverse);

        // Store path for debug overlay
        currentPath = fullPath;

        // Create the path follower
        activePathFollower = new BoatPathFollower(fullPath);

        // Start the sailing loop
        sailingExecutor = Executors.newSingleThreadScheduledExecutor();
        sailingFuture = sailingExecutor.scheduleWithFixedDelay(() -> {
            try
            {
                if (!Rs2Sailing.isOnBoat())
                {
                    Microbot.log("No longer on boat, stopping navigation.");
                    stopNavigation();
                    return;
                }

                boolean result = activePathFollower.loop();
                if (result)
                {
                    Microbot.log("Reached destination for task: " + task.taskName);
                    sailingFuture.cancel(true);
                }
            }
            catch (Exception e)
            {
                Microbot.log("Error during sailing navigation: " + e.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);

        // Refresh UI to show active state
        refreshTaskList();
    }

    private void stopNavigation()
    {
        if (sailingFuture != null)
        {
            sailingFuture.cancel(true);
            sailingFuture = null;
        }

        if (sailingExecutor != null)
        {
            sailingExecutor.shutdownNow();
            sailingExecutor = null;
        }

        activePathFollower = null;
        currentTask = null;
        currentManualPath = null;
        currentPath = null;

        // Stop the boat sails
        if (Rs2Sailing.isOnBoat() && Rs2Sailing.isNavigating())
        {
            Rs2Sailing.unsetSails();
        }

        Microbot.log("Sailing navigation stopped.");
        refreshTaskList();
    }

    private Border createCenteredTitledBorder(String title, String iconPath)
    {
        BufferedImage icon = ImageUtil.loadImageResource(ShortestPathPlugin.class, iconPath);
        ImageIcon imageIcon = new ImageIcon(icon);

        JLabel titleLabel = new JLabel("<html><b>" + title + "</b></html>", imageIcon, JLabel.CENTER);
        titleLabel.setHorizontalTextPosition(JLabel.RIGHT);
        titleLabel.setVerticalTextPosition(JLabel.CENTER);

        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        Border lineBorder = BorderFactory.createLineBorder(Color.GRAY);

        return BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                lineBorder,
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
            ),
            new TitledBorder(emptyBorder, title, TitledBorder.CENTER, TitledBorder.TOP, null, null)
            {
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
                {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.translate(x + width / 2 - titleLabel.getPreferredSize().width / 2, y);
                    titleLabel.setSize(titleLabel.getPreferredSize());
                    titleLabel.paint(g2d);
                    g2d.dispose();
                }
            }
        );
    }

    /**
     * Clean up resources when the panel is disposed.
     */
    public void dispose()
    {
        if (refreshTimer != null)
        {
            refreshTimer.stop();
            refreshTimer = null;
        }
        stopNavigation();
    }

    /**
     * Returns whether the debug overlay is enabled.
     */
    public boolean isDebugOverlayEnabled()
    {
        return debugOverlayEnabled;
    }

    /**
     * Returns the current navigation path.
     */
    public List<WorldPoint> getCurrentPath()
    {
        return currentPath;
    }

    /**
     * Returns the current waypoint index from the path follower.
     */
    public int getCurrentWaypointIndex()
    {
        if (activePathFollower != null)
        {
            return activePathFollower.getCurrentWaypointIndex();
        }
        return 0;
    }
}
