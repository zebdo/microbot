package net.runelite.client.plugins.microbot.util.poh.ui;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.poh.PohConfig;
import net.runelite.client.plugins.microbot.util.poh.data.NexusTeleport;

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
import static net.runelite.client.plugins.microbot.util.poh.PohConfig.NEXUS;

public class NexusPanel extends JPanel {

    private final DefaultListModel<NexusTeleport> selectedModel = new DefaultListModel<>();
    private JList<NexusTeleport> nexusList;
    private JButton addButton;
    private JButton removeButton;

    private final PohConfig config;

    public NexusPanel(PohConfig config) {
        this.config = config;
        setBorder(new TitledBorder("Portal Nexus Teleports"));
        setLayout(new GridBagLayout());

        nexusList = new JList<>(selectedModel);
        nexusList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        nexusList.setVisibleRowCount(6);
        nexusList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof NexusTeleport) {
                    setText(((NexusTeleport) value).getText());
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(nexusList);
        // Ensure the nexus panel doesn't render too small
        Dimension minSize = new Dimension(180, 150);
        scroll.setMinimumSize(minSize);
        scroll.setPreferredSize(minSize);


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

        // Controls
        addButton = new JButton("Add");
        removeButton = new JButton("Remove");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(addButton);
        buttons.add(removeButton);

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
        loadNexusTeleportsFromConfig().forEach(selectedModel::addElement);
        updateButtonsState();

        hookListeners();
    }

    private void hookListeners() {
        addButton.addActionListener(e -> openAddDialog());

        removeButton.addActionListener(e -> {
            int idx = nexusList.getSelectedIndex();
            if (idx >= 0) {
                selectedModel.remove(idx);
                saveNexusTeleportsToConfig();
                updateButtonsState();
            }
        });

        nexusList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonsState();
            }
        });
    }

    private void openAddDialog() {
        // Build a list of nexus teleports that are not yet selected
        List<NexusTeleport> available = Arrays.stream(NexusTeleport.values())
                .filter(n -> !containsInModel(n))
                .collect(Collectors.toList());

        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add Nexus Teleport", Dialog.ModalityType.APPLICATION_MODAL);
        DefaultListModel<NexusTeleport> model = new DefaultListModel<>();
        available.forEach(model::addElement);

        final JList<NexusTeleport> availableList = new JList<>(model);
        availableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof NexusTeleport) {
                    setText(((NexusTeleport) value).getText());
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
                        NexusTeleport nexusTeleport = availableList.getModel().getElementAt(index);
                        if (!containsInModel(nexusTeleport)) {
                            selectedModel.addElement(nexusTeleport);
                            saveNexusTeleportsToConfig();
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
            NexusTeleport nexusTeleport = availableList.getSelectedValue();
            if (nexusTeleport != null && !containsInModel(nexusTeleport)) {
                selectedModel.addElement(nexusTeleport);
                saveNexusTeleportsToConfig();
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

    private boolean containsInModel(NexusTeleport nexusTeleport) {
        for (int i = 0; i < selectedModel.size(); i++) {
            if (selectedModel.get(i) == nexusTeleport) {
                return true;
            }
        }
        return false;
    }

    private void updateButtonsState() {
        removeButton.setEnabled(!nexusList.isSelectionEmpty());
        addButton.setEnabled(selectedModel.getSize() < NexusTeleport.values().length);
    }

    public void detectNexusTeleports() {
        List<NexusTeleport> teleports = NexusTeleport.getAvailableTeleports();
        selectedModel.clear();
        selectedModel.addAll(teleports);
        saveNexusTeleportsToConfig();
    }

    public void saveNexusTeleportsToConfig() {
        List<NexusTeleport> nexusTeleports = new ArrayList<>();
        for (int i = 0; i < selectedModel.getSize(); i++) {
            nexusTeleports.add(selectedModel.get(i));
        }
        String serialized = nexusTeleports.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
        Microbot.getConfigManager().setConfiguration(CONFIG_GROUP, NEXUS, serialized);
    }

    // Helpers: load nexus teleports in a simple comma-separated enum format
    public List<NexusTeleport> loadNexusTeleportsFromConfig() {
        String raw = config.nexusTeleports();
        if (raw == null || raw.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(name ->
                {
                    try {
                        return NexusTeleport.valueOf(name);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
