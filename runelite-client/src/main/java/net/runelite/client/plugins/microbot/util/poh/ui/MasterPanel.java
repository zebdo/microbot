package net.runelite.client.plugins.microbot.util.poh.ui;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.poh.PohConfig;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;

public class MasterPanel extends PluginPanel {

    private PortalPanel portalPanel;
    private NexusPanel nexusPanel;
    private CheckboxPanel checkboxPanel;
    private JewelleryBoxPanel jewelleryBoxPanel;

    private JButton detectButton;
    private JScrollPane mainScrollPane;

    private PohConfig config;

    public MasterPanel(PohConfig config) {
        super(false);
        this.config = config;
        setLayout(new BorderLayout(8, 8));

        setPreferredSize(null); // Remove any fixed preferred size
        setMaximumSize(null);   // Remove any fixed maximum size

        // Create scrollable content
        mainScrollPane = createMainScrollPane();
        add(mainScrollPane, BorderLayout.CENTER);
        add(buildTopBar(), BorderLayout.SOUTH);
    }

    /**
     * Creates and configures the main scroll pane containing all the content panels.
     *
     * @return A properly configured JScrollPane with all content panels
     */
    private JScrollPane createMainScrollPane() {
        JPanel contentPanel = buildContent();

        JScrollPane scrollPane = new JScrollPane(contentPanel);

        // Configure scroll policies
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Remove default border for cleaner look
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Set background to match the content
        scrollPane.getViewport().setBackground(contentPanel.getBackground());

        // Configure scroll increment for smoother scrolling
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setBlockIncrement(64);

        // Let it fill available space - don't set fixed sizes
        scrollPane.setMinimumSize(new Dimension(120, 0));

        return scrollPane;
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets.set(4, 4, 4, 4);

        // Portals list
        gbc.gridy = 0;
        portalPanel = new PortalPanel(config);
        root.add(portalPanel, gbc);

        // Nexus panel
        gbc.gridy++;
        nexusPanel = new NexusPanel(config);
        root.add(nexusPanel, gbc);

        // Booleans section
        gbc.gridy++;
        checkboxPanel = new CheckboxPanel(config);
        root.add(checkboxPanel, gbc);

        // Dropdowns section
        gbc.gridy++;
        jewelleryBoxPanel = new JewelleryBoxPanel(config);
        root.add(jewelleryBoxPanel, gbc);

        // Add a filler component to push content to the top and allow proper stretching
        gbc.gridy++;
        gbc.weighty = 1.0; // This is crucial - it allows vertical expansion
        gbc.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        root.add(filler, gbc);

        return root;
    }

    private JButton buildTopBar() {
        detectButton = new JButton("Detect available POH teleports");
        detectButton.addActionListener(e -> detectPortalsFromPOH());

        return detectButton;
    }


    private void detectPortalsFromPOH() {
        if (!PohTeleports.isInHouse()) {
            Microbot.log("Please make sure you are in the POH first!");
            return;
        }
        detectButton.setEnabled(false);
        detectButton.setText("Detecting...");
        checkboxPanel.detectPohFacilities();
        portalPanel.detectPortals();
        nexusPanel.detectNexusTeleports();
        jewelleryBoxPanel.detectJewelleryBox();
        detectButton.setEnabled(true);
        detectButton.setText("Detect available POH teleports");
    }
}