// java
package net.runelite.client.plugins.microbot.util.poh.ui;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.poh.PohConfig;
import net.runelite.client.plugins.microbot.util.poh.data.PohPortal;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.poh.PohConfig.CONFIG_GROUP;
import static net.runelite.client.plugins.microbot.util.poh.PohConfig.PORTALS;

public class PortalPanel extends JPanel {
    private final DefaultListModel<PohPortal> selectedModel = new DefaultListModel<>();
    private JList<PohPortal> portalsList;
    private JButton addButton;
    private JButton removeButton;

    private final PohConfig config;

    public PortalPanel(PohConfig config) {
        this.config = config;
        setBorder(new TitledBorder("Portal Teleports"));
        setLayout(new GridBagLayout());

        portalsList = new JList<>(selectedModel);
        portalsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        portalsList.setVisibleRowCount(6);
        portalsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PohPortal) {
                    setText(((PohPortal) value).getDisplayName());
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(portalsList);
        // Ensure the portals panel doesn't render too small
        Dimension minSize = new Dimension(180, 150);
        scroll.setMinimumSize(minSize);
        scroll.setPreferredSize(minSize);

        // Controls
        addButton = new JButton("Add");
        removeButton = new JButton("Remove");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        buttons.add(addButton);
        buttons.add(removeButton);

        // Configure GridBagConstraints for scroll pane (list)
        GridBagConstraints scrollGbc = new GridBagConstraints();
        scrollGbc.gridx = 0;
        scrollGbc.gridy = 0;
        scrollGbc.gridwidth = 1;
        scrollGbc.gridheight = 1;
        scrollGbc.weightx = 0.0; // Don't expand horizontally
        scrollGbc.weighty = 1.0; // Allow vertical expansion
        scrollGbc.fill = GridBagConstraints.BOTH; // Fill available space in both directions
        scrollGbc.anchor = GridBagConstraints.NORTHWEST; // Anchor to northwest
        scrollGbc.insets = new Insets(0, 0, 8, 0); // Add spacing below the scroll pane

        add(scroll, scrollGbc);

        // Configure GridBagConstraints for button panel
        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.gridx = 0;
        buttonGbc.gridy = 1;
        buttonGbc.gridwidth = 1;
        buttonGbc.gridheight = 1;
        buttonGbc.weightx = 0.0; // Don't expand horizontally
        buttonGbc.weighty = 0.0; // Don't expand vertically
        buttonGbc.fill = GridBagConstraints.NONE; // Don't fill, keep natural size
        buttonGbc.anchor = GridBagConstraints.WEST; // Anchor to west (left side)
        buttonGbc.insets = new Insets(0, 0, 0, 0);

//        add(buttons, buttonGbc);

        // Load initial state
        loadPortalsFromConfig().forEach(selectedModel::addElement);
        updateButtonsState();

        hookListeners();
    }


    private void hookListeners() {
        addButton.addActionListener(e -> openAddDialog());

        removeButton.addActionListener(e -> {
            int idx = portalsList.getSelectedIndex();
            if (idx >= 0) {
                selectedModel.remove(idx);
                savePortalsToConfig();
                updateButtonsState();
            }
        });

        portalsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonsState();
            }
        });
    }

    private void openAddDialog() {
        // Build a list of portals that are not yet selected
        java.util.List<PohPortal> available = Arrays.stream(PohPortal.values())
                .filter(p -> !containsInModel(p))
                .collect(Collectors.toList());

        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add Portal", Dialog.ModalityType.APPLICATION_MODAL);
        DefaultListModel<PohPortal> model = new DefaultListModel<>();
        available.forEach(model::addElement);

        final JList<PohPortal> availableList = new JList<>(model);
        availableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PohPortal) {
                    setText(((PohPortal) value).getDisplayName());
                }
                return this;
            }
        });
        // Double-click to add and close
        availableList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int index = availableList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        PohPortal portal = availableList.getModel().getElementAt(index);
                        if (!containsInModel(portal)) {
                            selectedModel.addElement(portal);
                            savePortalsToConfig();
                        }
                        dialog.dispose();
                        updateButtonsState();
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(availableList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JButton ok = new JButton("Add");
        ok.addActionListener(ev ->
        {
            PohPortal portal = availableList.getSelectedValue();
            if (portal != null && !containsInModel(portal)) {
                selectedModel.addElement(portal);
                savePortalsToConfig();
            }
            dialog.dispose();
            updateButtonsState();
        });

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(ev -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(ok);
        buttons.add(cancel);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(scroll, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }


    private boolean containsInModel(PohPortal portal) {
        for (int i = 0; i < selectedModel.size(); i++) {
            if (selectedModel.get(i) == portal) {
                return true;
            }
        }
        return false;
    }

    private void updateButtonsState() {
        removeButton.setEnabled(!portalsList.isSelectionEmpty());
        addButton.setEnabled(selectedModel.getSize() < PohPortal.values().length);
    }

    public void savePortalsToConfig() {
        List<PohPortal> portals = new ArrayList<>();
        for (int i = 0; i < selectedModel.getSize(); i++) {
            portals.add(selectedModel.get(i));
        }
        String serialized = portals.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
        Microbot.getConfigManager().setConfiguration(CONFIG_GROUP, PORTALS, serialized);
    }

    public void detectPortals() {
        List<PohPortal> detected = PohPortal.findPortalsInPoh();
        selectedModel.clear();
        selectedModel.addAll(detected);
        savePortalsToConfig();
    }

    // Helpers: load portals in a simple comma-separated enum format
    public List<PohPortal> loadPortalsFromConfig() {
        String raw = config.portals();
        if (raw == null || raw.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(name ->
                {
                    try {
                        return PohPortal.valueOf(name);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}