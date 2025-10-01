package net.runelite.client.plugins.microbot.shortestpath.components;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathConfig;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.poh.PohTransport;
import net.runelite.client.plugins.microbot.util.poh.data.PohTeleport;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin.CONFIG_GROUP;

public class EnumListPanel<T extends Enum<T>> extends JPanel {

    private final DefaultListModel<T> selectedModel = new DefaultListModel<>();
    private JList<T> teleportList;
    private JButton addButton;
    private JButton removeButton;

    private final String title;
    private final ShortestPathConfig config;
    private final Class<T> enumClass;

    public EnumListPanel(ShortestPathConfig config, Class<T> enumClass, String title) {
        this.title = title;
        this.config = config;
        this.enumClass = enumClass;
        setBorder(new TitledBorder(title));
        setLayout(new GridBagLayout());

        teleportList = new JList<>(selectedModel);
        teleportList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        teleportList.setVisibleRowCount(6);
        teleportList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value.toString());
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(teleportList);
        // Ensure the panel doesn't render too small
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
        loadTeleportsFromConfig().forEach(selectedModel::addElement);
        updateButtonsState();

        hookListeners();
    }


    private void hookListeners() {
        addButton.addActionListener(e -> openAddDialog());

        removeButton.addActionListener(e -> {
            int idx = teleportList.getSelectedIndex();
            if (idx >= 0) {
                selectedModel.remove(idx);
                saveTeleportsToConfig();
                updateButtonsState();
            }
        });

        teleportList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonsState();
            }
        });
    }

    private void openAddDialog() {
        // Build a list of teleports that are not yet selected
        java.util.List<T> available = Arrays.stream(enumClass.getEnumConstants())
                .filter(n -> !containsInModel(n))
                .collect(Collectors.toList());

        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add Teleport", Dialog.ModalityType.APPLICATION_MODAL);
        DefaultListModel<T> model = new DefaultListModel<>();
        available.forEach(model::addElement);

        final JList<T> availableList = new JList<>(model);
        availableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value.toString());
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
                        T teleport = availableList.getModel().getElementAt(index);
                        if (!containsInModel(teleport)) {
                            selectedModel.addElement(teleport);
                            saveTeleportsToConfig();
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
            T teleport = availableList.getSelectedValue();
            if (teleport != null && !containsInModel(teleport)) {
                selectedModel.addElement(teleport);
                saveTeleportsToConfig();
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

    private boolean containsInModel(T teleport) {
        for (int i = 0; i < selectedModel.size(); i++) {
            if (selectedModel.get(i) == teleport) {
                return true;
            }
        }
        return false;
    }

    private void updateButtonsState() {
        removeButton.setEnabled(!teleportList.isSelectionEmpty());
        addButton.setEnabled(selectedModel.getSize() < enumClass.getEnumConstants().length);
    }

    public void setAll(java.util.List<T> teleports) {
        selectedModel.clear();
        teleports.forEach(selectedModel::addElement);
        saveTeleportsToConfig();
    }

    public void saveTeleportsToConfig() {
        java.util.List<T> teleports = new ArrayList<>();
        for (int i = 0; i < selectedModel.getSize(); i++) {
            teleports.add(selectedModel.get(i));
        }
        String serialized = teleports.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
        Microbot.getConfigManager().setConfiguration(CONFIG_GROUP, title, serialized);
    }

    public java.util.List<T> loadTeleportsFromConfig() {
        String raw = Microbot.getConfigManager().getConfiguration(CONFIG_GROUP, title);
        if (raw == null || raw.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(name ->
                {
                    try {
                        return Enum.valueOf(enumClass, name);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Set<PohTeleport> getTeleports() {
        Set<PohTeleport> teleports = new HashSet<>();
        for (int i = 0; i < selectedModel.getSize(); i++) {
            teleports.add((PohTeleport) selectedModel.get(i));
        }
        return teleports;
    }
}
